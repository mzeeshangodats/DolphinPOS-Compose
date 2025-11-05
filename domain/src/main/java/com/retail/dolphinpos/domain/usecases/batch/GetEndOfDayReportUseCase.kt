//package com.lingeriepos.services.usecases.reports.batch
//
//import com.lingeriepos.common.usecases.batch.GetBatchDetailsUseCase
//import com.lingeriepos.common.usecases.user.GetLoginDetailsUseCase
//import com.lingeriepos.services.repositories.ReportsRepository
//import javax.inject.Inject
//
//class GetEndOfDayReportUseCase @Inject constructor(
//    private val reportsRepository: ReportsRepository,
//    private val getBatchDetailsUseCase: GetBatchDetailsUseCase,
//    private val getLoginDetailsUseCase: GetLoginDetailsUseCase
//) {
//    suspend operator fun invoke(startDate: String, endDate: String) =
//        reportsRepository.getEndOfDayReport(
//            startDate = startDate,
//            endDate = endDate,
//            storeId = getLoginDetailsUseCase()?.user?.storeId!!
//        ).data
//}