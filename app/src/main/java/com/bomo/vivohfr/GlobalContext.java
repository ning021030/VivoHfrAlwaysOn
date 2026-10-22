package com.bomo.vivohfr; // 请根据你的实际包名修改

import android.content.Context;

public class GlobalContext {
    private static Context sContext;

    public static void setContext(Context context) {
        if (context != null) {
            sContext = context.getApplicationContext();
        }
    }

    public static Context getContext() {
        return sContext;
    }
}
