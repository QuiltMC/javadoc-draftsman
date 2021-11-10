package org.quiltmc.draftsman.test.input;

public class TestExtending extends TestClass {
    private final String fieldD;
    private int fieldE;

    public TestExtending(String fieldC, String fieldD, int fieldE) {
        super(fieldC);
        this.fieldD = fieldD;
        this.fieldE = fieldE;
    }
}
