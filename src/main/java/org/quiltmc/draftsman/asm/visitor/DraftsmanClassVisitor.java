package org.quiltmc.draftsman.asm.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.quiltmc.draftsman.asm.adapter.FieldValueEraserAdapter;
import org.quiltmc.draftsman.asm.adapter.MethodEraserAdapter;

/**
 * Class visitor that erases code in a class, leaving only the method and field definitions.
 * </p>
 * Same as calling {@link FieldValueEraserAdapter} and {@link MethodEraserAdapter} in sequence.
 */
public class DraftsmanClassVisitor extends ClassVisitor {
    public DraftsmanClassVisitor(ClassVisitor classVisitor) {
        // FieldValueEraserAdapter -> MethodEraserAdapter -> classVisitor
        super(Opcodes.ASM9, new FieldValueEraserAdapter(new MethodEraserAdapter(classVisitor)));
    }
}
