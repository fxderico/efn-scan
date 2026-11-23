package net.efn.client.module.impl.render;

import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.efn.client.config.FriendStore;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.gui.GuiPalette;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class TracersModule extends ClientModule implements HudOverlayProvider {
   private static final double DISTANCE_COLOR_RANGE = 60.0;
   private static final float ARROW_TIP = 0.73205F;
   private static final int WHITE = 16777215;
   private final BooleanSetting players = this.addSetting("players", new BooleanSetting("Players", true));
   private final BooleanSetting animals = this.addSetting("animals", new BooleanSetting("Animals", false));
   private final BooleanSetting waterAnimals = this.addSetting("water-animals", new BooleanSetting("Water Animals", false));
   private final BooleanSetting monsters = this.addSetting("monsters", new BooleanSetting("Monsters", false));
   private final BooleanSetting ambient = this.addSetting("ambient", new BooleanSetting("Ambient", false));
   private final BooleanSetting misc = this.addSetting("misc", new BooleanSetting("Misc", false));
   private final BooleanSetting ignoreSelf = this.addSetting("ignore-self", new BooleanSetting("Ignore Self", false));
   private final BooleanSetting ignoreFriends = this.addSetting("ignore-friends", new BooleanSetting("Ignore Friends", false));
   private final BooleanSetting showInvisible = this.addSetting("show-invisible", new BooleanSetting("Show Invisible", true));
   private final ModeSetting style = this.addSetting("style", new ModeSetting("Style", "Lines", "Lines", "Offscreen"));
   private final ModeSetting target = this.addSetting("target", new ModeSetting("Target", "Body", "Head", "Body", "Feet"));
   private final BooleanSetting stem = this.addSetting("stem", new BooleanSetting("Stem", true));
   private final NumberSetting maxDistance = this.addSetting("max-distance", new NumberSetting("Max Distance", 256.0F, 0.0F, 256.0F).step(1.0F));
   private final NumberSetting distanceOffscreen = this.addSetting(
      "distance-offscreen", new NumberSetting("Distance Offscreen", 200.0F, 0.0F, 500.0F).step(1.0F)
   );
   private final NumberSetting sizeOffscreen = this.addSetting("size-offscreen", new NumberSetting("Size Offscreen", 10.0F, 2.0F, 50.0F).step(1.0F));
   private final BooleanSetting blinkOffscreen = this.addSetting("blink-offscreen", new BooleanSetting("Blink Offscreen", true));
   private final NumberSetting blinkOffscreenSpeed = this.addSetting(
      "blink-offscreen-speed", new NumberSetting("Blink Offscreen Speed", 4.0F, 1.0F, 15.0F).step(1.0F)
   );
   private final BooleanSetting distanceColors = this.addSetting("distance-colors", new BooleanSetting("Distance Colors", false));
   private final BooleanSetting friendOverride = this.addSetting("show-friend-colors", new BooleanSetting("Show Friend Colors", true));
   private final BooleanSetting teamColors = this.addSetting("team-colors", new BooleanSetting("Team Colors", true));
   private final ColorSetting friendColor = this.addSetting("friend-color", new ColorSetting("Friend Color", -16711756));
   private final ColorSetting playersColor = this.addSetting("players-colors", new ColorSetting("Players Color", 2144193997));
   private final ColorSetting animalsColor = this.addSetting("animals-color", new ColorSetting("Animals Color", 2140274577));
   private final ColorSetting waterAnimalsColor = this.addSetting("water-animals-color", new ColorSetting("Water Animals Color", 2140246527));
   private final ColorSetting monstersColor = this.addSetting("monsters-color", new ColorSetting("Monsters Color", 2147455377));
   private final ColorSetting ambientColor = this.addSetting("ambient-color", new ColorSetting("Ambient Color", 2135640907));
   private final ColorSetting miscColor = this.addSetting("misc-color", new ColorSetting("Misc Color", 2140246417));

   public TracersModule() {
      super("tracers", "Tracers", "Displays tracer lines to specified entities.", ModuleCategory.RENDER);
      this.target.visibleWhen(() -> this.style.is("Lines"));
      this.stem.visibleWhen(() -> this.style.is("Lines"));
      this.distanceOffscreen.visibleWhen(() -> this.style.is("Offscreen"));
      this.sizeOffscreen.visibleWhen(() -> this.style.is("Offscreen"));
      this.blinkOffscreen.visibleWhen(() -> this.style.is("Offscreen"));
      this.blinkOffscreenSpeed.visibleWhen(() -> this.style.is("Offscreen") && this.blinkOffscreen.get());
      this.friendOverride.visibleWhen(() -> this.distanceColors.get() && !this.ignoreFriends.get());
      this.teamColors.visibleWhen(() -> !this.distanceColors.get() && this.players.get());
      this.friendColor.visibleWhen(() -> !this.ignoreFriends.get() && this.players.get() && (!this.distanceColors.get() || this.friendOverride.get()));
      this.playersColor.visibleWhen(() -> !this.distanceColors.get() && this.players.get());
      this.animalsColor.visibleWhen(() -> !this.distanceColors.get() && this.animals.get());
      this.waterAnimalsColor.visibleWhen(() -> !this.distanceColors.get() && this.waterAnimals.get());
      this.monstersColor.visibleWhen(() -> !this.distanceColors.get() && this.monsters.get());
      this.ambientColor.visibleWhen(() -> !this.distanceColors.get() && this.ambient.get());
      this.miscColor.visibleWhen(() -> !this.distanceColors.get() && this.misc.get());
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (!var2.field_1690.field_1842 && this.style.is("Lines")) {
         for (Entity var4 : this.targets(var2)) {
            int var5 = this.colorOf(var2, var4);
            Vec3d var6 = var4.method_30950(var1.partialTick);
            double var7 = var4.method_5829().method_17940();
            String var11 = this.target.get();

            double var9 = switch (var11) {
               case "Head" -> var7;
               case "Body" -> var7 / 2.0;
               default -> 0.0;
            };
            TracerEngine.draw(var6.method_1031(0.0, var9, 0.0), var5);
            if (this.stem.get()) {
               TracerEngine.stem(var6, var7, var5);
            }
         }
      }
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.style.is("Offscreen") && !MinecraftClient.method_1551().field_1690.field_1842;
   }

   @Override
   public void renderHudOverlay(ImDrawList var1, float var2, float var3) {
      MinecraftClient var4 = MinecraftClient.method_1551();
      float var5 = var4.method_61966().method_60637(false);
      float var6 = this.blinkOffscreen.get() ? this.blinkAlpha() : 1.0F;
      float var7 = this.distanceOffscreen.get();
      float var8 = this.sizeOffscreen.get();

      for (Entity var10 : this.targets(var4)) {
         Vector4f var11 = TracerEngine.clip(var10.method_30950(var5));
         if (var11 == null) {
            return;
         }

         if (!(var11.w > 0.0F) || !(Math.abs(var11.x) < var11.w) || !(Math.abs(var11.y) < var11.w)) {
            double var12 = Math.atan2(-var11.y * var3, var11.x * var2);
            int var14 = this.colorOf(var4, var10);
            int var15 = (int)((var14 >>> 24) * var6);
            arrow(
               var1,
               var2 * 0.5F + var7 * (float)Math.cos(var12),
               var3 * 0.5F + var7 * (float)Math.sin(var12),
               var8,
               (float)var12,
               GuiPalette.abgr(var15 << 24 | var14 & 16777215)
            );
         }
      }
   }

   private List<Entity> targets(MinecraftClient var1) {
      if (var1.field_1687 != null && var1.field_1724 != null) {
         FreecamModule var2 = ModuleRegistry.get(FreecamModule.class);
         boolean var3 = this.ignoreSelf.get() || var2 == null || !var2.isEnabled();
         double var4 = (double)this.maxDistance.get() * this.maxDistance.get();
         Vec3d var6 = var1.field_1773.method_19418().method_71156();
         double var7 = ((Integer)var1.field_1690.method_42503().method_41753() + 1) * 16.0;
         ArrayList var9 = new ArrayList();

         for (Entity var11 : var1.field_1687.method_18112()) {
            if ((var11 != var1.field_1724 || !var3)
               && this.groupColor(var11) != null
               && !(var11.method_5858(var1.field_1724) > var4)
               && (!this.ignoreFriends.get() || !isFriend(var11))
               && (this.showInvisible.get() || !var11.method_5767())
               && !(Math.abs(var6.field_1352 - var11.method_23317()) >= var7)
               && !(Math.abs(var6.field_1350 - var11.method_23321()) >= var7)) {
               var9.add(var11);
            }
         }

         return var9;
      } else {
         return List.of();
      }
   }

   private ColorSetting groupColor(Entity var1) {
      if (var1 instanceof PlayerEntity) {
         return this.players.get() ? this.playersColor : null;
      } else {
         return switch (var1.method_5864().method_5891()) {
            case field_6294 -> this.animals.get() ? this.animalsColor : null;
            case field_24460, field_6300, field_30092, field_34447 -> this.waterAnimals.get() ? this.waterAnimalsColor : null;
            case field_6302 -> this.monsters.get() ? this.monstersColor : null;
            case field_6303 -> this.ambient.get() ? this.ambientColor : null;
            default -> this.misc.get() ? this.miscColor : null;
         };
      }
   }

   private static boolean isFriend(Entity var0) {
      return !(var0 instanceof PlayerEntity var1) ? false : FriendStore.getInstance().isFriend(var1.method_7334().name());
   }

   private int colorOf(MinecraftClient var1, Entity var2) {
      boolean var3 = isFriend(var2);
      if (!this.distanceColors.get()) {
         if (var2 instanceof PlayerEntity var4) {
            int var5 = this.playersColor.argb();
            int var6 = var5 & 0xFF000000;
            if (var3) {
               return var6 | this.friendColor.rgb();
            } else {
               int var7 = this.teamColors.get() ? teamColor(var4) : 16777215;
               return var7 == 16777215 ? var5 : var6 | var7;
            }
         } else {
            return this.groupColor(var2).argb();
         }
      } else {
         return var3 && this.friendOverride.get() ? this.friendColor.argb() : distanceColor(var1, var2);
      }
   }

   private static int distanceColor(MinecraftClient var0, Entity var1) {
      double var2 = var0.field_1773.method_19418().method_71156().method_1022(var1.method_33571()) / 60.0;
      if (var2 > 1.0) {
         return -16711936;
      } else {
         int var4 = var2 < 0.5 ? 255 : 255 - (int)(255.0 * (var2 - 0.5) / 0.5);
         int var5 = var2 < 0.5 ? (int)(255.0 * var2 / 0.5) : 255;
         return 0xFF000000 | var4 << 16 | var5 << 8;
      }
   }

   private static int teamColor(PlayerEntity var0) {
      Int2IntOpenHashMap var1 = new Int2IntOpenHashMap();
      var0.method_5476().method_27658((var1x, var2x) -> {
         TextColor var3x = var1x.method_10973();
         var1.addTo(var3x == null ? 16777215 : var3x.method_27716(), var2x.length());
         return Optional.empty();
      }, Style.field_24360);
      int var2 = 16777215;
      int var3 = 0;
      ObjectIterator var4 = var1.int2IntEntrySet().iterator();

      while (var4.hasNext()) {
         Entry var5 = (Entry)var4.next();
         if (var5.getIntValue() > var3) {
            var2 = var5.getIntKey();
            var3 = var5.getIntValue();
         }
      }

      return var2;
   }

   private float blinkAlpha() {
      double var1 = Util.method_658() * (this.blinkOffscreenSpeed.get() / 4.0);
      return (float)Math.abs(var1 % 1000.0 - 500.0) / 500.0F;
   }

   private static void arrow(ImDrawList var0, float var1, float var2, float var3, float var4, int var5) {
      float var6 = (float)Math.cos(var4);
      float var7 = (float)Math.sin(var4);
      float var8 = var1 - var3 * 1.26795F / 3.0F;
      float[] var9 = new float[]{var1 - var3, var1 + var3 * 0.73205F, var1 - var3};
      float[] var10 = new float[]{var2 - var3, var2, var2 + var3};

      for (int var11 = 0; var11 < 3; var11++) {
         float var12 = var9[var11] - var8;
         float var13 = var10[var11] - var2;
         var9[var11] = var8 + var12 * var6 - var13 * var7;
         var10[var11] = var2 + var12 * var7 + var13 * var6;
      }

      var0.addTriangleFilled(var9[0], var10[0], var9[1], var10[1], var9[2], var10[2], var5);
   }
}
