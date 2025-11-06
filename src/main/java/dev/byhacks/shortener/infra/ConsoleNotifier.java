package dev.byhacks.shortener.infra;

import dev.byhacks.shortener.core.Notifier;
import java.util.UUID;

public class ConsoleNotifier implements Notifier {
  @Override
  public void notify(UUID userId, String message) {
    System.out.printf("[notify %s] %s%n", userId, message);
  }
}
