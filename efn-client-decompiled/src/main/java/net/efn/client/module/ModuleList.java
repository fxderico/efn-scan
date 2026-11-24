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

   private ModuleList(ClientModule[] clientModules) {
      this.modules = clientModules;
   }

   static ModuleList of(Collection<ClientModule> collection) {
      return collection.isEmpty() ? EMPTY : new ModuleList(collection.toArray(new ClientModule[0]));
   }

   ModuleList with(ClientModule clientModule) {
      ClientModule[] var2 = Arrays.copyOf(this.modules, this.modules.length + 1);
      var2[this.modules.length] = clientModule;
      return new ModuleList(var2);
   }

   ModuleList without(ClientModule clientModule) {
      int var2 = this.indexOfIdentity(clientModule);
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

   boolean containsIdentity(ClientModule clientModule) {
      return this.indexOfIdentity(clientModule) >= 0;
   }

   private int indexOfIdentity(ClientModule clientModule) {
      for (int var2 = 0; var2 < this.modules.length; var2++) {
         if (this.modules[var2] == clientModule) {
            return var2;
         }
      }

      return -1;
   }

   public ClientModule get(int i) {
      return this.modules[i];
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
   public void forEach(Consumer<? super ClientModule> consumer) {
      for (ClientModule var5 : this.modules) {
         consumer.accept(var5);
      }
   }

   @Override
   public Object[] toArray() {
      return Arrays.copyOf(this.modules, this.modules.length, Object[].class);
   }
}
