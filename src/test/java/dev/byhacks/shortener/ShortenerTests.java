package dev.byhacks.shortener;

import dev.byhacks.shortener.core.*;
import dev.byhacks.shortener.infra.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ShortenerTests {

  @Test
  void createAndResolve_withLimit() {
    var repo = new JsonRepository(Path.of("build/test-links.json"));
    var userRepo = new JsonUserRepository(Path.of("build/test-user.txt"));
    var notifier = new ConsoleNotifier();
    var cfg = new AppConfig(24, "cli.lk/", "build", 3600);
    var svc = new ShortenerService(repo, userRepo, notifier, cfg);

    UUID user = svc.ensureUser();
    var link = svc.createShortLink(user, "https://example.com", 2, Duration.ofHours(1));

    var r1 = svc.resolve(link.shortCode());
    assertEquals(ResolveStatus.OK, r1.status());
    var r2 = svc.resolve(link.shortCode());
    assertEquals(ResolveStatus.OK, r2.status());
    var r3 = svc.resolve(link.shortCode());
    assertEquals(ResolveStatus.BLOCKED, r3.status());
  }

  @Test
  void ttlExpires() throws InterruptedException {
    var repo = new JsonRepository(Path.of("build/test-links2.json"));
    var userRepo = new JsonUserRepository(Path.of("build/test-user2.txt"));
    var notifier = new ConsoleNotifier();
    var cfg = new AppConfig(24, "cli.lk/", "build", 1);
    var svc = new ShortenerService(repo, userRepo, notifier, cfg);
    UUID user = svc.ensureUser();
    var link = svc.createShortLink(user, "https://example.com", 0, Duration.ofSeconds(1));
    Thread.sleep(1500);
    var r = svc.resolve(link.shortCode());
    assertTrue(r.status() == ResolveStatus.BLOCKED || r.status() == ResolveStatus.NOT_FOUND);
  }
}
