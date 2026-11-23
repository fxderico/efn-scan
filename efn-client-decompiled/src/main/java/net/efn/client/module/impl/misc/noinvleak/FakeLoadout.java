package net.efn.client.module.impl.misc.noinvleak;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@Environment(EnvType.CLIENT)
public final class FakeLoadout {
   public static final int UNKNOWN = 41;
   private static final Item[] HOTBAR = new Item[]{
      Items.field_8371,
      Items.field_8802,
      Items.field_22022,
      Items.field_8403,
      Items.field_8377,
      Items.field_22024,
      Items.field_8475,
      Items.field_8556,
      Items.field_22025,
      Items.field_8699,
      Items.field_8250,
      Items.field_22023,
      Items.field_8102,
      Items.field_8705,
      Items.field_20412,
      Items.field_8810,
      Items.field_8176,
      Items.field_8071,
      Items.field_8229,
      Items.field_8634,
      Items.field_8118,
      Items.field_8831,
      Items.field_8884
   };
   private static final Item[] MAIN = new Item[]{
      Items.field_20412,
      Items.field_20391,
      Items.field_8831,
      Items.field_8583,
      Items.field_8118,
      Items.field_8600,
      Items.field_8713,
      Items.field_8620,
      Items.field_8695,
      Items.field_8477,
      Items.field_8725,
      Items.field_8759,
      Items.field_8107,
      Items.field_8276,
      Items.field_8606,
      Items.field_8511,
      Items.field_8054,
      Items.field_8858,
      Items.field_8110,
      Items.field_8317,
      Items.field_8861,
      Items.field_8121,
      Items.field_8281,
      Items.field_8280,
      Items.field_8745,
      Items.field_8153,
      Items.field_8145,
      Items.field_8680,
      Items.field_8279,
      Items.field_8261,
      Items.field_8550,
      Items.field_8529,
      Items.field_8407,
      Items.field_17531,
      Items.field_8465,
      Items.field_8106,
      Items.field_8732
   };
   private static final Item[] OFFHAND = new Item[]{Items.field_8255, Items.field_8810, Items.field_8705, Items.field_8107, Items.field_8176, Items.field_8463};
   private static final Item[][] ARMOR = new Item[][]{
      {Items.field_8660, Items.field_8313, Items.field_8285, Items.field_22030},
      {Items.field_8396, Items.field_8218, Items.field_8348, Items.field_22029},
      {Items.field_8523, Items.field_8873, Items.field_8058, Items.field_22028},
      {Items.field_8743, Items.field_8283, Items.field_8805, Items.field_22027}
   };
   private static final int FIRST_ARMOR = 36;
   private final ItemStack[] stacks = new ItemStack[42];
   private long seed;

   public FakeLoadout() {
   }

   public void reseed() {
      this.seed = ThreadLocalRandom.current().nextLong();
      Arrays.fill(this.stacks, null);
   }

   public ItemStack get(int var1) {
      if (var1 < 0 || var1 > 41) {
         var1 = 41;
      }

      ItemStack var2;
      if ((var2 = this.stacks[var1]) == null) {
         this.stacks[var1] = var2 = this.roll(var1);
      }

      return var2;
   }

   private ItemStack roll(int var1) {
      Item var2 = var1 < 9
         ? this.pick(HOTBAR, var1)
         : (
            var1 < 36 || var1 == 41
               ? this.pick(MAIN, var1)
               : (var1 == 40 ? this.pick(OFFHAND, var1) : ARMOR[var1 - 36][(int)Long.remainderUnsigned(mix(this.seed ^ 167L), ARMOR[0].length)])
         );
      ItemStack var3 = new ItemStack(var2);
      if (var3.method_7914() > 1) {
         var3.method_7939(1 + (int)Long.remainderUnsigned(mix(this.seed ^ var1 ^ 81L), var3.method_7914()));
      } else if (var3.method_7963()) {
         var3.method_7974((int)Long.remainderUnsigned(mix(this.seed ^ var1 ^ 82L), var3.method_7936() / 2));
      }

      return var3;
   }

   private Item pick(Item[] var1, int var2) {
      return var1[(int)Long.remainderUnsigned(mix(this.seed ^ var2 * -7046029254386353131L), var1.length)];
   }

   private static long mix(long var0) {
      var0 = (var0 ^ var0 >>> 30) * -4658895280553007687L;
      var0 = (var0 ^ var0 >>> 27) * -7723592293110705685L;
      return var0 ^ var0 >>> 31;
   }
}
