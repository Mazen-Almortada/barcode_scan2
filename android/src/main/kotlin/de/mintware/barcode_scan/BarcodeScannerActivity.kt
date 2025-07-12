package de.mintware.barcode_scan

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.hardware.Camera
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView
import java.io.File
import java.io.FileOutputStream

class BarcodeScannerActivity : Activity(), ZXingScannerView.ResultHandler {

    init {
        title = ""
    }

    private lateinit var config: Protos.Configuration
    private var scannerView: ZXingScannerView? = null
    private var captureButton: ImageButton? = null
    private var lastResult: Protos.ScanResult? = null
    private var isCodeDetected = false
    private var rootLayout: FrameLayout? = null


    companion object {
        const val TOGGLE_FLASH = 200
        const val CANCEL = 300
        const val EXTRA_CONFIG = "config"
        const val EXTRA_RESULT = "scan_result"
        const val EXTRA_ERROR_CODE = "error_code"

        private val formatMap: Map<Protos.BarcodeFormat, BarcodeFormat> = mapOf(
                Protos.BarcodeFormat.aztec to BarcodeFormat.AZTEC,
                Protos.BarcodeFormat.code39 to BarcodeFormat.CODE_39,
                Protos.BarcodeFormat.code93 to BarcodeFormat.CODE_93,
                Protos.BarcodeFormat.code128 to BarcodeFormat.CODE_128,
                Protos.BarcodeFormat.dataMatrix to BarcodeFormat.DATA_MATRIX,
                Protos.BarcodeFormat.ean8 to BarcodeFormat.EAN_8,
                Protos.BarcodeFormat.ean13 to BarcodeFormat.EAN_13,
                Protos.BarcodeFormat.interleaved2of5 to BarcodeFormat.ITF,
                Protos.BarcodeFormat.pdf417 to BarcodeFormat.PDF_417,
                Protos.BarcodeFormat.qr to BarcodeFormat.QR_CODE,
                Protos.BarcodeFormat.upce to BarcodeFormat.UPC_E
        )

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rotation = (getSystemService(
                Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        val orientation = when (rotation) {
            Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }

        requestedOrientation = orientation

        config = Protos.Configuration.parseFrom(intent.extras!!.getByteArray(EXTRA_CONFIG))

        val title = config.android.appBarTitle
        if (title.isNotEmpty()) {
            actionBar?.title = title
        }
        
        rootLayout = FrameLayout(this)
        setContentView(rootLayout)
    }

    private fun takePicture() {
        val camera = (scannerView as? ZXingAutofocusScannerView)?.mCamera
        camera?.takePicture(null, null, Camera.PictureCallback { data, _ ->
            if (lastResult != null) {
                val imageFile = File.createTempFile("barcode_scan_image", ".jpg", cacheDir)
                FileOutputStream(imageFile).use { it.write(data) }

                val builder = lastResult!!.toBuilder()
                builder.imagePath = imageFile.absolutePath
                val resultWithImagePath = builder.build()

                val intent = Intent()
                intent.putExtra(EXTRA_RESULT, resultWithImagePath.toByteArray())
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        })
    }


    private fun setupScannerView(forCapture: Boolean = false) {
        rootLayout?.removeAllViews()

        scannerView = ZXingAutofocusScannerView(this).apply {
            this.setLaserEnabled(!forCapture)
            this.setBorderColor(if (forCapture) Color.TRANSPARENT else Color.GREEN)
            this.setBorderStrokeWidth(0)
            
            setAutoFocus(config.android.useAutoFocus)
            if (!forCapture) {
                val restrictedFormats = mapRestrictedBarcodeTypes()
                if (restrictedFormats.isNotEmpty()) {
                    setFormats(restrictedFormats)
                }
            }
            setAspectTolerance(config.android.aspectTolerance.toFloat())
            if (config.autoEnableFlash) {
                flash = config.autoEnableFlash
                invalidateOptionsMenu()
            }
        }
        rootLayout?.addView(scannerView)

        val innerCircle = ShapeDrawable(OvalShape()).apply {
            paint.color = Color.WHITE
        }

        val outerRing = ShapeDrawable(OvalShape()).apply {
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = Color.WHITE
            paint.strokeWidth = 8f
        }

        val layers = arrayOf(outerRing, innerCircle)
        val layerDrawable = LayerDrawable(layers).apply {
            setLayerInset(1, 12, 12, 12, 12)
        }

        captureButton = ImageButton(this).apply {
            background = layerDrawable
            visibility = if (forCapture) View.VISIBLE else View.GONE
            val params = FrameLayout.LayoutParams(
                180,
                180,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            )
            params.bottomMargin = 100
            layoutParams = params

            setOnClickListener {
                it.isEnabled = false
                takePicture()
            }
        }
        rootLayout?.addView(captureButton)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        var buttonText = config.stringsMap["flash_on"]
        if (scannerView?.flash == true) {
            buttonText = config.stringsMap["flash_off"]
        }
        val flashButton = menu.add(0, TOGGLE_FLASH, 0, buttonText)
        flashButton.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        val cancelButton = menu.add(0, CANCEL, 0, config.stringsMap["cancel"])
        cancelButton.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == TOGGLE_FLASH) {
            scannerView?.toggleFlash()
            this.invalidateOptionsMenu()
            return true
        }
        if (item.itemId == CANCEL) {
            setResult(RESULT_CANCELED)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        scannerView?.stopCamera()
    }

    override fun onResume() {
        super.onResume()
        setupScannerView(forCapture = false)
        startCamera(forScanning = true)
    }

    private fun startCamera(forScanning: Boolean) {
        scannerView?.stopCamera()
        val handler = if (forScanning) this else null
        scannerView?.setResultHandler(handler)
        if (config.useCamera > -1) {
            scannerView?.startCamera(config.useCamera)
        } else {
            scannerView?.startCamera()
        }
    }

    override fun handleResult(result: Result?) {
        if (isCodeDetected) {
            return
        }
        isCodeDetected = true

        val builder = Protos.ScanResult.newBuilder()
        if (result == null) {
            builder.let {
                it.format = Protos.BarcodeFormat.unknown
                it.rawContent = "No data was scanned"
                it.type = Protos.ResultType.Error
            }
        } else {
            val format = (formatMap.filterValues { it == result.barcodeFormat }.keys.firstOrNull()
                ?: Protos.BarcodeFormat.unknown)

            var formatNote = ""
            if (format == Protos.BarcodeFormat.unknown) {
                formatNote = result.barcodeFormat.toString()
            }

            builder.let {
                it.format = format
                it.formatNote = formatNote
                it.rawContent = result.text
                it.type = Protos.ResultType.Barcode
            }
        }
        val scanResult = builder.build()

        if (config.withImage) {
            lastResult = scanResult
            
            setupScannerView(forCapture = true)
            startCamera(forScanning = false)

        } else {
            val intent = Intent()
            intent.putExtra(EXTRA_RESULT, scanResult.toByteArray())
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun mapRestrictedBarcodeTypes(): List<BarcodeFormat> {
        val types: MutableList<BarcodeFormat> = mutableListOf()

        this.config.restrictFormatList.filterNotNull().forEach {
            if (!formatMap.containsKey(it)) {
                print("Unrecognized")
                return@forEach
            }
            types.add(formatMap.getValue(it))
        }

        return types
    }
}
