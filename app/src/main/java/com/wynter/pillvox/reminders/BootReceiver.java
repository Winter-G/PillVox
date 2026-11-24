package com.wynter.pillvox.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.wynter.pillvox.data.PillDb;
import com.wynter.pillvox.data.Schedule;
import java.util.concurrent.Executors;

/** Runs once after the phone restarts: a reboot erases alarms, so set every saved schedule again. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        PendingResult pending = goAsync();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                for (Schedule s : PillDb.get(ctx).dao().allSchedules()) ReminderScheduler.schedule(ctx, s);
            } finally {
                pending.finish();
            }
        });
    }
}
