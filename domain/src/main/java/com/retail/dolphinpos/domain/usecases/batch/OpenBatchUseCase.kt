//package com.lingeriepos.services.usecases.reports.batch
//
//import com.lingeriepos.common.usecases.user.GetLoginDetailsUseCase
//import com.lingeriepos.common.usecases.user.GetUserDetailUseCase
//import com.lingeriepos.services.repositories.ReportsRepository
//import javax.inject.Inject
//
//class OpenBatchUseCase @Inject constructor(
//    private val reportsRepository: ReportsRepository,
//    private val getUserDetailUseCase: GetUserDetailUseCase,
//    private val getLoginDetailsUseCase: GetLoginDetailsUseCase
//) {
//    suspend operator fun invoke(startingAmount : Double) = reportsRepository.openBatch(
//        storeId = getUserDetailUseCase()!!.store.id,
//        cashierId = getUserDetailUseCase()!!.id,
//        storeRegisterId = getLoginDetailsUseCase()!!.registerId,
//        startingCashAmount = startingAmount
//    )
//}