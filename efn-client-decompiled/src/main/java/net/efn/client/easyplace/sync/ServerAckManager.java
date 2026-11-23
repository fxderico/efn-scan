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

   public void begin(SchematicTarget var1, long var2, int var4, int var5, int var6, BlockState var7) {
      this.pending = new PlacementConfirmation(var1.pos(), var1.expected(), var2, var2 + var4, var5, var6, var7);
   }

   public ServerAckManager.Result poll(ClientWorld var1, long var2) {
      if (this.pending == null) {
         return ServerAckManager.Result.IDLE;
      } else if (!BlockAcks.answered(var1, this.pending.sequence())) {
         return var2 >= this.pending.deadlineTick() ? ServerAckManager.Result.TIMEOUT : ServerAckManager.Result.WAITING;
      } else {
         BlockState var4 = this.pending.predicted() != null && !this.pending.predicted().method_26215()
            ? this.pending.predicted()
            : this.pending.expected().state();
         return BlockStateUtil.statesMatch(var4, var1.method_8320(this.pending.pos())) ? ServerAckManager.Result.CONFIRMED : ServerAckManager.Result.REFUSED;
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
