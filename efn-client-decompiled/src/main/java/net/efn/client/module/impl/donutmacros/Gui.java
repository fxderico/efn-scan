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

   Gui(MacroWorker var1) {
      this.worker = var1;
   }

   static MinecraftClient mc() {
      return MinecraftClient.method_1551();
   }

   static ClientPlayerEntity player() {
      ClientPlayerEntity var0 = mc().field_1724;
      if (var0 == null) {
         throw new IllegalStateException("Not in a world");
      } else {
         return var0;
      }
   }

   String title() {
      return this.worker.call(() -> mc().field_1755 == null ? "" : ServerText.normalize(mc().field_1755.method_25440()));
   }

   static boolean containerOpenNow() {
      ClientPlayerEntity var0 = mc().field_1724;
      return var0 != null && var0.field_7512 != var0.field_7498 && mc().field_1755 instanceof HandledScreen;
   }

   static List<ItemStack> containerNow() {
      ArrayList var0 = new ArrayList();
      ClientPlayerEntity var1 = mc().field_1724;
      if (var1 != null && var1.field_7512 != var1.field_7498) {
         for (Slot var3 : var1.field_7512.field_7761) {
            if (var3.field_7871 instanceof PlayerInventory) {
               break;
            }

            var0.add(var3.method_7677().method_7972());
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
      ClientPlayerEntity var1 = mc().field_1724;
      if (var1 != null) {
         var0.append(var1.field_7512.field_7763).append('#');
      }

      for (ItemStack var3 : containerNow()) {
         var0.append(Registries.field_41178.method_10221(var3.method_7909()))
            .append(':')
            .append(var3.method_7947())
            .append(':')
            .append(var3.method_7964().getString())
            .append('|');
      }

      return var0.toString();
   }

   String fingerprint() {
      return this.worker.call(Gui::fingerprintNow);
   }

   static ItemStack slotNow(int var0) {
      List var1 = containerNow();
      return var0 >= 0 && var0 < var1.size() ? (ItemStack)var1.get(var0) : ItemStack.field_8037;
   }

   static int findNow(Predicate<ItemStack> var0, int var1) {
      List var2 = containerNow();

      for (int var3 = 0; var3 < var2.size() && var3 < var1; var3++) {
         if (!((ItemStack)var2.get(var3)).method_7960() && var0.test((ItemStack)var2.get(var3))) {
            return var3;
         }
      }

      return -1;
   }

   static String nameOf(ItemStack var0) {
      return ServerText.normalize(var0.method_7964());
   }

   void click(int var1, int var2, SlotActionType var3) {
      this.worker.run(() -> {
         ClientPlayerEntity var3x = player();
         ScreenHandler var4 = var3x.field_7512;
         mc().field_1761.method_2906(var4.field_7763, var1, var2, var3, var3x);
      });
   }

   void clickInventory(int var1, int var2, SlotActionType var3) {
      this.worker.run(() -> {
         ClientPlayerEntity var3x = player();
         mc().field_1761.method_2906(var3x.field_7498.field_7763, var1, var2, var3, var3x);
      });
   }

   void closeInventory() {
      this.worker.run(() -> {
         ClientPlayerEntity var0 = player();
         if (var0.field_7512 == var0.field_7498) {
            var0.field_3944.method_52787(new CloseHandledScreenC2SPacket(var0.field_7498.field_7763));
         }
      });
   }

   void command(String var1) {
      this.worker.run(() -> player().field_3944.method_45730(var1));
      this.worker.sleep(200L);
   }

   void closeAll() {
      for (int var1 = 0; var1 < 4; var1++) {
         boolean var2 = this.worker.call(() -> {
            ClientPlayerEntity var0 = mc().field_1724;
            if (var0 == null) {
               return false;
            } else if (var0.field_7512 != var0.field_7498) {
               var0.method_7346();
               return true;
            } else if (mc().field_1755 != null && !(mc().field_1755 instanceof DeathScreen)) {
               mc().field_1755.method_25419();
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

   boolean waitChange(String var1, long var2) {
      return this.worker.waitFor(() -> !fingerprintNow().equals(var1), var2);
   }
}
