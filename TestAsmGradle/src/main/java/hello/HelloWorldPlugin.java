package hello;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Textifier;

public class HelloWorldPlugin implements Opcodes {
    public static interface Plugin {
        void sayHello(String name);
    }

    public static byte[] compile() {

        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, "hello/MyClass", null,
                "java/lang/Object",
                new String[] { "hello/HelloWorldPlugin$Plugin" });

        cw.visitInnerClass("hello/HelloWorldPlugin$Plugin",
                "hello/HelloWorldPlugin", "Plugin", ACC_PUBLIC + ACC_STATIC
                        + ACC_ABSTRACT + ACC_INTERFACE);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(5, l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>",
                    "()V");
            mv.visitInsn(RETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", "Lhello/MyClass;", null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "sayHello",
                    "(Ljava/lang/String;)V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(9, l0);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
                    "Ljava/io/PrintStream;");
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitLdcInsn("Hello, ");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder",
                    "<init>", "(Ljava/lang/String;)V");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder",
                    "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder",
                    "toString", "()Ljava/lang/String;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V");
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(10, l1);
            mv.visitInsn(RETURN);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLocalVariable("this", "Lhello/MyClass;", null, l0, l2, 0);
            mv.visitLocalVariable("name", "Ljava/lang/String;", null, l0, l2,
                    1);
            mv.visitMaxs(4, 2);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    public static class DynamicClassLoader extends ClassLoader {
        public DynamicClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> define(String className, byte[] bytecode) {
            return super.defineClass(className, bytecode, 0, bytecode.length);
        }
    };

    public static void main(String[] args) throws Exception {
    	/*
    	//ClassReader cr = new ClassReader(new FileInputStream("HelloWorld.class"));
    	String pathToClassFile = "E:/Workspace/test123/ExampleProgram.class";
    	ASMifier.main(new String[]{pathToClassFile});
        //Textifier.main(new String[]{pathToClassFile});
    	//cr.accept(new ASMifierClassVisitor(new PrintWriter(System.out)), 0);
    	
        DynamicClassLoader loader = new DynamicClassLoader(Thread
                .currentThread().getContextClassLoader());
        Class<?> helloWorldClass = loader.define("hello.MyClass", compile());
        Plugin plugin = (Plugin) helloWorldClass.newInstance();
        plugin.sayHello("Test");
        */
        
     // Create a File object on the root of the directory containing the class file
        File file = new File("E:/Workspace/test123/");

        try {
            // Convert File to a URL
            URL url = file.toURL();          // file:/c:/myclasses/
            URL[] urls = new URL[]{url};

            // Create a new class loader with the directory
            ClassLoader cl = new URLClassLoader(urls);

            // Load in the class; MyClass.class should be located in
            // the directory file:/c:/myclasses/com/mycompany
            Class<?> cls = cl.loadClass("hello.ExampleProgram");
            Object obj = cls.newInstance();
            Method fields = obj.getClass().getMethod("main", String[].class);
            fields.invoke(obj, (Object)(new String[]{"asd"}));
            
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
}