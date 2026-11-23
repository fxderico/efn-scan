package net.efn.client.gui.mainmenu;

import com.mojang.blaze3d.textures.GpuTexture;
import imgui.ImDrawList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.efn.client.account.Account;
import net.efn.client.account.AccountType;
import net.efn.client.gui.HudDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.PlayerSkinCache.Entry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
final class PlayerFaces {
   private static final float FACE_U = 0.125F;
   private static final float HAT_U = 0.625F;
   private static final float TOP_V = 0.125F;
   private static final float PART = 0.125F;
   private static final Map<UUID, Supplier<SkinTextures>> SKINS = new HashMap<>();
   private static final Set<GpuTexture> NEAREST = Collections.newSetFromMap(new WeakHashMap<>());

   private PlayerFaces() {
   }

   static void draw(ImDrawList var0, Account var1, float var2, float var3, float var4, float var5, float var6) {
      UUID var7;
      try {
         var7 = UUID.fromString(var1.uuid());
      } catch (IllegalArgumentException var9) {
         placeholder(var0, var2, var3, var4, var5, var6);
         return;
      }

      draw(var0, var7, var1.type() == AccountType.CRACKED, var2, var3, var4, var5, var6);
   }

   static void draw(ImDrawList var0, UUID var1, boolean var2, float var3, float var4, float var5, float var6, float var7) {
      int var8 = texture(skin(var1, var2).comp_1626().comp_3627());
      if (var8 == 0) {
         placeholder(var0, var3, var4, var5, var6, var7);
      } else {
         float var9 = Math.max(1.0F, (float)Math.round(3.0F * var6));
         int var10 = HudDraw.col(-1, var7);
         var0.addImageRounded(var8, var3, var4, var3 + var5, var4 + var5, 0.125F, 0.125F, 0.25F, 0.25F, var10, var9, 240);
         var0.addImageRounded(var8, var3, var4, var3 + var5, var4 + var5, 0.625F, 0.125F, 0.75F, 0.25F, var10, var9, 240);
      }
   }

   private static void placeholder(ImDrawList var0, float var1, float var2, float var3, float var4, float var5) {
      var0.addRectFilled(var1, var2, var1 + var3, var2 + var3, HudDraw.col(-7693914, 0.3F * var5), Math.round(3.0F * var4));
   }

   private static SkinTextures skin(UUID var0, boolean var1) {
      return var1 ? DefaultSkinHelper.method_4648(var0) : SKINS.computeIfAbsent(var0, var0x -> {
         Supplier var1x = MinecraftClient.method_1551().method_73362().method_74050(ProfileComponent.method_73312(var0x));
         return () -> ((Entry)var1x.get()).method_73503();
      }).get();
   }

   private static int texture(Identifier var0) {
      GpuTexture var2;
      try {
         var2 = MinecraftClient.method_1551().method_1531().method_4619(var0).method_68004();
      } catch (IllegalStateException var4) {
         return 0;
      }

      GlTexture var1;
      if (var2 instanceof GlTexture && !(var1 = (GlTexture)var2).isClosed()) {
         if (NEAREST.add(var2)) {
            int var3 = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, var1.method_68427());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glBindTexture(3553, var3);
         }

         return var1.method_68427();
      } else {
         return 0;
      }
   }
}
