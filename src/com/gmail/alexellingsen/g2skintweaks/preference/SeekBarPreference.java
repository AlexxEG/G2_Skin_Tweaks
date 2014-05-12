package com.gmail.alexellingsen.g2skintweaks.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import com.gmail.alexellingsen.g2skintweaks.R;

public class SeekBarPreference extends Preference {

    private Context mContext;
    private int mMax;
    private int mMin;
    private String mSummaryFormat;
    private int mValue;

    public SeekBarPreference(Context context) {
        this(context, null);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext = context;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, defStyle, 0);
        setMax(a.getInt(R.styleable.SeekBarPreference_max, 300));
        setMin(a.getInt(R.styleable.SeekBarPreference_min, 0));
        setSummaryFormat(a.getString(R.styleable.SeekBarPreference_summaryFormat));
        setValue(a.getInt(R.styleable.SeekBarPreference_value, getMin()));
        a.recycle();
    }

    @Override
    protected void onClick() {
        super.onClick();

        showSeekBarDialog();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        syncSummary(view);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mValue)
                : (Integer) defaultValue);
    }

    private void syncSummary(View view) {
        TextView summaryText = (TextView) view.findViewById(android.R.id.summary);

        if (summaryText != null) {
            if (TextUtils.isEmpty(getSummaryFormat())) {
                return;
            }

            summaryText.setText(String.format(getSummaryFormat(), getValue()));
        }
    }

    public int getMax() {
        return mMax;
    }

    public int getMin() {
        return mMin;
    }

    public String getSummaryFormat() {
        return mSummaryFormat;
    }

    public int getValue() {
        return mValue;
    }

    public void setMax(int max) {
        mMax = max;
    }

    public void setMin(int min) {
        mMin = min;
    }

    public void setSummaryFormat(String summaryFormat) {
        mSummaryFormat = summaryFormat;
        notifyChanged();
    }

    public void setValue(int value) {
        if (value != mValue) {
            mValue = value <= getMax() ? value : getMax();
            persistInt(value);
            notifyChanged();
        }
    }

    private void showSeekBarDialog() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.seek_bar_dialog, null);
        TextView tvTitle = (TextView) layout.findViewById(R.id.seek_bar_dialog_title);
        final TextView tv = (TextView) layout.findViewById(R.id.seek_bar_dialog_text_view);
        final SeekBar sb = (SeekBar) layout.findViewById(R.id.seek_bar_dialog_seek_bar);

        tvTitle.setText(getTitle());
        tv.setText(getValue() + "");

        sb.setMax(getMax() - getMin());
        sb.setProgress(getValue() - getMin());
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + getMin();
                value = value <= getMax() ? value : getMax();

                tv.setText(value + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setView(layout)
                .setPositiveButton(mContext.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int value = sb.getProgress() + getMin();
                        value = value <= getMax() ? value : getMax();

                        setValue(value);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(mContext.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
