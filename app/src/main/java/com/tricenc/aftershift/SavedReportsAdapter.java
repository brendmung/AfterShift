package com.tricenc.aftershift;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SavedReportsAdapter extends RecyclerView.Adapter<SavedReportsAdapter.SavedReportViewHolder> {

    private List<MainActivity.SavedReport> savedReports;
    private Context context;
    private OnReportInteractionListener listener;

    public interface OnReportInteractionListener {
        void onReportClick(MainActivity.SavedReport report);
        void onReportDelete(MainActivity.SavedReport report);
    }

    public SavedReportsAdapter(Context context, List<MainActivity.SavedReport> savedReports, OnReportInteractionListener listener) {
        this.context = context;
        this.savedReports = savedReports;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SavedReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.saved_report_item, parent, false);
        return new SavedReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedReportViewHolder holder, int position) {
        MainActivity.SavedReport report = savedReports.get(position);

        holder.titleText.setText(report.getTitle());
        
        // Use the enhanced formatter for both dates
        holder.dateText.setText(String.format("Last Edited: %s", formatDateWithRelativeTime(report.getDate())));
        holder.createdDateText.setText(String.format("Created: %s", formatDateWithRelativeTime(report.getCreatedDate())));
        
        holder.previewText.setText(report.getPreviewText());

        holder.itemView.setOnClickListener(v -> listener.onReportClick(report));

        holder.deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                .setTitle("Delete Report")
                .setMessage("Are you sure you want to delete this report?")
                .setPositiveButton("Delete", (dialog, which) -> listener.onReportDelete(report))
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    @Override
    public int getItemCount() {
        return savedReports.size();
    }

    /**
     * Formats the date string to use relative time (Today, Yesterday, DayOfWeek) if recent,
     * otherwise uses the full date and time.
     * @param dateString The date string in "dd/MM/yy HH:mm" format.
     * @return The formatted date string.
     */
    private String formatDateWithRelativeTime(String dateString) {
        SimpleDateFormat dbFormat = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
        SimpleDateFormat displayTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat displayDayOfWeekTimeFormat = new SimpleDateFormat("EEEE HH:mm", Locale.getDefault());
        SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());

        try {
            Date reportDate = dbFormat.parse(dateString);
            if (reportDate == null) return dateString;

            Calendar today = Calendar.getInstance();
            today.setTime(new Date());
            Calendar yesterday = Calendar.getInstance();
            yesterday.setTime(new Date());
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            
            Calendar sevenDaysAgo = Calendar.getInstance();
            sevenDaysAgo.setTime(new Date());
            sevenDaysAgo.add(Calendar.DAY_OF_YEAR, -7);

            Calendar reportCal = Calendar.getInstance();
            reportCal.setTime(reportDate);
            
            // Set time components of comparison dates to 00:00:00 for accurate day comparison
            clearTime(today);
            clearTime(yesterday);
            clearTime(sevenDaysAgo);
            clearTime(reportCal);
            
            // Re-parse the original date to get the correct time component back
            reportCal.setTime(reportDate);

            if (isSameDay(reportCal, today)) {
                return "Today " + displayTimeFormat.format(reportDate);
            } else if (isSameDay(reportCal, yesterday)) {
                return "Yesterday " + displayTimeFormat.format(reportDate);
            } else if (reportCal.after(sevenDaysAgo)) {
                // Within the last 7 days (excluding today and yesterday, which are already handled)
                return displayDayOfWeekTimeFormat.format(reportDate); // e.g., "Wednesday 10:30"
            } else {
                return displayDateFormat.format(reportDate);
            }

        } catch (ParseException e) {
            e.printStackTrace();
            return dateString;
        }
    }
    
    private void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static class SavedReportViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView dateText; // Last Edited Date
        TextView createdDateText; // NEW
        TextView previewText;
        ImageButton deleteButton;

        public SavedReportViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.titleText);
            dateText = itemView.findViewById(R.id.dateText);
            createdDateText = itemView.findViewById(R.id.createdDateText);
            previewText = itemView.findViewById(R.id.previewText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}