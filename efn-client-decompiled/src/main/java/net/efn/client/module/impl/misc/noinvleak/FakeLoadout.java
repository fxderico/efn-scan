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
      Items.IRON_SWORD,
      Items.DIAMOND_SWORD,
      Items.NETHERITE_SWORD,
      Items.IRON_PICKAXE,
      Items.DIAMOND_PICKAXE,
      Items.NETHERITE_PICKAXE,
      Items.IRON_AXE,
      Items.DIAMOND_AXE,
      Items.NETHERITE_AXE,
      Items.IRON_SHOVEL,
      Items.DIAMOND_SHOVEL,
      Items.NETHERITE_SHOVEL,
      Items.BOW,
      Items.WATER_BUCKET,
      Items.COBBLESTONE,
      Items.TORCH,
      Items.COOKED_BEEF,
      Items.GOLDEN_CARROT,
      Items.BREAD,
      Items.ENDER_PEARL,
      Items.OAK_PLANKS,
      Items.DIRT,
      Items.FLINT_AND_STEEL
   };
   private static final Item[] MAIN = new Item[]{
      Items.COBBLESTONE,
      Items.STONE,
      Items.DIRT,
      Items.OAK_LOG,
      Items.OAK_PLANKS,
      Items.STICK,
      Items.COAL,
      Items.IRON_INGOT,
      Items.GOLD_INGOT,
      Items.DIAMOND,
      Items.REDSTONE,
      Items.LAPIS_LAZULI,
      Items.ARROW,
      Items.STRING,
      Items.BONE,
      Items.ROTTEN_FLESH,
      Items.GUNPOWDER,
      Items.SAND,
      Items.GRAVEL,
      Items.WHEAT_SEEDS,
      Items.WHEAT,
      Items.LADDER,
      Items.OBSIDIAN,
      Items.GLASS,
      Items.LEATHER,
      Items.FEATHER,
      Items.FLINT,
      Items.SPIDER_EYE,
      Items.APPLE,
      Items.COOKED_PORKCHOP,
      Items.BUCKET,
      Items.BOOK,
      Items.PAPER,
      Items.SUGAR_CANE,
      Items.CRAFTING_TABLE,
      Items.CHEST,
      Items.FURNACE
   };
   private static final Item[] OFFHAND = new Item[]{Items.SHIELD, Items.TORCH, Items.WATER_BUCKET, Items.ARROW, Items.COOKED_BEEF, Items.GOLDEN_APPLE};
   private static final Item[][] ARMOR = new Item[][]{
      {Items.IRON_BOOTS, Items.CHAINMAIL_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS},
      {Items.IRON_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS},
      {Items.IRON_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE},
      {Items.IRON_HELMET, Items.CHAINMAIL_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET}
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

   public ItemStack get(int i) {
      if (i < 0 || i > 41) {
         i = 41;
      }

      ItemStack var2;
      if ((var2 = this.stacks[i]) == null) {
         this.stacks[i] = var2 = this.roll(i);
      }

      return var2;
   }

   private ItemStack roll(int i) {
      Item var2 = i < 9
         ? this.pick(HOTBAR, i)
         : (
            i < 36 || i == 41
               ? this.pick(MAIN, i)
               : (i == 40 ? this.pick(OFFHAND, i) : ARMOR[i - 36][(int)Long.remainderUnsigned(mix(this.seed ^ 167L), ARMOR[0].length)])
         );
      ItemStack var3 = new ItemStack(var2);
      if (var3.getMaxCount() > 1) {
         var3.setCount(1 + (int)Long.remainderUnsigned(mix(this.seed ^ i ^ 81L), var3.getMaxCount()));
      } else if (var3.isDamageable()) {
         var3.setDamage((int)Long.remainderUnsigned(mix(this.seed ^ i ^ 82L), var3.getMaxDamage() / 2));
      }

      return var3;
   }

   private Item pick(Item[] items, int i) {
      return items[(int)Long.remainderUnsigned(mix(this.seed ^ i * -7046029254386353131L), items.length)];
   }

   private static long mix(long l) {
      l = (l ^ l >>> 30) * -4658895280553007687L;
      l = (l ^ l >>> 27) * -7723592293110705685L;
      return l ^ l >>> 31;
   }
}
