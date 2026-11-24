package net.efn.client.security;

import java.lang.StackWalker.StackFrame;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class PayGuard {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final long ALLOW_MS = 5000L;
   private static final long SCREEN_GAP_MS = 30000L;
   private static final long CHAT_GAP_MS = 3000L;
   private static final int SHOWN_CHARS = 64;
   private static final Text TITLE = Text.literal("Blocked an automatic payment").formatted(Formatting.RED, Formatting.BOLD);
   private static final ConcurrentLinkedQueue<PayGuard.Allowance> allowed = new ConcurrentLinkedQueue<>();
   private static volatile long lastScreenMs;
   private static volatile long lastChatMs;
   private static volatile String lastChatShown = "";

   private PayGuard() {
   }

   public static void allowTyped(String string) {
      if (string != null && string.strip().startsWith("/")) {
         allow(string);
      }
   }

   public static void allow(String string) {
      if (isPay(string)) {
         long var1 = System.currentTimeMillis();
         allowed.removeIf(allowance -> var1 - allowance.atMs() > 5000L);
         allowed.offer(new PayGuard.Allowance(normalize(string), var1));
      }
   }

   public static boolean blocks(Packet<?> packet) {
      String var1;
      if (packet instanceof CommandExecutionC2SPacket var2) {
         var1 = var2.command();
      } else {
         if (!(packet instanceof ChatCommandSignedC2SPacket var3)) {
            return false;
         }

         var1 = var3.command();
      }

      if (isPay(var1) && !consume(var1)) {
         report(var1);
         return true;
      } else {
         return false;
      }
   }

   public static boolean isPay(String string) {
      String var1 = normalize(string);
      int var2 = var1.indexOf(32);
      String var3 = var2 < 0 ? var1 : var1.substring(0, var2);
      var3 = var3.substring(var3.indexOf(58) + 1);
      return var3.equals("pay") || var3.equals("epay");
   }

   private static String normalize(String string) {
      String var1 = string == null ? "" : string.strip();
      if (var1.startsWith("/")) {
         var1 = var1.substring(1).strip();
      }

      return var1.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
   }

   private static boolean consume(String string) {
      String var1 = normalize(string);
      long var2 = System.currentTimeMillis();
      Iterator var4 = allowed.iterator();

      while (var4.hasNext()) {
         PayGuard.Allowance var5 = (PayGuard.Allowance)var4.next();
         if (var2 - var5.atMs() > 5000L) {
            var4.remove();
         } else if (var5.command().equals(var1)) {
            var4.remove();
            return true;
         }
      }

      return false;
   }

   private static void report(String string) {
      String var1 = string.strip();
      String var2 = "/" + (var1.length() > 64 ? var1.substring(0, 64) + "..." : var1);
      LOGGER.warn("[EFN] Pay Guard blocked \"{}\" sent by {}", var2, caller());
      MinecraftClient var3 = MinecraftClient.getInstance();
      var3.execute(() -> warn(var3, var2));
   }

   private static String caller() {
      return StackWalker.getInstance()
         .walk(
            stream -> stream.map(StackFrame::getClassName)
               .filter(
                  string -> !string.startsWith("net.efn.client.security.")
                     && !string.startsWith("net.minecraft.")
                     && !string.startsWith("com.mojang.")
                     && !string.startsWith("io.netty.")
                     && !string.startsWith("java.")
                     && !string.startsWith("jdk.")
                     && !string.startsWith("org.spongepowered.")
                     && !string.startsWith("com.example.client.mixin.")
               )
               .findFirst()
               .orElse("unknown code")
         );
   }

   private static void warn(MinecraftClient minecraftClient, String string) {
      long var2 = System.currentTimeMillis();
      if (minecraftClient.inGameHud != null && (var2 - lastChatMs >= 3000L || !string.equals(lastChatShown))) {
         lastChatMs = var2;
         lastChatShown = string;
         minecraftClient.inGameHud
            .getChatHud()
            .addMessage(
               Text.empty()
                  .append(Text.literal("[EFN] ").formatted(Formatting.DARK_RED, Formatting.BOLD))
                  .append(
                     Text.literal(
                           "Blocked "
                              + string
                              + ". Something tried to send a payment command on its own without you typing it. If you didn't do this yourself, something else on your PC may be malicious."
                        )
                        .formatted(Formatting.RED)
                  )
            );
      }

      if (!(minecraftClient.currentScreen instanceof PayGuard.WarningScreen) && var2 - lastScreenMs >= 30000L) {
         lastScreenMs = var2;
         minecraftClient.setScreen(new PayGuard.WarningScreen(minecraftClient.currentScreen, string));
      }
   }

   @Environment(EnvType.CLIENT)
   private record Allowance(String command, long atMs) {
   }

   @Environment(EnvType.CLIENT)
   private static final class WarningScreen extends ConfirmScreen {
      WarningScreen(Screen screen, String string) {
         super(
            bl -> {
               if (bl) {
                  Util.getOperatingSystem().open("https://discord.gg/pxHtFp5b6h");
               }

               MinecraftClient.getInstance().setScreen(screen);
            },
            PayGuard.TITLE,
            Text.literal(
               "Something on this PC tried to send "
                  + string
                  + " by itself, without you typing it. It was blocked.\n\nIf you didn't do this yourself, something else running on your computer may be malicious. Consider scanning your PC.\n\nIf you think this is a false positive, report it in the EFN Discord."
            ),
            Text.literal("Open EFN Discord"),
            Text.literal("Close")
         );
      }
   }
}
