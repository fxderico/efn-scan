package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.efn.client.module.impl.misc.FakeStats;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin({PlayerListHud.class})
public abstract class MixinFakeStatsPlayerTabOverlay {
   public MixinFakeStatsPlayerTabOverlay() {
   }

   @ModifyReturnValue(
      method = {"method_1918"},
      at = {@At("RETURN")}
   )
   private Text efn$fakeStatsTabName(Text var1, @Local(argsOnly = true) PlayerListEntry var2) {
      return FakeStats.tabName(var2.method_2966().id(), var1);
   }

   @WrapOperation(
      method = {"method_1919"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_269;method_55430(Lnet/minecraft/class_9015;Lnet/minecraft/class_266;)Lnet/minecraft/class_9013;"
      )}
   )
   private ReadableScoreboardScore efn$fakeStatsTabScore(Scoreboard var1, ScoreHolder var2, ScoreboardObjective var3, Operation<ReadableScoreboardScore> var4) {
      return FakeStats.tabScore(var2, (ReadableScoreboardScore)var4.call(new Object[]{var1, var2, var3}));
   }

   @ModifyArg(
      method = {"method_1919"},
      at = {@At(
         value = "INVOKE",
         ordinal = 0,
         target = "Lnet/minecraft/class_327;method_1728(Lnet/minecraft/class_5348;I)Ljava/util/List;"
      )},
      index = 0
   )
   private StringVisitable efn$fakeStatsTabHeader(StringVisitable var1) {
      return FakeStats.tabText(var1);
   }

   @ModifyArg(
      method = {"method_1919"},
      at = {@At(
         value = "INVOKE",
         ordinal = 1,
         target = "Lnet/minecraft/class_327;method_1728(Lnet/minecraft/class_5348;I)Ljava/util/List;"
      )},
      index = 0
   )
   private StringVisitable efn$fakeStatsTabFooter(StringVisitable var1) {
      return FakeStats.tabText(var1);
   }
}
