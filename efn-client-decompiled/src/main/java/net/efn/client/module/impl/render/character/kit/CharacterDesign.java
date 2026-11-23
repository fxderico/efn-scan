package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class CharacterDesign {
   private final String id;
   private final String name;

   protected CharacterDesign(String var1, String var2) {
      if (var1 != null && var1.matches("[a-z0-9_]+")) {
         this.id = var1;
         this.name = var2;
      } else {
         throw new IllegalArgumentException("character id must match [a-z0-9_]+: " + var1);
      }
   }

   public final String id() {
      return this.id;
   }

   public final String name() {
      return this.name;
   }

   protected abstract void define(Rig var1);

   public final Rig rig() {
      Rig var1 = new Rig();
      this.define(var1);
      return var1;
   }
}
