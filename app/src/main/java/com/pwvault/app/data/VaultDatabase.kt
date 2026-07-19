package com.pwvault.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [VaultItemEntity::class], version = 1)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultItemDao(): VaultItemDao
}
