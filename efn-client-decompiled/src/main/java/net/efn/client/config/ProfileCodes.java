package net.efn.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class ProfileCodes {
   static final String PREFIX = "EfnProfile:";
   static final int MAX_JSON_BYTES = 4194304;
   private static final int MAX_CODE_CHARS = 1048576;
   private static final String DAMAGED = "That code is damaged or incomplete";
   private static final byte[] DICTIONARY = ShareDictionary.bytes();

   private ProfileCodes() {
   }

   static String encode(JsonObject var0) {
      return encode(var0, "EfnProfile:");
   }

   static String encode(JsonObject var0, String var1) {
      byte[] var2 = tidyNumbers(var0.deepCopy()).toString().getBytes(StandardCharsets.UTF_8);
      ByteArrayOutputStream var3 = new ByteArrayOutputStream(var2.length / 3 + 64);
      Deflater var4 = new Deflater(9, true);

      try {
         var4.setDictionary(DICTIONARY);
         var4.setInput(var2);
         var4.finish();
         byte[] var5 = new byte[4096];

         while (!var4.finished()) {
            var3.write(var5, 0, var4.deflate(var5));
         }
      } finally {
         var4.end();
      }

      return var1 + Base64.getEncoder().withoutPadding().encodeToString(var3.toByteArray());
   }

   static JsonObject decode(String var0) throws ProfileException {
      return decode(var0, "EfnProfile:", "No EFN profile code on the clipboard");
   }

   static JsonObject decode(String var0, String var1, String var2) throws ProfileException {
      String var4 = extract(var0, var1, var2);

      byte[] var3;
      try (InputStream var5 = unpack(Base64.getDecoder().decode(var4))) {
         var3 = var5.readNBytes(4194305);
      } catch (IllegalArgumentException | IOException var10) {
         throw new ProfileException("That code is damaged or incomplete");
      }

      if (var3.length > 4194304) {
         throw new ProfileException("That code is too large");
      } else {
         return parseObject(new String(var3, StandardCharsets.UTF_8), "That code is damaged or incomplete");
      }
   }

   private static InputStream unpack(byte[] var0) throws IOException {
      if (var0.length > 1 && (var0[0] & 255) == 31 && (var0[1] & 255) == 139) {
         return new GZIPInputStream(new ByteArrayInputStream(var0));
      } else {
         Inflater var1 = new Inflater(true);
         var1.setDictionary(DICTIONARY);
         return new InflaterInputStream(new ByteArrayInputStream(Arrays.copyOf(var0, var0.length + 1)), var1);
      }
   }

   private static JsonElement tidyNumbers(JsonElement var0) {
      if (var0.isJsonObject()) {
         JsonObject var3 = var0.getAsJsonObject();

         for (String var5 : List.copyOf(var3.keySet())) {
            var3.add(var5, tidyNumbers(var3.get(var5)));
         }
      } else if (var0.isJsonArray()) {
         JsonArray var6 = var0.getAsJsonArray();

         for (int var7 = 0; var7 < var6.size(); var7++) {
            var6.set(var7, tidyNumbers(var6.get(var7)));
         }
      } else {
         double var1;
         if (var0.isJsonPrimitive() && var0.getAsJsonPrimitive().isNumber() && (var1 = var0.getAsDouble()) == Math.rint(var1) && Math.abs(var1) < 1.0E15) {
            return new JsonPrimitive((long)var1);
         }
      }

      return var0;
   }

   static JsonObject parseObject(String var0, String var1) throws ProfileException {
      String var2 = var0.startsWith("\ufeff") ? var0.substring(1) : var0;

      try {
         JsonElement var3 = JsonParser.parseString(var2);
         if (var3.isJsonObject()) {
            return var3.getAsJsonObject();
         }
      } catch (RuntimeException var4) {
         throw new ProfileException(var1);
      }

      throw new ProfileException(var1);
   }

   static boolean contains(String var0, String var1) {
      if (var0 == null) {
         return false;
      } else {
         for (int var2 = 0; var2 + var1.length() <= var0.length(); var2++) {
            if (var0.regionMatches(true, var2, var1, 0, var1.length())) {
               return true;
            }
         }

         return false;
      }
   }

   private static String extract(String var0, String var1, String var2) throws ProfileException {
      if (var0 != null && !var0.isBlank()) {
         if (var0.length() > 2097152) {
            throw new ProfileException("That code is too large");
         } else {
            int var5 = -1;

            for (int var6 = 0; var6 + var1.length() <= var0.length() && var5 < 0; var6++) {
               if (var0.regionMatches(true, var6, var1, 0, var1.length())) {
                  var5 = var6;
               }
            }

            if (var5 < 0) {
               throw new ProfileException(var2);
            } else {
               int var3;
               int var4 = var3 = var5 + var1.length();

               while (var4 < var0.length() && isCodeChar(var0.charAt(var4))) {
                  var4++;
               }

               if (var4 == var3) {
                  throw new ProfileException("That code is damaged or incomplete");
               } else if (var4 - var3 > 1048576) {
                  throw new ProfileException("That code is too large");
               } else {
                  return var0.substring(var3, var4).replace('-', '+').replace('_', '/');
               }
            }
         }
      } else {
         throw new ProfileException("The clipboard is empty");
      }
   }

   private static boolean isCodeChar(char var0) {
      return var0 >= 'A' && var0 <= 'Z'
         || var0 >= 'a' && var0 <= 'z'
         || var0 >= '0' && var0 <= '9'
         || var0 == '+'
         || var0 == '/'
         || var0 == '='
         || var0 == '-'
         || var0 == '_';
   }
}
