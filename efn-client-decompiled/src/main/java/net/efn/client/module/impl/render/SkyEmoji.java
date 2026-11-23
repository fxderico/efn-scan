package net.efn.client.module.impl.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Ellipse2D.Float;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
enum SkyEmoji {
   BROKEN_HEART(Set.of("heartbreak", "heartbroken", "brokenhearted"), Set.of("k\u0131r\u0131k", "kirik", "y\u0131k\u0131l", "parampar\u00e7a")),
   HEART(Set.of("heart", "hearts"), Set.of("kalp", "kalb")),
   KISS(Set.of("kiss", "kisses", "kissing", "kissed"), Set.of("\u00f6p")),
   HUG(Set.of("hug", "hugs", "hold", "holding", "embrace", "cuddle"), Set.of("sar\u0131l", "kucak")),
   LOVE(Set.of("love", "loved", "lover", "lovers", "loving", "baby", "darling", "honey"), Set.of("a\u015fk", "sevgi", "seviyor", "sevdi")),
   SHY(Set.of("shy", "blush", "blushing", "crush", "cute"), Set.of("utan", "tatl\u0131")),
   CRY(Set.of("cry", "cried", "crying", "cries", "tears", "tear", "weep"), Set.of("a\u011fla", "g\u00f6zya\u015f")),
   SAD(
      Set.of("sad", "lonely", "alone", "pain", "hurt", "hurts", "sorrow", "goodbye"),
      Set.of("\u00fczg\u00fcn", "yaln\u0131z", "ac\u0131", "h\u00fcz\u00fcn", "keder", "elveda")
   ),
   LAUGH(Set.of("laugh", "laughing", "haha", "funny", "lol", "smile", "smiling"), Set.of("g\u00fcl\u00fcyor", "g\u00fclerim", "kahkaha", "g\u00fcl\u00fcmse")),
   DEVIL(
      Set.of("sin", "sins", "sinner", "sinners", "devil", "hell", "evil", "demon", "danger", "dangerous", "wicked"),
      Set.of("\u015feytan", "g\u00fcnah", "cehennem", "tehlike")
   ),
   FIRE(Set.of("fire", "burn", "burning", "flame", "flames", "lit", "blaze"), Set.of("ate\u015f", "alev", "yan\u0131yor", "yak\u0131yor")),
   ANGRY(Set.of("angry", "mad", "hate", "rage", "fury"), Set.of("k\u0131zg\u0131n", "nefret", "\u00f6fke", "sinir")),
   SHOCK(Set.of("wow", "omg", "shock", "shocked", "crazy", "insane"), Set.of("\u015fok", "inan\u0131lmaz", "\u00e7\u0131lg\u0131n")),
   COOL(Set.of("cool", "swag", "drip", "boss", "flex", "fresh"), Set.of("haval\u0131", "efsane")),
   MONEY(Set.of("money", "cash", "rich", "dollar", "dollars", "millionaire", "paid"), Set.of("zengin", "dolar", "paray\u0131", "paralar")),
   STAR(Set.of("star", "stars", "shine", "shining", "sparkle"), Set.of("y\u0131ld\u0131z", "parla")),
   MOON(Set.of("moon", "night", "midnight", "tonight", "dream", "dreams", "dreaming"), Set.of("gece", "r\u00fcya", "mehtap")),
   SLEEP(Set.of("sleep", "sleeping", "tired", "asleep"), Set.of("uyku", "uyuyor", "uyudu", "yorgun")),
   THINK(Set.of("think", "thinking", "thought", "wonder", "why", "maybe", "guess"), Set.of("d\u00fc\u015f\u00fcn", "neden", "belki", "acaba")),
   MUSIC(Set.of("dance", "dancing", "song", "songs", "music", "sing", "singing", "melody", "beat"), Set.of("dans", "\u015fark\u0131", "m\u00fczik", "melodi"));

   static final int SIZE = 256;
   private static final String IMAGES = "/assets/efn/textures/sky_emoji/";
   private static final int CANVAS = 192;
   private static final Pattern WORD = Pattern.compile("\\p{L}+");
   private static final String[][] PHRASES = new String[][]{
      {"broken heart", "broken heart"}, {"break my heart", "heart"}, {"kalbim k\u0131r\u0131k", "k\u0131r\u0131k"}
   };
   private static final Color INK = new Color(662080);
   private static final Color TEAR = new Color(10479359);
   private static final Color RED = new Color(16726876);
   private final Set<String> words;
   private final Set<String> stems;

   private SkyEmoji(Set<String> nullxx, Set<String> nullxxx) {
      this.words = nullxx;
      this.stems = nullxxx;
   }

   static SkyEmoji.Match match(String var0) {
      for (String[] var4 : PHRASES) {
         Matcher var5 = Pattern.compile(Pattern.quote(var4[0]), 66).matcher(var0);
         if (var5.find()) {
            int var6 = var5.start() + var4[0].indexOf(var4[1]);
            return new SkyEmoji.Match(BROKEN_HEART, var6, var6 + var4[1].length());
         }
      }

      ArrayList var13 = new ArrayList();
      ArrayList var14 = new ArrayList();
      Matcher var15 = WORD.matcher(var0);

      while (var15.find()) {
         var13.add(new int[]{var15.start(), var15.end()});
         var14.add(var15.group().toLowerCase(Locale.ROOT).replace("i\u0307", "i"));
      }

      for (SkyEmoji var7 : values()) {
         for (int var8 = 0; var8 < var14.size(); var8++) {
            String var9 = (String)var14.get(var8);
            boolean var10 = var7.words.contains(var9);

            for (String var12 : var7.stems) {
               var10 |= var9.startsWith(var12);
            }

            if (var10) {
               return new SkyEmoji.Match(var7, ((int[])var13.get(var8))[0], ((int[])var13.get(var8))[1]);
            }
         }
      }

      return null;
   }

   int[] draw() {
      BufferedImage var1 = this.picture();
      if (var1 != null || this != HUG && this != SHY && this != THINK) {
         BufferedImage var2 = new BufferedImage(256, 256, 2);
         Graphics2D var3 = var2.createGraphics();
         var3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         var3.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
         var3.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
         var3.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
         var3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         if (var1 != null) {
            var3.drawImage(var1, 0, 0, 256, 256, null);
            var3.dispose();
            return var2.getRGB(0, 0, 256, 256, null, 0, 256);
         } else {
            var3.scale(1.3333333333333333, 1.3333333333333333);
            switch (this) {
               case BROKEN_HEART:
                  heart(var3, 96.0F, 100.0F, 56.0F, true);
                  break;
               case HEART:
                  heart(var3, 96.0F, 100.0F, 64.0F, false);
                  break;
               case KISS:
               case HUG:
               case LOVE:
               case SHY:
               case CRY:
               case SAD:
               case LAUGH:
               case DEVIL:
               case ANGRY:
               case SHOCK:
               case COOL:
               case MONEY:
               case SLEEP:
               case THINK:
               default:
                  this.face(var3);
                  break;
               case FIRE:
                  flame(var3);
                  break;
               case STAR:
                  star(var3);
                  break;
               case MOON:
                  moon(var3);
                  break;
               case MUSIC:
                  notes(var3);
            }

            var3.dispose();
            return var2.getRGB(0, 0, 256, 256, null, 0, 256);
         }
      } else {
         return null;
      }
   }

   private BufferedImage picture() {
      try {
         BufferedImage var2;
         try (InputStream var1 = SkyEmoji.class.getResourceAsStream("/assets/efn/textures/sky_emoji/" + this.name().toLowerCase(Locale.ROOT) + ".png")) {
            var2 = var1 == null ? null : ImageIO.read(var1);
         }

         return var2;
      } catch (IOException var6) {
         return null;
      }
   }

   private void face(Graphics2D var1) {
      if (this == DEVIL) {
         horns(var1);
      }

      sphere(var1, 96.0F, 100.0F, 72.0F);
      switch (this) {
         case KISS:
            closedEye(var1, 72.0F, 92.0F, 13.0F, true);
            eye(var1, 120.0F, 90.0F, 11.0F, 14.0F, 0.0F, 0.0F);
            kissLips(var1, 102.0F, 128.0F);
            heart(var1, 150.0F, 62.0F, 12.0F, false);
            break;
         case HUG:
         case SHY:
         case FIRE:
         case STAR:
         case MOON:
         default:
            eye(var1, 72.0F, 92.0F, 11.0F, 14.0F, 0.0F, 0.0F);
            eye(var1, 120.0F, 92.0F, 11.0F, 14.0F, 0.0F, 0.0F);
            break;
         case LOVE:
            heart(var1, 70.0F, 90.0F, 20.0F, false);
            heart(var1, 122.0F, 90.0F, 20.0F, false);
            openSmile(var1, 96.0F, 122.0F, 30.0F, 18.0F, true);
            break;
         case CRY:
            tears(var1, 72.0F, 98.0F);
            tears(var1, 120.0F, 98.0F);
            closedEye(var1, 72.0F, 92.0F, 13.0F, false);
            closedEye(var1, 120.0F, 92.0F, 13.0F, false);
            brows(var1, 72.0F, 72.0F, 120.0F, 72.0F, -0.35F);
            openFrown(var1, 96.0F, 134.0F, 22.0F, 13.0F);
            break;
         case SAD:
            droopyEye(var1, 72.0F, 94.0F);
            droopyEye(var1, 120.0F, 94.0F);
            brows(var1, 72.0F, 74.0F, 120.0F, 74.0F, -0.3F);
            frown(var1, 96.0F, 134.0F, 18.0F);
            drop(var1, 62.0F, 118.0F, 7.0F);
            break;
         case LAUGH:
            happyEye(var1, 72.0F, 90.0F);
            happyEye(var1, 120.0F, 90.0F);
            laughMouth(var1, 96.0F, 120.0F);
            drop(var1, 44.0F, 98.0F, 8.0F);
            drop(var1, 148.0F, 98.0F, 8.0F);
            break;
         case DEVIL:
            eye(var1, 72.0F, 94.0F, 11.0F, 9.0F, 0.2F, 0.0F);
            eye(var1, 120.0F, 94.0F, 11.0F, 9.0F, -0.2F, 0.0F);
            brows(var1, 72.0F, 78.0F, 120.0F, 78.0F, 0.45F);
            smirk(var1, 96.0F, 126.0F, true);
            break;
         case ANGRY:
            eye(var1, 72.0F, 96.0F, 11.0F, 9.0F, 0.15F, 0.0F);
            eye(var1, 120.0F, 96.0F, 11.0F, 9.0F, -0.15F, 0.0F);
            brows(var1, 72.0F, 78.0F, 120.0F, 78.0F, 0.6F);
            teeth(var1, 96.0F, 130.0F);
            vein(var1, 146.0F, 56.0F);
            break;
         case SHOCK:
            eye(var1, 72.0F, 90.0F, 15.0F, 18.0F, 0.0F, 0.0F);
            eye(var1, 120.0F, 90.0F, 15.0F, 18.0F, 0.0F, 0.0F);
            brows(var1, 72.0F, 64.0F, 120.0F, 64.0F, -0.15F);
            ring(var1, 96.0F, 134.0F, 13.0F, 16.0F);
            break;
         case COOL:
            shades(var1);
            smirk(var1, 98.0F, 128.0F, false);
            break;
         case MONEY:
            dollarEyes(var1);
            openSmile(var1, 96.0F, 122.0F, 30.0F, 18.0F, false);
            banknoteTongue(var1, 96.0F, 138.0F);
            break;
         case SLEEP:
            closedEye(var1, 72.0F, 94.0F, 13.0F, true);
            closedEye(var1, 120.0F, 94.0F, 13.0F, true);
            ring(var1, 96.0F, 130.0F, 6.0F, 7.0F);
            zz(var1);
      }
   }

   private static void sphere(Graphics2D var0, float var1, float var2, float var3) {
      for (int var4 = 6; var4 >= 1; var4--) {
         float var5 = var4 * 2.2F;
         var0.setColor(new Color(0, 10, 40, 12));
         var0.fill(new Float(var1 - var3 - var5 * 0.5F, var2 - var3 + 6.0F - var5 * 0.3F, (var3 + var5 * 0.5F) * 2.0F, (var3 + var5 * 0.4F) * 2.0F));
      }

      Float var6 = new Float(var1 - var3, var2 - var3, var3 * 2.0F, var3 * 2.0F);
      var0.setPaint(
         new RadialGradientPaint(
            new java.awt.geom.Point2D.Float(var1 - var3 * 0.35F, var2 - var3 * 0.45F),
            var3 * 1.55F,
            new float[]{0.0F, 0.35F, 0.75F, 1.0F},
            new Color[]{new Color(11132159), new Color(4101621), new Color(1791689), new Color(798078)}
         )
      );
      var0.fill(var6);
      var0.setPaint(
         new RadialGradientPaint(
            new java.awt.geom.Point2D.Float(var1, var2 + var3 * 0.9F),
            var3 * 1.1F,
            new float[]{0.0F, 1.0F},
            new Color[]{new Color(7327999), new Color(7327999, true)}
         )
      );
      var0.fill(var6);
      var0.setStroke(new BasicStroke(2.5F));
      var0.setColor(new Color(666222));
      var0.draw(var6);
      AffineTransform var7 = var0.getTransform();
      var0.rotate(-0.5, var1 - var3 * 0.42F, var2 - var3 * 0.55F);
      var0.setPaint(new GradientPaint(0.0F, var2 - var3 * 0.8F, new Color(255, 255, 255, 190), 0.0F, var2 - var3 * 0.25F, new Color(255, 255, 255, 0)));
      var0.fill(new Float(var1 - var3 * 0.75F, var2 - var3 * 0.8F, var3 * 0.7F, var3 * 0.42F));
      var0.setTransform(var7);
   }

   private static void eye(Graphics2D var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      AffineTransform var7 = var0.getTransform();
      var0.rotate(var5, var1, var2);
      var0.setColor(Color.WHITE);
      var0.fill(new Float(var1 - var3, var2 - var4, var3 * 2.0F, var4 * 2.0F));
      var0.setColor(INK);
      float var8 = Math.min(var3, var4) * 0.62F;
      var0.fill(new Float(var1 - var8 + var6, var2 - var8 + var4 * 0.15F, var8 * 2.0F, var8 * 2.0F));
      var0.setColor(Color.WHITE);
      var0.fill(new Float(var1 - var8 * 0.25F + var6, var2 - var8 * 0.55F + var4 * 0.15F, var8 * 0.6F, var8 * 0.6F));
      var0.setTransform(var7);
   }

   private static void closedEye(Graphics2D var0, float var1, float var2, float var3, boolean var4) {
      var0.setColor(INK);
      var0.setStroke(new BasicStroke(5.0F, 1, 1));
      float var5 = var4 ? 8.0F : -8.0F;
      var0.draw(new java.awt.geom.QuadCurve2D.Float(var1 - var3, var2, var1, var2 + var5, var1 + var3, var2));
   }

   private static void happyEye(Graphics2D var0, float var1, float var2) {
      var0.setColor(INK);
      var0.setStroke(new BasicStroke(6.0F, 1, 1));
      var0.draw(new java.awt.geom.QuadCurve2D.Float(var1 - 13.0F, var2 + 6.0F, var1, var2 - 14.0F, var1 + 13.0F, var2 + 6.0F));
   }

   private static void droopyEye(Graphics2D var0, float var1, float var2) {
      Area var3 = new Area(new Float(var1 - 11.0F, var2 - 13.0F, 22.0F, 26.0F));
      var3.subtract(new Area(new Float(var1 - 16.0F, var2 - 26.0F, 32.0F, 22.0F)));
      var0.setColor(Color.WHITE);
      var0.fill(var3);
      var0.setColor(INK);
      var0.fill(new Float(var1 - 6.0F, var2 - 2.0F, 12.0F, 12.0F));
      var0.setStroke(new BasicStroke(3.5F, 1, 1));
      var0.draw(new java.awt.geom.QuadCurve2D.Float(var1 - 12.0F, var2 - 5.0F, var1, var2 - 1.0F, var1 + 12.0F, var2 - 5.0F));
   }

   private static void brows(Graphics2D var0, float var1, float var2, float var3, float var4, float var5) {
      var0.setColor(INK);
      var0.setStroke(new BasicStroke(6.0F, 1, 1));
      float var6 = 12.0F;
      float var7 = 12.0F * var5;
      var0.draw(new java.awt.geom.QuadCurve2D.Float(var1 - var6, var2 - var7, var1, var2 - 3.0F, var1 + var6, var2 + var7));
      var0.draw(new java.awt.geom.QuadCurve2D.Float(var3 - var6, var4 + var7, var3, var4 - 3.0F, var3 + var6, var4 - var7));
   }

   private static void openSmile(Graphics2D var0, float var1, float var2, float var3, float var4, boolean var5) {
      GeneralPath var6 = new GeneralPath();
      var6.moveTo(var1 - var3, var2);
      var6.quadTo(var1, var2 + var4 * 2.2F, var1 + var3, var2);
      var6.quadTo(var1, var2 + var4 * 0.4F, var1 - var3, var2);
      var6.closePath();
      var0.setColor(new Color(2755102));
      var0.fill(var6);
      if (var5) {
         Shape var7 = var0.getClip();
         var0.clip(var6);
         var0.setColor(new Color(16740241));
         var0.fill(new Float(var1 - var3 * 0.5F, var2 + var4 * 0.7F, var3, var4 * 1.4F));
         var0.setClip(var7);
      }

      var0.setColor(INK);
      var0.setStroke(new BasicStroke(3.0F, 1, 1));
      var0.draw(var6);
   }

   private static void openFrown(Graphics2D var0, float var1, float var2, float var3, float var4) {
      GeneralPath var5 = new GeneralPath();
      var5.moveTo(var1 - var3, var2 + var4 * 0.6F);
      var5.quadTo(var1, var2 - var4 * 1.4F, var1 + var3, var2 + var4 * 0.6F);
      var5.quadTo(var1, var2 + var4 * 0.2F, var1 - var3, var2 + var4 * 0.6F);
      var5.closePath();
      var0.setColor(new Color(2755102));
      var0.fill(var5);
      var0.setColor(INK);
      var0.setStroke(new BasicStroke(3.0F, 1, 1));
      var0.draw(var5);
   }

   private static void frown(Graphics2D var0, float var1, float var2, float var3) {
      var0.setColor(INK);
      var0.setStroke(new BasicStroke(6.0F, 1, 1));
      var0.draw(new java.awt.geom.QuadCurve2D.Float(var1 - var3, var2 + 4.0F, var1, var2 - 10.0F, var1 + var3, var2 + 4.0F));
   }

   private static void smirk(Graphics2D var0, float var1, float var2, boolean var3) {
      var0.setColor(INK);
      var0.setStroke(new BasicStroke(6.0F, 1, 1));
      float var4 = var3 ? 30.0F : 22.0F;
      GeneralPath var5 = new GeneralPath();
      var5.moveTo(var1 - var4, var2 - 2.0F);
      var5.curveTo(var1 - var4 * 0.3F, var2 + 10.0F, var1 + var4 * 0.5F, var2 + 8.0F, var1 + var4, var2 - 10.0F);
      var0.draw(var5);
   }

   private static void laughMouth(Graphics2D var0, float var1, float var2) {
      GeneralPath var3 = new GeneralPath();
      var3.moveTo(var1 - 34.0F, var2 - 4.0F);
      var3.lineTo(var1 + 34.0F, var2 - 4.0F);
      var3.quadTo(var1 + 30.0F, var2 + 38.0F, var1, var2 + 38.0F);
      var3.quadTo(var1 - 30.0F, var2 + 38.0F, var1 - 34.0F, var2 - 4.0F);
      var3.closePath();
      var0.setColor(new Color(2755102));
      var0.fill(var3);
      Shape var4 = var0.getClip();
      var0.clip(var3);
      var0.setColor(Color.WHITE);
      var0.fill(new java.awt.geom.RoundRectangle2D.Float(var1 - 34.0F, var2 - 6.0F, 68.0F, 13.0F, 6.0F, 6.0F));
      var0.setColor(new Color(16740241));
      var0.fill(new Float(var1 - 18.0F, var2 + 18.0F, 36.0F, 26.0F));
      var0.setClip(var4);
      var0.setColor(INK);
      var0.setStroke(new BasicStroke(3.0F, 1, 1));
      var0.draw(var3);
   }

   private static void teeth(Graphics2D var0, float var1, float var2) {
      java.awt.geom.RoundRectangle2D.Float var3 = new java.awt.geom.RoundRectangle2D.Float(var1 - 26.0F, var2 - 10.0F, 52.0F, 20.0F, 12.0F, 12.0F);
      var0.setColor(Color.WHITE);
      var0.fill(var3);
      var0.setColor(INK);
      var0.setStroke(new BasicStroke(2.5F));
      var0.draw(var3);
      var0.drawLine((int)(var1 - 26.0F), (int)var2, (int)(var1 + 26.0F), (int)var2);

      for (int var4 = -2; var4 <= 2; var4++) {
         var0.drawLine((int)(var1 + var4 * 9.0F), (int)(var2 - 10.0F), (int)(var1 + var4 * 9.0F), (int)(var2 + 10.0F));
      }
   }

   private static void ring(Graphics2D var0, float var1, float var2, float var3, float var4) {
      var0.setColor(new Color(2755102));
      var0.fill(new Float(var1 - var3, var2 - var4, var3 * 2.0F, var4 * 2.0F));
      var0.setColor(INK);
      var0.setStroke(new BasicStroke(3.0F));
      var0.draw(new Float(var1 - var3, var2 - var4, var3 * 2.0F, var4 * 2.0F));
   }

   private static void kissLips(Graphics2D var0, float var1, float var2) {
      var0.setColor(INK);
      var0.setStroke(new BasicStroke(5.5F, 1, 1));
      GeneralPath var3 = new GeneralPath();
      var3.moveTo(var1 - 6.0F, var2 - 12.0F);
      var3.quadTo(var1 + 10.0F, var2 - 10.0F, var1 + 2.0F, var2 - 2.0F);
      var3.quadTo(var1 + 12.0F, var2 + 4.0F, var1 - 6.0F, var2 + 10.0F);
      var0.draw(var3);
   }

   private static void tears(Graphics2D var0, float var1, float var2) {
      var0.setPaint(new GradientPaint(0.0F, var2, new Color(13235199), 0.0F, var2 + 70.0F, new Color(5227519)));
      var0.fill(new java.awt.geom.RoundRectangle2D.Float(var1 - 7.0F, var2, 14.0F, 66.0F, 14.0F, 14.0F));
   }

   private static void drop(Graphics2D var0, float var1, float var2, float var3) {
      GeneralPath var4 = new GeneralPath();
      var4.moveTo(var1, var2 - var3 * 2.2F);
      var4.curveTo(var1 + var3 * 1.2F, var2 - var3 * 0.6F, var1 + var3, var2 + var3, var1, var2 + var3);
      var4.curveTo(var1 - var3, var2 + var3, var1 - var3 * 1.2F, var2 - var3 * 0.6F, var1, var2 - var3 * 2.2F);
      var0.setColor(TEAR);
      var0.fill(var4);
      var0.setColor(new Color(255, 255, 255, 200));
      var0.fill(new Float(var1 - var3 * 0.45F, var2 - var3 * 0.4F, var3 * 0.5F, var3 * 0.6F));
   }

   private static void shades(Graphics2D var0) {
      GeneralPath var1 = new GeneralPath();
      var1.moveTo(42.0F, 80.0F);
      var1.lineTo(90.0F, 80.0F);
      var1.quadTo(90.0F, 108.0F, 68.0F, 108.0F);
      var1.quadTo(44.0F, 108.0F, 42.0F, 80.0F);
      var1.closePath();
      GeneralPath var2 = (GeneralPath)var1.clone();
      var2.transform(AffineTransform.getTranslateInstance(60.0, 0.0));
      var0.setColor(new Color(724764));
      var0.fill(var1);
      var0.fill(var2);
      var0.setStroke(new BasicStroke(6.0F, 1, 1));
      var0.draw(new java.awt.geom.Line2D.Float(88.0F, 84.0F, 104.0F, 84.0F));
      var0.setColor(new Color(255, 255, 255, 120));
      var0.setStroke(new BasicStroke(4.0F, 1, 1));
      var0.drawLine(52, 86, 62, 96);
      var0.drawLine(112, 86, 122, 96);
   }

   private static void dollarEyes(Graphics2D var0) {
      var0.setFont(new Font("SansSerif", 1, 40));
      var0.setColor(new Color(2347360));
      var0.drawString("$", 60, 106);
      var0.drawString("$", 108, 106);
   }

   private static void banknoteTongue(Graphics2D var0, float var1, float var2) {
      var0.setColor(new Color(4054148));
      var0.fill(new java.awt.geom.RoundRectangle2D.Float(var1 - 14.0F, var2 - 4.0F, 28.0F, 26.0F, 6.0F, 6.0F));
      var0.setColor(new Color(2002510));
      var0.setStroke(new BasicStroke(2.0F));
      var0.draw(new java.awt.geom.RoundRectangle2D.Float(var1 - 14.0F, var2 - 4.0F, 28.0F, 26.0F, 6.0F, 6.0F));
   }

   private static void zz(Graphics2D var0) {
      var0.setColor(Color.WHITE);
      var0.setFont(new Font("SansSerif", 1, 30));
      var0.drawString("Z", 140, 52);
      var0.setFont(new Font("SansSerif", 1, 20));
      var0.drawString("z", 162, 30);
   }

   private static void vein(Graphics2D var0, float var1, float var2) {
      var0.setColor(RED);
      var0.setStroke(new BasicStroke(5.0F, 1, 1));

      for (int var3 = 0; var3 < 4; var3++) {
         AffineTransform var4 = var0.getTransform();
         var0.rotate(var3 * 3.141592653589793 / 2.0, var1, var2);
         var0.draw(new java.awt.geom.QuadCurve2D.Float(var1 - 4.0F, var2 - 14.0F, var1 - 4.0F, var2 - 4.0F, var1 - 14.0F, var2 - 4.0F));
         var0.setTransform(var4);
      }
   }

   private static void horns(Graphics2D var0) {
      for (byte var1 = -1; var1 <= 1; var1 += 2) {
         GeneralPath var2 = new GeneralPath();
         float var3 = 96.0F + var1 * 40.0F;
         var2.moveTo(var3 - var1 * 16.0F, 48.0F);
         var2.quadTo(var3 + var1 * 12.0F, 34.0F, var3 + var1 * 18.0F, 10.0F);
         var2.quadTo(var3 + var1 * 26.0F, 40.0F, var3 + var1 * 14.0F, 62.0F);
         var2.closePath();
         var0.setPaint(new GradientPaint(0.0F, 10.0F, new Color(14183423), 0.0F, 62.0F, new Color(6954936)));
         var0.fill(var2);
         var0.setColor(new Color(4001138));
         var0.setStroke(new BasicStroke(2.5F));
         var0.draw(var2);
      }
   }

   private static Shape heartShape(float var0, float var1, float var2) {
      GeneralPath var3 = new GeneralPath();
      var3.moveTo(var0, var1 + var2 * 0.95F);
      var3.curveTo(var0 - var2 * 1.35F, var1 + var2 * 0.05F, var0 - var2 * 1.1F, var1 - var2 * 1.05F, var0, var1 - var2 * 0.45F);
      var3.curveTo(var0 + var2 * 1.1F, var1 - var2 * 1.05F, var0 + var2 * 1.35F, var1 + var2 * 0.05F, var0, var1 + var2 * 0.95F);
      var3.closePath();
      return var3;
   }

   private static void heart(Graphics2D var0, float var1, float var2, float var3, boolean var4) {
      Shape var5 = heartShape(var1, var2, var3);
      SkyEmoji.HeartPaint var6 = new SkyEmoji.HeartPaint(var1, var2, var3);
      if (!var4) {
         var6.fill(var0, var5);
      } else {
         GeneralPath var7 = new GeneralPath();
         var7.moveTo(var1, var2 - var3);
         var7.lineTo(var1 - var3 * 0.18F, var2 - var3 * 0.2F);
         var7.lineTo(var1 + var3 * 0.15F, var2 + var3 * 0.15F);
         var7.lineTo(var1 - var3 * 0.1F, var2 + var3 * 0.6F);
         var7.lineTo(var1, var2 + var3 * 1.2F);
         var7.lineTo(var1 - var3 * 2.0F, var2 + var3 * 1.2F);
         var7.lineTo(var1 - var3 * 2.0F, var2 - var3 * 1.2F);
         var7.closePath();
         Area var8 = new Area(var5);
         var8.intersect(new Area(var7));
         Area var9 = new Area(var5);
         var9.subtract(new Area(var7));
         var8.transform(AffineTransform.getRotateInstance(-0.1, var1, var2 + var3));
         var8.transform(AffineTransform.getTranslateInstance(-5.0, 0.0));
         var9.transform(AffineTransform.getRotateInstance(0.1, var1, var2 + var3));
         var9.transform(AffineTransform.getTranslateInstance(5.0, 0.0));
         var6.fill(var0, var8);
         var6.fill(var0, var9);
      }
   }

   private static void flame(Graphics2D var0) {
      GeneralPath var1 = new GeneralPath();
      var1.moveTo(96.0F, 176.0F);
      var1.curveTo(40.0F, 176.0F, 30.0F, 122.0F, 58.0F, 86.0F);
      var1.curveTo(66.0F, 110.0F, 76.0F, 112.0F, 80.0F, 108.0F);
      var1.curveTo(70.0F, 74.0F, 92.0F, 42.0F, 110.0F, 16.0F);
      var1.curveTo(112.0F, 58.0F, 150.0F, 76.0F, 152.0F, 120.0F);
      var1.curveTo(154.0F, 156.0F, 130.0F, 176.0F, 96.0F, 176.0F);
      var1.closePath();
      var0.setPaint(new GradientPaint(0.0F, 16.0F, new Color(8382719), 0.0F, 176.0F, new Color(1791689)));
      var0.fill(var1);
      var0.setColor(new Color(666222));
      var0.setStroke(new BasicStroke(2.5F));
      var0.draw(var1);
      GeneralPath var2 = new GeneralPath();
      var2.moveTo(98.0F, 168.0F);
      var2.curveTo(70.0F, 168.0F, 66.0F, 136.0F, 82.0F, 116.0F);
      var2.curveTo(88.0F, 132.0F, 96.0F, 130.0F, 98.0F, 122.0F);
      var2.curveTo(96.0F, 102.0F, 108.0F, 86.0F, 116.0F, 76.0F);
      var2.curveTo(118.0F, 104.0F, 132.0F, 118.0F, 128.0F, 142.0F);
      var2.curveTo(126.0F, 160.0F, 114.0F, 168.0F, 98.0F, 168.0F);
      var2.closePath();
      var0.setPaint(new GradientPaint(0.0F, 76.0F, Color.WHITE, 0.0F, 168.0F, new Color(11132159)));
      var0.fill(var2);
   }

   private static void star(Graphics2D var0) {
      GeneralPath var1 = new GeneralPath();

      for (int var2 = 0; var2 < 10; var2++) {
         double var3 = -1.5707963267948966 + var2 * 3.141592653589793 / 5.0;
         double var5 = var2 % 2 == 0 ? 80.0 : 34.0;
         float var7 = (float)(96.0 + Math.cos(var3) * var5);
         float var8 = (float)(100.0 + Math.sin(var3) * var5);
         if (var2 == 0) {
            var1.moveTo(var7, var8);
         } else {
            var1.lineTo(var7, var8);
         }
      }

      var1.closePath();
      var0.setPaint(
         new RadialGradientPaint(
            new java.awt.geom.Point2D.Float(80.0F, 80.0F),
            100.0F,
            new float[]{0.0F, 0.5F, 1.0F},
            new Color[]{new Color(16774832), new Color(16762941), new Color(14255642)},
            CycleMethod.NO_CYCLE
         )
      );
      var0.fill(var1);
      var0.setColor(new Color(9064202));
      var0.setStroke(new BasicStroke(2.5F, 1, 1));
      var0.draw(var1);
      var0.setColor(new Color(255, 255, 255, 170));
      var0.fill(new Float(72.0F, 62.0F, 22.0F, 12.0F));
   }

   private static void moon(Graphics2D var0) {
      Area var1 = new Area(new Float(30.0F, 26.0F, 140.0F, 140.0F));
      var1.subtract(new Area(new Float(70.0F, 8.0F, 130.0F, 130.0F)));
      var0.setPaint(
         new RadialGradientPaint(
            new java.awt.geom.Point2D.Float(60.0F, 70.0F), 120.0F, new float[]{0.0F, 1.0F}, new Color[]{new Color(16775112), new Color(15909198)}
         )
      );
      var0.fill(var1);
      var0.setColor(new Color(10119954));
      var0.setStroke(new BasicStroke(2.5F));
      var0.draw(var1);
      var0.setColor(new Color(16775112));
      sparkle(var0, 150.0F, 140.0F, 12.0F);
      sparkle(var0, 168.0F, 104.0F, 7.0F);
   }

   private static void sparkle(Graphics2D var0, float var1, float var2, float var3) {
      GeneralPath var4 = new GeneralPath();
      var4.moveTo(var1, var2 - var3);
      var4.quadTo(var1, var2, var1 + var3, var2);
      var4.quadTo(var1, var2, var1, var2 + var3);
      var4.quadTo(var1, var2, var1 - var3, var2);
      var4.quadTo(var1, var2, var1, var2 - var3);
      var0.fill(var4);
   }

   private static void notes(Graphics2D var0) {
      var0.setPaint(new GradientPaint(0.0F, 30.0F, new Color(9425919), 0.0F, 170.0F, new Color(1791689)));
      var0.fill(new Float(36.0F, 132.0F, 44.0F, 32.0F));
      var0.fill(new Float(112.0F, 116.0F, 44.0F, 32.0F));
      var0.fill(new java.awt.geom.Rectangle2D.Float(70.0F, 42.0F, 10.0F, 104.0F));
      var0.fill(new java.awt.geom.Rectangle2D.Float(146.0F, 26.0F, 10.0F, 104.0F));
      GeneralPath var1 = new GeneralPath();
      var1.moveTo(70.0F, 42.0F);
      var1.lineTo(156.0F, 26.0F);
      var1.lineTo(156.0F, 50.0F);
      var1.lineTo(70.0F, 66.0F);
      var1.closePath();
      var0.fill(var1);
      var0.setColor(new Color(666222));
      var0.setStroke(new BasicStroke(2.5F));
      var0.draw(new Float(36.0F, 132.0F, 44.0F, 32.0F));
      var0.draw(new Float(112.0F, 116.0F, 44.0F, 32.0F));
      var0.setColor(new Color(255, 255, 255, 160));
      var0.fill(new Float(46.0F, 138.0F, 14.0F, 8.0F));
      var0.fill(new Float(122.0F, 122.0F, 14.0F, 8.0F));
   }

   @Environment(EnvType.CLIENT)
   private record HeartPaint(float cx, float cy, float r) {
      void fill(Graphics2D var1, Shape var2) {
         var1.setPaint(
            new RadialGradientPaint(
               new java.awt.geom.Point2D.Float(this.cx - this.r * 0.4F, this.cy - this.r * 0.5F),
               this.r * 1.7F,
               new float[]{0.0F, 0.45F, 1.0F},
               new Color[]{new Color(16752050), new Color(16726876), new Color(11014190)}
            )
         );
         var1.fill(var2);
         var1.setColor(new Color(7211552));
         var1.setStroke(new BasicStroke(Math.max(1.5F, this.r * 0.05F)));
         var1.draw(var2);
         Shape var3 = var1.getClip();
         var1.clip(var2);
         var1.setColor(new Color(255, 255, 255, 150));
         var1.fill(new Float(this.cx - this.r * 0.85F, this.cy - this.r * 0.75F, this.r * 0.55F, this.r * 0.35F));
         var1.setClip(var3);
      }
   }

   @Environment(EnvType.CLIENT)
   record Match(SkyEmoji emoji, int start, int end) {
   }
}
