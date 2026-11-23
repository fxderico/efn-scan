import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import java.io.*;
import java.util.zip.*;

/**
 * Rewrites a jar with all debug attributes removed (LocalVariableTable, LineNumberTable,
 * SourceFile). The EFN stage-2 obfuscator plants junk LVT entries that confuse decompilers.
 * Usage: java -cp asm.jar StripDebug.java <in.jar> <out.jar>
 */
public class StripDebug {
    public static void main(String[] a) throws Exception {
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(a[0]));
             ZipOutputStream out = new ZipOutputStream(new FileOutputStream(a[1]))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                byte[] b = in.readAllBytes();
                if (e.getName().endsWith(".class")) {
                    ClassReader cr = new ClassReader(b);
                    ClassWriter cw = new ClassWriter(0);
                    cr.accept(cw, ClassReader.SKIP_DEBUG);
                    b = cw.toByteArray();
                }
                out.putNextEntry(new ZipEntry(e.getName()));
                out.write(b);
                out.closeEntry();
            }
        }
    }
}
