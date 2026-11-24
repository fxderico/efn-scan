package net.efn.client.module.impl.render;

import com.example.client.mixin.AccessorSpawnerNametagsBaseSpawner;
import imgui.ImDrawList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongPredicate;
import net.efn.client.esp.ChunkScanner;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.gui.GuiPalette;
import net.efn.client.gui.WorldLabel;
import net.efn.client.gui.WorldText;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.spawner.MobSpawnerEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public final class SpawnerNametagsModule extends ClientModule implements HudOverlayProvider, ChunkScanner.Consumer {
   private static final int MAX_LABELS = 48;
   private static final int RING_SEGMENTS = 64;
   private static final float RING_WIDTH = 1.5F;
   private static final float RING_ALPHA_INSIDE = 0.8F;
   private static final float RING_ALPHA_OUTSIDE = 0.4F;
   private static final double LABEL_HEIGHT = 1.2;
   private static final double FADE_SHARE = 0.125;
   private static final String SEPARATOR = "  \u00b7  ";
   private final NumberSetting sRange = this.addSetting("range", new NumberSetting("Range", 64.0F, 8.0F, 256.0F).step(1.0F));
   private final BooleanSetting sRadius = this.addSetting("activation-radius", new BooleanSetting("Activation Radius", true));
   private final LongOpenHashSet spawners = new LongOpenHashSet();

   public SpawnerNametagsModule() {
      super("spawner-nametags", "Spawner Nametags", "Shows each spawner's mob and activation range over the block.", ModuleCategory.RENDER);
   }

   @Override
   protected void onEnable() {
      ChunkScanner.register(this);
   }

   @Override
   protected void onDisable() {
      ChunkScanner.unregister(this);
      this.spawners.clear();
   }

   public static String mobName(MobSpawnerBlockEntity mobSpawnerBlockEntity) {
      MobSpawnerEntry var1 = ((AccessorSpawnerNametagsBaseSpawner)mobSpawnerBlockEntity.getLogic()).efn$spawnerNametagsNextSpawnData();
      if (var1 == null) {
         return null;
      } else {
         String var2 = var1.entity().getString("id", "");
         return var2.isEmpty() ? null : EntityType.get(var2).map(entityType -> entityType.getName().getString()).orElse(null);
      }
   }

   public static int activationRange(MobSpawnerBlockEntity mobSpawnerBlockEntity) {
      return ((AccessorSpawnerNametagsBaseSpawner)mobSpawnerBlockEntity.getLogic()).efn$spawnerNametagsRequiredPlayerRange();
   }

   @Override
   public boolean wants(BlockState blockState) {
      return blockState.isOf(Blocks.SPAWNER);
   }

   @Override
   public void found(long l, BlockState blockState) {
      this.spawners.add(l);
   }

   @Override
   public void removed(long l) {
      this.spawners.remove(l);
   }

   @Override
   public void reset() {
      this.spawners.clear();
   }

   @Override
   public void chunkScanned(long l, LongPredicate longPredicate) {
      LongIterator var4 = this.spawners.iterator();

      while (var4.hasNext()) {
         long var5 = var4.nextLong();
         if (ChunkPos.toLong(BlockPos.unpackLongX(var5) >> 4, BlockPos.unpackLongZ(var5) >> 4) == l && !longPredicate.test(var5)) {
            var4.remove();
         }
      }
   }

   private List<SpawnerNametagsModule.Shown> inRange(MinecraftClient minecraftClient) {
      ArrayList var2 = new ArrayList();
      if (!this.spawners.isEmpty() && minecraftClient.world != null && minecraftClient.player != null) {
         ClientWorld var3 = minecraftClient.world;
         Vec3d var4 = minecraftClient.player.getEntityPos();
         double var5 = this.sRange.get();
         LongIterator var7 = this.spawners.iterator();

         while (var7.hasNext()) {
            BlockPos var9 = BlockPos.fromLong(var7.nextLong());
            double var10 = var4.squaredDistanceTo(var9.getX() + 0.5, var9.getY() + 0.5, var9.getZ() + 0.5);
            BlockEntity var8;
            if (!(var10 > var5 * var5) && (var8 = var3.getBlockEntity(var9)) instanceof MobSpawnerBlockEntity) {
               MobSpawnerBlockEntity var12 = (MobSpawnerBlockEntity)var8;
               int var13 = activationRange(var12);
               boolean var14 = var13 < 0 || var10 < (double)var13 * var13;
               var2.add(new SpawnerNametagsModule.Shown(var9, Math.sqrt(var10), mobName(var12), var13, var14));
            }
         }

         var2.sort(Comparator.comparingDouble(SpawnerNametagsModule.Shown::distance));
         return (List<SpawnerNametagsModule.Shown>)(var2.size() > 48 ? var2.subList(0, 48) : var2);
      } else {
         return var2;
      }
   }

   private float fade(double d) {
      double var3 = this.sRange.get();
      return (float)Math.max(0.0, Math.min(1.0, (var3 - d) / (var3 * 0.125)));
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && !this.spawners.isEmpty();
   }

   @Override
   public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      if (!var4.options.hudHidden) {
         List var5 = this.inRange(var4);
         int var6 = WorldText.hudAccent();

         for (int var7 = var5.size() - 1; var7 >= 0; var7--) {
            SpawnerNametagsModule.Shown var10 = (SpawnerNametagsModule.Shown)var5.get(var7);
            float[] var11 = labelAnchor(var4, var10.pos(), f, g);
            if (var11 != null) {
               String var12 = var10.mob() == null ? "Spawner" : var10.mob() + " Spawner";
               String var13 = Math.round(var10.distance()) + "m";
               int[] var8;
               String[] var9;
               if (this.sRadius.get() && var10.range() > 0) {
                  var9 = new String[]{var13, "  \u00b7  ", var10.range() + "m range"};
                  var8 = new int[]{WorldText.LABEL, WorldText.LABEL, var10.inside() ? var6 : WorldText.LABEL};
               } else {
                  var9 = new String[]{var13};
                  var8 = new int[]{WorldText.LABEL};
               }

               WorldText.draw(imDrawList, var11[0], var11[1], var12, var9, var8, this.fade(var10.distance()));
            }
         }
      }
   }

   public static float[] labelAnchor(MinecraftClient minecraftClient, BlockPos blockPos, float f, float g) {
      float[] var4 = WorldLabel.project(minecraftClient, new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1.2, blockPos.getZ() + 0.5), f, g);
      if (var4 == null) {
         return null;
      } else {
         for (int var5 = 0; var5 < 4; var5++) {
            float[] var6 = WorldLabel.project(
               minecraftClient, new Vec3d(blockPos.getX() + (var5 & 1), blockPos.getY() + 1.0, blockPos.getZ() + (var5 >> 1)), f, g
            );
            if (var6 != null) {
               var4[1] = Math.min(var4[1], var6[1]);
            }
         }

         return var4;
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      if (this.sRadius.get()) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         List var3 = this.inRange(var2);
         if (!var3.isEmpty()) {
            int var4 = GuiPalette.abgr(WorldText.hudAccent()) & 16777215;

            for (SpawnerNametagsModule.Shown var6 : var3) {
               int var8;
               if (var6.range() > 0 && (var8 = Math.round((var6.inside() ? 0.8F : 0.4F) * this.fade(var6.distance()) * 255.0F) << 24 | var4) >>> 24 >= 4) {
                  drawRing(Vec3d.ofCenter(var6.pos()), var6.range(), var8);
               }
            }
         }
      }
   }

   private static void drawRing(Vec3d vec3d, double d, int i) {
      Vec3d var4 = null;

      for (int var5 = 0; var5 <= 64; var5++) {
         double var6 = 6.283185307179586 * var5 / 64.0;
         Vec3d var8 = vec3d.add(Math.cos(var6) * d, 0.0, Math.sin(var6) * d);
         if (var4 != null) {
            GizmoDrawing.line(var4, var8, i, 1.5F).ignoreOcclusion();
         }

         var4 = var8;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Shown(BlockPos pos, double distance, String mob, int range, boolean inside) {
   }
}
