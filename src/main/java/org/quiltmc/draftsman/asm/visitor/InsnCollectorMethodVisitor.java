package org.quiltmc.draftsman.asm.visitor;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.quiltmc.draftsman.Draftsman;
import org.quiltmc.draftsman.asm.Insn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InsnCollectorMethodVisitor extends MethodVisitor implements Opcodes {
    private final List<Integer> opcodes;
    private final List<Insn> insns = new ArrayList<>();

    public InsnCollectorMethodVisitor(MethodVisitor methodVisitor, int... opcodes) {
        super(Draftsman.ASM_VERSION, methodVisitor);
        this.opcodes = Arrays.stream(opcodes).boxed().toList();
    }

    public InsnCollectorMethodVisitor(MethodVisitor methodVisitor, List<Integer> opcodes) {
        super(Draftsman.ASM_VERSION, methodVisitor);
        this.opcodes = opcodes;
    }

    public List<Insn> getInsns() {
        return insns;
    }

    private void collect(int opcode, Object... args) {
        if (opcodes.contains(opcode)) {
            insns.add(new Insn(opcode, args));
        }
    }

    // insn visitors
    @Override
    public void visitInsn(int opcode) {
        collect(opcode);
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        collect(opcode, operand);
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        collect(opcode, var);
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        collect(opcode, type);
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        collect(opcode, owner, name, descriptor);
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        collect(opcode, owner, name, descriptor, isInterface);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        collect(INVOKEDYNAMIC, name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        collect(opcode, label);
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        collect(LDC, value);
        super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        collect(IINC);
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        collect(TABLESWITCH, min, max, dflt, labels);
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        collect(LOOKUPSWITCH, dflt, keys, labels);
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        collect(MULTIANEWARRAY, descriptor, numDimensions);
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
    }
}
