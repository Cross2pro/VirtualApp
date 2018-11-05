package io.virtualapp.delegate;

import com.lody.virtual.client.core.SettingHandler;
import com.lody.virtual.helper.compat.NativeLibraryHelperCompat;

import java.io.File;
import java.util.Set;

public class MySettingHandler implements SettingHandler {
    @Override
    public boolean isDisableDlOpen(String packageName, String apkPath) {
        return "com.facebook.katana".equals(packageName)
                || "jianghu2.lanjing.com".equals(packageName)
                || packageName.startsWith("jianghu2.lanjing.com.");
    }

    @Override
    public boolean isDisableNotCopyApk(String packageName, File apkPath) {
        //TODO legu libshella-xxx.so
        Set<String> soList = NativeLibraryHelperCompat.getSoListFromApk(apkPath);
        if (soList == null || soList.contains("libshella-2.8.so") || soList.contains("libme_unipay.so")) {
            return true;
        }
        for (String so : soList) {
            if (so.startsWith("libshella-")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUseRealDataDir(String packageName) {
        //dataDir = /data/data/packageName/
        return false;
    }

    @Override
    public boolean isUseOwnLibraryFiles(String packageName, String apkPath) {
        //com.tencent.tmgp.fgo
        //com.bilibili.fgo
        //com.tencent.tmgp.dwrg
        //TODO 根据apk的某个so判断
//        Set<String> soList = NativeLibraryHelperCompat.getSoListFromApk(new File(apkPath));
//        if (soList != null && soList.contains("libNetHTProtect.so"))) {
//            return false;
//        }
        if ((packageName.startsWith("com.") && packageName.endsWith(".fgo"))
                || (packageName.startsWith("com.") && packageName.endsWith(".dwrg"))
                || "com.tencent.tmgp.pubgmhd".equals(packageName)
                || (packageName.startsWith("com.") && packageName.endsWith(".fatego"))
                || packageName.startsWith("com.izhaohe.heroes")) {
            return false;
        }
        return true;
    }
}
