package com.example.client.mixin;

import net.efn.client.module.impl.misc.FakeStats;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Scoreboard.class})
public abstract class MixinFakeStatsScoreboard {
   public MixinFakeStatsScoreboard() {
   }

   @Inject(
      method = {"method_1158"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$fakeSidebar(ScoreboardDisplaySlot var1, ScoreboardObjective var2, CallbackInfo var3) {
      if (FakeStats.redirect((Scoreboard)this, var1, var2)) {
         var3.cancel();
      }
   }
}
