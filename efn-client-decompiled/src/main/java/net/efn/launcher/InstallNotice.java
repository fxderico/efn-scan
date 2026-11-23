package net.efn.launcher;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public final class InstallNotice {
   private static final String TITLE = "EFN Client";
   private static final String MESSAGE = "EFN Client is a Fabric mod for Minecraft 1.21.11.\nPut this file in your mods folder and start the game with Fabric Loader.";

   private InstallNotice() {
   }

   public static void main(String[] var0) {
      if (GraphicsEnvironment.isHeadless()) {
         System.out.println("EFN Client is a Fabric mod for Minecraft 1.21.11.\nPut this file in your mods folder and start the game with Fabric Loader.");
      } else {
         try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (Exception var3) {
         }

         String[] var1 = new String[]{"Open Mods Folder", "Close"};
         int var2 = JOptionPane.showOptionDialog(
            null,
            "EFN Client is a Fabric mod for Minecraft 1.21.11.\nPut this file in your mods folder and start the game with Fabric Loader.",
            "EFN Client",
            -1,
            1,
            null,
            var1,
            var1[1]
         );
         if (var2 == 0) {
            openModsFolder();
         }

         System.exit(0);
      }
   }

   private static void openModsFolder() {
      File var0 = modsFolder();
      if (!var0.isDirectory()) {
         var0.mkdirs();
      }

      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.OPEN) && var0.isDirectory()) {
         try {
            Desktop.getDesktop().open(var0);
            return;
         } catch (IOException var2) {
         }
      }

      JOptionPane.showMessageDialog(null, "Your mods folder is:\n" + var0.getAbsolutePath(), "EFN Client", 1);
   }

   private static File modsFolder() {
      String var0 = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      String var1 = System.getProperty("user.home", ".");
      if (var0.contains("win")) {
         String var2 = System.getenv("APPDATA");
         return new File(new File(var2 != null ? var2 : var1, ".minecraft"), "mods");
      } else {
         return var0.contains("mac") ? new File(var1, "Library/Application Support/minecraft/mods") : new File(var1, ".minecraft/mods");
      }
   }
}
