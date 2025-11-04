package com.retail.dolphinpos.data.setup.hardware.payment.pax

import com.pax.poslinksemiintegration.setting.TcpSetting
import com.retail.dolphinpos.common.utils.Constants
import javax.inject.Inject

interface GetTcpSettingsUseCase {
    operator fun invoke(ipAddress: String, portNumber: String): TcpSetting
}

class GetTcpSettingsUseCaseImpl @Inject constructor() : GetTcpSettingsUseCase {
    override operator fun invoke(ipAddress: String, portNumber: String): TcpSetting {
        return TcpSetting().apply {
            setIpAddress(ipAddress)
            setPort(portNumber.toIntOrNull() ?: 10009)
            setTimeout(Constants.PAX_DEFAULT_CONNECTION_TIME_OUT)
        }
    }
}
