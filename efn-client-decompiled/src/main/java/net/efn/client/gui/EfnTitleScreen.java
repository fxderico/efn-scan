package net.efn.client.gui;

import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.input.KeyInput;

@Environment(EnvType.CLIENT)
public final class EfnTitleScreen extends TitleScreen {
   private final MainMenuRenderer menu = new MainMenuRenderer(this);

   public EfnTitleScreen() {
   }

   public static boolean enabled() {
      return ThemeStore.get().customMainMenu.get();
   }

   @Override
   protected void init() {
      ImGuiManager.getInstance().activate(this.menu::render);
   }

   @Override
   public void tick() {
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      return this.menu.keyPressed(input.key()) || super.keyPressed(input);
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      return false;
   }

   @Override
   public void removed() {
      super.removed();
      ImGuiManager.getInstance().deactivate();
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      ImGuiManager.getInstance().requestCursor(context);
      context.drawDeferredElements();
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }
}
