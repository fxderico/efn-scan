package net.efn.client.util.seedscan;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class WgRandom {
   private long lo;
   private long hi;

   WgRandom() {
   }

   private WgRandom(long l, long m) {
      this.lo = l;
      this.hi = m;
   }

   WgRandom copy() {
      return new WgRandom(this.lo, this.hi);
   }

   boolean sameState(WgRandom wgRandom) {
      return this.lo == wgRandom.lo && this.hi == wgRandom.hi;
   }

   long stateHash() {
      return this.lo * 31L + this.hi;
   }

   private static long mix(long l) {
      l = (l ^ l >>> 30) * -4658895280553007687L;
      l = (l ^ l >>> 27) * -7723592293110705685L;
      return l ^ l >>> 31;
   }

   void setSeed(long l) {
      long var3 = l ^ 7640891576956012809L;
      long var5 = var3 + -7046029254386353131L;
      this.lo = mix(var3);
      this.hi = mix(var5);
      if ((this.lo | this.hi) == 0L) {
         this.lo = -7046029254386353131L;
         this.hi = 7640891576956012809L;
      }
   }

   private long step() {
      long var1 = this.lo;
      long var3 = this.hi;
      long var5 = Long.rotateLeft(var1 + var3, 17) + var1;
      long var7;
      this.lo = Long.rotateLeft(var1, 49) ^ (var7 = var3 ^ var1) ^ var7 << 21;
      this.hi = Long.rotateLeft(var7, 28);
      return var5;
   }

   void skip(int i) {
      for (int var2 = 0; var2 < i; var2++) {
         this.step();
      }
   }

   int next(int i) {
      return (int)(this.step() >>> 64 - i);
   }

   int nextInt(int i) {
      if ((i & i - 1) == 0) {
         return (int)((long)i * this.next(31) >> 31);
      } else {
         int var2;
         int var3;
         while ((var3 = this.next(31)) - (var2 = var3 % i) + (i - 1) < 0) {
         }

         return var2;
      }
   }

   int between(int i, int j) {
      return this.nextInt(j - i + 1) + i;
   }

   long nextLong() {
      int var1 = this.next(32);
      int var2 = this.next(32);
      return ((long)var1 << 32) + var2;
   }

   float nextFloat() {
      return this.next(24) * 5.9604645E-8F;
   }

   double nextDouble() {
      int var1 = this.next(26);
      int var2 = this.next(27);
      return (((long)var1 << 27) + var2) * 1.1102230246251565E-16;
   }

   long setDecorationSeed(long l, int i, int j) {
      this.setSeed(l);
      long var5 = this.nextLong() | 1L;
      long var7 = this.nextLong() | 1L;
      long var9 = i * var5 + j * var7 ^ l;
      this.setSeed(var9);
      return var9;
   }

   void setFeatureSeed(long l, int i, int j) {
      this.setSeed(l + i + 10000L * j);
   }
}
