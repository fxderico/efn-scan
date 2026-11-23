package net.efn.client.gui;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Atlases;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class BlockIcons {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final float SPRITE_TEXELS = 16.0F;
   private static final int PAGE_SIDE = 1024;
   private static final int RENDERS_PER_FRAME = 48;
   private static final int MAX_SHEETS = 4;
   private static final int MAX_LOSSES = 3;
   private static final int WATER_RGB = 4159204;
   private static final Int2ObjectOpenHashMap<BlockIcons.Sheet> SHEETS = new Int2ObjectOpenHashMap<>();
   private static final ReferenceOpenHashSet<GpuTexture> CRISP = new ReferenceOpenHashSet<>();
   private static GpuTexture atlas;
   private static final ItemRenderState ITEM = new ItemRenderState();
   private static int budgetFrame = -1;
   private static int rendersLeft;
   private static boolean itemsFailed;
   private static int auditedFrame;
   private static int losses;

   private BlockIcons() {
   }

   static void validate() {
      GpuTexture var0;
      try {
         var0 = MinecraftClient.getInstance().getAtlasManager().getAtlasTexture(Atlases.BLOCKS).getGlTexture();
      } catch (RuntimeException var2) {
         var0 = null;
      }

      if (var0 != atlas) {
         atlas = var0;
         clearSheets();
      } else {
         int var1 = ImGuiManager.getInstance().frameCount();
         if (var1 - auditedFrame >= 30) {
            auditedFrame = var1;
            if (!intact()) {
               clearSheets();
               if (++losses < 3) {
                  LOGGER.warn("Block icon pages were lost; drawing them again");
               } else {
                  itemsFailed = true;
                  LOGGER.warn("Block icon pages keep getting lost; using block textures instead");
               }
            }
         }
      }
   }

   private static boolean intact() {
      for (BlockIcons.Sheet var1 : SHEETS.values()) {
         if (var1.depth != null && !ItemIcons.alive(var1.depth)) {
            return false;
         }

         for (GpuTexture var3 : var1.pages) {
            if (!ItemIcons.alive(var3)) {
               return false;
            }
         }
      }

      return true;
   }

   static void forget() {
      clearSheets();
   }

   static float size(float f) {
      return Math.max(1, (int)(f / 16.0F)) * 16.0F;
   }

   static void draw(ImDrawList imDrawList, Block block, float f, float g, float h) {
      int var5 = Math.round(h);
      BlockIcons.Sheet var6 = sheet(var5);
      BlockIcons.Icon var7 = var6.icons.get(block);
      if (var7 == null) {
         var7 = resolve(var6, block);
         if (var7 == null) {
            return;
         }

         var6.icons.put(block, var7);
      }

      float var8 = Math.round(f);
      float var9 = Math.round(g);
      if (var7.texture != 0) {
         imDrawList.addImage(var7.texture, var8, var9, var8 + var5, var9 + var5, var7.u0, var7.v0, var7.u1, var7.v1, EfnWidgets.c(var7.tint));
      } else {
         float var10 = var5 * 0.2F;
         imDrawList.addRectFilled(var8, var9, var8 + var5, var9 + var5, EfnWidgets.c(-12240588), var10);
         imDrawList.addRect(var8, var9, var8 + var5, var9 + var5, EfnWidgets.c(536870911), var10, 0, 1.0F);
      }
   }

   private static BlockIcons.Sheet sheet(int i) {
      BlockIcons.Sheet var1 = SHEETS.get(i);
      if (var1 == null) {
         if (SHEETS.size() >= 4) {
            clearSheets();
         }

         var1 = new BlockIcons.Sheet(i);
         SHEETS.put(i, var1);
      }

      return var1;
   }

   private static void clearSheets() {
      for (BlockIcons.Sheet var1 : SHEETS.values()) {
         var1.close();
      }

      SHEETS.clear();
      CRISP.clear();
      ItemIcons.forgetVerdict();
   }

   private static BlockIcons.Icon resolve(BlockIcons.Sheet sheet, Block block) {
      Item var2 = block.asItem();
      Block var3 = block;
      if (var2 == Items.AIR && (var2 = standIn(block)) instanceof BlockItem) {
         BlockItem var4 = (BlockItem)var2;
         var3 = var4.getBlock();
      }

      if (var2 == Items.AIR || itemsFailed || sheet.cell > 256 || !ItemIcons.works()) {
         return sprite(block);
      } else if (!takeRender()) {
         return null;
      } else {
         MinecraftClient var10 = MinecraftClient.getInstance();

         try {
            var10.getItemModelManager().clearAndUpdate(ITEM, new ItemStack(var2), ItemDisplayContext.GUI, var10.world, null, 0);
         } catch (RuntimeException var9) {
            ITEM.clear();
            return sprite(block);
         }

         if (ITEM.isEmpty()) {
            return sprite(block);
         } else {
            BlockIcons.Icon var5 = new BlockIcons.Icon();
            boolean var6 = BlockRenderLayers.getBlockLayer(var3.getDefaultState()) == BlockRenderLayer.TRANSLUCENT;

            try {
               renderItem(sheet, var5, var6);
               return var5;
            } catch (RuntimeException var8) {
               itemsFailed = true;
               LOGGER.warn("Block item icons failed, using block textures instead", (Throwable)var8);
               return sprite(block);
            }
         }
      }
   }

   private static Item standIn(Block block) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.world != null) {
         try {
            ItemStack var2 = block.getDefaultState().getPickStack(var1.world, BlockPos.ORIGIN, false);
            if (!var2.isEmpty()) {
               return var2.getItem();
            }
         } catch (RuntimeException var3) {
         }
      }

      if (block == Blocks.MOVING_PISTON) {
         return Items.PISTON;
      } else if (block == Blocks.END_PORTAL) {
         return Items.ENDER_EYE;
      } else {
         return block == Blocks.END_GATEWAY ? Items.ENDER_PEARL : Items.AIR;
      }
   }

   private static boolean takeRender() {
      int var0 = ImGuiManager.getInstance().frameCount();
      if (var0 != budgetFrame) {
         budgetFrame = var0;
         rendersLeft = 48;
      }

      if (rendersLeft <= 0) {
         return false;
      } else {
         rendersLeft--;
         return true;
      }
   }

   private static void renderItem(BlockIcons.Sheet sheet, BlockIcons.Icon icon, boolean bl) {
      int var3 = sheet.cell;
      int var4 = 1024 / var3;
      if (sheet.pages.isEmpty() || sheet.used >= var4 * var4) {
         addPage(sheet);
      }

      int var5 = sheet.used++;
      int var6 = var5 % var4 * var3;
      int var7 = var5 / var4 * var3;
      int var8 = 1024 - var7 - var3;
      GpuTexture var9 = sheet.pages.get(sheet.pages.size() - 1);
      GpuTextureView var10 = sheet.views.get(sheet.views.size() - 1);

      try {
         ItemIcons.render(ITEM, var9, var10, sheet.depth, sheet.depthView, var6, var7, var3);
      } finally {
         ITEM.clear();
      }

      if (bl) {
         ItemIcons.straighten(var9, var6, var8, var3);
      }

      icon.texture = ((GlTexture)var9).getGlId();
      icon.u0 = var6 / 1024.0F;
      icon.u1 = (var6 + var3) / 1024.0F;
      icon.v0 = (1024 - var7) / 1024.0F;
      icon.v1 = var8 / 1024.0F;
   }

   private static void addPage(BlockIcons.Sheet sheet) {
      GpuDevice var1 = RenderSystem.getDevice();
      if (sheet.depth == null) {
         sheet.depth = ItemIcons.create("EFN block icons depth", 8, TextureFormat.DEPTH32, 1024);
         sheet.depthView = var1.createTextureView(sheet.depth);
      }

      GpuTexture var2 = ItemIcons.create("EFN block icons " + sheet.cell + "px", 15, TextureFormat.RGBA8, 1024);
      sheet.pages.add(var2);
      sheet.views.add(var1.createTextureView(var2));
      sheet.used = 0;
      var1.createCommandEncoder().clearColorAndDepthTextures(var2, 0, sheet.depth, 1.0);
   }

   private static BlockIcons.Icon sprite(Block block) {
      BlockIcons.Icon var1 = new BlockIcons.Icon();
      MinecraftClient var2 = MinecraftClient.getInstance();
      BlockState var3 = block.getDefaultState();

      try {
         Sprite var5 = var2.getBakedModelManager().getBlockModels().getModelParticleSprite(var3);
         if (MissingSprite.getMissingSpriteId().equals(var5.getContents().getId())) {
            return var1;
         }

         GpuTexture var6 = var2.getTextureManager().getTexture(var5.getAtlasId()).getGlTexture();
         GlTexture var4;
         if (!(var6 instanceof GlTexture) || (var4 = (GlTexture)var6).isClosed()) {
            return var1;
         }

         if (CRISP.add(var4)) {
            ItemIcons.crisp(var4);
         }

         var1.texture = var4.getGlId();
         var1.u0 = var5.getMinU();
         var1.v0 = var5.getMinV();
         var1.u1 = var5.getMaxU();
         var1.v1 = var5.getMaxV();
      } catch (RuntimeException var8) {
         return var1;
      }

      if (!var3.isOf(Blocks.GRASS_BLOCK)) {
         int var9 = -1;

         try {
            var9 = var2.getBlockColors().getColor(var3, null, null, 0);
         } catch (RuntimeException var7) {
         }

         if (var9 == -1 && (var3.isOf(Blocks.WATER) || var3.isOf(Blocks.BUBBLE_COLUMN))) {
            var9 = 4159204;
         }

         if (var9 != -1) {
            var1.tint = 0xFF000000 | (var9 & 0xFF) << 16 | var9 & 0xFF00 | var9 >> 16 & 0xFF;
         }
      }

      return var1;
   }

   @Environment(EnvType.CLIENT)
   private static final class Icon {
      int texture;
      float u0;
      float v0;
      float u1;
      float v1;
      int tint = -1;

      private Icon() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Sheet {
      final int cell;
      final Reference2ObjectOpenHashMap<Block, BlockIcons.Icon> icons = new Reference2ObjectOpenHashMap<>();
      final List<GpuTexture> pages = new ArrayList<>();
      final List<GpuTextureView> views = new ArrayList<>();
      GpuTexture depth;
      GpuTextureView depthView;
      int used;

      Sheet(int i) {
         this.cell = i;
      }

      void close() {
         for (GpuTextureView var2 : this.views) {
            var2.close();
         }

         for (GpuTexture var4 : this.pages) {
            ItemIcons.dispose(var4);
         }

         if (this.depthView != null) {
            this.depthView.close();
         }

         ItemIcons.dispose(this.depth);
         this.views.clear();
         this.pages.clear();
         this.depth = null;
         this.depthView = null;
      }
   }
}
