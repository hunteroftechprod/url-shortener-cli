package dev.byhacks.shortener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathsUtil {
  public static Path ensureDataDir(String dirName) {
    Path p = Path.of(dirName);
    try {
      Files.createDirectories(p);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return p;
  }
}
