package net.efn.client.imgui;

import com.mojang.blaze3d.opengl.GlStateManager;
import imgui.EfnAndroidNative;
import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImFontGlyphRangesBuilder;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.EfnGl3;
import imgui.glfw.EfnGlfw;
import imgui.glfw.EfnKeys;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import net.efn.client.config.HudTheme;
import net.efn.client.config.ThemeStore;
import net.efn.client.gui.AndroidSystemPicker;
import net.efn.client.gui.GuiPalette;
import net.efn.client.gui.ThemeImages;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.Cursor;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMonitorCallback;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@Environment(EnvType.CLIENT)
public final class ImGuiManager {
   private static final ImGuiManager INSTANCE = new ImGuiManager();
   private static final String MODULE_FONT_RESOURCE = "/assets/efn/fonts/geist-medium.ttf";
   private static final String MENU_FONT_RESOURCE = "/assets/efn/fonts/rajdhani-bold.ttf";
   public static final float MENU_FONT_SIZE = 24.0F;
   public static final float MENU_TAG_FONT_SIZE = 15.0F;
   public static final float MENU_BODY_FONT_SIZE = 18.0F;
   public static final float MENU_NAME_FONT_SIZE = 20.0F;
   public static final float MENU_ITEM_FONT_SIZE = 32.0F;
   public static final float HUD_VALUE_FONT_SIZE = 18.0F;
   public static final float HUD_TEXT_FONT_SIZE = 17.0F;
   public static final float HUD_LABEL_FONT_SIZE = 14.0F;
   private static final String EXTENDED_LATIN_GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\u00c7\u011e\u0130\u00d6\u015e\u00dc\u00e7\u011f\u0131\u00f6\u015f\u00fc\u00c2\u00e2\u00ca\u00ea\u00ce\u00ee\u00d4\u00f4\u00db\u00fb\u00c0\u00c1\u00c4\u00c8\u00c9\u00cb\u00cc\u00cd\u00cf\u00d2\u00d3\u00d6\u00d9\u00da\u00dc\u00dd\u00e0\u00e1\u00e4\u00e8\u00e9\u00eb\u00ec\u00ed\u00ef\u00f2\u00f3\u00f6\u00f9\u00fa\u00fc\u00fd\u00ff\u2212\u221e";
   private static final float REFERENCE_HEIGHT = 1080.0F;
   private final EfnGlfw glfw = new EfnGlfw() {
      public void keyCallback(long var1, int var3, int var4, int var5, int var6) {
         EfnKeys.record(var1, var3, var5);
         boolean var8 = var3 >= 0 && var3 < ImGuiManager.this.imguiKeysDown.length;
         boolean var7 = var5 == 0 ? var8 && ImGuiManager.this.imguiKeysDown[var3] : ImGuiManager.this.feedsKeyPress(var3, var6);
         if (!var7) {
            if (ImGuiManager.this.minecraftKeys != null) {
               ImGuiManager.this.minecraftKeys.invoke(var1, var3, var4, var5, var6);
            }
         } else {
            if (var8) {
               ImGuiManager.this.imguiKeysDown[var3] = var5 != 0;
            }

            super.keyCallback(var1, var3, var4, var5, var6);
         }
      }

      public void monitorCallback(long var1, int var3) {
         super.monitorCallback(var1, var3);
         if (ImGuiManager.this.minecraftMonitors != null) {
            ImGuiManager.this.minecraftMonitors.invoke(var1, var3);
         }
      }

      public void cursorPosCallback(long var1, double var3, double var5) {
         super.cursorPosCallback(var1, var3, var5);
         ImGuiManager.feedScaledCursor(var3, var5);
      }

      protected void updateMouseData() {
         super.updateMouseData();
         long var1 = MinecraftClient.method_1551().method_22683().method_4490();
         if (GLFW.glfwGetWindowAttrib(var1, 131073) != 0) {
            double[] var3 = new double[1];
            double[] var4 = new double[1];
            GLFW.glfwGetCursorPos(var1, var3, var4);
            ImGuiManager.feedScaledCursor(var3[0], var4[0]);
         }
      }
   };
   private final EfnGl3 gl3 = new EfnGl3();
   private final boolean[] imguiKeysDown = new boolean[349];
   private GLFWKeyCallback minecraftKeys;
   private GLFWMonitorCallback minecraftMonitors;
   private boolean initialized;
   private boolean idleSkipped;
   private Runnable activeRenderer;
   private Runnable hudRenderer;
   private BooleanSupplier hudHasWork = () -> true;
   private Runnable toastRenderer;
   private BooleanSupplier toastHasWork = () -> false;
   private Runnable overlayRenderer;
   private Runnable streamRenderer;
   private BooleanSupplier streamHasWork = () -> false;
   private BooleanSupplier overlayHasWork = () -> false;
   private Runnable alertRenderer;
   private BooleanSupplier alertHasWork = () -> false;
   private float uiScale = 1.0F;
   private float userScale = 1.0F;
   private UiFont uiFont = UiFont.GEIST;
   private UiFont hudFont = UiFont.GEIST;
   private int frameCount;
   private float frameDelta;
   private ImFont categoryFont;
   private ImFont moduleFont;
   private ImFont titleFont;
   private ImFont smallFont;
   private ImFont hudValueFont;
   private ImFont hudTextFont;
   private ImFont hudLabelFont;
   private ImFont menuFont;
   private ImFont menuTagFont;
   private ImFont menuBodyFont;
   private ImFont menuNameFont;
   private ImFont menuItemFont;
   private short[] glyphRanges;

   private static void feedScaledCursor(double var0, double var2) {
      float var4 = pixelsPerPointX();
      float var5 = pixelsPerPointY();
      if (var4 != 1.0F || var5 != 1.0F) {
         ImGui.getIO().addMousePosEvent((float)(var0 * var4), (float)(var2 * var5));
      }
   }

   private static float pixelsPerPointX() {
      Window var0 = MinecraftClient.method_1551().method_22683();
      return var0.method_4480() > 0 ? (float)var0.method_4489() / var0.method_4480() : 1.0F;
   }

   private static float pixelsPerPointY() {
      Window var0 = MinecraftClient.method_1551().method_22683();
      return var0.method_4507() > 0 ? (float)var0.method_4506() / var0.method_4507() : 1.0F;
   }

   private ImGuiManager() {
   }

   public static ImGuiManager getInstance() {
      return INSTANCE;
   }

   public ImFont categoryFont() {
      return this.categoryFont;
   }

   public ImFont moduleFont() {
      return this.moduleFont;
   }

   public ImFont titleFont() {
      return this.titleFont;
   }

   public ImFont smallFont() {
      return this.smallFont;
   }

   public ImFont hudValueFont() {
      return this.hudValueFont;
   }

   public ImFont hudTextFont() {
      return this.hudTextFont;
   }

   public ImFont hudLabelFont() {
      return this.hudLabelFont;
   }

   public ImFont menuFont() {
      return this.menuFont;
   }

   public ImFont menuTagFont() {
      return this.menuTagFont;
   }

   public ImFont menuBodyFont() {
      return this.menuBodyFont;
   }

   public ImFont menuNameFont() {
      return this.menuNameFont;
   }

   public ImFont menuItemFont() {
      return this.menuItemFont;
   }

   public float uiScale() {
      return this.uiScale;
   }

   public int frameCount() {
      return this.frameCount;
   }

   public float frameDelta() {
      return this.frameDelta;
   }

   public void activate(Runnable var1) {
      this.activeRenderer = var1;
   }

   public boolean typing() {
      return this.initialized && ImGui.getIO().getWantTextInput();
   }

   public void requestCursor(DrawContext var1) {
      if (this.initialized) {
         Cursor var2 = switch (ImGui.getMouseCursor()) {
            case 1 -> StandardCursors.field_62453;
            case 2 -> StandardCursors.field_62458;
            case 3 -> StandardCursors.field_62456;
            case 4 -> StandardCursors.field_62457;
            default -> null;
            case 7 -> StandardCursors.field_62455;
            case 8 -> StandardCursors.field_62459;
         };
         if (var2 != null) {
            var1.method_74037(var2);
         }
      }
   }

   private boolean feedsKeyPress(int var1, int var2) {
      return this.activeRenderer != null && (var1 != 258 || (var2 & 2) == 0);
   }

   public void deactivate() {
      this.activeRenderer = null;
      String var1;
      if (this.initialized && (var1 = ImGui.getIO().getIniFilename()) != null) {
         ImGui.saveIniSettingsToDisk(var1);
      }
   }

   public void setHudRenderer(Runnable var1, BooleanSupplier var2) {
      this.hudRenderer = var1;
      this.hudHasWork = var2;
   }

   public void setToastRenderer(Runnable var1, BooleanSupplier var2) {
      this.toastRenderer = var1;
      this.toastHasWork = var2;
   }

   public void setStreamRenderer(Runnable var1, BooleanSupplier var2) {
      this.streamRenderer = var1;
      this.streamHasWork = var2;
   }

   public void setOverlayRenderer(Runnable var1, BooleanSupplier var2) {
      this.overlayRenderer = var1;
      this.overlayHasWork = var2;
   }

   public void setAlertRenderer(Runnable var1, BooleanSupplier var2) {
      this.alertRenderer = var1;
      this.alertHasWork = var2;
   }

   public void renderIfActive() {
      boolean var2 = this.hudRenderer != null && this.hudHasWork.getAsBoolean();
      boolean var3 = this.toastRenderer != null && this.toastHasWork.getAsBoolean();
      boolean var4 = this.overlayRenderer != null && this.overlayHasWork.getAsBoolean();
      boolean var5 = this.alertRenderer != null && this.alertHasWork.getAsBoolean();
      boolean var1 = this.streamRenderer != null && this.streamHasWork.getAsBoolean();
      if (this.activeRenderer == null && !var2 && !var3 && !var4 && !var5 && !var1) {
         if (this.initialized) {
            ImGui.getIO().clearEventsQueue();
            this.idleSkipped = true;
         }
      } else {
         this.initIfNeeded();
         this.rebuildFontsIfNeeded();
         if (this.idleSkipped) {
            ImGuiIO var7 = ImGui.getIO();
            var7.clearInputKeys();
            var7.clearInputMouse();
            Arrays.fill(this.imguiKeysDown, false);
            this.idleSkipped = false;
         }

         MinecraftClient var25 = MinecraftClient.method_1551();
         int var8 = var25.method_22683().method_4489();
         int var9 = var25.method_22683().method_4506();
         GL30.glBindFramebuffer(36160, 0);
         GL11.glViewport(0, 0, var8, var9);
         GlStateManager._viewport(0, 0, var8, var9);
         this.glfw.newFrame();
         this.frameDelta = Math.min(ImGui.getIO().getDeltaTime(), 0.1F);
         ImGui.getIO().setDeltaTime(this.frameDelta);
         this.updateDisplayMetrics();
         this.gl3.newFrame();
         ThemeImages.get().beginFrame();
         ImGui.newFrame();
         this.frameCount++;
         boolean var10 = false;

         try {
            GuiPalette.refresh();
            if (this.streamRenderer != null) {
               this.streamRenderer.run();
            }

            if (this.hudRenderer != null) {
               this.hudRenderer.run();
            }

            if (this.activeRenderer != null) {
               this.activeRenderer.run();
            }

            if (this.toastRenderer != null) {
               this.toastRenderer.run();
            }

            if (this.alertRenderer != null) {
               this.alertRenderer.run();
            }

            if (this.overlayRenderer != null) {
               this.overlayRenderer.run();
            }

            ImGui.render();
            var10 = true;
            this.gl3.renderDrawData(ImGui.getDrawData());
         } finally {
            try {
               if (!var10) {
                  ImGui.endFrame();
               }
            } finally {
               ThemeImages.get().endFrame();
            }
         }
      }
   }

   public void dispose() {
      AndroidSystemPicker.cancel();
      if (this.initialized) {
         this.gl3.shutdown();
         this.glfw.shutdown();
         ImGui.destroyContext();
         this.categoryFont = null;
         this.moduleFont = null;
         this.titleFont = null;
         this.smallFont = null;
         this.hudValueFont = null;
         this.hudTextFont = null;
         this.hudLabelFont = null;
         this.menuFont = null;
         this.menuTagFont = null;
         this.menuBodyFont = null;
         this.menuNameFont = null;
         this.menuItemFont = null;
         this.initialized = false;
      }
   }

   private void initIfNeeded() {
      if (!this.initialized) {
         long var1 = MinecraftClient.method_1551().method_22683().method_4490();
         ImGui.createContext();
         ImGuiIO var3 = ImGui.getIO();
         var3.setIniFilename(iniPath());
         var3.addConfigFlags(1);
         var3.addConfigFlags(32);
         ImGui.styleColorsDark();
         this.uiScale = this.computeScale(this.uiScale);
         this.uiFont = chosenUiFont();
         this.hudFont = chosenHudFont();
         this.loadFonts(var3);
         this.minecraftKeys = GLFW.glfwSetKeyCallback(var1, null);
         GLFW.glfwSetKeyCallback(var1, this.minecraftKeys);
         this.minecraftMonitors = GLFW.glfwSetMonitorCallback(null);
         GLFW.glfwSetMonitorCallback(this.minecraftMonitors);
         this.glfw.init(var1, true);
         this.gl3.init(null);
         this.initialized = true;
      }
   }

   private void rebuildFontsIfNeeded() {
      float var1 = this.computeScale(this.uiScale);
      UiFont var2 = chosenUiFont();
      UiFont var3 = chosenHudFont();
      if (var1 != this.uiScale || var2 != this.uiFont || var3 != this.hudFont) {
         this.uiScale = var1;
         this.uiFont = var2;
         this.hudFont = var3;
         ImGuiIO var4 = ImGui.getIO();
         var4.getFonts().clear();
         this.loadFonts(var4);
         this.gl3.destroyFontsTexture();
         this.gl3.createFontsTexture();
      }
   }

   private static UiFont chosenUiFont() {
      return UiFont.of(ThemeStore.get().font.get());
   }

   private static UiFont chosenHudFont() {
      return UiFont.of(HudTheme.get().font.get());
   }

   private float computeScale(float var1) {
      int var2 = MinecraftClient.method_1551().method_22683().method_4506();
      if (var2 < 200) {
         return var1;
      } else {
         if (!this.initialized || !ImGui.getIO().getMouseDown(0)) {
            this.userScale = ThemeStore.get().uiScale.get();
         }

         float var3 = Math.max(0.6F, Math.min(3.5F, var2 / 1080.0F * this.userScale));
         return Math.round(var3 * 20.0F) / 20.0F;
      }
   }

   private static String iniPath() {
      try {
         Path var0 = FabricLoader.getInstance().getConfigDir().resolve("efn");
         Files.createDirectories(var0);
         return var0.resolve("gui.ini").toString();
      } catch (RuntimeException | IOException var1) {
         System.err.println("[EFN] GUI layout will not be saved: " + var1.getMessage());
         return null;
      }
   }

   private void updateDisplayMetrics() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      int var2 = Math.max(1, var1.method_22683().method_4489());
      int var3 = Math.max(1, var1.method_22683().method_4506());
      ImGuiIO var4 = ImGui.getIO();
      var4.setDisplaySize(var2, var3);
      var4.setDisplayFramebufferScale(1.0F, 1.0F);
   }

   private void loadFonts(ImGuiIO var1) {
      if (this.glyphRanges == null) {
         this.glyphRanges = buildGlyphRanges(var1);
      }

      try {
         var1.getFonts().setFreeTypeRenderer(true);
      } catch (Throwable var3) {
         System.err.println("[EFN] FreeType unavailable, using stb_truetype: " + var3.getMessage());
      }

      if (!this.buildFonts(var1, this.uiFont, this.hudFont)) {
         if (this.uiFont != UiFont.GEIST || this.hudFont != UiFont.GEIST) {
            System.err.println("[EFN] Couldn't build " + this.uiFont.label + " / " + this.hudFont.label + " fonts, using Geist");
            var1.getFonts().clear();
            if (this.buildFonts(var1, UiFont.GEIST, UiFont.GEIST)) {
               return;
            }
         }

         throw new IllegalStateException("ImGui font atlas could not be built");
      }
   }

   private boolean buildFonts(ImGuiIO var1, UiFont var2, UiFont var3) {
      float var4 = this.uiScale;
      this.moduleFont = this.loadFont(var1, var2, false, 16.0F * var4);
      this.categoryFont = this.loadFont(var1, var2, true, 17.0F * var4);
      this.titleFont = this.loadFont(var1, var2, true, 24.0F * var4);
      this.smallFont = this.loadFont(var1, var2, false, 13.0F * var4);
      this.hudValueFont = this.loadFont(var1, var3, true, 18.0F * var4);
      this.hudTextFont = this.loadFont(var1, var3, true, 17.0F * var4);
      this.hudLabelFont = this.loadFont(var1, var3, false, 14.0F * var4);
      this.menuFont = loadFont(var1, "/assets/efn/fonts/rajdhani-bold.ttf", 24.0F * var4, this.glyphRanges);
      this.menuTagFont = loadFont(var1, "/assets/efn/fonts/rajdhani-bold.ttf", 15.0F * var4, this.glyphRanges);
      this.menuBodyFont = loadFont(var1, "/assets/efn/fonts/geist-medium.ttf", 18.0F * var4, this.glyphRanges);
      this.menuNameFont = loadFont(var1, "/assets/efn/fonts/rajdhani-bold.ttf", 20.0F * var4, this.glyphRanges);
      this.menuItemFont = loadFont(var1, "/assets/efn/fonts/rajdhani-bold.ttf", 32.0F * var4, this.glyphRanges);
      if (this.moduleFont == null && this.categoryFont == null) {
         var1.getFonts().addFontDefault();
      } else if (this.moduleFont != null) {
         var1.setFontDefault(this.moduleFont);
      }

      return var1.getFonts().build();
   }

   private static short[] buildGlyphRanges(ImGuiIO var0) {
      ImFontGlyphRangesBuilder var1 = new ImFontGlyphRangesBuilder();
      var1.addRanges(var0.getFonts().getGlyphRangesDefault());
      var1.addRanges(var0.getFonts().getGlyphRangesVietnamese());
      var1.addText(
         "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\u00c7\u011e\u0130\u00d6\u015e\u00dc\u00e7\u011f\u0131\u00f6\u015f\u00fc\u00c2\u00e2\u00ca\u00ea\u00ce\u00ee\u00d4\u00f4\u00db\u00fb\u00c0\u00c1\u00c4\u00c8\u00c9\u00cb\u00cc\u00cd\u00cf\u00d2\u00d3\u00d6\u00d9\u00da\u00dc\u00dd\u00e0\u00e1\u00e4\u00e8\u00e9\u00eb\u00ec\u00ed\u00ef\u00f2\u00f3\u00f6\u00f9\u00fa\u00fc\u00fd\u00ff\u2212\u221e"
      );
      return var1.buildRanges();
   }

   private ImFont loadFont(ImGuiIO var1, UiFont var2, boolean var3, float var4) {
      float var5 = var2.size(var4);
      float var6 = var2.offsetY(var5);
      ImFont var7 = loadFont(var1, var2.resource(var3), var5, this.glyphRanges, var2.flags(), var6, false);
      if (var7 == null) {
         return var2 == UiFont.GEIST ? null : this.loadFont(var1, UiFont.GEIST, var3, var4);
      } else {
         if (var2 != UiFont.GEIST) {
            loadFont(var1, UiFont.GEIST.resource(var3), var2.fallbackSize(var5), this.glyphRanges, UiFont.GEIST.flags(), var6, true);
         }

         return var7;
      }
   }

   private static ImFont loadFont(ImGuiIO var0, String var1, float var2, short[] var3) {
      return loadFont(var0, var1, var2, var3, 8, 0.0F, false);
   }

   private static ImFont loadFont(ImGuiIO var0, String var1, float var2, short[] var3, int var4, float var5, boolean var6) {
      try {
         ImFont var11;
         try (InputStream var7 = ImGuiManager.class.getResourceAsStream(var1)) {
            if (var7 == null) {
               System.err.println("[EFN] Font resource not found: " + var1);
               return null;
            }

            byte[] var9 = var7.readAllBytes();
            ImFontConfig var10 = new ImFontConfig();

            ImFont var8;
            try {
               var10.setOversampleH(3);
               var10.setOversampleV(1);
               var10.setPixelSnapH(true);
               var10.setFontBuilderFlags(var4);
               var10.setGlyphOffset(0.0F, var5);
               var10.setMergeMode(var6);
               var8 = var0.getFonts().addFontFromMemoryTTF(var9, Math.round(var2), var10, var3);
            } catch (Throwable var13) {
               var10.destroy();
               throw var13;
            }

            var10.destroy();
            var11 = var8;
         }

         return var11;
      } catch (RuntimeException | IOException var15) {
         System.err.println("[EFN] Failed to load font " + var1 + ": " + var15.getMessage());
         return null;
      }
   }

   static {
      EfnAndroidNative.prepare();
   }
}
