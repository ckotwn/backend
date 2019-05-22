package org.col.matching;

import java.util.Objects;

import org.apache.ibatis.session.*;
import org.col.api.model.IssueContainer;
import org.col.api.model.Name;
import org.col.api.model.NameMatch;
import org.col.api.vocab.Issue;
import org.col.db.mapper.NameMapper;
import org.col.db.mapper.VerbatimRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetMatcher {
  private static final Logger LOG = LoggerFactory.getLogger(DatasetMatcher.class);
  private final SqlSessionFactory factory;
  private final NameIndex ni;
  
  public DatasetMatcher(SqlSessionFactory factory, NameIndex ni) {
    this.factory = factory;
    this.ni = ni;
  }
  
  /**
   * Matches all names of an entire dataset and updates its name index id and issues in postgres
   * @return number of names which have a changed match to before
   */
  public int match(int datasetKey) {
    try (SqlSession session = factory.openSession(false)){
      try (SqlSession batchSession = factory.openSession(ExecutorType.BATCH, false)){
        NameMapper nm = session.getMapper(NameMapper.class);
        BulkMatchHandler h = new BulkMatchHandler(ni, batchSession, datasetKey);
        nm.processDataset(datasetKey, h);
        batchSession.commit();
        LOG.info("Updated {} out of {} name matches for dataset {}", h.updates, h.counter, datasetKey);
        return h.updates;
      }
    }
  }
  
  
  static class BulkMatchHandler implements ResultHandler<Name> {
    int counter = 0;
    int updates = 0;
    private final int datasetKey;
    private final SqlSession session;
    private final NameIndex ni;
    private final NameMapper nm;
    private final VerbatimRecordMapper vm;
  
    BulkMatchHandler(NameIndex ni, SqlSession session, int datasetKey) {
      this.datasetKey = datasetKey;
      this.ni = ni;
      this.session = session;
      this.nm = session.getMapper(NameMapper.class);
      this.vm = session.getMapper(VerbatimRecordMapper.class);
    }
  
    @Override
    public void handleResult(ResultContext<? extends Name> ctx) {
      counter++;
      Name n = ctx.getResultObject();
      String oldId = n.getNameIndexId();
      NameMatch m = ni.match(n, true, false);
      
      if (!Objects.equals(oldId, m.hasMatch() ? m.getName().getId() : null)) {
        nm.updateMatch(datasetKey, n.getId(), m.hasMatch() ? m.getName().getId() : null);
        
        IssueContainer v = n.getVerbatimKey() != null ? vm.getIssues(datasetKey, n.getVerbatimKey()) : null;
        if (v != null) {
          int hash = v.getIssues().hashCode();
          clearMatchIssues(v);
          if (m.hasMatch()) {
            if (m.getType().issue != null) {
              v.addIssue(m.getType().issue);
            }
          } else {
            v.addIssue(Issue.NAME_MATCH_NONE);
          }
          // only update verbatim if issues changed
          if (hash != v.getIssues().hashCode()) {
            vm.update(datasetKey, n.getVerbatimKey(), v.getIssues());
          }
        }
        if (updates++ % 10000 == 0) {
          session.commit();
          LOG.debug("Updated {} out of {} name matches for dataset {}", updates, counter, datasetKey);
        }
      }
    }
    
    static void clearMatchIssues(IssueContainer issues){
      issues.removeIssue(Issue.NAME_MATCH_NONE);
      issues.removeIssue(Issue.NAME_MATCH_AMBIGUOUS);
      issues.removeIssue(Issue.NAME_MATCH_VARIANT);
      issues.removeIssue(Issue.NAME_MATCH_INSERTED);
    }
  }
}