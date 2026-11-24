package com.wynter.pillvox.feedback;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Vibration patterns. They only SUPPORT speech and text; they never carry critical medical information alone.
 * A pattern is [wait, vibrate, wait, vibrate, ...] in milliseconds.
 */
public final class Haptics {
    private Haptics() { }
    public static final long[] MATCH = {0, 600};                        // 1 long pulse = match / success
    public static final long[] UNCERTAIN = {0, 150, 100, 150, 100, 150}; // 3 short pulses = please re-scan
    public static final long[] WARNING = {0, 500, 150, 150, 150, 500};   // long, short, long = safety warning

    /** Example: Haptics.play(context, Haptics.WARNING); */
    public static void play(Context ctx, long[] pattern) {
        Vibrator v = ctx.getSystemService(Vibrator.class);
        if (v != null) v.vibrate(VibrationEffect.createWaveform(pattern, -1));   // -1 = play once
    }
}
