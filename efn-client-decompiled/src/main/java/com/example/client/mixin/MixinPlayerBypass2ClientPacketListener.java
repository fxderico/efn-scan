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
      method = {"method_11120"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Login(GameJoinS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onLogin(var1.comp_169(), var1.comp_98(), var1.comp_1727().comp_1554());
   }

   @Inject(
      method = {"method_11117"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Respawn(PlayerRespawnS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onRespawn(var1.comp_1728().comp_1554());
   }

   @Inject(
      method = {"method_11157"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Teleport(PlayerPositionLookS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onTeleport();
   }

   @Inject(
      method = {"method_39025"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Simulation(SimulationDistanceS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onSimulationDistance(var1.comp_170());
   }

   @Inject(
      method = {"method_20203"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2View(ChunkLoadDistanceS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onViewDistance(var1.method_20206());
   }

   @Inject(
      method = {"method_11128"},
      at = {@At("TAIL")}
   )
   private void efn$pb2Chunk(ChunkDataS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onChunk(var1.method_11523(), var1.method_11524());
   }

   @Inject(
      method = {"method_11155"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Move(EntityS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onMove(var1);
   }

   @Inject(
      method = {"method_64553"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2PositionSync(EntityPositionSyncS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onPositionSync(var1);
   }

   @Inject(
      method = {"method_11139"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Head(EntitySetHeadYawS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onHeadRotation(var1);
   }

   @Inject(
      method = {"method_11093"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2EntityData(EntityTrackerUpdateS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onEntityData(var1);
   }

   @Inject(
      method = {"method_11148"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2EntityEvent(EntityStatusS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onEntityEvent(var1);
   }

   @Inject(
      method = {"method_11112"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2AddEntity(EntitySpawnS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onAddEntity(var1);
   }

   @Inject(
      method = {"method_11095"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2RemoveEntities(EntitiesDestroyS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onRemoveEntities(var1.method_36548());
   }

   @Inject(
      method = {"method_11136"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Block(BlockUpdateS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onBlockUpdate(var1);
   }

   @Inject(
      method = {"method_11100"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Section(ChunkDeltaUpdateS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onSectionUpdate(var1);
   }

   @Inject(
      method = {"method_11150"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2TakeItem(ItemPickupAnimationS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onTakeItem(var1);
   }

   @Inject(
      method = {"method_49034"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Damage(EntityDamageS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onDamage(var1);
   }

   @Inject(
      method = {"method_11151"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$pb2Equipment(EntityEquipmentUpdateS2CPacket var1, CallbackInfo var2) {
      SimulationSignals.onEquipment(var1);
   }
}
