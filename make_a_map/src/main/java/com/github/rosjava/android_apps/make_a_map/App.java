package com.github.rosjava.android_apps.make_a_map;

import android.app.Application;
import android.os.Environment;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
//        String absolutePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/";
//        RCrashHandler.getInstance(absolutePath).init(this, null);
    }
}
