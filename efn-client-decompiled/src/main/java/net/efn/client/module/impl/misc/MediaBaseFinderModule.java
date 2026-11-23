package net.efn.client.module.impl.misc;

import imgui.ImDrawList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.HudPanel;
import net.efn.client.gui.ScreenText;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.NameFilter;
import net.efn.client.util.donut.DonutGoliaths;
import net.efn.client.util.staff.ShardTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class MediaBaseFinderModule extends ClientModule implements HudOverlayProvider {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final String MEDIA_PREFIX = "\ud83d\udcf9";
   private static final String MEDIA_TEAM = "09-";
   private static final int SETTLE_TICKS = 60;
   private static final int SCAN_TICKS = 10;
   private static final int MAX_LISTED = 8;
   private static final long RTP_WINDOW_NANOS = 60000000000L;
   private final BooleanSetting reportEmpty = this.addSetting("report-empty", new BooleanSetting("Report Empty Goliaths", true));
   private final BooleanSetting statusBox = this.addSetting("status-box", new BooleanSetting("Status Box", true));
   private final NumberSetting scale = this.addSetting("scale", new NumberSetting("Scale", 1.0F, 0.5F, 3.0F).step(0.05F));
   private final NumberSetting opacity = this.addSetting("opacity", HudPanel.opacitySetting());
   private final HudPanel panel = new HudPanel(
      this.addSetting("pos-x", HudPanel.coordinate("X")), this.addSetting("pos-y", HudPanel.coordinate("Y")), this.scale, this.opacity
   );
   private final ShardTracker shard = new ShardTracker();
   private final ScreenText screenText = new ScreenText(16733695, 3.0F);
   private final Queue<UUID> modeUpdates = new ConcurrentLinkedQueue<>();
   private final Set<String> proven = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
   private Set<String> here = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
   private String goliath = "\u2014";
   private int settle;
   private int ticks;
   private boolean reported;
   private boolean afterRtp;
   private volatile long rtpAt;

   public MediaBaseFinderModule() {
      super(
         "media-base-finder",
         "Media Base Finder",
         "Tells you which DonutSMP media are on the goliath you land on (after /rtp), where their base most likely is.",
         ModuleCategory.BASE_RAID
      );
      this.usesNotifications();
      this.scale.visibleWhen(this.statusBox::get);
      this.opacity.visibleWhen(this.statusBox::get);
   }

   @Override
   protected void onEnable() {
      this.arrive(MinecraftClient.getInstance());
   }

   @Override
   protected void onDisable() {
      this.shard.clear();
      this.modeUpdates.clear();
      this.proven.clear();
      this.here = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
   }

   private void arrive(MinecraftClient minecraftClient) {
      this.modeUpdates.clear();
      this.proven.clear();
      this.here = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      this.settle = 60;
      this.reported = false;
      this.goliath = label(minecraftClient);
      long var2 = this.rtpAt;
      this.afterRtp = var2 != 0L && System.nanoTime() - var2 < 60000000000L;
   }

   @EventHandler
   public void onPacketSend(PacketSendEvent packetSendEvent) {
      String var2;
      if (packetSendEvent.packet instanceof CommandExecutionC2SPacket var4) {
         var2 = var4.command();
      } else if (packetSendEvent.packet instanceof ChatCommandSignedC2SPacket var6) {
         var2 = var6.command();
      } else {
         var2 = null;
      }

      if (var2 != null) {
         String var7 = var2.trim().toLowerCase(Locale.ROOT);
         if (var7.equals("rtp") || var7.startsWith("rtp ")) {
            this.rtpAt = System.nanoTime();
         }
      }
   }

   @EventHandler(
      stage = Stage.POST
   )
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayNetworkHandler var3 = var2.getNetworkHandler();
      ClientPlayerEntity var4 = var2.player;
      if (var3 != null && var4 != null && var2.world != null) {
         if (this.shard.moved(var2)) {
            this.arrive(var2);
         }

         this.drainModeUpdates(var3);
         if (this.settle > 0) {
            this.settle--;
         } else if (!this.reported || ++this.ticks % 10 == 0) {
            Set var5 = this.scan(var2, var3, var4);
            this.goliath = label(var2);
            if (!this.reported) {
               this.reported = true;
               this.here = var5;
               this.report(var5);
            } else {
               ArrayList var6 = new ArrayList();

               for (String var8 : var5) {
                  if (!this.here.contains(var8)) {
                     var6.add(var8);
                  }
               }

               this.here = var5;
               if (!var6.isEmpty()) {
                  this.announce(var6, var5.size(), false);
               }
            }
         }
      } else {
         this.shard.clear();
      }
   }

   private Set<String> scan(MinecraftClient minecraftClient, ClientPlayNetworkHandler clientPlayNetworkHandler, ClientPlayerEntity clientPlayerEntity) {
      TreeSet var4 = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      String var5 = clientPlayerEntity.getGameProfile().name();

      for (Team var7 : minecraftClient.world.getScoreboard().getTeams()) {
         if (isMedia(var7)) {
            for (String var9 : var7.getPlayerList()) {
               if (!var9.equalsIgnoreCase(var5) && (clientPlayNetworkHandler.getPlayerListEntry(var9) == null || this.proven.contains(var9))) {
                  var4.add(var9);
               }
            }
         }
      }

      return var4;
   }

   private static boolean isMedia(Team team) {
      return team.getName().startsWith("09-") || team.getPrefix().getString().contains("\ud83d\udcf9");
   }

   @EventHandler
   public void onPacketReceiveNetty(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      this.collect(packetReceiveNettyEvent.packet);
   }

   private void collect(Packet<?> packet) {
      if (!(packet instanceof BundleS2CPacket var2)) {
         if (packet instanceof PlayerListS2CPacket var5) {
            if (var5.getActions().contains(Action.UPDATE_GAME_MODE) && !var5.getActions().contains(Action.ADD_PLAYER)) {
               for (Entry var7 : var5.getEntries()) {
                  if (var7 != null && var7.profileId() != null) {
                     this.modeUpdates.offer(var7.profileId());
                  }
               }
            }
         }
      } else {
         for (Packet var4 : var2.getPackets()) {
            this.collect(var4);
         }
      }
   }

   private void drainModeUpdates(ClientPlayNetworkHandler clientPlayNetworkHandler) {
      UUID var2;
      while ((var2 = this.modeUpdates.poll()) != null) {
         PlayerListEntry var4 = clientPlayNetworkHandler.getPlayerListEntry(var2);
         String var3;
         if (var4 != null && var4.getScoreboardTeam() != null && isMedia(var4.getScoreboardTeam()) && this.proven.add(var3 = var4.getProfile().name())) {
            LOGGER.info("[Media Base Finder] game mode update places {} on {}", var3, this.goliath);
         }
      }
   }

   private void report(Set<String> set) {
      this.rtpAt = 0L;
      LOGGER.info("[Media Base Finder] {}: {} media {}", this.goliath, set.size(), set);
      if (set.isEmpty()) {
         if (this.reportEmpty.get()) {
            ModuleChat.send(this, ModuleChat.body().text("No media on ").name(this.goliath).text("."));
         }
      } else {
         this.announce(new ArrayList<>(set), set.size(), true);
      }
   }

   private void announce(List<String> list, int i, boolean bl) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      String var5 = names(list);
      String var6 = area(var4);
      String var7 = i + (i == 1 ? " media base" : " media bases") + (var6 == null ? "" : " within " + var6 + " blocks");
      if (bl) {
         ModuleChat.send(this, ModuleChat.body().text(var7 + " on ").name(this.goliath).text(": ").name(var5));
      } else {
         ModuleChat.send(this, ModuleChat.body().name(var5).text(" joined ").name(this.goliath).text(", now " + var7 + "."));
         LOGGER.info("[Media Base Finder] {} joined {}, now {}", var5, this.goliath, i);
      }

      if (bl) {
         LOGGER.info("[Media Base Finder] screen text: afterRtp={} notifications={}", this.afterRtp, this.notificationsOn());
      }

      if (bl && this.afterRtp && this.notificationsOn()) {
         this.screenText.show(i + (i == 1 ? " Media Base" : " Media Bases"), var6 == null ? "on this goliath" : "within " + var6 + " blocks");
         var4.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.4F));
      }
   }

   private static String area(MinecraftClient minecraftClient) {
      if (minecraftClient.player != null && minecraftClient.world != null && minecraftClient.world.getRegistryKey() == World.OVERWORLD) {
         DonutGoliaths.Goliath var2 = DonutGoliaths.at(minecraftClient.player.getX(), minecraftClient.player.getZ());
         if (var2 == null) {
            return null;
         } else {
            int[] var3 = DonutGoliaths.CELLS[var2.region()][var2.index()];
            String var4 = blocks(var3[2] - var3[0]);
            String var1;
            return var4.equals(var1 = blocks(var3[3] - var3[1])) ? var4 : var4 + " \u00d7 " + var1;
         }
      } else {
         return null;
      }
   }

   private static String blocks(int i) {
      int var1 = i * 12500;
      return var1 % 1000 == 0 ? var1 / 1000 + "k" : String.format(Locale.ROOT, "%.1fk", var1 / 1000.0);
   }

   private static String names(List<String> list) {
      StringBuilder var1 = new StringBuilder();
      int var2 = Math.min(list.size(), 8);

      for (int var3 = 0; var3 < var2; var3++) {
         var1.append(var3 == 0 ? "" : ", ").append(NameFilter.apply((String)list.get(var3)));
      }

      if (list.size() > var2) {
         var1.append(" +").append(list.size() - var2);
      }

      return var1.toString();
   }

   private static String label(MinecraftClient minecraftClient) {
      if (minecraftClient.player != null && minecraftClient.world != null && minecraftClient.world.getRegistryKey() == World.OVERWORLD) {
         DonutGoliaths.Goliath var1 = DonutGoliaths.at(minecraftClient.player.getX(), minecraftClient.player.getZ());
         return var1 == null ? "Off the map" : var1.label();
      } else {
         return "\u2014";
      }
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.statusBox.get() && MinecraftClient.getInstance().player != null;
   }

   @Override
   public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
      ArrayList var4 = new ArrayList();
      if (this.settle > 0) {
         var4.add(new HudPanel.Line("Checking\u2026", -19648));
      } else if (this.here.isEmpty()) {
         var4.add(new HudPanel.Line("None here", -6052949));
      } else {
         int var5 = 0;

         for (String var7 : this.here) {
            if (var5++ == 8) {
               var4.add(new HudPanel.Line("+" + (this.here.size() - 8) + " more", -6052949));
               break;
            }

            var4.add(new HudPanel.Line(HudPanel.face(var7), NameFilter.apply(var7), -1, "", -6052949));
         }
      }

      String var8 = this.here.size() > 1 ? "MEDIA BASES ON YOUR GOLIATH" : "MEDIA BASE ON YOUR GOLIATH";
      this.panel.card(imDrawList, var8, "", -1, var4, "staff_list", f, g, 1.0F, 220.0F);
   }

   @EventHandler
   public void onRender2D(Render2DEvent render2DEvent) {
      this.screenText.render(render2DEvent.graphics);
   }
}
