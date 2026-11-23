package net.efn.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ClientBrand {
   public static final String NAME = "EFN";
   public static final String WINDOW_TITLE = "EFN Client";
   public static final String LOGGER_NAME = "EFN";

   private ClientBrand() {
   }
}
