package com.retail.dolphinpos.common.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.retail.dolphinpos.domain.model.auth.login.response.LoginData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PreferenceManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dolphin_prefs", Context.MODE_PRIVATE)

    fun setRegister(value: Boolean) {
        prefs.edit { putBoolean(Constants.SET_REGISTER, value) }
    }

    fun getRegister(defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(Constants.SET_REGISTER, defaultValue)
    }

    fun setLogin(value: Boolean) {
        prefs.edit { putBoolean(Constants.IS_LOGIN, value) }
    }

    fun isLogin(defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(Constants.IS_LOGIN, defaultValue)
    }

    fun setAccessToken(value: String) {
        prefs.edit { putString(Constants.ACCESS_TOKEN, value) }
    }

    fun getAccessToken(defaultValue: String = ""): String {
        return prefs.getString(Constants.ACCESS_TOKEN, defaultValue) ?: defaultValue
    }

    fun setRefreshToken(value: String) {
        prefs.edit { putString(Constants.REFRESH_TOKEN, value) }
    }

    fun getRefreshToken(defaultValue: String = ""): String {
        return prefs.getString(Constants.REFRESH_TOKEN, defaultValue) ?: defaultValue
    }

    fun setUserID(value: Int) {
        prefs.edit { putInt(Constants.USER_ID, value) }
    }

    fun getUserID(defaultValue: Int = 0): Int {
        return prefs.getInt(Constants.USER_ID, defaultValue)
    }

    fun setStoreID(value: Int) {
        prefs.edit { putInt(Constants.STORE_ID, value) }
    }

    fun getStoreID(defaultValue: Int = 0): Int {
        return prefs.getInt(Constants.STORE_ID, defaultValue)
    }

    fun setName(value: String) {
        prefs.edit { putString(Constants.NAME, value) }
    }

    fun getName(defaultValue: String = ""): String {
        return prefs.getString(Constants.NAME, defaultValue) ?: defaultValue
    }

    fun setPassword(value: String) {
        prefs.edit { putString(Constants.PASSWORD, value) }
    }

    fun getPassword(defaultValue: String = ""): String {
        return prefs.getString(Constants.PASSWORD, defaultValue) ?: defaultValue
    }

    fun setOccupiedLocationID(value: Int) {
        prefs.edit { putInt(Constants.OCCUPIED_LOCATION_ID, value) }
    }

    fun getOccupiedLocationID(defaultValue: Int = 0): Int {
        return prefs.getInt(Constants.OCCUPIED_LOCATION_ID, defaultValue)
    }

    fun setOccupiedRegisterID(value: Int) {
        prefs.edit { putInt(Constants.OCCUPIED_REGISTER_ID, value) }
    }

    fun getOccupiedRegisterID(defaultValue: Int = 0): Int {
        return prefs.getInt(Constants.OCCUPIED_REGISTER_ID, defaultValue)
    }

    fun saveLoginData(loginData: LoginData, password: String) {
        setStoreID(loginData.storeInfo.id)
        loginData.user.name?.let { setName(it) }
        setPassword(password)
        setAccessToken(loginData.accessToken)
        setRefreshToken(loginData.refreshToken)
        setLogin(true)
    }

    // Order discount persistence methods
    fun setOrderDiscountValue(value: String) {
        prefs.edit { putString(Constants.ORDER_DISCOUNT_VALUE, value) }
    }

    fun getOrderDiscountValue(defaultValue: String = ""): String {
        return prefs.getString(Constants.ORDER_DISCOUNT_VALUE, defaultValue) ?: defaultValue
    }

    fun setOrderDiscountType(type: String) {
        prefs.edit { putString(Constants.ORDER_DISCOUNT_TYPE, type) }
    }

    fun getOrderDiscountType(defaultValue: String = "PERCENTAGE"): String {
        return prefs.getString(Constants.ORDER_DISCOUNT_TYPE, defaultValue) ?: defaultValue
    }

//    fun setOrderDiscountReason(reason: String) {
//        prefs.edit { putString(Constants.ORDER_DISCOUNT_REASON, reason) }
//    }
//
//    fun getOrderDiscountReason(defaultValue: String = ""): String {
//        return prefs.getString(Constants.ORDER_DISCOUNT_REASON, defaultValue) ?: defaultValue
//    }

    // Clock-in/Check-in methods
    fun setClockInTime(timeInMillis: Long) {
        prefs.edit { putLong(Constants.CLOCK_IN_TIME, timeInMillis) }
    }

    fun getClockInTime(defaultValue: Long = 0L): Long {
        return prefs.getLong(Constants.CLOCK_IN_TIME, defaultValue)
    }

    fun setClockInStatus(isClockedIn: Boolean) {
        prefs.edit { putBoolean(Constants.IS_CLOCKED_IN, isClockedIn) }
    }

    fun isClockedIn(defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(Constants.IS_CLOCKED_IN, defaultValue)
    }

    fun clockOut() {
        prefs.edit {
            remove(Constants.CLOCK_IN_TIME)
            putBoolean(Constants.IS_CLOCKED_IN, false)
        }
    }

    fun setOrderDiscountReason(reason: String) {
        prefs.edit { putString(Constants.ORDER_DISCOUNT_REASON, reason) }
    }

    fun getOrderDiscountReason(defaultValue: String = "Select Reason"): String {
        return prefs.getString(Constants.ORDER_DISCOUNT_REASON, defaultValue) ?: defaultValue
    }

    fun clearOrderDiscountValues() {
        prefs.edit {
            remove(Constants.ORDER_DISCOUNT_VALUE)
            remove(Constants.ORDER_DISCOUNT_TYPE)
            remove(Constants.ORDER_DISCOUNT_REASON)
        }
    }

    // Customer methods
    fun setCustomerID(customerId: Int) {
        prefs.edit { putInt(Constants.CUSTOMER_ID, customerId) }
    }

    fun getCustomerID(defaultValue: Int = 0): Int {
        return prefs.getInt(Constants.CUSTOMER_ID, defaultValue)
    }

    fun clearCustomerID() {
        prefs.edit { remove(Constants.CUSTOMER_ID) }
    }

}