package com.jy.eventbusshample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.jy.eventbusshample.eventbus.EventBus;

/**
 * description:
 * author: Darren on 2017/10/31 09:36
 * email: 240336124@qq.com
 * version: 1.0
 */
public class TestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TextView)findViewById(R.id.tv1)).setText("测试页面");
        findViewById(R.id.tv1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post("从测试页面返回");
                finish();
            }
        });
    }
}
