package net.efn.client.module.impl.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.efn.client.module.impl.client.SpotifyHudModule;
import net.efn.client.util.spotify.Lyrics;
import net.efn.client.util.spotify.NowPlaying;
import net.efn.client.util.spotify.SyncedLyrics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class SkyLyrics {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final SkyLyrics INSTANCE = new SkyLyrics();
   private static final String FONT = "/assets/efn/fonts/manrope-bold.ttf";
   private static final long LEAD_MS = 200L;
   private static final float LINE_DEGREES = 7.0F;
   private static final float TITLE_DEGREES = 4.0F;
   private static final float TITLE_GAP_DEGREES = 0.6F;
   private static final float MAX_WIDTH_DEGREES = 140.0F;
   private static final float RISE_DEGREES = 1.6F;
   private static final float FADE_RATE = 2.6F;
   private static final float FOLLOW_RATE = 2.2F;
   private static final float LINE_PX = 96.0F;
   private static final float TITLE_PX = 64.0F;
   private static final float GLOW_ALPHA = 0.8F;
   private static final int PAD = 28;
   private static final int MAX_TEXTURE = 4096;
   private static final int BLUR = 10;
   private static final int SEGMENTS = 32;
   private static final int FALLBACK_GLOW = 9063935;
   private static final float EMOJI_SCALE = 1.0F;
   private static final float EMOJI_GAP_DEGREES = 0.8F;
   private static final long EMOJI_COOLDOWN_NS = 5000000000L;
   private static final float WORD_EMOJI_EM = 1.6F;
   private static final float BEHIND_SCALE = 2.8F;
   private static final float BEHIND_ALPHA = 0.32F;
   private static final float PEEP_IN_SECONDS = 5.0F;
   private static final float PEEP_OUT_SECONDS = 1.8F;
   private static final float PEEP_KEEP_SECONDS = 30.0F;
   private static final double PEEP_CLOCK_WRAP = 21600.0;
   private static final int PEEP_LINE = -24876;
   private static final int PEEP_TITLE = -34104;
   private static final int PEEP_GLOW = 16723349;
   private final ExecutorService raster = Executors.newSingleThreadExecutor(runnable -> {
      Thread var1 = new Thread(runnable, "EFN Sky Lyrics");
      var1.setDaemon(true);
      return var1;
   });
   private final SkyLyrics.Slot line = new SkyLyrics.Slot(7.0F, 96.0F);
   private final SkyLyrics.Slot title = new SkyLyrics.Slot(4.0F, 64.0F);
   private SkyCanvas canvas;
   private Font font;
   private boolean fontMissing;
   private boolean failed;
   private final Map<SkyEmoji, SkyTexture> emojiTextures = new EnumMap<>(SkyEmoji.class);
   private final Set<SkyEmoji> uploadedEmojis = ConcurrentHashMap.newKeySet();
   private float viewU = 0.0F / 0.0F;
   private long lastFrame;
   private long lastEmojiAt;
   private boolean emojiRight = true;
   private float emojiScale = 1.0F;
   private float glowAlpha = 0.8F;
   private int serial;
   private final PeepSky peep = new PeepSky();
   private float peepLevel;
   private float peepIdle;
   private double peepClock;
   private String peepTitle;
   private PeepSong peepSong;

   private SkyLyrics() {
   }

   public static void render() {
      INSTANCE.draw();
   }

   private void draw() {
      if (!this.failed) {
         long var1 = System.nanoTime();
         float var3 = this.lastFrame == 0L ? 0.0F : Math.min((float)(var1 - this.lastFrame) / 1.0E9F, 0.1F);
         this.lastFrame = var1;
         SpotifyHudModule var4 = SpotifyHudModule.active();
         boolean var5 = var4 != null && var4.peepPlaying();
         this.peepLevel = var5 ? Math.min(1.0F, this.peepLevel + var3 / 5.0F) : Math.max(0.0F, this.peepLevel - var3 / 1.8F);
         boolean var6 = var5 && var4.showLyrics();
         boolean var7 = var4 != null && (var4.skyLyrics() || var6);
         float var8 = this.peepIdle = !var5 && !(this.peepLevel > 0.0F) ? this.peepIdle + var3 : 0.0F;
         if (this.peepLevel <= 0.0F && !var5) {
            this.peep.restart();
            this.peepClock = 0.0;
            if (this.peepIdle > 30.0F && this.peep.holding()) {
               this.peep.close();
            }
         }

         if (var7 || !this.line.idle() || !this.title.idle() || !(this.peepLevel <= 0.0F)) {
            try {
               if (this.canvas == null) {
                  this.canvas = new SkyCanvas();
               }

               Camera var13 = MinecraftClient.getInstance().gameRenderer.getCamera();
               float var14 = MathHelper.floorMod(var13.getYaw() / 360.0F + 0.25F, 1.0F);
               float var15 = this.viewU = Float.isNaN(this.viewU) ? var14 : wrap(this.viewU + wrapDelta(var14 - this.viewU) * Math.min(1.0F, var3 * 2.2F));
               if (this.peepLevel > 0.0F) {
                  NowPlaying var16 = var4 != null ? var4.nowPlaying() : null;
                  if (var5 && !var16.title().equals(this.peepTitle)) {
                     this.peepTitle = var16.title();
                     this.peepSong = PeepSong.of(this.peepTitle);
                  }

                  this.peep.ready(this.canvas, this.raster);
                  this.peepLyrics(var7 ? var4 : null);
                  this.peep.update(var3, this.peepSong, this.peepLevel, this.viewU, var14, -var13.getPitch(), var6 ? linePulse(var4, var16) : 0.0F);
                  this.peepClock = (this.peepClock + var3) % 21600.0;
                  this.peep.draw(this.canvas, this.peepLevel, (float)this.peepClock);
                  this.canvas.flush(1.0F);
               }

               NowPlaying var25 = var7 ? var4.nowPlaying() : null;
               boolean var17 = var25 != null && var25.playing();
               this.line.want(var7 ? currentLine(var4, var25) : "", var7 && var4.skyEmojis() ? var4.skyEmojiStyle() : null, var7 ? var4.skyEmojiSize() : 1.0F);
               this.title.want(var7 && var17 && var4.skyTitle() ? var25.title().strip() : "", null, 1.0F);
               this.line.step(var3, this.viewU);
               this.title.step(var3, this.viewU);
               if (this.line.idle() && this.title.idle()) {
                  return;
               }

               boolean var18 = !var7 || var4.skyFollow();
               float var19 = var7 ? var4.skySize() : 1.0F;
               float var20 = var7 ? var4.skyTitleSize() : 1.0F;
               this.emojiScale = var7 ? var4.skyEmojiSize() : 1.0F;
               this.glowAlpha = var7 ? var4.skyGlow() : 0.8F;
               float var21 = var7 ? var4.skyHeight() : 28.0F;
               float var12 = var7 ? var4.skyOpacity() : 0.9F;
               int var11 = var6 ? -24876 : (var7 ? var4.skyTextColor() : -1);
               int var23 = var6 ? -34104 : (var7 ? var4.skyTitleColor() : -1);
               AtmosphereModule var10 = var7 && !var6 && var4.skyGlowAtmosphere() ? AtmosphereModule.get() : null;
               int var9 = var6 ? 16723349 : (var10 != null ? var10.lyricGlow() : (var7 ? var4.skyGlowColor() : 9063935));
               if ((var9 & 16777215) == 0 && var10 != null) {
                  var9 = 9063935;
               }

               this.drawSlot(this.line, var18, var21, var19, var11, var9);
               this.drawSlot(this.title, var18, titleElevation(var21, var19, var20), var20, var23, var9);
               this.canvas.flush(var12);
            } catch (RuntimeException var24) {
               this.failed = true;
               if (this.canvas != null) {
                  this.canvas.clear();
               }

               this.line.close();
               this.title.close();
               this.peep.close();
               LOGGER.error("[EFN] Sky lyrics stopped drawing", (Throwable)var24);
            }
         }
      }
   }

   private static float titleElevation(float f, float g, float h) {
      return f + (7.0F * g + 4.0F * h) * 0.5F + 0.6F * Math.max(g, h);
   }

   private void peepLyrics(SpotifyHudModule spotifyHudModule) {
      if (spotifyHudModule == null) {
         this.peep.lyrics(this.viewU, 0.0F / 0.0F, 0.0F, 0.0F, this.viewU, 0.0F / 0.0F, 0.0F, 0.0F);
      } else {
         float var2 = spotifyHudModule.skySize();
         float var3 = spotifyHudModule.skyTitleSize();
         float var4 = spotifyHudModule.skyHeight();
         SkyLyrics.Shown var5 = this.line.current != null ? this.line.current : this.line.leaving;
         SkyLyrics.Shown var6 = this.title.current != null ? this.title.current : this.title.leaving;
         boolean var7 = spotifyHudModule.skyFollow();
         float var8 = !var7 && var5 != null ? var5.u : this.viewU;
         float var9 = !var7 && var6 != null ? var6.u : var8;
         this.peep
            .lyrics(
               var8,
               var4,
               halfWidth(var5, 7.0F * var2),
               7.0F * var2 * 0.5F,
               var9,
               titleElevation(var4, var2, var3),
               halfWidth(var6, 4.0F * var3),
               4.0F * var3 * 0.5F
            );
      }
   }

   private static float halfWidth(SkyLyrics.Shown shown, float f) {
      return shown == null ? 0.0F : Math.min(f * shown.aspect, 140.0F) * 0.5F;
   }

   private void drawSlot(SkyLyrics.Slot slot, boolean bl, float f, float g, int i, int j) {
      SkyLyrics.Shown var7 = slot.leaving;
      SkyLyrics.Shown var8 = slot.current;
      if (var7 != null) {
         this.drawShown(var7, bl, f + (1.0F - var7.alpha) * 1.6F * g, slot.degrees * g, i, j);
      }

      if (var8 != null) {
         float var9 = 1.0F - var8.alpha;
         this.drawShown(var8, bl, f - var9 * var9 * 1.6F * g, slot.degrees * g, i, j);
      }
   }

   private void drawShown(SkyLyrics.Shown shown, boolean bl, float f, float g, int i, int j) {
      float var7 = MathHelper.clamp(shown.alpha, 0.0F, 1.0F);
      if (!(var7 <= 0.003F)) {
         float var8 = g;
         float var9 = g * shown.aspect;
         if (var9 > 140.0F) {
            var8 = g * (140.0F / var9);
            var9 = 140.0F;
         }

         float var10 = f * 0.017453292F;
         float var11 = 0.72F - var10 / 1.0471976F;
         float var12 = var8 * 0.017453292F / 1.0471976F * 0.5F;
         float var13 = var9 / (360.0F * Math.max(0.2F, MathHelper.cos(var10))) * 0.5F;
         float var14 = bl ? this.viewU : shown.u;
         float var15 = var11 - var12;
         float var16 = var11 + var12;
         float var17 = !(shown.alpha >= 1.0F) && shown == this.line.current ? backOut(var7) : var7;
         float var18 = (float)(System.nanoTime() % 3600000000000L) / 1.0E9F;
         if (shown.emoji != null && shown.style == SpotifyHudModule.SkyEmojiStyle.BEHIND) {
            float var19 = var8 * 2.8F * this.emojiScale * var17 * 0.017453292F / 1.0471976F;
            this.canvas
               .sprite(shown.emoji, var14, var11, var19, var19, MathHelper.sin(var18 * 0.9F + shown.phase) * 0.05F, withAlpha(16777215, var7 * 0.32F), false);
         }

         if (this.glowAlpha > 0.0F) {
            this.canvas.band(shown.glow, var14 - var13, var14 + var13, var15, var16, withAlpha(j, var7 * this.glowAlpha), true, 32);
         }

         this.canvas.band(shown.fill, var14 - var13, var14 + var13, var15, var16, withAlpha(i, var7 * (i >>> 24) / 255.0F), false, 32);
         if (shown.emoji != null && shown.style != SpotifyHudModule.SkyEmojiStyle.BEHIND) {
            if (shown.style == SpotifyHudModule.SkyEmojiStyle.REPLACE_WORD) {
               float var27 = MathHelper.sin(var18 * 2.2F + shown.phase) * var8 * 0.03F * 0.017453292F / 1.0471976F;
               float var28 = shown.emojiSize * (var16 - var15) * var17;
               this.canvas
                  .sprite(
                     shown.emoji,
                     var14 - var13 + shown.emojiX * var13 * 2.0F,
                     var15 + shown.emojiY * (var16 - var15) - var27,
                     var28,
                     var28,
                     MathHelper.sin(var18 * 1.7F + shown.phase) * 0.06F,
                     withAlpha(16777215, var7),
                     false
                  );
            } else {
               float var26 = var8 * 1.0F * this.emojiScale;
               float var20 = var26 * var17;
               float var21 = (var26 * 0.5F + 0.8F) / (360.0F * Math.max(0.2F, MathHelper.cos(var10)));
               float var22 = MathHelper.sin(var18 * 2.2F + shown.phase) * var8 * 0.06F;
               float var23 = 0.72F - (f + var22) * 0.017453292F / 1.0471976F;
               float var24 = var20 * 0.017453292F / 1.0471976F;
               float var25 = MathHelper.sin(var18 * 1.7F + shown.phase) * 0.12F;
               this.canvas
                  .sprite(shown.emoji, var14 + (shown.right ? 1.0F : -1.0F) * (var13 + var21), var23, var24, var24, var25, withAlpha(16777215, var7), false);
            }
         }
      }
   }

   private SkyLyrics.Shown show(SkyLyrics.Raster raster, float f) {
      int var3 = this.serial++;
      SkyTexture var4 = SkyTexture.upload("sky_lyrics_" + var3, raster.width(), raster.height(), raster.fill());
      SkyTexture var5 = SkyTexture.upload("sky_lyrics_glow_" + var3, raster.width(), raster.height(), raster.glow());
      SkyLyrics.Shown var6 = new SkyLyrics.Shown(var4, var5, (float)raster.width() / raster.height(), f);
      SkyEmoji var7 = raster.emoji();
      if (var7 == null) {
         return var6;
      } else {
         SkyTexture var8 = this.emojiTextures.get(var7);
         if (var8 == null && raster.emojiPixels() != null) {
            var8 = SkyTexture.upload("sky_emoji_" + var7.name().toLowerCase(Locale.ROOT), 256, 256, raster.emojiPixels());
            this.emojiTextures.put(var7, var8);
            this.uploadedEmojis.add(var7);
         }

         if (var8 == null) {
            return var6;
         } else {
            var6.emoji = var8;
            var6.style = raster.style();
            var6.emojiX = raster.emojiX();
            var6.emojiY = raster.emojiY();
            var6.emojiSize = raster.emojiSize();
            if (raster.style() == SpotifyHudModule.SkyEmojiStyle.BESIDE) {
               var6.right = this.emojiRight;
               this.emojiRight = !this.emojiRight;
            }

            this.lastEmojiAt = System.nanoTime();
            return var6;
         }
      }
   }

   private static float backOut(float f) {
      float var1 = 1.70158F;
      float var2 = f - 1.0F;
      return 1.0F + (var1 + 1.0F) * var2 * var2 * var2 + var1 * var2 * var2;
   }

   private static String currentLine(SpotifyHudModule spotifyHudModule, NowPlaying nowPlaying) {
      if (nowPlaying != null && nowPlaying.playing()) {
         Lyrics var2 = spotifyHudModule.lyricsFor(nowPlaying);
         if (var2 != null && var2.status() == Lyrics.Status.SYNCED) {
            SyncedLyrics var3 = var2.synced();
            int var4 = var3.indexAt(nowPlaying.positionAt(System.currentTimeMillis()) + 200L);
            if (var4 < 0) {
               return "";
            } else {
               StringBuilder var5 = new StringBuilder();

               for (int var6 = var3.firstWithTimeOf(var4); var6 <= var3.lastWithTimeOf(var4); var6++) {
                  String var7 = var3.line(var6).text().strip();
                  if (!var7.isEmpty()) {
                     if (!var5.isEmpty()) {
                        var5.append("  \u00b7  ");
                     }

                     var5.append(var7);
                  }
               }

               return var5.toString();
            }
         } else {
            return "";
         }
      } else {
         return "";
      }
   }

   private static float linePulse(SpotifyHudModule spotifyHudModule, NowPlaying nowPlaying) {
      if (nowPlaying != null && nowPlaying.playing()) {
         Lyrics var2 = spotifyHudModule.lyricsFor(nowPlaying);
         if (var2 != null && var2.status() == Lyrics.Status.SYNCED) {
            SyncedLyrics var3 = var2.synced();
            long var4 = nowPlaying.positionAt(System.currentTimeMillis()) + 200L;
            int var6 = var3.indexAt(var4);
            float var7 = 0.0F;

            for (int var8 = 0; var8 < 2 && var6 >= 0; var8++) {
               int var9 = var3.firstWithTimeOf(var6);
               if (hasWords(var3, var9, var3.lastWithTimeOf(var6))) {
                  var7 = Math.max(var7, PeepSky.heartbeat((float)(var4 - var3.line(var9).timeMs()) / 1000.0F));
               }

               var6 = var9 - 1;
            }

            return var7;
         } else {
            return 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   private static boolean hasWords(SyncedLyrics syncedLyrics, int i, int j) {
      for (int var3 = i; var3 <= j; var3++) {
         if (!syncedLyrics.line(var3).text().isBlank()) {
            return true;
         }
      }

      return false;
   }

   private SkyLyrics.Raster rasterize(int i, String string, float f, SpotifyHudModule.SkyEmojiStyle skyEmojiStyle, float g) {
      SkyEmoji.Match var7 = skyEmojiStyle != null ? SkyEmoji.match(string) : null;
      SkyEmoji var8 = var7 != null ? var7.emoji() : null;
      int[] var6 = var8 != null && !this.uploadedEmojis.contains(var8) ? var8.draw() : null;
      if (var6 == null && var8 != null && !this.uploadedEmojis.contains(var8)) {
         var8 = null;
      }

      boolean var10 = var8 != null && skyEmojiStyle == SpotifyHudModule.SkyEmojiStyle.REPLACE_WORD;
      String var11 = var10 ? string.substring(0, var7.start()) : string;
      String var12 = var10 ? string.substring(var7.end()) : "";
      Font var13 = this.fontFor(string);
      BufferedImage var14 = new BufferedImage(1, 1, 2);
      Graphics2D var15 = var14.createGraphics();
      hints(var15);
      Font var16 = var13.deriveFont(f);
      FontMetrics var17 = var15.getFontMetrics(var16);
      float var18 = var10 ? f * 1.6F * g : 0.0F;
      int var19 = Math.max(1, var17.stringWidth(var11) + Math.round(var18) + var17.stringWidth(var12));
      if (var19 + 56 > 4096) {
         float var20 = 4040.0F / var19;
         var16 = var13.deriveFont(f * var20);
         var17 = var15.getFontMetrics(var16);
         var19 = Math.max(1, Math.min(4040, var17.stringWidth(var11) + Math.round(var18 *= var20) + var17.stringWidth(var12)));
      }

      var15.dispose();
      int var31 = var19 + 56;
      int var21 = var17.getAscent() + var17.getDescent() + 56;
      int var22 = 28 + var17.getAscent();
      int var23 = var17.stringWidth(var11);
      BufferedImage var24 = new BufferedImage(var31, var21, 2);
      Graphics2D var25 = var24.createGraphics();
      hints(var25);
      var25.setFont(var16);
      var25.setColor(Color.WHITE);
      var25.drawString(var11, 28, var22);
      if (!var12.isEmpty()) {
         var25.drawString(var12, 28 + var23 + Math.round(var18), var22);
      }

      float var26 = (float)var16.createGlyphVector(var25.getFontRenderContext(), "H").getVisualBounds().getHeight();
      var25.dispose();
      int[] var27 = var24.getRGB(0, 0, var31, var21, null, 0, var31);
      int[] var28 = glow(var27, var31, var21);
      float var29 = var10 ? (28 + var23 + var18 * 0.5F) / var31 : 0.0F;
      float var30 = var10 ? (var22 - var26 * 0.5F) / var21 : 0.0F;
      return new SkyLyrics.Raster(i, var31, var21, var27, var28, var8, var6, var8 != null ? skyEmojiStyle : null, var29, var30, var10 ? var18 / var21 : 0.0F);
   }

   private Font fontFor(String string) {
      if (this.font == null && !this.fontMissing) {
         try (InputStream var2 = SkyLyrics.class.getResourceAsStream("/assets/efn/fonts/manrope-bold.ttf")) {
            if (var2 != null) {
               this.font = Font.createFont(0, var2);
            } else {
               this.fontMissing = true;
            }
         } catch (Exception var7) {
            this.fontMissing = true;
            LOGGER.warn("[EFN] Sky lyrics font couldn't load; using the system font", (Throwable)var7);
         }
      }

      return this.font != null && this.font.canDisplayUpTo(string) == -1 ? this.font : new Font("SansSerif", 1, 1);
   }

   private static void hints(Graphics2D graphics2D) {
      graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
   }

   private static int[] glow(int[] is, int i, int j) {
      float[] var3 = new float[i * j];

      for (int var4 = 0; var4 < var3.length; var4++) {
         var3[var4] = (is[var4] >>> 24) / 255.0F;
      }

      float[] var7 = new float[var3.length];

      for (int var5 = 0; var5 < 2; var5++) {
         blurRows(var3, var7, i, j);
         blurColumns(var7, var3, i, j);
      }

      int[] var8 = new int[var3.length];

      for (int var6 = 0; var6 < var8.length; var6++) {
         var8[var6] = Math.min(255, Math.round(var3[var6] * 400.0F)) << 24 | 16777215;
      }

      return var8;
   }

   private static void blurRows(float[] fs, float[] gs, int i, int j) {
      float var4 = 0.04761905F;

      for (int var5 = 0; var5 < j; var5++) {
         int var7 = var5 * i;
         float var8 = 0.0F;

         for (int var6 = 0; var6 <= Math.min(10, i - 1); var6++) {
            var8 += fs[var7 + var6];
         }

         for (int var11 = 0; var11 < i; var11++) {
            gs[var7 + var11] = var8 * var4;
            int var9 = var11 + 10 + 1;
            int var10 = var11 - 10;
            if (var9 < i) {
               var8 += fs[var7 + var9];
            }

            if (var10 >= 0) {
               var8 -= fs[var7 + var10];
            }
         }
      }
   }

   private static void blurColumns(float[] fs, float[] gs, int i, int j) {
      float var4 = 0.04761905F;

      for (int var5 = 0; var5 < i; var5++) {
         float var7 = 0.0F;

         for (int var6 = 0; var6 <= Math.min(10, j - 1); var6++) {
            var7 += fs[var6 * i + var5];
         }

         for (int var10 = 0; var10 < j; var10++) {
            gs[var10 * i + var5] = var7 * var4;
            int var8 = var10 + 10 + 1;
            int var9 = var10 - 10;
            if (var8 < j) {
               var7 += fs[var8 * i + var5];
            }

            if (var9 >= 0) {
               var7 -= fs[var9 * i + var5];
            }
         }
      }
   }

   private static int withAlpha(int i, float f) {
      return Math.round(MathHelper.clamp(f, 0.0F, 1.0F) * 255.0F) << 24 | i & 16777215;
   }

   private static float wrap(float f) {
      return f - (float)Math.floor(f);
   }

   private static float wrapDelta(float f) {
      return f - Math.round(f);
   }

   @Environment(EnvType.CLIENT)
   private record Raster(
      int id,
      int width,
      int height,
      int[] fill,
      int[] glow,
      SkyEmoji emoji,
      int[] emojiPixels,
      SpotifyHudModule.SkyEmojiStyle style,
      float emojiX,
      float emojiY,
      float emojiSize
   ) {
   }

   @Environment(EnvType.CLIENT)
   private static final class Shown {
      final SkyTexture fill;
      final SkyTexture glow;
      final float aspect;
      final float u;
      final float phase = (float)(Math.random() * 3.141592653589793 * 2.0);
      float alpha;
      SkyTexture emoji;
      SpotifyHudModule.SkyEmojiStyle style;
      boolean right;
      float emojiX;
      float emojiY;
      float emojiSize;

      Shown(SkyTexture skyTexture, SkyTexture skyTexture2, float f, float g) {
         this.fill = skyTexture;
         this.glow = skyTexture2;
         this.aspect = f;
         this.u = g;
      }

      void close() {
         this.fill.close();
         this.glow.close();
      }
   }

   @Environment(EnvType.CLIENT)
   private final class Slot {
      private final float degrees;
      private final float pixels;
      private String wanted = "";
      private SpotifyHudModule.SkyEmojiStyle wantedStyle;
      private volatile int request;
      private volatile SkyLyrics.Raster ready;
      private SkyLyrics.Shown current;
      private SkyLyrics.Shown leaving;

      Slot(float f, float g) {
         this.degrees = f;
         this.pixels = g;
      }

      void want(String string, SpotifyHudModule.SkyEmojiStyle skyEmojiStyle, float f) {
         if (!string.equals(this.wanted) || skyEmojiStyle != this.wantedStyle) {
            this.wanted = string;
            this.wantedStyle = skyEmojiStyle;
            int var4 = ++this.request;
            this.ready = null;
            if (!string.isEmpty()) {
               SpotifyHudModule.SkyEmojiStyle var5 = System.nanoTime() - SkyLyrics.this.lastEmojiAt >= 5000000000L ? skyEmojiStyle : null;
               SkyLyrics.this.raster.submit(() -> {
                  try {
                     SkyLyrics.Raster var5x = SkyLyrics.this.rasterize(var4, string, this.pixels, var5, f);
                     if (var4 == this.request) {
                        this.ready = var5x;
                     }
                  } catch (RuntimeException | Error var6) {
                     SkyLyrics.LOGGER.warn("[EFN] Sky lyrics couldn't draw a line", (Throwable)var6);
                  }
               });
            }
         }
      }

      void step(float f, float g) {
         SkyLyrics.Raster var3 = this.ready;
         if (var3 != null && var3.id() == this.request) {
            this.ready = null;
            this.retire();
            this.current = SkyLyrics.this.show(var3, g);
         } else if (this.wanted.isEmpty()) {
            this.retire();
         }

         if (this.current != null) {
            this.current.alpha = Math.min(1.0F, this.current.alpha + f * 2.6F);
         }

         if (this.leaving != null) {
            this.leaving.alpha -= f * 2.6F;
            if (this.leaving.alpha <= 0.0F) {
               this.leaving.close();
               this.leaving = null;
            }
         }
      }

      boolean idle() {
         return this.current == null && this.leaving == null && this.wanted.isEmpty();
      }

      private void retire() {
         if (this.current != null) {
            if (this.leaving != null) {
               this.leaving.close();
            }

            this.leaving = this.current;
            this.current = null;
         }
      }

      void close() {
         this.wanted = "";
         this.request++;
         this.ready = null;
         if (this.current != null) {
            this.current.close();
         }

         if (this.leaving != null) {
            this.leaving.close();
         }

         this.current = null;
         this.leaving = null;
      }
   }
}
