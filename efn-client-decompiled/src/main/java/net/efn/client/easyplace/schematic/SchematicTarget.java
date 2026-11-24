package net.efn.client.easyplace.schematic;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public record SchematicTarget(BlockPos pos, SchematicBlockState expected, BlockState actual, TargetStatus status, double distanceSquared) {
   public SchematicTarget withActual(BlockState blockState, TargetStatus targetStatus) {
      return new SchematicTarget(this.pos, this.expected, blockState, targetStatus, this.distanceSquared);
   }
}
