package com.bomo.vivohfr;

import android.content.Context;

public class GlobalContext {
    private static Context sContext;

    public static void setContext(Context context) {
        if (context != null && sContext == null) {
            sContext = context.getApplicationContext();
        }
    }

    public static Context getContext() {
        return sContext;
    }
}
