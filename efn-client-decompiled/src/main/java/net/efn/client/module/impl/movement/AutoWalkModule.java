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

   private void onPreTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var3 != null && var2.field_1687 != null) {
         if (this.disableOnY.get() && var3.field_6036 != var3.method_23318()) {
            this.requestDisable();
         } else {
            this.keyFor(var2.field_1690).method_23481(true);
         }
      }
   }

   private void onKey(KeyInputEvent var1) {
      if (var1.action == 1 && this.disableOnInput.get()) {
         if (var1.key != -1) {
            GameOptions var2 = MinecraftClient.method_1551().field_1690;
            KeyInput var3 = new KeyInput(var1.key, var1.scancode, var1.mods);
            if (var2.field_1894.method_1417(var3)
               || var2.field_1881.method_1417(var3)
               || var2.field_1913.method_1417(var3)
               || var2.field_1849.method_1417(var3)
               || var2.field_1832.method_1417(var3)
               || var2.field_1903.method_1417(var3)) {
               this.onMovementInput();
            }
         }
      }
   }

   private void onMouseButton(MouseInputEvent var1) {
      if (var1.action == 1 && this.disableOnInput.get()) {
         GameOptions var2 = MinecraftClient.method_1551().field_1690;
         Click var3 = new Click(0.0, 0.0, new MouseInput(var1.button, var1.mods));
         if (var2.field_1894.method_1433(var3)
            || var2.field_1881.method_1433(var3)
            || var2.field_1913.method_1433(var3)
            || var2.field_1849.method_1433(var3)
            || var2.field_1832.method_1433(var3)
            || var2.field_1903.method_1433(var3)) {
            this.onMovementInput();
         }
      }
   }

   private void onMovementInput() {
      if (MinecraftClient.method_1551().field_1755 == null) {
         this.requestDisable();
      }
   }

   private void onPlayerMove(PlayerMoveEvent var1) {
      if (this.waitForChunks.get()) {
         MinecraftClient var2 = MinecraftClient.method_1551();
         ClientPlayerEntity var3 = var2.field_1724;
         ClientWorld var4 = var2.field_1687;
         if (var3 != null && var4 != null) {
            int var5 = ChunkSectionPos.method_42615(var3.method_23317() + var1.x * 2.0);
            int var6 = ChunkSectionPos.method_42615(var3.method_23321() + var1.z * 2.0);
            if (!var4.method_2935().method_12123(var5, var6)) {
               var1.x = 0.0;
               var1.z = 0.0;
            }
         }
      }
   }

   private KeyBinding keyFor(GameOptions var1) {
      String var2 = this.direction.get();

      return switch (var2) {
         case "Backwards" -> var1.field_1881;
         case "Left" -> var1.field_1913;
         case "Right" -> var1.field_1849;
         default -> var1.field_1894;
      };
   }

   private static void releaseKeys() {
      MinecraftClient var0 = MinecraftClient.method_1551();
      GameOptions var1 = var0.field_1690;
      if (var1 != null) {
         boolean var2 = var0.field_1755 == null && !isFreecamActive();
         restore(var0, var1.field_1894, var2);
         restore(var0, var1.field_1881, var2);
         restore(var0, var1.field_1913, var2);
         restore(var0, var1.field_1849, var2);
      }
   }

   private static boolean isFreecamActive() {
      FreecamModule var0 = ModuleRegistry.get(FreecamModule.class);
      return var0 != null && var0.isEnabled();
   }

   private static void restore(MinecraftClient var0, KeyBinding var1, boolean var2) {
      var1.method_23481(var2 && isPhysicallyHeld(var0, var1));
   }

   private static boolean isPhysicallyHeld(MinecraftClient var0, KeyBinding var1) {
      if (var1.method_1415()) {
         return false;
      } else {
         Key var2 = InputUtil.method_15981(var1.method_1428());
         Type var3 = var2.method_1442();
         if (var3 == Type.field_1668) {
            return EfnKeyPolling.isKeyDown(var0.method_22683(), var2.method_1444());
         } else {
            return var3 == Type.field_1672 ? GLFW.glfwGetMouseButton(var0.method_22683().method_4490(), var2.method_1444()) == 1 : false;
         }
      }
   }
}
