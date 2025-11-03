package com.retail.dolphinpos.domain.model.setup.hardware.payment.pax

data class PaxDetail(
    val ipAddress : String,
    val portNumber : String,
    val timeOut : Int = 60000,
    val communicationType : String
) {
    fun isCommunicationTypeHTTP () = communicationType.lowercase() == "http/get"
    fun isCommunicationTypeTCP () = communicationType.lowercase() == "tcp/ip"
}
