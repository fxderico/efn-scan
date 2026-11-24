package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import java.util.function.Supplier;
import net.efn.client.gui.ImGuiScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class MenuContext extends MenuFrame {
   private static final float LOOK_EASE = 2.5F;
   private final Screen screen;
   private final Runnable openAccounts;
   private final Runnable openCredits;
   private final float[] hover = new float[MenuAction.values().length];
   private float lookX;
   private float lookY;

   public MenuContext(Screen screen, Runnable runnable, Runnable runnable2) {
      this.screen = screen;
      this.openAccounts = runnable;
      this.openCredits = runnable2;
   }

   @Override
   public void begin(ImDrawList imDrawList, ImDrawList imDrawList2, float f, float g, float h, float i, float j, float k, float l, boolean bl) {
      super.begin(imDrawList, imDrawList2, f, g, h, i, j, k, l, bl);
      boolean var11 = k >= 0.0F && l >= 0.0F && k <= f && l <= g;
      float var12 = var11 ? k / f * 2.0F - 1.0F : 0.0F;
      float var13 = var11 ? l / g * 2.0F - 1.0F : 0.0F;
      float var14 = Math.min(1.0F, j * 2.5F);
      this.lookX = this.lookX + (var12 - this.lookX) * var14;
      this.lookY = this.lookY + (var13 - this.lookY) * var14;
   }

   public boolean accountsAvailable() {
      return this.openAccounts != null;
   }

   public float lookX() {
      return this.lookX;
   }

   public float lookY() {
      return this.lookY;
   }

   public boolean enabled(MenuAction menuAction) {
      return menuAction != MenuAction.MULTIPLAYER && menuAction != MenuAction.REALMS || MinecraftClient.getInstance().isMultiplayerEnabled();
   }

   public float control(MenuAction menuAction, float f, float g, float h, float i) {
      boolean var6 = this.inside(f, g, h, i);
      if (!this.enabled(menuAction)) {
         if (var6) {
            this.tooltip("Multiplayer is disabled", this.mouseX(), g + i + this.px(6.0F));
         }

         return this.hover(menuAction, false);
      } else {
         if (var6 && this.clicked()) {
            this.activate(menuAction);
         }

         return this.hover(menuAction, var6);
      }
   }

   public float hover(MenuAction menuAction, boolean bl) {
      float var3 = this.hover[menuAction.ordinal()];
      var3 += ((bl ? 1.0F : 0.0F) - var3) * Math.min(1.0F, this.dt() * 12.0F);
      this.hover[menuAction.ordinal()] = var3;
      return var3;
   }

   public void activate(MenuAction menuAction) {
      if (this.enabled(menuAction)) {
         MinecraftClient var3 = MinecraftClient.getInstance();

         Supplier var2 = switch (menuAction) {
            case SINGLEPLAYER -> () -> new SelectWorldScreen(this.screen);
            case MULTIPLAYER -> () -> (Screen)(var3.options.skipMultiplayerWarning
               ? new MultiplayerScreen(this.screen)
               : new MultiplayerWarningScreen(this.screen));
            case REALMS -> () -> new RealmsMainScreen(this.screen);
            case OPTIONS -> () -> new OptionsScreen(this.screen, var3.options);
            case LANGUAGE -> () -> new LanguageOptionsScreen(this.screen, var3.options, var3.getLanguageManager());
            case ACCESSIBILITY -> () -> new AccessibilityOptionsScreen(this.screen, var3.options);
            case EFN -> ImGuiScreen::new;
            case QUIT, MODS, ACCOUNTS, CREDITS -> null;
         };
         if (menuAction == MenuAction.QUIT) {
            var3.execute(() -> var3.scheduleStop());
         } else if (menuAction == MenuAction.ACCOUNTS) {
            if (this.openAccounts != null) {
               this.openAccounts.run();
            }
         } else if (menuAction == MenuAction.CREDITS) {
            this.openCredits.run();
         } else if (menuAction == MenuAction.MODS) {
            var3.execute(() -> Util.getOperatingSystem().open(FabricLoader.getInstance().getGameDir().resolve("mods")));
         } else if (var2 != null) {
            var3.send(() -> var3.setScreen((Screen)var2.get()));
         }
      }
   }
}
