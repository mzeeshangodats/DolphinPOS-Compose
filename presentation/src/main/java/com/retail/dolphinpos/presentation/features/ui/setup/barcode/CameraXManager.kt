//package com.retail.dolphinpos.presentation.features.ui.setup.barcode.barcode
//
//import android.content.Context
//import android.graphics.Rect
//import androidx.annotation.OptIn
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ExperimentalGetImage
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.view.PreviewView
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.LifecycleOwner
//import com.google.mlkit.vision.barcode.BarcodeScanner
//import com.google.mlkit.vision.common.InputImage
//import dagger.hilt.android.qualifiers.ApplicationContext
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//import javax.inject.Inject
//
//open class CameraXManager @Inject constructor(
//    @ApplicationContext private val context: Context,
//    private val barcodeScanner: BarcodeScanner
//) {
//
//    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
//
//
//    fun startCamera(
//        lifecycleOwner: LifecycleOwner,
//        previewView: PreviewView,
//        cameraOverlay: TransparentOverlayView,
//        onBarcodeScanned: (String) -> Unit
//    ) {
//
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
//        cameraProviderFuture.addListener({
//                                             val cameraProvider = cameraProviderFuture.get()
//
//                                             val preview = Preview.Builder()
//                                                 .build().apply {
//                                                     surfaceProvider = previewView.surfaceProvider
//                                                 }
//
//                                             val analysisUseCase = ImageAnalysis.Builder()
//                                                 .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                                                 .build()
//
//                                             analysisUseCase.setAnalyzer(analysisExecutor) { imageProxy ->
//                                                 processImageProxy(
//                                                     imageProxy,
//                                                     cameraOverlay,
//                                                     onBarcodeScanned
//                                                 )
//                                             }
//
//                                             val hasBackCamera = CameraSelector.DEFAULT_BACK_CAMERA
//                                                 .filter(cameraProvider.availableCameraInfos)
//                                                 .isNotEmpty()
//
//                                             val cameraSelector = if (hasBackCamera) {
//                                                 CameraSelector.DEFAULT_BACK_CAMERA
//                                             } else {
//                                                 CameraSelector.DEFAULT_FRONT_CAMERA
//                                             }
//
//                                             try {
//                                                 cameraProvider.unbindAll()
//                                                 cameraProvider.bindToLifecycle(
//                                                     lifecycleOwner,
//                                                     cameraSelector,
//                                                     preview,
//                                                     analysisUseCase
//                                                 )
//                                             } catch (e: Exception) {
//                                                 e.printStackTrace()
//                                             }
//                                         }, ContextCompat.getMainExecutor(context))
//    }
//
//    @OptIn(ExperimentalGetImage::class)
//    private fun processImageProxy(
//        imageProxy: ImageProxy,
//        cameraOverlay: TransparentOverlayView,
//        onBarcodeScanned: (String) -> Unit
//    ) {
//        val mediaImage = imageProxy.image ?: run {
//            imageProxy.close()
//            return
//        }
//
//        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//        val overlayRect = getOverlayBounds(cameraOverlay)
//
//        barcodeScanner.process(image)
//            .addOnSuccessListener { barcodes ->
//                barcodes.forEach { barcode ->
//                    // TODO Temporarily disabled.
////                    val boundingBox = barcode.boundingBox
////                    if (boundingBox != null && overlayRect.contains(boundingBox)) {
////                        barcode.displayValue?.let { onBarcodeScanned(it) }
////                    }
//                    barcode.displayValue?.let { onBarcodeScanned(it) }
//
//                }
//            }
//            .addOnFailureListener { e ->
//
//                e.printStackTrace()
//            }
//            .addOnCompleteListener {
//                imageProxy.close()
//            }
//    }
//
//    private fun getOverlayBounds(overlay: TransparentOverlayView): Rect {
//        val rectF = overlay.getTransparentRect()
//        return Rect(
//            rectF.left.toInt(),
//            rectF.top.toInt(),
//            rectF.right.toInt(),
//            rectF.bottom.toInt()
//        )
//    }
//
//    fun shutdown() {
//        analysisExecutor.shutdown()
//    }
//}