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

   static ThemeImages.Texture effect(RegistryEntry<StatusEffect> registryEntry) {
      Identifier var1 = registryEntry.getKey().map(RegistryKey::getValue).orElse(null);
      return var1 == null ? null : texture(Identifier.of(var1.getNamespace(), "textures/mob_effect/" + var1.getPath() + ".png"));
   }

   static ThemeImages.Texture block(String string) {
      return texture(Identifier.ofVanilla("textures/block/" + string + ".png"));
   }

   private static ThemeImages.Texture texture(Identifier identifier) {
      ThemeImages.Texture var1 = TEXTURES.get(identifier);
      if (var1 == null && !MISSING.contains(identifier)) {
         try (InputStream var2 = MinecraftClient.getInstance().getResourceManager().open(identifier)) {
            var1 = ThemeImages.decode(var2.readAllBytes(), identifier.toString(), true);
         } catch (RuntimeException | IOException var7) {
            var1 = null;
         }

         if (var1 == null) {
            MISSING.add(identifier);
         } else {
            TEXTURES.put(identifier, var1);
         }

         return var1;
      } else {
         return var1;
      }
   }
}
