package me.sam.plugin;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class PluginManager {

    private static final String TAG = PluginManager.class.getSimpleName();

    private static PluginManager pluginManager;

    private Context context;

    public static PluginManager getInstance(Context context) {
        if (pluginManager == null) {
            synchronized (PluginManager.class) {
                if (pluginManager == null) {
                    pluginManager = new PluginManager(context);
                }
            }
        }
        return pluginManager;
    }

    public PluginManager(Context context) {
        this.context = context;
    }

    private DexClassLoader dexClassLoader;
    private Resources resources;

    /**
     * 加载插件
     */
    public void loadPlugin() {
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "plugin.apk");
        if (!file.exists()) {
            Log.d(TAG, "插件包不存在");
            return;
        }
        String pluginPath = file.getAbsolutePath();

        initClassLoader(pluginPath);

        initLayout(pluginPath);
    }

    /**
     * 加载插件中的layout
     */
    private void initLayout(String pluginPath) {
        try {
            // 加载资源
            AssetManager assetManager = AssetManager.class.newInstance();

            // 反射执行此方法，把插件包的路径添加进去
            Method addAssetPathMethod = assetManager.getClass().getMethod("addAssetPath", String.class); // 他是类类型 Class
            // 添加插件包路径
            addAssetPathMethod.invoke(assetManager, pluginPath);

            // 获取宿主资源配置信息
            Resources r = context.getResources();
            // 特殊的Resources，加载插件里面的资源的Resources
            resources = new Resources(assetManager, r.getDisplayMetrics(), r.getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载插件里面的 class
     */
    private void initClassLoader(String pluginPath) {
        // 建立缓存目录  /data/data/当前应用的包名/plugins
        File fileDir = context.getDir("plugins", Context.MODE_PRIVATE);
        // Activity class
        dexClassLoader = new DexClassLoader(pluginPath, fileDir.getAbsolutePath(), null, context.getClassLoader());
    }

    public ClassLoader getClassLoader() {
        return dexClassLoader;
    }

    public Resources getResources() {
        return resources;
    }

}
