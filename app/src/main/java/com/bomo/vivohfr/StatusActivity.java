package com.bomo.vivohfr;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;

public class StatusActivity extends AppCompatActivity {
    private static final String PREF_NAME = "config";
    private static final String KEY_MIN = "min_val";
    private static final String KEY_MAX = "max_val";
    private static final int MAX_LOG_LINES = 200; // 内存保留的最大日志行数，防止卡顿

    private EditText etMin;
    private EditText etMax;
    private Button btnSave;
    private Button btnClear;
    private TextView tvLog;
    private ScrollView logScroll;

    private Process logcatProcess;
    private Thread logcatThread;
    private volatile boolean isRunning = true;

    private final LinkedList<String> logBuffer = new LinkedList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    /**
     * 读取已保存的 eCPM 配置并显示到输入框
     */
    private void loadConfiguration() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (etMin != null) etMin.setText(sp.getString(KEY_MIN, "100000"));
        if (etMax != null) etMax.setText(sp.getString(KEY_MAX, "130000"));
    }

    /**
     * 设置按钮点击响应事件
     */
    private void setupListeners() {
        // 保存配置事件
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

                    Toast.makeText(this, "✅ 配置已成功保存！强行停止并重启目标小游戏后生效", Toast.LENGTH_LONG).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "⚠️ 请输入正确的数字格式", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 清空日志事件
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                synchronized (logBuffer) {
                    logBuffer.clear();
                }
                if (tvLog != null) {
                    tvLog.setText("日志已清空，等待新触发...");
                }
            });
        }
    }

    /**
     * 核心亮点：开启后台线程通过 logcat 管道实时监听 Hook 模块打印的日志
     */
    private void startLogcatListener() {
        if (tvLog != null) {
            tvLog.setText("引擎已就绪，请强行停止并重新打开目标小游戏...\n");
        }

        logcatThread = new Thread(() -> {
            try {
                // 清理旧日志缓存，仅监听当前 Tag 为 ApexMasterEngine_Ultimate 的新日志
                Runtime.getRuntime().exec("logcat -c");
                logcatProcess = Runtime.getRuntime().exec("logcat -s ApexMasterEngine_Ultimate:I *:S");

                BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
                String line;

                while (isRunning && (line = reader.readLine()) != null) {
                    // 过滤并格式化提取关键内容
                    final String displayLine = parseLogLine(line);
                    if (!TextUtils.isEmpty(displayLine)) {
                        mainHandler.post(() -> appendLogLine(displayLine));
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (tvLog != null) {
                        tvLog.append("\n[系统警告] 无法启动日志监听器: " + e.getMessage());
                    }
                });
            }
        }, "LogcatReader-Thread");

        logcatThread.start();
    }

    /**
     * 格式化 Logcat 输出文本（提取日志主体）
     */
    private String parseLogLine(String rawLine) {
        if (rawLine == null) return null;
        if (rawLine.contains("ApexMasterEngine_Ultimate")) {
            int tagIndex = rawLine.indexOf("ApexMasterEngine_Ultimate");
            return rawLine.substring(tagIndex).replace("ApexMasterEngine_Ultimate:", "").trim();
        }
        return rawLine;
    }

    /**
     * 安全地追加日志并平滑滚动 UI
     */
    private void appendLogLine(String line) {
        synchronized (logBuffer) {
            logBuffer.add(line);
            while (logBuffer.size() > MAX_LOG_LINES) {
                logBuffer.removeFirst();
            }

            StringBuilder sb = new StringBuilder();
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
        
        // 彻底释放线程与子进程，防止内存泄露
        if (logcatProcess != null) {
            logcatProcess.destroy();
        }
        if (logcatThread != null && logcatThread.isAlive()) {
            logcatThread.interrupt();
        }
        mainHandler.removeCallbacksAndMessages(null);
    }
}
