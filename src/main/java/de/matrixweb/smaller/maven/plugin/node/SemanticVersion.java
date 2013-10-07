package de.matrixweb.smaller.maven.plugin.node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SemanticVersion {

  static String getBestMatch(final Collection<String> versions, final String str)
      throws IOException {
    final String version = str.trim();
    final Range range = new Range(version);

    final List<ParsedVersion> pkgVersions = new ArrayList<ParsedVersion>();
    for (final String dv : versions) {
      pkgVersions.add(ParsedVersion.parse(dv));
    }
    Collections.sort(pkgVersions, Collections.reverseOrder());

    ParsedVersion match = null;
    final Iterator<ParsedVersion> it = pkgVersions.iterator();
    while (match == null && it.hasNext()) {
      final ParsedVersion pkgVer = it.next();
      if (range.satisfies(pkgVer)) {
        match = pkgVer;
      }
    }

    return match.toString();
  }

}

class ParsedVersion implements Comparable<ParsedVersion> {

  private static final String NUM = "0|[1-9][0-9]*";
  private static final String NONNUM = "[0-9]*[-a-zA-Z][-a-zA-Z0-9]*";
  private static final String MAIN = "(" + NUM + ")(?:\\.(" + NUM
      + "))?(?:\\.(" + NUM + "))?";
  private static final String PREREL = "(?:-((?:" + NUM + "|" + NONNUM
      + ")(?:\\.(?:" + NUM + "|" + NONNUM + "))*))";
  private static final String BUILD = "(?:\\+([-a-zA-Z0-9]+(?:\\.[-a-zA-Z0-9]+)*))";
  private static final Pattern FULL = Pattern.compile("v?" + MAIN + PREREL
      + "?" + BUILD + "?");

  private int major = 0;
  private int minor = 0;
  private int patch = 0;
  private String pre = null;
  private String build = null;

  static ParsedVersion parse(final String str) {
    final Matcher matcher = FULL.matcher(str);
    if (matcher.matches()) {
      final ParsedVersion v = new ParsedVersion();
      if (matcher.groupCount() > 0) {
        v.major = Integer.parseInt(matcher.group(1));
      }
      if (matcher.groupCount() > 1 && matcher.group(2) != null) {
        v.minor = Integer.parseInt(matcher.group(2));
      }
      if (matcher.groupCount() > 2 && matcher.group(3) != null) {
        v.patch = Integer.parseInt(matcher.group(3));
      }
      if (matcher.groupCount() > 3) {
        v.pre = matcher.group(4);
      }
      if (matcher.groupCount() > 4) {
        v.build = matcher.group(5);
      }
      return v;
    }
    return null;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final ParsedVersion o) {
    if (this.major == o.major) {
      if (this.minor == o.minor) {
        if (this.patch == o.patch) {
          return 0;
        }
        return this.patch - o.patch;
      }
      return this.minor - o.minor;
    }
    return this.major - o.major;
  }

  @Override
  public String toString() {
    return this.major + "." + this.minor + "." + this.patch
        + (this.pre != null ? "-" + this.pre : "")
        + (this.build != null ? "+" + this.build : "");
  }

}

class Range {

  private enum Op {
    LT("<"), LE("<="), EQ("="), GE(">="), GT(">");

    private final String op;

    private Op(final String op) {
      this.op = op;
    }

    static Op lookup(final String str) {
      if (str == null) {
        return EQ;
      }
      for (final Op op : values()) {
        if (op.op.equals(str)) {
          return op;
        }
      }
      throw new IllegalArgumentException(str);
    }

    boolean compare(final ParsedVersion v1, final ParsedVersion v2) {
      switch (this) {
      case LT:
        return v1.compareTo(v2) < 0;
      case LE:
        return v1.compareTo(v2) <= 0;
      case EQ:
        return v1.compareTo(v2) == 0;
      case GE:
        return v1.compareTo(v2) >= 0;
      case GT:
        return v1.compareTo(v2) > 0;
      default:
        return false;
      }
    }
  }

  private static final String OPS = "~|<|<=|=|>=|>";

  private static final Pattern OP_MATCH = Pattern.compile("^(" + OPS
      + ")?\\s?(.*)$");

  private final Map<ParsedVersion, Op> opMap = new TreeMap<ParsedVersion, Op>();

  Range(String str) {
    str = str.replaceAll("(" + OPS + ")\\s+", "$1");
    final String[] splits = str.split(" ");
    for (final String split : splits) {
      final Matcher matcher = OP_MATCH.matcher(split);
      if (!matcher.matches()) {
        throw new IllegalArgumentException(str);
      }
      final String verStr = matcher.group(2);
      final String op = matcher.group(1);
      if ("~".equals(op) && !(verStr.contains("x") || verStr.contains("X"))) {
        this.opMap.putAll(new Range(createTildeVersion(verStr)).opMap);
      } else if (verStr.contains("x") || verStr.contains("X")) {
        this.opMap.putAll(new Range(createXVersion(verStr)).opMap);
      } else {
        this.opMap.put(ParsedVersion.parse(verStr), Op.lookup(op));
      }
    }
  }

  private String createTildeVersion(final String str) {
    final StringBuilder sb = new StringBuilder();

    final String[] m = str.split("[-\\+\\.]", 4);
    final int[] n = new int[Math.min(3, m.length)];
    for (int i = 0; i < n.length; i++) {
      n[i] = m[i].equalsIgnoreCase("x") ? 0 : Integer.parseInt(m[i]);
    }

    sb.append(">=").append(n[0]);
    for (int i = 1; i < n.length; i++) {
      sb.append(".").append(n[i]);
    }
    if (n.length == 3 && n.length - 2 >= 0) {
      n[n.length - 2]++;
      n[n.length - 1] = 0;
    } else {
      n[n.length - 1]++;
    }
    sb.append(" <").append(n[0]);
    for (int i = 1; i < n.length; i++) {
      sb.append(".").append(n[i]);
    }

    return sb.toString();
  }

  private String createXVersion(final String str) {
    final StringBuilder sb = new StringBuilder();

    final String[] m = str.split("[-\\+\\.]", 4);
    final int[] n = new int[Math.min(3, m.length)];
    for (int i = 0; i < n.length; i++) {
      n[i] = m[i].equalsIgnoreCase("x") ? 0 : Integer.parseInt(m[i]);
    }

    sb.append(">=").append(n[0]);
    for (int i = 1; i < n.length; i++) {
      sb.append(".").append(n[i]);
    }
    boolean incOnce = false;
    sb.append(" <");
    if (m.length > 1 && m[1].equalsIgnoreCase("x")) {
      sb.append(n[0] + 1);
      incOnce = true;
    } else {
      sb.append(n[0]);
    }
    for (int i = 1; i < n.length; i++) {
      if (!incOnce && m.length > i + 1 && m[i + 1].equalsIgnoreCase("x")) {
        sb.append(".").append(n[i] + 1);
      } else {
        sb.append(".").append(n[i]);
      }
    }

    return sb.toString();
  }

  boolean satisfies(final ParsedVersion version) {
    boolean satisfies = true;
    for (final Entry<ParsedVersion, Op> entry : this.opMap.entrySet()) {
      satisfies &= entry.getValue().compare(version, entry.getKey());
    }
    return satisfies;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (final Entry<ParsedVersion, Op> entry : this.opMap.entrySet()) {
      sb.append(entry.getValue().op).append(entry.getKey()).append(' ');
    }
    return sb.toString().trim();
  }

}
