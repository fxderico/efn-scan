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
      ClientWorld var1 = MinecraftClient.getInstance().world;
      if (var1 != null) {
         this.applyForcedTime(var1);
      }
   }

   @Override
   protected void onDisable() {
      active = null;
      ClientWorld var1 = MinecraftClient.getInstance().world;
      if (var1 != null && var1 == this.trackedLevel) {
         writeDayTime(var1, this.currentServerDayTime(var1));
      }

      this.trackedLevel = null;
   }

   private void onPostTick(TickEvent tickEvent) {
      ClientWorld var2 = MinecraftClient.getInstance().world;
      if (var2 == null) {
         this.trackedLevel = null;
      } else {
         this.applyForcedTime(var2);
      }
   }

   private void applyForcedTime(ClientWorld clientWorld) {
      this.serverDayTime = clientWorld == this.trackedLevel ? this.currentServerDayTime(clientWorld) : clientWorld.getTimeOfDay();
      this.trackedLevel = clientWorld;
      this.appliedDayTime = this.forcedDayTime();
      writeDayTime(clientWorld, this.appliedDayTime);
   }

   private static void writeDayTime(ClientWorld clientWorld, long l) {
      if (clientWorld.getTimeOfDay() != l) {
         clientWorld.getLevelProperties().setTimeOfDay(l);
         clientWorld.getEnvironmentAttributes().tick();
      }
   }

   private long currentServerDayTime(ClientWorld clientWorld) {
      return this.serverDayTime + (clientWorld.getTimeOfDay() - this.appliedDayTime);
   }

   public long interceptServerDayTime(ClientWorld clientWorld, long l) {
      this.trackedLevel = clientWorld;
      this.serverDayTime = l;
      this.appliedDayTime = this.forcedDayTime();
      return this.appliedDayTime;
   }

   private long forcedDayTime() {
      return (long)this.time.get();
   }
}
