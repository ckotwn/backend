package life.catalogue.db.mapper;

import life.catalogue.api.model.SimpleNameClassification;
import life.catalogue.api.search.NameUsageWrapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Mapper dealing with methods returning the NameUsage interface, i.e. a name in the context of either a Taxon, TaxonVernacularUsage,
 * Synonym or BareName.
 * <p>
 * Mapper sql should be reusing sql fragments from the 3 concrete implementations as much as possible avoiding duplication.
 */
public interface NameUsageWrapperMapper {
  
  NameUsageWrapper get(@Param("datasetKey") int datasetKey, @Param("id") String taxonId);

  /**
   * Get bare name by its name id. If a usage with the name exists no bare name can be retrieved!
   * @return a wrapped bare name selected by its name id or null
   */
  NameUsageWrapper getBareName(@Param("datasetKey") int datasetKey, @Param("id") String nameId);

  /**
   * @return the full wrapper object but without the recursive classification property
   */
  NameUsageWrapper getWithoutClassification(@Param("datasetKey") int datasetKey,
                                            @Param("id") String taxonId);

  List<NameUsageWrapper> getSomeWithoutClassification(@Param("datasetKey") int datasetKey,
                                                @Param("ids") List<String> taxonIds);

  /**
   * Iterates over all usages for a given dataset.
   * The returned wrapper does only include the usage and issues related to name and usage, but no further information.
   */
  Cursor<NameUsageWrapper> processDatasetUsageWithIssues(@Param("datasetKey") int datasetKey);
  /**
   * Iterates over all bare names not linked to a synonym or taxon for a given dataset. This
   * allows a single query to efficiently stream all its values without keeping them in memory.
   */
  Cursor<NameUsageWrapper> processDatasetBareNames(@Param("datasetKey") Integer datasetKey,
                               @Nullable @Param("sectorKey") Integer sectorKey);


  /**
   * Traverses a subtree returning classifications as list of simple names objects only.
   * The first SimpleName in each classification list represents the usage being processed.
   *
   * @param datasetKey the dataset, e.g. catalogue, to process
   * @param sectorKey the optional sector to restrict the processed usages to
   * @param usageId the root usage to start processing the tree. Will be included in the processed result
   */
  Cursor<SimpleNameClassification> processTree(@Param("datasetKey") Integer datasetKey,
                      @Nullable @Param("sectorKey") Integer sectorKey,
                      @Param("usageId") String usageId);

}
