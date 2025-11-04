package com.retail.dolphinpos.domain.model.setup.hardware.payment.pax

data class PaxDetail(
    val ipAddress : String,
    val portNumber : String,
    val timeOut : Int = 60000,
    val communicationType : String
) {
    companion object {
        const val PAX_DEFAULT_CONNECTION_TIME_OUT = 60000
    }
    
    fun isCommunicationTypeHTTP () = communicationType.lowercase() == "http/get"
    fun isCommunicationTypeTCP () = communicationType.lowercase() == "tcp/ip"
}
