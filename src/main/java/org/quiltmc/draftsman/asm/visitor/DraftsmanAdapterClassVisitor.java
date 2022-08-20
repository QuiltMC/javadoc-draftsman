package org.quiltmc.draftsman.asm.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.draftsman.asm.DraftsmanClassAdapter;

/**
 * A wrapper class visitor around {@link DraftsmanClassAdapter}.
 */
public class DraftsmanAdapterClassVisitor extends ClassNode {
    private final ClassVisitor visitor;

    /**
     * @param visitor the visitor to use after the class has been adapted
     */
    public DraftsmanAdapterClassVisitor(ClassVisitor visitor) {
        super(Opcodes.ASM9);

        this.visitor = visitor;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        DraftsmanClassAdapter.adapt(this, this.visitor);
    }
}
