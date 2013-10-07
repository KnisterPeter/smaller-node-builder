package de.matrixweb.smaller.maven.plugin.node;

import java.util.Collections;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author markusw
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Descriptor {

  @JsonProperty("name")
  private String name;

  @JsonProperty("dist-tags")
  private Descriptor.DistTags distTags;

  @JsonProperty("versions")
  private Map<String, Descriptor.Version> versions;

  /**
   * @return the name
   */
  public String getName() {
    return this.name;
  }

  /**
   * @return the distTags
   */
  public Descriptor.DistTags getDistTags() {
    return this.distTags;
  }

  /**
   * @return the versions
   */
  public Map<String, Descriptor.Version> getVersions() {
    return this.versions;
  }

  /** */
  public static class DistTags {

    @JsonProperty("latest")
    private String latest;

    /**
     * @return the latest
     */
    public String getLatest() {
      return this.latest;
    }

  }

  /** */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Version {

    @JsonProperty("engine")
    private Object engine;

    @JsonProperty("dependencies")
    private Map<String, String> dependencies;

    @JsonProperty("dist")
    private Version.Dist dist;

    /**
     * @return the engine
     */
    public Object getEngine() {
      return this.engine;
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
     * @return the dist
     */
    public Version.Dist getDist() {
      return this.dist;
    }

    /** */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dist {

      @JsonProperty("tarball")
      private String tarball;

      /**
       * @return the tarball
       */
      public String getTarball() {
        return this.tarball;
      }

    }

  }

}
