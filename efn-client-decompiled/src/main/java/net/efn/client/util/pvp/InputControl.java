package net.efn.client.util.pvp;

import net.efn.client.module.impl.movement.SprintModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;

@Environment(EnvType.CLIENT)
public final class InputControl {
   private static boolean dropSprint;
   private static boolean releaseForward;
   private static boolean backward;
   private static boolean jump;
   private static boolean idle;

   private InputControl() {
   }

   public static void dropSprint() {
      dropSprint = true;
   }

   public static void releaseForward(boolean bl) {
      releaseForward = true;
      backward |= bl;
   }

   public static void jump() {
      jump = true;
   }

   public static void idle() {
      idle = true;
   }

   public static PlayerInput shape(ClientPlayerEntity clientPlayerEntity, PlayerInput playerInput) {
      boolean var3 = playerInput.forward();
      boolean var4 = playerInput.backward();
      boolean var5 = playerInput.left();
      boolean var6 = playerInput.right();
      boolean var7 = playerInput.jump();
      boolean var2 = playerInput.sprint() || SprintModule.holding();
      if (idle) {
         var2 = false;
         var7 = false;
         var6 = false;
         var5 = false;
         var4 = false;
         var3 = false;
      }

      if (releaseForward && !idle) {
         var3 = false;
         var4 |= backward;
      }

      if (jump && !idle) {
         var7 = true;
      }

      if (dropSprint || idle) {
         var2 = false;
         clientPlayerEntity.setSprinting(false);
      }

      idle = false;
      jump = false;
      backward = false;
      releaseForward = false;
      dropSprint = false;
      return var3 == playerInput.forward()
            && var4 == playerInput.backward()
            && var5 == playerInput.left()
            && var6 == playerInput.right()
            && var7 == playerInput.jump()
            && var2 == playerInput.sprint()
         ? playerInput
         : new PlayerInput(var3, var4, var5, var6, var7, playerInput.sneak(), var2);
   }
}
