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

   ProfileList(BiConsumer<String, Boolean> biConsumer) {
      this.messages = biConsumer;
   }

   void open(String string) {
      this.expanded = null;
      this.toggle(string);
      this.scrollTo = key(string);
   }

   void disarm() {
      this.armed = null;
   }

   float takeScrollTarget() {
      float var1 = this.scrollTarget;
      this.scrollTarget = 0.0F / 0.0F;
      return var1;
   }

   void render(ConfigProfiles configProfiles, List<ConfigProfiles.Profile> list, String string, float f, float g) {
      for (ConfigProfiles.Profile var7 : list) {
         this.row(configProfiles, var7, var7.name().equalsIgnoreCase(string), f, g);
      }
   }

   private void row(ConfigProfiles configProfiles, ConfigProfiles.Profile profile, boolean bl, float f, float g) {
      String var8 = profile.name();
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
      ImGui.setCursorPos(f, var11);
      float var15 = ImGui.getCursorScreenPosX();
      float var16 = ImGui.getCursorScreenPosY();
      ImGui.setNextItemAllowOverlap();
      ImGui.invisibleButton("##row", g, var12);
      if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
         this.load(configProfiles, profile);
      }

      boolean var17 = ImGui.isMouseHoveringRect(var15, var16, var15 + g, var16 + var12);
      float var18 = EfnWidgets.animate(ImGui.getItemID(), var17, 18.0F);
      ImDrawList var19 = ImGui.getWindowDrawList();
      float var20 = var15 - EfnWidgets.s(8.0F);
      float var21 = var15 + g + EfnWidgets.s(8.0F);
      float var22 = EfnWidgets.s(10.0F);
      int var7 = var13 > 0.001F ? 48 : 240;
      if (var13 > 0.001F) {
         var19.addRectFilled(var20, var16, var21, var16 + var12 + var14, EfnWidgets.c(EfnWidgets.withAlpha(184549375, var13)), var22);
      }

      if (bl) {
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
      float var31 = g - var30 - EfnWidgets.s(12.0F);
      float var32 = bl ? EfnWidgets.textWidth(var25, "Active") + EfnWidgets.s(14.0F) : 0.0F;
      String var33 = EfnWidgets.ellipsize(var24, var8, var31 - (bl ? var32 + EfnWidgets.s(8.0F) : 0.0F));
      float var34 = EfnWidgets.lineHeight(var24);
      float var35 = EfnWidgets.lineHeight(var25);
      float var36 = var16 + (var12 - var34 - EfnWidgets.s(3.0F) - var35) * 0.5F;
      EfnWidgets.text(var19, var24, var15, var36, -461582, var33);
      if (bl) {
         float var6 = var15 + EfnWidgets.textWidth(var24, var33) + EfnWidgets.s(8.0F);
         float var37 = var35 + EfnWidgets.s(6.0F);
         float var38 = var36 + (var34 - var37) * 0.5F;
         var19.addRectFilled(var6, var38, var6 + var32, var38 + var37, EfnWidgets.c(GuiPalette.accent()), var37 * 0.5F);
         EfnWidgets.text(var19, var25, var6 + EfnWidgets.s(7.0F), var38 + EfnWidgets.s(3.0F), -1, "Active");
      }

      EfnWidgets.text(
         var19, var25, var15, var36 + var34 + EfnWidgets.s(3.0F), profile.broken() ? -10856974 : -4542808, EfnWidgets.ellipsize(var25, summary(profile), var31)
      );
      float var45 = f + g - var30;
      float var46 = var11 + (var12 - var27) * 0.5F;
      ImGui.setCursorPos(var45, var46);
      if (EfnWidgets.chipButton("##load", "Load", var26, var27, false)) {
         this.load(configProfiles, profile);
      }

      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip("Apply the saved " + partList(profile.parts()) + " of this profile");
      }

      String var47 = "save:" + var9;
      boolean var39 = this.isArmed(var47);
      ImGui.setCursorPos(var45 + var26 + var29, var46);
      if (EfnWidgets.chipButton("##save", var39 ? "Sure?" : "Save", var26, var27, var39) && this.confirm(var47)) {
         this.save(configProfiles, profile);
      }

      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip("Overwrite this profile with your current " + partList(profile.parts()));
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
         this.editorHeights.put(var9, this.editor(configProfiles, profile, var9, f, g, var44));
         ImGui.popStyleVar();
         ImGui.popClipRect();
      }

      ImGui.setCursorPos(0.0F, var44 + var14 + EfnWidgets.s(4.0F));
      ImGui.popID();
   }

   private float editor(ConfigProfiles configProfiles, ConfigProfiles.Profile profile, String string, float f, float g, float h) {
      ImDrawList var7 = ImGui.getWindowDrawList();
      ImFont var8 = ImGui.getFont();
      float var9 = EfnWidgets.s(30.0F);
      float var10 = EfnWidgets.s(6.0F);
      float var11 = (var9 - ImGui.getTextLineHeight()) * 0.5F;
      float var12 = f + EfnWidgets.s(96.0F);
      float var13 = g - EfnWidgets.s(96.0F);
      float var14 = h + EfnWidgets.s(6.0F);
      label("Name", f, var14, var9);
      float var15 = EfnWidgets.s(78.0F);
      ImGui.setCursorPos(var12, var14);
      ImGui.pushStyleVar(11, EfnWidgets.s(10.0F), var11);
      ImGui.setNextItemWidth(var13 - var15 - var10);
      ImGui.inputTextWithHint("##rename", "Profile name", this.renameInput);
      boolean var16 = EfnWidgets.submittedWithEnter();
      ImGui.popStyleVar();
      ImGui.setCursorPos(f + g - var15, var14);
      if (EfnWidgets.chipButton("##rename_go", "Rename", var15, var9, false)) {
         var16 = true;
      }

      if (var16) {
         this.rename(configProfiles, profile);
      }

      float var27;
      label("Includes", f, var27 = var14 + (var9 + EfnWidgets.s(10.0F)), var9);
      ConfigProfiles.Part[] var17 = ConfigProfiles.Part.values();
      float var18 = (var13 - var10 * (var17.length - 1)) / var17.length;

      for (int var19 = 0; var19 < var17.length; var19++) {
         ConfigProfiles.Part var20 = var17[var19];
         boolean var21 = profile.parts().contains(var20);
         ImGui.setCursorPos(var12 + var19 * (var18 + var10), var27);
         if (EfnWidgets.chipButton("##part_" + var20.key, var20.label, var18, var9, var21)) {
            this.setPart(configProfiles, profile, var20, !var21);
         }

         if (ImGui.isItemHovered(65536)) {
            ClickGuiRenderer.showTooltip(partTip(var20, var21));
         }
      }

      label("Load on join", f, var14 = var27 + (var9 + EfnWidgets.s(10.0F)), var9);

      for (String var32 : profile.servers()) {
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
            this.removeServer(configProfiles, profile, var32);
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
      ImGui.setCursorPos(f + g - var31, var14);
      if (EfnWidgets.chipButton("##server_add", "Add", var31, var9, false)) {
         var33 = true;
      }

      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip("Loads this profile when you join one of these servers.");
      }

      if (var33) {
         this.addServer(configProfiles, profile);
      }

      float var35 = (g - var10 * 2.0F) / 3.0F;
      float var29;
      ImGui.setCursorPos(f, var29 = var14 + (var9 + EfnWidgets.s(12.0F)));
      if (EfnWidgets.chipButton("##export", "Export file", var35, var9, false)) {
         this.export(profile);
      }

      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip("Save this profile as a .json file.\nWebhooks, macros, seeds and other private settings are not included.");
      }

      ImGui.setCursorPos(f + var35 + var10, var29);
      if (EfnWidgets.chipButton("##code", "Copy code", var35, var9, false)) {
         this.copyCode(configProfiles, profile);
      }

      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip(
            "Copy a share code. Others load it with Paste code.\nWebhooks, macros, seeds and other private settings are not included."
         );
      }

      String var36 = "delete:" + string;
      boolean var37 = this.isArmed(var36);
      ImGui.setCursorPos(f + (var35 + var10) * 2.0F, var29);
      if (dangerChip("##delete", var37 ? "Sure?" : "Delete", var35, var9, var37) && this.confirm(var36)) {
         this.delete(configProfiles, profile);
      }

      if (ImGui.isItemHovered(65536)) {
         ClickGuiRenderer.showTooltip(var37 ? "Click again to delete" : "Delete this profile");
      }

      return var29 + (var9 + EfnWidgets.s(12.0F)) - h;
   }

   private void load(ConfigProfiles configProfiles, ConfigProfiles.Profile profile) {
      try {
         Set var3 = configProfiles.load(profile.name());
         this.messages.accept("Loaded " + profile.name() + " (" + partList(var3) + ")", false);
      } catch (ProfileException var4) {
         this.messages.accept(var4.getMessage(), true);
      }
   }

   private void save(ConfigProfiles configProfiles, ConfigProfiles.Profile profile) {
      try {
         Set var3 = configProfiles.save(profile.name());
         this.messages.accept("Saved your current " + partList(var3) + " to " + profile.name(), false);
      } catch (ProfileException var4) {
         this.messages.accept(var4.getMessage(), true);
      }
   }

   private void rename(ConfigProfiles configProfiles, ConfigProfiles.Profile profile) {
      try {
         String var3 = configProfiles.rename(profile.name(), this.renameInput.get());
         if (var3.equals(profile.name())) {
            return;
         }

         this.expanded = key(var3);
         this.renameInput.set(var3);
         this.messages.accept("Renamed " + profile.name() + " to " + var3, false);
      } catch (ProfileException var4) {
         this.messages.accept(var4.getMessage(), true);
      }
   }

   private void setPart(ConfigProfiles configProfiles, ConfigProfiles.Profile profile, ConfigProfiles.Part part, boolean bl) {
      try {
         configProfiles.setPart(profile.name(), part, bl);
         this.messages
            .accept(bl ? "Saved your current " + part.label + " into " + profile.name() : part.label + " is no longer part of " + profile.name(), false);
      } catch (ProfileException var6) {
         this.messages.accept(var6.getMessage(), true);
      }
   }

   private void addServer(ConfigProfiles configProfiles, ConfigProfiles.Profile profile) {
      try {
         String var3 = configProfiles.addServer(profile.name(), this.serverInput.get());
         this.serverInput.set("");
         this.messages.accept(profile.name() + " now loads when you join " + var3, false);
      } catch (ProfileException var4) {
         this.messages.accept(var4.getMessage(), true);
      }

      this.focusServer = true;
   }

   private void removeServer(ConfigProfiles configProfiles, ConfigProfiles.Profile profile, String string) {
      try {
         configProfiles.removeServer(profile.name(), string);
         this.messages.accept(profile.name() + " no longer loads on " + string, false);
      } catch (ProfileException var5) {
         this.messages.accept(var5.getMessage(), true);
      }
   }

   private void export(ConfigProfiles.Profile profile) {
      if (FileDialogs.busy()) {
         this.messages.accept("A file window is already open", true);
      } else {
         String var2 = profile.name();
         FileDialogs.saveProfile("Export " + var2, var2 + ".json", path -> {
            String var3 = path.getFileName().toString();
            Path var4 = var3.toLowerCase(Locale.ROOT).endsWith(".json") ? path : path.resolveSibling(var3 + ".json");

            try {
               ConfigProfiles.getInstance().exportTo(var2, var4);
               this.messages.accept("Exported " + var2 + " to " + var4.getFileName(), false);
            } catch (ProfileException var6) {
               this.messages.accept(var6.getMessage(), true);
            }
         });
      }
   }

   private void copyCode(ConfigProfiles configProfiles, ConfigProfiles.Profile profile) {
      try {
         String var3 = configProfiles.shareCode(profile.name());
         MinecraftClient.getInstance().keyboard.setClipboard(var3);
         this.messages
            .accept(
               var3.length() > 2000 ? "Copied, but it is too long for Discord. Send the file instead" : "Share code for " + profile.name() + " copied", false
            );
      } catch (ProfileException var4) {
         this.messages.accept(var4.getMessage(), true);
      }
   }

   private void delete(ConfigProfiles configProfiles, ConfigProfiles.Profile profile) {
      try {
         configProfiles.delete(profile.name());
         if (key(profile.name()).equals(this.expanded)) {
            this.expanded = null;
         }

         this.messages.accept("Deleted " + profile.name(), false);
      } catch (ProfileException var4) {
         this.messages.accept(var4.getMessage(), true);
      }
   }

   private void toggle(String string) {
      String var2 = key(string);
      if (var2.equals(this.expanded)) {
         this.expanded = null;
      } else {
         this.expanded = var2;
         this.renameInput.set(string);
         this.serverInput.set("");
      }
   }

   private boolean isArmed(String string) {
      return string.equals(this.armed) && System.nanoTime() - this.armedAt < 3000000000L;
   }

   private boolean confirm(String string) {
      if (this.isArmed(string)) {
         this.armed = null;
         return true;
      } else {
         this.armed = string;
         this.armedAt = System.nanoTime();
         return false;
      }
   }

   private static boolean dangerChip(String string, String string2, float f, float g, boolean bl) {
      float var6 = ImGui.getCursorScreenPosX();
      float var7 = ImGui.getCursorScreenPosY();
      boolean var8 = ImGui.invisibleButton(string, f, g);
      float var9 = EfnWidgets.animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
      int var5 = bl ? -10856974 : EfnTheme.lerpColor(-13226454, -12240588, var9);
      if (ImGui.isItemActive()) {
         var5 = EfnTheme.lerpColor(var5, -16777216, 0.12F);
      }

      ImDrawList var11 = ImGui.getWindowDrawList();
      var11.addRectFilled(var6, var7, var6 + f, var7 + g, EfnWidgets.c(var5), EfnWidgets.s(7.0F));
      ImFont var12 = ImGui.getFont();
      int var13 = bl ? -1 : EfnTheme.lerpColor(-461582, -10856974, var9);
      EfnWidgets.text(var11, var12, var6 + (f - EfnWidgets.textWidth(var12, string2)) * 0.5F, var7 + (g - EfnWidgets.lineHeight(var12)) * 0.5F, var13, string2);
      return var8;
   }

   private static void label(String string, float f, float g, float h) {
      ImFont var4 = ImGuiManager.getInstance().smallFont();
      float var5 = ImGui.getWindowPosX() + f;
      float var6 = ImGui.getWindowPosY() - ImGui.getScrollY() + g;
      EfnWidgets.text(ImGui.getWindowDrawList(), var4, var5, var6 + (h - EfnWidgets.lineHeight(var4)) * 0.5F, -4542808, string);
   }

   private static String summary(ConfigProfiles.Profile profile) {
      if (profile.broken()) {
         return "Can't be read - fix the file or delete it";
      } else {
         String var1 = partList(profile.parts());
         List var2 = profile.servers();
         if (var2.size() == 1) {
            return var1 + "  \u00b7  loads on " + (String)var2.get(0);
         } else {
            return var2.size() > 1 ? var1 + "  \u00b7  loads on " + var2.size() + " servers" : var1;
         }
      }
   }

   private static String partList(Set<ConfigProfiles.Part> set) {
      StringBuilder var1 = new StringBuilder();

      for (ConfigProfiles.Part var3 : set) {
         if (!var1.isEmpty()) {
            var1.append(", ");
         }

         var1.append(var3.label);
      }

      return var1.isEmpty() ? "nothing" : var1.toString();
   }

   private static String partTip(ConfigProfiles.Part part, boolean bl) {
      String var2 = switch (part) {
         case MODULES -> "Module settings, keybinds and toggles.";
         case HUD -> "HUD layout and the HUD theme.";
         case THEME -> "GUI theme.";
         case FRIENDS -> "Your friends list.";
      };
      return var2 + (bl ? "\nClick to remove it from this profile." : "\nClick to add it to this profile.");
   }

   private static String key(String string) {
      return string.toLowerCase(Locale.ROOT);
   }
}
