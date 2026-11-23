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

   public static String mobName(MobSpawnerBlockEntity var0) {
      MobSpawnerEntry var1 = ((AccessorSpawnerNametagsBaseSpawner)var0.method_11390()).efn$spawnerNametagsNextSpawnData();
      if (var1 == null) {
         return null;
      } else {
         String var2 = var1.comp_64().method_68564("id", "");
         return var2.isEmpty() ? null : EntityType.method_5898(var2).map(var0x -> var0x.method_5897().getString()).orElse(null);
      }
   }

   public static int activationRange(MobSpawnerBlockEntity var0) {
      return ((AccessorSpawnerNametagsBaseSpawner)var0.method_11390()).efn$spawnerNametagsRequiredPlayerRange();
   }

   @Override
   public boolean wants(BlockState var1) {
      return var1.method_27852(Blocks.field_10260);
   }

   @Override
   public void found(long var1, BlockState var3) {
      this.spawners.add(var1);
   }

   @Override
   public void removed(long var1) {
      this.spawners.remove(var1);
   }

   @Override
   public void reset() {
      this.spawners.clear();
   }

   @Override
   public void chunkScanned(long var1, LongPredicate var3) {
      LongIterator var4 = this.spawners.iterator();

      while (var4.hasNext()) {
         long var5 = var4.nextLong();
         if (ChunkPos.method_8331(BlockPos.method_10061(var5) >> 4, BlockPos.method_10083(var5) >> 4) == var1 && !var3.test(var5)) {
            var4.remove();
         }
      }
   }

   private List<SpawnerNametagsModule.Shown> inRange(MinecraftClient var1) {
      ArrayList var2 = new ArrayList();
      if (!this.spawners.isEmpty() && var1.field_1687 != null && var1.field_1724 != null) {
         ClientWorld var3 = var1.field_1687;
         Vec3d var4 = var1.field_1724.method_73189();
         double var5 = this.sRange.get();
         LongIterator var7 = this.spawners.iterator();

         while (var7.hasNext()) {
            BlockPos var9 = BlockPos.method_10092(var7.nextLong());
            double var10 = var4.method_1028(var9.method_10263() + 0.5, var9.method_10264() + 0.5, var9.method_10260() + 0.5);
            BlockEntity var8;
            if (!(var10 > var5 * var5) && (var8 = var3.method_8321(var9)) instanceof MobSpawnerBlockEntity) {
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

   private float fade(double var1) {
      double var3 = this.sRange.get();
      return (float)Math.max(0.0, Math.min(1.0, (var3 - var1) / (var3 * 0.125)));
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && !this.spawners.isEmpty();
   }

   @Override
   public void renderHudOverlay(ImDrawList var1, float var2, float var3) {
      MinecraftClient var4 = MinecraftClient.method_1551();
      if (!var4.field_1690.field_1842) {
         List var5 = this.inRange(var4);
         int var6 = WorldText.hudAccent();

         for (int var7 = var5.size() - 1; var7 >= 0; var7--) {
            SpawnerNametagsModule.Shown var10 = (SpawnerNametagsModule.Shown)var5.get(var7);
            float[] var11 = labelAnchor(var4, var10.pos(), var2, var3);
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

               WorldText.draw(var1, var11[0], var11[1], var12, var9, var8, this.fade(var10.distance()));
            }
         }
      }
   }

   public static float[] labelAnchor(MinecraftClient var0, BlockPos var1, float var2, float var3) {
      float[] var4 = WorldLabel.project(var0, new Vec3d(var1.method_10263() + 0.5, var1.method_10264() + 1.2, var1.method_10260() + 0.5), var2, var3);
      if (var4 == null) {
         return null;
      } else {
         for (int var5 = 0; var5 < 4; var5++) {
            float[] var6 = WorldLabel.project(
               var0, new Vec3d(var1.method_10263() + (var5 & 1), var1.method_10264() + 1.0, var1.method_10260() + (var5 >> 1)), var2, var3
            );
            if (var6 != null) {
               var4[1] = Math.min(var4[1], var6[1]);
            }
         }

         return var4;
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      if (this.sRadius.get()) {
         MinecraftClient var2 = MinecraftClient.method_1551();
         List var3 = this.inRange(var2);
         if (!var3.isEmpty()) {
            int var4 = GuiPalette.abgr(WorldText.hudAccent()) & 16777215;

            for (SpawnerNametagsModule.Shown var6 : var3) {
               int var8;
               if (var6.range() > 0 && (var8 = Math.round((var6.inside() ? 0.8F : 0.4F) * this.fade(var6.distance()) * 255.0F) << 24 | var4) >>> 24 >= 4) {
                  drawRing(Vec3d.method_24953(var6.pos()), var6.range(), var8);
               }
            }
         }
      }
   }

   private static void drawRing(Vec3d var0, double var1, int var3) {
      Vec3d var4 = null;

      for (int var5 = 0; var5 <= 64; var5++) {
         double var6 = 6.283185307179586 * var5 / 64.0;
         Vec3d var8 = var0.method_1031(Math.cos(var6) * var1, 0.0, Math.sin(var6) * var1);
         if (var4 != null) {
            GizmoDrawing.method_75546(var4, var8, var3, 1.5F).method_75533();
         }

         var4 = var8;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Shown(BlockPos pos, double distance, String mob, int range, boolean inside) {
   }
}
