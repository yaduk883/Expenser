package com.example.expenser

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ID നിർബന്ധമാണ്
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val category: String,
    @ColumnInfo(name = "sub_category") val subCategory: String,
    val date: Instant,
    @ColumnInfo(name = "profile_id") val profileId: Long
) {
    val amountAsDecimal: Double
        get() = amountCents / 100.0
}