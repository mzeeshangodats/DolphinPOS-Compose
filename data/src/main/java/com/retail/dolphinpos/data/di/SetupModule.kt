package com.retail.dolphinpos.data.di

import dagger.Provides

object SetupModule {

    @Provides
    fun provideGetTcpSettingsUseCase(getPaxDetailsUseCase: GetPaxDetailsUseCase): GetTcpSettingsUseCase {
        return GetTcpSettingsUseCase(getPaxDetailsUseCase)
    }

    @Provides
    fun provideGetHttpSettingsUseCase(getPaxDetailsUseCase: GetPaxDetailsUseCase): GetHttpSettingsUseCase {
        return GetHttpSettingsUseCase(getPaxDetailsUseCase)
    }

}