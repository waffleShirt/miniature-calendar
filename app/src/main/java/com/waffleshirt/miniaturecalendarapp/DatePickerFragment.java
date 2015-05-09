package com.waffleshirt.miniaturecalendarapp;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Tom on 9/05/2014.
 */
public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener
{
    private MainActivity mCaller;
    private Calendar mDate;

    public DatePickerFragment(MainActivity caller)
    {
        mCaller = caller;
    }

    public DatePickerFragment(MainActivity caller, Calendar date)
    {
        mCaller = caller;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Use the date of the currently displayed image as the current date in the picker
        final Calendar c = mCaller.getCurrentImageDate();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog
        DatePickerDialog dpd = new DatePickerDialog(getActivity(), this, year, month, day);

        // Set the min and max dates (epoch for Miniature Calendar is 20/04/2011)
        dpd.getDatePicker().setMinDate(new GregorianCalendar(2011, 3, 20).getTimeInMillis());
        dpd.getDatePicker().setMaxDate(Calendar.getInstance().getTimeInMillis());
        return dpd;
    }

    public void onDateSet(DatePicker view, int year, int month, int day)
    {
        // Remember to add 1 to month because it is 0 based
        mCaller.displayImageWithDate(year, month, day);
    }
}
