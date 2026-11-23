package net.efn.client.module.impl.misc.nameprotect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.HoverEvent.EntityContent;
import net.minecraft.text.HoverEvent.ShowEntity;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.text.StringVisitable.StyledVisitor;

@Environment(EnvType.CLIENT)
public final class ProtectedText {
   private ProtectedText() {
   }

   public static Text rewrite(NameMatcher var0, Text var1) {
      if (var1 != null && !var0.isEmpty()) {
         Text var2 = rebuild(var0, var1, true);
         return var2 != null ? var2 : var1;
      } else {
         return var1;
      }
   }

   public static StringVisitable rewrite(NameMatcher var0, StringVisitable var1) {
      if (var1 != null && !var0.isEmpty()) {
         Text var2 = rebuild(var0, var1, true);
         return (StringVisitable)(var2 != null ? var2 : var1);
      } else {
         return var1;
      }
   }

   private static Text rebuild(NameMatcher var0, StringVisitable var1, boolean var2) {
      ProtectedText.Pieces var4 = new ProtectedText.Pieces();
      var1.method_27658(var4, Style.field_24360);
      if (var4.count == 0) {
         return null;
      } else {
         var4.seal();
         String var5 = var4.text.toString();
         List var6 = var0.scan(var5, var4.starts, var4.count);
         Style[] var3 = var2 ? rewriteHovers(var0, var4) : null;
         if (var6.isEmpty() && var3 == null) {
            return null;
         } else {
            Style[] var8 = var3 != null ? var3 : var4.styles;
            ProtectedText.Runs var9 = new ProtectedText.Runs(var4);
            int var10 = 0;

            for (NameMatcher.Match var12 : var6) {
               var9.copy(var5, var10, var12.start());
               var9.spread(var12.alias(), var12.start(), var12.end());
               var10 = var12.end();
            }

            var9.copy(var5, var10, var5.length());
            return var9.build(var8);
         }
      }
   }

   private static Style[] rewriteHovers(NameMatcher var0, ProtectedText.Pieces var1) {
      Style[] var2 = null;
      IdentityHashMap var3 = null;

      for (int var4 = 0; var4 < var1.count; var4++) {
         Style var6 = var1.styles[var4];
         HoverEvent var7 = var6.method_10969();
         if (var7 != null) {
            if (var3 == null) {
               var3 = new IdentityHashMap();
            }

            HoverEvent var5;
            if ((var5 = (HoverEvent)var3.get(var7)) == null) {
               var5 = rewriteHover(var0, var7);
               var3.put(var7, var5);
            }

            if (var5 != var7) {
               if (var2 == null) {
                  var2 = Arrays.copyOf(var1.styles, var1.count);
               }

               var2[var4] = var6.method_10949(var5);
            }
         }
      }

      return var2;
   }

   private static HoverEvent rewriteHover(NameMatcher var0, HoverEvent var1) {
      if (var1 instanceof ShowText var5) {
         Text var6 = var5.comp_3510();
         Text var7 = var6 != null ? rebuild(var0, var6, false) : null;
         return (HoverEvent)(var7 != null ? new ShowText(var7) : var1);
      } else if (var1 instanceof ShowEntity var2) {
         EntityContent var3 = var2.comp_3508();
         if (var3 != null && !var3.field_24353.isEmpty()) {
            Text var4 = rebuild(var0, (StringVisitable)var3.field_24353.get(), false);
            return (HoverEvent)(var4 != null ? new ShowEntity(new EntityContent(var3.field_24351, var3.field_24352, var4)) : var1);
         } else {
            return var1;
         }
      } else {
         return var1;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Pieces implements StyledVisitor<Object> {
      final StringBuilder text = new StringBuilder();
      Style[] styles = new Style[8];
      int[] starts = new int[9];
      int count;

      private Pieces() {
      }

      public Optional<Object> accept(Style var1, String var2) {
         if (var2.isEmpty()) {
            return Optional.empty();
         } else {
            if (this.count == this.styles.length) {
               this.styles = Arrays.copyOf(this.styles, this.count * 2);
               this.starts = Arrays.copyOf(this.starts, this.count * 2 + 1);
            }

            this.styles[this.count] = var1;
            this.starts[this.count] = this.text.length();
            this.count++;
            this.text.append(var2);
            return Optional.empty();
         }
      }

      void seal() {
         this.starts[this.count] = this.text.length();
      }

      int pieceAt(int var1) {
         int var2 = Arrays.binarySearch(this.starts, 0, this.count, var1);
         return var2 >= 0 ? var2 : -var2 - 2;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Runs {
      private final ProtectedText.Pieces pieces;
      private final List<Integer> runPieces = new ArrayList<>();
      private final List<StringBuilder> runTexts = new ArrayList<>();

      Runs(ProtectedText.Pieces var1) {
         this.pieces = var1;
      }

      void copy(String var1, int var2, int var3) {
         for (int var4 = var2 < var3 ? this.pieces.pieceAt(var2) : 0; var2 < var3; var4++) {
            int var6 = Math.min(this.pieces.starts[var4 + 1], var3);
            this.run(var4).append(var1, var2, var6);
            var2 = var6;
         }
      }

      void spread(String var1, int var2, int var3) {
         int var4 = var3 - var2;
         int var5 = var1.codePointCount(0, var1.length());
         int var6 = 0;

         for (int var7 = 0; var7 < var1.length(); var6++) {
            int var8 = var1.codePointAt(var7);
            int var9 = var2 + (int)((long)var6 * var4 / var5);
            this.run(this.pieces.pieceAt(var9)).appendCodePoint(var8);
            var7 += Character.charCount(var8);
         }
      }

      private StringBuilder run(int var1) {
         int var2 = this.runPieces.size() - 1;
         if (var2 >= 0 && this.runPieces.get(var2) == var1) {
            return this.runTexts.get(var2);
         } else {
            StringBuilder var3 = new StringBuilder();
            this.runPieces.add(var1);
            this.runTexts.add(var3);
            return var3;
         }
      }

      Text build(Style[] var1) {
         MutableText var2 = Text.method_43473();

         for (int var3 = 0; var3 < this.runTexts.size(); var3++) {
            var2.method_10852(Text.method_43470(this.runTexts.get(var3).toString()).method_10862(var1[this.runPieces.get(var3)]));
         }

         return var2;
      }
   }
}
