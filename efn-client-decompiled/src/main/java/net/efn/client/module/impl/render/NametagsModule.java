package net.efn.client.module.impl.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.WorldLabel;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.NameFilter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;

@Environment(EnvType.CLIENT)
public class NametagsModule extends ClientModule {
   private static final int PANEL_RGB = 1447965;
   private static final int BORDER = 402653183;
   private static final int SHADOW_NEAR = 771751936;
   private static final int SHADOW_FAR = 385875968;
   private static final int DIVIDER = 536870911;
   private static final int EMPTY_SLOT = 452984831;
   private static final int TEXT_NAME = -1447442;
   private static final int TEXT_LABEL = -6052949;
   private static final int TEXT_VALUE = -657929;
   private static final int STATUS_GOOD = -13577896;
   private static final int STATUS_WARN = -10742;
   private static final int STATUS_BAD = -47814;
   private static final int ITEM_COUNT_COLOR = -10742;
   private static final float TEXT_GUARD_BELOW = 0.55F;
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{
      EquipmentSlot.field_6169, EquipmentSlot.field_6174, EquipmentSlot.field_6172, EquipmentSlot.field_6166
   };
   private static final int PADDING = 4;
   private static final int ROW_GAP = 3;
   private static final int ICON_SIZE = 10;
   private static final int ICON_GAP = 2;
   private static final int TEXT_GAP = 4;
   private static final int SECTION_GAP = 3;
   private static final float ICON_SCALE = 0.625F;
   private static final int TAG_CAP = 64;
   private static final double FULL_SIZE_DISTANCE = 8.0;
   private static final int SEEN_LIMIT = 9;
   private static final String SEEN_LABEL = "Seen";
   private static final int SEEN_PLAYERS = 128;
   private static final Comparator<NametagsModule.Nearby> NEAREST = Comparator.comparingDouble(NametagsModule.Nearby::distanceSq);
   private final BooleanSetting sPing = this.addSetting("ping", new BooleanSetting("Ping", true));
   private final BooleanSetting sHeldItems = this.addSetting("held-items", new BooleanSetting("Held Items", true));
   private final BooleanSetting sSeenItems = this.addSetting("seen-items", new BooleanSetting("Seen Items", true));
   private final BooleanSetting sArmor = this.addSetting("armor", new BooleanSetting("Armor", true));
   private final BooleanSetting sDurability = this.addSetting("durability", new BooleanSetting("Durability", true));
   private final BooleanSetting sHealth = this.addSetting("health", new BooleanSetting("Health", true));
   private final BooleanSetting sHideVanilla = this.addSetting("hide-vanilla", new BooleanSetting("Hide Vanilla", true));
   private final BooleanSetting sItems = this.addSetting("items", new BooleanSetting("Dropped Items", true));
   private final NumberSetting sRange = this.addSetting("range", new NumberSetting("Range", 128.0F, 8.0F, 512.0F).step(1.0F));
   private final NumberSetting sMaxTags = this.addSetting("max-tags", new NumberSetting("Max Tags", 64.0F, 1.0F, 64.0F).step(1.0F));
   private final NumberSetting sScale = this.addSetting("scale", new NumberSetting("Scale", 1.0F, 0.5F, 2.0F).step(0.1F));
   private final NumberSetting sMinScale = this.addSetting("min-scale", new NumberSetting("Min Scale", 0.55F, 0.3F, 1.0F).step(0.05F));
   private final NumberSetting sBackgroundAlpha = this.addSetting("background-alpha", new NumberSetting("Background Alpha", 0.45F, 0.0F, 1.0F).step(0.05F));
   private final Map<UUID, LinkedHashMap<Item, ItemStack>> seen = new LinkedHashMap<>(16, 0.75F, true);
   private ClientWorld seenLevel;

   public NametagsModule() {
      super("nametags", "Nametags", "Player nametags with held items, armour, health, ping and distance.", ModuleCategory.RENDER);
      this.sDurability.visibleWhen(() -> this.sHeldItems.get() || this.sArmor.get());
      this.sSeenItems.visibleWhen(this.sHeldItems::get);
   }

   public static boolean hideVanillaNametags() {
      NametagsModule var0 = ModuleRegistry.get(NametagsModule.class);
      return var0 != null && var0.isEnabled() && var0.sHideVanilla.get();
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1687 != this.seenLevel) {
         this.seen.clear();
         this.seenLevel = var2.field_1687;
      }

      if (var2.field_1687 != null && this.sHeldItems.get() && this.sSeenItems.get()) {
         for (AbstractClientPlayerEntity var4 : var2.field_1687.method_18456()) {
            if (var4 != var2.field_1724) {
               this.remember(var4.method_5667(), var4.method_6047());
               this.remember(var4.method_5667(), var4.method_6079());
            }
         }
      }
   }

   private void remember(UUID var1, ItemStack var2) {
      if (!var2.method_7960()) {
         LinkedHashMap var3 = this.seen.computeIfAbsent(var1, var0 -> new LinkedHashMap<>());
         ItemStack var4 = (ItemStack)var3.remove(var2.method_7909());
         var3.put(var2.method_7909(), var4 != null && ItemStack.method_31577(var4, var2) ? var4 : var2.method_7972());

         while (var3.size() > 9) {
            var3.remove(var3.keySet().iterator().next());
         }

         while (this.seen.size() > 128) {
            this.seen.remove(this.seen.keySet().iterator().next());
         }
      }
   }

   private List<ItemStack> seenItems(AbstractClientPlayerEntity var1) {
      LinkedHashMap var2 = this.seen.get(var1.method_5667());
      if (var2 != null && !var2.isEmpty()) {
         Item var3 = var1.method_6047().method_7909();
         Item var4 = var1.method_6079().method_7909();
         ArrayList var5 = new ArrayList(var2.size());

         for (ItemStack var7 : var2.values()) {
            if (var7.method_7909() != var3 && var7.method_7909() != var4) {
               var5.add(0, var7);
            }
         }

         return var5;
      } else {
         return List.of();
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      if (var3.field_1687 != null && var3.field_1724 != null && !var3.field_1690.field_1842) {
         Camera var4 = var3.field_1773.method_19418();
         if (var4 != null && var4.method_19332()) {
            float var5 = var1.partialTick;
            Vec3d var6 = var3.field_1724.method_5836(var5);
            double var7 = Math.max(1.0F, this.sRange.get());
            double var9 = var7 * var7;
            int var11 = Math.max(1, Math.min(64, this.sMaxTags.getInt()));
            ArrayList var12 = new ArrayList();

            for (AbstractClientPlayerEntity var14 : var3.field_1687.method_18456()) {
               double var15;
               if (var14 != var3.field_1724 && !var14.method_31481() && !var14.method_29504() && (var15 = var14.method_30950(var5).method_1025(var6)) <= var9) {
                  var12.add(new NametagsModule.Nearby(var14, var15));
               }
            }

            List var21 = nearest(var12, var11);
            int var22 = var11 - var21.size();
            Object var23 = new ArrayList();
            if (this.sItems.get() && var22 > 0) {
               for (Entity var16 : var3.field_1687.method_18112()) {
                  double var17;
                  ItemEntity var19;
                  if (var16 instanceof ItemEntity
                     && !(var19 = (ItemEntity)var16).method_31481()
                     && !var19.method_6983().method_7960()
                     && (var17 = var19.method_30950(var5).method_1025(var6)) <= var9) {
                     var23.add(new NametagsModule.Nearby(var19, var17));
                  }
               }

               var23 = nearest((List<NametagsModule.Nearby>)var23, var22);
            }

            for (int var2 = var23.size() - 1; var2 >= 0; var2--) {
               this.renderItemTag(var1.graphics, var3, var4, (ItemEntity)((NametagsModule.Nearby)var23.get(var2)).entity(), var5);
            }

            for (int var20 = var21.size() - 1; var20 >= 0; var20--) {
               this.renderTag(var1.graphics, var3, var4, (AbstractClientPlayerEntity)((NametagsModule.Nearby)var21.get(var20)).entity(), var5);
            }
         }
      }
   }

   private static List<NametagsModule.Nearby> nearest(List<NametagsModule.Nearby> var0, int var1) {
      var0.sort(NEAREST);
      return var0.size() <= var1 ? var0 : var0.subList(0, var1);
   }

   private void renderTag(DrawContext var1, MinecraftClient var2, Camera var3, AbstractClientPlayerEntity var4, float var5) {
      Vec3d var7 = var4.method_30950(var5).method_1031(0.0, var4.method_17682() + 0.15, 0.0);
      float[] var8 = WorldLabel.project(var2, var7, var1.method_51421(), var1.method_51443());
      if (var8 != null) {
         TextRenderer var9 = var2.field_1772;
         String var10 = NameFilter.apply(var4.method_7334().name());
         String var11 = null;
         int var12 = -6052949;
         PlayerListEntry var6;
         if (this.sPing.get() && var2.method_1562() != null && (var6 = var2.method_1562().method_2871(var4.method_5667())) != null) {
            int var13 = var6.method_2959();
            var11 = var13 + "ms";
            var12 = var13 < 60 ? -13577896 : (var13 < 130 ? -10742 : -47814);
         }

         List var50 = this.sHeldItems.get() ? heldItems(var4) : List.of();
         List var14 = this.sArmor.get() ? wornArmor(var4) : List.of();
         List var15 = this.sHeldItems.get() && this.sSeenItems.get() ? this.seenItems(var4) : List.of();
         String var16 = null;
         int var17 = -13577896;
         float var18 = var4.method_6032() + var4.method_6067();
         if (this.sHealth.get() && Float.isFinite(var18)) {
            float var19 = Math.min(1.0F, var18 / Math.max(1.0F, var4.method_6063()));
            var16 = String.valueOf((int)Math.ceil(var18));
            var17 = var19 > 0.6F ? -13577896 : (var19 > 0.3F ? -10742 : -47814);
         }

         double var51 = var7.method_1022(var3.method_71156());
         String var21 = Math.round((float)var51) + "m";
         int var22 = iconsWidth(var50.size());
         int var23 = iconsWidth(var14.size());
         int var24 = var16 != null ? var9.method_1727(var16) : 0;
         int var25 = var9.method_1727(var21);
         int var26 = var9.method_1727(var10) + (var11 != null ? 5 + var9.method_1727(var11) : 0);
         int var27 = extend(0, var22, 0);
         var27 = extend(var27, var23, 7);
         var27 = extend(var27, var24, 4);
         var27 = extend(var27, var25, 4);
         int var28 = var15.isEmpty() ? 0 : var9.method_1727("Seen") + 4 + iconsWidth(var15.size());
         int var29 = Math.max(Math.max(var26, var27), var28);
         int var30 = var29 + 8;
         int var31 = 30 + (var15.isEmpty() ? 0 : 13);
         float var32 = Math.max(0.5F, Math.min(2.0F, this.sScale.get()));
         double var33 = Math.max(0.3F, Math.min(1.0F, this.sMinScale.get()));
         float var35 = (float)Math.min(1.0, Math.max(var33, 8.0 / Math.max(1.0, var51)));
         float var36 = var32 * var35;
         float var37 = Math.max(0.0F, Math.min(1.0F, this.sBackgroundAlpha.get()));
         boolean var38 = var37 < 0.55F;
         float var39 = Math.min(1.0F, var37 * 2.0F);
         Matrix3x2fStack var40 = var1.method_51448();
         var40.pushMatrix();
         var40.translate(var8[0], var8[1]);
         var40.scale(var36, var36);
         int var41 = -var30 / 2;
         int var42 = -var31 - 1;
         drawPanel(var1, var41, var42, var41 + var30, var42 + var31, var37, var39);
         int var43 = var41 + 4;
         int var44 = var42 + 4;
         var1.method_51433(var9, var10, var43, var44, -1447442, var38);
         if (var11 != null) {
            var1.method_51433(var9, var11, var43 + var9.method_1727(var10) + 5, var44, var12, var38);
         }

         Objects.requireNonNull(var9);
         int var45 = var42 + 4 + 9 + 3;
         Objects.requireNonNull(var9);
         int var46 = var45 + 0 + 1;
         int var47 = var43;
         boolean var48 = false;
         if (!var50.isEmpty()) {
            var47 = this.drawIcons(var1, var9, var4, var50, 0, var43, var45, true, var39);
            var48 = true;
         }

         if (!var14.isEmpty()) {
            if (var48) {
               int var49 = var47 + 3;
               var1.method_25294(var49, var45 + 1, var49 + 1, var45 + 10 - 1, scaleAlpha(536870911, var39));
               var47 += 7;
            }

            var47 = this.drawIcons(var1, var9, var4, var14, var50.size(), var47, var45, false, var39);
            var48 = true;
         }

         if (var16 != null) {
            if (var48) {
               var47 += 4;
            }

            var1.method_51433(var9, var16, var47, var46, var17, var38);
            var47 += var24;
            var48 = true;
         }

         if (var48) {
            var47 += 4;
         }

         var1.method_51433(var9, var21, var47, var46, -6052949, var38);
         if (!var15.isEmpty()) {
            int var55 = var45 + 10 + 3;
            Objects.requireNonNull(var9);
            var1.method_51433(var9, "Seen", var43, var55 + 0 + 1, -6052949, var38);
            this.drawIcons(var1, var9, var4, var15, var50.size() + var14.size(), var43 + var9.method_1727("Seen") + 4, var55, false, var39);
         }

         var40.popMatrix();
      }
   }

   private static List<ItemStack> heldItems(AbstractClientPlayerEntity var0) {
      ItemStack var1 = var0.method_6047();
      ItemStack var2 = var0.method_6079();
      return var1.method_7960() && var2.method_7960() ? List.of() : List.of(var1, var2);
   }

   private static List<ItemStack> wornArmor(AbstractClientPlayerEntity var0) {
      ArrayList var1 = new ArrayList(ARMOR_SLOTS.length);

      for (EquipmentSlot var5 : ARMOR_SLOTS) {
         ItemStack var6 = var0.method_6118(var5);
         if (!var6.method_7960()) {
            var1.add(var6);
         }
      }

      return var1;
   }

   private static int iconsWidth(int var0) {
      return var0 == 0 ? 0 : var0 * 12 - 2;
   }

   private static int extend(int var0, int var1, int var2) {
      if (var1 <= 0) {
         return var0;
      } else {
         return var0 == 0 ? var1 : var0 + var2 + var1;
      }
   }

   private int drawIcons(
      DrawContext var1, TextRenderer var2, AbstractClientPlayerEntity var3, List<ItemStack> var4, int var5, int var6, int var7, boolean var8, float var9
   ) {
      for (int var10 = 0; var10 < var4.size(); var10++) {
         if (var10 > 0) {
            var6 += 2;
         }

         this.drawIcon(var1, var2, var3, (ItemStack)var4.get(var10), var3.method_5628() * 8 + var5 + var10, var6, var7, var8, var9);
         var6 += 10;
      }

      return var6;
   }

   private void drawIcon(
      DrawContext var1, TextRenderer var2, AbstractClientPlayerEntity var3, ItemStack var4, int var5, int var6, int var7, boolean var8, float var9
   ) {
      if (var4.method_7960()) {
         var1.method_25294(var6 + 2, var7 + 2, var6 + 10 - 2, var7 + 10 - 2, scaleAlpha(452984831, var9));
      } else {
         Matrix3x2fStack var10 = var1.method_51448();
         var10.pushMatrix();
         var10.translate(var6, var7);
         var10.scale(0.625F, 0.625F);
         var1.method_51423(var3, var4, 0, 0, var5);
         if (this.sDurability.get() && var4.method_31578()) {
            var1.method_25294(2, 13, 15, 15, -16777216);
            var1.method_25294(2, 13, 2 + var4.method_31579(), 14, 0xFF000000 | var4.method_31580());
         }

         if (var8 && var4.method_7947() > 1) {
            String var11 = String.valueOf(var4.method_7947());
            var1.method_51433(var2, var11, 17 - var2.method_1727(var11), 9, -657929, true);
         }

         var10.popMatrix();
      }
   }

   private void renderItemTag(DrawContext var1, MinecraftClient var2, Camera var3, ItemEntity var4, float var5) {
      Vec3d var6 = var4.method_30950(var5).method_1031(0.0, 0.35, 0.0);
      float[] var7 = WorldLabel.project(var2, var6, var1.method_51421(), var1.method_51443());
      if (var7 != null) {
         ItemStack var8 = var4.method_6983();
         String var9 = var8.method_7964().getString();
         int var10 = var8.method_7947();
         String var11 = var10 > 1 ? "x" + var10 : null;
         TextRenderer var12 = var2.field_1772;
         int var13 = var12.method_1727(var9);
         int var14 = var11 != null ? var12.method_1727(var11) : 0;
         int var15 = var13 + (var11 != null ? 3 + var14 : 0);
         double var16 = var6.method_1022(var3.method_71156());
         float var18 = (float)Math.min(1.0, Math.max(0.25, 6.0 / Math.max(1.0, var16)));
         Matrix3x2fStack var19 = var1.method_51448();
         var19.pushMatrix();
         var19.translate(var7[0], var7[1]);
         var19.scale(var18, var18);
         int var20 = -var15 / 2;
         byte var21 = -4;
         var1.method_51433(var12, var9, var20, var21, -1447442, true);
         if (var11 != null) {
            var1.method_51433(var12, var11, var20 + var13 + 3, var21, -10742, true);
         }

         var19.popMatrix();
      }
   }

   private static void drawPanel(DrawContext var0, int var1, int var2, int var3, int var4, float var5, float var6) {
      drawR4Rect(var0, var1 - 1, var2 + 1, var3 + 1, var4 + 2, scaleAlpha(385875968, var6));
      drawR4Rect(var0, var1, var2 + 1, var3, var4 + 1, scaleAlpha(771751936, var6));
      drawR4Rect(var0, var1, var2, var3, var4, Math.round(var5 * 255.0F) << 24 | 1447965);
      drawR4Border(var0, var1, var2, var3, var4, scaleAlpha(402653183, var6));
   }

   private static int scaleAlpha(int var0, float var1) {
      int var2 = Math.round((var0 >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, var1)));
      return var2 << 24 | var0 & 16777215;
   }

   private static void drawR4Rect(DrawContext var0, int var1, int var2, int var3, int var4, int var5) {
      if (var5 >>> 24 != 0) {
         var0.method_25294(var1 + 4, var2, var3 - 4, var2 + 1, var5);
         var0.method_25294(var1 + 2, var2 + 1, var3 - 2, var2 + 2, var5);
         var0.method_25294(var1 + 1, var2 + 2, var3 - 1, var2 + 4, var5);
         var0.method_25294(var1, var2 + 4, var3, var4 - 4, var5);
         var0.method_25294(var1 + 1, var4 - 4, var3 - 1, var4 - 2, var5);
         var0.method_25294(var1 + 2, var4 - 2, var3 - 2, var4 - 1, var5);
         var0.method_25294(var1 + 4, var4 - 1, var3 - 4, var4, var5);
      }
   }

   private static void drawR4Border(DrawContext var0, int var1, int var2, int var3, int var4, int var5) {
      if (var5 >>> 24 != 0) {
         var0.method_25294(var1 + 4, var2, var3 - 4, var2 + 1, var5);
         var0.method_25294(var1 + 4, var4 - 1, var3 - 4, var4, var5);
         var0.method_25294(var1, var2 + 4, var1 + 1, var4 - 4, var5);
         var0.method_25294(var3 - 1, var2 + 4, var3, var4 - 4, var5);
         var0.method_25294(var1 + 2, var2 + 1, var1 + 4, var2 + 2, var5);
         var0.method_25294(var1 + 1, var2 + 2, var1 + 2, var2 + 4, var5);
         var0.method_25294(var3 - 4, var2 + 1, var3 - 2, var2 + 2, var5);
         var0.method_25294(var3 - 2, var2 + 2, var3 - 1, var2 + 4, var5);
         var0.method_25294(var1 + 1, var4 - 4, var1 + 2, var4 - 2, var5);
         var0.method_25294(var1 + 2, var4 - 2, var1 + 4, var4 - 1, var5);
         var0.method_25294(var3 - 2, var4 - 4, var3 - 1, var4 - 2, var5);
         var0.method_25294(var3 - 4, var4 - 2, var3 - 2, var4 - 1, var5);
      }
   }

   @Environment(EnvType.CLIENT)
   private record Nearby(Entity entity, double distanceSq) {
   }
}
