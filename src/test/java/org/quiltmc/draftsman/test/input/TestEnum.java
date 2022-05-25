package org.quiltmc.draftsman.test.input;

public enum TestEnum {
    ONE("one"),
    TWO("two"),
    THREE("three"),
    FOUR("four"),
    FIVE("five"),
    SIX("six"),
    SEVEN("seven");

    private static String CONSTANT = "constant";
    private final String s;

    TestEnum(String s) {
        this.s = s;
    }
}
