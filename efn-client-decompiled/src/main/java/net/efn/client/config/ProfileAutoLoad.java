package net.efn.client.config;

import java.lang.ref.WeakReference;
import java.util.List;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ChallengeToast;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ProfileAutoLoad {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final Object OWNER = new Object();
   private static final long TOAST_WAIT_NANOS = 15000000000L;
   private static WeakReference<ClientConnection> handled = new WeakReference<>(null);
   private static String pendingTitle;
   private static String pendingMessage;
   private static long pendingSince;

   private ProfileAutoLoad() {
   }

   public static void install() {
      PacketAppliedEvent.TYPE.listen(OWNER, var0 -> {
         if (var0.packet instanceof GameJoinS2CPacket) {
            onJoin();
         }
      });
      TickEvent.TYPE.listen(OWNER, var0 -> showPendingToast());
   }

   private static void onJoin() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (!var1.method_1542()) {
         ClientPlayNetworkHandler var2 = var1.method_1562();
         ServerInfo var0 = var2 == null ? null : var2.method_45734();
         if (var0 != null && var0.field_3761 != null) {
            ClientConnection var4 = var2.method_48296();
            if (var4 != handled.get()) {
               handled = new WeakReference<>(var4);
               List var5 = ConfigProfiles.getInstance().loadForServer(var0.field_3761);
               if (!var5.isEmpty()) {
                  LOGGER.info("[EFN] Joined {} - loaded profile {}", var0.field_3761, String.join(", ", var5));
                  pendingTitle = var5.size() == 1 ? "Profile loaded" : "Profiles loaded";
                  pendingMessage = String.join(", ", var5);
                  pendingSince = System.nanoTime();
               }
            }
         }
      }
   }

   private static void showPendingToast() {
      if (pendingTitle != null) {
         MinecraftClient var1 = MinecraftClient.method_1551();
         boolean var0 = System.nanoTime() - pendingSince > 15000000000L;
         if (var0 || var1.field_1724 != null && !(var1.field_1755 instanceof LevelLoadingScreen)) {
            String var3 = pendingTitle;
            String var4 = pendingMessage;
            pendingTitle = null;
            pendingMessage = null;
            if (var1.field_1687 != null) {
               ChallengeToast.addOrUpdate(new ItemStack(Items.field_8674), var3, Text.method_43470(var4), false);
            }
         }
      }
   }
}
