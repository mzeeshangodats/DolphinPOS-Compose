package com.retail.dolphinpos.data.mapper

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.retail.dolphinpos.data.entities.user.ActiveUserDetailsEntity
import com.retail.dolphinpos.data.entities.user.BatchEntity
import com.retail.dolphinpos.data.entities.user.LocationEntity
import com.retail.dolphinpos.data.entities.user.RegisterEntity
import com.retail.dolphinpos.data.entities.user.RegisterStatusEntity
import com.retail.dolphinpos.data.entities.user.StoreEntity
import com.retail.dolphinpos.data.entities.user.StoreLogoUrlEntity
import com.retail.dolphinpos.data.entities.user.TaxDetailEntity
import com.retail.dolphinpos.data.entities.user.UserEntity
import com.retail.dolphinpos.domain.model.TaxDetail
import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails
import com.retail.dolphinpos.domain.model.auth.batch.Batch
import com.retail.dolphinpos.domain.model.auth.login.response.AllStoreUsers
import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.Registers
import com.retail.dolphinpos.domain.model.auth.login.response.Store
import com.retail.dolphinpos.domain.model.auth.login.response.StoreLogoUrl
import com.retail.dolphinpos.domain.model.auth.select_registers.reponse.updateRegister.UpdateStoreRegisterData
import java.lang.reflect.Type

object UserMapper {

    // -------------------------
    // Domain → Entity Mappers
    // -------------------------

    fun toUserEntity(user: AllStoreUsers, password: String): UserEntity {
        return UserEntity(
            id = user.id,
            name = user.name,
            email = user.email,
            username = user.username,
            password = password,
            pin = user.pin,
            status = user.status,
            phoneNo = user.phoneNo,
            storeId = user.storeId,
            locationId = user.locationId,
            roleId = user.roleId,
            roleTitle = user.roleTitle,
        )
    }

    fun toStoreEntity(userID: Int, store: Store): StoreEntity {
        return StoreEntity(
            id = store.id,
            userID = userID,
            name = store.name,
            address = store.address,
            multiCashier = store.multiCashier,
            policy = store.policy,
            advertisementImg = store.advertisementImg,
            isAdvertisement = store.isAdvertisement
        )
    }

    fun toStoreLogoUrlEntity(
        storeID: Int, logoUrl: StoreLogoUrl
    ): StoreLogoUrlEntity {
        return StoreLogoUrlEntity(
            storeID = storeID,
            alt = logoUrl.alt,
            original = logoUrl.original,
            thumbnail = logoUrl.thumbnail
        )
    }

    fun toLocationEntity(storeID: Int, location: Locations, gson: Gson? = null): LocationEntity {
        val taxDetailsJson = location.taxDetails?.let { taxDetails ->
            gson?.toJson(taxDetails) ?: null
        }
        return LocationEntity(
            id = location.id,
            storeID = storeID,
            name = location.name,
            address = location.address,
            status = location.status,
            zipCode = location.zipCode,
            taxValue = location.taxValue,
            taxTitle = location.taxTitle,
            taxDetails = taxDetailsJson,
            startTime = location.startTime,
            endTime = location.endTime,
            multiCashier = location.multiCashier,
            dualPricePercentage = location.dualPricePercentage?.toString()
        )
    }

    fun toRegisterEntity(register: Registers): RegisterEntity {
        return RegisterEntity(
            id = register.id,
            name = register.name,
            status = register.status,
            locationId = register.locationId
        )
    }

    fun toBatchEntity(batch: Batch): BatchEntity {
        return BatchEntity(
            batchNo = batch.batchNo,
            storeId = batch.storeId,
            userId = batch.userId,
            locationId = batch.locationId,
            registerId = batch.registerId,
            startingCashAmount = batch.startingCashAmount
        )
    }

    fun toRegisterStatusEntity(updateStoreRegisterData: UpdateStoreRegisterData): RegisterStatusEntity {
        return RegisterStatusEntity(
            storeId = updateStoreRegisterData.storeId,
            locationId = updateStoreRegisterData.locationId,
            storeRegisterId = updateStoreRegisterData.storeRegisterId,
            status = updateStoreRegisterData.status,
            updatedAt = updateStoreRegisterData.updatedAt
        )
    }

    fun toActiveUserDetailsEntity(activeUserDetails: ActiveUserDetails): ActiveUserDetailsEntity {
        return ActiveUserDetailsEntity(
            id = activeUserDetails.id,
            name = activeUserDetails.name,
            email = activeUserDetails.email,
            username = activeUserDetails.username,
            password = activeUserDetails.password,
            pin = activeUserDetails.pin,
            userStatus = activeUserDetails.userStatus,
            phoneNo = activeUserDetails.phoneNo,
            storeId = activeUserDetails.storeId,
            locationId = activeUserDetails.locationId,
            roleId = activeUserDetails.roleId,
            roleTitle = activeUserDetails.roleTitle,
            storeName = activeUserDetails.storeName,
            address = activeUserDetails.address,
            storeMultiCashier = activeUserDetails.storeMultiCashier,
            policy = activeUserDetails.policy,
            advertisementImg = activeUserDetails.advertisementImg,
            isAdvertisement = activeUserDetails.isAdvertisement,
            alt = activeUserDetails.alt,
            original = activeUserDetails.original,
            thumbnail = activeUserDetails.thumbnail,
            locationName = activeUserDetails.locationName,
            locationAddress = activeUserDetails.locationAddress,
            locationStatus = activeUserDetails.locationStatus,
            zipCode = activeUserDetails.zipCode,
            taxTitle = activeUserDetails.taxTitle,
            startTime = activeUserDetails.startTime,
            endTime = activeUserDetails.endTime,
            locationMultiCashier = activeUserDetails.locationMultiCashier,
            dualPricePercentage = activeUserDetails.dualPricePercentage,
            registerId = activeUserDetails.registerId,
            registerName = activeUserDetails.registerName,
            registerStatus = activeUserDetails.registerStatus
        )
    }

    // -------------------------
    // Entity → Domain Mappers
    // -------------------------

    fun toUsers(userEntity: UserEntity): AllStoreUsers {
        return AllStoreUsers(
            id = userEntity.id,
            name = userEntity.name,
            email = userEntity.email,
            username = userEntity.username,
            password = userEntity.password,
            pin = userEntity.pin,
            status = userEntity.status,
            phoneNo = userEntity.phoneNo,
            storeId = userEntity.storeId,
            locationId = userEntity.locationId,
            roleId = userEntity.roleId,
            roleTitle = userEntity.roleTitle,
        )
    }

    fun toStore(
        storeEntity: StoreEntity,
        storeLogoUrlEntity: StoreLogoUrlEntity?,
        locationEntities: List<LocationEntity>,
        registerEntities: List<RegisterEntity>,
        gson: Gson? = null
    ): Store {
        return Store(
            id = storeEntity.id,
            name = storeEntity.name,
            address = storeEntity.address,
            multiCashier = storeEntity.multiCashier,
            policy = storeEntity.policy,
            advertisementImg = storeEntity.advertisementImg,
            isAdvertisement = storeEntity.isAdvertisement,
            logoUrl = toStoreLogoUrl(storeLogoUrlEntity),
            locations = toLocations(locationEntities, registerEntities, gson)
        )
    }

    fun toStoreLogoUrl(storeLogoUrlEntity: StoreLogoUrlEntity?): StoreLogoUrl {
        return StoreLogoUrl(
            alt = storeLogoUrlEntity?.alt,
            original = storeLogoUrlEntity?.original,
            thumbnail = storeLogoUrlEntity?.thumbnail
        )
    }

    fun toLocations(
        locationEntities: List<LocationEntity>,
        registerEntities: List<RegisterEntity>,
        gson: Gson? = null
    ): List<Locations> {
        return locationEntities.map { location ->
            val taxDetails = decodeTaxDetails(location.taxDetails, gson)
            Locations(
                id = location.id,
                name = location.name,
                address = location.address,
                status = location.status,
                zipCode = location.zipCode,
                taxValue = location.taxValue,
                taxTitle = location.taxTitle,
                taxDetails = taxDetails,
                startTime = location.startTime,
                endTime = location.endTime,
                multiCashier = location.multiCashier,
                dualPricePercentage = location.dualPricePercentage?.toDoubleOrNull(),
                registers = registerEntities
                    .filter { it.locationId == location.id }
                    .map { toRegister(it) }
            )
        }
    }

    private fun toRegister(register: RegisterEntity): Registers {
        return Registers(
            id = register.id,
            name = register.name,
            status = register.status,
            locationId = register.locationId
        )
    }

    fun toLocationsAgainstStoreID(entities: List<LocationEntity>, gson: Gson? = null): List<Locations> {
        return entities.map { entity ->
            val taxDetails = decodeTaxDetails(entity.taxDetails, gson)
            Locations(
                id = entity.id,
                name = entity.name,
                address = entity.address,
                status = entity.status,
                zipCode = entity.zipCode,
                taxValue = entity.taxValue,
                taxTitle = entity.taxTitle,
                taxDetails = taxDetails,
                startTime = entity.startTime,
                endTime = entity.endTime,
                multiCashier = entity.multiCashier,
                dualPricePercentage = entity.dualPricePercentage?.toDoubleOrNull(),
                registers = emptyList()
            )
        }
    }


    fun toRegistersAgainstLocationID(
        locationID: Int,
        entities: List<RegisterEntity>
    ): List<Registers> {
        return entities.map { entity ->
            Registers(
                id = entity.id,
                name = entity.name,
                status = entity.status,
                locationId = locationID
            )
        }
    }

    fun toStoreAgainstStoreID(storeEntity: StoreEntity): Store {
        return Store(
            id = storeEntity.id,
            name = storeEntity.name,
            address = storeEntity.address,
            multiCashier = storeEntity.multiCashier,
            policy = storeEntity.policy,
            advertisementImg = storeEntity.advertisementImg,
            isAdvertisement = storeEntity.isAdvertisement,
            logoUrl = null,
            locations = null
        )
    }

    fun toLocationAgainstLocationID(locationEntity: LocationEntity, gson: Gson? = null): Locations {
        val taxDetails = decodeTaxDetails(locationEntity.taxDetails, gson)
        return Locations(
            id = locationEntity.id,
            name = locationEntity.name,
            address = locationEntity.address,
            status = locationEntity.status,
            zipCode = locationEntity.zipCode,
            taxValue = locationEntity.taxValue,
            taxTitle = locationEntity.taxTitle,
            taxDetails = taxDetails,
            startTime = locationEntity.startTime,
            endTime = locationEntity.endTime,
            multiCashier = locationEntity.multiCashier,
            dualPricePercentage = locationEntity.dualPricePercentage?.toDoubleOrNull(),
            registers = null
        )
    }

    fun toRegisterAgainstRegisterID(registerEntity: RegisterEntity): Registers {
        return Registers(
            id = registerEntity.id,
            name = registerEntity.name,
            status = registerEntity.status,
            locationId = registerEntity.locationId
        )
    }

    fun toRegisterStatus(registerStatusEntity: RegisterStatusEntity): UpdateStoreRegisterData {
        return UpdateStoreRegisterData(
            storeId = registerStatusEntity.storeId,
            locationId = registerStatusEntity.locationId,
            storeRegisterId = registerStatusEntity.storeRegisterId,
            status = registerStatusEntity.status,
            updatedAt = registerStatusEntity.updatedAt
        )
    }

    fun toActiveUserDetailsAgainstPin(entity: ActiveUserDetailsEntity): ActiveUserDetails {
        return ActiveUserDetails(
            id = entity.id,
            name = entity.name,
            email = entity.email,
            username = entity.username,
            password = entity.password,
            pin = entity.pin,
            userStatus = entity.userStatus,
            phoneNo = entity.phoneNo,
            storeId = entity.storeId,
            locationId = entity.locationId,
            roleId = entity.roleId,
            roleTitle = entity.roleTitle,
            storeName = entity.storeName,
            address = entity.address,
            storeMultiCashier = entity.storeMultiCashier,
            policy = entity.policy,
            advertisementImg = entity.advertisementImg,
            isAdvertisement = entity.isAdvertisement,
            alt = entity.alt,
            original = entity.original,
            thumbnail = entity.thumbnail,
            locationName = entity.locationName,
            locationAddress = entity.locationAddress,
            locationStatus = entity.locationStatus,
            zipCode = entity.zipCode,
            taxTitle = entity.taxTitle,
            startTime = entity.startTime,
            endTime = entity.endTime,
            locationMultiCashier = entity.locationMultiCashier,
            dualPricePercentage = entity.dualPricePercentage,
            registerId = entity.registerId,
            registerName = entity.registerName,
            registerStatus = entity.registerStatus
        )
    }

    fun toBatchDetails(batchEntity: BatchEntity): Batch {
        return Batch(
            batchId = batchEntity.batchId,
            batchNo = batchEntity.batchNo,
            storeId = batchEntity.storeId,
            userId = batchEntity.userId,
            registerId = batchEntity.registerId,
            locationId = batchEntity.locationId,
            startingCashAmount = batchEntity.startingCashAmount
        )
    }

    // -------------------------
    // Helper functions for TaxDetails
    // -------------------------

    private fun decodeTaxDetails(json: String?, gson: Gson?): List<TaxDetail>? {
        if (json.isNullOrBlank() || gson == null) return null
        return try {
            val type: Type = object : TypeToken<List<TaxDetail>>() {}.type
            gson.fromJson<List<TaxDetail>>(json, type)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    // -------------------------
    // TaxDetail Mappers
    // -------------------------

    fun toTaxDetailEntity(locationId: Int, taxDetail: TaxDetail): TaxDetailEntity {
        return TaxDetailEntity(
            locationId = locationId,
            type = taxDetail.type,
            title = taxDetail.title,
            value = taxDetail.value,
            amount = taxDetail.amount,
            isDefault = taxDetail.isDefault,
            refundedTax = taxDetail.refundedTax
        )
    }

    fun toTaxDetail(entity: TaxDetailEntity): TaxDetail {
        return TaxDetail(
            type = entity.type,
            title = entity.title,
            value = entity.value,
            amount = entity.amount,
            isDefault = entity.isDefault,
            refundedTax = entity.refundedTax
        )
    }
}