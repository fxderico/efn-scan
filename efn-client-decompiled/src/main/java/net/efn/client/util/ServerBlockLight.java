package net.efn.client.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.BitSet;
import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LightingProvider;

@Environment(EnvType.CLIENT)
public final class ServerBlockLight {
   public static final int UNKNOWN = -1;
   public static final int TOP_SECTION = 3;
   public static final int TOP_Y = 63;
   private static final byte[] DARK = new byte[0];
   private final Long2ObjectOpenHashMap<ServerBlockLight.Column> columns = new Long2ObjectOpenHashMap();

   public ServerBlockLight() {
   }

   public void onChunk(ClientWorld var1, int var2, int var3, LightData var4) {
      this.columns.put(ChunkPos.method_8331(var2, var3), apply(var1, null, var4));
   }

   public void onLight(ClientWorld var1, int var2, int var3, LightData var4) {
      long var5 = ChunkPos.method_8331(var2, var3);
      this.columns.put(var5, apply(var1, (ServerBlockLight.Column)this.columns.get(var5), var4));
   }

   public void onBlockChanged(BlockPos var1) {
      if (var1.method_10264() <= 63) {
         ServerBlockLight.Column var2 = (ServerBlockLight.Column)this.columns.get(ChunkPos.method_8331(var1.method_10263() >> 4, var1.method_10260() >> 4));
         if (var2 != null) {
            if (var2.touched == null) {
               var2.touched = new LongOpenHashSet();
            }

            var2.touched.add(var1.method_10063());
         }
      }
   }

   public ServerBlockLight.Column column(ClientWorld var1, int var2, int var3) {
      long var4 = ChunkPos.method_8331(var2, var3);
      ServerBlockLight.Column var6 = (ServerBlockLight.Column)this.columns.get(var4);
      if (var6 == null && LevelUtil.isLoaded(var1, var2, var3)) {
         var6 = fromClient(var1, var2, var3);
         this.columns.put(var4, var6);
      }

      return var6;
   }

   public void forgetUnloaded(ClientWorld var1) {
      this.columns.keySet().removeIf(var1x -> !LevelUtil.isLoaded(var1, ChunkPos.method_8325(var1x), ChunkPos.method_8332(var1x)));
   }

   public void clear() {
      this.columns.clear();
   }

   private static ServerBlockLight.Column apply(ClientWorld var0, ServerBlockLight.Column var1, LightData var2) {
      LightingProvider var3 = var0.method_22336();
      int var4 = var3.method_31929();
      int var5 = var3.method_31928();
      byte[][] var6 = var1 != null && var1.minSection == var4 && var1.sections.length == var5 ? (byte[][])var1.sections.clone() : new byte[var5][];
      BitSet var7 = var2.method_38608();
      BitSet var8 = var2.method_38609();
      Iterator var9 = var2.method_38610().iterator();

      for (int var10 = 0; var10 < var5; var10++) {
         boolean var11 = var4 + var10 <= 3;
         if (var7.get(var10)) {
            byte[] var13 = var9.hasNext() ? (byte[])var9.next() : null;
            var6[var10] = var11 && var13 != null && var13.length == 2048 ? (byte[])var13.clone() : null;
         } else if (var8.get(var10)) {
            var6[var10] = var11 ? DARK : null;
         }
      }

      return new ServerBlockLight.Column(var4, var6);
   }

   private static ServerBlockLight.Column fromClient(ClientWorld var0, int var1, int var2) {
      LightingProvider var3 = var0.method_22336();
      ChunkLightingView var4 = var3.method_15562(LightType.field_9282);
      int var5 = var3.method_31929();
      byte[][] var6 = new byte[var3.method_31928()][];

      for (int var7 = 0; var7 < var6.length && var5 + var7 <= 3; var7++) {
         ChunkNibbleArray var8 = var4.method_15544(ChunkSectionPos.method_18676(var1, var5 + var7, var2));
         if (var8 != null) {
            var6[var7] = var8.method_12146() ? DARK : (byte[])var8.method_12137().clone();
         }
      }

      return new ServerBlockLight.Column(var5, var6);
   }

   @Environment(EnvType.CLIENT)
   public static final class Column {
      private final int minSection;
      private final byte[][] sections;
      private LongOpenHashSet touched;

      private Column(int var1, byte[][] var2) {
         this.minSection = var1;
         this.sections = var2;
      }

      public int get(int var1, int var2, int var3) {
         int var4 = (var2 >> 4) - this.minSection;
         if (var4 >= 0 && var4 < this.sections.length) {
            byte[] var5 = this.sections[var4];
            if (var5 == null) {
               return -1;
            } else if (this.touched != null && this.touched.contains(BlockPos.method_10064(var1, var2, var3))) {
               return -1;
            } else if (var5 == ServerBlockLight.DARK) {
               return 0;
            } else {
               int var6 = (var2 & 15) << 8 | (var3 & 15) << 4 | var1 & 15;
               return var5[var6 >> 1] >> ((var6 & 1) << 2) & 15;
            }
         } else {
            return -1;
         }
      }
   }
}
