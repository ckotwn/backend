package org.col.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.col.api.model.Distribution;
import org.col.api.model.IntKey;

public interface TaxonExtensionMapper<T extends IntKey> {

	List<T> listByTaxon(@Param("datasetKey") int datasetKey, @Param("taxonId") String taxonId);

	T get(@Param("datasetKey") int datasetKey, @Param("key") int key);

	void create(@Param("obj") T object,
              @Param("taxonId") String taxonId,
              @Param("datasetKey") int datasetKey);

}