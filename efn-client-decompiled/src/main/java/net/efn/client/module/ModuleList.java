package net.efn.client.module;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class ModuleList extends AbstractList<ClientModule> implements RandomAccess {
   static final ModuleList EMPTY = new ModuleList(new ClientModule[0]);
   private final ClientModule[] modules;

   private ModuleList(ClientModule[] var1) {
      this.modules = var1;
   }

   static ModuleList of(Collection<ClientModule> var0) {
      return var0.isEmpty() ? EMPTY : new ModuleList(var0.toArray(new ClientModule[0]));
   }

   ModuleList with(ClientModule var1) {
      ClientModule[] var2 = Arrays.copyOf(this.modules, this.modules.length + 1);
      var2[this.modules.length] = var1;
      return new ModuleList(var2);
   }

   ModuleList without(ClientModule var1) {
      int var2 = this.indexOfIdentity(var1);
      if (var2 < 0) {
         return this;
      } else if (this.modules.length == 1) {
         return EMPTY;
      } else {
         ClientModule[] var3 = new ClientModule[this.modules.length - 1];
         System.arraycopy(this.modules, 0, var3, 0, var2);
         System.arraycopy(this.modules, var2 + 1, var3, var2, this.modules.length - var2 - 1);
         return new ModuleList(var3);
      }
   }

   boolean containsIdentity(ClientModule var1) {
      return this.indexOfIdentity(var1) >= 0;
   }

   private int indexOfIdentity(ClientModule var1) {
      for (int var2 = 0; var2 < this.modules.length; var2++) {
         if (this.modules[var2] == var1) {
            return var2;
         }
      }

      return -1;
   }

   public ClientModule get(int var1) {
      return this.modules[var1];
   }

   @Override
   public int size() {
      return this.modules.length;
   }

   @Override
   public Iterator<ClientModule> iterator() {
      return new Iterator<ClientModule>() {
         private int next;

         @Override
         public boolean hasNext() {
            return this.next < ModuleList.this.modules.length;
         }

         public ClientModule next() {
            if (this.next >= ModuleList.this.modules.length) {
               throw new NoSuchElementException();
            } else {
               return ModuleList.this.modules[this.next++];
            }
         }
      };
   }

   @Override
   public void forEach(Consumer<? super ClientModule> var1) {
      for (ClientModule var5 : this.modules) {
         var1.accept(var5);
      }
   }

   @Override
   public Object[] toArray() {
      return Arrays.copyOf(this.modules, this.modules.length, Object[].class);
   }
}
