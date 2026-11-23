package net.efn.client.module.impl.combat.crystal;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class LootGuard {
   private static final double HALF_WIDTH = 8.0;
   private static final double HALF_HEIGHT = 4.0;
   private static final double WATCH_RANGE = 16.0;
   private static final double LOADED_RANGE = 32.0;
   private static final long OTHER_MS = 60000L;
   private static final long OWN_MS = 300000L;
   private static final long EMPTY_GRACE_MS = 3000L;
   private static final List<LootGuard.Spot> SPOTS = new ArrayList<>();
   private static final IntSet SEEN = new IntOpenHashSet();
   private static RegistryKey<World> dimension;
   private static ClientPlayerEntity lastPlayer;
   private static int lastTick = -1;
   private static boolean selfDead;

   private LootGuard() {
   }

   public static void update(ClientWorld var0, ClientPlayerEntity var1) {
      if (var1 != lastPlayer || var1.field_6012 != lastTick) {
         lastPlayer = var1;
         lastTick = var1.field_6012;
         dimension = var0.method_27983();
         long var2 = Util.method_658();
         if (var1.method_29504()) {
            if (!selfDead) {
               SPOTS.add(new LootGuard.Spot(dimension, var1.method_73189(), var2, var2 + 300000L));
            }

            selfDead = true;
         } else {
            selfDead = false;
         }

         SEEN.removeIf(var1x -> var0.method_8469(var1x) == null);

         for (PlayerEntity var5 : var0.method_18456()) {
            if (var5 != var1 && var5.method_29504() && SEEN.add(var5.method_5628()) && var5.method_5739(var1) <= 16.0) {
               SPOTS.add(new LootGuard.Spot(dimension, var5.method_73189(), var2, var2 + 60000L));
            }
         }

         SPOTS.removeIf(
            var4 -> var2 > var4.until()
               || var4.dimension() == dimension
                  && var2 - var4.since() > 3000L
                  && var4.at().method_1022(var1.method_73189()) <= 32.0
                  && var0.method_18467(ItemEntity.class, var4.box()).isEmpty()
         );
      }
   }

   public static boolean near(Vec3d var0) {
      for (LootGuard.Spot var2 : SPOTS) {
         if (var2.dimension() == dimension && var2.box().method_1006(var0)) {
            return true;
         }
      }

      return false;
   }

   @Environment(EnvType.CLIENT)
   private record Spot(RegistryKey<World> dimension, Vec3d at, long since, long until) {
      Box box() {
         return new Box(
            this.at.field_1352 - 8.0,
            this.at.field_1351 - 4.0,
            this.at.field_1350 - 8.0,
            this.at.field_1352 + 8.0,
            this.at.field_1351 + 4.0,
            this.at.field_1350 + 8.0
         );
      }
   }
}
