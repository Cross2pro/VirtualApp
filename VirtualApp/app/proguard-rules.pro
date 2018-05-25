# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/lody/Desktop/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keepattributes *Annotation*,InnerClasses
-keepattributes Signature,EnclosingMethod
-keepclassmembers class * implements java.io.Serializable {*;}
-keepclassmembers class * implements android.io.Parcelable {*;}
-keep class * implements android.os.Parcelable {*;}

-dontwarn android.**
-dontwarn org.eclipse.**
-dontwarn com.google.**
-dontwarn org.slf4j.**
-dontwarn com.tencent.**
-dontwarn mirror.**
-dontwarn com.lody.virtual.server.job.VJobSchedulerService

# Parcelable
-keep class * implements android.io.Parcelable{
    *;
}
# ibinder
-keep class * extends android.os.Binder{
    *;
}
# map
-keep class c.t.m.g.**{*;}
-keep class com.tencent.**{*;}
# hook
-keep class com.lody.virtual.client.hook.base.Inject{*;}
-keep class com.lody.virtual.client.hook.base.LogInvocation{*;}
-keep class com.lody.virtual.client.hook.base.LogInvocation$Condition{*;}
-keep class com.lody.virtual.client.hook.base.SkipInject{*;}
#-keep class com.lody.virtual.client.badger.**{*;}
#-keep class com.lody.virtual.client.hook.base{*;}
#-keep class com.lody.virtual.client.hook.delegate{*;}
#-keep class com.lody.virtual.client.hook.providers{*;}
-keep class com.lody.virtual.client.hook.proxies.** {*;}
-keep class com.lody.virtual.client.ipc.**{*;}

-keep class * implements com.lody.virtual.client.badger.IBadger{
    public *;
}
-keep class * implements com.lody.virtual.server.interfaces.IPCInterface{
    public *;
}
# jni
-keep class com.lody.virtual.client.NativeEngine {
    public static void onKillProcess(...);
    public static int onGetCallingUid(...);
    public static void onOpenDexFileNative(...);
    public static int onGetUid(...);
    native <methods>;
}
# android
-keep class android.**{
    *;
}
-keepclassmembers class mirror.**{
    *;
}
-repackageclass z1
#-keepattributes SourceFile,LineNumberTable

