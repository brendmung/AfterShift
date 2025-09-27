package com.tricenc.aftershift;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType; // NEW
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportGeneratorActivity extends AppCompatActivity implements ReportGeneratorAdapter.OnItemInteractionListener {

    private static final String TAG = "ReportGenActivity";

    private RecyclerView fieldsRecyclerView;
    private ReportGeneratorAdapter reportAdapter;
    private Button saveButton;
    private Button sendButton; // Now "Preview" button
    private List<ReportItem> reportItems;
    private ReportDatabase reportDatabase;
    private TemplateManager templateManager;
    private ReportTemplate currentTemplate;
    private long currentReportId = -1;
    private String userDefinedTitle = null; // NEW: Field to store user-defined title

    private final Gson gson = new Gson();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Map<String, ReportItem> reportItemMap; // NEW: Map to quickly access ReportItems by internalId
    private Map<String, List<String>> fieldDependencies; // NEW: Map: fieldId -> list of calculated fieldIds that depend on it


    // --- ReportItem Interface and Implementations ---
    public interface ReportItem {
        int TYPE_FIELD = 0;
        int TYPE_HEADER = 1;
        int TYPE_SECTION_FIELD = 2;

        int getType();
        String getInternalId();
        String getDisplayLabel();
        void setDisplayLabel(String newLabel);
        String getValue();
        void setValue(String newValue);
        String getInputType();
        boolean isCustom();
        String getParentSectionId();
        void setParentSectionId(String parentId);
        boolean isError();
        void setError(boolean isError);
        String getCalculationFormula(); // NEW
        boolean isCalculated(); // NEW
    }

    public static class ReportField implements ReportItem {
        private String internalId;
        private String displayLabel;
        private String value;
        private String inputType;
        private boolean editable; // Can be overridden by isCalculated
        private boolean isCustom;
        private boolean isError;
        private String calculationFormula; // NEW
        private boolean isCalculated; // NEW

        public ReportField(String internalId, String displayLabel, String value, String inputType, boolean editable, boolean isCustom, String calculationFormula) {
            this.internalId = internalId;
            this.displayLabel = displayLabel;
            this.value = value;
            this.inputType = inputType;
            this.isCustom = isCustom;
            this.isError = false;
            this.calculationFormula = calculationFormula;
            this.isCalculated = (calculationFormula != null && !calculationFormula.isEmpty());
            this.editable = editable && !this.isCalculated; // If calculated, it's not editable
        }

        // Existing constructor for compatibility
        public ReportField(String internalId, String displayLabel, String value, String inputType, boolean editable, boolean isCustom) {
            this(internalId, displayLabel, value, inputType, editable, isCustom, null);
        }

        @Override
        public int getType() { return TYPE_FIELD; }
        @Override
        public String getInternalId() { return internalId; }
        @Override
        public String getDisplayLabel() { return displayLabel; }
        @Override
        public void setDisplayLabel(String newLabel) { this.displayLabel = newLabel; }
        @Override
        public String getValue() { return value; }
        @Override
        public void setValue(String newValue) { this.value = newValue; }
        @Override
        public String getInputType() { return inputType; }
        public boolean isEditable() { return editable; } // Use this getter for adapter
        @Override
        public boolean isCustom() { return isCustom; }
        @Override
        public String getParentSectionId() { return null; }
        @Override
        public void setParentSectionId(String parentId) { /* Not applicable */ }
        @Override
        public boolean isError() { return isError; }
        @Override
        public void setError(boolean error) { isError = error; }
        // NEW Getters
        public String getCalculationFormula() { return calculationFormula; }
        public boolean isCalculated() { return isCalculated; }
    }

    public static class ReportHeader implements ReportItem {
        private String internalId;
        private String displayLabel;
        private boolean isCustom;
        private List<SectionField> sectionFields;
        private boolean isError;

        public ReportHeader(String internalId, String displayLabel, boolean isCustom) {
            this.internalId = internalId;
            this.displayLabel = displayLabel;
            this.isCustom = isCustom;
            this.sectionFields = new ArrayList<>();
            this.isError = false;
        }

        @Override
        public int getType() { return TYPE_HEADER; }
        @Override
        public String getInternalId() { return internalId; }
        @Override
        public String getDisplayLabel() { return displayLabel; }
        @Override
        public void setDisplayLabel(String newLabel) { this.displayLabel = newLabel; }
        @Override
        public String getValue() { return null; }
        @Override
        public void setValue(String newValue) { /* Not applicable */ }
        @Override
        public String getInputType() { return null; }
        @Override
        public boolean isCustom() { return isCustom; }
        @Override
        public String getParentSectionId() { return null; }
        @Override
        public void setParentSectionId(String parentId) { /* Not applicable */ }
        @Override
        public boolean isError() { return isError; }
        @Override
        public void setError(boolean error) { isError = error; }
        // NEW Getters (not applicable for headers)
        public String getCalculationFormula() { return null; }
        public boolean isCalculated() { return false; }


        public List<SectionField> getSectionFields() { return sectionFields; }
        public void addSectionField(SectionField field) { sectionFields.add(field); }
        public void removeSectionField(SectionField field) { sectionFields.remove(field); }
    }

    public static class SectionField implements ReportItem {
        private String internalId;
        private String displayLabel;
        private String value;
        private String inputType;
        private boolean editable; // Can be overridden by isCalculated
        private boolean isCustom;
        private String parentSectionId;
        private boolean isError;
        private String calculationFormula; // NEW
        private boolean isCalculated; // NEW

        public SectionField(String internalId, String displayLabel, String value, String inputType, boolean editable, boolean isCustom, String parentSectionId, String calculationFormula) {
            this.internalId = internalId;
            this.displayLabel = displayLabel;
            this.value = value;
            this.inputType = inputType;
            this.isCustom = isCustom;
            this.parentSectionId = parentSectionId;
            this.isError = false;
            this.calculationFormula = calculationFormula;
            this.isCalculated = (calculationFormula != null && !calculationFormula.isEmpty());
            this.editable = editable && !this.isCalculated; // If calculated, it's not editable
        }

        // Existing constructor for compatibility
        public SectionField(String internalId, String displayLabel, String value, String inputType, boolean editable, boolean isCustom, String parentSectionId) {
            this(internalId, displayLabel, value, inputType, editable, isCustom, parentSectionId, null);
        }

        @Override
        public int getType() { return TYPE_SECTION_FIELD; }
        @Override
        public String getInternalId() { return internalId; }
        @Override
        public String getDisplayLabel() { return displayLabel; }
        @Override
        public void setDisplayLabel(String newLabel) { this.displayLabel = newLabel; }
        @Override
        public String getValue() { return value; }
        @Override
        public void setValue(String newValue) { this.value = newValue; }
        @Override
        public String getInputType() { return inputType; }
        public boolean isEditable() { return editable; } // Use this getter for adapter
        @Override
        public boolean isCustom() { return isCustom; }
        @Override
        public String getParentSectionId() { return parentSectionId; }
        @Override
        public void setParentSectionId(String parentId) { this.parentSectionId = parentId; }
        @Override
        public boolean isError() { return isError; }
        @Override
        public void setError(boolean error) { isError = error; }
        // NEW Getters
        public String getCalculationFormula() { return calculationFormula; }
        public boolean isCalculated() { return isCalculated; }
    }
    // --- End ReportItem Interface and Implementations ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_generator);

        templateManager = TemplateManager.getInstance(this);

        setupToolbar();
        initializeViews();
        setupRecyclerView();

        long reportId = getIntent().getLongExtra("REPORT_ID", -1);
        String initialTemplateId = getIntent().getStringExtra("REPORT_TEMPLATE_ID"); // NEW: Get saved template ID

        if (reportId != -1) {
            currentReportId = reportId;
            loadExistingReport(reportId, initialTemplateId); // Pass template ID to load
        } else {
            // For new reports, use the default template set in MainActivity
            currentTemplate = templateManager.getCurrentTemplate();
            if (currentTemplate == null) {
                 Log.e(TAG, "FATAL: Default template not found, falling back to hardcoded default.");
                 currentTemplate = templateManager.getTemplate(TemplateManager.DEFAULT_TEMPLATE_ID);
                 if(currentTemplate == null) {
                     Toast.makeText(this, "Critical error: Default template missing. App may not function.", Toast.LENGTH_LONG).show();
                     finish();
                     return;
                 }
            }
            setupFieldsFromTemplate(currentTemplate);
            // Initialize userDefinedTitle for new reports with a default suggestion
            userDefinedTitle = currentTemplate.getName() + " - " + new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(new Date());
        }

        updateToolbarTitle(userDefinedTitle != null ? userDefinedTitle : currentTemplate.getName()); // Update toolbar with template name or loaded title
        setupClickListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Auto-save logic
        autoSaveReport();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            // Fix: Ensure back arrow and title color adapt to theme
            toolbar.setTitleTextColor(getResources().getColor(R.color.on_surface, getTheme()));
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            // FIX: Use ContextCompat.getColorStateList for setNavigationIconTint
            //toolbar.setNavigationIconTint(ContextCompat.getColorStateList(this, R.color.on_surface));
        }
    }

    private void updateToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_report_generator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_change_title) { // Changed to handle Change Title
            showChangeTitleDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        fieldsRecyclerView = findViewById(R.id.fieldsRecyclerView);
        saveButton = findViewById(R.id.saveButton);
        sendButton = findViewById(R.id.sendButton); // This will become Preview button
        reportItems = new ArrayList<>();
        reportItemMap = new HashMap<>(); // NEW: Initialize map
        fieldDependencies = new HashMap<>(); // NEW: Initialize map
        reportDatabase = new ReportDatabase(this);
    }

    private void setupRecyclerView() {
        fieldsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new ReportGeneratorAdapter(this, reportItems, this);
        fieldsRecyclerView.setAdapter(reportAdapter);
    }

    // Modified to load the correct template first and load the report title
    private void loadExistingReport(long reportId, String savedTemplateId) {
        currentReportId = reportId;
        ReportDatabase reportDb = new ReportDatabase(this);
        MainActivity.SavedReport savedReport = reportDb.getReport(reportId);

        if (savedReport != null) {
            // NEW: Load existing title
            userDefinedTitle = savedReport.getTitle();

            ReportTemplate templateToUse = templateManager.getTemplate(savedReport.getTemplateId());
            if (templateToUse == null) {
                // Fallback to default if the template used for saving is no longer available
                Log.w(TAG, "Template '" + savedReport.getTemplateId() + "' not found for saved report. Falling back to default template.");
                templateToUse = templateManager.getTemplate(TemplateManager.DEFAULT_TEMPLATE_ID);
                if (templateToUse == null) { // Should ideally not happen if default is always there
                    Toast.makeText(this, "Critical error: Default template missing.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            currentTemplate = templateToUse; // Set the current template
            updateToolbarTitle(userDefinedTitle); // Update toolbar title based on loaded title

            setupFieldsFromTemplate(currentTemplate); // Populate UI with the loaded template's structure
            parseAndLoadReportData(savedReport.getReportData()); // Fill values from saved data
        } else {
            Log.e(TAG, "Report with ID " + reportId + " not found, falling back to default template.");
            currentTemplate = templateManager.getTemplate(TemplateManager.DEFAULT_TEMPLATE_ID);
            if (currentTemplate == null) {
                Toast.makeText(this, "Critical error: Default template missing.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            setupFieldsFromTemplate(currentTemplate);
            userDefinedTitle = currentTemplate.getName() + " - " + new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(new Date());
        }
    }


    private void parseAndLoadReportData(String reportData) {
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> savedValues = gson.fromJson(reportData, type);

        if (savedValues == null) {
            Log.e(TAG, "Failed to parse report data (JSON): " + reportData);
            Toast.makeText(this, "Failed to load report data.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (ReportItem item : reportItems) {
            if (item.getType() == ReportItem.TYPE_FIELD || item.getType() == ReportItem.TYPE_SECTION_FIELD) {
                String value = savedValues.get(item.getInternalId());
                if (value != null) {
                    item.setValue(value);
                }
            }
            // Special handling for the date field: If it's empty in savedValues, keep the current date.
            // This is useful if a report was saved incomplete and user re-opens it.
            if (item.getInternalId().equals("date_field") && (item.getValue() == null || item.getValue().isEmpty())) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                item.setValue(sdf.format(new Date()));
            }
        }

        // NEW: After loading all values, recalculate dependent fields
        for (ReportItem item : reportItems) {
            if (item.isCalculated()) {
                recalculateField(item);
            }
        }

        reportAdapter.notifyDataSetChanged();
    }

    // NEW: Method to populate fields from a given ReportTemplate
    private void setupFieldsFromTemplate(ReportTemplate template) {
        reportItems.clear();
        reportItemMap.clear(); // NEW: Clear map as well
        fieldDependencies.clear(); // NEW: Clear dependencies

        // Map for headers to add section fields, to maintain order correctly
        Map<String, ReportHeader> headerMap = new HashMap<>();

        for (TemplateField templateField : template.getFields()) {
            ReportItem item = null;
            if (templateField.getType() == TemplateField.TYPE_FIELD) {
                ReportField field = new ReportField(
                        templateField.getInternalId(),
                        templateField.getDisplayLabel(),
                        templateField.getDefaultValue() != null ? templateField.getDefaultValue() : "",
                        templateField.getInputType(),
                        templateField.isEditable(),
                        templateField.isCustom(),
                        templateField.getCalculationFormula() // NEW: Pass formula
                );
                item = field;
            } else if (templateField.getType() == TemplateField.TYPE_HEADER) {
                ReportHeader header = new ReportHeader(
                        templateField.getInternalId(),
                        templateField.getDisplayLabel(),
                        templateField.isCustom()
                );
                item = header;
                headerMap.put(header.getInternalId(), header);
            } else if (templateField.getType() == TemplateField.TYPE_SECTION_FIELD) {
                ReportHeader parentHeader = headerMap.get(templateField.getParentSectionId());
                if (parentHeader != null) {
                    SectionField sectionField = new SectionField(
                            templateField.getInternalId(),
                            templateField.getDisplayLabel(),
                            templateField.getDefaultValue() != null ? templateField.getDefaultValue() : "",
                            templateField.getInputType(),
                            templateField.isEditable(),
                            templateField.isCustom(),
                            templateField.getParentSectionId(),
                            templateField.getCalculationFormula() // NEW: Pass formula
                    );
                    parentHeader.addSectionField(sectionField); // Add to header's internal list (if needed)
                    item = sectionField; // Add to main list for adapter to display
                } else {
                    Log.e(TAG, "Parent header '" + templateField.getParentSectionId() + "' not found for section field: " + templateField.getDisplayLabel());
                }
            }
            if (item != null) {
                reportItems.add(item);
                reportItemMap.put(item.getInternalId(), item); // NEW: Add to map
            }
        }

        // Set current date for the "date_field" if it exists and is empty
        for (ReportItem item : reportItems) {
            if (item.getInternalId().equals("date_field") && (item.getValue() == null || item.getValue().isEmpty())) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                item.setValue(sdf.format(new Date()));
                break;
            }
        }

        // NEW: Build dependencies AFTER all items are in reportItemMap
        buildFieldDependencies();

        // NEW: Recalculate all calculated fields initially
        for (ReportItem item : reportItems) {
            if (item.isCalculated()) {
                recalculateField(item);
            }
        }

        if (reportAdapter != null) {
            reportAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onRequestFocusNext(int currentAdapterPosition) {
        int nextFocusPosition = -1;
        for (int i = currentAdapterPosition + 1; i < reportItems.size(); i++) {
            ReportItem item = reportItems.get(i);

            // Get input type safely. ReportHeader returns null.
            String inputType = item.getInputType();

            // Skip if it's a multiline text field, as Enter should insert a newline, not move focus.
            // FIX: Check for null on inputType to prevent NullPointerException for ReportHeader.
            if (inputType != null && inputType.equals("textMultiLine")) {
                continue;
            }

            if ((item.getType() == ReportItem.TYPE_FIELD && ((ReportField) item).isEditable()) ||
                (item.getType() == ReportItem.TYPE_SECTION_FIELD && ((SectionField) item).isEditable())) {
                nextFocusPosition = i;
                break;
            }
        }

        if (nextFocusPosition != -1) {
            final int targetPosition = nextFocusPosition;
            fieldsRecyclerView.scrollToPosition(targetPosition);

            handler.postDelayed(() -> {
                RecyclerView.ViewHolder viewHolder = fieldsRecyclerView.findViewHolderForAdapterPosition(targetPosition);
                if (viewHolder != null) {
                    EditText targetEditText = null;
                    if (viewHolder instanceof ReportGeneratorAdapter.FieldViewHolder) {
                        targetEditText = ((ReportGeneratorAdapter.FieldViewHolder) viewHolder).fieldValue;
                    } else if (viewHolder instanceof ReportGeneratorAdapter.SectionFieldViewHolder) {
                        targetEditText = ((ReportGeneratorAdapter.SectionFieldViewHolder) viewHolder).fieldValue;
                    }

                    if (targetEditText != null) {
                        targetEditText.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(targetEditText, InputMethodManager.SHOW_IMPLICIT);
                        }
                        Log.d(TAG, "Requested focus on item at position: " + targetPosition + " label: " + reportItems.get(targetPosition).getDisplayLabel());
                    } else {
                        Log.w(TAG, "EditText not found for ViewHolder at position: " + targetPosition);
                    }
                } else {
                    Log.w(TAG, "ViewHolder not found for position: " + targetPosition + " after delay.");
                }
            }, 100);
        } else {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                View focusedView = getCurrentFocus();
                if (focusedView != null) {
                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                }
            }
            sendButton.requestFocus(); // Now Preview button
            Log.d(TAG, "No more editable fields. Focused on Preview button.");
        }
    }

    // NEW: Implement the new interface method for field value updates
    @Override
    public void onFieldValueUpdated(String internalId, String newValue) {
        // Update the value in the ReportItem directly (it's already done by TextWatcher, but good for clarity)
        ReportItem updatedItem = reportItemMap.get(internalId);
        if (updatedItem != null) {
            updatedItem.setValue(newValue);
            // Trigger recalculations for any fields that depend on this one
            if (fieldDependencies.containsKey(internalId)) {
                for (String dependentFieldId : fieldDependencies.get(internalId)) {
                    ReportItem dependentItem = reportItemMap.get(dependentFieldId);
                    if (dependentItem != null && dependentItem.isCalculated()) {
                        recalculateField(dependentItem);
                    }
                }
            }
        }
    }

    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> saveReport());
        sendButton.setOnClickListener(v -> saveAndPreviewReport()); // Changed to saveAndPreviewReport
    }

    private void saveReport() {
        clearAllFieldErrors();

        // Auto-fill date field if present and empty
        for (ReportItem item : reportItems) {
            if (item.getInternalId().equals("date_field") && item.getValue().isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                item.setValue(sdf.format(new Date()));
                reportAdapter.notifyDataSetChanged();
                break;
            }
        }

        String reportJson = generateReport();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());

        // Use user-defined title if available, otherwise generate a default one
        String title = this.userDefinedTitle != null && !this.userDefinedTitle.isEmpty()
            ? this.userDefinedTitle
            : currentTemplate.getName() + " - " + currentDateTime;

        String previewText = generatePreviewText();

        if (currentReportId == -1) {
            currentReportId = reportDatabase.saveReport(title, currentDateTime, previewText, reportJson, currentTemplate.getTemplateId()); // NEW: Pass templateId
            Log.d(TAG, "New report saved with ID: " + currentReportId + " using template: " + currentTemplate.getTemplateId());
        } else {
            reportDatabase.updateReport(currentReportId, title, currentDateTime, previewText, reportJson, currentTemplate.getTemplateId()); // NEW: Pass templateId
            Log.d(TAG, "Report updated with ID: " + currentReportId + " using template: " + currentTemplate.getTemplateId());
        }

        Toast.makeText(this, "Report saved successfully", Toast.LENGTH_SHORT).show();
    }

    private void autoSaveReport() {
        // Only auto-save if we have a template loaded and fields populated
        if (currentTemplate == null || reportItems.isEmpty()) {
            Log.d(TAG, "Auto-save skipped: Template or fields not loaded.");
            return;
        }

        // Check if any field has a non-empty value (minimal check to avoid saving empty reports)
        boolean hasContent = false;
        for (ReportItem item : reportItems) {
            if ((item.getType() == ReportItem.TYPE_FIELD || item.getType() == ReportItem.TYPE_SECTION_FIELD) &&
                item.getValue() != null && !item.getValue().trim().isEmpty()) {
                hasContent = true;
                break;
            }
        }

        if (!hasContent && currentReportId == -1) {
            Log.d(TAG, "Auto-save skipped: New report with no content.");
            return;
        }

        // Auto-fill date field if present and empty
        for (ReportItem item : reportItems) {
            if (item.getInternalId().equals("date_field") && item.getValue().isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                item.setValue(sdf.format(new Date()));
                break;
            }
        }

        String reportJson = generateReport();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());

        // Use user-defined title if available, otherwise generate a default one
        String title = this.userDefinedTitle != null && !this.userDefinedTitle.isEmpty()
            ? this.userDefinedTitle
            : currentTemplate.getName() + " - " + currentDateTime;

        String previewText = generatePreviewText();

        if (currentReportId == -1) {
            // Save as a new report
            currentReportId = reportDatabase.saveReport(title, currentDateTime, previewText, reportJson, currentTemplate.getTemplateId());
            Log.d(TAG, "Auto-saved new report with ID: " + currentReportId);
        } else {
            // Update existing report
            reportDatabase.updateReport(currentReportId, title, currentDateTime, previewText, reportJson, currentTemplate.getTemplateId());
            Log.d(TAG, "Auto-updated report with ID: " + currentReportId);
        }
    }

    // Renamed from saveAndSendReport to saveAndPreviewReport
    private void saveAndPreviewReport() {
        clearAllFieldErrors();

        List<ReportItem> trulyEmptyFields = new ArrayList<>();
        // Validate and potentially auto-fill date
        for (ReportItem item : reportItems) {
            if (item.getType() == ReportItem.TYPE_FIELD || item.getType() == ReportItem.TYPE_SECTION_FIELD) {
                // Only validate editable fields
                if (((item instanceof ReportField) && ((ReportField) item).isEditable()) ||
                    ((item instanceof SectionField) && ((SectionField) item).isEditable())) {
                    if (item.getValue() == null || item.getValue().trim().isEmpty()) {
                        if (item.getInternalId().equals("date_field")) {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                            item.setValue(sdf.format(new Date()));
                            // No need for notifyDataSetChanged here, as highlightEmptyFields will re-bind
                        } else {
                            // Only force validation for non-optional fields.
                            // The 'additional_notes' field is considered optional, so we skip it here.
                            if (!item.getInternalId().equals("additional_notes")) {
                                trulyEmptyFields.add(item);
                            }
                        }
                    }
                }
            }
        }

        if (!trulyEmptyFields.isEmpty()) {
            highlightEmptyFields(trulyEmptyFields);
            Toast.makeText(this, "Please fill in all highlighted fields", Toast.LENGTH_LONG).show();
            return;
        }

        String reportJson = generateReport();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());

        // Use user-defined title if available, otherwise generate a default one
        String title = this.userDefinedTitle != null && !this.userDefinedTitle.isEmpty()
            ? this.userDefinedTitle
            : currentTemplate.getName() + " - " + currentDateTime;

        String previewText = generatePreviewText();

        if (currentReportId == -1) {
            currentReportId = reportDatabase.saveReport(title, currentDateTime, previewText, reportJson, currentTemplate.getTemplateId());
            Log.d(TAG, "New report saved with ID: " + currentReportId + " using template: " + currentTemplate.getTemplateId());
        } else {
            reportDatabase.updateReport(currentReportId, title, currentDateTime, previewText, reportJson, currentTemplate.getTemplateId());
            Log.d(TAG, "Report updated with ID: " + currentReportId + " using template: " + currentTemplate.getTemplateId());
        }

        String readableReportForPreview = generateReadableReportForSms(); // Use the SMS format for preview

        // NEW: Start ReportPreviewActivity
        Intent intent = new Intent(this, ReportPreviewActivity.class);
        intent.putExtra("REPORT_TEXT", readableReportForPreview);
        startActivity(intent);
        Toast.makeText(this, "Report saved and preview opened", Toast.LENGTH_SHORT).show();
    }

    private void highlightEmptyFields(List<ReportItem> emptyFields) {
        for (ReportItem item : emptyFields) {
            item.setError(true);
            int position = reportItems.indexOf(item);
            if (position != -1) {
                reportAdapter.notifyItemChanged(position);
            }
        }
        if (!emptyFields.isEmpty()) {
            int firstEmptyPos = reportItems.indexOf(emptyFields.get(0));
            if (firstEmptyPos != -1) {
                fieldsRecyclerView.scrollToPosition(firstEmptyPos);
            }
        }
    }

    private void clearAllFieldErrors() {
        boolean changed = false;
        for (int i = 0; i < reportItems.size(); i++) {
            ReportItem item = reportItems.get(i);
            if (item.isError()) {
                item.setError(false);
                changed = true;
            }
        }
        if (changed) {
            reportAdapter.notifyDataSetChanged();
        }
    }

    private String generatePreviewText() {
        Map<String, String> fieldValues = getFieldValuesMap();
        String formatString = currentTemplate.getPreviewFormat();
        return replacePlaceholders(formatString, fieldValues);
    }

    private String generateReport() {
        return gson.toJson(getFieldValuesMap());
    }

    private String generateReadableReportForSms() {
        Map<String, String> fieldValues = getFieldValuesMap();
        String formatString = currentTemplate.getReportFormat();

        // Handle the optional additional_notes line first to ensure it's removed if empty
        String notes = fieldValues.getOrDefault("additional_notes", "").trim();
        
        // Remove the placeholder and its surrounding newline from the format string if notes are empty
        if (notes.isEmpty()) {
            // We use replaceAll to handle potential multi-line notes placeholder if the template format was complex
            formatString = formatString.replaceAll("\\{additional_notes\\}\\n?", "");
        } else {
            // If notes exist, replace the placeholder with the actual notes.
            // The notes can contain newlines from the multi-line EditText.
            formatString = formatString.replace("{additional_notes}", notes);
        }
        
        // Now run the standard replacement for all other placeholders.
        return replacePlaceholders(formatString, fieldValues);
    }

    private Map<String, String> getFieldValuesMap() {
        Map<String, String> fieldValues = new HashMap<>();
        for (ReportItem item : reportItems) {
            if (item.getType() == ReportItem.TYPE_FIELD || item.getType() == ReportItem.TYPE_SECTION_FIELD) {
                fieldValues.put(item.getInternalId(), item.getValue() != null ? item.getValue() : "");
            }
        }
        return fieldValues;
    }

    private String replacePlaceholders(String formatString, Map<String, String> fieldValues) {
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(formatString);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String placeholderKey = matcher.group(1);
            String replacement = fieldValues.getOrDefault(placeholderKey, "0"); // Default to "0" for numeric values in SMS to prevent empty space
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // NEW: Helper method to build dependencies
    private void buildFieldDependencies() {
        fieldDependencies.clear();
        for (ReportItem item : reportItems) {
            if (item.isCalculated()) {
                String formula = item.getCalculationFormula();
                if (formula != null && !formula.isEmpty()) {
                    Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
                    Matcher matcher = pattern.matcher(formula);
                    while (matcher.find()) {
                        String dependentFieldId = matcher.group(1);
                        fieldDependencies.computeIfAbsent(dependentFieldId, k -> new ArrayList<>()).add(item.getInternalId());
                    }
                }
            }
        }
        Log.d(TAG, "Field dependencies built: " + fieldDependencies.toString());
    }

    // NEW: Helper method to recalculate a field
    private void recalculateField(ReportItem calculatedItem) {
        String formula = calculatedItem.getCalculationFormula();

        if (formula == null || formula.isEmpty()) {
            return; // Not a calculated field
        }

        String evaluatedFormula = formula;
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(formula);
        boolean allValuesNumeric = true;

        // Replace placeholders with actual values
        while (matcher.find()) {
            String placeholderKey = matcher.group(1);
            ReportItem dependentItem = reportItemMap.get(placeholderKey);
            String value = (dependentItem != null && dependentItem.getValue() != null && !dependentItem.getValue().isEmpty()) ? dependentItem.getValue() : "0"; // Default to 0 for calculations

            // Check if value is numeric
            if (!value.matches("-?\\d+(\\.\\d+)?")) {
                allValuesNumeric = false;
            }
            evaluatedFormula = evaluatedFormula.replace("{" + placeholderKey + "}", value);
        }

        if (!allValuesNumeric) {
            calculatedItem.setValue("Error"); // Or empty string, or specific error message
            Log.w(TAG, "Non-numeric value encountered in calculation for " + calculatedItem.getInternalId() + ": " + evaluatedFormula);
        } else {
            try {
                // Very basic arithmetic evaluation. For production, consider a robust library.
                double result = evaluateSimpleExpression(evaluatedFormula);
                calculatedItem.setValue(String.format(Locale.getDefault(), "%.2f", result)); // Format to 2 decimal places
            } catch (Exception e) {
                Log.e(TAG, "Error evaluating formula for " + calculatedItem.getInternalId() + ": " + evaluatedFormula, e);
                calculatedItem.setValue("Error");
            }
        }

        int position = reportItems.indexOf(calculatedItem);
        if (position != -1) {
            reportAdapter.notifyItemChanged(position);
        }
    }

    // NEW: Very basic arithmetic expression evaluator (only handles +, -, *, / in left-to-right fashion)
    private double evaluateSimpleExpression(String expression) {
        // This is a highly simplified evaluator. For production, consider a robust library (e.g., exp4j).
        // It processes operations from left to right without respecting operator precedence.
        // Example: "10 + 5 * 2" would be (10+5)*2=30, not 10+(5*2)=20.
        // For more complex expressions, a dedicated math expression parser library is strongly recommended.

        // First, handle multiplication and division
        expression = evaluateMultiplicationDivision(expression);

        // Then, handle addition and subtraction
        String[] parts = expression.split("(?<=[-+])|(?=[-+])"); // Split by + or -, keeping operators
        double result = 0;
        String currentOperator = "+";

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            if (part.equals("+") || part.equals("-")) {
                currentOperator = part;
            } else {
                try {
                    double number = Double.parseDouble(part);
                    switch (currentOperator) {
                        case "+": result += number; break;
                        case "-": result -= number; break;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid number in formula part: " + part, e);
                    throw new IllegalArgumentException("Invalid number in formula part: " + part);
                }
            }
        }
        return result;
    }

    private String evaluateMultiplicationDivision(String expression) {
        Pattern mdPattern = Pattern.compile("(-?\\d+(\\.\\d+)?)\\s*([*/])\\s*(-?\\d+(\\.\\d+)?)");
        Matcher matcher = mdPattern.matcher(expression);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            double operand1 = Double.parseDouble(matcher.group(1));
            String operator = matcher.group(3);
            double operand2 = Double.parseDouble(matcher.group(4));
            double intermediateResult;

            switch (operator) {
                case "*":
                    intermediateResult = operand1 * operand2;
                    break;
                case "/":
                    if (operand2 != 0) {
                        intermediateResult = operand1 / operand2;
                    } else {
                        Log.e(TAG, "Division by zero in formula: " + expression);
                        throw new ArithmeticException("Division by zero");
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected operator: " + operator);
            }
            matcher.appendReplacement(sb, String.valueOf(intermediateResult));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }


    // NEW: Dialog for changing the Report Title
    private void showChangeTitleDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint("Enter Report Title");

        // Pre-fill with current title
        if (userDefinedTitle != null) {
            input.setText(userDefinedTitle);
        } else {
            // Fallback to a default suggestion if title is somehow null
             input.setText(currentTemplate.getName() + " - " + new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(new Date()));
        }

        new AlertDialog.Builder(this)
            .setTitle("Change Report Title")
            .setView(input)
            .setPositiveButton("OK", (dialog, which) -> {
                String newTitle = input.getText().toString().trim();
                if (!newTitle.isEmpty()) {
                    userDefinedTitle = newTitle;
                    updateToolbarTitle(userDefinedTitle);
                    // Auto-save after title change
                    autoSaveReport();
                } else {
                    Toast.makeText(this, "Title cannot be empty.", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}