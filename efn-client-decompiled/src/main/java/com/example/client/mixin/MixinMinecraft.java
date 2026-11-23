package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.event.EventBus;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.EfnTitleScreen;
import net.efn.client.module.impl.misc.AutoEatModule;
import net.efn.client.util.PerformanceBudget;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({MinecraftClient.class})
public abstract class MixinMinecraft {
   @Shadow
   public ClientWorld field_1687;

   public MixinMinecraft() {
   }

   @Shadow
   public abstract void method_1507(Screen var1);

   @Inject(
      method = {"method_1507"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$replaceTitleScreen(Screen var1, CallbackInfo var2) {
      if (!(var1 instanceof EfnTitleScreen)) {
         boolean var3 = var1 instanceof TitleScreen || var1 == null && this.field_1687 == null;
         if (var3 && EfnTitleScreen.enabled()) {
            var2.cancel();
            this.method_1507(new EfnTitleScreen());
         }
      }
   }

   @Inject(
      method = {"method_24287"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$windowTitle(CallbackInfoReturnable<String> var1) {
      if (EfnTitleScreen.enabled()) {
         var1.setReturnValue("Minecraft " + SharedConstants.method_16673().comp_4025() + " - EFN Client");
      }
   }

   @Inject(
      method = {"method_1574"},
      at = {@At("HEAD")}
   )
   private void efn$onTickStart(CallbackInfo var1) {
      PerformanceBudget.beginTick();
      EventBus.INSTANCE.drainAsync();
      ServerRotation.startTick();
      TickEvent.dispatch();
   }

   @Inject(
      method = {"method_1574"},
      at = {@At("RETURN")}
   )
   private void efn$onTickEnd(CallbackInfo var1) {
      TickEvent.dispatchPost();
   }

   @ModifyExpressionValue(
      method = {"method_1583"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_239;method_17783()Lnet/minecraft/class_239$class_240;"
      )}
   )
   private Type efn$autoEatIgnoreCrosshair(Type var1) {
      return AutoEatModule.isEating() ? Type.field_1333 : var1;
   }
}
