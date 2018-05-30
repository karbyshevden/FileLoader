package com.karbyshev.fileloader;

import android.app.Application;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FileLoader.initInstance();
    }
}
