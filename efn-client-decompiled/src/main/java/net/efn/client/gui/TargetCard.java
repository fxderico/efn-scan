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
      EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
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

   float planWidth(float f) {
      return this.visible() ? Math.round(244.0F * f) : 1.0F;
   }

   float planHeight(float f) {
      return Math.round(62.0F * f);
   }

   void update(MinecraftClient minecraftClient, HudModule hudModule, boolean bl, boolean bl2, float f) {
      this.clock += f;
      if (this.target != null && (this.target.getEntityWorld() != minecraftClient.world || !bl2 && this.target == minecraftClient.player)) {
         this.reset();
      }

      Object var7 = bl ? pick(minecraftClient, hudModule) : null;
      if (var7 == null && bl2) {
         var7 = minecraftClient.player;
      }

      if (var7 != null && var7 != this.target) {
         this.adopt((LivingEntity)var7);
      }

      this.lost = var7 != null ? 0.0F : this.lost + f;
      boolean var6 = this.target != null && (var7 != null || this.lost < 0.8F);
      this.presence = bl2 && var6 ? 1.0F : MathHelper.clamp(this.presence + (var6 ? f / 0.2F : -f / 0.34F), 0.0F, 1.0F);
      if (this.presence <= 0.0F && !var6) {
         this.target = null;
         this.namedFor = null;
      } else {
         if (this.target != null) {
            this.follow(f);
         }
      }
   }

   private static LivingEntity pick(MinecraftClient minecraftClient, HudModule hudModule) {
      LivingEntity var2 = hudModule.targetOnLook() ? CombatTarget.current(4000L) : CombatTarget.lastHit(4000L);
      if (var2 == null || var2 == minecraftClient.player || var2 instanceof ArmorStandEntity) {
         return null;
      } else {
         return hudModule.targetPlayersOnly() && !(var2 instanceof PlayerEntity) ? null : var2;
      }
   }

   private void adopt(LivingEntity livingEntity) {
      this.target = livingEntity;
      float var2 = Math.max(1.0F, livingEntity.getMaxHealth());
      this.trail = this.lastHealth = MathHelper.clamp(livingEntity.getHealth(), 0.0F, var2);
      this.health = this.lastHealth;
      this.absorb = Math.max(0.0F, livingEntity.getAbsorptionAmount());
      this.trailHold = 0.0F;
      this.strike = 0.0F;
      this.lastHurt = livingEntity.hurtTime;
   }

   private void follow(float f) {
      float var2 = Math.max(1.0F, this.target.getMaxHealth());
      float var3 = MathHelper.clamp(this.target.getHealth(), 0.0F, var2);
      if (var3 < this.lastHealth - 0.01F) {
         this.trailHold = 0.4F;
         this.strike = 1.0F;
      }

      if (this.target.hurtTime > this.lastHurt) {
         this.strike = 1.0F;
      }

      this.lastHurt = this.target.hurtTime;
      this.lastHealth = var3;
      this.health = HudGlyphs.approach(this.health, var3, 13.0F, f);
      if (this.trailHold > 0.0F) {
         this.trailHold -= f;
      } else {
         this.trail = HudGlyphs.approach(this.trail, this.health, 4.5F, f);
      }

      if (this.trail < this.health) {
         this.trail = this.health;
      }

      this.absorb = HudGlyphs.approach(this.absorb, Math.max(0.0F, this.target.getAbsorptionAmount()), 13.0F, f);
      this.strike = Math.max(0.0F, this.strike - f / 0.45F);
   }

   void draw(ImDrawList imDrawList, HudLayoutStore.HudElementState hudElementState, float f, float g, float h, boolean bl, HudModule hudModule) {
      if (this.target != null) {
         float var8 = HudGlyphs.easeOutQuint(this.presence);
         float var9 = h * var8;
         if (!(var9 <= 0.004F)) {
            MinecraftClient var10 = MinecraftClient.getInstance();
            float var11 = HudRenderer.scaleOf(hudElementState);
            g = Math.round(g + 7.0F * var11 * (1.0F - var8));
            float var12 = hudElementState.width;
            float var13 = hudElementState.height;
            Surface var14 = HudRenderer.hudSurface(hudElementState);
            boolean var15 = HudChrome.needsTextGuard(var14);
            HudChrome.card(imDrawList, var14, f, g, var12, var13, HudRenderer.hudRounding() * var11, var9, bl, var11);
            ImGuiManager var16 = ImGuiManager.getInstance();
            ImFont var17 = var16.hudTextFont();
            ImFont var18 = var16.hudLabelFont();
            int var19 = HudChrome.px(17.0F, var11);
            int var20 = HudChrome.px(14.0F, var11);
            float var21 = f + Math.round(8.0F * var11);
            float var22 = g + Math.round(8.0F * var11);
            float var23 = Math.round(40.0F * var11);
            this.drawPortrait(imDrawList, var10, var21, var22 + Math.round((46.0F * var11 - var23) * 0.5F), var23, var11, var9);
            float var24 = var21 + var23 + Math.round(10.0F * var11);
            float var25 = f + var12 - Math.round(8.0F * var11);
            float var26 = Math.max(1.0F, this.target.getMaxHealth());
            float var27 = MathHelper.clamp(this.health / var26, 0.0F, 1.0F);
            float var28 = var22 + (float)Math.floor((18.0F * var11 - var19) * 0.5F);
            float var29 = var28 + HudChrome.baselineOffset(var17, var19, var18, var20);
            this.refreshNumbers(var10);
            float var30 = this.absorbText.isEmpty() ? 0.0F : HudChrome.width(var18, var20, this.absorbText);
            float var31 = HudChrome.tabularWidth(var17, var19, this.healthText);
            float var32 = 11.0F * var11;
            float var33 = var32 * 0.62F + 4.0F * var11 + var31 + (var30 > 0.0F ? 4.0F * var11 + var30 : 0.0F);
            float var34 = var25 - var33;
            HudGlyphs.bolt(imDrawList, var34 + var32 * 0.31F, var28 + var19 * 0.52F, var32, Math.max(1.0F, 1.3F * var11), HudGlyphs.ink(-15408912, var9));
            float var43;
            HudChrome.tabular(
               imDrawList,
               var17,
               var19,
               var43 = var34 + (var32 * 0.62F + 4.0F * var11),
               var28,
               HudGlyphs.ink(HudGlyphs.vitality(var27), var9),
               this.healthText,
               var15
            );
            if (var30 > 0.0F) {
               HudChrome.text(imDrawList, var18, var20, var43 + var31 + 4.0F * var11, var29, HudGlyphs.ink(-12750874, var9), this.absorbText, var15);
            }

            String var35 = this.fittedName(var17, var19, var25 - var33 - 8.0F * var11 - var24);
            HudChrome.text(imDrawList, var17, var19, var24, var28, HudChrome.scaleAlpha(HudChrome.TEXT_NAME, var9), var35, var15);
            float var36 = var22 + Math.round(22.0F * var11);
            this.drawBar(imDrawList, var24, var36, var25, Math.max(3.0F, (float)Math.round(6.0F * var11)), var26, var11, var9);
            int var37 = (int)ItemIcons.pixels(16.0F * var11);
            float var38 = var22 + Math.round(46.0F * var11) - var37;
            float var39 = HudChrome.width(var18, var20, this.distanceText);
            float var40 = var25 - var39;
            HudChrome.text(
               imDrawList,
               var18,
               var20,
               var40,
               var38 + Math.round((var37 - var20) * 0.5F),
               HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var9),
               this.distanceText,
               var15
            );
            if (hudModule.targetGear()) {
               this.drawGear(imDrawList, var24, var38, var37, var40 - 6.0F * var11, var11, var9);
            }

            float var41 = Math.max(1.0F, (float)Math.round(var11));
            HudGlyphs.register(
               imDrawList,
               f + var12 * 0.12F,
               f + var12 * 0.88F,
               g + var13 - var41 - Math.max(1.0F, (float)Math.round(2.0F * var11)),
               var41,
               HudGlyphs.ink(-15408912, var9 * 0.35F)
            );
         }
      }
   }

   private void drawPortrait(ImDrawList imDrawList, MinecraftClient minecraftClient, float f, float g, float h, float i, float j) {
      float var8 = Math.min(HudRenderer.hudRounding(), 6.0F) * i;
      imDrawList.addRectFilled(f, g, f + h, g + h, HudChrome.scaleAlpha(1493172224, j), var8);
      float var9 = Math.max(8.0F, Math.round(32.0F * i / 8.0F) * 8.0F);
      if (var9 > h - 4.0F) {
         var9 = Math.max(8.0F, var9 - 8.0F);
      }

      float var10 = Math.round(f + (h - var9) * 0.5F);
      float var11 = Math.round(g + (h - var9) * 0.5F);
      boolean var12 = false;
      if (this.target instanceof AbstractClientPlayerEntity var14) {
         int var15 = this.skinTexture(minecraftClient, var14);
         if (var15 != 0) {
            float var16 = Math.max(1.0F, (float)Math.round(2.0F * i));
            int var17 = HudChrome.scaleAlpha(-1, j);
            imDrawList.addImageRounded(var15, var10, var11, var10 + var9, var11 + var9, 0.125F, 0.125F, 0.25F, 0.25F, var17, var16, 240);
            imDrawList.addImageRounded(var15, var10, var11, var10 + var9, var11 + var9, 0.625F, 0.125F, 0.75F, 0.25F, var17, var16, 240);
            var12 = true;
         }
      } else {
         SpawnEggItem var18 = SpawnEggItem.forEntity(this.target.getType());
         if (var18 != null) {
            float var20 = ItemIcons.pixels(var9);
            float var22 = Math.round(f + (h - var20) * 0.5F);
            float var24 = Math.round(g + (h - var20) * 0.5F);
            ItemIcons.draw(imDrawList, var18, var22, var24, var20, j);
            var12 = true;
         }
      }

      if (!var12) {
         HudGlyphs.reticle(imDrawList, f + h * 0.5F, g + h * 0.46F, h * 0.3F, Math.max(1.0F, 1.4F * i), HudGlyphs.ink(-15408912, j * 0.85F));
      }

      int var19 = HudGlyphs.ink(HudDraw.mix(-15408912, -2537681, this.strike), j * (0.7F + 0.3F * this.strike));
      imDrawList.addRect(f, g, f + h, g + h, var19, var8, 0, Math.max(1.0F, i));
      float var21 = Math.max(1.0F, (float)Math.round(1.5F * i));
      float var23 = g + h + Math.max(1.0F, (float)Math.round(i));
      imDrawList.addRectFilled(f + h * 0.22F, var23, f + h * 0.78F, var23 + var21, HudGlyphs.ink(-15408912, j * 0.8F), var21 * 0.5F);
      if (this.strike > 0.0F) {
         float var25 = (1.0F - this.strike) * 6.0F * i;
         imDrawList.addRect(
            f - var25, g - var25, f + h + var25, g + h + var25, HudGlyphs.ink(-2537681, j * this.strike * 0.7F), var8 + var25, 0, Math.max(1.0F, 1.5F * i)
         );
      }
   }

   private void drawBar(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, float l) {
      float var9 = h - f;
      float var10 = i * 0.5F;
      float var11 = MathHelper.clamp(this.health / j, 0.0F, 1.0F);
      float var12 = MathHelper.clamp(this.trail / j, 0.0F, 1.0F);
      imDrawList.addRectFilled(f, g, h, g + i, HudChrome.scaleAlpha(452984831, l), var10);
      if (var12 > var11 + 0.002F) {
         float var13 = f + var9 * var12;
         imDrawList.addRectFilled(f, g, var13, g + i, HudGlyphs.ink(-6621958, l * 0.55F), Math.min(var10, (var13 - f) * 0.5F));
      }

      if (var11 > 0.0F) {
         float var18 = f + var9 * var11;
         float var14 = l;
         if (var11 < 0.25F) {
            var14 = l * (0.72F + 0.28F * (0.5F + 0.5F * (float)Math.sin(this.clock * 7.5F)));
         }

         float var15 = Math.min(var10, (var18 - f) * 0.5F);
         imDrawList.addRectFilled(f, g, var18, g + i, HudGlyphs.ink(HudGlyphs.vitality(var11), var14), var15);
         imDrawList.addRectFilled(f, g, var18, g + i * 0.45F, HudGlyphs.ink(-1, var14 * 0.16F), var15, 48);
      }

      float var19 = j <= 40.0F ? 2.0F : (j <= 200.0F ? 10.0F : j / 20.0F);
      float var20 = Math.max(1.0F, (float)Math.round(0.75F * k));
      int var21 = HudChrome.scaleAlpha(1191182336, l);

      for (float var16 = var19; var16 < j - 0.01F; var16 += var19) {
         float var17 = Math.round(f + var9 * var16 / j);
         imDrawList.addRectFilled(var17, g, var17 + var20, g + i, var21);
      }

      float var22 = MathHelper.clamp(this.absorb / j, 0.0F, 1.0F);
      if (var22 > 0.002F) {
         float var23 = f + var9 * var22;
         imDrawList.addRectFilled(f, g, var23, g + Math.max(1.5F, i * 0.45F), HudGlyphs.ink(-12750874, l * 0.95F), Math.min(var10, (var23 - f) * 0.5F), 48);
      }
   }

   private void drawGear(ImDrawList imDrawList, float f, float g, int i, float h, float j, float k) {
      float var8 = i + Math.round(3.0F * j);

      for (EquipmentSlot var12 : GEAR_ORDER) {
         ItemStack var14 = this.target.getEquippedStack(var12);
         if (!var14.isEmpty()) {
            if (f + i > h) {
               break;
            }

            ItemIcons.draw(imDrawList, var14, f, g, (float)i, k);
            if (var14.hasGlint()) {
               HudGlyphs.sparkle(imDrawList, f + i - 1.5F * j, g + 1.5F * j, 2.6F * j, HudGlyphs.ink(-6621958, k * 0.9F));
            }

            if (var14.isDamageable() && var14.getMaxDamage() > 0 && 1.0F - (float)var14.getDamage() / var14.getMaxDamage() < 0.25F) {
               float var15 = Math.max(1.0F, (float)Math.round(1.5F * j));
               imDrawList.addRectFilled(f + 1.0F, g + i + var15 * 0.5F, f + i - 1.0F, g + i + var15 * 1.5F, HudGlyphs.ink(-2537681, k), var15 * 0.5F);
            }

            f += var8;
         }
      }
   }

   private void refreshNumbers(MinecraftClient minecraftClient) {
      float var4 = Math.max(1.0F, this.target.getMaxHealth());
      int var5 = Math.round(MathHelper.clamp(this.target.getHealth(), 0.0F, var4) * 10.0F);
      if (var5 != this.healthTenths) {
         this.healthTenths = var5;
         String var6 = this.healthText = var5 % 10 == 0 ? Integer.toString(var5 / 10) : var5 / 10 + "." + var5 % 10;
      }

      int var3;
      if ((var3 = Math.round(Math.max(0.0F, this.target.getAbsorptionAmount()))) != this.absorbWhole) {
         this.absorbWhole = var3;
         this.absorbText = var3 > 0 ? "+" + var3 : "";
      }

      int var2 = minecraftClient.player == null ? 0 : Math.round(minecraftClient.player.distanceTo(this.target) * 10.0F);
      if (var2 != this.distanceTenths) {
         this.distanceTenths = var2;
         this.distanceText = var2 / 10 + "." + var2 % 10 + "m";
      }
   }

   private String fittedName(ImFont imFont, int i, float f) {
      int var5 = NameProtectModule.generation();
      long var6 = Util.getMeasuringTimeMs();
      if (this.target != this.namedFor || var5 != this.nameGeneration || var6 - this.namedAt > 1000L) {
         this.namedFor = this.target;
         this.nameGeneration = var5;
         this.namedAt = var6;
         String var8 = NameProtectModule.protect(this.target.getName()).getString();
         if (!var8.equals(this.rawName)) {
            this.rawName = var8;
            this.fittedWidth = -1;
         }
      }

      int var4;
      if ((var4 = Math.max(0, (int)f)) == this.fittedWidth && i == this.fittedPx) {
         return this.fittedName;
      } else {
         this.fittedWidth = var4;
         this.fittedPx = i;
         this.fittedName = fit(imFont, i, this.rawName, var4);
         return this.fittedName;
      }
   }

   private static String fit(ImFont imFont, int i, String string, float f) {
      if (HudChrome.width(imFont, i, string) <= f) {
         return string;
      } else {
         float var4 = HudChrome.width(imFont, i, "...");
         int var5 = 0;
         int var6 = string.length();

         while (var5 < var6) {
            int var7 = var5 + var6 + 1 >>> 1;
            if (HudChrome.width(imFont, i, string.substring(0, var7)) + var4 <= f) {
               var5 = var7;
            } else {
               var6 = var7 - 1;
            }
         }

         return var5 == 0 ? "" : string.substring(0, var5).stripTrailing() + "...";
      }
   }

   private int skinTexture(MinecraftClient minecraftClient, AbstractClientPlayerEntity abstractClientPlayerEntity) {
      SkinTextures var5 = abstractClientPlayerEntity.getSkin();
      if (NameProtectModule.hidesName(abstractClientPlayerEntity.getGameProfile().name())) {
         var5 = NameProtectModule.hiddenHead(abstractClientPlayerEntity.getUuid(), var5);
      }

      Identifier var6 = var5.body().texturePath();

      GpuTexture var4;
      try {
         var4 = minecraftClient.getTextureManager().getTexture(var6).getGlTexture();
      } catch (IllegalStateException var8) {
         return 0;
      }

      GlTexture var3;
      if (var4 instanceof GlTexture && !(var3 = (GlTexture)var4).isClosed()) {
         if (this.crispSkins.add(var4)) {
            int var7 = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, var3.getGlId());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glBindTexture(3553, var7);
         }

         return var3.getGlId();
      } else {
         return 0;
      }
   }
}
