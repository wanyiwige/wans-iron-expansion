import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Null-tolerant initialization and live ingredient lookup for Enigmatic Delicacy 1.0.1. */
public final class RecipeInitPatch {
    private static final String OWNER = "auviotre/enigmatic/delicacy/contents/crafting/EnigmaticAmuletRecipe";
    private static final String NINE = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;";

    private record Field(String owner, String name, String descriptor) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("input.class output.class");
        byte[] source = Files.readAllBytes(Path.of(args[0]));
        ClassReader reader = new ClassReader(source);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        List<Field> ingredients = new ArrayList<>();
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor base = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, base) {
                    boolean inIngredients = false;
                    @Override public void visitFieldInsn(int opcode, String fieldOwner, String fieldName, String fieldDescriptor) {
                        if (name.equals("<clinit>") && opcode == Opcodes.PUTSTATIC && fieldName.equals("SERIALIZER")) inIngredients = true;
                        else if (name.equals("<clinit>") && inIngredients && opcode == Opcodes.GETSTATIC && ingredients.size() < 9)
                            ingredients.add(new Field(fieldOwner, fieldName, fieldDescriptor));
                        if (!name.equals("<clinit>") && opcode == Opcodes.GETSTATIC && fieldOwner.equals(OWNER) && fieldName.equals("ITEM_LIST"))
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "codex$liveItems", "()Ljava/util/List;", false);
                        else super.visitFieldInsn(opcode, fieldOwner, fieldName, fieldDescriptor);
                    }
                    @Override public void visitMethodInsn(int opcode, String methodOwner, String methodName, String methodDescriptor, boolean itf) {
                        if (name.equals("<clinit>") && opcode == Opcodes.INVOKESTATIC && methodOwner.equals("java/util/List") && methodName.equals("of") && methodDescriptor.equals(NINE)) {
                            if (ingredients.size() != 9) throw new IllegalStateException("Unexpected ingredient sequence: " + ingredients);
                            inIngredients = false;
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "codex$safeList", NINE, false);
                        } else super.visitMethodInsn(opcode, methodOwner, methodName, methodDescriptor, itf);
                    }
                };
            }
            @Override public void visitEnd() {
                if (ingredients.size() != 9) throw new IllegalStateException("Ingredient sequence missing");
                MethodVisitor safe = super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "codex$safeList", NINE, null, null);
                safe.visitCode();
                safe.visitIntInsn(Opcodes.BIPUSH, 9);
                safe.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                for (int i = 0; i < 9; i++) {
                    safe.visitInsn(Opcodes.DUP);
                    safe.visitIntInsn(Opcodes.BIPUSH, i);
                    safe.visitVarInsn(Opcodes.ALOAD, i);
                    safe.visitInsn(Opcodes.AASTORE);
                }
                safe.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;", false);
                safe.visitInsn(Opcodes.ARETURN);
                safe.visitMaxs(0, 0);
                safe.visitEnd();

                MethodVisitor live = super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "codex$liveItems", "()Ljava/util/List;", null, null);
                live.visitCode();
                for (Field field : ingredients) live.visitFieldInsn(Opcodes.GETSTATIC, field.owner(), field.name(), field.descriptor());
                live.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "codex$safeList", NINE, false);
                live.visitInsn(Opcodes.ARETURN);
                live.visitMaxs(0, 0);
                live.visitEnd();
                super.visitEnd();
            }
        }, 0);
        Files.write(Path.of(args[1]), writer.toByteArray());
    }
}
