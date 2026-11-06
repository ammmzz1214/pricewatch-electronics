package com.pricewatch.electronics.data.local
import androidx.room.TypeConverter
class Converters {
    @TypeConverter
    fun fromNullableLong(value: Long?): Long? = value
}
