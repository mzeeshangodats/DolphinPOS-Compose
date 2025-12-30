package com.retail.dolphinpos.data.repositories.label

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.UsbManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.brother.sdk.lmprinter.Channel
import com.brother.sdk.lmprinter.OpenChannelError
import com.brother.sdk.lmprinter.PrintError
import com.brother.sdk.lmprinter.PrinterDriverGenerator
import com.brother.sdk.lmprinter.PrinterModel
import com.brother.sdk.lmprinter.setting.PrintImageSettings
import com.brother.sdk.lmprinter.setting.PrintImageSettings.PrintQuality
import com.brother.sdk.lmprinter.setting.QLPrintSettings
import com.retail.dolphinpos.data.repositories.label.searchUsbPrinters
import com.retail.dolphinpos.data.setup.hardware.printer.DiscoveryManager
import com.retail.dolphinpos.data.setup.hardware.printer.PrinterManager
import com.retail.dolphinpos.data.util.LabelBitmapGenerator
import com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo
import com.retail.dolphinpos.domain.model.label.Label
import com.retail.dolphinpos.domain.repositories.label.LabelPrinterRepository
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.GetPrinterDetailsUseCase
import com.starmicronics.stario10.StarConnectionSettings
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.printer.Alignment
import com.starmicronics.stario10.starxpandcommand.printer.CutType
import com.starmicronics.stario10.starxpandcommand.printer.ImageParameter
import com.starmicronics.stario10.InterfaceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import javax.inject.Inject

class LabelPrinterRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val printerManager: PrinterManager,
    private val getPrinterDetailsUseCase: GetPrinterDetailsUseCase,
    private val labelBitmapGenerator: LabelBitmapGenerator,
    private val logRepository: LogRepository

) : LabelPrinterRepository {

    private var currentPrinter: StarPrinter? = null
    private var isPrinting = false
    private var printJob: Job? = null // Used for canceling print jobs

    override suspend fun getAvailablePrinters(): List<DiscoveredPrinterInfo> =
        withContext(Dispatchers.IO) {
            val models = arrayOf("QL-800", "QL-810W", "QL-820NWB") // target models
            try {
                searchUsbPrinters(context, models)
            } catch (e: Exception) {
                throw e
            }
        }

    override suspend fun connectAndPrintLabels(
        printerDevice: DiscoveredPrinterInfo,
        labels: List<Label>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        printJob = coroutineContext.job
        logRepository.logToFile("==================================")
        logRepository.logToFile("🔹 connectAndPrintLabel STARTED")
        logRepository.logToFile("Labels received: ${labels.size}")

        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            logRepository.logToFile("✅ USB Manager retrieved")

            val device =
                usbManager.deviceList.values.find { it.productName == printerDevice.modelName }
            if (device == null) {
                logRepository.logToFile("❌ Error: Printer ${printerDevice.modelName} not found")
                return@withContext Result.failure(Exception("Selected printer not found"))
            }

            logRepository.logToFile("✅ Printer found: ${device.deviceName}")

            if (!usbManager.hasPermission(device)) {
                logRepository.logToFile("❌ Error: No USB permission for ${device.deviceName}")
                return@withContext Result.failure(Exception("USB permission not granted"))
            }

            logRepository.logToFile("✅ USB permission granted")

            val channel = Channel.newUsbChannel(usbManager)
            val result = PrinterDriverGenerator.openChannel(channel)

            if (result.error.code != OpenChannelError.ErrorCode.NoError) {
                logRepository.logToFile("❌ Error: Failed to open USB channel - ${result.error.code}")
                return@withContext Result.failure(Exception("Failed to open USB channel: ${result.error.code}"))
            }

            logRepository.logToFile("✅ USB channel opened successfully")

            val printerDriver = result.driver
            val mediaInfo = printerDriver.printerStatus.printerStatus.mediaInfo
            logRepository.logToFile("✅ Printer driver ready")

            val estimatedTimePerLabel = 1500L  // Adjust based on real printer speed

            // Process each label sequentially
            for (label in labels) {
                if (printJob?.isActive == false) {
                    logRepository.logToFile("❌ Print job canceled before printing")
                    return@withContext Result.failure(Exception("Print job canceled"))
                }

                logRepository.logToFile("🖨️ Printing Label: ${label.productName}, Quantity: ${label.printQuantity}")

                val qlPrintSettings = QLPrintSettings(PrinterModel.QL_800).apply {
//                    labelSize = LabelSize.RollW62RB
                    labelSize = mediaInfo?.qlLabelSize
                    workPath = StorageUtils.getInternalFolder(context)
                    isAutoCut = true // Cuts After every Print
                    numCopies = label.printQuantity.coerceAtLeast(1) // No need to loop
//                    isCutAtEnd = true

                    printQuality = PrintQuality.Best
                    halftone = PrintImageSettings.Halftone.ErrorDiffusion
                    compress = PrintImageSettings.CompressMode.None
                    resolution = PrintImageSettings.Resolution.High
                }

                val labelBitmap = labelBitmapGenerator.createLabelBitmapFromLayout(label)
                val printResult = printerDriver.printImage(labelBitmap, qlPrintSettings)
                logRepository.logToFile("Print result: ${printResult.code}, Description: ${printResult.errorDescription}")

                if (printResult.code == PrintError.ErrorCode.NoError) {
                    logRepository.logToFile("✅ Print success: $label")
                } else {
                    logRepository.logToFile("❌ Print failed: ${printResult.code} | ${printResult.errorDescription}")
                    return@withContext Result.failure(Exception("Printing failed"))
                }

                val delayTime = estimatedTimePerLabel * label.printQuantity
                logRepository.logToFile("⏳ Waiting $delayTime ms before next label...")
                delay(delayTime)
            }

            printerDriver.closeChannel()
            logRepository.logToFile("✅ Printing complete, channel closed")
            logRepository.logToFile("==================================")

            Result.success(Unit)

        } catch (t: Throwable) {
            logRepository.logToFile("❌ Exception: ${t.localizedMessage}")
            Result.failure(t)
        }
    }


 /*   override suspend fun connectAndPrintLabel(
        printerDevice: DiscoveredPrinterInfo,
        label: Bitmap,
        printQuantity: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        printJob = coroutineContext.job
        logRepository.logToFile("==================================")
        logRepository.logToFile("🔹 connectAndPrintLabel STARTED")
        logRepository.logToFile("Labels received: $label")

        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            logRepository.logToFile("✅ USB Manager retrieved")

            val device =
                usbManager.deviceList.values.find { it.productName == printerDevice.modelName }
            if (device == null) {
                logRepository.logToFile("❌ Error: Printer ${printerDevice.modelName} not found")
                return@withContext Result.failure(Exception("Selected printer not found"))
            }

            logRepository.logToFile("✅ Printer found: ${device.deviceName}")

            if (!usbManager.hasPermission(device)) {
                logRepository.logToFile("❌ Error: No USB permission for ${device.deviceName}")
                return@withContext Result.failure(Exception("USB permission not granted"))
            }

            logRepository.logToFile("✅ USB permission granted")

            val channel = Channel.newUsbChannel(usbManager)
            val result = PrinterDriverGenerator.openChannel(channel)

            if (result.error.code != OpenChannelError.ErrorCode.NoError) {
                logRepository.logToFile("❌ Error: Failed to open USB channel - ${result.error.code}")
                return@withContext Result.failure(Exception("Failed to open USB channel: ${result.error.code}"))
            }

            logRepository.logToFile("✅ USB channel opened successfully")

            val printerDriver = result.driver
            val mediaInfo = printerDriver.printerStatus.printerStatus.mediaInfo
            logRepository.logToFile("✅ Printer driver ready")


            val qlPrintSettings = QLPrintSettings(PrinterModel.QL_800).apply {
                labelSize = mediaInfo?.qlLabelSize
                workPath = StorageUtils.getInternalFolder(context)
                isAutoCut = true
                numCopies = printQuantity.coerceAtLeast(1)
                isCutAtEnd = true
            }

            logRepository.logToFile("🖨️ QL Settings: ${qlPrintSettings.labelSize}")
            logRepository.logToFile("🖨️ QL Settings: ${mediaInfo?.mediaType?.name}")
            logRepository.logToFile("🖨️ QL Settings: qlLabelSize_raw  ${mediaInfo?.qlLabelSize_raw}")
            logRepository.logToFile("🖨️ QL Settings: qlLabelSize ${mediaInfo?.qlLabelSize}")
            logRepository.logToFile("🖨️ QL Settings: backgroundColor ${mediaInfo?.backgroundColor}")
            logRepository.logToFile("🖨️ QL Settings: height_mm ${mediaInfo?.height_mm}")
            logRepository.logToFile("🖨️ QL Settings: width_mm ${mediaInfo?.width_mm}")
            logRepository.logToFile("🖨️ QL Settings: inkColor ${mediaInfo?.inkColor}")
            logRepository.logToFile("🖨️ QL Settings: ${qlPrintSettings.workPath}")
            logRepository.logToFile("🖨️ QL Settings: ${qlPrintSettings.isAutoCut}")
            logRepository.logToFile("🖨️ QL Settings: ${qlPrintSettings.numCopies}")

            if (printJob?.isActive == false) {
                logRepository.logToFile("❌ Print job canceled before printing")
                return@withContext Result.failure(Exception("Print job canceled"))
            }


            delay(500)
            val printResult = printerDriver.printImage(label, qlPrintSettings)
            logRepository.logToFile("Print result: ${printResult.code}, Description: ${printResult.errorDescription}")

            if (printResult.code == PrintError.ErrorCode.NoError) {
                logRepository.logToFile("✅ Print success: $label")
            } else {
                logRepository.logToFile("❌ Print failed: ${printResult.code} | ${printResult.errorDescription}")
                return@withContext Result.failure(Exception("Printing failed"))
            }

            printerDriver.closeChannel()
            logRepository.logToFile("✅ Printing complete, channel closed")
            logRepository.logToFile("==================================")

            Result.success(Unit)

        } catch (e: Exception) {
            logRepository.logToFile("❌ Exception: ${e.localizedMessage}")
            Result.failure(e)
        }
    }*/


    override suspend fun cancelPrintJob(): Result<Unit> {
        printJob?.cancel()
        //logRepository.logToFile("Print job canceled")
        return Result.success(Unit)
    }

    object StorageUtils {
        fun getInternalFolder(context: Context): String {
            return context.filesDir.absolutePath
        }

        fun getExternalFolder(context: Context): String {
            return context.getExternalFilesDir(null)?.absolutePath ?: ""
        }

        fun getSelectFileUri(context: Context, uri: Uri): String? {
            return kotlin.runCatching {
                val file = File(selectFileTemDir(context, uri))
                if (file.exists()) {
                    file.delete()
                }
                file.parentFile?.mkdirs()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                file.path
            }.getOrNull()
        }

        fun deleteSelectFolder(context: Context) {
            File(context.externalCacheDir?.path + File.separator + "select").delete()
        }

        private fun selectFileTemDir(context: Context, src: Uri): String {
            return context.externalCacheDir?.path + File.separator + "select" + File.separator + src.originalFileName(context)
        }

        private fun Uri.originalFileName(context: Context): String = when (scheme) {
            "content" -> {
                context.contentResolver.query(this, null, null, null, null, null)?.use {
                    if (!it.moveToFirst()) return@use ""
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    return@use if (index < 0) "" else it.getString(index)
                } ?: ""
            }
            else -> {
                this.lastPathSegment ?: ""
            }
        }

        fun Uri.hasFileWithExtension(extension: String, context: Context): Boolean {
            return originalFileName(context).endsWith(".$extension", true)
        }
    }
}
