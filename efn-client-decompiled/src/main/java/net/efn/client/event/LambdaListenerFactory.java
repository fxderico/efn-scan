package net.efn.client.event;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class LambdaListenerFactory {
   private static final Lookup LOOKUP = MethodHandles.lookup();
   private static final MethodType ERASED_INVOKE_TYPE = MethodType.methodType(void.class, Event.class);

   private LambdaListenerFactory() {
   }

   public static <E extends Event> Listener<E> create(Object object, Method method) {
      if (object == null) {
         return createStatic(method);
      } else {
         try {
            return (Listener)createFactory(method).invoke((Object)object);
         } catch (RuntimeException var3) {
            throw var3;
         } catch (Throwable var4) {
            throw new RuntimeException("Unexpected error creating listener for method " + method, var4);
         }
      }
   }

   public static <E extends Event> Listener<E> createStatic(Method method) {
      validateMethod(method);

      try {
         Lookup var1 = MethodHandles.privateLookupIn(method.getDeclaringClass(), LOOKUP);
         MethodHandle var2 = var1.unreflect(method);
         MethodType var3 = MethodType.methodType(void.class, method.getParameterTypes()[0]);
         CallSite var4 = LambdaMetafactory.metafactory(var1, "invoke", MethodType.methodType(Listener.class), ERASED_INVOKE_TYPE, var2, var3);
         return (Listener)var4.getTarget().invoke();
      } catch (Throwable var5) {
         throw wrap(method, var5);
      }
   }

   public static MethodHandle createFactory(Method method) {
      validateMethod(method);

      try {
         Lookup var1 = MethodHandles.privateLookupIn(method.getDeclaringClass(), LOOKUP);
         MethodHandle var2 = var1.unreflect(method);
         MethodType var3 = MethodType.methodType(void.class, method.getParameterTypes()[0]);
         MethodType var4 = MethodType.methodType(Listener.class, method.getDeclaringClass());
         CallSite var5 = LambdaMetafactory.metafactory(var1, "invoke", var4, ERASED_INVOKE_TYPE, var2, var3);
         return var5.getTarget();
      } catch (Throwable var6) {
         throw wrap(method, var6);
      }
   }

   private static RuntimeException wrap(Method method, Throwable throwable) {
      if (throwable instanceof LambdaConversionException) {
         return new RuntimeException(
            "LambdaMetafactory failed for method " + method + ". Ensure the listener method is public and its parameter type is a concrete Event subclass.",
            throwable
         );
      } else {
         return throwable instanceof IllegalAccessException
            ? new RuntimeException(
               "Cannot access method " + method + ". Listener methods must be public and their declaring class must be accessible.", throwable
            )
            : new RuntimeException("Unexpected error creating listener for method " + method, throwable);
      }
   }

   private static void validateMethod(Method method) {
      if (method.getReturnType() != void.class) {
         throw new IllegalArgumentException("@EventListener method must return void: " + method);
      } else {
         Class[] var1 = method.getParameterTypes();
         if (var1.length != 1) {
            throw new IllegalArgumentException("@EventListener method must have exactly one parameter: " + method);
         } else if (!Event.class.isAssignableFrom(var1[0])) {
            throw new IllegalArgumentException("@EventListener parameter must be a subclass of Event: " + method);
         }
      }
   }
}
