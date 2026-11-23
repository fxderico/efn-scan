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
         Entity var0 = MinecraftClient.getInstance().targetedEntity;
         return (var0 instanceof PlayerEntity || var0 instanceof Monster) && var0.isAlive();
      }
   }

   public boolean covers(boolean bl) {
      return (bl || this.offhand.get()) && this.styled;
   }

   public int swingTicks(int i) {
      return !this.styled ? i : Math.max(1, Math.round(i / this.tempo.get()));
   }

   public void pose(MatrixStack matrixStack, float f, int i) {
      if (!(f <= 0.0F) && !(f >= 1.0F)) {
         float var4 = this.intensity.get();
         switch (this.styleIndex) {
            case 1:
               bolt(matrixStack, f, i, var4);
               break;
            case 2:
               sickle(matrixStack, f, i, var4);
               break;
            case 3:
               spin(matrixStack, f, i, var4);
               break;
            case 4:
               doubleTap(matrixStack, f, i, var4);
               break;
            case 5:
               push(matrixStack, f, i, var4);
               break;
            default:
               khopesh(matrixStack, f, i, var4);
         }
      }
   }

   private static void khopesh(MatrixStack matrixStack, float f, int i, float g) {
      float var4 = crest(f, 0.3F) * g;
      matrixStack.translate(i * -0.3F * var4, 0.05F * MathHelper.sin(3.1415927F * f) * g, -0.1F * var4);
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 64.0F * var4));
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 48.0F * var4));
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-14.0F * var4));
   }

   private static void bolt(MatrixStack matrixStack, float f, int i, float g) {
      float var4 = f < 0.28F ? MathHelper.sin(3.1415927F * f / 0.28F) * g : 0.0F;
      float var5 = f <= 0.18F ? 0.0F : crest((f - 0.18F) / 0.82F, 0.28F) * g;
      matrixStack.translate(i * -0.06F * var5, 0.16F * var4 - 0.1F * var5, 0.06F * var4 - 0.24F * var5);
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * var4 - 105.0F * var5));
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 10.0F * var5));
   }

   private static void sickle(MatrixStack matrixStack, float f, int i, float g) {
      float var4 = crest(f, 0.42F);
      float var5 = var4 * var4 * g;
      float var6;
      matrixStack.translate(i * -0.1F * (var6 = var4 * g), -0.24F * var6, -0.06F * var6);
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -100.0F * var6));
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 30.0F * var5));
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F * var6));
   }

   private static void spin(MatrixStack matrixStack, float f, int i, float g) {
      float var4 = 6.2831855F * f;
      float var5 = MathHelper.cos(var4);
      float var6 = MathHelper.sin(var4);
      float var7 = MathHelper.sin(3.1415927F * f) * g;
      float var8 = 0.17F * g;
      matrixStack.translate(i * var8 * 0.6F * (var5 - 1.0F), var8 * var6, -0.12F * var7);
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 40.0F * var6 * g));
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40.0F * var7));
   }

   private static void doubleTap(MatrixStack matrixStack, float f, int i, float g) {
      float var4 = MathHelper.sin(6.2831855F * f);
      float var5 = var4 * var4 * (f < 0.5F ? 1.0F : 0.65F) * g;
      matrixStack.translate(i * -0.08F * var5, 0.06F * var5, -0.1F * var5);
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 26.0F * var5));
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F * var5));
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -22.0F * var5));
   }

   private static void push(MatrixStack matrixStack, float f, int i, float g) {
      float var4 = (0.5F - 0.5F * MathHelper.cos(6.2831855F * f)) * g;
      matrixStack.translate(0.0F, -0.06F * var4, -0.1F * var4);
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-24.0F * var4));
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 10.0F * var4));
   }

   private static float crest(float f, float g) {
      if (f <= 0.0F || f >= 1.0F) {
         return 0.0F;
      } else if (f < g) {
         float var3 = 1.0F - f / g;
         return 1.0F - var3 * var3 * var3;
      } else {
         float var2 = (f - g) / (1.0F - g);
         return 1.0F - var2 * var2 * (3.0F - 2.0F * var2);
      }
   }
}
