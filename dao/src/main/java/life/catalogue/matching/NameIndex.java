package life.catalogue.matching;

import life.catalogue.api.model.IndexName;
import life.catalogue.api.model.Name;
import life.catalogue.api.model.NameMatch;
import life.catalogue.common.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public interface NameIndex extends Managed, AutoCloseable {
  
  Logger LOG = LoggerFactory.getLogger(NameIndex.class);
  
  /**
   * Tries to match a parsed name against the names index.
   *
   * @param name         the parsed name to match against, ignoring any ids if present
   * @param allowInserts if true inserts the name to be matched into the index if not yet existing, avoiding NoMatch responses
   * @param verbose      if true adds verbose matching information, i.e. queue of alternative matches
   * @return a match which is never null, but might have a usageKey=null if nothing could be matched
   */
  NameMatch match(Name name, boolean allowInserts, boolean verbose);

  /**
   * Lookup IndexName by its key
   */
  IndexName get(Integer key);

  Iterable<IndexName> all();

  /**
   * @return the number of names in the index
   */
  int size();
  
  /**
   * Adds a name to the index, generating a new key and potentially inserting a canonical name record too.
   *
   * @param name
   */
  void add(IndexName name);
  
  /**
   * Adds a batch of names to the index
   */
  default void addAll(Collection<IndexName> names) {
    LOG.info("Adding {} names", names.size());
    for (IndexName n : names) {
      add(n);
    }
  }

  /**
   * Resets the names index, removing all entries and setting back the id sequence to 1.
   * This does truncate both the file based index as well as the underlying postgres data.
   */
  void reset();

  /**
   * @return true if started and ready to be queried
   */
  boolean hasStarted();

  /**
   * Makes sure the names index has started and throws an NamesIndexOfflineException otherwise
   */
  default NameIndex assertOnline() {
    if (!hasStarted()) {
      throw new NamesIndexOfflineException();
    }
    return this;
  }

  @Override
  default void close() throws Exception {
    stop();
  }

  class NamesIndexOfflineException extends IllegalStateException {

    public NamesIndexOfflineException() {
      super("Names Index is offline");
    }
  }
}
