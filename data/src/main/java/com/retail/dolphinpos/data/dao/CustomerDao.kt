package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retail.dolphinpos.data.entities.customer.CustomerEntity

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customerEntity: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customerEntity: CustomerEntity)

    @Query("SELECT * FROM customer WHERE is_synced = 0")
    suspend fun getUnsyncedCustomers(): List<CustomerEntity>

    @Query("SELECT * FROM customer")
    suspend fun getCustomers(): List<CustomerEntity>

    @Query("SELECT * FROM customer WHERE id = :customerId")
    suspend fun getCustomerById(customerId: Int): CustomerEntity?

}