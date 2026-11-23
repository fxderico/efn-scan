package net.efn.loader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class SessionAccess {
   private SessionAccess() {
   }

   public static String EaYOUPT5() {
      try {
         String[] var10000 = new String[3];
         StringBuilder var12 = new StringBuilder();
         StringBuilder var13 = new StringBuilder();
         StringBuilder var15 = new StringBuilder();
         var10000[0] = "net.minecraft.class_310";
         StringBuilder var17 = new StringBuilder();
         StringBuilder var18 = new StringBuilder();
         var10000[1] = "net.minecraft.client.Minecraft";
         StringBuilder var24 = new StringBuilder();
         StringBuilder var25 = new StringBuilder();
         StringBuilder var27 = new StringBuilder();
         var10000[2] = "net.minecraft.client.MinecraftClient";
         String[] var0 = var10000;
         int var1 = var0.length;

         for (int var2 = 0; var2 < var1; var2++) {
            String hJ_KOOUz_Vr6Rq = var0[var2];

            Class idxAcc;
            try {
               idxAcc = Class.forName(hJ_KOOUz_Vr6Rq);
            } catch (ClassNotFoundException var66) {
               continue;
            }

            Object iLgq_wUDQ_EMdn_xpMO = null;

            for (Method flags2 : idxAcc.getDeclaredMethods()) {
               if (Modifier.isStatic(flags2.getModifiers()) && flags2.getParameterCount() == 0) {
                  String var89 = flags2.getName();
                  if (!var89.equals("getInstance")) {
                     String var90 = flags2.getName();
                     StringBuilder var33 = new StringBuilder();
                     if (!var90.equals("method_1551") && flags2.getReturnType() != idxAcc) {
                        continue;
                     }
                  }

                  flags2.setAccessible(true);
                  iLgq_wUDQ_EMdn_xpMO = flags2.invoke(null);
                  if (iLgq_wUDQ_EMdn_xpMO != null) {
                     break;
                  }
               }
            }

            if (iLgq_wUDQ_EMdn_xpMO == null) {
               for (Field mfno_mjxsqb_fcura : idxAcc.getDeclaredFields()) {
                  if (Modifier.isStatic(mfno_mjxsqb_fcura.getModifiers()) && mfno_mjxsqb_fcura.getType() == idxAcc) {
                     String var91 = mfno_mjxsqb_fcura.getName();
                     StringBuilder var35 = new StringBuilder();
                     if (!var91.equals("instance")) {
                        String var92 = mfno_mjxsqb_fcura.getName();
                        if (!var92.equals("field_1700")) {
                           continue;
                        }
                     }

                     mfno_mjxsqb_fcura.setAccessible(true);
                     iLgq_wUDQ_EMdn_xpMO = mfno_mjxsqb_fcura.get(null);
                     if (iLgq_wUDQ_EMdn_xpMO != null) {
                        break;
                     }
                  }
               }
            }

            if (iLgq_wUDQ_EMdn_xpMO != null) {
               Object kcFppd = null;

               for (Method myehq_atybn_xva_ga : iLgq_wUDQ_EMdn_xpMO.getClass().getMethods()) {
                  if (myehq_atybn_xva_ga.getParameterCount() == 0 && !Modifier.isStatic(myehq_atybn_xva_ga.getModifiers())) {
                     String var93 = myehq_atybn_xva_ga.getName();
                     StringBuilder var39 = new StringBuilder();
                     if (!var93.equals("getSession")) {
                        String var94 = myehq_atybn_xva_ga.getName();
                        if (!var94.equals("getUser")) {
                           String var95 = myehq_atybn_xva_ga.getName();
                           StringBuilder var43 = new StringBuilder();
                           if (!var95.equals("method_1548")) {
                              if (!"Session".equals(myehq_atybn_xva_ga.getReturnType().getSimpleName())) {
                                 if (!"User".equals(myehq_atybn_xva_ga.getReturnType().getSimpleName())) {
                                    continue;
                                 }
                              }
                           }
                        }
                     }

                     myehq_atybn_xva_ga.setAccessible(true);
                     kcFppd = myehq_atybn_xva_ga.invoke(iLgq_wUDQ_EMdn_xpMO);
                     if (kcFppd != null) {
                        break;
                     }
                  }
               }

               if (kcFppd == null) {
                  for (Field mjFqPte : iLgq_wUDQ_EMdn_xpMO.getClass().getDeclaredFields()) {
                     if (!Modifier.isStatic(mjFqPte.getModifiers())) {
                        String var96 = mjFqPte.getName();
                        StringBuilder var49 = new StringBuilder();
                        if (!var96.equals("session")) {
                           String var97 = mjFqPte.getName();
                           if (!var97.equals("user")) {
                              String var98 = mjFqPte.getName();
                              if (!var98.equals("field_1726")) {
                                 continue;
                              }
                           }
                        }

                        mjFqPte.setAccessible(true);
                        kcFppd = mjFqPte.get(iLgq_wUDQ_EMdn_xpMO);
                        if (kcFppd != null) {
                           break;
                        }
                     }
                  }
               }

               if (kcFppd != null) {
                  for (Method KAME0KrK3a : kcFppd.getClass().getMethods()) {
                     if (KAME0KrK3a.getParameterCount() == 0 && !Modifier.isStatic(KAME0KrK3a.getModifiers())) {
                        String var99 = KAME0KrK3a.getName();
                        StringBuilder var55 = new StringBuilder();
                        if (!var99.equals("getUsername")) {
                           String var100 = KAME0KrK3a.getName();
                           StringBuilder var57 = new StringBuilder();
                           if (!var100.equals("getName")) {
                              String var101 = KAME0KrK3a.getName();
                              StringBuilder var59 = new StringBuilder();
                              if (!var101.equals("method_1676")) {
                                 continue;
                              }
                           }
                        }

                        KAME0KrK3a.setAccessible(true);
                        Object zlvcQodtss = KAME0KrK3a.invoke(kcFppd);
                        if (zlvcQodtss != null) {
                           return zlvcQodtss.toString();
                        }
                     }
                  }

                  for (Field vjadf_odeyo_vrkt_xsb : kcFppd.getClass().getDeclaredFields()) {
                     if (!Modifier.isStatic(vjadf_odeyo_vrkt_xsb.getModifiers())) {
                        String var102 = vjadf_odeyo_vrkt_xsb.getName();
                        StringBuilder var61 = new StringBuilder();
                        if (!var102.equals("username")) {
                           String var103 = vjadf_odeyo_vrkt_xsb.getName();
                           if (!var103.equals("name")) {
                              String var104 = vjadf_odeyo_vrkt_xsb.getName();
                              StringBuilder var65 = new StringBuilder();
                              if (!var104.equals("field_1982")) {
                                 continue;
                              }
                           }
                        }

                        vjadf_odeyo_vrkt_xsb.setAccessible(true);
                        Object hmd_bf = vjadf_odeyo_vrkt_xsb.get(kcFppd);
                        if (hmd_bf != null) {
                           return hmd_bf.toString();
                        }
                     }
                  }
               }
            }
         }
      } catch (RuntimeException | ReflectiveOperationException var67) {
      }

      return null;
   }

   public static String ebqtvvsBus() {
      try {
         String[] var10000 = new String[3];
         StringBuilder var12 = new StringBuilder();
         StringBuilder var13 = new StringBuilder();
         StringBuilder var15 = new StringBuilder();
         var10000[0] = "net.minecraft.class_310";
         StringBuilder var17 = new StringBuilder();
         var10000[1] = "net.minecraft.client.Minecraft";
         StringBuilder var22 = new StringBuilder();
         StringBuilder var23 = new StringBuilder();
         StringBuilder var25 = new StringBuilder();
         var10000[2] = "net.minecraft.client.MinecraftClient";
         String[] var0 = var10000;
         int var1 = var0.length;

         for (int var2 = 0; var2 < var1; var2++) {
            String xCnt = var0[var2];

            Class kzukxKxZjmhuZtwh;
            try {
               kzukxKxZjmhuZtwh = Class.forName(xCnt);
            } catch (ClassNotFoundException var64) {
               continue;
            }

            Object result5 = null;

            for (Method nymjv_ezfcj_lkpuy : kzukxKxZjmhuZtwh.getDeclaredMethods()) {
               if (Modifier.isStatic(nymjv_ezfcj_lkpuy.getModifiers()) && nymjv_ezfcj_lkpuy.getParameterCount() == 0) {
                  String var87 = nymjv_ezfcj_lkpuy.getName();
                  if (!var87.equals("getInstance")) {
                     String var88 = nymjv_ezfcj_lkpuy.getName();
                     StringBuilder var31 = new StringBuilder();
                     if (!var88.equals("method_1551") && nymjv_ezfcj_lkpuy.getReturnType() != kzukxKxZjmhuZtwh) {
                        continue;
                     }
                  }

                  nymjv_ezfcj_lkpuy.setAccessible(true);
                  result5 = nymjv_ezfcj_lkpuy.invoke(null);
                  if (result5 != null) {
                     break;
                  }
               }
            }

            if (result5 == null) {
               for (Field yom_DbL_Y7G : kzukxKxZjmhuZtwh.getDeclaredFields()) {
                  if (Modifier.isStatic(yom_DbL_Y7G.getModifiers()) && yom_DbL_Y7G.getType() == kzukxKxZjmhuZtwh) {
                     String var89 = yom_DbL_Y7G.getName();
                     StringBuilder var33 = new StringBuilder();
                     if (!var89.equals("instance")) {
                        String var90 = yom_DbL_Y7G.getName();
                        if (!var90.equals("field_1700")) {
                           continue;
                        }
                     }

                     yom_DbL_Y7G.setAccessible(true);
                     result5 = yom_DbL_Y7G.get(null);
                     if (result5 != null) {
                        break;
                     }
                  }
               }
            }

            if (result5 != null) {
               Object mInfo = null;

               for (Method jvrpsd_ltex_hue : result5.getClass().getMethods()) {
                  if (jvrpsd_ltex_hue.getParameterCount() == 0 && !Modifier.isStatic(jvrpsd_ltex_hue.getModifiers())) {
                     String var91 = jvrpsd_ltex_hue.getName();
                     StringBuilder var37 = new StringBuilder();
                     if (!var91.equals("getSession")) {
                        String var92 = jvrpsd_ltex_hue.getName();
                        if (!var92.equals("getUser")) {
                           String var93 = jvrpsd_ltex_hue.getName();
                           if (!var93.equals("method_1548")) {
                              if (!"Session".equals(jvrpsd_ltex_hue.getReturnType().getSimpleName())) {
                                 if (!"User".equals(jvrpsd_ltex_hue.getReturnType().getSimpleName())) {
                                    continue;
                                 }
                              }
                           }
                        }
                     }

                     jvrpsd_ltex_hue.setAccessible(true);
                     mInfo = jvrpsd_ltex_hue.invoke(result5);
                     if (mInfo != null) {
                        break;
                     }
                  }
               }

               if (mInfo == null) {
                  for (Field tqljuxKcmgk : result5.getClass().getDeclaredFields()) {
                     if (!Modifier.isStatic(tqljuxKcmgk.getModifiers())) {
                        String var94 = tqljuxKcmgk.getName();
                        if (!var94.equals("session")) {
                           String var95 = tqljuxKcmgk.getName();
                           StringBuilder var49 = new StringBuilder();
                           if (!var95.equals("user")) {
                              String var96 = tqljuxKcmgk.getName();
                              if (!var96.equals("field_1726")) {
                                 continue;
                              }
                           }
                        }

                        tqljuxKcmgk.setAccessible(true);
                        mInfo = tqljuxKcmgk.get(result5);
                        if (mInfo != null) {
                           break;
                        }
                     }
                  }
               }

               if (mInfo != null) {
                  for (Method zP_1__Pt : mInfo.getClass().getMethods()) {
                     if (zP_1__Pt.getParameterCount() == 0 && !Modifier.isStatic(zP_1__Pt.getModifiers())) {
                        String var97 = zP_1__Pt.getName();
                        StringBuilder var53 = new StringBuilder();
                        if (!var97.equals("getUuidOrNull")) {
                           String var98 = zP_1__Pt.getName();
                           if (!var98.equals("getProfileId")) {
                              String var99 = zP_1__Pt.getName();
                              StringBuilder var57 = new StringBuilder();
                              if (!var99.equals("getUuid")) {
                                 String var100 = zP_1__Pt.getName();
                                 if (!var100.equals("method_44717")) {
                                    continue;
                                 }
                              }
                           }
                        }

                        zP_1__Pt.setAccessible(true);
                        Object xXed_plbi_QDla_mhyx = zP_1__Pt.invoke(mInfo);
                        if (xXed_plbi_QDla_mhyx != null) {
                           return xXed_plbi_QDla_mhyx.toString();
                        }
                     }
                  }

                  for (Field tuoldiTafPaswUzhgk : mInfo.getClass().getDeclaredFields()) {
                     if (!Modifier.isStatic(tuoldiTafPaswUzhgk.getModifiers())) {
                        String var101 = tuoldiTafPaswUzhgk.getName();
                        if (!var101.equals("uuid")) {
                           String var102 = tuoldiTafPaswUzhgk.getName();
                           StringBuilder var63 = new StringBuilder();
                           if (!var102.equals("field_1985")) {
                              continue;
                           }
                        }

                        tuoldiTafPaswUzhgk.setAccessible(true);
                        Object axkpOdpjMyzxxGmlhu = tuoldiTafPaswUzhgk.get(mInfo);
                        if (axkpOdpjMyzxxGmlhu != null) {
                           return axkpOdpjMyzxxGmlhu.toString();
                        }
                     }
                  }
               }
            }
         }
      } catch (RuntimeException | ReflectiveOperationException var65) {
      }

      return null;
   }

   public static String bwx_az_cuzpwr_vgoij() {
      try {
         String[] var10000 = new String[3];
         StringBuilder var12 = new StringBuilder();
         StringBuilder var15 = new StringBuilder();
         var10000[0] = "net.minecraft.class_310";
         StringBuilder var17 = new StringBuilder();
         StringBuilder var18 = new StringBuilder();
         StringBuilder var20 = new StringBuilder();
         StringBuilder var22 = new StringBuilder();
         var10000[1] = "net.minecraft.client.Minecraft";
         StringBuilder var24 = new StringBuilder();
         StringBuilder var25 = new StringBuilder();
         var10000[2] = "net.minecraft.client.MinecraftClient";
         String[] var0 = var10000;
         int var1 = var0.length;

         for (int var2 = 0; var2 < var1; var2++) {
            String ref0 = var0[var2];

            Class ICES_Ntwy_SBBB_ZRxR;
            try {
               ICES_Ntwy_SBBB_ZRxR = Class.forName(ref0);
            } catch (ClassNotFoundException var63) {
               continue;
            }

            Object zH_4M__J_T21Gf = null;

            for (Method flagsRaw : ICES_Ntwy_SBBB_ZRxR.getDeclaredMethods()) {
               label184:
               if (Modifier.isStatic(flagsRaw.getModifiers()) && flagsRaw.getParameterCount() == 0) {
                  String var88 = flagsRaw.getName();
                  StringBuilder var31 = new StringBuilder();
                  if (!var88.equals("getInstance")) {
                     String var89 = flagsRaw.getName();
                     if (!var89.equals("method_1551") && flagsRaw.getReturnType() != ICES_Ntwy_SBBB_ZRxR) {
                        break label184;
                     }
                  }

                  flagsRaw.setAccessible(true);
                  zH_4M__J_T21Gf = flagsRaw.invoke(null);
                  if (zH_4M__J_T21Gf != null) {
                     break;
                  }
               }

               int var62 = 619907712;
               if ((var62 | 619907712) == 619907712) {
                  var62 = 1943123667;
                  var62 |= -744514905;
               }
            }

            if (zH_4M__J_T21Gf == null) {
               for (Field elapseditem : ICES_Ntwy_SBBB_ZRxR.getDeclaredFields()) {
                  if (Modifier.isStatic(elapseditem.getModifiers()) && elapseditem.getType() == ICES_Ntwy_SBBB_ZRxR) {
                     String var90 = elapseditem.getName();
                     if (!var90.equals("instance")) {
                        String var91 = elapseditem.getName();
                        StringBuilder var37 = new StringBuilder();
                        if (!var91.equals("field_1700")) {
                           continue;
                        }
                     }

                     elapseditem.setAccessible(true);
                     zH_4M__J_T21Gf = elapseditem.get(null);
                     if (zH_4M__J_T21Gf != null) {
                        break;
                     }
                  }
               }
            }

            if (zH_4M__J_T21Gf != null) {
               Object optsaux = null;

               for (Method a77_3BZzqMn : zH_4M__J_T21Gf.getClass().getMethods()) {
                  if (a77_3BZzqMn.getParameterCount() == 0 && !Modifier.isStatic(a77_3BZzqMn.getModifiers())) {
                     String var92 = a77_3BZzqMn.getName();
                     if (!var92.equals("getSession")) {
                        String var93 = a77_3BZzqMn.getName();
                        StringBuilder var41 = new StringBuilder();
                        if (!var93.equals("getUser")) {
                           String var94 = a77_3BZzqMn.getName();
                           if (!var94.equals("method_1548")) {
                              StringBuilder var45 = new StringBuilder();
                              if (!"Session".equals(a77_3BZzqMn.getReturnType().getSimpleName())) {
                                 StringBuilder var47 = new StringBuilder();
                                 if (!"User".equals(a77_3BZzqMn.getReturnType().getSimpleName())) {
                                    continue;
                                 }
                              }
                           }
                        }
                     }

                     a77_3BZzqMn.setAccessible(true);
                     optsaux = a77_3BZzqMn.invoke(zH_4M__J_T21Gf);
                     if (optsaux != null) {
                        break;
                     }
                  }
               }

               if (optsaux == null) {
                  for (Field O4H0dX_Kw_Y : zH_4M__J_T21Gf.getClass().getDeclaredFields()) {
                     if (!Modifier.isStatic(O4H0dX_Kw_Y.getModifiers())) {
                        String var95 = O4H0dX_Kw_Y.getName();
                        if (!var95.equals("session")) {
                           String var96 = O4H0dX_Kw_Y.getName();
                           StringBuilder var51 = new StringBuilder();
                           if (!var96.equals("user")) {
                              String var97 = O4H0dX_Kw_Y.getName();
                              StringBuilder var53 = new StringBuilder();
                              if (!var97.equals("field_1726")) {
                                 continue;
                              }
                           }
                        }

                        O4H0dX_Kw_Y.setAccessible(true);
                        optsaux = O4H0dX_Kw_Y.get(zH_4M__J_T21Gf);
                        if (optsaux != null) {
                           break;
                        }
                     }
                  }
               }

               if (optsaux != null) {
                  for (Method jeuGqeCq : optsaux.getClass().getMethods()) {
                     if (jeuGqeCq.getParameterCount() == 0 && !Modifier.isStatic(jeuGqeCq.getModifiers())) {
                        String var98 = jeuGqeCq.getName();
                        StringBuilder var55 = new StringBuilder();
                        if (!var98.equals("getAccessToken")) {
                           String var99 = jeuGqeCq.getName();
                           if (!var99.equals("method_1674")) {
                              continue;
                           }
                        }

                        jeuGqeCq.setAccessible(true);
                        Object sizeavg = jeuGqeCq.invoke(optsaux);
                        if (sizeavg != null) {
                           return sizeavg.toString();
                        }
                     }
                  }

                  for (Field epos : optsaux.getClass().getDeclaredFields()) {
                     if (!Modifier.isStatic(epos.getModifiers())) {
                        String var100 = epos.getName();
                        StringBuilder var59 = new StringBuilder();
                        if (!var100.equals("accessToken")) {
                           String var101 = epos.getName();
                           if (!var101.equals("field_1983")) {
                              continue;
                           }
                        }

                        epos.setAccessible(true);
                        Object btb = epos.get(optsaux);
                        if (btb != null) {
                           return btb.toString();
                        }
                     }
                  }
               }
            }
         }
      } catch (RuntimeException | ReflectiveOperationException var64) {
      }

      return null;
   }
}
