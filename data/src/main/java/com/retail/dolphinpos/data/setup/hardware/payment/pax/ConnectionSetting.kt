package com.retail.dolphinpos.data.setup.hardware.payment.pax

import com.pax.poscore.commsetting.HttpSetting
import com.pax.poscore.commsetting.TcpSetting

sealed class ConnectionSetting {
    data class Tcp(val tcpSetting: TcpSetting) : ConnectionSetting()
    data class Http(val httpSetting: HttpSetting) : ConnectionSetting()
}
