package com.tricenc.aftershift;

import java.util.List;

public class ReportTemplate {
    private String templateId; // Unique ID for the template
    private String name;       // Display name for the template
    private String description;
    private List<TemplateField> fields; // List of fields/headers
    private String reportFormat; // String for SMS output, with placeholders
    private String previewFormat; // String for main activity preview, with placeholders

    public ReportTemplate(String templateId, String name, String description, List<TemplateField> fields, String reportFormat, String previewFormat) {
        this.templateId = templateId;
        this.name = name;
        this.description = description;
        this.fields = fields;
        this.reportFormat = reportFormat;
        this.previewFormat = previewFormat;
    }

    // Getters
    public String getTemplateId() { return templateId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<TemplateField> getFields() { return fields; }
    public String getReportFormat() { return reportFormat; }
    public String getPreviewFormat() { return previewFormat; }

    // No setters needed for immutable template data once loaded
}
