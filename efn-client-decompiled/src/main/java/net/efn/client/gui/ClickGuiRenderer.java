package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.efn.client.config.BundledDefaults;
import net.efn.client.config.ConfigProfiles;
import net.efn.client.config.FriendStore;
import net.efn.client.config.HudTheme;
import net.efn.client.config.ModuleCodes;
import net.efn.client.config.ModuleConfigStore;
import net.efn.client.config.ProfileException;
import net.efn.client.config.Surface;
import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.client.HotkeysModule;
import net.efn.client.module.impl.client.HudModule;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.module.impl.client.RadarModule;
import net.efn.client.module.impl.client.RegionMapModule;
import net.efn.client.module.setting.ActionSetting;
import net.efn.client.module.setting.BlockColorListSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ItemColorListSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.efn.client.module.setting.StringListSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class ClickGuiRenderer {
   private static final float PANEL_W = 212.0F;
   private static final float PANEL_GAP = 14.0F;
   private static final float PANEL_TOP = 28.0F;
   private static final float HEADER_H = 42.0F;
   private static final float ROW_H = 30.0F;
   private static final float ROW_GAP = 2.0F;
   private static final float ROW_INSET = 6.0F;
   private static final int HEADER_BAND = 251658239;
   private static final int ROW_RULE = 620756991;
   private static final float CARD_W = 460.0F;
   private static final float CARD_MAX_H = 760.0F;
   private static final float CARD_PAD = 24.0F;
   private static final float SETTING_ROW_H = 46.0F;
   private static final float TAB_BAR_H = 44.0F;
   private static final float TAB_BAR_BOTTOM = 26.0F;
   private static final float INSPECTOR_W = 360.0F;
   private static final float SMALL_TOG_W = 16.0F;
   private static final float SMALL_TOG_H = 16.0F;
   public static final int ERROR_TEXT = -10856974;
   private static final int[] AVATAR_COLORS = new int[]{-624588, -10893516, -16080897, -2207057, -10536961, -3690448, -2730408, -16724737};
   private static final int PANEL_FLAGS = 123;
   private static final int CARD_FLAGS = 8511;
   public static final int CHILD_FLAGS = 24;
   private static final String PANEL_ID_PREFIX = "##panel_";
   private static final String SEARCH_PANEL_ID = "##panel_search";
   private static final int STYLE_VAR_COUNT = 11;
   private static final int DYNAMIC_COLOR_COUNT = 10;
   private static final int[] THEME_COLORS = new int[]{
      0,
      -461582,
      1,
      -4542808,
      3,
      0,
      4,
      -14015199,
      5,
      352321535,
      6,
      0,
      7,
      -13226454,
      8,
      -12240588,
      9,
      -12240588,
      21,
      -13226454,
      22,
      -12240588,
      24,
      -13226454,
      25,
      -12240588,
      27,
      268435455,
      14,
      0,
      15,
      822083583,
      16,
      1358954495,
      17,
      1895825407,
      30,
      0,
      31,
      0,
      32,
      0,
      53,
      0
   };
   private ClickGuiRenderer.Tab tab = ClickGuiRenderer.Tab.MODULES;
   private final long guiOpenedAt;
   private long viewChangedAt;
   private float tabIndicatorX;
   private float tabIndicatorW;
   private ClientModule settingsModule;
   private boolean cardJustOpened;
   private boolean dividerPending;
   private boolean panelGuard;
   private boolean tabBarGuard;
   private boolean cardGuard;
   private boolean cardTextLift;
   private final HashMap<String, float[]> scroll;
   private static float scrollDragFrom;
   private static final ImBoolean SCROLLBAR_HOVERED = new ImBoolean();
   private static final ImBoolean SCROLLBAR_HELD = new ImBoolean();
   private ClientModule listeningModule;
   private boolean listeningFirstFrame;
   private KeySetting listeningKeySetting;
   private boolean listeningKeyFirstFrame;
   private final ImBoolean colorPickerOpen;
   private ClientModule activeColorModule;
   private ColorSetting activeColorSetting;
   private final ImString globalSearch;
   private boolean focusSearch;
   private float searchBodyH;
   private final Map<String, float[]> panelSpots;
   private final Map<String, float[]> panelHomes;
   private final Map<String, float[]> panelTargets;
   private String panelLayoutKey;
   private boolean panelHomesDirty;
   private ClickGuiRenderer.CategoryPanel[] panelCache;
   private int panelCacheRevision;
   private final Map<String, float[]> panelFold;
   private final Map<String, Motion.Rows> rowMotion;
   private final Int2ObjectOpenHashMap<String> keyLabels;
   private int cachedRegistryRevision;
   private String cachedGlobalSearch;
   private List<ClientModule> cachedGlobalResults;
   private final ImString friendInput;
   private boolean focusFriendInput;
   private String friendMessage;
   private long friendMessageAt;
   private final ImString profileInput;
   private boolean focusProfileInput;
   private String configMessage;
   private boolean configMessageError;
   private long configMessageAt;
   private String moduleCodeFor;
   private String moduleCodeMessage;
   private boolean moduleCodeError;
   private long moduleCodeAt;
   private final ProfileList profileList;
   private String pendingConfirm;
   private long pendingConfirmAt;
   private String themeMessage;
   private boolean themeMessageError;
   private long themeMessageAt;
   private Surface themeSurface;
   private boolean inspectMode;
   private final List<float[]> panelRects;
   private float[] cardRect;
   private float[] tabBarRect;
   private float[] inspectorRect;
   private Surface hoveredSurface;
   private boolean rowDividers;
   private float rowAboveEmphasis;
   private boolean focusInspector;
   private long previewChangedAt;
   private String hudElementId;
   private boolean focusHudInspector;
   private String hudMessage;
   private boolean hudMessageError;
   private long hudMessageAt;
   private final BlockListEditor blockLists;
   private final ItemPickerEditor itemPickers;
   private ItemColorListSetting activeItemColorPalette;
   private boolean itemColorWindowOpen;
   private final ImString itemSearchBuf;
   private ItemColorListSetting.ItemEntry activeItemColorEntry;
   private boolean editingItemDefaultColor;
   private final HashMap<ItemColorListSetting.ItemEntry, Float> itemEntryTogAnim;
   private final ImBoolean itemColorPickerOpen;

   public ClickGuiRenderer() {
      this.viewChangedAt = this.guiOpenedAt = System.nanoTime();
      this.tabIndicatorX = -1.0F;
      this.scroll = new HashMap<>();
      this.listeningModule = null;
      this.listeningFirstFrame = false;
      this.listeningKeySetting = null;
      this.listeningKeyFirstFrame = false;
      this.colorPickerOpen = new ImBoolean(false);
      this.activeColorModule = null;
      this.activeColorSetting = null;
      this.globalSearch = new ImString(128);
      this.panelSpots = new HashMap<>();
      this.panelHomes = new HashMap<>();
      this.panelTargets = new HashMap<>();
      this.panelLayoutKey = "";
      this.panelCache = new ClickGuiRenderer.CategoryPanel[0];
      this.panelCacheRevision = -1;
      this.panelFold = new HashMap<>();
      this.rowMotion = new HashMap<>();
      this.keyLabels = new Int2ObjectOpenHashMap();
      this.cachedRegistryRevision = -1;
      this.cachedGlobalSearch = "";
      this.cachedGlobalResults = List.of();
      this.friendInput = new ImString(32);
      this.profileInput = new ImString(64);
      this.profileList = new ProfileList(this::setConfigMessage);
      this.panelRects = new ArrayList<>();
      this.blockLists = new BlockListEditor();
      this.itemPickers = new ItemPickerEditor();
      this.activeItemColorPalette = null;
      this.itemColorWindowOpen = false;
      this.itemSearchBuf = new ImString(256);
      this.activeItemColorEntry = null;
      this.editingItemDefaultColor = false;
      this.itemEntryTogAnim = new HashMap<>();
      this.itemColorPickerOpen = new ImBoolean(false);
   }

   public void render() {
      float var1 = animationDelta(Math.max(ImGui.getIO().getDeltaTime(), 0.001F));
      ThemeStore var2 = ThemeStore.get();
      this.panelGuard = HudChrome.needsTextGuard(var2.panels);
      this.tabBarGuard = HudChrome.needsTextGuard(var2.tabBar);
      this.cardGuard = HudChrome.needsTextGuard(var2.cards);
      this.cardTextLift = var2.cards.isImage() && !var2.cards.isBanner();
      applyTheme();
      boolean var3 = pushFont(ImGuiManager.getInstance().moduleFont());

      try {
         float var4 = ImGui.getIO().getDisplaySizeX();
         float var5 = ImGui.getIO().getDisplaySizeY();
         float var6 = easeOut(elapsed(this.guiOpenedAt, 240.0F));
         float var7 = easeOut(elapsed(this.viewChangedAt, 260.0F));
         float var8 = Math.max(0.01F, var6 * var7);
         ImGui.getBackgroundDrawList().addRectFilled(0.0F, 0.0F, var4, var5, EfnWidgets.withAlpha(-16777216, var2.backgroundDim.get() * var6));
         Path var9 = ThemeImages.get().takePendingImport();
         if (var9 != null) {
            this.importHeaderImage(var9);
         }

         this.handleShortcuts();
         switch (this.tab) {
            case MODULES:
               if (this.settingsModule == null) {
                  this.renderModulePanels(var1, var8);
               } else {
                  this.renderModuleCard(this.settingsModule, var1, var8, var7);
               }
               break;
            case HUD:
               this.renderHudStudio(var1, var8, var7);
               break;
            case FRIENDS:
               this.renderFriendsCard(var1, var8, var7);
               break;
            case THEME:
               this.renderThemeStudio(var1, var8, var7);
               break;
            case CONFIG:
               this.renderConfigCard(var1, var8, var7);
         }

         this.renderTabBar(var1, var6);
         this.savePanelHomes();
         this.renderColorPickerWindow();
         this.blockLists.renderWindows();
         this.itemPickers.renderWindows();
         this.renderItemColorPaletteWindow();
         this.closeCardOnOutsideClick();
      } finally {
         if (var3) {
            ImGui.popFont();
         }

         removeTheme();
      }
   }

   private void handleShortcuts() {
      if (this.tab == ClickGuiRenderer.Tab.MODULES && this.settingsModule == null && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(551)) {
         this.focusSearch = true;
      }
   }

   public void showModuleSettings(ClientModule var1) {
      this.switchTab(ClickGuiRenderer.Tab.MODULES);
      this.openSettings(var1);
   }

   private void switchTab(ClickGuiRenderer.Tab var1) {
      if (var1 != this.tab) {
         this.closeSettings();
         this.tab = var1;
         this.viewChangedAt = System.nanoTime();
         this.pendingConfirm = null;
         this.profileList.disarm();
         if (var1 == ClickGuiRenderer.Tab.CONFIG) {
            ConfigProfiles.getInstance().refresh();
         }
      }
   }

   private void renderModulePanels(float var1, float var2) {
      ClickGuiRenderer.CategoryPanel[] var3 = this.visiblePanels();
      boolean var4 = this.panelHomes.containsKey("##panel_search");

      for (ClickGuiRenderer.CategoryPanel var8 : var3) {
         var4 &= this.panelHomes.containsKey(var8.id());
      }

      if (!var4) {
         this.placeNewPanels(var3);
      }

      this.fitPanels(var3);

      for (ClickGuiRenderer.CategoryPanel var12 : var3) {
         this.renderCategoryPanel(var12, var1, var2);
      }

      this.renderSearchPanel(var1, var2);
   }

   private ClickGuiRenderer.CategoryPanel[] visiblePanels() {
      int var1 = ModuleRegistry.revision();
      if (var1 != this.panelCacheRevision) {
         ArrayList var2 = new ArrayList();

         for (ModuleCategory var4 : ModuleCategory.values()) {
            if (!this.modulesFor(var4).isEmpty()) {
               var2.add(new ClickGuiRenderer.CategoryPanel(var4, "##panel_" + var4.name(), "##rows_" + var4.name()));
            }
         }

         this.panelCache = var2.toArray(ClickGuiRenderer.CategoryPanel[]::new);
         this.panelCacheRevision = var1;
      }

      return this.panelCache;
   }

   private void renderCategoryPanel(ClickGuiRenderer.CategoryPanel var1, float var2, float var3) {
      List var4 = this.modulesFor(var1.category());
      float var5 = Math.min(rowListHeight(var4.size()), maxListHeight());
      float var6 = this.unfolded(var1, var2);
      if (var6 < 1.0F) {
         ImGui.setNextWindowSize(EfnWidgets.s(212.0F), EfnWidgets.s(42.0F) + var5 * var6, 1);
      }

      if (this.beginPanel(var1.id(), var3)) {
         String var7 = var1.category().displayName();
         this.drawPanelChrome(var7, var1.category());
         this.recordPanelRect();
         this.renderFoldControl(var1.category(), var6);
         if (var6 > 0.0F) {
            this.renderRowList(var1.rowsId(), var4, var5, false, var2);
         }
      }

      endPanel();
   }

   private float unfolded(ClickGuiRenderer.CategoryPanel var1, float var2) {
      float var3 = PanelStateStore.getInstance().isCollapsed(var1.category()) ? 0.0F : 1.0F;
      float[] var4 = this.panelFold.get(var1.id());
      if (var4 == null) {
         var4 = new float[]{var3};
         this.panelFold.put(var1.id(), var4);
      }

      var4[0] = smooth(var4[0], var3, var2, 14.0F);
      if (Math.abs(var4[0] - var3) < 0.002F) {
         var4[0] = var3;
      }

      return var4[0];
   }

   private void renderFoldControl(ModuleCategory var1, float var2) {
      float var3 = ImGui.getWindowPosX();
      float var4 = ImGui.getWindowPosY();
      float var5 = ImGui.getWindowWidth();
      float var6 = EfnWidgets.s(42.0F);
      float var7 = EfnWidgets.s(22.0F);
      float var8 = var3 + var5 - EfnWidgets.s(20.0F);
      float var9 = var4 + var6 * 0.5F;
      boolean var10 = !this.inspectMode && ImGui.isWindowHovered() && ImGui.isMouseHoveringRect(var3, var4, var3 + var5, var4 + var6);
      ImGui.setCursorScreenPos(var8 - var7 * 0.5F, var9 - var7 * 0.5F);
      boolean var11 = ImGui.invisibleButton("##fold", var7, var7) && !this.inspectMode;
      float var12 = EfnWidgets.animate(ImGui.getItemID(), var10, 18.0F);
      ImGui.setCursorPos(0.0F, var6);
      if (var11 || var10 && ImGui.isMouseClicked(1)) {
         PanelStateStore var13 = PanelStateStore.getInstance();
         var13.setCollapsed(var1, !var13.isCollapsed(var1));
      }

      EfnWidgets.chevronRotated(ImGui.getWindowDrawList(), var8, var9, EfnWidgets.s(4.0F), EfnTheme.lerpColor(-4542808, -461582, var12), var2 * 1.5707964F);
   }

   private void renderSearchPanel(float var1, float var2) {
      if (this.beginPanel("##panel_search", var2)) {
         this.drawPanelChrome("Search", null);
         this.recordPanelRect();
         float var4 = EfnWidgets.s(6.0F) + EfnWidgets.s(2.0F);
         ImGui.setCursorPos(var4, EfnWidgets.s(42.0F) - EfnWidgets.s(4.0F));
         ImGui.pushStyleVar(11, EfnWidgets.s(10.0F), EfnWidgets.s(7.0F));
         ImGui.setNextItemWidth(EfnWidgets.s(212.0F) - var4 * 2.0F);
         if (this.focusSearch) {
            ImGui.setKeyboardFocusHere();
            this.focusSearch = false;
         }

         ImGui.inputTextWithHint("##globalsearch", "Search modules...", this.globalSearch);
         ImGui.popStyleVar();
         ImGui.dummy(0.0F, EfnWidgets.s(8.0F));
         String var5 = normalizeSearch(this.globalSearch.get());
         List var3 = var5.isEmpty() ? List.of() : this.searchModules(var5);
         float var7 = var5.isEmpty() ? 0.0F : (var3.isEmpty() ? EfnWidgets.s(34.0F) : Math.min(rowListHeight(var3.size()), maxListHeight()));
         this.searchBodyH = smooth(this.searchBodyH, var7, var1, 14.0F);
         if (Math.abs(this.searchBodyH - var7) < 0.5F) {
            this.searchBodyH = var7;
         }

         if (this.searchBodyH >= 1.0F) {
            if (var3.isEmpty()) {
               ImGui.beginChild("##search_empty", EfnWidgets.s(212.0F), this.searchBodyH, false, 24);
               String var8 = "No results";
               ImFont var9 = ImGui.getFont();
               EfnWidgets.text(
                  ImGui.getWindowDrawList(),
                  var9,
                  ImGui.getWindowPosX() + (EfnWidgets.s(212.0F) - EfnWidgets.textWidth(var9, var8)) * 0.5F,
                  ImGui.getWindowPosY() + EfnWidgets.s(4.0F),
                  -4542808,
                  var8,
                  this.panelGuard
               );
               ImGui.endChild();
            } else {
               this.renderRowList("##search_rows", var3, this.searchBodyH, true, var1);
            }
         }
      }

      endPanel();
   }

   private boolean beginPanel(String var1, float var2) {
      float[] var4 = this.panelTargets.remove(var1);
      if (var4 != null) {
         ImGui.setNextWindowPos(var4[0], var4[1], 1);
      }

      ImGui.setNextWindowSizeConstraints(EfnWidgets.s(212.0F), 0.0F, EfnWidgets.s(212.0F), 3.4028235E38F);
      ImGui.pushStyleVar(0, var2);
      ImGui.pushStyleVar(2, 0.0F, 0.0F);
      ImGui.pushStyleVar(14, 0.0F, 0.0F);
      ImGui.pushStyleVar(3, panelRounding());
      boolean var5 = ImGui.begin(var1, 123);
      float var6 = ImGui.getWindowPosX();
      float var7 = ImGui.getWindowPosY();
      float[] var8 = this.panelSpots.computeIfAbsent(var1, var2x -> new float[]{var6, var7});
      boolean var3 = Math.abs(var6 - var8[0]) > 0.5F || Math.abs(var7 - var8[1]) > 0.5F;
      if (var4 == null && var3 && ImGui.isMouseDown(0)) {
         this.panelHomes.put(var1, new float[]{var6 / EfnWidgets.s(1.0F), var7 / EfnWidgets.s(1.0F)});
         this.panelHomesDirty = true;
      }

      var8[0] = var6;
      var8[1] = var7;
      return var5;
   }

   private void savePanelHomes() {
      if (this.panelHomesDirty && !ImGui.isMouseDown(0)) {
         this.panelHomesDirty = false;
         PanelStateStore.getInstance().setPanelHomes(this.panelHomes);
      }
   }

   private static void endPanel() {
      ImGui.end();
      ImGui.popStyleVar(4);
   }

   private void drawPanelChrome(String var1, ModuleCategory var2) {
      float var3 = ImGui.getWindowPosX();
      float var4 = ImGui.getWindowPosY();
      float var5 = ImGui.getWindowWidth();
      float var6 = ImGui.getWindowHeight();
      drawShadow(var3, var4, var5, var6, panelRounding(), 4);
      ImDrawList var7 = ImGui.getWindowDrawList();
      SurfaceBackground.draw(var7, ThemeStore.get().panels, var3, var4, var5, var6, panelRounding());
      ImFont var8 = ImGuiManager.getInstance().categoryFont();
      float var9 = EfnWidgets.s(42.0F);
      drawHeaderBand(var7, var3, var4, var5, var9);
      float var10 = EfnWidgets.s(7.0F);
      float var11 = var3 + EfnWidgets.s(14.0F) + var10;
      float var12 = var4 + var9 * 0.5F;
      if (var2 == null) {
         EfnWidgets.magnifier(var7, var11, var12, var10, -4542808);
      } else {
         drawCategoryIcon(var7, var2, var11, var12, var10, -4542808);
      }

      float var13 = var11 + var10 + EfnWidgets.s(9.0F);
      float var14 = var4 + (var9 - EfnWidgets.lineHeight(var8)) * 0.5F;
      EfnWidgets.text(var7, var8, var13, var14, -461582, var1, this.panelGuard);
      ImGui.dummy(var5, var9);
   }

   private static void drawHeaderBand(ImDrawList var0, float var1, float var2, float var3, float var4) {
      float var5 = panelRounding();
      float var6 = Math.min(var4, var5 + EfnWidgets.s(2.0F));
      int var7 = EfnWidgets.c(EfnWidgets.withAlpha(251658239, 1.0F - var6 / (2.0F * var4)));
      int var8 = EfnWidgets.c(EfnWidgets.withAlpha(251658239, 1.0F - var6 / var4));
      int var9 = EfnWidgets.c(16777215);
      var0.addRectFilled(var1, var2, var1 + var3, var2 + var6, var7, var5, 48);
      var0.addRectFilledMultiColor(var1, var2 + var6, var1 + var3, var2 + var4, var8, var8, var9, var9);
   }

   private static void softRule(ImDrawList var0, float var1, float var2, float var3, int var4) {
      float var5 = (var1 + var2) * 0.5F;
      int var6 = EfnWidgets.c(var4);
      int var7 = EfnWidgets.c(var4 & 16777215);
      var0.addRectFilledMultiColor(var1, var3, var5, var3 + 1.0F, var7, var6, var6, var7);
      var0.addRectFilledMultiColor(var5, var3, var2, var3 + 1.0F, var6, var7, var7, var6);
   }

   private static void drawCategoryIcon(ImDrawList var0, ModuleCategory var1, float var2, float var3, float var4, int var5) {
      switch (var1.icon()) {
         case CROSSHAIR:
            EfnWidgets.crosshair(var0, var2, var3, var4, var5);
            break;
         case CHEVRON:
            EfnWidgets.chevron(var0, var2, var3, var4, var5, true);
            break;
         case MOVE:
            EfnWidgets.move(var0, var2, var3, var4, var5);
            break;
         case GRID:
            EfnWidgets.grid(var0, var2, var3, var4, var5);
            break;
         case RING:
            EfnWidgets.ring(var0, var2, var3, var4, var5);
            break;
         case EYE:
            EfnWidgets.eye(var0, var2, var3, var4, var5);
            break;
         case MONITOR:
            EfnWidgets.monitor(var0, var2, var3, var4, var5);
            break;
         case HOUSE:
            EfnWidgets.house(var0, var2, var3, var4, var5);
            break;
         case SCROLL:
            EfnWidgets.scroll(var0, var2, var3, var4, var5);
            break;
         case BOLT:
            EfnWidgets.bolt(var0, var2, var3, var4, var5);
      }
   }

   public static void drawShadow(float var0, float var1, float var2, float var3, float var4, int var5) {
      ImDrawList var6 = ImGui.getBackgroundDrawList();

      for (int var7 = 1; var7 <= var5; var7++) {
         float var8 = EfnWidgets.s(2.5F) * var7;
         var6.addRectFilled(
            var0 - var8, var1 - var8 + EfnWidgets.s(4.0F), var0 + var2 + var8, var1 + var3 + var8 + EfnWidgets.s(4.0F), EfnWidgets.c(335544320), var4 + var8
         );
      }
   }

   private static float rowListHeight(int var0) {
      return var0 * EfnWidgets.s(32.0F) - EfnWidgets.s(2.0F) + EfnWidgets.s(6.0F);
   }

   private static float maxListHeight() {
      return Math.max(EfnWidgets.s(120.0F), ImGui.getIO().getDisplaySizeY() * 0.62F - EfnWidgets.s(42.0F));
   }

   private void renderRowList(String var1, List<ClientModule> var2, float var3, boolean var4, float var5) {
      if (ImGui.beginChild(var1, EfnWidgets.s(212.0F), Math.max(1.0F, var3), false, 24)) {
         Motion.Rows var6 = this.rowMotion.computeIfAbsent(var1, var0 -> new Motion.Rows());
         var6.bind(var2);
         this.rowDividers = ThemeStore.get().moduleDividers.get();

         for (int var7 = 0; var7 < var2.size(); var7++) {
            this.renderModuleRow((ClientModule)var2.get(var7), var4, var6, var7, var5);
         }

         ImGui.dummy(1.0F, EfnWidgets.s(6.0F) - EfnWidgets.s(2.0F));
         this.applySmoothScroll(var1, var5);
      }

      ImGui.endChild();
   }

   private void renderModuleRow(ClientModule var1, boolean var2, Motion.Rows var3, int var4, float var5) {
      ImGui.pushID(var1.id());
      float var7 = EfnWidgets.s(6.0F);
      float var8 = EfnWidgets.s(30.0F);
      float var9 = ImGui.getCursorPosY();
      ImGui.setCursorPosX(var7);
      float var10 = ImGui.getCursorScreenPosX();
      float var11 = ImGui.getCursorScreenPosY();
      float var12 = ImGui.getWindowWidth() - var7 * 2.0F;
      if (ImGui.invisibleButton("##row", var12, var8) && !this.inspectMode) {
         if (var1.isAction()) {
            var1.onClick();
         } else {
            var1.toggle();
         }
      }

      boolean var13 = ImGui.isItemHovered();
      if (!this.inspectMode && ImGui.isItemClicked(1)) {
         this.openSettings(var1);
      }

      if (!this.inspectMode && ImGui.isItemHovered(65536)) {
         String var14 = var1.description();
         showTooltip(var14 != null && !var14.isBlank() ? var14 + "\nRight-click for settings" : "Right-click for settings");
      }

      float var27 = var3.hover(var4, var13, var5, 18.0F);
      float var15 = var3.on(var4, var1.isEnabled(), var5, 14.0F);
      ImDrawList var16 = ImGui.getWindowDrawList();
      float var17 = Math.max(var27, var15);
      float var6;
      if (this.rowDividers && var4 > 0 && (var6 = 1.0F - Math.max(var17, this.rowAboveEmphasis)) > 0.01F) {
         float var18 = (float)Math.floor(var11 - EfnWidgets.s(2.0F) * 0.5F);
         softRule(var16, var10 + EfnWidgets.s(8.0F), var10 + var12 - EfnWidgets.s(8.0F), var18, EfnWidgets.withAlpha(620756991, var6));
      }

      this.rowAboveEmphasis = var17;
      float var28 = EfnWidgets.s(6.0F);
      if (var27 > 0.01F) {
         var16.addRectFilled(var10, var11, var10 + var12, var11 + var8, EfnWidgets.c(EfnWidgets.withAlpha(318767103, var27)), var28);
      }

      if (var15 > 0.01F) {
         var16.addRectFilled(
            var10,
            var11,
            var10 + var12,
            var11 + var8,
            EfnWidgets.c(EfnWidgets.withAlpha(EfnTheme.lerpColor(GuiPalette.accent(), GuiPalette.accentHover(), var27 * 0.3F), var15)),
            var28
         );
      }

      ImFont var19 = ImGui.getFont();
      ImFont var20 = ImGuiManager.getInstance().smallFont();
      String var21 = var2 ? var1.category().displayName() : (var1.keybind() == -1 ? null : this.keyLabel(var1.keybind()));
      float var22 = var21 == null ? 0.0F : EfnWidgets.textWidth(var20, var21);
      int var23 = EfnTheme.lerpColor(EfnTheme.lerpColor(-2042416, -461582, var27), -1, var15);
      float var24 = var12 - EfnWidgets.s(var21 == null && !var1.isAction() ? 20.0F : 28.0F) - var22;
      String var25 = EfnWidgets.ellipsize(var19, var1.name(), var24);
      EfnWidgets.text(var16, var19, var10 + EfnWidgets.s(10.0F), var11 + (var8 - EfnWidgets.lineHeight(var19)) * 0.5F, var23, var25, this.panelGuard);
      if (var21 != null) {
         int var26 = EfnTheme.lerpColor(-4542808, -419430401, var15);
         EfnWidgets.text(
            var16, var20, var10 + var12 - EfnWidgets.s(10.0F) - var22, var11 + (var8 - EfnWidgets.lineHeight(var20)) * 0.5F, var26, var21, this.panelGuard
         );
      } else if (var1.isAction()) {
         EfnWidgets.chevronRight(
            var16, var10 + var12 - EfnWidgets.s(14.0F), var11 + var8 * 0.5F, EfnWidgets.s(4.0F), EfnTheme.lerpColor(-4542808, -461582, var27)
         );
      }

      ImGui.setCursorPos(0.0F, var9 + var8 + EfnWidgets.s(2.0F));
      ImGui.popID();
   }

   public static void showTooltip(String var0) {
      ImGui.pushStyleVar(2, EfnWidgets.s(10.0F), EfnWidgets.s(8.0F));
      ImGui.beginTooltip();
      ImGui.pushTextWrapPos(EfnWidgets.s(280.0F));
      ImGui.textUnformatted(var0);
      ImGui.popTextWrapPos();
      ImGui.endTooltip();
      ImGui.popStyleVar();
   }

   private void applySmoothScroll(String var1, float var2) {
      smoothScroll(this.scroll.computeIfAbsent(var1, var0 -> new float[2]), var2);
   }

   public static void smoothScroll(float[] var0, float var1) {
      float var3 = ImGui.getScrollMaxY();
      float var4 = ImGui.getWindowPosX();
      float var5 = ImGui.getWindowPosY();
      float var6 = ImGui.getWindowWidth();
      float var7 = ImGui.getWindowHeight();
      float var8 = Math.max(EfnWidgets.s(24.0F), var7 * var7 / (var7 + var3));
      float var9 = Math.max(1.0F, var7 - var8);
      boolean var10 = false;
      boolean var11 = false;
      if (var3 > 0.5F) {
         int var12 = ImGui.getID("##scrollbar");
         imgui.internal.ImGui.keepAliveID(var12);
         imgui.internal.ImGui.buttonBehavior(var4 + var6 - EfnWidgets.s(8.0F), var5, var4 + var6, var5 + var7, var12, SCROLLBAR_HOVERED, SCROLLBAR_HELD);
         var10 = SCROLLBAR_HELD.get();
         var11 = var10 || SCROLLBAR_HOVERED.get();
         if (var10 && ImGui.isMouseClicked(0)) {
            float var14 = var5 + var9 * (var0[0] / var3);
            float var15 = ImGui.getMousePosY();
            var0[1] = var15 >= var14 && var15 <= var14 + var8 ? var0[0] : (var15 - var5 - var8 * 0.5F) / var9 * var3;
            scrollDragFrom = var0[1];
         } else if (var10) {
            var0[1] = scrollDragFrom + ImGui.getMouseDragDeltaY(0, 0.0F) * var3 / var9;
         }
      }

      float var2;
      if (!var10 && ImGui.isWindowHovered() && (var2 = ImGui.getIO().getMouseWheel()) != 0.0F) {
         var0[1] -= var2 * EfnWidgets.s(56.0F);
      }

      var0[1] = Math.max(0.0F, Math.min(var3, var0[1]));
      float var16 = var0[0] = var10 ? var0[1] : smooth(var0[0], var0[1], var1, 16.0F);
      if (Math.abs(var0[0] - var0[1]) < 0.3F) {
         var0[0] = var0[1];
      }

      ImGui.setScrollY(var0[0]);
      if (var3 > 0.5F) {
         float var13 = var5 + var9 * (var0[0] / var3);
         ImGui.getWindowDrawList()
            .addRectFilled(
               var4 + var6 - (var11 ? EfnWidgets.s(6.0F) : EfnWidgets.s(4.0F)),
               var13,
               var4 + var6 - EfnWidgets.s(1.5F),
               var13 + var8,
               EfnWidgets.c(var11 ? 1728053247 : 872415231),
               EfnWidgets.s(2.0F)
            );
      }
   }

   private ClickGuiRenderer.PanelLayout panelLayout(ClickGuiRenderer.CategoryPanel[] var1) {
      ArrayList var2 = new ArrayList();
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      PanelStateStore var5 = PanelStateStore.getInstance();

      for (ClickGuiRenderer.CategoryPanel var9 : var1) {
         var2.add(var9.id());
         var3.add(var9.category().name());
         var4.add(
            var5.isCollapsed(var9.category())
               ? EfnWidgets.s(42.0F)
               : EfnWidgets.s(42.0F) + Math.min(rowListHeight(this.modulesFor(var9.category()).size()), maxListHeight())
         );
      }

      var2.add("##panel_search");
      var3.add("search");
      var4.add(EfnWidgets.s(42.0F) + ImGui.getFontSize() + EfnWidgets.s(18.0F) + maxListHeight());
      return new ClickGuiRenderer.PanelLayout(var2, var3, var4);
   }

   private void placeNewPanels(ClickGuiRenderer.CategoryPanel[] var1) {
      ClickGuiRenderer.PanelLayout var2 = this.panelLayout(var1);
      List var3 = var2.ids();
      List var4 = var2.keys();
      List var5 = var2.heights();
      float var6 = EfnWidgets.s(1.0F);
      Map var7 = PanelStateStore.getInstance().panelHomes();
      Map var8 = var7.isEmpty() ? savedPanelSpots() : Map.of();
      ArrayList var9 = new ArrayList();

      for (int var10 = 0; var10 < var3.size(); var10++) {
         String var12 = (String)var3.get(var10);
         if (!this.panelHomes.containsKey(var12)) {
            float[] var11 = (float[])var7.get(var12);
            float[] var13 = (float[])var8.get(var12);
            if (var11 == null && var13 != null && coveredArea(var13[0], var13[1], (Float)var5.get(var10), var9) == 0.0F) {
               var11 = new float[]{var13[0] / var6, var13[1] / var6};
               this.panelHomesDirty = true;
            }

            if (var11 != null) {
               this.panelHomes.put(var12, var11);
            }
         }

         float[] var15;
         if ((var15 = this.panelHomes.get(var12)) != null) {
            var9.add(new float[]{var15[0] * var6, var15[1] * var6, EfnWidgets.s(212.0F), (Float)var5.get(var10)});
         }
      }

      boolean var14 = var9.isEmpty();

      for (int var16 = 0; var16 < var3.size(); var16++) {
         if (!this.panelHomes.containsKey(var3.get(var16))) {
            float var17 = (Float)var5.get(var16);
            float[] var18 = bundledSpot((String)var4.get(var16));
            if (var18 == null && var14) {
               var18 = panelPosFor(var16, var3.size());
            }

            if (var18 == null || coveredArea(var18[0], var18[1], var17, var9) > 0.0F) {
               var18 = freeSpot(var17, var9);
            }

            this.panelHomes.put((String)var3.get(var16), new float[]{var18[0] / var6, var18[1] / var6});
            var9.add(new float[]{var18[0], var18[1], EfnWidgets.s(212.0F), var17});
            this.panelHomesDirty = true;
         }
      }

      this.panelLayoutKey = "";
   }

   private void fitPanels(ClickGuiRenderer.CategoryPanel[] var1) {
      float var4 = ImGui.getIO().getDisplaySizeX();
      float var2;
      float var3;
      String var5 = var4 + "x" + (var3 = ImGui.getIO().getDisplaySizeY()) + "@" + (var2 = EfnWidgets.s(1.0F));
      if (!var5.equals(this.panelLayoutKey)) {
         this.panelLayoutKey = var5;
         ClickGuiRenderer.PanelLayout var6 = this.panelLayout(var1);
         float var7 = EfnWidgets.s(212.0F);
         float var8 = var3 - EfnWidgets.s(70.0F) - EfnWidgets.s(42.0F);
         ArrayList var9 = new ArrayList();
         ArrayList var10 = new ArrayList();

         for (int var11 = 0; var11 < var6.ids().size(); var11++) {
            String var12 = var6.ids().get(var11);
            float[] var13 = this.panelHomes.get(var12);
            float var14 = var13[0] * var2;
            float var15 = var13[1] * var2;
            if (var14 >= 0.0F && var15 >= 0.0F && var14 + var7 <= var4 && var15 <= var8) {
               this.panelTargets.put(var12, new float[]{var14, var15});
               var9.add(new float[]{var14, var15, var7, var6.heights().get(var11)});
            } else {
               var10.add(var11);
            }
         }

         for (int var17 : var10) {
            float var18 = var6.heights().get(var17);
            float[] var19 = freeSpot(var18, var9);
            this.panelTargets.put(var6.ids().get(var17), var19);
            var9.add(new float[]{var19[0], var19[1], var7, var18});
         }
      }
   }

   private static float[] bundledSpot(String var0) {
      float var2 = ImGui.getIO().getDisplaySizeX();
      float var1;
      float[] var3 = BundledDefaults.panelSpot(var0, var2, var1 = ImGui.getIO().getDisplaySizeY());
      if (var3 == null) {
         return null;
      } else {
         var3[0] = Math.max(0.0F, Math.min(var3[0], var2 - EfnWidgets.s(212.0F)));
         var3[1] = Math.max(0.0F, Math.min(var3[1], var1 - EfnWidgets.s(42.0F)));
         return var3;
      }
   }

   private static float[] panelPosFor(int var0, int var1) {
      float var2 = ImGui.getIO().getDisplaySizeX();
      float var3 = ImGui.getIO().getDisplaySizeY();
      float var4 = EfnWidgets.s(212.0F);
      float var5 = EfnWidgets.s(14.0F);
      int var6 = Math.max(1, (int)((var2 - EfnWidgets.s(40.0F) + var5) / (var4 + var5)));
      int var7 = Math.min(var6, var1);
      float var8 = var7 * (var4 + var5) - var5;
      float var9 = Math.max(EfnWidgets.s(20.0F), (var2 - var8) * 0.5F);
      return new float[]{var9 + var0 % var6 * (var4 + var5), EfnWidgets.s(28.0F) + var0 / var6 * var3 * 0.45F};
   }

   private static float[] freeSpot(float var0, List<float[]> var1) {
      float var3 = ImGui.getIO().getDisplaySizeX();
      float var4 = ImGui.getIO().getDisplaySizeY();
      float var5 = EfnWidgets.s(212.0F);
      float var6 = EfnWidgets.s(14.0F);
      float var7 = EfnWidgets.s(20.0F);
      float var8 = var4 - EfnWidgets.s(70.0F) - var6;
      float var9 = 3.4028235E38F;

      for (float[] var11 : var1) {
         var9 = Math.min(var9, var11[1]);
      }

      float[] var20 = null;

      for (float[] var12 : var1) {
         boolean var13 = var12[1] < var9 + EfnWidgets.s(42.0F);
         if (var13 && (var20 == null || var12[0] + var12[2] > var20[0] + var20[2])) {
            var20 = var12;
         }
      }

      float[] var2;
      if (var20 == null) {
         float[] var22 = new float[]{var7, 0.0F};
         var2 = var22;
         var22[1] = EfnWidgets.s(28.0F);
      } else {
         float[] var23 = new float[]{var20[0] + var20[2] + var6, 0.0F};
         var2 = var23;
         var23[1] = var20[1];
      }

      ArrayList var24 = new ArrayList();
      var24.add(new float[]{var7, EfnWidgets.s(28.0F)});

      for (float[] var14 : var1) {
         var24.add(new float[]{var14[0] + var14[2] + var6, var14[1]});
         var24.add(new float[]{var14[0], var14[1] + var14[3] + var6});
      }

      var24.sort(Comparator.<float[]>comparingDouble(var0x -> var0x[1]).thenComparingDouble(var0x -> var0x[0]));
      var24.addFirst(var2);

      for (float[] var28 : var24) {
         boolean var15 = var28[0] >= 0.0F && var28[1] >= 0.0F && var28[0] + var5 <= var3 - var7 && var28[1] + var0 <= var8;
         if (var15 && coveredArea(var28[0], var28[1], var0, var1) == 0.0F) {
            return var28;
         }
      }

      float[] var27 = null;
      float var29 = 3.4028235E38F;

      for (float[] var16 : var24) {
         float var18 = Math.max(0.0F, Math.min(var16[0], var3 - var5 - var7));
         float var17;
         float var19 = coveredArea(var18, var17 = Math.max(0.0F, Math.min(var16[1], var4 - var0 - var7)), var0, var1);
         if (var19 < var29) {
            var27 = new float[]{var18, var17};
            var29 = var19;
         }
      }

      return var27;
   }

   private static float coveredArea(float var0, float var1, float var2, List<float[]> var3) {
      float var4 = EfnWidgets.s(212.0F);
      float var5 = EfnWidgets.s(14.0F) * 0.5F;
      float var6 = 0.0F;

      for (float[] var8 : var3) {
         float var9 = Math.min(var0 + var4, var8[0] + var8[2]) + var5 - Math.max(var0, var8[0]);
         float var10 = Math.min(var1 + var2, var8[1] + var8[3]) + var5 - Math.max(var1, var8[1]);
         if (var9 > 0.0F && var10 > 0.0F) {
            var6 += var9 * var10;
         }
      }

      return var6;
   }

   private static Map<String, float[]> savedPanelSpots() {
      HashMap var0 = new HashMap();
      String var1 = ImGui.getIO().getIniFilename();
      if (var1 != null && Files.isRegularFile(Path.of(var1))) {
         String var2 = "[Window][";

         try {
            String var3 = null;

            for (String var5 : Files.readAllLines(Path.of(var1))) {
               if (var5.startsWith("[")) {
                  boolean var6 = var5.startsWith(var2 + "##panel_") && var5.endsWith("]");
                  var3 = var6 ? var5.substring(var2.length(), var5.length() - 1) : null;
               } else if (var3 != null && var5.startsWith("Pos=")) {
                  String[] var8 = var5.substring(4).split(",");
                  var0.put(var3, new float[]{Float.parseFloat(var8[0]), Float.parseFloat(var8[1])});
               }
            }
         } catch (RuntimeException | IOException var7) {
            var0.clear();
         }

         return var0;
      } else {
         return var0;
      }
   }

   private List<ClientModule> modulesFor(ModuleCategory var1) {
      return ModuleRegistry.inCategory(var1);
   }

   private List<ClientModule> searchModules(String var1) {
      int var2 = ModuleRegistry.revision();
      if (var2 != this.cachedRegistryRevision) {
         this.cachedRegistryRevision = var2;
         this.cachedGlobalSearch = "";
         this.cachedGlobalResults = List.of();
      }

      if (var1.equals(this.cachedGlobalSearch)) {
         return this.cachedGlobalResults;
      } else {
         ArrayList var3 = new ArrayList();

         for (ClientModule var5 : ModuleRegistry.all()) {
            if (var5.name().toLowerCase(Locale.ROOT).contains(var1)) {
               var3.add(var5);
            }
         }

         this.cachedGlobalSearch = var1;
         this.cachedGlobalResults = List.copyOf(var3);
         return this.cachedGlobalResults;
      }
   }

   private static String normalizeSearch(String var0) {
      return var0 == null ? "" : var0.toLowerCase(Locale.ROOT).trim();
   }

   private void renderTabBar(float var1, float var2) {
      ClickGuiRenderer.Tab[] var3 = ClickGuiRenderer.Tab.values();
      ImFont var4 = ImGui.getFont();
      float var5 = EfnWidgets.s(44.0F);
      float var6 = EfnWidgets.s(4.0F);
      float var7 = EfnWidgets.s(20.0F);
      float[] var8 = new float[var3.length];
      float var9 = var6 * 2.0F;

      for (int var10 = 0; var10 < var3.length; var10++) {
         var8[var10] = EfnWidgets.textWidth(var4, var3[var10].label) + var7 * 2.0F;
         var9 += var8[var10];
      }

      float var25 = ImGui.getIO().getDisplaySizeX();
      float var11 = ImGui.getIO().getDisplaySizeY();
      float var12 = (var25 - var9) * 0.5F;
      float var13 = var11 - var5 - EfnWidgets.s(26.0F) + EfnWidgets.s(16.0F) * (1.0F - var2);
      ImGui.setNextWindowPos(var12, var13, 1);
      ImGui.setNextWindowSize(var9, var5, 1);
      ImGui.pushStyleVar(0, Math.max(0.01F, var2));
      ImGui.pushStyleVar(2, 0.0F, 0.0F);
      ImGui.pushStyleVar(3, var5 * 0.5F);
      ImGui.pushStyleColor(2, GuiPalette.tabBarBg());
      if (ImGui.begin("##tabbar", 319)) {
         this.tabBarRect = new float[]{var12, var13, var9, var5};
         if (this.tab == ClickGuiRenderer.Tab.THEME) {
            this.outlineSurface(ThemeStore.get().tabBar, var12, var13, var9, var5, var5 * 0.5F);
         }

         drawShadow(var12, var13, var9, var5, var5 * 0.5F, 4);
         ImDrawList var14 = ImGui.getWindowDrawList();
         SurfaceBackground.draw(var14, ThemeStore.get().tabBar, var12, var13, var9, var5, var5 * 0.5F);
         var14.addRect(var12, var13, var12 + var9, var13 + var5, EfnWidgets.c(318767103), var5 * 0.5F, 0, 1.0F);
         float var15 = var6;

         for (int var16 = 0; var16 < this.tab.ordinal(); var16++) {
            var15 += var8[var16];
         }

         float var26 = var8[this.tab.ordinal()];
         if (this.tabIndicatorX < 0.0F) {
            this.tabIndicatorX = var15;
            this.tabIndicatorW = var26;
         }

         this.tabIndicatorX = smooth(this.tabIndicatorX, var15, var1, 18.0F);
         this.tabIndicatorW = smooth(this.tabIndicatorW, var26, var1, 18.0F);
         float var17 = var5 - var6 * 2.0F;
         var14.addRectFilled(
            var12 + this.tabIndicatorX,
            var13 + var6,
            var12 + this.tabIndicatorX + this.tabIndicatorW,
            var13 + var6 + var17,
            EfnWidgets.c(GuiPalette.accent()),
            var17 * 0.5F
         );
         float var18 = var6;

         for (int var19 = 0; var19 < var3.length; var19++) {
            ImGui.setCursorPos(var18, var6);
            if (ImGui.invisibleButton("##tab_" + var3[var19].name(), var8[var19], var17)) {
               this.switchTab(var3[var19]);
            }

            float var20 = EfnWidgets.animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
            float var21 = var12 + var18 + var8[var19] * 0.5F;
            float var22 = Math.max(0.0F, 1.0F - Math.abs(var21 - (var12 + this.tabIndicatorX + this.tabIndicatorW * 0.5F)) / Math.max(1.0F, var8[var19] * 0.5F));
            int var23 = EfnTheme.lerpColor(EfnTheme.lerpColor(-4542808, -461582, var20), -1, var22);
            float var24 = EfnWidgets.textWidth(var4, var3[var19].label);
            EfnWidgets.text(
               var14,
               var4,
               var12 + var18 + (var8[var19] - var24) * 0.5F,
               var13 + (var5 - EfnWidgets.lineHeight(var4)) * 0.5F,
               var23,
               var3[var19].label,
               this.tabBarGuard
            );
            var18 += var8[var19];
         }
      }

      ImGui.end();
      ImGui.popStyleColor();
      ImGui.popStyleVar(3);
   }

   private float[] beginCard(String var1, float var2, float var3) {
      return this.beginCard(var1, var2, var3, ImGui.getIO().getDisplaySizeX() * 0.5F);
   }

   private float[] beginCard(String var1, float var2, float var3, float var4) {
      float var5 = ImGui.getIO().getDisplaySizeX();
      float var6 = ImGui.getIO().getDisplaySizeY();
      float var7 = EfnWidgets.s(86.0F);
      float var8 = Math.min(var5 - EfnWidgets.s(32.0F), EfnWidgets.s(460.0F));
      float var9 = Math.min(var6 - var7 - EfnWidgets.s(32.0F), EfnWidgets.s(760.0F));
      float var10 = Math.max(EfnWidgets.s(16.0F), var4 - var8 * 0.5F);
      float var11 = Math.max(EfnWidgets.s(16.0F), (var6 - var7 - var9) * 0.5F + EfnWidgets.s(8.0F)) + EfnWidgets.s(24.0F) * (1.0F - var3);
      ImGui.setNextWindowPos(var10, var11, 1);
      ImGui.setNextWindowSize(var8, var9, 1);
      ImGui.pushStyleVar(0, var2);
      ImGui.pushStyleVar(2, 0.0F, 0.0F);
      ImGui.pushStyleVar(14, 0.0F, 0.0F);
      ImGui.pushStyleVar(3, cardRounding());
      ImGui.pushStyleColor(2, GuiPalette.cardBg());
      if (this.cardTextLift) {
         ImGui.pushStyleColor(0, -1);
         ImGui.pushStyleColor(1, -2042416);
      }

      ImGui.begin(var1, 8511);
      drawShadow(var10, var11, var8, var9, cardRounding(), 6);
      ImDrawList var12 = ImGui.getWindowDrawList();
      Surface var13 = ThemeStore.get().cards;
      SurfaceBackground.draw(var12, var13, var10, var11, var8, var9, cardRounding());
      var12.addRect(var10, var11, var10 + var8, var11 + var9, EfnWidgets.c(318767103), cardRounding(), 0, 1.0F);
      float var14 = SurfaceBackground.hasImageBanner(var13)
         ? Math.max(EfnWidgets.s(22.0F), SurfaceBackground.bannerHeight(var13) - EfnWidgets.s(44.0F))
         : EfnWidgets.s(22.0F);
      return new float[]{var8, var9, var14, var10, var11};
   }

   private static float panelRounding() {
      return EfnWidgets.s(GuiPalette.rounding());
   }

   public static float cardRounding() {
      return EfnWidgets.s(Math.min(28.0F, GuiPalette.rounding() * 2.2F));
   }

   private void endCard() {
      ImGui.end();
      ImGui.popStyleColor(this.cardTextLift ? 3 : 1);
      ImGui.popStyleVar(4);
   }

   public static void drawCardTitle(String var0, float var1, float var2) {
      drawCardTitle(var0, var1, var2, false);
   }

   private static void drawCardTitle(String var0, float var1, float var2, boolean var3) {
      ImFont var4 = ImGuiManager.getInstance().titleFont();
      EfnWidgets.text(
         ImGui.getWindowDrawList(), var4, ImGui.getWindowPosX() + var1, ImGui.getWindowPosY() + var2 - EfnWidgets.lineHeight(var4) * 0.5F, -461582, var0, var3
      );
   }

   public static float mutedParagraph(String var0, float var1, float var2, float var3) {
      ImGui.setCursorPos(var1, var2);
      ImGui.pushTextWrapPos(var3);
      ImGui.pushStyleColor(0, ImGui.getStyleColorVec4(1));
      ImGui.textUnformatted(var0);
      ImGui.popStyleColor();
      ImGui.popTextWrapPos();
      return ImGui.getCursorPosY();
   }

   public static void cardDivider(float var0, float var1, float var2) {
      float var3 = ImGui.getWindowPosX();
      float var4 = ImGui.getWindowPosY();
      ImGui.getWindowDrawList().addLine(var3 + var0, var4 + var2, var3 + var1 - var0, var4 + var2, EfnWidgets.c(268435455), 1.0F);
   }

   private void renderModuleCard(ClientModule var1, float var2, float var3, float var4) {
      this.renderModuleCard(var1, var2, var3, var4, false);
   }

   private void renderModuleCard(ClientModule var1, float var2, float var3, float var4, boolean var5) {
      this.renderModuleCard(var1, var2, var3, var4, var5, !var5);
   }

   private void renderModuleCard(ClientModule var1, float var2, float var3, float var4, boolean var5, boolean var6) {
      float var8 = ImGui.getIO().getDisplaySizeX();
      float var9 = ImGui.getIO().getDisplaySizeY();
      float[] var7;
      if (var5) {
         var7 = this.beginCard("##preview_card", var3, var4, (var8 - EfnWidgets.s(408.0F)) * 0.5F);
         this.cardRect = new float[]{var7[3], var7[4], var7[0], var7[1]};
         this.outlineSurface(ThemeStore.get().cards, var7[3], var7[4], var7[0], var7[1], cardRounding());
         ImGui.pushStyleVar(1, 1.0F);
         ImGui.beginDisabled();
      } else {
         ImGui.getBackgroundDrawList().addRectFilled(0.0F, 0.0F, var8, var9, EfnWidgets.withAlpha(1426063360, var4));
         var7 = this.beginCard("##module_card", var3, var4);
      }

      float var10 = var7[0];
      float var11 = var7[1];
      float var12 = EfnWidgets.s(24.0F);
      float var13 = var7[2];
      float var14 = EfnWidgets.s(32.0F);
      ImDrawList var15 = ImGui.getWindowDrawList();
      float var16 = ImGui.getWindowPosX();
      float var17 = ImGui.getWindowPosY();
      float var18 = var12;
      if (!var5 && var6) {
         ImGui.setCursorPos(var12 - EfnWidgets.s(4.0F), var13);
         if (EfnWidgets.iconButton("##back", var14)) {
            this.closeSettings();
         }

         EfnWidgets.chevronLeft(
            var15, var16 + var12 - EfnWidgets.s(4.0F) + var14 * 0.5F - EfnWidgets.s(1.0F), var17 + var13 + var14 * 0.5F, EfnWidgets.s(5.0F), -461582
         );
         var18 = var12 + var14 + EfnWidgets.s(8.0F);
      }

      drawCardTitle(var1.name(), var18, var13 + var14 * 0.5F, this.cardGuard);
      if (var1.isAction()) {
         float var19 = EfnWidgets.s(72.0F);
         float var20 = EfnWidgets.s(30.0F);
         ImGui.setCursorPos(var10 - var12 - var19, var13 + (var14 - var20) * 0.5F);
         if (EfnWidgets.chipButton("##open", "Open", var19, var20, true)) {
            var1.onClick();
         }
      } else {
         float var26 = EfnWidgets.s(46.0F);
         float var29 = EfnWidgets.s(26.0F);
         ImGui.setCursorPos(var10 - var12 - var26, var13 + (var14 - var29) * 0.5F);
         float var21 = ImGui.getCursorScreenPosX();
         float var22 = ImGui.getCursorScreenPosY();
         if (ImGui.invisibleButton("##enabled", var26, var29)) {
            var1.toggle();
         }

         EfnWidgets.drawSwitch(var15, var21, var22, var26, var29, EfnWidgets.animate(ImGui.getItemID(), var1.isEnabled(), 14.0F));
      }

      float var27 = var13 + var14 + EfnWidgets.s(12.0F);
      String var30 = var1.description();
      if (var30 != null && !var30.isBlank()) {
         var27 = mutedParagraph(var30, var12, var27, var10 - var12) + EfnWidgets.s(4.0F);
      }

      float var28;
      cardDivider(var12, var10, var28 = var27 + EfnWidgets.s(12.0F));
      ImGui.setCursorPos(0.0F, var28 + 1.0F);
      if (ImGui.beginChild("##body", var10, Math.max(1.0F, var11 - var28 - 1.0F - EfnWidgets.s(10.0F)), false, 24)) {
         float var31 = var10 - var12 * 2.0F;
         float var32 = ImGui.getCursorPosY() + EfnWidgets.s(16.0F);
         this.sectionLabel("Settings", var12);
         if (!var5 && var1.hasModifiedSettings()) {
            this.renderResetAllChip(var1, var12, var31, var32);
         }

         boolean var23 = false;

         for (Setting var25 : var1.settings()) {
            if (var25.isVisible() && (!var1.isAction() || var25 != var1.showInHud())) {
               var23 = true;
               ImGui.pushID(var25.id());
               this.renderSetting(var1, var25, var12, var31);
               ImGui.popID();
            }
         }

         if (!var23) {
            mutedParagraph("This module has no settings.", var12, ImGui.getCursorPosY() + EfnWidgets.s(8.0F), var10 - var12);
         }

         this.sectionLabel("Keybind", var12);
         ImGui.pushID("##keybind_row");
         this.renderKeybindRow(var1, var12, var31);
         ImGui.popID();
         if (isHudModule(var1)) {
            this.renderHudActions(var12, var31);
         }

         if (var1.inConfigs()) {
            this.sectionLabel("Config", var12);
            this.renderModuleConfigActions(var1, var12, var31);
         }

         ImGui.dummy(1.0F, EfnWidgets.s(18.0F));
         this.applySmoothScroll("card_" + var1.id(), var2);
      }

      ImGui.endChild();
      if (var5) {
         ImGui.endDisabled();
         ImGui.popStyleVar();
      }

      this.endCard();
   }

   private void renderResetAllChip(ClientModule var1, float var2, float var3, float var4) {
      float var5 = ImGui.getCursorPosY();
      String var6 = "reset:" + var1.id();
      String var7 = this.isConfirming(var6) ? "Confirm reset" : "Reset all";
      ImFont var8 = ImGuiManager.getInstance().smallFont();
      boolean var9 = pushFont(var8);
      float var10 = EfnWidgets.textWidth(var8, var7) + EfnWidgets.s(18.0F);
      float var11 = EfnWidgets.lineHeight(var8) + EfnWidgets.s(8.0F);
      ImGui.setCursorPos(var2 + var3 - var10, var4 + (ImGui.getTextLineHeight() - var11) * 0.5F);
      if (EfnWidgets.chipButton("##reset_all", var7, var10, var11, this.isConfirming(var6)) && this.confirmClick(var6)) {
         var1.resetSettings();
      }

      if (ImGui.isItemHovered(65536)) {
         showTooltip("Restore every setting to its default");
      }

      if (var9) {
         ImGui.popFont();
      }

      ImGui.setCursorPosY(var5);
   }

   private boolean confirmClick(String var1) {
      if (this.isConfirming(var1)) {
         this.pendingConfirm = null;
         return true;
      } else {
         this.pendingConfirm = var1;
         this.pendingConfirmAt = System.nanoTime();
         return false;
      }
   }

   private boolean isConfirming(String var1) {
      return var1.equals(this.pendingConfirm) && System.nanoTime() - this.pendingConfirmAt < 3000000000L;
   }

   private void sectionLabel(String var1, float var2) {
      ImGui.setCursorPos(var2, ImGui.getCursorPosY() + EfnWidgets.s(16.0F));
      boolean var3 = pushFont(ImGuiManager.getInstance().smallFont());
      ImGui.pushStyleColor(0, ImGui.getStyleColorVec4(1));
      ImGui.textUnformatted(var1);
      ImGui.popStyleColor();
      if (var3) {
         ImGui.popFont();
      }

      ImGui.setCursorPosY(ImGui.getCursorPosY() + EfnWidgets.s(4.0F));
      this.dividerPending = false;
   }

   private float rowStart(float var1, float var2) {
      float var3 = ImGui.getCursorPosY();
      if (this.dividerPending) {
         float var4 = ImGui.getWindowPosX();
         float var5 = ImGui.getCursorScreenPosY();
         ImGui.getWindowDrawList().addLine(var4 + var1, var5, var4 + var1 + var2, var5, EfnWidgets.c(268435455), 1.0F);
      }

      this.dividerPending = true;
      return var3;
   }

   public static void rowLabel(String var0, float var1, float var2, float var3) {
      ImGui.setCursorPos(var1, var2 + (var3 - ImGui.getTextLineHeight()) * 0.5F);
      ImGui.textUnformatted(var0);
   }

   private void openSettings(ClientModule var1) {
      this.settingsModule = var1;
      this.viewChangedAt = System.nanoTime();
      this.cardJustOpened = true;
   }

   private void closeSettings() {
      if (this.settingsModule != null) {
         this.viewChangedAt = System.nanoTime();
      }

      this.settingsModule = null;
      this.listeningModule = null;
      this.listeningKeySetting = null;
      this.activeColorModule = null;
      this.activeColorSetting = null;
      this.colorPickerOpen.set(false);
      this.blockLists.close();
      this.itemPickers.close();
      this.itemColorWindowOpen = false;
   }

   private void closeCardOnOutsideClick() {
      if (this.cardJustOpened) {
         this.cardJustOpened = false;
      } else if (this.settingsModule != null && this.listeningModule == null && this.listeningKeySetting == null) {
         if (ImGui.isMouseClicked(0) && !ImGui.isWindowHovered(4) && !ImGui.isAnyItemActive()) {
            this.closeSettings();
         }
      }
   }

   private void renderFriendsCard(float var1, float var2, float var3) {
      float[] var4 = this.beginCard("##friends_card", var2, var3);
      float var5 = var4[0];
      float var6 = var4[1];
      float var7 = EfnWidgets.s(24.0F);
      float var8 = var4[2];
      float var9 = EfnWidgets.s(32.0F);
      FriendStore var10 = FriendStore.getInstance();
      List var11 = var10.all();
      drawCardTitle("Friends", var7, var8 + var9 * 0.5F, this.cardGuard);
      String var12 = var11.size() == 1 ? "1 player" : var11.size() + " players";
      ImFont var13 = ImGuiManager.getInstance().smallFont();
      EfnWidgets.text(
         ImGui.getWindowDrawList(),
         var13,
         ImGui.getWindowPosX() + var5 - var7 - EfnWidgets.textWidth(var13, var12),
         ImGui.getWindowPosY() + var8 + (var9 - EfnWidgets.lineHeight(var13)) * 0.5F,
         -4542808,
         var12,
         this.cardGuard
      );
      float var14 = mutedParagraph("Add players by their exact username.", var7, var8 + var9 + EfnWidgets.s(12.0F), var5 - var7) + EfnWidgets.s(14.0F);
      float var15 = EfnWidgets.s(84.0F);
      float var16 = ImGui.getTextLineHeight() + EfnWidgets.s(16.0F);
      ImGui.setCursorPos(var7, var14);
      ImGui.pushStyleVar(11, EfnWidgets.s(12.0F), EfnWidgets.s(8.0F));
      ImGui.setNextItemWidth(var5 - var7 * 2.0F - var15 - EfnWidgets.s(8.0F));
      if (this.focusFriendInput) {
         ImGui.setKeyboardFocusHere();
         this.focusFriendInput = false;
      }

      ImGui.inputTextWithHint("##friend_input", "Username", this.friendInput, 16);
      boolean var17 = EfnWidgets.submittedWithEnter();
      ImGui.popStyleVar();
      ImGui.setCursorPos(var5 - var7 - var15, var14);
      if (EfnWidgets.chipButton("##friend_add", "Add", var15, var16, true)) {
         var17 = true;
      }

      if (var17) {
         this.tryAddFriend(var10);
      }

      var14 += var16 + EfnWidgets.s(8.0F);
      if (this.friendMessage != null && elapsed(this.friendMessageAt, 2500.0F) < 1.0F) {
         ImGui.setCursorPos(var7, var14);
         boolean var18 = pushFont(var13);
         ImGui.pushStyleColor(0, -10856974);
         ImGui.textUnformatted(this.friendMessage);
         ImGui.popStyleColor();
         if (var18) {
            ImGui.popFont();
         }
      }

      float var21;
      cardDivider(var7, var5, var21 = var14 + (EfnWidgets.lineHeight(var13) + EfnWidgets.s(10.0F)));
      ImGui.setCursorPos(0.0F, var21 + 1.0F);
      if (ImGui.beginChild("##friend_list", var5, Math.max(1.0F, var6 - var21 - 1.0F - EfnWidgets.s(10.0F)), false, 24)) {
         if (var11.isEmpty()) {
            String var23 = "No friends yet";
            ImGui.setCursorPos((var5 - ImGui.calcTextSize(var23).x) * 0.5F, EfnWidgets.s(40.0F));
            ImGui.textDisabled(var23);
         } else {
            ImGui.dummy(1.0F, EfnWidgets.s(8.0F));

            for (String var19 : var11) {
               this.renderFriendRow(var10, var19, var7, var5 - var7 * 2.0F);
            }
         }

         ImGui.dummy(1.0F, EfnWidgets.s(12.0F));
         this.applySmoothScroll("friends", var1);
      }

      ImGui.endChild();
      this.endCard();
   }

   private void renderFriendRow(FriendStore var1, String var2, float var3, float var4) {
      ImGui.pushID("friend_" + var2);
      float var5 = EfnWidgets.s(48.0F);
      float var6 = ImGui.getCursorPosY();
      ImGui.setCursorPos(var3, var6);
      float var7 = ImGui.getCursorScreenPosX();
      float var8 = ImGui.getCursorScreenPosY();
      ImGui.setNextItemAllowOverlap();
      ImGui.invisibleButton("##row", var4, var5);
      boolean var9 = ImGui.isMouseHoveringRect(var7, var8, var7 + var4, var8 + var5);
      float var10 = EfnWidgets.animate(ImGui.getItemID(), var9, 18.0F);
      ImDrawList var11 = ImGui.getWindowDrawList();
      if (var10 > 0.01F) {
         var11.addRectFilled(
            var7 - EfnWidgets.s(8.0F),
            var8,
            var7 + var4 + EfnWidgets.s(8.0F),
            var8 + var5,
            EfnWidgets.c(EfnWidgets.withAlpha(218103807, var10)),
            EfnWidgets.s(10.0F)
         );
      }

      float var12 = EfnWidgets.s(15.0F);
      float var13 = var7 + var12;
      float var14 = var8 + var5 * 0.5F;
      int var15 = AVATAR_COLORS[Math.floorMod(var2.toLowerCase(Locale.ROOT).hashCode(), AVATAR_COLORS.length)];
      var11.addCircleFilled(var13, var14, var12, EfnWidgets.c(var15), 32);
      ImFont var16 = ImGuiManager.getInstance().categoryFont();
      String var17 = var2.substring(0, 1).toUpperCase(Locale.ROOT);
      EfnWidgets.text(var11, var16, var13 - EfnWidgets.textWidth(var16, var17) * 0.5F, var14 - EfnWidgets.lineHeight(var16) * 0.5F, -1, var17);
      ImFont var18 = ImGui.getFont();
      EfnWidgets.text(var11, var18, var7 + var12 * 2.0F + EfnWidgets.s(12.0F), var8 + (var5 - EfnWidgets.lineHeight(var18)) * 0.5F, -461582, var2);
      float var19 = EfnWidgets.s(28.0F);
      ImGui.setCursorPos(var3 + var4 - var19, var6 + (var5 - var19) * 0.5F);
      float var20 = ImGui.getCursorScreenPosX();
      float var21 = ImGui.getCursorScreenPosY();
      if (ImGui.invisibleButton("##remove", var19, var19)) {
         var1.remove(var2);
      }

      float var22 = EfnWidgets.animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
      float var23 = var20 + var19 * 0.5F;
      float var24 = var21 + var19 * 0.5F;
      if (var22 > 0.01F) {
         var11.addCircleFilled(var23, var24, var19 * 0.5F, EfnWidgets.c(EfnWidgets.withAlpha(-10856974, 0.18F * var22)), 32);
      }

      EfnWidgets.cross(var11, var23, var24, EfnWidgets.s(4.5F), EfnTheme.lerpColor(EfnWidgets.withAlpha(-4542808, 0.25F + 0.75F * var10), -10856974, var22));
      ImGui.setCursorPos(0.0F, var6 + var5 + EfnWidgets.s(4.0F));
      ImGui.popID();
   }

   private void tryAddFriend(FriendStore var1) {
      String var2 = this.friendInput.get().trim();
      if (!FriendStore.isValidName(var2)) {
         this.friendMessage = "Use 1-16 letters, digits or _ (Bedrock names start with .)";
      } else if (var1.isFriend(var2)) {
         this.friendMessage = var2 + " is already on your list";
      } else {
         var1.add(var2);
         this.friendInput.set("");
         this.friendMessage = null;
      }

      this.friendMessageAt = System.nanoTime();
      this.focusFriendInput = true;
   }

   private void renderConfigCard(float var1, float var2, float var3) {
      float[] var4 = this.beginCard("##config_card", var2, var3);
      float var5 = var4[0];
      float var6 = var4[1];
      float var7 = EfnWidgets.s(24.0F);
      float var8 = var4[2];
      float var9 = EfnWidgets.s(32.0F);
      float var10 = var5 - var7 * 2.0F;
      ModuleConfigStore var11 = ModuleConfigStore.getInstance();
      ConfigProfiles var12 = ConfigProfiles.getInstance();
      ImFont var13 = ImGuiManager.getInstance().smallFont();
      drawCardTitle("Config", var7, var8 + var9 * 0.5F, this.cardGuard);
      String var14 = var11.isReadOnly() ? "Read-only" : (var11.hasUnsavedChanges() ? "Saving..." : "Saved");
      int var15 = var11.isReadOnly() ? -10856974 : -4542808;
      EfnWidgets.text(
         ImGui.getWindowDrawList(),
         var13,
         ImGui.getWindowPosX() + var5 - var7 - EfnWidgets.textWidth(var13, var14),
         ImGui.getWindowPosY() + var8 + (var9 - EfnWidgets.lineHeight(var13)) * 0.5F,
         var15,
         var14,
         this.cardGuard
      );
      if (var11.isReadOnly()
         && ImGui.isMouseHoveringRect(
            ImGui.getWindowPosX() + var5 - var7 - EfnWidgets.s(80.0F),
            ImGui.getWindowPosY() + var8,
            ImGui.getWindowPosX() + var5 - var7,
            ImGui.getWindowPosY() + var8 + var9
         )) {
         showTooltip("modules.json could not be read. Fix or delete it.");
      }

      float var16 = mutedParagraph("Changes save automatically. Profiles can be shared with a code.", var7, var8 + var9 + EfnWidgets.s(12.0F), var5 - var7)
         + EfnWidgets.s(14.0F);
      float var17 = EfnWidgets.s(8.0F);
      float var18 = ImGui.getTextLineHeight() + EfnWidgets.s(16.0F);
      float var19 = (var10 - var17 * 2.0F) / 3.0F;
      ImGui.setCursorPos(var7, var16);
      if (EfnWidgets.chipButton("##cfg_save", "Save all", var19, var18, true)) {
         this.saveAllConfigs(var11);
      }

      if (ImGui.isItemHovered(65536)) {
         showTooltip("Save everything now");
      }

      ImGui.setCursorPos(var7 + var19 + var17, var16);
      if (EfnWidgets.chipButton("##cfg_folder", "Open folder", var19, var18, false)) {
         this.openProfilesFolder(var12);
      }

      ImGui.setCursorPos(var7 + (var19 + var17) * 2.0F, var16);
      boolean var20 = this.isConfirming("reset-all-modules");
      if (EfnWidgets.chipButton("##cfg_reset", var20 ? "Confirm" : "Reset all", var19, var18, var20) && this.confirmClick("reset-all-modules")) {
         for (ClientModule var22 : ModuleRegistry.all()) {
            var22.resetSettings();
         }

         this.setConfigMessage("Every module setting is back to default", false);
      }

      if (ImGui.isItemHovered(65536)) {
         showTooltip("Reset all module settings. Keybinds stay.");
      }

      float var26;
      cardDivider(var7, var5, var26 = var16 + (var18 + EfnWidgets.s(18.0F)));
      float var31 = EfnWidgets.s(84.0F);
      ImGui.setCursorPos(var7, var16 = var26 + EfnWidgets.s(14.0F));
      ImGui.pushStyleVar(11, EfnWidgets.s(12.0F), EfnWidgets.s(8.0F));
      ImGui.setNextItemWidth(var10 - var31 - var17);
      if (this.focusProfileInput) {
         ImGui.setKeyboardFocusHere();
         this.focusProfileInput = false;
      }

      ImGui.inputTextWithHint("##profile_input", "New profile name", this.profileInput);
      boolean var32 = EfnWidgets.submittedWithEnter();
      ImGui.popStyleVar();
      ImGui.setCursorPos(var5 - var7 - var31, var16);
      if (EfnWidgets.chipButton("##profile_create", "Create", var31, var18, true)) {
         var32 = true;
      }

      if (ImGui.isItemHovered(65536)) {
         showTooltip("Save your current modules as a new profile");
      }

      if (var32) {
         this.tryCreateProfile(var12);
      }

      float var23 = (var10 - var17) * 0.5F;
      float var28;
      ImGui.setCursorPos(var7, var28 = var16 + (var18 + EfnWidgets.s(8.0F)));
      if (EfnWidgets.chipButton("##profile_import", "Import file", var23, var18, false)) {
         this.importProfileFile();
      }

      if (ImGui.isItemHovered(65536)) {
         showTooltip("Import a profile .json file. You can also drop it here.");
      }

      ImGui.setCursorPos(var7 + var23 + var17, var28);
      if (EfnWidgets.chipButton("##profile_paste", "Paste code", var23, var18, false)) {
         this.pasteProfileCode();
      }

      if (ImGui.isItemHovered(65536)) {
         showTooltip("Load a share code from your clipboard. Your current settings go to Auto backup.");
      }

      var16 = var28 + (var18 + EfnWidgets.s(8.0F));
      if (this.configMessage != null && elapsed(this.configMessageAt, 2500.0F) < 1.0F) {
         ImGui.setCursorPos(var7, var16);
         boolean var24 = pushFont(var13);
         ImGui.pushStyleColor(0, this.configMessageError ? -10856974 : -4542808);
         ImGui.textUnformatted(this.configMessage);
         ImGui.popStyleColor();
         if (var24) {
            ImGui.popFont();
         }
      }

      float var30;
      cardDivider(var7, var5, var30 = var16 + (EfnWidgets.lineHeight(var13) + EfnWidgets.s(10.0F)));
      List var33 = var12.list();
      ImGui.setCursorPos(0.0F, var30 + 1.0F);
      if (ImGui.beginChild("##profile_list", var5, Math.max(1.0F, var6 - var30 - 1.0F - EfnWidgets.s(10.0F)), false, 24)) {
         if (var33.isEmpty()) {
            String var25 = "No profiles yet";
            ImGui.setCursorPos((var5 - ImGui.calcTextSize(var25).x) * 0.5F, EfnWidgets.s(40.0F));
            ImGui.textDisabled(var25);
         } else {
            ImGui.dummy(1.0F, EfnWidgets.s(8.0F));
            this.profileList.render(var12, var33, var11.activeProfile(), var7, var10);
         }

         ImGui.dummy(1.0F, EfnWidgets.s(12.0F));
         float var34 = this.profileList.takeScrollTarget();
         if (!Float.isNaN(var34)) {
            this.scroll.computeIfAbsent("profiles", var0 -> new float[2])[1] = var34;
         }

         this.applySmoothScroll("profiles", var1);
      }

      ImGui.endChild();
      this.endCard();
   }

   private void tryCreateProfile(ConfigProfiles var1) {
      try {
         String var2 = var1.create(this.profileInput.get());
         this.profileInput.set("");
         this.profileList.open(var2);
         this.setConfigMessage("Created " + var2 + " from the current modules", false);
      } catch (ProfileException var3) {
         this.setConfigMessage(var3.getMessage(), true);
      }

      this.focusProfileInput = true;
   }

   private void importProfileFile() {
      if (FileDialogs.busy()) {
         this.setConfigMessage("A file window is already open", true);
      } else {
         FileDialogs.openProfile("Import an EFN profile", this::importProfile);
      }
   }

   private void importProfile(Path var1) {
      try {
         String var2 = ConfigProfiles.getInstance().importFile(var1);
         this.profileList.open(var2);
         this.setConfigMessage("Imported " + var2 + " - press Load to use it", false);
      } catch (ProfileException var3) {
         this.setConfigMessage(var3.getMessage(), true);
      }
   }

   private void pasteProfileCode() {
      ConfigProfiles var1 = ConfigProfiles.getInstance();

      try {
         String var2 = var1.importCode(MinecraftClient.method_1551().field_1774.method_1460());
         ConfigProfiles.Profile var3 = var1.find(var2);
         var1.backup(var3 == null ? Set.of(ConfigProfiles.Part.MODULES) : var3.parts());
         var1.load(var2);
         this.profileList.open(var2);
         this.setConfigMessage("Loaded " + var2 + " - your previous settings are in Auto backup", false);
      } catch (ProfileException var4) {
         this.setConfigMessage(var4.getMessage(), true);
      }
   }

   private void saveAllConfigs(ModuleConfigStore var1) {
      var1.save();
      ThemeStore.get().save();
      HudTheme.get().save();
      HudLayoutStore.getInstance().save();
      FriendStore.getInstance().save();
      if (var1.isReadOnly()) {
         this.setConfigMessage("modules.json is unreadable, so it was left untouched", true);
      } else {
         this.setConfigMessage("Everything saved", false);
      }
   }

   private void openProfilesFolder(ConfigProfiles var1) {
      try {
         Path var2 = var1.directory();
         Files.createDirectories(var2);
         Util.method_668().method_60932(var2);
      } catch (RuntimeException | IOException var3) {
         this.setConfigMessage("Could not open the profiles folder", true);
      }
   }

   private void setConfigMessage(String var1, boolean var2) {
      this.configMessage = var1;
      this.configMessageError = var2;
      this.configMessageAt = System.nanoTime();
   }

   private void renderThemeStudio(float var1, float var2, float var3) {
      ThemeStore var4 = ThemeStore.get();
      float var5 = easeOut(elapsed(this.previewChangedAt, 260.0F));
      this.panelRects.clear();
      this.cardRect = null;
      this.inspectMode = true;
      if (this.themeSurface == var4.cards) {
         ClientModule var6 = previewModule();
         if (var6 != null) {
            this.renderModuleCard(var6, var1, var2 * var5, Math.min(var3, var5), true);
         }
      } else {
         this.renderModulePanels(var1, var2 * var5);
      }

      this.inspectMode = false;
      this.renderInspector(var4, var1, var2);
      this.highlightSurfaces(var4);
   }

   private void renderInspector(ThemeStore var1, float var2, float var3) {
      float var4 = ImGui.getIO().getDisplaySizeX();
      float var5 = ImGui.getIO().getDisplaySizeY();
      float var6 = EfnWidgets.s(360.0F);
      float var7 = Math.min(var5 - EfnWidgets.s(86.0F) - EfnWidgets.s(40.0F), EfnWidgets.s(760.0F));
      ImGui.setNextWindowPos(var4 - var6 - EfnWidgets.s(24.0F), EfnWidgets.s(24.0F), 4);
      ImGui.setNextWindowSize(var6, var7, 1);
      if (this.focusInspector) {
         ImGui.setNextWindowFocus();
         this.focusInspector = false;
      }

      ImGui.pushStyleVar(0, var3);
      ImGui.pushStyleVar(2, 0.0F, 0.0F);
      ImGui.pushStyleVar(14, 0.0F, 0.0F);
      ImGui.pushStyleVar(3, cardRounding());
      ImGui.pushStyleColor(2, GuiPalette.abgr(var1.cards.color.argbWithAlpha(255)));
      byte var8 = 59;
      if (ImGui.begin("##theme_inspector", var8)) {
         float var9 = ImGui.getWindowPosX();
         float var10 = ImGui.getWindowPosY();
         this.inspectorRect = new float[]{var9, var10, var6, var7};
         drawShadow(var9, var10, var6, var7, cardRounding(), 6);
         ImGui.getWindowDrawList().addRect(var9, var10, var9 + var6, var10 + var7, EfnWidgets.c(318767103), cardRounding(), 0, 1.0F);
         float var11 = EfnWidgets.s(20.0F);
         float var12 = EfnWidgets.s(20.0F);
         drawCardTitle("Theme", var11, var12 + EfnWidgets.s(14.0F));
         float var13 = var12 + EfnWidgets.s(34.0F);
         if (this.themeMessage != null && System.nanoTime() - this.themeMessageAt < 3000000000L) {
            ImGui.setCursorPos(var11, var13);
            ImGui.pushTextWrapPos(var6 - var11);
            ImGui.pushStyleColor(0, this.themeMessageError ? -10856974 : -461582);
            ImGui.textUnformatted(this.themeMessage);
            ImGui.popStyleColor();
            ImGui.popTextWrapPos();
            var13 = ImGui.getCursorPosY();
         } else {
            var13 = mutedParagraph("Click a panel or card on screen to edit it.", var11, var13, var6 - var11);
         }

         var13 = this.renderSurfaceTabs(var1, var11, var13 + EfnWidgets.s(12.0F), var6 - var11 * 2.0F) + EfnWidgets.s(14.0F);
         cardDivider(var11, var6, var13);
         ImGui.setCursorPos(0.0F, var13 + 1.0F);
         if (ImGui.beginChild("##inspector_body", var6, Math.max(1.0F, var7 - var13 - 1.0F - EfnWidgets.s(10.0F)), false, 24)) {
            float var14 = var6 - var11 * 2.0F;
            if (this.themeSurface == null) {
               this.renderGlobalOptions(var1, var11, var14);
            } else {
               this.renderSurfaceOptions(this.themeSurface, var11, var14);
            }

            ImGui.dummy(1.0F, EfnWidgets.s(18.0F));
            this.applySmoothScroll("inspector", var2);
         }

         ImGui.endChild();
      }

      ImGui.end();
      ImGui.popStyleColor();
      ImGui.popStyleVar(4);
   }

   private float renderSurfaceTabs(ThemeStore var1, float var2, float var3, float var4) {
      String[] var5 = new String[]{"Global", "Panels", "Cards", "Tab bar"};
      Surface[] var6 = new Surface[]{null, var1.panels, var1.cards, var1.tabBar};
      int var7 = 0;

      for (int var8 = 1; var8 < var6.length; var8++) {
         if (var6[var8] == this.themeSurface) {
            var7 = var8;
         }
      }

      float var10 = EfnWidgets.s(32.0F);
      ImGui.setCursorPos(var2, var3);
      int var9 = EfnWidgets.segmented("##surface", var5, var7, var4, var10);
      if (var9 >= 0) {
         this.selectSurface(var6[var9]);
      }

      return var3 + var10;
   }

   private void renderGlobalOptions(ThemeStore var1, float var2, float var3) {
      this.sectionLabel("Presets", var2);
      this.renderPresetRow(var1, var2, var3);
      this.sectionLabel("Colors", var2);
      this.settingRows(var2, var3, var1.accent);
      this.sectionLabel("Layout", var2);
      this.settingRows(var2, var3, var1.rounding, var1.moduleDividers, var1.backgroundDim, var1.menuBlur);
      this.sectionLabel("Interface", var2);
      this.settingRows(var2, var3, var1.uiScale, var1.font, var1.animationSpeed, var1.animateLines, var1.customMainMenu, var1.communityPopup);
      this.sectionLabel("Share", var2);
      this.renderThemeActions(var1, var2, var3);
   }

   private void renderSurfaceOptions(Surface var1, float var2, float var3) {
      this.sectionLabel("Fill", var2);
      this.settingRows(var2, var3, var1.color, var1.opacity);
      this.sectionLabel("Background", var2);
      this.settingRows(var2, var3, var1.background);
      if (var1.isImage()) {
         this.renderImageGallery(var1, var2, var3);
      }

      this.settingRows(
         var2, var3, var1.mapping, var1.coverage, var1.headerHeight, var1.imageOpacity, var1.imageScale, var1.imageX, var1.imageY, var1.lineOpacity
      );
      if (var1.isScreenMapped()) {
         mutedParagraph(
            "Screen mapping pins the background to the screen, so each surface is a window onto it. Drag a panel to see another part.",
            var2,
            ImGui.getCursorPosY() + EfnWidgets.s(12.0F),
            var2 + var3
         );
      }
   }

   private void selectSurface(Surface var1) {
      if (var1 != this.themeSurface) {
         ThemeStore var2 = ThemeStore.get();
         if (var1 == var2.cards != (this.themeSurface == var2.cards)) {
            this.previewChangedAt = System.nanoTime();
         }

         this.themeSurface = var1;
         this.focusInspector = true;
      }
   }

   private void highlightSurfaces(ThemeStore var1) {
      float var3 = ImGui.getIO().getMousePosX();
      float var2;
      boolean var4 = contains(this.inspectorRect, var3, var2 = ImGui.getIO().getMousePosY()) || ImGui.isPopupOpen("", 3072);
      Surface var5 = null;
      if (!var4) {
         for (float[] var7 : this.panelRects) {
            if (contains(var7, var3, var2)) {
               var5 = var1.panels;
            }
         }

         if (contains(this.cardRect, var3, var2)) {
            var5 = var1.cards;
         }

         if (contains(this.tabBarRect, var3, var2)) {
            var5 = var1.tabBar;
         }
      }

      this.hoveredSurface = var5;
      if (var5 != null) {
         ImDrawList var15 = ImGui.getForegroundDrawList();
         boolean var16 = var5 == var1.tabBar;
         String var8 = var5.label + (var16 ? "  \u00b7  right-click to edit" : "  \u00b7  click to edit");
         ImFont var9 = ImGuiManager.getInstance().smallFont();
         float var10 = EfnWidgets.textWidth(var9, var8) + EfnWidgets.s(18.0F);
         float var11 = EfnWidgets.lineHeight(var9) + EfnWidgets.s(10.0F);
         float var12 = Math.min(var3 + EfnWidgets.s(14.0F), ImGui.getIO().getDisplaySizeX() - var10 - EfnWidgets.s(4.0F));
         float var13 = Math.min(var2 + EfnWidgets.s(18.0F), ImGui.getIO().getDisplaySizeY() - var11 - EfnWidgets.s(4.0F));
         var15.addRectFilled(var12, var13, var12 + var10, var13 + var11, GuiPalette.accent(), var11 * 0.5F);
         EfnWidgets.text(var15, var9, var12 + EfnWidgets.s(9.0F), var13 + EfnWidgets.s(5.0F), -1, var8);
         boolean var14 = var16 ? ImGui.isMouseClicked(1) : ImGui.isMouseClicked(0) || ImGui.isMouseClicked(1);
         if (var14) {
            this.selectSurface(var5);
         }
      }
   }

   private void outlineSurface(Surface var1, float var2, float var3, float var4, float var5, float var6) {
      int var7;
      if (var1 == this.themeSurface) {
         var7 = EfnWidgets.withAlpha(GuiPalette.accent(), 0.8F);
      } else {
         if (var1 != this.hoveredSurface) {
            return;
         }

         var7 = -2130706433;
      }

      float var8 = EfnWidgets.s(3.0F);
      float var9 = var8 + EfnWidgets.s(2.0F);
      ImDrawList var10 = ImGui.getWindowDrawList();
      var10.pushClipRect(var2 - var9, var3 - var9, var2 + var4 + var9, var3 + var5 + var9, false);
      var10.addRect(var2 - var8, var3 - var8, var2 + var4 + var8, var3 + var5 + var8, var7, var6 + var8, 0, EfnWidgets.s(1.5F));
      var10.popClipRect();
   }

   private void recordPanelRect() {
      if (this.inspectMode) {
         float var1 = ImGui.getWindowPosX();
         float var2 = ImGui.getWindowPosY();
         float var3 = ImGui.getWindowWidth();
         float var4 = ImGui.getWindowHeight();
         this.panelRects.add(new float[]{var1, var2, var3, var4});
         this.outlineSurface(ThemeStore.get().panels, var1, var2, var3, var4, panelRounding());
      }
   }

   private static boolean contains(float[] var0, float var1, float var2) {
      return var0 != null && var1 >= var0[0] && var1 <= var0[0] + var0[2] && var2 >= var0[1] && var2 <= var0[1] + var0[3];
   }

   private static ClientModule previewModule() {
      ClientModule var0 = null;

      for (ClientModule var2 : ModuleRegistry.all()) {
         if (var0 == null) {
            var0 = var2;
         }

         if (var2.settings().size() >= 3) {
            return var2;
         }
      }

      return var0;
   }

   private void settingRows(float var1, float var2, Setting... var3) {
      for (Setting var7 : var3) {
         if (var7.isVisible()) {
            ImGui.pushID(var7.id());
            this.renderSetting(null, var7, var1, var2);
            ImGui.popID();
         }
      }
   }

   private void renderPresetRow(ThemeStore var1, float var2, float var3) {
      float var4 = EfnWidgets.s(46.0F) + EfnWidgets.s(6.0F);
      float var5 = this.rowStart(var2, var3);
      float var6 = EfnWidgets.s(28.0F);
      float var7 = EfnWidgets.s(12.0F);
      ImDrawList var8 = ImGui.getWindowDrawList();

      for (int var9 = 0; var9 < ThemeStore.PRESETS.size(); var9++) {
         ThemeStore.Preset var10 = ThemeStore.PRESETS.get(var9);
         ImGui.setCursorPos(var2 + var9 * (var6 + var7), var5 + (var4 - var6) * 0.5F);
         float var11 = ImGui.getCursorScreenPosX() + var6 * 0.5F;
         float var12 = ImGui.getCursorScreenPosY() + var6 * 0.5F;
         if (ImGui.invisibleButton("##preset" + var9, var6, var6)) {
            var1.applyPreset(var10);
         }

         boolean var13 = ImGui.isItemHovered();
         float var14 = EfnWidgets.animate(ImGui.getItemID(), var13, 18.0F);
         boolean var15 = var1.matchesPreset(var10);
         var8.addCircleFilled(var11, var12, var6 * 0.5F - EfnWidgets.s(3.0F), EfnWidgets.c(GuiPalette.abgr(var10.accent())), 32);
         if (var15) {
            var8.addCircle(var11, var12, var6 * 0.5F, EfnWidgets.c(-1), 32, EfnWidgets.s(1.5F));
         } else if (var14 > 0.01F) {
            var8.addCircle(var11, var12, var6 * 0.5F, EfnWidgets.c(EfnWidgets.withAlpha(-1, 0.35F * var14)), 32, EfnWidgets.s(1.2F));
         }

         if (var13) {
            showTooltip(var10.name());
         }
      }

      ImGui.setCursorPos(0.0F, var5 + var4);
   }

   private void renderImageGallery(Surface var1, float var2, float var3) {
      List var4 = ThemeImages.get().files();
      byte var5 = 3;
      float var6 = EfnWidgets.s(8.0F);
      float var7 = EfnWidgets.s(8.0F);
      float var8 = (var3 - var6 * (var5 - 1)) / var5;
      float var9 = var8 * 0.6F;
      float var10 = ImGui.getCursorPosY() + EfnWidgets.s(4.0F);
      ImDrawList var11 = ImGui.getWindowDrawList();
      ImFont var12 = ImGuiManager.getInstance().smallFont();
      String var13 = null;
      int var14 = var4.size() + 1;

      for (int var15 = 0; var15 < var14; var15++) {
         ImGui.setCursorPos(var2 + var15 % var5 * (var8 + var6), var10 + var15 / var5 * (var9 + var6));
         float var16 = ImGui.getCursorScreenPosX();
         float var17 = ImGui.getCursorScreenPosY();
         String var18 = var15 == 0 ? null : (String)var4.get(var15 - 1);
         String var19 = var18 == null ? null : "image-delete:" + var18;
         boolean var20 = var19 != null && this.isConfirming(var19);
         float var21 = EfnWidgets.s(20.0F);
         float var22 = var16 + var8 - var21 - EfnWidgets.s(5.0F);
         float var23 = var17 + EfnWidgets.s(5.0F);
         boolean var24 = ImGui.isWindowHovered(128) && ImGui.isMouseHoveringRect(var16, var17, var16 + var8, var17 + var9);
         boolean var25 = var19 != null && (var20 || var24);
         boolean var26 = false;
         float var27 = 0.0F;
         if (var25) {
            ImGui.setCursorScreenPos(var22, var23);
            if (ImGui.invisibleButton("##tiledel" + var15, var21, var21) && this.confirmClick(var19)) {
               var13 = var18;
            }

            var26 = ImGui.isItemHovered();
            var27 = EfnWidgets.animate(ImGui.getItemID(), var26, 18.0F);
            var20 = this.isConfirming(var19);
            if (var26) {
               showTooltip(var20 ? "Click again to delete" : "Delete image");
            }

            ImGui.setCursorScreenPos(var16, var17);
         }

         boolean var28 = ImGui.invisibleButton("##tile" + var15, var8, var9);
         if (!PerformanceModule.optimizationsEnabled() || ImGui.isItemVisible()) {
            boolean var29 = ImGui.isItemHovered() || var26;
            float var30 = EfnWidgets.animate(ImGui.getItemID(), var29, 18.0F);
            if (var15 == 0) {
               var11.addRectFilled(var16, var17, var16 + var8, var17 + var9, EfnWidgets.c(EfnTheme.lerpColor(-13226454, -12240588, var30)), var7);
               int var31 = EfnTheme.lerpColor(-4542808, -461582, var30);
               float var32 = var16 + var8 * 0.5F;
               float var33 = var17 + var9 * 0.5F - EfnWidgets.s(8.0F);
               var11.addLine(var32 - EfnWidgets.s(7.0F), var33, var32 + EfnWidgets.s(7.0F), var33, EfnWidgets.c(var31), EfnWidgets.s(1.8F));
               var11.addLine(var32, var33 - EfnWidgets.s(7.0F), var32, var33 + EfnWidgets.s(7.0F), EfnWidgets.c(var31), EfnWidgets.s(1.8F));
               String var34 = "Add image";
               EfnWidgets.text(var11, var12, var32 - EfnWidgets.textWidth(var12, var34) * 0.5F, var33 + EfnWidgets.s(12.0F), var31, var34);
               if (var28) {
                  ThemeImages.get().browseAsync();
               }
            } else {
               ThemeImages.Texture var49 = ThemeImages.get().preview(var18);
               boolean var50 = var49 == null && ThemeImages.get().isBroken(var18);
               var11.addRectFilled(var16, var17, var16 + var8, var17 + var9, EfnWidgets.c(-13226454), var7);
               if (var49 != null) {
                  float[] var51 = SurfaceBackground.coverUv(var49.width(), var49.height(), var8, var9, 0.5F);
                  var11.addImageRounded(
                     var49.id(), var16, var17, var16 + var8, var17 + var9, var51[0], var51[1], var51[2], var51[3], EfnWidgets.c(-1), var7, 240
                  );
               } else if (var50 && !var20) {
                  float var52 = var16 + var8 * 0.5F;
                  float var56 = var17 + var9 * 0.5F - EfnWidgets.s(8.0F);
                  EfnWidgets.cross(var11, var52, var56, EfnWidgets.s(5.0F), -4542808);
                  String var35 = "Can't load";
                  EfnWidgets.text(var11, var12, var52 - EfnWidgets.textWidth(var12, var35) * 0.5F, var56 + EfnWidgets.s(10.0F), -4542808, var35);
               }

               if (ThemeImages.isGif(var18)) {
                  String var53 = "GIF";
                  float var57 = EfnWidgets.s(4.0F);
                  float var59 = EfnWidgets.lineHeight(var12) + EfnWidgets.s(2.0F);
                  float var36 = EfnWidgets.textWidth(var12, var53) + var57 * 2.0F;
                  float var37 = var16 + EfnWidgets.s(5.0F);
                  float var38 = var17 + var9 - var59 - EfnWidgets.s(5.0F);
                  var11.addRectFilled(var37, var38, var37 + var36, var38 + var59, EfnWidgets.c(EfnWidgets.withAlpha(-16777216, 0.6F)), EfnWidgets.s(4.0F));
                  EfnWidgets.text(var11, var12, var37 + var57, var38 + EfnWidgets.s(1.0F), -1, var53);
               }

               if (var20) {
                  String var54 = "Delete?";
                  var11.addRectFilled(var16, var17, var16 + var8, var17 + var9, EfnWidgets.c(EfnWidgets.withAlpha(-16777216, 0.6F)), var7);
                  EfnWidgets.text(
                     var11, var12, var16 + (var8 - EfnWidgets.textWidth(var12, var54)) * 0.5F, var17 + (var9 - EfnWidgets.lineHeight(var12)) * 0.5F, -1, var54
                  );
               }

               if (var18.equals(var1.image.get())) {
                  var11.addRect(
                     var16 - EfnWidgets.s(1.0F),
                     var17 - EfnWidgets.s(1.0F),
                     var16 + var8 + EfnWidgets.s(1.0F),
                     var17 + var9 + EfnWidgets.s(1.0F),
                     EfnWidgets.c(GuiPalette.accent()),
                     var7 + EfnWidgets.s(1.0F),
                     0,
                     EfnWidgets.s(2.0F)
                  );
               } else if (var30 > 0.01F) {
                  var11.addRect(var16, var17, var16 + var8, var17 + var9, EfnWidgets.c(EfnWidgets.withAlpha(-1, 0.4F * var30)), var7, 0, 1.0F);
               }

               if (var25) {
                  float var55 = var20 ? 1.0F : var30;
                  float var58 = var22 + var21 * 0.5F;
                  float var60 = var23 + var21 * 0.5F;
                  int var61 = var20 ? -10856974 : EfnTheme.lerpColor(EfnWidgets.withAlpha(-16777216, 0.55F), GuiPalette.accent(), var27);
                  var11.addCircleFilled(var58, var60, var21 * 0.5F, EfnWidgets.c(EfnWidgets.withAlpha(var61, var55)), 32);
                  EfnWidgets.cross(var11, var58, var60, EfnWidgets.s(3.5F), EfnWidgets.withAlpha(-1, var55));
               }

               if (var28) {
                  if (var20) {
                     this.pendingConfirm = null;
                  } else {
                     var1.image.set(var18);
                  }
               }

               if (!var26 && ImGui.isItemHovered(65536)) {
                  showTooltip(var50 ? var18 + "\nCouldn't load this image" : var18);
               }
            }
         }
      }

      if (var13 != null) {
         this.deleteImage(var13);
      }

      int var39 = (var14 + var5 - 1) / var5;
      float var40 = var10 + var39 * (var9 + var6) - var6 + EfnWidgets.s(10.0F);
      String var41 = "Drop an image on the screen or ";
      String var42 = "choose an image";
      float var43 = EfnWidgets.textWidth(var12, var41);
      float var44 = EfnWidgets.textWidth(var12, var42);
      float var45 = EfnWidgets.lineHeight(var12);
      float var46 = ImGui.getWindowPosX();
      float var47 = ImGui.getWindowPosY() - ImGui.getScrollY();
      EfnWidgets.text(var11, var12, var46 + var2, var47 + var40, -4542808, var41);
      ImGui.setCursorPos(var2 + var43, var40);
      if (ImGui.invisibleButton("##openfolder", var44, var45)) {
         ThemeImages.get().openFolder();
      }

      float var48 = EfnWidgets.animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
      EfnWidgets.text(var11, var12, var46 + var2 + var43, var47 + var40, EfnTheme.lerpColor(GuiPalette.accent(), GuiPalette.accentHover(), var48), var42);
      ImGui.setCursorPos(0.0F, var40 + var45 + EfnWidgets.s(8.0F));
   }

   private void renderThemeActions(ThemeStore var1, float var2, float var3) {
      float var4 = EfnWidgets.s(8.0F);
      float var5 = EfnWidgets.s(34.0F);
      float var6 = (var3 - var4 * 2.0F) / 3.0F;
      float var7 = ImGui.getCursorPosY() + EfnWidgets.s(6.0F);
      ImGui.setCursorPos(var2, var7);
      if (EfnWidgets.chipButton("##copytheme", "Copy theme", var6, var5, false)) {
         MinecraftClient.method_1551().field_1774.method_1455(var1.exportString());
         this.showThemeMessage("Theme copied to the clipboard", false);
      }

      ImGui.setCursorPos(var2 + var6 + var4, var7);
      if (EfnWidgets.chipButton("##pastetheme", "Paste theme", var6, var5, false)) {
         boolean var8 = var1.importString(MinecraftClient.method_1551().field_1774.method_1460());
         this.showThemeMessage(var8 ? "Theme applied from the clipboard" : "The clipboard doesn't contain an EFN theme", !var8);
      }

      ImGui.setCursorPos(var2 + (var6 + var4) * 2.0F, var7);
      if (EfnWidgets.chipButton("##resettheme", "Reset", var6, var5, false)) {
         var1.resetToDefaults();
         this.showThemeMessage("Theme reset to the defaults", false);
      }

      ImGui.setCursorPos(0.0F, var7 + var5);
   }

   private void showThemeMessage(String var1, boolean var2) {
      this.themeMessage = var1;
      this.themeMessageError = var2;
      this.themeMessageAt = System.nanoTime();
   }

   public void onFilesDropped(List<Path> var1) {
      if (!var1.isEmpty()) {
         if (this.tab == ClickGuiRenderer.Tab.CONFIG) {
            for (Path var3 : var1) {
               if (var3.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
                  this.importProfile(var3);
                  return;
               }
            }
         }

         for (Path var5 : var1) {
            if (ThemeImages.isImage(var5)) {
               if (this.tab == ClickGuiRenderer.Tab.HUD) {
                  this.importHudHeaderImage(var5);
               } else {
                  this.switchTab(ClickGuiRenderer.Tab.THEME);
                  this.importHeaderImage(var5);
               }

               return;
            }
         }

         if (this.tab == ClickGuiRenderer.Tab.HUD) {
            this.showHudMessage("Only PNG, JPG and GIF images can be used", true);
         } else {
            this.switchTab(ClickGuiRenderer.Tab.THEME);
            this.showThemeMessage("Only PNG, JPG and GIF images can be used", true);
         }
      }
   }

   private void importHudHeaderImage(Path var1) {
      String var2 = ThemeImages.get().importFile(var1);
      if (var2 == null) {
         this.showHudMessage("Only PNG, JPG and GIF images can be used", true);
      } else {
         String var3 = this.hudElementId != null ? this.hudElementId : "info_bar";
         Surface var4 = HudTheme.get().forElementId(var3);
         if (var4 != null) {
            var4.image.set(var2);
            var4.background.set("Image");
            this.selectHudElement(var3);
            this.showHudMessage(var2 + " is now the " + var4.label.toLowerCase(Locale.ROOT) + " background", false);
         }
      }
   }

   private void importHeaderImage(Path var1) {
      String var2 = ThemeImages.get().importFile(var1);
      if (var2 == null) {
         this.showThemeMessage("Only PNG, JPG and GIF images can be used", true);
      } else {
         ThemeStore var3 = ThemeStore.get();
         Surface var4 = this.themeSurface != null ? this.themeSurface : var3.cards;
         var4.image.set(var2);
         var4.background.set("Image");
         this.showThemeMessage(var2 + " is now the " + var4.label.toLowerCase(Locale.ROOT) + " background", false);
      }
   }

   private void deleteImage(String var1) {
      boolean var3 = ThemeImages.get().delete(var1);
      String var2 = var3 ? "Deleted " + var1 : "Couldn't delete " + var1;
      if (this.tab == ClickGuiRenderer.Tab.HUD) {
         this.showHudMessage(var2, !var3);
      } else {
         this.showThemeMessage(var2, !var3);
      }
   }

   private static void drawCheck(float var0, float var1, float var2, int var3) {
      ImDrawList var4 = ImGui.getWindowDrawList();
      var4.addRectFilled(var0, var1, var0 + 16.0F, var1 + 16.0F, -13226454, 4.0F);
      var4.addRect(var0 + 0.5F, var1 + 0.5F, var0 + 16.0F - 0.5F, var1 + 16.0F - 0.5F, EfnTheme.lerpColor(452984831, var3, var2), 4.0F);
      if (var2 > 0.01F) {
         var4.addLine(var0 + 4.0F, var1 + 8.0F, var0 + 7.0F, var1 + 11.0F, var3, 2.0F * var2);
         var4.addLine(var0 + 7.0F, var1 + 11.0F, var0 + 12.0F, var1 + 5.0F, var3, 2.0F * var2);
      }
   }

   static float smooth(float var0, float var1, float var2, float var3) {
      return var0 + (var1 - var0) * (1.0F - (float)Math.exp(-var3 * Math.max(0.0F, var2)));
   }

   public static float animationDelta(float var0) {
      float var1 = GuiPalette.animationSpeed();
      return var1 <= 0.0F ? 1.0F : var0 * var1;
   }

   public static float elapsed(long var0, float var2) {
      float var3 = GuiPalette.animationSpeed();
      return var3 <= 0.0F ? 1.0F : Math.min(1.0F, (float)(System.nanoTime() - var0) * var3 / (var2 * 1000000.0F));
   }

   public static float easeOut(float var0) {
      float var1 = 1.0F - var0;
      return 1.0F - var1 * var1 * var1;
   }

   private void renderKeybindRow(ClientModule var1, float var2, float var3) {
      float var5 = EfnWidgets.s(46.0F);
      float var6 = this.rowStart(var2, var3);
      boolean var4 = this.listeningModule == var1;
      if (var4) {
         if (this.listeningFirstFrame) {
            this.listeningFirstFrame = false;
         } else {
            int var8 = scanPressedKey();
            if (var8 == 256) {
               var1.setKeybind(-1);
               this.listeningModule = null;
               var4 = false;
            } else if (var8 != -1) {
               var1.setKeybind(var8);
               this.listeningModule = null;
               var4 = false;
            }
         }
      }

      rowLabel("Key", var2, var6, var5);
      float var14 = EfnWidgets.s(30.0F);
      float var9 = EfnWidgets.s(96.0F);
      float var10 = EfnWidgets.s(76.0F);
      float var11 = EfnWidgets.s(8.0F);
      float var12 = var6 + (var5 - var14) * 0.5F;
      if (!var1.isAction()) {
         ImGui.setCursorPos(var2 + var3 - var9 - var11 - var10, var12);
         if (EfnWidgets.chipButton("##bindmode", var1.isHoldMode() ? "Hold" : "Toggle", var10, var14, false)) {
            var1.setHoldMode(!var1.isHoldMode());
         }
      }

      String var13 = var4 ? "Press a key..." : (var1.keybind() == -1 ? "None" : keyName(var1.keybind()));
      ImGui.setCursorPos(var2 + var3 - var9, var12);
      if (EfnWidgets.chipButton("##bind", var13, var9, var14, var4)) {
         if (var4) {
            this.listeningModule = null;
         } else {
            this.listeningModule = var1;
            this.listeningFirstFrame = true;
            this.listeningKeySetting = null;
            this.listeningKeyFirstFrame = false;
         }
      }

      ImGui.setCursorPos(0.0F, var6 + var5);
   }

   public boolean capturesKeys() {
      return this.listeningModule != null || this.listeningKeySetting != null || ImGuiManager.getInstance().typing();
   }

   public boolean handleEscape() {
      if (this.listeningKeySetting != null) {
         this.listeningKeySetting.setValue(-1);
         this.listeningKeySetting = null;
         this.listeningKeyFirstFrame = false;
         return true;
      } else if (this.listeningModule != null) {
         this.listeningModule.setKeybind(-1);
         this.listeningModule = null;
         this.listeningFirstFrame = false;
         return true;
      } else if (this.blockLists.closePicker()) {
         return true;
      } else if (this.itemPickers.closeWindow()) {
         return true;
      } else if (this.settingsModule != null) {
         this.closeSettings();
         return true;
      } else {
         return false;
      }
   }

   public static int scanPressedKey() {
      long var0 = MinecraftClient.method_1551().method_22683().method_4490();

      for (int var2 = 32; var2 <= 348; var2++) {
         if (GLFW.glfwGetKey(var0, var2) == 1) {
            return var2;
         }
      }

      for (int var3 = 0; var3 <= 7; var3++) {
         if (GLFW.glfwGetMouseButton(var0, var3) == 1) {
            return 1000 + var3;
         }
      }

      return -1;
   }

   private String keyLabel(int var1) {
      String var2 = (String)this.keyLabels.get(var1);
      if (var2 == null) {
         var2 = keyName(var1);
         this.keyLabels.put(var1, var2);
      }

      return var2;
   }

   public static String keyName(int var0) {
      if (var0 >= 1000) {
         int var2 = var0 - 1000;

         return switch (var2) {
            case 0 -> "LMB";
            case 1 -> "RMB";
            case 2 -> "MMB";
            default -> "MB" + (var2 + 1);
         };
      } else {
         String var1;
         if ((var0 >= 39 && var0 <= 96 || var0 >= 161 && var0 <= 162) && (var1 = GLFW.glfwGetKeyName(var0, -1)) != null && !var1.isBlank()) {
            return var1.toUpperCase(Locale.ROOT);
         } else {
            return switch (var0) {
               case 32 -> "SPACE";
               case 256 -> "ESC";
               case 257 -> "ENTER";
               case 258 -> "TAB";
               case 259 -> "BKSP";
               case 260 -> "INS";
               case 261 -> "DEL";
               case 262 -> "RIGHT";
               case 263 -> "LEFT";
               case 264 -> "DOWN";
               case 265 -> "UP";
               case 266 -> "PGUP";
               case 267 -> "PGDN";
               case 268 -> "HOME";
               case 269 -> "END";
               case 290 -> "F1";
               case 291 -> "F2";
               case 292 -> "F3";
               case 293 -> "F4";
               case 294 -> "F5";
               case 295 -> "F6";
               case 296 -> "F7";
               case 297 -> "F8";
               case 298 -> "F9";
               case 299 -> "F10";
               case 300 -> "F11";
               case 301 -> "F12";
               case 335 -> "NUM ENTER";
               case 340 -> "L SHIFT";
               case 341 -> "L CTRL";
               case 342 -> "L ALT";
               case 344 -> "R SHIFT";
               case 345 -> "R CTRL";
               case 346 -> "R ALT";
               default -> "K" + var0;
            };
         }
      }
   }

   private void renderModuleConfigActions(ClientModule var1, float var2, float var3) {
      float var4 = EfnWidgets.s(8.0F);
      float var5 = EfnWidgets.s(36.0F);
      float var6 = (var3 - var4) * 0.5F;
      float var7 = ImGui.getCursorPosY() + EfnWidgets.s(6.0F);
      ImGui.setCursorPos(var2, var7);
      if (EfnWidgets.chipButton("##copy_config", "Copy Config", var6, var5, true)) {
         this.copyModuleConfig(var1);
      }

      if (ImGui.isItemHovered(65536)) {
         showTooltip("Copy this module's settings as a short code");
      }

      String var8 = "paste:" + var1.id();
      boolean var9 = this.isConfirming(var8);
      ImGui.setCursorPos(var2 + var6 + var4, var7);
      if (EfnWidgets.chipButton("##paste_config", var9 ? "Confirm paste" : "Paste Config", var6, var5, var9) && this.confirmClick(var8)) {
         this.pasteModuleConfig(var1);
      }

      if (ImGui.isItemHovered(65536)) {
         showTooltip("Replace this module's settings with a code from your clipboard");
      }

      var7 += var5 + EfnWidgets.s(8.0F);
      ImFont var10 = ImGuiManager.getInstance().smallFont();
      if (var1.id().equals(this.moduleCodeFor) && this.moduleCodeMessage != null && elapsed(this.moduleCodeAt, 2500.0F) < 1.0F) {
         ImGui.setCursorPos(var2, var7);
         boolean var11 = pushFont(var10);
         ImGui.pushStyleColor(0, this.moduleCodeError ? -10856974 : -4542808);
         ImGui.textUnformatted(this.moduleCodeMessage);
         ImGui.popStyleColor();
         if (var11) {
            ImGui.popFont();
         }
      }

      ImGui.setCursorPos(0.0F, var7 + EfnWidgets.lineHeight(var10));
   }

   private void copyModuleConfig(ClientModule var1) {
      String var2 = ModuleCodes.copy(var1);
      MinecraftClient.method_1551().field_1774.method_1455(var2);
      this.setModuleCodeMessage(var1, "Copied, " + var2.length() + " characters", false);
   }

   private void pasteModuleConfig(ClientModule var1) {
      try {
         ModuleCodes.paste(var1, MinecraftClient.method_1551().field_1774.method_1460());
         this.setModuleCodeMessage(var1, "Config pasted", false);
      } catch (ProfileException var3) {
         this.setModuleCodeMessage(var1, var3.getMessage(), true);
      }
   }

   private void setModuleCodeMessage(ClientModule var1, String var2, boolean var3) {
      this.moduleCodeFor = var1.id();
      this.moduleCodeMessage = var2;
      this.moduleCodeError = var3;
      this.moduleCodeAt = System.nanoTime();
   }

   private void renderHudActions(float var1, float var2) {
      float var3 = EfnWidgets.s(8.0F);
      float var4 = EfnWidgets.s(36.0F);
      float var5 = (var2 - var3) * 0.5F;
      float var6 = ImGui.getCursorPosY() + EfnWidgets.s(14.0F);
      ImGui.setCursorPos(var1, var6);
      if (EfnWidgets.chipButton("##editlayout", "Edit Layout", var5, var4, true)) {
         MinecraftClient.method_1551().method_1507(new HudEditorScreen());
      }

      ImGui.setCursorPos(var1 + var5 + var3, var6);
      if (EfnWidgets.chipButton("##resetlayout", "Reset Layout", var5, var4, false)) {
         HudLayoutStore.getInstance().resetToDefaults(ImGui.getIO().getDisplaySizeX(), ImGui.getIO().getDisplaySizeY());
      }

      ImGui.setCursorPos(0.0F, var6 + var4);
   }

   private static boolean isHudModule(ClientModule var0) {
      return var0.category() == ModuleCategory.CLIENT && "HUD".equals(var0.name());
   }

   private static ClientModule hudModule() {
      for (ClientModule var1 : ModuleRegistry.all()) {
         if (isHudModule(var1)) {
            return var1;
         }
      }

      return null;
   }

   private void renderHudStudio(float var1, float var2, float var3) {
      this.renderHudInspector(var1, var2);
      this.highlightHudElements();
      float var4 = ImGui.getIO().getDisplaySizeX();
      float var5 = ImGui.getIO().getDisplaySizeY();
      HudRenderer.getInstance().studioInput(var4, var5, hudStudioBlocked());
   }

   private static boolean hudStudioBlocked() {
      return ImGui.isWindowHovered(4) || ImGui.isAnyItemActive() || ImGui.isPopupOpen("", 3072);
   }

   private void renderHudInspector(float var1, float var2) {
      float var3 = ImGui.getIO().getDisplaySizeX();
      float var4 = ImGui.getIO().getDisplaySizeY();
      float var5 = EfnWidgets.s(360.0F);
      float var6 = Math.min(var4 - EfnWidgets.s(86.0F) - EfnWidgets.s(40.0F), EfnWidgets.s(760.0F));
      ImGui.setNextWindowPos(var3 - var5 - EfnWidgets.s(24.0F), EfnWidgets.s(24.0F), 4);
      ImGui.setNextWindowSize(var5, var6, 1);
      if (this.focusHudInspector) {
         ImGui.setNextWindowFocus();
         this.focusHudInspector = false;
      }

      ImGui.pushStyleVar(0, var2);
      ImGui.pushStyleVar(2, 0.0F, 0.0F);
      ImGui.pushStyleVar(14, 0.0F, 0.0F);
      ImGui.pushStyleVar(3, cardRounding());
      ImGui.pushStyleColor(2, GuiPalette.abgr(ThemeStore.get().cards.color.argbWithAlpha(255)));
      byte var7 = 59;
      if (ImGui.begin("##hud_inspector", var7)) {
         float var8 = ImGui.getWindowPosX();
         float var9 = ImGui.getWindowPosY();
         drawShadow(var8, var9, var5, var6, cardRounding(), 6);
         ImGui.getWindowDrawList().addRect(var8, var9, var8 + var5, var9 + var6, EfnWidgets.c(318767103), cardRounding(), 0, 1.0F);
         float var10 = EfnWidgets.s(20.0F);
         float var11 = EfnWidgets.s(20.0F);
         drawCardTitle("HUD", var10, var11 + EfnWidgets.s(14.0F));
         float var12 = var11 + EfnWidgets.s(34.0F);
         if (this.hudMessage != null && System.nanoTime() - this.hudMessageAt < 3000000000L) {
            ImGui.setCursorPos(var10, var12);
            ImGui.pushTextWrapPos(var5 - var10);
            ImGui.pushStyleColor(0, this.hudMessageError ? -10856974 : -461582);
            ImGui.textUnformatted(this.hudMessage);
            ImGui.popStyleColor();
            ImGui.popTextWrapPos();
            var12 = ImGui.getCursorPosY();
         } else {
            var12 = mutedParagraph("Click an element on screen to edit it.", var10, var12, var5 - var10);
         }

         var12 = this.renderHudElementTabs(var10, var12 + EfnWidgets.s(12.0F), var5 - var10 * 2.0F, var1) + EfnWidgets.s(14.0F);
         cardDivider(var10, var5, var12);
         ImGui.setCursorPos(0.0F, var12 + 1.0F);
         if (ImGui.beginChild("##hud_inspector_body", var5, Math.max(1.0F, var6 - var12 - 1.0F - EfnWidgets.s(10.0F)), false, 24)) {
            float var13 = var5 - var10 * 2.0F;
            this.dividerPending = false;
            if (this.hudElementId == null) {
               this.renderHudGeneralOptions(var10, var13);
            } else {
               this.renderHudElementOptions(this.hudElementId, var10, var13);
            }

            ImGui.dummy(1.0F, EfnWidgets.s(18.0F));
            this.applySmoothScroll("hud_inspector", var1);
         }

         ImGui.endChild();
      }

      ImGui.end();
      ImGui.popStyleColor();
      ImGui.popStyleVar(4);
   }

   private float renderHudElementTabs(float var1, float var2, float var3, float var4) {
      @Environment(EnvType.CLIENT)
      record Item(String id, String label) {
      }

      List var5 = List.of(
         new Item(null, "General"),
         new Item("info_bar", HudTheme.get().infoBar.label),
         new Item("coordinates", HudTheme.get().coordinates.label),
         new Item("module_list", HudTheme.get().moduleList.label),
         new Item("potion_tracker", HudTheme.get().potions.label),
         new Item("region_map", HudTheme.get().regionMap.label),
         new Item("anti_vanish", HudTheme.get().antiVanish.label),
         new Item("staff_list", HudTheme.get().staffList.label),
         new Item("spotify", HudTheme.get().spotify.label),
         new Item("hotkeys", HudTheme.get().hotkeys.label),
         new Item("radar", HudRenderer.editorLabel("radar")),
         new Item("target_card", HudTheme.get().targetCard.label),
         new Item("armor_status", HudTheme.get().armorStatus.label),
         new Item("item_cooldowns", HudTheme.get().itemCooldowns.label),
         new Item("toggle_toasts", HudTheme.get().toasts.label)
      );
      ImFont var6 = ImGuiManager.getInstance().smallFont();
      boolean var7 = pushFont(var6);
      float var8 = EfnWidgets.s(24.0F);
      float var9 = EfnWidgets.s(6.0F);
      float var10 = EfnWidgets.s(6.0F);
      float var11 = var1;
      float var12 = var2;

      try {
         for (Item var14 : var5) {
            boolean var15 = Objects.equals(var14.id(), this.hudElementId);
            float var16 = EfnWidgets.textWidth(var6, var14.label()) + EfnWidgets.s(18.0F);
            if (var11 + var16 > var1 + var3 && var11 > var1) {
               var11 = var1;
               var12 += var8 + var10;
            }

            ImGui.setCursorPos(var11, var12);
            if (EfnWidgets.chipButton("##hudel_" + (var14.id() == null ? "general" : var14.id()), var14.label(), var16, var8, var15)) {
               this.selectHudElement(var14.id());
            }

            var11 += var16 + var9;
         }
      } finally {
         if (var7) {
            ImGui.popFont();
         }
      }

      return var12 + var8;
   }

   private void renderHudGeneralOptions(float var1, float var2) {
      ClientModule var3 = hudModule();
      if (var3 != null) {
         this.sectionLabel("Settings", var1);
         this.settingRows(var1, var2, var3.settings().toArray(new Setting[0]));
      }

      this.sectionLabel("Layout", var1);
      this.settingRows(var1, var2, HudTheme.get().font, HudTheme.get().rounding);
      float var4 = ImGui.getCursorPosY() + EfnWidgets.s(6.0F);
      float var5 = EfnWidgets.s(34.0F);
      ImGui.setCursorPos(var1, var4);
      if (EfnWidgets.chipButton("##resethudlayout", "Reset positions & sizes", var2, var5, false)) {
         HudLayoutStore.getInstance().resetToDefaults(ImGui.getIO().getDisplaySizeX(), ImGui.getIO().getDisplaySizeY());
         this.showHudMessage("HUD layout reset", false);
      }

      ImGui.setCursorPos(0.0F, var4 + var5);
      this.sectionLabel("Share", var1);
      this.renderHudThemeActions(var1, var2);
   }

   private void renderHudElementOptions(String var1, float var2, float var3) {
      ClientModule var9 = hudModule();
      HudModule var7;
      List var8;
      if (var9 instanceof HudModule && !(var8 = (var7 = (HudModule)var9).elementSettings(var1)).isEmpty()) {
         this.sectionLabel(hudOptionsTitle(var1), var2);
         this.settingRows(var2, var3, var8.toArray(new Setting[0]));
      }

      RegionMapModule var6;
      if ("region_map".equals(var1) && (var6 = ModuleRegistry.get(RegionMapModule.class)) != null) {
         this.renderInlineModuleToggle(var6, var2, var3);
         this.sectionLabel("Settings", var2);
         this.settingRows(var2, var3, var6.settings().toArray(new Setting[0]));
      }

      ClientModule var5 = (ClientModule)("radar".equals(var1)
         ? ModuleRegistry.get(RadarModule.class)
         : ("hotkeys".equals(var1) ? ModuleRegistry.get(HotkeysModule.class) : null));
      if (var5 != null) {
         this.renderInlineModuleToggle(var5, var2, var3);
         if (!var5.settings().isEmpty()) {
            this.sectionLabel("Settings", var2);
            this.settingRows(var2, var3, var5.settings().toArray(new Setting[0]));
         }
      }

      Surface var4;
      if ((var4 = HudTheme.get().forElementId(var1)) != null) {
         this.renderSurfaceOptions(var4, var2, var3);
      }
   }

   private static String hudOptionsTitle(String var0) {
      return switch (var0) {
         case "toggle_toasts" -> "Style";
         case "target_card", "armor_status", "item_cooldowns" -> "Options";
         default -> "Show";
      };
   }

   private void renderInlineModuleToggle(ClientModule var1, float var2, float var3) {
      float var4 = EfnWidgets.s(46.0F);
      float var5 = this.rowStart(var2, var3);
      rowLabel("Active", var2, var5, var4);
      float var6 = EfnWidgets.s(40.0F);
      float var7 = EfnWidgets.s(22.0F);
      ImGui.setCursorPos(var2 + var3 - var6, var5 + (var4 - var7) * 0.5F);
      float var8 = ImGui.getCursorScreenPosX();
      float var9 = ImGui.getCursorScreenPosY();
      if (ImGui.invisibleButton("##active", var6, var7)) {
         var1.toggle();
      }

      EfnWidgets.drawSwitch(ImGui.getWindowDrawList(), var8, var9, var6, var7, EfnWidgets.animate(ImGui.getItemID(), var1.isEnabled(), 16.0F));
      ImGui.setCursorPos(0.0F, var5 + var4);
   }

   private void renderHudThemeActions(float var1, float var2) {
      HudTheme var3 = HudTheme.get();
      float var4 = EfnWidgets.s(8.0F);
      float var5 = EfnWidgets.s(34.0F);
      float var6 = (var2 - var4 * 2.0F) / 3.0F;
      float var7 = ImGui.getCursorPosY() + EfnWidgets.s(6.0F);
      ImGui.setCursorPos(var1, var7);
      if (EfnWidgets.chipButton("##copyhudtheme", "Copy theme", var6, var5, false)) {
         MinecraftClient.method_1551().field_1774.method_1455(var3.exportString());
         this.showHudMessage("HUD theme copied to the clipboard", false);
      }

      ImGui.setCursorPos(var1 + var6 + var4, var7);
      if (EfnWidgets.chipButton("##pastehudtheme", "Paste theme", var6, var5, false)) {
         boolean var8 = var3.importString(MinecraftClient.method_1551().field_1774.method_1460());
         this.showHudMessage(var8 ? "HUD theme applied from the clipboard" : "The clipboard doesn't contain an EFN HUD theme", !var8);
      }

      ImGui.setCursorPos(var1 + (var6 + var4) * 2.0F, var7);
      if (EfnWidgets.chipButton("##resethudtheme", "Reset", var6, var5, false)) {
         var3.resetToDefaults();
         this.showHudMessage("HUD theme reset to the defaults", false);
      }

      ImGui.setCursorPos(0.0F, var7 + var5);
   }

   private void showHudMessage(String var1, boolean var2) {
      this.hudMessage = var1;
      this.hudMessageError = var2;
      this.hudMessageAt = System.nanoTime();
   }

   private void selectHudElement(String var1) {
      if (!Objects.equals(var1, this.hudElementId)) {
         this.hudElementId = var1;
         this.focusHudInspector = true;
      }
   }

   private void highlightHudElements() {
      float var2 = ImGui.getIO().getMousePosX();
      float var3 = ImGui.getIO().getMousePosY();
      boolean var4 = HudRenderer.getInstance().isDragging();
      HudLayoutStore.HudElementState var5 = null;
      if (!var4 && !hudStudioBlocked()) {
         for (HudLayoutStore.HudElementState var7 : HudLayoutStore.getInstance().orderedElements()) {
            if (var7.positioned && contains(new float[]{var7.x, var7.y, var7.width, var7.height}, var2, var3)) {
               var5 = var7;
            }
         }
      }

      ImDrawList var16 = ImGui.getBackgroundDrawList();
      HudLayoutStore.HudElementState var1;
      if (this.hudElementId != null && (var1 = findHudElementState(this.hudElementId)) != null) {
         this.outlineHudElement(var16, var1, EfnWidgets.withAlpha(GuiPalette.accent(), 0.8F));
      }

      if (var5 != null) {
         if (!var5.id.equals(this.hudElementId)) {
            this.outlineHudElement(var16, var5, -2130706433);
         }

         ImDrawList var17 = ImGui.getForegroundDrawList();
         Surface var8 = HudTheme.get().forElementId(var5.id);
         String var9 = var8 != null ? var8.label : HudRenderer.editorLabel(var5.id);
         String var10 = var9 + "  \u00b7  click to edit, drag to move, scroll to resize";
         ImFont var11 = ImGuiManager.getInstance().smallFont();
         float var12 = EfnWidgets.textWidth(var11, var10) + EfnWidgets.s(18.0F);
         float var13 = EfnWidgets.lineHeight(var11) + EfnWidgets.s(10.0F);
         float var14 = Math.min(var2 + EfnWidgets.s(14.0F), ImGui.getIO().getDisplaySizeX() - var12 - EfnWidgets.s(4.0F));
         float var15 = Math.min(var3 + EfnWidgets.s(18.0F), ImGui.getIO().getDisplaySizeY() - var13 - EfnWidgets.s(4.0F));
         var17.addRectFilled(var14, var15, var14 + var12, var15 + var13, GuiPalette.accent(), var13 * 0.5F);
         EfnWidgets.text(var17, var11, var14 + EfnWidgets.s(9.0F), var15 + EfnWidgets.s(5.0F), -1, var10);
         if (ImGui.isMouseClicked(0) || ImGui.isMouseClicked(1)) {
            this.selectHudElement(var5.id);
         }
      }
   }

   private void outlineHudElement(ImDrawList var1, HudLayoutStore.HudElementState var2, int var3) {
      float var4 = EfnWidgets.s(3.0F);
      float var5 = HudRenderer.hudRounding() * HudRenderer.scaleOf(var2);
      var1.addRect(var2.x - var4, var2.y - var4, var2.x + var2.width + var4, var2.y + var2.height + var4, var3, var5 + var4, 0, EfnWidgets.s(1.5F));
   }

   private static HudLayoutStore.HudElementState findHudElementState(String var0) {
      for (HudLayoutStore.HudElementState var2 : HudLayoutStore.getInstance().orderedElements()) {
         if (var2.id.equals(var0)) {
            return var2;
         }
      }

      return null;
   }

   private void openColorPicker(ClientModule var1, ColorSetting var2) {
      this.activeColorModule = var1;
      this.activeColorSetting = var2;
      this.colorPickerOpen.set(true);
   }

   private void renderColorPickerWindow() {
      if (this.activeColorSetting != null) {
         if (!this.colorPickerOpen.get()) {
            this.activeColorSetting = null;
            this.activeColorModule = null;
         } else {
            ImGui.setNextWindowSize(270.0F, 330.0F, 4);
            ImGui.setNextWindowPos(260.0F, 90.0F, 4);
            short var2 = 352;
            String var1 = this.activeColorModule != null ? this.activeColorModule.name() + " / " : "";
            if (ImGui.begin("Color - " + var1 + this.activeColorSetting.name() + "##color_picker", this.colorPickerOpen, var2)) {
               int var4 = 135593984;
               if (ImGui.colorPicker4("##picker", this.activeColorSetting.value(), var4)) {
                  this.activeColorSetting.notifyChanged();
               }

               ImGui.setNextItemWidth(240.0F);
               if (ImGui.colorEdit4("RGBA", this.activeColorSetting.value(), var4)) {
                  this.activeColorSetting.notifyChanged();
               }

               if (ImGui.button("Close", 80.0F, 22.0F)) {
                  this.activeColorSetting.notifyChanged();
                  this.colorPickerOpen.set(false);
               }
            }

            ImGui.end();
            if (!this.colorPickerOpen.get()) {
               this.activeColorSetting = null;
               this.activeColorModule = null;
            }
         }
      }
   }

   private void renderItemColorPaletteWindow() {
      if (this.itemColorWindowOpen && this.activeItemColorPalette != null) {
         float var1 = Math.max(ImGui.getIO().getDeltaTime(), 0.001F);
         ImGui.setNextWindowSize(340.0F, 500.0F, 4);
         ImGui.setNextWindowPos(340.0F, 100.0F, 4);
         short var2 = 288;
         boolean var3 = ImGui.begin("Item Colors##icpalette", var2);
         if (!var3) {
            ImGui.end();
            if (this.activeItemColorPalette != null) {
               this.activeItemColorPalette.notifyChanged();
            }

            this.itemColorWindowOpen = false;
         } else {
            ImGui.text("Default:");
            ImGui.sameLine();
            int var4 = 131648;
            if (ImGui.colorButton("##icdefcol", this.activeItemColorPalette.defaultColor(), var4, 42.0F, 16.0F)) {
               this.editingItemDefaultColor = true;
               this.activeItemColorEntry = null;
               this.itemColorPickerOpen.set(true);
            }

            ImGui.separator();
            ImGui.text("Search:");
            ImGui.sameLine();
            ImGui.setNextItemWidth(200.0F);
            ImGui.inputText("##icsearch", this.itemSearchBuf);
            String var5 = this.itemSearchBuf.get().toLowerCase().trim();
            ImGui.separator();
            ImGui.beginChild("##iclist", 0.0F, 360.0F, false, 0);

            for (ItemColorListSetting.ItemEntry var7 : this.activeItemColorPalette.entries()) {
               boolean var8 = var5.isEmpty() || var7.displayName().toLowerCase().contains(var5) || var7.id().toLowerCase().contains(var5);
               if (var8) {
                  float var10 = ImGui.getCursorPosY();
                  float var11 = 22.0F;
                  float var12 = ImGui.getContentRegionAvailX();
                  ImGui.setCursorPos(4.0F, var10 + (var11 - 16.0F) * 0.5F);
                  ImGui.pushID(var7.id() + "_ictog");
                  this.renderItemEntryToggle(this.activeItemColorPalette, var7, var1);
                  ImGui.popID();
                  float var13 = ImGui.getTextLineHeight();
                  ImGui.setCursorPos(24.0F, var10 + (var11 - var13) * 0.5F);
                  ImGui.textUnformatted(var7.displayName());
                  float var14 = var12 - 46.0F;
                  if (var14 < 96.0F) {
                     var14 = 96.0F;
                  }

                  ImGui.setCursorPos(var14, var10 + (var11 - 16.0F) * 0.5F);
                  int var15 = 131648;
                  ImGui.pushID(var7.id() + "_iccol");
                  if (ImGui.colorButton("##ic", var7.color(), var15, 42.0F, 16.0F)) {
                     this.activeItemColorEntry = var7;
                     this.editingItemDefaultColor = false;
                     this.itemColorPickerOpen.set(true);
                  }

                  ImGui.popID();
                  ImGui.setCursorPosY(var10 + var11 + 2.0F);
               }
            }

            ImGui.endChild();
            ImGui.separator();
            if (ImGui.button("Close##icclose", 80.0F, 22.0F)) {
               this.activeItemColorPalette.notifyChanged();
               this.itemColorWindowOpen = false;
               this.activeItemColorPalette = null;
            }

            ImGui.end();
            this.renderItemColorPicker();
         }
      }
   }

   private void renderItemColorPicker() {
      if (!this.itemColorPickerOpen.get()) {
         this.activeItemColorEntry = null;
         this.editingItemDefaultColor = false;
      } else if (this.activeItemColorEntry == null && !this.editingItemDefaultColor) {
         this.itemColorPickerOpen.set(false);
      } else {
         String var1;
         float[] var2;
         if (this.editingItemDefaultColor && this.activeItemColorPalette != null) {
            var2 = this.activeItemColorPalette.defaultColor();
            var1 = "Default Color";
         } else {
            var2 = this.activeItemColorEntry.color();
            var1 = this.activeItemColorEntry.displayName();
         }

         ImGui.setNextWindowSize(270.0F, 330.0F, 4);
         ImGui.setNextWindowPos(700.0F, 100.0F, 4);
         short var3 = 352;
         if (ImGui.begin("Item Color - " + var1, this.itemColorPickerOpen, var3)) {
            int var4 = 135593984;
            ImGui.colorPicker4("##icpicker", var2, var4);
            ImGui.setNextItemWidth(240.0F);
            if (ImGui.colorEdit4("RGBA##icce", var2, var4) && this.activeItemColorPalette != null) {
               this.activeItemColorPalette.notifyChanged();
            }

            if (ImGui.button("Close##iccp", 80.0F, 22.0F)) {
               if (this.activeItemColorPalette != null) {
                  this.activeItemColorPalette.notifyChanged();
               }

               this.itemColorPickerOpen.set(false);
               this.activeItemColorEntry = null;
               this.editingItemDefaultColor = false;
            }
         }

         ImGui.end();
         if (!this.itemColorPickerOpen.get()) {
            this.activeItemColorEntry = null;
            this.editingItemDefaultColor = false;
         }
      }
   }

   private void renderItemEntryToggle(ItemColorListSetting var1, ItemColorListSetting.ItemEntry var2, float var3) {
      float var4 = 16.0F;
      float var5 = 16.0F;
      float var6 = ImGui.getCursorScreenPosX();
      float var7 = ImGui.getCursorScreenPosY();
      if (ImGui.invisibleButton("##ietog", 16.0F, 16.0F)) {
         var2.enabled().set(!var2.enabled().get());
         var1.notifyChanged();
      }

      float var8 = var2.enabled().get() ? 1.0F : 0.0F;
      float var9 = this.itemEntryTogAnim.getOrDefault(var2, var8);
      var9 = smooth(var9, var8, var3, 14.0F);
      this.itemEntryTogAnim.put(var2, var9);
      drawCheck(var6, var7, var9, GuiPalette.accent());
   }

   private void renderSetting(ClientModule var1, Setting var2, float var3, float var4) {
      float var9 = EfnWidgets.s(46.0F);
      float var10 = this.rowStart(var3, var4);
      ImDrawList var11 = ImGui.getWindowDrawList();
      if (var2 instanceof BooleanSetting var13) {
         ImGui.setCursorPos(var3, var10);
         float var14 = ImGui.getCursorScreenPosX();
         float var15 = ImGui.getCursorScreenPosY();
         if (ImGui.invisibleButton("##row", var4, var9)) {
            var13.set(!var13.get());
         }

         float var12;
         if ((var12 = EfnWidgets.animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F)) > 0.01F) {
            var11.addRectFilled(
               var14 - EfnWidgets.s(10.0F),
               var15 + EfnWidgets.s(3.0F),
               var14 + var4 + EfnWidgets.s(10.0F),
               var15 + var9 - EfnWidgets.s(3.0F),
               EfnWidgets.c(EfnWidgets.withAlpha(184549375, var12)),
               EfnWidgets.s(10.0F)
            );
         }

         rowLabel(var2.name(), var3, var10, var9);
         float var16 = EfnWidgets.s(40.0F);
         float var17 = EfnWidgets.s(22.0F);
         EfnWidgets.drawSwitch(
            var11, var14 + var4 - var16, var15 + (var9 - var17) * 0.5F, var16, var17, EfnWidgets.animate(ImGui.getID("##switch"), var13.get(), 16.0F)
         );
      } else if (var2 instanceof NumberSetting var23) {
         var9 = EfnWidgets.s(64.0F);
         ImGui.setCursorPos(var3, var10 + EfnWidgets.s(10.0F));
         ImGui.textUnformatted(var2.name());
         ImFont var32 = ImGuiManager.getInstance().smallFont();
         String var39 = var23.formatted();
         float var43 = EfnWidgets.textWidth(var32, var39);
         float var49 = var43 + EfnWidgets.s(16.0F);
         float var53 = EfnWidgets.lineHeight(var32) + EfnWidgets.s(8.0F);
         float var18 = ImGui.getWindowPosX();
         float var19 = ImGui.getWindowPosY() - ImGui.getScrollY();
         float var20 = var18 + var3 + var4 - var49;
         float var21 = var19 + var10 + EfnWidgets.s(8.0F);
         var11.addRectFilled(var20, var21, var20 + var49, var21 + var53, EfnWidgets.c(-13226454), var53 * 0.5F);
         EfnWidgets.text(var11, var32, var20 + EfnWidgets.s(8.0F), var21 + EfnWidgets.s(4.0F), -461582, var39);
         ImGui.setCursorPos(var3 - EfnWidgets.s(7.0F), var10 + EfnWidgets.s(36.0F));
         if (EfnWidgets.sliderFloat("##v", var23.value(), var23.min(), var23.max(), var4 + EfnWidgets.s(14.0F), var23::snap)) {
            var23.notifyChanged();
         }
      } else if (var2 instanceof ModeSetting var24) {
         rowLabel(var2.name(), var3, var10, var9);
         float var33 = 0.0F;

         for (String var44 : var24.modes()) {
            var33 = Math.max(var33, ImGui.calcTextSize(var44).x);
         }

         float var41 = Math.min(var4 * 0.6F, Math.max(EfnWidgets.s(120.0F), var33 + EfnWidgets.s(44.0F)));
         float var45 = EfnWidgets.s(30.0F);
         ImGui.setCursorPos(var3 + var4 - var41, var10 + (var9 - var45) * 0.5F);
         int var50 = EfnWidgets.dropdown("##mode", var24.get(), var24.modes(), var41, var45);
         if (var50 >= 0) {
            var24.set(var24.modes().get(var50));
         }
      } else if (var2 instanceof ColorSetting var25) {
         rowLabel(var2.name(), var3, var10, var9);
         float var34 = EfnWidgets.s(44.0F);
         float var42 = EfnWidgets.s(24.0F);
         ImGui.setCursorPos(var3 + var4 - var34, var10 + (var9 - var42) * 0.5F);
         float var46 = ImGui.getCursorScreenPosX();
         float var51 = ImGui.getCursorScreenPosY();
         if (ImGui.invisibleButton("##color", var34, var42)) {
            this.openColorPicker(var1, var25);
         }

         boolean var54 = ImGui.isItemHovered();
         float[] var56 = var25.value();
         int var57 = (int)(var56[0] * 255.0F) & 0xFF;
         int var58 = (int)(var56[1] * 255.0F) & 0xFF;
         int var59 = (int)(var56[2] * 255.0F) & 0xFF;
         int var22 = var56.length > 3 ? (int)(var56[3] * 255.0F) & 0xFF : 255;
         var11.addRectFilled(var46, var51, var46 + var34, var51 + var42, EfnWidgets.c(-13226454), EfnWidgets.s(7.0F));
         var11.addRectFilled(var46, var51, var46 + var34, var51 + var42, EfnWidgets.c(var22 << 24 | var59 << 16 | var58 << 8 | var57), EfnWidgets.s(7.0F));
         var11.addRect(var46, var51, var46 + var34, var51 + var42, EfnWidgets.c(var54 ? 1728053247 : 654311423), EfnWidgets.s(7.0F), 0, 1.0F);
         if (var54) {
            showTooltip(String.format("#%02X%02X%02X", var57, var58, var59));
         }
      } else if (var2 instanceof KeySetting var35) {
         boolean var26 = this.listeningKeySetting == var35;
         if (var26) {
            if (this.listeningKeyFirstFrame) {
               this.listeningKeyFirstFrame = false;
            } else {
               int var47 = scanPressedKey();
               if (var47 == 256) {
                  var35.setValue(-1);
                  this.listeningKeySetting = null;
                  var26 = false;
               } else if (var47 != -1) {
                  var35.setValue(var47);
                  this.listeningKeySetting = null;
                  var26 = false;
               }
            }
         }

         rowLabel(var2.name(), var3, var10, var9);
         float var48 = EfnWidgets.s(96.0F);
         float var52 = EfnWidgets.s(30.0F);
         String var55 = var26 ? "Press a key..." : (var35.value() == -1 ? "None" : keyName(var35.value()));
         ImGui.setCursorPos(var3 + var4 - var48, var10 + (var9 - var52) * 0.5F);
         if (EfnWidgets.chipButton("##keyval", var55, var48, var52, var26)) {
            if (var26) {
               this.listeningKeySetting = null;
            } else {
               this.listeningKeySetting = var35;
               this.listeningKeyFirstFrame = true;
               this.listeningModule = null;
               this.listeningFirstFrame = false;
            }
         }
      } else {
         StringSetting var8;
         if (var2 instanceof StringSetting && !(var8 = (StringSetting)var2).presets().isEmpty()) {
            var9 = this.itemPickers.renderPresets(var1, var8, var3, var4, var10);
         } else {
            StringSetting var7;
            if (var2 instanceof StringSetting && (var7 = (StringSetting)var2).isItemPicker()) {
               var9 = this.itemPickers.renderItem(var1, var7, var3, var4, var10);
            } else {
               StringSetting var6;
               if (var2 instanceof StringSetting && (var6 = (StringSetting)var2).enchantItem() != null) {
                  var9 = this.itemPickers.renderEnchants(var1, var6, var3, var4, var10);
               } else if (var2 instanceof StringSetting var36) {
                  ImGui.setCursorPos(var3, var10 + EfnWidgets.s(10.0F));
                  ImGui.textUnformatted(var2.name());
                  ImGui.setCursorPos(var3, var10 + EfnWidgets.s(34.0F));
                  ImGui.pushStyleVar(11, EfnWidgets.s(12.0F), EfnWidgets.s(8.0F));
                  ImGui.setNextItemWidth(var4);
                  boolean var27 = var36.hint() != null
                     ? ImGui.inputTextWithHint("##str", var36.hint(), var36.getImString())
                     : ImGui.inputText("##str", var36.getImString());
                  if (var27) {
                     var36.notifyChanged();
                  }

                  ImGui.popStyleVar();
                  var9 = EfnWidgets.s(34.0F) + ImGui.getTextLineHeight() + EfnWidgets.s(16.0F) + EfnWidgets.s(12.0F);
               } else {
                  StringListSetting var5;
                  if (var2 instanceof StringListSetting && (var5 = (StringListSetting)var2).isItemPicker()) {
                     var9 = this.itemPickers.renderList(var1, var5, var3, var4, var10);
                  } else if (var2 instanceof StringListSetting var28) {
                     var9 = renderStringList(var28, var3, var4, var10);
                  } else if (var2 instanceof ActionSetting var29) {
                     float var37 = EfnWidgets.s(36.0F);
                     ImGui.setCursorPos(var3, var10 + (var9 + EfnWidgets.s(6.0F) - var37) * 0.5F);
                     if (EfnWidgets.chipButton("##action", var2.name(), var4, var37, false)) {
                        var29.run();
                     }

                     var9 += EfnWidgets.s(6.0F);
                  } else if (var2 instanceof BlockColorListSetting var30) {
                     var9 = this.blockLists.render(var1, var30, var3, var4, var10);
                  } else if (var2 instanceof ItemColorListSetting var31) {
                     long var38 = var31.entries().stream().filter(var0 -> var0.enabled().get()).count();
                     if (listChip(var2.name(), "Edit (" + var38 + ")", var3, var4, var10, var9)) {
                        this.activeItemColorPalette = var31;
                        this.itemColorWindowOpen = true;
                        this.activeItemColorEntry = null;
                        this.editingItemDefaultColor = false;
                     }
                  }
               }
            }
         }
      }

      if (!(var2 instanceof ActionSetting)) {
         this.renderResetAffordance(var2, var3, var4, var10, var9);
      }

      ImGui.setCursorPos(0.0F, var10 + var9);
   }

   private void renderResetAffordance(Setting var1, float var2, float var3, float var4, float var5) {
      if (this.tab != ClickGuiRenderer.Tab.THEME) {
         boolean var7 = !var1.isDefault();
         float var8 = ImGui.getWindowPosX();
         float var9 = ImGui.getWindowPosY() - ImGui.getScrollY();
         boolean var10 = var1 instanceof NumberSetting
            || var1 instanceof StringSetting
            || var1 instanceof StringListSetting
            || var1 instanceof BlockColorListSetting;
         float var6 = var10 ? var4 + EfnWidgets.s(10.0F) + ImGui.getTextLineHeight() * 0.5F : var4 + var5 * 0.5F;
         if (var7) {
            float var12 = var8 + var2 - EfnWidgets.s(11.0F);
            float var13 = var9 + var6;
            float var14 = EfnWidgets.s(3.0F);
            ImGui.getWindowDrawList().addCircleFilled(var12, var13, var14, EfnWidgets.c(GuiPalette.accent()), 12);
            if (ImGui.isMouseHoveringRect(var12 - var14 * 2.0F, var13 - var14 * 2.0F, var12 + var14 * 2.0F, var13 + var14 * 2.0F)) {
               showTooltip("Modified - right-click the row to reset");
            }
         }

         float var15 = var8 + var2 - EfnWidgets.s(10.0F);
         float var16 = var9 + var4;
         if (ImGui.isWindowHovered() && ImGui.isMouseClicked(1) && ImGui.isMouseHoveringRect(var15, var16, var15 + var3 + EfnWidgets.s(20.0F), var16 + var5)) {
            ImGui.openPopup("##setting_ctx");
         }

         ImGui.pushStyleVar(2, EfnWidgets.s(6.0F), EfnWidgets.s(6.0F));
         if (ImGui.beginPopup("##setting_ctx")) {
            ImGui.textDisabled(var1.name());
            ImGui.separator();
            if (var7) {
               if (ImGui.selectable("Reset to default")) {
                  var1.resetToDefault();
               }
            } else {
               ImGui.textDisabled("Already at default");
            }

            ImGui.endPopup();
         }

         ImGui.popStyleVar();
      }
   }

   private static float renderStringList(StringListSetting var0, float var1, float var2, float var3) {
      ImGui.setCursorPos(var1, var3 + EfnWidgets.s(10.0F));
      ImGui.textUnformatted(var0.name());
      float var5 = var3 + EfnWidgets.s(34.0F);
      float var6 = EfnWidgets.s(6.0F);
      float var4;
      float var7 = var4 = ImGui.getTextLineHeight() + EfnWidgets.s(16.0F);
      int var8 = -1;
      ImGui.pushStyleVar(11, EfnWidgets.s(12.0F), EfnWidgets.s(8.0F));

      for (int var9 = 0; var9 < var0.size(); var9++) {
         ImGui.pushID(var9);
         ImGui.setCursorPos(var1, var5);
         ImGui.setNextItemWidth(var2 - var7 - var6);
         if (ImGui.inputText("##entry", var0.entry(var9))) {
            var0.edited();
         }

         ImGui.setCursorPos(var1 + var2 - var7, var5);
         if (EfnWidgets.chipButton("##remove", "x", var7, var4, false)) {
            var8 = var9;
         }

         ImGui.popID();
         var5 += var4 + var6;
      }

      ImGui.popStyleVar();
      if (var8 >= 0) {
         var0.remove(var8);
      }

      if (var0.canAdd()) {
         float var10 = EfnWidgets.s(30.0F);
         ImGui.setCursorPos(var1, var5);
         if (EfnWidgets.chipButton("##add", "+ Add", var2, var10, false)) {
            var0.add();
         }

         var5 += var10;
      }

      return var5 - var3 + EfnWidgets.s(12.0F);
   }

   private static boolean listChip(String var0, String var1, float var2, float var3, float var4, float var5) {
      rowLabel(var0, var2, var4, var5);
      float var6 = Math.max(EfnWidgets.s(96.0F), ImGui.calcTextSize(var1).x + EfnWidgets.s(28.0F));
      float var7 = EfnWidgets.s(30.0F);
      ImGui.setCursorPos(var2 + var3 - var6, var4 + (var5 - var7) * 0.5F);
      return EfnWidgets.chipButton("##listedit", var1, var6, var7, false);
   }

   public static void applyTheme() {
      ImGui.pushStyleVar(3, EfnWidgets.s(10.0F));
      ImGui.pushStyleVar(12, EfnWidgets.s(8.0F));
      ImGui.pushStyleVar(9, EfnWidgets.s(10.0F));
      ImGui.pushStyleVar(21, EfnWidgets.s(6.0F));
      ImGui.pushStyleVar(2, EfnWidgets.s(10.0F), EfnWidgets.s(10.0F));
      ImGui.pushStyleVar(11, EfnWidgets.s(10.0F), EfnWidgets.s(6.0F));
      ImGui.pushStyleVar(14, EfnWidgets.s(8.0F), EfnWidgets.s(6.0F));
      ImGui.pushStyleVar(4, 0.0F);
      ImGui.pushStyleVar(10, 1.0F);
      ImGui.pushStyleVar(18, EfnWidgets.s(6.0F));
      ImGui.pushStyleVar(19, EfnWidgets.s(3.0F));

      for (byte var0 = 0; var0 < THEME_COLORS.length; var0 += 2) {
         ImGui.pushStyleColor(THEME_COLORS[var0], THEME_COLORS[var0 + 1]);
      }

      int var2 = GuiPalette.accent();
      int var1 = GuiPalette.cardBg();
      ImGui.pushStyleColor(2, GuiPalette.panelBg());
      ImGui.pushStyleColor(10, var1);
      ImGui.pushStyleColor(11, var1);
      ImGui.pushStyleColor(12, var1);
      ImGui.pushStyleColor(18, var2);
      ImGui.pushStyleColor(19, var2);
      ImGui.pushStyleColor(20, var2);
      ImGui.pushStyleColor(23, var2);
      ImGui.pushStyleColor(26, var2);
      ImGui.pushStyleColor(51, EfnWidgets.withAlpha(var2, 0.4F));
   }

   public static void removeTheme() {
      ImGui.popStyleColor(THEME_COLORS.length / 2 + 10);
      ImGui.popStyleVar(11);
   }

   public static boolean pushFont(ImFont var0) {
      if (var0 == null) {
         return false;
      } else {
         ImGui.pushFont(var0);
         return true;
      }
   }

   @Environment(EnvType.CLIENT)
   private record CategoryPanel(ModuleCategory category, String id, String rowsId) {
   }

   @Environment(EnvType.CLIENT)
   private record PanelLayout(List<String> ids, List<String> keys, List<Float> heights) {
   }

   @Environment(EnvType.CLIENT)
   private static enum Tab {
      MODULES("Modules"),
      HUD("HUD"),
      FRIENDS("Friends"),
      THEME("Theme"),
      CONFIG("Config");

      final String label;

      private Tab(String nullxx) {
         this.label = nullxx;
      }
   }
}
