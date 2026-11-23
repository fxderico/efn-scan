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
      method = {"method_7679"},
      at = {@At("RETURN")}
   )
   private Identifier efn$noInvLeakSilhouette(Identifier var1) {
      Slot var2 = (Slot)this;
      return var1 != null && NoInvLeakModule.overlaying() && var2.field_7871 instanceof PlayerInventory && var2.method_7681() ? null : var1;
   }
}
