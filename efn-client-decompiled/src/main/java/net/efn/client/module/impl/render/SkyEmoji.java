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

   private SkyEmoji(Set<String> set, Set<String> set2) {
      this.words = set;
      this.stems = set2;
   }

   static SkyEmoji.Match match(String string) {
      for (String[] var4 : PHRASES) {
         Matcher var5 = Pattern.compile(Pattern.quote(var4[0]), 66).matcher(string);
         if (var5.find()) {
            int var6 = var5.start() + var4[0].indexOf(var4[1]);
            return new SkyEmoji.Match(BROKEN_HEART, var6, var6 + var4[1].length());
         }
      }

      ArrayList var13 = new ArrayList();
      ArrayList var14 = new ArrayList();
      Matcher var15 = WORD.matcher(string);

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

   private void face(Graphics2D graphics2D) {
      if (this == DEVIL) {
         horns(graphics2D);
      }

      sphere(graphics2D, 96.0F, 100.0F, 72.0F);
      switch (this) {
         case KISS:
            closedEye(graphics2D, 72.0F, 92.0F, 13.0F, true);
            eye(graphics2D, 120.0F, 90.0F, 11.0F, 14.0F, 0.0F, 0.0F);
            kissLips(graphics2D, 102.0F, 128.0F);
            heart(graphics2D, 150.0F, 62.0F, 12.0F, false);
            break;
         case HUG:
         case SHY:
         case FIRE:
         case STAR:
         case MOON:
         default:
            eye(graphics2D, 72.0F, 92.0F, 11.0F, 14.0F, 0.0F, 0.0F);
            eye(graphics2D, 120.0F, 92.0F, 11.0F, 14.0F, 0.0F, 0.0F);
            break;
         case LOVE:
            heart(graphics2D, 70.0F, 90.0F, 20.0F, false);
            heart(graphics2D, 122.0F, 90.0F, 20.0F, false);
            openSmile(graphics2D, 96.0F, 122.0F, 30.0F, 18.0F, true);
            break;
         case CRY:
            tears(graphics2D, 72.0F, 98.0F);
            tears(graphics2D, 120.0F, 98.0F);
            closedEye(graphics2D, 72.0F, 92.0F, 13.0F, false);
            closedEye(graphics2D, 120.0F, 92.0F, 13.0F, false);
            brows(graphics2D, 72.0F, 72.0F, 120.0F, 72.0F, -0.35F);
            openFrown(graphics2D, 96.0F, 134.0F, 22.0F, 13.0F);
            break;
         case SAD:
            droopyEye(graphics2D, 72.0F, 94.0F);
            droopyEye(graphics2D, 120.0F, 94.0F);
            brows(graphics2D, 72.0F, 74.0F, 120.0F, 74.0F, -0.3F);
            frown(graphics2D, 96.0F, 134.0F, 18.0F);
            drop(graphics2D, 62.0F, 118.0F, 7.0F);
            break;
         case LAUGH:
            happyEye(graphics2D, 72.0F, 90.0F);
            happyEye(graphics2D, 120.0F, 90.0F);
            laughMouth(graphics2D, 96.0F, 120.0F);
            drop(graphics2D, 44.0F, 98.0F, 8.0F);
            drop(graphics2D, 148.0F, 98.0F, 8.0F);
            break;
         case DEVIL:
            eye(graphics2D, 72.0F, 94.0F, 11.0F, 9.0F, 0.2F, 0.0F);
            eye(graphics2D, 120.0F, 94.0F, 11.0F, 9.0F, -0.2F, 0.0F);
            brows(graphics2D, 72.0F, 78.0F, 120.0F, 78.0F, 0.45F);
            smirk(graphics2D, 96.0F, 126.0F, true);
            break;
         case ANGRY:
            eye(graphics2D, 72.0F, 96.0F, 11.0F, 9.0F, 0.15F, 0.0F);
            eye(graphics2D, 120.0F, 96.0F, 11.0F, 9.0F, -0.15F, 0.0F);
            brows(graphics2D, 72.0F, 78.0F, 120.0F, 78.0F, 0.6F);
            teeth(graphics2D, 96.0F, 130.0F);
            vein(graphics2D, 146.0F, 56.0F);
            break;
         case SHOCK:
            eye(graphics2D, 72.0F, 90.0F, 15.0F, 18.0F, 0.0F, 0.0F);
            eye(graphics2D, 120.0F, 90.0F, 15.0F, 18.0F, 0.0F, 0.0F);
            brows(graphics2D, 72.0F, 64.0F, 120.0F, 64.0F, -0.15F);
            ring(graphics2D, 96.0F, 134.0F, 13.0F, 16.0F);
            break;
         case COOL:
            shades(graphics2D);
            smirk(graphics2D, 98.0F, 128.0F, false);
            break;
         case MONEY:
            dollarEyes(graphics2D);
            openSmile(graphics2D, 96.0F, 122.0F, 30.0F, 18.0F, false);
            banknoteTongue(graphics2D, 96.0F, 138.0F);
            break;
         case SLEEP:
            closedEye(graphics2D, 72.0F, 94.0F, 13.0F, true);
            closedEye(graphics2D, 120.0F, 94.0F, 13.0F, true);
            ring(graphics2D, 96.0F, 130.0F, 6.0F, 7.0F);
            zz(graphics2D);
      }
   }

   private static void sphere(Graphics2D graphics2D, float f, float g, float h) {
      for (int var4 = 6; var4 >= 1; var4--) {
         float var5 = var4 * 2.2F;
         graphics2D.setColor(new Color(0, 10, 40, 12));
         graphics2D.fill(new Float(f - h - var5 * 0.5F, g - h + 6.0F - var5 * 0.3F, (h + var5 * 0.5F) * 2.0F, (h + var5 * 0.4F) * 2.0F));
      }

      Float var6 = new Float(f - h, g - h, h * 2.0F, h * 2.0F);
      graphics2D.setPaint(
         new RadialGradientPaint(
            new java.awt.geom.Point2D.Float(f - h * 0.35F, g - h * 0.45F),
            h * 1.55F,
            new float[]{0.0F, 0.35F, 0.75F, 1.0F},
            new Color[]{new Color(11132159), new Color(4101621), new Color(1791689), new Color(798078)}
         )
      );
      graphics2D.fill(var6);
      graphics2D.setPaint(
         new RadialGradientPaint(
            new java.awt.geom.Point2D.Float(f, g + h * 0.9F), h * 1.1F, new float[]{0.0F, 1.0F}, new Color[]{new Color(7327999), new Color(7327999, true)}
         )
      );
      graphics2D.fill(var6);
      graphics2D.setStroke(new BasicStroke(2.5F));
      graphics2D.setColor(new Color(666222));
      graphics2D.draw(var6);
      AffineTransform var7 = graphics2D.getTransform();
      graphics2D.rotate(-0.5, f - h * 0.42F, g - h * 0.55F);
      graphics2D.setPaint(new GradientPaint(0.0F, g - h * 0.8F, new Color(255, 255, 255, 190), 0.0F, g - h * 0.25F, new Color(255, 255, 255, 0)));
      graphics2D.fill(new Float(f - h * 0.75F, g - h * 0.8F, h * 0.7F, h * 0.42F));
      graphics2D.setTransform(var7);
   }

   private static void eye(Graphics2D graphics2D, float f, float g, float h, float i, float j, float k) {
      AffineTransform var7 = graphics2D.getTransform();
      graphics2D.rotate(j, f, g);
      graphics2D.setColor(Color.WHITE);
      graphics2D.fill(new Float(f - h, g - i, h * 2.0F, i * 2.0F));
      graphics2D.setColor(INK);
      float var8 = Math.min(h, i) * 0.62F;
      graphics2D.fill(new Float(f - var8 + k, g - var8 + i * 0.15F, var8 * 2.0F, var8 * 2.0F));
      graphics2D.setColor(Color.WHITE);
      graphics2D.fill(new Float(f - var8 * 0.25F + k, g - var8 * 0.55F + i * 0.15F, var8 * 0.6F, var8 * 0.6F));
      graphics2D.setTransform(var7);
   }

   private static void closedEye(Graphics2D graphics2D, float f, float g, float h, boolean bl) {
      graphics2D.setColor(INK);
      graphics2D.setStroke(new BasicStroke(5.0F, 1, 1));
      float var5 = bl ? 8.0F : -8.0F;
      graphics2D.draw(new java.awt.geom.QuadCurve2D.Float(f - h, g, f, g + var5, f + h, g));
   }

   private static void happyEye(Graphics2D graphics2D, float f, float g) {
      graphics2D.setColor(INK);
      graphics2D.setStroke(new BasicStroke(6.0F, 1, 1));
      graphics2D.draw(new java.awt.geom.QuadCurve2D.Float(f - 13.0F, g + 6.0F, f, g - 14.0F, f + 13.0F, g + 6.0F));
   }

   private static void droopyEye(Graphics2D graphics2D, float f, float g) {
      Area var3 = new Area(new Float(f - 11.0F, g - 13.0F, 22.0F, 26.0F));
      var3.subtract(new Area(new Float(f - 16.0F, g - 26.0F, 32.0F, 22.0F)));
      graphics2D.setColor(Color.WHITE);
      graphics2D.fill(var3);
      graphics2D.setColor(INK);
      graphics2D.fill(new Float(f - 6.0F, g - 2.0F, 12.0F, 12.0F));
      graphics2D.setStroke(new BasicStroke(3.5F, 1, 1));
      graphics2D.draw(new java.awt.geom.QuadCurve2D.Float(f - 12.0F, g - 5.0F, f, g - 1.0F, f + 12.0F, g - 5.0F));
   }

   private static void brows(Graphics2D graphics2D, float f, float g, float h, float i, float j) {
      graphics2D.setColor(INK);
      graphics2D.setStroke(new BasicStroke(6.0F, 1, 1));
      float var6 = 12.0F;
      float var7 = 12.0F * j;
      graphics2D.draw(new java.awt.geom.QuadCurve2D.Float(f - var6, g - var7, f, g - 3.0F, f + var6, g + var7));
      graphics2D.draw(new java.awt.geom.QuadCurve2D.Float(h - var6, i + var7, h, i - 3.0F, h + var6, i - var7));
   }

   private static void openSmile(Graphics2D graphics2D, float f, float g, float h, float i, boolean bl) {
      GeneralPath var6 = new GeneralPath();
      var6.moveTo(f - h, g);
      var6.quadTo(f, g + i * 2.2F, f + h, g);
      var6.quadTo(f, g + i * 0.4F, f - h, g);
      var6.closePath();
      graphics2D.setColor(new Color(2755102));
      graphics2D.fill(var6);
      if (bl) {
         Shape var7 = graphics2D.getClip();
         graphics2D.clip(var6);
         graphics2D.setColor(new Color(16740241));
         graphics2D.fill(new Float(f - h * 0.5F, g + i * 0.7F, h, i * 1.4F));
         graphics2D.setClip(var7);
      }

      graphics2D.setColor(INK);
      graphics2D.setStroke(new BasicStroke(3.0F, 1, 1));
      graphics2D.draw(var6);
   }

   private static void openFrown(Graphics2D graphics2D, float f, float g, float h, float i) {
      GeneralPath var5 = new GeneralPath();
      var5.moveTo(f - h, g + i * 0.6F);
      var5.quadTo(f, g - i * 1.4F, f + h, g + i * 0.6F);
      var5.quadTo(f, g + i * 0.2F, f - h, g + i * 0.6F);
      var5.closePath();
      graphics2D.setColor(new Color(2755102));
      graphics2D.fill(var5);
      graphics2D.setColor(INK);
      graphics2D.setStroke(new BasicStroke(3.0F, 1, 1));
      graphics2D.draw(var5);
   }

   private static void frown(Graphics2D graphics2D, float f, float g, float h) {
      graphics2D.setColor(INK);
      graphics2D.setStroke(new BasicStroke(6.0F, 1, 1));
      graphics2D.draw(new java.awt.geom.QuadCurve2D.Float(f - h, g + 4.0F, f, g - 10.0F, f + h, g + 4.0F));
   }

   private static void smirk(Graphics2D graphics2D, float f, float g, boolean bl) {
      graphics2D.setColor(INK);
      graphics2D.setStroke(new BasicStroke(6.0F, 1, 1));
      float var4 = bl ? 30.0F : 22.0F;
      GeneralPath var5 = new GeneralPath();
      var5.moveTo(f - var4, g - 2.0F);
      var5.curveTo(f - var4 * 0.3F, g + 10.0F, f + var4 * 0.5F, g + 8.0F, f + var4, g - 10.0F);
      graphics2D.draw(var5);
   }

   private static void laughMouth(Graphics2D graphics2D, float f, float g) {
      GeneralPath var3 = new GeneralPath();
      var3.moveTo(f - 34.0F, g - 4.0F);
      var3.lineTo(f + 34.0F, g - 4.0F);
      var3.quadTo(f + 30.0F, g + 38.0F, f, g + 38.0F);
      var3.quadTo(f - 30.0F, g + 38.0F, f - 34.0F, g - 4.0F);
      var3.closePath();
      graphics2D.setColor(new Color(2755102));
      graphics2D.fill(var3);
      Shape var4 = graphics2D.getClip();
      graphics2D.clip(var3);
      graphics2D.setColor(Color.WHITE);
      graphics2D.fill(new java.awt.geom.RoundRectangle2D.Float(f - 34.0F, g - 6.0F, 68.0F, 13.0F, 6.0F, 6.0F));
      graphics2D.setColor(new Color(16740241));
      graphics2D.fill(new Float(f - 18.0F, g + 18.0F, 36.0F, 26.0F));
      graphics2D.setClip(var4);
      graphics2D.setColor(INK);
      graphics2D.setStroke(new BasicStroke(3.0F, 1, 1));
      graphics2D.draw(var3);
   }

   private static void teeth(Graphics2D graphics2D, float f, float g) {
      java.awt.geom.RoundRectangle2D.Float var3 = new java.awt.geom.RoundRectangle2D.Float(f - 26.0F, g - 10.0F, 52.0F, 20.0F, 12.0F, 12.0F);
      graphics2D.setColor(Color.WHITE);
      graphics2D.fill(var3);
      graphics2D.setColor(INK);
      graphics2D.setStroke(new BasicStroke(2.5F));
      graphics2D.draw(var3);
      graphics2D.drawLine((int)(f - 26.0F), (int)g, (int)(f + 26.0F), (int)g);

      for (int var4 = -2; var4 <= 2; var4++) {
         graphics2D.drawLine((int)(f + var4 * 9.0F), (int)(g - 10.0F), (int)(f + var4 * 9.0F), (int)(g + 10.0F));
      }
   }

   private static void ring(Graphics2D graphics2D, float f, float g, float h, float i) {
      graphics2D.setColor(new Color(2755102));
      graphics2D.fill(new Float(f - h, g - i, h * 2.0F, i * 2.0F));
      graphics2D.setColor(INK);
      graphics2D.setStroke(new BasicStroke(3.0F));
      graphics2D.draw(new Float(f - h, g - i, h * 2.0F, i * 2.0F));
   }

   private static void kissLips(Graphics2D graphics2D, float f, float g) {
      graphics2D.setColor(INK);
      graphics2D.setStroke(new BasicStroke(5.5F, 1, 1));
      GeneralPath var3 = new GeneralPath();
      var3.moveTo(f - 6.0F, g - 12.0F);
      var3.quadTo(f + 10.0F, g - 10.0F, f + 2.0F, g - 2.0F);
      var3.quadTo(f + 12.0F, g + 4.0F, f - 6.0F, g + 10.0F);
      graphics2D.draw(var3);
   }

   private static void tears(Graphics2D graphics2D, float f, float g) {
      graphics2D.setPaint(new GradientPaint(0.0F, g, new Color(13235199), 0.0F, g + 70.0F, new Color(5227519)));
      graphics2D.fill(new java.awt.geom.RoundRectangle2D.Float(f - 7.0F, g, 14.0F, 66.0F, 14.0F, 14.0F));
   }

   private static void drop(Graphics2D graphics2D, float f, float g, float h) {
      GeneralPath var4 = new GeneralPath();
      var4.moveTo(f, g - h * 2.2F);
      var4.curveTo(f + h * 1.2F, g - h * 0.6F, f + h, g + h, f, g + h);
      var4.curveTo(f - h, g + h, f - h * 1.2F, g - h * 0.6F, f, g - h * 2.2F);
      graphics2D.setColor(TEAR);
      graphics2D.fill(var4);
      graphics2D.setColor(new Color(255, 255, 255, 200));
      graphics2D.fill(new Float(f - h * 0.45F, g - h * 0.4F, h * 0.5F, h * 0.6F));
   }

   private static void shades(Graphics2D graphics2D) {
      GeneralPath var1 = new GeneralPath();
      var1.moveTo(42.0F, 80.0F);
      var1.lineTo(90.0F, 80.0F);
      var1.quadTo(90.0F, 108.0F, 68.0F, 108.0F);
      var1.quadTo(44.0F, 108.0F, 42.0F, 80.0F);
      var1.closePath();
      GeneralPath var2 = (GeneralPath)var1.clone();
      var2.transform(AffineTransform.getTranslateInstance(60.0, 0.0));
      graphics2D.setColor(new Color(724764));
      graphics2D.fill(var1);
      graphics2D.fill(var2);
      graphics2D.setStroke(new BasicStroke(6.0F, 1, 1));
      graphics2D.draw(new java.awt.geom.Line2D.Float(88.0F, 84.0F, 104.0F, 84.0F));
      graphics2D.setColor(new Color(255, 255, 255, 120));
      graphics2D.setStroke(new BasicStroke(4.0F, 1, 1));
      graphics2D.drawLine(52, 86, 62, 96);
      graphics2D.drawLine(112, 86, 122, 96);
   }

   private static void dollarEyes(Graphics2D graphics2D) {
      graphics2D.setFont(new Font("SansSerif", 1, 40));
      graphics2D.setColor(new Color(2347360));
      graphics2D.drawString("$", 60, 106);
      graphics2D.drawString("$", 108, 106);
   }

   private static void banknoteTongue(Graphics2D graphics2D, float f, float g) {
      graphics2D.setColor(new Color(4054148));
      graphics2D.fill(new java.awt.geom.RoundRectangle2D.Float(f - 14.0F, g - 4.0F, 28.0F, 26.0F, 6.0F, 6.0F));
      graphics2D.setColor(new Color(2002510));
      graphics2D.setStroke(new BasicStroke(2.0F));
      graphics2D.draw(new java.awt.geom.RoundRectangle2D.Float(f - 14.0F, g - 4.0F, 28.0F, 26.0F, 6.0F, 6.0F));
   }

   private static void zz(Graphics2D graphics2D) {
      graphics2D.setColor(Color.WHITE);
      graphics2D.setFont(new Font("SansSerif", 1, 30));
      graphics2D.drawString("Z", 140, 52);
      graphics2D.setFont(new Font("SansSerif", 1, 20));
      graphics2D.drawString("z", 162, 30);
   }

   private static void vein(Graphics2D graphics2D, float f, float g) {
      graphics2D.setColor(RED);
      graphics2D.setStroke(new BasicStroke(5.0F, 1, 1));

      for (int var3 = 0; var3 < 4; var3++) {
         AffineTransform var4 = graphics2D.getTransform();
         graphics2D.rotate(var3 * 3.141592653589793 / 2.0, f, g);
         graphics2D.draw(new java.awt.geom.QuadCurve2D.Float(f - 4.0F, g - 14.0F, f - 4.0F, g - 4.0F, f - 14.0F, g - 4.0F));
         graphics2D.setTransform(var4);
      }
   }

   private static void horns(Graphics2D graphics2D) {
      for (byte var1 = -1; var1 <= 1; var1 += 2) {
         GeneralPath var2 = new GeneralPath();
         float var3 = 96.0F + var1 * 40.0F;
         var2.moveTo(var3 - var1 * 16.0F, 48.0F);
         var2.quadTo(var3 + var1 * 12.0F, 34.0F, var3 + var1 * 18.0F, 10.0F);
         var2.quadTo(var3 + var1 * 26.0F, 40.0F, var3 + var1 * 14.0F, 62.0F);
         var2.closePath();
         graphics2D.setPaint(new GradientPaint(0.0F, 10.0F, new Color(14183423), 0.0F, 62.0F, new Color(6954936)));
         graphics2D.fill(var2);
         graphics2D.setColor(new Color(4001138));
         graphics2D.setStroke(new BasicStroke(2.5F));
         graphics2D.draw(var2);
      }
   }

   private static Shape heartShape(float f, float g, float h) {
      GeneralPath var3 = new GeneralPath();
      var3.moveTo(f, g + h * 0.95F);
      var3.curveTo(f - h * 1.35F, g + h * 0.05F, f - h * 1.1F, g - h * 1.05F, f, g - h * 0.45F);
      var3.curveTo(f + h * 1.1F, g - h * 1.05F, f + h * 1.35F, g + h * 0.05F, f, g + h * 0.95F);
      var3.closePath();
      return var3;
   }

   private static void heart(Graphics2D graphics2D, float f, float g, float h, boolean bl) {
      Shape var5 = heartShape(f, g, h);
      SkyEmoji.HeartPaint var6 = new SkyEmoji.HeartPaint(f, g, h);
      if (!bl) {
         var6.fill(graphics2D, var5);
      } else {
         GeneralPath var7 = new GeneralPath();
         var7.moveTo(f, g - h);
         var7.lineTo(f - h * 0.18F, g - h * 0.2F);
         var7.lineTo(f + h * 0.15F, g + h * 0.15F);
         var7.lineTo(f - h * 0.1F, g + h * 0.6F);
         var7.lineTo(f, g + h * 1.2F);
         var7.lineTo(f - h * 2.0F, g + h * 1.2F);
         var7.lineTo(f - h * 2.0F, g - h * 1.2F);
         var7.closePath();
         Area var8 = new Area(var5);
         var8.intersect(new Area(var7));
         Area var9 = new Area(var5);
         var9.subtract(new Area(var7));
         var8.transform(AffineTransform.getRotateInstance(-0.1, f, g + h));
         var8.transform(AffineTransform.getTranslateInstance(-5.0, 0.0));
         var9.transform(AffineTransform.getRotateInstance(0.1, f, g + h));
         var9.transform(AffineTransform.getTranslateInstance(5.0, 0.0));
         var6.fill(graphics2D, var8);
         var6.fill(graphics2D, var9);
      }
   }

   private static void flame(Graphics2D graphics2D) {
      GeneralPath var1 = new GeneralPath();
      var1.moveTo(96.0F, 176.0F);
      var1.curveTo(40.0F, 176.0F, 30.0F, 122.0F, 58.0F, 86.0F);
      var1.curveTo(66.0F, 110.0F, 76.0F, 112.0F, 80.0F, 108.0F);
      var1.curveTo(70.0F, 74.0F, 92.0F, 42.0F, 110.0F, 16.0F);
      var1.curveTo(112.0F, 58.0F, 150.0F, 76.0F, 152.0F, 120.0F);
      var1.curveTo(154.0F, 156.0F, 130.0F, 176.0F, 96.0F, 176.0F);
      var1.closePath();
      graphics2D.setPaint(new GradientPaint(0.0F, 16.0F, new Color(8382719), 0.0F, 176.0F, new Color(1791689)));
      graphics2D.fill(var1);
      graphics2D.setColor(new Color(666222));
      graphics2D.setStroke(new BasicStroke(2.5F));
      graphics2D.draw(var1);
      GeneralPath var2 = new GeneralPath();
      var2.moveTo(98.0F, 168.0F);
      var2.curveTo(70.0F, 168.0F, 66.0F, 136.0F, 82.0F, 116.0F);
      var2.curveTo(88.0F, 132.0F, 96.0F, 130.0F, 98.0F, 122.0F);
      var2.curveTo(96.0F, 102.0F, 108.0F, 86.0F, 116.0F, 76.0F);
      var2.curveTo(118.0F, 104.0F, 132.0F, 118.0F, 128.0F, 142.0F);
      var2.curveTo(126.0F, 160.0F, 114.0F, 168.0F, 98.0F, 168.0F);
      var2.closePath();
      graphics2D.setPaint(new GradientPaint(0.0F, 76.0F, Color.WHITE, 0.0F, 168.0F, new Color(11132159)));
      graphics2D.fill(var2);
   }

   private static void star(Graphics2D graphics2D) {
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
      graphics2D.setPaint(
         new RadialGradientPaint(
            new java.awt.geom.Point2D.Float(80.0F, 80.0F),
            100.0F,
            new float[]{0.0F, 0.5F, 1.0F},
            new Color[]{new Color(16774832), new Color(16762941), new Color(14255642)},
            CycleMethod.NO_CYCLE
         )
      );
      graphics2D.fill(var1);
      graphics2D.setColor(new Color(9064202));
      graphics2D.setStroke(new BasicStroke(2.5F, 1, 1));
      graphics2D.draw(var1);
      graphics2D.setColor(new Color(255, 255, 255, 170));
      graphics2D.fill(new Float(72.0F, 62.0F, 22.0F, 12.0F));
   }

   private static void moon(Graphics2D graphics2D) {
      Area var1 = new Area(new Float(30.0F, 26.0F, 140.0F, 140.0F));
      var1.subtract(new Area(new Float(70.0F, 8.0F, 130.0F, 130.0F)));
      graphics2D.setPaint(
         new RadialGradientPaint(
            new java.awt.geom.Point2D.Float(60.0F, 70.0F), 120.0F, new float[]{0.0F, 1.0F}, new Color[]{new Color(16775112), new Color(15909198)}
         )
      );
      graphics2D.fill(var1);
      graphics2D.setColor(new Color(10119954));
      graphics2D.setStroke(new BasicStroke(2.5F));
      graphics2D.draw(var1);
      graphics2D.setColor(new Color(16775112));
      sparkle(graphics2D, 150.0F, 140.0F, 12.0F);
      sparkle(graphics2D, 168.0F, 104.0F, 7.0F);
   }

   private static void sparkle(Graphics2D graphics2D, float f, float g, float h) {
      GeneralPath var4 = new GeneralPath();
      var4.moveTo(f, g - h);
      var4.quadTo(f, g, f + h, g);
      var4.quadTo(f, g, f, g + h);
      var4.quadTo(f, g, f - h, g);
      var4.quadTo(f, g, f, g - h);
      graphics2D.fill(var4);
   }

   private static void notes(Graphics2D graphics2D) {
      graphics2D.setPaint(new GradientPaint(0.0F, 30.0F, new Color(9425919), 0.0F, 170.0F, new Color(1791689)));
      graphics2D.fill(new Float(36.0F, 132.0F, 44.0F, 32.0F));
      graphics2D.fill(new Float(112.0F, 116.0F, 44.0F, 32.0F));
      graphics2D.fill(new java.awt.geom.Rectangle2D.Float(70.0F, 42.0F, 10.0F, 104.0F));
      graphics2D.fill(new java.awt.geom.Rectangle2D.Float(146.0F, 26.0F, 10.0F, 104.0F));
      GeneralPath var1 = new GeneralPath();
      var1.moveTo(70.0F, 42.0F);
      var1.lineTo(156.0F, 26.0F);
      var1.lineTo(156.0F, 50.0F);
      var1.lineTo(70.0F, 66.0F);
      var1.closePath();
      graphics2D.fill(var1);
      graphics2D.setColor(new Color(666222));
      graphics2D.setStroke(new BasicStroke(2.5F));
      graphics2D.draw(new Float(36.0F, 132.0F, 44.0F, 32.0F));
      graphics2D.draw(new Float(112.0F, 116.0F, 44.0F, 32.0F));
      graphics2D.setColor(new Color(255, 255, 255, 160));
      graphics2D.fill(new Float(46.0F, 138.0F, 14.0F, 8.0F));
      graphics2D.fill(new Float(122.0F, 122.0F, 14.0F, 8.0F));
   }

   @Environment(EnvType.CLIENT)
   private record HeartPaint(float cx, float cy, float r) {
      void fill(Graphics2D graphics2D, Shape shape) {
         graphics2D.setPaint(
            new RadialGradientPaint(
               new java.awt.geom.Point2D.Float(this.cx - this.r * 0.4F, this.cy - this.r * 0.5F),
               this.r * 1.7F,
               new float[]{0.0F, 0.45F, 1.0F},
               new Color[]{new Color(16752050), new Color(16726876), new Color(11014190)}
            )
         );
         graphics2D.fill(shape);
         graphics2D.setColor(new Color(7211552));
         graphics2D.setStroke(new BasicStroke(Math.max(1.5F, this.r * 0.05F)));
         graphics2D.draw(shape);
         Shape var3 = graphics2D.getClip();
         graphics2D.clip(shape);
         graphics2D.setColor(new Color(255, 255, 255, 150));
         graphics2D.fill(new Float(this.cx - this.r * 0.85F, this.cy - this.r * 0.75F, this.r * 0.55F, this.r * 0.35F));
         graphics2D.setClip(var3);
      }
   }

   @Environment(EnvType.CLIENT)
   record Match(SkyEmoji emoji, int start, int end) {
   }
}
