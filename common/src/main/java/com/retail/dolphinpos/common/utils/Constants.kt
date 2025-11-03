package com.retail.dolphinpos.common.utils

object Constants {
    const val SET_REGISTER = "set_register"
    const val IS_LOGIN = "is_login"
    const val ACCESS_TOKEN = "access_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val USER_ID = "user_id"
    const val STORE_ID = "store_id"
    const val OCCUPIED_LOCATION_ID = "occupied_location_id"
    const val OCCUPIED_REGISTER_ID = "occupied_register_id"
    const val NAME = "name"
    const val PASSWORD = "password"
    
    // Order discount persistence
    const val ORDER_DISCOUNT_VALUE = "order_discount_value"
    const val ORDER_DISCOUNT_TYPE = "order_discount_type"
    const val ORDER_DISCOUNT_REASON = "order_discount_reason"
    
    // Clock-in/Check-in persistence
    const val CLOCK_IN_TIME = "clock_in_time"
    const val IS_CLOCKED_IN = "is_clocked_in"
    
    // Customer persistence
    const val CUSTOMER_ID = "customer_id"
    
    // Barcode scanner constants
    const val IS_QR_CODE_MODE = "is_qr_code_mode"
    const val SCANNED_CODE = "scanned_code"
    
    // Hardware setup preferences
    const val SHARED_PREFS_FILE_NAME = "Dolphin-Preference"
    const val LOGIN_DETAIL_KEY = "login-detail"
    const val SHARED_PREF_USER_DETAIL_KEY = "user-detail"
    const val PAX_DETAIL = "pax_detail"
    const val PRINTER_DETAIL = "printer_detail"
    const val CUSTOMER_FACING_DISPLAY_DETAIL = "customer_facing_display_details"
    const val USER_PREFERENCE_KEY = "user_preference"
    const val SHOW_IS_LOYALTY_DIALOG = "is_loyalty_member_dialog"

}