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
   private static final Predicate<ItemStack> XP = var0 -> var0.method_31574(Items.field_8287);
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
   private final MinecraftClient mc = MinecraftClient.method_1551();
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
      ClientPlayerEntity var1 = this.mc.field_1724;
      if (var1 != null && this.mc.field_1687 != null) {
         this.on(TickEvent.TYPE, Stage.PRE, var1x -> this.step());
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

   private boolean begin(BlockPos var1, BlockPos var2) {
      BlockPos var3 = new BlockPos(
         Math.min(var1.method_10263(), var2.method_10263()),
         Math.min(var1.method_10264(), var2.method_10264()),
         Math.min(var1.method_10260(), var2.method_10260())
      );
      BlockPos var4 = new BlockPos(
         Math.max(var1.method_10263(), var2.method_10263()),
         Math.max(var1.method_10264(), var2.method_10264()),
         Math.max(var1.method_10260(), var2.method_10260())
      );
      long var5 = (long)(var4.method_10263() - var3.method_10263() + 1)
         * (var4.method_10264() - var3.method_10264() + 1)
         * (var4.method_10260() - var3.method_10260() + 1);
      if (var5 > 2000000L) {
         this.say(ModuleChat.body().warn("The area is too big").text(" (" + var5 + " blocks, at most 2000000)."));
         return false;
      } else {
         this.reset();
         this.min = var3;
         this.max = var4;
         this.seenLevel = this.mc.field_1687;
         this.startCount = this.countLeft(this.mc.field_1687);
         this.say(ModuleChat.body().text("Mining " + this.startCount + " blocks between " + this.min.method_23854() + " and " + this.max.method_23854() + "."));
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

   private void onMouse(MouseInputEvent var1) {
      if (this.picking != 0 && var1.button == 0 && var1.action == 1) {
         if (this.mc.field_1755 == null && this.mc.field_1724 != null && this.mc.field_1687 != null) {
            var1.setCancelled(true);
            BlockPos var2 = this.aimedBlock();
            if (var2 == null) {
               this.say(ModuleChat.body().warn("No block there").text(", look at a block."));
            } else {
               String var3 = var2.method_10263() + " " + var2.method_10264() + " " + var2.method_10260();
               if (this.picking == 1) {
                  this.firstPick = var2;
                  this.corner1.set(var3);
                  this.picking = 2;
                  this.say(ModuleChat.body().text("Corner 1 set to " + var2.method_23854() + ". Hit the second corner block."));
               } else {
                  this.corner2.set(var3);
                  this.say(ModuleChat.body().text("Corner 2 set to " + var2.method_23854() + "."));
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
      ClientPlayerEntity var3 = this.mc.field_1724;
      if (var3 != null && this.mc.field_1687 != null) {
         FreecamModule var4 = ModuleRegistry.get(FreecamModule.class);
         Vec3d var1;
         Vec3d var2;
         if (var4 != null && var4.isEnabled()) {
            Camera var5 = this.mc.field_1773.method_19418();
            var2 = var5.method_71156();
            var1 = Vec3d.method_1030(var5.method_19329(), var5.method_19330());
         } else {
            var2 = var3.method_5836(1.0F);
            var1 = var3.method_5828(1.0F);
         }

         double var8 = this.mc.field_1690.method_38521() * 16.0;
         BlockHitResult var7 = this.mc
            .field_1687
            .method_17742(new RaycastContext(var2, var2.method_1019(var1.method_1021(var8)), ShapeType.field_17559, FluidHandling.field_1348, var3));
         return var7.method_17783() == Type.field_1332 ? var7.method_17777() : null;
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
      ClientPlayerEntity var1 = this.mc.field_1724;
      ClientWorld var2 = this.mc.field_1687;
      if (var1 != null && var2 != null && this.mc.field_1761 != null) {
         if (this.picking == 0 && this.min != null) {
            this.tick++;
            if (var2 != this.seenLevel || !var1.method_5805()) {
               this.say(ModuleChat.body().warn("Stopped").text(", the world changed or you died."));
               this.requestDisable();
            } else if (this.mc.field_1755 != null && this.pauseInMenus.get()) {
               this.release();
            } else if (this.eater != null) {
               if (this.eater.tick(this.mc) != Eater.Result.RUNNING) {
                  this.eater = null;
               }
            } else if (this.mending) {
               this.mend(var1);
            } else if (this.breaker != null) {
               BlockState var6 = var2.method_8320(this.breaker.pos());
               if (var6.method_26215() || Hands.hold(this.mc, var1, toolFor(var1, var6, this.breakerShard))) {
                  Breaker.Result var4 = this.breaker.tick(this.mc);
                  if (var4 != Breaker.Result.RUNNING) {
                     BlockPos var5 = this.breaker.pos();
                     this.breaker = null;
                     if (minable(var2, var5) && this.breakTries.merge(var5.method_10063(), 1, Integer::sum) >= 3) {
                        this.unreachable.add(var5.method_10063());
                        this.feedback("gave-up", "Could not mine " + var5.method_23854() + ", leaving it", 40);
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

   private boolean pickLayer(ClientWorld var1) {
      this.layerCells.clear();
      Mutable var2 = new Mutable();

      for (int var3 = this.max.method_10264(); var3 >= this.min.method_10264(); var3--) {
         for (int var4 = this.min.method_10263(); var4 <= this.max.method_10263(); var4++) {
            for (int var5 = this.min.method_10260(); var5 <= this.max.method_10260(); var5++) {
               var2.method_10103(var4, var3, var5);
               long var6 = var2.method_10063();
               if (!this.unreachable.contains(var6) && var1.method_22340(var2) && minable(var1, var2)) {
                  if (this.holdsFluid(var1, var2)) {
                     if (this.holding.add(var6)) {
                        this.feedback("fluid", "Leaving " + var2.method_23854() + ", it holds back lava or water", 100);
                     }
                  } else {
                     this.holding.remove(var6);
                     this.layerCells.add(var2.method_10062());
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

   private void dig(ClientWorld var1, ClientPlayerEntity var2) {
      if (!this.layerCells.isEmpty()) {
         Vec3d var3 = var2.method_33571();
         double var4 = var2.method_55754() - 0.3;
         ArrayList var6 = new ArrayList();

         for (BlockPos var8 : this.layerCells) {
            if (var8.method_46558().method_1022(var3) <= var4 && minable(var1, var8)) {
               var6.add(var8);
            }
         }

         BlockPos var13 = var2.method_24515().method_10074();
         var6.sort(Comparator.<BlockPos>comparingInt(var1x -> var1x.equals(var13) ? 1 : 0).thenComparingDouble(var1x -> var1x.method_46558().method_1025(var3)));

         for (BlockPos var9 : var6) {
            if (Aim.at(this.mc, var9) != null) {
               BlockState var10 = var1.method_8320(var9);
               boolean var11 = this.breakerShard = this.useShard.get() && this.cutInside(var1, var9);
               if (!Hands.hold(this.mc, var2, toolFor(var2, var10, this.breakerShard))) {
                  return;
               }

               this.breaker = new Breaker(var9, true);
               return;
            }
         }

         if (this.tick >= this.noPlanUntil) {
            ArrayList var15 = new ArrayList();
            Vec3d var16 = var2.method_73189();
            ArrayList var17 = new ArrayList<>(this.layerCells);
            var17.sort(Comparator.comparingDouble(var1x -> var1x.method_19770(var16)));

            for (BlockPos var12 : var17) {
               if (var15.size() >= 400) {
                  break;
               }

               var15.add(new StandPlanner.Target(var12, var1.method_8320(var12), StandPlanner.Use.BREAK));
            }

            this.planner = new StandPlanner(var1, var2, var15, var0 -> false, null, null, null, null);
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
            this.routeStart = this.mc.field_1724 == null ? null : this.mc.field_1724.method_24515();
         } else {
            this.noPlanUntil = this.tick + 40L;
            boolean var3 = false;

            for (BlockPos var5 : this.layerCells) {
               if (this.planFails.merge(var5.method_10063(), 1, Integer::sum) >= 2) {
                  this.unreachable.add(var5.method_10063());
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

   private void followRoute(ClientWorld var1, ClientPlayerEntity var2) {
      if (this.routeIndex >= this.route.size()) {
         this.route = null;
         this.nextScan = 0L;
      } else if (var2.method_24828()) {
         BlockPos var4 = this.routeIndex == 0 ? this.routeStart : this.route.get(this.routeIndex - 1).pos();
         BlockPos var5 = var2.method_24515();
         if (var4 == null
            || var5.method_10263() == var4.method_10263()
               && var5.method_10260() == var4.method_10260()
               && Math.abs(var5.method_10264() - var4.method_10264()) <= 1) {
            StandPlanner.Step var3;
            if ((var3 = this.route.get(this.routeIndex++)).move() != StandPlanner.Move.WALK && var3.move() != StandPlanner.Move.LEAP) {
               this.route = null;
               this.noPlanUntil = this.tick + 40L;
            } else {
               boolean var6 = var3.move() == StandPlanner.Move.WALK
                  && this.routeIndex < this.route.size()
                  && this.route.get(this.routeIndex).move() == StandPlanner.Move.WALK;
               this.hop = new Hop(var3.pos(), var6, var3.move() == StandPlanner.Move.LEAP);
               if (var3.move() == StandPlanner.Move.WALK && (Walker.risky(var1, var5) || Walker.risky(var1, var3.pos()))) {
                  if (var3.pos().method_10264() >= var5.method_10264()) {
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

   private static boolean minable(ClientWorld var0, BlockPos var1) {
      BlockState var2 = var0.method_8320(var1);
      return !var2.method_26215() && !(var2.method_26204() instanceof FluidBlock) ? var2.method_26214(var0, var1) >= 0.0F : false;
   }

   private boolean holdsFluid(ClientWorld var1, BlockPos var2) {
      if (!var1.method_8320(var2).method_26227().method_15769()) {
         return true;
      } else {
         for (Direction var6 : Direction.values()) {
            if (var6 != Direction.field_11033 && !var1.method_8316(var2.method_10093(var6)).method_15769()) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean cutInside(ClientWorld var1, BlockPos var2) {
      for (int var3 = -1; var3 <= 1; var3++) {
         for (int var4 = -1; var4 <= 1; var4++) {
            for (int var5 = -1; var5 <= 1; var5++) {
               BlockPos var6 = var2.method_10069(var3, var4, var5);
               if (!this.inside(var6)) {
                  return false;
               }

               if (minable(var1, var6) && this.holdsFluid(var1, var6)) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private boolean inside(BlockPos var1) {
      return this.min != null
         && var1.method_10263() >= this.min.method_10263()
         && var1.method_10263() <= this.max.method_10263()
         && var1.method_10264() >= this.min.method_10264()
         && var1.method_10264() <= this.max.method_10264()
         && var1.method_10260() >= this.min.method_10260()
         && var1.method_10260() <= this.max.method_10260();
   }

   private static Predicate<ItemStack> toolFor(ClientPlayerEntity var0, BlockState var1, boolean var2) {
      PlayerInventory var3 = var0.method_31548();
      ItemStack var4 = ItemStack.field_8037;
      float var5 = 1.0F;

      for (int var6 = 0; var6 < 36; var6++) {
         ItemStack var8 = var3.method_5438(var6);
         float var7;
         if (!var8.method_7960()
            && isShard(var8) == var2
            && ((var7 = var8.method_7924(var1)) > var5 || var7 == var5 && !var4.method_7960() && var8.method_7951(var1) && !var4.method_7951(var1))) {
            var4 = var8;
            var5 = var7;
         }
      }

      if (var4.method_7960()) {
         return var0x -> !isShard(var0x);
      } else {
         ItemStack var9 = var4;
         return var1x -> ItemStack.method_31577(var1x, var9);
      }
   }

   static boolean isShard(ItemStack var0) {
      return var0.method_31573(ItemTags.field_42614) && TunnelMinerModule.shardName(var0);
   }

   private boolean mendDue(ClientPlayerEntity var1) {
      if (this.mend.get() && (Hands.has(var1, XP) || XP.test(var1.method_6079()))) {
         ItemStack var2 = var1.method_6047();
         return var2.method_7963() && mendable(var2) ? var2.method_7936() - var2.method_7919() < this.mendBelow.get() : false;
      } else {
         return false;
      }
   }

   private void mend(ClientPlayerEntity var1) {
      ItemStack var3 = var1.method_6047();
      boolean var4 = var3.method_7963() && var3.method_7919() == 0;
      boolean var2 = !XP.test(var1.method_6079()) && !Hands.has(var1, XP);
      if (var4 || var2 || this.tick - this.mendStarted > 1200L || !var3.method_7963()) {
         this.mc.field_1690.field_1904.method_23481(false);
         this.mending = false;
         if (var4) {
            this.feedback("mended", "Tool mended", 200);
         } else if (var2) {
            this.feedback("no-xp", "Out of XP bottles, mining on", 200);
         }
      } else if (!XP.test(var1.method_6079())) {
         this.mc.field_1690.field_1904.method_23481(false);
         if (this.tick % 5L == 0L && InventoryCalm.still(var1)) {
            Hands.toOffhand(this.mc, var1, XP);
         }
      } else {
         Aim.turn(this.mc, var1.method_36454(), 80.0F, 22.0F);
         this.mc.field_1690.field_1904.method_23481(var1.method_36455() > 60.0F);
      }
   }

   private static boolean mendable(ItemStack var0) {
      return var0.method_58657().method_57534().stream().anyMatch(var0x -> var0x.method_40225(Enchantments.field_9101));
   }

   private int countLeft(ClientWorld var1) {
      int var2 = 0;
      Mutable var3 = new Mutable();

      for (int var4 = this.min.method_10264(); var4 <= this.max.method_10264(); var4++) {
         for (int var5 = this.min.method_10263(); var5 <= this.max.method_10263(); var5++) {
            for (int var6 = this.min.method_10260(); var6 <= this.max.method_10260(); var6++) {
               var3.method_10103(var5, var4, var6);
               if (var1.method_22340(var3) && minable(var1, var3)) {
                  var2++;
               }
            }
         }
      }

      return var2;
   }

   private void finish() {
      this.release();
      ClientWorld var1 = this.mc.field_1687;
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

   private static String some(Set<Long> var0) {
      ArrayList var1 = new ArrayList();

      for (long var3 : var0) {
         if (var1.size() == 4) {
            break;
         }

         var1.add(BlockPos.method_10092(var3).method_23854());
      }

      return String.join("; ", var1) + (var0.size() > 4 ? "; ..." : "");
   }

   private void render(Render3DEvent var1) {
      if (this.picking != 0) {
         BlockPos var3 = this.aimedBlock();
         BlockPos var4 = this.picking == 2 ? this.firstPick : var3;
         BlockPos var2 = var3 != null ? var3 : var4;
         if (var4 != null) {
            GizmoDrawing.method_75541(
               new Box(
                  Math.min(var4.method_10263(), var2.method_10263()),
                  Math.min(var4.method_10264(), var2.method_10264()),
                  Math.min(var4.method_10260(), var2.method_10260()),
                  Math.max(var4.method_10263(), var2.method_10263()) + 1,
                  Math.max(var4.method_10264(), var2.method_10264()) + 1,
                  Math.max(var4.method_10260(), var2.method_10260()) + 1
               ),
               DrawStyle.method_75536(-11155201, 2.0F)
            );
            if (this.picking == 2) {
               GizmoDrawing.method_75541(new Box(this.firstPick), DrawStyle.method_75536(-20416, 2.0F));
            }
         }
      } else if (this.showArea.get() && this.min != null) {
         GizmoDrawing.method_75541(
            new Box(
               this.min.method_10263(),
               this.min.method_10264(),
               this.min.method_10260(),
               this.max.method_10263() + 1,
               this.max.method_10264() + 1,
               this.max.method_10260() + 1
            ),
            DrawStyle.method_75536(-11155201, 2.0F)
         );
         if (this.breaker != null) {
            GizmoDrawing.method_75550(this.breaker.pos(), DrawStyle.method_75536(-20416, 2.0F));
         }
      }
   }

   private void setCorner(StringSetting var1, boolean var2) {
      ClientPlayerEntity var3 = this.mc.field_1724;
      if (var3 != null) {
         BlockPos var4 = var3.method_24515();
         if (var2) {
            HitResult var6 = this.mc.field_1765;
            BlockHitResult var5;
            if (!(var6 instanceof BlockHitResult) || (var5 = (BlockHitResult)var6).method_17783() != Type.field_1332) {
               this.say(ModuleChat.body().warn("Look at a block first."));
               return;
            }

            var4 = var5.method_17777();
         }

         var1.set(var4.method_10263() + " " + var4.method_10264() + " " + var4.method_10260());
         this.say(ModuleChat.body().text(var1.name() + " set to " + var4.method_23854() + "."));
      }
   }

   private static BlockPos parse(String var0) {
      String[] var1 = var0.trim().replace(",", " ").split("\\s+");
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
      this.mc.field_1690.field_1904.method_23481(false);
      this.mc.field_1690.field_1894.method_23481(false);
      this.mc.field_1690.field_1881.method_23481(false);
      this.mc.field_1690.field_1913.method_23481(false);
      this.mc.field_1690.field_1849.method_23481(false);
      this.mc.field_1690.field_1903.method_23481(false);
      this.mc.field_1690.field_1867.method_23481(false);
      this.mc.field_1690.field_1832.method_23481(false);
      if (this.mc.field_1761 != null) {
         this.mc.field_1761.method_2925();
      }
   }

   private void say(ModuleChat.Body var1) {
      ModuleChat.send(this, var1);
   }

   private void feedback(String var1, String var2, int var3) {
      if (this.chatFeedback.get()) {
         long var4 = this.lastFeedback.getOrDefault(var1, -4611686018427387904L);
         if (this.tick - var4 >= var3) {
            this.lastFeedback.put(var1, this.tick);
            ModuleChat.send(this, var2);
         }
      }
   }
}
