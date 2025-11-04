//package com.lingeriepos.services.usecases.reports.batch
//
//import com.lingeriepos.common.usecases.user.GetUserDetailUseCase
//import com.lingeriepos.services.repositories.ReportsRepository
//import javax.inject.Inject
//
//class GetBatchDetailsUseCase @Inject constructor(
//    private val reportsRepository: ReportsRepository,
//    private val getUserDetailUseCase: GetUserDetailUseCase
//) {
//    suspend operator fun invoke() = reportsRepository.getBatchReports(
//        batchId = getUserDetailUseCase()?.batchDetails?.batchId!!,
//        storeId = getUserDetailUseCase()?.store?.id!!,
//    )
//}