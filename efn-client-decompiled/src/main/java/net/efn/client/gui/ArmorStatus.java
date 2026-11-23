package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import net.efn.client.config.Surface;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.impl.client.HudModule;
import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
final class ArmorStatus {
   private static final EquipmentSlot[] ARMOR = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final EquipmentSlot[] ARMOR_AND_HANDS = new EquipmentSlot[]{
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
   };
   private static final float PAD = 6.0F;
   private static final float ICON = 18.0F;
   private static final float TILE_GAP = 5.0F;
   private static final float HANDS_GAP = 11.0F;
   private static final float GAUGE_H = 3.0F;
   private static final float GAUGE_GAP = 3.0F;
   private static final float TEXT_GAP = 2.0F;
   private static final float ROW_GAP = 4.0F;
   private static final float SIDE_GAP = 7.0F;
   private static final float SIDE_GAUGE_W = 44.0F;
   private static final float CRITICAL = 0.12F;
   private static final float GAUGE_RATE = 10.0F;
   private static final float SHOW_TIME = 0.25F;
   private static final String WIDEST = "100%";
   private static final String[] PERCENT = new String[101];
   private static final String[] COUNT = new String[100];
   private final float[] wear = new float[ARMOR_AND_HANDS.length];
   private final Item[] seen = new Item[ARMOR_AND_HANDS.length];
   private float presence;
   private float clock;
   private boolean column;
   private boolean hands;
   private boolean gauge;
   private boolean percent;

   ArmorStatus() {
   }

   void reset() {
      this.presence = 0.0F;

      for (int var1 = 0; var1 < this.seen.length; var1++) {
         this.seen[var1] = null;
      }
   }

   boolean visible() {
      return this.presence > 0.0F;
   }

   void update(MinecraftClient minecraftClient, HudModule hudModule, boolean bl, boolean bl2, float f) {
      this.clock += f;
      this.column = hudModule.armorColumn();
      this.hands = hudModule.armorHands();
      HudModule.ArmorWear var7 = hudModule.armorWear();
      this.gauge = var7 != HudModule.ArmorWear.PERCENT;
      this.percent = var7 != HudModule.ArmorWear.GAUGE;
      ClientPlayerEntity var8 = minecraftClient.player;
      boolean var9 = false;
      EquipmentSlot[] var10 = this.order();

      for (int var11 = 0; var11 < var10.length; var11++) {
         ItemStack var12 = var8 == null ? ItemStack.EMPTY : var8.getEquippedStack(var10[var11]);
         if (!var12.isEmpty()) {
            var9 = true;
         }

         float var14 = remaining(var12);
         if (var12.getItem() != this.seen[var11]) {
            this.seen[var11] = var12.getItem();
            this.wear[var11] = var14;
         } else {
            this.wear[var11] = var14 < 0.0F ? var14 : HudGlyphs.approach(Math.max(0.0F, this.wear[var11]), var14, 10.0F, f);
         }
      }

      boolean var6 = bl2 || bl && var9;
      this.presence = bl2 ? 1.0F : Math.max(0.0F, Math.min(1.0F, this.presence + (var6 ? f : -f) / 0.25F));
   }

   private EquipmentSlot[] order() {
      return this.hands ? ARMOR_AND_HANDS : ARMOR;
   }

   private static float remaining(ItemStack itemStack) {
      return !itemStack.isEmpty() && itemStack.isDamageable() && itemStack.getMaxDamage() > 0
         ? Math.max(0.0F, 1.0F - (float)itemStack.getDamage() / itemStack.getMaxDamage())
         : -1.0F;
   }

   float planWidth(float f) {
      if (!this.visible()) {
         return 1.0F;
      } else {
         int var3 = this.order().length;
         float var4 = ItemIcons.pixels(18.0F * f);
         float var2 = this.percent ? (float)Math.ceil(percentWidth(f)) : 0.0F;
         if (this.column) {
            float var7 = (this.percent ? var2 : 0.0F) + (this.gauge ? 44.0F * f : 0.0F) + (this.percent && this.gauge ? 7.0F * f : 0.0F);
            return Math.round(6.0F * f * 2.0F + var4 + 7.0F * f + var7);
         } else {
            float var6 = Math.max(var4, var2);
            return Math.round(6.0F * f * 2.0F + var3 * var6 + (var3 - 1) * 5.0F * f + (this.hands ? 11.0F * f - 5.0F * f : 0.0F));
         }
      }
   }

   float planHeight(float f) {
      int var2 = this.order().length;
      float var3 = ItemIcons.pixels(18.0F * f);
      if (this.column) {
         return Math.round(6.0F * f * 2.0F + var2 * var3 + (var2 - 1) * 4.0F * f + (this.hands ? 11.0F * f - 4.0F * f : 0.0F));
      } else {
         float var4 = (this.gauge ? 3.0F * f + gaugeHeight(f) : 0.0F) + (this.percent ? 2.0F * f + HudChrome.px(14.0F, f) : 0.0F);
         return Math.round(6.0F * f * 2.0F + var3 + var4);
      }
   }

   private static float percentWidth(float f) {
      return HudChrome.tabularWidth(ImGuiManager.getInstance().hudLabelFont(), HudChrome.px(14.0F, f), "100%");
   }

   private static float gaugeHeight(float f) {
      return Math.max(2.0F, (float)Math.round(3.0F * f));
   }

   void draw(ImDrawList imDrawList, HudLayoutStore.HudElementState hudElementState, float f, float g, float h, boolean bl, MinecraftClient minecraftClient) {
      float var8 = h * HudGlyphs.easeOutQuint(this.presence);
      if (!(var8 <= 0.004F) && minecraftClient.player != null) {
         float var9 = HudRenderer.scaleOf(hudElementState);
         float var10 = hudElementState.width;
         float var11 = hudElementState.height;
         Surface var12 = HudRenderer.hudSurface(hudElementState);
         boolean var13 = HudChrome.needsTextGuard(var12);
         HudChrome.card(imDrawList, var12, f, g, var10, var11, HudRenderer.hudRounding() * var9, var8, bl, var9);
         ImFont var14 = ImGuiManager.getInstance().hudLabelFont();
         int var15 = HudChrome.px(14.0F, var9);
         int var16 = (int)ItemIcons.pixels(18.0F * var9);
         float var17 = Math.round(6.0F * var9);
         float var18 = this.percent ? (float)Math.ceil(percentWidth(var9)) : 0.0F;
         float var19 = Math.max((float)var16, var18);
         EquipmentSlot[] var20 = this.order();
         float var21 = f + var17;
         float var22 = g + var17;

         for (int var23 = 0; var23 < var20.length; var23++) {
            if (var23 == ARMOR.length) {
               this.drawHandsRule(imDrawList, f, g, var10, var11, var21, var22, var9, var8);
               if (this.column) {
                  var22 += 7.0F * var9;
               } else {
                  var21 += 6.0F * var9;
               }
            }

            ItemStack var24;
            ItemStack var25;
            float var26 = (var25 = NoInvLeakModule.shownInHud(var24 = minecraftClient.player.getEquippedStack(var20[var23]), var20[var23])) == var24
               ? this.wear[var23]
               : -1.0F;
            if (this.column) {
               this.drawSideways(imDrawList, var25, var26, var21, Math.round(var22), var16, var18, var14, var15, var9, var8, var13);
               var22 += var16 + 4.0F * var9;
            } else {
               this.drawStacked(imDrawList, var25, var26, Math.round(var21), var22, var16, var19, var14, var15, var9, var8, var13);
               var21 += var19 + 5.0F * var9;
            }
         }
      }
   }

   private void drawHandsRule(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, float l, float m) {
      int var10 = HudGlyphs.ink(-15408912, m * 0.45F);
      float var11 = Math.max(1.0F, (float)Math.round(l));
      if (this.column) {
         float var12 = Math.round(k + (11.0F * l - 4.0F * l) * 0.5F - 4.0F * l * 0.5F);
         HudGlyphs.register(imDrawList, f + h * 0.15F, f + h * 0.85F, var12, var11, var10);
      } else {
         float var14 = Math.round(j + (11.0F * l - 5.0F * l) * 0.5F - 5.0F * l * 0.5F);
         int var13 = var10 & 16777215;
         imDrawList.addRectFilledMultiColor(var14, g + i * 0.18F, var14 + var11, g + i * 0.5F, var13, var13, var10, var10);
         imDrawList.addRectFilledMultiColor(var14, g + i * 0.5F, var14 + var11, g + i * 0.82F, var10, var10, var13, var13);
      }
   }

   private void drawStacked(
      ImDrawList imDrawList, ItemStack itemStack, float f, float g, float h, int i, float j, ImFont imFont, int k, float l, float m, boolean bl
   ) {
      float var13 = Math.round(g + (j - i) * 0.5F);
      this.drawIcon(imDrawList, itemStack, f, var13, h, i, imFont, k, l, m);
      float var14 = h + i;
      if (this.gauge) {
         float var17;
         this.drawGauge(imDrawList, itemStack, f, g, var17 = var14 + 3.0F * l, j, l, m);
         var14 = var17 + gaugeHeight(l);
      }

      if (this.percent && f >= 0.0F) {
         String var15 = percentText(f);
         float var16 = HudChrome.tabularWidth(imFont, k, var15);
         HudChrome.tabular(
            imDrawList,
            imFont,
            k,
            Math.round(g + (j - var16) * 0.5F),
            Math.round(var14 + 2.0F * l),
            HudGlyphs.ink(HudGlyphs.vitality(f), m * this.criticalPulse(f)),
            var15,
            bl
         );
      }
   }

   private void drawSideways(
      ImDrawList imDrawList, ItemStack itemStack, float f, float g, float h, int i, float j, ImFont imFont, int k, float l, float m, boolean bl
   ) {
      this.drawIcon(imDrawList, itemStack, f, g, h, i, imFont, k, l, m);
      float var13 = g + i + 7.0F * l;
      float var14 = h + i * 0.5F;
      if (this.percent) {
         if (f >= 0.0F) {
            String var15 = percentText(f);
            float var16 = HudChrome.tabularWidth(imFont, k, var15);
            HudChrome.tabular(
               imDrawList,
               imFont,
               k,
               Math.round(var13 + j - var16),
               Math.round(var14 - k * 0.5F),
               HudGlyphs.ink(HudGlyphs.vitality(f), m * this.criticalPulse(f)),
               var15,
               bl
            );
         }

         var13 += j + 7.0F * l;
      }

      if (this.gauge) {
         this.drawGauge(imDrawList, itemStack, f, var13, Math.round(var14 - gaugeHeight(l) * 0.5F), 44.0F * l, l, m);
      }
   }

   private void drawIcon(ImDrawList imDrawList, ItemStack itemStack, float f, float g, float h, int i, ImFont imFont, int j, float k, float l) {
      if (itemStack.isEmpty()) {
         float var15 = Math.max(1.0F, (float)Math.round(2.0F * k));
         imDrawList.addRect(g + var15, h + var15, g + i - var15, h + i - var15, HudChrome.scaleAlpha(536870911, l), Math.max(1.0F, 3.0F * k), 0, 1.0F);
         imDrawList.addCircleFilled(g + i * 0.5F, h + i * 0.5F, Math.max(1.0F, 1.2F * k), HudChrome.scaleAlpha(536870911, l), 8);
      } else {
         if (f >= 0.0F && f < 0.12F) {
            float var12 = 0.5F + 0.5F * (float)Math.sin(this.clock * 6.5F);
            imDrawList.addCircleFilled(g + i * 0.5F, h + i * 0.5F, i * 0.62F, HudGlyphs.ink(-2537681, l * (0.18F + 0.22F * var12)), 20);
         }

         ItemIcons.draw(imDrawList, itemStack, g, h, (float)i, l);
         if (itemStack.hasGlint()) {
            HudGlyphs.sparkle(imDrawList, g + i - 1.5F * k, h + 1.5F * k, 2.8F * k, HudGlyphs.ink(-6621958, l * 0.9F));
         }

         int var11;
         if ((var11 = itemStack.getCount()) > 1) {
            String var14 = countText(var11);
            float var13 = HudChrome.width(imFont, j, var14);
            HudChrome.text(
               imDrawList, imFont, j, Math.round(g + i - var13 + 2.0F * k), Math.round(h + i - j + 3.0F * k), HudChrome.scaleAlpha(-1, l), var14, true
            );
         }
      }
   }

   private void drawGauge(ImDrawList imDrawList, ItemStack itemStack, float f, float g, float h, float i, float j, float k) {
      float var9 = gaugeHeight(j);
      float var10 = var9 * 0.5F;
      imDrawList.addRectFilled(g, h, g + i, h + var9, HudChrome.scaleAlpha(452984831, k), var10);
      if (!itemStack.isEmpty() && !(f < 0.0F)) {
         float var11 = i * Math.max(0.0F, Math.min(1.0F, f));
         if (!(var11 < 0.5F)) {
            imDrawList.addRectFilled(g, h, g + var11, h + var9, HudGlyphs.ink(HudGlyphs.vitality(f), k * this.criticalPulse(f)), Math.min(var10, var11 * 0.5F));
         }
      }
   }

   private float criticalPulse(float f) {
      return f >= 0.12F ? 1.0F : 0.6F + 0.4F * (0.5F + 0.5F * (float)Math.sin(this.clock * 6.5F));
   }

   private static String percentText(float f) {
      int var1 = Math.max(0, Math.min(100, Math.round(f * 100.0F)));
      String var2 = PERCENT[var1];
      if (var2 == null) {
         PERCENT[var1] = var2 = var1 + "%";
      }

      return var2;
   }

   private static String countText(int i) {
      if (i >= COUNT.length) {
         return Integer.toString(i);
      } else {
         String var1 = COUNT[i];
         if (var1 == null) {
            COUNT[i] = var1 = Integer.toString(i);
         }

         return var1;
      }
   }
}
