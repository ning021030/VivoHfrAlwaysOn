package com.bomo.vivohfr;

import android.content.Context;
import android.content.Intent;

public class LogUtils {
    public static final String ACTION_LOG_UPDATE = "com.bomo.vivohfr.ACTION_LOG_UPDATE";

    public static void sendLog(String msg) {
        try {
            Context context = GlobalContext.getContext();
            if (context == null) return;

            Intent intent = new Intent(ACTION_LOG_UPDATE);
            intent.putExtra("log_msg", msg);
            // 核心：跨进程发送广播必须显式指定接收方包名
            intent.setPackage("com.bomo.vivohfr");
            
            context.sendBroadcast(intent);
        } catch (Throwable ignored) {}
    }
}
