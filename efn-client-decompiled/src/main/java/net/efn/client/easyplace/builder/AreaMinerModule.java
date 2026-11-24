package net.efn.client.easyplace.builder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.efn.client.chat.ModuleChat;
import net.efn.client.easyplace.inventory.InventoryCalm;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.setting.ActionSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public final class AreaMinerModule extends ClientModule {
   private static final long PLAN_BUDGET_NANOS = 3000000L;
   private static final int PLAN_TARGETS = 400;
   private static final int BREAK_TRIES = 3;
   private static final int PLAN_FAILS = 2;
   private static final int SCAN_TICKS = 10;
   private static final int MEND_TICKS = 1200;
   private static final int MAX_CELLS = 2000000;
   private static final int AREA_COLOR = -11155201;
   private static final int TARGET_COLOR = -20416;
   private static final Predicate<ItemStack> XP = itemStack -> itemStack.isOf(Items.EXPERIENCE_BOTTLE);
   private final StringSetting corner1 = this.addSetting("corner-1", new StringSetting("Corner 1", "").hint("x y z"));
   private final StringSetting corner2 = this.addSetting("corner-2", new StringSetting("Corner 2", "").hint("x y z"));
   private final ActionSetting select = this.addSetting("select-area", new ActionSetting("Select Area With Clicks", this::selectArea));
   private final ActionSetting feet1 = this.addSetting("corner-1-feet", new ActionSetting("Corner 1 At Feet", () -> this.setCorner(this.corner1, false)));
   private final ActionSetting look1 = this.addSetting("corner-1-look", new ActionSetting("Corner 1 At Crosshair", () -> this.setCorner(this.corner1, true)));
   private final ActionSetting feet2 = this.addSetting("corner-2-feet", new ActionSetting("Corner 2 At Feet", () -> this.setCorner(this.corner2, false)));
   private final ActionSetting look2 = this.addSetting("corner-2-look", new ActionSetting("Corner 2 At Crosshair", () -> this.setCorner(this.corner2, true)));
   private final BooleanSetting useShard = this.addSetting("use-shard", new BooleanSetting("Use Shard Pickaxe", true));
   private final BooleanSetting eat = this.addSetting("eat", new BooleanSetting("Eat When Hungry", true));
   private final BooleanSetting mend = this.addSetting("mend", new BooleanSetting("Mend With XP Bottles", true));
   private final NumberSetting mendBelow = this.addSetting("mend-below", new NumberSetting("Mend Below Durability", 150.0F, 20.0F, 1000.0F).step(10.0F));
   private final BooleanSetting pauseInMenus = this.addSetting("pause-in-menus", new BooleanSetting("Pause In Menus", true));
   private final BooleanSetting showArea = this.addSetting("show-area", new BooleanSetting("Show Area", true));
   private final BooleanSetting chatFeedback = this.addSetting("chat-feedback", new BooleanSetting("Chat Feedback", true));
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private BlockPos min;
   private BlockPos max;
   private ClientWorld seenLevel;
   private long tick;
   private long nextScan;
   private long noPlanUntil;
   private Breaker breaker;
   private boolean breakerShard;
   private Hop hop;
   private Eater eater;
   private StandPlanner planner;
   private List<StandPlanner.Step> route;
   private int routeIndex;
   private BlockPos routeStart;
   private boolean mending;
   private long mendStarted;
   private int mined;
   private int startCount;
   private int layer = -2147483648;
   private final List<BlockPos> layerCells = new ArrayList<>();
   private final Map<Long, Integer> breakTries = new HashMap<>();
   private final Map<Long, Integer> planFails = new HashMap<>();
   private final Set<Long> unreachable = new HashSet<>();
   private final Set<Long> holding = new HashSet<>();
   private final Map<String, Long> lastFeedback = new HashMap<>();
   private int picking;
   private BlockPos firstPick;

   public AreaMinerModule() {
      super(
         "area-miner",
         "Area Miner",
         "Mines out everything between two corners, top layer first, walking and turning by hand like a player. A Shard Pickaxe cuts 3x3 wherever the whole cut stays inside the area; blocks holding back lava or water stay.",
         ModuleCategory.MISC
      );
      this.usesNotifications();
      this.mendBelow.visibleWhen(this.mend::get);
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      ClientPlayerEntity var1 = this.mc.player;
      if (var1 != null && this.mc.world != null) {
         this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.step());
         this.on(Render3DEvent.TYPE, Stage.POST, this::render);
         this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
         BlockPos var2 = parse(this.corner1.get());
         BlockPos var3 = parse(this.corner2.get());
         if (var2 != null && var3 != null) {
            if (!this.begin(var2, var3)) {
               this.requestDisable();
            }
         } else {
            this.startPicking();
         }
      } else {
         this.say(ModuleChat.body().warn("Join a world first."));
         this.requestDisable();
      }
   }

   private boolean begin(BlockPos blockPos, BlockPos blockPos2) {
      BlockPos var3 = new BlockPos(
         Math.min(blockPos.getX(), blockPos2.getX()), Math.min(blockPos.getY(), blockPos2.getY()), Math.min(blockPos.getZ(), blockPos2.getZ())
      );
      BlockPos var4 = new BlockPos(
         Math.max(blockPos.getX(), blockPos2.getX()), Math.max(blockPos.getY(), blockPos2.getY()), Math.max(blockPos.getZ(), blockPos2.getZ())
      );
      long var5 = (long)(var4.getX() - var3.getX() + 1) * (var4.getY() - var3.getY() + 1) * (var4.getZ() - var3.getZ() + 1);
      if (var5 > 2000000L) {
         this.say(ModuleChat.body().warn("The area is too big").text(" (" + var5 + " blocks, at most 2000000)."));
         return false;
      } else {
         this.reset();
         this.min = var3;
         this.max = var4;
         this.seenLevel = this.mc.world;
         this.startCount = this.countLeft(this.mc.world);
         this.say(
            ModuleChat.body().text("Mining " + this.startCount + " blocks between " + this.min.toShortString() + " and " + this.max.toShortString() + ".")
         );
         return true;
      }
   }

   private void startPicking() {
      this.release();
      this.min = null;
      this.max = null;
      this.firstPick = null;
      this.picking = 1;
      this.say(ModuleChat.body().text("Hit the first corner block").text(" (or type both corners in the settings)."));
   }

   private void selectArea() {
      this.corner1.set("");
      this.corner2.set("");
      if (this.isEnabled()) {
         this.startPicking();
      } else {
         this.setEnabled(true);
      }
   }

   private void onMouse(MouseInputEvent mouseInputEvent) {
      if (this.picking != 0 && mouseInputEvent.button == 0 && mouseInputEvent.action == 1) {
         if (this.mc.currentScreen == null && this.mc.player != null && this.mc.world != null) {
            mouseInputEvent.setCancelled(true);
            BlockPos var2 = this.aimedBlock();
            if (var2 == null) {
               this.say(ModuleChat.body().warn("No block there").text(", look at a block."));
            } else {
               String var3 = var2.getX() + " " + var2.getY() + " " + var2.getZ();
               if (this.picking == 1) {
                  this.firstPick = var2;
                  this.corner1.set(var3);
                  this.picking = 2;
                  this.say(ModuleChat.body().text("Corner 1 set to " + var2.toShortString() + ". Hit the second corner block."));
               } else {
                  this.corner2.set(var3);
                  this.say(ModuleChat.body().text("Corner 2 set to " + var2.toShortString() + "."));
                  if (this.begin(this.firstPick, var2)) {
                     this.picking = 0;
                  } else {
                     this.say(ModuleChat.body().text("Hit the second corner block again."));
                  }
               }
            }
         }
      }
   }

   private BlockPos aimedBlock() {
      ClientPlayerEntity var3 = this.mc.player;
      if (var3 != null && this.mc.world != null) {
         FreecamModule var4 = ModuleRegistry.get(FreecamModule.class);
         Vec3d var1;
         Vec3d var2;
         if (var4 != null && var4.isEnabled()) {
            Camera var5 = this.mc.gameRenderer.getCamera();
            var2 = var5.getCameraPos();
            var1 = Vec3d.fromPolar(var5.getPitch(), var5.getYaw());
         } else {
            var2 = var3.getCameraPosVec(1.0F);
            var1 = var3.getRotationVec(1.0F);
         }

         double var8 = this.mc.options.getClampedViewDistance() * 16.0;
         BlockHitResult var7 = this.mc.world.raycast(new RaycastContext(var2, var2.add(var1.multiply(var8)), ShapeType.OUTLINE, FluidHandling.NONE, var3));
         return var7.getType() == Type.BLOCK ? var7.getBlockPos() : null;
      } else {
         return null;
      }
   }

   @Override
   protected void onDisable() {
      this.release();
      this.planner = null;
      this.route = null;
      this.picking = 0;
      this.firstPick = null;
   }

   private void reset() {
      this.tick = 0L;
      this.nextScan = 0L;
      this.noPlanUntil = 0L;
      this.breaker = null;
      this.hop = null;
      this.eater = null;
      this.planner = null;
      this.route = null;
      this.mending = false;
      this.mined = 0;
      this.layer = -2147483648;
      this.layerCells.clear();
      this.breakTries.clear();
      this.planFails.clear();
      this.unreachable.clear();
      this.holding.clear();
      this.lastFeedback.clear();
   }

   private void step() {
      ClientPlayerEntity var1 = this.mc.player;
      ClientWorld var2 = this.mc.world;
      if (var1 != null && var2 != null && this.mc.interactionManager != null) {
         if (this.picking == 0 && this.min != null) {
            this.tick++;
            if (var2 != this.seenLevel || !var1.isAlive()) {
               this.say(ModuleChat.body().warn("Stopped").text(", the world changed or you died."));
               this.requestDisable();
            } else if (this.mc.currentScreen != null && this.pauseInMenus.get()) {
               this.release();
            } else if (this.eater != null) {
               if (this.eater.tick(this.mc) != Eater.Result.RUNNING) {
                  this.eater = null;
               }
            } else if (this.mending) {
               this.mend(var1);
            } else if (this.breaker != null) {
               BlockState var6 = var2.getBlockState(this.breaker.pos());
               if (var6.isAir() || Hands.hold(this.mc, var1, toolFor(var1, var6, this.breakerShard))) {
                  Breaker.Result var4 = this.breaker.tick(this.mc);
                  if (var4 != Breaker.Result.RUNNING) {
                     BlockPos var5 = this.breaker.pos();
                     this.breaker = null;
                     if (minable(var2, var5) && this.breakTries.merge(var5.asLong(), 1, Integer::sum) >= 3) {
                        this.unreachable.add(var5.asLong());
                        this.feedback("gave-up", "Could not mine " + var5.toShortString() + ", leaving it", 40);
                     }

                     this.nextScan = 0L;
                  }
               }
            } else if (this.hop != null) {
               Hop.Result var3 = this.hop.tick(this.mc);
               if (var3 != Hop.Result.RUNNING) {
                  this.hop = null;
                  if (var3 == Hop.Result.FAILED) {
                     this.route = null;
                     this.noPlanUntil = this.tick + 20L;
                  }
               }
            } else if (this.eat.get() && Eater.hungry(var1) && Eater.hasFood(var1) && InventoryCalm.still(var1)) {
               this.release();
               this.eater = new Eater();
            } else if (this.mendDue(var1)) {
               this.mending = true;
               this.mendStarted = this.tick;
               this.feedback("mend", "Tool is worn, mending it with XP bottles", 200);
            } else if (this.route != null) {
               this.followRoute(var2, var1);
            } else if (this.planner != null) {
               this.followPlanner();
            } else {
               if (this.tick >= this.nextScan) {
                  this.nextScan = this.tick + 10L;
                  if (!this.pickLayer(var2)) {
                     this.finish();
                     return;
                  }
               }

               this.dig(var2, var1);
            }
         }
      }
   }

   private boolean pickLayer(ClientWorld clientWorld) {
      this.layerCells.clear();
      Mutable var2 = new Mutable();

      for (int var3 = this.max.getY(); var3 >= this.min.getY(); var3--) {
         for (int var4 = this.min.getX(); var4 <= this.max.getX(); var4++) {
            for (int var5 = this.min.getZ(); var5 <= this.max.getZ(); var5++) {
               var2.set(var4, var3, var5);
               long var6 = var2.asLong();
               if (!this.unreachable.contains(var6) && clientWorld.isChunkLoaded(var2) && minable(clientWorld, var2)) {
                  if (this.holdsFluid(clientWorld, var2)) {
                     if (this.holding.add(var6)) {
                        this.feedback("fluid", "Leaving " + var2.toShortString() + ", it holds back lava or water", 100);
                     }
                  } else {
                     this.holding.remove(var6);
                     this.layerCells.add(var2.toImmutable());
                  }
               }
            }
         }

         if (!this.layerCells.isEmpty()) {
            if (var3 != this.layer) {
               this.layer = var3;
               this.planFails.clear();
               this.feedback("layer", "Layer Y=" + var3 + ", " + this.layerCells.size() + (this.layerCells.size() == 1 ? " block" : " blocks"), 1);
            }

            return true;
         }
      }

      return false;
   }

   private void dig(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      if (!this.layerCells.isEmpty()) {
         Vec3d var3 = clientPlayerEntity.getEyePos();
         double var4 = clientPlayerEntity.getBlockInteractionRange() - 0.3;
         ArrayList var6 = new ArrayList();

         for (BlockPos var8 : this.layerCells) {
            if (var8.toCenterPos().distanceTo(var3) <= var4 && minable(clientWorld, var8)) {
               var6.add(var8);
            }
         }

         BlockPos var13 = clientPlayerEntity.getBlockPos().down();
         var6.sort(
            Comparator.<BlockPos>comparingInt(blockPos2 -> blockPos2.equals(var13) ? 1 : 0)
               .thenComparingDouble(blockPos -> blockPos.toCenterPos().squaredDistanceTo(var3))
         );

         for (BlockPos var9 : var6) {
            if (Aim.at(this.mc, var9) != null) {
               BlockState var10 = clientWorld.getBlockState(var9);
               boolean var11 = this.breakerShard = this.useShard.get() && this.cutInside(clientWorld, var9);
               if (!Hands.hold(this.mc, clientPlayerEntity, toolFor(clientPlayerEntity, var10, this.breakerShard))) {
                  return;
               }

               this.breaker = new Breaker(var9, true);
               return;
            }
         }

         if (this.tick >= this.noPlanUntil) {
            ArrayList var15 = new ArrayList();
            Vec3d var16 = clientPlayerEntity.getEntityPos();
            ArrayList var17 = new ArrayList<>(this.layerCells);
            var17.sort(Comparator.comparingDouble(blockPos -> blockPos.getSquaredDistance(var16)));

            for (BlockPos var12 : var17) {
               if (var15.size() >= 400) {
                  break;
               }

               var15.add(new StandPlanner.Target(var12, clientWorld.getBlockState(var12), StandPlanner.Use.BREAK));
            }

            this.planner = new StandPlanner(clientWorld, clientPlayerEntity, var15, blockPos -> false, null, null, null, null);
            this.followPlanner();
         }
      }
   }

   private void followPlanner() {
      StandPlanner.Status var1 = this.planner.step(System.nanoTime() + 3000000L);
      if (var1 != StandPlanner.Status.RUNNING) {
         StandPlanner var2 = this.planner;
         this.planner = null;
         if ((var1 == StandPlanner.Status.FOUND || var1 == StandPlanner.Status.APPROACH) && !var2.route().isEmpty()) {
            this.route = var2.route();
            this.routeIndex = 0;
            this.routeStart = this.mc.player == null ? null : this.mc.player.getBlockPos();
         } else {
            this.noPlanUntil = this.tick + 40L;
            boolean var3 = false;

            for (BlockPos var5 : this.layerCells) {
               if (this.planFails.merge(var5.asLong(), 1, Integer::sum) >= 2) {
                  this.unreachable.add(var5.asLong());
                  var3 = true;
               }
            }

            if (var3) {
               this.feedback("unreachable", "No spot gets at the rest of layer Y=" + this.layer + ", leaving those blocks", 40);
            }

            this.nextScan = 0L;
         }
      }
   }

   private void followRoute(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      if (this.routeIndex >= this.route.size()) {
         this.route = null;
         this.nextScan = 0L;
      } else if (clientPlayerEntity.isOnGround()) {
         BlockPos var4 = this.routeIndex == 0 ? this.routeStart : this.route.get(this.routeIndex - 1).pos();
         BlockPos var5 = clientPlayerEntity.getBlockPos();
         if (var4 == null || var5.getX() == var4.getX() && var5.getZ() == var4.getZ() && Math.abs(var5.getY() - var4.getY()) <= 1) {
            StandPlanner.Step var3;
            if ((var3 = this.route.get(this.routeIndex++)).move() != StandPlanner.Move.WALK && var3.move() != StandPlanner.Move.LEAP) {
               this.route = null;
               this.noPlanUntil = this.tick + 40L;
            } else {
               boolean var6 = var3.move() == StandPlanner.Move.WALK
                  && this.routeIndex < this.route.size()
                  && this.route.get(this.routeIndex).move() == StandPlanner.Move.WALK;
               this.hop = new Hop(var3.pos(), var6, var3.move() == StandPlanner.Move.LEAP);
               if (var3.move() == StandPlanner.Move.WALK && (Walker.risky(clientWorld, var5) || Walker.risky(clientWorld, var3.pos()))) {
                  if (var3.pos().getY() >= var5.getY()) {
                     this.hop.careful();
                  } else {
                     this.hop.slow();
                  }
               }
            }
         } else {
            this.release();
            this.route = null;
            this.noPlanUntil = this.tick + 10L;
         }
      }
   }

   private static boolean minable(ClientWorld clientWorld, BlockPos blockPos) {
      BlockState var2 = clientWorld.getBlockState(blockPos);
      return !var2.isAir() && !(var2.getBlock() instanceof FluidBlock) ? var2.getHardness(clientWorld, blockPos) >= 0.0F : false;
   }

   private boolean holdsFluid(ClientWorld clientWorld, BlockPos blockPos) {
      if (!clientWorld.getBlockState(blockPos).getFluidState().isEmpty()) {
         return true;
      } else {
         for (Direction var6 : Direction.values()) {
            if (var6 != Direction.DOWN && !clientWorld.getFluidState(blockPos.offset(var6)).isEmpty()) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean cutInside(ClientWorld clientWorld, BlockPos blockPos) {
      for (int var3 = -1; var3 <= 1; var3++) {
         for (int var4 = -1; var4 <= 1; var4++) {
            for (int var5 = -1; var5 <= 1; var5++) {
               BlockPos var6 = blockPos.add(var3, var4, var5);
               if (!this.inside(var6)) {
                  return false;
               }

               if (minable(clientWorld, var6) && this.holdsFluid(clientWorld, var6)) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private boolean inside(BlockPos blockPos) {
      return this.min != null
         && blockPos.getX() >= this.min.getX()
         && blockPos.getX() <= this.max.getX()
         && blockPos.getY() >= this.min.getY()
         && blockPos.getY() <= this.max.getY()
         && blockPos.getZ() >= this.min.getZ()
         && blockPos.getZ() <= this.max.getZ();
   }

   private static Predicate<ItemStack> toolFor(ClientPlayerEntity clientPlayerEntity, BlockState blockState, boolean bl) {
      PlayerInventory var3 = clientPlayerEntity.getInventory();
      ItemStack var4 = ItemStack.EMPTY;
      float var5 = 1.0F;

      for (int var6 = 0; var6 < 36; var6++) {
         ItemStack var8 = var3.getStack(var6);
         float var7;
         if (!var8.isEmpty()
            && isShard(var8) == bl
            && (
               (var7 = var8.getMiningSpeedMultiplier(blockState)) > var5
                  || var7 == var5 && !var4.isEmpty() && var8.isSuitableFor(blockState) && !var4.isSuitableFor(blockState)
            )) {
            var4 = var8;
            var5 = var7;
         }
      }

      if (var4.isEmpty()) {
         return itemStack -> !isShard(itemStack);
      } else {
         ItemStack var9 = var4;
         return itemStack2 -> ItemStack.areItemsAndComponentsEqual(itemStack2, var9);
      }
   }

   static boolean isShard(ItemStack itemStack) {
      return itemStack.isIn(ItemTags.PICKAXES) && TunnelMinerModule.shardName(itemStack);
   }

   private boolean mendDue(ClientPlayerEntity clientPlayerEntity) {
      if (this.mend.get() && (Hands.has(clientPlayerEntity, XP) || XP.test(clientPlayerEntity.getOffHandStack()))) {
         ItemStack var2 = clientPlayerEntity.getMainHandStack();
         return var2.isDamageable() && mendable(var2) ? var2.getMaxDamage() - var2.getDamage() < this.mendBelow.get() : false;
      } else {
         return false;
      }
   }

   private void mend(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var3 = clientPlayerEntity.getMainHandStack();
      boolean var4 = var3.isDamageable() && var3.getDamage() == 0;
      boolean var2 = !XP.test(clientPlayerEntity.getOffHandStack()) && !Hands.has(clientPlayerEntity, XP);
      if (var4 || var2 || this.tick - this.mendStarted > 1200L || !var3.isDamageable()) {
         this.mc.options.useKey.setPressed(false);
         this.mending = false;
         if (var4) {
            this.feedback("mended", "Tool mended", 200);
         } else if (var2) {
            this.feedback("no-xp", "Out of XP bottles, mining on", 200);
         }
      } else if (!XP.test(clientPlayerEntity.getOffHandStack())) {
         this.mc.options.useKey.setPressed(false);
         if (this.tick % 5L == 0L && InventoryCalm.still(clientPlayerEntity)) {
            Hands.toOffhand(this.mc, clientPlayerEntity, XP);
         }
      } else {
         Aim.turn(this.mc, clientPlayerEntity.getYaw(), 80.0F, 22.0F);
         this.mc.options.useKey.setPressed(clientPlayerEntity.getPitch() > 60.0F);
      }
   }

   private static boolean mendable(ItemStack itemStack) {
      return itemStack.getEnchantments().getEnchantments().stream().anyMatch(registryEntry -> registryEntry.matchesKey(Enchantments.MENDING));
   }

   private int countLeft(ClientWorld clientWorld) {
      int var2 = 0;
      Mutable var3 = new Mutable();

      for (int var4 = this.min.getY(); var4 <= this.max.getY(); var4++) {
         for (int var5 = this.min.getX(); var5 <= this.max.getX(); var5++) {
            for (int var6 = this.min.getZ(); var6 <= this.max.getZ(); var6++) {
               var3.set(var5, var4, var6);
               if (clientWorld.isChunkLoaded(var3) && minable(clientWorld, var3)) {
                  var2++;
               }
            }
         }
      }

      return var2;
   }

   private void finish() {
      this.release();
      ClientWorld var1 = this.mc.world;
      int var2 = var1 == null ? 0 : this.countLeft(var1);
      ModuleChat.Body var3 = ModuleChat.body().good("Area cleared").text(", " + Math.max(0, this.startCount - var2) + " blocks mined.");
      if (!this.unreachable.isEmpty()) {
         var3.text(" ")
            .warn(this.unreachable.size() + (this.unreachable.size() == 1 ? " block" : " blocks") + " could not be reached")
            .text(" (" + some(this.unreachable) + ").");
      }

      if (!this.holding.isEmpty()) {
         var3.text(
            " "
               + this.holding.size()
               + (this.holding.size() == 1 ? " block was" : " blocks were")
               + " left to hold back lava or water ("
               + some(this.holding)
               + ")."
         );
      }

      this.say(var3);
      this.requestDisable();
   }

   private static String some(Set<Long> set) {
      ArrayList var1 = new ArrayList();

      for (long var3 : set) {
         if (var1.size() == 4) {
            break;
         }

         var1.add(BlockPos.fromLong(var3).toShortString());
      }

      return String.join("; ", var1) + (set.size() > 4 ? "; ..." : "");
   }

   private void render(Render3DEvent render3DEvent) {
      if (this.picking != 0) {
         BlockPos var3 = this.aimedBlock();
         BlockPos var4 = this.picking == 2 ? this.firstPick : var3;
         BlockPos var2 = var3 != null ? var3 : var4;
         if (var4 != null) {
            GizmoDrawing.box(
               new Box(
                  Math.min(var4.getX(), var2.getX()),
                  Math.min(var4.getY(), var2.getY()),
                  Math.min(var4.getZ(), var2.getZ()),
                  Math.max(var4.getX(), var2.getX()) + 1,
                  Math.max(var4.getY(), var2.getY()) + 1,
                  Math.max(var4.getZ(), var2.getZ()) + 1
               ),
               DrawStyle.stroked(-11155201, 2.0F)
            );
            if (this.picking == 2) {
               GizmoDrawing.box(new Box(this.firstPick), DrawStyle.stroked(-20416, 2.0F));
            }
         }
      } else if (this.showArea.get() && this.min != null) {
         GizmoDrawing.box(
            new Box(this.min.getX(), this.min.getY(), this.min.getZ(), this.max.getX() + 1, this.max.getY() + 1, this.max.getZ() + 1),
            DrawStyle.stroked(-11155201, 2.0F)
         );
         if (this.breaker != null) {
            GizmoDrawing.box(this.breaker.pos(), DrawStyle.stroked(-20416, 2.0F));
         }
      }
   }

   private void setCorner(StringSetting stringSetting, boolean bl) {
      ClientPlayerEntity var3 = this.mc.player;
      if (var3 != null) {
         BlockPos var4 = var3.getBlockPos();
         if (bl) {
            HitResult var6 = this.mc.crosshairTarget;
            BlockHitResult var5;
            if (!(var6 instanceof BlockHitResult) || (var5 = (BlockHitResult)var6).getType() != Type.BLOCK) {
               this.say(ModuleChat.body().warn("Look at a block first."));
               return;
            }

            var4 = var5.getBlockPos();
         }

         stringSetting.set(var4.getX() + " " + var4.getY() + " " + var4.getZ());
         this.say(ModuleChat.body().text(stringSetting.name() + " set to " + var4.toShortString() + "."));
      }
   }

   private static BlockPos parse(String string) {
      String[] var1 = string.trim().replace(",", " ").split("\\s+");
      if (var1.length != 3) {
         return null;
      } else {
         try {
            return new BlockPos(Integer.parseInt(var1[0]), Integer.parseInt(var1[1]), Integer.parseInt(var1[2]));
         } catch (NumberFormatException var3) {
            return null;
         }
      }
   }

   private void release() {
      if (this.breaker != null) {
         this.breaker.release(this.mc);
      }

      this.breaker = null;
      if (this.hop != null) {
         this.hop.release(this.mc);
      }

      this.hop = null;
      if (this.eater != null) {
         this.eater.release(this.mc);
      }

      this.eater = null;
      this.mending = false;
      this.mc.options.useKey.setPressed(false);
      this.mc.options.forwardKey.setPressed(false);
      this.mc.options.backKey.setPressed(false);
      this.mc.options.leftKey.setPressed(false);
      this.mc.options.rightKey.setPressed(false);
      this.mc.options.jumpKey.setPressed(false);
      this.mc.options.sprintKey.setPressed(false);
      this.mc.options.sneakKey.setPressed(false);
      if (this.mc.interactionManager != null) {
         this.mc.interactionManager.cancelBlockBreaking();
      }
   }

   private void say(ModuleChat.Body body) {
      ModuleChat.send(this, body);
   }

   private void feedback(String string, String string2, int i) {
      if (this.chatFeedback.get()) {
         long var4 = this.lastFeedback.getOrDefault(string, -4611686018427387904L);
         if (this.tick - var4 >= i) {
            this.lastFeedback.put(string, this.tick);
            ModuleChat.send(this, string2);
         }
      }
   }
}
