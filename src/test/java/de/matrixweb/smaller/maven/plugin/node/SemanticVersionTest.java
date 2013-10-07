package de.matrixweb.smaller.maven.plugin.node;

import org.junit.Test;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

/**
 * @author markusw
 */
public class SemanticVersionTest {

  /** */
  @Test
  public void test() {
    assertThat(new Range("2.x").toString(), is(">=2.0.0 <3.0.0"));
    assertThat(new Range("2.0.x").toString(), is(">=2.0.0 <2.1.0"));
    assertThat(new Range("2.x.x").toString(), is(">=2.0.0 <3.0.0"));

    assertThat(new Range("~2").toString(), is(">=2.0.0 <3.0.0"));
    assertThat(new Range("~2.x").toString(), is(">=2.0.0 <3.0.0"));
    assertThat(new Range("~2.x.x").toString(), is(">=2.0.0 <3.0.0"));
    assertThat(new Range("~2.0.x").toString(), is(">=2.0.0 <2.1.0"));
    assertThat(new Range("~1.2").toString(), is(">=1.2.0 <1.3.0"));
    assertThat(new Range("~1.2.x").toString(), is(">=1.2.0 <1.3.0"));
    assertThat(new Range("~1.2.3").toString(), is(">=1.2.3 <1.3.0"));
    assertThat(new Range("~1.2.0").toString(), is(">=1.2.0 <1.3.0"));
  }

}
