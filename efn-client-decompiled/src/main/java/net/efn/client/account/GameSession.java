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

   GameSession.Prepared prepare(AccountType accountType, Credentials credentials) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      boolean var5 = accountType == AccountType.CRACKED;
      Session var6 = new Session(credentials.name(), credentials.uuid(), credentials.accessToken(), Optional.empty(), Optional.empty());
      UserApiService var7 = var5 ? UserApiService.OFFLINE : this.mojangAuth(var4).createUserApiService(credentials.accessToken());

      UserProperties var3;
      try {
         var3 = var7.fetchProperties();
      } catch (AuthenticationException var9) {
         LOGGER.warn("[EFN] Couldn't fetch user properties for {}", credentials.name(), var9);
         var3 = UserApiService.OFFLINE_PROPERTIES;
      }

      ProfileResult var8 = var5 ? null : var4.getApiServices().sessionService().fetchProfile(credentials.uuid(), true);
      return new GameSession.Prepared(var6, var7, var3, var8, var5);
   }

   void apply(GameSession.Prepared prepared) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      AccessorAccountsMinecraft var3 = (AccessorAccountsMinecraft)var2;
      var3.efn$setUser(prepared.user());
      var3.efn$setUserApiService(prepared.api());
      var3.efn$setUserPropertiesFuture(CompletableFuture.completedFuture(prepared.properties()));
      var3.efn$setProfileFuture(CompletableFuture.completedFuture(prepared.profile()));
      var3.efn$setPlayerSocialManager(new SocialInteractionsManager(var2, prepared.api()));
      var3.efn$setProfileKeyPairManager(
         prepared.offline() ? ProfileKeys.MISSING : ProfileKeys.create(prepared.api(), prepared.user(), var2.runDirectory.toPath())
      );
      var3.efn$setReportingContext(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), prepared.api()));
   }

   private YggdrasilAuthenticationService mojangAuth(MinecraftClient minecraftClient) {
      if (this.mojangAuth == null) {
         this.mojangAuth = new YggdrasilAuthenticationService(minecraftClient.getNetworkProxy());
      }

      return this.mojangAuth;
   }

   @Environment(EnvType.CLIENT)
   record Prepared(Session user, UserApiService api, UserProperties properties, ProfileResult profile, boolean offline) {
   }
}
