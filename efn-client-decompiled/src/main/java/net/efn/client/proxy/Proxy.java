package net.efn.client.proxy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Proxy {
   public static final int UNCHECKED = -2;
   public static final int DEAD = -1;
   public String name = "";
   public ProxyType type = ProxyType.SOCKS5;
   public String address = "";
   public int port;
   public String username = "";
   public String password = "";
   public boolean enabled;
   public volatile int ping = -2;

   public Proxy() {
   }

   public boolean isValid() {
      return this.name != null
         && !this.name.isBlank()
         && this.address != null
         && !this.address.isBlank()
         && this.type != null
         && this.port >= 1
         && this.port <= 65535;
   }

   public String displayAddress() {
      String var1 = this.address == null ? "" : this.address.trim();
      return (var1.indexOf(58) >= 0 ? "[" + var1 + "]" : var1) + ":" + this.port;
   }

   Proxy copy() {
      Proxy var1 = new Proxy();
      var1.name = this.name;
      var1.type = this.type;
      var1.address = this.address;
      var1.port = this.port;
      var1.username = this.username;
      var1.password = this.password;
      var1.enabled = this.enabled;
      return var1;
   }

   @Override
   public String toString() {
      return (this.type == null ? "?" : this.type.label()) + " proxy " + this.displayAddress();
   }
}
