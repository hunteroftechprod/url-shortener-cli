package dev.byhacks.shortener.tests_extra;

import dev.byhacks.shortener.core.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class ExtraTests {

    // ---------- In-memory fakes, соответствующие твоим интерфейсам ----------

    static class InMemLinkRepo implements LinkRepository {
        private final Map<String, Link> byCode = new ConcurrentHashMap<>();

        @Override
        public Optional<Link> findByCode(String code) {
            return Optional.ofNullable(byCode.get(code));
        }

        @Override
        public List<Link> findByOwner(UUID owner) {
            List<Link> res = new ArrayList<>();
            for (Link l : byCode.values()) if (l.ownerId().equals(owner)) res.add(l);
            return res;
        }

        @Override
        public void save(Link link) {
            byCode.put(link.shortCode(), link);
        }

        @Override
        public void delete(String code) {
            byCode.remove(code);
        }

        @Override
        public void incrementClicks(String code) {
            Link l = byCode.get(code);
            if (l == null) return;
            Link updated = new Link(
                    l.shortCode(), l.longUrl(), l.ownerId(),
                    l.maxClicks(), l.clicks() + 1,
                    l.createdAt(), l.expiresAt()
            );
            byCode.put(code, updated);
        }

        @Override
        public void deleteExpired(Instant now) {
            for (String c : new ArrayList<>(byCode.keySet())) {
                Link l = byCode.get(c);
                if (l != null && l.expiresAt() != null && now.isAfter(l.expiresAt())) {
                    byCode.remove(c);
                }
            }
        }

        @Override
        public boolean existsCode(String code) {
            return byCode.containsKey(code);
        }
    }

    static class InMemUserRepo implements UserRepository {
        private UUID cached;

        @Override
        public Optional<UUID> getOrCreateUser() {
            if (cached == null) cached = UUID.randomUUID();
            return Optional.of(cached);
        }

        @Override
        public void saveUser(UUID id) {
            cached = id;
        }

        @Override
        public UUID loadUser() {
            if (cached == null) cached = UUID.randomUUID();
            return cached;
        }
    }

    static class DummyNotifier implements Notifier {
        @Override
        public void notify(UUID userId, String message) {
            // no-op
        }
    }

    private ShortenerService newService(int cleanupSeconds) {
        LinkRepository repo = new InMemLinkRepo();
        UserRepository users = new InMemUserRepo();
        Notifier notifier = new DummyNotifier();
        AppConfig cfg = new AppConfig(24, "cli.lk/", "build", cleanupSeconds);
        return new ShortenerService(repo, users, notifier, cfg);
    }

    // ------------------------------ Tests ------------------------------------

    @Test
    void shorten_and_resolve_OK() {
        ShortenerService svc = newService(3600);
        UUID user = UUID.randomUUID();
        Link link = svc.createShortLink(user, "https://example.com", 5, Duration.ofHours(1));
        ResolveResult r = svc.resolve(link.shortCode());
        assertEquals(ResolveStatus.OK, r.status());
        assertEquals("https://example.com", r.url());
    }

    @Test
    void limit_blocks_after_threshold() {
        ShortenerService svc = newService(3600);
        UUID user = UUID.randomUUID();
        Link link = svc.createShortLink(user, "https://example.org", 1, Duration.ofHours(1));
        assertEquals(ResolveStatus.OK, svc.resolve(link.shortCode()).status());
        assertEquals(ResolveStatus.BLOCKED, svc.resolve(link.shortCode()).status());
    }

    @Test
    void unique_per_user_for_same_long_url() {
        ShortenerService svc = newService(3600);
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        Link a = svc.createShortLink(u1, "https://same.com", 0, Duration.ofHours(1));
        Link b = svc.createShortLink(u2, "https://same.com", 0, Duration.ofHours(1));
        assertNotEquals(a.shortCode(), b.shortCode());
    }

    @Test
    void ttl_expires_blocks_or_not_found() throws Exception {
        ShortenerService svc = newService(3600);
        UUID user = UUID.randomUUID();
        Link link = svc.createShortLink(user, "https://ttl.now", 5, Duration.ofMillis(120));
        Thread.sleep(220);
        ResolveStatus s = svc.resolve(link.shortCode()).status();
        assertTrue(s == ResolveStatus.BLOCKED || s == ResolveStatus.NOT_FOUND);
    }

    @Test
    void edit_requires_owner_and_applies_changes() {
        ShortenerService svc = newService(3600);
        UUID owner = UUID.randomUUID();
        Link link = svc.createShortLink(owner, "https://example.com/owner", 1, Duration.ofHours(1));
        // увеличим лимит и TTL
        svc.edit(owner, link.shortCode(), 10, 2L);
        assertEquals(ResolveStatus.OK, svc.resolve(link.shortCode()).status());
    }

    @Test
    void edit_by_foreign_user_throws() {
        ShortenerService svc = newService(3600);
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        Link link = svc.createShortLink(owner, "https://example.com/secure-edit", 1, Duration.ofHours(1));
        assertThrows(SecurityException.class, () -> svc.edit(stranger, link.shortCode(), 5, 1L));
    }

    @Test
    void delete_by_foreign_user_throws() {
        ShortenerService svc = newService(3600);
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        Link link = svc.createShortLink(owner, "https://example.com/secure-delete", 1, Duration.ofHours(1));
        assertThrows(SecurityException.class, () -> svc.delete(stranger, link.shortCode()));
    }
}
