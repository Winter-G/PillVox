package com.wynter.pillvox.reminders;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.wynter.pillvox.MainActivity;
import com.wynter.pillvox.data.Medicine;
import com.wynter.pillvox.data.PillDao;
import com.wynter.pillvox.data.PillDb;
import com.wynter.pillvox.data.Schedule;
import java.util.concurrent.Executors;

/** Runs when a dose alarm fires: shows a notification, then arms tomorrow's alarm (daily repeat). */
public class ReminderReceiver extends BroadcastReceiver {
    @SuppressLint("MissingPermission")   // we check POST_NOTIFICATIONS ourselves below
    @Override
    public void onReceive(Context ctx, Intent intent) {
        long id = intent.getLongExtra("scheduleId", -1);
        PendingResult pending = goAsync();   // gives us time to read the database before Android stops us
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                PillDao dao = PillDb.get(ctx).dao();
                Schedule s = dao.schedule(id);
                if (s == null) return;
                Medicine m = dao.medicine(s.medicineId);
                String name = m != null ? m.name : "your medicine";

                ctx.getSystemService(NotificationManager.class).createNotificationChannel(
                        new NotificationChannel("doses", "Dose reminders", NotificationManager.IMPORTANCE_HIGH));
                PendingIntent open = PendingIntent.getActivity(ctx, (int) id,
                        new Intent(ctx, MainActivity.class).putExtra("scheduleId", id),
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                boolean allowed = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx,
                        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
                if (allowed) {
                    NotificationManagerCompat.from(ctx).notify((int) id, new NotificationCompat.Builder(ctx, "doses")
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle("Time for your medicine")
                            .setContentText(name + ". Tap to confirm.")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(open).setAutoCancel(true).build());
                }
                ReminderScheduler.schedule(ctx, s);   // arm tomorrow's alarm
            } finally {
                pending.finish();
            }
        });
    }
}