package com.retail.dolphinpos.data.repositories.auth

import com.google.gson.Gson
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCall
import com.retail.dolphinpos.domain.model.auth.login.request.LoginRequest
import com.retail.dolphinpos.domain.model.auth.login.response.AllStoreUsers
import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.LoginResponse
import com.retail.dolphinpos.domain.model.auth.login.response.Store
import com.retail.dolphinpos.domain.model.auth.login.response.StoreLogoUrl
import com.retail.dolphinpos.domain.repositories.auth.LoginRepository

class LoginRepositoryImpl(
    private val api: ApiService, private val userDao: UserDao, private val gson: Gson
) : LoginRepository {

    override suspend fun login(request: LoginRequest): LoginResponse {
        return safeApiCall(
            apiCall = { api.login(request) },
            defaultResponse = {
                LoginResponse(
                    loginData = null,
                    message = "Login failed",
                    errors = null
                )
            }
        )
    }

    override suspend fun insertUserIntoLocalDB(
        users: List<AllStoreUsers>, password: String
    ) {
        try {
            val userEntities = users.map { user ->
                UserMapper.toUserEntity(
                    user = user,
                    password = password
                )
            }
            userDao.insertUsers(userEntities)
        } catch (e: Exception) {
            throw e
        }
    }


    override suspend fun insertStoreIntoLocalDB(
        store: Store, userId: Int
    ) {
        try {
            userDao.insertStoreDetails(
                UserMapper.toStoreEntity(
                    userID = userId, store = store
                )
            )
        } catch (e: Exception) {
            throw e
        }
    }


    override suspend fun insertStoreLogoUrlIntoLocalDB(
        logoUrl: StoreLogoUrl, userId: Int, storeID: Int
    ) {
        try {
            userDao.insertStoreLogoUrlDetails(
                UserMapper.toStoreLogoUrlEntity(
                    storeID = storeID, logoUrl = logoUrl
                )
            )
        } catch (e: Exception) {
            throw e
        }
    }


    override suspend fun insertLocationsIntoLocalDB(
        locationsList: List<Locations>, storeID: Int
    ) {
        try {
            locationsList.forEach { location ->
                // Save location only (registers will be fetched from API when needed)
                val locationEntity = UserMapper.toLocationEntity(storeID, location, gson)
                userDao.insertLocations(locationEntity)
            }

        } catch (e: Exception) {
            throw e
        }
    }

}