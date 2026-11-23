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
   private static final Identifier EFN$SELECTION = Identifier.ofVanilla("hud/hotbar_selection");
   @Shadow
   private int heldItemTooltipFade;
   @Shadow
   private ItemStack currentStack;

   public MixinNoInvLeakGui() {
   }

   @ModifyVariable(
      method = {"renderHotbarItem"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private ItemStack efn$noInvLeakHotbar(
      ItemStack itemStack,
      @Local(argsOnly = true) DrawContext drawContext,
      @Local(argsOnly = true,ordinal = 0) int i,
      @Local(argsOnly = true,ordinal = 1) int j,
      @Local(argsOnly = true,ordinal = 2) int k
   ) {
      return NoInvLeakModule.hotbar(itemStack, NoInvLeakModule.screenX(drawContext, i, j, i), NoInvLeakModule.screenY(drawContext, i, j, j), k);
   }

   @Inject(
      method = {"renderHeldItemTooltip"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$noInvLeakItemName(DrawContext drawContext, CallbackInfo callbackInfo) {
      if (NoInvLeakModule.hidesItemName()) {
         if (NoInvLeakModule.overlaying() && this.heldItemTooltipFade > 0 && !this.currentStack.isEmpty()) {
            int var3 = Math.min(255, (int)(this.heldItemTooltipFade * 256.0F / 10.0F));
            int var4 = drawContext.getScaledWindowHeight() - 59;
            if (!MinecraftClient.getInstance().interactionManager.hasStatusBars()) {
               var4 += 14;
            }

            StreamOverlay.itemName(this.currentStack, var3, var4);
         }

         callbackInfo.cancel();
      }
   }

   @WrapWithCondition(
      method = {"renderHotbar"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"
      )}
   )
   private boolean efn$noInvLeakSelection(DrawContext drawContext, RenderPipeline renderPipeline, Identifier identifier, int i, int j, int k, int l) {
      if (NoInvLeakModule.hidesSelectedSlot() && EFN$SELECTION.equals(identifier)) {
         if (NoInvLeakModule.overlaying()) {
            StreamOverlay.selection(i, j);
         }

         return false;
      } else {
         return true;
      }
   }
}
