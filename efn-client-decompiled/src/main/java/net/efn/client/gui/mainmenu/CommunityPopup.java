package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;
import net.efn.client.config.ThemeStore;
import net.efn.client.gui.HudDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class CommunityPopup {
   public static final String INVITE = "discord.gg/pxHtFp5b6h";
   public static final String INVITE_URL = "https://discord.com/invite/pxHtFp5b6h";
   private static final float OPEN_DELAY = 0.6F;
   private static final float NEXT_DELAY = 0.35F;
   private static final float MARK_RING = 54.0F;
   private static final float MARK_GAP = 14.0F;
   private static final float LINE_GAP = 3.0F;
   private static final float CAPTION_TRACK = 4.0F;
   private static final float CHECK_BOX = 16.0F;
   private static final float CHECK_LABEL_GAP = 9.0F;
   private static final float CHECK_REACH = 5.0F;
   private static final float ROW_GAP = 18.0F;
   private static final float CONTENT_H = 88.0F;
   private static final float[] CLYDE = clydeOutline();
   private static final float CLYDE_EYE_X = 0.332F;
   private static final float CLYDE_EYE_Y = 0.076F;
   private static final float CLYDE_EYE_RX = 0.18F;
   private static final float CLYDE_EYE_RY = 0.2F;
   private static boolean offered;
   private final DuskWidgets ui = new DuskWidgets();
   private final MenuFrame frame = new MenuFrame();
   private DuskWidgets.Dialog dialog;
   private CommunityPopup.Offer offer = CommunityPopup.Offer.DISCORD;
   private boolean dontShowAgain;
   private long openedAt;
   private float closedAt;

   public CommunityPopup() {
   }

   public void update(float var1, boolean var2) {
      if (this.dialog != null && this.dialog.isClosed()) {
         this.dialog = null;
         this.closedAt = var1;
      }

      if (this.dialog == null && var2) {
         if (!offered) {
            if (var1 < 0.6F) {
               return;
            }

            offered = true;
            if (ThemeStore.get().communityPopup.get()) {
               this.open(CommunityPopup.Offer.DISCORD);
            }
         }
      }
   }

   public boolean isOpen() {
      return this.dialog != null && !this.dialog.isClosed();
   }

   public void render(ImDrawList var1, float var2, float var3, float var4, float var5, float var6, float var7, boolean var8) {
      if (this.isOpen()) {
         float var9 = (float)(System.nanoTime() - this.openedAt) / 1.0E9F;
         this.frame.begin(var1, var1, var2, var3, var4, var9, var5, var6, var7, var8);
         this.ui.begin(this.frame);
         this.ui.dialog(this.dialog);
      }
   }

   public boolean keyPressed(int var1) {
      if (!this.isOpen()) {
         return false;
      } else {
         if (var1 == 256) {
            this.dialog.dismiss();
         }

         return true;
      }
   }

   private void open(CommunityPopup.Offer var1) {
      this.offer = var1;
      this.dontShowAgain = false;
      this.openedAt = System.nanoTime();
      this.dialog = new DuskWidgets.Dialog("Join EFN Community", "Updates, configs and support. Come say hi.")
         .content(88.0F, this::drawContent)
         .action("Join", this::join)
         .cancel("Later", this::answered);
   }

   private void join() {
      Util.method_668().method_670(this.offer.url);
      this.answered();
   }

   private void answered() {
      if (this.dontShowAgain) {
         ThemeStore var1 = ThemeStore.get();
         var1.communityPopup.set(false);
         var1.save();
      }
   }

   private void drawContent(DuskWidgets var1, DuskWidgets.Dialog var2, float var3, float var4, float var5, float var6) {
      MenuFrame var7 = var1.frame();
      ImDrawList var8 = var7.bg();
      float var9 = var7.scale();
      float var10 = var1.alpha();
      ImFont var11 = var7.tagFont();
      ImFont var12 = var7.menuFont();
      String var13 = this.offer.caption;
      String var14 = this.offer.link;
      float var15 = 54.0F * var9;
      float var16 = 4.0F * var9;
      float var17 = var7.spacedWidth(var11, var13, var16);
      float var18 = var7.textWidth(var12, var14);
      float var19 = var15 + 14.0F * var9 + Math.max(var17, var18);
      float var20 = var3 + (var5 - var19) * 0.5F;
      boolean var21 = var1.hovered(var20, var4, var19, var15);
      if (var1.pressed(var20, var4, var19, var15)) {
         var2.confirm();
      }

      float var22 = var1.hover("invite", var21);
      float var23 = var20 + var15 * 0.5F;
      float var24 = var4 + var15 * 0.5F - 2.0F * var9;
      float var25 = var15 * 0.44F;
      DuskWidgets.ringMark(var8, var23, var24, var25, var22, var9, var10);
      discordMark(
         var8,
         var23,
         var24,
         var25 * 1.1F,
         HudDraw.col(HudDraw.mix(-6557705, -16777216, var22), var10),
         HudDraw.col(HudDraw.mix(-16381684, -15408912, var22), var10)
      );
      float var26 = var7.textHeight(var11);
      float var27 = var7.textHeight(var12);
      float var28 = var20 + var15 + 14.0F * var9;
      float var29 = var24 - (var26 + 3.0F * var9 + var27) * 0.5F;
      var7.spacedText(var8, var11, var28, var29, var16, -7693914, var10, var13);
      float var30 = var29 + var26 + 3.0F * var9;
      float var31 = var30 + var27 + var9;
      var7.text(var8, var12, var28, var30, HudDraw.mix(-15408912, -6621958, var22), var10, "discord.gg/pxHtFp5b6h");
      if (var22 > 0.01F) {
         var8.addLine(var28, var31, var28 + var18 * var22, var31, HudDraw.col(-6621958, var22 * var10), Math.max(1.0F, var9));
      }

      ImFont var32 = var7.tagFont();
      String var33 = "Don't show again";
      float var34 = 16.0F * var9;
      float var35 = 5.0F * var9;
      float var36 = var34 + 9.0F * var9 + var7.textWidth(var32, var33);
      float var37 = var3 + (var5 - var36) * 0.5F;
      float var38 = var4 + var6 - var34;
      boolean var39 = var1.hovered(var37, var38 - var35, var36, var34 + 2.0F * var35);
      if (var1.pressed(var37, var38 - var35, var36, var34 + 2.0F * var35)) {
         this.dontShowAgain = !this.dontShowAgain;
      }

      float var40 = var1.hover("dont-show-again", var39);
      checkBox(var8, var37, var38, var34, this.dontShowAgain, var40, var9, var10);
      int var41 = HudDraw.mix(this.dontShowAgain ? -6557705 : -7693914, -6621958, var40);
      var7.text(var8, var32, var37 + var34 + 9.0F * var9, var38 + (var34 - var7.textHeight(var32)) * 0.5F + var9, var41, var10, var33);
   }

   private static void checkBox(ImDrawList var0, float var1, float var2, float var3, boolean var4, float var5, float var6, float var7) {
      float var8 = 3.0F * var6;
      if (!var4) {
         var0.addRectFilled(var1, var2, var1 + var3, var2 + var3, HudDraw.col(-16777216, 0.55F * var7), var8);
         var0.addRect(var1, var2, var1 + var3, var2 + var3, HudDraw.col(-15408912, (0.45F + 0.4F * var5) * var7), var8, 0, Math.max(1.0F, 1.2F * var6));
      } else {
         var0.addRectFilled(var1, var2, var1 + var3, var2 + var3, HudDraw.col(HudDraw.mix(-15408912, -6621958, var5), var7), var8);
         var0.pathClear();
         var0.pathLineTo(var1 + var3 * 0.24F, var2 + var3 * 0.52F);
         var0.pathLineTo(var1 + var3 * 0.43F, var2 + var3 * 0.71F);
         var0.pathLineTo(var1 + var3 * 0.77F, var2 + var3 * 0.31F);
         var0.pathStroke(HudDraw.col(-16777216, var7), 0, Math.max(1.4F, 2.0F * var6));
      }
   }

   private static void discordMark(ImDrawList var0, float var1, float var2, float var3, int var4, int var5) {
      float var6 = var3 * 0.5F;
      var0.pathClear();

      for (byte var7 = 0; var7 < CLYDE.length; var7 += 2) {
         var0.pathLineTo(var1 + CLYDE[var7] * var6, var2 + CLYDE[var7 + 1] * var6);
      }

      var0.pathFillConcave(var4);
      byte var14 = 16;

      for (byte var8 = -1; var8 <= 1; var8 += 2) {
         float var9 = var1 + var8 * 0.332F * var6;
         float var10 = var2 + 0.076F * var6;
         var0.pathClear();

         for (int var11 = 0; var11 < var14; var11++) {
            double var12 = var11 * 3.141592653589793 * 2.0 / var14;
            var0.pathLineTo(var9 + (float)Math.cos(var12) * 0.18F * var6, var10 + (float)Math.sin(var12) * 0.2F * var6);
         }

         var0.pathFillConvex(var5);
      }
   }

   private static float[] clydeOutline() {
      byte var1 = 6;
      byte var2 = 4;
      float[] var3 = new float[2 * (4 + var1 + 3 + var2)];
      int var4 = 0;
      var4 = put(var3, var4, 0.0F, -0.667F);
      var4 = put(var3, var4, 0.229F, -0.65F);
      var4 = put(var3, var4, 0.282F, -0.758F);
      var4 = put(var3, var4, 0.694F, -0.631F);

      for (int var0 = 1; var0 <= var1; var0++) {
         float var5 = (float)var0 / var1;
         float var6 = 1.0F - var5;
         float var7 = var6 * var6 * var6;
         float var8 = 3.0F * var6 * var6 * var5;
         float var9 = 3.0F * var6 * var5 * var5;
         float var10 = var5 * var5 * var5;
         var4 = put(var3, var4, var7 * 0.694F + var8 * 0.938F + var9 * 1.03F + var10 * 0.992F, var7 * -0.631F + var8 * -0.272F + var9 * 0.103F + var10 * 0.504F);
      }

      var4 = put(var3, var4, 0.749F, 0.653F);
      var4 = put(var3, var4, 0.486F, 0.758F);
      var4 = put(var3, var4, 0.344F, 0.531F);

      for (int var11 = 1; var11 <= var2; var11++) {
         float var19 = 0.344F * (1.0F - (float)var11 / var2);
         var4 = put(var3, var4, var19, -0.606F + (float)Math.sqrt(1.413721F - var19 * var19));
      }

      int var20 = var4 / 2;
      float[] var21 = new float[2 * (2 * var20 - 2)];
      System.arraycopy(var3, 0, var21, 0, var4);

      for (int var22 = var20 - 2; var22 >= 1; var22--) {
         var4 = put(var21, var4, -var3[2 * var22], var3[2 * var22 + 1]);
      }

      return var21;
   }

   private static int put(float[] var0, int var1, float var2, float var3) {
      var0[var1] = var2;
      var0[var1 + 1] = var3;
      return var1 + 2;
   }

   @Environment(EnvType.CLIENT)
   private static enum Offer {
      DISCORD("DISCORD", "discord.gg/pxHtFp5b6h", "https://discord.gg/pxHtFp5b6h");

      final String caption;
      final String link;
      final String url;

      private Offer(String nullxx, String nullxxx, String nullxxxx) {
         this.caption = nullxx;
         this.link = nullxxx;
         this.url = nullxxxx;
      }
   }
}
