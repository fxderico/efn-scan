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
      ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
      if (var1 != null && !AutoEatModule.isEating() && !AutoInvTotemModule.wantsHotbar()) {
         this.restoreSlot(var1);
      }

      this.resetState();
      this.view.clear();
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var5 = MinecraftClient.getInstance();
      this.ageCounters();
      ClientPlayerEntity var6 = var5.player;
      if (var6 != null && var5.world != null) {
         LootGuard.update(var5.world, var6);
      }

      if (var6 != null && var5.world != null && var5.interactionManager != null && !var6.isDead()) {
         HurtClock.tick(var6);
         this.forgetGone(var5.world);
         boolean var7 = var6.age <= this.steerUntil;
         this.tickMin = var7 ? this.steerMin : this.minDamage.get();
         this.tickSelf = var7 ? this.steerSelf : this.maxSelfDamage.get();
         this.tickFloor = var6.age <= this.floorUntil ? this.floorY : -2147483648;
         this.fullSpeed = var6.age <= this.fastUntil;
         ItemStack var8 = var6.getMainHandStack();
         if (var8 != this.lastMain) {
            this.lastMain = var8;
            this.mainAge = 0;
         }

         CrystalAuraModule.Look var9 = ServerRotation.canRotate(var6) ? CrystalAuraModule.Look.SILENT : CrystalAuraModule.Look.CROSSHAIR;
         boolean var10 = !this.requireHold.get() || Crystals.isKeyDown(var5, this.holdKey.value());
         AnchorAuraModule var11 = ModuleRegistry.get(AnchorAuraModule.class);
         boolean var4 = var11 != null && var11.isEnabled() && var9 == CrystalAuraModule.Look.SILENT;
         if (MeleeAttacks.canAct(var5) && !var6.isSpectator() && (var10 || var4)) {
            if (!AutoInvTotemModule.wantsHotbar() && !AutoCityModule.mining() && !CombatMacroLock.heldByOther(this)) {
               if (this.ownSlot != -1 && !this.ownsSelection(var6)) {
                  this.forgetSlots();
               }

               ItemStack var2;
               boolean var3 = !Crystals.isCombatItem(var2 = var6.getOffHandStack()) && !var2.isOf(Items.END_CRYSTAL) && !var2.isOf(Items.OBSIDIAN);
               if (!MeleeAttacks.rightClickComing(var5) || !var3 && !var8.isOf(Items.RESPAWN_ANCHOR) && !var8.isOf(Items.GLOWSTONE)) {
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
                        EndCrystalEntity var17 = this.lastHit(var5.world, var16);
                        CrystalAuraModule.Fight var18 = new CrystalAuraModule.Fight(
                           var6,
                           var5.world,
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

                              CrystalAuraModule.Plan var22 = this.planCrystal(var18, this.lastHit(var5.world, var16), 1, this.stagedBreak(var18));
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

   public void steer(ClientPlayerEntity clientPlayerEntity, float f, float g) {
      this.steerMin = f;
      this.steerSelf = g;
      this.steerUntil = clientPlayerEntity.age + 2;
   }

   public void buildEagerly(ClientPlayerEntity clientPlayerEntity) {
      this.eagerUntil = clientPlayerEntity.age + 2;
   }

   public void crystalsFrom(ClientPlayerEntity clientPlayerEntity, int i) {
      this.floorY = i;
      this.floorUntil = clientPlayerEntity.age + 2;
   }

   public void fullSpeed(ClientPlayerEntity clientPlayerEntity) {
      this.fastUntil = clientPlayerEntity.age + 2;
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      if (this.render.get()) {
         this.view.draw();
      }
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.render.get() && this.view.showing();
   }

   @Override
   public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
      this.view.label(imDrawList, f, g);
   }

   @EventHandler
   public void onAttack(PlayerAttackEntityEvent playerAttackEntityEvent) {
      Entity var3 = playerAttackEntityEvent.target;
      EndCrystalEntity var2;
      if (var3 instanceof EndCrystalEntity && (var2 = (EndCrystalEntity)var3).getEntityWorld().isClient()) {
         this.markBreak(var2);
      }
   }

   @EventHandler
   public void onPacketReceive(PacketReceiveEvent packetReceiveEvent) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var2 != null) {
         if (packetReceiveEvent.packet instanceof BundleS2CPacket var4) {
            for (Packet var6 : var4.getPackets()) {
               this.note(var2, var6);
            }
         } else {
            this.note(var2, packetReceiveEvent.packet);
         }

         HurtClock.note(var2, packetReceiveEvent.packet);
      }
   }

   private void note(ClientPlayerEntity clientPlayerEntity, Packet<?> packet) {
      EntitySpawnS2CPacket var3;
      if (packet instanceof EntitySpawnS2CPacket && (var3 = (EntitySpawnS2CPacket)packet).getEntityType() == EntityType.END_CRYSTAL) {
         Crystals.noteSpawn(clientPlayerEntity, BlockPos.ofFloored(var3.getX(), var3.getY() - 1.0, var3.getZ()));
      }
   }

   private static boolean mayTakeHand(ClientPlayerEntity clientPlayerEntity, boolean bl) {
      ItemStack var2 = clientPlayerEntity.getMainHandStack();
      return var2.isOf(Items.END_CRYSTAL)
         || var2.isOf(Items.OBSIDIAN)
         || Crystals.isCombatItem(var2)
         || bl && (var2.isOf(Items.RESPAWN_ANCHOR) || var2.isOf(Items.GLOWSTONE));
   }

   private boolean ownsSelection(ClientPlayerEntity clientPlayerEntity) {
      return this.ownSlot != -1 && clientPlayerEntity.getInventory().getSelectedSlot() == this.ownSlot;
   }

   private void engage() {
      this.engaged = true;
      this.idleTicks = 0;
      CombatMacroLock.tryAcquire(this);
   }

   private void idle(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (!this.engaged || ++this.idleTicks >= 10) {
         this.disengage(minecraftClient, clientPlayerEntity);
      }
   }

   private void disengage(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      this.engaged = false;
      this.idleTicks = 0;
      CombatMacroLock.release(this);
      if (clientPlayerEntity == null) {
         this.forgetSlots();
      } else if (minecraftClient.currentScreen == null
         && !AutoEatModule.isEating()
         && !AutoInvTotemModule.wantsHotbar()
         && !MeleeAttacks.actedThisTick(clientPlayerEntity)) {
         this.restoreSlot(clientPlayerEntity);
      }
   }

   private void restoreSlot(ClientPlayerEntity clientPlayerEntity) {
      if (this.ownSlot != -1) {
         PlayerInventory var2 = clientPlayerEntity.getInventory();
         if (this.switchBack.get()
            && PlayerInventory.isValidHotbarIndex(this.savedSlot)
            && this.savedSlot != this.ownSlot
            && var2.getSelectedSlot() == this.ownSlot) {
            var2.setSelectedSlot(this.savedSlot);
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

   private void forgetGone(ClientWorld clientWorld) {
      forgetGone(clientWorld, this.breakAttempts.keySet());
      forgetGone(clientWorld, this.doomed.keySet());
   }

   private static void forgetGone(ClientWorld clientWorld, IntSet intSet) {
      IntIterator var2 = intSet.iterator();

      while (var2.hasNext()) {
         if (clientWorld.getEntityById(var2.nextInt()) == null) {
            var2.remove();
         }
      }
   }

   private static boolean oneLook(Vec3d vec3d, Box box, Vec3d vec3d2, double d) {
      Vec3d var5 = vec3d2.subtract(vec3d).normalize();
      return box.raycast(vec3d, vec3d.add(var5.multiply(d))).isPresent();
   }

   private boolean canHostSoon(CrystalAuraModule.Fight fight, BlockPos blockPos, Entity entity) {
      BlockPos var4 = blockPos.up();
      if (!fight.level.isAir(var4)) {
         return false;
      } else {
         Box var5 = new Box(var4.getX(), var4.getY(), var4.getZ(), var4.getX() + 1.0, var4.getY() + 2.0, var4.getZ() + 1.0);

         for (Entity var7 : fight.level.getOtherEntities(entity, var5)) {
            if (var7.isAlive()) {
               LivingEntity var9;
               if (!(var7 instanceof LivingEntity) || !fight.enemies.contains(var9 = (LivingEntity)var7)) {
                  return false;
               }

               Box var10 = var9.getBoundingBox().offset(Combo.lead(var9, fight.arriveLead));
               boolean var11 = var10.minY >= var5.maxY;
               boolean var8 = var10.maxX <= var5.minX || var10.minX >= var5.maxX || var10.maxZ <= var5.minZ || var10.minZ >= var5.maxZ;
               if (!var11 && !var8) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private EndCrystalEntity lastHit(ClientWorld clientWorld, int i) {
      if (this.pendingBreakId >= 0 && this.pendingBreakAge < i) {
         Entity var4 = clientWorld.getEntityById(this.pendingBreakId);
         EndCrystalEntity var3;
         return var4 instanceof EndCrystalEntity && (var3 = (EndCrystalEntity)var4).isAlive() ? var3 : null;
      } else {
         return null;
      }
   }

   private Combo.Shot choose(
      MinecraftClient minecraftClient, CrystalAuraModule.Fight fight, EndCrystalEntity endCrystalEntity, boolean bl, AnchorAuraModule anchorAuraModule
   ) {
      ArrayList var7 = new ArrayList();
      Combo.Shot var8 = null;
      float var9 = 0.0F;
      if (bl) {
         CrystalAuraModule.Plan var10 = this.stagedBreak(fight);
         CrystalAuraModule.Plan var11 = this.planCrystal(fight, endCrystalEntity, 0, var10);
         if (var11 != null) {
            var8 = this.shot(fight, var11, false);
            if (this.due(var11, 0)) {
               var7.add(var8);
            }
         }

         if (anchorAuraModule != null && var10 != null) {
            var9 = this.shot(fight, var10, true).value();
         }
      }

      if (anchorAuraModule != null) {
         var7.addAll(anchorAuraModule.shots(minecraftClient, fight.player, fight.enemies, fight.friends, fight.rtt, var9));
      }

      Combo.Shot var6;
      return (var6 = Combo.pick(var7)) != null ? var6 : var8;
   }

   private void show(CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan) {
      CrystalAuraModule.Side var3 = fight.side(plan.step());
      boolean var4 = plan.step() == CrystalAuraModule.Step.OBSIDIAN;
      Vec3d var5 = plan.step() == CrystalAuraModule.Step.BREAK ? plan.crystal().getEntityPos() : Crystals.explosionAt(plan.target());
      float var6 = 0.0F;

      for (CrystalDamage.Victim var8 : var3.enemies) {
         var6 = Math.max(var6, var8.damage(var5, var4));
      }

      BlockPos var9 = plan.step() == CrystalAuraModule.Step.BREAK ? plan.crystal().getBlockPos().down() : plan.target();
      this.view.show(new Box(var9), this.renderColor.argb(), var6, var3.self.damage(var5, var4));
   }

   private CrystalAuraModule.Plan planCrystal(CrystalAuraModule.Fight fight, EndCrystalEntity endCrystalEntity, int i, CrystalAuraModule.Plan plan) {
      CrystalAuraModule.Plan var6 = this.planBreak(fight, false);
      if (var6 != null && this.due(var6, i)) {
         return var6;
      } else {
         boolean var5 = var6 != null || plan != null || Crystals.anyPlacing(fight.player, fight.window);
         CrystalAuraModule.Plan var8 = var5 ? null : this.planPlacement(fight, endCrystalEntity, endCrystalEntity == null);
         return var8 != null ? var8 : var6;
      }
   }

   private CrystalAuraModule.Plan stagedBreak(CrystalAuraModule.Fight fight) {
      return this.planBreak(fight, true);
   }

   private Combo.Shot shot(CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan, boolean bl) {
      CrystalAuraModule.Step var5 = plan.step();
      Vec3d var4 = var5 == CrystalAuraModule.Step.BREAK ? plan.crystal().getEntityPos() : Crystals.explosionAt(plan.target());
      int var7 = var5 == CrystalAuraModule.Step.BREAK ? 0 : (var5 == CrystalAuraModule.Step.CRYSTAL ? fight.rtt : fight.rtt + 1);
      return new Combo.Shot(
         Combo.Kind.valueOf(var5.name()), plan.aim(), plan.route(), this.score(fight, var5, var4, -1.0F / 0.0F, bl), kills(fight, var5, var4, bl), var7, plan
      );
   }

   private CrystalAuraModule.Plan planBreak(CrystalAuraModule.Fight fight, boolean bl) {
      double var3 = fight.player.getEntityInteractionRange();
      CrystalAuraModule.Plan var5 = null;
      float var6 = -1.0F / 0.0F;
      Box var7 = fight.player.getBoundingBox().expand(var3 + 2.0);

      for (EndCrystalEntity var9 : fight.level.getNonSpectatingEntities(EndCrystalEntity.class, var7)) {
         Vec3d var12;
         if (var9.isAlive()
            && this.breakAttempts.get(var9.getId()) < 3
            && (var9.getId() != this.pendingBreakId || this.pendingBreakAge >= fight.window)
            && (!this.doomed.containsKey(var9.getId()) || fight.player.age - this.doomed.get(var9.getId()) >= fight.window)
            && !((var12 = fight.eye).squaredDistanceTo(TargetingUtils.closestPoint(var12, var9.getBoundingBox())) > var3 * var3)
            && !(var9.getY() < this.tickFloor - 0.01)) {
            Vec3d var13 = var9.getEntityPos();
            float var10;
            CrystalAuraModule.Plan var11;
            if ((!this.protectLoot.get() || !LootGuard.near(var13))
               && !(this.bound(fight, CrystalAuraModule.Step.BREAK, var13) + fight.lookBonus <= var6)
               && (var11 = this.breakLook(fight, var9)) != null
               && fight.inView(var11)
               && (var10 = this.score(fight, CrystalAuraModule.Step.BREAK, var13, var6 - fight.lookBonus, bl)) != -1.0F / 0.0F
               && !((var10 = var10 + fight.lookCredit(var11)) <= var6)) {
               var5 = var11;
               var6 = var10;
            }
         }
      }

      if (var5 == null) {
         return null;
      } else {
         int var14 = Crystals.weaponRoute(fight.player);
         return var14 == -1 ? null : var5.withRoute(var14);
      }
   }

   private CrystalAuraModule.Plan breakLook(CrystalAuraModule.Fight fight, EndCrystalEntity endCrystalEntity) {
      if (fight.fixedLook != null) {
         CrystalAuraModule.Plan var9 = new CrystalAuraModule.Plan(
            CrystalAuraModule.Step.BREAK, endCrystalEntity, null, null, endCrystalEntity.getBoundingBox().getCenter(), -2
         );
         return hitAlong(fight.player, var9, fight.eye, fight.fixedLook) != null ? var9 : null;
      } else {
         BlockPos var4 = endCrystalEntity.getBlockPos().down();
         CrystalAuraModule.Plan var3;
         if (Crystals.isBase(fight.level.getBlockState(var4))
            && hitAlong(
                  fight.player,
                  var3 = new CrystalAuraModule.Plan(CrystalAuraModule.Step.BREAK, endCrystalEntity, var4, var4, aimOnTop(fight.level, var4), -2),
                  fight.aimEye,
                  fight.lookAt(var3.aim())
               )
               != null) {
            return var3;
         } else {
            Vec3d var5 = fight.towardLook(endCrystalEntity.getBoundingBox().contract(0.1));
            CrystalAuraModule.Plan var6 = new CrystalAuraModule.Plan(CrystalAuraModule.Step.BREAK, endCrystalEntity, null, null, var5, -2);
            if (hitAlong(fight.player, var6, fight.aimEye, fight.lookAt(var5)) != null) {
               return var6;
            } else {
               Vec3d var7 = TargetingUtils.closestPoint(fight.aimEye, endCrystalEntity.getBoundingBox().contract(0.1));
               CrystalAuraModule.Plan var8 = new CrystalAuraModule.Plan(CrystalAuraModule.Step.BREAK, endCrystalEntity, null, null, var7, -2);
               return hitAlong(fight.player, var8, fight.aimEye, fight.lookAt(var7)) != null ? var8 : null;
            }
         }
      }
   }

   private CrystalAuraModule.Plan planPlacement(CrystalAuraModule.Fight fight, EndCrystalEntity endCrystalEntity, boolean bl) {
      CrystalAuraModule.Plan var4 = this.planBase(fight, CrystalAuraModule.Step.CRYSTAL, endCrystalEntity);
      if (this.autoObsidian.get() && bl) {
         boolean var5 = fight.player.age <= this.eagerUntil;
         float var6 = var4 == null ? 0.0F : this.enemyDamage(fight, var4);
         if (var4 != null) {
            float var7 = var5 ? 11.0F : 8.0F;
            if (var6 >= var7) {
               return var4;
            }
         }

         CrystalAuraModule.Plan var12;
         if ((var12 = this.planBase(fight, CrystalAuraModule.Step.OBSIDIAN, null)) != null && var4 != null) {
            float var8 = this.enemyDamage(fight, var12);
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

   private float enemyDamage(CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan) {
      Vec3d var3 = Crystals.explosionAt(plan.target());
      float var4 = 0.0F;

      for (CrystalDamage.Victim var6 : fight.side(plan.step()).enemies) {
         var4 = Math.max(var4, var6.damage(var3, plan.step() == CrystalAuraModule.Step.OBSIDIAN));
      }

      return var4;
   }

   private CrystalAuraModule.Plan planBase(CrystalAuraModule.Fight fight, CrystalAuraModule.Step step, EndCrystalEntity endCrystalEntity) {
      ClientPlayerEntity var6 = fight.player;
      ClientWorld var7 = fight.level;
      Item var8 = step == CrystalAuraModule.Step.CRYSTAL ? Items.END_CRYSTAL : Items.OBSIDIAN;
      BlockState var5;
      if (Crystals.route(var6, var8, var5 = Blocks.OBSIDIAN.getDefaultState(), this.savedSlot) == -1) {
         return null;
      } else if (step == CrystalAuraModule.Step.OBSIDIAN && Crystals.route(var6, Items.END_CRYSTAL, var5, this.savedSlot) == -1) {
         return null;
      } else {
         double var9 = fight.placeReach;
         double var11 = var6.getEntityInteractionRange() - 0.1;
         Vec3d var13 = fight.eye;
         BlockPos var14 = BlockPos.ofFloored(var13);
         int var15 = MathHelper.ceil(var9);
         float var16 = step == CrystalAuraModule.Step.CRYSTAL && fight.lookBonus > 0.0F ? 2.0F : 0.0F;
         ArrayList var17 = new ArrayList();
         Mutable var18 = new Mutable();
         Mutable var19 = new Mutable();

         for (int var20 = -var15; var20 <= var15; var20++) {
            for (int var21 = -var15; var21 <= var15; var21++) {
               for (int var22 = -var15; var22 <= var15; var22++) {
                  var18.set(var14, var20, var21, var22);
                  if (var18.getY() + 1 >= this.tickFloor) {
                     BlockState var25 = var7.getBlockState(var18);
                     if (step == CrystalAuraModule.Step.CRYSTAL ? Crystals.isBase(var25) : var25.isReplaceable()) {
                        var19.set(var18, Direction.DOWN);
                        if ((step != CrystalAuraModule.Step.OBSIDIAN || var7.getBlockState(var19).isSideSolidFullSquare(var7, var19, Direction.UP))
                           && (
                              step == CrystalAuraModule.Step.CRYSTAL
                                 ? !Crystals.placing(var6, var18, fight.window)
                                 : !var18.equals(this.pendingObsidian) || this.pendingObsidianAge >= fight.window
                           )) {
                           BlockPos var26 = var18.toImmutable();
                           BlockPos var27 = step == CrystalAuraModule.Step.CRYSTAL ? var26 : var19.toImmutable();
                           Vec3d var28 = aimOnTop(var7, var27);
                           float var23;
                           if (!(var13.squaredDistanceTo(var28) > var9 * var9)
                              && !(var13.squaredDistanceTo(TargetingUtils.closestPoint(var13, Crystals.boxOn(var26))) > var11 * var11)
                              && (!this.protectLoot.get() || !LootGuard.near(Crystals.explosionAt(var26)))
                              && (var23 = this.bound(fight, step, Crystals.explosionAt(var26))) != -1.0F / 0.0F) {
                              var17.add(new CrystalAuraModule.Candidate(new CrystalAuraModule.Plan(step, null, var27, var26, var28, -2), var23));
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
         while (var31.hasNext() && !((var4 = (CrystalAuraModule.Candidate)var31.next()).bound() + fight.lookBonus + var16 <= var30)) {
            CrystalAuraModule.Plan var24 = var4.plan();
            float var34 = this.score(fight, step, Crystals.explosionAt(var24.target()), var30 - fight.lookBonus - var16, false);
            CrystalAuraModule.Plan var32;
            if (var34 != -1.0F / 0.0F
               && var7.getWorldBorder().contains(var24.clicked())
               && this.canHostSoon(fight, var24.target(), step == CrystalAuraModule.Step.CRYSTAL ? endCrystalEntity : null)
               && (var32 = this.reachable(fight, var24)) != null
               && fight.inView(var32)) {
               var34 += fight.lookCredit(var32);
               if (var16 > 0.0F && oneLook(fight.aimEye, Crystals.boxOn(var32.target()), var32.aim(), var11)) {
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
            int var33 = Crystals.route(var6, var8, var7.getBlockState(var29.clicked()), this.savedSlot);
            return var33 == -1 ? null : var29.withRoute(var33);
         }
      }
   }

   private CrystalAuraModule.Plan reachable(CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan) {
      if (fight.fixedLook == null) {
         BlockPos var3 = plan.clicked();
         double var4 = plan.aim().y;
         Vec3d var6 = fight.towardLook(new Box(var3.getX() + 0.1, var4, var3.getZ() + 0.1, var3.getX() + 1.0 - 0.1, var4, var3.getZ() + 1.0 - 0.1));
         CrystalAuraModule.Plan var7 = new CrystalAuraModule.Plan(plan.step(), null, var3, plan.target(), var6, plan.route());
         if (fight.eye.squaredDistanceTo(var6) <= fight.placeReach * fight.placeReach && hitAlong(fight.player, var7, fight.aimEye, fight.lookAt(var6)) != null
            )
          {
            return var7;
         }
      }

      if (hitAlong(fight.player, plan, fight.aimEye, fight.lookAt(plan.aim())) != null) {
         return plan;
      } else if (plan.step() == CrystalAuraModule.Step.CRYSTAL && fight.fixedLook == null) {
         Vec3d var8 = Vec3d.ofCenter(plan.clicked());

         for (Direction var5 : Type.HORIZONTAL) {
            Vec3d var11 = var8.add(var5.getOffsetX() * 0.4, 0.0, var5.getOffsetZ() * 0.4);
            CrystalAuraModule.Plan var10;
            if (!((fight.aimEye.x - var11.x) * var5.getOffsetX() + (fight.aimEye.z - var11.z) * var5.getOffsetZ() <= 0.0)
               && hitAlong(
                     fight.player,
                     var10 = new CrystalAuraModule.Plan(plan.step(), null, plan.clicked(), plan.target(), var11, plan.route()),
                     fight.aimEye,
                     fight.lookAt(var11)
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

   private static Vec3d aimOnTop(ClientWorld clientWorld, BlockPos blockPos) {
      double var2 = clientWorld.getBlockState(blockPos).getOutlineShape(clientWorld, blockPos).getMax(Axis.Y);
      return new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + var2 - 0.1, blockPos.getZ() + 0.5);
   }

   private float need(LivingEntity livingEntity, boolean bl, boolean bl2) {
      float var4 = !bl2 && (!this.facePlace.get() || !facePlaced(livingEntity)) ? this.tickMin : Math.min(2.0F, this.tickMin);
      return bl ? var4 * 0.6F : var4;
   }

   private static boolean facePlaced(LivingEntity livingEntity) {
      return CrystalDamage.health(livingEntity) <= 10.0F || Combo.inHole(livingEntity) || armorLow(livingEntity);
   }

   private static boolean armorLow(LivingEntity livingEntity) {
      for (EquipmentSlot var2 : EquipmentSlot.VALUES) {
         ItemStack var3;
         if (var2.isArmorSlot()
            && (var3 = livingEntity.getEquippedStack(var2)).isDamageable()
            && var3.getMaxDamage() - var3.getDamage() <= var3.getMaxDamage() * 0.1F) {
            return true;
         }
      }

      return false;
   }

   private float bound(CrystalAuraModule.Fight fight, CrystalAuraModule.Step step, Vec3d vec3d) {
      float var4 = -1.0F / 0.0F;

      for (CrystalDamage.Victim var6 : fight.side(step).enemies) {
         LivingEntity var7 = var6.entity();
         float var8 = var6.upperBound(vec3d);
         if (Combo.kills(var8, var7) || !(var8 < this.need(var7, step == CrystalAuraModule.Step.BREAK, fight.forced))) {
            var4 = Math.max(var4, Combo.value(var8, var7));
         }
      }

      return var4;
   }

   private float score(CrystalAuraModule.Fight fight, CrystalAuraModule.Step step, Vec3d vec3d, float f, boolean bl) {
      CrystalAuraModule.Side var6 = fight.side(step);
      boolean var7 = step == CrystalAuraModule.Step.OBSIDIAN;
      float var8 = var6.self.damage(vec3d, var7);
      if (!(var8 > this.tickSelf) && !CrystalDamage.endangers(fight.player, var8)) {
         for (CrystalDamage.Victim var10 : var6.friends) {
            float var11 = var10.damage(vec3d, var7);
            if (var11 > this.tickSelf || CrystalDamage.endangers((PlayerEntity)var10.entity(), var11)) {
               return -1.0F / 0.0F;
            }
         }

         float var14 = -1.0F / 0.0F;

         for (CrystalDamage.Victim var16 : var6.enemies) {
            LivingEntity var12 = var16.entity();
            float var13 = effective(fight, var16, step, vec3d, bl);
            if (Combo.kills(var13, var12) || !(var13 < this.need(var12, step == CrystalAuraModule.Step.BREAK, fight.forced))) {
               var14 = Math.max(var14, Combo.value(var13, var12) - 0.5F * var8);
            }
         }

         return var14 > f ? var14 : -1.0F / 0.0F;
      } else {
         return -1.0F / 0.0F;
      }
   }

   private static boolean kills(CrystalAuraModule.Fight fight, CrystalAuraModule.Step step, Vec3d vec3d, boolean bl) {
      for (CrystalDamage.Victim var5 : fight.side(step).enemies) {
         if (Combo.kills(effective(fight, var5, step, vec3d, bl), var5.entity())) {
            return true;
         }
      }

      return false;
   }

   private static float effective(CrystalAuraModule.Fight fight, CrystalDamage.Victim victim, CrystalAuraModule.Step step, Vec3d vec3d, boolean bl) {
      int var5 = step == CrystalAuraModule.Step.BREAK ? (bl ? HurtClock.opensIn(victim.entity(), fight.rtt) : 0) : fight.rtt;
      return HurtClock.effective(victim, victim.raw(vec3d, step == CrystalAuraModule.Step.OBSIDIAN), var5, fight.rtt);
   }

   private static HitResult hitAlong(ClientPlayerEntity clientPlayerEntity, CrystalAuraModule.Plan plan, Vec3d vec3d, Rotation rotation) {
      if (rotation == null) {
         return null;
      } else {
         Vec3d var4 = Vec3d.fromPolar(rotation.pitch(), rotation.yaw());
         if (plan.step() == CrystalAuraModule.Step.BREAK) {
            Box var8 = plan.crystal().getBoundingBox();
            if (var8.contains(vec3d)) {
               return new EntityHitResult(plan.crystal(), vec3d);
            } else {
               Vec3d var9 = var8.raycast(vec3d, vec3d.add(var4.multiply(clientPlayerEntity.getEntityInteractionRange()))).orElse(null);
               return var9 != null && MeleeAttacks.lineIsClear(clientPlayerEntity, vec3d, var9) ? new EntityHitResult(plan.crystal(), var9) : null;
            }
         } else {
            ClientWorld var5 = (ClientWorld)clientPlayerEntity.getEntityWorld();
            BlockState var6 = var5.getBlockState(plan.clicked());
            BlockHitResult var7 = var6.getOutlineShape(var5, plan.clicked())
               .raycast(vec3d, vec3d.add(var4.multiply(clientPlayerEntity.getBlockInteractionRange())), plan.clicked());
            if (var7 != null && MeleeAttacks.lineIsClear(clientPlayerEntity, vec3d, var7.getPos())) {
               return plan.step() == CrystalAuraModule.Step.OBSIDIAN && !plan.target().equals(Crystals.placedObsidian(var5, clientPlayerEntity, var7, var6))
                  ? null
                  : var7;
            } else {
               return null;
            }
         }
      }
   }

   private boolean perform(
      MinecraftClient minecraftClient,
      ClientPlayerEntity clientPlayerEntity,
      CrystalAuraModule.Fight fight,
      CrystalAuraModule.Plan plan,
      CrystalAuraModule.Look look
   ) {
      Vec3d var8 = clientPlayerEntity.getEyePos();
      Rotation var9 = ServerRotation.sent();
      Rotation var10 = look == CrystalAuraModule.Look.SILENT ? var9 : RotationUtils.entityRotation(clientPlayerEntity);
      HitResult var7 = var9 == null ? null : hitAlong(clientPlayerEntity, plan, var8, var9);
      if (var7 != null && var10 != var9 && hitAlong(clientPlayerEntity, plan, var8, var10) == null) {
         var7 = null;
      }

      if (plan.route() != -2) {
         this.select(clientPlayerEntity, plan.route());
      }

      boolean var6 = var7 != null
         && this.due(plan, 0)
         && (
            plan.step() == CrystalAuraModule.Step.BREAK
               ? this.strike(minecraftClient, clientPlayerEntity, fight, plan.crystal())
               : this.place(minecraftClient, clientPlayerEntity, plan, (BlockHitResult)var7)
         );
      if (var6) {
         MeleeAttacks.claimTick(minecraftClient);
      } else if (look == CrystalAuraModule.Look.SILENT) {
         this.turn(var7 != null ? var9 : fight.lookAt(plan.aim()));
      }

      return var6;
   }

   private boolean fire(
      MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, CrystalAuraModule.Fight fight, AnchorAuraModule anchorAuraModule, Combo.Shot shot
   ) {
      if (shot.route() != -2) {
         this.select(clientPlayerEntity, shot.route());
      }

      if (!anchorAuraModule.ready(minecraftClient, clientPlayerEntity, shot)) {
         this.turn(fight.lookAt(shot.aim()));
         return false;
      } else {
         anchorAuraModule.fire(minecraftClient, clientPlayerEntity, shot);
         return true;
      }
   }

   private boolean strike(
      MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, CrystalAuraModule.Fight fight, EndCrystalEntity endCrystalEntity
   ) {
      if (this.mainAge < 2 && !Crystals.enchantsAddDamage(clientPlayerEntity.getMainHandStack()) && !Crystals.dealsDamage(clientPlayerEntity, ItemStack.EMPTY)) {
         return false;
      } else if (!Crystals.strike(minecraftClient, clientPlayerEntity, endCrystalEntity)) {
         return false;
      } else {
         this.markBreak(endCrystalEntity);
         this.breakAttempts.addTo(endCrystalEntity.getId(), 1);
         this.ticksSinceBreak = 0;
         this.breakGap = this.humanize.get() && !this.fullSpeed ? Crystals.humanGap() : 0;

         for (CrystalDamage.Victim var6 : fight.side(CrystalAuraModule.Step.BREAK).enemies) {
            float var7 = var6.raw(endCrystalEntity.getEntityPos(), false);
            if (var7 > 0.0F) {
               HurtClock.shot(clientPlayerEntity, var6.entity(), var7, endCrystalEntity.getId(), fight.rtt);
            }
         }

         Box var8 = endCrystalEntity.getBoundingBox().expand(12.0);

         for (EndCrystalEntity var10 : fight.level.getNonSpectatingEntities(EndCrystalEntity.class, var8)) {
            if (var10 != endCrystalEntity && var10.squaredDistanceTo(endCrystalEntity) <= 144.0) {
               this.doomed.put(var10.getId(), clientPlayerEntity.age);
            }
         }

         return true;
      }
   }

   private boolean place(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, CrystalAuraModule.Plan plan, BlockHitResult blockHitResult) {
      if (!Crystals.rightClick(minecraftClient, clientPlayerEntity, blockHitResult)) {
         return false;
      } else {
         if (plan.step() == CrystalAuraModule.Step.CRYSTAL) {
            Crystals.notePlace(clientPlayerEntity, plan.target());
         } else {
            this.pendingObsidian = plan.target();
            this.pendingObsidianAge = 0;
         }

         this.ticksSincePlace = 0;
         this.placeGap = this.humanize.get() && !this.fullSpeed ? Crystals.humanGap() : 0;
         return true;
      }
   }

   private void turn(Rotation rotation) {
      ServerRotation.request(rotation, this.fullSpeed ? this.turnSpeed.max() : this.turnSpeed.get());
   }

   private static boolean sameLook(Rotation rotation, Rotation rotation2) {
      return rotation != null
         && Math.abs(RotationUtils.yawDelta(rotation.yaw(), rotation2.yaw())) <= 2.0F
         && Math.abs(rotation.pitch() - rotation2.pitch()) <= 2.0F;
   }

   private boolean due(CrystalAuraModule.Plan plan, int i) {
      return plan.step() == CrystalAuraModule.Step.BREAK
         ? this.ticksSinceBreak + i > Math.max(this.breakDelay.getInt(), this.breakGap - 1)
         : this.ticksSincePlace + i > Math.max(this.placeDelay.getInt(), this.placeGap - 1);
   }

   private void select(ClientPlayerEntity clientPlayerEntity, int i) {
      PlayerInventory var3 = clientPlayerEntity.getInventory();
      if (this.savedSlot == -1) {
         this.savedSlot = var3.getSelectedSlot();
      }

      var3.setSelectedSlot(i);
      this.ownSlot = i;
      this.lastMain = clientPlayerEntity.getMainHandStack();
      this.mainAge = 0;
   }

   private void markBreak(EndCrystalEntity endCrystalEntity) {
      this.pendingBreakId = endCrystalEntity.getId();
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
         ClientPlayerEntity clientPlayerEntity,
         ClientWorld clientWorld,
         List<LivingEntity> list,
         List<LivingEntity> list2,
         int i,
         int j,
         Rotation rotation,
         float f,
         boolean bl
      ) {
         this.player = clientPlayerEntity;
         this.level = clientWorld;
         this.eye = clientPlayerEntity.getEyePos();
         this.aimEye = rotation != null ? this.eye : this.eye.add(clientPlayerEntity.getVelocity());
         this.enemies = list;
         this.friends = list2;
         this.window = i;
         this.rtt = j;
         this.fixedLook = rotation;
         this.serverLook = ServerRotation.sent();
         this.lookBonus = f;
         this.arriveLead = Math.min(j, 5);
         this.placeLead = Math.min(2 * j, 5);
         this.selfPlaceLead = Math.min(j, 5);
         this.forced = bl;
         this.placeReach = clientPlayerEntity.getBlockInteractionRange() - 0.1;
      }

      CrystalAuraModule.Side side(CrystalAuraModule.Step step) {
         if (step == CrystalAuraModule.Step.BREAK) {
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

      Rotation lookAt(Vec3d vec3d) {
         return this.fixedLook != null ? this.fixedLook : RotationUtils.fromTo(this.aimEye, vec3d);
      }

      boolean holds(CrystalAuraModule.Plan plan) {
         return CrystalAuraModule.hitAlong(this.player, plan, this.eye, this.serverLook) != null;
      }

      float lookCredit(CrystalAuraModule.Plan plan) {
         return this.lookBonus > 0.0F && this.holds(plan) ? this.lookBonus : 0.0F;
      }

      boolean inView(CrystalAuraModule.Plan plan) {
         if (this.fixedLook == null && !(CrystalAuraModule.this.viewAngle.get() >= 180.0F)) {
            float var2 = Math.abs(RotationUtils.yawDelta(this.player.getYaw(), RotationUtils.fromTo(this.aimEye, plan.aim()).yaw()));
            return var2 <= CrystalAuraModule.this.viewAngle.get() || var2 <= CrystalAuraModule.this.viewAngle.get() + 20.0F && this.holds(plan);
         } else {
            return true;
         }
      }

      Vec3d towardLook(Box box) {
         Rotation var4 = this.serverLook != null ? this.serverLook : RotationUtils.entityRotation(this.player);
         Vec3d var5 = Vec3d.fromPolar(var4.pitch(), var4.yaw());
         Vec3d var6 = box.getCenter();
         boolean var7 = box.getLengthY() < 1.0E-6 && var5.y < -1.0E-6 && this.aimEye.y > box.minY;
         double var2 = var7 ? (box.minY - this.aimEye.y) / var5.y : var6.subtract(this.aimEye).dotProduct(var5);
         if (var2 <= 0.0) {
            return var6;
         } else {
            Vec3d var10 = this.aimEye.add(var5.multiply(var2));
            return new Vec3d(
               MathHelper.clamp(var10.x, box.minX, box.maxX), MathHelper.clamp(var10.y, box.minY, box.maxY), MathHelper.clamp(var10.z, box.minZ, box.maxZ)
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
      CrystalAuraModule.Plan withRoute(int i) {
         return new CrystalAuraModule.Plan(this.step, this.crystal, this.clicked, this.target, this.aim, i);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Side {
      final List<CrystalDamage.Victim> enemies = new ArrayList<>();
      final List<CrystalDamage.Victim> friends = new ArrayList<>();
      final CrystalDamage.Victim self;

      Side(ClientPlayerEntity clientPlayerEntity, List<LivingEntity> list, List<LivingEntity> list2, int i, int j) {
         for (LivingEntity var7 : list) {
            this.enemies.add(new CrystalDamage.Victim(var7, Combo.lead(var7, i)));
         }

         for (LivingEntity var9 : list2) {
            this.friends.add(new CrystalDamage.Victim(var9, Combo.lead(var9, i)));
         }

         this.self = new CrystalDamage.Victim(clientPlayerEntity, Combo.lead(clientPlayerEntity, j));
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
