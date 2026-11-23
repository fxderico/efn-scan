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
   private static final Text TITLE = Text.method_43470("Blocked an automatic payment")
      .method_27695(new Formatting[]{Formatting.field_1061, Formatting.field_1067});
   private static final ConcurrentLinkedQueue<PayGuard.Allowance> allowed = new ConcurrentLinkedQueue<>();
   private static volatile long lastScreenMs;
   private static volatile long lastChatMs;
   private static volatile String lastChatShown = "";

   private PayGuard() {
   }

   public static void allowTyped(String var0) {
      if (var0 != null && var0.strip().startsWith("/")) {
         allow(var0);
      }
   }

   public static void allow(String var0) {
      if (isPay(var0)) {
         long var1 = System.currentTimeMillis();
         allowed.removeIf(var2 -> var1 - var2.atMs() > 5000L);
         allowed.offer(new PayGuard.Allowance(normalize(var0), var1));
      }
   }

   public static boolean blocks(Packet<?> var0) {
      String var1;
      if (var0 instanceof CommandExecutionC2SPacket var2) {
         var1 = var2.comp_808();
      } else {
         if (!(var0 instanceof ChatCommandSignedC2SPacket var3)) {
            return false;
         }

         var1 = var3.comp_2532();
      }

      if (isPay(var1) && !consume(var1)) {
         report(var1);
         return true;
      } else {
         return false;
      }
   }

   public static boolean isPay(String var0) {
      String var1 = normalize(var0);
      int var2 = var1.indexOf(32);
      String var3 = var2 < 0 ? var1 : var1.substring(0, var2);
      var3 = var3.substring(var3.indexOf(58) + 1);
      return var3.equals("pay") || var3.equals("epay");
   }

   private static String normalize(String var0) {
      String var1 = var0 == null ? "" : var0.strip();
      if (var1.startsWith("/")) {
         var1 = var1.substring(1).strip();
      }

      return var1.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
   }

   private static boolean consume(String var0) {
      String var1 = normalize(var0);
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

   private static void report(String var0) {
      String var1 = var0.strip();
      String var2 = "/" + (var1.length() > 64 ? var1.substring(0, 64) + "..." : var1);
      LOGGER.warn("[EFN] Pay Guard blocked \"{}\" sent by {}", var2, caller());
      MinecraftClient var3 = MinecraftClient.method_1551();
      var3.execute(() -> warn(var3, var2));
   }

   private static String caller() {
      return StackWalker.getInstance()
         .walk(
            var0 -> var0.map(StackFrame::getClassName)
               .filter(
                  var0x -> !var0x.startsWith("net.efn.client.security.")
                     && !var0x.startsWith("net.minecraft.")
                     && !var0x.startsWith("com.mojang.")
                     && !var0x.startsWith("io.netty.")
                     && !var0x.startsWith("java.")
                     && !var0x.startsWith("jdk.")
                     && !var0x.startsWith("org.spongepowered.")
                     && !var0x.startsWith("com.example.client.mixin.")
               )
               .findFirst()
               .orElse("unknown code")
         );
   }

   private static void warn(MinecraftClient var0, String var1) {
      long var2 = System.currentTimeMillis();
      if (var0.field_1705 != null && (var2 - lastChatMs >= 3000L || !var1.equals(lastChatShown))) {
         lastChatMs = var2;
         lastChatShown = var1;
         var0.field_1705
            .method_1743()
            .method_1812(
               Text.method_43473()
                  .method_10852(Text.method_43470("[EFN] ").method_27695(new Formatting[]{Formatting.field_1079, Formatting.field_1067}))
                  .method_10852(
                     Text.method_43470(
                           "Blocked "
                              + var1
                              + ". Something tried to send a payment command on its own without you typing it. If you didn't do this yourself, something else on your PC may be malicious."
                        )
                        .method_27692(Formatting.field_1061)
                  )
            );
      }

      if (!(var0.field_1755 instanceof PayGuard.WarningScreen) && var2 - lastScreenMs >= 30000L) {
         lastScreenMs = var2;
         var0.method_1507(new PayGuard.WarningScreen(var0.field_1755, var1));
      }
   }

   @Environment(EnvType.CLIENT)
   private record Allowance(String command, long atMs) {
   }

   @Environment(EnvType.CLIENT)
   private static final class WarningScreen extends ConfirmScreen {
      WarningScreen(Screen var1, String var2) {
         super(
            var1x -> {
               if (var1x) {
                  Util.method_668().method_670("https://discord.gg/pxHtFp5b6h");
               }

               MinecraftClient.method_1551().method_1507(var1);
            },
            PayGuard.TITLE,
            Text.method_43470(
               "Something on this PC tried to send "
                  + var2
                  + " by itself, without you typing it. It was blocked.\n\nIf you didn't do this yourself, something else running on your computer may be malicious. Consider scanning your PC.\n\nIf you think this is a false positive, report it in the EFN Discord."
            ),
            Text.method_43470("Open EFN Discord"),
            Text.method_43470("Close")
         );
      }
   }
}
