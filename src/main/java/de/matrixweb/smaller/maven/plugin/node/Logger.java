package de.matrixweb.smaller.maven.plugin.node;

/**
 * @author markusw
 */
public interface Logger {

  /**
   * @param message
   */
  void info(String message);

  /**
   * @param message
   */
  void debug(String message);

}
