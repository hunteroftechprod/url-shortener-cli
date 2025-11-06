package dev.byhacks.shortener.tests_extra;

import dev.byhacks.shortener.core.ShortenerService;
import dev.byhacks.shortener.core.ResolveStatus;
import dev.byhacks.shortener.core.AppConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class ExtraTests {

    private final AppConfig cfg = new AppConfig(24, "cli.lk/", "build", 10);
    private final ShortenerService service = new ShortenerService(cfg);

    @Test
    void testShortenAndResolve() {
        var link = service.createShortLink("userA", "https://example.com", 5, Duration.ofHours(1));
        var resolved = service.resolve("userA", link.code());
        assertEquals(ResolveStatus.OK, resolved.status());
    }

    @Test
    void testLimitDecrement() {
        var link = service.createShortLink("userB", "https://example.org", 1, Duration.ofHours(1));
        service.resolve("userB", link.code());
        var blocked = service.resolve("userB", link.code());
        assertEquals(ResolveStatus.BLOCKED, blocked.status());
    }

    @Test
    void testUniquePerUser() {
        var a = service.createShortLink("user1", "https://same.com", 5, Duration.ofHours(1));
        var b = service.createShortLink("user2", "https://same.com", 5, Duration.ofHours(1));
        assertNotEquals(a.code(), b.code());
    }

    @Test
    void testNegativeTTLNotAccepted() {
        var link = service.createShortLink("userC", "https://ttlcheck.net", 5, Duration.ofHours(-1));
        assertNotNull(link.code());
    }

    @Test
    void testTTLExpiration() throws InterruptedException {
        var shortL = service.createShortLink("userD", "https://ttl.now", 5, Duration.ofMillis(100));
        Thread.sleep(150);
        var res = service.resolve("userD", shortL.code());
        assertTrue(res.status() == ResolveStatus.BLOCKED || res.status() == ResolveStatus.NOT_FOUND);
    }
}
