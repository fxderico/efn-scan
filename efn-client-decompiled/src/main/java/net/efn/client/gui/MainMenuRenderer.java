package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import net.efn.client.gui.mainmenu.CommunityPopup;
import net.efn.client.gui.mainmenu.ConnectionsPanel;
import net.efn.client.gui.mainmenu.CreditsPopup;
import net.efn.client.gui.mainmenu.EfnMenu;
import net.efn.client.gui.mainmenu.MenuContext;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class MainMenuRenderer {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final float NOWHERE = -10000.0F;
   private final MenuContext context;
   private final CommunityPopup popup = new CommunityPopup();
   private final ConnectionsPanel accounts = new ConnectionsPanel(() -> {});
   private final CreditsPopup credits = new CreditsPopup();
   private long openedAt = System.nanoTime();
   private boolean failed;

   MainMenuRenderer(Screen var1) {
      this.context = new MenuContext(var1, () -> this.accounts.open(ConnectionsPanel.Tab.ACCOUNTS), this.credits::open);
   }

   boolean keyPressed(int var1) {
      return this.popup.keyPressed(var1) || this.credits.keyPressed(var1) || this.accounts.keyPressed(var1);
   }

   void render() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      long var2 = System.nanoTime();
      if (var1.method_18506() != null) {
         this.openedAt = var2;
      }

      ImGuiIO var4 = ImGui.getIO();
      boolean var5 = var1.method_18506() == null;
      float var6 = (float)(var2 - this.openedAt) / 1.0E9F;
      float var7 = var4.getMousePosX();
      float var8 = var4.getMousePosY();
      boolean var9 = var5 && ImGui.isMouseClicked(0);
      float var10 = var4.getDisplaySizeX();
      float var11 = var4.getDisplaySizeY();
      float var12 = ImGuiManager.getInstance().uiScale();
      float var13 = Math.max(0.001F, var4.getDeltaTime());
      this.popup.update(var6, var5 && !this.accounts.isOpen() && !this.credits.isOpen());
      boolean var14 = !this.accounts.isOpen() && !this.credits.isOpen() && !this.popup.isOpen();
      this.context
         .begin(
            ImGui.getBackgroundDrawList(),
            ImGui.getForegroundDrawList(),
            var10,
            var11,
            var12,
            var6,
            var13,
            var14 ? var7 : -10000.0F,
            var14 ? var8 : -10000.0F,
            var14 && var9
         );

      try {
         EfnMenu.render(this.context);
      } catch (RuntimeException var16) {
         if (!this.failed) {
            LOGGER.error("[EFN] Main menu failed to render", var16);
         }

         this.failed = true;
         return;
      }

      if (this.context.pendingTooltip() != null) {
         drawTooltip(this.context);
      }

      this.accounts.render(var10, var11, var12, var13);
      this.credits.render(ImGui.getForegroundDrawList(), var10, var11, var12, var13, var7, var8, var9);
      this.popup.render(ImGui.getForegroundDrawList(), var10, var11, var12, var13, var7, var8, var9);
   }

   private static void drawTooltip(MenuContext var0) {
      ImDrawList var1 = var0.fg();
      ImFont var2 = var0.tagFont();
      String var3 = var0.pendingTooltip();
      float var4 = var0.textHeight(var2);
      float var5 = var0.textWidth(var2, var3) + var0.px(18.0F);
      float var6 = var4 + var0.px(9.0F);
      float var7 = var0.tooltipX() - var5 * 0.5F;
      float var8 = var0.tooltipY();
      var1.addRectFilled(var7, var8, var7 + var5, var8 + var6, MenuContext.col(-16053492, 0.96F), var0.px(3.0F));
      var1.addRect(var7, var8, var7 + var5, var8 + var6, MenuContext.col(-1, 0.25F), var0.px(3.0F), 0, Math.max(1.0F, var0.scale()));
      var0.text(var1, var2, var7 + var0.px(9.0F), var8 + var0.px(4.5F), -921103, 1.0F, var3);
   }
}
