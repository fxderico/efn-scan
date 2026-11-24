package net.efn.client.util.playerbypass2;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Oxidizable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class SimulationSignals {
   private static final int RECENT = 6;
   private static final long SECOND = 1000000000L;
   private static final long CHUNK_SETTLE = 3000000000L;
   private static final long FRESH_ENTITY = 2000000000L;
   private static final long WANDER_MEMORY = 60000000000L;
   private static final long TELEPORT_QUIET = 2000000000L;
   private static final long LIGHTNING_LIFE = 3000000000L;
   private static final long UNDERGROUND_HOLD = 3000000000L;
   private static final long FROZEN_AFTER = 40000000000L;
   private static final int MAX_PINS = 32;
   private static final double MIN_MOVE_SQR = 1.0E-4;
   private static final double TELEPORT_JUMP_SQR = 64.0;
   private static final double SAMPLE_JUMP_SQR = 900.0;
   private static final double TRACKING_INSET = 24.0;
   private static final double TRACKED_DECAY = 0.5;
   private static final float FORGET_SCORE = 0.05F;
   private static final float WEAK_CAP = 4.0F;
   private static final float STRONG_CAP = 10.0F;
   private static final int MIN_Y = 0;
   private static final int BLOCK_SPILL = 1;
   private static final Set<Block> GROWS_FROM_AIR = Set.of(
      Blocks.SUGAR_CANE,
      Blocks.CACTUS,
      Blocks.BAMBOO,
      Blocks.BAMBOO_SAPLING,
      Blocks.MELON,
      Blocks.PUMPKIN,
      Blocks.SNOW,
      Blocks.FIRE,
      Blocks.SOUL_FIRE,
      Blocks.BROWN_MUSHROOM,
      Blocks.RED_MUSHROOM,
      Blocks.CHORUS_PLANT,
      Blocks.CHORUS_FLOWER
   );
   private static final Set<Block> GONE_TO_AIR = Set.of(Blocks.SNOW, Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.TURTLE_EGG, Blocks.SNIFFER_EGG, Blocks.FROGSPAWN);
   private static boolean active;
   private static int serverSim = -1;
   private static int serverView = -1;
   private static int simOverride;
   private static int margin;
   private static boolean ignoreVisiblePlayers = true;
   private static long memoryNanos = 30000000000L;
   private static int ticks;
   private static int session;
   private static RegistryKey<World> dimension;
   private static long quietUntil;
   private static long undergroundUntil;
   private static double lastX;
   private static double lastZ;
   private static double sampleX;
   private static double sampleZ;
   private static boolean hasLast;
   private static boolean hasSample;
   private static long passedCenter = -9223372036854775808L;
   private static int passedRadius = -1;
   private static final Long2ObjectOpenHashMap<SimulationSignals.ChunkSignal> CHUNKS = new Long2ObjectOpenHashMap<>();
   private static final Int2ObjectOpenHashMap<SimulationSignals.Share> ENTITY_SHARES = new Int2ObjectOpenHashMap<>();
   private static final Long2ObjectOpenHashMap<SimulationSignals.Share> BLOCK_SHARES = new Long2ObjectOpenHashMap<>();
   private static final Long2LongOpenHashMap CHUNK_LOADED = new Long2LongOpenHashMap();
   private static final Int2LongOpenHashMap ENTITY_ADDED = new Int2LongOpenHashMap();
   private static final Int2LongOpenHashMap ENTITY_INSIDE = new Int2LongOpenHashMap();
   private static final Map<RegistryKey<World>, LongOpenHashSet> PASSED_BY_DIMENSION = new HashMap<>();
   private static LongOpenHashSet passed = new LongOpenHashSet();
   private static final Int2DoubleOpenHashMap TRACKED_RANGE = new Int2DoubleOpenHashMap();
   private static final Int2ObjectOpenHashMap<long[]> LIGHTNING_PENDING = new Int2ObjectOpenHashMap<>();
   private static final Int2LongOpenHashMap LAST_ACTIVE = new Int2LongOpenHashMap();
   private static final LongOpenHashSet FROZEN = new LongOpenHashSet();
   private static final ArrayDeque<SimulationSignals.Pin> PINS = new ArrayDeque<>();

   private SimulationSignals() {
   }

   public static void configure(boolean bl, int i, int j, boolean bl2, int k) {
      if (active && !bl) {
         clear();
      }

      active = bl;
      simOverride = i;
      margin = j;
      ignoreVisiblePlayers = bl2;
      memoryNanos = Math.max(1, k) * 1000000000L;
   }

   public static void clear() {
      resetLevel();
      PASSED_BY_DIMENSION.clear();
      passed = new LongOpenHashSet();
      if (dimension != null) {
         PASSED_BY_DIMENSION.put(dimension, passed);
      }
   }

   private static void resetLevel() {
      CHUNKS.clear();
      ENTITY_SHARES.clear();
      BLOCK_SHARES.clear();
      ENTITY_ADDED.clear();
      ENTITY_INSIDE.clear();
      TRACKED_RANGE.clear();
      LIGHTNING_PENDING.clear();
      LAST_ACTIVE.clear();
      FROZEN.clear();
      PINS.clear();
      hasLast = false;
      hasSample = false;
      passedCenter = -9223372036854775808L;
      passedRadius = -1;
   }

   public static int session() {
      return session;
   }

   public static int serverView() {
      return serverView;
   }

   public static int simulation() {
      int var0 = simOverride > 0 ? simOverride : (serverSim > 0 ? serverSim : 5);
      return Math.max(1, Math.min(var0, view() - 3 - margin));
   }

   public static int view() {
      int var0 = MinecraftClient.getInstance().options.getClampedViewDistance();
      return serverView > 0 ? Math.min(serverView, var0) : var0;
   }

   public static int ringStart() {
      return simulation() + 2 + margin;
   }

   public static Long2ObjectMap<SimulationSignals.ChunkSignal> chunks() {
      return CHUNKS;
   }

   public static LongOpenHashSet frozen() {
      return FROZEN;
   }

   public static SimulationSignals.Pin pollPin() {
      return PINS.poll();
   }

   public static void dropPins() {
      PINS.clear();
   }

   public static boolean holding() {
      ClientPlayerEntity var0 = MinecraftClient.getInstance().player;
      return now() < undergroundUntil || var0 != null && var0.getY() < 0.0;
   }

   public static long now() {
      return System.nanoTime();
   }

   static float decay(long l) {
      return (float)Math.exp(-l / memoryNanos);
   }

   public static void onLogin(int i, int j, RegistryKey<World> registryKey) {
      serverSim = i;
      serverView = j;
      session++;
      dimension = registryKey;
      CHUNK_LOADED.clear();
      clear();
      startQuiet();
   }

   public static void onRespawn(RegistryKey<World> registryKey) {
      startQuiet();
      if (!registryKey.equals(dimension)) {
         dimension = registryKey;
         CHUNK_LOADED.clear();
         resetLevel();
         passed = PASSED_BY_DIMENSION.computeIfAbsent(registryKey, registryKeyx -> new LongOpenHashSet());
      }
   }

   public static void onTeleport() {
      startQuiet();
   }

   public static void onSimulationDistance(int i) {
      serverSim = i;
   }

   public static void onViewDistance(int i) {
      serverView = i;
   }

   public static void onChunk(int i, int j) {
      if (active) {
         CHUNK_LOADED.put(ChunkPos.toLong(i, j), now());
      }
   }

   public static void onMove(EntityS2CPacket entityS2CPacket) {
      if (active) {
         ClientWorld var2 = MinecraftClient.getInstance().world;
         if (var2 != null) {
            Entity var3 = entityS2CPacket.getEntity(var2);
            if (var3 != null) {
               boolean var1 = entityS2CPacket.isPositionChanged()
                  && (entityS2CPacket.getDeltaX() != 0 || entityS2CPacket.getDeltaY() != 0 || entityS2CPacket.getDeltaZ() != 0);
               if (var1 || entityS2CPacket.hasRotation()) {
                  LAST_ACTIVE.put(var3.getId(), now());
               }

               if (var1) {
                  entitySignal(var3, SimulationSignals.Kind.MOVE, 0);
               } else if (entityS2CPacket.hasRotation()) {
                  entitySignal(var3, SimulationSignals.Kind.HEAD, 0);
               }
            }
         }
      }
   }

   public static void onPositionSync(EntityPositionSyncS2CPacket entityPositionSyncS2CPacket) {
      if (active) {
         ClientWorld var1 = MinecraftClient.getInstance().world;
         if (var1 != null) {
            Entity var2 = var1.getEntityById(entityPositionSyncS2CPacket.id());
            if (var2 instanceof LivingEntity) {
               if (!(var2.getEntityPos().squaredDistanceTo(entityPositionSyncS2CPacket.values().position()) < 1.0E-4)) {
                  LAST_ACTIVE.put(var2.getId(), now());
                  entitySignal(var2, SimulationSignals.Kind.MOVE, 0);
               }
            }
         }
      }
   }

   public static void onHeadRotation(EntitySetHeadYawS2CPacket entitySetHeadYawS2CPacket) {
      if (active) {
         ClientWorld var1 = MinecraftClient.getInstance().world;
         if (var1 != null) {
            Entity var2 = entitySetHeadYawS2CPacket.getEntity(var1);
            if (var2 != null) {
               LAST_ACTIVE.put(var2.getId(), now());
               entitySignal(var2, SimulationSignals.Kind.HEAD, 0);
            }
         }
      }
   }

   public static void onEntityData(EntityTrackerUpdateS2CPacket entityTrackerUpdateS2CPacket) {
      if (active) {
         ClientWorld var1 = MinecraftClient.getInstance().world;
         if (var1 != null) {
            if (now() - ENTITY_ADDED.getOrDefault(entityTrackerUpdateS2CPacket.id(), -4611686018427387904L) >= 2000000000L) {
               Entity var2 = var1.getEntityById(entityTrackerUpdateS2CPacket.id());
               if (var2 != null) {
                  LAST_ACTIVE.put(var2.getId(), now());
                  entitySignal(var2, SimulationSignals.Kind.STATE, 0);
               }
            }
         }
      }
   }

   public static void onEntityEvent(EntityStatusS2CPacket entityStatusS2CPacket) {
      if (active) {
         ClientWorld var1 = MinecraftClient.getInstance().world;
         if (var1 != null) {
            Entity var2 = entityStatusS2CPacket.getEntity(var1);
            if (var2 != null) {
               LAST_ACTIVE.put(var2.getId(), now());
               entitySignal(var2, SimulationSignals.Kind.EVENT, 0);
            }
         }
      }
   }

   public static void onEquipment(EntityEquipmentUpdateS2CPacket entityEquipmentUpdateS2CPacket) {
      if (active) {
         ClientWorld var1 = MinecraftClient.getInstance().world;
         if (var1 != null) {
            if (now() - ENTITY_ADDED.getOrDefault(entityEquipmentUpdateS2CPacket.getEntityId(), -4611686018427387904L) >= 2000000000L) {
               Entity var2 = var1.getEntityById(entityEquipmentUpdateS2CPacket.getEntityId());
               if (var2 != null) {
                  LAST_ACTIVE.put(var2.getId(), now());
                  entitySignal(var2, SimulationSignals.Kind.EQUIP, 0);
               }
            }
         }
      }
   }

   public static void onDamage(EntityDamageS2CPacket entityDamageS2CPacket) {
      if (active) {
         MinecraftClient var1 = MinecraftClient.getInstance();
         ClientWorld var2 = var1.world;
         ClientPlayerEntity var3 = var1.player;
         if (var2 != null && var3 != null) {
            RegistryEntry var4 = entityDamageS2CPacket.sourceType();
            if (!var4.matchesKey(DamageTypes.SWEET_BERRY_BUSH) && !var4.matchesKey(DamageTypes.STALAGMITE) && !var4.matchesKey(DamageTypes.FALLING_STALACTITE)) {
               Entity var5 = var2.getEntityById(entityDamageS2CPacket.entityId());
               if (var5 != null) {
                  int var6 = entityDamageS2CPacket.sourceCauseId();
                  if (var4.matchesKey(DamageTypes.PLAYER_ATTACK) && var6 >= 0 && var6 != var3.getId() && var2.getEntityById(var6) == null) {
                     pin(var5.getBlockPos(), "Hidden Attacker hit " + name(var5.getType()));
                  }

                  LAST_ACTIVE.put(var5.getId(), now());
                  entitySignal(var5, SimulationSignals.Kind.HURT, 0);
               }
            }
         }
      }
   }

   public static void onTakeItem(ItemPickupAnimationS2CPacket itemPickupAnimationS2CPacket) {
      if (active) {
         MinecraftClient var1 = MinecraftClient.getInstance();
         ClientWorld var2 = var1.world;
         ClientPlayerEntity var3 = var1.player;
         if (var2 != null && var3 != null) {
            int var4 = itemPickupAnimationS2CPacket.getCollectorEntityId();
            if (var4 != var3.getId()) {
               Entity var5 = var2.getEntityById(itemPickupAnimationS2CPacket.getEntityId());
               if (var5 != null) {
                  Entity var6 = var2.getEntityById(var4);
                  if (var6 == null) {
                     String var7;
                     if (var5 instanceof ItemEntity var8) {
                        var7 = Registries.ITEM.getId(var8.getStack().getItem()).getPath();
                     } else {
                        var7 = name(var5.getType());
                     }

                     pin(var5.getBlockPos(), "Hidden Pickup " + var7);
                  } else if (!(var6 instanceof PlayerEntity)) {
                     LAST_ACTIVE.put(var6.getId(), now());
                     entitySignal(var6, SimulationSignals.Kind.EVENT, 0);
                  }
               }
            }
         }
      }
   }

   public static void onAddEntity(EntitySpawnS2CPacket entitySpawnS2CPacket) {
      if (active) {
         long var1 = now();
         ENTITY_ADDED.put(entitySpawnS2CPacket.getEntityId(), var1);
         LAST_ACTIVE.put(entitySpawnS2CPacket.getEntityId(), var1);
         EntityType var3 = entitySpawnS2CPacket.getEntityType();
         if (var3 != EntityType.PLAYER) {
            BlockPos var4 = BlockPos.ofFloored(entitySpawnS2CPacket.getX(), entitySpawnS2CPacket.getY(), entitySpawnS2CPacket.getZ());
            if (var3 == EntityType.LIGHTNING_BOLT) {
               LIGHTNING_PENDING.put(entitySpawnS2CPacket.getEntityId(), new long[]{var4.asLong(), var1});
            } else if (spawnable(var3) && !isQuiet()) {
               if (wellInsideTracking(entitySpawnS2CPacket.getX(), entitySpawnS2CPacket.getZ(), var3)) {
                  if (settled(var4)) {
                     int var5 = entitySpawnS2CPacket.getEntityId();
                     signal(var4, SimulationSignals.Kind.SPAWN, spill(var3), () -> entityShare(var5), () -> name(var3));
                  }
               }
            }
         }
      }
   }

   public static void onRemoveEntities(IntList intList) {
      if (active) {
         ClientWorld var1 = MinecraftClient.getInstance().world;
         if (var1 != null) {
            long var2 = now();
            boolean var4 = isQuiet();

            for (int var5 = 0; var5 < intList.size(); var5++) {
               int var7 = intList.getInt(var5);
               long[] var8 = LIGHTNING_PENDING.remove(var7);
               if (var8 != null) {
                  if (!var4 && var2 - var8[1] < 3000000000L) {
                     signal(BlockPos.fromLong(var8[0]), SimulationSignals.Kind.LIGHTNING, 0, () -> entityShare(var7), () -> "lightning_bolt");
                  }
               } else {
                  Entity var6;
                  if (!var4
                     && (var6 = var1.getEntityById(var7)) != null
                     && !(var6 instanceof PlayerEntity)
                     && var2 - ENTITY_ADDED.getOrDefault(var7, -4611686018427387904L) >= 2000000000L
                     && spawnable(var6.getType())
                     && wellInsideTracking(var6.getX(), var6.getZ(), var6.getType())
                     && settled(var6.getBlockPos())) {
                     entitySignal(var6, SimulationSignals.Kind.REMOVE, spill(var6.getType()));
                  }
               }
            }
         }
      }
   }

   public static void onBlockUpdate(BlockUpdateS2CPacket blockUpdateS2CPacket) {
      if (active) {
         block(blockUpdateS2CPacket.getPos(), blockUpdateS2CPacket.getState());
      }
   }

   public static void onSectionUpdate(ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket) {
      if (active) {
         chunkDeltaUpdateS2CPacket.visitUpdates(SimulationSignals::block);
      }
   }

   private static void block(BlockPos blockPos, BlockState blockState) {
      ClientWorld var2 = MinecraftClient.getInstance().world;
      if (var2 != null) {
         BlockState var3 = var2.getBlockState(blockPos);
         if (var3 != blockState) {
            if (!ignored(var3) && !ignored(blockState)) {
               if (settled(blockPos)) {
                  SimulationSignals.Kind var4 = ticked(var3, blockState) ? SimulationSignals.Kind.TICK : SimulationSignals.Kind.BLOCK;
                  signal(blockPos, var4, 1, () -> blockShare(blockPos), () -> describe(var3) + " -> " + describe(blockState));
               }
            }
         }
      }
   }

   static boolean ticked(BlockState blockState, BlockState blockState2) {
      Block var3 = blockState.getBlock();
      Block var2;
      if (var3 == (var2 = blockState2.getBlock())) {
         return true;
      } else if (!blockState.getFluidState().isEmpty() || !blockState2.getFluidState().isEmpty()) {
         return true;
      } else if (blockState.isIn(BlockTags.LEAVES) && blockState2.isAir()) {
         return true;
      } else if (blockState.isAir() && blockState2.isIn(BlockTags.LEAVES) && !blockState2.get(Properties.PERSISTENT, true)) {
         return true;
      } else if (!blockState.isIn(BlockTags.SAPLINGS) || !blockState2.isIn(BlockTags.LOGS) && !blockState2.isIn(BlockTags.LEAVES)) {
         if (blockState.isIn(BlockTags.CROPS) && blockState2.isAir()) {
            return true;
         } else if (var3 != Blocks.DIRT || var2 != Blocks.GRASS_BLOCK && var2 != Blocks.MYCELIUM) {
            if (var3 == Blocks.FARMLAND && var2 == Blocks.DIRT) {
               return true;
            } else if (var3 == Blocks.MELON_STEM && var2 == Blocks.ATTACHED_MELON_STEM) {
               return true;
            } else if (var3 == Blocks.PUMPKIN_STEM && var2 == Blocks.ATTACHED_PUMPKIN_STEM) {
               return true;
            } else if (blockState.isAir() && GROWS_FROM_AIR.contains(var2)) {
               return true;
            } else {
               return blockState2.isAir() && GONE_TO_AIR.contains(var3)
                  ? true
                  : Oxidizable.getIncreasedOxidationBlock(var3).map(block2 -> block2 == var2).orElse(false);
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean ignored(BlockState blockState) {
      return blockState.isOf(Blocks.POINTED_DRIPSTONE)
         || blockState.isOf(Blocks.CAVE_VINES)
         || blockState.isOf(Blocks.CAVE_VINES_PLANT)
         || blockState.isOf(Blocks.SWEET_BERRY_BUSH)
         || blockState.isOf(Blocks.BEEHIVE)
         || blockState.isOf(Blocks.BEE_NEST)
         || blockState.isOf(Blocks.VINE)
         || blockState.isOf(Blocks.WEEPING_VINES)
         || blockState.isOf(Blocks.WEEPING_VINES_PLANT)
         || blockState.isOf(Blocks.TWISTING_VINES)
         || blockState.isOf(Blocks.TWISTING_VINES_PLANT);
   }

   public static void tick() {
      if (active) {
         MinecraftClient var0 = MinecraftClient.getInstance();
         ClientPlayerEntity var1 = var0.player;
         if (var0.world != null && var1 != null) {
            long var2 = now();
            if (var1.getY() < 0.0) {
               undergroundUntil = var2 + 3000000000L;
            }

            double var4 = var1.getX();
            double var6 = var1.getZ();
            if (hasLast && square(var4 - lastX, var6 - lastZ) > 64.0) {
               startQuiet();
            }

            lastX = var4;
            lastZ = var6;
            hasLast = true;
            refreshPassed(var1);
            if (++ticks % 10 == 0) {
               int var8 = var1.getBlockPos().getX() >> 4;
               int var9 = var1.getBlockPos().getZ() >> 4;
               int var10 = ringStart();
               boolean var11 = !isQuiet() && (!hasSample || square(var4 - sampleX, var6 - sampleZ) <= 900.0);
               sampleX = var4;
               sampleZ = var6;
               hasSample = true;
               Int2DoubleOpenHashMap var12 = new Int2DoubleOpenHashMap();
               int var13 = view();
               FROZEN.clear();

               for (Entity var15 : var0.world.getEntities()) {
                  if (!(var15 instanceof PlayerEntity)) {
                     long var19 = LAST_ACTIVE.get(var15.getId());
                     if (var19 == 0L) {
                        LAST_ACTIVE.put(var15.getId(), var2);
                     } else {
                        int var16;
                        int var17;
                        int var18;
                        if (var2 - var19 > 40000000000L
                           && witness(var15)
                           && (var18 = distance(var17 = var15.getBlockPos().getX() >> 4, var16 = var15.getBlockPos().getZ() >> 4, var8, var9)) >= var10
                           && var18 <= var13
                           && !nearVisiblePlayer(var17, var16, var1, 0)) {
                           FROZEN.add(ChunkPos.toLong(var17, var16));
                        }
                     }

                     if (var11 && spawnable(var15.getType()) && !var15.hasPassengers() && !var15.hasVehicle()) {
                        int var21 = var15.getType().getMaxTrackDistance();
                        double var22 = Math.hypot(var15.getX() - var4, var15.getZ() - var6);
                        if (var22 > var12.get(var21)) {
                           var12.put(var21, var22);
                        }
                     }

                     if (distance(var15.getBlockPos().getX() >> 4, var15.getBlockPos().getZ() >> 4, var8, var9) < var10) {
                        ENTITY_INSIDE.put(var15.getId(), var2);
                     }
                  }
               }

               if (var11) {
                  for (int var31 : TRACKED_RANGE.keySet().toIntArray()) {
                     TRACKED_RANGE.put(var31, Math.max(var12.get(var31), TRACKED_RANGE.get(var31) - 0.5));
                  }

                  for (Entry var28 : var12.int2DoubleEntrySet()) {
                     if (var28.getDoubleValue() > TRACKED_RANGE.get(var28.getIntKey())) {
                        TRACKED_RANGE.put(var28.getIntKey(), var28.getDoubleValue());
                     }
                  }
               }

               if (ticks % 200 == 0) {
                  ObjectIterator var26 = CHUNKS.long2ObjectEntrySet().iterator();

                  while (var26.hasNext()) {
                     if (((SimulationSignals.ChunkSignal)((it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry)var26.next()).getValue()).score(var2) < 0.05F) {
                        var26.remove();
                     }
                  }

                  ENTITY_SHARES.values().removeIf(share -> share.amount(var2) < 0.05F);
                  BLOCK_SHARES.values().removeIf(share -> share.amount(var2) < 0.05F);
                  int var29 = view() + 4;
                  CHUNK_LOADED.long2LongEntrySet()
                     .removeIf(entry -> distance(ChunkPos.getPackedX(entry.getLongKey()), ChunkPos.getPackedZ(entry.getLongKey()), var8, var9) > var29);
                  ENTITY_ADDED.int2LongEntrySet().removeIf(entry -> var2 - entry.getLongValue() > 60000000000L);
                  ENTITY_INSIDE.int2LongEntrySet().removeIf(entry -> var2 - entry.getLongValue() > 60000000000L);
                  LIGHTNING_PENDING.values().removeIf(ls -> var2 - ls[1] > 3000000000L);
                  LAST_ACTIVE.keySet().removeIf(i -> var0.world.getEntityById(i) == null);
               }
            }
         }
      }
   }

   private static boolean witness(Entity entity) {
      PathAwareEntity var1;
      return entity instanceof PathAwareEntity
         && !((var1 = (PathAwareEntity)entity) instanceof ShulkerEntity)
         && !var1.isAiDisabled()
         && !var1.hasVehicle()
         && !var1.hasPassengers()
         && !var1.isSleeping();
   }

   private static void pin(BlockPos blockPos, String string) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var2 != null && !holding() && blockPos.getY() >= 0) {
         if (PINS.size() >= 32) {
            PINS.poll();
         }

         PINS.add(new SimulationSignals.Pin(blockPos.toImmutable(), string + " @ " + blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ()));
      }
   }

   private static void refreshPassed(ClientPlayerEntity clientPlayerEntity) {
      int var1 = clientPlayerEntity.getBlockPos().getX() >> 4;
      int var2 = clientPlayerEntity.getBlockPos().getZ() >> 4;
      long var3 = ChunkPos.toLong(var1, var2);
      int var5 = ringStart() - 1 + 1;
      if (var3 != passedCenter || var5 != passedRadius) {
         passedCenter = var3;
         passedRadius = var5;

         for (int var6 = -var5; var6 <= var5; var6++) {
            for (int var7 = -var5; var7 <= var5; var7++) {
               long var8 = ChunkPos.toLong(var1 + var6, var2 + var7);
               if (passed.add(var8)) {
                  CHUNKS.remove(var8);
               }
            }
         }
      }
   }

   private static void entitySignal(Entity entity, SimulationSignals.Kind kind, int i) {
      if (!(entity instanceof PlayerEntity)) {
         if (!(entity.getFirstPassenger() instanceof PlayerEntity)) {
            FallingBlockEntity var5;
            if (!(entity instanceof FallingBlockEntity) || !(var5 = (FallingBlockEntity)entity).getBlockState().isOf(Blocks.POINTED_DRIPSTONE)) {
               ClientPlayerEntity var6 = MinecraftClient.getInstance().player;
               if (var6 != null) {
                  Tameable var3;
                  LazyEntityReference var4;
                  if (!(entity instanceof Tameable) || (var4 = (var3 = (Tameable)entity).getOwnerReference()) == null || !var6.getUuid().equals(var4.getUuid())
                     )
                   {
                     if (now() - ENTITY_INSIDE.getOrDefault(entity.getId(), -4611686018427387904L) >= 60000000000L) {
                        signal(entity.getBlockPos(), kind, i, () -> entityShare(entity.getId()), () -> name(entity.getType()));
                     }
                  }
               }
            }
         }
      }
   }

   private static SimulationSignals.Share entityShare(int i) {
      return ENTITY_SHARES.computeIfAbsent(i, ix -> new SimulationSignals.Share());
   }

   private static SimulationSignals.Share blockShare(BlockPos blockPos) {
      return BLOCK_SHARES.computeIfAbsent(blockPos.asLong(), l -> new SimulationSignals.Share());
   }

   private static String name(EntityType<?> entityType) {
      return Registries.ENTITY_TYPE.getId(entityType).getPath();
   }

   private static String describe(BlockState blockState) {
      return blockState.toString().replace("Block{minecraft:", "").replace("}", "");
   }

   private static void signal(BlockPos blockPos, SimulationSignals.Kind kind, int i, Supplier<SimulationSignals.Share> supplier, Supplier<String> supplier2) {
      ClientPlayerEntity var8 = MinecraftClient.getInstance().player;
      if (var8 != null) {
         if (blockPos.getY() >= 0 && !holding()) {
            int var9 = blockPos.getX() >> 4;
            int var7;
            int var10 = distance(var9, var7 = blockPos.getZ() >> 4, var8.getBlockPos().getX() >> 4, var8.getBlockPos().getZ() >> 4);
            if (var10 >= ringStart() + i && var10 <= view()) {
               long var11 = ChunkPos.toLong(var9, var7);
               if (!passed.contains(var11)) {
                  if (!ignoreVisiblePlayers || !nearVisiblePlayer(var9, var7, var8, i)) {
                     long var13 = now();
                     float var15 = ((SimulationSignals.Share)supplier.get()).take(kind.weight, kind.cap, var13);
                     if (!(var15 <= 0.0F)) {
                        String var16 = kind.title + " " + (String)supplier2.get() + " @ " + blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ();
                        CHUNKS.computeIfAbsent(var11, l -> new SimulationSignals.ChunkSignal()).add(kind, var13, var15, var16);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean nearVisiblePlayer(int i, int j, ClientPlayerEntity clientPlayerEntity, int k) {
      ClientWorld var4 = MinecraftClient.getInstance().world;
      if (var4 == null) {
         return false;
      } else {
         int var5 = simulation() + 1 + k;

         for (PlayerEntity var7 : var4.getPlayers()) {
            if (var7 != clientPlayerEntity && distance(i, j, var7.getBlockPos().getX() >> 4, var7.getBlockPos().getZ() >> 4) <= var5) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean settled(BlockPos blockPos) {
      long var1 = CHUNK_LOADED.getOrDefault(ChunkPos.toLong(blockPos.getX() >> 4, blockPos.getZ() >> 4), -9223372036854775808L);
      return var1 == -9223372036854775808L || now() - var1 > 3000000000L;
   }

   private static boolean wellInsideTracking(double d, double e, EntityType<?> entityType) {
      ClientPlayerEntity var5 = MinecraftClient.getInstance().player;
      if (var5 == null) {
         return false;
      } else {
         double var6 = Math.min(TRACKED_RANGE.get(entityType.getMaxTrackDistance()), view() * 16.0);
         return Math.hypot(d - var5.getX(), e - var5.getZ()) < var6 - 24.0;
      }
   }

   private static boolean spawnable(EntityType<?> entityType) {
      return entityType.getSpawnGroup() == SpawnGroup.MONSTER || entityType == EntityType.ITEM || entityType == EntityType.EXPERIENCE_ORB;
   }

   private static int spill(EntityType<?> entityType) {
      return entityType != EntityType.ITEM && entityType != EntityType.EXPERIENCE_ORB ? 0 : 1;
   }

   private static void startQuiet() {
      quietUntil = now() + 2000000000L;
   }

   private static boolean isQuiet() {
      return now() < quietUntil;
   }

   private static double square(double d, double e) {
      return d * d + e * e;
   }

   public static int distance(int i, int j, int k, int l) {
      return Math.max(Math.abs(i - k), Math.abs(j - l));
   }

   @Environment(EnvType.CLIENT)
   public static final class ChunkSignal {
      private float score;
      private long updated;
      private int kinds;
      private final int[] counts = new int[SimulationSignals.Kind.values().length];
      private final ArrayDeque<String> recent = new ArrayDeque<>();

      public ChunkSignal() {
      }

      public float score(long l) {
         return this.score * SimulationSignals.decay(l - this.updated);
      }

      public boolean has(SimulationSignals.Kind kind) {
         return (this.kinds & 1 << kind.ordinal()) != 0;
      }

      public int count(SimulationSignals.Kind kind) {
         return this.counts[kind.ordinal()];
      }

      public List<String> recent() {
         return List.copyOf(this.recent);
      }

      void add(SimulationSignals.Kind kind, long l, float f, String string) {
         this.recent.addLast(string);
         if (this.recent.size() > 6) {
            this.recent.removeFirst();
         }

         this.score = this.score(l) + f;
         this.updated = l;
         this.kinds = this.kinds | 1 << kind.ordinal();
         int var6 = kind.ordinal();
         this.counts[var6]++;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Kind {
      MOVE("Mob Move", 2.5F, true),
      HEAD("Head Turn", 2.5F, true),
      SPAWN("Spawn / Drop", 2.0F, false),
      REMOVE("Despawn / Pickup", 1.5F, false),
      STATE("Entity State", 0.5F, false),
      EVENT("Entity Event", 5.0F, true),
      BLOCK("Block Change", 1.6F, false),
      TICK("Tick Change", 10.0F, true),
      LIGHTNING("Lightning", 10.0F, true),
      HURT("Hurt", 5.0F, true),
      EQUIP("Equipment", 5.0F, true);

      public final String title;
      final float weight;
      final float cap;

      private Kind(String string2, float f, boolean bl) {
         this.title = string2;
         this.weight = f;
         this.cap = bl ? 10.0F : 4.0F;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Pin(BlockPos pos, String detail) {
   }

   @Environment(EnvType.CLIENT)
   private static final class Share {
      private float amount;
      private long updated;

      private Share() {
      }

      float amount(long l) {
         return this.amount * SimulationSignals.decay(l - this.updated);
      }

      float take(float f, float g, long l) {
         float var5 = this.amount(l);
         float var6 = Math.min(f, Math.max(g, f) - var5);
         if (var6 <= 0.0F) {
            return 0.0F;
         } else {
            this.amount = var5 + var6;
            this.updated = l;
            return var6;
         }
      }
   }
}
