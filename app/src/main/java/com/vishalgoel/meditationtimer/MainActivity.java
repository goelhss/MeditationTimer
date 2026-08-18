package com.vishalgoel.meditationtimer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

@SuppressLint("SetTextI18n")
public final class MainActivity extends Activity {
    public static final String EXTRA_OPEN_TAB = "open_tab";
    public static final String TAB_TIMER = "timer";
    private static final String TAB_LOGS = "logs";
    private static final String TAB_REMINDER = "reminder";
    private static final String TAB_ABOUT = "about";
    private static final String SETTINGS_PREFS = "timer_settings";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 401;
    private static final float DIM_BRIGHTNESS = 0.08f;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> selectedLogIds = new HashSet<>();
    private LinearLayout root;
    private LinearLayout content;
    private Button timerTab;
    private Button logsTab;
    private Button reminderTab;
    private Button aboutTab;
    private TextView countdownView;
    private TextView activeStatusView;
    private String selectedTab = TAB_TIMER;
    private boolean mainUiShown;
    private boolean receiverRegistered;
    private String renderedStateKey = "";
    private AlertDialog pendingDialog;
    private String pendingDialogId = "";
    private ToneDingPlayer previewPlayer;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshForStateChange();
            showPendingLogPromptIfNeeded();
        }
    };

    private final Runnable uiTicker = new Runnable() {
        @Override
        public void run() {
            if (!mainUiShown) {
                return;
            }
            TimerState state = new TimerStateStore(MainActivity.this).load();
            String key = state.active + ":" + state.paused + ":" + state.startWallMs;
            if (!key.equals(renderedStateKey) && TAB_TIMER.equals(selectedTab)) {
                renderSelectedTab();
            } else if (state.active && countdownView != null) {
                countdownView.setText(MeditationTimerService.formatCountdown(
                        state.remainingMs(SystemClock.elapsedRealtime())));
                activeStatusView.setText(state.paused ? "Paused" : "Meditating");
                applyScreenMode(state);
            }
            new PendingMeditationStore(MainActivity.this)
                    .applyDefaultIfDue(System.currentTimeMillis());
            showPendingLogPromptIfNeeded();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectRequestedTab(getIntent());
        previewPlayer = new ToneDingPlayer(this, handler);
        if (savedInstanceState == null) {
            setContentView(LotusSplashView.create(this, handler, this::showMainUi));
        } else {
            showMainUi();
        }
    }

    private void selectRequestedTab(Intent intent) {
        if (intent == null) {
            return;
        }
        String requested = intent.getStringExtra(EXTRA_OPEN_TAB);
        if (TAB_TIMER.equals(requested) || TAB_LOGS.equals(requested)
                || TAB_REMINDER.equals(requested) || TAB_ABOUT.equals(requested)) {
            selectedTab = requested;
        }
    }

    private void showMainUi() {
        mainUiShown = true;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(currentColorTheme().backgroundColor());

        TextView title = new TextView(this);
        title.setText("Meditation Timer");
        title.setTextSize(25);
        title.setTextColor(primaryTextColor());
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(dp(16), dp(18), dp(16), dp(12));
        root.addView(title, matchWrap());

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(8), 0, dp(8), dp(8));
        timerTab = tabButton("Timer", TAB_TIMER);
        logsTab = tabButton("Logs", TAB_LOGS);
        reminderTab = tabButton("Reminder", TAB_REMINDER);
        aboutTab = tabButton("About", TAB_ABOUT);
        tabs.addView(timerTab, weighted());
        tabs.addView(logsTab, weighted());
        tabs.addView(reminderTab, weighted());
        tabs.addView(aboutTab, weighted());
        root.addView(tabs, matchWrap());

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        renderSelectedTab();
        showWhatsNewIfNeeded();
    }

    private Button tabButton(String label, String tab) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setMinHeight(dp(48));
        button.setOnClickListener(view -> {
            selectedTab = tab;
            renderSelectedTab();
        });
        return button;
    }

    private void renderSelectedTab() {
        if (!mainUiShown) {
            return;
        }
        content.removeAllViews();
        countdownView = null;
        activeStatusView = null;
        updateTabStyles();
        if (TAB_LOGS.equals(selectedTab)) {
            renderLogs();
        } else if (TAB_REMINDER.equals(selectedTab)) {
            renderReminder();
        } else if (TAB_ABOUT.equals(selectedTab)) {
            renderAbout();
        } else {
            renderTimer();
        }
    }

    private void updateTabStyles() {
        styleTab(timerTab, TAB_TIMER.equals(selectedTab));
        styleTab(logsTab, TAB_LOGS.equals(selectedTab));
        styleTab(reminderTab, TAB_REMINDER.equals(selectedTab));
        styleTab(aboutTab, TAB_ABOUT.equals(selectedTab));
    }

    private void styleTab(Button button, boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(14));
        background.setColor(selected ? Color.rgb(56, 108, 176) : Color.rgb(238, 243, 248));
        background.setStroke(dp(selected ? 2 : 1),
                selected ? Color.rgb(22, 58, 107) : Color.rgb(190, 202, 215));
        button.setBackground(background);
        button.setTextColor(selected ? Color.WHITE : Color.rgb(38, 53, 68));
    }

    private void renderTimer() {
        TimerState state = new TimerStateStore(this).load();
        renderedStateKey = state.active + ":" + state.paused + ":" + state.startWallMs;
        if (state.active) {
            renderActiveTimer(state);
        } else {
            restoreScreenMode();
            renderTimerSetup();
        }
    }

    private void renderTimerSetup() {
        SharedPreferences settings = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
        LinearLayout form = pageColumn();
        form.addView(sectionTitle("Set your meditation"), matchWrap());

        EditText duration = numberField(settings.getInt("duration", 60));
        EditText primary = numberField(settings.getInt("primary", 5));
        EditText additional = numberField(settings.getInt("additional", 10));
        EditText finish = numberField(settings.getInt("finish", 10));
        form.addView(labeledField("Meditation duration (minutes)", duration));
        form.addView(labeledField("One ding every (minutes)", primary));
        form.addView(labeledField("One additional ding every (minutes)", additional));
        form.addView(labeledField("Dings when finished", finish));

        CheckBox chimes = optionCheckBox("Chimes", settings.getBoolean("chimes", true));
        CheckBox vibrate = optionCheckBox("Vibrate", settings.getBoolean("vibrate", false));
        android.widget.CompoundButton.OnCheckedChangeListener cueModeListener = (button, checked) -> {
            if (!chimes.isChecked() && !vibrate.isChecked()) {
                button.setChecked(true);
                Toast.makeText(this, "Keep Chimes, Vibrate, or both enabled.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        chimes.setOnCheckedChangeListener(cueModeListener);
        vibrate.setOnCheckedChangeListener(cueModeListener);
        form.addView(chimes, matchWrap());
        form.addView(vibrate, matchWrap());

        CheckBox dim = new CheckBox(this);
        dim.setText("Dim screen while countdown is visible");
        dim.setTextSize(16);
        dim.setTextColor(primaryTextColor());
        dim.setChecked(settings.getBoolean("dim", true));
        dim.setPadding(0, dp(10), 0, dp(10));
        form.addView(dim, matchWrap());

        Button preview = actionButton("Preview cue", false);
        preview.setOnClickListener(view -> previewPlayer.play(1,
                chimes.isChecked(), vibrate.isChecked()));
        form.addView(preview, fullButtonParams());

        Button start = actionButton("▶ Start", true);
        start.setTextSize(20);
        start.setOnClickListener(view -> {
            try {
                int durationValue = parsePositive(duration);
                int primaryValue = parsePositive(primary);
                int additionalValue = parsePositive(additional);
                int finishValue = parsePositive(finish);
                new TimerSchedule(durationValue, primaryValue, additionalValue, finishValue);
                if (!chimes.isChecked() && !vibrate.isChecked()) {
                    throw new IllegalArgumentException("Enable Chimes, Vibrate, or both.");
                }
                settings.edit()
                        .putInt("duration", durationValue)
                        .putInt("primary", primaryValue)
                        .putInt("additional", additionalValue)
                        .putInt("finish", finishValue)
                        .putBoolean("chimes", chimes.isChecked())
                        .putBoolean("vibrate", vibrate.isChecked())
                        .putBoolean("dim", dim.isChecked())
                        .apply();
                requestNotificationPermissionIfNeeded();
                Intent service = new Intent(this, MeditationTimerService.class)
                        .setAction(MeditationTimerService.ACTION_START)
                        .putExtra(MeditationTimerService.EXTRA_DURATION_MINUTES, durationValue)
                        .putExtra(MeditationTimerService.EXTRA_PRIMARY_MINUTES, primaryValue)
                        .putExtra(MeditationTimerService.EXTRA_ADDITIONAL_MINUTES, additionalValue)
                        .putExtra(MeditationTimerService.EXTRA_FINISH_DINGS, finishValue)
                        .putExtra(MeditationTimerService.EXTRA_CHIMES_ENABLED,
                                chimes.isChecked())
                        .putExtra(MeditationTimerService.EXTRA_VIBRATION_ENABLED,
                                vibrate.isChecked())
                        .putExtra(MeditationTimerService.EXTRA_DIM_SCREEN, dim.isChecked());
                startForegroundService(service);
                promptExactAlarmAccessIfNeeded();
                handler.postDelayed(this::refreshForStateChange, 120L);
            } catch (IllegalArgumentException error) {
                new AlertDialog.Builder(this)
                        .setTitle("Check timer values")
                        .setMessage(error.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
        LinearLayout.LayoutParams startParams = fullButtonParams();
        startParams.topMargin = dp(14);
        form.addView(start, startParams);
        content.addView(scroll(form), fill());
    }

    private void renderActiveTimer(TimerState state) {
        LinearLayout page = pageColumn();
        activeStatusView = new TextView(this);
        activeStatusView.setText(state.paused ? "Paused" : "Meditating");
        activeStatusView.setTextSize(20);
        activeStatusView.setTextColor(accentTextColor());
        activeStatusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        activeStatusView.setGravity(Gravity.CENTER);
        page.addView(activeStatusView, matchWrap());

        countdownView = new TextView(this);
        countdownView.setText(MeditationTimerService.formatCountdown(
                state.remainingMs(SystemClock.elapsedRealtime())));
        countdownView.setTextSize(58);
        countdownView.setTextColor(Color.rgb(22, 58, 107));
        countdownView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        countdownView.setGravity(Gravity.CENTER);
        countdownView.setPadding(dp(8), dp(42), dp(8), dp(42));
        GradientDrawable clockBackground = new GradientDrawable();
        clockBackground.setCornerRadius(dp(28));
        clockBackground.setColor(Color.rgb(221, 238, 255));
        clockBackground.setStroke(dp(2), Color.rgb(56, 108, 176));
        countdownView.setBackground(clockBackground);
        LinearLayout.LayoutParams clockParams = fullButtonParams();
        clockParams.topMargin = dp(18);
        clockParams.bottomMargin = dp(18);
        page.addView(countdownView, clockParams);

        TextView detail = bodyText("Primary ding every " + state.primaryMs / TimerSchedule.MINUTE_MS
                + " min · additional ding every " + state.additionalMs / TimerSchedule.MINUTE_MS
                + " min\nCompletion: " + state.finishDings + " dings"
                + " · " + cueModeLabel(state.chimesEnabled, state.vibrationEnabled)
                + (state.dimScreen ? " · screen dimming on" : ""));
        detail.setGravity(Gravity.CENTER);
        page.addView(detail, matchWrap());

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        Button pauseResume = actionButton(state.paused ? "▶ Resume" : "Ⅱ Pause", true);
        pauseResume.setOnClickListener(view -> sendTimerAction(
                state.paused ? MeditationTimerService.ACTION_RESUME
                        : MeditationTimerService.ACTION_PAUSE));
        Button restart = actionButton("↻ Restart", false);
        restart.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle("Restart meditation?")
                .setMessage("The countdown and elapsed meditation time will restart from zero.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Restart", (dialog, which) ->
                        sendTimerAction(MeditationTimerService.ACTION_RESTART))
                .show());
        controls.addView(pauseResume, weighted());
        controls.addView(restart, weighted());
        page.addView(controls, matchWrap());

        Button end = actionButton("■ End", false);
        end.setTextColor(Color.rgb(179, 38, 30));
        end.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle("End meditation?")
                .setMessage("Your actual active meditation time can still be logged.")
                .setNegativeButton("Continue", null)
                .setPositiveButton("End", (dialog, which) ->
                        sendTimerAction(MeditationTimerService.ACTION_END))
                .show());
        LinearLayout.LayoutParams endParams = fullButtonParams();
        endParams.topMargin = dp(14);
        page.addView(end, endParams);

        applyScreenMode(state);
        content.addView(scroll(page), fill());
    }

    private void renderLogs() {
        restoreScreenMode();
        LinearLayout page = pageColumn();
        page.addView(sectionTitle("Meditation Logs"), matchWrap());
        List<MeditationLog> logs = new MeditationLogStore(this).all();
        Set<String> available = new HashSet<>();
        for (MeditationLog log : logs) {
            available.add(log.id());
        }
        selectedLogIds.retainAll(available);

        if (logs.isEmpty()) {
            TextView empty = bodyText("No meditation sessions have been logged yet.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(8), dp(40), dp(8), dp(40));
            page.addView(empty, matchWrap());
        } else {
            CheckBox selectAll = new CheckBox(this);
            selectAll.setText("Select all " + logs.size() + " entries");
            selectAll.setTextColor(primaryTextColor());
            selectAll.setChecked(selectedLogIds.size() == logs.size());
            selectAll.setOnCheckedChangeListener((button, checked) -> {
                if (checked) {
                    for (MeditationLog log : logs) {
                        selectedLogIds.add(log.id());
                    }
                } else if (selectedLogIds.size() == logs.size()) {
                    selectedLogIds.clear();
                }
                renderSelectedTab();
            });
            page.addView(selectAll, matchWrap());
            SimpleDateFormat format = new SimpleDateFormat("EEE, MMM d, yyyy · h:mm a", Locale.US);
            for (MeditationLog log : logs) {
                CheckBox row = new CheckBox(this);
                row.setText(format.format(new Date(log.startTimeMs())) + "\n"
                        + LogTextExporter.formatDuration(log.durationMs()));
                row.setTextSize(16);
                row.setTextColor(Color.rgb(38, 53, 68));
                row.setChecked(selectedLogIds.contains(log.id()));
                row.setPadding(dp(8), dp(12), dp(8), dp(12));
                row.setBackground(cardBackground(Color.WHITE));
                row.setOnCheckedChangeListener((button, checked) -> {
                    if (checked) {
                        selectedLogIds.add(log.id());
                    } else {
                        selectedLogIds.remove(log.id());
                    }
                });
                LinearLayout.LayoutParams rowParams = fullButtonParams();
                rowParams.bottomMargin = dp(8);
                page.addView(row, rowParams);
            }
        }

        Button share = actionButton(selectedLogIds.isEmpty()
                ? "Share all as text file" : "Share selected as text file", true);
        share.setEnabled(!logs.isEmpty());
        share.setOnClickListener(view -> shareLogs(logs));
        page.addView(share, fullButtonParams());

        Button deleteSelected = actionButton("Delete selected", false);
        deleteSelected.setEnabled(!selectedLogIds.isEmpty());
        deleteSelected.setTextColor(Color.rgb(179, 38, 30));
        deleteSelected.setOnClickListener(view -> confirmDeleteSelected());
        LinearLayout.LayoutParams deleteParams = fullButtonParams();
        deleteParams.topMargin = dp(10);
        page.addView(deleteSelected, deleteParams);

        Button deleteAll = actionButton("Delete all entries", false);
        deleteAll.setEnabled(!logs.isEmpty());
        deleteAll.setTextColor(Color.rgb(179, 38, 30));
        deleteAll.setOnClickListener(view -> confirmDeleteAll());
        LinearLayout.LayoutParams allParams = fullButtonParams();
        allParams.topMargin = dp(8);
        page.addView(deleteAll, allParams);
        content.addView(scroll(page), fill());
    }

    private void renderReminder() {
        restoreScreenMode();
        ReminderSchedule saved = new ReminderStore(this).load();
        LinearLayout page = pageColumn();
        page.addView(sectionTitle("Meditation Reminder"), matchWrap());
        page.addView(bodyText("Choose when Android should gently remind you to meditate."),
                matchWrap());

        CheckBox enabled = optionCheckBox("Remind me to meditate", saved.enabled());
        LinearLayout.LayoutParams enabledParams = matchWrap();
        enabledParams.topMargin = dp(12);
        page.addView(enabled, enabledParams);

        ReminderSchedule.Frequency[] frequencies = ReminderSchedule.Frequency.values();
        List<String> frequencyLabels = new ArrayList<>();
        for (ReminderSchedule.Frequency frequency : frequencies) {
            frequencyLabels.add(frequency.label());
        }
        ArrayAdapter<String> frequencyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, frequencyLabels);
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner frequency = new Spinner(this);
        frequency.setAdapter(frequencyAdapter);
        frequency.setSelection(saved.frequency().ordinal(), false);
        page.addView(labeledControl("How often", frequency));

        final int[] selectedHour = {saved.hour()};
        final int[] selectedMinute = {saved.minute()};
        Button time = actionButton(formatClockTime(saved.hour(), saved.minute()), false);
        time.setOnClickListener(view -> new TimePickerDialog(this, (picker, hour, minute) -> {
            selectedHour[0] = hour;
            selectedMinute[0] = minute;
            time.setText(formatClockTime(hour, minute));
        }, selectedHour[0], selectedMinute[0], DateFormat.is24HourFormat(this)).show());
        page.addView(labeledControl("When", time));

        LinearLayout selectedDays = new LinearLayout(this);
        selectedDays.setOrientation(LinearLayout.VERTICAL);
        TextView daysLabel = bodyText("Selected days");
        daysLabel.setTextColor(accentTextColor());
        selectedDays.addView(daysLabel, matchWrap());
        int[] calendarDays = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY};
        String[] dayLabels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        CheckBox[] dayChecks = new CheckBox[calendarDays.length];
        LinearLayout firstDays = new LinearLayout(this);
        firstDays.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout secondDays = new LinearLayout(this);
        secondDays.setOrientation(LinearLayout.HORIZONTAL);
        for (int index = 0; index < calendarDays.length; index += 1) {
            CheckBox day = optionCheckBox(dayLabels[index],
                    (saved.customDaysMask() & ReminderSchedule.dayBit(calendarDays[index])) != 0);
            day.setTextSize(14);
            dayChecks[index] = day;
            (index < 4 ? firstDays : secondDays).addView(day, weighted());
        }
        selectedDays.addView(firstDays, matchWrap());
        selectedDays.addView(secondDays, matchWrap());
        LinearLayout.LayoutParams daysParams = matchWrap();
        daysParams.topMargin = dp(10);
        page.addView(selectedDays, daysParams);

        Runnable updateControls = () -> {
            boolean active = enabled.isChecked();
            frequency.setEnabled(active);
            time.setEnabled(active);
            boolean custom = active && frequency.getSelectedItemPosition()
                    == ReminderSchedule.Frequency.CUSTOM.ordinal();
            selectedDays.setVisibility(custom ? View.VISIBLE : View.GONE);
            for (CheckBox day : dayChecks) {
                day.setEnabled(custom);
            }
        };
        enabled.setOnCheckedChangeListener((button, checked) -> updateControls.run());
        frequency.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                       int position, long id) {
                updateControls.run();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        updateControls.run();

        TextView next = bodyText(saved.enabled()
                ? "Next reminder: " + formatReminderDate(saved.nextTriggerAfter(
                        System.currentTimeMillis(), TimeZone.getDefault()))
                : "Reminders are currently off.");
        next.setPadding(0, dp(18), 0, dp(10));
        page.addView(next, matchWrap());

        Button save = actionButton("Save reminder", true);
        save.setOnClickListener(view -> {
            int customMask = 0;
            for (int index = 0; index < calendarDays.length; index += 1) {
                if (dayChecks[index].isChecked()) {
                    customMask |= ReminderSchedule.dayBit(calendarDays[index]);
                }
            }
            try {
                ReminderSchedule schedule = new ReminderSchedule(enabled.isChecked(),
                        frequencies[frequency.getSelectedItemPosition()], selectedHour[0],
                        selectedMinute[0], customMask);
                new ReminderStore(this).save(schedule);
                new ReminderScheduler(this).apply(schedule);
                if (schedule.enabled()) {
                    requestNotificationPermissionIfNeeded();
                    promptExactAlarmAccessIfNeeded();
                    Toast.makeText(this, "Reminder saved for "
                                    + formatReminderDate(schedule.nextTriggerAfter(
                                            System.currentTimeMillis(), TimeZone.getDefault())),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Meditation reminder turned off.",
                            Toast.LENGTH_SHORT).show();
                }
                renderSelectedTab();
            } catch (IllegalArgumentException error) {
                new AlertDialog.Builder(this)
                        .setTitle("Check reminder")
                        .setMessage(error.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
        page.addView(save, fullButtonParams());
        content.addView(scroll(page), fill());
    }

    private void shareLogs(List<MeditationLog> allLogs) {
        List<MeditationLog> selected = new ArrayList<>();
        if (selectedLogIds.isEmpty()) {
            selected.addAll(allLogs);
        } else {
            for (MeditationLog log : allLogs) {
                if (selectedLogIds.contains(log.id())) {
                    selected.add(log);
                }
            }
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        shareTextFile("meditation-logs-" + timestamp + ".txt",
                LogTextExporter.export(selected), "Share Meditation Logs");
    }

    private void confirmDeleteSelected() {
        if (selectedLogIds.isEmpty()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete selected entries?")
                .setMessage("This permanently deletes " + selectedLogIds.size() + " selected entries.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    new MeditationLogStore(this).delete(selectedLogIds);
                    selectedLogIds.clear();
                    renderSelectedTab();
                }).show();
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
                .setTitle("Delete all meditation logs?")
                .setMessage("This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete all", (dialog, which) -> {
                    new MeditationLogStore(this).deleteAll();
                    selectedLogIds.clear();
                    renderSelectedTab();
                }).show();
    }

    private void renderAbout() {
        restoreScreenMode();
        LinearLayout page = pageColumn();
        page.addView(sectionTitle("About"), matchWrap());
        page.addView(bodyText("Meditation Timer\nVersion " + BuildConfig.VERSION_NAME
                + " · build " + BuildConfig.VERSION_CODE
                + "\nAuthor: Vishal Goel\nLicense: MIT"), matchWrap());

        page.addView(subsectionTitle("Appearance"), matchWrap());
        LinearLayout appearanceRow = new LinearLayout(this);
        appearanceRow.setOrientation(LinearLayout.HORIZONTAL);
        appearanceRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView appearanceLabel = bodyText("Background color");
        appearanceRow.addView(appearanceLabel, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Spinner appearance = colorThemeSpinner();
        appearanceRow.addView(appearance, new LinearLayout.LayoutParams(dp(158),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(appearanceRow, matchWrap());

        Button update = actionButton("Check for Updates", true);
        update.setOnClickListener(view -> openPlayStore());
        LinearLayout.LayoutParams updateParams = fullButtonParams();
        updateParams.topMargin = dp(14);
        page.addView(update, updateParams);

        page.addView(subsectionTitle("Background status"), matchWrap());
        TimerState state = new TimerStateStore(this).load();
        ReminderSchedule reminder = new ReminderStore(this).load();
        page.addView(bodyText("Timer: " + (state.active
                        ? (state.paused ? "paused" : "running") : "inactive")
                + "\nReminder: " + (reminder.enabled()
                        ? reminder.frequency().label() + " at "
                                + formatClockTime(reminder.hour(), reminder.minute())
                        : "off")
                + "\nNotifications: " + (notificationsAllowed() ? "allowed" : "not allowed")
                + "\nExact alarms: " + (exactAlarmsAllowed() ? "allowed" : "not allowed")
                + "\nThe timer uses an ongoing notification and never forces itself over the lock screen."),
                matchWrap());

        page.addView(subsectionTitle("Diagnostics"), matchWrap());
        DiagnosticsStore diagnostics = new DiagnosticsStore(this);
        page.addView(bodyText(diagnostics.summary()), matchWrap());
        Button shareDiagnostics = actionButton("Share Debug logs", false);
        shareDiagnostics.setOnClickListener(view -> shareTextFile(
                "meditation-timer-v" + BuildConfig.VERSION_CODE + "-diagnostics.txt",
                diagnostics.export(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"),
                "Share Meditation Timer Diagnostics"));
        page.addView(shareDiagnostics, fullButtonParams());
        Button clearDiagnostics = actionButton("Clear Debug logs", false);
        clearDiagnostics.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle("Clear diagnostics?")
                .setMessage("This clears timer event and timing statistics only. Meditation logs are not affected.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (dialog, which) -> {
                    diagnostics.clear();
                    renderSelectedTab();
                }).show());
        LinearLayout.LayoutParams clearParams = fullButtonParams();
        clearParams.topMargin = dp(8);
        page.addView(clearDiagnostics, clearParams);

        page.addView(subsectionTitle("Version history"), matchWrap());
        page.addView(bodyText("1.1.0 · August 18, 2026\n"
                + "Large lotus launch screen, chime/vibration modes, and configurable meditation reminders.\n\n"
                + "1.0.0 · August 18, 2026\n"
                + "Timer, overlapping interval dings, screen-locked operation, completion prompt, logs, sharing, and diagnostics."),
                matchWrap());

        Button license = actionButton("View MIT License", false);
        license.setOnClickListener(view -> showLicense());
        LinearLayout.LayoutParams licenseParams = fullButtonParams();
        licenseParams.topMargin = dp(14);
        page.addView(license, licenseParams);
        content.addView(scroll(page), fill());
    }

    private void showWhatsNewIfNeeded() {
        SharedPreferences preferences = getSharedPreferences("app_version", MODE_PRIVATE);
        if (preferences.getInt("last_whats_new", 0) >= BuildConfig.VERSION_CODE) {
            return;
        }
        preferences.edit().putInt("last_whats_new", BuildConfig.VERSION_CODE).apply();
        new AlertDialog.Builder(this)
                .setTitle("What’s new in 1.1.0")
                .setMessage("• Large lotus launch screen with no countdown\n"
                        + "• Chimes, vibration, or both for every cue\n"
                        + "• Meditation Reminder tab with daily, weekday, weekend, or selected-day schedules\n"
                        + "• Reminder notifications open the Timer tab")
                .setPositiveButton("Continue", null)
                .show();
    }

    private void showLicense() {
        TextView text = bodyText("MIT License\n\nCopyright © 2026 Vishal Goel\n\n"
                + "Permission is hereby granted, free of charge, to any person obtaining a copy "
                + "of this software and associated documentation files, to deal in the Software "
                + "without restriction, subject to including the copyright and permission notice.\n\n"
                + "THE SOFTWARE IS PROVIDED ‘AS IS’, WITHOUT WARRANTY OF ANY KIND.");
        text.setPadding(dp(20), dp(12), dp(20), dp(12));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(text);
        new AlertDialog.Builder(this)
                .setTitle("MIT License")
                .setView(scroll)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showPendingLogPromptIfNeeded() {
        if (!mainUiShown) {
            return;
        }
        PendingMeditationStore.Pending pending = new PendingMeditationStore(this).get();
        if (pending == null) {
            if (pendingDialog != null) {
                pendingDialog.dismiss();
            }
            pendingDialog = null;
            pendingDialogId = "";
            return;
        }
        if (pending.id().equals(pendingDialogId) && pendingDialog != null
                && pendingDialog.isShowing()) {
            updatePendingMessage(pending);
            return;
        }
        pendingDialogId = pending.id();
        pendingDialog = new AlertDialog.Builder(this)
                .setTitle("Log this meditation?")
                .setMessage(pendingMessage(pending))
                .setPositiveButton("Yes, log it", (dialog, which) ->
                        sendTimerAction(MeditationTimerService.ACTION_LOG_YES))
                .setNegativeButton("No", (dialog, which) ->
                        sendTimerAction(MeditationTimerService.ACTION_LOG_NO))
                .create();
        pendingDialog.setOnDismissListener(dialog -> pendingDialogId = "");
        pendingDialog.show();
    }

    private void updatePendingMessage(PendingMeditationStore.Pending pending) {
        if (pendingDialog != null) {
            pendingDialog.setMessage(pendingMessage(pending));
        }
    }

    private String pendingMessage(PendingMeditationStore.Pending pending) {
        long seconds = CompletionDecisionPolicy.secondsRemaining(
                pending.decisionDeadlineMs(), System.currentTimeMillis());
        return "Active meditation: " + LogTextExporter.formatDuration(pending.durationMs())
                + "\n\nYes will be selected automatically in " + seconds + " seconds.";
    }

    private void sendTimerAction(String action) {
        startForegroundService(new Intent(this, MeditationTimerService.class).setAction(action));
        handler.postDelayed(this::refreshForStateChange, 120L);
    }

    private void refreshForStateChange() {
        renderedStateKey = "";
        if (TAB_TIMER.equals(selectedTab) || TAB_LOGS.equals(selectedTab)) {
            renderSelectedTab();
        }
    }

    private void promptExactAlarmAccessIfNeeded() {
        if (exactAlarmsAllowed()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Allow reliable timing")
                .setMessage("Allow Alarms & reminders so Android can recover a running meditation timer and deliver reminders at the time you choose.")
                .setNegativeButton("Not now", null)
                .setPositiveButton("Allow", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (RuntimeException unavailable) {
                        Toast.makeText(this, "Open Alarms & reminders in system settings.",
                                Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private boolean notificationsAllowed() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean exactAlarmsAllowed() {
        return Build.VERSION.SDK_INT < 31
                || getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    private void applyScreenMode(TimerState state) {
        if (state.active && state.dimScreen && !state.paused) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.screenBrightness = DIM_BRIGHTNESS;
            getWindow().setAttributes(attributes);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            restoreScreenMode();
        }
    }

    private void restoreScreenMode() {
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(attributes);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void shareTextFile(String filename, String text, String chooserTitle) {
        try {
            File directory = new File(getCacheDir(), "shared_logs");
            if (!directory.isDirectory() && !directory.mkdirs()) {
                throw new IOException("Could not create the share folder.");
            }
            File file = new File(directory, filename);
            try (FileOutputStream output = new FileOutputStream(file, false)) {
                output.write(text.getBytes(StandardCharsets.UTF_8));
            }
            Uri uri = new Uri.Builder().scheme("content")
                    .authority(getPackageName() + ".logs")
                    .appendPath(filename).build();
            Intent share = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "Meditation Timer")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            share.setClipData(ClipData.newRawUri(filename, uri));
            startActivity(Intent.createChooser(share, chooserTitle));
        } catch (IOException | RuntimeException error) {
            Toast.makeText(this, "Could not create the text file: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openPlayStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + getPackageName())));
        } catch (RuntimeException noStore) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private LinearLayout labeledField(String label, EditText field) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView labelView = bodyText(label);
        row.addView(labelView, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(field, new LinearLayout.LayoutParams(dp(92),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private LinearLayout labeledControl(String label, View control) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.addView(bodyText(label), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(control, new LinearLayout.LayoutParams(dp(170),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private CheckBox optionCheckBox(String text, boolean checked) {
        CheckBox option = new CheckBox(this);
        option.setText(text);
        option.setTextSize(16);
        option.setTextColor(primaryTextColor());
        option.setChecked(checked);
        option.setPadding(0, dp(8), 0, dp(8));
        return option;
    }

    private String cueModeLabel(boolean chimesEnabled, boolean vibrationEnabled) {
        if (chimesEnabled && vibrationEnabled) {
            return "chimes + vibration";
        }
        return chimesEnabled ? "chimes" : "vibration";
    }

    private String formatClockTime(int hour, int minute) {
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, hour);
        time.set(Calendar.MINUTE, minute);
        return DateFormat.getTimeFormat(this).format(time.getTime());
    }

    private String formatReminderDate(long wallTimeMs) {
        return new SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.US)
                .format(new Date(wallTimeMs));
    }

    private EditText numberField(int value) {
        EditText field = new EditText(this);
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        field.setText(String.valueOf(value));
        field.setTextSize(18);
        field.setTextColor(primaryTextColor());
        field.setSelectAllOnFocus(true);
        field.setGravity(Gravity.CENTER);
        return field;
    }

    private int parsePositive(EditText field) {
        try {
            return Integer.parseInt(field.getText().toString().trim());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Enter whole positive numbers for every timer value.");
        }
    }

    private LinearLayout pageColumn() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(12), dp(18), dp(32));
        return page;
    }

    private ScrollView scroll(View child) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(child, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private TextView sectionTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(23);
        view.setTextColor(accentTextColor());
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, dp(6), 0, dp(12));
        return view;
    }

    private TextView subsectionTitle(String text) {
        TextView view = sectionTitle(text);
        view.setTextSize(18);
        view.setPadding(0, dp(22), 0, dp(6));
        return view;
    }

    private TextView bodyText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(primaryTextColor());
        view.setLineSpacing(0f, 1.15f);
        return view;
    }

    private Button actionButton(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(primary ? Color.WHITE : Color.rgb(38, 53, 68));
        button.setMinHeight(dp(50));
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(15));
        background.setColor(primary ? Color.rgb(56, 108, 176) : Color.rgb(238, 243, 248));
        background.setStroke(dp(1), primary ? Color.rgb(22, 58, 107) : Color.rgb(190, 202, 215));
        button.setBackground(background);
        return button;
    }

    private GradientDrawable cardBackground(int color) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(14));
        background.setColor(color);
        background.setStroke(dp(1), Color.rgb(210, 219, 228));
        return background;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams fill() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private LinearLayout.LayoutParams fullButtonParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Spinner colorThemeSpinner() {
        AppColorTheme[] themes = AppColorTheme.values();
        List<String> labels = new ArrayList<>();
        int selected = 0;
        AppColorTheme current = currentColorTheme();
        for (int index = 0; index < themes.length; index++) {
            labels.add(themes[index].label());
            if (themes[index] == current) {
                selected = index;
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(primaryTextColor());
                view.setPadding(dp(10), dp(8), dp(10), dp(8));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Color.rgb(38, 53, 68));
                view.setBackgroundColor(Color.WHITE);
                view.setPadding(dp(12), dp(12), dp(12), dp(12));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected, false);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                       int position, long id) {
                AppColorTheme chosen = themes[position];
                if (chosen != currentColorTheme()) {
                    getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE).edit()
                            .putString("background_theme", chosen.id()).apply();
                    root.setBackgroundColor(chosen.backgroundColor());
                    renderSelectedTab();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        return spinner;
    }

    private AppColorTheme currentColorTheme() {
        String id = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE)
                .getString("background_theme", AppColorTheme.DARK_PURPLE.id());
        return AppColorTheme.fromId(id);
    }

    private int primaryTextColor() {
        return Color.rgb(245, 247, 252);
    }

    private int accentTextColor() {
        return Color.rgb(190, 218, 255);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        selectRequestedTab(intent);
        if (mainUiShown) {
            renderSelectedTab();
        }
    }

    @Override
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    protected void onStart() {
        super.onStart();
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(MeditationTimerService.EVENT_STATE_CHANGED);
            filter.addAction(MeditationTimerService.EVENT_COMPLETED);
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(stateReceiver, filter,
                        getPackageName() + ".permission.INTERNAL_EVENTS", null,
                        Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(stateReceiver, filter,
                        getPackageName() + ".permission.INTERNAL_EVENTS", null);
            }
            receiverRegistered = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(uiTicker);
        handler.post(uiTicker);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(uiTicker);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            unregisterReceiver(stateReceiver);
            receiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        restoreScreenMode();
        handler.removeCallbacksAndMessages(null);
        if (previewPlayer != null) {
            previewPlayer.release();
        }
        super.onDestroy();
    }
}
