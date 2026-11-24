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

   public static Text rewrite(NameMatcher nameMatcher, Text text) {
      if (text != null && !nameMatcher.isEmpty()) {
         Text var2 = rebuild(nameMatcher, text, true);
         return var2 != null ? var2 : text;
      } else {
         return text;
      }
   }

   public static StringVisitable rewrite(NameMatcher nameMatcher, StringVisitable stringVisitable) {
      if (stringVisitable != null && !nameMatcher.isEmpty()) {
         Text var2 = rebuild(nameMatcher, stringVisitable, true);
         return (StringVisitable)(var2 != null ? var2 : stringVisitable);
      } else {
         return stringVisitable;
      }
   }

   private static Text rebuild(NameMatcher nameMatcher, StringVisitable stringVisitable, boolean bl) {
      ProtectedText.Pieces var4 = new ProtectedText.Pieces();
      stringVisitable.visit(var4, Style.EMPTY);
      if (var4.count == 0) {
         return null;
      } else {
         var4.seal();
         String var5 = var4.text.toString();
         List var6 = nameMatcher.scan(var5, var4.starts, var4.count);
         Style[] var3 = bl ? rewriteHovers(nameMatcher, var4) : null;
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

   private static Style[] rewriteHovers(NameMatcher nameMatcher, ProtectedText.Pieces pieces) {
      Style[] var2 = null;
      IdentityHashMap var3 = null;

      for (int var4 = 0; var4 < pieces.count; var4++) {
         Style var6 = pieces.styles[var4];
         HoverEvent var7 = var6.getHoverEvent();
         if (var7 != null) {
            if (var3 == null) {
               var3 = new IdentityHashMap();
            }

            HoverEvent var5;
            if ((var5 = (HoverEvent)var3.get(var7)) == null) {
               var5 = rewriteHover(nameMatcher, var7);
               var3.put(var7, var5);
            }

            if (var5 != var7) {
               if (var2 == null) {
                  var2 = Arrays.copyOf(pieces.styles, pieces.count);
               }

               var2[var4] = var6.withHoverEvent(var5);
            }
         }
      }

      return var2;
   }

   private static HoverEvent rewriteHover(NameMatcher nameMatcher, HoverEvent hoverEvent) {
      if (hoverEvent instanceof ShowText var5) {
         Text var6 = var5.value();
         Text var7 = var6 != null ? rebuild(nameMatcher, var6, false) : null;
         return (HoverEvent)(var7 != null ? new ShowText(var7) : hoverEvent);
      } else if (hoverEvent instanceof ShowEntity var2) {
         EntityContent var3 = var2.entity();
         if (var3 != null && !var3.name.isEmpty()) {
            Text var4 = rebuild(nameMatcher, var3.name.get(), false);
            return (HoverEvent)(var4 != null ? new ShowEntity(new EntityContent(var3.entityType, var3.uuid, var4)) : hoverEvent);
         } else {
            return hoverEvent;
         }
      } else {
         return hoverEvent;
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

      @Override
      public Optional<Object> accept(Style style, String string) {
         if (string.isEmpty()) {
            return Optional.empty();
         } else {
            if (this.count == this.styles.length) {
               this.styles = Arrays.copyOf(this.styles, this.count * 2);
               this.starts = Arrays.copyOf(this.starts, this.count * 2 + 1);
            }

            this.styles[this.count] = style;
            this.starts[this.count] = this.text.length();
            this.count++;
            this.text.append(string);
            return Optional.empty();
         }
      }

      void seal() {
         this.starts[this.count] = this.text.length();
      }

      int pieceAt(int i) {
         int var2 = Arrays.binarySearch(this.starts, 0, this.count, i);
         return var2 >= 0 ? var2 : -var2 - 2;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Runs {
      private final ProtectedText.Pieces pieces;
      private final List<Integer> runPieces = new ArrayList<>();
      private final List<StringBuilder> runTexts = new ArrayList<>();

      Runs(ProtectedText.Pieces pieces) {
         this.pieces = pieces;
      }

      void copy(String string, int i, int j) {
         for (int var4 = i < j ? this.pieces.pieceAt(i) : 0; i < j; var4++) {
            int var6 = Math.min(this.pieces.starts[var4 + 1], j);
            this.run(var4).append(string, i, var6);
            i = var6;
         }
      }

      void spread(String string, int i, int j) {
         int var4 = j - i;
         int var5 = string.codePointCount(0, string.length());
         int var6 = 0;

         for (int var7 = 0; var7 < string.length(); var6++) {
            int var8 = string.codePointAt(var7);
            int var9 = i + (int)((long)var6 * var4 / var5);
            this.run(this.pieces.pieceAt(var9)).appendCodePoint(var8);
            var7 += Character.charCount(var8);
         }
      }

      private StringBuilder run(int i) {
         int var2 = this.runPieces.size() - 1;
         if (var2 >= 0 && this.runPieces.get(var2) == i) {
            return this.runTexts.get(var2);
         } else {
            StringBuilder var3 = new StringBuilder();
            this.runPieces.add(i);
            this.runTexts.add(var3);
            return var3;
         }
      }

      Text build(Style[] styles) {
         MutableText var2 = Text.empty();

         for (int var3 = 0; var3 < this.runTexts.size(); var3++) {
            var2.append(Text.literal(this.runTexts.get(var3).toString()).setStyle(styles[this.runPieces.get(var3)]));
         }

         return var2;
      }
   }
}
