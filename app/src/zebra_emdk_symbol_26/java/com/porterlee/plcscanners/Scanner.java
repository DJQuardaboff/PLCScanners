package com.porterlee.plcscanners;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

class Scanner extends AbstractScanner {
    private static final String TAG = Scanner.class.getCanonicalName();
    private static boolean gettingEmdkManager;
    private static EMDKManager emdkManager;
    private static com.symbol.emdk.barcode.Scanner mScanner;

    @Override
    public boolean init() {
        return getEmdkManager() == EMDKResults.STATUS_CODE.SUCCESS;
    }

    private EMDKResults.STATUS_CODE getEmdkManager() {
        if (gettingEmdkManager)
            return EMDKResults.STATUS_CODE.UNKNOWN;
        else
            gettingEmdkManager = true;

        try {
            if (getApplicationContext() != null) {
                return EMDKManager.getEMDKManager(getApplicationContext(), new EMDKManager.EMDKListener() {
                    @Override
                    public void onOpened(final EMDKManager emdkManager) {
                        if (Scanner.emdkManager != null)
                            return;

                        Scanner.emdkManager = emdkManager;
                        gettingEmdkManager = false;

                        try {
                            getScanner().enable();
                            getScanner().addStatusListener(new com.symbol.emdk.barcode.Scanner.StatusListener() {
                                @Override
                                public void onStatus(StatusData statusData) {
                                    if (statusData.getState().equals(StatusData.ScannerStates.IDLE) && getScanner().triggerType.equals(com.symbol.emdk.barcode.Scanner.TriggerType.HARD)) {
                                        try {
                                            getScanner().read();
                                        } catch (ScannerException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                            getScanner().addDataListener(new com.symbol.emdk.barcode.Scanner.DataListener() {
                                @Override
                                public void onData(ScanDataCollection scanDataCollection) {
                                    if (ScannerResults.SUCCESS.equals(scanDataCollection.getResult())) {
                                        onBarcodeScanned(scanDataCollection.getScanData().get(0).getData());
                                    } else {
                                        onScanComplete(false);
                                    }
                                }
                            });
                            getScanner().triggerType = com.symbol.emdk.barcode.Scanner.TriggerType.HARD;
                            if (!getScanner().isReadPending())
                                getScanner().read();
                        } catch (ScannerException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onClosed() {
                        if (mScanner != null) {
                            try {
                                mScanner.disable();
                                mScanner.release();
                            } catch (ScannerException e) {
                                e.printStackTrace();
                            }
                        }
                        if (emdkManager != null) {
                            mScanner = null;
                            emdkManager.release();
                            emdkManager = null;
                        }
                    }
                }).statusCode;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return EMDKResults.STATUS_CODE.UNKNOWN;
    }

    @Override
    public void enable() {
        getScanner().triggerType = com.symbol.emdk.barcode.Scanner.TriggerType.HARD;
        try {
            if (!getScanner().isReadPending())
                getScanner().read();
        } catch (ScannerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disable() {
        getScanner().triggerType = com.symbol.emdk.barcode.Scanner.TriggerType.SOFT_ALWAYS;
        try {
            if (getScanner().isReadPending())
                getScanner().cancelRead();
        } catch (ScannerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isReady() {
        return emdkManager != null;
    }

    private com.symbol.emdk.barcode.Scanner getScanner() {
        return mScanner == null && emdkManager != null ? (mScanner = ((BarcodeManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE)).getDevice(BarcodeManager.DeviceIdentifier.DEFAULT)) : mScanner;
    }

    @Override
    public void onResume() {
        if (emdkManager == null)
            getEmdkManager();
        if (mScanner == null)
            getScanner();
    }

    @Override
    public void onPause() {
        if (emdkManager != null) {
            mScanner = null;
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    public void onDestroy() {
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }
}
