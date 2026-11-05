# PriceWatch Electronics — ProGuard / R8 rules
-keep class com.pricewatch.electronics.data.local.** { *; }
-keep class com.pricewatch.electronics.domain.models.** { *; }
-keepattributes *Annotation*, Signature
-dontwarn kotlinx.serialization.**
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
# Coil
-dontwarn coil.**
# Room
-keep class * extends androidx.room.RoomDatabase
