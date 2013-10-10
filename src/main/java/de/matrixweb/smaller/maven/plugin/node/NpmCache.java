package de.matrixweb.smaller.maven.plugin.node;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;

/**
 * @author markusw
 */
class NpmCache {

  private final File base;

  NpmCache(final File base) {
    this.base = new File(base, "target/npm-cache");
    this.base.mkdirs();
  }

  private String mangle(final String in) {
    return in.replace('/', '-').replace(':', '-');
  }

  private File entry(final String url) {
    return new File(this.base, mangle(url));
  }

  InputStream get(final String url) throws IOException {
    final File entry = entry(url);
    if (entry.exists()) {
      return new FileInputStream(entry);
    }
    return null;
  }

  void put(final String url, final InputStream in) throws IOException {
    final File entry = entry(url);
    FileUtils.copyInputStreamToFile(in, entry);
  }

}
