package net.efn.client.easyplace;

import java.util.List;
import net.efn.client.easyplace.builder.SchematicBuilderModule;
import net.efn.client.easyplace.module.EasyPlaceModule;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public final class EasyPlaceModules {
   private EasyPlaceModules() {
   }

   public static boolean available() {
      FabricLoader var0 = FabricLoader.getInstance();
      return var0.isModLoaded("litematica") && var0.isModLoaded("malilib");
   }

   public static List<ClientModule> create() {
      return List.of(new EasyPlaceModule(), new SchematicBuilderModule());
   }
}
