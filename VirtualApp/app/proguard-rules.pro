-dontshrink
-keepattributes *Annotation*,InnerClasses
-keepattributes Signature,EnclosingMethod
-keepclassmembers class * implements java.io.Serializable {*;}

-dontwarn android.**
-dontwarn com.tencent.**
-dontwarn andhook.**
-dontwarn org.slf4j.**
-dontwarn org.eclipse.**

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.ContentProvider

# Parcelable
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * extends android.os.Binder{
    public <methods>;
}

-keepclasseswithmembernames class * {
    native <methods>;
}
# android
-keep class android.**{
    *;
}

-repackageclass z2


# Thirdparty Library
-keep class c.t.m.g.**{*;}
-keep class com.tencent.**{*;}
-keep class jonathanfinerty.once.**{public *;}

#-keepattributes SourceFile,LineNumberTable

-keep class andhook.lib.AndHook$Dalvik
-keepclassmembers class andhook.lib.AndHook$Dalvik {
   native <methods>;
}
-keep class andhook.lib.AndHook
-keepclassmembers class andhook.lib.AndHook {
   native <methods>;
}
-keep class andhook.lib.YunOSHelper
-keepclassmembers class andhook.lib.YunOSHelper {
   public *;
}

-keep class de.robv.android.xposed.*
-keepclassmembers class de.robv.android.xposed.* {
   *;
}
-keep class android.app.AndroidAppHelper
-keepclassmembers class android.app.AndroidAppHelper {
   public *;
}

-keepattributes Exceptions, InnerClasses, ...
-keep class andhook.lib.XC_MethodHook
-keepclassmembers class andhook.lib.XC_MethodHook {
   *;
}
-keep class andhook.lib.XC_MethodHook$*
-keepclassmembers class andhook.lib.XC_MethodHook$* {
   *;
}
-keep class * extends andhook.lib.XC_MethodHook
-keepclassmembers class * extends andhook.lib.XC_MethodHook {
   public *;
   protected *;
}
#-keep class * extends andhook.lib.XC_MethodReplacement
#-keepclassmembers class * extends andhook.lib.XC_MethodReplacement {
#   *;
#}

-keep class io.vposed.VPosed
-keep class com.android.**

-keepclassmembers class io.vposed.VPosed {
   public *;
}
