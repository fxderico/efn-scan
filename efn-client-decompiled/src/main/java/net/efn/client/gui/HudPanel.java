package net.efn.client.gui;

import com.mojang.blaze3d.textures.GpuTexture;
import imgui.ImDrawList;
import imgui.ImFont;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.efn.client.config.HudTheme;
import net.efn.client.config.Surface;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.client.HudModule;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
public final class HudPanel {
   public static final int LABEL = -6052949;
   public static final int VALUE = -1;
   public static final int WARN = -47814;
   public static final int GOOD = -13445301;
   public static final int CAUTION = -19648;
   private static final float GRIP = 14.0F;
   private static final float HEADER_H = 20.0F;
   private static final float ROW_H = 22.0F;
   private static final float PAD = 8.0F;
   private static final float FACE = 16.0F;
   private static final float GAP_FACE = 6.0F;
   private static final float GAP_COUNT = 5.0F;
   private static final float GAP_RIGHT = 14.0F;
   private static final float ROWS_RADIUS = 6.0F;
   private static final float SKIN_FACE_U = 0.125F;
   private static final float SKIN_HAT_U = 0.625F;
   private static final float SKIN_TOP_V = 0.125F;
   private static final float SKIN_PART = 0.125F;
   private static final Map<String, ProfileComponent> PROFILES = new HashMap<>();
   private static final Set<GpuTexture> NEAREST = Collections.newSetFromMap(new WeakHashMap<>());
   private final NumberSetting x;
   private final NumberSetting y;
   private final NumberSetting scale;
   private final NumberSetting opacity;
   private boolean dragging;
   private boolean resizing;
   private boolean wasDown;
   private float grabX;
   private float grabY;
   private float scaleAtGrab;
   private float widthAtGrab;
   private float alpha = 1.0F;

   public HudPanel(NumberSetting numberSetting, NumberSetting numberSetting2, NumberSetting numberSetting3, NumberSetting numberSetting4) {
      this.x = numberSetting;
      this.y = numberSetting2;
      this.scale = numberSetting3;
      this.opacity = numberSetting4;
      numberSetting.visibleWhen(() -> false);
      numberSetting2.visibleWhen(() -> false);
   }

   public static NumberSetting coordinate(String string) {
      return new NumberSetting(string, -1.0F, -1.0F, 10000.0F).step(1.0F);
   }

   public static NumberSetting opacitySetting() {
      return new NumberSetting("Opacity", 100.0F, 10.0F, 100.0F).step(5.0F);
   }

   public float[] place(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k) {
      MinecraftClient var8 = MinecraftClient.getInstance();
      this.alpha = Math.max(0.0F, Math.min(1.0F, this.opacity.get() / 100.0F));
      float var9 = this.x.get() < 0.0F ? j : this.x.get();
      float var10 = this.y.get() < 0.0F ? k : this.y.get();
      boolean var11 = var8.currentScreen instanceof ChatScreen;
      boolean var12 = var11 && GLFW.glfwGetMouseButton(var8.getWindow().getHandle(), 0) == 1;
      float var13 = (float)(var8.mouse.getX() * h / Math.max(1, var8.getWindow().getWidth()));
      float var14 = (float)(var8.mouse.getY() * i / Math.max(1, var8.getWindow().getHeight()));
      if (var12 && !this.wasDown) {
         if (this.scale != null && var13 >= var9 + f - 14.0F && var13 <= var9 + f && var14 >= var10 + g - 14.0F && var14 <= var10 + g) {
            this.resizing = true;
            this.scaleAtGrab = this.scale.get();
            this.widthAtGrab = Math.max(1.0F, f);
            this.grabX = var9 + f - var13;
         } else if (var13 >= var9 && var13 <= var9 + f && var14 >= var10 && var14 <= var10 + g) {
            this.dragging = true;
            this.grabX = var13 - var9;
            this.grabY = var14 - var10;
         }
      }

      if (this.resizing) {
         this.scale.set(Math.max(this.scale.min(), Math.min(this.scale.max(), this.scaleAtGrab * (var13 + this.grabX - var9) / this.widthAtGrab)));
      }

      if (this.dragging) {
         var9 = var13 - this.grabX;
         var10 = var14 - this.grabY;
      }

      var9 = Math.max(0.0F, Math.min(var9, h - f));
      var10 = Math.max(0.0F, Math.min(var10, i - g));
      if (!var12) {
         if (this.dragging) {
            this.x.set(Math.round(var9));
            this.y.set(Math.round(var10));
         }

         this.dragging = false;
         this.resizing = false;
      }

      this.wasDown = var12;
      if (var11) {
         imDrawList.addRect(
            var9 - 1.0F,
            var10 - 1.0F,
            var9 + f + 1.0F,
            var10 + g + 1.0F,
            GuiPalette.abgr(!this.dragging && !this.resizing ? 1627389951 : -1056964609),
            8.0F,
            0,
            1.0F
         );
         if (this.scale != null) {
            for (int var15 = 1; var15 <= 3; var15++) {
               float var16 = var15 * 4.0F;
               imDrawList.addLine(var9 + f - var16, var10 + g - 2.0F, var9 + f - 2.0F, var10 + g - var16, GuiPalette.abgr(-788529153), 1.5F);
            }
         }
      }

      return new float[]{var9, var10};
   }

   public void card(ImDrawList imDrawList, String string, String string2, int i, List<HudPanel.Line> list, String string3, float f, float g, float h, float j) {
      ImGuiManager var11 = ImGuiManager.getInstance();
      float var12 = (this.scale == null ? 1.0F : this.scale.get()) * var11.uiScale();
      ImFont var13 = var11.hudLabelFont();
      ImFont var14 = var11.hudTextFont();
      int var15 = HudChrome.px(14.0F, var12);
      int var16 = HudChrome.px(17.0F, var12);
      float var17 = 8.0F * var12;
      float var18 = 20.0F * var12;
      float var19 = 22.0F * var12;
      float var20 = faceSize(var12);
      float var21 = 6.0F * var12;
      boolean var22 = false;

      for (HudPanel.Line var24 : list) {
         var22 |= var24.face() != 0;
      }

      float var43 = var22 ? var20 + var21 : 0.0F;
      float var44 = HudChrome.width(var13, var15, string);
      float var25 = var44 + (string2.isEmpty() ? 0.0F : 5.0F * var12 + HudChrome.width(var14, var16, string2));

      for (HudPanel.Line var27 : list) {
         float var28 = var43 + HudChrome.width(var14, var16, var27.text());
         if (!var27.right().isEmpty()) {
            var28 += 14.0F * var12 + HudChrome.width(var13, var15, var27.right());
         }

         var25 = Math.max(var25, var28);
      }

      float var45 = (float)Math.ceil(var25 + var17 * 2.0F);
      float var46 = Math.round(var18 + var19 * list.size());
      float var47 = 10.0F * var12;
      float[] var29 = this.place(imDrawList, var45, var46, f, g, var47 + (f - var45 - var47 * 2.0F) * h, j);
      float var30 = Math.round(var29[0]);
      float var31 = Math.round(var29[1]);
      Surface var32 = HudTheme.get().forElementId(string3);
      float var33 = Math.min(HudTheme.get().rounding.get(), 6.0F) * var12;
      HudModule var34 = ModuleRegistry.get(HudModule.class);
      if (var34 == null || var34.shadow()) {
         HudChrome.shadow(imDrawList, var30, var31, var45, var46, var33, this.alpha, var12);
      }

      if (var32 != null) {
         imDrawList.addRectFilled(var30, var31, var30 + var45, var31 + var46, HudChrome.scaleAlpha(GuiPalette.fill(var32), this.alpha), var33);
         SurfaceBackground.draw(imDrawList, var32, var30, var31, var45, var46, var33, this.alpha);
      } else {
         imDrawList.addRectFilled(var30, var31, var30 + var45, var31 + var46, HudChrome.scaleAlpha(HudChrome.CHIP_FILL, this.alpha), var33);
      }

      float var35 = HudChrome.baselineOffset(var14, var16, var13, var15);
      float var36 = var31 + (float)Math.floor((var18 - var16) * 0.5F);
      HudChrome.text(imDrawList, var13, var15, var30 + var17, var36 + var35, this.abgr(-6052949), string, true);
      if (!string2.isEmpty()) {
         HudChrome.text(imDrawList, var14, var16, var30 + var17 + var44 + 5.0F * var12, var36, this.abgr(i), string2, true);
      }

      float var37 = (float)Math.floor((var19 - var16) * 0.5F);

      for (int var38 = 0; var38 < list.size(); var38++) {
         HudPanel.Line var39 = (HudPanel.Line)list.get(var38);
         float var40 = Math.round(var31 + var18 + var19 * var38);
         float var41 = var40 + var37;
         if (var39.face() != 0) {
            this.drawFace(imDrawList, var39.face(), Math.round(var30 + var17), Math.round(var40 + (var19 - var20) * 0.5F), var20, var12);
         }

         HudChrome.text(imDrawList, var14, var16, var30 + var17 + var43, var41, this.abgr(var39.color()), var39.text(), true);
         if (!var39.right().isEmpty()) {
            float var42 = var30 + var45 - var17 - HudChrome.width(var13, var15, var39.right());
            HudChrome.text(imDrawList, var13, var15, var42, var41 + var35, this.abgr(var39.rightColor()), var39.right(), true);
         }
      }
   }

   private static float faceSize(float f) {
      int var1 = Math.round(Math.round(16.0F * f) / 8.0F) * 8;
      if (var1 > 22.0F * f - 4.0F * f) {
         var1 -= 8;
      }

      return Math.max(8, var1);
   }

   public void drawFace(ImDrawList imDrawList, int i, float f, float g, float h, float j) {
      float var7 = Math.max(1.0F, (float)Math.round(2.0F * j));
      int var8 = this.abgr(-1);
      imDrawList.addImageRounded(i, f, g, f + h, g + h, 0.125F, 0.125F, 0.25F, 0.25F, var8, var7, 240);
      imDrawList.addImageRounded(i, f, g, f + h, g + h, 0.625F, 0.125F, 0.75F, 0.25F, var8, var7, 240);
      imDrawList.addRect(f, g, f + h, g + h, HudChrome.scaleAlpha(402653183, this.alpha), var7, 0, 1.0F);
   }

   public static int face(String string) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      ClientPlayNetworkHandler var4 = var3.getNetworkHandler();
      PlayerListEntry var5 = var4 == null ? null : var4.getPlayerListEntry(string);
      Identifier var6 = var5 != null
         ? var5.getSkinTextures().body().texturePath()
         : var3.getPlayerSkinCache()
            .get(PROFILES.computeIfAbsent(string.toLowerCase(Locale.ROOT), string2 -> ProfileComponent.ofDynamic(string)))
            .getTextures()
            .body()
            .texturePath();

      GpuTexture var2;
      try {
         var2 = var3.getTextureManager().getTexture(var6).getGlTexture();
      } catch (IllegalStateException var8) {
         return 0;
      }

      GlTexture var1;
      if (var2 instanceof GlTexture && !(var1 = (GlTexture)var2).isClosed()) {
         if (NEAREST.add(var2)) {
            int var7 = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, var1.getGlId());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glBindTexture(3553, var7);
         }

         return var1.getGlId();
      } else {
         return 0;
      }
   }

   public int abgr(int i) {
      int var2 = Math.round((i >>> 24 & 0xFF) * this.alpha);
      return GuiPalette.abgr(var2 << 24 | i & 16777215);
   }

   @Environment(EnvType.CLIENT)
   public record Line(int face, String text, int color, String right, int rightColor) {
      public Line(String string, int i) {
         this(0, string, i, "", -6052949);
      }
   }
}
