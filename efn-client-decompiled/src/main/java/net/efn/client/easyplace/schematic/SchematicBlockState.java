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
   public static SchematicBlockState of(BlockState var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var0.method_11656().forEach((var1x, var2) -> var1.put(var1x.method_11899(), var2.toString()));
      return new SchematicBlockState(var0, Registries.field_41175.method_10221(var0.method_26204()), Collections.unmodifiableMap(var1));
   }

   public String debugString() {
      if (this.properties.isEmpty()) {
         return this.blockId.toString();
      } else {
         StringBuilder var1 = new StringBuilder(this.blockId.toString()).append('[');
         this.properties.forEach((var1x, var2) -> var1.append(var1x).append('=').append(var2).append(','));
         var1.setCharAt(var1.length() - 1, ']');
         return var1.toString();
      }
   }
}
