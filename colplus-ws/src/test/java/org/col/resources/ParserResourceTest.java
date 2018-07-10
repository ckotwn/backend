package org.col.resources;

import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.col.WsServer;
import org.col.WsServerConfig;
import org.col.api.model.Name;
import org.col.api.model.NameAccordingTo;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.Rank;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParserResourceTest {

  @ClassRule
  public static final DropwizardAppRule<WsServerConfig> RULE =
      new DropwizardAppRule<>(WsServer.class, ResourceHelpers.resourceFilePath("config-test.yaml"));

  GenericType<List<NameAccordingTo>> PARSER_TYPE = new GenericType<List<NameAccordingTo>>(){};

  @Test
  public void parseGet() {
    Client client = new JerseyClientBuilder(RULE.getEnvironment()).build("test client");

    List<NameAccordingTo> resp = client.target(
        String.format("http://localhost:%d/parser/name", RULE.getLocalPort()))
        .queryParam("name", "Abies alba Mill.")
        .request()
        .get(PARSER_TYPE);

    Name abies = new Name();
    abies.setGenus("Abies");
    abies.setSpecificEpithet("alba");
    abies.getCombinationAuthorship().getAuthors().add("Mill.");
    abies.setType(NameType.SCIENTIFIC);
    abies.setRank(Rank.SPECIES);
    abies.updateScientificName();

    assertEquals(1, resp.size());
    assertEquals(abies, resp.get(0).getName());
  }
}