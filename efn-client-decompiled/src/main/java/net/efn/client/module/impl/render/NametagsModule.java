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
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
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
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world != this.seenLevel) {
         this.seen.clear();
         this.seenLevel = var2.world;
      }

      if (var2.world != null && this.sHeldItems.get() && this.sSeenItems.get()) {
         for (AbstractClientPlayerEntity var4 : var2.world.getPlayers()) {
            if (var4 != var2.player) {
               this.remember(var4.getUuid(), var4.getMainHandStack());
               this.remember(var4.getUuid(), var4.getOffHandStack());
            }
         }
      }
   }

   private void remember(UUID uUID, ItemStack itemStack) {
      if (!itemStack.isEmpty()) {
         LinkedHashMap var3 = this.seen.computeIfAbsent(uUID, uUIDx -> new LinkedHashMap<>());
         ItemStack var4 = (ItemStack)var3.remove(itemStack.getItem());
         var3.put(itemStack.getItem(), var4 != null && ItemStack.areItemsAndComponentsEqual(var4, itemStack) ? var4 : itemStack.copy());

         while (var3.size() > 9) {
            var3.remove(var3.keySet().iterator().next());
         }

         while (this.seen.size() > 128) {
            this.seen.remove(this.seen.keySet().iterator().next());
         }
      }
   }

   private List<ItemStack> seenItems(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      LinkedHashMap var2 = this.seen.get(abstractClientPlayerEntity.getUuid());
      if (var2 != null && !var2.isEmpty()) {
         Item var3 = abstractClientPlayerEntity.getMainHandStack().getItem();
         Item var4 = abstractClientPlayerEntity.getOffHandStack().getItem();
         ArrayList var5 = new ArrayList(var2.size());

         for (ItemStack var7 : var2.values()) {
            if (var7.getItem() != var3 && var7.getItem() != var4) {
               var5.add(0, var7);
            }
         }

         return var5;
      } else {
         return List.of();
      }
   }

   @EventHandler
   public void onRender2D(Render2DEvent render2DEvent) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3.world != null && var3.player != null && !var3.options.hudHidden) {
         Camera var4 = var3.gameRenderer.getCamera();
         if (var4 != null && var4.isReady()) {
            float var5 = render2DEvent.partialTick;
            Vec3d var6 = var3.player.getCameraPosVec(var5);
            double var7 = Math.max(1.0F, this.sRange.get());
            double var9 = var7 * var7;
            int var11 = Math.max(1, Math.min(64, this.sMaxTags.getInt()));
            ArrayList var12 = new ArrayList();

            for (AbstractClientPlayerEntity var14 : var3.world.getPlayers()) {
               double var15;
               if (var14 != var3.player && !var14.isRemoved() && !var14.isDead() && (var15 = var14.getLerpedPos(var5).squaredDistanceTo(var6)) <= var9) {
                  var12.add(new NametagsModule.Nearby(var14, var15));
               }
            }

            List var21 = nearest(var12, var11);
            int var22 = var11 - var21.size();
            Object var23 = new ArrayList();
            if (this.sItems.get() && var22 > 0) {
               for (Entity var16 : var3.world.getEntities()) {
                  double var17;
                  ItemEntity var19;
                  if (var16 instanceof ItemEntity
                     && !(var19 = (ItemEntity)var16).isRemoved()
                     && !var19.getStack().isEmpty()
                     && (var17 = var19.getLerpedPos(var5).squaredDistanceTo(var6)) <= var9) {
                     var23.add(new NametagsModule.Nearby(var19, var17));
                  }
               }

               var23 = nearest((List<NametagsModule.Nearby>)var23, var22);
            }

            for (int var2 = var23.size() - 1; var2 >= 0; var2--) {
               this.renderItemTag(render2DEvent.graphics, var3, var4, (ItemEntity)((NametagsModule.Nearby)var23.get(var2)).entity(), var5);
            }

            for (int var20 = var21.size() - 1; var20 >= 0; var20--) {
               this.renderTag(render2DEvent.graphics, var3, var4, (AbstractClientPlayerEntity)((NametagsModule.Nearby)var21.get(var20)).entity(), var5);
            }
         }
      }
   }

   private static List<NametagsModule.Nearby> nearest(List<NametagsModule.Nearby> list, int i) {
      list.sort(NEAREST);
      return list.size() <= i ? list : list.subList(0, i);
   }

   private void renderTag(
      DrawContext drawContext, MinecraftClient minecraftClient, Camera camera, AbstractClientPlayerEntity abstractClientPlayerEntity, float f
   ) {
      Vec3d var7 = abstractClientPlayerEntity.getLerpedPos(f).add(0.0, abstractClientPlayerEntity.getHeight() + 0.15, 0.0);
      float[] var8 = WorldLabel.project(minecraftClient, var7, drawContext.getScaledWindowWidth(), drawContext.getScaledWindowHeight());
      if (var8 != null) {
         TextRenderer var9 = minecraftClient.textRenderer;
         String var10 = NameFilter.apply(abstractClientPlayerEntity.getGameProfile().name());
         String var11 = null;
         int var12 = -6052949;
         PlayerListEntry var6;
         if (this.sPing.get()
            && minecraftClient.getNetworkHandler() != null
            && (var6 = minecraftClient.getNetworkHandler().getPlayerListEntry(abstractClientPlayerEntity.getUuid())) != null) {
            int var13 = var6.getLatency();
            var11 = var13 + "ms";
            var12 = var13 < 60 ? -13577896 : (var13 < 130 ? -10742 : -47814);
         }

         List var50 = this.sHeldItems.get() ? heldItems(abstractClientPlayerEntity) : List.of();
         List var14 = this.sArmor.get() ? wornArmor(abstractClientPlayerEntity) : List.of();
         List var15 = this.sHeldItems.get() && this.sSeenItems.get() ? this.seenItems(abstractClientPlayerEntity) : List.of();
         String var16 = null;
         int var17 = -13577896;
         float var18 = abstractClientPlayerEntity.getHealth() + abstractClientPlayerEntity.getAbsorptionAmount();
         if (this.sHealth.get() && Float.isFinite(var18)) {
            float var19 = Math.min(1.0F, var18 / Math.max(1.0F, abstractClientPlayerEntity.getMaxHealth()));
            var16 = String.valueOf((int)Math.ceil(var18));
            var17 = var19 > 0.6F ? -13577896 : (var19 > 0.3F ? -10742 : -47814);
         }

         double var51 = var7.distanceTo(camera.getCameraPos());
         String var21 = Math.round((float)var51) + "m";
         int var22 = iconsWidth(var50.size());
         int var23 = iconsWidth(var14.size());
         int var24 = var16 != null ? var9.getWidth(var16) : 0;
         int var25 = var9.getWidth(var21);
         int var26 = var9.getWidth(var10) + (var11 != null ? 5 + var9.getWidth(var11) : 0);
         int var27 = extend(0, var22, 0);
         var27 = extend(var27, var23, 7);
         var27 = extend(var27, var24, 4);
         var27 = extend(var27, var25, 4);
         int var28 = var15.isEmpty() ? 0 : var9.getWidth("Seen") + 4 + iconsWidth(var15.size());
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
         Matrix3x2fStack var40 = drawContext.getMatrices();
         var40.pushMatrix();
         var40.translate(var8[0], var8[1]);
         var40.scale(var36, var36);
         int var41 = -var30 / 2;
         int var42 = -var31 - 1;
         drawPanel(drawContext, var41, var42, var41 + var30, var42 + var31, var37, var39);
         int var43 = var41 + 4;
         int var44 = var42 + 4;
         drawContext.drawText(var9, var10, var43, var44, -1447442, var38);
         if (var11 != null) {
            drawContext.drawText(var9, var11, var43 + var9.getWidth(var10) + 5, var44, var12, var38);
         }

         Objects.requireNonNull(var9);
         int var45 = var42 + 4 + 9 + 3;
         Objects.requireNonNull(var9);
         int var46 = var45 + 0 + 1;
         int var47 = var43;
         boolean var48 = false;
         if (!var50.isEmpty()) {
            var47 = this.drawIcons(drawContext, var9, abstractClientPlayerEntity, var50, 0, var43, var45, true, var39);
            var48 = true;
         }

         if (!var14.isEmpty()) {
            if (var48) {
               int var49 = var47 + 3;
               drawContext.fill(var49, var45 + 1, var49 + 1, var45 + 10 - 1, scaleAlpha(536870911, var39));
               var47 += 7;
            }

            var47 = this.drawIcons(drawContext, var9, abstractClientPlayerEntity, var14, var50.size(), var47, var45, false, var39);
            var48 = true;
         }

         if (var16 != null) {
            if (var48) {
               var47 += 4;
            }

            drawContext.drawText(var9, var16, var47, var46, var17, var38);
            var47 += var24;
            var48 = true;
         }

         if (var48) {
            var47 += 4;
         }

         drawContext.drawText(var9, var21, var47, var46, -6052949, var38);
         if (!var15.isEmpty()) {
            int var55 = var45 + 10 + 3;
            Objects.requireNonNull(var9);
            drawContext.drawText(var9, "Seen", var43, var55 + 0 + 1, -6052949, var38);
            this.drawIcons(
               drawContext, var9, abstractClientPlayerEntity, var15, var50.size() + var14.size(), var43 + var9.getWidth("Seen") + 4, var55, false, var39
            );
         }

         var40.popMatrix();
      }
   }

   private static List<ItemStack> heldItems(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      ItemStack var1 = abstractClientPlayerEntity.getMainHandStack();
      ItemStack var2 = abstractClientPlayerEntity.getOffHandStack();
      return var1.isEmpty() && var2.isEmpty() ? List.of() : List.of(var1, var2);
   }

   private static List<ItemStack> wornArmor(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      ArrayList var1 = new ArrayList(ARMOR_SLOTS.length);

      for (EquipmentSlot var5 : ARMOR_SLOTS) {
         ItemStack var6 = abstractClientPlayerEntity.getEquippedStack(var5);
         if (!var6.isEmpty()) {
            var1.add(var6);
         }
      }

      return var1;
   }

   private static int iconsWidth(int i) {
      return i == 0 ? 0 : i * 12 - 2;
   }

   private static int extend(int i, int j, int k) {
      if (j <= 0) {
         return i;
      } else {
         return i == 0 ? j : i + k + j;
      }
   }

   private int drawIcons(
      DrawContext drawContext,
      TextRenderer textRenderer,
      AbstractClientPlayerEntity abstractClientPlayerEntity,
      List<ItemStack> list,
      int i,
      int j,
      int k,
      boolean bl,
      float f
   ) {
      for (int var10 = 0; var10 < list.size(); var10++) {
         if (var10 > 0) {
            j += 2;
         }

         this.drawIcon(
            drawContext, textRenderer, abstractClientPlayerEntity, (ItemStack)list.get(var10), abstractClientPlayerEntity.getId() * 8 + i + var10, j, k, bl, f
         );
         j += 10;
      }

      return j;
   }

   private void drawIcon(
      DrawContext drawContext,
      TextRenderer textRenderer,
      AbstractClientPlayerEntity abstractClientPlayerEntity,
      ItemStack itemStack,
      int i,
      int j,
      int k,
      boolean bl,
      float f
   ) {
      if (itemStack.isEmpty()) {
         drawContext.fill(j + 2, k + 2, j + 10 - 2, k + 10 - 2, scaleAlpha(452984831, f));
      } else {
         Matrix3x2fStack var10 = drawContext.getMatrices();
         var10.pushMatrix();
         var10.translate(j, k);
         var10.scale(0.625F, 0.625F);
         drawContext.drawItem(abstractClientPlayerEntity, itemStack, 0, 0, i);
         if (this.sDurability.get() && itemStack.isItemBarVisible()) {
            drawContext.fill(2, 13, 15, 15, -16777216);
            drawContext.fill(2, 13, 2 + itemStack.getItemBarStep(), 14, 0xFF000000 | itemStack.getItemBarColor());
         }

         if (bl && itemStack.getCount() > 1) {
            String var11 = String.valueOf(itemStack.getCount());
            drawContext.drawText(textRenderer, var11, 17 - textRenderer.getWidth(var11), 9, -657929, true);
         }

         var10.popMatrix();
      }
   }

   private void renderItemTag(DrawContext drawContext, MinecraftClient minecraftClient, Camera camera, ItemEntity itemEntity, float f) {
      Vec3d var6 = itemEntity.getLerpedPos(f).add(0.0, 0.35, 0.0);
      float[] var7 = WorldLabel.project(minecraftClient, var6, drawContext.getScaledWindowWidth(), drawContext.getScaledWindowHeight());
      if (var7 != null) {
         ItemStack var8 = itemEntity.getStack();
         String var9 = var8.getName().getString();
         int var10 = var8.getCount();
         String var11 = var10 > 1 ? "x" + var10 : null;
         TextRenderer var12 = minecraftClient.textRenderer;
         int var13 = var12.getWidth(var9);
         int var14 = var11 != null ? var12.getWidth(var11) : 0;
         int var15 = var13 + (var11 != null ? 3 + var14 : 0);
         double var16 = var6.distanceTo(camera.getCameraPos());
         float var18 = (float)Math.min(1.0, Math.max(0.25, 6.0 / Math.max(1.0, var16)));
         Matrix3x2fStack var19 = drawContext.getMatrices();
         var19.pushMatrix();
         var19.translate(var7[0], var7[1]);
         var19.scale(var18, var18);
         int var20 = -var15 / 2;
         byte var21 = -4;
         drawContext.drawText(var12, var9, var20, var21, -1447442, true);
         if (var11 != null) {
            drawContext.drawText(var12, var11, var20 + var13 + 3, var21, -10742, true);
         }

         var19.popMatrix();
      }
   }

   private static void drawPanel(DrawContext drawContext, int i, int j, int k, int l, float f, float g) {
      drawR4Rect(drawContext, i - 1, j + 1, k + 1, l + 2, scaleAlpha(385875968, g));
      drawR4Rect(drawContext, i, j + 1, k, l + 1, scaleAlpha(771751936, g));
      drawR4Rect(drawContext, i, j, k, l, Math.round(f * 255.0F) << 24 | 1447965);
      drawR4Border(drawContext, i, j, k, l, scaleAlpha(402653183, g));
   }

   private static int scaleAlpha(int i, float f) {
      int var2 = Math.round((i >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, f)));
      return var2 << 24 | i & 16777215;
   }

   private static void drawR4Rect(DrawContext drawContext, int i, int j, int k, int l, int m) {
      if (m >>> 24 != 0) {
         drawContext.fill(i + 4, j, k - 4, j + 1, m);
         drawContext.fill(i + 2, j + 1, k - 2, j + 2, m);
         drawContext.fill(i + 1, j + 2, k - 1, j + 4, m);
         drawContext.fill(i, j + 4, k, l - 4, m);
         drawContext.fill(i + 1, l - 4, k - 1, l - 2, m);
         drawContext.fill(i + 2, l - 2, k - 2, l - 1, m);
         drawContext.fill(i + 4, l - 1, k - 4, l, m);
      }
   }

   private static void drawR4Border(DrawContext drawContext, int i, int j, int k, int l, int m) {
      if (m >>> 24 != 0) {
         drawContext.fill(i + 4, j, k - 4, j + 1, m);
         drawContext.fill(i + 4, l - 1, k - 4, l, m);
         drawContext.fill(i, j + 4, i + 1, l - 4, m);
         drawContext.fill(k - 1, j + 4, k, l - 4, m);
         drawContext.fill(i + 2, j + 1, i + 4, j + 2, m);
         drawContext.fill(i + 1, j + 2, i + 2, j + 4, m);
         drawContext.fill(k - 4, j + 1, k - 2, j + 2, m);
         drawContext.fill(k - 2, j + 2, k - 1, j + 4, m);
         drawContext.fill(i + 1, l - 4, i + 2, l - 2, m);
         drawContext.fill(i + 2, l - 2, i + 4, l - 1, m);
         drawContext.fill(k - 2, l - 4, k - 1, l - 2, m);
         drawContext.fill(k - 4, l - 2, k - 2, l - 1, m);
      }
   }

   @Environment(EnvType.CLIENT)
   private record Nearby(Entity entity, double distanceSq) {
   }
}
