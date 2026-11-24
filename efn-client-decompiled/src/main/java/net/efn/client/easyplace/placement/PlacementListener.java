package net.efn.client.easyplace.placement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface PlacementListener {
   void feedback(String string, String string2, int i);
}
