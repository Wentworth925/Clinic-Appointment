package com.example.clinicapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.clinicapp.R;
import com.example.clinicapp.database.DatabaseHelper;
import com.example.clinicapp.utils.ModernDialog;
import com.example.clinicapp.utils.SessionManager;

public class AdminDashboardActivity extends AppCompatActivity {
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) { goToLogin(); return; }

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome, " + session.getUserName());

        CardView cardAppts = findViewById(R.id.cardAppts);
        CardView cardPatients = findViewById(R.id.cardPatients);
        CardView cardAbout = findViewById(R.id.cardAbout);
        CardView cardLogout = findViewById(R.id.cardLogout);

        cardAppts.setOnClickListener(v ->
                startActivity(new Intent(this, ManageAppointmentsActivity.class)));
        cardPatients.setOnClickListener(v ->
                startActivity(new Intent(this, ManagePatientsActivity.class)));
        cardAbout.setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));
        cardLogout.setOnClickListener(v -> confirmLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseHelper db = new DatabaseHelper(this);
        TextView tvStatAppts = findViewById(R.id.tvStatAppts);
        TextView tvStatPatients = findViewById(R.id.tvStatPatients);
        tvStatAppts.setText(String.valueOf(db.getAllAppointments().size()));
        tvStatPatients.setText(String.valueOf(db.getAllPatients().size()));
    }

    private void confirmLogout() {
        ModernDialog.showConfirm(this,
                "Sign Out",
                "Are you sure you want to sign out of the admin panel?",
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
