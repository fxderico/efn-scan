package net.efn.client.util.seedscan;

import com.mojang.serialization.Lifecycle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.DynamicRegistryManager.ImmutableImpl;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.structure.processor.StructureProcessorLists;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.BuiltinBiomes;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.PaletteProvider;
import net.minecraft.world.chunk.PalettesFactory;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.carver.CarverContext;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.carver.ConfiguredCarvers;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.densityfunction.DensityFunctions;
import net.minecraft.world.gen.feature.ConfiguredFeatures;
import net.minecraft.world.gen.feature.PlacedFeatures;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import net.minecraft.world.gen.noise.BuiltinNoiseParameters;
import net.minecraft.world.gen.noise.NoiseConfig;

@Environment(EnvType.CLIENT)
public final class SeedWorldgen {
   public static final int MIN_Y = -64;
   public static final int HEIGHT = 384;
   public static final int MAX_Y = 319;
   public final long seed;
   final NoiseChunkGenerator generator;
   final NoiseConfig randomState;
   final MultiNoiseBiomeSource biomeSource;
   final ChunkGeneratorSettings settings;
   private final List<OreSpec> specs;
   private final PalettesFactory containers;
   private final StructureAccessor noStructures;
   private final HeightLimitView height = HeightLimitView.create(-64, 384);
   private final long zoomSeed;
   private final Map<Long, GenerationSettings> carverBiomes = new ConcurrentHashMap<>();
   private final Map<Long, short[]> storedBiomes = new ConcurrentHashMap<>();
   private final List<RegistryEntry<Biome>> biomeIds;
   private final Map<RegistryEntry<Biome>, Integer> biomeIndex = new IdentityHashMap<>();
   private static final int MIN_QUART = -16;
   private static final int QUARTS_Y = 96;
   private static final int MAX_STORED_BIOMES = 4096;
   private static final int MAX_CARVER_BIOMES = 65536;
   private final BiomeAccess biomeManager;
   private static final DynamicRegistryManager CARVER_REGISTRIES = new ImmutableImpl(
      List.of(new SimpleRegistry<>(RegistryKeys.BIOME, Lifecycle.stable()).freeze())
   );
   private static final Set<RegistryKey<Biome>> TALL_TREES = Set.of(
      BiomeKeys.JUNGLE,
      BiomeKeys.BAMBOO_JUNGLE,
      BiomeKeys.SPARSE_JUNGLE,
      BiomeKeys.OLD_GROWTH_PINE_TAIGA,
      BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA,
      BiomeKeys.MANGROVE_SWAMP
   );

   private SeedWorldgen(long l) {
      this.seed = l;
      WrapperLookup var3 = new RegistryBuilder()
         .addRegistry(RegistryKeys.CONFIGURED_CARVER, ConfiguredCarvers::bootstrap)
         .addRegistry(RegistryKeys.PROCESSOR_LIST, StructureProcessorLists::bootstrap)
         .addRegistry(RegistryKeys.CONFIGURED_FEATURE, ConfiguredFeatures::bootstrap)
         .addRegistry(RegistryKeys.PLACED_FEATURE, PlacedFeatures::bootstrap)
         .addRegistry(RegistryKeys.BIOME, BuiltinBiomes::bootstrap)
         .addRegistry(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, MultiNoiseBiomeSourceParameterLists::bootstrap)
         .addRegistry(RegistryKeys.NOISE_PARAMETERS, BuiltinNoiseParameters::bootstrap)
         .addRegistry(RegistryKeys.DENSITY_FUNCTION, DensityFunctions::bootstrap)
         .addRegistry(RegistryKeys.CHUNK_GENERATOR_SETTINGS, ChunkGeneratorSettings::bootstrap)
         .createWrapperLookup(DynamicRegistryManager.of(Registries.REGISTRIES));
      Reference var4 = var3.getOrThrow(RegistryKeys.CHUNK_GENERATOR_SETTINGS).getOrThrow(ChunkGeneratorSettings.OVERWORLD);
      this.settings = (ChunkGeneratorSettings)var4.value();
      this.randomState = NoiseConfig.create(this.settings, var3.getOrThrow(RegistryKeys.NOISE_PARAMETERS), l);
      Reference var5 = var3.getOrThrow(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
      this.biomeSource = MultiNoiseBiomeSource.create(var5);
      this.generator = new NoiseChunkGenerator(this.biomeSource, var4);
      List var6 = List.copyOf(this.biomeSource.getBiomes());
      List var7 = PlacedFeatureIndexer.collectIndexedFeatures(var6, registryEntry -> this.generator.getGenerationSettings(registryEntry).getFeatures(), true);
      IdentityHashMap var8 = new IdentityHashMap();
      var3.getOrThrow(RegistryKeys.PLACED_FEATURE)
         .streamEntries()
         .forEach(reference -> var8.put(reference.value(), reference.registryKey().getValue().getPath()));
      this.specs = OreSpec.derive(var7, var6, this.generator, placedFeature -> var8.getOrDefault(placedFeature, "?"), -64, 319);
      Reference var9 = var3.getOrThrow(RegistryKeys.BIOME).getOrThrow(BiomeKeys.PLAINS);
      ArrayList var10 = new ArrayList(var6);
      if (!var10.contains(var9)) {
         var10.add(var9);
      }

      this.biomeIds = List.copyOf(var10);

      for (int var11 = 0; var11 < this.biomeIds.size(); var11++) {
         this.biomeIndex.put(this.biomeIds.get(var11), var11);
      }

      this.containers = new PalettesFactory(
         PaletteProvider.forBlockStates(Block.STATE_IDS), Blocks.AIR.getDefaultState(), null, PaletteProvider.forBiomes(listIds(var10)), var9, null
      );
      this.noStructures = new StructureAccessor(emptyLevel(this.containers, this.height), new GeneratorOptions(l, false, false), null);
      this.zoomSeed = BiomeAccess.hashSeed(l);
      this.biomeManager = new BiomeAccess(this::storedBiome, this.zoomSeed);
   }

   public static SeedWorldgen create(long l) {
      return new SeedWorldgen(l);
   }

   public List<OreSpec> specs() {
      return this.specs;
   }

   public int treeAllowance(int i, int j) {
      for (int var3 = -1; var3 <= 1; var3++) {
         for (int var4 = -1; var4 <= 1; var4++) {
            int var5 = (i + var3 << 4) + 8;
            int var6 = (j + var4 << 4) + 8;

            for (byte var7 = 60; var7 <= 130; var7 += 10) {
               if (this.biomeAt(var5, var7, var6).getKey().filter(TALL_TREES::contains).isPresent()) {
                  return GateHeights.TALL_TREE_ALLOWANCE;
               }
            }
         }
      }

      return GateHeights.TREE_ALLOWANCE;
   }

   public RegistryEntry<Biome> biomeAt(int i, int j, int k) {
      return this.biomeManager.getBiome(new BlockPos(i, j, k));
   }

   RegistryEntry<Biome> storedBiome(int i, int j, int k) {
      int var4 = Math.max(-16, Math.min(j, 79));
      short[] var5 = this.storedBiomes(BiomeCoords.toChunk(i), BiomeCoords.toChunk(k));
      return this.biomeIds.get(var5[((var4 - -16) * 4 + (k & 3)) * 4 + (i & 3)]);
   }

   private short[] storedBiomes(int i, int j) {
      long var3 = ChunkPos.toLong(i, j);
      short[] var5 = this.storedBiomes.get(var3);
      if (var5 != null) {
         return var5;
      } else {
         ProtoChunk var6 = new ProtoChunk(new ChunkPos(i, j), UpgradeData.NO_UPGRADE_DATA, this.height, this.containers, null);
         return this.fillBiomes(var6);
      }
   }

   private short[] fillBiomes(ProtoChunk protoChunk) {
      this.generator.populateBiomes(this.randomState, Blender.getNoBlending(), this.noStructures, protoChunk).join();
      protoChunk.setStatus(ChunkStatus.BIOMES);
      int var2 = BiomeCoords.fromBlock(protoChunk.getPos().getStartX());
      int var3 = BiomeCoords.fromBlock(protoChunk.getPos().getStartZ());
      short[] var4 = new short[1536];

      for (int var5 = 0; var5 < 96; var5++) {
         for (int var6 = 0; var6 < 4; var6++) {
            for (int var7 = 0; var7 < 4; var7++) {
               Integer var8 = this.biomeIndex.get(protoChunk.getBiomeForNoiseGen(var2 + var7, -16 + var5, var3 + var6));
               var4[(var5 * 4 + var6) * 4 + var7] = (short)(var8 == null ? 0 : var8);
            }
         }
      }

      if (this.storedBiomes.size() > 4096) {
         this.storedBiomes.clear();
      }

      this.storedBiomes.put(protoChunk.getPos().toLong(), var4);
      return var4;
   }

   public ProtoChunk generateTerrain(int i, int j) {
      ProtoChunk var3 = new ProtoChunk(new ChunkPos(i, j), UpgradeData.NO_UPGRADE_DATA, this.height, this.containers, null);
      this.fillBiomes(var3);
      this.generator.populateNoise(Blender.getNoBlending(), this.randomState, this.noStructures, var3).join();
      var3.setStatus(ChunkStatus.NOISE);
      this.generator
         .buildSurface(var3, new HeightContext(this.generator, var3), this.randomState, this.noStructures, this.biomeManager, null, Blender.getNoBlending());
      var3.setStatus(ChunkStatus.SURFACE);
      this.carve(var3);
      var3.setStatus(ChunkStatus.CARVERS);
      return var3;
   }

   public TerrainChunk terrain(int i, int j) {
      return TerrainChunk.of(this.generateTerrain(i, j));
   }

   void carve(ProtoChunk protoChunk) {
      if (this.carverBiomes.size() > 65536) {
         this.carverBiomes.clear();
      }

      BiomeAccess var2 = new BiomeAccess((i, j, k) -> this.biomeSource.getBiome(i, j, k, this.randomState.getMultiNoiseSampler()), this.zoomSeed);
      ChunkRandom var3 = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
      ChunkPos var4 = protoChunk.getPos();
      ChunkNoiseSampler var5 = protoChunk.getOrCreateChunkNoiseSampler(chunk -> {
         throw new IllegalStateException("carving before the noise fill");
      });
      AquiferSampler var6 = var5.getAquiferSampler();
      CarverContext var7 = new CarverContext(
         this.generator, CARVER_REGISTRIES, protoChunk.getHeightLimitView(), var5, this.randomState, this.settings.surfaceRule()
      );
      CarvingMask var8 = protoChunk.getOrCreateCarvingMask();

      for (int var9 = -8; var9 <= 8; var9++) {
         for (int var10 = -8; var10 <= 8; var10++) {
            ChunkPos var11 = new ChunkPos(var4.x + var9, var4.z + var10);
            GenerationSettings var12 = this.carverBiomes
               .computeIfAbsent(
                  var11.toLong(),
                  long_ -> this.generator
                     .getGenerationSettings(
                        this.biomeSource
                           .getBiome(
                              BiomeCoords.fromBlock(var11.getStartX()), 0, BiomeCoords.fromBlock(var11.getStartZ()), this.randomState.getMultiNoiseSampler()
                           )
                     )
               );
            int var13 = 0;

            for (RegistryEntry var15 : var12.getCarversForStep()) {
               var3.setCarverSeed(this.seed + var13, var11.x, var11.z);
               if (((ConfiguredCarver)var15.value()).shouldCarve(var3)) {
                  ((ConfiguredCarver)var15.value()).carve(var7, protoChunk, var2::getBiome, var3, var6, var11, var8);
               }

               var13++;
            }
         }
      }
   }

   private static IndexedIterable<RegistryEntry<Biome>> listIds(List<RegistryEntry<Biome>> list) {
      final IdentityHashMap var1 = new IdentityHashMap();

      for (int var2 = 0; var2 < list.size(); var2++) {
         var1.put((RegistryEntry)list.get(var2), var2);
      }

      return new IndexedIterable<RegistryEntry<Biome>>() {
         public int method_10206(RegistryEntry<Biome> registryEntry) {
            Integer var2 = (Integer)var1.get(registryEntry);
            return var2 == null ? -1 : var2;
         }

         public RegistryEntry<Biome> method_10200(int i) {
            return i >= 0 && i < list.size() ? (RegistryEntry)list.get(i) : null;
         }

         @Override
         public int size() {
            return list.size();
         }

         @Override
         public Iterator<RegistryEntry<Biome>> iterator() {
            return list.iterator();
         }
      };
   }

   private static WorldAccess emptyLevel(PalettesFactory palettesFactory, HeightLimitView heightLimitView) {
      ProtoChunk var2 = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.NO_UPGRADE_DATA, heightLimitView, palettesFactory, null);
      InvocationHandler var3 = (object, method, objects) -> {
         // $VF: Couldn't be decompiled
         // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
         // java.lang.RuntimeException: invalid constant type: Ljava/lang/Object; with value SeedWorldgen.emptyLevel
         //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ConstExprent.toJava(ConstExprent.java:356)
         //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:152)
         //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
         //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
         //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
         //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
         //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
         //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
         //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
         //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
         //   at org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement.toJava(RootStatement.java:36)
         //   at org.jetbrains.java.decompiler.main.ClassWriter.methodLambdaToJava(ClassWriter.java:971)
         //
         // Bytecode:
         // 00: aload 2
         // 01: invokevirtual java/lang/reflect/Method.getDeclaringClass ()Ljava/lang/Class;
         // 04: ldc java/lang/Object
         // 06: if_acmpne 8c
         // 09: aload 2
         // 0a: invokevirtual java/lang/reflect/Method.getName ()Ljava/lang/String;
         // 0d: astore 4
         // 0f: bipush -1
         // 10: istore 5
         // 12: aload 4
         // 14: invokevirtual java/lang/String.hashCode ()I
         // 17: lookupswitch 56 2 -1295482945 25 147696667 42
         // 30: aload 4
         // 32: ldc_w "equals"
         // 35: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
         // 38: ifeq 4f
         // 3b: bipush 0
         // 3c: istore 5
         // 3e: goto 4f
         // 41: aload 4
         // 43: ldc_w "hashCode"
         // 46: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
         // 49: ifeq 4f
         // 4c: bipush 1
         // 4d: istore 5
         // 4f: iload 5
         // 51: lookupswitch 55 2 0 27 1 45
         // 6c: aload 1
         // 6d: aload 3
         // 6e: bipush 0
         // 6f: aaload
         // 70: if_acmpne 77
         // 73: bipush 1
         // 74: goto 78
         // 77: bipush 0
         // 78: invokestatic java/lang/Boolean.valueOf (Z)Ljava/lang/Boolean;
         // 7b: goto 8b
         // 7e: aload 1
         // 7f: invokestatic java/lang/System.identityHashCode (Ljava/lang/Object;)I
         // 82: invokestatic java/lang/Integer.valueOf (I)Ljava/lang/Integer;
         // 85: goto 8b
         // 88: ldc_w "SeedWorldgen.emptyLevel"
         // 8b: areturn
         // 8c: ldc_w net/minecraft/world/chunk/Chunk
         // 8f: aload 2
         // 90: invokevirtual java/lang/reflect/Method.getReturnType ()Ljava/lang/Class;
         // 93: invokevirtual java/lang/Class.isAssignableFrom (Ljava/lang/Class;)Z
         // 96: ifeq a3
         // 99: aload 2
         // 9a: invokevirtual java/lang/reflect/Method.getParameterCount ()I
         // 9d: bipush 4
         // 9e: if_icmpne a3
         // a1: aload 0
         // a2: areturn
         // a3: aload 2
         // a4: invokevirtual java/lang/reflect/Method.isDefault ()Z
         // a7: ifeq b1
         // aa: aload 1
         // ab: aload 2
         // ac: aload 3
         // ad: invokestatic java/lang/reflect/InvocationHandler.invokeDefault (Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;
         // b0: areturn
         // b1: new java/lang/UnsupportedOperationException
         // b4: dup
         // b5: aload 2
         // b6: invokestatic java/lang/String.valueOf (Ljava/lang/Object;)Ljava/lang/String;
         // b9: invokedynamic makeConcatWithConstants (Ljava/lang/String;)Ljava/lang/String; bsm=java/lang/invoke/StringConcatFactory.makeConcatWithConstants (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[ "structure-free level: \u0001" ]
         // be: invokespecial java/lang/UnsupportedOperationException.<init> (Ljava/lang/String;)V
         // c1: athrow
      };
      return (WorldAccess)Proxy.newProxyInstance(WorldAccess.class.getClassLoader(), new Class[]{WorldAccess.class}, var3);
   }
}
