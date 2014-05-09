package com.gmail.alexellingsen.g2skintweaks.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import com.gmail.alexellingsen.g2skintweaks.R;

@SuppressWarnings("UnusedDeclaration")
public class PreviewColorPreference extends Preference {

    private int mColor = Color.BLACK;

    public PreviewColorPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    public PreviewColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public PreviewColorPreference(Context context) {
        super(context);

        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.preference_color_preview);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        ImageView mImageView = (ImageView) view.findViewById(R.id.image_view_preference);

        mImageView.setBackgroundColor(mColor);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, Color.BLACK);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setColor(restoreValue ? getPersistedInt(mColor)
                : (Integer) defaultValue);
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        if (color != mColor) {
            mColor = color;
            persistInt(color);
            notifyChanged();
        }
    }

}
