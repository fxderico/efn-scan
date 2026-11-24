package net.efn.client.util.seedscan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep.Feature;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.OreFeatureConfig.Target;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer.IndexedFeatures;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;

@Environment(EnvType.CLIENT)
public record OreSpec(
   String id,
   int step,
   int index,
   int order,
   int countMin,
   int countMax,
   int rarity,
   boolean trapezoid,
   int yMin,
   int yMax,
   int plateau,
   int size,
   float discard,
   byte kind,
   Set<RegistryEntry<Biome>> biomes
) {
   private static final int[] STEPS = new int[]{Feature.UNDERGROUND_ORES.ordinal(), Feature.UNDERGROUND_DECORATION.ordinal()};

   public int reach() {
      return MathHelper.ceil((this.size / 16.0F * 2.0F + 1.0F) / 2.0F);
   }

   public int halfWidth() {
      return MathHelper.ceil(this.size / 8.0F) + this.reach();
   }

   public boolean drawsOnTerrain() {
      return this.discard > 0.0F && this.discard < 1.0F;
   }

   int sampleCount(WgRandom wgRandom) {
      if (this.rarity > 0) {
         return wgRandom.nextFloat() < 1.0F / this.rarity ? 1 : 0;
      } else {
         return this.countMin == this.countMax ? this.countMin : wgRandom.between(this.countMin, this.countMax);
      }
   }

   int sampleY(WgRandom wgRandom) {
      if (this.yMin > this.yMax) {
         return this.yMin;
      } else if (!this.trapezoid) {
         return wgRandom.between(this.yMin, this.yMax);
      } else {
         int var2 = this.yMax - this.yMin;
         if (this.plateau >= var2) {
            return wgRandom.between(this.yMin, this.yMax);
         } else {
            int var3 = (var2 - this.plateau) / 2;
            int var4 = var2 - var3;
            return this.yMin + wgRandom.between(0, var4) + wgRandom.between(0, var3);
         }
      }
   }

   boolean inBiome(RegistryEntry<Biome> registryEntry) {
      return this.biomes == null || this.biomes.contains(registryEntry);
   }

   @Override
   public String toString() {
      return this.id + "(" + this.step + ":" + this.index + ")";
   }

   static List<OreSpec> derive(
      List<IndexedFeatures> list, List<RegistryEntry<Biome>> list2, ChunkGenerator chunkGenerator, Function<PlacedFeature, String> function, int i, int j
   ) {
      checkTags();
      ArrayList var6 = new ArrayList();

      for (int var10 : STEPS) {
         if (var10 < list.size()) {
            List var11 = ((IndexedFeatures)list.get(var10)).features();

            for (int var12 = 0; var12 < var11.size(); var12++) {
               PlacedFeature var13 = (PlacedFeature)var11.get(var12);
               ConfiguredFeature var14 = var13.feature().value();
               if (var14.feature() == net.minecraft.world.gen.feature.Feature.ORE) {
                  OreFeatureConfig var15 = (OreFeatureConfig)var14.config();
                  String var16 = (String)function.apply(var13);
                  Set var17 = Collections.newSetFromMap(new IdentityHashMap());

                  for (RegistryEntry var19 : list2) {
                     if (chunkGenerator.getGenerationSettings(var19).isFeatureAllowed(var13)) {
                        var17.add(var19);
                     }
                  }

                  var6.add(read(var16, var10, var12, var6.size(), var13, var15, var17.size() == list2.size() ? null : Set.copyOf(var17), i, j));
               }
            }
         }
      }

      return List.copyOf(var6);
   }

   private static OreSpec read(
      String string, int i, int j, int k, PlacedFeature placedFeature, OreFeatureConfig oreFeatureConfig, Set<RegistryEntry<Biome>> set, int l, int m
   ) {
      List var11 = placedFeature.placementModifiers();
      if (var11.size() != 4) {
         throw unsupported(string, "placement " + var11.size() + " modifiers");
      } else {
         JsonObject var12 = json(string, (PlacementModifier)var11.get(0));
         int var13 = 0;
         String var14 = type(var12);
         int var9;
         int var10;
         switch (var14) {
            case "minecraft:count":
               JsonElement var16 = var12.get("count");
               if (var16.isJsonPrimitive()) {
                  var9 = var10 = var16.getAsInt();
               } else {
                  JsonObject var17 = var16.getAsJsonObject();
                  if (!type(var17).equals("minecraft:uniform")) {
                     throw unsupported(string, "count " + var17);
                  }

                  var9 = var17.get("min_inclusive").getAsInt();
                  var10 = var17.get("max_inclusive").getAsInt();
               }
               break;
            case "minecraft:rarity_filter":
               var10 = 1;
               var9 = 1;
               var13 = var12.get("chance").getAsInt();
               break;
            default:
               throw unsupported(string, "first modifier " + var12);
         }

         if (!type(json(string, (PlacementModifier)var11.get(1))).equals("minecraft:in_square")) {
            throw unsupported(string, "second modifier");
         } else {
            JsonObject var20 = json(string, (PlacementModifier)var11.get(2));
            if (!type(var20).equals("minecraft:height_range")) {
               throw unsupported(string, "third modifier");
            } else if (!type(json(string, (PlacementModifier)var11.get(3))).equals("minecraft:biome")) {
               throw unsupported(string, "fourth modifier");
            } else {
               JsonObject var21 = var20.getAsJsonObject("height");
               String var23 = type(var21);

               boolean var22 = switch (var23) {
                  case "minecraft:uniform" -> false;
                  case "minecraft:trapezoid" -> true;
                  default -> throw unsupported(string, "height " + var21);
               };
               int var24 = var21.has("plateau") ? var21.get("plateau").getAsInt() : 0;
               int var25 = anchor(string, var21.getAsJsonObject("min_inclusive"), l, m);
               int var19 = anchor(string, var21.getAsJsonObject("max_inclusive"), l, m);
               return new OreSpec(
                  string,
                  i,
                  j,
                  k,
                  var9,
                  var10,
                  var13,
                  var22,
                  var25,
                  var19,
                  var24,
                  oreFeatureConfig.size,
                  oreFeatureConfig.discardOnAirChance,
                  kindOf(string, oreFeatureConfig),
                  set
               );
            }
         }
      }
   }

   private static int anchor(String string, JsonObject jsonObject, int i, int j) {
      if (jsonObject.has("absolute")) {
         return jsonObject.get("absolute").getAsInt();
      } else if (jsonObject.has("above_bottom")) {
         return i + jsonObject.get("above_bottom").getAsInt();
      } else if (jsonObject.has("below_top")) {
         return j - jsonObject.get("below_top").getAsInt();
      } else {
         throw unsupported(string, "anchor " + jsonObject);
      }
   }

   private static byte kindOf(String string, OreFeatureConfig oreFeatureConfig) {
      HashSet var3 = new HashSet();
      byte var4 = -1;

      for (Target var6 : oreFeatureConfig.targets) {
         JsonObject var7 = RuleTest.TYPE_CODEC.encodeStart(JsonOps.INSTANCE, var6.target).getOrThrow().getAsJsonObject();
         if (!var7.get("predicate_type").getAsString().equals("minecraft:tag_match")) {
            throw unsupported(string, "target " + var7);
         }

         var3.add(var7.get("tag").getAsString());
         byte var8 = VBlock.of(var6.state);
         if (var8 == 12 || var8 == 0 || var4 != -1 && var4 != var8) {
            throw unsupported(string, "writes " + var6.state);
         }

         var4 = var8;
      }

      boolean var2 = var3.equals(Set.of("minecraft:base_stone_overworld"))
         || var3.equals(Set.of("minecraft:stone_ore_replaceables", "minecraft:deepslate_ore_replaceables"));
      if (var2 && var4 != -1) {
         return var4;
      } else {
         throw unsupported(string, "targets " + var3);
      }
   }

   static void checkTags() {
      expect(BlockTags.BASE_STONE_OVERWORLD, Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.TUFF, Blocks.DEEPSLATE);
      expect(BlockTags.STONE_ORE_REPLACEABLES, Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE);
      expect(BlockTags.DEEPSLATE_ORE_REPLACEABLES, Blocks.DEEPSLATE, Blocks.TUFF);
   }

   private static void expect(TagKey<Block> tagKey, Block... blocks) {
      Set var2 = Collections.newSetFromMap(new IdentityHashMap());
      Collections.addAll(var2, blocks);
      Set var3 = Collections.newSetFromMap(new IdentityHashMap());

      for (RegistryEntry var5 : Registries.BLOCK.iterateEntries(tagKey)) {
         var3.add((Block)var5.value());
      }

      if (!var3.equals(var2)) {
         throw new IllegalStateException("block tag " + tagKey + " is not vanilla's: " + var3);
      }
   }

   private static JsonObject json(String string, PlacementModifier placementModifier) {
      try {
         return PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, placementModifier).getOrThrow().getAsJsonObject();
      } catch (RuntimeException var3) {
         throw unsupported(string, "modifier " + placementModifier + ": " + var3.getMessage());
      }
   }

   private static String type(JsonObject jsonObject) {
      JsonElement var1 = jsonObject.get("type");
      return var1 == null ? "" : var1.getAsString();
   }

   private static IllegalStateException unsupported(String string, String string2) {
      return new IllegalStateException("feature " + string + " is not placed like a vanilla ore: " + string2);
   }
}
