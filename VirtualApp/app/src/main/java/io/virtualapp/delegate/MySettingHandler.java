package io.virtualapp.delegate;

import com.lody.virtual.client.core.SettingHandler;

public class MySettingHandler implements SettingHandler {
    @Override
    public boolean isDisableDlOpen(String packageName) {
        return "com.facebook.katana".equals(packageName)
                || "jianghu2.lanjing.com".equals(packageName)
                || packageName.startsWith("jianghu2.lanjing.com.");
    }

    @Override
    public boolean isDisableNotCopyApk(String packageName) {
        return "com.imangi.templerun2".equals(packageName);
    }

    @Override
    public boolean isUseRealDataDir(String packageName) {
        //dataDir = /data/data/packageName/
        return false;
    }
}
