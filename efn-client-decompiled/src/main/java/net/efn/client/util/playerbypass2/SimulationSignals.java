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
      Blocks.field_10424,
      Blocks.field_10029,
      Blocks.field_10211,
      Blocks.field_10108,
      Blocks.field_46283,
      Blocks.field_46282,
      Blocks.field_10477,
      Blocks.field_10036,
      Blocks.field_22089,
      Blocks.field_10251,
      Blocks.field_10559,
      Blocks.field_10021,
      Blocks.field_10528
   );
   private static final Set<Block> GONE_TO_AIR = Set.of(
      Blocks.field_10477, Blocks.field_10036, Blocks.field_22089, Blocks.field_10195, Blocks.field_43230, Blocks.field_37575
   );
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
   private static final Long2ObjectOpenHashMap<SimulationSignals.ChunkSignal> CHUNKS = new Long2ObjectOpenHashMap();
   private static final Int2ObjectOpenHashMap<SimulationSignals.Share> ENTITY_SHARES = new Int2ObjectOpenHashMap();
   private static final Long2ObjectOpenHashMap<SimulationSignals.Share> BLOCK_SHARES = new Long2ObjectOpenHashMap();
   private static final Long2LongOpenHashMap CHUNK_LOADED = new Long2LongOpenHashMap();
   private static final Int2LongOpenHashMap ENTITY_ADDED = new Int2LongOpenHashMap();
   private static final Int2LongOpenHashMap ENTITY_INSIDE = new Int2LongOpenHashMap();
   private static final Map<RegistryKey<World>, LongOpenHashSet> PASSED_BY_DIMENSION = new HashMap<>();
   private static LongOpenHashSet passed = new LongOpenHashSet();
   private static final Int2DoubleOpenHashMap TRACKED_RANGE = new Int2DoubleOpenHashMap();
   private static final Int2ObjectOpenHashMap<long[]> LIGHTNING_PENDING = new Int2ObjectOpenHashMap();
   private static final Int2LongOpenHashMap LAST_ACTIVE = new Int2LongOpenHashMap();
   private static final LongOpenHashSet FROZEN = new LongOpenHashSet();
   private static final ArrayDeque<SimulationSignals.Pin> PINS = new ArrayDeque<>();

   private SimulationSignals() {
   }

   public static void configure(boolean var0, int var1, int var2, boolean var3, int var4) {
      if (active && !var0) {
         clear();
      }

      active = var0;
      simOverride = var1;
      margin = var2;
      ignoreVisiblePlayers = var3;
      memoryNanos = Math.max(1, var4) * 1000000000L;
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
      int var0 = MinecraftClient.method_1551().field_1690.method_38521();
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
      ClientPlayerEntity var0 = MinecraftClient.method_1551().field_1724;
      return now() < undergroundUntil || var0 != null && var0.method_23318() < 0.0;
   }

   public static long now() {
      return System.nanoTime();
   }

   static float decay(long var0) {
      return (float)Math.exp(-var0 / memoryNanos);
   }

   public static void onLogin(int var0, int var1, RegistryKey<World> var2) {
      serverSim = var0;
      serverView = var1;
      session++;
      dimension = var2;
      CHUNK_LOADED.clear();
      clear();
      startQuiet();
   }

   public static void onRespawn(RegistryKey<World> var0) {
      startQuiet();
      if (!var0.equals(dimension)) {
         dimension = var0;
         CHUNK_LOADED.clear();
         resetLevel();
         passed = PASSED_BY_DIMENSION.computeIfAbsent(var0, var0x -> new LongOpenHashSet());
      }
   }

   public static void onTeleport() {
      startQuiet();
   }

   public static void onSimulationDistance(int var0) {
      serverSim = var0;
   }

   public static void onViewDistance(int var0) {
      serverView = var0;
   }

   public static void onChunk(int var0, int var1) {
      if (active) {
         CHUNK_LOADED.put(ChunkPos.method_8331(var0, var1), now());
      }
   }

   public static void onMove(EntityS2CPacket var0) {
      if (active) {
         ClientWorld var2 = MinecraftClient.method_1551().field_1687;
         if (var2 != null) {
            Entity var3 = var0.method_11645(var2);
            if (var3 != null) {
               boolean var1 = var0.method_22826() && (var0.method_36150() != 0 || var0.method_36151() != 0 || var0.method_36152() != 0);
               if (var1 || var0.method_11652()) {
                  LAST_ACTIVE.put(var3.method_5628(), now());
               }

               if (var1) {
                  entitySignal(var3, SimulationSignals.Kind.MOVE, 0);
               } else if (var0.method_11652()) {
                  entitySignal(var3, SimulationSignals.Kind.HEAD, 0);
               }
            }
         }
      }
   }

   public static void onPositionSync(EntityPositionSyncS2CPacket var0) {
      if (active) {
         ClientWorld var1 = MinecraftClient.method_1551().field_1687;
         if (var1 != null) {
            Entity var2 = var1.method_8469(var0.comp_3223());
            if (var2 instanceof LivingEntity) {
               if (!(var2.method_73189().method_1025(var0.comp_3224().comp_3148()) < 1.0E-4)) {
                  LAST_ACTIVE.put(var2.method_5628(), now());
                  entitySignal(var2, SimulationSignals.Kind.MOVE, 0);
               }
            }
         }
      }
   }

   public static void onHeadRotation(EntitySetHeadYawS2CPacket var0) {
      if (active) {
         ClientWorld var1 = MinecraftClient.method_1551().field_1687;
         if (var1 != null) {
            Entity var2 = var0.method_11786(var1);
            if (var2 != null) {
               LAST_ACTIVE.put(var2.method_5628(), now());
               entitySignal(var2, SimulationSignals.Kind.HEAD, 0);
            }
         }
      }
   }

   public static void onEntityData(EntityTrackerUpdateS2CPacket var0) {
      if (active) {
         ClientWorld var1 = MinecraftClient.method_1551().field_1687;
         if (var1 != null) {
            if (now() - ENTITY_ADDED.getOrDefault(var0.comp_1127(), -4611686018427387904L) >= 2000000000L) {
               Entity var2 = var1.method_8469(var0.comp_1127());
               if (var2 != null) {
                  LAST_ACTIVE.put(var2.method_5628(), now());
                  entitySignal(var2, SimulationSignals.Kind.STATE, 0);
               }
            }
         }
      }
   }

   public static void onEntityEvent(EntityStatusS2CPacket var0) {
      if (active) {
         ClientWorld var1 = MinecraftClient.method_1551().field_1687;
         if (var1 != null) {
            Entity var2 = var0.method_11469(var1);
            if (var2 != null) {
               LAST_ACTIVE.put(var2.method_5628(), now());
               entitySignal(var2, SimulationSignals.Kind.EVENT, 0);
            }
         }
      }
   }

   public static void onEquipment(EntityEquipmentUpdateS2CPacket var0) {
      if (active) {
         ClientWorld var1 = MinecraftClient.method_1551().field_1687;
         if (var1 != null) {
            if (now() - ENTITY_ADDED.getOrDefault(var0.method_11820(), -4611686018427387904L) >= 2000000000L) {
               Entity var2 = var1.method_8469(var0.method_11820());
               if (var2 != null) {
                  LAST_ACTIVE.put(var2.method_5628(), now());
                  entitySignal(var2, SimulationSignals.Kind.EQUIP, 0);
               }
            }
         }
      }
   }

   public static void onDamage(EntityDamageS2CPacket var0) {
      if (active) {
         MinecraftClient var1 = MinecraftClient.method_1551();
         ClientWorld var2 = var1.field_1687;
         ClientPlayerEntity var3 = var1.field_1724;
         if (var2 != null && var3 != null) {
            RegistryEntry var4 = var0.comp_1268();
            if (!var4.method_40225(DamageTypes.field_42353) && !var4.method_40225(DamageTypes.field_42355) && !var4.method_40225(DamageTypes.field_42358)) {
               Entity var5 = var2.method_8469(var0.comp_1267());
               if (var5 != null) {
                  int var6 = var0.comp_1269();
                  if (var4.method_40225(DamageTypes.field_42320) && var6 >= 0 && var6 != var3.method_5628() && var2.method_8469(var6) == null) {
                     pin(var5.method_24515(), "Hidden Attacker hit " + name(var5.method_5864()));
                  }

                  LAST_ACTIVE.put(var5.method_5628(), now());
                  entitySignal(var5, SimulationSignals.Kind.HURT, 0);
               }
            }
         }
      }
   }

   public static void onTakeItem(ItemPickupAnimationS2CPacket var0) {
      if (active) {
         MinecraftClient var1 = MinecraftClient.method_1551();
         ClientWorld var2 = var1.field_1687;
         ClientPlayerEntity var3 = var1.field_1724;
         if (var2 != null && var3 != null) {
            int var4 = var0.method_11912();
            if (var4 != var3.method_5628()) {
               Entity var5 = var2.method_8469(var0.method_11915());
               if (var5 != null) {
                  Entity var6 = var2.method_8469(var4);
                  if (var6 == null) {
                     String var7;
                     if (var5 instanceof ItemEntity var8) {
                        var7 = Registries.field_41178.method_10221(var8.method_6983().method_7909()).method_12832();
                     } else {
                        var7 = name(var5.method_5864());
                     }

                     pin(var5.method_24515(), "Hidden Pickup " + var7);
                  } else if (!(var6 instanceof PlayerEntity)) {
                     LAST_ACTIVE.put(var6.method_5628(), now());
                     entitySignal(var6, SimulationSignals.Kind.EVENT, 0);
                  }
               }
            }
         }
      }
   }

   public static void onAddEntity(EntitySpawnS2CPacket var0) {
      if (active) {
         long var1 = now();
         ENTITY_ADDED.put(var0.method_11167(), var1);
         LAST_ACTIVE.put(var0.method_11167(), var1);
         EntityType var3 = var0.method_11169();
         if (var3 != EntityType.field_6097) {
            BlockPos var4 = BlockPos.method_49637(var0.method_11175(), var0.method_11174(), var0.method_11176());
            if (var3 == EntityType.field_6112) {
               LIGHTNING_PENDING.put(var0.method_11167(), new long[]{var4.method_10063(), var1});
            } else if (spawnable(var3) && !isQuiet()) {
               if (wellInsideTracking(var0.method_11175(), var0.method_11176(), var3)) {
                  if (settled(var4)) {
                     int var5 = var0.method_11167();
                     signal(var4, SimulationSignals.Kind.SPAWN, spill(var3), () -> entityShare(var5), () -> name(var3));
                  }
               }
            }
         }
      }
   }

   public static void onRemoveEntities(IntList var0) {
      if (active) {
         ClientWorld var1 = MinecraftClient.method_1551().field_1687;
         if (var1 != null) {
            long var2 = now();
            boolean var4 = isQuiet();

            for (int var5 = 0; var5 < var0.size(); var5++) {
               int var7 = var0.getInt(var5);
               long[] var8 = (long[])LIGHTNING_PENDING.remove(var7);
               if (var8 != null) {
                  if (!var4 && var2 - var8[1] < 3000000000L) {
                     signal(BlockPos.method_10092(var8[0]), SimulationSignals.Kind.LIGHTNING, 0, () -> entityShare(var7), () -> "lightning_bolt");
                  }
               } else {
                  Entity var6;
                  if (!var4
                     && (var6 = var1.method_8469(var7)) != null
                     && !(var6 instanceof PlayerEntity)
                     && var2 - ENTITY_ADDED.getOrDefault(var7, -4611686018427387904L) >= 2000000000L
                     && spawnable(var6.method_5864())
                     && wellInsideTracking(var6.method_23317(), var6.method_23321(), var6.method_5864())
                     && settled(var6.method_24515())) {
                     entitySignal(var6, SimulationSignals.Kind.REMOVE, spill(var6.method_5864()));
                  }
               }
            }
         }
      }
   }

   public static void onBlockUpdate(BlockUpdateS2CPacket var0) {
      if (active) {
         block(var0.method_11309(), var0.method_11308());
      }
   }

   public static void onSectionUpdate(ChunkDeltaUpdateS2CPacket var0) {
      if (active) {
         var0.method_30621(SimulationSignals::block);
      }
   }

   private static void block(BlockPos var0, BlockState var1) {
      ClientWorld var2 = MinecraftClient.method_1551().field_1687;
      if (var2 != null) {
         BlockState var3 = var2.method_8320(var0);
         if (var3 != var1) {
            if (!ignored(var3) && !ignored(var1)) {
               if (settled(var0)) {
                  SimulationSignals.Kind var4 = ticked(var3, var1) ? SimulationSignals.Kind.TICK : SimulationSignals.Kind.BLOCK;
                  signal(var0, var4, 1, () -> blockShare(var0), () -> describe(var3) + " -> " + describe(var1));
               }
            }
         }
      }
   }

   static boolean ticked(BlockState var0, BlockState var1) {
      Block var3 = var0.method_26204();
      Block var2;
      if (var3 == (var2 = var1.method_26204())) {
         return true;
      } else if (!var0.method_26227().method_15769() || !var1.method_26227().method_15769()) {
         return true;
      } else if (var0.method_26164(BlockTags.field_15503) && var1.method_26215()) {
         return true;
      } else if (var0.method_26215() && var1.method_26164(BlockTags.field_15503) && !(Boolean)var1.method_61767(Properties.field_12514, true)) {
         return true;
      } else if (!var0.method_26164(BlockTags.field_15462) || !var1.method_26164(BlockTags.field_15475) && !var1.method_26164(BlockTags.field_15503)) {
         if (var0.method_26164(BlockTags.field_20341) && var1.method_26215()) {
            return true;
         } else if (var3 != Blocks.field_10566 || var2 != Blocks.field_10219 && var2 != Blocks.field_10402) {
            if (var3 == Blocks.field_10362 && var2 == Blocks.field_10566) {
               return true;
            } else if (var3 == Blocks.field_46287 && var2 == Blocks.field_46285) {
               return true;
            } else if (var3 == Blocks.field_46286 && var2 == Blocks.field_46284) {
               return true;
            } else if (var0.method_26215() && GROWS_FROM_AIR.contains(var2)) {
               return true;
            } else {
               return var1.method_26215() && GONE_TO_AIR.contains(var3) ? true : Oxidizable.method_34737(var3).map(var1x -> var1x == var2).orElse(false);
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean ignored(BlockState var0) {
      return var0.method_27852(Blocks.field_28048)
         || var0.method_27852(Blocks.field_28675)
         || var0.method_27852(Blocks.field_28676)
         || var0.method_27852(Blocks.field_16999)
         || var0.method_27852(Blocks.field_20422)
         || var0.method_27852(Blocks.field_20421)
         || var0.method_27852(Blocks.field_10597)
         || var0.method_27852(Blocks.field_22123)
         || var0.method_27852(Blocks.field_22124)
         || var0.method_27852(Blocks.field_23078)
         || var0.method_27852(Blocks.field_23079);
   }

   public static void tick() {
      if (active) {
         MinecraftClient var0 = MinecraftClient.method_1551();
         ClientPlayerEntity var1 = var0.field_1724;
         if (var0.field_1687 != null && var1 != null) {
            long var2 = now();
            if (var1.method_23318() < 0.0) {
               undergroundUntil = var2 + 3000000000L;
            }

            double var4 = var1.method_23317();
            double var6 = var1.method_23321();
            if (hasLast && square(var4 - lastX, var6 - lastZ) > 64.0) {
               startQuiet();
            }

            lastX = var4;
            lastZ = var6;
            hasLast = true;
            refreshPassed(var1);
            if (++ticks % 10 == 0) {
               int var8 = var1.method_24515().method_10263() >> 4;
               int var9 = var1.method_24515().method_10260() >> 4;
               int var10 = ringStart();
               boolean var11 = !isQuiet() && (!hasSample || square(var4 - sampleX, var6 - sampleZ) <= 900.0);
               sampleX = var4;
               sampleZ = var6;
               hasSample = true;
               Int2DoubleOpenHashMap var12 = new Int2DoubleOpenHashMap();
               int var13 = view();
               FROZEN.clear();

               for (Entity var15 : var0.field_1687.method_18112()) {
                  if (!(var15 instanceof PlayerEntity)) {
                     long var19 = LAST_ACTIVE.get(var15.method_5628());
                     if (var19 == 0L) {
                        LAST_ACTIVE.put(var15.method_5628(), var2);
                     } else {
                        int var16;
                        int var17;
                        int var18;
                        if (var2 - var19 > 40000000000L
                           && witness(var15)
                           && (var18 = distance(var17 = var15.method_24515().method_10263() >> 4, var16 = var15.method_24515().method_10260() >> 4, var8, var9))
                              >= var10
                           && var18 <= var13
                           && !nearVisiblePlayer(var17, var16, var1, 0)) {
                           FROZEN.add(ChunkPos.method_8331(var17, var16));
                        }
                     }

                     if (var11 && spawnable(var15.method_5864()) && !var15.method_5782() && !var15.method_5765()) {
                        int var21 = var15.method_5864().method_18387();
                        double var22 = Math.hypot(var15.method_23317() - var4, var15.method_23321() - var6);
                        if (var22 > var12.get(var21)) {
                           var12.put(var21, var22);
                        }
                     }

                     if (distance(var15.method_24515().method_10263() >> 4, var15.method_24515().method_10260() >> 4, var8, var9) < var10) {
                        ENTITY_INSIDE.put(var15.method_5628(), var2);
                     }
                  }
               }

               if (var11) {
                  for (int var31 : TRACKED_RANGE.keySet().toIntArray()) {
                     TRACKED_RANGE.put(var31, Math.max(var12.get(var31), TRACKED_RANGE.get(var31) - 0.5));
                  }

                  ObjectIterator var25 = var12.int2DoubleEntrySet().iterator();

                  while (var25.hasNext()) {
                     Entry var28 = (Entry)var25.next();
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

                  ENTITY_SHARES.values().removeIf(var2x -> var2x.amount(var2) < 0.05F);
                  BLOCK_SHARES.values().removeIf(var2x -> var2x.amount(var2) < 0.05F);
                  int var29 = view() + 4;
                  CHUNK_LOADED.long2LongEntrySet()
                     .removeIf(var3 -> distance(ChunkPos.method_8325(var3.getLongKey()), ChunkPos.method_8332(var3.getLongKey()), var8, var9) > var29);
                  ENTITY_ADDED.int2LongEntrySet().removeIf(var2x -> var2 - var2x.getLongValue() > 60000000000L);
                  ENTITY_INSIDE.int2LongEntrySet().removeIf(var2x -> var2 - var2x.getLongValue() > 60000000000L);
                  LIGHTNING_PENDING.values().removeIf(var2x -> var2 - var2x[1] > 3000000000L);
                  LAST_ACTIVE.keySet().removeIf(var1x -> var0.field_1687.method_8469(var1x) == null);
               }
            }
         }
      }
   }

   private static boolean witness(Entity var0) {
      PathAwareEntity var1;
      return var0 instanceof PathAwareEntity
         && !((var1 = (PathAwareEntity)var0) instanceof ShulkerEntity)
         && !var1.method_5987()
         && !var1.method_5765()
         && !var1.method_5782()
         && !var1.method_6113();
   }

   private static void pin(BlockPos var0, String var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 != null && !holding() && var0.method_10264() >= 0) {
         if (PINS.size() >= 32) {
            PINS.poll();
         }

         PINS.add(new SimulationSignals.Pin(var0.method_10062(), var1 + " @ " + var0.method_10263() + " " + var0.method_10264() + " " + var0.method_10260()));
      }
   }

   private static void refreshPassed(ClientPlayerEntity var0) {
      int var1 = var0.method_24515().method_10263() >> 4;
      int var2 = var0.method_24515().method_10260() >> 4;
      long var3 = ChunkPos.method_8331(var1, var2);
      int var5 = ringStart() - 1 + 1;
      if (var3 != passedCenter || var5 != passedRadius) {
         passedCenter = var3;
         passedRadius = var5;

         for (int var6 = -var5; var6 <= var5; var6++) {
            for (int var7 = -var5; var7 <= var5; var7++) {
               long var8 = ChunkPos.method_8331(var1 + var6, var2 + var7);
               if (passed.add(var8)) {
                  CHUNKS.remove(var8);
               }
            }
         }
      }
   }

   private static void entitySignal(Entity var0, SimulationSignals.Kind var1, int var2) {
      if (!(var0 instanceof PlayerEntity)) {
         if (!(var0.method_31483() instanceof PlayerEntity)) {
            FallingBlockEntity var5;
            if (!(var0 instanceof FallingBlockEntity) || !(var5 = (FallingBlockEntity)var0).method_6962().method_27852(Blocks.field_28048)) {
               ClientPlayerEntity var6 = MinecraftClient.method_1551().field_1724;
               if (var6 != null) {
                  Tameable var3;
                  LazyEntityReference var4;
                  if (!(var0 instanceof Tameable) || (var4 = (var3 = (Tameable)var0).method_66287()) == null || !var6.method_5667().equals(var4.method_66263())
                     )
                   {
                     if (now() - ENTITY_INSIDE.getOrDefault(var0.method_5628(), -4611686018427387904L) >= 60000000000L) {
                        signal(var0.method_24515(), var1, var2, () -> entityShare(var0.method_5628()), () -> name(var0.method_5864()));
                     }
                  }
               }
            }
         }
      }
   }

   private static SimulationSignals.Share entityShare(int var0) {
      return (SimulationSignals.Share)ENTITY_SHARES.computeIfAbsent(var0, var0x -> new SimulationSignals.Share());
   }

   private static SimulationSignals.Share blockShare(BlockPos var0) {
      return (SimulationSignals.Share)BLOCK_SHARES.computeIfAbsent(var0.method_10063(), var0x -> new SimulationSignals.Share());
   }

   private static String name(EntityType<?> var0) {
      return Registries.field_41177.method_10221(var0).method_12832();
   }

   private static String describe(BlockState var0) {
      return var0.toString().replace("Block{minecraft:", "").replace("}", "");
   }

   private static void signal(BlockPos var0, SimulationSignals.Kind var1, int var2, Supplier<SimulationSignals.Share> var3, Supplier<String> var4) {
      ClientPlayerEntity var8 = MinecraftClient.method_1551().field_1724;
      if (var8 != null) {
         if (var0.method_10264() >= 0 && !holding()) {
            int var9 = var0.method_10263() >> 4;
            int var7;
            int var10 = distance(var9, var7 = var0.method_10260() >> 4, var8.method_24515().method_10263() >> 4, var8.method_24515().method_10260() >> 4);
            if (var10 >= ringStart() + var2 && var10 <= view()) {
               long var11 = ChunkPos.method_8331(var9, var7);
               if (!passed.contains(var11)) {
                  if (!ignoreVisiblePlayers || !nearVisiblePlayer(var9, var7, var8, var2)) {
                     long var13 = now();
                     float var15 = ((SimulationSignals.Share)var3.get()).take(var1.weight, var1.cap, var13);
                     if (!(var15 <= 0.0F)) {
                        String var16 = var1.title
                           + " "
                           + (String)var4.get()
                           + " @ "
                           + var0.method_10263()
                           + " "
                           + var0.method_10264()
                           + " "
                           + var0.method_10260();
                        ((SimulationSignals.ChunkSignal)CHUNKS.computeIfAbsent(var11, var0x -> new SimulationSignals.ChunkSignal()))
                           .add(var1, var13, var15, var16);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean nearVisiblePlayer(int var0, int var1, ClientPlayerEntity var2, int var3) {
      ClientWorld var4 = MinecraftClient.method_1551().field_1687;
      if (var4 == null) {
         return false;
      } else {
         int var5 = simulation() + 1 + var3;

         for (PlayerEntity var7 : var4.method_18456()) {
            if (var7 != var2 && distance(var0, var1, var7.method_24515().method_10263() >> 4, var7.method_24515().method_10260() >> 4) <= var5) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean settled(BlockPos var0) {
      long var1 = CHUNK_LOADED.getOrDefault(ChunkPos.method_8331(var0.method_10263() >> 4, var0.method_10260() >> 4), -9223372036854775808L);
      return var1 == -9223372036854775808L || now() - var1 > 3000000000L;
   }

   private static boolean wellInsideTracking(double var0, double var2, EntityType<?> var4) {
      ClientPlayerEntity var5 = MinecraftClient.method_1551().field_1724;
      if (var5 == null) {
         return false;
      } else {
         double var6 = Math.min(TRACKED_RANGE.get(var4.method_18387()), view() * 16.0);
         return Math.hypot(var0 - var5.method_23317(), var2 - var5.method_23321()) < var6 - 24.0;
      }
   }

   private static boolean spawnable(EntityType<?> var0) {
      return var0.method_5891() == SpawnGroup.field_6302 || var0 == EntityType.field_6052 || var0 == EntityType.field_6044;
   }

   private static int spill(EntityType<?> var0) {
      return var0 != EntityType.field_6052 && var0 != EntityType.field_6044 ? 0 : 1;
   }

   private static void startQuiet() {
      quietUntil = now() + 2000000000L;
   }

   private static boolean isQuiet() {
      return now() < quietUntil;
   }

   private static double square(double var0, double var2) {
      return var0 * var0 + var2 * var2;
   }

   public static int distance(int var0, int var1, int var2, int var3) {
      return Math.max(Math.abs(var0 - var2), Math.abs(var1 - var3));
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

      public float score(long var1) {
         return this.score * SimulationSignals.decay(var1 - this.updated);
      }

      public boolean has(SimulationSignals.Kind var1) {
         return (this.kinds & 1 << var1.ordinal()) != 0;
      }

      public int count(SimulationSignals.Kind var1) {
         return this.counts[var1.ordinal()];
      }

      public List<String> recent() {
         return List.copyOf(this.recent);
      }

      void add(SimulationSignals.Kind var1, long var2, float var4, String var5) {
         this.recent.addLast(var5);
         if (this.recent.size() > 6) {
            this.recent.removeFirst();
         }

         this.score = this.score(var2) + var4;
         this.updated = var2;
         this.kinds = this.kinds | 1 << var1.ordinal();
         int var6 = var1.ordinal();
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

      private Kind(String nullxx, float nullxxx, boolean nullxxxx) {
         this.title = nullxx;
         this.weight = nullxxx;
         this.cap = nullxxxx ? 10.0F : 4.0F;
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

      float amount(long var1) {
         return this.amount * SimulationSignals.decay(var1 - this.updated);
      }

      float take(float var1, float var2, long var3) {
         float var5 = this.amount(var3);
         float var6 = Math.min(var1, Math.max(var2, var1) - var5);
         if (var6 <= 0.0F) {
            return 0.0F;
         } else {
            this.amount = var5 + var6;
            this.updated = var3;
            return var6;
         }
      }
   }
}
