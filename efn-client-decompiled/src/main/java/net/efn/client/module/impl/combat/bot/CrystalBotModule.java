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
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class CrystalBotModule extends ClientModule {
   private static final double MIN = 2.4;
   private static final double IDEAL = 3.2;
   private static final double MAX = 4.0;
   private static final Predicate<ItemStack> GAPPLE = itemStack -> itemStack.isOf(Items.GOLDEN_APPLE) || itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE);
   private static final Predicate<ItemStack> XP = itemStack -> itemStack.isOf(Items.EXPERIENCE_BOTTLE);
   private static final Predicate<ItemStack> PEARL = itemStack -> itemStack.isOf(Items.ENDER_PEARL);
   private static final Predicate<ItemStack> OBSIDIAN = itemStack -> itemStack.isOf(Items.OBSIDIAN);
   private static final Predicate<ItemStack> ANCHOR = itemStack -> itemStack.isOf(Items.RESPAWN_ANCHOR);
   private static final Predicate<ItemStack> GLOWSTONE = itemStack -> itemStack.isOf(Items.GLOWSTONE);
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
   private final MinecraftClient mc = MinecraftClient.getInstance();
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
      this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.tick());
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
   public void onPacketApplied(PacketAppliedEvent packetAppliedEvent) {
      ClientPlayerEntity var3 = this.mc.player;
      if (var3 != null && this.mc.world != null) {
         Packet var4 = packetAppliedEvent.packet;
         EntityStatusS2CPacket var2;
         if (var4 instanceof EntityStatusS2CPacket && (var2 = (EntityStatusS2CPacket)var4).getStatus() == 35) {
            if (var2.getEntity(this.mc.world) == var3) {
               this.popStreak = this.tick - this.ownPopAt <= 600 ? this.popStreak + 1 : 1;
               this.ownPopAt = this.tick;
            } else if (this.fought != null && var2.getEntity(this.mc.world) == this.fought) {
               this.theirPopAt = this.tick;
               this.openerPops++;
            }
         }
      }
   }

   @EventHandler
   public void onPacketReceive(PacketReceiveEvent packetReceiveEvent) {
      ClientPlayerEntity var2 = this.mc.player;
      if (var2 != null) {
         if (packetReceiveEvent.packet instanceof PlayerPositionLookS2CPacket var4) {
            Vec3d var5 = var4.change().position();
            Vec3d var6 = var2.getEntityPos();
            Set var7 = var4.relatives();
            this.pause
               .moved(
                  new Vec3d(
                     var7.contains(PositionFlag.X) ? var6.x + var5.x : var5.x,
                     var7.contains(PositionFlag.Y) ? var6.y + var5.y : var5.y,
                     var7.contains(PositionFlag.Z) ? var6.z + var5.z : var5.z
                  ),
                  this.tick - this.pearlAt
               );
         } else if (packetReceiveEvent.packet instanceof WorldTimeUpdateS2CPacket var11) {
            this.pause.time(var11.time());
         } else if (packetReceiveEvent.packet instanceof ExplosionS2CPacket var12) {
            this.air.blast(var12.center());
         } else {
            Packet var10 = packetReceiveEvent.packet;
            EntityDamageS2CPacket var13;
            if (var10 instanceof EntityDamageS2CPacket && (var13 = (EntityDamageS2CPacket)var10).entityId() == var2.getId() && var13.sourceCauseId() >= 0) {
               this.attackedBy = var13.sourceCauseId();
               this.attackedAt = this.tick;
            }
         }
      }
   }

   private void tick() {
      this.tick++;
      ClientPlayerEntity var5 = this.mc.player;
      ClientWorld var6 = this.mc.world;
      this.body.release(this.mc);
      if (var5 != null && var6 != null && MeleeAttacks.canAct(this.mc) && !var5.isDead()) {
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

            boolean var3 = var5.getHealth() <= this.eatAt() && var5.getAbsorptionAmount() < 4.0F;
            if (var5.getHealth() > this.eatAt() + 4.0F || var5.getAbsorptionAmount() >= 4.0F) {
               this.needsGapple = false;
            }

            if (var8 == null) {
               this.launching = false;
               this.throwing = null;
               CombatMacroLock.release(this);
               if ((var3 || this.needsGapple || this.eating && var5.isUsingItem()) && this.hands() && this.body.use(this.mc, var5, GAPPLE)) {
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
               RivalBook.Rival var10 = this.learn.get() ? this.book.of(var8.getUuid()) : null;
               this.watch(var6, var8, var10);
               if (feetY(var5) < MathHelper.floor(var8.getY() + 0.01)) {
                  this.body.stayGrounded(true);
               }

               this.air.note(var8, this.tick, var10);
               Combo.prefer(var5, var8);
               Combo.flight(var5, var8, i -> this.air.lead(i, this.tick));
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
               double var12 = var5.distanceTo(var8);
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
                                 boolean var2 = var8.isUsingItem()
                                    && var8.getActiveItem().contains(DataComponentTypes.FOOD)
                                    && totems(var5) >= 1
                                    && var5.getHealth() > 8.0F;
                                 if (var3 && !var2) {
                                    this.needsGapple = true;
                                 }

                                 if (this.needsGapple || this.eating && var5.isUsingItem()) {
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
                                          boolean var1 = var12 >= 8.0 || BotBody.hotbarSlot(var5, itemStack -> itemStack.isOf(Items.END_CRYSTAL)) < 0;
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

   private PlayerEntity choose(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      PlayerEntity var4 = null;
      double var5 = -1.0 / 0.0;

      for (PlayerEntity var8 : enemies(clientWorld, clientPlayerEntity, this.range.get())) {
         double var9 = this.worth(clientPlayerEntity, var8);
         if (var9 > var5) {
            var4 = var8;
            var5 = var9;
         }
      }

      if (playerEntity == null || !playerEntity.isAlive() || this.fought == null) {
         this.challenger = null;
         if (var4 != null) {
            this.body.hold(var4);
         }

         return var4;
      } else if (var4 != null && var4 != playerEntity) {
         boolean var12 = clientPlayerEntity.distanceTo(playerEntity) <= 6.0;
         double var13 = this.worth(clientPlayerEntity, playerEntity);
         double var10 = var12 ? 8.0 : 2.0;
         if (var5 < var13 + var10) {
            this.challenger = null;
            return playerEntity;
         } else if (var4 != this.challenger) {
            this.challenger = var4;
            this.challengerSince = this.tick;
            return playerEntity;
         } else if (this.tick - this.challengerSince < (var12 ? 20 : 6)) {
            return playerEntity;
         } else {
            this.challenger = null;
            this.body.hold(var4);
            return var4;
         }
      } else {
         this.challenger = null;
         return playerEntity;
      }
   }

   private double worth(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      double var3 = clientPlayerEntity.distanceTo(playerEntity);
      double var5 = var3 <= 6.0 ? 1.0 : (var3 >= 10.0 ? 0.0 : (10.0 - var3) / 4.0);
      double var7 = 4.0 * var5 - 0.2 * var3 + (20.0F - Math.min(20.0F, CrystalDamage.health(playerEntity))) / 5.0;
      if (!playerEntity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) && !playerEntity.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
         var7++;
      }

      if (armorLow(playerEntity)) {
         var7++;
      }

      if (playerEntity.getId() == this.attackedBy && this.tick - this.attackedAt <= 60 && var3 <= 4.5) {
         var7 += 4.0;
      }

      return var7;
   }

   private boolean finish(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      if (this.pause.slowServer() || playerEntity.hurtTime > 0 || reach(clientPlayerEntity, playerEntity) > 2.95 || CrystalDamage.health(playerEntity) > 3.0F) {
         return false;
      } else if (!MeleeAttacks.isMeleeWeapon(clientPlayerEntity.getMainHandStack()) || MeleeAttacks.charge(clientPlayerEntity) < 0.9F) {
         return false;
      } else if (!this.hands()) {
         return true;
      } else {
         this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
         this.strike(clientPlayerEntity, playerEntity);
         return true;
      }
   }

   private void newFight(PlayerEntity playerEntity) {
      if (this.fought != null && this.lowSpell && this.pearledInSpell && this.learn.get()) {
         this.book.of(this.fought.getUuid()).lowSpell(true);
      }

      this.fought = playerEntity;
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
      float var2 = this.lastTheirHealth = playerEntity == null ? 0.0F : CrystalDamage.health(playerEntity);
      if (playerEntity != null) {
         ModuleChat.send(this, ModuleChat.body().text("Fighting ").name(playerEntity.getName().getString()).text("."));
         if (this.learn.get()) {
            this.book.of(playerEntity.getUuid()).fought();
         }
      } else {
         ModuleChat.send(this, ModuleChat.body().text("No enemy in range, waiting."));
      }
   }

   private CrystalBotModule.Stance stance(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, RivalBook.Rival rival) {
      int var6 = totems(clientPlayerEntity);
      float var7 = clientPlayerEntity.getHealth() + clientPlayerEntity.getAbsorptionAmount();
      float var8 = worstThreat(clientWorld, clientPlayerEntity, this.digAnchor) / Math.max(1.0F, var7);
      int var9 = enemiesWithin(clientWorld, clientPlayerEntity, 8.0);
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
      } else if (this.finishing(playerEntity, rival)) {
         return CrystalBotModule.Stance.PRESS;
      } else {
         boolean var11 = var7 <= this.retreatAt() + 2.0F || var6 <= 1 || var8 >= 0.35F;
         return var11 && var7 <= 8.0F ? CrystalBotModule.Stance.GUARD : CrystalBotModule.Stance.PRESS;
      }
   }

   private boolean finishing(PlayerEntity playerEntity, RivalBook.Rival rival) {
      if (this.theirPopAt >= 0 && !playerEntity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
         int var3 = rival == null ? 8 : MathHelper.clamp(rival.retotemWindow(8), 3, 30);
         return this.tick - this.theirPopAt <= var3;
      } else {
         return false;
      }
   }

   private void steer(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, RivalBook.Rival rival) {
      float var8 = 2.0F;
      float var9 = clientPlayerEntity.getHealth() + clientPlayerEntity.getAbsorptionAmount();
      float var10 = var9 - (clientPlayerEntity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) ? 1.0F : 4.0F);
      float var7;
      this.selfCapNow = var7 = Math.max(0.5F, Math.min(this.stance.selfCap + var8, var10));
      float var6 = this.finishing(playerEntity, rival) ? 2.0F : this.stance.minDamage;
      if (feetY(clientPlayerEntity) >= MathHelper.floor(playerEntity.getY() + 0.01) && !Combo.inHole(playerEntity)) {
         var6 = Math.min(var6, 2.5F);
      }

      CrystalAuraModule var5;
      if ((var5 = ModuleRegistry.get(CrystalAuraModule.class)) != null) {
         var5.steer(clientPlayerEntity, var6, var7);
         var5.crystalsFrom(clientPlayerEntity, feetY(clientPlayerEntity) + 1);
         var5.fullSpeed(clientPlayerEntity);
         var5.buildEagerly(clientPlayerEntity);
      }

      AnchorAuraModule var4;
      if ((var4 = ModuleRegistry.get(AnchorAuraModule.class)) != null) {
         var4.steer(clientPlayerEntity, this.finishing(playerEntity, rival) ? 2.0F : this.stance.anchorMin, Math.min(var7, 11.0F));
      }
   }

   private float eatAt() {
      return this.eatBelow.get() - 2.0F;
   }

   private float retreatAt() {
      return 4.0F;
   }

   private boolean critical(ClientPlayerEntity clientPlayerEntity) {
      return clientPlayerEntity.getHealth() + clientPlayerEntity.getAbsorptionAmount() <= this.retreatAt()
         && !clientPlayerEntity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
   }

   private static int totems(ClientPlayerEntity clientPlayerEntity) {
      int var1 = clientPlayerEntity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) ? 1 : 0;
      PlayerInventory var2 = clientPlayerEntity.getInventory();

      for (int var3 = 0; var3 < 36; var3++) {
         if (var2.getStack(var3).isOf(Items.TOTEM_OF_UNDYING)) {
            var1 += var2.getStack(var3).getCount();
         }
      }

      return var1;
   }

   private static float worstThreat(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      List var3 = enemies(clientWorld, clientPlayerEntity, 16.0);
      float var4 = 0.0F;

      for (EndCrystalEntity var6 : clientWorld.getNonSpectatingEntities(EndCrystalEntity.class, clientPlayerEntity.getBoundingBox().expand(12.0))) {
         Vec3d var7 = var6.getEntityPos();
         float var8 = CrystalDamage.estimate(var7, clientPlayerEntity);
         if (var8 > var4 && worthIt(var3, var6.getBoundingBox(), var7, var8, 4.0, 6.0F, null)) {
            var4 = var8;
         }
      }

      BlockPos var12 = clientPlayerEntity.getBlockPos();

      for (BlockPos var14 : BlockPos.iterate(var12.add(-5, -2, -5), var12.add(5, 3, 5))) {
         BlockState var11 = clientWorld.getBlockState(var14);
         Vec3d var9;
         float var10;
         BlockPos var15;
         if (var11.isOf(Blocks.RESPAWN_ANCHOR)
            && var11.get(RespawnAnchorBlock.CHARGES) > 0
            && !var14.equals(blockPos)
            && (var10 = CrystalDamage.estimate(var9 = Vec3d.ofCenter(var15 = var14.toImmutable()), clientPlayerEntity, 5.0F, var15)) > var4
            && worthIt(var3, new Box(var15), var9, var10, 5.5, 5.0F, var15)) {
            var4 = var10;
         }
      }

      return var4;
   }

   private static boolean worthIt(List<PlayerEntity> list, Box box, Vec3d vec3d, float f, double d, float g, BlockPos blockPos) {
      for (PlayerEntity var9 : list) {
         Vec3d var10 = var9.getEyePos();
         if (!(var10.squaredDistanceTo(TargetingUtils.closestPoint(var10, box)) > d * d) && CrystalDamage.estimate(vec3d, var9, g, blockPos) < f) {
            return true;
         }
      }

      return false;
   }

   private static int enemiesWithin(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, double d) {
      int var4 = 0;

      for (PlayerEntity var6 : clientWorld.getPlayers()) {
         if (var6 != clientPlayerEntity && var6.isAlive() && MeleeAttacks.isEnemy(clientPlayerEntity, var6) && var6.distanceTo(clientPlayerEntity) <= d) {
            var4++;
         }
      }

      return var4;
   }

   private static List<PlayerEntity> enemies(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, double d) {
      ArrayList var4 = new ArrayList();

      for (PlayerEntity var6 : clientWorld.getPlayers()) {
         if (var6 != clientPlayerEntity && var6.isAlive() && MeleeAttacks.isEnemy(clientPlayerEntity, var6) && var6.distanceTo(clientPlayerEntity) <= d) {
            var4.add(var6);
         }
      }

      return var4;
   }

   private void runAuras(ClientWorld clientWorld) {
      CrystalAuraModule var3 = ModuleRegistry.get(CrystalAuraModule.class);
      if (var3 != null && !var3.isEnabled()) {
         if (this.crystalWasOn == null) {
            this.crystalWasOn = false;
         }

         var3.setEnabled(true);
      }

      AnchorAuraModule var4 = ModuleRegistry.get(AnchorAuraModule.class);
      boolean var2 = this.anchors.get() && clientWorld.getRegistryKey() != World.NETHER;
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

   private static boolean blastNear(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      for (EndCrystalEntity var3 : clientWorld.getNonSpectatingEntities(EndCrystalEntity.class, clientPlayerEntity.getBoundingBox().expand(7.0))) {
         if (CrystalDamage.estimate(var3.getEntityPos(), clientPlayerEntity) >= 1.0F) {
            return true;
         }
      }

      BlockPos var7 = clientPlayerEntity.getBlockPos();
      int var8 = (int)Math.ceil(7.0);

      for (BlockPos var5 : BlockPos.iterate(var7.add(-var8, -2, -var8), var7.add(var8, 3, var8))) {
         BlockState var6 = clientWorld.getBlockState(var5);
         if (var6.isOf(Blocks.RESPAWN_ANCHOR)
            && var6.get(RespawnAnchorBlock.CHARGES) > 0
            && Vec3d.ofCenter(var5).distanceTo(clientPlayerEntity.getEntityPos()) <= 7.0) {
            return true;
         }
      }

      return false;
   }

   private static void handBack(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var1 = clientPlayerEntity.getMainHandStack();
      if (!var1.isOf(Items.END_CRYSTAL)
         && !var1.isOf(Items.OBSIDIAN)
         && !var1.isOf(Items.RESPAWN_ANCHOR)
         && !var1.isOf(Items.GLOWSTONE)
         && !Crystals.isCombatItem(var1)) {
         int var2 = BotBody.hotbarSlot(clientPlayerEntity, itemStack -> itemStack.isOf(Items.END_CRYSTAL));
         if (var2 < 0) {
            var2 = BotBody.hotbarSlot(clientPlayerEntity, MeleeAttacks::isMeleeWeapon);
         }

         if (var2 >= 0) {
            clientPlayerEntity.getInventory().setSelectedSlot(var2);
         }
      }
   }

   private boolean hands() {
      CombatMacroLock.take(this);
      return !MeleeAttacks.actedThisTick(this.mc.player);
   }

   private void watch(ClientWorld clientWorld, PlayerEntity playerEntity, RivalBook.Rival rival) {
      float var4 = CrystalDamage.health(playerEntity);
      if (var4 < this.lastTheirHealth - 0.5F) {
         this.hurtAt = this.tick;
      }

      if (var4 < this.lastTheirHealth - 0.5F || !Combo.inHole(playerEntity)) {
         this.progressAt = this.tick;
      }

      this.lastTheirHealth = var4;
      if (this.openerArm >= 0 && this.tick - this.openerAt >= 40) {
         float var5 = this.openerHealth - var4 + 10.0F * this.openerPops;
         if (rival != null) {
            rival.opened(this.openerArm, this.openerReach, MathHelper.clamp(var5 / 12.0F, 0.0F, 1.5F));
         }

         this.openerArm = -1;
      }

      if (this.launchedAt >= 0) {
         this.launchPeak = Math.max(this.launchPeak, playerEntity.getY() - this.launchY);
         if (this.tick - this.launchedAt >= 8) {
            if (rival != null) {
               rival.launched(this.launchPeak >= 0.3);
            }

            this.launchedAt = -1;
         }
      }

      if (this.theirPopAt >= 0 && rival != null) {
         if (playerEntity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            rival.retotemed(this.tick - this.theirPopAt);
            this.theirPopAt = -1;
         } else if (this.tick - this.theirPopAt > 100) {
            rival.retotemed(100);
            this.theirPopAt = -1;
         }
      } else if (this.theirPopAt >= 0 && this.tick - this.theirPopAt > 100) {
         this.theirPopAt = -1;
      }

      if (Combo.inHole(playerEntity)) {
         if (this.holeSince < 0) {
            this.holeSince = this.tick;
         }
      } else if (this.holeSince >= 0) {
         if (rival != null) {
            rival.leftHole(Math.min(600, this.tick - this.holeSince));
         }

         this.holeSince = -1;
      }

      if (!this.lowSpell && var4 <= 8.0F) {
         this.lowSpell = true;
         this.pearledInSpell = false;
      } else if (this.lowSpell && var4 > 12.0F) {
         if (rival != null) {
            rival.lowSpell(this.pearledInSpell);
         }

         this.lowSpell = false;
      }

      for (EnderPearlEntity var6 : clientWorld.getNonSpectatingEntities(EnderPearlEntity.class, playerEntity.getBoundingBox().expand(4.0))) {
         if (var6.getOwner() == playerEntity && var6.age <= 2 && var6.getId() != this.followedPearl) {
            if (this.lowSpell) {
               this.pearledInSpell = true;
            }

            this.theirPearl = var6.getId();
         }
      }
   }

   private boolean getAway(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      if (clientPlayerEntity.distanceTo(playerEntity) < 12.0 && this.pearlAway(clientWorld, clientPlayerEntity, playerEntity)) {
         return true;
      } else if (clientPlayerEntity.distanceTo(playerEntity) >= 16.0
         && clientPlayerEntity.getHealth() <= this.eatAt() + 4.0F
         && this.hands()
         && this.body.use(this.mc, clientPlayerEntity, GAPPLE)) {
         this.eating = true;
         return true;
      } else {
         this.body.flee(this.mc, clientPlayerEntity, playerEntity);
         return true;
      }
   }

   private boolean pearlAway(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      if (this.escape.get() && this.pearlReady(clientPlayerEntity) && this.tick >= this.searchAfter) {
         List var4 = enemies(clientWorld, clientPlayerEntity, 48.0);
         Rotation var5 = PearlFlight.escape(clientPlayerEntity, var4, 20.0);
         this.throwAway = 16.0;
         if (var5 == null) {
            var5 = PearlFlight.escape(clientPlayerEntity, var4, 12.0);
            this.throwAway = 12.0;
         }

         if (var5 == null) {
            this.searchAfter = this.tick + 10;
            return false;
         } else {
            this.startThrow(CrystalBotModule.Throw.ESCAPE, var5, null);
            return this.keepThrowing(clientPlayerEntity, playerEntity);
         }
      } else {
         return false;
      }
   }

   private boolean pearlReady(ClientPlayerEntity clientPlayerEntity) {
      return this.tick - this.pearlAt >= 30
         && BotBody.hotbarSlot(clientPlayerEntity, PEARL) >= 0
         && !clientPlayerEntity.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))
         && (
            clientPlayerEntity.getHealth() + clientPlayerEntity.getAbsorptionAmount() > 6.0F
               || clientPlayerEntity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
         );
   }

   private void startThrow(CrystalBotModule.Throw throw_, Rotation rotation, Vec3d vec3d) {
      this.throwing = throw_;
      this.throwLook = rotation;
      this.throwSpot = vec3d;
      this.throwUntil = this.tick + 14;
   }

   private boolean keepThrowing(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      if (this.tick > this.throwUntil || !this.pearlReady(clientPlayerEntity)) {
         this.throwing = null;
         this.pearlAt = this.tick - 15;
         return false;
      } else if (!this.hands()) {
         return true;
      } else {
         Aim.turn(this.mc, this.throwLook.yaw(), this.throwLook.pitch(), 27.0F);
         int var4 = BotBody.hotbarSlot(clientPlayerEntity, PEARL);
         if (clientPlayerEntity.getInventory().getSelectedSlot() != var4) {
            clientPlayerEntity.getInventory().setSelectedSlot(var4);
            return true;
         } else if (!ServerRotation.desynced() && !clientPlayerEntity.isUsingItem() && MeleeAttacks.handsFree(this.mc, clientPlayerEntity)) {
            if (clientPlayerEntity.isOnGround() && !(clientPlayerEntity.getMovement().horizontalLengthSquared() > 1.0E-4)) {
               float var5 = clientPlayerEntity.getYaw();
               float var6 = clientPlayerEntity.getPitch();
               Rotation var7 = ServerRotation.commit(clientPlayerEntity);
               if (!(Math.abs(MathHelper.wrapDegrees(var7.yaw() - var5)) > 0.01F) && !(Math.abs(var7.pitch() - var6) > 0.01F)) {
                  boolean var3 = this.throwing == CrystalBotModule.Throw.ESCAPE
                     ? PearlFlight.escapes(clientPlayerEntity, var5, var6, enemies(this.mc.world, clientPlayerEntity, 48.0), this.throwAway)
                     : PearlFlight.reaches(clientPlayerEntity, var5, var6, this.throwSpot, 0.0, 3.0);
                  if (!var3) {
                     return true;
                  } else {
                     if (this.mc.interactionManager.interactItem(clientPlayerEntity, Hand.MAIN_HAND).isAccepted()) {
                        clientPlayerEntity.swingHand(Hand.MAIN_HAND);
                        MeleeAttacks.claimTick(this.mc);
                        this.pearlAt = this.tick;
                        if (this.throwing == CrystalBotModule.Throw.ESCAPE) {
                           ModuleChat.send(this, ModuleChat.body().warn("Pearled away").text("."));
                        } else {
                           ModuleChat.send(this, ModuleChat.body().text("Pearled after ").name(playerEntity.getName().getString()).text("."));
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

   private boolean chasePearl(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, RivalBook.Rival rival) {
      if ((this.stance == CrystalBotModule.Stance.PRESS || this.stance == CrystalBotModule.Stance.HOLD) && this.pearlReady(clientPlayerEntity)) {
         float var5 = clientPlayerEntity.getHealth() + clientPlayerEntity.getAbsorptionAmount();
         if (var5 < 6.0F) {
            return false;
         } else {
            int var6 = count(clientPlayerEntity, PEARL);
            boolean var7 = this.theirPearl >= 0;
            boolean var8 = totems(clientPlayerEntity) == 0 && clientPlayerEntity.getHealth() <= 12.0F
               || !var7 && rival != null && rival.pearlsWhenLow() && CrystalDamage.health(playerEntity) <= 8.0F;
            if (var6 <= (var8 ? 1 : 0)) {
               return false;
            } else {
               Vec3d var9 = null;
               if (var7) {
                  this.followedPearl = this.theirPearl;
                  this.theirPearl = -1;
                  Entity var12 = clientWorld.getEntityById(this.followedPearl);
                  EnderPearlEntity var10;
                  PearlFlight.Landing var11;
                  if (var12 instanceof EnderPearlEntity
                     && (var11 = PearlFlight.fly(clientWorld, var10 = (EnderPearlEntity)var12, var10.getEntityPos(), var10.getVelocity())) != null
                     && var11.at().distanceTo(clientPlayerEntity.getEntityPos()) > 8.0) {
                     var9 = var11.at();
                  }
               }

               double var22 = clientPlayerEntity.distanceTo(playerEntity);
               if (var9 == null && var22 <= this.range.get() + 8.0) {
                  Vec3d var14 = playerEntity.getEntityPos().subtract(clientPlayerEntity.getEntityPos()).multiply(1.0, 0.0, 1.0).normalize();
                  Vec3d var15 = playerEntity.getEntityPos().subtract(playerEntity.lastX, playerEntity.lastY, playerEntity.lastZ);
                  boolean var16 = var22 >= 12.0 && var15.x * var14.x + var15.z * var14.z > 0.12;
                  BlockPos var17 = playerEntity.getBlockPos().down();
                  boolean var18 = !clientWorld.getBlockState(var17).getCollisionShape(clientWorld, var17).isEmpty();
                  boolean var19 = var22 >= 3.0 && playerEntity.getY() - clientPlayerEntity.getY() >= 3.0 && var18;
                  boolean var20 = var22 >= 4.0 && clientPlayerEntity.getY() - playerEntity.getY() >= 4.0 && var18;
                  boolean var13 = var22 > 6.0 && this.tick - this.closerAt > 60;
                  if (var16 || var19 || var20 || var13) {
                     var9 = playerEntity.getEntityPos().add(var15.multiply(1.0, 0.0, 1.0).multiply(PearlFlight.flightTicks(var22)));
                  }

                  AirTrack.Landing var23;
                  if (var9 == null
                     && !playerEntity.isOnGround()
                     && var22 >= 6.0
                     && (var23 = this.air.landing(this.tick)) != null
                     && playerEntity.getY() - var23.at().y >= 3.0) {
                     var9 = var23.at();
                  }
               }

               if (var9 != null && this.tick >= this.searchAfter) {
                  Rotation var24 = PearlFlight.toward(clientPlayerEntity, var9, 0.8, 3.0);
                  if (var24 == null) {
                     this.searchAfter = this.tick + 10;
                     return false;
                  } else {
                     this.startThrow(CrystalBotModule.Throw.CHASE, var24, var9);
                     return this.keepThrowing(clientPlayerEntity, playerEntity);
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

   private boolean getDown(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      double var5 = clientPlayerEntity.getY() - playerEntity.getY();
      double var7 = Math.hypot(playerEntity.getX() - clientPlayerEntity.getX(), playerEntity.getZ() - clientPlayerEntity.getZ());
      if (!(var5 < 3.0) && !(var7 > 8.0) && this.stance != CrystalBotModule.Stance.RUN && !this.critical(clientPlayerEntity)) {
         if (this.belowSince < 0) {
            this.belowSince = this.tick;
         }

         if (this.tick - this.belowSince >= 20 && clientPlayerEntity.isOnGround() && playerEntity.isOnGround()) {
            if (this.digAnchor != null && !clientWorld.getBlockState(this.digAnchor).isOf(Blocks.RESPAWN_ANCHOR)) {
               if (this.digPlaced) {
                  this.digBlasts++;
                  this.digWalkUntil = this.tick + 40;
               }

               this.digAnchor = null;
            }

            if (this.tick >= this.digWalkUntil && !openDrop(clientWorld, clientPlayerEntity, playerEntity)) {
               boolean var4 = var5 <= 6.0
                  && this.digBlasts < 2
                  && this.tick >= this.digAfter
                  && this.anchors.get()
                  && clientWorld.getRegistryKey() != World.NETHER
                  && BotBody.hotbarSlot(clientPlayerEntity, ANCHOR) >= 0
                  && BotBody.hotbarSlot(clientPlayerEntity, GLOWSTONE) >= 0;
               return var4 && this.blastDown(clientWorld, clientPlayerEntity, playerEntity) ? true : this.pearlDown(clientPlayerEntity, playerEntity);
            } else {
               this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
               this.body.dropToward(this.mc, clientPlayerEntity, playerEntity.getEntityPos());
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

   private boolean blastDown(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      if (this.digAnchor == null) {
         this.digAnchor = digCell(clientWorld, clientPlayerEntity, playerEntity);
         if (this.digAnchor == null) {
            this.digAfter = this.tick + 100;
            return false;
         }

         this.digPlaced = false;
         this.digGuards = 0;
         this.digUntil = this.tick + 80;
         if (!this.digAnnounced) {
            ModuleChat.send(this, ModuleChat.body().text("Blasting down to ").name(playerEntity.getName().getString()).text("."));
         }

         this.digAnnounced = true;
      }

      if (this.tick > this.digUntil) {
         this.digBlasts = 2;
         this.digAnchor = null;
         return false;
      } else {
         this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
         BlockState var5 = clientWorld.getBlockState(this.digAnchor);
         if (!var5.isOf(Blocks.RESPAWN_ANCHOR)) {
            AnchorGuard.Spot var10 = var5.isReplaceable()
               ? AnchorGuard.supportFor(clientWorld, clientPlayerEntity, clientPlayerEntity.getEyePos(), this.digAnchor)
               : null;
            if (var10 == null) {
               this.digAnchor = null;
               this.digAfter = this.tick + 100;
               return false;
            } else {
               if (this.placeAt(clientWorld, clientPlayerEntity, var10, BotBody.hotbarSlot(clientPlayerEntity, ANCHOR)) == 1) {
                  this.digPlaced = true;
               }

               return true;
            }
         } else if (var5.get(RespawnAnchorBlock.CHARGES) <= 0) {
            return this.clickDigAnchor(clientWorld, clientPlayerEntity, BotBody.hotbarSlot(clientPlayerEntity, GLOWSTONE));
         } else {
            Vec3d var6 = AnchorGuard.anchorAim(clientWorld, clientPlayerEntity, this.digAnchor);
            if (var6 == null) {
               this.digAnchor = null;
               return false;
            } else {
               int var7 = guardBlockSlot(clientPlayerEntity);
               AnchorGuard.Spot var4;
               if (this.digGuards < 2
                  && var7 >= 0
                  && (var4 = AnchorGuard.find(clientWorld, clientPlayerEntity, this.digAnchor, var6)) != null
                  && !var4.standing()) {
                  if (this.placeAt(clientWorld, clientPlayerEntity, var4, var7) == 1) {
                     this.digGuards++;
                  }

                  return true;
               } else {
                  float var8 = CrystalDamage.estimate(Vec3d.ofCenter(this.digAnchor), clientPlayerEntity, 5.0F, this.digAnchor);
                  int var9 = AnchorGuard.detonatorRoute(clientPlayerEntity.getInventory(), clientPlayerEntity.getMainHandStack());
                  if (!(var8 > Math.min(this.selfCapNow, 10.0F)) && !CrystalDamage.endangers(clientPlayerEntity, var8) && var9 != -1) {
                     return this.clickDigAnchor(clientWorld, clientPlayerEntity, var9 == -2 ? clientPlayerEntity.getInventory().getSelectedSlot() : var9);
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

   private static BlockPos digCell(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      Vec3d var3 = clientPlayerEntity.getEyePos();
      double var4 = clientPlayerEntity.getBlockInteractionRange() - 0.5;
      BlockPos var6 = clientPlayerEntity.getBlockPos();
      ArrayList var7 = new ArrayList();

      for (BlockPos var9 : BlockPos.iterate(var6.add(-4, -2, -4), var6.add(4, 0, 4))) {
         if (!(var3.distanceTo(Vec3d.ofCenter(var9)) > var4)) {
            BlockPos var11 = var9.down();
            Box var10;
            if (clientWorld.getBlockState(var9).isReplaceable()
               && clientWorld.getFluidState(var9).isEmpty()
               && clientWorld.getBlockState(var11).isSideSolidFullSquare(clientWorld, var11, Direction.UP)
               && !(var10 = new Box(var9)).intersects(clientPlayerEntity.getBoundingBox())
               && !var10.intersects(playerEntity.getBoundingBox())) {
               var7.add(var9.toImmutable());
            }
         }
      }

      var7.sort(Comparator.comparingDouble(blockPos -> Vec3d.ofCenter(blockPos).squaredDistanceTo(playerEntity.getEntityPos())));
      BlockPos var12 = null;

      for (BlockPos var14 : var7.subList(0, Math.min(var7.size(), 8))) {
         if (AnchorGuard.supportFor(clientWorld, clientPlayerEntity, var3, var14) != null) {
            if (var12 == null) {
               var12 = var14;
            }

            if (AnchorGuard.find(clientWorld, clientPlayerEntity, var14, TargetingUtils.closestPoint(var3, new Box(var14).contract(0.1))) != null) {
               return var14;
            }
         }
      }

      return var12;
   }

   private static boolean openDrop(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      Vec3d var3 = playerEntity.getEntityPos().subtract(clientPlayerEntity.getEntityPos()).multiply(1.0, 0.0, 1.0);
      if (var3.lengthSquared() < 0.25) {
         return false;
      } else {
         Vec3d var4 = clientPlayerEntity.getEntityPos().add(var3.normalize().multiply(0.8));
         BlockPos var5 = BlockPos.ofFloored(var4.x, clientPlayerEntity.getY() + 0.1, var4.z);
         int var6 = MathHelper.floor(playerEntity.getY() + 0.01);

         for (BlockPos var7 = var5; var7.getY() >= var6 - 1; var7 = var7.down()) {
            BlockState var8 = clientWorld.getBlockState(var7);
            if (var8.isOf(Blocks.LAVA) || var8.isIn(BlockTags.FIRE)) {
               return false;
            }

            if (!var8.getCollisionShape(clientWorld, var7).isEmpty()) {
               return var7.getY() < clientPlayerEntity.getY() - 1.5 && var7.getY() <= var6 + 1;
            }
         }

         return false;
      }
   }

   private boolean clickDigAnchor(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, int i) {
      if (i < 0) {
         this.digAnchor = null;
         return false;
      } else if (!this.hands()) {
         return true;
      } else {
         BlockHitResult var4 = AnchorGuard.anchorHit(clientWorld, clientPlayerEntity, this.digAnchor);
         if (var4 == null) {
            Vec3d var5 = AnchorGuard.anchorAim(clientWorld, clientPlayerEntity, this.digAnchor);
            if (var5 == null) {
               this.digAnchor = null;
               return false;
            } else {
               ServerRotation.request(RotationUtils.fromTo(clientPlayerEntity.getEyePos(), var5), 6.0F);
               return true;
            }
         } else if (clientPlayerEntity.getInventory().getSelectedSlot() != i) {
            clientPlayerEntity.getInventory().setSelectedSlot(i);
            return true;
         } else {
            if (MeleeAttacks.handsFree(this.mc, clientPlayerEntity)) {
               AnchorGuard.click(this.mc, clientPlayerEntity, var4);
            }

            return true;
         }
      }
   }

   private boolean pearlDown(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      if (this.chase.get() && this.pearlReady(clientPlayerEntity) && this.tick >= this.searchAfter) {
         Vec3d var3 = playerEntity.getEntityPos();
         Rotation var4 = PearlFlight.toward(clientPlayerEntity, var3, 0.8, 3.0);
         if (var4 == null) {
            this.searchAfter = this.tick + 10;
            return false;
         } else {
            this.startThrow(CrystalBotModule.Throw.CHASE, var4, var3);
            return this.keepThrowing(clientPlayerEntity, playerEntity);
         }
      } else {
         return false;
      }
   }

   private static int feetY(ClientPlayerEntity clientPlayerEntity) {
      return MathHelper.floor(clientPlayerEntity.getY() + 0.01);
   }

   private boolean wantsLower(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, double d) {
      return this.stance != CrystalBotModule.Stance.RUN
         && clientPlayerEntity.isOnGround()
         && playerEntity.isOnGround()
         && !Combo.inHole(playerEntity)
         && d <= 5.5
         && feetY(clientPlayerEntity) == MathHelper.floor(playerEntity.getY() + 0.01)
         && !this.justLaunched();
   }

   private boolean justLaunched() {
      return this.launching || this.launchedAt >= 0 || this.tick <= this.followUntil + 20;
   }

   private boolean stayLow(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, double d) {
      if (this.stance != CrystalBotModule.Stance.RUN
         && clientPlayerEntity.isOnGround()
         && playerEntity.isOnGround()
         && !Combo.inHole(playerEntity)
         && !(d > 5.5)) {
         if (feetY(clientPlayerEntity) < MathHelper.floor(playerEntity.getY() + 0.01)) {
            return true;
         } else if (!this.wantsLower(clientPlayerEntity, playerEntity, d)) {
            return false;
         } else {
            BlockPos var6 = lowerStep(clientWorld, clientPlayerEntity, playerEntity, feetY(clientPlayerEntity) - 1);
            if (var6 == null) {
               return false;
            } else {
               this.body.toward(this.mc, clientPlayerEntity, Vec3d.ofBottomCenter(var6));
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private boolean digDown(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, double d) {
      if (this.wantsLower(clientPlayerEntity, playerEntity, d) && this.tick >= this.stepDigAfter) {
         if (lowerStep(clientWorld, clientPlayerEntity, playerEntity, feetY(clientPlayerEntity) - 1) != null) {
            return false;
         } else {
            BlockPos var7 = clientPlayerEntity.getBlockPos().down();
            BlockState var8 = clientWorld.getBlockState(var7);
            if (BotBody.hotbarSlot(clientPlayerEntity, OBSIDIAN) < 0 && !Crystals.isBase(var8)) {
               return false;
            } else {
               BlockPos var9 = var7.down();
               boolean var6 = !var8.hasBlockEntity()
                  && digTicks(clientWorld, clientPlayerEntity, var7) <= 50
                  && !clientWorld.getBlockState(var9).getCollisionShape(clientWorld, var9).isEmpty()
                  && !nearLava(clientWorld, var7);
               if (var6 && this.mine(clientWorld, clientPlayerEntity, var7)) {
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

   private boolean digBase(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, double d) {
      int var6 = MathHelper.floor(playerEntity.getY() + 0.01);
      if (this.stance != CrystalBotModule.Stance.RUN
         && clientPlayerEntity.isOnGround()
         && playerEntity.isOnGround()
         && !Combo.inHole(playerEntity)
         && !(d > 5.5)
         && feetY(clientPlayerEntity) == var6 - 1
         && this.tick >= this.baseDigAfter
         && BotBody.hotbarSlot(clientPlayerEntity, OBSIDIAN) >= 0
         && !this.justLaunched()) {
         Vec3d var7 = clientPlayerEntity.getEyePos();
         BlockPos var8 = playerEntity.getBlockPos();
         BlockPos var9 = null;
         double var10 = 1.7976931348623157E308;

         for (BlockPos var13 : BlockPos.iterate(var8.add(-2, -1, -2), var8.add(2, -1, 2))) {
            BlockState var17 = clientWorld.getBlockState(var13);
            if (Crystals.isBase(var17) && Crystals.canHost(clientWorld, var13, null)) {
               return false;
            }

            Vec3d var14;
            double var15;
            if ((var13.getX() != var8.getX() || var13.getZ() != var8.getZ())
               && Math.max(Math.abs(var13.getX() - clientPlayerEntity.getBlockX()), Math.abs(var13.getZ() - clientPlayerEntity.getBlockZ())) >= 2
               && !var17.isReplaceable()
               && !var17.hasBlockEntity()
               && clientWorld.isAir(var13.up())
               && !((var15 = (var14 = Vec3d.ofCenter(var13).add(0.0, 0.5, 0.0)).distanceTo(playerEntity.getEntityPos())) > 2.5)
               && !(var7.distanceTo(var14) > clientPlayerEntity.getBlockInteractionRange() - 0.5)
               && digTicks(clientWorld, clientPlayerEntity, var13) <= 25
               && !(var15 >= var10)) {
               var9 = var13.toImmutable();
               var10 = var15;
            }
         }

         if (var9 != null && this.mine(clientWorld, clientPlayerEntity, var9)) {
            return true;
         } else {
            this.baseDigAfter = this.tick + 100;
            return false;
         }
      } else {
         return false;
      }
   }

   private static BlockPos lowerStep(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, int i) {
      BlockPos var4 = clientPlayerEntity.getBlockPos();
      BlockPos var5 = null;
      double var6 = 1.7976931348623157E308;

      for (BlockPos var9 : BlockPos.iterate(var4.add(-2, -1, -2), var4.add(2, -1, 2))) {
         BlockPos var12 = StandPicker.standable(clientWorld, var9);
         if (var12 != null && var12.getY() == i && var12.equals(var9)) {
            Vec3d var13 = Vec3d.ofBottomCenter(var12);
            double var14 = Math.hypot(var13.x - playerEntity.getX(), var13.z - playerEntity.getZ());
            double var10;
            if (!(var14 < 2.4)
               && !(var14 > 4.5)
               && walkable(clientWorld, clientPlayerEntity.getEntityPos(), var13)
               && (var10 = var12.getSquaredDistance(var4)) < var6) {
               var5 = var12.toImmutable();
               var6 = var10;
            }
         }
      }

      return var5;
   }

   private static int digTicks(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      BlockState var5 = clientWorld.getBlockState(blockPos);
      float var6 = var5.getHardness(clientWorld, blockPos);
      if (var6 < 0.0F) {
         return 2147483647;
      } else if (var6 == 0.0F) {
         return 1;
      } else {
         int var7 = toolSlot(clientPlayerEntity.getInventory(), var5);
         ItemStack var8 = var7 < 0 ? ItemStack.EMPTY : clientPlayerEntity.getInventory().getStack(var7);
         float var9 = var8.getMiningSpeedMultiplier(var5);
         int var4;
         if (var9 > 1.0F
            && (
                  var4 = clientPlayerEntity.getRegistryManager()
                     .getOrThrow(RegistryKeys.ENCHANTMENT)
                     .getOptional(Enchantments.EFFICIENCY)
                     .map(reference -> EnchantmentHelper.getLevel(reference, var8))
                     .orElse(0)
               )
               > 0) {
            var9 += var4 * var4 + 1;
         }

         boolean var10 = var8.isSuitableFor(var5) || !var5.isToolRequired();
         return MathHelper.ceil(var6 * (var10 ? 30.0F : 100.0F) / var9);
      }
   }

   private static boolean nearLava(ClientWorld clientWorld, BlockPos blockPos) {
      for (Direction var5 : Direction.values()) {
         if (clientWorld.getFluidState(blockPos.offset(var5)).isIn(FluidTags.LAVA)) {
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

   private boolean startLaunch(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, RivalBook.Rival rival) {
      if (clientPlayerEntity.age - this.diggingAt <= 1) {
         return false;
      } else if (this.tick >= this.nextLaunch
         && !this.pause.slowServer()
         && (this.stance == CrystalBotModule.Stance.PRESS || this.stance == CrystalBotModule.Stance.HOLD)
         && (rival == null || !rival.shrugsOffLaunches())) {
         if (playerEntity.isOnGround() && !Combo.inHole(playerEntity) && (!playerEntity.isBlocking() || axeInHotbar(clientPlayerEntity))) {
            if (!this.mc.world.getNonSpectatingEntities(EndCrystalEntity.class, playerEntity.getBoundingBox().expand(3.0)).isEmpty()) {
               return false;
            } else {
               double var5 = reach(clientPlayerEntity, playerEntity);
               if (weaponSlot(clientPlayerEntity, playerEntity.isBlocking()) >= 0 && !(var5 > 3.9)) {
                  boolean var4 = MeleeAttacks.isMeleeWeapon(clientPlayerEntity.getMainHandStack()) && MeleeAttacks.charge(clientPlayerEntity) >= 0.5F;
                  if (!var4) {
                     int var8 = rival != null ? rival.opener(var5) : (this.tick - this.hurtAt >= 20 ? 0 : 1);
                     if (this.openerArm < 0) {
                        this.judgeOpener(var8, var5, playerEntity);
                     }

                     if (var8 == 1) {
                        this.nextLaunch = this.tick + 40;
                        return false;
                     }
                  }

                  this.launching = true;
                  this.launchUntil = this.tick + 16;
                  return this.launch(clientPlayerEntity, playerEntity);
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

   private void judgeOpener(int i, double d, PlayerEntity playerEntity) {
      this.openerArm = i;
      this.openerReach = d;
      this.openerAt = this.tick;
      this.openerHealth = CrystalDamage.health(playerEntity);
      this.openerPops = 0;
   }

   private boolean followUp(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      AirTrack.Landing var5 = this.air.landing(this.tick);
      Vec3d var4 = var5 != null ? var5.at() : playerEntity.getEntityPos();
      if (this.anchorUnder(clientWorld, clientPlayerEntity, playerEntity, var4)) {
         return true;
      } else {
         int var7 = BotBody.hotbarSlot(clientPlayerEntity, OBSIDIAN);
         if (var7 < 0) {
            this.followLaid = true;
            return false;
         } else {
            BlockPos var8 = BlockPos.ofFloored(var4.x, var4.y + 0.2, var4.z);
            CrystalDamage.Victim var9 = new CrystalDamage.Victim(playerEntity, var4.subtract(playerEntity.getEntityPos()));
            CrystalDamage.Victim var10 = new CrystalDamage.Victim(clientPlayerEntity, Vec3d.ZERO);
            Vec3d var11 = clientPlayerEntity.getEyePos();
            AnchorGuard.Spot var12 = null;
            float var13 = 4.0F;

            for (BlockPos var15 : BlockPos.iterate(var8.add(-1, -1, -1), var8.add(1, 0, 1))) {
               if (var15.getX() != var8.getX() || var15.getZ() != var8.getZ()) {
                  BlockState var18 = clientWorld.getBlockState(var15);
                  Vec3d var19 = Crystals.explosionAt(var15);
                  if (Crystals.isBase(var18) && Crystals.canHost(clientWorld, var15, null)) {
                     if (var9.damage(var19, false) >= var13) {
                        this.followLaid = true;
                        return false;
                     }
                  } else {
                     BlockPos var17;
                     if (var18.isReplaceable()
                        && clientWorld.isAir(var15.up())
                        && clientWorld.isAir(var15.up(2))
                        && clientWorld.getBlockState(var17 = var15.down()).isSideSolidFullSquare(clientWorld, var17, Direction.UP)
                        && clientWorld.canPlace(Blocks.OBSIDIAN.getDefaultState(), var15, ShapeContext.absent())
                        && !(var11.distanceTo(Vec3d.ofCenter(var15)) > clientPlayerEntity.getBlockInteractionRange())) {
                        float var20 = var9.damage(var19, true);
                        float var21 = var10.damage(var19, true);
                        AnchorGuard.Spot var16;
                        if (!(var20 <= var13)
                           && !(var21 > this.selfCapNow)
                           && !CrystalDamage.endangers(clientPlayerEntity, var21)
                           && (var16 = AnchorGuard.supportFor(clientWorld, clientPlayerEntity, var11, var15.toImmutable())) != null) {
                           var12 = var16;
                           var13 = var20;
                        }
                     }
                  }
               }
            }

            this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
            if (var12 == null) {
               if (var11.distanceTo(var4) > 3.5) {
                  this.body.drive(this.mc, clientPlayerEntity, 1, 0.0, true, false);
               }

               return true;
            } else {
               if (this.placeAt(clientWorld, clientPlayerEntity, var12, var7) == 1) {
                  this.followLaid = true;
               }

               return true;
            }
         }
      }
   }

   private boolean anchorUnder(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, Vec3d vec3d) {
      if (this.anchors.get() && clientWorld.getRegistryKey() != World.NETHER && !playerEntity.isOnGround()) {
         int var5 = BotBody.hotbarSlot(clientPlayerEntity, itemStack -> itemStack.isOf(Items.RESPAWN_ANCHOR));
         if (var5 >= 0 && BotBody.hotbarSlot(clientPlayerEntity, itemStack -> itemStack.isOf(Items.GLOWSTONE)) >= 0) {
            BlockPos var6 = BlockPos.ofFloored(vec3d.x, vec3d.y + 0.2, vec3d.z);
            BlockPos var7 = var6.down();
            if (clientWorld.getBlockState(var6).isReplaceable() && clientWorld.getBlockState(var7).isSideSolidFullSquare(clientWorld, var7, Direction.UP)) {
               if (!playerEntity.getBoundingBox().intersects(new Box(var6)) && !clientPlayerEntity.getBoundingBox().intersects(new Box(var6))) {
                  Vec3d var8 = clientPlayerEntity.getEyePos();
                  if (var8.distanceTo(Vec3d.ofCenter(var6)) > clientPlayerEntity.getBlockInteractionRange()) {
                     return false;
                  } else {
                     Vec3d var9 = Vec3d.ofCenter(var6);
                     float var10 = CrystalDamage.estimate(var9, clientPlayerEntity, 5.0F, var6);
                     Vec3d var11 = new Vec3d(vec3d.x, var6.getY() + 1.0, vec3d.z);
                     float var12 = new CrystalDamage.Victim(playerEntity, var11.subtract(playerEntity.getEntityPos())).damage(var9, 5.0F, var6);
                     if (!(var12 < 6.0F) && !(var10 > Math.min(this.selfCapNow, 11.0F)) && !CrystalDamage.endangers(clientPlayerEntity, var10)) {
                        AnchorGuard.Spot var13 = AnchorGuard.supportFor(clientWorld, clientPlayerEntity, var8, var6);
                        if (var13 == null) {
                           return false;
                        } else {
                           this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
                           if (this.placeAt(clientWorld, clientPlayerEntity, var13, var5) == 1) {
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

   private boolean launch(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      if (this.tick > this.launchUntil
         || this.stance == CrystalBotModule.Stance.RUN
         || this.stance == CrystalBotModule.Stance.GUARD
         || reach(clientPlayerEntity, playerEntity) > 4.1
         || Combo.inHole(playerEntity)) {
         this.launching = false;
         this.nextLaunch = this.tick + 40;
         return false;
      } else if (!this.hands()) {
         return true;
      } else {
         int var3 = weaponSlot(clientPlayerEntity, playerEntity.isBlocking());
         if (var3 < 0) {
            this.launching = false;
            return false;
         } else {
            if (clientPlayerEntity.getInventory().getSelectedSlot() != var3) {
               clientPlayerEntity.getInventory().setSelectedSlot(var3);
            }

            this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
            float var4 = MeleeAttacks.charge(clientPlayerEntity);
            double var5 = reach(clientPlayerEntity, playerEntity);
            Rotation var7 = ServerRotation.sent();
            boolean var8 = clientPlayerEntity.isSprinting();
            if (var4 >= 0.95F && var5 <= 2.95 && this.strike(clientPlayerEntity, playerEntity)) {
               this.launching = false;
               this.nextLaunch = this.tick + 80;
               this.launchedAt = this.tick;
               this.launchY = playerEntity.getY();
               this.launchPeak = 0.0;
               this.followLaid = false;
               this.followUntil = this.tick + 12;
               this.air.kicked(clientPlayerEntity, playerEntity, var7 != null ? var7.yaw() : clientPlayerEntity.getYaw(), var8, this.tick);
               this.body.drive(this.mc, clientPlayerEntity, 0, 1.0, false, false);
               return true;
            } else {
               this.body.drive(this.mc, clientPlayerEntity, var5 > 2.9 ? 1 : 0, 0.0, var4 >= 0.85F, false);
               return true;
            }
         }
      }
   }

   private boolean strike(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      if (!this.pause.slowServer()
         && !MeleeAttacks.actedThisTick(clientPlayerEntity)
         && this.mc.attackCooldown <= 0
         && MeleeAttacks.isMeleeWeapon(clientPlayerEntity.getMainHandStack())) {
         Rotation var4 = ServerRotation.sent();
         if (var4 == null) {
            return false;
         } else {
            Vec3d var5 = clientPlayerEntity.getEyePos();
            double var6 = Math.min(2.95, clientPlayerEntity.getEntityInteractionRange());
            Box var8 = playerEntity.getBoundingBox();
            Vec3d var3 = var8.contains(var5) ? var5 : var8.raycast(var5, var5.add(Vec3d.fromPolar(var4.pitch(), var4.yaw()).multiply(var6))).orElse(null);
            if (var3 != null && MeleeAttacks.lineIsClear(clientPlayerEntity, var5, var3)) {
               if (!MeleeAttacks.attack(this.mc, new EntityHitResult(playerEntity, var3))) {
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

   private static double reach(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      Vec3d var2 = clientPlayerEntity.getEyePos();
      return var2.distanceTo(TargetingUtils.closestPoint(var2, playerEntity.getBoundingBox()));
   }

   private static boolean axeInHotbar(ClientPlayerEntity clientPlayerEntity) {
      return BotBody.hotbarSlot(clientPlayerEntity, itemStack -> itemStack.isIn(ItemTags.AXES)) >= 0;
   }

   private static int weaponSlot(ClientPlayerEntity clientPlayerEntity, boolean bl) {
      PlayerInventory var2 = clientPlayerEntity.getInventory();
      int var3 = -1;
      double var4 = 0.0;

      for (int var6 = 0; var6 < 9; var6++) {
         ItemStack var7 = var2.getStack(var6);
         if (MeleeAttacks.isMeleeWeapon(var7) && !var7.isOf(Items.MACE) && (!bl || var7.isIn(ItemTags.AXES))) {
            double var8 = MeleeAttacks.attackDamage(var7);
            if (var3 < 0 || var8 > var4) {
               var3 = var6;
               var4 = var8;
            }
         }
      }

      return var3;
   }

   private boolean breakHole(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, RivalBook.Rival rival) {
      AutoCityModule var7 = ModuleRegistry.get(AutoCityModule.class);
      if (var7 == null) {
         return false;
      } else if (var7.isEnabled()) {
         if (!this.cityStarted) {
            return false;
         } else {
            CombatMacroLock.release(this);
            this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
            return true;
         }
      } else {
         if (this.cityStarted) {
            this.cityStarted = false;
            this.breachAfter = this.tick + 100;
            this.progressAt = this.tick;
            if (Combo.inHole(playerEntity)) {
               this.cityFails++;
            }
         }

         if (this.stance != CrystalBotModule.Stance.RUN
            && this.tick >= this.breachAfter
            && Combo.inHole(playerEntity)
            && !(clientPlayerEntity.getHealth() + clientPlayerEntity.getAbsorptionAmount() < 8.0F)) {
            int var6 = rival != null && rival.campsInHoles() ? 15 : 40;
            if (this.tick - this.progressAt < var6) {
               return false;
            } else {
               boolean var5 = BotBody.hotbarSlot(clientPlayerEntity, itemStack -> itemStack.isIn(ItemTags.PICKAXES)) >= 0;
               if (this.cityFails >= 2 && var5 && this.roofOff(clientWorld, clientPlayerEntity, playerEntity)) {
                  return true;
               } else if (this.cityFails >= 2 || !var5) {
                  return this.camp(clientWorld, clientPlayerEntity, playerEntity);
               } else if (clientPlayerEntity.distanceTo(playerEntity) > 3.6) {
                  this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
                  this.body.keepDistance(this.mc, clientPlayerEntity, playerEntity, 1.6, 2.6, 3.4, false);
                  return true;
               } else {
                  var7.setEnabled(true);
                  if (!var7.isEnabled()) {
                     return false;
                  } else {
                     this.cityStarted = true;
                     ModuleChat.send(this, ModuleChat.body().text("Breaking ").name(playerEntity.getName().getString()).text("'s hole open."));
                     return true;
                  }
               }
            }
         } else {
            return false;
         }
      }
   }

   private boolean roofOff(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      if (this.anchors.get()
         && clientWorld.getRegistryKey() != World.NETHER
         && BotBody.hotbarSlot(clientPlayerEntity, itemStack -> itemStack.isOf(Items.RESPAWN_ANCHOR)) >= 0
         && BotBody.hotbarSlot(clientPlayerEntity, itemStack -> itemStack.isOf(Items.GLOWSTONE)) >= 0) {
         BlockPos var4 = playerEntity.getBlockPos().up(2);
         BlockState var5 = clientWorld.getBlockState(var4);
         if (var5.isReplaceable() || var5.getHardness(clientWorld, var4) < 0.0F) {
            return false;
         } else if (this.mine(clientWorld, clientPlayerEntity, var4)) {
            this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
            return true;
         } else {
            BlockPos var6 = roofView(clientWorld, clientPlayerEntity, var4);
            if (var6 == null) {
               return false;
            } else {
               this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
               this.body.toward(this.mc, clientPlayerEntity, Vec3d.ofBottomCenter(var6));
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean walkable(ClientWorld clientWorld, Vec3d vec3d, Vec3d vec3d2) {
      Vec3d var3 = vec3d2.subtract(vec3d);
      int var4 = Math.max(1, (int)Math.ceil(var3.horizontalLength() / 0.5));
      int var5 = MathHelper.floor(vec3d.y + 0.01);

      for (int var6 = 1; var6 <= var4; var6++) {
         Vec3d var7 = vec3d.add(var3.multiply((double)var6 / var4));
         BlockPos var8 = StandPicker.standable(clientWorld, BlockPos.ofFloored(var7.x, var5, var7.z));
         if (var8 == null || Math.abs(var8.getY() - var5) > 1) {
            return false;
         }

         var5 = var8.getY();
      }

      return true;
   }

   private static BlockPos roofView(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      BlockPos var3 = clientPlayerEntity.getBlockPos();
      BlockPos var4 = null;
      double var5 = 1.7976931348623157E308;
      double var7 = clientPlayerEntity.getBlockInteractionRange() - 0.2;

      for (BlockPos var10 : BlockPos.iterate(var3.add(-4, -1, -4), var3.add(4, 1, 4))) {
         BlockPos var11 = StandPicker.standable(clientWorld, var10);
         if (var11 != null && var11.equals(var10) && walkable(clientWorld, clientPlayerEntity.getEntityPos(), Vec3d.ofBottomCenter(var11))) {
            Vec3d var12 = Vec3d.ofBottomCenter(var11).add(0.0, clientPlayerEntity.getStandingEyeHeight(), 0.0);
            double var13 = var11.getSquaredDistance(var3);
            if (!(var13 >= var5)) {
               for (Direction var18 : Direction.values()) {
                  Vec3d var20 = Vec3d.ofCenter(blockPos).offset(var18, 0.5);
                  BlockHitResult var19;
                  if (!(var12.distanceTo(var20) > var7)
                     && (var19 = clientWorld.raycast(
                              new RaycastContext(var12, var20.offset(var18, -0.05), ShapeType.OUTLINE, FluidHandling.NONE, clientPlayerEntity)
                           ))
                           .getType()
                        == Type.BLOCK
                     && var19.getBlockPos().equals(blockPos)
                     && var19.getSide() == var18) {
                     var4 = var11.toImmutable();
                     var5 = var13;
                     break;
                  }
               }
            }
         }
      }

      return var4;
   }

   private boolean mine(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      BlockState var5 = clientWorld.getBlockState(blockPos);
      int var6 = toolSlot(clientPlayerEntity.getInventory(), var5);
      if (var6 < 0) {
         return false;
      } else {
         BlockHitResult var7 = visibleFace(clientWorld, clientPlayerEntity, blockPos);
         if (var7 == null) {
            return false;
         } else if (!this.hands()) {
            return true;
         } else {
            Rotation var8 = ServerRotation.sent();
            BlockHitResult var4 = var8 == null ? null : AnchorGuard.trace(clientWorld, clientPlayerEntity, var8);
            if (var4 == null || !var4.getBlockPos().equals(blockPos)) {
               ServerRotation.request(RotationUtils.fromTo(clientPlayerEntity.getEyePos(), var7.getPos()), 6.0F);
               return true;
            } else if (clientPlayerEntity.getInventory().getSelectedSlot() != var6) {
               clientPlayerEntity.getInventory().setSelectedSlot(var6);
               return true;
            } else {
               ServerRotation.request(var8);
               if (MeleeAttacks.actedThisTick(clientPlayerEntity)) {
                  return true;
               } else {
                  if (this.mc.interactionManager.updateBlockBreakingProgress(blockPos, var4.getSide())) {
                     clientPlayerEntity.swingHand(Hand.MAIN_HAND);
                  }

                  MeleeAttacks.noteAction(clientPlayerEntity);
                  this.diggingAt = clientPlayerEntity.age;
                  return true;
               }
            }
         }
      }
   }

   public static boolean digging() {
      CrystalBotModule var0 = ModuleRegistry.get(CrystalBotModule.class);
      ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
      return var0 != null && var0.isEnabled() && var1 != null && var1.age - var0.diggingAt <= 1;
   }

   private static int toolSlot(PlayerInventory playerInventory, BlockState blockState) {
      int var2 = -1;
      float var3 = 0.0F;

      for (int var4 = 0; var4 < 9; var4++) {
         ItemStack var6 = playerInventory.getStack(var4);
         boolean var5 = var6.isSuitableFor(blockState) || !blockState.isToolRequired();
         if (var5 && !(var6.getMiningSpeedMultiplier(blockState) <= var3)) {
            var2 = var4;
            var3 = var6.getMiningSpeedMultiplier(blockState);
         }
      }

      return var2;
   }

   private static BlockHitResult visibleFace(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      Vec3d var3 = clientPlayerEntity.getEyePos();
      BlockHitResult var4 = null;
      double var5 = clientPlayerEntity.getBlockInteractionRange() * clientPlayerEntity.getBlockInteractionRange();

      for (Direction var10 : Direction.values()) {
         Vec3d var12 = Vec3d.ofCenter(blockPos).offset(var10, 0.5);
         double var13 = var3.squaredDistanceTo(var12);
         BlockHitResult var11;
         if (!(var13 > var5)
            && (var11 = AnchorGuard.trace(clientWorld, clientPlayerEntity, RotationUtils.fromTo(var3, var12.offset(var10, -0.05)))) != null
            && var11.getBlockPos().equals(blockPos)
            && var11.getSide() == var10) {
            var4 = var11;
            var5 = var13;
         }
      }

      return var4;
   }

   private boolean camp(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      BlockPos var4 = playerEntity.getBlockPos();
      if (clientWorld.getNonSpectatingEntities(EndCrystalEntity.class, new Box(var4).expand(3.0, 1.0, 3.0)).size() >= 2) {
         return false;
      } else {
         Vec3d var5 = clientPlayerEntity.getEyePos();
         BlockPos var6 = null;
         float var7 = this.selfCapNow - 2.0F;
         double var8 = clientPlayerEntity.getEntityInteractionRange() - 0.1;

         for (BlockPos var11 : BlockPos.iterate(var4.add(-2, 0, -2), var4.add(2, 0, 2))) {
            float var12;
            BlockPos var13;
            if (Math.max(Math.abs(var11.getX() - var4.getX()), Math.abs(var11.getZ() - var4.getZ())) == 2
               && (var13 = var11.down().toImmutable()).getY() >= feetY(clientPlayerEntity)
               && Crystals.isBase(clientWorld.getBlockState(var13))
               && Crystals.canHost(clientWorld, var13, null)
               && !(var5.squaredDistanceTo(TargetingUtils.closestPoint(var5, Crystals.boxOn(var13))) > var8 * var8)
               && !((var12 = CrystalDamage.estimate(Crystals.explosionAt(var13), clientPlayerEntity)) > var7)
               && !CrystalDamage.endangers(clientPlayerEntity, var12)
               && topAim(clientWorld, clientPlayerEntity, var13) != null) {
               var6 = var13;
               var7 = var12;
            }
         }

         if (var6 == null) {
            return false;
         } else {
            this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
            this.placeCrystal(clientWorld, clientPlayerEntity, var6);
            return true;
         }
      }
   }

   private static Vec3d topAim(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      Vec3d var4 = clientPlayerEntity.getEyePos();

      for (double[] var8 : new double[][]{{0.5, 0.5}, {0.2, 0.2}, {0.8, 0.2}, {0.2, 0.8}, {0.8, 0.8}}) {
         Vec3d var10 = new Vec3d(blockPos.getX() + var8[0], blockPos.getY() + 1.0, blockPos.getZ() + var8[1]);
         BlockHitResult var9;
         if (!(var4.distanceTo(var10) > clientPlayerEntity.getBlockInteractionRange() - 0.1)
            && (var9 = AnchorGuard.trace(clientWorld, clientPlayerEntity, RotationUtils.fromTo(var4, var10))) != null
            && var9.getBlockPos().equals(blockPos)
            && var9.getSide() == Direction.UP) {
            return var10;
         }
      }

      return null;
   }

   private void placeCrystal(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      int var5 = BotBody.hotbarSlot(clientPlayerEntity, itemStack -> itemStack.isOf(Items.END_CRYSTAL));
      if (var5 >= 0 && this.hands()) {
         Rotation var6 = ServerRotation.sent();
         BlockHitResult var4 = var6 == null ? null : AnchorGuard.trace(clientWorld, clientPlayerEntity, var6);
         if (var4 == null || !var4.getBlockPos().equals(blockPos) || var4.getSide() != Direction.UP) {
            Vec3d var8 = topAim(clientWorld, clientPlayerEntity, blockPos);
            if (var8 != null) {
               ServerRotation.request(RotationUtils.fromTo(clientPlayerEntity.getEyePos(), var8), 6.0F);
            }
         } else if (clientPlayerEntity.getInventory().getSelectedSlot() != var5) {
            clientPlayerEntity.getInventory().setSelectedSlot(var5);
         } else if (MeleeAttacks.handsFree(this.mc, clientPlayerEntity)) {
            if (Crystals.rightClick(this.mc, clientPlayerEntity, var4)) {
               Crystals.notePlace(clientPlayerEntity, blockPos);
               MeleeAttacks.claimTick(this.mc);
            }

            ServerRotation.request(var6);
         }
      }
   }

   private boolean closeSpot(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      int var4 = guardBlockSlot(clientPlayerEntity);
      if (var4 < 0) {
         return false;
      } else {
         if (this.blocking == null && this.tick >= this.spotScanAt) {
            this.spotScanAt = this.tick + 5;
            this.blocking = dangerSpot(clientWorld, clientPlayerEntity, playerEntity);
         }

         if (this.blocking == null) {
            return false;
         } else if (clientWorld.getBlockState(this.blocking).isReplaceable()
            && clientWorld.canPlace(Blocks.OBSIDIAN.getDefaultState(), this.blocking, ShapeContext.absent())) {
            AnchorGuard.Spot var5 = AnchorGuard.supportFor(clientWorld, clientPlayerEntity, clientPlayerEntity.getEyePos(), this.blocking);
            if (var5 == null) {
               this.blocking = null;
               return false;
            } else {
               if (this.placeAt(clientWorld, clientPlayerEntity, var5, var4) == 1) {
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

   private static BlockPos dangerSpot(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      BlockPos var3 = clientPlayerEntity.getBlockPos();
      Vec3d var4 = playerEntity.getEyePos();
      double var5 = Math.max(4.6, clientPlayerEntity.getBlockInteractionRange() + 0.1);
      BlockPos var7 = null;
      float var8 = 0.0F;
      float var9 = 0.0F;

      for (BlockPos var11 : BlockPos.iterate(var3.add(-4, -2, -4), var3.add(4, 1, 4))) {
         Vec3d var12;
         float var13;
         if (Crystals.isBase(clientWorld.getBlockState(var11))
            && Crystals.canHost(clientWorld, var11, null)
            && !(var4.distanceTo(new Vec3d(var11.getX() + 0.5, var11.getY() + 1.0, var11.getZ() + 0.5)) > var5)
            && !((var13 = CrystalDamage.estimate(var12 = Crystals.explosionAt(var11), clientPlayerEntity)) <= var9)
            && !(CrystalDamage.estimate(var12, playerEntity) >= var13 + 2.0F)) {
            if (var13 > var8) {
               var9 = var8;
               var8 = var13;
               var7 = var11.up().toImmutable();
            } else {
               var9 = var13;
            }
         }
      }

      return var8 >= 8.0F && var9 <= var8 / 2.0F ? var7 : null;
   }

   private int placeAt(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, AnchorGuard.Spot spot, int i) {
      if (!this.hands()) {
         return 0;
      } else {
         Rotation var6 = ServerRotation.sent();
         BlockHitResult var5 = var6 == null ? null : AnchorGuard.hitAlong(clientWorld, clientPlayerEntity, var6, spot);
         if (var5 == null) {
            ServerRotation.request(RotationUtils.fromTo(clientPlayerEntity.getEyePos(), spot.aim()), 6.0F);
            return 0;
         } else if (clientPlayerEntity.getInventory().getSelectedSlot() != i) {
            clientPlayerEntity.getInventory().setSelectedSlot(i);
            return 0;
         } else if (!MeleeAttacks.handsFree(this.mc, clientPlayerEntity)) {
            return 0;
         } else {
            AnchorGuard.click(this.mc, clientPlayerEntity, var5);
            ServerRotation.request(var6);
            return 1;
         }
      }
   }

   private boolean defuse(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      if (this.tick - this.defuseAt >= 10 && clientWorld.getRegistryKey() != World.NETHER) {
         BlockPos var3 = clientPlayerEntity.getBlockPos();

         for (BlockPos var5 : BlockPos.iterate(var3.add(-5, -2, -5), var3.add(5, 3, 5))) {
            BlockState var8 = clientWorld.getBlockState(var5);
            BlockPos var6;
            Vec3d var7;
            if (var8.isOf(Blocks.RESPAWN_ANCHOR)
               && var8.get(RespawnAnchorBlock.CHARGES) > 0
               && clientPlayerEntity.canInteractWithBlockAt(var5, -0.1)
               && !var5.equals(this.digAnchor)
               && !(
                  CrystalDamage.estimate(var7 = Vec3d.ofCenter(var6 = var5.toImmutable()), clientPlayerEntity, 5.0F, var6)
                     > Math.max(1.0F, 0.15F * clientPlayerEntity.getHealth())
               )) {
               boolean var9 = false;

               for (PlayerEntity var11 : enemies(clientWorld, clientPlayerEntity, 16.0)) {
                  var9 |= var11.getEyePos().distanceTo(var7) <= 5.5;
               }

               if (var9) {
                  PlayerInventory var14 = clientPlayerEntity.getInventory();
                  int var15 = AnchorGuard.detonatorRoute(var14, clientPlayerEntity.getMainHandStack());
                  if (var15 == -1 || !this.hands()) {
                     return false;
                  }

                  BlockHitResult var12 = AnchorGuard.anchorHit(clientWorld, clientPlayerEntity, var6);
                  if (var12 != null) {
                     if (var15 != -2) {
                        var14.setSelectedSlot(var15);
                        return true;
                     }

                     if (!MeleeAttacks.handsFree(this.mc, clientPlayerEntity)) {
                        return true;
                     }

                     AnchorGuard.click(this.mc, clientPlayerEntity, var12);
                     this.defuseAt = this.tick;
                     return true;
                  }

                  Vec3d var13 = AnchorGuard.anchorAim(clientWorld, clientPlayerEntity, var6);
                  if (var13 != null) {
                     ServerRotation.request(RotationUtils.fromTo(clientPlayerEntity.getEyePos(), var13), 6.0F);
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

   private boolean guard(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      float var4 = clientPlayerEntity.getHealth() + clientPlayerEntity.getAbsorptionAmount();
      BlockPos var5 = null;
      float var6 = 0.0F;
      BlockPos var7 = clientPlayerEntity.getBlockPos();

      for (BlockPos var9 : BlockPos.iterate(var7.add(-5, -2, -5), var7.add(5, 3, 5))) {
         Vec3d var10;
         float var11;
         if (clientWorld.getBlockState(var9).isOf(Blocks.RESPAWN_ANCHOR)
            && !var9.equals(this.digAnchor)
            && (
               !((var11 = CrystalDamage.estimate(var10 = Vec3d.ofCenter(var9), clientPlayerEntity, 5.0F, var9.toImmutable())) < Math.max(4.0F, var4 * 0.3F))
                  || !(var11 < var4 - 1.0F)
            )
            && !(playerEntity.getEyePos().distanceTo(var10) > 5.5)
            && !(var11 <= var6)) {
            var6 = var11;
            var5 = var9.toImmutable();
         }
      }

      if (var5 == null) {
         return false;
      } else {
         AnchorGuard.Spot var12 = AnchorGuard.find(clientWorld, clientPlayerEntity, var5, clientPlayerEntity.getEyePos());
         if (var12 != null && var12.standing()) {
            return false;
         } else {
            int var13 = guardBlockSlot(clientPlayerEntity);
            if (var12 != null && var13 >= 0 && this.tick - this.shieldAt >= 20) {
               this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
               if (this.placeAt(clientWorld, clientPlayerEntity, var12, var13) == 1) {
                  this.shieldAt = this.tick;
               }

               return true;
            } else {
               this.body.face(this.mc, clientPlayerEntity, playerEntity, 1.0F);
               this.body.away(this.mc, clientPlayerEntity, Vec3d.ofCenter(var5));
               return true;
            }
         }
      }
   }

   private static int guardBlockSlot(ClientPlayerEntity clientPlayerEntity) {
      int var1 = BotBody.hotbarSlot(clientPlayerEntity, OBSIDIAN);
      return var1 >= 0 ? var1 : BotBody.hotbarSlot(clientPlayerEntity, AnchorGuard::isGuardBlock);
   }

   private boolean cover(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, double d) {
      boolean var7 = this.boxingAt != null && this.boxingAt.equals(clientPlayerEntity.getBlockPos()) && this.tick < this.boxingUntil;
      boolean var6 = clientPlayerEntity.getHealth() + clientPlayerEntity.getAbsorptionAmount() <= this.retreatAt() + 4.0F
         || this.stance == CrystalBotModule.Stance.GUARD;
      if (this.surround.get() && !var7 && var6 && clientPlayerEntity.isOnGround()) {
         this.boxingAt = clientPlayerEntity.getBlockPos();
         this.boxingUntil = this.tick + 60;
         var7 = true;
      }

      if (!var7) {
         this.boxingAt = null;
      }

      return this.surround.get() && var7 && clientPlayerEntity.isOnGround() && this.boxIn(clientWorld, clientPlayerEntity);
   }

   private boolean boxIn(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      List var3 = openAroundFeet(clientWorld, clientPlayerEntity);
      if (!var3.isEmpty() && BotBody.hotbarSlot(clientPlayerEntity, OBSIDIAN) >= 0) {
         if (!this.hands()) {
            return true;
         } else {
            for (BlockPos var5 : var3) {
               BlockHitResult var6 = AutoTrapModule.support(clientWorld, clientPlayerEntity, var5);
               if (var6 != null && (!var5.equals(this.boxSkipped) || this.tick >= this.boxSkippedUntil)) {
                  BlockHitResult var7 = AutoTrapModule.seenByServer(clientWorld, clientPlayerEntity, var6);
                  if (var7 != null) {
                     int var8 = BotBody.hotbarSlot(clientPlayerEntity, OBSIDIAN);
                     if (clientPlayerEntity.getInventory().getSelectedSlot() != var8) {
                        clientPlayerEntity.getInventory().setSelectedSlot(var8);
                        return true;
                     }

                     if (!MeleeAttacks.handsFree(this.mc, clientPlayerEntity)) {
                        return true;
                     }

                     ActionResult var9 = this.mc.interactionManager.interactBlock(clientPlayerEntity, Hand.MAIN_HAND, var7);
                     if (var9.isAccepted()) {
                        clientPlayerEntity.swingHand(Hand.MAIN_HAND);
                        MeleeAttacks.noteAction(clientPlayerEntity);
                        ServerRotation.request(ServerRotation.sent());
                     }

                     return true;
                  }

                  this.boxAimTicks = var5.equals(this.boxAiming) ? this.boxAimTicks + 1 : 0;
                  this.boxAiming = var5;
                  if (this.boxAimTicks < 10) {
                     ServerRotation.request(RotationUtils.fromTo(clientPlayerEntity.getEyePos(), var6.getPos()), 6.0F);
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

   private static List<BlockPos> openAroundFeet(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      ArrayList var2 = new ArrayList();

      for (BlockPos var4 : ring(clientPlayerEntity)) {
         BlockState var5 = clientWorld.getBlockState(var4);
         if (var5.isReplaceable()
            && !(var4.toCenterPos().distanceTo(clientPlayerEntity.getEyePos()) > clientPlayerEntity.getBlockInteractionRange())
            && clientWorld.canPlace(Blocks.OBSIDIAN.getDefaultState(), var4, ShapeContext.absent())) {
            var2.add(var4);
         }
      }

      return var2;
   }

   private static boolean surrounded(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      for (BlockPos var3 : ring(clientPlayerEntity)) {
         if (clientWorld.getBlockState(var3).getCollisionShape(clientWorld, var3).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private static Set<BlockPos> ring(ClientPlayerEntity clientPlayerEntity) {
      Box var1 = clientPlayerEntity.getBoundingBox();
      int var2 = MathHelper.floor(var1.minY + 0.01);
      LinkedHashSet var3 = new LinkedHashSet();

      for (int var4 = MathHelper.floor(var1.minX); var4 <= MathHelper.floor(var1.maxX - 1.0E-4); var4++) {
         for (int var5 = MathHelper.floor(var1.minZ); var5 <= MathHelper.floor(var1.maxZ - 1.0E-4); var5++) {
            var3.add(BlockPos.asLong(var4, 0, var5));
         }
      }

      LinkedHashSet var12 = new LinkedHashSet();

      for (long var6 : var3) {
         BlockPos var8 = BlockPos.fromLong(var6).withY(var2);

         for (Direction var10 : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            BlockPos var11 = var8.offset(var10);
            if (!var3.contains(BlockPos.asLong(var11.getX(), 0, var11.getZ()))) {
               var12.add(var11);
            }
         }
      }

      return var12;
   }

   private static boolean armorLow(LivingEntity livingEntity) {
      for (EquipmentSlot var2 : EquipmentSlot.VALUES) {
         ItemStack var3;
         if (var2.isArmorSlot()
            && (var3 = livingEntity.getEquippedStack(var2)).isDamageable()
            && var3.getMaxDamage() - var3.getDamage() < var3.getMaxDamage() * 0.3F) {
            return true;
         }
      }

      return false;
   }

   private boolean throwXp(ClientPlayerEntity clientPlayerEntity) {
      if (BotBody.hotbarSlot(clientPlayerEntity, XP) < 0) {
         return false;
      } else {
         this.mending = true;
         Aim.turn(this.mc, clientPlayerEntity.getYaw(), 86.0F, 22.0F);
         if (clientPlayerEntity.getPitch() > 70.0F) {
            this.body.use(this.mc, clientPlayerEntity, XP);
         }

         return true;
      }
   }

   private boolean mendIdle(ClientPlayerEntity clientPlayerEntity) {
      if (this.mendArmor.get() && (armorLow(clientPlayerEntity) || this.mending) && hasDamagedArmor(clientPlayerEntity)) {
         CombatMacroLock.take(this);
         return this.throwXp(clientPlayerEntity);
      } else {
         return false;
      }
   }

   private static boolean hasDamagedArmor(ClientPlayerEntity clientPlayerEntity) {
      for (EquipmentSlot var2 : EquipmentSlot.VALUES) {
         if (var2.isArmorSlot() && clientPlayerEntity.getEquippedStack(var2).isDamaged()) {
            return true;
         }
      }

      return false;
   }

   private static int count(ClientPlayerEntity clientPlayerEntity, Predicate<ItemStack> predicate) {
      int var2 = 0;
      PlayerInventory var3 = clientPlayerEntity.getInventory();

      for (int var4 = 0; var4 < 36; var4++) {
         if (predicate.test(var3.getStack(var4))) {
            var2 += var3.getStack(var4).getCount();
         }
      }

      return var2 + (predicate.test(clientPlayerEntity.getOffHandStack()) ? clientPlayerEntity.getOffHandStack().getCount() : 0);
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

      private Stance(float f, float g, float h, double d) {
         this.minDamage = f;
         this.selfCap = g;
         this.anchorMin = h;
         this.cover = d;
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
