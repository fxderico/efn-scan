package net.efn.client.module.impl.combat;

import imgui.ImDrawList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.anchor.AnchorGuard;
import net.efn.client.module.impl.combat.crystal.Combo;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.crystal.HurtClock;
import net.efn.client.module.impl.combat.crystal.LootGuard;
import net.efn.client.module.impl.combat.crystal.ShotView;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class AnchorAuraModule extends ClientModule implements HudOverlayProvider {
   private static final double REACH_MARGIN = 0.01;
   private static final int ARM_TICKS = 2;
   private static final int HOLD_TICKS = 10;
   private static final int MAX_LEAD_TICKS = 5;
   private static final int AIM_TIMEOUT_TICKS = 12;
   private static final int SKIP_TICKS = 40;
   private final NumberSetting minDamage = this.addSetting("min-damage", new NumberSetting("Min Damage", 6.0F, 0.0F, 20.0F).step(0.5F));
   private final NumberSetting maxSelfDamage = this.addSetting("max-self-damage", new NumberSetting("Max Self Damage", 6.0F, 0.0F, 20.0F).step(0.5F));
   private final BooleanSetting render = this.addSetting("render", new BooleanSetting("Render", true));
   private final ColorSetting renderColor = this.addSetting("render-color", new ColorSetting("Render Color", -26052));
   private final NumberSetting turnSpeed = this.addSetting("turn-speed", new NumberSetting("Turn Speed", 4.5F, 1.0F, 10.0F).step(0.5F));
   private final BooleanSetting protectLoot = this.addSetting("protect-loot", new BooleanSetting("Protect Loot", true));
   private final SlotReturn slots = new SlotReturn(this);
   private final ShotView view = new ShotView();
   private int tick;
   private int holdUntil;
   private BlockPos spent;
   private int spentUntil;
   private float aimTurn;
   private float placedTurn;
   private BlockPos aimingAt;
   private int aimingTicks;
   private BlockPos skipped;
   private int skippedUntil;
   private List<CrystalDamage.Victim> lastTargets;
   private List<CrystalDamage.Victim> lastSpared;
   private int lastRtt;
   private float steerMin;
   private float steerSelf;
   private int steerUntil = -1;
   private float tickMin;
   private float tickSelf;

   public AnchorAuraModule() {
      super("anchor-aura", "Anchor Aura", "Places, charges and detonates respawn anchors on nearby enemies.", ModuleCategory.COMBAT);
      this.renderColor.visibleWhen(this.render::get);
   }

   @Override
   protected void onDisable() {
      this.slots.drop(MinecraftClient.method_1551(), true);
      this.spent = null;
      this.view.clear();
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      this.tick++;
      MinecraftClient var3 = MinecraftClient.method_1551();
      ClientPlayerEntity var4 = var3.field_1724;
      if (var4 != null && var3.field_1687 != null) {
         LootGuard.update(var3.field_1687, var4);
      }

      CrystalAuraModule var2;
      if (((var2 = ModuleRegistry.get(CrystalAuraModule.class)) == null || !var2.isEnabled())
         && MeleeAttacks.canAct(var3)
         && !var4.method_7325()
         && !AutoInvTotemModule.wantsHotbar()
         && !AutoCityModule.mining()
         && !CombatMacroLock.heldByOther(this)
         && var3.field_1687.method_27983() != World.field_25180) {
         HurtClock.tick(var4);
         if (this.tick > this.holdUntil) {
            this.slots.tick(var3, true);
         }

         if (!MeleeAttacks.actedThisTick(var4) && MeleeAttacks.handsFree(var3, var4) && !var4.method_21823() && !MeleeAttacks.rightClickComing(var3)) {
            Combo.Shot var5 = Combo.pick(this.shots(var3, var4, Combo.enemies(var3, var4), Combo.friends(var3, var4), Crystals.rttTicks(var3, var4), 0.0F));
            if (var5 == null) {
               if (this.spent != null && this.tick < this.spentUntil && var3.field_1687.method_8320(this.spent).method_27852(Blocks.field_23152)) {
                  ServerRotation.request(ServerRotation.sent());
               }

               if (this.tick > this.holdUntil) {
                  this.release();
               }
            } else {
               this.show(var5);
               if (CombatMacroLock.tryAcquire(this)) {
                  if (!this.ready(var3, var4, var5)) {
                     BlockPos var6 = cellOf(var5);
                     this.aimingTicks = var6.equals(this.aimingAt) ? this.aimingTicks + 1 : 0;
                     this.aimingAt = var6;
                     if (this.aimingTicks >= 12) {
                        this.skipped = var6;
                        this.skippedUntil = this.tick + 40;
                        this.aimingAt = null;
                     } else {
                        ServerRotation.request(RotationUtils.fromTo(var4.method_33571(), var5.aim()), this.turnSpeed.get());
                     }
                  } else {
                     this.aimingAt = null;
                     if (var5.route() == -2 || this.slots.take(var4, var5.route())) {
                        this.fire(var3, var4, var5);
                        this.holdUntil = this.tick + 10;
                        ServerRotation.request(ServerRotation.sent());
                     }
                  }
               }
            }
         }
      } else {
         this.slots.tick(var3, true);
         this.release();
      }
   }

   @EventHandler
   public void onPacketReceive(PacketReceiveEvent var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 != null) {
         HurtClock.note(var2, var1.packet);
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      if (this.render.get()) {
         this.view.draw();
      }
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.render.get() && this.view.showing();
   }

   @Override
   public void renderHudOverlay(ImDrawList var1, float var2, float var3) {
      this.view.label(var1, var2, var3);
   }

   private static BlockPos cellOf(Combo.Shot var0) {
      BlockPos var1;
      if (var0.plan() instanceof AnchorGuard.Spot var3) {
         var1 = var3.pos();
      } else {
         var1 = (BlockPos)var0.plan();
      }

      return var1;
   }

   private boolean ruledOut(BlockPos var1) {
      return var1.equals(this.skipped) && this.tick < this.skippedUntil || this.protectLoot.get() && LootGuard.near(Vec3d.method_24953(var1));
   }

   public void show(Combo.Shot var1) {
      if (this.render.get()) {
         BlockPos var2 = cellOf(var1);
         Vec3d var3 = Vec3d.method_24953(var2);
         float var4 = 0.0F;

         for (CrystalDamage.Victim var6 : this.lastTargets) {
            var4 = Math.max(var4, var6.damage(var3, 5.0F, var2));
         }

         float var7 = this.lastSpared.get(0).damage(var3, 5.0F, var2);
         this.view.show(new Box(var2), this.renderColor.argb(), var4, var7);
      }
   }

   public void steer(ClientPlayerEntity var1, float var2, float var3) {
      this.steerMin = var2;
      this.steerSelf = var3;
      this.steerUntil = var1.field_6012 + 2;
   }

   public List<Combo.Shot> shots(MinecraftClient var1, ClientPlayerEntity var2, List<LivingEntity> var3, List<LivingEntity> var4, int var5, float var6) {
      ClientWorld var9 = var1.field_1687;
      boolean var10 = var2.field_6012 <= this.steerUntil;
      this.tickMin = var10 ? this.steerMin : this.minDamage.get();
      this.tickSelf = var10 ? this.steerSelf : this.maxSelfDamage.get();
      PlayerInventory var11 = var2.method_31548();
      ItemStack var12 = var2.method_6047();
      int var13 = AnchorGuard.detonatorRoute(var11, var12);
      if (var9.method_27983() != World.field_25180
         && ServerRotation.canRotate(var2)
         && !var2.method_6079().method_31574(Items.field_8801)
         && !var2.method_21823()
         && !var3.isEmpty()
         && var13 != -1) {
         ArrayList var14 = new ArrayList();
         var14.add(var2);
         var14.addAll(var4);
         this.lastRtt = var5;
         this.lastTargets = victims(var3, var5);
         this.lastSpared = victims(var14, var5);
         ArrayList var15 = new ArrayList();
         int var16 = routeOf(var11, var12, Items.field_8801);
         boolean var17 = false;

         for (BlockPos var19 : this.anchors(var9, var2, var3)) {
            boolean var20;
            int var21;
            AnchorAuraModule.Blast var22;
            Vec3d var23;
            if (!this.ruledOut(var19)
               && (var23 = AnchorGuard.anchorAim(var9, var2, var19)) != null
               && this.safe(this.lastSpared, var19)
               && (
                  (
                           var22 = this.blast(
                              var19, var21 = (var20 = (Integer)var9.method_8320(var19).method_11654(RespawnAnchorBlock.field_23153) > 0) ? 0 : 1, false
                           )
                        )
                        != null
                     || this.blast(var19, var21, true) != null
               )) {
               var17 = true;
               if (var22 != null) {
                  if (var20) {
                     var15.add(new Combo.Shot(Combo.Kind.DETONATE, var23, var13, var22.value(), var22.kills(), 0, var19));
                  } else if (var16 != -1 && HurtClock.opensIn(var22.source(), var5) <= 1 && (var6 <= 0.0F || var22.value() > var6 + 1.0F)) {
                     var15.add(new Combo.Shot(Combo.Kind.CHARGE, var23, var16, var22.value(), var22.kills(), 1, var19));
                  }
               }
            }
         }

         int var24 = routeOf(var11, var12, Items.field_23141);
         Combo.Shot var8;
         if (!var17
            && var24 != -1
            && var16 != -1
            && (var8 = this.place(var9, var2, var24, Crystals.reservedBoxes(var2, Crystals.confirmWindow(var1, var2)))) != null) {
            var15.add(var8);
         }

         return var15;
      } else {
         return List.of();
      }
   }

   public boolean ready(MinecraftClient var1, ClientPlayerEntity var2, Combo.Shot var3) {
      ClientWorld var5 = var1.field_1687;
      if (var3.kind() == Combo.Kind.ANCHOR) {
         AnchorGuard.Spot var9 = (AnchorGuard.Spot)var3.plan();
         Rotation var10 = ServerRotation.sent();
         if (var10 != null && AnchorGuard.hitAlong(var5, var2, var10, var9) != null) {
            return true;
         } else {
            float var8 = turnTo(var2, var9);
            if (var8 > 0.0F) {
               this.aimTurn = var8;
            }

            return false;
         }
      } else {
         BlockPos var6 = (BlockPos)var3.plan();
         BlockState var7 = var5.method_8320(var6);
         if (var7.method_27852(Blocks.field_23152) && AnchorGuard.anchorHit(var5, var2, var6) != null) {
            boolean var4 = (Integer)var7.method_11654(RespawnAnchorBlock.field_23153) > 0;
            return var3.kind() == Combo.Kind.CHARGE ? !var4 : var4 && this.safe(this.lastSpared, var6) && this.blast(var6, 0, false) != null;
         } else {
            return false;
         }
      }
   }

   public void fire(MinecraftClient var1, ClientPlayerEntity var2, Combo.Shot var3) {
      ClientWorld var4 = var1.field_1687;
      switch (var3.kind()) {
         case ANCHOR:
            AnchorGuard.Spot var10 = (AnchorGuard.Spot)var3.plan();
            AnchorGuard.click(var1, var2, AnchorGuard.hitAlong(var4, var2, ServerRotation.sent(), var10));
            if (this.aimTurn > 0.0F) {
               this.placedTurn = this.aimTurn;
            }
            break;
         case CHARGE:
            AnchorGuard.click(var1, var2, AnchorGuard.anchorHit(var4, var2, (BlockPos)var3.plan()));
            break;
         case DETONATE:
            BlockPos var5 = (BlockPos)var3.plan();
            AnchorGuard.click(var1, var2, AnchorGuard.anchorHit(var4, var2, var5));
            this.spent = var5;
            this.spentUntil = this.tick + Crystals.confirmWindow(var1, var2);
            Vec3d var6 = Vec3d.method_24953(var5);

            for (CrystalDamage.Victim var8 : this.lastTargets) {
               float var9 = var8.raw(var6, 5.0F, var5);
               if (var9 > 0.0F) {
                  HurtClock.shot(var2, var8.entity(), var9, var6, this.lastRtt);
               }
            }
      }
   }

   private Combo.Shot place(ClientWorld var1, ClientPlayerEntity var2, int var3, List<Box> var4) {
      LinkedHashMap var5 = new LinkedHashMap();

      for (CrystalDamage.Victim var7 : this.lastTargets) {
         BlockPos var8 = var7.entity().method_24515();

         for (BlockPos var10 : BlockPos.method_10097(var8.method_10069(-2, -1, -2), var8.method_10069(2, 2, 2))) {
            float var11;
            if (!var5.containsKey(var10)
               && var1.method_8320(var10).method_26215()
               && var2.method_56093(var10, -0.01)
               && !reservedAt(var4, var10)
               && !this.ruledOut(var10)
               && !this.walkedInto(var1, var10)
               && (var11 = this.upperValue(Vec3d.method_24953(var10))) >= 0.0F) {
               var5.put(var10.method_10062(), var11);
            }
         }
      }

      ArrayList var16 = new ArrayList(var5.keySet());
      var16.sort((var1x, var2x) -> Float.compare((Float)var5.get(var2x), (Float)var5.get(var1x)));
      Vec3d var17 = var2.method_33571();
      Combo.Shot var18 = null;

      for (BlockPos var20 : var16) {
         if (var18 != null && (Float)var5.get(var20) <= var18.value()) {
            break;
         }

         AnchorAuraModule.Blast var15 = this.blast(var20, 2, true);
         Rotation var12;
         AnchorGuard.Spot var14;
         float var21;
         if (var15 != null
            && (var18 == null || !(var15.value() <= var18.value()))
            && this.safe(this.lastSpared, var20)
            && (var14 = AnchorGuard.supportFor(var1, var2, var17, var20)) != null
            && !new Box(var20)
               .method_992(
                  var17,
                  var17.method_1019(
                     Vec3d.method_1030((var12 = ServerRotation.next(var2, RotationUtils.fromTo(var17, var14.aim()))).pitch(), var12.yaw())
                        .method_1021(var2.method_55754())
                  )
               )
               .isEmpty()
            && (!((var21 = turnTo(var2, var14)) > 2.0F) || !(Math.abs(var21 - this.placedTurn) < 1.0E-4F))) {
            var18 = new Combo.Shot(Combo.Kind.ANCHOR, var14.aim(), var3, var15.value(), var15.kills(), 2, var14);
         }
      }

      return var18;
   }

   private Set<BlockPos> anchors(ClientWorld var1, ClientPlayerEntity var2, List<LivingEntity> var3) {
      LinkedHashSet var4 = new LinkedHashSet();

      for (LivingEntity var6 : var3) {
         BlockPos var7 = var6.method_24515();

         for (BlockPos var9 : BlockPos.method_10097(var7.method_10069(-4, -3, -4), var7.method_10069(4, 4, 4))) {
            if (var1.method_8320(var9).method_27852(Blocks.field_23152)
               && var2.method_56093(var9, -0.01)
               && (!var9.equals(this.spent) || this.tick >= this.spentUntil)) {
               var4.add(var9.method_10062());
            }
         }
      }

      return var4;
   }

   private AnchorAuraModule.Blast blast(BlockPos var1, int var2, boolean var3) {
      Vec3d var4 = Vec3d.method_24953(var1);
      LivingEntity var5 = null;
      float var6 = 0.0F;
      boolean var7 = false;

      for (CrystalDamage.Victim var9 : this.lastTargets) {
         LivingEntity var11 = var9.entity();
         int var10 = var3 ? Math.max(var2, HurtClock.opensIn(var11, this.lastRtt)) : var2;
         float var13 = HurtClock.effective(var9, var9.raw(var4, 5.0F, var1), var10, this.lastRtt);
         if (!(var13 < this.needed(var11))) {
            var7 = var7 || Combo.kills(var13, var11);
            float var14 = Combo.value(var13, var11);
            if (var5 == null || var14 > var6) {
               var5 = var11;
               var6 = var14;
            }
         }
      }

      if (var5 == null) {
         return null;
      } else {
         float var15 = this.lastSpared.get(0).damage(var4, 5.0F, var1);
         return new AnchorAuraModule.Blast(var6 - 0.5F * var15, var7, var5);
      }
   }

   private float upperValue(Vec3d var1) {
      float var2 = -1.0F;

      for (CrystalDamage.Victim var4 : this.lastTargets) {
         float var5 = var4.upperBound(var1, 5.0F);
         if (var5 >= this.needed(var4.entity())) {
            var2 = Math.max(var2, Combo.value(var5, var4.entity()));
         }
      }

      return var2;
   }

   private boolean safe(List<CrystalDamage.Victim> var1, BlockPos var2) {
      Vec3d var3 = Vec3d.method_24953(var2);

      for (CrystalDamage.Victim var5 : var1) {
         float var6 = var5.damage(var3, 5.0F, var2);
         if (var6 > this.tickSelf || CrystalDamage.endangers((PlayerEntity)var5.entity(), var6)) {
            return false;
         }
      }

      return true;
   }

   private float needed(LivingEntity var1) {
      return Math.min(this.tickMin, CrystalDamage.health(var1));
   }

   private static List<CrystalDamage.Victim> victims(List<LivingEntity> var0, int var1) {
      ArrayList var2 = new ArrayList(var0.size());

      for (LivingEntity var4 : var0) {
         int var5 = var4 instanceof ClientPlayerEntity ? 0 : Math.min(var1, 5);
         var2.add(new CrystalDamage.Victim(var4, Combo.lead(var4, var5)));
      }

      return var2;
   }

   private boolean walkedInto(ClientWorld var1, BlockPos var2) {
      Box var3 = new Box(var2);
      int var4 = Math.min(this.lastRtt, 5);

      for (PlayerEntity var6 : var1.method_18456()) {
         Box var7;
         if (!var6.method_7325()
            && !(var6 instanceof ClientPlayerEntity)
            && ((var7 = var6.method_5829()).method_994(var3) || var7.method_997(Combo.lead(var6, var4)).method_994(var3))) {
            return true;
         }
      }

      return false;
   }

   private static boolean reservedAt(List<Box> var0, BlockPos var1) {
      for (Box var3 : var0) {
         if (var3.method_71152(var1)) {
            return true;
         }
      }

      return false;
   }

   private static int routeOf(PlayerInventory var0, ItemStack var1, Item var2) {
      if (var1.method_31574(var2)) {
         return -2;
      } else {
         int var3 = Crystals.hotbarSlotOf(var0, var2);
         return var3 == -1 ? -1 : var3;
      }
   }

   private static float turnTo(ClientPlayerEntity var0, AnchorGuard.Spot var1) {
      Rotation var2 = ServerRotation.sent();
      Rotation var3 = var2 != null ? var2 : RotationUtils.entityRotation(var0);
      Rotation var4 = ServerRotation.next(var0, RotationUtils.fromTo(var0.method_33571(), var1.aim()));
      return Math.abs(var4.yaw() - var3.yaw());
   }

   private void release() {
      if (!this.slots.holding()) {
         CombatMacroLock.release(this);
      }
   }

   @Environment(EnvType.CLIENT)
   private record Blast(float value, boolean kills, LivingEntity source) {
   }
}
