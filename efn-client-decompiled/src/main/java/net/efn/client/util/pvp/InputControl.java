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

   public static void releaseForward(boolean var0) {
      releaseForward = true;
      backward |= var0;
   }

   public static void jump() {
      jump = true;
   }

   public static void idle() {
      idle = true;
   }

   public static PlayerInput shape(ClientPlayerEntity var0, PlayerInput var1) {
      boolean var3 = var1.comp_3159();
      boolean var4 = var1.comp_3160();
      boolean var5 = var1.comp_3161();
      boolean var6 = var1.comp_3162();
      boolean var7 = var1.comp_3163();
      boolean var2 = var1.comp_3165() || SprintModule.holding();
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
         var0.method_5728(false);
      }

      idle = false;
      jump = false;
      backward = false;
      releaseForward = false;
      dropSprint = false;
      return var3 == var1.comp_3159()
            && var4 == var1.comp_3160()
            && var5 == var1.comp_3161()
            && var6 == var1.comp_3162()
            && var7 == var1.comp_3163()
            && var2 == var1.comp_3165()
         ? var1
         : new PlayerInput(var3, var4, var5, var6, var7, var1.comp_3164(), var2);
   }
}
