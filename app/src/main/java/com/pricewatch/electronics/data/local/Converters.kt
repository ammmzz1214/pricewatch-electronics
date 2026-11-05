package com.pricewatch.electronics.data.local

import androidx.room.TypeConverter

class Converters {
    // All enum fields are stored as plain String names; no converter is
    // strictly required for String columns. This class is retained for
    // future non-primitive fields and to satisfy the Room compiler hook.
    @TypeConverter
    fun fromNullableLong(value: Long?): Long? = value

    @TypeConverter
    fun toNullableLong(value: Long?): Long? = value
}
