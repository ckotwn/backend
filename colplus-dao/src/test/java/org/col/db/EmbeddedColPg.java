package org.col.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.col.common.io.PathUtils;
import org.col.common.io.PortUtil;
import org.col.common.lang.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;

import static java.util.Arrays.asList;

/**
 * An {@link EmbeddedPostgres} server that can be start up and inits a minimal CoL+ db.
 * If PgConfig.host is pointing to an absolute path it will be used to reuse a already unzipped, cached server instance,
 * but does not share a data directory.
 */
public class EmbeddedColPg {
  private static final Logger LOG = LoggerFactory.getLogger(EmbeddedColPg.class);
	
	private static final String VERSION = "10.6-1"; //"11.0-1" exists for OSX, but not for Linux yet !!!;
	private static final List<String> DEFAULT_ADD_PARAMS = asList(
			"-E", "SQL_ASCII",
			"--locale=C",
			"--lc-collate=C",
			"--lc-ctype=C");

	private EmbeddedPostgres postgres;
  private final PgConfig cfg;
  private Path serverDir;
  
  @Deprecated
  public EmbeddedColPg() {
    this.cfg = new PgConfig();
    cfg.host = null;
    cfg.user = "postgres";
    cfg.password = "postgres";
    cfg.database = "colplus";
  }
  
  public EmbeddedColPg(PgConfig cfg) {
    this.cfg = cfg;
  }
  
  public PgConfig getCfg() {
    return cfg;
  }
  
  public void start() {
    if (postgres == null) {
      startDb();
    } else {
      LOG.info("Embedded Postgres already running");
    }
  }
  
  private void startDb() {
    try {
      LOG.info("Starting embedded Postgres");
      Instant start = Instant.now();
      // cached server directory to not unzip postgres binaries on every run is flawed:
      // https://github.com/yandex-qatools/postgresql-embedded/issues/142
      // so we we delete the known server dir each time until thats fixed
      serverDir = cfg.host == null ? Files.createTempDirectory("colplus-pg-") : Paths.get(cfg.host);
      LOG.debug("Use embedded Postgres, server dir={}", serverDir);
      
      postgres = new EmbeddedPostgres(() -> VERSION);
      // assigned some free port using local socket 0
      cfg.port = PortUtil.findFreePort();
      cfg.host = "localhost";
      cfg.maximumPoolSize = 3;
      postgres.start(EmbeddedPostgres.cachedRuntimeConfig(serverDir),
          cfg.host, cfg.port, cfg.database, cfg.user, cfg.password,
          DEFAULT_ADD_PARAMS
      );
      if (postgres.getProcess().isPresent()) {
        LOG.info("Pg started on port {}. Startup time: {} ms", cfg.port, Duration.between(start, Instant.now()).toMillis());
      } else {
        throw new IllegalStateException("Embedded postgres failed to startup");
      }
      
    } catch (Exception e) {
      LOG.error("Pg startup error {}: {}", e.getMessage(), cfg, e);
      stop();
      Exceptions.throwRuntime(e);
    }
  }
  
  public void stop() {
    if (postgres != null && postgres.getProcess().isPresent()) {
      final File dataDir = postgres.getConfig().get().storage().dbDir();
      LOG.info("Stopping embedded Postgres server={}, data={}", serverDir, dataDir);
      postgres.stop();
      
      try {
        FileUtils.deleteDirectory(serverDir.toFile());
        LOG.info("Removed Postgres server directory {}", serverDir);
      } catch (IllegalArgumentException | IOException e) {
        LOG.warn("Failed to remove Postgres server directory {}", serverDir, e);
      }
      
      if (dataDir.exists()) {
        try {
          FileUtils.deleteDirectory(dataDir);
          PathUtils.removeFileAndParentsIfEmpty(dataDir.toPath());
          LOG.info("Removed Postgres data directory {}", dataDir);
        } catch (IllegalArgumentException | IOException e) {
          LOG.warn("Failed to remove Postgres data directory {}", dataDir, e);
        }
      }
    }
  }
  
}
