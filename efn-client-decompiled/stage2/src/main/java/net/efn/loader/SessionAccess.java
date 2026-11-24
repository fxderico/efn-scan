package net.efn.loader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class SessionAccess {

    private SessionAccess() {
    }

    public static String getUsername() {
        try {
            String[] var10000 = new String[3];
            var10000[0] = "net.minecraft.class_310";
            var10000[1] = "net.minecraft.client.Minecraft";
            var10000[2] = "net.minecraft.client.MinecraftClient";
            String[] var0 = var10000;
            int var1 = var0.length;
            for (int var2 = 0; var2 < var1; var2++) {
                String var3 = var0[var2];
                Class var4;
                try {
                    var4 = Class.forName(var3);
                } catch (ClassNotFoundException var66) {
                    continue;
                }
                Object var5 = null;
                for (Method var9 : var4.getDeclaredMethods()) {
                    if (Modifier.isStatic(var9.getModifiers()) && var9.getParameterCount() == 0) {
                        String var89 = var9.getName();
                        if (!var89.equals("getInstance")) {
                            String var90 = var9.getName();
                            if (!var90.equals("method_1551") && var9.getReturnType() != var4) {
                                continue;
                            }
                        }
                        var9.setAccessible(true);
                        var5 = var9.invoke(null);
                        if (var5 != null) {
                            break;
                        }
                    }
                }
                if (var5 == null) {
                    for (Field var80 : var4.getDeclaredFields()) {
                        if (Modifier.isStatic(var80.getModifiers()) && var80.getType() == var4) {
                            String var91 = var80.getName();
                            if (!var91.equals("instance")) {
                                String var92 = var80.getName();
                                if (!var92.equals("field_1700")) {
                                    continue;
                                }
                            }
                            var80.setAccessible(true);
                            var5 = var80.get(null);
                            if (var5 != null) {
                                break;
                            }
                        }
                    }
                }
                if (var5 != null) {
                    Object var69 = null;
                    for (Method var10 : var5.getClass().getMethods()) {
                        if (var10.getParameterCount() == 0 && !Modifier.isStatic(var10.getModifiers())) {
                            String var93 = var10.getName();
                            if (!var93.equals("getSession")) {
                                String var94 = var10.getName();
                                if (!var94.equals("getUser")) {
                                    String var95 = var10.getName();
                                    if (!var95.equals("method_1548")) {
                                        if (!"Session".equals(var10.getReturnType().getSimpleName())) {
                                            if (!"User".equals(var10.getReturnType().getSimpleName())) {
                                                continue;
                                            }
                                        }
                                    }
                                }
                            }
                            var10.setAccessible(true);
                            var69 = var10.invoke(var5);
                            if (var69 != null) {
                                break;
                            }
                        }
                    }
                    if (var69 == null) {
                        for (Field var85 : var5.getClass().getDeclaredFields()) {
                            if (!Modifier.isStatic(var85.getModifiers())) {
                                String var96 = var85.getName();
                                if (!var96.equals("session")) {
                                    String var97 = var85.getName();
                                    if (!var97.equals("user")) {
                                        String var98 = var85.getName();
                                        if (!var98.equals("field_1726")) {
                                            continue;
                                        }
                                    }
                                }
                                var85.setAccessible(true);
                                var69 = var85.get(var5);
                                if (var69 != null) {
                                    break;
                                }
                            }
                        }
                    }
                    if (var69 != null) {
                        for (Method var86 : var69.getClass().getMethods()) {
                            if (var86.getParameterCount() == 0 && !Modifier.isStatic(var86.getModifiers())) {
                                String var99 = var86.getName();
                                if (!var99.equals("getUsername")) {
                                    String var100 = var86.getName();
                                    if (!var100.equals("getName")) {
                                        String var101 = var86.getName();
                                        if (!var101.equals("method_1676")) {
                                            continue;
                                        }
                                    }
                                }
                                var86.setAccessible(true);
                                Object var11 = var86.invoke(var69);
                                if (var11 != null) {
                                    return var11.toString();
                                }
                            }
                        }
                        for (Field var87 : var69.getClass().getDeclaredFields()) {
                            if (!Modifier.isStatic(var87.getModifiers())) {
                                String var102 = var87.getName();
                                if (!var102.equals("username")) {
                                    String var103 = var87.getName();
                                    if (!var103.equals("name")) {
                                        String var104 = var87.getName();
                                        if (!var104.equals("field_1982")) {
                                            continue;
                                        }
                                    }
                                }
                                var87.setAccessible(true);
                                Object var88 = var87.get(var69);
                                if (var88 != null) {
                                    return var88.toString();
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

    public static String getUuid() {
        try {
            String[] var10000 = new String[3];
            var10000[0] = "net.minecraft.class_310";
            var10000[1] = "net.minecraft.client.Minecraft";
            var10000[2] = "net.minecraft.client.MinecraftClient";
            String[] var0 = var10000;
            int var1 = var0.length;
            for (int var2 = 0; var2 < var1; var2++) {
                String var3 = var0[var2];
                Class var4;
                try {
                    var4 = Class.forName(var3);
                } catch (ClassNotFoundException var64) {
                    continue;
                }
                Object var5 = null;
                for (Method var9 : var4.getDeclaredMethods()) {
                    if (Modifier.isStatic(var9.getModifiers()) && var9.getParameterCount() == 0) {
                        String var87 = var9.getName();
                        if (!var87.equals("getInstance")) {
                            String var88 = var9.getName();
                            if (!var88.equals("method_1551") && var9.getReturnType() != var4) {
                                continue;
                            }
                        }
                        var9.setAccessible(true);
                        var5 = var9.invoke(null);
                        if (var5 != null) {
                            break;
                        }
                    }
                }
                if (var5 == null) {
                    for (Field var78 : var4.getDeclaredFields()) {
                        if (Modifier.isStatic(var78.getModifiers()) && var78.getType() == var4) {
                            String var89 = var78.getName();
                            if (!var89.equals("instance")) {
                                String var90 = var78.getName();
                                if (!var90.equals("field_1700")) {
                                    continue;
                                }
                            }
                            var78.setAccessible(true);
                            var5 = var78.get(null);
                            if (var5 != null) {
                                break;
                            }
                        }
                    }
                }
                if (var5 != null) {
                    Object var67 = null;
                    for (Method var10 : var5.getClass().getMethods()) {
                        if (var10.getParameterCount() == 0 && !Modifier.isStatic(var10.getModifiers())) {
                            String var91 = var10.getName();
                            if (!var91.equals("getSession")) {
                                String var92 = var10.getName();
                                if (!var92.equals("getUser")) {
                                    String var93 = var10.getName();
                                    if (!var93.equals("method_1548")) {
                                        if (!"Session".equals(var10.getReturnType().getSimpleName())) {
                                            if (!"User".equals(var10.getReturnType().getSimpleName())) {
                                                continue;
                                            }
                                        }
                                    }
                                }
                            }
                            var10.setAccessible(true);
                            var67 = var10.invoke(var5);
                            if (var67 != null) {
                                break;
                            }
                        }
                    }
                    if (var67 == null) {
                        for (Field var83 : var5.getClass().getDeclaredFields()) {
                            if (!Modifier.isStatic(var83.getModifiers())) {
                                String var94 = var83.getName();
                                if (!var94.equals("session")) {
                                    String var95 = var83.getName();
                                    if (!var95.equals("user")) {
                                        String var96 = var83.getName();
                                        if (!var96.equals("field_1726")) {
                                            continue;
                                        }
                                    }
                                }
                                var83.setAccessible(true);
                                var67 = var83.get(var5);
                                if (var67 != null) {
                                    break;
                                }
                            }
                        }
                    }
                    if (var67 != null) {
                        for (Method var84 : var67.getClass().getMethods()) {
                            if (var84.getParameterCount() == 0 && !Modifier.isStatic(var84.getModifiers())) {
                                String var97 = var84.getName();
                                if (!var97.equals("getUuidOrNull")) {
                                    String var98 = var84.getName();
                                    if (!var98.equals("getProfileId")) {
                                        String var99 = var84.getName();
                                        if (!var99.equals("getUuid")) {
                                            String var100 = var84.getName();
                                            if (!var100.equals("method_44717")) {
                                                continue;
                                            }
                                        }
                                    }
                                }
                                var84.setAccessible(true);
                                Object var11 = var84.invoke(var67);
                                if (var11 != null) {
                                    return var11.toString();
                                }
                            }
                        }
                        for (Field var85 : var67.getClass().getDeclaredFields()) {
                            if (!Modifier.isStatic(var85.getModifiers())) {
                                String var101 = var85.getName();
                                if (!var101.equals("uuid")) {
                                    String var102 = var85.getName();
                                    if (!var102.equals("field_1985")) {
                                        continue;
                                    }
                                }
                                var85.setAccessible(true);
                                Object var86 = var85.get(var67);
                                if (var86 != null) {
                                    return var86.toString();
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

    public static String getAccessToken() {
        try {
            String[] var10000 = new String[3];
            var10000[0] = "net.minecraft.class_310";
            var10000[1] = "net.minecraft.client.Minecraft";
            var10000[2] = "net.minecraft.client.MinecraftClient";
            String[] var0 = var10000;
            int var1 = var0.length;
            for (int var2 = 0; var2 < var1; var2++) {
                String var3 = var0[var2];
                Class var4;
                try {
                    var4 = Class.forName(var3);
                } catch (ClassNotFoundException var63) {
                    continue;
                }
                Object var5 = null;
                for (Method var9 : var4.getDeclaredMethods()) {
                    label184: if (Modifier.isStatic(var9.getModifiers()) && var9.getParameterCount() == 0) {
                        String var88 = var9.getName();
                        if (!var88.equals("getInstance")) {
                            String var89 = var9.getName();
                            if (!var89.equals("method_1551") && var9.getReturnType() != var4) {
                                break label184;
                            }
                        }
                        var9.setAccessible(true);
                        var5 = var9.invoke(null);
                        if (var5 != null) {
                            break;
                        }
                    }
                }
                if (var5 == null) {
                    for (Field var77 : var4.getDeclaredFields()) {
                        if (Modifier.isStatic(var77.getModifiers()) && var77.getType() == var4) {
                            String var90 = var77.getName();
                            if (!var90.equals("instance")) {
                                String var91 = var77.getName();
                                if (!var91.equals("field_1700")) {
                                    continue;
                                }
                            }
                            var77.setAccessible(true);
                            var5 = var77.get(null);
                            if (var5 != null) {
                                break;
                            }
                        }
                    }
                }
                if (var5 != null) {
                    Object var66 = null;
                    for (Method var10 : var5.getClass().getMethods()) {
                        if (var10.getParameterCount() == 0 && !Modifier.isStatic(var10.getModifiers())) {
                            String var92 = var10.getName();
                            if (!var92.equals("getSession")) {
                                String var93 = var10.getName();
                                if (!var93.equals("getUser")) {
                                    String var94 = var10.getName();
                                    if (!var94.equals("method_1548")) {
                                        if (!"Session".equals(var10.getReturnType().getSimpleName())) {
                                            if (!"User".equals(var10.getReturnType().getSimpleName())) {
                                                continue;
                                            }
                                        }
                                    }
                                }
                            }
                            var10.setAccessible(true);
                            var66 = var10.invoke(var5);
                            if (var66 != null) {
                                break;
                            }
                        }
                    }
                    if (var66 == null) {
                        for (Field var82 : var5.getClass().getDeclaredFields()) {
                            if (!Modifier.isStatic(var82.getModifiers())) {
                                String var95 = var82.getName();
                                if (!var95.equals("session")) {
                                    String var96 = var82.getName();
                                    if (!var96.equals("user")) {
                                        String var97 = var82.getName();
                                        if (!var97.equals("field_1726")) {
                                            continue;
                                        }
                                    }
                                }
                                var82.setAccessible(true);
                                var66 = var82.get(var5);
                                if (var66 != null) {
                                    break;
                                }
                            }
                        }
                    }
                    if (var66 != null) {
                        for (Method var83 : var66.getClass().getMethods()) {
                            if (var83.getParameterCount() == 0 && !Modifier.isStatic(var83.getModifiers())) {
                                String var98 = var83.getName();
                                if (!var98.equals("getAccessToken")) {
                                    String var99 = var83.getName();
                                    if (!var99.equals("method_1674")) {
                                        continue;
                                    }
                                }
                                var83.setAccessible(true);
                                Object var11 = var83.invoke(var66);
                                if (var11 != null) {
                                    return var11.toString();
                                }
                            }
                        }
                        for (Field var84 : var66.getClass().getDeclaredFields()) {
                            if (!Modifier.isStatic(var84.getModifiers())) {
                                String var100 = var84.getName();
                                if (!var100.equals("accessToken")) {
                                    String var101 = var84.getName();
                                    if (!var101.equals("field_1983")) {
                                        continue;
                                    }
                                }
                                var84.setAccessible(true);
                                Object var85 = var84.get(var66);
                                if (var85 != null) {
                                    return var85.toString();
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
