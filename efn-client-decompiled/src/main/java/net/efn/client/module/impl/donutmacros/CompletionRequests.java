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

   void form(CompletionChannel.Form form) {
      this.form = form;
   }

   int fire(String string) {
      return this.fire(string, this.form);
   }

   int fire(String string, CompletionChannel.Form form) {
      return CompletionChannel.INSTANCE.fire(this, string, form);
   }

   List<String> poll(int i) {
      return CompletionChannel.INSTANCE.poll(i);
   }

   void forget(int i) {
      CompletionChannel.INSTANCE.forget(i);
   }

   void prune(long l) {
      CompletionChannel.INSTANCE.prune(l);
   }

   void clear() {
      CompletionChannel.INSTANCE.clear(this);
      this.form = CompletionChannel.Form.BOTH;
   }
}
