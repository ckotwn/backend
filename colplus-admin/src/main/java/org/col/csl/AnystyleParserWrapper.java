package org.col.csl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.col.api.model.CslData;
import org.col.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.dropwizard.lifecycle.Managed;

public class AnystyleParserWrapper implements Managed, AutoCloseable, Parser<CslData> {
  private static final Logger LOG = LoggerFactory.getLogger(AnystyleWebService.class);

  private static final TypeReference<List<Map<String, Object>>> ANYSTYLE_RESPONSE_TYPE =
      new TypeReference<List<Map<String, Object>>>() {};

  private final AnystyleWebService svc;
  private final CloseableHttpClient hc;
  private final ObjectMapper om;

  public AnystyleParserWrapper(CloseableHttpClient hc) {
    this.svc = new AnystyleWebService();
    this.hc = hc;
    this.om = new ObjectMapper();
  }

  public Optional<CslData> parse(String ref) {
    if (Strings.isNullOrEmpty(ref)) {
      return Optional.empty();
    }
    try (CloseableHttpResponse response = hc.execute(request(ref))) {
      InputStream in = response.getEntity().getContent();
      List<Map<String, Object>> raw = om.readValue(in, ANYSTYLE_RESPONSE_TYPE);
      if (raw.size() != 1) {
        LOG.error("Anystyle result is list of size {}", raw.size());
        throw new RuntimeException("Unexpected response from Anystyle");
      }
      Map<String, Object> map = raw.get(0);
      for (String key : new ArrayList<>(map.keySet())) {
        if (key.indexOf('-') != -1) {
          map.put(toCamelCase(key), map.remove(key));
        }
      }
      return Optional.of(om.convertValue(map, CslData.class));
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public synchronized void start() throws Exception {
    svc.start();
  }

  @Override
  public synchronized void stop() throws Exception {
    svc.stop();
  }

  @Override
  public void close() throws Exception {
    stop();
  }

  private static HttpGet request(String reference) throws URISyntaxException {
    URIBuilder ub = new URIBuilder();
    ub.setScheme("http");
    ub.setHost("localhost");
    ub.setPort(AnystyleWebService.HTTP_PORT);
    ub.setParameter(AnystyleWebService.QUERY_PARAM_REF, reference);
    return new HttpGet(ub.build());
  }

  private static String toCamelCase(String key) {
    StringBuilder sb = new StringBuilder(key.length());
    boolean hyphen = false;
    for (int i = 0; i < key.length(); i++) {
      if (key.charAt(i) == '-') {
        hyphen = true;
      } else {
        if (hyphen) {
          sb.append(Character.toUpperCase(key.charAt(i)));
          hyphen = false;
        } else {
          sb.append(key.charAt(i));
        }
      }
    }
    return sb.toString();
  }

}