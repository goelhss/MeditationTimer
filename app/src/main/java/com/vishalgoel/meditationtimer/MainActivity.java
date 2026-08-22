package com.vishalgoel.meditationtimer;

import android.Manifest;
import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.RevokeAccessRequest;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("SetTextI18n")
public final class MainActivity extends Activity {
    public static final String EXTRA_OPEN_TAB = "open_tab";
    public static final String TAB_TIMER = "timer";
    private static final String TAB_LOGS = "logs";
    private static final String TAB_STATS = "stats";
    private static final String TAB_RESOLUTION = "resolution";
    private static final String TAB_REMINDER = "reminder";
    private static final String TAB_BACKUP = "backup";
    private static final String TAB_ABOUT = "about";
    private static final String SETTINGS_PREFS = "timer_settings";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 401;
    private static final int GOOGLE_AUTH_REQUEST = 402;
    private static final int BACKUP_EXPORT_REQUEST = 403;
    private static final int BACKUP_IMPORT_REQUEST = 404;
    private static final float DIM_BRIGHTNESS = 0.08f;
    private static final Scope DRIVE_APPDATA_SCOPE = new Scope(
            "https://www.googleapis.com/auth/drive.appdata");

    private enum GoogleAction { CONNECT, BACKUP, RESTORE, DELETE, DISCONNECT, AUTO_BACKUP }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService backupExecutor = Executors.newSingleThreadExecutor();
    private final Set<String> selectedLogIds = new HashSet<>();
    private LinearLayout root;
    private LinearLayout content;
    private Button timerTab;
    private Button logsTab;
    private Button statsTab;
    private Button resolutionTab;
    private Button reminderTab;
    private Button backupTab;
    private Button aboutTab;
    private TextView countdownView;
    private AnalogTimerView analogTimerView;
    private TextView activeStatusView;
    private TextView activeProgressView;
    private String selectedTab = TAB_TIMER;
    private boolean mainUiShown;
    private boolean receiverRegistered;
    private String renderedStateKey = "";
    private AlertDialog pendingDialog;
    private String pendingDialogId = "";
    private String dismissedPendingDialogId = "";
    private TextView pendingMessageView;
    private ToneDingPlayer previewPlayer;
    private GoogleAction pendingGoogleAction;
    private boolean backupOperationRunning;

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
            String key = state.active + ":" + state.paused + ":" + state.preparing
                    + ":" + state.startWallMs;
            if (!key.equals(renderedStateKey) && TAB_TIMER.equals(selectedTab)) {
                renderSelectedTab();
            } else if (state.active && (countdownView != null || analogTimerView != null)) {
                long realtime = SystemClock.elapsedRealtime();
                long remaining = state.preparing ? state.preparationRemainingMs(realtime)
                        : state.remainingMs(realtime);
                if (countdownView != null) {
                    countdownView.setText(MeditationTimerService.formatCountdown(remaining));
                }
                if (analogTimerView != null) {
                    analogTimerView.setTime(remaining, state.durationMs);
                }
                activeStatusView.setText(state.preparing
                        ? (state.paused ? "Preparation paused" : "Get ready")
                        : (state.paused ? "Paused" : "Meditating"));
                if (activeProgressView != null) {
                    activeProgressView.setText(progressText(state, realtime));
                }
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
                || TAB_STATS.equals(requested)
                || TAB_RESOLUTION.equals(requested) || TAB_REMINDER.equals(requested)
                || TAB_BACKUP.equals(requested) || TAB_ABOUT.equals(requested)) {
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
        tabs.setOrientation(LinearLayout.VERTICAL);
        tabs.setPadding(dp(8), 0, dp(8), dp(8));
        LinearLayout topTabs = new LinearLayout(this);
        topTabs.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout bottomTabs = new LinearLayout(this);
        bottomTabs.setOrientation(LinearLayout.HORIZONTAL);
        timerTab = tabButton("Timer", TAB_TIMER);
        logsTab = tabButton("Logs", TAB_LOGS);
        statsTab = tabButton("Stats", TAB_STATS);
        resolutionTab = tabButton("Resolution", TAB_RESOLUTION);
        reminderTab = tabButton("Reminder", TAB_REMINDER);
        backupTab = tabButton("Backup", TAB_BACKUP);
        aboutTab = tabButton("About", TAB_ABOUT);
        topTabs.addView(timerTab, weighted());
        topTabs.addView(logsTab, weighted());
        topTabs.addView(statsTab, weighted());
        topTabs.addView(resolutionTab, weighted());
        bottomTabs.addView(reminderTab, weighted());
        bottomTabs.addView(backupTab, weighted());
        bottomTabs.addView(aboutTab, weighted());
        tabs.addView(topTabs, matchWrap());
        LinearLayout.LayoutParams bottomTabsParams = matchWrap();
        bottomTabsParams.topMargin = dp(5);
        tabs.addView(bottomTabs, bottomTabsParams);
        root.addView(tabs, matchWrap());

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        renderSelectedTab();
        showWhatsNewIfNeeded();
        showStreakNoticeIfNeeded();
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
        analogTimerView = null;
        activeStatusView = null;
        activeProgressView = null;
        updateTabStyles();
        if (TAB_LOGS.equals(selectedTab)) {
            renderLogs();
        } else if (TAB_STATS.equals(selectedTab)) {
            renderStats();
        } else if (TAB_RESOLUTION.equals(selectedTab)) {
            renderResolution();
        } else if (TAB_REMINDER.equals(selectedTab)) {
            renderReminder();
        } else if (TAB_BACKUP.equals(selectedTab)) {
            renderBackup();
        } else if (TAB_ABOUT.equals(selectedTab)) {
            renderAbout();
        } else {
            renderTimer();
        }
    }

    private void updateTabStyles() {
        styleTab(timerTab, TAB_TIMER.equals(selectedTab));
        styleTab(logsTab, TAB_LOGS.equals(selectedTab));
        styleTab(statsTab, TAB_STATS.equals(selectedTab));
        styleTab(resolutionTab, TAB_RESOLUTION.equals(selectedTab));
        styleTab(reminderTab, TAB_REMINDER.equals(selectedTab));
        styleTab(backupTab, TAB_BACKUP.equals(selectedTab));
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
        renderedStateKey = state.active + ":" + state.paused + ":" + state.preparing
                + ":" + state.startWallMs;
        if (state.active) {
            renderActiveTimer(state);
        } else {
            restoreScreenMode();
            renderTimerSetup();
        }
    }

    private void renderTimerSetup() {
        countdownView = null;
        analogTimerView = null;
        MeditationConfigurationStore configurationStore =
                new MeditationConfigurationStore(this);
        MeditationPreset selectedPreset = configurationStore.selectedPreset();
        MeditationConfiguration initial = selectedPreset.resolve(configurationStore.custom());
        LinearLayout form = pageColumn();
        form.addView(sectionTitle("Set your meditation"), matchWrap());

        MeditationPreset[] presets = MeditationPreset.values();
        ArrayAdapter<MeditationPreset> presetAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, presets);
        presetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner preset = new Spinner(this);
        preset.setAdapter(presetAdapter);
        preset.setSelection(selectedPreset.ordinal(), false);
        form.addView(labeledControl("What do you want to do today?", preset));

        EditText duration = numberField(initial.durationMinutes());
        EditText primary = numberField(initial.primaryMinutes());
        EditText additional = numberField(initial.additionalMinutes());
        EditText finish = numberField(initial.finishDings());
        EditText prep = numberField(initial.preparationSeconds());
        form.addView(labeledField("Meditation duration (minutes)", duration));
        form.addView(labeledField("Preparation time (seconds)", prep));
        form.addView(labeledField("One ding every (minutes)", primary));
        form.addView(labeledField("One additional ding every (minutes)", additional));
        form.addView(labeledField("Dings when finished", finish));

        CheckBox chimes = optionCheckBox("Chimes", initial.chimes());
        CheckBox vibrate = optionCheckBox("Vibrate", initial.vibrate());
        Spinner chimeSound = chimeSoundSpinner(initial.chimeSoundId());
        Spinner timerDisplay = timerDisplaySpinner(initial.timerDisplayId());
        boolean[] applyingConfiguration = {false};
        android.widget.CompoundButton.OnCheckedChangeListener cueModeListener = (button, checked) -> {
            if (!applyingConfiguration[0] && !chimes.isChecked() && !vibrate.isChecked()) {
                button.setChecked(true);
                Toast.makeText(this, "Keep Chimes, Vibrate, or both enabled.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        chimes.setOnCheckedChangeListener(cueModeListener);
        vibrate.setOnCheckedChangeListener(cueModeListener);
        form.addView(chimes, matchWrap());
        form.addView(vibrate, matchWrap());
        form.addView(labeledControl("Ding sound", chimeSound));
        form.addView(labeledControl("Timer display", timerDisplay));

        CheckBox dim = new CheckBox(this);
        dim.setText("Dim screen while countdown is visible");
        dim.setTextSize(16);
        dim.setTextColor(primaryTextColor());
        dim.setChecked(initial.dim());
        dim.setPadding(0, dp(10), 0, dp(10));
        form.addView(dim, matchWrap());

        java.util.function.Consumer<MeditationConfiguration> applyConfiguration = value -> {
            applyingConfiguration[0] = true;
            duration.setText(String.valueOf(value.durationMinutes()));
            prep.setText(String.valueOf(value.preparationSeconds()));
            primary.setText(String.valueOf(value.primaryMinutes()));
            additional.setText(String.valueOf(value.additionalMinutes()));
            finish.setText(String.valueOf(value.finishDings()));
            chimes.setChecked(value.chimes());
            vibrate.setChecked(value.vibrate());
            dim.setChecked(value.dim());
            chimeSound.setSelection(ChimeSound.fromId(value.chimeSoundId()).ordinal(), false);
            timerDisplay.setSelection(
                    TimerDisplayMode.fromId(value.timerDisplayId()).ordinal(), false);
            applyingConfiguration[0] = false;
        };
        preset.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                       int position, long id) {
                applyConfiguration.accept(presets[position].resolve(configurationStore.custom()));
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        Button saveCustom = actionButton("Save as the Custom configuration", false);
        saveCustom.setOnClickListener(view -> {
            try {
                MeditationConfiguration custom = readConfiguration(duration, prep, primary,
                        additional, finish, chimes, vibrate, dim, chimeSound, timerDisplay);
                configurationStore.saveCustom(custom);
                new BackupStatusStore(this).markDirty();
                preset.setSelection(MeditationPreset.CUSTOM.ordinal());
                Toast.makeText(this, "Custom configuration saved, including the ding sound.",
                        Toast.LENGTH_SHORT).show();
            } catch (IllegalArgumentException error) {
                showTimerValuesError(error);
            }
        });
        LinearLayout.LayoutParams customParams = fullButtonParams();
        customParams.topMargin = dp(8);
        form.addView(saveCustom, customParams);

        Button preview = actionButton("Preview cue", false);
        preview.setOnClickListener(view -> previewPlayer.play(1,
                chimes.isChecked(), vibrate.isChecked(),
                ((ChimeSound) chimeSound.getSelectedItem()).id()));
        form.addView(preview, fullButtonParams());

        Button start = actionButton("▶ Start", true);
        start.setTextSize(20);
        start.setOnClickListener(view -> {
            try {
                MeditationConfiguration configuration = readConfiguration(duration, prep,
                        primary, additional, finish, chimes, vibrate, dim, chimeSound,
                        timerDisplay);
                MeditationPreset chosenPreset = presets[preset.getSelectedItemPosition()];
                configurationStore.saveCurrent(configuration, chosenPreset);
                new BackupStatusStore(this).markDirty();
                requestNotificationPermissionIfNeeded();
                Intent service = new Intent(this, MeditationTimerService.class)
                        .setAction(MeditationTimerService.ACTION_START)
                        .putExtra(MeditationTimerService.EXTRA_DURATION_MINUTES,
                                configuration.durationMinutes())
                        .putExtra(MeditationTimerService.EXTRA_PRIMARY_MINUTES,
                                configuration.primaryMinutes())
                        .putExtra(MeditationTimerService.EXTRA_ADDITIONAL_MINUTES,
                                configuration.additionalMinutes())
                        .putExtra(MeditationTimerService.EXTRA_FINISH_DINGS,
                                configuration.finishDings())
                        .putExtra(MeditationTimerService.EXTRA_PREP_SECONDS,
                                configuration.preparationSeconds())
                        .putExtra(MeditationTimerService.EXTRA_CHIMES_ENABLED,
                                configuration.chimes())
                        .putExtra(MeditationTimerService.EXTRA_VIBRATION_ENABLED,
                                configuration.vibrate())
                        .putExtra(MeditationTimerService.EXTRA_CHIME_SOUND_ID,
                                configuration.chimeSoundId())
                        .putExtra(MeditationTimerService.EXTRA_DISPLAY_MODE_ID,
                                configuration.timerDisplayId())
                        .putExtra(MeditationTimerService.EXTRA_DIM_SCREEN,
                                configuration.dim());
                startForegroundService(service);
                promptExactAlarmAccessIfNeeded();
                handler.postDelayed(this::refreshForStateChange, 120L);
            } catch (IllegalArgumentException error) {
                showTimerValuesError(error);
            }
        });
        LinearLayout.LayoutParams startParams = fullButtonParams();
        startParams.topMargin = dp(14);
        form.addView(start, startParams);
        content.addView(scroll(form), fill());
    }

    private void renderActiveTimer(TimerState state) {
        countdownView = null;
        analogTimerView = null;
        LinearLayout page = pageColumn();
        activeStatusView = new TextView(this);
        activeStatusView.setText(state.preparing
                ? (state.paused ? "Preparation paused" : "Get ready")
                : (state.paused ? "Paused" : "Meditating"));
        activeStatusView.setTextSize(20);
        activeStatusView.setTextColor(accentTextColor());
        activeStatusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        activeStatusView.setGravity(Gravity.CENTER);
        page.addView(activeStatusView, matchWrap());

        LinearLayout.LayoutParams clockParams;
        if (!state.preparing
                && TimerDisplayMode.fromId(state.displayModeId) == TimerDisplayMode.ANALOG) {
            analogTimerView = new AnalogTimerView(this);
            analogTimerView.setTime(state.remainingMs(SystemClock.elapsedRealtime()),
                    state.durationMs);
            clockParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(340));
        } else {
            countdownView = new TextView(this);
            countdownView.setText(MeditationTimerService.formatCountdown(
                    state.preparing
                            ? state.preparationRemainingMs(SystemClock.elapsedRealtime())
                            : state.remainingMs(SystemClock.elapsedRealtime())));
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
            clockParams = fullButtonParams();
        }
        clockParams.topMargin = dp(18);
        clockParams.bottomMargin = dp(18);
        page.addView(analogTimerView != null ? analogTimerView : countdownView, clockParams);

        activeProgressView = bodyText(progressText(state, SystemClock.elapsedRealtime()));
        activeProgressView.setTextSize(18);
        activeProgressView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        activeProgressView.setGravity(Gravity.CENTER);
        activeProgressView.setPadding(0, 0, 0, dp(12));
        page.addView(activeProgressView, matchWrap());

        TextView detail = bodyText("Preparation: " + state.prepDurationMs / 1000L + " sec"
                + "\nPrimary ding every " + state.primaryMs / TimerSchedule.MINUTE_MS
                + " min · additional ding every " + state.additionalMs / TimerSchedule.MINUTE_MS
                + " min\nCompletion: " + state.finishDings + " dings"
                + " · " + cueModeLabel(state.chimesEnabled, state.vibrationEnabled)
                + (state.chimesEnabled ? " · " + ChimeSound.fromId(state.chimeSoundId).label() : "")
                + " · " + TimerDisplayMode.fromId(state.displayModeId).label()
                + (state.dimScreen ? " · screen dimming on" : ""));
        detail.setGravity(Gravity.CENTER);
        page.addView(detail, matchWrap());

        page.addView(subsectionTitle("Live cues"), matchWrap());
        LinearLayout cueControls = new LinearLayout(this);
        cueControls.setOrientation(LinearLayout.HORIZONTAL);
        CheckBox liveChimes = optionCheckBox("Chimes", state.chimesEnabled);
        CheckBox liveVibrate = optionCheckBox("Vibrate", state.vibrationEnabled);
        CheckBox liveDim = optionCheckBox("Dim", state.dimScreen);
        android.widget.CompoundButton.OnCheckedChangeListener liveCueListener =
                (button, checked) -> sendCueMode(liveChimes.isChecked(),
                        liveVibrate.isChecked());
        liveChimes.setOnCheckedChangeListener(liveCueListener);
        liveVibrate.setOnCheckedChangeListener(liveCueListener);
        liveDim.setOnCheckedChangeListener((button, checked) -> sendDimMode(checked));
        cueControls.addView(liveChimes, weighted());
        cueControls.addView(liveVibrate, weighted());
        cueControls.addView(liveDim, weighted());
        page.addView(cueControls, matchWrap());
        TextView cueHint = bodyText("Changes take effect immediately. Turn both cue switches off for silence.");
        cueHint.setGravity(Gravity.CENTER);
        page.addView(cueHint, matchWrap());

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
        StreakStore.Snapshot streak = new StreakStore(this).snapshot(logs,
                System.currentTimeMillis(), java.time.ZoneId.systemDefault());
        String streakSummary = streak.countingEnabled()
                ? "Current streak: " + streak.streak().currentDays()
                + " days\nLongest streak ever: " + streak.streak().bestDays() + " days"
                : "Streak counting: Off\nLongest streak previously recorded: "
                + streak.streak().bestDays() + " days";
        TextView streakCard = bodyText(streakSummary);
        streakCard.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        streakCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        streakCard.setBackground(cardBackground(Color.WHITE));
        page.addView(streakCard, fullButtonParams());
        Set<String> available = new HashSet<>();
        for (MeditationLog log : logs) {
            available.add(log.id());
        }
        selectedLogIds.retainAll(available);
        Button share = actionButton("Share all as text file", true);
        Button deleteSelected = actionButton("Delete selected", false);
        Runnable updateSelectionActions = () -> {
            share.setText(selectedLogIds.isEmpty()
                    ? "Share all as text file" : "Share selected as text file");
            deleteSelected.setEnabled(!selectedLogIds.isEmpty());
        };
        updateSelectionActions.run();

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
                    updateSelectionActions.run();
                });
                LinearLayout.LayoutParams rowParams = fullButtonParams();
                rowParams.bottomMargin = dp(8);
                page.addView(row, rowParams);
            }
        }

        share.setEnabled(!logs.isEmpty());
        share.setOnClickListener(view -> shareLogs(logs));
        page.addView(share, fullButtonParams());

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

    private void renderStats() {
        restoreScreenMode();
        LinearLayout page = pageColumn();
        page.addView(sectionTitle("Meditation Stats"), matchWrap());
        List<MeditationLog> logs = new MeditationLogStore(this).all();
        long now = System.currentTimeMillis();
        StreakStore streakStore = new StreakStore(this);
        StreakStore.Snapshot streakSnapshot = streakStore.snapshot(
                logs, now, java.time.ZoneId.systemDefault());
        MeditationStats.Streak streak = streakSnapshot.streak();

        CheckBox countStreaks = optionCheckBox("Count meditation streaks",
                streakSnapshot.countingEnabled());
        countStreaks.setOnCheckedChangeListener((button, checked) -> {
            streakStore.setCountingEnabled(checked);
            renderSelectedTab();
        });
        page.addView(countStreaks, matchWrap());

        CheckBox streakReminders = optionCheckBox("Use streak encouragement in reminders",
                streakSnapshot.reminderEnabled());
        streakReminders.setOnCheckedChangeListener((button, checked) -> {
            streakStore.setReminderEnabled(checked);
            Toast.makeText(this, checked ? "Streak encouragement enabled."
                    : "Streak encouragement disabled. General reminders are unchanged.",
                    Toast.LENGTH_SHORT).show();
        });
        page.addView(streakReminders, matchWrap());

        String streakText;
        if (!streakSnapshot.countingEnabled()) {
            streakText = "Streak counting is off";
        } else if (streakSnapshot.paused()) {
            streakText = "Vacation pause active through "
                    + formatStreakPauseDate(streakSnapshot.pauseUntilMs());
        } else if (streak.currentDays() == 0) {
            streakText = "Start your streak today";
        } else if (streak.meditatedToday()) {
            streakText = streak.currentDays() + "-day streak · protected today";
        } else if (streak.graceDaysRemaining() == 0) {
            streakText = streak.currentDays()
                    + "-day streak · meditate today to keep it alive";
        } else {
            streakText = streak.currentDays() + "-day streak · "
                    + streak.graceDaysRemaining() + " grace day"
                    + (streak.graceDaysRemaining() == 1 ? "" : "s") + " remaining";
        }
        TextView streakCard = bodyText(streakText + "\nLongest streak ever: "
                + streak.bestDays() + " days\n\nOne meditation day increases the tally once. "
                + "The streak resets after three full days of inactivity.");
        streakCard.setTextSize(18);
        streakCard.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        streakCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        streakCard.setBackground(cardBackground(Color.WHITE));
        page.addView(streakCard, fullButtonParams());

        if (streakSnapshot.countingEnabled()) {
            Button vacation = actionButton(streakSnapshot.paused()
                    ? "Resume streak now" : "Pause streak — going on vacation", false);
            vacation.setEnabled(streakSnapshot.paused() || streak.currentDays() > 0);
            vacation.setOnClickListener(view -> {
                if (streakSnapshot.paused()) {
                    streakStore.resumeNow(System.currentTimeMillis(),
                            java.time.ZoneId.systemDefault());
                    showStreakNoticeIfNeeded();
                    renderSelectedTab();
                } else {
                    showPauseStreakDialog(streakStore);
                }
            });
            LinearLayout.LayoutParams vacationParams = fullButtonParams();
            vacationParams.topMargin = dp(10);
            page.addView(vacation, vacationParams);
            TextView vacationHelp = bodyText((streak.currentDays() == 0
                    ? "Start a streak before using vacation pause. " : "")
                    + "A vacation pause lasts for at most 30 days. "
                    + "Open Meditation Timer within that time to preserve and resume your streak. "
                    + "After 30 days, it restarts gently at 1.");
            vacationHelp.setTextSize(13);
            vacationHelp.setPadding(0, dp(6), 0, 0);
            page.addView(vacationHelp, matchWrap());
        }

        TextView encouragement = bodyText("For emotional, spiritual, and long-term well-being:\nGrow old with a healthy soul. Meditate daily.");
        encouragement.setTextColor(Color.rgb(87, 56, 158));
        encouragement.setGravity(Gravity.CENTER);
        encouragement.setPadding(0, dp(14), 0, dp(14));
        page.addView(encouragement, matchWrap());

        SharedPreferences statsPreferences = getSharedPreferences("stats_settings", MODE_PRIVATE);
        MeditationStats.Range selectedRange = MeditationStats.Range.fromId(
                statsPreferences.getString("range", MeditationStats.Range.WEEKLY.id()));
        MeditationStats.Range[] ranges = MeditationStats.Range.values();
        ArrayAdapter<MeditationStats.Range> rangeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ranges);
        rangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner range = new Spinner(this);
        range.setAdapter(rangeAdapter);
        range.setSelection(selectedRange.ordinal(), false);
        range.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                       int position, long id) {
                MeditationStats.Range chosen = ranges[position];
                if (chosen != selectedRange) {
                    statsPreferences.edit().putString("range", chosen.id()).apply();
                    renderSelectedTab();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        page.addView(labeledControl("Chart period", range));

        MeditationStats.Report report = MeditationStats.report(
                logs, selectedRange, now, java.time.ZoneId.systemDefault());
        TextView summary = bodyText("Total: "
                + LogTextExporter.formatDuration(report.totalDurationMs())
                + " · " + report.sessions() + " session"
                + (report.sessions() == 1 ? "" : "s")
                + " · " + report.meditationDays() + " meditation day"
                + (report.meditationDays() == 1 ? "" : "s"));
        summary.setGravity(Gravity.CENTER);
        summary.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        page.addView(summary, matchWrap());

        StatsChartView chart = new StatsChartView(this);
        chart.setBuckets(report.buckets());
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(270));
        chartParams.topMargin = dp(12);
        page.addView(chart, chartParams);

        StringBuilder accessibleBreakdown = new StringBuilder();
        for (MeditationStats.Bucket bucket : report.buckets()) {
            if (accessibleBreakdown.length() > 0) {
                accessibleBreakdown.append(" · ");
            }
            accessibleBreakdown.append(bucket.label()).append(": ")
                    .append(LogTextExporter.formatDuration(bucket.durationMs()));
        }
        TextView breakdown = bodyText(accessibleBreakdown.toString());
        breakdown.setTextSize(13);
        breakdown.setGravity(Gravity.CENTER);
        page.addView(breakdown, matchWrap());
        content.addView(scroll(page), fill());
    }

    private void renderResolution() {
        restoreScreenMode();
        LinearLayout page = pageColumn();
        page.addView(sectionTitle("Resolution"), matchWrap());
        page.addView(bodyText("Record the promises you have made to your meditation practice, so you can return to them later."),
                matchWrap());

        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(Calendar.HOUR_OF_DAY, 12);
        selectedDate.set(Calendar.MINUTE, 0);
        selectedDate.set(Calendar.SECOND, 0);
        selectedDate.set(Calendar.MILLISECOND, 0);
        Button date = actionButton(formatResolutionDate(selectedDate.getTimeInMillis()), false);
        date.setOnClickListener(view -> new DatePickerDialog(this, (picker, year, month, day) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, day);
            date.setText(formatResolutionDate(selectedDate.getTimeInMillis()));
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show());
        page.addView(labeledControl("Date", date));

        TextView commentLabel = bodyText("Comment");
        commentLabel.setTextColor(accentTextColor());
        commentLabel.setPadding(0, dp(10), 0, dp(5));
        page.addView(commentLabel, matchWrap());
        EditText comment = new EditText(this);
        comment.setHint("For example: I resolve to meditate for one hour daily.");
        comment.setTextColor(Color.rgb(38, 53, 68));
        comment.setHintTextColor(Color.rgb(100, 112, 124));
        comment.setTextSize(17);
        comment.setGravity(Gravity.TOP | Gravity.START);
        comment.setMinLines(3);
        comment.setPadding(dp(12), dp(10), dp(12), dp(10));
        comment.setBackground(cardBackground(Color.WHITE));
        page.addView(comment, matchWrap());

        Button save = actionButton("Save resolution", true);
        LinearLayout.LayoutParams saveParams = fullButtonParams();
        saveParams.topMargin = dp(12);
        save.setOnClickListener(view -> {
            try {
                new ResolutionStore(this).add(selectedDate.getTimeInMillis(),
                        comment.getText().toString());
                Toast.makeText(this, "Resolution saved.", Toast.LENGTH_SHORT).show();
                renderSelectedTab();
            } catch (IllegalArgumentException error) {
                new AlertDialog.Builder(this)
                        .setTitle("Add a comment")
                        .setMessage(error.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
        page.addView(save, saveParams);

        page.addView(subsectionTitle("Past resolutions"), matchWrap());
        List<Resolution> resolutions = new ResolutionStore(this).all();
        if (resolutions.isEmpty()) {
            page.addView(bodyText("No resolutions recorded yet."), matchWrap());
        } else {
            for (Resolution resolution : resolutions) {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(12), dp(10), dp(12), dp(10));
                card.setBackground(cardBackground(Color.WHITE));
                TextView savedDate = bodyText(formatResolutionDate(resolution.dateMs()));
                savedDate.setTextColor(Color.rgb(87, 56, 158));
                savedDate.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                card.addView(savedDate, matchWrap());
                TextView savedComment = bodyText(resolution.comment());
                savedComment.setTextColor(Color.rgb(38, 53, 68));
                savedComment.setPadding(0, dp(6), 0, dp(6));
                card.addView(savedComment, matchWrap());
                Button delete = actionButton("Delete", false);
                delete.setTextColor(Color.rgb(179, 38, 30));
                delete.setOnClickListener(view -> new AlertDialog.Builder(this)
                        .setTitle("Delete this resolution?")
                        .setMessage(formatResolutionDate(resolution.dateMs()) + "\n\n"
                                + resolution.comment())
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dialog, which) -> {
                            new ResolutionStore(this).delete(resolution.id());
                            renderSelectedTab();
                        }).show());
                card.addView(delete, fullButtonParams());
                LinearLayout.LayoutParams cardParams = fullButtonParams();
                cardParams.bottomMargin = dp(10);
                page.addView(card, cardParams);
            }
        }
        content.addView(scroll(page), fill());
    }

    private void renderReminder() {
        restoreScreenMode();
        ReminderSchedule saved = new ReminderStore(this).load();
        LinearLayout page = pageColumn();
        page.addView(sectionTitle("Meditation Reminder"), matchWrap());
        page.addView(bodyText("Choose when Android should gently remind you to meditate and maintain your streak. Grow old with a healthy soul. Meditate daily."),
                matchWrap());

        CheckBox enabled = optionCheckBox("Remind me to meditate", saved.enabled());
        LinearLayout.LayoutParams enabledParams = matchWrap();
        enabledParams.topMargin = dp(12);
        page.addView(enabled, enabledParams);

        StreakStore streakStore = new StreakStore(this);
        CheckBox streakEncouragement = optionCheckBox(
                "Include streak encouragement in reminders",
                streakStore.load().reminderEnabled());
        streakEncouragement.setOnCheckedChangeListener((button, checked) ->
                streakStore.setReminderEnabled(checked));
        page.addView(streakEncouragement, matchWrap());

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
        StreakStore.Snapshot streak = new StreakStore(this).snapshot(allLogs,
                System.currentTimeMillis(), java.time.ZoneId.systemDefault());
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        shareTextFile("meditation-logs-" + timestamp + ".txt",
                LogTextExporter.export(selected, streak.countingEnabled(),
                        streak.streak().currentDays(), streak.streak().bestDays()),
                "Share Meditation Logs");
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

    private void renderBackup() {
        restoreScreenMode();
        BackupStatusStore status = new BackupStatusStore(this);
        LinearLayout page = pageColumn();
        page.addView(sectionTitle("Backup & Restore"), matchWrap());
        page.addView(bodyText("Save meditation logs, resolutions, timer settings, reminders, and streak preferences/history. Active timers and diagnostics are never included."),
                matchWrap());

        page.addView(subsectionTitle("Private Google Drive backup"), matchWrap());
        String connection = status.isGoogleConnected()
                ? "Connected to Google Drive" : "Not connected";
        String lastBackup = status.lastSuccessMs() > 0L
                ? formatBackupTime(status.lastSuccessMs()) : "Never";
        String error = status.lastError();
        page.addView(bodyText(connection + "\nLast successful backup: " + lastBackup
                + (status.isRestoreDecisionRequired()
                        ? "\nA Drive backup is waiting for your restore decision." : "")
                + (error == null || error.isBlank() ? "" : "\nLast issue: " + error)), matchWrap());

        CheckBox automatic = optionCheckBox("Back up automatically when the app is open",
                status.isAutoBackupEnabled());
        automatic.setOnCheckedChangeListener((button, checked) -> {
            status.setAutoBackupEnabled(checked);
            if (checked) {
                maybeRunAutomaticBackup();
            }
        });
        page.addView(automatic, matchWrap());
        page.addView(bodyText("Google receives one private app-data JSON file, capped at 1 MB. Its location and filename are managed automatically; you never need to choose a Drive folder. Each backup replaces the previous one."),
                matchWrap());

        Button connect = actionButton(status.isGoogleConnected()
                ? "Reconnect or change Google account" : "Connect Google Drive", true);
        connect.setEnabled(!backupOperationRunning);
        connect.setOnClickListener(view -> authorizeGoogle(GoogleAction.CONNECT));
        LinearLayout.LayoutParams connectParams = fullButtonParams();
        connectParams.topMargin = dp(12);
        page.addView(connect, connectParams);

        LinearLayout googleActions = new LinearLayout(this);
        googleActions.setOrientation(LinearLayout.HORIZONTAL);
        Button backupNow = actionButton("Backup now", false);
        backupNow.setEnabled(status.isGoogleConnected() && !backupOperationRunning);
        backupNow.setOnClickListener(view -> authorizeGoogle(GoogleAction.BACKUP));
        Button restore = actionButton("Restore", false);
        restore.setEnabled(status.isGoogleConnected() && !backupOperationRunning);
        restore.setOnClickListener(view -> authorizeGoogle(GoogleAction.RESTORE));
        googleActions.addView(backupNow, weighted());
        googleActions.addView(restore, weighted());
        LinearLayout.LayoutParams googleParams = matchWrap();
        googleParams.topMargin = dp(8);
        page.addView(googleActions, googleParams);

        Button disconnect = actionButton("Disconnect Google Drive", false);
        disconnect.setEnabled(status.isGoogleConnected() && !backupOperationRunning);
        disconnect.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle("Disconnect Google Drive?")
                .setMessage("Meditation Timer will revoke its private Drive permission. Existing backup data remains in your Google account until you remove the app data in Drive settings.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Disconnect", (dialog, which) ->
                        authorizeGoogle(GoogleAction.DISCONNECT))
                .show());
        LinearLayout.LayoutParams disconnectParams = fullButtonParams();
        disconnectParams.topMargin = dp(8);
        page.addView(disconnect, disconnectParams);

        Button deleteCloud = actionButton("Delete Google Drive backup", false);
        deleteCloud.setTextColor(Color.rgb(179, 38, 30));
        deleteCloud.setEnabled(status.isGoogleConnected() && !backupOperationRunning);
        deleteCloud.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle("Delete Google Drive backup?")
                .setMessage("This permanently deletes Meditation Timer's private backup from Google Drive and turns automatic backup off. Data currently on this phone is not deleted.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete backup", (dialog, which) ->
                        authorizeGoogle(GoogleAction.DELETE))
                .show());
        LinearLayout.LayoutParams deleteCloudParams = fullButtonParams();
        deleteCloudParams.topMargin = dp(8);
        page.addView(deleteCloud, deleteCloudParams);

        page.addView(subsectionTitle("Portable JSON file"), matchWrap());
        page.addView(bodyText("Export creates a readable file in a location you choose. Anyone who can access that file can read its meditation history and resolutions."),
                matchWrap());
        LinearLayout fileActions = new LinearLayout(this);
        fileActions.setOrientation(LinearLayout.HORIZONTAL);
        Button export = actionButton("Export file", false);
        export.setOnClickListener(view -> startBackupExport());
        Button importFile = actionButton("Import file", false);
        importFile.setOnClickListener(view -> startBackupImport());
        fileActions.addView(export, weighted());
        fileActions.addView(importFile, weighted());
        LinearLayout.LayoutParams fileParams = matchWrap();
        fileParams.topMargin = dp(8);
        page.addView(fileActions, fileParams);
        content.addView(scroll(page), fill());
    }

    private void startBackupExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, "meditation-timer-backup-"
                        + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date())
                        + ".json");
        startActivityForResult(intent, BACKUP_EXPORT_REQUEST);
    }

    private void startBackupImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json");
        startActivityForResult(intent, BACKUP_IMPORT_REQUEST);
    }

    private void authorizeGoogle(GoogleAction action) {
        if (backupOperationRunning) {
            return;
        }
        backupOperationRunning = true;
        pendingGoogleAction = action;
        AuthorizationRequest.Builder request = AuthorizationRequest.builder()
                .setRequestedScopes(List.of(DRIVE_APPDATA_SCOPE));
        if (action == GoogleAction.CONNECT) {
            request.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT);
        }
        Identity.getAuthorizationClient(this).authorize(request.build())
                .addOnSuccessListener(result -> {
                    if (result.hasResolution()) {
                        if (action == GoogleAction.AUTO_BACKUP) {
                            new BackupStatusStore(this).recordError(
                                    "Reconnect Google Drive to resume automatic backup.");
                            finishBackupOperation(false);
                            return;
                        }
                        try {
                            startIntentSenderForResult(result.getPendingIntent().getIntentSender(),
                                    GOOGLE_AUTH_REQUEST, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException error) {
                            showBackupError("Could not open Google authorization.");
                        }
                    } else {
                        performGoogleAction(action, result);
                    }
                })
                .addOnFailureListener(error -> showBackupError(
                        "Google authorization failed. Check Google Play services and try again."));
    }

    private void performGoogleAction(GoogleAction action, AuthorizationResult authorization) {
        if (action == GoogleAction.DISCONNECT) {
            revokeGoogleAccess(authorization);
            return;
        }
        String accessToken = authorization.getAccessToken();
        backupExecutor.execute(() -> {
            try {
                DriveAppDataClient drive = new DriveAppDataClient();
                if (action == GoogleAction.CONNECT) {
                    boolean hasBackup = drive.hasBackup(accessToken);
                    handler.post(() -> {
                        BackupStatusStore status = new BackupStatusStore(this);
                        status.setGoogleConnected(true);
                        status.recordError("");
                        if (hasBackup) {
                            status.requireRestoreDecision();
                            new AlertDialog.Builder(this)
                                    .setTitle("Google Drive connected")
                                    .setMessage("A Meditation Timer backup is available. Restore it now?")
                                    .setNegativeButton("Not now", (dialog, which) ->
                                            finishBackupOperation(true))
                                    .setPositiveButton("Restore", (dialog, which) ->
                                            performGoogleAction(GoogleAction.RESTORE,
                                                    authorization))
                                    .setOnCancelListener(dialog -> finishBackupOperation(true))
                                    .show();
                        } else {
                            Toast.makeText(this, "Google Drive connected.",
                                    Toast.LENGTH_LONG).show();
                            finishBackupOperation(true);
                            maybeRunAutomaticBackup();
                        }
                    });
                } else if (action == GoogleAction.BACKUP
                        || action == GoogleAction.AUTO_BACKUP) {
                    String json = BackupCodec.encode(
                            new BackupRepository(this).snapshot(System.currentTimeMillis()));
                    drive.upload(accessToken, json);
                    handler.post(() -> {
                        new BackupStatusStore(this).recordSuccess(System.currentTimeMillis());
                        if (action == GoogleAction.BACKUP) {
                            Toast.makeText(this, "Backup saved privately in Google Drive.",
                                    Toast.LENGTH_LONG).show();
                        }
                        finishBackupOperation(true);
                    });
                } else if (action == GoogleAction.RESTORE) {
                    BackupSnapshot snapshot = BackupCodec.decode(drive.download(accessToken));
                    handler.post(() -> {
                        finishBackupOperation(false);
                        confirmRestore(snapshot, "Google Drive");
                    });
                } else if (action == GoogleAction.DELETE) {
                    drive.deleteBackup(accessToken);
                    handler.post(() -> {
                        new BackupStatusStore(this).recordBackupDeleted();
                        Toast.makeText(this, "Google Drive backup permanently deleted.",
                                Toast.LENGTH_LONG).show();
                        finishBackupOperation(true);
                    });
                }
            } catch (Exception error) {
                handler.post(() -> showBackupError(cleanError(error)));
            }
        });
    }

    private void revokeGoogleAccess(AuthorizationResult authorization) {
        Account account = authorization.toGoogleSignInAccount() == null
                ? null : authorization.toGoogleSignInAccount().getAccount();
        if (account == null) {
            new BackupStatusStore(this).clearConnection();
            Toast.makeText(this, "Google Drive disconnected.", Toast.LENGTH_SHORT).show();
            finishBackupOperation(true);
            return;
        }
        RevokeAccessRequest request = RevokeAccessRequest.builder()
                .setAccount(account)
                .setScopes(List.of(DRIVE_APPDATA_SCOPE))
                .build();
        Identity.getAuthorizationClient(this).revokeAccess(request)
                .addOnSuccessListener(ignored -> {
                    new BackupStatusStore(this).clearConnection();
                    Toast.makeText(this, "Google Drive access revoked.", Toast.LENGTH_LONG).show();
                    finishBackupOperation(true);
                })
                .addOnFailureListener(error -> showBackupError(
                        "Could not revoke Google Drive access. Try again."));
    }

    private void confirmRestore(BackupSnapshot snapshot, String source) {
        String created = snapshot.generatedAtMs() > 0L
                ? formatBackupTime(snapshot.generatedAtMs()) : "Unknown";
        new AlertDialog.Builder(this)
                .setTitle("Restore from " + source + "?")
                .setMessage("Backup date: " + created
                        + "\nMeditation logs: " + snapshot.logs().size()
                        + "\nResolutions: " + snapshot.resolutions().size()
                        + "\n\nLogs and resolutions will be merged without duplicates. Timer, reminder, and streak preferences will be replaced; the greater longest-ever streak is kept.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Restore", (dialog, which) -> {
                    BackupRepository.RestoreResult result =
                            new BackupRepository(this).restore(snapshot);
                    if ("Google Drive".equals(source)) {
                        new BackupStatusStore(this).clearRestoreDecision();
                    }
                    root.setBackgroundColor(currentColorTheme().backgroundColor());
                    Toast.makeText(this, "Restored. Added " + result.logsAdded()
                                    + " logs and " + result.resolutionsAdded() + " resolutions.",
                            Toast.LENGTH_LONG).show();
                    renderSelectedTab();
                })
                .show();
    }

    private void maybeRunAutomaticBackup() {
        BackupStatusStore status = new BackupStatusStore(this);
        if (backupOperationRunning || !status.isGoogleConnected()
                || !status.isAutoBackupEnabled() || !status.isDirty()) {
            return;
        }
        if (status.isRestoreDecisionRequired()) {
            return;
        }
        authorizeGoogle(GoogleAction.AUTO_BACKUP);
    }

    private void finishBackupOperation(boolean rerender) {
        backupOperationRunning = false;
        pendingGoogleAction = null;
        if (rerender && TAB_BACKUP.equals(selectedTab) && mainUiShown) {
            renderSelectedTab();
        }
    }

    private void showBackupError(String message) {
        new BackupStatusStore(this).recordError(message);
        backupOperationRunning = false;
        pendingGoogleAction = null;
        if (TAB_BACKUP.equals(selectedTab) && mainUiShown) {
            renderSelectedTab();
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String cleanError(Throwable error) {
        String message = error == null ? "" : error.getMessage();
        if (message == null || message.isBlank()) {
            return "Backup operation failed. Try again.";
        }
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private String formatBackupTime(long wallTimeMs) {
        return new SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US)
                .format(new Date(wallTimeMs));
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
        page.addView(bodyText("1.7.0 · August 21, 2026\n"
                + "Session presets and saved Custom configuration, working individual log deletion, additive deduplicated restore, meditation charts, optional streak controls, persistent longest-ever history, a 30-day vacation pause, and new 13-petal purple-lotus ocean artwork.\n\n"
                + "1.6.0 · August 21, 2026\n"
                + "Private Google Drive backup, portable JSON export/import, restore safeguards, and explicit backup deletion.\n\n"
                + "1.5.0 · August 21, 2026\n"
                + "Meditation Bowl, preparation countdown, visible elapsed time, a Well done lotus, and meditation resolutions.\n\n"
                + "1.4.0 · August 18, 2026\n"
                + "Live Dim control joins the running-session Chimes and Vibrate controls.\n\n"
                + "1.3.0 · August 18, 2026\n"
                + "Live Chimes and Vibrate controls, including silent mode during a running session.\n\n"
                + "1.2.0 · August 18, 2026\n"
                + "Resonant sound choices plus large digital and analog timer displays.\n\n"
                + "1.1.0 · August 18, 2026\n"
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
                .setTitle("What’s new in 1.7.0")
                .setMessage("• Quick 5, Regular 30, Weekly 60, and saved Custom configurations\n"
                        + "• Individual meditation-log deletion fixed\n"
                        + "• Additive restore with content duplicate protection\n"
                        + "• Daily through yearly charts in the new Stats tab\n"
                        + "• Optional streak counting and reminder encouragement\n"
                        + "• Longest-ever history and a respectful 30-day vacation pause\n"
                        + "• New 13-petal purple-lotus ocean icon and in-app artwork")
                .setPositiveButton("Continue", null)
                .show();
    }

    private void showPauseStreakDialog(StreakStore streakStore) {
        new AlertDialog.Builder(this)
                .setTitle("Pause your streak for vacation?")
                .setMessage("Your current streak will be protected for up to 30 days. "
                        + "Opening Meditation Timer again within 30 days resumes it. "
                        + "If you return later, the app gently restarts the streak at 1.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Pause for vacation", (dialog, which) -> {
                    streakStore.pause(System.currentTimeMillis(),
                            java.time.ZoneId.systemDefault());
                    Toast.makeText(this, "Vacation pause started.", Toast.LENGTH_SHORT).show();
                    renderSelectedTab();
                })
                .show();
    }

    private String formatStreakPauseDate(long wallMs) {
        return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new Date(wallMs));
    }

    private void showStreakNoticeIfNeeded() {
        if (!mainUiShown) {
            return;
        }
        String notice = new StreakStore(this).consumeNotice();
        if (StreakStore.NOTICE_RESET.equals(notice)) {
            new AlertDialog.Builder(this)
                    .setTitle("Welcome back")
                    .setMessage("Streak reset to 1. Good luck this time.")
                    .setPositiveButton("Thank you", null)
                    .show();
        } else if (StreakStore.NOTICE_RESUMED.equals(notice)) {
            Toast.makeText(this, "Welcome back — your streak is active again.",
                    Toast.LENGTH_LONG).show();
        }
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
            pendingMessageView = null;
            dismissedPendingDialogId = "";
            return;
        }
        if (pending.id().equals(dismissedPendingDialogId)) {
            return;
        }
        if (pending.id().equals(pendingDialogId) && pendingDialog != null
                && pendingDialog.isShowing()) {
            updatePendingMessage(pending);
            return;
        }
        pendingDialogId = pending.id();
        pendingDialog = new AlertDialog.Builder(this)
                .setTitle("Well done.")
                .setView(completionPromptView(pending))
                .setPositiveButton("Yes, log it", (dialog, which) ->
                        sendTimerAction(MeditationTimerService.ACTION_LOG_YES))
                .setNegativeButton("No", (dialog, which) ->
                        sendTimerAction(MeditationTimerService.ACTION_LOG_NO))
                .setNeutralButton("Dismiss", null)
                .create();
        pendingDialog.setOnDismissListener(dialog -> {
            if (new PendingMeditationStore(this).get() != null) {
                dismissedPendingDialogId = pending.id();
            }
            pendingDialogId = "";
            pendingMessageView = null;
        });
        pendingDialog.show();
    }

    private View completionPromptView(PendingMeditationStore.Pending pending) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(24), dp(4), dp(24), dp(6));
        ImageView lotus = new ImageView(this);
        lotus.setImageResource(R.drawable.lotus_ocean_13_petals);
        lotus.setContentDescription("Purple 13-petal lotus over the ocean");
        lotus.setAdjustViewBounds(true);
        panel.addView(lotus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        pendingMessageView = bodyText(pendingMessage(pending));
        pendingMessageView.setTextColor(Color.rgb(38, 53, 68));
        pendingMessageView.setGravity(Gravity.CENTER);
        panel.addView(pendingMessageView, matchWrap());
        return panel;
    }

    private void updatePendingMessage(PendingMeditationStore.Pending pending) {
        if (pendingMessageView != null) {
            pendingMessageView.setText(pendingMessage(pending));
        }
    }

    private String pendingMessage(PendingMeditationStore.Pending pending) {
        long seconds = CompletionDecisionPolicy.secondsRemaining(
                pending.decisionDeadlineMs(), System.currentTimeMillis());
        return "You completed " + LogTextExporter.formatDuration(pending.durationMs())
                + " of meditation.\n\nLog this session? Yes will be selected automatically in "
                + seconds + " seconds.";
    }

    private void sendTimerAction(String action) {
        startForegroundService(new Intent(this, MeditationTimerService.class).setAction(action));
        handler.postDelayed(this::refreshForStateChange, 120L);
    }

    private void sendCueMode(boolean chimesEnabled, boolean vibrationEnabled) {
        Intent intent = new Intent(this, MeditationTimerService.class)
                .setAction(MeditationTimerService.ACTION_SET_CUES)
                .putExtra(MeditationTimerService.EXTRA_CHIMES_ENABLED, chimesEnabled)
                .putExtra(MeditationTimerService.EXTRA_VIBRATION_ENABLED, vibrationEnabled);
        startForegroundService(intent);
        handler.postDelayed(this::refreshForStateChange, 120L);
    }

    private void sendDimMode(boolean dimScreen) {
        Intent intent = new Intent(this, MeditationTimerService.class)
                .setAction(MeditationTimerService.ACTION_SET_DIM)
                .putExtra(MeditationTimerService.EXTRA_DIM_SCREEN, dimScreen);
        startForegroundService(intent);
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
        if (chimesEnabled) {
            return "chimes";
        }
        return vibrationEnabled ? "vibration" : "silent";
    }

    private String progressText(TimerState state, long realtimeMs) {
        if (state.preparing) {
            return "Meditation begins in " + MeditationTimerService.formatCountdown(
                    state.preparationRemainingMs(realtimeMs));
        }
        return "Elapsed " + MeditationTimerService.formatCountdown(
                state.elapsedActiveMs(realtimeMs))
                + "  ·  Remaining " + MeditationTimerService.formatCountdown(
                        state.remainingMs(realtimeMs));
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

    private String formatResolutionDate(long wallTimeMs) {
        return new SimpleDateFormat("MMM d, yyyy", Locale.US)
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

    private MeditationConfiguration readConfiguration(
            EditText duration, EditText prep, EditText primary, EditText additional,
            EditText finish, CheckBox chimes, CheckBox vibrate, CheckBox dim,
            Spinner chimeSound, Spinner timerDisplay) {
        return new MeditationConfiguration(
                parsePositive(duration), parseNonNegative(prep), parsePositive(primary),
                parsePositive(additional), parsePositive(finish), chimes.isChecked(),
                vibrate.isChecked(), dim.isChecked(),
                ((ChimeSound) chimeSound.getSelectedItem()).id(),
                ((TimerDisplayMode) timerDisplay.getSelectedItem()).id());
    }

    private void showTimerValuesError(IllegalArgumentException error) {
        new AlertDialog.Builder(this)
                .setTitle("Check timer values")
                .setMessage(error.getMessage())
                .setPositiveButton("OK", null)
                .show();
    }

    private int parsePositive(EditText field) {
        try {
            int value = Integer.parseInt(field.getText().toString().trim());
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Enter whole positive numbers for every timer value.");
        }
    }

    private int parseNonNegative(EditText field) {
        try {
            int value = Integer.parseInt(field.getText().toString().trim());
            if (value < 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Preparation time must be zero or more seconds.");
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
                    new BackupStatusStore(MainActivity.this).markDirty();
                    root.setBackgroundColor(chosen.backgroundColor());
                    renderSelectedTab();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        return spinner;
    }

    private Spinner chimeSoundSpinner(String selectedId) {
        ChimeSound[] sounds = ChimeSound.values();
        ArrayAdapter<ChimeSound> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sounds) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(sounds[position].label());
                view.setTextColor(primaryTextColor());
                view.setPadding(dp(10), dp(8), dp(10), dp(8));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setText(sounds[position].label());
                view.setTextColor(Color.rgb(38, 53, 68));
                view.setBackgroundColor(Color.WHITE);
                view.setPadding(dp(12), dp(14), dp(12), dp(14));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(adapter);
        ChimeSound selected = ChimeSound.fromId(selectedId);
        spinner.setSelection(selected.ordinal(), false);
        return spinner;
    }

    private Spinner timerDisplaySpinner(String selectedId) {
        TimerDisplayMode[] modes = TimerDisplayMode.values();
        ArrayAdapter<TimerDisplayMode> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, modes) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(modes[position].label());
                view.setTextColor(primaryTextColor());
                view.setPadding(dp(10), dp(8), dp(10), dp(8));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setText(modes[position].label());
                view.setTextColor(Color.rgb(38, 53, 68));
                view.setBackgroundColor(Color.WHITE);
                view.setPadding(dp(12), dp(14), dp(12), dp(14));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(adapter);
        spinner.setSelection(TimerDisplayMode.fromId(selectedId).ordinal(), false);
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
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_AUTH_REQUEST) {
            if (resultCode != RESULT_OK || data == null || pendingGoogleAction == null) {
                finishBackupOperation(true);
                return;
            }
            try {
                AuthorizationResult result = Identity.getAuthorizationClient(this)
                        .getAuthorizationResultFromIntent(data);
                performGoogleAction(pendingGoogleAction, result);
            } catch (ApiException error) {
                showBackupError("Google authorization was not completed.");
            }
            return;
        }
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == BACKUP_EXPORT_REQUEST) {
            backupExecutor.execute(() -> {
                try {
                    String json = BackupCodec.encode(
                            new BackupRepository(this).snapshot(System.currentTimeMillis()));
                    try (OutputStream output = getContentResolver().openOutputStream(uri, "w")) {
                        if (output == null) {
                            throw new IOException("The selected location could not be opened.");
                        }
                        output.write(json.getBytes(StandardCharsets.UTF_8));
                    }
                    handler.post(() -> Toast.makeText(this,
                            "Portable backup file saved.", Toast.LENGTH_LONG).show());
                } catch (Exception error) {
                    handler.post(() -> showBackupError(cleanError(error)));
                }
            });
        } else if (requestCode == BACKUP_IMPORT_REQUEST) {
            backupExecutor.execute(() -> {
                try {
                    BackupSnapshot snapshot = BackupCodec.decode(readBackupFile(uri));
                    handler.post(() -> confirmRestore(snapshot, "selected file"));
                } catch (Exception error) {
                    handler.post(() -> showBackupError(cleanError(error)));
                }
            });
        }
    }

    private String readBackupFile(Uri uri) throws IOException {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IOException("The selected file could not be opened.");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8_192];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > BackupCodec.MAX_BACKUP_BYTES) {
                    throw new IOException("The selected backup is larger than 1 MB.");
                }
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
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
        StreakStore streakStore = new StreakStore(this);
        boolean pauseFinished = streakStore.onAppOpened(System.currentTimeMillis(),
                java.time.ZoneId.systemDefault());
        if (pauseFinished && mainUiShown
                && (TAB_STATS.equals(selectedTab) || TAB_LOGS.equals(selectedTab))) {
            renderSelectedTab();
        }
        showStreakNoticeIfNeeded();
        handler.removeCallbacks(uiTicker);
        handler.post(uiTicker);
        maybeRunAutomaticBackup();
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
        backupExecutor.shutdownNow();
        super.onDestroy();
    }
}
