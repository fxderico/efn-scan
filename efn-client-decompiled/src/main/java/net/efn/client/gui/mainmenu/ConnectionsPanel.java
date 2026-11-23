package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.efn.client.account.Account;
import net.efn.client.account.AccountType;
import net.efn.client.account.Accounts;
import net.efn.client.gui.HudDraw;
import net.efn.client.proxy.Proxies;
import net.efn.client.proxy.Proxy;
import net.efn.client.proxy.ProxyChecker;
import net.efn.client.proxy.ProxyType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

@Environment(EnvType.CLIENT)
public final class ConnectionsPanel {
   private static final String NAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
   private static final int WINDOW_FLAGS = 447;
   private static final float ACCOUNTS_W = 820.0F;
   private static final float PROXIES_W = 860.0F;
   private static final float PAD = 24.0F;
   private static final float FIELD_H = 50.0F;
   private static final float ACCOUNT_ROW_H = 68.0F;
   private static final float PROXY_ROW_H = 46.0F;
   private static final float TAB_H = 40.0F;
   private static final float BAR_H = 30.0F;
   private static final float FACE = 46.0F;
   private static final float CHECK = 16.0F;
   private static final int ACCOUNT_ROWS = 6;
   private static final int PROXY_ROWS = 8;
   private static final float FADE = 0.15F;
   private static final float CONFIRM_FOR = 3.0F;
   static final int WINDOW = -15461610;
   private static final int ERROR = -1733516;
   private static final int GOOD = -6501498;
   private final Runnable onProxyChange;
   private final DuskWidgets ui = new DuskWidgets();
   private final MenuFrame frame = new MenuFrame();
   private boolean open;
   private boolean opening;
   private long openedAt;
   private ConnectionsPanel.Tab tab = ConnectionsPanel.Tab.ACCOUNTS;
   private String routeAtOpen = "";
   private Object armed;
   private float armedUntil;
   private AccountType adding = AccountType.CRACKED;
   private AccountType accountFilter;
   private final DuskWidgets.TextField crackedName = new DuskWidgets.TextField(16)
      .placeholder("Username")
      .allowedChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
   private final DuskWidgets.TextField token = new DuskWidgets.TextField(4096).placeholder("Minecraft access token").password(true);
   private CompletableFuture<?> microsoft;
   private String accountError;
   private final DuskWidgets.TextField proxyInput = new DuskWidgets.TextField(512).placeholder("host:port or socks5://user:pass@host:port");
   private String proxyMessage;
   private boolean proxyError;
   private ConnectionsPanel.Filter filter = ConnectionsPanel.Filter.ALL;
   private ProxyType typeFilter;
   private ConnectionsPanel.Sort sort = ConnectionsPanel.Sort.ADDED;
   private final Set<Proxy> selected = new HashSet<>();
   private boolean fileDialog;

   public ConnectionsPanel(Runnable runnable) {
      this.onProxyChange = runnable;
   }

   public void open(ConnectionsPanel.Tab tab) {
      if (!this.open) {
         this.open = true;
         this.opening = true;
         this.openedAt = System.nanoTime();
         this.routeAtOpen = route();
      }

      this.tab = tab;
      this.armed = null;
      if (tab == ConnectionsPanel.Tab.PROXIES) {
         this.proxyInput.focus();
      } else if (this.adding != AccountType.MICROSOFT) {
         this.field(this.adding).focus();
      }
   }

   public boolean isOpen() {
      return this.open;
   }

   public boolean keyPressed(int i) {
      if (!this.open) {
         return false;
      } else {
         if (i == 256) {
            this.close();
         }

         return true;
      }
   }

   public void render(float f, float g, float h, float i) {
      if (this.open) {
         ImGuiIO var5 = ImGui.getIO();
         boolean var6 = !this.opening && ImGui.isMouseClicked(0);
         this.opening = false;
         ImGui.setNextWindowPos(0.0F, 0.0F);
         ImGui.setNextWindowSize(f, g);
         ImGui.pushStyleVar(2, 0.0F, 0.0F);
         ImGui.pushStyleVar(4, 0.0F);
         ImGui.begin("##efn_connections", 447);
         ImGui.popStyleVar(2);

         try {
            this.frame
               .begin(
                  ImGui.getWindowDrawList(),
                  ImGui.getForegroundDrawList(),
                  f,
                  g,
                  h,
                  (float)(System.nanoTime() - this.openedAt) / 1.0E9F,
                  i,
                  var5.getMousePosX(),
                  var5.getMousePosY(),
                  var6
               );
            this.ui.begin(this.frame);

            try {
               this.draw();
            } finally {
               this.ui.end();
            }

            this.drawTooltip();
         } finally {
            ImGui.end();
         }
      }
   }

   private void close() {
      this.open = false;
      this.crackedName.setText("");
      this.token.setText("");
      this.proxyInput.setText("");
      this.selected.clear();
      if (this.microsoft != null) {
         Accounts.get().cancelMicrosoft();
      }

      if (!route().equals(this.routeAtOpen)) {
         this.onProxyChange.run();
      }
   }

   private void draw() {
      ImDrawList var1 = this.frame.bg();
      float var2 = this.frame.scale();
      float var3 = this.frame.width();
      float var4 = this.frame.height();
      float var5 = Math.min(1.0F, this.frame.time() / 0.15F);
      float var6 = 1.0F - (1.0F - var5) * (1.0F - var5);
      this.ui.alpha(var6);
      MenuVeil.veil(var1, var3, var4, 0.5F * var6);
      boolean var7 = this.tab == ConnectionsPanel.Tab.ACCOUNTS;
      List var8 = var7 ? List.of() : this.shownProxies();
      List var9 = var7 ? this.shownAccounts() : List.of();
      float var10 = this.frame.textHeight(this.frame.itemFont());
      float var11 = this.frame.textHeight(this.frame.bodyFont());
      float var12 = var7 ? Math.max(3, Math.min(var9.size(), 6)) * 68.0F * var2 : Math.max(1, Math.min(var8.size(), 8)) * 46.0F * var2;
      float var13 = var7 ? 96.0F * var2 : 60.0F * var2 + 22.0F * var2;
      float var14 = Math.min((var7 ? 820.0F : 860.0F) * var2, var3 - 32.0F * var2);
      float var15 = Math.min(
         24.0F * var2 + var10 + 16.0F * var2 + 50.0F * var2 + 12.0F * var2 + var11 + 18.0F * var2 + var13 + var12 + 16.0F * var2, var4 - 32.0F * var2
      );
      float var16 = Math.round((var3 - var14) * 0.5F);
      float var17 = Math.round((var4 - var15) * 0.5F + (1.0F - var6) * 6.0F * var2);

      for (int var18 = 1; var18 <= 3; var18++) {
         float var19 = var18 * 4.0F * var2;
         var1.addRectFilled(
            var16 - var19, var17 - var19 + 4.0F * var2, var16 + var14 + var19, var17 + var15 + var19 + 4.0F * var2, HudDraw.col(-16777216, 0.08F * var6)
         );
      }

      var1.addRectFilled(var16, var17, var16 + var14, var17 + var15, HudDraw.col(-15461610, var6));
      var1.addRect(var16, var17, var16 + var14, var17 + var15, HudDraw.col(-1575946, 0.1F * var6), 0.0F, 0, Math.max(1.0F, var2));
      float var21 = 24.0F * var2;
      this.frame.text(var1, this.frame.itemFont(), var16 + var21, var17 + var21 * 0.8F, -1575946, var6, var7 ? "Accounts" : "Proxies");
      this.closeCross(var16 + var14 - 44.0F * var2, var17 + var21 * 0.8F + var10 * 0.5F - 16.0F * var2, 32.0F * var2, var2, var6);
      float var22 = var17 + var21 + var10 + 16.0F * var2;
      float var20 = var17 + var15 - 16.0F * var2;
      if (var7) {
         this.accounts(var9, var16, var22, var14, var20, var2, var6);
      } else {
         this.proxies(var8, var16, var22, var14, var20, var2, var6);
      }
   }

   private void closeCross(float f, float g, float h, float i, float j) {
      ImDrawList var6 = this.frame.bg();
      boolean var7 = this.ui.hovered(f, g, h, h);
      if (this.ui.pressed(f, g, h, h)) {
         this.close();
      }

      float var8 = this.ui.hover("close", var7);
      float var9 = f + h * 0.5F;
      float var10 = g + h * 0.5F;
      float var11 = 6.0F * i;
      int var12 = HudDraw.col(HudDraw.mix(-7693914, -1575946, var8), j);
      float var13 = Math.max(1.2F, 1.6F * i);
      var6.addLine(var9 - var11, var10 - var11, var9 + var11, var10 + var11, var12, var13);
      var6.addLine(var9 - var11, var10 + var11, var9 + var11, var10 - var11, var12, var13);
   }

   private List<Account> shownAccounts() {
      List var1 = Accounts.get().list();
      return this.accountFilter == null ? var1 : var1.stream().filter(account -> account.type() == this.accountFilter).toList();
   }

   private void accounts(List<Account> list, float f, float g, float h, float i, float j, float k) {
      float var9 = 24.0F * j;
      float var10 = f + var9;
      float var11 = h - 2.0F * var9;
      boolean var12 = Accounts.get().busy();
      this.addTabs(var10, g, j, k);
      g += 54.0F * j;
      float var13 = 50.0F * j;
      ImFont var14 = this.frame.bodyFont();
      String var8;
      if (this.adding == AccountType.MICROSOFT) {
         String var15 = this.microsoft != null ? "Waiting for the browser..." : "Sign in with Microsoft";
         if (this.box("microsoft", var15, var10, g, var11, var13, this.microsoft == null && !var12, k)) {
            this.signInMicrosoft();
         }

         var8 = "Opens Microsoft's sign-in in your browser";
      } else {
         DuskWidgets.TextField var27 = this.field(this.adding);
         if (this.ui.textField(var27, var10, g, var11, var13) && !var27.text().isBlank() && !var12) {
            this.add(var27);
         }

         var8 = this.adding == AccountType.CRACKED
            ? "Press Enter to add. Joins servers that don't check accounts."
            : "Press Enter to add. A token works until it expires, about a day.";
      }

      g += var13 + 12.0F * j;
      if (this.microsoft != null) {
         float var28 = this.frame.textWidth(var14, "Cancel");
         this.text(DuskWidgets.fit(var14, Accounts.get().status(), var11 - var28 - 16.0F * j), var10, g, -1575946, k);
         if (this.link("cancel", "Cancel", var10 + var11 - var28, g, true, k)) {
            Accounts.get().cancelMicrosoft();
         }
      } else if (var12) {
         this.text(DuskWidgets.fit(var14, Accounts.get().status(), var11), var10, g, -7693914, k);
      } else if (this.accountError != null) {
         this.text(DuskWidgets.fit(var14, this.accountError, var11), var10, g, -1733516, k);
      } else {
         this.text(DuskWidgets.fit(var14, var8, var11), var10, g, -7693914, k);
      }

      float var25;
      this.accountFilters(var10, var25 = g + (this.frame.textHeight(var14) + 18.0F * j), j, k);
      float var29 = i - (g = var25 + 42.0F * j);
      if (list.isEmpty()) {
         this.centered(this.accountFilter == null ? "No accounts yet" : "No " + this.accountFilter.label() + " accounts", f, g, h, var29, k);
      } else {
         Account var16 = Accounts.get().active();
         float var17 = f + 8.0F * j;
         float var18 = h - 16.0F * j;
         float var19 = 68.0F * j;
         float var20 = this.ui.beginScroll("accounts", var17, g, var18, var29, list.size() * var19);

         for (int var21 = 0; var21 < list.size(); var21++) {
            Account var22 = (Account)list.get(var21);
            this.accountRow(var22, var22.equals(var16), var17, g - var20 + var21 * var19, var18, var19, j, k);
         }

         this.ui.endScroll();
      }
   }

   private void addTabs(float f, float g, float h, float i) {
      ImDrawList var5 = this.frame.bg();
      ImFont var6 = this.frame.menuFont();
      float var7 = 40.0F * h;
      float var8 = g + (var7 - this.frame.textHeight(var6)) * 0.5F;

      for (AccountType var12 : AccountType.values()) {
         String var13 = var12.label();
         float var14 = this.frame.textWidth(var6, var13);
         boolean var15 = this.ui.hovered(f - 6.0F * h, g, var14 + 12.0F * h, var7);
         if (this.ui.pressed(f - 6.0F * h, g, var14 + 12.0F * h, var7) && this.microsoft == null && var12 != this.adding) {
            this.adding = var12;
            this.accountError = null;
            if (var12 != AccountType.MICROSOFT) {
               this.field(var12).focus();
            }
         }

         float var16 = this.ui.hover(new ConnectionsPanel.Key("add", var13), var15);
         this.frame.text(var5, var6, f, var8, var12 == this.adding ? -1575946 : HudDraw.mix(-7693914, -1575946, var16), i, var13);
         if (var12 == this.adding) {
            var5.addRectFilled(f, g + var7 - 2.0F * h, f + var14, g + var7, HudDraw.col(-15408912, i));
         }

         f += var14 + 30.0F * h;
      }
   }

   private void accountFilters(float f, float g, float h, float i) {
      List var5 = Accounts.get().list();
      if (this.chip("all-accounts", "All", var5.size(), this.accountFilter == null, f, g, i)) {
         this.accountFilter = null;
      }

      f += this.chipWidth("All", var5.size()) + 18.0F * h;

      for (AccountType var9 : AccountType.values()) {
         int var10 = (int)var5.stream().filter(account -> account.type() == var9).count();
         if (this.chip(var9, var9.label(), var10, this.accountFilter == var9, f, g, i)) {
            this.accountFilter = var9;
         }

         f += this.chipWidth(var9.label(), var10) + 18.0F * h;
      }
   }

   private void accountRow(Account account, boolean bl, float f, float g, float h, float i, float j, float k) {
      ImDrawList var9 = this.frame.bg();
      boolean var10 = this.ui.hovered(f, g, h, i);
      float var11 = this.ui.hover(account, var10);
      if (var11 > 0.01F && !bl) {
         var9.addRectFilled(f, g, f + h, g + i, HudDraw.col(-1575946, 0.05F * var11 * k));
      }

      float var12 = 46.0F * j;
      float var13 = f + 12.0F * j;
      PlayerFaces.draw(var9, account, var13, g + (i - var12) * 0.5F, var12, j, k);
      String var14 = account.name().isEmpty() ? account.type().label() + " account" : account.name();
      float var15 = this.removeCross(new ConnectionsPanel.Key(account, "remove"), f + h - 8.0F * j, g, i, var11, j, k);
      if (var15 < 0.0F) {
         Accounts.get().remove(account);
      } else {
         if (bl) {
            var15 = this.tag("Active", -6557705, var15, g, i, k);
         }

         this.entry(this.frame.menuFont(), this.frame.listFont(), var13 + var12 + 16.0F * j, g, i, var15 - 12.0F * j, var14, account.type().label(), k);
         if (this.ui.pressed(f, g, h, i) && !bl) {
            this.login(account);
         }
      }
   }

   private DuskWidgets.TextField field(AccountType accountType) {
      return accountType == AccountType.SESSION ? this.token : this.crackedName;
   }

   private void add(DuskWidgets.TextField textField) {
      CompletableFuture var2 = textField == this.token ? Accounts.get().addSession(textField.text()) : Accounts.get().addCracked(textField.text());
      this.track(var2);
      var2.thenRun(() -> textField.setText(""));
   }

   private void login(Account account) {
      if (!Accounts.get().busy()) {
         this.track(Accounts.get().login(account));
      }
   }

   private void signInMicrosoft() {
      CompletableFuture var1 = Accounts.get().addMicrosoft();
      this.microsoft = var1;
      this.track(var1);
      var1.whenComplete((account, throwable) -> this.microsoft = null);
   }

   private void track(CompletableFuture<?> completableFuture) {
      this.accountError = null;
      completableFuture.whenComplete((object, throwable) -> this.accountError = throwable == null ? null : messageOf(throwable));
   }

   private static String messageOf(Throwable throwable) {
      Throwable var1 = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;
      String var2 = var1.getMessage();
      return var2 != null && !var2.isBlank() ? var2 : "Login failed";
   }

   private List<Proxy> shownProxies() {
      ArrayList var1 = new ArrayList();

      for (Proxy var3 : Proxies.get().list()) {
         if (this.filter.matches(var3) && (this.typeFilter == null || var3.type == this.typeFilter)) {
            var1.add(var3);
         }
      }

      if (this.sort.order != null) {
         var1.sort(this.sort.order);
      }

      return var1;
   }

   private static int pingOrder(Proxy proxy) {
      if (proxy.ping >= 0) {
         return proxy.ping;
      } else {
         return proxy.ping == -2 ? 2147483646 : 2147483647;
      }
   }

   private void proxies(List<Proxy> list, float f, float g, float h, float i, float j, float k) {
      float var9 = 24.0F * j;
      float var10 = f + var9;
      float var11 = h - 2.0F * var9;
      List var12 = Proxies.get().list();
      this.selected.retainAll(new HashSet(var12));
      float var13 = 50.0F * j;
      float var14 = var11 - var13 - 8.0F * j;
      if (this.ui.textField(this.proxyInput, var10, g, var14, var13) && !this.proxyInput.text().isBlank()) {
         int var15 = Proxies.get().importText(this.proxyInput.text());
         this.message(var15 == 0 ? "Not added: already saved, or not host:port" : null, var15 == 0);
         if (var15 > 0) {
            this.proxyInput.setText("");
         }
      }

      float var8;
      if (this.pasteButton(var8 = var10 + var11 - var13, g, var13, k)) {
         String var30 = MinecraftClient.getInstance().keyboard.getClipboard();
         this.imported(Proxies.get().importText(var30), var30.isBlank() ? "The clipboard is empty" : "Nothing new on the clipboard");
      }

      if (this.ui.hoverOf("paste") > 0.5F) {
         this.frame.tooltip("Paste proxies from the clipboard", var8 + var13 * 0.5F, g + var13 + 6.0F * j);
      }

      ImFont var31 = this.frame.bodyFont();
      if (this.link("file", "Import file", var10, g = g + 62.0F * j, !this.fileDialog, k)) {
         this.importFile();
      }

      if (this.ui.hoverOf("file") > 0.5F) {
         this.frame
            .tooltip(
               "A text file with one proxy per line", var10 + this.frame.textWidth(var31, "Import file") * 0.5F, g + this.frame.textHeight(var31) + 8.0F * j
            );
      }

      float var16 = var10 + (this.frame.textWidth(var31, "Import file") + 24.0F * j);
      int var17 = ProxyChecker.get().pending();
      if (var17 > 0) {
         this.statusRight("Checking, " + var17 + " left", false, var16, var10 + var11, g, k);
      } else if (this.proxyMessage != null) {
         this.statusRight(this.proxyMessage, this.proxyError, var16, var10 + var11, g, k);
      }

      float var27;
      this.filters(var12, var10, var27 = g + (this.frame.textHeight(var31) + 18.0F * j), var11, j, k);
      this.actions(list, var12, var10, g = var27 + (30.0F * j + 10.0F * j), var11, j, k);
      float var29;
      float var18 = i - (var29 = g + (30.0F * j + 12.0F * j));
      if (list.isEmpty()) {
         this.centered(var12.isEmpty() ? "No proxies yet" : "None match the filter", f, var29, h, var18, k);
      } else {
         float var19 = f + 8.0F * j;
         float var20 = h - 16.0F * j;
         float var21 = 46.0F * j;
         float var22 = this.ui.beginScroll("proxies", var19, var29, var20, var18, list.size() * var21);
         int var23 = Math.max(0, (int)(var22 / var21));
         int var24 = Math.min(list.size(), (int)((var22 + var18) / var21) + 1);

         for (int var25 = var23; var25 < var24; var25++) {
            this.proxyRow((Proxy)list.get(var25), var19, var29 - var22 + var25 * var21, var20, var21, j, k);
         }

         this.ui.endScroll();
      }
   }

   private void filters(List<Proxy> list, float f, float g, float h, float i, float j) {
      int[] var8 = new int[ConnectionsPanel.Filter.values().length];
      int[] var9 = new int[ProxyType.values().length];

      for (Proxy var11 : list) {
         for (ConnectionsPanel.Filter var15 : ConnectionsPanel.Filter.values()) {
            if (var15.matches(var11)) {
               int var16 = var15.ordinal();
               var8[var16]++;
            }
         }

         int var28 = var11.type.ordinal();
         var9[var28]++;
      }

      float var17 = f;

      for (ConnectionsPanel.Filter var29 : ConnectionsPanel.Filter.values()) {
         if (this.chip(var29, var29.label, var8[var29.ordinal()], this.filter == var29, var17, g, j)) {
            this.filter = var29;
         }

         var17 += this.chipWidth(var29.label, var8[var29.ordinal()]) + 18.0F * i;
      }

      float var18;
      this.frame.bg().addLine(var18 = var17 + 10.0F * i, g + 4.0F * i, var18, g + 30.0F * i - 4.0F * i, HudDraw.col(-1575946, 0.12F * j), Math.max(1.0F, i));
      var17 = var18 + 20.0F * i;

      for (ProxyType var30 : ProxyType.values()) {
         boolean var31 = this.typeFilter == var30;
         if (this.chip(var30, var30.label(), var9[var30.ordinal()], var31, var17, g, j)) {
            this.typeFilter = var31 ? null : var30;
         }

         var17 += this.chipWidth(var30.label(), var9[var30.ordinal()]) + 18.0F * i;
      }

      ImFont var22 = this.frame.bodyFont();
      String var25 = "Sort: " + this.sort.label;
      if (this.link("sort", var25, f + h - this.frame.textWidth(var22, var25), g + (30.0F * i - this.frame.textHeight(var22)) * 0.5F, true, j)) {
         this.sort = ConnectionsPanel.Sort.values()[(this.sort.ordinal() + 1) % ConnectionsPanel.Sort.values().length];
      }
   }

   private void actions(List<Proxy> list, List<Proxy> list2, float f, float g, float h, float i, float j) {
      ImFont var11 = this.frame.bodyFont();
      float var12 = g + 30.0F * i * 0.5F;
      float var13 = var12 - this.frame.textHeight(var11) * 0.5F;
      boolean var10 = !list.isEmpty() && this.selected.containsAll(list);
      if (this.checkbox("select-all", var10, f + 12.0F * i, var12, i, j)) {
         if (var10) {
            this.selected.removeAll(list);
         } else {
            this.selected.addAll(list);
         }
      }

      float var15 = f + 12.0F * i + 16.0F * i + 18.0F * i;
      if (this.selected.isEmpty()) {
         if (this.link("check-all", "Check all", var15, var13, !list.isEmpty(), j)) {
            ProxyChecker.get().check(list);
         }

         List var18;
         ConnectionsPanel.Key var24;
         if (!(var18 = list2.stream().filter(proxy -> proxy.ping == -1).toList()).isEmpty()
            && this.link(
               var24 = new ConnectionsPanel.Key("bulk", "remove-dead"),
               this.armed(var24) ? "Remove " + var18.size() + " dead?" : "Remove dead",
               var15 = var15 + (this.frame.textWidth(var11, "Check all") + 22.0F * i),
               var13,
               true,
               j,
               this.armed(var24) ? -1733516 : -7693914
            )
            && this.confirm(var24)) {
            Proxies.get().removeAll(var18);
            this.message("Removed " + var18.size() + " dead", false);
         }
      } else {
         List var16 = List.copyOf(this.selected);
         String var17 = var16.size() + " selected";
         this.text(var17, var15, var13, -1575946, j);
         if (this.link("check-selected", "Check", var15 = var15 + (this.frame.textWidth(var11, var17) + 22.0F * i), var13, true, j)) {
            ProxyChecker.get().check(var16);
         }

         if (this.link("copy-selected", "Copy", var15 = var15 + (this.frame.textWidth(var11, "Check") + 22.0F * i), var13, true, j)) {
            MinecraftClient.getInstance().keyboard.setClipboard(String.join("\n", var16.stream().map(ConnectionsPanel::line).toList()));
            this.message("Copied " + var16.size() + (var16.size() == 1 ? " proxy" : " proxies"), false);
         }

         String var8;
         ConnectionsPanel.Key var9;
         if (this.link(
               var9 = new ConnectionsPanel.Key("bulk", "remove-selected"),
               var8 = this.armed(var9) ? "Remove " + var16.size() + "?" : "Remove",
               var15 = var15 + (this.frame.textWidth(var11, "Copy") + 22.0F * i),
               var13,
               true,
               j,
               this.armed(var9) ? -1733516 : -7693914
            )
            && this.confirm(var9)) {
            Proxies.get().removeAll(var16);
            this.selected.clear();
            this.message("Removed " + var16.size(), false);
         }

         if (this.link("clear-selection", "Clear", var15 = var15 + (this.frame.textWidth(var11, var8) + 22.0F * i), var13, true, j)) {
            this.selected.clear();
         }
      }
   }

   private void proxyRow(Proxy proxy, float f, float g, float h, float i, float j, float k) {
      ImDrawList var8 = this.frame.bg();
      boolean var9 = proxy.enabled || proxy.isValid();
      boolean var10 = this.ui.hovered(f, g, h, i);
      float var11 = this.ui.hover(proxy, var10);
      boolean var12 = this.selected.contains(proxy);
      if (var12) {
         var8.addRectFilled(f, g, f + h, g + i, HudDraw.col(-15408912, 0.07F * k));
      } else if (var11 > 0.01F && var9) {
         var8.addRectFilled(f, g, f + h, g + i, HudDraw.col(-1575946, 0.05F * var11 * k));
      }

      float var13 = g + i * 0.5F;
      if (this.checkbox(new ConnectionsPanel.Key(proxy, "select"), var12, f + 12.0F * j, var13, j, k)) {
         if (var12) {
            this.selected.remove(proxy);
         } else {
            this.selected.add(proxy);
         }
      }

      proxyDot(var8, f + 52.0F * j, var13, proxy.enabled, 1.3F * j, k);
      float var14 = this.removeCross(new ConnectionsPanel.Key(proxy, "remove"), f + h - 8.0F * j, g, i, var11, j, k);
      if (var14 < 0.0F) {
         Proxies.get().remove(proxy);
         this.selected.remove(proxy);
      } else {
         if (proxy.enabled) {
            var14 = this.tag("In use", -6557705, var14, g, i, k);
         }

         var14 = this.ping(proxy, var14, g, i, k);
         String var15 = proxy.username.isEmpty() ? "" : " \u00b7 " + proxy.username;
         String var16 = proxy.name.equals(proxy.displayAddress())
            ? proxy.type.label() + var15
            : proxy.type.label() + " \u00b7 " + proxy.displayAddress() + var15;
         this.entry(this.frame.bodyFont(), this.frame.detailFont(), f + 74.0F * j, g, i, var14 - 12.0F * j, proxy.name, var16, k);
         if (this.ui.pressed(f, g, h, i) && var9) {
            Proxies.get().setEnabled(proxy, !proxy.enabled);
            this.message(null, false);
         }
      }
   }

   private float ping(Proxy proxy, float f, float g, float h, float i) {
      float var6 = f - 96.0F * this.frame.scale();
      if (ProxyChecker.get().isChecking(proxy)) {
         int var7 = (int)(this.frame.time() * 3.0F) % 4;
         this.tag("Checking" + ".".repeat(var7) + " ".repeat(3 - var7), -7693914, f, g, h, i);
      } else if (proxy.ping >= 0) {
         this.tag(proxy.ping + " ms", -6501498, f, g, h, i);
      } else if (proxy.ping == -1) {
         this.tag("Dead", -1733516, f, g, h, i);
      }

      return var6;
   }

   private static String line(Proxy proxy) {
      String var1 = proxy.username.isEmpty() ? "" : proxy.username + (proxy.password.isEmpty() ? "" : ":" + proxy.password) + "@";
      String var2 = proxy.type == ProxyType.SOCKS4 ? "socks4://" : (var1.isEmpty() ? "" : "socks5://");
      return var2 + var1 + proxy.displayAddress();
   }

   private void imported(int i, String string) {
      this.message(i > 0 ? "Added " + i + (i == 1 ? " proxy" : " proxies") : string, i == 0);
   }

   private void importFile() {
      this.fileDialog = true;
      Thread var1 = new Thread(() -> {
         String var1x = null;

         try {
            var1x = TinyFileDialogs.tinyfd_openFileDialog("Import proxies", null, null, null, false);
         } finally {
            String var4 = var1x;
            MinecraftClient.getInstance().execute(() -> {
               this.fileDialog = false;
               if (var4 != null) {
                  this.importFrom(Path.of(var4));
               }
            });
         }
      }, "EFN proxy import");
      var1.setDaemon(true);
      var1.start();
   }

   private void importFrom(Path path) {
      try {
         this.imported(Proxies.get().importText(Files.readString(path)), "No proxies found in " + path.getFileName());
      } catch (IOException var3) {
         this.message("Couldn't read " + path.getFileName(), true);
      }
   }

   private void message(String string, boolean bl) {
      this.proxyMessage = string;
      this.proxyError = bl;
   }

   private static String route() {
      Proxy var0 = Proxies.get().enabled();
      return var0 == null ? "" : var0.type + " " + var0.displayAddress() + " " + var0.username + " " + var0.password;
   }

   private void entry(ImFont imFont, ImFont imFont2, float f, float g, float h, float i, String string, String string2, float j) {
      float var10 = this.frame.textHeight(imFont);
      float var11 = 3.0F * this.frame.scale();
      boolean var12 = string2.isEmpty();
      float var13 = var12 ? g + (h - var10) * 0.5F : g + (h - var10 - var11 - this.frame.textHeight(imFont2)) * 0.5F;
      this.frame.text(this.frame.bg(), imFont, f, var13, -1575946, j, DuskWidgets.fit(imFont, string, i - f));
      if (!var12) {
         this.frame.text(this.frame.bg(), imFont2, f, var13 + var10 + var11, -7693914, j, DuskWidgets.fit(imFont2, string2, i - f));
      }
   }

   private float tag(String string, int i, float f, float g, float h, float j) {
      ImFont var7 = this.frame.bodyFont();
      float var8 = f - this.frame.textWidth(var7, string);
      this.frame.text(this.frame.bg(), var7, var8, g + (h - this.frame.textHeight(var7)) * 0.5F, i, j, string);
      return var8 - 14.0F * this.frame.scale();
   }

   private float removeCross(ConnectionsPanel.Key key, float f, float g, float h, float i, float j, float k) {
      ImDrawList var9 = this.frame.bg();
      if (this.armed(key)) {
         ImFont var19 = this.frame.bodyFont();
         String var20 = "Remove?";
         float var21 = this.frame.textWidth(var19, var20);
         float var13 = f - var21;
         float var22 = this.frame.textHeight(var19);
         boolean var23 = this.ui.hovered(var13 - 6.0F * j, g, var21 + 12.0F * j, h);
         if (this.ui.pressed(var13 - 6.0F * j, g, var21 + 12.0F * j, h)) {
            this.armed = null;
            return -1.0F;
         } else {
            this.frame.text(var9, var19, var13, g + (h - var22) * 0.5F, -1733516, var23 ? k : 0.85F * k, var20);
            return var13 - 14.0F * j;
         }
      } else {
         float var10 = 28.0F * j;
         float var11 = f - var10;
         float var12 = g + h * 0.5F;
         boolean var8 = i > 0.5F && this.ui.hovered(var11, var12 - var10 * 0.5F, var10, var10);
         if (var8 && this.ui.pressed(var11, var12 - var10 * 0.5F, var10, var10)) {
            this.confirm(key);
         }

         float var14 = this.ui.hover(key, var8);
         if (i > 0.01F) {
            float var15 = var11 + var10 * 0.5F;
            float var16 = 5.0F * j;
            float var17 = Math.max(1.2F, 1.5F * j);
            int var18 = HudDraw.col(HudDraw.mix(-7693914, -1733516, var14), i * k);
            var9.addLine(var15 - var16, var12 - var16, var15 + var16, var12 + var16, var18, var17);
            var9.addLine(var15 - var16, var12 + var16, var15 + var16, var12 - var16, var18, var17);
         }

         return var11 - 6.0F * j;
      }
   }

   private boolean checkbox(Object object, boolean bl, float f, float g, float h, float i) {
      ImDrawList var7 = this.frame.bg();
      float var8 = 8.0F * h;
      float var9 = 16.0F * h;
      float var10 = g - var9 * 0.5F;
      boolean var11 = this.ui.hovered(f - var8, var10 - var8, var9 + 2.0F * var8, var9 + 2.0F * var8);
      boolean var12 = var11 && this.ui.pressed(f - var8, var10 - var8, var9 + 2.0F * var8, var9 + 2.0F * var8);
      float var13 = this.ui.hover(object, var11);
      if (bl) {
         var7.addRectFilled(f, var10, f + var9, var10 + var9, HudDraw.col(-15408912, i));
         var7.pathClear();
         var7.pathLineTo(f + var9 * 0.22F, var10 + var9 * 0.52F);
         var7.pathLineTo(f + var9 * 0.42F, var10 + var9 * 0.72F);
         var7.pathLineTo(f + var9 * 0.78F, var10 + var9 * 0.3F);
         var7.pathStroke(HudDraw.col(-16777216, i), 0, Math.max(1.4F, 2.0F * h));
      } else {
         var7.addRectFilled(f, var10, f + var9, var10 + var9, HudDraw.col(-16777216, 0.3F * i));
         var7.addRect(
            f, var10, f + var9, var10 + var9, HudDraw.col(HudDraw.mix(-1575946, -15408912, var13), (0.3F + 0.4F * var13) * i), 0.0F, 0, Math.max(1.0F, h)
         );
      }

      return var12;
   }

   private float chipWidth(String string, int i) {
      return this.frame.textWidth(this.frame.bodyFont(), string) + 7.0F * this.frame.scale() + this.frame.textWidth(this.frame.bodyFont(), Integer.toString(i));
   }

   private boolean chip(Object object, String string, int i, boolean bl, float f, float g, float h) {
      ImDrawList var10 = this.frame.bg();
      ImFont var11 = this.frame.bodyFont();
      float var12 = this.frame.scale();
      float var8;
      float var9;
      boolean var13 = this.ui.hovered(f - 4.0F * var12, g, (var9 = this.chipWidth(string, i)) + 8.0F * var12, var8 = 30.0F * var12);
      boolean var14 = var13 && this.ui.pressed(f - 4.0F * var12, g, var9 + 8.0F * var12, var8);
      float var15 = this.ui.hover(object, var13);
      float var16 = g + (var8 - this.frame.textHeight(var11)) * 0.5F;
      this.frame.text(var10, var11, f, var16, bl ? -6557705 : HudDraw.mix(-7693914, -1575946, var15), h, string);
      float var17 = this.frame.textWidth(var11, string) + 7.0F * var12;
      this.frame.text(var10, var11, f + var17, var16, -7693914, 0.8F * h, Integer.toString(i));
      return var14;
   }

   private boolean box(Object object, String string, float f, float g, float h, float i, boolean bl, float j) {
      ImDrawList var9 = this.frame.bg();
      float var10 = this.frame.scale();
      float var11 = bl ? j : 0.5F * j;
      boolean var12 = bl && this.ui.hovered(f, g, h, i);
      boolean var13 = var12 && this.ui.pressed(f, g, h, i);
      float var14 = this.ui.hover(object, var12);
      var9.addRectFilled(f, g, f + h, g + i, HudDraw.col(-16777216, 0.35F * var11));
      if (var14 > 0.01F) {
         var9.addRectFilled(f, g, f + h, g + i, HudDraw.col(-15408912, 0.08F * var14 * var11));
      }

      var9.addRect(f, g, f + h, g + i, HudDraw.col(HudDraw.mix(-1575946, -15408912, var14), (0.2F + 0.5F * var14) * var11), 0.0F, 0, Math.max(1.0F, var10));
      ImFont var15 = this.frame.bodyFont();
      this.frame
         .text(
            var9,
            var15,
            f + (h - this.frame.textWidth(var15, string)) * 0.5F,
            g + (i - this.frame.textHeight(var15)) * 0.5F,
            HudDraw.mix(-1575946, -6621958, var14),
            var11,
            string
         );
      return var13;
   }

   private boolean pasteButton(float f, float g, float h, float i) {
      ImDrawList var5 = this.frame.bg();
      float var6 = this.frame.scale();
      boolean var7 = this.ui.hovered(f, g, h, h);
      boolean var8 = var7 && this.ui.pressed(f, g, h, h);
      float var9 = this.ui.hover("paste", var7);
      var5.addRectFilled(f, g, f + h, g + h, HudDraw.col(-16777216, 0.35F * i));
      if (var9 > 0.01F) {
         var5.addRectFilled(f, g, f + h, g + h, HudDraw.col(-15408912, 0.08F * var9 * i));
      }

      var5.addRect(f, g, f + h, g + h, HudDraw.col(HudDraw.mix(-1575946, -15408912, var9), (0.12F + 0.5F * var9) * i), 0.0F, 0, Math.max(1.0F, var6));
      float var10 = f + h * 0.5F;
      float var11 = g + h * 0.5F + var6;
      float var12 = Math.max(1.2F, 1.5F * var6);
      int var13 = HudDraw.col(HudDraw.mix(-7693914, -6621958, var9), i);
      var5.addRect(var10 - 7.0F * var6, var11 - 8.0F * var6, var10 + 7.0F * var6, var11 + 9.0F * var6, var13, 2.0F * var6, 0, var12);
      var5.addRectFilled(var10 - 3.5F * var6, var11 - 10.5F * var6, var10 + 3.5F * var6, var11 - 6.0F * var6, var13, var6);
      var5.addLine(var10 - 3.5F * var6, var11 - 1.0F * var6, var10 + 3.5F * var6, var11 - 1.0F * var6, var13, var12);
      var5.addLine(var10 - 3.5F * var6, var11 + 3.5F * var6, var10 + 2.0F * var6, var11 + 3.5F * var6, var13, var12);
      return var8;
   }

   private boolean link(Object object, String string, float f, float g, boolean bl, float h) {
      return this.link(object, string, f, g, bl, h, -7693914);
   }

   private boolean link(Object object, String string, float f, float g, boolean bl, float h, int i) {
      ImFont var8 = this.frame.bodyFont();
      float var9 = this.frame.scale();
      float var10 = this.frame.textWidth(var8, string);
      float var11 = this.frame.textHeight(var8);
      boolean var12 = bl && this.ui.hovered(f, g - 4.0F * var9, var10, var11 + 8.0F * var9);
      boolean var13 = var12 && this.ui.pressed(f, g - 4.0F * var9, var10, var11 + 8.0F * var9);
      float var14 = this.ui.hover(object, var12);
      float var15 = bl ? h : 0.4F * h;
      this.frame.text(this.frame.bg(), var8, f, g, HudDraw.mix(i, -1575946, var14), var15, string);
      if (var14 > 0.01F) {
         this.frame.bg().addLine(f, g + var11 + var9, f + var10 * var14, g + var11 + var9, HudDraw.col(-1575946, 0.6F * var14 * var15), Math.max(1.0F, var9));
      }

      return var13;
   }

   private void statusRight(String string, boolean bl, float f, float g, float h, float i) {
      ImFont var7 = this.frame.bodyFont();
      String var8 = DuskWidgets.fit(var7, string, g - f);
      this.text(var8, g - this.frame.textWidth(var7, var8), h, bl ? -1733516 : -7693914, i);
   }

   private void text(String string, float f, float g, int i, float h) {
      this.frame.text(this.frame.bg(), this.frame.bodyFont(), f, g, i, h, string);
   }

   private void centered(String string, float f, float g, float h, float i, float j) {
      ImFont var7 = this.frame.bodyFont();
      this.frame
         .text(this.frame.bg(), var7, f + (h - this.frame.textWidth(var7, string)) * 0.5F, g + (i - this.frame.textHeight(var7)) * 0.5F, -7693914, j, string);
   }

   private void drawTooltip() {
      String var1 = this.frame.pendingTooltip();
      if (var1 != null) {
         ImDrawList var2 = this.frame.fg();
         ImFont var3 = this.frame.detailFont();
         float var4 = this.frame.scale();
         float var5 = this.frame.textWidth(var3, var1) + 18.0F * var4;
         float var6 = this.frame.textHeight(var3) + 10.0F * var4;
         float var7 = Math.max(6.0F * var4, Math.min(this.frame.width() - var5 - 6.0F * var4, this.frame.tooltipX() - var5 * 0.5F));
         float var8 = this.frame.tooltipY();
         var2.addRectFilled(var7, var8, var7 + var5, var8 + var6, HudDraw.col(-16053748, 0.96F));
         var2.addRect(var7, var8, var7 + var5, var8 + var6, HudDraw.col(-1575946, 0.14F), 0.0F, 0, Math.max(1.0F, var4));
         this.frame.text(var2, var3, var7 + 9.0F * var4, var8 + 5.0F * var4, -1575946, 1.0F, var1);
      }
   }

   private boolean armed(Object object) {
      return object.equals(this.armed) && this.frame.time() < this.armedUntil;
   }

   private boolean confirm(Object object) {
      if (this.armed(object)) {
         this.armed = null;
         return true;
      } else {
         this.armed = object;
         this.armedUntil = this.frame.time() + 3.0F;
         return false;
      }
   }

   static void proxyDot(ImDrawList imDrawList, float f, float g, boolean bl, float h, float i) {
      if (bl) {
         imDrawList.addCircleFilled(f, g, 7.0F * h, HudDraw.col(-15408912, 0.16F * i), 20);
         imDrawList.addCircleFilled(f, g, 3.5F * h, HudDraw.col(-15408912, i), 12);
      } else {
         imDrawList.addCircle(f, g, 3.5F * h, HudDraw.col(-7693914, 0.8F * i), 12, Math.max(1.0F, h));
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Filter {
      ALL("All"),
      WORKING("Working"),
      DEAD("Dead"),
      UNCHECKED("Unchecked");

      final String label;

      private Filter(String string2) {
         this.label = string2;
      }

      boolean matches(Proxy proxy) {
         return switch (this) {
            case ALL -> true;
            case WORKING -> proxy.ping >= 0;
            case DEAD -> proxy.ping == -1;
            case UNCHECKED -> proxy.ping == -2;
         };
      }
   }

   @Environment(EnvType.CLIENT)
   private record Key(Object owner, String what) {
   }

   @Environment(EnvType.CLIENT)
   private static enum Sort {
      ADDED("Added", null),
      PING("Ping", Comparator.comparingInt(ConnectionsPanel::pingOrder)),
      NAME("Name", Comparator.comparing(proxy -> proxy.name.toLowerCase(Locale.ROOT)));

      final String label;
      final Comparator<Proxy> order;

      private Sort(String string2, Comparator<Proxy> comparator) {
         this.label = string2;
         this.order = comparator;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Tab {
      ACCOUNTS,
      PROXIES;

      private Tab() {
      }
   }
}
