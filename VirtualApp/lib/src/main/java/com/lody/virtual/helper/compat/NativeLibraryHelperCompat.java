package com.lody.virtual.helper.compat;

import android.annotation.TargetApi;
import android.os.Build;

import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;

import java.io.File;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import mirror.com.android.internal.content.NativeLibraryHelper;
import mirror.dalvik.system.VMRuntime;

public class NativeLibraryHelperCompat {

    private static String TAG = NativeLibraryHelperCompat.class.getSimpleName();

    public static int copyNativeBinaries(File apkFile, File sharedLibraryDir) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return copyNativeBinariesAfterL(apkFile, sharedLibraryDir);
        } else {
            return copyNativeBinariesBeforeL(apkFile, sharedLibraryDir);
        }
    }

    private static int copyNativeBinariesBeforeL(File apkFile, File sharedLibraryDir) {
        try {
            return Reflect.on(NativeLibraryHelper.TYPE).call("copyNativeBinariesIfNeededLI", apkFile, sharedLibraryDir)
                    .get();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean isSupportNative32(String apkPath) {
        //system is 32bit
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.SUPPORTED_64_BIT_ABIS.length == 0) {
                return true;
            }
        }
        Set<String> supportedABIs = getABIsFromApk(apkPath);
        if (supportedABIs == null || supportedABIs.isEmpty()) {
            //no has so
            return true;
        }
        //find 32 so
        for (String supportedAbi : supportedABIs) {
            if ("armeabi".endsWith(supportedAbi)
                    || "armeabi-v7a".equals(supportedAbi)
                    || "x86".equals(supportedAbi)
                    || "mips".equals(supportedAbi)) {
                return true;
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static int copyNativeBinariesAfterL(File apkFile, File sharedLibraryDir) {
        try {
            Object handle = NativeLibraryHelper.Handle.create.call(apkFile);
            if (handle == null) {
                return -1;
            }

            String abi = null;
            Set<String> abiSet = getABIsFromApk(apkFile.getAbsolutePath());
            if (abiSet == null || abiSet.isEmpty()) {
                return 0;
            }
            boolean is64Bit = VMRuntime.is64Bit.call(VMRuntime.getRuntime.call());
            if (is64Bit && isVM64(abiSet)) {
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                    int abiIndex = NativeLibraryHelper.findSupportedAbi.call(handle, Build.SUPPORTED_64_BIT_ABIS);
                    if (abiIndex >= 0) {
                        abi = Build.SUPPORTED_64_BIT_ABIS[abiIndex];
                    }
                }
            } else {
                if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                    int abiIndex = NativeLibraryHelper.findSupportedAbi.call(handle, Build.SUPPORTED_32_BIT_ABIS);
                    if (abiIndex >= 0) {
                        abi = Build.SUPPORTED_32_BIT_ABIS[abiIndex];
                    }
                }
            }

            if (abi == null) {
                VLog.e(TAG, "Not match any abi [%s].", apkFile.getPath());
                return -1;
            }
            return NativeLibraryHelper.copyNativeBinaries.call(handle, sharedLibraryDir, abi);
        } catch (Throwable e) {
            VLog.d(TAG, "copyNativeBinaries with error : %s", e.getLocalizedMessage());
            e.printStackTrace();
        }

        return -1;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean isVM64(Set<String> supportedABIs) {
        if (Build.SUPPORTED_64_BIT_ABIS.length == 0) {
            return false;
        }

        if (supportedABIs == null || supportedABIs.isEmpty()) {
            return true;
        }

        for (String supportedAbi : supportedABIs) {
            if ("arm64-v8a".endsWith(supportedAbi)
                    || "x86_64".equals(supportedAbi)
                    || "mips64".equals(supportedAbi)) {
                return true;
            }
        }

        return false;
    }

    private static Set<String> getABIsFromApk(String apk) {
        try {
            ZipFile apkFile = new ZipFile(apk);
            Enumeration<? extends ZipEntry> entries = apkFile.entries();
            Set<String> supportedABIs = new HashSet<String>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.contains("../")) {
                    continue;
                }
                if (name.startsWith("lib/") && !entry.isDirectory() && name.endsWith(".so")) {
                    String supportedAbi = name.substring(name.indexOf("/") + 1, name.lastIndexOf("/"));
                    supportedABIs.add(supportedAbi);
                }
            }
            return supportedABIs;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static Set<String> getSoListFromApk(File apk) {
        try {
            ZipFile apkFile = new ZipFile(apk);
            Enumeration<? extends ZipEntry> entries = apkFile.entries();
            Set<String> solist = new HashSet<String>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.contains("../")) {
                    continue;
                }
                if (name.startsWith("lib/") && !entry.isDirectory() && name.endsWith(".so")) {
                    String so = name.substring(name.lastIndexOf("/")+1);
                    if(!solist.contains(so)){
                        solist.add(so);
                    }
                }
            }
            return solist;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
