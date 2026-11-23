package net.efn.client.module.impl.movement;

import java.util.ArrayList;
import java.util.List;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.efn.client.module.impl.movement.entitycontrol.VehicleAntiKick;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.BoatPaddleStateC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

@Environment(EnvType.CLIENT)
public final class BoatNoclipModule extends ClientModule {
   public static volatile BoatNoclipModule active;
   private static final double MAX_MISS = 0.24;
   private static final double MAX_PACKET_LENGTH = 1.0;
   private static final int MAX_PACKETS_PER_TICK = 16;
   private static final double MAX_SERVER_LEAD = 19.0;
   private static final double MAX_UNCONFIRMED = 28.5;
   private static final int STALE_SERVER_TICKS = 40;
   private static final double MIN_BURST = 2.0;
   private static final double MOUNT_BURST = 4.0;
   private static final double BURST_RECOVERY = 0.025;
   private static final double CAUTION_STEP = 0.5;
   private static final double CAUTION_GROWTH = 1.25;
   private static final double LAVA_MARGIN = 3.0;
   private final NumberSetting speed = this.addSetting("speed", new NumberSetting("Speed", 10.0F, 0.0F, 320.0F).step(0.5F));
   private final NumberSetting speedInsideBlocks = this.addSetting(
      "speed-inside-blocks", new NumberSetting("Speed Inside Blocks", 5.0F, 0.0F, 320.0F).step(0.5F)
   );
   private final NumberSetting verticalSpeed = this.addSetting("vertical-speed", new NumberSetting("Vertical Speed", 6.0F, 0.0F, 320.0F).step(0.5F));
   private final NumberSetting fallSpeed = this.addSetting("fall-speed", new NumberSetting("Fall Speed", 0.0F, 0.0F, 10.0F).step(0.1F));
   private final BooleanSetting lavaProtection = this.addSetting("lava-protection", new BooleanSetting("Lava Protection", true));
   private final BooleanSetting antiKick = this.addSetting("anti-fly-kick", new BooleanSetting("Anti Fly Kick", true));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 40.0F, 1.0F, 80.0F).step(1.0F));
   private final VehicleAntiKick antiKicker = new VehicleAntiKick();
   private AbstractBoatEntity noclipBoat;
   private Vec3d serverPos;
   private Vec3d lastBase;
   private int staleTicks;
   private double cautionCap = 1.0 / 0.0;
   private boolean sendingStops;
   private int sinceBase;
   private double burst = 4.0;
   private double lastMoveLength;

   public BoatNoclipModule() {
      super("boat-noclip", "Boat Noclip", "Fly your boat through blocks and entities.", ModuleCategory.MOVEMENT);
      this.delay.visibleWhen(this.antiKick::get);
      this.antiKick.onChange(this.antiKicker::forget);
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      BoatFlyModule var1 = ModuleRegistry.get(BoatFlyModule.class);
      if (var1 != null) {
         var1.setEnabled(false);
      }

      this.antiKicker.start(this.delay.getInt());
      active = this;
      EntityControlVehicleHook.refresh();
      this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
      this.on(PacketSendEvent.TYPE, this::onPacketSend);
   }

   @Override
   protected void onDisable() {
      active = null;
      EntityControlVehicleHook.refresh();
      if (FreecamModule.active == null) {
         MinecraftClient.method_1551().field_1730 = true;
      }

      this.antiKicker.stop();
      this.releaseBoat();
   }

   private void onPreTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (this.noclipBoat != null && (var3 == null || var3.method_5854() != this.noclipBoat)) {
         this.releaseBoat();
      }

      this.antiKicker.tick();
      this.updateCulling(var2);
   }

   private void updateCulling(MinecraftClient var1) {
      if (FreecamModule.active == null && var1.field_1687 != null) {
         boolean var2 = this.noclipBoat != null && var1.field_1687.method_8320(var1.field_1773.method_19418().method_19328()).method_26216();
         var1.field_1730 = !var2;
      }
   }

   private void onPacketSend(PacketSendEvent var1) {
      if (this.antiKick.get()) {
         Packet var2 = var1.packet;
         if (var1.packet instanceof VehicleMoveC2SPacket) {
            VehicleMoveC2SPacket var3 = (VehicleMoveC2SPacket)var2;
            if (this.sendingStops) {
               this.antiKicker.observe(var3);
            } else if (this.antiKicker.onPacketSend(var3, this.delay.getInt())) {
               var1.setCancelled(true);
            }

            return;
         }
      }
   }

   public void onVehicleSetback() {
      this.antiKicker.forget();
      if (this.noclipBoat != null) {
         this.serverPos = this.noclipBoat.method_73189();
      }

      this.cautionCap = 0.5;
   }

   private void releaseBoat() {
      if (this.noclipBoat != null) {
         this.noclipBoat.field_5960 = false;
      }

      this.noclipBoat = null;
      this.serverPos = null;
      this.lastBase = null;
      this.sinceBase = 0;
      this.burst = 4.0;
   }

   public Vec3d boatMovement(AbstractBoatEntity var1, ClientPlayerEntity var2) {
      if (this.noclipBoat != var1) {
         this.releaseBoat();
         this.noclipBoat = var1;
      }

      var1.field_5960 = true;
      forceServerBroadcast(var1);
      double var3 = this.serverBudget(var1);
      if (this.antiKick.get() && this.antiKicker.isHolding()) {
         return Vec3d.field_1353;
      } else {
         var3 = Math.min(var3, this.cautionCap);
         this.cautionCap *= 1.25;
         double var5 = isInsideBlock(var1, var2) ? this.speedInsideBlocks.get() : this.speed.get();
         Vec3d var7 = EntityControlVehicleHook.horizontalVelocity(var2, var5);
         MinecraftClient var8 = MinecraftClient.method_1551();
         double var9 = 0.0;
         if (EntityControlVehicleHook.isPhysicallyDown(var8.field_1690.field_1903)) {
            var9 += this.verticalSpeed.get() / 20.0;
         }

         double var16;
         double var17;
         var9 = EntityControlVehicleHook.isPhysicallyDown(var8.field_1690.field_1867)
            ? (var16 = var9 - this.verticalSpeed.get() / 20.0)
            : (var17 = var9 - this.fallSpeed.get() / 20.0);
         Vec3d var11 = new Vec3d(var7.field_1352, var9, var7.field_1350);
         double var12 = var11.method_1033();
         if (var12 > var3) {
            var11 = var11.method_1021(var3 / var12);
         }

         Vec3d var14 = this.planMove(var1, var11);
         this.lastMoveLength = var14.method_1033();
         return var14;
      }
   }

   private static void forceServerBroadcast(AbstractBoatEntity var0) {
      ClientPlayNetworkHandler var1 = MinecraftClient.method_1551().method_1562();
      if (var1 != null) {
         var1.method_52787(new BoatPaddleStateC2SPacket(!var0.method_64491(0), var0.method_64491(1)));
      }
   }

   private double serverBudget(AbstractBoatEntity var1) {
      Vec3d var2 = var1.method_43389().method_60933();
      if (!var2.equals(this.lastBase)) {
         this.burst = Math.max(this.burst, (double)(this.sinceBase + 1));
         this.sinceBase = 0;
         this.lastBase = var2;
         this.serverPos = var2;
         this.staleTicks = 0;
      } else {
         if (this.lastMoveLength > 0.01) {
            this.sinceBase++;
         }

         if (++this.staleTicks >= 40) {
            this.serverPos = var1.method_73189();
         }
      }

      this.burst = Math.max(2.0, this.burst - 0.025);
      if (this.serverPos == null) {
         this.serverPos = var1.method_73189();
      }

      double var3 = var1.method_73189().method_1022(this.serverPos);
      return Math.max(0.0, Math.min(19.0 / this.burst, 28.5 - var3));
   }

   private Vec3d planMove(AbstractBoatEntity var1, Vec3d var2) {
      Vec3d var7;
      Vec3d var8 = var7 = var1.method_73189();
      Vec3d var9 = var2;
      List var10 = this.lavaProtection.get() ? nearbyLava(var1, var2) : List.of();
      ArrayList var11 = new ArrayList();

      Vec3d var3;
      Vec3d var4;
      double var5;
      while (
         var11.size() < 16
            && var9.method_1027() > 1.0E-8
            && !((var5 = allowedFraction(var1, var8, var4 = var9.method_1021(Math.min(1.0, 1.0 / var9.method_1033())))) <= 0.0)
            && !reachesUnloadedChunk(var1, var8, var3 = var4.method_1021(var5))
            && (var10.isEmpty() || !approachesLava(var1, var8, var3, var10))
      ) {
         var8 = var8.method_1019(var3);
         var9 = var9.method_1020(var3);
         var11.add(var8);
      }

      ClientPlayNetworkHandler var12 = MinecraftClient.method_1551().method_1562();
      if (var12 != null) {
         this.sendingStops = true;

         try {
            for (int var13 = 0; var13 < var11.size() - 1; var13++) {
               var12.method_52787(new VehicleMoveC2SPacket((Vec3d)var11.get(var13), var1.method_36454(), var1.method_36455(), false));
            }
         } finally {
            this.sendingStops = false;
         }
      }

      return var8.method_1020(var7);
   }

   private static List<Box> nearbyLava(AbstractBoatEntity var0, Vec3d var1) {
      Box var2 = var0.method_5829().method_18804(var1).method_1014(4.0);
      ArrayList var3 = new ArrayList();

      for (BlockPos var5 : BlockPos.method_10094(
         MathHelper.method_15357(var2.field_1323),
         MathHelper.method_15357(var2.field_1322),
         MathHelper.method_15357(var2.field_1321),
         MathHelper.method_15357(var2.field_1320),
         MathHelper.method_15357(var2.field_1325),
         MathHelper.method_15357(var2.field_1324)
      )) {
         if (var0.method_73183().method_8316(var5).method_15767(FluidTags.field_15518)) {
            var3.add(new Box(var5));
         }
      }

      return var3;
   }

   private static boolean approachesLava(AbstractBoatEntity var0, Vec3d var1, Vec3d var2, List<Box> var3) {
      Box var4 = var0.method_5829().method_997(var1.method_1020(var0.method_73189()));
      double var5 = distanceTo(var4.method_997(var2), var3);
      return var5 < 3.0 && var5 < distanceTo(var4, var3);
   }

   private static double distanceTo(Box var0, List<Box> var1) {
      double var2 = 1.7976931348623157E308;

      for (Box var5 : var1) {
         double var6 = Math.max(0.0, Math.max(var5.field_1323 - var0.field_1320, var0.field_1323 - var5.field_1320));
         double var8 = Math.max(0.0, Math.max(var5.field_1322 - var0.field_1325, var0.field_1322 - var5.field_1325));
         double var10 = Math.max(0.0, Math.max(var5.field_1321 - var0.field_1324, var0.field_1321 - var5.field_1324));
         var2 = Math.min(var2, var6 * var6 + var8 * var8 + var10 * var10);
      }

      return Math.sqrt(var2);
   }

   private static boolean reachesUnloadedChunk(AbstractBoatEntity var0, Vec3d var1, Vec3d var2) {
      Box var3 = var0.method_5829().method_997(var1.method_1020(var0.method_73189())).method_18804(var2);

      for (int var4 = ChunkSectionPos.method_42615(var3.field_1323); var4 <= ChunkSectionPos.method_42615(var3.field_1320); var4++) {
         for (int var5 = ChunkSectionPos.method_42615(var3.field_1321); var5 <= ChunkSectionPos.method_42615(var3.field_1324); var5++) {
            if (!var0.method_73183().method_8398().method_12123(var4, var5)) {
               return true;
            }
         }
      }

      return false;
   }

   private static double allowedFraction(AbstractBoatEntity var0, Vec3d var1, Vec3d var2) {
      Box var4 = var0.method_5829().method_997(var1.method_1020(var0.method_73189()));
      List var3;
      if (replayMiss(var0, var4, var2, var3 = serverEntityShapes(var0, var4.method_18804(var2))) <= 0.24) {
         return 1.0;
      } else {
         double var5 = 0.0;
         double var7 = 1.0;

         for (int var9 = 0; var9 < 12; var9++) {
            double var10 = (var5 + var7) / 2.0;
            if (replayMiss(var0, var4, var2.method_1021(var10), var3) <= 0.24) {
               var5 = var10;
            } else {
               var7 = var10;
            }
         }

         return var5;
      }
   }

   private static double replayMiss(AbstractBoatEntity var0, Box var1, Vec3d var2, List<VoxelShape> var3) {
      Vec3d var4 = Entity.method_20736(var0, var2, var1, var0.method_73183(), var3);
      return Math.hypot(var2.field_1352 - var4.field_1352, var2.field_1350 - var4.field_1350);
   }

   private static List<VoxelShape> serverEntityShapes(AbstractBoatEntity var0, Box var1) {
      ArrayList var2 = new ArrayList();

      for (Entity var4 : var0.method_73183().method_8333(var0, var1.method_1014(1.0E-7), EntityPredicates.field_6155)) {
         if (AbstractBoatEntity.method_64486(var0, var4)) {
            var2.add(VoxelShapes.method_1078(var4.method_5829()));
         }
      }

      return var2;
   }

   public static boolean hidesWallOverlay(PlayerEntity var0) {
      return active != null && var0.method_5854() instanceof AbstractBoatEntity;
   }

   private static boolean isInsideBlock(AbstractBoatEntity var0, ClientPlayerEntity var1) {
      ClientWorld var2 = MinecraftClient.method_1551().field_1687;
      if (var2 == null) {
         return false;
      } else {
         Box var3 = var0.method_5829();
         Box var4 = var3.method_1002(0.0, 0.05, 0.0).method_1009(0.5, 0.0, 0.5);
         int var5 = MathHelper.method_15357(var4.field_1320);
         int var6 = MathHelper.method_15357(var4.field_1325);
         int var7 = MathHelper.method_15357(var4.field_1324);
         Mutable var8 = new Mutable();

         for (int var9 = MathHelper.method_15357(var4.field_1323); var9 <= var5; var9++) {
            for (int var10 = MathHelper.method_15357(var4.field_1322); var10 <= var6; var10++) {
               for (int var11 = MathHelper.method_15357(var4.field_1321); var11 <= var7; var11++) {
                  BlockState var13 = var2.method_8320(var8.method_10103(var9, var10, var11));
                  VoxelShape var12;
                  if (!var13.method_26215()
                     && !(var12 = var13.method_26194(var2, var8, ShapeContext.method_16194())).method_1110()
                     && var3.method_994(var12.method_1107().method_996(var8))) {
                     return true;
                  }
               }
            }
         }

         for (Entity var15 : var2.method_8335(var0, var4)) {
            if (var15 != var1 && !var15.method_5626(var0) && !var0.method_5626(var15) && var15.method_5805() && var15.method_5829().method_994(var4)) {
               return true;
            }
         }

         return false;
      }
   }
}
