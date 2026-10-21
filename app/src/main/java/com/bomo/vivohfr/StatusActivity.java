package com.bomo.vivohfr;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class StatusActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, "Apex Master Engine 模块运行中", Toast.LENGTH_SHORT).show();
        finish();
    }
}
