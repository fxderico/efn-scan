package net.efn.client.module.impl.misc;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.EfnKeyPolling;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.LootLatch;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class AutoClickerModule extends ClientModule {
   private static final String CLICK = "Click";
   private static final String HOLD = "Hold";
   private static final String OFF = "Off";
   private static final double TICK_MS = 50.0;
   private static final double PACE_MIN_MS = 500.0;
   private static final double PACE_MAX_MS = 1500.0;
   private static final double PACE_EASE = 0.3;
   private static final double HOLD_MEAN_MS = 55.0;
   private static final double HOLD_SPREAD_MS = 18.0;
   private static final double HOLD_MIN_MS = 20.0;
   private static final double HOLD_MAX_MS = 110.0;
   private static final double HOLD_MAX_GAP = 0.7;
   private static final double GAP_MIN_FACTOR = 0.6;
   private static final double GAP_MAX_FACTOR = 2.2;
   private final ModeSetting left = this.addSetting("left", new ModeSetting("Left", "Click", "Click", "Hold", "Off"));
   private final NumberSetting leftMin = this.addSetting("left-min-cps", new NumberSetting("Left Min CPS", 9.0F, 1.0F, 20.0F).step(1.0F));
   private final NumberSetting leftMax = this.addSetting("left-max-cps", new NumberSetting("Left Max CPS", 13.0F, 1.0F, 20.0F).step(1.0F));
   private final ModeSetting right = this.addSetting("right", new ModeSetting("Right", "Off", "Click", "Hold", "Off"));
   private final NumberSetting rightMin = this.addSetting("right-min-cps", new NumberSetting("Right Min CPS", 8.0F, 1.0F, 20.0F).step(1.0F));
   private final NumberSetting rightMax = this.addSetting("right-max-cps", new NumberSetting("Right Max CPS", 12.0F, 1.0F, 20.0F).step(1.0F));
   private final NumberSetting randomization = this.addSetting("randomization", new NumberSetting("Randomization", 15.0F, 0.0F, 40.0F).step(1.0F));
   private final BooleanSetting onlyWhileHeld = this.addSetting("only-while-held", new BooleanSetting("Only While Held", false));
   private final BooleanSetting breakBlocks = this.addSetting("break-blocks", new BooleanSetting("Break Blocks", true));
   private final AutoClickerModule.Button leftButton = new AutoClickerModule.Button(
      true, this.left, this.leftMin, this.leftMax, var0 -> var0.field_1690.field_1886
   );
   private final AutoClickerModule.Button rightButton = new AutoClickerModule.Button(
      false, this.right, this.rightMin, this.rightMax, var0 -> var0.field_1690.field_1904
   );

   public AutoClickerModule() {
      super("auto-clicker", "Auto Clicker", "Clicks the left and/or right mouse button at a randomised, human CPS.", ModuleCategory.MISC);
      this.leftMin.visibleWhen(() -> this.left.is("Click"));
      this.leftMax.visibleWhen(() -> this.left.is("Click"));
      this.rightMin.visibleWhen(() -> this.right.is("Click"));
      this.rightMax.visibleWhen(() -> this.right.is("Click"));
      this.randomization.visibleWhen(this::anyClicking);
      this.onlyWhileHeld.visibleWhen(this::anyClicking);
      this.breakBlocks.visibleWhen(() -> this.left.is("Click"));
   }

   @Override
   public int settingsVersion() {
      return 1;
   }

   @Override
   public void migrateSettings(int var1) {
      if (var1 < 1) {
         this.onlyWhileHeld.set(false);
      }
   }

   @Override
   protected void onDisable() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      this.leftButton.handBack(var1);
      this.rightButton.handBack(var1);
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      boolean var3 = MeleeAttacks.canAct(var2)
         && var2.method_18506() == null
         && !CombatMacroLock.heldByOther(this)
         && !MeleeAttacks.actedThisTick(var2.field_1724);
      this.leftButton.tick(var2, var3);
      this.rightButton.tick(var2, var3 && !LootLatch.active());
   }

   private boolean anyClicking() {
      return this.left.is("Click") || this.right.is("Click");
   }

   private static boolean aimsAtBlock(MinecraftClient var0) {
      HitResult var2 = var0.field_1765;
      BlockHitResult var1;
      return var2 instanceof BlockHitResult
         && (var1 = (BlockHitResult)var2).method_17783() == Type.field_1332
         && var0.field_1687 != null
         && !var0.field_1687.method_8320(var1.method_17777()).method_26215();
   }

   private static boolean physicallyDown(MinecraftClient var0, KeyBinding var1) {
      if (var1.method_1415()) {
         return false;
      } else {
         Key var2 = KeyBindingHelper.getBoundKeyOf(var1);
         int var3 = var2.method_1444();
         if (var3 < 0) {
            return false;
         } else {
            return switch (var2.method_1442()) {
               case field_1672 -> GLFW.glfwGetMouseButton(var0.method_22683().method_4490(), var3) == 1;
               case field_1668 -> EfnKeyPolling.isKeyDown(var0.method_22683(), var3);
               default -> var1.method_1434();
            };
         }
      }
   }

   private static double clamp(double var0, double var2, double var4) {
      return Math.max(var2, Math.min(var4, var0));
   }

   @Environment(EnvType.CLIENT)
   private final class Button {
      private final boolean isLeft;
      private final ModeSetting mode;
      private final NumberSetting minCps;
      private final NumberSetting maxCps;
      private final Function<MinecraftClient, KeyBinding> mapping;
      private boolean driving;
      private boolean running;
      private double clock;
      private double nextPress;
      private double releaseAt;
      private double rate;
      private double pace;
      private double repaceAt;

      Button(boolean nullx, ModeSetting nullxx, NumberSetting nullxxx, NumberSetting nullxxxx, Function<MinecraftClient, KeyBinding> nullxxxxx) {
         this.isLeft = nullx;
         this.mode = nullxx;
         this.minCps = nullxxx;
         this.maxCps = nullxxxx;
         this.mapping = nullxxxxx;
      }

      void tick(MinecraftClient var1, boolean var2) {
         if (var2 && !this.mode.is("Off")) {
            KeyBinding var3 = this.mapping.apply(var1);
            if (this.mode.is("Hold")) {
               this.running = false;
               this.driving = true;
               var3.method_23481(true);
            } else if (!var3.method_1415()
               && (!AutoClickerModule.this.onlyWhileHeld.get() || AutoClickerModule.physicallyDown(var1, var3))
               && (!this.isLeft || !AutoClickerModule.this.breakBlocks.get() || !AutoClickerModule.aimsAtBlock(var1))
               && (this.isLeft || !var1.field_1724.method_6115())) {
               if (!this.running) {
                  this.start();
               }

               this.clock += 50.0;
               if (this.isLeft) {
                  var1.field_1771 = 0;
               }

               Key var4 = KeyBindingHelper.getBoundKeyOf(var3);

               while (this.nextPress <= this.clock) {
                  KeyBinding.method_1420(var4);
                  double var5 = this.nextGap();
                  this.releaseAt = this.nextPress + this.holdFor(var5);
                  this.nextPress += var5;
               }

               this.driving = true;
               var3.method_23481(this.clock < this.releaseAt);
            } else {
               this.handBack(var1);
            }
         } else {
            this.handBack(var1);
         }
      }

      private void start() {
         double var1 = Math.min(this.minCps.get(), this.maxCps.get());
         double var3 = Math.max(this.minCps.get(), this.maxCps.get());
         ThreadLocalRandom var5 = ThreadLocalRandom.current();
         this.running = true;
         this.clock = 0.0;
         this.releaseAt = 0.0;
         this.rate = this.pace = var1 + var5.nextDouble() * (var3 - var1);
         this.repaceAt = 500.0 + var5.nextDouble() * 1000.0;
         this.nextPress = this.nextGap();
      }

      private double nextGap() {
         double var1 = Math.min(this.minCps.get(), this.maxCps.get());
         double var3 = Math.max(this.minCps.get(), this.maxCps.get());
         ThreadLocalRandom var5 = ThreadLocalRandom.current();
         if (this.clock >= this.repaceAt) {
            this.pace = var1 + var5.nextDouble() * (var3 - var1);
            this.repaceAt = this.clock + 500.0 + var5.nextDouble() * 1000.0;
         }

         this.rate = AutoClickerModule.clamp(this.rate + (this.pace - this.rate) * 0.3, var1, var3);
         double var6 = 1000.0 / this.rate;
         double var8 = AutoClickerModule.this.randomization.get() / 100.0;
         if (var8 <= 0.0) {
            return var6;
         } else {
            double var10 = 1.0 + var5.nextGaussian() * var8;
            if (var5.nextDouble() < var8 * 0.25) {
               var10 += 0.4 + var5.nextDouble() * 0.6;
            }

            return var6 * AutoClickerModule.clamp(var10, 0.6, 2.2);
         }
      }

      private double holdFor(double var1) {
         if (AutoClickerModule.this.randomization.get() <= 0.0F) {
            return var1 * 0.5;
         } else {
            double var3 = 55.0 + ThreadLocalRandom.current().nextGaussian() * 18.0;
            return AutoClickerModule.clamp(var3, 20.0, Math.min(110.0, var1 * 0.7));
         }
      }

      void handBack(MinecraftClient var1) {
         this.running = false;
         if (this.driving) {
            this.driving = false;
            KeyBinding var2 = this.mapping.apply(var1);
            var2.method_23481(var1.field_1755 == null && AutoClickerModule.physicallyDown(var1, var2));
         }
      }
   }
}
