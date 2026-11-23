package net.efn.client.easyplace.placement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BellBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class Clicks {
   private Clicks() {
   }

   public static boolean usesClick(World world, BlockPos blockPos, boolean bl) {
      if (world.getBlockEntity(blockPos) instanceof NamedScreenHandlerFactory) {
         return true;
      } else if (!bl) {
         return false;
      } else {
         Block var3 = world.getBlockState(blockPos).getBlock();
         return world.getBlockEntity(blockPos) != null
            || var3 instanceof DoorBlock
            || var3 instanceof TrapdoorBlock
            || var3 instanceof FenceGateBlock
            || var3 instanceof ButtonBlock
            || var3 instanceof LeverBlock
            || var3 instanceof AbstractRedstoneGateBlock
            || var3 instanceof NoteBlock
            || var3 instanceof CraftingTableBlock
            || var3 instanceof AnvilBlock
            || var3 instanceof BellBlock
            || var3 instanceof DaylightDetectorBlock;
      }
   }
}
