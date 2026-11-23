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

   static String encode(JsonObject jsonObject) {
      return encode(jsonObject, "EfnProfile:");
   }

   static String encode(JsonObject jsonObject, String string) {
      byte[] var2 = tidyNumbers(jsonObject.deepCopy()).toString().getBytes(StandardCharsets.UTF_8);
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

      return string + Base64.getEncoder().withoutPadding().encodeToString(var3.toByteArray());
   }

   static JsonObject decode(String string) throws ProfileException {
      return decode(string, "EfnProfile:", "No EFN profile code on the clipboard");
   }

   static JsonObject decode(String string, String string2, String string3) throws ProfileException {
      String var4 = extract(string, string2, string3);

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

   private static InputStream unpack(byte[] bs) throws IOException {
      if (bs.length > 1 && (bs[0] & 255) == 31 && (bs[1] & 255) == 139) {
         return new GZIPInputStream(new ByteArrayInputStream(bs));
      } else {
         Inflater var1 = new Inflater(true);
         var1.setDictionary(DICTIONARY);
         return new InflaterInputStream(new ByteArrayInputStream(Arrays.copyOf(bs, bs.length + 1)), var1);
      }
   }

   private static JsonElement tidyNumbers(JsonElement jsonElement) {
      if (jsonElement.isJsonObject()) {
         JsonObject var3 = jsonElement.getAsJsonObject();

         for (String var5 : List.copyOf(var3.keySet())) {
            var3.add(var5, tidyNumbers(var3.get(var5)));
         }
      } else if (jsonElement.isJsonArray()) {
         JsonArray var6 = jsonElement.getAsJsonArray();

         for (int var7 = 0; var7 < var6.size(); var7++) {
            var6.set(var7, tidyNumbers(var6.get(var7)));
         }
      } else {
         double var1;
         if (jsonElement.isJsonPrimitive()
            && jsonElement.getAsJsonPrimitive().isNumber()
            && (var1 = jsonElement.getAsDouble()) == Math.rint(var1)
            && Math.abs(var1) < 1.0E15) {
            return new JsonPrimitive((long)var1);
         }
      }

      return jsonElement;
   }

   static JsonObject parseObject(String string, String string2) throws ProfileException {
      String var2 = string.startsWith("\ufeff") ? string.substring(1) : string;

      try {
         JsonElement var3 = JsonParser.parseString(var2);
         if (var3.isJsonObject()) {
            return var3.getAsJsonObject();
         }
      } catch (RuntimeException var4) {
         throw new ProfileException(string2);
      }

      throw new ProfileException(string2);
   }

   static boolean contains(String string, String string2) {
      if (string == null) {
         return false;
      } else {
         for (int var2 = 0; var2 + string2.length() <= string.length(); var2++) {
            if (string.regionMatches(true, var2, string2, 0, string2.length())) {
               return true;
            }
         }

         return false;
      }
   }

   private static String extract(String string, String string2, String string3) throws ProfileException {
      if (string != null && !string.isBlank()) {
         if (string.length() > 2097152) {
            throw new ProfileException("That code is too large");
         } else {
            int var5 = -1;

            for (int var6 = 0; var6 + string2.length() <= string.length() && var5 < 0; var6++) {
               if (string.regionMatches(true, var6, string2, 0, string2.length())) {
                  var5 = var6;
               }
            }

            if (var5 < 0) {
               throw new ProfileException(string3);
            } else {
               int var3;
               int var4 = var3 = var5 + string2.length();

               while (var4 < string.length() && isCodeChar(string.charAt(var4))) {
                  var4++;
               }

               if (var4 == var3) {
                  throw new ProfileException("That code is damaged or incomplete");
               } else if (var4 - var3 > 1048576) {
                  throw new ProfileException("That code is too large");
               } else {
                  return string.substring(var3, var4).replace('-', '+').replace('_', '/');
               }
            }
         }
      } else {
         throw new ProfileException("The clipboard is empty");
      }
   }

   private static boolean isCodeChar(char c) {
      return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '+' || c == '/' || c == '=' || c == '-' || c == '_';
   }
}
