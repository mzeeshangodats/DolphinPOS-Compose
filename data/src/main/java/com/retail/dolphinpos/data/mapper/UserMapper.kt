package com.retail.dolphinpos.data.mapper

import com.retail.dolphinpos.data.entities.user.ActiveUserDetailsEntity
import com.retail.dolphinpos.data.entities.user.BatchEntity
import com.retail.dolphinpos.data.entities.user.LocationEntity
import com.retail.dolphinpos.data.entities.user.RegisterEntity
import com.retail.dolphinpos.data.entities.user.RegisterStatusEntity
import com.retail.dolphinpos.data.entities.user.StoreEntity
import com.retail.dolphinpos.data.entities.user.StoreLogoUrlEntity
import com.retail.dolphinpos.data.entities.user.UserEntity
import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails
import com.retail.dolphinpos.domain.model.auth.batch.Batch
import com.retail.dolphinpos.domain.model.auth.login.response.AllStoreUsers
import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.Registers
import com.retail.dolphinpos.domain.model.auth.login.response.Store
import com.retail.dolphinpos.domain.model.auth.login.response.StoreLogoUrl
import com.retail.dolphinpos.domain.model.auth.select_registers.reponse.UpdateStoreRegisterData

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

    fun toLocationEntity(storeID: Int, location: Locations): LocationEntity {
        return LocationEntity(
            id = location.id,
            storeID = storeID,
            name = location.name,
            address = location.address,
            status = location.status,
            zipCode = location.zipCode,
            taxValue = location.taxValue,
            taxTitle = location.taxTitle,
            startTime = location.startTime,
            endTime = location.endTime,
            multiCashier = location.multiCashier
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
            taxValue = activeUserDetails.taxValue,
            taxTitle = activeUserDetails.taxTitle,
            startTime = activeUserDetails.startTime,
            endTime = activeUserDetails.endTime,
            locationMultiCashier = activeUserDetails.locationMultiCashier,
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
        registerEntities: List<RegisterEntity>
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
            locations = toLocations(locationEntities, registerEntities)
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
        registerEntities: List<RegisterEntity>
    ): List<Locations> {
        return locationEntities.map { location ->
            Locations(
                id = location.id,
                name = location.name,
                address = location.address,
                status = location.status,
                zipCode = location.zipCode,
                taxValue = location.taxValue,
                taxTitle = location.taxTitle,
                startTime = location.startTime,
                endTime = location.endTime,
                multiCashier = location.multiCashier,
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

    fun toLocationsAgainstStoreID(entities: List<LocationEntity>): List<Locations> {
        return entities.map { entity ->
            Locations(
                id = entity.id,
                name = entity.name,
                address = entity.address,
                status = entity.status,
                zipCode = entity.zipCode,
                taxValue = entity.taxValue,
                taxTitle = entity.taxTitle,
                startTime = entity.startTime,
                endTime = entity.endTime,
                multiCashier = entity.multiCashier,
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

    fun toLocationAgainstLocationID(locationEntity: LocationEntity): Locations {
        return Locations(
            id = locationEntity.id,
            name = locationEntity.name,
            address = locationEntity.address,
            status = locationEntity.status,
            zipCode = locationEntity.zipCode,
            taxValue = locationEntity.taxValue,
            taxTitle = locationEntity.taxTitle,
            startTime = locationEntity.startTime,
            endTime = locationEntity.endTime,
            multiCashier = locationEntity.multiCashier,
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
            taxValue = entity.taxValue,
            taxTitle = entity.taxTitle,
            startTime = entity.startTime,
            endTime = entity.endTime,
            locationMultiCashier = entity.locationMultiCashier,
            registerId = entity.registerId,
            registerName = entity.registerName,
            registerStatus = entity.registerStatus
        )
    }

    fun toBatchDetails(batchEntity: BatchEntity): Batch {
        return Batch(
            batchNo = batchEntity.batchNo,
            storeId = batchEntity.storeId,
            userId = batchEntity.userId,
            registerId = batchEntity.registerId,
            startingCashAmount = batchEntity.startingCashAmount
        )
    }
}