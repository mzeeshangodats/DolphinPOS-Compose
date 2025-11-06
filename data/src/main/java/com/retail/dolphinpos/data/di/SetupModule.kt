package com.retail.dolphinpos.data.di

import android.content.Context
import com.retail.dolphinpos.data.setup.hardware.payment.pax.CancelTransactionUseCaseImpl
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
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.CancelTransactionUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.InitializeTerminalUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.ProcessTransactionUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.ConnectPrinterUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.GetPrinterDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.OpenCashDrawerUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.SavePrinterDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.StartDiscoveryUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.TestPrintUseCase
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
    }
}