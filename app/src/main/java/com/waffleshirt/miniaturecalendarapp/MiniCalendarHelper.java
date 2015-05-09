package com.waffleshirt.miniaturecalendarapp;

import android.os.AsyncTask;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Tom on 7/05/2014.
 */
public class MiniCalendarHelper
{
    public static final String mURLPrefix = "http://miniature-calendar.com/wp-content/uploads";
    public static final String mFormatSuffix = ".jpg";
    /**
     * Returns the URL for the current days image
     * on the Miniature calendar website.
     * @return Image URL as a String
     */
    public static String GetTodayImageURL()
    {
        /*
        The format for images on the Miniature Calendar website is:
        http://miniature-calendar.com/wp-content/uploads/yyyy/mm/yymmddDAY.jpg
        where DAY is the day of the week as a three letter word.

        Example URL
        http://miniature-calendar.com/wp-content/uploads/2014/05/140507wed.jpg
         */

        // Start with the constant URL prefix
        String URL = mURLPrefix;

        // Get todays date
        Date d = new Date();

        // Extract the date information
        String year4digit = new SimpleDateFormat("yyyy").format(d);
        String year2digit = new SimpleDateFormat("yy").format(d);
        String month = new SimpleDateFormat("MM").format(d);
        String day = new SimpleDateFormat("dd").format(d);
        String dayAsWord = new SimpleDateFormat("EEE").format(d).toLowerCase();

        // Form full URL
        URL += "/" + year4digit;
        URL += "/" + month;
        URL += "/" + year2digit + month + day + dayAsWord;
        URL += mFormatSuffix;

        return URL;
    }
}
