package org.quiltmc.draftsman.asm.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.quiltmc.draftsman.asm.DraftsmanClassAdapter;

import java.util.Random;

/**
 * A wrapper class visitor around {@link DraftsmanClassAdapter}.
 */
public class DraftsmanAdapterClassVisitor extends ClassNode {
    private final ClassVisitor visitor;
    private final boolean fixRecordFields;

    /**
     * @param visitor the visitor to use after the class has been adapted
     *
     * @see #DraftsmanAdapterClassVisitor(ClassVisitor, boolean)
     */
    public DraftsmanAdapterClassVisitor(ClassVisitor visitor) {
        this(visitor, true);
    }

    /**
     * @param visitor         the visitor to use after the class has been adapted
     * @param fixRecordFields whether to fix wrong record field access
     *
     * @see #DraftsmanAdapterClassVisitor(ClassVisitor)
     */
    public DraftsmanAdapterClassVisitor(ClassVisitor visitor, boolean fixRecordFields) {
        super(Opcodes.ASM9);

        this.visitor = visitor;
        this.fixRecordFields = fixRecordFields;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();

        if (this.fixRecordFields && (access & Opcodes.ACC_RECORD) != 0) {
            for (FieldNode field : this.fields) {
                if ((field.access & Opcodes.ACC_STATIC) == 0 && (field.access & Opcodes.ACC_PRIVATE) == 0) {
                    field.access |= Opcodes.ACC_PRIVATE;
                }
            }
        }

        DraftsmanClassAdapter.adapt(this, this.visitor);
    }
}
