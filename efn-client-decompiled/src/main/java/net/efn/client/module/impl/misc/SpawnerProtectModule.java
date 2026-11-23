package net.efn.client.module.impl.misc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.misc.spawnerprotect.SpawnerAlerts;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class SpawnerProtectModule extends ClientModule {
   private static final int SHOP_END_CATEGORY_SLOT = 11;
   private static final int SHOP_ENDER_CHEST_SLOT = 9;
   private static final int SHOP_CONFIRM_SLOT = 25;
   private final NumberSetting targetStackCount = this.addSetting("stack-to-deposit", new NumberSetting("Stack To Deposit", 35.0F, 1.0F, 35.0F).step(1.0F));
   private final NumberSetting scanRange = this.addSetting("trigger-range", new NumberSetting("Trigger Range", 64.0F, 16.0F, 128.0F).step(0.1F));
   private final NumberSetting rotationSpeed = this.addSetting("rotation-speed", new NumberSetting("Rotation Speed", 15.0F, 1.0F, 30.0F).step(0.1F));
   private final BooleanSetting detectBlockUpdates = this.addSetting("detect-block-updates", new BooleanSetting("Detect Block Updates", true));
   private final StringSetting whitelist = this.addSetting("whitelist", new StringSetting("Whitelist", ""));
   private final NumberSetting breakRange = this.addSetting("break-range", new NumberSetting("Break Range", 5.5F, 1.0F, 8.0F).step(0.1F));
   private final NumberSetting blockUpdateThreshold = this.addSetting(
      "block-update-threshold", new NumberSetting("Block Update Threshold", 5.0F, 1.0F, 20.0F).step(1.0F)
   );
   private final NumberSetting blockUpdateWindow = this.addSetting(
      "block-update-window", new NumberSetting("Block Update Window", 60.0F, 1.0F, 600.0F).step(1.0F)
   );
   private final StringSetting webhookUrl = this.addSetting("webhook-url", new StringSetting("Webhook URL", ""));
   private SpawnerProtectModule.State currentState = SpawnerProtectModule.State.WAITING_FOR_STRANGER;
   private BlockPos targetBlock;
   private BlockPos targetChest;
   private int chestTick;
   private int breakCooldown;
   private int lagWaitTicks;
   private boolean strangerDetected;
   private final Set<UUID> nearbyPlayers = new HashSet<>();
   private int blockBreakCounter;
   private long lastBreakTimestamp;
   private int shopSequence;
   private int shopTick;
   private int miningTicks;

   public SpawnerProtectModule() {
      super("spawner-protect", "Spawner Protect", "Protects spawners on block updates; player arrivals only send webhook notifications.", ModuleCategory.DONUT);
      this.whitelist.privateValue();
      this.webhookUrl.privateValue();
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   public int settingsVersion() {
      return 1;
   }

   @Override
   public void migrateSettings(int i) {
      if (i < 1 && this.targetStackCount.get() == 3.0F) {
         this.targetStackCount.resetToDefault();
      }
   }

   @Override
   protected void onEnable() {
      this.on(TickEvent.TYPE, this::onTick);
      this.on(BlockUpdateEvent.TYPE, this::onBlockUpdate);
      this.resetModule();
   }

   @Override
   protected void onDisable() {
      this.stopMovement();
      this.updateSneak(false);
   }

   private void resetModule() {
      this.currentState = SpawnerProtectModule.State.WAITING_FOR_STRANGER;
      this.strangerDetected = false;
      this.nearbyPlayers.clear();
      this.lagWaitTicks = 0;
      this.blockBreakCounter = 0;
      this.lastBreakTimestamp = 0L;
      this.stopMovement();
      this.updateSneak(false);
      this.shopSequence = 0;
      this.shopTick = 0;
      this.resetState();
   }

   private void resetState() {
      this.targetChest = null;
      this.targetBlock = null;
      this.chestTick = 0;
      this.breakCooldown = 0;
      this.miningTicks = 0;
   }

   private void onBlockUpdate(BlockUpdateEvent blockUpdateEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (!this.strangerDetected && var2.player != null && var2.world != null && !this.isNearSpawn() && this.detectBlockUpdates.get()) {
         if (!blockUpdateEvent.oldState.isAir() && !(blockUpdateEvent.oldState.getBlock() instanceof ShulkerBoxBlock)) {
            this.checkBlockUpdate(blockUpdateEvent.pos, blockUpdateEvent.newState);
         }
      }
   }

   private void checkBlockUpdate(BlockPos blockPos, BlockState blockState) {
      MinecraftClient var5 = MinecraftClient.getInstance();
      if (!this.strangerDetected && var5.world != null) {
         for (PlayerEntity var7 : var5.world.getPlayers()) {
            if (var7.getEyePos().distanceTo(Vec3d.ofCenter(blockPos)) < 8.0 && this.isWhitelisted(var7.getName().getString())) {
               return;
            }
         }

         if (blockPos.getY() > -64 && blockState.isAir()) {
            if (!(var5.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos)) < 6.0)) {
               double var12 = var5.player.getX() - blockPos.getX();
               double var3;
               double var8 = Math.sqrt(var12 * var12 + (var3 = var5.player.getZ() - blockPos.getZ()) * var3);
               if (!(var8 > this.scanRange.get())) {
                  long var10 = System.currentTimeMillis();
                  if (var10 - this.lastBreakTimestamp > this.blockUpdateWindow.getInt() * 1000L) {
                     this.blockBreakCounter = 0;
                  }

                  this.blockBreakCounter++;
                  this.lastBreakTimestamp = var10;
                  if (this.blockBreakCounter >= this.blockUpdateThreshold.getInt()) {
                     this.strangerDetected = true;
                     this.currentState = SpawnerProtectModule.State.WORKING;
                     ModuleChat.send(
                        this,
                        "\ud83d\udea8 UZAK TESP\u0130T: Yak\u0131nda bir blok k\u0131r\u0131ld\u0131! (Yatay Mesafe: "
                           + (int)var8
                           + "m, Y: "
                           + blockPos.getY()
                           + ")"
                     );
                     this.sendWebhook(
                        "\ud83d\udea8 Protection activated",
                        "The block-break threshold was reached. Spawner collection has started.",
                        15548997,
                        SpawnerAlerts.field("\ud83d\udce1 Trigger", "Block breaking", true),
                        SpawnerAlerts.field("\ud83e\uddf1 Block counter", this.blockBreakCounter + " / " + this.blockUpdateThreshold.getInt(), true),
                        SpawnerAlerts.field("\ud83d\udccf Horizontal distance", (int)var8 + " blocks", true),
                        SpawnerAlerts.field("\ud83d\udccd Block location", coordinates(blockPos), false)
                     );
                  }
               }
            }
         }
      }
   }

   private void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player == null || var2.world == null || var2.interactionManager == null || var2.currentScreen instanceof DisconnectedScreen) {
         this.resetModule();
      } else if (this.isNearSpawn()) {
         if (this.strangerDetected) {
            this.resetModule();
         }
      } else {
         this.notifyNearbyPlayers();
         if (this.currentState != SpawnerProtectModule.State.WAITING_FOR_STRANGER) {
            if (this.currentState != SpawnerProtectModule.State.OPENING_CHEST
               && this.currentState != SpawnerProtectModule.State.DEPOSITING_ITEMS
               && this.currentState != SpawnerProtectModule.State.BUYING_ECHEST) {
               this.updateSneak(true);
            }

            this.handleRotation();
            switch (this.currentState) {
               case WORKING:
                  this.handleWorking();
                  break;
               case GOING_TO_CHEST:
                  this.handleGoingToChest();
                  break;
               case OPENING_CHEST:
                  this.handleOpeningChest();
                  break;
               case DEPOSITING_ITEMS:
                  this.handleDepositing();
                  break;
               case FINAL_EXIT:
                  this.handleFinalExit();
                  break;
               case BUYING_ECHEST:
                  this.handleBuyingEChest();
                  break;
               case PLACING_ECHEST:
                  this.handlePlacingEChest();
            }
         }
      }
   }

   private void notifyNearbyPlayers() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      HashSet var2 = new HashSet();

      for (PlayerEntity var4 : var1.world.getPlayers()) {
         if (var4 != var1.player
            && !var4.isSpectator()
            && !this.isWhitelisted(var4.getName().getString())
            && !(var1.player.distanceTo(var4) > this.scanRange.get())) {
            var2.add(var4.getUuid());
            if (!this.nearbyPlayers.contains(var4.getUuid())) {
               this.sendWebhook(
                  "\ud83d\udc64 A player entered your range",
                  "A new player is nearby. This event only sends a notification.",
                  5793266,
                  SpawnerAlerts.field("Player", "`" + var4.getName().getString() + "`", true),
                  SpawnerAlerts.field("\ud83d\udccf Distance", String.format(Locale.ROOT, "%.1f blocks", var1.player.distanceTo(var4)), true),
                  SpawnerAlerts.field("\ud83d\udee1\ufe0f Protection", this.strangerDetected ? "In progress" : "Standby", true),
                  SpawnerAlerts.field("\ud83d\udccd Player location", coordinates(var4.getBlockPos()), false)
               );
            }
         }
      }

      this.nearbyPlayers.clear();
      this.nearbyPlayers.addAll(var2);
   }

   private void handleRotation() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      Vec3d var2 = null;
      if (this.currentState == SpawnerProtectModule.State.WORKING) {
         ItemEntity var3 = this.findDroppedSpawner();
         if (var3 != null) {
            var2 = var3.getEntityPos();
         } else {
            this.retargetSpawner(var1);
            if (this.targetBlock != null) {
               var2 = Vec3d.ofCenter(this.targetBlock);
            }
         }
      } else if ((this.currentState == SpawnerProtectModule.State.GOING_TO_CHEST || this.currentState == SpawnerProtectModule.State.OPENING_CHEST)
         && this.targetChest != null) {
         var2 = Vec3d.ofCenter(this.targetChest);
      }

      if (var2 != null) {
         this.smoothLook(var1.player, var2);
      }
   }

   private void retargetSpawner(MinecraftClient minecraftClient) {
      if (this.targetBlock == null
         || minecraftClient.world.getBlockState(this.targetBlock).getBlock() != Blocks.SPAWNER
         || minecraftClient.player.getBlockPos().getSquaredDistance(this.targetBlock) > 256.0) {
         this.targetBlock = this.findRandomBlock(Blocks.SPAWNER, 16);
         this.miningTicks = 0;
      }
   }

   private void smoothLook(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
      Vec3d var3 = clientPlayerEntity.getEyePos();
      double var4 = vec3d.x - var3.x;
      double var6 = vec3d.y - var3.y;
      double var8 = vec3d.z - var3.z;
      double var10 = Math.sqrt(var4 * var4 + var8 * var8);
      float var12 = (float)Math.toDegrees(Math.atan2(-var4, var8));
      float var13 = (float)(-Math.toDegrees(Math.atan2(var6, var10)));
      float var14 = this.rotationSpeed.get();
      clientPlayerEntity.setYaw(clientPlayerEntity.getYaw() + MathHelper.clamp(MathHelper.wrapDegrees(var12 - clientPlayerEntity.getYaw()), -var14, var14));
      clientPlayerEntity.setPitch(
         clientPlayerEntity.getPitch() + MathHelper.clamp(MathHelper.wrapDegrees(var13 - clientPlayerEntity.getPitch()), -var14, var14)
      );
   }

   private void handleWorking() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      ClientPlayerEntity var2 = var1.player;
      if (var2.currentScreenHandler != var2.playerScreenHandler) {
         var2.closeHandledScreen();
      }

      if (this.findDroppedSpawner() != null) {
         this.lagWaitTicks = 0;
         this.stopBreaking();
         var1.options.forwardKey.setPressed(true);
      } else {
         int var3 = this.getSpawnerCount();
         if (var3 < this.targetStackCount.getInt() * 64 && (var3 <= 0 || var2.getInventory().getEmptySlot() != -1)) {
            this.retargetSpawner(var1);
            if (this.targetBlock == null) {
               this.stopMovement();
               if (this.lagWaitTicks < 40) {
                  this.lagWaitTicks++;
               } else if (this.getSpawnerCount() > 0) {
                  this.goToChest();
               } else {
                  this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
                  this.lagWaitTicks = 0;
               }
            } else {
               this.lagWaitTicks = 0;
               if (var2.getEyePos().distanceTo(Vec3d.ofCenter(this.targetBlock)) > this.breakRange.get()) {
                  this.stopBreaking();
                  var1.options.forwardKey.setPressed(true);
               } else {
                  var1.options.forwardKey.setPressed(false);
                  this.miningTicks++;
                  if (this.miningTicks > 60) {
                     var1.interactionManager
                        .interactBlock(var2, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(this.targetBlock), Direction.UP, this.targetBlock, false));
                     var2.swingHand(Hand.MAIN_HAND);
                     this.miningTicks = 0;
                  }

                  if (this.breakCooldown <= 0) {
                     var1.interactionManager.updateBlockBreakingProgress(this.targetBlock, Direction.UP);
                     var2.swingHand(Hand.MAIN_HAND);
                     var1.options.attackKey.setPressed(true);
                     this.breakCooldown = 6;
                  } else {
                     this.breakCooldown--;
                  }
               }
            }
         } else {
            this.goToChest();
         }
      }
   }

   private void goToChest() {
      this.stopMovement();
      this.targetChest = this.findNearestBlock(Blocks.ENDER_CHEST, 4);
      if (this.targetChest != null) {
         this.currentState = SpawnerProtectModule.State.GOING_TO_CHEST;
      } else if (this.getEnderChestCount() > 0) {
         this.currentState = SpawnerProtectModule.State.PLACING_ECHEST;
         this.shopTick = 0;
      } else {
         this.currentState = SpawnerProtectModule.State.BUYING_ECHEST;
         this.shopSequence = 0;
         this.shopTick = 0;
      }
   }

   private void handleGoingToChest() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (this.targetChest == null) {
         this.targetChest = this.findNearestBlock(Blocks.ENDER_CHEST, 4);
      }

      if (this.targetChest == null) {
         this.currentState = SpawnerProtectModule.State.WORKING;
      } else {
         var1.options.forwardKey.setPressed(true);
         if (var1.player.getBlockPos().isWithinDistance(this.targetChest, 4.0)) {
            this.stopMovement();
            this.currentState = SpawnerProtectModule.State.OPENING_CHEST;
         }
      }
   }

   private void handleOpeningChest() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      this.updateSneak(false);
      if (this.chestTick > 200) {
         ModuleChat.send(this, "Ender chest a\u00e7\u0131lamad\u0131; e\u015fyalar envanterde tutuluyor.");
         this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
         this.lagWaitTicks = 0;
      } else {
         if (this.chestTick % 12 == 0 && this.targetChest != null) {
            var1.interactionManager
               .interactBlock(var1.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(this.targetChest), Direction.UP, this.targetChest, false));
         }

         this.chestTick++;
         if (var1.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            this.chestTick = 0;
            this.currentState = SpawnerProtectModule.State.DEPOSITING_ITEMS;
            this.lagWaitTicks = 0;
         }
      }
   }

   private void handleBuyingEChest() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      this.stopMovement();
      this.updateSneak(false);
      this.shopTick++;
      if (this.shopTick > 200) {
         ModuleChat.send(this, "Market zaman a\u015f\u0131m\u0131; e\u015fyalar envanterde tutuluyor.");
         this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
         this.lagWaitTicks = 0;
      } else if (this.shopSequence > 0 && var1.currentScreen == null && this.shopTick > 60) {
         ModuleChat.send(this, "Market ekran\u0131 kapand\u0131, ba\u015ftan deneniyor...");
         this.shopSequence = 0;
         this.shopTick = 0;
      } else if (this.shopTick >= 30) {
         switch (this.shopSequence) {
            case 0:
               ModuleChat.send(this, "Market a\u00e7\u0131l\u0131yor: /shop");
               var1.getNetworkHandler().sendChatCommand("shop");
               this.shopSequence = 1;
               this.shopTick = 0;
               break;
            case 1:
               this.shopStep(var1, "SHOP", "Market: End kategorisi se\u00e7iliyor...", 11);
               break;
            case 2:
               this.shopStep(var1, "END", "Market: Ender Chest se\u00e7iliyor...", 9);
               break;
            case 3:
               this.shopStep(var1, "ENDER CHEST", "Market: Sat\u0131n alma onaylan\u0131yor...", 25);
               break;
            case 4:
               if (this.getEnderChestCount() > 0) {
                  ModuleChat.send(this, "Ender Chest sat\u0131n al\u0131nd\u0131.");
                  var1.player.closeHandledScreen();
                  this.currentState = SpawnerProtectModule.State.PLACING_ECHEST;
                  this.shopTick = 0;
               } else if (this.shopTick > 100) {
                  ModuleChat.send(this, "Sat\u0131n alma ba\u015far\u0131s\u0131z oldu (Zaman a\u015f\u0131m\u0131).");
                  var1.player.closeHandledScreen();
                  this.shopSequence = 0;
                  this.shopTick = 0;
               }
         }
      }
   }

   private void shopStep(MinecraftClient minecraftClient, String string, String string2, int i) {
      if (minecraftClient.currentScreen != null && minecraftClient.currentScreen.getTitle().getString().toUpperCase(Locale.ROOT).contains(string)) {
         ModuleChat.send(this, string2);
         minecraftClient.interactionManager.clickSlot(minecraftClient.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, minecraftClient.player);
         this.shopSequence++;
         this.shopTick = 0;
      }
   }

   private void handlePlacingEChest() {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      this.shopTick++;
      if (this.shopTick >= 10) {
         int var4 = -1;

         for (int var1 = 0; var1 < 9; var1++) {
            if (var3.getInventory().getStack(var1).getItem() == Blocks.ENDER_CHEST.asItem()) {
               var4 = var1;
               break;
            }
         }

         if (var4 == -1) {
            for (int var12 = 9; var12 < 36; var12++) {
               if (var3.getInventory().getStack(var12).getItem() == Blocks.ENDER_CHEST.asItem()) {
                  var3.closeHandledScreen();
                  var2.interactionManager.clickSlot(var3.playerScreenHandler.syncId, var12, 0, SlotActionType.SWAP, var3);
                  this.shopTick = 0;
                  return;
               }
            }

            this.currentState = SpawnerProtectModule.State.BUYING_ECHEST;
            this.shopSequence = 0;
         } else {
            var3.getInventory().setSelectedSlot(var4);
            BlockPos var5 = var3.getBlockPos();
            BlockPos var6 = null;

            for (Direction var10 : Direction.values()) {
               BlockPos var11;
               if (var10 != Direction.UP
                  && var10 != Direction.DOWN
                  && var2.world.getBlockState(var11 = var5.offset(var10)).isReplaceable()
                  && !var2.world.getBlockState(var11.down()).isAir()) {
                  var6 = var11;
                  break;
               }
            }

            if (var6 == null) {
               ModuleChat.send(this, "E-Chest koyacak yer bulunamad\u0131!");
               this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
               this.lagWaitTicks = 0;
            } else {
               BlockPos var13 = var6.down();
               var2.interactionManager
                  .interactBlock(var3, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(var13).add(0.0, 0.5, 0.0), Direction.UP, var13, false));
               var3.swingHand(Hand.MAIN_HAND);
               this.targetChest = var6;
               this.currentState = SpawnerProtectModule.State.GOING_TO_CHEST;
            }
         }
      }
   }

   private int getEnderChestCount() {
      return countInInventory(Blocks.ENDER_CHEST);
   }

   private void handleDepositing() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (this.lagWaitTicks < 15) {
         this.lagWaitTicks++;
      } else if (!(var1.player.currentScreenHandler instanceof GenericContainerScreenHandler var3)) {
         this.currentState = SpawnerProtectModule.State.OPENING_CHEST;
         this.chestTick = 0;
      } else {
         int var4 = var3.slots.size() - 36;
         boolean var5 = false;

         for (int var6 = 0; var6 < var4; var6++) {
            ItemStack var7 = var3.getSlot(var6).getStack();
            if (var7.isEmpty() || var7.getItem() == Blocks.SPAWNER.asItem() && var7.getCount() < var7.getMaxCount()) {
               var5 = true;
               break;
            }
         }

         boolean var9 = false;

         for (int var10 = 0; var10 < 36; var10++) {
            int var8 = var4 + var10;
            if (var3.getSlot(var8).getStack().getItem() == Blocks.SPAWNER.asItem()) {
               if (!var5) {
                  this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
                  this.lagWaitTicks = 0;
                  return;
               }

               var1.interactionManager.clickSlot(var3.syncId, var8, 0, SlotActionType.QUICK_MOVE, var1.player);
               var9 = true;
               break;
            }
         }

         if (!var9) {
            var1.player.closeHandledScreen();
            if (this.findNearestBlock(Blocks.SPAWNER, 16) != null) {
               this.currentState = SpawnerProtectModule.State.WORKING;
            } else {
               this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
               this.lagWaitTicks = 0;
            }
         }
      }
   }

   private void handleFinalExit() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (this.lagWaitTicks == 0) {
         int var2 = this.getSpawnerCount();
         boolean var3 = var2 > 0;
         this.sendWebhook(
            var3 ? "\u26a0\ufe0f Spawners remain in inventory" : "\u2705 Task completed",
            var3
               ? "Some spawners are still in your inventory. Disconnecting in two seconds."
               : "The collection sequence has ended. Disconnecting in two seconds.",
            var3 ? 16705372 : 5763719,
            SpawnerAlerts.field("\ud83c\udf92 Remaining in inventory", var2 + " spawners", true),
            SpawnerAlerts.field("\ud83d\udeaa Next step", "Disconnect from server", true)
         );
         ModuleChat.send(this, "G\u00f6rev bitti, e\u015fyalar\u0131n sunucuya kaydedilmesi i\u00e7in 2 saniye beklenip \u00e7\u0131k\u0131lacak...");
      }

      this.stopMovement();
      this.updateSneak(false);
      this.lagWaitTicks++;
      if (this.lagWaitTicks > 40) {
         if (var1.getNetworkHandler() != null && var1.getNetworkHandler().getConnection().isOpen()) {
            AutoReconnectModule.holdOff();
            var1.getNetworkHandler()
               .getConnection()
               .disconnect(Text.literal("[SpawnerProtect] G\u00f6rev tamamland\u0131, g\u00fcvenli \u00e7\u0131k\u0131\u015f yap\u0131ld\u0131."));
         }

         this.resetModule();
         this.requestDisable();
      }
   }

   @SafeVarargs
   private void sendWebhook(String string, String string2, int i, Map<String, Object>... maps) {
      MinecraftClient var5 = MinecraftClient.getInstance();
      if (!this.webhookUrl.get().isBlank() && var5.player != null && var5.world != null) {
         ArrayList var6 = new ArrayList<>(Arrays.asList(maps));
         var6.add(SpawnerAlerts.field("\ud83d\udc64 Account", "`" + var5.player.getName().getString() + "`", true));
         var6.add(SpawnerAlerts.field("\ud83c\udf0d Dimension", "`" + var5.world.getRegistryKey().getValue() + "`", true));
         var6.add(SpawnerAlerts.field("\ud83d\udccd Your location", coordinates(var5.player.getBlockPos()), false));
         SpawnerAlerts.send(this.webhookUrl.get(), SpawnerAlerts.payload(string, string2, i, var6, Instant.now()));
      }
   }

   private static String coordinates(BlockPos blockPos) {
      return "`X: " + blockPos.getX() + "  Y: " + blockPos.getY() + "  Z: " + blockPos.getZ() + "`";
   }

   private void updateSneak(boolean bl) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null) {
         var2.player.setSneaking(bl);
      }

      var2.options.sneakKey.setPressed(bl);
   }

   private boolean isNearSpawn() {
      ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
      return var1 != null && Math.abs(var1.getX()) < 100.0 && Math.abs(var1.getZ()) < 100.0;
   }

   private boolean isWhitelisted(String string) {
      return Arrays.stream(this.whitelist.get().split("[,;\\s]+")).anyMatch(string2 -> string2.equalsIgnoreCase(string));
   }

   private BlockPos findNearestBlock(Block block, int i) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      BlockPos var4 = var3.player.getBlockPos();
      BlockPos var5 = null;
      double var6 = 1.7976931348623157E308;

      for (int var8 = -i; var8 <= i; var8++) {
         for (int var9 = -i; var9 <= i; var9++) {
            for (int var10 = -i; var10 <= i; var10++) {
               BlockPos var13 = var4.add(var8, var9, var10);
               double var11;
               if (var3.world.getBlockState(var13).getBlock() == block && (var11 = var4.getSquaredDistance(var13)) < var6) {
                  var6 = var11;
                  var5 = var13;
               }
            }
         }
      }

      return var5;
   }

   private BlockPos findRandomBlock(Block block, int i) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      BlockPos var4 = var3.player.getBlockPos();
      ArrayList var5 = new ArrayList();
      double var6 = i * i;

      for (int var8 = -i; var8 <= i; var8++) {
         for (int var9 = -i; var9 <= i; var9++) {
            for (int var10 = -i; var10 <= i; var10++) {
               BlockPos var11 = var4.add(var8, var9, var10);
               if (var4.getSquaredDistance(var11) <= var6 && var3.world.getBlockState(var11).getBlock() == block) {
                  var5.add(var11);
               }
            }
         }
      }

      return var5.isEmpty() ? null : (BlockPos)var5.get(new Random().nextInt(var5.size()));
   }

   private int getSpawnerCount() {
      return countInInventory(Blocks.SPAWNER);
   }

   private static int countInInventory(Block block) {
      ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
      int var2 = 0;

      for (int var3 = 0; var3 < 36; var3++) {
         ItemStack var4 = var1.getInventory().getStack(var3);
         if (var4.getItem() == block.asItem()) {
            var2 += var4.getCount();
         }
      }

      return var2;
   }

   private ItemEntity findDroppedSpawner() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      ItemEntity var2 = null;
      float var3 = 3.4028235E38F;

      for (Entity var5 : var1.world.getEntities()) {
         float var6;
         ItemEntity var7;
         if (var5 instanceof ItemEntity
            && (var7 = (ItemEntity)var5).getStack().getItem() == Blocks.SPAWNER.asItem()
            && !((var6 = var1.player.distanceTo(var7)) >= 16.0F)
            && !var7.cannotPickup()
            && var6 < var3) {
            var3 = var6;
            var2 = var7;
         }
      }

      return var2;
   }

   private void stopBreaking() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      var1.options.attackKey.setPressed(false);
      if (var1.interactionManager != null) {
         var1.interactionManager.cancelBlockBreaking();
      }

      this.breakCooldown = 0;
   }

   private void stopMovement() {
      this.stopBreaking();
      MinecraftClient.getInstance().options.forwardKey.setPressed(false);
   }

   @Environment(EnvType.CLIENT)
   private static enum State {
      WAITING_FOR_STRANGER,
      WORKING,
      GOING_TO_CHEST,
      OPENING_CHEST,
      DEPOSITING_ITEMS,
      FINAL_EXIT,
      BUYING_ECHEST,
      PLACING_ECHEST;

      private State() {
      }
   }
}
