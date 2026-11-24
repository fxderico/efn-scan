package net.efn.client.module.impl.render.character.kit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class BakedDesign extends CharacterDesign {
   private final BakedDesign.Source source;

   public BakedDesign(String string, String string2, BakedDesign.Source source) {
      super(string, string2);
      this.source = source;
   }

   public static BakedDesign builtIn(String string, String string2, String string3) {
      return new BakedDesign(string, string2, () -> {
         InputStream var1 = BakedDesign.class.getResourceAsStream(string3);
         if (var1 == null) {
            throw new FileNotFoundException(string3);
         } else {
            return var1;
         }
      });
   }

   InputStream open() throws IOException {
      return this.source.open();
   }

   @Override
   protected void define(Rig rig) {
      throw new UnsupportedOperationException(this.id() + " is baked and has no rig");
   }

   @Environment(EnvType.CLIENT)
   public interface Source {
      InputStream open() throws IOException;
   }
}
