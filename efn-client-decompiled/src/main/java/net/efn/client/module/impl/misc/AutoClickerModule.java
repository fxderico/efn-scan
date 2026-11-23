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
      true, this.left, this.leftMin, this.leftMax, minecraftClient -> minecraftClient.options.attackKey
   );
   private final AutoClickerModule.Button rightButton = new AutoClickerModule.Button(
      false, this.right, this.rightMin, this.rightMax, minecraftClient -> minecraftClient.options.useKey
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
   public void migrateSettings(int i) {
      if (i < 1) {
         this.onlyWhileHeld.set(false);
      }
   }

   @Override
   protected void onDisable() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      this.leftButton.handBack(var1);
      this.rightButton.handBack(var1);
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      boolean var3 = MeleeAttacks.canAct(var2) && var2.getOverlay() == null && !CombatMacroLock.heldByOther(this) && !MeleeAttacks.actedThisTick(var2.player);
      this.leftButton.tick(var2, var3);
      this.rightButton.tick(var2, var3 && !LootLatch.active());
   }

   private boolean anyClicking() {
      return this.left.is("Click") || this.right.is("Click");
   }

   private static boolean aimsAtBlock(MinecraftClient minecraftClient) {
      HitResult var2 = minecraftClient.crosshairTarget;
      BlockHitResult var1;
      return var2 instanceof BlockHitResult
         && (var1 = (BlockHitResult)var2).getType() == Type.BLOCK
         && minecraftClient.world != null
         && !minecraftClient.world.getBlockState(var1.getBlockPos()).isAir();
   }

   private static boolean physicallyDown(MinecraftClient minecraftClient, KeyBinding keyBinding) {
      if (keyBinding.isUnbound()) {
         return false;
      } else {
         Key var2 = KeyBindingHelper.getBoundKeyOf(keyBinding);
         int var3 = var2.getCode();
         if (var3 < 0) {
            return false;
         } else {
            return switch (var2.getCategory()) {
               case MOUSE -> GLFW.glfwGetMouseButton(minecraftClient.getWindow().getHandle(), var3) == 1;
               case KEYSYM -> EfnKeyPolling.isKeyDown(minecraftClient.getWindow(), var3);
               default -> keyBinding.isPressed();
            };
         }
      }
   }

   private static double clamp(double d, double e, double f) {
      return Math.max(e, Math.min(f, d));
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

      Button(boolean bl, ModeSetting modeSetting, NumberSetting numberSetting, NumberSetting numberSetting2, Function<MinecraftClient, KeyBinding> function) {
         this.isLeft = bl;
         this.mode = modeSetting;
         this.minCps = numberSetting;
         this.maxCps = numberSetting2;
         this.mapping = function;
      }

      void tick(MinecraftClient minecraftClient, boolean bl) {
         if (bl && !this.mode.is("Off")) {
            KeyBinding var3 = this.mapping.apply(minecraftClient);
            if (this.mode.is("Hold")) {
               this.running = false;
               this.driving = true;
               var3.setPressed(true);
            } else if (!var3.isUnbound()
               && (!AutoClickerModule.this.onlyWhileHeld.get() || AutoClickerModule.physicallyDown(minecraftClient, var3))
               && (!this.isLeft || !AutoClickerModule.this.breakBlocks.get() || !AutoClickerModule.aimsAtBlock(minecraftClient))
               && (this.isLeft || !minecraftClient.player.isUsingItem())) {
               if (!this.running) {
                  this.start();
               }

               this.clock += 50.0;
               if (this.isLeft) {
                  minecraftClient.attackCooldown = 0;
               }

               Key var4 = KeyBindingHelper.getBoundKeyOf(var3);

               while (this.nextPress <= this.clock) {
                  KeyBinding.onKeyPressed(var4);
                  double var5 = this.nextGap();
                  this.releaseAt = this.nextPress + this.holdFor(var5);
                  this.nextPress += var5;
               }

               this.driving = true;
               var3.setPressed(this.clock < this.releaseAt);
            } else {
               this.handBack(minecraftClient);
            }
         } else {
            this.handBack(minecraftClient);
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

      private double holdFor(double d) {
         if (AutoClickerModule.this.randomization.get() <= 0.0F) {
            return d * 0.5;
         } else {
            double var3 = 55.0 + ThreadLocalRandom.current().nextGaussian() * 18.0;
            return AutoClickerModule.clamp(var3, 20.0, Math.min(110.0, d * 0.7));
         }
      }

      void handBack(MinecraftClient minecraftClient) {
         this.running = false;
         if (this.driving) {
            this.driving = false;
            KeyBinding var2 = this.mapping.apply(minecraftClient);
            var2.setPressed(minecraftClient.currentScreen == null && AutoClickerModule.physicallyDown(minecraftClient, var2));
         }
      }
   }
}
