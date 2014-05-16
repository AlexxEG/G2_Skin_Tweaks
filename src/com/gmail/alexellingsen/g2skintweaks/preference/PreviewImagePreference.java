package com.gmail.alexellingsen.g2skintweaks.preference;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import com.gmail.alexellingsen.g2skintweaks.R;

/**
 * A Preference class which can show a preview of a image,
 * and open the image in a dialog when clicked.
 */
public class PreviewImagePreference extends Preference {

    private Drawable mDrawable;

    public PreviewImagePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setWidgetLayoutResource(R.layout.preference_color_preview);
    }

    public PreviewImagePreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public PreviewImagePreference(Context context) {
        this(context, null);
    }

    private ImageView mImageView;

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mImageView = (ImageView) view.findViewById(R.id.image_view_preference);
        mImageView.setImageDrawable(mDrawable);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEnlargedPreview();
            }
        });
    }

    private void showEnlargedPreview() {
        ImageView imageView = new ImageView(getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // This makes the borders go away
        imageView.setBackground(mDrawable);

        final Dialog dialog = new Dialog(getContext());

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(imageView);
        dialog.getWindow().setBackgroundDrawable(null);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public Drawable getDrawable() {
        return mDrawable;
    }

    public void setDrawable(Drawable drawable) {
        mDrawable = drawable;
        if (mImageView != null) {
            mImageView.setImageDrawable(mDrawable);
        }
    }

}
