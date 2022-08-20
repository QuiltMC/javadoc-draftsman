package org.quiltmc.draftsman.test.input;

public enum TestComplexEnum {
    FIRST(new TestClass[] { new TestClass("1") }, 0.5F, true, new TestRecord[]{ new TestRecord("1", 1, 1) }, new TestRecord[]{ new TestRecord("1", 1, 1) }),
    SECOND(new TestClass[] { new TestClass("2") }, 0.75F, false, new TestRecord[]{ new TestRecord("2", 2, 2) }, new TestRecord[]{ new TestRecord("1", 1, 1) }),
    THIRD(new TestClass[] { new TestClass("2") }, 1.0F, false, new TestRecord[]{ new TestRecord("2", 2, 2) }, new TestRecord[]{ new TestRecord("3", 3, 3) });

    private final TestClass[] testClasses;
    private final float f;
    private final boolean bool;
    private final TestRecord[] a;
    private final TestRecord[] b;

    TestComplexEnum(TestClass[] testClasses, float f, boolean bool, TestRecord[] a, @Deprecated TestRecord[] b) {
        this.testClasses = testClasses;
        this.f = f;
        this.bool = bool;
        this.a = a;
        this.b = b;
    }
}
