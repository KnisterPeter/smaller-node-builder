package de.matrixweb.smaller.maven.plugin.node;

import java.util.Collections;
import java.util.Map;

/**
 * @author markusw
 */
public class Descriptor {

  private String name;

  private Descriptor.DistTags distTags;

  private Map<String, Descriptor.Version> versions;

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
   * @return the distTags
   */
  public Descriptor.DistTags getDistTags() {
    return this.distTags;
  }

  /**
   * @param distTags
   *          the distTags to set
   */
  public void setDistTags(final Descriptor.DistTags distTags) {
    this.distTags = distTags;
  }

  /**
   * @return the versions
   */
  public Map<String, Descriptor.Version> getVersions() {
    return this.versions;
  }

  /**
   * @param versions
   *          the versions to set
   */
  public void setVersions(final Map<String, Descriptor.Version> versions) {
    this.versions = versions;
  }

  /** */
  public static class DistTags {

    private String latest;

    /**
     * @return the latest
     */
    public String getLatest() {
      return this.latest;
    }

    /**
     * @param latest
     *          the latest to set
     */
    public void setLatest(final String latest) {
      this.latest = latest;
    }

  }

  /** */
  public static class Version {

    private Map<String, String> dependencies;

    private Version.Dist dist;

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

    /**
     * @return the dist
     */
    public Version.Dist getDist() {
      return this.dist;
    }

    /**
     * @param dist
     *          the dist to set
     */
    public void setDist(final Version.Dist dist) {
      this.dist = dist;
    }

    /** */
    public static class Dist {

      private String tarball;

      /**
       * @return the tarball
       */
      public String getTarball() {
        return this.tarball;
      }

      /**
       * @param tarball
       *          the tarball to set
       */
      public void setTarball(final String tarball) {
        this.tarball = tarball;
      }

    }

  }

}
