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
      this.samples[0].item = Items.ENDER_PEARL;
      this.samples[0].total = 20.0F;
      this.samples[1].item = Items.WIND_CHARGE;
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

   void update(MinecraftClient minecraftClient, HudModule hudModule, boolean bl, boolean bl2, float f) {
      this.clock += f;
      this.barStyle = hudModule.cooldownBar();
      float var7 = minecraftClient.world == null ? 20.0F : minecraftClient.world.getTickManager().getTickRate();
      this.tickRate = var7 > 0.0F ? var7 : 20.0F;
      ClientPlayerEntity var8 = minecraftClient.player;
      if (var8 != this.owner) {
         this.reset();
         this.owner = var8;
      }

      for (CooldownTray.Slot var10 : this.slots) {
         var10.live = false;
      }

      if (bl && var8 != null) {
         this.collect(minecraftClient, var8);
      }

      for (int var6 = this.slots.size() - 1; var6 >= 0; var6--) {
         CooldownTray.Slot var12 = this.slots.get(var6);
         if (var12.live) {
            var12.presence = Math.min(1.0F, var12.presence + f / 0.18F);
            if (var12.item == null && this.clock >= var12.lookupAt && var8 != null) {
               var12.item = resolve(var8, var12.group);
               var12.lookupAt = this.clock + 0.5F;
            }
         } else {
            if (!var12.done) {
               var12.done = true;
               var12.remaining = 0.0F;
               float var14 = var12.ready = bl ? 1.0F : 0.0F;
            }

            if (var12.ready > 0.0F) {
               var12.ready = Math.max(0.0F, var12.ready - f / 0.32F);
            } else {
               var12.presence -= f / 0.22F;
               if (var12.presence <= 0.0F) {
                  this.slots.remove(var6);
                  var12.group = null;
                  var12.item = null;
                  this.pool.add(var12);
               }
            }
         }
      }

      boolean var13 = this.sampling = bl2 && this.slots.isEmpty();
      if (this.sampling) {
         for (int var11 = 0; var11 < this.samples.length; var11++) {
            float var15 = (this.clock * 0.45F + var11 * 0.43F) % 1.0F;
            this.samples[var11].remaining = (1.0F - var15) * this.samples[var11].total;
         }
      }
   }

   private void collect(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      ItemCooldownManager var3 = clientPlayerEntity.getItemCooldownManager();
      AccessorHudItemCooldowns var4 = (AccessorHudItemCooldowns)var3;
      int var5 = var4.efn$hudTickCount();
      float var6 = minecraftClient.getRenderTickCounter().getTickProgress(true);

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

   private CooldownTray.Slot find(Identifier identifier) {
      for (CooldownTray.Slot var3 : this.slots) {
         if (var3.group.equals(identifier)) {
            return var3;
         }
      }

      return null;
   }

   private CooldownTray.Slot open(Identifier identifier) {
      CooldownTray.Slot var2 = this.pool.isEmpty() ? new CooldownTray.Slot() : this.pool.remove(this.pool.size() - 1);
      var2.group = identifier;
      var2.item = null;
      var2.lookupAt = 0.0F;
      var2.presence = 0.0F;
      var2.ready = 0.0F;
      var2.done = false;
      this.slots.add(var2);
      return var2;
   }

   private static Item resolve(ClientPlayerEntity clientPlayerEntity, Identifier identifier) {
      PlayerInventory var2 = clientPlayerEntity.getInventory();
      ItemCooldownManager var3 = clientPlayerEntity.getItemCooldownManager();

      for (int var4 = 0; var4 < var2.size(); var4++) {
         ItemStack var5 = var2.getStack(var4);
         if (!var5.isEmpty() && identifier.equals(var3.getGroup(var5))) {
            return var5.getItem();
         }
      }

      return Registries.ITEM.containsId(identifier) ? Registries.ITEM.get(identifier) : null;
   }

   float planWidth(float f) {
      if (!this.visible()) {
         return 1.0F;
      } else {
         float var2 = 0.0F;

         for (CooldownTray.Slot var5 : this.sampling ? this.sampleView : this.slots) {
            var2 += HudGlyphs.easeOutQuint(var5.presence) * 37.0F * f;
         }

         return Math.max(1.0F, (float)Math.round(6.0F * f * 2.0F + Math.max(0.0F, var2 - 5.0F * f)));
      }
   }

   float planHeight(float f) {
      float var2 = this.barStyle ? ItemIcons.pixels(18.0F * f) + 3.0F * f + barHeight(f) : Math.round(26.0F * f);
      return Math.round(6.0F * f * 2.0F + var2 + 2.0F * f + HudChrome.px(14.0F, f));
   }

   private static float barHeight(float f) {
      return Math.max(2.0F, (float)Math.round(3.0F * f));
   }

   void draw(ImDrawList imDrawList, HudLayoutStore.HudElementState hudElementState, float f, float g, float h, boolean bl, int i) {
      if (!(h <= 0.004F) && this.visible()) {
         List var8 = this.sampling ? this.sampleView : this.slots;
         float var9 = 0.0F;

         for (CooldownTray.Slot var11 : var8) {
            var9 += var11.presence;
         }

         float var28 = h * HudGlyphs.easeOutQuint(Math.min(1.0F, var9));
         if (!(var28 <= 0.004F)) {
            float var29 = HudRenderer.scaleOf(hudElementState);
            float var12 = hudElementState.width;
            float var13 = hudElementState.height;
            Surface var14 = HudRenderer.hudSurface(hudElementState);
            boolean var15 = HudChrome.needsTextGuard(var14);
            HudChrome.card(imDrawList, var14, f, g, var12, var13, HudRenderer.hudRounding() * var29, var28, bl, var29);
            ImFont var16 = ImGuiManager.getInstance().hudLabelFont();
            int var17 = HudChrome.px(14.0F, var29);
            float var18 = Math.round(6.0F * var29);
            float var19 = 32.0F * var29;
            float var20 = this.barStyle ? ItemIcons.pixels(18.0F * var29) + 3.0F * var29 + barHeight(var29) : Math.round(26.0F * var29);
            float var21 = Math.round(g + var18 + var20 + 2.0F * var29);
            float var22 = 10.0F * var29;
            imDrawList.pushClipRect(f, g - var22, f + var12, g + var13 + var22, true);
            float var23 = f + var18;

            for (CooldownTray.Slot var25 : var8) {
               float var26 = HudGlyphs.easeOutQuint(var25.presence);
               float var27 = h * var26;
               if (var27 > 0.004F) {
                  if (this.barStyle) {
                     this.drawBar(imDrawList, var25, Math.round(var23), g + var18, var19, var29, var27, i);
                  } else {
                     this.drawHalo(imDrawList, var25, var23 + var19 * 0.5F, g + var18 + Math.round(26.0F * var29) * 0.5F, var29, var27, i);
                  }

                  this.drawCaption(imDrawList, var25, var23 + var19 * 0.5F, var21, var16, var17, var29, var27, var15);
               }

               var23 += var26 * (var19 + 5.0F * var29);
            }

            imDrawList.popClipRect();
         }
      }
   }

   private void drawHalo(ImDrawList imDrawList, CooldownTray.Slot slot, float f, float g, float h, float i, int j) {
      float var8 = Math.round(26.0F * h) * 0.5F - 2.0F * h * 0.5F;
      float var9 = Math.max(1.5F, 2.0F * h);
      float var10 = fraction(slot);
      imDrawList.addCircle(f, g, var8, HudChrome.scaleAlpha(452984831, i), 0, var9);
      if (var10 > 0.0F) {
         int var11 = HudDraw.mix(j, GuiPalette.abgr(-15408912), 1.0F - var10);
         HudGlyphs.arc(imDrawList, f, g, var8, -1.5707964F, 6.2831855F * var10, var9, HudChrome.scaleAlpha(var11, i));
      }

      int var13 = (int)ItemIcons.pixels(16.0F * h);
      drawItem(imDrawList, slot, f - var13 * 0.5F, g - var13 * 0.5F, var13, h, i * (0.62F + 0.38F * (1.0F - var10)));
      if (slot.ready > 0.0F) {
         float var12 = (1.0F - slot.ready) * 7.0F * h;
         imDrawList.addCircle(f, g, var8 + var12, HudGlyphs.ink(-15408912, i * slot.ready * 0.9F), 0, var9);
      }
   }

   private void drawBar(ImDrawList imDrawList, CooldownTray.Slot slot, float f, float g, float h, float i, float j, int k) {
      int var9 = (int)ItemIcons.pixels(18.0F * i);
      float var10 = fraction(slot);
      float var11 = Math.round(f + (h - var9) * 0.5F);
      drawItem(imDrawList, slot, var11, g, var9, i, j * (0.62F + 0.38F * (1.0F - var10)));
      float var12 = g + var9 + 3.0F * i;
      float var13 = barHeight(i);
      float var14 = Math.round(3.0F * i);
      float var15 = f + var14;
      float var16 = f + h - var14;
      imDrawList.addRectFilled(var15, var12, var16, var12 + var13, HudChrome.scaleAlpha(452984831, j), var13 * 0.5F);
      if (var10 > 0.0F) {
         int var17 = HudDraw.mix(k, GuiPalette.abgr(-15408912), 1.0F - var10);
         float var18 = (var16 - var15) * var10;
         imDrawList.addRectFilled(var15, var12, var15 + var18, var12 + var13, HudChrome.scaleAlpha(var17, j), Math.min(var13 * 0.5F, var18 * 0.5F));
      }

      if (slot.ready > 0.0F) {
         float var19 = slot.ready;
         imDrawList.addRectFilled(var15, var12, var16, var12 + var13, HudGlyphs.ink(-6621958, j * var19), var13 * 0.5F);
      }
   }

   private static void drawItem(ImDrawList imDrawList, CooldownTray.Slot slot, float f, float g, int i, float h, float j) {
      if (slot.item != null) {
         ItemIcons.draw(imDrawList, slot.item, f, g, (float)i, j);
      } else {
         HudGlyphs.hourglass(imDrawList, f + i * 0.5F, g + i * 0.5F, i * 0.8F, Math.max(1.0F, 1.3F * h), HudGlyphs.ink(-3812390, j));
      }
   }

   private void drawCaption(ImDrawList imDrawList, CooldownTray.Slot slot, float f, float g, ImFont imFont, int i, float h, float j, boolean bl) {
      if (slot.done) {
         float var12 = i * 0.85F;
         HudGlyphs.bolt(imDrawList, f, g + i * 0.5F, var12, Math.max(1.0F, 1.2F * h), HudGlyphs.ink(-15408912, j * Math.max(slot.ready, 0.4F)));
      } else {
         String var10 = seconds(slot.remaining / this.tickRate);
         float var11 = HudChrome.tabularWidth(imFont, i, var10);
         HudChrome.tabular(imDrawList, imFont, i, Math.round(f - var11 * 0.5F), g, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, j), var10, bl);
      }
   }

   private static float fraction(CooldownTray.Slot slot) {
      return slot.total <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, slot.remaining / slot.total));
   }

   private static String seconds(float f) {
      if (f < 9.95F) {
         int var4 = Math.max(1, Math.min(99, (int)Math.ceil(f * 10.0F)));
         String var6 = TENTHS[var4];
         if (var6 == null) {
            TENTHS[var4] = var6 = var4 / 10 + "." + var4 % 10 + "s";
         }

         return var6;
      } else {
         int var1 = (int)Math.ceil(f);
         if (var1 < 100) {
            String var5 = SECONDS[var1];
            if (var5 == null) {
               SECONDS[var1] = var5 = var1 + "s";
            }

            return var5;
         } else {
            int var2 = Math.min(99, (int)Math.ceil(f / 60.0F));
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
