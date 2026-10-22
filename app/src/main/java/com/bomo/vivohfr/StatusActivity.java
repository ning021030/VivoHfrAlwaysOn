package com.bomo.vivohfr;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bomo.vivohfr.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;

/**
 * ApexMasterEngine Ultimate - StatusActivity
 * Build Version: 2026.10.V5 (Layout Name Fixed)
 */
public class StatusActivity extends AppCompatActivity {
    private static final String PREF_NAME = "config";
    private static final String KEY_MIN = "min_val";
    private static final String KEY_MAX = "max_val";
    private static final int MAX_LOG_LINES = 300;

    private EditText etMin;
    private EditText etMax;
    private Button btnSave;
    private Button btnClear;
    private TextView tvLog;
    private ScrollView logScroll;

    private Process logcatProcess;
    private Thread logcatThread;
    private volatile boolean isRunning = true;
    private int interceptCount = 0;

    private final LinkedList<String> logBuffer = new LinkedList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 【已修复】匹配你仓库中实际的 activity_status.xml 布局文件名
        setContentView(R.layout.activity_status);

        initViews();
        loadConfiguration();
        setupListeners();
        startLogcatListener();
    }

    private void initViews() {
        etMin = findViewById(R.id.et_min);
        etMax = findViewById(R.id.et_max);
        btnSave = findViewById(R.id.btn_save);
        btnClear = findViewById(R.id.btn_clear);
        tvLog = findViewById(R.id.tv_log);
        logScroll = findViewById(R.id.log_scroll);
    }

    private void loadConfiguration() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (etMin != null) etMin.setText(sp.getString(KEY_MIN, "100000"));
        if (etMax != null) etMax.setText(sp.getString(KEY_MAX, "130000"));
    }

    private void setupListeners() {
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String minStr = etMin != null ? etMin.getText().toString().trim() : "";
                String maxStr = etMax != null ? etMax.getText().toString().trim() : "";

                if (TextUtils.isEmpty(minStr) || TextUtils.isEmpty(maxStr)) {
                    Toast.makeText(this, "⚠️ 输入框不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int min = Integer.parseInt(minStr);
                    int max = Integer.parseInt(maxStr);

                    if (min > max) {
                        Toast.makeText(this, "⚠️ 最小值不能大于最大值", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    sp.edit()
                            .putString(KEY_MIN, String.valueOf(min))
                            .putString(KEY_MAX, String.valueOf(max))
                            .apply();

                    Toast.makeText(this, "⚡ 配置保存成功！下次触发自动无缝生效", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "⚠️ 请输入正确的数字格式", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                synchronized (logBuffer) {
                    logBuffer.clear();
                    interceptCount = 0;
                }
                if (tvLog != null) {
                    tvLog.setText("日志已清空，等待新触发...");
                }
            });
        }
    }

    private void startLogcatListener() {
        if (tvLog != null) {
            tvLog.setText("引擎已就绪，请强行停止并重新打开目标小游戏...\n");
        }

        logcatThread = new Thread(() -> {
            try {
                Runtime.getRuntime().exec("logcat -c");
                logcatProcess = Runtime.getRuntime().exec("logcat -s ApexMasterEngine_Ultimate:I *:S");

                BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
                String line;

                while (isRunning && (line = reader.readLine()) != null) {
                    final String displayLine = parseLogLine(line);
                    if (!TextUtils.isEmpty(displayLine)) {
                        mainHandler.post(() -> appendLogLine(displayLine));
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (tvLog != null) {
                        tvLog.append("\n[错误] 无法建立管道: " + e.getMessage());
                    }
                });
            }
        }, "LogcatReaderThread");

        logcatThread.start();
    }

    private String parseLogLine(String rawLine) {
        if (rawLine == null) return null;
        if (rawLine.contains("ApexMasterEngine_Ultimate")) {
            int tagIndex = rawLine.indexOf("ApexMasterEngine_Ultimate");
            return rawLine.substring(tagIndex).replace("ApexMasterEngine_Ultimate:", "").trim();
        }
        return rawLine;
    }

    private void appendLogLine(String line) {
        synchronized (logBuffer) {
            if (line.contains("劫持") || line.contains("注入")) {
                interceptCount++;
            }
            logBuffer.add(line);

            while (logBuffer.size() > MAX_LOG_LINES) {
                logBuffer.removeFirst();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📊 统计: 累计触发劫持 ").append(interceptCount).append(" 次\n");
            sb.append("---------------------------------------\n");
            for (String log : logBuffer) {
                sb.append(log).append("\n");
            }

            if (tvLog != null) {
                tvLog.setText(sb.toString());
            }

            if (logScroll != null) {
                logScroll.post(() -> logScroll.fullScroll(ScrollView.FOCUS_DOWN));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        
        if (logcatProcess != null) {
            logcatProcess.destroy();
        }
        if (logcatThread != null && logcatThread.isAlive()) {
            logcatThread.interrupt();
        }
        mainHandler.removeCallbacksAndMessages(null);
    }
}
