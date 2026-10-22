package com.bomo.vivohfr;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "ApexMasterEngine_Ultimate";
    private static final Set<String> HOOKED_CACHE = ConcurrentHashMap.newKeySet();
    private static final Random RANDOM = new Random();
    private static XSharedPreferences sPrefs;

    private static synchronized int[] getDynamicRange() {
        int minVal = 100000;
        int maxVal = 130000;
        try {
            if (sPrefs == null) {
                sPrefs = new XSharedPreferences("com.bomo.vivohfr", "config");
                sPrefs.makeWorldReadable();
            } else {
                sPrefs.reload();
            }
            String minStr = sPrefs.getString("min_val", "100000");
            String maxStr = sPrefs.getString("max_val", "130000");
            minVal = Integer.parseInt(minStr);
            maxVal = Integer.parseInt(maxStr);
        } catch (Throwable ignored) {}
        if (minVal > maxVal) {
            int temp = minVal;
            minVal = maxVal;
            maxVal = temp;
        }
        return new int[]{minVal, maxVal};
    }

    /**
     * 完美日志输出：通过 XposedBridge 统一输出到系统日志（Tag: ApexMasterEngine_Ultimate）
     * 控面板 App 可以直接通过读取 logcat 实时展示，彻底告别文件读写权限限制！
     */
    private static void log(String msg) {
        try {
            XposedBridge.log(TAG + " ➔ " + msg);
        } catch (Throwable ignored) {}
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName == null || 
            lpparam.packageName.startsWith("android") || 
            lpparam.packageName.startsWith("com.android") ||
            lpparam.packageName.startsWith("com.google") ||
            lpparam.packageName.equals("system") ||
            lpparam.packageName.equals("com.android.systemui") ||
            lpparam.packageName.equals("com.bomo.vivohfr")) {
            return;
        }

        log("🚀 薅羊毛全能通杀引擎已成功注入目标: " + lpparam.packageName);

        // ==================== 第一部分：硬核反检测与风控对抗 ====================
        try {
            // 1. 隐藏特征文件
            XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (Boolean.TRUE.equals(param.getResult())) {
                        File file = (File) param.thisObject;
                        if (file != null) {
                            String path = file.getAbsolutePath().toLowerCase();
                            if (path.contains("su") || path.contains("magisk") || 
                                path.contains("kernelsu") || path.contains("lsposed") || 
                                path.contains("frida") || path.contains("xposed") ||
                                path.contains("bin.mt") || path.contains("zygisk") ||
                                path.contains("qemu") || path.contains("nox")) {
                                param.setResult(false);
                            }
                        }
                    }
                }
            });

            // 2. 伪装系统安全属性
            Class<?> systemProperties = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(systemProperties, "get", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof String) {
                        String key = (String) param.args[0];
                        if ("ro.debuggable".equals(key)) param.setResult("0");
                        else if ("ro.secure".equals(key)) param.setResult("1");
                        else if ("ro.kernel.qemu".equals(key)) param.setResult("0");
                        else if ("ro.build.tags".equals(key)) param.setResult("release-keys");
                        else if ("ro.build.type".equals(key)) param.setResult("user");
                    }
                }
            });

            // 3. 过滤已安装应用列表
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, 
                "getInstalledPackages", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    java.util.List<?> list = (java.util.List<?>) param.getResult();
                    if (list != null) {
                        java.util.Iterator<?> iterator = list.iterator();
                        while (iterator.hasNext()) {
                            Object pkgInfo = iterator.next();
                            try {
                                String pkgName = (String) XposedHelpers.getObjectField(pkgInfo, "packageName");
                                if (pkgName != null && (pkgName.contains("lsposed") || pkgName.contains("magisk") || 
                                    pkgName.contains("bin.mt") || pkgName.contains("xposed") || 
                                    pkgName.contains("root") || pkgName.contains("frida"))) {
                                    iterator.remove();
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            });

            // 4. 堆栈特征清洗
            XposedHelpers.findAndHookMethod(Throwable.class, "getStackTrace", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    StackTraceElement[] stack = (StackTraceElement[]) param.getResult();
                    if (stack == null) return;
                    java.util.List<StackTraceElement> cleanList = new java.util.ArrayList<>();
                    for (StackTraceElement element : stack) {
                        String clsName = element.getClassName();
                        if (clsName != null && (clsName.contains("de.robv.android.xposed") || 
                            clsName.contains("com.saurik.substrate") || clsName.contains("lsposed"))) {
                            continue;
                        }
                        cleanList.add(element);
                    }
                    param.setResult(cleanList.toArray(new StackTraceElement[0]));
                }
            });
        } catch (Throwable ignored) {}

        // ==================== 第二部分：羊毛与广告全通杀劫持引擎 ====================
        try {
            XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Class<?> clazz = (Class<?>) param.getResult();
                    if (clazz == null) return;

                    String className = clazz.getName().toLowerCase();
                    if (className.startsWith("android.") || className.startsWith("java.") || 
                        className.startsWith("kotlin.") || className.startsWith("androidx.") ||
                        className.startsWith("com.google.") || className.startsWith("dalvik.")) {
                        return;
                    }

                    if (className.contains("ad") || className.contains("mediation") || 
                        className.contains("reward") || className.contains("ecpm") || 
                        className.contains("bidding") || className.contains("price") ||
                        className.contains("pangle") || className.contains("gdt") || 
                        className.contains("sigmob") || className.contains("topon") ||
                        className.contains("ksad") || className.contains("baidu") ||
                        className.contains("skip") || className.contains("countdown") ||
                        className.contains("coin") || className.contains("gold") ||
                        className.contains("money") || className.contains("cash") ||
                        className.contains("score") || className.contains("diamond") ||
                        className.contains("task") || className.contains("user") ||
                        className.contains("utils") || className.contains("manager")) {

                        try {
                            Method[] methods = clazz.getDeclaredMethods();
                            if (methods == null) return;

                            for (Method method : methods) {
                                if (method == null) continue;
                                String mName = method.getName().toLowerCase();
                                
                                // 1. 收益、金币、价格劫持
                                if (mName.contains("ecpm") || mName.contains("price") || 
                                    mName.contains("bid") || mName.contains("revenue") || 
                                    mName.contains("amount") || mName.contains("value") || 
                                    mName.contains("coin") || mName.contains("gold") || 
                                    mName.contains("cash") || mName.contains("score") ||
                                    mName.contains("diamond") || mName.contains("reward") ||
                                    mName.contains("balance")) {

                                    String methodKey = clazz.getName() + "#" + method.getName();
                                    if (!HOOKED_CACHE.add(methodKey)) continue;

                                    method.setAccessible(true);
                                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam methodParam) throws Throwable {
                                            Object res = methodParam.getResult();
                                            if (res == null) return;

                                            if (RANDOM.nextInt(100) < 10) return; // 拟态防封

                                            int[] range = getDynamicRange();
                                            int gaussianJitter = (int)(RANDOM.nextGaussian() * 2500);
                                            int fakeValue = range[0] + RANDOM.nextInt(range[1] - range[0] + 1) + gaussianJitter;
                                            if (fakeValue < range[0]) fakeValue = range[0];

                                            Class<?> retType = method.getReturnType();
                                            if (retType == double.class || retType == Double.class) {
                                                methodParam.setResult((double) fakeValue);
                                            } else if (retType == float.class || retType == Float.class) {
                                                methodParam.setResult((float) fakeValue);
                                            } else if (retType == long.class || retType == Long.class) {
                                                methodParam.setResult((long) fakeValue);
                                            } else if (retType == int.class || retType == Integer.class) {
                                                methodParam.setResult(fakeValue);
                                            } else if (retType == String.class) {
                                                methodParam.setResult(String.valueOf(fakeValue));
                                            }

                                            log("羊毛暴利劫持 [" + method.getName() + "] -> 成功注入: " + fakeValue);
                                        }
                                    });
                                }

                                // 2. 广告秒跳过与奖励强开
                                if (mName.contains("countdown") || mName.contains("remain") || 
                                    mName.contains("gettime") || mName.contains("iscompleted") ||
                                    mName.contains("isskip") || mName.contains("canclose") ||
                                    mName.contains("issuccess") || mName.contains("isreward") ||
                                    mName.contains("hasreward") || mName.contains("isvalid")) {

                                    String skipKey = clazz.getName() + "#SK#" + method.getName();
                                    if (!HOOKED_CACHE.add(skipKey)) continue;

                                    method.setAccessible(true);
                                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam methodParam) throws Throwable {
                                            Class<?> retType = method.getReturnType();
                                            String name = method.getName().toLowerCase();

                                            if (name.contains("countdown") || name.contains("remain") || name.contains("time")) {
                                                if (retType == int.class || retType == Integer.class) methodParam.setResult(0);
                                                else if (retType == long.class || retType == Long.class) methodParam.setResult(0L);
                                            } else if (retType == boolean.class || retType == Boolean.class) {
                                                methodParam.setResult(true);
                                            }
                                        }
                                    });
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}
    }
}
