package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.efn.client.module.impl.misc.noinvleak.StreamOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({InGameHud.class})
public abstract class MixinNoInvLeakGui {
   @Unique
   private static final Identifier EFN$SELECTION = Identifier.method_60656("hud/hotbar_selection");
   @Shadow
   private int field_2040;
   @Shadow
   private ItemStack field_2031;

   public MixinNoInvLeakGui() {
   }

   @ModifyVariable(
      method = {"method_1762"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private ItemStack efn$noInvLeakHotbar(
      ItemStack var1,
      @Local(argsOnly = true) DrawContext var2,
      @Local(argsOnly = true,ordinal = 0) int var3,
      @Local(argsOnly = true,ordinal = 1) int var4,
      @Local(argsOnly = true,ordinal = 2) int var5
   ) {
      return NoInvLeakModule.hotbar(var1, NoInvLeakModule.screenX(var2, var3, var4, var3), NoInvLeakModule.screenY(var2, var3, var4, var4), var5);
   }

   @Inject(
      method = {"method_1749"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$noInvLeakItemName(DrawContext var1, CallbackInfo var2) {
      if (NoInvLeakModule.hidesItemName()) {
         if (NoInvLeakModule.overlaying() && this.field_2040 > 0 && !this.field_2031.method_7960()) {
            int var3 = Math.min(255, (int)(this.field_2040 * 256.0F / 10.0F));
            int var4 = var1.method_51443() - 59;
            if (!MinecraftClient.method_1551().field_1761.method_2908()) {
               var4 += 14;
            }

            StreamOverlay.itemName(this.field_2031, var3, var4);
         }

         var2.cancel();
      }
   }

   @WrapWithCondition(
      method = {"method_1759"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_332;method_52706(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/class_2960;IIII)V"
      )}
   )
   private boolean efn$noInvLeakSelection(DrawContext var1, RenderPipeline var2, Identifier var3, int var4, int var5, int var6, int var7) {
      if (NoInvLeakModule.hidesSelectedSlot() && EFN$SELECTION.equals(var3)) {
         if (NoInvLeakModule.overlaying()) {
            StreamOverlay.selection(var4, var5);
         }

         return false;
      } else {
         return true;
      }
   }
}
