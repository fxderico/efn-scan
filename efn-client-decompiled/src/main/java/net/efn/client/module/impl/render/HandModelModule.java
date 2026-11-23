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

   static boolean wants(boolean var0) {
      HandModelModule var1 = active;
      return var1 != null && (var0 ? var1.mainHand.get() : var1.offHand.get());
   }

   static boolean covers(ItemStack var0, boolean var1) {
      HandModelModule var2 = active;
      return wants(var1) && var2 != null && (var2.items.is("All Items") || weaponOrTool(var0));
   }

   private static boolean weaponOrTool(ItemStack var0) {
      return var0.method_31573(ItemTags.field_42611)
         || var0.method_31573(ItemTags.field_42612)
         || var0.method_31573(ItemTags.field_42614)
         || var0.method_31573(ItemTags.field_42615)
         || var0.method_31573(ItemTags.field_42613)
         || var0.method_31573(ItemTags.field_63257)
         || var0.method_31574(Items.field_49814)
         || var0.method_31574(Items.field_8378)
         || var0.method_31574(Items.field_8868)
         || var0.method_31574(Items.field_8884)
         || var0.method_31574(Items.field_42716)
         || var0.method_31574(Items.field_8184)
         || var0.method_31574(Items.field_23254);
   }

   public static void shoulder(MatrixStack var0, boolean var1, PlayerEntityRenderer<?> var2) {
      FirstPersonArm.shoulder(var0, var1, ((AccessorHandPlayerModel)var2.method_4038()).efn$handSlim());
   }

   public static void renderArm(
      MatrixStack var0, LivingEntity var1, ItemStack var2, ItemDisplayContext var3, OrderedRenderCommandQueue var4, int var5, boolean var6
   ) {
      if (covers(var2, var6)) {
         FirstPersonArm.Arm var7 = FirstPersonArm.arm();
         if (var7 != null) {
            FirstPersonArm.hold(var0, var7, var2, var3, var1, var4, var5, 0.0F, 0.0F, 0.0F);
         }
      }
   }
}
