package net.efn.client.module.impl.movement;

import com.example.client.mixin.AccessorElytraFlyMultiPlayerGameMode;
import com.example.client.mixin.AccessorLocalPlayer;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.AutoInvTotemModule;
import net.efn.client.module.impl.movement.elytrafly.ElytraFlyInventory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.InputControl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class ElytraSwapModule extends ClientModule {
   private static final int[] SOURCE_INDICES = sourceIndices();
   private static final int MAX_WAIT_TICKS = 20;
   private static final int CHEST_SLOT_ID = 6;
   private static final Text SWAP_FAILED = ModuleChat.body().danger("Swap failed").text(", the item didn't move into the chest slot").build();
   private final KeySetting key = this.addSetting("key", new KeySetting("Key", -1));
   private final BooleanSetting swapBackOnLanding = this.addSetting("swap-back-on-landing", new BooleanSetting("Swap Back on Landing", true));
   private final NumberSetting landingDelay = this.addSetting("landing-delay", new NumberSetting("Landing Delay", 4.0F, 0.0F, 20.0F).step(1.0F));
   private boolean pending;
   private int pendingTicks;
   private boolean armed;
   private boolean glided;
   private int groundTicks;
   private int settleTicks;
   private int storedIndex = -1;
   private Item storedItem;

   public ElytraSwapModule() {
      super("elytra-swap", "Elytra Swap", "Swaps between your chestplate and elytra with one key.", ModuleCategory.MOVEMENT);
      this.usesNotifications();
      this.landingDelay.visibleWhen(this.swapBackOnLanding::get);
   }

   @Override
   protected void onEnable() {
      this.reset();
      this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
      this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
      this.on(TickEvent.TYPE, Stage.PRE, EventPriority.LOWEST, this::onTick);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.pending = false;
      this.pendingTicks = 0;
      this.disarm();
   }

   private void disarm() {
      this.armed = false;
      this.glided = false;
      this.groundTicks = 0;
      this.settleTicks = 0;
      this.storedIndex = -1;
      this.storedItem = null;
   }

   private void onKey(KeyInputEvent var1) {
      if (var1.action == 1) {
         this.onPress(var1.key);
      }
   }

   private void onMouse(MouseInputEvent var1) {
      if (var1.action == 1) {
         this.onPress(1000 + var1.button);
      }
   }

   private void onPress(int var1) {
      int var2 = this.key.value();
      if (ModuleKeybinds.isBindable(var2) && var1 == var2) {
         MinecraftClient var3 = MinecraftClient.method_1551();
         if (var3.field_1755 == null && var3.field_1724 != null) {
            this.pending = true;
            this.pendingTicks = 0;
         }
      }
   }

   private void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var3 == null || var2.field_1687 == null || var2.field_1761 == null) {
         this.reset();
      } else if (!this.pending) {
         if (this.armed) {
            this.followFlight(var2, var3);
         }
      } else {
         String var4 = this.busyReason(var3);
         if (var4 == null) {
            if (var2.field_1755 != null || this.swap(var2, var3)) {
               this.pending = false;
               return;
            }

            var4 = "you kept moving";
         }

         if (++this.pendingTicks > 20) {
            this.pending = false;
            ModuleChat.send(this, ModuleChat.body().warn("Swap skipped").text(", " + var4));
         }
      }
   }

   private String busyReason(ClientPlayerEntity var1) {
      if (CombatMacroLock.heldByOther(this)) {
         return "another macro kept the inventory busy";
      } else if (AutoInvTotemModule.busy()) {
         return "the totem refill kept the inventory busy";
      } else if (var1.method_6115()) {
         return "you kept using an item";
      } else {
         return ((AccessorLocalPlayer)var1).efn$lastSentInput().comp_3164() ? "you kept sneaking" : null;
      }
   }

   private void followFlight(MinecraftClient var1, ClientPlayerEntity var2) {
      if (!this.swapBackOnLanding.get() || !var2.method_6118(EquipmentSlot.field_6174).method_57826(DataComponentTypes.field_54197)) {
         this.disarm();
      } else if (var2.method_6128()) {
         this.glided = true;
         this.groundTicks = 0;
      } else if (this.glided) {
         if (!var2.method_24828() && !var2.method_5799()) {
            this.groundTicks = 0;
         } else if (this.groundTicks++ >= this.landingDelay.getInt()) {
            if (var1.field_1755 == null && inventoryReady(var2) && this.busyReason(var2) == null) {
               if (!isBound(var2, var2.method_6118(EquipmentSlot.field_6174))) {
                  int var3 = this.chestplateToEquip(var2);
                  if (var3 == -1) {
                     ModuleChat.send(this, ModuleChat.body().danger("Can't swap back").text(", no ").name("chestplate").text(" left in your inventory"));
                  } else if (!AutoInvTotemModule.clickSafe(var2)) {
                     if (++this.settleTicks <= 20) {
                        InputControl.idle();
                        return;
                     }

                     ModuleChat.send(this, ModuleChat.body().warn("Swap back skipped").text(", you kept moving"));
                  } else if (!equip(var1, var2, var3)) {
                     ModuleChat.send(this, SWAP_FAILED);
                  }
               }

               this.disarm();
            }
         }
      }
   }

   private boolean swap(MinecraftClient var1, ClientPlayerEntity var2) {
      if (var2.method_7325()) {
         return true;
      } else if (!inventoryReady(var2)) {
         ModuleChat.send(this, ModuleChat.body().warn("Swap skipped").text(", close the open container first"));
         return true;
      } else {
         ItemStack var4 = var2.method_6118(EquipmentSlot.field_6174);
         if (isBound(var2, var4)) {
            ModuleChat.send(this, ModuleChat.body().danger("Can't swap").text(", your chest piece has ").name("Curse of Binding"));
            return true;
         } else if (var4.method_57826(DataComponentTypes.field_54197)) {
            int var7 = this.chestplateToEquip(var2);
            if (var7 == -1) {
               ModuleChat.send(this, ModuleChat.body().danger("Can't swap").text(", no ").name("chestplate").text(" in your inventory"));
            } else {
               if (!AutoInvTotemModule.clickSafe(var2)) {
                  InputControl.idle();
                  return false;
               }

               if (!equip(var1, var2, var7)) {
                  ModuleChat.send(this, SWAP_FAILED);
               }
            }

            this.disarm();
            return true;
         } else {
            int var5 = findElytra(var2);
            if (var5 == -1) {
               ModuleChat.send(this, ModuleChat.body().danger("Can't swap").text(", no usable ").name("elytra").text(" in your inventory"));
               return true;
            } else if (!AutoInvTotemModule.clickSafe(var2)) {
               InputControl.idle();
               return false;
            } else {
               Item var3 = isChestplate(var4) ? var4.method_7909() : null;
               if (!equip(var1, var2, var5)) {
                  ModuleChat.send(this, SWAP_FAILED);
                  return true;
               } else {
                  this.disarm();
                  if (var3 != null) {
                     this.armed = this.swapBackOnLanding.get();
                     this.storedIndex = var5;
                     this.storedItem = var3;
                  }

                  return true;
               }
            }
         }
      }
   }

   private int chestplateToEquip(ClientPlayerEntity var1) {
      ItemStack var2;
      return this.storedIndex >= 0
            && this.storedItem != null
            && (var2 = var1.method_31548().method_5438(this.storedIndex)).method_31574(this.storedItem)
            && !isBound(var1, var2)
         ? this.storedIndex
         : findBestChestplate(var1);
   }

   private static boolean equip(MinecraftClient var0, ClientPlayerEntity var1, int var2) {
      Item var3 = var1.method_31548().method_5438(var2).method_7909();
      ((AccessorElytraFlyMultiPlayerGameMode)var0.field_1761).efn$elytraFlySyncSelected();
      if (!PlayerInventory.method_7380(var2) && var2 != 40) {
         ElytraFlyInventory.moveToChest(var2);
      } else {
         var0.field_1761.method_2906(var1.field_7498.field_7763, 6, var2, SlotActionType.field_7791, var1);
      }

      var1.field_3944.method_52787(new CloseHandledScreenC2SPacket(0));
      return var1.method_6118(EquipmentSlot.field_6174).method_31574(var3);
   }

   private static boolean inventoryReady(ClientPlayerEntity var0) {
      return var0.field_7512 == var0.field_7498 && var0.field_7512.method_34255().method_7960();
   }

   private static boolean isBound(ClientPlayerEntity var0, ItemStack var1) {
      return !var0.method_68878() && EnchantmentHelper.method_60142(var1, EnchantmentEffectComponentTypes.field_51656);
   }

   private static int findElytra(ClientPlayerEntity var0) {
      PlayerInventory var1 = var0.method_31548();
      int var2 = -1;
      int var3 = -1;

      for (int var7 : SOURCE_INDICES) {
         ItemStack var9 = var1.method_5438(var7);
         int var8;
         if (LivingEntity.method_63624(var9, EquipmentSlot.field_6174) && !isBound(var0, var9) && (var8 = durabilityLeft(var9)) > var3) {
            var2 = var7;
            var3 = var8;
         }
      }

      return var2;
   }

   private static int findBestChestplate(ClientPlayerEntity var0) {
      PlayerInventory var1 = var0.method_31548();
      int var2 = -1;
      double var3 = -1.0;
      int var5 = -1;

      for (int var9 : SOURCE_INDICES) {
         ItemStack var10 = var1.method_5438(var9);
         if (isChestplate(var10) && !isBound(var0, var10)) {
            double var11 = armorScore(var10);
            int var13 = durabilityLeft(var10);
            if (var11 > var3 || var11 == var3 && var13 > var5) {
               var2 = var9;
               var3 = var11;
               var5 = var13;
            }
         }
      }

      return var2;
   }

   private static boolean isChestplate(ItemStack var0) {
      if (!var0.method_7960() && !var0.method_57826(DataComponentTypes.field_54197)) {
         EquippableComponent var1 = (EquippableComponent)var0.method_58694(DataComponentTypes.field_54196);
         return var1 != null && var1.comp_3174() == EquipmentSlot.field_6174;
      } else {
         return false;
      }
   }

   private static double armorScore(ItemStack var0) {
      AttributeModifiersComponent var1 = (AttributeModifiersComponent)var0.method_58695(DataComponentTypes.field_49636, AttributeModifiersComponent.field_49326);
      return var1.method_57481(EntityAttributes.field_23724, 0.0, EquipmentSlot.field_6174)
         + var1.method_57481(EntityAttributes.field_23725, 0.0, EquipmentSlot.field_6174);
   }

   private static int durabilityLeft(ItemStack var0) {
      return var0.method_7963() ? var0.method_7936() - var0.method_7919() : 2147483647;
   }

   private static int[] sourceIndices() {
      int[] var0 = new int[37];
      int var1 = 0;

      while (var1 < 36) {
         var0[var1] = var1++;
      }

      var0[36] = 40;
      return var0;
   }
}
