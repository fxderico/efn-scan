package com.example.client.mixin;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.MaceModule;
import net.efn.client.module.impl.combat.ShieldBreakerModule;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinMaceMultiPlayerGameMode {
   public MixinMaceMultiPlayerGameMode() {
   }

   @Inject(
      method = {"method_2918"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_636;method_2911()V"
      )}
   )
   private void efn$weaponBeforeAttack(PlayerEntity var1, Entity var2, CallbackInfo var3) {
      ClientPlayerEntity var5 = MinecraftClient.method_1551().field_1724;
      if (var1 == var5 && !MeleeAttacks.actedThisTick(var5)) {
         ShieldBreakerModule var6 = ModuleRegistry.get(ShieldBreakerModule.class);
         if (var6 != null && var6.isEnabled()) {
            var6.beforeAttack(var5, var2);
         }

         MaceModule var4;
         if ((var4 = ModuleRegistry.get(MaceModule.class)) != null && var4.isEnabled()) {
            var4.beforeAttack(var5, var2);
         }
      }
   }
}
