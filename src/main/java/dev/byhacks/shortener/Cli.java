package dev.byhacks.shortener;

import java.util.HashMap;
import java.util.Map;

public class Cli {
  public static Map<String, String> parse(String[] args) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < args.length; i++) {
      String a = args[i];
      if (a.startsWith("--")) {
        String val = "true";
        if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
          val = args[++i];
        }
        map.put(a, val);
      }
    }
    return map;
  }
}
