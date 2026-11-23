package net.efn.client.module.impl.render.character.kit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CharacterBuilder {
   private static final float DECAL_RESERVE = 0.1F;
   private static final float AO_STEP = 0.38F;
   private static final int MIN_TRIANGLES = 24;
   private static final float MAX_EXTENT = 160.0F;
   private static final float OUTLINE_SHARE = 0.5F;
   private static final float BURIED = 0.02F;
   private static final float SINK = 0.12F;
   private static final int CORNER = 10;

   private CharacterBuilder() {
   }

   public static CharacterModel build(CharacterDesign var0, Detail var1) {
      if (var0 instanceof BakedDesign var33) {
         return BakedModel.load(var33, var1);
      } else {
         long var2 = System.nanoTime();
         Rig var4 = var0.rig();
         List var5 = order(var4);
         int var6 = var5.size();
         if (var6 == 0) {
            throw new IllegalStateException(var0.id() + ": the design defines no parts");
         } else {
            IdentityHashMap var7 = new IdentityHashMap();

            for (int var8 = 0; var8 < var6; var8++) {
               var7.put((Part)var5.get(var8), var8);
            }

            ArrayList var34 = new ArrayList();
            ArrayList var9 = new ArrayList();
            Shape[] var10 = new Shape[var6];
            float[] var11 = new float[var6];

            for (int var12 = 0; var12 < var6; var12++) {
               Part var13 = (Part)var5.get(var12);
               if (var13.shape != null) {
                  Shape var14 = var13.shape;
                  if (!var14.bounded()) {
                     throw new IllegalStateException(var13.name + ": shape is unbounded; intersect planes with a box");
                  }

                  if (var14.maxX - var14.minX > 160.0F || var14.maxY - var14.minY > 160.0F || var14.maxZ - var14.minZ > 160.0F) {
                     throw new IllegalStateException(var13.name + ": shape is larger than 160.0 units");
                  }

                  var11[var12] = SurfaceNets.fitStep(var14.maxX - var14.minX, var14.maxY - var14.minY, var14.maxZ - var14.minZ, var1.step * var13.resolution);
                  float var15 = var11[var12] * 0.75F;
                  var14 = var14.map(var1x -> {
                     Object var2x;
                     if (var1x instanceof Strand var3) {
                        var2x = var3.thickened(var15);
                     } else {
                        var2x = var1x;
                     }

                     return (Shape)var2x;
                  });
                  var10[var12] = var14;
                  var34.add(var14);
               }

               for (Decal var40 : var13.decals) {
                  var9.add(var40.canvas);
               }
            }

            Shape var35 = var34.isEmpty() ? null : Shapes.union(var34.toArray(new Shape[0]));
            Shape[] var36 = covers(var5, var10);
            Atlas var39 = Atlas.pack(var9);
            float[] var41 = new float[var6];

            for (int var16 = 0; var16 < var6; var16++) {
               Part var17 = (Part)var5.get(var16);
               var41[var16] = var17.outline >= 0.0F ? var17.outline : var4.outline;
            }

            Mesh[] var42 = new Mesh[var6];
            float[] var43 = new float[var6];
            IntStream.range(0, var6).parallel().forEach(var5x -> {
               Shape var6x = var10[var5x];
               if (var6x != null) {
                  var42[var5x] = SurfaceNets.mesh(var6x, var6x.minX, var6x.minY, var6x.minZ, var6x.maxX, var6x.maxY, var6x.maxZ, var11[var5x]);
                  var43[var5x] = visible(var42[var5x], var36[var5x]);
               }
            });
            int[] var18 = new int[var6];
            budget(var5, var42, var43, var1, var18, var41);
            Mesh[] var19 = new Mesh[var6];
            IntStream.range(0, var6).parallel().forEach(var3 -> {
               if (var42[var3] != null && var42[var3].triCount != 0) {
                  Simplifier var4x = new Simplifier(var42[var3]);
                  var4x.reduce(var18[var3]);
                  var19[var3] = var4x.snapshot();
               }
            });
            PartMesh[] var20 = new PartMesh[var6];
            float[] var21 = new float[]{3.4028235E38F, -3.4028235E38F};
            int[] var22 = new int[var6];
            int var23 = 0;

            for (int var24 = 0; var24 < var6; var24++) {
               Part var25 = (Part)var5.get(var24);
               var22[var24] = var25.extra && var25.motion != null ? var23++ : -1;
            }

            IntStream.range(0, var6)
               .parallel()
               .forEach(
                  var13x -> {
                     Part var16x = (Part)var5.get(var13x);
                     Surface var17x = null;
                     Surface var18x = null;
                     Surface var19x = null;
                     if (var19[var13x] != null && var19[var13x].triCount > 0) {
                        float var22x = var16x.occlusion >= 0.0F ? var16x.occlusion : var4.occlusion;
                        CharacterBuilder.Shaded var23x = shade(
                           var10[var13x], var35, var19[var13x], var11[var13x], var22x, var16x.flat >= 0.0F ? var16x.flat : var4.flat
                        );
                        Mesh var20x;
                        var17x = baseSurface(var23x, var20x = bury(var23x, var19[var13x], var36[var13x], var11[var13x]), var16x, var39);
                        if (var17x != null && var41[var13x] > 0.0F) {
                           int var24x = var16x.outlineColorSet ? var16x.outlineColor : var4.outlineColor;
                           var18x = hullSurface(var10[var13x], var23x, var17x, var16x, var41[var13x], var11[var13x], var24x, var39);
                        }

                        if (!var16x.decals.isEmpty()) {
                           var19x = decalSurface(var16x, var23x, var20x, var39);
                        }

                        for (int var25x = 0; var25x < var23x.count; var25x++) {
                           var21[0] = Math.min(var21[0], var23x.pos[var25x * 3 + 1]);
                           var21[1] = Math.max(var21[1], var23x.pos[var25x * 3 + 1]);
                        }
                     }

                     int var26x = var16x.parent == null ? -1 : (Integer)var7.get(var16x.parent);
                     Motion var15x = var16x.extra ? var16x.motion : null;
                     if (var15x != null && var15x.bendPower() > 0.0F) {
                        float var27x = reach(var17x);
                        weigh(var17x, var27x, var15x.bendPower());
                        if (var18x != null && var17x != null) {
                           var18x.weight = var17x.weight;
                        }

                        weigh(var19x, var27x, var15x.bendPower());
                     }

                     boolean var28x = !var16x.extra && (var16x.bone.arm() || var16x.bone.leg());
                     Part var29x = var16x;

                     while (var29x != null && !var29x.jointSet) {
                        var29x = var29x.parent;
                     }

                     boolean var14x = var29x != null;
                     if (var14x) {
                        jointWeights(var17x, var16x, var29x);
                        jointWeights(var19x, var16x, var29x);
                        if (var18x != null && var17x != null) {
                           var18x.joint = var17x.joint;
                        }
                     }

                     var20[var13x] = new PartMesh(
                        var16x.name,
                        var16x.bone,
                        var26x,
                        var16x.extra,
                        var16x.pivotX,
                        var16x.pivotY,
                        var16x.pivotZ,
                        var16x.gripX(),
                        var16x.gripY(),
                        var16x.gripZ(),
                        var15x,
                        var17x,
                        var18x,
                        var19x,
                        var22[var13x],
                        var28x ? var16x.swing : 1.0F,
                        var28x ? var16x.swingLimit : 0.0F,
                        var14x,
                        var14x ? var29x.jointX - var16x.pivotX : 0.0F,
                        var14x ? var29x.jointY - var16x.pivotY : 0.0F,
                        var14x ? var29x.jointZ - var16x.pivotZ : 0.0F,
                        var14x && var29x.neck,
                        var14x ? var29x.lookPitch : 0.0F,
                        var14x ? var29x.lookYaw : 0.0F,
                        var14x ? var29x.lookLimit : 0.0F
                     );
                  }
               );
            long var44 = System.nanoTime();
            float var26 = 12.0F;
            Part var27 = var4.bones[Bone.RIGHT_LEG.ordinal()];
            Part var28 = var4.bones[Bone.LEFT_LEG.ordinal()];
            if (var27 != null && var28 != null) {
               var26 = (var27.pivotY + var28.pivotY) * 0.5F;
            } else if (var27 != null) {
               var26 = var27.pivotY;
            } else if (var28 != null) {
               var26 = var28.pivotY;
            }

            float var29 = var21[0] == 3.4028235E38F ? 0.0F : var21[0];
            float var30 = var21[1] == -3.4028235E38F ? 32.0F : var21[1];
            long var31 = (var44 - var2) / 1000000L;
            return new CharacterModel(
               var0.id(),
               var0.name(),
               var1,
               var4.scale,
               var20,
               var23,
               var39.width,
               var39.height,
               var39.mips,
               var39.whiteU(),
               var39.whiteV(),
               var29,
               var30,
               var26,
               var4.firstPersonArms,
               var31
            );
         }
      }
   }

   private static List<Part> order(Rig var0) {
      ArrayList var1 = new ArrayList();

      for (Part var5 : var0.bones) {
         if (var5 != null) {
            var1.add(var5);
         }
      }

      var1.addAll(var0.extras);
      return var1;
   }

   private static Shape[] covers(List<Part> var0, Shape[] var1) {
      int var2 = var0.size();
      Part[] var3 = new Part[var2];

      for (int var4 = 0; var4 < var2; var4++) {
         Part var5 = (Part)var0.get(var4);

         while (var5.extra && var5.motion == null && var5.parent != null) {
            var5 = var5.parent;
         }

         var3[var4] = var5;
      }

      Shape[] var8 = new Shape[var2];

      for (int var9 = 0; var9 < var2; var9++) {
         if (var1[var9] != null) {
            ArrayList var6 = new ArrayList();

            for (int var7 = 0; var7 < var2; var7++) {
               if (var7 != var9 && var1[var7] != null && var3[var7] == var3[var9] && overlaps(var1[var9], var1[var7])) {
                  var6.add(var1[var7]);
               }
            }

            if (!var6.isEmpty()) {
               var8[var9] = Shapes.union(var6.toArray(new Shape[0]));
            }
         }
      }

      return var8;
   }

   private static boolean overlaps(Shape var0, Shape var1) {
      return var0.minX <= var1.maxX
         && var1.minX <= var0.maxX
         && var0.minY <= var1.maxY
         && var1.minY <= var0.maxY
         && var0.minZ <= var1.maxZ
         && var1.minZ <= var0.maxZ;
   }

   private static float visible(Mesh var0, Shape var1) {
      if (var1 != null && var0.triCount != 0) {
         boolean[] var2 = new boolean[var0.vertexCount];

         for (int var3 = 0; var3 < var0.vertexCount; var3++) {
            var2[var3] = var1.distance(var0.pos[var3 * 3], var0.pos[var3 * 3 + 1], var0.pos[var3 * 3 + 2]) < -0.02F;
         }

         int var5 = 0;

         for (int var4 = 0; var4 < var0.triCount; var4++) {
            if (var2[var0.tris[var4 * 3]] && var2[var0.tris[var4 * 3 + 1]] && var2[var0.tris[var4 * 3 + 2]]) {
               var5++;
            }
         }

         return Math.max(0.05F, 1.0F - (float)var5 / var0.triCount);
      } else {
         return 1.0F;
      }
   }

   private static void budget(List<Part> var0, Mesh[] var1, float[] var2, Detail var3, int[] var4, float[] var5) {
      int var6 = var0.size();
      float var7 = 0.0F;
      float var8 = 0.0F;
      float[] var9 = new float[var6];

      for (int var10 = 0; var10 < var6; var10++) {
         Part var11 = (Part)var0.get(var10);
         if (var1[var10] != null) {
            var9[var10] = var1[var10].triCount * var2[var10] * var11.detail;
            var7 += var9[var10];
            if (var5[var10] > 0.0F) {
               var8 += var9[var10];
            }
         }
      }

      if (!(var7 <= 0.0F)) {
         float var14 = var3.budget * 0.9F;
         float var15 = var14 / (1.0F + var8 / var7);

         for (int var12 = 0; var12 < var6; var12++) {
            if (var1[var12] != null) {
               int var13 = Math.max(24, Math.round(var15 * var9[var12] / var7 / var2[var12]));
               var4[var12] = Math.min(var13, var1[var12].triCount);
            }
         }
      }
   }

   private static CharacterBuilder.Shaded shade(Shape var0, Shape var1, Mesh var2, float var3, float var4, float var5) {
      CharacterBuilder.Shaded var6 = new CharacterBuilder.Shaded(var2.vertexCount);
      int var7 = (var2.vertexCount + 255) / 256;
      IntStream.range(0, var7).parallel().forEach(var7x -> {
         float[] var8 = new float[3];
         float[] var9 = new float[3];
         Sample var10 = new Sample();
         int var11 = Math.min(var2.vertexCount, (var7x + 1) * 256);

         for (int var12 = var7x * 256; var12 < var11; var12++) {
            var8[0] = var2.pos[var12 * 3];
            var8[1] = var2.pos[var12 * 3 + 1];
            var8[2] = var2.pos[var12 * 3 + 2];
            project(var0, var8, var9, var3);
            System.arraycopy(var8, 0, var6.pos, var12 * 3, 3);
            System.arraycopy(var9, 0, var6.nrm, var12 * 3, 3);
            var0.sample(var8[0], var8[1], var8[2], var10);
            float var13 = occlusion(var1, var8[0], var8[1], var8[2], var9[0], var9[1], var9[2], var10.occlusion(var4));
            var13 += (1.0F - var13) * Math.min(1.0F, var10.glow);
            var6.ao[var12] = var13;
            var6.flat[var12] = var10.flat(var5);
            var6.glow[var12] = (byte)(var10.glow >= 0.5F ? 1 : 0);
            var6.color[var12] = 0xFF000000 | Sample.channel(var10.r * var13) << 16 | Sample.channel(var10.g * var13) << 8 | Sample.channel(var10.b * var13);
         }
      });
      return var6;
   }

   private static Mesh bury(CharacterBuilder.Shaded var0, Mesh var1, Shape var2, float var3) {
      if (var2 == null) {
         return var1;
      } else {
         float[] var4 = new float[var0.count];
         int var5 = (var0.count + 255) / 256;
         IntStream.range(0, var5).parallel().forEach(var4x -> {
            float[] var5x = new float[3];
            float[] var6x = new float[3];
            int var7x = Math.min(var0.count, (var4x + 1) * 256);

            for (int var8x = var4x * 256; var8x < var7x; var8x++) {
               var5x[0] = var0.pos[var8x * 3];
               var5x[1] = var0.pos[var8x * 3 + 1];
               var5x[2] = var0.pos[var8x * 3 + 2];
               float var9;
               var4[var8x] = var9 = var2.distance(var5x[0], var5x[1], var5x[2]);
               if (var9 < 0.0F) {
                  gradient(var2, var5x, Math.max(0.01F, var3 * 0.3F), var6x);
                  float var10 = Math.min(0.12F, -2.0F * var9);
                  var0.pos[var8x * 3] = var5x[0] - var6x[0] * var10;
                  var0.pos[var8x * 3 + 1] = var5x[1] - var6x[1] * var10;
                  var0.pos[var8x * 3 + 2] = var5x[2] - var6x[2] * var10;
               }
            }
         });
         var0.cover = var4;
         int[] var6 = new int[var1.triCount * 3];
         int var7 = 0;

         for (int var8 = 0; var8 < var1.triCount; var8++) {
            int var12 = var1.tris[var8 * 3];
            int var13 = var1.tris[var8 * 3 + 1];
            int var14 = var1.tris[var8 * 3 + 2];
            if (!(var4[var12] < -0.02F)
               || !(var4[var13] < -0.02F)
               || !(var4[var14] < -0.02F)
               || !(
                  var2.distance(
                        (var1.pos[var12 * 3] + var1.pos[var13 * 3] + var1.pos[var14 * 3]) / 3.0F,
                        (var1.pos[var12 * 3 + 1] + var1.pos[var13 * 3 + 1] + var1.pos[var14 * 3 + 1]) / 3.0F,
                        (var1.pos[var12 * 3 + 2] + var1.pos[var13 * 3 + 2] + var1.pos[var14 * 3 + 2]) / 3.0F
                     )
                     < -0.02F
               )) {
               var6[var7 * 3] = var12;
               var6[var7 * 3 + 1] = var13;
               var6[var7 * 3 + 2] = var14;
               var7++;
            }
         }

         return var7 == var1.triCount ? var1 : new Mesh(var1.pos, var1.vertexCount, Arrays.copyOf(var6, var7 * 3), var7);
      }
   }

   static void project(Shape var0, float[] var1, float[] var2, float var3) {
      project(var0, var1, var2, var3, 0.0F);
   }

   static void project(Shape var0, float[] var1, float[] var2, float var3, float var4) {
      float var5 = Math.max(0.01F, var3 * 0.3F);
      float var6 = var3 * 0.01F;
      float var7 = var0.distance(var1[0], var1[1], var1[2]) - var4;
      gradient(var0, var1, var5, var2);

      for (int var8 = 0; var8 < 2 && Math.abs(var7) > var6; var8++) {
         float var9 = Math.max(-var3, Math.min(var3, var7));
         var1[0] -= var2[0] * var9;
         var1[1] -= var2[1] * var9;
         var1[2] -= var2[2] * var9;
         var7 = var0.distance(var1[0], var1[1], var1[2]) - var4;
         gradient(var0, var1, var5, var2);
      }
   }

   static void gradient(Shape var0, float[] var1, float var2, float[] var3) {
      float var6 = var1[0];
      float var7 = var1[1];
      float var8 = var1[2];
      float var9 = var0.distance(var6 + var2, var7, var8) - var0.distance(var6 - var2, var7, var8);
      float var4;
      float var5;
      float var10 = (float)Math.sqrt(
         var9 * var9
            + (var5 = var0.distance(var6, var7 + var2, var8) - var0.distance(var6, var7 - var2, var8)) * var5
            + (var4 = var0.distance(var6, var7, var8 + var2) - var0.distance(var6, var7, var8 - var2)) * var4
      );
      if (var10 < 1.0E-12F) {
         var3[0] = 0.0F;
         var3[1] = 1.0F;
         var3[2] = 0.0F;
      } else {
         var3[0] = var9 / var10;
         var3[1] = var5 / var10;
         var3[2] = var4 / var10;
      }
   }

   private static float occlusion(Shape var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      if (var0 != null && !(var7 <= 0.0F)) {
         float var8 = 0.0F;
         float var9 = 0.0F;
         float var10 = 1.0F;

         for (int var11 = 1; var11 <= 5; var11++) {
            float var12 = 0.38F * var11;
            float var13 = var0.distance(var1 + var4 * var12, var2 + var5 * var12, var3 + var6 * var12);
            var8 += Math.max(0.0F, var12 - var13) * var10;
            var9 += var12 * var10;
            var10 *= 0.72F;
         }

         float var14 = Math.min(1.0F, var8 / var9 * 1.6F);
         return 1.0F - var7 * var14 * (1.4F - 0.4F * var14);
      } else {
         return 1.0F;
      }
   }

   private static Surface baseSurface(CharacterBuilder.Shaded var0, Mesh var1, Part var2, Atlas var3) {
      if (var1.triCount == 0) {
         return null;
      } else {
         Surface.Builder var4 = new Surface.Builder();
         float var5 = var3.whiteU();
         float var6 = var3.whiteV();

         for (int var7 = 0; var7 < var0.count; var7++) {
            var4.vertex(
               var0.pos[var7 * 3] - var2.pivotX,
               var0.pos[var7 * 3 + 1] - var2.pivotY,
               var0.pos[var7 * 3 + 2] - var2.pivotZ,
               var0.nrm[var7 * 3],
               var0.nrm[var7 * 3 + 1],
               var0.nrm[var7 * 3 + 2],
               var5,
               var6,
               var0.color[var7],
               var0.glow[var7] != 0,
               var0.flat[var7]
            );
         }

         pair(var1.tris, var1.triCount, var1.vertexCount, var4);
         return var4.build();
      }
   }

   private static Surface hullSurface(Shape var0, CharacterBuilder.Shaded var1, Surface var2, Part var3, float var4, float var5, int var6, Atlas var7) {
      float[] var8 = new float[var1.count * 3];
      float[] var9 = new float[var1.count];
      float var10 = var4 / 0.5F * 1.5F;
      int var11 = (var1.count + 255) / 256;
      IntStream.range(0, var11).parallel().forEach(var7x -> {
         float[] var8x = new float[3];
         float[] var9x = new float[3];
         int var10x = Math.min(var1.count, (var7x + 1) * 256);

         for (int var11x = var7x * 256; var11x < var10x; var11x++) {
            float var13x = var1.pos[var11x * 3];
            float var14x = var1.pos[var11x * 3 + 1];
            float var15x = var1.pos[var11x * 3 + 2];
            float var16x = var1.nrm[var11x * 3];
            float var17x = var1.nrm[var11x * 3 + 1];
            float var18x = var1.nrm[var11x * 3 + 2];
            float var12x = var1.cover != null && var1.cover[var11x] < 0.0F ? 0.0F : var4;
            if (var12x > 0.0F) {
               float var20x = 0.0F;

               for (int var21x = 1; var21x <= 5; var21x++) {
                  float var22 = var10 * var21x / 5.0F;
                  var20x = Math.max(var20x, -var0.distance(var13x - var16x * var22, var14x - var17x * var22, var15x - var18x * var22));
               }

               var12x = Math.min(var12x, 0.5F * var20x);
            }

            var8x[0] = var13x + var16x * var12x;
            var8x[1] = var14x + var17x * var12x;
            var8x[2] = var15x + var18x * var12x;
            if (var12x > 0.0F) {
               project(var0, var8x, var9x, var5, var12x);
            }

            var8[var11x * 3] = var8x[0];
            var8[var11x * 3 + 1] = var8x[1];
            var8[var11x * 3 + 2] = var8x[2];
            var9[var11x] = var12x;
         }
      });
      Surface.Builder var12 = new Surface.Builder();
      float var13 = var7.whiteU();
      float var14 = var7.whiteV();

      for (int var15 = 0; var15 < var1.count; var15++) {
         var12.vertex(
            var8[var15 * 3] - var3.pivotX,
            var8[var15 * 3 + 1] - var3.pivotY,
            var8[var15 * 3 + 2] - var3.pivotZ,
            0.0F,
            1.0F,
            0.0F,
            var13,
            var14,
            var6 | 0xFF000000,
            false
         );
      }

      int[] var21 = var2.quads;

      for (byte var16 = 0; var16 < var21.length; var16 += 4) {
         int var17 = var21[var16];
         int var18 = var21[var16 + 1];
         int var19 = var21[var16 + 2];
         int var20 = var21[var16 + 3];
         if (!(var9[var17] <= 0.0F) || !(var9[var18] <= 0.0F) || !(var9[var19] <= 0.0F) || !(var9[var20] <= 0.0F)) {
            var12.quad(var17, var20, var19, var18, var19 == var20 ? 1 : 2);
         }
      }

      return var12.build();
   }

   static void pair(int[] var0, int var1, int var2, Surface.Builder var3) {
      long[] var4 = new long[var1 * 3];

      for (int var5 = 0; var5 < var1; var5++) {
         for (int var6 = 0; var6 < 3; var6++) {
            int var7 = var0[var5 * 3 + var6];
            int var8 = var0[var5 * 3 + (var6 + 1) % 3];
            long var9 = (long)Math.min(var7, var8) * var2 + Math.max(var7, var8);
            var4[var5 * 3 + var6] = var9 << 21 | var5 * 3L + var6;
         }
      }

      Arrays.sort(var4);
      int[] var21 = new int[var1 * 3];
      Arrays.fill(var21, -1);
      long var22 = 2097151L;

      for (int var23 = 0; var23 + 1 < var4.length; var23++) {
         if (var4[var23] >>> 21 == var4[var23 + 1] >>> 21) {
            if (var23 + 2 < var4.length && var4[var23 + 2] >>> 21 == var4[var23] >>> 21) {
               while (var23 + 1 < var4.length && var4[var23 + 1] >>> 21 == var4[var23] >>> 21) {
                  var23++;
               }
            } else {
               int var10 = (int)(var4[var23] & var22);
               int var24;
               var21[var10] = var24 = (int)(var4[var23 + 1] & var22);
               var21[var24] = var10;
               var23++;
            }
         }
      }

      boolean[] var25 = new boolean[var1];

      for (int var26 = 0; var26 < var1; var26++) {
         if (!var25[var26]) {
            int var11 = -1;

            for (int var12 = 0; var12 < 3 && var11 < 0; var12++) {
               int var13 = var21[var26 * 3 + var12];
               if (var13 >= 0 && !var25[var13 / 3] && var13 / 3 != var26) {
                  var11 = var12;
               }
            }

            int var27 = var0[var26 * 3];
            int var28 = var0[var26 * 3 + 1];
            int var14 = var0[var26 * 3 + 2];
            if (var11 < 0) {
               var25[var26] = true;
               var3.quad(var27, var28, var14, var14, 1);
            } else {
               int var15 = var21[var26 * 3 + var11];
               int var16 = var15 / 3;
               var25[var26] = true;
               var25[var16] = true;
               int var17 = var0[var26 * 3 + var11];
               int var18 = var0[var26 * 3 + (var11 + 1) % 3];
               int var19 = var0[var26 * 3 + (var11 + 2) % 3];
               int var20 = var0[var16 * 3 + (var15 % 3 + 2) % 3];
               var3.quad(var17, var20, var18, var19, 2);
            }
         }
      }
   }

   private static Surface decalSurface(Part var0, CharacterBuilder.Shaded var1, Mesh var2, Atlas var3) {
      Surface.Builder var4 = new Surface.Builder();
      float[] var5 = new float[200];
      float[] var6 = new float[200];

      for (Decal var8 : var0.decals) {
         for (int var9 = 0; var9 < var2.triCount; var9++) {
            int var10 = var2.tris[var9 * 3];
            int var11 = var2.tris[var9 * 3 + 1];
            int var12 = var2.tris[var9 * 3 + 2];
            if (faces(var8, var1, var10, var11, var12)) {
               int[] var13 = new int[]{var10, var11, var12};
               float var14 = 0.0F;

               for (int var15 = 0; var15 < 3; var15++) {
                  int var16 = var13[var15];
                  int var17 = var15 * 10;
                  var5[var17] = var1.pos[var16 * 3];
                  var5[var17 + 1] = var1.pos[var16 * 3 + 1];
                  var5[var17 + 2] = var1.pos[var16 * 3 + 2];
                  var5[var17 + 3] = var1.nrm[var16 * 3];
                  var5[var17 + 4] = var1.nrm[var16 * 3 + 1];
                  var5[var17 + 5] = var1.nrm[var16 * 3 + 2];
                  var5[var17 + 8] = var1.ao[var16];
                  var5[var17 + 9] = var1.flat[var16];
                  float var18 = var5[var17] - var8.cx;
                  float var19 = var5[var17 + 1] - var8.cy;
                  float var20 = var5[var17 + 2] - var8.cz;
                  if (!var8.wrap) {
                     var5[var17 + 6] = 0.5F + (var18 * var8.rx + var19 * var8.ry + var20 * var8.rz) / var8.width;
                     var5[var17 + 7] = 0.5F - (var18 * var8.ux + var19 * var8.uy + var20 * var8.uz) / var8.height;
                  } else {
                     float var21 = var18 * var8.dx + var19 * var8.dy + var20 * var8.dz;
                     float var22 = var18 - var21 * var8.dx;
                     float var23 = var19 - var21 * var8.dy;
                     float var24 = var20 - var21 * var8.dz;
                     float var25 = (float)Math.toDegrees(
                        Math.atan2(var22 * var8.rx + var23 * var8.ry + var24 * var8.rz, var22 * var8.ux + var23 * var8.uy + var24 * var8.uz)
                     );
                     if (var15 == 0) {
                        var14 = var25;
                     } else {
                        float var26 = var25 - var14;

                        while (var26 > 180.0F) {
                           var26 -= 360.0F;
                        }

                        while (var26 < -180.0F) {
                           var26 += 360.0F;
                        }

                        var25 = var14 + var26;
                     }

                     var5[var17 + 6] = 0.5F + var25 / var8.width;
                     var5[var17 + 7] = 0.5F - var21 / var8.height;
                  }
               }

               emitClipped(var8, var5, var6, 0.0F, var0, var3, var4);
               if (var8.wrap && var8.width >= 359.9F) {
                  emitClipped(var8, var5, var6, 1.0F, var0, var3, var4);
                  emitClipped(var8, var5, var6, -1.0F, var0, var3, var4);
               }
            }
         }
      }

      return var4.build();
   }

   private static boolean faces(Decal var0, CharacterBuilder.Shaded var1, int var2, int var3, int var4) {
      float var5 = var1.pos[var2 * 3 + 1];
      float var6 = var1.pos[var3 * 3 + 1] - var5;
      float var7 = var1.pos[var2 * 3 + 2];
      float var8 = var1.pos[var4 * 3 + 2] - var7;
      float var9 = var1.pos[var3 * 3 + 2] - var7;
      float var10 = var1.pos[var4 * 3 + 1] - var5;
      float var11 = var6 * var8 - var9 * var10;
      float var12 = var1.pos[var2 * 3];
      float var13 = var1.pos[var4 * 3] - var12;
      float var14 = var1.pos[var3 * 3] - var12;
      float var15 = var9 * var13 - var14 * var8;
      float var16 = var14 * var10 - var6 * var13;
      float var17 = (float)Math.sqrt(var11 * var11 + var15 * var15 + var16 * var16);
      if (var17 < 1.0E-10F) {
         return false;
      } else {
         var11 /= var17;
         var15 /= var17;
         var16 /= var17;
         int[] var18 = new int[]{var2, var3, var4};
         if (!var0.wrap) {
            if (var11 * var0.dx + var15 * var0.dy + var16 * var0.dz < var0.cosLimit) {
               return false;
            } else {
               for (int var34 : var18) {
                  float var35 = (var1.pos[var34 * 3] - var0.cx) * var0.dx
                     + (var1.pos[var34 * 3 + 1] - var0.cy) * var0.dy
                     + (var1.pos[var34 * 3 + 2] - var0.cz) * var0.dz;
                  if (var35 < var0.near || var35 > var0.far) {
                     return false;
                  }
               }

               return true;
            }
         } else {
            float var19 = (var1.pos[var2 * 3] + var1.pos[var3 * 3] + var1.pos[var4 * 3]) / 3.0F - var0.cx;
            float var20 = (var1.pos[var2 * 3 + 1] + var1.pos[var3 * 3 + 1] + var1.pos[var4 * 3 + 1]) / 3.0F - var0.cy;
            float var21 = (var1.pos[var2 * 3 + 2] + var1.pos[var3 * 3 + 2] + var1.pos[var4 * 3 + 2]) / 3.0F - var0.cz;
            float var22 = var19 * var0.dx + var20 * var0.dy + var21 * var0.dz;
            float var23 = var19 - var22 * var0.dx;
            float var24 = var20 - var22 * var0.dy;
            float var25 = var21 - var22 * var0.dz;
            float var26 = (float)Math.sqrt(var23 * var23 + var24 * var24 + var25 * var25);
            if (var26 < 1.0E-5F) {
               return false;
            } else if ((var11 * var23 + var15 * var24 + var16 * var25) / var26 < var0.cosLimit) {
               return false;
            } else {
               float var27 = var0.near == -3.4028235E38F ? 0.0F : var0.near;
               return var26 >= var27 && var26 <= var0.far;
            }
         }
      }
   }

   private static void emitClipped(Decal var0, float[] var1, float[] var2, float var3, Part var4, Atlas var5, Surface.Builder var6) {
      float[] var7 = new float[200];
      System.arraycopy(var1, 0, var7, 0, 30);
      if (var3 != 0.0F) {
         for (int var8 = 0; var8 < 3; var8++) {
            int var9 = var8 * 10 + 6;
            var7[var9] += var3;
         }
      }

      int var20 = 3;
      var20 = clip(var7, var20, var2, 6, 0.0F, true);
      var20 = clip(var2, var20, var7, 6, 1.0F, false);
      var20 = clip(var7, var20, var2, 7, 0.0F, true);
      if ((var20 = clip(var2, var20, var7, 7, 1.0F, false)) >= 3) {
         int var25 = -1;

         for (int var10 = 0; var10 < var20; var10++) {
            int var11 = var10 * 10;
            float var12 = var7[var11 + 3];
            float var13 = var7[var11 + 4];
            float var14 = var7[var11 + 5];
            float var15 = (float)Math.sqrt(var12 * var12 + var13 * var13 + var14 * var14);
            if (var15 > 1.0E-8F) {
               var12 /= var15;
               var13 /= var15;
               var14 /= var15;
            }

            int var16 = Sample.channel(1.0F - var0.occlusion * (1.0F - var7[var11 + 8]));
            int var17 = 0xFF000000 | var16 << 16 | var16 << 8 | var16;
            float var18 = var0.flat == var0.flat ? var0.flat : var7[var11 + 9];
            int var19 = var6.vertex(
               var7[var11] + var12 * var0.offset - var4.pivotX,
               var7[var11 + 1] + var13 * var0.offset - var4.pivotY,
               var7[var11 + 2] + var14 * var0.offset - var4.pivotZ,
               var12,
               var13,
               var14,
               var5.u(var0.canvas, var7[var11 + 6]),
               var5.v(var0.canvas, var7[var11 + 7]),
               var17,
               var0.glow,
               var18
            );
            if (var25 < 0) {
               var25 = var19;
            }
         }

         byte var26;
         for (var26 = 1; var26 + 2 < var20; var26 += 2) {
            var6.quad(var25, var25 + var26, var25 + var26 + 1, var25 + var26 + 2, 2);
         }

         if (var26 + 1 < var20) {
            var6.quad(var25, var25 + var26, var25 + var26 + 1, var25 + var26 + 1, 1);
         }
      }
   }

   private static int clip(float[] var0, int var1, float[] var2, int var3, float var4, boolean var5) {
      int var6 = 0;

      for (int var7 = 0; var7 < var1; var7++) {
         int var9 = var7 * 10;
         int var10 = (var7 + var1 - 1) % var1 * 10;
         float var11 = var5 ? var0[var9 + var3] - var4 : var4 - var0[var9 + var3];
         float var8 = var5 ? var0[var10 + var3] - var4 : var4 - var0[var10 + var3];
         if (var11 >= 0.0F) {
            if (var8 < 0.0F) {
               var6 = cut(var0, var10, var9, var8, var11, var2, var6);
            }

            System.arraycopy(var0, var9, var2, var6 * 10, 10);
            var6++;
         } else if (var8 >= 0.0F) {
            var6 = cut(var0, var10, var9, var8, var11, var2, var6);
         }

         if (var6 >= 19) {
            break;
         }
      }

      return var6;
   }

   private static int cut(float[] var0, int var1, int var2, float var3, float var4, float[] var5, int var6) {
      float var7 = var3 / (var3 - var4);

      for (int var8 = 0; var8 < 10; var8++) {
         var5[var6 * 10 + var8] = var0[var1 + var8] + (var0[var2 + var8] - var0[var1 + var8]) * var7;
      }

      return var6 + 1;
   }

   private static float reach(Surface var0) {
      if (var0 == null) {
         return 1.0F;
      } else {
         float var1 = 0.0F;

         for (int var2 = 0; var2 < var0.vertexCount(); var2++) {
            float var3 = var0.pos[var2 * 3];
            float var4 = var0.pos[var2 * 3 + 1];
            float var5 = var0.pos[var2 * 3 + 2];
            var1 = Math.max(var1, var3 * var3 + var4 * var4 + var5 * var5);
         }

         return Math.max(0.5F, (float)Math.sqrt(var1));
      }
   }

   private static void jointWeights(Surface var0, Part var1, Part var2) {
      if (var0 != null) {
         float[] var3 = new float[var0.vertexCount()];
         float var4 = var2.jointX - var1.pivotX;
         float var5 = var2.jointY - var1.pivotY;
         float var6 = var2.jointZ - var1.pivotZ;
         float var7 = var2.jointBlend;
         boolean var8 = false;

         for (int var9 = 0; var9 < var3.length; var9++) {
            float var10 = (var0.pos[var9 * 3] - var4) * var2.jointDirX
               + (var0.pos[var9 * 3 + 1] - var5) * var2.jointDirY
               + (var0.pos[var9 * 3 + 2] - var6) * var2.jointDirZ;
            float var11 = Math.max(0.0F, Math.min(1.0F, (var10 + var7) / (2.0F * var7)));
            var3[var9] = var11 * var11 * (3.0F - 2.0F * var11);
            var8 |= var3[var9] > 0.0F;
         }

         if (var8) {
            var0.joint = var3;
         }
      }
   }

   private static void weigh(Surface var0, float var1, float var2) {
      if (var0 != null) {
         float[] var3 = new float[var0.vertexCount()];

         for (int var4 = 0; var4 < var3.length; var4++) {
            float var5 = var0.pos[var4 * 3];
            float var6 = var0.pos[var4 * 3 + 1];
            float var7 = var0.pos[var4 * 3 + 2];
            float var8 = Math.min(1.0F, (float)Math.sqrt(var5 * var5 + var6 * var6 + var7 * var7) / var1);
            var3[var4] = (float)Math.pow(var8, var2);
         }

         var0.weight = var3;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Shaded {
      final int count;
      final float[] pos;
      final float[] nrm;
      final int[] color;
      final byte[] glow;
      final float[] ao;
      final float[] flat;
      float[] cover;

      Shaded(int var1) {
         this.count = var1;
         this.pos = new float[var1 * 3];
         this.nrm = new float[var1 * 3];
         this.color = new int[var1];
         this.glow = new byte[var1];
         this.ao = new float[var1];
         this.flat = new float[var1];
      }
   }
}
