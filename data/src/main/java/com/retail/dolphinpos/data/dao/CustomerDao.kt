package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.retail.dolphinpos.data.entities.customer.CustomerEntity
import com.retail.dolphinpos.domain.model.home.customer.Customer

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customerEntity: CustomerEntity)

    @Query("SELECT * FROM customer")
    suspend fun getCustomers(): List<Customer>

}