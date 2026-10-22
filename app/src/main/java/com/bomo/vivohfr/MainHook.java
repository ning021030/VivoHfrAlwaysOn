package com.bomo.vivohfr;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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

    // 跨进程日志写入公共 Download 目录，彻底解决沙盒权限限制，让控制面板App实时读取显示
    private static void writeLogToFile(String logMsg) {
        try {
            File logDir = new File("/sdcard/Download/VivoHfrLogs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            File logFile = new File(logDir, "hook_log.txt");
            if (!logFile.exists()) {
                logFile.createNewFile();
                logFile.setReadable(true, false);
                logFile.setWritable(true, false);
            }
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String content = "[" + time + "] " + logMsg + "\n";
            
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
        } catch (Throwable t) {
            XposedBridge.log(TAG + " Log write error: " + t.getMessage());
        }
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

        XposedBridge.log(TAG + " 🚀 薅羊毛全能通杀引擎已成功注入目标: " + lpparam.packageName);
        writeLogToFile("引擎已启动: " + lpparam.packageName);

        // ==================== 第一部分：硬核反检测与风控对抗 ====================
        try {
            // 1. 隐藏 Root / Magisk / LSPosed / 模拟器文件特征
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
                                path.contains("riru") || path.contains("hook") || 
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

            // 3. 过滤已安装应用列表（防 PM 扫描）
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

            // 4. 堆栈特征清洗（防止异常排查抓取到 Xposed）
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

            // 5. 拦截命令执行防检测
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String[].class, String[].class, File.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args != null && param.args[0] instanceof String[]) {
                        String[] cmds = (String[]) param.args[0];
                        for (String cmd : cmds) {
                            if (cmd != null && (cmd.contains("su") || cmd.contains("magisk") || cmd.contains("which"))) {
                                param.setResult(null);
                                break;
                            }
                        }
                    }
                }
            });
        } catch (Throwable ignored) {}

        // ==================== 第二部分：羊毛小游戏与广告全通杀劫持引擎 ====================
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

                    // 覆盖全量广告SDK与薅羊毛业务逻辑类
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
                                
                                // 1. 收益、金币、价格、元宝劫持
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

                                            // 拟态概率：90% 篡改，10% 保持自然波动，完美防封
                                            if (RANDOM.nextInt(100) < 10) return; 

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

                                            writeLogToFile("羊毛暴利劫持 [" + method.getName() + "] -> 成功注入: " + fakeValue);
                                        }
                                    });
                                }

                                // 2. 广告秒跳过、倒计时清零、强制奖励发放成功
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
                                                methodParam.setResult(true); // 强制全部判定为成功、可跳过、已领奖
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
