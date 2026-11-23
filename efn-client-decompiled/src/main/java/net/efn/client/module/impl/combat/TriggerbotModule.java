package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.InputControl;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class TriggerbotModule extends ClientModule {
   private static final String CRITS_OFF = "Off";
   private static final String CRITS_SMART = "Smart";
   private static final String CRITS_ONLY = "Only";
   private static final int TRACK_GRACE_TICKS = 4;
   private static final int SMART_MAX_WAIT_TICKS = 12;
   private final NumberSetting reaction = this.addSetting("reaction", new NumberSetting("Reaction", 3.0F, 0.0F, 10.0F).step(1.0F));
   private final ModeSetting crits = this.addSetting("crits", new ModeSetting("Crits", "Smart", "Off", "Smart", "Only"));
   private final BooleanSetting holdAttack = this.addSetting("hold-attack", new BooleanSetting("Hold Attack", false));
   private final BooleanSetting players = this.addSetting("players", new BooleanSetting("Players", true));
   private final BooleanSetting hostiles = this.addSetting("hostiles", new BooleanSetting("Hostiles", false));
   private final BooleanSetting invisible = this.addSetting("invisible", new BooleanSetting("Invisible", false));
   private final BooleanSetting weaponsOnly = this.addSetting("weapons-only", new BooleanSetting("Weapons Only", true));
   private int ticks;
   private int trackedId = -1;
   private int seenTick;
   private int reactionDoneTick;
   private int chargedTick = -1;
   private int chargePause;
   private int critWait;
   private int critWaitTick = -1;

   public TriggerbotModule() {
      super("triggerbot", "Triggerbot", "Hits the entity under the crosshair once the swing is charged, with a reaction delay.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onEnable() {
      this.reset();
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.ticks = 0;
      this.trackedId = -1;
      this.seenTick = 0;
      this.reactionDoneTick = 0;
      this.chargedTick = -1;
      this.chargePause = 0;
      this.critWait = 0;
      this.critWaitTick = -1;
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onTick(TickEvent var1) {
      this.ticks++;
      MinecraftClient var5 = MinecraftClient.method_1551();
      ClientPlayerEntity var6 = var5.field_1724;
      if (!MeleeAttacks.canAct(var5)) {
         this.trackedId = -1;
      } else {
         this.trackCharge(var6);
         EntityHitResult var7 = MeleeAttacks.crosshairHit(var5);
         if (this.holdAttack.get() && this.trackedId != -1 && var5.field_1765 != null && var5.field_1765.method_17783() == Type.field_1333) {
            MeleeAttacks.takeAttackClicks(var5);
         }

         LivingEntity var3;
         Entity var4;
         if (var7 != null && (var4 = var7.method_17782()) instanceof LivingEntity && this.isTarget(var6, var3 = (LivingEntity)var4)) {
            if (var3.method_5628() != this.trackedId) {
               this.trackedId = var3.method_5628();
               this.reactionDoneTick = this.ticks + this.reaction.getInt() + MeleeAttacks.spread(2);
            }

            this.seenTick = this.ticks;
            MaceModule var8 = ModuleRegistry.get(MaceModule.class);
            boolean var2 = var8 != null && var8.smashReady(var6, var3);
            if (this.ticks >= this.reactionDoneTick && (var2 || this.chargedTick >= 0 && this.ticks - this.chargedTick >= this.chargePause)) {
               if (!this.holdAttack.get() || var5.field_1690.field_1886.method_1434()) {
                  if (!this.weaponsOnly.get() || MeleeAttacks.isMeleeWeapon(var6.method_6047())) {
                     if (!CombatMacroLock.heldByOther(this) && !MeleeAttacks.wastedOnShield(var6, var3)) {
                        if (var5.field_1771 <= 0 && !MeleeAttacks.clickPending(var5) && !MeleeAttacks.rightClickComing(var5) && !ServerRotation.pending()) {
                           boolean var10 = var6.method_6047().method_57826(DataComponentTypes.field_63631);
                           if (var10 || var2 || this.critsAllow(var5, var6, var3)) {
                              if (!var10 || (var7 = this.stabHit(var6, var3)) != null) {
                                 if (var10 ? MeleeAttacks.stab(var5, var7) : MeleeAttacks.attack(var5, var7)) {
                                    this.chargedTick = -1;
                                    if (var2) {
                                       var8.noteSmash();
                                    }

                                    MeleeAttacks.claimTick(var5);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         } else {
            if (this.ticks - this.seenTick > 4) {
               this.trackedId = -1;
            }
         }
      }
   }

   private void trackCharge(ClientPlayerEntity var1) {
      if (MeleeAttacks.charge(var1) < 1.0F) {
         this.chargedTick = -1;
      } else if (this.chargedTick < 0) {
         this.chargedTick = this.ticks;
         this.chargePause = MeleeAttacks.spread(1);
      }
   }

   private EntityHitResult stabHit(ClientPlayerEntity var1, LivingEntity var2) {
      Rotation var3 = ServerRotation.sent();
      if (var3 == null) {
         return null;
      } else {
         AttackRangeComponent var4 = var1.method_76693();
         Vec3d var5 = Vec3d.method_1030(var3.pitch(), var3.yaw());
         Vec3d var6 = var1.method_33571();
         Vec3d var7 = var6.method_1019(var5.method_1021(var4.method_76732(var1)));
         Vec3d var8 = var6.method_1019(var5.method_1021(var4.method_76739(var1) + Math.max(0.0, var1.method_60478().method_1026(var5))));
         float var9 = var4.comp_5262();
         Vec3d var10 = (Vec3d)var2.method_5829().method_1014(var9).method_992(var6, var8).orElse(null);
         if (var10 != null && MeleeAttacks.lineIsClear(var1, var6, var10)) {
            for (Entity var12 : var1.method_73183().method_8335(var1, new Box(var7, var8).method_1014(1.0))) {
               Box var13;
               if (var12 != var2
                  && var12 instanceof LivingEntity var14
                  && PiercingWeaponComponent.method_75244(var1, var12)
                  && !this.isTarget(var1, var14)
                  && ((var13 = var12.method_5829().method_1014(var9)).method_1006(var7) || var13.method_992(var7, var8).isPresent())) {
                  return null;
               }
            }

            return new EntityHitResult(var2, var10);
         } else {
            return null;
         }
      }
   }

   private boolean isTarget(ClientPlayerEntity var1, LivingEntity var2) {
      if (!MeleeAttacks.isEnemy(var1, var2)) {
         return false;
      } else if (var2.method_5767() && !this.invisible.get()) {
         return false;
      } else {
         return var2 instanceof PlayerEntity ? this.players.get() : this.hostiles.get() && MeleeAttacks.isHostile(var2);
      }
   }

   private boolean critsAllow(MinecraftClient var1, ClientPlayerEntity var2, LivingEntity var3) {
      String var4 = this.crits.get();

      return switch (var4) {
         case "Only" -> {
            if (MeleeAttacks.airborne(var2) && var2.method_5624() && var2.field_6017 > 0.0) {
               InputControl.dropSprint();
            }

            yield MeleeAttacks.wouldCrit(var2, var3);
         }
         case "Smart" -> {
            if (MeleeAttacks.wouldCrit(var2, var3)) {
               yield true;
            } else {
               boolean var6 = var2.method_24828() && var1.field_1690.field_1903.method_1434() && !var2.method_5624();
               if (!MeleeAttacks.critStance(var2, var3) && !var6) {
                  yield true;
               } else {
                  if (this.critWaitTick != this.ticks - 1) {
                     this.critWait = 0;
                  }

                  this.critWaitTick = this.ticks;
                  yield this.critWait++ >= 12;
               }
            }
         }
         default -> true;
      };
   }
}
