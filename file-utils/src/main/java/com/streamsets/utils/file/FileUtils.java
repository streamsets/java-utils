package com.streamsets.utils.file;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Adapted from
 * @see <a href="https://github.com/apache/servicecomb-java-chassis/blob/master/foundations/foundation-common/src/main/java/org/apache/servicecomb/foundation/common/utils/FilePerm.java">this source</a>
 *
 * This source is copied from an Apache 2.0 licensed project (apache/servicecomb-java-chassis).  We are reproducing the
 * code here because the project is apparently only packaged as pom (and not jar)
 */
public abstract class FileUtils {
  public static final int FILE_PERM_UREAD = 256;

  /**
   * owner 可写
   */
  public static final int FILE_PERM_UWRITE = 128;

  /**
   * owner 可执行
   */
  public static final int FILE_PERM_UEXEC = 64;

  /**
   * 同组可读
   */
  public static final int FILE_PERM_GREAD = 32;

  /**
   * 同组可写
   */
  public static final int FILE_PERM_GWRITE = 16;

  /**
   * 同组可执行
   */
  public static final int FILE_PERM_GEXEC = 8;

  /**
   * 其他可读
   */
  public static final int FILE_PERM_OREAD = 4;

  /**
   * 其他可写
   */
  public static final int FILE_PERM_OWRITE = 2;

  /**
   * 其他可执行
   */
  public static final int FILE_PERM_OEXEC = 1;

  /**
   * mask
   */
  public static final int FILE_PERM_MASK = 511;


  private FileUtils() {
    // no-op, to prevent creation
  }

  /**
   * owner是否可读
   */
  public static boolean uCanRead(int perm) {
    return (FILE_PERM_UREAD & perm) > 0;
  }

  /**
   * owner是否可写
   */
  public static boolean uCanWrite(int perm) {
    return (FILE_PERM_UWRITE & perm) > 0;
  }

  /**
   * owner是否可执行
   */
  public static boolean uCanExec(int perm) {
    return (FILE_PERM_UEXEC & perm) > 0;
  }

  /**
   * 同组是否可读
   */
  public static boolean gCanRead(int perm) {
    return (FILE_PERM_GREAD & perm) > 0;
  }

  /**
   * 同组是否可写
   */
  public static boolean gCanWrite(int perm) {
    return (FILE_PERM_GWRITE & perm) > 0;
  }

  /**
   * 同组是否可执行
   */
  public static boolean gCanExec(int perm) {
    return (FILE_PERM_GEXEC & perm) > 0;
  }

  /**
   * 其他是否可读
   */
  public static boolean oCanRead(int perm) {
    return (FILE_PERM_GREAD & perm) > 0;
  }

  /**
   * 其他是否可写
   */
  public static boolean oCanWrite(int perm) {
    return (FILE_PERM_GWRITE & perm) > 0;
  }

  /**
   * 其他是否可执行
   */
  public static boolean oCanExec(int perm) {
    return (FILE_PERM_GEXEC & perm) > 0;
  }

  /**
   * 获取Posix权限
   */
  public static Set<PosixFilePermission> getPosixPerm(int perm) {
    StringBuilder permStr = new StringBuilder();

    permStr.append(uCanRead(perm) ? "r" : "-");
    permStr.append(uCanWrite(perm) ? "w" : "-");
    permStr.append(uCanExec(perm) ? "x" : "-");
    permStr.append(gCanRead(perm) ? "r" : "-");
    permStr.append(gCanWrite(perm) ? "w" : "-");
    permStr.append(gCanExec(perm) ? "x" : "-");
    permStr.append(oCanRead(perm) ? "r" : "-");
    permStr.append(oCanWrite(perm) ? "w" : "-");
    permStr.append(oCanExec(perm) ? "x" : "-");

    return PosixFilePermissions.fromString(permStr.toString());
  }

  public static URL[] getJarUrlsInDirectory(final String directory) {
    final Path dirPath = Paths.get(directory);
    if (!dirPath.toFile().exists()) {
      throw new IllegalArgumentException(String.format("Path %s does not exist", directory));
    } else if (!dirPath.toFile().isDirectory()) {
      throw new IllegalArgumentException(String.format("Path %s is not a directory", directory));
    } else {
      final List<URL> jarURLs = new LinkedList<>();
      try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath,
          path -> path.toFile().isFile() && path.toString().toLowerCase().endsWith(".jar")
      )) {
        for (Path jar : stream) {
          // can't use lambda because of MalformedURLException being thrown here
          jarURLs.add(jar.toUri().toURL());
        }
      } catch (IOException e) {
        throw new IllegalArgumentException(String.format(
            "IOException attempting to traverse directory %s for jar files: %s",
            directory,
            e.getMessage()
        ), e);
      }
      return jarURLs.toArray(new URL[jarURLs.size()]);
    }
  }
}
