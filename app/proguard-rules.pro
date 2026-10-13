# Flowlog release (R8) rules.
#
# The app reads Firestore documents through Map access (no toObject/POJO reflection),
# Room/WorkManager/Compose/kotlinx.serialization/Firebase ship their own consumer
# rules, and manifest components are kept automatically, so no broad -keep rules
# are needed here.

# Keep file/line info so Crashlytics stack traces stay readable after obfuscation.
# The Crashlytics Gradle plugin uploads the mapping file for release builds.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip verbose/debug/info logging from release builds. Several of these lines
# include Todo/activity titles or the Firebase uid, which must not reach logcat.
# Log.w/e stay for diagnostics and must not contain user content.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
