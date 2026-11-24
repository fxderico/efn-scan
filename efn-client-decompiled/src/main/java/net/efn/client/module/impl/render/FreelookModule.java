package net.efn.client.module.impl.render;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.EfnKeyPolling;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class FreelookModule extends ClientModule {
   private static final String PLAYER = "Player";
   private static final String CAMERA = "Camera";
   private static final float MIN_SENSITIVITY = 0.1F;
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "Player", "Player", "Camera"));
   private final BooleanSetting togglePerspective = this.addSetting("toggle-perspective", new BooleanSetting("Toggle Perspective", true));
   private final NumberSetting sensitivity = this.addSetting("camera-sensitivity", new NumberSetting("Camera Sensitivity", 8.0F, 0.0F, 10.0F).step(0.1F));
   private final BooleanSetting arrows = this.addSetting("arrows-control-opposite", new BooleanSetting("Arrows Control Opposite", true));
   private final NumberSetting arrowSpeed = this.addSetting("arrow-speed", new NumberSetting("Arrow Speed", 4.0F, 0.0F, 10.0F).step(0.5F));
   private float cameraYaw;
   private float cameraPitch;
   private Perspective previousView;

   public FreelookModule() {
      super("freelook", "Freelook", "Turns the camera and the player separately.", ModuleCategory.RENDER);
      this.sensitivity.visibleWhen(() -> this.mode.is("Camera"));
      this.arrowSpeed.visibleWhen(this.arrows::get);
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.player == null) {
         this.requestDisable();
      } else {
         this.cameraYaw = var1.player.getYaw();
         this.cameraPitch = var1.player.getPitch();
         this.previousView = var1.options.getPerspective();
         if (this.previousView != Perspective.THIRD_PERSON_BACK && this.togglePerspective.get()) {
            var1.options.setPerspective(Perspective.THIRD_PERSON_BACK);
         }

         this.on(TickEvent.TYPE, Stage.POST, this::onTick);
      }
   }

   @Override
   protected void onDisable() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (this.previousView != null && var1.options.getPerspective() != this.previousView && this.togglePerspective.get()) {
         var1.options.setPerspective(this.previousView);
      }

      this.previousView = null;
   }

   private void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null) {
         if (this.arrows.get() && var2.currentScreen == null) {
            float var4 = this.arrowSpeed.get();
            float var5 = (held(var2, 262) ? var4 : 0.0F) - (held(var2, 263) ? var4 : 0.0F);
            float var6 = (held(var2, 264) ? var4 : 0.0F) - (held(var2, 265) ? var4 : 0.0F);
            if (this.mode.is("Player")) {
               this.cameraYaw += var5;
               this.cameraPitch += var6;
            } else if (var5 != 0.0F || var6 != 0.0F) {
               var3.setYaw(var3.getYaw() + var5);
               var3.setPitch(var3.getPitch() + var6);
            }
         }

         this.cameraPitch = MathHelper.clamp(this.cameraPitch, -90.0F, 90.0F);
      }
   }

   private static boolean held(MinecraftClient minecraftClient, int i) {
      return EfnKeyPolling.isKeyDown(minecraftClient.getWindow(), i);
   }

   public boolean cameraMode() {
      return this.isEnabled() && this.mode.is("Camera");
   }

   public static boolean throughBlocks() {
      FreelookModule var0 = ModuleRegistry.get(FreelookModule.class);
      return var0 != null && var0.isEnabled() && FreecamModule.active == null;
   }

   public void turnCamera(double d, double e) {
      float var5 = Math.max(this.sensitivity.get(), 0.1F);
      this.cameraYaw += (float)(d / var5);
      this.cameraPitch += (float)(e / var5);
      if (Math.abs(this.cameraPitch) > 90.0F) {
         this.cameraPitch = this.cameraPitch > 0.0F ? 90.0F : -90.0F;
      }
   }

   public float cameraYaw() {
      return this.cameraYaw;
   }

   public float cameraPitch() {
      return this.cameraPitch;
   }
}
