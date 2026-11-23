package net.efn.client.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ProfileException extends Exception {
   public ProfileException(String var1) {
      super(var1);
   }
}
