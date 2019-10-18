package me.sam.hook.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class HookApplication extends Application {

    private static final String TAG = "Hook";
    private static final String PLUGIN_NAME = "plugin.apk";
    private static final int LAUNCH_ACTIVITY = 1000;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            hookAms();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "hookAms失败" + e.toString());
        }

        try {
            hookLaunchActivity();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "hookLaunchActivity失败" + e.toString());
        }

        try {
            pluginToAppAction();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "pluginToAppAction失败 " + e.toString());
        }
    }


    /**
     * 在执行 AMS 之前，把目标Activity 替换为可用的 ProxyActivity，从而通过ActivityNotFound的检查
     */
    private void hookAms() throws Exception {
        Class mIActivityManagerClass = Class.forName("android.app.IActivityManager");
        // 拿到IActivityManager对象，才能让动态代理里面的 invoke 正常执行下
        // 通过查看源码得知，执行这个方法 static public IActivityManager getDefault()，就能拿到 IActivityManager
        Class mActivityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
        final Object mIActivityManager = mActivityManagerNativeClass.getMethod("getDefault").invoke(null);

        // 为 IActivityManager 添加动态代理，替换要启动的Activity
        Object mIActivityManagerProxy = Proxy.newProxyInstance(
                HookApplication.class.getClassLoader(),

                new Class[]{mIActivityManagerClass},

                new InvocationHandler() { // IActivityManager 接口的回调方法

                    /**
                     * @param proxy
                     * @param method IActivityManager里面的方法
                     * @param args IActivityManager里面的参数
                     * @return
                     * @throws
                     */

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        if ("startActivity".equals(method.getName())) {
                            // 当执行 startActivity 分支时，替换目标 Activity
                            Intent intent = new Intent(HookApplication.this, ProxyActivity.class);
                            // 通过 extra 把目标Activity传递过去
                            intent.putExtra("actionIntent", ((Intent) args[2]));
                            args[2] = intent;
                        }

                        Log.d(TAG, "拦截到了IActivityManager里面的方法 " + method.getName());

                        // 继续执行下面的方法
                        return method.invoke(mIActivityManager, args);
                    }
                });

        // Hook点
        Class mSingletonClass = Class.forName("android.util.Singleton");
        // 获取此字段 mInstance
        Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        // 替换 源码中的IActivityManager
        mInstanceField.set(getGDefault(), mIActivityManagerProxy);
    }

    /**
     * 通过 ActivityManagerNative 拿到 gDefault 对象
     */
    private Object getGDefault() {
        try {
            Class mActivityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
            Field gDefaultField = mActivityManagerNativeClass.getDeclaredField("gDefault");
            gDefaultField.setAccessible(true);
            return gDefaultField.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Hook LaunchActivity
     * 这里将要实例化Activity，我们要把上一步替换的ProxyActivity换回来
     * <p>
     * 这里我们的 目标 是替换源码里的Handler
     * 源码地址：http://androidos.net.cn/android/7.1.1_r28/xref/frameworks/base/core/java/android/app/ActivityThread.java
     * 1.寻找H，先寻找ActivityThread
     * 在源码中发现，只需要执行 currentActivityThread() 方法，即可获取ActivityThread
     * 2.通过ActivityThread 找到 mH
     */
    private void hookLaunchActivity() throws Exception {
        Field mCallbackFiled = Handler.class.getDeclaredField("mCallback");
        mCallbackFiled.setAccessible(true); // 授权

        Class mActivityThreadClass = Class.forName("android.app.ActivityThread");
        // 获取ActivityThread对象
        Object mActivityThread = mActivityThreadClass.getMethod("currentActivityThread").invoke(null);
        // 获取mH变量字段
        Field mHField = mActivityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        // 获取真正Handler对象
        Handler mH = (Handler) mHField.get(mActivityThread);
        // 替换为我们自己的Handler（增加处理 LAUNCH_ACTIVITY 的逻辑）
        mCallbackFiled.set(mH, new MyCallback(mH));
    }

    class MyCallback implements Handler.Callback {

        private Handler mH;

        MyCallback(Handler mH) {
            this.mH = mH;
        }

        @Override
        public boolean handleMessage(Message msg) {
            // 把ProxyActivity 换成  目标Activity
            if (msg.what == LAUNCH_ACTIVITY) {
                Object obj = msg.obj;
                try {
                    // 我们要获取之前Hook携带过来的 TestActivity
                    Field intentField = obj.getClass().getDeclaredField("intent");
                    intentField.setAccessible(true);
                    // 获取 Intent 对象
                    Intent intent = (Intent) intentField.get(obj);
                    // 获取 actionIntent 中的值（就是我们在上面传过来的目标Activity Intent）
                    Intent actionIntent = intent.getParcelableExtra("actionIntent");
                    if (actionIntent != null) {
                        // 替换ProxyActivity 为 TestActivity
                        intentField.set(obj, actionIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mH.handleMessage(msg);
            return true;
        }
    }


    /**
     * 将插件的dexElements 和 宿主中的 dexElements 融为一体
     */
    private void pluginToAppAction() throws Exception {
        // 1. 找到宿主的 dexElements
        PathClassLoader pathClassLoader = (PathClassLoader) this.getClassLoader();
        Class mBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        // 获取宿主的 DexPathList
        Field pathListField = mBaseDexClassLoaderClass.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object mDexPathList = pathListField.get(pathClassLoader);

        Field dexElementsField = mDexPathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        // 本质就是 Element[] dexElements
        Object dexElements = dexElementsField.get(mDexPathList);

        // 2. 找到插件中的 dexElements
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + PLUGIN_NAME);
        if (!file.exists()) {
            throw new FileNotFoundException("没有找到插件包");
        }
        String pluginPath = file.getAbsolutePath();
        // 创建缓存目录 data/data/包名/plugins/
        File fileDir = this.getDir("plugins", Context.MODE_PRIVATE);
        DexClassLoader dexClassLoader = new
                DexClassLoader(pluginPath, fileDir.getAbsolutePath(), null, getClassLoader());

        Class mBaseDexClassLoaderClassPlugin = Class.forName("dalvik.system.BaseDexClassLoader");
        // 获取插件的 DexPathList pathList
        Field pathListFieldPlugin = mBaseDexClassLoaderClassPlugin.getDeclaredField("pathList");
        pathListFieldPlugin.setAccessible(true);
        Object mDexPathListPlugin = pathListFieldPlugin.get(dexClassLoader);

        Field dexElementsFieldPlugin = mDexPathListPlugin.getClass().getDeclaredField("dexElements");
        dexElementsFieldPlugin.setAccessible(true);
        // 本质就是 Element[] dexElements
        Object dexElementsPlugin = dexElementsFieldPlugin.get(mDexPathListPlugin);

        // 3. 创建 新的 dexElements []
        int mainDexLength = Array.getLength(dexElements);
        int pluginDexLength = Array.getLength(dexElementsPlugin);
        int sumDexLength = mainDexLength + pluginDexLength;

        // 创建数组对象
        Object newDexElements = Array.newInstance(dexElements.getClass().getComponentType(), sumDexLength);

        // 4. 宿主dexElements + 插件dexElements --> 新的 DexElements
        for (int i = 0; i < sumDexLength; i++) {
            if (i < mainDexLength) {
                // 先融合宿主
                Array.set(newDexElements, i, Array.get(dexElements, i));
            } else { // 再融合插件的
                Array.set(newDexElements, i, Array.get(dexElementsPlugin, i - mainDexLength));
            }

        }

        // 5. 把新的 newDexElements，设置到宿主中去
        dexElementsField.set(mDexPathList, newDexElements);

        // 处理加载插件中的布局
        doPluginLayoutLoad();
    }

    private Resources resources;
    private AssetManager assetManager;

    /**
     * 处理加载插件中的布局
     * Resources
     */
    private void doPluginLayoutLoad() throws Exception {
        assetManager = AssetManager.class.newInstance();

        // 把插件的路径 给 AssetManager
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + PLUGIN_NAME);
        if (!file.exists()) {
            throw new FileNotFoundException("没有找到插件包");
        }

        // 执行此 public final int addAssetPath(String path) 方法，把插件的路径添加进去
        Method method = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
        method.setAccessible(true);
        method.invoke(assetManager, file.getAbsolutePath());

        // 实例化此方法 final StringBlock[] ensureStringBlocks()
        Method ensureStringBlocksMethod = assetManager.getClass().getDeclaredMethod("ensureStringBlocks");
        ensureStringBlocksMethod.setAccessible(true);
        // 执行了ensureStringBlocks 插件中的 string.xml color.xml anim.xml 才被初始化
        ensureStringBlocksMethod.invoke(assetManager);

        // 拿到宿主的配置信息
        Resources r = getResources();
        // 特殊 Resources 专门加载插件资源
        resources = new Resources(assetManager, r.getDisplayMetrics(), r.getConfiguration());
    }

    @Override
    public Resources getResources() {
        return resources == null ? super.getResources() : resources;
    }

    @Override
    public AssetManager getAssets() {
        return assetManager == null ? super.getAssets() : assetManager;
    }
}
