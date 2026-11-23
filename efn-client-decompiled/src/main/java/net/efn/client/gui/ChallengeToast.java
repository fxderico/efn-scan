package net.efn.client.gui;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.toast.Toast.Visibility;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Language;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class ChallengeToast implements Toast {
   public static final String RAINBOW = "efn:rainbow";
   private static final long RAINBOW_MILLIS = 2500L;
   private static final int BACKGROUND = -266921698;
   private static final int TITLE_COLOR = -986891;
   private static final int MESSAGE_COLOR = -5656902;
   private static final int COUNT_COLOR = -6643540;
   private static final int TIMER_TRACK = 654311423;
   private static final int TIMER_FILL = -1711276033;
   private static final int TEXT_X = 30;
   private static final int TEXT_PAD = 7;
   private static final int RIGHT_GAP = 3;
   private static final int MAX_WIDTH = 240;
   private static final int LINE_HEIGHT = 10;
   private static final int MAX_LINES = 5;
   private static final long DISPLAY_MILLIS = 5000L;
   private final Object token;
   private final ItemStack icon;
   private final boolean sound;
   private Text title;
   private Text message;
   private int width;
   private int height = 32;
   private List<OrderedText> lines = List.of();
   private int count = 1;
   private boolean changed;
   private boolean hiding;
   private long changedAt;
   private Visibility wanted = Visibility.HIDE;

   public ChallengeToast(ItemStack itemStack, Text text, Text text2, boolean bl) {
      this(TYPE, itemStack, text, text2, bl);
   }

   private ChallengeToast(Object object, ItemStack itemStack, Text text, Text text2, boolean bl) {
      this.token = object;
      this.icon = itemStack.copy();
      this.title = text;
      this.message = text2;
      this.sound = bl;
      this.fit();
   }

   public static void addOrUpdate(ItemStack itemStack, String string, Text text, boolean bl) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      if (!var4.isOnThread()) {
         var4.execute(() -> addOrUpdate(itemStack, string, text, bl));
      } else {
         ToastManager var5 = var4.getToastManager();
         ChallengeToast var6 = var5.getToast(ChallengeToast.class, string);
         if (var6 == null) {
            var5.add(new ChallengeToast(string, itemStack, Text.literal(string), text, bl));
         } else {
            var6.count++;
            var6.message = text;
            var6.changed = true;
            var6.fit();
         }
      }
   }

   private String countLabel() {
      return this.count > 1 ? " x" + this.count : "";
   }

   private void fit() {
      TextRenderer var1 = MinecraftClient.getInstance().textRenderer;
      int var2 = Math.max(var1.getWidth(this.title) + var1.getWidth(this.countLabel()), var1.getWidth(this.message));
      this.width = MathHelper.clamp(30 + var2 + 7, 160, 240);
      List var3 = this.message.getString().isEmpty() ? List.of() : var1.wrapLines(this.message, this.width - 30 - 7);
      this.lines = var3.size() > 5 ? List.copyOf(var3.subList(0, 5)) : var3;
      this.height = Math.max(32, 18 + this.lines.size() * 10 + 4);
   }

   @Override
   public Object getType() {
      return this.token;
   }

   @Override
   public SoundEvent getSoundEvent() {
      return this.sound ? SoundEvents.BLOCK_NOTE_BLOCK_BELL.value() : null;
   }

   @Override
   public Visibility getVisibility() {
      return this.wanted;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public float getYPos(int topIndex) {
      float var2 = topIndex * 32.0F;
      float[] var3 = HudRenderer.infoBarBox();
      if (var3 == null) {
         return var2;
      } else {
         Window var4 = MinecraftClient.getInstance().getWindow();
         float var5 = var4.getScaleFactor();
         float var6 = this.getXPos(var4.getScaledWidth(), 1.0F);
         float var7 = var6 + this.width;
         float var8 = var3[0] / var5;
         float var9 = var3[1] / var5;
         float var10 = var3[2] / var5;
         float var11 = var3[3] / var5;
         return !(var9 > 32.0F) && !(var7 <= var8) && !(var6 >= var10) ? var2 + var11 + 2.0F : var2;
      }
   }

   protected long displayMillis() {
      return 5000L;
   }

   protected final int cardRight() {
      return this.width - 3;
   }

   protected final int cardBottom() {
      return this.height - 1;
   }

   @Override
   public void update(ToastManager manager, long time) {
      if (!this.hiding) {
         if (this.changed) {
            this.changedAt = time;
            this.changed = false;
         }

         this.hiding = time - this.changedAt >= this.displayMillis() * manager.getNotificationDisplayTimeMultiplier();
         this.wanted = this.hiding ? Visibility.HIDE : Visibility.SHOW;
      }
   }

   @Override
   public void onFinishedRendering() {
      if (this.changed) {
         this.hiding = false;
         MinecraftClient.getInstance().getToastManager().add(this);
      }
   }

   @Override
   public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
      int var5 = this.width - 3;
      int var6 = this.height - 1;
      rounded(context, 0, 1, var5, var6, -266921698);
      this.drawTimer(context, startTime, var5, var6);
      boolean var7 = this.lines.isEmpty();
      this.drawTitle(context, textRenderer, var7 ? 12 : 7);

      for (int var8 = 0; var8 < this.lines.size(); var8++) {
         context.drawText(textRenderer, rainbowed(this.lines.get(var8)), 30, 18 + var8 * 10, -5656902, true);
      }

      this.renderIcon(context, this.icon);
   }

   protected void renderIcon(DrawContext drawContext, ItemStack itemStack) {
      drawContext.drawItemWithoutEntity(itemStack, 8, 8);
   }

   private void drawTimer(DrawContext drawContext, long l, int i, int j) {
      double var6 = this.displayMillis() * MinecraftClient.getInstance().getToastManager().getNotificationDisplayTimeMultiplier();
      float var8 = 1.0F - (float)MathHelper.clamp((l - this.changedAt) / var6, 0.0, 1.0);
      byte var9 = 4;
      int var10 = i - 4;
      int var11 = j - 2;
      drawContext.fill(var9, var11, var10, var11 + 1, 654311423);
      drawContext.fill(var9, var11, var9 + Math.round((var10 - var9) * var8), var11 + 1, -1711276033);
   }

   private static void rounded(DrawContext drawContext, int i, int j, int k, int l, int m) {
      drawContext.fill(i + 2, j, k - 2, j + 1, m);
      drawContext.fill(i + 1, j + 1, k - 1, j + 2, m);
      drawContext.fill(i, j + 2, k, l - 2, m);
      drawContext.fill(i + 1, l - 2, k - 1, l - 1, m);
      drawContext.fill(i + 2, l - 1, k - 2, l, m);
   }

   private void drawTitle(DrawContext drawContext, TextRenderer textRenderer, int i) {
      String var4 = this.countLabel();
      int var5 = this.width - 30 - 7 - textRenderer.getWidth(var4);
      OrderedText var6 = this.title.asOrderedText();
      if (textRenderer.getWidth(this.title) > var5) {
         MutableText var7 = Text.literal("\u2026");
         StringVisitable var8 = textRenderer.trimToWidth(this.title, var5 - textRenderer.getWidth(var7));
         var6 = Language.getInstance().reorder(StringVisitable.concat(var8, var7));
      }

      drawContext.drawText(textRenderer, rainbowed(var6), 30, i, -986891, true);
      if (!var4.isEmpty()) {
         drawContext.drawText(textRenderer, var4, 30 + textRenderer.getWidth(var6), i, -6643540, true);
      }
   }

   private static OrderedText rainbowed(OrderedText orderedText) {
      float var1 = (float)(Util.getMeasuringTimeMs() % 2500L) / 2500.0F;
      return characterVisitor -> orderedText.accept((i, style, j) -> {
         if (!"efn:rainbow".equals(style.getInsertion())) {
            return characterVisitor.accept(i, style, j);
         } else {
            float var5 = i * 0.045F - var1;
            int var6 = MathHelper.hsvToRgb(var5 - (float)Math.floor(var5), 0.75F, 1.0F);
            return characterVisitor.accept(i, style.withInsertion(null).withColor(TextColor.fromRgb(var6)), j);
         }
      });
   }
}
