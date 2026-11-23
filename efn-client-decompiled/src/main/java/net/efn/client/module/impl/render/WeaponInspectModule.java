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
   private static final Identifier KNIFE_TEXTURE = Identifier.of("efn", "textures/misc/knife_base.png");
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

   public static void handsBegin(MatrixStack matrixStack) {
      WeaponInspectModule var1 = active;
      if (var1 != null) {
         var1.base.set(matrixStack.peek().getPositionMatrix());
         var1.base.invert(var1.baseInverse);
         var1.baseNormal.set(matrixStack.peek().getNormalMatrix());
         var1.baseKnown = true;
      }
   }

   public static boolean renderItem(
      MatrixStack matrixStack,
      LivingEntity livingEntity,
      ItemStack itemStack,
      ItemDisplayContext itemDisplayContext,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      boolean bl
   ) {
      WeaponInspectModule var7 = active;
      return var7 != null && var7.render(matrixStack, livingEntity, itemStack, itemDisplayContext, orderedRenderCommandQueue, i, bl);
   }

   private void onKey(KeyInputEvent keyInputEvent) {
      if (keyInputEvent.action != 0 && this.pressed(keyInputEvent.key, keyInputEvent.action == 2)) {
         keyInputEvent.setCancelled(true);
      }
   }

   private void onMouse(MouseInputEvent mouseInputEvent) {
      if (mouseInputEvent.action == 1 && this.pressed(1000 + mouseInputEvent.button, false)) {
         mouseInputEvent.setCancelled(true);
      }
   }

   private boolean pressed(int i, boolean bl) {
      int var3 = this.key.value();
      if (ModuleKeybinds.isBindable(var3) && i == var3) {
         MinecraftClient var4 = MinecraftClient.getInstance();
         ClientPlayerEntity var5 = var4.player;
         if (var4.currentScreen != null || var5 == null || var5.isSpectator()) {
            return false;
         } else if (this.sneakSkips.get() && var5.isSneaking()) {
            return false;
         } else {
            ItemStack var6 = var5.getMainHandStack();
            if (!this.inspectable(var6)) {
               return false;
            } else {
               if (!bl && !var5.isUsingItem()) {
                  this.start(1, var5.getInventory().getSelectedSlot(), var6);
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
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (ModuleKeybinds.isBindable(var1) && var2.currentScreen == null) {
         return var1 >= 1000 ? GLFW.glfwGetMouseButton(var2.getWindow().getHandle(), var1 - 1000) == 1 : EfnKeyPolling.isKeyDown(var2.getWindow(), var1);
      } else {
         return false;
      }
   }

   private boolean inspectable(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return false;
      } else {
         return !this.items.is("Any Item") && !isSword(itemStack)
            ? this.items.is("Weapons")
               && (itemStack.isIn(ItemTags.AXES) || itemStack.isIn(ItemTags.SPEARS) || itemStack.isOf(Items.MACE) || itemStack.isOf(Items.TRIDENT))
            : true;
      }
   }

   private static boolean isSword(ItemStack itemStack) {
      return !itemStack.isEmpty() && (itemStack.isIn(ItemTags.SWORDS) || Registries.ITEM.getId(itemStack.getItem()).getPath().endsWith("_sword"));
   }

   private static boolean handheld(ItemStack itemStack) {
      return isSword(itemStack)
         || itemStack.isIn(ItemTags.AXES)
         || itemStack.isIn(ItemTags.PICKAXES)
         || itemStack.isIn(ItemTags.SHOVELS)
         || itemStack.isIn(ItemTags.HOES);
   }

   private void start(int i, int j, ItemStack itemStack) {
      KnifeSkin var4 = isSword(itemStack) ? KnifeSkin.of(this.skin.get()) : null;
      this.clip = i == 2 ? var4.deploy : (var4 != null ? var4.inspect : InspectAnimation.clip(this.style.get()));
      this.clipKnife = var4;
      this.mode = i;
      this.seconds = this.clip.seconds;
      this.easingOut = false;
      this.startedAt = System.nanoTime();
      this.slot = j;
      this.item = itemStack.getItem();
   }

   private void easeOut() {
      long var1 = System.nanoTime();
      this.easeFrom = Math.min(this.progress(var1), 1.0F);
      this.easeStartedAt = var1;
      this.easingOut = true;
   }

   private void onTick(TickEvent tickEvent) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var2 == null) {
         this.mode = 0;
         this.heldItem = null;
      } else {
         ItemStack var3 = var2.getMainHandStack();
         int var4 = var2.getInventory().getSelectedSlot();
         if (var4 != this.heldSlot || !var3.isOf(this.heldItem)) {
            this.heldSlot = var4;
            this.heldItem = var3.getItem();
            if (this.deploy.get() && isSword(var3) && KnifeSkin.of(this.skin.get()) != null) {
               this.start(2, var4, var3);
            } else if (this.mode != 0 && !this.easingOut) {
               this.easeOut();
            }
         } else if (this.mode != 0 && !this.easingOut) {
            if (var2.handSwinging) {
               this.mode = 0;
            } else if (var2.isUsingItem()) {
               this.easeOut();
            } else if (this.loopedProgress(System.nanoTime()) >= 1.0F) {
               this.mode = 0;
            }
         }
      }
   }

   private float progress(long l) {
      return (float)(l - this.startedAt) / 1.0E9F / (this.seconds / this.speed.get());
   }

   private float loopedProgress(long l) {
      float var3 = this.progress(l);
      if (this.mode == 1 && this.clip.loops() && !(var3 < this.clip.loopExit) && this.held()) {
         float var4 = this.seconds / this.speed.get();
         long var5 = (long)((this.clip.loopExit - this.clip.loopEnter) * var4 * 1.0E9F);
         if (var5 <= 0L) {
            return var3;
         } else {
            long var7 = (long)((var3 - this.clip.loopExit) * var4 * 1.0E9F);
            this.startedAt += (var7 / var5 + 1L) * var5;
            return this.progress(l);
         }
      } else {
         return var3;
      }
   }

   private float animate(ItemStack itemStack, KnifeSkin knifeSkin, long l) {
      if (this.mode == 0 || !itemStack.isOf(this.item)) {
         return 0.0F;
      } else if (knifeSkin != this.clipKnife) {
         this.mode = 0;
         return 0.0F;
      } else {
         float var6 = 1.0F;
         float var5;
         if (this.easingOut) {
            float var7 = (float)(l - this.easeStartedAt) / 1.0E9F / 0.15F;
            if (var7 >= 1.0F) {
               this.mode = 0;
               return 0.0F;
            }

            var5 = this.easeFrom;
            var6 = 1.0F - var7 * var7 * (3.0F - 2.0F * var7);
         } else {
            var5 = this.loopedProgress(l);
            if (var5 >= 1.0F) {
               this.mode = 0;
               return 0.0F;
            }
         }

         this.clip.sample(var5, this.state);
         return var6;
      }
   }

   private boolean render(
      MatrixStack matrixStack,
      LivingEntity livingEntity,
      ItemStack itemStack,
      ItemDisplayContext itemDisplayContext,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      boolean bl
   ) {
      int var9 = itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
      boolean var10 = isSword(itemStack);
      KnifeSkin var11 = var10 ? KnifeSkin.of(this.skin.get()) : null;
      long var12 = System.nanoTime();
      float var14 = bl ? this.animate(itemStack, var11, var12) : 0.0F;
      float[] var15 = var14 > 0.0F ? this.state : REST;
      boolean var8 = bl && var10 && this.slashTrail.get() && this.baseKnown;
      if (bl && !var8) {
         this.clearTrails();
      }

      if (var11 == null) {
         FirstPersonArm.Arm var21;
         if ((this.showArm.get() && handheld(itemStack) || HandModelModule.covers(itemStack, bl)) && (var21 = FirstPersonArm.arm()) != null) {
            FirstPersonArm.hold(
               matrixStack,
               var21,
               itemStack,
               itemDisplayContext,
               livingEntity,
               orderedRenderCommandQueue,
               i,
               var15[0] * var14,
               var15[1] * var14,
               var15[2] * var14
            );
         }

         if (var14 > 0.0F) {
            InspectAnimation.place(matrixStack, var15, var14, var9, 0.07F, 0.2F, 0.07F);
         }

         if (var8) {
            this.swordTrail(matrixStack.peek().getPositionMatrix(), var9);
            this.record(var12, 1);
            this.submitGlow(orderedRenderCommandQueue, var12, KnifeMaterial.of(itemStack).glow, 0.0F, null);
         }

         return false;
      } else {
         KnifeMaterial var17 = KnifeMaterial.of(itemStack);
         FirstPersonArm.Arm var18 = !this.showArm.get() && !HandModelModule.wants(bl) ? null : FirstPersonArm.arm();
         boolean var19 = var18 != null && var18.slim();
         matrixStack.push();
         if (var9 < 0) {
            matrixStack.scale(-1.0F, 1.0F, 1.0F);
         }

         var11.draw(
            matrixStack,
            var15,
            var14,
            var19,
            this.knifeSize.get(),
            sink(orderedRenderCommandQueue, i, var9, var18, bl && offHandFree(), var17, itemStack.hasGlint()),
            var8 ? this.trailSample : null
         );
         matrixStack.pop();
         if (var8) {
            this.record(var12, var11.streakCount());
            float var20 = this.sparkles.get() && var14 > 0.0F ? var11.sparkle(var15, var14) : 0.0F;
            this.submitGlow(orderedRenderCommandQueue, var12, var17.glow, var20, var11);
         }

         return true;
      }
   }

   private static KnifeSkin.Sink sink(
      OrderedRenderCommandQueue orderedRenderCommandQueue, int i, int j, FirstPersonArm.Arm arm, boolean bl, KnifeMaterial knifeMaterial, boolean bl2
   ) {
      final RenderLayer var7 = RenderLayers.entityCutoutNoCull(KNIFE_TEXTURE);
      final int[] var8 = knifeMaterial.palette;
      return new KnifeSkin.Sink() {
         @Override
         public void part(MatrixStack matrixStack, KnifeMesh knifeMesh, KnifeMesh.Shine shine) {
            orderedRenderCommandQueue.submitCustom(matrixStack, var7, (entry, vertexConsumer) -> knifeMesh.emit(entry, vertexConsumer, var8, i, shine));
            if (bl2) {
               orderedRenderCommandQueue.getBatchingQueue(1)
                  .submitCustom(
                     matrixStack, RenderLayers.entityGlint(), (entry, vertexConsumer) -> knifeMesh.emit(entry, vertexConsumer, var8, i, KnifeMesh.Shine.NONE)
                  );
            }
         }

         @Override
         public void glow(MatrixStack matrixStack, KnifeMesh knifeMesh, KnifeMesh.Shine shine) {
            orderedRenderCommandQueue.getBatchingQueue(1)
               .submitCustom(
                  matrixStack,
                  RenderLayers.eyes(WeaponInspectModule.KNIFE_TEXTURE),
                  (entry, vertexConsumer) -> knifeMesh.emitGlow(entry, vertexConsumer, knifeMaterial.glow, shine)
               );
         }

         @Override
         public void arm(MatrixStack matrixStack, boolean bl) {
            if (arm != null && (!bl || bl)) {
               arm.draw(matrixStack, j < 0 != bl, orderedRenderCommandQueue, i);
            }
         }
      };
   }

   private static boolean offHandFree() {
      ClientPlayerEntity var0 = MinecraftClient.getInstance().player;
      return var0 != null && var0.getOffHandStack().isEmpty();
   }

   private void swordTrail(Matrix4f matrix4f, int i) {
      Matrix4f var3 = new Matrix4f(matrix4f)
         .translate(i * 1.13F / 16.0F, 0.2F, 0.070625F)
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

   private void record(long l, int i) {
      Vector3f var4 = new Vector3f();

      for (int var5 = 0; var5 < this.trails.length; var5++) {
         if (var5 >= i) {
            this.trails[var5].clear();
         } else {
            for (int var6 = var5 * 6; var6 < var5 * 6 + 6; var6 += 3) {
               this.baseInverse.transformPosition(this.trailSample[var6], this.trailSample[var6 + 1], this.trailSample[var6 + 2], var4);
               this.trailSample[var6] = var4.x;
               this.trailSample[var6 + 1] = var4.y;
               this.trailSample[var6 + 2] = var4.z;
            }

            this.trails[var5].add(l, this.trailSample, var5 * 6);
         }
      }
   }

   private void clearTrails() {
      for (SlashTrail var4 : this.trails) {
         var4.clear();
      }
   }

   private void submitGlow(OrderedRenderCommandQueue orderedRenderCommandQueue, long l, int i, float f, KnifeSkin knifeSkin) {
      long var7 = (long)(this.trailLength.get() * 1.0E9F);
      int var9 = knifeSkin != null ? knifeSkin.streakCount() : 1;
      boolean var10 = f > 0.0F;

      for (int var11 = 0; var11 < var9 && !var10; var11++) {
         var10 = !this.trails[var11].isEmpty(l, var7);
      }

      if (var10) {
         float var16 = this.trailSample[3];
         float var12 = this.trailSample[4];
         float var13 = this.trailSample[5];
         float var14 = (float)(l % 6283185307L) / 1.0E9F * 1.5F;
         MatrixStack var15 = new MatrixStack();
         var15.peek().getPositionMatrix().set(this.base);
         var15.peek().getNormalMatrix().set(this.baseNormal);
         orderedRenderCommandQueue.getBatchingQueue(1).submitCustom(var15, RenderLayers.eyes(KNIFE_TEXTURE), (entry, vertexConsumer) -> {
            for (int var15x = 0; var15x < var9; var15x++) {
               this.trails[var15x].emit(entry, vertexConsumer, l, var7, i, knifeSkin != null ? knifeSkin.streakStrength(var15x) : 1.0F);
            }

            if (f > 0.0F) {
               SlashTrail.sparkle(entry, vertexConsumer, var16, var12, var13, 0.07F * f, var14, i, f);
            }
         });
      }
   }
}
