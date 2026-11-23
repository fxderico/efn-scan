package net.efn.client.input;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.IntPredicate;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleRevision;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModuleKeybinds {
   public static final int MOUSE_OFFSET = 1000;
   private static final int KEY_COUNT = 1008;
   private final boolean[] previous = new boolean[1008];
   private final boolean[] current = new boolean[1008];
   private final boolean[] sampled = new boolean[1008];
   private int[] sampledKeys = new int[1008];
   private int[] previousKeys = new int[1008];
   private int sampledCount;
   private int previousCount;
   private final Map<ClientModule, Integer> held = new IdentityHashMap<>();
   private boolean wasAcceptingInput = true;
   private final List<ClientModule> bound = new ArrayList<>();
   private List<ClientModule> boundFrom;
   private long boundRevision;

   public ModuleKeybinds() {
   }

   public void update(List<ClientModule> var1, IntPredicate var2, boolean var3) {
      List var5 = this.boundModules(var1);

      for (int var6 = 0; var6 < this.previousCount; var6++) {
         int var7 = this.previousKeys[var6];
         this.current[var7] = false;
         this.sampled[var7] = false;
      }

      this.sampledCount = 0;

      for (ClientModule var15 : var5) {
         int var8 = var15.keybind();
         if (!this.sampled[var8]) {
            this.current[var8] = var2.test(var8);
            this.sampled[var8] = true;
            this.sampledKeys[this.sampledCount++] = var8;
         }
      }

      if (!this.held.isEmpty()) {
         Iterator var12 = this.held.entrySet().iterator();

         while (var12.hasNext()) {
            Entry var16 = (Entry)var12.next();
            ClientModule var18 = (ClientModule)var16.getKey();
            int var9 = (Integer)var16.getValue();
            if (!var3 || !var18.isHoldMode() || var18.keybind() != var9 || !this.current[var9]) {
               var12.remove();
               var18.setEnabled(false);
            }
         }
      }

      if (var3 && this.wasAcceptingInput) {
         for (ClientModule var17 : var5) {
            int var19 = var17.keybind();
            if (isBindable(var19) && this.current[var19] && !this.previous[var19]) {
               if (var17.isAction()) {
                  var17.onClick();
               } else if (var17.isHoldMode()) {
                  var17.setEnabled(true);
                  if (var17.isEnabled()) {
                     this.held.put(var17, var19);
                  }
               } else {
                  var17.toggle();
               }
            }
         }
      }

      for (int var4 = 0; var4 < this.previousCount; var4++) {
         this.previous[this.previousKeys[var4]] = false;
      }

      for (int var10 = 0; var10 < this.sampledCount; var10++) {
         this.previous[this.sampledKeys[var10]] = this.current[this.sampledKeys[var10]];
      }

      int[] var14 = this.previousKeys;
      this.previousKeys = this.sampledKeys;
      this.sampledKeys = var14;
      this.previousCount = this.sampledCount;
      this.wasAcceptingInput = var3;
   }

   private List<ClientModule> boundModules(List<ClientModule> var1) {
      long var2 = ModuleRevision.current();
      if (var1 != this.boundFrom || var2 != this.boundRevision) {
         this.bound.clear();

         for (ClientModule var5 : var1) {
            if (isBindable(var5.keybind())) {
               this.bound.add(var5);
            }
         }

         this.boundFrom = var1;
         this.boundRevision = var2;
      }

      return this.bound;
   }

   public static boolean isBindable(int var0) {
      return var0 >= 32 && var0 <= 348 || var0 >= 1000 && var0 < 1008;
   }
}
