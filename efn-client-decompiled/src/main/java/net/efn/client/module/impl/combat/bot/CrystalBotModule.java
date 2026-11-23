package net.efn.client.module.impl.combat.bot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.efn.client.chat.ModuleChat;
import net.efn.client.easyplace.builder.Aim;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.AnchorAuraModule;
import net.efn.client.module.impl.combat.AutoCityModule;
import net.efn.client.module.impl.combat.AutoInvTotemModule;
import net.efn.client.module.impl.combat.AutoTotemModule;
import net.efn.client.module.impl.combat.AutoTrapModule;
import net.efn.client.module.impl.combat.CrystalAuraModule;
import net.efn.client.module.impl.combat.anchor.AnchorGuard;
import net.efn.client.module.impl.combat.crystal.Combo;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class CrystalBotModule extends ClientModule {
   private static final double MIN = 2.4;
   private static final double IDEAL = 3.2;
   private static final double MAX = 4.0;
   private static final Predicate<ItemStack> GAPPLE = var0 -> var0.method_31574(Items.field_8463) || var0.method_31574(Items.field_8367);
   private static final Predicate<ItemStack> XP = var0 -> var0.method_31574(Items.field_8287);
   private static final Predicate<ItemStack> PEARL = var0 -> var0.method_31574(Items.field_8634);
   private static final Predicate<ItemStack> OBSIDIAN = var0 -> var0.method_31574(Items.field_8281);
   private static final Predicate<ItemStack> ANCHOR = var0 -> var0.method_31574(Items.field_23141);
   private static final Predicate<ItemStack> GLOWSTONE = var0 -> var0.method_31574(Items.field_8801);
   private static final float ARMOR_LOW = 0.3F;
   private static final int RUN_HOLD = 40;
   private static final int POP_COVER = 40;
   private static final int LAUNCH_GAP = 80;
   private static final int LAUNCH_STALL = 20;
   private static final int LAUNCH_TIMEOUT = 16;
   private static final double LAUNCH_START = 3.9;
   private static final double LAUNCH_GIVE_UP = 4.1;
   private static final double LAUNCH_STEP_IN = 2.9;
   private static final int LAUNCH_WATCH = 8;
   private static final double LAUNCH_LIFT = 0.3;
   private static final double HIT_RANGE = 2.95;
   private static final int THROW_TIMEOUT = 14;
   private static final int PEARL_GAP = 30;
   private static final int SHIELD_GAP = 20;
   private static final int BREACH_WAIT = 40;
   private static final int BREACH_WAIT_CAMPER = 15;
   private static final int BREACH_GAP = 100;
   private static final double ESCAPE_FAR = 20.0;
   private static final double ESCAPE_NEAR = 12.0;
   private static final int SEARCH_GAP = 10;
   private static final double EAT_CLEAR = 8.0;
   private static final int FOLLOW_TICKS = 12;
   private static final int OPENER_WINDOW = 40;
   private static final int CAMP_MAX = 2;
   private static final int SPOT_SCAN_GAP = 5;
   private static final float SPOT_DANGER = 8.0F;
   private static final int WORKING = 0;
   private static final int PLACED = 1;
   private static final int REGROUP_TICKS = 300;
   private static final double REGROUP_RANGE = 64.0;
   private static final int BOX_AIM_TIMEOUT = 10;
   private static final float RETREAT_HEALTH = 6.0F;
   private static final int POP_STREAK = 3;
   private static final int POP_STREAK_GAP = 600;
   private static final int POP_RUN = 200;
   private static final double BLAST_WATCH = 7.0;
   private static final double BELOW_DROP = 3.0;
   private static final double BELOW_REACH = 8.0;
   private static final double DIG_DEEPEST = 6.0;
   private static final int BELOW_SETTLE = 20;
   private static final int DIG_TIMEOUT = 80;
   private static final int DIG_WALK = 40;
   private static final int DIG_MAX = 2;
   private static final int DIG_GAP = 100;
   private static final int DIG_DOWN_TICKS = 50;
   private static final int DIG_BASE_TICKS = 25;
   private static final float LEVEL_MIN_DAMAGE = 2.5F;
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Fight Range", 16.0F, 8.0F, 32.0F).step(1.0F));
   private final BooleanSetting anchors = this.addSetting("anchors", new BooleanSetting("Use Anchors", true));
   private final BooleanSetting launches = this.addSetting("knock-up", new BooleanSetting("Knock-Up Hits", true));
   private final BooleanSetting breach = this.addSetting("break-holes", new BooleanSetting("Break Holes", true));
   private final BooleanSetting guardAnchors = this.addSetting("guard-anchors", new BooleanSetting("Guard Anchors", true));
   private final BooleanSetting surround = this.addSetting("surround", new BooleanSetting("Surround Self", true));
   private final NumberSetting eatBelow = this.addSetting("eat-below", new NumberSetting("Gapple Below HP", 14.0F, 0.0F, 20.0F).step(1.0F));
   private final BooleanSetting mendArmor = this.addSetting("mend-armor", new BooleanSetting("Mend Armor", true));
   private final BooleanSetting escape = this.addSetting("escape-pearls", new BooleanSetting("Escape Pearl", true));
   private final BooleanSetting chase = this.addSetting("chase-pearls", new BooleanSetting("Chase Pearls", true));
   private final BooleanSetting refill = this.addSetting("refill", new BooleanSetting("Refill Hotbar", true));
   private final BooleanSetting learn = this.addSetting("learn", new BooleanSetting("Learn Rivals", true));
   private final BooleanSetting dance = this.addSetting("strafe", new BooleanSetting("Strafe", true));
   private final MinecraftClient mc = MinecraftClient.method_1551();
   private final BotBody body = new BotBody();
   private final BotKit kit = new BotKit();
   private final RivalBook book = new RivalBook();
   private final BotPause pause = new BotPause();
   private final AirTrack air = new AirTrack();
   private PlayerEntity challenger;
   private int challengerSince;
   private int attackedBy = -1;
   private int attackedAt = -1000;
   private double closest;
   private int closerAt;
   private StandPicker.Spot standing;
   private int standScanAt;
   private int defuseAt = -1000;
   private int retreatAt = -1000;
   private Boolean crystalWasOn;
   private Boolean anchorWasOn;
   private Boolean totemWasOn;
   private int popStreak;
   private boolean poppedOut;
   private BlockPos digAnchor;
   private boolean digPlaced;
   private boolean digAnnounced;
   private int digGuards;
   private int digUntil;
   private int digWalkUntil;
   private int digBlasts;
   private int digAfter;
   private int belowSince = -1;
   private int stepDigAfter;
   private int baseDigAfter;
   private int diggingAt = -100;
   private boolean eating;
   private boolean needsGapple;
   private boolean mending;
   private BlockPos boxingAt;
   private int boxingUntil;
   private int tick;
   private PlayerEntity fought;
   private CrystalBotModule.Stance stance = CrystalBotModule.Stance.HOLD;
   private int runUntil;
   private int ownPopAt = -1000;
   private CrystalBotModule.Throw throwing;
   private Rotation throwLook;
   private Vec3d throwSpot;
   private double throwAway;
   private int throwUntil;
   private int pearlAt = -1000;
   private int searchAfter;
   private int theirPearl = -1;
   private int followedPearl = -1;
   private boolean launching;
   private int launchUntil;
   private int nextLaunch;
   private int launchedAt = -1;
   private double launchY;
   private double launchPeak;
   private int shieldAt = -1000;
   private BlockPos boxAiming;
   private int boxAimTicks;
   private BlockPos boxSkipped;
   private int boxSkippedUntil;
   private int followUntil;
   private float selfCapNow;
   private boolean followLaid = true;
   private BlockPos blocking;
   private int spotScanAt;
   private int cityFails;
   private int openerArm = -1;
   private double openerReach;
   private int openerAt;
   private float openerHealth;
   private int openerPops;
   private boolean cityStarted;
   private int breachAfter;
   private float lastTheirHealth;
   private int hurtAt;
   private int progressAt;
   private int theirPopAt = -1;
   private int holeSince = -1;
   private boolean lowSpell;
   private boolean pearledInSpell;

   public CrystalBotModule() {
      super(
         "crystal-bot",
         "Crystal PvP Bot",
         "Crystal-fights the nearest enemy player: steers the Crystal and Anchor Aura, knocks them up, breaks their hole, guards, eats, mends, refills and pearls.",
         ModuleCategory.COMBAT
      );
   }

   @Override
   protected void onEnable() {
      this.body.forget();
      this.needsGapple = false;
      this.cityStarted = false;
      this.launching = false;
      this.mending = false;
      this.eating = false;
      this.boxingAt = null;
      this.fought = null;
      this.throwing = null;
      this.tick = 0;
      this.runUntil = 0;
      this.boxSkippedUntil = 0;
      this.followUntil = 0;
      this.nextLaunch = 0;
      this.launchedAt = -1;
      this.shieldAt = -1000;
      this.pearlAt = -1000;
      this.ownPopAt = -1000;
      this.baseDigAfter = 0;
      this.stepDigAfter = 0;
      this.standScanAt = 0;
      this.spotScanAt = 0;
      this.breachAfter = 0;
      this.searchAfter = 0;
      this.retreatAt = -1000;
      this.attackedAt = -1000;
      this.defuseAt = -1000;
      this.attackedBy = -1;
      this.challenger = null;
      this.standing = null;
      this.air.forget();
      this.pause.reset();
      this.totemWasOn = null;
      this.anchorWasOn = null;
      this.crystalWasOn = null;
      this.popStreak = 0;
      this.forgetDig();
      this.on(TickEvent.TYPE, Stage.PRE, var1 -> this.tick());
   }

   @Override
   protected void onDisable() {
      this.body.release(this.mc);
      this.body.forget();
      CombatMacroLock.release(this);
      this.restoreAuras();
      AutoCityModule var1 = ModuleRegistry.get(AutoCityModule.class);
      if (this.cityStarted && var1 != null && var1.isEnabled()) {
         var1.setEnabled(false);
      }

      this.cityStarted = false;
      this.book.save(true);
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent var1) {
      ClientPlayerEntity var3 = this.mc.field_1724;
      if (var3 != null && this.mc.field_1687 != null) {
         Packet var4 = var1.packet;
         EntityStatusS2CPacket var2;
         if (var4 instanceof EntityStatusS2CPacket && (var2 = (EntityStatusS2CPacket)var4).method_11470() == 35) {
            if (var2.method_11469((World)this.mc.field_1687) == var3) {
               this.popStreak = this.tick - this.ownPopAt <= 600 ? this.popStreak + 1 : 1;
               this.ownPopAt = this.tick;
            } else if (this.fought != null && var2.method_11469((World)this.mc.field_1687) == this.fought) {
               this.theirPopAt = this.tick;
               this.openerPops++;
            }
         }
      }
   }

   @EventHandler
   public void onPacketReceive(PacketReceiveEvent var1) {
      ClientPlayerEntity var2 = this.mc.field_1724;
      if (var2 != null) {
         if (var1.packet instanceof PlayerPositionLookS2CPacket var4) {
            Vec3d var5 = var4.comp_3228().comp_3148();
            Vec3d var6 = var2.method_73189();
            Set var7 = var4.comp_3229();
            this.pause
               .moved(
                  new Vec3d(
                     var7.contains(PositionFlag.field_12400) ? var6.field_1352 + var5.field_1352 : var5.field_1352,
                     var7.contains(PositionFlag.field_12398) ? var6.field_1351 + var5.field_1351 : var5.field_1351,
                     var7.contains(PositionFlag.field_12403) ? var6.field_1350 + var5.field_1350 : var5.field_1350
                  ),
                  this.tick - this.pearlAt
               );
         } else if (var1.packet instanceof WorldTimeUpdateS2CPacket var11) {
            this.pause.time(var11.comp_3219());
         } else if (var1.packet instanceof ExplosionS2CPacket var12) {
            this.air.blast(var12.comp_2883());
         } else {
            Packet var10 = var1.packet;
            EntityDamageS2CPacket var13;
            if (var10 instanceof EntityDamageS2CPacket && (var13 = (EntityDamageS2CPacket)var10).comp_1267() == var2.method_5628() && var13.comp_1269() >= 0) {
               this.attackedBy = var13.comp_1269();
               this.attackedAt = this.tick;
            }
         }
      }
   }

   private void tick() {
      this.tick++;
      ClientPlayerEntity var5 = this.mc.field_1724;
      ClientWorld var6 = this.mc.field_1687;
      this.body.release(this.mc);
      if (var5 != null && var6 != null && MeleeAttacks.canAct(this.mc) && !var5.method_29504()) {
         CombatMacroLock.release(this);
         this.keepTotem();
         this.body.stayGrounded(blastNear(var6, var5));
         if (this.learn.get()) {
            this.book.save(false);
         }

         BotPause.State var4;
         if ((var4 = this.pause.tick(this.mc, var5)) == BotPause.State.STOP) {
            ModuleChat.send(this, ModuleChat.body().danger("The server keeps pulling us back").text(", stopping."));
            this.requestDisable();
         } else if (var4 != BotPause.State.RUN) {
            this.throwing = null;
            this.eating = false;
            this.launching = false;
            if (var4 == BotPause.State.HOLD) {
               CombatMacroLock.take(this);
            }
         } else {
            boolean var7 = this.needsGapple || this.eating || this.tick - this.retreatAt < 300;
            PlayerEntity var8 = this.choose(var6, var5, this.body.pick(this.mc, var5, this.range.get(), var7 ? 64.0 : this.range.get() + 8.0));
            if (var8 != this.fought) {
               this.newFight(var8);
            }

            boolean var3 = var5.method_6032() <= this.eatAt() && var5.method_6067() < 4.0F;
            if (var5.method_6032() > this.eatAt() + 4.0F || var5.method_6067() >= 4.0F) {
               this.needsGapple = false;
            }

            if (var8 == null) {
               this.launching = false;
               this.throwing = null;
               CombatMacroLock.release(this);
               if ((var3 || this.needsGapple || this.eating && var5.method_6115()) && this.hands() && this.body.use(this.mc, var5, GAPPLE)) {
                  this.eating = true;
               } else {
                  this.eating = false;
                  if (!this.refill.get() || !this.kit.refill(this.mc, var5, this.anchors.get())) {
                     if (!this.mendIdle(var5)) {
                        this.mending = false;
                     }
                  }
               }
            } else {
               RivalBook.Rival var10 = this.learn.get() ? this.book.of(var8.method_5667()) : null;
               this.watch(var6, var8, var10);
               if (feetY(var5) < MathHelper.method_15357(var8.method_23318() + 0.01)) {
                  this.body.stayGrounded(true);
               }

               this.air.note(var8, this.tick, var10);
               Combo.prefer(var5, var8);
               Combo.flight(var5, var8, var1x -> this.air.lead(var1x, this.tick));
               this.runAuras(var6);
               CrystalBotModule.Stance var11 = this.stance;
               this.stance = this.stance(var6, var5, var8, var10);
               if (this.stance == CrystalBotModule.Stance.RUN && var11 != CrystalBotModule.Stance.RUN) {
                  ModuleChat.send(
                     this,
                     this.poppedOut
                        ? ModuleChat.body().warn("3 totems popped").text(", getting away.")
                        : ModuleChat.body().warn("Losing").text(", getting away.")
                  );
               }

               this.poppedOut = false;
               this.steer(var5, var8, var10);
               double var12 = var5.method_5739(var8);
               if (var12 < this.closest - 0.5) {
                  this.closest = var12;
                  this.closerAt = this.tick;
               }

               if (this.throwing == null || !this.keepThrowing(var5, var8)) {
                  if (this.stance != CrystalBotModule.Stance.RUN || !this.getAway(var6, var5, var8)) {
                     if (!this.getDown(var6, var5, var8)) {
                        if (!this.guardAnchors.get() || !this.defuse(var6, var5) && !this.guard(var6, var5, var8)) {
                           if (!this.launching || !this.launch(var5, var8)) {
                              if (this.followLaid || this.tick > this.followUntil || !this.followUp(var6, var5, var8)) {
                                 boolean var2 = var8.method_6115()
                                    && var8.method_6030().method_57826(DataComponentTypes.field_50075)
                                    && totems(var5) >= 1
                                    && var5.method_6032() > 8.0F;
                                 if (var3 && !var2) {
                                    this.needsGapple = true;
                                 }

                                 if (this.needsGapple || this.eating && var5.method_6115()) {
                                    if (var12 >= 8.0) {
                                       if (BotBody.hotbarSlot(var5, GAPPLE) >= 0) {
                                          if (this.hands() && this.body.use(this.mc, var5, GAPPLE)) {
                                             this.eating = true;
                                          }

                                          this.body.face(this.mc, var5, var8, 1.0F);
                                          this.body.backOff(this.mc, var5);
                                          return;
                                       }
                                    } else if (this.critical(var5) && BotBody.hotbarSlot(var5, GAPPLE) >= 0) {
                                       this.eating = false;
                                       this.retreatAt = this.tick;
                                       if (this.pearlAway(var6, var5, var8)) {
                                          return;
                                       }

                                       this.body.flee(this.mc, var5, var8);
                                       return;
                                    }
                                 }

                                 this.eating = false;
                                 if (this.cover(var6, var5, var8, var12)) {
                                    this.body.face(this.mc, var5, var8, 1.0F);
                                 } else if (this.stance == CrystalBotModule.Stance.GUARD && this.closeSpot(var6, var5, var8)) {
                                    this.body.face(this.mc, var5, var8, 1.0F);
                                 } else if (!this.breach.get() || !this.breakHole(var6, var5, var8, var10)) {
                                    if (!this.mendArmor.get() || !(var12 >= 12.0) || !armorLow(var5) || !this.hands() || !this.throwXp(var5)) {
                                       this.mending = false;
                                       if (!this.chase.get() || !this.chasePearl(var6, var5, var8, var10)) {
                                          boolean var1 = var12 >= 8.0 || BotBody.hotbarSlot(var5, var0 -> var0.method_31574(Items.field_8301)) < 0;
                                          if (this.refill.get()
                                             && var1
                                             && this.kit.needed(var5, this.anchors.get())
                                             && this.hands()
                                             && this.kit.refill(this.mc, var5, this.anchors.get())) {
                                             this.body.face(this.mc, var5, var8, 1.0F);
                                          } else if (!this.finish(var5, var8)) {
                                             if (!this.launches.get() || !this.startLaunch(var5, var8, var10)) {
                                                if (!this.digDown(var6, var5, var8, var12) && !this.digBase(var6, var5, var8, var12)) {
                                                   CombatMacroLock.release(this);
                                                   handBack(var5);
                                                   this.body.face(this.mc, var5, var8, 1.0F);
                                                   if (!surrounded(var6, var5)) {
                                                      if (!this.stayLow(var6, var5, var8, var12)) {
                                                         if (this.tick >= this.standScanAt) {
                                                            this.standScanAt = this.tick + 10;
                                                            this.standing = StandPicker.pick(var6, var5, var8, 2.4, 3.2, 4.0, this.stance.cover);
                                                         }

                                                         if (this.standing == null || this.body.toward(this.mc, var5, this.standing.center())) {
                                                            this.standing = null;
                                                            this.body.keepDistance(this.mc, var5, var8, 2.4, 3.2, 4.0, this.dance.get());
                                                         }
                                                      }
                                                   }
                                                } else {
                                                   this.body.face(this.mc, var5, var8, 1.0F);
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         this.body.forget();
         CombatMacroLock.release(this);
         this.throwing = null;
         this.launching = false;
      }
   }

   private PlayerEntity choose(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3) {
      PlayerEntity var4 = null;
      double var5 = -1.0 / 0.0;

      for (PlayerEntity var8 : enemies(var1, var2, this.range.get())) {
         double var9 = this.worth(var2, var8);
         if (var9 > var5) {
            var4 = var8;
            var5 = var9;
         }
      }

      if (var3 == null || !var3.method_5805() || this.fought == null) {
         this.challenger = null;
         if (var4 != null) {
            this.body.hold(var4);
         }

         return var4;
      } else if (var4 != null && var4 != var3) {
         boolean var12 = var2.method_5739(var3) <= 6.0;
         double var13 = this.worth(var2, var3);
         double var10 = var12 ? 8.0 : 2.0;
         if (var5 < var13 + var10) {
            this.challenger = null;
            return var3;
         } else if (var4 != this.challenger) {
            this.challenger = var4;
            this.challengerSince = this.tick;
            return var3;
         } else if (this.tick - this.challengerSince < (var12 ? 20 : 6)) {
            return var3;
         } else {
            this.challenger = null;
            this.body.hold(var4);
            return var4;
         }
      } else {
         this.challenger = null;
         return var3;
      }
   }

   private double worth(ClientPlayerEntity var1, PlayerEntity var2) {
      double var3 = var1.method_5739(var2);
      double var5 = var3 <= 6.0 ? 1.0 : (var3 >= 10.0 ? 0.0 : (10.0 - var3) / 4.0);
      double var7 = 4.0 * var5 - 0.2 * var3 + (20.0F - Math.min(20.0F, CrystalDamage.health(var2))) / 5.0;
      if (!var2.method_6079().method_31574(Items.field_8288) && !var2.method_6047().method_31574(Items.field_8288)) {
         var7++;
      }

      if (armorLow(var2)) {
         var7++;
      }

      if (var2.method_5628() == this.attackedBy && this.tick - this.attackedAt <= 60 && var3 <= 4.5) {
         var7 += 4.0;
      }

      return var7;
   }

   private boolean finish(ClientPlayerEntity var1, PlayerEntity var2) {
      if (this.pause.slowServer() || var2.field_6235 > 0 || reach(var1, var2) > 2.95 || CrystalDamage.health(var2) > 3.0F) {
         return false;
      } else if (!MeleeAttacks.isMeleeWeapon(var1.method_6047()) || MeleeAttacks.charge(var1) < 0.9F) {
         return false;
      } else if (!this.hands()) {
         return true;
      } else {
         this.body.face(this.mc, var1, var2, 1.0F);
         this.strike(var1, var2);
         return true;
      }
   }

   private void newFight(PlayerEntity var1) {
      if (this.fought != null && this.lowSpell && this.pearledInSpell && this.learn.get()) {
         this.book.of(this.fought.method_5667()).lowSpell(true);
      }

      this.fought = var1;
      this.launching = false;
      this.holeSince = -1;
      this.theirPopAt = -1;
      this.launchedAt = -1;
      this.pearledInSpell = false;
      this.lowSpell = false;
      this.followedPearl = -1;
      this.theirPearl = -1;
      this.openerArm = -1;
      this.closest = 1.7976931348623157E308;
      this.closerAt = this.tick;
      this.standing = null;
      this.followLaid = true;
      this.blocking = null;
      this.cityFails = 0;
      this.air.forget();
      this.forgetDig();
      this.progressAt = this.hurtAt = this.tick;
      float var2 = this.lastTheirHealth = var1 == null ? 0.0F : CrystalDamage.health(var1);
      if (var1 != null) {
         ModuleChat.send(this, ModuleChat.body().text("Fighting ").name(var1.method_5477().getString()).text("."));
         if (this.learn.get()) {
            this.book.of(var1.method_5667()).fought();
         }
      } else {
         ModuleChat.send(this, ModuleChat.body().text("No enemy in range, waiting."));
      }
   }

   private CrystalBotModule.Stance stance(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3, RivalBook.Rival var4) {
      int var6 = totems(var2);
      float var7 = var2.method_6032() + var2.method_6067();
      float var8 = worstThreat(var1, var2, this.digAnchor) / Math.max(1.0F, var7);
      int var9 = enemiesWithin(var1, var2, 8.0);
      boolean var5 = var6 == 0 && var9 >= 2;
      if (var5) {
         this.runUntil = Math.max(this.runUntil, this.tick + 40);
      }

      if (this.popStreak >= 3) {
         this.popStreak = 0;
         this.poppedOut = true;
         this.runUntil = this.tick + 200;
      }

      if (this.tick < this.runUntil) {
         return CrystalBotModule.Stance.RUN;
      } else if (this.tick - this.ownPopAt <= 40) {
         return CrystalBotModule.Stance.GUARD;
      } else if (this.finishing(var3, var4)) {
         return CrystalBotModule.Stance.PRESS;
      } else {
         boolean var11 = var7 <= this.retreatAt() + 2.0F || var6 <= 1 || var8 >= 0.35F;
         return var11 && var7 <= 8.0F ? CrystalBotModule.Stance.GUARD : CrystalBotModule.Stance.PRESS;
      }
   }

   private boolean finishing(PlayerEntity var1, RivalBook.Rival var2) {
      if (this.theirPopAt >= 0 && !var1.method_6079().method_31574(Items.field_8288)) {
         int var3 = var2 == null ? 8 : MathHelper.method_15340(var2.retotemWindow(8), 3, 30);
         return this.tick - this.theirPopAt <= var3;
      } else {
         return false;
      }
   }

   private void steer(ClientPlayerEntity var1, PlayerEntity var2, RivalBook.Rival var3) {
      float var8 = 2.0F;
      float var9 = var1.method_6032() + var1.method_6067();
      float var10 = var9 - (var1.method_6079().method_31574(Items.field_8288) ? 1.0F : 4.0F);
      float var7;
      this.selfCapNow = var7 = Math.max(0.5F, Math.min(this.stance.selfCap + var8, var10));
      float var6 = this.finishing(var2, var3) ? 2.0F : this.stance.minDamage;
      if (feetY(var1) >= MathHelper.method_15357(var2.method_23318() + 0.01) && !Combo.inHole(var2)) {
         var6 = Math.min(var6, 2.5F);
      }

      CrystalAuraModule var5;
      if ((var5 = ModuleRegistry.get(CrystalAuraModule.class)) != null) {
         var5.steer(var1, var6, var7);
         var5.crystalsFrom(var1, feetY(var1) + 1);
         var5.fullSpeed(var1);
         var5.buildEagerly(var1);
      }

      AnchorAuraModule var4;
      if ((var4 = ModuleRegistry.get(AnchorAuraModule.class)) != null) {
         var4.steer(var1, this.finishing(var2, var3) ? 2.0F : this.stance.anchorMin, Math.min(var7, 11.0F));
      }
   }

   private float eatAt() {
      return this.eatBelow.get() - 2.0F;
   }

   private float retreatAt() {
      return 4.0F;
   }

   private boolean critical(ClientPlayerEntity var1) {
      return var1.method_6032() + var1.method_6067() <= this.retreatAt() && !var1.method_6079().method_31574(Items.field_8288);
   }

   private static int totems(ClientPlayerEntity var0) {
      int var1 = var0.method_6079().method_31574(Items.field_8288) ? 1 : 0;
      PlayerInventory var2 = var0.method_31548();

      for (int var3 = 0; var3 < 36; var3++) {
         if (var2.method_5438(var3).method_31574(Items.field_8288)) {
            var1 += var2.method_5438(var3).method_7947();
         }
      }

      return var1;
   }

   private static float worstThreat(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2) {
      List var3 = enemies(var0, var1, 16.0);
      float var4 = 0.0F;

      for (EndCrystalEntity var6 : var0.method_18467(EndCrystalEntity.class, var1.method_5829().method_1014(12.0))) {
         Vec3d var7 = var6.method_73189();
         float var8 = CrystalDamage.estimate(var7, var1);
         if (var8 > var4 && worthIt(var3, var6.method_5829(), var7, var8, 4.0, 6.0F, null)) {
            var4 = var8;
         }
      }

      BlockPos var12 = var1.method_24515();

      for (BlockPos var14 : BlockPos.method_10097(var12.method_10069(-5, -2, -5), var12.method_10069(5, 3, 5))) {
         BlockState var11 = var0.method_8320(var14);
         Vec3d var9;
         float var10;
         BlockPos var15;
         if (var11.method_27852(Blocks.field_23152)
            && (Integer)var11.method_11654(RespawnAnchorBlock.field_23153) > 0
            && !var14.equals(var2)
            && (var10 = CrystalDamage.estimate(var9 = Vec3d.method_24953(var15 = var14.method_10062()), var1, 5.0F, var15)) > var4
            && worthIt(var3, new Box(var15), var9, var10, 5.5, 5.0F, var15)) {
            var4 = var10;
         }
      }

      return var4;
   }

   private static boolean worthIt(List<PlayerEntity> var0, Box var1, Vec3d var2, float var3, double var4, float var6, BlockPos var7) {
      for (PlayerEntity var9 : var0) {
         Vec3d var10 = var9.method_33571();
         if (!(var10.method_1025(TargetingUtils.closestPoint(var10, var1)) > var4 * var4) && CrystalDamage.estimate(var2, var9, var6, var7) < var3) {
            return true;
         }
      }

      return false;
   }

   private static int enemiesWithin(ClientWorld var0, ClientPlayerEntity var1, double var2) {
      int var4 = 0;

      for (PlayerEntity var6 : var0.method_18456()) {
         if (var6 != var1 && var6.method_5805() && MeleeAttacks.isEnemy(var1, var6) && var6.method_5739(var1) <= var2) {
            var4++;
         }
      }

      return var4;
   }

   private static List<PlayerEntity> enemies(ClientWorld var0, ClientPlayerEntity var1, double var2) {
      ArrayList var4 = new ArrayList();

      for (PlayerEntity var6 : var0.method_18456()) {
         if (var6 != var1 && var6.method_5805() && MeleeAttacks.isEnemy(var1, var6) && var6.method_5739(var1) <= var2) {
            var4.add(var6);
         }
      }

      return var4;
   }

   private void runAuras(ClientWorld var1) {
      CrystalAuraModule var3 = ModuleRegistry.get(CrystalAuraModule.class);
      if (var3 != null && !var3.isEnabled()) {
         if (this.crystalWasOn == null) {
            this.crystalWasOn = false;
         }

         var3.setEnabled(true);
      }

      AnchorAuraModule var4 = ModuleRegistry.get(AnchorAuraModule.class);
      boolean var2 = this.anchors.get() && var1.method_27983() != World.field_25180;
      if (var4 != null && var2 && !var4.isEnabled()) {
         if (this.anchorWasOn == null) {
            this.anchorWasOn = false;
         }

         var4.setEnabled(true);
      }
   }

   private void keepTotem() {
      AutoInvTotemModule var1 = ModuleRegistry.get(AutoInvTotemModule.class);
      AutoTotemModule var2 = ModuleRegistry.get(AutoTotemModule.class);
      if (var1 != null && !var1.isEnabled() && (var2 == null || !var2.isEnabled())) {
         if (this.totemWasOn == null) {
            this.totemWasOn = false;
         }

         var1.setEnabled(true);
      }
   }

   private void restoreAuras() {
      CrystalAuraModule var3 = ModuleRegistry.get(CrystalAuraModule.class);
      if (var3 != null && Boolean.FALSE.equals(this.crystalWasOn)) {
         var3.setEnabled(false);
      }

      AnchorAuraModule var2;
      if ((var2 = ModuleRegistry.get(AnchorAuraModule.class)) != null && Boolean.FALSE.equals(this.anchorWasOn)) {
         var2.setEnabled(false);
      }

      AutoInvTotemModule var1;
      if ((var1 = ModuleRegistry.get(AutoInvTotemModule.class)) != null && Boolean.FALSE.equals(this.totemWasOn)) {
         var1.setEnabled(false);
      }

      this.totemWasOn = null;
      this.anchorWasOn = null;
      this.crystalWasOn = null;
   }

   private static boolean blastNear(ClientWorld var0, ClientPlayerEntity var1) {
      for (EndCrystalEntity var3 : var0.method_18467(EndCrystalEntity.class, var1.method_5829().method_1014(7.0))) {
         if (CrystalDamage.estimate(var3.method_73189(), var1) >= 1.0F) {
            return true;
         }
      }

      BlockPos var7 = var1.method_24515();
      int var8 = (int)Math.ceil(7.0);

      for (BlockPos var5 : BlockPos.method_10097(var7.method_10069(-var8, -2, -var8), var7.method_10069(var8, 3, var8))) {
         BlockState var6 = var0.method_8320(var5);
         if (var6.method_27852(Blocks.field_23152)
            && (Integer)var6.method_11654(RespawnAnchorBlock.field_23153) > 0
            && Vec3d.method_24953(var5).method_1022(var1.method_73189()) <= 7.0) {
            return true;
         }
      }

      return false;
   }

   private static void handBack(ClientPlayerEntity var0) {
      ItemStack var1 = var0.method_6047();
      if (!var1.method_31574(Items.field_8301)
         && !var1.method_31574(Items.field_8281)
         && !var1.method_31574(Items.field_23141)
         && !var1.method_31574(Items.field_8801)
         && !Crystals.isCombatItem(var1)) {
         int var2 = BotBody.hotbarSlot(var0, var0x -> var0x.method_31574(Items.field_8301));
         if (var2 < 0) {
            var2 = BotBody.hotbarSlot(var0, MeleeAttacks::isMeleeWeapon);
         }

         if (var2 >= 0) {
            var0.method_31548().method_61496(var2);
         }
      }
   }

   private boolean hands() {
      CombatMacroLock.take(this);
      return !MeleeAttacks.actedThisTick((PlayerEntity)this.mc.field_1724);
   }

   private void watch(ClientWorld var1, PlayerEntity var2, RivalBook.Rival var3) {
      float var4 = CrystalDamage.health(var2);
      if (var4 < this.lastTheirHealth - 0.5F) {
         this.hurtAt = this.tick;
      }

      if (var4 < this.lastTheirHealth - 0.5F || !Combo.inHole(var2)) {
         this.progressAt = this.tick;
      }

      this.lastTheirHealth = var4;
      if (this.openerArm >= 0 && this.tick - this.openerAt >= 40) {
         float var5 = this.openerHealth - var4 + 10.0F * this.openerPops;
         if (var3 != null) {
            var3.opened(this.openerArm, this.openerReach, MathHelper.method_15363(var5 / 12.0F, 0.0F, 1.5F));
         }

         this.openerArm = -1;
      }

      if (this.launchedAt >= 0) {
         this.launchPeak = Math.max(this.launchPeak, var2.method_23318() - this.launchY);
         if (this.tick - this.launchedAt >= 8) {
            if (var3 != null) {
               var3.launched(this.launchPeak >= 0.3);
            }

            this.launchedAt = -1;
         }
      }

      if (this.theirPopAt >= 0 && var3 != null) {
         if (var2.method_6079().method_31574(Items.field_8288)) {
            var3.retotemed(this.tick - this.theirPopAt);
            this.theirPopAt = -1;
         } else if (this.tick - this.theirPopAt > 100) {
            var3.retotemed(100);
            this.theirPopAt = -1;
         }
      } else if (this.theirPopAt >= 0 && this.tick - this.theirPopAt > 100) {
         this.theirPopAt = -1;
      }

      if (Combo.inHole(var2)) {
         if (this.holeSince < 0) {
            this.holeSince = this.tick;
         }
      } else if (this.holeSince >= 0) {
         if (var3 != null) {
            var3.leftHole(Math.min(600, this.tick - this.holeSince));
         }

         this.holeSince = -1;
      }

      if (!this.lowSpell && var4 <= 8.0F) {
         this.lowSpell = true;
         this.pearledInSpell = false;
      } else if (this.lowSpell && var4 > 12.0F) {
         if (var3 != null) {
            var3.lowSpell(this.pearledInSpell);
         }

         this.lowSpell = false;
      }

      for (EnderPearlEntity var6 : var1.method_18467(EnderPearlEntity.class, var2.method_5829().method_1014(4.0))) {
         if (var6.method_24921() == var2 && var6.field_6012 <= 2 && var6.method_5628() != this.followedPearl) {
            if (this.lowSpell) {
               this.pearledInSpell = true;
            }

            this.theirPearl = var6.method_5628();
         }
      }
   }

   private boolean getAway(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3) {
      if (var2.method_5739(var3) < 12.0 && this.pearlAway(var1, var2, var3)) {
         return true;
      } else if (var2.method_5739(var3) >= 16.0 && var2.method_6032() <= this.eatAt() + 4.0F && this.hands() && this.body.use(this.mc, var2, GAPPLE)) {
         this.eating = true;
         return true;
      } else {
         this.body.flee(this.mc, var2, var3);
         return true;
      }
   }

   private boolean pearlAway(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3) {
      if (this.escape.get() && this.pearlReady(var2) && this.tick >= this.searchAfter) {
         List var4 = enemies(var1, var2, 48.0);
         Rotation var5 = PearlFlight.escape(var2, var4, 20.0);
         this.throwAway = 16.0;
         if (var5 == null) {
            var5 = PearlFlight.escape(var2, var4, 12.0);
            this.throwAway = 12.0;
         }

         if (var5 == null) {
            this.searchAfter = this.tick + 10;
            return false;
         } else {
            this.startThrow(CrystalBotModule.Throw.ESCAPE, var5, null);
            return this.keepThrowing(var2, var3);
         }
      } else {
         return false;
      }
   }

   private boolean pearlReady(ClientPlayerEntity var1) {
      return this.tick - this.pearlAt >= 30
         && BotBody.hotbarSlot(var1, PEARL) >= 0
         && !var1.method_7357().method_7904(new ItemStack(Items.field_8634))
         && (var1.method_6032() + var1.method_6067() > 6.0F || var1.method_6079().method_31574(Items.field_8288));
   }

   private void startThrow(CrystalBotModule.Throw var1, Rotation var2, Vec3d var3) {
      this.throwing = var1;
      this.throwLook = var2;
      this.throwSpot = var3;
      this.throwUntil = this.tick + 14;
   }

   private boolean keepThrowing(ClientPlayerEntity var1, PlayerEntity var2) {
      if (this.tick > this.throwUntil || !this.pearlReady(var1)) {
         this.throwing = null;
         this.pearlAt = this.tick - 15;
         return false;
      } else if (!this.hands()) {
         return true;
      } else {
         Aim.turn(this.mc, this.throwLook.yaw(), this.throwLook.pitch(), 27.0F);
         int var4 = BotBody.hotbarSlot(var1, PEARL);
         if (var1.method_31548().method_67532() != var4) {
            var1.method_31548().method_61496(var4);
            return true;
         } else if (!ServerRotation.desynced() && !var1.method_6115() && MeleeAttacks.handsFree(this.mc, var1)) {
            if (var1.method_24828() && !(var1.method_60478().method_37268() > 1.0E-4)) {
               float var5 = var1.method_36454();
               float var6 = var1.method_36455();
               Rotation var7 = ServerRotation.commit(var1);
               if (!(Math.abs(MathHelper.method_15393(var7.yaw() - var5)) > 0.01F) && !(Math.abs(var7.pitch() - var6) > 0.01F)) {
                  boolean var3 = this.throwing == CrystalBotModule.Throw.ESCAPE
                     ? PearlFlight.escapes(var1, var5, var6, enemies(this.mc.field_1687, var1, 48.0), this.throwAway)
                     : PearlFlight.reaches(var1, var5, var6, this.throwSpot, 0.0, 3.0);
                  if (!var3) {
                     return true;
                  } else {
                     if (this.mc.field_1761.method_2919(var1, Hand.field_5808).method_23665()) {
                        var1.method_6104(Hand.field_5808);
                        MeleeAttacks.claimTick(this.mc);
                        this.pearlAt = this.tick;
                        if (this.throwing == CrystalBotModule.Throw.ESCAPE) {
                           ModuleChat.send(this, ModuleChat.body().warn("Pearled away").text("."));
                        } else {
                           ModuleChat.send(this, ModuleChat.body().text("Pearled after ").name(var2.method_5477().getString()).text("."));
                        }
                     }

                     this.throwing = null;
                     return true;
                  }
               } else {
                  return true;
               }
            } else {
               return true;
            }
         } else {
            return true;
         }
      }
   }

   private boolean chasePearl(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3, RivalBook.Rival var4) {
      if ((this.stance == CrystalBotModule.Stance.PRESS || this.stance == CrystalBotModule.Stance.HOLD) && this.pearlReady(var2)) {
         float var5 = var2.method_6032() + var2.method_6067();
         if (var5 < 6.0F) {
            return false;
         } else {
            int var6 = count(var2, PEARL);
            boolean var7 = this.theirPearl >= 0;
            boolean var8 = totems(var2) == 0 && var2.method_6032() <= 12.0F
               || !var7 && var4 != null && var4.pearlsWhenLow() && CrystalDamage.health(var3) <= 8.0F;
            if (var6 <= (var8 ? 1 : 0)) {
               return false;
            } else {
               Vec3d var9 = null;
               if (var7) {
                  this.followedPearl = this.theirPearl;
                  this.theirPearl = -1;
                  Entity var12 = var1.method_8469(this.followedPearl);
                  EnderPearlEntity var10;
                  PearlFlight.Landing var11;
                  if (var12 instanceof EnderPearlEntity
                     && (var11 = PearlFlight.fly(var1, var10 = (EnderPearlEntity)var12, var10.method_73189(), var10.method_18798())) != null
                     && var11.at().method_1022(var2.method_73189()) > 8.0) {
                     var9 = var11.at();
                  }
               }

               double var22 = var2.method_5739(var3);
               if (var9 == null && var22 <= this.range.get() + 8.0) {
                  Vec3d var14 = var3.method_73189().method_1020(var2.method_73189()).method_18805(1.0, 0.0, 1.0).method_1029();
                  Vec3d var15 = var3.method_73189().method_1023(var3.field_6014, var3.field_6036, var3.field_5969);
                  boolean var16 = var22 >= 12.0 && var15.field_1352 * var14.field_1352 + var15.field_1350 * var14.field_1350 > 0.12;
                  BlockPos var17 = var3.method_24515().method_10074();
                  boolean var18 = !var1.method_8320(var17).method_26220(var1, var17).method_1110();
                  boolean var19 = var22 >= 3.0 && var3.method_23318() - var2.method_23318() >= 3.0 && var18;
                  boolean var20 = var22 >= 4.0 && var2.method_23318() - var3.method_23318() >= 4.0 && var18;
                  boolean var13 = var22 > 6.0 && this.tick - this.closerAt > 60;
                  if (var16 || var19 || var20 || var13) {
                     var9 = var3.method_73189().method_1019(var15.method_18805(1.0, 0.0, 1.0).method_1021(PearlFlight.flightTicks(var22)));
                  }

                  AirTrack.Landing var23;
                  if (var9 == null
                     && !var3.method_24828()
                     && var22 >= 6.0
                     && (var23 = this.air.landing(this.tick)) != null
                     && var3.method_23318() - var23.at().field_1351 >= 3.0) {
                     var9 = var23.at();
                  }
               }

               if (var9 != null && this.tick >= this.searchAfter) {
                  Rotation var24 = PearlFlight.toward(var2, var9, 0.8, 3.0);
                  if (var24 == null) {
                     this.searchAfter = this.tick + 10;
                     return false;
                  } else {
                     this.startThrow(CrystalBotModule.Throw.CHASE, var24, var9);
                     return this.keepThrowing(var2, var3);
                  }
               } else {
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   private boolean getDown(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3) {
      double var5 = var2.method_23318() - var3.method_23318();
      double var7 = Math.hypot(var3.method_23317() - var2.method_23317(), var3.method_23321() - var2.method_23321());
      if (!(var5 < 3.0) && !(var7 > 8.0) && this.stance != CrystalBotModule.Stance.RUN && !this.critical(var2)) {
         if (this.belowSince < 0) {
            this.belowSince = this.tick;
         }

         if (this.tick - this.belowSince >= 20 && var2.method_24828() && var3.method_24828()) {
            if (this.digAnchor != null && !var1.method_8320(this.digAnchor).method_27852(Blocks.field_23152)) {
               if (this.digPlaced) {
                  this.digBlasts++;
                  this.digWalkUntil = this.tick + 40;
               }

               this.digAnchor = null;
            }

            if (this.tick >= this.digWalkUntil && !openDrop(var1, var2, var3)) {
               boolean var4 = var5 <= 6.0
                  && this.digBlasts < 2
                  && this.tick >= this.digAfter
                  && this.anchors.get()
                  && var1.method_27983() != World.field_25180
                  && BotBody.hotbarSlot(var2, ANCHOR) >= 0
                  && BotBody.hotbarSlot(var2, GLOWSTONE) >= 0;
               return var4 && this.blastDown(var1, var2, var3) ? true : this.pearlDown(var2, var3);
            } else {
               this.body.face(this.mc, var2, var3, 1.0F);
               this.body.dropToward(this.mc, var2, var3.method_73189());
               return true;
            }
         } else {
            return false;
         }
      } else {
         this.forgetDig();
         return false;
      }
   }

   private boolean blastDown(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3) {
      if (this.digAnchor == null) {
         this.digAnchor = digCell(var1, var2, var3);
         if (this.digAnchor == null) {
            this.digAfter = this.tick + 100;
            return false;
         }

         this.digPlaced = false;
         this.digGuards = 0;
         this.digUntil = this.tick + 80;
         if (!this.digAnnounced) {
            ModuleChat.send(this, ModuleChat.body().text("Blasting down to ").name(var3.method_5477().getString()).text("."));
         }

         this.digAnnounced = true;
      }

      if (this.tick > this.digUntil) {
         this.digBlasts = 2;
         this.digAnchor = null;
         return false;
      } else {
         this.body.face(this.mc, var2, var3, 1.0F);
         BlockState var5 = var1.method_8320(this.digAnchor);
         if (!var5.method_27852(Blocks.field_23152)) {
            AnchorGuard.Spot var10 = var5.method_45474() ? AnchorGuard.supportFor(var1, var2, var2.method_33571(), this.digAnchor) : null;
            if (var10 == null) {
               this.digAnchor = null;
               this.digAfter = this.tick + 100;
               return false;
            } else {
               if (this.placeAt(var1, var2, var10, BotBody.hotbarSlot(var2, ANCHOR)) == 1) {
                  this.digPlaced = true;
               }

               return true;
            }
         } else if ((Integer)var5.method_11654(RespawnAnchorBlock.field_23153) <= 0) {
            return this.clickDigAnchor(var1, var2, BotBody.hotbarSlot(var2, GLOWSTONE));
         } else {
            Vec3d var6 = AnchorGuard.anchorAim(var1, var2, this.digAnchor);
            if (var6 == null) {
               this.digAnchor = null;
               return false;
            } else {
               int var7 = guardBlockSlot(var2);
               AnchorGuard.Spot var4;
               if (this.digGuards < 2 && var7 >= 0 && (var4 = AnchorGuard.find(var1, var2, this.digAnchor, var6)) != null && !var4.standing()) {
                  if (this.placeAt(var1, var2, var4, var7) == 1) {
                     this.digGuards++;
                  }

                  return true;
               } else {
                  float var8 = CrystalDamage.estimate(Vec3d.method_24953((Vec3i)this.digAnchor), var2, 5.0F, this.digAnchor);
                  int var9 = AnchorGuard.detonatorRoute(var2.method_31548(), var2.method_6047());
                  if (!(var8 > Math.min(this.selfCapNow, 10.0F)) && !CrystalDamage.endangers(var2, var8) && var9 != -1) {
                     return this.clickDigAnchor(var1, var2, var9 == -2 ? var2.method_31548().method_67532() : var9);
                  } else {
                     this.digBlasts = 2;
                     this.digAnchor = null;
                     return false;
                  }
               }
            }
         }
      }
   }

   private static BlockPos digCell(ClientWorld var0, ClientPlayerEntity var1, PlayerEntity var2) {
      Vec3d var3 = var1.method_33571();
      double var4 = var1.method_55754() - 0.5;
      BlockPos var6 = var1.method_24515();
      ArrayList var7 = new ArrayList();

      for (BlockPos var9 : BlockPos.method_10097(var6.method_10069(-4, -2, -4), var6.method_10069(4, 0, 4))) {
         if (!(var3.method_1022(Vec3d.method_24953(var9)) > var4)) {
            BlockPos var11 = var9.method_10074();
            Box var10;
            if (var0.method_8320(var9).method_45474()
               && var0.method_8316(var9).method_15769()
               && var0.method_8320(var11).method_26206(var0, var11, Direction.field_11036)
               && !(var10 = new Box(var9)).method_994(var1.method_5829())
               && !var10.method_994(var2.method_5829())) {
               var7.add(var9.method_10062());
            }
         }
      }

      var7.sort(Comparator.comparingDouble(var1x -> Vec3d.method_24953(var1x).method_1025(var2.method_73189())));
      BlockPos var12 = null;

      for (BlockPos var14 : var7.subList(0, Math.min(var7.size(), 8))) {
         if (AnchorGuard.supportFor(var0, var1, var3, var14) != null) {
            if (var12 == null) {
               var12 = var14;
            }

            if (AnchorGuard.find(var0, var1, var14, TargetingUtils.closestPoint(var3, new Box(var14).method_1011(0.1))) != null) {
               return var14;
            }
         }
      }

      return var12;
   }

   private static boolean openDrop(ClientWorld var0, ClientPlayerEntity var1, PlayerEntity var2) {
      Vec3d var3 = var2.method_73189().method_1020(var1.method_73189()).method_18805(1.0, 0.0, 1.0);
      if (var3.method_1027() < 0.25) {
         return false;
      } else {
         Vec3d var4 = var1.method_73189().method_1019(var3.method_1029().method_1021(0.8));
         BlockPos var5 = BlockPos.method_49637(var4.field_1352, var1.method_23318() + 0.1, var4.field_1350);
         int var6 = MathHelper.method_15357(var2.method_23318() + 0.01);

         for (BlockPos var7 = var5; var7.method_10264() >= var6 - 1; var7 = var7.method_10074()) {
            BlockState var8 = var0.method_8320(var7);
            if (var8.method_27852(Blocks.field_10164) || var8.method_26164(BlockTags.field_21952)) {
               return false;
            }

            if (!var8.method_26220(var0, var7).method_1110()) {
               return var7.method_10264() < var1.method_23318() - 1.5 && var7.method_10264() <= var6 + 1;
            }
         }

         return false;
      }
   }

   private boolean clickDigAnchor(ClientWorld var1, ClientPlayerEntity var2, int var3) {
      if (var3 < 0) {
         this.digAnchor = null;
         return false;
      } else if (!this.hands()) {
         return true;
      } else {
         BlockHitResult var4 = AnchorGuard.anchorHit(var1, var2, this.digAnchor);
         if (var4 == null) {
            Vec3d var5 = AnchorGuard.anchorAim(var1, var2, this.digAnchor);
            if (var5 == null) {
               this.digAnchor = null;
               return false;
            } else {
               ServerRotation.request(RotationUtils.fromTo(var2.method_33571(), var5), 6.0F);
               return true;
            }
         } else if (var2.method_31548().method_67532() != var3) {
            var2.method_31548().method_61496(var3);
            return true;
         } else {
            if (MeleeAttacks.handsFree(this.mc, var2)) {
               AnchorGuard.click(this.mc, var2, var4);
            }

            return true;
         }
      }
   }

   private boolean pearlDown(ClientPlayerEntity var1, PlayerEntity var2) {
      if (this.chase.get() && this.pearlReady(var1) && this.tick >= this.searchAfter) {
         Vec3d var3 = var2.method_73189();
         Rotation var4 = PearlFlight.toward(var1, var3, 0.8, 3.0);
         if (var4 == null) {
            this.searchAfter = this.tick + 10;
            return false;
         } else {
            this.startThrow(CrystalBotModule.Throw.CHASE, var4, var3);
            return this.keepThrowing(var1, var2);
         }
      } else {
         return false;
      }
   }

   private static int feetY(ClientPlayerEntity var0) {
      return MathHelper.method_15357(var0.method_23318() + 0.01);
   }

   private boolean wantsLower(ClientPlayerEntity var1, PlayerEntity var2, double var3) {
      return this.stance != CrystalBotModule.Stance.RUN
         && var1.method_24828()
         && var2.method_24828()
         && !Combo.inHole(var2)
         && var3 <= 5.5
         && feetY(var1) == MathHelper.method_15357(var2.method_23318() + 0.01)
         && !this.justLaunched();
   }

   private boolean justLaunched() {
      return this.launching || this.launchedAt >= 0 || this.tick <= this.followUntil + 20;
   }

   private boolean stayLow(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3, double var4) {
      if (this.stance != CrystalBotModule.Stance.RUN && var2.method_24828() && var3.method_24828() && !Combo.inHole(var3) && !(var4 > 5.5)) {
         if (feetY(var2) < MathHelper.method_15357(var3.method_23318() + 0.01)) {
            return true;
         } else if (!this.wantsLower(var2, var3, var4)) {
            return false;
         } else {
            BlockPos var6 = lowerStep(var1, var2, var3, feetY(var2) - 1);
            if (var6 == null) {
               return false;
            } else {
               this.body.toward(this.mc, var2, Vec3d.method_24955(var6));
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private boolean digDown(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3, double var4) {
      if (this.wantsLower(var2, var3, var4) && this.tick >= this.stepDigAfter) {
         if (lowerStep(var1, var2, var3, feetY(var2) - 1) != null) {
            return false;
         } else {
            BlockPos var7 = var2.method_24515().method_10074();
            BlockState var8 = var1.method_8320(var7);
            if (BotBody.hotbarSlot(var2, OBSIDIAN) < 0 && !Crystals.isBase(var8)) {
               return false;
            } else {
               BlockPos var9 = var7.method_10074();
               boolean var6 = !var8.method_31709()
                  && digTicks(var1, var2, var7) <= 50
                  && !var1.method_8320(var9).method_26220(var1, var9).method_1110()
                  && !nearLava(var1, var7);
               if (var6 && this.mine(var1, var2, var7)) {
                  return true;
               } else {
                  this.stepDigAfter = this.tick + 100;
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   private boolean digBase(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3, double var4) {
      int var6 = MathHelper.method_15357(var3.method_23318() + 0.01);
      if (this.stance != CrystalBotModule.Stance.RUN
         && var2.method_24828()
         && var3.method_24828()
         && !Combo.inHole(var3)
         && !(var4 > 5.5)
         && feetY(var2) == var6 - 1
         && this.tick >= this.baseDigAfter
         && BotBody.hotbarSlot(var2, OBSIDIAN) >= 0
         && !this.justLaunched()) {
         Vec3d var7 = var2.method_33571();
         BlockPos var8 = var3.method_24515();
         BlockPos var9 = null;
         double var10 = 1.7976931348623157E308;

         for (BlockPos var13 : BlockPos.method_10097(var8.method_10069(-2, -1, -2), var8.method_10069(2, -1, 2))) {
            BlockState var17 = var1.method_8320(var13);
            if (Crystals.isBase(var17) && Crystals.canHost(var1, var13, null)) {
               return false;
            }

            Vec3d var14;
            double var15;
            if ((var13.method_10263() != var8.method_10263() || var13.method_10260() != var8.method_10260())
               && Math.max(Math.abs(var13.method_10263() - var2.method_31477()), Math.abs(var13.method_10260() - var2.method_31479())) >= 2
               && !var17.method_45474()
               && !var17.method_31709()
               && var1.method_22347(var13.method_10084())
               && !((var15 = (var14 = Vec3d.method_24953(var13).method_1031(0.0, 0.5, 0.0)).method_1022(var3.method_73189())) > 2.5)
               && !(var7.method_1022(var14) > var2.method_55754() - 0.5)
               && digTicks(var1, var2, var13) <= 25
               && !(var15 >= var10)) {
               var9 = var13.method_10062();
               var10 = var15;
            }
         }

         if (var9 != null && this.mine(var1, var2, var9)) {
            return true;
         } else {
            this.baseDigAfter = this.tick + 100;
            return false;
         }
      } else {
         return false;
      }
   }

   private static BlockPos lowerStep(ClientWorld var0, ClientPlayerEntity var1, PlayerEntity var2, int var3) {
      BlockPos var4 = var1.method_24515();
      BlockPos var5 = null;
      double var6 = 1.7976931348623157E308;

      for (BlockPos var9 : BlockPos.method_10097(var4.method_10069(-2, -1, -2), var4.method_10069(2, -1, 2))) {
         BlockPos var12 = StandPicker.standable(var0, var9);
         if (var12 != null && var12.method_10264() == var3 && var12.equals(var9)) {
            Vec3d var13 = Vec3d.method_24955(var12);
            double var14 = Math.hypot(var13.field_1352 - var2.method_23317(), var13.field_1350 - var2.method_23321());
            double var10;
            if (!(var14 < 2.4) && !(var14 > 4.5) && walkable(var0, var1.method_73189(), var13) && (var10 = var12.method_10262(var4)) < var6) {
               var5 = var12.method_10062();
               var6 = var10;
            }
         }
      }

      return var5;
   }

   private static int digTicks(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2) {
      BlockState var5 = var0.method_8320(var2);
      float var6 = var5.method_26214(var0, var2);
      if (var6 < 0.0F) {
         return 2147483647;
      } else if (var6 == 0.0F) {
         return 1;
      } else {
         int var7 = toolSlot(var1.method_31548(), var5);
         ItemStack var8 = var7 < 0 ? ItemStack.field_8037 : var1.method_31548().method_5438(var7);
         float var9 = var8.method_7924(var5);
         int var4;
         if (var9 > 1.0F
            && (
                  var4 = var1.method_56673()
                     .method_30530(RegistryKeys.field_41265)
                     .method_46746(Enchantments.field_9131)
                     .map(var1x -> EnchantmentHelper.method_8225(var1x, var8))
                     .orElse(0)
               )
               > 0) {
            var9 += var4 * var4 + 1;
         }

         boolean var10 = var8.method_7951(var5) || !var5.method_29291();
         return MathHelper.method_15386(var6 * (var10 ? 30.0F : 100.0F) / var9);
      }
   }

   private static boolean nearLava(ClientWorld var0, BlockPos var1) {
      for (Direction var5 : Direction.values()) {
         if (var0.method_8316(var1.method_10093(var5)).method_15767(FluidTags.field_15518)) {
            return true;
         }
      }

      return false;
   }

   private void forgetDig() {
      this.digAnchor = null;
      this.digAnnounced = false;
      this.digPlaced = false;
      this.digBlasts = 0;
      this.digGuards = 0;
      this.digAfter = 0;
      this.digWalkUntil = 0;
      this.digUntil = 0;
      this.belowSince = -1;
   }

   private boolean startLaunch(ClientPlayerEntity var1, PlayerEntity var2, RivalBook.Rival var3) {
      if (var1.field_6012 - this.diggingAt <= 1) {
         return false;
      } else if (this.tick >= this.nextLaunch
         && !this.pause.slowServer()
         && (this.stance == CrystalBotModule.Stance.PRESS || this.stance == CrystalBotModule.Stance.HOLD)
         && (var3 == null || !var3.shrugsOffLaunches())) {
         if (var2.method_24828() && !Combo.inHole(var2) && (!var2.method_6039() || axeInHotbar(var1))) {
            if (!this.mc.field_1687.method_18467(EndCrystalEntity.class, var2.method_5829().method_1014(3.0)).isEmpty()) {
               return false;
            } else {
               double var5 = reach(var1, var2);
               if (weaponSlot(var1, var2.method_6039()) >= 0 && !(var5 > 3.9)) {
                  boolean var4 = MeleeAttacks.isMeleeWeapon(var1.method_6047()) && MeleeAttacks.charge(var1) >= 0.5F;
                  if (!var4) {
                     int var8 = var3 != null ? var3.opener(var5) : (this.tick - this.hurtAt >= 20 ? 0 : 1);
                     if (this.openerArm < 0) {
                        this.judgeOpener(var8, var5, var2);
                     }

                     if (var8 == 1) {
                        this.nextLaunch = this.tick + 40;
                        return false;
                     }
                  }

                  this.launching = true;
                  this.launchUntil = this.tick + 16;
                  return this.launch(var1, var2);
               } else {
                  return false;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void judgeOpener(int var1, double var2, PlayerEntity var4) {
      this.openerArm = var1;
      this.openerReach = var2;
      this.openerAt = this.tick;
      this.openerHealth = CrystalDamage.health(var4);
      this.openerPops = 0;
   }

   private boolean followUp(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3) {
      AirTrack.Landing var5 = this.air.landing(this.tick);
      Vec3d var4 = var5 != null ? var5.at() : var3.method_73189();
      if (this.anchorUnder(var1, var2, var3, var4)) {
         return true;
      } else {
         int var7 = BotBody.hotbarSlot(var2, OBSIDIAN);
         if (var7 < 0) {
            this.followLaid = true;
            return false;
         } else {
            BlockPos var8 = BlockPos.method_49637(var4.field_1352, var4.field_1351 + 0.2, var4.field_1350);
            CrystalDamage.Victim var9 = new CrystalDamage.Victim(var3, var4.method_1020(var3.method_73189()));
            CrystalDamage.Victim var10 = new CrystalDamage.Victim(var2, Vec3d.field_1353);
            Vec3d var11 = var2.method_33571();
            AnchorGuard.Spot var12 = null;
            float var13 = 4.0F;

            for (BlockPos var15 : BlockPos.method_10097(var8.method_10069(-1, -1, -1), var8.method_10069(1, 0, 1))) {
               if (var15.method_10263() != var8.method_10263() || var15.method_10260() != var8.method_10260()) {
                  BlockState var18 = var1.method_8320(var15);
                  Vec3d var19 = Crystals.explosionAt(var15);
                  if (Crystals.isBase(var18) && Crystals.canHost(var1, var15, null)) {
                     if (var9.damage(var19, false) >= var13) {
                        this.followLaid = true;
                        return false;
                     }
                  } else {
                     BlockPos var17;
                     if (var18.method_45474()
                        && var1.method_22347(var15.method_10084())
                        && var1.method_22347(var15.method_10086(2))
                        && var1.method_8320(var17 = var15.method_10074()).method_26206(var1, var17, Direction.field_11036)
                        && var1.method_8628(Blocks.field_10540.method_9564(), var15, ShapeContext.method_16194())
                        && !(var11.method_1022(Vec3d.method_24953(var15)) > var2.method_55754())) {
                        float var20 = var9.damage(var19, true);
                        float var21 = var10.damage(var19, true);
                        AnchorGuard.Spot var16;
                        if (!(var20 <= var13)
                           && !(var21 > this.selfCapNow)
                           && !CrystalDamage.endangers(var2, var21)
                           && (var16 = AnchorGuard.supportFor(var1, var2, var11, var15.method_10062())) != null) {
                           var12 = var16;
                           var13 = var20;
                        }
                     }
                  }
               }
            }

            this.body.face(this.mc, var2, var3, 1.0F);
            if (var12 == null) {
               if (var11.method_1022(var4) > 3.5) {
                  this.body.drive(this.mc, var2, 1, 0.0, true, false);
               }

               return true;
            } else {
               if (this.placeAt(var1, var2, var12, var7) == 1) {
                  this.followLaid = true;
               }

               return true;
            }
         }
      }
   }

   private boolean anchorUnder(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3, Vec3d var4) {
      if (this.anchors.get() && var1.method_27983() != World.field_25180 && !var3.method_24828()) {
         int var5 = BotBody.hotbarSlot(var2, var0 -> var0.method_31574(Items.field_23141));
         if (var5 >= 0 && BotBody.hotbarSlot(var2, var0 -> var0.method_31574(Items.field_8801)) >= 0) {
            BlockPos var6 = BlockPos.method_49637(var4.field_1352, var4.field_1351 + 0.2, var4.field_1350);
            BlockPos var7 = var6.method_10074();
            if (var1.method_8320(var6).method_45474() && var1.method_8320(var7).method_26206(var1, var7, Direction.field_11036)) {
               if (!var3.method_5829().method_994(new Box(var6)) && !var2.method_5829().method_994(new Box(var6))) {
                  Vec3d var8 = var2.method_33571();
                  if (var8.method_1022(Vec3d.method_24953(var6)) > var2.method_55754()) {
                     return false;
                  } else {
                     Vec3d var9 = Vec3d.method_24953(var6);
                     float var10 = CrystalDamage.estimate(var9, var2, 5.0F, var6);
                     Vec3d var11 = new Vec3d(var4.field_1352, var6.method_10264() + 1.0, var4.field_1350);
                     float var12 = new CrystalDamage.Victim(var3, var11.method_1020(var3.method_73189())).damage(var9, 5.0F, var6);
                     if (!(var12 < 6.0F) && !(var10 > Math.min(this.selfCapNow, 11.0F)) && !CrystalDamage.endangers(var2, var10)) {
                        AnchorGuard.Spot var13 = AnchorGuard.supportFor(var1, var2, var8, var6);
                        if (var13 == null) {
                           return false;
                        } else {
                           this.body.face(this.mc, var2, var3, 1.0F);
                           if (this.placeAt(var1, var2, var13, var5) == 1) {
                              this.followLaid = true;
                           }

                           return true;
                        }
                     } else {
                        return false;
                     }
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean launch(ClientPlayerEntity var1, PlayerEntity var2) {
      if (this.tick > this.launchUntil
         || this.stance == CrystalBotModule.Stance.RUN
         || this.stance == CrystalBotModule.Stance.GUARD
         || reach(var1, var2) > 4.1
         || Combo.inHole(var2)) {
         this.launching = false;
         this.nextLaunch = this.tick + 40;
         return false;
      } else if (!this.hands()) {
         return true;
      } else {
         int var3 = weaponSlot(var1, var2.method_6039());
         if (var3 < 0) {
            this.launching = false;
            return false;
         } else {
            if (var1.method_31548().method_67532() != var3) {
               var1.method_31548().method_61496(var3);
            }

            this.body.face(this.mc, var1, var2, 1.0F);
            float var4 = MeleeAttacks.charge(var1);
            double var5 = reach(var1, var2);
            Rotation var7 = ServerRotation.sent();
            boolean var8 = var1.method_5624();
            if (var4 >= 0.95F && var5 <= 2.95 && this.strike(var1, var2)) {
               this.launching = false;
               this.nextLaunch = this.tick + 80;
               this.launchedAt = this.tick;
               this.launchY = var2.method_23318();
               this.launchPeak = 0.0;
               this.followLaid = false;
               this.followUntil = this.tick + 12;
               this.air.kicked(var1, var2, var7 != null ? var7.yaw() : var1.method_36454(), var8, this.tick);
               this.body.drive(this.mc, var1, 0, 1.0, false, false);
               return true;
            } else {
               this.body.drive(this.mc, var1, var5 > 2.9 ? 1 : 0, 0.0, var4 >= 0.85F, false);
               return true;
            }
         }
      }
   }

   private boolean strike(ClientPlayerEntity var1, PlayerEntity var2) {
      if (!this.pause.slowServer() && !MeleeAttacks.actedThisTick(var1) && this.mc.field_1771 <= 0 && MeleeAttacks.isMeleeWeapon(var1.method_6047())) {
         Rotation var4 = ServerRotation.sent();
         if (var4 == null) {
            return false;
         } else {
            Vec3d var5 = var1.method_33571();
            double var6 = Math.min(2.95, var1.method_55755());
            Box var8 = var2.method_5829();
            Vec3d var3 = var8.method_1006(var5)
               ? var5
               : (Vec3d)var8.method_992(var5, var5.method_1019(Vec3d.method_1030(var4.pitch(), var4.yaw()).method_1021(var6))).orElse(null);
            if (var3 != null && MeleeAttacks.lineIsClear(var1, var5, var3)) {
               if (!MeleeAttacks.attack(this.mc, new EntityHitResult(var2, var3))) {
                  return false;
               } else {
                  MeleeAttacks.claimTick(this.mc);
                  return true;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static double reach(ClientPlayerEntity var0, PlayerEntity var1) {
      Vec3d var2 = var0.method_33571();
      return var2.method_1022(TargetingUtils.closestPoint(var2, var1.method_5829()));
   }

   private static boolean axeInHotbar(ClientPlayerEntity var0) {
      return BotBody.hotbarSlot(var0, var0x -> var0x.method_31573(ItemTags.field_42612)) >= 0;
   }

   private static int weaponSlot(ClientPlayerEntity var0, boolean var1) {
      PlayerInventory var2 = var0.method_31548();
      int var3 = -1;
      double var4 = 0.0;

      for (int var6 = 0; var6 < 9; var6++) {
         ItemStack var7 = var2.method_5438(var6);
         if (MeleeAttacks.isMeleeWeapon(var7) && !var7.method_31574(Items.field_49814) && (!var1 || var7.method_31573(ItemTags.field_42612))) {
            double var8 = MeleeAttacks.attackDamage(var7);
            if (var3 < 0 || var8 > var4) {
               var3 = var6;
               var4 = var8;
            }
         }
      }

      return var3;
   }

   private boolean breakHole(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3, RivalBook.Rival var4) {
      AutoCityModule var7 = ModuleRegistry.get(AutoCityModule.class);
      if (var7 == null) {
         return false;
      } else if (var7.isEnabled()) {
         if (!this.cityStarted) {
            return false;
         } else {
            CombatMacroLock.release(this);
            this.body.face(this.mc, var2, var3, 1.0F);
            return true;
         }
      } else {
         if (this.cityStarted) {
            this.cityStarted = false;
            this.breachAfter = this.tick + 100;
            this.progressAt = this.tick;
            if (Combo.inHole(var3)) {
               this.cityFails++;
            }
         }

         if (this.stance != CrystalBotModule.Stance.RUN
            && this.tick >= this.breachAfter
            && Combo.inHole(var3)
            && !(var2.method_6032() + var2.method_6067() < 8.0F)) {
            int var6 = var4 != null && var4.campsInHoles() ? 15 : 40;
            if (this.tick - this.progressAt < var6) {
               return false;
            } else {
               boolean var5 = BotBody.hotbarSlot(var2, var0 -> var0.method_31573(ItemTags.field_42614)) >= 0;
               if (this.cityFails >= 2 && var5 && this.roofOff(var1, var2, var3)) {
                  return true;
               } else if (this.cityFails >= 2 || !var5) {
                  return this.camp(var1, var2, var3);
               } else if (var2.method_5739(var3) > 3.6) {
                  this.body.face(this.mc, var2, var3, 1.0F);
                  this.body.keepDistance(this.mc, var2, var3, 1.6, 2.6, 3.4, false);
                  return true;
               } else {
                  var7.setEnabled(true);
                  if (!var7.isEnabled()) {
                     return false;
                  } else {
                     this.cityStarted = true;
                     ModuleChat.send(this, ModuleChat.body().text("Breaking ").name(var3.method_5477().getString()).text("'s hole open."));
                     return true;
                  }
               }
            }
         } else {
            return false;
         }
      }
   }

   private boolean roofOff(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3) {
      if (this.anchors.get()
         && var1.method_27983() != World.field_25180
         && BotBody.hotbarSlot(var2, var0 -> var0.method_31574(Items.field_23141)) >= 0
         && BotBody.hotbarSlot(var2, var0 -> var0.method_31574(Items.field_8801)) >= 0) {
         BlockPos var4 = var3.method_24515().method_10086(2);
         BlockState var5 = var1.method_8320(var4);
         if (var5.method_45474() || var5.method_26214(var1, var4) < 0.0F) {
            return false;
         } else if (this.mine(var1, var2, var4)) {
            this.body.face(this.mc, var2, var3, 1.0F);
            return true;
         } else {
            BlockPos var6 = roofView(var1, var2, var4);
            if (var6 == null) {
               return false;
            } else {
               this.body.face(this.mc, var2, var3, 1.0F);
               this.body.toward(this.mc, var2, Vec3d.method_24955(var6));
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean walkable(ClientWorld var0, Vec3d var1, Vec3d var2) {
      Vec3d var3 = var2.method_1020(var1);
      int var4 = Math.max(1, (int)Math.ceil(var3.method_37267() / 0.5));
      int var5 = MathHelper.method_15357(var1.field_1351 + 0.01);

      for (int var6 = 1; var6 <= var4; var6++) {
         Vec3d var7 = var1.method_1019(var3.method_1021((double)var6 / var4));
         BlockPos var8 = StandPicker.standable(var0, BlockPos.method_49637(var7.field_1352, var5, var7.field_1350));
         if (var8 == null || Math.abs(var8.method_10264() - var5) > 1) {
            return false;
         }

         var5 = var8.method_10264();
      }

      return true;
   }

   private static BlockPos roofView(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2) {
      BlockPos var3 = var1.method_24515();
      BlockPos var4 = null;
      double var5 = 1.7976931348623157E308;
      double var7 = var1.method_55754() - 0.2;

      for (BlockPos var10 : BlockPos.method_10097(var3.method_10069(-4, -1, -4), var3.method_10069(4, 1, 4))) {
         BlockPos var11 = StandPicker.standable(var0, var10);
         if (var11 != null && var11.equals(var10) && walkable(var0, var1.method_73189(), Vec3d.method_24955(var11))) {
            Vec3d var12 = Vec3d.method_24955(var11).method_1031(0.0, var1.method_5751(), 0.0);
            double var13 = var11.method_10262(var3);
            if (!(var13 >= var5)) {
               for (Direction var18 : Direction.values()) {
                  Vec3d var20 = Vec3d.method_24953(var2).method_43206(var18, 0.5);
                  BlockHitResult var19;
                  if (!(var12.method_1022(var20) > var7)
                     && (var19 = var0.method_17742(
                              new RaycastContext(var12, var20.method_43206(var18, -0.05), ShapeType.field_17559, FluidHandling.field_1348, var1)
                           ))
                           .method_17783()
                        == Type.field_1332
                     && var19.method_17777().equals(var2)
                     && var19.method_17780() == var18) {
                     var4 = var11.method_10062();
                     var5 = var13;
                     break;
                  }
               }
            }
         }
      }

      return var4;
   }

   private boolean mine(ClientWorld var1, ClientPlayerEntity var2, BlockPos var3) {
      BlockState var5 = var1.method_8320(var3);
      int var6 = toolSlot(var2.method_31548(), var5);
      if (var6 < 0) {
         return false;
      } else {
         BlockHitResult var7 = visibleFace(var1, var2, var3);
         if (var7 == null) {
            return false;
         } else if (!this.hands()) {
            return true;
         } else {
            Rotation var8 = ServerRotation.sent();
            BlockHitResult var4 = var8 == null ? null : AnchorGuard.trace(var1, var2, var8);
            if (var4 == null || !var4.method_17777().equals(var3)) {
               ServerRotation.request(RotationUtils.fromTo(var2.method_33571(), var7.method_17784()), 6.0F);
               return true;
            } else if (var2.method_31548().method_67532() != var6) {
               var2.method_31548().method_61496(var6);
               return true;
            } else {
               ServerRotation.request(var8);
               if (MeleeAttacks.actedThisTick(var2)) {
                  return true;
               } else {
                  if (this.mc.field_1761.method_2902(var3, var4.method_17780())) {
                     var2.method_6104(Hand.field_5808);
                  }

                  MeleeAttacks.noteAction(var2);
                  this.diggingAt = var2.field_6012;
                  return true;
               }
            }
         }
      }
   }

   public static boolean digging() {
      CrystalBotModule var0 = ModuleRegistry.get(CrystalBotModule.class);
      ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
      return var0 != null && var0.isEnabled() && var1 != null && var1.field_6012 - var0.diggingAt <= 1;
   }

   private static int toolSlot(PlayerInventory var0, BlockState var1) {
      int var2 = -1;
      float var3 = 0.0F;

      for (int var4 = 0; var4 < 9; var4++) {
         ItemStack var6 = var0.method_5438(var4);
         boolean var5 = var6.method_7951(var1) || !var1.method_29291();
         if (var5 && !(var6.method_7924(var1) <= var3)) {
            var2 = var4;
            var3 = var6.method_7924(var1);
         }
      }

      return var2;
   }

   private static BlockHitResult visibleFace(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2) {
      Vec3d var3 = var1.method_33571();
      BlockHitResult var4 = null;
      double var5 = var1.method_55754() * var1.method_55754();

      for (Direction var10 : Direction.values()) {
         Vec3d var12 = Vec3d.method_24953(var2).method_43206(var10, 0.5);
         double var13 = var3.method_1025(var12);
         BlockHitResult var11;
         if (!(var13 > var5)
            && (var11 = AnchorGuard.trace(var0, var1, RotationUtils.fromTo(var3, var12.method_43206(var10, -0.05)))) != null
            && var11.method_17777().equals(var2)
            && var11.method_17780() == var10) {
            var4 = var11;
            var5 = var13;
         }
      }

      return var4;
   }

   private boolean camp(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3) {
      BlockPos var4 = var3.method_24515();
      if (var1.method_18467(EndCrystalEntity.class, new Box(var4).method_1009(3.0, 1.0, 3.0)).size() >= 2) {
         return false;
      } else {
         Vec3d var5 = var2.method_33571();
         BlockPos var6 = null;
         float var7 = this.selfCapNow - 2.0F;
         double var8 = var2.method_55755() - 0.1;

         for (BlockPos var11 : BlockPos.method_10097(var4.method_10069(-2, 0, -2), var4.method_10069(2, 0, 2))) {
            float var12;
            BlockPos var13;
            if (Math.max(Math.abs(var11.method_10263() - var4.method_10263()), Math.abs(var11.method_10260() - var4.method_10260())) == 2
               && (var13 = var11.method_10074().method_10062()).method_10264() >= feetY(var2)
               && Crystals.isBase(var1.method_8320(var13))
               && Crystals.canHost(var1, var13, null)
               && !(var5.method_1025(TargetingUtils.closestPoint(var5, Crystals.boxOn(var13))) > var8 * var8)
               && !((var12 = CrystalDamage.estimate(Crystals.explosionAt(var13), var2)) > var7)
               && !CrystalDamage.endangers(var2, var12)
               && topAim(var1, var2, var13) != null) {
               var6 = var13;
               var7 = var12;
            }
         }

         if (var6 == null) {
            return false;
         } else {
            this.body.face(this.mc, var2, var3, 1.0F);
            this.placeCrystal(var1, var2, var6);
            return true;
         }
      }
   }

   private static Vec3d topAim(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2) {
      Vec3d var4 = var1.method_33571();

      for (double[] var8 : new double[][]{{0.5, 0.5}, {0.2, 0.2}, {0.8, 0.2}, {0.2, 0.8}, {0.8, 0.8}}) {
         Vec3d var10 = new Vec3d(var2.method_10263() + var8[0], var2.method_10264() + 1.0, var2.method_10260() + var8[1]);
         BlockHitResult var9;
         if (!(var4.method_1022(var10) > var1.method_55754() - 0.1)
            && (var9 = AnchorGuard.trace(var0, var1, RotationUtils.fromTo(var4, var10))) != null
            && var9.method_17777().equals(var2)
            && var9.method_17780() == Direction.field_11036) {
            return var10;
         }
      }

      return null;
   }

   private void placeCrystal(ClientWorld var1, ClientPlayerEntity var2, BlockPos var3) {
      int var5 = BotBody.hotbarSlot(var2, var0 -> var0.method_31574(Items.field_8301));
      if (var5 >= 0 && this.hands()) {
         Rotation var6 = ServerRotation.sent();
         BlockHitResult var4 = var6 == null ? null : AnchorGuard.trace(var1, var2, var6);
         if (var4 == null || !var4.method_17777().equals(var3) || var4.method_17780() != Direction.field_11036) {
            Vec3d var8 = topAim(var1, var2, var3);
            if (var8 != null) {
               ServerRotation.request(RotationUtils.fromTo(var2.method_33571(), var8), 6.0F);
            }
         } else if (var2.method_31548().method_67532() != var5) {
            var2.method_31548().method_61496(var5);
         } else if (MeleeAttacks.handsFree(this.mc, var2)) {
            if (Crystals.rightClick(this.mc, var2, var4)) {
               Crystals.notePlace(var2, var3);
               MeleeAttacks.claimTick(this.mc);
            }

            ServerRotation.request(var6);
         }
      }
   }

   private boolean closeSpot(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3) {
      int var4 = guardBlockSlot(var2);
      if (var4 < 0) {
         return false;
      } else {
         if (this.blocking == null && this.tick >= this.spotScanAt) {
            this.spotScanAt = this.tick + 5;
            this.blocking = dangerSpot(var1, var2, var3);
         }

         if (this.blocking == null) {
            return false;
         } else if (var1.method_8320(this.blocking).method_45474()
            && var1.method_8628(Blocks.field_10540.method_9564(), this.blocking, ShapeContext.method_16194())) {
            AnchorGuard.Spot var5 = AnchorGuard.supportFor(var1, var2, var2.method_33571(), this.blocking);
            if (var5 == null) {
               this.blocking = null;
               return false;
            } else {
               if (this.placeAt(var1, var2, var5, var4) == 1) {
                  this.blocking = null;
               }

               return true;
            }
         } else {
            this.blocking = null;
            return false;
         }
      }
   }

   private static BlockPos dangerSpot(ClientWorld var0, ClientPlayerEntity var1, PlayerEntity var2) {
      BlockPos var3 = var1.method_24515();
      Vec3d var4 = var2.method_33571();
      double var5 = Math.max(4.6, var1.method_55754() + 0.1);
      BlockPos var7 = null;
      float var8 = 0.0F;
      float var9 = 0.0F;

      for (BlockPos var11 : BlockPos.method_10097(var3.method_10069(-4, -2, -4), var3.method_10069(4, 1, 4))) {
         Vec3d var12;
         float var13;
         if (Crystals.isBase(var0.method_8320(var11))
            && Crystals.canHost(var0, var11, null)
            && !(var4.method_1022(new Vec3d(var11.method_10263() + 0.5, var11.method_10264() + 1.0, var11.method_10260() + 0.5)) > var5)
            && !((var13 = CrystalDamage.estimate(var12 = Crystals.explosionAt(var11), var1)) <= var9)
            && !(CrystalDamage.estimate(var12, var2) >= var13 + 2.0F)) {
            if (var13 > var8) {
               var9 = var8;
               var8 = var13;
               var7 = var11.method_10084().method_10062();
            } else {
               var9 = var13;
            }
         }
      }

      return var8 >= 8.0F && var9 <= var8 / 2.0F ? var7 : null;
   }

   private int placeAt(ClientWorld var1, ClientPlayerEntity var2, AnchorGuard.Spot var3, int var4) {
      if (!this.hands()) {
         return 0;
      } else {
         Rotation var6 = ServerRotation.sent();
         BlockHitResult var5 = var6 == null ? null : AnchorGuard.hitAlong(var1, var2, var6, var3);
         if (var5 == null) {
            ServerRotation.request(RotationUtils.fromTo(var2.method_33571(), var3.aim()), 6.0F);
            return 0;
         } else if (var2.method_31548().method_67532() != var4) {
            var2.method_31548().method_61496(var4);
            return 0;
         } else if (!MeleeAttacks.handsFree(this.mc, var2)) {
            return 0;
         } else {
            AnchorGuard.click(this.mc, var2, var5);
            ServerRotation.request(var6);
            return 1;
         }
      }
   }

   private boolean defuse(ClientWorld var1, ClientPlayerEntity var2) {
      if (this.tick - this.defuseAt >= 10 && var1.method_27983() != World.field_25180) {
         BlockPos var3 = var2.method_24515();

         for (BlockPos var5 : BlockPos.method_10097(var3.method_10069(-5, -2, -5), var3.method_10069(5, 3, 5))) {
            BlockState var8 = var1.method_8320(var5);
            BlockPos var6;
            Vec3d var7;
            if (var8.method_27852(Blocks.field_23152)
               && (Integer)var8.method_11654(RespawnAnchorBlock.field_23153) > 0
               && var2.method_56093(var5, -0.1)
               && !var5.equals(this.digAnchor)
               && !(
                  CrystalDamage.estimate(var7 = Vec3d.method_24953(var6 = var5.method_10062()), var2, 5.0F, var6) > Math.max(1.0F, 0.15F * var2.method_6032())
               )) {
               boolean var9 = false;

               for (PlayerEntity var11 : enemies(var1, var2, 16.0)) {
                  var9 |= var11.method_33571().method_1022(var7) <= 5.5;
               }

               if (var9) {
                  PlayerInventory var14 = var2.method_31548();
                  int var15 = AnchorGuard.detonatorRoute(var14, var2.method_6047());
                  if (var15 == -1 || !this.hands()) {
                     return false;
                  }

                  BlockHitResult var12 = AnchorGuard.anchorHit(var1, var2, var6);
                  if (var12 != null) {
                     if (var15 != -2) {
                        var14.method_61496(var15);
                        return true;
                     }

                     if (!MeleeAttacks.handsFree(this.mc, var2)) {
                        return true;
                     }

                     AnchorGuard.click(this.mc, var2, var12);
                     this.defuseAt = this.tick;
                     return true;
                  }

                  Vec3d var13 = AnchorGuard.anchorAim(var1, var2, var6);
                  if (var13 != null) {
                     ServerRotation.request(RotationUtils.fromTo(var2.method_33571(), var13), 6.0F);
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean guard(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3) {
      float var4 = var2.method_6032() + var2.method_6067();
      BlockPos var5 = null;
      float var6 = 0.0F;
      BlockPos var7 = var2.method_24515();

      for (BlockPos var9 : BlockPos.method_10097(var7.method_10069(-5, -2, -5), var7.method_10069(5, 3, 5))) {
         Vec3d var10;
         float var11;
         if (var1.method_8320(var9).method_27852(Blocks.field_23152)
            && !var9.equals(this.digAnchor)
            && (
               !((var11 = CrystalDamage.estimate(var10 = Vec3d.method_24953(var9), var2, 5.0F, var9.method_10062())) < Math.max(4.0F, var4 * 0.3F))
                  || !(var11 < var4 - 1.0F)
            )
            && !(var3.method_33571().method_1022(var10) > 5.5)
            && !(var11 <= var6)) {
            var6 = var11;
            var5 = var9.method_10062();
         }
      }

      if (var5 == null) {
         return false;
      } else {
         AnchorGuard.Spot var12 = AnchorGuard.find(var1, var2, var5, var2.method_33571());
         if (var12 != null && var12.standing()) {
            return false;
         } else {
            int var13 = guardBlockSlot(var2);
            if (var12 != null && var13 >= 0 && this.tick - this.shieldAt >= 20) {
               this.body.face(this.mc, var2, var3, 1.0F);
               if (this.placeAt(var1, var2, var12, var13) == 1) {
                  this.shieldAt = this.tick;
               }

               return true;
            } else {
               this.body.face(this.mc, var2, var3, 1.0F);
               this.body.away(this.mc, var2, Vec3d.method_24953(var5));
               return true;
            }
         }
      }
   }

   private static int guardBlockSlot(ClientPlayerEntity var0) {
      int var1 = BotBody.hotbarSlot(var0, OBSIDIAN);
      return var1 >= 0 ? var1 : BotBody.hotbarSlot(var0, AnchorGuard::isGuardBlock);
   }

   private boolean cover(ClientWorld var1, ClientPlayerEntity var2, PlayerEntity var3, double var4) {
      boolean var7 = this.boxingAt != null && this.boxingAt.equals(var2.method_24515()) && this.tick < this.boxingUntil;
      boolean var6 = var2.method_6032() + var2.method_6067() <= this.retreatAt() + 4.0F || this.stance == CrystalBotModule.Stance.GUARD;
      if (this.surround.get() && !var7 && var6 && var2.method_24828()) {
         this.boxingAt = var2.method_24515();
         this.boxingUntil = this.tick + 60;
         var7 = true;
      }

      if (!var7) {
         this.boxingAt = null;
      }

      return this.surround.get() && var7 && var2.method_24828() && this.boxIn(var1, var2);
   }

   private boolean boxIn(ClientWorld var1, ClientPlayerEntity var2) {
      List var3 = openAroundFeet(var1, var2);
      if (!var3.isEmpty() && BotBody.hotbarSlot(var2, OBSIDIAN) >= 0) {
         if (!this.hands()) {
            return true;
         } else {
            for (BlockPos var5 : var3) {
               BlockHitResult var6 = AutoTrapModule.support(var1, var2, var5);
               if (var6 != null && (!var5.equals(this.boxSkipped) || this.tick >= this.boxSkippedUntil)) {
                  BlockHitResult var7 = AutoTrapModule.seenByServer(var1, var2, var6);
                  if (var7 != null) {
                     int var8 = BotBody.hotbarSlot(var2, OBSIDIAN);
                     if (var2.method_31548().method_67532() != var8) {
                        var2.method_31548().method_61496(var8);
                        return true;
                     }

                     if (!MeleeAttacks.handsFree(this.mc, var2)) {
                        return true;
                     }

                     ActionResult var9 = this.mc.field_1761.method_2896(var2, Hand.field_5808, var7);
                     if (var9.method_23665()) {
                        var2.method_6104(Hand.field_5808);
                        MeleeAttacks.noteAction(var2);
                        ServerRotation.request(ServerRotation.sent());
                     }

                     return true;
                  }

                  this.boxAimTicks = var5.equals(this.boxAiming) ? this.boxAimTicks + 1 : 0;
                  this.boxAiming = var5;
                  if (this.boxAimTicks < 10) {
                     ServerRotation.request(RotationUtils.fromTo(var2.method_33571(), var6.method_17784()), 6.0F);
                     return true;
                  }

                  this.boxSkipped = var5;
                  this.boxSkippedUntil = this.tick + 40;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private static List<BlockPos> openAroundFeet(ClientWorld var0, ClientPlayerEntity var1) {
      ArrayList var2 = new ArrayList();

      for (BlockPos var4 : ring(var1)) {
         BlockState var5 = var0.method_8320(var4);
         if (var5.method_45474()
            && !(var4.method_46558().method_1022(var1.method_33571()) > var1.method_55754())
            && var0.method_8628(Blocks.field_10540.method_9564(), var4, ShapeContext.method_16194())) {
            var2.add(var4);
         }
      }

      return var2;
   }

   private static boolean surrounded(ClientWorld var0, ClientPlayerEntity var1) {
      for (BlockPos var3 : ring(var1)) {
         if (var0.method_8320(var3).method_26220(var0, var3).method_1110()) {
            return false;
         }
      }

      return true;
   }

   private static Set<BlockPos> ring(ClientPlayerEntity var0) {
      Box var1 = var0.method_5829();
      int var2 = MathHelper.method_15357(var1.field_1322 + 0.01);
      LinkedHashSet var3 = new LinkedHashSet();

      for (int var4 = MathHelper.method_15357(var1.field_1323); var4 <= MathHelper.method_15357(var1.field_1320 - 1.0E-4); var4++) {
         for (int var5 = MathHelper.method_15357(var1.field_1321); var5 <= MathHelper.method_15357(var1.field_1324 - 1.0E-4); var5++) {
            var3.add(BlockPos.method_10064(var4, 0, var5));
         }
      }

      LinkedHashSet var12 = new LinkedHashSet();

      for (long var6 : var3) {
         BlockPos var8 = BlockPos.method_10092(var6).method_33096(var2);

         for (Direction var10 : net.minecraft.util.math.Direction.Type.field_11062) {
            BlockPos var11 = var8.method_10093(var10);
            if (!var3.contains(BlockPos.method_10064(var11.method_10263(), 0, var11.method_10260()))) {
               var12.add(var11);
            }
         }
      }

      return var12;
   }

   private static boolean armorLow(LivingEntity var0) {
      for (EquipmentSlot var2 : EquipmentSlot.field_54086) {
         ItemStack var3;
         if (var2.method_46643() && (var3 = var0.method_6118(var2)).method_7963() && var3.method_7936() - var3.method_7919() < var3.method_7936() * 0.3F) {
            return true;
         }
      }

      return false;
   }

   private boolean throwXp(ClientPlayerEntity var1) {
      if (BotBody.hotbarSlot(var1, XP) < 0) {
         return false;
      } else {
         this.mending = true;
         Aim.turn(this.mc, var1.method_36454(), 86.0F, 22.0F);
         if (var1.method_36455() > 70.0F) {
            this.body.use(this.mc, var1, XP);
         }

         return true;
      }
   }

   private boolean mendIdle(ClientPlayerEntity var1) {
      if (this.mendArmor.get() && (armorLow(var1) || this.mending) && hasDamagedArmor(var1)) {
         CombatMacroLock.take(this);
         return this.throwXp(var1);
      } else {
         return false;
      }
   }

   private static boolean hasDamagedArmor(ClientPlayerEntity var0) {
      for (EquipmentSlot var2 : EquipmentSlot.field_54086) {
         if (var2.method_46643() && var0.method_6118(var2).method_7986()) {
            return true;
         }
      }

      return false;
   }

   private static int count(ClientPlayerEntity var0, Predicate<ItemStack> var1) {
      int var2 = 0;
      PlayerInventory var3 = var0.method_31548();

      for (int var4 = 0; var4 < 36; var4++) {
         if (var1.test(var3.method_5438(var4))) {
            var2 += var3.method_5438(var4).method_7947();
         }
      }

      return var2 + (var1.test(var0.method_6079()) ? var0.method_6079().method_7947() : 0);
   }

   @Environment(EnvType.CLIENT)
   private static enum Stance {
      PRESS(3.5F, 10.0F, 5.0F, 0.7),
      HOLD(4.5F, 8.0F, 6.0F, 1.0),
      GUARD(6.0F, 5.0F, 7.0F, 1.6),
      RUN(99.0F, 3.0F, 99.0F, 2.0);

      final float minDamage;
      final float selfCap;
      final float anchorMin;
      final double cover;

      private Stance(float nullxx, float nullxxx, float nullxxxx, double nullxxxxx) {
         this.minDamage = nullxx;
         this.selfCap = nullxxx;
         this.anchorMin = nullxxxx;
         this.cover = nullxxxxx;
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Throw {
      ESCAPE,
      CHASE;

      private Throw() {
      }
   }
}
