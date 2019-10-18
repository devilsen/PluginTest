package me.sam.plugin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import me.sam.placeInterface.ActivityInterface;

public class BaseActivity implements ActivityInterface {

    protected Activity appActivity; // 宿主Context环境

    @Override
    public void insertAppContext(Activity appActivity) {
        this.appActivity = appActivity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onDestroy() {

    }

    public void setContentView(int resId) {
        appActivity.setContentView(resId);
    }

    public View findViewById(int layoutId) {
        return appActivity.findViewById(layoutId);
    }

    public void startActivity(Intent intent) {
        Intent intentNew = new Intent();
        intentNew.putExtra("className", intent.getComponent().getClassName()); // TestActivity 全类名
        // 调用ProxyActivity里的startActivity
        appActivity.startActivity(intentNew);
    }
}
