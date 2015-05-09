package com.waffleshirt.miniaturecalendarapp;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Tom on 4/06/2014.
 */
public class ImageTitleHelper
{
    private static String mPageUrlPrefix = "http://miniature-calendar.com/";
    private static final Date mPageUrlChangeEpoch = new GregorianCalendar(2012, 06, 15).getTime();

    // Date ranges for image titles based on different cases that exist on Miniature Calendar website

    // Image title appears as a title on the full page view for a calendar date
    private static final Date mImageTitleAsTitleStartDate = new GregorianCalendar(2012, 04, 31).getTime();
    // No end date for this case, has been ongoing since 31/05/12

    // Image title appears as a <H3> caption in English on the full page view for a calendar date
    private static final Date mImageTitleAsH3CaptionEnglishStartDate = new GregorianCalendar(2012, 00, 25).getTime();
    private static final Date mImageTitleAsH3CaptionEnglishEndDate = new GregorianCalendar(2012, 04, 30).getTime();

    // Image title appears as a <H3> caption in English or Japanese (inconsistent) on the full page view for a calendar date
    private static final Date mImageTitleAsH3CaptionStartDate = new GregorianCalendar(2012, 00, 16).getTime();
    private static final Date mImageTitleAsH3CaptionEndDate = new GregorianCalendar(2012, 00, 24).getTime();

    // Image title appears as a plain text caption in Japanese on the full page view for a calendar date
    private static final Date mImageTitleAsCaptionJapaneseStartDate = new GregorianCalendar(2011, 11, 1).getTime();
    private static final Date mImageTitleAsCaptionJapaneseEndDate = new GregorianCalendar(2012, 00, 15).getTime();

    private TitleDownloaderTask mTitleDownloaderTask;

    public void beginGetCaptionForDate(Calendar date, ImageTitleListener listener)
    {
        // Clean up any existing title downloader tasks, lest they cause us problems!
        if (mTitleDownloaderTask != null)
        {
            mTitleDownloaderTask.cancel(true);
            mTitleDownloaderTask = null;
        }

        mTitleDownloaderTask = new TitleDownloaderTask(date, listener);
        mTitleDownloaderTask.execute();
    }

    private String getUrlForDate(Calendar date)
    {
        /*
        We start by getting the URL for the image we are interested in.
        Fortunately all of the images on the Miniature Calendar website
        follow one of two formats (with one exception).

        From 20/04/11 until 14/07/12 the format was:
        http://miniature-calendar.com/m-d-DDD

        Example URL:
        http://miniature-calendar.com/2-26-sun/

        From 15/07/11 until now the format is:
        http://miniature-calendar.com/yymmdd/

        Example URL:

        http://miniature-calendar.com/140604/

        The only exception is 13/07/12 which has the URL:
        http://miniature-calendar.com/120713fri/
         */
        Date d = date.getTime();
        Date exceptionDate = new GregorianCalendar(2012, 06, 13).getTime();

        String Url = mPageUrlPrefix;

        if (d.before(mPageUrlChangeEpoch))
        {
            String month = new SimpleDateFormat("M").format(d);
            String day = new SimpleDateFormat("d").format(d);
            String dayWord = new SimpleDateFormat("EEE").format(d).toLowerCase();

            Url += month + "-" + day + "-" + dayWord;
        } else if (d.compareTo(exceptionDate) == 0)
        {
            Url = "http://miniature-calendar.com/120713fri/";
        } else
        {
            String dateCode = new SimpleDateFormat("yyMMdd").format(d);
            Url += dateCode;
        }

        return Url;
    }

    // Interface used by callers to receive the title
    public interface ImageTitleListener
    {
        void onImageTitleAcquired(String title);
    }

    // Task for downloading the image title off the UI thread
    private class TitleDownloaderTask extends AsyncTask<Void, Void, Void>
    {
        private ImageTitleListener mListener = null;
        private String mTitle = "";
        private Calendar mDate = null;

        public TitleDownloaderTask(Calendar date, ImageTitleListener listener)
        {
            mDate = date;
            mListener = listener;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            String Url = getUrlForDate(mDate);

            /*

            From 20/04/11 - 30/11/2011 Images had no caption

            -------------------------------------------------------------------------------

            1/12/2011 - 15/01/2012
            Image titles appeared as plain text captions under an image in a full page view
            for a calendar date. These captions were in Japanese.

            To select the title we select a div with class 'post-body' and then select
            the second <p> element which contains the title in Japanese.

            -------------------------------------------------------------------------------

            16/01/2012 - 24/01/2012
            Image titles appeared as a caption wrapped in a <H3> tag under an image in a
            full page view for a calendar date. The title was sometimes in Japanese and
            sometimes in English.

            To select the title we select a div with class 'post-body' and then
            grab the first (and only) h3 element in the div. This contains the
            image title.

            ---------------------The above an below cases are the same------------------

            From 25/01/2012 - 30/05/2012 the image titles were consistently
            in English and appeared as a caption under the image in the full
            page view for an image.

            To select the title we select a div with class 'post-body' and then
            grab the first (and only) h3 element in the div. This contains the
            image title.

            -------------------------------------------------------------------------------

            The image captions started appearing as a "title" on
            the image page from 31/05/12 onwards.

            To select the "title" we select a div with id 'single-wrapper'
            and then look for the first <a href inside the div. The text for
            the hyperlink is out title.

             */

            // Try and get the document
            Document doc = new Document("");
            try
            {
                // Get the document we are interested in
                doc = Jsoup.connect(Url).userAgent("Mozilla").get();
            }
            catch (IOException e)
            {
                // Couldn't connect for some reason. Return blank title
                Log.d("MiniCal", e.getMessage());
                e.printStackTrace();

                mTitle = "";
                return null;
            }

            mTitle = "";

            // Downloaded the page source, now we parse it with jsoup to get what we need
            if (isDateWithinRange(mDate.getTime(), mImageTitleAsCaptionJapaneseStartDate, mImageTitleAsCaptionJapaneseEndDate))
            {
                Element postBodyDiv = doc.select("div.post-body").first();

                if (postBodyDiv != null)
                {
                    // Select the second <p> element and get its text
                    mTitle = postBodyDiv.select("p").get(1).text();
                }
            }
            else if (isDateWithinRange(mDate.getTime(), mImageTitleAsH3CaptionStartDate, mImageTitleAsH3CaptionEnglishEndDate))
            {
                Element postBodyDiv = doc.select("div.post-body").first();

                if (postBodyDiv != null)
                {
                    // Select the second <p> element and get its text
                    mTitle = postBodyDiv.select("h3").first().text();
                }
            }
            else if (isDateWithinRange(mDate.getTime(), mImageTitleAsTitleStartDate, Calendar.getInstance().getTime()))
            {
                // Select the div with id single-wrapper
                Element singleWrapperDiv = doc.select("div#single-wrapper").first();

                if (singleWrapperDiv != null)
                {
                    // Select the first hyperlink and grab its text
                    mTitle = singleWrapperDiv.select("a[href]").first().text();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            // Perform the callback
            mListener.onImageTitleAcquired(mTitle);
        }
    }

    /**
     * Checks if a date falls between two dates. The start
     * and end dates are inclusive.
     * @param d Date to check
     * @param rangeStartInclusive Beginning of date range
     * @param rangeEndInclusive End of date range
     * @return True if date is in range, false otherwise.
     */
    private boolean isDateWithinRange(Date d, Date rangeStartInclusive, Date rangeEndInclusive)
    {
        // Credit to http://stackoverflow.com/a/494200/320867 for inclusive solution
        return !(d.before(rangeStartInclusive) || d.after(rangeEndInclusive));
    }
}
