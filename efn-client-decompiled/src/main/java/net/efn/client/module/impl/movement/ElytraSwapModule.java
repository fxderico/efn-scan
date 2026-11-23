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

   private void onKey(KeyInputEvent keyInputEvent) {
      if (keyInputEvent.action == 1) {
         this.onPress(keyInputEvent.key);
      }
   }

   private void onMouse(MouseInputEvent mouseInputEvent) {
      if (mouseInputEvent.action == 1) {
         this.onPress(1000 + mouseInputEvent.button);
      }
   }

   private void onPress(int i) {
      int var2 = this.key.value();
      if (ModuleKeybinds.isBindable(var2) && i == var2) {
         MinecraftClient var3 = MinecraftClient.getInstance();
         if (var3.currentScreen == null && var3.player != null) {
            this.pending = true;
            this.pendingTicks = 0;
         }
      }
   }

   private void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 == null || var2.world == null || var2.interactionManager == null) {
         this.reset();
      } else if (!this.pending) {
         if (this.armed) {
            this.followFlight(var2, var3);
         }
      } else {
         String var4 = this.busyReason(var3);
         if (var4 == null) {
            if (var2.currentScreen != null || this.swap(var2, var3)) {
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

   private String busyReason(ClientPlayerEntity clientPlayerEntity) {
      if (CombatMacroLock.heldByOther(this)) {
         return "another macro kept the inventory busy";
      } else if (AutoInvTotemModule.busy()) {
         return "the totem refill kept the inventory busy";
      } else if (clientPlayerEntity.isUsingItem()) {
         return "you kept using an item";
      } else {
         return ((AccessorLocalPlayer)clientPlayerEntity).efn$lastSentInput().sneak() ? "you kept sneaking" : null;
      }
   }

   private void followFlight(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (!this.swapBackOnLanding.get() || !clientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST).contains(DataComponentTypes.GLIDER)) {
         this.disarm();
      } else if (clientPlayerEntity.isGliding()) {
         this.glided = true;
         this.groundTicks = 0;
      } else if (this.glided) {
         if (!clientPlayerEntity.isOnGround() && !clientPlayerEntity.isTouchingWater()) {
            this.groundTicks = 0;
         } else if (this.groundTicks++ >= this.landingDelay.getInt()) {
            if (minecraftClient.currentScreen == null && inventoryReady(clientPlayerEntity) && this.busyReason(clientPlayerEntity) == null) {
               if (!isBound(clientPlayerEntity, clientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST))) {
                  int var3 = this.chestplateToEquip(clientPlayerEntity);
                  if (var3 == -1) {
                     ModuleChat.send(this, ModuleChat.body().danger("Can't swap back").text(", no ").name("chestplate").text(" left in your inventory"));
                  } else if (!AutoInvTotemModule.clickSafe(clientPlayerEntity)) {
                     if (++this.settleTicks <= 20) {
                        InputControl.idle();
                        return;
                     }

                     ModuleChat.send(this, ModuleChat.body().warn("Swap back skipped").text(", you kept moving"));
                  } else if (!equip(minecraftClient, clientPlayerEntity, var3)) {
                     ModuleChat.send(this, SWAP_FAILED);
                  }
               }

               this.disarm();
            }
         }
      }
   }

   private boolean swap(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity.isSpectator()) {
         return true;
      } else if (!inventoryReady(clientPlayerEntity)) {
         ModuleChat.send(this, ModuleChat.body().warn("Swap skipped").text(", close the open container first"));
         return true;
      } else {
         ItemStack var4 = clientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST);
         if (isBound(clientPlayerEntity, var4)) {
            ModuleChat.send(this, ModuleChat.body().danger("Can't swap").text(", your chest piece has ").name("Curse of Binding"));
            return true;
         } else if (var4.contains(DataComponentTypes.GLIDER)) {
            int var7 = this.chestplateToEquip(clientPlayerEntity);
            if (var7 == -1) {
               ModuleChat.send(this, ModuleChat.body().danger("Can't swap").text(", no ").name("chestplate").text(" in your inventory"));
            } else {
               if (!AutoInvTotemModule.clickSafe(clientPlayerEntity)) {
                  InputControl.idle();
                  return false;
               }

               if (!equip(minecraftClient, clientPlayerEntity, var7)) {
                  ModuleChat.send(this, SWAP_FAILED);
               }
            }

            this.disarm();
            return true;
         } else {
            int var5 = findElytra(clientPlayerEntity);
            if (var5 == -1) {
               ModuleChat.send(this, ModuleChat.body().danger("Can't swap").text(", no usable ").name("elytra").text(" in your inventory"));
               return true;
            } else if (!AutoInvTotemModule.clickSafe(clientPlayerEntity)) {
               InputControl.idle();
               return false;
            } else {
               Item var3 = isChestplate(var4) ? var4.getItem() : null;
               if (!equip(minecraftClient, clientPlayerEntity, var5)) {
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

   private int chestplateToEquip(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var2;
      return this.storedIndex >= 0
            && this.storedItem != null
            && (var2 = clientPlayerEntity.getInventory().getStack(this.storedIndex)).isOf(this.storedItem)
            && !isBound(clientPlayerEntity, var2)
         ? this.storedIndex
         : findBestChestplate(clientPlayerEntity);
   }

   private static boolean equip(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, int i) {
      Item var3 = clientPlayerEntity.getInventory().getStack(i).getItem();
      ((AccessorElytraFlyMultiPlayerGameMode)minecraftClient.interactionManager).efn$elytraFlySyncSelected();
      if (!PlayerInventory.isValidHotbarIndex(i) && i != 40) {
         ElytraFlyInventory.moveToChest(i);
      } else {
         minecraftClient.interactionManager.clickSlot(clientPlayerEntity.playerScreenHandler.syncId, 6, i, SlotActionType.SWAP, clientPlayerEntity);
      }

      clientPlayerEntity.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
      return clientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST).isOf(var3);
   }

   private static boolean inventoryReady(ClientPlayerEntity clientPlayerEntity) {
      return clientPlayerEntity.currentScreenHandler == clientPlayerEntity.playerScreenHandler
         && clientPlayerEntity.currentScreenHandler.getCursorStack().isEmpty();
   }

   private static boolean isBound(ClientPlayerEntity clientPlayerEntity, ItemStack itemStack) {
      return !clientPlayerEntity.isCreative() && EnchantmentHelper.hasAnyEnchantmentsWith(itemStack, EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE);
   }

   private static int findElytra(ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var1 = clientPlayerEntity.getInventory();
      int var2 = -1;
      int var3 = -1;

      for (int var7 : SOURCE_INDICES) {
         ItemStack var9 = var1.getStack(var7);
         int var8;
         if (LivingEntity.canGlideWith(var9, EquipmentSlot.CHEST) && !isBound(clientPlayerEntity, var9) && (var8 = durabilityLeft(var9)) > var3) {
            var2 = var7;
            var3 = var8;
         }
      }

      return var2;
   }

   private static int findBestChestplate(ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var1 = clientPlayerEntity.getInventory();
      int var2 = -1;
      double var3 = -1.0;
      int var5 = -1;

      for (int var9 : SOURCE_INDICES) {
         ItemStack var10 = var1.getStack(var9);
         if (isChestplate(var10) && !isBound(clientPlayerEntity, var10)) {
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

   private static boolean isChestplate(ItemStack itemStack) {
      if (!itemStack.isEmpty() && !itemStack.contains(DataComponentTypes.GLIDER)) {
         EquippableComponent var1 = itemStack.get(DataComponentTypes.EQUIPPABLE);
         return var1 != null && var1.slot() == EquipmentSlot.CHEST;
      } else {
         return false;
      }
   }

   private static double armorScore(ItemStack itemStack) {
      AttributeModifiersComponent var1 = itemStack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
      return var1.applyOperations(EntityAttributes.ARMOR, 0.0, EquipmentSlot.CHEST)
         + var1.applyOperations(EntityAttributes.ARMOR_TOUGHNESS, 0.0, EquipmentSlot.CHEST);
   }

   private static int durabilityLeft(ItemStack itemStack) {
      return itemStack.isDamageable() ? itemStack.getMaxDamage() - itemStack.getDamage() : 2147483647;
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
