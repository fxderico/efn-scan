package net.efn.client.module.impl.misc;

import com.example.client.mixin.AccessorCreativeSlotWrapper;
import com.sun.jna.Platform;
import net.efn.client.chat.ModuleChat;
import net.efn.client.config.ModuleConfigStore;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.noinvleak.FakeLoadout;
import net.efn.client.module.impl.misc.noinvleak.StreamOverlay;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import org.joml.Vector2f;

@Environment(EnvType.CLIENT)
public final class NoInvLeakModule extends ClientModule {
   private static final String STYLE_TOTEM = "Totem";
   private static final String STYLE_RANDOM = "Random";
   private static final String STYLE_EMPTY = "Empty";
   private static final int OFFHAND_SEED = 10;
   private static volatile NoInvLeakModule active;
   private static ItemStack totem;
   private static boolean bareModel;
   private static final Vector2f POINT = new Vector2f();
   private final ModeSetting style = this.addSetting("style", new ModeSetting("Style", "Totem", "Totem", "Random", "Empty"));
   private final BooleanSetting fillEmpty = this.addSetting("fill-empty", new BooleanSetting("Fill Empty Slots", true));
   private final BooleanSetting streamOnly = this.addSetting("stream-only", new BooleanSetting("Stream Only", false));
   private final BooleanSetting borderless = this.addSetting("borderless-fullscreen", new BooleanSetting("Borderless Fullscreen", true));
   private final BooleanSetting tooltips = this.addSetting("hide-tooltips", new BooleanSetting("Hide Tooltips", true));
   private final BooleanSetting itemName = this.addSetting("hide-item-name", new BooleanSetting("Hide Item Name", true));
   private final BooleanSetting selectedSlot = this.addSetting("hide-selected-slot", new BooleanSetting("Hide Selected Slot", true));
   private final BooleanSetting hud = this.addSetting("efn-hud", new BooleanSetting("EFN HUD", true));
   private final FakeLoadout loadout = new FakeLoadout();

   public NoInvLeakModule() {
      super("no-inv-leak", "No Inv Leak", "Hides your items on screen for screen shares. Stream Only keeps them visible to you alone.", ModuleCategory.MISC);
      this.fillEmpty.visibleWhen(() -> !this.style.is("Empty"));
      this.streamOnly.visibleWhen(Platform::isWindows);
      this.streamOnly.onChange(() -> {
         if (this.streamOnly.get()) {
            StreamOverlay.retry();
         } else {
            StreamOverlay.stop();
         }
      });
      this.borderless.visibleWhen(() -> Platform.isWindows() && this.streamOnly.get());
   }

   public static boolean wantsBorderless() {
      NoInvLeakModule var0 = ModuleRegistry.get(NoInvLeakModule.class);
      return var0 != null && var0.streamOnly.get() && var0.borderless.get() ? var0.isEnabled() || ModuleConfigStore.getInstance().enablesOnLoad(var0) : false;
   }

   @Override
   protected void onEnable() {
      this.loadout.reseed();
      StreamOverlay.retry();
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      StreamOverlay.stop();
   }

   public static ItemStack hotbar(ItemStack var0, int var1, int var2, int var3) {
      NoInvLeakModule var5 = active;
      if (var5 != null && var0 != null) {
         int var4 = var3 == 10 ? 40 : (var3 >= 1 && var3 <= 9 ? var3 - 1 : 41);
         if (var5.streamOverlaying() && StreamOverlay.recordsHotbar()) {
            StreamOverlay.hotbar(var4, var1, var2, var0);
            return ItemStack.field_8037;
         } else {
            return var5.shown(var0, var4);
         }
      } else {
         return var0;
      }
   }

   public static ItemStack container(ItemStack var0, Slot var1, int var2, int var3, boolean var4) {
      NoInvLeakModule var5 = active;
      if (var5 != null && var0 != null) {
         int var6 = inventoryIndex(var1);
         if (var5.streamOverlaying()) {
            StreamOverlay.container(var6, var2, var3, var0, var4);
            return ItemStack.field_8037;
         } else {
            return var5.shown(var0, var6);
         }
      } else {
         return var0;
      }
   }

   private static int inventoryIndex(Slot var0) {
      Slot var1;
      if (var0 instanceof AccessorCreativeSlotWrapper var2) {
         var1 = var2.efn$target();
      } else {
         var1 = var0;
      }

      return var1.method_34266();
   }

   public static ItemStack dragged(ItemStack var0) {
      NoInvLeakModule var1 = active;
      if (var1 != null && var0 != null && !var0.method_7960()) {
         return var1.streamOverlaying() ? ItemStack.field_8037 : var1.fake(var0, 41);
      } else {
         return var0;
      }
   }

   public static boolean hidesModelOf(LivingEntity var0) {
      return active != null && var0 != null && var0 == MinecraftClient.method_1551().field_1724;
   }

   public static void bareModel(boolean var0) {
      bareModel = var0;
   }

   public static ItemStack modelItem(LivingEntity var0, ItemStack var1) {
      return bareModel && var1 != null && !var1.method_7960() && var0 == MinecraftClient.method_1551().field_1724 ? ItemStack.field_8037 : var1;
   }

   public static int screenX(DrawContext var0, int var1, int var2, int var3) {
      if (var0 != null && active != null) {
         var0.method_51448().transformPosition(var1, var2, POINT);
         return Math.round(POINT.x);
      } else {
         return var3;
      }
   }

   public static int screenY(DrawContext var0, int var1, int var2, int var3) {
      if (var0 != null && active != null) {
         var0.method_51448().transformPosition(var1, var2, POINT);
         return Math.round(POINT.y);
      } else {
         return var3;
      }
   }

   public static ItemStack carried(ItemStack var0, int var1, int var2) {
      NoInvLeakModule var3 = active;
      if (var3 == null || var0 == null || var0.method_7960()) {
         return var0;
      } else if (var3.streamOverlaying()) {
         StreamOverlay.carried(var0, var1, var2);
         return ItemStack.field_8037;
      } else {
         return var3.fake(var0, 41);
      }
   }

   public static ItemStack shownInHud(ItemStack var0, EquipmentSlot var1) {
      NoInvLeakModule var2 = active;
      return var2 != null && var2.hud.get() && var0 != null && !var0.method_7960() ? var2.fake(var0, inventorySlot(var1)) : var0;
   }

   public static ItemStack fakeFor(int var0) {
      NoInvLeakModule var1 = active;
      return var1 == null ? ItemStack.field_8037 : var1.fake(ItemStack.field_8037, var0);
   }

   public static boolean fillsEmpty() {
      NoInvLeakModule var0 = active;
      return var0 != null && var0.fillEmpty.get() && !var0.style.is("Empty");
   }

   public static boolean overlaying() {
      NoInvLeakModule var0 = active;
      return var0 != null && var0.streamOnly.get() && StreamOverlay.usable();
   }

   private boolean streamOverlaying() {
      return this.streamOnly.get() && StreamOverlay.usable();
   }

   public static boolean ownSlot(Slot var0) {
      return active != null && var0 != null && var0.field_7871 instanceof PlayerInventory;
   }

   public static boolean hidesTooltips() {
      NoInvLeakModule var0 = active;
      return var0 != null && var0.tooltips.get();
   }

   public static boolean hidesItemName() {
      NoInvLeakModule var0 = active;
      return var0 != null && var0.itemName.get();
   }

   public static boolean hidesSelectedSlot() {
      NoInvLeakModule var0 = active;
      return var0 != null && var0.selectedSlot.get();
   }

   public static boolean overlayWanted() {
      NoInvLeakModule var0 = active;
      return var0 != null && var0.streamOnly.get();
   }

   public static void streamNotice(String var0, String var1) {
      NoInvLeakModule var2 = active;
      if (var2 != null) {
         ModuleChat.send(var2, ModuleChat.body().warn(var0).text(var1));
      }
   }

   public static boolean opaqueCells() {
      NoInvLeakModule var0 = active;
      return var0 != null && !var0.style.is("Empty");
   }

   private ItemStack shown(ItemStack var1, int var2) {
      return var1.method_7960() && !this.fillEmpty.get() ? var1 : this.fake(var1, var2);
   }

   private ItemStack fake(ItemStack var1, int var2) {
      if (this.style.is("Empty")) {
         return ItemStack.field_8037;
      } else if (this.style.is("Random")) {
         return this.loadout.get(var2);
      } else {
         ItemStack var3 = totem;
         if (var3 == null) {
            totem = var3 = new ItemStack(Items.field_8288);
         }

         return var3;
      }
   }

   private static int inventorySlot(EquipmentSlot var0) {
      return switch (var0) {
         case field_6169 -> 39;
         case field_6174 -> 38;
         case field_6172 -> 37;
         case field_6166 -> 36;
         case field_6171 -> 40;
         case field_6173 -> {
            ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
            yield var1 != null ? var1.method_31548().method_67532() : 41;
         }
         default -> 41;
      };
   }
}
