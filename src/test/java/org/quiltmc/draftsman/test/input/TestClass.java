package org.quiltmc.draftsman.test.input;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class TestClass {
    public static final int STATIC_PRIMITIVE_CONSTANT_A = 1234;
    public static final double STATIC_PRIMITIVE_CONSTANT_B = 3.14159;
    public static final String STATIC_PRIMITIVE_CONSTANT_C = "Hello world";
    public static final Comparator<String> STATIC_CONSTANT_A = Comparator.comparing(String::length);
    public static final Function<String, Integer> STATIC_CONSTANT_B = s -> s.length() + s.hashCode();
    public static final Comparator<String> STATIC_CONSTANT_C = Comparator.comparingInt(STATIC_CONSTANT_B::apply);
    public static final Pattern STATIC_CONSTANT_D = Pattern.compile("\\d+");
    public static List<String> STATIC_FIELD_A = new ArrayList<>(List.of("a", "b", "c"));
    public static Map<Integer, String> STATIC_FIELD_B = new AbstractMap<>() {
        @Override
        public Set<Entry<Integer, String>> entrySet() {
            return null;
        }
    };
    private static Comparator<Integer> STATIC_FIELD_C = Comparator.comparing(Integer::intValue);
    public static AtomicReference<?> STATIC_FIELD_D;
    public static String staticPrimitiveFieldA = "Foo bar";
    public static int staticPrimitiveFieldB = 256;
    public final int primitiveConstantA = 5678;
    public final double primitiveConstantB = 2.71828;
    public final String primitiveConstantC = "Goodbye world";
    private final Supplier<String> constantA = () -> STATIC_PRIMITIVE_CONSTANT_C;
    protected final Set<Integer> constantB = new HashSet<>();
    private final Deque<Integer> constantC;
    public int fieldA = 0;
    public double fieldB = 1.414213;
    private String fieldC;
    private boolean fieldD;

    public TestClass(String fieldC) {
        this.constantC = new ArrayDeque<>();
        this.fieldC = fieldC;
    }

    private TestClass(String fieldC, Deque<Integer> constantC) {
        this.fieldC = fieldC;
        this.constantC = constantC;
    }

    private TestClass(Deque<Integer> constantC) {
        this("", constantC);
    }

    public static TestClass create(String fieldC) {
        return new TestClass(fieldC);
    }

    public static TestClass create(String fieldC, boolean fieldD) {
        return new TestClass(fieldC) {{
            setFieldD(fieldD);
        }};
    }

    public static TestClass create(String fieldC, boolean fieldD, int fieldA1) {
        return new TestClass(fieldC) {{
            this.fieldA = fieldA1;
            setFieldD(fieldD);
        }};
    }

    public TestClass withConstantC(Deque<Integer> constantC) {
        int fieldA1 = this.fieldA;
        double fieldB1 = this.fieldB;
        boolean fieldD1 = this.fieldD;
        return new TestClass(this.fieldC, constantC) {{
            this.fieldA = fieldA1;
            this.fieldB = fieldB1;
            setFieldD(fieldD1);
        }};
    }

    public static Comparator<Integer> getStaticFieldC() {
        return STATIC_FIELD_C;
    }

    public static void setStaticFieldC(Comparator<Integer> staticFieldC) {
        STATIC_FIELD_C = staticFieldC;
    }

    public Set<Integer> getConstantB() {
        return this.constantB;
    }

    public Deque<Integer> getConstantC() {
        return this.constantC;
    }

    public String getFieldC() {
        return this.fieldC;
    }

    public void setFieldC(String fieldC) {
        this.fieldC = fieldC;
    }

    public boolean isFieldD() {
        return this.fieldD;
    }

    public void setFieldD(boolean fieldD) {
        this.fieldD = fieldD;
    }

    public abstract static class AbstractClass {
        public abstract void abstractMethod();
        protected abstract void protectedMethod();

        public void concreteMethod() {
            privateMethod();
        }

        private void privateMethod() {
            abstractMethod();
            protectedMethod();
        }
    }

    public interface Interface {
        void interfaceMethod();

        default void defaultMethod() {
            interfaceMethod();
            privateMethod();
        }

        int size();

        private void privateMethod() {
            System.out.println(STATIC_PRIMITIVE_CONSTANT_C);
        }
    }
}
