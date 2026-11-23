package net.efn.client.util.staff;

import java.util.ArrayDeque;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundInstance.AttenuationType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

@Environment(EnvType.CLIENT)
public final class StaffAlarm {
   public static final String BEEP = "Beep";
   public static final String BELL = "Bell";
   public static final String GUARDIAN = "Guardian";
   public static final String OFF = "Off";
   private static final int SIREN_INTERVAL = 4;
   private static final float SIREN_HIGH = 1.5F;
   private static final float SIREN_LOW = 1.0F;
   private static final int BEEP_INTERVAL = 3;
   private static final float BEEP_PITCH = 1.6F;
   private static final int BELL_INTERVAL = 10;
   private static final int GUARDIAN_INTERVAL = 60;
   private static final int MAX_RINGING = 8;
   private String mode = "Off";
   private float volume;
   private int ticksLeft;
   private int step;
   private final ArrayDeque<SoundInstance> ringing = new ArrayDeque<>(8);

   public StaffAlarm() {
   }

   public void start(String string, float f, float g) {
      if (string != null && !"Off".equals(string) && !(g <= 0.0F) && !(f <= 0.0F)) {
         int var4 = Math.max(1, Math.round(f * 20.0F));
         this.volume = Math.min(1.0F, g);
         if (this.isPlaying() && string.equals(this.mode)) {
            this.ticksLeft = Math.max(this.ticksLeft, var4);
         } else {
            this.stop();
            this.mode = string;
            this.ticksLeft = var4;
            this.step = 0;
         }
      }
   }

   public void tick() {
      if (this.ticksLeft > 0) {
         int var1 = interval(this.mode);
         if (this.step % var1 == 0) {
            this.play(this.step / var1);
         }

         this.step++;
         if (--this.ticksLeft == 0) {
            this.step = 0;
         }
      }
   }

   public void stop() {
      this.ticksLeft = 0;
      this.step = 0;
      if (!this.ringing.isEmpty()) {
         SoundManager var1 = MinecraftClient.getInstance().getSoundManager();
         if (var1 != null) {
            this.ringing.forEach(soundInstance -> var1.stop(soundInstance));
         }

         this.ringing.clear();
      }
   }

   public boolean isPlaying() {
      return this.ticksLeft > 0;
   }

   private void play(int i) {
      SoundManager var3 = MinecraftClient.getInstance().getSoundManager();
      if (var3 != null) {
         String var5 = this.mode;
         SoundEvent var2;

         float var4 = switch (var5) {
            case "Beep" -> {
               var2 = SoundEvents.BLOCK_NOTE_BLOCK_BIT.value();
               yield 1.6F;
            }
            case "Bell" -> {
               var2 = SoundEvents.BLOCK_BELL_USE;
               yield 1.0F;
            }
            case "Guardian" -> {
               var2 = SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE;
               yield 1.0F;
            }
            default -> {
               var2 = SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
               yield i % 2 == 0 ? 1.5F : 1.0F;
            }
         };
         PositionedSoundInstance var7 = new PositionedSoundInstance(
            var2.id(), SoundCategory.MASTER, this.volume, var4, SoundInstance.createRandom(), false, 0, AttenuationType.NONE, 0.0, 0.0, 0.0, true
         );
         if (this.ringing.size() >= 8) {
            this.ringing.removeFirst();
         }

         this.ringing.addLast(var7);
         var3.play(var7);
      }
   }

   private static int interval(String string) {
      return switch (string) {
         case "Beep" -> 3;
         case "Bell" -> 10;
         case "Guardian" -> 60;
         default -> 4;
      };
   }
}
