package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

@Environment(EnvType.CLIENT)
final class KnifeMaterial {
   static final int BLADE = 0;
   static final int BLADE_DARK = 1;
   static final int EDGE = 2;
   static final int HANDLE = 3;
   static final int HANDLE_DARK = 4;
   static final int METAL = 5;
   static final int ACCENT = 6;
   static final int WRAP = 7;
   static final int GOLD = 8;
   private static final KnifeMaterial DIAMOND = new KnifeMaterial(7340016, 4187087, 1941139, 13828087, 1780531, 989213, 12109772, 4187087, 1391168, 15253850);
   private static final KnifeMaterial NETHERITE = new KnifeMaterial(16742973, 6445663, 3813942, 11773356, 1709592, 920333, 7234665, 13715758, 3806228, 13213770);
   private static final KnifeMaterial IRON = new KnifeMaterial(16777215, 14277081, 10197915, 16777215, 4862754, 2825490, 9342606, 13158600, 2829099, 14267466);
   private static final KnifeMaterial GOLDEN = new KnifeMaterial(
      16769126, 16242762, 13015574, 16775096, 5905950, 3674128, 15188812, 16770688, 7216927, 15912795
   );
   private static final KnifeMaterial COPPER = new KnifeMaterial(16755319, 15043163, 11096108, 16763302, 3096378, 1844771, 6274975, 6274975, 2375738, 14263130);
   private static final KnifeMaterial STONE = new KnifeMaterial(14540253, 10263708, 6710886, 13158600, 4862754, 2825490, 8026746, 11053224, 3811868, 12558416);
   private static final KnifeMaterial WOOD = new KnifeMaterial(15782560, 12290902, 8279854, 14529402, 5914146, 3679763, 9071172, 13081442, 4861976, 13213770);
   final int[] palette;
   final int glow;

   private KnifeMaterial(int var1, int... var2) {
      this.glow = var1;
      this.palette = new int[var2.length];

      for (int var3 = 0; var3 < var2.length; var3++) {
         this.palette[var3] = 0xFF000000 | var2[var3];
      }
   }

   static KnifeMaterial of(ItemStack var0) {
      return named(Registries.field_41178.method_10221(var0.method_7909()).method_12832());
   }

   static KnifeMaterial named(String var0) {
      if (var0.startsWith("netherite")) {
         return NETHERITE;
      } else if (var0.startsWith("diamond")) {
         return DIAMOND;
      } else if (var0.startsWith("golden")) {
         return GOLDEN;
      } else if (var0.startsWith("copper")) {
         return COPPER;
      } else if (var0.startsWith("stone")) {
         return STONE;
      } else {
         return var0.startsWith("wooden") ? WOOD : IRON;
      }
   }

   static boolean blade(int var0) {
      return var0 == 0 || var0 == 1 || var0 == 2;
   }

   static boolean metallic(int var0) {
      return var0 == 0 || var0 == 1 || var0 == 2 || var0 == 5 || var0 == 8;
   }
}
