package com.example.expenser

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ProfileType {
    USER, FAMILY, PET
}

@Entity(
    tableName = "profiles",
    indices = [Index(value = ["type"]), Index(value = ["name"])]
)
data class Profile(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0, // Using Long is preferred for Primary Keys in Room

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "age")
    val age: Int,

    @ColumnInfo(name = "type")
    val type: ProfileType,

    @ColumnInfo(name = "profile_image")
    val profileImage: String? = null
)
