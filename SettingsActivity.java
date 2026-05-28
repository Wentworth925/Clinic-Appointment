package com.example.clinicapp.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.clinicapp.R;
import com.example.clinicapp.utils.SessionManager;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private SessionManager session;
    private TextView selectedTimePill;
    private final int[] reminderOptions = {15, 30, 60, 120, 1440};
    private final String[] reminderLabels = {"15 min", "30 min", "1 hour", "2 hours", "1 day"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        session = new SessionManager(this);

        // Account info
        ((TextView) findViewById(R.id.tvAccountName)).setText(session.getUserName());
        ((TextView) findViewById(R.id.tvAccountEmail)).setText(session.getUserEmail());

        // Notifications toggle
        SwitchMaterial switchNotif = findViewById(R.id.switchNotif);
        switchNotif.setChecked(session.isNotificationsEnabled());
        switchNotif.setOnCheckedChangeListener((b, isChecked) -> {
            session.setNotificationsEnabled(isChecked);
            updateReminderPillsEnabled(isChecked);
        });

        buildReminderTimePills();
        updateReminderPillsEnabled(session.isNotificationsEnabled());

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void buildReminderTimePills() {
        FlexboxLayout container = findViewById(R.id.reminderTimesContainer);
        int currentMinutes = session.getReminderMinutesBefore();
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < reminderOptions.length; i++) {
            final int minutes = reminderOptions[i];
            TextView pill = new TextView(this);
            pill.setText(reminderLabels[i]);
            pill.setTextSize(13);
            pill.setTypeface(null, Typeface.BOLD);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding((int)(18*density), (int)(10*density), (int)(18*density), (int)(10*density));

            FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, (int)(8*density), (int)(8*density));
            pill.setLayoutParams(lp);

            if (minutes == currentMinutes) {
                applySelected(pill);
                selectedTimePill = pill;
            } else {
                applyUnselected(pill);
            }

            pill.setOnClickListener(v -> {
                if (selectedTimePill != null) applyUnselected(selectedTimePill);
                applySelected(pill);
                selectedTimePill = pill;
                session.setReminderMinutesBefore(minutes);
            });

            container.addView(pill);
        }
    }

    private void applySelected(TextView pill) {
        pill.setBackgroundResource(R.drawable.chip_selected);
        pill.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private void applyUnselected(TextView pill) {
        pill.setBackgroundResource(R.drawable.chip_unselected);
        pill.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
    }

    private void updateReminderPillsEnabled(boolean enabled) {
        FlexboxLayout container = findViewById(R.id.reminderTimesContainer);
        container.setAlpha(enabled ? 1.0f : 0.4f);
        for (int i = 0; i < container.getChildCount(); i++) {
            container.getChildAt(i).setEnabled(enabled);
        }
    }
}
