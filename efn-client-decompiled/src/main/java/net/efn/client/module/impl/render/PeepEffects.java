package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class PeepEffects {
   private static final float SPOT_BASE = -7.0F;
   private static final float SPOT_LENGTH = 95.0F;
   private static final float SPOT_WIDTH = 30.0F;
   private static final float SPOT_SIDE = 0.085F;
   private static final int BLUE = 9074943;
   private static final int MAGENTA = 16736192;
   private static final int LILAC = 13995263;
   private static final int COOL = 8029951;
   static final int LIGHTNING_LANES = 2;
   static final int HEARTS = 20;
   private static final int METEOR_LANES = 15;
   private static final int RAIN_DROPS = 330;
   private static final int BEAMS = 8;
   private static final int SPARKS = 135;
   private static final int PETALS = 38;
   private static final int WISPS = 21;
   private static final int MOTES = 55;
   private static final int GLITTER = 84;
   private static final int SPOT_DUST = 44;
   private static final int SPIRITS = 12;
   private static final int FEATHERS = 22;
   private static final int METEOR_TRIES = 4;
   private static final int METEOR_SLOTS = 0;
   private static final int PETAL_SLOTS = 15;
   private static final int HEART_SLOTS = 53;
   private static final int WISP_SLOTS = 73;
   private static final int MOTE_SLOTS = 94;
   private static final int GLITTER_SLOTS = 149;
   private static final int SPIRIT_SLOTS = 233;
   private static final int FEATHER_SLOTS = 245;
   private static final float IN_VIEW = 0.8F;
   private static final float VIEW_SPREAD = 0.375F;
   private static final float CRY_ALONE_RAIN = 2.2F;
   private static final float RAIN_SLANT = 0.06F;
   private static final int BEAM_SAMPLES = 8;
   private static final float[] SCRATCH = new float[8];
   private static final float[] OTHER = new float[8];
   private static final float[] ARC = new float[9];
   private static final float[] AT = new float[2];
   private static final float METEOR_GAP = 5.0F;

   private PeepEffects() {
   }

   static void skyLight(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      float var5 = peepSky.w(PeepSong.THE_BRIGHTSIDE) * f;
      if (var5 > 0.003F) {
         sunrise(peepSky, skyPaint, var5, g);
      }

      float var4;
      if ((var4 = peepSky.w(PeepSong.SUNLIGHT_ON_YOUR_SKIN) * f) > 0.003F) {
         sunlight(peepSky, skyPaint, var4, g);
      }
   }

   private static void sunrise(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      float var4 = peepSky.age(PeepSong.THE_BRIGHTSIDE);
      float var5 = PeepSky.smootherstep(MathHelper.clamp(var4 / 14.0F, 0.0F, 1.0F));
      float var6 = MathHelper.lerp(var5, 0.28F, 0.7F) + 0.02F * MathHelper.sin(g * 0.25F);
      float var7 = 0.72F;
      float var8 = var7 - 0.16F;
      int var9 = PeepSky.tint(0.36F * f, 16743088);
      int var10 = PeepSky.tint(0.0F, 16743088);

      for (int var11 = 0; var11 < 64; var11++) {
         float var12 = var11 / 64.0F;
         float var13 = (var11 + 1) / 64.0F;
         float var14 = PeepSky.smooth(0.06F, 0.0F, Math.abs(PeepSky.wrapDelta(var12 + 0.0078125F)));
         int var15 = PeepSky.tint(0.28F * f * (1.0F - 0.5F * var14), 16761963);
         skyPaint.gradient(true, var12, var13, var7 - var6, var8, var10, var9);
         skyPaint.gradient(true, var12, var13, var8, var7 + 0.04F, var9, var15);
      }

      float var16 = MathHelper.lerp(PeepSky.smootherstep(MathHelper.clamp(var4 / 18.0F, 0.0F, 1.0F)), -3.0F, 9.0F);
      SkyImage var17 = peepSky.art[7];

      for (int var18 = 0; var18 < 3; var18++) {
         float var20 = (var18 - 1) * 0.55F + 0.08F * MathHelper.sin(g * 0.12F + var18 * 2.0F);
         float var22 = 0.085F * (0.8F + 0.2F * MathHelper.sin(g * 0.3F + var18 * 1.9F)) * (1.0F - 0.3F * peepSky.hazeAt(y(var16)));
         beam(
            skyPaint,
            var17,
            0.0F,
            var16 - 1.0F,
            var20,
            var18 == 0 ? 36.0F : (var18 == 2 ? 44.0F : 40.0F),
            var18 == 0 ? 12.0F : (var18 == 2 ? 16.0F : 14.0F),
            16763024,
            var22 * f
         );
      }

      float var19 = y(var16);
      float var21 = clear(peepSky, 0.0F, var16);
      skyPaint.glow(0.0F, var19, units(34.0F), 16752250, 0.23F * f);
      skyPaint.glow(0.0F, var19, units(13.0F), 16763024, 0.2F * f * (0.4F + 0.6F * var21));
      skyPaint.glow(0.0F, var19, units(3.6F), 16760468, 0.2F * f * (0.1F + 0.9F * var21) * core(peepSky, var19));
   }

   private static void sunlight(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      float var4 = peepSky.stageU + 0.05F;
      float var5 = 64.0F;
      skyPaint.glow(var4, y(50.0F), units(64.0F), 16736150, 0.13F * f);
      skyPaint.glow(var4, y(40.0F), units(70.0F), 16756848, 0.07F * f);
      skyPaint.glow(var4, y(var5 - 8.0F), units(34.0F), 16752762, 0.2F * f);
      skyPaint.glow(var4, y(var5), units(14.0F), 16762976, 0.32F * f);
      skyPaint.glow(var4, y(var5), units(5.0F), 16765562, 0.3F * f * core(peepSky, y(var5)));
      SkyImage var6 = peepSky.art[14];

      for (int var7 = 0; var7 < 5; var7++) {
         float var8 = 3.1415927F + (var7 - 2.0F) * 0.3F + 0.07F * MathHelper.sin(g * 0.17F + var7 * 1.3F);
         float var9 = 62.0F + 8.0F * PeepSky.hash01(var7 * 3 + 1);
         float var10 = 15.0F + 6.0F * PeepSky.hash01(var7 * 3 + 2);
         float var11 = 0.8F + 0.2F * MathHelper.sin(g * 0.31F + var7 * 1.7F);
         float var12 = var11 * beamClear(peepSky, var4, var5 + 2.0F, var8, var9, var10) * f;
         beam(skyPaint, var6, var4, var5 + 2.0F, var8, var9, var10, 16749704, 0.17F * var12);
         beam(skyPaint, var6, var4, var5 + 2.0F, var8, var9 * 0.45F, var10 * 0.7F, 14721070, 0.62F * var12, false);
         beam(skyPaint, var6, var4, var5 + 2.0F, var8, var9 * 0.45F, var10 * 0.6F, 16761418, 0.5F * var12);
      }
   }

   static void behind(PeepSky peepSky, SkyPaint skyPaint, float f) {
      float var11 = peepSky.alpha;
      float var12 = peepSky.w(PeepSong.CRY_ALONE) * var11;
      if (var12 > 0.003F) {
         lonely(peepSky, skyPaint, var12, f);
      }

      float var10;
      if ((var10 = peepSky.w(PeepSong.HELLBOY) * var11) > 0.003F) {
         float var13 = 0.85F + 0.15F * (0.5F + 0.5F * MathHelper.sin(f * 6.2831855F * 0.7F));
         horizonLine(skyPaint, 1.5F, 6.0F, 16724821, 0.17F * var13 * var10);
      }

      float var9;
      if ((var9 = peepSky.w(PeepSong.FOUR_GOLD_CHAINS) * var11) > 0.003F) {
         horizonLine(skyPaint, 2.0F, 7.0F, 16765562, 0.22F * var9);
      }

      float var8;
      if ((var8 = peepSky.w(PeepSong.STAR_SHOPPING) * var11) > 0.003F) {
         shootingStars(peepSky, skyPaint, var8, f);
      }

      float var7;
      if ((var7 = peepSky.w(PeepSong.BEAMER_BOY) * var11) > 0.003F) {
         concertBeams(peepSky, skyPaint, var7, f);
      }

      float var6;
      if ((var6 = peepSky.w(PeepSong.SPOTLIGHT) * var11) > 0.003F) {
         spotlight(peepSky, skyPaint, var6, f);
      }

      float var5;
      if ((var5 = peepSky.w(PeepSong.AWFUL_THINGS) * var11) > 0.003F) {
         storm(peepSky, skyPaint, var5, f);
      }

      float var4;
      if ((var4 = peepSky.w(PeepSong.WITCHBLADES) * var11) > 0.003F) {
         mist(skyPaint, peepSky.art[10], 5, f * 0.004F, -3.0F, 18.0F, 10108104, 0.2F * var4, true);
         mist(skyPaint, peepSky.art[10], 4, -f * 0.0025F + 0.3F, 4.0F, 34.0F, 16736200, 0.38F * var4, true);
      }

      float var3;
      if ((var3 = peepSky.w(PeepSong.GHOST_BOY) * var11) > 0.003F) {
         mist(skyPaint, peepSky.art[10], 6, f * 0.0015F, -4.0F, 11.0F, 12356338, 0.28F * var3, false);
         mist(skyPaint, peepSky.art[10], 5, -f * 0.001F + 0.5F, 0.0F, 16.0F, 10516706, 0.1F * var3, true);
         mist(skyPaint, peepSky.art[10], 5, f * 0.0012F + 0.2F, -2.0F, 9.0F, 14715600, 0.14F * var3, true);
      }
   }

   private static void lonely(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      mist(skyPaint, peepSky.art[10], 6, g * 0.0012F, -2.0F, 8.0F, 2755106, 0.35F * f, false);
   }

   private static void shootingStars(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      SkyImage var4 = peepSky.art[0];
      float[] var5 = SCRATCH;

      for (int var6 = 0; var6 < 2; var6++) {
         for (int var7 = 0; var7 < 15; var7++) {
            boolean var10 = var7 % 4 == 3;
            float var11 = 3.2F + 2.6F * PeepSky.hash01(var7 * 3 + 1);
            float var12 = g / var11 + PeepSky.hash01(var7 * 3 + 2);
            int var9;
            float var13 = (var12 - (var9 = MathHelper.floor(var12))) * var11;
            float var8;
            if (!(var13 > (var8 = (1.1F + 0.6F * PeepSky.hash01(var7, var9, 1)) * (var10 ? 1.3F : 1.0F)))) {
               float var14 = var13 / var8;
               int var15 = 0 + var7;
               int var16 = peepSky.picked(var15, var9);
               float var17 = peepSky.anchor(var15, var9);
               if (var16 < 0) {
                  var16 = 4;

                  for (int var18 = 0; var18 < 4 && var16 == 4; var18++) {
                     if (meteorClear(peepSky, var7, var9 * 4 + var18, var10, var17, var8, var13, g, var5)) {
                        var16 = var18;
                     }
                  }

                  peepSky.pick(var15, var9, var16);
               }

               if (var16 < 4) {
                  int var31 = var9 * 4 + var16;
                  meteor(var7, var31, var10, var17, 1.0F - (float)Math.pow(1.0F - var14, 1.7), var5);
                  float var19 = var5[0];
                  float var20 = var5[1];
                  float var21 = var5[2];
                  float var22 = var5[3];
                  float var23 = var5[4];
                  float var24 = var5[5];
                  float var25 = PeepSky.smooth(0.0F, 0.1F, var14)
                     * (1.0F - PeepSky.smooth(0.55F, 1.0F, var14))
                     * PeepSky.smooth(3.0F, 10.0F, var20)
                     * f
                     * clear(peepSky, var19, var20)
                     * PeepSky.smooth(0.5F, 1.0F, peepSky.alpha);
                  if (!(var25 <= 0.003F)) {
                     if (var6 == 0) {
                        meteor(var7, var31, var10, var17, 1.0F, OTHER);
                        float var26 = var10 ? 34.0F : (peepSky.lyricBox(OTHER[0], OTHER[1], 6.0F, 6.0F) > 0.05F ? 20.0F : 26.0F);
                        float var27 = Math.min(var23 * var24, var26) * (0.6F + 0.4F * (1.0F - var14));
                        float var28 = var20 - var22 * var27 * 0.4F;
                        float var29 = var19 - du(var21 * var27 * 0.4F, var28);
                        float var30 = (float)Math.atan2(-var22, var21);
                        skyPaint.sprite(var4, var29, y(var28), units(var27), units(4.5F), var30, PeepSky.tint(0.42F * var25, 16732080), true);
                        skyPaint.sprite(var4, var29, y(var28), units(var27), units(1.2F), var30, PeepSky.tint(0.5F * var25, 16756960), true);
                     } else {
                        skyPaint.glow(var19, y(var20), units(1.45F), 16756960, 0.38F * var25 * core(peepSky, y(var20)));
                        skyPaint.glow(var19, y(var20), units(7.0F), 16732080, 0.3F * var25);
                     }
                  }
               }
            }
         }
      }
   }

   private static void meteor(int i, int j, boolean bl, float f, float g, float[] fs) {
      float var6 = f + 0.04F + (PeepSky.hash01(i, j, 2) - 0.5F) * 0.36F;
      float var7 = 25.0F + 33.0F * PeepSky.hash01(i, j, 3);
      float var8 = (16.0F + 22.0F * PeepSky.hash01(i, j, 5)) * 0.017453292F;
      float var9 = bl ? 40.0F + 15.0F * PeepSky.hash01(i, j, 6) : 22.0F + 16.0F * PeepSky.hash01(i, j, 6);
      float var10 = -MathHelper.cos(var8);
      float var11 = -MathHelper.sin(var8);
      fs[1] = var7 + var11 * var9 * g;
      fs[0] = var6 + du(var10 * var9 * g, fs[1]);
      fs[2] = var10;
      fs[3] = var11;
      fs[4] = var9;
      fs[5] = g;
   }

   private static boolean meteorClear(PeepSky peepSky, int i, int j, boolean bl, float f, float g, float h, float k, float[] fs) {
      for (int var9 = 1; var9 <= 6; var9++) {
         float var11 = 0.08F + 0.84F * (var9 - 1) / 5.0F;
         meteor(i, j, bl, f, 1.0F - (float)Math.pow(1.0F - var11, 1.7), fs);
         float var12 = var11 * g - h;
         if (crowded(peepSky, i, k + var12, fs[0], fs[1])) {
            return false;
         }

         if (peepSky.lyricBox(fs[0], fs[1], 2.5F, 2.5F) > 0.02F) {
            return false;
         }

         float var10;
         if (!(fs[1] < 3.0F) && peepSky.clearOfTattoos(fs[0] - (var10 = 0.02F * var12), fs[1], 1.5F) < 0.9F) {
            return false;
         }
      }

      return true;
   }

   private static boolean crowded(PeepSky peepSky, int i, float f, float g, float h) {
      for (int var5 = 0; var5 < 15; var5++) {
         if (var5 != i) {
            boolean var8 = var5 % 4 == 3;
            float var9 = 3.2F + 2.6F * PeepSky.hash01(var5 * 3 + 1);
            float var10 = f / var9 + PeepSky.hash01(var5 * 3 + 2);
            int var7;
            float var11 = (var10 - (var7 = MathHelper.floor(var10))) * var9;
            float var6;
            if (!(var11 > (var6 = (1.1F + 0.6F * PeepSky.hash01(var5, var7, 1)) * (var8 ? 1.3F : 1.0F)))) {
               int var12 = peepSky.picked(0 + var5, var7);
               float var13 = peepSky.anchored(0 + var5, var7);
               if (var12 >= 0 && var12 < 4 && !Float.isNaN(var13)) {
                  meteor(var5, var7 * 4 + var12, var8, var13, 1.0F - (float)Math.pow(1.0F - var11 / var6, 1.7), OTHER);
                  float var14 = PeepSky.wrapDelta(OTHER[0] - g) * 360.0F * MathHelper.cos(h * 0.017453292F);
                  float var15 = OTHER[1] - h;
                  if (var14 * var14 + var15 * var15 < 25.0F) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private static void concertBeams(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      SkyImage var6 = peepSky.art[7];
      SkyImage var7 = peepSky.art[14];

      for (int var5 = 0; var5 < 8; var5++) {
         float var4 = (var5 + 0.5F) / 8.0F + 0.01F * MathHelper.sin(g * 0.05F + var5);

         for (int var8 = 0; var8 < 2; var8++) {
            float var9 = MathHelper.sin(g * 0.28F * (1.0F + 0.12F * var5) + var5 * 1.9F + var8 * 0.7F);
            float var10 = (var8 == 0 ? 1.0F : -1.0F) * (0.12F + 0.5F * var9);
            boolean var11 = (var5 + var8) % 2 == 0;
            float var12 = var11 ? 24.0F : 26.0F;
            float var13 = (0.78F + 0.22F * MathHelper.sin(g * 1.1F + var5 * 2.0F + var8)) * f * beamClear(peepSky, var4, -6.0F, var10, 82.0F, var12);
            beam(skyPaint, var7, var4, -6.0F, var10, 70.0F, var12 * 0.8F, var11 ? 16736192 : 8029951, (var11 ? 0.21F : 0.24F) * var13);
            beam(skyPaint, var6, var4, -6.0F, var10, 82.0F, var12, var11 ? 16736192 : 9074943, 0.33F * var13);
            beam(skyPaint, var6, var4, -6.0F, var10, 80.0F, 3.0F, var11 ? 16769268 : 14866175, 0.2F * var13);
         }
      }

      horizonLine(skyPaint, 1.0F, 7.0F, 9074943, 0.08F * f);

      for (int var15 = 0; var15 < 8; var15++) {
         float var14 = (var15 + 0.5F) / 8.0F + 0.01F * MathHelper.sin(g * 0.05F + var15);
         float var16 = f * clear(peepSky, var14, 2.5F);
         skyPaint.glow(var14, y(2.5F), units(9.0F), var15 % 2 == 0 ? 16736192 : 9074943, 0.3F * var16);
         skyPaint.glow(var14, y(2.5F), units(3.9F), var15 % 2 == 0 ? 16770806 : 14866175, 0.36F * var16);
      }
   }

   private static void spotlight(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      SkyImage var4 = peepSky.art[14];
      float var5 = spotTilt(g);
      float var6 = peepSky.stageU + 0.085F;
      float var7 = f * (0.3F + 0.7F * beamClear(peepSky, var6, -7.0F, var5, 95.0F, 30.0F));
      beam(skyPaint, var4, var6, -7.0F, var5, 95.0F, 54.0F, 16732080, 0.13F * var7);
      beam(skyPaint, var4, var6, -7.0F, var5, 95.0F, 30.0F, 15746463, 0.45F * var7);
      beam(skyPaint, var4, var6, -7.0F, var5, 52.25F, 13.5F, 16751318, 0.2F * var7);
      arc(var6, -7.0F, var5);
      onArc(8.5F / Math.max(0.3F, MathHelper.cos(var5)), 0.0F);
      float var8 = AT[0];
      float var9 = AT[1];
      float var10 = f * (0.25F + 0.75F * clear(peepSky, var8, var9));
      skyPaint.glow(var8, y(var9), units(13.0F), 16766704, 0.3F * var10);
      skyPaint.glow(var8, y(var9), units(5.8F), 16759008, 0.48F * var10);

      for (int var11 = 0; var11 < 44; var11++) {
         float var12 = MathHelper.fractionalPart(PeepSky.hash01(var11 * 5 + 1) + g * 3.0F / 58.9F);
         float var13 = 95.0F * (0.08F + 0.62F * var12);
         float var14 = coneHalf(var13 / 95.0F);
         float var15 = PeepSky.hash01(var11 * 5 + 2) - 0.5F;
         float var16 = var15 * var14 + 0.5F * MathHelper.sin(g * 0.45F + var11 * 2.0F);
         onArc(var13, var16);
         float var17 = AT[0];
         float var18 = AT[1];
         float var19 = 0.5F + 0.5F * (float)Math.pow(0.5F + 0.5F * MathHelper.sin(g * (0.7F + 0.5F * PeepSky.hash01(var11 * 5 + 3)) + var11 * 1.7F), 2.0);
         float var20 = PeepSky.smooth(0.0F, 0.12F, var12) * (1.0F - PeepSky.smooth(0.8F, 1.0F, var12));
         float var21 = (1.0F - 0.6F * Math.min(1.0F, Math.abs(var15) * 2.0F)) * (1.0F - PeepSky.smooth(0.3F, 0.5F, Math.abs(var15)));
         skyPaint.glow(
            var17, y(var18), units(0.4F + 0.4F * PeepSky.hash01(var11 * 5 + 4)), 16765164, 0.55F * var19 * var21 * var20 * f * clear(peepSky, var17, var18)
         );
      }
   }

   static float spotTilt(float f) {
      return -0.05F + 0.4F * MathHelper.sin(f * 0.2F + 0.5F);
   }

   static float spotlightOn(PeepSky peepSky, float f, float g) {
      arc(peepSky.stageU + 0.085F, -7.0F, spotTilt(peepSky.time));
      float var3 = alongArc(f, g);
      float var4 = acrossArc(f, g);
      if (!(var3 <= 0.0F) && !(var3 >= 95.0F)) {
         float var5 = var3 / 95.0F;
         float var6 = coneHalf(var5);
         return (1.0F - PeepSky.smooth(var6 * 0.55F, var6 * 1.1F, var4)) * (1.0F - PeepSky.smooth(0.75F, 1.0F, var5));
      } else {
         return 0.0F;
      }
   }

   private static float coneHalf(float f) {
      return (0.05F + 0.84999996F * f) * 30.0F * 0.5F;
   }

   private static void storm(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      SkyImage var4 = peepSky.art[10];
      mist(skyPaint, var4, 6, g * 0.002F, -2.0F, 24.0F, 1967638, 0.75F * f, false);
      mist(skyPaint, var4, 5, -g * 0.0013F + 0.4F, 10.0F, 34.0F, 2755104, 0.75F * f, false);
      mist(skyPaint, var4, 6, g * 0.002F + 0.37F, 2.0F, 20.0F, 5902140, 0.18F * f, true);
      mist(skyPaint, var4, 5, -g * 0.0016F + 0.15F, 16.0F, 52.0F, 9050714, 0.15F * f, true);
      float[] var5 = SCRATCH;

      for (int var6 = 0; var6 < 2; var6++) {
         for (int var7 = 0; var7 < 2; var7++) {
            float var8 = lightning(var7, g, var5) * f;
            if (!(var8 <= 0.003F)) {
               float var9 = peepSky.stageU + var5[0];
               float var10 = var5[1];
               var8 *= clear(peepSky, var9, var10 * 0.5F);
               if (var6 == 0) {
                  skyPaint.glow(var9, y(var10 * 0.7F), units(var10 * 2.4F), 9050714, 0.2F * var8);
                  skyPaint.glow(var9, y(var10 * 0.55F), units(var10 * 1.5F), 12720235, 0.26F * var8);
                  skyPaint.glow(var9, y(var10 * 0.45F), units(var10 * 0.6F), 16732080, 0.16F * var8);
               } else {
                  skyPaint.sprite(
                     peepSky.art[var5[2] < 0.5F ? 11 : 12],
                     var9,
                     y(var10 * 0.5F - 0.5F),
                     units(var10 * 0.5F),
                     units(var10 + 1.0F),
                     var5[3],
                     PeepSky.tint(0.6F * var8, 16777215),
                     true
                  );
               }
            }
         }
      }
   }

   static float lightning(int i, float f, float[] fs) {
      float var4 = 18.0F + 12.0F * PeepSky.hash01(i * 7 + 1);
      float var5 = f / var4 + PeepSky.hash01(i * 7 + 2);
      int var3;
      float var6 = (var5 - (var3 = MathHelper.floor(var5))) * var4;
      if (var6 > 2.6F) {
         return 0.0F;
      } else {
         fs[0] = (PeepSky.hash01(i, var3, 1) < 0.5F ? -1.0F : 1.0F) * (0.07F + 0.14F * PeepSky.hash01(i, var3, 5));
         fs[1] = 16.0F + 12.0F * PeepSky.hash01(i, var3, 2);
         fs[2] = PeepSky.hash01(i, var3, 3);
         fs[3] = (PeepSky.hash01(i, var3, 4) - 0.5F) * 0.25F;
         return Math.min(1.0F, flicker(var6) + 0.55F * flicker(var6 - 0.25F) + 0.3F * flicker(var6 - 0.65F));
      }
   }

   private static float flicker(float f) {
      if (f < 0.0F) {
         return 0.0F;
      } else {
         return f < 0.2F ? PeepSky.smooth(0.0F, 0.2F, f) : (float)Math.exp(-(f - 0.2F) / 0.24F);
      }
   }

   static void front(PeepSky peepSky, SkyPaint skyPaint, float f) {
      float var11 = peepSky.alpha;
      float var12 = peepSky.w(PeepSong.CRY_ALONE);
      float var13 = (peepSky.w(PeepSong.CRYBABY) + 2.2F * var12) * var11;
      if (var13 > 0.003F) {
         rain(peepSky, skyPaint, var13, 1.0F + 0.5F * var12, var12, f);
      }

      float var10;
      if ((var10 = peepSky.w(PeepSong.LIFE_IS_BEAUTIFUL) * var11) > 0.003F) {
         petals(peepSky, skyPaint, var10, f);
      }

      float var9;
      if ((var9 = peepSky.w(PeepSong.SAVE_THAT_SHIT) * var11) > 0.003F) {
         hearts(peepSky, skyPaint, var9, f);
      }

      float var8;
      if ((var8 = peepSky.w(PeepSong.FALLING_DOWN) * var11) > 0.003F) {
         feathers(peepSky, skyPaint, var8, f);
      }

      float var7;
      if ((var7 = peepSky.w(PeepSong.HELLBOY) * var11) > 0.003F) {
         sparks(peepSky, skyPaint, var7, f);
      }

      float var6;
      if ((var6 = peepSky.w(PeepSong.GHOST_GIRL) * var11) > 0.003F) {
         ghosts(peepSky, skyPaint, var6, f);
      }

      float var5;
      if ((var5 = peepSky.w(PeepSong.GHOST_BOY) * var11) > 0.003F) {
         spirits(peepSky, skyPaint, var5, f);
      }

      float var4;
      if ((var4 = peepSky.w(PeepSong.SUNLIGHT_ON_YOUR_SKIN) * var11) > 0.003F) {
         motes(peepSky, skyPaint, var4, f);
      }

      float var3;
      if ((var3 = peepSky.w(PeepSong.FOUR_GOLD_CHAINS) * var11) > 0.003F) {
         glitter(peepSky, skyPaint, var3, f);
      }
   }

   private static void rain(PeepSky peepSky, SkyPaint skyPaint, float f, float g, float h, float i) {
      SkyImage var6 = peepSky.art[1];
      float var7 = f / 2.2F;
      float var8 = Math.min(1.0F, f) * g;
      int var9 = PeepSky.mix(16747216, 16736184, 0.5F * h);
      float var10 = MathHelper.sin(0.05999999865889549);
      float var11 = MathHelper.cos(0.05999999865889549);
      float var12 = MathHelper.lerp(h, 0.28F, 0.22F);

      for (int var13 = 0; var13 < 2; var13++) {
         int var14 = var13 == 0 ? 1 : 3;

         for (int var15 = var13; var15 < 330; var15 += var14) {
            float var17 = MathHelper.clamp((var7 - PeepSky.hash01(var15 * 5 + 1)) * 10.0F, 0.0F, 1.0F);
            if (!(var17 <= 0.0F)) {
               float var18 = 2.2F + 1.2F * PeepSky.hash01(var15 * 5 + 2);
               float var19 = i / var18 + PeepSky.hash01(var15 * 5 + 3);
               int var20 = MathHelper.floor(var19);
               float var21 = var19 - var20;
               float var22 = PeepSky.hash01(var15 * 5 + 4);
               float var23 = MathHelper.lerp(var21, 78.0F, -2.0F);
               float var24 = PeepSky.hash01(var15, var20, 7) + du(-(78.0F - var23) * 0.06F, var23);
               float var25 = PeepSky.smooth(0.0F, 0.08F, var21) * PeepSky.smooth(2.0F, 10.0F, var23) * var17 * clear(peepSky, var24, var23);
               if (!(var25 <= 0.003F)) {
                  var25 *= var12 + (1.0F - var12) * PeepSky.smooth(0.25F, 1.0F, peepSky.clearOfTattoos(var24, var23, 0.4F));
                  boolean var26 = var22 > 0.85F - 0.08F * h;
                  float var16 = var26 ? MathHelper.lerp(h, 3.0F + 3.0F * var22, 7.5F) : 3.0F + 3.0F * var22;
                  if (var26) {
                     var25 *= 1.0F + 0.3F * h;
                  }

                  if (var13 == 0) {
                     skyPaint.sprite(
                        var6,
                        var24,
                        y(var23),
                        units(0.35F + 0.2F * var22),
                        units(var16),
                        0.06F,
                        PeepSky.tint((0.4F + 0.38F * var22) * var25 * var8, var9),
                        true
                     );
                  } else {
                     float var28 = var23 - 0.35F * var16 * var11;
                     skyPaint.glow(var24 + du(-0.35F * var16 * var10, var28), y(var28), units(0.7F), 16754397, 0.22F * var25 * var8);
                  }
               }
            }
         }
      }
   }

   private static void petals(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      for (int var4 = 0; var4 < 2; var4++) {
         for (int var5 = 0; var5 < 38; var5++) {
            float var6 = PeepSky.hash01(var5 * 7 + 1);
            float var7 = 16.0F + 8.0F * var6;
            float var8 = g / var7 + PeepSky.hash01(var5 * 7 + 2);
            int var9 = MathHelper.floor(var8);
            float var10 = var8 - var9;
            float var11 = PeepSky.hash01(var5 * 7 + 3);
            float var12 = MathHelper.lerp(var10, 76.0F, 2.0F);
            float var13 = around(peepSky, 15 + var5, var9, PeepSky.hash01(var5, var9, 9))
               + du(3.5F * MathHelper.sin(g * 0.55F * (0.8F + 0.4F * var6) + var5 * 1.3F), var12);
            float var14 = 2.2F + 1.6F * var11;
            float var15 = PeepSky.smooth(0.0F, 0.07F, var10)
               * PeepSky.smooth(3.0F, 11.0F, var12)
               * f
               * clearAround(peepSky, var13, var12)
               * peepSky.clearOfTattoos(var13, var12, var14 * 0.4F);
            if (!(var15 <= 0.003F)) {
               if (var4 == 0) {
                  skyPaint.glow(var13, y(var12), units(var14 * 1.1F), 16732080, 0.1F * var15 * PeepSky.smooth(6.0F, 13.0F, var12));
               } else {
                  float var16 = MathHelper.cos(g * (0.7F + 0.5F * var6) + var5);
                  float var17 = 0.42F + 0.58F * Math.abs(var16);
                  float var18 = var6 * 6.2831855F + g * (0.22F + 0.3F * var11) * (var5 % 2 == 0 ? 1.0F : -1.0F) + 0.6F * MathHelper.sin(g * 0.7F + var5);
                  int var19 = PeepSky.mix(var16 < 0.0F ? 16765668 : 16770800, 15916262, 0.4F * (1.0F - var11));
                  var19 = PeepSky.mix(var19, 16740264, 1.0F - var15);
                  float var20 = PeepSky.smooth(0.35F, 0.75F, var15);
                  SkyImage var21 = peepSky.art[var5 % 3 == 0 ? 15 : 2];
                  float var22 = units(var14 * 0.8F * var17);
                  float var23 = units(var14);
                  skyPaint.sprite(var21, var13, y(var12), var22, var23, var18, PeepSky.tint(0.9F * var20, var19), false);
                  skyPaint.sprite(var21, var13, y(var12), var22, var23, var18, PeepSky.tint(0.1F * var20, 16777215), true);
               }
            }
         }
      }
   }

   static void heart(int i, float f, float[] fs) {
      float var3 = PeepSky.hash01(i * 11 + 1);
      float var4 = 10.0F + 5.0F * var3;
      float var5 = f / var4 + PeepSky.hash01(i * 11 + 2);
      int var6 = MathHelper.floor(var5);
      float var7 = var5 - var6;
      boolean var8 = PeepSky.hash01(i, var6, 5) < 0.18F;
      fs[1] = MathHelper.lerp(var7, 76.0F, 3.0F);
      fs[0] = spread(PeepSky.hash01(i, var6, 13)) + du(2.4F * MathHelper.sin(f * 0.6F + i * 1.7F), fs[1]);
      fs[2] = var7 * var4;
      fs[3] = var8 ? fs[2] - (0.26F + 0.3F * PeepSky.hash01(i, var6, 6)) * var4 : 0.0F / 0.0F;
      fs[4] = (2.8F + 1.8F * PeepSky.hash01(i * 11 + 3)) * (var8 ? 1.35F : 1.0F);
      fs[5] = var7;
      fs[6] = var6;
   }

   private static void hearts(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      float[] var5 = SCRATCH;

      for (int var6 = 0; var6 < 2; var6++) {
         SkyImage var7 = peepSky.art[var6 == 0 ? 3 : 4];

         for (int var4 = 0; var4 < 20; var4++) {
            heart(var4, g, var5);
            float var10 = peepSky.anchor(53 + var4, (int)var5[6]) + var5[0];
            float var11 = var5[1];
            float var12 = var5[3];
            float var13 = var5[4];
            float var14 = var13 * 128.0F / 120.0F;
            float var15 = var5[5];
            var14 /= 1.0F
               + 0.35F * PeepSky.smooth(15.0F, 40.0F, Math.abs(PeepSky.wrapDelta(var10 - peepSky.aimU)) * 360.0F * MathHelper.cos(var11 * 0.017453292F));
            boolean var9 = !Float.isNaN(var12) && var12 >= 0.0F;
            if (var9 || var6 != 1) {
               float var17 = var9 ? (var6 == 0 ? -1.0F : 1.0F) : 0.0F;
               float var18 = 0.35F * MathHelper.sin(g * 0.5F + var4);
               float var19 = PeepSky.smooth(0.0F, 0.07F, var15) * PeepSky.smooth(1.0F, 8.0F, var11) * f;
               float var20 = 0.0F;
               float var21 = 0.0F;
               if (!Float.isNaN(var12) && var12 > -0.4F) {
                  if (var12 < 0.0F) {
                     var10 += du(PeepSky.smooth(-0.4F, 0.0F, var12) * 0.15F * MathHelper.sin(g * 37.0F + var4), var11);
                  } else {
                     float var22 = PeepSky.smootherstep(Math.min(1.0F, var12 / 1.8F));
                     var20 = 0.55F * var22;
                     var21 = (0.04F + 0.3F * var22) * var14;
                     var11 -= 2.5F * var22 * var22;
                     var19 *= 1.0F - PeepSky.smooth(0.8F, 2.2F, var12);
                  }
               }

               float var8;
               if (!((var19 = var19 * ((var8 = clearAround(peepSky, var10, var11) * peepSky.clearOfTattoos(var10, var11, var13 * 0.45F)) * var8)) <= 0.003F)) {
                  float var37 = var18 + var17 * var20;
                  float var23 = 0.33F * var13;
                  float var24 = var11 - var23;
                  float var25 = var23 * MathHelper.sin(var37) + var17 * var21;
                  float var26 = -var23 * MathHelper.cos(var37);
                  float var27 = var24 - var26;
                  float var28 = var10 + du(var25, var27);
                  if (var6 == 0) {
                     skyPaint.glow(var10, y(var11), units(var13 * 0.95F), 16732080, 0.12F * var19);
                  }

                  skyPaint.sprite(
                     var9 ? var7 : peepSky.art[13],
                     var28,
                     y(var27),
                     units(var14),
                     units(var13),
                     var37,
                     PeepSky.tint(var19 * PeepSky.smooth(0.12F, 0.45F, var19), var4 % 3 == 0 ? 16747208 : 16759014),
                     true
                  );
               }
            }
         }
      }

      SkyImage var30 = peepSky.art[8];

      for (int var31 = 0; var31 < 2; var31++) {
         for (int var29 = 0; var29 < 20; var29++) {
            heart(var29, g, var5);
            float var32 = var5[3];
            if (!Float.isNaN(var32) && !(var32 < 0.0F) && !(var32 > 0.8F)) {
               float var33 = peepSky.anchor(53 + var29, (int)var5[6]) + var5[0];
               float var34 = PeepSky.smooth(0.0F, 0.12F, var32) * (1.0F - PeepSky.smooth(0.2F, 0.8F, var32)) * f * clearAround(peepSky, var33, var5[1]);
               if (!(var34 <= 0.003F)) {
                  if (var31 == 0) {
                     skyPaint.glow(var33, y(var5[1]), units(var5[4] * 1.2F), 16732080, 0.3F * var34);
                  } else {
                     skyPaint.sprite(var30, var33, y(var5[1]), units(var5[4] * 2.6F), units(var5[4] * 2.6F), 0.3F, PeepSky.tint(0.8F * var34, 16766704), true);
                  }
               }
            }
         }
      }
   }

   private static void feathers(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      SkyImage var4 = peepSky.art[5];

      for (int var5 = 0; var5 < 2; var5++) {
         for (int var6 = 0; var6 < 22; var6++) {
            float var7 = PeepSky.hash01(var6 * 13 + 1);
            float var8 = 30.0F + 10.0F * var7;
            float var9 = g / var8 + PeepSky.hash01(var6 * 13 + 2);
            int var10 = MathHelper.floor(var9);
            float var11 = var9 - var10;
            float var12 = PeepSky.hash01(var6 * 13 + 3);
            float var13 = g * 0.75F * (0.85F + 0.3F * var7) + var6 * 2.0F;
            float var14 = MathHelper.lerp(var11, 78.0F, 4.0F) - 0.8F * MathHelper.abs(MathHelper.sin(var13));
            float var15 = around(peepSky, 245 + var6, var10, PeepSky.hash01(var6, var10, 17)) + du(4.5F * MathHelper.sin(var13), var14);
            float var16 = (4.0F + 2.0F * var12) * 1.1F;
            float var17 = PeepSky.smooth(0.0F, 0.08F, var11)
               * PeepSky.smooth(2.0F, 10.0F, var14)
               * f
               * clearAround(peepSky, var15, var14)
               * peepSky.clearOfTattoos(var15, var14, var16 * 0.25F);
            if (!(var17 <= 0.003F)) {
               if (var5 == 0) {
                  skyPaint.glow(var15, y(var14), units(var16 * 0.5F), 16732080, 0.22F * var17);
               } else {
                  float var18 = 0.5F * MathHelper.cos(var13) + (PeepSky.hash01(var6 * 13 + 4) - 0.5F) * 0.6F;
                  int var19 = PeepSky.mix(16756446, 14696602, 1.0F - var17);
                  skyPaint.sprite(var4, var15, y(var14), units(var16 * 0.28846154F), units(var16), var18, PeepSky.tint(var17, var19), true);
               }
            }
         }
      }
   }

   private static void sparks(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      SkyImage var4 = peepSky.art[0];

      for (int var5 = 0; var5 < 2; var5++) {
         for (int var6 = 0; var6 < 135; var6++) {
            float var7 = PeepSky.hash01(var6 * 17 + 1);
            float var8 = 3.5F + 2.5F * var7;
            float var9 = g / var8 + PeepSky.hash01(var6 * 17 + 2);
            int var10 = MathHelper.floor(var9);
            float var11 = var9 - var10;
            float var12 = PeepSky.hash01(var6 * 17 + 3);
            float var13 = 6.0F + 18.0F * PeepSky.hash01(var6, var10, 4);
            float var14 = -1.0F + (var13 + 1.0F) * (1.0F - (float)Math.pow(1.0F - var11, 2.2));
            float var15 = g * (1.1F + 0.6F * var7) + var6;
            float var16 = PeepSky.hash01(var6, var10, 19) + du(1.8F * MathHelper.sin(var15) * var11, var14);
            float var17 = 0.65F + 0.35F * (0.5F + 0.5F * MathHelper.sin(g * (13.0F + 12.0F * var7) + var6 * 2.3F));
            float var18 = PeepSky.smooth(0.03F, 0.12F, var11) * (1.0F - PeepSky.smooth(0.3F, 0.85F, var11)) * var17 * f * clearAround(peepSky, var16, var14);
            if (!(var18 <= 0.003F)) {
               float var19 = 0.4F + 0.5F * var12;
               int var20 = PeepSky.mix(PeepSky.mix(16751206, 16727150, PeepSky.smooth(0.1F, 0.5F, var11)), 12720235, PeepSky.smooth(0.5F, 1.0F, var11));
               float var21 = 1.8F * MathHelper.cos(var15) * (1.1F + 0.6F * var7) * var11 / Math.max(1.0F, var13);
               if (var5 == 0) {
                  float var22 = 3.5F + 2.5F * var12;
                  skyPaint.sprite(
                     var4, var16, y(var14 - var22 * 0.4F), units(var22), units(var19 * 0.7F), -1.5707964F + var21, PeepSky.tint(0.45F * var18, var20), true
                  );
               } else {
                  skyPaint.glow(var16, y(var14), units(var19 * 1.2F), var20, 0.58F * var18);
                  skyPaint.glow(var16, y(var14), units(var19 * 0.7F), 16763024, 0.4F * var18 * (1.0F - PeepSky.smooth(0.15F, 0.45F, var11)));
                  skyPaint.sprite(
                     peepSky.art[1],
                     var16,
                     y(var14 - var19 * 0.45F),
                     units(var19 * 0.5F),
                     units(var19 * 1.8F),
                     3.1415927F + var21,
                     PeepSky.tint(0.75F * var18, 16762010),
                     true
                  );
               }
            }
         }
      }
   }

   private static void ghosts(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      SkyImage var4 = peepSky.art[6];

      for (int var5 = 0; var5 < 2; var5++) {
         for (int var6 = 0; var6 < 21; var6++) {
            float var7 = PeepSky.hash01(var6 * 19 + 1);
            float var8 = 10.0F + 4.0F * var7;
            float var9 = g / var8 + PeepSky.hash01(var6 * 19 + 2);
            int var10 = MathHelper.floor(var9);
            float var11 = var9 - var10;
            float var12 = PeepSky.hash01(var6 * 19 + 3);
            float var13 = MathHelper.lerp((float)Math.pow(var11, 0.9), -4.0F, 58.0F);
            float var14 = g * 0.45F + var6 * 1.9F;
            float var15 = 0.85F + 0.35F * var11;
            float var16 = (8.0F + 5.0F * var12) * var15;
            float var17 = var16 * 96.0F / 256.0F * 1.8F;
            float var18 = var13 - var16 * 0.3F;
            float var19 = around(peepSky, 73 + var6, var10, PeepSky.hash01(var6, var10, 23)) + du(3.6F * MathHelper.sin(var14), var18);
            float var20 = PeepSky.smooth(0.0F, 0.12F, var11)
               * (1.0F - PeepSky.smooth(0.55F, 1.0F, var11))
               * PeepSky.smooth(1.0F, 12.0F, var13)
               * f
               * clearAround(peepSky, var19, var13)
               * clearAround(peepSky, var19, var18);
            if (!(var20 <= 0.003F)) {
               if (var5 == 0) {
                  skyPaint.sprite(var4, var19, y(var18), units(var17), units(var16), 0.32F * MathHelper.cos(var14), PeepSky.tint(0.42F * var20, 16750300), true);
               } else {
                  float var21 = 0.32F * MathHelper.cos(var14);
                  float var22 = var16 * 0.3F;
                  float var23 = var18 + var22 * MathHelper.cos(var21);
                  skyPaint.glow(var19 + du(var22 * MathHelper.sin(var21), var23), y(var23), units(2.0F), 16759020, 0.24F * var20);
               }
            }
         }
      }
   }

   private static void spirits(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      for (int var4 = 0; var4 < 12; var4++) {
         float var5 = PeepSky.hash01(var4 * 31 + 1);
         float var6 = 11.0F + 6.0F * var5;
         float var7 = g / var6 + PeepSky.hash01(var4 * 31 + 2);
         int var8 = MathHelper.floor(var7);
         float var9 = var7 - var8;
         float var10 = MathHelper.lerp((float)Math.pow(var9, 0.85), 1.0F, 26.0F + 10.0F * PeepSky.hash01(var4, var8, 3));
         float var11 = g * (0.4F + 0.2F * var5) + var4 * 2.3F;
         float var12 = around(peepSky, 233 + var4, var8, PeepSky.hash01(var4, var8, 2)) + du(2.2F * MathHelper.sin(var11), var10);
         float var13 = 0.8F + 0.2F * MathHelper.sin(g * (1.3F + 0.6F * var5) + var4);
         float var14 = PeepSky.smooth(0.0F, 0.15F, var9) * (1.0F - PeepSky.smooth(0.5F, 1.0F, var9)) * var13 * f * clearAround(peepSky, var12, var10);
         if (!(var14 <= 0.003F)) {
            float var15 = 0.7F + 0.5F * PeepSky.hash01(var4 * 31 + 4);
            skyPaint.glow(var12, y(var10), units(var15 * 6.0F), 10258144, 0.12F * var14);
            skyPaint.glow(var12, y(var10), units(var15 * 2.0F), 14208255, 0.3F * var14);
            skyPaint.glow(var12, y(var10), units(var15 * 0.7F), 16184063, 0.6F * var14 * core(peepSky, y(var10)));
         }
      }
   }

   private static void motes(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      float var4 = 1.0F + 0.5F * peepSky.pulse;

      for (int var5 = 0; var5 < 55; var5++) {
         float var6 = PeepSky.hash01(var5 * 23 + 1);
         float var7 = 9.0F + 6.0F * var6;
         float var8 = g / var7 + PeepSky.hash01(var5 * 23 + 2);
         int var9 = MathHelper.floor(var8);
         float var10 = var8 - var9;
         float var11 = 4.0F + 56.0F * PeepSky.hash01(var5, var9, 3) + 3.0F * var10 + 1.5F * MathHelper.sin(g * 0.15F + var5 * 1.3F);
         float var12 = around(peepSky, 94 + var5, var9, PeepSky.hash01(var5, var9, 2)) + du(1.2F * MathHelper.sin(g * 0.11F + var5), var11);
         float var13 = 0.55F + 0.45F * MathHelper.sin(g * (0.8F + 0.5F * var6) + var5);
         float var14 = var13 * PeepSky.smooth(0.0F, 0.2F, var10) * (1.0F - PeepSky.smooth(0.75F, 1.0F, var10)) * f * clearAround(peepSky, var12, var11) * var4;
         if (!(var14 <= 0.003F)) {
            float var15 = (0.6F + 0.72F * PeepSky.hash01(var5 * 23 + 4)) * (var5 % 2 == 0 ? 1.3F : 1.0F);
            int var16 = var5 % 2 == 0 ? 16762714 : (var5 % 4 == 1 ? 16752324 : 16765050);
            skyPaint.glow(var12, y(var11), units(var15 * 4.0F), var16, 0.2F * var14);
            skyPaint.glow(var12, y(var11), units(var15 * 1.4F), var16, 0.35F * var14);
            skyPaint.glow(var12, y(var11), units(var15 * 0.6F), var16, 0.42F * var14 * core(peepSky, y(var11)));
         }
      }
   }

   private static void glitter(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      SkyImage var4 = peepSky.art[16];

      for (int var5 = 0; var5 < 2; var5++) {
         for (int var6 = 0; var6 < 84; var6++) {
            float var9 = PeepSky.hash01(var6 * 29 + 1);
            float var10 = (g * (0.9F + 0.9F * var9) + var6 * 2.7F) / 6.2831855F;
            int var8;
            float var11 = var10 - (var8 = MathHelper.floor(var10));
            if (!(var11 >= 0.5F)) {
               float var12 = (float)Math.pow(MathHelper.sin(3.1415927F * (float)Math.pow(var11 * 2.0F, 0.6)), 2.0);
               float var13 = 5.0F + 60.0F * PeepSky.hash01(var6, var8, 3) + 1.5F * MathHelper.sin(g * 0.2F + var6);
               float var14 = around(peepSky, 149 + var6, var8, PeepSky.hash01(var6, var8, 2)) + du(0.6F * MathHelper.sin(g * 0.1F + var6), var13);
               float var15 = PeepSky.smooth(0.05F, 0.3F, var12) * f * clearAround(peepSky, var14, var13);
               if (!(var15 <= 0.003F)) {
                  float var16 = (1.3F + 1.2F * PeepSky.hash01(var6 * 29 + 4)) * (0.25F + 0.75F * var12);
                  boolean var7 = var6 % 4 == 3;
                  int var18 = var7 ? 16747213 : (var6 % 2 == 0 ? 16769952 : 16771504);
                  float var19 = (1.0F - 0.35F * PeepSky.smooth(14.0F, 4.0F, var13)) * core(peepSky, y(var13));
                  if (var5 == 0) {
                     skyPaint.glow(var14, y(var13), units(var16 * 0.9F), var7 ? 16732080 : 16758858, 0.28F * var15 * var12);
                  } else {
                     skyPaint.sprite(
                        var4,
                        var14,
                        y(var13),
                        units(var16),
                        units(var16),
                        0.4F * MathHelper.sin(g * 0.3F + var6),
                        PeepSky.tint((var7 ? 0.65F : 0.8F) * var15 * var19, var18),
                        true
                     );
                  }
               }
            }
         }
      }
   }

   static void orbits(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      SkyImage var4 = peepSky.art[9];
      float var5 = units(18.0F);

      for (int var6 = 0; var6 < peepSky.shownTattoos; var6++) {
         float var7 = Math.min(var5, (peepSky.tw[var6] + peepSky.th[var6]) * 0.5F);
         float var8 = peepSky.tGlow[var6] * (0.6F + 0.4F * peepSky.tGlow[var6]) * PeepSky.smooth(0.3F, 0.5F, peepSky.tGlow[var6]) * g;
         if (!(var8 <= 0.003F)) {
            float var9 = f * 0.55F + var6 * 1.3F;
            float var10 = f * 0.33F + var6 * 2.1F + 2.0F;
            skyPaint.sprite(var4, peepSky.tu[var6], peepSky.ty[var6], var7 * 1.35F, var7 * 1.35F, var9, PeepSky.tint(0.7F * var8, 13995263), true);
            skyPaint.sprite(var4, peepSky.tu[var6], peepSky.ty[var6], var7 * 1.6F, var7 * 1.6F, var10, PeepSky.tint(0.5F * var8, 16748512), true);
            arcHead(peepSky, skyPaint, var6, var7 * 1.35F, var9, 0.5F * var8);
            arcHead(peepSky, skyPaint, var6, var7 * 1.6F, var10, 0.35F * var8);
         }
      }
   }

   private static void arcHead(PeepSky peepSky, SkyPaint skyPaint, int i, float f, float g, float h) {
      float var6 = f * 0.421875F;
      float var7 = (0.72F - peepSky.ty[i]) * 1.0471976F * 57.295776F;
      float var8 = peepSky.tu[i] + du(var6 * MathHelper.cos(g) * 60.0F, var7);
      float var9 = peepSky.ty[i] + var6 * MathHelper.sin(g);
      skyPaint.glow(var8, var9, units(3.0F), 16732080, 0.3F * h);
      skyPaint.glow(var8, var9, units(1.8F), 16748512, 0.3F * h);
      skyPaint.glow(var8, var9, units(0.8F), 16766704, h);
   }

   static void goldSparkles(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
      SkyImage var4 = peepSky.art[8];

      for (int var5 = 0; var5 < 2; var5++) {
         for (int var6 = 0; var6 < peepSky.shownTattoos; var6++) {
            float var7 = PeepSky.smooth(0.15F, 0.6F, peepSky.tGlow[var6]) * g;
            if (!(var7 <= 0.003F)) {
               for (int var8 = 0; var8 < 3; var8++) {
                  float var9 = var8 * 2.1F + var6 + f * 0.15F;
                  float var10 = (float)Math.pow(Math.max(0.0F, MathHelper.sin(f * (1.1F + 0.3F * var8) + var6 * 1.9F + var8 * 2.2F)), 4.0);
                  if (!(var10 <= 0.03F)) {
                     float var11 = peepSky.tu[var6]
                        + MathHelper.cos(var9)
                           * peepSky.tw[var6]
                           * 0.5F
                           * 60.0F
                           / (360.0F * Math.max(0.2F, MathHelper.cos((0.72F - peepSky.ty[var6]) * 1.0471976F)));
                     float var12 = peepSky.ty[var6] + MathHelper.sin(var9) * peepSky.th[var6] * 0.55F;
                     float var13 = Math.min(peepSky.th[var6] * 0.6F, units(7.0F)) * (0.2F + 0.8F * var10);
                     float var14 = var7 * PeepSky.smooth(0.03F, 0.25F, var10);
                     if (var5 == 0) {
                        skyPaint.glow(var11, var12, var13 * 0.35F, 16758858, 0.25F * var14 * var10);
                     } else {
                        skyPaint.sprite(var4, var11, var12, var13, var13, f * 0.4F + var8, PeepSky.tint(var14, var8 == 1 ? 16774354 : 16770720), true);
                     }
                  }
               }
            }
         }
      }
   }

   static float spread(float f) {
      return f < 0.8F ? (f / 0.8F - 0.5F) * 0.375F : (f - 0.8F) / 0.19999999F;
   }

   private static float around(PeepSky peepSky, int i, int j, float f) {
      return peepSky.anchor(i, j) + spread(f);
   }

   private static void horizonLine(SkyPaint skyPaint, float f, float g, int i, float h) {
      if (!(h <= 0.003F)) {
         int var5 = PeepSky.tint(h, i);
         int var6 = PeepSky.tint(0.0F, i);
         float var7 = y(g);
         float var8 = y(f);
         float var9 = y(-1.0F);

         for (int var10 = 0; var10 < 48; var10++) {
            float var11 = var10 / 48.0F;
            float var12 = (var10 + 1) / 48.0F;
            skyPaint.gradient(true, var11, var12, var7, var8, var6, var5);
            skyPaint.gradient(true, var11, var12, var8, var9, var5, var6);
         }
      }
   }

   private static void mist(SkyPaint skyPaint, SkyImage skyImage, int i, float f, float g, float h, int j, float k, boolean bl) {
      if (!(k <= 0.003F)) {
         float var9 = y(h);
         float var10 = y(g);
         float var11 = PeepSky.wrap(f);
         int var12 = PeepSky.tint(k, j);

         for (int var13 = 0; var13 < i; var13++) {
            float var14 = (var13 + var11) / i;
            skyPaint.band(skyImage, var14, var14 + 1.0F / i, var9, var10, var12, bl, 24);
         }
      }
   }

   private static void beam(SkyPaint skyPaint, SkyImage skyImage, float f, float g, float h, float i, float j, int k, float l) {
      beam(skyPaint, skyImage, f, g, h, i, j, k, l, true);
   }

   private static void beam(SkyPaint skyPaint, SkyImage skyImage, float f, float g, float h, float i, float j, int k, float l, boolean bl) {
      if (!(l <= 0.003F)) {
         arc(f, g, h);
         float var10 = onArc(i * 0.5F, 0.0F);
         skyPaint.sprite(skyImage, AT[0], y(AT[1]), units(j), units(i), var10, PeepSky.tint(l, k), bl);
      }
   }

   private static void arc(float f, float g, float h) {
      float var3 = f * 6.2831855F;
      float var4 = g * 0.017453292F;
      float var5 = MathHelper.cos(var3);
      float var6 = MathHelper.sin(var3);
      float var7 = MathHelper.cos(var4);
      float var8 = MathHelper.sin(var4);
      float var9 = MathHelper.cos(h);
      float var10 = MathHelper.sin(h);
      ARC[0] = var7 * var5;
      ARC[1] = var7 * var6;
      ARC[2] = var8;
      ARC[3] = -var9 * var8 * var5 - var10 * var6;
      ARC[4] = -var9 * var8 * var6 + var10 * var5;
      ARC[5] = var9 * var7;
   }

   private static float onArc(float f, float g) {
      float var2 = f * 0.017453292F;
      float var3 = MathHelper.cos(var2);
      float var4 = MathHelper.sin(var2);
      float var5 = var3 * ARC[0] + var4 * ARC[3];
      float var6 = var3 * ARC[1] + var4 * ARC[4];
      float var7 = var3 * ARC[2] + var4 * ARC[5];
      float var8 = var3 * ARC[3] - var4 * ARC[0];
      float var9 = var3 * ARC[4] - var4 * ARC[1];
      float var10 = var3 * ARC[5] - var4 * ARC[2];
      if (g != 0.0F) {
         float var11 = g * 0.017453292F;
         float var12 = MathHelper.cos(var11);
         float var13 = MathHelper.sin(var11);
         float var14 = var9 * var7 - var10 * var6;
         float var15 = var10 * var5 - var8 * var7;
         float var16 = var8 * var6 - var9 * var5;
         var5 = var12 * var5 + var13 * var14;
         var6 = var12 * var6 + var13 * var15;
         var7 = var12 * var7 + var13 * var16;
      }

      ARC[6] = var5;
      ARC[7] = var6;
      ARC[8] = var7;
      float var19 = (float)Math.atan2(var6, var5);
      float var20 = (float)Math.asin(MathHelper.clamp(var7, -1.0F, 1.0F));
      AT[0] = PeepSky.wrap(var19 / 6.2831855F);
      AT[1] = var20 * 57.295776F;
      float var21 = MathHelper.cos(var19);
      float var22 = MathHelper.sin(var19);
      float var23 = MathHelper.sin(var20);
      float var24 = MathHelper.cos(var20);
      float var17 = -var8 * var23 * var21 - var9 * var23 * var22 + var10 * var24;
      float var18 = -var8 * var22 + var9 * var21;
      return (float)Math.atan2(var18, var17);
   }

   private static float alongArc(float f, float g) {
      float var2 = f * 6.2831855F;
      float var3 = g * 0.017453292F;
      float var4 = MathHelper.cos(var3);
      float var5 = var4 * MathHelper.cos(var2);
      float var6 = var4 * MathHelper.sin(var2);
      float var7 = MathHelper.sin(var3);
      return (float)Math.atan2(var5 * ARC[3] + var6 * ARC[4] + var7 * ARC[5], var5 * ARC[0] + var6 * ARC[1] + var7 * ARC[2]) * 57.295776F;
   }

   private static float acrossArc(float f, float g) {
      float var2 = f * 6.2831855F;
      float var3 = g * 0.017453292F;
      float var4 = MathHelper.cos(var3);
      float var5 = var4 * MathHelper.cos(var2);
      float var6 = var4 * MathHelper.sin(var2);
      float var7 = MathHelper.sin(var3);
      float var8 = ARC[4] * ARC[2] - ARC[5] * ARC[1];
      float var9 = ARC[5] * ARC[0] - ARC[3] * ARC[2];
      float var10 = ARC[3] * ARC[1] - ARC[4] * ARC[0];
      return (float)Math.asin(MathHelper.clamp(Math.abs(var5 * var8 + var6 * var9 + var7 * var10), 0.0F, 1.0F)) * 57.295776F;
   }

   private static float beamClear(PeepSky peepSky, float f, float g, float h, float i, float j) {
      arc(f, g, h);
      float var6 = MathHelper.clamp(alongArc(peepSky.aimU, peepSky.viewUp), 0.0F, i);
      onArc(var6, 0.0F);
      float var7 = peepSky.aimU * 6.2831855F;
      float var8 = peepSky.viewUp * 0.017453292F;
      float var9 = MathHelper.cos(var8);
      float var10 = var9 * MathHelper.cos(var7) * ARC[6] + var9 * MathHelper.sin(var7) * ARC[7] + MathHelper.sin(var8) * ARC[8];
      float var11 = (float)Math.acos(MathHelper.clamp(var10, -1.0F, 1.0F)) * 57.295776F - j * (0.025F + 0.42F * var6 / i);
      float var12 = 0.0F;

      for (int var13 = 1; var13 <= 8; var13++) {
         float var14 = i * var13 / 9.0F;
         float var15 = j * (0.025F + 0.42F * var14 / i) * 0.5F;
         onArc(var14, 0.0F);
         var12 = Math.max(var12, peepSky.lyricBox(AT[0], AT[1], var15, var15));
      }

      return (0.35F + 0.65F * PeepSky.smooth(3.0F, 12.0F, var11)) * (1.0F - 0.45F * var12);
   }

   private static float clear(PeepSky peepSky, float f, float g) {
      return peepSky.clear(f, g);
   }

   private static float core(PeepSky peepSky, float f) {
      return 1.0F - 0.4F * peepSky.hazeAt(f);
   }

   private static float clearAround(PeepSky peepSky, float f, float g) {
      return peepSky.clear(f, g);
   }

   private static float y(float f) {
      return PeepSky.elevationToY(f);
   }

   private static float units(float f) {
      return f * 0.017453292F / 1.0471976F;
   }

   private static float du(float f, float g) {
      return f / (360.0F * Math.max(0.2F, MathHelper.cos(g * 0.017453292F)));
   }
}
