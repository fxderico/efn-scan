package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
interface SkyPaint {
   float HORIZON = 0.72F;
   float RADIANS_PER_UNIT = 1.0471976F;

   SkyImage upload(String string, int i, int j, int[] is);

   SkyImage upload(String string, int i, int j, int[][] is);

   void gradient(boolean bl, float f, float g, float h, float i, int j, int k);

   void band(SkyImage skyImage, float f, float g, float h, float i, int j, boolean bl, int k);

   void sprite(SkyImage skyImage, float f, float g, float h, float i, float j, int k, boolean bl);

   void glow(float f, float g, float h, int i, float j);
}
