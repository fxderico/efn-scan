package net.efn.client.gui;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
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

   public ScreenText(int i, float f) {
      this.rgb = i & 16777215;
      this.stay = f;
   }

   public void show(String string, String string2) {
      this.title = Text.literal(string);
      this.subtitle = Text.literal(string2);
      this.start = 0L;
   }

   public void render(DrawContext drawContext) {
      if (this.title != null) {
         MinecraftClient var3 = MinecraftClient.getInstance();
         long var4 = System.nanoTime();
         if (this.start == 0L) {
            if (var3.currentScreen != null || var3.world == null) {
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
               TextRenderer var8 = var3.textRenderer;
               int var9 = ColorHelper.withAlpha(var7, this.rgb);
               drawContext.getMatrices().pushMatrix();
               drawContext.getMatrices().translate(drawContext.getScaledWindowWidth() / 2.0F, drawContext.getScaledWindowHeight() / 2.0F - 30.0F);
               drawContext.getMatrices().scale(3.0F, 3.0F);
               int var10 = -var8.getWidth(this.title) / 2;
               Objects.requireNonNull(var8);
               drawContext.drawText(var8, this.title, var10, -4, var9, true);
               drawContext.getMatrices().popMatrix();
               drawContext.getMatrices().pushMatrix();
               drawContext.getMatrices().translate(drawContext.getScaledWindowWidth() / 2.0F, drawContext.getScaledWindowHeight() / 2.0F);
               drawContext.getMatrices().scale(1.5F, 1.5F);
               int var11 = -var8.getWidth(this.subtitle) / 2;
               Objects.requireNonNull(var8);
               drawContext.drawText(var8, this.subtitle, var11, -4, var9, true);
               drawContext.getMatrices().popMatrix();
            }
         }
      }
   }
}
