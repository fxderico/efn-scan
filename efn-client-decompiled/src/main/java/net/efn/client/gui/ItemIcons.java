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
   private static final Int2ObjectOpenHashMap<ItemIcons.Sheet> SHEETS = new Int2ObjectOpenHashMap<>();
   private static final List<ItemIcons.Sheet> RETIRED = new ArrayList<>();
   private static final Reference2ObjectOpenHashMap<Item, ItemStack> PLAIN = new Reference2ObjectOpenHashMap<>();
   private static final ReferenceOpenHashSet<Item> BROKEN = new ReferenceOpenHashSet<>();
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

   static float pixels(float f) {
      float var1 = f < 16.0F ? Math.round(f) : 16.0F * (float)Math.floor(f / 16.0F + 0.25F);
      return Math.max(8.0F, Math.min(96.0F, var1));
   }

   static void draw(ImDrawList imDrawList, Item item, float f, float g, float h, float i) {
      if (item != null && item != Items.AIR) {
         ItemStack var6 = PLAIN.get(item);
         if (var6 == null) {
            var6 = item.getDefaultStack();
            PLAIN.put(item, var6);
         }

         draw(imDrawList, var6, f, g, h, i);
      }
   }

   public static void draw(ImDrawList imDrawList, ItemStack itemStack, float f, float g, float h, float i) {
      if (itemStack != null && !itemStack.isEmpty() && !(i <= 0.004F)) {
         startFrame();
         int var7 = (int)pixels(h);
         float var8 = Math.round(f);
         float var9 = Math.round(g);

         ItemIcons.Cell var6;
         try {
            var6 = cell(itemStack, var7);
         } finally {
            MODEL.clear();
         }

         if (var6 != null) {
            if (var6.texture == 0) {
               float var10 = Math.max(1.0F, var7 * 0.18F);
               imDrawList.addRectFilled(var8 + 1.0F, var9 + 1.0F, var8 + var7 - 1.0F, var9 + var7 - 1.0F, HudChrome.scaleAlpha(452984831, i), var10);
            } else {
               imDrawList.addImage(var6.texture, var8, var9, var8 + var7, var9 + var7, var6.u0, var6.v0, var6.u1, var6.v1, HudChrome.scaleAlpha(-1, i));
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

            for (ItemIcons.Sheet var6 : SHEETS.values()) {
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

      for (ItemIcons.Sheet var2 : SHEETS.values()) {
         var0 |= var2.color != null && !alive(var2.color);
      }

      if (var0) {
         LOGGER.warn("HUD item icon textures were lost; drawing them again");
      }

      return var0;
   }

   private static GpuTexture currentAtlas() {
      try {
         return MinecraftClient.getInstance().getAtlasManager().getAtlasTexture(Atlases.ITEMS).getGlTexture();
      } catch (RuntimeException var1) {
         return null;
      }
   }

   private static ItemIcons.Cell cell(ItemStack itemStack, int i) {
      ItemIcons.Sheet var2 = sheet(i);
      if (var2 == null) {
         return null;
      } else {
         var2.lastUsed = frame;
         if (!resolve(itemStack)) {
            return BLANK;
         } else {
            ItemIcons.Cell var3 = var2.cells.get(MODEL.identity);
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
                  var2 = new ItemIcons.Sheet(i);
                  var2.lastUsed = frame;
                  SHEETS.put(i, var2);
               }

               var3 = paint(var2, seeThrough(itemStack.getItem()));
               var2.cells.put(Arrays.asList(MODEL.identity.toArray()), var3);
               return var3;
            }
         }
      }
   }

   private static ItemIcons.Sheet sheet(int i) {
      ItemIcons.Sheet var1 = SHEETS.get(i);
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

         var1 = new ItemIcons.Sheet(i);
         SHEETS.put(i, var1);
         return var1;
      }
   }

   private static ItemIcons.Sheet stalest() {
      ItemIcons.Sheet var0 = null;

      for (ItemIcons.Sheet var2 : SHEETS.values()) {
         if (var0 == null || var2.lastUsed < var0.lastUsed) {
            var0 = var2;
         }
      }

      return var0;
   }

   private static void retire(ItemIcons.Sheet sheet) {
      SHEETS.remove(sheet.px);
      RETIRED.add(sheet);
      retiredFrame = frame;
   }

   private static boolean resolve(ItemStack itemStack) {
      if (BROKEN.contains(itemStack.getItem())) {
         return false;
      } else if (MODEL.resolve(itemStack)) {
         return true;
      } else {
         BROKEN.add(itemStack.getItem());
         return false;
      }
   }

   private static boolean seeThrough(Item item) {
      return !(item instanceof BlockItem var1) ? false : BlockRenderLayers.getBlockLayer(var1.getBlock().getDefaultState()) == BlockRenderLayer.TRANSLUCENT;
   }

   private static ItemIcons.Cell paint(ItemIcons.Sheet sheet, boolean bl) {
      if (MODEL.isEmpty()) {
         return BLANK;
      } else {
         try {
            if (sheet.color == null) {
               allocate(sheet);
            }

            int var2 = sheet.px;
            int var3 = 512 / var2;
            int var4 = sheet.filled++;
            int var5 = var4 % var3 * var2;
            int var6 = var4 / var3 * var2;
            int var7 = 512 - var6 - var2;
            render(MODEL, sheet.color, sheet.colorView, depth, depthView, var5, var6, var2);
            if (bl) {
               straighten(sheet.color, var5, var7, var2);
            }

            ItemIcons.Cell var8 = new ItemIcons.Cell();
            var8.texture = ((GlTexture)sheet.color).getGlId();
            var8.u0 = var5 / 512.0F;
            var8.u1 = (var5 + var2) / 512.0F;
            var8.v0 = (512 - var6) / 512.0F;
            var8.v1 = var7 / 512.0F;
            return var8;
         } catch (RuntimeException var9) {
            disabled = true;
            LOGGER.warn("HUD item icons could not be drawn; showing plain slots instead", (Throwable)var9);
            return BLANK;
         }
      }
   }

   private static void allocate(ItemIcons.Sheet sheet) {
      GpuDevice var1 = RenderSystem.getDevice();
      if (depth == null) {
         depth = create("EFN HUD icon depth", 8, TextureFormat.DEPTH32, 512);
         depthView = var1.createTextureView(depth);
      }

      sheet.color = create("EFN HUD icons " + sheet.px, 15, TextureFormat.RGBA8, 512);
      sheet.colorView = var1.createTextureView(sheet.color);
      var1.createCommandEncoder().clearColorAndDepthTextures(sheet.color, 0, depth, 1.0);
   }

   public static void render(
      ItemRenderState itemRenderState,
      GpuTexture gpuTexture,
      GpuTextureView gpuTextureView,
      GpuTexture gpuTexture2,
      GpuTextureView gpuTextureView2,
      int i,
      int j,
      int k
   ) {
      MinecraftClient var8 = MinecraftClient.getInstance();
      int var9 = gpuTexture.getWidth(0);
      int var10 = var9 - j - k;
      RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(gpuTexture, 0, gpuTexture2, 1.0, i, var10, k, k);
      if (features == null) {
         buffers = VertexConsumerProvider.immediate(new BufferAllocator(262144));
         submits = new OrderedRenderCommandQueueImpl();
         features = new RenderDispatcher(
            submits,
            var8.getBlockRenderManager(),
            buffers,
            var8.getAtlasManager(),
            var8.getBufferBuilders().getOutlineVertexConsumers(),
            var8.getBufferBuilders().getEffectVertexConsumers(),
            var8.textRenderer
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
         RenderSystem.outputColorTextureOverride = gpuTextureView;
         RenderSystem.outputDepthTextureOverride = gpuTextureView2;
         if (projection == null) {
            projection = new ProjectionMatrix2("EFN icons", -1000.0F, 1000.0F, true);
         }

         RenderSystem.setProjectionMatrix(projection.set(var9, var9), ProjectionType.ORTHOGRAPHIC);
         Type var18 = itemRenderState.isSideLit() ? Type.ITEMS_3D : Type.ITEMS_FLAT;
         var8.gameRenderer.getDiffuseLighting().setShaderLights(var18);
         RenderSystem.setShaderFog(((AccessorGameRenderer)var8.gameRenderer).efn$getFogRenderer().getFogBuffer(FogType.NONE));
         RenderSystem.enableScissorForRenderTypeDraws(i, var10, k, k);
         MatrixStack var19 = new MatrixStack();
         var19.translate(i + k * 0.5F, j + k * 0.5F, 0.0F);
         var19.scale(k, -k, k);

         try {
            itemRenderState.render(var19, submits, 15728880, OverlayTexture.DEFAULT_UV, 0);
            features.render();
         } finally {
            submits.clear();
            buffers.draw();
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

   static void straighten(GpuTexture gpuTexture, int i, int j, int k) {
      if (!straightenFailed) {
         int var4 = k * k * 4;
         long var5 = (long)gpuTexture.getWidth(0) * gpuTexture.getHeight(0) * 4L;

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
            var8.copyTextureToBuffer(gpuTexture, readback, 0L, () -> {}, 0, i, j, k, k);

            try (GpuFence var9 = var8.createFence()) {
               GL11.glFlush();
               if (!var9.awaitCompletion(1000000000L)) {
                  throw new IllegalStateException("icon read-back timed out");
               }
            }

            straightened.clear().limit(var4);

            try (MappedView var28 = var8.mapBuffer(readback.slice(0L, var4), true, false)) {
               ByteBuffer var10 = var28.data();

               for (byte var11 = 0; var11 < var4; var11 += 4) {
                  int var12 = var10.get(var11 + 3) & 255;

                  for (int var13 = 0; var13 < 3; var13++) {
                     int var14 = var12 == 0 ? 0 : Math.min(255, ((var10.get(var11 + var13) & 255) * 255 + var12 / 2) / var12);
                     straightened.put(var11 + var13, (byte)var14);
                  }

                  straightened.put(var11 + 3, (byte)var12);
               }
            }

            var8.writeToTexture(gpuTexture, straightened, Format.RGBA, 0, 0, i, j, k, k);
         } catch (RuntimeException var26) {
            straightenFailed = true;
            LOGGER.warn("Couldn't read icons back; see-through ones stay darker", (Throwable)var26);
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
      MinecraftClient var0 = MinecraftClient.getInstance();
      GpuDevice var1 = RenderSystem.getDevice();
      ItemRenderState var2 = new ItemRenderState();
      GpuTexture var3 = null;
      GpuTexture var4 = null;
      GpuTextureView var5 = null;
      GpuTextureView var6 = null;
      GpuBuffer var7 = null;

      try {
         var0.getItemModelManager().clearAndUpdate(var2, new ItemStack(Items.STONE), ItemDisplayContext.GUI, var0.world, null, 0);
         if (var2.isEmpty()) {
            return true;
         } else {
            var3 = create("EFN icon check", 10, TextureFormat.RGBA8, 32);
            var4 = create("EFN icon check depth", 8, TextureFormat.DEPTH32, 32);
            var5 = var1.createTextureView(var3);
            var6 = var1.createTextureView(var4);
            render(var2, var3, var5, var4, var6, 0, 0, 32);
            var7 = var1.createBuffer(() -> "EFN icon check", 9, 4096L);
            CommandEncoder var31 = var1.createCommandEncoder();
            var31.copyTextureToBuffer(var3, var7, 0L, () -> {}, 0, 0, 0, 32, 32);

            try (GpuFence var33 = var31.createFence()) {
               GL11.glFlush();
               if (!var33.awaitCompletion(1000000000L)) {
                  return true;
               }
            }

            int var34 = 0;
            long var11 = 0L;

            try (MappedView var13 = var31.mapBuffer(var7, true, false)) {
               ByteBuffer var14 = var13.data();

               for (byte var15 = 0; var15 < 4096; var15 += 4) {
                  if ((var14.get(var15 + 3) & 255) >= 128) {
                     var34++;
                     var11 += (var14.get(var15) & 255) + (var14.get(var15 + 1) & 255) + (var14.get(var15 + 2) & 255);
                  }
               }
            }

            long var37 = var34 == 0 ? 0L : var11 / var34 / 3L;
            boolean var8 = var34 >= 128 && var37 >= 24L;
            if (!var8) {
               LOGGER.warn("Item icons come out blank or black here (covered {}, brightness {}); block lists use flat textures", var34, var37);
            }

            return var8;
         }
      } catch (RuntimeException var29) {
         LOGGER.warn("Couldn't check item icons", (Throwable)var29);
         return true;
      } finally {
         var2.clear();
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
   }

   static GpuTexture create(String string, int i, TextureFormat textureFormat, int j) {
      GlStateManager._activeTexture(GL11.glGetInteger(34016));
      GlStateManager._bindTexture(GL11.glGetInteger(32873));
      GpuTexture var5 = RenderSystem.getDevice().createTexture(string, i, textureFormat, j, j, 1, 1);
      GlTexture var4;
      if (var5 instanceof GlTexture && !prepare(var4 = (GlTexture)var5)) {
         var5.close();
         throw new IllegalStateException(string + " got no storage");
      } else {
         return var5;
      }
   }

   private static boolean prepare(GlTexture glTexture) {
      int var1 = GL11.glGetInteger(32873);
      GL11.glBindTexture(3553, glTexture.getGlId());

      boolean var8;
      try {
         if (!holds(glTexture)) {
            int var2 = GL11.glGetInteger(35055);
            GL15.glBindBuffer(35052, 0);
            TextureFormat var3 = glTexture.getFormat();
            GL11.glTexImage2D(
               3553,
               0,
               GlConst.toGlInternalId(var3),
               glTexture.getWidth(0),
               glTexture.getHeight(0),
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
         boolean var7 = holds(glTexture);
         var8 = var7;
      } finally {
         GL11.glBindTexture(3553, var1);
      }

      return var8;
   }

   static boolean alive(GpuTexture gpuTexture) {
      if (gpuTexture.isClosed()) {
         return false;
      } else if (!(gpuTexture instanceof GlTexture var1)) {
         return true;
      } else if (!GL11.glIsTexture(var1.getGlId())) {
         return false;
      } else {
         int var2 = GL11.glGetInteger(32873);
         GL11.glBindTexture(3553, var1.getGlId());
         boolean var3 = GL11.glGetInteger(32873) == var1.getGlId() && holds(var1);
         GL11.glBindTexture(3553, var2);
         if (!var3) {
            GlStateManager.clearGlErrors();
         }

         return var3;
      }
   }

   private static boolean holds(GpuTexture gpuTexture) {
      return GL11.glGetTexLevelParameteri(3553, 0, 4096) == gpuTexture.getWidth(0) && GL11.glGetTexLevelParameteri(3553, 0, 4097) == gpuTexture.getHeight(0);
   }

   static void dispose(GpuTexture gpuTexture) {
      if (gpuTexture != null && alive(gpuTexture)) {
         gpuTexture.close();
      }
   }

   static void crisp(GlTexture glTexture) {
      int var1 = GL11.glGetInteger(32873);
      GL11.glBindTexture(3553, glTexture.getGlId());
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
      final Object2ObjectOpenHashMap<Object, ItemIcons.Cell> cells = new Object2ObjectOpenHashMap<>();
      GpuTexture color;
      GpuTextureView colorView;
      int filled;
      int lastUsed;

      Sheet(int i) {
         this.px = i;
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
