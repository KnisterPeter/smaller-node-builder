package de.matrixweb.smaller.maven.plugin.node;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

class Extractor {

  static void uncompress(final String name, final String version,
      final Logger log, final InputStream in, final File target)
      throws IOException {
    uncompress(name + '@' + version, log, in, target);
  }

  static void uncompress(final String name, final Logger log,
      final InputStream in, final File target) throws IOException {
    try {
      uncompress(log, in, target);
    } catch (final CompressorException e) {
      throw new IOException("Failed to decompress " + name, e);
    } catch (final ArchiveException e) {
      throw new IOException("Failed to decompress " + name, e);
    }
  }

  static void uncompress(final Logger log, final InputStream in,
      final File target) throws IOException, CompressorException,
      ArchiveException {
    final CompressorStreamFactory csf = new CompressorStreamFactory();
    csf.setDecompressConcatenated(true);
    final CompressorInputStream cin = csf
        .createCompressorInputStream(new BufferedInputStream(in));
    final File temp = File.createTempFile("smaller-npm", ".tar");
    try {
      FileUtils.copyInputStreamToFile(cin, temp);

      final FileInputStream fin = new FileInputStream(temp);
      try {
        final ArchiveInputStream ain = new ArchiveStreamFactory()
            .createArchiveInputStream(new BufferedInputStream(fin));
        ArchiveEntry entry = ain.getNextEntry();
        while (entry != null) {
          extractEntry(log, target, ain, entry);
          entry = ain.getNextEntry();
        }
      } finally {
        IOUtils.closeQuietly(fin);
      }
    } finally {
      temp.delete();
    }
  }

  private static void extractEntry(final Logger log, final File target,
      final ArchiveInputStream ain, final ArchiveEntry entry)
      throws IOException {
    String name = entry.getName();
    if (name.startsWith("package")) {
      name = name.substring("package/".length());
    }
    log.debug("... extracting " + name);
    final File file = new File(target, name);
    if (!entry.isDirectory()) {
      file.getParentFile().mkdirs();
      final FileOutputStream out = new FileOutputStream(file);
      try {
        final byte[] buf = new byte[4096];
        long len = entry.getSize();
        int read = ain.read(buf, 0, Math.min(4096, (int) len));
        while (len > 0) {
          len -= read;
          out.write(buf, 0, read);
          if (len > 0) {
            read = ain.read(buf, 0, Math.min(4096, (int) len));
          }
        }
      } finally {
        IOUtils.closeQuietly(out);
      }
    }
  }

}
