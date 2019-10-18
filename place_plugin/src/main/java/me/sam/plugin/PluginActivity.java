package me.sam.plugin;

import android.os.Bundle;
import android.widget.Toast;

public class PluginActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_test);

        // 只能使用appActivity，因为没有上下文环境，this会报错
        Toast.makeText(appActivity, "这是插件里的Activity", Toast.LENGTH_SHORT).show();
    }

}
