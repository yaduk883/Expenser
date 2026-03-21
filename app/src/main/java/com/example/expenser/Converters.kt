package com.example.expenser

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromInstant(value: String?): Instant? = value?.let { Instant.parse(it) }

    @TypeConverter
    fun instantToString(instant: Instant?): String? = instant?.toString()

    @TypeConverter
    fun fromProfileType(type: ProfileType): String = type.name

    @TypeConverter
    fun toProfileType(value: String): ProfileType = ProfileType.valueOf(value)
}