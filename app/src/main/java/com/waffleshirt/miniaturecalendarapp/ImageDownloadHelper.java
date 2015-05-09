package com.waffleshirt.miniaturecalendarapp;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.joda.time.LocalDate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Tom on 8/05/2014.
 */
public class ImageDownloadHelper
{
    public static final String mUrlPrefix = "http://miniature-calendar.com/wp-content/uploads";
    public static final String mOldUrlPrefix = "http://t-tana-works.sakura.ne.jp/wp01/wp-content/uploads";                  // Images from epoch to 31/05/11 were held at a different address.
    public static final String mFormatSuffix = ".jpg";
    public static ImageLoader mImageLoader = ImageLoader.getInstance();
    public static final Date mEpoch = new GregorianCalendar(2011, 3, 20).getTime();
    public static final Date mUrlPrefixChangeEpoch = new GregorianCalendar(2011, 5, 1).getTime();
    public static final Date mConsistentUrlEpoch = new GregorianCalendar(2011, 11, 1).getTime();                            // From 1/12/11 images were sorted on the server by month not by year
    public static final Date mNov2011SpecialDateRangeStartNotInclusive = new GregorianCalendar(2011, 10, 1).getTime();      // From 02/11/11 - 30/11/11 the URL format is inconsistent and some days have more than one image.
    public static final Date mNov2011SpecialDateRangeEndNotInclusive = new GregorianCalendar(2011, 11, 1).getTime();        // We handle november as a completely separate case because of it's lack of a pattern for the URL
    public static final String mDateFormat = "dd/MM/yyyy";
    public static final LocalDate mCutoffDate = new LocalDate(2012, 07, 15);                                                // Cutoff date for using the more stable task system to acquire image URLs.
                                                                                                                            // Dates before this date will use the older system where all dates that fell outside
                                                                                                                            // of the patterns on the MC website were hardcoded

    private static final String TAG = "ImageDownloadHelper";

    private CalendarImageURLGetterTask mCalendarImageURLGetterTask = null;

    public static void Test()
    {
        // Valid day
        try
        {
            getImageUrlForDate(2014, 01, 21);       // Valid date
        }
        catch (InvalidParameterException e)
        {

        }
        try
        {
            getImageUrlForDate(2011, 01, 21);       // Before epoch
        }
        catch (InvalidParameterException e)
        {

        }
        try
        {
            getImageUrlForDate(2015, 01, 21);       // After today
        }
        catch (InvalidParameterException e)
        {

        }
        try
        {
            getImageUrlForDate(2010, 05, 48);       // Invalid
        }
        catch (InvalidParameterException e)
        {

        }
    }

    /**
     * Returns the URL for the current days image
     * on the Miniature calendar website.
     * @return Image URL as a String
     */
    public static String getTodayImageUrl()
    {
        /*
        The format for images on the Miniature Calendar website is:
        http://miniature-calendar.com/wp-content/uploads/yyyy/mm/yymmddDAY.jpg
        where DAY is the day of the week as a three letter word.

        Example URL
        http://miniature-calendar.com/wp-content/uploads/2014/05/140507wed.jpg
         */

        // Start with the constant URL prefix
        String URL = mUrlPrefix;

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

    /**
     * Kicks off a task to find todays calendar image URL.
     * When the task finishes it will hand over to the appropriate
     * callback to download the image.
     */
    public void beginAcquiringCalendarImage(LocalDate date, ImageView imgView, ImageLoadingListener callbackObject)
    {
        // Clean up any existing calendar image URL getter tasks, lest they cause us problems!
        if (mCalendarImageURLGetterTask != null)
        {
            mCalendarImageURLGetterTask.cancel(true);
            mCalendarImageURLGetterTask = null;
        }

        mCalendarImageURLGetterTask = new CalendarImageURLGetterTask(date.toDate() ,imgView, callbackObject);
        mCalendarImageURLGetterTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Get the Image Url for a specific date.
     * @param year Desired year
     * @param month Desired month in range 1 to 12
     * @param day Desire day in range 1 to 31
     * @return If the date specified is invalid, before the
     * Miniature Calendar epoch (April 20, 2011) or after the
     * current day, a null string is returned, otherwise the
     * full Url string is returned.
     */
    public static String getImageUrlForDate(int year, int month, int day)
    {
        // Construct date objects
        Date d = new GregorianCalendar(year, month - 1, day).getTime();             // Have to sub 1 off month because it is 0 based
        Date today = GregorianCalendar.getInstance().getTime();

        // Check if date passed in is valid
        if (!isDateValid(String.format("%02d", day) + "/" + String.format("%02d", month) + "/" + String.format("%04d", year)))
        {
            throw new InvalidParameterException("The date passed in is not a valid date on the Gregorian calendar.");
        }
        else if (d.before(mEpoch))
        {
            throw new InvalidParameterException("The date passed in is before Miniature Calendar began, April 20, 2011.");
        }
        else if (d.after(today))
        {
            throw new InvalidParameterException("The date passed in is after today and is not valid");
        }

        if (d.after(mNov2011SpecialDateRangeStartNotInclusive) && d.before(mNov2011SpecialDateRangeEndNotInclusive))
        {
            /*
            During November 2011 there was a lot if inconsistency in the image URL formats.
            We handle November 2011 ans a totally separate case.
             */
            return getImageUrlForNov2011(year, month, day);
        }

        /*
        The format for images on the Miniature Calendar website is:
        http://miniature-calendar.com/wp-content/uploads/yyyy/mm/yymmddDAY.jpg
        where DAY is the day of the week as a three letter word.

        Example URL
        http://miniature-calendar.com/wp-content/uploads/2014/05/140507wed.jpg

        From 20/04/11 - 31/05/11 the url format was slightly different. It was in the format:
        http://t-tana-works.sakura.ne.jp/wp01/wp-content/uploads/yyyy/yy/yymmdd.jpg

        Example URL
        http://t-tana-works.sakura.ne.jp/wp01/wp-content/uploads/2011/11/110420.jpg
         */

        /*
        Start with the constant URL prefix.
        Note that we have to check if the date we are requesting
        is before the date that the URL prefix changed (see mUrlChangeEpoch)
        and if it is before that date then we use the old url prefix.
         */
        String Url = "";

        if (d.before(mUrlPrefixChangeEpoch))
            Url = mOldUrlPrefix;
        else
            Url = mUrlPrefix;

        // Extract the date information
        String year4digit = new SimpleDateFormat("yyyy").format(d);
        String year2digit = new SimpleDateFormat("yy").format(d);
        String _month = new SimpleDateFormat("MM").format(d);
        String _day = new SimpleDateFormat("dd").format(d);
        String dayAsWord = new SimpleDateFormat("EEE").format(d).toLowerCase();

        // Form full URL
        Url += "/" + year4digit;

        if (d.before(mConsistentUrlEpoch))                  // After this date images are sorted in folders by month.
            Url += "/" + year2digit;
        else
            Url += "/" + _month;

        Url += "/" + year2digit + _month + _day;

        // Only add the 3 letter day if the desired date is on or after the URL prefix change epoch
        if (!d.before(mUrlPrefixChangeEpoch))
            Url +=  dayAsWord;

        Url += mFormatSuffix;

        return Url;
    }

    /**
     * Get the Image Url for days in November 2011.
     * From 02/11/11 until 30/11/11 the URL format was
     * very inconsistent, with many days having a superfluous 1
     * at the end of the URL. Other days had more than one image
     * and so the URL had a 1, 2 or 3 at the end of it depending
     * on how many images were displayed on the day.
     * @param year Desired year
     * @param month Desired month in range 1 to 12
     * @param day Desire day in range 1 to 31
     * @return If the date specified is invalid, before the
     * Miniature Calendar epoch (April 20, 2011) or after the
     * current day, a null string is returned, otherwise the
     * full Url string is returned.
     */
    public static String getImageUrlForNov2011(int year, int month, int day)
    {
        // Construct date objects
        Date d = new GregorianCalendar(year, month - 1, day).getTime();             // Have to sub 1 off month because it is 0 based
        Date today = GregorianCalendar.getInstance().getTime();

        // Check if date passed in is valid
        if (!isDateValid(String.format("%02d", day) + "/" + String.format("%02d", month) + "/" + String.format("%04d", year)))
        {
            throw new InvalidParameterException("The date passed in is not a valid date on the Gregorian calendar.");
        }
        else if (d.before(mEpoch))
        {
            throw new InvalidParameterException("The date passed in is before Miniature Calendar began, April 20, 2011.");
        }
        else if (d.after(today))
        {
            throw new InvalidParameterException("The date passed in is after today and is not valid");
        }

        String Url = mUrlPrefix;

        // Extract the date information
        String year4digit = new SimpleDateFormat("yyyy").format(d);
        String year2digit = new SimpleDateFormat("yy").format(d);
        String _month = new SimpleDateFormat("MM").format(d);
        String _day = new SimpleDateFormat("dd").format(d);
        String dayAsWord = new SimpleDateFormat("EEE").format(d).toLowerCase();

        // Form full URL
        Url += "/" + year4digit;

        Url += "/" + "12";          // For some reason the Nov 2011 images are in the Dec 2011 folder

        Url += "/" + year2digit + _month + _day;

        // Only add the 3 letter day if the desired date is on or after the URL prefix change epoch
        Url +=  dayAsWord;

        /*
        Specific days had a 1 before the format suffix, either because more than one image
        was displayed on that day, or for no obvious reason. There isn't currently a plan
        on how to display the multiple images. Instead we'll just show the first image and
        solve that problem some other time.
         */
        Date nov2 = new GregorianCalendar(2011, 10, 2).getTime();
        Date nov3 = new GregorianCalendar(2011, 10, 3).getTime();
        Date nov5 = new GregorianCalendar(2011, 10, 5).getTime();
        Date nov7 = new GregorianCalendar(2011, 10, 7).getTime();
        Date nov8 = new GregorianCalendar(2011, 10, 8).getTime();
        Date nov11 = new GregorianCalendar(2011, 10, 11).getTime();
        Date nov12 = new GregorianCalendar(2011, 10, 12).getTime();
        Date nov13 = new GregorianCalendar(2011, 10, 13).getTime();
        Date nov14 = new GregorianCalendar(2011, 10, 14).getTime();
        Date nov15 = new GregorianCalendar(2011, 10, 15).getTime();
        Date nov16 = new GregorianCalendar(2011, 10, 16).getTime();
        Date nov17 = new GregorianCalendar(2011, 10, 17).getTime();
        Date nov19 = new GregorianCalendar(2011, 10, 19).getTime();
        Date nov20 = new GregorianCalendar(2011, 10, 20).getTime();
        Date nov23 = new GregorianCalendar(2011, 10, 23).getTime();
        Date nov26 = new GregorianCalendar(2011, 10, 26).getTime();
        Date nov27 = new GregorianCalendar(2011, 10, 27).getTime();
        Date nov28 = new GregorianCalendar(2011, 10, 28).getTime();
        Date nov29 = new GregorianCalendar(2011, 10, 29).getTime();
        Date nov30 = new GregorianCalendar(2011, 10, 30).getTime();

        if (d.compareTo(nov2) == 0 ||
            d.compareTo(nov3) == 0 ||
            d.compareTo(nov5) == 0 ||
            d.compareTo(nov7) == 0 ||
            d.compareTo(nov8) == 0 ||
            d.compareTo(nov11) == 0 ||
            d.compareTo(nov12) == 0 ||
            d.compareTo(nov13) == 0 ||
            d.compareTo(nov14) == 0 ||
            d.compareTo(nov15) == 0 ||
            d.compareTo(nov16) == 0 ||
            d.compareTo(nov17) == 0 ||
            d.compareTo(nov19) == 0 ||
            d.compareTo(nov20) == 0 ||
            d.compareTo(nov23) == 0 ||
            d.compareTo(nov26) == 0 ||
            d.compareTo(nov27) == 0 ||
            d.compareTo(nov28) == 0 ||
            d.compareTo(nov29) == 0 ||
            d.compareTo(nov30) == 0)
        {
            Url += "1";
        }

        Url += mFormatSuffix;

        return Url;
    }

    /**
     * Validates a date. Implementation taken from
     * http://www.mkyong.com/java/how-to-check-if-date-is-valid-in-java/
     * @param dateToValidate The date to be validated, should be in dd/MM/yyyy format
     * @return True if date is valid, otherwise false
     */
    private static boolean isDateValid(String dateToValidate)
    {
        if(dateToValidate == null)
        {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(mDateFormat);
        sdf.setLenient(false);

        try
        {
            //if not valid, it will throw ParseException
            Date date = sdf.parse(dateToValidate);
            System.out.println(date);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Displays the image for the current date
     * in the specified image view.
     * @param imgView ImageView that will display the image
     */
    public void displayTodayImage(ImageView imgView, ImageLoadingListener callbackObject)
    {
        beginAcquiringCalendarImage(new LocalDate(), imgView, callbackObject);
    }

    /**
     * Displays a n image for a specific date. If the date given
     * is not valid then no image will be displayed.
     * @param year Desired year
     * @param month Desired month in range 1 to 12
     * @param day Desire day in range 1 to 31
     * @param imgView ImageView that will display the image
     */
    public void displayImageForDate(int year, int month, int day, ImageView imgView, ImageLoadingListener callbackObject)
    {
        LocalDate date = new LocalDate(year, month, day);

        if (date.isBefore(mCutoffDate))
        {
            // Use the old system for getting calendar image URLS. This system was less stable
            // but should work fine as all dates that fell outside of patterns have the URLs
            // either hardcoded or a fix is implemented
            String urlForDate = getImageUrlForDate(year, month, day);

            if (urlForDate != null)
            {
                mImageLoader.displayImage(urlForDate, imgView, callbackObject);
            }
        }
        else
        {
            // Calendar image requested can be found using the more stable URL acquisition method.
            beginAcquiringCalendarImage(new LocalDate(year, month, day), imgView, callbackObject);
        }
    }

    //----------------------------------------------------------------------------------------------

    // Task for finding the image URL off the UI thread
    private class CalendarImageURLGetterTask extends AsyncTask<Void, Void, Void>
    {
        private String mCalImgURL = "";
        private String mCalPageURL = "";
        private ImageView mImgView;
        private ImageLoadingListener mCallbackObject;
        private Date mDate = null;

        public CalendarImageURLGetterTask(Date date, ImageView imgView, ImageLoadingListener callbackObject)
        {
            mDate = date;
            mImgView = imgView;
            mCallbackObject = callbackObject;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            /*
            The format for images on the Miniature Calendar website is:
            http://miniature-calendar.com/wp-content/uploads/yyyy/mm/yymmddDAY.jpg
            where DAY is the day of the week as a three letter word.

            Example URL
            http://miniature-calendar.com/wp-content/uploads/2014/05/140507wed.jpg

            Occasionally there are exceptions. For example, April 26 2015
            http://miniature-calendar.com/wp-content/uploads/2015/04/150426sun1.jpg
            The trailing 1 after the DAY token breaks from the standard URL.

            It would be better if we could pull out the URL of the image in a way
            that is less reliant on constructing the full URL manually. The format
            of the URL for each calendar day has been consistent for a few years,
            so we'd be better off just trying to find the URL or the image on the page.

            Since July 16, 2012 the URL for each calendar day (the page that actually
            contains the calendar image) has been of the format:
            http://miniature-calendar.com/YYMMDD/
            Example:
            http://miniature-calendar.com/120716/

            This should always work for finding the image for the current date.
            That is, as long as the site doesn't change again....

            To find the image URL we first construct the URL to the page on the
            Miniature Calendar website that contains the image for a chosen date.
            Then using JSOUP we drill down to find the div with class post-body (this
            only exists once), we then grab the first img src tag and pull out the
            text for that tag. Pretty simple
             */

            Log.d(TAG, "Beginning task to acquire image URL");

            // Extract the date information
            String year2digit = new SimpleDateFormat("yy").format(mDate);
            String month = new SimpleDateFormat("MM").format(mDate);
            String day = new SimpleDateFormat("dd").format(mDate);

            // Form full URL
            mCalPageURL = "http://miniature-calendar.com/";
            mCalPageURL += year2digit + month + day + "/";

            // Get the calendar page as a document
            Log.d(TAG, "Connecting to calendar page");
            Document doc = new Document("");
            try
            {
                // Get the document we are interested in
                doc = Jsoup.connect(mCalPageURL).userAgent("Mozilla").get();
            }
            catch (IOException e)
            {
                // Couldn't connect for some reason. Set the image URL
                // as some junk to indicate a fail. This will cause the
                // image loading listener to execute the image loading
                // failed handler.
                Log.d(TAG, e.getMessage());
                e.printStackTrace();

                mCalImgURL = "-1";
                return null;
            }

            // Now find the div with class post-body
            Log.d(TAG, "Got calendar page source. Extracting calendar img URL");
            Element postBodyDiv = doc.select("div.post-body").first();

            if (postBodyDiv != null)
            {
                // Select the img src tag
                Element img = postBodyDiv.select("img").first();

                /*
                Because the website uses lazy loading we can't use the src url
                as it is initially the lazy loading place holder which changes
                once the full images has loaded. Instead we use the data-lazy-src
                which dontains the URL we really care about.
                 */
                mCalImgURL = img.absUrl("data-lazy-src");
            }
            else
            {
                // Couldn't find the URL. Set the image URL
                // as some junk to indicate a fail. This will cause the
                // image loading listener to execute the image loading
                // failed handler.
                Log.d(TAG, "Couldn't get image URL. Setting image URL to unreachable string");
                mCalImgURL = "-1";
                return null;
            }

            Log.d(TAG, "Image URL was acquired successfully");
            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            Log.d(TAG, "Displaying image with result"); // !!!! What is the result?
            mImageLoader.displayImage(mCalImgURL, mImgView, mCallbackObject);
        }
    }
}
