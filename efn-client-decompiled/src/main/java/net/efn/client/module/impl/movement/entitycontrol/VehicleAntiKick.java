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

   public void start(int var1) {
      this.delayLeft = var1;
      this.forget();
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      this.trackedVehicle = var2 != null ? var2.method_5854() : null;
   }

   public void stop() {
      this.trackedVehicle = null;
   }

   public void tick() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      ClientPlayerEntity var2 = var1.field_1724;
      Entity var3 = var2 != null ? var2.method_5854() : null;
      this.trackVehicle(var3);
      if (this.sentPacket && System.nanoTime() - this.dipNanos >= 50000000L) {
         ClientPlayNetworkHandler var4 = var1.method_1562();
         if (var3 != null && var4 != null && this.lastPacketY != 1.7976931348623157E308) {
            VehicleMoveC2SPacket var5 = VehicleMoveC2SPacket.method_65307(var3);
            Vec3d var6 = var5.comp_3350();
            var4.method_52787(
               new VehicleMoveC2SPacket(new Vec3d(var6.field_1352, this.lastPacketY, var6.field_1350), var5.comp_3351(), var5.comp_3352(), var5.comp_3353())
            );
         }

         this.sentPacket = false;
      }

      if (this.delayLeft > 0) {
         this.delayLeft--;
      }
   }

   public boolean onPacketSend(VehicleMoveC2SPacket var1, int var2) {
      MinecraftClient var4 = MinecraftClient.method_1551();
      if (!var4.method_18854()) {
         return false;
      } else {
         ClientPlayerEntity var5 = var4.field_1724;
         Entity var3 = var5 != null ? var5.method_5854() : null;
         if (var3 == null) {
            return false;
         } else {
            this.trackVehicle(var3);
            Vec3d var7 = var1.comp_3350();
            double var8 = var7.field_1351;
            if (this.sentPacket && System.nanoTime() - this.dipNanos < 50000000L) {
               this.lastPacketY = var8;
               return true;
            } else {
               if (this.delayLeft <= 0 && !this.sentPacket && this.shouldFlyDown(var8) && EntityControlVehicleHook.isOnAir(var3) && !var3.method_70987()) {
                  ((AccessorBoatFlyServerboundMoveVehiclePacket)var1).efn$setPosition(new Vec3d(var7.field_1352, this.lastPacketY - 0.0313, var7.field_1350));
                  this.sentPacket = true;
                  this.dipNanos = System.nanoTime();
                  this.delayLeft = var2;
               }

               this.lastPacketY = var8;
               return false;
            }
         }
      }
   }

   public void observe(VehicleMoveC2SPacket var1) {
      this.lastPacketY = var1.comp_3350().field_1351;
   }

   public boolean isHolding() {
      return this.sentPacket && System.nanoTime() - this.dipNanos < 50000000L;
   }

   public void forget() {
      this.lastPacketY = 1.7976931348623157E308;
      this.sentPacket = false;
   }

   private boolean shouldFlyDown(double var1) {
      return var1 >= this.lastPacketY ? true : this.lastPacketY - var1 < 0.0313;
   }

   private void trackVehicle(Entity var1) {
      if (var1 != this.trackedVehicle) {
         this.trackedVehicle = var1;
         this.forget();
      }
   }
}
