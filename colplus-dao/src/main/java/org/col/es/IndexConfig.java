package org.col.es;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.col.api.jackson.ApiModule;
import org.col.api.model.NameUsage;
import org.col.api.search.NameUsageWrapper;

public class IndexConfig {

  private static ObjectMapper simpleMapper;

  /**
   * The model class corresponding to the type.
   */
  public String modelClass;
  public int numShards = 1;
  public int numReplicas = 0;
  /**
   * Batch size for bulk requests
   */
  public int batchSize = 1000;
  /**
   * Whether to store enums as ints or as strings. Storings as ints squeezes a bit more performance
   * out of ES, but not much because cardinality will be low anyhow. And it saves space, notably in
   * the "source" field of EsNameUsage. On the other hand, it makes the index harder to read in
   * Kibana.
   */
  public Boolean storeEnumAsInt = Boolean.TRUE;

  private ObjectReader reader;
  private ObjectWriter writer;
  private ObjectWriter nameUsageWrapperWriter;
  private ObjectReader nameUsageWrapperReader;
  

  public ObjectMapper getMapper() {
    if (!storeEnumAsInt) {
      return ApiModule.MAPPER;
    }
    if (simpleMapper == null) {
      simpleMapper = new ObjectMapper();
      simpleMapper.setSerializationInclusion(Include.NON_NULL);
    }
    return simpleMapper;
  }


  /**
   * Returns an ObjectReader that deserializes ES documents into EsNameUsage instances.
   */
  public ObjectReader getObjectReader() {
    if (reader == null) {
      try {
        reader = getMapper().readerFor(Class.forName(modelClass));
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return reader;
  }

  /**
   * Returns an ObjectWriter that serializes EsNameUsage instances to ES documents.
   * 
   * @return
   */
  public ObjectWriter getObjectWriter() {
    if (writer == null) {
      try {
        writer = getMapper().writerFor(Class.forName(modelClass));
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return writer;
  }

  /**
   * Returns an ObjectWriter that writes NameUsageWrapper objects coming from Postgres/MyBatis to
   * the "source" field of the EsNameUsage class.
   * 
   * @return
   */
  public ObjectWriter getWriterForNameUsageWrapper() {
    if (nameUsageWrapperWriter == null) {
      nameUsageWrapperWriter =
          getMapper().writerFor(new TypeReference<NameUsageWrapper<? extends NameUsage>>() {});
    }
    return nameUsageWrapperWriter;
  }

}
