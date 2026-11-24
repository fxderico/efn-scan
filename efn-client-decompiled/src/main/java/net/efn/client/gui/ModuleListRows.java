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
   private static final Comparator<ModuleListRows.Row> WIDEST_FIRST = Comparator.<ModuleListRows.Row>comparingDouble(row -> -row.nameW)
      .thenComparing(row -> row.module.name());
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

   void update(List<ClientModule> list, boolean bl, float f) {
      long var4 = ModuleRevision.current();
      if (list != this.seenEnabled || var4 != this.seenRevision || bl != this.seenEditMode) {
         this.seenEnabled = list;
         this.seenRevision = var4;
         this.seenEditMode = bl;
         this.rows.removeIf(row -> !listed(row.module, bl) || row.module.isEnabled() && !list.contains(row.module));

         for (ClientModule var7 : list) {
            if (listed(var7, bl) && !this.hasRow(var7)) {
               this.rows.add(new ModuleListRows.Row(var7));
               this.unsorted = true;
            }
         }
      }

      float var12 = 1.0F - (float)Math.exp(-12.0F * f);

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

   <F> List<ModuleListRows.Row> ordered(F object, int i, ModuleListRows.TextWidth<F> textWidth, boolean bl) {
      for (int var5 = 0; var5 < this.rows.size(); var5++) {
         ModuleListRows.Row var6 = this.rows.get(var5);
         if (var6.measuredPx != i || var6.measuredFont != object) {
            var6.nameW = textWidth.of(object, i, var6.module.name());
            var6.measuredFont = object;
            var6.measuredPx = i;
            this.unsorted = true;
         }
      }

      if (this.unsorted || bl != this.sortedBottomFirst) {
         this.rows.sort(bl ? WIDEST_LAST : WIDEST_FIRST);
         this.unsorted = false;
         this.sortedBottomFirst = bl;
      }

      return this.view;
   }

   private static boolean listed(ClientModule clientModule, boolean bl) {
      return clientModule.category() != ModuleCategory.CLIENT && (bl || clientModule.showInHud() == null || clientModule.showInHud().get());
   }

   private boolean hasRow(ClientModule clientModule) {
      for (int var2 = 0; var2 < this.rows.size(); var2++) {
         if (this.rows.get(var2).module == clientModule) {
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

      private Row(ClientModule clientModule) {
         this.module = clientModule;
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
      float of(F object, int i, String string);
   }
}
