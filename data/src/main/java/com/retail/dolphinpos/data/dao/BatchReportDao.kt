package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.retail.dolphinpos.data.entities.report.BatchReportEntity

@Dao
interface BatchReportDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchReport(batchReportEntity: BatchReportEntity)
    
    @Query("SELECT * FROM batch_report WHERE batchNo = :batchNo LIMIT 1")
    suspend fun getBatchReportByBatchNo(batchNo: String): BatchReportEntity?
    
    @Query("SELECT * FROM batch_report")
    suspend fun getAllBatchReports(): List<BatchReportEntity>
    
    @Query("DELETE FROM batch_report")
    suspend fun deleteAllBatchReports()
    
    @Query("DELETE FROM batch_report WHERE batchNo = :batchNo")
    suspend fun deleteBatchReport(batchNo: String)
}

