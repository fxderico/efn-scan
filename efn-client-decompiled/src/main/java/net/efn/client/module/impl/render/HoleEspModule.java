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
         .forEach(setting -> setting.onChange(this::resetScan));
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
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      boolean var4 = PerformanceModule.optimizationsEnabled();
      if (this.lastOptimized != var4 || this.sessionLevel != var3.world) {
         this.resetScan();
      }

      this.lastOptimized = var4;
      this.sessionLevel = var3.world;
      if (var3.world != null && var3.player != null) {
         int var5 = var3.player.getBlockX() >> 4;
         int var6 = var3.player.getBlockZ() >> 4;
         long var7 = ChunkPos.toLong(var5, var6);
         int var9 = this.scanRadius(var3);
         boolean var10 = isFastTravel(var3);
         boolean var2 = ++this.refreshTicks >= (var10 ? 5 : 20);
         if (var7 != this.lastPlayerChunk || var2) {
            this.refreshTicks = 0;
            this.lastPlayerChunk = var7;
            this.chunkCache.keySet().removeIf(long_ -> {
               int var4x = chunkX(long_);
               int var5x = chunkZ(long_);
               return Math.abs(var4x - var5) > var9 || Math.abs(var5x - var6) > var9;
            });
            ArrayList var12 = new ArrayList();
            Vec3d var13 = var3.player.getVelocity();
            double var14 = var13.x;
            double var16 = var13.z;
            double var18 = Math.sqrt(var14 * var14 + var16 * var16);
            if (var18 > 1.0E-4) {
               var14 /= var18;
               var16 /= var18;
            }

            for (int var20 = var5 - var9; var20 <= var5 + var9; var20++) {
               for (int var21 = var6 - var9; var21 <= var6 + var9; var21++) {
                  long var22 = ChunkPos.toLong(var20, var21);
                  if (!this.chunkCache.containsKey(var22)
                     && !this.queued.contains(var22)
                     && (this.activeScan == null || this.activeScan.key != var22)
                     && var3.world.getChunkManager().isChunkLoaded(var20, var21)) {
                     int var24 = var20 - var5;
                     int var25 = var21 - var6;
                     long var26 = (var24 * var24 + var25 * var25) * 1024L;
                     long var28 = var10 ? Math.round((var24 * var14 + var25 * var16) * 1536.0) : 0L;
                     var12.add(new long[]{var22, var26 - var28});
                  }
               }
            }

            var12.sort(Comparator.comparingLong(ls -> ls[1]));

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
                     if (!this.activeScan.validFor(var3.world, var5, var6, var9)) {
                        int var42 = this.activeScan.chunk.getPos().x;
                        int var44 = this.activeScan.chunk.getPos().z;
                        long var45 = this.activeScan.key;
                        this.activeScan = null;
                        if (Math.abs((long)var42 - var5) <= var9
                           && Math.abs((long)var44 - var6) <= var9
                           && var3.world.getChunkManager().isChunkLoaded(var42, var44)
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
                     if (var3.world.getChunkManager().isChunkLoaded(var43, var23)) {
                        this.scanChunk(var3.world, var43, var23, var41);
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
   public void onPacketApplied(PacketAppliedEvent packetAppliedEvent) {
      if (packetAppliedEvent.packet instanceof ChunkDataS2CPacket var3) {
         this.invalidateAround(var3.getChunkX(), var3.getChunkZ());
      }
   }

   private void invalidateAround(int i, int j) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3.player != null) {
         int var4 = this.scanRadius(var3) + 1;
         if (Math.abs((long)i - (var3.player.getBlockX() >> 4)) <= var4 && Math.abs((long)j - (var3.player.getBlockZ() >> 4)) <= var4) {
            for (int var5 = i - 1; var5 <= i + 1; var5++) {
               for (int var6 = j - 1; var6 <= j + 1; var6++) {
                  long var7 = ChunkPos.toLong(var5, var6);
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
   public void onBlockUpdate(BlockUpdateEvent blockUpdateEvent) {
      if (blockUpdateEvent.pos != null) {
         this.invalidateAround(blockUpdateEvent.pos.getX() >> 4, blockUpdateEvent.pos.getZ() >> 4);
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3.player != null) {
         boolean var4 = this.sFill == null || this.sFill.value().get();
         boolean var2 = this.sOutline == null || this.sOutline.value().get();
         if (var4 || var2) {
            int var6 = var3.player.getBlockX() >> 4;
            int var7 = var3.player.getBlockZ() >> 4;
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
                        ? DrawStyle.stroked(this.lineColor(var16.type()), var9)
                        : (
                           !var2
                              ? DrawStyle.filled(this.fillColor(var16.type()))
                              : DrawStyle.filledAndStroked(this.lineColor(var16.type()), var9, this.fillColor(var16.type()))
                        );
                     GizmoDrawing.box(var16.box(), var17).ignoreOcclusion();
                  }
               }
            }
         }
      }
   }

   private void scanChunk(ClientWorld clientWorld, int i, int j, long l) {
      WorldChunk var7 = clientWorld.getChunkManager().getWorldChunk(i, j);
      if (var7 != null) {
         int var8 = i << 4;
         int var9 = j << 4;
         int var10 = clientWorld.getDimension().minY();
         int var11 = var10 + var7.getSectionArray().length * 16;
         int var12 = Math.min(var11 - 1, var10 + Math.round(value(this.sMinYOffset, 0.0F)));
         int var13 = Math.max(var12 + 1, var11 - Math.round(value(this.sMaxYOffset, 0.0F)));
         HoleEspModule.Mode var14 = this.currentMode();
         boolean var6 = this.sAirOnly != null && this.sAirOnly.value().get();
         if (var14.holes() && !var14.tunnels() && !var14.stairs()) {
            if (PerformanceModule.optimizationsEnabled()) {
               this.activeScan = new HoleEspModule.FastHoleScan(this, var7, l, var8, var9, var12, var13);
            } else {
               this.storeHits(l, this.holeHits(clientWorld, var8, var9, var12, var13));
            }
         } else {
            Object var16 = var14.holes() ? this.holeHits(clientWorld, var8, var9, var12, var13) : new ArrayList();
            HashSet var17 = new HashSet();
            Mutable var18 = new Mutable();
            ChunkSection[] var19 = var7.getSectionArray();

            for (int var20 = 0; var20 < var19.length; var20++) {
               ChunkSection var21 = var19[var20];
               if (var21 != null && !var21.isEmpty()) {
                  int var22 = var10 + var20 * 16;

                  for (int var23 = 0; var23 < 16; var23++) {
                     int var24 = var22 + var23;
                     if (var24 > var12 && var24 < var13) {
                        for (int var25 = 0; var25 < 16; var25++) {
                           for (int var26 = 0; var26 < 16; var26++) {
                              int var27 = var8 + var26;
                              int var28 = var9 + var25;
                              var18.set(var27, var24, var28);
                              if (isPassable(clientWorld, var18, var6)) {
                                 if (var14.tunnels()) {
                                    this.checkTunnelStarts(clientWorld, var18, var6, (List<HoleEspModule.Hit>)var16, var17);
                                 }

                                 if (var14.stairs()) {
                                    this.checkStaircaseStarts(clientWorld, var18, var6, (List<HoleEspModule.Hit>)var16, var17);
                                 }

                                 if (var16.size() >= 1024) {
                                    this.chunkCache.put(l, List.copyOf((Collection<? extends HoleEspModule.Hit>)var16));
                                    return;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            this.storeHits(l, (List<HoleEspModule.Hit>)var16);
         }
      }
   }

   private void storeHits(long l, List<HoleEspModule.Hit> list) {
      this.chunkCache.put(l, List.copyOf(list));
   }

   private List<HoleEspModule.Hit> holeHits(ClientWorld clientWorld, int i, int j, int k, int l) {
      ArrayList var6 = new ArrayList();
      int var7 = k - 1;
      int var8 = l - var7 + 1;
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
                  var10.set(i + var16, var12, j + var14);
                  var9[var15 + var16 + 1] = isOpen(clientWorld, var10);
               }
            }
         }

         int var17 = Math.round(value(this.sMinHoleDepth, 4.0F));
         HashSet var18 = new HashSet();

         for (int var19 = 0; var19 < 256 && var6.size() < 1024; var19++) {
            shaftsInColumn(var9, var19 & 15, var19 >> 4, var7, k, l, var17, i, j, var6, var18);
         }

         return var6;
      }
   }

   private static void shaftsInColumn(boolean[] bls, int i, int j, int k, int l, int m, int n, int o, int p, List<HoleEspModule.Hit> list, Set<String> set) {
      int var11 = o + i;
      int var12 = p + j;
      int var13 = 0;
      int var14 = 0;
      int var15 = 0;

      for (int var16 = l; var16 <= m; var16++) {
         boolean var17 = var16 < m;
         var13 = var17 && walled1x1(bls, var16, i, j, k) ? ++var13 : endShaft(var13, var16, n, list, set, var11, var12, 1, 1, 0);
         var14 = var17 && walled3x1X(bls, var16, i, j, k) ? ++var14 : endShaft(var14, var16, n, list, set, var11, var12, 3, 1, 1);
         if (var17 && walled3x1Z(bls, var16, i, j, k)) {
            var15++;
         } else {
            var15 = endShaft(var15, var16, n, list, set, var11, var12, 1, 3, 1);
         }
      }
   }

   private static int endShaft(int i, int j, int k, List<HoleEspModule.Hit> list, Set<String> set, int l, int m, int n, int o, int p) {
      if (i >= k) {
         addHit(list, set, new Box(l, j - i, m, l + n, j, m + o), p);
      }

      return 0;
   }

   private static boolean walled1x1(boolean[] bls, int i, int j, int k, int l) {
      return grid(bls, i, j, k, l) && !grid(bls, i, j - 1, k, l) && !grid(bls, i, j + 1, k, l) && !grid(bls, i, j, k - 1, l) && !grid(bls, i, j, k + 1, l);
   }

   private static boolean walled3x1X(boolean[] bls, int i, int j, int k, int l) {
      return grid(bls, i, j, k, l)
         && grid(bls, i, j + 1, k, l)
         && grid(bls, i, j + 2, k, l)
         && !grid(bls, i, j - 1, k, l)
         && !grid(bls, i, j + 3, k, l)
         && !grid(bls, i, j, k - 1, l)
         && !grid(bls, i, j, k + 1, l)
         && !grid(bls, i, j + 1, k - 1, l)
         && !grid(bls, i, j + 1, k + 1, l)
         && !grid(bls, i, j + 2, k - 1, l)
         && !grid(bls, i, j + 2, k + 1, l);
   }

   private static boolean walled3x1Z(boolean[] bls, int i, int j, int k, int l) {
      return grid(bls, i, j, k, l)
         && grid(bls, i, j, k + 1, l)
         && grid(bls, i, j, k + 2, l)
         && !grid(bls, i, j, k - 1, l)
         && !grid(bls, i, j, k + 3, l)
         && !grid(bls, i, j - 1, k, l)
         && !grid(bls, i, j + 1, k, l)
         && !grid(bls, i, j - 1, k + 1, l)
         && !grid(bls, i, j + 1, k + 1, l)
         && !grid(bls, i, j - 1, k + 2, l)
         && !grid(bls, i, j + 1, k + 2, l);
   }

   private static boolean grid(boolean[] bls, int i, int j, int k, int l) {
      int var5 = i - l;
      return bls[var5 * 20 * 20 + (k + 1) * 20 + j + 1];
   }

   private void checkTunnelStarts(ClientWorld clientWorld, BlockPos blockPos, boolean bl, List<HoleEspModule.Hit> list, Set<String> set) {
      int var6 = Math.round(value(this.sMinTunnelLength, 3.0F));

      for (int[] var10 : HORIZONTAL_DIRS) {
         Mutable var11 = new Mutable(blockPos.getX() - var10[0], blockPos.getY(), blockPos.getZ() - var10[1]);
         if (!this.isTunnelSection(clientWorld, var11, var10, bl)) {
            Mutable var12 = new Mutable(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            BlockPos var13 = null;
            BlockPos var14 = null;
            int var15 = 0;
            int var16 = 0;
            if (this.isTunnelSection(clientWorld, var12, var10, bl)) {
               var13 = var12.toImmutable();
            }

            while (this.isTunnelSection(clientWorld, var12, var10, bl)) {
               var16 = Math.max(var16, this.getTunnelHeight(clientWorld, var12, bl));
               var14 = var12.toImmutable();
               var12.move(var10[0], 0, var10[1]);
               var15++;
            }

            if (var13 != null && var14 != null && var15 >= var6) {
               addHit(
                  list,
                  set,
                  new Box(
                     Math.min(var13.getX(), var14.getX()),
                     var13.getY(),
                     Math.min(var13.getZ(), var14.getZ()),
                     Math.max(var13.getX(), var14.getX()) + 1,
                     var13.getY() + var16,
                     Math.max(var13.getZ(), var14.getZ()) + 1
                  ),
                  2
               );
            }
         }
      }
   }

   private void checkStaircaseStarts(ClientWorld clientWorld, BlockPos blockPos, boolean bl, List<HoleEspModule.Hit> list, Set<String> set) {
      int var6 = Math.round(value(this.sMinStairLength, 3.0F));

      for (int[] var10 : HORIZONTAL_DIRS) {
         Mutable var11 = new Mutable(blockPos.getX() - var10[0], blockPos.getY() - 1, blockPos.getZ() - var10[1]);
         if (!this.isStaircaseSection(clientWorld, var11, var10, bl)) {
            Mutable var12 = new Mutable(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            int var13 = 0;

            ArrayList var14;
            for (var14 = new ArrayList(); this.isStaircaseSection(clientWorld, var12, var10, bl); var13++) {
               int var15 = this.getStaircaseHeight(clientWorld, var12, bl);
               var14.add(new Box(var12.getX(), var12.getY(), var12.getZ(), var12.getX() + 1, var12.getY() + var15, var12.getZ() + 1));
               var12.move(var10[0], 1, var10[1]);
            }

            if (var13 >= var6) {
               for (Box var16 : var14) {
                  addHit(list, set, var16, 3);
               }
            }
         }
      }
   }

   private boolean isTunnelSection(ClientWorld clientWorld, BlockPos blockPos, int[] is, boolean bl) {
      int var5 = this.getTunnelHeight(clientWorld, blockPos, bl);
      if (var5 >= Math.round(value(this.sMinTunnelHeight, 2.0F)) && var5 <= Math.round(value(this.sMaxTunnelHeight, 3.0F))) {
         if (!isPassable(clientWorld, blockPos.getX(), blockPos.getY() - 1, blockPos.getZ(), bl)
            && !isPassable(clientWorld, blockPos.getX(), blockPos.getY() + var5, blockPos.getZ(), bl)) {
            boolean var6 = is[0] != 0;

            for (int var7 = 0; var7 < var5; var7++) {
               int var8 = blockPos.getY() + var7;
               if (var6
                  ? isPassable(clientWorld, blockPos.getX(), var8, blockPos.getZ() - 1, bl)
                     || isPassable(clientWorld, blockPos.getX(), var8, blockPos.getZ() + 1, bl)
                  : isPassable(clientWorld, blockPos.getX() - 1, var8, blockPos.getZ(), bl)
                     || isPassable(clientWorld, blockPos.getX() + 1, var8, blockPos.getZ(), bl)) {
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

   private boolean isStaircaseSection(ClientWorld clientWorld, BlockPos blockPos, int[] is, boolean bl) {
      int var5 = this.getStaircaseHeight(clientWorld, blockPos, bl);
      if (var5 >= Math.round(value(this.sMinStairHeight, 3.0F)) && var5 <= Math.round(value(this.sMaxStairHeight, 5.0F))) {
         if (!isPassable(clientWorld, blockPos.getX(), blockPos.getY() - 1, blockPos.getZ(), bl)
            && !isPassable(clientWorld, blockPos.getX(), blockPos.getY() + var5, blockPos.getZ(), bl)) {
            boolean var6 = is[0] != 0;

            for (int var7 = 0; var7 < var5; var7++) {
               int var8 = blockPos.getY() + var7;
               if (var6
                  ? isPassable(clientWorld, blockPos.getX(), var8, blockPos.getZ() - 1, bl)
                     || isPassable(clientWorld, blockPos.getX(), var8, blockPos.getZ() + 1, bl)
                  : isPassable(clientWorld, blockPos.getX() - 1, var8, blockPos.getZ(), bl)
                     || isPassable(clientWorld, blockPos.getX() + 1, var8, blockPos.getZ(), bl)) {
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

   private int getTunnelHeight(ClientWorld clientWorld, BlockPos blockPos, boolean bl) {
      int var5 = Math.round(value(this.sMaxTunnelHeight, 3.0F));
      int var4 = 0;

      while (var4 < var5 && isPassable(clientWorld, blockPos.getX(), blockPos.getY() + var4, blockPos.getZ(), bl)) {
         var4++;
      }

      return var4;
   }

   private int getStaircaseHeight(ClientWorld clientWorld, BlockPos blockPos, boolean bl) {
      int var5 = Math.round(value(this.sMaxStairHeight, 5.0F));
      int var4 = 0;

      while (var4 < var5 && isPassable(clientWorld, blockPos.getX(), blockPos.getY() + var4, blockPos.getZ(), bl)) {
         var4++;
      }

      return var4;
   }

   private static boolean isOpen(ClientWorld clientWorld, BlockPos blockPos) {
      BlockState var2 = clientWorld.getBlockState(blockPos);
      return var2.isAir() || var2.getCollisionShape(clientWorld, blockPos).isEmpty();
   }

   private static boolean isPassable(ClientWorld clientWorld, int i, int j, int k, boolean bl) {
      return isPassable(clientWorld, PASSABLE_POS.get().set(i, j, k), bl);
   }

   private static boolean isPassable(ClientWorld clientWorld, BlockPos blockPos, boolean bl) {
      BlockState var3 = clientWorld.getBlockState(blockPos);
      if (var3.isAir()) {
         return true;
      } else if (bl) {
         return false;
      } else {
         VoxelShape var4 = var3.getCollisionShape(clientWorld, blockPos);
         return var4.isEmpty() || !VoxelShapes.fullCube().equals(var4);
      }
   }

   private static void addHit(List<HoleEspModule.Hit> list, Set<String> set, Box box, int i) {
      String var4 = i + ":" + box.minX + "," + box.minY + "," + box.minZ + "," + box.maxX + "," + box.maxY + "," + box.maxZ;
      if (set.add(var4)) {
         for (HoleEspModule.Hit var6 : list) {
            if (var6.type() == i && var6.box().intersects(box)) {
               return;
            }
         }

         list.add(new HoleEspModule.Hit(box, i));
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

   private int lineColor(int i) {
      return switch (i) {
         case 1 -> color(this.sHole3Line, 1610589440);
         case 2 -> color(this.sTunnelLine, 1593835775);
         case 3 -> color(this.sStairLine, 1610547455);
         default -> color(this.sHoleLine, 1610547200);
      };
   }

   private int fillColor(int i) {
      return switch (i) {
         case 1 -> color(this.sHole3Fill, 520070400);
         case 2 -> color(this.sTunnelFill, 503316735);
         case 3 -> color(this.sStairFill, 520028415);
         default -> color(this.sHoleFill, 520028160);
      };
   }

   private static int chunkX(long l) {
      return (int)l;
   }

   private static int chunkZ(long l) {
      return (int)(l >> 32);
   }

   private int scanRadius(MinecraftClient minecraftClient) {
      int var2 = minecraftClient.options.getViewDistance().getValue();
      float var3 = value(this.sScanRadius, 4.0F);
      if (isFastTravel(minecraftClient)) {
         var3 = Math.max(var3, 12.0F);
      }

      int var4 = Math.max(1, Math.round(var3));
      return Math.min(var2, var4);
   }

   private static boolean isFastTravel(MinecraftClient minecraftClient) {
      if (minecraftClient.player == null) {
         return false;
      } else {
         Vec3d var1 = minecraftClient.player.getVelocity();
         double var2 = var1.x * var1.x + var1.z * var1.z;
         return minecraftClient.player.isGliding() || var2 > 0.18;
      }
   }

   private static float value(NumberSetting numberSetting, float f) {
      return numberSetting == null ? f : numberSetting.value()[0];
   }

   private static int color(ColorSetting colorSetting, int i) {
      return colorSetting == null ? i : colorSetting.argb();
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

      FastHoleScan(HoleEspModule holeEspModule2, WorldChunk worldChunk, long l, int i, int j, int k, int m) {
         this.chunk = worldChunk;
         this.level = holeEspModule2.sessionLevel;
         this.key = l;
         this.baseX = i;
         this.baseZ = j;
         this.minY = k;
         this.maxY = m;
         this.gridMinY = k - 1;
         this.yCount = m - this.gridMinY + 1;
         this.minDepth = Math.round(HoleEspModule.value(holeEspModule2.sMinHoleDepth, 4.0F));
         int var10 = Math.max(0, this.yCount) * 20 * 20;
         if (holeEspModule2.passableBuffer.length < var10) {
            holeEspModule2.passableBuffer = new boolean[var10];
         }

         this.passable = holeEspModule2.passableBuffer;

         for (int var11 = -1; var11 <= 1; var11++) {
            for (int var12 = -1; var12 <= 1; var12++) {
               this.neighbors[(var11 + 1) * 3 + var12 + 1] = this.level.getChunkManager().getWorldChunk((i >> 4) + var11, (j >> 4) + var12);
            }
         }
      }

      boolean validFor(ClientWorld clientWorld, int i, int j, int k) {
         if (clientWorld == this.level && Math.abs((long)this.chunk.getPos().x - i) <= k && Math.abs((long)this.chunk.getPos().z - j) <= k) {
            for (int var5 = -1; var5 <= 1; var5++) {
               for (int var6 = -1; var6 <= 1; var6++) {
                  if (this.neighbors[(var5 + 1) * 3 + var6 + 1]
                     != this.level.getChunkManager().getWorldChunk((this.baseX >> 4) + var5, (this.baseZ >> 4) + var6)) {
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
                  this.pos.set(this.baseX + var5, var6, this.baseZ + var3);
                  this.passable[var4 + var5 + 1] = HoleEspModule.isOpen(this.level, this.pos);
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
