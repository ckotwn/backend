package life.catalogue.command;

import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.setup.Bootstrap;
import life.catalogue.WsServerConfig;
import life.catalogue.api.model.Dataset;
import life.catalogue.api.vocab.DatasetOrigin;
import life.catalogue.dao.DatasetInfoCache;
import life.catalogue.dao.DatasetProjectSourceDao;
import life.catalogue.db.MybatisFactory;
import life.catalogue.db.mapper.DatasetMapper;
import life.catalogue.db.mapper.ProjectSourceMapper;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command to add new partition tables for a given master table.
 * When adding new partitioned tables to the db schema we need to create a partition table
 * for every existing dataset that has data.
 *
 * The command will look at the existing name partition tables to find the datasets with data.
 * The master table must exist already and be defined to be partitioned by column dataset_key !!!
 */
public class RebuiltSourceCitationCmd extends AbstractPromptCmd {
  private static final Logger LOG = LoggerFactory.getLogger(RebuiltSourceCitationCmd.class);
  private static final String ARG_KEY = "key";
  private static final String ARG_DRY = "dry";

  private SqlSessionFactory factory;
  private HikariDataSource dataSource;
  private Dataset release;

  public RebuiltSourceCitationCmd() {
    super("updCitations", "Update all source citations in metadata for an existing release based on the current project template");
  }
  
  @Override
  public void configure(Subparser subparser) {
    super.configure(subparser);
    // Adds import options
    subparser.addArgument("--"+ARG_KEY, "-k")
        .dest(ARG_KEY)
        .type(Integer.class)
        .required(true)
        .help("dataset key of the release to update");
    subparser.addArgument("--"+ARG_DRY)
      .dest(ARG_DRY)
      .type(Boolean.class)
      .required(false)
      .setDefault(false)
      .help("dry run only showing the regenerated citations, not storing them");
  }

  @Override
  public void execute(Bootstrap<WsServerConfig> bootstrap, Namespace namespace, WsServerConfig cfg) throws Exception {
    int releaseKey = namespace.getInt(ARG_KEY);
    boolean dryRun = namespace.getBoolean(ARG_DRY);

    dataSource = cfg.db.pool();
    factory = MybatisFactory.configure(dataSource, "tools");
    DatasetInfoCache.CACHE.setFactory(factory);

    try (SqlSession session = factory.openSession()) {
      DatasetMapper dm = session.getMapper(DatasetMapper.class);
      release = dm.get(releaseKey);
      if (release.getOrigin() != DatasetOrigin.RELEASED) {
        throw new IllegalArgumentException("Dataset key "+releaseKey+" is not a release!");
      }
    }

    System.out.printf("Citation for release %s: %s\n\n", release.getKey(), release.getCitation());
    DatasetProjectSourceDao dao = new DatasetProjectSourceDao(factory);
    if (dryRun) {
      System.out.println("Dry run");
      show(dao);
    } else {
      update(dao);
    }
    System.out.println("Done.");
  }

  void show(DatasetProjectSourceDao dao){
    dao.list(release.getKey(), release, true).forEach(d -> {
      System.out.printf("%s: %s\n", d.getKey(), d.getCitation());
    });
  }

  void update(DatasetProjectSourceDao dao) {
    try (SqlSession session = factory.openSession(false)) {
      ProjectSourceMapper psm = session.getMapper(ProjectSourceMapper.class);
      int cnt = psm.deleteByProject(release.getKey());
      session.commit();
      System.out.printf("Deleted %s old source metadata records\n", cnt);

      AtomicInteger counter = new AtomicInteger(0);
      dao.list(release.getKey(), release, true).forEach(d -> {
        counter.incrementAndGet();
        System.out.printf("%s: %s\n", d.getKey(), d.getCitation());
        psm.create(release.getKey(), d);
      });
      session.commit();
      System.out.printf("Created %s new source metadata records\n", counter);
    }
  }

}
