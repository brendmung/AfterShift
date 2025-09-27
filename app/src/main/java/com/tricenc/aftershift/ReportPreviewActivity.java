package com.tricenc.aftershift;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat; // For getColor

public class ReportPreviewActivity extends AppCompatActivity {

    private TextView previewTextView;
    private Button copyButton;
    private Button sendButton;
    private Button backButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_preview); // Create this layout

        setupToolbar();
        initializeViews();
        setupClickListeners();

        String reportText = getIntent().getStringExtra("REPORT_TEXT");
        if (reportText != null) {
            previewTextView.setText(reportText);
        } else {
            previewTextView.setText("No report data to display.");
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Report Preview");
            // Ensure back arrow and title color adapt to theme
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.on_surface));
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            // FIX: Use ContextCompat.getColorStateList for setNavigationIconTint
            //toolbar.setNavigationIconTint(ContextCompat.getColorStateList(this, R.color.on_surface));
        }
    }

    private void initializeViews() {
        previewTextView = findViewById(R.id.previewTextView);
        copyButton = findViewById(R.id.copyButton);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupClickListeners() {
        copyButton.setOnClickListener(v -> copyReportText());
        sendButton.setOnClickListener(v -> sendReportText());
        backButton.setOnClickListener(v -> onBackPressed()); // Go back to generator
    }

    private void copyReportText() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Shop Report", previewTextView.getText());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Report copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendReportText() {
        String reportText = previewTextView.getText().toString();
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, reportText);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Shop Report"); // Optional subject

        // Try to open SMS app first, then fallback to chooser
        Intent smsIntent = new Intent(Intent.ACTION_VIEW);
        smsIntent.setData(android.net.Uri.parse("sms:"));
        smsIntent.putExtra("sms_body", reportText);

        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            // If SMS app fails, try general share intent
            try {
                startActivity(Intent.createChooser(sendIntent, "Send Report via..."));
            } catch (Exception ex) {
                Toast.makeText(this, "No messaging app available", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}