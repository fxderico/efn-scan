package net.efn.client.module.impl.render;

import com.example.client.mixin.AccessorHandPlayerModel;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

@Environment(EnvType.CLIENT)
public final class HandModelModule extends ClientModule {
   public static volatile HandModelModule active;
   private static final String ALL_ITEMS = "All Items";
   private static final String WEAPONS_TOOLS = "Weapons & Tools";
   private final ModeSetting items = this.addSetting("items", new ModeSetting("Items", "All Items", "All Items", "Weapons & Tools"));
   private final BooleanSetting mainHand = this.addSetting("main-hand", new BooleanSetting("Main Hand", true));
   private final BooleanSetting offHand = this.addSetting("off-hand", new BooleanSetting("Off Hand", true));

   public HandModelModule() {
      super("3d-hand", "3D Hand", "Shows your own arm holding the item in first person: swords, tools, items and blocks.", ModuleCategory.VISUALS);
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   static boolean wants(boolean bl) {
      HandModelModule var1 = active;
      return var1 != null && (bl ? var1.mainHand.get() : var1.offHand.get());
   }

   static boolean covers(ItemStack itemStack, boolean bl) {
      HandModelModule var2 = active;
      return wants(bl) && var2 != null && (var2.items.is("All Items") || weaponOrTool(itemStack));
   }

   private static boolean weaponOrTool(ItemStack itemStack) {
      return itemStack.isIn(ItemTags.SWORDS)
         || itemStack.isIn(ItemTags.AXES)
         || itemStack.isIn(ItemTags.PICKAXES)
         || itemStack.isIn(ItemTags.SHOVELS)
         || itemStack.isIn(ItemTags.HOES)
         || itemStack.isIn(ItemTags.SPEARS)
         || itemStack.isOf(Items.MACE)
         || itemStack.isOf(Items.FISHING_ROD)
         || itemStack.isOf(Items.SHEARS)
         || itemStack.isOf(Items.FLINT_AND_STEEL)
         || itemStack.isOf(Items.BRUSH)
         || itemStack.isOf(Items.CARROT_ON_A_STICK)
         || itemStack.isOf(Items.WARPED_FUNGUS_ON_A_STICK);
   }

   public static void shoulder(MatrixStack matrixStack, boolean bl, PlayerEntityRenderer<?> playerEntityRenderer) {
      FirstPersonArm.shoulder(matrixStack, bl, ((AccessorHandPlayerModel)playerEntityRenderer.getModel()).efn$handSlim());
   }

   public static void renderArm(
      MatrixStack matrixStack,
      LivingEntity livingEntity,
      ItemStack itemStack,
      ItemDisplayContext itemDisplayContext,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      boolean bl
   ) {
      if (covers(itemStack, bl)) {
         FirstPersonArm.Arm var7 = FirstPersonArm.arm();
         if (var7 != null) {
            FirstPersonArm.hold(matrixStack, var7, itemStack, itemDisplayContext, livingEntity, orderedRenderCommandQueue, i, 0.0F, 0.0F, 0.0F);
         }
      }
   }
}
