package net.efn.client.module.impl.render;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import javax.imageio.ImageIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class PeepSky {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   static final int PINK = 16732080;
   static final int HOT = 16766704;
   static final int DEEP = 12720235;
   private static final int BLUR = 9;
   private static final int SEGMENTS = 64;
   private static final int ROWS = 14;
   private static final int TATTOO_PAD = 26;
   private static final int TATTOO_MAX = 320;
   private static final float COVER_SHARE = 0.7F;
   private static final float TATTOO_ART = 0.8F;
   static final float SPIN_PER_SECOND = 0.02F;
   private static final String TATTOOS = "/assets/efn/textures/peep/";
   private static final String[] KINDS = new String[]{"star", "anarchy", "cake", "heart", "wave", "rose", "mom", "horseshoe", "hellboy", "rip"};
   static final int STAR = 0;
   static final int RIP = 9;
   private static final float[] ELEVATION = new float[]{41.0F, 18.0F, 34.0F, 46.0F, 22.0F, 30.0F, 38.0F, 49.0F, 23.0F, 36.0F};
   private static final float[] SIZE = new float[]{6.0F, 10.0F, 8.0F, 6.0F, 7.0F, 9.0F, 5.0F, 6.0F, 6.0F, 12.0F};
   private static final int STARS = 140;
   private static final int BOKEH = 12;
   private static final float CROSSFADE_SECONDS = 2.0F;
   private static final float STAGE_FOLLOW = 0.5F;
   private static final float FALL_PER_SECOND = 0.014285714F;
   private static final float FALL_LOW = 12.0F;
   private static final float FALL_HIGH = 64.0F;
   private static final float STAR_LOW = -1.5F;
   private static final float STAR_HIGH = 86.0F;
   private static final float LYRIC_EASE = 3.0F;
   private static final float PARTICLE_KEEP_OUT = 1.0F;
   private static final float TEXT_GAP = 3.0F;
   private static final float TEXT_GAP_BELOW = 1.0F;
   private static final float TEXT_FADE = 5.0F;
   private static final float TEXT_FADE_BELOW = 1.5F;
   private static final float COLUMN_HALF = 14.0F;
   private static final float AIM_NEAR = 6.0F;
   private static final float AIM_FAR = 15.0F;
   private static final float HORIZON_HOLD = 0.04F;
   private static final float HORIZON_DEEP = 0.36F;
   private static final float HORIZON_FADE = 0.6F;
   private static final float HORIZON_DEEP_SHARE = 0.78F;
   private static final float BEAT_RISE = 0.14F;
   private static final int GOLD = 16769658;
   private static final int GHOST = 14208255;
   private static final float LIT_GLOW = 0.6F;
   private volatile PeepSky.Built built;
   private volatile int generation;
   private boolean building;
   private SkyImage[] tattoos;
   private SkyImage[] rims;
   private SkyImage[] golds;
   private SkyImage[] veils;
   private int[] kinds;
   SkyImage[] art;
   private final float[] weight = new float[PeepSong.ALL.length];
   private final float[] shown = new float[PeepSong.ALL.length];
   private final float[] songAge = new float[PeepSong.ALL.length];
   private PeepSong song;
   float time;
   float alpha;
   float viewU;
   float aimU;
   float viewUp;
   float stageU = 0.0F / 0.0F;
   float pulse;
   int horizonRgb;
   float horizonShare;
   private float fall;
   private float fallFade;
   private boolean fresh = true;
   static final int ANCHORS = 512;
   private static final int GHOST_SLOTS = 512 - KINDS.length;
   private static final float HAUNT_SPEED = 0.42F;
   private static final float HAUNT_PHASE = 3.5F;
   private static final float[] HAUNT_AZIMUTH = new float[]{-40.0F, -40.0F, -56.0F, -56.0F, -33.0F, -33.0F, 33.0F, 33.0F, 40.0F, 40.0F, 56.0F};
   private static final boolean[] HAUNT_HIGH = new boolean[]{false, false, true, true, true, true, true, true, false, false, true};
   private final float[] anchorU = new float[512];
   private final int[] anchorRun = new int[512];
   private final byte[] pick = new byte[512];
   private final int[] pickRun = new int[512];
   private boolean lyricsShown;
   private float lyricU;
   private float titleU;
   private float lyricUp;
   private float lineHalfWide;
   private float lineHalfTall;
   private float titleUp;
   private float titleHalfWide;
   private float titleHalfTall;
   private float wantLineWide;
   private float wantTitleWide;
   private final PeepSky.Look look;
   private static final int MAX_SHOWN = KINDS.length * 2;
   final float[] tu;
   final float[] ty;
   final float[] tw;
   final float[] th;
   final float[] tGlow;
   final float[] tLit;
   private final float[] tSpin;
   private final float[] tAlpha;
   private final float[] tGhost;
   private final int[] tTex;
   int shownTattoos;
   private static final PeepSky.Palette BASE = new PeepSky.Palette(16732080, 12720235, 1836056, 525072, 3934768, 16766704, 16732080, 0.42F, 1.0F, 1.0F);
   private static final PeepSky.Palette[] PALETTES = new PeepSky.Palette[PeepSong.ALL.length];

   PeepSky() {
      Arrays.fill(this.anchorRun, -2147483648);
      Arrays.fill(this.pickRun, -2147483648);
      this.lyricUp = 28.0F;
      this.look = new PeepSky.Look();
      this.tu = new float[MAX_SHOWN];
      this.ty = new float[MAX_SHOWN];
      this.tw = new float[MAX_SHOWN];
      this.th = new float[MAX_SHOWN];
      this.tGlow = new float[MAX_SHOWN];
      this.tLit = new float[MAX_SHOWN];
      this.tSpin = new float[MAX_SHOWN];
      this.tAlpha = new float[MAX_SHOWN];
      this.tGhost = new float[MAX_SHOWN];
      this.tTex = new int[MAX_SHOWN];
   }

   private static PeepSky.Palette palette(PeepSong var0) {
      return switch (var0) {
         case STAR_SHOPPING -> new PeepSky.Palette(16732080, 12720235, 1574434, 394260, 3017786, 16766704, 16740296, 0.4F, 1.0F, 1.2F);
         case CRYBABY -> new PeepSky.Palette(15751854, 11016294, 1574426, 459791, 3412026, 16766704, 15755960, 0.38F, 0.92F, 0.9F);
         case CRY_ALONE -> new PeepSky.Palette(12073606, 7999568, 1114896, 262664, 2230304, 15760056, 12720235, 0.26F, 0.68F, 0.4F);
         case HELLBOY -> new PeepSky.Palette(16727406, 11539004, 1967116, 655878, 4720670, 16752312, 16724821, 0.52F, 1.0F, 0.85F);
         case BEAMER_BOY -> new PeepSky.Palette(14241744, 8002188, 1181220, 328468, 2363456, 15780095, 11554303, 0.34F, 0.9F, 0.9F);
         case SPOTLIGHT -> new PeepSky.Palette(12728968, 7999568, 1442831, 393739, 2492966, 15909598, 12720235, 0.32F, 0.85F, 0.8F);
         case THE_BRIGHTSIDE -> new PeepSky.Palette(16740277, 12728960, 4198974, 2886188, 15764620, 16769224, 16756848, 0.48F, 1.0F, 0.45F);
         case SUNLIGHT_ON_YOUR_SKIN -> new PeepSky.Palette(16751540, 13654656, 5247556, 3017264, 16746144, 16770760, 16760944, 0.4F, 1.0F, 0.6F);
         case LIFE_IS_BEAUTIFUL -> new PeepSky.Palette(16740250, 13117530, 2492440, 1049610, 5904440, 16765660, 16742550, 0.46F, 1.0F, 0.9F);
         case FALLING_DOWN -> new PeepSky.Palette(14711487, 9186424, 1509924, 525846, 2889272, 16046320, 15043792, 0.32F, 0.9F, 1.0F);
         case SAVE_THAT_SHIT -> new PeepSky.Palette(16732080, 13639802, 2229532, 656144, 4721718, 16766704, 16732080, 0.48F, 1.0F, 1.0F);
         case AWFUL_THINGS -> new PeepSky.Palette(9050714, 4852272, 786952, 196868, 2754592, 14701982, 12720235, 0.3F, 0.62F, 0.45F);
         case WITCHBLADES -> new PeepSky.Palette(13127167, 8002216, 1705520, 656408, 4593238, 15255807, 14835936, 0.44F, 0.95F, 0.85F);
         case FOUR_GOLD_CHAINS -> new PeepSky.Palette(16736170, 12720235, 2033430, 656392, 4854832, 16770736, 16732080, 0.5F, 1.0F, 1.0F);
         case GHOST_BOY -> new PeepSky.Palette(12352724, 7617154, 2362428, 1378338, 5121634, 14735600, 13671666, 0.42F, 0.85F, 0.7F);
         case GHOST_GIRL -> new PeepSky.Palette(15774948, 12093128, 4991604, 3415640, 14198482, 16773370, 16769268, 0.46F, 1.0F, 0.6F);
      };
   }

   void ready(SkyPaint var1, ExecutorService var2) {
      if (this.tattoos == null) {
         PeepSky.Built var3 = this.built;
         if (var3 != null && var3.generation() != this.generation) {
            this.built = null;
            var3 = null;
         }

         if (var3 == null) {
            if (!this.building) {
               this.building = true;
               int var12 = this.generation;
               var2.submit(() -> {
                  if (var12 == this.generation) {
                     try {
                        PeepSky.Built var2x = build(var12);
                        if (var12 == this.generation) {
                           this.built = var2x;
                        }
                     } catch (RuntimeException | Error var3x) {
                        LOGGER.warn("[EFN] Sky easter egg textures couldn't be drawn", var3x);
                     }
                  }
               });
            }
         } else {
            SkyImage[] var4 = new SkyImage[var3.tattoos().length];
            SkyImage[] var5 = new SkyImage[var4.length];
            SkyImage[] var6 = new SkyImage[var4.length];
            SkyImage[] var7 = new SkyImage[var4.length];
            SkyImage[] var8 = new SkyImage[var3.art().length];

            try {
               for (int var9 = 0; var9 < var4.length; var9++) {
                  var4[var9] = var1.upload("peep_tattoo_" + var9, var3.widths()[var9], var3.heights()[var9], var3.tattoos()[var9]);
                  var5[var9] = var1.upload("peep_rim_" + var9, var3.widths()[var9], var3.heights()[var9], var3.rims()[var9]);
                  var6[var9] = var1.upload("peep_gold_" + var9, var3.widths()[var9], var3.heights()[var9], var3.golds()[var9]);
                  var7[var9] = var1.upload("peep_veil_" + var9, var3.widths()[var9], var3.heights()[var9], var3.veils()[var9]);
               }

               for (int var13 = 0; var13 < var8.length; var13++) {
                  PeepArt.Art var10 = var3.art()[var13];
                  var8[var13] = var1.upload("peep_fx_" + var13, var10.width(), var10.height(), var3.artLevels()[var13]);
               }
            } catch (RuntimeException | Error var11) {
               closeAll(var4);
               closeAll(var5);
               closeAll(var6);
               closeAll(var7);
               closeAll(var8);
               throw var11;
            }

            this.kinds = var3.kinds();
            this.rims = var5;
            this.golds = var6;
            this.veils = var7;
            this.art = var8;
            this.tattoos = var4;
            this.built = null;
            this.building = false;
         }
      }
   }

   boolean holding() {
      return this.tattoos != null || this.building || this.built != null;
   }

   private static void closeAll(SkyImage[] var0) {
      if (var0 != null) {
         for (SkyImage var4 : var0) {
            if (var4 != null) {
               var4.close();
            }
         }
      }
   }

   void close() {
      closeAll(this.tattoos);
      closeAll(this.rims);
      closeAll(this.golds);
      closeAll(this.veils);
      closeAll(this.art);
      this.tattoos = null;
      this.rims = null;
      this.golds = null;
      this.veils = null;
      this.art = null;
      this.generation++;
      this.built = null;
      this.building = false;
      this.restart();
   }

   void restart() {
      Arrays.fill(this.weight, 0.0F);
      Arrays.fill(this.shown, 0.0F);
      Arrays.fill(this.songAge, 0.0F);
      this.song = null;
      this.stageU = 0.0F / 0.0F;
      this.fall = 0.0F;
      this.fallFade = 0.0F;
      this.fresh = true;
      Arrays.fill(this.anchorRun, -2147483648);
      Arrays.fill(this.pickRun, -2147483648);
   }

   float anchor(int var1, int var2) {
      if (this.anchorRun[var1] != var2) {
         this.anchorRun[var1] = var2;
         this.anchorU[var1] = Float.isNaN(this.stageU) ? this.viewU : this.stageU;
      }

      return this.anchorU[var1];
   }

   float anchored(int var1, int var2) {
      return this.anchorRun[var1] == var2 ? this.anchorU[var1] : 0.0F / 0.0F;
   }

   int picked(int var1, int var2) {
      return this.pickRun[var1] == var2 ? this.pick[var1] : -1;
   }

   void pick(int var1, int var2, int var3) {
      this.pickRun[var1] = var2;
      this.pick[var1] = (byte)var3;
   }

   void lyrics(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      this.lyricsShown = !Float.isNaN(var2);
      this.lyricU = wrap(var1);
      this.titleU = wrap(var5);
      this.lyricUp = this.lyricsShown ? var2 : 28.0F;
      this.lineHalfTall = var4;
      this.titleUp = var6;
      this.titleHalfTall = var8;
      this.wantLineWide = this.lyricsShown ? var3 : 0.0F;
      this.wantTitleWide = this.lyricsShown ? var7 : 0.0F;
   }

   void update(float var1, PeepSong var2, float var3, float var4, float var5, float var6, float var7) {
      this.song = var2;
      boolean var9 = var3 <= 0.0F || this.fresh;
      this.fresh = false;
      float var10 = var1 / 2.0F;
      float var11 = 0.0F;

      for (int var8 = 0; var8 < this.weight.length; var8++) {
         boolean var13 = this.song != null && this.song.ordinal() == var8;
         float var12 = var13 ? 1.0F : 0.0F;
         this.weight[var8] = var9
            ? var12
            : (this.weight[var8] < var12 ? Math.min(var12, this.weight[var8] + var10) : Math.max(var12, this.weight[var8] - var10));
         this.shown[var8] = smootherstep(this.weight[var8]);
         var11 += this.shown[var8];
         this.songAge[var8] = var13 ? Math.min(this.songAge[var8] + var1, 100000.0F) : (this.weight[var8] <= 0.0F ? 0.0F : this.songAge[var8]);
      }

      if (var11 > 1.0F) {
         int var15 = 0;

         while (var15 < this.shown.length) {
            int var16 = var15++;
            this.shown[var16] = this.shown[var16] / var11;
         }
      }

      this.viewU = wrap(var4);
      this.aimU = wrap(var5);
      this.viewUp = var6;
      if (!this.lyricsShown) {
         this.lyricU = this.viewU;
         this.titleU = this.viewU;
      }

      float var17 = var9 ? 1.0F : Math.min(1.0F, var1 * 3.0F);
      this.lineHalfWide = this.lineHalfWide + (this.wantLineWide - this.lineHalfWide) * var17;
      this.titleHalfWide = this.titleHalfWide + (this.wantTitleWide - this.titleHalfWide) * var17;
      this.pulse = var7;
      this.stageU = !Float.isNaN(this.stageU) && !var9 ? wrap(this.stageU + wrapDelta(this.viewU - this.stageU) * Math.min(1.0F, var1 * 0.5F)) : this.viewU;
      float var18 = this.w(PeepSong.FALLING_DOWN);
      if (var18 > 0.001F) {
         this.fall = MathHelper.method_22450(this.fall + var1 * 0.014285714F * var18);
      }

      float var14 = this.fallFade = this.song == PeepSong.FALLING_DOWN ? Math.min(1.0F, this.fallFade + var10) : Math.max(0.0F, this.fallFade - var10);
      if (this.fallFade <= 0.0F) {
         this.fall = 0.0F;
      }
   }

   float w(PeepSong var1) {
      return this.shown[var1.ordinal()];
   }

   float age(PeepSong var1) {
      return this.songAge[var1.ordinal()];
   }

   void draw(SkyPaint var1, float var2, float var3) {
      this.time = var3;
      float var11 = smootherstep(MathHelper.method_15363(var2 / 0.7F, 0.0F, 1.0F));
      float var12 = smootherstep(MathHelper.method_15363((var2 - 0.7F) / 0.3F, 0.0F, 1.0F));
      if (var11 <= 0.0F) {
         this.horizonShare = 0.0F;
         PeepWorld.horizon(this.horizonRgb, 0.0F);
      } else {
         this.alpha = var12;
         this.blendLook();
         float var13 = -0.78F;
         float var14 = 1.27F;
         float var15 = MathHelper.method_16439(var11, var14, var13 - 0.05F);
         this.horizon(var15, var11, var3);

         for (int var16 = 0; var16 < 14; var16++) {
            float var17 = MathHelper.method_16439(var16 / 14.0F, var15, var14);
            float var18 = MathHelper.method_16439((var16 + 1) / 14.0F, var15, var14);
            int var19 = this.shade(var17 - var15, var17, 0.72F);
            int var20 = this.shade(var18 - var15, var18, 0.72F);

            for (int var21 = 0; var21 < 64; var21++) {
               float var22 = var21 / 64.0F;
               float var23 = (var21 + 1) / 64.0F;
               var1.gradient(false, var22, var23, var17, var18, var19, var20);
            }
         }

         float var31 = 0.09F;
         int var32 = tint(0.85F, this.look.topColor);
         int var33 = tint(0.0F, this.look.topColor);

         for (int var34 = 0; var34 < 64; var34++) {
            float var35 = var34 / 64.0F;
            float var37 = (var34 + 1) / 64.0F;
            var1.gradient(true, var35, var37, var15 - var31, var15, var33, var32);
            var1.gradient(true, var35, var37, var15, var15 + var31 * 2.2F, tint(0.55F, this.look.rimColor), var33);
         }

         boolean var10 = this.art != null && var12 > 0.0F;
         if (var10) {
            PeepEffects.skyLight(this, var1, var11, var3);
         }

         this.horizonGlow(var1, var15, var11, var3);
         if (this.pulse > 0.0F && this.lyricsShown) {
            var1.glow(this.lyricU, elevationToY(this.lyricUp + 1.0F), 0.4F, 16732080, 0.18F * this.pulse * var12);
         }

         float var36 = this.fall;
         float var38 = smootherstep(this.fallFade);
         float var39 = 1.0F - 0.5F * this.w(PeepSong.CRY_ALONE);

         for (int var9 = 0; var9 < 12; var9++) {
            float var8 = MathHelper.method_22450(var9 * 0.2718F + var3 * (0.004F + 0.002F * (var9 % 3)) * (var9 % 2 == 0 ? 1.0F : -1.0F));
            float var7 = MathHelper.method_16439(MathHelper.method_22450(var9 * 0.5713F + 0.21F), var13 + 0.45F, 0.67F);
            float var6 = 1.0F;
            if (var38 > 0.0F) {
               float var5 = (0.72F - var7) * 60.0F;
               float var4 = MathHelper.method_22450((var5 - -1.5F) / 87.5F - var36);
               var7 = elevationToY(MathHelper.method_16439(var38, var5, -1.5F + var4 * 87.5F));
               var6 = MathHelper.method_16439(var38, 1.0F, smooth(0.0F, 0.08F, var4) * (1.0F - smooth(0.7F, 0.85F, var4)));
            }

            if (!(var7 < var15 + 0.05F)) {
               float var40 = 0.6F + 0.4F * MathHelper.method_15374(var3 * (0.35F + 0.07F * var9) + var9 * 1.3F);
               float var24 = 0.07F + 0.05F * (var9 % 4);
               var1.glow(
                  var8,
                  var7,
                  var24,
                  var9 % 3 == 0 ? this.look.deepColor : this.look.topColor,
                  0.16F * var40 * var11 * this.look.light * (var6 * smooth(0.02F, 0.02F + var24, 0.72F - var7)) * var39 * (1.0F + 0.8F * this.pulse)
               );
            }
         }

         for (int var30 = 0; var30 < 140; var30++) {
            float var29 = MathHelper.method_22450(var30 * 0.618034F);
            float var28 = MathHelper.method_16439(MathHelper.method_22450(var30 * 0.41421F + 0.13F), var13 + 0.08F, 0.74F);
            float var27 = 1.0F;
            if (var38 > 0.0F) {
               float var26 = (0.72F - var28) * 60.0F;
               float var25 = MathHelper.method_22450((var26 - -1.5F) / 87.5F - var36);
               var28 = elevationToY(MathHelper.method_16439(var38, var26, -1.5F + var25 * 87.5F));
               var27 = MathHelper.method_16439(var38, 1.0F, smooth(0.0F, 0.03F, var25) * (1.0F - smooth(0.9F, 0.99F, var25)));
            }

            if (!(var28 < var15 + 0.01F)) {
               float var41 = 0.35F + 0.65F * Math.max(0.0F, MathHelper.method_15374(var3 * (0.9F + var30 % 7 * 0.31F) + var30 * 2.17F));
               var1.glow(
                  var29,
                  var28,
                  0.004F + 0.0035F * (var30 % 4),
                  var30 % 5 == 0 ? 16766704 : 16773882,
                  Math.min(1.0F, 0.8F * var41 * this.look.starLight) * var11 * var27
               );
            }
         }

         boolean var42 = var12 > 0.0F && this.tattoos != null;
         this.shownTattoos = 0;
         if (var42) {
            this.layoutTattoos(smooth(0.35F, 1.0F, var12) * var12, var3);
         }

         if (var10) {
            PeepEffects.behind(this, var1, var3);
         }

         if (var42) {
            this.drawTattoos(var1, var3);
         }

         if (var10) {
            PeepEffects.front(this, var1, var3);
         }
      }
   }

   private void horizon(float var1, float var2, float var3) {
      float var4 = 0.72F;
      int var5 = this.shade(var4 - var1, var4, var4);
      float var6 = 0.0F;
      if (Math.max(var1, var4 - 0.42F) < var4) {
         float var7 = 0.85F + 0.15F * MathHelper.method_15374(var3 * 0.6F);
         var6 = MathHelper.method_15363(this.look.glowStrength * var2 * var7 * (1.0F + 0.45F * this.pulse), 0.0F, 1.0F);
      }

      int var11 = this.look.glowColor;
      int var8 = Math.min(255, Math.round((var5 >> 16 & 0xFF) + (var11 >> 16 & 0xFF) * var6));
      int var9 = Math.min(255, Math.round((var5 >> 8 & 0xFF) + (var11 >> 8 & 0xFF) * var6));
      int var10 = Math.min(255, Math.round((var5 & 0xFF) + (var11 & 0xFF) * var6));
      this.horizonRgb = var8 << 16 | var9 << 8 | var10;
      this.horizonShare = 0.97F * MathHelper.method_15363((var4 - var1) / 0.1F, 0.0F, 1.0F);
      PeepWorld.horizon(this.horizonRgb, this.horizonShare);
   }

   private void horizonGlow(SkyPaint var1, float var2, float var3, float var4) {
      float var5 = Math.max(var2, 0.30000004F);
      float var6 = 0.72F;
      float var7 = 0.76000005F;
      float var8 = 1.08F;
      float var9 = 1.32F;
      if (!(var5 >= var6)) {
         float var10 = 0.85F + 0.15F * MathHelper.method_15374(var4 * 0.6F);
         float var11 = this.look.glowStrength * var3 * var10 * (1.0F + 0.45F * this.pulse);
         int var12 = tint(var11, this.look.glowColor);
         int var13 = tint(var11 * 0.78F, this.look.glowColor);
         int var14 = tint(0.0F, this.look.glowColor);

         for (int var15 = 0; var15 < 64; var15++) {
            float var16 = var15 / 64.0F;
            float var17 = (var15 + 1) / 64.0F;
            var1.gradient(true, var16, var17, var5, var6, var14, var12);
            var1.gradient(true, var16, var17, var6, var7, var12, var12);
            var1.gradient(true, var16, var17, var7, var8, var12, var13);
            var1.gradient(true, var16, var17, var8, var9, var13, var14);
         }
      }
   }

   float lyricBox(float var1, float var2, float var3, float var4) {
      if (!this.lyricsShown) {
         return 0.0F;
      } else {
         float var5 = 360.0F * MathHelper.method_15362(var2 * 0.017453292F);
         float var6 = this.lineHalfWide <= 0.05F
            ? 0.0F
            : smooth(0.0F, 3.0F, this.lineHalfWide)
               * box(Math.abs(wrapDelta(var1 - this.lyricU)) * var5, var2 - this.lyricUp, this.lineHalfWide + var3 + 2.5F, this.lineHalfTall + var4 + 2.0F);
         float var7 = this.titleHalfWide <= 0.05F
            ? 0.0F
            : smooth(0.0F, 3.0F, this.titleHalfWide)
               * box(Math.abs(wrapDelta(var1 - this.titleU)) * var5, var2 - this.titleUp, this.titleHalfWide + var3 + 2.5F, this.titleHalfTall + var4 + 2.0F);
         return Math.max(var6, var7);
      }
   }

   float clear(float var1, float var2) {
      float var3 = 1.0F - 1.0F * this.lyricBox(var1, var2, 0.8F, 0.8F);
      float var4 = var2 * 0.017453292F;
      float var5 = this.viewUp * 0.017453292F;
      float var6 = MathHelper.method_15374(var4) * MathHelper.method_15374(var5)
         + MathHelper.method_15362(var4) * MathHelper.method_15362(var5) * MathHelper.method_15362((var1 - this.aimU) * 6.2831855F);
      if (var6 < 0.93F) {
         return var3;
      } else {
         float var7 = (float)Math.acos(Math.min(1.0F, var6)) * 57.295776F;
         return var3 * smooth(9.0F, 20.0F, var7);
      }
   }

   private float aimClear(float var1, float var2, float var3, float var4) {
      float var5 = Math.abs(wrapDelta(var1 - this.aimU)) * 360.0F * MathHelper.method_15362((var2 + this.viewUp) * 0.5F * 0.017453292F);
      float var6 = Math.max(0.0F, var5 - var3);
      float var7 = Math.max(0.0F, Math.abs(var2 - this.viewUp) - var4);
      return smooth(6.0F, 15.0F, (float)Math.sqrt(var6 * var6 + var7 * var7));
   }

   private float keepOut(float var1, float var2, float var3, float var4) {
      return (1.0F - this.textRoom(var1, var2, var4, var3)) * this.aimClear(var1, var2, var4, var3);
   }

   private float textRoom(float var1, float var2, float var3, float var4) {
      if (!this.lyricsShown) {
         return 0.0F;
      } else {
         float var7 = 360.0F * MathHelper.method_15362(var2 * 0.017453292F);
         float var8 = 0.0F;
         if (this.lineHalfWide > 0.05F) {
            float var6 = Math.abs(wrapDelta(var1 - this.lyricU)) * var7;
            float var5 = smooth(0.0F, 3.0F, this.lineHalfWide);
            var8 = var5 * near(var6, this.lineHalfWide + var3) * nearUp(var2 - this.lyricUp, this.lineHalfTall + var4);
            if (var2 > this.lyricUp) {
               var8 = Math.max(var8, var5 * near(var6, 14.0F + var3));
            }
         }

         if (this.titleHalfWide > 0.05F) {
            float var11 = Math.abs(wrapDelta(var1 - this.titleU)) * var7;
            float var10 = smooth(0.0F, 3.0F, this.titleHalfWide);
            float var9 = var2 > this.titleUp ? 1.0F : nearUp(var2 - this.titleUp, this.titleHalfTall + var4);
            var8 = Math.max(var8, var10 * near(var11, this.titleHalfWide + var3) * var9);
         }

         return var8;
      }
   }

   private static float near(float var0, float var1) {
      return 1.0F - smooth(var1 + 3.0F, var1 + 3.0F + 5.0F, Math.abs(var0));
   }

   private static float nearUp(float var0, float var1) {
      return var0 >= 0.0F ? near(var0, var1) : 1.0F - smooth(var1 + 1.0F, var1 + 1.0F + 1.5F, -var0);
   }

   private static float box(float var0, float var1, float var2, float var3) {
      return (1.0F - smooth(var2 * 0.85F, var2, Math.abs(var0))) * (1.0F - smooth(var3 * 0.8F, var3, Math.abs(var1)));
   }

   private static float artShare(int var0) {
      return 1.0F - 52.0F / var0;
   }

   int kindAt(int var1) {
      return this.kinds[this.tTex[var1]];
   }

   private void layoutTattoos(float var1, float var2) {
      int var3 = this.tattoos.length;
      float var4 = this.w(PeepSong.CRY_ALONE);
      float var5 = this.w(PeepSong.GHOST_BOY);
      float var6 = this.w(PeepSong.GHOST_GIRL);
      float var7 = this.w(PeepSong.SPOTLIGHT);
      float var8 = MathHelper.method_16439(0.5F, 1.0F, this.look.light);
      float var9 = 1.0F + 0.1F * this.pulse;
      float var10 = smootherstep(this.fallFade);
      int var11 = 0;

      for (int var12 = 0; var12 < var3; var12++) {
         SkyImage var13 = this.tattoos[var12];
         int var14 = this.kinds[var12] % ELEVATION.length;
         float var15 = var1 * (1.0F - var4) * var8;
         if (!(var15 <= 0.003F)) {
            float var16 = 0.85F + 0.15F * MathHelper.method_15374(var2 * 2.8F + var12);
            float var17 = (float)var13.width() / var13.height();
            float var18 = MathHelper.method_22450((float)var12 / var3 + var2 * 0.02F);
            float var19 = ELEVATION[var14] + 2.2F * MathHelper.method_15374(var2 * 0.8F + var12 * 1.7F);
            float var20 = 1.0F;
            if (var10 > 0.0F) {
               float var21 = MathHelper.method_22450((var19 - 12.0F) / 52.0F - this.fall);
               var19 = MathHelper.method_16439(var10, var19, 12.0F + var21 * 52.0F);
               var20 = MathHelper.method_16439(var10, 1.0F, smooth(0.0F, 0.12F, var21) * (1.0F - smooth(0.88F, 0.98F, var21)));
            }

            if (var5 > 0.0F) {
               float var26 = (var2 * 0.42F + var12 * 3.5F) / 6.2831855F + 0.25F;
               int var22 = MathHelper.method_15375(var26);
               int var23 = var12 % HAUNT_AZIMUTH.length;
               float var24 = (HAUNT_HIGH[var23] ? 41.0F : 12.5F) + 6.0F * hash01(var12, var22, 1);
               float var25 = this.anchor(GHOST_SLOTS + var12, var22)
                  + (HAUNT_AZIMUTH[var23] + 8.0F * (hash01(var12, var22, 2) - 0.5F)) / (360.0F * MathHelper.method_15362(var24 * 0.017453292F));
               var18 = wrap(var18 + wrapDelta(var25 - var18) * var5);
               var19 = MathHelper.method_16439(var5, var19, var24);
            }

            float var27 = SIZE[var14] * (0.94F + 0.06F * MathHelper.method_15374(var2 * 2.0F + var12)) * (1.0F + 0.15F * var5);
            float var28 = this.keepOut(var18, var19, var27 * 0.5F * artShare(var13.height()), var27 * var17 * 0.5F * artShare(var13.width()));
            var11 = this.place(
               var11,
               var12,
               var18,
               var19,
               var27 / 60.0F,
               var17,
               0.1F * MathHelper.method_15374(var2 * 0.7F + var12 * 2.1F),
               var15 * (var20 * (var28 * smooth(0.15F, 0.45F, var28))) * var16,
               var2,
               var6,
               var5,
               var7,
               var9
            );
         }
      }

      this.shownTattoos = var11;
   }

   float clearOfTattoos(float var1, float var2, float var3) {
      float var4 = 1.0F;
      float var5 = 360.0F * MathHelper.method_15362(var2 * 0.017453292F);

      for (int var6 = 0; var6 < this.shownTattoos && var4 > 0.0F; var6++) {
         if (!(this.tGlow[var6] <= 0.05F)) {
            float var7 = (0.72F - this.ty[var6]) * 1.0471976F * 57.295776F;
            float var8 = Math.abs(wrapDelta(var1 - this.tu[var6])) * var5;
            float var9 = this.tw[var6] * 30.0F * 0.8F;
            float var10 = this.th[var6] * 30.0F * 0.8F;
            float var11 = Math.max(0.0F, var8 - var9 - var3);
            float var12 = Math.max(0.0F, Math.abs(var2 - var7) - var10 - var3);
            if (!(var11 > 1.5F) && !(var12 > 1.5F)) {
               var4 *= 1.0F - Math.min(1.0F, this.tGlow[var6]) * (1.0F - smooth(0.0F, 1.5F, (float)Math.sqrt(var11 * var11 + var12 * var12)));
            }
         }
      }

      return var4;
   }

   private void drawTattoos(SkyPaint var1, float var2) {
      int var5 = this.shownTattoos;
      float var6 = this.w(PeepSong.GHOST_BOY);
      float var7 = this.w(PeepSong.GHOST_GIRL);
      float var8 = this.w(PeepSong.SPOTLIGHT);
      float var9 = this.w(PeepSong.FOUR_GOLD_CHAINS);
      float var10 = this.w(PeepSong.WITCHBLADES);
      float var11 = 1.0F + 1.3F * this.pulse;
      int var12 = mix(mix(mix(16732080, 16757575, var9 * 0.4F), 14208255, var6 * 0.7F), 16763118, var7 * 0.3F);

      for (int var13 = 0; var13 < var5; var13++) {
         var1.glow(
            this.tu[var13],
            this.ty[var13],
            Math.max(this.th[var13], this.tw[var13]) * 0.6F,
            var12,
            0.22F * this.tGlow[var13] * var11 * (1.0F + 0.6F * this.tLit[var13]) * (1.0F + 0.5F * var6 * this.tGhost[var13])
         );
      }

      int var27 = mix(mix(16732080, 16740552, var7), 13150463, var6);
      float var14 = Math.min(1.0F, var7 + var6);
      float var15 = 1.0F - 0.7F * var6;
      float var16 = smooth(0.7F, 1.0F, this.look.light);

      for (int var4 = 0; var4 < var5; var4++) {
         int var17 = this.kindAt(var4);
         boolean var18 = var17 == 0;
         float var19 = this.filled(var4, var9, var6);
         float var20 = this.tAlpha[var4]
            * (var18 ? 0.3F : var15 * (1.0F - bodyBack(var17) * var19))
            * (1.0F - var14 * (1.0F - smooth(0.18F, 0.45F, this.tAlpha[var4])));
         float var21 = smooth(0.2F, 0.8F, this.tAlpha[var4])
            * var16
            * 0.7F
            * (1.0F - 0.5F * var9)
            * (1.0F - var19)
            * (1.0F - 0.7F * this.tLit[var4])
            * (1.0F - 0.5F * var7);
         int var3 = var18 ? mix(16732080, 16740296, var21) : mix(mix(var27, 16732080, var19), 16777215, var21);
         if (var6 > 0.0F) {
            var3 = mix(var3, mix(10121471, 14203135, this.tGhost[var4]), var6);
            var20 *= 1.0F - var6 * (1.0F - this.haunt(var4));
         }

         if (var9 > 0.0F) {
            var20 *= 1.0F - 0.35F * var9;
            var3 = mix(var3, 12720235, 0.4F * var9);
         }

         var1.sprite(this.tattoos[this.tTex[var4]], this.tu[var4], this.ty[var4], this.tw[var4], this.th[var4], this.tSpin[var4], tint(var20, var3), true);
      }

      if (var6 > 0.003F) {
         for (int var24 = 0; var24 < var5; var24++) {
            float var28 = 0.72F
               * this.tAlpha[var24]
               * var6
               * smooth(0.1F, 0.35F, this.tAlpha[var24])
               * (1.0F - 0.08F * smooth(0.8F, 1.0F, this.tGhost[var24]))
               * this.haunt(var24);
            if (!(var28 <= 0.003F)) {
               int var32 = mix(11697407, 13145855, this.tGhost[var24]);
               var1.sprite(
                  this.veils[this.tTex[var24]], this.tu[var24], this.ty[var24], this.tw[var24], this.th[var24], this.tSpin[var24], tint(var28, var32), true
               );
            }
         }
      }

      if (var9 > 0.003F) {
         for (int var25 = 0; var25 < var5; var25++) {
            float var29 = var9 * smooth(0.12F, 0.6F, this.tGlow[var25]);
            if (!(var29 <= 0.003F)) {
               var1.sprite(
                  this.golds[this.tTex[var25]],
                  this.tu[var25],
                  this.ty[var25],
                  this.tw[var25],
                  this.th[var25],
                  this.tSpin[var25],
                  tint(0.95F * var29, 4859392),
                  false
               );
               var1.sprite(
                  this.golds[this.tTex[var25]],
                  this.tu[var25],
                  this.ty[var25],
                  this.tw[var25],
                  this.th[var25],
                  this.tSpin[var25],
                  tint(0.9F * var29, 16769658),
                  true
               );
            }
         }
      }

      for (int var26 = 0; var26 < var5; var26++) {
         float var30 = this.filled(var26, var9, var6);
         if (!(var30 <= 0.003F)) {
            float var33 = (this.kindAt(var26) == 9 ? 0.92F : 0.8F) * Math.min(1.0F, var30 * 1.35F) * this.tGlow[var26];
            var1.sprite(
               this.golds[this.tTex[var26]], this.tu[var26], this.ty[var26], this.tw[var26], this.th[var26], this.tSpin[var26], tint(var33, 16749268), true
            );
         }
      }

      int var31 = mix(16732080, 12099839, var6);

      for (int var34 = 0; var34 < var5; var34++) {
         float var35 = var8 * this.tLit[var34] * 0.35F;
         float var36 = var6 * this.tGhost[var34] * 0.26F;
         float var37 = this.kindAt(var34) == 0 ? 0.6F * (1.0F - var9) * (1.0F - var6) : 0.0F;
         float var22 = var35 + var36 + var37;
         if (!(var22 <= 0.003F)) {
            int var23 = mix(var31, average(16743115, var35, 15916287, var36, 16732080, var37), smooth(0.2F, 0.8F, this.tGlow[var34]));
            var1.sprite(
               this.rims[this.tTex[var34]],
               this.tu[var34],
               this.ty[var34],
               this.tw[var34],
               this.th[var34],
               this.tSpin[var34],
               tint(Math.min(1.0F, var22) * this.tGlow[var34], var23),
               true
            );
         }
      }

      if (var10 > 0.0F) {
         PeepEffects.orbits(this, var1, var2, var10);
      }

      if (var9 > 0.0F) {
         PeepEffects.goldSparkles(this, var1, var2, var9);
      }
   }

   private static float bodyBack(int var0) {
      return var0 == 9 ? 0.45F : 0.25F;
   }

   private float haunt(int var1) {
      boolean var2 = HAUNT_HIGH[this.tTex[var1] % HAUNT_HIGH.length];
      return (var2 ? 1.0F : 0.8F) * (1.0F - 0.4F * this.hazeAt(this.ty[var1]));
   }

   private float filled(int var1, float var2, float var3) {
      float var4 = this.kindAt(var1) == 9 ? 0.6F : 0.0F;
      return var4 * (1.0F - var2) * (1.0F - var3);
   }

   private int place(
      int var1,
      int var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      float var13
   ) {
      if (var1 >= MAX_SHOWN) {
         return var1;
      } else {
         if (var10 > 0.0F) {
            var3 += var10
               * 0.35F
               * (MathHelper.method_15374(var9 * 2.3F + var2 * 1.1F) + 0.5F * MathHelper.method_15374(var9 * 3.7F + var2 * 2.9F))
               / (360.0F * Math.max(0.2F, MathHelper.method_15362(var4 * 0.017453292F)));
            var4 += var10 * 0.3F * (MathHelper.method_15374(var9 * 2.9F + var2 * 1.7F) + 0.5F * MathHelper.method_15374(var9 * 4.3F + var2));
            var8 *= 1.0F - var10 * 0.15F * (0.5F + 0.5F * MathHelper.method_15374(var9 * 3.1F + var2));
         }

         float var14 = 1.0F;
         if (var11 > 0.0F) {
            float var15 = 0.5F + 0.5F * MathHelper.method_15374(var9 * 0.42F + var2 * 3.5F);
            var14 = smootherstep(smooth(0.38F, 0.68F, var15));
            var4 += var11 * 1.5F * var15;
         }

         float var17 = var12 > 0.0F ? var12 * PeepEffects.spotlightOn(this, var3, var4) : 0.0F;
         float var16 = var13 * (1.0F + 0.06F * var17);
         this.tTex[var1] = var2;
         this.tu[var1] = wrap(var3);
         this.ty[var1] = elevationToY(var4);
         this.th[var1] = var5 * var16;
         this.tw[var1] = var5 * var6 * var16;
         this.tSpin[var1] = var7;
         this.tGhost[var1] = var14;
         this.tLit[var1] = var17;
         this.tAlpha[var1] = Math.min(var8 * MathHelper.method_16439(var11, 1.0F, var14 * 0.85F), 1.0F - 0.3F * var11);
         this.tGlow[var1] = var8 * MathHelper.method_16439(var11, 1.0F, var14);
         return var1 + 1;
      }
   }

   private void blendLook() {
      float[] var1 = this.look.rgb;
      Arrays.fill(var1, 0.0F);
      float var2 = 1.0F;
      float var3 = 0.0F;
      float var4 = 0.0F;
      float var5 = 0.0F;

      for (PeepSong var9 : PeepSong.ALL) {
         float var10 = this.shown[var9.ordinal()];
         if (!(var10 <= 0.0F)) {
            var2 -= var10;
            PeepSky.Palette var11 = PALETTES[var9.ordinal()];
            add(var1, var11, var10);
            var3 += var10 * var11.glowStrength();
            var4 += var10 * var11.light();
            var5 += var10 * var11.starLight();
         }
      }

      var2 = Math.max(0.0F, var2);
      add(var1, BASE, var2);
      this.look.glowStrength = var3 + var2 * BASE.glowStrength();
      this.look.light = var4 + var2 * BASE.light();
      this.look.starLight = var5 + var2 * BASE.starLight();
      this.look.topColor = packed(var1, 0);
      this.look.deepColor = packed(var1, 1);
      this.look.duskColor = packed(var1, 2);
      this.look.nightColor = packed(var1, 3);
      this.look.hazeColor = packed(var1, 4);
      this.look.hazeLight = MathHelper.method_15363((0.6F * luma(this.look.hazeColor) + this.look.glowStrength * luma(packed(var1, 6))) / 255.0F, 0.0F, 1.0F);
      this.look.rimColor = packed(var1, 5);
      this.look.glowColor = packed(var1, 6);
   }

   private static void add(float[] var0, PeepSky.Palette var1, float var2) {
      if (!(var2 <= 0.0F)) {
         add(var0, 0, var1.top(), var2);
         add(var0, 1, var1.deep(), var2);
         add(var0, 2, var1.dusk(), var2);
         add(var0, 3, var1.night(), var2);
         add(var0, 4, var1.haze(), var2);
         add(var0, 5, var1.rim(), var2);
         add(var0, 6, var1.glow(), var2);
      }
   }

   private static void add(float[] var0, int var1, int var2, float var3) {
      int var4 = var1 * 3;
      var0[var4] += var3 * (var2 >> 16 & 0xFF);
      int var5 = var1 * 3 + 1;
      var0[var5] += var3 * (var2 >> 8 & 0xFF);
      int var6 = var1 * 3 + 2;
      var0[var6] += var3 * (var2 & 0xFF);
   }

   private static int packed(float[] var0, int var1) {
      return MathHelper.method_15340(Math.round(var0[var1 * 3]), 0, 255) << 16
         | MathHelper.method_15340(Math.round(var0[var1 * 3 + 1]), 0, 255) << 8
         | MathHelper.method_15340(Math.round(var0[var1 * 3 + 2]), 0, 255);
   }

   float hazeAt(float var1) {
      return MathHelper.method_15363(1.0F - Math.max(0.0F, 0.72F - var1) / 0.4F, 0.0F, 1.0F) * this.look.hazeLight;
   }

   private static float luma(int var0) {
      return 0.2126F * (var0 >> 16 & 0xFF) + 0.7152F * (var0 >> 8 & 0xFF) + 0.0722F * (var0 & 0xFF);
   }

   private int shade(float var1, float var2, float var3) {
      float var4 = Math.max(0.0F, var1);
      int var5 = var4 < 0.05F
         ? mix(this.look.topColor, this.look.deepColor, var4 / 0.05F)
         : (
            var4 < 0.3F
               ? mix(this.look.deepColor, this.look.duskColor, (var4 - 0.05F) / 0.25F)
               : mix(this.look.duskColor, this.look.nightColor, Math.min(1.0F, (var4 - 0.3F) / 0.5F))
         );
      float var6 = MathHelper.method_15363(1.0F - Math.max(0.0F, var3 - var2) / 0.4F, 0.0F, 1.0F);
      var5 = mix(var5, this.look.hazeColor, var6 * 0.6F);
      return tint(var4 < 0.02F ? 0.92F : 0.97F, var5);
   }

   static float heartbeat(float var0) {
      return Math.max(beat(var0, 0.17F), 0.6F * beat(var0 - 0.3F, 0.42F));
   }

   private static float beat(float var0, float var1) {
      if (var0 < 0.0F) {
         return 0.0F;
      } else {
         return var0 < 0.14F ? smooth(0.0F, 0.14F, var0) : (float)Math.exp(-(var0 - 0.14F) / var1);
      }
   }

   private static int average(int var0, float var1, int var2, float var3, int var4, float var5) {
      float var6 = Math.max(var1 + var3 + var5, 1.0E-4F);
      int var7 = Math.round(((var0 >> 16 & 0xFF) * var1 + (var2 >> 16 & 0xFF) * var3 + (var4 >> 16 & 0xFF) * var5) / var6);
      int var8 = Math.round(((var0 >> 8 & 0xFF) * var1 + (var2 >> 8 & 0xFF) * var3 + (var4 >> 8 & 0xFF) * var5) / var6);
      int var9 = Math.round(((var0 & 0xFF) * var1 + (var2 & 0xFF) * var3 + (var4 & 0xFF) * var5) / var6);
      return MathHelper.method_15340(var7, 0, 255) << 16 | MathHelper.method_15340(var8, 0, 255) << 8 | MathHelper.method_15340(var9, 0, 255);
   }

   static float elevationToY(float var0) {
      return 0.72F - var0 * 0.017453292F / 1.0471976F;
   }

   static float hash01(int var0) {
      int var1 = var0 * -1640531535;
      var1 ^= var1 >>> 15;
      var1 *= -2048144777;
      var1 ^= var1 >>> 13;
      var1 *= -1028477379;
      var1 ^= var1 >>> 16;
      return (var1 >>> 8) * 5.9604645E-8F;
   }

   static float hash01(int var0, int var1, int var2) {
      return hash01(var0 * 73856093 ^ var1 * 19349663 ^ var2 * 83492791);
   }

   static int tint(float var0, int var1) {
      return ColorHelper.method_71346(MathHelper.method_15363(var0, 0.0F, 1.0F), var1);
   }

   static int mix(int var0, int var1, float var2) {
      return ColorHelper.method_61319(MathHelper.method_15363(var2, 0.0F, 1.0F), var0, var1) & 16777215;
   }

   static float smootherstep(float var0) {
      return var0 * var0 * var0 * (var0 * (var0 * 6.0F - 15.0F) + 10.0F);
   }

   static float smooth(float var0, float var1, float var2) {
      float var3 = MathHelper.method_15363((var2 - var0) / (var1 - var0), 0.0F, 1.0F);
      return var3 * var3 * (3.0F - 2.0F * var3);
   }

   static float wrap(float var0) {
      return var0 - (float)Math.floor(var0);
   }

   static float wrapDelta(float var0) {
      return var0 - Math.round(var0);
   }

   private static PeepSky.Built build(int var0) {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = new ArrayList();
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      ArrayList var5 = new ArrayList();
      ArrayList var6 = new ArrayList();
      ArrayList var7 = new ArrayList();

      for (int var8 = 0; var8 < KINDS.length; var8++) {
         BufferedImage var9 = load(KINDS[var8]);
         if (var9 != null) {
            float var10 = Math.min(3.0F, 320.0F / Math.max(var9.getWidth(), var9.getHeight()));
            int var11 = Math.round(var9.getWidth() * var10) + 52;
            int var12 = Math.round(var9.getHeight() * var10) + 52;
            BufferedImage var13 = new BufferedImage(var11, var12, 2);
            Graphics2D var14 = var13.createGraphics();
            var14.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            var14.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            var14.drawImage(var9, 26, 26, var11 - 52, var12 - 52, null);
            var14.dispose();
            int[] var15 = var13.getRGB(0, 0, var11, var12, null, 0, var11);
            var1.add(SkyMips.chain(neon(var15, var11, var12), var11, var12));
            var2.add(SkyMips.chain(PeepArt.rim(var15, var11, var12), var11, var12));
            var3.add(SkyMips.chain(PeepArt.gold(var15, var11, var12), var11, var12));
            var4.add(SkyMips.chain(PeepArt.veil(var15, var11, var12), var11, var12));
            var5.add(var11);
            var6.add(var12);
            var7.add(var8);
         }
      }

      PeepArt.Art[] var16 = PeepArt.drawAll();
      int[][][] var17 = new int[var16.length][][];

      for (int var18 = 0; var18 < var16.length; var18++) {
         var17[var18] = SkyMips.chain(var16[var18].argb(), var16[var18].width(), var16[var18].height());
      }

      return new PeepSky.Built(
         var0,
         var1.toArray(new int[0][][]),
         var2.toArray(new int[0][][]),
         var3.toArray(new int[0][][]),
         var4.toArray(new int[0][][]),
         var5.stream().mapToInt(Integer::intValue).toArray(),
         var6.stream().mapToInt(Integer::intValue).toArray(),
         var7.stream().mapToInt(Integer::intValue).toArray(),
         var16,
         var17
      );
   }

   private static BufferedImage load(String var0) {
      try {
         BufferedImage var3;
         try (InputStream var1 = PeepSky.class.getResourceAsStream("/assets/efn/textures/peep/" + var0 + ".png")) {
            if (var1 == null) {
               LOGGER.warn("[EFN] Sky easter egg tattoo {} is missing", var0);
               return null;
            }

            BufferedImage var2 = ImageIO.read(var1);
            var3 = var2;
         }

         return var3;
      } catch (IOException var6) {
         LOGGER.warn("[EFN] Sky easter egg tattoo {} couldn't be read", var0, var6);
         return null;
      }
   }

   private static int[] neon(int[] var0, int var1, int var2) {
      float[] var3 = new float[var1 * var2];

      for (int var4 = 0; var4 < var3.length; var4++) {
         var3[var4] = (var0[var4] >>> 24) / 255.0F;
      }

      float[] var13 = blur(var3, var1, var2, 9);
      float[] var5 = new float[var1 * var2];

      for (int var6 = 0; var6 < var5.length; var6++) {
         var5[var6] = smooth(0.6F, 0.9F, var3[var6]);
      }

      float[] var14 = blur(var5, var1, var2, 7);
      float[] var7 = blur(var5, var1, var2, 1);
      int[] var8 = new int[var1 * var2];

      for (int var9 = 0; var9 < var8.length; var9++) {
         float var10 = Math.max(var3[var9], Math.min(1.0F, var7[var9] * 1.6F) * (1.0F - smooth(0.2F, 0.4F, var14[var9])));
         float var11 = Math.min(1.0F, var13[var9] * 2.6F);
         float var12 = smooth(0.7F, 1.0F, var10) * (1.0F - 0.75F * smooth(0.45F, 0.8F, var14[var9]));
         var8[var9] = tint(Math.max(var10, var11 * 0.85F), mix(16732080, 16766704, var12));
      }

      return var8;
   }

   static float[] blur(float[] var0, int var1, int var2, int var3) {
      float[] var4 = (float[])var0.clone();
      float[] var5 = new float[var0.length];

      for (int var6 = 0; var6 < 2; var6++) {
         for (int var8 = 0; var8 < var2; var8++) {
            float var7 = 0.0F;

            for (int var9 = -var3; var9 <= var3; var9++) {
               var7 += var4[var8 * var1 + MathHelper.method_15340(var9, 0, var1 - 1)];
            }

            for (int var12 = 0; var12 < var1; var12++) {
               var5[var8 * var1 + var12] = var7 / (var3 * 2 + 1);
               var7 += var4[var8 * var1 + Math.min(var1 - 1, var12 + var3 + 1)] - var4[var8 * var1 + Math.max(0, var12 - var3)];
            }
         }

         for (int var11 = 0; var11 < var1; var11++) {
            float var10 = 0.0F;

            for (int var13 = -var3; var13 <= var3; var13++) {
               var10 += var5[MathHelper.method_15340(var13, 0, var2 - 1) * var1 + var11];
            }

            for (int var14 = 0; var14 < var2; var14++) {
               var4[var14 * var1 + var11] = var10 / (var3 * 2 + 1);
               var10 += var5[Math.min(var2 - 1, var14 + var3 + 1) * var1 + var11] - var5[Math.max(0, var14 - var3) * var1 + var11];
            }
         }
      }

      return var4;
   }

   static {
      for (PeepSong var3 : PeepSong.ALL) {
         PALETTES[var3.ordinal()] = palette(var3);
      }
   }

   @Environment(EnvType.CLIENT)
   private record Built(
      int generation,
      int[][][] tattoos,
      int[][][] rims,
      int[][][] golds,
      int[][][] veils,
      int[] widths,
      int[] heights,
      int[] kinds,
      PeepArt.Art[] art,
      int[][][] artLevels
   ) {
   }

   @Environment(EnvType.CLIENT)
   private static final class Look {
      final float[] rgb = new float[21];
      float glowStrength;
      float light;
      float starLight;
      float hazeLight;
      int topColor;
      int deepColor;
      int duskColor;
      int nightColor;
      int hazeColor;
      int rimColor;
      int glowColor;

      private Look() {
      }
   }

   @Environment(EnvType.CLIENT)
   private record Palette(int top, int deep, int dusk, int night, int haze, int rim, int glow, float glowStrength, float light, float starLight) {
   }
}
