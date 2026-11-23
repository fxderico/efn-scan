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
   private Visibility wanted = Visibility.field_2209;

   public ChallengeToast(ItemStack var1, Text var2, Text var3, boolean var4) {
      this(field_2208, var1, var2, var3, var4);
   }

   private ChallengeToast(Object var1, ItemStack var2, Text var3, Text var4, boolean var5) {
      this.token = var1;
      this.icon = var2.method_7972();
      this.title = var3;
      this.message = var4;
      this.sound = var5;
      this.fit();
   }

   public static void addOrUpdate(ItemStack var0, String var1, Text var2, boolean var3) {
      MinecraftClient var4 = MinecraftClient.method_1551();
      if (!var4.method_18854()) {
         var4.execute(() -> addOrUpdate(var0, var1, var2, var3));
      } else {
         ToastManager var5 = var4.method_1566();
         ChallengeToast var6 = (ChallengeToast)var5.method_1997(ChallengeToast.class, var1);
         if (var6 == null) {
            var5.method_1999(new ChallengeToast(var1, var0, Text.method_43470(var1), var2, var3));
         } else {
            var6.count++;
            var6.message = var2;
            var6.changed = true;
            var6.fit();
         }
      }
   }

   private String countLabel() {
      return this.count > 1 ? " x" + this.count : "";
   }

   private void fit() {
      TextRenderer var1 = MinecraftClient.method_1551().field_1772;
      int var2 = Math.max(
         var1.method_27525((StringVisitable)this.title) + var1.method_1727(this.countLabel()), var1.method_27525((StringVisitable)this.message)
      );
      this.width = MathHelper.method_15340(30 + var2 + 7, 160, 240);
      List var3 = this.message.getString().isEmpty() ? List.of() : var1.method_1728((StringVisitable)this.message, this.width - 30 - 7);
      this.lines = var3.size() > 5 ? List.copyOf(var3.subList(0, 5)) : var3;
      this.height = Math.max(32, 18 + this.lines.size() * 10 + 4);
   }

   public Object method_1987() {
      return this.token;
   }

   public SoundEvent method_66530() {
      return this.sound ? (SoundEvent)SoundEvents.field_14793.comp_349() : null;
   }

   public Visibility method_61988() {
      return this.wanted;
   }

   public int method_29049() {
      return this.width;
   }

   public int method_29050() {
      return this.height;
   }

   public float method_71808(int var1) {
      float var2 = var1 * 32.0F;
      float[] var3 = HudRenderer.infoBarBox();
      if (var3 == null) {
         return var2;
      } else {
         Window var4 = MinecraftClient.method_1551().method_22683();
         float var5 = var4.method_4495();
         float var6 = this.method_71809(var4.method_4486(), 1.0F);
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

   public void method_61989(ToastManager var1, long var2) {
      if (!this.hiding) {
         if (this.changed) {
            this.changedAt = var2;
            this.changed = false;
         }

         this.hiding = var2 - this.changedAt >= this.displayMillis() * var1.method_48221();
         this.wanted = this.hiding ? Visibility.field_2209 : Visibility.field_2210;
      }
   }

   public void method_71995() {
      if (this.changed) {
         this.hiding = false;
         MinecraftClient.method_1551().method_1566().method_1999(this);
      }
   }

   public void method_1986(DrawContext var1, TextRenderer var2, long var3) {
      int var5 = this.width - 3;
      int var6 = this.height - 1;
      rounded(var1, 0, 1, var5, var6, -266921698);
      this.drawTimer(var1, var3, var5, var6);
      boolean var7 = this.lines.isEmpty();
      this.drawTitle(var1, var2, var7 ? 12 : 7);

      for (int var8 = 0; var8 < this.lines.size(); var8++) {
         var1.method_51430(var2, rainbowed(this.lines.get(var8)), 30, 18 + var8 * 10, -5656902, true);
      }

      this.renderIcon(var1, this.icon);
   }

   protected void renderIcon(DrawContext var1, ItemStack var2) {
      var1.method_51445(var2, 8, 8);
   }

   private void drawTimer(DrawContext var1, long var2, int var4, int var5) {
      double var6 = this.displayMillis() * MinecraftClient.method_1551().method_1566().method_48221();
      float var8 = 1.0F - (float)MathHelper.method_15350((var2 - this.changedAt) / var6, 0.0, 1.0);
      byte var9 = 4;
      int var10 = var4 - 4;
      int var11 = var5 - 2;
      var1.method_25294(var9, var11, var10, var11 + 1, 654311423);
      var1.method_25294(var9, var11, var9 + Math.round((var10 - var9) * var8), var11 + 1, -1711276033);
   }

   private static void rounded(DrawContext var0, int var1, int var2, int var3, int var4, int var5) {
      var0.method_25294(var1 + 2, var2, var3 - 2, var2 + 1, var5);
      var0.method_25294(var1 + 1, var2 + 1, var3 - 1, var2 + 2, var5);
      var0.method_25294(var1, var2 + 2, var3, var4 - 2, var5);
      var0.method_25294(var1 + 1, var4 - 2, var3 - 1, var4 - 1, var5);
      var0.method_25294(var1 + 2, var4 - 1, var3 - 2, var4, var5);
   }

   private void drawTitle(DrawContext var1, TextRenderer var2, int var3) {
      String var4 = this.countLabel();
      int var5 = this.width - 30 - 7 - var2.method_1727(var4);
      OrderedText var6 = this.title.method_30937();
      if (var2.method_27525((StringVisitable)this.title) > var5) {
         MutableText var7 = Text.method_43470("\u2026");
         StringVisitable var8 = var2.method_1714((StringVisitable)this.title, var5 - var2.method_27525(var7));
         var6 = Language.method_10517().method_30934(StringVisitable.method_29433(new StringVisitable[]{var8, var7}));
      }

      var1.method_51430(var2, rainbowed(var6), 30, var3, -986891, true);
      if (!var4.isEmpty()) {
         var1.method_51433(var2, var4, 30 + var2.method_30880(var6), var3, -6643540, true);
      }
   }

   private static OrderedText rainbowed(OrderedText var0) {
      float var1 = (float)(Util.method_658() % 2500L) / 2500.0F;
      return var2 -> var0.accept((var2x, var3, var4) -> {
         if (!"efn:rainbow".equals(var3.method_10955())) {
            return var2.accept(var2x, var3, var4);
         } else {
            float var5 = var2x * 0.045F - var1;
            int var6 = MathHelper.method_15369(var5 - (float)Math.floor(var5), 0.75F, 1.0F);
            return var2.accept(var2x, var3.method_10975(null).method_27703(TextColor.method_27717(var6)), var4);
         }
      });
   }
}
