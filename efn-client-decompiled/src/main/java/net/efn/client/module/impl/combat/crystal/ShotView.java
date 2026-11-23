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

   public void show(Box var1, int var2, float var3, float var4) {
      long var5 = System.currentTimeMillis();
      if (!this.showing(var5)) {
         this.from = var1;
         this.movedAt = var5;
      } else if (!var1.equals(this.to)) {
         this.from = this.box(var5);
         this.movedAt = var5;
      }

      this.to = var1;
      this.shownAt = var5;
      this.color = var2;
      this.damage = var3;
      this.selfDamage = var4;
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
         GizmoDrawing.method_75541(this.box(var1), DrawStyle.method_75537(fade(this.color, var3), 1.5F, fade(this.color, var3 * 0.22F))).method_75533();
      }
   }

   public void label(ImDrawList var1, float var2, float var3) {
      long var4 = System.currentTimeMillis();
      if (this.showing(var4)) {
         Box var6 = this.box(var4);
         Vec3d var7 = new Vec3d(var6.method_1005().field_1352, var6.field_1325 + 0.25, var6.method_1005().field_1350);
         float[] var8 = WorldLabel.project(MinecraftClient.method_1551(), var7, var2, var3);
         if (var8 != null) {
            WorldLabel.drawText(
               var1,
               var8[0],
               var8[1],
               String.format(Locale.ROOT, "%.1f", this.damage),
               String.format(Locale.ROOT, "self %.1f", this.selfDamage),
               this.alpha(var4)
            );
         }
      }
   }

   private boolean showing(long var1) {
      return this.to != null && this.alpha(var1) > 0.0F;
   }

   private float alpha(long var1) {
      long var3 = var1 - this.shownAt - 150L;
      return var3 <= 0L ? 1.0F : Math.max(0.0F, 1.0F - (float)var3 / 350.0F);
   }

   private Box box(long var1) {
      float var3 = Math.min(1.0F, (float)(var1 - this.movedAt) / 120.0F);
      float var4 = 1.0F - (1.0F - var3) * (1.0F - var3) * (1.0F - var3);
      return new Box(this.from.method_61125().method_35590(this.to.method_61125(), var4), this.from.method_61126().method_35590(this.to.method_61126(), var4));
   }

   private static int fade(int var0, float var1) {
      return Math.round((var0 >>> 24) * var1) << 24 | var0 & 16777215;
   }
}
