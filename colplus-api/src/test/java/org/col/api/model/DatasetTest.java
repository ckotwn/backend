package org.col.api.model;

import java.net.URI;

import org.col.api.jackson.ApiModule;
import org.col.api.jackson.SerdeTestBase;
import org.col.api.vocab.DatasetOrigin;
import org.col.api.vocab.DatasetType;
import org.col.api.vocab.Frequency;
import org.col.api.vocab.License;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 *
 */
public class DatasetTest extends SerdeTestBase<Dataset> {
  
  public DatasetTest() {
    super(Dataset.class);
  }
  
  @Override
  public Dataset genTestValue() throws Exception {
    Dataset d = new Dataset();
    d.setKey(12345);
    d.setTitle("gfdscdscw");
    d.setDescription("gefzw fuewh gczew fw hfueh j ijdfeiw jfie eö.. few . few .");
    d.setOrigin(DatasetOrigin.UPLOADED);
    d.setType(DatasetType.GLOBAL);
    d.setImportFrequency(Frequency.MONTHLY);
    d.setDataAccess(URI.create("www.gbif.org"));
    d.setWebsite(URI.create("www.gbif.org"));
    d.setLogo(URI.create("www.gbif.org"));
    d.setLicense(License.CC0);
    d.setCitation("cf5twv867cwcgewcwe");
    d.setContact("Me");
    d.getOrganisations().add("bla");
    d.getOrganisations().add("bla");
    d.getOrganisations().add("bla");
    d.setContact("foo");
    d.setNotes("cuzdsghazugbe67wqt6c g cuzdsghazugbe67wqt6c g  nhjs");
    return d;
  }
  
  @Test
  public void testEmptyString() throws Exception {
    String json = ApiModule.MAPPER.writeValueAsString(genTestValue());
    json = json.replaceAll("www\\.gbif\\.org", "");
    json = json.replaceAll("cc0", "");
    
    Dataset d = ApiModule.MAPPER.readValue(json, Dataset.class);
    assertNull(d.getWebsite());
    assertNull(d.getDataAccess());
    assertNull(d.getLogo());
    assertNull(d.getLicense());
  }

  @Override
  protected void debug(String json, Wrapper<Dataset> wrapper, Wrapper<Dataset> wrapper2) {
    System.out.println(json);
  }
}