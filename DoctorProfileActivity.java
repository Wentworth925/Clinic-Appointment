package com.example.clinicapp.activities;

import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clinicapp.R;
import com.example.clinicapp.database.DatabaseHelper;
import com.example.clinicapp.models.Appointment;
import com.example.clinicapp.models.Doctor;
import com.example.clinicapp.utils.ModernDialog;
import com.example.clinicapp.utils.NotificationHelper;
import com.example.clinicapp.utils.SessionManager;
import com.google.android.flexbox.FlexboxLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DoctorProfileActivity extends AppCompatActivity {

    private Doctor doctor;
    private Calendar selectedDate;
    private String selectedTime;
    private LinearLayout selectedDayPill;
    private TextView selectedTimePill;

    private LinearLayout dayPillsContainer;
    private FlexboxLayout timeSlotsContainer;

    // Map of timeString -> the TextView pill, so we can update them when date changes
    private final Map<String, TextView> timeSlotPills = new HashMap<>();
    private List<String> bookedTimes;

    private static final String[] TIME_SLOTS = {
            "08:00", "09:00", "10:00", "11:00", "12:00",
            "13:00", "14:00", "15:00", "16:00"
    };

    private static final String[] DAY_LABELS = {
            "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        int doctorId = getIntent().getIntExtra("doctor_id", -1);
        String name = getIntent().getStringExtra("doctor_name");
        String spec = getIntent().getStringExtra("doctor_spec");
        String days = getIntent().getStringExtra("doctor_days");

        if (doctorId == -1 || name == null) {
            Toast.makeText(this, "Doctor not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        doctor = new Doctor(doctorId, name, spec, days);

        ((TextView) findViewById(R.id.tvInitials)).setText(doctor.getInitials());
        ((TextView) findViewById(R.id.tvName)).setText(doctor.getName());
        ((TextView) findViewById(R.id.tvSpec)).setText("✚ " + doctor.getSpecialization());
        ((TextView) findViewById(R.id.tvAbout)).setText(doctor.getAbout());

        // Format available days nicely
        String availDays = doctor.getAvailableDays();
        if (availDays != null && !availDays.isEmpty()) {
            ((TextView) findViewById(R.id.tvAvailableDays))
                    .setText("📆 Available: " + availDays.replace(",", ", "));
        }

        ((TextView) findViewById(R.id.tvPatients)).setText(doctor.getPatientsCount() + "+");
        ((TextView) findViewById(R.id.tvExperience)).setText(doctor.getYearsExperience() + "+");
        ((TextView) findViewById(R.id.tvReviews)).setText(doctor.getReviewsCount() + "+");

        ((TextView) findViewById(R.id.tvMonth)).setText(
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        .format(Calendar.getInstance().getTime()));

        dayPillsContainer = findViewById(R.id.dayPillsContainer);
        timeSlotsContainer = findViewById(R.id.timeSlotsContainer);

        buildDayPills();
        buildTimeSlots();

        Button btnBook = findViewById(R.id.btnBook);
        btnBook.setOnClickListener(v -> attemptBooking());
    }

    private void buildDayPills() {
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 14; i++) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            String label = DAY_LABELS[dayOfWeek];
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            Calendar pillDate = (Calendar) cal.clone();

            View pill = createDayPill(label, dayOfMonth, pillDate);
            dayPillsContainer.addView(pill);

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private View createDayPill(String label, int dayOfMonth, Calendar date) {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.VERTICAL);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(16), dp(12), dp(16), dp(12));
        pill.setBackgroundResource(R.drawable.chip_unselected);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dp(64), LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        pill.setLayoutParams(lp);

        TextView tvNum = new TextView(this);
        tvNum.setText(String.valueOf(dayOfMonth));
        tvNum.setTextColor(getResources().getColor(R.color.text_primary));
        tvNum.setTextSize(18);
        tvNum.setGravity(Gravity.CENTER);
        tvNum.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(getResources().getColor(R.color.text_secondary));
        tvLabel.setTextSize(11);
        tvLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(2);
        tvLabel.setLayoutParams(labelParams);

        pill.addView(tvNum);
        pill.addView(tvLabel);

        pill.setOnClickListener(v -> selectDayPill(pill, date, tvNum, tvLabel));
        return pill;
    }

    private void selectDayPill(LinearLayout pill, Calendar date, TextView tvNum, TextView tvLabel) {
        if (selectedDayPill != null) {
            selectedDayPill.setBackgroundResource(R.drawable.chip_unselected);
            View child0 = selectedDayPill.getChildAt(0);
            View child1 = selectedDayPill.getChildAt(1);
            if (child0 instanceof TextView) {
                ((TextView) child0).setTextColor(getResources().getColor(R.color.text_primary));
            }
            if (child1 instanceof TextView) {
                ((TextView) child1).setTextColor(getResources().getColor(R.color.text_secondary));
            }
        }
        pill.setBackgroundResource(R.drawable.chip_selected);
        tvNum.setTextColor(getResources().getColor(R.color.white));
        tvLabel.setTextColor(getResources().getColor(R.color.gold));
        selectedDayPill = pill;
        selectedDate = date;

        // Reset any previously selected time and refresh visual state
        selectedTime = null;
        selectedTimePill = null;
        refreshTimeSlotAvailability();
    }

    private void buildTimeSlots() {
        for (String time : TIME_SLOTS) {
            TextView slot = createTimeSlot(time);
            timeSlotsContainer.addView(slot);
            timeSlotPills.put(time, slot);
        }
    }

    private TextView createTimeSlot(String time) {
        TextView tv = new TextView(this);
        tv.setText(formatDisplayTime(time));
        tv.setBackgroundResource(R.drawable.chip_unselected);
        tv.setTextColor(getResources().getColor(R.color.text_primary));
        tv.setTextSize(13);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(dp(16), dp(10), dp(16), dp(10));
        tv.setGravity(Gravity.CENTER);

        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(8), dp(8));
        tv.setLayoutParams(lp);

        tv.setOnClickListener(v -> handleTimeSlotClick(tv, time));
        return tv;
    }

    private void handleTimeSlotClick(TextView pill, String time) {
        if (bookedTimes != null && bookedTimes.contains(time)) {
            ModernDialog.showWarning(this, "Slot Unavailable",
                    "This time slot is already booked.\nPlease choose a different time.");
            return;
        }
        selectTimeSlot(pill, time);
    }

    private void selectTimeSlot(TextView pill, String time) {
        if (selectedTimePill != null) {
            selectedTimePill.setBackgroundResource(R.drawable.chip_unselected);
            selectedTimePill.setTextColor(getResources().getColor(R.color.text_primary));
        }
        pill.setBackgroundResource(R.drawable.chip_selected);
        pill.setTextColor(getResources().getColor(R.color.white));
        selectedTimePill = pill;
        selectedTime = time;
    }

    /**
     * Updates the visual state of all time slot pills based on which times
     * are already booked for the selected doctor on the selected date.
     */
    private void refreshTimeSlotAvailability() {
        if (selectedDate == null) return;

        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(selectedDate.getTime());

        DatabaseHelper db = new DatabaseHelper(this);
        bookedTimes = db.getBookedTimes(doctor.getName(), dateStr);

        for (Map.Entry<String, TextView> entry : timeSlotPills.entrySet()) {
            String time = entry.getKey();
            TextView pill = entry.getValue();

            if (bookedTimes.contains(time)) {
                // Mark as booked - greyed out with strikethrough
                pill.setBackgroundResource(R.drawable.chip_booked);
                pill.setTextColor(getResources().getColor(R.color.text_tertiary));
                pill.setPaintFlags(pill.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                pill.setText(formatDisplayTime(time) + " (Booked)");
            } else {
                // Available - reset to default
                pill.setBackgroundResource(R.drawable.chip_unselected);
                pill.setTextColor(getResources().getColor(R.color.text_primary));
                pill.setPaintFlags(pill.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                pill.setText(formatDisplayTime(time));
            }
        }
    }

    private String formatDisplayTime(String hhmm) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return out.format(in.parse(hhmm));
        } catch (Exception e) {
            return hhmm;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void attemptBooking() {
        if (selectedDate == null) {
            ModernDialog.showWarning(this, "Date Required",
                    "Please select a date for your appointment.");
            return;
        }
        if (selectedTime == null) {
            ModernDialog.showWarning(this, "Time Required",
                    "Please select a time slot for your appointment.");
            return;
        }
        EditText etReason = findViewById(R.id.etReason);
        EditText etSymptoms = findViewById(R.id.etSymptoms);
        String reason = etReason.getText().toString().trim();
        String symptoms = etSymptoms.getText().toString().trim();
        if (TextUtils.isEmpty(reason)) {
            etReason.setError("Please describe the reason");
            ModernDialog.showWarning(this, "Reason Required",
                    "Please briefly describe the reason for your visit.");
            return;
        }

        String[] parts = selectedTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar appt = (Calendar) selectedDate.clone();
        appt.set(Calendar.HOUR_OF_DAY, hour);
        appt.set(Calendar.MINUTE, minute);
        appt.set(Calendar.SECOND, 0);
        appt.set(Calendar.MILLISECOND, 0);

        if (appt.getTimeInMillis() <= System.currentTimeMillis()) {
            ModernDialog.showError(this, "Invalid Time",
                    "Please choose a future date and time for your appointment.");
            return;
        }

        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(appt.getTime());
        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(appt.getTime());

        DatabaseHelper db = new DatabaseHelper(this);
        if (db.isSlotTaken(doctor.getName(), dateStr, timeStr)) {
            ModernDialog.showError(this, "Slot Taken",
                    "Sorry, this slot was just booked by another patient. " +
                            "Please choose a different time.");
            refreshTimeSlotAvailability();
            return;
        }

        SessionManager session = new SessionManager(this);
        Appointment a = new Appointment();
        a.setPatientId(session.getUserId());
        a.setPatientName(session.getUserName());
        a.setDoctorName(doctor.getName());
        a.setSpecialization(doctor.getSpecialization());
        a.setDate(dateStr);
        a.setTime(timeStr);
        a.setReason(reason);
        a.setSymptoms(symptoms);
        a.setStatus(Appointment.STATUS_PENDING);
        a.setTimestamp(appt.getTimeInMillis());

        long id = db.bookAppointment(a);
        if (id > 0) {
            a.setId((int) id);
            NotificationHelper.scheduleReminder(this, a);

            new ModernDialog(this)
                    .setType(ModernDialog.Type.SUCCESS)
                    .setTitle("Booking Confirmed!")
                    .setMessage("Your appointment with " + doctor.getName() +
                            "\non " + dateStr + " at " + formatDisplayTime(timeStr) +
                            "\n\nYou will receive a reminder 1 hour before.")
                    .setPositiveButton("Done", () -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            ModernDialog.showError(this, "Booking Failed",
                    "Could not save your appointment. Please try again.");
        }
    }
}
