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
import net.minecraft.world.biome.source.BiomeSource;
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
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.densityfunction.DensityFunctions;
import net.minecraft.world.gen.feature.ConfiguredFeatures;
import net.minecraft.world.gen.feature.PlacedFeature;
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
   private final HeightLimitView height = HeightLimitView.method_39034(-64, 384);
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
      List.of(new SimpleRegistry(RegistryKeys.field_41236, Lifecycle.stable()).method_40276())
   );
   private static final Set<RegistryKey<Biome>> TALL_TREES = Set.of(
      BiomeKeys.field_9417, BiomeKeys.field_9440, BiomeKeys.field_35118, BiomeKeys.field_35119, BiomeKeys.field_35113, BiomeKeys.field_38748
   );

   private SeedWorldgen(long var1) {
      this.seed = var1;
      WrapperLookup var3 = new RegistryBuilder()
         .method_46777(RegistryKeys.field_41238, ConfiguredCarvers::method_46843)
         .method_46777(RegistryKeys.field_41247, StructureProcessorLists::method_46846)
         .method_46777(RegistryKeys.field_41239, ConfiguredFeatures::method_39702)
         .method_46777(RegistryKeys.field_41245, PlacedFeatures::method_39735)
         .method_46777(RegistryKeys.field_41236, BuiltinBiomes::method_40363)
         .method_46777(RegistryKeys.field_43089, MultiNoiseBiomeSourceParameterLists::method_49522)
         .method_46777(RegistryKeys.field_41244, BuiltinNoiseParameters::method_39216)
         .method_46777(RegistryKeys.field_41240, DensityFunctions::method_41100)
         .method_46777(RegistryKeys.field_41243, ChunkGeneratorSettings::method_31111)
         .method_46780(DynamicRegistryManager.method_40302(Registries.field_41167));
      Reference var4 = var3.method_46762(RegistryKeys.field_41243).method_46747(ChunkGeneratorSettings.field_26355);
      this.settings = (ChunkGeneratorSettings)var4.comp_349();
      this.randomState = NoiseConfig.method_41556(this.settings, var3.method_46762(RegistryKeys.field_41244), var1);
      Reference var5 = var3.method_46762(RegistryKeys.field_43089).method_46747(MultiNoiseBiomeSourceParameterLists.field_42992);
      this.biomeSource = MultiNoiseBiomeSource.method_49503(var5);
      this.generator = new NoiseChunkGenerator((BiomeSource)this.biomeSource, var4);
      List var6 = List.copyOf(this.biomeSource.method_28443());
      List var7 = PlacedFeatureIndexer.method_44210(var6, var1x -> this.generator.method_44216(var1x).method_30983(), true);
      IdentityHashMap var8 = new IdentityHashMap();
      var3.method_46762(RegistryKeys.field_41245)
         .method_42017()
         .forEach(var1x -> var8.put((PlacedFeature)var1x.comp_349(), var1x.method_40237().method_29177().method_12832()));
      this.specs = OreSpec.derive(var7, var6, (ChunkGenerator)this.generator, var1x -> var8.getOrDefault(var1x, "?"), -64, 319);
      Reference var9 = var3.method_46762(RegistryKeys.field_41236).method_46747(BiomeKeys.field_9451);
      ArrayList var10 = new ArrayList(var6);
      if (!var10.contains(var9)) {
         var10.add(var9);
      }

      this.biomeIds = List.copyOf(var10);

      for (int var11 = 0; var11 < this.biomeIds.size(); var11++) {
         this.biomeIndex.put(this.biomeIds.get(var11), var11);
      }

      this.containers = new PalettesFactory(
         PaletteProvider.method_74162(Block.field_10651), Blocks.field_10124.method_9564(), null, PaletteProvider.method_74165(listIds(var10)), var9, null
      );
      this.noStructures = new StructureAccessor(emptyLevel(this.containers, this.height), new GeneratorOptions(var1, false, false), null);
      this.zoomSeed = BiomeAccess.method_27984(var1);
      this.biomeManager = new BiomeAccess(this::storedBiome, this.zoomSeed);
   }

   public static SeedWorldgen create(long var0) {
      return new SeedWorldgen(var0);
   }

   public List<OreSpec> specs() {
      return this.specs;
   }

   public int treeAllowance(int var1, int var2) {
      for (int var3 = -1; var3 <= 1; var3++) {
         for (int var4 = -1; var4 <= 1; var4++) {
            int var5 = (var1 + var3 << 4) + 8;
            int var6 = (var2 + var4 << 4) + 8;

            for (byte var7 = 60; var7 <= 130; var7 += 10) {
               if (this.biomeAt(var5, var7, var6).method_40230().filter(TALL_TREES::contains).isPresent()) {
                  return GateHeights.TALL_TREE_ALLOWANCE;
               }
            }
         }
      }

      return GateHeights.TREE_ALLOWANCE;
   }

   public RegistryEntry<Biome> biomeAt(int var1, int var2, int var3) {
      return this.biomeManager.method_22393(new BlockPos(var1, var2, var3));
   }

   RegistryEntry<Biome> storedBiome(int var1, int var2, int var3) {
      int var4 = Math.max(-16, Math.min(var2, 79));
      short[] var5 = this.storedBiomes(BiomeCoords.method_33103(var1), BiomeCoords.method_33103(var3));
      return this.biomeIds.get(var5[((var4 - -16) * 4 + (var3 & 3)) * 4 + (var1 & 3)]);
   }

   private short[] storedBiomes(int var1, int var2) {
      long var3 = ChunkPos.method_8331(var1, var2);
      short[] var5 = this.storedBiomes.get(var3);
      if (var5 != null) {
         return var5;
      } else {
         ProtoChunk var6 = new ProtoChunk(new ChunkPos(var1, var2), UpgradeData.field_12950, this.height, this.containers, null);
         return this.fillBiomes(var6);
      }
   }

   private short[] fillBiomes(ProtoChunk var1) {
      this.generator.method_38275(this.randomState, Blender.method_39336(), this.noStructures, var1).join();
      var1.method_12308(ChunkStatus.field_12794);
      int var2 = BiomeCoords.method_33100(var1.method_12004().method_8326());
      int var3 = BiomeCoords.method_33100(var1.method_12004().method_8328());
      short[] var4 = new short[1536];

      for (int var5 = 0; var5 < 96; var5++) {
         for (int var6 = 0; var6 < 4; var6++) {
            for (int var7 = 0; var7 < 4; var7++) {
               Integer var8 = this.biomeIndex.get(var1.method_16359(var2 + var7, -16 + var5, var3 + var6));
               var4[(var5 * 4 + var6) * 4 + var7] = (short)(var8 == null ? 0 : var8);
            }
         }
      }

      if (this.storedBiomes.size() > 4096) {
         this.storedBiomes.clear();
      }

      this.storedBiomes.put(var1.method_12004().method_8324(), var4);
      return var4;
   }

   public ProtoChunk generateTerrain(int var1, int var2) {
      ProtoChunk var3 = new ProtoChunk(new ChunkPos(var1, var2), UpgradeData.field_12950, this.height, this.containers, null);
      this.fillBiomes(var3);
      this.generator.method_12088(Blender.method_39336(), this.randomState, this.noStructures, var3).join();
      var3.method_12308(ChunkStatus.field_12804);
      this.generator
         .method_41538(
            var3, new HeightContext((ChunkGenerator)this.generator, var3), this.randomState, this.noStructures, this.biomeManager, null, Blender.method_39336()
         );
      var3.method_12308(ChunkStatus.field_12796);
      this.carve(var3);
      var3.method_12308(ChunkStatus.field_12801);
      return var3;
   }

   public TerrainChunk terrain(int var1, int var2) {
      return TerrainChunk.of(this.generateTerrain(var1, var2));
   }

   void carve(ProtoChunk var1) {
      if (this.carverBiomes.size() > 65536) {
         this.carverBiomes.clear();
      }

      BiomeAccess var2 = new BiomeAccess(
         (var1x, var2x, var3x) -> this.biomeSource.method_38109(var1x, var2x, var3x, this.randomState.method_42371()), this.zoomSeed
      );
      ChunkRandom var3 = new ChunkRandom(new CheckedRandom(RandomSeed.method_39001()));
      ChunkPos var4 = var1.method_12004();
      ChunkNoiseSampler var5 = var1.method_38255(var0 -> {
         throw new IllegalStateException("carving before the noise fill");
      });
      AquiferSampler var6 = var5.method_38354();
      CarverContext var7 = new CarverContext(this.generator, CARVER_REGISTRIES, var1.method_39460(), var5, this.randomState, this.settings.comp_478());
      CarvingMask var8 = var1.method_28510();

      for (int var9 = -8; var9 <= 8; var9++) {
         for (int var10 = -8; var10 <= 8; var10++) {
            ChunkPos var11 = new ChunkPos(var4.field_9181 + var9, var4.field_9180 + var10);
            GenerationSettings var12 = this.carverBiomes
               .computeIfAbsent(
                  var11.method_8324(),
                  var2x -> this.generator
                     .method_44216(
                        this.biomeSource
                           .method_38109(
                              BiomeCoords.method_33100(var11.method_8326()), 0, BiomeCoords.method_33100(var11.method_8328()), this.randomState.method_42371()
                           )
                     )
               );
            int var13 = 0;

            for (RegistryEntry var15 : var12.method_30976()) {
               var3.method_12663(this.seed + var13, var11.field_9181, var11.field_9180);
               if (((ConfiguredCarver)var15.comp_349()).method_12669(var3)) {
                  ((ConfiguredCarver)var15.comp_349()).method_12668(var7, var1, var2::method_22393, var3, var6, var11, var8);
               }

               var13++;
            }
         }
      }
   }

   private static IndexedIterable<RegistryEntry<Biome>> listIds(final List<RegistryEntry<Biome>> var0) {
      final IdentityHashMap var1 = new IdentityHashMap();

      for (int var2 = 0; var2 < var0.size(); var2++) {
         var1.put((RegistryEntry)var0.get(var2), var2);
      }

      return new IndexedIterable<RegistryEntry<Biome>>() {
         public int method_10206(RegistryEntry<Biome> var1x) {
            Integer var2 = (Integer)var1.get(var1x);
            return var2 == null ? -1 : var2;
         }

         public RegistryEntry<Biome> method_10200(int var1x) {
            return var1x >= 0 && var1x < var0.size() ? (RegistryEntry)var0.get(var1x) : null;
         }

         public int method_10204() {
            return var0.size();
         }

         public Iterator<RegistryEntry<Biome>> iterator() {
            return var0.iterator();
         }
      };
   }

   private static WorldAccess emptyLevel(PalettesFactory var0, HeightLimitView var1) {
      ProtoChunk var2 = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.field_12950, var1, var0, null);
      InvocationHandler var3 = (var1x, var2x, var3x) -> {
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
