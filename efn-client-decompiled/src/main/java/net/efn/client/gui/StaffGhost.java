package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImGui;
import java.util.Objects;
import java.util.UUID;
import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.util.NameFilter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public final class StaffGhost {
   private static final float LIFE_SECONDS = 1.5F;
   private static final float TITLE_SECONDS = 1.25F;
   private static final int TITLE_IN = 10;
   private static final int TITLE_STAY = 40;
   private static final int TITLE_OUT = 15;
   private static final float NOTE_SECONDS = 4.5F;
   private static final float NOTE_IN = 0.5F;
   private static final float NOTE_STAY = 2.5F;
   private static final float NOTE_OUT = 1.0F;
   private static final Text NOTE = Text.literal("Goodluck EFN User, Be Careful");
   private static final int NOTE_COLOR = 16777045;
   private static final float DISTANCE = 1.49F;
   private static final float LIFT = 0.24F;
   private static final float HALF = 0.45F;
   private static final float HAT = 1.125F;
   private static final float HALF_FOV_TAN = (float)Math.tan(Math.toRadians(35.0));
   private static final float NEAR = -0.08F;
   private static final int CELLS = 8;
   private static final float[][] FACES = new float[][]{
      {8.0F, 8.0F, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F},
      {0.0F, 8.0F, -1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, -1.0F, -1.0F, -1.0F, 0.8F},
      {16.0F, 8.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 0.8F},
      {8.0F, 0.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 0.95F},
      {16.0F, 0.0F, -1.0F, -1.0F, 1.0F, 1.0F, -1.0F, 1.0F, -1.0F, -1.0F, -1.0F, 0.6F}
   };
   private static UUID watcher;
   private static boolean hidden;
   private static String title;
   private static int stage;
   private static long startNanos;
   private static long noteNanos;
   private static boolean active;

   private StaffGhost() {
   }

   public static void watching(UUID uUID, String string, int i) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3.world != null && string != null) {
         String var4 = NameFilter.apply(string);
         watcher = uUID;
         hidden = !var4.equals(string);
         title = i > 0 ? var4 + " +" + i : var4;
         stage = 0;
         startNanos = System.nanoTime();
         noteNanos = 0L;
         active = true;
      }
   }

   public static boolean hasWork() {
      if (active && MinecraftClient.getInstance().world == null) {
         active = false;
      }

      return active;
   }

   public static void renderNote(Render2DEvent render2DEvent) {
      if (noteNanos != 0L) {
         float var1 = (float)(System.nanoTime() - noteNanos) / 1.0E9F;
         if (var1 >= 4.0F) {
            noteNanos = 0L;
         } else {
            float var2 = var1 < 0.5F ? var1 / 0.5F : Math.min(1.0F, (4.0F - var1) / 1.0F);
            int var3 = Math.round(var2 * 255.0F);
            if (var3 > 8) {
               DrawContext var4 = render2DEvent.graphics;
               TextRenderer var5 = MinecraftClient.getInstance().textRenderer;
               var4.getMatrices().pushMatrix();
               var4.getMatrices().translate(var4.getScaledWindowWidth() / 2.0F, var4.getScaledWindowHeight() / 2.0F);
               var4.getMatrices().scale(2.0F, 2.0F);
               int var6 = -var5.getWidth(NOTE) / 2;
               Objects.requireNonNull(var5);
               var4.drawText(var5, NOTE, var6, -4, ColorHelper.withAlpha(var3, 16777045), true);
               var4.getMatrices().popMatrix();
            }
         }
      }
   }

   public static void render() {
      if (hasWork()) {
         MinecraftClient var0 = MinecraftClient.getInstance();
         float var1 = elapsed();
         if (stage == 0 && var1 >= 1.25F) {
            stage = 1;
            var0.inGameHud.setTitleTicks(10, 40, 15);
            var0.inGameHud.setSubtitle(Text.literal("WATCHING YOU").formatted(Formatting.RED, Formatting.BOLD));
            var0.inGameHud.setTitle(Text.literal(title).formatted(Formatting.RED, Formatting.BOLD));
         } else if (stage == 1 && var1 >= 4.5F) {
            stage = 2;
            var0.inGameHud.setDefaultTitleFade();
            noteNanos = System.nanoTime();
            active = false;
            return;
         }

         if (!(var1 >= 1.5F) && watcher != null) {
            int var2 = HudRenderer.getInstance().staffFace(watcher, hidden);
            if (var2 != 0) {
               float var3 = var1 / 1.5F;
               float var4 = 0.05F + 0.6F * (float)Math.sin(var3 * 3.141592653589793);
               double var5 = Math.toRadians(60.0 - 150.0 * var3);
               float var7 = (float)Math.cos(var5);
               float var8 = (float)Math.sin(var5);
               float var9 = ImGui.getIO().getDisplaySizeX();
               float var10 = ImGui.getIO().getDisplaySizeY();
               float var11 = var10 * 0.5F / HALF_FOV_TAN;
               ImDrawList var12 = ImGui.getForegroundDrawList();
               drawCube(var12, var2, 0.45F, 0.0F, var7, var8, var9, var10, var11, var4);
               drawCube(var12, var2, 0.50624996F, 32.0F, var7, var8, var9, var10, var11, var4);
            }
         }
      }
   }

   private static void drawCube(ImDrawList imDrawList, int i, float f, float g, float h, float j, float k, float l, float m, float n) {
      float[] var10 = new float[3];
      float[] var11 = new float[3];
      float[] var12 = new float[3];
      float[] var13 = new float[81];
      float[] var14 = new float[var13.length];

      for (float[] var18 : FACES) {
         corner(var18, 2, f, h, j, var10);
         corner(var18, 5, f, h, j, var11);
         corner(var18, 8, f, h, j, var12);
         float var19 = var11[0] - var10[0];
         float var20 = var11[1] - var10[1];
         float var21 = var11[2] - var10[2];
         float var22 = var12[0] - var10[0];
         float var23 = var12[1] - var10[1];
         float var24 = var12[2] - var10[2];
         float var25 = var20 * var24 - var21 * var23;
         float var26 = var21 * var22 - var19 * var24;
         float var27 = var19 * var23 - var20 * var22;
         float var28 = var10[0] + (var19 + var22) * 0.5F;
         float var29 = var10[1] + (var20 + var23) * 0.5F;
         float var30 = var10[2] + (var21 + var24) * 0.5F;
         if (!(var25 * var28 + var26 * var29 + var27 * var30 <= 0.0F)
            && !(var10[2] > -0.08F)
            && !(var11[2] > -0.08F)
            && !(var12[2] > -0.08F)
            && !(var12[2] + var21 > -0.08F)) {
            for (int var31 = 0; var31 <= 8; var31++) {
               for (int var32 = 0; var32 <= 8; var32++) {
                  float var33 = var32 / 8.0F;
                  float var34 = var31 / 8.0F;
                  float var35 = var10[0] + var19 * var33 + var22 * var34;
                  float var36 = var10[1] + var20 * var33 + var23 * var34;
                  float var37 = var10[2] + var21 * var33 + var24 * var34;
                  var13[var31 * 9 + var32] = k * 0.5F + var35 / -var37 * m;
                  var14[var31 * 9 + var32] = l * 0.5F - var36 / -var37 * m;
               }
            }

            int var42 = Math.round(var18[11] * 255.0F);
            int var43 = Math.round(Math.max(0.0F, Math.min(1.0F, n)) * 255.0F) << 24 | var42 << 16 | var42 << 8 | var42;
            float var44 = (var18[0] + g) / 64.0F;
            float var45 = var18[1] / 64.0F;
            float var46 = 0.015625F;

            for (int var47 = 0; var47 < 8; var47++) {
               for (int var48 = 0; var48 < 8; var48++) {
                  int var38 = var47 * 9 + var48;
                  int var39 = var38 + 8 + 1;
                  float var40 = var44 + var48 * var46;
                  float var41 = var45 + var47 * var46;
                  imDrawList.addImageQuad(
                     i,
                     var13[var38],
                     var14[var38],
                     var13[var38 + 1],
                     var14[var38 + 1],
                     var13[var39 + 1],
                     var14[var39 + 1],
                     var13[var39],
                     var14[var39],
                     var40,
                     var41,
                     var40 + var46,
                     var41,
                     var40 + var46,
                     var41 + var46,
                     var40,
                     var41 + var46,
                     var43
                  );
               }
            }
         }
      }
   }

   private static void corner(float[] fs, int i, float f, float g, float h, float[] gs) {
      float var6 = fs[i] * f;
      float var7 = fs[i + 1] * f + 0.24F;
      float var8 = fs[i + 2] * f - 1.49F;
      gs[0] = var6;
      gs[1] = var7 * g - var8 * h;
      gs[2] = var7 * h + var8 * g;
   }

   private static float elapsed() {
      return (float)(System.nanoTime() - startNanos) / 1.0E9F;
   }
}
