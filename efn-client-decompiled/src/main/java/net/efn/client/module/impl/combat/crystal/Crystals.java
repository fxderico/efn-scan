package net.efn.client.module.impl.combat.crystal;

import com.example.client.mixin.AccessorLocalPlayer;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.efn.client.input.EfnKeyPolling;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.effect.EnchantmentEffectEntry;
import net.minecraft.enchantment.effect.value.AddEnchantmentEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class Crystals {
   public static final int READY = -2;
   public static final int NO_ROUTE = -1;
   private static final int MIN_CONFIRM_TICKS = 4;
   private static final int MAX_CONFIRM_TICKS = 20;
   private static final float RTT_SMOOTHING = 0.25F;
   private static final double RESERVE_RANGE = 12.0;
   private static final Set<Block> USABLE_BLOCKS = Set.of(
      Blocks.RESPAWN_ANCHOR, Blocks.NOTE_BLOCK, Blocks.LEVER, Blocks.REPEATER, Blocks.REDSTONE_WIRE, Blocks.CAKE, Blocks.COMPOSTER, Blocks.DRAGON_EGG
   );
   private static final Long2IntOpenHashMap PLACED_AT = new Long2IntOpenHashMap();
   private static float measuredRtt;

   private Crystals() {
   }

   public static boolean isBase(BlockState blockState) {
      return blockState.isOf(Blocks.OBSIDIAN) || blockState.isOf(Blocks.BEDROCK);
   }

   public static Vec3d explosionAt(BlockPos blockPos) {
      return Vec3d.ofBottomCenter(blockPos.up());
   }

   public static Box boxOn(BlockPos blockPos) {
      return EntityType.END_CRYSTAL.getDimensions().getBoxAt(explosionAt(blockPos));
   }

   public static boolean hasCrystal(World world, BlockPos blockPos, Entity entity) {
      BlockPos var3 = blockPos.up();

      for (EndCrystalEntity var5 : world.getNonSpectatingEntities(EndCrystalEntity.class, new Box(var3))) {
         if (var5 != entity && var5.getBlockPos().equals(var3)) {
            return true;
         }
      }

      return false;
   }

   public static boolean canHost(World world, BlockPos blockPos, Entity entity) {
      BlockPos var3 = blockPos.up();
      if (!world.isAir(var3)) {
         return false;
      } else {
         Box var4 = new Box(var3.getX(), var3.getY(), var3.getZ(), var3.getX() + 1.0, var3.getY() + 2.0, var3.getZ() + 1.0);

         for (Entity var6 : world.getOtherEntities(entity, var4)) {
            if (var6.isAlive()) {
               return false;
            }
         }

         return true;
      }
   }

   public static BlockPos obsidianTarget(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockHitResult blockHitResult, BlockState blockState) {
      BlockPos var4 = placedObsidian(clientWorld, clientPlayerEntity, blockHitResult, blockState);
      return var4 != null && canHost(clientWorld, var4, null) ? var4 : null;
   }

   public static BlockPos placedObsidian(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockHitResult blockHitResult, BlockState blockState) {
      if (!clientPlayerEntity.shouldCancelInteraction() && reactsToUse(clientWorld, blockHitResult.getBlockPos(), blockState)) {
         return null;
      } else {
         ItemPlacementContext var4 = new ItemPlacementContext(clientPlayerEntity, Hand.MAIN_HAND, new ItemStack(Items.OBSIDIAN), blockHitResult);
         if (!var4.canPlace()) {
            return null;
         } else {
            BlockPos var5 = var4.getBlockPos();
            if (clientWorld.isOutOfHeightLimit(var5) || !clientWorld.getWorldBorder().contains(var5)) {
               return null;
            } else {
               return !clientWorld.canPlace(Blocks.OBSIDIAN.getDefaultState(), var5, ShapeContext.ofPlacement(clientPlayerEntity)) ? null : var5;
            }
         }
      }
   }

   private static boolean reactsToUse(World world, BlockPos blockPos, BlockState blockState) {
      Block var3 = blockState.getBlock();
      if (var3 instanceof BlockEntityProvider || USABLE_BLOCKS.contains(var3)) {
         return true;
      } else {
         return !blockState.isIn(BlockTags.DOORS)
               && !blockState.isIn(BlockTags.TRAPDOORS)
               && !blockState.isIn(BlockTags.FENCE_GATES)
               && !blockState.isIn(BlockTags.BUTTONS)
               && !blockState.isIn(BlockTags.CAULDRONS)
               && !blockState.isIn(BlockTags.CANDLE_CAKES)
               && !blockState.isIn(BlockTags.FLOWER_POTS)
            ? blockState.createScreenHandlerFactory(world, blockPos) != null
            : true;
      }
   }

   public static int route(ClientPlayerEntity clientPlayerEntity, Item item, BlockState blockState, int i) {
      ItemStack var4 = clientPlayerEntity.getMainHandStack();
      if (var4.isOf(item)) {
         return -2;
      } else {
         boolean var5 = clientPlayerEntity.getOffHandStack().isOf(item);
         if (var5 && letsClickThrough(var4, blockState)) {
            return -2;
         } else {
            PlayerInventory var6 = clientPlayerEntity.getInventory();
            int var7 = hotbarSlotOf(var6, item);
            if (var7 != -1) {
               return var7;
            } else if (!var5) {
               return -1;
            } else if (PlayerInventory.isValidHotbarIndex(i) && letsClickThrough(var6.getStack(i), blockState)) {
               return i;
            } else {
               for (int var8 = 0; var8 < PlayerInventory.getHotbarSize(); var8++) {
                  if (letsClickThrough(var6.getStack(var8), blockState)) {
                     return var8;
                  }
               }

               return -1;
            }
         }
      }
   }

   public static boolean letsClickThrough(ItemStack itemStack, BlockState blockState) {
      if (itemStack.isEmpty()) {
         return true;
      } else if (!itemStack.contains(DataComponentTypes.CONSUMABLE)
         && !itemStack.contains(DataComponentTypes.BLOCKS_ATTACKS)
         && !itemStack.contains(DataComponentTypes.KINETIC_WEAPON)) {
         EquippableComponent var2 = itemStack.get(DataComponentTypes.EQUIPPABLE);
         return var2 != null && var2.swappable()
            ? false
            : itemStack.isIn(ItemTags.SWORDS)
               || itemStack.isOf(Items.MACE)
               || itemStack.isOf(Items.TOTEM_OF_UNDYING)
               || itemStack.isIn(ItemTags.AXES) && isBase(blockState);
      } else {
         return false;
      }
   }

   public static boolean isCombatItem(ItemStack itemStack) {
      return itemStack.isEmpty()
         || itemStack.isIn(ItemTags.SWORDS)
         || itemStack.isIn(ItemTags.AXES)
         || itemStack.isOf(Items.MACE)
         || itemStack.isOf(Items.TOTEM_OF_UNDYING);
   }

   public static int hotbarSlotOf(PlayerInventory playerInventory, Item item) {
      for (int var2 = 0; var2 < PlayerInventory.getHotbarSize(); var2++) {
         if (playerInventory.getStack(var2).isOf(item)) {
            return var2;
         }
      }

      return -1;
   }

   public static boolean strike(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, Entity entity) {
      if (clientPlayerEntity != null
         && minecraftClient.interactionManager != null
         && entity != null
         && !entity.isRemoved()
         && !clientPlayerEntity.isSpectator()) {
         if (!MeleeAttacks.actedThisTick(clientPlayerEntity)
            && MeleeAttacks.handsFree(minecraftClient, clientPlayerEntity)
            && clientPlayerEntity.isSprinting() == ((AccessorLocalPlayer)clientPlayerEntity).efn$wasSprinting()) {
            minecraftClient.interactionManager.attackEntity(clientPlayerEntity, entity);
            clientPlayerEntity.swingHand(Hand.MAIN_HAND);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean rightClick(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, BlockHitResult blockHitResult) {
      if (!MeleeAttacks.actedThisTick(clientPlayerEntity) && MeleeAttacks.handsFree(minecraftClient, clientPlayerEntity)) {
         Hand var4 = letsClickThrough(clientPlayerEntity.getMainHandStack(), minecraftClient.world.getBlockState(blockHitResult.getBlockPos()))
            ? Hand.OFF_HAND
            : Hand.MAIN_HAND;
         ItemStack var5 = clientPlayerEntity.getStackInHand(var4);
         if (!var5.isItemEnabled(minecraftClient.world.getEnabledFeatures())) {
            return false;
         } else {
            int var6 = var5.getCount();
            ActionResult var7 = minecraftClient.interactionManager.interactBlock(clientPlayerEntity, var4, blockHitResult);
            Success var3;
            if (var7 instanceof Success && (var3 = (Success)var7).swingSource() == SwingSource.CLIENT) {
               clientPlayerEntity.swingHand(var4);
               if (!var5.isEmpty() && (var5.getCount() != var6 || clientPlayerEntity.isInCreativeMode())) {
                  minecraftClient.gameRenderer.firstPersonRenderer.resetEquipProgress(var4);
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static int weaponRoute(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var1 = clientPlayerEntity.getMainHandStack();
      if (!swings(clientPlayerEntity, var1) || !enchantsAddDamage(var1) && !dealsDamage(clientPlayerEntity, var1)) {
         PlayerInventory var2 = clientPlayerEntity.getInventory();
         int var3 = -1;

         for (int var4 = 0; var4 < PlayerInventory.getHotbarSize(); var4++) {
            ItemStack var5 = var2.getStack(var4);
            if (swings(clientPlayerEntity, var5)) {
               if (enchantsAddDamage(var5)) {
                  return var4;
               }

               if (var3 == -1 && dealsDamage(clientPlayerEntity, var5)) {
                  var3 = var4;
               }
            }
         }

         return var3;
      } else {
         return -2;
      }
   }

   private static boolean swings(ClientPlayerEntity clientPlayerEntity, ItemStack itemStack) {
      return !itemStack.contains(DataComponentTypes.PIERCING_WEAPON) && !clientPlayerEntity.isBelowMinimumAttackCharge(itemStack, 0);
   }

   public static boolean enchantsAddDamage(ItemStack itemStack) {
      float var1 = 0.0F;

      for (Entry var3 : itemStack.getEnchantments().getEnchantmentEntries()) {
         Enchantment var4 = (Enchantment)((RegistryEntry)var3.getKey()).value();

         for (EnchantmentEffectEntry var6 : var4.getEffect(EnchantmentEffectComponentTypes.DAMAGE)) {
            Object var7;
            if (var6.requirements().isPresent() || !((var7 = var6.effect()) instanceof AddEnchantmentEffect)) {
               return false;
            }

            AddEnchantmentEffect var8 = (AddEnchantmentEffect)var7;
            var1 += var8.value().getValue(var3.getIntValue());
         }
      }

      return var1 > 0.0F;
   }

   public static boolean dealsDamage(LivingEntity livingEntity, ItemStack itemStack) {
      EntityAttributeInstance var2 = new EntityAttributeInstance(EntityAttributes.ATTACK_DAMAGE, entityAttributeInstance -> {});
      var2.setBaseValue(livingEntity.getAttributeBaseValue(EntityAttributes.ATTACK_DAMAGE));

      for (StatusEffectInstance var4 : livingEntity.getStatusEffects()) {
         var4.getEffectType()
            .value()
            .forEachAttributeModifier(var4.getAmplifier(), (registryEntry, entityAttributeModifier) -> include(var2, registryEntry, entityAttributeModifier));
      }

      for (EquipmentSlot var7 : EquipmentSlot.VALUES) {
         ItemStack var5 = var7 == EquipmentSlot.MAINHAND ? itemStack : livingEntity.getEquippedStack(var7);
         if (!var5.isEmpty() && !var5.shouldBreak()) {
            var5.applyAttributeModifiers(var7, (registryEntry, entityAttributeModifier) -> include(var2, registryEntry, entityAttributeModifier));
         }
      }

      return (float)var2.getValue() > 0.0F;
   }

   private static void include(
      EntityAttributeInstance entityAttributeInstance, RegistryEntry<EntityAttribute> registryEntry, EntityAttributeModifier entityAttributeModifier
   ) {
      if (EntityAttributes.ATTACK_DAMAGE.equals(registryEntry)) {
         entityAttributeInstance.removeModifier(entityAttributeModifier.id());
         entityAttributeInstance.addTemporaryModifier(entityAttributeModifier);
      }
   }

   public static int confirmWindow(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      return MathHelper.clamp(rttTicks(minecraftClient, clientPlayerEntity) + 2, 4, 20);
   }

   public static int rttTicks(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      return measuredRtt > 0.0F ? Math.round(measuredRtt) : latencyMs(minecraftClient, clientPlayerEntity) / 50 + 1;
   }

   public static void notePlace(ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      ObjectIterator var2 = PLACED_AT.long2IntEntrySet().iterator();

      while (var2.hasNext()) {
         int var3 = clientPlayerEntity.age - ((it.unimi.dsi.fastutil.longs.Long2IntMap.Entry)var2.next()).getIntValue();
         if (var3 < 0 || var3 > 20) {
            var2.remove();
         }
      }

      PLACED_AT.put(blockPos.asLong(), clientPlayerEntity.age);
   }

   public static void noteSpawn(ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      long var2 = blockPos.asLong();
      if (PLACED_AT.containsKey(var2)) {
         int var4 = clientPlayerEntity.age - PLACED_AT.remove(var2);
         if (var4 > 0 && var4 <= 20) {
            measuredRtt = measuredRtt > 0.0F ? measuredRtt + (var4 - measuredRtt) * 0.25F : var4;
         }
      }
   }

   public static boolean placing(ClientPlayerEntity clientPlayerEntity, BlockPos blockPos, int i) {
      long var3 = blockPos.asLong();
      return PLACED_AT.containsKey(var3) && sent(clientPlayerEntity, PLACED_AT.get(var3), i);
   }

   public static boolean anyPlacing(ClientPlayerEntity clientPlayerEntity, int i) {
      IntIterator var2 = PLACED_AT.values().iterator();

      while (var2.hasNext()) {
         int var3 = var2.next();
         if (sent(clientPlayerEntity, var3, i)) {
            return true;
         }
      }

      return false;
   }

   public static List<Box> reservedBoxes(ClientPlayerEntity clientPlayerEntity, int i) {
      ArrayList var2 = new ArrayList();

      for (EndCrystalEntity var4 : clientPlayerEntity.getEntityWorld()
         .getNonSpectatingEntities(EndCrystalEntity.class, clientPlayerEntity.getBoundingBox().expand(12.0))) {
         if (var4.isAlive()) {
            var2.add(var4.getBoundingBox());
         }
      }

      for (it.unimi.dsi.fastutil.longs.Long2IntMap.Entry var6 : PLACED_AT.long2IntEntrySet()) {
         if (sent(clientPlayerEntity, var6.getIntValue(), i)) {
            var2.add(boxOn(BlockPos.fromLong(var6.getLongKey())));
         }
      }

      return var2;
   }

   private static boolean sent(ClientPlayerEntity clientPlayerEntity, int i, int j) {
      int var3 = clientPlayerEntity.age - i;
      return var3 >= 0 && var3 < j;
   }

   public static int humanGap() {
      double var0 = ThreadLocalRandom.current().nextDouble();
      return var0 < 0.7 ? 2 : (var0 < 0.95 ? 3 : 4);
   }

   public static void forgetRtt() {
      PLACED_AT.clear();
      measuredRtt = 0.0F;
   }

   private static int latencyMs(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      ClientPlayNetworkHandler var2 = minecraftClient.getNetworkHandler();
      if (var2 == null) {
         return 0;
      } else {
         PlayerListEntry var3 = var2.getPlayerListEntry(clientPlayerEntity.getUuid());
         return var3 == null ? 0 : Math.max(var3.getLatency(), 0);
      }
   }

   public static boolean isKeyDown(MinecraftClient minecraftClient, int i) {
      if (!ModuleKeybinds.isBindable(i)) {
         return false;
      } else {
         return i >= 1000
            ? GLFW.glfwGetMouseButton(minecraftClient.getWindow().getHandle(), i - 1000) == 1
            : EfnKeyPolling.isKeyDown(minecraftClient.getWindow(), i);
      }
   }

   public static boolean isPhysicallyDown(MinecraftClient minecraftClient, KeyBinding keyBinding) {
      Key var2;
      try {
         var2 = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
      } catch (IllegalArgumentException var4) {
         return false;
      }

      int var3 = var2.getCode();
      if (var3 < 0) {
         return false;
      } else {
         return switch (var2.getCategory()) {
            case MOUSE -> GLFW.glfwGetMouseButton(minecraftClient.getWindow().getHandle(), var3) == 1;
            case KEYSYM -> EfnKeyPolling.isKeyDown(minecraftClient.getWindow(), var3);
            default -> false;
         };
      }
   }
}
