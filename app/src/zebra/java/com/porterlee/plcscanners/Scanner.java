package com.porterlee.plcscanners;

import androidx.annotation.NonNull;

import android.util.Log;
import android.widget.Toast;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.BarcodeManager.ScannerConnectionListener;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

class Scanner extends AbstractScanner implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener {
    private static final String TAG = Scanner.class.getCanonicalName();
    private EMDKManager mEmdkManager;
    private BarcodeManager mBarcodeManager;
    private com.symbol.emdk.barcode.Scanner mScanner;

    @Override
    public void onOpened(final EMDKManager emdkManager) {
        mEmdkManager = emdkManager;
        initBarcodeManager();
    }

    @Override
    public void onClosed() {
        if (mEmdkManager != null) {
            mEmdkManager.release();
            mEmdkManager = null;
        }
        Toast.makeText(getApplicationContext(), "EMDK closed unexpectedly! Please close and restart the application.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        if (ScannerResults.SUCCESS.equals(scanDataCollection.getResult())) {
            String barcode = scanDataCollection.getScanData().get(0).getData();
            onBarcodeScanned(barcode == null ? "" : barcode);
        } else {
            onScanComplete(false);
        }
    }

    @Override
    public void onStatus(StatusData statusData) {
        StatusData.ScannerStates state = statusData.getState();
        switch (state) {
            case IDLE:
                if (mScanner.triggerType.equals(com.symbol.emdk.barcode.Scanner.TriggerType.HARD)) {
                    try {
                        if (!mScanner.isReadPending()) {
                            mScanner.read();
                        }
                    } catch (ScannerException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case WAITING:
                if (mScanner.triggerType.equals(com.symbol.emdk.barcode.Scanner.TriggerType.SOFT_ALWAYS)) {
                    try {
                        if (mScanner.isReadPending()) {
                            mScanner.cancelRead();
                        }
                    } catch (ScannerException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case SCANNING:
                break;
            case DISABLED:
                Toast.makeText(getApplicationContext(), statusData.getFriendlyName() + " is disabled.", Toast.LENGTH_SHORT).show();
                break;
            case ERROR:
                Toast.makeText(getApplicationContext(), "An error has occurred.", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, BarcodeManager.ConnectionState connectionState) {
        /*switch (connectionState) {
            case CONNECTED:
                bSoftTriggerSelected = false;
                synchronized (lock) {
                    initScanner();
                    bExtScannerDisconnected = false;
                }
                break;
            case DISCONNECTED:
                bExtScannerDisconnected = true;
                synchronized (lock) {
                    deInitScanner();
                }
                break;
        }*/
    }

    @Override
    public boolean init() {
        setIsEnabled(true);
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        return results.statusCode.equals(EMDKResults.STATUS_CODE.SUCCESS);
    }

    @Override
    public void onIsEnabledChanged(boolean isEnabled) {
        try {
            if (isEnabled) {
                mScanner.triggerType = com.symbol.emdk.barcode.Scanner.TriggerType.HARD;
                if (!mScanner.isReadPending()) {
                    mScanner.read();
                }
            } else {
                mScanner.triggerType = com.symbol.emdk.barcode.Scanner.TriggerType.SOFT_ALWAYS;
                if (mScanner.isReadPending()) {
                    mScanner.cancelRead();
                }
            }
        } catch (ScannerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isReady() {
        return mEmdkManager != null;
    }

    @NonNull
    @Override
    public String[] getPermissions() {
        return new String[]{"com.symbol.emdk.permission.EMDK"};
    }

    @Override
    public void close() {
        deInitScanner();
        deInitBarcodeManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mEmdkManager != null) {
            initBarcodeManager();
            initScanner();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        deInitScanner();
        deInitBarcodeManager();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Release all the resources
        if (mEmdkManager != null) {
            mEmdkManager.release();
            mEmdkManager = null;
        }
    }

    private void initScanner() {
        if (mScanner == null) {
            if (mBarcodeManager != null) {
                mScanner = mBarcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT);
            }

            if (mScanner != null) {
                mScanner.addDataListener(this);
                mScanner.addStatusListener(this);
                try {
                    mScanner.enable();
                } catch (ScannerException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    deInitScanner();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Failed to initialize the scanner device", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deInitScanner() {
        if (mScanner != null) {
            try {
                mScanner.disable();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                mScanner.removeDataListener(this);
                mScanner.removeStatusListener(this);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                mScanner.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mScanner = null;
        }
    }

    private void initBarcodeManager() {
        mBarcodeManager = (BarcodeManager) mEmdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
        // Add connection listener
        if (mBarcodeManager != null) {
            mBarcodeManager.addConnectionListener(this);
        }
    }

    private void deInitBarcodeManager() {
        if (mEmdkManager != null) {
            mEmdkManager.release(EMDKManager.FEATURE_TYPE.BARCODE);
        }
    }
}
