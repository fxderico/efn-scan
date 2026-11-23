package net.efn.client.gui;

import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AndroidSystemPicker {
   private AndroidSystemPicker() {
   }

   public static synchronized void open() {
   }

   public static synchronized Path takeSelection() {
      return null;
   }

   public static synchronized boolean blocksEditor() {
      return false;
   }

   public static synchronized boolean handleEscape() {
      return false;
   }

   public static synchronized void cancel() {
   }

   public static synchronized void render() {
   }
}
