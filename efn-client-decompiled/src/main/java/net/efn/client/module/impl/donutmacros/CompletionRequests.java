package net.efn.client.module.impl.donutmacros;

import java.util.List;
import net.efn.client.util.CompletionChannel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class CompletionRequests {
   private CompletionChannel.Form form = CompletionChannel.Form.BOTH;

   CompletionRequests() {
   }

   CompletionChannel.Form form() {
      return this.form;
   }

   void form(CompletionChannel.Form var1) {
      this.form = var1;
   }

   int fire(String var1) {
      return this.fire(var1, this.form);
   }

   int fire(String var1, CompletionChannel.Form var2) {
      return CompletionChannel.INSTANCE.fire(this, var1, var2);
   }

   List<String> poll(int var1) {
      return CompletionChannel.INSTANCE.poll(var1);
   }

   void forget(int var1) {
      CompletionChannel.INSTANCE.forget(var1);
   }

   void prune(long var1) {
      CompletionChannel.INSTANCE.prune(var1);
   }

   void clear() {
      CompletionChannel.INSTANCE.clear(this);
      this.form = CompletionChannel.Form.BOTH;
   }
}
