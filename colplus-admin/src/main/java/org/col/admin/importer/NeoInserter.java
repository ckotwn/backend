package org.col.admin.importer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.col.admin.importer.neo.NeoDb;
import org.col.admin.importer.neo.model.Labels;
import org.col.admin.importer.neo.model.NeoNameRel;
import org.col.admin.importer.neo.model.NeoTaxon;
import org.col.admin.importer.reference.ReferenceFactory;
import org.col.api.model.Dataset;
import org.col.api.model.TermRecord;
import org.col.api.model.VerbatimEntity;
import org.col.api.vocab.Issue;
import org.col.csv.CsvReader;
import org.gbif.dwc.terms.Term;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public abstract class NeoInserter {
  private static final Logger LOG = LoggerFactory.getLogger(NeoInserter.class);

  protected final NeoDb store;
  protected final Path folder;
  protected final InsertMetadata meta = new InsertMetadata();
  protected final ReferenceFactory refFactory;
  private int vcounter;

  public NeoInserter(Path folder, NeoDb store, ReferenceFactory refFactory) {
    this.folder = folder;
    this.store = store;
    this.refFactory = refFactory;
  }

  final InsertMetadata insertAll() throws NormalizationFailedException {
    // the key will be preserved by the store
    Optional<Dataset> d = readMetadata();
    d.ifPresent(store::put);

    store.startBatchMode();
    batchInsert();
    LOG.info("Batch insert completed, {} verbatim records processed, {} nodes created", vcounter, store.size());

    store.endBatchMode();
    LOG.info("Neo batch inserter closed, data flushed to disk");

    final int batchV = vcounter;
    final int batchRec = store.size();
    postBatchInsert();
    LOG.info("Post batch insert completed, {} verbatim records processed creating {} new nodes", batchV, store.size()-batchRec);

    LOG.debug("Start processing explicit relations ...");
    store.process(Labels.ALL,10000, relationProcessor());

    LOG.info("Insert of {} verbatim records and {} nodes completed", vcounter, store.size());
    return meta;
  }

  private void processVerbatim(final CsvReader reader, final Term classTerm, Function<TermRecord, Boolean> proc) {
    if (Thread.interrupted()) {
      LOG.warn("NeoInserter interrupted, exit early with incomplete import");
      throw new NormalizationFailedException("NeoInserter interrupted");
    }
    final AtomicInteger counter = new AtomicInteger(0);
    final AtomicInteger success = new AtomicInteger(0);
    reader.stream(classTerm).forEach(rec -> {
      store.assignKey(rec);
      if (proc.apply(rec)) {
        success.incrementAndGet();
      } else {
        rec.addIssue(Issue.NOT_INTERPRETED);
      }
      store.put(rec);
      counter.incrementAndGet();
    });
    LOG.info("Inserted {} verbatim, {} successfully processed {}", counter.get(), success.get(), classTerm.prefixedName());
    vcounter += counter.get();
  }

  protected <T extends VerbatimEntity> void insertEntities(final CsvReader reader, final Term classTerm,
                                                           Function<TermRecord, Optional<T>> interpret,
                                                           Consumer<T> add
  ) {
    processVerbatim(reader, classTerm, rec -> {
      Optional<T> opt = interpret.apply(rec);
      if (opt.isPresent()) {
        T obj = opt.get();
        obj.setVerbatimKey(rec.getKey());
        add.accept(obj);
        return true;
      }
      return false;
    });
  }

  protected <T extends VerbatimEntity> void insertTaxonEntities(final CsvReader reader, final Term classTerm,
                                                                Function<TermRecord, List<T>> interpret,
                                                                Term taxonIdTerm,
                                                                BiConsumer<NeoTaxon, T> add
  ) {
    processVerbatim(reader, classTerm, rec -> {
      List<T> results = interpret.apply(rec);
      if (reader.isEmpty()) return false;
      boolean interpreted = true;
      for (T obj : results) {
        obj.setVerbatimKey(rec.getKey());
        String id = rec.getRaw(taxonIdTerm);
        NeoTaxon t = store.getByID(id);
        if (t != null) {
          add.accept(t, obj);
          store.update(t);
        } else {
          interpreted = false;
          LOG.warn("Non existing {} {} found in {} record line {}, {}", taxonIdTerm.simpleName(), id, rec.getType().simpleName(), rec.getLine(), rec.getFile());
        }
      }
      return interpreted;
    });
  }

  protected void insertNameRelations(final CsvReader reader, final Term classTerm,
                                                                Function<TermRecord, Optional<NeoNameRel>> interpret,
                                                                Term nameIdTerm, Term relatedNameIdTerm
  ) {
    processVerbatim(reader, classTerm, rec -> {
      Optional<NeoNameRel> opt = interpret.apply(rec);
      if (opt.isPresent()) {
        Node n1 = store.byID(rec.getRaw(nameIdTerm));
        Node n2 = store.byID(rec.getRaw(relatedNameIdTerm));
        store.createNameRel(n1, n2, opt.get());
        return true;
      }
      return false;
    });
  }

  public abstract void batchInsert() throws NormalizationFailedException;

  public abstract void postBatchInsert() throws NormalizationFailedException;

  protected abstract NeoDb.NodeBatchProcessor relationProcessor();

  protected abstract Optional<Dataset> readMetadata();

}