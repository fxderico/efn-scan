package net.efn.client.module.impl.render;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.ActionSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public final class HandsModule extends ClientModule {
   public static volatile HandsModule live;
   private static final String MAIN = "Main Hand";
   private static final String OFF = "Offhand";
   private static final float PIVOT_X = 0.56F;
   private static final float PIVOT_Y = -0.52F;
   private static final float PIVOT_Z = -0.72F;
   private static final float[] REST = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
   private static final int X = 0;
   private static final int Y = 1;
   private static final int Z = 2;
   private static final int PITCH = 3;
   private static final int YAW = 4;
   private static final int ROLL = 5;
   private static final int SCALE = 6;
   private static final float SETTLED = 0.001F;
   private static final float MAX_STEP = 0.1F;
   private static final float SLOWEST_RATE = 3.0F;
   private static final float FASTEST_RATE = 30.0F;
   private final BooleanSetting link = this.addSetting("link-hands", new BooleanSetting("Link Hands", false));
   private final ModeSetting editing = this.addSetting("editing", new ModeSetting("Editing", "Main Hand", "Main Hand", "Offhand"));
   private final NumberSetting[] main = this.axes("main", true);
   private final NumberSetting[] off = this.axes("off", false);
   private final NumberSetting glide = this.addSetting("glide", new NumberSetting("Glide", 0.4F, 0.0F, 1.0F).step(0.05F));
   private final HandsModule.HandState mainState = new HandsModule.HandState();
   private final HandsModule.HandState offState = new HandsModule.HandState();

   public HandsModule() {
      super("hand-pose", "Hand Pose", "Moves, rotates and resizes your first-person hands.", ModuleCategory.VISUALS);
      this.editing.visibleWhen(() -> !this.link.get());
      this.addSetting("reset-hand", new ActionSetting("Reset Hand", this::resetEdited));
   }

   private NumberSetting[] axes(String var1, boolean var2) {
      NumberSetting[] var3;
      for (NumberSetting var7 : var3 = new NumberSetting[]{
         this.addSetting(var1 + "-x", new NumberSetting("Offset X", REST[0], -1.0F, 1.0F).step(0.01F)),
         this.addSetting(var1 + "-y", new NumberSetting("Offset Y", REST[1], -1.0F, 1.0F).step(0.01F)),
         this.addSetting(var1 + "-z", new NumberSetting("Offset Z", REST[2], -1.0F, 1.0F).step(0.01F)),
         this.addSetting(var1 + "-pitch", new NumberSetting("Pitch", REST[3], -180.0F, 180.0F).step(1.0F)),
         this.addSetting(var1 + "-yaw", new NumberSetting("Yaw", REST[4], -180.0F, 180.0F).step(1.0F)),
         this.addSetting(var1 + "-roll", new NumberSetting("Roll", REST[5], -180.0F, 180.0F).step(1.0F)),
         this.addSetting(var1 + "-scale", new NumberSetting("Scale", REST[6], 0.3F, 2.0F).step(0.05F))
      }) {
         var7.visibleWhen(var2 ? () -> this.link.get() || this.editing.is("Main Hand") : () -> !this.link.get() && this.editing.is("Offhand"));
      }

      return var3;
   }

   @Override
   protected void onEnable() {
      this.mainState.stamp = 0L;
      this.offState.stamp = 0L;
      live = this;
   }

   private void resetEdited() {
      NumberSetting[] var1 = !this.link.get() && !this.editing.is("Main Hand") ? this.off : this.main;

      for (int var2 = 0; var2 < var1.length; var2++) {
         var1[var2].set(REST[var2]);
      }
   }

   public static void shape(MatrixStack var0, boolean var1, int var2) {
      HandsModule var3 = live;
      if (var3 != null) {
         var3.shapeHand(var0, var1, var2);
      }
   }

   private void shapeHand(MatrixStack var1, boolean var2, int var3) {
      boolean var4 = this.isEnabled();
      HandsModule.HandState var5 = var2 ? this.mainState : this.offState;
      var5.step(!var2 && !this.link.get() ? this.off : this.main, var4, this.follow());
      if (!var4 && this.mainState.rested && this.offState.rested) {
         live = null;
      } else if (!var5.rested) {
         float[] var6 = var5.value;
         var1.method_46416(var3 * (0.56F + var6[0]), -0.52F + var6[1], -0.72F + var6[2]);
         var1.method_22907(RotationAxis.field_40716.rotationDegrees(var3 * var6[4]));
         var1.method_22907(RotationAxis.field_40714.rotationDegrees(var6[3]));
         var1.method_22907(RotationAxis.field_40718.rotationDegrees(var3 * var6[5]));
         var1.method_22905(var6[6], var6[6], var6[6]);
         var1.method_46416(-var3 * 0.56F, 0.52F, 0.72F);
      }
   }

   private float follow() {
      float var1 = this.glide.get();
      return var1 <= 0.0F ? -1.0F : 30.0F + -27.0F * var1;
   }

   @Environment(EnvType.CLIENT)
   private static final class HandState {
      final float[] value = (float[])HandsModule.REST.clone();
      long stamp;
      boolean rested = true;

      private HandState() {
      }

      void step(NumberSetting[] var1, boolean var2, float var3) {
         long var4 = System.nanoTime();
         float var6 = this.stamp == 0L ? 0.0F : Math.min(0.1F, (float)(var4 - this.stamp) / 1.0E9F);
         this.stamp = var4;
         float var7 = var3 < 0.0F ? 1.0F : 1.0F - (float)Math.exp(-var3 * var6);
         boolean var8 = true;

         for (int var9 = 0; var9 < this.value.length; var9++) {
            float var11 = var2 ? var1[var9].get() : HandsModule.REST[var9];
            float var10;
            if (Math.abs(var11 - (var10 = this.value[var9] + (var11 - this.value[var9]) * var7)) < 0.001F) {
               var10 = var11;
            }

            this.value[var9] = var10;
            if (var10 != HandsModule.REST[var9]) {
               var8 = false;
            }
         }

         this.rested = var8;
      }
   }
}
