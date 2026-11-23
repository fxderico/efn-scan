package net.efn.client.module.impl.misc;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.ListenerHandle;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ChallengeToast;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.playerbypass.AutoRtpLoop;
import net.efn.client.util.playerbypass.ChunkLightSnapshot;
import net.efn.client.util.playerbypass.PlayerBypassLightTracker;
import net.efn.client.util.playerbypass.RtpReply;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
final class PlayerBypassAutoRtp {
   private static final double TELEPORT_DISTANCE_SQ = 256.0;
   private static final long REPLY_WINDOW_MS = 5000L;
   private static final long STOP_REPLY_WINDOW_MS = 2500L;
   private static final long RESUME_GRACE_MS = 1000L;
   private static final long RESPAWN_GRACE_MS = 3000L;
   private static final long OFFLINE_LIMIT_MS = 30000L;
   private static final long MENU_CLICK_DELAY_MS = 300L;
   private static final long MENU_FILL_MS = 2500L;
   private static final long MENU_CLAIM_MS = 4000L;
   private static final int MAX_MENU_CLICKS = 3;
   private static final int MENU_NAMES_SHOWN = 10;
   private static final long LONG_COOLDOWN_MS = 60000L;
   private static final long BAR_REFRESH_MS = 2000L;
   private static final long NO_CHUNK = -9223372036854775808L;
   private static final long NEVER = -9223372036854775808L;
   private final PlayerBypassModule module;
   private final BooleanSetting enabled;
   private final StringSetting command;
   private final NumberSetting scanTime;
   private final NumberSetting delay;
   private final NumberSetting timeout;
   private final NumberSetting maxRtps;
   private final BooleanSetting stopOnSuspect;
   private final StringSetting menuItem;
   private final BooleanSetting sound;
   private final BooleanSetting showCoordinates;
   private final AutoRtpLoop loop = new AutoRtpLoop();
   private final LongOpenHashSet knownSuspects = new LongOpenHashSet();
   private ListenerHandle<TickEvent> tickHandle;
   private ListenerHandle<PacketReceiveEvent> packetHandle;
   private ListenerHandle<PacketSendEvent> sendHandle;
   private ClientConnection link;
   private ClientWorld level;
   private Vec3d anchor;
   private int startChunkX;
   private int startChunkZ;
   private boolean wasDead;
   private long offlineSince;
   private long lastRevision;
   private boolean lastTryLanded;
   private long commandSentAt = -9223372036854775808L;
   private boolean sendingCommand;
   private volatile long userActionAt = -9223372036854775808L;
   private int menuId = -1;
   private int ignoredMenuId = -1;
   private long menuSeenAt;
   private boolean menuClicked;
   private int menuClicks;
   private long lastMenuClickAt;
   private long noticedCooldownUntil;
   private String lastBar = "";
   private long lastBarAt;

   PlayerBypassAutoRtp(
      PlayerBypassModule var1,
      BooleanSetting var2,
      StringSetting var3,
      NumberSetting var4,
      NumberSetting var5,
      NumberSetting var6,
      NumberSetting var7,
      BooleanSetting var8,
      StringSetting var9,
      BooleanSetting var10,
      BooleanSetting var11
   ) {
      this.module = var1;
      this.enabled = var2;
      this.command = var3;
      this.scanTime = var4;
      this.delay = var5;
      this.timeout = var6;
      this.maxRtps = var7;
      this.stopOnSuspect = var8;
      this.menuItem = var9;
      this.sound = var10;
      this.showCoordinates = var11;
   }

   void start() {
      if (!this.loop.running()) {
         MinecraftClient var3 = MinecraftClient.method_1551();
         ClientPlayerEntity var4 = var3.field_1724;
         ClientPlayNetworkHandler var5 = var3.method_1562();
         if (var4 != null && var3.field_1687 != null && var5 != null && var5.method_48296().method_10758()) {
            long var6 = System.currentTimeMillis();
            AutoRtpLoop.Config var8 = this.config();
            this.loop.start(var6, var8);
            this.link = var5.method_48296();
            this.level = var3.field_1687;
            this.anchor = var4.method_73189();
            this.startChunkX = var4.method_24515().method_10263() >> 4;
            this.startChunkZ = var4.method_24515().method_10260() >> 4;
            this.wasDead = var4.method_29504();
            this.offlineSince = 0L;
            this.lastRevision = PlayerBypassLightTracker.visibilityRevision();
            this.lastTryLanded = false;
            this.noticedCooldownUntil = 0L;
            this.lastBar = "";
            this.resetMenu();
            this.rememberSuspects();
            if (this.tickHandle == null) {
               this.tickHandle = TickEvent.TYPE.listen(this, var1x -> this.tick());
            }

            if (this.packetHandle == null) {
               this.packetHandle = PacketReceiveEvent.TYPE.listen(this, var1x -> this.onPacket(var1x.packet));
            }

            if (this.sendHandle == null) {
               this.sendHandle = PacketSendEvent.TYPE.listen(this, var1x -> this.onPacketSent(var1x.packet));
            }

            ModuleChat.Body var9 = ModuleChat.body()
               .good("Auto RTP started")
               .text(": ")
               .value("/" + this.command())
               .text(", ")
               .value(seconds(var8.scanMs()))
               .text(" scan per spot, at least ")
               .value(seconds(var8.delayMs()))
               .text(" between RTPs");
            if (var8.maxRtps() > 0) {
               var9.text(", max ").value(var8.maxRtps());
            }

            long var1;
            if ((var1 = this.loop.cooldownLeft(var6)) > 0L) {
               this.noticedCooldownUntil = var6 + var1;
               var9.text(", server cooldown ").value(seconds(var1)).text(" left");
               if (var1 >= 60000L) {
                  var9.text(" (turn Player Bypass off and on to clear it)");
               }
            }

            this.say(var9.text("."));
         } else {
            this.say(ModuleChat.body().warn("Join a server first").text(", Auto RTP is off."));
            this.enabled.set(false);
         }
      }
   }

   void stopByUser() {
      if (!this.loop.running()) {
         this.unlisten();
      } else {
         this.stop(ModuleChat.body().text("Auto RTP stopped after ").value(this.loop.landings()).text(rtpWord(this.loop.landings())).text("."));
      }
   }

   void forgetCooldown() {
      this.loop.forgetCooldown();
   }

   private void stop(ModuleChat.Body var1) {
      boolean var2 = this.loop.running() || this.tickHandle != null;
      this.loop.stop();
      this.unlisten();
      this.resetMenu();
      this.link = null;
      this.level = null;
      this.anchor = null;
      if (var2 && var1 != null) {
         this.say(var1);
      }
   }

   private void stopWithRestartHint(ModuleChat.Body var1) {
      this.stop(var1.text(" Turn ").value("Auto RTP").text(" off and on to start again."));
   }

   private void unlisten() {
      if (this.tickHandle != null) {
         this.tickHandle.unregister();
      }

      if (this.packetHandle != null) {
         this.packetHandle.unregister();
      }

      if (this.sendHandle != null) {
         this.sendHandle.unregister();
      }

      this.tickHandle = null;
      this.packetHandle = null;
      this.sendHandle = null;
   }

   boolean baseSpotted(int var1, int var2, boolean var3) {
      if (this.loop.running() && !this.atStartSpot(var1 >> 4, var2 >> 4)) {
         this.found("Base", var1, var2, var3);
         return true;
      } else {
         return false;
      }
   }

   private void found(String var1, int var2, int var3, boolean var4) {
      ClientPlayerEntity var5 = MinecraftClient.method_1551().field_1724;
      boolean var6 = this.loop.phase() == AutoRtpLoop.Phase.TELEPORTING;
      boolean var7 = var5 != null && this.anchor != null && (var5.method_73183() != this.level || var5.method_73189().method_1025(this.anchor) > 256.0);
      boolean var8 = this.loop.awaitingTeleport() && var5 != null && !var7;
      int var9 = this.loop.landings() + (var6 && var7 ? 1 : 0);
      boolean var10 = this.showCoordinates.get();
      ModuleChat.Body var11 = ModuleChat.body().good(var1 + " found");
      if (var10) {
         var11.text(" at ").value(var2 + ", " + var3);
      }

      this.stopWithRestartHint(var11.text(" after ").value(var9).text(rtpWord(var9) + ". Auto RTP stopped."));
      if (var8 && !var4) {
         this.say(
            ModuleChat.body()
               .warn("Your /" + this.command() + " is still pending")
               .text(": move a little to cancel it (most servers cancel a teleport when you move),")
               .text(" or the server may still teleport you away.")
         );
      }

      ChallengeToast.addOrUpdate(
         new ItemStack(var1.equals("Base") ? Items.field_8575 : Items.field_27070),
         var1 + " Found - Auto RTP",
         Text.method_43470(var10 ? "at " + var2 + ", " + var3 : "after " + var9 + rtpWord(var9)),
         this.sound.get()
      );
      if (this.sound.get()) {
         MinecraftClient.method_1551().method_1483().method_4873(PositionedSoundInstance.method_4758(SoundEvents.field_14709, 1.0F));
      }
   }

   private void tick() {
      if (this.loop.running()) {
         MinecraftClient var5 = MinecraftClient.method_1551();
         ClientPlayerEntity var6 = var5.field_1724;
         ClientPlayNetworkHandler var7 = var5.method_1562();
         long var8 = System.currentTimeMillis();
         if (this.link != null && this.link.method_10758() && (var7 == null || var7.method_48296() == this.link)) {
            if (var6 != null && var5.field_1687 != null && var7 != null && var5.field_1761 != null) {
               this.offlineSince = 0L;
               AutoRtpLoop.Config var10 = this.config();
               long var11 = PlayerBypassLightTracker.visibilityRevision();
               if (var11 != this.lastRevision) {
                  this.lastRevision = var11;
                  this.loop.dataArrived(var8);
               }

               if (var6.method_29504()) {
                  if (!this.wasDead) {
                     this.wasDead = true;
                     this.loop.interrupt(var8, var10);
                     this.resetMenu();
                  }

                  this.loop.holdUntil(var8 + 3000L);
                  this.bar(var8, this.label(var10) + " - paused, you died", var8 - this.lastBarAt >= 2000L);
               } else {
                  if (this.wasDead) {
                     this.wasDead = false;
                     this.level = var5.field_1687;
                     this.anchor = var6.method_73189();
                     this.loop.holdUntil(var8 + 3000L);
                  }

                  boolean var4 = var5.field_1687 != this.level;
                  if (var4) {
                     this.level = var5.field_1687;
                     this.knownSuspects.clear();
                  }

                  if (var4 || this.anchor != null && var6.method_73189().method_1025(this.anchor) > 256.0) {
                     switch (this.loop.phase()) {
                        case TELEPORTING:
                           this.land(var6, var8, var10);
                           break;
                        case SCANNING:
                           this.anchor = var6.method_73189();
                           this.loop.relanded(var8, var10);
                           break;
                        default:
                           if (this.loop.landedLate(var8, var10)) {
                              this.anchor = var6.method_73189();
                              this.closeMenu(var6);
                              this.resetMenu();
                           } else if (var4) {
                              this.stopWithRestartHint(ModuleChat.body().warn("World changed").text(", Auto RTP stopped."));
                              return;
                           }
                     }
                  }

                  long var2;
                  if (this.stopOnSuspect.get() && !var4 && (var2 = this.newSuspectNear(var5, var6)) != -9223372036854775808L) {
                     this.found("Suspect", (ChunkPos.method_8325(var2) << 4) + 8, (ChunkPos.method_8332(var2) << 4) + 8, false);
                  } else if (!this.handleMenu(var5, var6, var8, var10)) {
                     String var1 = var5.field_1755 != null && !this.ourMenuOpen(var5, var6) ? "a menu is open" : null;
                     if (var1 != null) {
                        this.loop.holdUntil(var8 + 1000L);
                     }

                     switch (this.loop.tick(var8, var10)) {
                        case SEND:
                           this.send(var6, var7, var8);
                           break;
                        case TIMED_OUT:
                           this.lastTryLanded = false;
                           this.closeMenu(var6);
                           break;
                        case SCAN_CLEAR:
                           this.lastTryLanded = true;
                           break;
                        case LIMIT:
                           this.stopWithRestartHint(
                              ModuleChat.body().text("Max RTPs reached (").value(var10.maxRtps()).text("), no base found. Auto RTP stopped.")
                           );
                           return;
                        case GAVE_UP:
                           this.closeMenu(var6);
                           this.stopWithRestartHint(
                              ModuleChat.body()
                                 .warn("No teleport after 5 tries")
                                 .text(", check ")
                                 .value("RTP Command")
                                 .text(" and ")
                                 .value("Menu Item")
                                 .text(". Auto RTP stopped.")
                           );
                           return;
                     }

                     this.progress(var8, var1, var10);
                  }
               }
            } else {
               if (this.offlineSince == 0L) {
                  this.offlineSince = var8;
               }

               if (var8 - this.offlineSince > 30000L) {
                  this.stopWithRestartHint(ModuleChat.body().warn("No world for 30s").text(", Auto RTP stopped."));
               }
            }
         } else {
            this.stopWithRestartHint(ModuleChat.body().warn("Disconnected").text(", Auto RTP stopped."));
         }
      }
   }

   private void send(ClientPlayerEntity var1, ClientPlayNetworkHandler var2, long var3) {
      this.anchor = var1.method_73189();
      this.resetMenu();
      this.rememberSuspects();
      this.commandSentAt = var3;
      this.sendingCommand = true;

      try {
         var2.method_45730(this.command());
      } finally {
         this.sendingCommand = false;
      }
   }

   private void land(ClientPlayerEntity var1, long var2, AutoRtpLoop.Config var4) {
      this.anchor = var1.method_73189();
      this.closeMenu(var1);
      this.resetMenu();
      this.loop.landed(var2, var4);
   }

   private void onPacket(Packet<?> var1) {
      if (this.loop.running()) {
         if (var1 instanceof EntityDamageS2CPacket var2) {
            this.onDamage(var2);
         } else if (var1 instanceof GameMessageS2CPacket var3) {
            this.onServerText(var3.comp_763().getString(), !var3.comp_906());
         } else if (var1 instanceof TitleS2CPacket var4) {
            this.onServerText(var4.comp_2281().getString(), false);
         } else if (var1 instanceof SubtitleS2CPacket var5) {
            this.onServerText(var5.comp_2280().getString(), false);
         }
      }
   }

   private void onPacketSent(Packet<?> var1) {
      if (!this.sendingCommand) {
         PlayerInteractEntityC2SPacket var2;
         if (var1 instanceof PlayerInteractBlockC2SPacket
            || var1 instanceof CommandExecutionC2SPacket
            || var1 instanceof ChatCommandSignedC2SPacket
            || var1 instanceof PlayerInteractEntityC2SPacket && isInteraction(var2 = (PlayerInteractEntityC2SPacket)var1)) {
            this.userActionAt = System.currentTimeMillis();
         }
      }
   }

   private static boolean isInteraction(PlayerInteractEntityC2SPacket var0) {
      final boolean[] var1 = new boolean[]{false};
      var0.method_34209(new Handler() {
         public void method_34219(Hand var1x) {
            var1[0] = true;
         }

         public void method_34220(Hand var1x, Vec3d var2) {
            var1[0] = true;
         }

         public void method_34218() {
         }
      });
      return var1[0];
   }

   private void onServerText(String var1, boolean var2) {
      long var4 = System.currentTimeMillis();
      boolean var3 = this.loop.phase() == AutoRtpLoop.Phase.TELEPORTING;
      if (var3 || this.loop.sentWithin(var4, 5000L)) {
         if (!var2 || !RtpReply.looksLikePlayerChat(var1, otherPlayerNames())) {
            RtpReply var7 = RtpReply.parse(var1);
            boolean var8 = this.loop.sentWithin(var4, 2500L);
            switch (var7.kind()) {
               case COOLDOWN:
                  this.loop.cooldown(var4, var7.millis());
                  this.noticeCooldown(var4, var7.millis());
                  break;
               case WARMUP:
                  this.loop.warmup(var4, var7.millis(), this.config());
                  if (var7.nextUseMs() >= 0L) {
                     this.loop.nextUseIn(var4, var7.nextUseMs());
                     this.noticeCooldown(var4, var7.nextUseMs());
                  }
                  break;
               case CANCELLED:
                  this.loop.cancelled(var4);
                  break;
               case UNKNOWN_COMMAND:
                  if (var8) {
                     this.stopWithRestartHint(
                        ModuleChat.body()
                           .warn("The server does not know ")
                           .value("/" + this.command())
                           .text(", check ")
                           .value("RTP Command")
                           .text(". Auto RTP stopped.")
                     );
                  }
                  break;
               case NO_PERMISSION:
                  if (var8) {
                     this.stopWithRestartHint(ModuleChat.body().warn("No permission for ").value("/" + this.command()).text(". Auto RTP stopped."));
                  }
                  break;
               case COMBAT:
                  if (var3 && var8) {
                     this.stopWithRestartHint(ModuleChat.body().danger("In combat").text(", Auto RTP stopped."));
                  }
            }
         }
      }
   }

   private void noticeCooldown(long var1, long var3) {
      long var5 = var1 + var3;
      if (var3 >= 60000L && var5 > this.noticedCooldownUntil + 5000L) {
         this.noticedCooldownUntil = var5;
         this.say(ModuleChat.body().text("Server cooldown ").value(seconds(var3)).text(", Auto RTP waits."));
      }
   }

   private static List<String> otherPlayerNames() {
      MinecraftClient var0 = MinecraftClient.method_1551();
      ClientPlayNetworkHandler var1 = var0.method_1562();
      if (var1 == null) {
         return List.of();
      } else {
         ClientPlayerEntity var2 = var0.field_1724;
         UUID var3 = var2 == null ? null : var2.method_5667();
         String var4 = var2 == null ? null : var2.method_7334().name();
         ArrayList var5 = new ArrayList();

         for (PlayerListEntry var7 : var1.method_2880()) {
            GameProfile var8 = var7.method_2966();
            if (var8 != null && var8.name() != null && !Objects.equals(var8.id(), var3) && !var8.name().equalsIgnoreCase(var4)) {
               var5.add(var8.name());
            }
         }

         return var5;
      }
   }

   private void onDamage(EntityDamageS2CPacket var1) {
      MinecraftClient var4 = MinecraftClient.method_1551();
      ClientPlayerEntity var5 = var4.field_1724;
      if (var5 != null && var4.field_1687 != null && var1.comp_1267() == var5.method_5628()) {
         Entity var3 = var1.comp_1269() >= 0 ? var4.field_1687.method_8469(var1.comp_1269()) : null;
         if (var3 != var5) {
            boolean var2 = var3 instanceof PlayerEntity
               || var1.comp_1268().method_40225(DamageTypes.field_42320)
               || var1.comp_1268().method_40225(DamageTypes.field_42332);
            if (var2) {
               ModuleChat.Body var8 = ModuleChat.body().danger("Hit by ");
               if (var3 instanceof PlayerEntity var9) {
                  var8.name(var9.method_7334().name());
               } else {
                  var8.text("a player");
               }

               this.stopWithRestartHint(var8.text(", Auto RTP stopped (combat)."));
            }
         }
      }
   }

   private boolean handleMenu(MinecraftClient var1, ClientPlayerEntity var2, long var3, AutoRtpLoop.Config var5) {
      if (this.loop.phase() == AutoRtpLoop.Phase.TELEPORTING && containerOpen(var1, var2)) {
         ScreenHandler var7 = var2.field_7512;
         if (var7.field_7763 != this.menuId) {
            if (var7.field_7763 == this.ignoredMenuId) {
               return false;
            }

            if (!this.menuMayBeOurs(var3)) {
               this.ignoredMenuId = var7.field_7763;
               return false;
            }

            if (this.menuClicks >= 3) {
               this.stopWithRestartHint(ModuleChat.body().warn("The RTP menu keeps opening").text(", Auto RTP stopped."));
               return true;
            }

            this.menuId = var7.field_7763;
            this.menuSeenAt = var3;
            this.menuClicked = false;
            this.loop.extend(var3, var5);
         }

         if (!this.menuClicked && var3 - this.menuSeenAt >= 300L) {
            ArrayList var8 = new ArrayList();
            String var9 = this.menuItem.get().trim();
            int var10 = findSlot(var7, var9, var8);
            boolean var6 = var3 - this.menuSeenAt < 2500L;
            if (var9.isEmpty()) {
               if (var8.isEmpty() && var6) {
                  return false;
               } else {
                  this.stopWithRestartHint(
                     ModuleChat.body()
                        .warn("The RTP command opened a menu")
                        .text(", set ")
                        .value("Menu Item")
                        .text(" to the item to click")
                        .text(nameList(var8))
                        .text(". Auto RTP stopped.")
                  );
                  return true;
               }
            } else if (var10 >= 0) {
               var1.field_1761.method_2906(var7.field_7763, var10, 0, SlotActionType.field_7790, var2);
               this.menuClicked = true;
               this.menuClicks++;
               this.lastMenuClickAt = var3;
               this.loop.extend(var3, var5);
               return false;
            } else if (var6) {
               return false;
            } else {
               this.stopWithRestartHint(ModuleChat.body().warn("No \"" + var9 + "\" in the RTP menu").text(nameList(var8)).text(". Auto RTP stopped."));
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean menuMayBeOurs(long var1) {
      if (this.commandSentAt != -9223372036854775808L && this.userActionAt < this.commandSentAt) {
         return var1 - this.commandSentAt <= 4000L ? true : this.menuClicks > 0 && var1 - this.lastMenuClickAt <= 4000L;
      } else {
         return false;
      }
   }

   private static int findSlot(ScreenHandler var0, String var1, List<String> var2) {
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      ArrayList var5 = new ArrayList();

      for (Slot var7 : var0.field_7761) {
         if (var7.field_7871 instanceof PlayerInventory) {
            break;
         }

         ItemStack var8 = var7.method_7677();
         if (!var8.method_7960()) {
            String var9 = RtpReply.normalize(var8.method_7964().getString());
            var3.add(var7);
            var4.add(var9);
            var5.add(Registries.field_41178.method_10221(var8.method_7909()).method_12832());
            if (!var9.isEmpty() && !var2.contains(var9)) {
               var2.add(var9);
            }
         }
      }

      for (String var16 : var1.split(",")) {
         String var10 = RtpReply.normalize(var16);
         if (!var10.isEmpty()) {
            String var11 = var10.replace(' ', '_');

            for (int var12 = 0; var12 < var3.size(); var12++) {
               if (((String)var4.get(var12)).contains(var10) || ((String)var5.get(var12)).equals(var11)) {
                  return ((Slot)var3.get(var12)).field_7874;
               }
            }
         }
      }

      return -1;
   }

   private static String nameList(List<String> var0) {
      if (var0.isEmpty()) {
         return "";
      } else {
         List var1 = var0.size() > 10 ? var0.subList(0, 10) : var0;
         return " (items: " + String.join(", ", var1) + (var0.size() > var1.size() ? ", ..." : "") + ")";
      }
   }

   private static boolean containerOpen(MinecraftClient var0, ClientPlayerEntity var1) {
      return var1.field_7512 != var1.field_7498 && var0.field_1755 instanceof HandledScreen;
   }

   private boolean ourMenuOpen(MinecraftClient var1, ClientPlayerEntity var2) {
      return this.menuId >= 0 && containerOpen(var1, var2) && var2.field_7512.field_7763 == this.menuId;
   }

   private void closeMenu(ClientPlayerEntity var1) {
      if (this.menuId >= 0 && var1.field_7512 != var1.field_7498 && var1.field_7512.field_7763 == this.menuId) {
         var1.method_7346();
      }
   }

   private void resetMenu() {
      this.menuId = -1;
      this.ignoredMenuId = -1;
      this.menuClicked = false;
      this.menuClicks = 0;
   }

   private void rememberSuspects() {
      this.knownSuspects.clear();
      LongIterator var1 = PlayerBypassLightTracker.snapshots().keySet().iterator();

      while (var1.hasNext()) {
         long var2 = var1.next();
         if (PlayerBypassLightTracker.isSuspect(var2)) {
            this.knownSuspects.add(var2);
         }
      }
   }

   private long newSuspectNear(MinecraftClient var1, ClientPlayerEntity var2) {
      int var3 = var1.field_1690.method_38521() + 1;
      int var4 = var2.method_24515().method_10263() >> 4;
      int var5 = var2.method_24515().method_10260() >> 4;
      ObjectIterator var6 = PlayerBypassLightTracker.snapshots().long2ObjectEntrySet().iterator();

      while (var6.hasNext()) {
         Entry var7 = (Entry)var6.next();
         long var8 = var7.getLongKey();
         if (!this.knownSuspects.contains(var8)
            && PlayerBypassLightTracker.isSuspect(var8)
            && PlayerBypassLightTracker.isFlagged(var8, (ChunkLightSnapshot)var7.getValue())) {
            int var10 = ChunkPos.method_8325(var8);
            int var11 = ChunkPos.method_8332(var8);
            if (Math.abs(var10 - var4) <= var3 && Math.abs(var11 - var5) <= var3 && !this.atStartSpot(var10, var11)) {
               return var8;
            }
         }
      }

      return -9223372036854775808L;
   }

   private boolean atStartSpot(int var1, int var2) {
      if (this.loop.landings() > 0) {
         return false;
      } else {
         int var3 = MinecraftClient.method_1551().field_1690.method_38521() + 1;
         return Math.abs(var1 - this.startChunkX) <= var3 && Math.abs(var2 - this.startChunkZ) <= var3;
      }
   }

   private void progress(long var1, String var3, AutoRtpLoop.Config var4) {
      String var6 = this.label(var4);
      String var5;
      if (var3 != null) {
         var5 = var6 + " - paused, " + var3;
      } else {
         long var7 = this.loop.nextSendIn(var1);

         var5 = switch (this.loop.phase()) {
            case TELEPORTING -> var6 + " - waiting for the teleport";
            case SCANNING -> var6 + " - scanning, " + seconds(this.loop.scanLeft(var1)) + " left";
            case WAITING -> this.loop.cooldownLeft(var1) > 0L
               ? var6 + " - server cooldown, next in " + seconds(var7)
               : (
                  this.loop.landings() == 0 && this.loop.sent() == 0
                     ? var6 + " - starting" + (var7 >= 1000L ? " in " + seconds(var7) : "")
                     : (
                        !this.lastTryLanded
                           ? var6 + " - no teleport, retry in " + seconds(var7)
                           : (this.loop.settling(var1) ? var6 + " - no base yet, chunks still loading" : var6 + " - no base, next in " + seconds(var7))
                     )
               );
            default -> "";
         };
      }

      boolean var9 = this.loop.phase() != AutoRtpLoop.Phase.TELEPORTING && var1 - this.lastBarAt >= 2000L;
      this.bar(var1, var5, var9);
   }

   private String label(AutoRtpLoop.Config var1) {
      return "Auto RTP #" + this.loop.sent() + (var1.maxRtps() > 0 ? "/" + var1.maxRtps() : "");
   }

   private void bar(long var1, String var3, boolean var4) {
      if (!var3.isEmpty() && (!var3.equals(this.lastBar) || var4)) {
         this.lastBar = var3;
         this.lastBarAt = var1;
         ModuleChat.actionBarAlways(this.module, ModuleChat.body().text(var3));
      }
   }

   private AutoRtpLoop.Config config() {
      return new AutoRtpLoop.Config(millis(this.scanTime), millis(this.delay), millis(this.timeout), this.maxRtps.getInt());
   }

   private String command() {
      String var1 = this.command.get().trim();

      while (var1.startsWith("/")) {
         var1 = var1.substring(1).trim();
      }

      return var1.isEmpty() ? "rtp" : var1;
   }

   private void say(ModuleChat.Body var1) {
      ModuleChat.sendAlways(this.module, var1);
   }

   private static long millis(NumberSetting var0) {
      return Math.round(var0.get() * 1000.0);
   }

   private static String seconds(long var0) {
      long var2 = (var0 + 999L) / 1000L;
      return var2 >= 120L ? String.format(Locale.ROOT, "%dm %ds", var2 / 60L, var2 % 60L) : var2 + "s";
   }

   private static String rtpWord(int var0) {
      return var0 == 1 ? " RTP" : " RTPs";
   }
}
