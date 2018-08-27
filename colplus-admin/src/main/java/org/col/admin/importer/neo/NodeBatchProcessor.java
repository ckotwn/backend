package org.col.admin.importer.neo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.col.admin.importer.ImportJob;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface NodeBatchProcessor {

  void process(Node n);

  /**
   * Indicates whether the batch should be committed or not
   * @param counter the total record counter of processed records at this point
   * @return true if the batch should be committed.
   */
  void commitBatch(int counter) throws InterruptedException;


  class BatchConsumer implements Runnable {
    public static final List<Node> POISON_PILL = new ArrayList<>();
    private static final Logger LOG = LoggerFactory.getLogger(BatchConsumer.class);
    private final int datasetKey;
    private final GraphDatabaseService neo;
    private final NodeBatchProcessor callback;
    private final BlockingQueue<List<Node>> queue;
    private int batchCounter = 0;
    private int recordCounter = 0;
    private RuntimeException error = null;
    private final Thread parentThread;

    BatchConsumer(int datasetKey, GraphDatabaseService neo, NodeBatchProcessor callback, BlockingQueue<List<Node>> queue, Thread parentThread) {
      this.datasetKey = datasetKey;
      this.neo = neo;
      this.callback = callback;
      this.queue = queue;
      this.parentThread = parentThread;
    }

    @Override
    public void run() {
      while(!(Thread.currentThread().interrupted())) {
        try {
          final List<Node> batch = queue.take();
          if (batch == POISON_PILL) {
            break;
          }

          batchCounter++;
          try (Transaction tx = neo.beginTx()) {
            ImportJob.setMDC(datasetKey);
            LOG.debug("Start new neo processing batch {} with {} nodes, first={}", batchCounter, batch.size(), batch.get(0));
            for (Node n : batch) {
              callback.process(n);
              recordCounter++;
            }
            tx.success();
            callback.commitBatch(recordCounter);

          } finally {
            ImportJob.removeMDC();
          }

        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();  // set interrupt flag back
          LOG.warn("Interrupted batch consumer for {}, skip remaining {} batches", callback, queue.size(), ex);
          break;

        } catch (RuntimeException e) {
          die(e);
          break;

        } catch (Throwable e) {
          die(new RuntimeException(e));
          break;
        }
      }
      LOG.debug("Batch processor stopped for {}", callback);
      queue.clear();
    }

    void die(RuntimeException e) {
      error = e;
      LOG.error("Error processing batch {} for {}, skip remaining {} batches", batchCounter, callback, queue.size(), e);
      parentThread.interrupt();
    }

    synchronized public RuntimeException getError() {
      return error;
    }

    synchronized boolean hasError() {
      return error != null;
    }

    synchronized int getBatchCounter() {
      return batchCounter;
    }

    synchronized int getRecordCounter() {
      return recordCounter;
    }
  }
}
