package org.col.es;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.col.api.TestEntityGenerator;
import org.col.api.search.NameUsageWrapper;
import org.col.api.vocab.NameField;
import org.col.api.vocab.TaxonomicStatus;
import org.col.es.model.EsNameUsage;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.Rank;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/*
 * Also separately tests serde for payload field, which contains the serialized NameUsageWrapper
 * object. NB Can't extend SerdeTestBase b/c it's specifically about (de)serialization to ES
 * documents, which uses another ObjectMapper.
 */
public class EsNameUsageSerde {

  static Logger LOG = LoggerFactory.getLogger(EsNameUsageSerde.class);
  
  static final ObjectReader PAYLOAD_READER = EsModule.NAME_USAGE_READER;
  static final ObjectWriter PAYLOAD_WRITER = EsModule.NAME_USAGE_WRITER;

  static EsConfig config1;
  static EsConfig config2;

  static ObjectWriter esWriter;
  static ObjectReader esReader;

  @BeforeClass
  public static void init() {
    config1 = new EsConfig();
    IndexConfig ic = new IndexConfig();
    ic.modelClass = EsNameUsage.class.getName();
    config1.nameUsage = ic;

    config2 = new EsConfig();
    ic = new IndexConfig();
    ic.modelClass = EsNameUsage.class.getName();
    config2.nameUsage = ic;
  }

  @Test
  public void testTaxon1() throws IOException {
    NameUsageWrapper<?> nuwIn = TestEntityGenerator.newNameUsageTaxonWrapper();
    String json = PAYLOAD_WRITER.writeValueAsString(nuwIn);
    LOG.debug(json);
    NameUsageWrapper<?> nuwOut = PAYLOAD_READER.readValue(json);
    assertEquals(nuwIn, nuwOut);
  }

  @Test
  public void testTaxon2() throws IOException {
    NameUsageWrapper<?> nuwIn = TestEntityGenerator.newNameUsageTaxonWrapper();
    String json = PAYLOAD_WRITER.writeValueAsString(nuwIn);
    LOG.debug(json);
    NameUsageWrapper<?> nuwOut = PAYLOAD_READER.readValue(json);
    assertEquals(nuwIn, nuwOut);
  }

  @Test
  public void testSynonym1() throws IOException {
    NameUsageWrapper<?> nuwIn = TestEntityGenerator.newNameUsageSynonymWrapper();
    String json = PAYLOAD_WRITER.writeValueAsString(nuwIn);
    LOG.debug(json);
    NameUsageWrapper<?> nuwOut = PAYLOAD_READER.readValue(json);
    assertEquals(nuwIn, nuwOut);
  }

  @Test
  public void testSynonym2() throws IOException {
    NameUsageWrapper<?> nuwIn = TestEntityGenerator.newNameUsageSynonymWrapper();
    String json = PAYLOAD_WRITER.writeValueAsString(nuwIn);
    LOG.debug(json);
    NameUsageWrapper<?> nuwOut = PAYLOAD_READER.readValue(json);
    assertEquals(nuwIn, nuwOut);
  }

  @Test
  public void testBareName1() throws IOException {
    NameUsageWrapper<?> nuwIn = TestEntityGenerator.newNameUsageBareNameWrapper();
    String json = PAYLOAD_WRITER.writeValueAsString(nuwIn);
    LOG.debug(json);
    NameUsageWrapper<?> nuwOut = PAYLOAD_READER.readValue(json);
    assertEquals(nuwIn, nuwOut);
  }

  @Test
  public void testBareName2() throws IOException {
    NameUsageWrapper<?> nuwIn = TestEntityGenerator.newNameUsageBareNameWrapper();
    String json = PAYLOAD_WRITER.writeValueAsString(nuwIn);
    LOG.debug(json);
    NameUsageWrapper<?> nuwOut = PAYLOAD_READER.readValue(json);
    assertEquals(nuwIn, nuwOut);
  }

  @Test
  public void testEsNameUsage1() throws IOException {
    esWriter = config1.nameUsage.getObjectWriter();
    esReader = config1.nameUsage.getObjectReader();

    EsNameUsage enuIn = new EsNameUsage();
    enuIn.setPayload(PAYLOAD_WRITER.writeValueAsString(TestEntityGenerator.newNameUsageTaxonWrapper()));
    enuIn.setAuthorship("John Smith");
    enuIn.setDatasetKey(472);
    enuIn.setNameFields(EnumSet.of(NameField.COMBINATION_EX_AUTHORS, NameField.UNINOMIAL));
    enuIn.setNameId("16");
    enuIn.setNameIndexId("afd56770af");
    enuIn.setPublishedInId("AMO333");
    enuIn.setRank(Rank.SPECIES);
    enuIn.setScientificName("Malus Sylvestris");
    enuIn.setStatus(TaxonomicStatus.ACCEPTED);
    enuIn.setType(NameType.SCIENTIFIC);
    enuIn.setVernacularNames(Arrays.asList("Apple tree"));

    String json = esWriter.writeValueAsString(enuIn);
    LOG.debug(json);

    EsNameUsage enuOut = esReader.readValue(json);
    assertEquals(enuIn, enuOut);

    NameUsageWrapper<?> nuw = PAYLOAD_READER.readValue(enuOut.getPayload());
    assertEquals(TestEntityGenerator.newNameUsageTaxonWrapper(), nuw);
  }

  @Test
  public void testEsNameUsage2() throws IOException {
    esWriter = config2.nameUsage.getObjectWriter();
    esReader = config2.nameUsage.getObjectReader();

    EsNameUsage enuIn = new EsNameUsage();
    enuIn.setPayload(
        PAYLOAD_WRITER.writeValueAsString(TestEntityGenerator.newNameUsageTaxonWrapper()));
    enuIn.setAuthorship("John Smith");
    enuIn.setDatasetKey(472);
    enuIn.setNameFields(EnumSet.of(NameField.COMBINATION_EX_AUTHORS, NameField.UNINOMIAL));
    enuIn.setNameId("16");
    enuIn.setNameIndexId("afd56770af");
    enuIn.setPublishedInId("AMO333");
    enuIn.setRank(Rank.SPECIES);
    enuIn.setScientificName("Malus Sylvestris");
    enuIn.setStatus(TaxonomicStatus.ACCEPTED);
    enuIn.setType(NameType.SCIENTIFIC);
    enuIn.setVernacularNames(Arrays.asList("Apple tree"));

    String json = esWriter.writeValueAsString(enuIn);
    LOG.debug(json);

    EsNameUsage enuOut = esReader.readValue(json);
    assertEquals(enuIn, enuOut);

    NameUsageWrapper<?> nuw = PAYLOAD_READER.readValue(enuOut.getPayload());
    assertEquals(TestEntityGenerator.newNameUsageTaxonWrapper(), nuw);
  }
}