package net.efn.client.gui;

import java.nio.file.Path;
import java.util.List;
import net.efn.client.config.HudTheme;
import net.efn.client.config.ModuleConfigStore;
import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class ImGuiScreen extends Screen {
   private final EfnClickGui clickGui = new EfnClickGui();

   public ImGuiScreen() {
      super(Text.method_43470("EFN"));
   }

   protected void method_25426() {
      super.method_25426();
      ImGuiManager.getInstance().activate(this.clickGui::render);
   }

   public void method_25432() {
      super.method_25432();
      this.clickGui.handleEscape();
      ImGuiManager.getInstance().deactivate();
      ModuleConfigStore.getInstance().saveIfChanged();
      ThemeStore.get().save();
      HudTheme.get().save();
   }

   public void method_25420(DrawContext var1, int var2, int var3, float var4) {
      if (ThemeStore.get().menuBlur.get()) {
         super.method_25420(var1, var2, var3, var4);
      }
   }

   public void method_29638(List<Path> var1) {
      this.clickGui.onFilesDropped(var1);
   }

   public void method_25394(DrawContext var1, int var2, int var3, float var4) {
      ImGuiManager.getInstance().requestCursor(var1);
   }

   public boolean method_25404(KeyInput var1) {
      return var1.comp_4795() == 256 && this.clickGui.handleEscape() ? true : super.method_25404(var1);
   }

   public boolean capturesKeys() {
      return this.clickGui.capturesKeys();
   }

   public boolean method_25421() {
      return false;
   }
}
