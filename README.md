# AfterShift Report

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-green.svg)]()

## Overview

An Android application designed for rapid, templated data entry and report generation, primarily for shift reporting, but highly customizable for any structured data collection.

The application allows users to define custom report structures via JSON templates, save reports locally, and generate a clean, formatted text output for easy sharing.

## Features

*   **Customizable Templates:** Import custom JSON templates to change the structure of the report form.
*   **Default Template:** Includes a robust default template for common shift reporting needs.(was for a specific domain i was working on)
*   **Calculated Fields:** Define fields that automatically calculate values based on other input fields (e.g., totals, percentages).
*   **Report Management:** View, edit, and delete saved reports.
*   **Local Persistence:** Saved reports are stored securely in a local SQLite database.
*   **Preview & Share:** Preview the final formatted report and copy or share it directly via SMS or other apps.
*   **Theme Support:** Toggle between Light, Dark, and System Default themes.

## Setup and Installation

### For Users (Installing the App)

1.  Download the latest `app-release.apk` from the [Releases page](https://github.com/brendmung/AfterShift/releases).
2.  Install the APK on your Android device.

### For Developers (Building from Source)

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/tricenc/AfterShift-Report.git
    ```
2.  **Open in Android Studio:**
    Open the cloned directory as an existing Android Studio project.
3.  **Sync and Build:**
    Allow Gradle to sync dependencies. The project should build successfully using the configured Gradle version (`8.0.0`).

## Custom Template Documentation

The core flexibility of AfterShift Report lies in its JSON template system. You can define a new template, save it as a `.json` file, and import it via the app's menu.

### Template Structure (`ReportTemplate.java`)

A template file must be a single JSON object with the following keys:

| Key | Type | Description |
| :--- | :--- | :--- |
| `templateId` | String | **Unique identifier** for the template (e.g., `"my_custom_report"`). |
| `name` | String | Display name shown in the app (e.g., `"Warehouse Daily Report"`). |
| `description` | String | A brief description. |
| `fields` | Array | A list of `TemplateField` objects defining the report structure. |
| `reportFormat` | String | The text format used when sharing/sending the final report (uses placeholders). |
| `previewFormat` | String | A short format string used for the report preview in the main screen (uses placeholders). |

### Field Structure (`TemplateField.java`)

Each object in the `fields` array must contain the following keys:

| Key | Type | Description |
| :--- | :--- | :--- |
| `type` | Integer | Defines the element type: `0` (Field), `1` (Header), `2` (Section Field). |
| `internalId` | String | **Unique ID** for the field/header (used as a placeholder in formats and formulas). |
| `displayLabel` | String | The text label shown next to the input field. |
| `defaultValue` | String | Optional initial value for the field. |
| `inputType` | String | Defines the keyboard type (see table below). |
| `editable` | Boolean | `true` if the user can edit the value (`false` for headers or calculated fields). |
| `isCustom` | Boolean | Reserved for future use (set to `false`). |
| `parentSectionId` | String | **Required only if `type` is 2.** Must match the `internalId` of a preceding Header (`type: 1`). |
| `calculationFormula` | String | **Optional.** A formula using other field IDs in curly braces (e.g., `"{field1} + {field2}"`). **If present, `editable` must be `false`.** |

#### `inputType` Options:

| Value | Description |
| :--- | :--- |
| `"text"` | Standard text input. |
| `"number"` | Numeric input (integers only). |
| `"numberDecimal"` | Numeric input with decimal support (recommended for currency/totals). |
| `"phone"` | Phone number input. |
| `"date"` | Date input. |
| `"textMultiLine"` | Multiline text input (for notes/descriptions). |

### Calculated Fields Syntax

The `calculationFormula` supports basic arithmetic operations (`+`, `-`, `*`, `/`) using field IDs as placeholders.

**Important Note on Calculation:** The built-in evaluator (`ReportGeneratorActivity.java`) is simple. It performs multiplication and division first (left-to-right), followed by addition and subtraction (left-to-right). **It does NOT support parentheses `()` for controlling order of operations.** Keep formulas simple (e.g., `"{a} + {b}"` or `"{a} * {b}"`).

### Example Template JSON

This example shows a simple template with a calculated field for **Net Sales**.

```json
{
  "templateId": "simple_sales_report",
  "name": "Simple Daily Sales",
  "description": "A basic report for gross sales, tax, and net calculation.",
  "fields": [
    {
      "type": 0,
      "internalId": "date_field",
      "displayLabel": "Date",
      "defaultValue": "",
      "inputType": "date",
      "editable": true,
      "isCustom": false,
      "parentSectionId": null,
      "calculationFormula": null
    },
    {
      "type": 0,
      "internalId": "gross_sales",
      "displayLabel": "Gross Sales ($)",
      "defaultValue": "0.00",
      "inputType": "numberDecimal",
      "editable": true,
      "isCustom": false,
      "parentSectionId": null,
      "calculationFormula": null
    },
    {
      "type": 0,
      "internalId": "tax_amount",
      "displayLabel": "Tax Amount ($)",
      "defaultValue": "0.00",
      "inputType": "numberDecimal",
      "editable": true,
      "isCustom": false,
      "parentSectionId": null,
      "calculationFormula": null
    },
    {
      "type": 0,
      "internalId": "net_sales",
      "displayLabel": "Net Sales (Calculated)",
      "defaultValue": "0.00",
      "inputType": "numberDecimal",
      "editable": false,
      "isCustom": false,
      "parentSectionId": null,
      "calculationFormula": "{gross_sales} - {tax_amount}"
    },
    {
      "type": 0,
      "internalId": "notes",
      "displayLabel": "Daily Notes",
      "defaultValue": "",
      "inputType": "textMultiLine",
      "editable": true,
      "isCustom": false,
      "parentSectionId": null,
      "calculationFormula": null
    }
  ],
  "reportFormat": "Daily Sales Report for {date_field}:\nGross: ${gross_sales}\nTax: ${tax_amount}\nNet: ${net_sales}\nNotes: {notes}",
  "previewFormat": "Gross: ${gross_sales} | Net: ${net_sales}"
}
```

### Importing a Template

1.  Create your template file (e.g., `my_report.json`).
2.  In the **MainActivity** (the list of saved reports), tap the **Menu (⋮)** button in the top right.
3.  Select **Import Template**.
4.  Navigate to and select your `.json` file.
5.  Once imported, go back to the menu and select **Set Default Template** to choose your new template for all future reports.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
