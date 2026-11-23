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

   static Dialogs.View current(MinecraftClient var0) {
      if (!(var0.field_1755 instanceof DialogScreen var2)) {
         return null;
      } else {
         Dialog var3 = ((AccessorOrderDialogScreen)var2).efn$dialog();
         if (var3 == null) {
            return null;
         } else {
            ArrayList var4 = new ArrayList();
            if (var3 instanceof MultiActionDialog var5) {
               var4.addAll(var5.comp_4317());
            }

            if (var3 instanceof SimpleDialog var11) {
               var4.addAll(var11.method_71698());
            }

            if (var3 instanceof ColumnsDialog var12) {
               var12.comp_4395().ifPresent(var4::add);
            }

            ArrayList var13 = new ArrayList(var4.size());

            for (DialogActionButtonData var7 : var4) {
               Text var8 = var7.comp_4390().comp_4298();
               String var9 = visibleText(var8);
               var13.add(new Dialogs.Button(var7, ServerText.normalize(var9), ServerText.key(var9), var8));
            }

            ArrayList var14 = new ArrayList();
            ArrayList var15 = new ArrayList();

            for (DialogBody var18 : var3.comp_4305().comp_4304()) {
               if (var18 instanceof PlainMessageDialogBody var10) {
                  var14.add(ServerText.normalize(visibleText(var10.comp_4330())));
               }

               if (var18 instanceof ItemDialogBody var20) {
                  var15.add(var20.comp_4324());
                  var20.comp_4325().ifPresent(var1 -> var14.add(ServerText.normalize(visibleText(var1.comp_4330()))));
               }
            }

            ArrayList var17 = new ArrayList();

            for (DialogInput var21 : var3.comp_4305().comp_4394()) {
               if (var21.comp_4313() instanceof TextInputControl) {
                  var17.add(var21.comp_4312());
               }
            }

            return new Dialogs.View(var3, ServerText.normalize(visibleText(var3.comp_4305().comp_4301())), var13, var14, var17, var15);
         }
      }
   }

   static String visibleText(Text var0) {
      StringBuilder var1 = new StringBuilder();
      appendVisible(var0, var1);
      return var1.toString();
   }

   private static void appendVisible(Text var0, StringBuilder var1) {
      if (!(var0.method_10851() instanceof ObjectTextContent)) {
         var0.method_10851().method_27659(var1x -> {
            var1.append(var1x);
            return Optional.empty();
         });
      }

      for (Text var3 : var0.method_10855()) {
         appendVisible(var3, var1);
      }
   }

   static boolean press(MinecraftClient var0, Dialogs.View var1, Dialogs.Button var2, Map<String, String> var3) {
      if (!(var0.field_1755 instanceof DialogScreen var5)) {
         return false;
      } else if (((AccessorOrderDialogScreen)var5).efn$dialog() != var1.dialog()) {
         return false;
      } else {
         HashMap var6 = new HashMap();

         for (DialogInput var8 : var1.dialog().comp_4305().comp_4394()) {
            if (var8.comp_4313() instanceof TextInputControl var10) {
               var6.put(var8.comp_4312(), ValueGetter.method_72070(var3.getOrDefault(var8.comp_4312(), var10.comp_4354())));
            } else if (var8.comp_4313() instanceof BooleanInputControl var13) {
               var6.put(var8.comp_4312(), booleanValue(var13));
            }
         }

         Optional var11 = var2.raw().comp_4391().flatMap(var1x -> var1x.method_72068(var6));
         var5.method_72141(var11);
         return true;
      }
   }

   static void close(MinecraftClient var0) {
      if (var0.field_1755 instanceof DialogScreen var2) {
         var2.method_25419();
      }
   }

   private static ValueGetter booleanValue(final BooleanInputControl var0) {
      final boolean var1 = var0.comp_4333();
      return new ValueGetter() {
         public String method_72069() {
            return var1 ? var0.comp_4334() : var0.comp_4335();
         }

         public NbtElement method_72073() {
            return NbtByte.method_23234(var1);
         }
      };
   }

   private static boolean isGreen(int var0) {
      int var1 = var0 >> 16 & 0xFF;
      int var2 = var0 >> 8 & 0xFF;
      int var3 = var0 & 0xFF;
      return var2 >= 192 && var1 < 144 && var3 < 144;
   }

   @Environment(EnvType.CLIENT)
   record Button(DialogActionButtonData raw, String label, String key, Text component) {
      boolean green() {
         boolean[] var1 = new boolean[]{false};
         this.component.method_27658((var1x, var2) -> {
            TextColor var3 = var1x.method_10973();
            if (!var2.isBlank() && var3 != null && Dialogs.isGreen(var3.method_27716())) {
               var1[0] = true;
            }

            return Optional.empty();
         }, Style.field_24360);
         return var1[0];
      }
   }

   @Environment(EnvType.CLIENT)
   record View(Dialog dialog, String title, List<Dialogs.Button> buttons, List<String> body, List<String> textInputs, List<ItemStack> items) {
      Dialogs.Button find(Predicate<Dialogs.Button> var1) {
         for (Dialogs.Button var3 : this.buttons) {
            if (var1.test(var3)) {
               return var3;
            }
         }

         return null;
      }

      Dialogs.Button labelled(String... var1) {
         for (String var5 : var1) {
            String var6 = ServerText.key(var5);
            Dialogs.Button var7 = this.find(var1x -> var1x.key().equals(var6));
            if (var7 != null) {
               return var7;
            }
         }

         for (String var11 : var1) {
            String var12 = ServerText.key(var11);
            Dialogs.Button var13 = this.find(var1x -> var1x.key().contains(var12));
            if (var13 != null) {
               return var13;
            }
         }

         return null;
      }

      boolean titled(String var1) {
         return this.title.contains(var1);
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
