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
   private static final EquipmentSlot[] ARMOR = new EquipmentSlot[]{
      EquipmentSlot.field_6169, EquipmentSlot.field_6174, EquipmentSlot.field_6172, EquipmentSlot.field_6166
   };
   private static final EquipmentSlot[] ARMOR_AND_HANDS = new EquipmentSlot[]{
      EquipmentSlot.field_6169,
      EquipmentSlot.field_6174,
      EquipmentSlot.field_6172,
      EquipmentSlot.field_6166,
      EquipmentSlot.field_6173,
      EquipmentSlot.field_6171
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

   void update(MinecraftClient var1, HudModule var2, boolean var3, boolean var4, float var5) {
      this.clock += var5;
      this.column = var2.armorColumn();
      this.hands = var2.armorHands();
      HudModule.ArmorWear var7 = var2.armorWear();
      this.gauge = var7 != HudModule.ArmorWear.PERCENT;
      this.percent = var7 != HudModule.ArmorWear.GAUGE;
      ClientPlayerEntity var8 = var1.field_1724;
      boolean var9 = false;
      EquipmentSlot[] var10 = this.order();

      for (int var11 = 0; var11 < var10.length; var11++) {
         ItemStack var12 = var8 == null ? ItemStack.field_8037 : var8.method_6118(var10[var11]);
         if (!var12.method_7960()) {
            var9 = true;
         }

         float var14 = remaining(var12);
         if (var12.method_7909() != this.seen[var11]) {
            this.seen[var11] = var12.method_7909();
            this.wear[var11] = var14;
         } else {
            this.wear[var11] = var14 < 0.0F ? var14 : HudGlyphs.approach(Math.max(0.0F, this.wear[var11]), var14, 10.0F, var5);
         }
      }

      boolean var6 = var4 || var3 && var9;
      this.presence = var4 ? 1.0F : Math.max(0.0F, Math.min(1.0F, this.presence + (var6 ? var5 : -var5) / 0.25F));
   }

   private EquipmentSlot[] order() {
      return this.hands ? ARMOR_AND_HANDS : ARMOR;
   }

   private static float remaining(ItemStack var0) {
      return !var0.method_7960() && var0.method_7963() && var0.method_7936() > 0
         ? Math.max(0.0F, 1.0F - (float)var0.method_7919() / var0.method_7936())
         : -1.0F;
   }

   float planWidth(float var1) {
      if (!this.visible()) {
         return 1.0F;
      } else {
         int var3 = this.order().length;
         float var4 = ItemIcons.pixels(18.0F * var1);
         float var2 = this.percent ? (float)Math.ceil(percentWidth(var1)) : 0.0F;
         if (this.column) {
            float var7 = (this.percent ? var2 : 0.0F) + (this.gauge ? 44.0F * var1 : 0.0F) + (this.percent && this.gauge ? 7.0F * var1 : 0.0F);
            return Math.round(6.0F * var1 * 2.0F + var4 + 7.0F * var1 + var7);
         } else {
            float var6 = Math.max(var4, var2);
            return Math.round(6.0F * var1 * 2.0F + var3 * var6 + (var3 - 1) * 5.0F * var1 + (this.hands ? 11.0F * var1 - 5.0F * var1 : 0.0F));
         }
      }
   }

   float planHeight(float var1) {
      int var2 = this.order().length;
      float var3 = ItemIcons.pixels(18.0F * var1);
      if (this.column) {
         return Math.round(6.0F * var1 * 2.0F + var2 * var3 + (var2 - 1) * 4.0F * var1 + (this.hands ? 11.0F * var1 - 4.0F * var1 : 0.0F));
      } else {
         float var4 = (this.gauge ? 3.0F * var1 + gaugeHeight(var1) : 0.0F) + (this.percent ? 2.0F * var1 + HudChrome.px(14.0F, var1) : 0.0F);
         return Math.round(6.0F * var1 * 2.0F + var3 + var4);
      }
   }

   private static float percentWidth(float var0) {
      return HudChrome.tabularWidth(ImGuiManager.getInstance().hudLabelFont(), HudChrome.px(14.0F, var0), "100%");
   }

   private static float gaugeHeight(float var0) {
      return Math.max(2.0F, (float)Math.round(3.0F * var0));
   }

   void draw(ImDrawList var1, HudLayoutStore.HudElementState var2, float var3, float var4, float var5, boolean var6, MinecraftClient var7) {
      float var8 = var5 * HudGlyphs.easeOutQuint(this.presence);
      if (!(var8 <= 0.004F) && var7.field_1724 != null) {
         float var9 = HudRenderer.scaleOf(var2);
         float var10 = var2.width;
         float var11 = var2.height;
         Surface var12 = HudRenderer.hudSurface(var2);
         boolean var13 = HudChrome.needsTextGuard(var12);
         HudChrome.card(var1, var12, var3, var4, var10, var11, HudRenderer.hudRounding() * var9, var8, var6, var9);
         ImFont var14 = ImGuiManager.getInstance().hudLabelFont();
         int var15 = HudChrome.px(14.0F, var9);
         int var16 = (int)ItemIcons.pixels(18.0F * var9);
         float var17 = Math.round(6.0F * var9);
         float var18 = this.percent ? (float)Math.ceil(percentWidth(var9)) : 0.0F;
         float var19 = Math.max((float)var16, var18);
         EquipmentSlot[] var20 = this.order();
         float var21 = var3 + var17;
         float var22 = var4 + var17;

         for (int var23 = 0; var23 < var20.length; var23++) {
            if (var23 == ARMOR.length) {
               this.drawHandsRule(var1, var3, var4, var10, var11, var21, var22, var9, var8);
               if (this.column) {
                  var22 += 7.0F * var9;
               } else {
                  var21 += 6.0F * var9;
               }
            }

            ItemStack var24;
            ItemStack var25;
            float var26 = (var25 = NoInvLeakModule.shownInHud(var24 = var7.field_1724.method_6118(var20[var23]), var20[var23])) == var24
               ? this.wear[var23]
               : -1.0F;
            if (this.column) {
               this.drawSideways(var1, var25, var26, var21, Math.round(var22), var16, var18, var14, var15, var9, var8, var13);
               var22 += var16 + 4.0F * var9;
            } else {
               this.drawStacked(var1, var25, var26, Math.round(var21), var22, var16, var19, var14, var15, var9, var8, var13);
               var21 += var19 + 5.0F * var9;
            }
         }
      }
   }

   private void drawHandsRule(ImDrawList var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      int var10 = HudGlyphs.ink(-15408912, var9 * 0.45F);
      float var11 = Math.max(1.0F, (float)Math.round(var8));
      if (this.column) {
         float var12 = Math.round(var7 + (11.0F * var8 - 4.0F * var8) * 0.5F - 4.0F * var8 * 0.5F);
         HudGlyphs.register(var1, var2 + var4 * 0.15F, var2 + var4 * 0.85F, var12, var11, var10);
      } else {
         float var14 = Math.round(var6 + (11.0F * var8 - 5.0F * var8) * 0.5F - 5.0F * var8 * 0.5F);
         int var13 = var10 & 16777215;
         var1.addRectFilledMultiColor(var14, var3 + var5 * 0.18F, var14 + var11, var3 + var5 * 0.5F, var13, var13, var10, var10);
         var1.addRectFilledMultiColor(var14, var3 + var5 * 0.5F, var14 + var11, var3 + var5 * 0.82F, var10, var10, var13, var13);
      }
   }

   private void drawStacked(
      ImDrawList var1, ItemStack var2, float var3, float var4, float var5, int var6, float var7, ImFont var8, int var9, float var10, float var11, boolean var12
   ) {
      float var13 = Math.round(var4 + (var7 - var6) * 0.5F);
      this.drawIcon(var1, var2, var3, var13, var5, var6, var8, var9, var10, var11);
      float var14 = var5 + var6;
      if (this.gauge) {
         float var17;
         this.drawGauge(var1, var2, var3, var4, var17 = var14 + 3.0F * var10, var7, var10, var11);
         var14 = var17 + gaugeHeight(var10);
      }

      if (this.percent && var3 >= 0.0F) {
         String var15 = percentText(var3);
         float var16 = HudChrome.tabularWidth(var8, var9, var15);
         HudChrome.tabular(
            var1,
            var8,
            var9,
            Math.round(var4 + (var7 - var16) * 0.5F),
            Math.round(var14 + 2.0F * var10),
            HudGlyphs.ink(HudGlyphs.vitality(var3), var11 * this.criticalPulse(var3)),
            var15,
            var12
         );
      }
   }

   private void drawSideways(
      ImDrawList var1, ItemStack var2, float var3, float var4, float var5, int var6, float var7, ImFont var8, int var9, float var10, float var11, boolean var12
   ) {
      this.drawIcon(var1, var2, var3, var4, var5, var6, var8, var9, var10, var11);
      float var13 = var4 + var6 + 7.0F * var10;
      float var14 = var5 + var6 * 0.5F;
      if (this.percent) {
         if (var3 >= 0.0F) {
            String var15 = percentText(var3);
            float var16 = HudChrome.tabularWidth(var8, var9, var15);
            HudChrome.tabular(
               var1,
               var8,
               var9,
               Math.round(var13 + var7 - var16),
               Math.round(var14 - var9 * 0.5F),
               HudGlyphs.ink(HudGlyphs.vitality(var3), var11 * this.criticalPulse(var3)),
               var15,
               var12
            );
         }

         var13 += var7 + 7.0F * var10;
      }

      if (this.gauge) {
         this.drawGauge(var1, var2, var3, var13, Math.round(var14 - gaugeHeight(var10) * 0.5F), 44.0F * var10, var10, var11);
      }
   }

   private void drawIcon(ImDrawList var1, ItemStack var2, float var3, float var4, float var5, int var6, ImFont var7, int var8, float var9, float var10) {
      if (var2.method_7960()) {
         float var15 = Math.max(1.0F, (float)Math.round(2.0F * var9));
         var1.addRect(
            var4 + var15, var5 + var15, var4 + var6 - var15, var5 + var6 - var15, HudChrome.scaleAlpha(536870911, var10), Math.max(1.0F, 3.0F * var9), 0, 1.0F
         );
         var1.addCircleFilled(var4 + var6 * 0.5F, var5 + var6 * 0.5F, Math.max(1.0F, 1.2F * var9), HudChrome.scaleAlpha(536870911, var10), 8);
      } else {
         if (var3 >= 0.0F && var3 < 0.12F) {
            float var12 = 0.5F + 0.5F * (float)Math.sin(this.clock * 6.5F);
            var1.addCircleFilled(var4 + var6 * 0.5F, var5 + var6 * 0.5F, var6 * 0.62F, HudGlyphs.ink(-2537681, var10 * (0.18F + 0.22F * var12)), 20);
         }

         ItemIcons.draw(var1, var2, var4, var5, (float)var6, var10);
         if (var2.method_7958()) {
            HudGlyphs.sparkle(var1, var4 + var6 - 1.5F * var9, var5 + 1.5F * var9, 2.8F * var9, HudGlyphs.ink(-6621958, var10 * 0.9F));
         }

         int var11;
         if ((var11 = var2.method_7947()) > 1) {
            String var14 = countText(var11);
            float var13 = HudChrome.width(var7, var8, var14);
            HudChrome.text(
               var1,
               var7,
               var8,
               Math.round(var4 + var6 - var13 + 2.0F * var9),
               Math.round(var5 + var6 - var8 + 3.0F * var9),
               HudChrome.scaleAlpha(-1, var10),
               var14,
               true
            );
         }
      }
   }

   private void drawGauge(ImDrawList var1, ItemStack var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = gaugeHeight(var7);
      float var10 = var9 * 0.5F;
      var1.addRectFilled(var4, var5, var4 + var6, var5 + var9, HudChrome.scaleAlpha(452984831, var8), var10);
      if (!var2.method_7960() && !(var3 < 0.0F)) {
         float var11 = var6 * Math.max(0.0F, Math.min(1.0F, var3));
         if (!(var11 < 0.5F)) {
            var1.addRectFilled(
               var4, var5, var4 + var11, var5 + var9, HudGlyphs.ink(HudGlyphs.vitality(var3), var8 * this.criticalPulse(var3)), Math.min(var10, var11 * 0.5F)
            );
         }
      }
   }

   private float criticalPulse(float var1) {
      return var1 >= 0.12F ? 1.0F : 0.6F + 0.4F * (0.5F + 0.5F * (float)Math.sin(this.clock * 6.5F));
   }

   private static String percentText(float var0) {
      int var1 = Math.max(0, Math.min(100, Math.round(var0 * 100.0F)));
      String var2 = PERCENT[var1];
      if (var2 == null) {
         PERCENT[var1] = var2 = var1 + "%";
      }

      return var2;
   }

   private static String countText(int var0) {
      if (var0 >= COUNT.length) {
         return Integer.toString(var0);
      } else {
         String var1 = COUNT[var0];
         if (var1 == null) {
            COUNT[var0] = var1 = Integer.toString(var0);
         }

         return var1;
      }
   }
}
