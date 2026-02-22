package com.hytale.updater.transformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Core boilerplate for the Dynamic Bytecode Transformer.
 * Uses ObjectWeb ASM to statically transform compiled Java .class files within outdated Hytale mods.
 * The goal is to intercept outdated Hytale API references and rewrite them to match the new API.
 */
public class DynamicBytecodeTransformer {

    // A simple representation of our Translation Dictionary
    private final Map<String, MethodMapping> translationDictionary = new HashMap<>();

    public DynamicBytecodeTransformer() {
        // Load the Translation Dictionary populated by the Diff Engine.
        // Example: The mod calls OldPlayer.getHealth()I, but the Hytale API renamed it to NewPlayer.calculateHealth()I
        translationDictionary.put(
                "com/hytale/api/OldPlayer.getHealth()I",
                new MethodMapping("com/hytale/api/entities/Player", "calculateHealth", "()I")
        );
        
        // Example for field accesses or class renaming can also be added here.
    }

    /**
     * Transforms an outdated class file's bytecode based on the translation dictionary.
     *
     * @param classInputStream The raw bytes of the outdated mod's .class file from the .jar
     * @return The updated bytecode as a byte array to be repacked into the new .jar
     */
    public byte[] transformClass(InputStream classInputStream) throws IOException {
        // ClassReader parses the original class byte array
        ClassReader classReader = new ClassReader(classInputStream);

        // ClassWriter rebuilds the class file. COMPUTE_FRAMES is crucial as modifying instructions 
        // often shifts the local variable and stack map frames.
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        // ClassVisitor intercepts the ClassReader's events and forwards them to the ClassWriter,
        // allowing us to inject custom logic during the visit process.
        ClassVisitor classVisitor = new ModUpdatingClassVisitor(Opcodes.ASM9, classWriter);

        // Begin the visitor pattern pass. ClassReader sends events to ClassVisitor.
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);

        // Return the modified bytecode
        return classWriter.toByteArray();
    }

    /**
     * Custom ClassVisitor to intercept methods.
     */
    private class ModUpdatingClassVisitor extends ClassVisitor {
        public ModUpdatingClassVisitor(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            // Get the default MethodVisitor from the ClassWriter
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

            // Wrap it in our custom MethodVisitor to modify the instructions inside the method
            return new ModUpdatingMethodVisitor(api, mv);
        }
    }

    /**
     * Custom MethodVisitor to intercept method invocations within the bytecode.
     */
    private class ModUpdatingMethodVisitor extends MethodVisitor {
        public ModUpdatingMethodVisitor(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        /**
         * Intercepts standard method invocations like INVOKEVIRTUAL, INVOKESTATIC, INVOKESPECIAL, INVOKEINTERFACE.
         */
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            // Construct a signature key used in our Translation Dictionary
            String methodKey = owner + "." + name + descriptor;

            // Check if this method call is in our Translation Dictionary (i.e., it's a broken API reference)
            if (translationDictionary.containsKey(methodKey)) {
                MethodMapping newMapping = translationDictionary.get(methodKey);

                System.out.println("Rewriting method call: " + methodKey + " -> " + newMapping.getOwner() + "." + newMapping.getName() + newMapping.getDescriptor());

                // Rewrite the bytecode instruction to point to the new mapped Method
                // We emit the instruction to the parent MethodVisitor using the updated parameters.
                super.visitMethodInsn(opcode, newMapping.getOwner(), newMapping.getName(), newMapping.getDescriptor(), isInterface);
            } else {
                // If the method signature hasn't changed or it's not a Hytale API call, leave it intact
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        }
        
        /**
         * Intercepts field access instructions like GETFIELD, PUTFIELD, GETSTATIC, PUTSTATIC.
         */
        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
             // Similar logic would go here if field names/types were changed according to the Translation Dictionary.
             super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        /**
         * Intercepts type instructions like NEW, ANEWARRAY, CHECKCAST, INSTANCEOF.
         * Useful if a class was completely renamed in the new Hytale API.
         */
        @Override
        public void visitTypeInsn(int opcode, String type) {
             // Similar logic for handling Type changes (e.g., class renames).
             super.visitTypeInsn(opcode, type);
        }
    }

    /**
     * Simple Data Class to hold the mapped signature elements.
     */
    private static class MethodMapping {
        private final String owner;
        private final String name;
        private final String descriptor;

        public MethodMapping(String owner, String name, String descriptor) {
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
        }

        public String getOwner() { return owner; }
        public String getName() { return name; }
        public String getDescriptor() { return descriptor; }
    }
}
