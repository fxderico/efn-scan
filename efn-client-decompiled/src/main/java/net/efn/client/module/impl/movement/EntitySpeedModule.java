package net.efn.client.module.impl.movement;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;

@Environment(EnvType.CLIENT)
public final class EntitySpeedModule extends ClientModule {
   public static volatile EntitySpeedModule active;
   private final NumberSetting horizontalSpeed = this.addSetting("horizontal-speed", new NumberSetting("Horizontal Speed", 10.0F, 0.0F, 50.0F).step(0.5F));
   private final BooleanSetting onlyOnGround = this.addSetting("only-on-ground", new BooleanSetting("Only On Ground", false));
   private final BooleanSetting inWater = this.addSetting("in-water", new BooleanSetting("In Water", true));

   public EntitySpeedModule() {
      super("entity-speed", "Entity Speed", "Makes you go faster horizontally when riding entities.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      active = this;
      EntityControlVehicleHook.refresh();
   }

   @Override
   protected void onDisable() {
      active = null;
      EntityControlVehicleHook.refresh();
   }

   public boolean appliesTo(Entity var1) {
      return (!this.onlyOnGround.get() || var1.method_24828() || var1.method_70987()) && (this.inWater.get() || !var1.method_5799());
   }

   public double horizontalSpeed() {
      return this.horizontalSpeed.get();
   }
}
