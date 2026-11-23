package net.efn.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ActionSetting extends Setting {
   private final Runnable action;

   public ActionSetting(String var1, Runnable var2) {
      super(var1);
      this.action = var2;
   }

   public void run() {
      this.action.run();
   }

   @Override
   public JsonElement serialize() {
      return JsonNull.INSTANCE;
   }

   @Override
   protected void deserializeValue(JsonElement var1) {
   }
}
