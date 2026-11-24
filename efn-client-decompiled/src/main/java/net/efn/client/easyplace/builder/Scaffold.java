package net.efn.client.easyplace.builder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.TransparentBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;

@Environment(EnvType.CLIENT)
final class Scaffold {
   private Scaffold() {
   }

   static boolean item(ItemStack itemStack) {
      return glass(itemStack) || itemStack.isIn(ItemTags.LEAVES);
   }

   static boolean glass(ItemStack itemStack) {
      Item var2 = itemStack.getItem();
      BlockItem var1;
      return var2 instanceof BlockItem && (var1 = (BlockItem)var2).getBlock() instanceof TransparentBlock;
   }

   static boolean block(BlockState blockState) {
      return blockState.isIn(BlockTags.LEAVES) || blockState.getBlock() instanceof TransparentBlock;
   }

   static int count(ClientPlayerEntity clientPlayerEntity) {
      int var1 = 0;
      PlayerInventory var2 = clientPlayerEntity.getInventory();

      for (int var3 = 0; var3 < 36; var3++) {
         if (item(var2.getStack(var3))) {
            var1 += var2.getStack(var3).getCount();
         }
      }

      if (item(clientPlayerEntity.getOffHandStack())) {
         var1 += clientPlayerEntity.getOffHandStack().getCount();
      }

      return var1;
   }

   static boolean has(ClientPlayerEntity clientPlayerEntity) {
      return Hands.has(clientPlayerEntity, Scaffold::item);
   }

   static boolean hold(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (Hands.has(clientPlayerEntity, itemStack -> itemStack.isOf(Items.GLASS))) {
         return Hands.hold(minecraftClient, clientPlayerEntity, itemStack -> itemStack.isOf(Items.GLASS));
      } else {
         return Hands.has(clientPlayerEntity, itemStack -> itemStack.isIn(ItemTags.LEAVES))
            ? Hands.hold(minecraftClient, clientPlayerEntity, itemStack -> itemStack.isIn(ItemTags.LEAVES))
            : Hands.hold(minecraftClient, clientPlayerEntity, Scaffold::glass);
      }
   }
}
