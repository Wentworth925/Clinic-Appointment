package com.example.clinicapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clinicapp.R;
import com.example.clinicapp.database.DatabaseHelper;
import com.example.clinicapp.models.User;
import com.example.clinicapp.utils.ModernDialog;
import com.example.clinicapp.utils.SessionManager;

public class AdminLoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnAdminLogin;
    private TextView tvBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        etEmail = findViewById(R.id.etAdminEmail);
        etPassword = findViewById(R.id.etAdminPassword);
        btnAdminLogin = findViewById(R.id.btnAdminLogin);
        tvBack = findViewById(R.id.tvBack);

        btnAdminLogin.setOnClickListener(v -> attemptAdminLogin());
        tvBack.setOnClickListener(v -> finish());
    }

    private void attemptAdminLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            ModernDialog.showWarning(this, "Email Required",
                    "Please enter the admin email address.");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            ModernDialog.showWarning(this, "Password Required",
                    "Please enter the admin password.");
            return;
        }

        DatabaseHelper db = new DatabaseHelper(this);
        User user = db.loginUser(email, password);

        if (user == null) {
            ModernDialog.showError(this, "Access Denied",
                    "Invalid admin credentials.\nPlease try again.");
            return;
        }

        if (!"admin".equals(user.getRole())) {
            ModernDialog.showWarning(this, "Not an Admin",
                    "This account is not an admin.\nPlease use the regular Patient Login.");
            return;
        }

        SessionManager session = new SessionManager(this);
        session.createSession(user.getId(), user.getFullName(),
                user.getEmail(), user.getRole());

        Toast.makeText(this, "Welcome, " + user.getFullName(), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, AdminDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
