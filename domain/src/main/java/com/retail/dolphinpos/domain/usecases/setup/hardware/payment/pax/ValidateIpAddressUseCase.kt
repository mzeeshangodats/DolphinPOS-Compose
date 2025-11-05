package com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax

import javax.inject.Inject

val IP_REGEX = """^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[0-9])\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[0-9])$""".toRegex()


class ValidateIpAddressUseCase @Inject constructor() {

    operator fun invoke(ipAddress : String) = IP_REGEX.matches(ipAddress)

}