package com.example.funsdkdemo.test;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import com.example.funsdkdemo.R;
import com.lib.MsgContent;
import com.lib.funsdk.support.FunPath;
import com.lib.funsdk.support.utils.BaseThreadPool;
import com.manager.device.DeviceManager;
import com.manager.device.media.MediaManager;
import com.manager.device.media.attribute.PlayerAttribute;
import com.manager.device.media.monitor.MonitorManager;
import com.xm.ui.media.PlayVideoWnd;

import java.io.File;

/**
 * @author hws
 * @class 测试
 * @time 2019-08-19 17:02
 */
public class TestActivity extends AppCompatActivity implements MediaManager.OnMediaManagerListener{
    private MonitorManager manager;
    private ViewGroup surface;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        surface = findViewById(R.id.fl_surface);
        initData();
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        String devId = intent.getStringExtra("devId");
        manager = DeviceManager.getInstance().createMonitorPlayer(surface,devId);
        manager.setOnMediaManagerListener(this);
        manager.startMonitor();
    }

    public void onTest(View view) {
        if (manager.getPlayState() == PlayVideoWnd.E_STATE_PlAY) {
            BaseThreadPool.getInstance().doTask(new Runnable() {
                @Override
                public void run() {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            manager.capture(FunPath.PATH_CAPTURE_TEMP + File.separator);
                        }
                    });
                }
            },1,3);
        }
    }

    @Override
    public void onMediaPlayState(PlayerAttribute playerAttribute, int state) {

    }

    @Override
    public void onFailed(PlayerAttribute playerAttribute, int i, int i1) {

    }

    @Override
    public void onShowRateAndTime(PlayerAttribute playerAttribute, boolean b, String s, String s1) {

    }

    @Override
    public void onVideoBufferEnd(PlayerAttribute playerAttribute, MsgContent msgContent) {

    }
}
