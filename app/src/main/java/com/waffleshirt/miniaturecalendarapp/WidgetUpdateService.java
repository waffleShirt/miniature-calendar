package com.waffleshirt.miniaturecalendarapp;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.joda.time.LocalDate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Tom on 4/05/2014.
 */
public class WidgetUpdateService extends Service implements ImageLoadingListener, ImageTitleHelper.ImageTitleListener
{
    private AppWidgetManager mAppWidgetManager = null;
    private int[] mAllWidgetIds = null;

    private ImageDownloadHelper mImageDownloaderHelper = new ImageDownloadHelper();

    private boolean mTodaysImageLoaded = false;
    private LocalDate mTodaysDate = null;

    private static final String TAG = "WidgetUpdateService";

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate()");
    }

    public void onDestroy()
    {
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand called");

        if (intent.getBooleanExtra("PROVIDER_DELETED_FLAG", false) == true)
        {
            mTodaysImageLoaded = false;
            mTodaysDate = null;

            return START_NOT_STICKY;
        }

        mAppWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());

        mAllWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

        ComponentName thisWidget = new ComponentName(getApplicationContext(), WidgetProvider.class);

        for (int widgetId : mAllWidgetIds)
        {
            Log.d(TAG, "Widget ID:" + widgetId);
            RemoteViews remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.widget_homescreen_layout);

            // Update the text views
            updateTextViews(remoteViews);

            // Register an onClickListener that will open the full app when the image is clicked.
            Intent openAppIntent = new Intent("android.intent.action.MAIN");
            openAppIntent.setClass(this.getApplicationContext(), MainActivity.class);                                   // Launch the main activity
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, openAppIntent, 0);
            RemoteViews views = new RemoteViews(getApplication().getPackageName(), R.layout.widget_homescreen_layout);
            views.setOnClickPendingIntent(R.id.widgetCalendarImageView, pendingIntent);

            // Need to find a way to make clicking on the day text view call onReceive in the widgetprovider
            Intent updateWidgetIntent = new Intent("android.intent.action.USER_UPDATE_WIDGET");
            updateWidgetIntent.setClass(this.getApplicationContext(), HomeScreenWidgetActivity.class);                                   // Launch the main activity
            pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), widgetId, updateWidgetIntent, 0);
            views.setOnClickPendingIntent(R.id.widgetDayTextView, pendingIntent);

            mAppWidgetManager.updateAppWidget(widgetId, views);

            // Check to see if the date has change and we need to load the next calendar image
            if (mTodaysDate != null && !(new LocalDate().isEqual(mTodaysDate)))
            {
                // It has ticked over to a new day.
                mTodaysImageLoaded = false;
                mTodaysDate = null;
            }

            // Begin the process of displaying todays image
            if (!mTodaysImageLoaded)
            {
                mTodaysDate = new LocalDate();
                displayTodayImage();
            }

            // Begin the process of displaying todays image title
            new ImageTitleHelper().beginGetCaptionForDate(Calendar.getInstance(), this);

            mAppWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

        return START_NOT_STICKY;
    }

    private void updateTextViews(RemoteViews remoteViews)
    {
        // Get todays date, which is what we're interested in for the widget
        Date d = Calendar.getInstance().getTime();

        // Construct the necessary strings
        String dayWord = new SimpleDateFormat("EEEE").format(d);
        String monthWord = new SimpleDateFormat("MMMM").format(d);
        String dayNumber = new SimpleDateFormat("dd").format(d);
        String yearNumber = new SimpleDateFormat("yyyy").format(d);

        String monthDayYearText = monthWord + " " + dayNumber + ", " + yearNumber;

        // Apply the strings
        remoteViews.setTextViewText(R.id.widgetDayTextView, dayWord);
        remoteViews.setTextViewText(R.id.widgetMonthDayYearView, monthDayYearText);
    }

    private void displayTodayImage()
    {
        /*
          Get todays image URL
          We don't actually need to pass the image view that the image will be loaded to
          because it has to be found in a remote view in the callback that executes
          once the image URL is found. However we still have to pass a dummy ImageView
          because if we pass null the ImageLoader library will tell us that we have an
          invalid argument.
          */
        Log.d(TAG, "Requesting todays image to be loaded");
        mImageDownloaderHelper.displayTodayImage(new ImageView(this.getApplicationContext()), this);
    }

    // Implement an empty onBind to satisfy the extension of Service
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }


    //----------------------------------------------------------------------------------------------


    // Implementation required to receive an image title from the ImageTitleHelper
    public void onImageTitleAcquired(String title)
    {
        String finalTitle = "";

        if (title != "")
            finalTitle = "\"" + title + "\"";

        for (int widgetId : mAllWidgetIds)
        {
            RemoteViews remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.widget_homescreen_layout);
            remoteViews.setTextViewText(R.id.widgetImageTitleView, finalTitle);

            // Now update the widget
            mAppWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }


    //----------------------------------------------------------------------------------------------


    /*
    Method implementations required by ImageLoadingListener are included below
     */
    @Override
    public void onLoadingStarted(String imageUri, View view)
    {

    }
    @Override
    public void onLoadingFailed(String imageUri, View view, FailReason failReason)
    {
        // The image couldn't be found
        // Apply the image to the remote ImageView
        Log.d(TAG, "Image loading failed. Displaying load failed image");
        for (int widgetId : mAllWidgetIds)
        {
            RemoteViews remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.widget_homescreen_layout);

            remoteViews.setImageViewResource(R.id.widgetCalendarImageView, R.drawable.dummy_fail_image);

            // Now update the widget
            mAppWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

        mTodaysImageLoaded = false;

        // Stop the service?
        //stopSelf();

    }
    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage)
    {
        // Apply the image to the remote ImageView
        Log.d(TAG, "Image was successfully downloaded");
        Log.d(TAG, "Applying image to widget ImageView");
        for (int widgetId : mAllWidgetIds)
        {
            RemoteViews remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.widget_homescreen_layout);

            remoteViews.setImageViewBitmap(R.id.widgetCalendarImageView, loadedImage);

            // Now update the widget
            mAppWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

        mTodaysImageLoaded = true;

        // Stop the service?
        //stopSelf();
    }
    @Override
    public void onLoadingCancelled(String imageUri, View view)
    {

    }
}
