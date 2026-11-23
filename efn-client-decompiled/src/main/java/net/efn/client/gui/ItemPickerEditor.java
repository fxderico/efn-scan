package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImString;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.setting.StringListSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.ItemEnchants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
final class ItemPickerEditor {
   private static final int INLINE_MAX_ROWS = 8;
   private static final float HEADER_H = 40.0F;
   private static final float INLINE_ROW_H = 38.0F;
   private static final float LIST_PAD = 4.0F;
   private static final float EMPTY_H = 44.0F;
   private static final float LIST_W = 820.0F;
   private static final float SINGLE_W = 560.0F;
   private static final float ENCHANT_W = 560.0F;
   private static final float PICKER_MAX_H = 760.0F;
   private static final float ENCHANT_MAX_H = 640.0F;
   private static final float PICKER_ROW_H = 42.0F;
   private static final float COLUMN_GAP = 28.0F;
   private static final float TAB_BAR_ROOM = 86.0F;
   private static final float ICON_SLOT = 32.0F;
   private static final float PRESET_ICON = 48.0F;
   private static final float PRESET_GAP = 8.0F;
   private static final int LIST_BG = EfnWidgets.withAlpha(-13226454, 0.5F);
   private static final int ROW_HOVER = 318767103;
   private static final int SCRIM = 1929379840;
   private static final String SELECT_LABEL = "Select";
   private static final String EDIT_LABEL = "Edit";
   private static final String NO_ITEMS = "No items yet";
   private static final String NO_ITEM = "No item selected";
   private static final String ANY_ENCHANTS = "Any enchantments";
   private static final String LIST_TITLE = "Select Items";
   private static final String SINGLE_TITLE = "Select Item";
   private static final String ENCHANT_TITLE = "Enchantments";
   private static final String AVAILABLE_TITLE = "All items";
   private static final String CHOSEN_TITLE = "Selected";
   private static final String NO_MATCHES = "No items match";
   private static final String NONE_CHOSEN = "Nothing selected yet";
   private static final String SEARCH_HINT = "Search items...";
   private static final String CLEAR_LABEL = "Clear";
   private static final String DONE_LABEL = "Done";
   private static final String TEXT_HINT = "text match";
   private static final String UNKNOWN_HINT = "unknown item";
   private static final String ENCHANT_HELP = "Pick what the item must have. Clashing enchantments turn off on their own.";
   private static final String NO_WORLD = "Join a world to load enchantments.";
   private static final String NO_ENCHANTS = "This item takes no enchantments.";
   private static final String FULL_LIST = "The list is full";
   private static final String OTHER_ITEMS = "Other items";
   private static final int PICKER_FLAGS = 8639;
   private static final int LEV_INSERT = 1;
   private static final int LEV_SUBSTITUTE = 8;
   private static final int LEV_DELETE = 8;
   private static final List<ItemPickerEditor.ItemInfo> ITEMS = new ArrayList<>();
   private static final Reference2ObjectOpenHashMap<Item, ItemPickerEditor.ItemInfo> BY_ITEM = new Reference2ObjectOpenHashMap();
   private static final Map<StringSetting.Preset, ItemStack> PRESET_ICONS = new HashMap<>();
   private static String language;
   private static int namesGeneration;
   private static int[] levPrev = new int[64];
   private static int[] levCur = new int[64];
   private final Reference2ObjectOpenHashMap<StringListSetting, float[]> inlineScroll = new Reference2ObjectOpenHashMap();
   private final Reference2ObjectOpenHashMap<StringSetting, ItemPickerEditor.Summary> summaries = new Reference2ObjectOpenHashMap();
   private ClientModule pickerModule;
   private StringListSetting listTarget;
   private StringSetting itemTarget;
   private String pickerSubtitle;
   private final ImString search = new ImString(64);
   private boolean focusSearch;
   private boolean searchActive;
   private boolean pickerJustOpened;
   private long pickerOpenedAt;
   private final float[] availableScroll = new float[2];
   private final float[] chosenScroll = new float[2];
   private ItemPickerEditor.ItemInfo[] available = new ItemPickerEditor.ItemInfo[0];
   private ItemPickerEditor.ItemInfo[] chosen = new ItemPickerEditor.ItemInfo[0];
   private long[] sortKeys = new long[0];
   private int availableCount;
   private int chosenCount;
   private Object columnsTarget;
   private long columnsRevision = -1L;
   private String columnsKey;
   private int columnsGeneration = -1;
   private StringSetting enchantTarget;
   private Item enchantItem;
   private String enchantSubtitle;
   private final List<ItemPickerEditor.EnchantRow> enchantRows = new ArrayList<>();
   private boolean enchantsLoaded;
   private long enchantOpenedAt;
   private boolean enchantJustOpened;
   private final float[] enchantScroll = new float[2];

   ItemPickerEditor() {
   }

   float renderList(ClientModule var1, StringListSetting var2, float var3, float var4, float var5) {
      beginFrame();
      ImDrawList var6 = ImGui.getWindowDrawList();
      ImFont var7 = ImGuiManager.getInstance().moduleFont();
      ImFont var8 = ImGuiManager.getInstance().smallFont();
      float var9 = ImGui.getWindowPosX();
      float var10 = ImGui.getWindowPosY() - ImGui.getScrollY();
      List var11 = var2.get();
      ImGui.setCursorPos(var3, var5 + EfnWidgets.s(10.0F));
      ImGui.textUnformatted(var2.name());
      float var12 = var10 + var5 + EfnWidgets.s(10.0F) + ImGui.getTextLineHeight() * 0.5F;
      countPill(var6, var8, ImGui.getItemRectMaxX() + EfnWidgets.s(8.0F), var12, Integer.toString(var11.size()));
      if (headerChip("##pick_items", "Select", var9 + var3 + var4, var12, var7)) {
         this.openList(var1, var2);
      }

      float var13 = var5 + EfnWidgets.s(40.0F);
      float var14 = EfnWidgets.s(38.0F);
      int var15 = var11.size();
      float var16 = var15 == 0 ? EfnWidgets.s(44.0F) : Math.min(var15, 8) * var14 + EfnWidgets.s(4.0F) * 2.0F;
      float var17 = var9 + var3;
      float var18 = var10 + var13;
      var6.addRectFilled(var17, var18, var17 + var4, var18 + var16, EfnWidgets.c(LIST_BG), EfnWidgets.s(10.0F));
      String var19 = null;
      if (var15 == 0) {
         EfnWidgets.text(var6, var7, var17 + EfnWidgets.s(14.0F), var18 + (var16 - EfnWidgets.lineHeight(var7)) * 0.5F, -4542808, "No items yet");
      } else if (var15 <= 8) {
         var19 = this.drawEntries(var6, var11, var3, var13 + EfnWidgets.s(4.0F), var4, var14, 0, var15, var7, var8);
      } else {
         ImGui.setCursorPos(var3, var13 + EfnWidgets.s(4.0F));
         if (ImGui.beginChild("##chosen_items", var4, var16 - EfnWidgets.s(4.0F) * 2.0F, false, 24)) {
            ImGui.setCursorPos(0.0F, 0.0F);
            ImGui.dummy(1.0F, var15 * var14);
            float var20 = ImGui.getScrollY();
            int var21 = Math.max(0, (int)(var20 / var14));
            int var22 = Math.min(var15, (int)Math.ceil((var20 + ImGui.getWindowHeight()) / var14));
            var19 = this.drawEntries(ImGui.getWindowDrawList(), var11, 0.0F, 0.0F, var4, var14, var21, var22, var7, var8);
            ClickGuiRenderer.smoothScroll((float[])this.inlineScroll.computeIfAbsent(var2, var0 -> new float[2]), frameDelta());
         }

         ImGui.endChild();
      }

      if (var19 != null) {
         var2.removeValue(var19);
      }

      return EfnWidgets.s(40.0F) + var16 + EfnWidgets.s(12.0F);
   }

   float renderItem(ClientModule var1, StringSetting var2, float var3, float var4, float var5) {
      beginFrame();
      ImDrawList var7 = ImGui.getWindowDrawList();
      ImFont var8 = ImGuiManager.getInstance().moduleFont();
      ImFont var9 = ImGuiManager.getInstance().smallFont();
      float var10 = ImGui.getWindowPosX();
      float var11 = ImGui.getWindowPosY() - ImGui.getScrollY();
      ImGui.setCursorPos(var3, var5 + EfnWidgets.s(10.0F));
      ImGui.textUnformatted(var2.name());
      float var12 = var11 + var5 + EfnWidgets.s(10.0F) + ImGui.getTextLineHeight() * 0.5F;
      boolean var13 = headerChip("##pick_item", "Select", var10 + var3 + var4, var12, var8);
      float var14 = var5 + EfnWidgets.s(40.0F);
      float var15 = EfnWidgets.s(38.0F) + EfnWidgets.s(4.0F) * 2.0F;
      float var16 = var10 + var3;
      float var17 = var11 + var14;
      float var18 = var17 + var15 * 0.5F;
      float var19 = EfnWidgets.lineHeight(var8);
      float var20 = EfnWidgets.lineHeight(var9);
      String var21 = var2.get().trim();
      float var22 = EfnWidgets.s(22.0F);
      float var23 = var16 + var4 - EfnWidgets.s(10.0F);
      boolean var24 = false;
      boolean var25 = false;
      if (!var21.isEmpty()) {
         ImGui.setCursorScreenPos(var23 - var22, var18 - var22 * 0.5F);
         var24 = ImGui.invisibleButton("##clear_item", var22, var22);
         var25 = ImGui.isItemHovered();
      }

      float var26 = var21.isEmpty() ? var4 : Math.max(1.0F, var4 - var22 - EfnWidgets.s(14.0F));
      ImGui.setCursorScreenPos(var16, var17);
      if (ImGui.invisibleButton("##item_box", var26, var15)) {
         var13 = true;
      }

      var7.addRectFilled(
         var16, var17, var16 + var4, var17 + var15, EfnWidgets.c(ImGui.isItemHovered() ? EfnWidgets.withAlpha(-13226454, 0.8F) : LIST_BG), EfnWidgets.s(10.0F)
      );
      if (var21.isEmpty()) {
         EfnWidgets.text(var7, var8, var16 + EfnWidgets.s(14.0F), var18 - var19 * 0.5F, -4542808, "No item selected");
      } else {
         if (var25) {
            var7.addCircleFilled(var23 - var22 * 0.5F, var18, var22 * 0.5F, EfnWidgets.c(-12240588), 20);
         }

         EfnWidgets.cross(var7, var23 - var22 * 0.5F, var18, EfnWidgets.s(3.5F), var25 ? -10856974 : -4542808);
         float var27 = var23 - var22 - EfnWidgets.s(8.0F);
         Item var28 = ItemEnchants.resolve(var21);
         float var29 = var16 + EfnWidgets.s(8.0F);
         if (var28 != null) {
            ItemPickerEditor.ItemInfo var30 = info(var28);
            float var31 = ItemIcons.pixels(EfnWidgets.s(32.0F));
            ItemIcons.draw(var7, var28, var29, var18 - var31 * 0.5F, var31, 1.0F);
            float var32 = var29 + var31 + EfnWidgets.s(10.0F);
            drawLabel(var7, var8, var9, var32, var18, var27 - var32, var30.name, var30.stored, -461582, var19, var20);
         } else {
            drawLabel(var7, var8, var9, var29 + EfnWidgets.s(6.0F), var18, var27 - var29 - EfnWidgets.s(6.0F), var21, "unknown item", -10856974, var19, var20);
         }
      }

      if (var24) {
         var2.set("");
         if (var2.enchantSetting() != null) {
            var2.enchantSetting().set("");
         }
      } else if (var13) {
         this.openItem(var1, var2);
      }

      return EfnWidgets.s(40.0F) + var15 + EfnWidgets.s(12.0F);
   }

   float renderPresets(ClientModule var1, StringSetting var2, float var3, float var4, float var5) {
      beginFrame();
      ImDrawList var6 = ImGui.getWindowDrawList();
      ImFont var7 = ImGuiManager.getInstance().moduleFont();
      ImFont var8 = ImGuiManager.getInstance().smallFont();
      float var9 = ImGui.getWindowPosX();
      float var10 = ImGui.getWindowPosY() - ImGui.getScrollY();
      ImGui.setCursorPos(var3, var5 + EfnWidgets.s(10.0F));
      ImGui.textUnformatted(var2.name());
      List var11 = var2.presets();
      String var12 = var2.get().trim();
      String var13 = var12.toLowerCase(Locale.ROOT).replace(' ', '_');
      StringSetting.Preset var14 = null;

      for (StringSetting.Preset var16 : var11) {
         if (var16.value().equalsIgnoreCase(var13)) {
            var14 = var16;
         }
      }

      int var38 = GuiPalette.accent();
      float var39 = EfnWidgets.s(8.0F);
      float var17 = EfnWidgets.lineHeight(var7);
      float var18 = EfnWidgets.lineHeight(var8);
      float var19 = ItemIcons.pixels(EfnWidgets.s(48.0F));
      float var20 = (var4 - var39 * (var11.size() - 1)) / var11.size();
      float var21 = var19 + EfnWidgets.s(30.0F) + var18;
      float var22 = var10 + var5 + EfnWidgets.s(40.0F);

      for (int var23 = 0; var23 < var11.size(); var23++) {
         StringSetting.Preset var24 = (StringSetting.Preset)var11.get(var23);
         boolean var25 = var24 == var14;
         float var26 = var9 + var3 + var23 * (var20 + var39);
         ImGui.setCursorScreenPos(var26, var22);
         ImGui.pushID(var23);
         boolean var27 = ImGui.invisibleButton("##preset", var20, var21);
         boolean var28 = ImGui.isItemHovered();
         ImGui.popID();
         presetTile(var6, var26, var22, var20, var21, var25, var28, var38);
         ItemIcons.draw(var6, presetIcon(var24), var26 + (var20 - var19) * 0.5F, var22 + EfnWidgets.s(12.0F), var19, 1.0F);
         String var29 = EfnWidgets.ellipsize(var8, var24.label(), var20 - EfnWidgets.s(10.0F));
         float var30 = EfnWidgets.textWidth(var8, var29);
         EfnWidgets.text(var6, var8, var26 + (var20 - var30) * 0.5F, var22 + var21 - EfnWidgets.s(12.0F) - var18, !var25 && !var28 ? -2042416 : -461582, var29);
         if (var28 && !var29.equals(var24.label())) {
            ClickGuiRenderer.showTooltip(var24.label());
         }

         if (var27) {
            var2.set(var24.value());
         }
      }

      boolean var40 = var14 == null && !var12.isEmpty();
      Item var41 = var40 ? ItemEnchants.resolve(var12) : null;
      float var42 = var22 + var21 + var39;
      float var43 = EfnWidgets.s(38.0F) + EfnWidgets.s(4.0F) * 2.0F;
      float var44 = var9 + var3;
      float var45 = var42 + var43 * 0.5F;
      ImGui.setCursorScreenPos(var44, var42);
      boolean var46 = ImGui.invisibleButton("##other_items", var4, var43);
      boolean var47 = ImGui.isItemHovered();
      presetTile(var6, var44, var42, var4, var43, var40, var47, var38);
      float var31 = ItemIcons.pixels(EfnWidgets.s(32.0F));
      float var32 = var44 + EfnWidgets.s(10.0F);
      float var33 = var32 + var31 + EfnWidgets.s(10.0F);
      float var34 = var44 + var4 - EfnWidgets.s(14.0F) - var33;
      if (var41 != null) {
         ItemIcons.draw(var6, var41, var32, var45 - var31 * 0.5F, var31, 1.0F);
         drawLabel(var6, var7, var8, var33, var45, var34, info(var41).name, "Other items", -461582, var17, var18);
      } else {
         float var35 = var32 + var31 * 0.5F;
         float var36 = var31 * 0.34F;
         int var37 = EfnWidgets.c(var47 ? -461582 : -4542808);
         var6.addCircle(var35, var45, var36, var37, 24, EfnWidgets.s(1.5F));
         var6.addLine(var35 - var36 * 0.5F, var45, var35 + var36 * 0.5F, var45, var37, EfnWidgets.s(1.5F));
         var6.addLine(var35, var45 - var36 * 0.5F, var35, var45 + var36 * 0.5F, var37, EfnWidgets.s(1.5F));
         drawLabel(
            var6,
            var7,
            var8,
            var33,
            var45,
            var34,
            var40 ? var12 : "Other items",
            var40 ? "Other items" : null,
            !var47 && !var40 ? -2042416 : -461582,
            var17,
            var18
         );
      }

      if (var46) {
         this.openItem(var1, var2);
      }

      return EfnWidgets.s(40.0F) + var21 + var39 + var43 + EfnWidgets.s(12.0F);
   }

   private static void presetTile(ImDrawList var0, float var1, float var2, float var3, float var4, boolean var5, boolean var6, int var7) {
      int var8 = var5 ? EfnWidgets.withAlpha(var7, 0.18F) : (var6 ? EfnWidgets.withAlpha(-13226454, 0.8F) : LIST_BG);
      var0.addRectFilled(var1, var2, var1 + var3, var2 + var4, EfnWidgets.c(var8), EfnWidgets.s(10.0F));
      if (var5) {
         var0.addRect(var1, var2, var1 + var3, var2 + var4, EfnWidgets.c(EfnWidgets.withAlpha(var7, 0.9F)), EfnWidgets.s(10.0F), 0, EfnWidgets.s(1.5F));
      }
   }

   private static ItemStack presetIcon(StringSetting.Preset var0) {
      ItemStack var1 = PRESET_ICONS.get(var0);
      if (var1 != null) {
         return var1;
      } else {
         Item var2 = ItemEnchants.resolve(var0.icon());
         var1 = new ItemStack(var2 == null ? Items.field_8077 : var2);
         if (var0.glint()) {
            var1.method_57379(DataComponentTypes.field_49641, true);
         }

         PRESET_ICONS.put(var0, var1);
         return var1;
      }
   }

   float renderEnchants(ClientModule var1, StringSetting var2, float var3, float var4, float var5) {
      beginFrame();
      ImDrawList var7 = ImGui.getWindowDrawList();
      ImFont var8 = ImGuiManager.getInstance().moduleFont();
      float var9 = ImGui.getWindowPosX();
      float var10 = ImGui.getWindowPosY() - ImGui.getScrollY();
      ImGui.setCursorPos(var3, var5 + EfnWidgets.s(10.0F));
      ImGui.textUnformatted(var2.name());
      float var11 = var10 + var5 + EfnWidgets.s(10.0F) + ImGui.getTextLineHeight() * 0.5F;
      boolean var12 = headerChip("##edit_enchants", "Edit", var9 + var3 + var4, var11, var8);
      float var13 = var5 + EfnWidgets.s(40.0F);
      float var14 = EfnWidgets.s(38.0F) + EfnWidgets.s(4.0F) * 2.0F;
      float var15 = var9 + var3;
      float var16 = var10 + var13;
      ImGui.setCursorScreenPos(var15, var16);
      if (ImGui.invisibleButton("##enchant_box", var4, var14)) {
         var12 = true;
      }

      boolean var6;
      var7.addRectFilled(
         var15,
         var16,
         var15 + var4,
         var16 + var14,
         EfnWidgets.c((var6 = ImGui.isItemHovered()) ? EfnWidgets.withAlpha(-13226454, 0.8F) : LIST_BG),
         EfnWidgets.s(10.0F)
      );
      String var17 = this.summary(var2);
      float var18 = EfnWidgets.lineHeight(var8);
      float var19 = var15 + EfnWidgets.s(14.0F);
      float var20 = var4 - EfnWidgets.s(28.0F);
      if (var17.isEmpty()) {
         EfnWidgets.text(var7, var8, var19, var16 + (var14 - var18) * 0.5F, -4542808, "Any enchantments");
      } else {
         String var21 = EfnWidgets.ellipsize(var8, var17, var20);
         EfnWidgets.text(var7, var8, var19, var16 + (var14 - var18) * 0.5F, -461582, var21);
         if (var6 && !var21.equals(var17)) {
            ClickGuiRenderer.showTooltip(var17);
         }
      }

      if (var12) {
         this.openEnchants(var1, var2);
      }

      return EfnWidgets.s(40.0F) + var14 + EfnWidgets.s(12.0F);
   }

   void renderWindows() {
      if (this.listTarget != null || this.itemTarget != null) {
         this.renderPicker();
      } else if (this.enchantTarget != null) {
         this.renderEnchantWindow();
      }
   }

   void close() {
      this.closePicker();
      this.closeEnchants();
   }

   boolean closeWindow() {
      if (this.listTarget != null || this.itemTarget != null) {
         this.closePicker();
         return true;
      } else if (this.enchantTarget != null) {
         this.closeEnchants();
         return true;
      } else {
         return false;
      }
   }

   private String drawEntries(ImDrawList var1, List<String> var2, float var3, float var4, float var5, float var6, int var7, int var8, ImFont var9, ImFont var10) {
      boolean var11 = ImGui.isWindowHovered();
      float var12 = ItemIcons.pixels(EfnWidgets.s(32.0F));
      float var13 = EfnWidgets.s(22.0F);
      float var14 = EfnWidgets.lineHeight(var9);
      float var15 = EfnWidgets.lineHeight(var10);
      String var16 = null;

      for (int var17 = var7; var17 < var8; var17++) {
         String var19 = (String)var2.get(var17);
         ImGui.setCursorPos(var3, var4 + var17 * var6);
         float var20 = ImGui.getCursorScreenPosX();
         float var21 = ImGui.getCursorScreenPosY();
         float var22 = var21 + var6 * 0.5F;
         if (var11 && ImGui.isMouseHoveringRect(var20, var21, var20 + var5, var21 + var6)) {
            var1.addRectFilled(
               var20 + EfnWidgets.s(4.0F),
               var21 + EfnWidgets.s(1.0F),
               var20 + var5 - EfnWidgets.s(4.0F),
               var21 + var6 - EfnWidgets.s(1.0F),
               EfnWidgets.c(318767103),
               EfnWidgets.s(7.0F)
            );
         }

         ImGui.pushID(var17);
         float var23 = var20 + var5 - EfnWidgets.s(10.0F);
         ImGui.setCursorScreenPos(var23 - var13, var22 - var13 * 0.5F);
         if (ImGui.invisibleButton("##remove", var13, var13)) {
            var16 = var19;
         }

         boolean var18;
         if (var18 = ImGui.isItemHovered()) {
            var1.addCircleFilled(var23 - var13 * 0.5F, var22, var13 * 0.5F, EfnWidgets.c(-12240588), 20);
         }

         EfnWidgets.cross(var1, var23 - var13 * 0.5F, var22, EfnWidgets.s(3.5F), var18 ? -10856974 : -4542808);
         ImGui.popID();
         var23 -= var13 + EfnWidgets.s(8.0F);
         float var24 = var20 + EfnWidgets.s(8.0F);
         Item var25 = ItemEnchants.resolve(var19);
         if (var25 != null) {
            ItemPickerEditor.ItemInfo var26 = info(var25);
            ItemIcons.draw(var1, var25, var24, var22 - var12 * 0.5F, var12, 1.0F);
            float var27 = var24 + var12 + EfnWidgets.s(10.0F);
            drawLabel(var1, var9, var10, var27, var22, var23 - var27, var26.name, var26.stored, -461582, var14, var15);
         } else {
            float var29 = var24 + EfnWidgets.s(6.0F);
            drawLabel(var1, var9, var10, var29, var22, var23 - var29, var19, "text match", -2042416, var14, var15);
         }
      }

      return var16;
   }

   private static void drawLabel(
      ImDrawList var0, ImFont var1, ImFont var2, float var3, float var4, float var5, String var6, String var7, int var8, float var9, float var10
   ) {
      if (!(var5 <= 0.0F)) {
         String var11 = EfnWidgets.ellipsize(var1, var6, var5);
         EfnWidgets.text(var0, var1, var3, var4 - var9 * 0.5F, var8, var11);
         float var12 = EfnWidgets.textWidth(var1, var11);
         float var13 = var5 - var12 - EfnWidgets.s(6.0F);
         if (var7 != null && var13 >= EfnWidgets.s(40.0F)) {
            EfnWidgets.text(var0, var2, var3 + var12 + EfnWidgets.s(6.0F), var4 - var10 * 0.5F, -4542808, EfnWidgets.ellipsize(var2, var7, var13));
         }
      }
   }

   private void openList(ClientModule var1, StringListSetting var2) {
      this.closeEnchants();
      this.listTarget = var2;
      this.itemTarget = null;
      this.startPicker(var1, var2.name());
   }

   private void openItem(ClientModule var1, StringSetting var2) {
      this.closeEnchants();
      this.itemTarget = var2;
      this.listTarget = null;
      this.startPicker(var1, var2.name());
   }

   private void startPicker(ClientModule var1, String var2) {
      this.pickerModule = var1;
      this.pickerSubtitle = var1.name() + " / " + var2;
      this.search.set("");
      this.focusSearch = true;
      this.searchActive = false;
      this.pickerOpenedAt = System.nanoTime();
      this.pickerJustOpened = true;
      this.chosenScroll[1] = 0.0F;
      this.chosenScroll[0] = 0.0F;
      this.availableScroll[1] = 0.0F;
      this.availableScroll[0] = 0.0F;
      this.columnsTarget = null;
   }

   private void closePicker() {
      this.listTarget = null;
      this.itemTarget = null;
      this.columnsTarget = null;
   }

   private void renderPicker() {
      beginFrame();
      boolean var1 = this.listTarget != null;
      float var2 = ImGui.getIO().getDisplaySizeX();
      float var3 = ImGui.getIO().getDisplaySizeY();
      float var4 = EfnWidgets.s(16.0F);
      float var5 = Math.min(var2 - var4 * 2.0F, EfnWidgets.s(var1 ? 820.0F : 560.0F));
      float var6 = Math.min(var3 - EfnWidgets.s(86.0F) - EfnWidgets.s(32.0F), EfnWidgets.s(760.0F));
      float var7 = ClickGuiRenderer.easeOut(ClickGuiRenderer.elapsed(this.pickerOpenedAt, 200.0F));
      float var8 = Math.round((var2 - var5) * 0.5F);
      float var9 = Math.round(Math.max(var4, (var3 - EfnWidgets.s(86.0F) - var6) * 0.5F + EfnWidgets.s(8.0F)) + EfnWidgets.s(14.0F) * (1.0F - var7));
      float var10 = ClickGuiRenderer.cardRounding();
      ImGui.setNextWindowPos(0.0F, 0.0F, 1);
      ImGui.setNextWindowSize(var2, var3, 1);
      if (this.pickerJustOpened) {
         ImGui.setNextWindowFocus();
      }

      ImGui.pushStyleVar(0, Math.max(0.01F, var7));
      ImGui.pushStyleVar(2, 0.0F, 0.0F);
      ImGui.pushStyleVar(14, 0.0F, 0.0F);
      ImGui.pushStyleVar(4, 0.0F);
      boolean var11 = false;
      ItemPickerEditor.ItemInfo var12 = null;
      ItemPickerEditor.ItemInfo var13 = null;
      boolean var14 = ImGui.begin("##item_picker", 8639);
      if (this.pickerJustOpened) {
         imgui.internal.ImGui.bringWindowToDisplayFront(imgui.internal.ImGui.getCurrentWindow());
      }

      if (var14) {
         ImDrawList var16 = ImGui.getWindowDrawList();
         var16.addRectFilled(0.0F, 0.0F, var2, var3, EfnWidgets.c(1929379840));
         shadow(var16, var8, var9, var5, var6, var10);
         var16.addRectFilled(var8, var9, var8 + var5, var9 + var6, EfnWidgets.c(GuiPalette.abgr(ThemeStore.get().cards.color.argbWithAlpha(255))), var10);
         var16.addRect(var8, var9, var8 + var5, var9 + var6, EfnWidgets.c(318767103), var10, 0, 1.0F);
         ImFont var17 = ImGuiManager.getInstance().moduleFont();
         ImFont var18 = ImGuiManager.getInstance().smallFont();
         float var19 = EfnWidgets.s(24.0F);
         float var20 = var9 + EfnWidgets.s(22.0F);
         float var21 = EfnWidgets.s(28.0F);
         ClickGuiRenderer.drawCardTitle(var1 ? "Select Items" : "Select Item", var8 + var19, var20 + var21 * 0.5F);
         ImGui.setCursorPos(var8 + var5 - var19 - var21, var20);
         if (EfnWidgets.iconButton("##close", var21)) {
            var11 = true;
         }

         EfnWidgets.cross(var16, var8 + var5 - var19 - var21 * 0.5F, var20 + var21 * 0.5F, EfnWidgets.s(4.5F), -461582);
         if (var1
            && chip(
               "##clear_items",
               "Clear",
               this.listTarget.get().isEmpty(),
               var8 + var5 - var19 - var21 - EfnWidgets.s(10.0F),
               var20 + var21 * 0.5F,
               var18,
               "Takes every item off the list"
            )) {
            this.listTarget.clear();
         }

         float var22 = var20 + var21 + EfnWidgets.s(2.0F);
         EfnWidgets.text(var16, var18, var8 + var19, var22, -4542808, this.pickerSubtitle);
         ImGui.setCursorPos(var8 + var19, var22 + EfnWidgets.lineHeight(var18) + EfnWidgets.s(14.0F));
         ImGui.pushStyleVar(11, EfnWidgets.s(12.0F), EfnWidgets.s(8.0F));
         ImGui.setNextItemWidth(var5 - var19 * 2.0F);
         if (this.focusSearch) {
            ImGui.setKeyboardFocusHere();
            this.focusSearch = false;
         }

         boolean var23 = this.searchActive && (ImGui.isKeyPressed(525, false) || ImGui.isKeyPressed(627, false));
         ImGui.inputTextWithHint("##item_search", "Search items...", this.search);
         this.searchActive = ImGui.isItemActive();
         ImGui.popStyleVar();
         float var24 = ImGui.getItemRectMaxY() + EfnWidgets.s(18.0F);
         String var25 = this.columnsKey;
         this.refreshColumns();
         if (var25 != null && !var25.equals(this.columnsKey)) {
            this.chosenScroll[1] = 0.0F;
            this.chosenScroll[0] = 0.0F;
            this.availableScroll[1] = 0.0F;
            this.availableScroll[0] = 0.0F;
         }

         if (var23 && !this.search.get().isBlank() && this.availableCount > 0) {
            var12 = this.available[0];
            this.focusSearch = var1;
         }

         float var26 = EfnWidgets.s(28.0F);
         float var27 = var24 + var26 + EfnWidgets.s(10.0F);
         float var28 = var9 + var6 - EfnWidgets.s(18.0F);
         float var29 = Math.max(1.0F, var28 - var27);
         String var15 = this.search.get().isBlank() ? "Nothing selected yet" : "No items match";
         if (var1) {
            float var31 = (var5 - var19 * 2.0F - EfnWidgets.s(28.0F)) * 0.5F;
            float var32 = var8 + var19;
            float var33 = var32 + var31 + EfnWidgets.s(28.0F);
            columnHeader(
               var16,
               var17,
               var18,
               var32,
               var24,
               var26,
               var31,
               "All items",
               this.listTarget.canAdd() ? Integer.toString(this.availableCount) : "The list is full"
            );
            columnHeader(var16, var17, var18, var33, var24, var26, var31, "Selected", Integer.toString(this.chosenCount));
            float var34 = Math.round(var32 + var31 + EfnWidgets.s(28.0F) * 0.5F);
            var16.addLine(var34, var24, var34, var28, EfnWidgets.c(268435455), 1.0F);
            ItemPickerEditor.ItemInfo var35 = this.column(
               "##available", this.available, this.availableCount, true, null, var32, var27, var31, var29, this.availableScroll, "No items match", var17, var18
            );
            ItemPickerEditor.ItemInfo var36 = this.column(
               "##chosen", this.chosen, this.chosenCount, false, null, var33, var27, var31, var29, this.chosenScroll, var15, var17, var18
            );
            if (var35 != null) {
               var12 = var35;
            }

            if (var36 != null) {
               var13 = var36;
            }
         } else {
            float var37 = var5 - var19 * 2.0F;
            columnHeader(var16, var17, var18, var8 + var19, var24, var26, var37, "All items", Integer.toString(this.availableCount));
            Item var38 = ItemEnchants.resolve(this.itemTarget.get());
            ItemPickerEditor.ItemInfo var39 = this.column(
               "##single",
               this.available,
               this.availableCount,
               true,
               var38,
               var8 + var19,
               var27,
               var37,
               var29,
               this.availableScroll,
               "No items match",
               var17,
               var18
            );
            if (var39 != null) {
               var12 = var39;
            }
         }

         if (!this.pickerJustOpened && ImGui.isMouseClicked(0) && ImGui.isWindowHovered() && !ImGui.isMouseHoveringRect(var8, var9, var8 + var5, var9 + var6)) {
            var11 = true;
         }
      }

      ImGui.end();
      ImGui.popStyleVar(4);
      this.pickerJustOpened = false;
      if (var13 != null && this.listTarget != null) {
         removeFromList(this.listTarget, var13.item);
      }

      if (var12 != null) {
         if (this.listTarget != null) {
            this.listTarget.addValue(var12.stored);
         } else if (this.itemTarget != null) {
            this.pickItem(var12);
            return;
         }
      }

      if (var11) {
         this.closePicker();
      }
   }

   private void pickItem(ItemPickerEditor.ItemInfo var1) {
      StringSetting var2 = this.itemTarget;
      ClientModule var3 = this.pickerModule;
      Item var4 = ItemEnchants.resolve(var2.get());
      var2.set(var1.stored);
      this.closePicker();
      StringSetting var5 = var2.enchantSetting();
      if (var5 != null) {
         if (var4 != var1.item) {
            var5.set("");
         }

         if (var3 != null && ItemEnchants.supports(var1.item)) {
            this.openEnchants(var3, var5);
         }
      }
   }

   private static void removeFromList(StringListSetting var0, Item var1) {
      for (String var3 : var0.get()) {
         if (ItemEnchants.resolve(var3) == var1) {
            var0.removeValue(var3);
            return;
         }
      }
   }

   private ItemPickerEditor.ItemInfo column(
      String var1,
      ItemPickerEditor.ItemInfo[] var2,
      int var3,
      boolean var4,
      Item var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float[] var10,
      String var11,
      ImFont var12,
      ImFont var13
   ) {
      ItemPickerEditor.ItemInfo var14 = null;
      ImGui.setCursorPos(var6, var7);
      if (ImGui.beginChild(var1, var8, var9, false, 24)) {
         float var15 = EfnWidgets.s(42.0F);
         float var16 = var8 - EfnWidgets.s(6.0F);
         float var17 = var15 - EfnWidgets.s(2.0F);
         ImGui.setCursorPos(0.0F, 0.0F);
         ImGui.dummy(1.0F, var3 * var15 + EfnWidgets.s(6.0F));
         ImDrawList var18 = ImGui.getWindowDrawList();
         if (var3 == 0) {
            EfnWidgets.text(var18, var12, ImGui.getWindowPosX() + EfnWidgets.s(8.0F), ImGui.getWindowPosY() + EfnWidgets.s(12.0F), -4542808, var11);
         } else {
            float var19 = EfnWidgets.lineHeight(var12);
            float var20 = EfnWidgets.lineHeight(var13);
            float var21 = ItemIcons.pixels(EfnWidgets.s(32.0F));
            float var22 = EfnWidgets.s(11.0F);
            float var23 = ImGui.getScrollY();
            int var24 = Math.max(0, (int)(var23 / var15));
            int var25 = Math.min(var3, (int)Math.ceil((var23 + ImGui.getWindowHeight()) / var15));
            int var26 = GuiPalette.accent();

            for (int var27 = var24; var27 < var25; var27++) {
               ItemPickerEditor.ItemInfo var29 = var2[var27];
               ImGui.setCursorPos(0.0F, var27 * var15);
               float var30 = ImGui.getCursorScreenPosX();
               float var31 = ImGui.getCursorScreenPosY();
               float var32 = var31 + var17 * 0.5F;
               ImGui.pushID(var29.rawId);
               if (ImGui.invisibleButton("##row", var16, var17)) {
                  var14 = var29;
               }

               boolean var33 = ImGui.isItemHovered();
               ImGui.popID();
               boolean var28 = var5 != null && var29.item == var5;
               if (var28) {
                  var18.addRectFilled(var30, var31, var30 + var16, var31 + var17, EfnWidgets.c(EfnWidgets.withAlpha(var26, 0.18F)), EfnWidgets.s(8.0F));
               } else if (var33) {
                  var18.addRectFilled(var30, var31, var30 + var16, var31 + var17, EfnWidgets.c(318767103), EfnWidgets.s(8.0F));
               }

               float var35 = var30 + EfnWidgets.s(6.0F);
               ItemIcons.draw(var18, var29.item, var35, var32 - var21 * 0.5F, var21, 1.0F);
               float var36 = var30 + var16 - EfnWidgets.s(8.0F) - var22;
               float var37 = var35 + var21 + EfnWidgets.s(12.0F);
               drawLabel(
                  var18,
                  var12,
                  var13,
                  var37,
                  var32,
                  var36 - var22 - EfnWidgets.s(10.0F) - var37,
                  var29.name,
                  var29.stored,
                  !var33 && !var28 ? -2042416 : -461582,
                  var19,
                  var20
               );
               if (var28) {
                  var18.addCircleFilled(var36, var32, var22, EfnWidgets.c(var26), 24);
                  EfnWidgets.check(var18, var36, var32, var22 * 0.5F, -1);
               } else {
                  int var38 = !var33 ? -13226454 : (var4 ? var26 : -10856974);
                  int var39 = var33 ? -1 : -4542808;
                  var18.addCircleFilled(var36, var32, var22, EfnWidgets.c(var38), 24);
                  var18.addLine(var36 - var22 * 0.45F, var32, var36 + var22 * 0.45F, var32, EfnWidgets.c(var39), EfnWidgets.s(1.6F));
                  if (var4) {
                     var18.addLine(var36, var32 - var22 * 0.45F, var36, var32 + var22 * 0.45F, EfnWidgets.c(var39), EfnWidgets.s(1.6F));
                  }
               }
            }
         }

         ClickGuiRenderer.smoothScroll(var10, frameDelta());
      }

      ImGui.endChild();
      return var14;
   }

   private void refreshColumns() {
      Object var1 = this.listTarget != null ? this.listTarget : this.itemTarget;
      long var2 = this.listTarget != null ? this.listTarget.revision() : this.itemTarget.revision();
      String var4 = this.search.get();
      if (var1 != this.columnsTarget || var2 != this.columnsRevision || !var4.equals(this.columnsKey) || this.columnsGeneration != namesGeneration) {
         this.columnsTarget = var1;
         this.columnsRevision = var2;
         this.columnsKey = var4;
         this.columnsGeneration = namesGeneration;
         ReferenceOpenHashSet var5 = new ReferenceOpenHashSet();
         if (this.listTarget != null) {
            for (String var7 : this.listTarget.get()) {
               Item var8 = ItemEnchants.resolve(var7);
               if (var8 != null) {
                  var5.add(var8);
               }
            }
         }

         String var15 = var4.trim().toLowerCase(Locale.ROOT);
         String[] var16 = var15.split(" ");
         int var17 = ITEMS.size();
         if (this.available.length < var17) {
            this.available = new ItemPickerEditor.ItemInfo[var17];
            this.chosen = new ItemPickerEditor.ItemInfo[var17];
            this.sortKeys = new long[var17];
         }

         int var9 = 0;

         for (int var10 = 0; var10 < var17; var10++) {
            ItemPickerEditor.ItemInfo var11 = ITEMS.get(var10);
            if (!var5.contains(var11.item)) {
               if (var15.isEmpty()) {
                  this.sortKeys[var9++] = var10;
               } else {
                  int var12 = matchDistance(var11, var15, var16);
                  if (var12 >= 0) {
                     this.sortKeys[var9++] = (long)var12 << 32 | var10;
                  }
               }
            }
         }

         Arrays.sort(this.sortKeys, 0, var9);

         for (int var18 = 0; var18 < var9; var18++) {
            this.available[var18] = ITEMS.get((int)this.sortKeys[var18]);
         }

         this.availableCount = var9;
         int var19 = 0;
         if (this.listTarget != null) {
            for (String var21 : this.listTarget.get()) {
               Item var13 = ItemEnchants.resolve(var21);
               if (var13 != null) {
                  ItemPickerEditor.ItemInfo var14 = info(var13);
                  if (var15.isEmpty() || matchDistance(var14, var15, var16) >= 0) {
                     this.chosen[var19++] = var14;
                  }
               }
            }
         }

         this.chosenCount = var19;
      }
   }

   private void openEnchants(ClientModule var1, StringSetting var2) {
      this.closePicker();
      this.enchantTarget = var2;
      this.enchantItem = var2.enchantItem() == null ? null : ItemEnchants.resolve(var2.enchantItem().get());
      String var3 = this.enchantItem == null ? "No item" : info(this.enchantItem).name;
      this.enchantSubtitle = var1.name() + " / " + var3;
      this.loadEnchantRows();
      this.enchantOpenedAt = System.nanoTime();
      this.enchantJustOpened = true;
      this.enchantScroll[1] = 0.0F;
      this.enchantScroll[0] = 0.0F;
   }

   private void closeEnchants() {
      this.enchantTarget = null;
      this.enchantItem = null;
      this.enchantRows.clear();
   }

   private void loadEnchantRows() {
      this.enchantRows.clear();
      boolean var1 = this.enchantsLoaded = MinecraftClient.method_1551().field_1687 != null;
      if (this.enchantsLoaded && this.enchantItem != null) {
         Map var2 = ItemEnchants.parse(this.enchantTarget.get());

         for (RegistryEntry var4 : ItemEnchants.applicable(this.enchantItem)) {
            ItemPickerEditor.EnchantRow var5 = new ItemPickerEditor.EnchantRow(var4);
            Integer var6 = (Integer)var2.get(var5.key);
            if (var6 == null) {
               var6 = (Integer)var2.get(ItemEnchants.key(var5.name));
            }

            if (var6 != null) {
               var5.on = true;
               var5.level = Math.max(1, Math.min(var5.max, var6));
            }

            this.enchantRows.add(var5);
         }
      }
   }

   private void writeEnchants() {
      ArrayList var1 = new ArrayList();

      for (ItemPickerEditor.EnchantRow var3 : this.enchantRows) {
         if (var3.on) {
            var1.add(var3.max > 1 ? var3.path + " " + var3.level : var3.path);
         }
      }

      this.enchantTarget.set(String.join(", ", var1));
   }

   private void toggle(ItemPickerEditor.EnchantRow var1) {
      boolean var2 = var1.on = !var1.on;
      if (var1.on) {
         for (ItemPickerEditor.EnchantRow var4 : this.enchantRows) {
            if (var4 != var1 && var4.on && !Enchantment.method_60033(var1.holder, var4.holder)) {
               var4.on = false;
            }
         }
      }

      this.writeEnchants();
   }

   private void renderEnchantWindow() {
      beginFrame();
      float var1 = ImGui.getIO().getDisplaySizeX();
      float var2 = ImGui.getIO().getDisplaySizeY();
      float var3 = EfnWidgets.s(16.0F);
      float var4 = Math.min(var1 - var3 * 2.0F, EfnWidgets.s(560.0F));
      float var5 = Math.min(var2 - EfnWidgets.s(86.0F) - EfnWidgets.s(32.0F), EfnWidgets.s(640.0F));
      float var6 = ClickGuiRenderer.easeOut(ClickGuiRenderer.elapsed(this.enchantOpenedAt, 200.0F));
      float var7 = Math.round((var1 - var4) * 0.5F);
      float var8 = Math.round(Math.max(var3, (var2 - EfnWidgets.s(86.0F) - var5) * 0.5F + EfnWidgets.s(8.0F)) + EfnWidgets.s(14.0F) * (1.0F - var6));
      float var9 = ClickGuiRenderer.cardRounding();
      ImGui.setNextWindowPos(0.0F, 0.0F, 1);
      ImGui.setNextWindowSize(var1, var2, 1);
      if (this.enchantJustOpened) {
         ImGui.setNextWindowFocus();
      }

      ImGui.pushStyleVar(0, Math.max(0.01F, var6));
      ImGui.pushStyleVar(2, 0.0F, 0.0F);
      ImGui.pushStyleVar(14, 0.0F, 0.0F);
      ImGui.pushStyleVar(4, 0.0F);
      boolean var10 = false;
      ItemPickerEditor.EnchantRow var11 = null;
      boolean var12 = ImGui.begin("##enchant_picker", 8639);
      if (this.enchantJustOpened) {
         imgui.internal.ImGui.bringWindowToDisplayFront(imgui.internal.ImGui.getCurrentWindow());
      }

      if (var12) {
         ImDrawList var13 = ImGui.getWindowDrawList();
         var13.addRectFilled(0.0F, 0.0F, var1, var2, EfnWidgets.c(1929379840));
         shadow(var13, var7, var8, var4, var5, var9);
         var13.addRectFilled(var7, var8, var7 + var4, var8 + var5, EfnWidgets.c(GuiPalette.abgr(ThemeStore.get().cards.color.argbWithAlpha(255))), var9);
         var13.addRect(var7, var8, var7 + var4, var8 + var5, EfnWidgets.c(318767103), var9, 0, 1.0F);
         ImFont var14 = ImGuiManager.getInstance().moduleFont();
         ImFont var15 = ImGuiManager.getInstance().smallFont();
         float var16 = EfnWidgets.s(24.0F);
         float var17 = var8 + EfnWidgets.s(22.0F);
         float var18 = EfnWidgets.s(28.0F);
         ClickGuiRenderer.drawCardTitle("Enchantments", var7 + var16, var17 + var18 * 0.5F);
         ImGui.setCursorPos(var7 + var4 - var16 - var18, var17);
         if (EfnWidgets.iconButton("##close", var18)) {
            var10 = true;
         }

         EfnWidgets.cross(var13, var7 + var4 - var16 - var18 * 0.5F, var17 + var18 * 0.5F, EfnWidgets.s(4.5F), -461582);
         float var19 = var17 + var18 + EfnWidgets.s(2.0F);
         float var20 = EfnWidgets.lineHeight(var15);
         float var21 = EfnWidgets.lineHeight(var14);
         float var22 = var7 + var16;
         if (this.enchantItem != null) {
            float var23 = ItemIcons.pixels(EfnWidgets.s(20.0F));
            ItemIcons.draw(var13, this.enchantItem, var22, var19 + var20 * 0.5F - var23 * 0.5F, var23, 1.0F);
            var22 += var23 + EfnWidgets.s(8.0F);
         }

         EfnWidgets.text(var13, var15, var22, var19, -4542808, this.enchantSubtitle);
         float var54 = var19 + var20 + EfnWidgets.s(8.0F);
         EfnWidgets.text(
            var13,
            var15,
            var7 + var16,
            var54,
            -4542808,
            EfnWidgets.ellipsize(var15, "Pick what the item must have. Clashing enchantments turn off on their own.", var4 - var16 * 2.0F)
         );
         float var24 = EfnWidgets.s(32.0F);
         float var25 = var54 + var20 + EfnWidgets.s(14.0F);
         float var26 = var8 + var5 - EfnWidgets.s(20.0F) - var24 - EfnWidgets.s(12.0F);
         float var27 = Math.max(1.0F, var26 - var25);
         float var28 = var4 - var16 * 2.0F;
         var13.addLine(var7 + var16, var25 - EfnWidgets.s(6.0F), var7 + var16 + var28, var25 - EfnWidgets.s(6.0F), EfnWidgets.c(268435455), 1.0F);
         if (this.enchantsLoaded && !this.enchantRows.isEmpty()) {
            ImGui.setCursorPos(var7 + var16, var25);
            if (ImGui.beginChild("##enchant_rows", var28, var27, false, 24)) {
               float var29 = EfnWidgets.s(42.0F);
               float var30 = var28 - EfnWidgets.s(6.0F);
               float var31 = var29 - EfnWidgets.s(2.0F);
               ImGui.setCursorPos(0.0F, 0.0F);
               ImGui.dummy(1.0F, this.enchantRows.size() * var29 + EfnWidgets.s(6.0F));
               ImDrawList var32 = ImGui.getWindowDrawList();
               int var33 = GuiPalette.accent();
               float var34 = ImGui.getScrollY();
               int var35 = Math.max(0, (int)(var34 / var29));
               int var36 = Math.min(this.enchantRows.size(), (int)Math.ceil((var34 + ImGui.getWindowHeight()) / var29));

               for (int var37 = var35; var37 < var36; var37++) {
                  ItemPickerEditor.EnchantRow var38 = this.enchantRows.get(var37);
                  ImGui.setCursorPos(0.0F, var37 * var29);
                  float var39 = ImGui.getCursorScreenPosX();
                  float var40 = ImGui.getCursorScreenPosY();
                  float var41 = var40 + var31 * 0.5F;
                  float var42 = var38.max > 1 ? EfnWidgets.s(112.0F) : 0.0F;
                  ImGui.pushID(var37);
                  if (ImGui.invisibleButton("##toggle", Math.max(1.0F, var30 - var42), var31)) {
                     var11 = var38;
                  }

                  boolean var43 = ImGui.isItemHovered();
                  if (var38.on) {
                     var32.addRectFilled(var39, var40, var39 + var30, var40 + var31, EfnWidgets.c(EfnWidgets.withAlpha(var33, 0.14F)), EfnWidgets.s(8.0F));
                  } else if (var43) {
                     var32.addRectFilled(var39, var40, var39 + var30, var40 + var31, EfnWidgets.c(318767103), EfnWidgets.s(8.0F));
                  }

                  float var44 = EfnWidgets.s(18.0F);
                  float var45 = var39 + EfnWidgets.s(10.0F);
                  var32.addRectFilled(var45, var41 - var44 * 0.5F, var45 + var44, var41 + var44 * 0.5F, EfnWidgets.c(-13226454), EfnWidgets.s(4.0F));
                  var32.addRect(
                     var45 + 0.5F,
                     var41 - var44 * 0.5F + 0.5F,
                     var45 + var44 - 0.5F,
                     var41 + var44 * 0.5F - 0.5F,
                     EfnWidgets.c(var38.on ? var33 : (var43 ? 1090519039 : 452984831)),
                     EfnWidgets.s(4.0F),
                     0,
                     1.0F
                  );
                  if (var38.on) {
                     EfnWidgets.check(var32, var45 + var44 * 0.5F, var41, var44 * 0.28F, var33);
                  }

                  float var46 = var45 + var44 + EfnWidgets.s(12.0F);
                  String var47 = var38.max > 1 ? var38.name + "  (max " + ItemEnchants.roman(var38.max) + ")" : var38.name;
                  EfnWidgets.text(
                     var32,
                     var14,
                     var46,
                     var41 - var21 * 0.5F,
                     !var38.on && !var43 ? -2042416 : -461582,
                     EfnWidgets.ellipsize(var14, var47, var39 + var30 - var42 - EfnWidgets.s(10.0F) - var46)
                  );
                  if (var38.max > 1) {
                     float var48 = EfnWidgets.s(28.0F);
                     float var49 = EfnWidgets.s(26.0F);
                     float var50 = var39 + var30 - EfnWidgets.s(8.0F);
                     ImGui.setCursorScreenPos(var50 - var48, var41 - var49 * 0.5F);
                     if (EfnWidgets.chipButton("##up", "+", var48, var49, false) && var38.level < var38.max) {
                        var38.level++;
                        if (!var38.on) {
                           var11 = var38;
                        } else {
                           this.writeEnchants();
                        }
                     }

                     String var51 = ItemEnchants.roman(var38.level);
                     float var52 = EfnWidgets.s(40.0F);
                     float var53 = var50 - var48 - var52;
                     EfnWidgets.text(
                        var32, var14, var53 + (var52 - EfnWidgets.textWidth(var14, var51)) * 0.5F, var41 - var21 * 0.5F, var38.on ? -461582 : -4542808, var51
                     );
                     ImGui.setCursorScreenPos(var53 - var48, var41 - var49 * 0.5F);
                     if (EfnWidgets.chipButton("##down", "-", var48, var49, false) && var38.level > 1) {
                        var38.level--;
                        if (!var38.on) {
                           var11 = var38;
                        } else {
                           this.writeEnchants();
                        }
                     }
                  }

                  ImGui.popID();
               }

               ClickGuiRenderer.smoothScroll(this.enchantScroll, frameDelta());
            }

            ImGui.endChild();
         } else {
            EfnWidgets.text(
               var13,
               var14,
               var7 + var16 + EfnWidgets.s(8.0F),
               var25 + EfnWidgets.s(10.0F),
               -4542808,
               this.enchantsLoaded ? "This item takes no enchantments." : "Join a world to load enchantments."
            );
         }

         float var55 = var8 + var5 - EfnWidgets.s(20.0F) - var24;
         float var56 = EfnWidgets.textWidth(var14, "Done") + EfnWidgets.s(36.0F);
         ImGui.setCursorPos(var7 + var4 - var16 - var56, var55);
         if (EfnWidgets.chipButton("##done", "Done", var56, var24, true)) {
            var10 = true;
         }

         float var57 = EfnWidgets.textWidth(var14, "Clear") + EfnWidgets.s(36.0F);
         ImGui.setCursorPos(var7 + var16, var55);
         boolean var58 = false;

         for (ItemPickerEditor.EnchantRow var61 : this.enchantRows) {
            var58 |= var61.on;
         }

         ImGui.beginDisabled(!var58);
         if (EfnWidgets.chipButton("##clear_enchants", "Clear", var57, var24, false)) {
            for (ItemPickerEditor.EnchantRow var62 : this.enchantRows) {
               var62.on = false;
            }

            this.writeEnchants();
         }

         ImGui.endDisabled();
         if (!this.enchantJustOpened && ImGui.isMouseClicked(0) && ImGui.isWindowHovered() && !ImGui.isMouseHoveringRect(var7, var8, var7 + var4, var8 + var5)) {
            var10 = true;
         }
      }

      ImGui.end();
      ImGui.popStyleVar(4);
      this.enchantJustOpened = false;
      if (var11 != null) {
         this.toggle(var11);
      }

      if (var10) {
         this.closeEnchants();
      }
   }

   private String summary(StringSetting var1) {
      ItemPickerEditor.Summary var2 = (ItemPickerEditor.Summary)this.summaries.get(var1);
      if (var2 != null && var2.revision() == var1.revision() && var2.names() == namesGeneration) {
         return var2.text();
      } else {
         Map var3 = ItemEnchants.parse(var1.get());
         ArrayList var4 = new ArrayList();
         List var5 = ItemEnchants.all();

         for (Entry var7 : var3.entrySet()) {
            String var8 = null;
            int var9 = 0;

            for (RegistryEntry var11 : var5) {
               if (ItemEnchants.key(ItemEnchants.path(var11)).equals(var7.getKey())) {
                  var8 = ((Enchantment)var11.comp_349()).comp_2686().getString();
                  var9 = ((Enchantment)var11.comp_349()).method_8183();
                  break;
               }
            }

            if (var8 == null) {
               var8 = (String)var7.getKey();
            }

            var4.add(var9 == 1 ? var8 : var8 + " " + ItemEnchants.roman((Integer)var7.getValue()));
         }

         String var12 = String.join(", ", var4);
         this.summaries.put(var1, new ItemPickerEditor.Summary(var1.revision(), namesGeneration, var12));
         return var12;
      }
   }

   private static boolean headerChip(String var0, String var1, float var2, float var3, ImFont var4) {
      float var5 = EfnWidgets.textWidth(var4, var1) + EfnWidgets.s(28.0F);
      float var6 = EfnWidgets.s(28.0F);
      ImGui.setCursorScreenPos(var2 - var5, var3 - var6 * 0.5F);
      return EfnWidgets.chipButton(var0, var1, var5, var6, false);
   }

   private static boolean chip(String var0, String var1, boolean var2, float var3, float var4, ImFont var5, String var6) {
      boolean var7 = ClickGuiRenderer.pushFont(var5);
      float var8 = EfnWidgets.textWidth(var5, var1) + EfnWidgets.s(20.0F);
      float var9 = EfnWidgets.lineHeight(var5) + EfnWidgets.s(12.0F);
      ImGui.setCursorPos(var3 - var8, var4 - var9 * 0.5F);
      ImGui.beginDisabled(var2);
      boolean var10 = EfnWidgets.chipButton(var0, var1, var8, var9, false);
      ImGui.endDisabled();
      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip(var6);
      }

      if (var7) {
         ImGui.popFont();
      }

      return var10;
   }

   private static void columnHeader(ImDrawList var0, ImFont var1, ImFont var2, float var3, float var4, float var5, float var6, String var7, String var8) {
      float var9 = var4 + var5 * 0.5F;
      EfnWidgets.text(var0, var1, var3 + EfnWidgets.s(4.0F), var9 - EfnWidgets.lineHeight(var1) * 0.5F, -461582, var7);
      countPill(var0, var2, var3 + EfnWidgets.s(4.0F) + EfnWidgets.textWidth(var1, var7) + EfnWidgets.s(8.0F), var9, var8);
      var0.addLine(var3, var4 + var5 + EfnWidgets.s(4.0F), var3 + var6, var4 + var5 + EfnWidgets.s(4.0F), EfnWidgets.c(268435455), 1.0F);
   }

   private static void countPill(ImDrawList var0, ImFont var1, float var2, float var3, String var4) {
      float var5 = EfnWidgets.textWidth(var1, var4) + EfnWidgets.s(14.0F);
      float var6 = EfnWidgets.lineHeight(var1) + EfnWidgets.s(6.0F);
      float var7 = var3 - var6 * 0.5F;
      var0.addRectFilled(var2, var7, var2 + var5, var7 + var6, EfnWidgets.c(-13226454), var6 * 0.5F);
      EfnWidgets.text(var0, var1, var2 + EfnWidgets.s(7.0F), var7 + EfnWidgets.s(3.0F), -4542808, var4);
   }

   private static void shadow(ImDrawList var0, float var1, float var2, float var3, float var4, float var5) {
      for (int var6 = 1; var6 <= 6; var6++) {
         float var7 = EfnWidgets.s(2.5F) * var6;
         var0.addRectFilled(
            var1 - var7, var2 - var7 + EfnWidgets.s(4.0F), var1 + var3 + var7, var2 + var4 + var7 + EfnWidgets.s(4.0F), EfnWidgets.c(335544320), var5 + var7
         );
      }
   }

   private static void beginFrame() {
      refreshNames();
   }

   private static ItemPickerEditor.ItemInfo info(Item var0) {
      refreshNames();
      ItemPickerEditor.ItemInfo var1 = (ItemPickerEditor.ItemInfo)BY_ITEM.get(var0);
      if (var1 == null) {
         var1 = new ItemPickerEditor.ItemInfo(var0);
         var1.name = var0.method_7854().method_7964().getString();
         var1.nameLower = var1.name.toLowerCase(Locale.ROOT);
         BY_ITEM.put(var0, var1);
      }

      return var1;
   }

   private static void refreshNames() {
      String var0 = MinecraftClient.method_1551().method_1526().method_4669();
      if (!var0.equals(language) || ITEMS.isEmpty()) {
         language = var0;
         if (ITEMS.isEmpty()) {
            for (Item var2 : Registries.field_41178) {
               if (var2 != Items.field_8162) {
                  ItemPickerEditor.ItemInfo var3 = new ItemPickerEditor.ItemInfo(var2);
                  ITEMS.add(var3);
                  BY_ITEM.put(var2, var3);
               }
            }
         }

         for (ItemPickerEditor.ItemInfo var5 : ITEMS) {
            var5.name = var5.item.method_7854().method_7964().getString();
            var5.nameLower = var5.name.toLowerCase(Locale.ROOT);
         }

         ITEMS.sort(Comparator.<ItemPickerEditor.ItemInfo, String>comparing(var0x -> var0x.nameLower).thenComparing(var0x -> var0x.id));
         namesGeneration++;
      }
   }

   private static int matchDistance(ItemPickerEditor.ItemInfo var0, String var1, String[] var2) {
      int var3 = Math.max(wordsFound(var0.nameLower, var2), wordsFound(var0.idLower, var2));
      int var4 = levenshtein(var1, var0.nameLower);
      int var5 = levenshtein(var1, var0.idLower);
      float var6 = Math.max(1.0F - (float)var4 / Math.max(1, var0.nameLower.length()), 1.0F - (float)var5 / Math.max(1, var0.idLower.length()));
      return var3 <= 0 && !(var6 >= 0.5F) ? -1 : Math.min(var4, var5);
   }

   private static int wordsFound(String var0, String[] var1) {
      int var2 = 0;

      for (String var6 : var1) {
         if (!var0.contains(var6)) {
            return 0;
         }

         if (!var6.isEmpty()) {
            for (int var7 = var0.indexOf(var6); var7 >= 0; var7 = var0.indexOf(var6, var7 + var6.length())) {
               var2++;
            }
         }
      }

      return var2;
   }

   private static int levenshtein(String var0, String var1) {
      int var2 = var0.length();
      int var3 = var1.length();
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
            char var7 = var0.charAt(var10 - 1);

            for (int var8 = 1; var8 <= var3; var8++) {
               int var9 = var4[var8 - 1] + (var7 == var1.charAt(var8 - 1) ? 0 : 8);
               var5[var8] = Math.min(Math.min(var4[var8] + 8, var5[var8 - 1] + 1), var9);
            }

            int[] var11 = var4;
            var4 = var5;
            var5 = var11;
         }

         return var4[var3];
      }
   }

   private static float frameDelta() {
      return ClickGuiRenderer.animationDelta(Math.max(ImGui.getIO().getDeltaTime(), 0.001F));
   }

   @Environment(EnvType.CLIENT)
   private static final class EnchantRow {
      final RegistryEntry<Enchantment> holder;
      final String path;
      final String key;
      final String name;
      final int max;
      int level;
      boolean on;

      EnchantRow(RegistryEntry<Enchantment> var1) {
         this.holder = var1;
         this.path = ItemEnchants.path(var1);
         this.key = ItemEnchants.key(this.path);
         this.name = ((Enchantment)var1.comp_349()).comp_2686().getString();
         this.level = this.max = Math.max(1, ((Enchantment)var1.comp_349()).method_8183());
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class ItemInfo {
      final Item item;
      final int rawId;
      final String id;
      final String stored;
      final String idLower;
      String name;
      String nameLower;

      ItemInfo(Item var1) {
         this.item = var1;
         this.rawId = Registries.field_41178.method_10206(var1);
         Identifier var2 = Registries.field_41178.method_10221(var1);
         this.id = var2.toString();
         this.stored = ItemEnchants.stored(var1);
         this.idLower = this.id.toLowerCase(Locale.ROOT);
      }
   }

   @Environment(EnvType.CLIENT)
   private record Summary(long revision, int names, String text) {
   }
}
