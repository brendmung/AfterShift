package com.tricenc.aftershift;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView; // NEW
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
//import android.utils.Log;
//import com.tricenc.shopreport.BuildConfig;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.InputStream;
import java.io.IOException; // ADD THIS LINE
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SavedReportsAdapter.OnReportInteractionListener {

    private RecyclerView savedReportsRecyclerView;
    private SavedReportsAdapter savedReportsAdapter;
    private FloatingActionButton fabAddReport;
    private TextView emptyStateText;
    private TextView greetingText;
    private TextView reportCountText;
    private TextView toolbarTitle; // For the custom app title
    private ImageButton menuButton; // NEW: Menu button for custom header
    private List<SavedReport> savedReports;
    private ReportDatabase reportDatabase;
    private TemplateManager templateManager; // NEW

    // ActivityResultLauncher for picking a file (for importing templates)
    private final ActivityResultLauncher<Intent> pickFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handlePickedFile(result.getData());
                } else {
                    Toast.makeText(this, "File picking cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before super.onCreate and setContentView
        templateManager = TemplateManager.getInstance(this); // Initialize TemplateManager early
        applyTheme(); // NEW: Apply saved theme mode

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerView();
        updateGreeting();
        loadSavedReports();
        setupClickListeners();
        setupMenuButton(); // NEW: Setup menu button
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply theme just in case it was changed and activity was paused/resumed
        applyTheme();
        updateGreeting(); // Refresh greeting as time might change
        loadSavedReports(); // Refresh the list when returning from report generator
    }

    // NEW: Apply theme from preferences
    private void applyTheme() {
        int themeMode = templateManager.getThemeMode();
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    private void initializeViews() {
        savedReportsRecyclerView = findViewById(R.id.savedReportsRecyclerView);
        fabAddReport = findViewById(R.id.fabAddReport);
        emptyStateText = findViewById(R.id.emptyStateText);
        greetingText = findViewById(R.id.greetingText);
        reportCountText = findViewById(R.id.reportCountText);
        toolbarTitle = findViewById(R.id.toolbarTitle);
        menuButton = findViewById(R.id.menuButton); // NEW: Initialize menu button

        // Set the app title in the custom header
        toolbarTitle.setText(getString(R.string.app_name_main_title));

        savedReports = new ArrayList<>();
        reportDatabase = new ReportDatabase(this);
    }

    private void setupRecyclerView() {
        savedReportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        savedReportsAdapter = new SavedReportsAdapter(this, savedReports, this);
        savedReportsRecyclerView.setAdapter(savedReportsAdapter);
    }

    private void loadSavedReports() {
        savedReports.clear();
        savedReports.addAll(reportDatabase.getAllReports());
        savedReportsAdapter.notifyDataSetChanged();

        updateReportCount();

        // Show/hide empty state
        if (savedReports.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            savedReportsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            savedReportsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning!";
        } else if (hour >= 12 && hour < 18) {
            greeting = "Good Afternoon!";
        } else {
            greeting = "Good Evening!";
        }
        greetingText.setText(greeting);
    }

    private void updateReportCount() {
        reportCountText.setText(getString(R.string.report_count_format, savedReports.size()));
    }


    private void setupClickListeners() {
        fabAddReport.setOnClickListener(v -> createNewReport());
    }

    private void createNewReport() {
        Intent intent = new Intent(this, ReportGeneratorActivity.class);
        startActivity(intent);
    }

    @Override
    public void onReportClick(SavedReport report) {
        Intent intent = new Intent(this, ReportGeneratorActivity.class);
        intent.putExtra("REPORT_ID", report.getId());
        intent.putExtra("REPORT_TEMPLATE_ID", report.getTemplateId()); // Pass saved template ID
        startActivity(intent);
    }

    @Override
    public void onReportDelete(SavedReport report) {
        reportDatabase.deleteReport(report.getId());
        savedReports.remove(report);
        savedReportsAdapter.notifyDataSetChanged();
        loadSavedReports(); // Update empty state and report count
        Toast.makeText(this, "Report deleted", Toast.LENGTH_SHORT).show();
    }

    // NEW: Setup the custom menu button
    private void setupMenuButton() {
        menuButton.setOnClickListener(this::showPopupMenu);
    }

    // NEW: Show the popup menu
    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.menu_main_activity, popup.getMenu());
        popup.setOnMenuItemClickListener(this::onMainMenuItemClick);
        popup.show();
    }

    // NEW: Handle menu item clicks for MainActivity
    private boolean onMainMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_import_template) {
            openFilePicker();
            return true;
        } else if (item.getItemId() == R.id.action_set_default_template) {
            showSetDefaultTemplateDialog();
            return true;
        } else if (item.getItemId() == R.id.action_change_theme) {
            showChangeThemeDialog();
            return true;
        } else if (item.getItemId() == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return false;
    }

    // NEW: Open file picker for importing templates
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");

        pickFileLauncher.launch(intent);
    }

    // NEW: Handle picked file (template import)
    private void handlePickedFile(Intent data) {
        if (data.getData() != null) {
            try (InputStream inputStream = getContentResolver().openInputStream(data.getData())) {
                if (inputStream != null) {
                    String fileName = getFileName(data.getData());
                    templateManager.importTemplate(inputStream, fileName);
                    Toast.makeText(this, "Template imported successfully!", Toast.LENGTH_SHORT).show();
                    // Optionally, you might want to immediately set it as current or default
                } else {
                    Toast.makeText(this, "Failed to open file.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // Log and show more detailed error to user
                String errorMessage = "Error importing template. Please check the JSON format. Details: " + e.getMessage();
                Log.e("MainActivity", errorMessage, e); // Use Log.e for errors
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    // NEW: Helper to get filename from URI
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    // NEW: Dialog for setting default template (with long-click delete)
    private void showSetDefaultTemplateDialog() {
        List<ReportTemplate> templates = templateManager.getAvailableTemplates();
        List<String> templateDisplayNames = new ArrayList<>();

        // Sort templates alphabetically by name, but put default first
        templates.sort((t1, t2) -> {
            if (t1.getTemplateId().equals(TemplateManager.DEFAULT_TEMPLATE_ID)) return -1;
            if (t2.getTemplateId().equals(TemplateManager.DEFAULT_TEMPLATE_ID)) return 1;
            return t1.getName().compareToIgnoreCase(t2.getName());
        });

        for (ReportTemplate t : templates) {
            templateDisplayNames.add(t.getName() + (t.getTemplateId().equals(templateManager.getCurrentTemplateId()) ? " (Current Default)" : ""));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Default Template for New Reports");

        // Use a ListView directly in the AlertDialog for better control over click listeners
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, templateDisplayNames);
        builder.setAdapter(adapter, (dialog, which) -> {
            // Regular click: Set as default
            ReportTemplate selectedTemplate = templates.get(which);
            templateManager.saveCurrentTemplateId(selectedTemplate.getTemplateId()); // Save as default
            Toast.makeText(this, "'" + selectedTemplate.getName() + "' set as default for new reports.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // To handle long clicks, we need to get access to the ListView after it's created.
        // This is a bit of a hack, but works for AlertDialogs using setAdapter.
        final AlertDialog dialog = builder.create();
        dialog.show(); // Show the dialog first
        ListView listView = dialog.getListView();
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            ReportTemplate longClickedTemplate = templates.get(position);
            if (longClickedTemplate.getTemplateId().equals(TemplateManager.DEFAULT_TEMPLATE_ID)) {
                Toast.makeText(this, "Cannot delete the built-in default template.", Toast.LENGTH_SHORT).show();
                return true; // Consume the long click
            }

            new AlertDialog.Builder(this)
                    .setTitle("Delete Template")
                    .setMessage("Are you sure you want to delete template '" + longClickedTemplate.getName() + "'?")
                    .setPositiveButton("Delete", (deleteDialog, whichBtn) -> {
                        if (reportDatabase.hasReportsUsingTemplate(longClickedTemplate.getTemplateId())) {
                            Toast.makeText(this, "Cannot delete template: Reports are currently using it.", Toast.LENGTH_LONG).show();
                        } else {
                            try {
                                templateManager.deleteTemplate(longClickedTemplate.getTemplateId());
                                Toast.makeText(this, "'" + longClickedTemplate.getName() + "' deleted.", Toast.LENGTH_SHORT).show();
                                dialog.dismiss(); // Dismiss the template selection dialog
                                showSetDefaultTemplateDialog(); // Re-open to refresh the list
                            } catch (IOException | IllegalArgumentException e) {
                                Toast.makeText(this, "Error deleting template: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e("MainActivity", "Error deleting template", e);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true; // Consume the long click
        });

        builder.setNegativeButton("Cancel", null); // Keep the cancel button
        // Re-create the dialog to ensure the negative button is correctly set up if the dialog was dismissed and re-shown
        // This line is effectively redundant if the dialog is dismissed and re-created, but harmless.
        // The dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener is a more direct way if needed.
    }

    // NEW: Dialog for changing theme
    private void showChangeThemeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Theme");

        final String[] themeOptions = {"System Default", "Light Mode", "Dark Mode"};
        int checkedItem = -1; // No item checked by default

        int currentMode = templateManager.getThemeMode();
        if (currentMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            checkedItem = 0;
        } else if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) {
            checkedItem = 1;
        } else if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            checkedItem = 2;
        }

        builder.setSingleChoiceItems(themeOptions, checkedItem, (dialog, which) -> {
            int newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // Default to system

            if (which == 1) { // Light Mode
                newMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (which == 2) { // Dark Mode
                newMode = AppCompatDelegate.MODE_NIGHT_YES;
            }

            templateManager.saveThemeMode(newMode);
            AppCompatDelegate.setDefaultNightMode(newMode);
            dialog.dismiss();

            // Recreate activity to apply theme changes immediately.
            // This might cause a slight flicker, but ensures full theme application.
            recreate();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // NEW: About Dialog
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name_main_title) + " " + "1.0") // Assuming BuildConfig has VERSION_NAME
                .setMessage(getString(R.string.about_text))
                .setPositiveButton("OK", null)
                .show();
    }


    public static class SavedReport {
        private long id;
        private String title;
        private String date; // Last Edited Date
        private String previewText;
        private String reportData;
        private String templateId;
        private String createdDate; // NEW

        public SavedReport(long id, String title, String date, String previewText, String reportData, String templateId, String createdDate) {
            this.id = id;
            this.title = title;
            this.date = date; // Last Edited Date
            this.previewText = previewText;
            this.reportData = reportData;
            this.templateId = templateId;
            this.createdDate = createdDate; // NEW
        }

        // Existing constructor for compatibility (will be called by old ReportDatabase versions)
        public SavedReport(long id, String title, String date, String previewText, String reportData, String templateId) {
            this(id, title, date, previewText, reportData, templateId, date); // Default createdDate to last edited date
        }

        // Getters and setters
        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDate() { return date; } // Last Edited Date
        public String getPreviewText() { return previewText; }
        public void setPreviewText(String previewText) { this.previewText = previewText; }
        public String getReportData() { return reportData; }
        public void setReportData(String reportData) { this.reportData = reportData; }
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
        public String getCreatedDate() { return createdDate; } // NEW
        public void setCreatedDate(String createdDate) { this.createdDate = createdDate; } // NEW
    }
}