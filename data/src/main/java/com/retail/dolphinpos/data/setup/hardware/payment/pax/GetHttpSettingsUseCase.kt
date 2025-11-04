package com.retail.dolphinpos.data.setup.hardware.payment.pax

import com.pax.poscore.commsetting.HttpSetting
import com.retail.dolphinpos.common.utils.Constants
import javax.inject.Inject

interface GetHttpSettingsUseCase {
    operator fun invoke(ipAddress: String, portNumber: String): HttpSetting
}

class GetHttpSettingsUseCaseImpl @Inject constructor() : GetHttpSettingsUseCase {
    override operator fun invoke(ipAddress: String, portNumber: String): HttpSetting {
        return HttpSetting().apply {
//            setIpAddress(ipAddress)
//            setPort(portNumber.toIntOrNull() ?: 10009)
//            setTimeout(Constants.PAX_DEFAULT_CONNECTION_TIME_OUT)
        }
    }
}
