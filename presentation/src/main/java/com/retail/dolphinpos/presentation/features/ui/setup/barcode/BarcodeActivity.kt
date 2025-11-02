package com.retail.dolphinpos.presentation.features.ui.setup.barcode

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.retail.dolphinpos.common.utils.snackbar.SnackBarManager
import com.retail.dolphinpos.common.utils.Constants
import com.retail.dolphinpos.data.setup.hardware.barcode.CameraXManager
import com.retail.dolphinpos.data.setup.hardware.barcode.TransparentOverlayView
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.databinding.ActivityBarcodeBinding
import com.retail.dolphinpos.presentation.features.base.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BarcodeActivity : AppCompatActivity() {

    @Inject
    lateinit var cameraXManager: CameraXManager

    @Inject
    lateinit var snackBarManager: SnackBarManager

    private val binding by viewBinding(ActivityBarcodeBinding::inflate)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                showPermissionDeniedDialog()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationaleDialog()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.camera_permission_title))
            .setMessage(getString(R.string.camera_permission_description))
            .setPositiveButton(getString(R.string.allow)) { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.camera_permission_denied))
            .setMessage(getString(R.string.camera_permission_denied_description))
            .setPositiveButton(getString(R.string.settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun startCamera() {

        val isQRCodeMode = intent.getBooleanExtra(Constants.IS_QR_CODE_MODE, false)
        if (isQRCodeMode) {
            binding.overlay.overlayShape = TransparentOverlayView.OverlayShape.SQUARE
        } else {
            binding.overlay.overlayShape = TransparentOverlayView.OverlayShape.RECTANGLE
        }

        cameraXManager.startCamera(
            lifecycleOwner = this,
            previewView = binding.previewView,
            cameraOverlay = binding.overlay,
            onBarcodeScanned = { barcode ->
                val resultIntent = Intent().apply {
                    putExtra(Constants.SCANNED_CODE, barcode)
                }
                setResult(RESULT_OK, resultIntent)

                snackBarManager.showInfoSnackBar(binding.root, "Scanned: $barcode")
                finish()
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraXManager.shutdown()
    }
}