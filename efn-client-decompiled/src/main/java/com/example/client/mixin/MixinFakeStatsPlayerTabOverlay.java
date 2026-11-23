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
      method = {"getPlayerName"},
      at = {@At("RETURN")}
   )
   private Text efn$fakeStatsTabName(Text text, @Local(argsOnly = true) PlayerListEntry playerListEntry) {
      return FakeStats.tabName(playerListEntry.getProfile().id(), text);
   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/scoreboard/Scoreboard;getScore(Lnet/minecraft/scoreboard/ScoreHolder;Lnet/minecraft/scoreboard/ScoreboardObjective;)Lnet/minecraft/scoreboard/ReadableScoreboardScore;"
      )}
   )
   private ReadableScoreboardScore efn$fakeStatsTabScore(
      Scoreboard scoreboard, ScoreHolder scoreHolder, ScoreboardObjective scoreboardObjective, Operation<ReadableScoreboardScore> operation
   ) {
      return FakeStats.tabScore(scoreHolder, (ReadableScoreboardScore)operation.call(scoreboard, scoreHolder, scoreboardObjective));
   }

   @ModifyArg(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         ordinal = 0,
         target = "Lnet/minecraft/client/font/TextRenderer;wrapLines(Lnet/minecraft/text/StringVisitable;I)Ljava/util/List;"
      )},
      index = 0
   )
   private StringVisitable efn$fakeStatsTabHeader(StringVisitable stringVisitable) {
      return FakeStats.tabText(stringVisitable);
   }

   @ModifyArg(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         ordinal = 1,
         target = "Lnet/minecraft/client/font/TextRenderer;wrapLines(Lnet/minecraft/text/StringVisitable;I)Ljava/util/List;"
      )},
      index = 0
   )
   private StringVisitable efn$fakeStatsTabFooter(StringVisitable stringVisitable) {
      return FakeStats.tabText(stringVisitable);
   }
}
