package com.retail.dolphinpos.data.setup.hardware.payment.pax

import android.util.Log
import com.pax.poscore.commsetting.HttpSetting
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.GetPaxDetailsUseCase
import javax.inject.Inject

class GetHttpSettingsUseCase  @Inject constructor(val getPaxDetailsUseCase: GetPaxDetailsUseCase) {

    suspend operator fun invoke(
        ipAddress: String,
        port: String,
        isTestConnection : Boolean = false
    ): HttpSetting {
        val httpSettings = HttpSetting()

        if(!isTestConnection){
            getPaxDetailsUseCase()?.let { paxDetail ->
                httpSettings.ip = paxDetail.ipAddress
                httpSettings.port = paxDetail.portNumber
                httpSettings.timeout = paxDetail.timeOut
            } ?: run {
                httpSettings.ip = ipAddress
                httpSettings.port = port
                httpSettings.timeout = PAX_DEFAULT_CONNECTION_TIME_OUT
            }
        }else{
            httpSettings.ip = ipAddress
            httpSettings.port = port
            httpSettings.timeout = PAX_DEFAULT_CONNECTION_TIME_OUT
        }

        return httpSettings
    }

}