package org.quiltmc.draftsman.test.input;

public enum TestEnumAnonymous {
    FIRST {
        @Override
        public void foo() {
            System.out.println("Hello world");
        }
    },
    SECOND {
        @Override
        public void foo() {
            System.out.println("Goodbye world");
        }
    };

    public void foo() {
    }
}
