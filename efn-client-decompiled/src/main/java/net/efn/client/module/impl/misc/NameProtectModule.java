package net.efn.client.module.impl.misc;

import com.mojang.authlib.GameProfile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.nameprotect.AliasPolicy;
import net.efn.client.module.impl.misc.nameprotect.NameMatcher;
import net.efn.client.module.impl.misc.nameprotect.ProtectedText;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.NameFilter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.session.Session;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class NameProtectModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final String OTHERS_KEEP = "Keep";
   private static final String OTHERS_SAME = "Same Label";
   private static final String OTHERS_NUMBERED = "Numbered";
   private static final String DEFAULT_SELF_ALIAS = "You";
   private static final int DEPARTED_MEMORY = 256;
   private static final int SEEN_MEMORY = 8192;
   private static final int MIN_OTHER_NAME_LENGTH = 3;
   private static final int NPC_UUID_VERSION = 2;
   private static final int CHAT_REFRESH_INTERVAL_TICKS = 20;
   private static final int REFUSAL_NOTICE_DELAY_TICKS = 30;
   private static final int RECENT_CHAT_LINES = 100;
   private static final int DISPLAY_CACHE_SLOTS = 64;
   private static volatile NameMatcher active;
   private static volatile NameProtectModule running;
   private static volatile int generation;
   private static volatile boolean failureLogged;
   private static final Object[] cacheInputs = new Object[64];
   private static final Object[] cacheOutputs = new Object[64];
   private static NameMatcher cacheMatcher;
   private static final String[] recentChat = new String[100];
   private static int recentChatNext;
   private final StringSetting selfAlias = this.addSetting("self-alias", new StringSetting("Your Alias", "You"));
   private final StringSetting extraNames = this.addSetting("extra-names", new StringSetting("Extra Names", ""));
   private final ModeSetting others = this.addSetting("others", new ModeSetting("Others", "Keep", "Keep", "Same Label", "Numbered"));
   private final ModeSetting othersLabel = this.addSetting("others-label", new ModeSetting("Others Label", "Player", "Player", "Anonymous", "Unknown"));
   private final LinkedHashMap<String, String> knownOthers = new LinkedHashMap<>();
   private final Map<String, Integer> numbers = new HashMap<>();
   private int nextNumber = 1;
   private final LinkedHashSet<String> seenSkeletons = new LinkedHashSet<>();
   private WeakReference<ClientPlayNetworkHandler> trackedConnection = new WeakReference<>(null);
   private volatile boolean dirty = true;
   private volatile boolean settingsEdited;
   private int builtListSize = -1;
   private String builtProfileName;
   private NameMatcher matcher = NameMatcher.EMPTY;
   private String refusedAlias;
   private AliasPolicy.Refusal refusalReason;
   private String noticedAlias;
   private int refusalNoticeDelay;
   private boolean chatRefreshPending;
   private int chatRefreshCooldown;

   public NameProtectModule() {
      super("name-protect", "Name Protect", "Shows aliases instead of player names on your screen.", ModuleCategory.MISC);
      this.usesNotifications();
      this.othersLabel.visibleWhen(() -> !this.others.is("Keep"));
      this.extraNames.privateValue();
      this.selfAlias.onChange(this::markEdited);
      this.extraNames.onChange(this::markEdited);
      this.others.onChange(this::markEdited);
      this.othersLabel.onChange(this::markEdited);
   }

   public static Text protect(Text text) {
      NameMatcher var3 = active;
      if (var3 != null && text != null) {
         boolean var4 = onRenderThread();
         Object var2;
         if (var4 && (var2 = cached(var3, text)) instanceof Text) {
            Text var5 = (Text)var2;
            return var5;
         } else {
            Text var1;
            try {
               var1 = ProtectedText.rewrite(var3, text);
            } catch (RuntimeException var6) {
               reportFailure(var6);
               return text;
            }

            if (var4) {
               remember(text, var1);
            }

            return var1;
         }
      } else {
         return text;
      }
   }

   public static boolean hidesName(String string) {
      NameMatcher var1 = active;
      return var1 != null && string != null && var1.aliasAt(string, 0, string.length()) != null;
   }

   public static SkinTextures hiddenHead(UUID uUID, SkinTextures skinTextures) {
      SkinChangeModule var3 = ModuleRegistry.get(SkinChangeModule.class);
      SkinTextures var2;
      return var3 != null && var3.isEnabled() && (var2 = var3.skinFor(uUID, skinTextures)) != skinTextures ? var2 : DefaultSkinHelper.getSkinTextures(uUID);
   }

   public static StringVisitable protectText(StringVisitable stringVisitable) {
      NameMatcher var3 = active;
      if (var3 != null && stringVisitable != null) {
         boolean var4 = onRenderThread();
         Object var2;
         if (var4 && (var2 = cached(var3, stringVisitable)) instanceof StringVisitable) {
            StringVisitable var5 = (StringVisitable)var2;
            return var5;
         } else {
            StringVisitable var1;
            try {
               var1 = ProtectedText.rewrite(var3, stringVisitable);
            } catch (RuntimeException var6) {
               reportFailure(var6);
               return stringVisitable;
            }

            if (var4) {
               remember(stringVisitable, var1);
            }

            return var1;
         }
      } else {
         return stringVisitable;
      }
   }

   public static StringVisitable protectChatLine(StringVisitable stringVisitable) {
      NameMatcher var1 = active;
      if (var1 != null && stringVisitable != null) {
         try {
            if (onRenderThread()) {
               recentChat[recentChatNext] = stringVisitable.getString();
               recentChatNext = (recentChatNext + 1) % 100;
            }

            return ProtectedText.rewrite(var1, stringVisitable);
         } catch (RuntimeException var3) {
            reportFailure(var3);
            return stringVisitable;
         }
      } else {
         return stringVisitable;
      }
   }

   public static int generation() {
      return generation;
   }

   public static void onPlayerInfoApplied(PlayerListS2CPacket playerListS2CPacket) {
      NameProtectModule var1 = running;
      if (var1 != null && playerListS2CPacket != null) {
         EnumSet var2 = playerListS2CPacket.getActions();
         if (var2.contains(Action.ADD_PLAYER) || var2.contains(Action.UPDATE_LISTED)) {
            MinecraftClient var3 = MinecraftClient.getInstance();
            if (var3.isOnThread()) {
               var1.syncConnection(var3);
               var1.rebuild();
            }
         }
      }
   }

   private static String filterName(String string) {
      NameMatcher var1 = active;
      return var1 != null ? var1.replace(string) : string;
   }

   private static void reportFailure(RuntimeException runtimeException) {
      if (!failureLogged) {
         failureLogged = true;
         LOGGER.error("Name Protect could not rewrite a text; showing it unchanged", (Throwable)runtimeException);
      }
   }

   private static boolean onRenderThread() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      return var0 != null && var0.isOnThread();
   }

   private static Object cached(NameMatcher nameMatcher, Object object) {
      if (cacheMatcher != nameMatcher) {
         clearDisplayCache();
         cacheMatcher = nameMatcher;
         return null;
      } else {
         int var2 = System.identityHashCode(object) & 63;
         return cacheInputs[var2] == object ? cacheOutputs[var2] : null;
      }
   }

   private static void remember(Object object, Object object2) {
      int var2 = System.identityHashCode(object) & 63;
      cacheInputs[var2] = object;
      cacheOutputs[var2] = object2;
   }

   private static void clearDisplayCache() {
      Arrays.fill(cacheInputs, null);
      Arrays.fill(cacheOutputs, null);
      cacheMatcher = null;
   }

   @Override
   protected void onEnable() {
      running = this;
      MinecraftClient var1 = MinecraftClient.getInstance();
      this.syncConnection(var1);
      this.settingsEdited = false;
      this.rebuild();
      NameFilter.set(NameProtectModule::filterName);
      this.chatRefreshPending = true;
      this.chatRefreshCooldown = 0;
      this.on(TickEvent.TYPE, Stage.PRE, this::onTick);
   }

   @Override
   protected void onDisable() {
      running = null;
      active = null;
      generation++;
      this.matcher = NameMatcher.EMPTY;
      NameFilter.set(null);
      this.chatRefreshPending = false;
      this.dirty = true;
      this.refusedAlias = null;
      this.noticedAlias = null;
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1 != null) {
         var1.execute(() -> {
            clearDisplayCache();
            Arrays.fill(recentChat, null);
            if (var1.inGameHud != null) {
               var1.inGameHud.getChatHud().reset();
            }
         });
      }
   }

   private void markEdited() {
      this.settingsEdited = true;
      this.dirty = true;
   }

   private void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      this.syncConnection(var2);
      if (listSize(var2.getNetworkHandler()) != this.builtListSize) {
         this.dirty = true;
      }

      if (!Objects.equals(profileName(var2), this.builtProfileName)) {
         this.dirty = true;
      }

      if (this.dirty) {
         this.rebuild();
      }

      this.announceRefusal(var2);
      if (this.chatRefreshCooldown > 0) {
         this.chatRefreshCooldown--;
      }

      if (this.chatRefreshPending && this.chatRefreshCooldown == 0 && !chatOpen(var2) && var2.inGameHud != null) {
         this.chatRefreshPending = false;
         this.chatRefreshCooldown = 20;
         var2.inGameHud.getChatHud().reset();
      }
   }

   private void syncConnection(MinecraftClient minecraftClient) {
      ClientPlayNetworkHandler var2 = minecraftClient != null ? minecraftClient.getNetworkHandler() : null;
      if (var2 != this.trackedConnection.get()) {
         this.trackedConnection = new WeakReference<>(var2);
         this.knownOthers.clear();
         this.numbers.clear();
         this.nextNumber = 1;
         this.builtListSize = -1;
         this.dirty = true;
      }
   }

   private void rebuild() {
      this.dirty = false;
      boolean var1 = this.settingsEdited;
      this.settingsEdited = false;
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayNetworkHandler var3 = var2 != null ? var2.getNetworkHandler() : null;
      Session var4 = var2 != null ? var2.getSession() : null;
      String var5 = NameMatcher.nameCore(var4 != null ? var4.getUsername() : null);
      this.builtProfileName = profileName(var2);
      String var6 = NameMatcher.nameCore(this.builtProfileName);
      this.builtListSize = listSize(var3);
      ArrayList var7 = new ArrayList();

      for (String var11 : this.extraNames.get().split("[,;\\s]+")) {
         String var12 = NameMatcher.nameCore(var11);
         if (var12 != null) {
            var7.add(var12);
         }
      }

      ArrayList var15 = new ArrayList(var7.size() + 2);
      if (var5 != null) {
         var15.add(var5);
      }

      if (var6 != null) {
         var15.add(var6);
      }

      var15.addAll(var7);
      this.rememberOnline(var3, var5, var6);
      String var16 = this.resolveOwnAlias(var7);
      LinkedHashMap var17 = new LinkedHashMap();

      for (String var20 : var15) {
         put(var17, var20, var16);
      }

      if (!this.others.is("Keep")) {
         this.trackOthers(var3, var15);
         String var19 = this.othersLabel.get();
         boolean var21 = this.others.is("Numbered");

         for (Entry var14 : this.knownOthers.entrySet()) {
            put(var17, (String)var14.getValue(), var21 ? var19 + " " + this.numbers.get(var14.getKey()) : var19);
         }
      }

      this.publish(new NameMatcher(var17), var1);
   }

   private void publish(NameMatcher nameMatcher, boolean bl) {
      NameMatcher var3 = this.matcher;
      this.matcher = nameMatcher;
      active = nameMatcher;
      if (!nameMatcher.sameMapping(var3)) {
         generation++;
         if (bl || recentChatMentions(nameMatcher.newNamesSince(var3))) {
            this.chatRefreshPending = true;
         }
      }
   }

   private static boolean recentChatMentions(NameMatcher nameMatcher) {
      if (nameMatcher.isEmpty()) {
         return false;
      } else {
         for (String var4 : recentChat) {
            if (nameMatcher.mentionsAny(var4)) {
               return true;
            }
         }

         return false;
      }
   }

   private String resolveOwnAlias(List<String> list) {
      String var2 = AliasPolicy.sanitize(this.selfAlias.get());
      if (!var2.isEmpty() && !var2.equalsIgnoreCase("You")) {
         HashSet var3 = new HashSet();

         for (String var5 : list) {
            var3.add(AliasPolicy.skeleton(var5));
         }

         AliasPolicy.Refusal var6 = AliasPolicy.check(var2, string -> this.seenSkeletons.contains(string) || var3.contains(string));
         if (var6 == null) {
            this.refusedAlias = null;
            return var2;
         } else {
            if (!var2.equals(this.refusedAlias)) {
               this.refusedAlias = var2;
               this.refusalNoticeDelay = 30;
            }

            this.refusalReason = var6;
            return "You";
         }
      } else {
         this.refusedAlias = null;
         return var2.isEmpty() ? "You" : var2;
      }
   }

   private void announceRefusal(MinecraftClient minecraftClient) {
      if (this.refusedAlias == null) {
         this.noticedAlias = null;
      } else if (!this.refusedAlias.equals(this.noticedAlias)) {
         if (this.refusalNoticeDelay > 0) {
            this.refusalNoticeDelay--;
         } else if (minecraftClient.player != null) {
            this.noticedAlias = this.refusedAlias;
            ModuleChat.send(
               this,
               ModuleChat.body()
                  .text("Alias ")
                  .name(this.refusedAlias)
                  .text(" ")
                  .danger("not used")
                  .text(": " + this.refusalReason.reason() + ", showing ")
                  .name("You")
                  .text(" instead")
            );
         }
      }
   }

   private void rememberOnline(ClientPlayNetworkHandler clientPlayNetworkHandler, String string, String string2) {
      if (clientPlayNetworkHandler != null) {
         for (PlayerListEntry var5 : clientPlayNetworkHandler.getPlayerList()) {
            GameProfile var6 = var5.getProfile();
            String var7 = NameMatcher.nameCore(var6 != null ? var6.name() : null);
            if (var7 != null && !var7.equalsIgnoreCase(string) && !var7.equalsIgnoreCase(string2)) {
               String var8 = AliasPolicy.skeleton(var7);
               this.seenSkeletons.remove(var8);
               this.seenSkeletons.add(var8);
            }
         }

         Iterator var9 = this.seenSkeletons.iterator();

         while (this.seenSkeletons.size() > 8192 && var9.hasNext()) {
            var9.next();
            var9.remove();
         }
      }
   }

   private void trackOthers(ClientPlayNetworkHandler clientPlayNetworkHandler, List<String> list) {
      if (clientPlayNetworkHandler != null) {
         int var3 = 0;

         for (PlayerListEntry var5 : clientPlayNetworkHandler.getListedPlayerListEntries()) {
            GameProfile var7 = var5.getProfile();
            String var6;
            if (var7 != null
               && (var7.id() == null || var7.id().version() != 2)
               && isLikelyPlayer(var6 = NameMatcher.nameCore(var7.name()))
               && !isOwn(var6, list)) {
               String var8 = var6.toLowerCase(Locale.ROOT);
               this.knownOthers.remove(var8);
               this.knownOthers.put(var8, var6);
               this.numbers.computeIfAbsent(var8, string -> this.nextNumber++);
               var3++;
            }
         }

         Iterator var9 = this.knownOthers.keySet().iterator();

         while (this.knownOthers.size() > var3 + 256 && var9.hasNext()) {
            this.numbers.remove(var9.next());
            var9.remove();
         }
      }
   }

   private static void put(Map<String, String> map, String string, String string2) {
      String var3 = NameMatcher.nameCore(string);
      if (var3 != null && !string2.equalsIgnoreCase(var3)) {
         map.putIfAbsent(var3, string2);
      }
   }

   private static boolean isLikelyPlayer(String string) {
      if (string != null && string.length() >= 3) {
         for (int var1 = 0; var1 < string.length(); var1++) {
            if (Character.isLetter(string.charAt(var1))) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean isOwn(String string, List<String> list) {
      for (String var3 : list) {
         if (var3.equalsIgnoreCase(string)) {
            return true;
         }
      }

      return false;
   }

   private static String profileName(MinecraftClient minecraftClient) {
      return minecraftClient != null && minecraftClient.player != null ? minecraftClient.player.getGameProfile().name() : null;
   }

   private static int listSize(ClientPlayNetworkHandler clientPlayNetworkHandler) {
      return clientPlayNetworkHandler != null ? clientPlayNetworkHandler.getListedPlayerListEntries().size() : -1;
   }

   private static boolean chatOpen(MinecraftClient minecraftClient) {
      return minecraftClient.inGameHud != null && minecraftClient.inGameHud.getChatHud().isChatFocused();
   }
}
