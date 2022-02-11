package org.quiltmc.draftsman.asm;

import java.util.List;

/**
 * Represents an instruction by its opcode and arguments.
 *
 * <p>
 * The type and size of the {@link #args} depends on the opcode.
 * For example
 * <ul>
 *     <li>{@code opcode} is {@link org.objectweb.asm.Opcodes#ILOAD iload} and {@link #args} is a single {@code int}</li>
 *     <li>{@code opcode} is {@link org.objectweb.asm.Opcodes#INVOKESTATIC invokestatic} and {@link #args} is three {@link String} and a {@code boolean}</li>
 *     <li>{@code opcode} is {@link org.objectweb.asm.Opcodes#ICONST_0 iconst_0} and {@link #args} is empty</li>
 * </ul>
 */
public record Insn(int opcode, List<Object> args) {
    public Insn(int opcode, Object... args) {
        this(opcode, List.of(args));
    }

    public Object getArg(int index) {
        return args.get(index);
    }
}
