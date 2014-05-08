package com.gmail.alexellingsen.g2skintweaks.preference;

import android.content.Context;
import android.graphics.Color;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import com.gmail.alexellingsen.g2skintweaks.R;

@SuppressWarnings("UnusedDeclaration")
public class PreviewColorPreference extends Preference {

    private int mColor = Color.BLACK;
    private ImageView mImageView;

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
        mImageView = (ImageView) view.findViewById(R.id.image_view_preference);
        mImageView.setBackgroundColor(mColor);
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        mColor = color;
        notifyChanged();
    }

}
