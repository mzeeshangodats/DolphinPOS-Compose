package com.retail.dolphinpos.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retail.dolphinpos.data.dao.HoldCartDao
import com.retail.dolphinpos.data.entities.holdcart.HoldCartEntity
import com.retail.dolphinpos.domain.model.home.cart.CartItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HoldCartRepository @Inject constructor(
    private val holdCartDao: HoldCartDao,
    private val gson: Gson
) {

    suspend fun saveHoldCart(
        cartName: String,
        cartItems: List<CartItem>,
        subtotal: Double,
        tax: Double,
        totalAmount: Double,
        cashDiscountTotal: Double = 0.0,
        orderDiscountTotal: Double = 0.0,
        isCashSelected: Boolean = false,
        userId: Int,
        storeId: Int,
        registerId: Int
    ): Long {
        val cartItemsJson = gson.toJson(cartItems)
        
        val holdCart = HoldCartEntity(
            cartName = cartName,
            cartItems = cartItemsJson,
            subtotal = subtotal,
            tax = tax,
            totalAmount = totalAmount,
            cashDiscountTotal = cashDiscountTotal,
            orderDiscountTotal = orderDiscountTotal,
            isCashSelected = isCashSelected,
            userId = userId,
            storeId = storeId,
            registerId = registerId
        )
        
        return holdCartDao.insertHoldCart(holdCart)
    }

    suspend fun getHoldCarts(userId: Int, storeId: Int, registerId: Int): List<HoldCartEntity> {
        return holdCartDao.getHoldCartsByUser(userId, storeId, registerId)
    }

    fun getHoldCartsFlow(userId: Int, storeId: Int, registerId: Int): Flow<List<HoldCartEntity>> {
        return holdCartDao.getHoldCartsByUserFlow(userId, storeId, registerId)
    }

    suspend fun getHoldCartById(holdCartId: Long): HoldCartEntity? {
        return holdCartDao.getHoldCartById(holdCartId)
    }

    suspend fun getHoldCartCount(userId: Int, storeId: Int, registerId: Int): Int {
        return holdCartDao.getHoldCartCount(userId, storeId, registerId)
    }

    fun getHoldCartCountFlow(userId: Int, storeId: Int, registerId: Int): Flow<Int> {
        return holdCartDao.getHoldCartCountFlow(userId, storeId, registerId)
    }

    suspend fun deleteHoldCart(holdCartId: Long) {
        holdCartDao.deleteHoldCartById(holdCartId)
    }

    suspend fun deleteAllHoldCarts(userId: Int, storeId: Int, registerId: Int) {
        holdCartDao.deleteAllHoldCartsByUser(userId, storeId, registerId)
    }

    fun parseCartItemsFromJson(cartItemsJson: String): List<CartItem> {
        return try {
            val type = object : TypeToken<List<CartItem>>() {}.type
            gson.fromJson(cartItemsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun convertCartItemsToJson(cartItems: List<CartItem>): String {
        return gson.toJson(cartItems)
    }
}
