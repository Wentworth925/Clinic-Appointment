package com.example.clinicapp.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clinicapp.R;
import com.example.clinicapp.database.DatabaseHelper;
import com.example.clinicapp.utils.ModernDialog;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText etEmail, etNewPassword, etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        etEmail = findViewById(R.id.etEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        Button btnReset = findViewById(R.id.btnReset);
        TextView tvBack = findViewById(R.id.tvBack);

        btnReset.setOnClickListener(v -> attemptReset());
        tvBack.setOnClickListener(v -> finish());
    }

    private void attemptReset() {
        String email = etEmail.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ModernDialog.showWarning(this, "Valid Email Required",
                    "Please enter the email address you registered with.");
            return;
        }
        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 6) {
            ModernDialog.showWarning(this, "Password Too Short",
                    "Password must be at least 6 characters long.");
            return;
        }
        if (!newPassword.equals(confirm)) {
            ModernDialog.showWarning(this, "Passwords Don't Match",
                    "Please make sure both password fields are the same.");
            return;
        }

        DatabaseHelper db = new DatabaseHelper(this);
        if (!db.emailExists(email)) {
            ModernDialog.showError(this, "Email Not Found",
                    "We couldn't find an account with that email address.\n" +
                            "Please check the email and try again.");
            return;
        }

        if (db.updatePassword(email, newPassword)) {
            new ModernDialog(this)
                    .setType(ModernDialog.Type.SUCCESS)
                    .setTitle("Password Reset!")
                    .setMessage("Your password has been updated.\nYou can now sign in with your new password.")
                    .setPositiveButton("Sign In", () -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            ModernDialog.showError(this, "Reset Failed",
                    "Could not reset your password.\nPlease try again.");
        }
    }
}
