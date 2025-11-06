package dev.byhacks.shortener;

import dev.byhacks.shortener.core.*;
import dev.byhacks.shortener.infra.*;
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    var config = AppConfig.load();
    Path dataDir = PathsUtil.ensureDataDir(config.getDataDir());
    var repo = new JsonRepository(dataDir.resolve("links.json"));
    var userRepo = new JsonUserRepository(dataDir.resolve("users.json"));
    var notifier = new ConsoleNotifier();
    var service = new ShortenerService(repo, userRepo, notifier, config);

    if (args.length == 0) {
      printHelp();
      return;
    }

    String command = args[0];
    switch (command) {
      case "init" -> {
        UUID userId = service.ensureUser();
        System.out.println("Ваш UUID: " + userId);
      }
      case "shorten" -> {
        var opts = Cli.parse(Arrays.copyOfRange(args, 1, args.length));
        String url = opts.getOrDefault("--url", "");
        int limit = Integer.parseInt(opts.getOrDefault("--limit", "0"));
        long ttlHours = Long.parseLong(opts.getOrDefault("--ttl", String.valueOf(config.getDefaultTtlHours())));

        UUID userId = service.ensureUser();
        var link = service.createShortLink(userId, url, limit, Duration.ofHours(ttlHours));
        System.out.println("Создано: " + config.getShortHost() + link.shortCode());
      }
      case "open" -> {
        if (args.length < 2) {
          System.err.println("Укажите shortCode.");
          return;
        }
        String code = args[1];
        var result = service.resolve(code);
        if (result.status() == ResolveStatus.OK) {
          System.out.println("Открываю: " + result.url());
          if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI(result.url()));
          }
        } else {
          System.out.println("Недоступно: " + result.message());
        }
      }
      case "list" -> {
        UUID userId = service.ensureUser();
        service.listLinks(userId).forEach(l -> {
          System.out.printf("%s -> %s | clicks=%d/%s | expires=%s%n",
              l.shortCode(), l.longUrl(), l.clicks(), (l.maxClicks() == 0 ? "∞" : String.valueOf(l.maxClicks())),
              l.expiresAt());
        });
      }
      case "edit" -> {
        if (args.length < 2) {
          System.err.println("Укажите shortCode.");
          return;
        }
        String code = args[1];
        var opts = Cli.parse(Arrays.copyOfRange(args, 2, args.length));
        Integer newLimit = opts.containsKey("--limit") ? Integer.parseInt(opts.get("--limit")) : null;
        Long ttlH = opts.containsKey("--ttl") ? Long.parseLong(opts.get("--ttl")) : null;
        UUID userId = service.ensureUser();
        service.edit(userId, code, newLimit, ttlH);
        System.out.println("Обновлено.");
      }
      case "delete" -> {
        if (args.length < 2) {
          System.err.println("Укажите shortCode.");
          return;
        }
        String code = args[1];
        UUID userId = service.ensureUser();
        service.delete(userId, code);
        System.out.println("Удалено.");
      }
      case "help" -> printHelp();
      default -> {
        if (!List.of("init","shorten","open","list","edit","delete","help").contains(command)) {
          System.err.println("Неизвестная команда: " + command);
        }
        printHelp();
      }
    }
  }

  private static void printHelp() {
    String help = ""
        + "URL Shortener CLI\n"
        + "Команды:\n"
        + "  init                                - создать/показать UUID пользователя\n"
        + "  shorten --url <URL> [--limit N] [--ttl H]\n"
        + "                                      - создать короткую ссылку; limit макс. кликов (0=без лимита), ttl часы\n"
        + "  open <shortCode>                    - перейти по короткой ссылке\n"
        + "  list                                - список ваших ссылок\n"
        + "  edit <shortCode> [--limit N] [--ttl H] - изменить лимит/TTL (только владелец)\n"
        + "  delete <shortCode>                  - удалить ссылку (только владелец)\n"
        + "  help                                - справка\n";
    System.out.println(help);
  }
}
