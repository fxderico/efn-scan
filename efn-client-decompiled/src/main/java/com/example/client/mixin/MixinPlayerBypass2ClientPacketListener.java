package com.example.client.mixin;

import net.efn.client.util.playerbypass2.SimulationSignals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.SimulationDistanceS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayNetworkHandler.class})
public abstract class MixinPlayerBypass2ClientPacketListener {
   private static final String ON_MAIN_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V";

   public MixinPlayerBypass2ClientPacketListener() {
   }

   @Inject(
      method = {"onGameJoin"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Login(GameJoinS2CPacket gameJoinS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onLogin(gameJoinS2CPacket.simulationDistance(), gameJoinS2CPacket.viewDistance(), gameJoinS2CPacket.commonPlayerSpawnInfo().dimension());
   }

   @Inject(
      method = {"onPlayerRespawn"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Respawn(PlayerRespawnS2CPacket playerRespawnS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onRespawn(playerRespawnS2CPacket.commonPlayerSpawnInfo().dimension());
   }

   @Inject(
      method = {"onPlayerPositionLook"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Teleport(PlayerPositionLookS2CPacket playerPositionLookS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onTeleport();
   }

   @Inject(
      method = {"onSimulationDistance"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Simulation(SimulationDistanceS2CPacket simulationDistanceS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onSimulationDistance(simulationDistanceS2CPacket.simulationDistance());
   }

   @Inject(
      method = {"onChunkLoadDistance"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2View(ChunkLoadDistanceS2CPacket chunkLoadDistanceS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onViewDistance(chunkLoadDistanceS2CPacket.getDistance());
   }

   @Inject(
      method = {"onChunkData"},
      at = {@At("TAIL")}
   )
   private void efn$pb2Chunk(ChunkDataS2CPacket chunkDataS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onChunk(chunkDataS2CPacket.getChunkX(), chunkDataS2CPacket.getChunkZ());
   }

   @Inject(
      method = {"onEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Move(EntityS2CPacket entityS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onMove(entityS2CPacket);
   }

   @Inject(
      method = {"onEntityPositionSync"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2PositionSync(EntityPositionSyncS2CPacket entityPositionSyncS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onPositionSync(entityPositionSyncS2CPacket);
   }

   @Inject(
      method = {"onEntitySetHeadYaw"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Head(EntitySetHeadYawS2CPacket entitySetHeadYawS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onHeadRotation(entitySetHeadYawS2CPacket);
   }

   @Inject(
      method = {"onEntityTrackerUpdate"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2EntityData(EntityTrackerUpdateS2CPacket entityTrackerUpdateS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onEntityData(entityTrackerUpdateS2CPacket);
   }

   @Inject(
      method = {"onEntityStatus"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2EntityEvent(EntityStatusS2CPacket entityStatusS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onEntityEvent(entityStatusS2CPacket);
   }

   @Inject(
      method = {"onEntitySpawn"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2AddEntity(EntitySpawnS2CPacket entitySpawnS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onAddEntity(entitySpawnS2CPacket);
   }

   @Inject(
      method = {"onEntitiesDestroy"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2RemoveEntities(EntitiesDestroyS2CPacket entitiesDestroyS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onRemoveEntities(entitiesDestroyS2CPacket.getEntityIds());
   }

   @Inject(
      method = {"onBlockUpdate"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Block(BlockUpdateS2CPacket blockUpdateS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onBlockUpdate(blockUpdateS2CPacket);
   }

   @Inject(
      method = {"onChunkDeltaUpdate"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Section(ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onSectionUpdate(chunkDeltaUpdateS2CPacket);
   }

   @Inject(
      method = {"onItemPickupAnimation"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2TakeItem(ItemPickupAnimationS2CPacket itemPickupAnimationS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onTakeItem(itemPickupAnimationS2CPacket);
   }

   @Inject(
      method = {"onEntityDamage"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Damage(EntityDamageS2CPacket entityDamageS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onDamage(entityDamageS2CPacket);
   }

   @Inject(
      method = {"onEntityEquipmentUpdate"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Equipment(EntityEquipmentUpdateS2CPacket entityEquipmentUpdateS2CPacket, CallbackInfo callbackInfo) {
      SimulationSignals.onEquipment(entityEquipmentUpdateS2CPacket);
   }
}
