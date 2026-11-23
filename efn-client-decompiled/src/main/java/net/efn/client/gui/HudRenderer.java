package net.efn.client.gui;

import com.example.client.mixin.AccessorHudToastInstance;
import com.example.client.mixin.AccessorHudToastManager;
import com.mojang.blaze3d.textures.GpuTexture;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImFontGlyph;
import imgui.ImGui;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.nio.ByteBuffer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.efn.client.config.HudTheme;
import net.efn.client.config.Surface;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.ModuleRevision;
import net.efn.client.module.impl.client.HotkeysModule;
import net.efn.client.module.impl.client.HudModule;
import net.efn.client.module.impl.client.RadarModule;
import net.efn.client.module.impl.client.RegionMapModule;
import net.efn.client.module.impl.client.SpotifyHudModule;
import net.efn.client.module.impl.misc.AntiVanishModule;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.efn.client.module.impl.misc.StaffListModule;
import net.efn.client.util.spotify.AlbumArt;
import net.efn.client.util.spotify.DisplayText;
import net.efn.client.util.spotify.Lyrics;
import net.efn.client.util.spotify.NowPlaying;
import net.efn.client.util.spotify.PlaybackClock;
import net.efn.client.util.spotify.SpotifyWindow;
import net.efn.client.util.spotify.SyncedLyrics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.Toast.Visibility;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.Window;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

@Environment(EnvType.CLIENT)
public final class HudRenderer {
   private static final HudRenderer INSTANCE = new HudRenderer();
   private static final float CAP_H = 28.0F;
   private static final float PAD_X = 12.0F;
   private static final float GAP_UNIT = 4.0F;
   private static final float GAP_DIV = 10.0F;
   private static final float DIV_H = 12.0F;
   private static final float EMBLEM_H = 24.0F;
   static final float ANIM_SPEED = 12.0F;
   private static final float WIDTH_SPEED = 16.0F;
   private static final float INTRO_DURATION = 0.36F;
   private static final float INTRO_STAGGER = 0.04F;
   private static final float INTRO_SLIDE = 6.0F;
   private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
   private static final String MINUS = "\u2212";
   private static final String INFINITY = "\u221e";
   private static final String[] SECONDS = IntStream.range(0, 60).mapToObj(var0 -> String.format(":%02d", var0)).toArray(String[]::new);
   private final HudLayoutStore layout = HudLayoutStore.getInstance();
   private final ModuleListRows moduleRows = new ModuleListRows();
   private final TargetCard targetCard = new TargetCard();
   private final ArmorStatus armorStatus = new ArmorStatus();
   private final CooldownTray cooldownTray = new CooldownTray();
   private final Map<String, Int2ObjectOpenHashMap<StableWidth>> slots = new HashMap<>();
   private int slotCount;
   private float moduleListW;
   private float toastShift;
   private boolean layoutDirty;
   private long lastWheelNanos;
   private final HudModule hudModule;
   private final RegionMapModule regionMapModule;
   private final RadarModule radarModule;
   private float introTime;
   private boolean hudVisibleLastFrame;
   private String draggingId;
   private float dragOffsetX;
   private static final SpotifyWindow.MediaKey[] SPOTIFY_BUTTONS = new SpotifyWindow.MediaKey[]{
      SpotifyWindow.MediaKey.PREVIOUS, SpotifyWindow.MediaKey.PLAY_PAUSE, SpotifyWindow.MediaKey.NEXT
   };
   private final float[] spotifyButtons = new float[12];
   private boolean spotifyButtonsShown;
   private boolean spotifyButtonsDrawn;
   private boolean chatInput;
   private float dragOffsetY;
   private Float snapGuideX;
   private Float snapGuideY;
   private static int studioFrame = -2;
   private static volatile float[] infoBarBox;
   private static volatile long infoBarDrawnAt;
   private static final float HIDDEN_ALPHA = 0.35F;
   private long infoSecond = -9223372036854775808L;
   private int infoFps = -1;
   private int infoPing = -1;
   private List<HudRenderer.InfoItem> infoItems = List.of();
   private static final float ROWS_RADIUS = 6.0F;
   private static final int ROWS_SHADOW_LAYERS = 3;
   private static final int ROWS_TEXT = -1;
   private static final float COORD_ROW_H = 20.0F;
   private static final float COORD_PAD = 7.0F;
   private static final float COORD_ICON = 16.0F;
   private static final float COORD_GAP_ICON = 8.0F;
   private static final float COORD_GAP = 12.0F;
   private static final float COORD_ELSEWHERE_ALPHA = 0.62F;
   private static final String[] COORD_SLOTS = new String[]{"coords.0", "coords.1", "coords.2"};
   private final Map<String, Float> coordAnim = new HashMap<>();
   private static final float LIST_ROW_H = 18.0F;
   private static final float LIST_PAD = 6.0F;
   private static final String NO_MODULES = "No active modules";
   private static final float TOAST_GAP = 10.0F;
   private static final float POTION_ROW_H = 22.0F;
   private static final float POTION_PAD = 6.0F;
   private static final float ICON = 18.0F;
   private static final float GAP_ICON = 6.0F;
   private static final float GAP_DURATION = 14.0F;
   private static final String NO_EFFECTS = "No active effects";
   private static final float VANISH_ROW_H = 22.0F;
   private static final float VANISH_PAD = 8.0F;
   private static final float GAP_TAG = 8.0F;
   private static final String NO_VANISH = "No vanish alerts";
   private static final float STAFF_ROW_H = 22.0F;
   private static final float STAFF_HEADER_H = 20.0F;
   private static final float STAFF_PAD = 8.0F;
   private static final float STAFF_FACE = 16.0F;
   private static final float GAP_FACE = 6.0F;
   private static final float GAP_COUNT = 5.0F;
   private static final float GAP_STATE = 14.0F;
   private static final String NO_STAFF = "No staff online";
   private static final String STAFF_HEADER = "STAFF";
   private static final String WATCHING_YOU = "WATCHING YOU";
   private static final float SKIN_FACE_U = 0.125F;
   private static final float SKIN_HAT_U = 0.625F;
   private static final float SKIN_TOP_V = 0.125F;
   private static final float SKIN_PART = 0.125F;
   private final Map<UUID, Identifier> staffSkins = new HashMap<>();
   private final Set<GpuTexture> nearestSkins = Collections.newSetFromMap(new WeakHashMap<>());
   private static final float RADAR_DEFAULT_SIZE = 150.0F;
   private static final float[] RADAR_RINGS = new float[]{0.33333334F, 0.6666667F};
   private static final float RADAR_LETTER_INSET = 10.0F;
   private static final float RADAR_LETTER_CLEAR = 4.0F;
   private static final float RADAR_EDGE = 5.0F;
   private static final float RADAR_SELF = 3.0F;
   private static final float RADAR_MARKER_INSET = 1.5F;
   private static final float RADAR_MARKER_H = 5.0F;
   private static final float RADAR_MARKER_W = 3.5F;
   private static final float RADAR_DOT = 3.2F;
   private static final String[] RADAR_LETTERS = new String[]{"N", "E", "S", "W"};
   private static final int[] RADAR_LETTER_DX = new int[]{0, 1, 0, -1};
   private static final int[] RADAR_LETTER_DZ = new int[]{-1, 0, 1, 0};
   private static final int RADAR_DOT_EDGE = -1946157056;
   private final float[] radarLetterW = new float[4];
   private float radarLetterMid;
   private float radarLetterHalf;
   private ImFont radarLetterFont;
   private int radarLetterPx = -1;
   private static final float HOTKEY_ROW_H = 22.0F;
   private static final float HOTKEY_HEADER_H = 20.0F;
   private static final float HOTKEY_PAD = 8.0F;
   private static final float GAP_HOTKEY = 16.0F;
   private static final float KEYCAP_H = 16.0F;
   private static final float KEYCAP_PAD = 5.0F;
   private static final float KEYCAP_RADIUS = 4.0F;
   private static final String HOTKEYS_HEADER = "HOTKEYS";
   private static final String NO_HOTKEYS = "No keys bound";
   private List<HudRenderer.HotkeyLine> hotkeyLines = List.of();
   private long hotkeyRevision = -9223372036854775808L;
   private ImFont hotkeyFont;
   private int hotkeyTextPx = -1;
   private int hotkeyKeyPx = -1;
   private float hotkeyNameColumn;
   private float hotkeyKeyColumn;
   private float hotkeyHeaderW;
   private float hotkeyCapMid;
   private static final float SPOTIFY_PAD_Y = 8.0F;
   private static final float SPOTIFY_LINE_GAP = 2.0F;
   private static final float SPOTIFY_GLYPH = 10.0F;
   private static final float SPOTIFY_GLYPH_GAP = 10.0F;
   private static final float SPOTIFY_PAUSED = 0.62F;
   private static final float MARQUEE_SPEED = 30.0F;
   private static final float MARQUEE_HOLD = 2.4F;
   private static final float MARQUEE_GAP = 40.0F;
   private static final String ELLIPSIS = "...";
   private static final float CARD_PAD = 10.0F;
   private static final float CARD_MIN_WIDTH = 200.0F;
   private static final float CARD_DEFAULT_WIDTH = 320.0F;
   private static final float ART_GAP = 10.0F;
   private static final float ART_MIN_COLUMN = 128.0F;
   private static final float ART_RADIUS = 4.0F;
   private static final int ART_PLACEHOLDER = 318767103;
   private static final float LABEL_GAP = 3.0F;
   private static final float ARTIST_GAP = 1.0F;
   private static final float TRANSPORT_GAP = 7.0F;
   private static final float PROGRESS_GAP = 6.0F;
   private static final float PROGRESS_H = 2.0F;
   private static final float SKIP_GLYPH = 8.0F;
   private static final float PLAY_GLYPH = 10.0F;
   private static final float GLYPH_SPACING = 14.0F;
   private static final float GLYPH_MARGIN = 8.0F;
   private static final float LYRICS_GAP = 12.0F;
   private static final float LYRIC_ROW_GAP = 6.0F;
   private static final float LYRIC_LINE_GAP = 6.0F;
   private static final float LYRIC_SPRING = 13.0F;
   private static final float LYRIC_SETTLE_TIME = 1.1F;
   private static final float LYRIC_STAGGER = 0.045F;
   private static final int LYRIC_MAX_STAGGERED = 6;
   private static final int LYRIC_MAX_SHIFTS = 8;
   private static final float LYRIC_GLOW_TIME = 0.35F;
   private static final float LYRIC_ARRIVE_TIME = 0.3F;
   private static final float LYRIC_FADE_TIME = 0.32F;
   private static final float LYRIC_RISE = 8.0F;
   private static final int LYRIC_MAX_ROWS = 4;
   private static final float LYRIC_GROW_SPEED = 14.0F;
   private static final float LYRIC_SHRINK_SPEED = 9.0F;
   private static final float LYRIC_FAR_ALPHA = 0.6F;
   private static final float LYRIC_PAUSED_ALPHA = 0.7F;
   private static final float LYRIC_PAUSE_SPEED = 8.0F;
   private static final float LYRIC_BREATH_PERIOD = 1.6F;
   private static final long LYRIC_LEAD_MS = 200L;
   private static final long LYRIC_SWEEP_MS = 180L;
   private static final float LYRIC_SLOW_PACE = 2.0F;
   private static final float LYRIC_CHAR_MS = 90.0F;
   private static final float LYRIC_LIT_ALL = 1.0F / 0.0F;
   private static final long LYRIC_QUIET_MS = 700L;
   private static final float LYRIC_MIN_COVERAGE = 0.75F;
   private static final String[] NO_ROWS = new String[0];
   private static final NowPlaying SAMPLE_TRACK = new NowPlaying(
      NowPlaying.Status.PLAYING, "Golden Hour Drive", "Sample Artist", "", NowPlaying.Source.SESSION, 214000L, 84000L, 0L, 1.0, null
   );
   private static final Lyrics SAMPLE_LYRICS = new Lyrics(
      Lyrics.Status.SYNCED,
      SyncedLyrics.parse(
         "[01:21.00]Every light in the city knows my name\n[01:27.40]We keep on driving till the morning comes\n[01:33.80]Nothing but the radio and you\n"
      ),
      List.of(),
      0L
   );
   private float marqueeTime;
   private String marqueeTitle = "";
   private String marqueeArtist = "";
   private final PlaybackClock spotifyClock = new PlaybackClock();
   private NowPlaying spotifyFolded;
   private String spotifyTitle = "";
   private String spotifyArtist = "";
   private AlbumArt spotifyArt;
   private ThemeImages.Texture spotifyArtTexture;
   private final Map<Long, Float> capTops = new HashMap<>();
   private Lyrics lyricPrepared;
   private String[] lyricTexts = NO_ROWS;
   private String[][] lyricRows = new String[0][];
   private int lyricRowsPx;
   private int lyricRowsWidth;
   private HudRenderer.LyricWords[] lyricWords = new HudRenderer.LyricWords[0];
   private float lyricCharMs = 90.0F;
   private boolean lyricReadable = true;
   private float lyricViewH = -1.0F;
   private int lyricViewLines;
   private float lyricViewScale;
   private SyncedLyrics lyricSource;
   private int lyricStart;
   private final List<HudRenderer.LyricShift> lyricShifts = new ArrayList<>(8);
   private float lyricClock;
   private List<HudRenderer.LyricItem> lyricFadeItems;
   private float lyricFade = 1.0F;
   private List<HudRenderer.LyricItem> lyricLastItems = List.of();
   private List<HudRenderer.LyricItem> lyricLeaveItems;
   private float lyricLeave;
   private float[] lyricShown = new float[0];
   private float lyricBreath;
   private float lyricPlaying = 1.0F;
   private int lyricCurrentFirst = -1;
   private int lyricCurrentLast = -1;
   private float[] lyricBright = new float[0];
   private int lyricBrightLo;
   private int lyricBrightHi = -1;
   private float lyricBlockH;
   private long lyricLoadingSince;
   private ImFont glyphFont;
   private final Map<Integer, Boolean> glyphs = new HashMap<>();
   private static final float SNAP_THRESHOLD = 6.0F;

   private HudRenderer() {
      this.hudModule = ModuleRegistry.get(HudModule.class);
      this.regionMapModule = ModuleRegistry.get(RegionMapModule.class);
      this.radarModule = ModuleRegistry.get(RadarModule.class);
   }

   public static HudRenderer getInstance() {
      return INSTANCE;
   }

   public static float[] infoBarBox() {
      return System.nanoTime() - infoBarDrawnAt < 250000000L ? infoBarBox : null;
   }

   static void markStudioPreview() {
      studioFrame = ImGuiManager.getInstance().frameCount();
   }

   static boolean studioPreview() {
      int var0 = ImGuiManager.getInstance().frameCount() - studioFrame;
      return var0 == 0 || var0 == 1;
   }

   boolean isDragging() {
      return this.draggingId != null;
   }

   void studioInput(float var1, float var2, boolean var3) {
      markStudioPreview();
      if (!var3 || this.draggingId != null) {
         this.updateEditorInput(var1, var2);
      }
   }

   public void render() {
      MinecraftClient var6 = MinecraftClient.method_1551();
      if (var6.field_1687 == null) {
         this.releaseSpotifyArt();
         this.resetCombatElements();
      } else {
         boolean var7 = var6.field_1755 instanceof HudEditorScreen;
         boolean var8 = studioPreview();
         boolean var5 = var7 || var8;
         if (var6.field_1755 instanceof ImGuiScreen && !var8) {
            if (this.draggingId != null || this.layoutDirty) {
               this.saveLayout();
            }

            this.draggingId = null;
            this.snapGuideX = null;
            this.snapGuideY = null;
            this.hudVisibleLastFrame = false;
         } else {
            boolean var10 = var6.field_1755 instanceof ChatScreen;
            boolean var11 = var7 || var10;
            this.chatInput = var10;
            this.spotifyButtonsShown = this.spotifyButtonsDrawn;
            this.spotifyButtonsDrawn = false;
            boolean var12 = shouldRenderHudOverlays(var6);
            float var13 = Math.max(ImGui.getIO().getDeltaTime(), 0.001F);
            float var14 = ImGui.getIO().getDisplaySizeX();
            float var15 = ImGui.getIO().getDisplaySizeY();
            this.flushLayoutIfIdle();
            boolean var16 = computeHudOverlays();
            boolean var17 = this.isRegionMapEnabled();
            HudLayoutStore.HudElementState var18 = this.layout.element("region_map");
            boolean var19 = var17 && var18.visible;
            boolean var4 = this.hudModule != null && (var5 || this.hudModule.isEnabled());
            if (!var4) {
               this.hudVisibleLastFrame = false;
               this.introTime = 0.0F;
               this.clearSlots();
               this.moduleListW = 0.0F;
               this.toastShift = 0.0F;
               this.releaseSpotifyArt();
               this.resetCombatElements();
               if ((var16 || var19) && var12) {
                  this.drawOverlayOnly(var14, var15, var10, var19);
               }
            } else {
               boolean var21 = var5 || this.hudModule.ping();
               boolean var22 = var5 || this.hudModule.fps();
               boolean var23 = var5 || this.hudModule.clock();
               boolean var24 = this.hudModule.shadow();
               this.updateIntro(var5, var13);
               this.moduleRows.update(ModuleRegistry.enabled(), var5, var13);
               if (this.slotCount > 256) {
                  this.clearSlots();
               }

               HudLayoutStore.HudElementState var25 = this.layout.element("info_bar");
               HudLayoutStore.HudElementState var26 = this.layout.element("coordinates");
               HudLayoutStore.HudElementState var27 = this.layout.element("module_list");
               HudLayoutStore.HudElementState var28 = this.layout.element("potion_tracker");
               HudLayoutStore.HudElementState var29 = this.layout.element("anti_vanish");
               HudLayoutStore.HudElementState var30 = this.layout.element("staff_list");
               HudLayoutStore.HudElementState var31 = this.layout.element("spotify");
               HudLayoutStore.HudElementState var32 = this.layout.element("radar");
               HudLayoutStore.HudElementState var33 = this.layout.element("hotkeys");
               HudLayoutStore.HudElementState var34 = this.layout.element("target_card");
               HudLayoutStore.HudElementState var35 = this.layout.element("armor_status");
               HudLayoutStore.HudElementState var36 = this.layout.element("item_cooldowns");
               HudRenderer.InfoPlan var37 = this.planInfoBar(var25, this.buildInfoItems(var22, var23, var21, var6), var13);
               this.layout.updateBounds(var25.id, var37.width(), var37.height(), var14, var15);
               HudRenderer.CoordPlan var38 = this.planCoordinates(var26, this.coordinateRows(var6, var5, var13), var5, var13);
               this.layout.updateBounds(var26.id, var38.width(), var38.height(), var14, var15);
               HudRenderer.ListPlan var39 = this.planModuleList(var27, var5, var13);
               this.layout.updateBounds(var27.id, var39.width(), var39.height(), var14, var15);
               HudRenderer.PotionPlan var40 = this.planPotions(var28, this.buildPotionRows(var6), var5, var13);
               this.layout.updateBounds(var28.id, var40.width(), var40.height(), var14, var15);
               HudRenderer.VanishPlan var41 = this.planVanish(var29, var5, var13);
               this.layout.updateBounds(var29.id, var41.width(), var41.height(), var14, var15);
               HudRenderer.StaffPlan var42 = this.planStaff(var30, var5, var13);
               this.layout.updateBounds(var30.id, var42.width(), var42.height(), var14, var15);
               HudRenderer.SpotifyPlan var43 = this.planSpotify(var31, var5, var13);
               this.layout.updateBounds(var31.id, var43.width(), var43.height(), var14, var15);
               float var44 = this.planRadar(var32, var5);
               this.layout.updateBounds(var32.id, var44, var44, var14, var15);
               HudRenderer.HotkeyPlan var45 = this.planHotkeys(var33, var5, var13);
               this.layout.updateBounds(var33.id, var45.width(), var45.height(), var14, var15);
               boolean var46 = this.hudModule.isEnabled() && !var6.field_1690.field_1842;
               boolean var47 = var46 && var34.visible && this.hudModule.targetCard();
               boolean var48 = var46 && var35.visible && this.hudModule.armorStatus();
               boolean var49 = var46 && var36.visible && this.hudModule.itemCooldowns();
               float var50 = scaleOf(var34);
               float var51 = scaleOf(var35);
               float var52 = scaleOf(var36);
               this.targetCard.update(var6, this.hudModule, var47, var5, var13);
               this.layout.updateBounds(var34.id, this.targetCard.planWidth(var50), this.targetCard.planHeight(var50), var14, var15);
               this.armorStatus.update(var6, this.hudModule, var48, var5, var13);
               this.layout.updateBounds(var35.id, this.armorStatus.planWidth(var51), this.armorStatus.planHeight(var51), var14, var15);
               this.cooldownTray.update(var6, this.hudModule, var49, var5, var13);
               this.layout.updateBounds(var36.id, this.cooldownTray.planWidth(var52), this.cooldownTray.planHeight(var52), var14, var15);
               if (var17 || var5) {
                  this.updateRegionMapBounds(var14, var15);
               }

               if (var11) {
                  this.updateEditorInput(var14, var15);
               } else if (!var8) {
                  if (this.draggingId != null || this.layoutDirty) {
                     this.saveLayout();
                  }

                  this.draggingId = null;
                  this.snapGuideX = null;
                  this.snapGuideY = null;
               }

               float var53 = this.updateToastShift(var6, var27, var5, var13);
               ImGui.setNextWindowPos(0.0F, 0.0F, 1);
               ImGui.setNextWindowSize(var14, var15);
               int var54 = 205215;
               if (!var11) {
                  var54 |= 197120;
               }

               ImGui.pushStyleVar(4, 0.0F);
               if (!ImGui.begin("##hud_overlay", var54)) {
                  ImGui.end();
                  ImGui.popStyleVar();
               } else {
                  ImGui.popStyleVar();
                  ImDrawList var55 = ImGui.getWindowDrawList();
                  if (var12) {
                     drawHudOverlays(var55, var14, var15);
                  }

                  boolean var3;
                  boolean var56 = (var3 = this.hudModule.isEnabled())
                     && var25.visible
                     && (this.hudModule.fps() || this.hudModule.clock() || this.hudModule.ping());
                  boolean var57 = var3 && var26.visible;
                  boolean var58 = var3 && var27.visible && this.hudModule.moduleList();
                  boolean var59 = var3 && var28.visible && this.hudModule.potions();
                  boolean var60 = var3 && var29.visible && AntiVanishModule.shouldShowHud();
                  boolean var61 = var3 && var30.visible && StaffListModule.shouldShowHud();
                  boolean var62 = var3 && var31.visible && SpotifyHudModule.shouldShowHud();
                  boolean var63 = gameHidesHud(var6);
                  boolean var64 = var3 && var32.visible && RadarModule.shouldShowHud() && !var63;
                  boolean var2 = var3 && var33.visible && HotkeysModule.shouldShowHud() && !var63;
                  if (var56 && !var37.segments().isEmpty() || var5) {
                     this.drawInfoBar(var55, var25, var37, var24, this.transform(var25, var5, var56));
                  }

                  if (var56 && !var37.segments().isEmpty() && !var5) {
                     infoBarBox = new float[]{var25.x, var25.y, var25.x + var25.width, var25.y + var25.height};
                     infoBarDrawnAt = System.nanoTime();
                  }

                  if (var57 && !var38.rows().isEmpty() || var5) {
                     this.drawCoordinates(var55, var26, var38, var24, this.transform(var26, var5, var57), var14);
                  }

                  if (var58 && !var39.rows().isEmpty() || var5) {
                     this.drawModuleList(var55, var27, var39, var24, this.transform(var27, var5, var58).down(var53), var14);
                  }

                  if (var59 && !var40.lines().isEmpty() || var5) {
                     this.drawPotions(var55, var28, var40, var24, this.transform(var28, var5, var59));
                  }

                  if (var60 && !var41.lines().isEmpty() || var5) {
                     this.drawVanish(var55, var29, var41, var24, this.transform(var29, var5, var60));
                  }

                  if (var61 || var5) {
                     this.drawStaff(var55, var30, var42, var24, this.transform(var30, var5, var61));
                  }

                  if (var62 || var5) {
                     this.drawSpotify(var55, var31, var43, var24, this.transform(var31, var5, var62));
                  }

                  if (var64 || var5) {
                     this.drawRadar(var55, var32, var44, var6, var24, this.transform(var32, var5, var64));
                  }

                  if (var2 && !var45.lines().isEmpty() || var5) {
                     this.drawHotkeys(var55, var33, var45, var24, this.transform(var33, var5, var2));
                  }

                  if (this.targetCard.visible()) {
                     HudRenderer.DrawTransform var1 = this.transform(var34, var5, var47);
                     this.targetCard
                        .draw(var55, var34, Math.round(var34.x + var1.xOffset()), Math.round(var34.y + var1.yOffset()), var1.alpha(), var24, this.hudModule);
                  }

                  if (this.armorStatus.visible()) {
                     HudRenderer.DrawTransform var66 = this.transform(var35, var5, var48);
                     this.armorStatus
                        .draw(var55, var35, Math.round(var35.x + var66.xOffset()), Math.round(var35.y + var66.yOffset()), var66.alpha(), var24, var6);
                  }

                  if (this.cooldownTray.visible()) {
                     HudRenderer.DrawTransform var67 = this.transform(var36, var5, var49);
                     this.cooldownTray
                        .draw(
                           var55, var36, Math.round(var36.x + var67.xOffset()), Math.round(var36.y + var67.yOffset()), var67.alpha(), var24, this.hudAccent()
                        );
                  }

                  if (var19 || var5) {
                     RegionMapRenderer.render(var55, this.regionMapModule, var18, var5 && !var19 ? 0.35F : 1.0F);
                  }

                  if (var7) {
                     this.drawEditorChrome(var55, var14, var15);
                  } else if (var8) {
                     this.drawSnapGuides(var55, var14, var15);
                  }

                  ImGui.end();
               }
            }
         }
      }
   }

   private HudRenderer.DrawTransform transform(HudLayoutStore.HudElementState var1, boolean var2, boolean var3) {
      HudRenderer.DrawTransform var4 = this.introTransform(var1, var2);
      return var2 && !var3 ? new HudRenderer.DrawTransform(var4.xOffset(), var4.yOffset(), var4.alpha() * 0.35F) : var4;
   }

   private void resetCombatElements() {
      this.targetCard.reset();
      this.armorStatus.reset();
      this.cooldownTray.reset();
   }

   private void saveLayout() {
      this.layout.save();
      this.layoutDirty = false;
   }

   private void flushLayoutIfIdle() {
      if (this.layoutDirty && System.nanoTime() - this.lastWheelNanos > 400000000L) {
         this.saveLayout();
      }
   }

   private void drawOverlayOnly(float var1, float var2, boolean var3, boolean var4) {
      if (var4) {
         this.updateRegionMapBounds(var1, var2);
      }

      ImGui.setNextWindowPos(0.0F, 0.0F, 1);
      ImGui.setNextWindowSize(var1, var2);
      int var5 = 205215;
      if (!var3) {
         var5 |= 197120;
      }

      ImGui.pushStyleVar(4, 0.0F);
      if (!ImGui.begin("##efn_overlay_only", var5)) {
         ImGui.end();
         ImGui.popStyleVar();
      } else {
         ImGui.popStyleVar();
         if (var3) {
            this.updateEditorInput(var1, var2);
         } else {
            if (this.draggingId != null || this.layoutDirty) {
               this.saveLayout();
            }

            this.draggingId = null;
         }

         ImDrawList var6 = ImGui.getWindowDrawList();
         drawHudOverlays(var6, var1, var2);
         if (var4 && this.regionMapModule != null) {
            RegionMapRenderer.render(var6, this.regionMapModule, this.layout.element("region_map"));
         }

         ImGui.end();
      }
   }

   private boolean isRegionMapEnabled() {
      return this.regionMapModule != null && this.regionMapModule.isEnabled();
   }

   private void updateRegionMapBounds(float var1, float var2) {
      if (this.regionMapModule != null) {
         HudLayoutStore.HudElementState var3 = this.layout.element("region_map");
         RegionMapRenderer.Size var4 = RegionMapRenderer.measureSize(this.regionMapModule, scaleOf(var3));
         this.layout.updateBounds("region_map", var4.width(), var4.height(), var1, var2);
      }
   }

   public boolean hasWork() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (var1.field_1687 == null) {
         this.resetCombatElements();
         return false;
      } else {
         return var1.field_1755 instanceof HudEditorScreen
            || studioPreview()
            || this.hudModule != null && (this.hudModule.isEnabled() || this.hudVisibleLastFrame)
            || this.isRegionMapEnabled()
            || computeHudOverlays();
      }
   }

   private static boolean shouldRenderHudOverlays(MinecraftClient var0) {
      return var0.field_1755 == null || var0.field_1755 instanceof ChatScreen;
   }

   private static boolean gameHidesHud(MinecraftClient var0) {
      return var0.field_1690.field_1842 || var0.field_61504.method_72776();
   }

   private static boolean computeHudOverlays() {
      for (HudOverlayProvider var1 : ModuleRegistry.instancesOf(HudOverlayProvider.class)) {
         if (var1.isHudOverlayEnabled()) {
            return true;
         }
      }

      return false;
   }

   private static void drawHudOverlays(ImDrawList var0, float var1, float var2) {
      for (HudOverlayProvider var4 : ModuleRegistry.instancesOf(HudOverlayProvider.class)) {
         if (var4.isHudOverlayEnabled()) {
            var4.renderHudOverlay(var0, var1, var2);
         }
      }
   }

   private void updateIntro(boolean var1, float var2) {
      float var3 = 0.56F;
      if (var1) {
         this.introTime = var3;
         this.hudVisibleLastFrame = true;
      } else if (!this.hudVisibleLastFrame) {
         this.introTime = 0.0F;
         this.hudVisibleLastFrame = true;
      } else {
         this.introTime = Math.min(this.introTime + var2, var3);
      }
   }

   private HudRenderer.DrawTransform introTransform(HudLayoutStore.HudElementState var1, boolean var2) {
      if (var2) {
         return HudRenderer.DrawTransform.identity();
      } else {
         float var3 = clamp01((this.introTime - introDelay(var1.id)) / 0.36F);
         float var4 = easeOutCubic(var3);
         float var5 = 6.0F * scaleOf(var1) * (1.0F - var4);

         return switch (var1.anchorX) {
            case 1 -> new HudRenderer.DrawTransform(0.0F, -var5, var4);
            case 2 -> new HudRenderer.DrawTransform(var5, 0.0F, var4);
            default -> new HudRenderer.DrawTransform(-var5, 0.0F, var4);
         };
      }
   }

   private static float introDelay(String var0) {
      return switch (var0) {
         case "info_bar" -> 0.04F;
         case "coordinates" -> 0.08F;
         case "anti_vanish" -> 0.12F;
         case "spotify" -> 0.12F;
         case "module_list" -> 0.16F;
         case "staff_list" -> 0.16F;
         case "hotkeys" -> 0.16F;
         case "radar" -> 0.19999999F;
         case "potion_tracker" -> 0.19999999F;
         case "armor_status" -> 0.16F;
         case "item_cooldowns" -> 0.12F;
         case "target_card" -> 0.08F;
         default -> 0.0F;
      };
   }

   private float slot(String var1, int var2, float var3, float var4) {
      Int2ObjectOpenHashMap var5 = this.slots.computeIfAbsent(var1, var0 -> new Int2ObjectOpenHashMap());
      StableWidth var6 = (StableWidth)var5.get(var2);
      if (var6 == null) {
         var6 = new StableWidth();
         var5.put(var2, var6);
         this.slotCount++;
      }

      return var6.update(var3, var4);
   }

   private void removeSlot(String var1, int var2) {
      Int2ObjectOpenHashMap var3 = this.slots.get(var1);
      if (var3 != null && var3.remove(var2) != null) {
         this.slotCount--;
      }
   }

   private void clearSlots() {
      this.slots.clear();
      this.slotCount = 0;
   }

   private static String number(long var0) {
      return var0 < 0L ? "\u2212" + -var0 : Long.toString(var0);
   }

   private List<HudRenderer.InfoItem> buildInfoItems(boolean var1, boolean var2, boolean var3, MinecraftClient var4) {
      long var5 = var2 ? System.currentTimeMillis() / 1000L : -1L;
      int var7 = var1 ? Math.max(0, var4.method_47599()) : -1;
      int var8 = -1;
      if (var3) {
         ClientPlayNetworkHandler var9 = var4.method_1562();
         int var10 = 0;
         if (var9 != null && var4.field_1724 != null && (var10 = readTabLatency(var4, var9)) <= 0) {
            var10 = readServerPing(var9);
         }

         var8 = Math.max(0, var10);
      }

      if (var5 == this.infoSecond && var7 == this.infoFps && var8 == this.infoPing) {
         return this.infoItems;
      } else {
         this.infoSecond = var5;
         this.infoFps = var7;
         this.infoPing = var8;
         ArrayList var11 = new ArrayList(3);
         if (var2) {
            LocalTime var12 = LocalTime.now();
            var11.add(new HudRenderer.InfoItem("info.time", var12.format(CLOCK_FORMAT), SECONDS[var12.getSecond()], null));
         }

         if (var1) {
            var11.add(new HudRenderer.InfoItem("info.fps", String.valueOf(var7), null, "fps"));
         }

         if (var3) {
            var11.add(new HudRenderer.InfoItem("info.ping", String.valueOf(var8), null, "ms"));
         }

         this.infoItems = List.copyOf(var11);
         return this.infoItems;
      }
   }

   private HudRenderer.InfoPlan planInfoBar(HudLayoutStore.HudElementState var1, List<HudRenderer.InfoItem> var2, float var3) {
      float var5 = scaleOf(var1);
      HudRenderer.Type var6 = HudRenderer.Type.of(var5);
      ArrayList var7 = new ArrayList(var2.size());
      float var8 = 12.0F * var5 * 2.0F;

      for (int var9 = 0; var9 < var2.size(); var9++) {
         HudRenderer.InfoItem var10 = (HudRenderer.InfoItem)var2.get(var9);
         float var11 = HudChrome.tabularWidth(var6.value(), var6.valuePx(), var10.value());
         float var12 = var10.dim() != null ? HudChrome.tabularWidth(var6.value(), var6.valuePx(), var10.dim()) : 0.0F;
         float var13 = var11 + var12;
         if (var10.unit() != null) {
            var13 += 4.0F * var5 + HudChrome.width(var6.label(), var6.labelPx(), var10.unit());
         }

         float var14 = this.slot(var10.slot(), var6.valuePx(), (float)Math.ceil(var13), var3);
         var7.add(new HudRenderer.Segment(var10, var11, var12, var13, var14));
         var8 += var14;
      }

      if (var7.size() > 1) {
         var8 += (var7.size() - 1) * (10.0F * var5 * 2.0F + 1.0F);
      }

      float var15 = 0.0F;
      ThemeImages.Texture var4 = this.hudModule != null && this.hudModule.logo() && !var7.isEmpty() ? HudEmblem.forHeight(Math.round(24.0F * var5)) : null;
      if (var4 != null) {
         var15 = var4.width();
         var8 += var15 + 10.0F * var5 * 2.0F + 1.0F;
      }

      return new HudRenderer.InfoPlan(var7, var6, (float)Math.round(var8), (float)Math.round(28.0F * var5), var15);
   }

   private void drawInfoBar(ImDrawList var1, HudLayoutStore.HudElementState var2, HudRenderer.InfoPlan var3, boolean var4, HudRenderer.DrawTransform var5) {
      float var7 = var5.alpha();
      if (!(var7 <= 0.001F)) {
         float var8 = scaleOf(var2);
         float var9 = Math.round(var2.x + var5.xOffset());
         float var10 = Math.round(var2.y + var5.yOffset());
         float var11 = var2.width;
         float var12 = var2.height;
         Surface var13 = hudSurface(var2);
         boolean var14 = HudChrome.needsTextGuard(var13);
         HudChrome.card(var1, var13, var9, var10, var11, var12, hudRounding() * var8, var7, var4, var8);
         HudRenderer.Type var15 = var3.type();
         float var16 = var10 + (float)Math.floor((var12 - var15.valuePx()) * 0.5F);
         float var17 = var16 + HudChrome.baselineOffset(var15.value(), var15.valuePx(), var15.label(), var15.labelPx());
         float var18 = var9 + 12.0F * var8;
         ThemeImages.Texture var6 = var3.emblemW() > 0.0F ? HudEmblem.forHeight(Math.round(24.0F * var8)) : null;
         if (var6 != null) {
            float var20 = Math.round(var18);
            float var21 = Math.round(var10 + (var12 - var6.height()) * 0.5F);
            var1.addImage(
               var6.id(), var20, var21, var20 + var6.width(), var21 + var6.height(), 0.0F, 0.0F, 1.0F, 1.0F, HudChrome.scaleAlpha(this.hudAccent(), var7)
            );
            float var25;
            HudChrome.divider(var1, (var25 = var18 + var3.emblemW()) + 10.0F * var8, var10 + var12 * 0.5F, 12.0F * var8, var7);
            var18 = var25 + (10.0F * var8 * 2.0F + 1.0F);
         }

         List var26 = var3.segments();

         for (int var27 = 0; var27 < var26.size(); var27++) {
            HudRenderer.Segment var22 = (HudRenderer.Segment)var26.get(var27);
            HudRenderer.InfoItem var23 = var22.item();
            float var24 = var23.unit() != null ? var18 + var22.slotW() - var22.contentW() : var18;
            HudChrome.tabular(var1, var15.value(), var15.valuePx(), var24, var16, HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, var7), var23.value(), var14);
            var24 += var22.valueW();
            if (var23.dim() != null) {
               HudChrome.tabular(var1, var15.value(), var15.valuePx(), var24, var16, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var7), var23.dim(), var14);
               var24 += var22.dimW();
            }

            if (var23.unit() != null) {
               HudChrome.text(
                  var1, var15.label(), var15.labelPx(), var24 + 4.0F * var8, var17, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var7), var23.unit(), var14
               );
            }

            var18 += var22.slotW();
            if (var27 < var26.size() - 1) {
               HudChrome.divider(var1, var18 + 10.0F * var8, var10 + var12 * 0.5F, 12.0F * var8, var7);
               var18 += 10.0F * var8 * 2.0F + 1.0F;
            }
         }
      }
   }

   private static float rowsRadius(float var0) {
      return Math.min(hudRounding(), 6.0F) * var0;
   }

   private void drawRows(
      ImDrawList var1, List<HudRenderer.Step> var2, Surface var3, float var4, float var5, float var6, float var7, float var8, float var9, boolean var10
   ) {
      if (var10) {
         drawRowsShadow(var1, var2, var8, var9);
      }

      int var11 = GuiPalette.fill(var3);

      for (int var12 = 0; var12 < var2.size(); var12++) {
         HudRenderer.Step var13 = (HudRenderer.Step)var2.get(var12);
         HudRenderer.Step var14 = var12 > 0 ? (HudRenderer.Step)var2.get(var12 - 1) : null;
         HudRenderer.Step var15 = var12 < var2.size() - 1 ? (HudRenderer.Step)var2.get(var12 + 1) : null;
         int var16 = convexCorners(var13, var14, var15);
         int var17 = HudChrome.scaleAlpha(var11, var13.alpha());
         if (var16 == 256) {
            var1.addRectFilled(var13.l(), var13.t(), var13.r(), var13.b(), var17);
         } else {
            var1.addRectFilled(var13.l(), var13.t(), var13.r(), var13.b(), var17, var8, var16);
         }

         SurfaceBackground.drawPart(
            var1, var3, var4, var5, var6, var7, var13.l(), var13.t(), var13.r() - var13.l(), var13.b() - var13.t(), var13.alpha(), var8, var16
         );
         float var18 = var13.b() - var13.t();
         if (var14 != null && var14.l() < var13.l() - 0.5F) {
            fillet(var1, var13.l(), var13.t(), -1, 1, Math.min(var8, Math.min(var13.l() - var14.l(), var18)), var17);
         }

         if (var14 != null && var14.r() > var13.r() + 0.5F) {
            fillet(var1, var13.r(), var13.t(), 1, 1, Math.min(var8, Math.min(var14.r() - var13.r(), var18)), var17);
         }

         if (var15 != null && var15.l() < var13.l() - 0.5F) {
            fillet(var1, var13.l(), var13.b(), -1, -1, Math.min(var8, Math.min(var13.l() - var15.l(), var18)), var17);
         }

         if (var15 != null && var15.r() > var13.r() + 0.5F) {
            fillet(var1, var13.r(), var13.b(), 1, -1, Math.min(var8, Math.min(var15.r() - var13.r(), var18)), var17);
         }
      }
   }

   private static int convexCorners(HudRenderer.Step var0, HudRenderer.Step var1, HudRenderer.Step var2) {
      short var3 = 0;
      if (var1 == null || var1.l() > var0.l() + 0.5F) {
         var3 |= 16;
      }

      if (var1 == null || var1.r() < var0.r() - 0.5F) {
         var3 |= 32;
      }

      if (var2 == null || var2.l() > var0.l() + 0.5F) {
         var3 |= 64;
      }

      if (var2 == null || var2.r() < var0.r() - 0.5F) {
         var3 |= 128;
      }

      return var3 == 0 ? 256 : var3;
   }

   private static void fillet(ImDrawList var0, float var1, float var2, int var3, int var4, float var5, int var6) {
      if (!(var5 < 1.0F)) {
         float var8 = var1 + var3 * var5;
         float var9 = var2 + var4 * var5;
         float var7 = var3 < 0 ? 0.0F : 3.1415927F;
         float var11 = var4 > 0 ? (var3 < 0 ? -1.5707964F : 4.712389F) : 1.5707964F;
         var0.pathClear();
         var0.pathLineTo(var1, var2);
         var0.pathArcTo(var8, var9, var5, var7, var11, 8);
         var0.pathFillConcave(var6);
      }
   }

   private static void drawRowsShadow(ImDrawList var0, List<HudRenderer.Step> var1, float var2, float var3) {
      for (int var4 = 3; var4 >= 1; var4--) {
         float var5 = var4 * Math.max(1.0F, var3);

         for (int var6 = 0; var6 < var1.size(); var6++) {
            HudRenderer.Step var7 = (HudRenderer.Step)var1.get(var6);
            boolean var8 = var6 == 0;
            boolean var9 = var6 == var1.size() - 1;
            int var10 = convexCorners(var7, var8 ? null : (HudRenderer.Step)var1.get(var6 - 1), var9 ? null : (HudRenderer.Step)var1.get(var6 + 1));
            float var11 = var7.t() - (var8 ? var5 : 0.0F);
            float var12 = var7.b() + (var9 ? var5 + Math.max(1.0F, var3) : 0.0F);
            int var13 = HudChrome.scaleAlpha(218103808, var7.alpha());
            if (var10 == 256) {
               var0.addRectFilled(var7.l() - var5, var11, var7.r() + var5, var12, var13);
            } else {
               var0.addRectFilled(var7.l() - var5, var11, var7.r() + var5, var12, var13, var2 + var5, var10);
            }
         }
      }
   }

   private static boolean alignsRight(HudLayoutStore.HudElementState var0, float var1) {
      return switch (var0.anchorX) {
         case 0 -> false;
         case 2 -> true;
         default -> var0.x + var0.width * 0.5F >= var1 * 0.5F;
      };
   }

   private List<HudRenderer.CoordRow> coordinateRows(MinecraftClient var1, boolean var2, float var3) {
      long var4 = 0L;
      long var6 = 0L;
      long var8 = 0L;
      if (var1.field_1724 != null) {
         var4 = (long)Math.floor(var1.field_1724.method_23317());
         var6 = (long)Math.floor(var1.field_1724.method_23318());
         var8 = (long)Math.floor(var1.field_1724.method_23321());
      }

      boolean var10 = var1.field_1687 != null && var1.field_1687.method_27983() == World.field_25180;
      boolean var11 = var1.field_1687 != null && var1.field_1687.method_27983() == World.field_25181;
      boolean var12 = this.hudModule.coordinates();
      boolean var13 = this.hudModule.netherCoordinates();
      float var14 = 1.0F - (float)Math.exp(-12.0F * var3);
      ArrayList var15 = new ArrayList(2);
      if (var11) {
         this.addCoordRow(var15, "end", "end_stone", true, var12 || var13, var2, new long[]{var4, var6, var8}, var14);
      } else {
         long[] var17;
         if (var10) {
            long[] var18 = new long[]{var4 * 8L, var6, 0L};
            var17 = var18;
            var18[2] = var8 * 8L;
         } else {
            long[] var20 = new long[]{var4, var6, 0L};
            var17 = var20;
            var20[2] = var8;
         }

         long[] var16;
         if (var10) {
            long[] var19 = new long[]{var4, var6, 0L};
            var16 = var19;
            var19[2] = var8;
         } else {
            long[] var21 = new long[]{Math.floorDiv(var4, 8), var6, 0L};
            var16 = var21;
            var21[2] = Math.floorDiv(var8, 8);
         }

         this.addCoordRow(var15, "overworld", "grass_block_side", !var10, var12, var2, var17, var14);
         this.addCoordRow(var15, "nether", "netherrack", var10, var13, var2, var16, var14);
      }

      return var15;
   }

   private void addCoordRow(List<HudRenderer.CoordRow> var1, String var2, String var3, boolean var4, boolean var5, boolean var6, long[] var7, float var8) {
      float var9 = !var5 && !var6 ? 0.0F : 1.0F;
      float var10 = this.coordAnim.getOrDefault(var2, var9);
      if (Math.abs((var10 = var10 + (var9 - var10) * var8) - var9) < 0.005F) {
         var10 = var9;
      }

      this.coordAnim.put(var2, var10);
      if (!(var10 <= 0.005F)) {
         var1.add(new HudRenderer.CoordRow(var3, var4, var5, new String[]{number(var7[0]), number(var7[1]), number(var7[2])}, var10));
      }
   }

   private static float coordIconSize(float var0) {
      return var0 >= 1.0F ? 16.0F * Math.max(1.0F, (float)Math.floor(var0 + 0.25F)) : Math.round(16.0F * var0);
   }

   private HudRenderer.CoordPlan planCoordinates(HudLayoutStore.HudElementState var1, List<HudRenderer.CoordRow> var2, boolean var3, float var4) {
      float var6 = scaleOf(var1);
      HudRenderer.Type var7 = HudRenderer.Type.of(var6);
      float var8 = 20.0F * var6;
      float[] var9 = new float[3];
      float var10 = 0.0F;

      for (HudRenderer.CoordRow var12 : var2) {
         for (int var5 = 0; var5 < 3; var5++) {
            var9[var5] = Math.max(var9[var5], HudChrome.tabularWidth(var7.text(), var7.textPx(), var12.values()[var5]));
         }

         var10 += var8 * smooth(var12.anim());
      }

      float[] var14 = new float[3];
      float var15 = 7.0F * var6 * 2.0F + Math.min(coordIconSize(var6), var8 - 2.0F) + 8.0F * var6 + 12.0F * var6 * 2.0F;

      for (int var13 = 0; var13 < 3; var13++) {
         var14[var13] = this.slot(COORD_SLOTS[var13], var7.textPx(), (float)Math.ceil(var9[var13]), var4);
         var15 += var14[var13];
      }

      if (var3) {
         var10 = Math.max(var10, var8);
      }

      return new HudRenderer.CoordPlan(var2, var7, var14, (float)Math.round(var15), (float)Math.max(1, Math.round(var10)));
   }

   private void drawCoordinates(
      ImDrawList var1, HudLayoutStore.HudElementState var2, HudRenderer.CoordPlan var3, boolean var4, HudRenderer.DrawTransform var5, float var6
   ) {
      float var7 = var5.alpha();
      if (!(var7 <= 0.001F) && !var3.rows().isEmpty()) {
         float var8 = scaleOf(var2);
         float var9 = Math.round(var2.x + var5.xOffset());
         float var10 = Math.round(var2.y + var5.yOffset());
         float var11 = var2.width;
         float var12 = var2.height;
         HudRenderer.Type var13 = var3.type();
         boolean var14 = alignsRight(var2, var6);
         float var15 = 20.0F * var8;
         ArrayList var16 = new ArrayList(var3.rows().size());
         float var17 = var10;

         for (int var18 = 0; var18 < var3.rows().size(); var18++) {
            HudRenderer.CoordRow var19 = var3.rows().get(var18);
            float var20 = smooth(var19.anim());
            float var21 = var15 * var20;
            float var22 = Math.round(var17);
            float var23 = Math.round(var17 + var21);
            var17 += var21;
            if (!(var23 <= var22)) {
               float var24 = Math.round(var11 * (1.0F - var20));
               float var25 = var14 ? var9 + var24 : var9 - var24;
               float var26 = var7 * (var19.enabled() ? 1.0F : 0.35F) * Math.min(1.0F, var19.anim() * 1.5F);
               var16.add(new HudRenderer.Step(var18, var25, var22, var25 + var11, var23, var26));
            }
         }

         this.drawRows(var1, var16, hudSurface(var2), var9, var10, var11, var12, rowsRadius(var8), var8, var4);
         float var36 = Math.min(coordIconSize(var8), var15 - 2.0F);
         float var37 = 2.5F * var8;
         float var38 = (float)Math.floor((var15 - var13.textPx()) * 0.5F);

         for (HudRenderer.Step var40 : var16) {
            HudRenderer.CoordRow var42 = var3.rows().get(var40.item());
            float var43 = var40.b() - var40.t();
            float var44 = (var15 - var43) * 0.5F;
            float var27 = var40.alpha() * var42.anim();
            boolean var41 = var43 < var15 - 0.5F;
            if (var41) {
               var1.pushClipRect(var40.l(), var40.t(), var40.r(), var40.b(), true);
            }

            float var29 = Math.round(var40.l() + 7.0F * var8);
            float var30 = Math.round(var40.t() + (var15 - var36) * 0.5F - var44);
            ThemeImages.Texture var31 = GameIcons.block(var42.block());
            if (var31 != null) {
               var1.addImageRounded(var31.id(), var29, var30, var29 + var36, var30 + var36, 0.0F, 0.0F, 1.0F, 1.0F, HudChrome.scaleAlpha(-1, var27), var37, 240);
            }

            float var32 = var29 + var36 + 8.0F * var8;
            float var33 = Math.round(var40.t() + var38 - var44);
            int var34 = HudChrome.scaleAlpha(-1, var27 * (var42.here() ? 1.0F : 0.62F));

            for (int var35 = 0; var35 < 3; var35++) {
               HudChrome.tabular(var1, var13.text(), var13.textPx(), var32, var33, var34, var42.values()[var35], true);
               var32 += var3.slotW()[var35] + 12.0F * var8;
            }

            if (var41) {
               var1.popClipRect();
            }
         }
      }
   }

   private static float rowWidth(float var0, float var1) {
      return Math.round(12.0F * var1 + var0);
   }

   private HudRenderer.ListPlan planModuleList(HudLayoutStore.HudElementState var1, boolean var2, float var3) {
      float var5 = scaleOf(var1);
      HudRenderer.Type var6 = HudRenderer.Type.of(var5);
      float var7 = 18.0F * var5;
      List var8 = this.moduleRows.ordered(var6.text(), var6.textPx(), HudChrome::width, var1.anchorBottom);
      float var9 = 0.0F;
      float var10 = 0.0F;

      for (int var11 = 0; var11 < var8.size(); var11++) {
         ModuleListRows.Row var12 = (ModuleListRows.Row)var8.get(var11);
         var9 = Math.max(var9, var12.nameW());
         var10 += var7 * smooth(var12.anim());
      }

      if (var8.isEmpty()) {
         this.moduleListW = var2 ? rowWidth(HudChrome.width(var6.label(), var6.labelPx(), "No active modules"), var5) : 0.0F;
         return new HudRenderer.ListPlan(var8, var6, (float)Math.max(1, Math.round(this.moduleListW)), (float)Math.round(var7));
      } else {
         if (var2) {
            var10 = Math.max(var10, var7);
         }

         float var4;
         this.moduleListW = !((var4 = rowWidth(var9, var5)) >= this.moduleListW) && !(Math.abs(var4 - this.moduleListW) < 0.5F)
            ? (this.moduleListW = this.moduleListW + (var4 - this.moduleListW) * (1.0F - (float)Math.exp(-16.0F * var3)))
            : var4;
         return new HudRenderer.ListPlan(var8, var6, (float)Math.round(this.moduleListW), (float)Math.max(1, Math.round(var10)));
      }
   }

   private void drawModuleList(
      ImDrawList var1, HudLayoutStore.HudElementState var2, HudRenderer.ListPlan var3, boolean var4, HudRenderer.DrawTransform var5, float var6
   ) {
      float var7 = var5.alpha();
      if (!(var7 <= 0.001F)) {
         float var8 = scaleOf(var2);
         float var9 = Math.round(var2.x + var5.xOffset());
         float var10 = Math.round(var2.y + var5.yOffset());
         float var11 = var2.width;
         float var12 = var2.height;
         HudRenderer.Type var13 = var3.type();
         boolean var14 = alignsRight(var2, var6);
         float var15 = 18.0F * var8;
         ArrayList var16 = new ArrayList(Math.max(1, var3.rows().size()));
         if (var3.rows().isEmpty()) {
            float var17 = rowWidth(HudChrome.width(var13.label(), var13.labelPx(), "No active modules"), var8);
            float var18 = var14 ? var9 + var11 - var17 : var9;
            var16.add(new HudRenderer.Step(-1, var18, var10, var18 + var17, (float)Math.round(var10 + var15), var7));
         } else {
            float var31 = var10;

            for (int var33 = 0; var33 < var3.rows().size(); var33++) {
               ModuleListRows.Row var19 = var3.rows().get(var33);
               float var20 = smooth(var19.anim());
               float var21 = var15 * var20;
               float var22 = Math.round(var31);
               float var23 = Math.round(var31 + var21);
               var31 += var21;
               if (!(var23 <= var22)) {
                  float var24 = rowWidth(var19.nameW(), var8);
                  float var25 = Math.round(var24 * (1.0F - var20));
                  float var26 = var14 ? var9 + var11 - var24 + var25 : var9 - var25;
                  var16.add(new HudRenderer.Step(var33, var26, var22, var26 + var24, var23, var7 * Math.min(1.0F, var19.anim() * 1.5F)));
               }
            }
         }

         this.drawRows(var1, var16, hudSurface(var2), var9, var10, var11, var12, rowsRadius(var8), var8, var4);
         float var32 = (float)Math.floor((var15 - var13.textPx()) * 0.5F);
         float var34 = 6.0F * var8;

         for (HudRenderer.Step var36 : var16) {
            if (var36.item() < 0) {
               float var38 = HudChrome.width(var13.label(), var13.labelPx(), "No active modules");
               float var40 = var14 ? var36.r() - var34 - var38 : var36.l() + var34;
               HudChrome.text(
                  var1,
                  var13.label(),
                  var13.labelPx(),
                  var40,
                  var36.t() + (float)Math.floor((var15 - var13.labelPx()) * 0.5F),
                  HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var7),
                  "No active modules",
                  true
               );
            } else {
               ModuleListRows.Row var39 = var3.rows().get(var36.item());
               float var41 = var36.b() - var36.t();
               float var42 = var14 ? var36.r() - var34 - var39.nameW() : var36.l() + var34;
               float var43 = var36.t() + var32 - (var15 - var41) * 0.5F;
               boolean var37 = var41 < var15 - 0.5F;
               if (var37) {
                  var1.pushClipRect(var36.l(), var36.t(), var36.r(), var36.b(), true);
               }

               int var27 = var3.rows().size();
               float var28 = 1.0F - 0.6F * (var27 <= 1 ? 0.0F : (float)var36.item() / (var27 - 1));
               float var29 = Math.max(1.0F, (float)Math.round(2.0F * var8));
               float var30 = var14 ? var36.r() - var29 : var36.l();
               var1.addRectFilled(
                  var30,
                  var36.t() + 2.0F * var8,
                  var30 + var29,
                  var36.b() - 2.0F * var8,
                  HudChrome.scaleAlpha(this.hudAccent(), var7 * var39.anim() * var28),
                  var29 * 0.5F
               );
               HudChrome.text(
                  var1,
                  var13.text(),
                  var13.textPx(),
                  Math.round(var42),
                  Math.round(var43),
                  HudChrome.scaleAlpha(-1, var7 * var39.anim() * var39.anim()),
                  var39.module().name(),
                  true
               );
               if (var37) {
                  var1.popClipRect();
               }
            }
         }
      }
   }

   private float updateToastShift(MinecraftClient var1, HudLayoutStore.HudElementState var2, boolean var3, float var4) {
      float var5 = !var3 && this.draggingId == null ? toastClearance(var1, var2) : 0.0F;
      this.toastShift = !var3 && !(Math.abs(var5 - this.toastShift) < 0.5F)
         ? (this.toastShift = this.toastShift + (var5 - this.toastShift) * (1.0F - (float)Math.exp(-12.0F * var4)))
         : var5;
      return this.toastShift;
   }

   private static float toastClearance(MinecraftClient var0, HudLayoutStore.HudElementState var1) {
      if (var1.anchorX == 2 && !var1.anchorBottom && !var0.field_1690.field_1842) {
         Window var2 = var0.method_22683();
         int var3 = var2.method_4495();
         int var4 = var2.method_4486();
         float var5 = 0.0F;

         for (Object var7 : ((AccessorHudToastManager)var0.method_1566()).efn$hudVisibleToasts()) {
            AccessorHudToastInstance var8 = (AccessorHudToastInstance)var7;
            Toast var9 = var8.efn$hudToast();
            float var10 = var8.efn$hudVisibility() == Visibility.field_2209 ? var8.efn$hudVisiblePortion() : 1.0F;
            float var11 = var9.method_71809(var4, var10) * var3;
            if (!(var11 >= var1.x + var1.width) && !(var11 + var9.method_29049() * var3 <= var1.x)) {
               var5 = Math.max(var5, (var9.method_71808(var8.efn$hudFirstSlot()) + var9.method_29050()) * var3);
            }
         }

         return var5 <= 0.0F ? 0.0F : MathHelper.method_15363(var5 + 10.0F * ImGuiManager.getInstance().uiScale() - var1.y, 0.0F, var5);
      } else {
         return 0.0F;
      }
   }

   private static float iconSize(float var0) {
      return var0 >= 1.0F ? 18.0F * Math.max(1.0F, (float)Math.floor(var0 + 0.25F)) : Math.round(18.0F * var0);
   }

   private HudRenderer.PotionPlan planPotions(HudLayoutStore.HudElementState var1, List<HudRenderer.PotionRow> var2, boolean var3, float var4) {
      float var5 = scaleOf(var1);
      HudRenderer.Type var6 = HudRenderer.Type.of(var5);
      float var7 = 22.0F * var5;
      ArrayList var8 = new ArrayList(var2);
      var8.sort(Comparator.comparingInt(HudRenderer.PotionRow::ticks).thenComparing(HudRenderer.PotionRow::name));
      if (var1.anchorBottom) {
         Collections.reverse(var8);
      }

      ArrayList var9 = new ArrayList(var8.size());
      float var10 = 0.0F;
      float var11 = 6.0F * var5 * 2.0F + Math.min(iconSize(var5), var7 - 2.0F) + 6.0F * var5 + 14.0F * var5;

      for (HudRenderer.PotionRow var13 : var8) {
         float var14 = HudChrome.width(var6.text(), var6.textPx(), var13.name());
         float var15 = var13.amplifier().isEmpty() ? 0.0F : HudChrome.width(var6.text(), var6.textPx(), var13.amplifier());
         float var16 = var13.infinite()
            ? HudChrome.width(var6.text(), var6.textPx(), "\u221e")
            : HudChrome.tabularWidth(var6.text(), var6.textPx(), var13.duration());
         var9.add(new HudRenderer.PotionLine(var13, var14, var15, var16));
         var10 = Math.max(var10, var11 + var14 + (var15 > 0.0F ? 4.0F * var5 + var15 : 0.0F) + var16);
      }

      if (var9.isEmpty()) {
         this.removeSlot("potions", var6.textPx());
         float var18 = var3 ? 6.0F * var5 * 2.0F + HudChrome.width(var6.label(), var6.labelPx(), "No active effects") : 1.0F;
         return new HudRenderer.PotionPlan(var9, var6, (float)Math.round(var18), (float)Math.round(var7));
      } else {
         float var17 = this.slot("potions", var6.textPx(), (float)Math.ceil(var10), var4);
         return new HudRenderer.PotionPlan(var9, var6, (float)Math.round(var17), (float)Math.round(var7 * var9.size()));
      }
   }

   private void drawPotions(ImDrawList var1, HudLayoutStore.HudElementState var2, HudRenderer.PotionPlan var3, boolean var4, HudRenderer.DrawTransform var5) {
      float var6 = var5.alpha();
      if (!(var6 <= 0.001F)) {
         float var7 = scaleOf(var2);
         float var8 = Math.round(var2.x + var5.xOffset());
         float var9 = Math.round(var2.y + var5.yOffset());
         float var10 = var2.width;
         float var11 = var2.height;
         HudRenderer.Type var12 = var3.type();
         float var13 = 22.0F * var7;
         float var14 = 6.0F * var7;
         int var15 = Math.max(1, var3.lines().size());
         ArrayList var16 = new ArrayList(var15);

         for (int var17 = 0; var17 < var15; var17++) {
            var16.add(
               new HudRenderer.Step(
                  var3.lines().isEmpty() ? -1 : var17,
                  var8,
                  (float)Math.round(var9 + var13 * var17),
                  var8 + var10,
                  (float)Math.round(var9 + var13 * (var17 + 1)),
                  var6
               )
            );
         }

         this.drawRows(var1, var16, hudSurface(var2), var8, var9, var10, var11, rowsRadius(var7), var7, var4);
         if (var3.lines().isEmpty()) {
            HudChrome.text(
               var1,
               var12.label(),
               var12.labelPx(),
               var8 + var14,
               var9 + (float)Math.floor((var13 - var12.labelPx()) * 0.5F),
               HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var6),
               "No active effects",
               true
            );
         } else {
            float var31 = Math.min(iconSize(var7), var13 - 2.0F);
            float var18 = (float)Math.floor((var13 - var12.textPx()) * 0.5F);

            for (HudRenderer.Step var20 : var16) {
               HudRenderer.PotionLine var22 = var3.lines().get(var20.item());
               HudRenderer.PotionRow var23 = var22.row();
               float var24 = Math.round(var20.l() + var14);
               float var25 = Math.round(var20.t() + (var13 - var31) * 0.5F);
               float var26 = var6;
               if (!var23.infinite() && var23.ticks() <= 200) {
                  var26 = var6 * (0.55F + 0.45F * Math.abs((float)Math.cos(var23.ticks() * 3.141592653589793 / 10.0)));
               }

               ThemeImages.Texture var21;
               if ((var21 = GameIcons.effect(var23.effect())) != null) {
                  var1.addImage(var21.id(), var24, var25, var24 + var31, var25 + var31, 0.0F, 0.0F, 1.0F, 1.0F, HudChrome.scaleAlpha(-1, var26));
               } else {
                  int var27 = GuiPalette.abgr(0xFF000000 | ((StatusEffect)var23.effect().comp_349()).method_5556());
                  var1.addCircleFilled(var24 + var31 * 0.5F, var25 + var31 * 0.5F, var31 * 0.3F, HudChrome.scaleAlpha(var27, var26), 16);
               }

               float var32 = var20.t() + var18;
               float var28 = var24 + var31 + 6.0F * var7;
               HudChrome.text(var1, var12.text(), var12.textPx(), var28, var32, HudChrome.scaleAlpha(-1, var6), var23.name(), true);
               if (var22.ampW() > 0.0F) {
                  HudChrome.text(
                     var1,
                     var12.text(),
                     var12.textPx(),
                     var28 + var22.nameW() + 4.0F * var7,
                     var32,
                     HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var6),
                     var23.amplifier(),
                     true
                  );
               }

               float var29 = var20.r() - var14 - var22.durationW();
               if (var23.infinite()) {
                  HudChrome.text(var1, var12.text(), var12.textPx(), var29, var32, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var6), "\u221e", true);
               } else {
                  int var30 = var23.expiringSoon() ? HudChrome.WARN : HudChrome.TEXT_LABEL;
                  HudChrome.tabular(var1, var12.text(), var12.textPx(), var29, var32, HudChrome.scaleAlpha(var30, var6), var23.duration(), true);
               }
            }
         }
      }
   }

   private HudRenderer.VanishPlan planVanish(HudLayoutStore.HudElementState var1, boolean var2, float var3) {
      float var4 = scaleOf(var1);
      HudRenderer.Type var5 = HudRenderer.Type.of(var4);
      float var6 = 22.0F * var4;
      ArrayList var7 = new ArrayList();

      for (AntiVanishModule.HudEntry var9 : AntiVanishModule.hudEntries()) {
         String var10 = AntiVanishModule.hudTag(var9);
         String var11 = AntiVanishModule.hudValue(var9);
         boolean var12 = "ALERT".equals(var10) || "VANISH".equals(var10);
         var7.add(
            new HudRenderer.VanishLine(
               var10, var11, var12, HudChrome.width(var5.label(), var5.labelPx(), var10), HudChrome.width(var5.text(), var5.textPx(), var11)
            )
         );
      }

      if (var1.anchorBottom) {
         Collections.reverse(var7);
      }

      if (var7.isEmpty()) {
         this.removeSlot("vanish", var5.textPx());
         float var14 = var2 ? 8.0F * var4 * 2.0F + HudChrome.width(var5.label(), var5.labelPx(), "No vanish alerts") : 1.0F;
         return new HudRenderer.VanishPlan(var7, var5, 0.0F, (float)Math.round(var14), (float)Math.round(var6));
      } else {
         float var13 = 0.0F;
         float var15 = 0.0F;

         for (HudRenderer.VanishLine var18 : var7) {
            var13 = Math.max(var13, var18.tagW());
            var15 = Math.max(var15, var18.valueW());
         }

         float var17 = 8.0F * var4 * 2.0F + var13 + 8.0F * var4 + var15;
         float var19 = this.slot("vanish", var5.textPx(), (float)Math.ceil(var17), var3);
         return new HudRenderer.VanishPlan(var7, var5, var13, (float)Math.round(var19), (float)Math.round(var6 * var7.size()));
      }
   }

   private void drawVanish(ImDrawList var1, HudLayoutStore.HudElementState var2, HudRenderer.VanishPlan var3, boolean var4, HudRenderer.DrawTransform var5) {
      float var6 = var5.alpha();
      if (!(var6 <= 0.001F)) {
         float var7 = scaleOf(var2);
         float var8 = Math.round(var2.x + var5.xOffset());
         float var9 = Math.round(var2.y + var5.yOffset());
         float var10 = var2.width;
         float var11 = var2.height;
         HudRenderer.Type var12 = var3.type();
         float var13 = 22.0F * var7;
         float var14 = 8.0F * var7;
         int var15 = Math.max(1, var3.lines().size());
         ArrayList var16 = new ArrayList(var15);

         for (int var17 = 0; var17 < var15; var17++) {
            var16.add(
               new HudRenderer.Step(
                  var3.lines().isEmpty() ? -1 : var17,
                  var8,
                  (float)Math.round(var9 + var13 * var17),
                  var8 + var10,
                  (float)Math.round(var9 + var13 * (var17 + 1)),
                  var6
               )
            );
         }

         this.drawRows(var1, var16, hudSurface(var2), var8, var9, var10, var11, rowsRadius(var7), var7, var4);
         if (var3.lines().isEmpty()) {
            HudChrome.text(
               var1,
               var12.label(),
               var12.labelPx(),
               var8 + var14,
               var9 + (float)Math.floor((var13 - var12.labelPx()) * 0.5F),
               HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var6),
               "No vanish alerts",
               true
            );
         } else {
            float var24 = (float)Math.floor((var13 - var12.textPx()) * 0.5F);

            for (HudRenderer.Step var19 : var16) {
               HudRenderer.VanishLine var20 = var3.lines().get(var19.item());
               float var21 = var19.t() + var24;
               float var22 = var21 + HudChrome.baselineOffset(var12.text(), var12.textPx(), var12.label(), var12.labelPx());
               float var23 = var19.l() + var14;
               HudChrome.text(
                  var1,
                  var12.label(),
                  var12.labelPx(),
                  var23,
                  var22,
                  HudChrome.scaleAlpha(var20.urgent() ? HudChrome.WARN : HudChrome.TEXT_LABEL, var6),
                  var20.tag(),
                  true
               );
               HudChrome.text(
                  var1, var12.text(), var12.textPx(), var23 + var3.tagColumn() + 8.0F * var7, var21, HudChrome.scaleAlpha(-1, var6), var20.value(), true
               );
            }
         }
      }
   }

   private HudRenderer.StaffPlan planStaff(HudLayoutStore.HudElementState var1, boolean var2, float var3) {
      float var4 = scaleOf(var1);
      HudRenderer.Type var5 = HudRenderer.Type.of(var4);
      float var6 = 22.0F * var4;
      StaffListModule.HudList var7 = StaffListModule.hudRows();
      ArrayList var8 = new ArrayList(var7.rows().size() + 1);

      for (StaffListModule.HudRow var10 : var7.rows()) {
         int var12 = this.staffFace(var10.id(), var10.nameHidden());
         String var13 = var10.presence() == StaffListModule.Presence.VANISHED
            ? "VANISHED"
            : (var10.presence() == StaffListModule.Presence.SPECTATOR ? "SPECTATOR" : "");
         float var11 = var13.isEmpty() ? 0.0F : HudChrome.width(var5.label(), var5.labelPx(), var13);
         if (var10.watching()) {
            String var15 = var10.nearby() ? var10.distanceLabel() : var10.watchingFor();
            float var16 = HudChrome.tabularWidth(var5.text(), var5.textPx(), var15);
            float var17 = HudChrome.width(var5.label(), var5.labelPx(), "WATCHING YOU");
            float var18 = HudChrome.width(var5.text(), var5.textPx(), var10.name()) + (var13.isEmpty() ? 0.0F : 8.0F * var4 + var11);
            var8.add(
               new HudRenderer.StaffLine(
                  var10.name(),
                  var12,
                  "WATCHING YOU",
                  false,
                  var18,
                  var17 + 4.0F * var4 + var16,
                  true,
                  var10.nearby(),
                  var10.alertLevel(),
                  var13,
                  var11,
                  var15,
                  var16,
                  var17
               )
            );
         } else {
            var8.add(HudRenderer.StaffLine.plain(var10.name(), var12, var13, false, HudChrome.width(var5.text(), var5.textPx(), var10.name()), var11));
         }
      }

      if (var7.more() > 0) {
         String var19 = "+" + var7.more() + " more";
         var8.add(HudRenderer.StaffLine.plain(var19, 0, "", true, HudChrome.width(var5.label(), var5.labelPx(), var19), 0.0F));
      }

      if (var1.anchorBottom) {
         Collections.reverse(var8);
      }

      if (var8.isEmpty()) {
         this.removeSlot("staff", var5.textPx());
         boolean var21 = var2 || StaffListModule.shouldShowHud();
         float var23 = var21 ? 8.0F * var4 * 2.0F + HudChrome.width(var5.label(), var5.labelPx(), "No staff online") : 1.0F;
         return new HudRenderer.StaffPlan(var8, var5, "", (float)Math.round(var23), (float)Math.round(var6));
      } else {
         String var20 = Integer.toString(var7.rows().size() + var7.more());
         float var22 = HudChrome.width(var5.label(), var5.labelPx(), "STAFF") + 5.0F * var4 + HudChrome.tabularWidth(var5.text(), var5.textPx(), var20);
         float var24 = 0.0F;
         float var25 = 0.0F;

         for (HudRenderer.StaffLine var14 : var8) {
            var24 = Math.max(var24, var14.nameW());
            var25 = Math.max(var25, var14.stateW());
         }

         float var27 = faceSize(var4) + 6.0F * var4 + var24 + (var25 > 0.0F ? 14.0F * var4 + var25 : 0.0F);
         float var28 = 8.0F * var4 * 2.0F + Math.max(var22, var27);
         float var29 = this.slot("staff", var5.textPx(), (float)Math.ceil(var28), var3);
         return new HudRenderer.StaffPlan(var8, var5, var20, (float)Math.round(var29), (float)Math.round(20.0F * var4 + var6 * var8.size()));
      }
   }

   private void drawStaff(ImDrawList var1, HudLayoutStore.HudElementState var2, HudRenderer.StaffPlan var3, boolean var4, HudRenderer.DrawTransform var5) {
      float var7 = var5.alpha();
      if (!(var7 <= 0.001F)) {
         float var8 = scaleOf(var2);
         float var9 = Math.round(var2.x + var5.xOffset());
         float var10 = Math.round(var2.y + var5.yOffset());
         float var11 = var2.width;
         float var12 = var2.height;
         HudRenderer.Type var13 = var3.type();
         float var14 = 22.0F * var8;
         float var15 = 8.0F * var8;
         float var16 = (float)Math.floor((var14 - var13.labelPx()) * 0.5F);
         if (var3.lines().isEmpty()) {
            List var31 = List.of(new HudRenderer.Step(-1, var9, var10, var9 + var11, (float)Math.round(var10 + var14), var7));
            this.drawRows(var1, var31, hudSurface(var2), var9, var10, var11, var12, rowsRadius(var8), var8, var4);
            HudChrome.text(
               var1, var13.label(), var13.labelPx(), var9 + var15, var10 + var16, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var7), "No staff online", true
            );
         } else {
            float var17 = 20.0F * var8;
            ArrayList var18 = new ArrayList(var3.lines().size() + 1);
            var18.add(new HudRenderer.Step(-1, var9, var10, var9 + var11, (float)Math.round(var10 + var17), var7));

            for (int var6 = 0; var6 < var3.lines().size(); var6++) {
               var18.add(
                  new HudRenderer.Step(
                     var6, var9, (float)Math.round(var10 + var17 + var14 * var6), var9 + var11, (float)Math.round(var10 + var17 + var14 * (var6 + 1)), var7
                  )
               );
            }

            this.drawRows(var1, var18, hudSurface(var2), var9, var10, var11, var12, rowsRadius(var8), var8, var4);

            for (int var30 = 1; var30 < var18.size(); var30++) {
               HudRenderer.Step var19 = (HudRenderer.Step)var18.get(var30);
               HudRenderer.StaffLine var20 = var3.lines().get(var19.item());
               if (var20.server()) {
                  int var21 = convexCorners(
                     var19, (HudRenderer.Step)var18.get(var30 - 1), var30 < var18.size() - 1 ? (HudRenderer.Step)var18.get(var30 + 1) : null
                  );
                  float var22 = 0.14F;
                  if (var20.alert() > 0.0F) {
                     float var23 = (1.0F - var20.alert()) * 4000.0F;
                     float var24 = 0.5F + 0.5F * (float)Math.cos(var23 * 2.0 * 3.141592653589793 / 500.0);
                     var22 += 0.24F * var20.alert() * var24;
                  }

                  int var36 = HudChrome.scaleAlpha(HudChrome.WARN, var7 * var22);
                  if (var21 == 256) {
                     var1.addRectFilled(var19.l(), var19.t(), var19.r(), var19.b(), var36);
                  } else {
                     var1.addRectFilled(var19.l(), var19.t(), var19.r(), var19.b(), var36, rowsRadius(var8), var21);
                  }

                  var1.addRectFilled(
                     var19.l() + 3.0F * var8,
                     var19.t() + 4.0F * var8,
                     var19.l() + 5.0F * var8,
                     var19.b() - 4.0F * var8,
                     HudChrome.scaleAlpha(HudChrome.WARN, var7),
                     1.0F * var8
                  );
               }
            }

            HudRenderer.Step var32 = (HudRenderer.Step)var18.get(0);
            float var33 = var32.t() + (float)Math.floor((var32.b() - var32.t() - var13.textPx()) * 0.5F);
            HudChrome.text(
               var1,
               var13.label(),
               var13.labelPx(),
               var9 + var15,
               var33 + HudChrome.baselineOffset(var13.text(), var13.textPx(), var13.label(), var13.labelPx()),
               HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var7),
               "STAFF",
               true
            );
            HudChrome.tabular(
               var1,
               var13.text(),
               var13.textPx(),
               var9 + var15 + HudChrome.width(var13.label(), var13.labelPx(), "STAFF") + 5.0F * var8,
               var33,
               HudChrome.scaleAlpha(-1, var7),
               var3.count(),
               true
            );
            float var34 = faceSize(var8);
            float var35 = (float)Math.floor((var14 - var13.textPx()) * 0.5F);
            float var37 = var15 + var34 + 6.0F * var8;

            for (int var38 = 1; var38 < var18.size(); var38++) {
               HudRenderer.Step var25 = (HudRenderer.Step)var18.get(var38);
               HudRenderer.StaffLine var26 = var3.lines().get(var25.item());
               if (var26.more()) {
                  HudChrome.text(
                     var1,
                     var13.label(),
                     var13.labelPx(),
                     var25.l() + var37,
                     var25.t() + var16,
                     HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var7),
                     var26.name(),
                     true
                  );
               } else {
                  if (var26.face() != 0) {
                     drawFace(
                        var1, var26.face(), Math.round(var25.l() + var15), Math.round(var25.t() + (var25.b() - var25.t() - var34) * 0.5F), var34, var8, var7
                     );
                  }

                  float var27 = var25.t() + var35;
                  float var28 = var27 + HudChrome.baselineOffset(var13.text(), var13.textPx(), var13.label(), var13.labelPx());
                  HudChrome.text(var1, var13.text(), var13.textPx(), var25.l() + var37, var27, HudChrome.scaleAlpha(-1, var7), var26.name(), true);
                  if (var26.server()) {
                     if (!var26.note().isEmpty()) {
                        HudChrome.text(
                           var1,
                           var13.label(),
                           var13.labelPx(),
                           var25.l() + var37 + var26.nameW() - var26.noteW(),
                           var28,
                           HudChrome.scaleAlpha(HudChrome.WARN, var7),
                           var26.note(),
                           true
                        );
                     }

                     float var29 = var25.r() - var15 - var26.stateW();
                     HudChrome.text(var1, var13.label(), var13.labelPx(), var29, var28, HudChrome.scaleAlpha(HudChrome.WARN, var7), var26.state(), true);
                     HudChrome.tabular(
                        var1,
                        var13.text(),
                        var13.textPx(),
                        var29 + var26.serverTagW() + 4.0F * var8,
                        var27,
                        HudChrome.scaleAlpha(var26.nearby() ? -1 : HudChrome.TEXT_LABEL, var7),
                        var26.value(),
                        true
                     );
                  } else if (!var26.state().isEmpty()) {
                     HudChrome.text(
                        var1,
                        var13.label(),
                        var13.labelPx(),
                        var25.r() - var15 - var26.stateW(),
                        var28,
                        HudChrome.scaleAlpha(HudChrome.WARN, var7),
                        var26.state(),
                        true
                     );
                  }
               }
            }
         }
      }
   }

   private static float faceSize(float var0) {
      int var1 = Math.round(Math.round(16.0F * var0) / 8.0F) * 8;
      if (var1 > 22.0F * var0 - 4.0F * var0) {
         var1 -= 8;
      }

      return Math.max(8, var1);
   }

   private static void drawFace(ImDrawList var0, int var1, float var2, float var3, float var4, float var5, float var6) {
      float var7 = Math.max(1.0F, (float)Math.round(2.0F * var5));
      int var8 = HudChrome.scaleAlpha(-1, var6);
      var0.addImageRounded(var1, var2, var3, var2 + var4, var3 + var4, 0.125F, 0.125F, 0.25F, 0.25F, var8, var7, 240);
      var0.addImageRounded(var1, var2, var3, var2 + var4, var3 + var4, 0.625F, 0.125F, 0.75F, 0.25F, var8, var7, 240);
      var0.addRect(var2, var3, var2 + var4, var3 + var4, HudChrome.scaleAlpha(402653183, var6), var7, 0, 1.0F);
   }

   int staffFace(UUID var1, boolean var2) {
      MinecraftClient var7 = MinecraftClient.method_1551();
      ClientPlayNetworkHandler var8 = var7.method_1562();
      PlayerListEntry var6 = var8 == null ? null : var8.method_2871(var1);
      Identifier var5;
      if (var6 != null) {
         var5 = var6.method_52810().comp_1626().comp_3627();
         this.staffSkins.put(var1, var5);
         if (var2) {
            var5 = NameProtectModule.hiddenHead(var1, var6.method_52810()).comp_1626().comp_3627();
         }
      } else if (var2) {
         var5 = NameProtectModule.hiddenHead(var1, DefaultSkinHelper.method_4648(var1)).comp_1626().comp_3627();
      } else {
         var5 = this.staffSkins.get(var1);
         if (var5 == null) {
            var5 = DefaultSkinHelper.method_4648(var1).comp_1626().comp_3627();
         }
      }

      GpuTexture var4;
      try {
         var4 = var7.method_1531().method_4619(var5).method_68004();
      } catch (IllegalStateException var11) {
         return 0;
      }

      GlTexture var3;
      if (var4 instanceof GlTexture && !(var3 = (GlTexture)var4).isClosed()) {
         if (this.nearestSkins.add(var4)) {
            int var10 = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, var3.method_68427());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glBindTexture(3553, var10);
         }

         return var3.method_68427();
      } else {
         return 0;
      }
   }

   private float planRadar(HudLayoutStore.HudElementState var1, boolean var2) {
      if (!var2 && !RadarModule.shouldShowHud()) {
         return 1.0F;
      } else {
         float var3 = this.radarModule != null ? this.radarModule.size() : 150.0F;
         return Math.max(2.0F, (float)Math.round(var3 * scaleOf(var1)));
      }
   }

   private void drawRadar(ImDrawList var1, HudLayoutStore.HudElementState var2, float var3, MinecraftClient var4, boolean var5, HudRenderer.DrawTransform var6) {
      float var7 = var6.alpha();
      if (!(var7 <= 0.001F) && !(var3 <= 1.0F)) {
         float var8 = scaleOf(var2);
         HudRenderer.Type var9 = HudRenderer.Type.of(var8);
         float var10 = Math.round(var2.x + var6.xOffset());
         float var11 = Math.round(var2.y + var6.yOffset());
         float var12 = var3 * 0.5F;
         float var13 = var10 + var12;
         float var14 = var11 + var12;
         int var15 = this.hudAccent();
         Surface var16 = hudSurface(var2);
         if (var5) {
            drawDiscShadow(var1, var13, var14, var12, var8, var7);
         }

         var1.addCircleFilled(var13, var14, var12, HudChrome.scaleAlpha(GuiPalette.fill(var16), var7), 0);
         SurfaceBackground.drawDisc(var1, var16, var13, var14, var12, var7);
         var1.addCircle(var13, var14, var12 - 0.5F, HudChrome.scaleAlpha(402653183, var7), 0, 1.0F);
         this.measureRadarLetters(var9.label(), var9.labelPx());
         float var17 = var12 - 10.0F * var8 - this.radarLetterHalf;
         float var18 = var17 - this.radarLetterHalf - 4.0F * var8;
         float var19 = var12 - 5.0F * var8;
         int var20 = HudChrome.scaleAlpha(536870911, var7);

         for (float var24 : RADAR_RINGS) {
            if (var19 * var24 < var18) {
               var1.addCircle(var13, var14, var19 * var24, var20, 0, 1.0F);
            }
         }

         float var52 = (float)Math.floor(var13) + 0.5F;
         float var53 = (float)Math.floor(var14) + 0.5F;
         var1.addLine(var52, var14 - var18, var52, var14 + var18, var20, 1.0F);
         var1.addLine(var13 - var18, var53, var13 + var18, var53, var20, 1.0F);
         ClientPlayerEntity var54 = var4.field_1724;
         boolean var55 = this.radarModule == null || this.radarModule.rotate();
         float var25 = var4.method_61966().method_60637(false);
         double var26 = var54 != null ? Math.toRadians(var54.method_5705(var25)) : 0.0;
         float var28 = (float)Math.sin(var26);
         float var29 = (float)Math.cos(var26);

         for (int var30 = 0; var30 < RADAR_LETTERS.length; var30++) {
            int var31 = RADAR_LETTER_DX[var30];
            int var32 = RADAR_LETTER_DZ[var30];
            float var33 = var55 ? -var31 * var29 - var32 * var28 : var31;
            float var34 = var55 ? var31 * var28 - var32 * var29 : var32;
            int var35 = var30 == 0 ? var15 : HudChrome.TEXT_LABEL;
            HudChrome.text(
               var1,
               var9.label(),
               var9.labelPx(),
               Math.round(var13 + var33 * var17 - this.radarLetterW[var30] * 0.5F),
               Math.round(var14 + var34 * var17 - this.radarLetterMid),
               HudChrome.scaleAlpha(var35, var7),
               RADAR_LETTERS[var30],
               true
            );
         }

         if (this.radarModule != null && var54 != null && var4.field_1687 != null) {
            float var56 = Math.max(1.0F, this.radarModule.range());
            double var58 = (double)var56 * var56;
            float var61 = var19 / var56;
            double var63 = MathHelper.method_16436(var25, var54.field_6014, var54.method_23317());
            double var36 = MathHelper.method_16436(var25, var54.field_5969, var54.method_23321());
            float var38 = 3.2F * var8;
            float var39 = Math.max(1.0F, 0.75F * var8);
            int var40 = HudChrome.scaleAlpha(-1946157056, var7);
            int var41 = HudChrome.scaleAlpha(GuiPalette.abgr(this.radarModule.dotColor()), var7);

            for (AbstractClientPlayerEntity var43 : var4.field_1687.method_18456()) {
               double var44;
               double var46;
               if (var43 != var54
                  && this.radarModule.shows(var43)
                  && !(
                     (var46 = MathHelper.method_16436(var25, var43.field_6014, var43.method_23317()) - var63) * var46
                           + (var44 = MathHelper.method_16436(var25, var43.field_5969, var43.method_23321()) - var36) * var44
                        > var58
                  )) {
                  float var48 = var55 ? (float)(-var46 * var29 - var44 * var28) : (float)var46;
                  float var49 = var55 ? (float)(var46 * var28 - var44 * var29) : (float)var44;
                  float var50 = var13 + var48 * var61;
                  float var51 = var14 + var49 * var61;
                  var1.addCircleFilled(var50, var51, var38 + var39, var40, 12);
                  var1.addCircleFilled(var50, var51, var38, var41, 12);
               }
            }
         }

         var1.addCircleFilled(var13, var14, 6.0F * var8, HudChrome.scaleAlpha(var15, var7 * 0.25F), 0);
         var1.addCircleFilled(var13, var14, 3.0F * var8, HudChrome.scaleAlpha(var15, var7), 0);
         float var57 = var55 ? 0.0F : -var28;
         float var59 = var55 ? -1.0F : var29;
         float var60 = var12 - 1.0F - 1.5F * var8;
         float var62 = var60 - 5.0F * var8;
         float var64 = 3.5F * var8;
         var1.addTriangleFilled(
            var13 + var57 * var60,
            var14 + var59 * var60,
            var13 + var57 * var62 - var59 * var64,
            var14 + var59 * var62 + var57 * var64,
            var13 + var57 * var62 + var59 * var64,
            var14 + var59 * var62 - var57 * var64,
            HudChrome.scaleAlpha(var15, var7)
         );
      }
   }

   private static void drawDiscShadow(ImDrawList var0, float var1, float var2, float var3, float var4, float var5) {
      float var6 = Math.max(1.0F, var4);
      int var7 = HudChrome.scaleAlpha(218103808, var5);

      for (int var8 = 3; var8 >= 1; var8--) {
         var0.addCircleFilled(var1, var2 + var6 * 0.5F, var3 + (var8 + 0.5F) * var6, var7, 0);
      }
   }

   private void measureRadarLetters(ImFont var1, int var2) {
      if (var1 != this.radarLetterFont || var2 != this.radarLetterPx) {
         this.radarLetterFont = var1;
         this.radarLetterPx = var2;

         for (int var3 = 0; var3 < RADAR_LETTERS.length; var3++) {
            this.radarLetterW[var3] = HudChrome.width(var1, var2, RADAR_LETTERS[var3]);
         }

         float var5 = this.capTop(var1, var2);
         float var4 = HudChrome.ascent(var1, var2);
         this.radarLetterMid = (var5 + var4) * 0.5F;
         this.radarLetterHalf = (var4 - var5) * 0.5F;
      }
   }

   private HudRenderer.HotkeyPlan planHotkeys(HudLayoutStore.HudElementState var1, boolean var2, float var3) {
      float var5 = scaleOf(var1);
      HudRenderer.Type var6 = HudRenderer.Type.of(var5);
      float var7 = 22.0F * var5;
      List var4 = !var2 && !HotkeysModule.shouldShowHud() ? List.of() : this.hotkeyLinesFor(var6);
      if (var4.isEmpty()) {
         this.removeSlot("hotkeys", var6.textPx());
         float var11 = var2 ? 8.0F * var5 * 2.0F + HudChrome.width(var6.label(), var6.labelPx(), "No keys bound") : 1.0F;
         return new HudRenderer.HotkeyPlan(var4, var6, (float)Math.round(var11), (float)Math.round(var7));
      } else {
         float var8 = this.hotkeyNameColumn + 16.0F * var5 + keycapWidth(this.hotkeyKeyColumn, var5);
         float var9 = 8.0F * var5 * 2.0F + Math.max(this.hotkeyHeaderW, var8);
         float var10 = this.slot("hotkeys", var6.textPx(), (float)Math.ceil(var9), var3);
         return new HudRenderer.HotkeyPlan(var4, var6, (float)Math.round(var10), (float)Math.round(20.0F * var5 + var7 * var4.size()));
      }
   }

   private List<HudRenderer.HotkeyLine> hotkeyLinesFor(HudRenderer.Type var1) {
      long var2 = ModuleRevision.current();
      if (var2 == this.hotkeyRevision && var1.text() == this.hotkeyFont && var1.textPx() == this.hotkeyTextPx && var1.labelPx() == this.hotkeyKeyPx) {
         return this.hotkeyLines;
      } else {
         this.hotkeyRevision = var2;
         this.hotkeyFont = var1.text();
         this.hotkeyTextPx = var1.textPx();
         this.hotkeyKeyPx = var1.labelPx();
         ArrayList var4 = new ArrayList();
         float var5 = 0.0F;
         float var6 = 0.0F;

         for (ClientModule var8 : ModuleRegistry.all()) {
            int var9 = var8.keybind();
            if (var9 != -1) {
               String var10 = ClickGuiRenderer.keyName(var9).toUpperCase(Locale.ROOT);
               float var11 = HudChrome.width(var1.text(), var1.textPx(), var8.name());
               float var12 = HudChrome.width(var1.text(), var1.labelPx(), var10);
               var4.add(new HudRenderer.HotkeyLine(var8.name(), var10, var8.isEnabled(), var11, var12));
               var5 = Math.max(var5, var11);
               var6 = Math.max(var6, var12);
            }
         }

         this.hotkeyNameColumn = var5;
         this.hotkeyKeyColumn = var6;
         this.hotkeyHeaderW = HudChrome.width(var1.label(), var1.labelPx(), "HOTKEYS");
         this.hotkeyCapMid = (this.capTop(var1.text(), var1.labelPx()) + HudChrome.ascent(var1.text(), var1.labelPx())) * 0.5F;
         this.hotkeyLines = List.copyOf(var4);
         return this.hotkeyLines;
      }
   }

   private void drawHotkeys(ImDrawList var1, HudLayoutStore.HudElementState var2, HudRenderer.HotkeyPlan var3, boolean var4, HudRenderer.DrawTransform var5) {
      float var6 = var5.alpha();
      if (!(var6 <= 0.001F)) {
         float var7 = scaleOf(var2);
         float var8 = Math.round(var2.x + var5.xOffset());
         float var9 = Math.round(var2.y + var5.yOffset());
         float var10 = var2.width;
         float var11 = var2.height;
         HudRenderer.Type var12 = var3.type();
         float var13 = 22.0F * var7;
         float var14 = 8.0F * var7;
         if (var3.lines().isEmpty()) {
            List var25 = List.of(new HudRenderer.Step(-1, var8, var9, var8 + var10, (float)Math.round(var9 + var13), var6));
            this.drawRows(var1, var25, hudSurface(var2), var8, var9, var10, var11, rowsRadius(var7), var7, var4);
            HudChrome.text(
               var1,
               var12.label(),
               var12.labelPx(),
               var8 + var14,
               var9 + (float)Math.floor((var13 - var12.labelPx()) * 0.5F),
               HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var6),
               "No keys bound",
               true
            );
         } else {
            float var15 = 20.0F * var7;
            ArrayList var16 = new ArrayList(var3.lines().size() + 1);
            var16.add(new HudRenderer.Step(-1, var8, var9, var8 + var10, (float)Math.round(var9 + var15), var6));

            for (int var17 = 0; var17 < var3.lines().size(); var17++) {
               var16.add(
                  new HudRenderer.Step(
                     var17, var8, (float)Math.round(var9 + var15 + var13 * var17), var8 + var10, (float)Math.round(var9 + var15 + var13 * (var17 + 1)), var6
                  )
               );
            }

            this.drawRows(var1, var16, hudSurface(var2), var8, var9, var10, var11, rowsRadius(var7), var7, var4);
            HudRenderer.Step var26 = (HudRenderer.Step)var16.get(0);
            float var18 = var26.t() + (float)Math.floor((var26.b() - var26.t() - var12.textPx()) * 0.5F);
            float var19 = var18 + HudChrome.baselineOffset(var12.text(), var12.textPx(), var12.label(), var12.labelPx());
            HudChrome.text(var1, var12.label(), var12.labelPx(), var8 + var14, var19, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var6), "HOTKEYS", true);
            float var20 = keycapWidth(this.hotkeyKeyColumn, var7);
            float var21 = (float)Math.floor((var13 - var12.textPx()) * 0.5F);

            for (int var22 = 1; var22 < var16.size(); var22++) {
               HudRenderer.Step var23 = (HudRenderer.Step)var16.get(var22);
               HudRenderer.HotkeyLine var24 = var3.lines().get(var23.item());
               HudChrome.text(
                  var1,
                  var12.text(),
                  var12.textPx(),
                  var23.l() + var14,
                  var23.t() + var21,
                  HudChrome.scaleAlpha(var24.on() ? -1 : HudChrome.TEXT_LABEL, var6),
                  var24.name(),
                  true
               );
               this.drawKeycap(var1, var12, var24, var23.r() - var14, (var23.t() + var23.b()) * 0.5F, var20, var7, var6);
            }
         }
      }
   }

   private void drawKeycap(ImDrawList var1, HudRenderer.Type var2, HudRenderer.HotkeyLine var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = keycapHeight(var7);
      float var10 = Math.round(var4 - var6);
      float var11 = Math.round(var5 - var9 * 0.5F);
      float var12 = Math.min(hudRounding(), 4.0F) * var7;
      var1.addRectFilled(var10, var11, var10 + var6, var11 + var9, HudChrome.scaleAlpha(452984831, var8), var12);
      var1.addRect(var10, var11, var10 + var6, var11 + var9, HudChrome.scaleAlpha(402653183, var8), var12, 0, 1.0F);
      HudChrome.text(
         var1,
         var2.text(),
         var2.labelPx(),
         Math.round(var10 + (var6 - var3.keyW()) * 0.5F),
         Math.round(var11 + var9 * 0.5F - this.hotkeyCapMid),
         HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var8),
         var3.key(),
         true
      );
   }

   private static float keycapHeight(float var0) {
      return Math.round(16.0F * var0);
   }

   private static float keycapWidth(float var0, float var1) {
      return Math.max(keycapHeight(var1), (float)Math.ceil(var0 + 5.0F * var1 * 2.0F));
   }

   private HudRenderer.SpotifyPlan planSpotify(HudLayoutStore.HudElementState var1, boolean var2, float var3) {
      float var5 = scaleOf(var1);
      HudRenderer.Type var6 = HudRenderer.Type.of(var5);
      if (!var2 && !SpotifyHudModule.shouldShowHud()) {
         this.removeSlot("spotify", var6.textPx());
         this.releaseSpotifyArt();
         this.lyricBlockH = 0.0F;
         return new HudRenderer.SpotifyPlan(
            var6,
            HudRenderer.SpotifyLook.HIDDEN,
            HudRenderer.SpotifyGlyph.NONE,
            "",
            "",
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            false,
            null,
            HudRenderer.SpotifyColors.DEFAULT,
            1.0F,
            (float)Math.round(28.0F * var5)
         );
      } else {
         SpotifyHudModule var7 = SpotifyHudModule.get();
         SpotifyHudModule var8 = SpotifyHudModule.active();
         NowPlaying var9 = var8 != null ? var8.nowPlaying() : NowPlaying.WAITING;
         boolean var10 = var9.active() && var9.hasTrack();
         boolean var4 = var2 || var1.visible;
         if (var10 && var9.source() == NowPlaying.Source.SESSION) {
            return this.planSpotifyCard(var7, var9, false, var4, var6, var5, var2, var3);
         } else if (var2 && !var10) {
            return this.planSpotifyCard(var7, SAMPLE_TRACK, true, true, var6, var5, true, var3);
         } else {
            this.releaseSpotifyArt();
            this.lyricBlockH = 0.0F;
            return this.planSpotifyCompact(var7, var9, var6, var5, var2, var3);
         }
      }
   }

   private HudRenderer.SpotifyPlan planSpotifyCompact(SpotifyHudModule var1, NowPlaying var2, HudRenderer.Type var3, float var4, boolean var5, float var6) {
      boolean var8 = var1 == null || var1.showArtist();
      String var9 = "";
      HudRenderer.SpotifyGlyph var10 = HudRenderer.SpotifyGlyph.NONE;
      boolean var11 = false;
      String var7;
      switch (var2.status()) {
         case PLAYING:
         case PAUSED:
            var10 = var2.playing() ? HudRenderer.SpotifyGlyph.PLAYING : HudRenderer.SpotifyGlyph.PAUSED;
            this.foldTrack(var2, var3.text());
            var7 = var2.hasTrack() ? this.spotifyTitle : "Not playing";
            if (var8) {
               var9 = this.spotifyArtist;
            }
            break;
         case NOT_RUNNING:
            var7 = "Spotify is not open";
            var11 = true;
            break;
         case UNSUPPORTED:
            var7 = "Spotify \u00b7 not supported here";
            var11 = true;
            break;
         default:
            var7 = "Nothing playing";
            var11 = true;
      }

      ImFont var12 = var11 ? var3.label() : var3.text();
      int var13 = var11 ? var3.labelPx() : var3.textPx();
      float var14 = HudChrome.width(var12, var13, var7);
      float var15 = var9.isEmpty() ? 0.0F : HudChrome.width(var3.label(), var3.labelPx(), var9);
      float var16 = var10 == HudRenderer.SpotifyGlyph.NONE ? 0.0F : 20.0F * var4;
      float var17 = 12.0F * var4 * 2.0F + var16;
      float var18 = Math.max(40.0F * var4, (var1 != null ? var1.cardWidth() : 320.0F) * var4 - var17);
      float var19 = Math.min(Math.max(var14, var15), var18);
      float var20 = this.slot("spotify", var3.textPx(), (float)Math.ceil(var17 + var19), var6);
      float var21 = var9.isEmpty() ? 28.0F * var4 : 8.0F * var4 * 2.0F + var3.textPx() + 2.0F * var4 + var3.labelPx();
      float var22 = Math.max(1.0F, var20 - var17);
      boolean var23 = var1 == null || var1.scrollTitles();
      float var24 = 40.0F * var4;
      float[] var25 = this.marquee(var7, var9, var14, var15, var22, var23, var2.playing() || var5, var24, var4, var6);
      return new HudRenderer.SpotifyPlan(
         var3,
         var11 ? HudRenderer.SpotifyLook.IDLE : HudRenderer.SpotifyLook.COMPACT,
         var10,
         var7,
         var9,
         var14,
         var15,
         var25[0],
         var25[1],
         var24,
         var23,
         null,
         HudRenderer.SpotifyColors.of(var1),
         (float)Math.round(var20),
         (float)Math.round(var21)
      );
   }

   private void drawSpotify(ImDrawList var1, HudLayoutStore.HudElementState var2, HudRenderer.SpotifyPlan var3, boolean var4, HudRenderer.DrawTransform var5) {
      float var6 = var5.alpha();
      if (!(var6 <= 0.001F) && var3.look() != HudRenderer.SpotifyLook.HIDDEN) {
         float var7 = scaleOf(var2);
         float var8 = Math.round(var2.x + var5.xOffset());
         float var9 = Math.round(var2.y + var5.yOffset());
         float var10 = var2.width;
         float var11 = var2.height;
         Surface var12 = hudSurface(var2);
         boolean var13 = HudChrome.needsTextGuard(var12);
         HudChrome.card(var1, var12, var8, var9, var10, var11, hudRounding() * var7, var6, var4, var7);
         if (var3.look() == HudRenderer.SpotifyLook.CARD) {
            this.drawSpotifyCard(var1, var3, var8, var9, var10, var11, var7, var6, var13);
         } else {
            this.drawSpotifyCompact(var1, var3, var8, var9, var10, var11, var7, var6, var13);
         }
      }
   }

   private void drawSpotifyCompact(
      ImDrawList var1, HudRenderer.SpotifyPlan var2, float var3, float var4, float var5, float var6, float var7, float var8, boolean var9
   ) {
      HudRenderer.Type var10 = var2.type();
      float var11 = var3 + 12.0F * var7;
      if (var2.glyph() != HudRenderer.SpotifyGlyph.NONE) {
         this.drawPlayState(var1, var11, var4 + var6 * 0.5F, 10.0F * var7, var2.glyph() == HudRenderer.SpotifyGlyph.PLAYING, var8);
         var11 += 20.0F * var7;
      }

      float var12 = Math.max(1.0F, var3 + var5 - 12.0F * var7 - var11);
      if (var2.look() == HudRenderer.SpotifyLook.IDLE) {
         float var18 = var4 + (float)Math.floor((var6 - var10.labelPx()) * 0.5F);
         marqueeLine(
            var1,
            var10.label(),
            var10.labelPx(),
            var11,
            var18,
            var12,
            var2.titleW(),
            0.0F,
            var2.loopGap(),
            false,
            var2.title(),
            HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var8),
            var9
         );
      } else {
         float var13 = var2.glyph() == HudRenderer.SpotifyGlyph.PAUSED ? 0.62F : 1.0F;
         boolean var14 = !var2.artist().isEmpty();
         float var15 = var14 ? var10.textPx() + 2.0F * var7 + var10.labelPx() : var10.textPx();
         float var16 = var4 + (float)Math.floor((var6 - var15) * 0.5F);
         marqueeLine(
            var1,
            var10.text(),
            var10.textPx(),
            var11,
            var16,
            var12,
            var2.titleW(),
            var2.titleOffset(),
            var2.loopGap(),
            var2.scroll(),
            var2.title(),
            HudChrome.scaleAlpha(var2.colors().title(), var8 * var13),
            var9
         );
         if (var14) {
            float var17 = var16 + var10.textPx() + 2.0F * var7;
            marqueeLine(
               var1,
               var10.label(),
               var10.labelPx(),
               var11,
               var17,
               var12,
               var2.artistW(),
               var2.artistOffset(),
               var2.loopGap(),
               var2.scroll(),
               var2.artist(),
               HudChrome.scaleAlpha(var2.colors().artist(), var8),
               var9
            );
         }
      }
   }

   private void drawPlayState(ImDrawList var1, float var2, float var3, float var4, boolean var5, float var6) {
      float var7 = Math.round(var3 - var4 * 0.5F);
      float var8 = var7 + Math.round(var4);
      if (var5) {
         var1.addTriangleFilled(var2, var7, var2 + var4 * 0.9F, (var7 + var8) * 0.5F, var2, var8, HudChrome.scaleAlpha(this.hudAccent(), var6));
      } else {
         float var9 = Math.max(2.0F, (float)Math.round(var4 * 0.3F));
         float var10 = Math.max(2.0F, (float)Math.round(var4 * 0.25F));
         int var11 = HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var6);
         float var12 = Math.round(var2 + (var4 - var9 * 2.0F - var10) * 0.5F);
         var1.addRectFilled(var12, var7, var12 + var9, var8, var11, 1.0F);
         var1.addRectFilled(var12 + var9 + var10, var7, var12 + var9 * 2.0F + var10, var8, var11, 1.0F);
      }
   }

   private HudRenderer.SpotifyPlan planSpotifyCard(
      SpotifyHudModule var1, NowPlaying var2, boolean var3, boolean var4, HudRenderer.Type var5, float var6, boolean var7, float var8
   ) {
      boolean var9 = var1 == null || var1.showArtist();
      boolean var10 = var1 == null || var1.showAlbumArt();
      boolean var11 = var1 == null || var1.showProgress();
      boolean var12 = var1 == null || var1.showLyrics();
      boolean var13 = var1 == null || var1.scrollTitles();
      long var14 = System.currentTimeMillis();
      this.foldTrack(var2, var5.text());
      String var16 = this.spotifyTitle;
      String var17 = var9 ? this.spotifyArtist : "";
      boolean var18 = var2.hasTimeline();
      long var19 = var2.positionMs();
      if (!var3) {
         this.spotifyClock.update(var2, var14);
         var19 = this.spotifyClock.positionAt(var14);
      }

      float var21 = this.capTop(var5.label(), var5.labelPx());
      float var22 = var5.labelPx() + 3.0F * var6 + var5.textPx() + (var17.isEmpty() ? 0.0F : 1.0F * var6 + var5.labelPx()) + 7.0F * var6;
      boolean var23 = var11 && var18;
      float var24 = var23 ? var22 + var5.labelPx() + 6.0F * var6 + progressHeight(var6) : var22 + HudChrome.ascent(var5.label(), var5.labelPx());
      float var25 = Math.round(var24 - var21);
      float var26 = 10.0F * var6;
      float var27 = Math.round(Math.max(200.0F, var1 != null ? var1.cardWidth() : 320.0F) * var6);
      boolean var28 = var10 && var27 - var26 * 2.0F - var25 - 10.0F * var6 >= 128.0F * var6;
      float var29 = var27 - var26 * 2.0F - (var28 ? var25 + 10.0F * var6 : 0.0F);
      String var30 = "";
      String var31 = "";
      float var32 = 0.0F;
      if (var18) {
         long var33 = var19 / 1000L;
         long var35 = (var2.durationMs() + 500L) / 1000L;
         var30 = formatSeconds(var33);
         var31 = "\u2212" + formatSeconds(Math.max(0L, var35 - var33));
         var32 = clamp01((float)var19 / (float)var2.durationMs());
      }

      float var48 = HudChrome.width(var5.text(), var5.textPx(), var16);
      float var34 = var17.isEmpty() ? 0.0F : HudChrome.width(var5.label(), var5.labelPx(), var17);
      float var49 = 40.0F * var6;
      float[] var36 = this.marquee(var16, var17, var48, var34, var29, var13, var2.playing() || var7, var49, var6, var8);
      boolean var37 = var3 ? var12 && var18 : var4 && var1 != null && var1.wantsLyrics(var2);
      Lyrics var38 = !var37 ? null : (var3 ? SAMPLE_LYRICS : var1.lyricsFor(var2));
      boolean var39 = var1 == null || var1.lyricsOnline();
      int var40 = var1 == null ? 2 : var1.lyricLines();
      SpotifyHudModule.LyricMotion var41 = var1 == null ? SpotifyHudModule.LyricMotion.WAVE : var1.lyricMotion();
      SpotifyHudModule.LyricHighlight var42 = var1 == null ? SpotifyHudModule.LyricHighlight.OFF : var1.lyricHighlight();
      HudRenderer.LyricView var43 = this.planLyrics(var38, var37, var39, var19, var40, var41, var42, var5, var27 - var26 * 2.0F, var6, var8, var14);
      float var44 = var2.playing() ? 1.0F : 0.0F;
      this.lyricPlaying = this.lyricPlaying + (var44 - this.lyricPlaying) * (1.0F - (float)Math.exp(-8.0F * var8));
      float var45 = 0.7F + 0.3F * this.lyricPlaying;
      HudRenderer.SpotifyCard var46 = new HudRenderer.SpotifyCard(
         var2, var3, var28, var25, var26 - var21, var29, var18, var30, var31, var32, var23, var43, var45
      );
      float var47 = var26 * 2.0F + var25 + var43.height();
      return new HudRenderer.SpotifyPlan(
         var5,
         HudRenderer.SpotifyLook.CARD,
         HudRenderer.SpotifyGlyph.NONE,
         var16,
         var17,
         var48,
         var34,
         var36[0],
         var36[1],
         var49,
         var13,
         var46,
         HudRenderer.SpotifyColors.of(var1),
         var27,
         (float)Math.round(var47)
      );
   }

   private void drawSpotifyCard(
      ImDrawList var1, HudRenderer.SpotifyPlan var2, float var3, float var4, float var5, float var6, float var7, float var8, boolean var9
   ) {
      HudRenderer.SpotifyCard var11 = var2.card();
      HudRenderer.Type var12 = var2.type();
      float var13 = 10.0F * var7;
      boolean var14 = var11.track().playing();
      float var15 = var3 + var13;
      if (var11.art()) {
         this.drawAlbumArt(var1, var11, var15, var4 + var13, var11.header(), var7, var8);
         var15 += var11.header() + 10.0F * var7;
      } else {
         this.releaseSpotifyArt();
      }

      float var16 = var11.column();
      float var17 = var4 + var11.textTop();
      HudChrome.text(
         var1,
         var12.label(),
         var12.labelPx(),
         var15,
         Math.round(var17),
         HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var8),
         var14 ? "NOW PLAYING" : "PAUSED",
         var9
      );
      float var19;
      marqueeLine(
         var1,
         var12.text(),
         var12.textPx(),
         var15,
         Math.round(var19 = var17 + (var12.labelPx() + 3.0F * var7)),
         var16,
         var2.titleW(),
         var2.titleOffset(),
         var2.loopGap(),
         var2.scroll(),
         var2.title(),
         HudChrome.scaleAlpha(var2.colors().title(), var8),
         var9
      );
      var17 = var19 + var12.textPx();
      if (!var2.artist().isEmpty()) {
         float var21;
         marqueeLine(
            var1,
            var12.label(),
            var12.labelPx(),
            var15,
            Math.round(var21 = var17 + 1.0F * var7),
            var16,
            var2.artistW(),
            var2.artistOffset(),
            var2.loopGap(),
            var2.scroll(),
            var2.artist(),
            HudChrome.scaleAlpha(var2.colors().artist(), var8),
            var9
         );
         var17 = var21 + var12.labelPx();
      }

      var17 = Math.round(var17 + 7.0F * var7);
      this.drawTransport(var1, var12, var11, var15, var17, var16, var7, var8, var9);
      if (var11.progressBar()) {
         float var18 = Math.round(var17 + var12.labelPx() + 6.0F * var7);
         this.drawProgress(var1, var15, var18, var16, progressHeight(var7), var11.progress(), var14, var8);
      }

      HudRenderer.LyricView var10;
      if ((var10 = var11.lyrics()).height() > 0.5F) {
         float var23 = Math.round(var4 + var13 + var11.header() + 12.0F * var7);
         this.drawLyrics(
            var1,
            var12,
            var10,
            var2.colors(),
            var3 + var13,
            var23,
            var5 - var13 * 2.0F,
            var4 + var6 - Math.max(1.0F, var7),
            var7,
            var8 * var11.lyricAlpha(),
            var9
         );
      }
   }

   private void drawAlbumArt(ImDrawList var1, HudRenderer.SpotifyCard var2, float var3, float var4, float var5, float var6, float var7) {
      float var9 = Math.min(hudRounding() * 0.5F, 4.0F) * var6;
      if (var2.sample()) {
         this.releaseSpotifyArt();
      }

      ThemeImages.Texture var8 = var2.sample() ? null : this.spotifyArtTexture(var2.track().art());
      if (var8 != null) {
         var1.addImageRounded(var8.id(), var3, var4, var3 + var5, var4 + var5, 0.0F, 0.0F, 1.0F, 1.0F, HudChrome.scaleAlpha(-1, var7), var9, 240);
      } else {
         var1.addRectFilled(var3, var4, var3 + var5, var4 + var5, HudChrome.scaleAlpha(318767103, var7), var9);
         float var11 = Math.max(2.0F, (float)Math.round(var5 * 0.07F));
         float var12 = Math.max(2.0F, (float)Math.round(var11 * 0.8F));
         float var13 = Math.round(var4 + var5 * 0.62F);
         float var14 = Math.round(var3 + (var5 - var11 * 3.0F - var12 * 2.0F) * 0.5F);
         int var15 = HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var7 * 0.45F);
         float[] var16 = new float[]{0.2F, 0.34F, 0.26F};

         for (int var17 = 0; var17 < var16.length; var17++) {
            float var18 = var14 + var17 * (var11 + var12);
            var1.addRectFilled(var18, var13 - Math.round(var5 * var16[var17]), var18 + var11, var13, var15, 1.0F);
         }
      }

      var1.addRect(var3, var4, var3 + var5, var4 + var5, HudChrome.scaleAlpha(402653183, var7), var9, 0, 1.0F);
   }

   private void drawTransport(
      ImDrawList var1, HudRenderer.Type var2, HudRenderer.SpotifyCard var3, float var4, float var5, float var6, float var7, float var8, boolean var9
   ) {
      float var16 = 0.0F;
      float var17 = 0.0F;
      if (var3.timeline()) {
         var16 = HudChrome.tabularWidth(var2.label(), var2.labelPx(), var3.elapsed());
         var17 = HudChrome.tabularWidth(var2.label(), var2.labelPx(), var3.remaining());
         int var18 = HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var8);
         HudChrome.tabular(var1, var2.label(), var2.labelPx(), var4, var5, var18, var3.elapsed(), var9);
         HudChrome.tabular(var1, var2.label(), var2.labelPx(), Math.round(var4 + var6 - var17), var5, var18, var3.remaining(), var9);
      }

      float var10;
      float var11;
      float var12;
      float var13;
      float var14;
      float var15;
      if (!(
            (var15 = var4 + var6 * 0.5F)
                  - (
                        var14 = (var13 = Math.max(4.0F, (float)Math.round(8.0F * var7))) * 2.0F
                           + (var12 = Math.max(5.0F, (float)Math.round(10.0F * var7)))
                           + (var11 = Math.round(14.0F * var7)) * 2.0F
                     )
                     * 0.5F
               < var4 + var16 + (var10 = 8.0F * var7)
         )
         && !(var15 + var14 * 0.5F > var4 + var6 - var17 - var10)) {
         float var33 = var5 + (this.capTop(var2.label(), var2.labelPx()) + HudChrome.ascent(var2.label(), var2.labelPx())) * 0.5F;
         float var19 = Math.round(var15 - var12 * 0.5F);
         float[] var20 = new float[]{var19 - var11 - var13, var19, var19 + var12 + var11};
         float[] var21 = new float[]{var13, var12, var13};
         float var22 = Math.round(3.0F * var7);
         float var23 = ImGui.getMousePosX();
         float var24 = ImGui.getMousePosY();

         for (int var25 = 0; var25 < 3; var25++) {
            float var27 = var20[var25] - var22;
            float var28 = var20[var25] + var21[var25] + var22;
            float var29 = Math.round(var33 - var21[var25] * 0.5F) - var22;
            float var30 = Math.round(var33 + var21[var25] * 0.5F) + var22;
            this.spotifyButtons[var25 * 4] = var27;
            this.spotifyButtons[var25 * 4 + 1] = var29;
            this.spotifyButtons[var25 * 4 + 2] = var28;
            this.spotifyButtons[var25 * 4 + 3] = var30;
            boolean var26 = this.chatInput && var23 >= var27 && var23 <= var28 && var24 >= var29 && var24 <= var30;
            if (var26) {
               var1.addRectFilled(var27, var29, var28, var30, HudChrome.scaleAlpha(654311423, var8), (var28 - var27) * 0.5F);
            }

            int var32 = HudChrome.scaleAlpha(!var26 && var25 != 1 ? HudChrome.TEXT_LABEL : HudChrome.TEXT_NAME, var8);
            if (var25 == 1) {
               drawPlayPauseGlyph(var1, var20[var25], var33, var12, var3.track().playing(), var32);
            } else {
               drawSkipGlyph(var1, var20[var25], var33, var13, var25 == 2, var32);
            }
         }

         this.spotifyButtonsDrawn = true;
      }
   }

   private int spotifyButtonAt(float var1, float var2) {
      for (int var3 = 0; var3 < SPOTIFY_BUTTONS.length; var3++) {
         if (var1 >= this.spotifyButtons[var3 * 4]
            && var1 <= this.spotifyButtons[var3 * 4 + 2]
            && var2 >= this.spotifyButtons[var3 * 4 + 1]
            && var2 <= this.spotifyButtons[var3 * 4 + 3]) {
            return var3;
         }
      }

      return -1;
   }

   private static void drawSkipGlyph(ImDrawList var0, float var1, float var2, float var3, boolean var4, int var5) {
      float var6 = Math.round(var2 - var3 * 0.5F);
      float var7 = var6 + var3;
      float var8 = (var6 + var7) * 0.5F;
      float var9 = Math.max(1.0F, (float)Math.round(var3 * 0.2F));
      if (var4) {
         var0.addTriangleFilled(var1, var6, var1 + var3 - var9, var8, var1, var7, var5);
         var0.addRectFilled(var1 + var3 - var9, var6, var1 + var3, var7, var5);
      } else {
         var0.addRectFilled(var1, var6, var1 + var9, var7, var5);
         var0.addTriangleFilled(var1 + var3, var6, var1 + var3, var7, var1 + var9, var8, var5);
      }
   }

   private static void drawPlayPauseGlyph(ImDrawList var0, float var1, float var2, float var3, boolean var4, int var5) {
      float var6 = Math.round(var2 - var3 * 0.5F);
      float var7 = var6 + var3;
      if (var4) {
         float var11 = Math.max(2.0F, (float)Math.round(var3 * 0.3F));
         float var9 = Math.max(2.0F, (float)Math.round(var3 * 0.24F));
         float var10 = Math.round(var1 + (var3 - var11 * 2.0F - var9) * 0.5F);
         var0.addRectFilled(var10, var6, var10 + var11, var7, var5, 1.0F);
         var0.addRectFilled(var10 + var11 + var9, var6, var10 + var11 * 2.0F + var9, var7, var5, 1.0F);
      } else {
         float var8 = Math.round(var3 * 0.08F);
         var0.addTriangleFilled(var1 + var8, var6, var1 + var8 + var3 * 0.86F, (var6 + var7) * 0.5F, var1 + var8, var7, var5);
      }
   }

   private void drawProgress(ImDrawList var1, float var2, float var3, float var4, float var5, float var6, boolean var7, float var8) {
      float var9 = var5 * 0.5F;
      var1.addRectFilled(var2, var3, var2 + var4, var3 + var5, HudChrome.scaleAlpha(452984831, var8), var9);
      float var10 = Math.round(var4 * clamp01(var6));
      if (!(var10 < 1.0F)) {
         var1.addRectFilled(var2, var3, var2 + var10, var3 + var5, HudChrome.scaleAlpha(var7 ? this.hudAccent() : HudChrome.TEXT_LABEL, var8), var9);
      }
   }

   private static float progressHeight(float var0) {
      return Math.max(1.0F, (float)Math.round(2.0F * var0));
   }

   private HudRenderer.LyricView planLyrics(
      Lyrics var1,
      boolean var2,
      boolean var3,
      long var4,
      int var6,
      SpotifyHudModule.LyricMotion var7,
      SpotifyHudModule.LyricHighlight var8,
      HudRenderer.Type var9,
      float var10,
      float var11,
      float var12,
      long var13
   ) {
      Object var15 = List.of();
      String var16 = null;
      float var17 = -1.0F;
      boolean var18 = false;
      if (!var2) {
         this.lyricLoadingSince = 0L;
         var17 = 0.0F;
      } else {
         if (var1 == null || var1.status() != Lyrics.Status.LOADING) {
            this.lyricLoadingSince = 0L;
         } else if (this.lyricLoadingSince == 0L) {
            this.lyricLoadingSince = var13;
         }

         if (var1 != null) {
            switch (var1.status()) {
               case LOADING:
                  var16 = var13 - this.lyricLoadingSince < 700L ? null : "Loading lyrics";
                  break;
               case SYNCED:
                  this.prepareLyrics(var1, var9.text());
                  if (this.lyricReadable) {
                     var15 = new ArrayList(16);
                     float var19 = this.lyricViewHeight(var1.synced(), var6, var9, var10, var11);
                     this.syncedItems((List<HudRenderer.LyricItem>)var15, var1.synced(), var4, var19, var7, var8, var9, var10, var11, var12);
                     var17 = var19 - (var9.textPx() - HudChrome.ascent(var9.text(), var9.textPx()));
                     var18 = true;
                  } else {
                     var16 = "Lyrics in a script the HUD font lacks";
                  }
                  break;
               case PLAIN:
                  var16 = "No synced lyrics";
                  break;
               case INSTRUMENTAL:
                  var16 = "Instrumental";
                  break;
               case NOT_FOUND:
                  var16 = var3 ? "No lyrics found" : "No saved lyrics";
                  break;
               case UNAVAILABLE:
                  var16 = "Lyrics unavailable";
            }
         }
      }

      if (var16 != null) {
         var17 = Math.round((var9.textPx() - var9.labelPx()) * 0.5F) + HudChrome.ascent(var9.label(), var9.labelPx());
      }

      if (var18) {
         this.lyricLastItems = (List<HudRenderer.LyricItem>)var15;
         this.lyricLeaveItems = null;
      } else {
         var15 = this.leavingItems(var16 == null, var9, var11, var12);
      }

      float var20 = var17 < 0.0F ? this.lyricBlockH : (var17 > 0.0F ? 12.0F * var11 + Math.round(var17) : 0.0F);
      return new HudRenderer.LyricView((List<HudRenderer.LyricItem>)var15, var16, this.easeLyricBlock(var20, var12));
   }

   private List<HudRenderer.LyricItem> leavingItems(boolean var1, HudRenderer.Type var2, float var3, float var4) {
      this.lyricSource = null;
      if (!this.lyricLastItems.isEmpty()) {
         this.lyricLeaveItems = this.lyricLastItems;
         this.lyricLeave = 0.0F;
         this.lyricLastItems = List.of();
      }

      if (this.lyricLeaveItems == null) {
         return List.of();
      } else {
         this.lyricLeave = Math.min(1.0F, this.lyricLeave + var4 / 0.32F);
         if (var1 && !(this.lyricLeave >= 1.0F)) {
            ArrayList var5 = new ArrayList(this.lyricLeaveItems.size());
            addFaded(var5, this.lyricLeaveItems, smooth(this.lyricLeave), 8.0F * var3);
            return var5;
         } else {
            this.lyricLeaveItems = null;
            return List.of();
         }
      }
   }

   private static void addFaded(List<HudRenderer.LyricItem> var0, List<HudRenderer.LyricItem> var1, float var2, float var3) {
      for (HudRenderer.LyricItem var5 : var1) {
         float var6 = var5.alpha() * (1.0F - var2);
         if (!(var6 <= 0.004F)) {
            var0.add(new HudRenderer.LyricItem(var5.text(), var5.top() - var3 * var2, var5.highlight(), var6, var5.progress(), var5.fill()));
         }
      }
   }

   private void syncedItems(
      List<HudRenderer.LyricItem> var1,
      SyncedLyrics var2,
      long var3,
      float var5,
      SpotifyHudModule.LyricMotion var6,
      SpotifyHudModule.LyricHighlight var7,
      HudRenderer.Type var8,
      float var9,
      float var10,
      float var11
   ) {
      float var13 = 6.0F * var10;
      int var14 = var2.indexAt(var3 + 200L);
      int var15 = var2.firstWithTimeOf(var14);
      int var16 = Math.max(0, var15);
      if (var2 != this.lyricSource) {
         this.lyricSource = var2;
         this.lyricStart = var16;
         this.lyricShifts.clear();
         this.lyricClock = 0.0F;
         this.lyricFadeItems = null;
         this.lyricFade = 1.0F;
         this.lyricShown = new float[var2.size() * 2];
         this.lyricBright = new float[var2.size()];
         this.lyricBrightLo = 0;
         this.lyricBrightHi = -1;

         for (int var17 = Math.max(0, var15); var17 <= var14; var17++) {
            this.lyricBright[var17] = 1.0F;
         }

         if (var14 >= 0) {
            this.lyricBrightLo = Math.max(0, var15);
            this.lyricBrightHi = var14;
         }
      }

      this.lyricClock += var11;
      this.lyricBreath = (this.lyricBreath + var11) % 1.6F;
      this.lyricCurrentFirst = var15;
      this.lyricCurrentLast = var14;
      float var12 = var6 == SpotifyHudModule.LyricMotion.WAVE ? 0.045F : 0.0F;
      if (var16 != this.lyricStart) {
         float var18 = var16 > this.lyricStart ? this.restDistance(this.lyricStart, var16, var8, var9, var10) : -1.0F;
         if (var6 != SpotifyHudModule.LyricMotion.FADE && var18 > 0.0F && var18 <= var5 + var13 + 0.5F) {
            if (this.lyricShifts.size() == 8) {
               this.lyricShifts.remove(0);
            }

            this.lyricShifts.add(new HudRenderer.LyricShift(var18, this.lyricClock, var16, var12));
         } else {
            this.lyricFadeItems = this.lyricLastItems;
            this.lyricFade = 0.0F;
            this.lyricShifts.clear();
            this.showView(var2, var16, var5, var8, var9, var10);
         }

         this.lyricStart = var16;
      }

      this.updateBrightness(var15, var14, var11);
      this.lyricFade = Math.min(1.0F, this.lyricFade + var11 / 0.32F);
      if (this.lyricFade >= 1.0F) {
         this.lyricFadeItems = null;
      }

      float var21 = 1.37F;
      this.lyricShifts.removeIf(var2x -> this.lyricClock - var2x.at() > var21);
      if (this.lyricShifts.isEmpty()) {
         this.lyricClock = 0.0F;
      }

      float var19 = 8.0F * var10;
      if (this.lyricFadeItems != null) {
         float var20 = smooth(this.lyricFade);
         addFaded(var1, this.lyricFadeItems, var20, var19);
         this.addLyricView(var1, var2, this.lyricStart, var20, var19 * (1.0F - var20), var5, var3, var7, var8, var9, var10, var11);
      } else {
         this.addLyricView(var1, var2, this.lyricStart, 1.0F, 0.0F, var5, var3, var7, var8, var9, var10, var11);
      }
   }

   private void addLyricView(
      List<HudRenderer.LyricItem> var1,
      SyncedLyrics var2,
      int var3,
      float var4,
      float var5,
      float var6,
      long var7,
      SpotifyHudModule.LyricHighlight var9,
      HudRenderer.Type var10,
      float var11,
      float var12,
      float var13
   ) {
      ImFont var18 = var10.text();
      int var19 = var10.textPx();
      float var20 = 6.0F * var12;
      float var21 = 6.0F * var12;
      float var22 = var19 + var21;
      boolean var23 = var9 == SpotifyHudModule.LyricHighlight.KARAOKE;
      float var24 = 0.0F;

      for (int var25 = var3; var25 < var2.size() && var24 <= var6 + var22 * 2.0F; var25++) {
         String[] var17 = this.rowsFor(var25, var18, var19, var11);
         int var26 = Math.max(1, var17.length);
         float var27 = var24 + this.lyricOffset(var25) + var5;
         float var28 = 1.0F - 0.39999998F * clamp01(var27 / var22 - 1.0F);
         float var16 = this.lyricGlow(var25, var9);
         float var15 = var17.length == 0 ? this.breakProgress(var2, var25, var7) : 1.0F;

         for (int var14 = 0; var14 < var26; var14++) {
            float var29 = var24 + var14 * (var19 + var20);
            float var30 = this.updateShown(var25, var14, var29 + var19 <= var6 + 0.5F, var13);
            float var31 = var27 + var14 * (var19 + var20);
            float var32 = var4 * var28 * smooth(var30) * topFade(var31, var12) * bottomFade(var31 + var19 - var6, var19);
            if (!(var32 <= 0.004F)) {
               float var33 = var23 && var17.length > 0 ? this.rowFill(var2, var25, var17, var14, var18, var19, var7) : 0.0F;
               var1.add(new HudRenderer.LyricItem(var17.length == 0 ? null : var17[var14], var31, var16, var32, var15, var33));
            }
         }

         var24 += lineHeight(var17, var19, var20) + var21;
      }

      float var37 = 0.0F;

      for (int var38 = var3 - 1; var38 >= 0; var38--) {
         String[] var39 = this.rowsFor(var38, var18, var19, var11);
         int var40 = Math.max(1, var39.length);
         float var41 = (var37 -= lineHeight(var39, var19, var20) + var21) + this.lyricOffset(var38) + var5;
         if (topFade(var41 + (var40 - 1) * (var19 + var20), var12) <= 0.0F) {
            break;
         }

         float var36 = this.lyricGlow(var38, var9);
         float var35 = var39.length == 0 ? this.breakProgress(var2, var38, var7) : 1.0F;

         for (int var34 = 0; var34 < var40; var34++) {
            float var42 = var41 + var34 * (var19 + var20);
            float var43 = var4 * smooth(this.lyricShown[var38 * 2 + var34]) * topFade(var42, var12);
            if (!(var43 <= 0.004F)) {
               float var44 = var23 && var39.length > 0 ? this.rowFill(var2, var38, var39, var34, var18, var19, var7) : 0.0F;
               var1.add(new HudRenderer.LyricItem(var39.length == 0 ? null : var39[var34], var42, var36, var43, var35, var44));
            }
         }
      }
   }

   private float lyricOffset(int var1) {
      float var2 = 0.0F;

      for (int var3 = 0; var3 < this.lyricShifts.size(); var3++) {
         HudRenderer.LyricShift var4 = this.lyricShifts.get(var3);
         float var5 = Math.max(0, Math.min(6, var1 - var4.start())) * var4.stagger();
         float var6 = this.lyricClock - var4.at() - var5;
         var2 += var6 <= 0.0F ? var4.distance() : var4.distance() * springLeft(var6);
      }

      return var2;
   }

   private static float springLeft(float var0) {
      float var1 = 13.0F * var0;
      return (1.0F + var1) * (float)Math.exp(-var1);
   }

   private static float topFade(float var0, float var1) {
      return var0 >= 0.0F ? 1.0F : smooth(1.0F + var0 / (12.0F * var1 * 0.75F));
   }

   private static float bottomFade(float var0, int var1) {
      return var0 <= 0.5F ? 1.0F : smooth(1.0F - var0 / (var1 * 0.6F));
   }

   private float updateShown(int var1, int var2, boolean var3, float var4) {
      int var6 = var1 * 2 + var2;
      float var7 = var4 / 0.3F;
      float var5;
      this.lyricShown[var6] = var5 = var3 ? Math.min(1.0F, this.lyricShown[var6] + var7) : Math.max(0.0F, this.lyricShown[var6] - var7);
      return var5;
   }

   private void showView(SyncedLyrics var1, int var2, float var3, HudRenderer.Type var4, float var5, float var6) {
      Arrays.fill(this.lyricShown, 0.0F);
      int var8 = var4.textPx();
      float var9 = 6.0F * var6;
      float var10 = 0.0F;

      for (int var11 = var2; var11 < var1.size() && var10 < var3; var11++) {
         String[] var7 = this.rowsFor(var11, var4.text(), var8, var5);
         int var12 = Math.max(1, var7.length);

         for (int var13 = 0; var13 < var12; var13++) {
            if (var10 + var13 * (var8 + var9) + var8 <= var3 + 0.5F) {
               this.lyricShown[var11 * 2 + var13] = 1.0F;
            }
         }

         var10 += lineHeight(var7, var8, var9) + 6.0F * var6;
      }
   }

   private float restDistance(int var1, int var2, HudRenderer.Type var3, float var4, float var5) {
      float var6 = 6.0F * var5;
      float var7 = 0.0F;

      for (int var8 = var1; var8 < var2; var8++) {
         var7 += lineHeight(this.rowsFor(var8, var3.text(), var3.textPx(), var4), var3.textPx(), var6) + 6.0F * var5;
      }

      return var7;
   }

   private void updateBrightness(int var1, int var2, float var3) {
      int var4 = this.lyricBrightHi >= 0 ? this.lyricBrightLo : 2147483647;
      int var5 = this.lyricBrightHi;
      if (var2 >= 0) {
         var4 = Math.min(var4, Math.max(0, var1));
         var5 = Math.max(var5, var2);
      }

      float var6 = var3 / 0.35F;
      int var7 = 2147483647;
      int var8 = -1;

      for (int var9 = Math.max(0, var4); var9 <= var5 && var9 < this.lyricBright.length; var9++) {
         boolean var11 = var9 >= var1 && var9 <= var2;
         float var10;
         this.lyricBright[var9] = var10 = var11 ? Math.min(1.0F, this.lyricBright[var9] + var6) : Math.max(0.0F, this.lyricBright[var9] - var6);
         if (var10 > 0.0F) {
            var7 = Math.min(var7, var9);
            var8 = var9;
         }
      }

      this.lyricBrightLo = var8 < 0 ? 0 : var7;
      this.lyricBrightHi = var8;
   }

   private float lyricGlow(int var1, SpotifyHudModule.LyricHighlight var2) {
      if (var2 != SpotifyHudModule.LyricHighlight.OFF && var1 >= 0 && var1 < this.lyricCurrentFirst) {
         return 1.0F;
      } else {
         return var1 >= 0 && var1 < this.lyricBright.length ? smooth(this.lyricBright[var1]) : 0.0F;
      }
   }

   private float breakProgress(SyncedLyrics var1, int var2, long var3) {
      if (var2 >= this.lyricCurrentFirst && var2 <= this.lyricCurrentLast) {
         int var5 = var1.lastWithTimeOf(var2) + 1;
         if (var5 >= var1.size()) {
            return 1.0F;
         } else {
            long var6 = var1.line(var2).timeMs();
            long var8 = var1.line(var5).timeMs() - 200L;
            return var8 <= var6 ? 1.0F : clamp01((float)(var3 - var6) / (float)(var8 - var6));
         }
      } else {
         return 1.0F;
      }
   }

   private float rowFill(SyncedLyrics var1, int var2, String[] var3, int var4, ImFont var5, int var6, long var7) {
      if (var2 < this.lyricCurrentFirst) {
         return 1.0F / 0.0F;
      } else if (var2 > this.lyricCurrentLast) {
         return 0.0F;
      } else {
         String var10 = var3[var4];
         int var11 = 0;
         if (var4 > 0) {
            String var12 = this.lyricTexts[var2];
            var11 = var3[0].length();

            while (var11 < var12.length() && var12.charAt(var11) == ' ') {
               var11++;
            }
         }

         float var9;
         if ((var9 = this.litChars(var1, var2, var7) - var11) <= 0.0F) {
            return 0.0F;
         } else if (var9 >= var10.length()) {
            return 1.0F / 0.0F;
         } else {
            int var16 = (int)var9;
            if (var16 > 0 && Character.isLowSurrogate(var10.charAt(var16))) {
               var16--;
            }

            int var13 = var16 + Character.charCount(var10.codePointAt(var16));
            float var14 = HudChrome.width(var5, var6, var10.substring(0, var16));
            float var15 = HudChrome.width(var5, var6, var10.substring(0, var13));
            return var14 + (var15 - var14) * clamp01((var9 - var16) / (var13 - var16));
         }
      }
   }

   private float litChars(SyncedLyrics var1, int var2, long var3) {
      HudRenderer.LyricWords var5 = this.lyricWords[var2];
      if (var5 == null) {
         this.timeWords(var1, var1.firstWithTimeOf(var2));
         var5 = this.lyricWords[var2];
      }

      float var6 = 0.0F;
      long[] var7 = var5.start();

      for (int var8 = 0; var8 < var7.length && var3 >= var7[var8]; var8++) {
         float var9 = smooth((float)(var3 - var7[var8]) / (float)var5.sweep()[var8]);
         var6 = var5.from()[var8] + (var5.to()[var8] - var5.from()[var8]) * var9;
      }

      return var6;
   }

   private void timeWords(SyncedLyrics var1, int var2) {
      int var3 = var1.lastWithTimeOf(var2);
      int var4 = var3 + 1;
      long var5 = var1.line(var2).timeMs();
      long var7 = var4 < var1.size() ? var1.line(var4).timeMs() - 200L : 9223372036854775807L;
      int var9 = 0;

      for (int var10 = var2; var10 <= var3; var10++) {
         var9 += letterCount(this.lyricTexts[var10]);
      }

      long var28 = var4 < var1.size() ? Math.min(var7 - var5, (long)Math.round(var9 * this.lyricCharMs * 2.0F)) : Math.round(var9 * this.lyricCharMs);
      var28 = Math.max(0L, var28);
      int var12 = 0;

      for (int var13 = var2; var13 <= var3; var13++) {
         String var14 = this.lyricTexts[var13];
         int var15 = var14.isEmpty() ? 0 : var14.length() - letterCount(var14) + 1;
         int[] var16 = new int[var15];
         int[] var17 = new int[var15];
         long[] var18 = new long[var15];
         long[] var19 = new long[var15];
         List var20 = var1.line(var13).wordTimes();
         boolean var21 = var20.size() == var15;
         int var30 = 0;

         for (int var23 = 0; var23 < var15; var23++) {
            var16[var23] = var30;
            if ((var30 = var14.indexOf(32, var30)) < 0) {
               var30 = var14.length();
            }

            var17[var23] = var30++;
            long var24 = var21 ? (Long)var20.get(var23) : var5 + var28 * var12 / var9;
            long var26 = var21
               ? (var23 + 1 < var15 ? (Long)var20.get(var23 + 1) : Math.min(var7, var24 + 180L))
               : var5 + var28 * (var12 += var17[var23] - var16[var23]) / var9;
            var18[var23] = var24;
            var19[var23] = Math.max(1L, Math.min(180L, var26 - var24));
         }

         this.lyricWords[var13] = new HudRenderer.LyricWords(var16, var17, var18, var19);
      }
   }

   private float lyricPace(SyncedLyrics var1) {
      float[] var2 = new float[var1.size()];
      int var3 = 0;
      int var4 = 0;

      while (var4 < var1.size()) {
         int var5 = var1.lastWithTimeOf(var4);
         int var6 = 0;

         for (int var7 = var4; var7 <= var5; var7++) {
            var6 += letterCount(this.lyricTexts[var7]);
         }

         if (var6 > 0 && var5 + 1 < var1.size()) {
            var2[var3++] = (float)(var1.line(var5 + 1).timeMs() - var1.line(var4).timeMs()) / var6;
         }

         var4 = var5 + 1;
      }

      if (var3 == 0) {
         return 90.0F;
      } else {
         Arrays.sort(var2, 0, var3);
         return var2[var3 / 2];
      }
   }

   private static int letterCount(String var0) {
      int var1 = var0.length();

      for (int var2 = 0; var2 < var0.length(); var2++) {
         if (var0.charAt(var2) == ' ') {
            var1--;
         }
      }

      return var1;
   }

   private float lyricViewHeight(SyncedLyrics var1, int var2, HudRenderer.Type var3, float var4, float var5) {
      int var6 = var3.textPx();
      this.prepareRows(var6, var4);
      if (this.lyricViewH >= 0.0F && this.lyricViewLines == var2 && this.lyricViewScale == var5) {
         return this.lyricViewH;
      } else {
         int var7 = var2;
         int var8 = 0;

         while (var8 < var1.size()) {
            int var9 = 0;
            int var10 = var1.lastWithTimeOf(var8);

            for (int var11 = var8; var11 <= var10; var11++) {
               var9 += Math.max(1, this.rowsFor(var11, var3.text(), var6, var4).length);
            }

            var7 = Math.max(var7, Math.min(4, var9));
            var8 = var1.lastWithTimeOf(var8) + 1;
         }

         this.lyricViewH = var7 * var6 + (var7 - 1) * 6.0F * var5;
         this.lyricViewLines = var2;
         this.lyricViewScale = var5;
         return this.lyricViewH;
      }
   }

   private static float lineHeight(String[] var0, int var1, float var2) {
      return var0.length <= 1 ? var1 : var0.length * var1 + (var0.length - 1) * var2;
   }

   private float easeLyricBlock(float var1, float var2) {
      float var3 = var1 - this.lyricBlockH;
      if (Math.abs(var3) < 0.5F) {
         this.lyricBlockH = var1;
      } else {
         float var4 = var3 > 0.0F ? 14.0F : 9.0F;
         this.lyricBlockH = this.lyricBlockH + var3 * (1.0F - (float)Math.exp(-var4 * var2));
      }

      return this.lyricBlockH;
   }

   private void prepareLyrics(Lyrics var1, ImFont var2) {
      if (var1 != this.lyricPrepared) {
         this.lyricPrepared = var1;
         this.lyricViewH = -1.0F;
         SyncedLyrics var3 = var1.synced();
         this.lyricTexts = new String[var3.size()];
         this.lyricRows = new String[var3.size()][];
         this.lyricWords = new HudRenderer.LyricWords[var3.size()];
         int var4 = 0;
         int var5 = 0;
         IntPredicate var6 = var2x -> this.hudHasGlyph(var2, var2x);

         for (int var7 = 0; var7 < var3.size(); var7++) {
            String var8;
            this.lyricTexts[var7] = var8 = DisplayText.fold(var3.line(var7).text(), var6);
            int var9 = 0;

            while (var9 < var8.length()) {
               int var10 = var8.codePointAt(var9);
               var9 += Character.charCount(var10);
               if (Character.isLetter(var10)) {
                  var4++;
                  if (var10 >= 128 && !this.hudHasGlyph(var2, var10)) {
                     var5++;
                  }
               }
            }
         }

         this.lyricReadable = var5 <= var4 * 0.25F;
         this.lyricCharMs = this.lyricPace(var3);
      }
   }

   private boolean hudHasGlyph(ImFont var1, int var2) {
      if (var2 < 128 || var1 == null) {
         return true;
      } else if (var2 > 65535) {
         return false;
      } else {
         if (var1 != this.glyphFont) {
            this.glyphFont = var1;
            this.glyphs.clear();
         }

         Boolean var3;
         if ((var3 = this.glyphs.get(var2)) == null) {
            if (this.glyphs.size() > 1024) {
               this.glyphs.clear();
            }

            var3 = var1.findGlyphNoFallback(var2).isValidPtr();
            this.glyphs.put(var2, var3);
         }

         return var3;
      }
   }

   private void prepareRows(int var1, float var2) {
      int var3 = (int)var2;
      if (var1 != this.lyricRowsPx || var3 != this.lyricRowsWidth) {
         Arrays.fill((Object[])this.lyricRows, null);
         this.lyricRowsPx = var1;
         this.lyricRowsWidth = var3;
         this.lyricViewH = -1.0F;
      }
   }

   private String[] rowsFor(int var1, ImFont var2, int var3, float var4) {
      this.prepareRows(var3, var4);
      String[] var5 = this.lyricRows[var1];
      if (var5 == null) {
         var5 = wrapLyric(var2, var3, this.lyricTexts[var1], this.lyricRowsWidth);
         this.lyricRows[var1] = var5;
      }

      return var5;
   }

   private static String[] wrapLyric(ImFont var0, int var1, String var2, float var3) {
      if (var2.isEmpty()) {
         return NO_ROWS;
      } else if (HudChrome.width(var0, var1, var2) <= var3) {
         return new String[]{var2};
      } else {
         int var5 = -1;
         float var6 = 3.4028235E38F;

         float var4;
         for (int var7 = var2.indexOf(32);
            var7 > 0 && !((var4 = HudChrome.width(var0, var1, var2.substring(0, var7).stripTrailing())) > var3);
            var7 = var2.indexOf(32, var7 + 1)
         ) {
            float var8 = HudChrome.width(var0, var1, var2.substring(var7 + 1).stripLeading());
            if (var8 <= var3 && Math.max(var4, var8) < var6) {
               var6 = Math.max(var4, var8);
               var5 = var7;
            }
         }

         if (var5 > 0) {
            return new String[]{var2.substring(0, var5).stripTrailing(), var2.substring(var5 + 1).stripLeading()};
         } else {
            int var11 = fitPrefix(var0, var1, var2, var3);
            String var9 = var2.substring(0, var11).stripTrailing();
            String var10 = var2.substring(var11).stripLeading();
            return var10.isEmpty()
               ? new String[]{var9}
               : new String[]{var9, HudChrome.width(var0, var1, var10) <= var3 ? var10 : ellipsize(var0, var1, var10, var3)};
         }
      }
   }

   private static int fitPrefix(ImFont var0, int var1, String var2, float var3) {
      int var4 = -1;

      for (int var5 = var2.indexOf(32); var5 > 0 && !(HudChrome.width(var0, var1, var2.substring(0, var5)) > var3); var5 = var2.indexOf(32, var5 + 1)) {
         var4 = var5;
      }

      if (var4 > 0) {
         return var4 + 1;
      } else {
         int var6 = 1;
         int var7 = var2.length();

         while (var6 < var7) {
            int var8 = var6 + var7 + 1 >>> 1;
            if (HudChrome.width(var0, var1, var2.substring(0, var8)) <= var3) {
               var6 = var8;
            } else {
               var7 = var8 - 1;
            }
         }

         if (var6 > 1 && var6 < var2.length() && Character.isHighSurrogate(var2.charAt(var6 - 1))) {
            var6--;
         }

         return var6;
      }
   }

   private void drawLyrics(
      ImDrawList var1,
      HudRenderer.Type var2,
      HudRenderer.LyricView var3,
      HudRenderer.SpotifyColors var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      boolean var11
   ) {
      float var12 = var6 - 12.0F * var9 + Math.max(1.0F, 2.0F * var9);
      if (!(var8 <= var12) && !(var10 <= 0.001F)) {
         float var13 = (float)Math.floor(var5 - 2.0F);
         float var14 = (float)Math.ceil(var5 + var7 + 2.0F);
         var1.pushClipRect(var13, var12, var14, var8, true);
         if (var3.status() != null) {
            float var15 = Math.round(var6 + (var2.textPx() - var2.labelPx()) * 0.5F);
            HudChrome.text(var1, var2.label(), var2.labelPx(), var5, var15, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var10 * 0.85F), var3.status(), var11);
         }

         for (HudRenderer.LyricItem var16 : var3.items()) {
            float var17 = var10 * var16.alpha();
            if (!(var17 <= 0.004F)) {
               float var18 = var6 + var16.top();
               if (var16.text() == null) {
                  float var19 = var18 + (this.capTop(var2.text(), var2.textPx()) + HudChrome.ascent(var2.text(), var2.textPx())) * 0.5F;
                  this.drawLyricBreak(var1, var4, var5, var19, var9, var16.highlight(), var16.progress(), var17);
               } else {
                  int var23 = HudChrome.scaleAlpha(EfnTheme.lerpColor(var4.upcoming(), var4.lyric(), var16.highlight()), var17);
                  int var20 = HudChrome.scaleAlpha(var4.lit(), var17);
                  float var21 = Math.min(var14, (float)Math.floor(var5) + Math.round(var16.fill()));
                  if (var16.fill() <= 0.0F) {
                     HudChrome.text(var1, var2.text(), var2.textPx(), var5, Math.round(var18), var23, var16.text(), var11);
                  } else if (var21 >= var14) {
                     HudChrome.text(var1, var2.text(), var2.textPx(), var5, Math.round(var18), var20, var16.text(), var11);
                  } else {
                     var1.pushClipRect(var13, var12, var21, var8, true);
                     HudChrome.text(var1, var2.text(), var2.textPx(), var5, Math.round(var18), var20, var16.text(), var11);
                     var1.popClipRect();
                     var1.pushClipRect(var21, var12, var14, var8, true);
                     HudChrome.text(var1, var2.text(), var2.textPx(), var5, Math.round(var18), var23, var16.text(), var11);
                     var1.popClipRect();
                  }
               }
            }
         }

         var1.popClipRect();
      }
   }

   private void drawLyricBreak(ImDrawList var1, HudRenderer.SpotifyColors var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = Math.max(1.0F, 1.6F * var5);
      float var10 = Math.max(4.0F, (float)Math.round(6.0F * var5));
      float var11 = 0.5F - 0.5F * (float)Math.cos(this.lyricBreath / 1.6F * 2.0 * 3.141592653589793);

      for (int var12 = 0; var12 < 3; var12++) {
         float var13 = smooth(var7 * 3.0F - var12) * var6;
         int var14 = HudChrome.scaleAlpha(EfnTheme.lerpColor(var2.upcoming(), var2.lit(), var13), var8);
         float var15 = var9 * (1.0F + 0.3F * var13 + 0.12F * var11 * var6);
         var1.addCircleFilled(var3 + var9 + var12 * var10, var4, var15, var14, 12);
      }
   }

   private void foldTrack(NowPlaying var1, ImFont var2) {
      if (var1 != this.spotifyFolded) {
         this.spotifyFolded = var1;
         IntPredicate var3 = var2x -> this.hudHasGlyph(var2, var2x);
         String var4 = DisplayText.fold(var1.title(), var3);
         this.spotifyTitle = var4.isEmpty() ? var1.title() : var4;
         this.spotifyArtist = DisplayText.fold(var1.artist(), var3);
      }
   }

   private float[] marquee(String var1, String var2, float var3, float var4, float var5, boolean var6, boolean var7, float var8, float var9, float var10) {
      if (!var1.equals(this.marqueeTitle) || !var2.equals(this.marqueeArtist)) {
         this.marqueeTitle = var1;
         this.marqueeArtist = var2;
         this.marqueeTime = 0.0F;
      }

      float var11 = 0.0F;
      float var12 = 0.0F;
      float var13 = Math.max(var3 > var5 ? var3 : 0.0F, var4 > var5 ? var4 : 0.0F);
      if (var6 && var13 > 0.0F) {
         float var14 = 30.0F * var9;
         float var15 = 2.4F + (var13 + var8) / var14;
         this.marqueeTime %= var15;
         if (var7 || this.marqueeTime > 2.4F) {
            this.marqueeTime = (this.marqueeTime + var10) % var15;
         }

         float var16 = Math.max(0.0F, this.marqueeTime - 2.4F) * var14;
         if (var3 > var5) {
            var11 = Math.min(var16, var3 + var8);
         }

         if (var4 > var5) {
            var12 = Math.min(var16, var4 + var8);
         }
      } else {
         this.marqueeTime = 0.0F;
      }

      return new float[]{var11, var12};
   }

   private static void marqueeLine(
      ImDrawList var0,
      ImFont var1,
      int var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      boolean var9,
      String var10,
      int var11,
      boolean var12
   ) {
      if (var6 <= var5 + 0.5F) {
         HudChrome.text(var0, var1, var2, var3, var4, var11, var10, var12);
      } else if (!var9) {
         HudChrome.text(var0, var1, var2, var3, var4, var11, ellipsize(var1, var2, var10, var5), var12);
      } else {
         var0.pushClipRect(var3, var4 - 2.0F, var3 + var5, var4 + var2 + 3.0F, true);
         HudChrome.text(var0, var1, var2, var3 - var7, var4, var11, var10, var12);
         if (var7 > 0.0F) {
            HudChrome.text(var0, var1, var2, var3 - var7 + var6 + var8, var4, var11, var10, var12);
         }

         var0.popClipRect();
      }
   }

   private static String ellipsize(ImFont var0, int var1, String var2, float var3) {
      float var4 = var3 - HudChrome.width(var0, var1, "...");
      int var5 = 0;
      int var6 = var2.length();

      while (var5 < var6) {
         int var7 = var5 + var6 + 1 >>> 1;
         if (HudChrome.width(var0, var1, var2.substring(0, var7)) <= var4) {
            var5 = var7;
         } else {
            var6 = var7 - 1;
         }
      }

      return var2.substring(0, var5).stripTrailing() + "...";
   }

   private static String formatSeconds(long var0) {
      long var2 = Math.max(0L, var0);
      long var4 = var2 / 3600L;
      long var6 = var2 / 60L % 60L;
      StringBuilder var8 = new StringBuilder(8);
      if (var4 > 0L) {
         var8.append(var4).append(':').append(var6 < 10L ? "0" : "");
      }

      var8.append(var6).append(':');
      if (var2 % 60L < 10L) {
         var8.append('0');
      }

      return var8.append(var2 % 60L).toString();
   }

   private float capTop(ImFont var1, int var2) {
      if (var1 != null && !(var1.getFontSize() <= 0.0F)) {
         long var3 = (long)System.identityHashCode(var1) << 16 ^ var2;
         Float var5 = this.capTops.get(var3);
         if (var5 != null) {
            return var5;
         } else {
            ImFontGlyph var6 = var1.findGlyphNoFallback(72);
            float var7 = var6.isValidPtr() ? var6.getY0() * var2 / var1.getFontSize() : var2 * 0.2F;
            float var8 = Math.max(0.0F, (float)Math.round(var7));
            if (this.capTops.size() > 64) {
               this.capTops.clear();
            }

            this.capTops.put(var3, var8);
            return var8;
         }
      } else {
         return Math.round(var2 * 0.2F);
      }
   }

   private ThemeImages.Texture spotifyArtTexture(AlbumArt var1) {
      if (var1 == this.spotifyArt) {
         return this.spotifyArtTexture;
      } else {
         this.releaseSpotifyArt();
         if (var1 == null) {
            return null;
         } else {
            this.spotifyArt = var1;
            ByteBuffer var2 = MemoryUtil.memAlloc(var1.byteSize());

            try {
               var1.copyTo(var2);
               var2.flip();
               this.spotifyArtTexture = ThemeImages.fromPixels(var2, var1.size(), var1.size(), false);
            } catch (RuntimeException var7) {
               this.spotifyArtTexture = null;
            } finally {
               MemoryUtil.memFree(var2);
            }

            return this.spotifyArtTexture;
         }
      }
   }

   private void releaseSpotifyArt() {
      if (this.spotifyArtTexture != null) {
         GL11.glDeleteTextures(this.spotifyArtTexture.id());
      }

      this.spotifyArtTexture = null;
      this.spotifyArt = null;
   }

   private List<HudRenderer.PotionRow> buildPotionRows(MinecraftClient var1) {
      if (var1.field_1724 == null) {
         return List.of();
      } else {
         ArrayList var2 = new ArrayList();

         for (StatusEffectInstance var4 : var1.field_1724.method_6026()) {
            RegistryEntry var5 = var4.method_5579();
            String var6 = I18n.method_4662(((StatusEffect)var5.comp_349()).method_5567(), new Object[0]);
            int var7 = var4.method_5578();
            String var8 = var7 > 0 ? toRomanNumeral(var7 + 1) : "";
            int var9 = var4.method_5584();
            if (var9 >= 0 && var9 != 2147483647 && !var4.method_48559()) {
               int var10 = Math.max(0, var9 / 20);
               String var11 = var10 / 60 + ":" + String.format("%02d", var10 % 60);
               var2.add(new HudRenderer.PotionRow(var5, var6, var8, var11, false, var10 < 30, var9));
            } else {
               var2.add(new HudRenderer.PotionRow(var5, var6, var8, "", true, false, 2147483647));
            }
         }

         return var2;
      }
   }

   private static String toRomanNumeral(int var0) {
      return switch (var0) {
         case 1 -> "I";
         case 2 -> "II";
         case 3 -> "III";
         case 4 -> "IV";
         case 5 -> "V";
         case 6 -> "VI";
         case 7 -> "VII";
         case 8 -> "VIII";
         case 9 -> "IX";
         case 10 -> "X";
         default -> String.valueOf(var0);
      };
   }

   private int hudAccent() {
      return this.hudModule != null && this.hudModule.customAccent() ? GuiPalette.abgr(this.hudModule.accentArgb()) : GuiPalette.accent();
   }

   static Surface hudSurface(HudLayoutStore.HudElementState var0) {
      return HudTheme.get().forElementId(var0.id);
   }

   static float hudRounding() {
      return HudTheme.get().rounding.get();
   }

   private void updateEditorInput(float var1, float var2) {
      float var7 = ImGui.getMousePosX();
      float var8 = ImGui.getMousePosY();
      if (this.draggingId != null) {
         if (ImGui.isMouseDown(0)) {
            HudLayoutStore.HudElementState var17 = this.layout.element(this.draggingId);
            float var18 = var7 - this.dragOffsetX;
            float var11 = var8 - this.dragOffsetY;
            HudRenderer.Snap var12 = bestSnap(var18, var17.width, this.snapAnchorsX(this.draggingId, var1));
            HudRenderer.Snap var13 = bestSnap(var11, var17.height, this.snapAnchorsY(this.draggingId, var2));
            this.snapGuideX = var12 != null ? var12.guideAt() : null;
            this.snapGuideY = var13 != null ? var13.guideAt() : null;
            this.layout.moveTo(this.draggingId, var12 != null ? var12.position() : var18, var13 != null ? var13.position() : var11, var1, var2);
         } else {
            this.draggingId = null;
            this.snapGuideX = null;
            this.snapGuideY = null;
            this.saveLayout();
         }
      } else {
         int var6;
         if (this.chatInput && this.spotifyButtonsShown && ImGui.isMouseClicked(0) && (var6 = this.spotifyButtonAt(var7, var8)) >= 0) {
            SpotifyHudModule.hudButton(SPOTIFY_BUTTONS[var6]);
         } else if (!ImGui.isAnyItemHovered()) {
            float var9 = ImGui.getIO().getMouseWheel();
            if (var9 != 0.0F) {
               List var5 = this.layout.orderedElements();

               for (int var4 = var5.size() - 1; var4 >= 0; var4--) {
                  HudLayoutStore.HudElementState var3 = (HudLayoutStore.HudElementState)var5.get(var4);
                  if (contains(var3, var7, var8)) {
                     float var10 = Math.max(0.5F, Math.min(3.0F, var3.scale + var9 * 0.05F));
                     if (var10 != var3.scale) {
                        var3.scale = var10;
                        this.layoutDirty = true;
                        this.lastWheelNanos = System.nanoTime();
                     }
                     break;
                  }
               }
            }

            if (ImGui.isMouseClicked(0)) {
               List var16 = this.layout.orderedElements();

               for (int var15 = var16.size() - 1; var15 >= 0; var15--) {
                  HudLayoutStore.HudElementState var14 = (HudLayoutStore.HudElementState)var16.get(var15);
                  if (contains(var14, var7, var8)) {
                     this.draggingId = var14.id;
                     this.dragOffsetX = var7 - var14.x;
                     this.dragOffsetY = var8 - var14.y;
                     break;
                  }
               }
            }
         }
      }
   }

   private List<Float> snapAnchorsX(String var1, float var2) {
      ArrayList var3 = new ArrayList<>(List.of(0.0F, var2, var2 * 0.5F));

      for (HudLayoutStore.HudElementState var5 : this.layout.orderedElements()) {
         if (!var5.id.equals(var1) && var5.positioned) {
            var3.add(var5.x);
            var3.add(var5.x + var5.width);
            var3.add(var5.x + var5.width * 0.5F);
         }
      }

      return var3;
   }

   private List<Float> snapAnchorsY(String var1, float var2) {
      ArrayList var3 = new ArrayList<>(List.of(0.0F, var2, var2 * 0.5F));

      for (HudLayoutStore.HudElementState var5 : this.layout.orderedElements()) {
         if (!var5.id.equals(var1) && var5.positioned) {
            var3.add(var5.y);
            var3.add(var5.y + var5.height);
            var3.add(var5.y + var5.height * 0.5F);
         }
      }

      return var3;
   }

   private static HudRenderer.Snap bestSnap(float var0, float var1, List<Float> var2) {
      float[] var3 = new float[]{0.0F, var1 * 0.5F, var1};
      float var4 = 6.0F;
      HudRenderer.Snap var5 = null;

      for (float var7 : var2) {
         for (float var11 : var3) {
            float var12 = Math.abs(var0 + var11 - var7);
            if (var12 < var4) {
               var4 = var12;
               var5 = new HudRenderer.Snap(var7 - var11, var7);
            }
         }
      }

      return var5;
   }

   private void drawSnapGuides(ImDrawList var1, float var2, float var3) {
      if (this.draggingId != null) {
         int var4 = HudChrome.scaleAlpha(this.hudAccent(), 0.8F);
         if (this.snapGuideX != null) {
            var1.addLine(this.snapGuideX, 0.0F, this.snapGuideX, var3, var4, 1.0F);
         }

         if (this.snapGuideY != null) {
            var1.addLine(0.0F, this.snapGuideY, var2, this.snapGuideY, var4, 1.0F);
         }
      }
   }

   private void drawEditorChrome(ImDrawList var1, float var2, float var3) {
      float var4 = ImGui.getMousePosX();
      float var5 = ImGui.getMousePosY();
      float var6 = ImGuiManager.getInstance().uiScale();
      ImFont var7 = ImGuiManager.getInstance().hudLabelFont();
      int var8 = HudChrome.px(14.0F, var6);
      this.drawSnapGuides(var1, var2, var3);
      float var9 = Math.round(Math.max(2.0F, 3.0F * var6));
      List var10 = this.layout.orderedElements();
      ArrayList var11 = new ArrayList(var10.size());

      for (HudLayoutStore.HudElementState var13 : var10) {
         boolean var14 = var13.id.equals(this.draggingId);
         boolean var15 = contains(var13, var4, var5);
         float var16 = var14 ? 1.0F : (var15 ? 0.85F : 0.4F);
         float var17 = Math.round(var13.x);
         float var18 = Math.round(var13.y);
         var1.addRect(
            var17 - var9,
            var18 - var9,
            var17 + var13.width + var9,
            var18 + var13.height + var9,
            HudChrome.scaleAlpha(this.hudAccent(), var16),
            hudRounding() * scaleOf(var13) + var9,
            0,
            var14 ? 1.5F : 1.0F
         );
         this.drawEditorLabel(var1, var7, var8, var6, var13, var14 || var15, var10, var11, var2, var3);
      }

      drawEditorHint(var1, var7, var8, var6, var2, var3);
   }

   private static void drawEditorHint(ImDrawList var0, ImFont var1, int var2, float var3, float var4, float var5) {
      String var6 = "Drag to move  \u00b7  Scroll to resize  \u00b7  Click the eye to hide";
      float var7 = Math.round(HudChrome.width(var1, var2, var6) + 12.0F * var3 * 2.0F);
      float var8 = Math.round(28.0F * var3);
      float var9 = Math.round((var4 - var7) * 0.5F);
      float var10 = Math.round(var5 - var8 - 96.0F * var3);
      float var11 = hudRounding() * var3;
      var0.addRectFilled(var9, var10, var9 + var7, var10 + var8, HudChrome.CHIP_FILL, var11);
      var0.addRect(var9, var10, var9 + var7, var10 + var8, 402653183, var11, 0, 1.0F);
      HudChrome.text(var0, var1, var2, var9 + 12.0F * var3, var10 + (float)Math.floor((var8 - var2) * 0.5F), HudChrome.TEXT_LABEL, var6, false);
   }

   private void drawEditorLabel(
      ImDrawList var1,
      ImFont var2,
      int var3,
      float var4,
      HudLayoutStore.HudElementState var5,
      boolean var6,
      List<HudLayoutStore.HudElementState> var7,
      List<float[]> var8,
      float var9,
      float var10
   ) {
      String var11 = editorLabel(var5.id);
      float var12 = var6 ? 1.0F : 0.6F;
      float var13 = 8.0F * var4;
      float var14 = 12.0F * var4;
      float var15 = Math.round(20.0F * var4);
      float var16 = HudChrome.width(var2, var3, var11);
      float var17 = Math.round(var13 + var16 + 8.0F * var4 + var14 + var13);
      float[] var18 = placeChip(var5, var17, var15, 7.0F * var4, var7, var8, var9, var10);
      var8.add(var18);
      float var19 = var18[0];
      float var20 = var18[1];
      float var21 = 6.0F * var4;
      var1.addRectFilled(var19, var20, var19 + var17, var20 + var15, HudChrome.scaleAlpha(HudChrome.CHIP_FILL, var12), var21);
      var1.addRect(var19, var20, var19 + var17, var20 + var15, HudChrome.scaleAlpha(402653183, var12), var21, 0, 1.0F);
      int var22 = HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, var12);
      HudChrome.text(var1, var2, var3, var19 + var13, var20 + (float)Math.floor((var15 - var3) * 0.5F), var22, var11, false);
      float var23 = var19 + var13 + var16 + 8.0F * var4;
      float var24 = var20 + (var15 - var14) * 0.5F;
      ImGui.setCursorPos(var23, var24);
      if (ImGui.invisibleButton("##vis_" + var5.id, var14, var14)) {
         var5.visible = !var5.visible;
         this.saveLayout();
      }

      boolean var25 = ImGui.isItemHovered();
      float var26 = var23 + var14 * 0.5F;
      float var27 = var24 + var14 * 0.5F;
      float var28 = var14 * 0.34F;
      int var29 = var25 ? HudChrome.TEXT_VALUE : HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var5.visible ? var12 : var12 * 0.6F);
      EfnWidgets.eye(var1, var26, var27, var28, var29);
      if (!var5.visible) {
         var1.addLine(var26 - var28, var27 - var28, var26 + var28, var27 + var28, var29, Math.max(1.0F, 1.4F * var4));
      }
   }

   private static float[] placeChip(
      HudLayoutStore.HudElementState var0,
      float var1,
      float var2,
      float var3,
      List<HudLayoutStore.HudElementState> var4,
      List<float[]> var5,
      float var6,
      float var7
   ) {
      float var9 = 4.0F;
      float var10 = var0.x;
      float var11 = var0.x + var0.width - var1;
      float var12 = alignsRight(var0, var6) ? var11 : var10;
      float var13 = var0.y + (var0.height - var2) * 0.5F;

      float[][] var8;
      for (float[] var17 : var8 = new float[][]{
         {var12, var0.y - var3 - var2},
         {var12, var0.y + var0.height + var3},
         {var0.x + var0.width + var3, Math.min(var13, var0.y)},
         {var0.x - var3 - var1, Math.min(var13, var0.y)}
      }) {
         float var18 = Math.round(var17[0]);
         float var19 = Math.round(var17[1]);
         if (!(var18 < var9)
            && !(var19 < var9)
            && !(var18 + var1 > var6 - var9)
            && !(var19 + var2 > var7 - var9)
            && !overlapsAny(var18, var19, var1, var2, var0, var4, var5, var3)) {
            return new float[]{var18, var19, var1, var2};
         }
      }

      float var20 = Math.round(Math.max(var9, Math.min(var12, var6 - var9 - var1)));
      float var21 = var8[0][1] >= var9 ? var8[0][1] : var8[1][1];
      return new float[]{var20, Math.round(Math.max(var9, Math.min(var21, var7 - var9 - var2))), var1, var2};
   }

   private static boolean overlapsAny(
      float var0,
      float var1,
      float var2,
      float var3,
      HudLayoutStore.HudElementState var4,
      List<HudLayoutStore.HudElementState> var5,
      List<float[]> var6,
      float var7
   ) {
      for (HudLayoutStore.HudElementState var9 : var5) {
         if (var9 != var4
            && var9.positioned
            && overlaps(var0, var1, var2, var3, var9.x - var7 * 0.5F, var9.y - var7 * 0.5F, var9.width + var7, var9.height + var7)) {
            return true;
         }
      }

      for (float[] var11 : var6) {
         if (overlaps(var0, var1, var2, var3, var11[0], var11[1], var11[2], var11[3])) {
            return true;
         }
      }

      return false;
   }

   private static boolean overlaps(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      return var0 < var4 + var6 && var4 < var0 + var2 && var1 < var5 + var7 && var5 < var1 + var3;
   }

   private static boolean contains(HudLayoutStore.HudElementState var0, float var1, float var2) {
      return !var0.positioned ? false : var1 >= var0.x && var1 <= var0.x + var0.width && var2 >= var0.y && var2 <= var0.y + var0.height;
   }

   static String editorLabel(String var0) {
      return switch (var0) {
         case "info_bar" -> "Info Bar";
         case "coordinates" -> "Coordinates";
         case "module_list" -> "Module List";
         case "potion_tracker" -> "Potions";
         case "region_map" -> "Region Map";
         case "anti_vanish" -> "Anti Vanish";
         case "staff_list" -> "Staff List";
         case "spotify" -> "Spotify";
         case "radar" -> "Radar";
         case "hotkeys" -> "Hotkeys";
         case "target_card" -> "Target HUD";
         case "armor_status" -> "Armor HUD";
         case "item_cooldowns" -> "Cooldown";
         default -> var0;
      };
   }

   private static int readTabLatency(MinecraftClient var0, ClientPlayNetworkHandler var1) {
      PlayerListEntry var2 = var1.method_2871(var1.method_2879().id());
      if (var2 == null) {
         var2 = var1.method_2871(var0.field_1724.method_5667());
      }

      if (var2 == null) {
         var2 = var1.method_73471(var1.method_2879().name());
      }

      if (var2 == null) {
         var2 = var1.method_73471(var0.field_1724.method_5477().getString());
      }

      return var2 == null ? 0 : Math.max(0, var2.method_2959());
   }

   private static int readServerPing(ClientPlayNetworkHandler var0) {
      ServerInfo var1 = var0.method_45734();
      return var1 != null && var1.field_3758 > 0L && var1.field_3758 <= 60000L ? (int)var1.field_3758 : 0;
   }

   private static float easeOutCubic(float var0) {
      float var1 = clamp01(var0);
      float var2 = 1.0F - var1;
      return 1.0F - var2 * var2 * var2;
   }

   private static float smooth(float var0) {
      float var1 = clamp01(var0);
      return var1 * var1 * (3.0F - 2.0F * var1);
   }

   private static float clamp01(float var0) {
      return Math.max(0.0F, Math.min(1.0F, var0));
   }

   private static float elementScale(HudLayoutStore.HudElementState var0) {
      return !Float.isNaN(var0.scale) && !Float.isInfinite(var0.scale) ? Math.max(0.5F, Math.min(3.0F, var0.scale)) : 1.0F;
   }

   static float scaleOf(HudLayoutStore.HudElementState var0) {
      return elementScale(var0) * ImGuiManager.getInstance().uiScale();
   }

   @Environment(EnvType.CLIENT)
   private record CoordPlan(List<HudRenderer.CoordRow> rows, HudRenderer.Type type, float[] slotW, float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   private record CoordRow(String block, boolean here, boolean enabled, String[] values, float anim) {
   }

   @Environment(EnvType.CLIENT)
   private record DrawTransform(float xOffset, float yOffset, float alpha) {
      private static HudRenderer.DrawTransform identity() {
         return new HudRenderer.DrawTransform(0.0F, 0.0F, 1.0F);
      }

      private HudRenderer.DrawTransform down(float var1) {
         return var1 == 0.0F ? this : new HudRenderer.DrawTransform(this.xOffset, this.yOffset + var1, this.alpha);
      }
   }

   @Environment(EnvType.CLIENT)
   private record HotkeyLine(String name, String key, boolean on, float nameW, float keyW) {
   }

   @Environment(EnvType.CLIENT)
   private record HotkeyPlan(List<HudRenderer.HotkeyLine> lines, HudRenderer.Type type, float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   private record InfoItem(String slot, String value, String dim, String unit) {
   }

   @Environment(EnvType.CLIENT)
   private record InfoPlan(List<HudRenderer.Segment> segments, HudRenderer.Type type, float width, float height, float emblemW) {
   }

   @Environment(EnvType.CLIENT)
   private record ListPlan(List<ModuleListRows.Row> rows, HudRenderer.Type type, float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   private record LyricItem(String text, float top, float highlight, float alpha, float progress, float fill) {
   }

   @Environment(EnvType.CLIENT)
   private record LyricShift(float distance, float at, int start, float stagger) {
   }

   @Environment(EnvType.CLIENT)
   private record LyricView(List<HudRenderer.LyricItem> items, String status, float height) {
   }

   @Environment(EnvType.CLIENT)
   private record LyricWords(int[] from, int[] to, long[] start, long[] sweep) {
   }

   @Environment(EnvType.CLIENT)
   private record PotionLine(HudRenderer.PotionRow row, float nameW, float ampW, float durationW) {
   }

   @Environment(EnvType.CLIENT)
   private record PotionPlan(List<HudRenderer.PotionLine> lines, HudRenderer.Type type, float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   private record PotionRow(
      RegistryEntry<StatusEffect> effect, String name, String amplifier, String duration, boolean infinite, boolean expiringSoon, int ticks
   ) {
   }

   @Environment(EnvType.CLIENT)
   private record Segment(HudRenderer.InfoItem item, float valueW, float dimW, float contentW, float slotW) {
   }

   @Environment(EnvType.CLIENT)
   private record Snap(float position, float guideAt) {
   }

   @Environment(EnvType.CLIENT)
   private record SpotifyCard(
      NowPlaying track,
      boolean sample,
      boolean art,
      float header,
      float textTop,
      float column,
      boolean timeline,
      String elapsed,
      String remaining,
      float progress,
      boolean progressBar,
      HudRenderer.LyricView lyrics,
      float lyricAlpha
   ) {
   }

   @Environment(EnvType.CLIENT)
   private record SpotifyColors(int title, int artist, int lyric, int upcoming, int lit) {
      static final HudRenderer.SpotifyColors DEFAULT = new HudRenderer.SpotifyColors(
         HudChrome.TEXT_NAME, HudChrome.TEXT_LABEL, HudChrome.TEXT_VALUE, HudChrome.TEXT_LABEL, HudChrome.TEXT_VALUE
      );

      static HudRenderer.SpotifyColors of(SpotifyHudModule var0) {
         if (var0 == null) {
            return DEFAULT;
         } else {
            SpotifyHudModule.LyricHighlight var1 = var0.lyricHighlight();
            int var2 = GuiPalette.abgr(var0.lyricsColor());
            int var3 = GuiPalette.abgr(var0.highlightColor());
            return new HudRenderer.SpotifyColors(
               GuiPalette.abgr(var0.titleColor()),
               GuiPalette.abgr(var0.artistColor()),
               var1 == SpotifyHudModule.LyricHighlight.LINE ? var3 : var2,
               GuiPalette.abgr(var0.upcomingLyricsColor()),
               var1 == SpotifyHudModule.LyricHighlight.OFF ? var2 : var3
            );
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum SpotifyGlyph {
      NONE,
      PLAYING,
      PAUSED;

      private SpotifyGlyph() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum SpotifyLook {
      HIDDEN,
      IDLE,
      COMPACT,
      CARD;

      private SpotifyLook() {
      }
   }

   @Environment(EnvType.CLIENT)
   private record SpotifyPlan(
      HudRenderer.Type type,
      HudRenderer.SpotifyLook look,
      HudRenderer.SpotifyGlyph glyph,
      String title,
      String artist,
      float titleW,
      float artistW,
      float titleOffset,
      float artistOffset,
      float loopGap,
      boolean scroll,
      HudRenderer.SpotifyCard card,
      HudRenderer.SpotifyColors colors,
      float width,
      float height
   ) {
   }

   @Environment(EnvType.CLIENT)
   private record StaffLine(
      String name,
      int face,
      String state,
      boolean more,
      float nameW,
      float stateW,
      boolean server,
      boolean nearby,
      float alert,
      String note,
      float noteW,
      String value,
      float valueW,
      float serverTagW
   ) {
      static HudRenderer.StaffLine plain(String var0, int var1, String var2, boolean var3, float var4, float var5) {
         return new HudRenderer.StaffLine(var0, var1, var2, var3, var4, var5, false, false, 0.0F, "", 0.0F, "", 0.0F, 0.0F);
      }
   }

   @Environment(EnvType.CLIENT)
   private record StaffPlan(List<HudRenderer.StaffLine> lines, HudRenderer.Type type, String count, float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   private record Step(int item, float l, float t, float r, float b, float alpha) {
   }

   @Environment(EnvType.CLIENT)
   private record Type(ImFont value, int valuePx, ImFont text, int textPx, ImFont label, int labelPx) {
      static HudRenderer.Type of(float var0) {
         ImGuiManager var1 = ImGuiManager.getInstance();
         return new HudRenderer.Type(
            var1.hudValueFont(), HudChrome.px(18.0F, var0), var1.hudTextFont(), HudChrome.px(17.0F, var0), var1.hudLabelFont(), HudChrome.px(14.0F, var0)
         );
      }
   }

   @Environment(EnvType.CLIENT)
   private record VanishLine(String tag, String value, boolean urgent, float tagW, float valueW) {
   }

   @Environment(EnvType.CLIENT)
   private record VanishPlan(List<HudRenderer.VanishLine> lines, HudRenderer.Type type, float tagColumn, float width, float height) {
   }
}
