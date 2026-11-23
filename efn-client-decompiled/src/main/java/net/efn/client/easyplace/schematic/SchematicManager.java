package net.efn.client.easyplace.schematic;

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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;

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

   public SchematicManager(MinecraftClient minecraftClient) {
      this.mc = minecraftClient;
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

   public Collection<SchematicTarget> getTargets(long l, double d, boolean bl, boolean bl2) {
      if (this.mc.player != null && this.mc.world != null) {
         BlockPos var8 = this.mc.player.getBlockPos();
         boolean var9 = this.lastCenter == null || var8.getSquaredDistance(this.lastCenter) > 1.0;
         int var10 = this.getLayerRangeHash();
         boolean var7 = d != this.lastRange || bl != this.lastAura || bl2 != this.lastAllBlocks || var10 != this.lastLayerRangeHash;
         if (l - this.lastRefreshTick >= 5L || var9 || var7) {
            this.refresh(l, var8, d, bl, bl2);
         }

         return this.cachedTargets.values();
      } else {
         this.clear();
         return List.of();
      }
   }

   private void refresh(long l, BlockPos blockPos, double d, boolean bl, boolean bl2) {
      this.cachedTargets.clear();
      this.lastRefreshTick = l;
      this.lastCenter = blockPos.toImmutable();
      this.lastRange = d;
      this.lastAura = bl;
      this.lastAllBlocks = bl2;
      this.lastLayerRangeHash = this.getLayerRangeHash();
      if (this.rebuildActiveBoxes()) {
         WorldSchematic var8 = SchematicWorldHandler.getSchematicWorld();
         if (var8 != null) {
            if (!bl) {
               BlockPos var17 = RayTraceUtils.getSchematicWorldTraceIfClosestNoFluids(this.mc.world, this.mc.player, d);
               if (var17 != null) {
                  this.addTarget(var17, var8, bl2);
               }
            } else {
               Vec3d var9 = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
               int var10 = (int)Math.ceil(d);
               double var11 = d * d;
               Mutable var13 = new Mutable();

               for (int var14 = blockPos.getX() - var10; var14 <= blockPos.getX() + var10; var14++) {
                  for (int var15 = blockPos.getY() - var10; var15 <= blockPos.getY() + var10; var15++) {
                     for (int var16 = blockPos.getZ() - var10; var16 <= blockPos.getZ() + var10; var16++) {
                        var13.set(var14, var15, var16);
                        if (!(var13.toCenterPos().squaredDistanceTo(var9) > var11)) {
                           this.addTarget(var13, var8, bl2);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void addTarget(BlockPos blockPos, WorldSchematic worldSchematic, boolean bl) {
      if (this.insideActivePlacement(blockPos)) {
         if (this.isInRenderLayer(blockPos)) {
            BlockState var4 = worldSchematic.getBlockState(blockPos);
            if (!var4.isAir() && BlockStateUtil.isPrimaryPlacementState(var4)) {
               if (bl || BlockStateUtil.isSimpleFullBlock(var4, worldSchematic, blockPos)) {
                  BlockState var5 = this.mc.world.getBlockState(blockPos);
                  TargetStatus var6 = BlockStateUtil.classify(var4, var5);
                  if (var6 != TargetStatus.CORRECT) {
                     BlockPos var7 = blockPos.toImmutable();
                     double var8 = var7.toCenterPos().squaredDistanceTo(this.mc.player.getEyePos());
                     this.cachedTargets.put(var7.asLong(), new SchematicTarget(var7, SchematicBlockState.of(var4), var5, var6, var8));
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
            for (Box var5 : var3.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).values()) {
               if (var5.getPos1() != null && var5.getPos2() != null) {
                  this.activeBoxes.add(SchematicManager.ActiveBox.of(var5.getPos1(), var5.getPos2()));
               }
            }
         }
      }

      return !this.activeBoxes.isEmpty();
   }

   private boolean insideActivePlacement(BlockPos blockPos) {
      for (SchematicManager.ActiveBox var3 : this.activeBoxes) {
         if (var3.contains(blockPos)) {
            return true;
         }
      }

      return false;
   }

   public static boolean inRenderLayer(BlockPos blockPos) {
      try {
         LayerRange var1 = DataManager.getRenderLayerRange();
         return var1 == null || var1.isPositionWithinRange(blockPos);
      } catch (Exception var2) {
         return true;
      }
   }

   private boolean isInRenderLayer(BlockPos blockPos) {
      try {
         LayerRange var2 = DataManager.getRenderLayerRange();
         return var2 == null || var2.isPositionWithinRange(blockPos);
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

   public SchematicTarget nearestMissingForNavigation(long l, boolean bl) {
      if (this.mc.player != null && this.mc.world != null) {
         if (l - this.lastNavRefreshTick < 20L) {
            return this.cachedNavTarget;
         } else {
            this.lastNavRefreshTick = l;
            this.cachedNavTarget = null;

            try {
               if (!this.rebuildActiveBoxes()) {
                  return null;
               }

               WorldSchematic var4 = SchematicWorldHandler.getSchematicWorld();
               if (var4 == null) {
                  return null;
               }

               Vec3d var5 = this.mc.player.getEyePos();
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
                           var8.set(var14, var15, var16);
                           BlockState var18 = var4.getBlockState(var8);
                           if (!var18.isAir() && BlockStateUtil.isPrimaryPlacementState(var18)) {
                              var10++;
                              BlockState var17;
                              if ((bl || BlockStateUtil.isSimpleFullBlock(var18, var4, var8))
                                 && BlockStateUtil.classify(var18, var17 = this.mc.world.getBlockState(var8)) == TargetStatus.MISSING) {
                                 var11++;
                                 BlockPos var19 = var8.toImmutable();
                                 double var20 = var19.toCenterPos().squaredDistanceTo(var5);
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
                     var9,
                     var10,
                     var11,
                     this.cachedNavTarget == null ? "null" : this.cachedNavTarget.pos().toShortString()
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
      private static SchematicManager.ActiveBox of(BlockPos blockPos, BlockPos blockPos2) {
         return new SchematicManager.ActiveBox(
            Math.min(blockPos.getX(), blockPos2.getX()),
            Math.min(blockPos.getY(), blockPos2.getY()),
            Math.min(blockPos.getZ(), blockPos2.getZ()),
            Math.max(blockPos.getX(), blockPos2.getX()),
            Math.max(blockPos.getY(), blockPos2.getY()),
            Math.max(blockPos.getZ(), blockPos2.getZ())
         );
      }

      public boolean contains(BlockPos blockPos) {
         return blockPos.getX() >= this.minX
            && blockPos.getX() <= this.maxX
            && blockPos.getY() >= this.minY
            && blockPos.getY() <= this.maxY
            && blockPos.getZ() >= this.minZ
            && blockPos.getZ() <= this.maxZ;
      }
   }
}
