package com.bomo.vivohfr; // 请根据你的实际包名修改

import android.content.Intent;
import android.content.Context;
import de.robv.android.xposed.XposedBridge;

public class LogUtils {
    public static void sendLog(String msg) {
        Context context = GlobalContext.getContext();
        if (context != null) {
            try {
                Intent intent = new Intent("com.bomo.vivohfr.ACTION_LOG_UPDATE");
                intent.putExtra("log_msg", msg);
                intent.setPackage("com.bomo.vivohfr"); // 必须是你的 UI 应用包名
                context.sendBroadcast(intent);
            } catch (Throwable e) {
                XposedBridge.log("发送实时日志失败: " + e.getMessage());
            }
        }
    }
}
