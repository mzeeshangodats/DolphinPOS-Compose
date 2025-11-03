package com.retail.dolphinpos.data.setup.hardware.payment.pax

import com.pax.poscore.commsetting.TcpSetting
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.GetPaxDetailsUseCase
import javax.inject.Inject

const val PAX_DEFAULT_CONNECTION_TIME_OUT = 60000

class GetTcpSettingsUseCase @Inject constructor(val getPaxDetailsUseCase: GetPaxDetailsUseCase) {

    suspend operator fun invoke(
        ipAddress: String,
        port: String,
        isTestConnection : Boolean = false
    ): TcpSetting {
        val tcpSettings = TcpSetting()

        if(!isTestConnection){
            getPaxDetailsUseCase()?.let { paxDetail ->
                tcpSettings.ip = paxDetail.ipAddress
                tcpSettings.port = paxDetail.portNumber
                tcpSettings.timeout = paxDetail.timeOut
            } ?: run {
                tcpSettings.ip = ipAddress
                tcpSettings.port = port
                tcpSettings.timeout = PAX_DEFAULT_CONNECTION_TIME_OUT
            }
        }else{
            tcpSettings.ip = ipAddress
            tcpSettings.port = port
            tcpSettings.timeout = PAX_DEFAULT_CONNECTION_TIME_OUT
        }

        return tcpSettings
    }

}

