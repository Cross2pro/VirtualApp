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
-dontshrink
-keepattributes *Annotation*,InnerClasses
-keepattributes Signature,EnclosingMethod
-keepclassmembers class * implements java.io.Serializable {*;}

-dontwarn android.**
-dontwarn org.eclipse.**
-dontwarn com.google.**
-dontwarn org.slf4j.**
-dontwarn com.tencent.**
-dontwarn mirror.**
-dontwarn com.lody.virtual.server.job.VJobSchedulerService

# Parcelable
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# ibinder
-keepclassmembers class * extends android.os.Binder{
    public <methods>;
}

#fix multi dex
-keep @interface com.lody.virtual.client.hook.annotations.** {*;}

#Parcelable map old version data
-keep class com.lody.virtual.server.pm.PackageSetting{
 public static final android.os.Parcelable$Creator *;
}
-keep class com.lody.virtual.server.pm.PackageUserState{
 public static final android.os.Parcelable$Creator *;
}
-keep class com.lody.virtual.server.location.VirtualLocationService$VLocConfig{
 public static final android.os.Parcelable$Creator *;
}
-keep class com.lody.virtual.server.vs.VSConfig{
 public static final android.os.Parcelable$Creator *;
}

#ipc
-keepclassmembers class com.lody.virtual.client.ipc.**{public *;}
#native
-keepclassmembers class * implements com.lody.virtual.client.badger.IBadger{
    public <methods>;
}

-keepclasseswithmembernames class * {
    native <methods>;
}
# android
-keep class android.**{
    *;
}
-keep @interface mirror.** {*;}
-keepclassmembers class mirror.**{
   public *;
}
#64bit
-keep class com.lody.virtual.server.bit64.V64BitHelper{
    public <methods>;
}
-repackageclass z1

# map
-keep class c.t.m.g.**{*;}
-keep class com.tencent.**{*;}
# once
-keep class jonathanfinerty.once.**{public *;}
#debug
#-keepattributes SourceFile,LineNumberTable

