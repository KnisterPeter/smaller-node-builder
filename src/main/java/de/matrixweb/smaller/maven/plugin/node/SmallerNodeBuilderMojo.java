package de.matrixweb.smaller.maven.plugin.node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.stringtemplate.v4.ST;

/**
 * @author markusw
 */
@Mojo(name = "smaller-node-builder", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class SmallerNodeBuilderMojo extends AbstractMojo {

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

  @Parameter(alias = "npm-only", defaultValue = "false")
  private boolean npmOnly;

  /**
   * The folder to store the package and its requirements. Defaults to
   * ${basedir}/target/generated-resources/npm-modules.
   */
  @Parameter(defaultValue = "${basedir}/target/generated-resources/npm-modules")
  private File target;

  private File tempInstall;

  @Parameter(alias = "script-file")
  private File scriptFile;

  /**
   * @deprecated Use {@link #useRuntimeScript} instead
   */
  @Deprecated
  @Parameter(alias = "skip-script")
  private Boolean skipScript;

  @Parameter(alias = "use-runtime-script", defaultValue = "false")
  private boolean useRuntimeScript;

  /**
   * This script is the main entry-point into the node-module and builds the
   * bridge to the JVM.
   */
  @Parameter
  private String script;

  /**
   * Set this to true to force an update of the package.
   */
  @Parameter(defaultValue = "false")
  private boolean forceUpdate;

  /**
   * Set this to true to delete 'test', 'tests', 'example' and 'examples'
   * folders.
   */
  @Parameter(alias = "delete-test-and-example", defaultValue = "true")
  private boolean deleteTestAndExampleFolders;

  private NpmCache cache;

  /**
   * @param skipScript
   *          the skipScript to set
   * @deprecated Use <code>use-runtime-script</code> instead
   */
  @Deprecated
  public void setSkipScript(final boolean skipScript) {
    this.useRuntimeScript = skipScript;
  }

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
    if (this.script == null && this.scriptFile == null) {
      throw new MojoFailureException("Either script or script-file is required");
    }

    this.cache = new NpmCache(this.basedir);
    try {
      this.tempInstall = File.createTempFile("smaller-node-builder-temp",
          ".dir");
      this.tempInstall.delete();
      this.tempInstall.mkdirs();
      try {
        for (final String pkgSpec : this.packages) {
          getLog().info("Installing " + pkgSpec);
          final PackageInfo pkg = PackageInfo.createPackage(pkgSpec,
              new MavenLogger(), this.cache);
          pkg.install(this.tempInstall, this.tempInstall,
              this.deleteTestAndExampleFolders);
          FileUtils.copyDirectory(this.tempInstall, new File(
              getPackageTarget(), "node_modules"));
        }
      } finally {
        FileUtils.deleteDirectory(this.tempInstall);
      }
      final String javaCodeFolder = writeSources();
      updateProjectModel(javaCodeFolder);
    } catch (final IOException e) {
      throw new MojoExecutionException("Failed to connect to npm registry", e);
    }
  }

  private String writeSources() throws IOException {
    if (this.scriptFile != null) {
      if (!this.useRuntimeScript) {
        FileUtils.copyFile(this.scriptFile, new File(getPackageTarget(),
            "index.js"));
      }
    } else {
      FileUtils.write(new File(getPackageTarget(), "index.js"),
          new ST(IOUtils.toString(getClass().getResource("/index.js.tmpl")))
              .add("script", this.script).render());
    }

    final File classbase = new File(this.basedir,
        "target/generated-sources/npm-processor");
    if (!this.npmOnly) {
      final String[] nameParts = this.name.split("-", 2);
      final String lowername = nameParts[0].toLowerCase();
      final String uppername = nameParts[0].substring(0, 1).toUpperCase()
          + nameParts[0].substring(1).toLowerCase();
      final String nameVersion = this.name;
      final String uppertype = this.type.toUpperCase();
      final File classtarget = new File(classbase, "de/matrixweb/smaller/"
          + lowername);
      String scriptName = "null";
      if (this.useRuntimeScript && this.scriptFile != null) {
        scriptName = this.scriptFile.getAbsolutePath();
        if (scriptName.startsWith(this.basedir.getAbsolutePath() + '/')) {
          scriptName = scriptName
              .substring((this.basedir.getAbsolutePath() + '/').length());
        }
        if (scriptName.startsWith("src/main/resources/")) {
          scriptName = scriptName.substring("src/main/resources/".length());
        }
        scriptName = '"' + scriptName + '"';
        getLog().info("Using runtime script: " + scriptName);
      }
      classbase.mkdirs();
      FileUtils.write(
          new File(classtarget, uppername + "Processor.java"),
          new ST(IOUtils.toString(getClass()
              .getResource("/Processor.java.tmpl")))
              .add("lowername", lowername).add("uppername", uppername)
              .add("name", nameParts[0]).add("nameVersion", nameVersion)
              .add("version", nameParts[1]).add("uppertype", uppertype)
              .add("merging", this.merging).add("scriptName", scriptName)
              .render());
    }
    return classbase.getPath();
  }

  private void updateProjectModel(final String javaCodeFolder) {
    if (!this.npmOnly) {
      this.project.addCompileSourceRoot(javaCodeFolder);
    }
    final Resource resource = new Resource();
    resource.setDirectory(this.target.getAbsolutePath());
    this.project.addResource(resource);
  }

  private File getPackageTarget() {
    return new File(this.target, this.name);
  }

  private class MavenLogger implements Logger {

    /**
     * @see de.matrixweb.smaller.maven.plugin.node.Logger#info(java.lang.String)
     */
    @Override
    public void info(final String message) {
      getLog().info(message);
    }

    /**
     * @see de.matrixweb.smaller.maven.plugin.node.Logger#debug(java.lang.String)
     */
    @Override
    public void debug(final String message) {
      getLog().debug(message);
    }

  }

}
