package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class InventoryCalm {
   private static final double STOPPED = 0.0025;
   private static final double HALTED = 1.0E-4;

   private InventoryCalm() {
   }

   public static boolean idle(ClientPlayerEntity clientPlayerEntity) {
      PlayerInput var1 = clientPlayerEntity.input.playerInput;
      return !moving(var1) && !var1.sneak();
   }

   public static boolean still(ClientPlayerEntity clientPlayerEntity) {
      return idle(clientPlayerEntity) && clientPlayerEntity.isOnGround() && clientPlayerEntity.getVelocity().horizontalLengthSquared() < 0.0025;
   }

   public static boolean settle(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (still(clientPlayerEntity)) {
         return true;
      } else {
         GameOptions var2 = minecraftClient.options;
         var2.forwardKey.setPressed(false);
         var2.backKey.setPressed(false);
         var2.leftKey.setPressed(false);
         var2.rightKey.setPressed(false);
         var2.jumpKey.setPressed(false);
         var2.sprintKey.setPressed(false);
         var2.useKey.setPressed(false);
         var2.attackKey.setPressed(false);
         PlayerInput var3 = clientPlayerEntity.input.playerInput;
         if (moving(var3) || !clientPlayerEntity.isOnGround() || clientPlayerEntity.getVelocity().horizontalLengthSquared() >= 1.0E-4) {
            return false;
         } else if (!overGround(clientPlayerEntity)) {
            return true;
         } else {
            var2.sneakKey.setPressed(false);
            return false;
         }
      }
   }

   private static boolean moving(PlayerInput playerInput) {
      return playerInput.forward() || playerInput.backward() || playerInput.left() || playerInput.right() || playerInput.jump() || playerInput.sprint();
   }

   private static boolean overGround(ClientPlayerEntity clientPlayerEntity) {
      BlockPos var1 = BlockPos.ofFloored(clientPlayerEntity.getX(), clientPlayerEntity.getY() - 0.05, clientPlayerEntity.getZ());
      return !clientPlayerEntity.getEntityWorld().getBlockState(var1).getCollisionShape(clientPlayerEntity.getEntityWorld(), var1).isEmpty();
   }
}
