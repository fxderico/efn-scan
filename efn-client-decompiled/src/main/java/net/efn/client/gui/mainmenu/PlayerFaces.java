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

   static void draw(ImDrawList imDrawList, Account account, float f, float g, float h, float i, float j) {
      UUID var7;
      try {
         var7 = UUID.fromString(account.uuid());
      } catch (IllegalArgumentException var9) {
         placeholder(imDrawList, f, g, h, i, j);
         return;
      }

      draw(imDrawList, var7, account.type() == AccountType.CRACKED, f, g, h, i, j);
   }

   static void draw(ImDrawList imDrawList, UUID uUID, boolean bl, float f, float g, float h, float i, float j) {
      int var8 = texture(skin(uUID, bl).body().texturePath());
      if (var8 == 0) {
         placeholder(imDrawList, f, g, h, i, j);
      } else {
         float var9 = Math.max(1.0F, (float)Math.round(3.0F * i));
         int var10 = HudDraw.col(-1, j);
         imDrawList.addImageRounded(var8, f, g, f + h, g + h, 0.125F, 0.125F, 0.25F, 0.25F, var10, var9, 240);
         imDrawList.addImageRounded(var8, f, g, f + h, g + h, 0.625F, 0.125F, 0.75F, 0.25F, var10, var9, 240);
      }
   }

   private static void placeholder(ImDrawList imDrawList, float f, float g, float h, float i, float j) {
      imDrawList.addRectFilled(f, g, f + h, g + h, HudDraw.col(-7693914, 0.3F * j), Math.round(3.0F * i));
   }

   private static SkinTextures skin(UUID uUID, boolean bl) {
      return bl ? DefaultSkinHelper.getSkinTextures(uUID) : SKINS.computeIfAbsent(uUID, uUIDx -> {
         Supplier var1 = MinecraftClient.getInstance().getPlayerSkinCache().getSupplier(ProfileComponent.ofDynamic(uUIDx));
         return () -> ((Entry)var1.get()).getTextures();
      }).get();
   }

   private static int texture(Identifier identifier) {
      GpuTexture var2;
      try {
         var2 = MinecraftClient.getInstance().getTextureManager().getTexture(identifier).getGlTexture();
      } catch (IllegalStateException var4) {
         return 0;
      }

      GlTexture var1;
      if (var2 instanceof GlTexture && !(var1 = (GlTexture)var2).isClosed()) {
         if (NEAREST.add(var2)) {
            int var3 = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, var1.getGlId());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glBindTexture(3553, var3);
         }

         return var1.getGlId();
      } else {
         return 0;
      }
   }
}
