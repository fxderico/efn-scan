package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({VehicleMoveC2SPacket.class})
public interface AccessorBoatFlyServerboundMoveVehiclePacket {
   @Mutable
   @Accessor("comp_3350")
   void efn$setPosition(Vec3d var1);
}
