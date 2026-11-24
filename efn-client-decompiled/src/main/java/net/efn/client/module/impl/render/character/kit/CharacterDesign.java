package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class CharacterDesign {
   private final String id;
   private final String name;

   protected CharacterDesign(String string, String string2) {
      if (string != null && string.matches("[a-z0-9_]+")) {
         this.id = string;
         this.name = string2;
      } else {
         throw new IllegalArgumentException("character id must match [a-z0-9_]+: " + string);
      }
   }

   public final String id() {
      return this.id;
   }

   public final String name() {
      return this.name;
   }

   protected abstract void define(Rig rig);

   public final Rig rig() {
      Rig var1 = new Rig();
      this.define(var1);
      return var1;
   }
}
