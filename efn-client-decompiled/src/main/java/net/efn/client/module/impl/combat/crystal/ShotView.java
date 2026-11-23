package net.efn.client.module.impl.combat.crystal;

import imgui.ImDrawList;
import java.util.Locale;
import net.efn.client.gui.WorldLabel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public final class ShotView {
   private static final long SLIDE_MS = 120L;
   private static final long HOLD_MS = 150L;
   private static final long FADE_MS = 350L;
   private static final float FILL_ALPHA = 0.22F;
   private static final float LINE_WIDTH = 1.5F;
   private static final double LABEL_RISE = 0.25;
   private Box from;
   private Box to;
   private long movedAt;
   private long shownAt;
   private int color;
   private float damage;
   private float selfDamage;

   public ShotView() {
   }

   public void show(Box box, int i, float f, float g) {
      long var5 = System.currentTimeMillis();
      if (!this.showing(var5)) {
         this.from = box;
         this.movedAt = var5;
      } else if (!box.equals(this.to)) {
         this.from = this.box(var5);
         this.movedAt = var5;
      }

      this.to = box;
      this.shownAt = var5;
      this.color = i;
      this.damage = f;
      this.selfDamage = g;
   }

   public void clear() {
      this.to = null;
   }

   public boolean showing() {
      return this.showing(System.currentTimeMillis());
   }

   public void draw() {
      long var1 = System.currentTimeMillis();
      if (this.showing(var1)) {
         float var3 = this.alpha(var1);
         GizmoDrawing.box(this.box(var1), DrawStyle.filledAndStroked(fade(this.color, var3), 1.5F, fade(this.color, var3 * 0.22F))).ignoreOcclusion();
      }
   }

   public void label(ImDrawList imDrawList, float f, float g) {
      long var4 = System.currentTimeMillis();
      if (this.showing(var4)) {
         Box var6 = this.box(var4);
         Vec3d var7 = new Vec3d(var6.getCenter().x, var6.maxY + 0.25, var6.getCenter().z);
         float[] var8 = WorldLabel.project(MinecraftClient.getInstance(), var7, f, g);
         if (var8 != null) {
            WorldLabel.drawText(
               imDrawList,
               var8[0],
               var8[1],
               String.format(Locale.ROOT, "%.1f", this.damage),
               String.format(Locale.ROOT, "self %.1f", this.selfDamage),
               this.alpha(var4)
            );
         }
      }
   }

   private boolean showing(long l) {
      return this.to != null && this.alpha(l) > 0.0F;
   }

   private float alpha(long l) {
      long var3 = l - this.shownAt - 150L;
      return var3 <= 0L ? 1.0F : Math.max(0.0F, 1.0F - (float)var3 / 350.0F);
   }

   private Box box(long l) {
      float var3 = Math.min(1.0F, (float)(l - this.movedAt) / 120.0F);
      float var4 = 1.0F - (1.0F - var3) * (1.0F - var3) * (1.0F - var3);
      return new Box(this.from.getMinPos().lerp(this.to.getMinPos(), var4), this.from.getMaxPos().lerp(this.to.getMaxPos(), var4));
   }

   private static int fade(int i, float f) {
      return Math.round((i >>> 24) * f) << 24 | i & 16777215;
   }
}
