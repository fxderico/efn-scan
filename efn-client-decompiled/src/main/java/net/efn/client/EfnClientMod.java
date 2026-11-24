package net.efn.client;

import net.efn.client.addon.AddonManager;
import net.efn.client.config.HudTheme;
import net.efn.client.config.ModuleConfigStore;
import net.efn.client.config.ProfileAutoLoad;
import net.efn.client.config.ThemeStore;
import net.efn.client.esp.ChunkScanner;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.BorderlessFullscreen;
import net.efn.client.gui.HudLayoutStore;
import net.efn.client.gui.HudRenderer;
import net.efn.client.gui.ImGuiScreen;
import net.efn.client.gui.LoadingScreens;
import net.efn.client.gui.StaffGhost;
import net.efn.client.gui.ToggleToasts;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.input.EfnKeyPolling;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.noinvleak.StreamOverlay;
import net.efn.client.util.BlockEntityLeakTracker;
import net.efn.client.util.CompletionChannel;
import net.efn.client.util.combat.CombatTarget;
import net.efn.client.util.netherite.NetheriteChunkTracker;
import net.efn.client.util.playerbypass.PlayerBypassLightTracker;
import net.efn.client.util.playerbypass.ProtectedChunkStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class EfnClientMod implements ClientModInitializer {
   public static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   public static final Identifier SPAWNER_NOTIFY_SOUND_ID = Identifier.of("efn", "spawner_notify");
   public static final SoundEvent SPAWNER_NOTIFY_SOUND = SoundEvent.of(SPAWNER_NOTIFY_SOUND_ID);
   public static final Category KEY_CATEGORY = Category.create(Identifier.of("efn", "controls"));
   private static KeyBinding openGuiKey;
   private final ModuleKeybinds keybinds = new ModuleKeybinds();
   private static final Object LEAK_TRACKER_OWNER = new Object();
   private static final Object PLAYER_BYPASS_TRACKER_OWNER = new Object();
   private static final Object NETHERITE_TRACKER_OWNER = new Object();

   public EfnClientMod() {
   }

   @Override
   public void onInitializeClient() {
      LOGGER.info("[EFN] Initialising");
      AddonManager.init();
      ImGuiManager.getInstance().setHudRenderer(HudRenderer.getInstance()::render, HudRenderer.getInstance()::hasWork);
      ImGuiManager.getInstance().setStreamRenderer(StreamOverlay::drawFakes, StreamOverlay::hasFakes);
      ImGuiManager.getInstance().setToastRenderer(ToggleToasts::render, ToggleToasts::hasWork);
      ImGuiManager.getInstance().setOverlayRenderer(LoadingScreens::render, LoadingScreens::hasWork);
      ImGuiManager.getInstance().setAlertRenderer(StaffGhost::render, StaffGhost::hasWork);
      Render2DEvent.TYPE.listen(StaffGhost.class, StaffGhost::renderNote);
      ChunkScanner.init();
      PacketReceiveNettyEvent.TYPE.listen(LEAK_TRACKER_OWNER, packetReceiveNettyEvent -> BlockEntityLeakTracker.ingest(packetReceiveNettyEvent.packet));
      CompletionChannel.install();
      PacketAppliedEvent.TYPE.listen(PLAYER_BYPASS_TRACKER_OWNER, packetAppliedEvent -> PlayerBypassLightTracker.onPacketApplied(packetAppliedEvent.packet));
      TickEvent.TYPE.listen(PLAYER_BYPASS_TRACKER_OWNER, tickEvent -> PlayerBypassLightTracker.onTick());
      TickEvent.TYPE.listen(BorderlessFullscreen.class, tickEvent -> BorderlessFullscreen.tick());
      CombatTarget.install();
      PacketAppliedEvent.TYPE.listen(NETHERITE_TRACKER_OWNER, packetAppliedEvent -> NetheriteChunkTracker.onPacketApplied(packetAppliedEvent.packet));
      ToggleToasts.install();
      ProfileAutoLoad.install();
      ClientLifecycleEvents.CLIENT_STOPPING.register(minecraftClient -> {
         HudLayoutStore.getInstance().save();
         ModuleConfigStore.getInstance().saveAndWait();
         ThemeStore.get().save();
         HudTheme.get().save();
         ProtectedChunkStore.get().flushAndWait();
         StreamOverlay.stop();
      });
      openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.efn.open_gui", Type.KEYSYM, 344, KEY_CATEGORY));
      ModuleConfigStore.getInstance().load();
      AddonManager.configLoaded();
      ClientTickEvents.END_CLIENT_TICK
         .register(
            minecraftClient -> {
               ModuleConfigStore.getInstance().applyPendingEnabled();

               while (openGuiKey.wasPressed()) {
                  if (minecraftClient.currentScreen instanceof ImGuiScreen) {
                     minecraftClient.setScreen(null);
                  } else {
                     minecraftClient.setScreen(new ImGuiScreen());
                  }
               }

               this.keybinds
                  .update(
                     ModuleRegistry.all(),
                     EfnClientMod::isBoundKeyDown,
                     minecraftClient.currentScreen == null && minecraftClient.isWindowFocused() && minecraftClient.player != null
                  );
               ModuleConfigStore.getInstance().tickAutosave();
            }
         );
   }

   public static boolean isGuiKey(long l, KeyInput keyInput) {
      return openGuiKey != null && l == MinecraftClient.getInstance().getWindow().getHandle() && openGuiKey.matchesKey(keyInput);
   }

   public static boolean toggleGuiFromKey() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.currentScreen == null) {
         var1.setScreen(new ImGuiScreen());
         return true;
      } else {
         Screen var2 = var1.currentScreen;
         ImGuiScreen var0;
         if (var2 instanceof ImGuiScreen && !(var0 = (ImGuiScreen)var2).capturesKeys()) {
            var1.setScreen(null);
         }

         return false;
      }
   }

   public static void dropGuiClicks() {
      while (openGuiKey.wasPressed()) {
      }
   }

   private static boolean isBoundKeyDown(int i) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      return i >= 1000 ? GLFW.glfwGetMouseButton(var1.getWindow().getHandle(), i - 1000) == 1 : EfnKeyPolling.isKeyDown(var1.getWindow(), i);
   }
}
