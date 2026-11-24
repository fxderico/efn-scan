package net.efn.client.addon;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

@Environment(EnvType.CLIENT)
public abstract class EfnAddon {
   private String name = this.getClass().getSimpleName();
   private String version = "";
   private List<String> authors = List.of();
   String packageName;

   public EfnAddon() {
   }

   public abstract void onInitialize();

   public void onRegisterCategories() {
   }

   public void onConfigLoaded() {
   }

   public abstract String getPackage();

   public String getWebsite() {
      return null;
   }

   public EfnAddon.GithubRepo getRepo() {
      return null;
   }

   public final String name() {
      return this.name;
   }

   public final String version() {
      return this.version;
   }

   public final List<String> authors() {
      return this.authors;
   }

   final void bind(ModMetadata modMetadata) {
      this.name = modMetadata.getName();
      this.version = modMetadata.getVersion().getFriendlyString();
      ArrayList var2 = new ArrayList(modMetadata.getAuthors().size());

      for (Person var4 : modMetadata.getAuthors()) {
         var2.add(var4.getName());
      }

      this.authors = List.copyOf(var2);
   }

   @Environment(EnvType.CLIENT)
   public record GithubRepo(String owner, String name) {
      public String url() {
         return "https://github.com/" + this.owner + "/" + this.name;
      }
   }
}
