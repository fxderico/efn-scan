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
      if (this.origin != -1 && MinecraftClient.getInstance().player != null) {
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
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      ClientPlayerEntity var5 = var4.player;
      if (var5 != null && var4.world != null) {
         int var6 = var5.getInventory().getSelectedSlot();
         if (this.origin != -1 && var6 != this.ourSlot) {
            this.origin = -1;
         }

         BlockPos var7 = var4.currentScreen == null && var4.options.attackKey.isPressed() ? aimedBlock(var4) : null;
         BlockState var3 = var7 == null ? null : var4.world.getBlockState(var7);
         if (var7 != null && !var3.isAir()) {
            if (!var7.equals(this.target)) {
               this.target = var7.toImmutable();
               this.pending = -1;
               this.delay = 0;
            }

            int var9 = shardSlot(var5.getInventory(), var3, var6);
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

   private static BlockPos aimedBlock(MinecraftClient minecraftClient) {
      HitResult var2 = minecraftClient.crosshairTarget;
      BlockHitResult var1;
      return var2 instanceof BlockHitResult && (var1 = (BlockHitResult)var2).getType() == Type.BLOCK ? var1.getBlockPos() : null;
   }

   private int drillSlot(MinecraftClient minecraftClient, BlockState blockState, BlockPos blockPos, int i) {
      if (i != -1) {
         return breakingDelta(minecraftClient, i, blockState, blockPos) >= 1.0F ? i : -1;
      } else {
         PlayerInventory var5 = minecraftClient.player.getInventory();

         for (int var6 = 0; var6 < 9; var6++) {
            ItemStack var7 = var5.getStack(var6);
            if (var7.isOf(Items.NETHERITE_PICKAXE) && !var7.willBreakNextUse() && breakingDelta(minecraftClient, var6, blockState, blockPos) >= 1.0F) {
               return var6;
            }
         }

         return -1;
      }
   }

   private int bestSlot(MinecraftClient minecraftClient, BlockState blockState, BlockPos blockPos, int i, int j) {
      PlayerInventory var6 = minecraftClient.player.getInventory();
      ItemStack var7 = var6.getStack(i);
      boolean var8 = i == this.ourSlot && this.origin != -1 && var7.willBreakNextUse();
      int var9 = i;
      float var10 = var8 ? -1.0F : breakingDelta(minecraftClient, i, blockState, blockPos);

      for (int var11 = 0; var11 < 9; var11++) {
         ItemStack var13 = var6.getStack(var11);
         float var12;
         if (var11 != i && !var13.isEmpty() && !var13.willBreakNextUse() && (var12 = breakingDelta(minecraftClient, var11, blockState, blockPos)) > var10) {
            var10 = var12;
            var9 = var11;
         }
      }

      return j != -1 && var6.getStack(var9).isIn(ItemTags.PICKAXES) ? j : var9;
   }

   private static int shardSlot(PlayerInventory playerInventory, BlockState blockState, int i) {
      if (isShardFor(playerInventory.getStack(i), blockState)) {
         return i;
      } else {
         for (int var3 = 0; var3 < 9; var3++) {
            if (var3 != i && isShardFor(playerInventory.getStack(var3), blockState)) {
               return var3;
            }
         }

         return -1;
      }
   }

   private static boolean isShardFor(ItemStack itemStack, BlockState blockState) {
      return !itemStack.willBreakNextUse() && itemStack.isSuitableFor(blockState) && isShardPickaxe(itemStack);
   }

   private static boolean isShardPickaxe(ItemStack itemStack) {
      if (!itemStack.isIn(ItemTags.PICKAXES)) {
         return false;
      } else if (AntiVanishText.normalize(itemStack.getName().getString()).contains("shard pickaxe")) {
         return true;
      } else {
         for (Text var2 : itemStack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines()) {
            if (AntiVanishText.normalize(var2.getString()).contains("breaks 9 blocks at once")) {
               return true;
            }
         }

         return false;
      }
   }

   private static float breakingDelta(MinecraftClient minecraftClient, int i, BlockState blockState, BlockPos blockPos) {
      float var4 = blockState.getHardness(minecraftClient.world, blockPos);
      if (var4 == -1.0F) {
         return 0.0F;
      } else {
         ClientPlayerEntity var5 = minecraftClient.player;
         ItemStack var6 = var5.getInventory().getStack(i);
         float var7 = var6.getMiningSpeedMultiplier(blockState);
         if (var7 > 1.0F) {
            double[] var8 = new double[]{0.0};
            var6.applyAttributeModifiers(EquipmentSlot.MAINHAND, (registryEntry, entityAttributeModifier) -> {
               if (registryEntry == EntityAttributes.MINING_EFFICIENCY) {
                  var8[0] += entityAttributeModifier.value();
               }
            });
            var7 += (float)var8[0];
         }

         boolean var9 = !blockState.isToolRequired() || var6.isSuitableFor(blockState);
         return withMiningModifiers(var5, var7) / var4 / (var9 ? 30 : 100);
      }
   }

   private static float withMiningModifiers(ClientPlayerEntity clientPlayerEntity, float f) {
      if (StatusEffectUtil.hasHaste(clientPlayerEntity)) {
         f *= 1.0F + (StatusEffectUtil.getHasteAmplifier(clientPlayerEntity) + 1) * 0.2F;
      }

      StatusEffectInstance var2;
      if ((var2 = clientPlayerEntity.getStatusEffect(StatusEffects.MINING_FATIGUE)) != null) {
         f *= switch (var2.getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
      }

      f *= (float)clientPlayerEntity.getAttributeValue(EntityAttributes.BLOCK_BREAK_SPEED);
      if (clientPlayerEntity.isSubmergedIn(FluidTags.WATER)) {
         f *= (float)clientPlayerEntity.getAttributeValue(EntityAttributes.SUBMERGED_MINING_SPEED);
      }

      if (!clientPlayerEntity.isOnGround()) {
         f /= 5.0F;
      }

      return f;
   }

   private void select(int i) {
      PlayerInventory var2 = MinecraftClient.getInstance().player.getInventory();
      if (this.origin == -1) {
         this.origin = var2.getSelectedSlot();
      }

      var2.setSelectedSlot(i);
      this.ourSlot = i;
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
