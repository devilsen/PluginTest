package me.sam.hook.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

/**
 * desc : 占位Activity，用来通过AMS检查
 * date : 2019-10-18
 *
 * @author : dongSen
 */
public class ProxyActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toast.makeText(this, "我是代理的Activity", Toast.LENGTH_SHORT).show();
    }
}
