package net.efn.client.module.impl.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public final class AutoTrapModule extends ClientModule {
   private static final Direction[] SUPPORT_ORDER = new Direction[]{
      Direction.field_11033, Direction.field_11043, Direction.field_11035, Direction.field_11034, Direction.field_11039, Direction.field_11036
   };
   private static final int TARGET_COLOR = -24512;
   private static final int CELL_COLOR = -2130706433;
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 4.0F, 1.0F, 6.0F).step(0.5F));
   private final BooleanSetting rotate = this.addSetting("rotate", new BooleanSetting("Rotate", true));
   private final NumberSetting perTick = this.addSetting("blocks-per-tick", new NumberSetting("Blocks Per Tick", 4.0F, 1.0F, 8.0F).step(1.0F));
   private final BooleanSetting top = this.addSetting("top-block", new BooleanSetting("Top Block", true));
   private final BooleanSetting show = this.addSetting("show-target", new BooleanSetting("Show Target", true));
   private final MinecraftClient mc = MinecraftClient.method_1551();
   private final SlotReturn slot = new SlotReturn(this);
   private PlayerEntity target;
   private List<BlockPos> plan = List.of();
   private boolean warnedEmpty;

   public AutoTrapModule() {
      super("auto-trap", "Auto Trap", "Boxes the nearest enemy in with obsidian: sides at feet and head height, then the top.", ModuleCategory.COMBAT);
      this.perTick.visibleWhen(() -> !this.rotate.get());
   }

   @Override
   protected void onEnable() {
      this.target = null;
      this.plan = List.of();
      this.warnedEmpty = false;
      this.on(TickEvent.TYPE, Stage.PRE, var1 -> this.tick());
      this.on(Render3DEvent.TYPE, Stage.POST, var1 -> this.render());
   }

   @Override
   protected void onDisable() {
      this.slot.drop(this.mc, true);
      this.target = null;
      this.plan = List.of();
   }

   private void tick() {
      ClientPlayerEntity var1 = this.mc.field_1724;
      ClientWorld var2 = this.mc.field_1687;
      if (var1 != null && var2 != null && this.mc.field_1761 != null && MeleeAttacks.canAct(this.mc)) {
         this.target = this.pick(var1);
         this.plan = this.target == null ? List.of() : this.cells(var2, var1, this.target);
         if (this.plan.isEmpty()) {
            this.slot.tick(this.mc, true);
         } else {
            int var3 = Crystals.hotbarSlotOf(var1.method_31548(), Items.field_8281);
            if (var3 < 0) {
               if (!this.warnedEmpty) {
                  ModuleChat.send(this, ModuleChat.body().warn("No obsidian in the hotbar"));
               }

               this.warnedEmpty = true;
               this.slot.tick(this.mc, true);
            } else {
               this.warnedEmpty = false;
               int var4 = this.rotate.get() ? 1 : Math.round(this.perTick.get());
               int var5 = 0;

               for (BlockPos var7 : this.plan) {
                  if (var5 < var4) {
                     BlockHitResult var8 = support(var2, var1, var7);
                     if (var8 == null) {
                        continue;
                     }

                     if (this.rotate.get()) {
                        BlockHitResult var9 = seenByServer(var2, var1, var8);
                        if (var9 == null) {
                           ServerRotation.request(RotationUtils.fromTo(var1.method_33571(), var8.method_17784()));
                           return;
                        }

                        var8 = var9;
                     }

                     if (var5 != 0 || !MeleeAttacks.actedThisTick(var1) && MeleeAttacks.handsFree(this.mc, var1)) {
                        if (!this.slot.take(var1, var3)) {
                           return;
                        }

                        ActionResult var10 = this.mc.field_1761.method_2896(var1, Hand.field_5808, var8);
                        if (var10.method_23665()) {
                           var1.method_6104(Hand.field_5808);
                           MeleeAttacks.noteAction(var1);
                           var5++;
                        }
                        continue;
                     }

                     return;
                  }
                  break;
               }

               if (this.rotate.get() && var5 > 0) {
                  ServerRotation.request(ServerRotation.sent());
               }

               return;
            }
         }
      } else {
         this.slot.tick(this.mc, true);
      }
   }

   private PlayerEntity pick(ClientPlayerEntity var1) {
      double var2 = this.range.get() + 1.5;
      PlayerEntity var4 = null;

      for (PlayerEntity var6 : this.mc.field_1687.method_18456()) {
         double var7;
         if (var6 != var1
            && var6.method_5805()
            && MeleeAttacks.isEnemy(var1, var6)
            && (var7 = var6.method_5739(var1)) <= var2
            && (var4 == null || var7 < var4.method_5739(var1))) {
            var4 = var6;
         }
      }

      return var4;
   }

   private List<BlockPos> cells(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3) {
      Box var4 = var3.method_5829();
      int var5 = MathHelper.method_15357(var4.field_1322 + 0.01);
      LinkedHashSet var6 = new LinkedHashSet();

      for (int var7 = MathHelper.method_15357(var4.field_1323); var7 <= MathHelper.method_15357(var4.field_1320 - 1.0E-4); var7++) {
         for (int var8 = MathHelper.method_15357(var4.field_1321); var8 <= MathHelper.method_15357(var4.field_1324 - 1.0E-4); var8++) {
            var6.add(BlockPos.method_10064(var7, 0, var8));
         }
      }

      ArrayList var17 = new ArrayList();

      for (int var18 = 0; var18 <= 1; var18++) {
         ArrayList var9 = new ArrayList();

         for (long var11 : var6) {
            BlockPos var13 = BlockPos.method_10092(var11).method_33096(var5 + var18);

            for (Direction var15 : Type.field_11062) {
               BlockPos var16 = var13.method_10093(var15);
               if (!var6.contains(BlockPos.method_10064(var16.method_10263(), 0, var16.method_10260()))
                  && this.open(var1, var2, var16)
                  && !var9.contains(var16)) {
                  var9.add(var16);
               }
            }
         }

         var9.sort(Comparator.comparingDouble(var1x -> var1x.method_19770(var2.method_33571())));
         var17.addAll(var9);
      }

      if (this.top.get()) {
         for (long var20 : var6) {
            BlockPos var21 = BlockPos.method_10092(var20).method_33096(var5 + 2);
            if (this.open(var1, var2, var21)) {
               var17.add(var21);
            }
         }
      }

      return var17;
   }

   private boolean open(ClientWorld var1, ClientPlayerEntity var2, BlockPos var3) {
      BlockState var4 = var1.method_8320(var3);
      if (var4.method_45474() && (var4.method_26227().method_15769() || var4.method_26227().method_15771())) {
         return var3.method_46558().method_1022(var2.method_33571()) > var2.method_55754()
            ? false
            : var1.method_8628(Blocks.field_10540.method_9564(), var3, ShapeContext.method_16194());
      } else {
         return false;
      }
   }

   public static BlockHitResult support(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2) {
      Vec3d var3 = var1.method_33571();
      double var4 = var1.method_55754();

      for (Direction var9 : SUPPORT_ORDER) {
         BlockPos var11 = var2.method_10093(var9);
         BlockState var12 = var0.method_8320(var11);
         if (!var12.method_45474() && !var12.method_26220(var0, var11).method_1110()) {
            Direction var13 = var9.method_10153();
            Vec3d var14 = Vec3d.method_24953(var11).method_1031(var13.method_10148() * 0.5, var13.method_10164() * 0.5, var13.method_10165() * 0.5);
            BlockHitResult var10;
            if (!(var14.method_1022(var3) > var4)
               && (var10 = var0.method_17742(
                        new RaycastContext(
                           var3,
                           var14.method_1019(Vec3d.method_24954(var9.method_62675()).method_1021(0.05)),
                           ShapeType.field_17559,
                           FluidHandling.field_1348,
                           var1
                        )
                     ))
                     .method_17783()
                  == net.minecraft.util.hit.HitResult.Type.field_1332
               && var10.method_17777().equals(var11)
               && var10.method_17780() == var13) {
               return new BlockHitResult(var14, var13, var11, false);
            }
         }
      }

      return null;
   }

   public static BlockHitResult seenByServer(ClientWorld var0, ClientPlayerEntity var1, BlockHitResult var2) {
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
         return var6.method_17783() == net.minecraft.util.hit.HitResult.Type.field_1332
               && var6.method_17777().equals(var2.method_17777())
               && var6.method_17780() == var2.method_17780()
            ? var6
            : null;
      }
   }

   private void render() {
      if (this.show.get() && this.target != null) {
         GizmoDrawing.method_75541(this.target.method_5829(), DrawStyle.method_75536(-24512, 2.0F));

         for (BlockPos var2 : this.plan) {
            GizmoDrawing.method_75550(var2, DrawStyle.method_75536(-2130706433, 1.0F));
         }
      }
   }
}
