package com.gmail.alexellingsen.g2skintweaks;

import it.gmariotti.android.colorpicker.calendarstock.ColorPickerDialog;
import it.gmariotti.android.colorpicker.calendarstock.ColorPickerSwatch.OnColorSelectedListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {

	public static final String	PREF_NAME						= "G2SkinTweaks";
	public static final String	PREF_ENABLE_MESSENGER_FONT_SIZE	= "enableMessengerFontSize";
	public static final String	PREF_ENABLE_REPLACE_SWICTH		= "enableReplaceSwitch";
	public static final String	PREF_ENABLE_SMS_TEXT_COLOR		= "enableSmsTextColor";
	public static final String	PREF_ENABLE_SQUARE_BUBBLE		= "enableSquareBubble";
	public static final String	PREF_SMS_BODY_SIZE				= "smsBodySize";
	public static final String	PREF_SMS_DATE_SIZE				= "smsDateSize";
	public static final String	PREF_SMS_TEXT_COLOR				= "selectedSmsTextColor";
	public static final String	PREF_SQUARE_COLOR_LEFT			= "selectedSquareColorLeft";
	public static final String	PREF_SQUARE_COLOR_RIGHT			= "selectedSquareColorString";
	public static final String	PREF_TURN_ON_SCREEN_NEW_SMS		= "turnOnScreenOnNewSms";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_settings) {
			return true;
		} else if (id == R.id.action_about) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public static class PlaceholderFragment extends Fragment {

		private CheckBox			chbReplaceSwitch		= null;
		private CheckBox			chbSquareBubble			= null;
		private CheckBox			chbSmsTextColor			= null;
		private CheckBox			chbMessengerFontSize	= null;
		private Button				btnMessengerSetFontSize	= null;

		private SharedPreferences	preferences				= null;

		public PlaceholderFragment() {
		}

		@SuppressLint("WorldReadableFiles")
		@SuppressWarnings("deprecation")
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);

			preferences = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_WORLD_READABLE);

			setupReplaceSwitch(rootView);
			setupMessengerCustomization(rootView);
			setupMessengerFontSize(rootView);
			setupTurnOnScreenNewSMS(rootView);

			return rootView;
		}

		private int[] colorChoice(Context context) {
			int[] mColorChoices = null;
			String[] color_array = context.getResources().getStringArray(R.array.default_color_choice_values);

			if (color_array != null && color_array.length > 0) {
				mColorChoices = new int[color_array.length];
				for (int i = 0; i < color_array.length; i++) {
					mColorChoices[i] = Color.parseColor(color_array[i]);
				}
			}
			return mColorChoices;
		}

		private void setupMessengerFontSize(View rootView) {
			boolean ENABLE_MESSENGER_FONT_SIZE = preferences.getBoolean(PREF_ENABLE_MESSENGER_FONT_SIZE, false);

			this.btnMessengerSetFontSize = (Button) rootView.findViewById(R.id.btn_messenger_set_font_size);

			int body_size = preferences.getInt(PREF_SMS_BODY_SIZE, 18);
			int date_size = preferences.getInt(PREF_SMS_DATE_SIZE, 18);
			updateFontSizeButton(body_size, date_size);

			this.btnMessengerSetFontSize.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showMessengerFontSizePicker();
				}
			});

			this.chbMessengerFontSize = (CheckBox) rootView.findViewById(R.id.chb_messenger_font_size);
			this.chbMessengerFontSize.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SharedPreferences.Editor editor = preferences.edit();

					btnMessengerSetFontSize.setEnabled(isChecked);
					editor.putBoolean(PREF_ENABLE_MESSENGER_FONT_SIZE, isChecked);

					// Apply the edits!
					editor.apply();
				}
			});
			this.chbMessengerFontSize.setChecked(ENABLE_MESSENGER_FONT_SIZE);
		}

		private void setupReplaceSwitch(View rootView) {
			boolean ENABLE_REPLACE_SWITCH = preferences.getBoolean(PREF_ENABLE_REPLACE_SWICTH, false);

			this.chbReplaceSwitch = (CheckBox) rootView.findViewById(R.id.chb_replace_switch);
			this.chbReplaceSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SharedPreferences.Editor editor = preferences.edit();

					editor.putBoolean(PREF_ENABLE_REPLACE_SWICTH, isChecked);

					// Apply the edits!
					editor.apply();
				}
			});
			this.chbReplaceSwitch.setChecked(ENABLE_REPLACE_SWITCH);
		}

		private void setupMessengerCustomization(View rootView) {
			boolean ENABLE_SQUARE_BUBBLE = preferences.getBoolean(PREF_ENABLE_SQUARE_BUBBLE, false);
			boolean ENABLE_SMS_TEXT_COLOR = preferences.getBoolean(PREF_ENABLE_SMS_TEXT_COLOR, false);

			final Button btnSquareLeftColor = (Button) rootView.findViewById(R.id.btn_square_left_color);
			final Button btnSquareRightColor = (Button) rootView.findViewById(R.id.btn_square_right_color);
			final Button btnSmsTextColor = (Button) rootView.findViewById(R.id.btn_sms_text_color);

			btnSquareLeftColor.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showLeftSquareColorPicker(v);
				}
			});
			btnSquareRightColor.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showRightSquareColorPicker(v);
				}
			});

			btnSquareLeftColor.setEnabled(ENABLE_SQUARE_BUBBLE);
			btnSquareRightColor.setEnabled(ENABLE_SQUARE_BUBBLE);
			btnSquareLeftColor.setBackgroundColor(preferences.getInt(PREF_SQUARE_COLOR_LEFT, Color.WHITE));
			btnSquareRightColor.setBackgroundColor(preferences.getInt(PREF_SQUARE_COLOR_RIGHT, Color.WHITE));

			btnSmsTextColor.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showSmsTextColorPicker(v);
				}
			});
			btnSmsTextColor.setBackgroundColor(preferences.getInt(PREF_SMS_TEXT_COLOR, Color.WHITE));

			this.chbSquareBubble = (CheckBox) rootView.findViewById(R.id.chb_square_bubble);
			this.chbSquareBubble.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SharedPreferences.Editor editor = preferences.edit();

					editor.putBoolean(PREF_ENABLE_SQUARE_BUBBLE, isChecked);
					editor.apply();

					btnSquareLeftColor.setEnabled(isChecked);
					btnSquareRightColor.setEnabled(isChecked);
				}
			});
			this.chbSquareBubble.setChecked(ENABLE_SQUARE_BUBBLE);

			this.chbSmsTextColor = (CheckBox) rootView.findViewById(R.id.chb_sms_text_color);
			this.chbSmsTextColor.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SharedPreferences.Editor editor = preferences.edit();

					editor.putBoolean(PREF_ENABLE_SMS_TEXT_COLOR, isChecked);
					editor.apply();

					btnSmsTextColor.setEnabled(isChecked);
				}
			});
			this.chbSmsTextColor.setChecked(ENABLE_SMS_TEXT_COLOR);
		}

		private void setupTurnOnScreenNewSMS(View rootView) {
			boolean ENABLE_TURN_ON_SCREEN_NEW_SMS = preferences.getBoolean(PREF_TURN_ON_SCREEN_NEW_SMS, true);

			final CheckBox chbTurnOnScreenNewSMS = (CheckBox) rootView.findViewById(R.id.chb_turn_on_screen);

			chbTurnOnScreenNewSMS.setChecked(ENABLE_TURN_ON_SCREEN_NEW_SMS);
			chbTurnOnScreenNewSMS.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SharedPreferences.Editor editor = preferences.edit();

					editor.putBoolean(PREF_TURN_ON_SCREEN_NEW_SMS, isChecked);

					editor.apply();
				}
			});
		}

		private void showSmsTextColorPicker(final View v) {
			int[] mColor = colorChoice(getActivity());
			int mSelectedColor = preferences.getInt(PREF_SMS_TEXT_COLOR, Color.BLACK);

			ColorPickerDialog colorCalendar = ColorPickerDialog.newInstance(
					R.string.color_picker_default_title,
					mColor,
					mSelectedColor,
					4,
					ColorPickerDialog.SIZE_SMALL);

			colorCalendar.setOnColorSelectedListener(new OnColorSelectedListener() {
				@Override
				public void onColorSelected(int color) {
					SharedPreferences.Editor editor = preferences.edit();

					editor.putInt(PREF_SMS_TEXT_COLOR, color);
					editor.apply();

					v.setBackgroundColor(color);
				}
			});

			colorCalendar.show(getFragmentManager(), "cal");
		}

		private void showLeftSquareColorPicker(final View v) {
			int[] mColor = colorChoice(getActivity());
			int mSelectedColor = preferences.getInt(PREF_SQUARE_COLOR_LEFT, Color.WHITE);

			ColorPickerDialog colorCalendar = ColorPickerDialog.newInstance(
					R.string.color_picker_default_title,
					mColor,
					mSelectedColor,
					4,
					ColorPickerDialog.SIZE_SMALL);

			colorCalendar.setOnColorSelectedListener(new OnColorSelectedListener() {
				@Override
				public void onColorSelected(int color) {
					SharedPreferences.Editor editor = preferences.edit();

					editor.putInt(PREF_SQUARE_COLOR_LEFT, color);

					editor.apply();

					v.setBackgroundColor(color);
				}
			});

			colorCalendar.show(getFragmentManager(), "cal");
		}

		private void showRightSquareColorPicker(final View v) {
			int[] mColor = colorChoice(getActivity());
			int mSelectedColor = preferences.getInt(PREF_SQUARE_COLOR_RIGHT, Color.WHITE);

			ColorPickerDialog colorCalendar = ColorPickerDialog.newInstance(
					R.string.color_picker_default_title,
					mColor,
					mSelectedColor,
					4,
					ColorPickerDialog.SIZE_SMALL);

			colorCalendar.setOnColorSelectedListener(new OnColorSelectedListener() {
				@Override
				public void onColorSelected(int color) {
					SharedPreferences.Editor editor = preferences.edit();

					editor.putInt(PREF_SQUARE_COLOR_RIGHT, color);

					editor.apply();

					v.setBackgroundColor(color);
				}
			});

			colorCalendar.show(getFragmentManager(), "cal");
		}

		private void showMessengerFontSizePicker() {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			LayoutInflater inflater = LayoutInflater.from(getActivity());

			final View inflator = inflater.inflate(R.layout.text_size_picker, null);

			final TextView txt_sms_body_size = (TextView) inflator.findViewById(R.id.txt_sms_body_size);
			final TextView txt_sms_date_size = (TextView) inflator.findViewById(R.id.txt_sms_date_size);
			final SeekBar sms_body_size = (SeekBar) inflator.findViewById(R.id.sms_body_size);
			final SeekBar sms_date_size = (SeekBar) inflator.findViewById(R.id.sms_date_size);

			txt_sms_body_size.setText(preferences.getInt(PREF_SMS_BODY_SIZE, 18) + "");
			txt_sms_date_size.setText(preferences.getInt(PREF_SMS_DATE_SIZE, 18) + "");
			sms_body_size.setProgress(preferences.getInt(PREF_SMS_BODY_SIZE, 18) - 12);
			sms_date_size.setProgress(preferences.getInt(PREF_SMS_DATE_SIZE, 18) - 12);

			sms_body_size.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					int sp = progress + 12;

					txt_sms_body_size.setText(sp + "");
				}
			});

			sms_date_size.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					int sp = progress + 12;

					txt_sms_date_size.setText(sp + "");
				}
			});

			builder.setView(inflator)
					.setPositiveButton("Save", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							int spBody = sms_body_size.getProgress() + 12;
							int spDate = sms_date_size.getProgress() + 12;

							SharedPreferences.Editor editor = preferences.edit();

							editor.putInt(PREF_SMS_BODY_SIZE, spBody);
							editor.putInt(PREF_SMS_DATE_SIZE, spDate);

							updateFontSizeButton(spBody, spDate);

							// Apply the edits!
							editor.apply();
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});

			AlertDialog alertDialog = builder.create();

			alertDialog.show();
		}

		private void updateFontSizeButton(int body, int date) {
			String text = "Set font size<br/><small>(Body: " + body + "sp, Date: " + date + "sp)</small>";

			this.btnMessengerSetFontSize.setText(Html.fromHtml(text));
		}
	}

}
