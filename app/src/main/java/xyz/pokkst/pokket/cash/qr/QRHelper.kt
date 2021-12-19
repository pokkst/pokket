package xyz.pokkst.pokket.cash.qr

import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity

class QRHelper {
    fun startQRScan(fragment: Fragment, requestCode: Int) {
        IntentIntegrator.forSupportFragment(fragment).setPrompt("Scan QR").setBeepEnabled(false)
            .setDesiredBarcodeFormats(BarcodeFormat.QR_CODE.name).setOrientationLocked(true)
            .setCameraId(0).setCaptureActivity(CaptureActivity::class.java)
            .setRequestCode(requestCode).initiateScan()
    }
}
