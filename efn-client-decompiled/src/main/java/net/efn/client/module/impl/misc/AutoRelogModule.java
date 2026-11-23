package net.efn.client.module.impl.misc;

import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.LevelUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class AutoRelogModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final int REARM_HEIGHT = 4;
   private static final int REARM_TICKS = 40;
   private final NumberSetting yLevel = this.addSetting("y-level", new NumberSetting("Y Level", 0.0F, -60.0F, 16.0F).step(1.0F));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 3.0F, 1.0F, 30.0F).step(0.5F));
   private ClientWorld seenLevel;
   private ClientPlayerEntity seenPlayer;
   private int aboveTicks;
   private ServerInfo rejoin;
   private boolean disconnected;
   private int rejoinTicks;

   public AutoRelogModule() {
      super("auto-relog", "Auto Relog", "Relogs once when you go down to deepslate level.", ModuleCategory.DONUT);
   }

   @Override
   protected void onEnable() {
      this.forget();
      this.on(TickEvent.TYPE, var1 -> this.onTick());
   }

   @Override
   protected void onDisable() {
      this.forget();
   }

   private void forget() {
      this.seenLevel = null;
      this.seenPlayer = null;
      this.aboveTicks = 0;
      this.rejoin = null;
      this.disconnected = false;
   }

   private void onTick() {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (this.rejoin != null) {
         this.waitToRejoin(var2);
      } else {
         ClientPlayerEntity var3 = var2.field_1724;
         ClientWorld var4 = var2.field_1687;
         if (var3 != null && var4 != null && !(var2.field_1755 instanceof LevelLoadingScreen)) {
            if (var4 != this.seenLevel || var3 != this.seenPlayer) {
               this.seenLevel = var4;
               this.seenPlayer = var3;
               this.aboveTicks = 0;
            }

            if (!LevelUtil.isOverworld(var4)) {
               this.aboveTicks = 0;
            } else {
               double var5 = var3.method_23318();
               float var1;
               if (var5 >= (var1 = this.yLevel.get()) + 4.0F) {
                  if (this.aboveTicks < 40) {
                     this.aboveTicks++;
                  }
               } else if (var5 < var1) {
                  boolean var7 = this.aboveTicks >= 40;
                  this.aboveTicks = 0;
                  if (var7) {
                     this.relog(var2, var5);
                  }
               }
            }
         } else {
            this.aboveTicks = 0;
         }
      }
   }

   private void relog(MinecraftClient var1, double var2) {
      ServerInfo var4 = var1.method_1542() ? null : var1.method_1558();
      if (var4 != null && !var4.method_52811()) {
         LOGGER.info("[EFN] Auto Relog: reached Y {}, relogging", (int)Math.floor(var2));
         this.rejoin = var4;
         this.disconnected = false;
         this.rejoinTicks = Math.round(this.delay.get() * 20.0F);
         var1.method_63588(() -> {
            if (this.rejoin == var4 && var1.field_1687 != null) {
               var1.method_73360(ClientWorld.field_61021);
               this.disconnected = true;
            }
         });
      }
   }

   private void waitToRejoin(MinecraftClient var1) {
      if (this.disconnected) {
         if (var1.field_1687 == null && var1.field_1755 instanceof MultiplayerScreen) {
            if (--this.rejoinTicks <= 0) {
               ServerInfo var2 = this.rejoin;
               Screen var3 = var1.field_1755;
               this.rejoin = null;
               var1.method_63588(() -> {
                  if (var1.field_1755 == var3 && var1.field_1687 == null) {
                     ConnectScreen.method_36877(var3, var1, ServerAddress.method_2950(var2.field_3761), var2, false, null);
                  }
               });
            }
         } else {
            this.rejoin = null;
         }
      }
   }
}
