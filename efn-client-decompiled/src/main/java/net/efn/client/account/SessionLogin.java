package net.efn.client.account;

import com.mojang.util.UndashedUuid;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class SessionLogin {
   private SessionLogin() {
   }

   static Credentials signIn(String string) throws InterruptedException {
      AuthHttp.Reply var1 = AuthHttp.get("https://api.minecraftservices.com/minecraft/profile", string);
      if (var1.status() == 401) {
         throw new AccountException("This session token is invalid or has expired");
      } else if (var1.status() == 404) {
         throw new AccountException("This token's account doesn't own Minecraft: Java Edition");
      } else {
         String var2 = var1.text("name");
         String var3 = var1.text("id");
         if (var1.ok() && var2 != null && var3 != null) {
            return new Credentials(var2, UndashedUuid.fromStringLenient(var3), string, string);
         } else {
            throw new AccountException("Couldn't load the profile for this token (HTTP " + var1.status() + ")");
         }
      }
   }
}
