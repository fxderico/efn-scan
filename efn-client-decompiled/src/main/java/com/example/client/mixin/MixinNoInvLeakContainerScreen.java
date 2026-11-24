package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.efn.client.module.impl.misc.noinvleak.StreamOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({HandledScreen.class})
public abstract class MixinNoInvLeakContainerScreen {
   @Shadow
   protected Slot focusedSlot;
   @Shadow
   protected int x;
   @Shadow
   protected int y;

   public MixinNoInvLeakContainerScreen() {
   }

   @Shadow
   public abstract ScreenHandler getScreenHandler();

   @WrapOperation(
      method = {"drawSlot"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/screen/slot/Slot;getStack()Lnet/minecraft/item/ItemStack;"
      )}
   )
   private ItemStack efn$noInvLeakSlot(Slot slot, Operation<ItemStack> operation, @Local(argsOnly = true) DrawContext drawContext) {
      ItemStack var4 = (ItemStack)operation.call(slot);
      if (!NoInvLeakModule.ownSlot(slot)) {
         return var4;
      } else {
         int var5 = NoInvLeakModule.screenX(drawContext, slot.x, slot.y, this.x + slot.x);
         int var6 = NoInvLeakModule.screenY(drawContext, slot.x, slot.y, this.y + slot.y);
         return NoInvLeakModule.container(var4, slot, var5, var6, slot == this.focusedSlot);
      }
   }

   @WrapOperation(
      method = {"drawSlot"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/item/ItemStack;copyWithCount(I)Lnet/minecraft/item/ItemStack;"
      )}
   )
   private ItemStack efn$noInvLeakDragPreview(ItemStack itemStack, int i, Operation<ItemStack> operation) {
      return NoInvLeakModule.dragged((ItemStack)operation.call(itemStack, i));
   }

   @WrapOperation(
      method = {"drawSlot"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/screen/slot/Slot;getBackgroundSprite()Lnet/minecraft/util/Identifier;"
      )}
   )
   private Identifier efn$noInvLeakSilhouette(Slot slot, Operation<Identifier> operation) {
      Identifier var3 = (Identifier)operation.call(slot);
      return var3 != null && NoInvLeakModule.overlaying() && NoInvLeakModule.ownSlot(slot) && slot.hasStack() ? null : var3;
   }

   @ModifyVariable(
      method = {"drawItem"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private ItemStack efn$noInvLeakCarried(ItemStack itemStack, @Local(argsOnly = true,ordinal = 0) int i, @Local(argsOnly = true,ordinal = 1) int j) {
      return NoInvLeakModule.carried(itemStack, i, j);
   }

   @Inject(
      method = {"drawMouseoverTooltip"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$noInvLeakTooltip(DrawContext drawContext, int i, int j, CallbackInfo callbackInfo) {
      if (NoInvLeakModule.hidesTooltips() && NoInvLeakModule.ownSlot(this.focusedSlot)) {
         if (NoInvLeakModule.overlaying() && this.focusedSlot.hasStack() && this.getScreenHandler().getCursorStack().isEmpty()) {
            StreamOverlay.tooltip(this.focusedSlot.getStack(), i, j);
         }

         callbackInfo.cancel();
      }
   }
}
