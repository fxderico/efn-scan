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
      super(Text.literal("EFN HUD Layout"));
   }

   @Override
   public void removed() {
      super.removed();
      HudLayoutStore.getInstance().save();
   }

   @Override
   public void close() {
      HudLayoutStore.getInstance().save();
      MinecraftClient.getInstance().setScreen(null);
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      ImGuiManager.getInstance().requestCursor(context);
   }

   @Override
   public boolean shouldPause() {
      return false;
   }
}
