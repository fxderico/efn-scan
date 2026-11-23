package net.efn.client.easyplace.schematic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record SchematicBlockState(BlockState state, Identifier blockId, Map<String, String> properties) {
   public static SchematicBlockState of(BlockState blockState) {
      LinkedHashMap var1 = new LinkedHashMap();
      blockState.getEntries().forEach((property, comparable) -> var1.put(property.getName(), comparable.toString()));
      return new SchematicBlockState(blockState, Registries.BLOCK.getId(blockState.getBlock()), Collections.unmodifiableMap(var1));
   }

   public String debugString() {
      if (this.properties.isEmpty()) {
         return this.blockId.toString();
      } else {
         StringBuilder var1 = new StringBuilder(this.blockId.toString()).append('[');
         this.properties.forEach((string, string2) -> var1.append(string).append('=').append(string2).append(','));
         var1.setCharAt(var1.length() - 1, ']');
         return var1.toString();
      }
   }
}
