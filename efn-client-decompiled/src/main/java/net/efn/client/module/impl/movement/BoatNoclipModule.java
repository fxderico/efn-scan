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
         MinecraftClient.getInstance().chunkCullingEnabled = true;
      }

      this.antiKicker.stop();
      this.releaseBoat();
   }

   private void onPreTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (this.noclipBoat != null && (var3 == null || var3.getVehicle() != this.noclipBoat)) {
         this.releaseBoat();
      }

      this.antiKicker.tick();
      this.updateCulling(var2);
   }

   private void updateCulling(MinecraftClient minecraftClient) {
      if (FreecamModule.active == null && minecraftClient.world != null) {
         boolean var2 = this.noclipBoat != null
            && minecraftClient.world.getBlockState(minecraftClient.gameRenderer.getCamera().getBlockPos()).isOpaqueFullCube();
         minecraftClient.chunkCullingEnabled = !var2;
      }
   }

   private void onPacketSend(PacketSendEvent packetSendEvent) {
      if (this.antiKick.get()) {
         Packet var2 = packetSendEvent.packet;
         if (packetSendEvent.packet instanceof VehicleMoveC2SPacket) {
            VehicleMoveC2SPacket var3 = (VehicleMoveC2SPacket)var2;
            if (this.sendingStops) {
               this.antiKicker.observe(var3);
            } else if (this.antiKicker.onPacketSend(var3, this.delay.getInt())) {
               packetSendEvent.setCancelled(true);
            }

            return;
         }
      }
   }

   public void onVehicleSetback() {
      this.antiKicker.forget();
      if (this.noclipBoat != null) {
         this.serverPos = this.noclipBoat.getEntityPos();
      }

      this.cautionCap = 0.5;
   }

   private void releaseBoat() {
      if (this.noclipBoat != null) {
         this.noclipBoat.noClip = false;
      }

      this.noclipBoat = null;
      this.serverPos = null;
      this.lastBase = null;
      this.sinceBase = 0;
      this.burst = 4.0;
   }

   public Vec3d boatMovement(AbstractBoatEntity abstractBoatEntity, ClientPlayerEntity clientPlayerEntity) {
      if (this.noclipBoat != abstractBoatEntity) {
         this.releaseBoat();
         this.noclipBoat = abstractBoatEntity;
      }

      abstractBoatEntity.noClip = true;
      forceServerBroadcast(abstractBoatEntity);
      double var3 = this.serverBudget(abstractBoatEntity);
      if (this.antiKick.get() && this.antiKicker.isHolding()) {
         return Vec3d.ZERO;
      } else {
         var3 = Math.min(var3, this.cautionCap);
         this.cautionCap *= 1.25;
         double var5 = isInsideBlock(abstractBoatEntity, clientPlayerEntity) ? this.speedInsideBlocks.get() : this.speed.get();
         Vec3d var7 = EntityControlVehicleHook.horizontalVelocity(clientPlayerEntity, var5);
         MinecraftClient var8 = MinecraftClient.getInstance();
         double var9 = 0.0;
         if (EntityControlVehicleHook.isPhysicallyDown(var8.options.jumpKey)) {
            var9 += this.verticalSpeed.get() / 20.0;
         }

         double var16;
         double var17;
         var9 = EntityControlVehicleHook.isPhysicallyDown(var8.options.sprintKey)
            ? (var16 = var9 - this.verticalSpeed.get() / 20.0)
            : (var17 = var9 - this.fallSpeed.get() / 20.0);
         Vec3d var11 = new Vec3d(var7.x, var9, var7.z);
         double var12 = var11.length();
         if (var12 > var3) {
            var11 = var11.multiply(var3 / var12);
         }

         Vec3d var14 = this.planMove(abstractBoatEntity, var11);
         this.lastMoveLength = var14.length();
         return var14;
      }
   }

   private static void forceServerBroadcast(AbstractBoatEntity abstractBoatEntity) {
      ClientPlayNetworkHandler var1 = MinecraftClient.getInstance().getNetworkHandler();
      if (var1 != null) {
         var1.sendPacket(new BoatPaddleStateC2SPacket(!abstractBoatEntity.isPaddleMoving(0), abstractBoatEntity.isPaddleMoving(1)));
      }
   }

   private double serverBudget(AbstractBoatEntity abstractBoatEntity) {
      Vec3d var2 = abstractBoatEntity.getTrackedPosition().getPos();
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
            this.serverPos = abstractBoatEntity.getEntityPos();
         }
      }

      this.burst = Math.max(2.0, this.burst - 0.025);
      if (this.serverPos == null) {
         this.serverPos = abstractBoatEntity.getEntityPos();
      }

      double var3 = abstractBoatEntity.getEntityPos().distanceTo(this.serverPos);
      return Math.max(0.0, Math.min(19.0 / this.burst, 28.5 - var3));
   }

   private Vec3d planMove(AbstractBoatEntity abstractBoatEntity, Vec3d vec3d) {
      Vec3d var7;
      Vec3d var8 = var7 = abstractBoatEntity.getEntityPos();
      Vec3d var9 = vec3d;
      List var10 = this.lavaProtection.get() ? nearbyLava(abstractBoatEntity, vec3d) : List.of();
      ArrayList var11 = new ArrayList();

      Vec3d var3;
      Vec3d var4;
      double var5;
      while (
         var11.size() < 16
            && var9.lengthSquared() > 1.0E-8
            && !((var5 = allowedFraction(abstractBoatEntity, var8, var4 = var9.multiply(Math.min(1.0, 1.0 / var9.length())))) <= 0.0)
            && !reachesUnloadedChunk(abstractBoatEntity, var8, var3 = var4.multiply(var5))
            && (var10.isEmpty() || !approachesLava(abstractBoatEntity, var8, var3, var10))
      ) {
         var8 = var8.add(var3);
         var9 = var9.subtract(var3);
         var11.add(var8);
      }

      ClientPlayNetworkHandler var12 = MinecraftClient.getInstance().getNetworkHandler();
      if (var12 != null) {
         this.sendingStops = true;

         try {
            for (int var13 = 0; var13 < var11.size() - 1; var13++) {
               var12.sendPacket(new VehicleMoveC2SPacket((Vec3d)var11.get(var13), abstractBoatEntity.getYaw(), abstractBoatEntity.getPitch(), false));
            }
         } finally {
            this.sendingStops = false;
         }
      }

      return var8.subtract(var7);
   }

   private static List<Box> nearbyLava(AbstractBoatEntity abstractBoatEntity, Vec3d vec3d) {
      Box var2 = abstractBoatEntity.getBoundingBox().stretch(vec3d).expand(4.0);
      ArrayList var3 = new ArrayList();

      for (BlockPos var5 : BlockPos.iterate(
         MathHelper.floor(var2.minX),
         MathHelper.floor(var2.minY),
         MathHelper.floor(var2.minZ),
         MathHelper.floor(var2.maxX),
         MathHelper.floor(var2.maxY),
         MathHelper.floor(var2.maxZ)
      )) {
         if (abstractBoatEntity.getEntityWorld().getFluidState(var5).isIn(FluidTags.LAVA)) {
            var3.add(new Box(var5));
         }
      }

      return var3;
   }

   private static boolean approachesLava(AbstractBoatEntity abstractBoatEntity, Vec3d vec3d, Vec3d vec3d2, List<Box> list) {
      Box var4 = abstractBoatEntity.getBoundingBox().offset(vec3d.subtract(abstractBoatEntity.getEntityPos()));
      double var5 = distanceTo(var4.offset(vec3d2), list);
      return var5 < 3.0 && var5 < distanceTo(var4, list);
   }

   private static double distanceTo(Box box, List<Box> list) {
      double var2 = 1.7976931348623157E308;

      for (Box var5 : list) {
         double var6 = Math.max(0.0, Math.max(var5.minX - box.maxX, box.minX - var5.maxX));
         double var8 = Math.max(0.0, Math.max(var5.minY - box.maxY, box.minY - var5.maxY));
         double var10 = Math.max(0.0, Math.max(var5.minZ - box.maxZ, box.minZ - var5.maxZ));
         var2 = Math.min(var2, var6 * var6 + var8 * var8 + var10 * var10);
      }

      return Math.sqrt(var2);
   }

   private static boolean reachesUnloadedChunk(AbstractBoatEntity abstractBoatEntity, Vec3d vec3d, Vec3d vec3d2) {
      Box var3 = abstractBoatEntity.getBoundingBox().offset(vec3d.subtract(abstractBoatEntity.getEntityPos())).stretch(vec3d2);

      for (int var4 = ChunkSectionPos.getSectionCoordFloored(var3.minX); var4 <= ChunkSectionPos.getSectionCoordFloored(var3.maxX); var4++) {
         for (int var5 = ChunkSectionPos.getSectionCoordFloored(var3.minZ); var5 <= ChunkSectionPos.getSectionCoordFloored(var3.maxZ); var5++) {
            if (!abstractBoatEntity.getEntityWorld().getChunkManager().isChunkLoaded(var4, var5)) {
               return true;
            }
         }
      }

      return false;
   }

   private static double allowedFraction(AbstractBoatEntity abstractBoatEntity, Vec3d vec3d, Vec3d vec3d2) {
      Box var4 = abstractBoatEntity.getBoundingBox().offset(vec3d.subtract(abstractBoatEntity.getEntityPos()));
      List var3;
      if (replayMiss(abstractBoatEntity, var4, vec3d2, var3 = serverEntityShapes(abstractBoatEntity, var4.stretch(vec3d2))) <= 0.24) {
         return 1.0;
      } else {
         double var5 = 0.0;
         double var7 = 1.0;

         for (int var9 = 0; var9 < 12; var9++) {
            double var10 = (var5 + var7) / 2.0;
            if (replayMiss(abstractBoatEntity, var4, vec3d2.multiply(var10), var3) <= 0.24) {
               var5 = var10;
            } else {
               var7 = var10;
            }
         }

         return var5;
      }
   }

   private static double replayMiss(AbstractBoatEntity abstractBoatEntity, Box box, Vec3d vec3d, List<VoxelShape> list) {
      Vec3d var4 = Entity.adjustMovementForCollisions(abstractBoatEntity, vec3d, box, abstractBoatEntity.getEntityWorld(), list);
      return Math.hypot(vec3d.x - var4.x, vec3d.z - var4.z);
   }

   private static List<VoxelShape> serverEntityShapes(AbstractBoatEntity abstractBoatEntity, Box box) {
      ArrayList var2 = new ArrayList();

      for (Entity var4 : abstractBoatEntity.getEntityWorld().getOtherEntities(abstractBoatEntity, box.expand(1.0E-7), EntityPredicates.EXCEPT_SPECTATOR)) {
         if (AbstractBoatEntity.canCollide(abstractBoatEntity, var4)) {
            var2.add(VoxelShapes.cuboid(var4.getBoundingBox()));
         }
      }

      return var2;
   }

   public static boolean hidesWallOverlay(PlayerEntity playerEntity) {
      return active != null && playerEntity.getVehicle() instanceof AbstractBoatEntity;
   }

   private static boolean isInsideBlock(AbstractBoatEntity abstractBoatEntity, ClientPlayerEntity clientPlayerEntity) {
      ClientWorld var2 = MinecraftClient.getInstance().world;
      if (var2 == null) {
         return false;
      } else {
         Box var3 = abstractBoatEntity.getBoundingBox();
         Box var4 = var3.shrink(0.0, 0.05, 0.0).expand(0.5, 0.0, 0.5);
         int var5 = MathHelper.floor(var4.maxX);
         int var6 = MathHelper.floor(var4.maxY);
         int var7 = MathHelper.floor(var4.maxZ);
         Mutable var8 = new Mutable();

         for (int var9 = MathHelper.floor(var4.minX); var9 <= var5; var9++) {
            for (int var10 = MathHelper.floor(var4.minY); var10 <= var6; var10++) {
               for (int var11 = MathHelper.floor(var4.minZ); var11 <= var7; var11++) {
                  BlockState var13 = var2.getBlockState(var8.set(var9, var10, var11));
                  VoxelShape var12;
                  if (!var13.isAir()
                     && !(var12 = var13.getCollisionShape(var2, var8, ShapeContext.absent())).isEmpty()
                     && var3.intersects(var12.getBoundingBox().offset(var8))) {
                     return true;
                  }
               }
            }
         }

         for (Entity var15 : var2.getOtherEntities(abstractBoatEntity, var4)) {
            if (var15 != clientPlayerEntity
               && !var15.hasPassenger(abstractBoatEntity)
               && !abstractBoatEntity.hasPassenger(var15)
               && var15.isAlive()
               && var15.getBoundingBox().intersects(var4)) {
               return true;
            }
         }

         return false;
      }
   }
}
