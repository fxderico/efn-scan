package net.efn.client.module.impl.render;

import com.example.client.mixin.AccessorPinkWaterLevelRenderer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLongArray;
import net.efn.client.event.EventPriority;
import net.efn.client.event.ListenerHandle;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class PinkWater {
   public static final String OFF = "Off";
   static final int WARM = 4445678;
   static final int LUKEWARM = 4566514;
   static final int CHERRY_GROVE = 6141935;
   static final int OCEAN = 4159204;
   static final int PALE_GARDEN = 7768221;
   static final int SWAMP = 6388580;
   static final int MANGROVE = 3832426;
   static final int COLD = 4020182;
   static final int MEADOW = 937679;
   static final int FROZEN = 3750089;
   static final int[] BIOMES = new int[]{4445678, 4566514, 6141935, 4159204, 7768221, 6388580, 3832426, 4020182, 937679, 3750089};
   private static final double[][] BIOME_LAB = new double[BIOMES.length][];
   private static final double FOG_HUE = 240.0;
   private static final double FOG_SATURATION = 0.902;
   private static final int SWITCH_ON_TICKS = 60;
   private static final int SWITCH_OFF_TICKS = 40;
   private static final int CHANGE_TICKS = 15;
   private static final float FOG_STEP = 0.05F;
   private static final int UNSEEN_BATCH = 64;
   private static final int UNSEEN_WAIT = 2;
   private static final int UNSEEN_PATIENCE = 20;
   private static final int UNSEEN_TRICKLE = 8;
   private static final double BLEND_POWER = 3.0;
   private static final double LIGHT_CARRY = 0.8;
   private static volatile PinkWater.Design shown;
   private static volatile PinkWater.Design fogDesign;
   private static volatile float fogShare;
   private static PinkWater.Design chosen;
   private static PinkWater.Design pending;
   private static int pendingTicks;
   private static float fogLevel;
   private static final List<PinkWater.Unseen> UNSEEN;
   private static int unseenNext;
   private static ClientWorld unseenLevel;
   private static BuiltChunkStorage unseenArea;
   private static int unseenWait;
   private static int unseenStalled;
   private static final Object UNSEEN_OWNER;
   private static ListenerHandle<TickEvent> unseenHook;
   private static final int CACHE_SIZE = 64;
   private static final AtomicLongArray CACHE;

   private PinkWater() {
   }

   public static String[] modes() {
      PinkWater.Design[] var0 = PinkWater.Design.values();
      String[] var1 = new String[var0.length + 1];
      var1[0] = "Off";

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2 + 1] = var0[var2].label;
      }

      return var1;
   }

   public static boolean shows() {
      return shown != null;
   }

   public static int water(int i, boolean bl) {
      PinkWater.Design var2 = shown;
      return var2 == null ? i : cached(i, bl, var2);
   }

   public static byte caveBiome(Biome biome) {
      ClientWorld var1 = MinecraftClient.getInstance().world;
      if (var1 == null) {
         return 0;
      } else {
         Optional var2 = var1.getRegistryManager().getOptional(RegistryKeys.BIOME);
         if (var2.isEmpty()) {
            return 0;
         } else {
            Optional var3 = ((Registry)var2.get()).getKey(biome);
            if (var3.isEmpty()) {
               return 1;
            } else {
               RegistryKey var4 = (RegistryKey)var3.get();
               return (byte)(!var4.equals(BiomeKeys.LUSH_CAVES) && !var4.equals(BiomeKeys.DRIPSTONE_CAVES) && !var4.equals(BiomeKeys.DEEP_DARK) ? 1 : 2);
            }
         }
      }
   }

   public static int waterFog(int i) {
      float var1 = fogShare;
      PinkWater.Design var2 = fogDesign;
      return !(var1 <= 0.0F) && var2 != null ? mix(i, waterFog(i, var2), var1) : i;
   }

   public static void underwater(Vector4f vector4f) {
      float var1 = fogShare;
      PinkWater.Design var2 = fogDesign;
      if (!(var1 <= 0.0F) && var2 != null) {
         float var3 = 1.0F + ((float)var2.fog.shade - 1.0F) * var1;
         vector4f.x *= var3;
         vector4f.y *= var3;
         vector4f.z *= var3;
      }
   }

   public static int light(int i) {
      PinkWater.Design var1 = shown;
      return var1 == null ? i : light(i, var1);
   }

   public static void tick(MinecraftClient minecraftClient, boolean bl, PinkWater.Design design) {
      PinkWater.Design var3 = bl ? design : null;
      if (var3 == chosen) {
         pending = null;
         pendingTicks = 0;
      } else {
         if (var3 != pending) {
            pending = var3;
            pendingTicks = 0;
         }

         int var5 = chosen == null ? 60 : (design != null && var3 == null ? 40 : 15);
         if (++pendingTicks >= var5) {
            chosen = var3;
            pending = null;
            pendingTicks = 0;
         }
      }

      ClientWorld var7 = minecraftClient.world;
      boolean var6 = var7 != null && var7.getRegistryKey() != World.NETHER && var7.getRegistryKey() != World.END;
      show(minecraftClient, var6 ? chosen : null);
      fogLevel = Math.max(0.0F, Math.min(1.0F, fogLevel + (shown != null ? 0.05F : -0.05F)));
      fogShare = fogLevel * fogLevel * (3.0F - 2.0F * fogLevel);
   }

   public static void reset(MinecraftClient minecraftClient) {
      chosen = null;
      pending = null;
      pendingTicks = 0;
      fogLevel = 0.0F;
      fogShare = 0.0F;
      show(minecraftClient, null);
   }

   private static void show(MinecraftClient minecraftClient, PinkWater.Design design) {
      if (design != shown) {
         shown = design;
         if (design != null) {
            fogDesign = design;
         }

         ClientWorld var2 = minecraftClient.world;
         if (minecraftClient.world != null && minecraftClient.worldRenderer != null) {
            var2.reloadColor();
            refresh(minecraftClient, var2);
         } else {
            clearUnseen();
         }
      }
   }

   private static void refresh(MinecraftClient minecraftClient, ClientWorld clientWorld) {
      WorldRenderer var2 = minecraftClient.worldRenderer;
      ChunkSectionPos var3 = ChunkSectionPos.from(minecraftClient.gameRenderer.getCamera().getCameraPos());
      int var4 = minecraftClient.options.getClampedViewDistance() + 1;
      LongOpenHashSet var5 = new LongOpenHashSet();

      for (int var6 = var3.getSectionX() - var4; var6 <= var3.getSectionX() + var4; var6++) {
         for (int var7 = var3.getSectionZ() - var4; var7 <= var3.getSectionZ() + var4; var7++) {
            WorldChunk var8 = clientWorld.getChunkManager().getWorldChunk(var6, var7);
            if (var8 != null) {
               ChunkSection[] var9 = var8.getSectionArray();

               for (int var10 = 0; var10 < var9.length; var10++) {
                  ChunkSection var11 = var9[var10];
                  if (!var11.isEmpty() && var11.hasAny(PinkWater::tinted)) {
                     var5.add(ChunkSectionPos.asLong(var6, var8.sectionIndexToCoord(var10), var7));
                  }
               }
            }
         }
      }

      queueUnseen(var2, clientWorld, var3, var5);
      LongIterator var12 = var5.iterator();

      while (var12.hasNext()) {
         long var13 = var12.nextLong();
         var2.scheduleChunkRender(ChunkSectionPos.unpackX(var13), ChunkSectionPos.unpackY(var13), ChunkSectionPos.unpackZ(var13));
      }
   }

   private static boolean tinted(BlockState blockState) {
      return Fluids.WATER.matchesType(blockState.getFluidState().getFluid()) || blockState.isOf(Blocks.WATER_CAULDRON);
   }

   private static void queueUnseen(WorldRenderer worldRenderer, ClientWorld clientWorld, ChunkSectionPos chunkSectionPos, LongOpenHashSet longOpenHashSet) {
      clearUnseen();
      BuiltChunkStorage var4 = ((AccessorPinkWaterLevelRenderer)worldRenderer).efn$pinkWaterViewArea();
      if (var4 != null && var4.chunks != null && !longOpenHashSet.isEmpty()) {
         for (BuiltChunk var8 : var4.chunks) {
            long var9;
            if (var8 != null && longOpenHashSet.contains(var9 = var8.getSectionPos())) {
               boolean var11 = var8.getCurrentRenderData() == ChunkRenderData.HIDDEN && !var8.needsRebuild();
               UNSEEN.add(new PinkWater.Unseen(var8, var9, var11));
            }
         }

         if (!UNSEEN.isEmpty()) {
            UNSEEN.sort(Comparator.comparingLong(unseen -> distance(unseen.node, chunkSectionPos)));
            unseenLevel = clientWorld;
            unseenArea = var4;
            unseenWait = 2;
            unseenStalled = 0;
            if (unseenHook == null) {
               unseenHook = TickEvent.TYPE.listen(UNSEEN_OWNER, Stage.POST, EventPriority.NORMAL, tickEvent -> rebuildUnseen(MinecraftClient.getInstance()));
            }
         }
      }
   }

   private static void rebuildUnseen(MinecraftClient minecraftClient) {
      if (unseenNext >= UNSEEN.size()) {
         clearUnseen();
         unhookUnseen();
      } else {
         WorldRenderer var3 = minecraftClient.worldRenderer;
         ChunkBuilder var2 = var3 == null ? null : var3.getChunkBuilder();
         if (minecraftClient.world != unseenLevel || var2 == null || ((AccessorPinkWaterLevelRenderer)var3).efn$pinkWaterViewArea() != unseenArea) {
            clearUnseen();
            unhookUnseen();
         } else if (unseenWait > 0) {
            unseenWait--;
         } else {
            int var1;
            if (var2.getScheduledTaskCount() == 0) {
               var1 = 64;
               unseenStalled = 0;
            } else {
               if (++unseenStalled < 20) {
                  return;
               }

               var1 = 8;
               unseenStalled = 0;
            }

            ChunkRendererRegionBuilder var5 = null;

            while (var1 > 0 && unseenNext < UNSEEN.size()) {
               PinkWater.Unseen var6 = UNSEEN.get(unseenNext++);
               BuiltChunk var7 = var6.section;
               if (var7.getSectionPos() == var6.node
                  && var7.needsRebuild()
                  && (var7.getCurrentRenderData() != ChunkRenderData.HIDDEN || var6.firstBuild && var7.shouldBuild())) {
                  if (var5 == null) {
                     var5 = new ChunkRendererRegionBuilder();
                  }

                  var7.scheduleRebuild(var5);
                  var7.cancelRebuild();
                  var1--;
               }
            }
         }
      }
   }

   private static void clearUnseen() {
      UNSEEN.clear();
      unseenNext = 0;
      unseenLevel = null;
      unseenArea = null;
   }

   private static void unhookUnseen() {
      if (unseenHook != null) {
         unseenHook.unregister();
         unseenHook = null;
      }
   }

   private static long distance(long l, ChunkSectionPos chunkSectionPos) {
      long var3 = ChunkSectionPos.unpackX(l) - chunkSectionPos.getSectionX();
      long var5 = ChunkSectionPos.unpackY(l) - chunkSectionPos.getSectionY();
      long var7 = ChunkSectionPos.unpackZ(l) - chunkSectionPos.getSectionZ();
      return var3 * var3 + var5 * var5 + var7 * var7;
   }

   public static int water(int i, PinkWater.Design design) {
      return water(i, false, design);
   }

   public static int water(int i, boolean bl, PinkWater.Design design) {
      int var4 = index(i & 16777215);
      double[] var3 = var4 >= 0 ? (double[])design.water[var4].clone() : between(oklab(i), design);
      if (bl) {
         for (int var6 = 0; var6 < 3; var6++) {
            var3[var6] += design.cave[var6];
         }
      }

      return fromOklab(var3[0], var3[1], var3[2]);
   }

   private static double[] between(double[] ds, PinkWater.Design design) {
      double var2 = 0.0;
      double var4 = 0.0;
      double var6 = 0.0;
      double var8 = 0.0;
      double var10 = 0.0;
      double var12 = 0.0;

      for (int var14 = 0; var14 < BIOMES.length; var14++) {
         double[] var15 = BIOME_LAB[var14];
         double[] var16 = design.water[var14];
         double var17 = ds[0] - var15[0];
         double var19 = ds[1] - var15[1];
         double var21 = ds[2] - var15[2];
         double var23 = 1.0 / Math.pow(Math.max(var17 * var17 + var19 * var19 + var21 * var21, 1.0E-12), 1.5);
         var2 += var23;
         var4 += var23 * var16[0];
         var6 += var23 * var16[1];
         var8 += var23 * var16[2];
         var10 += var23 * var15[0];
         var12 += var23 * Math.hypot(var15[1], var15[2]);
      }

      var4 /= var2;
      var4 = clamp(var4 + (ds[0] - var10 / var2) * 0.8, 0.3, 0.92);
      double var29 = Math.sqrt(clamp(Math.hypot(ds[1], ds[2]) / Math.max(var12 / var2, 1.0E-6), 0.4, 1.6));
      double var27;
      double var28;
      return new double[]{var4, (var27 = var6 / var2) * var29, (var28 = var8 / var2) * var29};
   }

   static int index(int i) {
      for (int var1 = 0; var1 < BIOMES.length; var1++) {
         if (BIOMES[var1] == i) {
            return var1;
         }
      }

      return -1;
   }

   public static int waterFog(int i, PinkWater.Design design) {
      PinkWater.Fog var2 = design.fog;
      int var3 = i >> 16 & 0xFF;
      int var4 = i >> 8 & 0xFF;
      int var5 = i & 0xFF;
      int var6 = Math.max(var3, Math.max(var4, var5));
      int var7 = Math.min(var3, Math.min(var4, var5));
      double var8 = var6 / 255.0;
      double var10 = var6 == 0 ? 0.0 : (double)(var6 - var7) / var6;
      double var12 = 0.0;
      if (var6 != var7) {
         double var14 = var6 - var7;
         var12 = var6 == var3 ? 60.0 * ((var4 - var5) / var14) : (var6 == var4 ? 60.0 * ((var5 - var3) / var14 + 2.0) : 60.0 * ((var3 - var4) / var14 + 4.0));
      }

      double var22 = var2.hueFollow * smooth(0.1, 0.3, var10);
      double var16 = var2.hue + clamp(-var22 * wrap(var12 - 240.0), -var2.maxShift, var2.maxShift);
      double var18 = clamp(var2.saturation * Math.pow(clamp(var10 / 0.902, 0.25, 1.1), 0.7), 0.0, 1.0);
      double var20 = clamp(var8 * var2.value, 0.0, 1.0);
      return i & 0xFF000000 | hsv(var16, var18, var20);
   }

   public static int underwater(int i, PinkWater.Design design) {
      double var2 = design.fog.shade;
      return srgbChannel((i >> 16 & 0xFF) / 255.0 * var2) << 16 | srgbChannel((i >> 8 & 0xFF) / 255.0 * var2) << 8 | srgbChannel((i & 0xFF) / 255.0 * var2);
   }

   public static int light(int i, PinkWater.Design design) {
      int var2 = i >>> 20 & 15;
      int var3 = (design.glow * var2 + 7) / 15 << 4;
      return var3 > 0 && (i & 0xFF) < var3 ? i & -256 | var3 : i;
   }

   private static int cached(int i, boolean bl, PinkWater.Design design) {
      int var3 = i & 16777215 | design.ordinal() + 1 << 24 | (bl ? 1073741824 : 0);
      int var4 = var3 * -1640531535 >>> 26 & 63;
      long var5 = CACHE.getOpaque(var4);
      if ((int)(var5 >>> 32) == var3) {
         return (int)var5;
      } else {
         int var7 = water(i, bl, design);
         CACHE.setOpaque(var4, (long)var3 << 32 | var7 & 4294967295L);
         return var7;
      }
   }

   static double[] oklab(int i) {
      double var1 = linear((i >> 16 & 0xFF) / 255.0);
      double var3 = linear((i >> 8 & 0xFF) / 255.0);
      double var5 = linear((i & 0xFF) / 255.0);
      double var7 = Math.cbrt(0.4122214708 * var1 + 0.5363325363 * var3 + 0.0514459929 * var5);
      double var9 = Math.cbrt(0.2119034982 * var1 + 0.6806995451 * var3 + 0.1073969566 * var5);
      double var11 = Math.cbrt(0.0883024619 * var1 + 0.2817188376 * var3 + 0.6299787005 * var5);
      return new double[]{
         0.2104542553 * var7 + 0.793617785 * var9 - 0.0040720468 * var11,
         1.9779984951 * var7 - 2.428592205 * var9 + 0.4505937099 * var11,
         0.0259040371 * var7 + 0.7827717662 * var9 - 0.808675766 * var11
      };
   }

   private static int fromOklab(double d, double e, double f) {
      double[] var6 = new double[3];
      if (!fromOklab(d, e, f, var6)) {
         double var7 = 0.0;
         double var9 = 1.0;

         for (int var11 = 0; var11 < 14; var11++) {
            double var12 = (var7 + var9) * 0.5;
            if (fromOklab(d, e * var12, f * var12, var6)) {
               var7 = var12;
            } else {
               var9 = var12;
            }
         }

         fromOklab(d, e * var7, f * var7, var6);
      }

      return channel(var6[0]) << 16 | channel(var6[1]) << 8 | channel(var6[2]);
   }

   private static boolean fromOklab(double d, double e, double f, double[] ds) {
      double var7 = d + 0.3963377774 * e + 0.2158037573 * f;
      double var9 = d - 0.1055613458 * e - 0.0638541728 * f;
      double var11 = d - 0.0894841775 * e - 1.291485548 * f;
      var7 = var7 * var7 * var7;
      var9 = var9 * var9 * var9;
      var11 = var11 * var11 * var11;
      ds[0] = 4.0767416621 * var7 - 3.3077115913 * var9 + 0.2309699292 * var11;
      ds[1] = -1.2684380046 * var7 + 2.6097574011 * var9 - 0.3413193965 * var11;
      ds[2] = -0.0041960863 * var7 - 0.7034186147 * var9 + 1.707614701 * var11;
      double var13 = 1.0E-4;
      return ds[0] >= -var13 && ds[0] <= 1.0 + var13 && ds[1] >= -var13 && ds[1] <= 1.0 + var13 && ds[2] >= -var13 && ds[2] <= 1.0 + var13;
   }

   private static int hsv(double d, double e, double f) {
      double var10 = (d % 360.0 + 360.0) % 360.0 / 60.0;
      int var12 = (int)Math.floor(var10) % 6;
      double var13 = var10 - Math.floor(var10);
      double var15 = f * (1.0 - e);
      double var17 = f * (1.0 - e * var13);
      double var19 = f * (1.0 - e * (1.0 - var13));
      double var6;
      double var8;

      double var21 = switch (var12) {
         case 0 -> {
            var8 = f;
            var6 = var19;
            yield var15;
         }
         case 1 -> {
            var8 = var17;
            var6 = f;
            yield var15;
         }
         case 2 -> {
            var8 = var15;
            var6 = f;
            yield var19;
         }
         case 3 -> {
            var8 = var15;
            var6 = var17;
            yield f;
         }
         case 4 -> {
            var8 = var19;
            var6 = var15;
            yield f;
         }
         default -> {
            var8 = f;
            var6 = var15;
            yield var17;
         }
      };
      return srgbChannel(var8) << 16 | srgbChannel(var6) << 8 | srgbChannel(var21);
   }

   private static double linear(double d) {
      return d <= 0.04045 ? d / 12.92 : Math.pow((d + 0.055) / 1.055, 2.4);
   }

   private static int channel(double d) {
      double var2 = clamp(d, 0.0, 1.0);
      double var4 = var2 <= 0.0031308 ? var2 * 12.92 : 1.055 * Math.pow(var2, 0.4166666666666667) - 0.055;
      return srgbChannel(var4);
   }

   private static int srgbChannel(double d) {
      return (int)Math.round(clamp(d, 0.0, 1.0) * 255.0);
   }

   private static int mix(int i, int j, float f) {
      int var3 = Math.round((i >> 16 & 0xFF) + ((j >> 16 & 0xFF) - (i >> 16 & 0xFF)) * f);
      int var4 = Math.round((i >> 8 & 0xFF) + ((j >> 8 & 0xFF) - (i >> 8 & 0xFF)) * f);
      int var5 = Math.round((i & 0xFF) + ((j & 0xFF) - (i & 0xFF)) * f);
      return i & 0xFF000000 | var3 << 16 | var4 << 8 | var5;
   }

   private static double wrap(double d) {
      double var2 = d % 360.0;
      if (var2 > 180.0) {
         var2 -= 360.0;
      }

      if (var2 <= -180.0) {
         var2 += 360.0;
      }

      return var2;
   }

   private static double smooth(double d, double e, double f) {
      double var6 = clamp((f - d) / (e - d), 0.0, 1.0);
      return var6 * var6 * (3.0 - 2.0 * var6);
   }

   private static double clamp(double d, double e, double f) {
      return d < e ? e : (d > f ? f : d);
   }

   static {
      for (int var0 = 0; var0 < BIOMES.length; var0++) {
         BIOME_LAB[var0] = oklab(BIOMES[var0]);
      }

      UNSEEN = new ArrayList<>();
      UNSEEN_OWNER = new Object();
      CACHE = new AtomicLongArray(64);
   }

   @Environment(EnvType.CLIENT)
   public static enum Design {
      ROSE_QUARTZ(
         "Rose Quartz",
         new int[]{16742585, 15429040, 15626161, 13392801, 12357803, 11165580, 10508930, 12476315, 12146838, 11817105},
         11862992,
         new PinkWater.Fog(338.0, 0.25, 15.0, 0.47, 1.0, 0.84),
         6
      ),
      PINK_SALT_LAKE(
         "Pink Salt Lake",
         new int[]{16759265, 16361428, 16427734, 15633086, 14923972, 14781610, 14452650, 15762095, 14908074, 14119845},
         12279295,
         new PinkWater.Fog(340.0, 0.15, 10.0, 0.36, 1.6, 0.77),
         7
      ),
      SUNSET_LAGOON(
         "Sunset Lagoon",
         new int[]{16751278, 16746659, 16747428, 16737936, 14130594, 14444925, 13789560, 15753090, 14900600, 14113390},
         16732671,
         new PinkWater.Fog(352.0, 0.3, 15.0, 0.46, 1.15, 0.81),
         2
      ),
      NEON_NIGHT(
         "Neon Night",
         new int[]{16740085, 15753959, 15885545, 14175952, 13076403, 13459385, 12802735, 12466090, 11808155, 11150220},
         15466751,
         new PinkWater.Fog(322.0, 0.2, 12.0, 0.5, 1.0, 0.82),
         8
      ),
      ORCHID_DEPTHS(
         "Orchid Depths",
         new int[]{13459425, 12538831, 12670418, 11026355, 10317461, 9193110, 9518220, 9515665, 8531337, 7547010},
         6815936,
         new PinkWater.Fog(300.0, 0.3, 15.0, 0.47, 0.9, 0.76),
         1
      ),
      COTTON_CANDY(
         "Cotton Candy",
         new int[]{16425215, 15569654, 15701240, 14253288, 14595804, 12157650, 12156370, 12485350, 11959777, 11499740},
         8402175,
         new PinkWater.Fog(300.0, 0.3, 20.0, 0.38, 1.5, 0.77),
         3
      );

      public static final PinkWater.Design DEFAULT = ROSE_QUARTZ;
      private final String label;
      final double[][] water;
      final double[] cave;
      final PinkWater.Fog fog;
      final int glow;

      private Design(String string2, int[] is, int j, PinkWater.Fog fog, int k) {
         if (is.length != PinkWater.BIOMES.length) {
            throw new IllegalArgumentException(string2 + ": one colour per biome");
         } else {
            this.label = string2;
            this.water = new double[is.length][];

            for (int var8 = 0; var8 < is.length; var8++) {
               this.water[var8] = PinkWater.oklab(is[var8]);
            }

            double[] var10 = this.water[PinkWater.index(4159204)];
            double[] var9 = PinkWater.oklab(j);
            this.cave = new double[]{var9[0] - var10[0], var9[1] - var10[1], var9[2] - var10[2]};
            this.fog = fog;
            this.glow = k;
         }
      }

      public String label() {
         return this.label;
      }

      public static PinkWater.Design byLabel(String string) {
         for (PinkWater.Design var4 : values()) {
            if (var4.label.equals(string)) {
               return var4;
            }
         }

         return null;
      }
   }

   @Environment(EnvType.CLIENT)
   record Fog(double hue, double hueFollow, double maxShift, double saturation, double value, double shade) {
   }

   @Environment(EnvType.CLIENT)
   private record Unseen(BuiltChunk section, long node, boolean firstBuild) {
   }
}
