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
   private static final ExecutorService WEBHOOK_EXECUTOR = Executors.newSingleThreadExecutor(var0 -> {
      Thread var1 = new Thread(var0, "EFN-CoordSnapper-Webhook");
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

   private void onKey(KeyInputEvent var1) {
      if (var1.action == 1) {
         this.onPress(var1.key);
      }
   }

   private void onMouse(MouseInputEvent var1) {
      if (var1.action == 1) {
         this.onPress(1000 + var1.button);
      }
   }

   private void onPress(int var1) {
      int var2 = this.key.value();
      if (ModuleKeybinds.isBindable(var2) && var1 == var2) {
         MinecraftClient var3 = MinecraftClient.method_1551();
         if (var3.field_1755 == null && var3.field_1724 != null && var3.field_1687 != null) {
            this.snap(var3, var3.field_1724, var3.field_1687);
         }
      }
   }

   private void snap(MinecraftClient var1, ClientPlayerEntity var2, ClientWorld var3) {
      BlockPos var4;
      if (this.target.is("Own Position")) {
         var4 = var2.method_24515();
      } else {
         var4 = this.raycast(var1, var2, var3);
         if (var4 == null) {
            ModuleChat.send(
               this, ModuleChat.body().danger("Nothing copied").text(": no loaded block within ").value(this.range.getInt()).text(" blocks of your aim")
            );
            return;
         }

         if (this.format.is("/tp Command")) {
            var4 = var4.method_10084();
         }
      }

      String var5 = this.formatCoords(var4, var3.method_27983());
      var1.field_1774.method_1455(var5);
      if (this.confirm.get()) {
         ModuleChat.send(this, ModuleChat.body().good("Copied").text(" ").value(var5));
      }

      if (this.webhook.get()) {
         this.sendWebhook(var1, var2, var4, var3.method_27983());
      }
   }

   private void sendWebhook(MinecraftClient var1, ClientPlayerEntity var2, BlockPos var3, RegistryKey<World> var4) {
      String var5 = this.webhookUrl.get().trim();
      if (var5.isEmpty()) {
         ModuleChat.send(this, ModuleChat.body().text("Webhook URL is ").warn("not set"));
      } else if (!WebhookConfig.isValidUrl(var5)) {
         ModuleChat.send(this, ModuleChat.body().text("Webhook URL is ").danger("invalid"));
      } else {
         ServerInfo var6 = var1.method_1558();
         String var7 = var6 != null && var6.field_3752 != null ? var6.field_3752 : "Singleplayer";
         String var8 = "**Coordinates:** `"
            + String.format(Locale.ROOT, "%d %d %d", var3.method_10263(), var3.method_10264(), var3.method_10260())
            + "`\n**Dimension:** "
            + dimensionName(var4)
            + "\n**Server:** "
            + escapeMarkdown(var7)
            + "\n**Player:** "
            + escapeMarkdown(var2.method_5477().getString())
            + "\n**Time:** <t:"
            + System.currentTimeMillis() / 1000L
            + ":R>";
         WebhookPayloadBuilder var9 = new WebhookPayloadBuilder()
            .addEmbed(new WebhookEmbed("\ud83d\udccd Coordinates", var8, 3438838, null, null, "Sent by EFN", null));
         WebhookConfig var10 = new WebhookConfig(var5);
         boolean var11 = this.confirm.get();
         WEBHOOK_EXECUTOR.execute(() -> {
            boolean var4x;
            try {
               var4x = CoordSnapperModule.WebhookHolder.SERVICE.send(var10, var9);
            } catch (RuntimeException | LinkageError var6x) {
               var4x = false;
            }

            if (!var4x) {
               ModuleChat.send(this, ModuleChat.body().text("Webhook ").danger("failed").text(" (see the log for details)"));
            } else if (var11) {
               ModuleChat.send(this, ModuleChat.body().text("Sent to ").good("webhook"));
            }
         });
      }
   }

   private static String escapeMarkdown(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length() + 8);

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         if (var3 == '\\' || var3 == '*' || var3 == '_' || var3 == '~' || var3 == '`' || var3 == '|') {
            var1.append('\\');
         }

         var1.append(var3);
      }

      return var1.toString();
   }

   private BlockPos raycast(MinecraftClient var1, ClientPlayerEntity var2, ClientWorld var3) {
      FreecamModule var7 = ModuleRegistry.get(FreecamModule.class);
      Vec3d var5;
      Vec3d var6;
      if (var7 != null && var7.isEnabled()) {
         Camera var8 = var1.field_1773.method_19418();
         var6 = var8.method_71156();
         var5 = Vec3d.method_1030(var8.method_19329(), var8.method_19330());
      } else {
         var6 = var2.method_5836(1.0F);
         var5 = var2.method_5828(1.0F);
      }

      Vec3d var14 = var6.method_1019(var5.method_1021(this.range.get()));
      FluidHandling var9 = this.stopAtFluids.get() ? FluidHandling.field_1347 : FluidHandling.field_1348;
      RaycastContext var10 = new RaycastContext(var6, var14, ShapeType.field_17559, var9, var2);
      BlockPos var11 = BlockPos.method_49638(var6);
      if (startsInside(var10, var3, var11, var6)) {
         Optional var12 = new Box(var11).method_992(var14, var6);
         if (var12.isEmpty()) {
            return null;
         }

         Vec3d var13 = ((Vec3d)var12.get()).method_1019(var5.method_1021(0.001));
         var10 = new RaycastContext(var13, var14, ShapeType.field_17559, var9, var2);
      }

      BlockHitResult var4;
      return (var4 = var3.method_17742(var10)).method_17783() == Type.field_1332 ? var4.method_17777() : null;
   }

   private static boolean startsInside(RaycastContext var0, ClientWorld var1, BlockPos var2, Vec3d var3) {
      return contains(var0.method_17748(var1.method_8320(var2), var1, var2), var2, var3)
         || contains(var0.method_17749(var1.method_8316(var2), var1, var2), var2, var3);
   }

   private static boolean contains(VoxelShape var0, BlockPos var1, Vec3d var2) {
      for (Box var4 : var0.method_1090()) {
         if (var4.method_996(var1).method_1006(var2)) {
            return true;
         }
      }

      return false;
   }

   private String formatCoords(BlockPos var1, RegistryKey<World> var2) {
      int var3 = var1.method_10263();
      int var4 = var1.method_10264();
      int var5 = var1.method_10260();
      boolean var6 = this.includeDimension.get();
      String var7 = this.format.get();

      return switch (var7) {
         case "/tp Command" -> var6
            ? String.format(Locale.ROOT, "/execute in %s run tp @s %d %d %d", var2.method_29177(), var3, var4, var5)
            : String.format(Locale.ROOT, "/tp %d %d %d", var3, var4, var5);
         case "X, Y, Z" -> String.format(Locale.ROOT, "%d, %d, %d", var3, var4, var5) + dimensionSuffix(var2, var6);
         default -> String.format(Locale.ROOT, "%d %d %d", var3, var4, var5) + dimensionSuffix(var2, var6);
      };
   }

   private static String dimensionSuffix(RegistryKey<World> var0, boolean var1) {
      return var1 ? " (" + dimensionName(var0) + ")" : "";
   }

   private static String dimensionName(RegistryKey<World> var0) {
      if (World.field_25179.equals(var0)) {
         return "Overworld";
      } else if (World.field_25180.equals(var0)) {
         return "Nether";
      } else {
         return World.field_25181.equals(var0) ? "End" : var0.method_29177().toString();
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class WebhookHolder {
      static final WebhookService SERVICE = new WebhookService();

      private WebhookHolder() {
      }
   }
}
