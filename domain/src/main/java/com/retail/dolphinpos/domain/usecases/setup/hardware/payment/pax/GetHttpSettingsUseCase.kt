//package com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax
//
//import com.pax.poscore.commsetting.HttpSetting
//import javax.inject.Inject
//
//class GetHttpSettingsUseCase  @Inject constructor(val getPaxDetailsUseCase: GetPaxDetailsUseCase) {
//
//    operator fun invoke(
//        ipAddress: String,
//        port: String,
//        isTestConnection : Boolean = false
//    ): HttpSetting {
//        val httpSettings = HttpSetting()
//
//        if(!isTestConnection){
//            getPaxDetailsUseCase()?.let { paxDetail ->
//                httpSettings.ip = paxDetail.ipAddress
//                httpSettings.port = paxDetail.portNumber
//                httpSettings.timeout = paxDetail.timeOut
//            } ?: run {
//                httpSettings.ip = ipAddress
//                httpSettings.port = port
//                httpSettings.timeout = PAX_DEFAULT_CONNECTION_TIME_OUT
//            }
//        }else{
//            httpSettings.ip = ipAddress
//            httpSettings.port = port
//            httpSettings.timeout = PAX_DEFAULT_CONNECTION_TIME_OUT
//        }
//
//
//
//        return httpSettings
//    }
//
//}