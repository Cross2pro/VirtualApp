package io.virtualapp.delegate;

import com.lody.virtual.client.core.SettingHandler;
import com.lody.virtual.helper.compat.NativeLibraryHelperCompat;

import java.io.File;
import java.util.Set;

public class MySettingHandler implements SettingHandler {
    @Override
    public boolean isDisableDlOpen(String packageName) {
        return "com.facebook.katana".equals(packageName)
                || "jianghu2.lanjing.com".equals(packageName)
                || packageName.startsWith("jianghu2.lanjing.com.");
    }

    @Override
    public boolean isDisableNotCopyApk(String packageName, File apkPath) {
        //TODO legu libshella-xxx.so
        Set<String> soList = NativeLibraryHelperCompat.getSoListFromApk(apkPath);
        if (soList == null || soList.contains("libshella-2.8.so")) {
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
    public boolean isUseVirtualLibraryFiles(String packageName, String apkPath) {
        //com.tencent.tmgp.fgo
        //com.bilibili.fgo
        //com.tencent.tmgp.dwrg
        //TODO 根据apk的某个so判断
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
