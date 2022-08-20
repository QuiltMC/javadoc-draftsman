package org.quiltmc.draftsman.test.input;

public record TestRecord(String str, int i, double d){
    public static final String STR = "str";
    public static final int X = 230;

    public TestRecord(String str, int i) {
        this(str, i, i);
    }
}
