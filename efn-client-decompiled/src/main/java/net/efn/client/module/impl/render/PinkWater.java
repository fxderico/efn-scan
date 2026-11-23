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

   public static int water(int var0, boolean var1) {
      PinkWater.Design var2 = shown;
      return var2 == null ? var0 : cached(var0, var1, var2);
   }

   public static byte caveBiome(Biome var0) {
      ClientWorld var1 = MinecraftClient.method_1551().field_1687;
      if (var1 == null) {
         return 0;
      } else {
         Optional var2 = var1.method_30349().method_46759(RegistryKeys.field_41236);
         if (var2.isEmpty()) {
            return 0;
         } else {
            Optional var3 = ((Registry)var2.get()).method_29113(var0);
            if (var3.isEmpty()) {
               return 1;
            } else {
               RegistryKey var4 = (RegistryKey)var3.get();
               return (byte)(!var4.equals(BiomeKeys.field_29218) && !var4.equals(BiomeKeys.field_28107) && !var4.equals(BiomeKeys.field_37543) ? 1 : 2);
            }
         }
      }
   }

   public static int waterFog(int var0) {
      float var1 = fogShare;
      PinkWater.Design var2 = fogDesign;
      return !(var1 <= 0.0F) && var2 != null ? mix(var0, waterFog(var0, var2), var1) : var0;
   }

   public static void underwater(Vector4f var0) {
      float var1 = fogShare;
      PinkWater.Design var2 = fogDesign;
      if (!(var1 <= 0.0F) && var2 != null) {
         float var3 = 1.0F + ((float)var2.fog.shade - 1.0F) * var1;
         var0.x *= var3;
         var0.y *= var3;
         var0.z *= var3;
      }
   }

   public static int light(int var0) {
      PinkWater.Design var1 = shown;
      return var1 == null ? var0 : light(var0, var1);
   }

   public static void tick(MinecraftClient var0, boolean var1, PinkWater.Design var2) {
      PinkWater.Design var3 = var1 ? var2 : null;
      if (var3 == chosen) {
         pending = null;
         pendingTicks = 0;
      } else {
         if (var3 != pending) {
            pending = var3;
            pendingTicks = 0;
         }

         int var5 = chosen == null ? 60 : (var2 != null && var3 == null ? 40 : 15);
         if (++pendingTicks >= var5) {
            chosen = var3;
            pending = null;
            pendingTicks = 0;
         }
      }

      ClientWorld var7 = var0.field_1687;
      boolean var6 = var7 != null && var7.method_27983() != World.field_25180 && var7.method_27983() != World.field_25181;
      show(var0, var6 ? chosen : null);
      fogLevel = Math.max(0.0F, Math.min(1.0F, fogLevel + (shown != null ? 0.05F : -0.05F)));
      fogShare = fogLevel * fogLevel * (3.0F - 2.0F * fogLevel);
   }

   public static void reset(MinecraftClient var0) {
      chosen = null;
      pending = null;
      pendingTicks = 0;
      fogLevel = 0.0F;
      fogShare = 0.0F;
      show(var0, null);
   }

   private static void show(MinecraftClient var0, PinkWater.Design var1) {
      if (var1 != shown) {
         shown = var1;
         if (var1 != null) {
            fogDesign = var1;
         }

         ClientWorld var2 = var0.field_1687;
         if (var0.field_1687 != null && var0.field_1769 != null) {
            var2.method_23784();
            refresh(var0, var2);
         } else {
            clearUnseen();
         }
      }
   }

   private static void refresh(MinecraftClient var0, ClientWorld var1) {
      WorldRenderer var2 = var0.field_1769;
      ChunkSectionPos var3 = ChunkSectionPos.method_18680(var0.field_1773.method_19418().method_71156());
      int var4 = var0.field_1690.method_38521() + 1;
      LongOpenHashSet var5 = new LongOpenHashSet();

      for (int var6 = var3.method_18674() - var4; var6 <= var3.method_18674() + var4; var6++) {
         for (int var7 = var3.method_18687() - var4; var7 <= var3.method_18687() + var4; var7++) {
            WorldChunk var8 = var1.method_2935().method_21730(var6, var7);
            if (var8 != null) {
               ChunkSection[] var9 = var8.method_12006();

               for (int var10 = 0; var10 < var9.length; var10++) {
                  ChunkSection var11 = var9[var10];
                  if (!var11.method_38292() && var11.method_19523(PinkWater::tinted)) {
                     var5.add(ChunkSectionPos.method_18685(var6, var8.method_31604(var10), var7));
                  }
               }
            }
         }
      }

      queueUnseen(var2, var1, var3, var5);
      LongIterator var12 = var5.iterator();

      while (var12.hasNext()) {
         long var13 = var12.nextLong();
         var2.method_8571(ChunkSectionPos.method_18686(var13), ChunkSectionPos.method_18689(var13), ChunkSectionPos.method_18690(var13));
      }
   }

   private static boolean tinted(BlockState var0) {
      return Fluids.field_15910.method_15780(var0.method_26227().method_15772()) || var0.method_27852(Blocks.field_27097);
   }

   private static void queueUnseen(WorldRenderer var0, ClientWorld var1, ChunkSectionPos var2, LongOpenHashSet var3) {
      clearUnseen();
      BuiltChunkStorage var4 = ((AccessorPinkWaterLevelRenderer)var0).efn$pinkWaterViewArea();
      if (var4 != null && var4.field_4150 != null && !var3.isEmpty()) {
         for (BuiltChunk var8 : var4.field_4150) {
            long var9;
            if (var8 != null && var3.contains(var9 = var8.method_62975())) {
               boolean var11 = var8.method_72052() == ChunkRenderData.field_60935 && !var8.method_3672();
               UNSEEN.add(new PinkWater.Unseen(var8, var9, var11));
            }
         }

         if (!UNSEEN.isEmpty()) {
            UNSEEN.sort(Comparator.comparingLong(var1x -> distance(var1x.node, var2)));
            unseenLevel = var1;
            unseenArea = var4;
            unseenWait = 2;
            unseenStalled = 0;
            if (unseenHook == null) {
               unseenHook = TickEvent.TYPE.listen(UNSEEN_OWNER, Stage.POST, EventPriority.NORMAL, var0x -> rebuildUnseen(MinecraftClient.method_1551()));
            }
         }
      }
   }

   private static void rebuildUnseen(MinecraftClient var0) {
      if (unseenNext >= UNSEEN.size()) {
         clearUnseen();
         unhookUnseen();
      } else {
         WorldRenderer var3 = var0.field_1769;
         ChunkBuilder var2 = var3 == null ? null : var3.method_34810();
         if (var0.field_1687 != unseenLevel || var2 == null || ((AccessorPinkWaterLevelRenderer)var3).efn$pinkWaterViewArea() != unseenArea) {
            clearUnseen();
            unhookUnseen();
         } else if (unseenWait > 0) {
            unseenWait--;
         } else {
            int var1;
            if (var2.method_72046() == 0) {
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
               if (var7.method_62975() == var6.node
                  && var7.method_3672()
                  && (var7.method_72052() != ChunkRenderData.field_60935 || var6.firstBuild && var7.method_3673())) {
                  if (var5 == null) {
                     var5 = new ChunkRendererRegionBuilder();
                  }

                  var7.method_22777(var5);
                  var7.method_3662();
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

   private static long distance(long var0, ChunkSectionPos var2) {
      long var3 = ChunkSectionPos.method_18686(var0) - var2.method_18674();
      long var5 = ChunkSectionPos.method_18689(var0) - var2.method_18683();
      long var7 = ChunkSectionPos.method_18690(var0) - var2.method_18687();
      return var3 * var3 + var5 * var5 + var7 * var7;
   }

   public static int water(int var0, PinkWater.Design var1) {
      return water(var0, false, var1);
   }

   public static int water(int var0, boolean var1, PinkWater.Design var2) {
      int var4 = index(var0 & 16777215);
      double[] var3 = var4 >= 0 ? (double[])var2.water[var4].clone() : between(oklab(var0), var2);
      if (var1) {
         for (int var6 = 0; var6 < 3; var6++) {
            var3[var6] += var2.cave[var6];
         }
      }

      return fromOklab(var3[0], var3[1], var3[2]);
   }

   private static double[] between(double[] var0, PinkWater.Design var1) {
      double var2 = 0.0;
      double var4 = 0.0;
      double var6 = 0.0;
      double var8 = 0.0;
      double var10 = 0.0;
      double var12 = 0.0;

      for (int var14 = 0; var14 < BIOMES.length; var14++) {
         double[] var15 = BIOME_LAB[var14];
         double[] var16 = var1.water[var14];
         double var17 = var0[0] - var15[0];
         double var19 = var0[1] - var15[1];
         double var21 = var0[2] - var15[2];
         double var23 = 1.0 / Math.pow(Math.max(var17 * var17 + var19 * var19 + var21 * var21, 1.0E-12), 1.5);
         var2 += var23;
         var4 += var23 * var16[0];
         var6 += var23 * var16[1];
         var8 += var23 * var16[2];
         var10 += var23 * var15[0];
         var12 += var23 * Math.hypot(var15[1], var15[2]);
      }

      var4 /= var2;
      var4 = clamp(var4 + (var0[0] - var10 / var2) * 0.8, 0.3, 0.92);
      double var29 = Math.sqrt(clamp(Math.hypot(var0[1], var0[2]) / Math.max(var12 / var2, 1.0E-6), 0.4, 1.6));
      double var27;
      double var28;
      return new double[]{var4, (var27 = var6 / var2) * var29, (var28 = var8 / var2) * var29};
   }

   static int index(int var0) {
      for (int var1 = 0; var1 < BIOMES.length; var1++) {
         if (BIOMES[var1] == var0) {
            return var1;
         }
      }

      return -1;
   }

   public static int waterFog(int var0, PinkWater.Design var1) {
      PinkWater.Fog var2 = var1.fog;
      int var3 = var0 >> 16 & 0xFF;
      int var4 = var0 >> 8 & 0xFF;
      int var5 = var0 & 0xFF;
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
      return var0 & 0xFF000000 | hsv(var16, var18, var20);
   }

   public static int underwater(int var0, PinkWater.Design var1) {
      double var2 = var1.fog.shade;
      return srgbChannel((var0 >> 16 & 0xFF) / 255.0 * var2) << 16
         | srgbChannel((var0 >> 8 & 0xFF) / 255.0 * var2) << 8
         | srgbChannel((var0 & 0xFF) / 255.0 * var2);
   }

   public static int light(int var0, PinkWater.Design var1) {
      int var2 = var0 >>> 20 & 15;
      int var3 = (var1.glow * var2 + 7) / 15 << 4;
      return var3 > 0 && (var0 & 0xFF) < var3 ? var0 & -256 | var3 : var0;
   }

   private static int cached(int var0, boolean var1, PinkWater.Design var2) {
      int var3 = var0 & 16777215 | var2.ordinal() + 1 << 24 | (var1 ? 1073741824 : 0);
      int var4 = var3 * -1640531535 >>> 26 & 63;
      long var5 = CACHE.getOpaque(var4);
      if ((int)(var5 >>> 32) == var3) {
         return (int)var5;
      } else {
         int var7 = water(var0, var1, var2);
         CACHE.setOpaque(var4, (long)var3 << 32 | var7 & 4294967295L);
         return var7;
      }
   }

   static double[] oklab(int var0) {
      double var1 = linear((var0 >> 16 & 0xFF) / 255.0);
      double var3 = linear((var0 >> 8 & 0xFF) / 255.0);
      double var5 = linear((var0 & 0xFF) / 255.0);
      double var7 = Math.cbrt(0.4122214708 * var1 + 0.5363325363 * var3 + 0.0514459929 * var5);
      double var9 = Math.cbrt(0.2119034982 * var1 + 0.6806995451 * var3 + 0.1073969566 * var5);
      double var11 = Math.cbrt(0.0883024619 * var1 + 0.2817188376 * var3 + 0.6299787005 * var5);
      return new double[]{
         0.2104542553 * var7 + 0.793617785 * var9 - 0.0040720468 * var11,
         1.9779984951 * var7 - 2.428592205 * var9 + 0.4505937099 * var11,
         0.0259040371 * var7 + 0.7827717662 * var9 - 0.808675766 * var11
      };
   }

   private static int fromOklab(double var0, double var2, double var4) {
      double[] var6 = new double[3];
      if (!fromOklab(var0, var2, var4, var6)) {
         double var7 = 0.0;
         double var9 = 1.0;

         for (int var11 = 0; var11 < 14; var11++) {
            double var12 = (var7 + var9) * 0.5;
            if (fromOklab(var0, var2 * var12, var4 * var12, var6)) {
               var7 = var12;
            } else {
               var9 = var12;
            }
         }

         fromOklab(var0, var2 * var7, var4 * var7, var6);
      }

      return channel(var6[0]) << 16 | channel(var6[1]) << 8 | channel(var6[2]);
   }

   private static boolean fromOklab(double var0, double var2, double var4, double[] var6) {
      double var7 = var0 + 0.3963377774 * var2 + 0.2158037573 * var4;
      double var9 = var0 - 0.1055613458 * var2 - 0.0638541728 * var4;
      double var11 = var0 - 0.0894841775 * var2 - 1.291485548 * var4;
      var7 = var7 * var7 * var7;
      var9 = var9 * var9 * var9;
      var11 = var11 * var11 * var11;
      var6[0] = 4.0767416621 * var7 - 3.3077115913 * var9 + 0.2309699292 * var11;
      var6[1] = -1.2684380046 * var7 + 2.6097574011 * var9 - 0.3413193965 * var11;
      var6[2] = -0.0041960863 * var7 - 0.7034186147 * var9 + 1.707614701 * var11;
      double var13 = 1.0E-4;
      return var6[0] >= -var13 && var6[0] <= 1.0 + var13 && var6[1] >= -var13 && var6[1] <= 1.0 + var13 && var6[2] >= -var13 && var6[2] <= 1.0 + var13;
   }

   private static int hsv(double var0, double var2, double var4) {
      double var10 = (var0 % 360.0 + 360.0) % 360.0 / 60.0;
      int var12 = (int)Math.floor(var10) % 6;
      double var13 = var10 - Math.floor(var10);
      double var15 = var4 * (1.0 - var2);
      double var17 = var4 * (1.0 - var2 * var13);
      double var19 = var4 * (1.0 - var2 * (1.0 - var13));
      double var6;
      double var8;

      double var21 = switch (var12) {
         case 0 -> {
            var8 = var4;
            var6 = var19;
            yield var15;
         }
         case 1 -> {
            var8 = var17;
            var6 = var4;
            yield var15;
         }
         case 2 -> {
            var8 = var15;
            var6 = var4;
            yield var19;
         }
         case 3 -> {
            var8 = var15;
            var6 = var17;
            yield var4;
         }
         case 4 -> {
            var8 = var19;
            var6 = var15;
            yield var4;
         }
         default -> {
            var8 = var4;
            var6 = var15;
            yield var17;
         }
      };
      return srgbChannel(var8) << 16 | srgbChannel(var6) << 8 | srgbChannel(var21);
   }

   private static double linear(double var0) {
      return var0 <= 0.04045 ? var0 / 12.92 : Math.pow((var0 + 0.055) / 1.055, 2.4);
   }

   private static int channel(double var0) {
      double var2 = clamp(var0, 0.0, 1.0);
      double var4 = var2 <= 0.0031308 ? var2 * 12.92 : 1.055 * Math.pow(var2, 0.4166666666666667) - 0.055;
      return srgbChannel(var4);
   }

   private static int srgbChannel(double var0) {
      return (int)Math.round(clamp(var0, 0.0, 1.0) * 255.0);
   }

   private static int mix(int var0, int var1, float var2) {
      int var3 = Math.round((var0 >> 16 & 0xFF) + ((var1 >> 16 & 0xFF) - (var0 >> 16 & 0xFF)) * var2);
      int var4 = Math.round((var0 >> 8 & 0xFF) + ((var1 >> 8 & 0xFF) - (var0 >> 8 & 0xFF)) * var2);
      int var5 = Math.round((var0 & 0xFF) + ((var1 & 0xFF) - (var0 & 0xFF)) * var2);
      return var0 & 0xFF000000 | var3 << 16 | var4 << 8 | var5;
   }

   private static double wrap(double var0) {
      double var2 = var0 % 360.0;
      if (var2 > 180.0) {
         var2 -= 360.0;
      }

      if (var2 <= -180.0) {
         var2 += 360.0;
      }

      return var2;
   }

   private static double smooth(double var0, double var2, double var4) {
      double var6 = clamp((var4 - var0) / (var2 - var0), 0.0, 1.0);
      return var6 * var6 * (3.0 - 2.0 * var6);
   }

   private static double clamp(double var0, double var2, double var4) {
      return var0 < var2 ? var2 : (var0 > var4 ? var4 : var0);
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

      private Design(String nullxx, int[] nullxxx, int nullxxxx, PinkWater.Fog nullxxxxx, int nullxxxxxx) {
         if (nullxxx.length != PinkWater.BIOMES.length) {
            throw new IllegalArgumentException(nullxx + ": one colour per biome");
         } else {
            this.label = nullxx;
            this.water = new double[nullxxx.length][];

            for (int var8 = 0; var8 < nullxxx.length; var8++) {
               this.water[var8] = PinkWater.oklab(nullxxx[var8]);
            }

            double[] var10 = this.water[PinkWater.index(4159204)];
            double[] var9 = PinkWater.oklab(nullxxxx);
            this.cave = new double[]{var9[0] - var10[0], var9[1] - var10[1], var9[2] - var10[2]};
            this.fog = nullxxxxx;
            this.glow = nullxxxxxx;
         }
      }

      public String label() {
         return this.label;
      }

      public static PinkWater.Design byLabel(String var0) {
         for (PinkWater.Design var4 : values()) {
            if (var4.label.equals(var0)) {
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
