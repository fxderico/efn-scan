package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.util.donut.DonutGoliaths;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class GoliathPickerScreen extends Screen {
   private static final float SQUARE = 7.0F;
   private static final float PAD = 14.0F;
   private static final float HEADER_H = 42.0F;
   private static final float INFO_H = 28.0F;
   private static final float BUTTON_W = 96.0F;
   private static final float BUTTON_H = 26.0F;
   private static final int HEADER_BAND = 251658239;
   private static final int WINDOW_FLAGS = 367;
   private final Set<DonutGoliaths.Goliath> picked;
   private final Consumer<Set<DonutGoliaths.Goliath>> onPick;
   private boolean confirmed;
   private boolean dragging;
   private boolean dragAdds;
   private DonutGoliaths.Goliath dragLast;

   public GoliathPickerScreen(Set<DonutGoliaths.Goliath> set, Consumer<Set<DonutGoliaths.Goliath>> consumer) {
      super(Text.literal("Goliath RTP"));
      this.picked = new LinkedHashSet<>(set);
      this.onPick = consumer;
   }

   @Override
   protected void init() {
      super.init();
      ImGuiManager.getInstance().activate(this::draw);
   }

   @Override
   public void removed() {
      super.removed();
      ImGuiManager.getInstance().deactivate();
      this.onPick.accept(this.confirmed ? Set.copyOf(this.picked) : null);
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      if (ThemeStore.get().menuBlur.get()) {
         super.renderBackground(context, mouseX, mouseY, deltaTicks);
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      ImGuiManager.getInstance().requestCursor(context);
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      if (input.key() != 257 && input.key() != 335) {
         return super.keyPressed(input);
      } else {
         this.confirm();
         return true;
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private void confirm() {
      if (!this.picked.isEmpty()) {
         this.confirmed = true;
         this.client.execute(() -> this.close());
      }
   }

   private void draw() {
      float var1 = EfnWidgets.s(252.0F);
      float var2 = EfnWidgets.s(14.0F);
      float var3 = EfnWidgets.s(42.0F);
      float var4 = EfnWidgets.s(28.0F);
      float var5 = EfnWidgets.s(GuiPalette.rounding());
      ImGui.setNextWindowPos(ImGui.getIO().getDisplaySizeX() * 0.5F, ImGui.getIO().getDisplaySizeY() * 0.5F, 1, 0.5F, 0.5F);
      ImGui.pushStyleColor(2, GuiPalette.panelBg());
      ImGui.pushStyleVar(2, 0.0F, 0.0F);
      ImGui.pushStyleVar(14, 0.0F, 0.0F);
      ImGui.pushStyleVar(3, var5);
      ImGui.pushStyleVar(4, 0.0F);
      if (ImGui.begin("##goliath_rtp", 367)) {
         float var6 = ImGui.getWindowPosX();
         float var7 = ImGui.getWindowPosY();
         float var8 = var1 + var2 * 2.0F;
         float var9 = ImGui.getWindowHeight();
         ClickGuiRenderer.drawShadow(var6, var7, var8, var9, var5, 4);
         ImDrawList var10 = ImGui.getWindowDrawList();
         SurfaceBackground.draw(var10, ThemeStore.get().panels, var6, var7, var8, var9, var5);
         boolean var11 = HudChrome.needsTextGuard(ThemeStore.get().panels);
         drawHeader(var10, var6, var7, var8, var3, var5, var11);
         ImGui.dummy(var8, var3);
         ImGui.setCursorPos(var2, var3);
         DonutGoliaths.Goliath var12 = this.drawMap(var10, var1);
         ImFont var13 = ImGuiManager.getInstance().smallFont();
         String var14 = var12 != null
            ? var12.label() + (var12.region() == 2 ? "  \u00b7  no /rtp" : "")
            : (
               this.picked.isEmpty()
                  ? "Click or drag to pick goliaths"
                  : this.picked.size() + (this.picked.size() == 1 ? " goliath picked" : " goliaths picked")
            );
         float var15 = var7 + var3 + var1;
         EfnWidgets.text(var10, var13, var6 + var2, var15 + (var4 - EfnWidgets.lineHeight(var13)) * 0.5F, -4542808, var14, var11);
         float var16 = EfnWidgets.s(96.0F);
         float var17 = EfnWidgets.s(26.0F);
         float var18 = var3 + var1 + var4;
         boolean var19 = ClickGuiRenderer.pushFont(var13);
         ImGui.setCursorPos(var2, var18);
         if (EfnWidgets.chipButton("##goliath_clear", "Clear", var16, var17, false)) {
            this.picked.clear();
         }

         ImGui.setCursorPos(var2 + var1 - var16, var18);
         String var20 = this.picked.isEmpty() ? "Go" : "Go (" + this.picked.size() + ")";
         if (EfnWidgets.chipButton("##goliath_go", var20, var16, var17, !this.picked.isEmpty())) {
            this.confirm();
         }

         if (var19) {
            ImGui.popFont();
         }

         ImGui.setCursorPos(0.0F, var18 + var17);
         ImGui.dummy(var8, var2);
      }

      ImGui.end();
      ImGui.popStyleVar(4);
      ImGui.popStyleColor();
   }

   private static void drawHeader(ImDrawList imDrawList, float f, float g, float h, float i, float j, boolean bl) {
      float var7 = Math.min(i, j + EfnWidgets.s(2.0F));
      int var8 = EfnWidgets.c(EfnWidgets.withAlpha(251658239, 1.0F - var7 / (2.0F * i)));
      int var9 = EfnWidgets.c(EfnWidgets.withAlpha(251658239, 1.0F - var7 / i));
      int var10 = EfnWidgets.c(16777215);
      imDrawList.addRectFilled(f, g, f + h, g + var7, var8, j, 48);
      imDrawList.addRectFilledMultiColor(f, g + var7, f + h, g + i, var9, var9, var10, var10);
      ImFont var11 = ImGuiManager.getInstance().categoryFont();
      float var12 = EfnWidgets.s(7.0F);
      float var13 = f + EfnWidgets.s(14.0F) + var12;
      float var14 = g + i * 0.5F;
      EfnWidgets.grid(imDrawList, var13, var14, var12, -4542808);
      EfnWidgets.text(imDrawList, var11, var13 + var12 + EfnWidgets.s(9.0F), g + (i - EfnWidgets.lineHeight(var11)) * 0.5F, -461582, "Goliath RTP", bl);
   }

   private DonutGoliaths.Goliath drawMap(ImDrawList imDrawList, float f) {
      float var6 = ImGui.getCursorScreenPosX();
      float var7 = ImGui.getCursorScreenPosY();
      ImGui.invisibleButton("##goliath_map", f, f);
      float var8 = f / 36.0F;
      DonutGoliaths.Goliath var9 = ImGui.isItemHovered() ? goliathAt(ImGui.getMousePosX() - var6, ImGui.getMousePosY() - var7, var8) : null;
      boolean var5 = var9 != null && var9.region() != 2;
      if (ImGui.isItemClicked(0) && var5) {
         this.dragAdds = !this.picked.contains(var9);
         this.toggle(var9);
         this.dragging = true;
         this.dragLast = var9;
      } else {
         DonutGoliaths.Goliath var4;
         if (this.dragging
            && ImGui.isMouseDown(0)
            && (var4 = goliathAt(ImGui.getMousePosX() - var6, ImGui.getMousePosY() - var7, var8)) != null
            && var4.region() != 2
            && !var4.equals(this.dragLast)) {
            this.toggle(var4);
            this.dragLast = var4;
         }
      }

      if (!ImGui.isMouseDown(0)) {
         this.dragging = false;
         this.dragLast = null;
      }

      float var11 = ImGuiManager.getInstance().uiScale();
      float var12 = Math.min(EfnWidgets.s(4.0F), var8);
      boolean var13 = !this.picked.isEmpty();

      for (int var3 = 0; var3 < DonutGoliaths.CELLS.length; var3++) {
         int var14 = RegionMapRenderer.regionColor(var3);
         boolean var15 = var3 == 2;
         int[][] var16 = DonutGoliaths.CELLS[var3];

         for (int var17 = 0; var17 < var16.length; var17++) {
            DonutGoliaths.Goliath var19 = new DonutGoliaths.Goliath(var3, var17);
            boolean var18 = !var15 && (this.picked.contains(var19) || var19.equals(var9));
            float var21 = var15 ? 0.3F : (var18 ? 1.0F : (var13 ? 0.45F : 0.85F));
            RegionMapRenderer.tile(imDrawList, var6, var7, var8, var16[var17], HudChrome.scaleAlpha(var14, var21), var12);
         }
      }

      RegionMapRenderer.drawDividers(imDrawList, var6, var7, var8, 1.0F, var11);

      for (int var24 = 0; var24 < DonutGoliaths.CELLS.length; var24++) {
         boolean var25 = var24 == 2;
         int[][] var27 = DonutGoliaths.CELLS[var24];

         for (int var28 = 0; var28 < var27.length; var28++) {
            int[] var30 = var27[var28];
            float var31 = var6 + var30[0] * var8;
            float var33 = var7 + var30[1] * var8;
            float var20 = var6 + var30[2] * var8;
            float var35 = var7 + var30[3] * var8;
            DonutGoliaths.Goliath var22 = new DonutGoliaths.Goliath(var24, var28);
            float var23 = EfnWidgets.s(0.75F);
            if (this.picked.contains(var22)) {
               imDrawList.addRect(var31 + var23, var33 + var23, var20 - var23, var35 - var23, -1, 0.0F, 0, EfnWidgets.s(1.5F));
            } else if (!var25 && var22.equals(var9)) {
               imDrawList.addRect(var31 + var23, var33 + var23, var20 - var23, var35 - var23, -1711276033, 0.0F, 0, EfnWidgets.s(1.0F));
            }

            RegionMapRenderer.drawCenteredTileText(imDrawList, var28 + 1, var31, var33, var20 - var31, var35 - var33, 1.0F, var25 ? 0.5F : 1.0F, var11);
         }
      }

      if (this.client.player != null && this.client.world != null && this.client.world.getRegistryKey() != World.END) {
         double var26 = this.client.world.getRegistryKey() == World.NETHER ? 8.0 : 1.0;
         double var29 = 450000.0;
         double var32 = (this.client.player.getX() * var26 + 225000.0) / var29;
         double var34 = (this.client.player.getZ() * var26 + 225000.0) / var29;
         if (var32 >= 0.0 && var32 < 1.0 && var34 >= 0.0 && var34 < 1.0) {
            float var36 = var6 + (float)(var32 * f);
            float var37 = var7 + (float)(var34 * f);
            imDrawList.addCircleFilled(var36, var37, EfnWidgets.s(3.0F), -13487361);
            imDrawList.addCircle(var36, var37, EfnWidgets.s(3.0F), -1728053248, 16, EfnWidgets.s(1.0F));
         }
      }

      return var9;
   }

   private void toggle(DonutGoliaths.Goliath goliath) {
      if (this.dragAdds) {
         this.picked.add(goliath);
      } else {
         this.picked.remove(goliath);
      }
   }

   private static DonutGoliaths.Goliath goliathAt(float f, float g, float h) {
      return !(f < 0.0F) && !(g < 0.0F) ? DonutGoliaths.at(f / h * 12500.0F - 225000.0F, g / h * 12500.0F - 225000.0F) : null;
   }
}
