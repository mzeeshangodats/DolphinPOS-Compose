package com.retail.dolphinpos.domain.repositories.auth

import com.retail.dolphinpos.domain.model.auth.login.request.LoginRequest
import com.retail.dolphinpos.domain.model.auth.login.response.AllStoreUsers
import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.LoginResponse
import com.retail.dolphinpos.domain.model.auth.login.response.Store
import com.retail.dolphinpos.domain.model.auth.login.response.StoreLogoUrl

interface LoginRepository {
    suspend fun login(request: LoginRequest): LoginResponse

    suspend fun insertUsersDataIntoLocalDB(
        users: List<AllStoreUsers>,
        store: Store,
        logoUrl: StoreLogoUrl,
        locationsList: List<Locations>,
        password: String,
        userId: Int,
        storeID: Int,
        locationID: Int
    ) {
        insertUserIntoLocalDB(users, password)
        insertStoreIntoLocalDB(store, userId)
        insertStoreLogoUrlIntoLocalDB(logoUrl, userId, storeID)
        insertLocationsIntoLocalDB(locationsList, storeID)
    }

    suspend fun insertUserIntoLocalDB(users: List<AllStoreUsers>, password: String)

    suspend fun insertStoreIntoLocalDB(store: Store, userId: Int)

    suspend fun insertStoreLogoUrlIntoLocalDB(
        logoUrl: StoreLogoUrl,
        userId: Int,
        storeID: Int
    )

    suspend fun insertLocationsIntoLocalDB(locationsList: List<Locations>, storeID: Int)

}