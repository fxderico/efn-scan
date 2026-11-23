package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImString;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.efn.client.config.ConfigProfiles;
import net.efn.client.config.FriendStore;
import net.efn.client.config.ModuleConfigStore;
import net.efn.client.config.ProfileException;
import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.ActionSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class EfnClickGui {
   public static final String BRAND = "efn";
   public static final String BRAND_TAG = "client";
   public static final String BRAND_FOOTER = "efn.lol";
   public static final String VERSION = "v1.2.0";
   private static final String[] GENERAL_LABELS = new String[]{"HUD", "Friends", "Theme", "Configs"};
   private static final int[] GENERAL_TABS = new int[]{1, 2, 3, 4};
   private static final int HOST_FLAGS = 447;
   private static final int BG_PANEL = rgba(13, 13, 16, 247);
   private static final int BG_SIDEBAR = rgba(18, 18, 22, 255);
   private static final int BG_CARD = rgba(23, 23, 28, 255);
   private static final int BG_CARD_HOVER = rgba(30, 30, 37, 255);
   private static final int BG_FIELD = rgba(10, 10, 13, 255);
   private static final int LINE = rgba(255, 255, 255, 16);
   private static final int TEXT = rgba(236, 235, 240, 255);
   private static final int MUTED = rgba(138, 137, 150, 255);
   private static final int SWITCH_OFF = rgba(52, 52, 63, 255);
   private static final int KNOB = rgba(250, 248, 255, 255);
   private int page;
   private final ImString friendInput = new ImString(32);
   private final ImString profileInput = new ImString(40);
   private String armedDelete;
   private long armedAt;
   private String pageMsg = "";
   private boolean pageMsgErr;
   private long pageMsgAt;
   private final ClickGuiRenderer legacy = new ClickGuiRenderer();
   private final long openedAt = System.nanoTime();
   private EfnClickGui.View view = EfnClickGui.View.MODULES;
   private ModuleCategory category;
   private ClientModule detail;
   private final ImString search = new ImString(128);
   private boolean focusSearch;
   private ClientModule listeningModule;
   private KeySetting listeningKey;
   private boolean listenFirstFrame;
   private String dragId;
   private String popupFor;
   private final Map<String, float[]> anim = new HashMap<>();
   private final Map<Integer, String> keyLabels = new HashMap<>();
   private List<ModuleCategory> catCache = new ArrayList<>();
   private int catCacheRevision = -1;
   private float ga = 1.0F;
   private float dt;
   private float mx;
   private float my;
   private boolean clicked;
   private boolean rclicked;
   private boolean down;
   private boolean winHover;
   private boolean popupBlock;
   private ImFont fTitle;
   private ImFont fMod;
   private ImFont fSmall;
   private ImFont fCat;
   private static final String[] PAGE_FIRST = new String[]{"HUD", "Friends", "Theme", "Config"};
   private static final String[] PAGE_REST = new String[]{"Layout", "List", "Settings", "Profiles"};
   private String viewKey = "";
   private long viewChangedAt = System.nanoTime();

   public EfnClickGui() {
   }

   public void render() {
      GuiPalette.refresh();
      ThemeStore var1 = ThemeStore.get();
      ImGuiManager var2 = ImGuiManager.getInstance();
      float var3 = ImGui.getIO().getDisplaySizeX();
      float var4 = ImGui.getIO().getDisplaySizeY();
      this.dt = ClickGuiRenderer.animationDelta(Math.max(ImGui.getIO().getDeltaTime(), 0.001F));
      if (this.view == EfnClickGui.View.LEGACY) {
         this.legacy.render();
         this.renderBackPill(var3, var4);
      } else {
         this.fTitle = var2.titleFont();
         this.fMod = var2.moduleFont();
         this.fSmall = var2.smallFont();
         this.fCat = var2.categoryFont();
         this.mx = ImGui.getMousePosX();
         this.my = ImGui.getMousePosY();
         this.clicked = ImGui.isMouseClicked(0);
         this.rclicked = ImGui.isMouseClicked(1);
         this.down = ImGui.isMouseDown(0);
         this.popupBlock = this.popupFor != null && ImGui.isPopupOpen("", 3072);
         float var5 = ClickGuiRenderer.easeOut(ClickGuiRenderer.elapsed(this.openedAt, 240.0F));
         this.ga = Math.max(0.01F, var5);
         ImGui.getBackgroundDrawList().addRectFilled(0.0F, 0.0F, var3, var4, EfnWidgets.withAlpha(-16777216, var1.backgroundDim.get() * var5));
         if (ImGui.getIO().getKeyCtrl() && (ImGui.isKeyPressed(551) || ImGui.isKeyPressed(556))) {
            this.detail = null;
            this.focusSearch = true;
         }

         this.updateKeyListening();
         this.refreshCategories();
         float var6 = Math.min(EfnWidgets.s(1040.0F), var3 - EfnWidgets.s(48.0F));
         float var7 = Math.min(EfnWidgets.s(680.0F), var4 - EfnWidgets.s(48.0F));
         float var8 = (var3 - var6) * 0.5F;
         float var9 = (var4 - var7) * 0.5F + (1.0F - var5) * EfnWidgets.s(12.0F);
         ImGui.setNextWindowPos(0.0F, 0.0F, 1);
         ImGui.setNextWindowSize(var3, var4, 1);
         ImGui.pushStyleVar(2, 0.0F, 0.0F);
         ImGui.pushStyleVar(4, 0.0F);
         ImGui.pushStyleColor(2, 0);
         boolean var10 = ClickGuiRenderer.pushFont(this.fMod);

         try {
            if (ImGui.begin("##efn_host", 447)) {
               this.winHover = ImGui.isWindowHovered();
               this.renderWindow(ImGui.getWindowDrawList(), var8, var9, var6, var7);
            }

            ImGui.end();
         } finally {
            if (var10) {
               ImGui.popFont();
            }

            ImGui.popStyleColor(1);
            ImGui.popStyleVar(2);
         }
      }
   }

   public boolean handleEscape() {
      if (this.view == EfnClickGui.View.LEGACY) {
         if (!this.legacy.handleEscape()) {
            this.view = EfnClickGui.View.MODULES;
         }

         return true;
      } else if (this.listeningKey != null) {
         this.listeningKey.setValue(-1);
         this.listeningKey = null;
         return true;
      } else if (this.listeningModule != null) {
         this.listeningModule.setKeybind(-1);
         this.listeningModule = null;
         return true;
      } else if (this.popupFor != null) {
         this.popupFor = null;
         return true;
      } else if (this.view == EfnClickGui.View.PAGE) {
         this.view = EfnClickGui.View.MODULES;
         return true;
      } else if (this.detail != null) {
         this.detail = null;
         return true;
      } else if (!this.search.get().isEmpty()) {
         this.search.set("");
         return true;
      } else {
         return false;
      }
   }

   public boolean capturesKeys() {
      return this.view == EfnClickGui.View.LEGACY
         ? this.legacy.capturesKeys()
         : this.listeningModule != null || this.listeningKey != null || ImGuiManager.getInstance().typing();
   }

   public void onFilesDropped(List<Path> var1) {
      this.legacy.onFilesDropped(var1);
   }

   private void renderWindow(ImDrawList var1, float var2, float var3, float var4, float var5) {
      float var6 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding()) + EfnWidgets.s(6.0F);
      ClickGuiRenderer.drawShadow(var2, var3, var4, var5, var6, 6);
      var1.addRectFilled(var2, var3, var2 + var4, var3 + var5, this.f(BG_PANEL), var6);
      var1.addRect(var2, var3, var2 + var4, var3 + var5, this.f(LINE), var6, 0, 1.0F);
      float var7 = EfnWidgets.s(12.0F);
      float var8 = EfnWidgets.s(232.0F);
      this.renderSidebar(var1, var2 + var7, var3 + var7, var8, var5 - var7 * 2.0F);
      float var9 = var2 + var7 + var8 + EfnWidgets.s(22.0F);
      float var10 = var2 + var4 - var7 - EfnWidgets.s(14.0F) - var9;
      float var11 = var3 + var7 + EfnWidgets.s(14.0F);
      float var12 = this.view != EfnClickGui.View.PAGE && this.detail != null ? EfnWidgets.s(96.0F) : EfnWidgets.s(72.0F);
      this.renderHeader(var1, var9, var11, var10);
      float var13 = var11 + var12;
      int var14 = GuiPalette.accent();
      var1.addRectFilled(var9, var13, var9 + var10, var13 + EfnWidgets.s(1.0F), this.f(LINE));
      float var15 = var13 + EfnWidgets.s(14.0F);
      float var16 = var3 + var5 - var7 - EfnWidgets.s(10.0F) - var15;
      if (this.view == EfnClickGui.View.PAGE) {
         this.renderPage(var9, var15, var10, var16);
      } else if (this.detail != null) {
         this.renderDetail(var9, var15, var10, var16, this.detail);
      } else {
         this.renderGrid(var9, var15, var10, var16, this.currentModules());
      }
   }

   private void renderSidebar(ImDrawList var1, float var2, float var3, float var4, float var5) {
      float var6 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding());
      var1.addRectFilled(var2, var3, var2 + var4, var3 + var5, this.f(BG_SIDEBAR), var6);
      var1.addRect(var2, var3, var2 + var4, var3 + var5, this.f(LINE), var6, 0, 1.0F);
      int var7 = GuiPalette.accent();
      float var8 = EfnWidgets.s(18.0F);
      float var9 = var3 + EfnWidgets.s(16.0F);
      float var10 = EfnWidgets.s(36.0F);
      this.drawLogo(var1, var2 + var8, var9, var10, var7);
      float var11 = var2 + var8 + var10 + EfnWidgets.s(12.0F);
      EfnWidgets.text(var1, this.fCat, var11, var9 - EfnWidgets.s(1.0F), this.f(TEXT), "efn".toUpperCase(Locale.ROOT));
      EfnWidgets.text(var1, this.fSmall, var11, var9 + EfnWidgets.s(18.0F), this.f(MUTED), "v1.2.0");
      float var12 = var9 + var10 + EfnWidgets.s(18.0F);
      EfnWidgets.text(var1, this.fSmall, var2 + var8, var12, this.f(MUTED), "MODULES");
      var12 += EfnWidgets.lineHeight(this.fSmall) + EfnWidgets.s(6.0F);
      float var13 = EfnWidgets.s(34.0F);
      float var14 = var4 - EfnWidgets.s(20.0F);
      float var15 = var2 + EfnWidgets.s(10.0F);

      for (ModuleCategory var17 : this.catCache) {
         boolean var18 = this.view != EfnClickGui.View.PAGE
            && (var17 == this.category && this.detail == null || var17 == this.category && !this.search.get().isEmpty());
         boolean var19 = this.hot(var15, var12, var14, var13);
         float[] var20 = this.state("cat:" + var17.name(), var18);
         var20[0] += ((var19 ? 1.0F : 0.0F) - var20[0]) * Math.min(1.0F, this.dt * 14.0F);
         var20[1] += ((this.category == var17 && this.view != EfnClickGui.View.PAGE ? 1.0F : 0.0F) - var20[1]) * Math.min(1.0F, this.dt * 12.0F);
         if (var20[1] > 0.01F) {
            var1.addRectFilled(var15, var12, var15 + var14, var12 + var13, this.f(a(var7, 0.16F * var20[1])), EfnWidgets.s(10.0F));
            var1.addRectFilled(var15, var12 + var13 * 0.22F, var15 + EfnWidgets.s(3.0F), var12 + var13 * 0.78F, this.f(a(var7, var20[1])), EfnWidgets.s(2.0F));
         } else if (var20[0] > 0.01F) {
            var1.addRectFilled(var15, var12, var15 + var14, var12 + var13, this.f(a(rgba(255, 255, 255, 255), 0.05F * var20[0])), EfnWidgets.s(10.0F));
         }

         int var21 = EfnTheme.lerpColor(MUTED, var7, var20[1]);
         this.drawIcon(var1, var17.icon(), var15 + EfnWidgets.s(22.0F), var12 + var13 * 0.5F, EfnWidgets.s(7.0F), this.f(var21));
         int var22 = EfnTheme.lerpColor(rgba(189, 188, 200, 255), TEXT, Math.max(var20[1], var20[0]));
         EfnWidgets.text(
            var1, this.fMod, var15 + EfnWidgets.s(42.0F), var12 + (var13 - EfnWidgets.lineHeight(this.fMod)) * 0.5F, this.f(var22), this.label(var17)
         );
         String var23 = Integer.toString(ModuleRegistry.inCategory(var17).size());
         float var24 = EfnWidgets.textWidth(this.fSmall, var23);
         EfnWidgets.text(
            var1, this.fSmall, var15 + var14 - var24 - EfnWidgets.s(12.0F), var12 + (var13 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F, this.f(MUTED), var23
         );
         if (var19 && this.clicked) {
            this.category = var17;
            this.view = EfnClickGui.View.MODULES;
            this.detail = null;
            this.search.set("");
         }

         var12 += var13 + EfnWidgets.s(2.0F);
      }

      var12 += EfnWidgets.s(12.0F);
      EfnWidgets.text(var1, this.fSmall, var2 + var8, var12, this.f(MUTED), "GENERAL");
      var12 += EfnWidgets.lineHeight(this.fSmall) + EfnWidgets.s(6.0F);

      for (int var28 = 0; var28 < GENERAL_LABELS.length; var28++) {
         boolean var30 = this.hot(var15, var12, var14, var13);
         float[] var31 = this.state("gen:" + var28, false);
         var31[0] += ((var30 ? 1.0F : 0.0F) - var31[0]) * Math.min(1.0F, this.dt * 14.0F);
         boolean var32 = this.view == EfnClickGui.View.PAGE && this.page == var28;
         var31[1] += ((var32 ? 1.0F : 0.0F) - var31[1]) * Math.min(1.0F, this.dt * 12.0F);
         if (var31[1] > 0.01F) {
            int var33 = GuiPalette.accent();
            var1.addRectFilled(var15, var12, var15 + var14, var12 + var13, this.f(a(var33, 0.16F * var31[1])), EfnWidgets.s(10.0F));
            var1.addRectFilled(var15, var12 + var13 * 0.22F, var15 + EfnWidgets.s(3.0F), var12 + var13 * 0.78F, this.f(a(var33, var31[1])), EfnWidgets.s(2.0F));
         } else if (var31[0] > 0.01F) {
            var1.addRectFilled(var15, var12, var15 + var14, var12 + var13, this.f(a(rgba(255, 255, 255, 255), 0.05F * var31[0])), EfnWidgets.s(10.0F));
         }

         this.drawGeneralIcon(
            var1,
            var28,
            var15 + EfnWidgets.s(22.0F),
            var12 + var13 * 0.5F,
            EfnWidgets.s(6.5F),
            this.f(EfnTheme.lerpColor(EfnTheme.lerpColor(MUTED, TEXT, var31[0]), GuiPalette.accent(), var31[1]))
         );
         EfnWidgets.text(
            var1,
            this.fMod,
            var15 + EfnWidgets.s(42.0F),
            var12 + (var13 - EfnWidgets.lineHeight(this.fMod)) * 0.5F,
            this.f(EfnTheme.lerpColor(rgba(189, 188, 200, 255), TEXT, Math.max(var31[0], var31[1]))),
            GENERAL_LABELS[var28]
         );
         if (var30 && this.clicked) {
            this.page = var28;
            this.view = EfnClickGui.View.PAGE;
            this.detail = null;
            this.popupFor = null;
            this.pageMsg = "";
         }

         var12 += var13 + EfnWidgets.s(2.0F);
      }

      float var29 = EfnWidgets.textWidth(this.fSmall, "efn.lol");
      EfnWidgets.text(var1, this.fSmall, var2 + (var4 - var29) * 0.5F, var3 + var5 - EfnWidgets.s(26.0F), this.f(a(MUTED, 0.8F)), "efn.lol");
   }

   private void renderHeader(ImDrawList var1, float var2, float var3, float var4) {
      if (this.view == EfnClickGui.View.PAGE) {
         this.renderPageHeader(var1, var2, var3, var4);
      } else {
         int var5 = GuiPalette.accent();
         boolean var6 = this.detail == null && !this.search.get().trim().isEmpty();
         if (this.detail != null) {
            ClientModule var28 = this.detail;
            EfnWidgets.text(var1, this.fTitle, var2, var3, this.f(TEXT), var28.name());
            String var29 = var28.description() == null ? "" : var28.description();
            float var30 = var4 - EfnWidgets.s(260.0F);
            EfnWidgets.text(
               var1,
               this.fSmall,
               var2,
               var3 + EfnWidgets.lineHeight(this.fTitle) + EfnWidgets.s(6.0F),
               this.f(MUTED),
               EfnWidgets.ellipsize(this.fSmall, var29, var30)
            );
            float var31 = EfnWidgets.s(84.0F);
            float var33 = EfnWidgets.s(30.0F);
            float var35 = var3 + EfnWidgets.s(54.0F);
            boolean var36 = this.hot(var2, var35, var31, var33);
            var1.addRectFilled(var2, var35, var2 + var31, var35 + var33, this.f(var36 ? BG_CARD_HOVER : BG_CARD), EfnWidgets.s(9.0F));
            var1.addRect(var2, var35, var2 + var31, var35 + var33, this.f(LINE), EfnWidgets.s(9.0F), 0, 1.0F);
            EfnWidgets.chevronLeft(var1, var2 + EfnWidgets.s(16.0F), var35 + var33 * 0.5F, EfnWidgets.s(4.5F), this.f(TEXT));
            EfnWidgets.text(var1, this.fSmall, var2 + EfnWidgets.s(30.0F), var35 + (var33 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F, this.f(TEXT), "Back");
            if (var36 && this.clicked) {
               this.detail = null;
               this.popupFor = null;
            }

            float var37 = EfnWidgets.s(46.0F);
            float var38 = EfnWidgets.s(24.0F);
            float var39 = var2 + var4 - var37;
            float var18 = var3 + EfnWidgets.s(10.0F);
            float[] var19 = this.state("tog:" + var28.id(), var28.isEnabled());
            var19[1] += ((var28.isEnabled() ? 1.0F : 0.0F) - var19[1]) * Math.min(1.0F, this.dt * 10.0F);
            this.drawSwitch(var1, var39, var18, var37, var38, var19[1]);
            if (this.hot(var39, var18, var37, var38) && this.clicked) {
               this.toggle(var28);
            }

            String var20 = var28.isEnabled() ? "Enabled" : "Disabled";
            float var21 = EfnWidgets.textWidth(this.fSmall, var20);
            EfnWidgets.text(
               var1,
               this.fSmall,
               var39 - var21 - EfnWidgets.s(12.0F),
               var18 + (var38 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F,
               this.f(var28.isEnabled() ? var5 : MUTED),
               var20
            );
            if (var28.hasModifiedSettings()) {
               float var22 = EfnWidgets.s(78.0F);
               float var23 = EfnWidgets.s(28.0F);
               float var24 = var2 + var4 - var22;
               float var25 = var3 + EfnWidgets.s(54.0F);
               boolean var26 = this.hot(var24, var25, var22, var23);
               var1.addRectFilled(var24, var25, var24 + var22, var25 + var23, this.f(var26 ? BG_CARD_HOVER : BG_CARD), EfnWidgets.s(9.0F));
               var1.addRect(var24, var25, var24 + var22, var25 + var23, this.f(LINE), EfnWidgets.s(9.0F), 0, 1.0F);
               float var27 = EfnWidgets.textWidth(this.fSmall, "Reset");
               EfnWidgets.text(
                  var1, this.fSmall, var24 + (var22 - var27) * 0.5F, var25 + (var23 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F, this.f(TEXT), "Reset"
               );
               if (var26 && this.clicked) {
                  var28.resetSettings();
               }
            }
         } else {
            String var8 = var6 ? "Search" : (this.category == null ? "Modules" : this.label(this.category));
            EfnWidgets.text(var1, this.fTitle, var2, var3, this.f(var5), var8);
            float var9 = EfnWidgets.textWidth(this.fTitle, var8 + " ");
            EfnWidgets.text(var1, this.fTitle, var2 + var9, var3, this.f(TEXT), var6 ? "Results" : "Modules");
            List var10 = this.currentModules();
            int var11 = 0;

            for (ClientModule var13 : var10) {
               if (var13.isEnabled()) {
                  var11++;
               }
            }

            EfnWidgets.text(
               var1,
               this.fSmall,
               var2,
               var3 + EfnWidgets.lineHeight(this.fTitle) + EfnWidgets.s(6.0F),
               this.f(MUTED),
               var10.size() + " modules  \u00b7  " + var11 + " enabled"
            );
            float var32 = Math.min(EfnWidgets.s(300.0F), var4 * 0.42F);
            float var34 = EfnWidgets.s(40.0F);
            float var14 = var2 + var4 - var32;
            float var15 = var3 + EfnWidgets.s(2.0F);
            var1.addRectFilled(var14, var15, var14 + var32, var15 + var34, this.f(BG_FIELD), EfnWidgets.s(11.0F));
            var1.addRect(var14, var15, var14 + var32, var15 + var34, this.f(var6 ? a(var5, 0.7F) : LINE), EfnWidgets.s(11.0F), 0, 1.0F);
            EfnWidgets.magnifier(var1, var14 + EfnWidgets.s(20.0F), var15 + var34 * 0.5F, EfnWidgets.s(6.0F), this.f(MUTED));
            String var16 = "Ctrl K";
            float var17 = EfnWidgets.textWidth(this.fSmall, var16) + EfnWidgets.s(14.0F);
            var1.addRectFilled(
               var14 + var32 - var17 - EfnWidgets.s(8.0F),
               var15 + EfnWidgets.s(8.0F),
               var14 + var32 - EfnWidgets.s(8.0F),
               var15 + var34 - EfnWidgets.s(8.0F),
               this.f(rgba(255, 255, 255, 14)),
               EfnWidgets.s(6.0F)
            );
            EfnWidgets.text(
               var1, this.fSmall, var14 + var32 - var17 - EfnWidgets.s(1.0F), var15 + (var34 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F, this.f(MUTED), var16
            );
            ImGui.setCursorScreenPos(var14 + EfnWidgets.s(40.0F), var15 + (var34 - EfnWidgets.lineHeight(this.fMod)) * 0.5F);
            ImGui.setNextItemWidth(var32 - EfnWidgets.s(40.0F) - var17 - EfnWidgets.s(18.0F));
            ImGui.pushStyleColor(7, 0);
            ImGui.pushStyleColor(8, 0);
            ImGui.pushStyleColor(9, 0);
            ImGui.pushStyleColor(0, TEXT);
            ImGui.pushStyleColor(1, MUTED);
            ImGui.pushStyleVar(11, 0.0F, 0.0F);
            if (this.focusSearch) {
               ImGui.setKeyboardFocusHere();
               this.focusSearch = false;
            }

            ImGui.inputTextWithHint("##efn_search", "Search modules...", this.search);
            ImGui.popStyleVar(1);
            ImGui.popStyleColor(5);
         }
      }
   }

   private void renderGrid(float var1, float var2, float var3, float var4, List<ClientModule> var5) {
      ImGui.setCursorScreenPos(var1, var2);
      this.pushChildStyle();
      if (ImGui.beginChild("##efn_grid", var3, var4, false, 0)) {
         this.winHover = ImGui.isWindowHovered();
         ImDrawList var6 = ImGui.getWindowDrawList();
         float var7 = ImGui.getCursorScreenPosX();
         float var8 = ImGui.getCursorScreenPosY() + EfnWidgets.s(6.0F);
         float var9 = EfnWidgets.s(12.0F);
         this.trackView();
         float var10 = (var3 - EfnWidgets.s(12.0F) - var9) * 0.5F;
         float var11 = EfnWidgets.s(70.0F);
         if (var5.isEmpty()) {
            String var12 = "No modules found";
            float var13 = EfnWidgets.textWidth(this.fMod, var12);
            EfnWidgets.text(var6, this.fMod, var7 + (var3 - var13) * 0.5F, var8 + EfnWidgets.s(60.0F), this.f(MUTED), var12);
         }

         int var18 = (var5.size() + 1) / 2;

         for (int var19 = 0; var19 < var5.size(); var19++) {
            float var14 = var7 + var19 % 2 * (var10 + var9);
            float var15 = var8 + var19 / 2 * (var11 + var9);
            float var16 = ClickGuiRenderer.easeOut(Math.max(0.0F, Math.min(1.0F, (this.sinceViewChange() - Math.min(var19, 14) * 0.022F) / 0.32F)));
            float var17 = this.ga;
            this.ga = var17 * Math.max(0.01F, var16);
            this.drawModuleCard(var6, (ClientModule)var5.get(var19), var14, var15 + (1.0F - var16) * EfnWidgets.s(10.0F), var10, var11);
            this.ga = var17;
         }

         ImGui.setCursorScreenPos(var7, var8 + var18 * (var11 + var9) + EfnWidgets.s(6.0F));
         ImGui.dummy(1.0F, 1.0F);
      }

      ImGui.endChild();
      this.popChildStyle();
   }

   private void drawModuleCard(ImDrawList var1, ClientModule var2, float var3, float var4, float var5, float var6) {
      int var7 = GuiPalette.accent();
      float var8 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding());
      boolean var9 = this.hot(var3, var4, var5, var6);
      boolean var10 = var2.isEnabled();
      float[] var11 = this.state("card:" + var2.id(), var10);
      var11[0] += ((var9 ? 1.0F : 0.0F) - var11[0]) * Math.min(1.0F, this.dt * 14.0F);
      var11[1] += ((var10 ? 1.0F : 0.0F) - var11[1]) * Math.min(1.0F, this.dt * 10.0F);
      var1.addRectFilled(var3, var4, var3 + var5, var4 + var6, this.f(EfnTheme.lerpColor(BG_CARD, BG_CARD_HOVER, var11[0])), var8);
      if (var11[1] > 0.01F) {
         var1.addRectFilled(var3, var4, var3 + var5, var4 + var6, this.f(a(var7, 0.08F * var11[1])), var8);
      }

      var1.addRect(var3, var4, var3 + var5, var4 + var6, this.f(EfnTheme.lerpColor(LINE, var7, var11[1])), var8, 0, EfnWidgets.s(1.0F));
      float var12 = EfnWidgets.s(18.0F);
      float var13 = EfnWidgets.s(44.0F);
      float var14 = EfnWidgets.s(24.0F);
      float var15 = var3 + var5 - var12 - var13;
      float var16 = var4 + (var6 - var14) * 0.5F;
      float var17 = var15 - EfnWidgets.s(26.0F);
      float var18 = var4 + var6 * 0.5F;
      float var19 = var17 - EfnWidgets.s(18.0F) - (var3 + var12);
      String var20 = EfnWidgets.ellipsize(this.fMod, var2.name(), var19 - EfnWidgets.s(40.0F));
      float var21 = var4 + EfnWidgets.s(13.0F);
      EfnWidgets.text(var1, this.fMod, var3 + var12, var21, this.f(TEXT), var20);
      boolean var22 = false;
      int var23 = var2.isAction() ? -1 : var2.keybind();
      if (var23 >= 0) {
         String var24 = this.keyLabel(var23) + (var2.isHoldMode() ? " hold" : "");
         float var25 = EfnWidgets.textWidth(this.fSmall, var24) + EfnWidgets.s(12.0F);
         float var26 = var3 + var12 + EfnWidgets.textWidth(this.fMod, var20) + EfnWidgets.s(8.0F);
         float var27 = EfnWidgets.lineHeight(this.fSmall) + EfnWidgets.s(4.0F);
         float var28 = var21 + (EfnWidgets.lineHeight(this.fMod) - var27) * 0.5F;
         var1.addRectFilled(var26, var28, var26 + var25, var28 + var27, this.f(rgba(255, 255, 255, 15)), EfnWidgets.s(5.0F));
         EfnWidgets.text(var1, this.fSmall, var26 + EfnWidgets.s(6.0F), var28 + EfnWidgets.s(2.0F), this.f(MUTED), var24);
      }

      String var29 = var2.isAction() ? "Action" : (var2.description() == null ? "" : var2.description());
      EfnWidgets.text(
         var1,
         this.fSmall,
         var3 + var12,
         var4 + var6 - EfnWidgets.s(14.0F) - EfnWidgets.lineHeight(this.fSmall),
         this.f(MUTED),
         EfnWidgets.ellipsize(this.fSmall, var29, var19)
      );
      boolean var30 = this.hot(var17 - EfnWidgets.s(12.0F), var18 - EfnWidgets.s(12.0F), EfnWidgets.s(24.0F), EfnWidgets.s(24.0F));
      this.drawCog(var1, var17, var18, EfnWidgets.s(7.5F), this.f(var30 ? TEXT : MUTED));
      this.drawSwitch(var1, var15, var16, var13, var14, var11[1]);
      if (this.clicked) {
         if (var30) {
            this.detail = var2;
            this.popupFor = null;
         } else if (var9 && !var22) {
            this.toggle(var2);
         }
      } else if (this.rclicked && var9) {
         this.detail = var2;
         this.popupFor = null;
      }
   }

   private void renderDetail(float var1, float var2, float var3, float var4, ClientModule var5) {
      ImGui.setCursorScreenPos(var1, var2);
      this.pushChildStyle();
      if (ImGui.beginChild("##efn_detail", var3, var4, false, 0)) {
         this.winHover = ImGui.isWindowHovered();
         ImDrawList var6 = ImGui.getWindowDrawList();
         float var7 = ImGui.getCursorScreenPosX();
         float var8 = ImGui.getCursorScreenPosY() + EfnWidgets.s(6.0F);
         float var9 = var3 - EfnWidgets.s(12.0F);
         float var10 = EfnWidgets.s(54.0F);
         float var11 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding());
         float var12 = var8;
         int var13 = var5.isAction() ? 0 : (var5.keybind() >= 0 ? 2 : 1);
         if (var13 > 0) {
            this.beginContainer(var6, var7, var8, var9, var13 * var10, var11);
            this.rowLabel(var6, var7, var8, var10, "Keybind");
            this.keyRow(var6, var7, var8, var9, var10, var5);
            if (var13 == 2) {
               float var14 = var8 + var10;
               var6.addLine(var7 + EfnWidgets.s(18.0F), var14, var7 + var9 - EfnWidgets.s(18.0F), var14, this.f(LINE), 1.0F);
               this.rowLabel(var6, var7, var14, var10, "Hold to enable");
               float var15 = EfnWidgets.s(44.0F);
               float var16 = EfnWidgets.s(24.0F);
               float var17 = var7 + var9 - EfnWidgets.s(18.0F) - var15;
               float var18 = var14 + (var10 - var16) * 0.5F;
               float[] var19 = this.state("hold:" + var5.id(), var5.isHoldMode());
               var19[1] += ((var5.isHoldMode() ? 1.0F : 0.0F) - var19[1]) * Math.min(1.0F, this.dt * 10.0F);
               this.drawSwitch(var6, var17, var18, var15, var16, var19[1]);
               if (this.clicked && this.hot(var17, var18, var15, var16)) {
                  var5.setHoldMode(!var5.isHoldMode());
               }
            }

            var12 = var8 + (var13 * var10 + EfnWidgets.s(14.0F));
         }

         ArrayList var20 = new ArrayList();

         for (Setting var23 : var5.settings()) {
            if (var23.isVisible()) {
               var20.add(var23);
            }
         }

         if (!var20.isEmpty()) {
            this.beginContainer(var6, var7, var12, var9, var20.size() * var10, var11);

            for (int var22 = 0; var22 < var20.size(); var22++) {
               float var24 = var12 + var22 * var10;
               if (var22 > 0) {
                  var6.addLine(var7 + EfnWidgets.s(18.0F), var24, var7 + var9 - EfnWidgets.s(18.0F), var24, this.f(LINE), 1.0F);
               }

               this.settingRow(var6, var7, var24, var9, var10, var5, (Setting)var20.get(var22));
            }

            var12 += var20.size() * var10;
         }

         ImGui.setCursorScreenPos(var7, var12 + EfnWidgets.s(24.0F));
         ImGui.dummy(1.0F, 1.0F);
      }

      ImGui.endChild();
      this.popChildStyle();
   }

   private void beginContainer(ImDrawList var1, float var2, float var3, float var4, float var5, float var6) {
      var1.addRectFilled(var2, var3, var2 + var4, var3 + var5, this.f(BG_CARD), var6);
      var1.addRect(var2, var3, var2 + var4, var3 + var5, this.f(LINE), var6, 0, 1.0F);
   }

   private void rowLabel(ImDrawList var1, float var2, float var3, float var4, String var5) {
      EfnWidgets.text(var1, this.fMod, var2 + EfnWidgets.s(18.0F), var3 + (var4 - EfnWidgets.lineHeight(this.fMod)) * 0.5F, this.f(TEXT), var5);
   }

   private void keyRow(ImDrawList var1, float var2, float var3, float var4, float var5, ClientModule var6) {
      float var7 = EfnWidgets.s(140.0F);
      float var8 = EfnWidgets.s(32.0F);
      float var9 = var2 + var4 - EfnWidgets.s(18.0F) - var7;
      float var10 = var3 + (var5 - var8) * 0.5F;
      boolean var11 = this.listeningModule == var6;
      String var12 = var11 ? "Press a key..." : (var6.keybind() < 0 ? "None" : this.keyLabel(var6.keybind()));
      if (this.pill(var1, var9, var10, var7, var8, var12, var11) && this.clicked) {
         this.listeningKey = null;
         this.listeningModule = var6;
         this.listenFirstFrame = true;
         this.popupFor = null;
      }
   }

   private void settingRow(ImDrawList var1, float var2, float var3, float var4, float var5, ClientModule var6, Setting var7) {
      this.settingRow(var1, var2, var3, var4, var5, var6.id(), var6, var7);
   }

   private void settingRow(ImDrawList var1, float var2, float var3, float var4, float var5, String var6, ClientModule var7, Setting var8) {
      float var9 = EfnWidgets.s(18.0F);
      float var10 = var2 + var4 - var9;
      String var11 = var6 + "/" + var8.id();
      this.rowLabel(var1, var2, var3, var5, var8.name());
      if (var8 instanceof BooleanSetting var12) {
         float var13 = EfnWidgets.s(44.0F);
         float var14 = EfnWidgets.s(24.0F);
         float var15 = var10 - var13;
         float var16 = var3 + (var5 - var14) * 0.5F;
         float[] var17 = this.state("bs:" + var11, var12.get());
         var17[1] += ((var12.get() ? 1.0F : 0.0F) - var17[1]) * Math.min(1.0F, this.dt * 10.0F);
         this.drawSwitch(var1, var15, var16, var13, var14, var17[1]);
         if (this.clicked && this.hot(var2, var3, var4, var5)) {
            var12.set(!var12.get());
         }
      } else if (var8 instanceof NumberSetting) {
         this.sliderRow(var1, var2, var3, var4, var5, var11, (NumberSetting)var8);
      } else if (var8 instanceof ModeSetting) {
         this.modeRow(var1, var10, var3, var5, var11, (ModeSetting)var8);
      } else if (var8 instanceof ColorSetting) {
         this.colorRow(var1, var10, var3, var5, var11, (ColorSetting)var8);
      } else if (var8 instanceof KeySetting var19) {
         float var22 = EfnWidgets.s(140.0F);
         float var25 = EfnWidgets.s(32.0F);
         float var26 = var10 - var22;
         float var27 = var3 + (var5 - var25) * 0.5F;
         boolean var28 = this.listeningKey == var19;
         String var18 = var28 ? "Press a key..." : (var19.value() < 0 ? "None" : this.keyLabel(var19.value()));
         if (this.pill(var1, var26, var27, var22, var25, var18, var28) && this.clicked) {
            this.listeningModule = null;
            this.listeningKey = var19;
            this.listenFirstFrame = true;
            this.popupFor = null;
         }
      } else if (var8 instanceof StringSetting) {
         this.stringRow(var1, var2, var10, var3, var5, var11, (StringSetting)var8);
      } else if (var8 instanceof ActionSetting) {
         float var20 = EfnWidgets.s(96.0F);
         float var23 = EfnWidgets.s(32.0F);
         if (this.pill(var1, var10 - var20, var3 + (var5 - var23) * 0.5F, var20, var23, "Run", false) && this.clicked) {
            ((ActionSetting)var8).run();
         }
      } else {
         float var21 = EfnWidgets.s(130.0F);
         float var24 = EfnWidgets.s(32.0F);
         if (var7 != null && this.pill(var1, var10 - var21, var3 + (var5 - var24) * 0.5F, var21, var24, "Open editor", false) && this.clicked) {
            this.legacy.showModuleSettings(var7);
            this.view = EfnClickGui.View.LEGACY;
         }
      }
   }

   private void sliderRow(ImDrawList var1, float var2, float var3, float var4, float var5, String var6, NumberSetting var7) {
      int var8 = GuiPalette.accent();
      float var9 = EfnWidgets.s(18.0F);
      float var10 = EfnWidgets.s(78.0F);
      float var11 = EfnWidgets.s(30.0F);
      float var12 = var2 + var4 - var9 - var10;
      float var13 = var3 + (var5 - var11) * 0.5F;
      var1.addRectFilled(var12, var13, var12 + var10, var13 + var11, this.f(BG_FIELD), EfnWidgets.s(8.0F));
      var1.addRect(var12, var13, var12 + var10, var13 + var11, this.f(LINE), EfnWidgets.s(8.0F), 0, 1.0F);
      String var14 = var7.formatted();
      float var15 = EfnWidgets.textWidth(this.fSmall, var14);
      EfnWidgets.text(var1, this.fSmall, var12 + (var10 - var15) * 0.5F, var13 + (var11 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F, this.f(TEXT), var14);
      float var16 = Math.min(EfnWidgets.s(320.0F), var4 * 0.38F);
      float var17 = var12 - EfnWidgets.s(18.0F) - var16;
      float var18 = var3 + var5 * 0.5F;
      float var19 = var7.max() - var7.min();
      float var20 = var19 <= 0.0F ? 0.0F : (var7.get() - var7.min()) / var19;
      var20 = Math.max(0.0F, Math.min(1.0F, var20));
      float[] var21 = this.state("sl:" + var6, false);
      var21[0] += (var20 - var21[0]) * Math.min(1.0F, this.dt * 20.0F);
      float var22 = EfnWidgets.s(6.0F);
      var1.addRectFilled(var17, var18 - var22 * 0.5F, var17 + var16, var18 + var22 * 0.5F, this.f(SWITCH_OFF), var22 * 0.5F);
      var1.addRectFilled(var17, var18 - var22 * 0.5F, var17 + var16 * var21[0], var18 + var22 * 0.5F, this.f(var8), var22 * 0.5F);
      float var23 = var17 + var16 * var21[0];
      var1.addCircleFilled(var23, var18, EfnWidgets.s(8.5F), this.f(a(var8, 0.25F)), 20);
      var1.addCircleFilled(var23, var18, EfnWidgets.s(6.5F), this.f(KNOB), 20);
      if (this.clicked && this.hot(var17 - EfnWidgets.s(8.0F), var18 - EfnWidgets.s(13.0F), var16 + EfnWidgets.s(16.0F), EfnWidgets.s(26.0F))) {
         this.dragId = var6;
      }

      if (var6.equals(this.dragId)) {
         if (this.down) {
            float var24 = Math.max(0.0F, Math.min(1.0F, (this.mx - var17) / var16));
            var7.set(var7.min() + var24 * var19);
         } else {
            this.dragId = null;
         }
      }
   }

   private void modeRow(ImDrawList var1, float var2, float var3, float var4, String var5, ModeSetting var6) {
      float var7 = EfnWidgets.s(190.0F);
      float var8 = EfnWidgets.s(32.0F);
      float var9 = var2 - var7;
      float var10 = var3 + (var4 - var8) * 0.5F;
      boolean var11 = var5.equals(this.popupFor);
      if (this.pill(var1, var9, var10, var7, var8, var6.get(), var11) && this.clicked) {
         this.popupFor = var5;
         ImGui.openPopup("##efn_mode");
      }

      EfnWidgets.chevron(var1, var9 + var7 - EfnWidgets.s(16.0F), var10 + var8 * 0.5F, EfnWidgets.s(4.0F), this.f(MUTED), var11);
      if (var11) {
         ImGui.setNextWindowPos(var9, var10 + var8 + EfnWidgets.s(4.0F));
         ImGui.setNextWindowSize(var7, 0.0F);
         this.pushPopupStyle();
         if (!ImGui.beginPopup("##efn_mode")) {
            this.popupFor = null;
         } else {
            for (String var13 : var6.modes()) {
               if (ImGui.selectable(var13, var13.equals(var6.get()))) {
                  var6.set(var13);
                  this.popupFor = null;
                  ImGui.closeCurrentPopup();
               }
            }

            ImGui.endPopup();
         }

         this.popPopupStyle();
      }
   }

   private void colorRow(ImDrawList var1, float var2, float var3, float var4, String var5, ColorSetting var6) {
      float var7 = EfnWidgets.s(64.0F);
      float var8 = EfnWidgets.s(28.0F);
      float var9 = var2 - var7;
      float var10 = var3 + (var4 - var8) * 0.5F;
      boolean var11 = var5.equals(this.popupFor);
      int var12 = var6.argbWithAlpha(255);
      var1.addRectFilled(var9, var10, var9 + var7, var10 + var8, this.f(GuiPalette.abgr(var12)), EfnWidgets.s(8.0F));
      var1.addRect(var9, var10, var9 + var7, var10 + var8, this.f(var11 ? GuiPalette.accent() : rgba(255, 255, 255, 60)), EfnWidgets.s(8.0F), 0, 1.4F);
      if (this.clicked && this.hot(var9, var10, var7, var8)) {
         this.popupFor = var5;
         ImGui.openPopup("##efn_color");
      }

      if (var11) {
         ImGui.setNextWindowPos(var9 + var7 - EfnWidgets.s(260.0F), var10 + var8 + EfnWidgets.s(6.0F));
         this.pushPopupStyle();
         if (ImGui.beginPopup("##efn_color")) {
            int var13 = 65808;
            if (ImGui.colorPicker4("##efn_cp", var6.value(), var13)) {
               var6.notifyChanged();
            }

            ImGui.endPopup();
         } else {
            this.popupFor = null;
         }

         this.popPopupStyle();
      }
   }

   private void stringRow(ImDrawList var1, float var2, float var3, float var4, float var5, String var6, StringSetting var7) {
      float var8 = Math.min(EfnWidgets.s(300.0F), (var3 - var2) * 0.45F);
      float var9 = EfnWidgets.s(32.0F);
      float var10 = var3 - var8;
      float var11 = var4 + (var5 - var9) * 0.5F;
      var1.addRectFilled(var10, var11, var10 + var8, var11 + var9, this.f(BG_FIELD), EfnWidgets.s(8.0F));
      var1.addRect(var10, var11, var10 + var8, var11 + var9, this.f(LINE), EfnWidgets.s(8.0F), 0, 1.0F);
      ImGui.setCursorScreenPos(var10 + EfnWidgets.s(12.0F), var11 + (var9 - EfnWidgets.lineHeight(this.fMod)) * 0.5F);
      ImGui.setNextItemWidth(var8 - EfnWidgets.s(24.0F));
      ImGui.pushStyleColor(7, 0);
      ImGui.pushStyleColor(8, 0);
      ImGui.pushStyleColor(9, 0);
      ImGui.pushStyleColor(0, TEXT);
      ImGui.pushStyleVar(11, 0.0F, 0.0F);
      ImString var12 = var7.getImString();
      if (ImGui.inputText("##efn_s_" + var6, var12)) {
         var7.set(var12.get());
      }

      ImGui.popStyleVar(1);
      ImGui.popStyleColor(4);
   }

   private void renderPageHeader(ImDrawList var1, float var2, float var3, float var4) {
      int var5 = GuiPalette.accent();
      String var6 = PAGE_FIRST[this.page];
      EfnWidgets.text(var1, this.fTitle, var2, var3, this.f(var5), var6);
      float var7 = EfnWidgets.textWidth(this.fTitle, var6 + " ");
      EfnWidgets.text(var1, this.fTitle, var2 + var7, var3, this.f(TEXT), PAGE_REST[this.page]);

      EfnWidgets.text(var1, this.fSmall, var2, var3 + EfnWidgets.lineHeight(this.fTitle) + EfnWidgets.s(6.0F), this.f(MUTED), switch (this.page) {
         case 0 -> "Move and restyle your on-screen elements.";
         case 1 -> {
            int var11 = FriendStore.getInstance().all().size();
            yield var11 == 1 ? "1 player" : var11 + " players";
         }
         case 2 -> "Accent colour and how the client looks.";
         default -> {
            String var9 = ModuleConfigStore.getInstance().activeProfile();
            int var10 = ConfigProfiles.getInstance().list().size();
            yield var10 + (var10 == 1 ? " profile" : " profiles") + (var9 != null ? "  \u00b7  active: " + var9 : "");
         }
      });
      if (this.page == 2) {
         float var12 = EfnWidgets.s(96.0F);
         float var13 = EfnWidgets.s(32.0F);
         if (this.pill(var1, var2 + var4 - var12, var3 + EfnWidgets.s(4.0F), var12, var13, "Reset all", false) && this.clicked) {
            ThemeStore.get().resetToDefaults();
         }
      }
   }

   private void renderPage(float var1, float var2, float var3, float var4) {
      ImGui.setCursorScreenPos(var1, var2);
      this.pushChildStyle();
      if (ImGui.beginChild("##efn_page", var3, var4, false, 0)) {
         this.winHover = ImGui.isWindowHovered();
         ImDrawList var5 = ImGui.getWindowDrawList();
         float var6 = ImGui.getCursorScreenPosX();
         float var7 = ImGui.getCursorScreenPosY() + EfnWidgets.s(6.0F);
         float var8 = var3 - EfnWidgets.s(12.0F);

         float var10 = this.pageMessage(var5, var6, switch (this.page) {
            case 0 -> this.pageHud(var5, var6, var7, var8);
            case 1 -> this.pageFriends(var5, var6, var7, var8);
            case 2 -> this.pageTheme(var5, var6, var7, var8);
            default -> this.pageConfigs(var5, var6, var7, var8);
         } + EfnWidgets.s(12.0F), var8);
         ImGui.setCursorScreenPos(var6, var10 + EfnWidgets.s(24.0F));
         ImGui.dummy(1.0F, 1.0F);
      }

      ImGui.endChild();
      this.popChildStyle();
   }

   private void say(String var1, boolean var2) {
      this.pageMsg = var1;
      this.pageMsgErr = var2;
      this.pageMsgAt = System.nanoTime();
   }

   private float pageMessage(ImDrawList var1, float var2, float var3, float var4) {
      if (this.pageMsg.isEmpty()) {
         return var3 - EfnWidgets.s(12.0F);
      } else if ((float)(System.nanoTime() - this.pageMsgAt) / 1.0E9F > 6.0F) {
         this.pageMsg = "";
         return var3 - EfnWidgets.s(12.0F);
      } else {
         int var5 = this.pageMsgErr ? rgba(255, 110, 110, 255) : GuiPalette.accent();
         EfnWidgets.text(var1, this.fSmall, var2 + EfnWidgets.s(4.0F), var3, this.f(var5), this.pageMsg);
         return var3 + EfnWidgets.lineHeight(this.fSmall);
      }
   }

   private float rowsTop(ImDrawList var1, float var2, float var3, float var4, float var5, int var6) {
      float var7 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding());
      this.beginContainer(var1, var2, var3, var4, var6 * var5, var7);

      for (int var8 = 1; var8 < var6; var8++) {
         float var9 = var3 + var8 * var5;
         var1.addLine(var2 + EfnWidgets.s(18.0F), var9, var2 + var4 - EfnWidgets.s(18.0F), var9, this.f(LINE), 1.0F);
      }

      return var3 + var6 * var5;
   }

   private boolean textField(String var1, float var2, float var3, float var4, float var5, String var6, ImString var7) {
      ImDrawList var8 = ImGui.getWindowDrawList();
      var8.addRectFilled(var2, var3, var2 + var4, var3 + var5, this.f(BG_FIELD), EfnWidgets.s(8.0F));
      var8.addRect(var2, var3, var2 + var4, var3 + var5, this.f(LINE), EfnWidgets.s(8.0F), 0, 1.0F);
      ImGui.setCursorScreenPos(var2 + EfnWidgets.s(12.0F), var3 + (var5 - EfnWidgets.lineHeight(this.fMod)) * 0.5F);
      ImGui.setNextItemWidth(var4 - EfnWidgets.s(24.0F));
      ImGui.pushStyleColor(7, 0);
      ImGui.pushStyleColor(8, 0);
      ImGui.pushStyleColor(9, 0);
      ImGui.pushStyleColor(0, TEXT);
      ImGui.pushStyleColor(1, MUTED);
      ImGui.pushStyleVar(11, 0.0F, 0.0F);
      boolean var9 = ImGui.inputTextWithHint(var1, var6, var7, 64);
      ImGui.popStyleVar(1);
      ImGui.popStyleColor(5);
      return var9;
   }

   private float pageTheme(ImDrawList var1, float var2, float var3, float var4) {
      ThemeStore var5 = ThemeStore.get();
      float var6 = EfnWidgets.s(54.0F);
      float var7 = var3;
      float var8 = this.rowsTop(var1, var2, var3, var4, var6, 2);
      this.rowLabel(var1, var2, var3, var6, "Accent colour");
      float var9 = EfnWidgets.s(11.0F);
      float var10 = EfnWidgets.s(12.0F);
      float var11 = var2 + var4 - EfnWidgets.s(18.0F);
      int var12 = ThemeStore.PRESETS.size();
      float var13 = var11 - var12 * (2.0F * var9) - (var12 - 1) * var10 + var9;
      int var14 = var5.accent.argbWithAlpha(255);

      for (int var15 = 0; var15 < var12; var15++) {
         ThemeStore.Preset var16 = ThemeStore.PRESETS.get(var15);
         float var17 = var13 + var15 * (2.0F * var9 + var10);
         float var18 = var7 + var6 * 0.5F;
         boolean var19 = this.hot(var17 - var9 - 3.0F, var18 - var9 - 3.0F, 2.0F * var9 + 6.0F, 2.0F * var9 + 6.0F);
         float[] var20 = this.state("sw:" + var15, false);
         var20[0] += ((var19 ? 1.0F : 0.0F) - var20[0]) * Math.min(1.0F, this.dt * 14.0F);
         boolean var21 = (var16.accent() | 0xFF000000) == (var14 | 0xFF000000);
         var1.addCircleFilled(var17, var18, var9 + EfnWidgets.s(1.5F) * var20[0], this.f(GuiPalette.abgr(var16.accent() | 0xFF000000)), 28);
         if (var21) {
            var1.addCircle(var17, var18, var9 + EfnWidgets.s(5.0F), this.f(TEXT), 28, 1.6F);
         }

         if (var19 && this.clicked) {
            var5.applyPreset(var16);
         }
      }

      float var24 = var7 + var6;
      this.rowLabel(var1, var2, var24, var6, "Custom colour");
      this.colorRow(var1, var11, var24, var6, "theme/accent", var5.accent);
      var7 = var8 + EfnWidgets.s(14.0F);
      ArrayList var25 = new ArrayList();
      var25.add(var5.rounding);
      var25.add(var5.backgroundDim);
      var25.add(var5.menuBlur);
      var25.add(var5.uiScale);
      var25.add(var5.font);
      var25.add(var5.animationSpeed);
      var25.add(var5.customMainMenu);
      var8 = this.rowsTop(var1, var2, var7, var4, var6, var25.size());

      for (int var26 = 0; var26 < var25.size(); var26++) {
         this.settingRow(var1, var2, var7 + var26 * var6, var4, var6, "theme", null, (Setting)var25.get(var26));
      }

      return var8;
   }

   private float pageFriends(ImDrawList var1, float var2, float var3, float var4) {
      FriendStore var5 = FriendStore.getInstance();
      float var6 = EfnWidgets.s(54.0F);
      float var7 = EfnWidgets.s(18.0F);
      float var9 = this.rowsTop(var1, var2, var3, var4, var6, 1);
      this.rowLabel(var1, var2, var3, var6, "Add player");
      float var10 = EfnWidgets.s(84.0F);
      float var11 = EfnWidgets.s(32.0F);
      float var12 = Math.min(EfnWidgets.s(280.0F), var4 * 0.4F);
      float var13 = var2 + var4 - var7 - var10;
      float var14 = var13 - EfnWidgets.s(10.0F) - var12;
      float var15 = var3 + (var6 - var11) * 0.5F;
      boolean var16 = this.textField("##efn_friend", var14, var15, var12, var11, "Exact username", this.friendInput);
      boolean var17 = this.pill(var1, var13, var15, var10, var11, "Add", false) && this.clicked;
      if (var16 || var17) {
         String var18 = this.friendInput.get().trim();
         if (!FriendStore.isValidName(var18)) {
            this.say("That is not a valid Minecraft username", true);
         } else if (!var5.add(var18)) {
            this.say(var18 + " is already on your list", true);
         } else {
            var5.save();
            this.friendInput.set("");
            this.say("Added " + var18, false);
         }
      }

      float var8 = var9 + EfnWidgets.s(14.0F);
      List var24 = var5.all();
      if (var24.isEmpty()) {
         float var25 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding());
         this.beginContainer(var1, var2, var8, var4, var6 * 1.4F, var25);
         String var26 = "No friends yet. Players you add are protected from your combat modules.";
         float var27 = EfnWidgets.textWidth(this.fSmall, var26);
         EfnWidgets.text(
            var1, this.fSmall, var2 + (var4 - var27) * 0.5F, var8 + (var6 * 1.4F - EfnWidgets.lineHeight(this.fSmall)) * 0.5F, this.f(MUTED), var26
         );
         return var8 + var6 * 1.4F;
      } else {
         var9 = this.rowsTop(var1, var2, var8, var4, var6, var24.size());

         for (int var19 = 0; var19 < var24.size(); var19++) {
            String var20 = (String)var24.get(var19);
            float var21 = var8 + var19 * var6;
            this.rowLabel(var1, var2, var21, var6, var20);
            float var22 = EfnWidgets.s(90.0F);
            if (this.pill(var1, var2 + var4 - var7 - var22, var21 + (var6 - var11) * 0.5F, var22, var11, "Remove", false) && this.clicked) {
               var5.remove(var20);
               var5.save();
               this.say("Removed " + var20, false);
            }
         }

         return var9;
      }
   }

   private float pageConfigs(ImDrawList var1, float var2, float var3, float var4) {
      ConfigProfiles var5 = ConfigProfiles.getInstance();
      float var6 = EfnWidgets.s(54.0F);
      float var7 = EfnWidgets.s(18.0F);
      float var8 = EfnWidgets.s(32.0F);
      float var10 = this.rowsTop(var1, var2, var3, var4, var6, 2);
      this.rowLabel(var1, var2, var3, var6, "New profile");
      float var11 = EfnWidgets.s(110.0F);
      float var12 = Math.min(EfnWidgets.s(280.0F), var4 * 0.4F);
      float var13 = var2 + var4 - var7 - var11;
      float var14 = var13 - EfnWidgets.s(10.0F) - var12;
      float var15 = var3 + (var6 - var8) * 0.5F;
      boolean var16 = this.textField("##efn_profile", var14, var15, var12, var8, "Profile name", this.profileInput);
      boolean var17 = this.pill(var1, var13, var15, var11, var8, "Save current", false) && this.clicked;
      if (var16 || var17) {
         try {
            String var18 = var5.create(this.profileInput.get());
            var5.save(var18);
            this.profileInput.set("");
            this.say("Saved " + var18, false);
         } catch (ProfileException var42) {
            this.say(var42.getMessage(), true);
         }
      }

      float var44 = var3 + var6;
      this.rowLabel(var1, var2, var44, var6, "Import from clipboard");
      float var19 = EfnWidgets.s(110.0F);
      if (this.pill(var1, var2 + var4 - var7 - var19, var44 + (var6 - var8) * 0.5F, var19, var8, "Import", false) && this.clicked) {
         try {
            String var20 = var5.importCode(ImGui.getClipboardText());
            this.say("Imported " + var20, false);
         } catch (ProfileException var41) {
            this.say(var41.getMessage(), true);
         }
      }

      float var9 = var10 + EfnWidgets.s(14.0F);
      List var45 = var5.list();
      if (var45.isEmpty()) {
         float var46 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding());
         this.beginContainer(var1, var2, var9, var4, var6 * 1.4F, var46);
         String var47 = "No profiles yet. Name one above and save your current setup.";
         float var48 = EfnWidgets.textWidth(this.fSmall, var47);
         EfnWidgets.text(
            var1, this.fSmall, var2 + (var4 - var48) * 0.5F, var9 + (var6 * 1.4F - EfnWidgets.lineHeight(this.fSmall)) * 0.5F, this.f(MUTED), var47
         );
         return var9 + var6 * 1.4F;
      } else {
         String var21 = ModuleConfigStore.getInstance().activeProfile();
         if (this.armedDelete != null && (float)(System.nanoTime() - this.armedAt) / 1.0E9F > 3.0F) {
            this.armedDelete = null;
         }

         int var22 = GuiPalette.accent();
         float var23 = EfnWidgets.s(70.0F);
         float var24 = EfnWidgets.s(8.0F);
         var10 = this.rowsTop(var1, var2, var9, var4, var6, var45.size());

         for (int var25 = 0; var25 < var45.size(); var25++) {
            ConfigProfiles.Profile var26 = (ConfigProfiles.Profile)var45.get(var25);
            float var27 = var9 + var25 * var6;
            float var28 = var27 + (var6 - var8) * 0.5F;
            boolean var29 = var26.name().equalsIgnoreCase(var21);
            float var30 = var2 + var7;
            if (var29) {
               var1.addCircleFilled(var30 + EfnWidgets.s(4.0F), var27 + var6 * 0.5F, EfnWidgets.s(3.5F), this.f(var22), 12);
               var30 += EfnWidgets.s(16.0F);
            }

            float var31 = var23 * 3.0F + EfnWidgets.s(80.0F) + var24 * 3.0F;
            String var32 = EfnWidgets.ellipsize(this.fMod, var26.name(), var4 - var7 * 2.0F - var31 - EfnWidgets.s(20.0F));
            EfnWidgets.text(var1, this.fMod, var30, var27 + var6 * 0.5F - EfnWidgets.lineHeight(this.fMod) + EfnWidgets.s(4.0F), this.f(TEXT), var32);
            StringBuilder var33 = new StringBuilder();

            for (ConfigProfiles.Part var35 : var26.parts()) {
               if (var33.length() > 0) {
                  var33.append("  \u00b7  ");
               }

               var33.append(var35.label);
            }

            EfnWidgets.text(
               var1,
               this.fSmall,
               var30,
               var27 + var6 * 0.5F + EfnWidgets.s(6.0F),
               this.f(var26.broken() ? rgba(255, 110, 110, 255) : MUTED),
               var26.broken() ? "Could not be read" : var33.toString()
            );
            float var49 = var2 + var4 - var7 - EfnWidgets.s(80.0F);
            boolean var53 = var26.name().equals(this.armedDelete);
            if (this.pill(var1, var49, var28, EfnWidgets.s(80.0F), var8, var53 ? "Sure?" : "Delete", var53) && this.clicked) {
               if (var53) {
                  try {
                     var5.delete(var26.name());
                     this.say("Deleted " + var26.name(), false);
                  } catch (ProfileException var40) {
                     this.say(var40.getMessage(), true);
                  }

                  this.armedDelete = null;
               } else {
                  this.armedDelete = var26.name();
                  this.armedAt = System.nanoTime();
               }
            }

            var49 -= var24 + var23;
            if (this.pill(var1, var49, var28, var23, var8, "Share", false) && this.clicked) {
               try {
                  ImGui.setClipboardText(var5.shareCode(var26.name()));
                  this.say("Share code for " + var26.name() + " copied", false);
               } catch (ProfileException var39) {
                  this.say(var39.getMessage(), true);
               }
            }

            var49 -= var24 + var23;
            if (this.pill(var1, var49, var28, var23, var8, "Save", false) && this.clicked) {
               try {
                  var5.save(var26.name());
                  this.say("Saved over " + var26.name(), false);
               } catch (ProfileException var38) {
                  this.say(var38.getMessage(), true);
               }
            }

            var49 -= var24 + var23;
            if (this.pill(var1, var49, var28, var23, var8, "Load", var29) && this.clicked) {
               try {
                  var5.load(var26.name());
                  this.say("Loaded " + var26.name(), false);
               } catch (ProfileException var37) {
                  this.say(var37.getMessage(), true);
               }
            }
         }

         return var10;
      }
   }

   private float pageHud(ImDrawList var1, float var2, float var3, float var4) {
      float var5 = EfnWidgets.s(54.0F);
      float var6 = EfnWidgets.s(18.0F);
      float var7 = EfnWidgets.s(32.0F);
      float var9 = this.rowsTop(var1, var2, var3, var4, var5, 2);
      this.rowLabel(var1, var2, var3, var5, "Layout editor");
      float var10 = EfnWidgets.s(110.0F);
      if (this.pill(var1, var2 + var4 - var6 - var10, var3 + (var5 - var7) * 0.5F, var10, var7, "Open", false) && this.clicked) {
         MinecraftClient.method_1551().method_1507(new HudEditorScreen());
      }

      float var11 = var3 + var5;
      this.rowLabel(var1, var2, var11, var5, "Reset layout");
      if (this.pill(var1, var2 + var4 - var6 - var10, var11 + (var5 - var7) * 0.5F, var10, var7, "Reset", false) && this.clicked) {
         HudLayoutStore.getInstance().resetToDefaults(ImGui.getIO().getDisplaySizeX(), ImGui.getIO().getDisplaySizeY());
         this.say("HUD layout reset", false);
      }

      ClientModule var12 = null;

      for (ClientModule var14 : ModuleRegistry.all()) {
         if (var14.category() == ModuleCategory.CLIENT && "HUD".equals(var14.name())) {
            var12 = var14;
            break;
         }
      }

      if (var12 == null) {
         return var9;
      } else {
         ArrayList var17 = new ArrayList();

         for (Setting var15 : var12.settings()) {
            if (var15.isVisible()) {
               var17.add(var15);
            }
         }

         if (var17.isEmpty()) {
            return var9;
         } else {
            float var8 = var9 + EfnWidgets.s(14.0F);
            var9 = this.rowsTop(var1, var2, var8, var4, var5, var17.size());

            for (int var19 = 0; var19 < var17.size(); var19++) {
               this.settingRow(var1, var2, var8 + var19 * var5, var4, var5, var12, (Setting)var17.get(var19));
            }

            return var9;
         }
      }
   }

   private boolean pill(ImDrawList var1, float var2, float var3, float var4, float var5, String var6, boolean var7) {
      boolean var8 = this.hot(var2, var3, var4, var5);
      var1.addRectFilled(var2, var3, var2 + var4, var3 + var5, this.f(var8 ? BG_CARD_HOVER : BG_FIELD), EfnWidgets.s(9.0F));
      var1.addRect(var2, var3, var2 + var4, var3 + var5, this.f(var7 ? GuiPalette.accent() : LINE), EfnWidgets.s(9.0F), 0, var7 ? 1.4F : 1.0F);
      String var9 = EfnWidgets.ellipsize(this.fSmall, var6, var4 - EfnWidgets.s(24.0F));
      float var10 = EfnWidgets.textWidth(this.fSmall, var9);
      EfnWidgets.text(var1, this.fSmall, var2 + (var4 - var10) * 0.5F, var3 + (var5 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F, this.f(TEXT), var9);
      return var8;
   }

   private void trackView() {
      String var1 = (this.category == null ? "" : this.category.name()) + "|" + this.search.get();
      if (!var1.equals(this.viewKey)) {
         this.viewKey = var1;
         this.viewChangedAt = System.nanoTime();
      }
   }

   private float sinceViewChange() {
      return (float)((System.nanoTime() - this.viewChangedAt) / 1.0E9);
   }

   private void drawLogo(ImDrawList var1, float var2, float var3, float var4, int var5) {
      float var6 = var4 / 64.0F;
      var1.addRectFilled(var2, var3, var2 + var4, var3 + var4, this.f(rgba(26, 29, 36, 255)), 15.0F * var6);
      float[][] var7 = new float[][]{{14.0F, 12.0F, 50.0F, 22.0F}, {14.0F, 22.0F, 27.0F, 42.0F}, {27.0F, 27.0F, 44.0F, 37.0F}, {14.0F, 42.0F, 50.0F, 52.0F}};

      for (float[] var11 : var7) {
         float var12 = var11[0];
         float var13 = var11[1];
         float var14 = var11[2];
         float var15 = var11[3];
         float var16 = var2 + (var12 - 0.25F * var13 + 8.0F) * var6;
         float var17 = var3 + var13 * var6;
         float var18 = var2 + (var14 - 0.25F * var13 + 8.0F) * var6;
         float var19 = var2 + (var14 - 0.25F * var15 + 8.0F) * var6;
         float var20 = var3 + var15 * var6;
         float var21 = var2 + (var12 - 0.25F * var15 + 8.0F) * var6;
         var1.addQuadFilled(var16, var17, var18, var17, var19, var20, var21, var20, this.f(var5));
      }
   }

   private void drawSwitch(ImDrawList var1, float var2, float var3, float var4, float var5, float var6) {
      float var7 = var5 * 0.5F;
      var1.addRectFilled(var2, var3, var2 + var4, var3 + var5, this.f(EfnTheme.lerpColor(SWITCH_OFF, GuiPalette.accent(), var6)), var7);
      float var8 = var7 - EfnWidgets.s(3.0F);
      float var9 = var2 + var7 + (var4 - 2.0F * var7) * var6;
      var1.addCircleFilled(var9, var3 + var7 + EfnWidgets.s(1.0F), var8, this.f(rgba(0, 0, 0, 50)), 20);
      var1.addCircleFilled(var9, var3 + var7, var8, this.f(KNOB), 20);
   }

   private void drawCog(ImDrawList var1, float var2, float var3, float var4, int var5) {
      var1.addCircle(var2, var3, var4 * 0.55F, var5, 14, 1.5F);

      for (int var6 = 0; var6 < 8; var6++) {
         double var7 = 6.283185307179586 * var6 / 8.0;
         float var9 = (float)Math.cos(var7);
         float var10 = (float)Math.sin(var7);
         var1.addLine(var2 + var9 * var4 * 0.85F, var3 + var10 * var4 * 0.85F, var2 + var9 * var4 * 1.15F, var3 + var10 * var4 * 1.15F, var5, 2.0F);
      }

      var1.addCircle(var2, var3, var4 * 0.95F, var5, 18, 1.5F);
   }

   private void drawIcon(ImDrawList var1, ModuleCategory.Icon var2, float var3, float var4, float var5, int var6) {
      switch (var2) {
         case CROSSHAIR:
            EfnWidgets.crosshair(var1, var3, var4, var5, var6);
            break;
         case MOVE:
            EfnWidgets.move(var1, var3, var4, var5, var6);
            break;
         case GRID:
            EfnWidgets.grid(var1, var3, var4, var5, var6);
            break;
         case RING:
            EfnWidgets.ring(var1, var3, var4, var5, var6);
            break;
         case EYE:
            EfnWidgets.eye(var1, var3, var4, var5, var6);
            break;
         case MONITOR:
            EfnWidgets.monitor(var1, var3, var4, var5, var6);
            break;
         case HOUSE:
            EfnWidgets.house(var1, var3, var4, var5, var6);
            break;
         case SCROLL:
            EfnWidgets.scroll(var1, var3, var4, var5, var6);
            break;
         case BOLT:
            EfnWidgets.bolt(var1, var3, var4, var5, var6);
            break;
         default:
            EfnWidgets.chevronRight(var1, var3, var4, var5, var6);
      }
   }

   private void drawGeneralIcon(ImDrawList var1, int var2, float var3, float var4, float var5, int var6) {
      switch (var2) {
         case 0:
            EfnWidgets.monitor(var1, var3, var4, var5, var6);
            break;
         case 1:
            EfnWidgets.ring(var1, var3, var4, var5, var6);
            break;
         case 2:
            EfnWidgets.eye(var1, var3, var4, var5, var6);
            break;
         default:
            EfnWidgets.grid(var1, var3, var4, var5, var6);
      }
   }

   private void renderBackPill(float var1, float var2) {
      ImDrawList var3 = ImGui.getForegroundDrawList();
      float var4 = EfnWidgets.s(118.0F);
      float var5 = EfnWidgets.s(36.0F);
      float var6 = EfnWidgets.s(20.0F);
      float var7 = var2 - EfnWidgets.s(20.0F) - var5;
      float var8 = ImGui.getMousePosX();
      float var9 = ImGui.getMousePosY();
      boolean var10 = var8 >= var6 && var8 < var6 + var4 && var9 >= var7 && var9 < var7 + var5;
      var3.addRectFilled(var6, var7, var6 + var4, var7 + var5, var10 ? BG_CARD_HOVER : BG_SIDEBAR, var5 * 0.5F);
      var3.addRect(var6, var7, var6 + var4, var7 + var5, GuiPalette.accent(), var5 * 0.5F, 0, 1.3F);
      EfnWidgets.chevronLeft(var3, var6 + EfnWidgets.s(20.0F), var7 + var5 * 0.5F, EfnWidgets.s(5.0F), TEXT);
      ImFont var11 = ImGuiManager.getInstance().smallFont();
      EfnWidgets.text(var3, var11, var6 + EfnWidgets.s(36.0F), var7 + (var5 - EfnWidgets.lineHeight(var11)) * 0.5F, TEXT, "efn");
      if (var10 && ImGui.isMouseClicked(0)) {
         this.view = EfnClickGui.View.MODULES;
      }
   }

   private void updateKeyListening() {
      if (this.listeningModule != null || this.listeningKey != null) {
         if (this.listenFirstFrame) {
            this.listenFirstFrame = false;
         } else {
            int var1 = ClickGuiRenderer.scanPressedKey();
            if (var1 != -1) {
               if (this.listeningKey != null) {
                  this.listeningKey.setValue(var1);
               } else {
                  this.listeningModule.setKeybind(var1);
               }

               this.listeningKey = null;
               this.listeningModule = null;
            }
         }
      }
   }

   private void toggle(ClientModule var1) {
      if (var1.isAction()) {
         var1.onClick();
      } else {
         var1.setEnabled(!var1.isEnabled());
      }
   }

   private List<ClientModule> currentModules() {
      String var1 = this.search.get().trim().toLowerCase(Locale.ROOT);
      if (var1.isEmpty()) {
         return (List<ClientModule>)(this.category == null ? new ArrayList<>() : ModuleRegistry.inCategory(this.category));
      } else {
         ArrayList var2 = new ArrayList();

         for (ClientModule var4 : ModuleRegistry.all()) {
            String var5 = var4.description() == null ? "" : var4.description().toLowerCase(Locale.ROOT);
            if (var4.name().toLowerCase(Locale.ROOT).contains(var1) || var5.contains(var1)) {
               var2.add(var4);
            }
         }

         return var2;
      }
   }

   private void refreshCategories() {
      int var1 = ModuleRegistry.revision();
      if (var1 != this.catCacheRevision) {
         ArrayList var2 = new ArrayList();

         for (ModuleCategory var4 : ModuleCategory.values()) {
            if (!ModuleRegistry.inCategory(var4).isEmpty()) {
               var2.add(var4);
            }
         }

         this.catCache = var2;
         this.catCacheRevision = var1;
      }

      if ((this.category == null || !this.catCache.contains(this.category)) && !this.catCache.isEmpty()) {
         this.category = this.catCache.get(0);
      }
   }

   private String label(ModuleCategory var1) {
      return var1.displayName().replace("EFN", "efn".toUpperCase(Locale.ROOT));
   }

   private String keyLabel(int var1) {
      String var2 = this.keyLabels.get(var1);
      if (var2 == null) {
         var2 = ClickGuiRenderer.keyName(var1);
         this.keyLabels.put(var1, var2);
      }

      return var2;
   }

   private float[] state(String var1, boolean var2) {
      float[] var3 = this.anim.get(var1);
      if (var3 == null) {
         var3 = new float[]{0.0F, var2 ? 1.0F : 0.0F};
         this.anim.put(var1, var3);
      }

      return var3;
   }

   private boolean hot(float var1, float var2, float var3, float var4) {
      return this.winHover && !this.popupBlock && this.mx >= var1 && this.mx < var1 + var3 && this.my >= var2 && this.my < var2 + var4;
   }

   private void pushChildStyle() {
      ImGui.pushStyleColor(3, 0);
      ImGui.pushStyleColor(14, 0);
      ImGui.pushStyleColor(15, a(GuiPalette.accent(), 0.45F));
      ImGui.pushStyleColor(16, a(GuiPalette.accent(), 0.7F));
      ImGui.pushStyleColor(17, GuiPalette.accent());
      ImGui.pushStyleVar(18, EfnWidgets.s(5.0F));
      ImGui.pushStyleVar(2, 0.0F, 0.0F);
   }

   private void popChildStyle() {
      ImGui.popStyleVar(2);
      ImGui.popStyleColor(5);
   }

   private void pushPopupStyle() {
      ImGui.pushStyleColor(4, rgba(16, 14, 30, 252));
      ImGui.pushStyleColor(5, rgba(255, 255, 255, 28));
      ImGui.pushStyleColor(24, a(GuiPalette.accent(), 0.3F));
      ImGui.pushStyleColor(25, a(GuiPalette.accent(), 0.2F));
      ImGui.pushStyleColor(26, a(GuiPalette.accent(), 0.4F));
      ImGui.pushStyleColor(0, TEXT);
      ImGui.pushStyleVar(2, EfnWidgets.s(8.0F), EfnWidgets.s(8.0F));
      ImGui.pushStyleVar(9, EfnWidgets.s(10.0F));
      ImGui.pushStyleVar(10, 1.0F);
      ImGui.pushStyleVar(14, EfnWidgets.s(6.0F), EfnWidgets.s(4.0F));
   }

   private void popPopupStyle() {
      ImGui.popStyleVar(4);
      ImGui.popStyleColor(6);
   }

   private int f(int var1) {
      return a(var1, this.ga);
   }

   private static int a(int var0, float var1) {
      int var2 = Math.round((var0 >>> 24) * Math.max(0.0F, Math.min(1.0F, var1)));
      return var2 << 24 | var0 & 16777215;
   }

   private static int rgba(int var0, int var1, int var2, int var3) {
      return var3 << 24 | var2 << 16 | var1 << 8 | var0;
   }

   private static enum View {
      MODULES,
      PAGE,
      LEGACY;

      private View() {
      }
   }
}
