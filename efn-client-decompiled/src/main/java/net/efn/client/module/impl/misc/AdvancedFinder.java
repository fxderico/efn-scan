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

   AdvancedFinder(NetheriteFinderModule netheriteFinderModule) {
      this.module = netheriteFinderModule;
   }

   boolean armed() {
      return this.armed;
   }

   Collection<BlockPos> revealed() {
      return this.revealed;
   }

   boolean isDone(NetheriteChunkTracker.Candidate candidate) {
      return this.done.contains(candidate);
   }

   float progress(NetheriteChunkTracker.Candidate candidate) {
      AdvancedFinder.Scan var2 = this.armed ? this.scans.get(candidate) : null;
      return var2 == null ? -1.0F : fraction(var2);
   }

   private static float fraction(AdvancedFinder.Scan scan) {
      return scan.total == 0 ? 1.0F : Math.min(1.0F, (float)scan.sent / scan.total);
   }

   void forEachProbe(Vec3d vec3d, double d, int i, float f, AdvancedFinder.ProbeVisitor probeVisitor) {
      if (this.armed) {
         double var7 = d * d;
         int var9 = 0;

         for (AdvancedFinder.Scan var11 : this.scans.values()) {
            if (!(fraction(var11) >= f)) {
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
                        (var23 = (var22 = var12 + (var15 & 15)) + 0.5 - vec3d.x) * var23
                              + (var20 = (var19 = var13 + (var15 >> 8)) + 0.5 - vec3d.y) * var20
                              + (var17 = (var16 = var14 + (var15 >> 4 & 15)) + 0.5 - vec3d.z) * var17
                           > var7
                     )) {
                     probeVisitor.visit(var22, var19, var16, this.ticks - var25 + 1);
                     if (++var9 >= i) {
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   void forEachMissing(Vec3d vec3d, double d, int i, float f, AdvancedFinder.MissingVisitor missingVisitor) {
      if (this.armed) {
         double var7 = d * d;
         int var9 = 0;

         for (AdvancedFinder.Scan var11 : this.scans.values()) {
            if (!(fraction(var11) < f)) {
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
                        (var23 = (var22 = var12 + (var25 & 15)) + 0.5 - vec3d.x) * var23
                              + (var20 = (var19 = var13 + (var25 >> 8)) + 0.5 - vec3d.y) * var20
                              + (var17 = (var16 = var14 + (var25 >> 4 & 15)) + 0.5 - vec3d.z) * var17
                           > var7
                     )) {
                     missingVisitor.visit(var22, var19, var16);
                     if (++var9 >= i) {
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private void refreshMissing(AdvancedFinder.Scan scan) {
      if (this.level != null && this.ticks - scan.missingAt >= 10) {
         scan.missingAt = this.ticks;
         int var2 = scan.section.chunkX() << 4;
         int var3 = scan.section.sectionY() << 4;
         int var4 = scan.section.chunkZ() << 4;
         Mutable var5 = new Mutable();
         int var6 = 0;

         for (int var7 = scan.probed.nextClearBit(0); var7 < 4096; var7 = scan.probed.nextClearBit(var7 + 1)) {
            var5.set(var2 + (var7 & 15), var3 + (var7 >> 8), var4 + (var7 >> 4 & 15));
            if (worthProbing(this.level, var5)) {
               scan.missing[var6++] = var7;
            }
         }

         scan.missingCount = var6;
      }
   }

   void arm() {
      this.armed = true;
      this.lastReason = "";
   }

   void disarm(MinecraftClient minecraftClient) {
      this.armed = false;
      this.lastReason = "";
   }

   void reset(MinecraftClient minecraftClient) {
      this.disarm(minecraftClient);
      this.scans.clear();
      this.done.clear();
      this.revealed.clear();
      this.budget = 0.0;
      this.level = null;
   }

   boolean onRevealed(BlockPos blockPos) {
      NetheriteChunkTracker.Candidate var2 = new NetheriteChunkTracker.Candidate(blockPos.getX() >> 4, blockPos.getZ() >> 4, blockPos.getY() >> 4);
      if (!this.revealed.add(blockPos)) {
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
               this.module, ModuleChat.body().text("Ancient debris ").good("found").text(" at ").coords(blockPos.getX(), blockPos.getY(), blockPos.getZ())
            );
         }

         return true;
      }
   }

   void maintain(MinecraftClient minecraftClient) {
      ClientWorld var2 = minecraftClient.world;
      if (var2 == this.level) {
         if (var2 != null) {
            this.revealed.removeIf(blockPos -> !var2.getBlockState(blockPos).isOf(Blocks.ANCIENT_DEBRIS));
         }
      } else {
         this.scans.clear();
         this.done.clear();
         this.revealed.clear();
         this.level = var2;
      }
   }

   void tick(MinecraftClient minecraftClient) {
      if (this.armed) {
         ClientPlayerEntity var2 = minecraftClient.player;
         ClientWorld var3 = minecraftClient.world;
         if (var2 != null && var3 != null && var2.networkHandler != null && minecraftClient.interactionManager != null) {
            this.ticks++;
            this.budget = Math.min(600.0, this.budget + 18.0);
            if (this.handWarning > 0) {
               this.handWarning--;
            }

            if (!NetheriteChunkTracker.isNether(var3)) {
               this.hold("Not in the Nether");
            } else {
               this.lastReason = "";
               if (minecraftClient.currentScreen == null && !var2.isUsingItem() && !minecraftClient.interactionManager.isBreakingBlock()) {
                  Vec3d var4 = var2.getEyePos();
                  this.refreshScans(var3, var4);
                  if (!this.scans.isEmpty() && !(this.budget < 1.0)) {
                     List var5 = this.collect(var3, var4);
                     if (!var5.isEmpty()) {
                        int var6 = var2.getInventory().getSelectedSlot();
                        int var7 = probeSlot(var2, var6);
                        if (var7 < 0) {
                           if (this.handWarning <= 0) {
                              this.handWarning = 200;
                              ModuleChat.send(this.module, ModuleChat.body().text("Hotbar needs an empty or non-tool slot"));
                           }
                        } else {
                           if (var7 != var6) {
                              var2.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(var7));
                           }

                           BlockPos var8 = null;

                           for (AdvancedFinder.Probe var10 : var5) {
                              if (this.budget < 1.0) {
                                 break;
                              }

                              sendAction(var2, var3, Action.START_DESTROY_BLOCK, var10.pos());
                              var10.scan().probed.set(var10.index());
                              var10.scan().sent++;
                              var10.scan().stamp[var10.index()] = this.ticks;
                              var10.scan().dirty = true;
                              this.budget--;
                              var8 = var10.pos();
                           }

                           if (var8 != null) {
                              sendAction(var2, var3, Action.ABORT_DESTROY_BLOCK, var8);
                           }

                           if (var7 != var6) {
                              var2.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(var6));
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void refreshScans(ClientWorld clientWorld, Vec3d vec3d) {
      HashSet var3 = new HashSet<>(NetheriteChunkTracker.snapshot());
      this.scans.keySet().retainAll(var3);

      for (NetheriteChunkTracker.Candidate var5 : var3) {
         if (!this.done.contains(var5) && !this.scans.containsKey(var5) && distanceToSectionSq(vec3d, var5) <= 25.0) {
            this.scans.put(var5, new AdvancedFinder.Scan(var5, countProbeable(clientWorld, var5)));
         }
      }

      if (this.recountCooldown-- <= 0) {
         this.recountCooldown = 10;
         Iterator var7 = this.scans.values().iterator();

         while (var7.hasNext()) {
            AdvancedFinder.Scan var8 = (AdvancedFinder.Scan)var7.next();
            if (var8.dirty) {
               var8.dirty = false;
               if (!hasUnprobed(clientWorld, var8)) {
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

   private List<AdvancedFinder.Probe> collect(ClientWorld clientWorld, Vec3d vec3d) {
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      int var5 = (int)Math.ceil(5.0);
      int var6 = (int)Math.floor(vec3d.x);
      int var7 = (int)Math.floor(vec3d.y);
      int var8 = (int)Math.floor(vec3d.z);
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
                     var9.set(var21, var22, var23);
                     double var28 = distanceToBlockSq(vec3d, var9);
                     if (!(var28 > 25.0)) {
                        if (!worthProbing(clientWorld, var9)) {
                           var11.probed.set(var27);
                        } else {
                           AdvancedFinder.Probe var30 = new AdvancedFinder.Probe(var11, var27, var9.toImmutable(), var28);
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

   private static int countProbeable(ClientWorld clientWorld, NetheriteChunkTracker.Candidate candidate) {
      int var2 = candidate.chunkX() << 4;
      int var3 = candidate.sectionY() << 4;
      int var4 = candidate.chunkZ() << 4;
      Mutable var5 = new Mutable();
      int var6 = 0;

      for (int var7 = 0; var7 < 4096; var7++) {
         var5.set(var2 + (var7 & 15), var3 + (var7 >> 8), var4 + (var7 >> 4 & 15));
         if (worthProbing(clientWorld, var5)) {
            var6++;
         }
      }

      return var6;
   }

   private static boolean hasUnprobed(ClientWorld clientWorld, AdvancedFinder.Scan scan) {
      int var2 = scan.section.chunkX() << 4;
      int var3 = scan.section.sectionY() << 4;
      int var4 = scan.section.chunkZ() << 4;
      Mutable var5 = new Mutable();

      for (int var6 = scan.probed.nextClearBit(0); var6 < 4096; var6 = scan.probed.nextClearBit(var6 + 1)) {
         var5.set(var2 + (var6 & 15), var3 + (var6 >> 8), var4 + (var6 >> 4 & 15));
         if (worthProbing(clientWorld, var5)) {
            return true;
         }
      }

      return false;
   }

   private static boolean worthProbing(ClientWorld clientWorld, BlockPos blockPos) {
      BlockState var2 = clientWorld.getBlockState(blockPos);
      return !var2.isAir() && var2.getFluidState().isEmpty() && !var2.isOf(Blocks.ANCIENT_DEBRIS)
         ? var2.isOpaqueFullCube() && var2.getHardness(clientWorld, blockPos) > 0.0F
         : false;
   }

   private void hold(String string) {
      if (!string.equals(this.lastReason)) {
         this.lastReason = string;
         ModuleChat.send(this.module, ModuleChat.body().text(string));
      }
   }

   private static void sendAction(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld, Action action, BlockPos blockPos) {
      PendingUpdateManager var4 = ((AccessorNetheriteClientLevel)clientWorld).efn$blockStatePredictionHandler();

      try (PendingUpdateManager var5 = var4.incrementSequence()) {
         clientPlayerEntity.networkHandler.sendPacket(new PlayerActionC2SPacket(action, blockPos, Direction.UP, var5.getSequence()));
      }
   }

   private static double distanceToSectionSq(Vec3d vec3d, NetheriteChunkTracker.Candidate candidate) {
      double var2 = candidate.chunkX() * 16.0;
      double var4 = candidate.sectionY() * 16.0;
      double var6 = candidate.chunkZ() * 16.0;
      double var8 = Math.max(0.0, Math.max(var2 - vec3d.x, vec3d.x - (var2 + 16.0)));
      double var10 = Math.max(0.0, Math.max(var4 - vec3d.y, vec3d.y - (var4 + 16.0)));
      double var12 = Math.max(0.0, Math.max(var6 - vec3d.z, vec3d.z - (var6 + 16.0)));
      return var8 * var8 + var10 * var10 + var12 * var12;
   }

   private static double distanceToBlockSq(Vec3d vec3d, BlockPos blockPos) {
      double var2 = Math.max(0.0, Math.max(blockPos.getX() - vec3d.x, vec3d.x - (blockPos.getX() + 1.0)));
      double var4 = Math.max(0.0, Math.max(blockPos.getY() - vec3d.y, vec3d.y - (blockPos.getY() + 1.0)));
      double var6 = Math.max(0.0, Math.max(blockPos.getZ() - vec3d.z, vec3d.z - (blockPos.getZ() + 1.0)));
      return var2 * var2 + var4 * var4 + var6 * var6;
   }

   private static int probeSlot(ClientPlayerEntity clientPlayerEntity, int i) {
      if (!isTool(clientPlayerEntity.getInventory().getStack(i))) {
         return i;
      } else {
         for (int var2 = 0; var2 < 9; var2++) {
            if (clientPlayerEntity.getInventory().getStack(var2).isEmpty()) {
               return var2;
            }
         }

         for (int var3 = 0; var3 < 9; var3++) {
            if (!isTool(clientPlayerEntity.getInventory().getStack(var3))) {
               return var3;
            }
         }

         return -1;
      }
   }

   private static boolean isTool(ItemStack itemStack) {
      return itemStack.contains(DataComponentTypes.TOOL);
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface MissingVisitor {
      void visit(int i, int j, int k);
   }

   @Environment(EnvType.CLIENT)
   private record Probe(AdvancedFinder.Scan scan, int index, BlockPos pos, double distanceSq) {
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface ProbeVisitor {
      void visit(int i, int j, int k, int l);
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

      Scan(NetheriteChunkTracker.Candidate candidate, int i) {
         this.section = candidate;
         this.total = i;
      }
   }
}
