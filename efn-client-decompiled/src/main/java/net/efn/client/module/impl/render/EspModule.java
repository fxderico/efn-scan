package net.efn.client.module.impl.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.efn.client.config.FriendStore;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public final class EspModule extends ClientModule {
   private static EspModule instance;
   private static final int ENTITY_CAP = 96;
   private static final Comparator<EspModule.Target> PRIORITY = Comparator.<EspModule.Target, Boolean>comparing(var0 -> var0.group() != EspModule.Group.PLAYER)
      .thenComparingDouble(EspModule.Target::distanceSq);
   private final BooleanSetting sPlayers = this.addSetting("players", new BooleanSetting("Players", true));
   private final BooleanSetting sMobs = this.addSetting("mobs", new BooleanSetting("Hostile Mobs", true));
   private final BooleanSetting sAnimals = this.addSetting("animals", new BooleanSetting("Passive Mobs", false));
   private final BooleanSetting sIgnoreSelf = this.addSetting("ignore-self", new BooleanSetting("Ignore Self", true));
   private final BooleanSetting sBox = this.addSetting("box", new BooleanSetting("Box", true));
   private final BooleanSetting sTracers = this.addSetting("tracers", new BooleanSetting("Tracers", true));
   private final BooleanSetting sCustomTracerColor = this.addSetting("custom-tracer-color", new BooleanSetting("Custom Tracer Color", false));
   private final ColorSetting sTracerColor = this.addSetting("tracer-color", new ColorSetting("Tracer Color", -1));
   private final ModeSetting sShape = this.addSetting("shape", new ModeSetting("Shape", "Glow", "Glow", "Box", "Fill", "Both"));
   private final NumberSetting sRange = this.addSetting("range", new NumberSetting("Range", 96.0F, 16.0F, 256.0F).step(1.0F));
   private final NumberSetting sMaxEntities = this.addSetting("max-entities", new NumberSetting("Max Entities", 48.0F, 8.0F, 96.0F).step(1.0F));
   private final ColorSetting sPlayerColor = this.addSetting("player-color", new ColorSetting("Player Color", -1));
   private final BooleanSetting sFriends = this.addSetting("friends", new BooleanSetting("Highlight Friends", true));
   private final ColorSetting sFriendColor = this.addSetting("friend-color", new ColorSetting("Friend Color", -10169601));
   private final ColorSetting sMobColor = this.addSetting("mob-color", new ColorSetting("Hostile Color", -47814));
   private final ColorSetting sAnimalColor = this.addSetting("animal-color", new ColorSetting("Passive Color", -13577896));
   private final NumberSetting sFillAlpha = this.addSetting("fill-alpha", new NumberSetting("Fill Alpha", 0.3F, 0.0F, 1.0F).step(0.05F));
   private final NumberSetting sLineWidth = this.addSetting("line-width", new NumberSetting("Line Width", 1.5F, 0.5F, 5.0F).step(0.5F));
   private final Map<Entity, Integer> glowing = new IdentityHashMap<>();
   private long glowFrame = -1L;
   private ClientWorld glowLevel;

   public EspModule() {
      super("esp", "ESP", "Highlights players, hostile and passive mobs with outlines, boxes and tracers.", ModuleCategory.RENDER);
      instance = this;
      this.sBox.visibleWhen(() -> !this.sShape.is("Glow"));
      this.sFillAlpha.visibleWhen(() -> this.sShape.is("Fill") || this.sShape.is("Both"));
      this.sLineWidth.visibleWhen(() -> this.sShape.is("Box") || this.sShape.is("Both"));
      this.sCustomTracerColor.visibleWhen(this.sTracers::get);
      this.sTracerColor.visibleWhen(() -> this.sTracers.get() && this.sCustomTracerColor.get());
      this.sPlayerColor.visibleWhen(this.sPlayers::get);
      this.sFriends.visibleWhen(this.sPlayers::get);
      this.sFriendColor.visibleWhen(() -> this.sPlayers.get() && this.sFriends.get());
      this.sMobColor.visibleWhen(this.sMobs::get);
      this.sAnimalColor.visibleWhen(this.sAnimals::get);
   }

   @Override
   protected void onDisable() {
      this.resetGlow();
   }

   public static boolean shouldGlow(Entity var0) {
      EspModule var1 = instance;
      return var1 != null && var1.isEnabled() && var1.sShape.is("Glow") ? var1.glowing().containsKey(var0) : false;
   }

   public static int glowColor(Entity var0) {
      EspModule var1 = instance;
      Integer var2 = var1 == null ? null : var1.glowing().get(var0);
      return (var2 == null ? 16777215 : var2) & 16777215;
   }

   private Map<Entity, Integer> glowing() {
      long var1 = Render3DEvent.frame();
      if (var1 != this.glowFrame) {
         this.glowFrame = var1;
         this.glowing.clear();

         for (EspModule.Target var4 : this.targets(1.0F)) {
            this.glowing.put(var4.entity(), this.colorFor(var4));
         }
      }

      return this.glowing;
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      ClientWorld var2 = MinecraftClient.method_1551().field_1687;
      if (var2 != this.glowLevel || !this.sShape.is("Glow")) {
         this.resetGlow();
      }

      this.glowLevel = var2;
   }

   private void resetGlow() {
      this.glowing.clear();
      this.glowFrame = -1L;
      this.glowLevel = null;
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      boolean var2 = this.sBox.get() && !this.sShape.is("Glow");
      boolean var3 = this.sTracers.get();
      if (var2 || var3) {
         String var4 = this.sShape.get();
         float var5 = this.sLineWidth.get();
         int var6 = Math.round(this.sFillAlpha.get() * 255.0F);

         for (EspModule.Target var8 : this.targets(var1.partialTick)) {
            int var9 = this.colorFor(var8);
            Box var10 = interpolatedBox(var8.entity(), var1.partialTick);
            if (var2) {
               int var11 = var9 & 16777215 | var6 << 24;

               DrawStyle var12 = switch (var4) {
                  case "Fill" -> DrawStyle.method_75539(var11);
                  case "Both" -> DrawStyle.method_75537(var9, var5, var11);
                  default -> DrawStyle.method_75536(var9, var5);
               };
               GizmoDrawing.method_75541(var10, var12).method_75533();
            }

            if (var3) {
               TracerEngine.draw(var10.method_1005(), this.sCustomTracerColor.get() ? this.sTracerColor.argb() : var9);
            }
         }
      }
   }

   private List<EspModule.Target> targets(float var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1687 != null && var2.field_1724 != null) {
         int var3 = Math.max(1, Math.min(96, this.sMaxEntities.getInt()));
         float var4 = Math.max(1.0F, this.sRange.get());
         double var5 = (double)var4 * var4;
         Vec3d var7 = var2.field_1724.method_5836(var1);
         ArrayList var8 = new ArrayList();

         for (Entity var10 : var2.field_1687.method_18112()) {
            EspModule.Group var15 = groupOf(var10);
            double var12;
            PlayerEntity var14;
            if (var15 != null
               && this.shown(var15)
               && (var10 != var2.field_1724 || !this.sIgnoreSelf.get())
               && !isGone(var10)
               && (!(var10 instanceof PlayerEntity) || !(var14 = (PlayerEntity)var10).method_7325())
               && !((var12 = var10.method_30950(var1).method_1025(var7)) > var5)) {
               var8.add(new EspModule.Target(var10, var15, var12));
            }
         }

         if (var8.size() <= var3) {
            return var8;
         } else {
            var8.sort(PRIORITY);
            return var8.subList(0, var3);
         }
      } else {
         return List.of();
      }
   }

   private static boolean isGone(Entity var0) {
      LivingEntity var1;
      return var0.method_31481() || var0 instanceof LivingEntity && (var1 = (LivingEntity)var0).method_29504();
   }

   private static EspModule.Group groupOf(Entity var0) {
      if (var0 instanceof PlayerEntity) {
         return EspModule.Group.PLAYER;
      } else if (var0 instanceof Monster) {
         return EspModule.Group.HOSTILE;
      } else {
         return var0 instanceof MobEntity ? EspModule.Group.PASSIVE : null;
      }
   }

   private boolean shown(EspModule.Group var1) {
      return switch (var1) {
         case PLAYER -> this.sPlayers.get();
         case HOSTILE -> this.sMobs.get();
         case PASSIVE -> this.sAnimals.get();
      };
   }

   private int colorFor(EspModule.Target var1) {
      return switch (var1.group()) {
         case PLAYER -> {
            Entity var2;
            if (this.sFriends.get() && (var2 = var1.entity()) instanceof PlayerEntity) {
               PlayerEntity var3 = (PlayerEntity)var2;
               if (FriendStore.getInstance().isFriend(var3.method_7334().name())) {
                  yield this.sFriendColor.argb();
               }
            }

            yield this.sPlayerColor.argb();
         }
         case HOSTILE -> this.sMobColor.argb();
         case PASSIVE -> this.sAnimalColor.argb();
      };
   }

   private static Box interpolatedBox(Entity var0, float var1) {
      Vec3d var2 = var0.method_73189();
      Vec3d var3 = var0.method_30950(var1);
      return var0.method_5829().method_989(var3.field_1352 - var2.field_1352, var3.field_1351 - var2.field_1351, var3.field_1350 - var2.field_1350);
   }

   @Environment(EnvType.CLIENT)
   private static enum Group {
      PLAYER,
      HOSTILE,
      PASSIVE;

      private Group() {
      }
   }

   @Environment(EnvType.CLIENT)
   private record Target(Entity entity, EspModule.Group group, double distanceSq) {
   }
}
