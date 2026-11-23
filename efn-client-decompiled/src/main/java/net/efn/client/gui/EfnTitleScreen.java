package net.efn.client.gui;

import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.input.KeyInput;

@Environment(EnvType.CLIENT)
public final class EfnTitleScreen extends TitleScreen {
   private final MainMenuRenderer menu = new MainMenuRenderer((Screen)this);

   public EfnTitleScreen() {
   }

   public static boolean enabled() {
      return ThemeStore.get().customMainMenu.get();
   }

   protected void method_25426() {
      ImGuiManager.getInstance().activate(this.menu::render);
   }

   public void method_25393() {
   }

   public boolean method_25404(KeyInput var1) {
      return this.menu.keyPressed(var1.comp_4795()) || super.method_25404(var1);
   }

   public boolean method_25402(Click var1, boolean var2) {
      return false;
   }

   public void method_25432() {
      super.method_25432();
      ImGuiManager.getInstance().deactivate();
   }

   public void method_25420(DrawContext var1, int var2, int var3, float var4) {
   }

   public void method_25394(DrawContext var1, int var2, int var3, float var4) {
      ImGuiManager.getInstance().requestCursor(var1);
      var1.method_73199();
   }

   public boolean method_25422() {
      return false;
   }

   public boolean method_25421() {
      return false;
   }
}
