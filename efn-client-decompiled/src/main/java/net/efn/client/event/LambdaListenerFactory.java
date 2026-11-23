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

   public static <E extends Event> Listener<E> create(Object var0, Method var1) {
      if (var0 == null) {
         return createStatic(var1);
      } else {
         try {
            return (Listener)createFactory(var1).invoke((Object)var0);
         } catch (RuntimeException var3) {
            throw var3;
         } catch (Throwable var4) {
            throw new RuntimeException("Unexpected error creating listener for method " + var1, var4);
         }
      }
   }

   public static <E extends Event> Listener<E> createStatic(Method var0) {
      validateMethod(var0);

      try {
         Lookup var1 = MethodHandles.privateLookupIn(var0.getDeclaringClass(), LOOKUP);
         MethodHandle var2 = var1.unreflect(var0);
         MethodType var3 = MethodType.methodType(void.class, var0.getParameterTypes()[0]);
         CallSite var4 = LambdaMetafactory.metafactory(var1, "invoke", MethodType.methodType(Listener.class), ERASED_INVOKE_TYPE, var2, var3);
         return (Listener)var4.getTarget().invoke();
      } catch (Throwable var5) {
         throw wrap(var0, var5);
      }
   }

   public static MethodHandle createFactory(Method var0) {
      validateMethod(var0);

      try {
         Lookup var1 = MethodHandles.privateLookupIn(var0.getDeclaringClass(), LOOKUP);
         MethodHandle var2 = var1.unreflect(var0);
         MethodType var3 = MethodType.methodType(void.class, var0.getParameterTypes()[0]);
         MethodType var4 = MethodType.methodType(Listener.class, var0.getDeclaringClass());
         CallSite var5 = LambdaMetafactory.metafactory(var1, "invoke", var4, ERASED_INVOKE_TYPE, var2, var3);
         return var5.getTarget();
      } catch (Throwable var6) {
         throw wrap(var0, var6);
      }
   }

   private static RuntimeException wrap(Method var0, Throwable var1) {
      if (var1 instanceof LambdaConversionException) {
         return new RuntimeException(
            "LambdaMetafactory failed for method " + var0 + ". Ensure the listener method is public and its parameter type is a concrete Event subclass.", var1
         );
      } else {
         return var1 instanceof IllegalAccessException
            ? new RuntimeException("Cannot access method " + var0 + ". Listener methods must be public and their declaring class must be accessible.", var1)
            : new RuntimeException("Unexpected error creating listener for method " + var0, var1);
      }
   }

   private static void validateMethod(Method var0) {
      if (var0.getReturnType() != void.class) {
         throw new IllegalArgumentException("@EventListener method must return void: " + var0);
      } else {
         Class[] var1 = var0.getParameterTypes();
         if (var1.length != 1) {
            throw new IllegalArgumentException("@EventListener method must have exactly one parameter: " + var0);
         } else if (!Event.class.isAssignableFrom(var1[0])) {
            throw new IllegalArgumentException("@EventListener parameter must be a subclass of Event: " + var0);
         }
      }
   }
}
