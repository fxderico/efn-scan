package net.efn.client.easyplace.sync;

import net.efn.client.easyplace.schematic.SchematicTarget;
import net.efn.client.easyplace.util.BlockStateUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public final class ServerAckManager {
   private PlacementConfirmation pending;

   public ServerAckManager() {
   }

   public void begin(SchematicTarget schematicTarget, long l, int i, int j, int k, BlockState blockState) {
      this.pending = new PlacementConfirmation(schematicTarget.pos(), schematicTarget.expected(), l, l + i, j, k, blockState);
   }

   public ServerAckManager.Result poll(ClientWorld clientWorld, long l) {
      if (this.pending == null) {
         return ServerAckManager.Result.IDLE;
      } else if (!BlockAcks.answered(clientWorld, this.pending.sequence())) {
         return l >= this.pending.deadlineTick() ? ServerAckManager.Result.TIMEOUT : ServerAckManager.Result.WAITING;
      } else {
         BlockState var4 = this.pending.predicted() != null && !this.pending.predicted().isAir() ? this.pending.predicted() : this.pending.expected().state();
         return BlockStateUtil.statesMatch(var4, clientWorld.getBlockState(this.pending.pos()))
            ? ServerAckManager.Result.CONFIRMED
            : ServerAckManager.Result.REFUSED;
      }
   }

   public PlacementConfirmation getPending() {
      return this.pending;
   }

   public void clear() {
      this.pending = null;
   }

   @Environment(EnvType.CLIENT)
   public static enum Result {
      IDLE,
      WAITING,
      CONFIRMED,
      REFUSED,
      TIMEOUT;

      private Result() {
      }
   }
}
