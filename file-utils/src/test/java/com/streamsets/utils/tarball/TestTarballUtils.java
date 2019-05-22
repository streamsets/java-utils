package com.streamsets.utils.tarball;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestTarballUtils {

  @Test
  public void testCopyAndExtractArchiveToDirectory() throws IOException, URISyntaxException, ArchiveException {
    testArchiveExtractionHelper("sampleTarball.tar.gz");
  }

  @Test
  public void testCopyAndExtractGzippedArchiveToDirectory() throws IOException, URISyntaxException, ArchiveException {
    testArchiveExtractionHelper("sampleTarball.tar");
  }

  private void testArchiveExtractionHelper(String tarballName)
      throws IOException, URISyntaxException, ArchiveException  {

    final Path tempDir = Files.createTempDirectory(String.format(
        "testArchiveExtractionHelper_%d",
        Clock.systemUTC().millis()
    ));
    tempDir.toFile().deleteOnExit();

    final Path archiveFile = Paths.get(getClass().getResource(tarballName).toURI());

    TarballUtils.copyAndExtractArchiveToDirectory(archiveFile, tempDir);

    assertExtractedTestFiles(tempDir);

  }

  private static void assertExtractedTestFiles(final Path directory) throws IOException {
    final Set<PosixFilePermission> defaultPerms = PosixFilePermissions.fromString("rw-r--r--");
    final Set<PosixFilePermission> ownerOnlyReadPerms = PosixFilePermissions.fromString("r--------");
    final Object[][] expectedFileNamesContentsPermissions = new Object[][] {
        new Object[] {"file1.txt", "file1\n", defaultPerms},
        new Object[] {"dir1/subdir1/file2.txt", "file2\n", defaultPerms},
        new Object[] {"dir1/subdir1/file3.txt", "file3\n", defaultPerms},
        new Object[] {"dir1/subdir2/file4.txt", "file4\n", defaultPerms},
        new Object[] {"dir1/file5_400.txt", "file5\n", ownerOnlyReadPerms},
        new Object[] {"dir2/file6.txt", "file6\n", defaultPerms},
        new Object[] {"file7.txt", "file7\n", defaultPerms}
    };

    for (Object[] expectedFileVals : expectedFileNamesContentsPermissions) {
      final Path file = Paths.get(directory.toString(), (String) expectedFileVals[0]);
      assertTrue(Files.isRegularFile(file));
      final CharSource contents = com.google.common.io.Files.asCharSource(file.toFile(), Charsets.UTF_8);
      assertThat(contents.read(), equalTo(expectedFileVals[1]));
      final Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
      assertThat(perms, equalTo((Set<PosixFilePermission>) expectedFileVals[2]));
    }
  }
}
