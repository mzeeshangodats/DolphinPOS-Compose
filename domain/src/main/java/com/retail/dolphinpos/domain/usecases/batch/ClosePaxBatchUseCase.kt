//package com.lingeriepos.services.usecases.reports.batch
//
//import com.lingeriepos.common.usecases.user.GetUserDetailUseCase
//import com.lingeriepos.services.repositories.ReportsRepository
//import javax.inject.Inject
//
//class ClosePaxBatchUseCase @Inject constructor(
//    private val reportsRepository: ReportsRepository,
//    private val getUserDetailUseCase: GetUserDetailUseCase,
//) {
//    suspend operator fun invoke(paxBatchNo: String): Result<Unit> {
//        val maxRetries = 3
//        val retryDelay = 1000L
//
//        repeat(maxRetries) { attempt ->
//            try {
//                reportsRepository.closePaxBatch(
//                    storeId = getUserDetailUseCase()!!.store.id,
//                    paxBatchNo = paxBatchNo
//                )
//                return Result.success(Unit)
//            } catch (e: Exception) {
//                println("Attempt ${attempt + 1} failed: ${e.message}")
//                if (attempt == maxRetries - 1) {
//                    return Result.failure(e)
//                }
//                kotlinx.coroutines.delay(retryDelay)
//            }
//        }
//        return Result.failure(Exception("Failed to close Pax Batch after $maxRetries attempts"))
//    }
//}
//
