package dev.byhacks.shortener.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.byhacks.shortener.core.Link;
import dev.byhacks.shortener.core.LinkRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JsonRepository implements LinkRepository {
  private final Path file;
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private final Map<String, Link> store = new ConcurrentHashMap<>();

  public JsonRepository(Path file) {
    this.file = file;
    load();
  }

  private synchronized void load() {
    try {
      if (!Files.exists(file)) {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "[]");
      }
      var list = mapper.readValue(Files.readString(file), new TypeReference<List<Link>>(){});
      store.clear();
      for (Link l : list) store.put(l.shortCode(), l);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private synchronized void flush() {
    try {
      var list = new ArrayList<>(store.values());
      Files.writeString(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<Link> findByCode(String code) {
    return Optional.ofNullable(store.get(code));
  }

  @Override
  public List<Link> findByOwner(UUID owner) {
    return store.values().stream().filter(l -> l.ownerId().equals(owner))
        .sorted(Comparator.comparing(Link::createdAt).reversed())
        .collect(Collectors.toList());
  }

  @Override
  public void save(Link link) {
    store.put(link.shortCode(), link);
    flush();
  }

  @Override
  public void delete(String code) {
    store.remove(code);
    flush();
  }

  @Override
  public void incrementClicks(String code) {
    var l = store.get(code);
    if (l != null) {
      var updated = new Link(l.shortCode(), l.longUrl(), l.ownerId(), l.maxClicks(), l.clicks()+1, l.createdAt(), l.expiresAt());
      store.put(code, updated);
      flush();
    }
  }

  @Override
  public void deleteExpired(Instant now) {
    var toRemove = store.values().stream()
        .filter(l -> l.expiresAt() != null && l.expiresAt().isBefore(now))
        .map(Link::shortCode).toList();
    for (var c : toRemove) store.remove(c);
    if (!toRemove.isEmpty()) flush();
  }

  @Override
  public boolean existsCode(String code) {
    return store.containsKey(code);
  }
}
