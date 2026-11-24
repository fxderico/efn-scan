package net.efn.client.module.impl.render.character.kit;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.zip.InflaterInputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class BakedModel {
   public static final int MAGIC = 1095648072;
   public static final int VERSION = 3;
   private static final int SHIPPED_VERSION = 4;

   private BakedModel() {
   }

   static CharacterModel load(BakedDesign bakedDesign, Detail detail) {
      long var3 = System.nanoTime();

      try {
         CharacterModel var2;
         try (InputStream var5 = bakedDesign.open()) {
            DataInputStream var7 = new DataInputStream(new BufferedInputStream(var5));
            int var6 = var7.readInt() == 1095648072 ? var7.readInt() : -1;
            if (var6 != 3 && var6 != 4) {
               throw new IllegalStateException(bakedDesign.id() + ": not a version 3 baked character");
            }

            int var9 = 0;
            int var10 = 0;
            int[] var11 = null;
            if (var6 == 4) {
               var9 = var7.readInt();
               var10 = var7.readInt();
               byte[] var12 = new byte[var9 * var10 * 4];
               var7.readFully(var12);
               var11 = unfilter(var12, var9, var10);
            }

            float var44 = var7.readFloat();
            float var13 = var7.readFloat();
            float var14 = var7.readFloat();
            float var15 = var7.readFloat();
            boolean var16 = var7.readBoolean();
            boolean var17 = var7.readBoolean();
            int var18 = var7.readInt();
            int var19 = Math.min(detail.ordinal(), var18 - 1);
            PartMesh[] var20 = new PartMesh[var7.readInt()];

            for (int var21 = 0; var21 < var20.length; var21++) {
               Bone var22 = Bone.values()[var7.readByte()];
               float var23 = var7.readFloat();
               float var24 = var7.readFloat();
               float var25 = var7.readFloat();
               float var26 = var7.readFloat();
               float var27 = var7.readFloat();
               float var28 = var7.readFloat();
               float[] var29 = new float[6];

               for (int var30 = 0; var30 < 6; var30++) {
                  var29[var30] = var7.readFloat();
               }

               float var47 = var7.readFloat();
               float var31 = var7.readFloat();
               boolean var32 = var7.readBoolean();
               float var33 = 0.0F;
               float var34 = 0.0F;
               float var35 = 0.0F;
               if (var32) {
                  var33 = var7.readFloat() - var23;
                  var34 = var7.readFloat() - var24;
                  var35 = var7.readFloat() - var25;
               }

               Surface var36 = null;
               Surface var37 = null;

               for (int var38 = 0; var38 < var18; var38++) {
                  Surface var39 = surface(var7, var29, var23, var24, var25, var15, var17, var32, var38 == var19);
                  Surface var40 = surface(var7, var29, var23, var24, var25, var15, var17, var32, var38 == var19);
                  if (var38 == var19) {
                     var36 = var39;
                     var37 = var40;
                  }
               }

               var20[var21] = new PartMesh(
                  var22.name(),
                  var22,
                  -1,
                  false,
                  var23,
                  var24,
                  var25,
                  var26,
                  var27,
                  var28,
                  null,
                  var36,
                  null,
                  var37,
                  -1,
                  var47,
                  var31,
                  var32,
                  var33,
                  var34,
                  var35,
                  false,
                  0.0F,
                  0.0F,
                  0.0F
               );
            }

            if (var6 == 3) {
               var9 = var7.readInt();
               var10 = var7.readInt();
               byte[] var45 = new byte[var7.readInt()];
               var7.readFully(var45);
               var11 = pixels(var45, var9, var10);
            }

            long var46 = (System.nanoTime() - var3) / 1000000L;
            var2 = new CharacterModel(
               bakedDesign.id(),
               bakedDesign.name(),
               detail,
               1.0F,
               var20,
               0,
               var9,
               var10,
               Atlas.mips(var11, var9, var10),
               0.0F,
               0.0F,
               var44,
               var13,
               var14,
               var16,
               var46
            );
         }

         return var2;
      } catch (IOException var43) {
         throw new UncheckedIOException(bakedDesign.name() + ": " + var43.getMessage(), var43);
      }
   }

   private static int[] pixels(byte[] bs, int i, int j) throws IOException {
      byte[] var3 = new byte[i * j * 4];

      try (InflaterInputStream var4 = new InflaterInputStream(new ByteArrayInputStream(bs))) {
         int var5 = var4.readNBytes(var3, 0, var3.length);
         if (var5 != var3.length) {
            throw new IOException("atlas is truncated");
         }
      }

      return unfilter(var3, i, j);
   }

   private static int[] unfilter(byte[] bs, int i, int j) {
      int[] var3 = new int[i * j];

      for (int var4 = 0; var4 < var3.length; var4++) {
         int var5 = var4 % i == 0 ? 0 : var3[var4 - 1];
         int var6 = 0;

         for (int var7 = 0; var7 < 4; var7++) {
            int var8 = 24 - var7 * 8;
            var6 |= (bs[var4 * 4 + var7] + (var5 >>> var8) & 0xFF) << var8;
         }

         var3[var4] = var6;
      }

      return var3;
   }

   private static Surface surface(DataInputStream dataInputStream, float[] fs, float f, float g, float h, float i, boolean bl, boolean bl2, boolean bl3) throws IOException {
      int var9 = dataInputStream.readInt();
      if (!bl3) {
         dataInputStream.skipNBytes(var9 * (13L + (bl ? 3L : 0L) + (bl2 ? 1L : 0L)));
         dataInputStream.skipNBytes(dataInputStream.readInt() * (var9 > 65535 ? 12L : 6L));
         return null;
      } else {
         float[] var10 = new float[var9 * 3];

         for (int var11 = 0; var11 < var10.length; var11++) {
            int var12 = var11 % 3;
            var10[var11] = fs[var12] + dataInputStream.readChar() / 65535.0F * (fs[var12 + 3] - fs[var12]);
         }

         byte[] var23 = new byte[var9 * 3];
         dataInputStream.readFully(var23);
         float[] var24 = new float[var9 * 2];

         for (int var13 = 0; var13 < var24.length; var13++) {
            var24[var13] = dataInputStream.readChar() / 65535.0F;
         }

         int[] var25 = new int[var9];

         for (int var14 = 0; var14 < var9; var14++) {
            var25[var14] = bl
               ? 0xFF000000 | dataInputStream.readUnsignedByte() << 16 | dataInputStream.readUnsignedByte() << 8 | dataInputStream.readUnsignedByte()
               : -1;
         }

         float[] var26 = null;
         if (bl2) {
            var26 = new float[var9];

            for (int var15 = 0; var15 < var9; var15++) {
               var26[var15] = dataInputStream.readUnsignedByte() / 255.0F;
            }
         }

         int var27 = dataInputStream.readInt();
         int[] var16 = new int[var27 * 3];

         for (int var17 = 0; var17 < var16.length; var17++) {
            var16[var17] = var9 > 65535 ? dataInputStream.readInt() : dataInputStream.readChar();
         }

         if (var9 != 0 && var27 != 0) {
            Surface.Builder var28 = new Surface.Builder();

            for (int var18 = 0; var18 < var9; var18++) {
               float var19 = var23[var18 * 3] / 127.0F;
               float var20 = var23[var18 * 3 + 1] / 127.0F;
               float var21 = var23[var18 * 3 + 2] / 127.0F;
               float var22 = (float)Math.sqrt(var19 * var19 + var20 * var20 + var21 * var21);
               if (var22 > 1.0E-6F) {
                  var19 /= var22;
                  var20 /= var22;
                  var21 /= var22;
               }

               var28.vertex(
                  var10[var18 * 3] - f,
                  var10[var18 * 3 + 1] - g,
                  var10[var18 * 3 + 2] - h,
                  var19,
                  var20,
                  var21,
                  var24[var18 * 2],
                  var24[var18 * 2 + 1],
                  var25[var18],
                  false,
                  i
               );
            }

            CharacterBuilder.pair(var16, var27, var9, var28);
            Surface var29 = var28.build();
            if (var29 != null && var26 != null) {
               for (float var33 : var26) {
                  if (var33 > 0.0F) {
                     var29.joint = var26;
                     break;
                  }
               }
            }

            return var29;
         } else {
            return null;
         }
      }
   }
}
