package com.retail.dolphinpos.domain.usecases.label


import javax.inject.Inject

class GetDualPricePercentageUseCase @Inject constructor(
//    private val userDao: UserDao,
//    private val preferenceManager: PreferenceManager
) {
    operator fun invoke(): Double {
        return try {
            val locationId = 1//preferenceManager.getOccupiedLocationID()
            val location = 1//userDao.getLocationByLocationId(locationId)
            (/*location.dualPricePercentage*/2.0 ?: 0.0) / 100.0
        } catch (e: Exception) {
            0.0
        }
    }
}

