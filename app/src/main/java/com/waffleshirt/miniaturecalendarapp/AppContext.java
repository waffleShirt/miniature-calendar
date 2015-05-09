package com.waffleshirt.miniaturecalendarapp;

import android.app.Application;
import android.content.Context;

/**
 * Created by Tom on 12/02/2015.
 */
public class AppContext extends Application
{
    private static AppContext instance = null;

    public static AppContext getInstance()
    {
        return instance;
    }

    public static Context getContext()
    {
        if (instance == null)
        {
            instance = new AppContext();
        }

        return instance.getApplicationContext();
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
    }
}
