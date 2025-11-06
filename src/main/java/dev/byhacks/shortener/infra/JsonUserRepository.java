package dev.byhacks.shortener.infra;

import dev.byhacks.shortener.core.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class JsonUserRepository implements UserRepository {
  private final Path file;

  public JsonUserRepository(Path file) { this.file = file; }

  @Override
  public Optional<UUID> getOrCreateUser() {
    try {
      if (Files.exists(file)) {
        var s = Files.readString(file).trim();
        if (!s.isBlank()) return Optional.of(UUID.fromString(s));
      }
      UUID id = UUID.randomUUID();
      saveUser(id);
      return Optional.of(id);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void saveUser(UUID id) {
    try {
      Files.createDirectories(file.getParent());
      Files.writeString(file, id.toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public UUID loadUser() {
    try {
      if (!Files.exists(file)) throw new IllegalStateException("Пользователь не инициализирован. Выполните: init");
      return UUID.fromString(Files.readString(file).trim());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
