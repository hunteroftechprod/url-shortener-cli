package dev.byhacks.shortener.core;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
  Optional<UUID> getOrCreateUser();
  void saveUser(UUID id);
  UUID loadUser();
}
