package net.efn.client.gui.mainmenu;

import com.example.client.mixin.AccessorMenusJoinMultiplayerScreen;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import net.efn.client.account.Account;
import net.efn.client.account.Accounts;
import net.efn.client.gui.HudDraw;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.proxy.Proxies;
import net.efn.client.proxy.Proxy;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.util.Window;

@Environment(EnvType.CLIENT)
public final class MultiplayerOverlay {
   private static final int HEADER_HEIGHT = 33;
   private static final float NOWHERE = -10000.0F;
   private static final float BUTTON_H = 62.0F;
   private static final float ICON = 34.0F;
   private static final float MARGIN = 16.0F;
   private final MultiplayerScreen screen;
   private final ConnectionsPanel panel;
   private final DuskWidgets ui = new DuskWidgets();
   private final MenuFrame frame = new MenuFrame();
   private long shownAt;
   private boolean overButton;

   public MultiplayerOverlay(MultiplayerScreen var1) {
      this.screen = var1;
      MinecraftClient var2 = MinecraftClient.method_1551();
      this.panel = new ConnectionsPanel(() -> var2.method_63588(((AccessorMenusJoinMultiplayerScreen)var1)::efn$refreshServerList));
   }

   public void show() {
      this.shownAt = System.nanoTime();
      ImGuiManager.getInstance().activate(this::render);
   }

   public void hide() {
      ImGuiManager.getInstance().deactivate();
   }

   public boolean takesPointer() {
      return this.overButton || this.panel.isOpen();
   }

   public boolean keyPressed(int var1) {
      return this.panel.keyPressed(var1);
   }

   private void render() {
      ImGuiIO var1 = ImGui.getIO();
      float var2 = var1.getDisplaySizeX();
      float var3 = var1.getDisplaySizeY();
      float var4 = ImGuiManager.getInstance().uiScale();
      float var5 = Math.max(0.001F, var1.getDeltaTime());
      boolean var6 = !this.panel.isOpen();
      this.frame
         .begin(
            ImGui.getBackgroundDrawList(),
            ImGui.getForegroundDrawList(),
            var2,
            var3,
            var4,
            (float)(System.nanoTime() - this.shownAt) / 1.0E9F,
            var5,
            var6 ? var1.getMousePosX() : -10000.0F,
            var6 ? var1.getMousePosY() : -10000.0F,
            var6 && ImGui.isMouseClicked(0)
         );
      this.ui.begin(this.frame);

      try {
         this.header();
      } finally {
         this.ui.end();
      }

      this.panel.render(var2, var3, var4, var5);
   }

   private void header() {
      Window var1 = MinecraftClient.method_1551().method_22683();
      double var2 = var1.method_4495();
      float var4 = this.frame.scale();
      float var5 = this.frame.intro(0.0F);
      float var6 = 62.0F * var4;
      float var7 = 16.0F * var4;
      float var8 = (float)(33.0 * var2);
      float var9 = Math.round((var8 - var6) * 0.5F);
      ImFont var10 = this.frame.menuFont();
      float var11 = Math.max(this.frame.textWidth(var10, "Accounts"), this.frame.textWidth(var10, "Proxies")) + 82.0F * var4;
      Proxy var12 = Proxies.get().enabled();
      boolean var14 = this.button(ConnectionsPanel.Tab.ACCOUNTS, "Accounts", null, var7, var9, var11, var6, var4, var5);
      this.overButton = var14 | this.button(ConnectionsPanel.Tab.PROXIES, "Proxies", var12, var7 + var11 + 8.0F * var4, var9, var11, var6, var4, var5);
      float var15 = (float)((var1.method_4486() + MinecraftClient.method_1551().field_1772.method_27525(this.screen.method_25440())) / 2.0 * var2);
      float var16 = this.frame.width() - var7;
      float var17 = var16 - var15 - 3.0F * var7;
      if (!(var17 < 40.0F * var4)) {
         ImFont var18 = this.frame.menuFont();
         ImFont var19 = this.frame.listFont();
         float var20 = 4.0F * var4;
         float var21 = this.frame.textHeight(var18) + var20 + this.frame.textHeight(var19);
         float var22 = Math.round((var8 - var21) * 0.5F);
         String var23 = DuskWidgets.fit(var18, this.frame.displayName(), var17);
         String var24 = DuskWidgets.fit(var19, var12 != null ? var12.displayAddress() : "No proxy", var17);
         ImDrawList var25 = this.frame.bg();
         this.frame.text(var25, var18, var16 - this.frame.textWidth(var18, var23), var22, -1575946, var5, var23);
         this.frame
            .text(
               var25,
               var19,
               var16 - this.frame.textWidth(var19, var24),
               var22 + this.frame.textHeight(var18) + var20,
               var12 != null ? -6557705 : -7693914,
               var5,
               var24
            );
      }
   }

   private boolean button(ConnectionsPanel.Tab var1, String var2, Proxy var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      ImDrawList var10 = this.frame.bg();
      boolean var11 = this.ui.hovered(var4, var5, var6, var7);
      if (this.ui.pressed(var4, var5, var6, var7)) {
         this.panel.open(var1);
      }

      float var12 = this.ui.hover(var1, var11);
      var10.addRectFilled(var4, var5, var4 + var6, var5 + var7, HudDraw.col(-15461610, 0.88F * var9));
      if (var12 > 0.01F) {
         var10.addRectFilled(var4, var5, var4 + var6, var5 + var7, HudDraw.col(-1575946, 0.06F * var12 * var9));
      }

      var10.addRect(
         var4,
         var5,
         var4 + var6,
         var5 + var7,
         HudDraw.col(HudDraw.mix(-1575946, -15408912, var12), (0.18F + 0.4F * var12) * var9),
         0.0F,
         0,
         Math.max(1.0F, var8)
      );
      float var13 = 34.0F * var8;
      float var14 = var4 + 16.0F * var8;
      float var15 = var5 + (var7 - var13) * 0.5F;
      if (var1 == ConnectionsPanel.Tab.ACCOUNTS) {
         Account var16 = Accounts.get().active();
         if (var16 != null) {
            PlayerFaces.draw(var10, var16, var14, var15, var13, var8, var9);
         } else {
            PlayerFaces.draw(var10, MinecraftClient.method_1551().method_1548().method_44717(), false, var14, var15, var13, var8, var9);
         }
      } else {
         ConnectionsPanel.proxyDot(var10, var14 + var13 * 0.5F, var15 + var13 * 0.5F, var3 != null, 2.1F * var8, var9);
      }

      ImFont var17 = this.frame.menuFont();
      this.frame
         .text(
            var10, var17, var14 + var13 + 12.0F * var8, var5 + (var7 - this.frame.textHeight(var17)) * 0.5F, HudDraw.mix(-1575946, -6621958, var12), var9, var2
         );
      return var11;
   }
}
