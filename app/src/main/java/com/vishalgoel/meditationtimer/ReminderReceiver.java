package com.vishalgoel.meditationtimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_REMINDER = "com.vishalgoel.meditationtimer.REMINDER";
    private static final String CHANNEL_REMINDERS = "meditation_reminders";
    private static final int NOTIFICATION_REMINDER = 3102;

    @Override
    public void onReceive(Context context, Intent intent) {
        ReminderSchedule schedule = new ReminderStore(context).load();
        if (!schedule.enabled()) {
            return;
        }
        if (intent != null && ACTION_REMINDER.equals(intent.getAction())) {
            showReminder(context);
        }
        new ReminderScheduler(context).apply(schedule);
    }

    private void showReminder(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL_REMINDERS,
                "Meditation reminders", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Reminds you at the meditation times you choose.");
        manager.createNotificationChannel(channel);

        Intent openTimer = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_TIMER)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open = PendingIntent.getActivity(context, 0, openTimer,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_meditation)
                .setContentTitle("Time to meditate")
                .setContentText("A quiet moment is waiting for you.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build();
        manager.notify(NOTIFICATION_REMINDER, notification);
    }
}
