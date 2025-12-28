package com.retail.dolphinpos.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retail.dolphinpos.data.entities.refund.RefundEntity
import com.retail.dolphinpos.data.entities.refund.RefundStatus
import com.retail.dolphinpos.data.entities.refund.RefundType
import com.retail.dolphinpos.data.entities.transaction.PaymentMethod
import com.retail.dolphinpos.domain.model.refund.Refund
import com.retail.dolphinpos.domain.model.refund.RefundedItem
import com.retail.dolphinpos.domain.model.refund.RefundStatus as DomainRefundStatus
import com.retail.dolphinpos.domain.model.refund.RefundType as DomainRefundType

object RefundMapper {
    
    private val gson = Gson()
    
    // Entity → Domain
    fun toRefund(entity: RefundEntity): Refund {
        val refundedItems = parseRefundedItems(entity.refundedItems)
        
        return Refund(
            id = entity.id,
            refundId = entity.refundId,
            orderId = entity.orderId,
            orderNo = entity.orderNo,
            refundType = when (entity.refundType) {
                RefundType.FULL -> DomainRefundType.FULL
                RefundType.PARTIAL -> DomainRefundType.PARTIAL
            },
            refundAmount = entity.refundAmount,
            refundedItems = refundedItems,
            paymentMethod = entity.paymentMethod.value,
            refundStatus = when (entity.refundStatus) {
                RefundStatus.PENDING -> DomainRefundStatus.PENDING
                RefundStatus.SYNCED -> DomainRefundStatus.SYNCED
            },
            serverId = entity.serverId,
            storeId = entity.storeId,
            locationId = entity.locationId,
            userId = entity.userId,
            batchNo = entity.batchNo,
            reason = entity.reason,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    // Domain → Entity
    fun toRefundEntity(
        refund: Refund,
        gson: Gson
    ): RefundEntity {
        return RefundEntity(
            id = refund.id,
            refundId = refund.refundId,
            orderId = refund.orderId,
            orderNo = refund.orderNo,
            refundType = when (refund.refundType) {
                DomainRefundType.FULL -> RefundType.FULL
                DomainRefundType.PARTIAL -> RefundType.PARTIAL
            },
            refundAmount = refund.refundAmount,
            refundedItems = serializeRefundedItems(refund.refundedItems, gson),
            paymentMethod = PaymentMethod.fromString(refund.paymentMethod),
            refundStatus = when (refund.refundStatus) {
                DomainRefundStatus.PENDING -> RefundStatus.PENDING
                DomainRefundStatus.SYNCED -> RefundStatus.SYNCED
            },
            serverId = refund.serverId,
            storeId = refund.storeId,
            locationId = refund.locationId,
            userId = refund.userId,
            batchNo = refund.batchNo,
            reason = refund.reason,
            createdAt = refund.createdAt,
            updatedAt = refund.updatedAt
        )
    }
    
    private fun parseRefundedItems(json: String): List<RefundedItem> {
        return try {
            val type = object : TypeToken<List<RefundedItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun serializeRefundedItems(items: List<RefundedItem>, gson: Gson): String {
        return gson.toJson(items)
    }
}

