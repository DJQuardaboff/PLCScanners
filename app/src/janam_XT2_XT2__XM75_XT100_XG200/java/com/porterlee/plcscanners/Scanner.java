package com.porterlee.plcscanners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.view.KeyEvent;

import device.common.DecodeResult;
import device.common.IScannerService;
import device.common.ScanConst;
import device.sdk.DeviceServer;

class Scanner extends AbstractScanner {
    private static final String TAG = Scanner.class.getCanonicalName();
    private static final String READ_FAIL_SYMBOL = "READ_FAIL";
    private final DecodeResult mDecodeResult = new DecodeResult();
    private IScannerService mScanner;
    private int keysDown;
    private final IntentFilter resultFilter = new IntentFilter(ScanConst.INTENT_USERMSG);
    private final BroadcastReceiver mResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                getScanner().aDecodeGetResult(mDecodeResult.recycle());
                if (!READ_FAIL_SYMBOL.equals(mDecodeResult.symName))
                    onBarcodeScanned(mDecodeResult.toString());
            } catch (RemoteException | SecurityException e) {
                e.printStackTrace();
            }
        }
    };

    private final BroadcastReceiver mScanKeyEventReceiver = new BroadcastReceiver() {
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
                        } catch (RemoteException | SecurityException e) {
                            e.printStackTrace();
                        }
                }
            }
        }
    };

    private IScannerService getScanner() {
        return mScanner != null ? mScanner : (mScanner = DeviceServer.getIScannerService());
    }

    @Override
    public boolean init() {
        try {
            getScanner().aDecodeAPIInit();
            return true;
        } catch (RemoteException | SecurityException ignored) { }
        return false;
    }

    @Override
    public void enable() {
        try {
            getScanner().aDecodeSetTriggerEnable(1);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disable() {
        try {
            getScanner().aDecodeSetTriggerEnable(0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isReady() {
        return getScanner() != null;
    }

    @Override
    public void onResume() {
        keysDown = 0;

        if (getApplicationContext() != null) {
            getApplicationContext().registerReceiver(mResultReceiver, resultFilter);
            getApplicationContext().registerReceiver(mScanKeyEventReceiver, new IntentFilter(ScanConst.INTENT_SCANKEY_EVENT));
        }

        try {
            getScanner().aDecodeSetTriggerOn(0);
            getScanner().aDecodeSetBeepEnable(0);
            getScanner().aDecodeSetVibratorEnable(0);
            getScanner().aDecodeSetDecodeEnable(1);
            getScanner().aDecodeSetTriggerEnable(1);
            getScanner().aDecodeSetTerminator(ScanConst.Terminator.DCD_TERMINATOR_NONE);
            getScanner().aDecodeSetResultType(ScanConst.ResultType.DCD_RESULT_USERMSG);
            getScanner().aDecodeSetTriggerMode(ScanConst.TriggerMode.DCD_TRIGGER_MODE_ONESHOT);
            getScanner().aDecodeSetPrefixEnable(0);
            getScanner().aDecodeSetPostfixEnable(0);
        } catch (RemoteException | SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        if (getApplicationContext() != null) {
            try {
                getApplicationContext().unregisterReceiver(mResultReceiver);
            } catch (IllegalArgumentException ignored) { }

            try {
                getApplicationContext().unregisterReceiver(mScanKeyEventReceiver);
            } catch (IllegalArgumentException ignored) { }
        }

        try {
            getScanner().aDecodeSetTriggerOn(0);
        } catch (RemoteException | SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        if (getApplicationContext() != null) {
            try {
                getApplicationContext().unregisterReceiver(mResultReceiver);
            } catch (IllegalArgumentException ignored) { }

            try {
                getApplicationContext().unregisterReceiver(mScanKeyEventReceiver);
            } catch (IllegalArgumentException ignored) { }
        }

        try {
            getScanner().aDecodeSetTriggerOn(0);
        } catch (RemoteException | SecurityException e) {
            e.printStackTrace();
        }
    }
}
