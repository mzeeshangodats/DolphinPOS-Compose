//package com.retail.dolphinpos.domain.usecases.batch
//
//import com.lingeriepos.common.usecases.savecart.GetAllCartListUseCase
//import com.lingeriepos.models.request.BatchCloseRequest
//import com.lingeriepos.services.repositories.ReportsRepository
//import com.lingeriepos.services.usecases.reports.batch.GetBatchDetailsUseCase
//import com.lingeriepos.utils.toAbandonCarts
//import com.retail.dolphinpos.domain.usecases.auth.GetUserDetailUseCase
//import javax.inject.Inject
//
//class CloseBatchUseCase @Inject constructor(
//    private val reportsRepository: ReportsRepository,
//    private val getUserDetailUseCase: GetUserDetailUseCase,
//    private val getBatchDetailsUseCase: GetBatchDetailsUseCase,
//    private val getAllCartListUseCase: GetAllCartListUseCase
//    ) {
//    suspend operator fun invoke(endingAmount : Double,paxBatchNo : String? = null) =
//        reportsRepository.closeBatch(
//            batchId = getBatchDetailsUseCase()?.batchId!!,
//            request = BatchCloseRequest(
//                storeId = getUserDetailUseCase()!!.store.id,
//                cashierId = getUserDetailUseCase()!!.id,
//                closingCashAmount = endingAmount,
//                paxBatchNo = paxBatchNo,
//                orders = getAllCartListUseCase().toAbandonCarts()
//            )
//
//        )
//}