package net.efn.client.gui;

import com.mojang.blaze3d.textures.GpuTexture;
import imgui.ImDrawList;
import imgui.ImFont;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.efn.client.config.Surface;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.impl.client.HudModule;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.efn.client.util.combat.CombatTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
final class TargetCard {
   private static final long COMBAT_MEMORY_MS = 4000L;
   private static final float LINGER = 0.8F;
   private static final float SHOW_TIME = 0.2F;
   private static final float HIDE_TIME = 0.34F;
   private static final float ENTER_LIFT = 7.0F;
   private static final float HEALTH_RATE = 13.0F;
   private static final float TRAIL_HOLD = 0.4F;
   private static final float TRAIL_RATE = 4.5F;
   private static final float STRIKE_TIME = 0.45F;
   private static final long NAME_REFRESH_MS = 1000L;
   private static final float PAD = 8.0F;
   private static final float PORTRAIT = 40.0F;
   private static final float FACE = 32.0F;
   private static final float CONTENT_H = 46.0F;
   private static final float COLUMN_GAP = 10.0F;
   private static final float COLUMN_W = 178.0F;
   private static final float NAME_ROW = 18.0F;
   private static final float BAR_TOP = 22.0F;
   private static final float BAR_H = 6.0F;
   private static final float GEAR = 16.0F;
   private static final float GEAR_GAP = 3.0F;
   private static final float BOLT_H = 11.0F;
   private static final float BOLT_GAP = 4.0F;
   private static final float CLUSTER_GAP = 8.0F;
   private static final int MAT = 1493172224;
   private static final int NOTCH = 1191182336;
   private static final String ELLIPSIS = "...";
   private static final float SKIN_FACE_U = 0.125F;
   private static final float SKIN_HAT_U = 0.625F;
   private static final float SKIN_TOP_V = 0.125F;
   private static final float SKIN_PART = 0.125F;
   private static final EquipmentSlot[] GEAR_ORDER = new EquipmentSlot[]{
      EquipmentSlot.field_6173,
      EquipmentSlot.field_6171,
      EquipmentSlot.field_6169,
      EquipmentSlot.field_6174,
      EquipmentSlot.field_6172,
      EquipmentSlot.field_6166
   };
   private LivingEntity target;
   private float presence;
   private float lost;
   private float health;
   private float trail;
   private float trailHold;
   private float absorb;
   private float lastHealth;
   private int lastHurt;
   private float strike;
   private float clock;
   private LivingEntity namedFor;
   private int nameGeneration = -2147483648;
   private long namedAt;
   private String rawName = "";
   private String fittedName = "";
   private int fittedWidth = -1;
   private int fittedPx = -1;
   private int distanceTenths = -2147483648;
   private String distanceText = "";
   private int healthTenths = -2147483648;
   private String healthText = "";
   private int absorbWhole = -2147483648;
   private String absorbText = "";
   private final Set<GpuTexture> crispSkins = Collections.newSetFromMap(new WeakHashMap<>());

   TargetCard() {
   }

   void reset() {
      this.target = null;
      this.namedFor = null;
      this.presence = 0.0F;
      this.lost = 0.0F;
   }

   boolean visible() {
      return this.target != null && this.presence > 0.0F;
   }

   float planWidth(float var1) {
      return this.visible() ? Math.round(244.0F * var1) : 1.0F;
   }

   float planHeight(float var1) {
      return Math.round(62.0F * var1);
   }

   void update(MinecraftClient var1, HudModule var2, boolean var3, boolean var4, float var5) {
      this.clock += var5;
      if (this.target != null && (this.target.method_73183() != var1.field_1687 || !var4 && this.target == var1.field_1724)) {
         this.reset();
      }

      Object var7 = var3 ? pick(var1, var2) : null;
      if (var7 == null && var4) {
         var7 = var1.field_1724;
      }

      if (var7 != null && var7 != this.target) {
         this.adopt((LivingEntity)var7);
      }

      this.lost = var7 != null ? 0.0F : this.lost + var5;
      boolean var6 = this.target != null && (var7 != null || this.lost < 0.8F);
      this.presence = var4 && var6 ? 1.0F : MathHelper.method_15363(this.presence + (var6 ? var5 / 0.2F : -var5 / 0.34F), 0.0F, 1.0F);
      if (this.presence <= 0.0F && !var6) {
         this.target = null;
         this.namedFor = null;
      } else {
         if (this.target != null) {
            this.follow(var5);
         }
      }
   }

   private static LivingEntity pick(MinecraftClient var0, HudModule var1) {
      LivingEntity var2 = var1.targetOnLook() ? CombatTarget.current(4000L) : CombatTarget.lastHit(4000L);
      if (var2 == null || var2 == var0.field_1724 || var2 instanceof ArmorStandEntity) {
         return null;
      } else {
         return var1.targetPlayersOnly() && !(var2 instanceof PlayerEntity) ? null : var2;
      }
   }

   private void adopt(LivingEntity var1) {
      this.target = var1;
      float var2 = Math.max(1.0F, var1.method_6063());
      this.trail = this.lastHealth = MathHelper.method_15363(var1.method_6032(), 0.0F, var2);
      this.health = this.lastHealth;
      this.absorb = Math.max(0.0F, var1.method_6067());
      this.trailHold = 0.0F;
      this.strike = 0.0F;
      this.lastHurt = var1.field_6235;
   }

   private void follow(float var1) {
      float var2 = Math.max(1.0F, this.target.method_6063());
      float var3 = MathHelper.method_15363(this.target.method_6032(), 0.0F, var2);
      if (var3 < this.lastHealth - 0.01F) {
         this.trailHold = 0.4F;
         this.strike = 1.0F;
      }

      if (this.target.field_6235 > this.lastHurt) {
         this.strike = 1.0F;
      }

      this.lastHurt = this.target.field_6235;
      this.lastHealth = var3;
      this.health = HudGlyphs.approach(this.health, var3, 13.0F, var1);
      if (this.trailHold > 0.0F) {
         this.trailHold -= var1;
      } else {
         this.trail = HudGlyphs.approach(this.trail, this.health, 4.5F, var1);
      }

      if (this.trail < this.health) {
         this.trail = this.health;
      }

      this.absorb = HudGlyphs.approach(this.absorb, Math.max(0.0F, this.target.method_6067()), 13.0F, var1);
      this.strike = Math.max(0.0F, this.strike - var1 / 0.45F);
   }

   void draw(ImDrawList var1, HudLayoutStore.HudElementState var2, float var3, float var4, float var5, boolean var6, HudModule var7) {
      if (this.target != null) {
         float var8 = HudGlyphs.easeOutQuint(this.presence);
         float var9 = var5 * var8;
         if (!(var9 <= 0.004F)) {
            MinecraftClient var10 = MinecraftClient.method_1551();
            float var11 = HudRenderer.scaleOf(var2);
            var4 = Math.round(var4 + 7.0F * var11 * (1.0F - var8));
            float var12 = var2.width;
            float var13 = var2.height;
            Surface var14 = HudRenderer.hudSurface(var2);
            boolean var15 = HudChrome.needsTextGuard(var14);
            HudChrome.card(var1, var14, var3, var4, var12, var13, HudRenderer.hudRounding() * var11, var9, var6, var11);
            ImGuiManager var16 = ImGuiManager.getInstance();
            ImFont var17 = var16.hudTextFont();
            ImFont var18 = var16.hudLabelFont();
            int var19 = HudChrome.px(17.0F, var11);
            int var20 = HudChrome.px(14.0F, var11);
            float var21 = var3 + Math.round(8.0F * var11);
            float var22 = var4 + Math.round(8.0F * var11);
            float var23 = Math.round(40.0F * var11);
            this.drawPortrait(var1, var10, var21, var22 + Math.round((46.0F * var11 - var23) * 0.5F), var23, var11, var9);
            float var24 = var21 + var23 + Math.round(10.0F * var11);
            float var25 = var3 + var12 - Math.round(8.0F * var11);
            float var26 = Math.max(1.0F, this.target.method_6063());
            float var27 = MathHelper.method_15363(this.health / var26, 0.0F, 1.0F);
            float var28 = var22 + (float)Math.floor((18.0F * var11 - var19) * 0.5F);
            float var29 = var28 + HudChrome.baselineOffset(var17, var19, var18, var20);
            this.refreshNumbers(var10);
            float var30 = this.absorbText.isEmpty() ? 0.0F : HudChrome.width(var18, var20, this.absorbText);
            float var31 = HudChrome.tabularWidth(var17, var19, this.healthText);
            float var32 = 11.0F * var11;
            float var33 = var32 * 0.62F + 4.0F * var11 + var31 + (var30 > 0.0F ? 4.0F * var11 + var30 : 0.0F);
            float var34 = var25 - var33;
            HudGlyphs.bolt(var1, var34 + var32 * 0.31F, var28 + var19 * 0.52F, var32, Math.max(1.0F, 1.3F * var11), HudGlyphs.ink(-15408912, var9));
            float var43;
            HudChrome.tabular(
               var1,
               var17,
               var19,
               var43 = var34 + (var32 * 0.62F + 4.0F * var11),
               var28,
               HudGlyphs.ink(HudGlyphs.vitality(var27), var9),
               this.healthText,
               var15
            );
            if (var30 > 0.0F) {
               HudChrome.text(var1, var18, var20, var43 + var31 + 4.0F * var11, var29, HudGlyphs.ink(-12750874, var9), this.absorbText, var15);
            }

            String var35 = this.fittedName(var17, var19, var25 - var33 - 8.0F * var11 - var24);
            HudChrome.text(var1, var17, var19, var24, var28, HudChrome.scaleAlpha(HudChrome.TEXT_NAME, var9), var35, var15);
            float var36 = var22 + Math.round(22.0F * var11);
            this.drawBar(var1, var24, var36, var25, Math.max(3.0F, (float)Math.round(6.0F * var11)), var26, var11, var9);
            int var37 = (int)ItemIcons.pixels(16.0F * var11);
            float var38 = var22 + Math.round(46.0F * var11) - var37;
            float var39 = HudChrome.width(var18, var20, this.distanceText);
            float var40 = var25 - var39;
            HudChrome.text(
               var1,
               var18,
               var20,
               var40,
               var38 + Math.round((var37 - var20) * 0.5F),
               HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var9),
               this.distanceText,
               var15
            );
            if (var7.targetGear()) {
               this.drawGear(var1, var24, var38, var37, var40 - 6.0F * var11, var11, var9);
            }

            float var41 = Math.max(1.0F, (float)Math.round(var11));
            HudGlyphs.register(
               var1,
               var3 + var12 * 0.12F,
               var3 + var12 * 0.88F,
               var4 + var13 - var41 - Math.max(1.0F, (float)Math.round(2.0F * var11)),
               var41,
               HudGlyphs.ink(-15408912, var9 * 0.35F)
            );
         }
      }
   }

   private void drawPortrait(ImDrawList var1, MinecraftClient var2, float var3, float var4, float var5, float var6, float var7) {
      float var8 = Math.min(HudRenderer.hudRounding(), 6.0F) * var6;
      var1.addRectFilled(var3, var4, var3 + var5, var4 + var5, HudChrome.scaleAlpha(1493172224, var7), var8);
      float var9 = Math.max(8.0F, Math.round(32.0F * var6 / 8.0F) * 8.0F);
      if (var9 > var5 - 4.0F) {
         var9 = Math.max(8.0F, var9 - 8.0F);
      }

      float var10 = Math.round(var3 + (var5 - var9) * 0.5F);
      float var11 = Math.round(var4 + (var5 - var9) * 0.5F);
      boolean var12 = false;
      if (this.target instanceof AbstractClientPlayerEntity var14) {
         int var15 = this.skinTexture(var2, var14);
         if (var15 != 0) {
            float var16 = Math.max(1.0F, (float)Math.round(2.0F * var6));
            int var17 = HudChrome.scaleAlpha(-1, var7);
            var1.addImageRounded(var15, var10, var11, var10 + var9, var11 + var9, 0.125F, 0.125F, 0.25F, 0.25F, var17, var16, 240);
            var1.addImageRounded(var15, var10, var11, var10 + var9, var11 + var9, 0.625F, 0.125F, 0.75F, 0.25F, var17, var16, 240);
            var12 = true;
         }
      } else {
         SpawnEggItem var18 = SpawnEggItem.method_8019(this.target.method_5864());
         if (var18 != null) {
            float var20 = ItemIcons.pixels(var9);
            float var22 = Math.round(var3 + (var5 - var20) * 0.5F);
            float var24 = Math.round(var4 + (var5 - var20) * 0.5F);
            ItemIcons.draw(var1, var18, var22, var24, var20, var7);
            var12 = true;
         }
      }

      if (!var12) {
         HudGlyphs.reticle(var1, var3 + var5 * 0.5F, var4 + var5 * 0.46F, var5 * 0.3F, Math.max(1.0F, 1.4F * var6), HudGlyphs.ink(-15408912, var7 * 0.85F));
      }

      int var19 = HudGlyphs.ink(HudDraw.mix(-15408912, -2537681, this.strike), var7 * (0.7F + 0.3F * this.strike));
      var1.addRect(var3, var4, var3 + var5, var4 + var5, var19, var8, 0, Math.max(1.0F, var6));
      float var21 = Math.max(1.0F, (float)Math.round(1.5F * var6));
      float var23 = var4 + var5 + Math.max(1.0F, (float)Math.round(var6));
      var1.addRectFilled(var3 + var5 * 0.22F, var23, var3 + var5 * 0.78F, var23 + var21, HudGlyphs.ink(-15408912, var7 * 0.8F), var21 * 0.5F);
      if (this.strike > 0.0F) {
         float var25 = (1.0F - this.strike) * 6.0F * var6;
         var1.addRect(
            var3 - var25,
            var4 - var25,
            var3 + var5 + var25,
            var4 + var5 + var25,
            HudGlyphs.ink(-2537681, var7 * this.strike * 0.7F),
            var8 + var25,
            0,
            Math.max(1.0F, 1.5F * var6)
         );
      }
   }

   private void drawBar(ImDrawList var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = var4 - var2;
      float var10 = var5 * 0.5F;
      float var11 = MathHelper.method_15363(this.health / var6, 0.0F, 1.0F);
      float var12 = MathHelper.method_15363(this.trail / var6, 0.0F, 1.0F);
      var1.addRectFilled(var2, var3, var4, var3 + var5, HudChrome.scaleAlpha(452984831, var8), var10);
      if (var12 > var11 + 0.002F) {
         float var13 = var2 + var9 * var12;
         var1.addRectFilled(var2, var3, var13, var3 + var5, HudGlyphs.ink(-6621958, var8 * 0.55F), Math.min(var10, (var13 - var2) * 0.5F));
      }

      if (var11 > 0.0F) {
         float var18 = var2 + var9 * var11;
         float var14 = var8;
         if (var11 < 0.25F) {
            var14 = var8 * (0.72F + 0.28F * (0.5F + 0.5F * (float)Math.sin(this.clock * 7.5F)));
         }

         float var15 = Math.min(var10, (var18 - var2) * 0.5F);
         var1.addRectFilled(var2, var3, var18, var3 + var5, HudGlyphs.ink(HudGlyphs.vitality(var11), var14), var15);
         var1.addRectFilled(var2, var3, var18, var3 + var5 * 0.45F, HudGlyphs.ink(-1, var14 * 0.16F), var15, 48);
      }

      float var19 = var6 <= 40.0F ? 2.0F : (var6 <= 200.0F ? 10.0F : var6 / 20.0F);
      float var20 = Math.max(1.0F, (float)Math.round(0.75F * var7));
      int var21 = HudChrome.scaleAlpha(1191182336, var8);

      for (float var16 = var19; var16 < var6 - 0.01F; var16 += var19) {
         float var17 = Math.round(var2 + var9 * var16 / var6);
         var1.addRectFilled(var17, var3, var17 + var20, var3 + var5, var21);
      }

      float var22 = MathHelper.method_15363(this.absorb / var6, 0.0F, 1.0F);
      if (var22 > 0.002F) {
         float var23 = var2 + var9 * var22;
         var1.addRectFilled(
            var2, var3, var23, var3 + Math.max(1.5F, var5 * 0.45F), HudGlyphs.ink(-12750874, var8 * 0.95F), Math.min(var10, (var23 - var2) * 0.5F), 48
         );
      }
   }

   private void drawGear(ImDrawList var1, float var2, float var3, int var4, float var5, float var6, float var7) {
      float var8 = var4 + Math.round(3.0F * var6);

      for (EquipmentSlot var12 : GEAR_ORDER) {
         ItemStack var14 = this.target.method_6118(var12);
         if (!var14.method_7960()) {
            if (var2 + var4 > var5) {
               break;
            }

            ItemIcons.draw(var1, var14, var2, var3, (float)var4, var7);
            if (var14.method_7958()) {
               HudGlyphs.sparkle(var1, var2 + var4 - 1.5F * var6, var3 + 1.5F * var6, 2.6F * var6, HudGlyphs.ink(-6621958, var7 * 0.9F));
            }

            if (var14.method_7963() && var14.method_7936() > 0 && 1.0F - (float)var14.method_7919() / var14.method_7936() < 0.25F) {
               float var15 = Math.max(1.0F, (float)Math.round(1.5F * var6));
               var1.addRectFilled(
                  var2 + 1.0F, var3 + var4 + var15 * 0.5F, var2 + var4 - 1.0F, var3 + var4 + var15 * 1.5F, HudGlyphs.ink(-2537681, var7), var15 * 0.5F
               );
            }

            var2 += var8;
         }
      }
   }

   private void refreshNumbers(MinecraftClient var1) {
      float var4 = Math.max(1.0F, this.target.method_6063());
      int var5 = Math.round(MathHelper.method_15363(this.target.method_6032(), 0.0F, var4) * 10.0F);
      if (var5 != this.healthTenths) {
         this.healthTenths = var5;
         String var6 = this.healthText = var5 % 10 == 0 ? Integer.toString(var5 / 10) : var5 / 10 + "." + var5 % 10;
      }

      int var3;
      if ((var3 = Math.round(Math.max(0.0F, this.target.method_6067()))) != this.absorbWhole) {
         this.absorbWhole = var3;
         this.absorbText = var3 > 0 ? "+" + var3 : "";
      }

      int var2 = var1.field_1724 == null ? 0 : Math.round(var1.field_1724.method_5739((Entity)this.target) * 10.0F);
      if (var2 != this.distanceTenths) {
         this.distanceTenths = var2;
         this.distanceText = var2 / 10 + "." + var2 % 10 + "m";
      }
   }

   private String fittedName(ImFont var1, int var2, float var3) {
      int var5 = NameProtectModule.generation();
      long var6 = Util.method_658();
      if (this.target != this.namedFor || var5 != this.nameGeneration || var6 - this.namedAt > 1000L) {
         this.namedFor = this.target;
         this.nameGeneration = var5;
         this.namedAt = var6;
         String var8 = NameProtectModule.protect(this.target.method_5477()).getString();
         if (!var8.equals(this.rawName)) {
            this.rawName = var8;
            this.fittedWidth = -1;
         }
      }

      int var4;
      if ((var4 = Math.max(0, (int)var3)) == this.fittedWidth && var2 == this.fittedPx) {
         return this.fittedName;
      } else {
         this.fittedWidth = var4;
         this.fittedPx = var2;
         this.fittedName = fit(var1, var2, this.rawName, var4);
         return this.fittedName;
      }
   }

   private static String fit(ImFont var0, int var1, String var2, float var3) {
      if (HudChrome.width(var0, var1, var2) <= var3) {
         return var2;
      } else {
         float var4 = HudChrome.width(var0, var1, "...");
         int var5 = 0;
         int var6 = var2.length();

         while (var5 < var6) {
            int var7 = var5 + var6 + 1 >>> 1;
            if (HudChrome.width(var0, var1, var2.substring(0, var7)) + var4 <= var3) {
               var5 = var7;
            } else {
               var6 = var7 - 1;
            }
         }

         return var5 == 0 ? "" : var2.substring(0, var5).stripTrailing() + "...";
      }
   }

   private int skinTexture(MinecraftClient var1, AbstractClientPlayerEntity var2) {
      SkinTextures var5 = var2.method_52814();
      if (NameProtectModule.hidesName(var2.method_7334().name())) {
         var5 = NameProtectModule.hiddenHead(var2.method_5667(), var5);
      }

      Identifier var6 = var5.comp_1626().comp_3627();

      GpuTexture var4;
      try {
         var4 = var1.method_1531().method_4619(var6).method_68004();
      } catch (IllegalStateException var8) {
         return 0;
      }

      GlTexture var3;
      if (var4 instanceof GlTexture && !(var3 = (GlTexture)var4).isClosed()) {
         if (this.crispSkins.add(var4)) {
            int var7 = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, var3.method_68427());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glBindTexture(3553, var7);
         }

         return var3.method_68427();
      } else {
         return 0;
      }
   }
}
