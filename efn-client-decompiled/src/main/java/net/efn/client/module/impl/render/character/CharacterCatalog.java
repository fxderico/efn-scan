package net.efn.client.module.impl.render.character;

import java.util.ArrayList;
import java.util.List;
import net.efn.client.module.impl.render.character.designs.Aoi;
import net.efn.client.module.impl.render.character.designs.Luna;
import net.efn.client.module.impl.render.character.designs.Mika;
import net.efn.client.module.impl.render.character.designs.Miku;
import net.efn.client.module.impl.render.character.designs.Sakura;
import net.efn.client.module.impl.render.character.designs.Tralalero;
import net.efn.client.module.impl.render.character.designs.TungTungSahur;
import net.efn.client.module.impl.render.character.kit.BakedDesign;
import net.efn.client.module.impl.render.character.kit.CharacterDesign;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CharacterCatalog {
   private static final List<CharacterDesign> BUILT_IN = List.of(
      new Sakura(),
      new Luna(),
      new Mika(),
      new Aoi(),
      new Miku(),
      BakedDesign.builtIn("cj", "CJ", "/assets/efn/characters/cj.anc"),
      BakedDesign.builtIn("neighbor", "Neighbor", "/assets/efn/characters/neighbor.anc"),
      new TungTungSahur(),
      new Tralalero()
   );
   private static volatile List<CharacterDesign> designs = BUILT_IN;

   private CharacterCatalog() {
   }

   public static List<CharacterDesign> all() {
      return designs;
   }

   static boolean refresh() {
      ArrayList var0 = new ArrayList<>(BUILT_IN);
      ArrayList var1 = new ArrayList<>(names(BUILT_IN));

      for (BakedDesign var3 : CustomCharacters.scan()) {
         if (!var1.contains(var3.name())) {
            var1.add(var3.name());
            var0.add(var3);
         }
      }

      List var4 = designs;
      designs = List.copyOf(var0);
      if (var4.size() != var0.size()) {
         return true;
      } else {
         for (int var5 = 0; var5 < var0.size(); var5++) {
            if (!((CharacterDesign)var4.get(var5)).id().equals(((CharacterDesign)var0.get(var5)).id())) {
               return true;
            }
         }

         return false;
      }
   }

   static List<String> names() {
      return names(designs);
   }

   private static List<String> names(List<CharacterDesign> var0) {
      ArrayList var1 = new ArrayList(var0.size());

      for (CharacterDesign var3 : var0) {
         var1.add(var3.name());
      }

      return var1;
   }

   static CharacterDesign byName(String var0) {
      List var1 = designs;

      for (CharacterDesign var3 : var1) {
         if (var3.name().equals(var0)) {
            return var3;
         }
      }

      return (CharacterDesign)var1.get(0);
   }
}
