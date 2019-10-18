package me.sam.placeInterface;

import android.app.Activity;
import android.os.Bundle;

public interface ActivityInterface {

    /**
     * 将宿主环境 传递给 插件
     */
    void insertAppContext(Activity appActivity);

    /**
     * 接管生命周期方法
     */
    void onCreate(Bundle savedInstanceState);

    void onStart();

    void onResume();

    void onDestroy();

}
