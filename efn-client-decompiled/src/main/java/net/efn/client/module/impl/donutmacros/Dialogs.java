package net.efn.client.module.impl.donutmacros;

import com.example.client.mixin.AccessorOrderDialogScreen;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.dialog.DialogScreen;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.action.DialogAction.ValueGetter;
import net.minecraft.dialog.body.DialogBody;
import net.minecraft.dialog.body.ItemDialogBody;
import net.minecraft.dialog.body.PlainMessageDialogBody;
import net.minecraft.dialog.input.BooleanInputControl;
import net.minecraft.dialog.input.TextInputControl;
import net.minecraft.dialog.type.ColumnsDialog;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.dialog.type.DialogInput;
import net.minecraft.dialog.type.MultiActionDialog;
import net.minecraft.dialog.type.SimpleDialog;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

@Environment(EnvType.CLIENT)
final class Dialogs {
   private Dialogs() {
   }

   static Dialogs.View current(MinecraftClient minecraftClient) {
      if (!(minecraftClient.currentScreen instanceof DialogScreen var2)) {
         return null;
      } else {
         Dialog var3 = ((AccessorOrderDialogScreen)var2).efn$dialog();
         if (var3 == null) {
            return null;
         } else {
            ArrayList var4 = new ArrayList();
            if (var3 instanceof MultiActionDialog var5) {
               var4.addAll(var5.actions());
            }

            if (var3 instanceof SimpleDialog var11) {
               var4.addAll(var11.getButtons());
            }

            if (var3 instanceof ColumnsDialog var12) {
               var12.exitAction().ifPresent(var4::add);
            }

            ArrayList var13 = new ArrayList(var4.size());

            for (DialogActionButtonData var7 : var4) {
               Text var8 = var7.data().label();
               String var9 = visibleText(var8);
               var13.add(new Dialogs.Button(var7, ServerText.normalize(var9), ServerText.key(var9), var8));
            }

            ArrayList var14 = new ArrayList();
            ArrayList var15 = new ArrayList();

            for (DialogBody var18 : var3.common().body()) {
               if (var18 instanceof PlainMessageDialogBody var10) {
                  var14.add(ServerText.normalize(visibleText(var10.contents())));
               }

               if (var18 instanceof ItemDialogBody var20) {
                  var15.add(var20.item());
                  var20.description().ifPresent(plainMessageDialogBody -> var14.add(ServerText.normalize(visibleText(plainMessageDialogBody.contents()))));
               }
            }

            ArrayList var17 = new ArrayList();

            for (DialogInput var21 : var3.common().inputs()) {
               if (var21.control() instanceof TextInputControl) {
                  var17.add(var21.key());
               }
            }

            return new Dialogs.View(var3, ServerText.normalize(visibleText(var3.common().title())), var13, var14, var17, var15);
         }
      }
   }

   static String visibleText(Text text) {
      StringBuilder var1 = new StringBuilder();
      appendVisible(text, var1);
      return var1.toString();
   }

   private static void appendVisible(Text text, StringBuilder stringBuilder) {
      if (!(text.getContent() instanceof ObjectTextContent)) {
         text.getContent().visit(string -> {
            stringBuilder.append(string);
            return Optional.empty();
         });
      }

      for (Text var3 : text.getSiblings()) {
         appendVisible(var3, stringBuilder);
      }
   }

   static boolean press(MinecraftClient minecraftClient, Dialogs.View view, Dialogs.Button button, Map<String, String> map) {
      if (!(minecraftClient.currentScreen instanceof DialogScreen var5)) {
         return false;
      } else if (((AccessorOrderDialogScreen)var5).efn$dialog() != view.dialog()) {
         return false;
      } else {
         HashMap var6 = new HashMap();

         for (DialogInput var8 : view.dialog().common().inputs()) {
            if (var8.control() instanceof TextInputControl var10) {
               var6.put(var8.key(), ValueGetter.of(map.getOrDefault(var8.key(), var10.initial())));
            } else if (var8.control() instanceof BooleanInputControl var13) {
               var6.put(var8.key(), booleanValue(var13));
            }
         }

         Optional var11 = button.raw().action().flatMap(dialogAction -> dialogAction.createClickEvent(var6));
         var5.runAction(var11);
         return true;
      }
   }

   static void close(MinecraftClient minecraftClient) {
      if (minecraftClient.currentScreen instanceof DialogScreen var2) {
         var2.close();
      }
   }

   private static ValueGetter booleanValue(BooleanInputControl booleanInputControl) {
      final boolean var1 = booleanInputControl.initial();
      return new ValueGetter() {
         @Override
         public String get() {
            return var1 ? booleanInputControl.onTrue() : booleanInputControl.onFalse();
         }

         @Override
         public NbtElement getAsNbt() {
            return NbtByte.of(var1);
         }
      };
   }

   private static boolean isGreen(int i) {
      int var1 = i >> 16 & 0xFF;
      int var2 = i >> 8 & 0xFF;
      int var3 = i & 0xFF;
      return var2 >= 192 && var1 < 144 && var3 < 144;
   }

   @Environment(EnvType.CLIENT)
   record Button(DialogActionButtonData raw, String label, String key, Text component) {
      boolean green() {
         boolean[] var1 = new boolean[]{false};
         this.component.visit((style, string) -> {
            TextColor var3 = style.getColor();
            if (!string.isBlank() && var3 != null && Dialogs.isGreen(var3.getRgb())) {
               var1[0] = true;
            }

            return Optional.empty();
         }, Style.EMPTY);
         return var1[0];
      }
   }

   @Environment(EnvType.CLIENT)
   record View(Dialog dialog, String title, List<Dialogs.Button> buttons, List<String> body, List<String> textInputs, List<ItemStack> items) {
      Dialogs.Button find(Predicate<Dialogs.Button> predicate) {
         for (Dialogs.Button var3 : this.buttons) {
            if (predicate.test(var3)) {
               return var3;
            }
         }

         return null;
      }

      Dialogs.Button labelled(String... strings) {
         for (String var5 : strings) {
            String var6 = ServerText.key(var5);
            Dialogs.Button var7 = this.find(button -> button.key().equals(var6));
            if (var7 != null) {
               return var7;
            }
         }

         for (String var11 : strings) {
            String var12 = ServerText.key(var11);
            Dialogs.Button var13 = this.find(button -> button.key().contains(var12));
            if (var13 != null) {
               return var13;
            }
         }

         return null;
      }

      boolean titled(String string) {
         return this.title.contains(string);
      }

      String describe() {
         ArrayList var1 = new ArrayList();

         for (Dialogs.Button var3 : this.buttons) {
            if (!var3.label().isBlank()) {
               var1.add(var3.label());
            }

            if (var1.size() >= 12) {
               break;
            }
         }

         return "'" + this.title + "' buttons: " + (var1.isEmpty() ? "(none)" : String.join(", ", var1));
      }
   }
}
