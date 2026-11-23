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
import net.minecraft.util.math.Vec3i;

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
   public void migrateSettings(int var1) {
      if (var1 < 1 && this.targetStackCount.get() == 3.0F) {
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

   private void onBlockUpdate(BlockUpdateEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (!this.strangerDetected && var2.field_1724 != null && var2.field_1687 != null && !this.isNearSpawn() && this.detectBlockUpdates.get()) {
         if (!var1.oldState.method_26215() && !(var1.oldState.method_26204() instanceof ShulkerBoxBlock)) {
            this.checkBlockUpdate(var1.pos, var1.newState);
         }
      }
   }

   private void checkBlockUpdate(BlockPos var1, BlockState var2) {
      MinecraftClient var5 = MinecraftClient.method_1551();
      if (!this.strangerDetected && var5.field_1687 != null) {
         for (PlayerEntity var7 : var5.field_1687.method_18456()) {
            if (var7.method_33571().method_1022(Vec3d.method_24953(var1)) < 8.0 && this.isWhitelisted(var7.method_5477().getString())) {
               return;
            }
         }

         if (var1.method_10264() > -64 && var2.method_26215()) {
            if (!(var5.field_1724.method_33571().method_1022(Vec3d.method_24953(var1)) < 6.0)) {
               double var12 = var5.field_1724.method_23317() - var1.method_10263();
               double var3;
               double var8 = Math.sqrt(var12 * var12 + (var3 = var5.field_1724.method_23321() - var1.method_10260()) * var3);
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
                           + var1.method_10264()
                           + ")"
                     );
                     this.sendWebhook(
                        "\ud83d\udea8 Protection activated",
                        "The block-break threshold was reached. Spawner collection has started.",
                        15548997,
                        SpawnerAlerts.field("\ud83d\udce1 Trigger", "Block breaking", true),
                        SpawnerAlerts.field("\ud83e\uddf1 Block counter", this.blockBreakCounter + " / " + this.blockUpdateThreshold.getInt(), true),
                        SpawnerAlerts.field("\ud83d\udccf Horizontal distance", (int)var8 + " blocks", true),
                        SpawnerAlerts.field("\ud83d\udccd Block location", coordinates(var1), false)
                     );
                  }
               }
            }
         }
      }
   }

   private void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1724 == null || var2.field_1687 == null || var2.field_1761 == null || var2.field_1755 instanceof DisconnectedScreen) {
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
      MinecraftClient var1 = MinecraftClient.method_1551();
      HashSet var2 = new HashSet();

      for (PlayerEntity var4 : var1.field_1687.method_18456()) {
         if (var4 != var1.field_1724
            && !var4.method_7325()
            && !this.isWhitelisted(var4.method_5477().getString())
            && !(var1.field_1724.method_5739(var4) > this.scanRange.get())) {
            var2.add(var4.method_5667());
            if (!this.nearbyPlayers.contains(var4.method_5667())) {
               this.sendWebhook(
                  "\ud83d\udc64 A player entered your range",
                  "A new player is nearby. This event only sends a notification.",
                  5793266,
                  SpawnerAlerts.field("Player", "`" + var4.method_5477().getString() + "`", true),
                  SpawnerAlerts.field("\ud83d\udccf Distance", String.format(Locale.ROOT, "%.1f blocks", var1.field_1724.method_5739(var4)), true),
                  SpawnerAlerts.field("\ud83d\udee1\ufe0f Protection", this.strangerDetected ? "In progress" : "Standby", true),
                  SpawnerAlerts.field("\ud83d\udccd Player location", coordinates(var4.method_24515()), false)
               );
            }
         }
      }

      this.nearbyPlayers.clear();
      this.nearbyPlayers.addAll(var2);
   }

   private void handleRotation() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      Vec3d var2 = null;
      if (this.currentState == SpawnerProtectModule.State.WORKING) {
         ItemEntity var3 = this.findDroppedSpawner();
         if (var3 != null) {
            var2 = var3.method_73189();
         } else {
            this.retargetSpawner(var1);
            if (this.targetBlock != null) {
               var2 = Vec3d.method_24953((Vec3i)this.targetBlock);
            }
         }
      } else if ((this.currentState == SpawnerProtectModule.State.GOING_TO_CHEST || this.currentState == SpawnerProtectModule.State.OPENING_CHEST)
         && this.targetChest != null) {
         var2 = Vec3d.method_24953((Vec3i)this.targetChest);
      }

      if (var2 != null) {
         this.smoothLook(var1.field_1724, var2);
      }
   }

   private void retargetSpawner(MinecraftClient var1) {
      if (this.targetBlock == null
         || var1.field_1687.method_8320(this.targetBlock).method_26204() != Blocks.field_10260
         || var1.field_1724.method_24515().method_10262((Vec3i)this.targetBlock) > 256.0) {
         this.targetBlock = this.findRandomBlock(Blocks.field_10260, 16);
         this.miningTicks = 0;
      }
   }

   private void smoothLook(ClientPlayerEntity var1, Vec3d var2) {
      Vec3d var3 = var1.method_33571();
      double var4 = var2.field_1352 - var3.field_1352;
      double var6 = var2.field_1351 - var3.field_1351;
      double var8 = var2.field_1350 - var3.field_1350;
      double var10 = Math.sqrt(var4 * var4 + var8 * var8);
      float var12 = (float)Math.toDegrees(Math.atan2(-var4, var8));
      float var13 = (float)(-Math.toDegrees(Math.atan2(var6, var10)));
      float var14 = this.rotationSpeed.get();
      var1.method_36456(var1.method_36454() + MathHelper.method_15363(MathHelper.method_15393(var12 - var1.method_36454()), -var14, var14));
      var1.method_36457(var1.method_36455() + MathHelper.method_15363(MathHelper.method_15393(var13 - var1.method_36455()), -var14, var14));
   }

   private void handleWorking() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2.field_7512 != var2.field_7498) {
         var2.method_7346();
      }

      if (this.findDroppedSpawner() != null) {
         this.lagWaitTicks = 0;
         this.stopBreaking();
         var1.field_1690.field_1894.method_23481(true);
      } else {
         int var3 = this.getSpawnerCount();
         if (var3 < this.targetStackCount.getInt() * 64 && (var3 <= 0 || var2.method_31548().method_7376() != -1)) {
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
               if (var2.method_33571().method_1022(Vec3d.method_24953((Vec3i)this.targetBlock)) > this.breakRange.get()) {
                  this.stopBreaking();
                  var1.field_1690.field_1894.method_23481(true);
               } else {
                  var1.field_1690.field_1894.method_23481(false);
                  this.miningTicks++;
                  if (this.miningTicks > 60) {
                     var1.field_1761
                        .method_2896(
                           var2,
                           Hand.field_5808,
                           new BlockHitResult(Vec3d.method_24953((Vec3i)this.targetBlock), Direction.field_11036, this.targetBlock, false)
                        );
                     var2.method_6104(Hand.field_5808);
                     this.miningTicks = 0;
                  }

                  if (this.breakCooldown <= 0) {
                     var1.field_1761.method_2902(this.targetBlock, Direction.field_11036);
                     var2.method_6104(Hand.field_5808);
                     var1.field_1690.field_1886.method_23481(true);
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
      this.targetChest = this.findNearestBlock(Blocks.field_10443, 4);
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
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (this.targetChest == null) {
         this.targetChest = this.findNearestBlock(Blocks.field_10443, 4);
      }

      if (this.targetChest == null) {
         this.currentState = SpawnerProtectModule.State.WORKING;
      } else {
         var1.field_1690.field_1894.method_23481(true);
         if (var1.field_1724.method_24515().method_19771((Vec3i)this.targetChest, 4.0)) {
            this.stopMovement();
            this.currentState = SpawnerProtectModule.State.OPENING_CHEST;
         }
      }
   }

   private void handleOpeningChest() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      this.updateSneak(false);
      if (this.chestTick > 200) {
         ModuleChat.send(this, "Ender chest a\u00e7\u0131lamad\u0131; e\u015fyalar envanterde tutuluyor.");
         this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
         this.lagWaitTicks = 0;
      } else {
         if (this.chestTick % 12 == 0 && this.targetChest != null) {
            var1.field_1761
               .method_2896(
                  var1.field_1724,
                  Hand.field_5808,
                  new BlockHitResult(Vec3d.method_24953((Vec3i)this.targetChest), Direction.field_11036, this.targetChest, false)
               );
         }

         this.chestTick++;
         if (var1.field_1724.field_7512 instanceof GenericContainerScreenHandler) {
            this.chestTick = 0;
            this.currentState = SpawnerProtectModule.State.DEPOSITING_ITEMS;
            this.lagWaitTicks = 0;
         }
      }
   }

   private void handleBuyingEChest() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      this.stopMovement();
      this.updateSneak(false);
      this.shopTick++;
      if (this.shopTick > 200) {
         ModuleChat.send(this, "Market zaman a\u015f\u0131m\u0131; e\u015fyalar envanterde tutuluyor.");
         this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
         this.lagWaitTicks = 0;
      } else if (this.shopSequence > 0 && var1.field_1755 == null && this.shopTick > 60) {
         ModuleChat.send(this, "Market ekran\u0131 kapand\u0131, ba\u015ftan deneniyor...");
         this.shopSequence = 0;
         this.shopTick = 0;
      } else if (this.shopTick >= 30) {
         switch (this.shopSequence) {
            case 0:
               ModuleChat.send(this, "Market a\u00e7\u0131l\u0131yor: /shop");
               var1.method_1562().method_45730("shop");
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
                  var1.field_1724.method_7346();
                  this.currentState = SpawnerProtectModule.State.PLACING_ECHEST;
                  this.shopTick = 0;
               } else if (this.shopTick > 100) {
                  ModuleChat.send(this, "Sat\u0131n alma ba\u015far\u0131s\u0131z oldu (Zaman a\u015f\u0131m\u0131).");
                  var1.field_1724.method_7346();
                  this.shopSequence = 0;
                  this.shopTick = 0;
               }
         }
      }
   }

   private void shopStep(MinecraftClient var1, String var2, String var3, int var4) {
      if (var1.field_1755 != null && var1.field_1755.method_25440().getString().toUpperCase(Locale.ROOT).contains(var2)) {
         ModuleChat.send(this, var3);
         var1.field_1761.method_2906(var1.field_1724.field_7512.field_7763, var4, 0, SlotActionType.field_7790, var1.field_1724);
         this.shopSequence++;
         this.shopTick = 0;
      }
   }

   private void handlePlacingEChest() {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      this.shopTick++;
      if (this.shopTick >= 10) {
         int var4 = -1;

         for (int var1 = 0; var1 < 9; var1++) {
            if (var3.method_31548().method_5438(var1).method_7909() == Blocks.field_10443.method_8389()) {
               var4 = var1;
               break;
            }
         }

         if (var4 == -1) {
            for (int var12 = 9; var12 < 36; var12++) {
               if (var3.method_31548().method_5438(var12).method_7909() == Blocks.field_10443.method_8389()) {
                  var3.method_7346();
                  var2.field_1761.method_2906(var3.field_7498.field_7763, var12, 0, SlotActionType.field_7791, var3);
                  this.shopTick = 0;
                  return;
               }
            }

            this.currentState = SpawnerProtectModule.State.BUYING_ECHEST;
            this.shopSequence = 0;
         } else {
            var3.method_31548().method_61496(var4);
            BlockPos var5 = var3.method_24515();
            BlockPos var6 = null;

            for (Direction var10 : Direction.values()) {
               BlockPos var11;
               if (var10 != Direction.field_11036
                  && var10 != Direction.field_11033
                  && var2.field_1687.method_8320(var11 = var5.method_10093(var10)).method_45474()
                  && !var2.field_1687.method_8320(var11.method_10074()).method_26215()) {
                  var6 = var11;
                  break;
               }
            }

            if (var6 == null) {
               ModuleChat.send(this, "E-Chest koyacak yer bulunamad\u0131!");
               this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
               this.lagWaitTicks = 0;
            } else {
               BlockPos var13 = var6.method_10074();
               var2.field_1761
                  .method_2896(
                     var3, Hand.field_5808, new BlockHitResult(Vec3d.method_24953(var13).method_1031(0.0, 0.5, 0.0), Direction.field_11036, var13, false)
                  );
               var3.method_6104(Hand.field_5808);
               this.targetChest = var6;
               this.currentState = SpawnerProtectModule.State.GOING_TO_CHEST;
            }
         }
      }
   }

   private int getEnderChestCount() {
      return countInInventory(Blocks.field_10443);
   }

   private void handleDepositing() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (this.lagWaitTicks < 15) {
         this.lagWaitTicks++;
      } else if (!(var1.field_1724.field_7512 instanceof GenericContainerScreenHandler var3)) {
         this.currentState = SpawnerProtectModule.State.OPENING_CHEST;
         this.chestTick = 0;
      } else {
         int var4 = var3.field_7761.size() - 36;
         boolean var5 = false;

         for (int var6 = 0; var6 < var4; var6++) {
            ItemStack var7 = var3.method_7611(var6).method_7677();
            if (var7.method_7960() || var7.method_7909() == Blocks.field_10260.method_8389() && var7.method_7947() < var7.method_7914()) {
               var5 = true;
               break;
            }
         }

         boolean var9 = false;

         for (int var10 = 0; var10 < 36; var10++) {
            int var8 = var4 + var10;
            if (var3.method_7611(var8).method_7677().method_7909() == Blocks.field_10260.method_8389()) {
               if (!var5) {
                  this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
                  this.lagWaitTicks = 0;
                  return;
               }

               var1.field_1761.method_2906(var3.field_7763, var8, 0, SlotActionType.field_7794, var1.field_1724);
               var9 = true;
               break;
            }
         }

         if (!var9) {
            var1.field_1724.method_7346();
            if (this.findNearestBlock(Blocks.field_10260, 16) != null) {
               this.currentState = SpawnerProtectModule.State.WORKING;
            } else {
               this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
               this.lagWaitTicks = 0;
            }
         }
      }
   }

   private void handleFinalExit() {
      MinecraftClient var1 = MinecraftClient.method_1551();
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
         if (var1.method_1562() != null && var1.method_1562().method_48296().method_10758()) {
            AutoReconnectModule.holdOff();
            var1.method_1562()
               .method_48296()
               .method_10747(Text.method_43470("[SpawnerProtect] G\u00f6rev tamamland\u0131, g\u00fcvenli \u00e7\u0131k\u0131\u015f yap\u0131ld\u0131."));
         }

         this.resetModule();
         this.requestDisable();
      }
   }

   @SafeVarargs
   private void sendWebhook(String var1, String var2, int var3, Map<String, Object>... var4) {
      MinecraftClient var5 = MinecraftClient.method_1551();
      if (!this.webhookUrl.get().isBlank() && var5.field_1724 != null && var5.field_1687 != null) {
         ArrayList var6 = new ArrayList<>(Arrays.asList(var4));
         var6.add(SpawnerAlerts.field("\ud83d\udc64 Account", "`" + var5.field_1724.method_5477().getString() + "`", true));
         var6.add(SpawnerAlerts.field("\ud83c\udf0d Dimension", "`" + var5.field_1687.method_27983().method_29177() + "`", true));
         var6.add(SpawnerAlerts.field("\ud83d\udccd Your location", coordinates(var5.field_1724.method_24515()), false));
         SpawnerAlerts.send(this.webhookUrl.get(), SpawnerAlerts.payload(var1, var2, var3, var6, Instant.now()));
      }
   }

   private static String coordinates(BlockPos var0) {
      return "`X: " + var0.method_10263() + "  Y: " + var0.method_10264() + "  Z: " + var0.method_10260() + "`";
   }

   private void updateSneak(boolean var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1724 != null) {
         var2.field_1724.method_5660(var1);
      }

      var2.field_1690.field_1832.method_23481(var1);
   }

   private boolean isNearSpawn() {
      ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
      return var1 != null && Math.abs(var1.method_23317()) < 100.0 && Math.abs(var1.method_23321()) < 100.0;
   }

   private boolean isWhitelisted(String var1) {
      return Arrays.stream(this.whitelist.get().split("[,;\\s]+")).anyMatch(var1x -> var1x.equalsIgnoreCase(var1));
   }

   private BlockPos findNearestBlock(Block var1, int var2) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      BlockPos var4 = var3.field_1724.method_24515();
      BlockPos var5 = null;
      double var6 = 1.7976931348623157E308;

      for (int var8 = -var2; var8 <= var2; var8++) {
         for (int var9 = -var2; var9 <= var2; var9++) {
            for (int var10 = -var2; var10 <= var2; var10++) {
               BlockPos var13 = var4.method_10069(var8, var9, var10);
               double var11;
               if (var3.field_1687.method_8320(var13).method_26204() == var1 && (var11 = var4.method_10262(var13)) < var6) {
                  var6 = var11;
                  var5 = var13;
               }
            }
         }
      }

      return var5;
   }

   private BlockPos findRandomBlock(Block var1, int var2) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      BlockPos var4 = var3.field_1724.method_24515();
      ArrayList var5 = new ArrayList();
      double var6 = var2 * var2;

      for (int var8 = -var2; var8 <= var2; var8++) {
         for (int var9 = -var2; var9 <= var2; var9++) {
            for (int var10 = -var2; var10 <= var2; var10++) {
               BlockPos var11 = var4.method_10069(var8, var9, var10);
               if (var4.method_10262(var11) <= var6 && var3.field_1687.method_8320(var11).method_26204() == var1) {
                  var5.add(var11);
               }
            }
         }
      }

      return var5.isEmpty() ? null : (BlockPos)var5.get(new Random().nextInt(var5.size()));
   }

   private int getSpawnerCount() {
      return countInInventory(Blocks.field_10260);
   }

   private static int countInInventory(Block var0) {
      ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
      int var2 = 0;

      for (int var3 = 0; var3 < 36; var3++) {
         ItemStack var4 = var1.method_31548().method_5438(var3);
         if (var4.method_7909() == var0.method_8389()) {
            var2 += var4.method_7947();
         }
      }

      return var2;
   }

   private ItemEntity findDroppedSpawner() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      ItemEntity var2 = null;
      float var3 = 3.4028235E38F;

      for (Entity var5 : var1.field_1687.method_18112()) {
         float var6;
         ItemEntity var7;
         if (var5 instanceof ItemEntity
            && (var7 = (ItemEntity)var5).method_6983().method_7909() == Blocks.field_10260.method_8389()
            && !((var6 = var1.field_1724.method_5739(var7)) >= 16.0F)
            && !var7.method_6977()
            && var6 < var3) {
            var3 = var6;
            var2 = var7;
         }
      }

      return var2;
   }

   private void stopBreaking() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      var1.field_1690.field_1886.method_23481(false);
      if (var1.field_1761 != null) {
         var1.field_1761.method_2925();
      }

      this.breakCooldown = 0;
   }

   private void stopMovement() {
      this.stopBreaking();
      MinecraftClient.method_1551().field_1690.field_1894.method_23481(false);
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
