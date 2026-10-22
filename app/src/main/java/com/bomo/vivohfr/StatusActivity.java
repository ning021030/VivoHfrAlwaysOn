package com.bomo.vivohfr; // 请根据你的实际包名修改

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
        setContentView(R.layout.activity_status); // 确保你的 layout 文件名正确

        // 绑定你界面里的 TextView（请确保你在 activity_status.xml 中给它设置了 android:id="@+id/tv_log_content"）
        tvLogContent = findViewById(R.id.tv_log_content);

        // 初始化并注册广播接收器
        logReceiver = new LogBroadcastReceiver();
        IntentFilter filter = new IntentFilter("com.bomo.vivohfr.ACTION_LOG_UPDATE");
        
        // 兼容 Android 13/14+ 的广播导出策略
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(logReceiver, filter);
        }
        
        // 初始化显示提示
        if (tvLogContent != null) {
            tvLogContent.setText("日志监听已启动，等待新触发...\n");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 页面销毁时注销广播，防止内存泄漏
        if (logReceiver != null) {
            unregisterReceiver(logReceiver);
        }
    }

    // 内部广播接收器
    private class LogBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "com.bomo.vivohfr.ACTION_LOG_UPDATE".equals(intent.getAction())) {
                String msg = intent.getStringExtra("log_msg");
                if (msg != null) {
                    // 将新日志追加到缓冲区
                    logBuffer.append(msg).append("\n");
                    
                    // 实时更新到界面上的 TextView
                    if (tvLogContent != null) {
                        tvLogContent.setText(logBuffer.toString());
                    }
                }
            }
        }
    }
}
