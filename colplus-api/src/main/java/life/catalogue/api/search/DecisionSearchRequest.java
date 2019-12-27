package life.catalogue.api.search;

import life.catalogue.api.model.EditorialDecision;
import org.gbif.nameparser.api.Rank;

import javax.ws.rs.QueryParam;
import java.util.Objects;

public class DecisionSearchRequest {
  
  @QueryParam("id")
  private String id;

  @QueryParam("datasetKey")
  private Integer datasetKey;
  
  @QueryParam("subjectDatasetKey")
  private Integer subjectDatasetKey;
  
  @QueryParam("rank")
  private Rank rank;
  
  @QueryParam("mode")
  private EditorialDecision.Mode mode;

  @QueryParam("userKey")
  private Integer userKey;
  
  @QueryParam("broken")
  private boolean broken = false;
  
  public static DecisionSearchRequest byCatalogue(int datasetKey){
    DecisionSearchRequest req = new DecisionSearchRequest();
    req.datasetKey = datasetKey;
    return req;
  }

  public static DecisionSearchRequest byDataset(int subjectDatasetKey){
    DecisionSearchRequest req = new DecisionSearchRequest();
    req.subjectDatasetKey = subjectDatasetKey;
    return req;
  }

  public static DecisionSearchRequest byDataset(int datasetKey, int subjectDatasetKey){
    DecisionSearchRequest req = byCatalogue(datasetKey);
    req.subjectDatasetKey = subjectDatasetKey;
    return req;
  }

  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public Integer getDatasetKey() {
    return datasetKey;
  }
  
  public void setDatasetKey(Integer datasetKey) {
    this.datasetKey = datasetKey;
  }
  
  public Integer getSubjectDatasetKey() {
    return subjectDatasetKey;
  }
  
  public void setSubjectDatasetKey(Integer subjectDatasetKey) {
    this.subjectDatasetKey = subjectDatasetKey;
  }
  
  public EditorialDecision.Mode getMode() {
    return mode;
  }
  
  public void setMode(EditorialDecision.Mode mode) {
    this.mode = mode;
  }
  
  public Rank getRank() {
    return rank;
  }
  
  public void setRank(Rank rank) {
    this.rank = rank;
  }
  
  public Integer getUserKey() {
    return userKey;
  }
  
  public void setUserKey(Integer userKey) {
    this.userKey = userKey;
  }
  
  public boolean isBroken() {
    return broken;
  }
  
  public void setBroken(boolean broken) {
    this.broken = broken;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DecisionSearchRequest that = (DecisionSearchRequest) o;
    return broken == that.broken &&
        Objects.equals(id, that.id) &&
        Objects.equals(datasetKey, that.datasetKey) &&
        Objects.equals(subjectDatasetKey, that.subjectDatasetKey) &&
        rank == that.rank &&
        mode == that.mode &&
        Objects.equals(userKey, that.userKey);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id, datasetKey, subjectDatasetKey, rank, mode, userKey, broken);
  }
}
