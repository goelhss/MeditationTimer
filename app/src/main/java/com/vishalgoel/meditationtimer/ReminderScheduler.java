package com.vishalgoel.meditationtimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.TimeZone;

public final class ReminderScheduler {
    private static final int REQUEST_REMINDER = 3101;
    private final Context context;
    private final AlarmManager alarms;

    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        alarms = this.context.getSystemService(AlarmManager.class);
    }

    public void apply(ReminderSchedule schedule) {
        alarms.cancel(reminderIntent());
        if (!schedule.enabled()) {
            return;
        }
        long triggerAt = schedule.nextTriggerAfter(System.currentTimeMillis(),
                TimeZone.getDefault());
        try {
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt,
                        reminderIntent());
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt,
                        reminderIntent());
            }
        } catch (SecurityException denied) {
            alarms.set(AlarmManager.RTC_WAKEUP, triggerAt, reminderIntent());
        }
    }

    private PendingIntent reminderIntent() {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .setAction(ReminderReceiver.ACTION_REMINDER);
        return PendingIntent.getBroadcast(context, REQUEST_REMINDER, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
