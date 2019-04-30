package org.col.common.csl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.col.api.TestEntityGenerator;
import org.col.api.jackson.ApiModule;
import org.col.api.model.CslData;
import org.col.common.io.Resources;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CslUtilTest {

  @Test
  public void makeBibliography() {
    for (int i = 0; i < 10; i++) {
      System.out.println(CslUtil.buildCitation(TestEntityGenerator.newReference()));
    }
  }
  
  @Test
  public void buildCitation() throws IOException {
    InputStream in = Resources.stream("references/test.json");
    TypeReference<List<CslData>> cslType = new TypeReference<List<CslData>>(){};
    List<CslData> refs = ApiModule.MAPPER.readValue(in, cslType);
    
    assertEquals("Droege, G., Barker, K., Seberg, O., Coddington, J., Benson, E., Berendsohn, W. G., Bunk, B., et al. (2016). The Global Genome Biodiversity Network (GGBN) Data Standard specification. Database, 2016, baw125. doi:10.1093/database/baw125", CslUtil.buildCitation(refs.get(0)));
  }
}