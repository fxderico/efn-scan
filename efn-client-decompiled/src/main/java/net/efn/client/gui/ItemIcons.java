package net.efn.client.gui;

import com.example.client.mixin.AccessorGameRenderer;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.buffers.GpuBuffer.MappedView;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.DiffuseLighting.Type;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.fog.FogRenderer.FogType;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.NativeImage.Format;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Atlases;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ItemIcons {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final int SHEET_SIDE = 512;
   private static final int MIN_PX = 8;
   private static final int MAX_PX = 96;
   private static final int MAX_SHEETS = 8;
   private static final int BUSY_FRAMES = 2;
   private static final int IDLE_FRAMES = 600;
   private static final int PAINTS_PER_FRAME = 6;
   private static final int CHECK_SIDE = 32;
   static final int AUDIT_FRAMES = 30;
   private static final ItemIcons.Cell BLANK = new ItemIcons.Cell();
   private static final Int2ObjectOpenHashMap<ItemIcons.Sheet> SHEETS = new Int2ObjectOpenHashMap();
   private static final List<ItemIcons.Sheet> RETIRED = new ArrayList<>();
   private static final Reference2ObjectOpenHashMap<Item, ItemStack> PLAIN = new Reference2ObjectOpenHashMap();
   private static final ReferenceOpenHashSet<Item> BROKEN = new ReferenceOpenHashSet();
   private static final ItemLook MODEL = new ItemLook();
   private static final int[] VIEWPORT = new int[4];
   private static GpuTexture depth;
   private static GpuTextureView depthView;
   private static ProjectionMatrix2 projection;
   private static OrderedRenderCommandQueueImpl submits;
   private static RenderDispatcher features;
   private static Immediate buffers;
   private static GpuBuffer readback;
   private static ByteBuffer straightened;
   private static boolean straightenFailed;
   private static int verdict;
   private static GpuTexture itemAtlas;
   private static int frame = -2147483648;
   private static int retiredFrame;
   private static int auditedFrame;
   private static int paintsLeft;
   private static boolean disabled;

   private ItemIcons() {
   }

   static float pixels(float var0) {
      float var1 = var0 < 16.0F ? Math.round(var0) : 16.0F * (float)Math.floor(var0 / 16.0F + 0.25F);
      return Math.max(8.0F, Math.min(96.0F, var1));
   }

   static void draw(ImDrawList var0, Item var1, float var2, float var3, float var4, float var5) {
      if (var1 != null && var1 != Items.field_8162) {
         ItemStack var6 = (ItemStack)PLAIN.get(var1);
         if (var6 == null) {
            var6 = var1.method_7854();
            PLAIN.put(var1, var6);
         }

         draw(var0, var6, var2, var3, var4, var5);
      }
   }

   public static void draw(ImDrawList var0, ItemStack var1, float var2, float var3, float var4, float var5) {
      if (var1 != null && !var1.method_7960() && !(var5 <= 0.004F)) {
         startFrame();
         int var7 = (int)pixels(var4);
         float var8 = Math.round(var2);
         float var9 = Math.round(var3);

         ItemIcons.Cell var6;
         try {
            var6 = cell(var1, var7);
         } finally {
            MODEL.method_65605();
         }

         if (var6 != null) {
            if (var6.texture == 0) {
               float var10 = Math.max(1.0F, var7 * 0.18F);
               var0.addRectFilled(var8 + 1.0F, var9 + 1.0F, var8 + var7 - 1.0F, var9 + var7 - 1.0F, HudChrome.scaleAlpha(452984831, var5), var10);
            } else {
               var0.addImage(var6.texture, var8, var9, var8 + var7, var9 + var7, var6.u0, var6.v0, var6.u1, var6.v1, HudChrome.scaleAlpha(-1, var5));
            }
         }
      }
   }

   private static void startFrame() {
      int var1 = ImGuiManager.getInstance().frameCount();
      if (var1 != frame) {
         frame = var1;
         paintsLeft = PerformanceModule.optimizationsEnabled() ? 2 : 6;
         if (!RETIRED.isEmpty() && var1 != retiredFrame) {
            for (ItemIcons.Sheet var4 : RETIRED) {
               var4.release();
            }

            RETIRED.clear();
         }

         GpuTexture var7 = currentAtlas();
         boolean var0 = var1 - auditedFrame >= 30 && audit();
         if (var7 == itemAtlas && !var0) {
            ObjectIterator var8 = SHEETS.values().iterator();

            while (var8.hasNext()) {
               ItemIcons.Sheet var9 = (ItemIcons.Sheet)var8.next();
               if (var1 - var9.lastUsed > 600) {
                  var9.release();
                  var8.remove();
               }
            }
         } else {
            itemAtlas = var7;
            ObjectIterator var5 = SHEETS.values().iterator();

            while (var5.hasNext()) {
               ItemIcons.Sheet var6 = (ItemIcons.Sheet)var5.next();
               var6.release();
            }

            SHEETS.clear();
            BROKEN.clear();
         }
      }
   }

   private static boolean audit() {
      auditedFrame = frame;
      boolean var0 = false;
      if (depth != null && !alive(depth)) {
         depthView.close();
         depth = null;
         depthView = null;
         var0 = true;
      }

      ObjectIterator var1 = SHEETS.values().iterator();

      while (var1.hasNext()) {
         ItemIcons.Sheet var2 = (ItemIcons.Sheet)var1.next();
         var0 |= var2.color != null && !alive(var2.color);
      }

      if (var0) {
         LOGGER.warn("HUD item icon textures were lost; drawing them again");
      }

      return var0;
   }

   private static GpuTexture currentAtlas() {
      try {
         return MinecraftClient.method_1551().method_72703().method_73025(Atlases.field_64477).method_68004();
      } catch (RuntimeException var1) {
         return null;
      }
   }

   private static ItemIcons.Cell cell(ItemStack var0, int var1) {
      ItemIcons.Sheet var2 = sheet(var1);
      if (var2 == null) {
         return null;
      } else {
         var2.lastUsed = frame;
         if (!resolve(var0)) {
            return BLANK;
         } else {
            ItemIcons.Cell var3 = (ItemIcons.Cell)var2.cells.get(MODEL.identity);
            if (var3 != null) {
               return var3;
            } else if (disabled) {
               return BLANK;
            } else if (paintsLeft <= 0) {
               return null;
            } else {
               paintsLeft--;
               if (var2.full()) {
                  retire(var2);
                  var2 = new ItemIcons.Sheet(var1);
                  var2.lastUsed = frame;
                  SHEETS.put(var1, var2);
               }

               var3 = paint(var2, seeThrough(var0.method_7909()));
               var2.cells.put(Arrays.asList(MODEL.identity.toArray()), var3);
               return var3;
            }
         }
      }
   }

   private static ItemIcons.Sheet sheet(int var0) {
      ItemIcons.Sheet var1 = (ItemIcons.Sheet)SHEETS.get(var0);
      if (var1 != null) {
         return var1;
      } else {
         if (SHEETS.size() >= 8) {
            ItemIcons.Sheet var2 = stalest();
            if (var2 == null || frame - var2.lastUsed < 2) {
               return null;
            }

            retire(var2);
         }

         var1 = new ItemIcons.Sheet(var0);
         SHEETS.put(var0, var1);
         return var1;
      }
   }

   private static ItemIcons.Sheet stalest() {
      ItemIcons.Sheet var0 = null;
      ObjectIterator var1 = SHEETS.values().iterator();

      while (var1.hasNext()) {
         ItemIcons.Sheet var2 = (ItemIcons.Sheet)var1.next();
         if (var0 == null || var2.lastUsed < var0.lastUsed) {
            var0 = var2;
         }
      }

      return var0;
   }

   private static void retire(ItemIcons.Sheet var0) {
      SHEETS.remove(var0.px);
      RETIRED.add(var0);
      retiredFrame = frame;
   }

   private static boolean resolve(ItemStack var0) {
      if (BROKEN.contains(var0.method_7909())) {
         return false;
      } else if (MODEL.resolve(var0)) {
         return true;
      } else {
         BROKEN.add(var0.method_7909());
         return false;
      }
   }

   private static boolean seeThrough(Item var0) {
      return !(var0 instanceof BlockItem var1) ? false : BlockRenderLayers.method_23679(var1.method_7711().method_9564()) == BlockRenderLayer.field_60926;
   }

   private static ItemIcons.Cell paint(ItemIcons.Sheet var0, boolean var1) {
      if (MODEL.method_65606()) {
         return BLANK;
      } else {
         try {
            if (var0.color == null) {
               allocate(var0);
            }

            int var2 = var0.px;
            int var3 = 512 / var2;
            int var4 = var0.filled++;
            int var5 = var4 % var3 * var2;
            int var6 = var4 / var3 * var2;
            int var7 = 512 - var6 - var2;
            render(MODEL, var0.color, var0.colorView, depth, depthView, var5, var6, var2);
            if (var1) {
               straighten(var0.color, var5, var7, var2);
            }

            ItemIcons.Cell var8 = new ItemIcons.Cell();
            var8.texture = ((GlTexture)var0.color).method_68427();
            var8.u0 = var5 / 512.0F;
            var8.u1 = (var5 + var2) / 512.0F;
            var8.v0 = (512 - var6) / 512.0F;
            var8.v1 = var7 / 512.0F;
            return var8;
         } catch (RuntimeException var9) {
            disabled = true;
            LOGGER.warn("HUD item icons could not be drawn; showing plain slots instead", var9);
            return BLANK;
         }
      }
   }

   private static void allocate(ItemIcons.Sheet var0) {
      GpuDevice var1 = RenderSystem.getDevice();
      if (depth == null) {
         depth = create("EFN HUD icon depth", 8, TextureFormat.DEPTH32, 512);
         depthView = var1.createTextureView(depth);
      }

      var0.color = create("EFN HUD icons " + var0.px, 15, TextureFormat.RGBA8, 512);
      var0.colorView = var1.createTextureView(var0.color);
      var1.createCommandEncoder().clearColorAndDepthTextures(var0.color, 0, depth, 1.0);
   }

   public static void render(ItemRenderState var0, GpuTexture var1, GpuTextureView var2, GpuTexture var3, GpuTextureView var4, int var5, int var6, int var7) {
      MinecraftClient var8 = MinecraftClient.method_1551();
      int var9 = var1.getWidth(0);
      int var10 = var9 - var6 - var7;
      RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(var1, 0, var3, 1.0, var5, var10, var7, var7);
      if (features == null) {
         buffers = VertexConsumerProvider.method_22991(new BufferAllocator(262144));
         submits = new OrderedRenderCommandQueueImpl();
         features = new RenderDispatcher(
            submits, var8.method_1541(), buffers, var8.method_72703(), var8.method_22940().method_23003(), var8.method_22940().method_23001(), var8.field_1772
         );
      }

      GpuTextureView var11 = RenderSystem.outputColorTextureOverride;
      GpuTextureView var12 = RenderSystem.outputDepthTextureOverride;
      GpuBufferSlice var13 = RenderSystem.getShaderLights();
      GpuBufferSlice var14 = RenderSystem.getShaderFog();
      GpuBufferSlice var15 = RenderSystem.getProjectionMatrixBuffer();
      ProjectionType var16 = RenderSystem.getProjectionType();
      GL11.glGetIntegerv(2978, VIEWPORT);
      GlStateManager._viewport(VIEWPORT[0], VIEWPORT[1], VIEWPORT[2], VIEWPORT[3]);
      Matrix4fStack var17 = RenderSystem.getModelViewStack();
      var17.pushMatrix();
      var17.identity();

      try {
         RenderSystem.outputColorTextureOverride = var2;
         RenderSystem.outputDepthTextureOverride = var4;
         if (projection == null) {
            projection = new ProjectionMatrix2("EFN icons", -1000.0F, 1000.0F, true);
         }

         RenderSystem.setProjectionMatrix(projection.method_71092(var9, var9), ProjectionType.field_54954);
         Type var18 = var0.method_65608() ? Type.field_60027 : Type.field_60026;
         var8.field_1773.method_71114().method_71034(var18);
         RenderSystem.setShaderFog(((AccessorGameRenderer)var8.field_1773).efn$getFogRenderer().method_71109(FogType.field_60101));
         RenderSystem.enableScissorForRenderTypeDraws(var5, var10, var7, var7);
         MatrixStack var19 = new MatrixStack();
         var19.method_46416(var5 + var7 * 0.5F, var6 + var7 * 0.5F, 0.0F);
         var19.method_22905(var7, -var7, var7);

         try {
            var0.method_65604(var19, submits, 15728880, OverlayTexture.field_21444, 0);
            features.method_73002();
         } finally {
            submits.method_72953();
            buffers.method_22993();
         }
      } finally {
         RenderSystem.disableScissorForRenderTypeDraws();
         RenderSystem.outputColorTextureOverride = var11;
         RenderSystem.outputDepthTextureOverride = var12;
         RenderSystem.setShaderLights(var13);
         RenderSystem.setShaderFog(var14);
         RenderSystem.setProjectionMatrix(var15, var16);
         var17.popMatrix();
         GlStateManager._viewport(VIEWPORT[0], VIEWPORT[1], VIEWPORT[2], VIEWPORT[3]);
      }
   }

   static void straighten(GpuTexture var0, int var1, int var2, int var3) {
      if (!straightenFailed) {
         int var4 = var3 * var3 * 4;
         long var5 = (long)var0.getWidth(0) * var0.getHeight(0) * 4L;

         try {
            GpuDevice var7 = RenderSystem.getDevice();
            if (readback == null || readback.size() < var5) {
               if (readback != null) {
                  readback.close();
               }

               readback = var7.createBuffer(() -> "EFN icon readback", 9, var5);
            }

            if (straightened == null || straightened.capacity() < var4) {
               straightened = BufferUtils.createByteBuffer(var4);
            }

            CommandEncoder var8 = var7.createCommandEncoder();
            var8.copyTextureToBuffer(var0, readback, 0L, () -> {}, 0, var1, var2, var3, var3);
            GpuFence var9 = var8.createFence();

            try {
               GL11.glFlush();
               if (!var9.awaitCompletion(1000000000L)) {
                  throw new IllegalStateException("icon read-back timed out");
               }
            } catch (Throwable var25) {
               if (var9 != null) {
                  try {
                     var9.close();
                  } catch (Throwable var23) {
                     var25.addSuppressed(var23);
                  }
               }

               throw var25;
            }

            if (var9 != null) {
               var9.close();
            }

            straightened.clear().limit(var4);
            MappedView var28 = var8.mapBuffer(readback.slice(0L, var4), true, false);

            try {
               ByteBuffer var10 = var28.data();

               for (byte var11 = 0; var11 < var4; var11 += 4) {
                  int var12 = var10.get(var11 + 3) & 255;

                  for (int var13 = 0; var13 < 3; var13++) {
                     int var14 = var12 == 0 ? 0 : Math.min(255, ((var10.get(var11 + var13) & 255) * 255 + var12 / 2) / var12);
                     straightened.put(var11 + var13, (byte)var14);
                  }

                  straightened.put(var11 + 3, (byte)var12);
               }
            } catch (Throwable var24) {
               if (var28 != null) {
                  try {
                     var28.close();
                  } catch (Throwable var22) {
                     var24.addSuppressed(var22);
                  }
               }

               throw var24;
            }

            if (var28 != null) {
               var28.close();
            }

            var8.writeToTexture(var0, straightened, Format.field_4997, 0, 0, var1, var2, var3, var3);
         } catch (RuntimeException var26) {
            straightenFailed = true;
            LOGGER.warn("Couldn't read icons back; see-through ones stay darker", var26);
         } finally {
            GlStateManager._pixelStore(3330, 0);
         }
      }
   }

   static void forgetVerdict() {
      verdict = 0;
   }

   static boolean works() {
      if (verdict == 0) {
         verdict = check() ? 1 : -1;
      }

      return verdict > 0;
   }

   private static boolean check() {
      MinecraftClient var0 = MinecraftClient.method_1551();
      GpuDevice var1 = RenderSystem.getDevice();
      ItemRenderState var2 = new ItemRenderState();
      GpuTexture var3 = null;
      GpuTexture var4 = null;
      GpuTextureView var5 = null;
      GpuTextureView var6 = null;
      GpuBuffer var7 = null;

      boolean var12;
      try {
         var0.method_65386().method_65598(var2, new ItemStack(Items.field_20391), ItemDisplayContext.field_4317, var0.field_1687, null, 0);
         if (var2.method_65606()) {
            return true;
         }

         var3 = create("EFN icon check", 10, TextureFormat.RGBA8, 32);
         var4 = create("EFN icon check depth", 8, TextureFormat.DEPTH32, 32);
         var5 = var1.createTextureView(var3);
         var6 = var1.createTextureView(var4);
         render(var2, var3, var5, var4, var6, 0, 0, 32);
         var7 = var1.createBuffer(() -> "EFN icon check", 9, 4096L);
         CommandEncoder var31 = var1.createCommandEncoder();
         var31.copyTextureToBuffer(var3, var7, 0L, () -> {}, 0, 0, 0, 32, 32);
         GpuFence var33 = var31.createFence();

         label328: {
            try {
               GL11.glFlush();
               if (!var33.awaitCompletion(1000000000L)) {
                  boolean var36 = true;
                  var12 = var36;
                  break label328;
               }
            } catch (Throwable var28) {
               if (var33 != null) {
                  try {
                     var33.close();
                  } catch (Throwable var26) {
                     var28.addSuppressed(var26);
                  }
               }

               throw var28;
            }

            if (var33 != null) {
               var33.close();
            }

            int var34 = 0;
            long var11 = 0L;
            MappedView var13 = var31.mapBuffer(var7, true, false);

            try {
               ByteBuffer var14 = var13.data();

               for (byte var15 = 0; var15 < 4096; var15 += 4) {
                  if ((var14.get(var15 + 3) & 255) >= 128) {
                     var34++;
                     var11 += (var14.get(var15) & 255) + (var14.get(var15 + 1) & 255) + (var14.get(var15 + 2) & 255);
                  }
               }
            } catch (Throwable var27) {
               if (var13 != null) {
                  try {
                     var13.close();
                  } catch (Throwable var25) {
                     var27.addSuppressed(var25);
                  }
               }

               throw var27;
            }

            if (var13 != null) {
               var13.close();
            }

            long var37 = var34 == 0 ? 0L : var11 / var34 / 3L;
            boolean var8 = var34 >= 128 && var37 >= 24L;
            if (!var8) {
               LOGGER.warn("Item icons come out blank or black here (covered {}, brightness {}); block lists use flat textures", var34, var37);
            }

            return var8;
         }

         if (var33 != null) {
            var33.close();
         }
      } catch (RuntimeException var29) {
         LOGGER.warn("Couldn't check item icons", var29);
         return true;
      } finally {
         var2.method_65605();
         GlStateManager._pixelStore(3330, 0);
         if (var7 != null) {
            var7.close();
         }

         if (var5 != null) {
            var5.close();
         }

         if (var6 != null) {
            var6.close();
         }

         if (var3 != null) {
            var3.close();
         }

         if (var4 != null) {
            var4.close();
         }
      }

      return var12;
   }

   static GpuTexture create(String var0, int var1, TextureFormat var2, int var3) {
      GlStateManager._activeTexture(GL11.glGetInteger(34016));
      GlStateManager._bindTexture(GL11.glGetInteger(32873));
      GpuTexture var5 = RenderSystem.getDevice().createTexture(var0, var1, var2, var3, var3, 1, 1);
      GlTexture var4;
      if (var5 instanceof GlTexture && !prepare(var4 = (GlTexture)var5)) {
         var5.close();
         throw new IllegalStateException(var0 + " got no storage");
      } else {
         return var5;
      }
   }

   private static boolean prepare(GlTexture var0) {
      int var1 = GL11.glGetInteger(32873);
      GL11.glBindTexture(3553, var0.method_68427());

      boolean var8;
      try {
         if (!holds(var0)) {
            int var2 = GL11.glGetInteger(35055);
            GL15.glBindBuffer(35052, 0);
            TextureFormat var3 = var0.getFormat();
            GL11.glTexImage2D(
               3553,
               0,
               GlConst.toGlInternalId(var3),
               var0.getWidth(0),
               var0.getHeight(0),
               0,
               GlConst.toGlExternalId(var3),
               GlConst.toGlType(var3),
               (ByteBuffer)null
            );
            GL15.glBindBuffer(35052, var2);
         }

         GL11.glTexParameteri(3553, 33085, 0);
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         boolean var7 = holds(var0);
         var8 = var7;
      } finally {
         GL11.glBindTexture(3553, var1);
      }

      return var8;
   }

   static boolean alive(GpuTexture var0) {
      if (var0.isClosed()) {
         return false;
      } else if (!(var0 instanceof GlTexture var1)) {
         return true;
      } else if (!GL11.glIsTexture(var1.method_68427())) {
         return false;
      } else {
         int var2 = GL11.glGetInteger(32873);
         GL11.glBindTexture(3553, var1.method_68427());
         boolean var3 = GL11.glGetInteger(32873) == var1.method_68427() && holds(var1);
         GL11.glBindTexture(3553, var2);
         if (!var3) {
            GlStateManager.clearGlErrors();
         }

         return var3;
      }
   }

   private static boolean holds(GpuTexture var0) {
      return GL11.glGetTexLevelParameteri(3553, 0, 4096) == var0.getWidth(0) && GL11.glGetTexLevelParameteri(3553, 0, 4097) == var0.getHeight(0);
   }

   static void dispose(GpuTexture var0) {
      if (var0 != null && alive(var0)) {
         var0.close();
      }
   }

   static void crisp(GlTexture var0) {
      int var1 = GL11.glGetInteger(32873);
      GL11.glBindTexture(3553, var0.method_68427());
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glBindTexture(3553, var1);
   }

   @Environment(EnvType.CLIENT)
   private static final class Cell {
      int texture;
      float u0;
      float v0;
      float u1;
      float v1;

      private Cell() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Sheet {
      final int px;
      final Object2ObjectOpenHashMap<Object, ItemIcons.Cell> cells = new Object2ObjectOpenHashMap();
      GpuTexture color;
      GpuTextureView colorView;
      int filled;
      int lastUsed;

      Sheet(int var1) {
         this.px = var1;
      }

      boolean full() {
         int var1 = 512 / this.px;
         return this.filled >= var1 * var1;
      }

      void release() {
         if (this.colorView != null) {
            this.colorView.close();
         }

         ItemIcons.dispose(this.color);
         this.colorView = null;
         this.color = null;
         this.cells.clear();
      }
   }
}
