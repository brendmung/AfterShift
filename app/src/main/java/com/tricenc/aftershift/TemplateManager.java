package com.tricenc.aftershift;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate; // NEW

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateManager {
    private static final String TAG = "TemplateManager";
    private static final String PREFS_NAME = "app_preferences"; // Changed to general app preferences
    private static final String KEY_CURRENT_TEMPLATE_ID = "current_template_id"; // This is the DEFAULT template for new reports
    private static final String KEY_THEME_MODE = "theme_mode"; // NEW: For theme preference
    private static final String TEMPLATE_DIR = "templates"; // Directory for user-imported templates

    private static TemplateManager instance;
    private final Context context;
    private final Gson gson;
    private final SharedPreferences prefs;

    // Map to hold all loaded templates, by ID
    private Map<String, ReportTemplate> availableTemplates;

    private TemplateManager(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        this.gson = new Gson();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.availableTemplates = new HashMap<>();
        loadAllTemplates();
    }

    public static synchronized TemplateManager getInstance(Context context) {
        if (instance == null) {
            instance = new TemplateManager(context);
        }
        return instance;
    }

    private void loadAllTemplates() {
        availableTemplates.clear(); // Clear existing to ensure fresh load
        // Always add the default template first
        availableTemplates.put(DEFAULT_TEMPLATE_ID, createDefaultTemplate());

        // Load user-imported templates from internal storage
        File templateDir = new File(context.getFilesDir(), TEMPLATE_DIR);
        if (templateDir.exists() && templateDir.isDirectory()) {
            File[] templateFiles = templateDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (templateFiles != null) {
                for (File file : templateFiles) {
                    try {
                        String json = readFile(file);
                        ReportTemplate template = parseTemplateJson(json);
                        if (template != null) {
                            availableTemplates.put(template.getTemplateId(), template);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading imported template file: " + file.getName(), e);
                    }
                }
            }
        }
        Log.d(TAG, "Loaded " + availableTemplates.size() + " templates.");
    }

    // This method creates a template from the original hardcoded fields.
    // It will be the "default" template.
    private ReportTemplate createDefaultTemplate() {
        List<TemplateField> fields = new ArrayList<>();

        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "date_field", "Date", "", "text", true, false));
        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "apex_$", "Apex $", "", "numberDecimal", true, false));
        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "in_$", "In $", "", "numberDecimal", true, false));
        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "out_$", "Out $", "", "numberDecimal", true, false));
        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "airtime_$", "Airtime $", "", "numberDecimal", true, false));

        fields.add(new TemplateField(TemplateField.TYPE_HEADER, "header_fv", "Fruit and veggies", null, "text", false, false));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "r_fv", "R", "", "numberDecimal", true, false, "header_fv"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "p_fv", "P", "", "numberDecimal", true, false, "header_fv"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "us_fv", "US", "", "numberDecimal", true, false, "header_fv"));

        fields.add(new TemplateField(TemplateField.TYPE_HEADER, "header_fvt", "Fruit and veggies totals", null, "text", false, false));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "r_fvt", "R", "", "numberDecimal", true, false, "header_fvt"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "p_fvt", "P", "", "numberDecimal", true, false, "header_fvt"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "us_fvt", "US", "", "numberDecimal", true, false, "header_fvt"));

        fields.add(new TemplateField(TemplateField.TYPE_HEADER, "header_float", "Float", null, "text", false, false));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "r_float", "R", "", "numberDecimal", true, false, "header_float"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "us_float", "US", "", "numberDecimal", true, false, "header_float"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "apex_$_float", "Apex $", "", "numberDecimal", true, false, "header_float"));

        fields.add(new TemplateField(TemplateField.TYPE_HEADER, "header_na", "New Atronics", null, "text", false, false));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "r_na", "R", "", "numberDecimal", true, false, "header_na"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "mtd_r_na", "MTD R", "", "numberDecimal", true, false, "header_na"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "us_na", "US", "", "numberDecimal", true, false, "header_na"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "mtd_us_na", "MTD US", "", "numberDecimal", true, false, "header_na"));

        fields.add(new TemplateField(TemplateField.TYPE_HEADER, "header_mg", "Mixed Grain", null, "text", false, false));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "r_mg", "R", "", "numberDecimal", true, false, "header_mg"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "mtd_r_mg", "MTD R", "", "numberDecimal", true, false, "header_mg"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "us_mg", "US", "", "numberDecimal", true, false, "header_mg"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "mtd_us_mg", "MTD US", "", "numberDecimal", true, false, "header_mg"));

        fields.add(new TemplateField(TemplateField.TYPE_HEADER, "header_chunks", "Chunks", null, "text", false, false));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "r_chunks", "R", "", "numberDecimal", true, false, "header_chunks"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "mtd_r_chunks", "MTD R", "", "numberDecimal", true, false, "header_chunks"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "us_chunks", "US", "", "numberDecimal", true, false, "header_chunks"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "mtd_us_chunks", "MTD US", "", "numberDecimal", true, false, "header_chunks"));

        fields.add(new TemplateField(TemplateField.TYPE_HEADER, "header_jumbo", "Jumbo", null, "text", false, false));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "r_jumbo", "R", "", "numberDecimal", true, false, "header_jumbo"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "mtd_r_jumbo", "MTD R", "", "numberDecimal", true, false, "header_jumbo"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "us_jumbo", "US", "", "numberDecimal", true, false, "header_jumbo"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "mtd_us_jumbo", "MTD US", "", "numberDecimal", true, false, "header_jumbo"));

        fields.add(new TemplateField(TemplateField.TYPE_HEADER, "header_unyawuthi", "Unyawuthi", null, "text", false, false));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "r_unyawuthi", "R", "", "numberDecimal", true, false, "header_unyawuthi"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "mtd_r_unyawuthi", "MTD R", "", "numberDecimal", true, false, "header_unyawuthi"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "us_unyawuthi", "US", "", "numberDecimal", true, false, "header_unyawuthi"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "mtd_us_unyawuthi", "MTD US", "", "numberDecimal", true, false, "header_unyawuthi"));

        // MODIFIED: apex_took and admiral_took inputType set to "text" (free-form)
        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "apex_took", "Apex took", "", "text", true, false));
        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "admiral_took", "Admiral took", "", "text", true, false));

        // NEW: additional_notes field for free-form, non-structured daily events, with multi-line input
        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "additional_notes", "Additional Notes", "", "textMultiLine", true, false));

        fields.add(new TemplateField(TemplateField.TYPE_HEADER, "header_bar", "Bar", null, "text", false, false));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "r_bar", "R", "", "numberDecimal", true, false, "header_bar"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "total_r_bar", "Total R", "", "numberDecimal", true, false, "header_bar"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "us_bar", "US", "", "numberDecimal", true, false, "header_bar"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "total_us_bar", "Total US", "", "numberDecimal", true, false, "header_bar"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "p_bar", "P", "", "numberDecimal", true, false, "header_bar"));
        fields.add(new TemplateField(TemplateField.TYPE_SECTION_FIELD, "total_p_bar", "Total P", "", "numberDecimal", true, false, "header_bar"));

        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "admiral_$", "Admiral $", "", "numberDecimal", true, false));
        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "mtd_$_admiral", "MTD $", "", "numberDecimal", true, false));
        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "cash_$", "Cash $", "", "numberDecimal", true, false));
        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "apex_cash_$", "Apex Cash $", "", "numberDecimal", true, false));
        fields.add(new TemplateField(TemplateField.TYPE_FIELD, "mtd_$_apex_cash", "MTD $", "", "numberDecimal", true, false));

        // Default preview format
        String previewFormat = "Admiral: {admiral_$} | MTD: {mtd_$_admiral} | Cash: {cash_$}";

        // Default SMS format (original format from generateReadableReportForSms)
        // Note: Placeholders should exactly match internalIds
        String smsFormat = "{date_field}\n" +
                "Apex ${apex_$}   In ${in_$}   Out ${out_$}\n" +
                "Airtime ${airtime_$}\n" +
                "Fruit and veggies\n" +
                " R{r_fv}.     P{p_fv}.      US{us_fv}.\n" +
                "Fruit and veggies totals\n" +
                " R{r_fvt}.     P{p_fvt}.     US{us_fvt}.\n" +
                "Float     R{r_float}.   US{us_float}.  Apex ${apex_$_float}\n" +
                "New Atronics\n" +
                " R{r_na}.     MTD R{mtd_r_na}.\n" +
                " US{us_na}.    MTD US{mtd_us_na}.\n" +
                "Mixed Grain\n" +
                " R{r_mg}.     MTD R{mtd_r_mg}.\n" +
                " US{us_mg}.    MTD US{mtd_us_mg}.\n" +
                "Chunks\n" +
                " R{r_chunks}.     MTD R{mtd_r_chunks}.\n" +
                " US{us_chunks}.    MTD US{mtd_us_chunks}.\n" +
                "Jumbo\n" +
                " R{r_jumbo}.     MTD R{mtd_r_jumbo}.\n" +
                " US{us_jumbo}.    MTD US{mtd_us_jumbo}.\n" +
                "Unyawuthi\n" +
                " R{r_unyawuthi}.     MTD R{mtd_r_unyawuthi}.\n" +
                " US{us_unyawuthi}.   MTD US{mtd_us_unyawuthi}.\n" +
                "Apex took {apex_took}\n" + 
                "Admiral took {admiral_took}\n" + 
                // Placeholder for optional notes. We will remove this line entirely if the field is empty in ReportGeneratorActivity.
                "{additional_notes}\n" +
                "Bar\n" +
                " R{r_bar}.   Total R{total_r_bar}.\n" +
                " US{us_bar}.   Total US{total_us_bar}.\n" +
                " P{p_bar}.     Total P{total_p_bar}.\n" +
                "Admiral ${admiral_$}    MTD ${mtd_$_admiral}    Cash ${cash_$}\n" +
                "Apex Cash ${apex_cash_$}    MTD ${mtd_$_apex_cash}\n";

        return new ReportTemplate(DEFAULT_TEMPLATE_ID, "Report", "The original report structure", fields, smsFormat, previewFormat);
    }

    public static final String DEFAULT_TEMPLATE_ID = "default_shop_report";

    public List<ReportTemplate> getAvailableTemplates() {
        return new ArrayList<>(availableTemplates.values());
    }

    public ReportTemplate getTemplate(String templateId) {
        return availableTemplates.get(templateId);
    }

    // This method now saves the *default template for new reports*
    public void saveCurrentTemplateId(String templateId) {
        prefs.edit().putString(KEY_CURRENT_TEMPLATE_ID, templateId).apply();
    }

    // This method retrieves the *default template for new reports*
    public String getCurrentTemplateId() {
        return prefs.getString(KEY_CURRENT_TEMPLATE_ID, DEFAULT_TEMPLATE_ID);
    }

    // This method retrieves the *default template for new reports*
    public ReportTemplate getCurrentTemplate() {
        return availableTemplates.get(getCurrentTemplateId());
    }

    /**
     * Imports a new template from an InputStream, parses it, and saves it to internal storage.
     * If a template with the same ID already exists, it will be overwritten.
     * After import, it reloads all templates to ensure the new one is available.
     *
     * @param inputStream The input stream of the JSON template file.
     * @param originalFilename The original filename (used for logging).
     * @throws IOException If there's an error reading or writing the file.
     */
    public void importTemplate(InputStream inputStream, String originalFilename) throws IOException {
        String json = readStream(inputStream);
        ReportTemplate template = parseTemplateJson(json);
        if (template != null) {
            if (availableTemplates.containsKey(template.getTemplateId())) {
                Log.w(TAG, "Template with ID " + template.getTemplateId() + " already exists. Overwriting.");
            }
            // Save to internal storage
            saveTemplateToFile(template);
            // Reload all templates to include the new one
            loadAllTemplates();
            Log.d(TAG, "Template imported and reloaded: " + template.getName());
        } else {
            throw new IOException("Failed to parse template from file: " + originalFilename);
        }
    }

    private ReportTemplate parseTemplateJson(String json) {
        try {
            return gson.fromJson(json, ReportTemplate.class);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing template JSON: " + json, e);
            return null;
        }
    }

    private void saveTemplateToFile(ReportTemplate template) throws IOException {
        File templateDir = new File(context.getFilesDir(), TEMPLATE_DIR);
        if (!templateDir.exists()) {
            templateDir.mkdirs();
        }

        String fileName = template.getTemplateId() + ".json"; // Use templateId as filename for uniqueness
        File file = new File(templateDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(gson.toJson(template).getBytes());
            Log.d(TAG, "Template saved to " + file.getAbsolutePath());
        }
    }

    private String readFile(File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String readStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        reader.close();
        return sb.toString();
    }

    // NEW: Delete a template file from internal storage and reload available templates
    public void deleteTemplate(String templateId) throws IOException, IllegalArgumentException {
        if (templateId.equals(DEFAULT_TEMPLATE_ID)) {
            throw new IllegalArgumentException("Cannot delete the built-in default template.");
        }

        File templateDir = new File(context.getFilesDir(), TEMPLATE_DIR);
        String fileName = templateId + ".json";
        File file = new File(templateDir, fileName);

        if (file.exists()) {
            if (file.delete()) {
                Log.d(TAG, "Template file deleted: " + file.getAbsolutePath());
                // If the deleted template was the current default for new reports, reset it
                if (getCurrentTemplateId().equals(templateId)) {
                    saveCurrentTemplateId(DEFAULT_TEMPLATE_ID);
                }
                loadAllTemplates(); // Reload all templates to update the in-memory map
            } else {
                throw new IOException("Failed to delete template file: " + file.getAbsolutePath());
            }
        } else {
            throw new IOException("Template file not found: " + file.getAbsolutePath());
        }
    }

    // NEW: Theme preference management
    public void saveThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); // Default to system setting
    }
}