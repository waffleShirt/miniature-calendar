package com.waffleshirt.miniaturecalendarapp;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.waffleshirt.miniaturecalendarapp.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class MainActivity extends Activity implements ImageLoadingListener, ImageTitleHelper.ImageTitleListener
{
    private Calendar mCurrImgDate;
    private Menu mActionBarMenu = null;
    private static String mTag = "MainActivity";
    private ProgressDialog mProgressDialog = null;
    public static final Date mEpoch = new GregorianCalendar(2011, 3, 20).getTime();

    private static String mPageUrlPrefix = "http://miniature-calendar.com/";
    private static final Date mPageUrlChangeEpoch = new GregorianCalendar(2012, 06, 15).getTime();

    private static final Date mImageCaptionAsTitleEpoch = new GregorianCalendar(2012, 04, 31).getTime();

    public Calendar getCurrentImageDate()
    {
        return mCurrImgDate;
    }

    private boolean mImageAcquired = false;
    private boolean mTitleAcquired = false;

    private static final int mHttpConnectTimeout = 5 * 1000;       // milliseconds
    private static final int mHttpReadTimeout = 30 * 1000;       // milliseconds

    private ImageTitleHelper mImageTitleHelper = new ImageTitleHelper();
    private ImageDownloadHelper mImageDownloaderHelper = new ImageDownloadHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Disable the title in the action bar
        getActionBar().setDisplayShowTitleEnabled(false);

        if (! (ImageLoader.getInstance().isInited()) )
        {
            // Setup the Universal Image Loader with a global configuration
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                    .threadPriority(Thread.NORM_PRIORITY)
                    .denyCacheImageMultipleSizesInMemory()
                    .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                    .tasksProcessingOrder(QueueProcessingType.FIFO)
                    .imageDownloader(new BaseImageDownloader(getApplicationContext(), mHttpConnectTimeout, mHttpReadTimeout))
                    .build();

            ImageLoader.getInstance().init(config);
        }

        // Hide the error submission button
        disableErrorSubmissionViews();

        // Initialise todays date
        mCurrImgDate = Calendar.getInstance();

        // Start by showing todays image
        displayTodayImage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main_actions, menu);

        // Retain reference
        mActionBarMenu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        else if (id == R.id.action_go_to_day)
        {
            disableErrorSubmissionViews();

            /*
            Display the date picker. Once a date is selected
            the image will be displayed in the UI.
             */
            DialogFragment datePickerDialog = new DatePickerFragment(this);
            datePickerDialog.show(getFragmentManager(), "DatePicker");
        }
        else if (id == R.id.action_prev_img)
        {
            disableErrorSubmissionViews();

            // Adjust the calendar date to the previous day
            mCurrImgDate.add(Calendar.DAY_OF_YEAR, -1);

            // Display the image
            displayImageWithDate(mCurrImgDate);
        }
        else if (id == R.id.action_next_img)
        {
            disableErrorSubmissionViews();

            // Adjust the calendar date to the previous day
            mCurrImgDate.add(Calendar.DAY_OF_YEAR, 1);

            // Display the image
            displayImageWithDate(mCurrImgDate);
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayTodayImage()
    {
        // Begin the process of downloading todays image
        beginCalendarImageAcquisition();

        // Get todays image caption
        Log.d(mTag, "Begin acquiring today's image caption");
        mImageTitleHelper.beginGetCaptionForDate(mCurrImgDate, this);

        // Get todays image
        Log.d(mTag, "Begin acquiring today's image");
        mImageDownloaderHelper.displayTodayImage((ImageView) findViewById(R.id.calendarImageView), this);
    }

    public void displayImageWithDate(Calendar date)
    {
        // Update action bar
        updateActionBarMenu();

        // Set the date for the current image
        mCurrImgDate = date;

        beginCalendarImageAcquisition();

        // Get the image title
        Log.d(mTag, "Begin acquiring image caption for specific date");
        mImageTitleHelper.beginGetCaptionForDate(mCurrImgDate, this);

        // Get the image
        Log.d(mTag, "Begin acquiring image for specific date");
        mImageDownloaderHelper.displayImageForDate(date.get(Calendar.YEAR),
                                                date.get(Calendar.MONTH) + 1,                           // Months are 0 based
                                                date.get(Calendar.DAY_OF_MONTH),
                                                (ImageView)findViewById(R.id.calendarImageView),
                                                this);
    }

    public void displayImageWithDate(int year, int month, int day)
    {
        // Update action bar
        updateActionBarMenu();

        // Set the date for the current image
        mCurrImgDate = new GregorianCalendar(year, month, day);

        beginCalendarImageAcquisition();

        // Get the image title
        Log.d(mTag, "Begin acquiring image caption for specific date");
        mImageTitleHelper.beginGetCaptionForDate(mCurrImgDate, this);

        // Get the image
        Log.d(mTag, "Begin acquiring image for specific date");
        mImageDownloaderHelper.displayImageForDate(year, month + 1, day, (ImageView)findViewById(R.id.calendarImageView), this);
    }

    /**
     * Updates the Action Bar Menu by enabling/disabling
     * buttons dependent on the current date being displayed.
     */
    private void updateActionBarMenu()
    {
        /*
        Invalidate the current menu to force an update.
        All the actual work is done in onPrepareOptionsMenu()
        */
        invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        Calendar today = Calendar.getInstance();

        // Disable the next button if we are at the current date already
        if (mCurrImgDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            mCurrImgDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR))
        {
            MenuItem nextButton = mActionBarMenu.findItem(R.id.action_next_img);
            nextButton.setEnabled(false);
        }
        else
        {
            MenuItem nextButton = mActionBarMenu.findItem(R.id.action_next_img);
            nextButton.setEnabled(true);
        }

        // Disable the previous button if we are on the epoch date for the calendar
        if (mCurrImgDate.getTime().compareTo(mEpoch) == 0)
        {
            MenuItem prevButton = mActionBarMenu.findItem(R.id.action_prev_img);
            prevButton.setEnabled(false);
        }
        else
        {
            MenuItem prevButton = mActionBarMenu.findItem(R.id.action_prev_img);
            prevButton.setEnabled(true);
        }

        return true;
    }

    /**
     * Updates the views that display the day and month/day/year.
     */
    private void updateTextViews()
    {
        Date d = mCurrImgDate.getTime();

        String dayWord = new SimpleDateFormat("EEEE").format(d);
        String monthWord = new SimpleDateFormat("MMMM").format(d);
        String dayNumber = new SimpleDateFormat("dd").format(d);
        String yearNumber = new SimpleDateFormat("yyyy").format(d);

        TextView dayView = (TextView)findViewById(R.id.dayTextView);
        dayView.setText(dayWord);

        TextView monthDayYearView = (TextView)findViewById(R.id.monthDayYearView);
        monthDayYearView.setText(monthWord + " " + dayNumber + ", " + yearNumber);
    }

    /**
     * Begins the process of acquiring a calendar image
     * by hiding the current image and image caption.
     */
    private void beginCalendarImageAcquisition()
    {
        displayProgressDialog();

        // Update the text views so that the new date appears immediately
        updateTextViews();

        // Make the views invisible
        TextView titleView = (TextView)findViewById(R.id.imageTitleView);
        titleView.setVisibility(View.INVISIBLE);

        ImageView imageView = (ImageView)findViewById(R.id.calendarImageView);
        imageView.setVisibility(View.INVISIBLE);
    }

    /**
     * When both the desired calendar image and it's title
     * have been downloaded then we can finalise all the
     * views. The text views will be updated, and the title
     * and image views will become visible. This way all of
     * the screen content is updated at once, instead of
     * slightly staggered depending on which downloads first
     * out of the image and the title.
     */
    private void finaliseCalendarImageAcquired()
    {
        // Hide the progress dialog
        mProgressDialog.hide();

        // Reset control bools
        mImageAcquired = false;
        mTitleAcquired = false;

        // Make the views visible
        TextView titleView = (TextView)findViewById(R.id.imageTitleView);
        titleView.setVisibility(View.VISIBLE);

        ImageView imageView = (ImageView)findViewById(R.id.calendarImageView);
        imageView.setVisibility(View.VISIBLE);
    }

    private void enableErrorSubmissionViews()
    {
        Button errorSubmission = (Button)findViewById(R.id.errorSubmissionButton);
        if (errorSubmission != null)
        {
            errorSubmission.setVisibility(View.VISIBLE);
        }
    }

    private void disableErrorSubmissionViews()
    {
        Button errorSubmission = (Button)findViewById(R.id.errorSubmissionButton);
        if (errorSubmission != null)
        {
            errorSubmission.setVisibility(View.INVISIBLE);
        }
    }

    // Implementation required to receive an image title from the ImageTitleHelper
    public void onImageTitleAcquired(String title)
    {
        TextView imageTitleView = (TextView) findViewById(R.id.imageTitleView);
        if (title != "")
            imageTitleView.setText("\"" + title + "\"");
        else
            imageTitleView.setText("");

        mTitleAcquired = true;

        if (mImageAcquired && mTitleAcquired)
            finaliseCalendarImageAcquired();
    }

    /**
     * Displays a progress dialog on the screen.
     * Should be called whenever an image is loaded.
     */
    private void displayProgressDialog()
    {
        if (mProgressDialog == null)
        {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setTitle(R.string.loading_image_progress_dialog_title);
            mProgressDialog.setCancelable(false);
        }

        mProgressDialog.show();
    }

    /**
     * Hides a progress dialog. Should be called
     * whenever an image is finished loading.
     */
    private void hideProgressDialog()
    {
        mProgressDialog.hide();
    }

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
        /*
        The image couldn't be loaded. The most likely
        cause for the issue is a malformed URL resulting
        in the image not being found. But it might have
        been a HTTP connection timeout.
        */


        /*
        First, find out WHY the image didn't load.

        If the date for the image we are trying
        to load is today's date then we can assume
        the image hasn't been put online yet.

        If the date is for any other day then we
        can assume that we have hit one of the rare
        edge cases where a calendar date had more
        than 1 image posted. In that situation we
        enable the error submission button which
        will deliver all the goods we need via
        email so it can be fixed.

        Note that we don't assume network connection
        dropouts here. They should be handled elsewhere.
        */

        Log.d(mTag, "Image loading failed");
        Log.d(mTag, "Displaying image not found image");

        Calendar today = Calendar.getInstance();

        // Disable the next button if we are at the current date already
        if (mCurrImgDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            mCurrImgDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR))
        {
            /*
            The calendar image for today isn't available. Set
            a dummy image for now. Note that this will rarely
            occur in the main activity, but will likely happen
            often in the widget.
             */
            hideProgressDialog();

            // Display the "todays image not available" dialog
            ImageView imgView = (ImageView)findViewById(R.id.calendarImageView);
            if (imgView != null)
            {
                imgView.setImageResource(R.drawable.dummy_fail_image);
                imgView.setVisibility(View.VISIBLE);
            }
        }
        else
        {
            /*
            Image for the date selected could not be found.
            Hide the progress dialog and enable error
            submission views.
             */

            // !!!! Maybe display the dummy "its not available" image as well
            hideProgressDialog();
            enableErrorSubmissionViews();
        }

    }
    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage)
    {
        Log.d(mTag, "Image was loaded successfully");
        Log.d(mTag, "Applying image to ImageView");
        mImageAcquired = true;

        if (mImageAcquired && mTitleAcquired)
            finaliseCalendarImageAcquired();
    }
    @Override
    public void onLoadingCancelled(String imageUri, View view)
    {

    }
}
