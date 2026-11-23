package net.efn.client.account;

import java.util.concurrent.CompletionException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class AccountException extends CompletionException {
   AccountException(String var1) {
      super(var1);
   }

   AccountException(String var1, Throwable var2) {
      super(var1, var2);
   }
}
