package dev.byhacks.shortener.core;

import org.apache.commons.validator.routines.UrlValidator;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ShortenerService {
  private final LinkRepository repo;
  private final UserRepository userRepo;
  private final Notifier notifier;
  private final AppConfig config;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final UrlValidator validator = UrlValidator.getInstance();

  public ShortenerService(LinkRepository repo, UserRepository userRepo, Notifier notifier, AppConfig config) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.notifier = notifier;
    this.config = config;
    scheduler.scheduleAtFixedRate(
        () -> repo.deleteExpired(Instant.now()),
        config.getCleanupIntervalSeconds(),
        config.getCleanupIntervalSeconds(),
        TimeUnit.SECONDS
    );
  }

  public UUID ensureUser() {
    return userRepo.getOrCreateUser().orElseThrow();
  }

  public Link createShortLink(UUID userId, String url, int maxClicks, Duration ttl) {
    if (!validator.isValid(url)) {
      throw new IllegalArgumentException("Невалидный URL");
    }
    String code = generateUniqueCode();
    Instant now = Instant.now();
    // учитываем любой положительный TTL (секунды/минуты/часы)
    Instant expires = (ttl != null && !ttl.isZero() && !ttl.isNegative()) ? now.plus(ttl) : null;

    Link link = new Link(code, url, userId, maxClicks, 0, now, expires);
    repo.save(link);
    return link;
  }

  public ResolveResult resolve(String code) {
    var opt = repo.findByCode(code);
    if (opt.isEmpty()) return ResolveResult.notFound();
    var link = opt.get();

    if (link.expiresAt() != null && Instant.now().isAfter(link.expiresAt())) {
      repo.delete(code);
      notifier.notify(link.ownerId(), "Срок жизни ссылки " + code + " истек. Создайте новую.");
      return ResolveResult.blocked("Ссылка истекла");
    }
    if (link.maxClicks() > 0 && link.clicks() >= link.maxClicks()) {
      notifier.notify(link.ownerId(), "Лимит переходов по ссылке " + code + " исчерпан.");
      return ResolveResult.blocked("Лимит переходов исчерпан");
    }

    repo.incrementClicks(code);
    var after = repo.findByCode(code).orElse(link);
    if (after.maxClicks() > 0 && after.clicks() >= after.maxClicks()) {
      notifier.notify(after.ownerId(), "Лимит переходов по ссылке " + code + " исчерпан.");
    }
    return ResolveResult.ok(link.longUrl());
  }

  public List<Link> listLinks(UUID userId) {
    return repo.findByOwner(userId);
  }

  public void edit(UUID userId, String code, Integer newLimit, Long newTtlHours) {
    var opt = repo.findByCode(code);
    if (opt.isEmpty()) throw new IllegalArgumentException("Не найдено");
    var l = opt.get();
    if (!l.ownerId().equals(userId)) throw new SecurityException("Недостаточно прав");

    long limit = newLimit != null ? newLimit : l.maxClicks();
    var expires = l.expiresAt();
    if (newTtlHours != null) {
      expires = newTtlHours > 0 ? Instant.now().plus(Duration.ofHours(newTtlHours)) : null;
    }

    var updated = new Link(l.shortCode(), l.longUrl(), l.ownerId(), limit, l.clicks(), l.createdAt(), expires);
    repo.save(updated);
  }

  public void delete(UUID userId, String code) {
    var opt = repo.findByCode(code);
    if (opt.isEmpty()) return;
    var l = opt.get();
    if (!l.ownerId().equals(userId)) throw new SecurityException("Недостаточно прав");
    repo.delete(code);
  }

  private String generateUniqueCode() {
    String alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    Random rnd = new Random();
    while (true) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 7; i++) sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
      String code = sb.toString();
      if (!repo.existsCode(code)) return code;
    }
  }
}
