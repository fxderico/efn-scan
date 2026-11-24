package net.efn.client.module.impl.misc;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
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
      PlayerBypassModule playerBypassModule,
      BooleanSetting booleanSetting,
      StringSetting stringSetting,
      NumberSetting numberSetting,
      NumberSetting numberSetting2,
      NumberSetting numberSetting3,
      NumberSetting numberSetting4,
      BooleanSetting booleanSetting2,
      StringSetting stringSetting2,
      BooleanSetting booleanSetting3,
      BooleanSetting booleanSetting4
   ) {
      this.module = playerBypassModule;
      this.enabled = booleanSetting;
      this.command = stringSetting;
      this.scanTime = numberSetting;
      this.delay = numberSetting2;
      this.timeout = numberSetting3;
      this.maxRtps = numberSetting4;
      this.stopOnSuspect = booleanSetting2;
      this.menuItem = stringSetting2;
      this.sound = booleanSetting3;
      this.showCoordinates = booleanSetting4;
   }

   void start() {
      if (!this.loop.running()) {
         MinecraftClient var3 = MinecraftClient.getInstance();
         ClientPlayerEntity var4 = var3.player;
         ClientPlayNetworkHandler var5 = var3.getNetworkHandler();
         if (var4 != null && var3.world != null && var5 != null && var5.getConnection().isOpen()) {
            long var6 = System.currentTimeMillis();
            AutoRtpLoop.Config var8 = this.config();
            this.loop.start(var6, var8);
            this.link = var5.getConnection();
            this.level = var3.world;
            this.anchor = var4.getEntityPos();
            this.startChunkX = var4.getBlockPos().getX() >> 4;
            this.startChunkZ = var4.getBlockPos().getZ() >> 4;
            this.wasDead = var4.isDead();
            this.offlineSince = 0L;
            this.lastRevision = PlayerBypassLightTracker.visibilityRevision();
            this.lastTryLanded = false;
            this.noticedCooldownUntil = 0L;
            this.lastBar = "";
            this.resetMenu();
            this.rememberSuspects();
            if (this.tickHandle == null) {
               this.tickHandle = TickEvent.TYPE.listen(this, tickEvent -> this.tick());
            }

            if (this.packetHandle == null) {
               this.packetHandle = PacketReceiveEvent.TYPE.listen(this, packetReceiveEvent -> this.onPacket(packetReceiveEvent.packet));
            }

            if (this.sendHandle == null) {
               this.sendHandle = PacketSendEvent.TYPE.listen(this, packetSendEvent -> this.onPacketSent(packetSendEvent.packet));
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

   private void stop(ModuleChat.Body body) {
      boolean var2 = this.loop.running() || this.tickHandle != null;
      this.loop.stop();
      this.unlisten();
      this.resetMenu();
      this.link = null;
      this.level = null;
      this.anchor = null;
      if (var2 && body != null) {
         this.say(body);
      }
   }

   private void stopWithRestartHint(ModuleChat.Body body) {
      this.stop(body.text(" Turn ").value("Auto RTP").text(" off and on to start again."));
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

   boolean baseSpotted(int i, int j, boolean bl) {
      if (this.loop.running() && !this.atStartSpot(i >> 4, j >> 4)) {
         this.found("Base", i, j, bl);
         return true;
      } else {
         return false;
      }
   }

   private void found(String string, int i, int j, boolean bl) {
      ClientPlayerEntity var5 = MinecraftClient.getInstance().player;
      boolean var6 = this.loop.phase() == AutoRtpLoop.Phase.TELEPORTING;
      boolean var7 = var5 != null && this.anchor != null && (var5.getEntityWorld() != this.level || var5.getEntityPos().squaredDistanceTo(this.anchor) > 256.0);
      boolean var8 = this.loop.awaitingTeleport() && var5 != null && !var7;
      int var9 = this.loop.landings() + (var6 && var7 ? 1 : 0);
      boolean var10 = this.showCoordinates.get();
      ModuleChat.Body var11 = ModuleChat.body().good(string + " found");
      if (var10) {
         var11.text(" at ").value(i + ", " + j);
      }

      this.stopWithRestartHint(var11.text(" after ").value(var9).text(rtpWord(var9) + ". Auto RTP stopped."));
      if (var8 && !bl) {
         this.say(
            ModuleChat.body()
               .warn("Your /" + this.command() + " is still pending")
               .text(": move a little to cancel it (most servers cancel a teleport when you move),")
               .text(" or the server may still teleport you away.")
         );
      }

      ChallengeToast.addOrUpdate(
         new ItemStack(string.equals("Base") ? Items.PLAYER_HEAD : Items.SPYGLASS),
         string + " Found - Auto RTP",
         Text.literal(var10 ? "at " + i + ", " + j : "after " + var9 + rtpWord(var9)),
         this.sound.get()
      );
      if (this.sound.get()) {
         MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F));
      }
   }

   private void tick() {
      if (this.loop.running()) {
         MinecraftClient var5 = MinecraftClient.getInstance();
         ClientPlayerEntity var6 = var5.player;
         ClientPlayNetworkHandler var7 = var5.getNetworkHandler();
         long var8 = System.currentTimeMillis();
         if (this.link != null && this.link.isOpen() && (var7 == null || var7.getConnection() == this.link)) {
            if (var6 != null && var5.world != null && var7 != null && var5.interactionManager != null) {
               this.offlineSince = 0L;
               AutoRtpLoop.Config var10 = this.config();
               long var11 = PlayerBypassLightTracker.visibilityRevision();
               if (var11 != this.lastRevision) {
                  this.lastRevision = var11;
                  this.loop.dataArrived(var8);
               }

               if (var6.isDead()) {
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
                     this.level = var5.world;
                     this.anchor = var6.getEntityPos();
                     this.loop.holdUntil(var8 + 3000L);
                  }

                  boolean var4 = var5.world != this.level;
                  if (var4) {
                     this.level = var5.world;
                     this.knownSuspects.clear();
                  }

                  if (var4 || this.anchor != null && var6.getEntityPos().squaredDistanceTo(this.anchor) > 256.0) {
                     switch (this.loop.phase()) {
                        case TELEPORTING:
                           this.land(var6, var8, var10);
                           break;
                        case SCANNING:
                           this.anchor = var6.getEntityPos();
                           this.loop.relanded(var8, var10);
                           break;
                        default:
                           if (this.loop.landedLate(var8, var10)) {
                              this.anchor = var6.getEntityPos();
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
                     this.found("Suspect", (ChunkPos.getPackedX(var2) << 4) + 8, (ChunkPos.getPackedZ(var2) << 4) + 8, false);
                  } else if (!this.handleMenu(var5, var6, var8, var10)) {
                     String var1 = var5.currentScreen != null && !this.ourMenuOpen(var5, var6) ? "a menu is open" : null;
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

   private void send(ClientPlayerEntity clientPlayerEntity, ClientPlayNetworkHandler clientPlayNetworkHandler, long l) {
      this.anchor = clientPlayerEntity.getEntityPos();
      this.resetMenu();
      this.rememberSuspects();
      this.commandSentAt = l;
      this.sendingCommand = true;

      try {
         clientPlayNetworkHandler.sendChatCommand(this.command());
      } finally {
         this.sendingCommand = false;
      }
   }

   private void land(ClientPlayerEntity clientPlayerEntity, long l, AutoRtpLoop.Config config) {
      this.anchor = clientPlayerEntity.getEntityPos();
      this.closeMenu(clientPlayerEntity);
      this.resetMenu();
      this.loop.landed(l, config);
   }

   private void onPacket(Packet<?> packet) {
      if (this.loop.running()) {
         if (packet instanceof EntityDamageS2CPacket var2) {
            this.onDamage(var2);
         } else if (packet instanceof GameMessageS2CPacket var3) {
            this.onServerText(var3.content().getString(), !var3.overlay());
         } else if (packet instanceof TitleS2CPacket var4) {
            this.onServerText(var4.text().getString(), false);
         } else if (packet instanceof SubtitleS2CPacket var5) {
            this.onServerText(var5.text().getString(), false);
         }
      }
   }

   private void onPacketSent(Packet<?> packet) {
      if (!this.sendingCommand) {
         PlayerInteractEntityC2SPacket var2;
         if (packet instanceof PlayerInteractBlockC2SPacket
            || packet instanceof CommandExecutionC2SPacket
            || packet instanceof ChatCommandSignedC2SPacket
            || packet instanceof PlayerInteractEntityC2SPacket && isInteraction(var2 = (PlayerInteractEntityC2SPacket)packet)) {
            this.userActionAt = System.currentTimeMillis();
         }
      }
   }

   private static boolean isInteraction(PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
      final boolean[] var1 = new boolean[]{false};
      playerInteractEntityC2SPacket.handle(new Handler() {
         @Override
         public void interact(Hand hand) {
            var1[0] = true;
         }

         @Override
         public void interactAt(Hand hand, Vec3d pos) {
            var1[0] = true;
         }

         @Override
         public void attack() {
         }
      });
      return var1[0];
   }

   private void onServerText(String string, boolean bl) {
      long var4 = System.currentTimeMillis();
      boolean var3 = this.loop.phase() == AutoRtpLoop.Phase.TELEPORTING;
      if (var3 || this.loop.sentWithin(var4, 5000L)) {
         if (!bl || !RtpReply.looksLikePlayerChat(string, otherPlayerNames())) {
            RtpReply var7 = RtpReply.parse(string);
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

   private void noticeCooldown(long l, long m) {
      long var5 = l + m;
      if (m >= 60000L && var5 > this.noticedCooldownUntil + 5000L) {
         this.noticedCooldownUntil = var5;
         this.say(ModuleChat.body().text("Server cooldown ").value(seconds(m)).text(", Auto RTP waits."));
      }
   }

   private static List<String> otherPlayerNames() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      ClientPlayNetworkHandler var1 = var0.getNetworkHandler();
      if (var1 == null) {
         return List.of();
      } else {
         ClientPlayerEntity var2 = var0.player;
         UUID var3 = var2 == null ? null : var2.getUuid();
         String var4 = var2 == null ? null : var2.getGameProfile().name();
         ArrayList var5 = new ArrayList();

         for (PlayerListEntry var7 : var1.getPlayerList()) {
            GameProfile var8 = var7.getProfile();
            if (var8 != null && var8.name() != null && !Objects.equals(var8.id(), var3) && !var8.name().equalsIgnoreCase(var4)) {
               var5.add(var8.name());
            }
         }

         return var5;
      }
   }

   private void onDamage(EntityDamageS2CPacket entityDamageS2CPacket) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      ClientPlayerEntity var5 = var4.player;
      if (var5 != null && var4.world != null && entityDamageS2CPacket.entityId() == var5.getId()) {
         Entity var3 = entityDamageS2CPacket.sourceCauseId() >= 0 ? var4.world.getEntityById(entityDamageS2CPacket.sourceCauseId()) : null;
         if (var3 != var5) {
            boolean var2 = var3 instanceof PlayerEntity
               || entityDamageS2CPacket.sourceType().matchesKey(DamageTypes.PLAYER_ATTACK)
               || entityDamageS2CPacket.sourceType().matchesKey(DamageTypes.PLAYER_EXPLOSION);
            if (var2) {
               ModuleChat.Body var8 = ModuleChat.body().danger("Hit by ");
               if (var3 instanceof PlayerEntity var9) {
                  var8.name(var9.getGameProfile().name());
               } else {
                  var8.text("a player");
               }

               this.stopWithRestartHint(var8.text(", Auto RTP stopped (combat)."));
            }
         }
      }
   }

   private boolean handleMenu(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, long l, AutoRtpLoop.Config config) {
      if (this.loop.phase() == AutoRtpLoop.Phase.TELEPORTING && containerOpen(minecraftClient, clientPlayerEntity)) {
         ScreenHandler var7 = clientPlayerEntity.currentScreenHandler;
         if (var7.syncId != this.menuId) {
            if (var7.syncId == this.ignoredMenuId) {
               return false;
            }

            if (!this.menuMayBeOurs(l)) {
               this.ignoredMenuId = var7.syncId;
               return false;
            }

            if (this.menuClicks >= 3) {
               this.stopWithRestartHint(ModuleChat.body().warn("The RTP menu keeps opening").text(", Auto RTP stopped."));
               return true;
            }

            this.menuId = var7.syncId;
            this.menuSeenAt = l;
            this.menuClicked = false;
            this.loop.extend(l, config);
         }

         if (!this.menuClicked && l - this.menuSeenAt >= 300L) {
            ArrayList var8 = new ArrayList();
            String var9 = this.menuItem.get().trim();
            int var10 = findSlot(var7, var9, var8);
            boolean var6 = l - this.menuSeenAt < 2500L;
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
               minecraftClient.interactionManager.clickSlot(var7.syncId, var10, 0, SlotActionType.PICKUP, clientPlayerEntity);
               this.menuClicked = true;
               this.menuClicks++;
               this.lastMenuClickAt = l;
               this.loop.extend(l, config);
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

   private boolean menuMayBeOurs(long l) {
      if (this.commandSentAt != -9223372036854775808L && this.userActionAt < this.commandSentAt) {
         return l - this.commandSentAt <= 4000L ? true : this.menuClicks > 0 && l - this.lastMenuClickAt <= 4000L;
      } else {
         return false;
      }
   }

   private static int findSlot(ScreenHandler screenHandler, String string, List<String> list) {
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      ArrayList var5 = new ArrayList();

      for (Slot var7 : screenHandler.slots) {
         if (var7.inventory instanceof PlayerInventory) {
            break;
         }

         ItemStack var8 = var7.getStack();
         if (!var8.isEmpty()) {
            String var9 = RtpReply.normalize(var8.getName().getString());
            var3.add(var7);
            var4.add(var9);
            var5.add(Registries.ITEM.getId(var8.getItem()).getPath());
            if (!var9.isEmpty() && !list.contains(var9)) {
               list.add(var9);
            }
         }
      }

      for (String var16 : string.split(",")) {
         String var10 = RtpReply.normalize(var16);
         if (!var10.isEmpty()) {
            String var11 = var10.replace(' ', '_');

            for (int var12 = 0; var12 < var3.size(); var12++) {
               if (((String)var4.get(var12)).contains(var10) || ((String)var5.get(var12)).equals(var11)) {
                  return ((Slot)var3.get(var12)).id;
               }
            }
         }
      }

      return -1;
   }

   private static String nameList(List<String> list) {
      if (list.isEmpty()) {
         return "";
      } else {
         List var1 = list.size() > 10 ? list.subList(0, 10) : list;
         return " (items: " + String.join(", ", var1) + (list.size() > var1.size() ? ", ..." : "") + ")";
      }
   }

   private static boolean containerOpen(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      return clientPlayerEntity.currentScreenHandler != clientPlayerEntity.playerScreenHandler && minecraftClient.currentScreen instanceof HandledScreen;
   }

   private boolean ourMenuOpen(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      return this.menuId >= 0 && containerOpen(minecraftClient, clientPlayerEntity) && clientPlayerEntity.currentScreenHandler.syncId == this.menuId;
   }

   private void closeMenu(ClientPlayerEntity clientPlayerEntity) {
      if (this.menuId >= 0
         && clientPlayerEntity.currentScreenHandler != clientPlayerEntity.playerScreenHandler
         && clientPlayerEntity.currentScreenHandler.syncId == this.menuId) {
         clientPlayerEntity.closeHandledScreen();
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

   private long newSuspectNear(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      int var3 = minecraftClient.options.getClampedViewDistance() + 1;
      int var4 = clientPlayerEntity.getBlockPos().getX() >> 4;
      int var5 = clientPlayerEntity.getBlockPos().getZ() >> 4;

      for (Entry var7 : PlayerBypassLightTracker.snapshots().long2ObjectEntrySet()) {
         long var8 = var7.getLongKey();
         if (!this.knownSuspects.contains(var8)
            && PlayerBypassLightTracker.isSuspect(var8)
            && PlayerBypassLightTracker.isFlagged(var8, (ChunkLightSnapshot)var7.getValue())) {
            int var10 = ChunkPos.getPackedX(var8);
            int var11 = ChunkPos.getPackedZ(var8);
            if (Math.abs(var10 - var4) <= var3 && Math.abs(var11 - var5) <= var3 && !this.atStartSpot(var10, var11)) {
               return var8;
            }
         }
      }

      return -9223372036854775808L;
   }

   private boolean atStartSpot(int i, int j) {
      if (this.loop.landings() > 0) {
         return false;
      } else {
         int var3 = MinecraftClient.getInstance().options.getClampedViewDistance() + 1;
         return Math.abs(i - this.startChunkX) <= var3 && Math.abs(j - this.startChunkZ) <= var3;
      }
   }

   private void progress(long l, String string, AutoRtpLoop.Config config) {
      String var6 = this.label(config);
      String var5;
      if (string != null) {
         var5 = var6 + " - paused, " + string;
      } else {
         long var7 = this.loop.nextSendIn(l);

         var5 = switch (this.loop.phase()) {
            case TELEPORTING -> var6 + " - waiting for the teleport";
            case SCANNING -> var6 + " - scanning, " + seconds(this.loop.scanLeft(l)) + " left";
            case WAITING -> this.loop.cooldownLeft(l) > 0L
               ? var6 + " - server cooldown, next in " + seconds(var7)
               : (
                  this.loop.landings() == 0 && this.loop.sent() == 0
                     ? var6 + " - starting" + (var7 >= 1000L ? " in " + seconds(var7) : "")
                     : (
                        !this.lastTryLanded
                           ? var6 + " - no teleport, retry in " + seconds(var7)
                           : (this.loop.settling(l) ? var6 + " - no base yet, chunks still loading" : var6 + " - no base, next in " + seconds(var7))
                     )
               );
            default -> "";
         };
      }

      boolean var9 = this.loop.phase() != AutoRtpLoop.Phase.TELEPORTING && l - this.lastBarAt >= 2000L;
      this.bar(l, var5, var9);
   }

   private String label(AutoRtpLoop.Config config) {
      return "Auto RTP #" + this.loop.sent() + (config.maxRtps() > 0 ? "/" + config.maxRtps() : "");
   }

   private void bar(long l, String string, boolean bl) {
      if (!string.isEmpty() && (!string.equals(this.lastBar) || bl)) {
         this.lastBar = string;
         this.lastBarAt = l;
         ModuleChat.actionBarAlways(this.module, ModuleChat.body().text(string));
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

   private void say(ModuleChat.Body body) {
      ModuleChat.sendAlways(this.module, body);
   }

   private static long millis(NumberSetting numberSetting) {
      return Math.round(numberSetting.get() * 1000.0);
   }

   private static String seconds(long l) {
      long var2 = (l + 999L) / 1000L;
      return var2 >= 120L ? String.format(Locale.ROOT, "%dm %ds", var2 / 60L, var2 % 60L) : var2 + "s";
   }

   private static String rtpWord(int i) {
      return i == 1 ? " RTP" : " RTPs";
   }
}
