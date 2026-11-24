package net.efn.client.module.impl.movement.elytrafly;

import net.efn.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class VanillaMode extends ElytraFlightMode {
   public VanillaMode(ElytraFlyModule elytraFlyModule) {
      super(elytraFlyModule);
   }
}
