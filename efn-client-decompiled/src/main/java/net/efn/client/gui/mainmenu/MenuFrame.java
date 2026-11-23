package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;
import net.efn.client.gui.HudDraw;
import net.efn.client.gui.ThemeImages;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.util.NameFilter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class MenuFrame {
   public static final String HEAD = "/assets/efn/textures/gui/head.png";
   private ImDrawList bg;
   private ImDrawList fg;
   private float width;
   private float height;
   private float scale;
   private float time;
   private float dt;
   private float mouseX;
   private float mouseY;
   private boolean clicked;
   private String tooltip;
   private float tooltipX;
   private float tooltipY;

   public MenuFrame() {
   }

   public void begin(ImDrawList var1, ImDrawList var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, boolean var10) {
      this.bg = var1;
      this.fg = var2;
      this.width = var3;
      this.height = var4;
      this.scale = var5;
      this.time = var6;
      this.dt = var7;
      this.mouseX = var8;
      this.mouseY = var9;
      this.clicked = var10;
      this.tooltip = null;
   }

   public ImDrawList bg() {
      return this.bg;
   }

   public ImDrawList fg() {
      return this.fg;
   }

   public float width() {
      return this.width;
   }

   public float height() {
      return this.height;
   }

   public float scale() {
      return this.scale;
   }

   public float px(float var1) {
      return var1 * this.scale;
   }

   public float time() {
      return this.time;
   }

   public float dt() {
      return this.dt;
   }

   public float mouseX() {
      return this.mouseX;
   }

   public float mouseY() {
      return this.mouseY;
   }

   public boolean clicked() {
      return this.clicked;
   }

   public float intro(float var1) {
      return HudDraw.intro(this.time, var1);
   }

   public boolean inside(float var1, float var2, float var3, float var4) {
      return this.mouseX >= var1 && this.mouseX <= var1 + var3 && this.mouseY >= var2 && this.mouseY <= var2 + var4;
   }

   public void tooltip(String var1, float var2, float var3) {
      this.tooltip = var1;
      this.tooltipX = var2;
      this.tooltipY = var3;
   }

   public String pendingTooltip() {
      return this.tooltip;
   }

   public float tooltipX() {
      return this.tooltipX;
   }

   public float tooltipY() {
      return this.tooltipY;
   }

   public static int col(int var0, float var1) {
      return HudDraw.col(var0, var1);
   }

   public static int col(int var0) {
      return HudDraw.col(var0, 1.0F);
   }

   public static int mix(int var0, int var1, float var2) {
      return HudDraw.mix(var0, var1, var2);
   }

   public ImFont menuFont() {
      return ImGuiManager.getInstance().menuFont();
   }

   public ImFont bodyFont() {
      return ImGuiManager.getInstance().menuBodyFont();
   }

   public ImFont tagFont() {
      return ImGuiManager.getInstance().menuTagFont();
   }

   public ImFont nameFont() {
      return ImGuiManager.getInstance().menuNameFont();
   }

   public ImFont itemFont() {
      return ImGuiManager.getInstance().menuItemFont();
   }

   public ImFont listFont() {
      return ImGuiManager.getInstance().moduleFont();
   }

   public ImFont detailFont() {
      return ImGuiManager.getInstance().hudLabelFont();
   }

   public void text(ImDrawList var1, ImFont var2, float var3, float var4, int var5, float var6, String var7) {
      HudDraw.text(var1, var2, var3, var4, var7, col(var5, var6));
   }

   public float textWidth(ImFont var1, String var2) {
      return HudDraw.textWidth(var1, var2);
   }

   public float textHeight(ImFont var1) {
      return var1 != null ? var1.getFontSize() : this.px(15.0F);
   }

   public float spacedWidth(ImFont var1, String var2, float var3) {
      return HudDraw.spacedWidth(var1, var2, var3);
   }

   public void spacedText(ImDrawList var1, ImFont var2, float var3, float var4, float var5, int var6, float var7, String var8) {
      for (int var9 = 0; var9 < var8.length(); var9++) {
         String var10 = String.valueOf(var8.charAt(var9));
         HudDraw.text(var1, var2, var3, var4, var10, col(var6, var7));
         var3 += this.textWidth(var2, var10) + var5;
      }
   }

   public ThemeImages.Texture head() {
      return MenuTextures.original("/assets/efn/textures/gui/head.png");
   }

   public ThemeImages.Texture headMask() {
      return MenuTextures.mask("/assets/efn/textures/gui/head.png");
   }

   public ThemeImages.Texture headBlur(int var1) {
      return MenuTextures.blurred("/assets/efn/textures/gui/head.png", var1);
   }

   public void image(ImDrawList var1, ThemeImages.Texture var2, float var3, float var4, float var5, float var6, int var7, float var8) {
      if (var2 != null) {
         var1.addImage(var2.id(), var3, var4, var5, var6, 0.0F, 0.0F, 1.0F, 1.0F, col(var7, var8));
      }
   }

   public String displayName() {
      return NameFilter.apply(MinecraftClient.method_1551().method_1548().method_1676());
   }
}
