package org.col.common.kryo.map;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.esotericsoftware.kryo.pool.KryoPool;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.col.api.model.VerbatimRecord;
import org.col.common.kryo.ApiKryoFactory;
import org.gbif.dwc.terms.DwcTerm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

public class MapDbObjectSerializerTest {
  File dbf;
  KryoPool pool;
  
  @Before
  public void init() throws IOException {
    dbf = new File("/tmp/mapdb-test");
    // make sure mapdb parent dirs exist
    if (!dbf.getParentFile().exists()) {
      dbf.getParentFile().mkdirs();
    }
  
    pool = new KryoPool.Builder(new ApiKryoFactory())
        .softReferences()
        .build();
  }
  
  @After
  public void shutdown() {
    FileUtils.deleteQuietly(dbf);
  }
  
  @Test
  public void serde() {
  
    DB mapDb = DBMaker
        .fileDB(dbf)
        .fileMmapEnableIfSupported()
        .make();
    
    Map<Long, VerbatimRecord> verbatim = mapDb.hashMap("verbatim")
        .keySerializer(Serializer.LONG)
        .valueSerializer(new MapDbObjectSerializer(VerbatimRecord.class, pool, 128))
        .create();
  
    StopWatch watch = new StopWatch();
    verbatim.put(0l, gen(0));
    watch.start();
    System.out.println(watch.getNanoTime());
    for (long x=1; x<1000; x++) {
      verbatim.put(x, gen(x));
    }
    watch.suspend();
    System.out.println(watch.getNanoTime());
  
    mapDb.close();
  }
  
  public static VerbatimRecord gen(long x) {
    return new VerbatimRecord(x, "myfile.txt", DwcTerm.Taxon);
  }
}