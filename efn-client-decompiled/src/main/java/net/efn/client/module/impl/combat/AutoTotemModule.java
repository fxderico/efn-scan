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
   private final OffhandRefill refill = new OffhandRefill(new OffhandRefill.Driver() {
      @Override
      public boolean needsTotem() {
         return AutoTotemModule.player().method_6079().method_7960();
      }

      @Override
      public boolean canInteract() {
         MinecraftClient var1 = MinecraftClient.method_1551();
         ClientPlayerEntity var2 = var1.field_1724;
         return var1.field_1761.method_2920() == GameMode.field_9215 && var2.field_7512 == var2.field_7498 && var2.field_7498.method_34255().method_7960();
      }

      @Override
      public int findSource() {
         return AutoTotemModule.this.findSource(AutoTotemModule.player().method_31548());
      }

      @Override
      public boolean validSource(int var1) {
         ClientPlayerEntity var2 = AutoTotemModule.player();
         if (!AutoTotemModule.isSource(var2.method_31548(), var1, AutoTotemModule.this.useHotbar.get())) {
            return false;
         } else {
            Slot var3 = var2.field_7498.method_7611(AutoTotemModule.menuSlot(var1));
            return var3.field_7871 == var2.method_31548() && var3.method_34266() == var1 && var3.method_7674(var2);
         }
      }

      @Override
      public void swap(int var1) {
         MinecraftClient var2 = MinecraftClient.method_1551();
         ClientPlayerEntity var3 = var2.field_1724;
         var2.field_1761.method_2906(var3.field_7498.field_7763, AutoTotemModule.menuSlot(var1), 40, SlotActionType.field_7791, var3);
         AutoTotemModule.this.previousSource = var1;
      }

      @Override
      public int containerId() {
         return AutoTotemModule.player().field_7498.field_7763;
      }

      @Override
      public int stateId() {
         return AutoTotemModule.player().field_7498.method_37421();
      }

      @Override
      public int latencyMillis() {
         ClientPlayNetworkHandler var1 = MinecraftClient.method_1551().method_1562();
         PlayerListEntry var2 = var1 == null ? null : var1.method_2871(AutoTotemModule.player().method_5667());
         return var2 == null ? 0 : var2.method_2959();
      }
   }, ThreadLocalRandom.current());
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
      ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
      return var0 != null && var0.isEnabled() && var1 != null && !var1.method_7325() && !var1.method_56992()
         ? var0.refill.busy() || var1.method_6079().method_7960() && var0.findSource(var1.method_31548()) >= 0
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
   public void onTick(TickEvent var1) {
      this.update();
   }

   @EventHandler
   public void onRender2D(Render2DEvent var1) {
      this.update();
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent var1) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      if (var3.field_1687 != null && var3.field_1724 != null) {
         Packet var4 = var1.packet;
         EntityStatusS2CPacket var2;
         if (var4 instanceof EntityStatusS2CPacket
            && (var2 = (EntityStatusS2CPacket)var4).method_11470() == 35
            && var2.method_11469(var3.field_1687) == var3.field_1724) {
            this.reset();
         }
      }
   }

   @EventHandler
   public void onPacketReceiveNetty(PacketReceiveNettyEvent var1) {
      long var2 = System.nanoTime();
      if (var1.packet instanceof SetPlayerInventoryS2CPacket var5) {
         if (var5.comp_2891() == 40) {
            this.observe(-1, -1, offhand(var5.comp_2892()), var2);
         }
      } else if (var1.packet instanceof ScreenHandlerSlotUpdateS2CPacket var8) {
         if (var8.method_11450() == 45) {
            this.observe(var8.method_11452(), var8.method_37439(), offhand(var8.method_11449()), var2);
         }
      } else {
         Packet var7 = var1.packet;
         InventoryS2CPacket var9;
         if (var7 instanceof InventoryS2CPacket && (var9 = (InventoryS2CPacket)var7).comp_3839().size() > 45) {
            this.observe(var9.comp_3837(), var9.comp_3838(), offhand((ItemStack)var9.comp_3839().get(45)), var2);
         }
      }
   }

   private void observe(int var1, int var2, OffhandRefill.Offhand var3, long var4) {
      MinecraftClient.method_1551().execute(() -> {
         if (this.isEnabled()) {
            this.refill.observe(var1, var2, var3, var4);
         }
      });
   }

   private void update() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      ClientPlayerEntity var2 = var1.field_1724;
      if (var1.field_1687 != null && var2 != null && var1.field_1761 != null && var2.method_5805() && !var2.method_31481() && !(var2.method_6032() <= 0.0F)) {
         this.refill.step(System.nanoTime(), this.refillDelay.getInt());
      } else {
         this.reset();
      }
   }

   private void reset() {
      this.refill.reset();
      this.previousSource = -1;
   }

   private int findSource(PlayerInventory var1) {
      ThreadLocalRandom var3 = ThreadLocalRandom.current();
      if (this.useHotbar.get()) {
         ArrayList var4 = new ArrayList();

         for (int var2 = 0; var2 < 9; var2++) {
            if (isSource(var1, var2, true)) {
               var4.add(var2);
            }
         }

         if (!var4.isEmpty()) {
            return (Integer)var4.get(var3.nextInt(var4.size()));
         }
      }

      ArrayList var7 = new ArrayList();

      for (int var6 = 9; var6 < 36; var6++) {
         if (isSource(var1, var6, false)) {
            var7.add(var6);
         }
      }

      if (var7.isEmpty()) {
         return -1;
      } else {
         int var5 = this.previousSource >= 9 && this.previousSource < 36 ? this.previousSource - 9 : var3.nextInt(27);
         var7.sort(Comparator.comparingInt(var1x -> {
            int var2x = var1x - 9;
            return Math.abs(var2x % 9 - var5 % 9) + Math.abs(var2x / 9 - var5 / 9);
         }));
         return (Integer)var7.get(var3.nextInt(Math.min(3, var7.size())));
      }
   }

   private static boolean isSource(PlayerInventory var0, int var1, boolean var2) {
      return var1 >= (var2 ? 0 : 9) && var1 < 36 && var0.method_5438(var1).method_31574(Items.field_8288);
   }

   private static int menuSlot(int var0) {
      return var0 < 9 ? var0 + 36 : var0;
   }

   private static OffhandRefill.Offhand offhand(ItemStack var0) {
      if (var0.method_7960()) {
         return OffhandRefill.Offhand.EMPTY;
      } else {
         return var0.method_31574(Items.field_8288) ? OffhandRefill.Offhand.TOTEM : OffhandRefill.Offhand.OTHER;
      }
   }

   private static ClientPlayerEntity player() {
      return MinecraftClient.method_1551().field_1724;
   }
}
