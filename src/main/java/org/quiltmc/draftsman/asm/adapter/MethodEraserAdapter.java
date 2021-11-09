package org.quiltmc.draftsman.asm.adapter;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.quiltmc.draftsman.Draftsman;

public class MethodEraserAdapter extends ClassVisitor implements Opcodes {
    public MethodEraserAdapter(ClassVisitor classVisitor) {
        super(Draftsman.ASM_VERSION, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.equals("<init>") || name.equals("<clinit>")) {
            return super.visitMethod(access, name, descriptor, signature, exceptions); // FieldValueEraserAdapter uses these
        }

        if ((access & ACC_ABSTRACT) != 0) {
            return super.visitMethod(access, name, descriptor, signature, exceptions); // Abstract methods don't need to be erased
        }

        return new Eraser(super.visitMethod(access, name, descriptor, signature, exceptions));
    }

    public static class Eraser extends MethodVisitor {
        public Eraser(MethodVisitor methodVisitor) {
            super(Draftsman.ASM_VERSION, methodVisitor);
        }

        @Override
        public void visitEnd() {
            super.visitCode();
            super.visitTypeInsn(NEW, "java/lang/AbstractMethodError");
            super.visitInsn(DUP);
            super.visitMethodInsn(INVOKESPECIAL, "java/lang/AbstractMethodError", "<init>", "()V", false);
            super.visitInsn(ATHROW);
            super.visitMaxs(0, 0);
            super.visitEnd();
        }

        // Ignore bytecode
        @Override
        public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        }

        @Override
        public void visitInsn(int opcode) {
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
        }

        @Override
        public void visitLabel(Label label) {
        }

        @Override
        public void visitLdcInsn(Object value) {
        }

        @Override
        public void visitIincInsn(int var, int increment) {
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            return null;
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            return null;
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
            return null;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
        }
    }
}
