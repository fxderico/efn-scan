package net.efn.client.module.impl.misc;

import com.example.client.mixin.AccessorNetheriteClientLevel;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.efn.client.chat.ModuleChat;
import net.efn.client.util.netherite.NetheriteChunkTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;

@Environment(EnvType.CLIENT)
final class AdvancedFinder {
   private static final double REACH = 5.0;
   private static final double PROBES_PER_TICK = 18.0;
   private static final double BURST = 600.0;
   private static final int RECOUNT_INTERVAL_TICKS = 10;
   private static final int MAX_REVEALED = 256;
   private final NetheriteFinderModule module;
   private final Map<NetheriteChunkTracker.Candidate, AdvancedFinder.Scan> scans = new HashMap<>();
   private final Set<NetheriteChunkTracker.Candidate> done = new HashSet<>();
   private final Set<BlockPos> revealed = new LinkedHashSet<>();
   private boolean armed;
   private int ticks;
   private double budget;
   private int recountCooldown;
   private static final int MISSING_REFRESH_TICKS = 10;
   private int handWarning;
   private String lastReason = "";
   private ClientWorld level;

   AdvancedFinder(NetheriteFinderModule var1) {
      this.module = var1;
   }

   boolean armed() {
      return this.armed;
   }

   Collection<BlockPos> revealed() {
      return this.revealed;
   }

   boolean isDone(NetheriteChunkTracker.Candidate var1) {
      return this.done.contains(var1);
   }

   float progress(NetheriteChunkTracker.Candidate var1) {
      AdvancedFinder.Scan var2 = this.armed ? this.scans.get(var1) : null;
      return var2 == null ? -1.0F : fraction(var2);
   }

   private static float fraction(AdvancedFinder.Scan var0) {
      return var0.total == 0 ? 1.0F : Math.min(1.0F, (float)var0.sent / var0.total);
   }

   void forEachProbe(Vec3d var1, double var2, int var4, float var5, AdvancedFinder.ProbeVisitor var6) {
      if (this.armed) {
         double var7 = var2 * var2;
         int var9 = 0;

         for (AdvancedFinder.Scan var11 : this.scans.values()) {
            if (!(fraction(var11) >= var5)) {
               int var12 = var11.section.chunkX() << 4;
               int var13 = var11.section.sectionY() << 4;
               int var14 = var11.section.chunkZ() << 4;

               for (int var15 = 0; var15 < 4096; var15++) {
                  int var25 = var11.stamp[var15];
                  int var16;
                  double var17;
                  int var19;
                  double var20;
                  int var22;
                  double var23;
                  if (var25 != 0
                     && !(
                        (var23 = (var22 = var12 + (var15 & 15)) + 0.5 - var1.field_1352) * var23
                              + (var20 = (var19 = var13 + (var15 >> 8)) + 0.5 - var1.field_1351) * var20
                              + (var17 = (var16 = var14 + (var15 >> 4 & 15)) + 0.5 - var1.field_1350) * var17
                           > var7
                     )) {
                     var6.visit(var22, var19, var16, this.ticks - var25 + 1);
                     if (++var9 >= var4) {
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   void forEachMissing(Vec3d var1, double var2, int var4, float var5, AdvancedFinder.MissingVisitor var6) {
      if (this.armed) {
         double var7 = var2 * var2;
         int var9 = 0;

         for (AdvancedFinder.Scan var11 : this.scans.values()) {
            if (!(fraction(var11) < var5)) {
               this.refreshMissing(var11);
               int var12 = var11.section.chunkX() << 4;
               int var13 = var11.section.sectionY() << 4;
               int var14 = var11.section.chunkZ() << 4;

               for (int var15 = 0; var15 < var11.missingCount; var15++) {
                  int var25 = var11.missing[var15];
                  int var16;
                  double var17;
                  int var19;
                  double var20;
                  int var22;
                  double var23;
                  if (!var11.probed.get(var25)
                     && !(
                        (var23 = (var22 = var12 + (var25 & 15)) + 0.5 - var1.field_1352) * var23
                              + (var20 = (var19 = var13 + (var25 >> 8)) + 0.5 - var1.field_1351) * var20
                              + (var17 = (var16 = var14 + (var25 >> 4 & 15)) + 0.5 - var1.field_1350) * var17
                           > var7
                     )) {
                     var6.visit(var22, var19, var16);
                     if (++var9 >= var4) {
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private void refreshMissing(AdvancedFinder.Scan var1) {
      if (this.level != null && this.ticks - var1.missingAt >= 10) {
         var1.missingAt = this.ticks;
         int var2 = var1.section.chunkX() << 4;
         int var3 = var1.section.sectionY() << 4;
         int var4 = var1.section.chunkZ() << 4;
         Mutable var5 = new Mutable();
         int var6 = 0;

         for (int var7 = var1.probed.nextClearBit(0); var7 < 4096; var7 = var1.probed.nextClearBit(var7 + 1)) {
            var5.method_10103(var2 + (var7 & 15), var3 + (var7 >> 8), var4 + (var7 >> 4 & 15));
            if (worthProbing(this.level, var5)) {
               var1.missing[var6++] = var7;
            }
         }

         var1.missingCount = var6;
      }
   }

   void arm() {
      this.armed = true;
      this.lastReason = "";
   }

   void disarm(MinecraftClient var1) {
      this.armed = false;
      this.lastReason = "";
   }

   void reset(MinecraftClient var1) {
      this.disarm(var1);
      this.scans.clear();
      this.done.clear();
      this.revealed.clear();
      this.budget = 0.0;
      this.level = null;
   }

   boolean onRevealed(BlockPos var1) {
      NetheriteChunkTracker.Candidate var2 = new NetheriteChunkTracker.Candidate(var1.method_10263() >> 4, var1.method_10260() >> 4, var1.method_10264() >> 4);
      if (!this.revealed.add(var1)) {
         return false;
      } else {
         if (this.revealed.size() > 256) {
            Iterator var3 = this.revealed.iterator();
            var3.next();
            var3.remove();
         }

         if (this.armed && this.scans.remove(var2) != null) {
            this.done.add(var2);
            ModuleChat.send(
               this.module,
               ModuleChat.body().text("Ancient debris ").good("found").text(" at ").coords(var1.method_10263(), var1.method_10264(), var1.method_10260())
            );
         }

         return true;
      }
   }

   void maintain(MinecraftClient var1) {
      ClientWorld var2 = var1.field_1687;
      if (var2 == this.level) {
         if (var2 != null) {
            this.revealed.removeIf(var1x -> !var2.method_8320(var1x).method_27852(Blocks.field_22109));
         }
      } else {
         this.scans.clear();
         this.done.clear();
         this.revealed.clear();
         this.level = var2;
      }
   }

   void tick(MinecraftClient var1) {
      if (this.armed) {
         ClientPlayerEntity var2 = var1.field_1724;
         ClientWorld var3 = var1.field_1687;
         if (var2 != null && var3 != null && var2.field_3944 != null && var1.field_1761 != null) {
            this.ticks++;
            this.budget = Math.min(600.0, this.budget + 18.0);
            if (this.handWarning > 0) {
               this.handWarning--;
            }

            if (!NetheriteChunkTracker.isNether(var3)) {
               this.hold("Not in the Nether");
            } else {
               this.lastReason = "";
               if (var1.field_1755 == null && !var2.method_6115() && !var1.field_1761.method_2923()) {
                  Vec3d var4 = var2.method_33571();
                  this.refreshScans(var3, var4);
                  if (!this.scans.isEmpty() && !(this.budget < 1.0)) {
                     List var5 = this.collect(var3, var4);
                     if (!var5.isEmpty()) {
                        int var6 = var2.method_31548().method_67532();
                        int var7 = probeSlot(var2, var6);
                        if (var7 < 0) {
                           if (this.handWarning <= 0) {
                              this.handWarning = 200;
                              ModuleChat.send(this.module, ModuleChat.body().text("Hotbar needs an empty or non-tool slot"));
                           }
                        } else {
                           if (var7 != var6) {
                              var2.field_3944.method_52787(new UpdateSelectedSlotC2SPacket(var7));
                           }

                           BlockPos var8 = null;

                           for (AdvancedFinder.Probe var10 : var5) {
                              if (this.budget < 1.0) {
                                 break;
                              }

                              sendAction(var2, var3, Action.field_12968, var10.pos());
                              var10.scan().probed.set(var10.index());
                              var10.scan().sent++;
                              var10.scan().stamp[var10.index()] = this.ticks;
                              var10.scan().dirty = true;
                              this.budget--;
                              var8 = var10.pos();
                           }

                           if (var8 != null) {
                              sendAction(var2, var3, Action.field_12971, var8);
                           }

                           if (var7 != var6) {
                              var2.field_3944.method_52787(new UpdateSelectedSlotC2SPacket(var6));
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void refreshScans(ClientWorld var1, Vec3d var2) {
      HashSet var3 = new HashSet<>(NetheriteChunkTracker.snapshot());
      this.scans.keySet().retainAll(var3);

      for (NetheriteChunkTracker.Candidate var5 : var3) {
         if (!this.done.contains(var5) && !this.scans.containsKey(var5) && distanceToSectionSq(var2, var5) <= 25.0) {
            this.scans.put(var5, new AdvancedFinder.Scan(var5, countProbeable(var1, var5)));
         }
      }

      if (this.recountCooldown-- <= 0) {
         this.recountCooldown = 10;
         Iterator var7 = this.scans.values().iterator();

         while (var7.hasNext()) {
            AdvancedFinder.Scan var8 = (AdvancedFinder.Scan)var7.next();
            if (var8.dirty) {
               var8.dirty = false;
               if (!hasUnprobed(var1, var8)) {
                  var7.remove();
                  this.done.add(var8.section);
                  NetheriteChunkTracker.Candidate var6 = var8.section;
                  ModuleChat.send(
                     this.module,
                     ModuleChat.body()
                        .text("Section ")
                        .good("clean")
                        .text(" at ")
                        .coords(var6.chunkX() * 16 + 8, var6.sectionY() * 16 + 8, var6.chunkZ() * 16 + 8)
                  );
               }
            }
         }
      }
   }

   private List<AdvancedFinder.Probe> collect(ClientWorld var1, Vec3d var2) {
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      int var5 = (int)Math.ceil(5.0);
      int var6 = (int)Math.floor(var2.field_1352);
      int var7 = (int)Math.floor(var2.field_1351);
      int var8 = (int)Math.floor(var2.field_1350);
      Mutable var9 = new Mutable();

      for (AdvancedFinder.Scan var11 : this.scans.values()) {
         int var12 = var11.section.chunkX() << 4;
         int var13 = var11.section.sectionY() << 4;
         int var14 = var11.section.chunkZ() << 4;
         int var15 = Math.max(var12, var6 - var5);
         int var16 = Math.min(var12 + 15, var6 + var5);
         int var17 = Math.max(var13, var7 - var5);
         int var18 = Math.min(var13 + 15, var7 + var5);
         int var19 = Math.max(var14, var8 - var5);
         int var20 = Math.min(var14 + 15, var8 + var5);

         for (int var21 = var15; var21 <= var16; var21++) {
            for (int var22 = var17; var22 <= var18; var22++) {
               for (int var23 = var19; var23 <= var20; var23++) {
                  int var24 = var22 & 15;
                  int var25 = var23 & 15;
                  int var26 = var21 & 15;
                  int var27 = var24 << 8 | var25 << 4 | var26;
                  if (!var11.probed.get(var27)) {
                     var9.method_10103(var21, var22, var23);
                     double var28 = distanceToBlockSq(var2, var9);
                     if (!(var28 > 25.0)) {
                        if (!worthProbing(var1, var9)) {
                           var11.probed.set(var27);
                        } else {
                           AdvancedFinder.Probe var30 = new AdvancedFinder.Probe(var11, var27, var9.method_10062(), var28);
                           if (var26 % 3 == 1 && var24 % 3 == 1 && var25 % 3 == 1) {
                              var3.add(var30);
                           } else {
                              var4.add(var30);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      var3.sort(Comparator.comparingDouble(AdvancedFinder.Probe::distanceSq));
      var4.sort(Comparator.comparingDouble(AdvancedFinder.Probe::distanceSq));
      var3.addAll(var4);
      return var3;
   }

   private static int countProbeable(ClientWorld var0, NetheriteChunkTracker.Candidate var1) {
      int var2 = var1.chunkX() << 4;
      int var3 = var1.sectionY() << 4;
      int var4 = var1.chunkZ() << 4;
      Mutable var5 = new Mutable();
      int var6 = 0;

      for (int var7 = 0; var7 < 4096; var7++) {
         var5.method_10103(var2 + (var7 & 15), var3 + (var7 >> 8), var4 + (var7 >> 4 & 15));
         if (worthProbing(var0, var5)) {
            var6++;
         }
      }

      return var6;
   }

   private static boolean hasUnprobed(ClientWorld var0, AdvancedFinder.Scan var1) {
      int var2 = var1.section.chunkX() << 4;
      int var3 = var1.section.sectionY() << 4;
      int var4 = var1.section.chunkZ() << 4;
      Mutable var5 = new Mutable();

      for (int var6 = var1.probed.nextClearBit(0); var6 < 4096; var6 = var1.probed.nextClearBit(var6 + 1)) {
         var5.method_10103(var2 + (var6 & 15), var3 + (var6 >> 8), var4 + (var6 >> 4 & 15));
         if (worthProbing(var0, var5)) {
            return true;
         }
      }

      return false;
   }

   private static boolean worthProbing(ClientWorld var0, BlockPos var1) {
      BlockState var2 = var0.method_8320(var1);
      return !var2.method_26215() && var2.method_26227().method_15769() && !var2.method_27852(Blocks.field_22109)
         ? var2.method_26216() && var2.method_26214(var0, var1) > 0.0F
         : false;
   }

   private void hold(String var1) {
      if (!var1.equals(this.lastReason)) {
         this.lastReason = var1;
         ModuleChat.send(this.module, ModuleChat.body().text(var1));
      }
   }

   private static void sendAction(ClientPlayerEntity var0, ClientWorld var1, Action var2, BlockPos var3) {
      PendingUpdateManager var4 = ((AccessorNetheriteClientLevel)var1).efn$blockStatePredictionHandler();
      PendingUpdateManager var5 = var4.method_41937();

      try {
         var0.field_3944.method_52787(new PlayerActionC2SPacket(var2, var3, Direction.field_11036, var5.method_41942()));
      } catch (Throwable var9) {
         if (var5 != null) {
            try {
               var5.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (var5 != null) {
         var5.close();
      }
   }

   private static double distanceToSectionSq(Vec3d var0, NetheriteChunkTracker.Candidate var1) {
      double var2 = var1.chunkX() * 16.0;
      double var4 = var1.sectionY() * 16.0;
      double var6 = var1.chunkZ() * 16.0;
      double var8 = Math.max(0.0, Math.max(var2 - var0.field_1352, var0.field_1352 - (var2 + 16.0)));
      double var10 = Math.max(0.0, Math.max(var4 - var0.field_1351, var0.field_1351 - (var4 + 16.0)));
      double var12 = Math.max(0.0, Math.max(var6 - var0.field_1350, var0.field_1350 - (var6 + 16.0)));
      return var8 * var8 + var10 * var10 + var12 * var12;
   }

   private static double distanceToBlockSq(Vec3d var0, BlockPos var1) {
      double var2 = Math.max(0.0, Math.max(var1.method_10263() - var0.field_1352, var0.field_1352 - (var1.method_10263() + 1.0)));
      double var4 = Math.max(0.0, Math.max(var1.method_10264() - var0.field_1351, var0.field_1351 - (var1.method_10264() + 1.0)));
      double var6 = Math.max(0.0, Math.max(var1.method_10260() - var0.field_1350, var0.field_1350 - (var1.method_10260() + 1.0)));
      return var2 * var2 + var4 * var4 + var6 * var6;
   }

   private static int probeSlot(ClientPlayerEntity var0, int var1) {
      if (!isTool(var0.method_31548().method_5438(var1))) {
         return var1;
      } else {
         for (int var2 = 0; var2 < 9; var2++) {
            if (var0.method_31548().method_5438(var2).method_7960()) {
               return var2;
            }
         }

         for (int var3 = 0; var3 < 9; var3++) {
            if (!isTool(var0.method_31548().method_5438(var3))) {
               return var3;
            }
         }

         return -1;
      }
   }

   private static boolean isTool(ItemStack var0) {
      return var0.method_57826(DataComponentTypes.field_50077);
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface MissingVisitor {
      void visit(int var1, int var2, int var3);
   }

   @Environment(EnvType.CLIENT)
   private record Probe(AdvancedFinder.Scan scan, int index, BlockPos pos, double distanceSq) {
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface ProbeVisitor {
      void visit(int var1, int var2, int var3, int var4);
   }

   @Environment(EnvType.CLIENT)
   private static final class Scan {
      final NetheriteChunkTracker.Candidate section;
      final BitSet probed = new BitSet(4096);
      final int[] stamp = new int[4096];
      final int total;
      final int[] missing = new int[4096];
      int missingCount;
      int missingAt = -1000;
      int sent;
      boolean dirty;

      Scan(NetheriteChunkTracker.Candidate var1, int var2) {
         this.section = var1;
         this.total = var2;
      }
   }
}
