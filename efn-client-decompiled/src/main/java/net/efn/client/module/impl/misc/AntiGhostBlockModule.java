package net.efn.client.module.impl.misc;

import com.example.client.mixin.AccessorNetheriteClientLevel;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class AntiGhostBlockModule extends ClientModule {
   private static final int MAX_PENDING = 256;
   private static AntiGhostBlockModule instance;
   private final Map<Long, AntiGhostBlockModule.Pending> pending = new LinkedHashMap<>();
   private ClientWorld world;

   public AntiGhostBlockModule() {
      super(
         "anti-ghost-block",
         "Anti Ghost Block",
         "Stops invisible ghost blocks when mining: a broken block stays visible until the server confirms the break.",
         ModuleCategory.MISC
      );
      instance = this;
   }

   @Override
   protected void onEnable() {
      this.pending.clear();
      this.world = MinecraftClient.getInstance().world;
   }

   @Override
   protected void onDisable() {
      this.pending.clear();
      this.world = null;
   }

   private static AntiGhostBlockModule active(ClientWorld clientWorld) {
      AntiGhostBlockModule var1 = instance;
      if (var1 != null && var1.isEnabled()) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var1.world != var2.world) {
            var1.pending.clear();
            var1.world = var2.world;
         }

         return clientWorld != null && clientWorld == var2.world && var2.player != null && var2.getNetworkHandler() != null && !var2.isInSingleplayer()
            ? var1
            : null;
      } else {
         return null;
      }
   }

   private static long timeoutNanos(MinecraftClient minecraftClient) {
      PlayerListEntry var1 = minecraftClient.getNetworkHandler() != null && minecraftClient.player != null
         ? minecraftClient.getNetworkHandler().getPlayerListEntry(minecraftClient.player.getUuid())
         : null;
      long var2 = var1 == null ? 0L : Math.max(0, var1.getLatency());
      return Math.max(2000L, var2 * 3L + 500L) * 1000000L;
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      AntiGhostBlockModule var3 = active(var2.world);
      if (var3 != null) {
         long var4 = System.nanoTime();
         long var6 = timeoutNanos(var2);
         var3.pending.values().removeIf(pending -> var4 - pending.since >= var6);
      }
   }

   public static boolean holdBreak(BlockPos blockPos) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      AntiGhostBlockModule var2 = active(var1.world);
      if (var2 != null && !var1.world.getBlockState(blockPos).isAir()) {
         int var3 = ((AccessorNetheriteClientLevel)var1.world).efn$blockStatePredictionHandler().getSequence();
         long var4 = blockPos.asLong();
         if (!var2.pending.containsKey(var4)) {
            if (var2.pending.size() >= 256) {
               Iterator var6 = var2.pending.keySet().iterator();
               var6.next();
               var6.remove();
            }

            var2.pending.put(var4, new AntiGhostBlockModule.Pending(var3, System.nanoTime()));
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean waiting(BlockPos blockPos) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      AntiGhostBlockModule var2 = active(var1.world);
      if (var2 == null) {
         return false;
      } else {
         AntiGhostBlockModule.Pending var3 = var2.pending.get(blockPos.asLong());
         if (var3 == null) {
            return false;
         } else if (System.nanoTime() - var3.since < timeoutNanos(var1)) {
            return true;
         } else {
            var2.pending.remove(blockPos.asLong());
            return false;
         }
      }
   }

   public static boolean serverAnswered(ClientWorld clientWorld, BlockPos blockPos) {
      AntiGhostBlockModule var2 = active(clientWorld);
      return var2 != null && var2.pending.remove(blockPos.asLong()) != null;
   }

   public static void acknowledged(ClientWorld clientWorld, int i) {
      AntiGhostBlockModule var2 = active(clientWorld);
      if (var2 != null) {
         var2.pending.values().removeIf(pending -> pending.sequence <= i);
      }
   }

   @Environment(EnvType.CLIENT)
   private record Pending(int sequence, long since) {
   }
}
