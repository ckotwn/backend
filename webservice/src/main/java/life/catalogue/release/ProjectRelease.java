package life.catalogue.release;

import life.catalogue.api.model.Dataset;
import life.catalogue.api.model.DatasetSettings;
import life.catalogue.api.model.NameMatch;
import life.catalogue.api.vocab.DatasetOrigin;
import life.catalogue.api.vocab.ImportState;
import life.catalogue.api.vocab.Setting;
import life.catalogue.common.text.CitationUtils;
import life.catalogue.dao.DatasetImportDao;
import life.catalogue.dao.DatasetProjectSourceDao;
import life.catalogue.db.mapper.DatasetMapper;
import life.catalogue.db.mapper.NameMapper;
import life.catalogue.db.mapper.ProjectSourceMapper;
import life.catalogue.es.NameUsageIndexService;
import life.catalogue.img.ImageService;
import life.catalogue.matching.NameIndex;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

public class ProjectRelease extends AbstractProjectCopy {
  private static final String DEFAULT_TITLE_TEMPLATE = "{title}, {date}";
  private static final String DEFAULT_ALIAS_TEMPLATE = "{aliasOrTitle}-{date}";

  private final ImageService imageService;
  private final NameIndex nameIndex;
  private final boolean useStableIDs;

  ProjectRelease(SqlSessionFactory factory, NameIndex nameIndex, NameUsageIndexService indexService, DatasetImportDao diDao, ImageService imageService,
                 int datasetKey, Dataset release, int userKey, boolean useStableIDs) {
    super("releasing", factory, diDao, indexService, userKey, datasetKey, release, true);
    this.imageService = imageService;
    this.nameIndex = nameIndex;
    this.useStableIDs = useStableIDs;
  }

  @Override
  void prepWork() throws Exception {
    // map ids
    updateState(ImportState.MATCHING);
    matchUnmatchedNames();
    IdProvider idProvider;
    if (useStableIDs) {
      idProvider = IdProvider.withNoReleases(datasetKey, metrics.getAttempt(), factory);
    } else {
      idProvider = IdProvider.withAllReleases(datasetKey, metrics.getAttempt(), factory);
    }
    idProvider.run();

    // archive dataset metadata & logos
    updateState(ImportState.ARCHIVING);
    DatasetProjectSourceDao dao = new DatasetProjectSourceDao(factory);
    try (SqlSession session = factory.openSession(true)) {
      ProjectSourceMapper psm = session.getMapper(ProjectSourceMapper.class);
      final AtomicInteger counter = new AtomicInteger(0);
      dao.list(datasetKey).forEach(d -> {
        LOG.info("Archive dataset {}#{} for release {}", d.getKey(), d.getImportAttempt(), newDatasetKey);
        psm.create(newDatasetKey, d);
        // archive logos
        try {
          imageService.archiveDatasetLogo(newDatasetKey, d.getKey());
        } catch (IOException e) {
          LOG.warn("Failed to archive logo for source dataset {} of release {}", d.getKey(), newDatasetKey, e);
        }
        counter.incrementAndGet();
      });
      LOG.info("Archived metadata for {} source datasets of release {}", counter.get(), newDatasetKey);
    }

    // create new dataset "import" metrics in mother project
    updateState(ImportState.ANALYZING);
    metrics();
  }

  /**
   * Makes sure all names are matched to the names index.
   * When syncing names from other sources the names index match is carried over
   * so there should really not be any name without a match.
   * We still make sure here that at least there is no such case in releases.
   */
  private void matchUnmatchedNames() {
    try (SqlSession session = factory.openSession(false)) {
      AtomicInteger counter = new AtomicInteger();
      NameMapper nm = session.getMapper(NameMapper.class);
      nm.processUnmatched(datasetKey).forEach(n -> {
        NameMatch match = nameIndex.match(n, true, false);
        if (match.hasMatch()) {
          nm.updateMatch(datasetKey, n.getId(), match.getName().getKey(), match.getType());
          if (counter.getAndIncrement() % 1000 == 0) {
            session.commit();
          }
        }
      });
      session.commit();
    }
  }

  private void metrics() {
    LOG.info("Build import metrics for dataset " + datasetKey);
    diDao.updateMetrics(metrics, newDatasetKey);
    diDao.update(metrics);

    // update both the projects and release datasets import attempt pointer
    try (SqlSession session = factory.openSession(true)) {
      DatasetMapper dm = session.getMapper(DatasetMapper.class);
      dm.updateLastImport(datasetKey, metrics.getAttempt());
      dm.updateLastImport(newDatasetKey, metrics.getAttempt());
    }
  }

  public static void releaseDataset(Dataset d, DatasetSettings ds) {
    d.setOrigin(DatasetOrigin.RELEASED);
    final LocalDate today = LocalDate.now();
    d.setReleased(today);
    d.setVersion(today.toString());

    String alias = CitationUtils.fromTemplate(d, ds, Setting.RELEASE_ALIAS_TEMPLATE, DEFAULT_ALIAS_TEMPLATE);
    d.setAlias(alias);

    String title = CitationUtils.fromTemplate(d, ds, Setting.RELEASE_TITLE_TEMPLATE, DEFAULT_TITLE_TEMPLATE);
    d.setTitle(title);

    if (ds.has(Setting.RELEASE_CITATION_TEMPLATE)) {
      String citation = CitationUtils.fromTemplate(d, ds.getString(Setting.RELEASE_CITATION_TEMPLATE));
      d.setCitation(citation);
    }

    d.setPrivat(true); // all releases are private candidate releases first
  }

}
