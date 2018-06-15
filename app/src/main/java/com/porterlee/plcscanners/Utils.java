package com.porterlee.plcscanners;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import org.jetbrains.annotations.NotNull;

public class Utils {
    private static long[] vibrationPattern = { 0L, 150L, 100L, 150L };

    public static void setVibrationPattern(long[] vibrationPattern) {
        Utils.vibrationPattern = vibrationPattern;
    }

    public static boolean vibrate(@NotNull Context context) {
        final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (vibrator != null) {
                vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1));
                return true;
            }
        } else {
            if (vibrator != null) {
                vibrator.vibrate(vibrationPattern, -1);
                return true;
            }
        }
        return false;
    }
}
