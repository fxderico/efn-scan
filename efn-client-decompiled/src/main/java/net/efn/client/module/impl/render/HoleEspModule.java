package net.efn.client.module.impl.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.PerformanceBudget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public class HoleEspModule extends ClientModule {
   private static final String MODE_ALL = "All";
   private static final String MODE_HOLES_TUNNELS = "Holes + Tunnels";
   private static final String MODE_HOLES_STAIRS = "Holes + Stairs";
   private static final String MODE_TUNNELS_STAIRS = "Tunnels + Stairs";
   private static final String MODE_HOLES = "Holes";
   private static final String MODE_TUNNELS = "Tunnels";
   private static final String MODE_STAIRS = "Stairs";
   private static final int TYPE_HOLE = 0;
   private static final int TYPE_HOLE_3X1 = 1;
   private static final int TYPE_TUNNEL = 2;
   private static final int TYPE_STAIR = 3;
   private static final int MAX_HITS_PER_CHUNK = 1024;
   private static final int MAX_RENDER_BOXES = 8192;
   private static final int HOLE_GRID_W = 20;
   private static final int ELYTRA_REFRESH_TICKS = 5;
   private static final int NORMAL_REFRESH_TICKS = 20;
   private static final float ELYTRA_SCAN_RADIUS = 12.0F;
   private static final float ELYTRA_SCAN_BUDGET_MS = 8.0F;
   private static final int[][] HORIZONTAL_DIRS = new int[][]{{1, 0}, {-1, 0}, {0, -1}, {0, 1}};
   private static final ThreadLocal<Mutable> PASSABLE_POS = ThreadLocal.withInitial(Mutable::new);
   private final ConcurrentHashMap<Long, List<HoleEspModule.Hit>> chunkCache = new ConcurrentHashMap<>();
   private final Queue<Long> scanQueue = new ConcurrentLinkedQueue<>();
   private final Set<Long> queued = ConcurrentHashMap.newKeySet();
   private long lastPlayerChunk = -9223372036854775808L;
   private int refreshTicks = 0;
   private ClientWorld sessionLevel;
   private boolean lastOptimized;
   private HoleEspModule.FastHoleScan activeScan;
   private boolean[] passableBuffer = new boolean[0];
   private final ModeSetting sMode = this.addSetting(
      "detection", new ModeSetting("Detection", "Holes", "Holes", "All", "Holes + Tunnels", "Holes + Stairs", "Tunnels + Stairs", "Tunnels", "Stairs")
   );
   private final NumberSetting sScanRadius = this.addSetting("scan-radius", new NumberSetting("Scan Radius", 8.0F, 1.0F, 24.0F).step(1.0F));
   private final NumberSetting sChunksPerTick = this.addSetting("chunks-tick", new NumberSetting("Chunks/Tick", 64.0F, 1.0F, 100.0F).step(1.0F));
   private final NumberSetting sScanBudgetMs = this.addSetting("scan-budget-ms", new NumberSetting("Scan Budget ms", 3.0F, 1.0F, 10.0F).step(1.0F));
   private final BooleanSetting sAirOnly = this.addSetting("only-air-blocks", new BooleanSetting("Only Air Blocks", true));
   private final NumberSetting sMinYOffset = this.addSetting("min-y-offset", new NumberSetting("Min Y Offset", 0.0F, 0.0F, 319.0F).step(1.0F));
   private final NumberSetting sMaxYOffset = this.addSetting("max-y-offset", new NumberSetting("Max Y Offset", 0.0F, 0.0F, 319.0F).step(1.0F));
   private final NumberSetting sMinHoleDepth = this.addSetting("min-hole-depth", new NumberSetting("Min Hole Depth", 4.0F, 1.0F, 20.0F).step(1.0F));
   private final NumberSetting sMinTunnelLength = this.addSetting("min-tunnel-length", new NumberSetting("Min Tunnel Length", 3.0F, 1.0F, 20.0F).step(1.0F));
   private final NumberSetting sMinTunnelHeight = this.addSetting("min-tunnel-height", new NumberSetting("Min Tunnel Height", 2.0F, 1.0F, 10.0F).step(1.0F));
   private final NumberSetting sMaxTunnelHeight = this.addSetting("max-tunnel-height", new NumberSetting("Max Tunnel Height", 3.0F, 2.0F, 10.0F).step(1.0F));
   private final NumberSetting sMinStairLength = this.addSetting("min-stair-length", new NumberSetting("Min Stair Length", 3.0F, 1.0F, 20.0F).step(1.0F));
   private final NumberSetting sMinStairHeight = this.addSetting("min-stair-height", new NumberSetting("Min Stair Height", 3.0F, 2.0F, 10.0F).step(1.0F));
   private final NumberSetting sMaxStairHeight = this.addSetting("max-stair-height", new NumberSetting("Max Stair Height", 5.0F, 2.0F, 10.0F).step(1.0F));
   private final BooleanSetting sFill = this.addSetting("fill", new BooleanSetting("Fill", true));
   private final BooleanSetting sOutline = this.addSetting("outline", new BooleanSetting("Outline", true));
   private final NumberSetting sLineWidth = this.addSetting("line-width", new NumberSetting("Line Width", 1.5F, 0.5F, 5.0F).step(0.5F));
   private final ColorSetting sHoleLine = this.addSetting("1x1-line", new ColorSetting("1x1 Line", 1291845631));
   private final ColorSetting sHoleFill = this.addSetting("1x1-fill", new ColorSetting("1x1 Fill", 1342177279));
   private final ColorSetting sHole3Line = this.addSetting("3x1-line", new ColorSetting("3x1 Line", 644245094));
   private final ColorSetting sHole3Fill = this.addSetting("3x1-fill", new ColorSetting("3x1 Fill", 644245094));
   private final ColorSetting sTunnelLine = this.addSetting("tunnel-line", new ColorSetting("Tunnel Line", 1593835775));
   private final ColorSetting sTunnelFill = this.addSetting("tunnel-fill", new ColorSetting("Tunnel Fill", 503316735));
   private final ColorSetting sStairLine = this.addSetting("stair-line", new ColorSetting("Stair Line", 1610547455));
   private final ColorSetting sStairFill = this.addSetting("stair-fill", new ColorSetting("Stair Fill", 520028415));

   public HoleEspModule() {
      super("hole-esp", "Hole ESP", "Highlights holes, tunnels, and staircases.", ModuleCategory.RENDER);
      this.sAirOnly.visibleWhen(() -> this.currentMode().tunnels() || this.currentMode().stairs());
      List.of(
            this.sMode,
            this.sScanRadius,
            this.sAirOnly,
            this.sMinYOffset,
            this.sMaxYOffset,
            this.sMinHoleDepth,
            this.sMinTunnelLength,
            this.sMinTunnelHeight,
            this.sMaxTunnelHeight,
            this.sMinStairLength,
            this.sMinStairHeight,
            this.sMaxStairHeight
         )
         .forEach(var1 -> var1.onChange(this::resetScan));
   }

   @Override
   protected void onEnable() {
      this.resetScan();
   }

   private void resetScan() {
      this.lastPlayerChunk = -9223372036854775808L;
      this.refreshTicks = 0;
      this.chunkCache.clear();
      this.scanQueue.clear();
      this.queued.clear();
      this.activeScan = null;
   }

   @Override
   protected void onDisable() {
      this.resetScan();
      this.sessionLevel = null;
      this.passableBuffer = new boolean[0];
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      boolean var4 = PerformanceModule.optimizationsEnabled();
      if (this.lastOptimized != var4 || this.sessionLevel != var3.field_1687) {
         this.resetScan();
      }

      this.lastOptimized = var4;
      this.sessionLevel = var3.field_1687;
      if (var3.field_1687 != null && var3.field_1724 != null) {
         int var5 = var3.field_1724.method_31477() >> 4;
         int var6 = var3.field_1724.method_31479() >> 4;
         long var7 = ChunkPos.method_8331(var5, var6);
         int var9 = this.scanRadius(var3);
         boolean var10 = isFastTravel(var3);
         boolean var2 = ++this.refreshTicks >= (var10 ? 5 : 20);
         if (var7 != this.lastPlayerChunk || var2) {
            this.refreshTicks = 0;
            this.lastPlayerChunk = var7;
            this.chunkCache.keySet().removeIf(var3x -> {
               int var4x = chunkX(var3x);
               int var5x = chunkZ(var3x);
               return Math.abs(var4x - var5) > var9 || Math.abs(var5x - var6) > var9;
            });
            ArrayList var12 = new ArrayList();
            Vec3d var13 = var3.field_1724.method_18798();
            double var14 = var13.field_1352;
            double var16 = var13.field_1350;
            double var18 = Math.sqrt(var14 * var14 + var16 * var16);
            if (var18 > 1.0E-4) {
               var14 /= var18;
               var16 /= var18;
            }

            for (int var20 = var5 - var9; var20 <= var5 + var9; var20++) {
               for (int var21 = var6 - var9; var21 <= var6 + var9; var21++) {
                  long var22 = ChunkPos.method_8331(var20, var21);
                  if (!this.chunkCache.containsKey(var22)
                     && !this.queued.contains(var22)
                     && (this.activeScan == null || this.activeScan.key != var22)
                     && var3.field_1687.method_2935().method_12123(var20, var21)) {
                     int var24 = var20 - var5;
                     int var25 = var21 - var6;
                     long var26 = (var24 * var24 + var25 * var25) * 1024L;
                     long var28 = var10 ? Math.round((var24 * var14 + var25 * var16) * 1536.0) : 0L;
                     var12.add(new long[]{var22, var26 - var28});
                  }
               }
            }

            var12.sort(Comparator.comparingLong(var0 -> var0[1]));

            for (long[] var40 : var12) {
               this.scanQueue.add(var40[0]);
               this.queued.add(var40[0]);
            }
         }

         int var33 = Math.max(1, Math.round(value(this.sChunksPerTick, 4.0F)));
         float var34 = var10 && !var4 ? Math.max(value(this.sScanBudgetMs, 3.0F), 8.0F) : value(this.sScanBudgetMs, 3.0F);
         long var35 = (long)(Math.max(1.0F, var34) * 1000000.0F);
         long var36 = var4 ? PerformanceBudget.acquire(var35) : System.nanoTime() + var35;
         long var37 = System.nanoTime();
         if (var37 < var36) {
            try {
               int var39 = 0;

               while (var39 < var33 && System.nanoTime() < var36) {
                  if (this.activeScan != null) {
                     if (!this.activeScan.validFor(var3.field_1687, var5, var6, var9)) {
                        int var42 = this.activeScan.chunk.method_12004().field_9181;
                        int var44 = this.activeScan.chunk.method_12004().field_9180;
                        long var45 = this.activeScan.key;
                        this.activeScan = null;
                        if (Math.abs((long)var42 - var5) <= var9
                           && Math.abs((long)var44 - var6) <= var9
                           && var3.field_1687.method_2935().method_12123(var42, var44)
                           && this.queued.add(var45)) {
                           this.scanQueue.add(var45);
                        }
                     } else if (this.activeScan.step()) {
                        this.storeHits(this.activeScan.key, this.activeScan.hits);
                        this.activeScan = null;
                        var39++;
                     }
                  } else {
                     Long var41 = this.scanQueue.poll();
                     if (var41 == null) {
                        break;
                     }

                     this.queued.remove(var41);
                     int var43 = chunkX(var41);
                     int var23 = chunkZ(var41);
                     if (var3.field_1687.method_2935().method_12123(var43, var23)) {
                        this.scanChunk(var3.field_1687, var43, var23, var41);
                     }

                     if (this.activeScan == null) {
                        var39++;
                     }
                  }
               }
            } finally {
               if (var4) {
                  PerformanceBudget.record(var37);
               }
            }
         }
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent var1) {
      if (var1.packet instanceof ChunkDataS2CPacket var3) {
         this.invalidateAround(var3.method_11523(), var3.method_11524());
      }
   }

   private void invalidateAround(int var1, int var2) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      if (var3.field_1724 != null) {
         int var4 = this.scanRadius(var3) + 1;
         if (Math.abs((long)var1 - (var3.field_1724.method_31477() >> 4)) <= var4 && Math.abs((long)var2 - (var3.field_1724.method_31479() >> 4)) <= var4) {
            for (int var5 = var1 - 1; var5 <= var1 + 1; var5++) {
               for (int var6 = var2 - 1; var6 <= var2 + 1; var6++) {
                  long var7 = ChunkPos.method_8331(var5, var6);
                  boolean var9 = this.chunkCache.containsKey(var7);
                  if (this.activeScan != null && this.activeScan.key == var7) {
                     this.activeScan = null;
                     var9 = true;
                  }

                  if (var9 && this.queued.add(var7)) {
                     this.scanQueue.add(var7);
                  }
               }
            }

            this.lastPlayerChunk = -9223372036854775808L;
         }
      }
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent var1) {
      if (var1.pos != null) {
         this.invalidateAround(var1.pos.method_10263() >> 4, var1.pos.method_10260() >> 4);
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      if (var3.field_1724 != null) {
         boolean var4 = this.sFill == null || this.sFill.value().get();
         boolean var2 = this.sOutline == null || this.sOutline.value().get();
         if (var4 || var2) {
            int var6 = var3.field_1724.method_31477() >> 4;
            int var7 = var3.field_1724.method_31479() >> 4;
            int var8 = this.scanRadius(var3);
            float var9 = value(this.sLineWidth, 1.5F);
            int var10 = 0;

            for (Entry var12 : this.chunkCache.entrySet()) {
               long var13 = (Long)var12.getKey();
               if (Math.abs(chunkX(var13) - var6) <= var8 && Math.abs(chunkZ(var13) - var7) <= var8) {
                  for (HoleEspModule.Hit var16 : (List)var12.getValue()) {
                     if (++var10 > 8192) {
                        return;
                     }

                     DrawStyle var17 = !var4
                        ? DrawStyle.method_75536(this.lineColor(var16.type()), var9)
                        : (
                           !var2
                              ? DrawStyle.method_75539(this.fillColor(var16.type()))
                              : DrawStyle.method_75537(this.lineColor(var16.type()), var9, this.fillColor(var16.type()))
                        );
                     GizmoDrawing.method_75541(var16.box(), var17).method_75533();
                  }
               }
            }
         }
      }
   }

   private void scanChunk(ClientWorld var1, int var2, int var3, long var4) {
      WorldChunk var7 = var1.method_2935().method_21730(var2, var3);
      if (var7 != null) {
         int var8 = var2 << 4;
         int var9 = var3 << 4;
         int var10 = var1.method_8597().comp_651();
         int var11 = var10 + var7.method_12006().length * 16;
         int var12 = Math.min(var11 - 1, var10 + Math.round(value(this.sMinYOffset, 0.0F)));
         int var13 = Math.max(var12 + 1, var11 - Math.round(value(this.sMaxYOffset, 0.0F)));
         HoleEspModule.Mode var14 = this.currentMode();
         boolean var6 = this.sAirOnly != null && this.sAirOnly.value().get();
         if (var14.holes() && !var14.tunnels() && !var14.stairs()) {
            if (PerformanceModule.optimizationsEnabled()) {
               this.activeScan = new HoleEspModule.FastHoleScan(this, var7, var4, var8, var9, var12, var13);
            } else {
               this.storeHits(var4, this.holeHits(var1, var8, var9, var12, var13));
            }
         } else {
            Object var16 = var14.holes() ? this.holeHits(var1, var8, var9, var12, var13) : new ArrayList();
            HashSet var17 = new HashSet();
            Mutable var18 = new Mutable();
            ChunkSection[] var19 = var7.method_12006();

            for (int var20 = 0; var20 < var19.length; var20++) {
               ChunkSection var21 = var19[var20];
               if (var21 != null && !var21.method_38292()) {
                  int var22 = var10 + var20 * 16;

                  for (int var23 = 0; var23 < 16; var23++) {
                     int var24 = var22 + var23;
                     if (var24 > var12 && var24 < var13) {
                        for (int var25 = 0; var25 < 16; var25++) {
                           for (int var26 = 0; var26 < 16; var26++) {
                              int var27 = var8 + var26;
                              int var28 = var9 + var25;
                              var18.method_10103(var27, var24, var28);
                              if (isPassable(var1, var18, var6)) {
                                 if (var14.tunnels()) {
                                    this.checkTunnelStarts(var1, var18, var6, (List<HoleEspModule.Hit>)var16, var17);
                                 }

                                 if (var14.stairs()) {
                                    this.checkStaircaseStarts(var1, var18, var6, (List<HoleEspModule.Hit>)var16, var17);
                                 }

                                 if (var16.size() >= 1024) {
                                    this.chunkCache.put(var4, List.copyOf((Collection<? extends HoleEspModule.Hit>)var16));
                                    return;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            this.storeHits(var4, (List<HoleEspModule.Hit>)var16);
         }
      }
   }

   private void storeHits(long var1, List<HoleEspModule.Hit> var3) {
      this.chunkCache.put(var1, List.copyOf(var3));
   }

   private List<HoleEspModule.Hit> holeHits(ClientWorld var1, int var2, int var3, int var4, int var5) {
      ArrayList var6 = new ArrayList();
      int var7 = var4 - 1;
      int var8 = var5 - var7 + 1;
      if (var8 <= 1) {
         return var6;
      } else {
         boolean[] var9 = new boolean[var8 * 20 * 20];
         Mutable var10 = new Mutable();

         for (int var11 = 0; var11 < var8; var11++) {
            int var12 = var7 + var11;
            int var13 = var11 * 20 * 20;

            for (int var14 = -1; var14 <= 18; var14++) {
               int var15 = var13 + (var14 + 1) * 20;

               for (int var16 = -1; var16 <= 18; var16++) {
                  var10.method_10103(var2 + var16, var12, var3 + var14);
                  var9[var15 + var16 + 1] = isOpen(var1, var10);
               }
            }
         }

         int var17 = Math.round(value(this.sMinHoleDepth, 4.0F));
         HashSet var18 = new HashSet();

         for (int var19 = 0; var19 < 256 && var6.size() < 1024; var19++) {
            shaftsInColumn(var9, var19 & 15, var19 >> 4, var7, var4, var5, var17, var2, var3, var6, var18);
         }

         return var6;
      }
   }

   private static void shaftsInColumn(
      boolean[] var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, List<HoleEspModule.Hit> var9, Set<String> var10
   ) {
      int var11 = var7 + var1;
      int var12 = var8 + var2;
      int var13 = 0;
      int var14 = 0;
      int var15 = 0;

      for (int var16 = var4; var16 <= var5; var16++) {
         boolean var17 = var16 < var5;
         var13 = var17 && walled1x1(var0, var16, var1, var2, var3) ? ++var13 : endShaft(var13, var16, var6, var9, var10, var11, var12, 1, 1, 0);
         var14 = var17 && walled3x1X(var0, var16, var1, var2, var3) ? ++var14 : endShaft(var14, var16, var6, var9, var10, var11, var12, 3, 1, 1);
         if (var17 && walled3x1Z(var0, var16, var1, var2, var3)) {
            var15++;
         } else {
            var15 = endShaft(var15, var16, var6, var9, var10, var11, var12, 1, 3, 1);
         }
      }
   }

   private static int endShaft(int var0, int var1, int var2, List<HoleEspModule.Hit> var3, Set<String> var4, int var5, int var6, int var7, int var8, int var9) {
      if (var0 >= var2) {
         addHit(var3, var4, new Box(var5, var1 - var0, var6, var5 + var7, var1, var6 + var8), var9);
      }

      return 0;
   }

   private static boolean walled1x1(boolean[] var0, int var1, int var2, int var3, int var4) {
      return grid(var0, var1, var2, var3, var4)
         && !grid(var0, var1, var2 - 1, var3, var4)
         && !grid(var0, var1, var2 + 1, var3, var4)
         && !grid(var0, var1, var2, var3 - 1, var4)
         && !grid(var0, var1, var2, var3 + 1, var4);
   }

   private static boolean walled3x1X(boolean[] var0, int var1, int var2, int var3, int var4) {
      return grid(var0, var1, var2, var3, var4)
         && grid(var0, var1, var2 + 1, var3, var4)
         && grid(var0, var1, var2 + 2, var3, var4)
         && !grid(var0, var1, var2 - 1, var3, var4)
         && !grid(var0, var1, var2 + 3, var3, var4)
         && !grid(var0, var1, var2, var3 - 1, var4)
         && !grid(var0, var1, var2, var3 + 1, var4)
         && !grid(var0, var1, var2 + 1, var3 - 1, var4)
         && !grid(var0, var1, var2 + 1, var3 + 1, var4)
         && !grid(var0, var1, var2 + 2, var3 - 1, var4)
         && !grid(var0, var1, var2 + 2, var3 + 1, var4);
   }

   private static boolean walled3x1Z(boolean[] var0, int var1, int var2, int var3, int var4) {
      return grid(var0, var1, var2, var3, var4)
         && grid(var0, var1, var2, var3 + 1, var4)
         && grid(var0, var1, var2, var3 + 2, var4)
         && !grid(var0, var1, var2, var3 - 1, var4)
         && !grid(var0, var1, var2, var3 + 3, var4)
         && !grid(var0, var1, var2 - 1, var3, var4)
         && !grid(var0, var1, var2 + 1, var3, var4)
         && !grid(var0, var1, var2 - 1, var3 + 1, var4)
         && !grid(var0, var1, var2 + 1, var3 + 1, var4)
         && !grid(var0, var1, var2 - 1, var3 + 2, var4)
         && !grid(var0, var1, var2 + 1, var3 + 2, var4);
   }

   private static boolean grid(boolean[] var0, int var1, int var2, int var3, int var4) {
      int var5 = var1 - var4;
      return var0[var5 * 20 * 20 + (var3 + 1) * 20 + var2 + 1];
   }

   private void checkTunnelStarts(ClientWorld var1, BlockPos var2, boolean var3, List<HoleEspModule.Hit> var4, Set<String> var5) {
      int var6 = Math.round(value(this.sMinTunnelLength, 3.0F));

      for (int[] var10 : HORIZONTAL_DIRS) {
         Mutable var11 = new Mutable(var2.method_10263() - var10[0], var2.method_10264(), var2.method_10260() - var10[1]);
         if (!this.isTunnelSection(var1, var11, var10, var3)) {
            Mutable var12 = new Mutable(var2.method_10263(), var2.method_10264(), var2.method_10260());
            BlockPos var13 = null;
            BlockPos var14 = null;
            int var15 = 0;
            int var16 = 0;
            if (this.isTunnelSection(var1, var12, var10, var3)) {
               var13 = var12.method_10062();
            }

            while (this.isTunnelSection(var1, var12, var10, var3)) {
               var16 = Math.max(var16, this.getTunnelHeight(var1, var12, var3));
               var14 = var12.method_10062();
               var12.method_10100(var10[0], 0, var10[1]);
               var15++;
            }

            if (var13 != null && var14 != null && var15 >= var6) {
               addHit(
                  var4,
                  var5,
                  new Box(
                     Math.min(var13.method_10263(), var14.method_10263()),
                     var13.method_10264(),
                     Math.min(var13.method_10260(), var14.method_10260()),
                     Math.max(var13.method_10263(), var14.method_10263()) + 1,
                     var13.method_10264() + var16,
                     Math.max(var13.method_10260(), var14.method_10260()) + 1
                  ),
                  2
               );
            }
         }
      }
   }

   private void checkStaircaseStarts(ClientWorld var1, BlockPos var2, boolean var3, List<HoleEspModule.Hit> var4, Set<String> var5) {
      int var6 = Math.round(value(this.sMinStairLength, 3.0F));

      for (int[] var10 : HORIZONTAL_DIRS) {
         Mutable var11 = new Mutable(var2.method_10263() - var10[0], var2.method_10264() - 1, var2.method_10260() - var10[1]);
         if (!this.isStaircaseSection(var1, var11, var10, var3)) {
            Mutable var12 = new Mutable(var2.method_10263(), var2.method_10264(), var2.method_10260());
            int var13 = 0;

            ArrayList var14;
            for (var14 = new ArrayList(); this.isStaircaseSection(var1, var12, var10, var3); var13++) {
               int var15 = this.getStaircaseHeight(var1, var12, var3);
               var14.add(
                  new Box(
                     var12.method_10263(),
                     var12.method_10264(),
                     var12.method_10260(),
                     var12.method_10263() + 1,
                     var12.method_10264() + var15,
                     var12.method_10260() + 1
                  )
               );
               var12.method_10100(var10[0], 1, var10[1]);
            }

            if (var13 >= var6) {
               for (Box var16 : var14) {
                  addHit(var4, var5, var16, 3);
               }
            }
         }
      }
   }

   private boolean isTunnelSection(ClientWorld var1, BlockPos var2, int[] var3, boolean var4) {
      int var5 = this.getTunnelHeight(var1, var2, var4);
      if (var5 >= Math.round(value(this.sMinTunnelHeight, 2.0F)) && var5 <= Math.round(value(this.sMaxTunnelHeight, 3.0F))) {
         if (!isPassable(var1, var2.method_10263(), var2.method_10264() - 1, var2.method_10260(), var4)
            && !isPassable(var1, var2.method_10263(), var2.method_10264() + var5, var2.method_10260(), var4)) {
            boolean var6 = var3[0] != 0;

            for (int var7 = 0; var7 < var5; var7++) {
               int var8 = var2.method_10264() + var7;
               if (var6
                  ? isPassable(var1, var2.method_10263(), var8, var2.method_10260() - 1, var4)
                     || isPassable(var1, var2.method_10263(), var8, var2.method_10260() + 1, var4)
                  : isPassable(var1, var2.method_10263() - 1, var8, var2.method_10260(), var4)
                     || isPassable(var1, var2.method_10263() + 1, var8, var2.method_10260(), var4)) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isStaircaseSection(ClientWorld var1, BlockPos var2, int[] var3, boolean var4) {
      int var5 = this.getStaircaseHeight(var1, var2, var4);
      if (var5 >= Math.round(value(this.sMinStairHeight, 3.0F)) && var5 <= Math.round(value(this.sMaxStairHeight, 5.0F))) {
         if (!isPassable(var1, var2.method_10263(), var2.method_10264() - 1, var2.method_10260(), var4)
            && !isPassable(var1, var2.method_10263(), var2.method_10264() + var5, var2.method_10260(), var4)) {
            boolean var6 = var3[0] != 0;

            for (int var7 = 0; var7 < var5; var7++) {
               int var8 = var2.method_10264() + var7;
               if (var6
                  ? isPassable(var1, var2.method_10263(), var8, var2.method_10260() - 1, var4)
                     || isPassable(var1, var2.method_10263(), var8, var2.method_10260() + 1, var4)
                  : isPassable(var1, var2.method_10263() - 1, var8, var2.method_10260(), var4)
                     || isPassable(var1, var2.method_10263() + 1, var8, var2.method_10260(), var4)) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int getTunnelHeight(ClientWorld var1, BlockPos var2, boolean var3) {
      int var5 = Math.round(value(this.sMaxTunnelHeight, 3.0F));
      int var4 = 0;

      while (var4 < var5 && isPassable(var1, var2.method_10263(), var2.method_10264() + var4, var2.method_10260(), var3)) {
         var4++;
      }

      return var4;
   }

   private int getStaircaseHeight(ClientWorld var1, BlockPos var2, boolean var3) {
      int var5 = Math.round(value(this.sMaxStairHeight, 5.0F));
      int var4 = 0;

      while (var4 < var5 && isPassable(var1, var2.method_10263(), var2.method_10264() + var4, var2.method_10260(), var3)) {
         var4++;
      }

      return var4;
   }

   private static boolean isOpen(ClientWorld var0, BlockPos var1) {
      BlockState var2 = var0.method_8320(var1);
      return var2.method_26215() || var2.method_26220(var0, var1).method_1110();
   }

   private static boolean isPassable(ClientWorld var0, int var1, int var2, int var3, boolean var4) {
      return isPassable(var0, PASSABLE_POS.get().method_10103(var1, var2, var3), var4);
   }

   private static boolean isPassable(ClientWorld var0, BlockPos var1, boolean var2) {
      BlockState var3 = var0.method_8320(var1);
      if (var3.method_26215()) {
         return true;
      } else if (var2) {
         return false;
      } else {
         VoxelShape var4 = var3.method_26220(var0, var1);
         return var4.method_1110() || !VoxelShapes.method_1077().equals(var4);
      }
   }

   private static void addHit(List<HoleEspModule.Hit> var0, Set<String> var1, Box var2, int var3) {
      String var4 = var3
         + ":"
         + var2.field_1323
         + ","
         + var2.field_1322
         + ","
         + var2.field_1321
         + ","
         + var2.field_1320
         + ","
         + var2.field_1325
         + ","
         + var2.field_1324;
      if (var1.add(var4)) {
         for (HoleEspModule.Hit var6 : var0) {
            if (var6.type() == var3 && var6.box().method_994(var2)) {
               return;
            }
         }

         var0.add(new HoleEspModule.Hit(var2, var3));
      }
   }

   private HoleEspModule.Mode currentMode() {
      String var2 = this.sMode == null ? "All" : this.sMode.value();

      return switch (var2) {
         case "Holes + Tunnels" -> new HoleEspModule.Mode(true, true, false);
         case "Holes + Stairs" -> new HoleEspModule.Mode(true, false, true);
         case "Tunnels + Stairs" -> new HoleEspModule.Mode(false, true, true);
         case "Holes" -> new HoleEspModule.Mode(true, false, false);
         case "Tunnels" -> new HoleEspModule.Mode(false, true, false);
         case "Stairs" -> new HoleEspModule.Mode(false, false, true);
         default -> new HoleEspModule.Mode(true, true, true);
      };
   }

   private int lineColor(int var1) {
      return switch (var1) {
         case 1 -> color(this.sHole3Line, 1610589440);
         case 2 -> color(this.sTunnelLine, 1593835775);
         case 3 -> color(this.sStairLine, 1610547455);
         default -> color(this.sHoleLine, 1610547200);
      };
   }

   private int fillColor(int var1) {
      return switch (var1) {
         case 1 -> color(this.sHole3Fill, 520070400);
         case 2 -> color(this.sTunnelFill, 503316735);
         case 3 -> color(this.sStairFill, 520028415);
         default -> color(this.sHoleFill, 520028160);
      };
   }

   private static int chunkX(long var0) {
      return (int)var0;
   }

   private static int chunkZ(long var0) {
      return (int)(var0 >> 32);
   }

   private int scanRadius(MinecraftClient var1) {
      int var2 = (Integer)var1.field_1690.method_42503().method_41753();
      float var3 = value(this.sScanRadius, 4.0F);
      if (isFastTravel(var1)) {
         var3 = Math.max(var3, 12.0F);
      }

      int var4 = Math.max(1, Math.round(var3));
      return Math.min(var2, var4);
   }

   private static boolean isFastTravel(MinecraftClient var0) {
      if (var0.field_1724 == null) {
         return false;
      } else {
         Vec3d var1 = var0.field_1724.method_18798();
         double var2 = var1.field_1352 * var1.field_1352 + var1.field_1350 * var1.field_1350;
         return var0.field_1724.method_6128() || var2 > 0.18;
      }
   }

   private static float value(NumberSetting var0, float var1) {
      return var0 == null ? var1 : var0.value()[0];
   }

   private static int color(ColorSetting var0, int var1) {
      return var0 == null ? var1 : var0.argb();
   }

   @Environment(EnvType.CLIENT)
   private final class FastHoleScan {
      final WorldChunk chunk;
      final ClientWorld level;
      final WorldChunk[] neighbors = new WorldChunk[9];
      final long key;
      final int baseX;
      final int baseZ;
      final int minY;
      final int maxY;
      final int gridMinY;
      final int yCount;
      final int minDepth;
      final boolean[] passable;
      final List<HoleEspModule.Hit> hits = new ArrayList<>();
      final Set<String> seen = new HashSet<>();
      final Mutable pos = new Mutable();
      int row;
      int column;

      FastHoleScan(HoleEspModule nullx, WorldChunk nullxx, long nullxxx, int nullxxxx, int nullxxxxx, int nullxxxxxx, int nullxxxxxxx) {
         this.chunk = nullxx;
         this.level = nullx.sessionLevel;
         this.key = nullxxx;
         this.baseX = nullxxxx;
         this.baseZ = nullxxxxx;
         this.minY = nullxxxxxx;
         this.maxY = nullxxxxxxx;
         this.gridMinY = nullxxxxxx - 1;
         this.yCount = nullxxxxxxx - this.gridMinY + 1;
         this.minDepth = Math.round(HoleEspModule.value(nullx.sMinHoleDepth, 4.0F));
         int var10 = Math.max(0, this.yCount) * 20 * 20;
         if (nullx.passableBuffer.length < var10) {
            nullx.passableBuffer = new boolean[var10];
         }

         this.passable = nullx.passableBuffer;

         for (int var11 = -1; var11 <= 1; var11++) {
            for (int var12 = -1; var12 <= 1; var12++) {
               this.neighbors[(var11 + 1) * 3 + var12 + 1] = this.level.method_2935().method_21730((nullxxxx >> 4) + var11, (nullxxxxx >> 4) + var12);
            }
         }
      }

      boolean validFor(ClientWorld var1, int var2, int var3, int var4) {
         if (var1 == this.level
            && Math.abs((long)this.chunk.method_12004().field_9181 - var2) <= var4
            && Math.abs((long)this.chunk.method_12004().field_9180 - var3) <= var4) {
            for (int var5 = -1; var5 <= 1; var5++) {
               for (int var6 = -1; var6 <= 1; var6++) {
                  if (this.neighbors[(var5 + 1) * 3 + var6 + 1] != this.level.method_2935().method_21730((this.baseX >> 4) + var5, (this.baseZ >> 4) + var6)) {
                     return false;
                  }
               }
            }

            return true;
         } else {
            return false;
         }
      }

      boolean step() {
         if (this.yCount <= 1) {
            return true;
         } else if (this.row < this.yCount) {
            int var6 = this.gridMinY + this.row;
            int var7 = this.row++ * 20 * 20;

            for (int var3 = -1; var3 <= 18; var3++) {
               int var4 = var7 + (var3 + 1) * 20;

               for (int var5 = -1; var5 <= 18; var5++) {
                  this.pos.method_10103(this.baseX + var5, var6, this.baseZ + var3);
                  this.passable[var4 + var5 + 1] = HoleEspModule.isOpen(this.level, (BlockPos)this.pos);
               }
            }

            return false;
         } else if (this.column >= 256) {
            return true;
         } else {
            int var1 = this.column & 15;
            int var2 = this.column++ >> 4;
            HoleEspModule.shaftsInColumn(
               this.passable, var1, var2, this.gridMinY, this.minY, this.maxY, this.minDepth, this.baseX, this.baseZ, this.hits, this.seen
            );
            return this.column >= 256 || this.hits.size() >= 1024;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record Hit(Box box, int type) {
   }

   @Environment(EnvType.CLIENT)
   private record Mode(boolean holes, boolean tunnels, boolean stairs) {
   }
}
