package net.efn.client.module.impl.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.totem.OffhandRefill;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.world.GameMode;

@Environment(EnvType.CLIENT)
public final class AutoTotemModule extends ClientModule {
   private static final int OFFHAND_BUTTON = 40;
   private static final int OFFHAND_MENU_SLOT = 45;
   private static final int TOTEM_POP_EVENT = 35;
   private static final int NEAREST_CHOICES = 3;
   private static AutoTotemModule instance;
   private final NumberSetting refillDelay = this.addSetting("refill-delay", new NumberSetting("Refill Delay", 3.0F, 1.0F, 10.0F).step(1.0F));
   private final BooleanSetting useHotbar = this.addSetting("use-hotbar-totems", new BooleanSetting("Use Hotbar Totems", false));
   private final OffhandRefill refill = new OffhandRefill(
      new OffhandRefill.Driver() {
         @Override
         public boolean needsTotem() {
            return AutoTotemModule.player().getOffHandStack().isEmpty();
         }

         @Override
         public boolean canInteract() {
            MinecraftClient var1 = MinecraftClient.getInstance();
            ClientPlayerEntity var2 = var1.player;
            return var1.interactionManager.getCurrentGameMode() == GameMode.SURVIVAL
               && var2.currentScreenHandler == var2.playerScreenHandler
               && var2.playerScreenHandler.getCursorStack().isEmpty();
         }

         @Override
         public int findSource() {
            return AutoTotemModule.this.findSource(AutoTotemModule.player().getInventory());
         }

         @Override
         public boolean validSource(int i) {
            ClientPlayerEntity var2 = AutoTotemModule.player();
            if (!AutoTotemModule.isSource(var2.getInventory(), i, AutoTotemModule.this.useHotbar.get())) {
               return false;
            } else {
               Slot var3 = var2.playerScreenHandler.getSlot(AutoTotemModule.menuSlot(i));
               return var3.inventory == var2.getInventory() && var3.getIndex() == i && var3.canTakeItems(var2);
            }
         }

         @Override
         public void swap(int i) {
            MinecraftClient var2 = MinecraftClient.getInstance();
            ClientPlayerEntity var3 = var2.player;
            var2.interactionManager.clickSlot(var3.playerScreenHandler.syncId, AutoTotemModule.menuSlot(i), 40, SlotActionType.SWAP, var3);
            AutoTotemModule.this.previousSource = i;
         }

         @Override
         public int containerId() {
            return AutoTotemModule.player().playerScreenHandler.syncId;
         }

         @Override
         public int stateId() {
            return AutoTotemModule.player().playerScreenHandler.getRevision();
         }

         @Override
         public int latencyMillis() {
            ClientPlayNetworkHandler var1 = MinecraftClient.getInstance().getNetworkHandler();
            PlayerListEntry var2 = var1 == null ? null : var1.getPlayerListEntry(AutoTotemModule.player().getUuid());
            return var2 == null ? 0 : var2.getLatency();
         }
      },
      ThreadLocalRandom.current()
   );
   private int previousSource = -1;

   public AutoTotemModule() {
      super(
         "autototem",
         "Auto Totem",
         "Refills an empty offhand with a totem from your inventory, at a human pace you choose with Refill Delay.",
         ModuleCategory.COMBAT
      );
      instance = this;
   }

   public static boolean wantsHotbar() {
      return false;
   }

   public static boolean busy() {
      AutoTotemModule var0 = instance;
      ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
      return var0 != null && var0.isEnabled() && var1 != null && !var1.isSpectator() && !var1.isInCreativeMode()
         ? var0.refill.busy() || var1.getOffHandStack().isEmpty() && var0.findSource(var1.getInventory()) >= 0
         : false;
   }

   @Override
   protected void onEnable() {
      AutoInvTotemModule var1 = ModuleRegistry.get(AutoInvTotemModule.class);
      if (var1 != null) {
         var1.setEnabled(false);
      }

      this.reset();
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   @EventHandler(
      stage = Stage.POST
   )
   public void onTick(TickEvent tickEvent) {
      this.update();
   }

   @EventHandler
   public void onRender2D(Render2DEvent render2DEvent) {
      this.update();
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent packetAppliedEvent) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3.world != null && var3.player != null) {
         Packet var4 = packetAppliedEvent.packet;
         EntityStatusS2CPacket var2;
         if (var4 instanceof EntityStatusS2CPacket && (var2 = (EntityStatusS2CPacket)var4).getStatus() == 35 && var2.getEntity(var3.world) == var3.player) {
            this.reset();
         }
      }
   }

   @EventHandler
   public void onPacketReceiveNetty(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      long var2 = System.nanoTime();
      if (packetReceiveNettyEvent.packet instanceof SetPlayerInventoryS2CPacket var5) {
         if (var5.slot() == 40) {
            this.observe(-1, -1, offhand(var5.contents()), var2);
         }
      } else if (packetReceiveNettyEvent.packet instanceof ScreenHandlerSlotUpdateS2CPacket var8) {
         if (var8.getSlot() == 45) {
            this.observe(var8.getSyncId(), var8.getRevision(), offhand(var8.getStack()), var2);
         }
      } else {
         Packet var7 = packetReceiveNettyEvent.packet;
         InventoryS2CPacket var9;
         if (var7 instanceof InventoryS2CPacket && (var9 = (InventoryS2CPacket)var7).contents().size() > 45) {
            this.observe(var9.syncId(), var9.revision(), offhand(var9.contents().get(45)), var2);
         }
      }
   }

   private void observe(int i, int j, OffhandRefill.Offhand offhand, long l) {
      MinecraftClient.getInstance().execute(() -> {
         if (this.isEnabled()) {
            this.refill.observe(i, j, offhand, l);
         }
      });
   }

   private void update() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      ClientPlayerEntity var2 = var1.player;
      if (var1.world != null && var2 != null && var1.interactionManager != null && var2.isAlive() && !var2.isRemoved() && !(var2.getHealth() <= 0.0F)) {
         this.refill.step(System.nanoTime(), this.refillDelay.getInt());
      } else {
         this.reset();
      }
   }

   private void reset() {
      this.refill.reset();
      this.previousSource = -1;
   }

   private int findSource(PlayerInventory playerInventory) {
      ThreadLocalRandom var3 = ThreadLocalRandom.current();
      if (this.useHotbar.get()) {
         ArrayList var4 = new ArrayList();

         for (int var2 = 0; var2 < 9; var2++) {
            if (isSource(playerInventory, var2, true)) {
               var4.add(var2);
            }
         }

         if (!var4.isEmpty()) {
            return (Integer)var4.get(var3.nextInt(var4.size()));
         }
      }

      ArrayList var7 = new ArrayList();

      for (int var6 = 9; var6 < 36; var6++) {
         if (isSource(playerInventory, var6, false)) {
            var7.add(var6);
         }
      }

      if (var7.isEmpty()) {
         return -1;
      } else {
         int var5 = this.previousSource >= 9 && this.previousSource < 36 ? this.previousSource - 9 : var3.nextInt(27);
         var7.sort(Comparator.comparingInt(integer -> {
            int var2x = integer - 9;
            return Math.abs(var2x % 9 - var5 % 9) + Math.abs(var2x / 9 - var5 / 9);
         }));
         return (Integer)var7.get(var3.nextInt(Math.min(3, var7.size())));
      }
   }

   private static boolean isSource(PlayerInventory playerInventory, int i, boolean bl) {
      return i >= (bl ? 0 : 9) && i < 36 && playerInventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING);
   }

   private static int menuSlot(int i) {
      return i < 9 ? i + 36 : i;
   }

   private static OffhandRefill.Offhand offhand(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return OffhandRefill.Offhand.EMPTY;
      } else {
         return itemStack.isOf(Items.TOTEM_OF_UNDYING) ? OffhandRefill.Offhand.TOTEM : OffhandRefill.Offhand.OTHER;
      }
   }

   private static ClientPlayerEntity player() {
      return MinecraftClient.getInstance().player;
   }
}
