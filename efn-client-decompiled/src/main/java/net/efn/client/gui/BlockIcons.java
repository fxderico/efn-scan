package net.efn.client.gui;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
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
   private static final Int2ObjectOpenHashMap<BlockIcons.Sheet> SHEETS = new Int2ObjectOpenHashMap();
   private static final ReferenceOpenHashSet<GpuTexture> CRISP = new ReferenceOpenHashSet();
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
         var0 = MinecraftClient.method_1551().method_72703().method_73025(Atlases.field_56382).method_68004();
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
      ObjectIterator var0 = SHEETS.values().iterator();

      while (var0.hasNext()) {
         BlockIcons.Sheet var1 = (BlockIcons.Sheet)var0.next();
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

   static float size(float var0) {
      return Math.max(1, (int)(var0 / 16.0F)) * 16.0F;
   }

   static void draw(ImDrawList var0, Block var1, float var2, float var3, float var4) {
      int var5 = Math.round(var4);
      BlockIcons.Sheet var6 = sheet(var5);
      BlockIcons.Icon var7 = (BlockIcons.Icon)var6.icons.get(var1);
      if (var7 == null) {
         var7 = resolve(var6, var1);
         if (var7 == null) {
            return;
         }

         var6.icons.put(var1, var7);
      }

      float var8 = Math.round(var2);
      float var9 = Math.round(var3);
      if (var7.texture != 0) {
         var0.addImage(var7.texture, var8, var9, var8 + var5, var9 + var5, var7.u0, var7.v0, var7.u1, var7.v1, EfnWidgets.c(var7.tint));
      } else {
         float var10 = var5 * 0.2F;
         var0.addRectFilled(var8, var9, var8 + var5, var9 + var5, EfnWidgets.c(-12240588), var10);
         var0.addRect(var8, var9, var8 + var5, var9 + var5, EfnWidgets.c(536870911), var10, 0, 1.0F);
      }
   }

   private static BlockIcons.Sheet sheet(int var0) {
      BlockIcons.Sheet var1 = (BlockIcons.Sheet)SHEETS.get(var0);
      if (var1 == null) {
         if (SHEETS.size() >= 4) {
            clearSheets();
         }

         var1 = new BlockIcons.Sheet(var0);
         SHEETS.put(var0, var1);
      }

      return var1;
   }

   private static void clearSheets() {
      ObjectIterator var0 = SHEETS.values().iterator();

      while (var0.hasNext()) {
         BlockIcons.Sheet var1 = (BlockIcons.Sheet)var0.next();
         var1.close();
      }

      SHEETS.clear();
      CRISP.clear();
      ItemIcons.forgetVerdict();
   }

   private static BlockIcons.Icon resolve(BlockIcons.Sheet var0, Block var1) {
      Item var2 = var1.method_8389();
      Block var3 = var1;
      if (var2 == Items.field_8162 && (var2 = standIn(var1)) instanceof BlockItem) {
         BlockItem var4 = (BlockItem)var2;
         var3 = var4.method_7711();
      }

      if (var2 == Items.field_8162 || itemsFailed || var0.cell > 256 || !ItemIcons.works()) {
         return sprite(var1);
      } else if (!takeRender()) {
         return null;
      } else {
         MinecraftClient var10 = MinecraftClient.method_1551();

         try {
            var10.method_65386().method_65598(ITEM, new ItemStack(var2), ItemDisplayContext.field_4317, var10.field_1687, null, 0);
         } catch (RuntimeException var9) {
            ITEM.method_65605();
            return sprite(var1);
         }

         if (ITEM.method_65606()) {
            return sprite(var1);
         } else {
            BlockIcons.Icon var5 = new BlockIcons.Icon();
            boolean var6 = BlockRenderLayers.method_23679(var3.method_9564()) == BlockRenderLayer.field_60926;

            try {
               renderItem(var0, var5, var6);
               return var5;
            } catch (RuntimeException var8) {
               itemsFailed = true;
               LOGGER.warn("Block item icons failed, using block textures instead", var8);
               return sprite(var1);
            }
         }
      }
   }

   private static Item standIn(Block var0) {
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (var1.field_1687 != null) {
         try {
            ItemStack var2 = var0.method_9564().method_65171(var1.field_1687, BlockPos.field_10980, false);
            if (!var2.method_7960()) {
               return var2.method_7909();
            }
         } catch (RuntimeException var3) {
         }
      }

      if (var0 == Blocks.field_10008) {
         return Items.field_8249;
      } else if (var0 == Blocks.field_10027) {
         return Items.field_8449;
      } else {
         return var0 == Blocks.field_10613 ? Items.field_8634 : Items.field_8162;
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

   private static void renderItem(BlockIcons.Sheet var0, BlockIcons.Icon var1, boolean var2) {
      int var3 = var0.cell;
      int var4 = 1024 / var3;
      if (var0.pages.isEmpty() || var0.used >= var4 * var4) {
         addPage(var0);
      }

      int var5 = var0.used++;
      int var6 = var5 % var4 * var3;
      int var7 = var5 / var4 * var3;
      int var8 = 1024 - var7 - var3;
      GpuTexture var9 = var0.pages.get(var0.pages.size() - 1);
      GpuTextureView var10 = var0.views.get(var0.views.size() - 1);

      try {
         ItemIcons.render(ITEM, var9, var10, var0.depth, var0.depthView, var6, var7, var3);
      } finally {
         ITEM.method_65605();
      }

      if (var2) {
         ItemIcons.straighten(var9, var6, var8, var3);
      }

      var1.texture = ((GlTexture)var9).method_68427();
      var1.u0 = var6 / 1024.0F;
      var1.u1 = (var6 + var3) / 1024.0F;
      var1.v0 = (1024 - var7) / 1024.0F;
      var1.v1 = var8 / 1024.0F;
   }

   private static void addPage(BlockIcons.Sheet var0) {
      GpuDevice var1 = RenderSystem.getDevice();
      if (var0.depth == null) {
         var0.depth = ItemIcons.create("EFN block icons depth", 8, TextureFormat.DEPTH32, 1024);
         var0.depthView = var1.createTextureView(var0.depth);
      }

      GpuTexture var2 = ItemIcons.create("EFN block icons " + var0.cell + "px", 15, TextureFormat.RGBA8, 1024);
      var0.pages.add(var2);
      var0.views.add(var1.createTextureView(var2));
      var0.used = 0;
      var1.createCommandEncoder().clearColorAndDepthTextures(var2, 0, var0.depth, 1.0);
   }

   private static BlockIcons.Icon sprite(Block var0) {
      BlockIcons.Icon var1 = new BlockIcons.Icon();
      MinecraftClient var2 = MinecraftClient.method_1551();
      BlockState var3 = var0.method_9564();

      try {
         Sprite var5 = var2.method_1554().method_4743().method_3339(var3);
         if (MissingSprite.method_4539().equals(var5.method_45851().method_45816())) {
            return var1;
         }

         GpuTexture var6 = var2.method_1531().method_4619(var5.method_45852()).method_68004();
         GlTexture var4;
         if (!(var6 instanceof GlTexture) || (var4 = (GlTexture)var6).isClosed()) {
            return var1;
         }

         if (CRISP.add(var4)) {
            ItemIcons.crisp(var4);
         }

         var1.texture = var4.method_68427();
         var1.u0 = var5.method_4594();
         var1.v0 = var5.method_4593();
         var1.u1 = var5.method_4577();
         var1.v1 = var5.method_4575();
      } catch (RuntimeException var8) {
         return var1;
      }

      if (!var3.method_27852(Blocks.field_10219)) {
         int var9 = -1;

         try {
            var9 = var2.method_1505().method_1697(var3, null, null, 0);
         } catch (RuntimeException var7) {
         }

         if (var9 == -1 && (var3.method_27852(Blocks.field_10382) || var3.method_27852(Blocks.field_10422))) {
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
      final Reference2ObjectOpenHashMap<Block, BlockIcons.Icon> icons = new Reference2ObjectOpenHashMap();
      final List<GpuTexture> pages = new ArrayList<>();
      final List<GpuTextureView> views = new ArrayList<>();
      GpuTexture depth;
      GpuTextureView depthView;
      int used;

      Sheet(int var1) {
         this.cell = var1;
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
