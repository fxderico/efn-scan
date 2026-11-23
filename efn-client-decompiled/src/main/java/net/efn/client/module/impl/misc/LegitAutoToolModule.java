package net.efn.client.module.impl.misc;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.util.antivanish.AntiVanishText;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class LegitAutoToolModule extends ClientModule {
   private static final int SWITCH_DELAY_TICKS = 2;
   private static final String SHARD_NAME = "shard pickaxe";
   private static final String SHARD_LORE = "breaks 9 blocks at once";
   private final BooleanSetting drill = this.addSetting("use-drill-for-instabreak", new BooleanSetting("Drill Instabreak", true));
   private final BooleanSetting delayed = this.addSetting("delay", new BooleanSetting("Delay", true));
   private BlockPos target;
   private int pending = -1;
   private int delay;
   private int origin = -1;
   private int ourSlot = -1;
   private int returnDelay;

   public LegitAutoToolModule() {
      super("legit-auto-tool", "Legit Auto Tool", "Auto tool that only switches hotbar slots like a player.", ModuleCategory.MISC);
   }

   @Override
   protected void onEnable() {
      this.reset();
   }

   @Override
   protected void onDisable() {
      if (this.origin != -1 && MinecraftClient.method_1551().field_1724 != null) {
         this.select(this.origin);
      }

      this.reset();
   }

   private void reset() {
      this.target = null;
      this.pending = -1;
      this.delay = 0;
      this.origin = -1;
      this.ourSlot = -1;
      this.returnDelay = 0;
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var4 = MinecraftClient.method_1551();
      ClientPlayerEntity var5 = var4.field_1724;
      if (var5 != null && var4.field_1687 != null) {
         int var6 = var5.method_31548().method_67532();
         if (this.origin != -1 && var6 != this.ourSlot) {
            this.origin = -1;
         }

         BlockPos var7 = var4.field_1755 == null && var4.field_1690.field_1886.method_1434() ? aimedBlock(var4) : null;
         BlockState var3 = var7 == null ? null : var4.field_1687.method_8320(var7);
         if (var7 != null && !var3.method_26215()) {
            if (!var7.equals(this.target)) {
               this.target = var7.method_10062();
               this.pending = -1;
               this.delay = 0;
            }

            int var9 = shardSlot(var5.method_31548(), var3, var6);
            int var2;
            if (this.drill.get() && (var2 = this.drillSlot(var4, var3, var7, var9)) != -1) {
               if (var2 != var6) {
                  this.select(var2);
               }

               this.pending = -1;
            } else {
               int var10 = this.bestSlot(var4, var3, var7, var6, var9);
               if (var10 == var6) {
                  this.pending = -1;
               } else {
                  if (this.pending != var10) {
                     this.pending = var10;
                     this.delay = this.switchDelay();
                  }

                  if (this.delay > 0) {
                     this.delay--;
                  } else {
                     this.select(var10);
                     this.pending = -1;
                  }
               }
            }
         } else {
            this.target = null;
            this.pending = -1;
            this.delay = 0;
            this.giveBack();
         }
      } else {
         this.reset();
      }
   }

   private static BlockPos aimedBlock(MinecraftClient var0) {
      HitResult var2 = var0.field_1765;
      BlockHitResult var1;
      return var2 instanceof BlockHitResult && (var1 = (BlockHitResult)var2).method_17783() == Type.field_1332 ? var1.method_17777() : null;
   }

   private int drillSlot(MinecraftClient var1, BlockState var2, BlockPos var3, int var4) {
      if (var4 != -1) {
         return breakingDelta(var1, var4, var2, var3) >= 1.0F ? var4 : -1;
      } else {
         PlayerInventory var5 = var1.field_1724.method_31548();

         for (int var6 = 0; var6 < 9; var6++) {
            ItemStack var7 = var5.method_5438(var6);
            if (var7.method_31574(Items.field_22024) && !var7.method_63692() && breakingDelta(var1, var6, var2, var3) >= 1.0F) {
               return var6;
            }
         }

         return -1;
      }
   }

   private int bestSlot(MinecraftClient var1, BlockState var2, BlockPos var3, int var4, int var5) {
      PlayerInventory var6 = var1.field_1724.method_31548();
      ItemStack var7 = var6.method_5438(var4);
      boolean var8 = var4 == this.ourSlot && this.origin != -1 && var7.method_63692();
      int var9 = var4;
      float var10 = var8 ? -1.0F : breakingDelta(var1, var4, var2, var3);

      for (int var11 = 0; var11 < 9; var11++) {
         ItemStack var13 = var6.method_5438(var11);
         float var12;
         if (var11 != var4 && !var13.method_7960() && !var13.method_63692() && (var12 = breakingDelta(var1, var11, var2, var3)) > var10) {
            var10 = var12;
            var9 = var11;
         }
      }

      return var5 != -1 && var6.method_5438(var9).method_31573(ItemTags.field_42614) ? var5 : var9;
   }

   private static int shardSlot(PlayerInventory var0, BlockState var1, int var2) {
      if (isShardFor(var0.method_5438(var2), var1)) {
         return var2;
      } else {
         for (int var3 = 0; var3 < 9; var3++) {
            if (var3 != var2 && isShardFor(var0.method_5438(var3), var1)) {
               return var3;
            }
         }

         return -1;
      }
   }

   private static boolean isShardFor(ItemStack var0, BlockState var1) {
      return !var0.method_63692() && var0.method_7951(var1) && isShardPickaxe(var0);
   }

   private static boolean isShardPickaxe(ItemStack var0) {
      if (!var0.method_31573(ItemTags.field_42614)) {
         return false;
      } else if (AntiVanishText.normalize(var0.method_7964().getString()).contains("shard pickaxe")) {
         return true;
      } else {
         for (Text var2 : ((LoreComponent)var0.method_58695(DataComponentTypes.field_49632, LoreComponent.field_49340)).comp_2400()) {
            if (AntiVanishText.normalize(var2.getString()).contains("breaks 9 blocks at once")) {
               return true;
            }
         }

         return false;
      }
   }

   private static float breakingDelta(MinecraftClient var0, int var1, BlockState var2, BlockPos var3) {
      float var4 = var2.method_26214(var0.field_1687, var3);
      if (var4 == -1.0F) {
         return 0.0F;
      } else {
         ClientPlayerEntity var5 = var0.field_1724;
         ItemStack var6 = var5.method_31548().method_5438(var1);
         float var7 = var6.method_7924(var2);
         if (var7 > 1.0F) {
            double[] var8 = new double[]{0.0};
            var6.method_57354(EquipmentSlot.field_6173, (var1x, var2x) -> {
               if (var1x == EntityAttributes.field_51581) {
                  var8[0] += var2x.comp_2449();
               }
            });
            var7 += (float)var8[0];
         }

         boolean var9 = !var2.method_29291() || var6.method_7951(var2);
         return withMiningModifiers(var5, var7) / var4 / (var9 ? 30 : 100);
      }
   }

   private static float withMiningModifiers(ClientPlayerEntity var0, float var1) {
      if (StatusEffectUtil.method_5576(var0)) {
         var1 *= 1.0F + (StatusEffectUtil.method_5575(var0) + 1) * 0.2F;
      }

      StatusEffectInstance var2;
      if ((var2 = var0.method_6112(StatusEffects.field_5901)) != null) {
         var1 *= switch (var2.method_5578()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
      }

      var1 *= (float)var0.method_45325(EntityAttributes.field_49076);
      if (var0.method_5777(FluidTags.field_15517)) {
         var1 *= (float)var0.method_45325(EntityAttributes.field_51576);
      }

      if (!var0.method_24828()) {
         var1 /= 5.0F;
      }

      return var1;
   }

   private void select(int var1) {
      PlayerInventory var2 = MinecraftClient.method_1551().field_1724.method_31548();
      if (this.origin == -1) {
         this.origin = var2.method_67532();
      }

      var2.method_61496(var1);
      this.ourSlot = var1;
      this.returnDelay = this.switchDelay();
   }

   private int switchDelay() {
      return this.delayed.get() ? 2 : 0;
   }

   private void giveBack() {
      if (this.origin != -1) {
         if (this.returnDelay > 0) {
            this.returnDelay--;
         } else {
            this.select(this.origin);
            this.origin = -1;
         }
      }
   }
}
