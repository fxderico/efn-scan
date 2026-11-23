package net.efn.client.module.impl.render;

import java.util.Arrays;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public final class CrownModule extends HeadCosmeticModule {
   private static final String STYLE_CROWN = "Crown";
   private static final String STYLE_HALO = "Halo";
   private static final String STYLE_SHARDS = "Shards";
   private final ModeSetting style = this.addSetting("style", new ModeSetting("Style", "Crown", "Crown", "Halo", "Shards"));
   private final NumberSetting size = this.addSetting("size", new NumberSetting("Size", 1.0F, 0.8F, 1.3F).step(0.05F));
   private final ModeSetting color = this.addSetting("color", new ModeSetting("Color", "Theme", "Theme", "Manual"));
   private final ColorSetting gold = this.addSetting("manual-gold", new ColorSetting("Main Color", -7688204));
   private final ColorSetting inlay = this.addSetting("manual-inlay", new ColorSetting("Trim Color", -13738055));
   private final ColorSetting gem = this.addSetting("manual-gem", new ColorSetting("Gem Color", -10313473));
   private final BooleanSetting gemGlow = this.addSetting("gem-glow", new BooleanSetting("Gem Glow", true));
   private final BooleanSetting glint = this.addSetting("glint", new BooleanSetting("Glint", true));
   private final BooleanSetting friends = this.addSetting("friends", new BooleanSetting("On Friends", false));
   private final CrownMesh[] meshes = new CrownMesh[4];
   private long styleRevision = -1L;

   public CrownModule() {
      super("efn-crown", "EFN Crown", "Puts a glowing EFN crown, halo or crystal shards on your player model.");
      this.gold.visibleWhen(() -> this.color.is("Manual"));
      this.inlay.visibleWhen(() -> this.color.is("Manual"));
      this.gem.visibleWhen(() -> this.color.is("Manual"));
   }

   @Override
   void reset() {
      super.reset();
      Arrays.fill(this.meshes, null);
      this.styleRevision = -1L;
   }

   @Override
   CrownMesh headMesh(boolean var1) {
      this.refreshMeshes();
      int var2 = var1 ? 1 : 0;
      CrownMesh var3 = this.meshes[var2];
      if (var3 == null) {
         this.meshes[var2] = var3 = CrownMesh.head(this.styleIndex(), 0, var1);
      }

      return var3;
   }

   @Override
   CrownMesh bodyMesh(boolean var1) {
      this.refreshMeshes();
      int var2 = var1 ? 3 : 2;
      CrownMesh var3 = this.meshes[var2];
      if (var3 == null) {
         this.meshes[var2] = var3 = CrownMesh.broadCollar(var1, false);
      }

      return var3;
   }

   private void refreshMeshes() {
      long var1 = this.style.revision();
      if (var1 != this.styleRevision) {
         this.styleRevision = var1;
         Arrays.fill(this.meshes, null);
      }
   }

   private int styleIndex() {
      if (this.style.is("Halo")) {
         return 1;
      } else {
         return this.style.is("Shards") ? 2 : 0;
      }
   }

   @Override
   float scale() {
      return this.size.get();
   }

   @Override
   int[] palette() {
      int[] var1 = new int[5];
      if (this.color.is("Manual")) {
         int var2;
         var1[0] = var2 = this.gold.argb() | 0xFF000000;
         var1[1] = this.inlay.argb() | 0xFF000000;
         var1[2] = this.gem.argb() | 0xFF000000;
         var1[3] = bronze(var2);
      } else {
         int var3 = CosmeticAccent.argb();
         int var4;
         var1[0] = var4 = metal(var3);
         var1[1] = ColorHelper.method_61321(var3, 0.78F);
         var1[2] = gem(var3);
         var1[3] = ColorHelper.method_61321(var4, 0.62F);
      }

      return var1;
   }

   @Override
   boolean glows() {
      return this.gemGlow.get();
   }

   @Override
   boolean glints() {
      return this.glint.get();
   }

   @Override
   boolean onFriends() {
      return this.friends.get();
   }

   private static int bronze(int var0) {
      int var1 = (int)((var0 >> 16 & 0xFF) * 0.7F);
      int var2 = (int)((var0 >> 8 & 0xFF) * 0.55F);
      int var3 = (int)((var0 & 0xFF) * 0.4F);
      return 0xFF000000 | var1 << 16 | var2 << 8 | var3;
   }
}
