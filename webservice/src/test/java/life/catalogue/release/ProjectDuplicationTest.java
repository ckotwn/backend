package life.catalogue.release;

import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import life.catalogue.WsServerConfig;
import life.catalogue.api.model.Dataset;
import life.catalogue.api.vocab.ImportState;
import life.catalogue.api.vocab.Users;
import life.catalogue.dao.DatasetDao;
import life.catalogue.dao.DatasetImportDao;
import life.catalogue.dao.TreeRepoRule;
import life.catalogue.db.PgSetupRule;
import life.catalogue.db.TestDataRule;
import life.catalogue.db.mapper.DatasetMapper;
import life.catalogue.es.NameUsageIndexService;
import life.catalogue.img.ImageService;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ProjectDuplicationTest {


  @ClassRule
  public static PgSetupRule pgSetupRule = new PgSetupRule();

  @Rule
  public final TreeRepoRule treeRepoRule = new TreeRepoRule();

  @Rule
  public TestDataRule testDataRule = TestDataRule.apple();

  WsServerConfig cfg;
  DatasetImportDao diDao;
  DatasetDao dDao;
  Dataset d;

  @Before
  public void init()  {
    cfg = new WsServerConfig();
    cfg.db = PgSetupRule.getCfg();
    cfg.exportDir = Files.createTempDir();
    cfg.normalizer.scratchDir  = Files.createTempDir();
    diDao = new DatasetImportDao(PgSetupRule.getSqlSessionFactory(), treeRepoRule.getRepo());
    EventBus bus = mock(EventBus.class);
    dDao = new DatasetDao(PgSetupRule.getSqlSessionFactory(), null, ImageService.passThru(), diDao, NameUsageIndexService.passThru(), null, bus);

    // dataset needs to be a managed one
    try (SqlSession s = PgSetupRule.getSqlSessionFactory().openSession()) {
      DatasetMapper dm = s.getMapper(DatasetMapper.class);
      d = dm.get(TestDataRule.APPLE.key);
    }
  }

  @Test
  public void copy() throws Exception {
    ProjectDuplication dupl = ReleaseManager.duplicate(PgSetupRule.getSqlSessionFactory(), NameUsageIndexService.passThru(), diDao, dDao, d.getKey(), Users.TESTER);
    dupl.run();
    assertEquals(ImportState.FINISHED, dupl.getMetrics().getState());
  }

}