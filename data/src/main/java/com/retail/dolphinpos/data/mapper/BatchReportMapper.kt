package com.retail.dolphinpos.data.mapper

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.retail.dolphinpos.data.entities.report.BatchReportEntity
import com.retail.dolphinpos.domain.model.report.batch_report.BatchReportData
import com.retail.dolphinpos.domain.model.report.batch_report.Closed
import com.retail.dolphinpos.domain.model.report.batch_report.Opened

object BatchReportMapper {
    
    private val gson = Gson()
    
    /**
     * Helper function to parse JSON string to Any type
     * Tries to parse as Number first, then String, then returns as-is
     */
    private fun parseAnyFromJson(jsonString: String?): Any? {
        if (jsonString == null) return null
        return try {
            val jsonElement: JsonElement = JsonParser.parseString(jsonString)
            when {
                jsonElement.isJsonPrimitive -> {
                    val primitive = jsonElement.asJsonPrimitive
                    when {
                        primitive.isNumber -> primitive.asNumber
                        primitive.isString -> primitive.asString
                        primitive.isBoolean -> primitive.asBoolean
                        else -> jsonString
                    }
                }
                jsonElement.isJsonObject || jsonElement.isJsonArray -> {
                    // Return as JsonElement for complex objects
                    jsonElement
                }
                else -> jsonString
            }
        } catch (e: Exception) {
            // If parsing fails, try to parse as number or return as string
            jsonString.toDoubleOrNull() ?: jsonString.toIntOrNull() ?: jsonString
        }
    }
    
    // -------------------------
    // Domain → Entity Mappers
    // -------------------------
    
    fun toBatchReportEntity(batchReportData: BatchReportData): BatchReportEntity {
        return BatchReportEntity(
            batchNo = batchReportData.batchNo,
            closed = batchReportData.closed?.let { gson.toJson(it) },
            closedBy = batchReportData.closedBy,
            closingCashAmount = batchReportData.closingCashAmount,
            closingTime = batchReportData.closingTime,
            createdAt = batchReportData.createdAt,
            id = batchReportData.id,
            locationId = batchReportData.locationId,
            openTime = batchReportData.openTime,
            opened = batchReportData.opened?.let { gson.toJson(it) },
            openedBy = batchReportData.openedBy,
            payInCard = try { gson.toJson(batchReportData.payInCard) } catch (e: Exception) { null },
            payInCash = try { gson.toJson(batchReportData.payInCash) } catch (e: Exception) { null },
            payOutCard = try { gson.toJson(batchReportData.payOutCard) } catch (e: Exception) { null },
            payOutCash = try { gson.toJson(batchReportData.payOutCash) } catch (e: Exception) { null },
            startingCashAmount = batchReportData.startingCashAmount,
            status = batchReportData.status,
            storeId = batchReportData.storeId,
            storeRegisterId = batchReportData.storeRegisterId,
            totalAbandonOrders = batchReportData.totalAbandonOrders,
            totalAmount = batchReportData.totalAmount,
            totalCardAmount = batchReportData.totalCardAmount,
            totalCashAmount = batchReportData.totalCashAmount,
            totalCashDiscount = batchReportData.totalCashDiscount,
            totalDiscount = batchReportData.totalDiscount,
            totalOnlineSales = batchReportData.totalOnlineSales,
            totalPayIn = try { gson.toJson(batchReportData.totalPayIn) } catch (e: Exception) { null },
            totalPayOut = try { gson.toJson(batchReportData.totalPayOut) } catch (e: Exception) { null },
            totalRewardDiscount = batchReportData.totalRewardDiscount,
            totalSales = try { gson.toJson(batchReportData.totalSales) } catch (e: Exception) { null },
            totalTax = batchReportData.totalTax,
            totalTip = batchReportData.totalTip,
            totalTipCard = batchReportData.totalTipCard,
            totalTipCash = batchReportData.totalTipCash,
            totalTransactions = batchReportData.totalTransactions,
            updatedAt = batchReportData.updatedAt
        )
    }
    
    // -------------------------
    // Entity → Domain Mappers
    // -------------------------
    
    fun toBatchReportData(batchReportEntity: BatchReportEntity): BatchReportData {
        return BatchReportData(
            batchNo = batchReportEntity.batchNo,
            closed = batchReportEntity.closed?.let { 
                try { gson.fromJson(it, Closed::class.java) } catch (e: Exception) { null }
            },
            closedBy = batchReportEntity.closedBy,
            closingCashAmount = batchReportEntity.closingCashAmount,
            closingTime = batchReportEntity.closingTime,
            createdAt = batchReportEntity.createdAt,
            id = batchReportEntity.id,
            locationId = batchReportEntity.locationId,
            openTime = batchReportEntity.openTime,
            opened = batchReportEntity.opened?.let { 
                try { gson.fromJson(it, Opened::class.java) } catch (e: Exception) { null }
            },
            openedBy = batchReportEntity.openedBy,
            payInCard = parseAnyFromJson(batchReportEntity.payInCard) ?: 0,
            payInCash = parseAnyFromJson(batchReportEntity.payInCash) ?: 0,
            payOutCard = parseAnyFromJson(batchReportEntity.payOutCard) ?: 0,
            payOutCash = parseAnyFromJson(batchReportEntity.payOutCash) ?: 0,
            startingCashAmount = batchReportEntity.startingCashAmount,
            status = batchReportEntity.status,
            storeId = batchReportEntity.storeId,
            storeRegisterId = batchReportEntity.storeRegisterId,
            totalAbandonOrders = batchReportEntity.totalAbandonOrders,
            totalAmount = batchReportEntity.totalAmount,
            totalCardAmount = batchReportEntity.totalCardAmount,
            totalCashAmount = batchReportEntity.totalCashAmount,
            totalCashDiscount = batchReportEntity.totalCashDiscount,
            totalDiscount = batchReportEntity.totalDiscount,
            totalOnlineSales = batchReportEntity.totalOnlineSales,
            totalPayIn = parseAnyFromJson(batchReportEntity.totalPayIn) ?: 0,
            totalPayOut = parseAnyFromJson(batchReportEntity.totalPayOut) ?: 0,
            totalRewardDiscount = batchReportEntity.totalRewardDiscount,
            totalSales = parseAnyFromJson(batchReportEntity.totalSales) ?: 0,
            totalTax = batchReportEntity.totalTax,
            totalTip = batchReportEntity.totalTip,
            totalTipCard = batchReportEntity.totalTipCard,
            totalTipCash = batchReportEntity.totalTipCash,
            totalTransactions = batchReportEntity.totalTransactions,
            updatedAt = batchReportEntity.updatedAt
        )
    }
}

