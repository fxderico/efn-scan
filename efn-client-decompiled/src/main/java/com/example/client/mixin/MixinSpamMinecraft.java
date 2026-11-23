package com.example.client.mixin;

import net.efn.client.module.impl.misc.SpamModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({MinecraftClient.class})
public abstract class MixinSpamMinecraft {
   @Shadow
   public ClientWorld field_1687;

   public MixinSpamMinecraft() {
   }

   @Inject(
      method = {"method_18096"},
      at = {@At("HEAD")}
   )
   private void efn$spamGameLeft(Screen var1, boolean var2, boolean var3, CallbackInfo var4) {
      if (SpamModule.active != null && this.field_1687 != null) {
         SpamModule.onGameLeft();
      }
   }

   @Inject(
      method = {"method_1507"},
      at = {@At("HEAD")}
   )
   private void efn$spamDisconnectedScreen(Screen var1, CallbackInfo var2) {
      if (SpamModule.active != null && this.field_1687 != null && var1 instanceof DisconnectedScreen) {
         SpamModule.onDisconnectedScreen();
      }
   }
}
