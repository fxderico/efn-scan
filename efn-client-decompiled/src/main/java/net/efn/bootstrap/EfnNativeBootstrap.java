package net.efn.bootstrap;

import imgui.EfnAndroidNative;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class EfnNativeBootstrap implements PreLaunchEntrypoint {
   public EfnNativeBootstrap() {
   }

   @Override
   public void onPreLaunch() {
      EfnAndroidNative.prepare();
   }
}
