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
   @Accessor("field_1726")
   void efn$setUser(Session var1);

   @Mutable
   @Accessor("field_26902")
   void efn$setUserApiService(UserApiService var1);

   @Mutable
   @Accessor("field_47680")
   void efn$setUserPropertiesFuture(CompletableFuture<UserProperties> var1);

   @Mutable
   @Accessor("field_45899")
   void efn$setProfileFuture(CompletableFuture<ProfileResult> var1);

   @Mutable
   @Accessor("field_26842")
   void efn$setPlayerSocialManager(SocialInteractionsManager var1);

   @Mutable
   @Accessor("field_39068")
   void efn$setProfileKeyPairManager(ProfileKeys var1);

   @Accessor("field_39492")
   void efn$setReportingContext(AbuseReportContext var1);
}
