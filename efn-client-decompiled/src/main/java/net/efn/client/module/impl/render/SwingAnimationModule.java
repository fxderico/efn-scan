package net.efn.client.module.impl.render;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.combat.CombatTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public final class SwingAnimationModule extends ClientModule {
   public static volatile SwingAnimationModule active;
   private static final String KHOPESH = "Slash";
   private static final String BOLT = "Chop";
   private static final String SICKLE = "Hook";
   private static final String SPIN = "Spin";
   private static final String TAP = "Double Tap";
   private static final String PUSH = "Push";
   private static final long COMBAT_WINDOW_MS = 3000L;
   private static final float PI = 3.1415927F;
   private static final float TAU = 6.2831855F;
   private final ModeSetting style = this.addSetting("style", new ModeSetting("Style", "Slash", "Slash", "Chop", "Hook", "Spin", "Double Tap", "Push"));
   private final NumberSetting tempo = this.addSetting("tempo", new NumberSetting("Tempo", 1.0F, 0.4F, 2.5F).step(0.05F));
   private final NumberSetting intensity = this.addSetting("intensity", new NumberSetting("Intensity", 1.0F, 0.2F, 2.0F).step(0.05F));
   private final BooleanSetting offhand = this.addSetting("offhand", new BooleanSetting("Offhand Too", true));
   private final BooleanSetting combatOnly = this.addSetting("combat-only", new BooleanSetting("Combat Only", false));
   private int styleIndex;
   private boolean styled;

   public SwingAnimationModule() {
      super("swing-animation", "Swing Animation", "Changes your first-person swing animation.", ModuleCategory.VISUALS);
      this.style.onChange(this::cacheStyle);
      this.cacheStyle();
   }

   @Override
   protected void onEnable() {
      this.latch();
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   private void cacheStyle() {
      this.styleIndex = Math.max(0, this.style.modes().indexOf(this.style.get()));
   }

   public void latch() {
      this.styled = !this.combatOnly.get() || inCombat();
   }

   private static boolean inCombat() {
      if (CombatTarget.lastHit(3000L) != null) {
         return true;
      } else {
         Entity var0 = MinecraftClient.method_1551().field_1692;
         return (var0 instanceof PlayerEntity || var0 instanceof Monster) && var0.method_5805();
      }
   }

   public boolean covers(boolean var1) {
      return (var1 || this.offhand.get()) && this.styled;
   }

   public int swingTicks(int var1) {
      return !this.styled ? var1 : Math.max(1, Math.round(var1 / this.tempo.get()));
   }

   public void pose(MatrixStack var1, float var2, int var3) {
      if (!(var2 <= 0.0F) && !(var2 >= 1.0F)) {
         float var4 = this.intensity.get();
         switch (this.styleIndex) {
            case 1:
               bolt(var1, var2, var3, var4);
               break;
            case 2:
               sickle(var1, var2, var3, var4);
               break;
            case 3:
               spin(var1, var2, var3, var4);
               break;
            case 4:
               doubleTap(var1, var2, var3, var4);
               break;
            case 5:
               push(var1, var2, var3, var4);
               break;
            default:
               khopesh(var1, var2, var3, var4);
         }
      }
   }

   private static void khopesh(MatrixStack var0, float var1, int var2, float var3) {
      float var4 = crest(var1, 0.3F) * var3;
      var0.method_46416(var2 * -0.3F * var4, 0.05F * MathHelper.method_15374(3.1415927F * var1) * var3, -0.1F * var4);
      var0.method_22907(RotationAxis.field_40716.rotationDegrees(var2 * 64.0F * var4));
      var0.method_22907(RotationAxis.field_40718.rotationDegrees(var2 * 48.0F * var4));
      var0.method_22907(RotationAxis.field_40714.rotationDegrees(-14.0F * var4));
   }

   private static void bolt(MatrixStack var0, float var1, int var2, float var3) {
      float var4 = var1 < 0.28F ? MathHelper.method_15374(3.1415927F * var1 / 0.28F) * var3 : 0.0F;
      float var5 = var1 <= 0.18F ? 0.0F : crest((var1 - 0.18F) / 0.82F, 0.28F) * var3;
      var0.method_46416(var2 * -0.06F * var5, 0.16F * var4 - 0.1F * var5, 0.06F * var4 - 0.24F * var5);
      var0.method_22907(RotationAxis.field_40714.rotationDegrees(30.0F * var4 - 105.0F * var5));
      var0.method_22907(RotationAxis.field_40718.rotationDegrees(var2 * 10.0F * var5));
   }

   private static void sickle(MatrixStack var0, float var1, int var2, float var3) {
      float var4 = crest(var1, 0.42F);
      float var5 = var4 * var4 * var3;
      float var6;
      var0.method_46416(var2 * -0.1F * (var6 = var4 * var3), -0.24F * var6, -0.06F * var6);
      var0.method_22907(RotationAxis.field_40718.rotationDegrees(var2 * -100.0F * var6));
      var0.method_22907(RotationAxis.field_40716.rotationDegrees(var2 * 30.0F * var5));
      var0.method_22907(RotationAxis.field_40714.rotationDegrees(-25.0F * var6));
   }

   private static void spin(MatrixStack var0, float var1, int var2, float var3) {
      float var4 = 6.2831855F * var1;
      float var5 = MathHelper.method_15362(var4);
      float var6 = MathHelper.method_15374(var4);
      float var7 = MathHelper.method_15374(3.1415927F * var1) * var3;
      float var8 = 0.17F * var3;
      var0.method_46416(var2 * var8 * 0.6F * (var5 - 1.0F), var8 * var6, -0.12F * var7);
      var0.method_22907(RotationAxis.field_40718.rotationDegrees(var2 * 40.0F * var6 * var3));
      var0.method_22907(RotationAxis.field_40714.rotationDegrees(-40.0F * var7));
   }

   private static void doubleTap(MatrixStack var0, float var1, int var2, float var3) {
      float var4 = MathHelper.method_15374(6.2831855F * var1);
      float var5 = var4 * var4 * (var1 < 0.5F ? 1.0F : 0.65F) * var3;
      var0.method_46416(var2 * -0.08F * var5, 0.06F * var5, -0.1F * var5);
      var0.method_22907(RotationAxis.field_40716.rotationDegrees(var2 * 26.0F * var5));
      var0.method_22907(RotationAxis.field_40714.rotationDegrees(-50.0F * var5));
      var0.method_22907(RotationAxis.field_40718.rotationDegrees(var2 * -22.0F * var5));
   }

   private static void push(MatrixStack var0, float var1, int var2, float var3) {
      float var4 = (0.5F - 0.5F * MathHelper.method_15362(6.2831855F * var1)) * var3;
      var0.method_46416(0.0F, -0.06F * var4, -0.1F * var4);
      var0.method_22907(RotationAxis.field_40714.rotationDegrees(-24.0F * var4));
      var0.method_22907(RotationAxis.field_40716.rotationDegrees(var2 * 10.0F * var4));
   }

   private static float crest(float var0, float var1) {
      if (var0 <= 0.0F || var0 >= 1.0F) {
         return 0.0F;
      } else if (var0 < var1) {
         float var3 = 1.0F - var0 / var1;
         return 1.0F - var3 * var3 * var3;
      } else {
         float var2 = (var0 - var1) / (1.0F - var1);
         return 1.0F - var2 * var2 * (3.0F - 2.0F * var2);
      }
   }
}
