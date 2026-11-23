package net.efn.client.gui.mainmenu;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum MenuAction {
   SINGLEPLAYER("Singleplayer"),
   MULTIPLAYER("Multiplayer"),
   REALMS("Realms"),
   OPTIONS("Options"),
   QUIT("Quit"),
   LANGUAGE("Language"),
   ACCESSIBILITY("Accessibility"),
   EFN("EFN Client"),
   MODS("Mods Folder"),
   ACCOUNTS("Accounts"),
   CREDITS("Credits");

   public static final List<MenuAction> MAIN = List.of(SINGLEPLAYER, MULTIPLAYER, REALMS, OPTIONS, QUIT);
   private final String label;

   private MenuAction(String nullxx) {
      this.label = nullxx;
   }

   public String label() {
      return this.label;
   }
}
