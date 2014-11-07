package de.brudaswen.picturewatcher.app;

import android.app.Activity;
import android.app.Application;

/**
 * Created by deekay on 12/06/14.
 */
public class App extends Application {

    private static App instance;

    private boolean isInForeground = false;
    public Activity activity;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static App get() {
        return instance;
    }

    public boolean isInForeground() {
        return isInForeground;
    }

    public void setInForeground(boolean isInForeground) {
        this.isInForeground = isInForeground;
    }
}
