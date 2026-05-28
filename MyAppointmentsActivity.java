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
import com.example.clinicapp.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyAppointmentsActivity extends AppCompatActivity {
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

        // Check if a default filter was passed (e.g. from History card)
        String requestedFilter = getIntent().getStringExtra("default_filter");
        String headerTitle = getIntent().getStringExtra("header_title");
        String headerLabel = getIntent().getStringExtra("header_label");

        if (requestedFilter != null) currentFilter = requestedFilter;

        // Set hero header (custom from intent or default)
        ((TextView) findViewById(R.id.tvHeaderLabel))
                .setText(headerLabel != null ? headerLabel : "MY BOOKINGS");
        ((TextView) findViewById(R.id.tvHeaderTitle))
                .setText(headerTitle != null ? headerTitle : "My Appointments");

        // Show search and filter
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
            if (f.equals(currentFilter)) {
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
        etSearch.setHint("Search by doctor or specialty...");
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
        SessionManager session = new SessionManager(this);
        DatabaseHelper db = new DatabaseHelper(this);
        allAppointments = db.getAppointmentsForPatient(session.getUserId());
        renderList();
    }

    private void renderList() {
        List<Appointment> filtered = new ArrayList<>();
        String q = currentSearch.toLowerCase(Locale.getDefault()).trim();
        for (Appointment a : allAppointments) {
            if (!"All".equals(currentFilter) && !currentFilter.equals(a.getStatus())) continue;
            if (!q.isEmpty()) {
                boolean matches = a.getDoctorName().toLowerCase(Locale.getDefault()).contains(q)
                        || (a.getSpecialization() != null && a.getSpecialization()
                            .toLowerCase(Locale.getDefault()).contains(q));
                if (!matches) continue;
            }
            filtered.add(a);
        }

        if (filtered.isEmpty()) {
            emptyContainer.setVisibility(View.VISIBLE);
            if (allAppointments.isEmpty()) {
                tvEmpty.setText("No appointments yet.\nTap 'Book Appointment' to get started.");
            } else {
                tvEmpty.setText("No matches for your filter.");
            }
            rv.setVisibility(View.GONE);
            return;
        }
        emptyContainer.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
        rv.setAdapter(new AppointmentAdapter(filtered, false, this::showOptions));
    }

    private void showOptions(Appointment a) {
        String details = "Date: " + a.getDate() + " " + a.getTime() +
                "\nStatus: " + a.getStatus() +
                "\nReason: " + a.getReason();

        if (Appointment.STATUS_CANCELLED.equals(a.getStatus()) ||
                Appointment.STATUS_COMPLETED.equals(a.getStatus())) {
            ModernDialog.showInfo(this, a.getDoctorName(), details);
            return;
        }
        new ModernDialog(this)
                .setType(ModernDialog.Type.CONFIRM)
                .setTitle(a.getDoctorName())
                .setMessage(details)
                .setPositiveButton("Cancel Booking", () -> cancelAppt(a))
                .setNegativeButton("Close", null)
                .show();
    }

    private void cancelAppt(Appointment a) {
        DatabaseHelper db = new DatabaseHelper(this);
        if (db.updateAppointmentStatus(a.getId(), Appointment.STATUS_CANCELLED)) {
            NotificationHelper.cancelReminder(this, a.getId());
            ModernDialog.showSuccess(this, "Appointment Cancelled",
                    "Your appointment has been cancelled successfully.");
            loadFromDb();
        }
    }
}
