package imgui.gl3;

import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.type.ImInt;
import java.nio.ByteBuffer;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

@Environment(EnvType.CLIENT)
public class EfnGl3 extends ImGuiImplGl3 {
   public EfnGl3() {
   }

   public boolean createFontsTexture() {
      ImFontAtlas var11 = ImGui.getIO().getFonts();
      ImInt var12 = new ImInt();
      ImInt var13 = new ImInt();
      ByteBuffer var14 = var11.getTexDataAsRGBA32(var12, var13);
      int var15 = GL11.glGetInteger(3379);
      long var16 = (long)var12.get() * var13.get() * 4L;
      if (var12.get() > 0
         && var13.get() > 0
         && var12.get() <= var15
         && var13.get() <= var15
         && var14 != null
         && var16 <= var14.capacity()
         && var16 <= 2147483647L) {
         ByteBuffer var18 = var14.duplicate();
         var18.position(0);
         var18.limit((int)var16);
         int var10 = GL11.glGetInteger(34016);
         GL13.glActiveTexture(33984);
         int var9 = GL11.glGetInteger(32873);
         int var8 = GL11.glGetInteger(35055);
         int var7 = GL11.glGetInteger(3317);
         int var6 = GL11.glGetInteger(3314);
         int var5 = GL11.glGetInteger(3315);
         int var4 = GL11.glGetInteger(3316);
         int var3 = this.data.fontTexture;
         int var2 = 0;
         boolean var1 = false;

         label82: {
            try {
               reportExistingErrors();
               GL15.glBindBuffer(35052, 0);
               GL11.glPixelStorei(3317, 1);
               GL11.glPixelStorei(3314, 0);
               GL11.glPixelStorei(3315, 0);
               GL11.glPixelStorei(3316, 0);
               var2 = GL11.glGenTextures();
               GL11.glBindTexture(3553, var2);
               GL11.glTexParameteri(3553, 10241, 9729);
               GL11.glTexParameteri(3553, 10240, 9729);
               GL11.glTexParameteri(3553, 10242, 33071);
               GL11.glTexParameteri(3553, 10243, 33071);
               GL11.glTexParameteri(3553, 33084, 0);
               GL11.glTexParameteri(3553, 33085, 0);
               GL11.glTexImage2D(3553, 0, 32856, var12.get(), var13.get(), 0, 6408, 5121, var18);
               int var19 = GL11.glGetError();
               if (var2 == 0 || var19 != 0) {
                  throw new IllegalStateException(
                     "[EFN ImGui] Font upload failed: "
                        + var12.get()
                        + "x"
                        + var13.get()
                        + ", GL error=0x"
                        + Integer.toHexString(var19)
                        + ", renderer="
                        + GL11.glGetString(7937)
                  );
               }

               this.data.fontTexture = var2;
               var11.setTexID(var2);
               var1 = true;
               System.err
                  .printf(
                     Locale.ROOT,
                     "[EFN ImGui] Font atlas uploaded: %dx%d RGBA8, max=%d, previousUnpackBuffer=%d, GL=%s, renderer=%s%n",
                     var12.get(),
                     var13.get(),
                     var15,
                     var8,
                     GL11.glGetString(7938),
                     GL11.glGetString(7937)
                  );
               if (var1 || var2 == 0) {
                  break label82;
               }
            } catch (Throwable var21) {
               if (!var1 && var2 != 0) {
                  GL11.glDeleteTextures(var2);
               }

               int var20 = var1 && var3 != 0 && var9 == var3 ? var2 : var9;
               GL11.glBindTexture(3553, var20);
               GL15.glBindBuffer(35052, var8);
               GL11.glPixelStorei(3317, var7);
               GL11.glPixelStorei(3314, var6);
               GL11.glPixelStorei(3315, var5);
               GL11.glPixelStorei(3316, var4);
               GL13.glActiveTexture(var10);
               if (var1 && var3 != 0 && var3 != var2) {
                  GL11.glDeleteTextures(var3);
               }

               throw var21;
            }

            GL11.glDeleteTextures(var2);
         }

         int var22 = var1 && var3 != 0 && var9 == var3 ? var2 : var9;
         GL11.glBindTexture(3553, var22);
         GL15.glBindBuffer(35052, var8);
         GL11.glPixelStorei(3317, var7);
         GL11.glPixelStorei(3314, var6);
         GL11.glPixelStorei(3315, var5);
         GL11.glPixelStorei(3316, var4);
         GL13.glActiveTexture(var10);
         if (var1 && var3 != 0 && var3 != var2) {
            GL11.glDeleteTextures(var3);
         }

         return true;
      } else {
         throw new IllegalStateException("[EFN ImGui] Invalid font atlas " + var12.get() + "x" + var13.get() + "; GL_MAX_TEXTURE_SIZE=" + var15);
      }
   }

   private static void reportExistingErrors() {
      for (int var0 = 0; var0 < 16; var0++) {
         int var1 = GL11.glGetError();
         if (var1 == 0) {
            return;
         }

         System.err.println("[EFN ImGui] GL error predating font upload: 0x" + Integer.toHexString(var1));
      }
   }
}
