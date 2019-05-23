package com.streamsets.utils.tarball;

import com.streamsets.utils.file.FileUtils;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public abstract class TarballUtils {
  private static final Logger LOG = LoggerFactory.getLogger(TarballUtils.class);

  private TarballUtils() {
    // no-op, to prevent creation
  }

  /**
   * <p>
   * Adapted from StackOverflow answer
   * </p>
   *
   * @see <a href="https://stackoverflow.com/a/7556307/375670">Source</a>
   *
   * <p>
   * Untar an input file into an output file.
   * The output file is created in the output folder, having the same name
   * as the input file, minus the '.tar' extension.
   * </p>
   *
   * @param inputFile     the input .tar file
   * @param outputDir     the output directory file.
   * @throws IOException if thrown by any underlying libraries
   * @throws ArchiveException if thrown by any underlying libraries
   *
   * @return The {@link List} of {@link File}s with the untared content.
   */
  @SuppressWarnings("squid:S3776")
  public static List<File> unTar(final File inputFile, final File outputDir) throws
      IOException, ArchiveException {

    if (LOG.isInfoEnabled()) {
      LOG.info(String.format("Untaring %s to dir %s.", inputFile.getAbsolutePath(), outputDir.getAbsolutePath()));
    }

    final List<File> untaredFiles = new LinkedList<>();
    try (
        final InputStream is = new FileInputStream(inputFile);
        final TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory()
            .createArchiveInputStream("tar", is)) {
      TarArchiveEntry entry = null;
      while ((entry = (TarArchiveEntry)debInputStream.getNextEntry()) != null) {
        final File outputFile = new File(outputDir, entry.getName());
        if (entry.isDirectory()) {
          if (LOG.isInfoEnabled()) {
            LOG.info(String.format("Attempting to write output directory %s.", outputFile.getAbsolutePath()));
          }
          if (!outputFile.exists()) {
            if (LOG.isInfoEnabled()) {
              LOG.info(String.format("Attempting to create output directory %s.", outputFile.getAbsolutePath()));
            }
            if (!outputFile.mkdirs()) {
              throw new IllegalStateException(String.format(
                  "Couldn't create directory %s.",
                  outputFile.getAbsolutePath()
              ));
            }
          }
        } else {
          if (LOG.isInfoEnabled()) {
            LOG.info(String.format("Creating output file %s.", outputFile.getAbsolutePath()));
          }
          final OutputStream outputFileStream = new FileOutputStream(outputFile);
          IOUtils.copy(debInputStream, outputFileStream);
          outputFileStream.close();
          Files.setPosixFilePermissions(outputFile.toPath(), FileUtils.getPosixPerm(entry.getMode()));
        }
        untaredFiles.add(outputFile);
      }

      return untaredFiles;
    }
  }

  /**
   * @see <a href="https://stackoverflow.com/a/7556307/375670">Source</a>
   *
   * Ungzip an input file into an output file.
   * <p>
   * The output file is created in the output folder, having the same name
   * as the input file, minus the '.gz' extension.
   * </p>
   *
   * @param inputFile     the input .gz file
   * @param outputDir     the output directory file.
   * @throws IOException if thrown by any underlying libraries
   *
   * @return The {@link File} with the ungzipped content.
   */
  public static File unGzip(final File inputFile, final File outputDir) throws IOException {
    if (LOG.isInfoEnabled()) {
      LOG.info(String.format("Ungzipping %s to dir %s.", inputFile.getAbsolutePath(), outputDir.getAbsolutePath()));
    }

    final File outputFile = new File(outputDir, inputFile.getName().substring(0, inputFile.getName().length() - 3));

    try (
      final GZIPInputStream in = new GZIPInputStream(new FileInputStream(inputFile));
      final FileOutputStream out = new FileOutputStream(outputFile);
    ) {
      IOUtils.copy(in, out);
      return outputFile;
    }
  }

  /**
   * Copies an archive file (tarball), which is either gzipped or not, to a target directory, then extracts the new copy
   * where it sits.
   *
   * @param archiveFile the tarball to be copied and the copy extracted
   * @param targetDir the directory into which a copy of the tarball should be extracted
   * @throws IOException if thrown by any underlying libraries
   * @throws ArchiveException if thrown by any underlying libraries
   */
  public static void copyAndExtractArchiveToDirectory(Path archiveFile, Path targetDir)
      throws IOException, ArchiveException {
    Files.copy(archiveFile, targetDir.resolve(archiveFile.getFileName()));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Copied data file {} to temp directory {}", archiveFile, targetDir);
    }

    Path newArchiveFile = targetDir.resolve(archiveFile.getFileName());

    if (newArchiveFile.toString().toLowerCase().endsWith(".gz")) {
      final File unzipped = unGzip(newArchiveFile.toFile(), targetDir.toFile());
      newArchiveFile = Paths.get(unzipped.toURI());
      if (LOG.isDebugEnabled()) {
        LOG.debug("Unzipped gzip archive file ({}) in temp directory ({})", newArchiveFile, targetDir);
      }
    }

    // at this point, the archive file is definitely not gzipped
    unTar(newArchiveFile.toFile(), targetDir.toFile());
  }

}
