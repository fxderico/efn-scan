package net.efn.client.module.impl.combat;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.PlayerAttackEntityEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class CrystalOptimizerModule extends ClientModule {
   private static final int SAMPLE_CAPACITY = 64;
   private final ItemStack[] heldSamples = new ItemStack[64];
   private final int[] sampleTicks = new int[64];
   private int sampleCursor;
   private int sampleCount;
   private int ticks;
   private int trackedSince;
   private ClientPlayerEntity trackedPlayer;
   private ClientWorld trackedLevel;
   private boolean attackSent;
   private EndCrystalEntity pendingRemoval;
   private final CrystalOptimizerModule.AttackProbe attackProbe = new CrystalOptimizerModule.AttackProbe();
   private final Int2ObjectMap<CrystalOptimizerModule.Ghost> ghosts = new Int2ObjectOpenHashMap();
   private final IntSet stubborn = new IntOpenHashSet();
   private final IntSet serverRemoved = new IntOpenHashSet();

   public CrystalOptimizerModule() {
      super("crystal-optimizer", "Crystal Optimizer", "Removes crystals client-side as soon as your hit breaks them.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onEnable() {
      this.reset();
   }

   @Override
   protected void onDisable() {
      ClientWorld var1 = MinecraftClient.method_1551().field_1687;
      if (var1 != null && var1 == this.trackedLevel) {
         ObjectIterator var2 = this.ghosts.values().iterator();

         while (var2.hasNext()) {
            CrystalOptimizerModule.Ghost var3 = (CrystalOptimizerModule.Ghost)var2.next();
            revive(var1, var3.crystal());
         }
      }

      this.reset();
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onTick(TickEvent var1) {
      this.attackSent = false;
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      ClientWorld var4 = var2.field_1687;
      if (var3 != null && var4 != null) {
         if (var3 != this.trackedPlayer || var4 != this.trackedLevel) {
            this.reset();
            this.trackedPlayer = var3;
            this.trackedLevel = var4;
         }

         if (this.pendingRemoval != null && !this.pendingRemoval.method_31481()) {
            var4.method_2945(this.pendingRemoval.method_5628(), RemovalReason.field_26999);
            this.ghosts.put(this.pendingRemoval.method_5628(), new CrystalOptimizerModule.Ghost(this.pendingRemoval, this.ticks));
         }

         this.pendingRemoval = null;
         this.ticks++;
         this.serverRemoved.removeIf(var1x -> var4.method_8469(var1x) == null);
         this.sample(var3.method_6047());
         this.restoreGhosts(var2, var3, var4);
      } else {
         if (this.trackedPlayer != null || this.sampleCount > 0) {
            this.reset();
         }
      }
   }

   @EventHandler
   public void onPacketReceive(PacketReceiveEvent var1) {
      if (var1.packet instanceof EntitiesDestroyS2CPacket var2) {
         IntListIterator var3 = var2.method_36548().iterator();

         while (var3.hasNext()) {
            int var4 = var3.next();
            this.ghosts.remove(var4);
            this.stubborn.remove(var4);
            if (this.pendingRemoval != null && this.pendingRemoval.method_5628() == var4) {
               this.pendingRemoval = null;
            }

            if (this.trackedLevel != null && this.trackedLevel.method_8469(var4) instanceof EndCrystalEntity) {
               this.serverRemoved.add(var4);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onPacketSend(PacketSendEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.method_18854()) {
         if (var1.packet instanceof PlayerInteractEntityC2SPacket var4) {
            this.attackSent = this.attackProbe.isAttack(var4);
         } else if (var1.packet instanceof UpdateSelectedSlotC2SPacket var7) {
            ClientPlayerEntity var5 = var2.field_1724;
            if (var5 != null && var5 == this.trackedPlayer && PlayerInventory.method_7380(var7.method_12442())) {
               this.sample(var5.method_31548().method_5438(var7.method_12442()));
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onAttack(PlayerAttackEntityEvent var1) {
      boolean var3 = this.attackSent;
      this.attackSent = false;
      if (var3) {
         Entity var2 = var1.target;
         if (var1.target instanceof EndCrystalEntity) {
            EndCrystalEntity var4 = (EndCrystalEntity)var2;
            MinecraftClient var5 = MinecraftClient.method_1551();
            ClientPlayerEntity var6 = var5.field_1724;
            ClientWorld var7 = var5.field_1687;
            if (var6 != null && var7 != null && var6 == this.trackedPlayer && var4.method_73183() == var7) {
               if (!this.stubborn.contains(var4.method_5628())
                  && !this.serverRemoved.contains(var4.method_5628())
                  && this.breaksOnServer(var5, var6, var7, var4)) {
                  this.pendingRemoval = var4;
                  return;
               }

               return;
            }

            return;
         }
      }
   }

   private void restoreGhosts(MinecraftClient var1, ClientPlayerEntity var2, ClientWorld var3) {
      if (!this.ghosts.isEmpty()) {
         int var4 = Crystals.confirmWindow(var1, var2);
         ObjectIterator var5 = this.ghosts.values().iterator();

         while (var5.hasNext()) {
            CrystalOptimizerModule.Ghost var6 = (CrystalOptimizerModule.Ghost)var5.next();
            if (this.ticks - var6.removedAt() > var4) {
               var5.remove();
               if (revive(var3, var6.crystal())) {
                  this.stubborn.add(var6.crystal().method_5628());
               }
            }
         }
      }
   }

   private static boolean revive(ClientWorld var0, EndCrystalEntity var1) {
      if (var0.method_8469(var1.method_5628()) != null) {
         return false;
      } else {
         EndCrystalEntity var2 = new EndCrystalEntity(var0, var1.method_23317(), var1.method_23318(), var1.method_23321());
         var2.method_5838(var1.method_5628());
         var2.method_5826(var1.method_5667());
         var2.method_6839(var1.method_6836());
         var0.method_53875(var2);
         return true;
      }
   }

   private boolean breaksOnServer(MinecraftClient var1, ClientPlayerEntity var2, ClientWorld var3, EndCrystalEntity var4) {
      if (var4.method_31481()) {
         return false;
      } else if (!var2.method_7325() && !var2.method_29504() && !var2.method_6123()) {
         ItemStack var5 = var2.method_6047();
         if (var5.method_45435(var3.method_45162()) && !var2.method_75202(var5, 0)) {
            if (!var3.method_8621().method_11952(var4.method_24515())) {
               return false;
            } else {
               return !var2.method_76729(var4.method_5829(), 0.0) ? false : Crystals.enchantsAddDamage(var5) || this.recentHandsDealDamage(var1, var2, var5);
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean recentHandsDealDamage(MinecraftClient var1, ClientPlayerEntity var2, ItemStack var3) {
      if (!Crystals.dealsDamage(var2, var3)) {
         return false;
      } else {
         int var6 = Crystals.confirmWindow(var1, var2);
         boolean var7 = this.ticks - this.trackedSince >= var6;
         ItemStack var8 = var3;

         int var4;
         int var5;
         for (var5 = 0; var5 < this.sampleCount && this.ticks - this.sampleTicks[var4 = Math.floorMod(this.sampleCursor - 1 - var5, 64)] <= var6; var5++) {
            ItemStack var9 = this.heldSamples[var4];
            if (var9 != var8) {
               if (!Crystals.dealsDamage(var2, var9)) {
                  return false;
               }

               var8 = var9;
            }
         }

         if (var5 == 64) {
            var7 = false;
         }

         return var7 || Crystals.dealsDamage(var2, ItemStack.field_8037);
      }
   }

   private void sample(ItemStack var1) {
      this.heldSamples[this.sampleCursor] = var1;
      this.sampleTicks[this.sampleCursor] = this.ticks;
      this.sampleCursor = (this.sampleCursor + 1) % 64;
      if (this.sampleCount < 64) {
         this.sampleCount++;
      }
   }

   private void reset() {
      Arrays.fill(this.heldSamples, null);
      this.sampleCursor = 0;
      this.sampleCount = 0;
      this.trackedSince = this.ticks;
      this.trackedPlayer = null;
      this.trackedLevel = null;
      this.attackSent = false;
      this.pendingRemoval = null;
      this.ghosts.clear();
      this.stubborn.clear();
      this.serverRemoved.clear();
   }

   @Environment(EnvType.CLIENT)
   private static final class AttackProbe implements Handler {
      private boolean attack;

      private AttackProbe() {
      }

      boolean isAttack(PlayerInteractEntityC2SPacket var1) {
         this.attack = false;
         var1.method_34209(this);
         return this.attack;
      }

      public void method_34219(Hand var1) {
      }

      public void method_34220(Hand var1, Vec3d var2) {
      }

      public void method_34218() {
         this.attack = true;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Ghost(EndCrystalEntity crystal, int removedAt) {
   }
}
