package de.mintware.barcode_scan

import android.content.Context
import android.hardware.Camera
import android.util.Log
import me.dm7.barcodescanner.core.CameraWrapper
import me.dm7.barcodescanner.zxing.ZXingScannerView

class ZXingAutofocusScannerView(context: Context) : ZXingScannerView(context) {

    private var mCameraWrapper: CameraWrapper? = null

    val mCamera: Camera?
        get() = mCameraWrapper?.mCamera


    private var callbackFocus = false
    private var autofocusPresence = false

    override fun setupCameraPreview(cameraWrapper: CameraWrapper?) {
        this.mCameraWrapper = cameraWrapper

        cameraWrapper?.mCamera?.parameters?.let { parameters ->
            try {
                autofocusPresence = parameters.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO);
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                cameraWrapper.mCamera.parameters = parameters
            } catch (ex: Exception) {
                callbackFocus = true
            }
        }
        super.setupCameraPreview(cameraWrapper)
    }

    override fun setAutoFocus(state: Boolean) {
        if(autofocusPresence){
            super.setAutoFocus(callbackFocus)
        }
    }
}