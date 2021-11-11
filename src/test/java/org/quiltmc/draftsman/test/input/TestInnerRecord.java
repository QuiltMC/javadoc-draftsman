package org.quiltmc.draftsman.test.input;

public class TestInnerRecord {
    public static final String INNER_RECORD_NAME = "InnerRecord";

    static record InnerRecord(String name, long l, int i, byte b, boolean z, TestRecord r, TestInnerRecord r2) {
        public InnerRecord(String name, long l, int i, byte b, boolean z, TestRecord r, TestInnerRecord r2) {
            this.name = name;
            this.l = l;
            this.i = i;
            this.b = b;
            this.z = z;
            this.r = r;
            this.r2 = r2;
        }
    }
}
