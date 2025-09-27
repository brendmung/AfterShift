package com.tricenc.aftershift;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReportGeneratorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ReportGeneratorActivity.ReportItem> reportItems;
    private Context context;
    private OnItemInteractionListener listener;

    public interface OnItemInteractionListener {
        void onRequestFocusNext(int currentAdapterPosition);
        void onFieldValueUpdated(String internalId, String newValue); // NEW
    }

    public ReportGeneratorAdapter(Context context, List<ReportGeneratorActivity.ReportItem> reportItems, OnItemInteractionListener listener) {
        this.context = context;
        this.reportItems = reportItems;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return reportItems.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ReportGeneratorActivity.ReportItem.TYPE_FIELD) {
            View view = inflater.inflate(R.layout.generator_field_item, parent, false);
            return new FieldViewHolder(view);
        } else if (viewType == ReportGeneratorActivity.ReportItem.TYPE_SECTION_FIELD) {
            View view = inflater.inflate(R.layout.generator_section_field_item, parent, false);
            return new SectionFieldViewHolder(view);
        } else { // TYPE_HEADER
            View view = inflater.inflate(R.layout.generator_header_item, parent, false);
            return new HeaderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ReportGeneratorActivity.ReportItem item = reportItems.get(position);

        if (holder.getItemViewType() == ReportGeneratorActivity.ReportItem.TYPE_FIELD) {
            FieldViewHolder fieldHolder = (FieldViewHolder) holder;
            ReportGeneratorActivity.ReportField field = (ReportGeneratorActivity.ReportField) item;
            bindFieldViewHolder(fieldHolder, field, position);

        } else if (holder.getItemViewType() == ReportGeneratorActivity.ReportItem.TYPE_SECTION_FIELD) {
            SectionFieldViewHolder sectionFieldHolder = (SectionFieldViewHolder) holder;
            ReportGeneratorActivity.SectionField sectionField = (ReportGeneratorActivity.SectionField) item;
            bindSectionFieldViewHolder(sectionFieldHolder, sectionField, position);

        } else { // TYPE_HEADER
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            ReportGeneratorActivity.ReportHeader header = (ReportGeneratorActivity.ReportHeader) item;
            bindHeaderViewHolder(headerHolder, header);
        }
    }

    private void bindFieldViewHolder(FieldViewHolder fieldHolder, ReportGeneratorActivity.ReportField field, int position) {
        fieldHolder.fieldLabel.setText(field.getDisplayLabel());

        if (fieldHolder.fieldValue.getTag() instanceof TextWatcher) {
            fieldHolder.fieldValue.removeTextChangedListener((TextWatcher) fieldHolder.fieldValue.getTag());
        }

        fieldHolder.fieldValue.setText(field.getValue());
        fieldHolder.fieldValue.setEnabled(field.isEditable()); // Use the isEditable() getter

        int inputType = mapInputTypeStringToInt(field.getInputType());
        fieldHolder.fieldValue.setInputType(inputType);

        // Set IME options based on input type
        if ((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == InputType.TYPE_TEXT_FLAG_MULTI_LINE) {
            fieldHolder.fieldValue.setSingleLine(false);
            fieldHolder.fieldValue.setImeOptions(EditorInfo.IME_ACTION_NONE); // Enter key inserts newline
            fieldHolder.fieldValue.setMinLines(2); // Set minimum lines to 2 for multi-line
            fieldHolder.fieldValue.setMaxLines(5); // Optional: set a max line limit
        } else {
            fieldHolder.fieldValue.setSingleLine(true);
            fieldHolder.fieldValue.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            fieldHolder.fieldValue.setMinLines(1);
            fieldHolder.fieldValue.setMaxLines(1);
        }
        
        fieldHolder.fieldValue.setOnEditorActionListener((v, actionId, event) -> {
            // Only handle IME_ACTION_NEXT for single-line fields
            if (fieldHolder.fieldValue.isSingleLine() && actionId == EditorInfo.IME_ACTION_NEXT) {
                if (listener != null) {
                    listener.onRequestFocusNext(position);
                }
                return true;
            }
            // Allow Enter key to insert newline in multi-line fields
            return false;
        });

        // Apply error background if item is in error state, otherwise normal background
        if (field.isError()) {
            fieldHolder.fieldValue.setBackgroundResource(R.drawable.rounded_edit_text_error_background);
        } else {
            // Use different background for non-editable (calculated) fields
            if (!field.isEditable()) {
                fieldHolder.fieldValue.setBackgroundResource(R.drawable.rounded_edit_text_disabled_background); // NEW drawable needed
            } else {
                fieldHolder.fieldValue.setBackgroundResource(R.drawable.rounded_edit_text_background);
            }
        }

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                field.setValue(s.toString());
                // If field was in error and now has text, clear error and revert background
                if (field.isError() && !s.toString().trim().isEmpty()) {
                    field.setError(false);
                    fieldHolder.fieldValue.setBackgroundResource(field.isEditable() ? R.drawable.rounded_edit_text_background : R.drawable.rounded_edit_text_disabled_background);
                }
                // NEW: Notify activity for potential recalculations, only if field is editable
                if (listener != null && field.isEditable()) {
                    listener.onFieldValueUpdated(field.getInternalId(), s.toString());
                }
            }
        };
        fieldHolder.fieldValue.addTextChangedListener(textWatcher);
        fieldHolder.fieldValue.setTag(textWatcher);
    }

    private void bindSectionFieldViewHolder(SectionFieldViewHolder sectionFieldHolder, ReportGeneratorActivity.SectionField sectionField, int position) {
        sectionFieldHolder.fieldLabel.setText(sectionField.getDisplayLabel());

        if (sectionFieldHolder.fieldValue.getTag() instanceof TextWatcher) {
            sectionFieldHolder.fieldValue.removeTextChangedListener((TextWatcher) sectionFieldHolder.fieldValue.getTag());
        }

        sectionFieldHolder.fieldValue.setText(sectionField.getValue());
        sectionFieldHolder.fieldValue.setEnabled(sectionField.isEditable()); // Use the isEditable() getter

        int inputType = mapInputTypeStringToInt(sectionField.getInputType());
        sectionFieldHolder.fieldValue.setInputType(inputType);

        // Section fields are typically single line, but we'll apply the same logic just in case
        if ((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == InputType.TYPE_TEXT_FLAG_MULTI_LINE) {
            sectionFieldHolder.fieldValue.setSingleLine(false);
            sectionFieldHolder.fieldValue.setImeOptions(EditorInfo.IME_ACTION_NONE); // Enter key inserts newline
            sectionFieldHolder.fieldValue.setMinLines(2);
            sectionFieldHolder.fieldValue.setMaxLines(5);
        } else {
            sectionFieldHolder.fieldValue.setSingleLine(true);
            sectionFieldHolder.fieldValue.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            sectionFieldHolder.fieldValue.setMinLines(1);
            sectionFieldHolder.fieldValue.setMaxLines(1);
        }

        sectionFieldHolder.fieldValue.setOnEditorActionListener((v, actionId, event) -> {
            // Only handle IME_ACTION_NEXT for single-line fields
            if (sectionFieldHolder.fieldValue.isSingleLine() && actionId == EditorInfo.IME_ACTION_NEXT) {
                if (listener != null) {
                    listener.onRequestFocusNext(position);
                }
                return true;
            }
            // Allow Enter key to insert newline in multi-line fields
            return false;
        });

        // Apply error background if item is in error state, otherwise normal background
        if (sectionField.isError()) {
            sectionFieldHolder.fieldValue.setBackgroundResource(R.drawable.rounded_edit_text_error_background);
        } else {
            // Use different background for non-editable (calculated) fields
            if (!sectionField.isEditable()) {
                sectionFieldHolder.fieldValue.setBackgroundResource(R.drawable.rounded_edit_text_disabled_background); // NEW drawable needed
            } else {
                sectionFieldHolder.fieldValue.setBackgroundResource(R.drawable.rounded_edit_text_background);
            }
        }

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                sectionField.setValue(s.toString());
                // If field was in error and now has text, clear error and revert background
                if (sectionField.isError() && !s.toString().trim().isEmpty()) {
                    sectionField.setError(false);
                    sectionFieldHolder.fieldValue.setBackgroundResource(sectionField.isEditable() ? R.drawable.rounded_edit_text_background : R.drawable.rounded_edit_text_disabled_background);
                }
                // NEW: Notify activity for potential recalculations, only if field is editable
                if (listener != null && sectionField.isEditable()) {
                    listener.onFieldValueUpdated(sectionField.getInternalId(), s.toString());
                }
            }
        };
        sectionFieldHolder.fieldValue.addTextChangedListener(textWatcher);
        sectionFieldHolder.fieldValue.setTag(textWatcher);
    }

    private void bindHeaderViewHolder(HeaderViewHolder headerHolder, ReportGeneratorActivity.ReportHeader header) {
        headerHolder.headerText.setText(header.getDisplayLabel());
    }

    // NEW: Helper to map string input type to Android InputType constants
    private int mapInputTypeStringToInt(String inputTypeString) {
        if (inputTypeString == null) {
            return InputType.TYPE_CLASS_TEXT;
        }
        switch (inputTypeString) {
            case "number":
                return InputType.TYPE_CLASS_NUMBER;
            case "numberDecimal":
                return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
            case "numberSigned":
                return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
            case "phone":
                return InputType.TYPE_CLASS_PHONE;
            case "date":
                return InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE;
            case "time":
                return InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME;
            case "password":
                return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
            case "email":
                return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
            case "textMultiLine": // NEW
                return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
            // Add more as needed
            case "text":
            default:
                return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        }
    }

    @Override
    public int getItemCount() {
        return reportItems.size();
    }

    public void highlightField(int position) {
        // The isError flag is set on the ReportItem itself in ReportGeneratorActivity.highlightEmptyFields
        notifyItemChanged(position); // This will cause onBindViewHolder to be called for the item
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof FieldViewHolder) {
            FieldViewHolder fieldHolder = (FieldViewHolder) holder;
            if (fieldHolder.fieldValue.getTag() instanceof TextWatcher) {
                fieldHolder.fieldValue.removeTextChangedListener((TextWatcher) fieldHolder.fieldValue.getTag());
                fieldHolder.fieldValue.setTag(null);
            }
            fieldHolder.fieldValue.setOnEditorActionListener(null);
        } else if (holder instanceof SectionFieldViewHolder) {
            SectionFieldViewHolder sectionFieldHolder = (SectionFieldViewHolder) holder;
            if (sectionFieldHolder.fieldValue.getTag() instanceof TextWatcher) {
                sectionFieldHolder.fieldValue.removeTextChangedListener((TextWatcher) sectionFieldHolder.fieldValue.getTag());
                sectionFieldHolder.fieldValue.setTag(null);
            }
            sectionFieldHolder.fieldValue.setOnEditorActionListener(null);
        }
    }

    public static class FieldViewHolder extends RecyclerView.ViewHolder {
        TextView fieldLabel;
        EditText fieldValue;

        public FieldViewHolder(@NonNull View itemView) {
            super(itemView);
            fieldLabel = itemView.findViewById(R.id.fieldLabel);
            fieldValue = itemView.findViewById(R.id.fieldValue);
        }
    }

    public static class SectionFieldViewHolder extends RecyclerView.ViewHolder {
        TextView fieldLabel;
        EditText fieldValue;

        public SectionFieldViewHolder(@NonNull View itemView) {
            super(itemView);
            fieldLabel = itemView.findViewById(R.id.fieldLabel);
            fieldValue = itemView.findViewById(R.id.fieldValue);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
        }
    }
}