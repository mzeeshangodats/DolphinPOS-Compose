package com.retail.dolphinpos.data.di

import android.content.Context
import com.retail.dolphinpos.data.setup.hardware.payment.pax.CancelTransactionUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.payment.pax.CloseBatchUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.payment.pax.GetHttpSettingsUseCase
import com.retail.dolphinpos.data.setup.hardware.payment.pax.GetHttpSettingsUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.payment.pax.GetTcpSettingsUseCase
import com.retail.dolphinpos.data.setup.hardware.payment.pax.GetTcpSettingsUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.payment.pax.InitializeTerminalUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.payment.pax.ProcessTransactionUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.printer.ConnectPrinterUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.printer.CreateBitmapFromTextUseCase
import com.retail.dolphinpos.data.setup.hardware.printer.CreateImageParameterFromTextUseCase
import com.retail.dolphinpos.data.setup.hardware.printer.GetAppLogoUseCase
import com.retail.dolphinpos.data.setup.hardware.printer.GetPrinterDetailsUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.printer.OpenCashDrawerUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.printer.SavePrinterDetailsUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.printer.StartDiscoveryUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.printer.TestPrintUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.printer.PrintOrderReceiptUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.printer.GetPrinterReceiptTemplateUseCase
import com.retail.dolphinpos.data.setup.hardware.printer.DownloadAndUpdateCachedImageUseCase
import com.retail.dolphinpos.data.setup.hardware.receipt.GenerateBarcodeImageUseCase
import com.retail.dolphinpos.data.setup.hardware.receipt.GenerateReceiptTextUseCase
import com.retail.dolphinpos.data.setup.hardware.receipt.GetStoreDetailsFromLocalUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.CancelTransactionUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.CloseBatchUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.InitializeTerminalUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.ProcessTransactionUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.ConnectPrinterUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.GetPrinterDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.OpenCashDrawerUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.SavePrinterDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.StartDiscoveryUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.TestPrintUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.PrintOrderReceiptUseCase
import com.retail.dolphinpos.domain.usecases.order.GetLastPendingOrderUseCase
import com.retail.dolphinpos.data.usecases.order.GetLastPendingOrderUseCaseImpl
import com.retail.dolphinpos.domain.usecases.order.GetLatestOnlineOrderUseCase
import com.retail.dolphinpos.domain.usecases.order.GetPrintableOrderFromOrderDetailUseCase
import com.retail.dolphinpos.data.usecases.home.GetProductByBarcodeUseCaseImpl
import com.retail.dolphinpos.data.usecases.order.GetLatestOnlineOrderUseCaseImpl
import com.retail.dolphinpos.data.usecases.order.GetPrintableOrderFromOrderDetailUseCaseImpl
import com.retail.dolphinpos.domain.usecases.home.GetProductByBarcodeUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SetupModule {

    @Binds
    @Singleton
    abstract fun bindInitializeTerminalUseCase(
        impl: InitializeTerminalUseCaseImpl
    ): InitializeTerminalUseCase

    @Binds
    @Singleton
    abstract fun bindProcessTransactionUseCase(
        impl: ProcessTransactionUseCaseImpl
    ): ProcessTransactionUseCase

    @Binds
    @Singleton
    abstract fun bindCancelTransactionUseCase(
        impl: CancelTransactionUseCaseImpl
    ): CancelTransactionUseCase

    @Binds
    @Singleton
    abstract fun bindCloseBatchUseCase(
        impl: CloseBatchUseCaseImpl
    ): CloseBatchUseCase

    @Binds
    @Singleton
    abstract fun bindGetTcpSettingsUseCase(
        impl: GetTcpSettingsUseCaseImpl
    ): GetTcpSettingsUseCase

    @Binds
    @Singleton
    abstract fun bindGetHttpSettingsUseCase(
        impl: GetHttpSettingsUseCaseImpl
    ): GetHttpSettingsUseCase

    @Binds
    @Singleton
    abstract fun bindGetPrinterDetailsUseCase(
        impl: GetPrinterDetailsUseCaseImpl
    ): GetPrinterDetailsUseCase

    @Binds
    @Singleton
    abstract fun bindSavePrinterDetailsUseCase(
        impl: SavePrinterDetailsUseCaseImpl
    ): SavePrinterDetailsUseCase

    @Binds
    @Singleton
    abstract fun bindTestPrintUseCase(
        impl: TestPrintUseCaseImpl
    ): TestPrintUseCase

    @Binds
    @Singleton
    abstract fun bindStartDiscoveryUseCase(
        impl: StartDiscoveryUseCaseImpl
    ): StartDiscoveryUseCase

    @Binds
    @Singleton
    abstract fun bindConnectPrinterUseCase(
        impl: ConnectPrinterUseCaseImpl
    ): ConnectPrinterUseCase

    @Binds
    @Singleton
    abstract fun bindOpenCashDrawerUseCase(
        impl: OpenCashDrawerUseCaseImpl
    ): OpenCashDrawerUseCase

    @Binds
    @Singleton
    abstract fun bindGetLastPendingOrderUseCase(
        impl: GetLastPendingOrderUseCaseImpl
    ): GetLastPendingOrderUseCase

    @Binds
    @Singleton
    abstract fun bindGetProductByBarcodeUseCase(
        impl: GetProductByBarcodeUseCaseImpl
    ): GetProductByBarcodeUseCase

    @Binds
    @Singleton
    abstract fun bindGetLatestOnlineOrderUseCase(
        impl: GetLatestOnlineOrderUseCaseImpl
    ): GetLatestOnlineOrderUseCase

    @Binds
    @Singleton
    abstract fun bindGetPrintableOrderFromOrderDetailUseCase(
        impl: GetPrintableOrderFromOrderDetailUseCaseImpl
    ): GetPrintableOrderFromOrderDetailUseCase

    @Binds
    @Singleton
    abstract fun bindPrintOrderReceiptUseCase(
        impl: PrintOrderReceiptUseCaseImpl
    ): PrintOrderReceiptUseCase

    companion object {
        @Provides
        @Singleton
        fun provideGetAppLogoUseCase(
            @ApplicationContext context: Context
        ): GetAppLogoUseCase {
            return GetAppLogoUseCase(context)
        }

        @Provides
        @Singleton
        fun provideCreateBitmapFromTextUseCase(): CreateBitmapFromTextUseCase {
            return CreateBitmapFromTextUseCase()
        }

        @Provides
        @Singleton
        fun provideCreateImageParameterFromTextUseCase(
            createBitmapFromTextUseCase: CreateBitmapFromTextUseCase
        ): CreateImageParameterFromTextUseCase {
            return CreateImageParameterFromTextUseCase(createBitmapFromTextUseCase)
        }

        @Provides
        @Singleton
        fun provideGenerateBarcodeImageUseCase(): GenerateBarcodeImageUseCase {
            return GenerateBarcodeImageUseCase()
        }

        @Provides
        @Singleton
        fun provideGetStoreDetailsFromLocalUseCase(
            getStoreDetailsUseCase: com.retail.dolphinpos.domain.usecases.auth.GetStoreDetailsUseCase
        ): GetStoreDetailsFromLocalUseCase {
            return GetStoreDetailsFromLocalUseCase(getStoreDetailsUseCase)
        }

        @Provides
        @Singleton
        fun provideDownloadAndUpdateCachedImageUseCase(
            @ApplicationContext context: Context
        ): DownloadAndUpdateCachedImageUseCase {
            return DownloadAndUpdateCachedImageUseCase(context)
        }

        @Provides
        @Singleton
        fun provideGetPrinterReceiptTemplateUseCase(
            createImageParameterFromTextUseCase: CreateImageParameterFromTextUseCase,
            generateReceiptTextUseCase: GenerateReceiptTextUseCase,
            generateBarcodeImageUseCase: GenerateBarcodeImageUseCase,
            getStoreDetailsFromLocalUseCase: GetStoreDetailsFromLocalUseCase,
            downloadAndUpdateCachedImageUseCase: DownloadAndUpdateCachedImageUseCase,
            getAppLogoUseCase: GetAppLogoUseCase
        ): GetPrinterReceiptTemplateUseCase {
            return GetPrinterReceiptTemplateUseCase(
                createImageParameterFromTextUseCase = createImageParameterFromTextUseCase,
                generateReceiptTextUseCase = generateReceiptTextUseCase,
                generateBarcodeImageUseCase = generateBarcodeImageUseCase,
                getStoreDetailsFromLocalUseCase = getStoreDetailsFromLocalUseCase,
                downloadAndUpdateCachedImageUseCase = downloadAndUpdateCachedImageUseCase,
                getAppLogoUseCase = getAppLogoUseCase
            )
        }
    }
}