package net.efn.client.module.impl.misc;

import java.util.Set;
import java.util.TreeMap;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.GoliathPickerScreen;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.util.donut.DonutGoliaths;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class GoliathRtpModule extends ClientModule {
   private static final int[] HOP = new int[]{1, 0, 0, 4, 3, 6, 5};
   private static final int SETTLE_TICKS = 15;
   private static final int RESEND_TICKS = 100;
   private static final double LANDED_SQ = 10000.0;
   private static Set<DonutGoliaths.Goliath> lastPick = Set.of();
   private Set<DonutGoliaths.Goliath> targets;
   private String pending;
   private int sinceSent;
   private int settle;
   private int sent;
   private double lastX;
   private double lastZ;

   public GoliathRtpModule() {
      super(
         "goliath-rtp",
         "Goliath RTP",
         "Opens a map of every goliath: pick as many as you like and it uses /rtp until you land on one of them.",
         ModuleCategory.DONUT
      );
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.targets = null;
      this.pending = null;
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.player != null && var1.getNetworkHandler() != null) {
         var1.execute(() -> {
            if (this.isEnabled()) {
               var1.setScreen(new GoliathPickerScreen(lastPick, this::picked));
            }
         });
      } else {
         ModuleChat.send(this, ModuleChat.body().text("Join the server first."));
         this.requestDisable();
      }
   }

   @Override
   protected void onDisable() {
      this.targets = null;
      this.pending = null;
   }

   private void picked(Set<DonutGoliaths.Goliath> set) {
      if (this.isEnabled()) {
         if (set != null && !set.isEmpty()) {
            this.targets = set;
            lastPick = set;
            this.pending = null;
            this.settle = 0;
            this.sent = 0;
            ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
            if (var2 != null) {
               this.lastX = var2.getX();
               this.lastZ = var2.getZ();
            }

            if (set.size() == 1) {
               DonutGoliaths.Goliath var6 = (DonutGoliaths.Goliath)set.iterator().next();
               ModuleChat.send(this, ModuleChat.body().text("Going to ").name(var6.label()).text(" with /rtp " + DonutGoliaths.RTP[var6.region()] + "."));
            } else {
               TreeMap var3 = new TreeMap();

               for (DonutGoliaths.Goliath var5 : set) {
                  var3.merge(DonutGoliaths.REGIONS[var5.region()], 1, Integer::sum);
               }

               StringBuilder var7 = new StringBuilder();
               var3.forEach((string, integer) -> var7.append(var7.isEmpty() ? "" : ", ").append(string).append(' ').append(integer));
               ModuleChat.send(this, ModuleChat.body().text("Going to any of ").name(set.size() + " goliaths").text(" (" + var7 + ")."));
            }
         } else {
            this.requestDisable();
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var5 = MinecraftClient.getInstance();
      ClientPlayerEntity var6 = var5.player;
      if (this.targets != null && var6 != null && var5.world != null && var5.getNetworkHandler() != null) {
         double var7 = var6.getX() - this.lastX;
         double var3;
         boolean var9 = var7 * var7 + (var3 = var6.getZ() - this.lastZ) * var3 > 10000.0;
         this.lastX = var6.getX();
         this.lastZ = var6.getZ();
         if (this.pending != null) {
            if (!var9) {
               if (++this.sinceSent >= 100) {
                  this.send(var5, this.pending);
               }

               return;
            }

            this.pending = null;
            this.settle = 15;
         }

         DonutGoliaths.Goliath var2;
         if ((var2 = here(var5)) != null && this.targets.contains(var2)) {
            ModuleChat.send(this, ModuleChat.body().text("Landed on ").name(var2.label()).text(" after " + this.sent + " /rtp."));
            this.requestDisable();
         } else if (this.settle > 0) {
            this.settle--;
         } else {
            int var10 = var2 == null ? -1 : var2.region();
            int var11 = this.bestRegion(var10);
            if (var11 < 0) {
               var11 = var10 >= 0 ? HOP[var10] : HOP[0];
            }

            this.send(var5, DonutGoliaths.RTP[var11]);
         }
      }
   }

   private int bestRegion(int i) {
      int[] var2 = new int[DonutGoliaths.REGIONS.length];

      for (DonutGoliaths.Goliath var4 : this.targets) {
         int var5 = var4.region();
         var2[var5]++;
      }

      int var9 = -1;
      double var10 = 0.0;

      for (int var6 = 0; var6 < var2.length; var6++) {
         double var7;
         if (var6 != i && var2[var6] != 0 && DonutGoliaths.RTP[var6] != null && (var7 = (double)var2[var6] / DonutGoliaths.CELLS[var6].length) > var10) {
            var9 = var6;
            var10 = var7;
         }
      }

      return var9;
   }

   private void send(MinecraftClient minecraftClient, String string) {
      minecraftClient.getNetworkHandler().sendChatCommand("rtp " + string);
      this.pending = string;
      this.sinceSent = 0;
      this.sent++;
   }

   private static DonutGoliaths.Goliath here(MinecraftClient minecraftClient) {
      if (minecraftClient.world.getRegistryKey() == World.END) {
         return null;
      } else {
         double var1 = minecraftClient.world.getRegistryKey() == World.NETHER ? 8.0 : 1.0;
         return DonutGoliaths.at(minecraftClient.player.getX() * var1, minecraftClient.player.getZ() * var1);
      }
   }
}
