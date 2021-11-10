package org.quiltmc.draftsman.asm;

import java.util.List;

public record Insn(int opcode, List<Object> args) {
    public Insn(int opcode, Object... args) {
        this(opcode, List.of(args));
    }
}
