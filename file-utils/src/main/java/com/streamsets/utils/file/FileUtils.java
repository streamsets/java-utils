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
 * <p>
 * This source is copied from an Apache 2.0 licensed project (apache/servicecomb-java-chassis).  We are reproducing the
 * code here because the project is apparently only packaged as pom (and not jar)
 * </p>
 */
public abstract class FileUtils {
  /**
   * permission bit or user read
   */
  public static final int FILE_PERM_UREAD = 256;

  /**
   * permission bit for user write
   */
  public static final int FILE_PERM_UWRITE = 128;

  /**
   * permission bit for user execute
   */
  public static final int FILE_PERM_UEXEC = 64;

  /**
   * permission bit for group read
   */
  public static final int FILE_PERM_GREAD = 32;

  /**
   * permission bit for group write
   */
  public static final int FILE_PERM_GWRITE = 16;

  /**
   * permission bit for group execute
   */
  public static final int FILE_PERM_GEXEC = 8;

  /**
   * permission bit for other read
   */
  public static final int FILE_PERM_OREAD = 4;

  /**
   * permission bit for other write
   */
  public static final int FILE_PERM_OWRITE = 2;

  /**
   * permission bit for other exec
   */
  public static final int FILE_PERM_OEXEC = 1;

  /**
   * private constructor for utility class
   */
  private FileUtils() {
    // no-op, to prevent creation
  }

  /**
   * Checks whether user can read, given an int representation of the POSIX permissions
   * @param perm int representation of POSIX permissions
   * @return whether the user can read, given the permissions
   */
  public static boolean uCanRead(int perm) {
    return (FILE_PERM_UREAD & perm) > 0;
  }

  /**
   * Checks whether user can write, given an int representation of the POSIX permissions
   * @param perm int representation of POSIX permissions
   * @return whether the user can write, given the permissions
   */
  public static boolean uCanWrite(int perm) {
    return (FILE_PERM_UWRITE & perm) > 0;
  }

  /**
   * Checks whether user can execute, given an int representation of the POSIX permissions
   * @param perm int representation of POSIX permissions
   * @return whether the user can execute, given the permissions
   */
  public static boolean uCanExec(int perm) {
    return (FILE_PERM_UEXEC & perm) > 0;
  }

  /**
   * Checks whether group can read, given an int representation of the POSIX permissions
   * @param perm int representation of POSIX permissions
   * @return whether the group can read, given the permissions
   */
  public static boolean gCanRead(int perm) {
    return (FILE_PERM_GREAD & perm) > 0;
  }

  /**
   * Checks whether group can write, given an int representation of the POSIX permissions
   * @param perm int representation of POSIX permissions
   * @return whether the group can write, given the permissions
   */
  public static boolean gCanWrite(int perm) {
    return (FILE_PERM_GWRITE & perm) > 0;
  }

  /**
   * Checks whether group can exec, given an int representation of the POSIX permissions
   * @param perm int representation of POSIX permissions
   * @return whether the group can exec, given the permissions
   */
  public static boolean gCanExec(int perm) {
    return (FILE_PERM_GEXEC & perm) > 0;
  }

  /**
   * Checks whether others can read, given an int representation of the POSIX permissions
   * @param perm int representation of POSIX permissions
   * @return whether others can read, given the permissions
   */
  public static boolean oCanRead(int perm) {
    return (FILE_PERM_OREAD & perm) > 0;
  }

  /**
   * Checks whether others can write, given an int representation of the POSIX permissions
   * @param perm int representation of POSIX permissions
   * @return whether others can write, given the permissions
   */
  public static boolean oCanWrite(int perm) {
    return (FILE_PERM_OWRITE & perm) > 0;
  }

  /**
   * Checks whether others can exec, given an int representation of the POSIX permissions
   * @param perm int representation of POSIX permissions
   * @return whether others can exec, given the permissions
   */
  public static boolean oCanExec(int perm) {
    return (FILE_PERM_OEXEC & perm) > 0;
  }

  /**
   * Converts an integer representation of POSIX permissions to a {@link Set} of {@link PosixFilePermission}
   *
   * @param perm the integer representation of permissions
   * @return the permissions expressed as a set of the appropriate Java type
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

  /**
   * Checks a directory for all jar files, and returns them as an array of {@link URL} instances.  Most useful
   * for configuring {@link java.net.URLClassLoader} instances.
   *
   * @param directory the directory from which jar files will be listed
   * @return an array of {@link URL} instances corresponding to each jar file found in the given directory
   */
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
