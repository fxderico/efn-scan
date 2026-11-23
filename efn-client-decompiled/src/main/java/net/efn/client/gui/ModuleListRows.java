package net.efn.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRevision;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class ModuleListRows {
   private static final float SNAP = 0.005F;
   private static final Comparator<ModuleListRows.Row> WIDEST_FIRST = Comparator.<ModuleListRows.Row>comparingDouble(var0 -> -var0.nameW)
      .thenComparing(var0 -> var0.module.name());
   private static final Comparator<ModuleListRows.Row> WIDEST_LAST = WIDEST_FIRST.reversed();
   private final List<ModuleListRows.Row> rows = new ArrayList<>();
   private final List<ModuleListRows.Row> view = Collections.unmodifiableList(this.rows);
   private List<ClientModule> seenEnabled;
   private long seenRevision = -9223372036854775808L;
   private boolean seenEditMode;
   private boolean unsorted;
   private boolean sortedBottomFirst;

   ModuleListRows() {
   }

   void update(List<ClientModule> var1, boolean var2, float var3) {
      long var4 = ModuleRevision.current();
      if (var1 != this.seenEnabled || var4 != this.seenRevision || var2 != this.seenEditMode) {
         this.seenEnabled = var1;
         this.seenRevision = var4;
         this.seenEditMode = var2;
         this.rows.removeIf(var2x -> !listed(var2x.module, var2) || var2x.module.isEnabled() && !var1.contains(var2x.module));

         for (ClientModule var7 : var1) {
            if (listed(var7, var2) && !this.hasRow(var7)) {
               this.rows.add(new ModuleListRows.Row(var7));
               this.unsorted = true;
            }
         }
      }

      float var12 = 1.0F - (float)Math.exp(-12.0F * var3);

      for (int var13 = this.rows.size() - 1; var13 >= 0; var13--) {
         ModuleListRows.Row var9 = this.rows.get(var13);
         float var8 = var9.module.isEnabled() ? 1.0F : 0.0F;
         if (var9.anim != var8) {
            float var11 = var9.anim + (var8 - var9.anim) * var12;
            if (Math.abs(var11 - var8) < 0.005F) {
               var11 = var8;
            }

            var9.anim = var11;
            if (var11 == 0.0F) {
               this.rows.remove(var13);
            }
         }
      }
   }

   <F> List<ModuleListRows.Row> ordered(F var1, int var2, ModuleListRows.TextWidth<F> var3, boolean var4) {
      for (int var5 = 0; var5 < this.rows.size(); var5++) {
         ModuleListRows.Row var6 = this.rows.get(var5);
         if (var6.measuredPx != var2 || var6.measuredFont != var1) {
            var6.nameW = var3.of(var1, var2, var6.module.name());
            var6.measuredFont = var1;
            var6.measuredPx = var2;
            this.unsorted = true;
         }
      }

      if (this.unsorted || var4 != this.sortedBottomFirst) {
         this.rows.sort(var4 ? WIDEST_LAST : WIDEST_FIRST);
         this.unsorted = false;
         this.sortedBottomFirst = var4;
      }

      return this.view;
   }

   private static boolean listed(ClientModule var0, boolean var1) {
      return var0.category() != ModuleCategory.CLIENT && (var1 || var0.showInHud() == null || var0.showInHud().get());
   }

   private boolean hasRow(ClientModule var1) {
      for (int var2 = 0; var2 < this.rows.size(); var2++) {
         if (this.rows.get(var2).module == var1) {
            return true;
         }
      }

      return false;
   }

   @Environment(EnvType.CLIENT)
   static final class Row {
      private final ClientModule module;
      private float anim;
      private float nameW;
      private Object measuredFont;
      private int measuredPx = -1;

      private Row(ClientModule var1) {
         this.module = var1;
      }

      ClientModule module() {
         return this.module;
      }

      float anim() {
         return this.anim;
      }

      float nameW() {
         return this.nameW;
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface TextWidth<F> {
      float of(F var1, int var2, String var3);
   }
}
