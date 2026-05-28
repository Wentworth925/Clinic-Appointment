package com.example.clinicapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clinicapp.R;
import com.example.clinicapp.adapters.DoctorAdapter;
import com.example.clinicapp.database.DatabaseHelper;
import com.example.clinicapp.models.Doctor;

import java.util.List;

public class DoctorListActivity extends AppCompatActivity {

    private DoctorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        DatabaseHelper db = new DatabaseHelper(this);
        List<Doctor> doctors = db.getAllDoctors();

        RecyclerView rv = findViewById(R.id.rvDoctors);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoctorAdapter(doctors, this::openProfile);
        rv.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void openProfile(Doctor doctor) {
        Intent intent = new Intent(this, DoctorProfileActivity.class);
        intent.putExtra("doctor_id", doctor.getId());
        intent.putExtra("doctor_name", doctor.getName());
        intent.putExtra("doctor_spec", doctor.getSpecialization());
        intent.putExtra("doctor_days", doctor.getAvailableDays());
        startActivity(intent);
    }
}
