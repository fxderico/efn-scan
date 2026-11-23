package net.efn.client.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import imgui.type.ImBoolean;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class BlockColorListSetting extends Setting {
   static final String CHANGES = "changes";
   private static final String VANILLA = "minecraft:";
   private List<BlockColorListSetting.BlockEntry> entries;
   private Supplier<List<BlockColorListSetting.BlockEntry>> entriesSupplier;
   private final Supplier<List<BlockColorListSetting.BlockEntry>> shipped;
   private final float[] defaultColor;
   private final float[] shippedDefaultColor;
   private final boolean colorsHidden;
   private final List<String> defaultOrder;
   private final Map<String, BlockColorListSetting.BlockEntry> byId = new HashMap<>();
   private final ArrayList<BlockColorListSetting.BlockEntry> selection = new ArrayList<>();
   private final ReferenceOpenHashSet<BlockColorListSetting.BlockEntry> selectionSet = new ReferenceOpenHashSet();
   private final List<BlockColorListSetting.BlockEntry> selectionView = Collections.unmodifiableList(this.selection);
   private long selectionVersion = -1L;
   private Map<String, Integer> enabledLookup;
   private Map<String, Integer> tracerLookup;
   private Reference2IntOpenHashMap<Block> enabledByBlock;
   private Reference2IntOpenHashMap<Block> tracerByBlock;
   private long lookupVersion = -1L;
   private long currentVersion = 0L;
   private static final String WALL_BANNER = "_wall_banner";

   public BlockColorListSetting(String var1, List<BlockColorListSetting.BlockEntry> var2, float[] var3) {
      super(var1);
      this.entries = new ArrayList<>(var2);
      List var4 = var2.stream().map(BlockColorListSetting::copyEntry).toList();
      this.shipped = () -> var4;
      this.defaultColor = var3;
      this.shippedDefaultColor = copyColor(var3);
      this.colorsHidden = false;
      this.defaultOrder = List.of();
      this.index();
      this.onChange(this::invalidateLookup);
   }

   public BlockColorListSetting(String var1, Supplier<List<BlockColorListSetting.BlockEntry>> var2, float[] var3) {
      this(var1, var2, var3, false, List.of());
   }

   private BlockColorListSetting(String var1, Supplier<List<BlockColorListSetting.BlockEntry>> var2, float[] var3, boolean var4, List<String> var5) {
      super(var1);
      this.entriesSupplier = var2;
      this.shipped = var2;
      this.defaultColor = var3;
      this.shippedDefaultColor = copyColor(var3);
      this.colorsHidden = var4;
      this.defaultOrder = var5;
      this.onChange(this::invalidateLookup);
   }

   public boolean colorsHidden() {
      return this.colorsHidden;
   }

   private List<BlockColorListSetting.BlockEntry> entriesInternal() {
      if (this.entries == null) {
         this.entries = new ArrayList<>(this.entriesSupplier.get());
         this.entriesSupplier = null;
         this.index();
      }

      return this.entries;
   }

   private void index() {
      for (BlockColorListSetting.BlockEntry var2 : this.entries) {
         this.byId.putIfAbsent(var2.id(), var2);
      }

      for (String var6 : this.defaultOrder) {
         BlockColorListSetting.BlockEntry var3 = this.byId.get(var6);
         if (var3 != null && var3.enabled().get()) {
            this.addToSelection(var3);
         }
      }

      for (BlockColorListSetting.BlockEntry var7 : this.entries) {
         if (var7.enabled().get()) {
            this.addToSelection(var7);
         }
      }

      this.selectionVersion = this.currentVersion;
   }

   public List<BlockColorListSetting.BlockEntry> entries() {
      return Collections.unmodifiableList(this.entriesInternal());
   }

   public BlockColorListSetting.BlockEntry entry(String var1) {
      this.entriesInternal();
      return this.byId.get(var1);
   }

   public float[] defaultColor() {
      return this.defaultColor;
   }

   public void resetColors() {
      this.entriesInternal();

      for (BlockColorListSetting.BlockEntry var2 : this.shipped.get()) {
         BlockColorListSetting.BlockEntry var3 = this.byId.get(var2.id());
         if (var3 != null) {
            System.arraycopy(var2.color(), 0, var3.color(), 0, 4);
            var3.tracerEnabled().set(var2.tracerEnabled().get());
            System.arraycopy(var2.tracerColor(), 0, var3.tracerColor(), 0, 4);
         }
      }

      System.arraycopy(this.shippedDefaultColor, 0, this.defaultColor, 0, 4);
      this.notifyChanged();
   }

   public List<BlockColorListSetting.BlockEntry> selected() {
      List var1 = this.entriesInternal();
      if (this.selectionVersion != this.currentVersion) {
         for (int var2 = this.selection.size() - 1; var2 >= 0; var2--) {
            BlockColorListSetting.BlockEntry var3 = this.selection.get(var2);
            if (!var3.enabled().get()) {
               this.selection.remove(var2);
               this.selectionSet.remove(var3);
            }
         }

         for (BlockColorListSetting.BlockEntry var5 : var1) {
            if (var5.enabled().get()) {
               this.addToSelection(var5);
            }
         }

         this.selectionVersion = this.currentVersion;
      }

      return this.selectionView;
   }

   public boolean select(BlockColorListSetting.BlockEntry var1) {
      this.selected();
      if (!this.addToSelection(var1)) {
         return false;
      } else {
         var1.enabled().set(true);
         this.notifyChanged();
         return true;
      }
   }

   public boolean deselect(BlockColorListSetting.BlockEntry var1) {
      this.selected();
      if (!this.selectionSet.remove(var1)) {
         return false;
      } else {
         for (int var2 = 0; var2 < this.selection.size(); var2++) {
            if (this.selection.get(var2) == var1) {
               this.selection.remove(var2);
               break;
            }
         }

         var1.enabled().set(false);
         this.notifyChanged();
         return true;
      }
   }

   public int selectAll(Iterable<BlockColorListSetting.BlockEntry> var1) {
      this.selected();
      int var2 = 0;

      for (BlockColorListSetting.BlockEntry var4 : var1) {
         if (this.addToSelection(var4)) {
            var4.enabled().set(true);
            var2++;
         }
      }

      if (var2 > 0) {
         this.notifyChanged();
      }

      return var2;
   }

   public int deselectAll() {
      this.selected();
      int var1 = this.selection.size();
      if (var1 == 0) {
         return 0;
      } else {
         for (BlockColorListSetting.BlockEntry var3 : this.selection) {
            var3.enabled().set(false);
         }

         this.selection.clear();
         this.selectionSet.clear();
         this.notifyChanged();
         return var1;
      }
   }

   private boolean addToSelection(BlockColorListSetting.BlockEntry var1) {
      if (!this.selectionSet.add(var1)) {
         return false;
      } else {
         this.selection.add(var1);
         return true;
      }
   }

   public void invalidateLookup() {
      this.currentVersion++;
   }

   private Map<String, Integer> lookup() {
      if (this.enabledLookup == null || this.lookupVersion != this.currentVersion) {
         this.rebuildLookup();
      }

      return this.enabledLookup;
   }

   private void rebuildLookup() {
      HashMap var1 = new HashMap();
      HashMap var2 = new HashMap();
      Reference2IntOpenHashMap var3 = new Reference2IntOpenHashMap();
      Reference2IntOpenHashMap var4 = new Reference2IntOpenHashMap();

      for (BlockColorListSetting.BlockEntry var6 : this.entriesInternal()) {
         if (var6.enabled().get()) {
            BlockColorListSetting.BlockEntry var7 = this.colorSource(var6);
            int var8 = rgbaToArgb(var7.color());
            var1.put(var6.id(), var8);
            Block var9 = resolveBlock(var6.id());
            if (var9 != null) {
               var3.put(var9, var8);
            }

            if (var7.tracerEnabled().get()) {
               int var10 = rgbaToArgb(var7.tracerColor());
               var2.put(var6.id(), var10);
               if (var9 != null) {
                  var4.put(var9, var10);
               }
            }
         }
      }

      this.enabledLookup = var1;
      this.tracerLookup = var2;
      this.enabledByBlock = var3;
      this.tracerByBlock = var4;
      this.lookupVersion = this.currentVersion;
   }

   private BlockColorListSetting.BlockEntry colorSource(BlockColorListSetting.BlockEntry var1) {
      String var2 = var1.id();
      if (!var2.endsWith("_wall_banner")) {
         return var1;
      } else {
         BlockColorListSetting.BlockEntry var3 = this.byId.get(var2.substring(0, var2.length() - "_wall_banner".length()) + "_banner");
         return var3 != null && var3.enabled().get() ? var3 : var1;
      }
   }

   private static Block resolveBlock(String var0) {
      Identifier var1 = Identifier.method_12829(var0);
      return var1 == null ? null : (Block)Registries.field_41175.method_17966(var1).orElse(null);
   }

   private Map<String, Integer> tracerLookup() {
      if (this.tracerLookup == null || this.lookupVersion != this.currentVersion) {
         this.rebuildLookup();
      }

      return this.tracerLookup;
   }

   private Reference2IntOpenHashMap<Block> blockLookup() {
      if (this.enabledByBlock == null || this.lookupVersion != this.currentVersion) {
         this.rebuildLookup();
      }

      return this.enabledByBlock;
   }

   private Reference2IntOpenHashMap<Block> tracerBlockLookup() {
      if (this.tracerByBlock == null || this.lookupVersion != this.currentVersion) {
         this.rebuildLookup();
      }

      return this.tracerByBlock;
   }

   public boolean isEnabled(Block var1) {
      return this.blockLookup().containsKey(var1);
   }

   public Set<Block> enabledBlocks() {
      return Collections.unmodifiableSet(this.blockLookup().keySet());
   }

   public int colorArgb(Block var1) {
      Reference2IntOpenHashMap var2 = this.blockLookup();
      return var2.containsKey(var1) ? var2.getInt(var1) : rgbaToArgb(this.defaultColor);
   }

   public boolean hasAnyEnabled() {
      return !this.lookup().isEmpty();
   }

   public Set<String> enabledIds() {
      return this.lookup().keySet();
   }

   public int tracerColorArgb(Block var1) {
      Reference2IntOpenHashMap var2 = this.tracerBlockLookup();
      return var2.containsKey(var1) ? var2.getInt(var1) : 0;
   }

   public boolean hasAnyTracerEnabled() {
      return !this.tracerLookup().isEmpty();
   }

   @Override
   public JsonElement serialize() {
      JsonObject var1 = new JsonObject();
      var1.add("defaultColor", colorJson(this.defaultColor));
      JsonArray var2 = new JsonArray();

      for (BlockColorListSetting.BlockEntry var4 : this.selected()) {
         var2.add(entryJson(var4));
      }

      for (BlockColorListSetting.BlockEntry var6 : this.entriesInternal()) {
         if (!this.selectionSet.contains(var6)) {
            var2.add(entryJson(var6));
         }
      }

      var1.add("blocks", var2);
      return var1;
   }

   private static JsonObject entryJson(BlockColorListSetting.BlockEntry var0) {
      JsonObject var1 = new JsonObject();
      var1.addProperty("id", var0.id());
      var1.addProperty("enabled", var0.enabled().get());
      var1.add("color", colorJson(var0.color()));
      var1.addProperty("tracerEnabled", var0.tracerEnabled().get());
      var1.add("tracerColor", colorJson(var0.tracerColor()));
      return var1;
   }

   private static JsonArray colorJson(float[] var0) {
      JsonArray var1 = new JsonArray();

      for (float var5 : var0) {
         var1.add(clamp01(var5));
      }

      return var1;
   }

   @Override
   public JsonElement compact(JsonElement var1) {
      JsonElement var2 = this.defaultJson();
      if (var1 != null && var1.isJsonObject() && var2 != null && var2.isJsonObject()) {
         JsonObject var3 = var1.getAsJsonObject();
         JsonObject var4 = var2.getAsJsonObject();
         if (!var3.has("changes") && var3.has("blocks") && var4.has("blocks")) {
            try {
               HashMap var5 = new HashMap();

               for (JsonElement var7 : var4.getAsJsonArray("blocks")) {
                  JsonObject var8 = var7.getAsJsonObject();
                  var5.put(var8.get("id").getAsString(), fields(var8));
               }

               JsonObject var16 = new JsonObject();

               for (JsonElement var19 : var3.getAsJsonArray("blocks")) {
                  if (var19.isJsonObject() && var19.getAsJsonObject().has("id")) {
                     JsonObject var9 = var19.getAsJsonObject();
                     String var10 = var9.get("id").getAsString();
                     String[] var11 = fields(var9);
                     String[] var12 = (String[])var5.get(var10);
                     if (var12 == null || !Arrays.equals((Object[])var11, (Object[])var12)) {
                        boolean var13 = var12 == null || !var11[2].equals(var12[2]) || !var11[3].equals(var12[3]);
                        StringBuilder var14 = new StringBuilder(var11[0]);
                        if (var13 || !var11[1].equals(var12[1])) {
                           var14.append(' ').append(var11[1]);
                        }

                        if (var13) {
                           var14.append(' ').append(var11[2]).append(' ').append(var11[3]);
                        }

                        var16.addProperty(shortId(var10), var14.toString());
                     }
                  }
               }

               JsonObject var18 = new JsonObject();
               String var20 = hexColor(var3.get("defaultColor"));
               if (var20 != null && !var20.equals(hexColor(var4.get("defaultColor")))) {
                  var18.addProperty("defaultColor", var20);
               }

               var18.add("changes", var16);
               return var18;
            } catch (RuntimeException var15) {
               return var1;
            }
         } else {
            return var1;
         }
      } else {
         return var1;
      }
   }

   @Override
   protected void deserializeValue(JsonElement var1) {
      if (var1 != null && var1.isJsonObject()) {
         try {
            JsonObject var2 = var1.getAsJsonObject();
            if (var2.has("changes") && var2.get("changes").isJsonObject()) {
               this.applyChanges(var2);
            } else {
               this.applyFull(var2);
            }
         } catch (Exception var3) {
         }
      }
   }

   private void applyFull(JsonObject var1) {
      if (var1.has("defaultColor")) {
         readColor(var1.getAsJsonArray("defaultColor"), this.defaultColor);
      }

      if (var1.has("blocks")) {
         JsonArray var2 = var1.getAsJsonArray("blocks");
         ArrayList var3 = new ArrayList<>(this.selected());
         this.selection.clear();
         this.selectionSet.clear();

         for (JsonElement var5 : var2) {
            BlockColorListSetting.BlockEntry var6;
            JsonObject var7;
            if (var5.isJsonObject() && (var7 = var5.getAsJsonObject()).has("id") && (var6 = this.byId.get(var7.get("id").getAsString())) != null) {
               if (var7.has("enabled")) {
                  var6.enabled().set(var7.get("enabled").getAsBoolean());
               }

               if (var7.has("color")) {
                  readColor(var7.getAsJsonArray("color"), var6.color());
               }

               if (var7.has("tracerEnabled")) {
                  var6.tracerEnabled().set(var7.get("tracerEnabled").getAsBoolean());
               }

               if (var7.has("tracerColor")) {
                  readColor(var7.getAsJsonArray("tracerColor"), var6.tracerColor());
               }

               if (var6.enabled().get()) {
                  this.addToSelection(var6);
               }
            }
         }

         for (BlockColorListSetting.BlockEntry var9 : var3) {
            if (var9.enabled().get()) {
               this.addToSelection(var9);
            }
         }
      }
   }

   private void applyChanges(JsonObject var1) {
      this.entriesInternal();
      JsonElement var2 = this.defaultJson();
      if (var2 != null && var2.isJsonObject()) {
         this.applyFull(var2.getAsJsonObject());
      }

      if (var1.has("defaultColor")) {
         readHex(var1.get("defaultColor").getAsString(), this.defaultColor);
      }

      for (Entry var4 : var1.getAsJsonObject("changes").entrySet()) {
         BlockColorListSetting.BlockEntry var7 = this.byId.get(fullId((String)var4.getKey()));
         if (var7 != null && ((JsonElement)var4.getValue()).isJsonPrimitive()) {
            String[] var8 = ((JsonElement)var4.getValue()).getAsString().split(" ");

            float[] var5;
            float[] var6;
            try {
               var6 = var8.length >= 2 ? parseHex(var8[1]) : null;
               var5 = var8.length >= 4 ? parseHex(var8[3]) : null;
            } catch (NumberFormatException var10) {
               continue;
            }

            var7.enabled().set("1".equals(var8[0]));
            if (var6 != null) {
               System.arraycopy(var6, 0, var7.color(), 0, 4);
            }

            if (var5 != null) {
               var7.tracerEnabled().set("1".equals(var8[2]));
               System.arraycopy(var5, 0, var7.tracerColor(), 0, 4);
            }

            if (var7.enabled().get()) {
               this.addToSelection(var7);
            }
         }
      }
   }

   private static String[] fields(JsonObject var0) {
      return new String[]{
         flag(var0, "enabled"), String.valueOf(hexColor(var0.get("color"))), flag(var0, "tracerEnabled"), String.valueOf(hexColor(var0.get("tracerColor")))
      };
   }

   private static String flag(JsonObject var0, String var1) {
      return var0.has(var1) && var0.get(var1).getAsBoolean() ? "1" : "0";
   }

   static String hexColor(JsonElement var0) {
      if (var0 != null && var0.isJsonArray()) {
         float[] var1 = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
         readColor(var0.getAsJsonArray(), var1);
         return ColorSetting.hex(rgbaToArgb(var1));
      } else {
         return null;
      }
   }

   static void readHex(String var0, float[] var1) {
      System.arraycopy(parseHex(var0), 0, var1, 0, 4);
   }

   static float[] parseHex(String var0) {
      int var1 = (int)Long.parseLong(var0, 16);
      return new float[]{(var1 >> 16 & 0xFF) / 255.0F, (var1 >> 8 & 0xFF) / 255.0F, (var1 & 0xFF) / 255.0F, (var1 >>> 24) / 255.0F};
   }

   static String shortId(String var0) {
      return var0.startsWith("minecraft:") ? var0.substring("minecraft:".length()) : var0;
   }

   static String fullId(String var0) {
      return var0.indexOf(58) < 0 ? "minecraft:" + var0 : var0;
   }

   private static void readColor(JsonArray var0, float[] var1) {
      for (int var2 = 0; var2 < Math.min(4, var0.size()); var2++) {
         var1[var2] = clamp01(var0.get(var2).getAsFloat());
      }
   }

   public static BlockColorListSetting createDefault() {
      return new BlockColorListSetting("Block Colors", () -> buildEntries(var0 -> false), white());
   }

   public static BlockColorListSetting blockList(String var0, List<String> var1) {
      Set var2 = Set.copyOf(var1);
      return new BlockColorListSetting(var0, () -> buildEntries(var2::contains), white(), true, List.copyOf(var1));
   }

   private static List<BlockColorListSetting.BlockEntry> buildEntries(Predicate<String> var0) {
      ArrayList var1 = new ArrayList();

      for (Block var3 : Registries.field_41175) {
         Identifier var4;
         if (!var3.method_9564().method_26215() && (var4 = Registries.field_41175.method_10221(var3)) != null) {
            String var5 = var4.toString();
            var1.add(entry(var5, prettify(var4.method_12832()), "Blocks", var0.test(var5), 1.0F, 1.0F, 1.0F, 1.0F));
         }
      }

      return var1;
   }

   public static BlockColorListSetting createForBlockEntityDebug() {
      return new BlockColorListSetting("Block Colors", BlockColorListSetting::buildBlockEntityDebugEntries, white());
   }

   private static List<BlockColorListSetting.BlockEntry> buildBlockEntityDebugEntries() {
      ArrayList var0 = new ArrayList();

      for (Block var2 : Registries.field_41175) {
         Identifier var3;
         if (!var2.method_9564().method_26215() && (var3 = Registries.field_41175.method_10221(var2)) != null) {
            String var4 = var3.method_12832();
            int var5 = blockEntityDebugRgb(var4);
            boolean var6 = var5 != -1 || blockEntityDebugPicked(var4);
            var0.add(entry(var3.toString(), prettify(var4), "Blocks", var6, var5 == -1 ? 16777215 : var5, true));
         }
      }

      return var0;
   }

   private static int blockEntityDebugRgb(String var0) {
      if (var0.endsWith("copper_chest")) {
         return 10246912;
      } else if (var0.endsWith("shulker_box")) {
         return 8781982;
      } else {
         return switch (var0) {
            case "chest" -> 10246912;
            case "trapped_chest" -> 13775410;
            case "ender_chest" -> 7667967;
            case "barrel" -> 6572850;
            case "hopper" -> 5263440;
            case "dispenser", "dropper" -> 7887420;
            case "brewing_stand" -> 7874760;
            case "furnace", "blast_furnace", "smoker" -> 6579300;
            case "spawner" -> 9076390;
            case "enchanting_table" -> 13107200;
            case "crafter" -> 3978400;
            case "moving_piston", "piston", "sticky_piston", "piston_head" -> 65280;
            case "lectern" -> 13806160;
            case "jukebox" -> 2017380;
            case "decorated_pot" -> 11822120;
            case "beehive", "bee_nest" -> 15779840;
            case "beacon" -> 61660;
            case "conduit" -> 46320;
            case "comparator" -> 13120050;
            default -> -1;
         };
      }
   }

   private static boolean blockEntityDebugPicked(String var0) {
      if (!var0.contains("sign") && !var0.endsWith("_bed") && !var0.endsWith("_banner") && !var0.endsWith("_skull") && !var0.endsWith("_head")) {
         return switch (var0) {
            case "bell", "campfire", "soul_campfire", "jigsaw", "structure_block", "command_block", "chain_command_block", "repeating_command_block", "end_portal", "end_gateway", "sculk_sensor", "calibrated_sculk_sensor", "sculk_catalyst", "sculk_shrieker", "suspicious_sand", "suspicious_gravel", "trial_spawner", "vault", "chiseled_bookshelf", "daylight_detector" -> true;
            default -> false;
         };
      } else {
         return true;
      }
   }

   private static String prettify(String var0) {
      String[] var1 = var0.split("_");
      StringBuilder var2 = new StringBuilder();

      for (int var3 = 0; var3 < var1.length; var3++) {
         if (!var1[var3].isEmpty()) {
            var2.append(Character.toUpperCase(var1[var3].charAt(0)));
            var2.append(var1[var3].substring(1));
            if (var3 < var1.length - 1) {
               var2.append(" ");
            }
         }
      }

      return var2.toString();
   }

   private static float[] white() {
      return new float[]{1.0F, 1.0F, 1.0F, 1.0F};
   }

   private static BlockColorListSetting.BlockEntry entry(String var0, String var1, String var2, boolean var3, float var4, float var5, float var6, float var7) {
      float[] var8 = new float[]{var4, var5, var6, var7};
      return new BlockColorListSetting.BlockEntry(var0, var1, var2, new ImBoolean(var3), var8, new ImBoolean(false), copyColor(var8));
   }

   private static BlockColorListSetting.BlockEntry entry(String var0, String var1, String var2, boolean var3, int var4, boolean var5) {
      float[] var6 = new float[]{(var4 >> 16 & 0xFF) / 255.0F, (var4 >> 8 & 0xFF) / 255.0F, (var4 & 0xFF) / 255.0F, 1.0F};
      return new BlockColorListSetting.BlockEntry(var0, var1, var2, new ImBoolean(var3), var6, new ImBoolean(var5), copyColor(var6));
   }

   private static BlockColorListSetting.BlockEntry copyEntry(BlockColorListSetting.BlockEntry var0) {
      return new BlockColorListSetting.BlockEntry(
         var0.id(),
         var0.displayName(),
         var0.category(),
         new ImBoolean(var0.enabled().get()),
         copyColor(var0.color()),
         new ImBoolean(var0.tracerEnabled().get()),
         copyColor(var0.tracerColor())
      );
   }

   private static float[] copyColor(float[] var0) {
      return new float[]{var0[0], var0[1], var0[2], var0[3]};
   }

   public static int rgbaToArgb(float[] var0) {
      int var1 = Math.round(clamp01(var0[0]) * 255.0F);
      int var2 = Math.round(clamp01(var0[1]) * 255.0F);
      int var3 = Math.round(clamp01(var0[2]) * 255.0F);
      int var4 = Math.round(clamp01(var0[3]) * 255.0F);
      return var4 << 24 | var1 << 16 | var2 << 8 | var3;
   }

   private static float clamp01(float var0) {
      return !Float.isNaN(var0) && !Float.isInfinite(var0) ? Math.max(0.0F, Math.min(1.0F, var0)) : 1.0F;
   }

   @Environment(EnvType.CLIENT)
   public record BlockEntry(String id, String displayName, String category, ImBoolean enabled, float[] color, ImBoolean tracerEnabled, float[] tracerColor) {
   }
}
