package net.efn.client.addon;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MenuPanels {
   private static volatile Supplier<? extends MenuPanels.Panel> accounts;

   private MenuPanels() {
   }

   public static void registerAccounts(Supplier<? extends MenuPanels.Panel> var0) {
      accounts = var0;
   }

   public static MenuPanels.Panel newAccountsPanel() {
      Supplier var0 = accounts;
      return var0 == null ? null : (MenuPanels.Panel)var0.get();
   }

   @Environment(EnvType.CLIENT)
   public interface Panel {
      void open();

      boolean isOpen();

      boolean keyPressed(int var1);

      void render(float var1, float var2, float var3, float var4);
   }
}
