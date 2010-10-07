package test;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

public class SimpleClassFileTransformer implements ClassFileTransformer {

    private Map<String, String> map = new HashMap<String, String>() {
        {
            put("java/util/Date", "");
        }
    };

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> redefiningClass, ProtectionDomain domain, byte[] bytes)
            throws IllegalClassFormatException {
        try {
            return doClass(className, redefiningClass, bytes);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] doClass(String name, Class clazz, byte[] b) throws NotFoundException {
        CtClass cl = null;
        if (!map.containsKey(name))
            return b;

        try {
            ClassPool pool = ClassPool.getDefault();
            cl = pool.makeClass(new java.io.ByteArrayInputStream(b));
            if (cl.isInterface() == false) {
                cl.instrument(new ExprEditor() {
                    @Override
                    public void edit(FieldAccess f) throws CannotCompileException {
                        if (f.isWriter() && f.getFieldName().equals("fastTime")) {
                            f.replace("$proceed($$);fastTime += test.DateOffset.offset;");
                        }
                    }
                });

                b = cl.toBytecode();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not instrument  " + name + ",  exception : " + e.getMessage());
        } finally {
            if (cl != null) {
                cl.detach();
            }
        }
        return b;
    }

}
