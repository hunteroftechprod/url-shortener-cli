package dev.byhacks.shortener.core;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository {
  Optional<Link> findByCode(String code);
  List<Link> findByOwner(UUID owner);
  void save(Link link);
  void delete(String code);
  void incrementClicks(String code);
  void deleteExpired(Instant now);
  boolean existsCode(String code);
}
