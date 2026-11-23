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

   public EventType(String var1) {
      this.name = var1;
   }

   public final String name() {
      return this.name;
   }

   public void register(ListenerHandle<E> var1) {
      this.list.register(var1);
   }

   public void unregister(ListenerHandle<E> var1) {
      this.list.unregister(var1);
   }

   public ListenerHandle<E> listen(Object var1, Consumer<E> var2) {
      return this.listen(var1, Stage.PRE, EventPriority.NORMAL, var2);
   }

   public ListenerHandle<E> listen(Object var1, EventPriority var2, Consumer<E> var3) {
      return this.listen(var1, Stage.PRE, var2, var3);
   }

   public ListenerHandle<E> listen(Object var1, Stage var2, EventPriority var3, Consumer<E> var4) {
      ListenerHandle var5 = new ListenerHandle<>(this, var2, var3, var1, var4);
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

   public final void dispatch(E var1) {
      this.dispatchPre((E)var1);
   }

   public void dispatchPre(E var1) {
      this.list.dispatchPre((E)var1);
   }

   public void dispatchPost(E var1) {
      this.list.dispatchPost((E)var1);
   }
}
