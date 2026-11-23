package net.efn.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.efn.client.event.Event;
import net.efn.client.event.EventBus;
import net.efn.client.event.EventPriority;
import net.efn.client.event.EventType;
import net.efn.client.event.ListenerHandle;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.ModuleToggleEvent;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   public static final String SHOW_IN_HUD_SETTING = "Show in HUD";
   private final String id;
   private final String name;
   private final String description;
   private final ModuleCategory category;
   private boolean enabled;
   private boolean enabling;
   private boolean disabling;
   private boolean disableRequested;
   private final List<Setting> settings = new ArrayList<>();
   private final List<Setting> settingsView = Collections.unmodifiableList(this.settings);
   private final BooleanSetting showInHud;
   private final BooleanSetting toggleMessage;
   private BooleanSetting notifications;
   private final List<ListenerHandle<?>> ownedListeners = new ArrayList<>();
   private boolean busRegistered;
   volatile boolean registered;
   private int keybind = -1;
   private boolean holdMode;

   protected ClientModule(String var1, String var2, ModuleCategory var3) {
      this(StableId.legacy(var1), var1, var2, var3);
   }

   protected ClientModule(String var1, String var2, String var3, ModuleCategory var4) {
      this.id = StableId.validate(var1);
      this.name = var2;
      this.description = var3;
      this.category = Objects.requireNonNull(var4, "category");
      this.showInHud = var4 != ModuleCategory.CLIENT ? this.addSetting("show-in-hud", new BooleanSetting("Show in HUD", true)) : null;
      BooleanSetting var5 = this.toggleMessage = var4 != ModuleCategory.CLIENT
         ? this.addSetting("toggle-message", new BooleanSetting("Toggle Message", true))
         : null;
      if (this.toggleMessage != null) {
         this.toggleMessage.visibleWhen(() -> !this.isAction());
      }
   }

   protected final <S extends Setting> S addSetting(S var1) {
      return this.addSetting(var1.id(), (S)var1);
   }

   protected final <S extends Setting> S addSetting(String var1, S var2) {
      return this.insertSetting(this.settings.size(), var1, (S)var2);
   }

   private <S extends Setting> S insertSetting(int var1, String var2, S var3) {
      for (Setting var5 : this.settings) {
         if (var5.id().equals(var2)) {
            throw new IllegalArgumentException("Duplicate setting id: " + this.id + "/" + var2);
         }
      }

      var3.bindId(var2);
      var3.onChange(ModuleRevision.CHANGED);
      this.settings.add(var1, var3);
      ModuleRevision.changed();
      return (S)var3;
   }

   protected final BooleanSetting usesNotifications() {
      if (this.notifications == null) {
         int var1 = (this.showInHud != null ? 1 : 0) + (this.toggleMessage != null ? 1 : 0);
         this.notifications = this.insertSetting(var1, "notifications", new BooleanSetting("Notifications", true));
      }

      return this.notifications;
   }

   public final boolean notificationsOn() {
      return this.notifications == null || this.notifications.get();
   }

   public final boolean toggleMessageOn() {
      return this.toggleMessage != null && this.toggleMessage.get() && !this.isAction();
   }

   public final String id() {
      return this.id;
   }

   public final String name() {
      return this.name;
   }

   public final String description() {
      return this.description;
   }

   public final ModuleCategory category() {
      return this.category;
   }

   public final List<Setting> settings() {
      return this.settingsView;
   }

   public final void captureDefaults() {
      for (Setting var2 : this.settings) {
         var2.captureDefault();
      }
   }

   public final boolean hasModifiedSettings() {
      for (Setting var2 : this.settings) {
         if (!var2.isDefault()) {
            return true;
         }
      }

      return false;
   }

   public final void resetSettings() {
      for (Setting var2 : this.settings) {
         var2.resetToDefault();
      }
   }

   public final BooleanSetting showInHud() {
      return this.showInHud;
   }

   public boolean persistEnabled() {
      return true;
   }

   public boolean enabledByDefault() {
      return false;
   }

   public boolean inConfigs() {
      return true;
   }

   public int settingsVersion() {
      return 0;
   }

   public void migrateSettings(int var1) {
   }

   public boolean isAction() {
      return false;
   }

   public void onClick() {
   }

   public final boolean isEnabled() {
      return this.enabled;
   }

   public final void setEnabled(boolean var1) {
      boolean var2 = this.enabled;
      this.switchTo(var1);
      if (this.enabled != var2) {
         ModuleToggleEvent.dispatch(this);
      }
   }

   private void switchTo(boolean var1) {
      if (this.enabled != var1 && !this.disabling) {
         this.enabled = var1;
         ModuleRevision.changed();
         if (this.registered) {
            ModuleRegistry.switched(this);
         }

         if (var1) {
            this.enabling = true;
            this.disableRequested = false;

            try {
               this.busRegistered = EventBus.INSTANCE.register(this) > 0;
               this.onEnable();
               this.enabling = false;
               if (this.disableRequested) {
                  this.disableRequested = false;
                  this.switchTo(false);
               }
            } catch (Throwable var39) {
               this.enabling = false;
               this.disableRequested = false;

               try {
                  this.switchTo(false);
               } catch (Throwable var38) {
                  if (var38 != var39) {
                     var39.addSuppressed(var38);
                  }

                  if (var38 instanceof Error var4) {
                     throw var4;
                  }
               }

               LOGGER.error("Failed to enable module '{}'; activation rolled back", this.name, var39);
               if (var39 instanceof Error var3) {
                  throw var3;
               }
            }
         } else {
            this.disabling = true;

            try {
               this.onDisable();
               return;
            } catch (RuntimeException var40) {
               LOGGER.error("Failed to deactivate module '{}'", this.name, var40);
            } finally {
               try {
                  this.ownedListeners.forEach(ListenerHandle::unregister);
                  this.ownedListeners.clear();
                  if (this.busRegistered) {
                     this.busRegistered = false;
                     EventBus.INSTANCE.unregister(this);
                  }
               } finally {
                  this.disabling = false;
               }
            }
         }
      }
   }

   public final void toggle() {
      this.setEnabled(!this.enabled);
   }

   protected final void requestDisable() {
      if (this.enabling) {
         this.disableRequested = true;
      } else {
         this.setEnabled(false);
      }
   }

   public final int keybind() {
      return this.keybind;
   }

   public final void setKeybind(int var1) {
      if (this.keybind != var1) {
         this.keybind = var1;
         ModuleRevision.changed();
      }
   }

   public final boolean isHoldMode() {
      return this.holdMode;
   }

   public final void setHoldMode(boolean var1) {
      if (this.holdMode != var1) {
         this.holdMode = var1;
         ModuleRevision.changed();
      }
   }

   protected void onEnable() {
   }

   protected void onDisable() {
   }

   protected <E extends Event> void on(EventType<E> var1, Consumer<E> var2) {
      this.on(var1, Stage.PRE, EventPriority.NORMAL, var2);
   }

   protected <E extends Event> void on(EventType<E> var1, Stage var2, Consumer<E> var3) {
      this.on(var1, var2, EventPriority.NORMAL, var3);
   }

   protected <E extends Event> void on(EventType<E> var1, Stage var2, EventPriority var3, Consumer<E> var4) {
      if (!this.enabled) {
         throw new IllegalStateException("Cannot register a listener on disabled module: " + this.name);
      } else {
         this.ownedListeners.add(var1.listen(this, var2, var3, var4));
      }
   }
}
