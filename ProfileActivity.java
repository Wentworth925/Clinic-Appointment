package com.example.clinicapp.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.example.clinicapp.utils.SessionManager;
import com.google.android.flexbox.FlexboxLayout;

public class ProfileActivity extends AppCompatActivity {
    private EditText etName, etPhone, etAge, etAddress;
    private TextView tvEmail;
    private SessionManager session;

    private String selectedGender = "";
    private TextView selectedGenderPill;
    private final String[] genderOptions = {"Male", "Female", "Prefer not to say"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etAge = findViewById(R.id.etAge);
        etAddress = findViewById(R.id.etAddress);
        tvEmail = findViewById(R.id.tvEmail);
        Button btnSave = findViewById(R.id.btnSave);

        session = new SessionManager(this);
        DatabaseHelper db = new DatabaseHelper(this);
        User u = db.getUserById(session.getUserId());

        if (u != null) {
            etName.setText(u.getFullName());
            tvEmail.setText(u.getEmail());
            etPhone.setText(u.getPhone());
            if (u.getAge() > 0) etAge.setText(String.valueOf(u.getAge()));
            etAddress.setText(u.getAddress() == null ? "" : u.getAddress());
            selectedGender = u.getGender() == null ? "" : u.getGender();
        } else {
            etName.setText(session.getUserName());
            tvEmail.setText(session.getUserEmail());
        }

        buildGenderPills();

        btnSave.setOnClickListener(v -> save());
    }

    private void buildGenderPills() {
        FlexboxLayout container = findViewById(R.id.genderContainer);
        float density = getResources().getDisplayMetrics().density;
        for (String gender : genderOptions) {
            TextView pill = new TextView(this);
            pill.setText(gender);
            pill.setTextSize(13);
            pill.setTypeface(null, Typeface.BOLD);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding((int)(16*density), (int)(10*density), (int)(16*density), (int)(10*density));

            FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, (int)(8*density), (int)(8*density));
            pill.setLayoutParams(lp);

            if (gender.equals(selectedGender)) {
                pill.setBackgroundResource(R.drawable.chip_selected);
                pill.setTextColor(ContextCompat.getColor(this, R.color.white));
                selectedGenderPill = pill;
            } else {
                pill.setBackgroundResource(R.drawable.chip_unselected);
                pill.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            }

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

    private void save() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            ModernDialog.showWarning(this, "Name Required",
                    "Please enter your full name.");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            ModernDialog.showWarning(this, "Phone Required",
                    "Please enter your phone number.");
            return;
        }
        int age = 0;
        if (!TextUtils.isEmpty(ageStr)) {
            try {
                age = Integer.parseInt(ageStr);
                if (age < 1 || age > 120) {
                    ModernDialog.showWarning(this, "Invalid Age",
                            "Please enter an age between 1 and 120.");
                    return;
                }
            } catch (NumberFormatException e) {
                ModernDialog.showWarning(this, "Invalid Age",
                        "Please enter a valid age number.");
                return;
            }
        }

        DatabaseHelper db = new DatabaseHelper(this);
        User u = new User();
        u.setId(session.getUserId());
        u.setFullName(name);
        u.setPhone(phone);
        u.setAge(age);
        u.setGender(selectedGender);
        u.setAddress(address);

        if (db.updateUser(u)) {
            session.createSession(session.getUserId(), name,
                    session.getUserEmail(), session.getUserRole());
            new ModernDialog(this)
                    .setType(ModernDialog.Type.SUCCESS)
                    .setTitle("Profile Updated!")
                    .setMessage("Your profile information has been saved successfully.")
                    .setPositiveButton("Done", () -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            ModernDialog.showError(this, "Update Failed",
                    "Could not save your profile.\nPlease try again.");
        }
    }
}
