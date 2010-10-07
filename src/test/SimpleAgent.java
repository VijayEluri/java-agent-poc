package test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class SimpleAgent {

    private static Instrumentation instrumentation;
    private static File agentJarFile;

    // lazy initialization of VirtualMachine class, in case we don't have
    // tools.jar on classpath
    private static class Vm {

        public void loadAgent(File agentJarFile) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException {
            VirtualMachine vm = VirtualMachine.attach(getPid());
            vm.loadAgent(agentJarFile.getAbsolutePath());
            vm.detach();
        }

    }

    public static void premain(String agentArguments, Instrumentation instrumentation) throws UnmodifiableClassException, IOException,
            URISyntaxException {
        agentJarFile = new File(SimpleAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        System.out.println("Running premain");
        doMain(agentArguments, instrumentation);

    }

    private static void doMain(String agentArguments, Instrumentation instrumentation) throws IOException, UnmodifiableClassException {
        SimpleAgent.instrumentation = instrumentation;
        try {
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(agentJarFile));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        instrumentation.addTransformer(new SimpleClassFileTransformer(), true);
        instrumentation.retransformClasses(Date.class);
    }

    public static void agentmain(String agentArguments, Instrumentation instrumentation) throws UnmodifiableClassException, IOException {
        doMain(agentArguments, instrumentation);
    }

    public static void loadAgent() {
        try {
            Manifest manifest = createManifest("test.SimpleAgent");
            agentJarFile = createJarFile(manifest, "test.SimpleAgent", "test.DateOffset");
            new Vm().loadAgent(agentJarFile);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private static String getPid() {
        // not the best thing, but reportedly works on hotspot, ibm and jrockit
        // vm's
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }

    public static File createJarFile(Manifest manifest, String... otherClassNames) throws IOException, CannotCompileException, NotFoundException {
        File jarFile = File.createTempFile("agent", ".jar");
        jarFile.deleteOnExit();
        ClassPool pool = ClassPool.getDefault();
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest);
        try {
            for (String name : otherClassNames) {
                jos.putNextEntry(new JarEntry(name.replace('.', '/') + ".class"));
                jos.write(pool.get(name).toBytecode());
                jos.closeEntry();
            }
        } finally {
            jos.close();
        }

        return jarFile;
    }

    private static Manifest createManifest(String agentClassName) {
        final Manifest manifest = new Manifest();
        final Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttributes.put(new Attributes.Name("Agent-Class"), agentClassName);
        mainAttributes.put(new Attributes.Name("Can-Retransform-Classes"), "true");
        mainAttributes.put(new Attributes.Name("Can-Redefine-Classes"), "true");
        return manifest;
    }
}
