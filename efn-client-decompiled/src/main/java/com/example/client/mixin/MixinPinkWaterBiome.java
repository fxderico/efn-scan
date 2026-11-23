package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.render.PinkWater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({Biome.class})
public abstract class MixinPinkWaterBiome {
   @Unique
   private volatile byte efn$cave;

   public MixinPinkWaterBiome() {
   }

   @ModifyReturnValue(
      method = {"getWaterColor"},
      at = {@At("RETURN")}
   )
   private int efn$pinkWater(int i) {
      if (!PinkWater.shows()) {
         return i;
      } else {
         byte var2 = this.efn$cave;
         if (var2 == 0 && (var2 = PinkWater.caveBiome((Biome)this)) != 0) {
            this.efn$cave = var2;
         }

         return PinkWater.water(i, var2 == 2);
      }
   }
}
