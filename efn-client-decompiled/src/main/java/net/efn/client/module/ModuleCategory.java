package net.efn.client.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModuleCategory {
   private static final List<ModuleCategory> REGISTERED = new ArrayList<>();
   private static volatile List<ModuleCategory> values = List.of();
   public static final ModuleCategory COMBAT = register("COMBAT", "Combat", ModuleCategory.Icon.CROSSHAIR);
   public static final ModuleCategory MISC = register("MISC", "Misc", ModuleCategory.Icon.GRID);
   public static final ModuleCategory DONUT = register("DONUT", "Donut", ModuleCategory.Icon.RING);
   public static final ModuleCategory RENDER = register("RENDER", "Render", ModuleCategory.Icon.EYE);
   public static final ModuleCategory CLIENT = register("CLIENT", "EFN Client", ModuleCategory.Icon.BOLT);
   public static final ModuleCategory MOVEMENT = register("MOVEMENT", "Movement", ModuleCategory.Icon.MOVE);
   public static final ModuleCategory BASE_RAID = register("BASE_RAID", "Base Finding", ModuleCategory.Icon.HOUSE);
   public static final ModuleCategory VISUALS = register("VISUALS", "Visuals", ModuleCategory.Icon.MONITOR);
   public static final ModuleCategory DONUT_MACROS = register("DONUT_MACROS", "Macro", ModuleCategory.Icon.SCROLL);
   private final String id;
   private final String displayName;
   private final ModuleCategory.Icon icon;

   private ModuleCategory(String string, String string2, ModuleCategory.Icon icon) {
      this.id = string;
      this.displayName = string2;
      this.icon = icon;
   }

   public static synchronized ModuleCategory register(String string, String string2, ModuleCategory.Icon icon) {
      if (!isValidId(string)) {
         throw new IllegalArgumentException("Category id must be upper-case letters, digits and '_': " + string);
      } else {
         for (ModuleCategory var4 : REGISTERED) {
            if (var4.id.equals(string)) {
               throw new IllegalArgumentException("Duplicate category id: " + string);
            }
         }

         ModuleCategory var5 = new ModuleCategory(string, Objects.requireNonNull(string2, "displayName"), Objects.requireNonNull(icon, "icon"));
         REGISTERED.add(var5);
         values = List.copyOf(REGISTERED);
         return var5;
      }
   }

   private static boolean isValidId(String string) {
      if (string != null && !string.isEmpty() && string.charAt(0) >= 'A' && string.charAt(0) <= 'Z') {
         for (int var1 = 1; var1 < string.length(); var1++) {
            char var2 = string.charAt(var1);
            if ((var2 < 'A' || var2 > 'Z') && (var2 < '0' || var2 > '9') && var2 != '_') {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static ModuleCategory register(String string, String string2) {
      return register(string, string2, ModuleCategory.Icon.BOLT);
   }

   public static List<ModuleCategory> values() {
      return values;
   }

   public String name() {
      return this.id;
   }

   public String displayName() {
      return this.displayName;
   }

   public ModuleCategory.Icon icon() {
      return this.icon;
   }

   @Override
   public String toString() {
      return this.id;
   }

   @Environment(EnvType.CLIENT)
   public static enum Icon {
      CROSSHAIR,
      CHEVRON,
      GRID,
      RING,
      EYE,
      MONITOR,
      HOUSE,
      SCROLL,
      MOVE,
      BOLT;

      private Icon() {
      }
   }
}
