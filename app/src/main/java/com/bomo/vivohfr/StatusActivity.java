package com.bomo.vivohfr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class StatusActivity extends AppCompatActivity {

    private TextView tvLogContent;
    private LogBroadcastReceiver logReceiver;
    private StringBuilder logBuffer = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        tvLogContent = findViewById(R.id.tv_log);

        logReceiver = new LogBroadcastReceiver();
        IntentFilter filter = new IntentFilter(LogUtils.ACTION_LOG_UPDATE);
        
        // 兼容 Android 13/14+ 的广播导出策略
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(logReceiver, filter);
        }
        
        if (tvLogContent != null) {
            tvLogContent.setText("日志监听已启动，等待新触发...\n");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (logReceiver != null) {
            unregisterReceiver(logReceiver);
        }
    }

    private class LogBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && LogUtils.ACTION_LOG_UPDATE.equals(intent.getAction())) {
                String msg = intent.getStringExtra("log_msg");
                if (msg != null) {
                    logBuffer.append(msg).append("\n");
                    if (tvLogContent != null) {
                        tvLogContent.setText(logBuffer.toString());
                    }
                }
            }
        }
    }
}
