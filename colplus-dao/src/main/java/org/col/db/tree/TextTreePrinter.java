package org.col.db.tree;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.col.api.model.Name;
import org.col.api.model.NameUsageBase;
import org.col.api.model.Sector;
import org.col.api.model.Taxon;
import org.col.db.mapper.NameUsageMapper;
import org.col.db.mapper.SectorMapper;

/**
 * Print an entire dataset in the indented text format used by TxtPrinter.
 * Synonyms are prefixed with an asterisk *,
 * Pro parte synoynms with a double asterisk **,
 * basionyms are prefixed by a $ and listed first in the synonymy.
 * <p>
 * Ranks are given in brackets after the scientific name
 * <p>
 * A basic example tree would look like this:
 * <pre>
 * Plantae [kingdom]
 * Compositae Giseke [family]
 * Asteraceae [family]
 * Artemisia L. [genus]
 * Artemisia elatior (Torr. & A. Gray) Rydb.
 * $Artemisia tilesii var. elatior Torr. & A. Gray
 * $Artemisia rupestre Schrank L. [species]
 * Absinthium rupestre (L.) Schrank [species]
 * Absinthium viridifolium var. rupestre (L.) Besser
 * </pre>
 */
public class TextTreePrinter implements ResultHandler<NameUsageBase> {
  public static final String SYNONYM_SYMBOL = "*";
  public static final String BASIONYM_SYMBOL = "$";
  
  private static final int indentation = 2;
  private int level = 0;
  private int counter = 0;
  private final Writer writer;
  private final int datasetKey;
  private final Integer sectorKey;
  private final String startID;
  private final SqlSessionFactory factory;
  private SqlSession session;
  private final LinkedList<NameUsageBase> parents = new LinkedList<>();
  
  /**
   * @param sectorKey optional sectorKey to restrict printed tree to
   */
  private TextTreePrinter(int datasetKey, Integer sectorKey, String startID, SqlSessionFactory factory, Writer writer) {
    this.datasetKey = datasetKey;
    this.startID = startID;
    this.sectorKey = sectorKey;
    this.factory = factory;
    this.writer = writer;
  }
  
  public static TextTreePrinter dataset(int datasetKey, SqlSessionFactory factory, Writer writer) {
    return new TextTreePrinter(datasetKey, null, null, factory, writer);
  }
  
  /**
   * Prints a sector from the given catalogue.
   */
  public static TextTreePrinter sector(int catalogueKey, final int sectorKey, SqlSessionFactory factory, Writer writer) {
    try (SqlSession session = factory.openSession(true)) {
      Sector s = session.getMapper(SectorMapper.class).get(sectorKey);
      return new TextTreePrinter(catalogueKey, sectorKey, s.getTarget().getId(), factory, writer);
    }
  }
  
  /**
   * @return number of written lines, i.e. name usages
   * @throws IOException
   */
  public int print() throws IOException {
    counter = 0;
    try {
      session = factory.openSession(true);
      NameUsageMapper num = session.getMapper(NameUsageMapper.class);
      num.processTree(datasetKey, sectorKey, startID, null, true, true, this);

    } finally {
      writer.flush();
      session.close();
    }
    return counter;
  }
  
  @Override
  public void handleResult(ResultContext<? extends NameUsageBase> resultContext) {
    try {
      NameUsageBase u = resultContext.getResultObject();
      // send end signals
      while (!parents.isEmpty() && !parents.peekLast().getId().equals(u.getParentId())) {
        end(parents.removeLast());
      }
      start(u);
      parents.add(u);
      
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void start(NameUsageBase u) throws IOException {
    counter++;
    Name n = u.getName();
    writer.write(StringUtils.repeat(' ', level * indentation));
    if (u.isSynonym()) {
      writer.write(SYNONYM_SYMBOL);
    }
    //TODO: flag basionyms
    writer.write(n.canonicalName());
    if (n.getRank() != null) {
      writer.write(" [");
      writer.write(n.getRank().name().toLowerCase());
      writer.write("]");
    }
    
    if (u.isTaxon()) {
      Taxon t = (Taxon) u;
      if (t.getSectorKey() != null) {
        writer.write(" (S");
        writer.write(t.getSectorKey().toString());
        writer.write(')');
      }
    }
    
    writer.write('\n');
    level++;
  }
  
  private void end(NameUsageBase u) {
    level--;
  }
  
}
