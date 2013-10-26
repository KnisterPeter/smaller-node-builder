package de.matrixweb.smaller.maven.plugin.node;

import java.util.Collections;
import java.util.Map;

/**
 * @author markusw
 */
public class PackageJson {

  private String name;

  private String version;

  private Map<String, String> dependencies;

  /**
   * @return the name
   */
  public String getName() {
    return this.name;
  }

  /**
   * @param name
   *          the name to set
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return this.version;
  }

  /**
   * @param version
   *          the version to set
   */
  public void setVersion(final String version) {
    this.version = version;
  }

  /**
   * @return the dependencies
   */
  @SuppressWarnings("unchecked")
  public Map<String, String> getDependencies() {
    return this.dependencies != null ? this.dependencies
        : Collections.EMPTY_MAP;
  }

  /**
   * @param dependencies
   *          the dependencies to set
   */
  public void setDependencies(final Map<String, String> dependencies) {
    this.dependencies = dependencies;
  }

}
