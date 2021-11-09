package org.quiltmc.draftsman.test.input;

public class TestDoubleNestedClass {
    public static final String STR = "java/lang/Object";

    public abstract static class Inner {
        public static final int INT = 90210;
        public int x;

        abstract InnerInner getInner();

        public class InnerInner {
            public void test() {
                System.out.println("test");
            }
        }
    }
}
