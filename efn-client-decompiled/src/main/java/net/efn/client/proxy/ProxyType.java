package net.efn.client.proxy;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum ProxyType {
   SOCKS4("Socks4"),
   SOCKS5("Socks5");

   private final String label;

   private ProxyType(String string2) {
      this.label = string2;
   }

   public String label() {
      return this.label;
   }

   static ProxyType parse(String string) {
      if (string == null) {
         return null;
      } else {
         String var1 = string.trim().toLowerCase(Locale.ROOT);

         return switch (var1) {
            case "socks4", "socks4a" -> SOCKS4;
            case "socks5", "socks5h", "socks" -> SOCKS5;
            default -> null;
         };
      }
   }
}
