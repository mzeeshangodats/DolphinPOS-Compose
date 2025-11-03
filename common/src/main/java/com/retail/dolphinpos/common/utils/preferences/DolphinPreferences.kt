package com.retail.dolphinpos.common.utils.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import javax.crypto.AEADBadTagException
import androidx.core.content.edit

const val SHARED_PREFS_FILE_NAME = "Dolphin-Preference"
const val LOGIN_DETAIL_KEY = "login-detail"
const val SHARED_PREF_USER_DETAIL_KEY = "user-detail"
const val PRINTER_DETAIL = "printer_detail"
const val CUSTOMER_FACING_DISPLAY_DETAIL = "customer_facing_display_details"
const val USER_PREFERENCE_KEY = "user_preference"
const val SHOW_IS_LOYALTY_DIALOG = "is_loyalty_member_dialog"


fun isSamsungDevice(): Boolean {
    return Build.MANUFACTURER.equals("Samsung", ignoreCase = true)
}

fun Context.saveLoginObjectToSharedPreference(value: Any) {
    try {
        val sharedPreference = initSharedPreference(this)
        sharedPreference.edit {
            val gson = Gson()
            val jsonObject = gson.toJson(value)
            putString(LOGIN_DETAIL_KEY, jsonObject)
        }
    } catch (exception: Exception) {

        if (exception is AEADBadTagException && isSamsungDevice()) {
            // 🔥 Fallback: Use normal (unencrypted) SharedPreferences for Samsung tablets
            val sharedPreference = getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE)
            sharedPreference.edit().putString(LOGIN_DETAIL_KEY, Gson().toJson(value)).apply()
            sharedPreference.edit { apply() }
        }
    }
}

//fun Context.saveUserPreferenceList(value: List<UserPreference>) {
//    try {
//        val sharedPreference = initSharedPreference(this)
//        val editor = sharedPreference.edit()
//        val gson = Gson()
//        val jsonString = gson.toJson(value)
//        editor.putString(USER_PREFERENCE_KEY, jsonString)
//        editor.apply()
//    } catch (exception: Exception) {
//        if (exception is AEADBadTagException && isSamsungDevice()) {
//            val sharedPreference = getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE)
//            sharedPreference.edit().putString(USER_PREFERENCE_KEY, Gson().toJson(value)).apply()
//        }
//    }
//}
//
//fun Context.getUserPreferenceList(): List<UserPreference> {
//    val sharedPreference = initSharedPreference(this)
//    val gson = Gson()
//    val jsonString = sharedPreference.getString(USER_PREFERENCE_KEY, null) ?: return emptyList()
//
//    val type = object : TypeToken<List<UserPreference>>() {}.type
//    return gson.fromJson(jsonString, type) ?: emptyList()
//}

fun Context.saveObjectToSharedPreference(key: String, value: Any) {
    try {
        val sharedPreference = initSharedPreference(this)
        sharedPreference.edit {
            val gson = Gson()
            val jsonObject = gson.toJson(value)
            putString(key, jsonObject)
        }
    } catch (exception: Exception) {
        if (exception is AEADBadTagException) {
            val sharedPreference = initSharedPreference(this)
            sharedPreference.edit().clear().apply()
        }
    }
}

fun Context.removeObjectFromSharedPreference(key: String) {
    try {
        val sharedPreference = initSharedPreference(this)
        sharedPreference.edit {
            remove(key)
        }
    } catch (exception: Exception) {
        Log.d("Exception ", exception.message.toString())
    }
}

inline fun <reified T> Context.getObjectFromSharedPreference(key: String): T? {
    val sharedPreference = initSharedPreference(this)
    val gson = Gson()
    val json = sharedPreference.getString(key, null)
    return json?.let { gson.fromJson(it, T::class.java) }
}


//fun Context.getLoginDetailsFromSharedPrefs(): LoginDetails? {
//    return try {
//        val sharedPreference = initSharedPreference(this)
//        val gson = Gson()
//        val value = sharedPreference.getString(LOGIN_DETAIL_KEY, "")
//        gson.fromJson(value, LoginDetails::class.java)
//    } catch (exception: Exception) {
//        Log.d("Exception", exception.message.toString())
//        null
//    }
//}

fun initSharedPreference(context: Context): SharedPreferences {

    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        SHARED_PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    return sharedPreferences
}


fun Context.saveBooleanToSharedPreference(key: String, value: Boolean) {
    try {
        val sharedPreference = initSharedPreference(this)
        val editor = sharedPreference.edit()
        editor.putBoolean(key, value)
        editor.apply()
    } catch (exception: Exception) {
        if (exception is AEADBadTagException && isSamsungDevice()) {
            val sharedPreference = getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE)
            sharedPreference.edit().putBoolean(key, value).apply()
        }
    }
}

fun Context.getBooleanFromSharedPreference(key: String, defaultValue: Boolean = false): Boolean {
    return try {
        val sharedPreference = initSharedPreference(this)
        sharedPreference.getBoolean(key, defaultValue)
    } catch (exception: Exception) {
        if (exception is AEADBadTagException && isSamsungDevice()) {
            val sharedPreference = getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE)
            sharedPreference.getBoolean(key, defaultValue)
        } else {
            defaultValue
        }
    }
}


fun Context.saveStringInSharedPreference(key: String, value: String) {
    try {
        val sharedPreference = initSharedPreference(this)
        val editor = sharedPreference.edit()
        editor.putString(key, value)
        editor.apply()
    } catch (exception: Exception) {
        if (exception is AEADBadTagException && isSamsungDevice()) {
            val sharedPreference = getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE)
            sharedPreference.edit().putString(key, value).apply()
        }
    }
}

fun Context.getStringInSharedPreference(key: String): String? {
    return try {
        val sharedPreference = initSharedPreference(this)
        sharedPreference.getString(key,null)
    } catch (exception: Exception) {
        if (exception is AEADBadTagException && isSamsungDevice()) {
            val sharedPreference = getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE)
            sharedPreference.getString(key, null)
        } else {
            null
        }
    }
}



