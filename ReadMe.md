# 浅谈Android插件化示例

#### Module说明

##### Hook式
1. hook_app    （宿主）
2. hook_plugin （插件） 

##### 占位式
1. place_app               （宿主）
2. place_plugin            （插件） 
3. place_plugin_interface  （接口库）

#### 运行说明
1. 以占位式为例，运行Build Apk(s)打包插件项目
2. 将插件apk改名为 plugin.apk 
3. 将 plugin.apk 文件放在手机根目录下
4. 运行place_app，点击 Load Plugin，Star Plugin Activity 即可启动插件


#### 配合源码阅读
* [Android源码  API Level 25](http://androidos.net.cn/androidossearch?query=ActivityThread&sid=&from=code)
* [文章地址](https://www.jianshu.com/p/c62eea0c3f7f)

>推广
>[高性能二维码扫描库](https://github.com/devilsen/CZXing)
