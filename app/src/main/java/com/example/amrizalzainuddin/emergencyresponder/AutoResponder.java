package com.example.amrizalzainuddin.emergencyresponder;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;

/**
 * Created by amrizal.zainuddin on 29/7/2015.
 */
public class AutoResponder extends Activity {

    Spinner respondForSpinner;
    CheckBox locationCheckBox;
    EditText responseTextBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.autoresponder);

        respondForSpinner = (Spinner)findViewById(R.id.spinnerRespondFor);
        locationCheckBox = (CheckBox)findViewById(R.id.checkboxLocation);
        responseTextBox = (EditText)findViewById(R.id.responseText);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.respondForDisplayItems, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        respondForSpinner.setAdapter(adapter);

        Button okButton = (Button)findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
                setResult(RESULT_OK, null);
                finish();
            }
        });

        Button cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                respondForSpinner.setSelection(-1);
                savePreferences();
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });

        //load the saved preferences and update the UI
        updateUIFromPreferences();
    }

    public static final String autoResponsePref = "autoResponsePref";
    public static final String responseTextPref = "responseTextPref";
    public static final String includeLocPref = "includeLocPref";
    public static final String respondForPref = "respondForPref";
    public static final String defaultResponseText = "defaultResponseText";

    private void updateUIFromPreferences() {
        //get the saves settings
        String preferenceName = getString(R.string.user_preferences);
        SharedPreferences sp = getSharedPreferences(preferenceName, 0);

        boolean autoRespond = sp.getBoolean(autoResponsePref, false);
        String respondText = sp.getString(responseTextPref, defaultResponseText);
        boolean includeLoc = sp.getBoolean(includeLocPref, false);
        int respondForIndex = sp.getInt(respondForPref, 0);

        //applu the saved settings to the UI
        if(autoRespond){
            respondForSpinner.setSelection(respondForIndex);
        }else{
            respondForSpinner.setSelection(0);
        }

        locationCheckBox.setChecked(includeLoc);
        responseTextBox.setText(respondText);
    }

    private void savePreferences() {
        //get the current settings from the UI
        boolean autoRespond = respondForSpinner.getSelectedItemPosition() > 0;
        int respondForIndex = respondForSpinner.getSelectedItemPosition();
        boolean includeLoc = locationCheckBox.isChecked();
        String respondText = responseTextBox.getText().toString();

        //save them to the shared preference file
        String preferenceName = getString(R.string.user_preferences);
        SharedPreferences sp = getSharedPreferences(preferenceName, 0);

        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(autoResponsePref, autoRespond);
        editor.putString(responseTextPref, respondText);
        editor.putBoolean(includeLocPref, includeLoc);
        editor.putInt(respondForPref, respondForIndex);
        editor.commit();

        //set the alarm to turn off the autoresponder
        setAlarm(respondForIndex);
    }

    public static final String alarmAction = "com.example.amrizalzainuddin.AUTO_RESPONSE_EXPIRED";

    private BroadcastReceiver stopAutoResponderReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(alarmAction)){
                String preferenceName = getString(R.string.user_preferences);
                SharedPreferences sp = getSharedPreferences(preferenceName, 0);

                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean(autoResponsePref, false);
                editor.commit();
            }
        }
    };

    PendingIntent intentToFire;

    private void setAlarm(int respondForIndex) {
        //create the alarm and register the alarm intent receiver

        AlarmManager alarms = (AlarmManager)getSystemService(ALARM_SERVICE);

        if(intentToFire == null){
            Intent intent = new Intent(alarmAction);
            intentToFire = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

            IntentFilter filter = new IntentFilter(alarmAction);

            registerReceiver(stopAutoResponderReceiver, filter);
        }

        if(respondForIndex < 1){
            //if 'disabled' is selected, cancel the alarm
            alarms.cancel(intentToFire);
        }else{
            //otherwise find the length of time represented by the selection and set
            //the alarm to tigger after the item has passed
            Resources r = getResources();
            int[] respondForValues = r.getIntArray(R.array.respondForValues);
            int respondFor = respondForValues[respondForIndex];

            long t = System.currentTimeMillis();
            t = t + respondFor*1000*60;

            //set the alarm
            alarms.set(AlarmManager.RTC_WAKEUP, t, intentToFire);
        }
    }
}
