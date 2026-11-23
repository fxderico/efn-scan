package net.efn.client.module.impl.render;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.WorldLabel;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.ActionSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix3x2fStack;

@Environment(EnvType.CLIENT)
public final class WaypointsModule extends ClientModule {
   private static final Pattern SPLIT = Pattern.compile("[\\s,]+");
   private static final Pattern DEATH_NAME = Pattern.compile("Death \\d{2}:\\d{2}");
   private static final DateTimeFormatter DEATH_TIME = DateTimeFormatter.ofPattern("HH:mm");
   private static final double PROJECT_DISTANCE = 64.0;
   private static final double NETHER_SCALE = 8.0;
   private static final float FOCUS_RADIUS = 40.0F;
   private static final float FADE_BLOCKS = 4.0F;
   private static final int PADDING = 3;
   private static final int MARKER = 5;
   private static final int PANEL_RGB = 1052692;
   private static final int TEXT_LABEL = -4934466;
   private final StringListSetting points = this.addSetting("points", new StringListSetting("Waypoints"));
   private final KeySetting addKey = this.addSetting("add-key", new KeySetting("Add Here Key", -1));
   private final ActionSetting addHere = this.addSetting("add-here", new ActionSetting("Add Here", this::addHere));
   private final BooleanSetting deaths = this.addSetting("deaths", new BooleanSetting("Death Waypoints", true));
   private final NumberSetting deathMemory = this.addSetting("death-memory", new NumberSetting("Death Memory", 3.0F, 1.0F, 10.0F).step(1.0F));
   private final BooleanSetting otherDimension = this.addSetting("other-dimension", new BooleanSetting("Show In Other Dimension", true));
   private final BooleanSetting showDistance = this.addSetting("show-distance", new BooleanSetting("Show Distance", true));
   private final NumberSetting maxDistance = this.addSetting("max-distance", new NumberSetting("Max Distance", 0.0F, 0.0F, 30000.0F).step(100.0F));
   private final NumberSetting hideWithin = this.addSetting("hide-within", new NumberSetting("Hide Within", 3.0F, 0.0F, 32.0F).step(1.0F));
   private final NumberSetting scale = this.addSetting("scale", new NumberSetting("Scale", 1.0F, 0.5F, 2.0F).step(0.1F));
   private final ColorSetting color = this.addSetting("color", new ColorSetting("Color", -14006));
   private final ColorSetting deathColor = this.addSetting("death-color", new ColorSetting("Death Color", -47814));
   private List<WaypointsModule.Point> parsed;
   private boolean wasDead;

   public WaypointsModule() {
      super(
         "waypoints",
         "Waypoints",
         "Saved places shown on screen with distance. Entries are \"Name x y z\", optionally ending in nether or end.",
         ModuleCategory.RENDER
      );
      this.points.onChange(() -> this.parsed = null);
      this.deathMemory.visibleWhen(this.deaths::get);
      this.deathColor.visibleWhen(this.deaths::get);
   }

   @Override
   protected void onEnable() {
      this.wasDead = false;
      this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.NORMAL, var1 -> {
         if (var1.action == 1) {
            this.pressed(var1.key);
         }
      });
      this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.NORMAL, var1 -> {
         if (var1.action == 1) {
            this.pressed(1000 + var1.button);
         }
      });
   }

   private void pressed(int var1) {
      if (ModuleKeybinds.isBindable(var1) && var1 == this.addKey.value() && MinecraftClient.method_1551().field_1755 == null) {
         this.addHere();
      }
   }

   private void addHere() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 != null && var1.field_1687 != null) {
         String var3 = this.freeName();
         this.save(var3, var2.method_31477(), var2.method_31478(), var2.method_31479(), var1.field_1687.method_27983());
      } else {
         this.say(ModuleChat.body().warn("Join a world first."));
      }
   }

   private void save(String var1, int var2, int var3, int var4, RegistryKey<World> var5) {
      String var6 = var1 + " " + var2 + " " + var3 + " " + var4 + suffix(var5);
      if (!this.points.addValue(var6)) {
         this.say(ModuleChat.body().warn("The waypoint list is full").text(" (64), remove one first."));
      } else {
         this.say(ModuleChat.body().good("Saved ").name(var1).text(" at ").coords(var2, var3, var4).text("."));
      }
   }

   private String freeName() {
      List var1 = this.parsed();
      int var2 = var1.size() + 1;

      while (true) {
         String var3 = "Waypoint " + var2;
         if (var1.stream().noneMatch(var1x -> var1x.name().equalsIgnoreCase(var3))) {
            return var3;
         }

         var2++;
      }
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var3 != null && var2.field_1687 != null) {
         boolean var4 = var3.method_29504();
         if (var4 && !this.wasDead && this.deaths.get()) {
            this.forgetOldDeaths(Math.max(0, this.deathMemory.getInt() - 1));
            this.save(
               "Death " + LocalTime.now().format(DEATH_TIME), var3.method_31477(), var3.method_31478(), var3.method_31479(), var2.field_1687.method_27983()
            );
         }

         this.wasDead = var4;
      } else {
         this.wasDead = false;
      }
   }

   private void forgetOldDeaths(int var1) {
      ArrayList var3 = new ArrayList();

      for (int var2 = 0; var2 < this.points.size(); var2++) {
         WaypointsModule.Point var4 = parse(this.points.entry(var2).get());
         if (var4 != null && var4.death()) {
            var3.add(var2);
         }
      }

      for (int var5 = var3.size() - var1 - 1; var5 >= 0; var5--) {
         this.points.remove((Integer)var3.get(var5));
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1687 != null && var2.field_1724 != null && !var2.field_1690.field_1842) {
         Camera var3 = var2.field_1773.method_19418();
         if (var3 != null && var3.method_19332()) {
            Vec3d var4 = var3.method_71156();
            RegistryKey var5 = var2.field_1687.method_27983();
            double var6 = this.maxDistance.get();
            ArrayList var8 = new ArrayList();

            for (WaypointsModule.Point var10 : this.parsed()) {
               WaypointsModule.Shown var11 = this.place(var10, var5, var4);
               if (var11 != null && (!(var6 > 0.0) || !(var11.distance() > var6))) {
                  var8.add(var11);
               }
            }

            var8.sort(Comparator.comparingDouble(WaypointsModule.Shown::distance).reversed());
            DrawContext var12 = var1.graphics;

            for (WaypointsModule.Shown var14 : var8) {
               this.draw(var12, var2, var4, var14);
            }
         }
      }
   }

   private WaypointsModule.Shown place(WaypointsModule.Point var1, RegistryKey<World> var2, Vec3d var3) {
      double var4 = var1.x() + 0.5;
      double var6 = var1.z() + 0.5;
      boolean var8 = false;
      if (var1.dimension() != var2) {
         if (!this.otherDimension.get()) {
            return null;
         }

         if (var1.dimension() == World.field_25179 && var2 == World.field_25180) {
            var4 /= 8.0;
            var6 /= 8.0;
         } else {
            if (var1.dimension() != World.field_25180 || var2 != World.field_25179) {
               return null;
            }

            var4 *= 8.0;
            var6 *= 8.0;
         }

         var8 = true;
      }

      double var9 = var1.y() != null && !var8 ? var1.y().intValue() + 0.5 : var3.field_1351;
      Vec3d var11 = new Vec3d(var4, var9, var6);
      return new WaypointsModule.Shown(var1, var11, var11.method_1022(var3), var8);
   }

   private void draw(DrawContext var1, MinecraftClient var2, Vec3d var3, WaypointsModule.Shown var4) {
      float var9 = Math.min(1.0F, Math.max(0.0F, (float)(var4.distance() - this.hideWithin.get()) / 4.0F));
      if (!(var9 <= 0.02F)) {
         Vec3d var10 = var4.at().method_1020(var3);
         Vec3d var11 = var4.distance() > 64.0 ? var3.method_1019(var10.method_1021(64.0 / var4.distance())) : var4.at();
         float[] var12 = WorldLabel.project(var2, var11, var1.method_51421(), var1.method_51443());
         if (var12 != null) {
            WaypointsModule.Point var13 = var4.point();
            TextRenderer var14 = var2.field_1772;
            int var15 = var13.death() ? this.deathColor.argb() : this.color.argb();
            String var16 = var13.name();
            String var8 = this.showDistance.get() ? distance(var4.distance()) : null;
            if (var4.converted()) {
               var8 = (var8 == null ? "" : var8 + " ") + "(" + dimensionName(var13.dimension()) + ")";
            }

            float var6;
            float var7;
            String var18 = (var7 = var12[0] - var1.method_51421() * 0.5F) * var7 + (var6 = var12[1] - var1.method_51443() * 0.5F) * var6 <= 1600.0F
               ? var13.x() + ", " + (var13.y() == null ? "~" : var13.y()) + ", " + var13.z()
               : null;
            int var19 = var14.method_1727(var16) + (var8 != null ? 4 + var14.method_1727(var8) : 0);
            int var20 = Math.max(var19, var18 != null ? var14.method_1727(var18) : 0) + 6;
            Objects.requireNonNull(var14);
            byte var5;
            if (var18 != null) {
               var5 = 10;
            } else {
               var5 = 0;
            }

            int var21 = 15 + var5;
            Matrix3x2fStack var22 = var1.method_51448();
            var22.pushMatrix();
            var22.translate(var12[0], var12[1]);
            float var23 = this.scale.get();
            var22.scale(var23, var23);
            int var24 = fade(var15, var9);
            var1.method_25294(-2, -2, 3, 3, fade(-16777216, var9 * 0.6F));
            var1.method_25294(-1, -1, 2, 2, var24);
            int var25 = -var20 / 2;
            int var26 = -7 - var21;
            var1.method_25294(var25, var26, var25 + var20, var26 + var21, fade(-15724524, var9 * 0.55F));
            var1.method_25294(var25, var26 + var21 - 1, var25 + var20, var26 + var21, var24);
            int var27 = var25 + 3;
            int var28 = var26 + 3;
            var1.method_51433(var14, var16, var27, var28, fade(var15, var9), true);
            if (var8 != null) {
               var1.method_51433(var14, var8, var27 + var14.method_1727(var16) + 4, var28, fade(-4934466, var9), true);
            }

            if (var18 != null) {
               Objects.requireNonNull(var14);
               var1.method_51433(var14, var18, var27, var28 + 9 + 1, fade(-1, var9), true);
            }

            var22.popMatrix();
         }
      }
   }

   private List<WaypointsModule.Point> parsed() {
      if (this.parsed == null) {
         ArrayList var1 = new ArrayList();

         for (String var3 : this.points.get()) {
            WaypointsModule.Point var4 = parse(var3);
            if (var4 != null) {
               var1.add(var4);
            }
         }

         this.parsed = List.copyOf(var1);
      }

      return this.parsed;
   }

   private static WaypointsModule.Point parse(String var0) {
      ArrayList var3 = new ArrayList<>(List.of(SPLIT.split(var0.trim())));
      var3.removeIf(String::isEmpty);
      RegistryKey var4 = World.field_25179;
      RegistryKey var2;
      if (!var3.isEmpty() && (var2 = dimension((String)var3.get(var3.size() - 1))) != null) {
         var4 = var2;
         var3.remove(var3.size() - 1);
      }

      ArrayList var5 = new ArrayList();

      Integer var1;
      while (!var3.isEmpty() && var5.size() < 3 && (var1 = number((String)var3.get(var3.size() - 1))) != null) {
         var5.add(0, var1);
         var3.remove(var3.size() - 1);
      }

      if (var5.size() < 2) {
         return null;
      } else {
         String var6 = String.join(" ", var3).trim();
         if (var6.isEmpty()) {
            var6 = "Waypoint";
         }

         boolean var7 = DEATH_NAME.matcher(var6).matches();
         return var5.size() == 3
            ? new WaypointsModule.Point(var6, (Integer)var5.get(0), (Integer)var5.get(1), (Integer)var5.get(2), var4, var7)
            : new WaypointsModule.Point(var6, (Integer)var5.get(0), null, (Integer)var5.get(1), var4, var7);
      }
   }

   private static Integer number(String var0) {
      try {
         return (int)Math.floor(Double.parseDouble(var0));
      } catch (NumberFormatException var2) {
         return null;
      }
   }

   private static RegistryKey<World> dimension(String var0) {
      String var1 = var0.toLowerCase(Locale.ROOT);

      return switch (var1) {
         case "nether", "the_nether", "minecraft:the_nether" -> World.field_25180;
         case "end", "the_end", "minecraft:the_end" -> World.field_25181;
         case "overworld", "minecraft:overworld" -> World.field_25179;
         default -> null;
      };
   }

   private static String suffix(RegistryKey<World> var0) {
      return var0 == World.field_25180 ? " nether" : (var0 == World.field_25181 ? " end" : "");
   }

   private static String dimensionName(RegistryKey<World> var0) {
      return var0 == World.field_25180 ? "Nether" : (var0 == World.field_25181 ? "End" : "Overworld");
   }

   private static String distance(double var0) {
      return var0 >= 10000.0 ? String.format(Locale.ROOT, "%.1fk m", var0 / 1000.0) : Math.round(var0) + "m";
   }

   private static int fade(int var0, float var1) {
      int var2 = Math.round((var0 >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, var1)));
      return var2 << 24 | var0 & 16777215;
   }

   private void say(ModuleChat.Body var1) {
      ModuleChat.send(this, var1);
   }

   @Environment(EnvType.CLIENT)
   private record Point(String name, int x, Integer y, int z, RegistryKey<World> dimension, boolean death) {
   }

   @Environment(EnvType.CLIENT)
   private record Shown(WaypointsModule.Point point, Vec3d at, double distance, boolean converted) {
   }
}
