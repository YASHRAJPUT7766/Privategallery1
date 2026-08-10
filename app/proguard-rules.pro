# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Media3 / ExoPlayer
-dontwarn com.google.android.exoplayer2.**
-keep class androidx.media3.** { *; }

# App domain/data models survive minification with original field names so
# Room's generated code (which reflects on entity fields) keeps working.
-keep class com.yash.privategallery.domain.model.** { *; }
-keep class com.yash.privategallery.data.database.entity.** { *; }
