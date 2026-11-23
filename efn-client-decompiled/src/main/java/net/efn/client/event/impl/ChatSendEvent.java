package net.efn.client.event.impl;

import net.efn.client.event.CancellableEvent;
import net.efn.client.event.CancellableEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ChatSendEvent extends CancellableEvent {
   public static final CancellableEventType<ChatSendEvent> TYPE = new CancellableEventType<>("ChatSendEvent");
   public String message;
   private static final ChatSendEvent INSTANCE = new ChatSendEvent();

   private ChatSendEvent() {
   }

   public static boolean dispatch(String var0) {
      if (!TYPE.hasPreListeners()) {
         return false;
      } else {
         INSTANCE.message = var0;
         INSTANCE.resetCancellation();
         TYPE.dispatch(INSTANCE);
         boolean var1 = INSTANCE.isCancelled();
         INSTANCE.message = null;
         return var1;
      }
   }
}
