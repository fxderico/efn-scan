package net.efn.client.event;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class EventType<E extends Event> {
   protected final ListenerList<E> list = new ListenerList<>();
   private final String name;

   public EventType() {
      this("unnamed");
   }

   public EventType(String string) {
      this.name = string;
   }

   public final String name() {
      return this.name;
   }

   public void register(ListenerHandle<E> listenerHandle) {
      this.list.register(listenerHandle);
   }

   public void unregister(ListenerHandle<E> listenerHandle) {
      this.list.unregister(listenerHandle);
   }

   public ListenerHandle<E> listen(Object object, Consumer<E> consumer) {
      return this.listen(object, Stage.PRE, EventPriority.NORMAL, consumer);
   }

   public ListenerHandle<E> listen(Object object, EventPriority eventPriority, Consumer<E> consumer) {
      return this.listen(object, Stage.PRE, eventPriority, consumer);
   }

   public ListenerHandle<E> listen(Object object, Stage stage, EventPriority eventPriority, Consumer<E> consumer) {
      ListenerHandle var5 = new ListenerHandle<>(this, stage, eventPriority, object, consumer);
      this.register(var5);
      return var5;
   }

   public boolean hasPreListeners() {
      return this.list.preCallbacks().length != 0;
   }

   public boolean hasPostListeners() {
      return this.list.postCallbacks().length != 0;
   }

   public boolean hasListeners() {
      return this.hasPreListeners() || this.hasPostListeners();
   }

   public final void dispatch(E event) {
      this.dispatchPre((E)event);
   }

   public void dispatchPre(E event) {
      this.list.dispatchPre((E)event);
   }

   public void dispatchPost(E event) {
      this.list.dispatchPost((E)event);
   }
}
