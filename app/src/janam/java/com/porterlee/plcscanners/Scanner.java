package com.porterlee.plcscanners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import android.view.KeyEvent;

import device.common.DecodeResult;
import device.common.IScannerService;
import device.common.ScanConst;
import device.sdk.DeviceServer;

class Scanner extends AbstractScanner {
    private static final String TAG = Scanner.class.getCanonicalName();
    private static final String READ_FAIL_SYMBOL = "READ_FAIL";
    private static final DecodeResult DECODE_RESULT;
    static {
        DecodeResult temp = null;
        try {
            temp = new DecodeResult();
        } catch (NoClassDefFoundError ignored) { }
        DECODE_RESULT = temp;
    }
    private static IScannerService mScanner;
    private static int keysDown;
    private static final IntentFilter SCAN_RESULT_EVENT_FILTER = new IntentFilter(ScanConst.INTENT_USERMSG);
    private static final BroadcastReceiver SCAN_RESULT_EVENT_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (DECODE_RESULT != null) {
                    getScanner().aDecodeGetResult(DECODE_RESULT.recycle());
                    if (!READ_FAIL_SYMBOL.equals(DECODE_RESULT.symName)) {
                        String barcode = DECODE_RESULT.toString();
                        onBarcodeScanned(barcode == null ? "" : barcode);
                    }
                }
            } catch (RemoteException | SecurityException | NoClassDefFoundError e) {
                e.printStackTrace();
            }
        }
    };

    private static final IntentFilter SCAN_KEY_EVENT_FILTER = new IntentFilter(ScanConst.INTENT_USERMSG);
    private static final BroadcastReceiver SCAN_KEY_EVENT_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ScanConst.INTENT_SCANKEY_EVENT.equals(intent.getAction())) {
                final KeyEvent event = intent.getParcelableExtra(ScanConst.EXTRA_SCANKEY_EVENT);
                switch (event.getKeyCode()) {
                    case ScanConst.KEYCODE_SCAN_FRONT: case ScanConst.KEYCODE_SCAN_LEFT: case ScanConst.KEYCODE_SCAN_RIGHT: case ScanConst.KEYCODE_SCAN_REAR:
                        switch (event.getAction()) {
                            case KeyEvent.ACTION_DOWN:
                                if (keysDown >= 4)
                                    keysDown = 4;
                                else
                                    keysDown++;
                                break;
                            case KeyEvent.ACTION_UP:
                                if (keysDown <= 0)
                                    keysDown = 0;
                                else
                                    keysDown--;
                                break;
                        }

                        try {
                            if (keysDown > 0) {
                                getScanner().aDecodeSetTriggerOn(1);
                            } else {
                                getScanner().aDecodeSetTriggerOn(0);
                            }
                        } catch (RemoteException | SecurityException | NoClassDefFoundError e) {
                            e.printStackTrace();
                        }
                }
            }
        }
    };

    private static IScannerService getScanner() {
        return mScanner != null ? mScanner : (mScanner = DeviceServer.getIScannerService());
    }

    @Override
    public boolean init() {
        if (DECODE_RESULT == null)
            return false;
        try {
            getScanner().aDecodeAPIInit();
            return true;
        } catch (RemoteException | SecurityException | NoClassDefFoundError ignored) { }
        return false;
    }

    @Override
    public void onIsEnabledChanged(boolean isEnabled) {
        try {
            getScanner().aDecodeSetTriggerEnable(isEnabled ? 1 : 0);
        } catch (RemoteException | SecurityException | NoClassDefFoundError e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isReady() {
        return getScanner() != null;
    }

    @NonNull
    @Override
    public String[] getPermissions() {
        return new String[0];
    }

    @Override
    public void close() {
        if (getApplicationContext() != null) {
            try {
                getApplicationContext().unregisterReceiver(SCAN_RESULT_EVENT_RECEIVER);
            } catch (IllegalArgumentException ignored) { }

            try {
                getApplicationContext().unregisterReceiver(SCAN_KEY_EVENT_RECEIVER);
            } catch (IllegalArgumentException ignored) { }
        }

        try {
            getScanner().aDecodeSetTriggerEnable(1);
            getScanner().aDecodeSetTriggerOn(0);
            getScanner().aDecodeAPIDeinit();
        } catch (RemoteException | SecurityException | NoClassDefFoundError e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        keysDown = 0;

        if (getApplicationContext() != null) {
            getApplicationContext().registerReceiver(SCAN_RESULT_EVENT_RECEIVER, SCAN_RESULT_EVENT_FILTER);
            getApplicationContext().registerReceiver(SCAN_KEY_EVENT_RECEIVER, SCAN_KEY_EVENT_FILTER);
        }

        try {
            getScanner().aDecodeSetTriggerOn(0);
            getScanner().aDecodeSetBeepEnable(0);
            getScanner().aDecodeSetVibratorEnable(0);
            getScanner().aDecodeSetDecodeEnable(1);
            getScanner().aDecodeSetTerminator(ScanConst.Terminator.DCD_TERMINATOR_NONE);
            getScanner().aDecodeSetResultType(ScanConst.ResultType.DCD_RESULT_USERMSG);
            getScanner().aDecodeSetTriggerMode(ScanConst.TriggerMode.DCD_TRIGGER_MODE_ONESHOT);
            getScanner().aDecodeSetPrefixEnable(0);
            getScanner().aDecodeSetPostfixEnable(0);
            onIsEnabledChanged(getIsEnabled());
        } catch (RemoteException | SecurityException | NoClassDefFoundError e) {
            e.printStackTrace();
        }
    }
}
