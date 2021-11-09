package org.quiltmc.draftsman.test.input;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleTestClass {
    public static final String TEST_STRING = "test";
    public static final AtomicInteger TEST_ATOMIC = new AtomicInteger(0);
    public final Date testDate = new Date();
    private final int i = 64;
    public boolean testBoolean;

    public static void m() {
        System.out.println("Hello, World!");
    }

    private SimpleTestClass() {
        throw new AbstractMethodError();
    }
}
