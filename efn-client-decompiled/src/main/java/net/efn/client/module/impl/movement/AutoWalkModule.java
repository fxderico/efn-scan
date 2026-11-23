package net.efn.client.module.impl.movement;

import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.PlayerMoveEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.EfnKeyPolling;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class AutoWalkModule extends ClientModule {
   private final ModeSetting direction = this.addSetting(
      "simple-direction", new ModeSetting("Simple Direction", "Forwards", "Forwards", "Backwards", "Left", "Right")
   );
   private final BooleanSetting disableOnInput = this.addSetting("disable-on-input", new BooleanSetting("Disable On Input", false));
   private final BooleanSetting disableOnY = this.addSetting("disable-on-y-change", new BooleanSetting("Disable On Y Change", false));
   private final BooleanSetting waitForChunks = this.addSetting("no-unloaded-chunks", new BooleanSetting("No Unloaded Chunks", true));

   public AutoWalkModule() {
      super("auto-walk", "Auto Walk", "Automatically walks forward.", ModuleCategory.MOVEMENT);
      this.direction.onChange(() -> {
         if (this.isEnabled()) {
            releaseKeys();
         }
      });
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.on(TickEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onPreTick);
      this.on(KeyInputEvent.TYPE, this::onKey);
      this.on(MouseInputEvent.TYPE, this::onMouseButton);
      this.on(PlayerMoveEvent.TYPE, this::onPlayerMove);
   }

   @Override
   protected void onDisable() {
      releaseKeys();
   }

   private void onPreTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null && var2.world != null) {
         if (this.disableOnY.get() && var3.lastY != var3.getY()) {
            this.requestDisable();
         } else {
            this.keyFor(var2.options).setPressed(true);
         }
      }
   }

   private void onKey(KeyInputEvent keyInputEvent) {
      if (keyInputEvent.action == 1 && this.disableOnInput.get()) {
         if (keyInputEvent.key != -1) {
            GameOptions var2 = MinecraftClient.getInstance().options;
            KeyInput var3 = new KeyInput(keyInputEvent.key, keyInputEvent.scancode, keyInputEvent.mods);
            if (var2.forwardKey.matchesKey(var3)
               || var2.backKey.matchesKey(var3)
               || var2.leftKey.matchesKey(var3)
               || var2.rightKey.matchesKey(var3)
               || var2.sneakKey.matchesKey(var3)
               || var2.jumpKey.matchesKey(var3)) {
               this.onMovementInput();
            }
         }
      }
   }

   private void onMouseButton(MouseInputEvent mouseInputEvent) {
      if (mouseInputEvent.action == 1 && this.disableOnInput.get()) {
         GameOptions var2 = MinecraftClient.getInstance().options;
         Click var3 = new Click(0.0, 0.0, new MouseInput(mouseInputEvent.button, mouseInputEvent.mods));
         if (var2.forwardKey.matchesMouse(var3)
            || var2.backKey.matchesMouse(var3)
            || var2.leftKey.matchesMouse(var3)
            || var2.rightKey.matchesMouse(var3)
            || var2.sneakKey.matchesMouse(var3)
            || var2.jumpKey.matchesMouse(var3)) {
            this.onMovementInput();
         }
      }
   }

   private void onMovementInput() {
      if (MinecraftClient.getInstance().currentScreen == null) {
         this.requestDisable();
      }
   }

   private void onPlayerMove(PlayerMoveEvent playerMoveEvent) {
      if (this.waitForChunks.get()) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         ClientPlayerEntity var3 = var2.player;
         ClientWorld var4 = var2.world;
         if (var3 != null && var4 != null) {
            int var5 = ChunkSectionPos.getSectionCoordFloored(var3.getX() + playerMoveEvent.x * 2.0);
            int var6 = ChunkSectionPos.getSectionCoordFloored(var3.getZ() + playerMoveEvent.z * 2.0);
            if (!var4.getChunkManager().isChunkLoaded(var5, var6)) {
               playerMoveEvent.x = 0.0;
               playerMoveEvent.z = 0.0;
            }
         }
      }
   }

   private KeyBinding keyFor(GameOptions gameOptions) {
      String var2 = this.direction.get();

      return switch (var2) {
         case "Backwards" -> gameOptions.backKey;
         case "Left" -> gameOptions.leftKey;
         case "Right" -> gameOptions.rightKey;
         default -> gameOptions.forwardKey;
      };
   }

   private static void releaseKeys() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      GameOptions var1 = var0.options;
      if (var1 != null) {
         boolean var2 = var0.currentScreen == null && !isFreecamActive();
         restore(var0, var1.forwardKey, var2);
         restore(var0, var1.backKey, var2);
         restore(var0, var1.leftKey, var2);
         restore(var0, var1.rightKey, var2);
      }
   }

   private static boolean isFreecamActive() {
      FreecamModule var0 = ModuleRegistry.get(FreecamModule.class);
      return var0 != null && var0.isEnabled();
   }

   private static void restore(MinecraftClient minecraftClient, KeyBinding keyBinding, boolean bl) {
      keyBinding.setPressed(bl && isPhysicallyHeld(minecraftClient, keyBinding));
   }

   private static boolean isPhysicallyHeld(MinecraftClient minecraftClient, KeyBinding keyBinding) {
      if (keyBinding.isUnbound()) {
         return false;
      } else {
         Key var2 = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
         Type var3 = var2.getCategory();
         if (var3 == Type.KEYSYM) {
            return EfnKeyPolling.isKeyDown(minecraftClient.getWindow(), var2.getCode());
         } else {
            return var3 == Type.MOUSE ? GLFW.glfwGetMouseButton(minecraftClient.getWindow().getHandle(), var2.getCode()) == 1 : false;
         }
      }
   }
}
