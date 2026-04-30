package com.example.focusmodejv;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.focusmodejv.model.SettingsItem;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements SettingsAdapter.OnSettingsClickListener {

    private RecyclerView rvSettings;
    private SettingsAdapter adapter;
    private List<SettingsItem> settingsItems;

    private ActivityResultLauncher<Intent> ringtoneLauncher;
    private ActivityResultLauncher<Intent> documentLauncher;
    private Uri selectedSoundUri;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyBrightness();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupLaunchers();

        rvSettings = findViewById(R.id.rvSettings);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPremium).setOnClickListener(v -> {
            Intent intent = new Intent(this, PremiumActivity.class);
            startActivity(intent);
        });

        setupSettingsList();
        
        adapter = new SettingsAdapter(this, settingsItems, this);
        rvSettings.setLayoutManager(new LinearLayoutManager(this));
        rvSettings.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh DND toggle state if permission was granted/denied while away
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            boolean hasDnd = nm.isNotificationPolicyAccessGranted();
            boolean prefDnd = getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("pref_dnd_mode", false);
            if (prefDnd && !hasDnd) {
                // If user revoked permission in settings, update preference
                getSharedPreferences("Settings", MODE_PRIVATE).edit().putBoolean("pref_dnd_mode", false).apply();
                recreate();
            }
        }
    }

    private void setupSettingsList() {
        settingsItems = new ArrayList<>();

        // Section: TOOLS
        settingsItems.add(new SettingsItem(getString(R.string.section_tools)));
        settingsItems.add(new SettingsItem(getString(R.string.pref_floating_clock), getString(R.string.pref_floating_clock_desc), 0, SettingsItem.Type.TOGGLE, "pref_floating_clock"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_widget), getString(R.string.pref_widget_desc), 0, SettingsItem.Type.NAVIGATE, "pref_widget"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_wallpaper), getString(R.string.pref_wallpaper_desc), 0, SettingsItem.Type.NAVIGATE, "pref_wallpaper", true));
        settingsItems.add(new SettingsItem(getString(R.string.pref_standby), getString(R.string.pref_standby_desc), 0, SettingsItem.Type.TOGGLE, "pref_standby"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_aod), getString(R.string.pref_aod_desc), 0, SettingsItem.Type.TOGGLE, "pref_aod"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_reminder), getString(R.string.pref_reminder_desc), 0, SettingsItem.Type.TOGGLE, "pref_reminder"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_quick_start), getString(R.string.pref_quick_start_desc), 0, SettingsItem.Type.TOGGLE, "pref_quick_start"));

        // Section: PERSONALIZE
        settingsItems.add(new SettingsItem(getString(R.string.section_personalize)));
        settingsItems.add(new SettingsItem(getString(R.string.pref_themes), getString(R.string.pref_themes_desc), 0, SettingsItem.Type.NAVIGATE, "pref_themes"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_accent_color), getString(R.string.pref_accent_color_desc), 0, SettingsItem.Type.NAVIGATE, "pref_accent_color", true));
        settingsItems.add(new SettingsItem(getString(R.string.pref_brightness), getString(R.string.pref_brightness_desc), 0, SettingsItem.Type.DIALOG, "pref_brightness"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_language), getString(R.string.pref_language_desc), 0, SettingsItem.Type.NAVIGATE, "pref_language"));

        // Section: GENERAL
        settingsItems.add(new SettingsItem(getString(R.string.section_general)));
        settingsItems.add(new SettingsItem(getString(R.string.pref_default_timer), getString(R.string.pref_default_timer_desc), 0, SettingsItem.Type.DIALOG, "pref_default_timer"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_default_break), getString(R.string.pref_default_break_desc), 0, SettingsItem.Type.DIALOG, "pref_default_break"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_auto_start), getString(R.string.pref_auto_start_desc), 0, SettingsItem.Type.TOGGLE, "pref_auto_start"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_vibration), getString(R.string.pref_vibration_desc), 0, SettingsItem.Type.TOGGLE, "pref_vibration"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_keep_screen_on), getString(R.string.pref_keep_screen_on_desc), 0, SettingsItem.Type.TOGGLE, "pref_keep_screen_on"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_timer_sound), getString(R.string.pref_timer_sound_desc), 0, SettingsItem.Type.DIALOG, "pref_timer_sound"));
        settingsItems.add(new SettingsItem(getString(R.string.pref_dnd_mode), getString(R.string.pref_dnd_mode_desc), 0, SettingsItem.Type.TOGGLE, "pref_dnd_mode"));

        // Section: PRIVACY
        settingsItems.add(new SettingsItem(getString(R.string.section_privacy)));
        SettingsItem storageInfo = new SettingsItem("Data Storage Info", "All data is stored locally on your device", 0, SettingsItem.Type.INFO, "pref_storage_info");
        storageInfo.setInfoValue("");
        settingsItems.add(storageInfo);
        settingsItems.add(new SettingsItem(getString(R.string.pref_reset_data), getString(R.string.pref_reset_data_desc), 0, SettingsItem.Type.ACTION, "pref_reset_data"));
        settingsItems.add(new SettingsItem("Analytics", "Allow anonymous usage data", 0, SettingsItem.Type.TOGGLE, "pref_analytics"));

        // Section: ABOUT
        settingsItems.add(new SettingsItem(getString(R.string.section_about)));
        String versionName = "1.0.0";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) { e.printStackTrace(); }
        
        SettingsItem versionItem = new SettingsItem("App Version", versionName, 0, SettingsItem.Type.INFO, "pref_version");
        versionItem.setInfoValue(versionName);
        settingsItems.add(versionItem);
        settingsItems.add(new SettingsItem("Share App", "Share the app with others", 0, SettingsItem.Type.ACTION, "pref_share"));
        settingsItems.add(new SettingsItem("Contact Support", "Send feedback or report issues", 0, SettingsItem.Type.ACTION, "pref_contact"));
        settingsItems.add(new SettingsItem("Rate App", "Leave a review on Play Store", 0, SettingsItem.Type.ACTION, "pref_rate"));
        settingsItems.add(new SettingsItem("Privacy Policy", "View privacy policy details", 0, SettingsItem.Type.NAVIGATE, "pref_privacy_policy"));
    }

    @Override
    public void onItemClick(SettingsItem item) {
        // Always redirect premium items to PremiumActivity for now
        if (item.isPremium()) {
            Intent intent = new Intent(this, PremiumActivity.class);
            startActivity(intent);
            return;
        }

        switch (item.getKey()) {
            case "pref_share":
                shareApp();
                break;
            case "pref_reset_data":
                showResetConfirmation();
                break;
            case "pref_floating_clock":
                checkOverlayPermission();
                break;
            case "pref_contact":
                contactSupport();
                break;
            case "pref_rate":
                rateApp();
                break;
            case "pref_widget":
                openFullScreenSetting(item);
                break;
            case "pref_wallpaper":
                // Clock Wallpaper is now handled by ConfigurationActivity
                openFullScreenSetting(item);
                break;
            case "pref_themes":
                Intent themeIntent = new Intent(this, ClockThemeActivity.class);
                startActivity(themeIntent);
                break;
            case "pref_accent_color":
                Intent accentIntent = new Intent(this, AccentColorActivity.class);
                startActivity(accentIntent);
                break;
            case "pref_language":
                Intent langIntent = new Intent(this, LanguageActivity.class);
                startActivity(langIntent);
                break;
            case "pref_brightness":
                BrightnessBottomSheet brightnessBottomSheet = new BrightnessBottomSheet();
                brightnessBottomSheet.show(getSupportFragmentManager(), "BrightnessBottomSheet");
                break;
            case "pref_storage_info":
                showDataStorageInfo();
                break;
            case "pref_privacy_policy":
                openPrivacyPolicy();
                break;
            case "pref_default_timer":
                getSharedPreferences("TimerPrefs", MODE_PRIVATE).edit()
                        .putInt("default_minutes", 45)
                        .apply();
                Toast.makeText(this, "Default Timer set to 45 min", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("focus_time", 45);
                setResult(RESULT_OK, resultIntent);
                finish(); 
                break;
            case "pref_default_break":
                getSharedPreferences("TimerPrefs", MODE_PRIVATE).edit()
                        .putInt("break_minutes", 5)
                        .apply();
                Toast.makeText(this, "The break duration 5 min is set", Toast.LENGTH_SHORT).show();
                break;
            case "pref_timer_sound":
                showSoundSelector();
                break;
            default:
                if (item.getType() == SettingsItem.Type.ACTION) {
                    // Handled by specific cases above if they have keys
                } else if (item.getType() == SettingsItem.Type.NAVIGATE) {
                    // Handled by specific cases above
                }
                break;
        }
    }

    private void contactSupport() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:support@yourapp.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "App Support");
        intent.putExtra(Intent.EXTRA_TEXT, "Describe your issue...");
        try {
            startActivity(intent);
            Toast.makeText(this, "Opening email...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void rateApp() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(intent);
            Toast.makeText(this, "Opening Play Store...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void openFullScreenSetting(SettingsItem item) {
        Intent intent = new Intent(this, ConfigurationActivity.class);
        intent.putExtra("setting_title", item.getTitle());
        intent.putExtra("setting_key", item.getKey());
        startActivity(intent);
    }

    private void showDialogPlaceholder(SettingsItem item) {
        // Not used anymore as all items have real implementations
    }

    private void showDataStorageInfo() {
        new AlertDialog.Builder(this)
            .setTitle("Data Storage Info")
            .setMessage("• All session data is stored locally using SQLite.\n" +
                        "• Your preferences are saved using SharedPreferences.\n" +
                        "• No data is synced to the cloud or shared externally.")
            .setPositiveButton("Close", null)
            .show();
    }

    private void openPrivacyPolicy() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://yourapp.com/privacy"));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onToggleClick(SettingsItem item, boolean isChecked) {
        if (item.isPremium()) {
            boolean isPremium = getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("is_premium", false);
            if (!isPremium) {
                // Revert the toggle if not premium
                getSharedPreferences("Settings", MODE_PRIVATE).edit().putBoolean(item.getKey(), !isChecked).apply();
                Intent intent = new Intent(this, PremiumActivity.class);
                startActivity(intent);
                recreate(); // Refresh UI to show correct toggle state
                return;
            }
        }

        if (item.getKey().equals("pref_floating_clock") && isChecked) {
            if (!Settings.canDrawOverlays(this)) {
                checkOverlayPermission();
            }
        }

        if (item.getKey().equals("pref_dnd_mode") && isChecked) {
            checkDndPermission();
        }
        Toast.makeText(this, item.getTitle() + (isChecked ? " Enabled" : " Disabled"), Toast.LENGTH_SHORT).show();
    }

    private void checkDndPermission() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null && !notificationManager.isNotificationPolicyAccessGranted()) {
            new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Do Not Disturb Mode requires permission to silence notifications.")
                .setPositiveButton("Grant", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Revert the toggle if permission is denied/cancelled
                    getSharedPreferences("Settings", MODE_PRIVATE).edit().putBoolean("pref_dnd_mode", false).apply();
                    recreate();
                })
                .show();
        }
    }

    private void checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Floating Clock requires permission to display over other apps.")
                .setPositiveButton("Grant", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    private void showResetConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Reset all data?")
            .setMessage("This will permanently delete all sessions and settings. This action cannot be undone.")
            .setPositiveButton("Reset", (dialog, which) -> {
                // Clear Database
                new com.example.focusmodejv.data.DatabaseHelper(this).clearAllSessions();
                
                // Clear SharedPreferences
                getSharedPreferences("Settings", MODE_PRIVATE).edit().clear().apply();
                getSharedPreferences("TimerPrefs", MODE_PRIVATE).edit().clear().apply();
                
                Toast.makeText(SettingsActivity.this, "All data has been reset", Toast.LENGTH_SHORT).show();
                
                // Restart App or Recreate Activity to reset UI
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void shareApp() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out this Focus Timer app!");
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share via"));
    }

    private void applyBrightness() {
        android.content.SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        float brightness = prefs.getFloat("pref_brightness", -1f);
        if (brightness != -1f) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = brightness;
            getWindow().setAttributes(layoutParams);
        }
    }

    private void setupLaunchers() {
        ringtoneLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        if (uri != null) {
                            Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                            String name = ringtone != null ? ringtone.getTitle(this) : "System Sound";
                            saveSoundSelection(uri, name);
                        }
                    }
                }
        );

        documentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (SecurityException e) { e.printStackTrace(); }
                            String name = getFileName(uri);
                            saveSoundSelection(uri, name);
                        }
                    }
                }
        );
    }

    private void showSoundSelector() {
        String[] options = {"Default Sounds", "Choose from Device"};
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Select Timer Sound")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM | RingtoneManager.TYPE_NOTIFICATION);
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound");
                        ringtoneLauncher.launch(intent);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("audio/*");
                        documentLauncher.launch(intent);
                    }
                })
                .show();
    }

    private void saveSoundSelection(Uri uri, String name) {
        getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE).edit()
                .putString("sound_uri", uri.toString())
                .putString("sound_name", name)
                .apply();
        Toast.makeText(this, "Sound saved: " + name, Toast.LENGTH_SHORT).show();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result != null ? result : "Custom Audio";
    }
}
