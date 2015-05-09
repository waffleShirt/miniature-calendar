package com.waffleshirt.miniaturecalendarapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

/**
 * Created by Tom on 4/05/2014.
 */
public class WidgetProvider extends AppWidgetProvider
{
    private static final String TAG = "WidgetProvider";
    private static final int mHttpConnectTimeout = 5 * 1000;       // milliseconds
    private static final int mHttpReadTimeout = 30 * 1000;       // milliseconds

    @Override
    public void onDeleted(Context context, int[] appWidgetIds)
    {
        super.onDeleted(context, appWidgetIds);
        Log.d(TAG, "onDeleted()");

        Intent intent = new Intent(context.getApplicationContext(), WidgetUpdateService.class);
        intent.putExtra("PROVIDER_DELETED_FLAG", true);

        Log.d(TAG, "Informing update service of provider deletion");
        context.startService(intent);
    }

    @Override
    public void onDisabled(Context context)
    {
        super.onDisabled(context);
        Log.d(TAG, "onDisabled()");
    }

    @Override
    public void onEnabled(Context context)
    {
        super.onEnabled(context);
        Log.d(TAG, "onEnabled()");

        /*
        onEnabled will be called on a reboot. This requires the ImageLoader
        library to be initialised again. If we fail to provide it with a config
        an exception will be thrown.
         */

        // Setup the Universal Image Loader with a global configuration
        if (! (ImageLoader.getInstance().isInited()) )
        {
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                    .threadPriority(Thread.NORM_PRIORITY)
                    .denyCacheImageMultipleSizesInMemory()
                    .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                    .tasksProcessingOrder(QueueProcessingType.FIFO)
                    .imageDownloader(new BaseImageDownloader(context, mHttpConnectTimeout, mHttpReadTimeout))
                    .build();

            ImageLoader.getInstance().init(config);
        }
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds)
    {
        //super.onRestored(context, oldWidgetIds, newWidgetIds);
        Log.d(TAG, "onRestored()");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        Log.d(TAG, "onUpdate was called");

        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // Get all widget IDs
        ComponentName thisWidget = new ComponentName(context, WidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        // Build the intent to call the service
        Intent intent = new Intent(context.getApplicationContext(), WidgetUpdateService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

        // To prevent ANR timeouts we perform the update in a service
        Log.d(TAG, "Starting update service");
        context.startService(intent);
    }
}
