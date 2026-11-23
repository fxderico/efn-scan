package net.efn.client.module.impl.render;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public final class TimeChangerModule extends ClientModule {
   public static volatile TimeChangerModule active;
   private final NumberSetting time = this.addSetting("time", new NumberSetting("Time", 0.0F, -20000.0F, 20000.0F).step(1.0F));
   private ClientWorld trackedLevel;
   private long serverDayTime;
   private long appliedDayTime;

   public TimeChangerModule() {
      super("time-changer", "Time Changer", "Makes you able to set a custom time.", ModuleCategory.RENDER);
   }

   @Override
   protected void onEnable() {
      active = this;
      this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
      ClientWorld var1 = MinecraftClient.method_1551().field_1687;
      if (var1 != null) {
         this.applyForcedTime(var1);
      }
   }

   @Override
   protected void onDisable() {
      active = null;
      ClientWorld var1 = MinecraftClient.method_1551().field_1687;
      if (var1 != null && var1 == this.trackedLevel) {
         writeDayTime(var1, this.currentServerDayTime(var1));
      }

      this.trackedLevel = null;
   }

   private void onPostTick(TickEvent var1) {
      ClientWorld var2 = MinecraftClient.method_1551().field_1687;
      if (var2 == null) {
         this.trackedLevel = null;
      } else {
         this.applyForcedTime(var2);
      }
   }

   private void applyForcedTime(ClientWorld var1) {
      this.serverDayTime = var1 == this.trackedLevel ? this.currentServerDayTime(var1) : var1.method_8532();
      this.trackedLevel = var1;
      this.appliedDayTime = this.forcedDayTime();
      writeDayTime(var1, this.appliedDayTime);
   }

   private static void writeDayTime(ClientWorld var0, long var1) {
      if (var0.method_8532() != var1) {
         var0.method_28104().method_165(var1);
         var0.method_75728().method_76405();
      }
   }

   private long currentServerDayTime(ClientWorld var1) {
      return this.serverDayTime + (var1.method_8532() - this.appliedDayTime);
   }

   public long interceptServerDayTime(ClientWorld var1, long var2) {
      this.trackedLevel = var1;
      this.serverDayTime = var2;
      this.appliedDayTime = this.forcedDayTime();
      return this.appliedDayTime;
   }

   private long forcedDayTime() {
      return (long)this.time.get();
   }
}
