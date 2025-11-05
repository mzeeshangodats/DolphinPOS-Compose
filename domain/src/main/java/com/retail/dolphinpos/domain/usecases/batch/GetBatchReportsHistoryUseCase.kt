//package com.lingeriepos.services.usecases.reports.batch
//
//import com.lingeriepos.common.usecases.user.GetLoginDetailsUseCase
//import com.lingeriepos.common.usecases.user.GetUserDetailUseCase
//import com.lingeriepos.services.repositories.ReportsRepository
//import javax.inject.Inject
//
//class GetBatchReportsHistoryUseCase @Inject constructor(
//    private val reportsRepository: ReportsRepository,
//    private val getUserDetailUseCase: GetUserDetailUseCase
//) {
//    suspend operator fun invoke(
//        startDate: String,
//        endDate: String,
//        page: Int = 1,
//        limit: Int = 20,
//        keyword: String? = null
//    ) =
//        reportsRepository.getBatchReportHistory(
//            storeId = getUserDetailUseCase()?.store?.id!!,
//            startDate = startDate,
//            endDate = endDate,
//            page = page,
//            limit = limit,
//            keyword = keyword
//        )
//}