package net.efn.client.module.impl.render;

import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.EfnKeyPolling;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class WeaponInspectModule extends ClientModule {
   public static volatile WeaponInspectModule active;
   private static final String SWORDS = "Swords";
   private static final String WEAPONS = "Weapons";
   private static final String ANY_ITEM = "Any Item";
   private static final Identifier KNIFE_TEXTURE = Identifier.method_60655("efn", "textures/misc/knife_base.png");
   private static final float EASE_OUT_SECONDS = 0.15F;
   private static final int IDLE = 0;
   private static final int INSPECT = 1;
   private static final int DEPLOY = 2;
   private static final float[] REST = new float[16];
   private static final float SPARKLE_SIZE = 0.07F;
   private final KeySetting key = this.addSetting("key", new KeySetting("Inspect Key", 70));
   private final ModeSetting skin = this.addSetting("skin", new ModeSetting("Knife Skin", "Butterfly", "None", "Butterfly", "Karambit", "Katana"));
   private final ModeSetting style = this.addSetting("style", new ModeSetting("Style", "Classic", "Classic", "Twirl"));
   private final ModeSetting items = this.addSetting("items", new ModeSetting("Items", "Swords", "Swords", "Weapons", "Any Item"));
   private final NumberSetting speed = this.addSetting("speed", new NumberSetting("Speed", 1.0F, 0.5F, 2.0F).step(0.05F));
   private final NumberSetting knifeSize = this.addSetting("knife-size", new NumberSetting("Knife Size", 1.0F, 0.5F, 2.0F).step(0.05F));
   private final BooleanSetting showArm = this.addSetting("show-arm", new BooleanSetting("Show Arm", true));
   private final BooleanSetting deploy = this.addSetting("deploy", new BooleanSetting("Deploy Animation", true));
   private final BooleanSetting slashTrail = this.addSetting("slash-trail", new BooleanSetting("Slash Trail", true));
   private final NumberSetting trailLength = this.addSetting("trail-length", new NumberSetting("Trail Length", 0.14F, 0.05F, 0.35F).step(0.01F));
   private final BooleanSetting sparkles = this.addSetting("sparkles", new BooleanSetting("Blade Sparkle", true));
   private final BooleanSetting sneakSkips = this.addSetting("sneak-skips", new BooleanSetting("Sneak Skips Inspect", true));
   private int mode = 0;
   private InspectAnimation.Clip clip;
   private KnifeSkin clipKnife;
   private long startedAt;
   private float seconds;
   private boolean easingOut;
   private long easeStartedAt;
   private float easeFrom;
   private int slot;
   private Item item;
   private int heldSlot = -1;
   private Item heldItem;
   private final float[] state = new float[16];
   private final float[] trailSample = new float[12];
   private final SlashTrail[] trails = new SlashTrail[2];
   private final Matrix4f base = new Matrix4f();
   private final Matrix4f baseInverse = new Matrix4f();
   private final Matrix3f baseNormal = new Matrix3f();
   private boolean baseKnown;

   public WeaponInspectModule() {
      super(
         "weapon-inspect",
         "CS Knife",
         "Knife skins for your sword and CS-style inspects with a key of your choice (F by default); hold the key to keep the knife spinning.",
         ModuleCategory.VISUALS
      );

      for (int var1 = 0; var1 < this.trails.length; var1++) {
         this.trails[var1] = new SlashTrail();
      }

      this.knifeSize.visibleWhen(() -> !this.skin.is("None"));
      this.deploy.visibleWhen(() -> !this.skin.is("None"));
      this.sparkles.visibleWhen(() -> !this.skin.is("None"));
      this.trailLength.visibleWhen(this.slashTrail::get);
   }

   @Override
   protected void onEnable() {
      this.mode = 0;
      this.heldItem = null;
      this.clearTrails();
      active = this;
      this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
      this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
   }

   @Override
   protected void onDisable() {
      active = null;
      this.mode = 0;
      this.clearTrails();
   }

   public static void handsBegin(MatrixStack var0) {
      WeaponInspectModule var1 = active;
      if (var1 != null) {
         var1.base.set(var0.method_23760().method_23761());
         var1.base.invert(var1.baseInverse);
         var1.baseNormal.set(var0.method_23760().method_23762());
         var1.baseKnown = true;
      }
   }

   public static boolean renderItem(
      MatrixStack var0, LivingEntity var1, ItemStack var2, ItemDisplayContext var3, OrderedRenderCommandQueue var4, int var5, boolean var6
   ) {
      WeaponInspectModule var7 = active;
      return var7 != null && var7.render(var0, var1, var2, var3, var4, var5, var6);
   }

   private void onKey(KeyInputEvent var1) {
      if (var1.action != 0 && this.pressed(var1.key, var1.action == 2)) {
         var1.setCancelled(true);
      }
   }

   private void onMouse(MouseInputEvent var1) {
      if (var1.action == 1 && this.pressed(1000 + var1.button, false)) {
         var1.setCancelled(true);
      }
   }

   private boolean pressed(int var1, boolean var2) {
      int var3 = this.key.value();
      if (ModuleKeybinds.isBindable(var3) && var1 == var3) {
         MinecraftClient var4 = MinecraftClient.method_1551();
         ClientPlayerEntity var5 = var4.field_1724;
         if (var4.field_1755 != null || var5 == null || var5.method_7325()) {
            return false;
         } else if (this.sneakSkips.get() && var5.method_5715()) {
            return false;
         } else {
            ItemStack var6 = var5.method_6047();
            if (!this.inspectable(var6)) {
               return false;
            } else {
               if (!var2 && !var5.method_6115()) {
                  this.start(1, var5.method_31548().method_67532(), var6);
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   private boolean held() {
      int var1 = this.key.value();
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (ModuleKeybinds.isBindable(var1) && var2.field_1755 == null) {
         return var1 >= 1000
            ? GLFW.glfwGetMouseButton(var2.method_22683().method_4490(), var1 - 1000) == 1
            : EfnKeyPolling.isKeyDown(var2.method_22683(), var1);
      } else {
         return false;
      }
   }

   private boolean inspectable(ItemStack var1) {
      if (var1.method_7960()) {
         return false;
      } else {
         return !this.items.is("Any Item") && !isSword(var1)
            ? this.items.is("Weapons")
               && (
                  var1.method_31573(ItemTags.field_42612)
                     || var1.method_31573(ItemTags.field_63257)
                     || var1.method_31574(Items.field_49814)
                     || var1.method_31574(Items.field_8547)
               )
            : true;
      }
   }

   private static boolean isSword(ItemStack var0) {
      return !var0.method_7960()
         && (var0.method_31573(ItemTags.field_42611) || Registries.field_41178.method_10221(var0.method_7909()).method_12832().endsWith("_sword"));
   }

   private static boolean handheld(ItemStack var0) {
      return isSword(var0)
         || var0.method_31573(ItemTags.field_42612)
         || var0.method_31573(ItemTags.field_42614)
         || var0.method_31573(ItemTags.field_42615)
         || var0.method_31573(ItemTags.field_42613);
   }

   private void start(int var1, int var2, ItemStack var3) {
      KnifeSkin var4 = isSword(var3) ? KnifeSkin.of(this.skin.get()) : null;
      this.clip = var1 == 2 ? var4.deploy : (var4 != null ? var4.inspect : InspectAnimation.clip(this.style.get()));
      this.clipKnife = var4;
      this.mode = var1;
      this.seconds = this.clip.seconds;
      this.easingOut = false;
      this.startedAt = System.nanoTime();
      this.slot = var2;
      this.item = var3.method_7909();
   }

   private void easeOut() {
      long var1 = System.nanoTime();
      this.easeFrom = Math.min(this.progress(var1), 1.0F);
      this.easeStartedAt = var1;
      this.easingOut = true;
   }

   private void onTick(TickEvent var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 == null) {
         this.mode = 0;
         this.heldItem = null;
      } else {
         ItemStack var3 = var2.method_6047();
         int var4 = var2.method_31548().method_67532();
         if (var4 != this.heldSlot || !var3.method_31574(this.heldItem)) {
            this.heldSlot = var4;
            this.heldItem = var3.method_7909();
            if (this.deploy.get() && isSword(var3) && KnifeSkin.of(this.skin.get()) != null) {
               this.start(2, var4, var3);
            } else if (this.mode != 0 && !this.easingOut) {
               this.easeOut();
            }
         } else if (this.mode != 0 && !this.easingOut) {
            if (var2.field_6252) {
               this.mode = 0;
            } else if (var2.method_6115()) {
               this.easeOut();
            } else if (this.loopedProgress(System.nanoTime()) >= 1.0F) {
               this.mode = 0;
            }
         }
      }
   }

   private float progress(long var1) {
      return (float)(var1 - this.startedAt) / 1.0E9F / (this.seconds / this.speed.get());
   }

   private float loopedProgress(long var1) {
      float var3 = this.progress(var1);
      if (this.mode == 1 && this.clip.loops() && !(var3 < this.clip.loopExit) && this.held()) {
         float var4 = this.seconds / this.speed.get();
         long var5 = (long)((this.clip.loopExit - this.clip.loopEnter) * var4 * 1.0E9F);
         if (var5 <= 0L) {
            return var3;
         } else {
            long var7 = (long)((var3 - this.clip.loopExit) * var4 * 1.0E9F);
            this.startedAt += (var7 / var5 + 1L) * var5;
            return this.progress(var1);
         }
      } else {
         return var3;
      }
   }

   private float animate(ItemStack var1, KnifeSkin var2, long var3) {
      if (this.mode == 0 || !var1.method_31574(this.item)) {
         return 0.0F;
      } else if (var2 != this.clipKnife) {
         this.mode = 0;
         return 0.0F;
      } else {
         float var6 = 1.0F;
         float var5;
         if (this.easingOut) {
            float var7 = (float)(var3 - this.easeStartedAt) / 1.0E9F / 0.15F;
            if (var7 >= 1.0F) {
               this.mode = 0;
               return 0.0F;
            }

            var5 = this.easeFrom;
            var6 = 1.0F - var7 * var7 * (3.0F - 2.0F * var7);
         } else {
            var5 = this.loopedProgress(var3);
            if (var5 >= 1.0F) {
               this.mode = 0;
               return 0.0F;
            }
         }

         this.clip.sample(var5, this.state);
         return var6;
      }
   }

   private boolean render(MatrixStack var1, LivingEntity var2, ItemStack var3, ItemDisplayContext var4, OrderedRenderCommandQueue var5, int var6, boolean var7) {
      int var9 = var4 == ItemDisplayContext.field_4321 ? -1 : 1;
      boolean var10 = isSword(var3);
      KnifeSkin var11 = var10 ? KnifeSkin.of(this.skin.get()) : null;
      long var12 = System.nanoTime();
      float var14 = var7 ? this.animate(var3, var11, var12) : 0.0F;
      float[] var15 = var14 > 0.0F ? this.state : REST;
      boolean var8 = var7 && var10 && this.slashTrail.get() && this.baseKnown;
      if (var7 && !var8) {
         this.clearTrails();
      }

      if (var11 == null) {
         FirstPersonArm.Arm var21;
         if ((this.showArm.get() && handheld(var3) || HandModelModule.covers(var3, var7)) && (var21 = FirstPersonArm.arm()) != null) {
            FirstPersonArm.hold(var1, var21, var3, var4, var2, var5, var6, var15[0] * var14, var15[1] * var14, var15[2] * var14);
         }

         if (var14 > 0.0F) {
            InspectAnimation.place(var1, var15, var14, var9, 0.07F, 0.2F, 0.07F);
         }

         if (var8) {
            this.swordTrail(var1.method_23760().method_23761(), var9);
            this.record(var12, 1);
            this.submitGlow(var5, var12, KnifeMaterial.of(var3).glow, 0.0F, null);
         }

         return false;
      } else {
         KnifeMaterial var17 = KnifeMaterial.of(var3);
         FirstPersonArm.Arm var18 = !this.showArm.get() && !HandModelModule.wants(var7) ? null : FirstPersonArm.arm();
         boolean var19 = var18 != null && var18.slim();
         var1.method_22903();
         if (var9 < 0) {
            var1.method_22905(-1.0F, 1.0F, 1.0F);
         }

         var11.draw(
            var1,
            var15,
            var14,
            var19,
            this.knifeSize.get(),
            sink(var5, var6, var9, var18, var7 && offHandFree(), var17, var3.method_7958()),
            var8 ? this.trailSample : null
         );
         var1.method_22909();
         if (var8) {
            this.record(var12, var11.streakCount());
            float var20 = this.sparkles.get() && var14 > 0.0F ? var11.sparkle(var15, var14) : 0.0F;
            this.submitGlow(var5, var12, var17.glow, var20, var11);
         }

         return true;
      }
   }

   private static KnifeSkin.Sink sink(
      final OrderedRenderCommandQueue var0,
      final int var1,
      final int var2,
      final FirstPersonArm.Arm var3,
      final boolean var4,
      final KnifeMaterial var5,
      final boolean var6
   ) {
      final RenderLayer var7 = RenderLayers.method_75994(KNIFE_TEXTURE);
      final int[] var8 = var5.palette;
      return new KnifeSkin.Sink() {
         @Override
         public void part(MatrixStack var1x, KnifeMesh var2x, KnifeMesh.Shine var3x) {
            var0.method_73483(var1x, var7, (var4xx, var5xx) -> var2x.emit(var4xx, var5xx, var8, var1, var3x));
            if (var6) {
               var0.method_73529(1)
                  .method_73483(var1x, RenderLayers.method_75997(), (var3xxx, var4xx) -> var2x.emit(var3xxx, var4xx, var8, var1, KnifeMesh.Shine.NONE));
            }
         }

         @Override
         public void glow(MatrixStack var1x, KnifeMesh var2x, KnifeMesh.Shine var3x) {
            var0.method_73529(1)
               .method_73483(
                  var1x, RenderLayers.method_76014(WeaponInspectModule.KNIFE_TEXTURE), (var3xxx, var4xx) -> var2x.emitGlow(var3xxx, var4xx, var5.glow, var3x)
               );
         }

         @Override
         public void arm(MatrixStack var1x, boolean var2x) {
            if (var3 != null && (!var2x || var4)) {
               var3.draw(var1x, var2 < 0 != var2x, var0, var1);
            }
         }
      };
   }

   private static boolean offHandFree() {
      ClientPlayerEntity var0 = MinecraftClient.method_1551().field_1724;
      return var0 != null && var0.method_6079().method_7960();
   }

   private void swordTrail(Matrix4f var1, int var2) {
      Matrix4f var3 = new Matrix4f(var1)
         .translate(var2 * 1.13F / 16.0F, 0.2F, 0.070625F)
         .rotate(new Quaternionf().rotationXYZ(0.0F, -1.5707964F, 0.43633232F))
         .scale(0.68F)
         .translate(-0.5F, -0.5F, -0.5F);
      Vector3f var4 = var3.transformPosition(0.3125F, 0.3125F, 0.5F, new Vector3f());
      this.trailSample[0] = var4.x;
      this.trailSample[1] = var4.y;
      this.trailSample[2] = var4.z;
      var3.transformPosition(0.96875F, 0.96875F, 0.5F, var4);
      this.trailSample[3] = var4.x;
      this.trailSample[4] = var4.y;
      this.trailSample[5] = var4.z;
   }

   private void record(long var1, int var3) {
      Vector3f var4 = new Vector3f();

      for (int var5 = 0; var5 < this.trails.length; var5++) {
         if (var5 >= var3) {
            this.trails[var5].clear();
         } else {
            for (int var6 = var5 * 6; var6 < var5 * 6 + 6; var6 += 3) {
               this.baseInverse.transformPosition(this.trailSample[var6], this.trailSample[var6 + 1], this.trailSample[var6 + 2], var4);
               this.trailSample[var6] = var4.x;
               this.trailSample[var6 + 1] = var4.y;
               this.trailSample[var6 + 2] = var4.z;
            }

            this.trails[var5].add(var1, this.trailSample, var5 * 6);
         }
      }
   }

   private void clearTrails() {
      for (SlashTrail var4 : this.trails) {
         var4.clear();
      }
   }

   private void submitGlow(OrderedRenderCommandQueue var1, long var2, int var4, float var5, KnifeSkin var6) {
      long var7 = (long)(this.trailLength.get() * 1.0E9F);
      int var9 = var6 != null ? var6.streakCount() : 1;
      boolean var10 = var5 > 0.0F;

      for (int var11 = 0; var11 < var9 && !var10; var11++) {
         var10 = !this.trails[var11].isEmpty(var2, var7);
      }

      if (var10) {
         float var16 = this.trailSample[3];
         float var12 = this.trailSample[4];
         float var13 = this.trailSample[5];
         float var14 = (float)(var2 % 6283185307L) / 1.0E9F * 1.5F;
         MatrixStack var15 = new MatrixStack();
         var15.method_23760().method_23761().set(this.base);
         var15.method_23760().method_23762().set(this.baseNormal);
         var1.method_73529(1).method_73483(var15, RenderLayers.method_76014(KNIFE_TEXTURE), (var13x, var14x) -> {
            for (int var15x = 0; var15x < var9; var15x++) {
               this.trails[var15x].emit(var13x, var14x, var2, var7, var4, var6 != null ? var6.streakStrength(var15x) : 1.0F);
            }

            if (var5 > 0.0F) {
               SlashTrail.sparkle(var13x, var14x, var16, var12, var13, 0.07F * var5, var14, var4, var5);
            }
         });
      }
   }
}
