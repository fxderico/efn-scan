package net.efn.client.module.impl.combat;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.Combo;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class AutoCityModule extends ClientModule {
   private static final float CRYSTAL_POWER = 6.0F;
   private static final double FACE_INSET = 0.05;
   private static final int GIVE_UP_TICKS = 200;
   private static volatile AutoCityModule active;
   private final SlotReturn slots = new SlotReturn(this);
   private LivingEntity target;
   private BlockPos feet;
   private BlockPos block;
   private int ticks;
   private boolean broken;

   public AutoCityModule() {
      super(
         "auto-city",
         "Auto City",
         "Mines a surround block of the best enemy in a hole, then turns off so Crystal Aura can blow through it.",
         ModuleCategory.COMBAT
      );
      this.usesNotifications();
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.target = null;
      this.feet = null;
      this.block = null;
      this.ticks = 0;
      this.broken = false;
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (!this.broken && this.block != null && var1.field_1761 != null && var1.field_1761.method_2923()) {
         var1.field_1761.method_2925();
      }

      this.slots.drop(var1, true);
      CombatMacroLock.release(this);
      this.target = null;
      this.block = null;
   }

   public static boolean mining() {
      AutoCityModule var0 = active;
      return var0 != null && var0.block != null && !var0.broken;
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var3 == null || var2.field_1687 == null || var2.field_1761 == null || var3.method_29504() || this.broken || userClicks(var2)) {
         this.requestDisable();
      } else if (this.block == null && !this.pick(var2, var3)) {
         this.stop("No enemy in a hole within reach");
      } else {
         ClientWorld var4 = var2.field_1687;
         BlockState var5 = var4.method_8320(this.block);
         if (var5.method_45474()) {
            this.opened();
         } else {
            this.ticks++;
            String var6 = this.problem(var3, var5);
            if (var6 != null) {
               this.stop(var6);
            } else if (MeleeAttacks.canAct(var2) && !AutoInvTotemModule.wantsHotbar() && !var3.method_6115() && !var3.method_3144()) {
               if (CombatMacroLock.tryAcquire(this) && this.slots.take(var3, toolSlot(var3.method_31548(), var5))) {
                  BlockHitResult var7 = aim(var4, var3, this.block);
                  if (var7 == null) {
                     this.stop("Lost sight of the block");
                  } else {
                     ServerRotation.request(RotationUtils.fromTo(var3.method_33571(), var7.method_17784()));
                     BlockHitResult var8 = held(var4, var3, this.block);
                     if (var8 != null && !MeleeAttacks.actedThisTick(var3)) {
                        if (var2.field_1761.method_2902(this.block, var8.method_17780())) {
                           var3.method_6104(Hand.field_5808);
                        }

                        MeleeAttacks.noteAction(var3);
                        if (var4.method_8320(this.block).method_45474()) {
                           this.opened();
                           this.note(ModuleChat.body().text("Opened ").name(this.target.method_5477().getString()).text("'s surround"));
                        }
                     }
                  }
               }
            } else {
               if (var2.field_1761.method_2923()) {
                  var2.field_1761.method_2925();
               }

               CombatMacroLock.release(this);
            }
         }
      }
   }

   private void opened() {
      this.broken = true;
      CombatMacroLock.release(this);
   }

   private void stop(String var1) {
      this.note(ModuleChat.body().text(var1));
      this.requestDisable();
   }

   private static boolean userClicks(MinecraftClient var0) {
      return var0.field_1755 == null
         && (
            Crystals.isPhysicallyDown(var0, var0.field_1690.field_1886)
               || MeleeAttacks.clickPending(var0)
               || Crystals.isPhysicallyDown(var0, var0.field_1690.field_1904)
         );
   }

   private boolean pick(MinecraftClient var1, ClientPlayerEntity var2) {
      ClientWorld var3 = var1.field_1687;
      float var4 = 0.0F;

      for (LivingEntity var6 : Combo.enemies(var1, var2)) {
         if (Combo.inHole(var6)) {
            BlockPos var7 = var6.method_24515();

            for (Direction var9 : Type.field_11062) {
               BlockPos var13 = var7.method_10093(var9);
               BlockState var14 = var3.method_8320(var13);
               float var10;
               Vec3d var11;
               float var12;
               if (!(var14.method_26214(var3, var13) < 0.0F)
                  && Crystals.isBase(var3.method_8320(var13.method_10074()))
                  && var2.method_56093(var13, 0.0)
                  && toolSlot(var2.method_31548(), var14) != -1
                  && aim(var3, var2, var13) != null
                  && !CrystalDamage.endangers(var2, var12 = CrystalDamage.estimate(var11 = Crystals.explosionAt(var13.method_10074()), var2, 6.0F, var13))
                  && !((var10 = CrystalDamage.estimate(var11, var6, 6.0F, var13) - 0.5F * var12) <= var4)) {
                  var4 = var10;
                  this.target = var6;
                  this.feet = var7;
                  this.block = var13;
               }
            }
         }
      }

      return this.block != null;
   }

   private String problem(ClientPlayerEntity var1, BlockState var2) {
      BlockPos var3 = this.target.method_24515();
      if (!this.target.method_5805()
         || var3.method_10263() != this.feet.method_10263()
         || var3.method_10260() != this.feet.method_10260()
         || Math.abs(var3.method_10264() - this.feet.method_10264()) > 1) {
         return "Target left the hole";
      } else if (!var1.method_56093(this.block, 0.0)) {
         return "Block went out of reach";
      } else if (toolSlot(var1.method_31548(), var2) == -1) {
         return "No pickaxe that can break it";
      } else if (!ServerRotation.canRotate(var1)) {
         return "Can't aim while flying, swimming or riding";
      } else {
         return this.ticks > 200 ? "Gave up, the block took too long" : null;
      }
   }

   private void note(ModuleChat.Body var1) {
      if (this.notificationsOn()) {
         ModuleChat.actionBar(this, var1);
      }
   }

   private static int toolSlot(PlayerInventory var0, BlockState var1) {
      int var2 = -1;
      float var3 = 0.0F;

      for (int var4 = 0; var4 < PlayerInventory.method_7368(); var4++) {
         ItemStack var5 = var0.method_5438(var4);
         if (var5.method_7951(var1) && !(var5.method_7924(var1) <= var3)) {
            var2 = var4;
            var3 = var5.method_7924(var1);
         }
      }

      return var2;
   }

   private static BlockHitResult aim(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2) {
      Vec3d var3 = var1.method_33571();
      double var4 = var1.method_55754();
      BlockHitResult var6 = null;
      double var7 = var4 * var4;

      for (Direction var12 : Direction.values()) {
         Vec3d var14 = Vec3d.method_24953(var2).method_43206(var12, 0.5);
         double var15 = var3.method_1025(var14);
         BlockHitResult var13;
         if (!(var15 > var7)
            && (var13 = var0.method_17742(new RaycastContext(var3, var14.method_43206(var12, -0.05), ShapeType.field_17559, FluidHandling.field_1348, var1)))
                  .method_17783()
               == net.minecraft.util.hit.HitResult.Type.field_1332
            && var13.method_17777().equals(var2)
            && var13.method_17780() == var12) {
            var6 = var13;
            var7 = var15;
         }
      }

      return var6;
   }

   private static BlockHitResult held(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2) {
      Rotation var4 = ServerRotation.sent();
      if (var4 == null) {
         return null;
      } else {
         Vec3d var5 = var1.method_33571();
         BlockHitResult var6 = var0.method_17742(
            new RaycastContext(
               var5,
               var5.method_1019(Vec3d.method_1030(var4.pitch(), var4.yaw()).method_1021(var1.method_55754())),
               ShapeType.field_17559,
               FluidHandling.field_1348,
               var1
            )
         );
         return var6.method_17783() == net.minecraft.util.hit.HitResult.Type.field_1332 && var6.method_17777().equals(var2) ? var6 : null;
      }
   }
}
