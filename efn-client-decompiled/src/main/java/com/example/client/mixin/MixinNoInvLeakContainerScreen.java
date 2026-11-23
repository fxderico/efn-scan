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
   protected Slot field_2787;
   @Shadow
   protected int field_2776;
   @Shadow
   protected int field_2800;

   public MixinNoInvLeakContainerScreen() {
   }

   @Shadow
   public abstract ScreenHandler method_17577();

   @WrapOperation(
      method = {"method_2385"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1735;method_7677()Lnet/minecraft/class_1799;"
      )}
   )
   private ItemStack efn$noInvLeakSlot(Slot var1, Operation<ItemStack> var2, @Local(argsOnly = true) DrawContext var3) {
      ItemStack var4 = (ItemStack)var2.call(new Object[]{var1});
      if (!NoInvLeakModule.ownSlot(var1)) {
         return var4;
      } else {
         int var5 = NoInvLeakModule.screenX(var3, var1.field_7873, var1.field_7872, this.field_2776 + var1.field_7873);
         int var6 = NoInvLeakModule.screenY(var3, var1.field_7873, var1.field_7872, this.field_2800 + var1.field_7872);
         return NoInvLeakModule.container(var4, var1, var5, var6, var1 == this.field_2787);
      }
   }

   @WrapOperation(
      method = {"method_2385"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1799;method_46651(I)Lnet/minecraft/class_1799;"
      )}
   )
   private ItemStack efn$noInvLeakDragPreview(ItemStack var1, int var2, Operation<ItemStack> var3) {
      return NoInvLeakModule.dragged((ItemStack)var3.call(new Object[]{var1, var2}));
   }

   @WrapOperation(
      method = {"method_2385"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1735;method_7679()Lnet/minecraft/class_2960;"
      )}
   )
   private Identifier efn$noInvLeakSilhouette(Slot var1, Operation<Identifier> var2) {
      Identifier var3 = (Identifier)var2.call(new Object[]{var1});
      return var3 != null && NoInvLeakModule.overlaying() && NoInvLeakModule.ownSlot(var1) && var1.method_7681() ? null : var3;
   }

   @ModifyVariable(
      method = {"method_2382"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private ItemStack efn$noInvLeakCarried(ItemStack var1, @Local(argsOnly = true,ordinal = 0) int var2, @Local(argsOnly = true,ordinal = 1) int var3) {
      return NoInvLeakModule.carried(var1, var2, var3);
   }

   @Inject(
      method = {"method_2380"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$noInvLeakTooltip(DrawContext var1, int var2, int var3, CallbackInfo var4) {
      if (NoInvLeakModule.hidesTooltips() && NoInvLeakModule.ownSlot(this.field_2787)) {
         if (NoInvLeakModule.overlaying() && this.field_2787.method_7681() && this.method_17577().method_34255().method_7960()) {
            StreamOverlay.tooltip(this.field_2787.method_7677(), var2, var3);
         }

         var4.cancel();
      }
   }
}
