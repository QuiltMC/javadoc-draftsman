package org.quiltmc.draftsman.test.input;

public record TestRecordMultipleConstructors(int i, int j, String s, boolean b) {
  public TestRecordMultipleConstructors(int i, int j, String s) {
    this(i, j, s, false);
    System.out.println(s);
  }
}
