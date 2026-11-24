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
      PacketAppliedEvent.TYPE.listen(OWNER, packetAppliedEvent -> {
         if (packetAppliedEvent.packet instanceof GameJoinS2CPacket) {
            onJoin();
         }
      });
      TickEvent.TYPE.listen(OWNER, tickEvent -> showPendingToast());
   }

   private static void onJoin() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (!var1.isInSingleplayer()) {
         ClientPlayNetworkHandler var2 = var1.getNetworkHandler();
         ServerInfo var0 = var2 == null ? null : var2.getServerInfo();
         if (var0 != null && var0.address != null) {
            ClientConnection var4 = var2.getConnection();
            if (var4 != handled.get()) {
               handled = new WeakReference<>(var4);
               List var5 = ConfigProfiles.getInstance().loadForServer(var0.address);
               if (!var5.isEmpty()) {
                  LOGGER.info("[EFN] Joined {} - loaded profile {}", var0.address, String.join(", ", var5));
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
         MinecraftClient var1 = MinecraftClient.getInstance();
         boolean var0 = System.nanoTime() - pendingSince > 15000000000L;
         if (var0 || var1.player != null && !(var1.currentScreen instanceof LevelLoadingScreen)) {
            String var3 = pendingTitle;
            String var4 = pendingMessage;
            pendingTitle = null;
            pendingMessage = null;
            if (var1.world != null) {
               ChallengeToast.addOrUpdate(new ItemStack(Items.WRITABLE_BOOK), var3, Text.literal(var4), false);
            }
         }
      }
   }
}
