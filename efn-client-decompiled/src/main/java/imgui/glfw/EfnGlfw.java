package imgui.glfw;

import imgui.EfnAndroidNative;
import imgui.ImGui;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharModsCallback;

@Environment(EnvType.CLIENT)
public class EfnGlfw extends ImGuiImplGlfw {
   private final boolean android;
   private GLFWCharModsCallback ownedCharMods;
   private GLFWCharModsCallback previousCharMods;
   private long charModsWindow;
   private boolean usingCharMods;
   private boolean reportedCharacters;
   private boolean wantedTextInput;

   public EfnGlfw() {
      this(EfnAndroidNative.isAndroidRuntime());
   }

   protected EfnGlfw(boolean var1) {
      this.android = var1;
   }

   public boolean init(long var1, boolean var3) {
      boolean var4 = super.init(var1, var3);
      if (var4 && this.android) {
         ImGui.getIO().removeBackendFlags(2);
         System.err.println("[EFN ImGui] Android cursor management delegated to launcher");
      }

      return var4;
   }

   protected void updateMouseCursor() {
      if (!this.android) {
         super.updateMouseCursor();
      }
   }

   public void installCallbacks(long var1) {
      super.installCallbacks(var1);
      if (this.android) {
         this.ownedCharMods = GLFWCharModsCallback.create(this::charModsCallback);

         try {
            this.previousCharMods = GLFW.glfwSetCharModsCallback(var1, this.ownedCharMods);
            this.charModsWindow = var1;
            this.usingCharMods = true;
            System.err.println("[EFN ImGui] Android text input: Minecraft character-with-modifiers channel enabled");
         } catch (LinkageError var4) {
            this.ownedCharMods.free();
            this.ownedCharMods = null;
            System.err
               .println("[EFN ImGui] Character-with-modifiers callback unavailable; keeping regular character input: " + var4.getClass().getSimpleName());
         }
      }
   }

   public void charModsCallback(long var1, int var3, int var4) {
      if (this.previousCharMods != null && this.shouldChainCallback(var1)) {
         this.previousCharMods.invoke(var1, var3, var4);
      }

      if (this.usingCharMods && var1 == this.charModsWindow) {
         ImGui.getIO().addInputCharacter(var3);
         if (!this.reportedCharacters) {
            System.err.println("[EFN ImGui] Android text character received");
            this.reportedCharacters = true;
         }
      }
   }

   public void charCallback(long var1, int var3) {
      if (!this.usingCharMods || var1 != this.charModsWindow) {
         super.charCallback(var1, var3);
      } else if (this.data.prevUserCallbackChar != null && this.shouldChainCallback(var1)) {
         this.data.prevUserCallbackChar.invoke(var1, var3);
      }
   }

   public void newFrame() {
      super.newFrame();
      boolean var1;
      if (this.usingCharMods && (var1 = ImGui.getIO().getWantTextInput()) != this.wantedTextInput) {
         System.err.println("[EFN ImGui] Text field " + (var1 ? "focused" : "unfocused"));
         this.wantedTextInput = var1;
      }
   }

   public void windowFocusCallback(long var1, boolean var3) {
      if (!var3) {
         EfnKeys.clear(var1);
      }

      super.windowFocusCallback(var1, var3);
   }

   public void restoreCallbacks(long var1) {
      EfnKeys.clear(var1);
      if (this.usingCharMods && var1 == this.charModsWindow) {
         GLFW.glfwSetCharModsCallback(var1, this.previousCharMods);
         this.usingCharMods = false;
         this.previousCharMods = null;
         this.charModsWindow = 0L;
         this.ownedCharMods.free();
         this.ownedCharMods = null;
         this.reportedCharacters = false;
         this.wantedTextInput = false;
      }

      super.restoreCallbacks(var1);
   }
}
