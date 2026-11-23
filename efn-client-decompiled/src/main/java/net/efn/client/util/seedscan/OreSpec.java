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
   private static final int[] STEPS = new int[]{Feature.field_13176.ordinal(), Feature.field_13177.ordinal()};

   public int reach() {
      return MathHelper.method_15386((this.size / 16.0F * 2.0F + 1.0F) / 2.0F);
   }

   public int halfWidth() {
      return MathHelper.method_15386(this.size / 8.0F) + this.reach();
   }

   public boolean drawsOnTerrain() {
      return this.discard > 0.0F && this.discard < 1.0F;
   }

   int sampleCount(WgRandom var1) {
      if (this.rarity > 0) {
         return var1.nextFloat() < 1.0F / this.rarity ? 1 : 0;
      } else {
         return this.countMin == this.countMax ? this.countMin : var1.between(this.countMin, this.countMax);
      }
   }

   int sampleY(WgRandom var1) {
      if (this.yMin > this.yMax) {
         return this.yMin;
      } else if (!this.trapezoid) {
         return var1.between(this.yMin, this.yMax);
      } else {
         int var2 = this.yMax - this.yMin;
         if (this.plateau >= var2) {
            return var1.between(this.yMin, this.yMax);
         } else {
            int var3 = (var2 - this.plateau) / 2;
            int var4 = var2 - var3;
            return this.yMin + var1.between(0, var4) + var1.between(0, var3);
         }
      }
   }

   boolean inBiome(RegistryEntry<Biome> var1) {
      return this.biomes == null || this.biomes.contains(var1);
   }

   @Override
   public String toString() {
      return this.id + "(" + this.step + ":" + this.index + ")";
   }

   static List<OreSpec> derive(
      List<IndexedFeatures> var0, List<RegistryEntry<Biome>> var1, ChunkGenerator var2, Function<PlacedFeature, String> var3, int var4, int var5
   ) {
      checkTags();
      ArrayList var6 = new ArrayList();

      for (int var10 : STEPS) {
         if (var10 < var0.size()) {
            List var11 = ((IndexedFeatures)var0.get(var10)).comp_303();

            for (int var12 = 0; var12 < var11.size(); var12++) {
               PlacedFeature var13 = (PlacedFeature)var11.get(var12);
               ConfiguredFeature var14 = (ConfiguredFeature)var13.comp_334().comp_349();
               if (var14.comp_332() == net.minecraft.world.gen.feature.Feature.field_13517) {
                  OreFeatureConfig var15 = (OreFeatureConfig)var14.comp_333();
                  String var16 = (String)var3.apply(var13);
                  Set var17 = Collections.newSetFromMap(new IdentityHashMap());

                  for (RegistryEntry var19 : var1) {
                     if (var2.method_44216(var19).method_38105(var13)) {
                        var17.add(var19);
                     }
                  }

                  var6.add(read(var16, var10, var12, var6.size(), var13, var15, var17.size() == var1.size() ? null : Set.copyOf(var17), var4, var5));
               }
            }
         }
      }

      return List.copyOf(var6);
   }

   private static OreSpec read(
      String var0, int var1, int var2, int var3, PlacedFeature var4, OreFeatureConfig var5, Set<RegistryEntry<Biome>> var6, int var7, int var8
   ) {
      List var11 = var4.comp_335();
      if (var11.size() != 4) {
         throw unsupported(var0, "placement " + var11.size() + " modifiers");
      } else {
         JsonObject var12 = json(var0, (PlacementModifier)var11.get(0));
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
                     throw unsupported(var0, "count " + var17);
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
               throw unsupported(var0, "first modifier " + var12);
         }

         if (!type(json(var0, (PlacementModifier)var11.get(1))).equals("minecraft:in_square")) {
            throw unsupported(var0, "second modifier");
         } else {
            JsonObject var20 = json(var0, (PlacementModifier)var11.get(2));
            if (!type(var20).equals("minecraft:height_range")) {
               throw unsupported(var0, "third modifier");
            } else if (!type(json(var0, (PlacementModifier)var11.get(3))).equals("minecraft:biome")) {
               throw unsupported(var0, "fourth modifier");
            } else {
               JsonObject var21 = var20.getAsJsonObject("height");
               String var23 = type(var21);

               boolean var22 = switch (var23) {
                  case "minecraft:uniform" -> false;
                  case "minecraft:trapezoid" -> true;
                  default -> throw unsupported(var0, "height " + var21);
               };
               int var24 = var21.has("plateau") ? var21.get("plateau").getAsInt() : 0;
               int var25 = anchor(var0, var21.getAsJsonObject("min_inclusive"), var7, var8);
               int var19 = anchor(var0, var21.getAsJsonObject("max_inclusive"), var7, var8);
               return new OreSpec(
                  var0, var1, var2, var3, var9, var10, var13, var22, var25, var19, var24, var5.field_13723, var5.field_29064, kindOf(var0, var5), var6
               );
            }
         }
      }
   }

   private static int anchor(String var0, JsonObject var1, int var2, int var3) {
      if (var1.has("absolute")) {
         return var1.get("absolute").getAsInt();
      } else if (var1.has("above_bottom")) {
         return var2 + var1.get("above_bottom").getAsInt();
      } else if (var1.has("below_top")) {
         return var3 - var1.get("below_top").getAsInt();
      } else {
         throw unsupported(var0, "anchor " + var1);
      }
   }

   private static byte kindOf(String var0, OreFeatureConfig var1) {
      HashSet var3 = new HashSet();
      byte var4 = -1;

      for (Target var6 : var1.field_29063) {
         JsonObject var7 = ((JsonElement)RuleTest.field_25012.encodeStart(JsonOps.INSTANCE, var6.field_29068).getOrThrow()).getAsJsonObject();
         if (!var7.get("predicate_type").getAsString().equals("minecraft:tag_match")) {
            throw unsupported(var0, "target " + var7);
         }

         var3.add(var7.get("tag").getAsString());
         byte var8 = VBlock.of(var6.field_29069);
         if (var8 == 12 || var8 == 0 || var4 != -1 && var4 != var8) {
            throw unsupported(var0, "writes " + var6.field_29069);
         }

         var4 = var8;
      }

      boolean var2 = var3.equals(Set.of("minecraft:base_stone_overworld"))
         || var3.equals(Set.of("minecraft:stone_ore_replaceables", "minecraft:deepslate_ore_replaceables"));
      if (var2 && var4 != -1) {
         return var4;
      } else {
         throw unsupported(var0, "targets " + var3);
      }
   }

   static void checkTags() {
      expect(BlockTags.field_25806, Blocks.field_10340, Blocks.field_10474, Blocks.field_10508, Blocks.field_10115, Blocks.field_27165, Blocks.field_28888);
      expect(BlockTags.field_28992, Blocks.field_10340, Blocks.field_10474, Blocks.field_10508, Blocks.field_10115);
      expect(BlockTags.field_28993, Blocks.field_28888, Blocks.field_27165);
   }

   private static void expect(TagKey<Block> var0, Block... var1) {
      Set var2 = Collections.newSetFromMap(new IdentityHashMap());
      Collections.addAll(var2, var1);
      Set var3 = Collections.newSetFromMap(new IdentityHashMap());

      for (RegistryEntry var5 : Registries.field_41175.method_40286(var0)) {
         var3.add((Block)var5.comp_349());
      }

      if (!var3.equals(var2)) {
         throw new IllegalStateException("block tag " + var0 + " is not vanilla's: " + var3);
      }
   }

   private static JsonObject json(String var0, PlacementModifier var1) {
      try {
         return ((JsonElement)PlacementModifier.field_35736.encodeStart(JsonOps.INSTANCE, var1).getOrThrow()).getAsJsonObject();
      } catch (RuntimeException var3) {
         throw unsupported(var0, "modifier " + var1 + ": " + var3.getMessage());
      }
   }

   private static String type(JsonObject var0) {
      JsonElement var1 = var0.get("type");
      return var1 == null ? "" : var1.getAsString();
   }

   private static IllegalStateException unsupported(String var0, String var1) {
      return new IllegalStateException("feature " + var0 + " is not placed like a vanilla ore: " + var1);
   }
}
