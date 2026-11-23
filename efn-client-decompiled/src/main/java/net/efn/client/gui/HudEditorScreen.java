package net.efn.client.gui;

import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class HudEditorScreen extends Screen {
   public HudEditorScreen() {
      super(Text.method_43470("EFN HUD Layout"));
   }

   public void method_25432() {
      super.method_25432();
      HudLayoutStore.getInstance().save();
   }

   public void method_25419() {
      HudLayoutStore.getInstance().save();
      MinecraftClient.method_1551().method_1507(null);
   }

   public void method_25420(DrawContext var1, int var2, int var3, float var4) {
   }

   public void method_25394(DrawContext var1, int var2, int var3, float var4) {
      ImGuiManager.getInstance().requestCursor(var1);
   }

   public boolean method_25421() {
      return false;
   }
}
