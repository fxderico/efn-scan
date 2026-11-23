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

    private static final Map<String, HttpTransport.CacheEntry> dnsCache = new ConcurrentHashMap<>();

    private static SSLSocketFactory trustAllSslFactory;

    public static HttpTransport.Response get(String var0) throws IOException {
        return request("GET", var0, null, null, null);
    }

    public static HttpTransport.Response get(String var0, Map<String, String> var1) throws IOException {
        return request("GET", var0, null, null, var1);
    }

    public static HttpTransport.Response post(String var0, String var1, String var2) throws IOException {
        return request("POST", var0, var1, var2, null);
    }

    public static HttpTransport.Response postWithHeaders(String var0, String var1, String var2, Map<String, String> var3) throws IOException {
        return request("POST", var0, var1, var2, var3);
    }

    public static HttpTransport.Response put(String var0, String var1, String var2, Map<String, String> var3) throws IOException {
        return request("PUT", var0, var1, var2, var3);
    }

    public static HttpTransport.Response request(String var0, String var1, String var2, String var3, Map<String, String> var4) throws IOException {
        URL var5 = URI.create(var1).toURL();
        String var6 = var5.getHost();
        int var7 = var5.getPort();
        String var10000 = var5.getProtocol();
        boolean var8 = var10000.equalsIgnoreCase("https");
        if (var7 == -1) {
            var7 = var8 ? 443 : 80;
        }
        String var9 = var5.getPath();
        if (var9 == null || var9.isEmpty()) {
            var9 = "/";
        }
        if (var5.getQuery() != null) {
            String var25 = var5.getQuery();
            var9 = var9 + "?" + var25;
        }
        String var10 = resolveViaDoH(var6);
        Socket var11 = new Socket();
        var11.setReceiveBufferSize(524288);
        var11.connect(new InetSocketAddress(var10, var7), 10000);
        var11.setSoTimeout(30000);
        Socket var12;
        if (var8) {
            SSLSocket var13 = (SSLSocket) trustAllSslFactory.createSocket(var11, var6, var7, true);
            SSLParameters var14 = var13.getSSLParameters();
            var14.setServerNames(List.of(new SNIHostName(var6)));
            var14.setEndpointIdentificationAlgorithm("HTTPS");
            var13.setSSLParameters(var14);
            var13.startHandshake();
            var12 = var13;
        } else {
            var12 = var11;
        }
        HttpTransport.Response var69;
        try {
            OutputStream var66 = var12.getOutputStream();
            InputStream var67 = var12.getInputStream();
            StringBuilder var15 = new StringBuilder();
            StringBuilder var70 = var15.append(var0).append(" ").append(var9);
            var70.append(" HTTP/1.1\r\n");
            var15.append("Host: ").append(var6);
            if (var8 && var7 != 443 || !var8 && var7 != 80) {
                var15.append(":").append(var7);
            }
            var15.append("\r\n");
            var15.append("Connection: close\r\n");
            boolean var16 = false;
            boolean var17 = false;
            if (var4 != null) {
                for (Entry var19 : var4.entrySet()) {
                    String var20 = (String) var19.getKey();
                    if (!var20.equalsIgnoreCase("Host")) {
                        if (!var20.equalsIgnoreCase("Connection")) {
                            if (!var20.equalsIgnoreCase("Content-Length")) {
                                if (var20.equalsIgnoreCase("Accept")) {
                                    var16 = true;
                                }
                                if (var20.equalsIgnoreCase("User-Agent")) {
                                    var17 = true;
                                }
                                var15.append(var20).append(": ").append((String) var19.getValue()).append("\r\n");
                            }
                        }
                    }
                }
            }
            if (!var16) {
                var15.append("Accept: */*\r\n");
            }
            if (!var17) {
                var15.append("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\r\n");
            }
            byte[] var68 = null;
            if (var2 != null) {
                var68 = var2.getBytes(StandardCharsets.UTF_8);
                if (var3 != null) {
                    var15.append("Content-Type: ").append(var3).append("\r\n");
                }
                var15.append("Content-Length: ").append(var68.length).append("\r\n");
            }
            var15.append("\r\n");
            var66.write(var15.toString().getBytes(StandardCharsets.ISO_8859_1));
            if (var68 != null) {
                var66.write(var68);
            }
            var66.flush();
            var69 = readResponse(var67);
        } finally {
            var12.close();
        }
        return var69;
    }

    private static HttpTransport.Response readResponse(InputStream var0) throws IOException {
        BufferedInputStream var1 = new BufferedInputStream(var0);
        ByteArrayOutputStream var2 = new ByteArrayOutputStream();
        int var3 = -1;
        int var4;
        while ((var4 = var1.read()) != -1) {
            var2.write(var4);
            if (var3 == 13 && var4 == 10) {
                int var5 = var2.size();
                byte[] var6 = var2.toByteArray();
                if (var5 >= 4 && var6[var5 - 4] == 13 && var6[var5 - 3] == 10) {
                    break;
                }
            }
            var3 = var4;
        }
        String var27 = var2.toString(StandardCharsets.ISO_8859_1);
        String[] var28 = var27.split("\r\n");
        String var7 = var28.length > 0 ? var28[0] : "";
        int var8 = 0;
        if (var7.startsWith("HTTP/")) {
            int var9 = var7.indexOf(32);
            if (var9 > 0) {
                int var10 = var7.indexOf(32, var9 + 1);
                String var11 = var10 > 0 ? var7.substring(var9 + 1, var10) : var7.substring(var9 + 1);
                try {
                    var8 = Integer.parseInt(var11.trim());
                } catch (NumberFormatException var26) {
                }
            }
        }
        LinkedHashMap var29 = new LinkedHashMap();
        int var30 = -1;
        boolean var31 = false;
        for (int var12 = 1; var12 < var28.length; var12++) {
            String var13 = var28[var12];
            int var14 = var13.indexOf(58);
            if (var14 > 0) {
                String var15 = var13.substring(0, var14).trim().toLowerCase();
                String var16 = var13.substring(var14 + 1).trim();
                var29.put(var15, var16);
                if (var15.equals("content-length")) {
                    try {
                        var30 = Integer.parseInt(var16);
                    } catch (NumberFormatException var25) {
                    }
                } else {
                    if (var15.equals("transfer-encoding")) {
                        String var10000 = var16.toLowerCase();
                        if (var10000.contains("chunked")) {
                            var31 = true;
                        }
                    }
                }
            }
        }
        byte[] var32;
        if (var31) {
            var32 = readChunkedBody(var1);
        } else if (var30 >= 0) {
            var32 = new byte[var30];
            int var33 = 0;
            while (var33 < var30) {
                int var35 = var1.read(var32, var33, var30 - var33);
                if (var35 == -1) {
                    break;
                }
                var33 += var35;
            }
        } else {
            ByteArrayOutputStream var34 = new ByteArrayOutputStream(1048576);
            byte[] var36 = new byte[131072];
            int var37;
            while ((var37 = var1.read(var36)) != -1) {
                var34.write(var36, 0, var37);
            }
            var32 = var34.toByteArray();
        }
        return new HttpTransport.Response(var8, var7, var29, var32);
    }

    private static byte[] readChunkedBody(InputStream var0) throws IOException {
        ByteArrayOutputStream var1 = new ByteArrayOutputStream(1048576);
        StringBuilder var2 = new StringBuilder();
        while (true) {
            var2.setLength(0);
            int var3;
            while ((var3 = var0.read()) != -1 && var3 != 13) {
                var2.append((char) var3);
            }
            if (var3 == 13) {
                var0.read();
            }
            String var4 = var2.toString();
            int var5 = var4.indexOf(59);
            if (var5 >= 0) {
                var4 = var4.substring(0, var5);
            }
            if (var4.isEmpty()) {
                break;
            }
            int var6 = Integer.parseInt(var4.trim(), 16);
            if (var6 == 0) {
                break;
            }
            byte[] var7 = new byte[var6];
            int var8 = 0;
            while (true) {
                if (var8 < var6) {
                    int var9 = var0.read(var7, var8, var6 - var8);
                    if (var9 != -1) {
                        var8 += var9;
                        continue;
                    }
                }
                var1.write(var7, 0, var8);
                var0.read();
                var0.read();
                break;
            }
        }
        return var1.toByteArray();
    }

    public static String resolveViaDoH(String var0) throws IOException {
        if (isIpv4Literal(var0)) {
            return var0;
        } else {
            HttpTransport.CacheEntry var1 = dnsCache.get(var0);
            if (var1 != null && !var1.isExpired()) {
                return var1.ip;
            } else {
                byte[] var2 = buildDnsQuery(var0);
                byte[] var3 = queryCloudflareDoH(var2);
                String var4 = parseDnsARecord(var3);
                if (var4 != null) {
                    dnsCache.put(var0, new HttpTransport.CacheEntry(var4, System.currentTimeMillis() + 300000L));
                }
                return var4;
            }
        }
    }

    private static boolean isIpv4Literal(String var0) {
        if (var0 != null && !var0.isEmpty()) {
            for (int var1 = 0; var1 < var0.length(); var1++) {
                char var2 = var0.charAt(var1);
                if (var2 != '.' && (var2 < '0' || var2 > '9')) {
                    return false;
                }
            }
            return var0.contains(".");
        } else {
            return false;
        }
    }

    private static byte[] buildDnsQuery(String var0) {
        ByteArrayOutputStream var1 = new ByteArrayOutputStream();
        DataOutputStream var2 = new DataOutputStream(var1);
        try {
            short var3 = (short) (System.nanoTime() & 65535L);
            var2.writeShort(var3);
            var2.writeShort(256);
            var2.writeShort(1);
            var2.writeShort(0);
            var2.writeShort(0);
            var2.writeShort(0);
            String[] var4 = var0.split("\\.");
            int var5 = var4.length;
            int var6 = 0;
            while (true) {
                if (var6 >= var5) {
                    var2.writeByte(0);
                    var2.writeShort(1);
                    var2.writeShort(1);
                    return var1.toByteArray();
                }
                String var7 = var4[var6];
                var2.writeByte(var7.length());
                var2.writeBytes(var7);
                var6++;
            }
        } catch (IOException var10) {
            throw new RuntimeException(var10);
        }
    }

    private static byte[] queryCloudflareDoH(byte[] var0) throws IOException {
        Socket var1 = new Socket();
        var1.connect(new InetSocketAddress("1.1.1.1", 443), 10000);
        var1.setSoTimeout(30000);
        SSLSocket var2 = (SSLSocket) trustAllSslFactory.createSocket(var1, "cloudflare-dns.com", 443, true);
        SSLParameters var3 = var2.getSSLParameters();
        var3.setServerNames(List.of(new SNIHostName("cloudflare-dns.com")));
        var2.setSSLParameters(var3);
        var2.startHandshake();
        byte[] var9;
        try {
            OutputStream var4 = var2.getOutputStream();
            InputStream var5 = var2.getInputStream();
            String var6 = Base64.getUrlEncoder().withoutPadding().encodeToString(var0);
            StringBuilder var7 = new StringBuilder();
            StringBuilder var10000 = var7.append("GET /dns-query?dns=").append(var6);
            var10000.append(" HTTP/1.1\r\n");
            var7.append("Host: cloudflare-dns.com\r\n");
            var7.append("Accept: application/dns-message\r\n");
            var7.append("Connection: close\r\n");
            var7.append("\r\n");
            var4.write(var7.toString().getBytes(StandardCharsets.ISO_8859_1));
            var4.flush();
            HttpTransport.Response var8 = readResponse(var5);
            var9 = var8.body;
        } finally {
            var2.close();
        }
        return var9;
    }

    private static String parseDnsARecord(byte[] var0) {
        if (var0 != null && var0.length >= 12) {
            ByteBuffer var1 = ByteBuffer.wrap(var0);
            var1.getShort();
            short var2 = var1.getShort();
            if ((var2 & '\u8000') == 0) {
                return null;
            } else {
                int var3 = var2 & 15;
                if (var3 != 0) {
                    return null;
                } else {
                    int var4 = var1.getShort() & '\uffff';
                    int var5 = var1.getShort() & '\uffff';
                    var1.getShort();
                    var1.getShort();
                    for (int var6 = 0; var6 < var4; var6++) {
                        skipDnsName(var1);
                        var1.getShort();
                        var1.getShort();
                    }
                    for (int var14 = 0; var14 < var5; var14++) {
                        skipDnsName(var1);
                        int var7 = var1.getShort() & '\uffff';
                        var1.getShort();
                        var1.getInt();
                        int var8 = var1.getShort() & '\uffff';
                        if (var7 == 1 && var8 == 4) {
                            byte[] var9 = new byte[4];
                            var1.get(var9);
                            int var10000 = var9[0] & 255;
                            int var10001 = var9[1] & 255;
                            int var10002 = var9[2] & 255;
                            int var13 = var9[3] & 255;
                            int var12 = var10002;
                            int var11 = var10001;
                            int var10 = var10000;
                            return var10 + "." + var11 + "." + var12 + "." + var13;
                        }
                        var1.position(var1.position() + var8);
                    }
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    private static void skipDnsName(ByteBuffer var0) {
        while (var0.hasRemaining()) {
            int var1 = var0.get() & 255;
            if (var1 != 0) {
                if ((var1 & 192) != 192) {
                    var0.position(var0.position() + var1);
                    continue;
                } else {
                    var0.get();
                }
            }
            break;
        }
    }

    static {
        try {
            SSLContext var0 = SSLContext.getInstance("TLS");
            var0.init(null, new TrustManager[] { new HttpTransport.TrustAll() }, new SecureRandom());
            trustAllSslFactory = var0.getSocketFactory();
        } catch (Exception var1) {
            trustAllSslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
    }

    private static class CacheEntry {

        final String ip;

        final long expiresAt;

        CacheEntry(String var1, long var2) {
            this.ip = var1;
            this.expiresAt = var2;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > this.expiresAt;
        }
    }

    public static class Response {

        public final int statusCode;

        public final String statusLine;

        public final Map<String, String> headers;

        public final byte[] body;

        Response(int var1, String var2, Map<String, String> var3, byte[] var4) {
            this.statusCode = var1;
            this.statusLine = var2;
            this.headers = var3;
            this.body = var4;
        }

        public String bodyString() {
            return new String(this.body, StandardCharsets.UTF_8);
        }

        public String header(String var1) {
            return this.headers.get(var1.toLowerCase());
        }
    }

    private static class TrustAll implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] var1, String var2) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] var1, String var2) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
