package net.efn.client.util.seedscan;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class WgRandom {
   private long lo;
   private long hi;

   WgRandom() {
   }

   private WgRandom(long var1, long var3) {
      this.lo = var1;
      this.hi = var3;
   }

   WgRandom copy() {
      return new WgRandom(this.lo, this.hi);
   }

   boolean sameState(WgRandom var1) {
      return this.lo == var1.lo && this.hi == var1.hi;
   }

   long stateHash() {
      return this.lo * 31L + this.hi;
   }

   private static long mix(long var0) {
      var0 = (var0 ^ var0 >>> 30) * -4658895280553007687L;
      var0 = (var0 ^ var0 >>> 27) * -7723592293110705685L;
      return var0 ^ var0 >>> 31;
   }

   void setSeed(long var1) {
      long var3 = var1 ^ 7640891576956012809L;
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

   void skip(int var1) {
      for (int var2 = 0; var2 < var1; var2++) {
         this.step();
      }
   }

   int next(int var1) {
      return (int)(this.step() >>> 64 - var1);
   }

   int nextInt(int var1) {
      if ((var1 & var1 - 1) == 0) {
         return (int)((long)var1 * this.next(31) >> 31);
      } else {
         int var2;
         int var3;
         while ((var3 = this.next(31)) - (var2 = var3 % var1) + (var1 - 1) < 0) {
         }

         return var2;
      }
   }

   int between(int var1, int var2) {
      return this.nextInt(var2 - var1 + 1) + var1;
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

   long setDecorationSeed(long var1, int var3, int var4) {
      this.setSeed(var1);
      long var5 = this.nextLong() | 1L;
      long var7 = this.nextLong() | 1L;
      long var9 = var3 * var5 + var4 * var7 ^ var1;
      this.setSeed(var9);
      return var9;
   }

   void setFeatureSeed(long var1, int var3, int var4) {
      this.setSeed(var1 + var3 + 10000L * var4);
   }
}
