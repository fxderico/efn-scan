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
import net.minecraft.entity.effect.StatusEffect;
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
      Blocks.field_23152,
      Blocks.field_10179,
      Blocks.field_10363,
      Blocks.field_10450,
      Blocks.field_10091,
      Blocks.field_10183,
      Blocks.field_17563,
      Blocks.field_10081
   );
   private static final Long2IntOpenHashMap PLACED_AT = new Long2IntOpenHashMap();
   private static float measuredRtt;

   private Crystals() {
   }

   public static boolean isBase(BlockState var0) {
      return var0.method_27852(Blocks.field_10540) || var0.method_27852(Blocks.field_9987);
   }

   public static Vec3d explosionAt(BlockPos var0) {
      return Vec3d.method_24955(var0.method_10084());
   }

   public static Box boxOn(BlockPos var0) {
      return EntityType.field_6110.method_18386().method_30757(explosionAt(var0));
   }

   public static boolean hasCrystal(World var0, BlockPos var1, Entity var2) {
      BlockPos var3 = var1.method_10084();

      for (EndCrystalEntity var5 : var0.method_18467(EndCrystalEntity.class, new Box(var3))) {
         if (var5 != var2 && var5.method_24515().equals(var3)) {
            return true;
         }
      }

      return false;
   }

   public static boolean canHost(World var0, BlockPos var1, Entity var2) {
      BlockPos var3 = var1.method_10084();
      if (!var0.method_22347(var3)) {
         return false;
      } else {
         Box var4 = new Box(
            var3.method_10263(), var3.method_10264(), var3.method_10260(), var3.method_10263() + 1.0, var3.method_10264() + 2.0, var3.method_10260() + 1.0
         );

         for (Entity var6 : var0.method_8335(var2, var4)) {
            if (var6.method_5805()) {
               return false;
            }
         }

         return true;
      }
   }

   public static BlockPos obsidianTarget(ClientWorld var0, ClientPlayerEntity var1, BlockHitResult var2, BlockState var3) {
      BlockPos var4 = placedObsidian(var0, var1, var2, var3);
      return var4 != null && canHost(var0, var4, null) ? var4 : null;
   }

   public static BlockPos placedObsidian(ClientWorld var0, ClientPlayerEntity var1, BlockHitResult var2, BlockState var3) {
      if (!var1.method_21823() && reactsToUse(var0, var2.method_17777(), var3)) {
         return null;
      } else {
         ItemPlacementContext var4 = new ItemPlacementContext(var1, Hand.field_5808, new ItemStack(Items.field_8281), var2);
         if (!var4.method_7716()) {
            return null;
         } else {
            BlockPos var5 = var4.method_8037();
            if (var0.method_31606(var5) || !var0.method_8621().method_11952(var5)) {
               return null;
            } else {
               return !var0.method_8628(Blocks.field_10540.method_9564(), var5, ShapeContext.method_67715(var1)) ? null : var5;
            }
         }
      }
   }

   private static boolean reactsToUse(World var0, BlockPos var1, BlockState var2) {
      Block var3 = var2.method_26204();
      if (var3 instanceof BlockEntityProvider || USABLE_BLOCKS.contains(var3)) {
         return true;
      } else {
         return !var2.method_26164(BlockTags.field_15495)
               && !var2.method_26164(BlockTags.field_15487)
               && !var2.method_26164(BlockTags.field_25147)
               && !var2.method_26164(BlockTags.field_15493)
               && !var2.method_26164(BlockTags.field_26985)
               && !var2.method_26164(BlockTags.field_26984)
               && !var2.method_26164(BlockTags.field_15470)
            ? var2.method_26196(var0, var1) != null
            : true;
      }
   }

   public static int route(ClientPlayerEntity var0, Item var1, BlockState var2, int var3) {
      ItemStack var4 = var0.method_6047();
      if (var4.method_31574(var1)) {
         return -2;
      } else {
         boolean var5 = var0.method_6079().method_31574(var1);
         if (var5 && letsClickThrough(var4, var2)) {
            return -2;
         } else {
            PlayerInventory var6 = var0.method_31548();
            int var7 = hotbarSlotOf(var6, var1);
            if (var7 != -1) {
               return var7;
            } else if (!var5) {
               return -1;
            } else if (PlayerInventory.method_7380(var3) && letsClickThrough(var6.method_5438(var3), var2)) {
               return var3;
            } else {
               for (int var8 = 0; var8 < PlayerInventory.method_7368(); var8++) {
                  if (letsClickThrough(var6.method_5438(var8), var2)) {
                     return var8;
                  }
               }

               return -1;
            }
         }
      }
   }

   public static boolean letsClickThrough(ItemStack var0, BlockState var1) {
      if (var0.method_7960()) {
         return true;
      } else if (!var0.method_57826(DataComponentTypes.field_53964)
         && !var0.method_57826(DataComponentTypes.field_56396)
         && !var0.method_57826(DataComponentTypes.field_63632)) {
         EquippableComponent var2 = (EquippableComponent)var0.method_58694(DataComponentTypes.field_54196);
         return var2 != null && var2.comp_3213()
            ? false
            : var0.method_31573(ItemTags.field_42611)
               || var0.method_31574(Items.field_49814)
               || var0.method_31574(Items.field_8288)
               || var0.method_31573(ItemTags.field_42612) && isBase(var1);
      } else {
         return false;
      }
   }

   public static boolean isCombatItem(ItemStack var0) {
      return var0.method_7960()
         || var0.method_31573(ItemTags.field_42611)
         || var0.method_31573(ItemTags.field_42612)
         || var0.method_31574(Items.field_49814)
         || var0.method_31574(Items.field_8288);
   }

   public static int hotbarSlotOf(PlayerInventory var0, Item var1) {
      for (int var2 = 0; var2 < PlayerInventory.method_7368(); var2++) {
         if (var0.method_5438(var2).method_31574(var1)) {
            return var2;
         }
      }

      return -1;
   }

   public static boolean strike(MinecraftClient var0, ClientPlayerEntity var1, Entity var2) {
      if (var1 != null && var0.field_1761 != null && var2 != null && !var2.method_31481() && !var1.method_7325()) {
         if (!MeleeAttacks.actedThisTick(var1) && MeleeAttacks.handsFree(var0, var1) && var1.method_5624() == ((AccessorLocalPlayer)var1).efn$wasSprinting()) {
            var0.field_1761.method_2918(var1, var2);
            var1.method_6104(Hand.field_5808);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean rightClick(MinecraftClient var0, ClientPlayerEntity var1, BlockHitResult var2) {
      if (!MeleeAttacks.actedThisTick(var1) && MeleeAttacks.handsFree(var0, var1)) {
         Hand var4 = letsClickThrough(var1.method_6047(), var0.field_1687.method_8320(var2.method_17777())) ? Hand.field_5810 : Hand.field_5808;
         ItemStack var5 = var1.method_5998(var4);
         if (!var5.method_45435(var0.field_1687.method_45162())) {
            return false;
         } else {
            int var6 = var5.method_7947();
            ActionResult var7 = var0.field_1761.method_2896(var1, var4, var2);
            Success var3;
            if (var7 instanceof Success && (var3 = (Success)var7).comp_2909() == SwingSource.field_52427) {
               var1.method_6104(var4);
               if (!var5.method_7960() && (var5.method_7947() != var6 || var1.method_56992())) {
                  var0.field_1773.field_4012.method_3215(var4);
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static int weaponRoute(ClientPlayerEntity var0) {
      ItemStack var1 = var0.method_6047();
      if (!swings(var0, var1) || !enchantsAddDamage(var1) && !dealsDamage(var0, var1)) {
         PlayerInventory var2 = var0.method_31548();
         int var3 = -1;

         for (int var4 = 0; var4 < PlayerInventory.method_7368(); var4++) {
            ItemStack var5 = var2.method_5438(var4);
            if (swings(var0, var5)) {
               if (enchantsAddDamage(var5)) {
                  return var4;
               }

               if (var3 == -1 && dealsDamage(var0, var5)) {
                  var3 = var4;
               }
            }
         }

         return var3;
      } else {
         return -2;
      }
   }

   private static boolean swings(ClientPlayerEntity var0, ItemStack var1) {
      return !var1.method_57826(DataComponentTypes.field_63631) && !var0.method_75202(var1, 0);
   }

   public static boolean enchantsAddDamage(ItemStack var0) {
      float var1 = 0.0F;

      for (Entry var3 : var0.method_58657().method_57539()) {
         Enchantment var4 = (Enchantment)((RegistryEntry)var3.getKey()).comp_349();

         for (EnchantmentEffectEntry var6 : var4.method_60034(EnchantmentEffectComponentTypes.field_51661)) {
            Object var7;
            if (var6.comp_2681().isPresent() || !((var7 = var6.comp_2680()) instanceof AddEnchantmentEffect)) {
               return false;
            }

            AddEnchantmentEffect var8 = (AddEnchantmentEffect)var7;
            var1 += var8.comp_2704().method_60188(var3.getIntValue());
         }
      }

      return var1 > 0.0F;
   }

   public static boolean dealsDamage(LivingEntity var0, ItemStack var1) {
      EntityAttributeInstance var2 = new EntityAttributeInstance(EntityAttributes.field_23721, var0x -> {});
      var2.method_6192(var0.method_45326(EntityAttributes.field_23721));

      for (StatusEffectInstance var4 : var0.method_6026()) {
         ((StatusEffect)var4.method_5579().comp_349()).method_55650(var4.method_5578(), (var1x, var2x) -> include(var2, var1x, var2x));
      }

      for (EquipmentSlot var7 : EquipmentSlot.field_54086) {
         ItemStack var5 = var7 == EquipmentSlot.field_6173 ? var1 : var0.method_6118(var7);
         if (!var5.method_7960() && !var5.method_61657()) {
            var5.method_57354(var7, (var1x, var2x) -> include(var2, var1x, var2x));
         }
      }

      return (float)var2.method_6194() > 0.0F;
   }

   private static void include(EntityAttributeInstance var0, RegistryEntry<EntityAttribute> var1, EntityAttributeModifier var2) {
      if (EntityAttributes.field_23721.equals(var1)) {
         var0.method_6200(var2.comp_2447());
         var0.method_26835(var2);
      }
   }

   public static int confirmWindow(MinecraftClient var0, ClientPlayerEntity var1) {
      return MathHelper.method_15340(rttTicks(var0, var1) + 2, 4, 20);
   }

   public static int rttTicks(MinecraftClient var0, ClientPlayerEntity var1) {
      return measuredRtt > 0.0F ? Math.round(measuredRtt) : latencyMs(var0, var1) / 50 + 1;
   }

   public static void notePlace(ClientPlayerEntity var0, BlockPos var1) {
      ObjectIterator var2 = PLACED_AT.long2IntEntrySet().iterator();

      while (var2.hasNext()) {
         int var3 = var0.field_6012 - ((it.unimi.dsi.fastutil.longs.Long2IntMap.Entry)var2.next()).getIntValue();
         if (var3 < 0 || var3 > 20) {
            var2.remove();
         }
      }

      PLACED_AT.put(var1.method_10063(), var0.field_6012);
   }

   public static void noteSpawn(ClientPlayerEntity var0, BlockPos var1) {
      long var2 = var1.method_10063();
      if (PLACED_AT.containsKey(var2)) {
         int var4 = var0.field_6012 - PLACED_AT.remove(var2);
         if (var4 > 0 && var4 <= 20) {
            measuredRtt = measuredRtt > 0.0F ? measuredRtt + (var4 - measuredRtt) * 0.25F : var4;
         }
      }
   }

   public static boolean placing(ClientPlayerEntity var0, BlockPos var1, int var2) {
      long var3 = var1.method_10063();
      return PLACED_AT.containsKey(var3) && sent(var0, PLACED_AT.get(var3), var2);
   }

   public static boolean anyPlacing(ClientPlayerEntity var0, int var1) {
      IntIterator var2 = PLACED_AT.values().iterator();

      while (var2.hasNext()) {
         int var3 = var2.next();
         if (sent(var0, var3, var1)) {
            return true;
         }
      }

      return false;
   }

   public static List<Box> reservedBoxes(ClientPlayerEntity var0, int var1) {
      ArrayList var2 = new ArrayList();

      for (EndCrystalEntity var4 : var0.method_73183().method_18467(EndCrystalEntity.class, var0.method_5829().method_1014(12.0))) {
         if (var4.method_5805()) {
            var2.add(var4.method_5829());
         }
      }

      ObjectIterator var5 = PLACED_AT.long2IntEntrySet().iterator();

      while (var5.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2IntMap.Entry var6 = (it.unimi.dsi.fastutil.longs.Long2IntMap.Entry)var5.next();
         if (sent(var0, var6.getIntValue(), var1)) {
            var2.add(boxOn(BlockPos.method_10092(var6.getLongKey())));
         }
      }

      return var2;
   }

   private static boolean sent(ClientPlayerEntity var0, int var1, int var2) {
      int var3 = var0.field_6012 - var1;
      return var3 >= 0 && var3 < var2;
   }

   public static int humanGap() {
      double var0 = ThreadLocalRandom.current().nextDouble();
      return var0 < 0.7 ? 2 : (var0 < 0.95 ? 3 : 4);
   }

   public static void forgetRtt() {
      PLACED_AT.clear();
      measuredRtt = 0.0F;
   }

   private static int latencyMs(MinecraftClient var0, ClientPlayerEntity var1) {
      ClientPlayNetworkHandler var2 = var0.method_1562();
      if (var2 == null) {
         return 0;
      } else {
         PlayerListEntry var3 = var2.method_2871(var1.method_5667());
         return var3 == null ? 0 : Math.max(var3.method_2959(), 0);
      }
   }

   public static boolean isKeyDown(MinecraftClient var0, int var1) {
      if (!ModuleKeybinds.isBindable(var1)) {
         return false;
      } else {
         return var1 >= 1000
            ? GLFW.glfwGetMouseButton(var0.method_22683().method_4490(), var1 - 1000) == 1
            : EfnKeyPolling.isKeyDown(var0.method_22683(), var1);
      }
   }

   public static boolean isPhysicallyDown(MinecraftClient var0, KeyBinding var1) {
      Key var2;
      try {
         var2 = InputUtil.method_15981(var1.method_1428());
      } catch (IllegalArgumentException var4) {
         return false;
      }

      int var3 = var2.method_1444();
      if (var3 < 0) {
         return false;
      } else {
         return switch (var2.method_1442()) {
            case field_1672 -> GLFW.glfwGetMouseButton(var0.method_22683().method_4490(), var3) == 1;
            case field_1668 -> EfnKeyPolling.isKeyDown(var0.method_22683(), var3);
            default -> false;
         };
      }
   }
}
