package com.example.clinicapp.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clinicapp.R;
import com.example.clinicapp.database.DatabaseHelper;
import com.example.clinicapp.models.User;
import com.example.clinicapp.utils.ModernDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManagePatientsActivity extends AppCompatActivity {
    private RecyclerView rv;
    private TextView tvEmpty;
    private View emptyContainer;
    private List<User> allPatients = new ArrayList<>();
    private String currentSearch = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        ((TextView) findViewById(R.id.tvHeaderLabel)).setText("ADMIN PANEL");
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText("Manage Patients");

        findViewById(R.id.searchBarContainer).setVisibility(View.VISIBLE);

        rv = findViewById(R.id.rv);
        tvEmpty = findViewById(R.id.tvEmpty);
        emptyContainer = findViewById(R.id.emptyContainer);
        rv.setLayoutManager(new LinearLayoutManager(this));

        wireSearch();
    }

    private void wireSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.setHint("Search by name or email...");
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString();
                renderList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFromDb();
    }

    private void loadFromDb() {
        DatabaseHelper db = new DatabaseHelper(this);
        allPatients = db.getAllPatients();
        renderList();
    }

    private void renderList() {
        List<User> filtered = new ArrayList<>();
        String q = currentSearch.toLowerCase(Locale.getDefault()).trim();
        for (User u : allPatients) {
            if (!q.isEmpty()) {
                boolean matches = u.getFullName().toLowerCase(Locale.getDefault()).contains(q)
                        || u.getEmail().toLowerCase(Locale.getDefault()).contains(q);
                if (!matches) continue;
            }
            filtered.add(u);
        }

        if (filtered.isEmpty()) {
            emptyContainer.setVisibility(View.VISIBLE);
            tvEmpty.setText(allPatients.isEmpty()
                    ? "No registered patients yet."
                    : "No matches for your search.");
            rv.setVisibility(View.GONE);
            return;
        }
        emptyContainer.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
        rv.setAdapter(new PatientAdapter(filtered));
    }

    private void confirmDelete(User u) {
        ModernDialog.showConfirm(this,
                "Delete Patient",
                "Delete " + u.getFullName() + " and all of their appointments?\n\n" +
                        "This action cannot be undone.",
                "Delete",
                () -> {
                    DatabaseHelper db = new DatabaseHelper(this);
                    if (db.deletePatient(u.getId())) {
                        ModernDialog.showSuccess(this, "Patient Deleted",
                                u.getFullName() + " has been removed from the system.");
                        loadFromDb();
                    }
                });
    }

    private class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.VH> {
        private final List<User> data;
        PatientAdapter(List<User> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_patient, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            User u = data.get(position);
            h.tvName.setText(u.getFullName());
            h.tvEmail.setText(u.getEmail());
            h.tvPhone.setText(u.getPhone());
            h.itemView.setOnLongClickListener(v -> { confirmDelete(u); return true; });
            h.itemView.setOnClickListener(v ->
                    ModernDialog.showInfo(ManagePatientsActivity.this,
                            u.getFullName(),
                            "Email: " + u.getEmail() +
                                    "\nPhone: " + u.getPhone() +
                                    "\n\nLong-press to delete this patient."));
        }

        @Override
        public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvPhone;
            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvEmail = itemView.findViewById(R.id.tvEmail);
                tvPhone = itemView.findViewById(R.id.tvPhone);
            }
        }
    }
}
