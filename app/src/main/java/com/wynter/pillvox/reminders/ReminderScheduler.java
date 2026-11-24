package com.wynter.pillvox.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.wynter.pillvox.data.Schedule;
import java.time.ZonedDateTime;

/**
 * REMINDERS: how a dose alert reaches the user even when the app is closed.
 *   schedule saved -> ReminderScheduler sets an exact alarm (Android AlarmManager)
 *   alarm fires    -> ReminderReceiver shows a notification, then sets tomorrow's alarm
 *   phone reboots  -> BootReceiver sets all alarms again (a reboot erases them)
 *   user taps the notification -> MainActivity opens on Reminders and asks "Have you taken it?"
 * Why not WorkManager? It can run minutes late. Medicine times need exact alarms.
 */
public final class ReminderScheduler {
    private ReminderScheduler() { }

    /** Next clock time (today, or tomorrow if already passed) as epoch millis. Example: nextTriggerMillis(13, 0) */
    public static long nextTriggerMillis(int hour, int minute) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime t = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        if (!t.isAfter(now)) t = t.plusDays(1);
        return t.toInstant().toEpochMilli();
    }

    /** Example: ReminderScheduler.schedule(context, scheduleFromDb);   (schedule.id must be set) */
    public static void schedule(Context ctx, Schedule s) {
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, (int) s.id,
                new Intent(ctx, ReminderReceiver.class).putExtra("scheduleId", s.id),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        long t = nextTriggerMillis(s.hour, s.minute);
        // Android 12+ needs the user to allow "Alarms & reminders"; if not allowed we fall back to a
        // slightly less precise alarm so reminders still arrive.
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
    }
}
