package net.efn.client.module.impl.misc;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.webhook.WebhookConfig;
import net.efn.client.webhook.WebhookEmbed;
import net.efn.client.webhook.WebhookPayloadBuilder;
import net.efn.client.webhook.WebhookService;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class CoordSnapperModule extends ClientModule {
   private static final String TARGET_LOOKED_AT = "Looked At";
   private static final String TARGET_OWN_POSITION = "Own Position";
   private static final String FORMAT_SPACES = "X Y Z";
   private static final String FORMAT_COMMAS = "X, Y, Z";
   private static final String FORMAT_TP = "/tp Command";
   private static final double STEP_PAST_BLOCK = 0.001;
   private static final String WEBHOOK_TITLE = "\ud83d\udccd Coordinates";
   private static final int WEBHOOK_COLOR = 3438838;
   private static final ExecutorService WEBHOOK_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
      Thread var1 = new Thread(runnable, "EFN-CoordSnapper-Webhook");
      var1.setDaemon(true);
      return var1;
   });
   private final KeySetting key = this.addSetting("key", new KeySetting("Key", -1));
   private final ModeSetting target = this.addSetting("target", new ModeSetting("Target", "Looked At", "Looked At", "Own Position"));
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 256.0F, 16.0F, 512.0F).step(1.0F));
   private final BooleanSetting stopAtFluids = this.addSetting("stop-at-fluids", new BooleanSetting("Stop at Fluids", false));
   private final ModeSetting format = this.addSetting("format", new ModeSetting("Format", "X Y Z", "X Y Z", "X, Y, Z", "/tp Command"));
   private final BooleanSetting includeDimension = this.addSetting("include-dimension", new BooleanSetting("Include Dimension", true));
   private final BooleanSetting confirm = this.addSetting("confirm", new BooleanSetting("Chat Confirmation", true));
   private final BooleanSetting webhook = this.addSetting("webhook", new BooleanSetting("Webhook", false));
   private final StringSetting webhookUrl = this.addSetting("webhook-url", new StringSetting("Webhook URL", ""));

   public CoordSnapperModule() {
      super("coord-snapper", "Coord Snapper", "Copies the coordinates you look at or stand on with one key.", ModuleCategory.MISC);
      this.range.visibleWhen(() -> this.target.is("Looked At"));
      this.stopAtFluids.visibleWhen(() -> this.target.is("Looked At"));
      BooleanSetting var1 = this.usesNotifications();
      this.confirm.visibleWhen(var1::get);
      this.webhookUrl.visibleWhen(this.webhook::get);
      this.webhookUrl.privateValue();
   }

   @Override
   protected void onEnable() {
      this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
      this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
   }

   private void onKey(KeyInputEvent keyInputEvent) {
      if (keyInputEvent.action == 1) {
         this.onPress(keyInputEvent.key);
      }
   }

   private void onMouse(MouseInputEvent mouseInputEvent) {
      if (mouseInputEvent.action == 1) {
         this.onPress(1000 + mouseInputEvent.button);
      }
   }

   private void onPress(int i) {
      int var2 = this.key.value();
      if (ModuleKeybinds.isBindable(var2) && i == var2) {
         MinecraftClient var3 = MinecraftClient.getInstance();
         if (var3.currentScreen == null && var3.player != null && var3.world != null) {
            this.snap(var3, var3.player, var3.world);
         }
      }
   }

   private void snap(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      BlockPos var4;
      if (this.target.is("Own Position")) {
         var4 = clientPlayerEntity.getBlockPos();
      } else {
         var4 = this.raycast(minecraftClient, clientPlayerEntity, clientWorld);
         if (var4 == null) {
            ModuleChat.send(
               this, ModuleChat.body().danger("Nothing copied").text(": no loaded block within ").value(this.range.getInt()).text(" blocks of your aim")
            );
            return;
         }

         if (this.format.is("/tp Command")) {
            var4 = var4.up();
         }
      }

      String var5 = this.formatCoords(var4, clientWorld.getRegistryKey());
      minecraftClient.keyboard.setClipboard(var5);
      if (this.confirm.get()) {
         ModuleChat.send(this, ModuleChat.body().good("Copied").text(" ").value(var5));
      }

      if (this.webhook.get()) {
         this.sendWebhook(minecraftClient, clientPlayerEntity, var4, clientWorld.getRegistryKey());
      }
   }

   private void sendWebhook(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos, RegistryKey<World> registryKey) {
      String var5 = this.webhookUrl.get().trim();
      if (var5.isEmpty()) {
         ModuleChat.send(this, ModuleChat.body().text("Webhook URL is ").warn("not set"));
      } else if (!WebhookConfig.isValidUrl(var5)) {
         ModuleChat.send(this, ModuleChat.body().text("Webhook URL is ").danger("invalid"));
      } else {
         ServerInfo var6 = minecraftClient.getCurrentServerEntry();
         String var7 = var6 != null && var6.name != null ? var6.name : "Singleplayer";
         String var8 = "**Coordinates:** `"
            + String.format(Locale.ROOT, "%d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ())
            + "`\n**Dimension:** "
            + dimensionName(registryKey)
            + "\n**Server:** "
            + escapeMarkdown(var7)
            + "\n**Player:** "
            + escapeMarkdown(clientPlayerEntity.getName().getString())
            + "\n**Time:** <t:"
            + System.currentTimeMillis() / 1000L
            + ":R>";
         WebhookPayloadBuilder var9 = new WebhookPayloadBuilder()
            .addEmbed(new WebhookEmbed("\ud83d\udccd Coordinates", var8, 3438838, null, null, "Sent by EFN", null));
         WebhookConfig var10 = new WebhookConfig(var5);
         boolean var11 = this.confirm.get();
         WEBHOOK_EXECUTOR.execute(() -> {
            boolean var4;
            try {
               var4 = CoordSnapperModule.WebhookHolder.SERVICE.send(var10, var9);
            } catch (RuntimeException | LinkageError var6x) {
               var4 = false;
            }

            if (!var4) {
               ModuleChat.send(this, ModuleChat.body().text("Webhook ").danger("failed").text(" (see the log for details)"));
            } else if (var11) {
               ModuleChat.send(this, ModuleChat.body().text("Sent to ").good("webhook"));
            }
         });
      }
   }

   private static String escapeMarkdown(String string) {
      StringBuilder var1 = new StringBuilder(string.length() + 8);

      for (int var2 = 0; var2 < string.length(); var2++) {
         char var3 = string.charAt(var2);
         if (var3 == '\\' || var3 == '*' || var3 == '_' || var3 == '~' || var3 == '`' || var3 == '|') {
            var1.append('\\');
         }

         var1.append(var3);
      }

      return var1.toString();
   }

   private BlockPos raycast(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      FreecamModule var7 = ModuleRegistry.get(FreecamModule.class);
      Vec3d var5;
      Vec3d var6;
      if (var7 != null && var7.isEnabled()) {
         Camera var8 = minecraftClient.gameRenderer.getCamera();
         var6 = var8.getCameraPos();
         var5 = Vec3d.fromPolar(var8.getPitch(), var8.getYaw());
      } else {
         var6 = clientPlayerEntity.getCameraPosVec(1.0F);
         var5 = clientPlayerEntity.getRotationVec(1.0F);
      }

      Vec3d var14 = var6.add(var5.multiply(this.range.get()));
      FluidHandling var9 = this.stopAtFluids.get() ? FluidHandling.ANY : FluidHandling.NONE;
      RaycastContext var10 = new RaycastContext(var6, var14, ShapeType.OUTLINE, var9, clientPlayerEntity);
      BlockPos var11 = BlockPos.ofFloored(var6);
      if (startsInside(var10, clientWorld, var11, var6)) {
         Optional var12 = new Box(var11).raycast(var14, var6);
         if (var12.isEmpty()) {
            return null;
         }

         Vec3d var13 = ((Vec3d)var12.get()).add(var5.multiply(0.001));
         var10 = new RaycastContext(var13, var14, ShapeType.OUTLINE, var9, clientPlayerEntity);
      }

      BlockHitResult var4;
      return (var4 = clientWorld.raycast(var10)).getType() == Type.BLOCK ? var4.getBlockPos() : null;
   }

   private static boolean startsInside(RaycastContext raycastContext, ClientWorld clientWorld, BlockPos blockPos, Vec3d vec3d) {
      return contains(raycastContext.getBlockShape(clientWorld.getBlockState(blockPos), clientWorld, blockPos), blockPos, vec3d)
         || contains(raycastContext.getFluidShape(clientWorld.getFluidState(blockPos), clientWorld, blockPos), blockPos, vec3d);
   }

   private static boolean contains(VoxelShape voxelShape, BlockPos blockPos, Vec3d vec3d) {
      for (Box var4 : voxelShape.getBoundingBoxes()) {
         if (var4.offset(blockPos).contains(vec3d)) {
            return true;
         }
      }

      return false;
   }

   private String formatCoords(BlockPos blockPos, RegistryKey<World> registryKey) {
      int var3 = blockPos.getX();
      int var4 = blockPos.getY();
      int var5 = blockPos.getZ();
      boolean var6 = this.includeDimension.get();
      String var7 = this.format.get();

      return switch (var7) {
         case "/tp Command" -> var6
            ? String.format(Locale.ROOT, "/execute in %s run tp @s %d %d %d", registryKey.getValue(), var3, var4, var5)
            : String.format(Locale.ROOT, "/tp %d %d %d", var3, var4, var5);
         case "X, Y, Z" -> String.format(Locale.ROOT, "%d, %d, %d", var3, var4, var5) + dimensionSuffix(registryKey, var6);
         default -> String.format(Locale.ROOT, "%d %d %d", var3, var4, var5) + dimensionSuffix(registryKey, var6);
      };
   }

   private static String dimensionSuffix(RegistryKey<World> registryKey, boolean bl) {
      return bl ? " (" + dimensionName(registryKey) + ")" : "";
   }

   private static String dimensionName(RegistryKey<World> registryKey) {
      if (World.OVERWORLD.equals(registryKey)) {
         return "Overworld";
      } else if (World.NETHER.equals(registryKey)) {
         return "Nether";
      } else {
         return World.END.equals(registryKey) ? "End" : registryKey.getValue().toString();
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class WebhookHolder {
      static final WebhookService SERVICE = new WebhookService();

      private WebhookHolder() {
      }
   }
}
