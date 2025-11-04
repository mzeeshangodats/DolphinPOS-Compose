package com.retail.dolphinpos.data.service

import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseRequest
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseResponse
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchOpenRequest
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchOpenResponse
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutHistoryResponse
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutRequest
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutResponse
import com.retail.dolphinpos.domain.model.auth.login.request.LoginRequest
import com.retail.dolphinpos.domain.model.auth.login.response.LoginResponse
import com.retail.dolphinpos.domain.model.auth.logout.LogoutResponse
import com.retail.dolphinpos.domain.model.auth.select_registers.reponse.StoreRegistersResponse
import com.retail.dolphinpos.domain.model.auth.select_registers.reponse.updateRegister.UpdateStoreRegisterResponse
import com.retail.dolphinpos.domain.model.auth.select_registers.request.UpdateStoreRegisterRequest
import com.retail.dolphinpos.domain.model.home.catrgories_products.ProductsResponse
import com.retail.dolphinpos.domain.model.home.create_order.CreateOrderRequest
import com.retail.dolphinpos.domain.model.home.create_order.CreateOrderResponse
import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout(): LogoutResponse

    @POST("time-slot")
    suspend fun clockInOut(
        @Body clockInOutRequest: ClockInOutRequest
    ): ClockInOutResponse

    @GET("time-slot")
    suspend fun getClockInOutHistory(
        @Query("userId") userId: Int,
    ): ClockInOutHistoryResponse

    @GET("store-registers")
    suspend fun getStoreRegisters(
        @Query("storeId") storeId: Int, @Query("locationId") locationId: Int
    ): StoreRegistersResponse

    @POST("offline-registers/occupy")
    suspend fun updateStoreRegister(@Body request: UpdateStoreRegisterRequest): UpdateStoreRegisterResponse

    @GET("product/offline-download")
    suspend fun getProducts(
        @Query("storeId") storeId: Int, @Query("locationId") locationId: Int
    ): ProductsResponse

    @POST("batch/open")
    suspend fun batchOpen(@Body batchOpenRequest: BatchOpenRequest): BatchOpenResponse

    @POST("batch/close")
    suspend fun batchClose(@Body batchCloseRequest: BatchCloseRequest): BatchCloseResponse

    @POST("order")
    suspend fun createOrder(@Body createOrderRequest: CreateOrderRequest): CreateOrderResponse

    @GET("order")
    suspend fun getOrdersDetails(
        @Query("orderBy") orderBy: String = "createdAt",
        @Query("order") order: String = "desc",
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("limit") limit: Int,
        @Query("page") page: Int,
        @Query("paginate") paginate: Boolean = true,
        @Query("storeId") storeId: Int,
        @Query("keyword") keyword: String? = null
    ): OrderDetailsResponse

}