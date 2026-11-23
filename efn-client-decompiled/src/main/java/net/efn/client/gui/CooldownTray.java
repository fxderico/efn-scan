package net.efn.client.gui;

import com.example.client.mixin.AccessorHudCooldownInstance;
import com.example.client.mixin.AccessorHudItemCooldowns;
import imgui.ImDrawList;
import imgui.ImFont;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import net.efn.client.config.Surface;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.impl.client.HudModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
final class CooldownTray {
   private static final float PAD = 6.0F;
   private static final float TILE_W = 32.0F;
   private static final float TILE_GAP = 5.0F;
   private static final float DIAL = 26.0F;
   private static final float DIAL_ICON = 16.0F;
   private static final float DIAL_STROKE = 2.0F;
   private static final float BAR_ICON = 18.0F;
   private static final float BAR_H = 3.0F;
   private static final float BAR_GAP = 3.0F;
   private static final float TEXT_GAP = 2.0F;
   private static final float APPEAR_TIME = 0.18F;
   private static final float READY_TIME = 0.32F;
   private static final float VANISH_TIME = 0.22F;
   private static final float LOOKUP_INTERVAL = 0.5F;
   private static final float TICKS_PER_SECOND = 20.0F;
   private static final float TAU = 6.2831855F;
   private static final float TOP = -1.5707964F;
   private static final String[] TENTHS = new String[100];
   private static final String[] SECONDS = new String[100];
   private static final String[] MINUTES = new String[100];
   private final List<CooldownTray.Slot> slots = new ArrayList<>();
   private final List<CooldownTray.Slot> pool = new ArrayList<>();
   private final CooldownTray.Slot[] samples = new CooldownTray.Slot[]{new CooldownTray.Slot(), new CooldownTray.Slot()};
   private final List<CooldownTray.Slot> sampleView = List.of(this.samples);
   private ClientPlayerEntity owner;
   private boolean sampling;
   private boolean barStyle;
   private float clock;
   private float tickRate = 20.0F;

   CooldownTray() {
      this.samples[0].item = Items.field_8634;
      this.samples[0].total = 20.0F;
      this.samples[1].item = Items.field_49098;
      this.samples[1].total = 10.0F;

      for (CooldownTray.Slot var4 : this.samples) {
         var4.presence = 1.0F;
      }
   }

   void reset() {
      for (CooldownTray.Slot var2 : this.slots) {
         var2.group = null;
         var2.item = null;
         this.pool.add(var2);
      }

      this.slots.clear();
      this.owner = null;
   }

   boolean visible() {
      return this.sampling || !this.slots.isEmpty();
   }

   void update(MinecraftClient var1, HudModule var2, boolean var3, boolean var4, float var5) {
      this.clock += var5;
      this.barStyle = var2.cooldownBar();
      float var7 = var1.field_1687 == null ? 20.0F : var1.field_1687.method_54719().method_54748();
      this.tickRate = var7 > 0.0F ? var7 : 20.0F;
      ClientPlayerEntity var8 = var1.field_1724;
      if (var8 != this.owner) {
         this.reset();
         this.owner = var8;
      }

      for (CooldownTray.Slot var10 : this.slots) {
         var10.live = false;
      }

      if (var3 && var8 != null) {
         this.collect(var1, var8);
      }

      for (int var6 = this.slots.size() - 1; var6 >= 0; var6--) {
         CooldownTray.Slot var12 = this.slots.get(var6);
         if (var12.live) {
            var12.presence = Math.min(1.0F, var12.presence + var5 / 0.18F);
            if (var12.item == null && this.clock >= var12.lookupAt && var8 != null) {
               var12.item = resolve(var8, var12.group);
               var12.lookupAt = this.clock + 0.5F;
            }
         } else {
            if (!var12.done) {
               var12.done = true;
               var12.remaining = 0.0F;
               float var14 = var12.ready = var3 ? 1.0F : 0.0F;
            }

            if (var12.ready > 0.0F) {
               var12.ready = Math.max(0.0F, var12.ready - var5 / 0.32F);
            } else {
               var12.presence -= var5 / 0.22F;
               if (var12.presence <= 0.0F) {
                  this.slots.remove(var6);
                  var12.group = null;
                  var12.item = null;
                  this.pool.add(var12);
               }
            }
         }
      }

      boolean var13 = this.sampling = var4 && this.slots.isEmpty();
      if (this.sampling) {
         for (int var11 = 0; var11 < this.samples.length; var11++) {
            float var15 = (this.clock * 0.45F + var11 * 0.43F) % 1.0F;
            this.samples[var11].remaining = (1.0F - var15) * this.samples[var11].total;
         }
      }
   }

   private void collect(MinecraftClient var1, ClientPlayerEntity var2) {
      ItemCooldownManager var3 = var2.method_7357();
      AccessorHudItemCooldowns var4 = (AccessorHudItemCooldowns)var3;
      int var5 = var4.efn$hudTickCount();
      float var6 = var1.method_61966().method_60637(true);

      for (Entry var8 : var4.efn$hudCooldowns().entrySet()) {
         AccessorHudCooldownInstance var9 = (AccessorHudCooldownInstance)var8.getValue();
         int var10 = var9.efn$hudStartTime();
         int var11 = var9.efn$hudEndTime();
         float var12 = var11 - (var5 + var6);
         if (!(var12 <= 0.0F)) {
            CooldownTray.Slot var13 = this.find((Identifier)var8.getKey());
            if (var13 == null) {
               var13 = this.open((Identifier)var8.getKey());
            }

            var13.live = true;
            var13.done = false;
            var13.ready = 0.0F;
            var13.remaining = var12;
            var13.total = Math.max(1.0F, (float)(var11 - var10));
         }
      }
   }

   private CooldownTray.Slot find(Identifier var1) {
      for (CooldownTray.Slot var3 : this.slots) {
         if (var3.group.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   private CooldownTray.Slot open(Identifier var1) {
      CooldownTray.Slot var2 = this.pool.isEmpty() ? new CooldownTray.Slot() : this.pool.remove(this.pool.size() - 1);
      var2.group = var1;
      var2.item = null;
      var2.lookupAt = 0.0F;
      var2.presence = 0.0F;
      var2.ready = 0.0F;
      var2.done = false;
      this.slots.add(var2);
      return var2;
   }

   private static Item resolve(ClientPlayerEntity var0, Identifier var1) {
      PlayerInventory var2 = var0.method_31548();
      ItemCooldownManager var3 = var0.method_7357();

      for (int var4 = 0; var4 < var2.method_5439(); var4++) {
         ItemStack var5 = var2.method_5438(var4);
         if (!var5.method_7960() && var1.equals(var3.method_62836(var5))) {
            return var5.method_7909();
         }
      }

      return Registries.field_41178.method_10250(var1) ? (Item)Registries.field_41178.method_63535(var1) : null;
   }

   float planWidth(float var1) {
      if (!this.visible()) {
         return 1.0F;
      } else {
         float var2 = 0.0F;

         for (CooldownTray.Slot var5 : this.sampling ? this.sampleView : this.slots) {
            var2 += HudGlyphs.easeOutQuint(var5.presence) * 37.0F * var1;
         }

         return Math.max(1.0F, (float)Math.round(6.0F * var1 * 2.0F + Math.max(0.0F, var2 - 5.0F * var1)));
      }
   }

   float planHeight(float var1) {
      float var2 = this.barStyle ? ItemIcons.pixels(18.0F * var1) + 3.0F * var1 + barHeight(var1) : Math.round(26.0F * var1);
      return Math.round(6.0F * var1 * 2.0F + var2 + 2.0F * var1 + HudChrome.px(14.0F, var1));
   }

   private static float barHeight(float var0) {
      return Math.max(2.0F, (float)Math.round(3.0F * var0));
   }

   void draw(ImDrawList var1, HudLayoutStore.HudElementState var2, float var3, float var4, float var5, boolean var6, int var7) {
      if (!(var5 <= 0.004F) && this.visible()) {
         List var8 = this.sampling ? this.sampleView : this.slots;
         float var9 = 0.0F;

         for (CooldownTray.Slot var11 : var8) {
            var9 += var11.presence;
         }

         float var28 = var5 * HudGlyphs.easeOutQuint(Math.min(1.0F, var9));
         if (!(var28 <= 0.004F)) {
            float var29 = HudRenderer.scaleOf(var2);
            float var12 = var2.width;
            float var13 = var2.height;
            Surface var14 = HudRenderer.hudSurface(var2);
            boolean var15 = HudChrome.needsTextGuard(var14);
            HudChrome.card(var1, var14, var3, var4, var12, var13, HudRenderer.hudRounding() * var29, var28, var6, var29);
            ImFont var16 = ImGuiManager.getInstance().hudLabelFont();
            int var17 = HudChrome.px(14.0F, var29);
            float var18 = Math.round(6.0F * var29);
            float var19 = 32.0F * var29;
            float var20 = this.barStyle ? ItemIcons.pixels(18.0F * var29) + 3.0F * var29 + barHeight(var29) : Math.round(26.0F * var29);
            float var21 = Math.round(var4 + var18 + var20 + 2.0F * var29);
            float var22 = 10.0F * var29;
            var1.pushClipRect(var3, var4 - var22, var3 + var12, var4 + var13 + var22, true);
            float var23 = var3 + var18;

            for (CooldownTray.Slot var25 : var8) {
               float var26 = HudGlyphs.easeOutQuint(var25.presence);
               float var27 = var5 * var26;
               if (var27 > 0.004F) {
                  if (this.barStyle) {
                     this.drawBar(var1, var25, Math.round(var23), var4 + var18, var19, var29, var27, var7);
                  } else {
                     this.drawHalo(var1, var25, var23 + var19 * 0.5F, var4 + var18 + Math.round(26.0F * var29) * 0.5F, var29, var27, var7);
                  }

                  this.drawCaption(var1, var25, var23 + var19 * 0.5F, var21, var16, var17, var29, var27, var15);
               }

               var23 += var26 * (var19 + 5.0F * var29);
            }

            var1.popClipRect();
         }
      }
   }

   private void drawHalo(ImDrawList var1, CooldownTray.Slot var2, float var3, float var4, float var5, float var6, int var7) {
      float var8 = Math.round(26.0F * var5) * 0.5F - 2.0F * var5 * 0.5F;
      float var9 = Math.max(1.5F, 2.0F * var5);
      float var10 = fraction(var2);
      var1.addCircle(var3, var4, var8, HudChrome.scaleAlpha(452984831, var6), 0, var9);
      if (var10 > 0.0F) {
         int var11 = HudDraw.mix(var7, GuiPalette.abgr(-15408912), 1.0F - var10);
         HudGlyphs.arc(var1, var3, var4, var8, -1.5707964F, 6.2831855F * var10, var9, HudChrome.scaleAlpha(var11, var6));
      }

      int var13 = (int)ItemIcons.pixels(16.0F * var5);
      drawItem(var1, var2, var3 - var13 * 0.5F, var4 - var13 * 0.5F, var13, var5, var6 * (0.62F + 0.38F * (1.0F - var10)));
      if (var2.ready > 0.0F) {
         float var12 = (1.0F - var2.ready) * 7.0F * var5;
         var1.addCircle(var3, var4, var8 + var12, HudGlyphs.ink(-15408912, var6 * var2.ready * 0.9F), 0, var9);
      }
   }

   private void drawBar(ImDrawList var1, CooldownTray.Slot var2, float var3, float var4, float var5, float var6, float var7, int var8) {
      int var9 = (int)ItemIcons.pixels(18.0F * var6);
      float var10 = fraction(var2);
      float var11 = Math.round(var3 + (var5 - var9) * 0.5F);
      drawItem(var1, var2, var11, var4, var9, var6, var7 * (0.62F + 0.38F * (1.0F - var10)));
      float var12 = var4 + var9 + 3.0F * var6;
      float var13 = barHeight(var6);
      float var14 = Math.round(3.0F * var6);
      float var15 = var3 + var14;
      float var16 = var3 + var5 - var14;
      var1.addRectFilled(var15, var12, var16, var12 + var13, HudChrome.scaleAlpha(452984831, var7), var13 * 0.5F);
      if (var10 > 0.0F) {
         int var17 = HudDraw.mix(var8, GuiPalette.abgr(-15408912), 1.0F - var10);
         float var18 = (var16 - var15) * var10;
         var1.addRectFilled(var15, var12, var15 + var18, var12 + var13, HudChrome.scaleAlpha(var17, var7), Math.min(var13 * 0.5F, var18 * 0.5F));
      }

      if (var2.ready > 0.0F) {
         float var19 = var2.ready;
         var1.addRectFilled(var15, var12, var16, var12 + var13, HudGlyphs.ink(-6621958, var7 * var19), var13 * 0.5F);
      }
   }

   private static void drawItem(ImDrawList var0, CooldownTray.Slot var1, float var2, float var3, int var4, float var5, float var6) {
      if (var1.item != null) {
         ItemIcons.draw(var0, var1.item, var2, var3, (float)var4, var6);
      } else {
         HudGlyphs.hourglass(var0, var2 + var4 * 0.5F, var3 + var4 * 0.5F, var4 * 0.8F, Math.max(1.0F, 1.3F * var5), HudGlyphs.ink(-3812390, var6));
      }
   }

   private void drawCaption(ImDrawList var1, CooldownTray.Slot var2, float var3, float var4, ImFont var5, int var6, float var7, float var8, boolean var9) {
      if (var2.done) {
         float var12 = var6 * 0.85F;
         HudGlyphs.bolt(var1, var3, var4 + var6 * 0.5F, var12, Math.max(1.0F, 1.2F * var7), HudGlyphs.ink(-15408912, var8 * Math.max(var2.ready, 0.4F)));
      } else {
         String var10 = seconds(var2.remaining / this.tickRate);
         float var11 = HudChrome.tabularWidth(var5, var6, var10);
         HudChrome.tabular(var1, var5, var6, Math.round(var3 - var11 * 0.5F), var4, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var8), var10, var9);
      }
   }

   private static float fraction(CooldownTray.Slot var0) {
      return var0.total <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, var0.remaining / var0.total));
   }

   private static String seconds(float var0) {
      if (var0 < 9.95F) {
         int var4 = Math.max(1, Math.min(99, (int)Math.ceil(var0 * 10.0F)));
         String var6 = TENTHS[var4];
         if (var6 == null) {
            TENTHS[var4] = var6 = var4 / 10 + "." + var4 % 10 + "s";
         }

         return var6;
      } else {
         int var1 = (int)Math.ceil(var0);
         if (var1 < 100) {
            String var5 = SECONDS[var1];
            if (var5 == null) {
               SECONDS[var1] = var5 = var1 + "s";
            }

            return var5;
         } else {
            int var2 = Math.min(99, (int)Math.ceil(var0 / 60.0F));
            String var3 = MINUTES[var2];
            if (var3 == null) {
               MINUTES[var2] = var3 = var2 + "m";
            }

            return var3;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Slot {
      Identifier group;
      Item item;
      float lookupAt;
      float remaining;
      float total;
      float presence;
      float ready;
      boolean live;
      boolean done;

      private Slot() {
      }
   }
}
