package net.efn.client.easyplace.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({Mouse.class})
public interface AccessorMouseHandler {
   @Accessor("field_1789")
   void easyplace$setAccumulatedDX(double var1);

   @Accessor("field_1787")
   void easyplace$setAccumulatedDY(double var1);
}
