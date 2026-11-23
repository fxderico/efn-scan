package net.efn.client.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.efn.client.module.ModuleRevision;
import net.efn.client.module.StableId;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public abstract class Setting {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private final String name;
   private String id;
   private boolean idBound;
   private long revision;
   private final List<Runnable> changeListeners = new ArrayList<>(1);
   private BooleanSupplier visibility;
   private boolean privateValue;
   private boolean sessionOnly;
   private JsonElement defaultValue;
   private long defaultCheckedRevision = -1L;
   private boolean defaultCheckResult = true;

   protected Setting(String var1) {
      this.name = var1;
      this.id = StableId.legacy(var1);
   }

   public final String id() {
      return this.id;
   }

   public final long revision() {
      return this.revision;
   }

   public final void bindId(String var1) {
      if (this.idBound) {
         throw new IllegalStateException("Setting id already bound");
      } else {
         this.id = StableId.validate(var1);
         this.idBound = true;
      }
   }

   public final String name() {
      return this.name;
   }

   public final Setting onChange(Runnable var1) {
      this.changeListeners.add(var1);
      return this;
   }

   public final void notifyChanged() {
      this.revision++;

      for (int var1 = 0; var1 < this.changeListeners.size(); var1++) {
         Runnable var2 = this.changeListeners.get(var1);

         try {
            if (var2 == ModuleRevision.CHANGED) {
               ModuleRevision.changed();
            } else {
               var2.run();
            }
         } catch (Throwable var4) {
            LOGGER.error("Setting change listener failed for '{}'", this.name, var4);
         }
      }
   }

   public final Setting visibleWhen(BooleanSupplier var1) {
      this.visibility = var1;
      return this;
   }

   public final boolean isVisible() {
      return this.visibility == null || this.visibility.getAsBoolean();
   }

   public final Setting privateValue() {
      this.privateValue = true;
      return this;
   }

   public final boolean isPrivate() {
      return this.privateValue;
   }

   public final Setting sessionOnly() {
      this.sessionOnly = true;
      this.privateValue = true;
      return this;
   }

   public final boolean isSessionOnly() {
      return this.sessionOnly;
   }

   public final boolean isSecret() {
      return false;
   }

   public final void captureDefault() {
      if (this.defaultValue == null) {
         this.defaultValue = this.serialize();
      }
   }

   public final void adoptDefault(JsonElement var1) {
      this.deserialize(var1);
      this.defaultValue = this.serialize();
      this.defaultCheckedRevision = -1L;
   }

   public final boolean isDefault() {
      if (this.defaultValue == null) {
         return true;
      } else {
         if (this.defaultCheckedRevision != this.revision) {
            this.defaultCheckResult = this.defaultValue.equals(this.serialize());
            this.defaultCheckedRevision = this.revision;
         }

         return this.defaultCheckResult;
      }
   }

   public final boolean isDefault(JsonElement var1) {
      return this.defaultValue != null && sameValue(this.defaultValue, var1);
   }

   public static boolean sameValue(JsonElement var0, JsonElement var1) {
      if (var0 == null || var1 == null) {
         return var0 == var1;
      } else if (var0.isJsonPrimitive() && var1.isJsonPrimitive()) {
         JsonPrimitive var8 = var0.getAsJsonPrimitive();
         JsonPrimitive var10 = var1.getAsJsonPrimitive();
         if (var8.isNumber() && var10.isNumber()) {
            try {
               return var8.getAsBigDecimal().compareTo(var10.getAsBigDecimal()) == 0;
            } catch (NumberFormatException var6) {
               return false;
            }
         } else {
            return var8.equals(var10);
         }
      } else if (var0.isJsonArray() && var1.isJsonArray()) {
         JsonArray var7 = var0.getAsJsonArray();
         JsonArray var9 = var1.getAsJsonArray();
         if (var7.size() != var9.size()) {
            return false;
         } else {
            for (int var11 = 0; var11 < var7.size(); var11++) {
               if (!sameValue(var7.get(var11), var9.get(var11))) {
                  return false;
               }
            }

            return true;
         }
      } else if (var0.isJsonObject() && var1.isJsonObject()) {
         JsonObject var2 = var0.getAsJsonObject();
         JsonObject var3 = var1.getAsJsonObject();
         if (!var2.keySet().equals(var3.keySet())) {
            return false;
         } else {
            for (String var5 : var2.keySet()) {
               if (!sameValue(var2.get(var5), var3.get(var5))) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return var0.equals(var1);
      }
   }

   public final void resetToDefault() {
      if (!this.isDefault()) {
         this.deserialize(this.defaultValue);
      }
   }

   public JsonElement compact(JsonElement var1) {
      return var1;
   }

   protected final JsonElement defaultJson() {
      return this.defaultValue;
   }

   public abstract JsonElement serialize();

   public final void deserialize(JsonElement var1) {
      this.deserializeValue(var1);
      this.notifyChanged();
   }

   protected abstract void deserializeValue(JsonElement var1);
}
