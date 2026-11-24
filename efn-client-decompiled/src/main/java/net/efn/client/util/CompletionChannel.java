package net.efn.client.util;

import com.mojang.brigadier.suggestion.Suggestion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;

@Environment(EnvType.CLIENT)
public final class CompletionChannel {
   public static final CompletionChannel INSTANCE = new CompletionChannel();
   private static final int WIRE_ID = 0;
   private static final long FORGET_MS = 15000L;
   private static final long FOREIGN_MS = 4000L;
   private static final Object FOREIGN = new Object();
   private final Object lock = new Object();
   private final LinkedList<CompletionChannel.Request> line = new LinkedList<>();
   private final Map<Integer, CompletionChannel.Request> byKey = new HashMap<>();
   private Object connection;
   private int nextKey = 1;
   private boolean sending;
   private boolean installed;

   private CompletionChannel() {
   }

   public static void install() {
      if (!INSTANCE.installed) {
         INSTANCE.installed = true;
         PacketSendEvent.TYPE.listen(CompletionChannel.class, packetSendEvent -> INSTANCE.outgoing(packetSendEvent.packet));
         PacketReceiveNettyEvent.TYPE.listen(CompletionChannel.class, packetReceiveNettyEvent -> {
            if (INSTANCE.ingest(packetReceiveNettyEvent.packet)) {
               packetReceiveNettyEvent.setCancelled(true);
            }
         });
      }
   }

   public int fire(Object object, String string, CompletionChannel.Form form) {
      ClientPlayNetworkHandler var5 = MinecraftClient.getInstance().getNetworkHandler();
      if (var5 != null && object != null && string != null) {
         Object var6 = this.lock;
         CompletionChannel.Request var4;
         synchronized (var6) {
            this.follow(var5);
            int var8 = this.nextKey++;
            if (this.nextKey == 2147483647) {
               this.nextKey = 1;
            }

            var4 = new CompletionChannel.Request(var8, object, want(string), System.currentTimeMillis(), form == CompletionChannel.Form.BOTH ? 2 : 1);
            this.line.addLast(var4);
            this.byKey.put(var8, var4);
         }

         this.sending = true;

         byte var10;
         try {
            if (form != CompletionChannel.Form.BARE) {
               var5.sendPacket(new RequestCommandCompletionsC2SPacket(0, "/" + string));
            }

            if (form != CompletionChannel.Form.SLASH) {
               var5.sendPacket(new RequestCommandCompletionsC2SPacket(0, string));
            }

            return var4.key;
         } catch (RuntimeException var17) {
            Object var20 = this.lock;
            synchronized (var20) {
               this.line.remove(var4);
               this.byKey.remove(var4.key);
            }

            byte var9 = -1;
            var10 = var9;
         } finally {
            this.sending = false;
         }

         return var10;
      } else {
         return -1;
      }
   }

   public List<String> poll(int i) {
      Object var2 = this.lock;
      synchronized (var2) {
         CompletionChannel.Request var4 = this.byKey.get(i);
         return var4 != null && !var4.forgotten ? var4.names : null;
      }
   }

   public void forget(int i) {
      Object var2 = this.lock;
      synchronized (var2) {
         CompletionChannel.Request var4 = this.byKey.remove(i);
         if (var4 != null) {
            var4.forgotten = true;
            if (!var4.waiting()) {
               this.line.remove(var4);
            }
         }
      }
   }

   public void prune(long l) {
      Object var3 = this.lock;
      synchronized (var3) {
         this.expire(l);
      }
   }

   public void clear(Object object) {
      Object var2 = this.lock;
      synchronized (var2) {
         Iterator var4 = this.line.iterator();

         while (var4.hasNext()) {
            CompletionChannel.Request var5 = (CompletionChannel.Request)var4.next();
            if (var5.owner == object) {
               var5.forgotten = true;
               this.byKey.remove(var5.key);
               if (!var5.waiting()) {
                  var4.remove();
               }
            }
         }
      }
   }

   private void outgoing(Packet<?> packet) {
      if (!this.sending && packet instanceof RequestCommandCompletionsC2SPacket var2) {
         ClientPlayNetworkHandler var3 = MinecraftClient.getInstance().getNetworkHandler();
         Object var4 = this.lock;
         synchronized (var4) {
            this.follow(var3);
            this.line.addLast(new CompletionChannel.Request(0, FOREIGN, want(var2.getPartialCommand()), System.currentTimeMillis(), 1));
         }
      }
   }

   private boolean ingest(Packet<?> packet) {
      if (!(packet instanceof CommandSuggestionsS2CPacket var2)) {
         return false;
      } else {
         ArrayList var3 = new ArrayList();

         for (Suggestion var5 : var2.getSuggestions().getList()) {
            String var6 = var5.getText();
            if (var6 != null && !var6.isBlank()) {
               var3.add(var6.trim());
            }
         }

         String var13 = var3.isEmpty() ? "" : core((String)var3.get(0));
         String var14 = var3.isEmpty() ? "" : core((String)var3.get(var3.size() - 1));
         Object var15 = this.lock;
         synchronized (var15) {
            this.expire(System.currentTimeMillis());
            CompletionChannel.Request var8 = null;

            for (CompletionChannel.Request var10 : this.line) {
               if (var10.waiting() && (var3.isEmpty() || var13.startsWith(var10.want) && var14.startsWith(var10.want))) {
                  var8 = var10;
                  break;
               }
            }

            if (var8 == null) {
               return false;
            } else {
               Iterator var16 = this.line.iterator();

               CompletionChannel.Request var17;
               while (var16.hasNext() && (var17 = (CompletionChannel.Request)var16.next()) != var8) {
                  if (var17.waiting()) {
                     var17.answered = var17.expected;
                     if (var17.owner == FOREIGN || var17.forgotten) {
                        var16.remove();
                     }
                  }
               }

               var8.answered++;
               if (var3.isEmpty() || var8.names != null && !var8.names.isEmpty()) {
                  if (var8.names == null && !var8.waiting()) {
                     var8.names = List.of();
                  }
               } else {
                  var8.names = List.copyOf(var3);
               }

               if (!var8.waiting() && (var8.owner == FOREIGN || var8.forgotten)) {
                  this.line.remove(var8);
               }

               return var8.owner != FOREIGN;
            }
         }
      }
   }

   private void follow(ClientPlayNetworkHandler clientPlayNetworkHandler) {
      if (clientPlayNetworkHandler != this.connection) {
         this.connection = clientPlayNetworkHandler;
         this.line.clear();
         this.byKey.clear();
      }
   }

   private void expire(long l) {
      Iterator var3 = this.line.iterator();

      while (var3.hasNext()) {
         CompletionChannel.Request var4 = (CompletionChannel.Request)var3.next();
         long var5 = l - var4.at;
         if (var5 > 15000L || var4.owner == FOREIGN && var5 > 4000L) {
            var3.remove();
            if (var4.owner != FOREIGN) {
               this.byKey.remove(var4.key, var4);
            }
         }
      }
   }

   static String want(String string) {
      String var2 = string == null ? "" : string.trim();
      if (var2.startsWith("/")) {
         var2 = var2.substring(1);
      }

      int var1;
      var2 = (var1 = var2.lastIndexOf(32)) >= 0 ? var2.substring(var1 + 1) : "";
      return stripDots(var2).toLowerCase(Locale.ROOT);
   }

   public static String core(String string) {
      String var1 = string.replaceAll("\u00a7.", "").trim();
      int var2 = var1.lastIndexOf(32);
      if (var2 >= 0) {
         var1 = var1.substring(var2 + 1);
      }

      var1 = var1.replaceAll("^[\"']|[\"']$", "");
      return stripDots(var1).toLowerCase(Locale.ROOT);
   }

   private static String stripDots(String string) {
      int var1 = 0;

      while (var1 < string.length() && string.charAt(var1) == '.') {
         var1++;
      }

      return string.substring(var1);
   }

   @Environment(EnvType.CLIENT)
   public static enum Form {
      BOTH,
      SLASH,
      BARE;

      private Form() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Request {
      final int key;
      final Object owner;
      final String want;
      final long at;
      final int expected;
      int answered;
      List<String> names;
      boolean forgotten;

      Request(int i, Object object, String string, long l, int j) {
         this.key = i;
         this.owner = object;
         this.want = string;
         this.at = l;
         this.expected = j;
      }

      boolean waiting() {
         return this.answered < this.expected;
      }
   }
}
