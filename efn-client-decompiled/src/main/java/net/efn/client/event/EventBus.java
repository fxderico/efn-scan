package net.efn.client.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EventBus {
   public static final EventBus INSTANCE = new EventBus();
   private final Map<Object, List<EventBus.RegistrationEntry<?>>> subscriptions = new IdentityHashMap<>();
   private final ArrayBlockingQueue<Runnable> asyncQueue = new ArrayBlockingQueue<>(4096);
   private volatile long failedTasks;
   private static final MethodType FACTORY_TYPE = MethodType.methodType(Listener.class, Object.class);
   private static final ClassValue<List<EventBus.HandlerSpec>> HANDLER_SPECS = new ClassValue<List<EventBus.HandlerSpec>>() {
      protected List<EventBus.HandlerSpec> computeValue(Class<?> var1) {
         return EventBus.scan(var1);
      }
   };
   private static final ClassValue<EventType<?>> TYPE_CACHE = new ClassValue<EventType<?>>() {
      protected EventType<?> computeValue(Class<?> var1) {
         try {
            Field var2 = var1.getField("TYPE");
            if (!EventType.class.isAssignableFrom(var2.getType())) {
               throw new IllegalArgumentException(var1.getName() + ".TYPE must be an EventType instance.");
            } else {
               return (EventType<?>)var2.get(null);
            }
         } catch (NoSuchFieldException var3) {
            throw new IllegalArgumentException("Event class " + var1.getName() + " must declare 'public static final EventType<...> TYPE'.", var3);
         } catch (IllegalAccessException var4) {
            throw new IllegalArgumentException("Cannot access " + var1.getName() + ".TYPE", var4);
         }
      }
   };

   public EventBus() {
   }

   public boolean postAsync(Runnable var1) {
      return this.asyncQueue.offer(Objects.requireNonNull(var1));
   }

   public int drainAsync() {
      return this.drainAsync(512, 2000000L);
   }

   public int drainAsync(int var1, long var2) {
      if (var1 > 0 && var2 > 0L) {
         long var5 = System.nanoTime();
         int var7 = 0;

         Runnable var4;
         while (var7 < var1 && System.nanoTime() - var5 < var2 && (var4 = this.asyncQueue.poll()) != null) {
            var7++;

            try {
               var4.run();
            } catch (RuntimeException var11) {
               long var9;
               if ((var9 = ++this.failedTasks) <= 4L || var9 % 256L == 0L) {
                  System.err.println("[EfnEventBus] Async task failed (#" + var9 + ")");
                  var11.printStackTrace();
               }
            }
         }

         return var7;
      } else {
         throw new IllegalArgumentException("Drain limits must be positive");
      }
   }

   public <E extends Event> void off(ListenerHandle<E> var1) {
      var1.unregister();
   }

   public int register(Object var1) {
      List var2 = HANDLER_SPECS.get(var1.getClass());
      if (var2.isEmpty()) {
         return 0;
      } else {
         synchronized (this) {
            return this.register(var1, var2);
         }
      }
   }

   private int register(Object var1, List<EventBus.HandlerSpec> var2) {
      if (this.subscriptions.containsKey(var1)) {
         throw new IllegalStateException("Subscriber already registered: " + var1.getClass().getName());
      } else {
         List var3 = buildEntries(var1, var2);

         try {
            for (EventBus.RegistrationEntry var11 : var3) {
               var11.register();
            }
         } catch (RuntimeException | Error var9) {
            Error var4 = var9;

            for (EventBus.RegistrationEntry var6 : var3) {
               try {
                  var6.unregister();
               } catch (RuntimeException | Error var8) {
                  if (var4 != var8) {
                     var4.addSuppressed(var8);
                  }
               }
            }

            throw var4;
         }

         this.subscriptions.put(var1, var3);
         return var3.size();
      }
   }

   public synchronized void unregister(Object var1) {
      List var2 = this.subscriptions.remove(var1);
      if (var2 != null) {
         for (EventBus.RegistrationEntry var4 : var2) {
            var4.unregister();
         }
      }
   }

   private static List<EventBus.RegistrationEntry<?>> buildEntries(Object var0, List<EventBus.HandlerSpec> var1) {
      ArrayList var2 = new ArrayList(var1.size());

      for (EventBus.HandlerSpec var4 : var1) {
         Listener var5 = var4.staticListener();
         if (var5 == null) {
            try {
               var5 = castListener((Listener)var4.listenerFactory().invokeExact((Object)var0));
            } catch (Throwable var7) {
               throw new RuntimeException("Failed to bind listener for " + var0.getClass().getName(), var7);
            }
         }

         ListenerHandle var6 = new ListenerHandle<>(var4.type(), var4.stage(), var4.priority(), var0, var5::invoke);
         var2.add(new EventBus.RegistrationEntry<>(var4.type(), var6));
      }

      return var2;
   }

   private static Listener<Event> castListener(Listener<?> var0) {
      return var0;
   }

   private static List<EventBus.HandlerSpec> scan(Class<?> var0) {
      ArrayList var1 = new ArrayList();

      for (Class var2 = var0; var2 != null; var2 = var2.getSuperclass()) {
         for (Method var6 : var2.getDeclaredMethods()) {
            if (isHandler(var6) && !Modifier.isPublic(var6.getModifiers())) {
               throw new IllegalArgumentException("Event handler must be public: " + var6);
            }
         }
      }

      for (Method var16 : var0.getMethods()) {
         Stage var17 = Stage.PRE;
         EventPriority var7 = EventPriority.NORMAL;
         boolean var8 = false;
         EventHandler var9 = var16.getAnnotation(EventHandler.class);
         if (var9 != null) {
            var17 = var9.stage();
            var7 = var9.priority();
            var8 = true;
         } else {
            EventListener var10 = var16.getAnnotation(EventListener.class);
            if (var10 != null) {
               var17 = Stage.PRE;
               var7 = EventPriority.fromInt(var10.priority());
               var8 = true;
            }
         }

         if (var8) {
            Class[] var18 = var16.getParameterTypes();
            if (var18.length != 1 || !Event.class.isAssignableFrom(var18[0]) || var16.getReturnType() != void.class) {
               throw new IllegalArgumentException("Event handler must return void and accept one Event: " + var16);
            }

            Class var11 = var18[0];
            EventType var12 = resolveTypeFor(var11);
            if (Modifier.isStatic(var16.getModifiers())) {
               var1.add(new EventBus.HandlerSpec(var12, var17, var7, null, LambdaListenerFactory.createStatic(var16)));
            } else {
               var1.add(new EventBus.HandlerSpec(var12, var17, var7, LambdaListenerFactory.createFactory(var16).asType(FACTORY_TYPE), null));
            }
         }
      }

      return var1.isEmpty() ? List.of() : List.copyOf(var1);
   }

   private static boolean isHandler(Method var0) {
      return var0.isAnnotationPresent(EventHandler.class) || var0.isAnnotationPresent(EventListener.class);
   }

   static <E extends Event> EventType<E> resolveTypeFor(Class<E> var0) {
      return (EventType<E>)TYPE_CACHE.get(var0);
   }

   @Environment(EnvType.CLIENT)
   private record HandlerSpec(EventType<Event> type, Stage stage, EventPriority priority, MethodHandle listenerFactory, Listener<Event> staticListener) {
   }

   @Environment(EnvType.CLIENT)
   private record RegistrationEntry<E extends Event>(EventType<E> type, ListenerHandle<E> handle) {
      void register() {
         this.type.register(this.handle);
      }

      void unregister() {
         this.type.unregister(this.handle);
      }
   }
}
