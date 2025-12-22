package com.retail.dolphinpos.data.entities.sync

import androidx.room.TypeConverter

class SyncTypeConverters {
    
    @TypeConverter
    fun fromCommandType(commandType: CommandType): String {
        return commandType.name
    }
    
    @TypeConverter
    fun toCommandType(value: String): CommandType {
        return CommandType.valueOf(value)
    }
    
    @TypeConverter
    fun fromCommandStatus(status: CommandStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toCommandStatus(value: String): CommandStatus {
        return CommandStatus.valueOf(value)
    }
}

