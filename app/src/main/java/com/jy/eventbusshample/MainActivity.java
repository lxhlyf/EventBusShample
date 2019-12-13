package com.jy.eventbusshample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.jy.eventbusshample.eventbus.EventBus;
import com.jy.eventbusshample.eventbus.Subscribe;
import com.jy.eventbusshample.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {

    private  TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);

        textView = findViewById(R.id.tv1);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TestActivity.class));
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN, priority = 10, sticky = true)
    public void onChangeText(String text) {
        Log.e("TAG","msg1 = " + text);
        textView.setText(text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unRegister(this);
    }
}
