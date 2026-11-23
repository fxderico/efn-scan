package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.InputControl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

@Environment(EnvType.CLIENT)
public final class WTapRecodeModule extends ClientModule {
   private static final String W_TAP = "W Tap";
   private static final String S_TAP = "S Tap";
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "W Tap", "W Tap", "S Tap"));
   private final NumberSetting hold = this.addSetting("hold", new NumberSetting("Hold", 1.0F, 1.0F, 3.0F).step(1.0F));
   private boolean sprintHit;
   private int taps;
   private int tapTick;

   public WTapRecodeModule() {
      super("w-tap-recode", "W Tap", "Resets your sprint right after a sprint hit so the next hit knocks back too.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onDisable() {
      this.taps = 0;
   }

   public void beforeAttack(PlayerEntity var1, Entity var2) {
      ClientPlayerEntity var3 = MinecraftClient.method_1551().field_1724;
      this.sprintHit = var1 == var3
         && var3.method_5624()
         && MeleeAttacks.charge(var3) > 0.9F
         && var2 instanceof LivingEntity
         && var3.field_3913.field_54155.comp_3159();
   }

   public void afterAttack() {
      if (this.sprintHit) {
         this.start(MinecraftClient.method_1551().field_1724);
      }
   }

   public void afterStab() {
      ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
      if (var1.method_5624() && var1.field_3913.field_54155.comp_3159()) {
         if (MeleeAttacks.stabTarget(var1, var1x -> PiercingWeaponComponent.method_75244(var1, var1x)) != null) {
            this.start(var1);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onTick(TickEvent var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 == null) {
         this.taps = 0;
      } else {
         if (this.taps > 0 && var2.field_6012 != this.tapTick) {
            this.apply();
         }
      }
   }

   private void start(ClientPlayerEntity var1) {
      this.taps = this.hold.getInt();
      this.tapTick = var1.field_6012;
      this.apply();
   }

   private void apply() {
      InputControl.releaseForward(this.mode.is("S Tap"));
      this.taps--;
   }
}
