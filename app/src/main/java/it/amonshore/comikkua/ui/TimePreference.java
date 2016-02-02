package it.amonshore.comikkua.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Switch;
import android.widget.TimePicker;

import it.amonshore.comikkua.R;

/**
 * Created by narsenico on 18/01/16.
 *
 */
public class TimePreference extends DialogPreference {

    private TimePicker mTimePicker = null;
    private Switch mDayBeforeSwitch = null;
    private int mValue; //in millisecondi

    private final static int DEFAULT_VALUE = 8 * 3_600_000;

    public TimePreference(Context ctxt) {
        this(ctxt, null);
    }

    public TimePreference(Context ctxt, AttributeSet attrs) {
        this(ctxt, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public TimePreference(Context ctxt, AttributeSet attrs, int defStyle) {
        super(ctxt, attrs, defStyle);

        setDialogLayoutResource(R.layout.dialog_time_preference);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        mDayBeforeSwitch = (Switch)v.findViewById(R.id.dayBeforeSwitch);
        mTimePicker = (TimePicker)v.findViewById(R.id.timePicker);
        mTimePicker.setIs24HourView(true);

        //mValue è espresso in ms, se negativo l'orario è riferito al giorno prima
        final int ms = Math.abs(mValue);
        final int hour = ms / 3_600_000;
        final int minute = (ms - (hour * 3_600_000)) / 60_000;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mTimePicker.setCurrentHour(hour);
            mTimePicker.setCurrentMinute(minute);
        } else {
            mTimePicker.setHour(hour);
            mTimePicker.setMinute(minute);
        }

        mDayBeforeSwitch.setChecked(mValue < 0);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                mValue = (mTimePicker.getCurrentHour() * 60 + mTimePicker.getCurrentMinute()) * 60_000;
            } else {
                mValue = (mTimePicker.getHour() * 60 + mTimePicker.getMinute()) * 60_000;
            }

            if (mDayBeforeSwitch.isChecked()) {
                mValue *= -1;
            }

            setSummary(getSummary());
            if (callChangeListener(mValue)) {
                persistInt(mValue);
                notifyChanged();
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            if (defaultValue == null) {
                mValue = getPersistedInt(DEFAULT_VALUE);
            } else {
                mValue = Integer.parseInt(getPersistedString((String) defaultValue));
            }
        } else {
            if (defaultValue == null) {
                mValue = getPersistedInt(DEFAULT_VALUE);
            } else {
                mValue = Integer.parseInt((String) defaultValue);
            }
        }
        setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {
        final int ms = Math.abs(mValue);
        final int hour = ms / 3_600_000;
        final int minute = (ms - (hour * 3_600_000)) / 60_000;

        return this.getContext().getString((mValue < 0 ?
                        R.string.pref_reminder_time_summary_day_before :
                        R.string.pref_reminder_time_summary),
                hour, minute);
    }
}