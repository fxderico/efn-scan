package net.efn.client.module.impl.render.character;

import net.efn.client.module.impl.render.character.kit.Lights;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting.Type;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.dimension.DimensionType.CardinalLightType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class CharacterLighting {
   private static final Vector3f LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
   private static final Vector3f LIGHT_1 = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();
   private static final Lights NETHER = new Lights(0.2F, 1.0F, -0.7F, -0.2F, -1.0F, 0.7F);
   private static final Lights INVENTORY = new Lights(0.2F, -1.0F, 1.0F, -0.2F, -1.0F, 0.0F);
   private static final Lights ITEMS_FLAT = transformed(new Matrix4f().rotationY(-0.3926991F).rotateX(2.3561945F));
   private static final Lights ITEMS_3D = transformed(
      new Matrix4f().scaling(1.0F, -1.0F, 1.0F).rotateYXZ(1.0821041F, 3.2375858F, 0.0F).rotateYXZ(-0.3926991F, 2.3561945F, 0.0F)
   );
   private static Type entry = Type.field_60025;

   private CharacterLighting() {
   }

   public static void setup(Type var0) {
      entry = var0;
   }

   static Lights current() {
      return switch (entry) {
         case field_60025 -> nether() ? NETHER : Lights.WORLD;
         case field_60026 -> ITEMS_FLAT;
         case field_60027 -> ITEMS_3D;
         case field_60028, field_60029 -> INVENTORY;
         default -> throw new MatchException(null, null);
      };
   }

   private static boolean nether() {
      ClientWorld var0 = MinecraftClient.method_1551().field_1687;
      return var0 != null && var0.method_8597().comp_5221() == CardinalLightType.field_64381;
   }

   private static Lights transformed(Matrix4f var0) {
      Vector3f var1 = var0.transformDirection(LIGHT_0, new Vector3f());
      Vector3f var2 = var0.transformDirection(LIGHT_1, new Vector3f());
      return new Lights(var1.x, var1.y, var1.z, var2.x, var2.y, var2.z);
   }
}
