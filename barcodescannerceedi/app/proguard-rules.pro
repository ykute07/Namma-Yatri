# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable
# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Kotlin
#https://stackoverflow.com/questions/33547643/how-to-use-kotlin-with-proguard
#https://medium.com/@AthorNZ/kotlin-metadata-jackson-and-proguard-f64f51e5ed32
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Android X
-dontwarn androidx.**
-dontwarn com.google.android.material.**
-keep interface androidx.* { *; }
-keep class androidx.** { *; }
-keep class com.google.android.material.** { *; }

# RxJava, RxAndroid (https://gist.github.com/kosiara/487868792fbd3214f9c9)
#noinspection ShrinkerUnresolvedReference
-keep class rx.schedulers.Schedulers {
    public static <methods>;
}
#noinspection ShrinkerUnresolvedReference
-keep class rx.schedulers.ImmediateScheduler {
    public <methods>;
}
#noinspection ShrinkerUnresolvedReference
-keep class rx.schedulers.TestScheduler {
    public <methods>;
}
#noinspection ShrinkerUnresolvedReference
-keep class rx.schedulers.Schedulers {
    public static ** test();
}
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
#noinspection ShrinkerUnresolvedReference
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    long producerNode;
    long consumerNode;
}
-dontwarn sun.misc.Unsafe
-dontwarn org.reactivestreams.FlowAdapters
-dontwarn org.reactivestreams.**
-dontwarn java.util.concurrent.flow.**
-dontwarn java.util.concurrent.**

### Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
#noinspection ShrinkerUnresolvedReference
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }

# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
#noinspection ShrinkerUnresolvedReference
-keep class * implements com.google.gson.TypeAdapterFactory
#noinspection ShrinkerUnresolvedReference
-keep class * implements com.google.gson.JsonSerializer
#noinspection ShrinkerUnresolvedReference
-keep class * implements com.google.gson.JsonDeserializer

# LeakCanary
-keep class org.eclipse.mat.** { *; }
-keep class com.squareup.leakcanary.** { *; }

# Room
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase

# VCard Parser
-dontwarn ezvcard.**
-keep,includedescriptorclasses class ezvcard.** { *; }