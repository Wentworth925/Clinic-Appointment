package com.example.clinicapp.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clinicapp.R;
import com.example.clinicapp.adapters.AppointmentAdapter;
import com.example.clinicapp.database.DatabaseHelper;
import com.example.clinicapp.models.Appointment;
import com.example.clinicapp.utils.ModernDialog;
import com.example.clinicapp.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageAppointmentsActivity extends AppCompatActivity {
    private RecyclerView rv;
    private TextView tvEmpty;
    private View emptyContainer;
    private List<Appointment> allAppointments = new ArrayList<>();
    private String currentFilter = "All";
    private String currentSearch = "";
    private TextView selectedChip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        ((TextView) findViewById(R.id.tvHeaderLabel)).setText("ADMIN PANEL");
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText("Manage Appointments");

        findViewById(R.id.searchBarContainer).setVisibility(View.VISIBLE);
        findViewById(R.id.filterScroll).setVisibility(View.VISIBLE);

        rv = findViewById(R.id.rv);
        tvEmpty = findViewById(R.id.tvEmpty);
        emptyContainer = findViewById(R.id.emptyContainer);
        rv.setLayoutManager(new LinearLayoutManager(this));

        buildFilterChips();
        wireSearch();
    }

    private void buildFilterChips() {
        LinearLayout container = findViewById(R.id.filterChipsContainer);
        String[] filters = {"All", "Pending", "Confirmed", "Completed", "Cancelled"};
        for (String f : filters) {
            TextView chip = createChip(f);
            if ("All".equals(f)) {
                applySelectedStyle(chip);
                selectedChip = chip;
            }
            container.addView(chip);
        }
    }

    private TextView createChip(String label) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setBackgroundResource(R.drawable.chip_unselected);
        chip.setTextColor(getResources().getColor(R.color.text_primary));
        chip.setTextSize(13);
        chip.setTypeface(null, android.graphics.Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(16), dp(8), dp(16), dp(8));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        chip.setLayoutParams(lp);

        chip.setOnClickListener(v -> {
            if (selectedChip != null) applyUnselectedStyle(selectedChip);
            applySelectedStyle(chip);
            selectedChip = chip;
            currentFilter = label;
            renderList();
        });

        return chip;
    }

    private void applySelectedStyle(TextView chip) {
        chip.setBackgroundResource(R.drawable.chip_selected);
        chip.setTextColor(getResources().getColor(R.color.white));
    }

    private void applyUnselectedStyle(TextView chip) {
        chip.setBackgroundResource(R.drawable.chip_unselected);
        chip.setTextColor(getResources().getColor(R.color.text_primary));
    }

    private void wireSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.setHint("Search by patient or doctor...");
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString();
                renderList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFromDb();
    }

    private void loadFromDb() {
        DatabaseHelper db = new DatabaseHelper(this);
        allAppointments = db.getAllAppointments();
        renderList();
    }

    private void renderList() {
        List<Appointment> filtered = new ArrayList<>();
        String q = currentSearch.toLowerCase(Locale.getDefault()).trim();
        for (Appointment a : allAppointments) {
            if (!"All".equals(currentFilter) && !currentFilter.equals(a.getStatus())) continue;
            if (!q.isEmpty()) {
                boolean matches = a.getDoctorName().toLowerCase(Locale.getDefault()).contains(q)
                        || (a.getPatientName() != null &&
                            a.getPatientName().toLowerCase(Locale.getDefault()).contains(q));
                if (!matches) continue;
            }
            filtered.add(a);
        }

        if (filtered.isEmpty()) {
            emptyContainer.setVisibility(View.VISIBLE);
            tvEmpty.setText(allAppointments.isEmpty()
                    ? "No appointments to manage yet."
                    : "No matches for your filter.");
            rv.setVisibility(View.GONE);
            return;
        }
        emptyContainer.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
        rv.setAdapter(new AppointmentAdapter(filtered, true, this::showActions));
    }

    private void showActions(Appointment a) {
        String[] actions = {"✓ Confirm", "✓ Mark Completed", "✕ Cancel", "🗑 Delete"};

        // Build rich subtitle with patient details + symptoms
        DatabaseHelper db = new DatabaseHelper(this);
        com.example.clinicapp.models.User patient = db.getUserById(a.getPatientId());

        StringBuilder subtitle = new StringBuilder();
        subtitle.append("📅 ").append(a.getDate()).append(" at ").append(a.getTime())
                .append("\n🏷 Status: ").append(a.getStatus());

        if (patient != null) {
            subtitle.append("\n\n👤 PATIENT DETAILS");
            if (patient.getAge() > 0) subtitle.append("\nAge: ").append(patient.getAge());
            if (patient.getGender() != null && !patient.getGender().isEmpty())
                subtitle.append("\nGender: ").append(patient.getGender());
            if (patient.getPhone() != null && !patient.getPhone().isEmpty())
                subtitle.append("\nPhone: ").append(patient.getPhone());
            if (patient.getAddress() != null && !patient.getAddress().isEmpty())
                subtitle.append("\nAddress: ").append(patient.getAddress());
        }

        subtitle.append("\n\n📝 Reason: ").append(a.getReason());
        if (a.getSymptoms() != null && !a.getSymptoms().isEmpty()) {
            subtitle.append("\n🤒 Symptoms: ").append(a.getSymptoms());
        }

        ModernDialog.showActionPicker(this,
                a.getPatientName() + "\n" + a.getDoctorName(),
                subtitle.toString(),
                actions,
                which -> {
                    DatabaseHelper db2 = new DatabaseHelper(this);
                    switch (which) {
                        case 0:
                            db2.updateAppointmentStatus(a.getId(), Appointment.STATUS_CONFIRMED);
                            ModernDialog.showSuccess(this, "Confirmed",
                                    "The appointment has been confirmed.");
                            break;
                        case 1:
                            db2.updateAppointmentStatus(a.getId(), Appointment.STATUS_COMPLETED);
                            NotificationHelper.cancelReminder(this, a.getId());
                            ModernDialog.showSuccess(this, "Completed",
                                    "The appointment has been marked as completed.");
                            break;
                        case 2:
                            db2.updateAppointmentStatus(a.getId(), Appointment.STATUS_CANCELLED);
                            NotificationHelper.cancelReminder(this, a.getId());
                            ModernDialog.showSuccess(this, "Cancelled",
                                    "The appointment has been cancelled.");
                            break;
                        case 3:
                            ModernDialog.showConfirm(this, "Delete Appointment",
                                    "Are you sure you want to permanently delete this appointment?",
                                    "Delete",
                                    () -> {
                                        db2.deleteAppointment(a.getId());
                                        NotificationHelper.cancelReminder(this, a.getId());
                                        ModernDialog.showSuccess(this, "Deleted",
                                                "The appointment has been removed.");
                                        loadFromDb();
                                    });
                            return;
                    }
                    loadFromDb();
                });
    }
}
