package net.efn.client.module.impl.combat;

import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.PlayerAttackEntityEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.crystal.Combo;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.crystal.HurtClock;
import net.efn.client.module.impl.combat.crystal.LootGuard;
import net.efn.client.module.impl.combat.crystal.ShotView;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.misc.AutoEatModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.Type;

@Environment(EnvType.CLIENT)
public final class CrystalAuraModule extends ClientModule implements HudOverlayProvider {
   private static final int COUNTER_CAP = 1000;
   private static final int IDLE_TICKS = 10;
   private static final float FACE_PLACE_HEALTH = 10.0F;
   private static final float FACE_PLACE_DAMAGE = 2.0F;
   private static final float ARMOR_LOW = 0.1F;
   private static final float BREAK_FACTOR = 0.6F;
   private static final double AIM_INSET = 0.1;
   private static final int MAX_BREAK_ATTEMPTS = 3;
   private static final double CHAIN_RANGE = 12.0;
   private static final int WEAPON_SETTLE_TICKS = 2;
   private static final float LOOK_BONUS = 2.0F;
   private static final int MAX_LEAD_TICKS = 5;
   private static final float LOOK_MATCH_DEGREES = 2.0F;
   private static final double PLACE_BREAK_MARGIN = 0.1;
   private static final float ONE_LOOK_BONUS = 2.0F;
   private static final float OBSIDIAN_UPGRADE_BAR = 8.0F;
   private static final float OBSIDIAN_MIN_GAIN = 5.0F;
   private static final float OBSIDIAN_MIN_RATIO = 2.0F;
   private static final float EAGER_UPGRADE_BAR = 11.0F;
   private static final float EAGER_MIN_GAIN = 3.0F;
   private static final float EAGER_MIN_RATIO = 1.4F;
   private static final double PLACE_REACH_MARGIN = 0.1;
   private static final float HELD_VIEW_BONUS = 20.0F;
   private final BooleanSetting requireHold = this.addSetting("require-hold", new BooleanSetting("Require Hold", false));
   private final KeySetting holdKey = this.addSetting("hold-key", new KeySetting("Hold Key", 1003));
   private final NumberSetting placeDelay = this.addSetting("place-delay", new NumberSetting("Place Delay", 0.0F, 0.0F, 10.0F).step(1.0F));
   private final NumberSetting breakDelay = this.addSetting("break-delay", new NumberSetting("Break Delay", 0.0F, 0.0F, 10.0F).step(1.0F));
   private final NumberSetting minDamage = this.addSetting("min-damage", new NumberSetting("Min Damage", 4.0F, 0.0F, 20.0F).step(0.5F));
   private final NumberSetting maxSelfDamage = this.addSetting("max-self-damage", new NumberSetting("Max Self Damage", 8.0F, 0.0F, 20.0F).step(0.5F));
   private final BooleanSetting facePlace = this.addSetting("face-place", new BooleanSetting("Face Place", true));
   private final KeySetting forceFacePlace = this.addSetting("force-face-place", new KeySetting("Force Face Place", -1));
   private final BooleanSetting autoObsidian = this.addSetting("auto-obsidian", new BooleanSetting("Auto Obsidian", true));
   private final BooleanSetting switchBack = this.addSetting("switch-back", new BooleanSetting("Switch Back", true));
   private final NumberSetting turnSpeed = this.addSetting("turn-speed", new NumberSetting("Turn Speed", 4.5F, 1.0F, 10.0F).step(0.5F));
   private final NumberSetting viewAngle = this.addSetting("view-angle", new NumberSetting("View Angle", 90.0F, 30.0F, 180.0F).step(5.0F));
   private final BooleanSetting humanize = this.addSetting("humanize", new BooleanSetting("Human Pace", true));
   private final BooleanSetting protectLoot = this.addSetting("protect-loot", new BooleanSetting("Protect Loot", true));
   private final BooleanSetting render = this.addSetting("render", new BooleanSetting("Render", true));
   private final ColorSetting renderColor = this.addSetting("render-color", new ColorSetting("Render Color", -2728705));
   private final ShotView view = new ShotView();
   private boolean engaged;
   private int idleTicks;
   private int savedSlot = -1;
   private int ownSlot = -1;
   private int ticksSinceBreak = 1000;
   private int ticksSincePlace = 1000;
   private int breakGap;
   private int placeGap;
   private ItemStack lastMain;
   private int mainAge = 1000;
   private int pendingBreakId = -1;
   private int pendingBreakAge = 1000;
   private BlockPos pendingObsidian;
   private int pendingObsidianAge = 1000;
   private final Int2IntOpenHashMap breakAttempts = new Int2IntOpenHashMap();
   private final Int2IntOpenHashMap doomed = new Int2IntOpenHashMap();
   private float steerMin;
   private float steerSelf;
   private int steerUntil = -1;
   private float tickMin;
   private float tickSelf;
   private int eagerUntil = -1;
   private int floorY;
   private int floorUntil = -1;
   private int tickFloor = -2147483648;
   private int fastUntil = -1;
   private boolean fullSpeed;

   public CrystalAuraModule() {
      super("crystal-aura", "Crystal Aura", "Places and breaks end crystals for the most damage.", ModuleCategory.COMBAT);
      this.holdKey.visibleWhen(this.requireHold::get);
      this.renderColor.visibleWhen(this.render::get);
   }

   @Override
   protected void onEnable() {
      this.resetState();
   }

   @Override
   protected void onDisable() {
      ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
      if (var1 != null && !AutoEatModule.isEating() && !AutoInvTotemModule.wantsHotbar()) {
         this.restoreSlot(var1);
      }

      this.resetState();
      this.view.clear();
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var5 = MinecraftClient.method_1551();
      this.ageCounters();
      ClientPlayerEntity var6 = var5.field_1724;
      if (var6 != null && var5.field_1687 != null) {
         LootGuard.update(var5.field_1687, var6);
      }

      if (var6 != null && var5.field_1687 != null && var5.field_1761 != null && !var6.method_29504()) {
         HurtClock.tick(var6);
         this.forgetGone(var5.field_1687);
         boolean var7 = var6.field_6012 <= this.steerUntil;
         this.tickMin = var7 ? this.steerMin : this.minDamage.get();
         this.tickSelf = var7 ? this.steerSelf : this.maxSelfDamage.get();
         this.tickFloor = var6.field_6012 <= this.floorUntil ? this.floorY : -2147483648;
         this.fullSpeed = var6.field_6012 <= this.fastUntil;
         ItemStack var8 = var6.method_6047();
         if (var8 != this.lastMain) {
            this.lastMain = var8;
            this.mainAge = 0;
         }

         CrystalAuraModule.Look var9 = ServerRotation.canRotate(var6) ? CrystalAuraModule.Look.SILENT : CrystalAuraModule.Look.CROSSHAIR;
         boolean var10 = !this.requireHold.get() || Crystals.isKeyDown(var5, this.holdKey.value());
         AnchorAuraModule var11 = ModuleRegistry.get(AnchorAuraModule.class);
         boolean var4 = var11 != null && var11.isEnabled() && var9 == CrystalAuraModule.Look.SILENT;
         if (MeleeAttacks.canAct(var5) && !var6.method_7325() && (var10 || var4)) {
            if (!AutoInvTotemModule.wantsHotbar() && !AutoCityModule.mining() && !CombatMacroLock.heldByOther(this)) {
               if (this.ownSlot != -1 && !this.ownsSelection(var6)) {
                  this.forgetSlots();
               }

               ItemStack var2;
               boolean var3 = !Crystals.isCombatItem(var2 = var6.method_6079()) && !var2.method_31574(Items.field_8301) && !var2.method_31574(Items.field_8281);
               if (!MeleeAttacks.rightClickComing(var5) || !var3 && !var8.method_31574(Items.field_23141) && !var8.method_31574(Items.field_8801)) {
                  if (!MeleeAttacks.actedThisTick(var6) && MeleeAttacks.handsFree(var5, var6) && (this.ownsSelection(var6) || mayTakeHand(var6, var4))) {
                     List var14 = Combo.enemies(var5, var6);
                     if (var14.isEmpty()) {
                        this.disengage(var5, var6);
                     } else {
                        Rotation var15 = null;
                        if (var9 == CrystalAuraModule.Look.CROSSHAIR) {
                           var15 = RotationUtils.entityRotation(var6);
                           if (ServerRotation.pending() || !sameLook(ServerRotation.sent(), var15)) {
                              this.idle(var5, var6);
                              return;
                           }
                        }

                        int var16 = Crystals.confirmWindow(var5, var6);
                        EndCrystalEntity var17 = this.lastHit(var5.field_1687, var16);
                        CrystalAuraModule.Fight var18 = new CrystalAuraModule.Fight(
                           var6,
                           var5.field_1687,
                           var14,
                           Combo.friends(var5, var6),
                           var16,
                           Crystals.rttTicks(var5, var6),
                           var15,
                           var9 == CrystalAuraModule.Look.SILENT ? 2.0F : 0.0F,
                           Crystals.isKeyDown(var5, this.forceFacePlace.value())
                        );
                        Combo.Shot var19 = this.choose(var5, var18, var17, var10, var4 ? var11 : null);
                        if (var19 == null) {
                           this.idle(var5, var6);
                        } else {
                           this.engage();
                           if (var19.plan() instanceof CrystalAuraModule.Plan var21) {
                              if (this.render.get()) {
                                 this.show(var18, var21);
                              }

                              if (!this.perform(var5, var6, var18, var21, var9) || var9 != CrystalAuraModule.Look.SILENT) {
                                 return;
                              }

                              CrystalAuraModule.Plan var22 = this.planCrystal(var18, this.lastHit(var5.field_1687, var16), 1, this.stagedBreak(var18));
                              Rotation var23 = ServerRotation.sent();
                              this.turn(var22 != null && !var18.holds(var22) ? var18.lookAt(var22.aim()) : var23);
                           } else {
                              var11.show(var19);
                              if (this.fire(var5, var6, var18, var11, var19)) {
                                 ServerRotation.request(ServerRotation.sent());
                              }
                           }
                        }
                     }
                  } else {
                     this.idle(var5, var6);
                  }
               } else {
                  this.disengage(var5, var6);
               }
            } else {
               CombatMacroLock.release(this);
            }
         } else {
            this.disengage(var5, var6);
         }
      } else {
         this.disengage(var5, null);
         Crystals.forgetRtt();
      }
   }

   public void steer(ClientPlayerEntity var1, float var2, float var3) {
      this.steerMin = var2;
      this.steerSelf = var3;
      this.steerUntil = var1.field_6012 + 2;
   }

   public void buildEagerly(ClientPlayerEntity var1) {
      this.eagerUntil = var1.field_6012 + 2;
   }

   public void crystalsFrom(ClientPlayerEntity var1, int var2) {
      this.floorY = var2;
      this.floorUntil = var1.field_6012 + 2;
   }

   public void fullSpeed(ClientPlayerEntity var1) {
      this.fastUntil = var1.field_6012 + 2;
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

   @EventHandler
   public void onAttack(PlayerAttackEntityEvent var1) {
      Entity var3 = var1.target;
      EndCrystalEntity var2;
      if (var3 instanceof EndCrystalEntity && (var2 = (EndCrystalEntity)var3).method_73183().method_8608()) {
         this.markBreak(var2);
      }
   }

   @EventHandler
   public void onPacketReceive(PacketReceiveEvent var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 != null) {
         if (var1.packet instanceof BundleS2CPacket var4) {
            for (Packet var6 : var4.method_48324()) {
               this.note(var2, var6);
            }
         } else {
            this.note(var2, var1.packet);
         }

         HurtClock.note(var2, var1.packet);
      }
   }

   private void note(ClientPlayerEntity var1, Packet<?> var2) {
      EntitySpawnS2CPacket var3;
      if (var2 instanceof EntitySpawnS2CPacket && (var3 = (EntitySpawnS2CPacket)var2).method_11169() == EntityType.field_6110) {
         Crystals.noteSpawn(var1, BlockPos.method_49637(var3.method_11175(), var3.method_11174() - 1.0, var3.method_11176()));
      }
   }

   private static boolean mayTakeHand(ClientPlayerEntity var0, boolean var1) {
      ItemStack var2 = var0.method_6047();
      return var2.method_31574(Items.field_8301)
         || var2.method_31574(Items.field_8281)
         || Crystals.isCombatItem(var2)
         || var1 && (var2.method_31574(Items.field_23141) || var2.method_31574(Items.field_8801));
   }

   private boolean ownsSelection(ClientPlayerEntity var1) {
      return this.ownSlot != -1 && var1.method_31548().method_67532() == this.ownSlot;
   }

   private void engage() {
      this.engaged = true;
      this.idleTicks = 0;
      CombatMacroLock.tryAcquire(this);
   }

   private void idle(MinecraftClient var1, ClientPlayerEntity var2) {
      if (!this.engaged || ++this.idleTicks >= 10) {
         this.disengage(var1, var2);
      }
   }

   private void disengage(MinecraftClient var1, ClientPlayerEntity var2) {
      this.engaged = false;
      this.idleTicks = 0;
      CombatMacroLock.release(this);
      if (var2 == null) {
         this.forgetSlots();
      } else if (var1.field_1755 == null && !AutoEatModule.isEating() && !AutoInvTotemModule.wantsHotbar() && !MeleeAttacks.actedThisTick(var2)) {
         this.restoreSlot(var2);
      }
   }

   private void restoreSlot(ClientPlayerEntity var1) {
      if (this.ownSlot != -1) {
         PlayerInventory var2 = var1.method_31548();
         if (this.switchBack.get() && PlayerInventory.method_7380(this.savedSlot) && this.savedSlot != this.ownSlot && var2.method_67532() == this.ownSlot) {
            var2.method_61496(this.savedSlot);
         }

         this.forgetSlots();
      }
   }

   private void forgetSlots() {
      this.savedSlot = -1;
      this.ownSlot = -1;
   }

   private void resetState() {
      this.engaged = false;
      this.idleTicks = 0;
      this.forgetSlots();
      this.ticksSinceBreak = 1000;
      this.ticksSincePlace = 1000;
      this.breakGap = 0;
      this.placeGap = 0;
      this.lastMain = null;
      this.mainAge = 1000;
      this.pendingBreakId = -1;
      this.pendingBreakAge = 1000;
      this.pendingObsidian = null;
      this.pendingObsidianAge = 1000;
      this.breakAttempts.clear();
      this.doomed.clear();
      Crystals.forgetRtt();
      CombatMacroLock.release(this);
   }

   private void forgetGone(ClientWorld var1) {
      forgetGone(var1, this.breakAttempts.keySet());
      forgetGone(var1, this.doomed.keySet());
   }

   private static void forgetGone(ClientWorld var0, IntSet var1) {
      IntIterator var2 = var1.iterator();

      while (var2.hasNext()) {
         if (var0.method_8469(var2.nextInt()) == null) {
            var2.remove();
         }
      }
   }

   private static boolean oneLook(Vec3d var0, Box var1, Vec3d var2, double var3) {
      Vec3d var5 = var2.method_1020(var0).method_1029();
      return var1.method_992(var0, var0.method_1019(var5.method_1021(var3))).isPresent();
   }

   private boolean canHostSoon(CrystalAuraModule.Fight var1, BlockPos var2, Entity var3) {
      BlockPos var4 = var2.method_10084();
      if (!var1.level.method_22347(var4)) {
         return false;
      } else {
         Box var5 = new Box(
            var4.method_10263(), var4.method_10264(), var4.method_10260(), var4.method_10263() + 1.0, var4.method_10264() + 2.0, var4.method_10260() + 1.0
         );

         for (Entity var7 : var1.level.method_8335(var3, var5)) {
            if (var7.method_5805()) {
               LivingEntity var9;
               if (!(var7 instanceof LivingEntity) || !var1.enemies.contains(var9 = (LivingEntity)var7)) {
                  return false;
               }

               Box var10 = var9.method_5829().method_997(Combo.lead(var9, var1.arriveLead));
               boolean var11 = var10.field_1322 >= var5.field_1325;
               boolean var8 = var10.field_1320 <= var5.field_1323
                  || var10.field_1323 >= var5.field_1320
                  || var10.field_1324 <= var5.field_1321
                  || var10.field_1321 >= var5.field_1324;
               if (!var11 && !var8) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private EndCrystalEntity lastHit(ClientWorld var1, int var2) {
      if (this.pendingBreakId >= 0 && this.pendingBreakAge < var2) {
         Entity var4 = var1.method_8469(this.pendingBreakId);
         EndCrystalEntity var3;
         return var4 instanceof EndCrystalEntity && (var3 = (EndCrystalEntity)var4).method_5805() ? var3 : null;
      } else {
         return null;
      }
   }

   private Combo.Shot choose(MinecraftClient var1, CrystalAuraModule.Fight var2, EndCrystalEntity var3, boolean var4, AnchorAuraModule var5) {
      ArrayList var7 = new ArrayList();
      Combo.Shot var8 = null;
      float var9 = 0.0F;
      if (var4) {
         CrystalAuraModule.Plan var10 = this.stagedBreak(var2);
         CrystalAuraModule.Plan var11 = this.planCrystal(var2, var3, 0, var10);
         if (var11 != null) {
            var8 = this.shot(var2, var11, false);
            if (this.due(var11, 0)) {
               var7.add(var8);
            }
         }

         if (var5 != null && var10 != null) {
            var9 = this.shot(var2, var10, true).value();
         }
      }

      if (var5 != null) {
         var7.addAll(var5.shots(var1, var2.player, var2.enemies, var2.friends, var2.rtt, var9));
      }

      Combo.Shot var6;
      return (var6 = Combo.pick(var7)) != null ? var6 : var8;
   }

   private void show(CrystalAuraModule.Fight var1, CrystalAuraModule.Plan var2) {
      CrystalAuraModule.Side var3 = var1.side(var2.step());
      boolean var4 = var2.step() == CrystalAuraModule.Step.OBSIDIAN;
      Vec3d var5 = var2.step() == CrystalAuraModule.Step.BREAK ? var2.crystal().method_73189() : Crystals.explosionAt(var2.target());
      float var6 = 0.0F;

      for (CrystalDamage.Victim var8 : var3.enemies) {
         var6 = Math.max(var6, var8.damage(var5, var4));
      }

      BlockPos var9 = var2.step() == CrystalAuraModule.Step.BREAK ? var2.crystal().method_24515().method_10074() : var2.target();
      this.view.show(new Box(var9), this.renderColor.argb(), var6, var3.self.damage(var5, var4));
   }

   private CrystalAuraModule.Plan planCrystal(CrystalAuraModule.Fight var1, EndCrystalEntity var2, int var3, CrystalAuraModule.Plan var4) {
      CrystalAuraModule.Plan var6 = this.planBreak(var1, false);
      if (var6 != null && this.due(var6, var3)) {
         return var6;
      } else {
         boolean var5 = var6 != null || var4 != null || Crystals.anyPlacing(var1.player, var1.window);
         CrystalAuraModule.Plan var8 = var5 ? null : this.planPlacement(var1, var2, var2 == null);
         return var8 != null ? var8 : var6;
      }
   }

   private CrystalAuraModule.Plan stagedBreak(CrystalAuraModule.Fight var1) {
      return this.planBreak(var1, true);
   }

   private Combo.Shot shot(CrystalAuraModule.Fight var1, CrystalAuraModule.Plan var2, boolean var3) {
      CrystalAuraModule.Step var5 = var2.step();
      Vec3d var4 = var5 == CrystalAuraModule.Step.BREAK ? var2.crystal().method_73189() : Crystals.explosionAt(var2.target());
      int var7 = var5 == CrystalAuraModule.Step.BREAK ? 0 : (var5 == CrystalAuraModule.Step.CRYSTAL ? var1.rtt : var1.rtt + 1);
      return new Combo.Shot(
         Combo.Kind.valueOf(var5.name()), var2.aim(), var2.route(), this.score(var1, var5, var4, -1.0F / 0.0F, var3), kills(var1, var5, var4, var3), var7, var2
      );
   }

   private CrystalAuraModule.Plan planBreak(CrystalAuraModule.Fight var1, boolean var2) {
      double var3 = var1.player.method_55755();
      CrystalAuraModule.Plan var5 = null;
      float var6 = -1.0F / 0.0F;
      Box var7 = var1.player.method_5829().method_1014(var3 + 2.0);

      for (EndCrystalEntity var9 : var1.level.method_18467(EndCrystalEntity.class, var7)) {
         Vec3d var12;
         if (var9.method_5805()
            && this.breakAttempts.get(var9.method_5628()) < 3
            && (var9.method_5628() != this.pendingBreakId || this.pendingBreakAge >= var1.window)
            && (!this.doomed.containsKey(var9.method_5628()) || var1.player.field_6012 - this.doomed.get(var9.method_5628()) >= var1.window)
            && !((var12 = var1.eye).method_1025(TargetingUtils.closestPoint(var12, var9.method_5829())) > var3 * var3)
            && !(var9.method_23318() < this.tickFloor - 0.01)) {
            Vec3d var13 = var9.method_73189();
            float var10;
            CrystalAuraModule.Plan var11;
            if ((!this.protectLoot.get() || !LootGuard.near(var13))
               && !(this.bound(var1, CrystalAuraModule.Step.BREAK, var13) + var1.lookBonus <= var6)
               && (var11 = this.breakLook(var1, var9)) != null
               && var1.inView(var11)
               && (var10 = this.score(var1, CrystalAuraModule.Step.BREAK, var13, var6 - var1.lookBonus, var2)) != -1.0F / 0.0F
               && !((var10 = var10 + var1.lookCredit(var11)) <= var6)) {
               var5 = var11;
               var6 = var10;
            }
         }
      }

      if (var5 == null) {
         return null;
      } else {
         int var14 = Crystals.weaponRoute(var1.player);
         return var14 == -1 ? null : var5.withRoute(var14);
      }
   }

   private CrystalAuraModule.Plan breakLook(CrystalAuraModule.Fight var1, EndCrystalEntity var2) {
      if (var1.fixedLook != null) {
         CrystalAuraModule.Plan var9 = new CrystalAuraModule.Plan(CrystalAuraModule.Step.BREAK, var2, null, null, var2.method_5829().method_1005(), -2);
         return hitAlong(var1.player, var9, var1.eye, var1.fixedLook) != null ? var9 : null;
      } else {
         BlockPos var4 = var2.method_24515().method_10074();
         CrystalAuraModule.Plan var3;
         if (Crystals.isBase(var1.level.method_8320(var4))
            && hitAlong(
                  var1.player,
                  var3 = new CrystalAuraModule.Plan(CrystalAuraModule.Step.BREAK, var2, var4, var4, aimOnTop(var1.level, var4), -2),
                  var1.aimEye,
                  var1.lookAt(var3.aim())
               )
               != null) {
            return var3;
         } else {
            Vec3d var5 = var1.towardLook(var2.method_5829().method_1011(0.1));
            CrystalAuraModule.Plan var6 = new CrystalAuraModule.Plan(CrystalAuraModule.Step.BREAK, var2, null, null, var5, -2);
            if (hitAlong(var1.player, var6, var1.aimEye, var1.lookAt(var5)) != null) {
               return var6;
            } else {
               Vec3d var7 = TargetingUtils.closestPoint(var1.aimEye, var2.method_5829().method_1011(0.1));
               CrystalAuraModule.Plan var8 = new CrystalAuraModule.Plan(CrystalAuraModule.Step.BREAK, var2, null, null, var7, -2);
               return hitAlong(var1.player, var8, var1.aimEye, var1.lookAt(var7)) != null ? var8 : null;
            }
         }
      }
   }

   private CrystalAuraModule.Plan planPlacement(CrystalAuraModule.Fight var1, EndCrystalEntity var2, boolean var3) {
      CrystalAuraModule.Plan var4 = this.planBase(var1, CrystalAuraModule.Step.CRYSTAL, var2);
      if (this.autoObsidian.get() && var3) {
         boolean var5 = var1.player.field_6012 <= this.eagerUntil;
         float var6 = var4 == null ? 0.0F : this.enemyDamage(var1, var4);
         if (var4 != null) {
            float var7 = var5 ? 11.0F : 8.0F;
            if (var6 >= var7) {
               return var4;
            }
         }

         CrystalAuraModule.Plan var12;
         if ((var12 = this.planBase(var1, CrystalAuraModule.Step.OBSIDIAN, null)) != null && var4 != null) {
            float var8 = this.enemyDamage(var1, var12);
            float var9 = var5 ? 3.0F : 5.0F;
            boolean var10;
            if (var8 >= var6 + var9) {
               float var11 = var5 ? 1.4F : 2.0F;
               var10 = var8 >= var6 * var11;
            } else {
               var10 = false;
            }

            return var10 ? var12 : var4;
         } else {
            return var12 == null ? var4 : var12;
         }
      } else {
         return var4;
      }
   }

   private float enemyDamage(CrystalAuraModule.Fight var1, CrystalAuraModule.Plan var2) {
      Vec3d var3 = Crystals.explosionAt(var2.target());
      float var4 = 0.0F;

      for (CrystalDamage.Victim var6 : var1.side(var2.step()).enemies) {
         var4 = Math.max(var4, var6.damage(var3, var2.step() == CrystalAuraModule.Step.OBSIDIAN));
      }

      return var4;
   }

   private CrystalAuraModule.Plan planBase(CrystalAuraModule.Fight var1, CrystalAuraModule.Step var2, EndCrystalEntity var3) {
      ClientPlayerEntity var6 = var1.player;
      ClientWorld var7 = var1.level;
      Item var8 = var2 == CrystalAuraModule.Step.CRYSTAL ? Items.field_8301 : Items.field_8281;
      BlockState var5;
      if (Crystals.route(var6, var8, var5 = Blocks.field_10540.method_9564(), this.savedSlot) == -1) {
         return null;
      } else if (var2 == CrystalAuraModule.Step.OBSIDIAN && Crystals.route(var6, Items.field_8301, var5, this.savedSlot) == -1) {
         return null;
      } else {
         double var9 = var1.placeReach;
         double var11 = var6.method_55755() - 0.1;
         Vec3d var13 = var1.eye;
         BlockPos var14 = BlockPos.method_49638(var13);
         int var15 = MathHelper.method_15384(var9);
         float var16 = var2 == CrystalAuraModule.Step.CRYSTAL && var1.lookBonus > 0.0F ? 2.0F : 0.0F;
         ArrayList var17 = new ArrayList();
         Mutable var18 = new Mutable();
         Mutable var19 = new Mutable();

         for (int var20 = -var15; var20 <= var15; var20++) {
            for (int var21 = -var15; var21 <= var15; var21++) {
               for (int var22 = -var15; var22 <= var15; var22++) {
                  var18.method_25504(var14, var20, var21, var22);
                  if (var18.method_10264() + 1 >= this.tickFloor) {
                     BlockState var25 = var7.method_8320(var18);
                     if (var2 == CrystalAuraModule.Step.CRYSTAL ? Crystals.isBase(var25) : var25.method_45474()) {
                        var19.method_25505(var18, Direction.field_11033);
                        if ((var2 != CrystalAuraModule.Step.OBSIDIAN || var7.method_8320(var19).method_26206(var7, var19, Direction.field_11036))
                           && (
                              var2 == CrystalAuraModule.Step.CRYSTAL
                                 ? !Crystals.placing(var6, var18, var1.window)
                                 : !var18.equals(this.pendingObsidian) || this.pendingObsidianAge >= var1.window
                           )) {
                           BlockPos var26 = var18.method_10062();
                           BlockPos var27 = var2 == CrystalAuraModule.Step.CRYSTAL ? var26 : var19.method_10062();
                           Vec3d var28 = aimOnTop(var7, var27);
                           float var23;
                           if (!(var13.method_1025(var28) > var9 * var9)
                              && !(var13.method_1025(TargetingUtils.closestPoint(var13, Crystals.boxOn(var26))) > var11 * var11)
                              && (!this.protectLoot.get() || !LootGuard.near(Crystals.explosionAt(var26)))
                              && (var23 = this.bound(var1, var2, Crystals.explosionAt(var26))) != -1.0F / 0.0F) {
                              var17.add(new CrystalAuraModule.Candidate(new CrystalAuraModule.Plan(var2, null, var27, var26, var28, -2), var23));
                           }
                        }
                     }
                  }
               }
            }
         }

         var17.sort(Comparator.comparingDouble(CrystalAuraModule.Candidate::bound).reversed());
         CrystalAuraModule.Plan var29 = null;
         float var30 = -1.0F / 0.0F;
         Iterator var31 = var17.iterator();

         CrystalAuraModule.Candidate var4;
         while (var31.hasNext() && !((var4 = (CrystalAuraModule.Candidate)var31.next()).bound() + var1.lookBonus + var16 <= var30)) {
            CrystalAuraModule.Plan var24 = var4.plan();
            float var34 = this.score(var1, var2, Crystals.explosionAt(var24.target()), var30 - var1.lookBonus - var16, false);
            CrystalAuraModule.Plan var32;
            if (var34 != -1.0F / 0.0F
               && var7.method_8621().method_11952(var24.clicked())
               && this.canHostSoon(var1, var24.target(), var2 == CrystalAuraModule.Step.CRYSTAL ? var3 : null)
               && (var32 = this.reachable(var1, var24)) != null
               && var1.inView(var32)) {
               var34 += var1.lookCredit(var32);
               if (var16 > 0.0F && oneLook(var1.aimEye, Crystals.boxOn(var32.target()), var32.aim(), var11)) {
                  var34 += var16;
               }

               if (!(var34 <= var30)) {
                  var29 = var32;
                  var30 = var34;
               }
            }
         }

         if (var29 == null) {
            return null;
         } else {
            int var33 = Crystals.route(var6, var8, var7.method_8320(var29.clicked()), this.savedSlot);
            return var33 == -1 ? null : var29.withRoute(var33);
         }
      }
   }

   private CrystalAuraModule.Plan reachable(CrystalAuraModule.Fight var1, CrystalAuraModule.Plan var2) {
      if (var1.fixedLook == null) {
         BlockPos var3 = var2.clicked();
         double var4 = var2.aim().field_1351;
         Vec3d var6 = var1.towardLook(
            new Box(var3.method_10263() + 0.1, var4, var3.method_10260() + 0.1, var3.method_10263() + 1.0 - 0.1, var4, var3.method_10260() + 1.0 - 0.1)
         );
         CrystalAuraModule.Plan var7 = new CrystalAuraModule.Plan(var2.step(), null, var3, var2.target(), var6, var2.route());
         if (var1.eye.method_1025(var6) <= var1.placeReach * var1.placeReach && hitAlong(var1.player, var7, var1.aimEye, var1.lookAt(var6)) != null) {
            return var7;
         }
      }

      if (hitAlong(var1.player, var2, var1.aimEye, var1.lookAt(var2.aim())) != null) {
         return var2;
      } else if (var2.step() == CrystalAuraModule.Step.CRYSTAL && var1.fixedLook == null) {
         Vec3d var8 = Vec3d.method_24953(var2.clicked());

         for (Direction var5 : Type.field_11062) {
            Vec3d var11 = var8.method_1031(var5.method_10148() * 0.4, 0.0, var5.method_10165() * 0.4);
            CrystalAuraModule.Plan var10;
            if (!((var1.aimEye.field_1352 - var11.field_1352) * var5.method_10148() + (var1.aimEye.field_1350 - var11.field_1350) * var5.method_10165() <= 0.0)
               && hitAlong(
                     var1.player,
                     var10 = new CrystalAuraModule.Plan(var2.step(), null, var2.clicked(), var2.target(), var11, var2.route()),
                     var1.aimEye,
                     var1.lookAt(var11)
                  )
                  != null) {
               return var10;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static Vec3d aimOnTop(ClientWorld var0, BlockPos var1) {
      double var2 = var0.method_8320(var1).method_26218(var0, var1).method_1105(Axis.field_11052);
      return new Vec3d(var1.method_10263() + 0.5, var1.method_10264() + var2 - 0.1, var1.method_10260() + 0.5);
   }

   private float need(LivingEntity var1, boolean var2, boolean var3) {
      float var4 = !var3 && (!this.facePlace.get() || !facePlaced(var1)) ? this.tickMin : Math.min(2.0F, this.tickMin);
      return var2 ? var4 * 0.6F : var4;
   }

   private static boolean facePlaced(LivingEntity var0) {
      return CrystalDamage.health(var0) <= 10.0F || Combo.inHole(var0) || armorLow(var0);
   }

   private static boolean armorLow(LivingEntity var0) {
      for (EquipmentSlot var2 : EquipmentSlot.field_54086) {
         ItemStack var3;
         if (var2.method_46643() && (var3 = var0.method_6118(var2)).method_7963() && var3.method_7936() - var3.method_7919() <= var3.method_7936() * 0.1F) {
            return true;
         }
      }

      return false;
   }

   private float bound(CrystalAuraModule.Fight var1, CrystalAuraModule.Step var2, Vec3d var3) {
      float var4 = -1.0F / 0.0F;

      for (CrystalDamage.Victim var6 : var1.side(var2).enemies) {
         LivingEntity var7 = var6.entity();
         float var8 = var6.upperBound(var3);
         if (Combo.kills(var8, var7) || !(var8 < this.need(var7, var2 == CrystalAuraModule.Step.BREAK, var1.forced))) {
            var4 = Math.max(var4, Combo.value(var8, var7));
         }
      }

      return var4;
   }

   private float score(CrystalAuraModule.Fight var1, CrystalAuraModule.Step var2, Vec3d var3, float var4, boolean var5) {
      CrystalAuraModule.Side var6 = var1.side(var2);
      boolean var7 = var2 == CrystalAuraModule.Step.OBSIDIAN;
      float var8 = var6.self.damage(var3, var7);
      if (!(var8 > this.tickSelf) && !CrystalDamage.endangers(var1.player, var8)) {
         for (CrystalDamage.Victim var10 : var6.friends) {
            float var11 = var10.damage(var3, var7);
            if (var11 > this.tickSelf || CrystalDamage.endangers((PlayerEntity)var10.entity(), var11)) {
               return -1.0F / 0.0F;
            }
         }

         float var14 = -1.0F / 0.0F;

         for (CrystalDamage.Victim var16 : var6.enemies) {
            LivingEntity var12 = var16.entity();
            float var13 = effective(var1, var16, var2, var3, var5);
            if (Combo.kills(var13, var12) || !(var13 < this.need(var12, var2 == CrystalAuraModule.Step.BREAK, var1.forced))) {
               var14 = Math.max(var14, Combo.value(var13, var12) - 0.5F * var8);
            }
         }

         return var14 > var4 ? var14 : -1.0F / 0.0F;
      } else {
         return -1.0F / 0.0F;
      }
   }

   private static boolean kills(CrystalAuraModule.Fight var0, CrystalAuraModule.Step var1, Vec3d var2, boolean var3) {
      for (CrystalDamage.Victim var5 : var0.side(var1).enemies) {
         if (Combo.kills(effective(var0, var5, var1, var2, var3), var5.entity())) {
            return true;
         }
      }

      return false;
   }

   private static float effective(CrystalAuraModule.Fight var0, CrystalDamage.Victim var1, CrystalAuraModule.Step var2, Vec3d var3, boolean var4) {
      int var5 = var2 == CrystalAuraModule.Step.BREAK ? (var4 ? HurtClock.opensIn(var1.entity(), var0.rtt) : 0) : var0.rtt;
      return HurtClock.effective(var1, var1.raw(var3, var2 == CrystalAuraModule.Step.OBSIDIAN), var5, var0.rtt);
   }

   private static HitResult hitAlong(ClientPlayerEntity var0, CrystalAuraModule.Plan var1, Vec3d var2, Rotation var3) {
      if (var3 == null) {
         return null;
      } else {
         Vec3d var4 = Vec3d.method_1030(var3.pitch(), var3.yaw());
         if (var1.step() == CrystalAuraModule.Step.BREAK) {
            Box var8 = var1.crystal().method_5829();
            if (var8.method_1006(var2)) {
               return new EntityHitResult(var1.crystal(), var2);
            } else {
               Vec3d var9 = (Vec3d)var8.method_992(var2, var2.method_1019(var4.method_1021(var0.method_55755()))).orElse(null);
               return var9 != null && MeleeAttacks.lineIsClear(var0, var2, var9) ? new EntityHitResult(var1.crystal(), var9) : null;
            }
         } else {
            ClientWorld var5 = (ClientWorld)var0.method_73183();
            BlockState var6 = var5.method_8320(var1.clicked());
            BlockHitResult var7 = var6.method_26218(var5, var1.clicked())
               .method_1092(var2, var2.method_1019(var4.method_1021(var0.method_55754())), var1.clicked());
            if (var7 != null && MeleeAttacks.lineIsClear(var0, var2, var7.method_17784())) {
               return var1.step() == CrystalAuraModule.Step.OBSIDIAN && !var1.target().equals(Crystals.placedObsidian(var5, var0, var7, var6)) ? null : var7;
            } else {
               return null;
            }
         }
      }
   }

   private boolean perform(
      MinecraftClient var1, ClientPlayerEntity var2, CrystalAuraModule.Fight var3, CrystalAuraModule.Plan var4, CrystalAuraModule.Look var5
   ) {
      Vec3d var8 = var2.method_33571();
      Rotation var9 = ServerRotation.sent();
      Rotation var10 = var5 == CrystalAuraModule.Look.SILENT ? var9 : RotationUtils.entityRotation(var2);
      HitResult var7 = var9 == null ? null : hitAlong(var2, var4, var8, var9);
      if (var7 != null && var10 != var9 && hitAlong(var2, var4, var8, var10) == null) {
         var7 = null;
      }

      if (var4.route() != -2) {
         this.select(var2, var4.route());
      }

      boolean var6 = var7 != null
         && this.due(var4, 0)
         && (var4.step() == CrystalAuraModule.Step.BREAK ? this.strike(var1, var2, var3, var4.crystal()) : this.place(var1, var2, var4, (BlockHitResult)var7));
      if (var6) {
         MeleeAttacks.claimTick(var1);
      } else if (var5 == CrystalAuraModule.Look.SILENT) {
         this.turn(var7 != null ? var9 : var3.lookAt(var4.aim()));
      }

      return var6;
   }

   private boolean fire(MinecraftClient var1, ClientPlayerEntity var2, CrystalAuraModule.Fight var3, AnchorAuraModule var4, Combo.Shot var5) {
      if (var5.route() != -2) {
         this.select(var2, var5.route());
      }

      if (!var4.ready(var1, var2, var5)) {
         this.turn(var3.lookAt(var5.aim()));
         return false;
      } else {
         var4.fire(var1, var2, var5);
         return true;
      }
   }

   private boolean strike(MinecraftClient var1, ClientPlayerEntity var2, CrystalAuraModule.Fight var3, EndCrystalEntity var4) {
      if (this.mainAge < 2 && !Crystals.enchantsAddDamage(var2.method_6047()) && !Crystals.dealsDamage(var2, ItemStack.field_8037)) {
         return false;
      } else if (!Crystals.strike(var1, var2, var4)) {
         return false;
      } else {
         this.markBreak(var4);
         this.breakAttempts.addTo(var4.method_5628(), 1);
         this.ticksSinceBreak = 0;
         this.breakGap = this.humanize.get() && !this.fullSpeed ? Crystals.humanGap() : 0;

         for (CrystalDamage.Victim var6 : var3.side(CrystalAuraModule.Step.BREAK).enemies) {
            float var7 = var6.raw(var4.method_73189(), false);
            if (var7 > 0.0F) {
               HurtClock.shot(var2, var6.entity(), var7, var4.method_5628(), var3.rtt);
            }
         }

         Box var8 = var4.method_5829().method_1014(12.0);

         for (EndCrystalEntity var10 : var3.level.method_18467(EndCrystalEntity.class, var8)) {
            if (var10 != var4 && var10.method_5858(var4) <= 144.0) {
               this.doomed.put(var10.method_5628(), var2.field_6012);
            }
         }

         return true;
      }
   }

   private boolean place(MinecraftClient var1, ClientPlayerEntity var2, CrystalAuraModule.Plan var3, BlockHitResult var4) {
      if (!Crystals.rightClick(var1, var2, var4)) {
         return false;
      } else {
         if (var3.step() == CrystalAuraModule.Step.CRYSTAL) {
            Crystals.notePlace(var2, var3.target());
         } else {
            this.pendingObsidian = var3.target();
            this.pendingObsidianAge = 0;
         }

         this.ticksSincePlace = 0;
         this.placeGap = this.humanize.get() && !this.fullSpeed ? Crystals.humanGap() : 0;
         return true;
      }
   }

   private void turn(Rotation var1) {
      ServerRotation.request(var1, this.fullSpeed ? this.turnSpeed.max() : this.turnSpeed.get());
   }

   private static boolean sameLook(Rotation var0, Rotation var1) {
      return var0 != null && Math.abs(RotationUtils.yawDelta(var0.yaw(), var1.yaw())) <= 2.0F && Math.abs(var0.pitch() - var1.pitch()) <= 2.0F;
   }

   private boolean due(CrystalAuraModule.Plan var1, int var2) {
      return var1.step() == CrystalAuraModule.Step.BREAK
         ? this.ticksSinceBreak + var2 > Math.max(this.breakDelay.getInt(), this.breakGap - 1)
         : this.ticksSincePlace + var2 > Math.max(this.placeDelay.getInt(), this.placeGap - 1);
   }

   private void select(ClientPlayerEntity var1, int var2) {
      PlayerInventory var3 = var1.method_31548();
      if (this.savedSlot == -1) {
         this.savedSlot = var3.method_67532();
      }

      var3.method_61496(var2);
      this.ownSlot = var2;
      this.lastMain = var1.method_6047();
      this.mainAge = 0;
   }

   private void markBreak(EndCrystalEntity var1) {
      this.pendingBreakId = var1.method_5628();
      this.pendingBreakAge = 0;
   }

   private void ageCounters() {
      this.ticksSinceBreak = Math.min(this.ticksSinceBreak + 1, 1000);
      this.ticksSincePlace = Math.min(this.ticksSincePlace + 1, 1000);
      this.mainAge = Math.min(this.mainAge + 1, 1000);
      this.pendingBreakAge = Math.min(this.pendingBreakAge + 1, 1000);
      this.pendingObsidianAge = Math.min(this.pendingObsidianAge + 1, 1000);
   }

   @Environment(EnvType.CLIENT)
   private record Candidate(CrystalAuraModule.Plan plan, float bound) {
   }

   @Environment(EnvType.CLIENT)
   private final class Fight {
      final ClientPlayerEntity player;
      final ClientWorld level;
      final Vec3d eye;
      final Vec3d aimEye;
      final List<LivingEntity> enemies;
      final List<LivingEntity> friends;
      final int window;
      final int rtt;
      final Rotation fixedLook;
      final Rotation serverLook;
      final float lookBonus;
      final int arriveLead;
      private final int placeLead;
      private final int selfPlaceLead;
      final boolean forced;
      final double placeReach;
      private CrystalAuraModule.Side breakSide;
      private CrystalAuraModule.Side placeSide;

      Fight(
         ClientPlayerEntity nullx,
         ClientWorld nullxx,
         List<LivingEntity> nullxxx,
         List<LivingEntity> nullxxxx,
         int nullxxxxx,
         int nullxxxxxx,
         Rotation nullxxxxxxx,
         float nullxxxxxxxx,
         boolean nullxxxxxxxxx
      ) {
         this.player = nullx;
         this.level = nullxx;
         this.eye = nullx.method_33571();
         this.aimEye = nullxxxxxxx != null ? this.eye : this.eye.method_1019(nullx.method_18798());
         this.enemies = nullxxx;
         this.friends = nullxxxx;
         this.window = nullxxxxx;
         this.rtt = nullxxxxxx;
         this.fixedLook = nullxxxxxxx;
         this.serverLook = ServerRotation.sent();
         this.lookBonus = nullxxxxxxxx;
         this.arriveLead = Math.min(nullxxxxxx, 5);
         this.placeLead = Math.min(2 * nullxxxxxx, 5);
         this.selfPlaceLead = Math.min(nullxxxxxx, 5);
         this.forced = nullxxxxxxxxx;
         this.placeReach = nullx.method_55754() - 0.1;
      }

      CrystalAuraModule.Side side(CrystalAuraModule.Step var1) {
         if (var1 == CrystalAuraModule.Step.BREAK) {
            if (this.breakSide == null) {
               this.breakSide = new CrystalAuraModule.Side(this.player, this.enemies, this.friends, this.arriveLead, 0);
            }

            return this.breakSide;
         } else {
            if (this.placeSide == null) {
               this.placeSide = new CrystalAuraModule.Side(this.player, this.enemies, this.friends, this.placeLead, this.selfPlaceLead);
            }

            return this.placeSide;
         }
      }

      Rotation lookAt(Vec3d var1) {
         return this.fixedLook != null ? this.fixedLook : RotationUtils.fromTo(this.aimEye, var1);
      }

      boolean holds(CrystalAuraModule.Plan var1) {
         return CrystalAuraModule.hitAlong(this.player, var1, this.eye, this.serverLook) != null;
      }

      float lookCredit(CrystalAuraModule.Plan var1) {
         return this.lookBonus > 0.0F && this.holds(var1) ? this.lookBonus : 0.0F;
      }

      boolean inView(CrystalAuraModule.Plan var1) {
         if (this.fixedLook == null && !(CrystalAuraModule.this.viewAngle.get() >= 180.0F)) {
            float var2 = Math.abs(RotationUtils.yawDelta(this.player.method_36454(), RotationUtils.fromTo(this.aimEye, var1.aim()).yaw()));
            return var2 <= CrystalAuraModule.this.viewAngle.get() || var2 <= CrystalAuraModule.this.viewAngle.get() + 20.0F && this.holds(var1);
         } else {
            return true;
         }
      }

      Vec3d towardLook(Box var1) {
         Rotation var4 = this.serverLook != null ? this.serverLook : RotationUtils.entityRotation((Entity)this.player);
         Vec3d var5 = Vec3d.method_1030(var4.pitch(), var4.yaw());
         Vec3d var6 = var1.method_1005();
         boolean var7 = var1.method_17940() < 1.0E-6 && var5.field_1351 < -1.0E-6 && this.aimEye.field_1351 > var1.field_1322;
         double var2 = var7 ? (var1.field_1322 - this.aimEye.field_1351) / var5.field_1351 : var6.method_1020(this.aimEye).method_1026(var5);
         if (var2 <= 0.0) {
            return var6;
         } else {
            Vec3d var10 = this.aimEye.method_1019(var5.method_1021(var2));
            return new Vec3d(
               MathHelper.method_15350(var10.field_1352, var1.field_1323, var1.field_1320),
               MathHelper.method_15350(var10.field_1351, var1.field_1322, var1.field_1325),
               MathHelper.method_15350(var10.field_1350, var1.field_1321, var1.field_1324)
            );
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Look {
      SILENT,
      CROSSHAIR;

      private Look() {
      }
   }

   @Environment(EnvType.CLIENT)
   private record Plan(CrystalAuraModule.Step step, EndCrystalEntity crystal, BlockPos clicked, BlockPos target, Vec3d aim, int route) {
      CrystalAuraModule.Plan withRoute(int var1) {
         return new CrystalAuraModule.Plan(this.step, this.crystal, this.clicked, this.target, this.aim, var1);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Side {
      final List<CrystalDamage.Victim> enemies = new ArrayList<>();
      final List<CrystalDamage.Victim> friends = new ArrayList<>();
      final CrystalDamage.Victim self;

      Side(ClientPlayerEntity var1, List<LivingEntity> var2, List<LivingEntity> var3, int var4, int var5) {
         for (LivingEntity var7 : var2) {
            this.enemies.add(new CrystalDamage.Victim(var7, Combo.lead(var7, var4)));
         }

         for (LivingEntity var9 : var3) {
            this.friends.add(new CrystalDamage.Victim(var9, Combo.lead(var9, var4)));
         }

         this.self = new CrystalDamage.Victim(var1, Combo.lead(var1, var5));
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Step {
      BREAK,
      CRYSTAL,
      OBSIDIAN;

      private Step() {
      }
   }
}
