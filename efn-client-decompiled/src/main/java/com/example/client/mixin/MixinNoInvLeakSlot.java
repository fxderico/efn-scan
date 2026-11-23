package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({Slot.class})
public abstract class MixinNoInvLeakSlot {
   public MixinNoInvLeakSlot() {
   }

   @ModifyReturnValue(
      method = {"getBackgroundSprite"},
      at = {@At("RETURN")}
   )
   private Identifier efn$noInvLeakSilhouette(Identifier identifier) {
      Slot var2 = (Slot)this;
      return identifier != null && NoInvLeakModule.overlaying() && var2.inventory instanceof PlayerInventory && var2.hasStack() ? null : identifier;
   }
}
