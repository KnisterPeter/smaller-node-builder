package de.matrixweb.smaller.maven.plugin.node;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

/**
 * @author markusw
 */
public class PackageInfoTest {

  private TestLogger logger;

  private NpmCache cache;

  private File install;

  /**
   * 
   */
  @Before
  public void setUp() {
    this.logger = new TestLogger();
    this.cache = new NpmCache(new File("."));
    this.install = new File("./target/test-install");
  }

  /**
   * @throws IOException
   */
  @After
  public void tearDown() throws IOException {
    if (this.install.exists()) {
      FileUtils.deleteDirectory(this.install);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testNameAndVersion() throws Exception {
    FileUtils.deleteDirectory(this.install);

    final PackageInfo pkg = PackageInfo.createPackage("file@0.2.1",
        this.logger, this.cache);
    pkg.install(this.install, this.install, false);

    assertInstallDir();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGitUrl() throws Exception {
    FileUtils.deleteDirectory(this.install);

    final PackageInfo pkg = PackageInfo.createPackage(
        "git://github.com/aconbere/node-file-utils.git#master", this.logger,
        this.cache);
    pkg.install(this.install, this.install, false);

    assertInstallDir();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testTarballDownload() throws Exception {
    FileUtils.deleteDirectory(this.install);

    final PackageInfo pkg = PackageInfo.createPackage(
        "http://registry.npmjs.org/file/-/file-0.2.1.tgz", this.logger,
        this.cache);
    pkg.install(this.install, this.install, false);

    assertInstallDir();
  }

  private void assertInstallDir() {
    final File file = new File(this.install, "file");
    assertThat(file.exists(), is(true));
    assertThat(file.isDirectory(), is(true));
    assertThat(new File(file, "package.json").exists(), is(true));
  }

  private class TestLogger implements Logger {

    @Override
    public void info(final String message) {
      System.out.println(message);
    }

    @Override
    public void debug(final String message) {
      System.out.println(message);
    }

  }

}
