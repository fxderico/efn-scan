package net.efn.client.easyplace.schematic;

import com.google.common.collect.UnmodifiableIterator;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.LayerRange;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.efn.client.easyplace.EasyPlace;
import net.efn.client.easyplace.util.BlockStateUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class SchematicManager {
   private static final int CACHE_REFRESH_TICKS = 5;
   private static final int NAV_REFRESH_TICKS = 20;
   private final MinecraftClient mc;
   private final Map<Long, SchematicTarget> cachedTargets = new LinkedHashMap<>();
   private final List<SchematicManager.ActiveBox> activeBoxes = new ArrayList<>();
   private long lastRefreshTick = -9223372036854775808L;
   private BlockPos lastCenter;
   private double lastRange = -1.0;
   private boolean lastAura;
   private boolean lastAllBlocks;
   private int lastLayerRangeHash;
   private SchematicTarget cachedNavTarget;
   private long lastNavRefreshTick = -9223372036854775808L;

   public SchematicManager(MinecraftClient var1) {
      this.mc = var1;
   }

   public void clear() {
      this.cachedTargets.clear();
      this.activeBoxes.clear();
      this.lastRefreshTick = -9223372036854775808L;
      this.lastNavRefreshTick = -9223372036854775808L;
      this.cachedNavTarget = null;
      this.lastCenter = null;
   }

   public void invalidate() {
      this.lastRefreshTick = -9223372036854775808L;
   }

   public boolean hasActiveSchematic() {
      return this.rebuildActiveBoxes();
   }

   public List<SchematicManager.ActiveBox> activeBoxes() {
      this.rebuildActiveBoxes();
      return List.copyOf(this.activeBoxes);
   }

   public WorldSchematic getSchematicWorld() {
      return SchematicWorldHandler.getSchematicWorld();
   }

   public Collection<SchematicTarget> getTargets(long var1, double var3, boolean var5, boolean var6) {
      if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
         BlockPos var8 = this.mc.field_1724.method_24515();
         boolean var9 = this.lastCenter == null || var8.method_10262((Vec3i)this.lastCenter) > 1.0;
         int var10 = this.getLayerRangeHash();
         boolean var7 = var3 != this.lastRange || var5 != this.lastAura || var6 != this.lastAllBlocks || var10 != this.lastLayerRangeHash;
         if (var1 - this.lastRefreshTick >= 5L || var9 || var7) {
            this.refresh(var1, var8, var3, var5, var6);
         }

         return this.cachedTargets.values();
      } else {
         this.clear();
         return List.of();
      }
   }

   private void refresh(long var1, BlockPos var3, double var4, boolean var6, boolean var7) {
      this.cachedTargets.clear();
      this.lastRefreshTick = var1;
      this.lastCenter = var3.method_10062();
      this.lastRange = var4;
      this.lastAura = var6;
      this.lastAllBlocks = var7;
      this.lastLayerRangeHash = this.getLayerRangeHash();
      if (this.rebuildActiveBoxes()) {
         WorldSchematic var8 = SchematicWorldHandler.getSchematicWorld();
         if (var8 != null) {
            if (!var6) {
               BlockPos var17 = RayTraceUtils.getSchematicWorldTraceIfClosestNoFluids((World)this.mc.field_1687, (Entity)this.mc.field_1724, var4);
               if (var17 != null) {
                  this.addTarget(var17, var8, var7);
               }
            } else {
               Vec3d var9 = new Vec3d(this.mc.field_1724.method_23317(), this.mc.field_1724.method_23318(), this.mc.field_1724.method_23321());
               int var10 = (int)Math.ceil(var4);
               double var11 = var4 * var4;
               Mutable var13 = new Mutable();

               for (int var14 = var3.method_10263() - var10; var14 <= var3.method_10263() + var10; var14++) {
                  for (int var15 = var3.method_10264() - var10; var15 <= var3.method_10264() + var10; var15++) {
                     for (int var16 = var3.method_10260() - var10; var16 <= var3.method_10260() + var10; var16++) {
                        var13.method_10103(var14, var15, var16);
                        if (!(var13.method_46558().method_1025(var9) > var11)) {
                           this.addTarget(var13, var8, var7);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void addTarget(BlockPos var1, WorldSchematic var2, boolean var3) {
      if (this.insideActivePlacement(var1)) {
         if (this.isInRenderLayer(var1)) {
            BlockState var4 = var2.method_8320(var1);
            if (!var4.method_26215() && BlockStateUtil.isPrimaryPlacementState(var4)) {
               if (var3 || BlockStateUtil.isSimpleFullBlock(var4, var2, var1)) {
                  BlockState var5 = this.mc.field_1687.method_8320(var1);
                  TargetStatus var6 = BlockStateUtil.classify(var4, var5);
                  if (var6 != TargetStatus.CORRECT) {
                     BlockPos var7 = var1.method_10062();
                     double var8 = var7.method_46558().method_1025(this.mc.field_1724.method_33571());
                     this.cachedTargets.put(var7.method_10063(), new SchematicTarget(var7, SchematicBlockState.of(var4), var5, var6, var8));
                  }
               }
            }
         }
      }
   }

   private boolean rebuildActiveBoxes() {
      this.activeBoxes.clear();

      for (SchematicPlacement var3 : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
         if (var3.isEnabled()) {
            UnmodifiableIterator var4 = var3.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).values().iterator();

            while (var4.hasNext()) {
               Box var5 = (Box)var4.next();
               if (var5.getPos1() != null && var5.getPos2() != null) {
                  this.activeBoxes.add(SchematicManager.ActiveBox.of(var5.getPos1(), var5.getPos2()));
               }
            }
         }
      }

      return !this.activeBoxes.isEmpty();
   }

   private boolean insideActivePlacement(BlockPos var1) {
      for (SchematicManager.ActiveBox var3 : this.activeBoxes) {
         if (var3.contains(var1)) {
            return true;
         }
      }

      return false;
   }

   public static boolean inRenderLayer(BlockPos var0) {
      try {
         LayerRange var1 = DataManager.getRenderLayerRange();
         return var1 == null || var1.isPositionWithinRange(var0);
      } catch (Exception var2) {
         return true;
      }
   }

   private boolean isInRenderLayer(BlockPos var1) {
      try {
         LayerRange var2 = DataManager.getRenderLayerRange();
         return var2 == null || var2.isPositionWithinRange(var1);
      } catch (Exception var3) {
         return true;
      }
   }

   private int getLayerRangeHash() {
      try {
         LayerRange var1 = DataManager.getRenderLayerRange();
         return var1 == null ? 0 : Objects.hash(var1.getLayerMode(), var1.getLayerRangeMin(), var1.getLayerRangeMax(), var1.getLayerSingle(), var1.getAxis());
      } catch (Exception var2) {
         return 0;
      }
   }

   public SchematicTarget nearestMissingForNavigation(long var1, boolean var3) {
      if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
         if (var1 - this.lastNavRefreshTick < 20L) {
            return this.cachedNavTarget;
         } else {
            this.lastNavRefreshTick = var1;
            this.cachedNavTarget = null;

            try {
               if (!this.rebuildActiveBoxes()) {
                  return null;
               }

               WorldSchematic var4 = SchematicWorldHandler.getSchematicWorld();
               if (var4 == null) {
                  return null;
               }

               Vec3d var5 = this.mc.field_1724.method_33571();
               double var6 = 1.7976931348623157E308;
               Mutable var8 = new Mutable();
               int var9 = 0;
               int var10 = 0;
               int var11 = 0;

               for (SchematicManager.ActiveBox var13 : this.activeBoxes) {
                  for (int var14 = var13.minX(); var14 <= var13.maxX(); var14++) {
                     for (int var15 = var13.minY(); var15 <= var13.maxY(); var15++) {
                        for (int var16 = var13.minZ(); var16 <= var13.maxZ(); var16++) {
                           var9++;
                           var8.method_10103(var14, var15, var16);
                           BlockState var18 = var4.method_8320(var8);
                           if (!var18.method_26215() && BlockStateUtil.isPrimaryPlacementState(var18)) {
                              var10++;
                              BlockState var17;
                              if ((var3 || BlockStateUtil.isSimpleFullBlock(var18, var4, var8))
                                 && BlockStateUtil.classify(var18, var17 = this.mc.field_1687.method_8320(var8)) == TargetStatus.MISSING) {
                                 var11++;
                                 BlockPos var19 = var8.method_10062();
                                 double var20 = var19.method_46558().method_1025(var5);
                                 if (var20 < var6) {
                                    var6 = var20;
                                    this.cachedNavTarget = new SchematicTarget(var19, SchematicBlockState.of(var18), var17, TargetStatus.MISSING, var20);
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               EasyPlace.LOG
                  .debug(
                     "NavScan: scanned={} nonAir={} missing={} result={}",
                     new Object[]{var9, var10, var11, this.cachedNavTarget == null ? "null" : this.cachedNavTarget.pos().method_23854()}
                  );
            } catch (Exception var22) {
               EasyPlace.LOG.warn("NavScan error: {}: {}", var22.getClass().getSimpleName(), var22.getMessage());
            }

            return this.cachedNavTarget;
         }
      } else {
         return null;
      }
   }

   @Environment(EnvType.CLIENT)
   public record ActiveBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      private static SchematicManager.ActiveBox of(BlockPos var0, BlockPos var1) {
         return new SchematicManager.ActiveBox(
            Math.min(var0.method_10263(), var1.method_10263()),
            Math.min(var0.method_10264(), var1.method_10264()),
            Math.min(var0.method_10260(), var1.method_10260()),
            Math.max(var0.method_10263(), var1.method_10263()),
            Math.max(var0.method_10264(), var1.method_10264()),
            Math.max(var0.method_10260(), var1.method_10260())
         );
      }

      public boolean contains(BlockPos var1) {
         return var1.method_10263() >= this.minX
            && var1.method_10263() <= this.maxX
            && var1.method_10264() >= this.minY
            && var1.method_10264() <= this.maxY
            && var1.method_10260() >= this.minZ
            && var1.method_10260() <= this.maxZ;
      }
   }
}
