package com.retail.dolphinpos.data.setup.hardware.payment.pax

import com.pax.poscore.commsetting.TcpSetting
import com.retail.dolphinpos.common.utils.Constants
import javax.inject.Inject

interface GetTcpSettingsUseCase {
    operator fun invoke(ipAddress: String, portNumber: String): TcpSetting
}

class GetTcpSettingsUseCaseImpl @Inject constructor() : GetTcpSettingsUseCase {
    override operator fun invoke(ipAddress: String, portNumber: String): TcpSetting {
        return TcpSetting(
            ipAddress,
            portNumber,
            Constants.PAX_DEFAULT_CONNECTION_TIME_OUT
        )
    }
}
