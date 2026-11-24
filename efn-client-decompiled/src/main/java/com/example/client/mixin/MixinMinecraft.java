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
   public ClientWorld world;

   public MixinMinecraft() {
   }

   @Shadow
   public abstract void setScreen(Screen screen);

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$replaceTitleScreen(Screen screen, CallbackInfo callbackInfo) {
      if (!(screen instanceof EfnTitleScreen)) {
         boolean var3 = screen instanceof TitleScreen || screen == null && this.world == null;
         if (var3 && EfnTitleScreen.enabled()) {
            callbackInfo.cancel();
            this.setScreen(new EfnTitleScreen());
         }
      }
   }

   @Inject(
      method = {"getWindowTitle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$windowTitle(CallbackInfoReturnable<String> callbackInfoReturnable) {
      if (EfnTitleScreen.enabled()) {
         callbackInfoReturnable.setReturnValue("Minecraft " + SharedConstants.getGameVersion().name() + " - EFN Client");
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void efn$onTickStart(CallbackInfo callbackInfo) {
      PerformanceBudget.beginTick();
      EventBus.INSTANCE.drainAsync();
      ServerRotation.startTick();
      TickEvent.dispatch();
   }

   @Inject(
      method = {"tick"},
      at = {@At("RETURN")}
   )
   private void efn$onTickEnd(CallbackInfo callbackInfo) {
      TickEvent.dispatchPost();
   }

   @ModifyExpressionValue(
      method = {"doItemUse"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;"
      )}
   )
   private Type efn$autoEatIgnoreCrosshair(Type type) {
      return AutoEatModule.isEating() ? Type.MISS : type;
   }
}
