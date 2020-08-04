package life.catalogue.dao;

import life.catalogue.api.exception.NotFoundException;
import life.catalogue.api.model.DSID;
import life.catalogue.common.io.UTF8IoUtils;
import life.catalogue.db.mapper.NameMapper;
import life.catalogue.db.tree.TextTreePrinter;
import org.apache.commons.io.FileUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DAO giving read and write access to potentially large text trees and name lists
 * stored on the filesystem. We use compression to keep storage small.
 */
public abstract class FileMetricsDao<K> {
  private static final Logger LOG = LoggerFactory.getLogger(FileMetricsDao.class);

  protected final SqlSessionFactory factory;
  protected final File repo;
  protected final String type;

  public FileMetricsDao(String type, SqlSessionFactory factory, File repo) {
    this.type = type;
    this.factory = factory;
    this.repo = repo;
  }
  
  public static Set<String> readLines(File f) throws IOException{
    try (BufferedReader br = UTF8IoUtils.readerFromFile(f)) {
      return br.lines().collect(Collectors.toSet());
    }
  }
  
  public void updateNames(K key, int attempt) {
    try (SqlSession session = factory.openSession(true);
        NamesWriter nHandler = new NamesWriter(namesFile(key, attempt));
        NamesIdWriter idHandler = new NamesIdWriter(namesIdFile(key, attempt))
    ){
      NameMapper nm = session.getMapper(NameMapper.class);

      DSID<Integer> skey = sectorKey(key);
      nm.processNameStrings(skey.getDatasetKey(), skey.getId()).forEach(nHandler);
      LOG.info("Written {} name strings for {} {}-{}", nHandler.counter, type, key, attempt);

      nm.processIndexIds(skey.getDatasetKey(), skey.getId()).forEach(idHandler);
      LOG.info("Written {} names index ids for {} {}-{}", idHandler.counter, type, key, attempt);
    }
  }

  public String getType() {
    return type;
  }

  static class NamesWriter implements Consumer<String>, AutoCloseable {
    public int counter = 0;
    private final File f;
    private final BufferedWriter w;
    
    NamesWriter(File f) {
      this.f=f;
      try {
        w = UTF8IoUtils.writerFromGzipFile(f);
        
      } catch (IOException e) {
        LOG.error("Failed to write to {}", f.getAbsolutePath());
        throw new RuntimeException(e);
      }
    }
  
    @Override
    public void close() {
      try {
        w.close();
      } catch (IOException e) {
        LOG.error("Failed to close {}", f.getAbsolutePath());
      }
    }
  
    @Override
    public void accept(String id) {
      try {
        if (id != null) {
          counter++;
          w.append(id);
          w.append('\n');
        }
      } catch (IOException e) {
        LOG.error("Failed to write to {}", f.getAbsolutePath());
        throw new RuntimeException(e);
      }
    }
  }

  static class NamesIdWriter implements Consumer<Integer>, AutoCloseable {
    public int counter = 0;
    private final File f;
    private final BufferedWriter w;

    NamesIdWriter(File f) {
      this.f=f;
      try {
        w = UTF8IoUtils.writerFromGzipFile(f);

      } catch (IOException e) {
        LOG.error("Failed to write to {}", f.getAbsolutePath());
        throw new RuntimeException(e);
      }
    }

    @Override
    public void close() {
      try {
        w.close();
      } catch (IOException e) {
        LOG.error("Failed to close {}", f.getAbsolutePath());
      }
    }

    @Override
    public void accept(Integer id) {
      try {
        if (id != null) {
          counter++;
          w.append(id.toString());
          w.append('\n');
        }
      } catch (IOException e) {
        LOG.error("Failed to write to {}", f.getAbsolutePath());
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Deletes all metrics stored for the given key, incl tree and name index sets.
   */
  public void deleteAll(K key) throws IOException {
    File dir = subdir(key);
    FileUtils.deleteDirectory(dir);
    LOG.info("Deleted all file metrics for {} {}", type, key);
  }

  public int updateTree(K key, int attempt) throws IOException {
    try (Writer writer = UTF8IoUtils.writerFromGzipFile(treeFile(key, attempt))) {
      TextTreePrinter ttp = ttPrinter(key, factory, writer);
      int count = ttp.print();
      LOG.info("Written text tree with {} lines for {} {}-{}", count, type, key, attempt);
      return count;
    }
  }

  abstract TextTreePrinter ttPrinter(K key, SqlSessionFactory factory, Writer writer);

  public Stream<String> getNames(K key, int attempt) {
    return streamFile(namesFile(key, attempt), key, attempt);
  }

  public Stream<String> getNameIds(K key, int attempt) {
    return streamFile(namesIdFile(key, attempt), key, attempt);
  }

  public Stream<String> getTree(K key, int attempt) {
    return streamFile(treeFile(key, attempt), key, attempt);
  }

  private Stream<String> streamFile(File f, K key, int attempt) {
    try {
      BufferedReader br = UTF8IoUtils.readerFromGzipFile(f);
      return br.lines();

    } catch (FileNotFoundException e) {
      throw new AttemptMissingException(type, key, attempt, e);

    } catch (IOException e) {
      throw new RuntimeException("Failed to stream file " + f.getAbsolutePath(), e);
    }
  }

  public static class AttemptMissingException extends NotFoundException {
    public final int attempt;

    public AttemptMissingException(String type, Object key, int attempt) {
      super(key, buildMessage(type, key, attempt));
      this.attempt = attempt;
    }

    public AttemptMissingException(String type, Object key, int attempt, IOException cause) {
      super(key, buildMessage(type, key, attempt), cause);
      this.attempt = attempt;
    }

    private static String buildMessage(String type, Object key, int attempt) {
      return String.format("Import attempt %s for %s %s missing", attempt, type, key);
    }
  }

  public File treeFile(K key, int attempt) {
    return new File(subdir(key), "tree/"+attempt+".txt.gz");
  }

  public File namesFile(K key, int attempt) {
    return new File(subdir(key), "names/"+attempt+"-strings.txt.gz");
  }

  public File namesIdFile(K key, int attempt) {
    return new File(subdir(key), "names/"+attempt+".txt.gz");
  }

  abstract File subdir(K key);

  abstract DSID<Integer> sectorKey(K key);
}