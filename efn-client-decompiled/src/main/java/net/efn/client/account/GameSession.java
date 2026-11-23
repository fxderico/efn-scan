package net.efn.client.account;

import com.example.client.mixin.AccessorAccountsMinecraft;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserProperties;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class GameSession {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private YggdrasilAuthenticationService mojangAuth;

   GameSession() {
   }

   GameSession.Prepared prepare(AccountType var1, Credentials var2) {
      MinecraftClient var4 = MinecraftClient.method_1551();
      boolean var5 = var1 == AccountType.CRACKED;
      Session var6 = new Session(var2.name(), var2.uuid(), var2.accessToken(), Optional.empty(), Optional.empty());
      UserApiService var7 = var5 ? UserApiService.OFFLINE : this.mojangAuth(var4).createUserApiService(var2.accessToken());

      UserProperties var3;
      try {
         var3 = var7.fetchProperties();
      } catch (AuthenticationException var9) {
         LOGGER.warn("[EFN] Couldn't fetch user properties for {}", var2.name(), var9);
         var3 = UserApiService.OFFLINE_PROPERTIES;
      }

      ProfileResult var8 = var5 ? null : var4.method_73361().comp_837().fetchProfile(var2.uuid(), true);
      return new GameSession.Prepared(var6, var7, var3, var8, var5);
   }

   void apply(GameSession.Prepared var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      AccessorAccountsMinecraft var3 = (AccessorAccountsMinecraft)var2;
      var3.efn$setUser(var1.user());
      var3.efn$setUserApiService(var1.api());
      var3.efn$setUserPropertiesFuture(CompletableFuture.completedFuture(var1.properties()));
      var3.efn$setProfileFuture(CompletableFuture.completedFuture(var1.profile()));
      var3.efn$setPlayerSocialManager(new SocialInteractionsManager(var2, var1.api()));
      var3.efn$setProfileKeyPairManager(var1.offline() ? ProfileKeys.field_40800 : ProfileKeys.method_46532(var1.api(), var1.user(), var2.field_1697.toPath()));
      var3.efn$setReportingContext(AbuseReportContext.method_44599(ReporterEnvironment.method_44586(), var1.api()));
   }

   private YggdrasilAuthenticationService mojangAuth(MinecraftClient var1) {
      if (this.mojangAuth == null) {
         this.mojangAuth = new YggdrasilAuthenticationService(var1.method_1487());
      }

      return this.mojangAuth;
   }

   @Environment(EnvType.CLIENT)
   record Prepared(Session user, UserApiService api, UserProperties properties, ProfileResult profile, boolean offline) {
   }
}
