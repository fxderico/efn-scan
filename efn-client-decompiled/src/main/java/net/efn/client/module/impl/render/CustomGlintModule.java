package net.efn.client.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class CustomGlintModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   public static volatile CustomGlintModule active;
   private static final String COLOR_CUSTOM = "Custom";
   private static final String COLOR_ACCENT = "Accent";
   private final ModeSetting colorMode = this.addSetting("color-mode", new ModeSetting("Color", "Custom", "Custom", "Accent"));
   private final ColorSetting itemColor = this.addSetting("item-color", new ColorSetting("Item Color", -9524994));
   private final BooleanSetting separateArmor = this.addSetting("separate-armor", new BooleanSetting("Separate Armor", false));
   private final ColorSetting armorColor = this.addSetting("armor-color", new ColorSetting("Armor Color", -3697942));
   private final NumberSetting intensity = this.addSetting("intensity", new NumberSetting("Intensity", 1.0F, 0.05F, 1.0F).step(0.05F));
   private final NumberSetting speed = this.addSetting("speed", new NumberSetting("Speed", 1.0F, 0.1F, 4.0F).step(0.1F));
   private final NumberSetting patternSize = this.addSetting("pattern-size", new NumberSetting("Pattern Size", 1.0F, 0.25F, 4.0F).step(0.25F));
   private final CustomGlintModule.GlintTexture items = new CustomGlintModule.GlintTexture(ItemRenderer.ITEM_ENCHANTMENT_GLINT);
   private final CustomGlintModule.GlintTexture armor = new CustomGlintModule.GlintTexture(ItemRenderer.ENTITY_ENCHANTMENT_GLINT);
   private double glintClock = 0.0 / 0.0;
   private long glintClockReal;

   public CustomGlintModule() {
      super("custom-glint", "Custom Glint", "Recolours the enchantment glint and changes its speed and pattern size.", ModuleCategory.VISUALS);
      this.itemColor.visibleWhen(() -> this.colorMode.is("Custom"));
      this.separateArmor.visibleWhen(() -> this.colorMode.is("Custom"));
      this.armorColor.visibleWhen(() -> this.colorMode.is("Custom") && this.separateArmor.get());
   }

   @Override
   protected void onEnable() {
      this.glintClock = 0.0 / 0.0;
      active = this;
      this.on(TickEvent.TYPE, tickEvent -> this.refresh());
   }

   @Override
   protected void onDisable() {
      active = null;
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.isOnThread()) {
         this.restore();
      } else {
         var1.execute(() -> {
            if (active == null) {
               this.restore();
            }
         });
      }
   }

   public long glintMillis(long l) {
      this.glintClock = Double.isNaN(this.glintClock)
         ? l
         : (this.glintClock = this.glintClock + (double)Math.max(0L, l - this.glintClockReal) * this.speed.get());
      this.glintClockReal = l;
      return (long)this.glintClock;
   }

   public float uvScaleMultiplier() {
      return 1.0F / this.patternSize.get();
   }

   private void refresh() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.world != null && var1.getOverlay() == null && RenderSystem.tryGetDevice() != null) {
         int var2 = this.colorMode.is("Accent") ? CosmeticAccent.argb() & 16777215 : this.itemColor.rgb();
         int var3 = this.colorMode.is("Custom") && this.separateArmor.get() ? this.armorColor.rgb() : var2;
         float var4 = this.intensity.get();
         this.items.tint(var1, var2, var4);
         this.armor.tint(var1, var3, var4);
      }
   }

   private void restore() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      this.items.restore(var1);
      this.armor.restore(var1);
   }

   @Environment(EnvType.CLIENT)
   private static final class GlintTexture {
      private final Identifier id;
      private int width;
      private int height;
      private int[] original;
      private int[] pattern;
      private GpuTexture written;
      private long writtenKey;
      private GpuTexture unusable;

      GlintTexture(Identifier identifier) {
         this.id = identifier;
      }

      void tint(MinecraftClient minecraftClient, int i, float f) {
         GpuTexture var4 = this.gpuTexture(minecraftClient);
         if (var4 != null && !var4.isClosed() && var4 != this.unusable) {
            long var5 = (long)(i & 16777215) << 8 | Math.round(f * 255.0F);
            if (var4 != this.written || var5 != this.writtenKey) {
               if (var4 != this.written) {
                  this.written = null;
                  if ((var4.usage() & 1) == 0 || !this.readShipped(minecraftClient, var4)) {
                     this.unusable = var4;
                     return;
                  }
               }

               if (!this.upload(var4, this.tinted(i, f))) {
                  this.unusable = var4;
               } else {
                  this.written = var4;
                  this.writtenKey = var5;
               }
            }
         }
      }

      void restore(MinecraftClient minecraftClient) {
         GpuTexture var2 = this.written != null ? this.gpuTexture(minecraftClient) : null;
         if (var2 != null && var2 == this.written && !var2.isClosed() && this.original != null) {
            this.upload(var2, this.original);
         }

         this.written = null;
         this.unusable = null;
         this.original = null;
         this.pattern = null;
      }

      private GpuTexture gpuTexture(MinecraftClient minecraftClient) {
         try {
            AbstractTexture var2 = minecraftClient.getTextureManager().getTexture(this.id);
            return var2 == null ? null : var2.getGlTexture();
         } catch (IllegalStateException var3) {
            return null;
         }
      }

      private boolean readShipped(MinecraftClient minecraftClient, GpuTexture gpuTexture) {
         Optional var3 = minecraftClient.getResourceManager().getResource(this.id);
         if (var3.isEmpty()) {
            return false;
         } else {
            try {
               boolean var10;
               try (InputStream var4 = ((Resource)var3.get()).getInputStream()) {
                  NativeImage var8 = NativeImage.read(var4);
                  int var7 = var8.getWidth();
                  int var6 = var8.getHeight();
                  if (var7 == gpuTexture.getWidth(0) && var6 == gpuTexture.getHeight(0)) {
                     boolean var5;
                     try {
                        int[] var21 = new int[var7 * var6];
                        int[] var22 = new int[var7 * var6];
                        int var11 = 0;

                        for (int var12 = 0; var12 < var6; var12++) {
                           for (int var13 = 0; var13 < var7; var11++) {
                              int var14;
                              var21[var11] = var14 = var8.getColorArgb(var13, var12);
                              var22[var11] = Math.max(ColorHelper.getRed(var14), Math.max(ColorHelper.getGreen(var14), ColorHelper.getBlue(var14)));
                              var13++;
                           }
                        }

                        this.width = var7;
                        this.height = var6;
                        this.original = var21;
                        this.pattern = var22;
                        var5 = true;
                        if (var8 == null) {
                           return var5;
                        }
                     } catch (Throwable var17) {
                        if (var8 != null) {
                           try {
                              var8.close();
                           } catch (Throwable var16) {
                              var17.addSuppressed(var16);
                           }
                        }

                        throw var17;
                     }

                     var8.close();
                     return var5;
                  }

                  boolean var9 = false;
                  if (var8 != null) {
                     var8.close();
                  }

                  var10 = var9;
               }

               return var10;
            } catch (RuntimeException | IOException var19) {
               CustomGlintModule.LOGGER.warn("[EFN] Custom Glint could not read {}", this.id, var19);
               return false;
            }
         }
      }

      private int[] tinted(int i, float f) {
         float var3 = ColorHelper.getRed(i) * f / 255.0F;
         float var4 = ColorHelper.getGreen(i) * f / 255.0F;
         float var5 = ColorHelper.getBlue(i) * f / 255.0F;
         int[] var6 = new int[this.pattern.length];

         for (int var7 = 0; var7 < var6.length; var7++) {
            int var8 = this.pattern[var7];
            var6[var7] = ColorHelper.getArgb(
               ColorHelper.getAlpha(this.original[var7]), Math.round(var8 * var3), Math.round(var8 * var4), Math.round(var8 * var5)
            );
         }

         return var6;
      }

      private boolean upload(GpuTexture gpuTexture, int[] is) {
         NativeImage var4 = new NativeImage(this.width, this.height, false);

         boolean var3;
         try {
            int var11 = 0;

            for (int var6 = 0; var6 < this.height; var6++) {
               for (int var7 = 0; var7 < this.width; var11++) {
                  var4.setColorArgb(var7, var6, is[var11]);
                  var7++;
               }
            }

            RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpuTexture, var4);
            var3 = true;
         } catch (Throwable var10) {
            Throwable var5 = var10;

            try {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var5.addSuppressed(var8);
               }

               throw var5;
            } catch (RuntimeException var9) {
               CustomGlintModule.LOGGER.warn("[EFN] Custom Glint could not write {}", this.id, var9);
               return false;
            }
         }

         var4.close();
         return var3;
      }
   }
}
