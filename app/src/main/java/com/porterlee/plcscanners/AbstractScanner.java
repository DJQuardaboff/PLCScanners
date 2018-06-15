package com.porterlee.plcscanners;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.jetbrains.annotations.Contract;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;

public abstract class AbstractScanner implements Closeable {
    private static final String TAG = AbstractScanner.class.getCanonicalName();
    private static final String PREFERENCES_NAME = "plc_scanner_preferences";
    private static final String FIRST_TIME_SCANNER_SETUP_KEY = "first_time_setup_" + BuildConfig.FLAVOR;
    private static MediaPlayer scanFailMediaPlayer;
    private static MediaPlayer scanSuccessMediaPlayer;
    private static int userNum;
    private static OnBarcodeScannedListener mOnBarcodeScannedListener;
    private static WeakReference<Activity> activityWeakReference;
    private static Scanner mScanner;
    private static boolean firstTimeScannerSetup;
    private static boolean mIsEnabled = true;

    public static void setActivity(Activity activity) {
        if (activityWeakReference != null && activityWeakReference.get() == activity) {
            return;
        }

        activityWeakReference = new WeakReference<>(activity);
        if (getApplicationContext() != null) {
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            firstTimeScannerSetup = sharedPreferences.getBoolean(FIRST_TIME_SCANNER_SETUP_KEY, true);
            if (firstTimeScannerSetup)
                sharedPreferences.edit().putBoolean(FIRST_TIME_SCANNER_SETUP_KEY, false).apply();
            if (scanFailMediaPlayer == null) {
                scanFailMediaPlayer = new MediaPlayer();
                try {
                    scanFailMediaPlayer.setDataSource(getApplicationContext(), Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.scan_fail));
                    scanFailMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                    scanFailMediaPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "scan_fail.wav could not be initialized");
                }
            }

            if (scanSuccessMediaPlayer == null) {
                scanSuccessMediaPlayer = new MediaPlayer();
                try {
                    scanSuccessMediaPlayer.setDataSource(getApplicationContext(), Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.scan_success));
                    scanSuccessMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                    scanSuccessMediaPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "scan_success.wav could not be initialized");
                }
            }
        }
    }

    protected static int getUserNum() {
        return userNum;
    }

    @NonNull
    public static AbstractScanner getInstance() {
        return mScanner != null ? mScanner : (mScanner = new Scanner());
    }

    @Nullable
    protected static Activity getActivity() {
        return activityWeakReference != null ? activityWeakReference.get() : null;
    }

    @Nullable
    protected static Context getApplicationContext() {
        return activityWeakReference != null ? (activityWeakReference.get() != null ? activityWeakReference.get().getApplicationContext() : null) : null;
    }

    public static boolean isCompatible() {
        return BuildConfig.COMPATIBLE_MANUFACTURERS.contains(Build.MANUFACTURER) && BuildConfig.COMPATIBLE_MODELS.contains(Build.MODEL);
    }

    protected static void toast(final String message, final int length) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), message, length).show();
                }
            });
        }
    }

    @Contract(pure = true)
    protected static boolean isFirstTimeScannerSetup() {
        return firstTimeScannerSetup;
    }

    public abstract boolean init();
    public abstract void onIsEnabledChanged(boolean isEnabled);
    public abstract boolean isReady();
    @NonNull
    public abstract String[] getPermissions();

    public void setIsEnabled(boolean isEnabled) {
        mIsEnabled = isEnabled;
        onIsEnabledChanged(isEnabled);
    }
    public static boolean getIsEnabled() {
        return mIsEnabled;
    }

    public static void onScanComplete(boolean success) {
        if (success) {
            scanSuccessMediaPlayer.start();
        } else {
            scanFailMediaPlayer.start();
            if (getApplicationContext()!= null)
                Utils.vibrate(getApplicationContext());
        }
    }

    public static void setOnBarcodeScannedListener(OnBarcodeScannedListener onBarcodeScannedListener) {
        mOnBarcodeScannedListener = onBarcodeScannedListener;
    }

    public static OnBarcodeScannedListener getOnBarcodeScannedListener() {
        return mOnBarcodeScannedListener;
    }

    protected static void onBarcodeScanned(final String barcode) {
        if (mOnBarcodeScannedListener != null && getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mOnBarcodeScannedListener.onBarcodeScanned(barcode);
                }
            });
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) { return true; }
    public boolean onPrepareOptionsMenu(Menu menu) { return true; }
    public boolean onOptionsItemSelected(MenuItem item) { return false; }
    public void onStart() { }
    public void onResume() { userNum++; }
    public void onPause() { userNum--; }
    public void onStop() { }
    public void onDestroy() {
        if (getUserNum() <= 0) {
            try {
                Log.i(TAG, "onDestroy called with no users, releasing resources");
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnBarcodeScannedListener {
        void onBarcodeScanned(String barcode);
    }
}
