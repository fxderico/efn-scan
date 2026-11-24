package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;
import java.util.List;
import net.efn.client.gui.HudDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CreditsPopup {
   private static final List<String> CREDITS = List.of("EFN", "Meteor Client");
   private static final float ROW_H = 34.0F;
   private static final float DIVIDER_HALF = 56.0F;
   private static final float DIAMOND = 3.0F;
   private final DuskWidgets ui = new DuskWidgets();
   private final MenuFrame frame = new MenuFrame();
   private DuskWidgets.Dialog dialog;
   private long openedAt;

   public CreditsPopup() {
   }

   public void open() {
      this.openedAt = System.nanoTime();
      this.dialog = new DuskWidgets.Dialog("Credits", "").content(CREDITS.size() * 34.0F, this::drawContent).cancel("Close");
   }

   public boolean isOpen() {
      return this.dialog != null && !this.dialog.isClosed();
   }

   public boolean keyPressed(int i) {
      if (!this.isOpen()) {
         return false;
      } else {
         if (i == 256) {
            this.dialog.dismiss();
         }

         return true;
      }
   }

   public void render(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, boolean bl) {
      if (this.isOpen()) {
         float var9 = (float)(System.nanoTime() - this.openedAt) / 1.0E9F;
         this.frame.begin(imDrawList, imDrawList, f, g, h, var9, i, j, k, bl);
         this.ui.begin(this.frame);
         this.ui.dialog(this.dialog);
      }
   }

   private void drawContent(DuskWidgets duskWidgets, DuskWidgets.Dialog dialog, float f, float g, float h, float i) {
      MenuFrame var7 = duskWidgets.frame();
      ImDrawList var8 = var7.bg();
      float var9 = var7.scale();
      float var10 = duskWidgets.alpha();
      ImFont var11 = var7.menuFont();
      float var12 = 34.0F * var9;
      float var13 = f + h * 0.5F;

      for (int var14 = 0; var14 < CREDITS.size(); var14++) {
         String var15 = CREDITS.get(var14);
         float var16 = g + var14 * var12;
         var7.text(var8, var11, var13 - var7.textWidth(var11, var15) * 0.5F, var16 + (var12 - var7.textHeight(var11)) * 0.5F, -6621958, var10, var15);
         if (var14 < CREDITS.size() - 1) {
            divider(var8, var13, var16 + var12, var9, var10);
         }
      }
   }

   private static void divider(ImDrawList imDrawList, float f, float g, float h, float i) {
      float var5 = 56.0F * h;
      float var6 = 3.0F * h;
      float var7 = var6 * 2.0F;
      float var8 = Math.max(1.0F, h);
      int var9 = HudDraw.col(-15408912, 0.4F * i);
      imDrawList.addLine(f - var5, g, f - var7, g, var9, var8);
      imDrawList.addLine(f + var7, g, f + var5, g, var9, var8);
      imDrawList.addQuadFilled(f, g - var6, f + var6, g, f, g + var6, f - var6, g, HudDraw.col(-15408912, 0.75F * i));
   }
}
