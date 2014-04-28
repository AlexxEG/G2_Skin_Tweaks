package com.gmail.alexellingsen.g2skintweaks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import it.gmariotti.android.colorpicker.calendarstock.ColorPickerDialog;
import it.gmariotti.android.colorpicker.calendarstock.ColorPickerSwatch.OnColorSelectedListener;

public class MainActivity extends Activity {

    private static final int PREFERENCE_ACTIVITY = 100;

    private PlaceholderFragment fragment = null;
    private static SettingsHelper settings = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = new SettingsHelper(this);

        if (savedInstanceState == null) {
            fragment = new PlaceholderFragment();

            getFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean debugging = settings.getBoolean(Prefs.ENABLE_DEBUGGING, false);

        menu.findItem(R.id.action_enable_debugging).setChecked(debugging);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_reset_default) {
            fragment.askResetToDefault();
            return true;
        } else if (id == R.id.action_enable_debugging) {
            item.setChecked(!item.isChecked());
            settings.putBoolean(Prefs.ENABLE_DEBUGGING, item.isChecked());
            return true;
        } else if (id == R.id.action_settings) {
            Intent i = new Intent(getApplicationContext(), PrefsActivity.class);
            startActivityForResult(i, PREFERENCE_ACTIVITY);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PREFERENCE_ACTIVITY) {
            // Update text to show new preferences, if any.
            fragment.updateAfterPreferences();
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static class PlaceholderFragment extends Fragment {

        private View rootView = null;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_main, container, false);

            setup();

            return rootView;
        }

        private void askResetToDefault() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle(getString(R.string.are_you_sure));
            builder.setMessage(getString(R.string.confirm_reset_message));
            builder.setPositiveButton(getString(R.string.yes), new AlertDialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    resetToDefault();
                }
            });
            builder.setNegativeButton(getString(R.string.no), new AlertDialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing
                }
            });
            builder.show();
        }

        private int[] getColorChoice() {
            int[] mColorChoices = null;
            String[] color_array = getResources().getStringArray(R.array.default_color_choice_values);

            if (color_array != null && color_array.length > 0) {
                mColorChoices = new int[color_array.length];
                for (int i = 0; i < color_array.length; i++) {
                    mColorChoices[i] = Color.parseColor(color_array[i]);
                }
            }
            return mColorChoices;
        }

        public void resetToDefault() {
            rootView.findViewById(R.id.btn_square_left_color).setBackgroundColor(Color.WHITE);
            rootView.findViewById(R.id.btn_square_right_color).setBackgroundColor(Color.WHITE);
            rootView.findViewById(R.id.btn_sms_text_color_left).setBackgroundColor(Color.BLACK);
            rootView.findViewById(R.id.btn_sms_text_color_right).setBackgroundColor(Color.BLACK);
            ((CheckBox) rootView.findViewById(R.id.chb_replace_switch)).setChecked(false);
            ((CheckBox) rootView.findViewById(R.id.chb_square_bubble)).setChecked(false);
            ((CheckBox) rootView.findViewById(R.id.chb_sms_text_color)).setChecked(false);
            ((CheckBox) rootView.findViewById(R.id.chb_smaller_sms_size)).setChecked(false);
            ((CheckBox) rootView.findViewById(R.id.chb_turn_on_screen)).setChecked(true);

            // Listeners will update most preferences
            settings.putInt(Prefs.SQUARE_COLOR_LEFT, Color.WHITE);
            settings.putInt(Prefs.SQUARE_COLOR_RIGHT, Color.WHITE);
            settings.putInt(Prefs.SMS_TEXT_COLOR_LEFT, Color.BLACK);
            settings.putInt(Prefs.SMS_TEXT_COLOR_RIGHT, Color.BLACK);

            String text = getString(R.string.reboot_notice);
            Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
        }

        private void setup() {
            setupReplaceSwitch();
            setupSmsTextColor();
            setupMessengerFontSize();
            setupConversationColor();
            setupTurnOnScreenNewSMS();
            setupSquareBubble();
        }

        private void setupConversationColor() {
            boolean b = settings.getBoolean(Prefs.ENABLE_CONVERSATION_COLOR, false);

            final Button top = (Button) rootView.findViewById(R.id.btn_conversation_color_top);
            final Button bottom = (Button) rootView.findViewById(R.id.btn_conversation_color_bottom);
            final CheckBox chbConversationColor = (CheckBox) rootView.findViewById(R.id.chb_conversation_color);

            chbConversationColor.setChecked(b);
            chbConversationColor.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_CONVERSATION_COLOR, isChecked);

                    top.setEnabled(isChecked);
                    bottom.setEnabled(isChecked);
                }
            });

            bottom.setEnabled(b);
            bottom.setBackgroundColor(settings.getInt(Prefs.CONVERSATION_COLOR_BOTTOM, Color.BLACK));
            bottom.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(v, Prefs.CONVERSATION_COLOR_BOTTOM, Color.BLACK);
                }
            });

            top.setEnabled(b);
            top.setBackgroundColor(settings.getInt(Prefs.CONVERSATION_COLOR_TOP, Color.BLACK));
            top.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(v, Prefs.CONVERSATION_COLOR_TOP, Color.BLACK);
                }
            });
        }

        private void setupMessengerFontSize() {
            int minimumZoom = settings.getInt(Prefs.MINIMUM_ZOOM_LEVEL, 30);
            boolean enableSmallerSmsSize = settings.getBoolean(Prefs.ENABLE_SMALLER_SMS_SIZE, false);

            CheckBox chbMessengerFontSize = (CheckBox) rootView.findViewById(R.id.chb_smaller_sms_size);

            chbMessengerFontSize.setText(getString(R.string.enable_sms_smaller_size, minimumZoom));
            chbMessengerFontSize.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_SMALLER_SMS_SIZE, isChecked);
                }
            });
            chbMessengerFontSize.setChecked(enableSmallerSmsSize);
        }

        private void setupReplaceSwitch() {
            boolean ENABLE_REPLACE_SWITCH = settings.getBoolean(Prefs.ENABLE_REPLACE_SWITCH, false);

            CheckBox chbReplaceSwitch = (CheckBox) rootView.findViewById(R.id.chb_replace_switch);
            chbReplaceSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_REPLACE_SWITCH, isChecked);
                }
            });
            chbReplaceSwitch.setChecked(ENABLE_REPLACE_SWITCH);
        }

        private void setupSquareBubble() {
            boolean enableSquareBubble = settings.getBoolean(Prefs.ENABLE_SQUARE_BUBBLE, false);

            final Button btnSquareLeftColor = (Button) rootView.findViewById(R.id.btn_square_left_color);
            final Button btnSquareRightColor = (Button) rootView.findViewById(R.id.btn_square_right_color);

            CheckBox chbSquareBubble = (CheckBox) rootView.findViewById(R.id.chb_square_bubble);
            chbSquareBubble.setChecked(enableSquareBubble);
            chbSquareBubble.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_SQUARE_BUBBLE, isChecked);

                    btnSquareLeftColor.setEnabled(isChecked);
                    btnSquareRightColor.setEnabled(isChecked);
                }
            });

            btnSquareLeftColor.setEnabled(enableSquareBubble);
            btnSquareLeftColor.setBackgroundColor(settings.getInt(Prefs.SQUARE_COLOR_LEFT, Color.WHITE));
            btnSquareLeftColor.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(v, Prefs.SQUARE_COLOR_LEFT, Color.WHITE);
                }
            });

            btnSquareRightColor.setEnabled(enableSquareBubble);
            btnSquareRightColor.setBackgroundColor(settings.getInt(Prefs.SQUARE_COLOR_RIGHT, Color.WHITE));
            btnSquareRightColor.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(v, Prefs.SQUARE_COLOR_RIGHT, Color.WHITE);
                }
            });
        }

        private void setupSmsTextColor() {
            boolean enableSmsTextColor = settings.getBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, false);

            final Button btnSmsTextColorLeft = (Button) rootView.findViewById(R.id.btn_sms_text_color_left);
            final Button btnSmsTextColorRight = (Button) rootView.findViewById(R.id.btn_sms_text_color_right);

            CheckBox chbSmsTextColor = (CheckBox) rootView.findViewById(R.id.chb_sms_text_color);
            chbSmsTextColor.setChecked(enableSmsTextColor);
            chbSmsTextColor.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, isChecked);

                    btnSmsTextColorLeft.setEnabled(isChecked);
                    btnSmsTextColorRight.setEnabled(isChecked);
                }
            });

            btnSmsTextColorLeft.setEnabled(enableSmsTextColor);
            btnSmsTextColorLeft.setBackgroundColor(settings.getInt(Prefs.SMS_TEXT_COLOR_LEFT, Color.BLACK));
            btnSmsTextColorLeft.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(v, Prefs.SMS_TEXT_COLOR_LEFT, Color.BLACK);
                }
            });

            btnSmsTextColorRight.setEnabled(enableSmsTextColor);
            btnSmsTextColorRight.setBackgroundColor(settings.getInt(Prefs.SMS_TEXT_COLOR_RIGHT, Color.BLACK));
            btnSmsTextColorRight.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(v, Prefs.SMS_TEXT_COLOR_RIGHT, Color.BLACK);
                }
            });
        }

        private void setupTurnOnScreenNewSMS() {
            boolean enableTurnOnScreenNewSms = settings.getBoolean(Prefs.TURN_ON_SCREEN_NEW_SMS, true);
            boolean enablePowerLed = settings.getBoolean(Prefs.ENABLE_POWER_LED, true);

            final CheckBox chbTurnOnScreenNewSMS = (CheckBox) rootView.findViewById(R.id.chb_turn_on_screen);
            final CheckBox chbEnablePowerLed = (CheckBox) rootView.findViewById(R.id.chb_enable_power_led);
            final Button btnRequestRoot = (Button) rootView.findViewById(R.id.btn_request_root);

            chbTurnOnScreenNewSMS.setChecked(enableTurnOnScreenNewSms);
            chbTurnOnScreenNewSMS.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.TURN_ON_SCREEN_NEW_SMS, isChecked);

                    chbEnablePowerLed.setEnabled(!isChecked);
                    btnRequestRoot.setEnabled(!isChecked);
                }
            });

            chbEnablePowerLed.setChecked(enablePowerLed);
            chbEnablePowerLed.setEnabled(!enableTurnOnScreenNewSms);
            chbEnablePowerLed.setText(Html.fromHtml(getString(R.string.chb_flash_power_led)));
            chbEnablePowerLed.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_POWER_LED, isChecked);
                }
            });

            btnRequestRoot.setEnabled(!enableTurnOnScreenNewSms);
            btnRequestRoot.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    RootFunctions.requestRoot();
                }
            });
        }

        private void showColorPicker(final View v, final String key, int defaultColor) {
            int[] mColor = getColorChoice();
            int mSelectedColor = settings.getInt(key, defaultColor);

            ColorPickerDialog colorPicker = ColorPickerDialog.newInstance(
                    R.string.color_picker_default_title,
                    mColor,
                    mSelectedColor,
                    4,
                    ColorPickerDialog.SIZE_SMALL);

            colorPicker.setOnColorSelectedListener(new OnColorSelectedListener() {
                @Override
                public void onColorSelected(int color) {
                    settings.putInt(key, color);

                    v.setBackgroundColor(color);
                }
            });

            colorPicker.show(getFragmentManager(), "cal");
        }

        public void updateAfterPreferences() {
            CheckBox chbMessengerFontSize = (CheckBox) rootView.findViewById(R.id.chb_smaller_sms_size);

            int minimumZoom = settings.getInt(Prefs.MINIMUM_ZOOM_LEVEL, 30);

            chbMessengerFontSize.setText(getString(R.string.enable_sms_smaller_size, minimumZoom));
        }

    }

}
