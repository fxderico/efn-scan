package net.efn.client.module.impl.misc.noinvleak;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.buffers.GpuBuffer.MappedView;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.sun.jna.Platform;
import imgui.ImDrawList;
import imgui.ImGui;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.efn.client.gui.BorderlessFullscreen;
import net.efn.client.gui.ItemIcons;
import net.efn.client.gui.ItemLook;
import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Atlases;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class StreamOverlay {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final int SLOTS = 41;
   private static final int CURSOR = 41;
   private static final int ENTRIES = 42;
   private static final long SLOT_BITS = 2199023255551L;
   private static final int CELLS_PER_SIDE = 7;
   private static final int CACHE_LIMIT = 4194304;
   private static final long READBACK_TIMEOUT = 1000000000L;
   private static final long BACKGROUND_INTERVAL = 33000000L;
   private static final int FALLBACK_BACKGROUND = -7631989;
   private static final int[] BLANK = new int[0];
   private static final Runnable NOTHING = () -> {};
   private static final ItemStack[] stack = new ItemStack[42];
   private static final int[] guiX = new int[42];
   private static final int[] guiY = new int[42];
   private static long mask;
   private static int hovered = -1;
   private static boolean selectionSeen;
   private static int selectionX;
   private static int selectionY;
   private static long fakeMask;
   private static int fakeScale;
   private static ImDrawList fakeList;
   private static final DigitFont.Sink FAKE_SINK = (var0, var1, var2, var3, var4) -> fakeList.addRectFilled(
      var0, var1, var0 + var2, var1 + var3, var4 & -16711936 | var4 << 16 & 0xFF0000 | var4 >>> 16 & 0xFF
   );
   private static final ItemStack[] shownStack = new ItemStack[42];
   private static final int[] shownX = new int[42];
   private static final int[] shownY = new int[42];
   private static final int[] shownCount = new int[42];
   private static final int[] shownBar = new int[42];
   private static long shownMask;
   private static int shownScale;
   private static boolean shownOpaque;
   private static boolean shownSelection;
   private static int shownSelectionX;
   private static int shownSelectionY;
   private static int shownHover = -1;
   private static int shownWindowX;
   private static int shownWindowY;
   private static GpuTexture shownAtlas;
   private static int boxX;
   private static int boxY;
   private static boolean backgroundValid;
   private static ItemStack cursorStack;
   private static int cursorCount;
   private static int cursorBar;
   private static int cursorScale;
   private static int cursorX;
   private static int cursorY;
   private static final ItemLook look = new ItemLook();
   private static final Object2ObjectOpenHashMap<List<Object>, int[]> icons = new Object2ObjectOpenHashMap();
   private static final int[][] slotIcon = new int[42][];
   private static final int[] pendingSlot = new int[49];
   private static final List<Object>[] pendingKey = newKeys(49);
   private static int cachedBytes;
   private static final int[][] background = new int[41][];
   private static final int[][] backgroundNext = new int[41][];
   private static final int[] syncRegion = new int[4];
   private static long backgroundAt;
   private static GpuFence asyncFence;
   private static GpuBuffer asyncBuffer;
   private static long asyncMask;
   private static int asyncScale;
   private static final int[] asyncX = new int[41];
   private static final int[] asyncY = new int[41];
   private static final int[] asyncRegion = new int[4];
   private static final Canvas canvas = new Canvas();
   private static OverlayWindow slotWindow;
   private static OverlayWindow cursorWindow;
   private static GpuTexture scratch;
   private static GpuTextureView scratchView;
   private static GpuTexture scratchDepth;
   private static GpuTextureView scratchDepthView;
   private static GpuBuffer readback;
   private static int iconPx;
   private static volatile boolean unsupported;
   private static volatile long retryAt;
   private static int transientFailures;
   private static boolean toldUnsupported;
   private static boolean toldPaused;
   private static boolean toldFullscreen;
   private static boolean diagnosed;
   private static final long[] BACKOFF = new long[]{2000000000L, 10000000000L, 60000000000L};
   private static final long REPEAT_LOG_INTERVAL = 60000000000L;
   private static final Map<String, long[]> reported = new HashMap<>();

   private StreamOverlay() {
   }

   public static boolean usable() {
      if (Platform.isWindows() && !unsupported) {
         long var0 = retryAt;
         return var0 == 0L || System.nanoTime() - var0 >= 0L;
      } else {
         return false;
      }
   }

   public static void retry() {
      unsupported = false;
      retryAt = 0L;
      transientFailures = 0;
   }

   public static boolean recordsHotbar() {
      return hotbarVisible();
   }

   public static void hotbar(int var0, int var1, int var2, ItemStack var3) {
      if (hotbarVisible()) {
         record(var0, var1, var2, var3);
      }
   }

   public static void container(int var0, int var1, int var2, ItemStack var3, boolean var4) {
      record(var0, var1, var2, var3);
      if (var4) {
         hovered = var0;
      }
   }

   public static void carried(ItemStack var0, int var1, int var2) {
      record(41, var1, var2, var0);
   }

   public static void selection(int var0, int var1) {
      if (hotbarVisible()) {
         selectionSeen = true;
         selectionX = var0;
         selectionY = var1;
      }
   }

   public static void tooltip(ItemStack var0, int var1, int var2) {
      TextPanels.tooltip(var0, var1, var2);
   }

   public static void itemName(ItemStack var0, int var1, int var2) {
      TextPanels.itemName(var0, var1, var2);
   }

   private static boolean hotbarVisible() {
      Screen var0 = MinecraftClient.method_1551().field_1755;
      return var0 == null || var0 instanceof ChatScreen;
   }

   private static void record(int var0, int var1, int var2, ItemStack var3) {
      if (var0 >= 0 && var0 < 42 && (var0 != 41 || !var3.method_7960())) {
         stack[var0] = var3;
         guiX[var0] = var1;
         guiY[var0] = var2;
         mask |= 1L << var0;
      }
   }

   public static boolean hasFakes() {
      return fakeMask != 0L;
   }

   public static void drawFakes() {
      long var0 = fakeMask;
      int var2 = fakeScale;
      if (var0 != 0L && var2 > 0) {
         try {
            fakeList = ImGui.getBackgroundDrawList();

            for (int var3 = 0; var3 < 41; var3++) {
               ItemStack var4;
               if ((var0 >>> var3 & 1L) != 0L && !(var4 = NoInvLeakModule.fakeFor(var3)).method_7960()) {
                  int var5 = guiX[var3] * var2;
                  int var6 = guiY[var3] * var2;
                  ItemIcons.draw(fakeList, var4, var5, var6, 16.0F * var2, 1.0F);
                  DigitFont.decorations(FAKE_SINK, var4, var5, var6, var2);
               }
            }
         } catch (RuntimeException | LinkageError var7) {
            fakeMask = 0L;
            fail(var7, "drawing the stream's fake items");
         }
      }
   }

   public static void endFrame() {
      long var0 = mask;
      mask = 0L;
      int var2 = hovered;
      hovered = -1;
      boolean var3 = selectionSeen && NoInvLeakModule.hidesSelectedSlot();
      selectionSeen = false;
      if (NoInvLeakModule.overlayWanted() && usable()) {
         try {
            MinecraftClient var4 = MinecraftClient.method_1551();
            Window var5 = var4.method_22683();
            if (var5.method_65966()) {
               fakeMask = 0L;
               hideAll();
            } else {
               int var6 = var5.method_4495();
               int var7 = var5.method_4499();
               int var8 = var5.method_4477();
               boolean var9 = NoInvLeakModule.opaqueCells();
               var0 = withoutEmpty(var0, var9 && NoInvLeakModule.fillsEmpty());
               GpuTexture var10 = itemAtlas(var4);
               if (var10 != shownAtlas) {
                  OverlayArt.reset();
                  resetIcons(16 * var6);
                  shownAtlas = var10;
                  shownMask = 0L;
               }

               fakeMask = var0 & 2199023255551L;
               fakeScale = var6;
               layers(var4, var5, var0, var6, var7, var8, var9, var3, var2);
               TextPanels.update(var5, var6, var7, var8);
            }
         } catch (RuntimeException | LinkageError var11) {
            fakeMask = 0L;
            fail(var11, "drawing the overlay");
         }

         TextPanels.frameDone();
      } else {
         fakeMask = 0L;
         if (started()) {
            release();
         }

         TextPanels.frameDone();
      }
   }

   private static void fail(Throwable var0, String var1) {
      boolean var3 = var0 instanceof OverlayWindow.Unsupported || var0 instanceof LinkageError;
      String var4 = var0.getMessage() == null ? var0.getClass().getSimpleName() : var0.getMessage();
      long var5 = 0L;
      if (var3) {
         unsupported = true;
      } else {
         var5 = BACKOFF[Math.min(transientFailures++, BACKOFF.length - 1)];
         retryAt = System.nanoTime() + var5;
      }

      String var7 = var1 + "|" + var0.getClass().getName() + "|" + var4;
      long var8 = System.nanoTime();
      long[] var10 = reported.get(var7);
      String var2 = var3 ? "turned off for this session" : "retrying in " + var5 / 1000000000L + " s";
      if (var10 == null) {
         reported.put(var7, new long[]{var8, 0L});
         LOGGER.warn(
            "[EFN] No Inv Leak: Stream Only failed while {} ({}); {}. Windows build {}. Your items are shown as hidden to you as well until it works again.",
            new Object[]{var1, var4, var2, OverlayWindow.windowsBuild(), var0}
         );
      } else if (var8 - var10[0] >= 60000000000L) {
         LOGGER.warn(
            "[EFN] No Inv Leak: Stream Only failed again while {} ({}), {} more time(s) since the last report; {}",
            new Object[]{var1, var4, var10[1] + 1L, var2}
         );
         var10[0] = var8;
         var10[1] = 0L;
      } else {
         var10[1]++;
      }

      if (var3 && !toldUnsupported) {
         toldUnsupported = true;
         NoInvLeakModule.streamNotice("Stream Only unavailable", ": " + var4 + ". Items stay hidden for you too.");
      } else if (!var3 && !toldPaused) {
         toldPaused = true;
         NoInvLeakModule.streamNotice("Stream Only paused", ": " + var4 + ". Retrying automatically.");
      }

      try {
         release();
      } catch (RuntimeException | LinkageError var13) {
         LOGGER.debug("[EFN] No Inv Leak: overlay cleanup failed", var13);
      }
   }

   private static void diagnose(MinecraftClient var0, Window var1) {
      if (!diagnosed) {
         diagnosed = true;
         transientFailures = 0;
         boolean var4 = var1.method_4498();
         boolean var3 = var4 && ((BorderlessFullscreen.Switchable)var1).efn$isBorderless();
         String var5 = !var4 ? "windowed" : (var3 ? "borderless fullscreen" : "exclusive fullscreen");

         String var2;
         try {
            var2 = GL11.glGetString(7936) + " / " + GL11.glGetString(7937);
         } catch (RuntimeException | LinkageError var7) {
            var2 = "unknown";
         }

         LOGGER.info(
            "[EFN] No Inv Leak: Stream Only overlay active (Windows build {}, {}, window {}x{} at {},{}, framebuffer {}x{}, gui scale {}, {}, overlay {})",
            new Object[]{
               OverlayWindow.windowsBuild(),
               var5,
               var1.method_4480(),
               var1.method_4507(),
               var1.method_4499(),
               var1.method_4477(),
               var0.method_1522().field_1482,
               var0.method_1522().field_1481,
               var1.method_4495(),
               var2,
               slotWindow != null ? slotWindow.describe() : "pending"
            }
         );
         if (var4 && !var3 && !toldFullscreen) {
            toldFullscreen = true;
            NoInvLeakModule.streamNotice("Stream Only", ": exclusive fullscreen can hide the overlay from you. Turn on Borderless Fullscreen.");
         }
      }
   }

   public static void stop() {
      diagnosed = false;
      if (RenderSystem.isOnRenderThread()) {
         release();
      } else {
         MinecraftClient.method_1551().execute(StreamOverlay::release);
      }
   }

   private static boolean started() {
      return slotWindow != null || cursorWindow != null || scratch != null || TextPanels.started();
   }

   private static void layers(MinecraftClient var0, Window var1, long var2, int var4, int var5, int var6, boolean var7, boolean var8, int var9) {
      long var11 = var2 & 2199023255551L;
      boolean var10 = var11 != 0L || var8;
      if (!var10 && (var2 >>> 41 & 1L) == 0L) {
         hideSlotsAndCursor();
      } else {
         int var14 = 16 * var4;
         if (var14 != iconPx || cachedBytes > 4194304) {
            resetIcons(var14);
         }

         if (scratch == null) {
            allocate(var14);
         }

         if (slotWindow == null && var10) {
            slotWindow = new OverlayWindow(var1.method_4490());
         }

         if (cursorWindow == null && (var2 >>> 41 & 1L) != 0L) {
            cursorWindow = new OverlayWindow(var1.method_4490());
         }

         boolean var15 = var11 != shownMask
            || var4 != shownScale
            || var7 != shownOpaque
            || var8 != shownSelection
            || var8 && (selectionX != shownSelectionX || selectionY != shownSelectionY);
         boolean var16 = false;

         for (int var17 = 0; var17 < 41; var17++) {
            if ((var11 >>> var17 & 1L) != 0L) {
               if (guiX[var17] != shownX[var17] || guiY[var17] != shownY[var17]) {
                  var15 = true;
               } else if (stack[var17] != shownStack[var17] || stack[var17].method_7947() != shownCount[var17] || bar(stack[var17]) != shownBar[var17]) {
                  var16 = true;
               }
            }
         }

         if (var15) {
            backgroundValid = false;
            dropAsync();
         }

         int var20 = resolveIcons(var2, var14);
         boolean var18 = var7 && var11 != 0L && !backgroundValid;
         boolean var19 = false;
         if (var20 > 0 || var18) {
            synchronousRead(var11, var4, var18, var20);
            if (var18) {
               var19 = true;
               shownHover = var9;
            }
         } else if (var7 && var11 != 0L) {
            var19 = takeAsync(var11, var4);
         }

         if (!var10) {
            if (slotWindow != null) {
               slotWindow.hide();
            }

            shownMask = 0L;
         } else if (var15 || var16 || var19 || var20 > 0) {
            composeSlots(var11, var4, var7, var8, var5, var6);
         } else if (var5 != shownWindowX || var6 != shownWindowY) {
            slotWindow.move(var5 + boxX, var6 + boxY);
            shownWindowX = var5;
            shownWindowY = var6;
         }

         if (var7
            && var11 != 0L
            && asyncFence == null
            && backgroundValid
            && (var9 != shownHover || hotbarVisible() && System.nanoTime() - backgroundAt >= 33000000L)) {
            requestAsync(var11, var4);
            shownHover = var9;
         }

         cursor(var2, var4, var5, var6);
      }
   }

   private static void hideSlotsAndCursor() {
      if (slotWindow != null) {
         slotWindow.hide();
      }

      if (cursorWindow != null) {
         cursorWindow.hide();
      }

      shownMask = 0L;
      cursorStack = null;
   }

   private static long withoutEmpty(long var0, boolean var2) {
      if (var2) {
         return var0;
      } else {
         long var3 = var0;

         for (int var5 = 0; var5 < 41; var5++) {
            if ((var3 >>> var5 & 1L) != 0L && stack[var5].method_7960()) {
               var3 &= ~(1L << var5);
            }
         }

         return var3;
      }
   }

   private static void hideAll() {
      hideSlotsAndCursor();
      TextPanels.hide();
   }

   private static int bar(ItemStack var0) {
      return var0.method_31578() ? var0.method_31580() << 5 | var0.method_31579() : -1;
   }

   private static int cellX(int var0, int var1) {
      return var0 % 7 * var1;
   }

   private static int cellY(int var0, int var1) {
      return var0 / 7 * var1;
   }

   private static int resolveIcons(long var0, int var2) {
      int var3 = 0;

      for (int var4 = 0; var4 < 42; var4++) {
         if ((var0 >>> var4 & 1L) != 0L) {
            ItemStack var5 = stack[var4];
            if (var5 != shownStack[var4]) {
               slotIcon[var4] = null;
            }

            if (slotIcon[var4] == null) {
               if (!var5.method_7960() && look.resolve(var5)) {
                  int[] var6 = (int[])icons.get(look.identity);
                  if (var6 != null) {
                     slotIcon[var4] = var6;
                  } else {
                     int var7 = var3++;

                     try {
                        ItemIcons.render(look, scratch, scratchView, scratchDepth, scratchDepthView, cellX(var7, var2), cellY(var7, var2), var2);
                     } catch (RuntimeException var10) {
                        slotIcon[var4] = BLANK;
                        look.method_65605();
                        String var9 = "icon|" + var5.method_7909();
                        if (reported.putIfAbsent(var9, new long[]{System.nanoTime(), 0L}) == null) {
                           LOGGER.warn("[EFN] No Inv Leak: Stream Only could not draw the icon for {}; it stays blank", var5.method_7909(), var10);
                        }
                        continue;
                     }

                     pendingSlot[var7] = var4;
                     pendingKey[var7] = Arrays.asList(look.identity.toArray());
                  }

                  look.method_65605();
               } else {
                  slotIcon[var4] = BLANK;
               }
            }
         }
      }

      return var3;
   }

   private static void resetIcons(int var0) {
      icons.clear();
      cachedBytes = 0;
      Arrays.fill((Object[])slotIcon, null);
      dropAsync();
      backgroundValid = false;
      if (var0 != iconPx || scratch == null) {
         closeTextures();
         Arrays.fill((Object[])background, null);
         Arrays.fill((Object[])backgroundNext, null);
      }
   }

   private static void allocate(int var0) {
      int var1 = 7 * var0;
      GpuDevice var2 = RenderSystem.getDevice();
      scratch = var2.createTexture("EFN overlay icons", 15, TextureFormat.RGBA8, var1, var1, 1, 1);
      scratchView = var2.createTextureView(scratch);
      scratchDepth = var2.createTexture("EFN overlay icon depth", 8, TextureFormat.DEPTH32, var1, var1, 1, 1);
      scratchDepthView = var2.createTextureView(scratchDepth);
      var2.createCommandEncoder().clearColorAndDepthTextures(scratch, 0, scratchDepth, 1.0);
      iconPx = var0;
   }

   private static GpuTexture itemAtlas(MinecraftClient var0) {
      try {
         return var0.method_72703().method_73025(Atlases.field_64477).method_68004();
      } catch (RuntimeException var2) {
         return null;
      }
   }

   private static void synchronousRead(long var0, int var2, boolean var3, int var4) {
      int var5 = 16 * var2;
      int var6 = 7 * var5;
      long var7 = var4 > 0 ? (long)var6 * var6 * 4L : 0L;
      boolean var9 = var3 && region(var0, var2, syncRegion);
      long var10 = var9 ? (long)syncRegion[2] * syncRegion[3] * 4L : 0L;
      long var12 = var9 ? var7 + frameTextureBytes() : var7;
      long var14 = var7 + var10;
      if (var3) {
         backgroundValid = true;
      }

      if (var12 == 0L) {
         if (var3) {
            unpackBackground(null, 0, syncRegion, false, var0, var2);
         }
      } else {
         GpuDevice var16 = RenderSystem.getDevice();
         if (readback == null || readback.size() < var12) {
            if (readback != null) {
               readback.close();
            }

            readback = var16.createBuffer(() -> "EFN overlay readback", 9, var12);
         }

         CommandEncoder var17 = var16.createCommandEncoder();
         packDefaults();

         try {
            if (var4 > 0) {
               var17.copyTextureToBuffer(scratch, readback, 0L, NOTHING, 0);
            }

            if (var9) {
               copyRegion(var17, readback, var7, syncRegion);
            }
         } finally {
            packReset();
         }

         GpuFence var18 = var17.createFence();

         try {
            GL11.glFlush();
            if (!var18.awaitCompletion(1000000000L)) {
               throw new IllegalStateException("overlay read-back timed out");
            }
         } catch (Throwable var29) {
            if (var18 != null) {
               try {
                  var18.close();
               } catch (Throwable var28) {
                  var29.addSuppressed(var28);
               }
            }

            throw var29;
         }

         if (var18 != null) {
            var18.close();
         }

         MappedView var32 = var17.mapBuffer(readback.slice(0L, var14), true, false);

         try {
            ByteBuffer var19 = var32.data();

            for (int var20 = 0; var20 < var4; var20++) {
               int[] var21 = unpackIcon(var19, cellX(var20, var5), cellY(var20, var5), var6, var5);
               icons.put(pendingKey[var20], var21);
               pendingKey[var20] = null;
               cachedBytes += var5 * var5 * 4;
               slotIcon[pendingSlot[var20]] = var21;
            }

            if (var3) {
               unpackBackground(var19, (int)var7, syncRegion, var9, var0, var2);
               backgroundAt = System.nanoTime();
            }
         } catch (Throwable var30) {
            if (var32 != null) {
               try {
                  var32.close();
               } catch (Throwable var27) {
                  var30.addSuppressed(var27);
               }
            }

            throw var30;
         }

         if (var32 != null) {
            var32.close();
         }
      }
   }

   private static void requestAsync(long var0, int var2) {
      backgroundAt = System.nanoTime();
      if (region(var0, var2, asyncRegion)) {
         long var3 = frameTextureBytes();
         GpuDevice var5 = RenderSystem.getDevice();
         if (asyncBuffer == null || asyncBuffer.size() < var3) {
            if (asyncBuffer != null) {
               asyncBuffer.close();
            }

            asyncBuffer = var5.createBuffer(() -> "EFN overlay background", 9, var3);
         }

         CommandEncoder var6 = var5.createCommandEncoder();
         packDefaults();

         try {
            copyRegion(var6, asyncBuffer, 0L, asyncRegion);
         } finally {
            packReset();
         }

         asyncFence = var6.createFence();
         GL11.glFlush();
         asyncMask = var0;
         asyncScale = var2;

         for (int var7 = 0; var7 < 41; var7++) {
            asyncX[var7] = guiX[var7];
            asyncY[var7] = guiY[var7];
         }
      }
   }

   private static boolean takeAsync(long var0, int var2) {
      if (asyncFence != null && asyncFence.awaitCompletion(0L)) {
         boolean var3 = asyncMask == var0 && asyncScale == var2;

         for (int var4 = 0; var3 && var4 < 41; var4++) {
            if ((var0 >>> var4 & 1L) != 0L && (asyncX[var4] != guiX[var4] || asyncY[var4] != guiY[var4])) {
               var3 = false;
            }
         }

         boolean var13 = false;
         if (var3) {
            long var5 = (long)asyncRegion[2] * asyncRegion[3] * 4L;
            CommandEncoder var7 = RenderSystem.getDevice().createCommandEncoder();
            MappedView var8 = var7.mapBuffer(asyncBuffer.slice(0L, var5), true, false);

            try {
               var13 = unpackBackground(var8.data(), 0, asyncRegion, true, var0, var2);
            } catch (Throwable var12) {
               if (var8 != null) {
                  try {
                     var8.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (var8 != null) {
               var8.close();
            }
         }

         dropAsync();
         return var13;
      } else {
         return false;
      }
   }

   private static void dropAsync() {
      if (asyncFence != null) {
         asyncFence.close();
         asyncFence = null;
      }
   }

   private static long frameTextureBytes() {
      GpuTexture var0 = MinecraftClient.method_1551().method_1522().method_30277();
      return (long)var0.getWidth(0) * var0.getHeight(0) * 4L;
   }

   private static void packDefaults() {
      GlStateManager._pixelStore(3333, 4);
      GlStateManager._pixelStore(3331, 0);
      GlStateManager._pixelStore(3332, 0);
   }

   private static void packReset() {
      GlStateManager._pixelStore(3330, 0);
   }

   private static boolean region(long var0, int var2, int[] var3) {
      GpuTexture var4 = MinecraftClient.method_1551().method_1522().method_30277();
      int var5 = 16 * var2;
      int var6 = 2147483647;
      int var7 = 2147483647;
      int var8 = -2147483648;
      int var9 = -2147483648;

      for (int var10 = 0; var10 < 41; var10++) {
         if ((var0 >>> var10 & 1L) != 0L) {
            var6 = Math.min(var6, guiX[var10] * var2);
            var7 = Math.min(var7, guiY[var10] * var2);
            var8 = Math.max(var8, guiX[var10] * var2 + var5);
            var9 = Math.max(var9, guiY[var10] * var2 + var5);
         }
      }

      var6 = Math.max(var6, 0);
      var7 = Math.max(var7, 0);
      var8 = Math.min(var8, var4.getWidth(0));
      var9 = Math.min(var9, var4.getHeight(0));
      if (var8 > var6 && var9 > var7) {
         var3[0] = var6;
         var3[1] = var7;
         var3[2] = var8 - var6;
         var3[3] = var9 - var7;
         return true;
      } else {
         return false;
      }
   }

   private static void copyRegion(CommandEncoder var0, GpuBuffer var1, long var2, int[] var4) {
      GpuTexture var5 = MinecraftClient.method_1551().method_1522().method_30277();
      var0.copyTextureToBuffer(var5, var1, var2, NOTHING, 0, var4[0], var5.getHeight(0) - var4[1] - var4[3], var4[2], var4[3]);
   }

   private static int[] unpackIcon(ByteBuffer var0, int var1, int var2, int var3, int var4) {
      int[] var5 = new int[var4 * var4];

      for (int var6 = 0; var6 < var4; var6++) {
         int var7 = ((var3 - 1 - (var2 + var6)) * var3 + var1) * 4;

         for (int var8 = 0; var8 < var4; var8++) {
            int var9 = var7 + var8 * 4;
            int var10 = var0.get(var9 + 3) & 255;
            var5[var6 * var4 + var8] = var10 << 24
               | Math.min(var10, var0.get(var9) & 255) << 16
               | Math.min(var10, var0.get(var9 + 1) & 255) << 8
               | Math.min(var10, var0.get(var9 + 2) & 255);
         }
      }

      return var5;
   }

   private static boolean unpackBackground(ByteBuffer var0, int var1, int[] var2, boolean var3, long var4, int var6) {
      int var7 = 16 * var6;
      int var8 = var2[0];
      int var9 = var2[1];
      int var10 = var2[2];
      int var11 = var2[3];
      boolean var12 = false;

      for (int var13 = 0; var13 < 41; var13++) {
         if ((var4 >>> var13 & 1L) != 0L) {
            int var14 = guiX[var13] * var6 - var8;
            int var15 = guiY[var13] * var6 - var9;
            if (var3 && var14 >= 0 && var15 >= 0 && var14 + var7 <= var10 && var15 + var7 <= var11) {
               int[] var16 = backgroundNext[var13];
               if (var16 == null || var16.length != var7 * var7) {
                  var16 = new int[var7 * var7];
               }

               for (int var17 = 0; var17 < var7; var17++) {
                  int var18 = var1 + ((var11 - 1 - (var15 + var17)) * var10 + var14) * 4;

                  for (int var19 = 0; var19 < var7; var19++) {
                     int var20 = var18 + var19 * 4;
                     var16[var17 * var7 + var19] = 0xFF000000 | (var0.get(var20) & 255) << 16 | (var0.get(var20 + 1) & 255) << 8 | var0.get(var20 + 2) & 255;
                  }
               }

               int[] var21 = background[var13];
               if (var21 != null && Arrays.equals(var21, var16)) {
                  backgroundNext[var13] = var16;
               } else {
                  var12 = true;
                  background[var13] = var16;
                  backgroundNext[var13] = var21;
               }
            } else {
               if (background[var13] != null) {
                  var12 = true;
               }

               background[var13] = null;
            }
         }
      }

      return var12;
   }

   private static void composeSlots(long var0, int var2, boolean var3, boolean var4, int var5, int var6) {
      int var10 = 16 * var2;
      OverlayArt.Image var11 = var4 ? OverlayArt.selection() : null;
      boolean var12 = var11 != null;
      int var13 = 2147483647;
      int var14 = 2147483647;
      int var15 = -2147483648;
      int var16 = -2147483648;

      for (int var17 = 0; var17 < 41; var17++) {
         if ((var0 >>> var17 & 1L) != 0L) {
            var13 = Math.min(var13, guiX[var17]);
            var14 = Math.min(var14, guiY[var17]);
            var15 = Math.max(var15, guiX[var17] + 17);
            var16 = Math.max(var16, guiY[var17] + 17);
         }
      }

      if (var12) {
         var13 = Math.min(var13, selectionX);
         var14 = Math.min(var14, selectionY);
         var15 = Math.max(var15, selectionX + var11.width());
         var16 = Math.max(var16, selectionY + var11.height());
      }

      if (var13 > var15) {
         slotWindow.hide();
         shownMask = 0L;
      } else {
         int var26 = var13 * var2;
         int var18 = var14 * var2;
         int var19 = (var15 - var13) * var2;
         int var20 = (var16 - var14) * var2;
         canvas.wrap(slotWindow.pixels(var19, var20), slotWindow.stride(), var19, var20);
         if (var3) {
            for (int var9 = 0; var9 < 41; var9++) {
               if ((var0 >>> var9 & 1L) != 0L) {
                  int var8 = guiX[var9] * var2 - var26;
                  int var7 = guiY[var9] * var2 - var18;
                  int[] var21 = background[var9];
                  if (var21 != null) {
                     canvas.copy(var21, var10, var10, var8, var7);
                  } else {
                     canvas.rect(var8, var7, var10, var10, -7631989);
                  }
               }
            }
         }

         if (var12) {
            canvas.blend(var11.pixels(), var11.width(), var11.height(), selectionX * var2 - var26, selectionY * var2 - var18, var2);
         }

         for (int var24 = 0; var24 < 41; var24++) {
            if ((var0 >>> var24 & 1L) != 0L) {
               int var23 = guiX[var24] * var2 - var26;
               int var22 = guiY[var24] * var2 - var18;
               int[] var27 = slotIcon[var24];
               if (var27 != null && var27 != BLANK) {
                  canvas.blend(var27, var10, var10, var23, var22, 1);
               }

               if (!stack[var24].method_7960()) {
                  DigitFont.decorations(canvas, stack[var24], var23, var22, var2);
               }
            }
         }

         slotWindow.present(var5 + var26, var6 + var18, var19, var20);
         diagnose(MinecraftClient.method_1551(), MinecraftClient.method_1551().method_22683());

         for (int var25 = 0; var25 < 41; var25++) {
            if ((var0 >>> var25 & 1L) != 0L) {
               shownStack[var25] = stack[var25];
               shownX[var25] = guiX[var25];
               shownY[var25] = guiY[var25];
               shownCount[var25] = stack[var25].method_7947();
               shownBar[var25] = bar(stack[var25]);
            }
         }

         shownMask = var0;
         shownScale = var2;
         shownOpaque = var3;
         shownSelection = var4;
         shownSelectionX = selectionX;
         shownSelectionY = selectionY;
         shownWindowX = var5;
         shownWindowY = var6;
         boxX = var26;
         boxY = var18;
      }
   }

   private static void cursor(long var0, int var2, int var3, int var4) {
      if ((var0 >>> 41 & 1L) == 0L) {
         if (cursorWindow != null) {
            cursorWindow.hide();
         }

         cursorStack = null;
      } else {
         ItemStack var6 = stack[41];
         int var7 = 16 * var2;
         int var8 = var3 + guiX[41] * var2;
         int var9 = var4 + guiY[41] * var2;
         boolean var5 = var6 == cursorStack && var6.method_7947() == cursorCount && bar(var6) == cursorBar && var2 == cursorScale;
         if (var5) {
            if (var8 != cursorX || var9 != cursorY) {
               cursorWindow.move(var8, var9);
            }
         } else {
            int var11 = var7 + var2;
            canvas.wrap(cursorWindow.pixels(var11, var11), cursorWindow.stride(), var11, var11);
            int[] var12 = slotIcon[41];
            if (var12 != null && var12 != BLANK) {
               canvas.blend(var12, var7, var7, 0, 0, 1);
            }

            DigitFont.decorations(canvas, var6, 0, 0, var2);
            cursorWindow.present(var8, var9, var11, var11);
            cursorStack = var6;
            cursorCount = var6.method_7947();
            cursorBar = bar(var6);
            cursorScale = var2;
         }

         cursorX = var8;
         cursorY = var9;
         shownStack[41] = var6;
      }
   }

   private static void closeTextures() {
      if (scratchView != null) {
         scratchView.close();
      }

      if (scratch != null) {
         scratch.close();
      }

      if (scratchDepthView != null) {
         scratchDepthView.close();
      }

      if (scratchDepth != null) {
         scratchDepth.close();
      }

      scratchView = null;
      scratch = null;
      scratchDepthView = null;
      scratchDepth = null;
      iconPx = 0;
   }

   private static void release() {
      dropAsync();
      if (slotWindow != null) {
         slotWindow.destroy();
         slotWindow = null;
      }

      if (cursorWindow != null) {
         cursorWindow.destroy();
         cursorWindow = null;
      }

      TextPanels.release();
      closeTextures();
      if (readback != null) {
         readback.close();
         readback = null;
      }

      if (asyncBuffer != null) {
         asyncBuffer.close();
         asyncBuffer = null;
      }

      icons.clear();
      cachedBytes = 0;
      Arrays.fill((Object[])slotIcon, null);
      Arrays.fill(pendingKey, null);
      Arrays.fill((Object[])background, null);
      Arrays.fill((Object[])backgroundNext, null);
      Arrays.fill(stack, null);
      Arrays.fill(shownStack, null);
      mask = 0L;
      fakeMask = 0L;
      shownMask = 0L;
      shownAtlas = null;
      shownHover = -1;
      cursorStack = null;
      backgroundValid = false;
   }

   private static List<Object>[] newKeys(int var0) {
      return new List[var0];
   }
}
