package com.example.client.mixin.indigo;

import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

@Environment(EnvType.CLIENT)
public final class EfnIndigoMixinPlugin implements IMixinConfigPlugin {
   private boolean indigoLoaded;

   public EfnIndigoMixinPlugin() {
   }

   public void onLoad(String var1) {
      this.indigoLoaded = FabricLoader.getInstance().isModLoaded("fabric-renderer-indigo");
   }

   public String getRefMapperConfig() {
      return null;
   }

   public boolean shouldApplyMixin(String var1, String var2) {
      return this.indigoLoaded;
   }

   public void acceptTargets(Set<String> var1, Set<String> var2) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String var1, ClassNode var2, String var3, IMixinInfo var4) {
   }

   public void postApply(String var1, ClassNode var2, String var3, IMixinInfo var4) {
   }
}
