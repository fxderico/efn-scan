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
   private final Int2ObjectMap<CrystalOptimizerModule.Ghost> ghosts = new Int2ObjectOpenHashMap<>();
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
      ClientWorld var1 = MinecraftClient.getInstance().world;
      if (var1 != null && var1 == this.trackedLevel) {
         for (CrystalOptimizerModule.Ghost var3 : this.ghosts.values()) {
            revive(var1, var3.crystal());
         }
      }

      this.reset();
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onTick(TickEvent tickEvent) {
      this.attackSent = false;
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      ClientWorld var4 = var2.world;
      if (var3 != null && var4 != null) {
         if (var3 != this.trackedPlayer || var4 != this.trackedLevel) {
            this.reset();
            this.trackedPlayer = var3;
            this.trackedLevel = var4;
         }

         if (this.pendingRemoval != null && !this.pendingRemoval.isRemoved()) {
            var4.removeEntity(this.pendingRemoval.getId(), RemovalReason.DISCARDED);
            this.ghosts.put(this.pendingRemoval.getId(), new CrystalOptimizerModule.Ghost(this.pendingRemoval, this.ticks));
         }

         this.pendingRemoval = null;
         this.ticks++;
         this.serverRemoved.removeIf(i -> var4.getEntityById(i) == null);
         this.sample(var3.getMainHandStack());
         this.restoreGhosts(var2, var3, var4);
      } else {
         if (this.trackedPlayer != null || this.sampleCount > 0) {
            this.reset();
         }
      }
   }

   @EventHandler
   public void onPacketReceive(PacketReceiveEvent packetReceiveEvent) {
      if (packetReceiveEvent.packet instanceof EntitiesDestroyS2CPacket var2) {
         IntListIterator var3 = var2.getEntityIds().iterator();

         while (var3.hasNext()) {
            int var4 = var3.next();
            this.ghosts.remove(var4);
            this.stubborn.remove(var4);
            if (this.pendingRemoval != null && this.pendingRemoval.getId() == var4) {
               this.pendingRemoval = null;
            }

            if (this.trackedLevel != null && this.trackedLevel.getEntityById(var4) instanceof EndCrystalEntity) {
               this.serverRemoved.add(var4);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onPacketSend(PacketSendEvent packetSendEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.isOnThread()) {
         if (packetSendEvent.packet instanceof PlayerInteractEntityC2SPacket var4) {
            this.attackSent = this.attackProbe.isAttack(var4);
         } else if (packetSendEvent.packet instanceof UpdateSelectedSlotC2SPacket var7) {
            ClientPlayerEntity var5 = var2.player;
            if (var5 != null && var5 == this.trackedPlayer && PlayerInventory.isValidHotbarIndex(var7.getSelectedSlot())) {
               this.sample(var5.getInventory().getStack(var7.getSelectedSlot()));
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onAttack(PlayerAttackEntityEvent playerAttackEntityEvent) {
      boolean var3 = this.attackSent;
      this.attackSent = false;
      if (var3) {
         Entity var2 = playerAttackEntityEvent.target;
         if (playerAttackEntityEvent.target instanceof EndCrystalEntity) {
            EndCrystalEntity var4 = (EndCrystalEntity)var2;
            MinecraftClient var5 = MinecraftClient.getInstance();
            ClientPlayerEntity var6 = var5.player;
            ClientWorld var7 = var5.world;
            if (var6 != null && var7 != null && var6 == this.trackedPlayer && var4.getEntityWorld() == var7) {
               if (!this.stubborn.contains(var4.getId()) && !this.serverRemoved.contains(var4.getId()) && this.breaksOnServer(var5, var6, var7, var4)) {
                  this.pendingRemoval = var4;
                  return;
               }

               return;
            }

            return;
         }
      }
   }

   private void restoreGhosts(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      if (!this.ghosts.isEmpty()) {
         int var4 = Crystals.confirmWindow(minecraftClient, clientPlayerEntity);
         ObjectIterator var5 = this.ghosts.values().iterator();

         while (var5.hasNext()) {
            CrystalOptimizerModule.Ghost var6 = (CrystalOptimizerModule.Ghost)var5.next();
            if (this.ticks - var6.removedAt() > var4) {
               var5.remove();
               if (revive(clientWorld, var6.crystal())) {
                  this.stubborn.add(var6.crystal().getId());
               }
            }
         }
      }
   }

   private static boolean revive(ClientWorld clientWorld, EndCrystalEntity endCrystalEntity) {
      if (clientWorld.getEntityById(endCrystalEntity.getId()) != null) {
         return false;
      } else {
         EndCrystalEntity var2 = new EndCrystalEntity(clientWorld, endCrystalEntity.getX(), endCrystalEntity.getY(), endCrystalEntity.getZ());
         var2.setId(endCrystalEntity.getId());
         var2.setUuid(endCrystalEntity.getUuid());
         var2.setShowBottom(endCrystalEntity.shouldShowBottom());
         clientWorld.addEntity(var2);
         return true;
      }
   }

   private boolean breaksOnServer(
      MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld, EndCrystalEntity endCrystalEntity
   ) {
      if (endCrystalEntity.isRemoved()) {
         return false;
      } else if (!clientPlayerEntity.isSpectator() && !clientPlayerEntity.isDead() && !clientPlayerEntity.isUsingRiptide()) {
         ItemStack var5 = clientPlayerEntity.getMainHandStack();
         if (var5.isItemEnabled(clientWorld.getEnabledFeatures()) && !clientPlayerEntity.isBelowMinimumAttackCharge(var5, 0)) {
            if (!clientWorld.getWorldBorder().contains(endCrystalEntity.getBlockPos())) {
               return false;
            } else {
               return !clientPlayerEntity.canAttackEntityIn(endCrystalEntity.getBoundingBox(), 0.0)
                  ? false
                  : Crystals.enchantsAddDamage(var5) || this.recentHandsDealDamage(minecraftClient, clientPlayerEntity, var5);
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean recentHandsDealDamage(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, ItemStack itemStack) {
      if (!Crystals.dealsDamage(clientPlayerEntity, itemStack)) {
         return false;
      } else {
         int var6 = Crystals.confirmWindow(minecraftClient, clientPlayerEntity);
         boolean var7 = this.ticks - this.trackedSince >= var6;
         ItemStack var8 = itemStack;

         int var4;
         int var5;
         for (var5 = 0; var5 < this.sampleCount && this.ticks - this.sampleTicks[var4 = Math.floorMod(this.sampleCursor - 1 - var5, 64)] <= var6; var5++) {
            ItemStack var9 = this.heldSamples[var4];
            if (var9 != var8) {
               if (!Crystals.dealsDamage(clientPlayerEntity, var9)) {
                  return false;
               }

               var8 = var9;
            }
         }

         if (var5 == 64) {
            var7 = false;
         }

         return var7 || Crystals.dealsDamage(clientPlayerEntity, ItemStack.EMPTY);
      }
   }

   private void sample(ItemStack itemStack) {
      this.heldSamples[this.sampleCursor] = itemStack;
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

      boolean isAttack(PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
         this.attack = false;
         playerInteractEntityC2SPacket.handle(this);
         return this.attack;
      }

      @Override
      public void interact(Hand hand) {
      }

      @Override
      public void interactAt(Hand hand, Vec3d pos) {
      }

      @Override
      public void attack() {
         this.attack = true;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Ghost(EndCrystalEntity crystal, int removedAt) {
   }
}
