package me.sam.hook.app;


import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AndPermission.with(this)
                .permission(Permission.STORAGE)
                .start();
    }

    /**
     * 启动插件Activity
     */
    public void startTestActivity(View view) {
        // 启动插件里面的PluginActivity
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("me.sam.hook.app", "me.sam.hook.plugin.PluginActivity"));
        startActivity(intent);
    }
}
