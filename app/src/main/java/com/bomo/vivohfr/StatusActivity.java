package com.bomo.vivohfr;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class StatusActivity extends Activity {
    private TextView tvLog;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable logRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        final EditText etMin = findViewById(R.id.etMin);
        final EditText etMax = findViewById(R.id.etMax);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnClear = findViewById(R.id.btnClear);
        tvLog = findViewById(R.id.tvLog);

        final SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
        etMin.setText(sp.getString("min_val", "100000"));
        etMax.setText(sp.getString("max_val", "130000"));

        // 保存配置逻辑
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("min_val", etMin.getText().toString());
                editor.putString("max_val", etMax.getText().toString());
                editor.apply();

                try {
                    File prefsFile = new File(getApplicationContext().getDataDir(), "shared_prefs/config.xml");
                    if (prefsFile.exists()) {
                        prefsFile.setReadable(true, false);
                    }
                } catch (Throwable ignored) {}

                Toast.makeText(StatusActivity.this, "配置已保存！强行重启目标游戏生效", Toast.LENGTH_SHORT).show();
            }
        });

        // 清空日志逻辑
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLogFile();
                tvLog.setText("日志已清空，等待小游戏触发...");
            }
        });

        // 开启 1.5 秒定时轮询读取最新日志
        logRunnable = new Runnable() {
            @Override
            public void run() {
                tvLog.setText(readLogContent());
                handler.postDelayed(this, 1500);
            }
        };
        handler.post(logRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && logRunnable != null) {
            handler.removeCallbacks(logRunnable);
        }
    }

    private String readLogContent() {
        try {
            File logFile = new File("/sdcard/Android/data/com.bomo.vivohfr/files/hook_log.txt");
            if (!logFile.exists() || logFile.length() == 0) {
                return "引擎已就绪，请强行停止并重新打开目标小游戏...";
            }
            FileInputStream fis = new FileInputStream(logFile);
            byte[] buffer = new byte[(int) logFile.length()];
            fis.read(buffer);
            fis.close();
            return new String(buffer, "UTF-8");
        } catch (Exception e) {
            return "暂无运行日志...";
        }
    }

    private void clearLogFile() {
        try {
            File logFile = new File("/sdcard/Android/data/com.bomo.vivohfr/files/hook_log.txt");
            if (logFile.exists()) {
                FileOutputStream fos = new FileOutputStream(logFile);
                fos.write("".getBytes());
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
