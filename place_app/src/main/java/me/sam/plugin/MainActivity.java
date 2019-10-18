package me.sam.plugin;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.io.File;

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
     * 加载插件
     */
    public void loadPlugin(View view) {
        PluginManager.getInstance(this).loadPlugin();
    }

    /**
     * 启动插件中的Activity
     */
    public void startPluginActivity(View view) {
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "plugin.apk");
        String path = file.getAbsolutePath();

        // 获取插件包 里面的 Activity
        PackageManager packageManager = getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        ActivityInfo activityInfo = packageInfo.activities[0];

        // 占位Activity
        Intent intent = new Intent(this, ProxyActivity.class);
        intent.putExtra("className", activityInfo.name);
        startActivity(intent);
    }

}
