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

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin, btnAdminLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnAdminLogin = findViewById(R.id.btnAdminLogin);
        tvRegister = findViewById(R.id.tvRegister);
        TextView tvForgot = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> attemptPatientLogin());
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        btnAdminLogin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminLoginActivity.class)));
        tvForgot.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void attemptPatientLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            ModernDialog.showWarning(this, "Email Required",
                    "Please enter your email address.");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            ModernDialog.showWarning(this, "Password Required",
                    "Please enter your password.");
            return;
        }

        DatabaseHelper db = new DatabaseHelper(this);
        User user = db.loginUser(email, password);

        if (user == null) {
            ModernDialog.showError(this, "Login Failed",
                    "Invalid email or password.\nPlease try again.");
            return;
        }

        if ("admin".equals(user.getRole())) {
            ModernDialog.showWarning(this, "Wrong Login",
                    "This is an admin account.\nPlease use the 'Admin Access' button below.");
            return;
        }

        SessionManager session = new SessionManager(this);
        session.createSession(user.getId(), user.getFullName(),
                user.getEmail(), user.getRole());

        Toast.makeText(this, "Welcome, " + user.getFullName(), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, PatientDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
