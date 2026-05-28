package com.example.clinicapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.example.clinicapp.R;
import com.example.clinicapp.database.DatabaseHelper;
import com.example.clinicapp.models.Appointment;
import com.example.clinicapp.utils.ModernDialog;
import com.example.clinicapp.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PatientDashboardActivity extends AppCompatActivity {
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) { goToLogin(); return; }

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Time-aware greeting
        TextView tvGreetingLabel = findViewById(R.id.tvGreetingLabel);
        tvGreetingLabel.setText(getTimeGreeting());

        // Avatar initial
        String name = session.getUserName();
        TextView tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvAvatarInitial.setText(getInitial(name));

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Hello, " + getFirstName(name) + "!");

        // Wire up action cards
        CardView cardBook = findViewById(R.id.cardBook);
        CardView cardMyAppts = findViewById(R.id.cardMyAppts);
        CardView cardProfile = findViewById(R.id.cardProfile);
        CardView cardSettings = findViewById(R.id.cardSettings);
        CardView cardAbout = findViewById(R.id.cardAbout);
        CardView cardLogout = findViewById(R.id.cardLogout);
        CardView cardNextAppt = findViewById(R.id.cardNextAppt);
        CardView cardHistory = findViewById(R.id.cardHistory);
        View avatarBtn = findViewById(R.id.avatarBtn);

        cardBook.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorListActivity.class)));
        cardMyAppts.setOnClickListener(v ->
                startActivity(new Intent(this, MyAppointmentsActivity.class)));
        cardProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        cardSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        cardAbout.setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));
        cardLogout.setOnClickListener(v -> confirmLogout());
        cardNextAppt.setOnClickListener(v ->
                startActivity(new Intent(this, MyAppointmentsActivity.class)));
        cardHistory.setOnClickListener(v -> {
            Intent i = new Intent(this, MyAppointmentsActivity.class);
            i.putExtra("default_filter", "Completed");
            i.putExtra("header_label", "HISTORY");
            i.putExtra("header_title", "Appointment History");
            startActivity(i);
        });
        avatarBtn.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        requestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        DatabaseHelper db = new DatabaseHelper(this);
        int patientId = session.getUserId();

        int upcoming = db.countUpcomingForPatient(patientId);
        int completed = db.countAppointmentsByStatus(patientId, Appointment.STATUS_COMPLETED);
        int total = db.getAppointmentsForPatient(patientId).size();

        ((TextView) findViewById(R.id.tvStatUpcoming)).setText(String.valueOf(upcoming));
        ((TextView) findViewById(R.id.tvStatCompleted)).setText(String.valueOf(completed));
        ((TextView) findViewById(R.id.tvStatTotal)).setText(String.valueOf(total));

        // Show next appointment if exists
        Appointment next = db.getNextAppointmentForPatient(patientId);
        CardView cardNext = findViewById(R.id.cardNextAppt);
        if (next != null) {
            cardNext.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvNextDoctor)).setText(next.getDoctorName());
            ((TextView) findViewById(R.id.tvNextSpec)).setText(next.getSpecialization());
            ((TextView) findViewById(R.id.tvNextDateTime)).setText(
                    formatDateTime(next.getDate(), next.getTime()));
        } else {
            cardNext.setVisibility(View.GONE);
        }
    }

    private String formatDateTime(String date, String time) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault());
            return out.format(in.parse(date + " " + time));
        } catch (Exception e) {
            return date + " " + time;
        }
    }

    private String getTimeGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "GOOD MORNING";
        if (hour < 17) return "GOOD AFTERNOON";
        return "GOOD EVENING";
    }

    private String getFirstName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "there";
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    private String getInitial(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "?";
        return String.valueOf(fullName.trim().charAt(0)).toUpperCase();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void confirmLogout() {
        ModernDialog.showConfirm(this,
                "Sign Out",
                "Are you sure you want to sign out of your account?",
                "Sign Out",
                () -> {
                    session.logout();
                    goToLogin();
                });
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
