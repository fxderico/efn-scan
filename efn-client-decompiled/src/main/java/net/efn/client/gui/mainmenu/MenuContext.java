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

   public MenuContext(Screen var1, Runnable var2, Runnable var3) {
      this.screen = var1;
      this.openAccounts = var2;
      this.openCredits = var3;
   }

   @Override
   public void begin(ImDrawList var1, ImDrawList var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, boolean var10) {
      super.begin(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10);
      boolean var11 = var8 >= 0.0F && var9 >= 0.0F && var8 <= var3 && var9 <= var4;
      float var12 = var11 ? var8 / var3 * 2.0F - 1.0F : 0.0F;
      float var13 = var11 ? var9 / var4 * 2.0F - 1.0F : 0.0F;
      float var14 = Math.min(1.0F, var7 * 2.5F);
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

   public boolean enabled(MenuAction var1) {
      return var1 != MenuAction.MULTIPLAYER && var1 != MenuAction.REALMS || MinecraftClient.method_1551().method_29043();
   }

   public float control(MenuAction var1, float var2, float var3, float var4, float var5) {
      boolean var6 = this.inside(var2, var3, var4, var5);
      if (!this.enabled(var1)) {
         if (var6) {
            this.tooltip("Multiplayer is disabled", this.mouseX(), var3 + var5 + this.px(6.0F));
         }

         return this.hover(var1, false);
      } else {
         if (var6 && this.clicked()) {
            this.activate(var1);
         }

         return this.hover(var1, var6);
      }
   }

   public float hover(MenuAction var1, boolean var2) {
      float var3 = this.hover[var1.ordinal()];
      var3 += ((var2 ? 1.0F : 0.0F) - var3) * Math.min(1.0F, this.dt() * 12.0F);
      this.hover[var1.ordinal()] = var3;
      return var3;
   }

   public void activate(MenuAction var1) {
      if (this.enabled(var1)) {
         MinecraftClient var3 = MinecraftClient.method_1551();

         Supplier var2 = switch (var1) {
            case SINGLEPLAYER -> () -> new SelectWorldScreen(this.screen);
            case MULTIPLAYER -> () -> (Screen)(var3.field_1690.field_21840 ? new MultiplayerScreen(this.screen) : new MultiplayerWarningScreen(this.screen));
            case REALMS -> () -> new RealmsMainScreen(this.screen);
            case OPTIONS -> () -> new OptionsScreen(this.screen, var3.field_1690);
            case LANGUAGE -> () -> new LanguageOptionsScreen(this.screen, var3.field_1690, var3.method_1526());
            case ACCESSIBILITY -> () -> new AccessibilityOptionsScreen(this.screen, var3.field_1690);
            case EFN -> ImGuiScreen::new;
            case QUIT, MODS, ACCOUNTS, CREDITS -> null;
         };
         if (var1 == MenuAction.QUIT) {
            var3.execute(() -> var3.method_1592());
         } else if (var1 == MenuAction.ACCOUNTS) {
            if (this.openAccounts != null) {
               this.openAccounts.run();
            }
         } else if (var1 == MenuAction.CREDITS) {
            this.openCredits.run();
         } else if (var1 == MenuAction.MODS) {
            var3.execute(() -> Util.method_668().method_60932(FabricLoader.getInstance().getGameDir().resolve("mods")));
         } else if (var2 != null) {
            var3.method_63588(() -> var3.method_1507((Screen)var2.get()));
         }
      }
   }
}
