package net.efn.client.module.impl.movement.entitycontrol;

import com.example.client.mixin.AccessorBoatFlyServerboundMoveVehiclePacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class VehicleAntiKick {
   private static final double Y_OFFSET = 0.0313;
   private static final long HOLD_NANOS = 50000000L;
   private int delayLeft;
   private double lastPacketY = 1.7976931348623157E308;
   private boolean sentPacket;
   private long dipNanos;
   private Entity trackedVehicle;

   public VehicleAntiKick() {
   }

   public void start(int i) {
      this.delayLeft = i;
      this.forget();
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      this.trackedVehicle = var2 != null ? var2.getVehicle() : null;
   }

   public void stop() {
      this.trackedVehicle = null;
   }

   public void tick() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      ClientPlayerEntity var2 = var1.player;
      Entity var3 = var2 != null ? var2.getVehicle() : null;
      this.trackVehicle(var3);
      if (this.sentPacket && System.nanoTime() - this.dipNanos >= 50000000L) {
         ClientPlayNetworkHandler var4 = var1.getNetworkHandler();
         if (var3 != null && var4 != null && this.lastPacketY != 1.7976931348623157E308) {
            VehicleMoveC2SPacket var5 = VehicleMoveC2SPacket.fromVehicle(var3);
            Vec3d var6 = var5.position();
            var4.sendPacket(new VehicleMoveC2SPacket(new Vec3d(var6.x, this.lastPacketY, var6.z), var5.yaw(), var5.pitch(), var5.onGround()));
         }

         this.sentPacket = false;
      }

      if (this.delayLeft > 0) {
         this.delayLeft--;
      }
   }

   public boolean onPacketSend(VehicleMoveC2SPacket vehicleMoveC2SPacket, int i) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      if (!var4.isOnThread()) {
         return false;
      } else {
         ClientPlayerEntity var5 = var4.player;
         Entity var3 = var5 != null ? var5.getVehicle() : null;
         if (var3 == null) {
            return false;
         } else {
            this.trackVehicle(var3);
            Vec3d var7 = vehicleMoveC2SPacket.position();
            double var8 = var7.y;
            if (this.sentPacket && System.nanoTime() - this.dipNanos < 50000000L) {
               this.lastPacketY = var8;
               return true;
            } else {
               if (this.delayLeft <= 0 && !this.sentPacket && this.shouldFlyDown(var8) && EntityControlVehicleHook.isOnAir(var3) && !var3.isFlyingVehicle()) {
                  ((AccessorBoatFlyServerboundMoveVehiclePacket)vehicleMoveC2SPacket).efn$setPosition(new Vec3d(var7.x, this.lastPacketY - 0.0313, var7.z));
                  this.sentPacket = true;
                  this.dipNanos = System.nanoTime();
                  this.delayLeft = i;
               }

               this.lastPacketY = var8;
               return false;
            }
         }
      }
   }

   public void observe(VehicleMoveC2SPacket vehicleMoveC2SPacket) {
      this.lastPacketY = vehicleMoveC2SPacket.position().y;
   }

   public boolean isHolding() {
      return this.sentPacket && System.nanoTime() - this.dipNanos < 50000000L;
   }

   public void forget() {
      this.lastPacketY = 1.7976931348623157E308;
      this.sentPacket = false;
   }

   private boolean shouldFlyDown(double d) {
      return d >= this.lastPacketY ? true : this.lastPacketY - d < 0.0313;
   }

   private void trackVehicle(Entity entity) {
      if (entity != this.trackedVehicle) {
         this.trackedVehicle = entity;
         this.forget();
      }
   }
}
