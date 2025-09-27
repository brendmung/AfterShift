package com.tricenc.aftershift;

// This mirrors the ReportItem structure but for defining the template,
// not the actual runtime UI element.
public class TemplateField {
    public static final int TYPE_FIELD = 0;
    public static final int TYPE_HEADER = 1;
    public static final int TYPE_SECTION_FIELD = 2;

    private int type;
    private String internalId;
    private String displayLabel;
    private String defaultValue; // Optional default value for new reports
    private String inputType;    // NEW: e.g., "text", "numberDecimal", "number", "phone"
    private boolean editable;
    private boolean isCustom; // Whether this field can be removed/edited by user (not used for this feature, but good to keep)
    private String parentSectionId; // For TYPE_SECTION_FIELD
    private String calculationFormula; // NEW: Formula for calculated fields, e.g., "{field1} + {field2}"

    public TemplateField(int type, String internalId, String displayLabel, String defaultValue, String inputType, boolean editable, boolean isCustom, String parentSectionId, String calculationFormula) {
        this.type = type;
        this.internalId = internalId;
        this.displayLabel = displayLabel;
        this.defaultValue = defaultValue;
        this.inputType = inputType != null ? inputType : "text"; // Default to text
        this.editable = editable;
        this.isCustom = isCustom;
        this.parentSectionId = parentSectionId;
        this.calculationFormula = calculationFormula;

        // If a calculation formula is provided, the field is implicitly not editable
        if (this.calculationFormula != null && !this.calculationFormula.isEmpty()) {
            this.editable = false;
        }
    }

    // Constructor for non-section fields (TYPE_FIELD, TYPE_HEADER) without formula
    public TemplateField(int type, String internalId, String displayLabel, String defaultValue, String inputType, boolean editable, boolean isCustom) {
        this(type, internalId, displayLabel, defaultValue, inputType, editable, isCustom, null, null);
    }

    // Constructor for section fields (TYPE_SECTION_FIELD) without formula
    public TemplateField(int type, String internalId, String displayLabel, String defaultValue, String inputType, boolean editable, boolean isCustom, String parentSectionId) {
        this(type, internalId, displayLabel, defaultValue, inputType, editable, isCustom, parentSectionId, null);
    }

    // Getters
    public int getType() { return type; }
    public String getInternalId() { return internalId; }
    public String getDisplayLabel() { return displayLabel; }
    public String getDefaultValue() { return defaultValue; }
    public String getInputType() { return inputType; }
    public boolean isEditable() { return editable; }
    public boolean isCustom() { return isCustom; }
    public String getParentSectionId() { return parentSectionId; }
    public String getCalculationFormula() { return calculationFormula; }
    public boolean isCalculated() { return calculationFormula != null && !calculationFormula.isEmpty(); }
}
