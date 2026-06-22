# Keep Hilt
-keepclassmembers class * {
    @dagger.hilt.android.EarlyEntryPoint <fields>;
    @dagger.hilt.android.EarlyEntryPoint <methods>;
}

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
