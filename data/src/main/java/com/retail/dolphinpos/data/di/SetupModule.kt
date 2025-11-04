package com.retail.dolphinpos.data.di

import com.retail.dolphinpos.data.setup.hardware.payment.pax.CancelTransactionUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.payment.pax.GetHttpSettingsUseCase
import com.retail.dolphinpos.data.setup.hardware.payment.pax.GetHttpSettingsUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.payment.pax.GetTcpSettingsUseCase
import com.retail.dolphinpos.data.setup.hardware.payment.pax.GetTcpSettingsUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.payment.pax.InitializeTerminalUseCaseImpl
import com.retail.dolphinpos.data.setup.hardware.payment.pax.ProcessTransactionUseCaseImpl
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.CancelTransactionUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.InitializeTerminalUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.ProcessTransactionUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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

}