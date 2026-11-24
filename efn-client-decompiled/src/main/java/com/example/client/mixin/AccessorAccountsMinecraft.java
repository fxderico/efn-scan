package com.example.client.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserProperties;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({MinecraftClient.class})
public interface AccessorAccountsMinecraft {
   @Mutable
   @Accessor("session")
   void efn$setUser(Session session);

   @Mutable
   @Accessor("userApiService")
   void efn$setUserApiService(UserApiService userApiService);

   @Mutable
   @Accessor("userPropertiesFuture")
   void efn$setUserPropertiesFuture(CompletableFuture<UserProperties> completableFuture);

   @Mutable
   @Accessor("gameProfileFuture")
   void efn$setProfileFuture(CompletableFuture<ProfileResult> completableFuture);

   @Mutable
   @Accessor("socialInteractionsManager")
   void efn$setPlayerSocialManager(SocialInteractionsManager socialInteractionsManager);

   @Mutable
   @Accessor("profileKeys")
   void efn$setProfileKeyPairManager(ProfileKeys profileKeys);

   @Accessor("abuseReportContext")
   void efn$setReportingContext(AbuseReportContext abuseReportContext);
}
