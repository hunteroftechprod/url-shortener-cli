package dev.byhacks.shortener.core;

import java.util.UUID;

public interface Notifier {
  void notify(UUID userId, String message);
}
