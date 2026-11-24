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

   @Override
   public void onLoad(String string) {
      this.indigoLoaded = FabricLoader.getInstance().isModLoaded("fabric-renderer-indigo");
   }

   @Override
   public String getRefMapperConfig() {
      return null;
   }

   @Override
   public boolean shouldApplyMixin(String string, String string2) {
      return this.indigoLoaded;
   }

   @Override
   public void acceptTargets(Set<String> set, Set<String> set2) {
   }

   @Override
   public List<String> getMixins() {
      return null;
   }

   @Override
   public void preApply(String string, ClassNode classNode, String string2, IMixinInfo iMixinInfo) {
   }

   @Override
   public void postApply(String string, ClassNode classNode, String string2, IMixinInfo iMixinInfo) {
   }
}
