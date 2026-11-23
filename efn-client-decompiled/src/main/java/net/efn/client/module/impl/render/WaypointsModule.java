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
      this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.NORMAL, keyInputEvent -> {
         if (keyInputEvent.action == 1) {
            this.pressed(keyInputEvent.key);
         }
      });
      this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.NORMAL, mouseInputEvent -> {
         if (mouseInputEvent.action == 1) {
            this.pressed(1000 + mouseInputEvent.button);
         }
      });
   }

   private void pressed(int i) {
      if (ModuleKeybinds.isBindable(i) && i == this.addKey.value() && MinecraftClient.getInstance().currentScreen == null) {
         this.addHere();
      }
   }

   private void addHere() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      ClientPlayerEntity var2 = var1.player;
      if (var2 != null && var1.world != null) {
         String var3 = this.freeName();
         this.save(var3, var2.getBlockX(), var2.getBlockY(), var2.getBlockZ(), var1.world.getRegistryKey());
      } else {
         this.say(ModuleChat.body().warn("Join a world first."));
      }
   }

   private void save(String string, int i, int j, int k, RegistryKey<World> registryKey) {
      String var6 = string + " " + i + " " + j + " " + k + suffix(registryKey);
      if (!this.points.addValue(var6)) {
         this.say(ModuleChat.body().warn("The waypoint list is full").text(" (64), remove one first."));
      } else {
         this.say(ModuleChat.body().good("Saved ").name(string).text(" at ").coords(i, j, k).text("."));
      }
   }

   private String freeName() {
      List var1 = this.parsed();
      int var2 = var1.size() + 1;

      while (true) {
         String var3 = "Waypoint " + var2;
         if (var1.stream().noneMatch(point -> point.name().equalsIgnoreCase(var3))) {
            return var3;
         }

         var2++;
      }
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null && var2.world != null) {
         boolean var4 = var3.isDead();
         if (var4 && !this.wasDead && this.deaths.get()) {
            this.forgetOldDeaths(Math.max(0, this.deathMemory.getInt() - 1));
            this.save("Death " + LocalTime.now().format(DEATH_TIME), var3.getBlockX(), var3.getBlockY(), var3.getBlockZ(), var2.world.getRegistryKey());
         }

         this.wasDead = var4;
      } else {
         this.wasDead = false;
      }
   }

   private void forgetOldDeaths(int i) {
      ArrayList var3 = new ArrayList();

      for (int var2 = 0; var2 < this.points.size(); var2++) {
         WaypointsModule.Point var4 = parse(this.points.entry(var2).get());
         if (var4 != null && var4.death()) {
            var3.add(var2);
         }
      }

      for (int var5 = var3.size() - i - 1; var5 >= 0; var5--) {
         this.points.remove((Integer)var3.get(var5));
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent render2DEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world != null && var2.player != null && !var2.options.hudHidden) {
         Camera var3 = var2.gameRenderer.getCamera();
         if (var3 != null && var3.isReady()) {
            Vec3d var4 = var3.getCameraPos();
            RegistryKey var5 = var2.world.getRegistryKey();
            double var6 = this.maxDistance.get();
            ArrayList var8 = new ArrayList();

            for (WaypointsModule.Point var10 : this.parsed()) {
               WaypointsModule.Shown var11 = this.place(var10, var5, var4);
               if (var11 != null && (!(var6 > 0.0) || !(var11.distance() > var6))) {
                  var8.add(var11);
               }
            }

            var8.sort(Comparator.comparingDouble(WaypointsModule.Shown::distance).reversed());
            DrawContext var12 = render2DEvent.graphics;

            for (WaypointsModule.Shown var14 : var8) {
               this.draw(var12, var2, var4, var14);
            }
         }
      }
   }

   private WaypointsModule.Shown place(WaypointsModule.Point point, RegistryKey<World> registryKey, Vec3d vec3d) {
      double var4 = point.x() + 0.5;
      double var6 = point.z() + 0.5;
      boolean var8 = false;
      if (point.dimension() != registryKey) {
         if (!this.otherDimension.get()) {
            return null;
         }

         if (point.dimension() == World.OVERWORLD && registryKey == World.NETHER) {
            var4 /= 8.0;
            var6 /= 8.0;
         } else {
            if (point.dimension() != World.NETHER || registryKey != World.OVERWORLD) {
               return null;
            }

            var4 *= 8.0;
            var6 *= 8.0;
         }

         var8 = true;
      }

      double var9 = point.y() != null && !var8 ? point.y().intValue() + 0.5 : vec3d.y;
      Vec3d var11 = new Vec3d(var4, var9, var6);
      return new WaypointsModule.Shown(point, var11, var11.distanceTo(vec3d), var8);
   }

   private void draw(DrawContext drawContext, MinecraftClient minecraftClient, Vec3d vec3d, WaypointsModule.Shown shown) {
      float var9 = Math.min(1.0F, Math.max(0.0F, (float)(shown.distance() - this.hideWithin.get()) / 4.0F));
      if (!(var9 <= 0.02F)) {
         Vec3d var10 = shown.at().subtract(vec3d);
         Vec3d var11 = shown.distance() > 64.0 ? vec3d.add(var10.multiply(64.0 / shown.distance())) : shown.at();
         float[] var12 = WorldLabel.project(minecraftClient, var11, drawContext.getScaledWindowWidth(), drawContext.getScaledWindowHeight());
         if (var12 != null) {
            WaypointsModule.Point var13 = shown.point();
            TextRenderer var14 = minecraftClient.textRenderer;
            int var15 = var13.death() ? this.deathColor.argb() : this.color.argb();
            String var16 = var13.name();
            String var8 = this.showDistance.get() ? distance(shown.distance()) : null;
            if (shown.converted()) {
               var8 = (var8 == null ? "" : var8 + " ") + "(" + dimensionName(var13.dimension()) + ")";
            }

            float var6;
            float var7;
            String var18 = (var7 = var12[0] - drawContext.getScaledWindowWidth() * 0.5F) * var7
                     + (var6 = var12[1] - drawContext.getScaledWindowHeight() * 0.5F) * var6
                  <= 1600.0F
               ? var13.x() + ", " + (var13.y() == null ? "~" : var13.y()) + ", " + var13.z()
               : null;
            int var19 = var14.getWidth(var16) + (var8 != null ? 4 + var14.getWidth(var8) : 0);
            int var20 = Math.max(var19, var18 != null ? var14.getWidth(var18) : 0) + 6;
            Objects.requireNonNull(var14);
            byte var5;
            if (var18 != null) {
               var5 = 10;
            } else {
               var5 = 0;
            }

            int var21 = 15 + var5;
            Matrix3x2fStack var22 = drawContext.getMatrices();
            var22.pushMatrix();
            var22.translate(var12[0], var12[1]);
            float var23 = this.scale.get();
            var22.scale(var23, var23);
            int var24 = fade(var15, var9);
            drawContext.fill(-2, -2, 3, 3, fade(-16777216, var9 * 0.6F));
            drawContext.fill(-1, -1, 2, 2, var24);
            int var25 = -var20 / 2;
            int var26 = -7 - var21;
            drawContext.fill(var25, var26, var25 + var20, var26 + var21, fade(-15724524, var9 * 0.55F));
            drawContext.fill(var25, var26 + var21 - 1, var25 + var20, var26 + var21, var24);
            int var27 = var25 + 3;
            int var28 = var26 + 3;
            drawContext.drawText(var14, var16, var27, var28, fade(var15, var9), true);
            if (var8 != null) {
               drawContext.drawText(var14, var8, var27 + var14.getWidth(var16) + 4, var28, fade(-4934466, var9), true);
            }

            if (var18 != null) {
               Objects.requireNonNull(var14);
               drawContext.drawText(var14, var18, var27, var28 + 9 + 1, fade(-1, var9), true);
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

   private static WaypointsModule.Point parse(String string) {
      ArrayList var3 = new ArrayList<>(List.of(SPLIT.split(string.trim())));
      var3.removeIf(String::isEmpty);
      RegistryKey var4 = World.OVERWORLD;
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

   private static Integer number(String string) {
      try {
         return (int)Math.floor(Double.parseDouble(string));
      } catch (NumberFormatException var2) {
         return null;
      }
   }

   private static RegistryKey<World> dimension(String string) {
      String var1 = string.toLowerCase(Locale.ROOT);

      return switch (var1) {
         case "nether", "the_nether", "minecraft:the_nether" -> World.NETHER;
         case "end", "the_end", "minecraft:the_end" -> World.END;
         case "overworld", "minecraft:overworld" -> World.OVERWORLD;
         default -> null;
      };
   }

   private static String suffix(RegistryKey<World> registryKey) {
      return registryKey == World.NETHER ? " nether" : (registryKey == World.END ? " end" : "");
   }

   private static String dimensionName(RegistryKey<World> registryKey) {
      return registryKey == World.NETHER ? "Nether" : (registryKey == World.END ? "End" : "Overworld");
   }

   private static String distance(double d) {
      return d >= 10000.0 ? String.format(Locale.ROOT, "%.1fk m", d / 1000.0) : Math.round(d) + "m";
   }

   private static int fade(int i, float f) {
      int var2 = Math.round((i >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, f)));
      return var2 << 24 | i & 16777215;
   }

   private void say(ModuleChat.Body body) {
      ModuleChat.send(this, body);
   }

   @Environment(EnvType.CLIENT)
   private record Point(String name, int x, Integer y, int z, RegistryKey<World> dimension, boolean death) {
   }

   @Environment(EnvType.CLIENT)
   private record Shown(WaypointsModule.Point point, Vec3d at, double distance, boolean converted) {
   }
}
