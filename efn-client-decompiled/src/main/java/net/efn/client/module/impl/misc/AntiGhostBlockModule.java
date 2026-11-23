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
      this.world = MinecraftClient.method_1551().field_1687;
   }

   @Override
   protected void onDisable() {
      this.pending.clear();
      this.world = null;
   }

   private static AntiGhostBlockModule active(ClientWorld var0) {
      AntiGhostBlockModule var1 = instance;
      if (var1 != null && var1.isEnabled()) {
         MinecraftClient var2 = MinecraftClient.method_1551();
         if (var1.world != var2.field_1687) {
            var1.pending.clear();
            var1.world = var2.field_1687;
         }

         return var0 != null && var0 == var2.field_1687 && var2.field_1724 != null && var2.method_1562() != null && !var2.method_1542() ? var1 : null;
      } else {
         return null;
      }
   }

   private static long timeoutNanos(MinecraftClient var0) {
      PlayerListEntry var1 = var0.method_1562() != null && var0.field_1724 != null ? var0.method_1562().method_2871(var0.field_1724.method_5667()) : null;
      long var2 = var1 == null ? 0L : Math.max(0, var1.method_2959());
      return Math.max(2000L, var2 * 3L + 500L) * 1000000L;
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      AntiGhostBlockModule var3 = active(var2.field_1687);
      if (var3 != null) {
         long var4 = System.nanoTime();
         long var6 = timeoutNanos(var2);
         var3.pending.values().removeIf(var4x -> var4 - var4x.since >= var6);
      }
   }

   public static boolean holdBreak(BlockPos var0) {
      MinecraftClient var1 = MinecraftClient.method_1551();
      AntiGhostBlockModule var2 = active(var1.field_1687);
      if (var2 != null && !var1.field_1687.method_8320(var0).method_26215()) {
         int var3 = ((AccessorNetheriteClientLevel)var1.field_1687).efn$blockStatePredictionHandler().method_41942();
         long var4 = var0.method_10063();
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

   public static boolean waiting(BlockPos var0) {
      MinecraftClient var1 = MinecraftClient.method_1551();
      AntiGhostBlockModule var2 = active(var1.field_1687);
      if (var2 == null) {
         return false;
      } else {
         AntiGhostBlockModule.Pending var3 = var2.pending.get(var0.method_10063());
         if (var3 == null) {
            return false;
         } else if (System.nanoTime() - var3.since < timeoutNanos(var1)) {
            return true;
         } else {
            var2.pending.remove(var0.method_10063());
            return false;
         }
      }
   }

   public static boolean serverAnswered(ClientWorld var0, BlockPos var1) {
      AntiGhostBlockModule var2 = active(var0);
      return var2 != null && var2.pending.remove(var1.method_10063()) != null;
   }

   public static void acknowledged(ClientWorld var0, int var1) {
      AntiGhostBlockModule var2 = active(var0);
      if (var2 != null) {
         var2.pending.values().removeIf(var1x -> var1x.sequence <= var1);
      }
   }

   @Environment(EnvType.CLIENT)
   private record Pending(int sequence, long since) {
   }
}
