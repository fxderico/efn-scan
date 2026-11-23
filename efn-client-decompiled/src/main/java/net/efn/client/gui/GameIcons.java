package net.efn.client.gui;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
final class GameIcons {
   private static final Map<Identifier, ThemeImages.Texture> TEXTURES = new HashMap<>();
   private static final Set<Identifier> MISSING = new HashSet<>();

   private GameIcons() {
   }

   static ThemeImages.Texture effect(RegistryEntry<StatusEffect> var0) {
      Identifier var1 = var0.method_40230().<Identifier>map(RegistryKey::method_29177).orElse(null);
      return var1 == null ? null : texture(Identifier.method_60655(var1.method_12836(), "textures/mob_effect/" + var1.method_12832() + ".png"));
   }

   static ThemeImages.Texture block(String var0) {
      return texture(Identifier.method_60656("textures/block/" + var0 + ".png"));
   }

   private static ThemeImages.Texture texture(Identifier var0) {
      ThemeImages.Texture var1 = TEXTURES.get(var0);
      if (var1 == null && !MISSING.contains(var0)) {
         try (InputStream var2 = MinecraftClient.method_1551().method_1478().open(var0)) {
            var1 = ThemeImages.decode(var2.readAllBytes(), var0.toString(), true);
         } catch (RuntimeException | IOException var7) {
            var1 = null;
         }

         if (var1 == null) {
            MISSING.add(var0);
         } else {
            TEXTURES.put(var0, var1);
         }

         return var1;
      } else {
         return var1;
      }
   }
}
