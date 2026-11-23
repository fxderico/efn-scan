package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImString;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.efn.client.config.ConfigProfiles;
import net.efn.client.config.ProfileException;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
final class ProfileList {
   private static final float ROW_H = 52.0F;
   private static final float CHIP_W = 58.0F;
   private static final float CHIP_H = 28.0F;
   private static final float GAP = 6.0F;
   private static final float LABEL_W = 96.0F;
   private static final float FIELD_H = 30.0F;
   private static final long ARM_NANOS = 3000000000L;
   private static final int DISCORD_LIMIT = 2000;
   private static final String PRIVATE_NOTE = "Webhooks, macros, seeds and other private settings are not included.";
   private final BiConsumer<String, Boolean> messages;
   private final ImString renameInput = new ImString(64);
   private final ImString serverInput = new ImString(256);
   private final Map<String, Float> editorHeights = new HashMap<>();
   private String expanded;
   private String armed;
   private long armedAt;
   private String scrollTo;
   private float scrollTarget = 0.0F / 0.0F;
   private boolean focusServer;

   ProfileList(BiConsumer<String, Boolean> var1) {
      this.messages = var1;
   }

   void open(String var1) {
      this.expanded = null;
      this.toggle(var1);
      this.scrollTo = key(var1);
   }

   void disarm() {
      this.armed = null;
   }

   float takeScrollTarget() {
      float var1 = this.scrollTarget;
      this.scrollTarget = 0.0F / 0.0F;
      return var1;
   }

   void render(ConfigProfiles var1, List<ConfigProfiles.Profile> var2, String var3, float var4, float var5) {
      for (ConfigProfiles.Profile var7 : var2) {
         this.row(var1, var7, var7.name().equalsIgnoreCase(var3), var4, var5);
      }
   }

   private void row(ConfigProfiles var1, ConfigProfiles.Profile var2, boolean var3, float var4, float var5) {
      String var8 = var2.name();
      String var9 = key(var8);
      boolean var10 = var9.equals(this.expanded);
      ImGui.pushID("profile_" + var9);
      float var11 = ImGui.getCursorPosY();
      if (var9.equals(this.scrollTo)) {
         this.scrollTarget = var11 - EfnWidgets.s(8.0F);
         this.scrollTo = null;
      }

      float var12 = EfnWidgets.s(52.0F);
      float var13 = EfnWidgets.animate(ImGui.getID("##reveal"), var10, 16.0F);
      float var14 = var13 > 0.001F ? this.editorHeights.getOrDefault(var9, EfnWidgets.s(240.0F)) * var13 : 0.0F;
      ImGui.setCursorPos(var4, var11);
      float var15 = ImGui.getCursorScreenPosX();
      float var16 = ImGui.getCursorScreenPosY();
      ImGui.setNextItemAllowOverlap();
      ImGui.invisibleButton("##row", var5, var12);
      if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
         this.load(var1, var2);
      }

      boolean var17 = ImGui.isMouseHoveringRect(var15, var16, var15 + var5, var16 + var12);
      float var18 = EfnWidgets.animate(ImGui.getItemID(), var17, 18.0F);
      ImDrawList var19 = ImGui.getWindowDrawList();
      float var20 = var15 - EfnWidgets.s(8.0F);
      float var21 = var15 + var5 + EfnWidgets.s(8.0F);
      float var22 = EfnWidgets.s(10.0F);
      int var7 = var13 > 0.001F ? 48 : 240;
      if (var13 > 0.001F) {
         var19.addRectFilled(var20, var16, var21, var16 + var12 + var14, EfnWidgets.c(EfnWidgets.withAlpha(184549375, var13)), var22);
      }

      if (var3) {
         var19.addRectFilled(var20, var16, var21, var16 + var12, EfnWidgets.c(EfnWidgets.withAlpha(GuiPalette.accent(), 0.16F)), var22, var7);
      }

      if (var18 > 0.01F) {
         var19.addRectFilled(var20, var16, var21, var16 + var12, EfnWidgets.c(EfnWidgets.withAlpha(218103807, var18)), var22, var7);
      }

      ImFont var24 = ImGui.getFont();
      ImFont var25 = ImGuiManager.getInstance().smallFont();
      float var26 = EfnWidgets.s(58.0F);
      float var27 = EfnWidgets.s(28.0F);
      float var28 = EfnWidgets.s(28.0F);
      float var29 = EfnWidgets.s(6.0F);
      float var30 = var26 * 2.0F + var28 + var29 * 2.0F;
      float var31 = var5 - var30 - EfnWidgets.s(12.0F);
      float var32 = var3 ? EfnWidgets.textWidth(var25, "Active") + EfnWidgets.s(14.0F) : 0.0F;
      String var33 = EfnWidgets.ellipsize(var24, var8, var31 - (var3 ? var32 + EfnWidgets.s(8.0F) : 0.0F));
      float var34 = EfnWidgets.lineHeight(var24);
      float var35 = EfnWidgets.lineHeight(var25);
      float var36 = var16 + (var12 - var34 - EfnWidgets.s(3.0F) - var35) * 0.5F;
      EfnWidgets.text(var19, var24, var15, var36, -461582, var33);
      if (var3) {
         float var6 = var15 + EfnWidgets.textWidth(var24, var33) + EfnWidgets.s(8.0F);
         float var37 = var35 + EfnWidgets.s(6.0F);
         float var38 = var36 + (var34 - var37) * 0.5F;
         var19.addRectFilled(var6, var38, var6 + var32, var38 + var37, EfnWidgets.c(GuiPalette.accent()), var37 * 0.5F);
         EfnWidgets.text(var19, var25, var6 + EfnWidgets.s(7.0F), var38 + EfnWidgets.s(3.0F), -1, "Active");
      }

      EfnWidgets.text(
         var19, var25, var15, var36 + var34 + EfnWidgets.s(3.0F), var2.broken() ? -10856974 : -4542808, EfnWidgets.ellipsize(var25, summary(var2), var31)
      );
      float var45 = var4 + var5 - var30;
      float var46 = var11 + (var12 - var27) * 0.5F;
      ImGui.setCursorPos(var45, var46);
      if (EfnWidgets.chipButton("##load", "Load", var26, var27, false)) {
         this.load(var1, var2);
      }

      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip("Apply the saved " + partList(var2.parts()) + " of this profile");
      }

      String var47 = "save:" + var9;
      boolean var39 = this.isArmed(var47);
      ImGui.setCursorPos(var45 + var26 + var29, var46);
      if (EfnWidgets.chipButton("##save", var39 ? "Sure?" : "Save", var26, var27, var39) && this.confirm(var47)) {
         this.save(var1, var2);
      }

      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip("Overwrite this profile with your current " + partList(var2.parts()));
      }

      ImGui.setCursorPos(var45 + (var26 + var29) * 2.0F, var11 + (var12 - var28) * 0.5F);
      float var40 = ImGui.getCursorScreenPosX() + var28 * 0.5F;
      float var41 = ImGui.getCursorScreenPosY() + var28 * 0.5F;
      if (ImGui.invisibleButton("##edit", var28, var28)) {
         this.toggle(var8);
      }

      boolean var42 = ImGui.isItemHovered();
      float var43 = Math.max(EfnWidgets.animate(ImGui.getItemID(), var42, 18.0F), var13);
      if (var43 > 0.01F) {
         var19.addCircleFilled(var40, var41, var28 * 0.5F, EfnWidgets.c(EfnWidgets.withAlpha(-12240588, var43)), 32);
      }

      EfnWidgets.chevronRotated(var19, var40, var41, EfnWidgets.s(4.0F), EfnTheme.lerpColor(-4542808, -461582, var43), var13 * 1.5707964F);
      if (var42) {
         ClickGuiRenderer.showTooltip(var10 ? "Close" : "Edit, share or delete");
      }

      float var44 = var11 + var12;
      if (var13 > 0.001F) {
         ImGui.pushClipRect(var20, var16 + var12, var21, var16 + var12 + var14, true);
         ImGui.pushStyleVar(0, ImGui.getStyle().getAlpha() * var13);
         this.editorHeights.put(var9, this.editor(var1, var2, var9, var4, var5, var44));
         ImGui.popStyleVar();
         ImGui.popClipRect();
      }

      ImGui.setCursorPos(0.0F, var44 + var14 + EfnWidgets.s(4.0F));
      ImGui.popID();
   }

   private float editor(ConfigProfiles var1, ConfigProfiles.Profile var2, String var3, float var4, float var5, float var6) {
      ImDrawList var7 = ImGui.getWindowDrawList();
      ImFont var8 = ImGui.getFont();
      float var9 = EfnWidgets.s(30.0F);
      float var10 = EfnWidgets.s(6.0F);
      float var11 = (var9 - ImGui.getTextLineHeight()) * 0.5F;
      float var12 = var4 + EfnWidgets.s(96.0F);
      float var13 = var5 - EfnWidgets.s(96.0F);
      float var14 = var6 + EfnWidgets.s(6.0F);
      label("Name", var4, var14, var9);
      float var15 = EfnWidgets.s(78.0F);
      ImGui.setCursorPos(var12, var14);
      ImGui.pushStyleVar(11, EfnWidgets.s(10.0F), var11);
      ImGui.setNextItemWidth(var13 - var15 - var10);
      ImGui.inputTextWithHint("##rename", "Profile name", this.renameInput);
      boolean var16 = EfnWidgets.submittedWithEnter();
      ImGui.popStyleVar();
      ImGui.setCursorPos(var4 + var5 - var15, var14);
      if (EfnWidgets.chipButton("##rename_go", "Rename", var15, var9, false)) {
         var16 = true;
      }

      if (var16) {
         this.rename(var1, var2);
      }

      float var27;
      label("Includes", var4, var27 = var14 + (var9 + EfnWidgets.s(10.0F)), var9);
      ConfigProfiles.Part[] var17 = ConfigProfiles.Part.values();
      float var18 = (var13 - var10 * (var17.length - 1)) / var17.length;

      for (int var19 = 0; var19 < var17.length; var19++) {
         ConfigProfiles.Part var20 = var17[var19];
         boolean var21 = var2.parts().contains(var20);
         ImGui.setCursorPos(var12 + var19 * (var18 + var10), var27);
         if (EfnWidgets.chipButton("##part_" + var20.key, var20.label, var18, var9, var21)) {
            this.setPart(var1, var2, var20, !var21);
         }

         if (ImGui.isItemHovered(65536)) {
            ClickGuiRenderer.showTooltip(partTip(var20, var21));
         }
      }

      label("Load on join", var4, var14 = var27 + (var9 + EfnWidgets.s(10.0F)), var9);

      for (String var32 : var2.servers()) {
         ImGui.pushID("server_" + var32);
         ImGui.setCursorPos(var12, var14);
         float var34 = ImGui.getCursorScreenPosX();
         float var22 = ImGui.getCursorScreenPosY();
         var7.addRectFilled(var34, var22, var34 + var13, var22 + var9, EfnWidgets.c(-13226454), EfnWidgets.s(7.0F));
         EfnWidgets.text(
            var7,
            var8,
            var34 + EfnWidgets.s(10.0F),
            var22 + (var9 - EfnWidgets.lineHeight(var8)) * 0.5F,
            -461582,
            EfnWidgets.ellipsize(var8, var32, var13 - var9 - EfnWidgets.s(14.0F))
         );
         ImGui.setCursorPos(var12 + var13 - var9, var14);
         if (ImGui.invisibleButton("##remove", var9, var9)) {
            this.removeServer(var1, var2, var32);
         }

         boolean var23 = ImGui.isItemHovered();
         float var24 = EfnWidgets.animate(ImGui.getItemID(), var23, 18.0F);
         float var25 = var34 + var13 - var9 * 0.5F;
         float var26 = var22 + var9 * 0.5F;
         if (var24 > 0.01F) {
            var7.addCircleFilled(var25, var26, var9 * 0.38F, EfnWidgets.c(EfnWidgets.withAlpha(-10856974, 0.18F * var24)), 24);
         }

         EfnWidgets.cross(var7, var25, var26, EfnWidgets.s(3.5F), EfnTheme.lerpColor(-4542808, -10856974, var24));
         if (var23) {
            ClickGuiRenderer.showTooltip("Stop loading on " + var32);
         }

         ImGui.popID();
         var14 += var9 + var10;
      }

      float var31 = EfnWidgets.s(60.0F);
      ImGui.setCursorPos(var12, var14);
      ImGui.pushStyleVar(11, EfnWidgets.s(10.0F), var11);
      ImGui.setNextItemWidth(var13 - var31 - var10);
      if (this.focusServer) {
         ImGui.setKeyboardFocusHere();
         this.focusServer = false;
      }

      ImGui.inputTextWithHint("##server", "Server address", this.serverInput, 16);
      boolean var33 = EfnWidgets.submittedWithEnter();
      ImGui.popStyleVar();
      ImGui.setCursorPos(var4 + var5 - var31, var14);
      if (EfnWidgets.chipButton("##server_add", "Add", var31, var9, false)) {
         var33 = true;
      }

      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip("Loads this profile when you join one of these servers.");
      }

      if (var33) {
         this.addServer(var1, var2);
      }

      float var35 = (var5 - var10 * 2.0F) / 3.0F;
      float var29;
      ImGui.setCursorPos(var4, var29 = var14 + (var9 + EfnWidgets.s(12.0F)));
      if (EfnWidgets.chipButton("##export", "Export file", var35, var9, false)) {
         this.export(var2);
      }

      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip("Save this profile as a .json file.\nWebhooks, macros, seeds and other private settings are not included.");
      }

      ImGui.setCursorPos(var4 + var35 + var10, var29);
      if (EfnWidgets.chipButton("##code", "Copy code", var35, var9, false)) {
         this.copyCode(var1, var2);
      }

      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip(
            "Copy a share code. Others load it with Paste code.\nWebhooks, macros, seeds and other private settings are not included."
         );
      }

      String var36 = "delete:" + var3;
      boolean var37 = this.isArmed(var36);
      ImGui.setCursorPos(var4 + (var35 + var10) * 2.0F, var29);
      if (dangerChip("##delete", var37 ? "Sure?" : "Delete", var35, var9, var37) && this.confirm(var36)) {
         this.delete(var1, var2);
      }

      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip(var37 ? "Click again to delete" : "Delete this profile");
      }

      return var29 + (var9 + EfnWidgets.s(12.0F)) - var6;
   }

   private void load(ConfigProfiles var1, ConfigProfiles.Profile var2) {
      try {
         Set var3 = var1.load(var2.name());
         this.messages.accept("Loaded " + var2.name() + " (" + partList(var3) + ")", false);
      } catch (ProfileException var4) {
         this.messages.accept(var4.getMessage(), true);
      }
   }

   private void save(ConfigProfiles var1, ConfigProfiles.Profile var2) {
      try {
         Set var3 = var1.save(var2.name());
         this.messages.accept("Saved your current " + partList(var3) + " to " + var2.name(), false);
      } catch (ProfileException var4) {
         this.messages.accept(var4.getMessage(), true);
      }
   }

   private void rename(ConfigProfiles var1, ConfigProfiles.Profile var2) {
      try {
         String var3 = var1.rename(var2.name(), this.renameInput.get());
         if (var3.equals(var2.name())) {
            return;
         }

         this.expanded = key(var3);
         this.renameInput.set(var3);
         this.messages.accept("Renamed " + var2.name() + " to " + var3, false);
      } catch (ProfileException var4) {
         this.messages.accept(var4.getMessage(), true);
      }
   }

   private void setPart(ConfigProfiles var1, ConfigProfiles.Profile var2, ConfigProfiles.Part var3, boolean var4) {
      try {
         var1.setPart(var2.name(), var3, var4);
         this.messages.accept(var4 ? "Saved your current " + var3.label + " into " + var2.name() : var3.label + " is no longer part of " + var2.name(), false);
      } catch (ProfileException var6) {
         this.messages.accept(var6.getMessage(), true);
      }
   }

   private void addServer(ConfigProfiles var1, ConfigProfiles.Profile var2) {
      try {
         String var3 = var1.addServer(var2.name(), this.serverInput.get());
         this.serverInput.set("");
         this.messages.accept(var2.name() + " now loads when you join " + var3, false);
      } catch (ProfileException var4) {
         this.messages.accept(var4.getMessage(), true);
      }

      this.focusServer = true;
   }

   private void removeServer(ConfigProfiles var1, ConfigProfiles.Profile var2, String var3) {
      try {
         var1.removeServer(var2.name(), var3);
         this.messages.accept(var2.name() + " no longer loads on " + var3, false);
      } catch (ProfileException var5) {
         this.messages.accept(var5.getMessage(), true);
      }
   }

   private void export(ConfigProfiles.Profile var1) {
      if (FileDialogs.busy()) {
         this.messages.accept("A file window is already open", true);
      } else {
         String var2 = var1.name();
         FileDialogs.saveProfile("Export " + var2, var2 + ".json", var2x -> {
            String var3 = var2x.getFileName().toString();
            Path var4 = var3.toLowerCase(Locale.ROOT).endsWith(".json") ? var2x : var2x.resolveSibling(var3 + ".json");

            try {
               ConfigProfiles.getInstance().exportTo(var2, var4);
               this.messages.accept("Exported " + var2 + " to " + var4.getFileName(), false);
            } catch (ProfileException var6) {
               this.messages.accept(var6.getMessage(), true);
            }
         });
      }
   }

   private void copyCode(ConfigProfiles var1, ConfigProfiles.Profile var2) {
      try {
         String var3 = var1.shareCode(var2.name());
         MinecraftClient.method_1551().field_1774.method_1455(var3);
         this.messages
            .accept(var3.length() > 2000 ? "Copied, but it is too long for Discord. Send the file instead" : "Share code for " + var2.name() + " copied", false);
      } catch (ProfileException var4) {
         this.messages.accept(var4.getMessage(), true);
      }
   }

   private void delete(ConfigProfiles var1, ConfigProfiles.Profile var2) {
      try {
         var1.delete(var2.name());
         if (key(var2.name()).equals(this.expanded)) {
            this.expanded = null;
         }

         this.messages.accept("Deleted " + var2.name(), false);
      } catch (ProfileException var4) {
         this.messages.accept(var4.getMessage(), true);
      }
   }

   private void toggle(String var1) {
      String var2 = key(var1);
      if (var2.equals(this.expanded)) {
         this.expanded = null;
      } else {
         this.expanded = var2;
         this.renameInput.set(var1);
         this.serverInput.set("");
      }
   }

   private boolean isArmed(String var1) {
      return var1.equals(this.armed) && System.nanoTime() - this.armedAt < 3000000000L;
   }

   private boolean confirm(String var1) {
      if (this.isArmed(var1)) {
         this.armed = null;
         return true;
      } else {
         this.armed = var1;
         this.armedAt = System.nanoTime();
         return false;
      }
   }

   private static boolean dangerChip(String var0, String var1, float var2, float var3, boolean var4) {
      float var6 = ImGui.getCursorScreenPosX();
      float var7 = ImGui.getCursorScreenPosY();
      boolean var8 = ImGui.invisibleButton(var0, var2, var3);
      float var9 = EfnWidgets.animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
      int var5 = var4 ? -10856974 : EfnTheme.lerpColor(-13226454, -12240588, var9);
      if (ImGui.isItemActive()) {
         var5 = EfnTheme.lerpColor(var5, -16777216, 0.12F);
      }

      ImDrawList var11 = ImGui.getWindowDrawList();
      var11.addRectFilled(var6, var7, var6 + var2, var7 + var3, EfnWidgets.c(var5), EfnWidgets.s(7.0F));
      ImFont var12 = ImGui.getFont();
      int var13 = var4 ? -1 : EfnTheme.lerpColor(-461582, -10856974, var9);
      EfnWidgets.text(var11, var12, var6 + (var2 - EfnWidgets.textWidth(var12, var1)) * 0.5F, var7 + (var3 - EfnWidgets.lineHeight(var12)) * 0.5F, var13, var1);
      return var8;
   }

   private static void label(String var0, float var1, float var2, float var3) {
      ImFont var4 = ImGuiManager.getInstance().smallFont();
      float var5 = ImGui.getWindowPosX() + var1;
      float var6 = ImGui.getWindowPosY() - ImGui.getScrollY() + var2;
      EfnWidgets.text(ImGui.getWindowDrawList(), var4, var5, var6 + (var3 - EfnWidgets.lineHeight(var4)) * 0.5F, -4542808, var0);
   }

   private static String summary(ConfigProfiles.Profile var0) {
      if (var0.broken()) {
         return "Can't be read - fix the file or delete it";
      } else {
         String var1 = partList(var0.parts());
         List var2 = var0.servers();
         if (var2.size() == 1) {
            return var1 + "  \u00b7  loads on " + (String)var2.get(0);
         } else {
            return var2.size() > 1 ? var1 + "  \u00b7  loads on " + var2.size() + " servers" : var1;
         }
      }
   }

   private static String partList(Set<ConfigProfiles.Part> var0) {
      StringBuilder var1 = new StringBuilder();

      for (ConfigProfiles.Part var3 : var0) {
         if (!var1.isEmpty()) {
            var1.append(", ");
         }

         var1.append(var3.label);
      }

      return var1.isEmpty() ? "nothing" : var1.toString();
   }

   private static String partTip(ConfigProfiles.Part var0, boolean var1) {
      String var2 = switch (var0) {
         case MODULES -> "Module settings, keybinds and toggles.";
         case HUD -> "HUD layout and the HUD theme.";
         case THEME -> "GUI theme.";
         case FRIENDS -> "Your friends list.";
      };
      return var2 + (var1 ? "\nClick to remove it from this profile." : "\nClick to add it to this profile.");
   }

   private static String key(String var0) {
      return var0.toLowerCase(Locale.ROOT);
   }
}
