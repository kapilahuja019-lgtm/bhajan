# Optimization rules for Bhajan Sangrah

# Media3 ExoPlayer & Session rules
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-dontwarn androidx.media3.**

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# Keep our data models for asset parsing
-keep class com.example.data.model.** { *; }
-keep class com.example.data.db.** { *; }

# Coil image loading
-keep class coil.** { *; }
-dontwarn coil.**

# General shrinker optimizations
-repackageclasses
-allowaccessmodification
