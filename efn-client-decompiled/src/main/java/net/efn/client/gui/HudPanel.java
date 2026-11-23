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

   public HudPanel(NumberSetting var1, NumberSetting var2, NumberSetting var3, NumberSetting var4) {
      this.x = var1;
      this.y = var2;
      this.scale = var3;
      this.opacity = var4;
      var1.visibleWhen(() -> false);
      var2.visibleWhen(() -> false);
   }

   public static NumberSetting coordinate(String var0) {
      return new NumberSetting(var0, -1.0F, -1.0F, 10000.0F).step(1.0F);
   }

   public static NumberSetting opacitySetting() {
      return new NumberSetting("Opacity", 100.0F, 10.0F, 100.0F).step(5.0F);
   }

   public float[] place(ImDrawList var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      MinecraftClient var8 = MinecraftClient.method_1551();
      this.alpha = Math.max(0.0F, Math.min(1.0F, this.opacity.get() / 100.0F));
      float var9 = this.x.get() < 0.0F ? var6 : this.x.get();
      float var10 = this.y.get() < 0.0F ? var7 : this.y.get();
      boolean var11 = var8.field_1755 instanceof ChatScreen;
      boolean var12 = var11 && GLFW.glfwGetMouseButton(var8.method_22683().method_4490(), 0) == 1;
      float var13 = (float)(var8.field_1729.method_1603() * var4 / Math.max(1, var8.method_22683().method_4480()));
      float var14 = (float)(var8.field_1729.method_1604() * var5 / Math.max(1, var8.method_22683().method_4507()));
      if (var12 && !this.wasDown) {
         if (this.scale != null && var13 >= var9 + var2 - 14.0F && var13 <= var9 + var2 && var14 >= var10 + var3 - 14.0F && var14 <= var10 + var3) {
            this.resizing = true;
            this.scaleAtGrab = this.scale.get();
            this.widthAtGrab = Math.max(1.0F, var2);
            this.grabX = var9 + var2 - var13;
         } else if (var13 >= var9 && var13 <= var9 + var2 && var14 >= var10 && var14 <= var10 + var3) {
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

      var9 = Math.max(0.0F, Math.min(var9, var4 - var2));
      var10 = Math.max(0.0F, Math.min(var10, var5 - var3));
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
         var1.addRect(
            var9 - 1.0F,
            var10 - 1.0F,
            var9 + var2 + 1.0F,
            var10 + var3 + 1.0F,
            GuiPalette.abgr(!this.dragging && !this.resizing ? 1627389951 : -1056964609),
            8.0F,
            0,
            1.0F
         );
         if (this.scale != null) {
            for (int var15 = 1; var15 <= 3; var15++) {
               float var16 = var15 * 4.0F;
               var1.addLine(var9 + var2 - var16, var10 + var3 - 2.0F, var9 + var2 - 2.0F, var10 + var3 - var16, GuiPalette.abgr(-788529153), 1.5F);
            }
         }
      }

      return new float[]{var9, var10};
   }

   public void card(ImDrawList var1, String var2, String var3, int var4, List<HudPanel.Line> var5, String var6, float var7, float var8, float var9, float var10) {
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

      for (HudPanel.Line var24 : var5) {
         var22 |= var24.face() != 0;
      }

      float var43 = var22 ? var20 + var21 : 0.0F;
      float var44 = HudChrome.width(var13, var15, var2);
      float var25 = var44 + (var3.isEmpty() ? 0.0F : 5.0F * var12 + HudChrome.width(var14, var16, var3));

      for (HudPanel.Line var27 : var5) {
         float var28 = var43 + HudChrome.width(var14, var16, var27.text());
         if (!var27.right().isEmpty()) {
            var28 += 14.0F * var12 + HudChrome.width(var13, var15, var27.right());
         }

         var25 = Math.max(var25, var28);
      }

      float var45 = (float)Math.ceil(var25 + var17 * 2.0F);
      float var46 = Math.round(var18 + var19 * var5.size());
      float var47 = 10.0F * var12;
      float[] var29 = this.place(var1, var45, var46, var7, var8, var47 + (var7 - var45 - var47 * 2.0F) * var9, var10);
      float var30 = Math.round(var29[0]);
      float var31 = Math.round(var29[1]);
      Surface var32 = HudTheme.get().forElementId(var6);
      float var33 = Math.min(HudTheme.get().rounding.get(), 6.0F) * var12;
      HudModule var34 = ModuleRegistry.get(HudModule.class);
      if (var34 == null || var34.shadow()) {
         HudChrome.shadow(var1, var30, var31, var45, var46, var33, this.alpha, var12);
      }

      if (var32 != null) {
         var1.addRectFilled(var30, var31, var30 + var45, var31 + var46, HudChrome.scaleAlpha(GuiPalette.fill(var32), this.alpha), var33);
         SurfaceBackground.draw(var1, var32, var30, var31, var45, var46, var33, this.alpha);
      } else {
         var1.addRectFilled(var30, var31, var30 + var45, var31 + var46, HudChrome.scaleAlpha(HudChrome.CHIP_FILL, this.alpha), var33);
      }

      float var35 = HudChrome.baselineOffset(var14, var16, var13, var15);
      float var36 = var31 + (float)Math.floor((var18 - var16) * 0.5F);
      HudChrome.text(var1, var13, var15, var30 + var17, var36 + var35, this.abgr(-6052949), var2, true);
      if (!var3.isEmpty()) {
         HudChrome.text(var1, var14, var16, var30 + var17 + var44 + 5.0F * var12, var36, this.abgr(var4), var3, true);
      }

      float var37 = (float)Math.floor((var19 - var16) * 0.5F);

      for (int var38 = 0; var38 < var5.size(); var38++) {
         HudPanel.Line var39 = (HudPanel.Line)var5.get(var38);
         float var40 = Math.round(var31 + var18 + var19 * var38);
         float var41 = var40 + var37;
         if (var39.face() != 0) {
            this.drawFace(var1, var39.face(), Math.round(var30 + var17), Math.round(var40 + (var19 - var20) * 0.5F), var20, var12);
         }

         HudChrome.text(var1, var14, var16, var30 + var17 + var43, var41, this.abgr(var39.color()), var39.text(), true);
         if (!var39.right().isEmpty()) {
            float var42 = var30 + var45 - var17 - HudChrome.width(var13, var15, var39.right());
            HudChrome.text(var1, var13, var15, var42, var41 + var35, this.abgr(var39.rightColor()), var39.right(), true);
         }
      }
   }

   private static float faceSize(float var0) {
      int var1 = Math.round(Math.round(16.0F * var0) / 8.0F) * 8;
      if (var1 > 22.0F * var0 - 4.0F * var0) {
         var1 -= 8;
      }

      return Math.max(8, var1);
   }

   public void drawFace(ImDrawList var1, int var2, float var3, float var4, float var5, float var6) {
      float var7 = Math.max(1.0F, (float)Math.round(2.0F * var6));
      int var8 = this.abgr(-1);
      var1.addImageRounded(var2, var3, var4, var3 + var5, var4 + var5, 0.125F, 0.125F, 0.25F, 0.25F, var8, var7, 240);
      var1.addImageRounded(var2, var3, var4, var3 + var5, var4 + var5, 0.625F, 0.125F, 0.75F, 0.25F, var8, var7, 240);
      var1.addRect(var3, var4, var3 + var5, var4 + var5, HudChrome.scaleAlpha(402653183, this.alpha), var7, 0, 1.0F);
   }

   public static int face(String var0) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      ClientPlayNetworkHandler var4 = var3.method_1562();
      PlayerListEntry var5 = var4 == null ? null : var4.method_2874(var0);
      Identifier var6 = var5 != null
         ? var5.method_52810().comp_1626().comp_3627()
         : var3.method_73362()
            .method_73495(PROFILES.computeIfAbsent(var0.toLowerCase(Locale.ROOT), var1x -> ProfileComponent.method_74889(var0)))
            .method_73503()
            .comp_1626()
            .comp_3627();

      GpuTexture var2;
      try {
         var2 = var3.method_1531().method_4619(var6).method_68004();
      } catch (IllegalStateException var8) {
         return 0;
      }

      GlTexture var1;
      if (var2 instanceof GlTexture && !(var1 = (GlTexture)var2).isClosed()) {
         if (NEAREST.add(var2)) {
            int var7 = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, var1.method_68427());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glBindTexture(3553, var7);
         }

         return var1.method_68427();
      } else {
         return 0;
      }
   }

   public int abgr(int var1) {
      int var2 = Math.round((var1 >>> 24 & 0xFF) * this.alpha);
      return GuiPalette.abgr(var2 << 24 | var1 & 16777215);
   }

   @Environment(EnvType.CLIENT)
   public record Line(int face, String text, int color, String right, int rightColor) {
      public Line(String var1, int var2) {
         this(0, var1, var2, "", -6052949);
      }
   }
}
