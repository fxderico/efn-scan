package net.efn.client.module.impl.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.LightSnapshot;
import net.efn.client.util.SessionWorker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.VisibilityConfigurable;

@Environment(EnvType.CLIENT)
public class LightEspModule extends ClientModule {
   private static final long SCAN_COOLDOWN_MS = 1000L;
   private static final int MAX_BOXES = 12000;
   private static final float LINE_WIDTH = 1.5F;
   private final SessionWorker<Map<Integer, List<Box>>> worker = new SessionWorker<>("EFN-Light");
   private ClientWorld sessionLevel;
   private LightSnapshot collecting;
   private long lastScanTime;
   private Map<Integer, List<Box>> renderData = Map.of();
   private final NumberSetting sRadius = this.addSetting("render-radius", new NumberSetting("Render Radius", 8.0F, 1.0F, 16.0F).step(1.0F));
   private final NumberSetting sMinLight = this.addSetting("min-light", new NumberSetting("Min Light", 5.0F, 1.0F, 15.0F).step(1.0F));
   private final BooleanSetting sDenoise = this.addSetting("denoise", new BooleanSetting("Denoise", true));
   private final ColorSetting sColor = this.addSetting("color", new ColorSetting("Color", -2130719744));

   public LightEspModule() {
      super("light-debug", "Light Debug", "Highlights artificial light underground.", ModuleCategory.BASE_RAID);
      List.of(this.sRadius, this.sMinLight, this.sDenoise).forEach(setting -> setting.onChange(this::resetScan));
   }

   @Override
   protected void onEnable() {
      this.resetScan();
   }

   @Override
   protected void onDisable() {
      this.resetScan();
   }

   private void resetScan() {
      this.worker.close();
      this.sessionLevel = null;
      this.collecting = null;
      this.renderData = Map.of();
      this.lastScanTime = 0L;
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (this.sessionLevel != var2.world) {
         this.resetScan();
         this.sessionLevel = var2.world;
      }

      if (var2.world != null && var2.player != null) {
         this.worker.poll(map -> this.renderData = map);
         if (!this.worker.busy()) {
            long var3 = System.currentTimeMillis();
            if (this.collecting == null) {
               if (var3 - this.lastScanTime < 1000L) {
                  return;
               }

               this.collecting = new LightSnapshot(var2.player.getChunkPos(), clampInt(num(this.sRadius, 8), 1, 16), clampInt(num(this.sMinLight, 5), 1, 15));
            }

            if (this.collecting.collect(var2.world)) {
               Map var5 = this.collecting.hits();
               boolean var6 = this.isDenoise();
               int var7 = clampInt(num(this.sMinLight, 5), 1, 15);
               this.collecting = null;
               this.lastScanTime = var3;
               this.worker.submit(() -> buildBoxes(var5, var6, var7));
            }
         }
      }
   }

   static Map<Integer, List<Box>> buildBoxes(Map<BlockPos, Integer> map, boolean bl, int i) {
      HashSet var3 = new HashSet();
      if (bl) {
         for (Entry var5 : map.entrySet()) {
            if (Thread.currentThread().isInterrupted()) {
               throw new CancellationException();
            }

            BlockPos var6 = (BlockPos)var5.getKey();
            int var7 = (Integer)var5.getValue();
            if (var7 >= i + 1 && var7 <= i + 2 && !var3.contains(var6)) {
               boolean var8 = false;

               for (int var9 = -1; var9 <= 1 && !var8; var9++) {
                  for (int var10 = -1; var10 <= 1 && !var8; var10++) {
                     for (int var11 = -1; var11 <= 1; var11++) {
                        if (map.getOrDefault(var6.add(var9, var10, var11), 0) > var7) {
                           var8 = true;
                           break;
                        }
                     }
                  }
               }

               if (!var8) {
                  ArrayDeque var22 = new ArrayDeque();
                  var22.add(var6);
                  var3.add(var6);

                  while (!var22.isEmpty()) {
                     if (Thread.currentThread().isInterrupted()) {
                        throw new CancellationException();
                     }

                     BlockPos var24 = (BlockPos)var22.removeFirst();

                     for (Direction var14 : Direction.values()) {
                        BlockPos var15 = var24.offset(var14);
                        int var16 = map.getOrDefault(var15, 0);
                        if (var16 >= i && var16 <= var7 && var3.add(var15)) {
                           var22.addLast(var15);
                        }
                     }
                  }
               }
            }
         }
      }

      HashMap var17 = new HashMap();
      int var18 = 0;

      for (Entry var20 : map.entrySet()) {
         if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
         }

         BlockPos var21 = (BlockPos)var20.getKey();
         if (!var3.contains(var21)) {
            boolean var23 = false;

            for (Direction var29 : Direction.values()) {
               BlockPos var30 = var21.offset(var29);
               if (!map.containsKey(var30) || var3.contains(var30)) {
                  var23 = true;
                  break;
               }
            }

            if (var23) {
               var17.computeIfAbsent((Integer)var20.getValue(), integer -> new ArrayList<>()).add(new Box(var21));
               if (++var18 < 12000) {
                  continue;
               }
               break;
            }
         }
      }

      var17.replaceAll((integer, list) -> List.copyOf(list));
      return Map.copyOf(var17);
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world != null && var2.player != null && var2.world == this.sessionLevel) {
         Map var3 = this.renderData;
         if (!var3.isEmpty() && this.sColor != null) {
            int var4 = this.sColor.argb();
            int var5 = var4 >>> 24 & 0xFF;
            int var6 = var4 >> 16 & 0xFF;
            int var7 = var4 >> 8 & 0xFF;
            int var8 = var4 & 0xFF;

            for (Entry var10 : var3.entrySet()) {
               float var11 = clamp01(((Integer)var10.getKey()).intValue() / 15.0F);
               int var12 = lerp(var6, 255, var11);
               int var13 = lerp(var7, 255, var11);
               int var14 = lerp(var8, 255, var11);
               int var15 = clampByte(Math.round(var5 * (0.5F + 0.5F * var11)));
               int var16 = var15 << 24 | var12 << 16 | var13 << 8 | var14;
               int var17 = 0xFF000000 | Math.round(var12 * 0.45F) << 16 | Math.round(var13 * 0.45F) << 8 | Math.round(var14 * 0.45F);
               DrawStyle var18 = DrawStyle.filledAndStroked(var17, 1.5F, var16);

               for (Box var20 : (List)var10.getValue()) {
                  int var21 = (int)var20.minX >> 4;
                  int var22 = (int)var20.minZ >> 4;
                  if (var2.world.getChunkManager().isChunkLoaded(var21, var22)) {
                     VisibilityConfigurable var23 = GizmoDrawing.box(var20, var18);
                     var23.ignoreOcclusion();
                  }
               }
            }
         }
      }
   }

   private boolean isDenoise() {
      return this.sDenoise != null && this.sDenoise.value().get();
   }

   private static int num(NumberSetting numberSetting, int i) {
      return numberSetting == null ? i : Math.round(numberSetting.value()[0]);
   }

   private static int clampInt(int i, int j, int k) {
      return Math.max(j, Math.min(k, i));
   }

   private static int clampByte(int i) {
      return Math.max(0, Math.min(255, i));
   }

   private static float clamp01(float f) {
      return Math.max(0.0F, Math.min(1.0F, f));
   }

   private static int lerp(int i, int j, float f) {
      return Math.round(i + (j - i) * f);
   }
}
