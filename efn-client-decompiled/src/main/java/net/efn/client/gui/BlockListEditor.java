package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.setting.BlockColorListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
final class BlockListEditor {
   private static final int INLINE_MAX_ROWS = 8;
   private static final float HEADER_H = 40.0F;
   private static final float INLINE_ROW_H = 38.0F;
   private static final float LIST_PAD = 4.0F;
   private static final float EMPTY_H = 44.0F;
   private static final float PICKER_W = 820.0F;
   private static final float PICKER_MAX_H = 760.0F;
   private static final float PICKER_ROW_H = 42.0F;
   private static final float COLUMN_GAP = 28.0F;
   private static final float TAB_BAR_ROOM = 86.0F;
   private static final float ICON_SLOT = 32.0F;
   private static final float COLOR_CONTROLS_W = 88.0F;
   private static final float COLOR_COLUMN_EXTRA = 98.0F;
   private static final float COLOR_WINDOW_GAP = 12.0F;
   private static final float COLOR_WINDOW_W = 290.0F;
   private static final float COLOR_WINDOW_H = 360.0F;
   private static final int ADD_ALL_CONFIRM_OVER = 64;
   private static final long ARM_NANOS = 3000000000L;
   private static final int ARM_RESET = 1;
   private static final int ARM_CLEAR = 2;
   private static final int ARM_ADD = 3;
   private static final int LIST_BG = EfnWidgets.withAlpha(-13226454, 0.5F);
   private static final int ROW_HOVER = 318767103;
   private static final int SCRIM = 1929379840;
   private static final int TRACER_ACCENT = -12276993;
   private static final String SELECT_LABEL = "Select";
   private static final String EMPTY_TEXT = "No blocks yet";
   private static final String PICKER_TITLE = "Select Blocks";
   private static final String AVAILABLE_TITLE = "All blocks";
   private static final String CHOSEN_TITLE = "Selected";
   private static final String NO_MATCHES = "No blocks match";
   private static final String ALL_CHOSEN = "Every block is selected";
   private static final String NONE_CHOSEN = "Nothing selected yet";
   private static final String SEARCH_HINT = "Search blocks...";
   private static final String RESET_LABEL = "Reset";
   private static final String ADD_SHOWN_LABEL = "Add all shown";
   private static final String CLEAR_LABEL = "Clear all";
   private static final String CONFIRM_LABEL = "Confirm";
   private static final int PICKER_FLAGS = 8639;
   private static final int LEV_INSERT = 1;
   private static final int LEV_SUBSTITUTE = 8;
   private static final int LEV_DELETE = 8;
   private static final Reference2ObjectOpenHashMap<Block, BlockListEditor.BlockInfo> INFOS = new Reference2ObjectOpenHashMap<>();
   private static final Map<BlockColorListSetting, BlockListEditor.ListState> LISTS = new WeakHashMap<>();
   private static String language;
   private static int iconsFrame = -2;
   private static int namesGeneration;
   private static int[] levPrev = new int[64];
   private static int[] levCur = new int[64];
   private final Reference2ObjectOpenHashMap<BlockColorListSetting, float[]> inlineScroll = new Reference2ObjectOpenHashMap<>();
   private BlockColorListSetting picker;
   private BlockListEditor.ListState pickerList;
   private String pickerSubtitle;
   private final ImString search = new ImString(64);
   private boolean focusSearch;
   private boolean searchActive;
   private long pickerOpenedAt;
   private boolean pickerJustOpened;
   private final float[] availableScroll = new float[2];
   private final float[] chosenScroll = new float[2];
   private int armedAction;
   private BlockColorListSetting armedFor;
   private long armedAt;
   private BlockColorListSetting colorList;
   private BlockColorListSetting.BlockEntry colorEntry;
   private boolean colorTracer;
   private String colorTitle;
   private final ImBoolean colorOpen = new ImBoolean(false);
   private float colorAtX;
   private float colorAtY;
   private boolean colorFocus;

   BlockListEditor() {
   }

   float render(ClientModule clientModule, BlockColorListSetting blockColorListSetting, float f, float g, float h) {
      beginFrame();
      BlockListEditor.ListState var6 = list(blockColorListSetting);
      refreshChosen(blockColorListSetting, var6);
      boolean var7 = !blockColorListSetting.colorsHidden();
      ImDrawList var8 = ImGui.getWindowDrawList();
      ImFont var9 = ImGuiManager.getInstance().moduleFont();
      ImFont var10 = ImGuiManager.getInstance().smallFont();
      float var11 = ImGui.getWindowPosX();
      float var12 = ImGui.getWindowPosY() - ImGui.getScrollY();
      ImGui.setCursorPos(f, h + EfnWidgets.s(10.0F));
      ImGui.textUnformatted(blockColorListSetting.name());
      float var13 = var12 + h + EfnWidgets.s(10.0F) + ImGui.getTextLineHeight() * 0.5F;
      countPill(var8, var10, ImGui.getItemRectMaxX() + EfnWidgets.s(8.0F), var13, var6.countPill);
      float var14 = EfnWidgets.textWidth(var9, "Select") + EfnWidgets.s(28.0F);
      float var15 = EfnWidgets.s(28.0F);
      float var16 = var11 + f + g - var14;
      ImGui.setCursorScreenPos(var16, var13 - var15 * 0.5F);
      if (EfnWidgets.chipButton("##add_blocks", "Select", var14, var15, false)) {
         this.openPicker(clientModule, blockColorListSetting, var6);
      }

      if (var7) {
         float var17 = EfnWidgets.s(30.0F);
         float var18 = EfnWidgets.s(20.0F);
         if (swatch(var8, "##default_color", blockColorListSetting.defaultColor(), var16 - EfnWidgets.s(10.0F) - var17, var13 - var18 * 0.5F, var17, var18)) {
            this.openColor(blockColorListSetting, null, false);
         }

         if (ImGui.isItemHovered(32768) && !this.editing(blockColorListSetting, null, false)) {
            ClickGuiRenderer.showTooltip("Default colour, for blocks not on the list");
         }
      }

      float var27 = h + EfnWidgets.s(40.0F);
      float var28 = EfnWidgets.s(38.0F);
      int var19 = var6.chosenCount;
      float var20 = var19 == 0 ? EfnWidgets.s(44.0F) : Math.min(var19, 8) * var28 + EfnWidgets.s(4.0F) * 2.0F;
      float var21 = var11 + f;
      float var22 = var12 + var27;
      float var23 = EfnWidgets.s(10.0F);
      var8.addRectFilled(var21, var22, var21 + g, var22 + var20, EfnWidgets.c(LIST_BG), var23);
      if (var19 == 0) {
         EfnWidgets.text(var8, var9, var21 + EfnWidgets.s(14.0F), var22 + (var20 - EfnWidgets.lineHeight(var9)) * 0.5F, -4542808, "No blocks yet");
      } else if (var19 <= 8) {
         this.drawChosenRows(var8, blockColorListSetting, var6, f, var27 + EfnWidgets.s(4.0F), g, var28, 0, var19, var7, var9, var10);
      } else {
         ImGui.setCursorPos(f, var27 + EfnWidgets.s(4.0F));
         if (ImGui.beginChild("##chosen", g, var20 - EfnWidgets.s(4.0F) * 2.0F, false, 24)) {
            ImGui.setCursorPos(0.0F, 0.0F);
            ImGui.dummy(1.0F, var19 * var28);
            float var24 = ImGui.getScrollY();
            int var25 = Math.max(0, (int)(var24 / var28));
            int var26 = Math.min(var19, (int)Math.ceil((var24 + ImGui.getWindowHeight()) / var28));
            this.drawChosenRows(ImGui.getWindowDrawList(), blockColorListSetting, var6, 0.0F, 0.0F, g, var28, var25, var26, var7, var9, var10);
            ClickGuiRenderer.smoothScroll(this.inlineScroll.computeIfAbsent(blockColorListSetting, object -> new float[2]), frameDelta());
         }

         ImGui.endChild();
      }

      return EfnWidgets.s(40.0F) + var20 + EfnWidgets.s(12.0F);
   }

   private void drawChosenRows(
      ImDrawList imDrawList,
      BlockColorListSetting blockColorListSetting,
      BlockListEditor.ListState listState,
      float f,
      float g,
      float h,
      float i,
      int j,
      int k,
      boolean bl,
      ImFont imFont,
      ImFont imFont2
   ) {
      boolean var13 = ImGui.isWindowHovered();
      float var14 = BlockIcons.size(EfnWidgets.s(32.0F));
      float var15 = EfnWidgets.s(22.0F);
      float var16 = EfnWidgets.lineHeight(imFont);
      float var17 = EfnWidgets.lineHeight(imFont2);
      BlockListEditor.Row var18 = null;

      for (int var19 = j; var19 < k; var19++) {
         BlockListEditor.Row var21 = listState.chosen[var19];
         ImGui.setCursorPos(f, g + var19 * i);
         float var22 = ImGui.getCursorScreenPosX();
         float var23 = ImGui.getCursorScreenPosY();
         float var24 = var23 + i * 0.5F;
         if (var13 && ImGui.isMouseHoveringRect(var22, var23, var22 + h, var23 + i)) {
            imDrawList.addRectFilled(
               var22 + EfnWidgets.s(4.0F),
               var23 + EfnWidgets.s(1.0F),
               var22 + h - EfnWidgets.s(4.0F),
               var23 + i - EfnWidgets.s(1.0F),
               EfnWidgets.c(318767103),
               EfnWidgets.s(7.0F)
            );
         }

         ImGui.pushID(var21.info.rawId);
         float var25 = var22 + h - EfnWidgets.s(10.0F);
         ImGui.setCursorScreenPos(var25 - var15, var24 - var15 * 0.5F);
         if (ImGui.invisibleButton("##remove", var15, var15)) {
            var18 = var21;
         }

         boolean var20;
         if (var20 = ImGui.isItemHovered()) {
            imDrawList.addCircleFilled(var25 - var15 * 0.5F, var24, var15 * 0.5F, EfnWidgets.c(-12240588), 20);
         }

         EfnWidgets.cross(imDrawList, var25 - var15 * 0.5F, var24, EfnWidgets.s(3.5F), var20 ? -10856974 : -4542808);
         var25 -= var15 + EfnWidgets.s(8.0F);
         if (bl) {
            var25 = this.colorControls(imDrawList, blockColorListSetting, var21, var25, var24);
         }

         ImGui.popID();
         float var26 = var22 + EfnWidgets.s(8.0F);
         BlockIcons.draw(imDrawList, var21.info.block, var26, var24 - var14 * 0.5F, var14);
         float var27 = var26 + var14 + EfnWidgets.s(10.0F);
         BlockListEditor.Fit var28 = fit(var21.inlineFit, var21.info, imFont, imFont2, var25 - var27);
         EfnWidgets.text(imDrawList, imFont, var27, var24 - var16 * 0.5F, -461582, var28.text);
         if (var28.hint != null) {
            EfnWidgets.text(imDrawList, imFont2, var27 + var28.textW + EfnWidgets.s(6.0F), var24 - var17 * 0.5F, -4542808, var28.hint);
         }
      }

      if (var18 != null) {
         deselect(blockColorListSetting, var18);
      }
   }

   private float colorControls(ImDrawList imDrawList, BlockColorListSetting blockColorListSetting, BlockListEditor.Row row, float f, float g) {
      float var6 = EfnWidgets.s(30.0F);
      float var7 = EfnWidgets.s(18.0F);
      float var8 = EfnWidgets.s(16.0F);
      float var9 = EfnWidgets.s(6.0F);
      float var10 = f - var6;
      if (swatch(imDrawList, "##tracer_color", row.entry.tracerColor(), var10, g - var7 * 0.5F, var6, var7)) {
         this.openColor(blockColorListSetting, row, true);
      }

      if (ImGui.isItemHovered(32768) && !this.editing(blockColorListSetting, row.entry, true)) {
         ClickGuiRenderer.showTooltip("Tracer colour");
      }

      if (checkbox(imDrawList, "##tracer", row.entry.tracerEnabled().get(), var10 = var10 - (var9 + var8), g - var8 * 0.5F, var8)) {
         row.entry.tracerEnabled().set(!row.entry.tracerEnabled().get());
         blockColorListSetting.notifyChanged();
      }

      if (ImGui.isItemHovered(32768)) {
         ClickGuiRenderer.showTooltip("Tracer");
      }

      if (swatch(imDrawList, "##box_color", row.entry.color(), var10 = var10 - (var9 + var6), g - var7 * 0.5F, var6, var7)) {
         this.openColor(blockColorListSetting, row, false);
      }

      if (ImGui.isItemHovered(32768) && !this.editing(blockColorListSetting, row.entry, false)) {
         ClickGuiRenderer.showTooltip("Box colour");
      }

      return var10 - EfnWidgets.s(10.0F);
   }

   void renderWindows() {
      if (this.picker != null) {
         this.renderPicker();
      }

      if (this.colorList != null) {
         this.renderColorWindow();
      }
   }

   void close() {
      this.closePicker();
      this.closeColor();
   }

   boolean closePicker() {
      if (this.picker == null) {
         return false;
      } else {
         this.picker = null;
         this.pickerList = null;
         this.disarm();
         return true;
      }
   }

   private void openPicker(ClientModule clientModule, BlockColorListSetting blockColorListSetting, BlockListEditor.ListState listState) {
      this.closeColor();
      this.picker = blockColorListSetting;
      this.pickerList = listState;
      this.pickerSubtitle = clientModule.name() + " / " + blockColorListSetting.name();
      this.search.set("");
      this.focusSearch = true;
      this.searchActive = false;
      this.pickerOpenedAt = System.nanoTime();
      this.pickerJustOpened = true;
      this.chosenScroll[1] = 0.0F;
      this.chosenScroll[0] = 0.0F;
      this.availableScroll[1] = 0.0F;
      this.availableScroll[0] = 0.0F;
      this.disarm();
   }

   private void renderPicker() {
      beginFrame();
      BlockColorListSetting var1 = this.picker;
      BlockListEditor.ListState var2 = this.pickerList;
      refreshChosen(var1, var2);
      boolean var3 = !var1.colorsHidden();
      float var4 = ImGui.getIO().getDisplaySizeX();
      float var5 = ImGui.getIO().getDisplaySizeY();
      float var6 = EfnWidgets.s(16.0F);
      float var7 = var3 ? EfnWidgets.s(98.0F) : 0.0F;
      float var8 = Math.min(var4 - var6 * 2.0F, EfnWidgets.s(820.0F) + var7);
      float var9 = Math.min(var5 - EfnWidgets.s(86.0F) - EfnWidgets.s(32.0F), EfnWidgets.s(760.0F));
      float var10 = ClickGuiRenderer.easeOut(ClickGuiRenderer.elapsed(this.pickerOpenedAt, 200.0F));
      float var11 = Math.round((var4 - var8) * 0.5F);
      float var12 = Math.round(Math.max(var6, (var5 - EfnWidgets.s(86.0F) - var9) * 0.5F + EfnWidgets.s(8.0F)) + EfnWidgets.s(14.0F) * (1.0F - var10));
      float var13 = ClickGuiRenderer.cardRounding();
      ImGui.setNextWindowPos(0.0F, 0.0F, 1);
      ImGui.setNextWindowSize(var4, var5, 1);
      if (this.pickerJustOpened) {
         ImGui.setNextWindowFocus();
      }

      ImGui.pushStyleVar(0, Math.max(0.01F, var10));
      ImGui.pushStyleVar(2, 0.0F, 0.0F);
      ImGui.pushStyleVar(14, 0.0F, 0.0F);
      ImGui.pushStyleVar(4, 0.0F);
      boolean var14 = false;
      boolean var15 = ImGui.begin("##block_picker", 8639);
      if (this.pickerJustOpened) {
         imgui.internal.ImGui.bringWindowToDisplayFront(imgui.internal.ImGui.getCurrentWindow());
      }

      if (var15) {
         ImDrawList var16 = ImGui.getWindowDrawList();
         var16.addRectFilled(0.0F, 0.0F, var4, var5, EfnWidgets.c(1929379840));
         shadow(var16, var11, var12, var8, var9, var13);
         var16.addRectFilled(var11, var12, var11 + var8, var12 + var9, EfnWidgets.c(GuiPalette.abgr(ThemeStore.get().cards.color.argbWithAlpha(255))), var13);
         var16.addRect(var11, var12, var11 + var8, var12 + var9, EfnWidgets.c(318767103), var13, 0, 1.0F);
         ImFont var17 = ImGuiManager.getInstance().moduleFont();
         ImFont var18 = ImGuiManager.getInstance().smallFont();
         float var19 = EfnWidgets.s(24.0F);
         float var20 = var12 + EfnWidgets.s(22.0F);
         float var21 = EfnWidgets.s(28.0F);
         ClickGuiRenderer.drawCardTitle("Select Blocks", var11 + var19, var20 + var21 * 0.5F);
         ImGui.setCursorPos(var11 + var8 - var19 - var21, var20);
         if (EfnWidgets.iconButton("##close", var21)) {
            var14 = true;
         }

         EfnWidgets.cross(var16, var11 + var8 - var19 - var21 * 0.5F, var20 + var21 * 0.5F, EfnWidgets.s(4.5F), -461582);
         if (chip(
               "##reset_blocks",
               "Reset",
               this.armed(1, var1),
               var1.isDefault(),
               var11 + var8 - var19 - var21 - EfnWidgets.s(10.0F),
               var20 + var21 * 0.5F,
               var18,
               "Back to the default blocks"
            )
            && this.confirm(1, var1)) {
            var1.resetToDefault();
         }

         float var22 = var20 + var21 + EfnWidgets.s(2.0F);
         EfnWidgets.text(var16, var18, var11 + var19, var22, -4542808, this.pickerSubtitle);
         ImGui.setCursorPos(var11 + var19, var22 + EfnWidgets.lineHeight(var18) + EfnWidgets.s(14.0F));
         ImGui.pushStyleVar(11, EfnWidgets.s(12.0F), EfnWidgets.s(8.0F));
         ImGui.setNextItemWidth(var8 - var19 * 2.0F);
         if (this.focusSearch) {
            ImGui.setKeyboardFocusHere();
            this.focusSearch = false;
         }

         boolean var23 = this.searchActive && (ImGui.isKeyPressed(525, false) || ImGui.isKeyPressed(627, false));
         ImGui.inputTextWithHint("##block_search", "Search blocks...", this.search);
         this.searchActive = ImGui.isItemActive();
         ImGui.popStyleVar();
         float var24 = ImGui.getItemRectMaxY() + EfnWidgets.s(18.0F);
         String var25 = var2.searchKey;
         refreshSearch(var2, this.search.get());
         if (var2.searchKey != var25) {
            this.chosenScroll[1] = 0.0F;
            this.chosenScroll[0] = 0.0F;
            this.availableScroll[1] = 0.0F;
            this.availableScroll[0] = 0.0F;
            if (this.armedAction == 3) {
               this.disarm();
            }
         }

         refreshColumns(var1, var2);
         if (var23 && !var2.searchKey.isBlank()) {
            if (var2.availableCount > 0) {
               select(var1, var2.available[0]);
            }

            this.focusSearch = true;
         }

         float var26 = (var8 - var19 * 2.0F - EfnWidgets.s(28.0F) - var7) * 0.5F;
         float var27 = var26 + var7;
         float var28 = var11 + var19;
         float var29 = var28 + var26 + EfnWidgets.s(28.0F);
         float var30 = EfnWidgets.s(28.0F);
         float var31 = var24 + var30 + EfnWidgets.s(10.0F);
         float var32 = var12 + var9 - EfnWidgets.s(18.0F);
         if (var2.availableLabelFor != var2.availableCount) {
            var2.availableLabelFor = var2.availableCount;
            var2.availableLabel = Integer.toString(var2.availableCount);
         }

         if (var2.shownLabelFor != var2.shownCount) {
            var2.shownLabelFor = var2.shownCount;
            var2.shownLabel = Integer.toString(var2.shownCount);
         }

         columnHeader(var16, var17, var18, var28, var24, var30, var26, "All blocks", var2.availableLabel);
         columnHeader(var16, var17, var18, var29, var24, var30, var27, "Selected", var2.shownLabel);
         float var33 = var24 + var30 * 0.5F;
         boolean var34 = chip(
            "##add_shown",
            "Add all shown",
            this.armed(3, var1),
            var2.availableCount == 0,
            var28 + var26 - EfnWidgets.s(6.0F),
            var33,
            var18,
            "Adds every block this column lists"
         );
         boolean var35 = chip(
            "##clear_all",
            "Clear all",
            this.armed(2, var1),
            var2.chosenCount == 0,
            var29 + var27 - EfnWidgets.s(6.0F),
            var33,
            var18,
            "Takes every block off the list"
         );
         float var36 = Math.round(var28 + var26 + EfnWidgets.s(28.0F) * 0.5F);
         var16.addLine(var36, var24, var36, var32, EfnWidgets.c(268435455), 1.0F);
         float var37 = Math.max(1.0F, var32 - var31);
         String var38 = var2.searchKey.isBlank() ? "Every block is selected" : "No blocks match";
         String var39 = var2.searchKey.isBlank() ? "Nothing selected yet" : "No blocks match";
         BlockListEditor.Row var40 = this.column(
            "##available", var2.available, var2.availableCount, true, null, var28, var31, var26, var37, this.availableScroll, var38, var17, var18
         );
         BlockListEditor.Row var41 = this.column(
            "##chosen", var2.shown, var2.shownCount, false, var3 ? var1 : null, var29, var31, var27, var37, this.chosenScroll, var39, var17, var18
         );
         if (var40 != null) {
            select(var1, var40);
         }

         if (var41 != null) {
            deselect(var1, var41);
         }

         if (var34) {
            this.addAllShown(var1, var2);
         }

         if (var35) {
            this.clearAll(var1);
         }

         if (!this.pickerJustOpened
            && ImGui.isMouseClicked(0)
            && ImGui.isWindowHovered()
            && !ImGui.isMouseHoveringRect(var11, var12, var11 + var8, var12 + var9)) {
            var14 = true;
         }
      }

      ImGui.end();
      ImGui.popStyleVar(4);
      this.pickerJustOpened = false;
      if (var14) {
         this.closePicker();
      }
   }

   private static boolean chip(String string, String string2, boolean bl, boolean bl2, float f, float g, ImFont imFont, String string3) {
      boolean var8 = ClickGuiRenderer.pushFont(imFont);
      float var9 = Math.max(EfnWidgets.textWidth(imFont, string2), EfnWidgets.textWidth(imFont, "Confirm")) + EfnWidgets.s(20.0F);
      float var10 = EfnWidgets.lineHeight(imFont) + EfnWidgets.s(12.0F);
      ImGui.setCursorPos(f - var9, g - var10 * 0.5F);
      ImGui.beginDisabled(bl2);
      boolean var11 = EfnWidgets.chipButton(string, bl ? "Confirm" : string2, var9, var10, bl);
      ImGui.endDisabled();
      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip(string3);
      }

      if (var8) {
         ImGui.popFont();
      }

      return var11;
   }

   private boolean confirm(int i, BlockColorListSetting blockColorListSetting) {
      if (this.armed(i, blockColorListSetting)) {
         this.disarm();
         return true;
      } else {
         this.armedAction = i;
         this.armedFor = blockColorListSetting;
         this.armedAt = System.nanoTime();
         return false;
      }
   }

   private boolean armed(int i, BlockColorListSetting blockColorListSetting) {
      return this.armedAction == i && this.armedFor == blockColorListSetting && System.nanoTime() - this.armedAt < 3000000000L;
   }

   private void disarm() {
      this.armedAction = 0;
      this.armedFor = null;
   }

   private static void columnHeader(ImDrawList imDrawList, ImFont imFont, ImFont imFont2, float f, float g, float h, float i, String string, String string2) {
      float var9 = g + h * 0.5F;
      EfnWidgets.text(imDrawList, imFont, f + EfnWidgets.s(4.0F), var9 - EfnWidgets.lineHeight(imFont) * 0.5F, -461582, string);
      countPill(imDrawList, imFont2, f + EfnWidgets.s(4.0F) + EfnWidgets.textWidth(imFont, string) + EfnWidgets.s(8.0F), var9, string2);
      imDrawList.addLine(f, g + h + EfnWidgets.s(4.0F), f + i, g + h + EfnWidgets.s(4.0F), EfnWidgets.c(268435455), 1.0F);
   }

   private BlockListEditor.Row column(
      String string,
      BlockListEditor.Row[] rows,
      int i,
      boolean bl,
      BlockColorListSetting blockColorListSetting,
      float f,
      float g,
      float h,
      float j,
      float[] fs,
      String string2,
      ImFont imFont,
      ImFont imFont2
   ) {
      BlockListEditor.Row var14 = null;
      ImGui.setCursorPos(f, g);
      if (ImGui.beginChild(string, h, j, false, 24)) {
         float var15 = EfnWidgets.s(42.0F);
         float var16 = h - EfnWidgets.s(6.0F);
         float var17 = var15 - EfnWidgets.s(2.0F);
         ImGui.setCursorPos(0.0F, 0.0F);
         ImGui.dummy(1.0F, i * var15 + EfnWidgets.s(6.0F));
         ImDrawList var18 = ImGui.getWindowDrawList();
         if (i == 0) {
            EfnWidgets.text(var18, imFont, ImGui.getWindowPosX() + EfnWidgets.s(8.0F), ImGui.getWindowPosY() + EfnWidgets.s(12.0F), -4542808, string2);
         } else {
            float var19 = EfnWidgets.lineHeight(imFont);
            float var20 = EfnWidgets.lineHeight(imFont2);
            float var21 = BlockIcons.size(EfnWidgets.s(32.0F));
            float var22 = EfnWidgets.s(11.0F);
            float var23 = ImGui.getScrollY();
            int var24 = Math.max(0, (int)(var23 / var15));
            int var25 = Math.min(i, (int)Math.ceil((var23 + ImGui.getWindowHeight()) / var15));
            int var26 = GuiPalette.accent();
            boolean var27 = ImGui.isWindowHovered();

            for (int var28 = var24; var28 < var25; var28++) {
               BlockListEditor.Row var31 = rows[var28];
               ImGui.setCursorPos(0.0F, var28 * var15);
               float var32 = ImGui.getCursorScreenPosX();
               float var33 = ImGui.getCursorScreenPosY();
               float var34 = var33 + var17 * 0.5F;
               float var35 = var32 + var16 - EfnWidgets.s(8.0F) - var22;
               ImGui.pushID(var31.info.rawId);
               float var29;
               boolean var30;
               if (blockColorListSetting == null) {
                  if (ImGui.invisibleButton("##row", var16, var17)) {
                     var14 = var31;
                  }

                  if (var30 = ImGui.isItemHovered()) {
                     var18.addRectFilled(var32, var33, var32 + var16, var33 + var17, EfnWidgets.c(318767103), EfnWidgets.s(8.0F));
                  }

                  var29 = var35 - var22 - EfnWidgets.s(10.0F);
               } else {
                  if (var27 && ImGui.isMouseHoveringRect(var32, var33, var32 + var16, var33 + var17)) {
                     var18.addRectFilled(var32, var33, var32 + var16, var33 + var17, EfnWidgets.c(318767103), EfnWidgets.s(8.0F));
                  }

                  float var36 = var35 - var22 - EfnWidgets.s(6.0F);
                  var29 = this.colorControls(var18, blockColorListSetting, var31, var36 - EfnWidgets.s(4.0F), var34);
                  ImGui.setCursorScreenPos(var36, var33);
                  if (ImGui.invisibleButton("##minus", var32 + var16 - var36, var17)) {
                     var14 = var31;
                  }

                  var30 = ImGui.isItemHovered();
                  ImGui.setCursorScreenPos(var32, var33);
                  if (ImGui.invisibleButton("##row", Math.max(1.0F, var29 + EfnWidgets.s(4.0F) - var32), var17)) {
                     var14 = var31;
                  }

                  var30 |= ImGui.isItemHovered();
               }

               ImGui.popID();
               float var42 = var32 + EfnWidgets.s(6.0F);
               BlockIcons.draw(var18, var31.info.block, var42, var34 - var21 * 0.5F, var21);
               float var37 = var42 + var21 + EfnWidgets.s(12.0F);
               BlockListEditor.Fit var38 = fit(var31.pickerFit, var31.info, imFont, imFont2, var29 - var37);
               EfnWidgets.text(var18, imFont, var37, var34 - var19 * 0.5F, var30 ? -461582 : -2042416, var38.text);
               if (var38.hint != null) {
                  EfnWidgets.text(var18, imFont2, var37 + var38.textW + EfnWidgets.s(6.0F), var34 - var20 * 0.5F, -4542808, var38.hint);
               }

               int var39 = !var30 ? -13226454 : (bl ? var26 : -10856974);
               int var40 = var30 ? -1 : -4542808;
               var18.addCircleFilled(var35, var34, var22, EfnWidgets.c(var39), 24);
               var18.addLine(var35 - var22 * 0.45F, var34, var35 + var22 * 0.45F, var34, EfnWidgets.c(var40), EfnWidgets.s(1.6F));
               if (bl) {
                  var18.addLine(var35, var34 - var22 * 0.45F, var35, var34 + var22 * 0.45F, EfnWidgets.c(var40), EfnWidgets.s(1.6F));
               }
            }
         }

         ClickGuiRenderer.smoothScroll(fs, frameDelta());
      }

      ImGui.endChild();
      return var14;
   }

   private void openColor(BlockColorListSetting blockColorListSetting, BlockListEditor.Row row, boolean bl) {
      this.closeColor();
      this.colorList = blockColorListSetting;
      this.colorEntry = row == null ? null : row.entry;
      this.colorTracer = bl;
      String var4 = row == null ? "Default Color" : (bl ? row.info.name + " Tracer" : row.info.name);
      this.colorTitle = "Block Color - " + var4 + "###block_color";
      this.colorOpen.set(true);
      if (ImGui.isMousePosValid()) {
         this.colorAtX = ImGui.getIO().getMousePosX();
         this.colorAtY = ImGui.getIO().getMousePosY();
      } else {
         this.colorAtX = ImGui.getIO().getDisplaySizeX() * 0.5F;
         this.colorAtY = ImGui.getIO().getDisplaySizeY() * 0.5F;
      }

      this.colorFocus = true;
   }

   private void renderColorWindow() {
      float[] var2 = this.colorEntry == null ? this.colorList.defaultColor() : (this.colorTracer ? this.colorEntry.tracerColor() : this.colorEntry.color());
      float var3 = ImGui.getIO().getDisplaySizeX();
      float var4 = ImGui.getIO().getDisplaySizeY();
      boolean var5 = this.colorAtX + 12.0F + 290.0F > var3;
      boolean var6 = this.colorAtY + 12.0F + 360.0F > var4;
      ImGui.setNextWindowPos(this.colorAtX + (var5 ? -12.0F : 12.0F), this.colorAtY + (var6 ? -12.0F : 12.0F), 8, var5 ? 1.0F : 0.0F, var6 ? 1.0F : 0.0F);
      ImGui.setNextWindowSize(270.0F, 330.0F, 4);
      if (this.colorFocus) {
         ImGui.setNextWindowFocus();
         this.colorFocus = false;
      }

      short var1 = 352;
      if (ImGui.begin(this.colorTitle, this.colorOpen, 352)) {
         int var7 = 135593984;
         if (ImGui.colorPicker4("##bcpicker", var2, var7)) {
            this.colorList.notifyChanged();
         }

         ImGui.setNextItemWidth(240.0F);
         if (ImGui.colorEdit4("RGBA##bcce", var2, var7)) {
            this.colorList.notifyChanged();
         }

         if (ImGui.button("Close##bccp", 80.0F, 22.0F)) {
            this.colorOpen.set(false);
         }
      }

      ImGui.end();
      if (!this.colorOpen.get()) {
         this.closeColor();
      }
   }

   private boolean editing(BlockColorListSetting blockColorListSetting, BlockColorListSetting.BlockEntry blockEntry, boolean bl) {
      return this.colorList == blockColorListSetting && this.colorEntry == blockEntry && this.colorTracer == bl;
   }

   private void closeColor() {
      if (this.colorList != null) {
         this.colorList.notifyChanged();
         this.colorList = null;
         this.colorEntry = null;
         this.colorOpen.set(false);
      }
   }

   private static void select(BlockColorListSetting blockColorListSetting, BlockListEditor.Row row) {
      blockColorListSetting.select(row.entry);
      if (row.follower != null) {
         blockColorListSetting.select(row.follower.entry);
      }
   }

   private static void deselect(BlockColorListSetting blockColorListSetting, BlockListEditor.Row row) {
      blockColorListSetting.deselect(row.entry);
      if (row.follower != null) {
         blockColorListSetting.deselect(row.follower.entry);
      }
   }

   private void addAllShown(BlockColorListSetting blockColorListSetting, BlockListEditor.ListState listState) {
      int var3 = listState.availableCount;
      if (var3 != 0 && (var3 <= 64 || this.confirm(3, blockColorListSetting))) {
         ArrayList var4 = new ArrayList(var3 + 8);

         for (int var5 = 0; var5 < var3; var5++) {
            BlockListEditor.Row var6 = listState.available[var5];
            var4.add(var6.entry);
            if (var6.follower != null) {
               var4.add(var6.follower.entry);
            }
         }

         blockColorListSetting.selectAll(var4);
      }
   }

   private void clearAll(BlockColorListSetting blockColorListSetting) {
      if (this.confirm(2, blockColorListSetting)) {
         blockColorListSetting.deselectAll();
      }
   }

   private static void beginFrame() {
      refreshNames();
      int var0 = ImGuiManager.getInstance().frameCount();
      if (var0 > iconsFrame + 1) {
         BlockIcons.forget();
      }

      iconsFrame = var0;
      BlockIcons.validate();
   }

   private static void refreshNames() {
      String var0 = MinecraftClient.getInstance().getLanguageManager().getLanguage();
      if (!var0.equals(language)) {
         language = var0;
         if (INFOS.isEmpty()) {
            for (Block var2 : Registries.BLOCK) {
               INFOS.put(var2, new BlockListEditor.BlockInfo(var2));
            }
         }

         Object2IntOpenHashMap var4 = new Object2IntOpenHashMap();

         for (BlockListEditor.BlockInfo var3 : INFOS.values()) {
            var3.name = var3.block.getName().getString();
            var3.nameLower = var3.name.toLowerCase(Locale.ROOT);
            if (!var3.wallBanner) {
               var4.addTo(var3.name, 1);
            }
         }

         for (BlockListEditor.BlockInfo var7 : INFOS.values()) {
            var7.hint = !var7.wallBanner && var4.getInt(var7.name) <= 1 ? null : var7.path;
         }

         namesGeneration++;
      }
   }

   private static BlockListEditor.ListState list(BlockColorListSetting blockColorListSetting) {
      BlockListEditor.ListState var1 = LISTS.get(blockColorListSetting);
      if (var1 == null) {
         var1 = new BlockListEditor.ListState(blockColorListSetting);
         LISTS.put(blockColorListSetting, var1);
      }

      return var1;
   }

   private static void refreshChosen(BlockColorListSetting blockColorListSetting, BlockListEditor.ListState listState) {
      if (listState.chosenRevision != blockColorListSetting.revision()) {
         listState.chosenRevision = blockColorListSetting.revision();
         List var2 = blockColorListSetting.selected();
         if (listState.chosen.length < var2.size()) {
            listState.chosen = new BlockListEditor.Row[var2.size()];
         }

         int var3 = 0;

         for (int var4 = 0; var4 < var2.size(); var4++) {
            BlockListEditor.Row var5 = listState.byEntry.get(var2.get(var4));
            if (var5 != null && (var5.leader == null || !var5.leader.entry.enabled().get())) {
               listState.chosen[var3++] = var5;
            }
         }

         listState.chosenCount = var3;
         if (listState.labelsFor != var3) {
            listState.labelsFor = var3;
            listState.countPill = Integer.toString(var3);
         }
      }
   }

   private static void refreshSearch(BlockListEditor.ListState listState, String string) {
      if (string != listState.searchKey || listState.searchGeneration != namesGeneration) {
         listState.searchKey = string;
         listState.searchGeneration = namesGeneration;
         BlockListEditor.Row[] var2 = listState.pickable;
         String var3 = string.trim().toLowerCase(Locale.ROOT);
         if (var3.isEmpty()) {
            int var9 = 0;

            while (var9 < var2.length) {
               listState.results[var9] = var9++;
            }

            listState.resultCount = var2.length;
         } else {
            String[] var4 = var3.split(" ");
            long[] var5 = listState.sortKeys;
            int var6 = 0;

            for (int var7 = 0; var7 < var2.length; var7++) {
               int var8 = matchDistance(var2[var7].info, var3, var4);
               if (var8 >= 0) {
                  var5[var6++] = (long)var8 << 32 | var7;
               }
            }

            Arrays.sort(var5, 0, var6);

            for (int var10 = 0; var10 < var6; var10++) {
               listState.results[var10] = (int)var5[var10];
            }

            listState.resultCount = var6;
         }
      }
   }

   private static void refreshColumns(BlockColorListSetting blockColorListSetting, BlockListEditor.ListState listState) {
      if (listState.columnsRevision != blockColorListSetting.revision()
         || listState.columnsKey != listState.searchKey
         || listState.columnsGeneration != namesGeneration) {
         listState.columnsRevision = blockColorListSetting.revision();
         listState.columnsKey = listState.searchKey;
         listState.columnsGeneration = namesGeneration;
         int var3 = 0;

         for (int var4 = 0; var4 < listState.resultCount; var4++) {
            BlockListEditor.Row var5 = listState.pickable[listState.results[var4]];
            if (!var5.entry.enabled().get()) {
               listState.available[var3++] = var5;
            }
         }

         listState.availableCount = var3;
         if (listState.shown.length < listState.chosen.length) {
            listState.shown = new BlockListEditor.Row[listState.chosen.length];
            listState.shownKeys = new long[listState.chosen.length];
         }

         String var2;
         if ((var2 = listState.searchKey.trim().toLowerCase(Locale.ROOT)).isEmpty()) {
            System.arraycopy(listState.chosen, 0, listState.shown, 0, listState.chosenCount);
            listState.shownCount = listState.chosenCount;
         } else {
            String[] var9 = var2.split(" ");
            long[] var10 = listState.shownKeys;
            int var6 = 0;

            for (int var7 = 0; var7 < listState.chosenCount; var7++) {
               int var8 = matchDistance(listState.chosen[var7].info, var2, var9);
               if (var8 >= 0) {
                  var10[var6++] = (long)var8 << 32 | var7;
               }
            }

            Arrays.sort(var10, 0, var6);

            for (int var11 = 0; var11 < var6; var11++) {
               listState.shown[var11] = listState.chosen[(int)var10[var11]];
            }

            listState.shownCount = var6;
         }
      }
   }

   private static int matchDistance(BlockListEditor.BlockInfo blockInfo, String string, String[] strings) {
      int var3 = Math.max(wordsFound(blockInfo.nameLower, strings), wordsFound(blockInfo.idLower, strings));
      int var4 = levenshtein(string, blockInfo.nameLower);
      int var5 = levenshtein(string, blockInfo.idLower);
      float var6 = Math.max(1.0F - (float)var4 / blockInfo.nameLower.length(), 1.0F - (float)var5 / blockInfo.idLower.length());
      return var3 <= 0 && !(var6 >= 0.5F) ? -1 : Math.min(var4, var5);
   }

   private static int wordsFound(String string, String[] strings) {
      int var2 = 0;

      for (String var6 : strings) {
         if (!string.contains(var6)) {
            return 0;
         }

         if (!var6.isEmpty()) {
            for (int var7 = string.indexOf(var6); var7 >= 0; var7 = string.indexOf(var6, var7 + var6.length())) {
               var2++;
            }
         }
      }

      return var2;
   }

   private static int levenshtein(String string, String string2) {
      int var2 = string.length();
      int var3 = string2.length();
      if (var2 == 0) {
         return var3 * 1;
      } else if (var3 == 0) {
         return var2 * 8;
      } else {
         if (levPrev.length <= var3) {
            levPrev = new int[var3 + 1];
            levCur = new int[var3 + 1];
         }

         int[] var4 = levPrev;
         int[] var5 = levCur;

         for (int var6 = 0; var6 <= var3; var6++) {
            var4[var6] = var6 * 1;
         }

         for (int var10 = 1; var10 <= var2; var10++) {
            var5[0] = var10 * 8;
            char var7 = string.charAt(var10 - 1);

            for (int var8 = 1; var8 <= var3; var8++) {
               int var9 = var4[var8 - 1] + (var7 == string2.charAt(var8 - 1) ? 0 : 8);
               var5[var8] = Math.min(Math.min(var4[var8] + 8, var5[var8 - 1] + 1), var9);
            }

            int[] var11 = var4;
            var4 = var5;
            var5 = var11;
         }

         return var4[var3];
      }
   }

   private static BlockListEditor.Fit fit(BlockListEditor.Fit fit, BlockListEditor.BlockInfo blockInfo, ImFont imFont, ImFont imFont2, float f) {
      if (fit.width == f && fit.generation == namesGeneration) {
         return fit;
      } else {
         fit.width = f;
         fit.generation = namesGeneration;
         fit.text = EfnWidgets.ellipsize(imFont, blockInfo.name, f);
         fit.textW = EfnWidgets.textWidth(imFont, fit.text);
         float var5 = f - fit.textW - EfnWidgets.s(6.0F);
         fit.hint = blockInfo.hint != null && var5 >= EfnWidgets.s(40.0F) ? EfnWidgets.ellipsize(imFont2, blockInfo.hint, var5) : null;
         return fit;
      }
   }

   private static void countPill(ImDrawList imDrawList, ImFont imFont, float f, float g, String string) {
      float var5 = EfnWidgets.textWidth(imFont, string) + EfnWidgets.s(14.0F);
      float var6 = EfnWidgets.lineHeight(imFont) + EfnWidgets.s(6.0F);
      float var7 = g - var6 * 0.5F;
      imDrawList.addRectFilled(f, var7, f + var5, var7 + var6, EfnWidgets.c(-13226454), var6 * 0.5F);
      EfnWidgets.text(imDrawList, imFont, f + EfnWidgets.s(7.0F), var7 + EfnWidgets.s(3.0F), -4542808, string);
   }

   private static void shadow(ImDrawList imDrawList, float f, float g, float h, float i, float j) {
      for (int var6 = 1; var6 <= 6; var6++) {
         float var7 = EfnWidgets.s(2.5F) * var6;
         imDrawList.addRectFilled(f - var7, g - var7 + EfnWidgets.s(4.0F), f + h + var7, g + i + var7 + EfnWidgets.s(4.0F), EfnWidgets.c(335544320), j + var7);
      }
   }

   private static boolean swatch(ImDrawList imDrawList, String string, float[] fs, float f, float g, float h, float i) {
      ImGui.setCursorScreenPos(f, g);
      boolean var7 = ImGui.invisibleButton(string, h, i);
      boolean var8 = ImGui.isItemHovered();
      float var9 = EfnWidgets.s(5.0F);
      imDrawList.addRectFilled(f, g, f + h, g + i, EfnWidgets.c(-13226454), var9);
      imDrawList.addRectFilled(f, g, f + h, g + i, EfnWidgets.c(abgr(fs)), var9);
      imDrawList.addRect(f, g, f + h, g + i, EfnWidgets.c(var8 ? 1728053247 : 654311423), var9, 0, 1.0F);
      return var7;
   }

   private static boolean checkbox(ImDrawList imDrawList, String string, boolean bl, float f, float g, float h) {
      ImGui.setCursorScreenPos(f, g);
      boolean var6 = ImGui.invisibleButton(string, h, h);
      int var7 = bl ? -12276993 : (ImGui.isItemHovered() ? 1090519039 : 452984831);
      float var8 = EfnWidgets.s(4.0F);
      imDrawList.addRectFilled(f, g, f + h, g + h, EfnWidgets.c(-13226454), var8);
      imDrawList.addRect(f + 0.5F, g + 0.5F, f + h - 0.5F, g + h - 0.5F, EfnWidgets.c(var7), var8, 0, 1.0F);
      if (bl) {
         EfnWidgets.check(imDrawList, f + h * 0.5F, g + h * 0.5F, h * 0.28F, -12276993);
      }

      return var6;
   }

   private static int abgr(float[] fs) {
      return unit(fs[3]) << 24 | unit(fs[2]) << 16 | unit(fs[1]) << 8 | unit(fs[0]);
   }

   private static int unit(float f) {
      return Math.round(Math.max(0.0F, Math.min(1.0F, f)) * 255.0F);
   }

   private static float frameDelta() {
      return ClickGuiRenderer.animationDelta(Math.max(ImGui.getIO().getDeltaTime(), 0.001F));
   }

   @Environment(EnvType.CLIENT)
   private static final class BlockInfo {
      final Block block;
      final int rawId;
      final String id;
      final String idLower;
      final String path;
      final boolean wallBanner;
      final String wallBannerId;
      String name;
      String nameLower;
      String hint;

      BlockInfo(Block block) {
         this.block = block;
         this.rawId = Registries.BLOCK.getRawId(block);
         Identifier var2 = Registries.BLOCK.getId(block);
         this.id = var2.toString();
         this.idLower = this.id.toLowerCase(Locale.ROOT);
         this.path = var2.getPath();
         this.wallBanner = this.path.endsWith("_wall_banner");
         this.wallBannerId = !this.wallBanner && this.path.endsWith("_banner")
            ? var2.getNamespace() + ":" + this.path.substring(0, this.path.length() - "banner".length()) + "wall_banner"
            : null;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Fit {
      String text;
      float textW;
      String hint;
      float width = -1.0F;
      int generation = -1;

      private Fit() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class ListState {
      final BlockListEditor.Row[] pickable;
      final Reference2ObjectOpenHashMap<BlockColorListSetting.BlockEntry, BlockListEditor.Row> byEntry = new Reference2ObjectOpenHashMap<>();
      BlockListEditor.Row[] chosen = new BlockListEditor.Row[0];
      int chosenCount;
      long chosenRevision = -1L;
      int labelsFor = -1;
      String countPill;
      final int[] results;
      final long[] sortKeys;
      int resultCount;
      String searchKey;
      int searchGeneration = -1;
      final BlockListEditor.Row[] available;
      int availableCount;
      BlockListEditor.Row[] shown = new BlockListEditor.Row[0];
      long[] shownKeys = new long[0];
      int shownCount;
      long columnsRevision = -1L;
      String columnsKey;
      int columnsGeneration = -1;
      int availableLabelFor = -1;
      int shownLabelFor = -1;
      String availableLabel;
      String shownLabel;

      ListState(BlockColorListSetting blockColorListSetting) {
         List var2 = blockColorListSetting.entries();
         ArrayList var3 = new ArrayList(var2.size());

         for (BlockColorListSetting.BlockEntry var5 : var2) {
            Identifier var7 = Identifier.tryParse(var5.id());
            Block var8 = var7 == null ? null : Registries.BLOCK.getOptionalValue(var7).orElse(null);
            BlockListEditor.BlockInfo var6 = var8 == null ? null : BlockListEditor.INFOS.get(var8);
            if (var6 != null) {
               BlockListEditor.Row var10 = new BlockListEditor.Row(var5, var6);
               var3.add(var10);
               this.byEntry.put(var5, var10);
            }
         }

         ArrayList var11 = new ArrayList(var3.size());

         for (BlockListEditor.Row var13 : var3) {
            if (var13.info.wallBannerId != null) {
               BlockColorListSetting.BlockEntry var14 = blockColorListSetting.entry(var13.info.wallBannerId);
               BlockListEditor.Row var15 = var13.follower = var14 == null ? null : this.byEntry.get(var14);
               if (var13.follower != null) {
                  var13.follower.leader = var13;
               }
            }

            if (!var13.info.wallBanner) {
               var11.add(var13);
            }
         }

         this.pickable = var11.toArray(new BlockListEditor.Row[0]);
         this.results = new int[this.pickable.length];
         this.sortKeys = new long[this.pickable.length];
         this.available = new BlockListEditor.Row[this.pickable.length];
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Row {
      final BlockColorListSetting.BlockEntry entry;
      final BlockListEditor.BlockInfo info;
      BlockListEditor.Row follower;
      BlockListEditor.Row leader;
      final BlockListEditor.Fit inlineFit = new BlockListEditor.Fit();
      final BlockListEditor.Fit pickerFit = new BlockListEditor.Fit();

      Row(BlockColorListSetting.BlockEntry blockEntry, BlockListEditor.BlockInfo blockInfo) {
         this.entry = blockEntry;
         this.info = blockInfo;
      }
   }
}
