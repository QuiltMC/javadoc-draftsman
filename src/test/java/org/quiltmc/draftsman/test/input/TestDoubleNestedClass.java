package org.quiltmc.draftsman.test.input;

import java.util.List;

public class TestDoubleNestedClass {
    public static final String STR = "java/lang/Object";

    public abstract static class Inner {
        public static final int INT = 90210;
        public int x;

        abstract InnerInner getInner();

        public class InnerInner {
            private final List<Runnable> callbacks;

            public InnerInner(List<Runnable> callbacks) {
                this.callbacks = callbacks;
            }

            public void test() {
                System.out.println("test");
            }
        }
    }
}
