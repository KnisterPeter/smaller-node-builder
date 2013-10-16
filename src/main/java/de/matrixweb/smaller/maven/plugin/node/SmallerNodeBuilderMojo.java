package de.matrixweb.smaller.maven.plugin.node;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.stringtemplate.v4.ST;

import de.matrixweb.smaller.maven.plugin.node.Descriptor.Version;

/**
 * @author markusw
 */
@Mojo(name = "smaller-node-builder", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class SmallerNodeBuilderMojo extends AbstractMojo {

  private final ObjectMapper om = new ObjectMapper();

  @Parameter(required = true, readonly = true, defaultValue = "${project}")
  private MavenProject project;

  @Parameter(required = true, readonly = true, defaultValue = "${basedir}")
  private File basedir;

  /**
   * Defines the type of code the package could handle. Should be either js or
   * css or both separated by comma.
   */
  @Parameter(required = true)
  private String type;

  @Parameter(defaultValue = "false")
  private boolean merging;

  @Parameter(required = true)
  private String name;

  @Parameter(required = true)
  private List<String> packages;

  /**
   * The folder to store the package and its requirements. Defaults to
   * ${basedir}/target/generated-resources/npm-modules.
   */
  @Parameter(defaultValue = "${basedir}/target/generated-resources/npm-modules")
  private File target;

  private File tempInstall;

  /**
   * This script is the main entry-point into the node-module and builds the
   * bridge to the JVM.
   */
  @Parameter(required = true)
  private String script;

  /**
   * Set this to true to force an update of the package.
   */
  @Parameter(defaultValue = "false")
  private boolean forceUpdate;

  private NpmCache cache;

  /**
   * @return the packages
   */
  public final List<String> getPackages() {
    if (this.packages == null) {
      this.packages = new ArrayList<String>();
    }
    return this.packages;
  }

  /**
   * @param packages
   *          the packages to set
   */
  public final void setPackages(final List<String> packages) {
    this.packages = packages;
  }

  /**
   * @param str
   */
  public final void addPackage(final String str) {
    getPackages().add(str);
  }

  /**
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    this.cache = new NpmCache(this.basedir);
    try {
      this.tempInstall = File.createTempFile("smaller-node-builder-temp",
          ".dir");
      this.tempInstall.delete();
      this.tempInstall.mkdirs();
      try {
        for (final String pkgSpec : this.packages) {
          getLog().info("Installing " + pkgSpec);
          final Package pkg = new Package(pkgSpec);
          pkg.install(this.tempInstall, this.tempInstall);
          FileUtils.copyDirectory(this.tempInstall, new File(
              getPackageTarget(), "node_modules"));
        }
      } finally {
        FileUtils.deleteDirectory(this.tempInstall);
      }
      FileUtils.write(new File(getPackageTarget(), "index.js"),
          new ST(IOUtils.toString(getClass().getResource("/index.js.tmpl")))
              .add("script", this.script).render());

      final String[] nameParts = this.name.split("-", 2);
      final String lowername = nameParts[0].toLowerCase();
      final String uppername = nameParts[0].substring(0, 1).toUpperCase()
          + nameParts[0].substring(1).toLowerCase();
      final String nameVersion = this.name;
      final String uppertype = this.type.toUpperCase();
      final File classbase = new File(this.basedir,
          "target/generated-sources/npm-processor");
      final File classtarget = new File(classbase, "de/matrixweb/smaller/"
          + lowername);
      classbase.mkdirs();
      FileUtils.write(
          new File(classtarget, uppername + "Processor.java"),
          new ST(IOUtils.toString(getClass()
              .getResource("/Processor.java.tmpl")))
              .add("lowername", lowername).add("uppername", uppername)
              .add("name", nameParts[0]).add("nameVersion", nameVersion)
              .add("uppertype", uppertype).add("merging", this.merging)
              .render());

      this.project.addCompileSourceRoot(classbase.getPath());
      final Resource resource = new Resource();
      resource.setDirectory(this.target.getAbsolutePath());
      this.project.addResource(resource);
    } catch (final IOException e) {
      throw new MojoExecutionException("Failed to connect to npm registry", e);
    }
  }

  private File getPackageTarget() {
    return new File(this.target, this.name);
  }

  class Package {

    private String name;

    private String version;

    private Descriptor descriptor;

    Package(final String input) {
      final int idx = input.indexOf('@');
      if (idx > -1) {
        this.name = input.substring(0, idx);
        this.version = input.substring(idx + 1);
      } else {
        this.name = input;
        this.version = "";
      }
    }

    /**
     * @param version
     *          the version to set
     */
    public void setVersion(final String version) {
      this.version = version;
    }

    Descriptor getDescriptor() throws IOException {
      if (this.descriptor == null) {
        final String url = "http://registry.npmjs.org/" + this.name;
        InputStream stream = SmallerNodeBuilderMojo.this.cache.get(url);
        if (stream == null) {
          getLog().debug("Requesting " + url);
          final Response response = Request.Get(url).execute();
          SmallerNodeBuilderMojo.this.cache.put(url, response.returnContent()
              .asStream());
          stream = SmallerNodeBuilderMojo.this.cache.get(url);
        }
        this.descriptor = SmallerNodeBuilderMojo.this.om.readValue(stream,
            Descriptor.class);
      }
      return this.descriptor;
    }

    InputStream downloadDist(final Version ver) throws IOException {
      final String url = ver.getDist().getTarball();
      InputStream in = SmallerNodeBuilderMojo.this.cache.get(url);
      if (in == null) {
        getLog().info("Downloading " + url);
        final Response response = Request.Get(url).execute();
        SmallerNodeBuilderMojo.this.cache.put(url, response.returnContent()
            .asStream());
        in = SmallerNodeBuilderMojo.this.cache.get(url);
      }
      return in;
    }

    void install(final File root, final File installDir) throws IOException {
      String selectedVersion = this.version;
      if ("".equals(selectedVersion)) {
        selectedVersion = getDescriptor().getDistTags().getLatest();
      }
      final Version ver = getDescriptor().getVersions().get(selectedVersion);

      final File pkgDir = new File(installDir, this.name);
      try {
        final InputStream in = downloadDist(ver);
        try {
          Extractor.uncompress(getLog(), in, pkgDir);
        } finally {
          IOUtils.closeQuietly(in);
        }
      } catch (final CompressorException e) {
        throw new IOException("Failed to decompress " + this.name + '@'
            + selectedVersion, e);
      } catch (final ArchiveException e) {
        throw new IOException("Failed to decompress " + this.name + '@'
            + selectedVersion, e);
      }

      for (final Entry<String, String> dependency : ver.getDependencies()
          .entrySet()) {
        final String pkgName = dependency.getKey();
        final String requiredVersion = dependency.getValue();
        getLog().debug(
            "Looking for " + pkgName + '@' + requiredVersion
                + " in parent folders of " + pkgDir);
        final String foundVersion = findInstalledVersion(root, pkgDir, pkgName);
        if (foundVersion == null
            || !new Range(requiredVersion).satisfies(ParsedVersion
                .parse(foundVersion))) {
          final Package depPkg = new Package(pkgName);
          depPkg.setVersion(SemanticVersion.getBestMatch(depPkg.getDescriptor()
              .getVersions().keySet(), requiredVersion));
          depPkg.install(root, new File(pkgDir, "node_modules"));
        }
      }
    }

    private String findInstalledVersion(final File root, final File dir,
        final String pkgName) throws IOException {
      String version = null;

      final File nodeModulesDir = new File(dir, "node_modules");
      final File pkgDir = new File(nodeModulesDir, pkgName);
      if (pkgDir.exists()) {
        getLog().debug("  searching " + pkgDir + "...");
        final Map<String, Object> map = SmallerNodeBuilderMojo.this.om
            .readValue(new File(pkgDir, "package.json"),
                new TypeReference<Map<String, Object>>() {
                });
        version = (String) map.get("version");
        getLog().debug("  found version " + version);
      } else if (!dir.getParentFile().equals(root)) {
        version = findInstalledVersion(root, dir.getParentFile(), pkgName);
      }

      return version;
    }

  }

}

class Extractor {

  static void uncompress(final Log log, final InputStream in, final File target)
      throws IOException, CompressorException, ArchiveException {
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

  private static void extractEntry(final Log log, final File target,
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
