package net.efn.client.module;

import imgui.ImDrawList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface HudOverlayProvider {
   boolean isHudOverlayEnabled();

   void renderHudOverlay(ImDrawList imDrawList, float f, float g);
}
