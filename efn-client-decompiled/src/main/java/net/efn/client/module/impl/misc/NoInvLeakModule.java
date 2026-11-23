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

   public static ItemStack hotbar(ItemStack itemStack, int i, int j, int k) {
      NoInvLeakModule var5 = active;
      if (var5 != null && itemStack != null) {
         int var4 = k == 10 ? 40 : (k >= 1 && k <= 9 ? k - 1 : 41);
         if (var5.streamOverlaying() && StreamOverlay.recordsHotbar()) {
            StreamOverlay.hotbar(var4, i, j, itemStack);
            return ItemStack.EMPTY;
         } else {
            return var5.shown(itemStack, var4);
         }
      } else {
         return itemStack;
      }
   }

   public static ItemStack container(ItemStack itemStack, Slot slot, int i, int j, boolean bl) {
      NoInvLeakModule var5 = active;
      if (var5 != null && itemStack != null) {
         int var6 = inventoryIndex(slot);
         if (var5.streamOverlaying()) {
            StreamOverlay.container(var6, i, j, itemStack, bl);
            return ItemStack.EMPTY;
         } else {
            return var5.shown(itemStack, var6);
         }
      } else {
         return itemStack;
      }
   }

   private static int inventoryIndex(Slot slot) {
      Slot var1;
      if (slot instanceof AccessorCreativeSlotWrapper var2) {
         var1 = var2.efn$target();
      } else {
         var1 = slot;
      }

      return var1.getIndex();
   }

   public static ItemStack dragged(ItemStack itemStack) {
      NoInvLeakModule var1 = active;
      if (var1 != null && itemStack != null && !itemStack.isEmpty()) {
         return var1.streamOverlaying() ? ItemStack.EMPTY : var1.fake(itemStack, 41);
      } else {
         return itemStack;
      }
   }

   public static boolean hidesModelOf(LivingEntity livingEntity) {
      return active != null && livingEntity != null && livingEntity == MinecraftClient.getInstance().player;
   }

   public static void bareModel(boolean bl) {
      bareModel = bl;
   }

   public static ItemStack modelItem(LivingEntity livingEntity, ItemStack itemStack) {
      return bareModel && itemStack != null && !itemStack.isEmpty() && livingEntity == MinecraftClient.getInstance().player ? ItemStack.EMPTY : itemStack;
   }

   public static int screenX(DrawContext drawContext, int i, int j, int k) {
      if (drawContext != null && active != null) {
         drawContext.getMatrices().transformPosition(i, j, POINT);
         return Math.round(POINT.x);
      } else {
         return k;
      }
   }

   public static int screenY(DrawContext drawContext, int i, int j, int k) {
      if (drawContext != null && active != null) {
         drawContext.getMatrices().transformPosition(i, j, POINT);
         return Math.round(POINT.y);
      } else {
         return k;
      }
   }

   public static ItemStack carried(ItemStack itemStack, int i, int j) {
      NoInvLeakModule var3 = active;
      if (var3 == null || itemStack == null || itemStack.isEmpty()) {
         return itemStack;
      } else if (var3.streamOverlaying()) {
         StreamOverlay.carried(itemStack, i, j);
         return ItemStack.EMPTY;
      } else {
         return var3.fake(itemStack, 41);
      }
   }

   public static ItemStack shownInHud(ItemStack itemStack, EquipmentSlot equipmentSlot) {
      NoInvLeakModule var2 = active;
      return var2 != null && var2.hud.get() && itemStack != null && !itemStack.isEmpty() ? var2.fake(itemStack, inventorySlot(equipmentSlot)) : itemStack;
   }

   public static ItemStack fakeFor(int i) {
      NoInvLeakModule var1 = active;
      return var1 == null ? ItemStack.EMPTY : var1.fake(ItemStack.EMPTY, i);
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

   public static boolean ownSlot(Slot slot) {
      return active != null && slot != null && slot.inventory instanceof PlayerInventory;
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

   public static void streamNotice(String string, String string2) {
      NoInvLeakModule var2 = active;
      if (var2 != null) {
         ModuleChat.send(var2, ModuleChat.body().warn(string).text(string2));
      }
   }

   public static boolean opaqueCells() {
      NoInvLeakModule var0 = active;
      return var0 != null && !var0.style.is("Empty");
   }

   private ItemStack shown(ItemStack itemStack, int i) {
      return itemStack.isEmpty() && !this.fillEmpty.get() ? itemStack : this.fake(itemStack, i);
   }

   private ItemStack fake(ItemStack itemStack, int i) {
      if (this.style.is("Empty")) {
         return ItemStack.EMPTY;
      } else if (this.style.is("Random")) {
         return this.loadout.get(i);
      } else {
         ItemStack var3 = totem;
         if (var3 == null) {
            totem = var3 = new ItemStack(Items.TOTEM_OF_UNDYING);
         }

         return var3;
      }
   }

   private static int inventorySlot(EquipmentSlot equipmentSlot) {
      return switch (equipmentSlot) {
         case HEAD -> 39;
         case CHEST -> 38;
         case LEGS -> 37;
         case FEET -> 36;
         case OFFHAND -> 40;
         case MAINHAND -> {
            ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
            yield var1 != null ? var1.getInventory().getSelectedSlot() : 41;
         }
         default -> 41;
      };
   }
}
