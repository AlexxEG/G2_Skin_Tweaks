package com.gmail.alexellingsen.g2skintweaks.preference;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

@SuppressWarnings("UnusedDeclaration")
public class EditIntPreference extends EditTextPreference {

    public EditIntPreference(Context context) {
        super(context);
    }

    public EditIntPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditIntPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        return String.valueOf(getPersistedInt(-1));
    }

    @Override
    protected boolean persistString(String value) {
        return persistInt(Integer.valueOf(value));
    }

}
