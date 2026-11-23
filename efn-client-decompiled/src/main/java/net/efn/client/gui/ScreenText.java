package net.efn.client.gui;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public final class ScreenText {
   private static final float IN = 0.4F;
   private static final float OUT = 0.8F;
   private final int rgb;
   private final float stay;
   private Text title;
   private Text subtitle;
   private long start;

   public ScreenText(int var1, float var2) {
      this.rgb = var1 & 16777215;
      this.stay = var2;
   }

   public void show(String var1, String var2) {
      this.title = Text.method_43470(var1);
      this.subtitle = Text.method_43470(var2);
      this.start = 0L;
   }

   public void render(DrawContext var1) {
      if (this.title != null) {
         MinecraftClient var3 = MinecraftClient.method_1551();
         long var4 = System.nanoTime();
         if (this.start == 0L) {
            if (var3.field_1755 != null || var3.field_1687 == null) {
               return;
            }

            this.start = var4;
         }

         float var2;
         if ((var2 = (float)(var4 - this.start) / 1.0E9F) >= 0.4F + this.stay + 0.8F) {
            this.title = null;
         } else {
            float var6 = var2 < 0.4F ? var2 / 0.4F : Math.min(1.0F, (0.4F + this.stay + 0.8F - var2) / 0.8F);
            int var7 = Math.round(var6 * 255.0F);
            if (var7 > 8) {
               TextRenderer var8 = var3.field_1772;
               int var9 = ColorHelper.method_61330(var7, this.rgb);
               var1.method_51448().pushMatrix();
               var1.method_51448().translate(var1.method_51421() / 2.0F, var1.method_51443() / 2.0F - 30.0F);
               var1.method_51448().scale(3.0F, 3.0F);
               int var10 = -var8.method_27525((StringVisitable)this.title) / 2;
               Objects.requireNonNull(var8);
               var1.method_51439(var8, this.title, var10, -4, var9, true);
               var1.method_51448().popMatrix();
               var1.method_51448().pushMatrix();
               var1.method_51448().translate(var1.method_51421() / 2.0F, var1.method_51443() / 2.0F);
               var1.method_51448().scale(1.5F, 1.5F);
               int var11 = -var8.method_27525((StringVisitable)this.subtitle) / 2;
               Objects.requireNonNull(var8);
               var1.method_51439(var8, this.subtitle, var11, -4, var9, true);
               var1.method_51448().popMatrix();
            }
         }
      }
   }
}
