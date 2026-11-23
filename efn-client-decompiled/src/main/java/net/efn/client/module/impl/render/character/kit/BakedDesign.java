package net.efn.client.module.impl.render.character.kit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class BakedDesign extends CharacterDesign {
   private final BakedDesign.Source source;

   public BakedDesign(String var1, String var2, BakedDesign.Source var3) {
      super(var1, var2);
      this.source = var3;
   }

   public static BakedDesign builtIn(String var0, String var1, String var2) {
      return new BakedDesign(var0, var1, () -> {
         InputStream var1x = BakedDesign.class.getResourceAsStream(var2);
         if (var1x == null) {
            throw new FileNotFoundException(var2);
         } else {
            return var1x;
         }
      });
   }

   InputStream open() throws IOException {
      return this.source.open();
   }

   @Override
   protected void define(Rig var1) {
      throw new UnsupportedOperationException(this.id() + " is baked and has no rig");
   }

   @Environment(EnvType.CLIENT)
   public interface Source {
      InputStream open() throws IOException;
   }
}
