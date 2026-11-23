package net.efn.client.module.impl.combat.bot;

import java.util.List;
import net.efn.client.util.pvp.Rotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
final class PearlFlight {
   private static final double SPEED = 1.5;
   private static final double GRAVITY = 0.03;
   private static final double DRAG = 0.99;
   private static final double WATER_DRAG = 0.8;
   private static final int MAX_TICKS = 100;
   private static final int MAX_DROP = 3;
   private static final double EDGE_MARGIN = 0.8;

   private PearlFlight() {
   }

   static PearlFlight.Landing land(ClientPlayerEntity var0, float var1, float var2) {
      Vec3d var3 = var0.method_33571().method_1023(0.0, 0.1, 0.0);
      Vec3d var4 = var0.method_60478();
      Vec3d var5 = Vec3d.method_1030(var2, var1).method_1021(1.5).method_1031(var4.field_1352, var0.method_24828() ? 0.0 : var4.field_1351, var4.field_1350);
      return fly(var0.method_73183(), var0, var3, var5);
   }

   static PearlFlight.Landing fly(World var0, Entity var1, Vec3d var2, Vec3d var3) {
      Vec3d var4 = var2;
      Vec3d var5 = var3;

      for (int var6 = 1; var6 <= 100; var6++) {
         BlockPos var7 = BlockPos.method_49638(var4);
         if (!var0.method_8398().method_12123(var7.method_10263() >> 4, var7.method_10260() >> 4) || var4.field_1351 < var0.method_31607()) {
            return null;
         }

         boolean var8 = var0.method_8316(var7).method_15767(FluidTags.field_15517);
         var5 = new Vec3d(var5.field_1352, var5.field_1351 - 0.03, var5.field_1350).method_1021(var8 ? 0.8 : 0.99);
         Vec3d var9 = var4.method_1019(var5);
         BlockHitResult var10 = var0.method_17742(new RaycastContext(var4, var9, ShapeType.field_17558, FluidHandling.field_1348, var1));
         if (var10.method_17783() != Type.field_1333) {
            Vec3d var11 = var10.method_17784();
            BlockPos var12 = floorUnder(var0, var11);
            return var12 == null ? null : new PearlFlight.Landing(var11, var12, var6);
         }

         var4 = var9;
      }

      return null;
   }

   static Rotation escape(ClientPlayerEntity var0, List<? extends Entity> var1, double var2) {
      Vec3d var4 = Vec3d.field_1353;

      for (Entity var6 : var1) {
         var4 = var4.method_1019(var6.method_73189());
      }

      var4 = var4.method_1021(1.0 / Math.max(1, var1.size()));
      float var18 = (float)Math.toDegrees(Math.atan2(-(var0.method_23317() - var4.field_1352), var0.method_23321() - var4.field_1350));
      Rotation var19 = null;
      double var7 = -1.0 / 0.0;

      for (int var9 = -4; var9 <= 4; var9++) {
         float var10 = MathHelper.method_15393(var18 + var9 * 22.5F);

         for (float var11 = -45.0F; var11 <= 5.0F; var11 += 7.0F) {
            PearlFlight.Landing var16 = land(var0, var10, var11);
            double var12;
            double var14;
            if (var16 != null
               && safe(var0.method_73183(), var16)
               && !((var14 = nearest(var1, var16.at())) < var2)
               && (var12 = var14 - 0.5 * (var16.at().field_1351 - var16.floor().method_10264() - 1.0) - 0.05 * var16.ticks()) > var7) {
               var7 = var12;
               var19 = new Rotation(var10, var11);
            }
         }
      }

      return var19;
   }

   static Rotation toward(ClientPlayerEntity var0, Vec3d var1, double var2, double var4) {
      Vec3d var6 = var0.method_33571();
      double var7 = Math.hypot(var1.field_1352 - var6.field_1352, var1.field_1350 - var6.field_1350);
      float var9 = (float)Math.toDegrees(Math.atan2(-(var1.field_1352 - var6.field_1352), var1.field_1350 - var6.field_1350));
      double var10 = (var2 + var4) / 2.0;

      for (boolean var15 : new boolean[]{false, true}) {
         double[] var16;
         if (var15) {
            double[] var17 = new double[]{0.0, var10, 0.0};
            var16 = var17;
            var17[2] = -var10;
         } else {
            double[] var29 = new double[]{var10, 0.0};
            var16 = var29;
            var29[1] = -var10;
         }

         for (double var20 : var16) {
            double var22 = var7 + var20;
            if (!(var22 < 1.0)) {
               for (float var27 : new float[]{0.0F, -1.5F, 1.5F, -3.0F, 3.0F}) {
                  Rotation var28 = pitchFor(var0, var6, MathHelper.method_15393(var9 + var27), var22, var15);
                  if (var28 != null && reaches(var0, var28.yaw(), var28.pitch(), var1, var15 ? 0.0 : var2, var4)) {
                     return var28;
                  }
               }
            }
         }
      }

      return null;
   }

   private static Rotation pitchFor(ClientPlayerEntity var0, Vec3d var1, float var2, double var3, boolean var5) {
      float var6 = 0.0F / 0.0F;
      float var7 = var5 ? -88.0F : 15.0F;
      float var8 = var5 ? -40.0F : -60.0F;
      float var9 = var5 ? 4.0F : -5.0F;

      for (float var10 = var7; var5 ? var10 <= var8 : var10 >= var8; var10 += var9) {
         if (!(carry(var0, var1, var2, var10) < var3)) {
            if (Float.isNaN(var6)) {
               return null;
            }

            float var11 = var6;
            float var12 = var10;

            for (int var13 = 0; var13 < 8; var13++) {
               float var14 = (var11 + var12) / 2.0F;
               if (carry(var0, var1, var2, var14) >= var3) {
                  var12 = var14;
               } else {
                  var11 = var14;
               }
            }

            return new Rotation(var2, (var11 + var12) / 2.0F);
         }

         var6 = var10;
      }

      return null;
   }

   private static double carry(ClientPlayerEntity var0, Vec3d var1, float var2, float var3) {
      PearlFlight.Landing var4 = land(var0, var2, var3);
      return var4 == null ? 1.7976931348623157E308 : Math.hypot(var4.at().field_1352 - var1.field_1352, var4.at().field_1350 - var1.field_1350);
   }

   static boolean escapes(ClientPlayerEntity var0, float var1, float var2, List<? extends Entity> var3, double var4) {
      PearlFlight.Landing var6 = land(var0, var1, var2);
      return var6 != null && safe(var0.method_73183(), var6) && nearest(var3, var6.at()) >= var4;
   }

   static boolean reaches(ClientPlayerEntity var0, float var1, float var2, Vec3d var3, double var4, double var6) {
      PearlFlight.Landing var8 = land(var0, var1, var2);
      if (var8 != null && safe(var0.method_73183(), var8)) {
         double var9 = horizontal(var8.at(), var3);
         return var9 >= var4 && var9 <= var6 && Math.abs(var8.at().field_1351 - var3.field_1351) <= 3.0;
      } else {
         return false;
      }
   }

   static int flightTicks(double var0) {
      if (var0 <= 6.0) {
         return 5;
      } else if (var0 <= 10.0) {
         return 5 + (int)Math.round((var0 - 6.0) / 2.0);
      } else if (var0 <= 14.0) {
         return 7 + (int)Math.round((var0 - 10.0) * 0.75);
      } else {
         return var0 <= 20.0 ? 10 + (int)Math.round((var0 - 14.0) * 0.83) : 15 + (int)Math.round((var0 - 20.0) * 0.8);
      }
   }

   static double nearest(List<? extends Entity> var0, Vec3d var1) {
      double var2 = 1.7976931348623157E308;

      for (Entity var5 : var0) {
         var2 = Math.min(var2, var5.method_73189().method_1022(var1));
      }

      return var2;
   }

   private static double horizontal(Vec3d var0, Vec3d var1) {
      return Math.hypot(var0.field_1352 - var1.field_1352, var0.field_1350 - var1.field_1350);
   }

   private static BlockPos floorUnder(World var0, Vec3d var1) {
      BlockPos var2 = BlockPos.method_49637(var1.field_1352, var1.field_1351 + 0.05, var1.field_1350);

      for (int var3 = 0; var3 <= 4; var3++) {
         BlockPos var4 = var2.method_10087(var3);
         if (!var0.method_8320(var4).method_26220(var0, var4).method_1110()) {
            return var4;
         }
      }

      return null;
   }

   private static boolean safe(World var0, PearlFlight.Landing var1) {
      BlockPos var2 = var1.floor();
      if (var1.at().field_1351 - (var2.method_10264() + 1.0) > 3.0) {
         return false;
      } else {
         for (double[] var6 : new double[][]{{0.8, 0.0}, {-0.8, 0.0}, {0.0, 0.8}, {0.0, -0.8}}) {
            BlockPos var7 = floorUnder(var0, var1.at().method_1031(var6[0], 0.0, var6[1]));
            if (var7 == null || var7.method_10264() != var2.method_10264()) {
               return false;
            }
         }

         if (!harmful(var0.method_8320(var2)) && !var0.method_8320(var2).method_27852(Blocks.field_10092)) {
            for (int var8 = 1; var8 <= 2; var8++) {
               BlockPos var9 = var2.method_10086(var8);
               BlockState var10 = var0.method_8320(var9);
               if (harmful(var10) || !var10.method_26220(var0, var9).method_1110() || !var0.method_8316(var9).method_15769()) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private static boolean harmful(BlockState var0) {
      return var0.method_27852(Blocks.field_10164)
         || var0.method_26164(BlockTags.field_21952)
         || var0.method_27852(Blocks.field_10029)
         || var0.method_27852(Blocks.field_16999)
         || var0.method_27852(Blocks.field_10343)
         || var0.method_27852(Blocks.field_27879)
         || var0.method_27852(Blocks.field_10606);
   }

   @Environment(EnvType.CLIENT)
   record Landing(Vec3d at, BlockPos floor, int ticks) {
   }
}
