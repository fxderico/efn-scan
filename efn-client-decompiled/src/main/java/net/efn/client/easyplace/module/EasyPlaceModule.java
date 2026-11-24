package net.efn.client.easyplace.module;

import java.util.HashMap;
import java.util.Map;
import net.efn.client.chat.ModuleChat;
import net.efn.client.easyplace.builder.SchematicBuilderModule;
import net.efn.client.easyplace.compat.BaritoneCompat;
import net.efn.client.easyplace.inventory.InventoryPickSourceMode;
import net.efn.client.easyplace.placement.PlacementConfig;
import net.efn.client.easyplace.placement.PlacementManager;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.FoglessLines;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class EasyPlaceModule extends ClientModule {
   private static final int FOV_COLOR = -1275016961;
   private static final double FOV_LENGTH = 6.0;
   private final BooleanSetting allBlocks = this.addSetting("all-blocks", new BooleanSetting("All Blocks", true));
   private final BooleanSetting rotationSpoof = this.addSetting("rotation-spoof", new BooleanSetting("Rotation Spoof", true));
   private final NumberSetting maxYawChange = this.addSetting("max-yaw-change", new NumberSetting("Max Yaw Change", 120.0F, 30.0F, 180.0F).step(1.0F));
   private final ModeSetting inventorySource = this.addSetting(
      "inventory-source", new ModeSetting("Inventory Source", InventoryPickSourceMode.HOTBAR_ONLY.title(), InventoryPickSourceMode.titles())
   );
   private final BooleanSetting schematicOnly = this.addSetting("schematic-only", new BooleanSetting("Schematic Only", true));
   private final NumberSetting tickDelay = this.addSetting("tick-delay", new NumberSetting("Tick Delay", 0.0F, 0.0F, 10.0F).step(1.0F));
   private final BooleanSetting fovFilter = this.addSetting("fov-filter", new BooleanSetting("FOV Filter", false));
   private final BooleanSetting drawFov = this.addSetting("draw-fov", new BooleanSetting("Draw FOV", false));
   private final NumberSetting fovDegrees = this.addSetting("fov-degrees", new NumberSetting("FOV Degrees", 60.0F, 10.0F, 180.0F).step(1.0F));
   private final BooleanSetting schematicAura = this.addSetting("schematic-aura", new BooleanSetting("Schematic Aura", true));
   private final NumberSetting auraRange = this.addSetting("aura-range", new NumberSetting("Aura Range", 4.0F, 1.0F, 8.0F).step(0.1F));
   private final BooleanSetting hopperForceFacing = this.addSetting("hopper-force-facing", new BooleanSetting("Hopper Force Facing", true));
   private final BooleanSetting baritoneWalk = this.addSetting("baritone-walk", new BooleanSetting("Baritone Walk", false));
   private final BooleanSetting chatFeedback = this.addSetting("chat-feedback", new BooleanSetting("Chat Feedback", true));
   private final BooleanSetting debug = this.addSetting("debug", new BooleanSetting("Debug Log", false));
   private final PlacementManager placementManager = new PlacementManager(MinecraftClient.getInstance(), this::feedback);
   private final Map<String, Long> lastFeedbackTick = new HashMap<>();
   private long moduleTick;

   public EasyPlaceModule() {
      super("easy-place", "Easy Place", "Places missing blocks from active Litematica placements and waits for server confirmation.", ModuleCategory.DONUT);
      this.drawFov.visibleWhen(this.fovFilter::get);
      this.fovDegrees.visibleWhen(this.fovFilter::get);
   }

   @Override
   protected void onEnable() {
      SchematicBuilderModule var1 = ModuleRegistry.get(SchematicBuilderModule.class);
      if (var1 != null && var1.isEnabled()) {
         var1.setEnabled(false);
      }

      this.placementManager.start();
      if (this.baritoneWalk.get()) {
         ModuleChat.send(this, BaritoneCompat.isAvailable() ? "Baritone detected, walking is on" : "Baritone not found, walking is off");
      }

      this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.placementManager.tickPre(this.config()));
      this.on(TickEvent.TYPE, Stage.POST, tickEvent -> {
         this.moduleTick++;
         this.placementManager.tick(this.config());
      });
      this.on(Render3DEvent.TYPE, Stage.POST, this::renderFov);
   }

   @Override
   protected void onDisable() {
      this.placementManager.reset();
      this.lastFeedbackTick.clear();
      BaritoneCompat.cancel();
   }

   private void renderFov(Render3DEvent render3DEvent) {
      if (this.fovFilter.get() && this.drawFov.get()) {
         ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
         if (var2 != null) {
            Vec3d var3 = var2.getCameraPosVec(render3DEvent.partialTick);
            double var4 = Math.toRadians(var2.getYaw(render3DEvent.partialTick));
            double var6 = Math.toRadians(var2.getPitch(render3DEvent.partialTick));
            double var8 = Math.tan(Math.toRadians(this.fovDegrees.get() * 0.5));
            Vec3d var10 = new Vec3d(-Math.sin(var4) * Math.cos(var6), -Math.sin(var6), Math.cos(var4) * Math.cos(var6));
            Vec3d var11 = new Vec3d(Math.cos(var4), 0.0, Math.sin(var4)).multiply(var8);
            Vec3d var12 = new Vec3d(-Math.sin(var4) * Math.sin(var6), Math.cos(var6), Math.cos(var4) * Math.sin(var6)).multiply(var8);
            Vec3d[] var13 = new Vec3d[]{
               var10.add(var11).add(var12), var10.subtract(var11).add(var12), var10.subtract(var11).subtract(var12), var10.add(var11).subtract(var12)
            };
            FoglessLines var14 = FoglessLines.forFrame(render3DEvent.partialTick);

            for (int var15 = 0; var15 < var13.length; var15++) {
               Vec3d var16 = var3.add(var13[var15].multiply(6.0));
               Vec3d var17 = var3.add(var13[(var15 + 1) % var13.length].multiply(6.0));
               var14.line(var3, var16, -1275016961, 1.5F);
               var14.line(var16, var17, -1275016961, 1.5F);
            }
         }
      }
   }

   private PlacementConfig config() {
      return new PlacementConfig(
         this.allBlocks.get(),
         this.rotationSpoof.get(),
         this.maxYawChange.get(),
         InventoryPickSourceMode.of(this.inventorySource.get()),
         this.schematicOnly.get(),
         this.tickDelay.getInt(),
         this.schematicAura.get(),
         this.auraRange.get(),
         this.hopperForceFacing.get(),
         this.baritoneWalk.get(),
         this.debug.get(),
         this.fovFilter.get(),
         this.fovDegrees.get()
      );
   }

   private void feedback(String string, String string2, int i) {
      if (this.chatFeedback.get()) {
         long var4 = this.lastFeedbackTick.getOrDefault(string, -4611686018427387904L);
         if (this.moduleTick - var4 >= i) {
            this.lastFeedbackTick.put(string, this.moduleTick);
            ModuleChat.send(this, string2);
         }
      }
   }
}
