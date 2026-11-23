package net.efn.client.easyplace.schematic;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public record SchematicTarget(BlockPos pos, SchematicBlockState expected, BlockState actual, TargetStatus status, double distanceSquared) {
   public SchematicTarget withActual(BlockState var1, TargetStatus var2) {
      return new SchematicTarget(this.pos, this.expected, var1, var2, this.distanceSquared);
   }
}
