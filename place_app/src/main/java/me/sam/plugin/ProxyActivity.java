package me.sam.plugin;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.lang.reflect.Constructor;

import me.sam.placeInterface.ActivityInterface;

/**
 * desc : 代理的Activity 代理/占位 插件里面的Activity
 * date : 2019-10-18
 * @author : dongSen
 */
public class ProxyActivity extends Activity {

    @Override
    public Resources getResources() {
        return PluginManager.getInstance(this).getResources();
    }

    @Override
    public ClassLoader getClassLoader() {
        return PluginManager.getInstance(this).getClassLoader();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 真正的加载 插件里面的 Activity
        String className = getIntent().getStringExtra("className");

        try {
            Class mPluginActivityClass = getClassLoader().loadClass(className);
            // 实例化 插件包里面的 Activity
            Constructor constructor = mPluginActivityClass.getConstructor(new Class[]{});
            Object mPluginActivity = constructor.newInstance(new Object[]{});

            ActivityInterface activityInterface = (ActivityInterface) mPluginActivity;
            // 注入Context
            activityInterface.insertAppContext(this);

            Bundle bundle = new Bundle();
            bundle.putString("info", "this is from main app");
            // 执行插件里面的onCreate方法
            activityInterface.onCreate(bundle);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startActivity(Intent intent) {
        String className = intent.getStringExtra("className");

        Intent proxyIntent = new Intent(this, ProxyActivity.class);
        proxyIntent.putExtra("className", className);

        // 要给TestActivity 进栈
        super.startActivity(proxyIntent);
    }
}
