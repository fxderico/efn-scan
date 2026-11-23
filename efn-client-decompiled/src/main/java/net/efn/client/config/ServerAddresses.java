package net.efn.client.config;

import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import java.net.IDN;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ServerAddresses {
   public static final int DEFAULT_PORT = 25565;
   private static final int MAX_HOST = 253;
   private static final Pattern HOST = Pattern.compile("[a-z0-9_-]+(\\.[a-z0-9_-]+)*");
   private static final Set<String> FRONT_DOORS = Set.of("play", "mc", "www", "join", "hub", "lobby", "server", "connect", "go");

   private ServerAddresses() {
   }

   public static ServerAddresses.Address parse(String string) {
      if (string == null) {
         return null;
      } else {
         String var1 = string.trim();
         if (!var1.isEmpty() && var1.length() <= 261) {
            try {
               HostAndPort var2 = HostAndPort.fromString(var1).withDefaultPort(25565);
               String var3 = var2.getHost().toLowerCase(Locale.ROOT);

               while (var3.endsWith(".")) {
                  var3 = var3.substring(0, var3.length() - 1);
               }

               if (var3.isEmpty()) {
                  return null;
               } else {
                  return InetAddresses.isInetAddress(var3)
                        || (var3 = IDN.toASCII(var3).toLowerCase(Locale.ROOT)).length() <= 253 && HOST.matcher(var3).matches()
                     ? new ServerAddresses.Address(var3, var2.getPort())
                     : null;
               }
            } catch (IllegalStateException | IllegalArgumentException var4) {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static int score(ServerAddresses.Address address, ServerAddresses.Address address2) {
      if (address != null && address2 != null && address.port() == address2.port()) {
         String var3 = address.host();
         String var2;
         if (var3.equals(var2 = address2.host())) {
            return 3;
         } else if (!InetAddresses.isInetAddress(var3) && !InetAddresses.isInetAddress(var2)) {
            String var4 = site(var3);
            if (var4.equals(site(var2))) {
               return 2;
            } else {
               return var2.endsWith("." + var4) ? 1 : 0;
            }
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   private static String site(String string) {
      int var1 = string.indexOf(46);
      if (var1 > 0 && FRONT_DOORS.contains(string.substring(0, var1))) {
         String var2 = string.substring(var1 + 1);

         try {
            return InternetDomainName.from(var2).isUnderPublicSuffix() ? var2 : string;
         } catch (IllegalArgumentException var4) {
            return var2.indexOf(46) > 0 ? var2 : string;
         }
      } else {
         return string;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Address(String host, int port) {
      @Override
      public String toString() {
         String var1 = this.host.indexOf(58) >= 0 ? "[" + this.host + "]" : this.host;
         return this.port == 25565 ? var1 : var1 + ":" + this.port;
      }
   }
}
