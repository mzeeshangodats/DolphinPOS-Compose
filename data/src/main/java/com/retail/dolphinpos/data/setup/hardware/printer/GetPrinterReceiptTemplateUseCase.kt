//package com.retail.dolphinpos.data.setup.hardware.printer
//
//import android.annotation.SuppressLint
//import com.lingeriepos.common.usecases.printer.receipt.GenerateReceiptTextUseCase
//import com.lingeriepos.common.usecases.store.GetStoreDetailsFromLocalUseCase
//import com.lingeriepos.common.usecases.user.GetUserDetailUseCase
//import com.lingeriepos.models.response.SingleOrder
//import com.lingeriepos.utils.createCenteredLogoBitmap
//import com.lingeriepos.utils.formatTimeString
//import com.lingeriepos.utils.getCurrentDateUsFormat
//import com.lingeriepos.utils.getCurrentTimeFormatted
//import com.lingeriepos.utils.toOrderDateFormat
//import com.retail.dolphinpos.domain.usecases.auth.GetUserDetailUseCase
//import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
//import com.starmicronics.stario10.starxpandcommand.MagnificationParameter
//import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
//import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
//import com.starmicronics.stario10.starxpandcommand.printer.Alignment
//import com.starmicronics.stario10.starxpandcommand.printer.BarcodeParameter
//import com.starmicronics.stario10.starxpandcommand.printer.BarcodeSymbology
//import com.starmicronics.stario10.starxpandcommand.printer.CutType
//import com.starmicronics.stario10.starxpandcommand.printer.ImageParameter
//import com.starmicronics.stario10.starxpandcommand.printer.TextEllipsizeType
//import com.starmicronics.stario10.starxpandcommand.printer.TextParameter
//import com.starmicronics.stario10.starxpandcommand.printer.TextWidthParameter
//
//class GetPrinterReceiptTemplateUseCase(
//    val createImageParameterFromTextUseCase: CreateImageParameterFromTextUseCase,
//    val generateReceiptTextUseCase: GenerateReceiptTextUseCase,
//    //val generateBarcodeImageUseCase: GenerateBarcodeImageUseCase,
//    val getStoreDetailsFromLocalUseCase: GetStoreDetailsFromLocalUseCase,
//    val getUserDetailUseCase: GetUserDetailUseCase,
//    val downloadAndUpdateCachedImageUseCase: DownloadAndUpdateCachedImageUseCase,
//    val getAppLogo: GetAppLogoUseCase,
//) {
//
//    @SuppressLint("DefaultLocale")
//    suspend operator fun invoke(
//        order: SingleOrder?,
//        isGraphic: Boolean = false,
//        isReceiptForRefund: Boolean = false
//    ): String {
//        val builder = StarXpandCommandBuilder()
//
//        order?.let {
//
//            val logo = downloadAndUpdateCachedImageUseCase(getStoreDetailsFromLocalUseCase()?.logo?.fileURL)
//
//            val centeredLogo = logo?.let { createCenteredLogoBitmap(it) }
//
//
//            if (isGraphic) {
//
//                val printerBuilder = PrinterBuilder().styleAlignment(Alignment.Center)
//
//                centeredLogo?.let {
//                    printerBuilder.actionPrintImage(ImageParameter(it, 600))
//                }
//
//                    printerBuilder
//                    .actionPrintText("\n\n\n")
//                    .actionPrintImage(
//                        createImageParameterFromTextUseCase(
//                            generateReceiptTextUseCase(
//                                order,
//                                isReceiptForRefund = isReceiptForRefund
//                            )
//                        )
//                    )
//                    .actionPrintText("\n\n")
//                    .styleAlignment(Alignment.Center)
//                    .actionPrintImage(generateBarcodeImageUseCase(order.orderNumber!!))
//                    .actionCut(CutType.Partial)
//
//
//                builder.addDocument(DocumentBuilder().addPrinter(printerBuilder))
//
//            } /*else {
//
//
//                val store = order.store
//                val orderItems = order.orderItems ?: emptyList()
//                val storePolicy = getStoreDetailsFromLocalUseCase()?.policy ?: ""
//
//                val date =
//                    if (isReceiptForRefund) getCurrentDateUsFormat() else order.createdAt?.toOrderDateFormat()
//                        ?: getCurrentDateUsFormat()
//                val time =
//                    if (isReceiptForRefund) getCurrentTimeFormatted() else order.createdAt?.formatTimeString()
//                        ?: getCurrentTimeFormatted()
//
//                val subtotal = order.subTotal?.let { if (isReceiptForRefund) -it else it } ?: 0.0
//                val tax = order.taxValue?.let { if (isReceiptForRefund) -it else it } ?: 0.0
//                val discount =
//                    order.discountAmount?.let { if (isReceiptForRefund) -it else it } ?: 0.0
//                val total = order.total?.let { if (isReceiptForRefund) -it else it } ?: 0.0
//                val totalAmountLabel =
//                    if (isReceiptForRefund) "TOTAL REFUND AMOUNT:" else "TOTAL AMOUNT:"
//
//
//                val divider = "-----------------------------------------\n"
//
//                val receiptTitle = if(isReceiptForRefund) "REFUND RECEIPT" else "RECEIPT"
//                var productVariant = ""
//
//
//                builder.addDocument(
//                    DocumentBuilder()
//                        .settingPrintableArea(80.0)
//                        .addPrinter(
//                            PrinterBuilder()
//                                .styleAlignment(Alignment.Center)
//                                .apply { logo?.let { actionPrintImage(ImageParameter(it, 200)) } }
//                                .actionFeedLine(1)
//                                .add(
//                                    PrinterBuilder()
//                                        .styleBold(true)
//                                        .styleMagnification(MagnificationParameter(2, 2))
//                                        .actionPrintText("${store?.name?.uppercase() ?: getStoreDetailsFromLocalUseCase()?.name}\n")
//                                )
//                                .actionPrintText("${store?.location ?: getStoreDetailsFromLocalUseCase()?.location}\n\n")
//                                .styleAlignment(Alignment.Left)
//                                .actionPrintText("Order No   : ${order.orderNumber?.take(30) ?: ""}\n")
//                                .actionPrintText(
//                                    "Date : $date   Time : $time\n",
//                                    TextParameter().setWidth(
//                                        48,
//                                        TextWidthParameter().setEllipsizeType(TextEllipsizeType.End)
//                                    )
//                                )
//                                .actionFeedLine(1)
//                                .actionPrintText(centerAlign(receiptTitle) + "\n")
//                                .actionPrintText(divider)
//                                *//*.actionPrintText(
//                                    "Description       Qty  Price       Total\n",
//                                    TextParameter().setWidth(
//                                        48,
//                                        TextWidthParameter().setEllipsizeType(TextEllipsizeType.End)
//                                    )
//                                )*//*
//                                .actionPrintText(
//                                    "Description            Total\n",
//                                    TextParameter().setWidth(
//                                        48,
//                                        TextWidthParameter().setEllipsizeType(TextEllipsizeType.End)
//                                    )
//                                )
//                                .actionPrintText(divider)
//                                .actionFeedLine(1)
//                                .also { printer ->
//                                    val consolidatedItems = orderItems
//                                        .groupBy {
//                                            val product = it.product
//                                            val variant = it.productVariant
//                                            if (isReceiptForRefund) {
//                                                if (variant != null) "variant:${variant.id}" else "product:${product?.id}"
//                                            } else
//                                                if (product?.variantId != null) "variant:${product.variantId}" else "product:${product?.id}"
//
//                                        }
//                                        .map { (_, items) ->
//                                            items.first().copy(
//                                                quantity = items.sumOf { it.quantity ?: 0 },
//                                                price = items.first().price
//                                            )
//                                        }
//                                    consolidatedItems.forEach { item ->
//                                        val price = item.price?.toDouble() ?: 0.0
//                                        val quantity = item.quantity ?: 0
//                                        val totalItem = price * quantity
//                                        val effectivePrice =
//                                            if (isReceiptForRefund) -price else price
//                                        val effectiveTotal =
//                                            if (isReceiptForRefund) -totalItem else totalItem
//
//                                        val productNameString = item.product?.name ?: "-"
//                                        val parts =
//                                            productNameString.split("\n", limit = 2)
//
//                                        val mainProductName = parts[0].trim()
//                                        if (parts.size > 1)
//                                            productVariant = parts[1].trim()
//                                        else {
//                                            productVariant = item.productVariant?.title ?: ""
//                                        }
//
//                                        printer.actionPrintText(
//                                            *//*String.format(
//                                                "%-15s %4d %6.2f %12.2f\n",
//                                                item.product?.name?.take(15) ?: "-",
//                                                quantity,
//                                                effectivePrice,
//                                                effectiveTotal
//                                            ),*//*
//                                            String.format(
//                                                "%-26s %12.2f\n",
//                                                mainProductName,
//                                                effectiveTotal
//                                            ),
//                                            TextParameter().setWidth(
//                                                48,
//                                                TextWidthParameter().setEllipsizeType(
//                                                    TextEllipsizeType.End
//                                                )
//                                            )
//                                        )
//                                    }
//                                }
//                                .actionPrintText(
//                                    productVariant,
//                                    TextParameter().setWidth(
//                                        48,
//                                        TextWidthParameter().setEllipsizeType(TextEllipsizeType.End)
//                                    )
//                                )
//                                .actionPrintText(
//                                    "\n",
//                                    TextParameter().setWidth(
//                                        48,
//                                        TextWidthParameter().setEllipsizeType(TextEllipsizeType.End)
//                                    )
//                                )
//                                .actionPrintText(divider)
//                                .actionPrintText(
//                                    String.format(
//                                        "%-25s %14s\n",
//                                        "DISCOUNT:",
//                                        "$%.2f".format(discount)
//                                    ),
//                                    TextParameter().setWidth(
//                                        48,
//                                        TextWidthParameter().setEllipsizeType(TextEllipsizeType.End)
//                                    )
//                                )
//                                .actionPrintText(
//                                    String.format(
//                                        "%-25s %14s\n",
//                                        "SUBTOTAL:",
//                                        "$%.2f".format(subtotal)
//                                    ),
//                                    TextParameter().setWidth(
//                                        48,
//                                        TextWidthParameter().setEllipsizeType(TextEllipsizeType.End)
//                                    )
//                                )
//                                .actionPrintText(
//                                    String.format("%-25s %14s\n", "TAX:", "$%.2f".format(tax)),
//                                    TextParameter().setWidth(
//                                        48,
//                                        TextWidthParameter().setEllipsizeType(TextEllipsizeType.End)
//                                    )
//                                )
//                                .actionPrintText(
//                                    "\n",
//                                    TextParameter().setWidth(
//                                        48,
//                                        TextWidthParameter().setEllipsizeType(TextEllipsizeType.End)
//                                    )
//                                )
//                                .actionPrintText(divider)
//                                .actionPrintText(
//                                    String.format(
//                                        "%-25s %14s\n",
//                                        totalAmountLabel,
//                                        "$%.2f".format(total)
//                                    ),
//                                    TextParameter().setWidth(
//                                        48,
//                                        TextWidthParameter().setEllipsizeType(TextEllipsizeType.End)
//                                    )
//                                )
//                                .actionPrintText(divider)
//                                .actionPrintText("\n\n")
//                                .actionPrintText(
//                                    storePolicy.split(" ")
//                                        .chunked(5)
//                                        .joinToString("\n") { line ->
//                                            centerAlign(line.joinToString(" "))
//                                        } + "\n"
//                                )
//                                .actionPrintText("\n")
//                                .styleAlignment(Alignment.Center)
//                                .actionPrintBarcode(
//                                    BarcodeParameter(
//                                        order.orderNumber!!,
//                                        symbology = BarcodeSymbology.Code128
//                                    )
//                                        .setPrintHri(true)
//                                )
//                                .actionPrintText("\n\n")
//                                .actionPrintText("Thank you for shopping with us.")
//                                .actionCut(CutType.Partial)
//
//                        )
//
//                )
//            }*/
//
//        } ?: run {
//            builder.addDocument(
//                DocumentBuilder()
//                    .addPrinter(
//                        PrinterBuilder()
//                            .styleAlignment(Alignment.Left)
//                            .actionPrintText("Order was empty")
//                            .actionCut(CutType.Partial)
//                    )
//            )
//        }
//        return builder.getCommands()
//    }
//
//    private fun centerAlign(text: String): String =
//        text.padStart((48 + text.length) / 2, ' ').padEnd(48, ' ')
//
//
//}