package org.quiltmc.draftsman.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.quiltmc.draftsman.asm.adapter.FieldValueEraserAdapter;
import org.quiltmc.draftsman.asm.adapter.MethodEraserAdapter;

import java.io.PrintWriter;

public class DraftsmanClassTransformer {
    private static final PrintWriter TRACE_WRITER = new PrintWriter(System.out);
    private final ClassReader reader;
    private final ClassWriter writer;
    private final ClassVisitor classVisitor;

    public DraftsmanClassTransformer(byte[] classFile, boolean trace) {
        reader = new ClassReader(classFile);
        writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        classVisitor = trace ? new TraceClassVisitor(writer, TRACE_WRITER) : writer;
    }

    public byte[] transform() {
        // reader -> fieldValueEraser -> methodEraser -> classVisitor (writer | tracer -> writer)
        MethodEraserAdapter methodEraserAdapter = new MethodEraserAdapter(classVisitor);
        FieldValueEraserAdapter fieldValueEraserAdapter = new FieldValueEraserAdapter(methodEraserAdapter);
        reader.accept(fieldValueEraserAdapter, 0);
        return writer.toByteArray();
    }
}
