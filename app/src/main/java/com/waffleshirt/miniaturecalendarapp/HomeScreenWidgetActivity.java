package com.waffleshirt.miniaturecalendarapp;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class HomeScreenWidgetActivity extends BroadcastReceiver
{
    private static final String TAG = "HomeScreenActivity";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(TAG, "onReceive was called");

        if (intent.getAction().equals("android.intent.action.USER_UPDATE_WIDGET"))
        {
            /*
            The day text view in the widget was pressed. This indicates a forced
            update to the widget should be performed.
             */
            // Start the update service same as we do in onUpdate
            // Get all widget IDs
            ComponentName thisWidget = new ComponentName(context, WidgetProvider.class);
            int[] allWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(thisWidget);

            // Build the intent to call the service
            Intent updateIntent = new Intent(context.getApplicationContext(), WidgetUpdateService.class);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

            // To prevent ANR timeouts we perform the update in a service
            Log.d(TAG, "Starting update service");
            context.startService(updateIntent);
        }
    }
}
