package dev.byhacks.shortener.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
  private final long defaultTtlHours;
  private final String shortHost;
  private final String dataDir;
  private final long cleanupIntervalSeconds;

  public AppConfig(long defaultTtlHours, String shortHost, String dataDir, long cleanupIntervalSeconds) {
    this.defaultTtlHours = defaultTtlHours;
    this.shortHost = shortHost;
    this.dataDir = dataDir;
    this.cleanupIntervalSeconds = cleanupIntervalSeconds;
  }

  public static AppConfig load() {
    try (InputStream is = AppConfig.class.getResourceAsStream("/application.properties")) {
      Properties p = new Properties();
      p.load(is);
      long ttl = Long.parseLong(p.getProperty("default.ttl.hours", "24"));
      String host = p.getProperty("short.host", "cli.lk/");
      String dir = p.getProperty("data.dir", ".shortener-data");
      long cleanup = Long.parseLong(p.getProperty("cleanup.interval.seconds", "30"));
      return new AppConfig(ttl, host, dir, cleanup);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public long getDefaultTtlHours() { return defaultTtlHours; }
  public String getShortHost() { return shortHost; }
  public String getDataDir() { return dataDir; }
  public long getCleanupIntervalSeconds() { return cleanupIntervalSeconds; }
}
