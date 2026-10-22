package com.bomo.vivohfr;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class StatusActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        final EditText etMin = findViewById(R.id.etMin);
        final EditText etMax = findViewById(R.id.etMax);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnClear = findViewById(R.id.btnClear);
        final TextView tvLog = findViewById(R.id.tvLog);

        final SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
        etMin.setText(sp.getString("min_val", "100000"));
        etMax.setText(sp.getString("max_val", "130000"));

        tvLog.setText(readLogContent());

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("min_val", etMin.getText().toString());
                editor.putString("max_val", etMax.getText().toString());
                editor.apply();

                // 赋予全局可读权限，确保 Xposed 模块能跨进程读取配置
                try {
                    File prefsFile = new File(getApplicationContext().getDataDir(), "shared_prefs/config.xml");
                    if (prefsFile.exists()) {
                        prefsFile.setReadable(true, false);
                    }
                } catch (Throwable ignored) {}

                Toast.makeText(StatusActivity.this, "保存成功！重启目标软件生效", Toast.LENGTH_SHORT).show();
                tvLog.setText(readLogContent());
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLogFile();
                tvLog.setText("日志已清空，等待新注入...");
            }
        });
    }

    private String readLogContent() {
        try {
            File logFile = new File(getFilesDir(), "hook_log.txt");
            if (!logFile.exists()) return "日志就绪，重启目标应用后此处显示注入日志...";
            FileInputStream fis = new FileInputStream(logFile);
            byte[] buffer = new byte[(int) logFile.length()];
            fis.read(buffer);
            fis.close();
            return new String(buffer, "UTF-8");
        } catch (Exception e) {
            return "暂无实时日志，等待触发...";
        }
    }

    private void clearLogFile() {
        try {
            File logFile = new File(getFilesDir(), "hook_log.txt");
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
