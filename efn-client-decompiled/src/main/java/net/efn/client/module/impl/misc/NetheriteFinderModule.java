package net.efn.client.module.impl.misc;

import imgui.ImDrawList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.netherite.NetheriteChunkTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class NetheriteFinderModule extends ClientModule implements HudOverlayProvider {
   private static final int MAX_RENDERED_SECTIONS = 128;
   private static final long NOTIFY_COOLDOWN_MS = 2000L;
   private final ColorSetting color = this.addSetting("color", new ColorSetting("Near Color", -34304));
   private final ColorSetting farColor = this.addSetting("far-color", new ColorSetting("Far Color", -11688961));
   private final NumberSetting fade = this.addSetting("fade-distance", new NumberSetting("Fade Distance", 192.0F, 32.0F, 512.0F).step(8.0F));
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 512.0F, 64.0F, 1024.0F).step(1.0F));
   private final BooleanSetting tracers = this.addSetting("tracers", new BooleanSetting("Chunk Tracers", true));
   private final NumberSetting tracerCount = this.addSetting("tracer-count", new NumberSetting("Tracer Count", 3.0F, 1.0F, 8.0F).step(1.0F));
   private final BooleanSetting advanced = this.addSetting("advanced-finder", new BooleanSetting("Advanced Finder", false));
   private final BooleanSetting showProbes = this.addSetting("show-probes", new BooleanSetting("Show Probes", true));
   private final ModeSetting probeStyle = this.addSetting("probe-style", new ModeSetting("Probe Style", "Dots", "Dots", "Cubes", "Pulses"));
   private final NumberSetting missingFrom = this.addSetting("missing-from", new NumberSetting("Mark Missing From %", 60.0F, 30.0F, 100.0F).step(5.0F));
   private final AdvancedFinder advancedFinder = new AdvancedFinder(this);
   private final NetheriteVisuals visuals = new NetheriteVisuals();
   private int pendingCount;
   private BlockPos nearestPending;
   private double nearestPendingDistanceSq;
   private long lastNotifyAt;

   public NetheriteFinderModule() {
      super("netherite-finder", "Netherite Finder", "Boxes Nether chunk sections hiding ancient debris.", ModuleCategory.DONUT);
      this.usesNotifications().set(false);
      this.tracerCount.visibleWhen(this.tracers::get);
      this.showProbes.visibleWhen(this.advanced::get);
      this.probeStyle.visibleWhen(() -> this.advanced.get() && this.showProbes.get());
      this.missingFrom.visibleWhen(() -> this.advanced.get() && this.showProbes.get());
   }

   @Override
   protected void onEnable() {
      this.clearPending();
      this.lastNotifyAt = 0L;
      NetheriteChunkTracker.setListener(this::onCandidate);
      NetheriteChunkTracker.setRevealListener(this::onRevealed);
   }

   @Override
   protected void onDisable() {
      NetheriteChunkTracker.setListener(null);
      NetheriteChunkTracker.setRevealListener(null);
      this.advancedFinder.reset(MinecraftClient.getInstance());
      this.visuals.clear();
   }

   private void onRevealed(BlockPos blockPos) {
      if (this.advancedFinder.onRevealed(blockPos)) {
         this.visuals.burst(blockPos, this.color.argb());
      }
   }

   private void onCandidate(NetheriteChunkTracker.Candidate candidate) {
      if (this.notificationsOn()) {
         BlockPos var4 = new BlockPos((candidate.chunkX() << 4) + 8, (candidate.sectionY() << 4) + 8, (candidate.chunkZ() << 4) + 8);
         this.pendingCount++;
         MinecraftClient var5 = MinecraftClient.getInstance();
         double var2 = var5.player != null ? var5.player.squaredDistanceTo(var4.getX(), var4.getY(), var4.getZ()) : 0.0;
         if (this.nearestPending == null || var2 < this.nearestPendingDistanceSq) {
            this.nearestPending = var4;
            this.nearestPendingDistanceSq = var2;
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      this.flushNotifications();
      MinecraftClient var2 = MinecraftClient.getInstance();
      this.advancedFinder.maintain(var2);
      if (this.advanced.get()) {
         if (!this.advancedFinder.armed()) {
            this.advancedFinder.arm();
         }

         this.advancedFinder.tick(var2);
      } else if (this.advancedFinder.armed()) {
         this.advancedFinder.disarm(var2);
      }
   }

   private void flushNotifications() {
      if (this.pendingCount != 0) {
         if (!this.notificationsOn()) {
            this.clearPending();
         } else {
            long var1 = System.currentTimeMillis();
            if (var1 - this.lastNotifyAt >= 2000L) {
               ModuleChat.send(
                  this,
                  ModuleChat.body()
                     .value(this.pendingCount)
                     .text(" new hidden-debris chunk" + (this.pendingCount == 1 ? " " : "s "))
                     .good("found")
                     .text(", nearest at ")
                     .coords(this.nearestPending.getX(), this.nearestPending.getY(), this.nearestPending.getZ())
               );
               this.clearPending();
               this.lastNotifyAt = var1;
            }
         }
      }
   }

   private void clearPending() {
      this.pendingCount = 0;
      this.nearestPending = null;
      this.nearestPendingDistanceSq = 1.7976931348623157E308;
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world != null && var2.player != null && NetheriteChunkTracker.isNether(var2.world)) {
         Vec3d var3 = var2.player.getCameraPosVec(render3DEvent.partialTick);
         double var4 = this.range.get();
         ArrayList var6 = new ArrayList();

         for (NetheriteChunkTracker.Candidate var8 : NetheriteChunkTracker.snapshot()) {
            double var9;
            double var11;
            double var13;
            Box var15;
            double var16;
            if (!this.advancedFinder.isDone(var8)
               && (
                     var16 = Math.sqrt(
                        distanceSq(
                           var15 = new Box(
                              var13 = var8.chunkX() * 16.0,
                              var11 = var8.sectionY() * 16.0,
                              var9 = var8.chunkZ() * 16.0,
                              var13 + 16.0,
                              var11 + 16.0,
                              var9 + 16.0
                           ),
                           var3
                        )
                     )
                  )
                  <= var4) {
               var6.add(new NetheriteVisuals.Section(var15, var16, this.advancedFinder.progress(var8)));
            }
         }

         var6.sort(Comparator.comparingDouble(NetheriteVisuals.Section::distance));
         if (var6.size() > 128) {
            var6 = new ArrayList(var6.subList(0, 128));
         }

         NetheriteVisuals.Options var18 = new NetheriteVisuals.Options(
            this.color.argb(),
            this.farColor.argb(),
            this.fade.get(),
            var4,
            this.probeStyle.get(),
            this.missingFrom.get() / 100.0F,
            this.tracers.get() ? this.tracerCount.getInt() : 0,
            this.showProbes.get()
         );
         this.visuals.render(var2, render3DEvent.partialTick, var3, var6, List.copyOf(this.advancedFinder.revealed()), this.advancedFinder, var18);
      } else {
         this.visuals.clear();
      }
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.visuals.hasLabels();
   }

   @Override
   public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
      this.visuals.overlay(imDrawList, f, g);
   }

   private static double distanceSq(Box box, Vec3d vec3d) {
      double var2 = Math.max(0.0, Math.max(box.minX - vec3d.x, vec3d.x - box.maxX));
      double var4 = Math.max(0.0, Math.max(box.minY - vec3d.y, vec3d.y - box.maxY));
      double var6 = Math.max(0.0, Math.max(box.minZ - vec3d.z, vec3d.z - box.maxZ));
      return var2 * var2 + var4 * var4 + var6 * var6;
   }
}
