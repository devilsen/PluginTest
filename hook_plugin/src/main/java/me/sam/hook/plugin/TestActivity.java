package me.sam.hook.plugin;

import android.os.Bundle;
import android.widget.Toast;

/**
 * desc : Hook 插件目标界面
 * date : 2019-10-18
 * @author : dongSen
 */
public class TestActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hook_plugin);
        Toast.makeText(this, "这是Hook插件Activity", Toast.LENGTH_SHORT).show();
    }
}
