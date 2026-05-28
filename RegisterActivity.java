package com.example.clinicapp.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.clinicapp.R;
import com.example.clinicapp.database.DatabaseHelper;
import com.example.clinicapp.models.User;
import com.example.clinicapp.utils.ModernDialog;
import com.google.android.flexbox.FlexboxLayout;

public class RegisterActivity extends AppCompatActivity {
    private EditText etName, etEmail, etPhone, etPassword, etConfirm, etAge, etAddress;
    private String selectedGender = "";
    private TextView selectedGenderPill;
    private final String[] genderOptions = {"Male", "Female", "Prefer not to say"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        etAge = findViewById(R.id.etAge);
        etAddress = findViewById(R.id.etAddress);

        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        buildGenderPills();

        btnRegister.setOnClickListener(v -> attemptRegister());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void buildGenderPills() {
        FlexboxLayout container = findViewById(R.id.genderContainer);
        float density = getResources().getDisplayMetrics().density;
        for (String gender : genderOptions) {
            TextView pill = new TextView(this);
            pill.setText(gender);
            pill.setBackgroundResource(R.drawable.chip_unselected);
            pill.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            pill.setTextSize(13);
            pill.setTypeface(null, Typeface.BOLD);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding((int)(16*density), (int)(10*density), (int)(16*density), (int)(10*density));

            FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, (int)(8*density), (int)(8*density));
            pill.setLayoutParams(lp);

            pill.setOnClickListener(v -> {
                if (selectedGenderPill != null) {
                    selectedGenderPill.setBackgroundResource(R.drawable.chip_unselected);
                    selectedGenderPill.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                }
                pill.setBackgroundResource(R.drawable.chip_selected);
                pill.setTextColor(ContextCompat.getColor(this, R.color.white));
                selectedGenderPill = pill;
                selectedGender = gender;
            });

            container.addView(pill);
        }
    }

    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            ModernDialog.showWarning(this, "Name Required",
                    "Please enter your full name.");
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ModernDialog.showWarning(this, "Valid Email Required",
                    "Please enter a valid email address.");
            return;
        }
        if (TextUtils.isEmpty(phone) || phone.length() < 7) {
            ModernDialog.showWarning(this, "Valid Phone Required",
                    "Please enter a valid phone number.");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            ModernDialog.showWarning(this, "Password Too Short",
                    "Password must be at least 6 characters long.");
            return;
        }
        if (!password.equals(confirm)) {
            ModernDialog.showWarning(this, "Passwords Don't Match",
                    "Please make sure both password fields are the same.");
            return;
        }
        if (TextUtils.isEmpty(ageStr)) {
            ModernDialog.showWarning(this, "Age Required",
                    "Please enter your age.");
            return;
        }
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            ModernDialog.showWarning(this, "Invalid Age",
                    "Please enter a valid age number.");
            return;
        }
        if (age < 1 || age > 120) {
            ModernDialog.showWarning(this, "Invalid Age",
                    "Please enter an age between 1 and 120.");
            return;
        }
        if (TextUtils.isEmpty(selectedGender)) {
            ModernDialog.showWarning(this, "Gender Required",
                    "Please select your gender.");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            ModernDialog.showWarning(this, "Address Required",
                    "Please enter your address.");
            return;
        }

        DatabaseHelper db = new DatabaseHelper(this);
        if (db.emailExists(email)) {
            ModernDialog.showError(this, "Email Already Used",
                    "This email is already registered.\nPlease use a different email or sign in.");
            return;
        }

        User user = new User(0, name, email, phone, password, "patient",
                age, selectedGender, address);
        long result = db.registerUser(user);

        if (result > 0) {
            new ModernDialog(this)
                    .setType(ModernDialog.Type.SUCCESS)
                    .setTitle("Registration Successful!")
                    .setMessage("Your account has been created.\nYou can now sign in.")
                    .setPositiveButton("Sign In", () -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            ModernDialog.showError(this, "Registration Failed",
                    "Could not create your account.\nPlease try again.");
        }
    }
}
