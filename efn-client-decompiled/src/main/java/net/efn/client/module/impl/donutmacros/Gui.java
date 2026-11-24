package net.efn.client.module.impl.donutmacros;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@Environment(EnvType.CLIENT)
final class Gui {
   private static final int COMMAND_GAP_MS = 200;
   final MacroWorker worker;

   Gui(MacroWorker macroWorker) {
      this.worker = macroWorker;
   }

   static MinecraftClient mc() {
      return MinecraftClient.getInstance();
   }

   static ClientPlayerEntity player() {
      ClientPlayerEntity var0 = mc().player;
      if (var0 == null) {
         throw new IllegalStateException("Not in a world");
      } else {
         return var0;
      }
   }

   String title() {
      return this.worker.call(() -> mc().currentScreen == null ? "" : ServerText.normalize(mc().currentScreen.getTitle()));
   }

   static boolean containerOpenNow() {
      ClientPlayerEntity var0 = mc().player;
      return var0 != null && var0.currentScreenHandler != var0.playerScreenHandler && mc().currentScreen instanceof HandledScreen;
   }

   static List<ItemStack> containerNow() {
      ArrayList var0 = new ArrayList();
      ClientPlayerEntity var1 = mc().player;
      if (var1 != null && var1.currentScreenHandler != var1.playerScreenHandler) {
         for (Slot var3 : var1.currentScreenHandler.slots) {
            if (var3.inventory instanceof PlayerInventory) {
               break;
            }

            var0.add(var3.getStack().copy());
         }

         return var0;
      } else {
         return var0;
      }
   }

   List<ItemStack> container() {
      return this.worker.call(Gui::containerNow);
   }

   static String fingerprintNow() {
      StringBuilder var0 = new StringBuilder();
      ClientPlayerEntity var1 = mc().player;
      if (var1 != null) {
         var0.append(var1.currentScreenHandler.syncId).append('#');
      }

      for (ItemStack var3 : containerNow()) {
         var0.append(Registries.ITEM.getId(var3.getItem())).append(':').append(var3.getCount()).append(':').append(var3.getName().getString()).append('|');
      }

      return var0.toString();
   }

   String fingerprint() {
      return this.worker.call(Gui::fingerprintNow);
   }

   static ItemStack slotNow(int i) {
      List var1 = containerNow();
      return i >= 0 && i < var1.size() ? (ItemStack)var1.get(i) : ItemStack.EMPTY;
   }

   static int findNow(Predicate<ItemStack> predicate, int i) {
      List var2 = containerNow();

      for (int var3 = 0; var3 < var2.size() && var3 < i; var3++) {
         if (!((ItemStack)var2.get(var3)).isEmpty() && predicate.test((ItemStack)var2.get(var3))) {
            return var3;
         }
      }

      return -1;
   }

   static String nameOf(ItemStack itemStack) {
      return ServerText.normalize(itemStack.getName());
   }

   void click(int i, int j, SlotActionType slotActionType) {
      this.worker.run(() -> {
         ClientPlayerEntity var3 = player();
         ScreenHandler var4 = var3.currentScreenHandler;
         mc().interactionManager.clickSlot(var4.syncId, i, j, slotActionType, var3);
      });
   }

   void clickInventory(int i, int j, SlotActionType slotActionType) {
      this.worker.run(() -> {
         ClientPlayerEntity var3 = player();
         mc().interactionManager.clickSlot(var3.playerScreenHandler.syncId, i, j, slotActionType, var3);
      });
   }

   void closeInventory() {
      this.worker.run(() -> {
         ClientPlayerEntity var0 = player();
         if (var0.currentScreenHandler == var0.playerScreenHandler) {
            var0.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(var0.playerScreenHandler.syncId));
         }
      });
   }

   void command(String string) {
      this.worker.run(() -> player().networkHandler.sendChatCommand(string));
      this.worker.sleep(200L);
   }

   void closeAll() {
      for (int var1 = 0; var1 < 4; var1++) {
         boolean var2 = this.worker.call(() -> {
            ClientPlayerEntity var0 = mc().player;
            if (var0 == null) {
               return false;
            } else if (var0.currentScreenHandler != var0.playerScreenHandler) {
               var0.closeHandledScreen();
               return true;
            } else if (mc().currentScreen != null && !(mc().currentScreen instanceof DeathScreen)) {
               mc().currentScreen.close();
               return true;
            } else {
               return false;
            }
         });
         if (!var2) {
            return;
         }

         this.worker.ticks(2);
      }
   }

   boolean waitChange(String string, long l) {
      return this.worker.waitFor(() -> !fingerprintNow().equals(string), l);
   }
}
