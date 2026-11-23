package net.efn.client.module.impl.combat.bot;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.efn.client.easyplace.builder.Aim;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
final class BotBody {
   private static final double KEEP_EXTRA = 8.0;
   private static final float FAR_OFF = 40.0F;
   private static final int FLIP_COOLDOWN = 8;
   private static final int DROP_SCAN = 16;
   private PlayerEntity target;
   private boolean grounded;
   private int side = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
   private int sideUntil;
   private int flippedAt = -100;
   private double sideCarry;
   private int tick;

   BotBody() {
   }

   PlayerEntity target() {
      return this.target;
   }

   int tick() {
      return this.tick;
   }

   void forget() {
      this.target = null;
   }

   void hold(PlayerEntity var1) {
      this.target = var1;
   }

   void stayGrounded(boolean var1) {
      this.grounded = var1;
   }

   void release(MinecraftClient var1) {
      var1.field_1690.field_1894.method_23481(false);
      var1.field_1690.field_1881.method_23481(false);
      var1.field_1690.field_1913.method_23481(false);
      var1.field_1690.field_1849.method_23481(false);
      var1.field_1690.field_1903.method_23481(false);
      var1.field_1690.field_1867.method_23481(false);
      var1.field_1690.field_1904.method_23481(false);
   }

   PlayerEntity pick(MinecraftClient var1, ClientPlayerEntity var2, double var3) {
      return this.pick(var1, var2, var3, var3 + 8.0);
   }

   PlayerEntity pick(MinecraftClient var1, ClientPlayerEntity var2, double var3, double var5) {
      this.tick++;
      if (this.tick >= this.sideUntil) {
         this.side = -this.side;
         this.sideUntil = this.tick + 10 + ThreadLocalRandom.current().nextInt(13);
      }

      if (this.target != null
         && (
            !this.target.method_5805()
               || this.target.method_31481()
               || this.target.method_73183() != var2.method_73183()
               || this.target.method_5739(var2) > var5
               || !MeleeAttacks.isEnemy(var2, (Entity)this.target)
         )) {
         this.target = null;
      }

      if (this.target != null) {
         return this.target;
      } else {
         PlayerEntity var7 = null;

         for (PlayerEntity var9 : var1.field_1687.method_18456()) {
            double var10;
            if (var9 != var2
               && var9.method_5805()
               && !var9.method_7325()
               && MeleeAttacks.isEnemy(var2, var9)
               && (var10 = var9.method_5739(var2)) <= var3
               && (var7 == null || var10 < var7.method_5739(var2))) {
               var7 = var9;
            }
         }

         this.target = var7;
         return var7;
      }
   }

   void flipSide() {
      if (this.tick - this.flippedAt >= 8) {
         this.flippedAt = this.tick;
         this.side = -this.side;
         this.sideUntil = this.tick + 10 + ThreadLocalRandom.current().nextInt(13);
      }
   }

   void face(MinecraftClient var1, ClientPlayerEntity var2, PlayerEntity var3, float var4) {
      Vec3d var5 = var2.method_33571();
      Vec3d var6 = var3.method_73189().method_1023(var3.field_6014, var3.field_6036, var3.field_5969);
      Box var7 = var3.method_5829().method_997(var6);
      Box var8 = var7.method_35580(var7.method_17939() * 0.2, var7.method_17940() * 0.15, var7.method_17941() * 0.2);
      Rotation var9 = RotationUtils.fromTo(var5, TargetingUtils.closestPoint(var5, var8));
      float var10 = Math.abs(MathHelper.method_15393(var9.yaw() - var2.method_36454()));
      Aim.turn(var1, var9.yaw(), var9.pitch(), (var10 > 40.0F ? 27.0F : 22.0F) * var4);
   }

   void drive(MinecraftClient var1, ClientPlayerEntity var2, int var3, double var4, boolean var6, boolean var7) {
      float var9 = var2.method_36454();
      Vec3d var10 = Vec3d.method_1030(0.0F, var9);
      if (var3 > 0 && safeStep(var2, var10)) {
         var1.field_1690.field_1894.method_23481(true);
         var1.field_1690.field_1867.method_23481(var6);
         if (var2.field_5976 && var2.method_24828()) {
            var7 = true;
         }
      } else if (var3 < 0 && safeStep(var2, var10.method_1021(-1.0))) {
         var1.field_1690.field_1881.method_23481(true);
      }

      if (var7 && !this.grounded && var2.method_24828() && !var2.method_5799()) {
         var1.field_1690.field_1903.method_23481(true);
      }

      this.sideCarry = this.sideCarry + Math.max(0.0, Math.min(1.0, var4));
      if (!(this.sideCarry < 1.0)) {
         this.sideCarry--;
         Vec3d var11 = Vec3d.method_1030(0.0F, var9 - 90.0F);
         Vec3d var8 = this.side > 0 ? var11 : var11.method_1021(-1.0);
         if (!safeStep(var2, var8)) {
            this.side = -this.side;
            if (!safeStep(var2, var8.method_1021(-1.0))) {
               return;
            }
         }

         (this.side > 0 ? var1.field_1690.field_1913 : var1.field_1690.field_1849).method_23481(true);
      }
   }

   void keepDistance(MinecraftClient var1, ClientPlayerEntity var2, PlayerEntity var3, double var4, double var6, double var8, boolean var10) {
      double var11 = Math.hypot(var3.method_23317() - var2.method_23317(), var3.method_23321() - var2.method_23321());
      int var13 = var11 > var6 + 0.25 ? 1 : (var11 < var4 ? -1 : 0);
      this.drive(var1, var2, var13, var10 && var11 <= var8 + 1.0 ? 1.0 : 0.0, var11 > var8, false);
   }

   void flee(MinecraftClient var1, ClientPlayerEntity var2, PlayerEntity var3) {
      float var4 = RotationUtils.fromTo(var3.method_33571(), var2.method_33571()).yaw();
      Aim.turn(var1, var4, 5.0F, 27.0F);
      this.drive(var1, var2, 1, 0.0, true, false);
   }

   void away(MinecraftClient var1, ClientPlayerEntity var2, Vec3d var3) {
      Vec3d var4 = var2.method_73189().method_1020(var3).method_18805(1.0, 0.0, 1.0);
      if (var4.method_1027() < 1.0E-6) {
         var4 = Vec3d.method_1030(0.0F, var2.method_36454()).method_1021(-1.0);
      }

      this.go(var1, var2, var4);
   }

   boolean toward(MinecraftClient var1, ClientPlayerEntity var2, Vec3d var3) {
      Vec3d var4 = var3.method_1020(var2.method_73189()).method_18805(1.0, 0.0, 1.0);
      if (var4.method_1027() < 0.09) {
         return true;
      } else {
         this.go(var1, var2, var4);
         if (!this.grounded && var2.field_5976 && var2.method_24828()) {
            var1.field_1690.field_1903.method_23481(true);
         }

         return false;
      }
   }

   void dropToward(MinecraftClient var1, ClientPlayerEntity var2, Vec3d var3) {
      Vec3d var4 = var3.method_1020(var2.method_73189()).method_18805(1.0, 0.0, 1.0);
      if (!(var4.method_1027() < 0.09) && safeStep(var2, var4.method_1029(), 16)) {
         keys(var1, var2, var4);
      }
   }

   private void go(MinecraftClient var1, ClientPlayerEntity var2, Vec3d var3) {
      if (safeStep(var2, var3.method_1029())) {
         keys(var1, var2, var3);
      }
   }

   private static void keys(MinecraftClient var0, ClientPlayerEntity var1, Vec3d var2) {
      float var3 = (float)Math.toDegrees(Math.atan2(-var2.field_1352, var2.field_1350));
      float var4 = MathHelper.method_15393(var3 - var1.method_36454());
      float var5 = Math.abs(var4);
      if (var5 <= 67.5F) {
         var0.field_1690.field_1894.method_23481(true);
      }

      if (var5 >= 112.5F) {
         var0.field_1690.field_1881.method_23481(true);
      }

      if (var4 <= -22.5F && var4 >= -157.5F) {
         var0.field_1690.field_1913.method_23481(true);
      }

      if (var4 >= 22.5F && var4 <= 157.5F) {
         var0.field_1690.field_1849.method_23481(true);
      }
   }

   void backOff(MinecraftClient var1, ClientPlayerEntity var2) {
      this.drive(var1, var2, -1, 0.0, false, false);
   }

   boolean use(MinecraftClient var1, ClientPlayerEntity var2, Predicate<ItemStack> var3) {
      int var4 = hotbarSlot(var2, var3);
      if (var4 < 0) {
         return false;
      } else {
         PlayerInventory var5 = var2.method_31548();
         if (var5.method_67532() != var4) {
            var5.method_61496(var4);
            return true;
         } else {
            var1.field_1690.field_1904.method_23481(true);
            return true;
         }
      }
   }

   static int hotbarSlot(ClientPlayerEntity var0, Predicate<ItemStack> var1) {
      PlayerInventory var2 = var0.method_31548();
      if (var1.test(var2.method_5438(var2.method_67532()))) {
         return var2.method_67532();
      } else {
         for (int var3 = 0; var3 < 9; var3++) {
            if (var1.test(var2.method_5438(var3))) {
               return var3;
            }
         }

         return -1;
      }
   }

   static boolean safeStep(ClientPlayerEntity var0, Vec3d var1) {
      return safeStep(var0, var1, 3);
   }

   private static boolean safeStep(ClientPlayerEntity var0, Vec3d var1, int var2) {
      World var3 = var0.method_73183();
      BlockPos var4 = BlockPos.method_49637(var0.method_23317() + var1.field_1352 * 0.8, var0.method_23318() + 0.1, var0.method_23321() + var1.field_1350 * 0.8);

      for (int var5 = 0; var5 <= var2; var5++) {
         BlockPos var6 = var4.method_10087(var5);
         BlockState var7 = var3.method_8320(var6);
         if (var7.method_27852(Blocks.field_10164)
            || var7.method_26164(BlockTags.field_21952)
            || var7.method_27852(Blocks.field_10029)
            || var7.method_27852(Blocks.field_16999)
            || var7.method_27852(Blocks.field_10343)
            || var5 <= 1 && var7.method_27852(Blocks.field_10092)) {
            return false;
         }

         if (!var7.method_26220(var3, var6).method_1110()) {
            return true;
         }
      }

      return false;
   }
}
