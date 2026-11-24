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

   public void begin(ImDrawList imDrawList, ImDrawList imDrawList2, float f, float g, float h, float i, float j, float k, float l, boolean bl) {
      this.bg = imDrawList;
      this.fg = imDrawList2;
      this.width = f;
      this.height = g;
      this.scale = h;
      this.time = i;
      this.dt = j;
      this.mouseX = k;
      this.mouseY = l;
      this.clicked = bl;
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

   public float px(float f) {
      return f * this.scale;
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

   public float intro(float f) {
      return HudDraw.intro(this.time, f);
   }

   public boolean inside(float f, float g, float h, float i) {
      return this.mouseX >= f && this.mouseX <= f + h && this.mouseY >= g && this.mouseY <= g + i;
   }

   public void tooltip(String string, float f, float g) {
      this.tooltip = string;
      this.tooltipX = f;
      this.tooltipY = g;
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

   public static int col(int i, float f) {
      return HudDraw.col(i, f);
   }

   public static int col(int i) {
      return HudDraw.col(i, 1.0F);
   }

   public static int mix(int i, int j, float f) {
      return HudDraw.mix(i, j, f);
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

   public void text(ImDrawList imDrawList, ImFont imFont, float f, float g, int i, float h, String string) {
      HudDraw.text(imDrawList, imFont, f, g, string, col(i, h));
   }

   public float textWidth(ImFont imFont, String string) {
      return HudDraw.textWidth(imFont, string);
   }

   public float textHeight(ImFont imFont) {
      return imFont != null ? imFont.getFontSize() : this.px(15.0F);
   }

   public float spacedWidth(ImFont imFont, String string, float f) {
      return HudDraw.spacedWidth(imFont, string, f);
   }

   public void spacedText(ImDrawList imDrawList, ImFont imFont, float f, float g, float h, int i, float j, String string) {
      for (int var9 = 0; var9 < string.length(); var9++) {
         String var10 = String.valueOf(string.charAt(var9));
         HudDraw.text(imDrawList, imFont, f, g, var10, col(i, j));
         f += this.textWidth(imFont, var10) + h;
      }
   }

   public ThemeImages.Texture head() {
      return MenuTextures.original("/assets/efn/textures/gui/head.png");
   }

   public ThemeImages.Texture headMask() {
      return MenuTextures.mask("/assets/efn/textures/gui/head.png");
   }

   public ThemeImages.Texture headBlur(int i) {
      return MenuTextures.blurred("/assets/efn/textures/gui/head.png", i);
   }

   public void image(ImDrawList imDrawList, ThemeImages.Texture texture, float f, float g, float h, float i, int j, float k) {
      if (texture != null) {
         imDrawList.addImage(texture.id(), f, g, h, i, 0.0F, 0.0F, 1.0F, 1.0F, col(j, k));
      }
   }

   public String displayName() {
      return NameFilter.apply(MinecraftClient.getInstance().getSession().getUsername());
   }
}
