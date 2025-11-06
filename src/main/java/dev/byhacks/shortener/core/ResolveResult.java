package dev.byhacks.shortener.core;

public record ResolveResult(ResolveStatus status, String url, String message) {
  public static ResolveResult ok(String url) { return new ResolveResult(ResolveStatus.OK, url, ""); }
  public static ResolveResult blocked(String message) { return new ResolveResult(ResolveStatus.BLOCKED, null, message); }
  public static ResolveResult notFound() { return new ResolveResult(ResolveStatus.NOT_FOUND, null, "Не найдено"); }
}
