package com.lody.virtual.client.core;

import java.io.File;

public interface SettingHandler {
    boolean isDisableDlOpen(String packageName);

    boolean isUseRealDataDir(String packageName);

    boolean isDisableNotCopyApk(String packageName, File apkPath);

    boolean isUseVirtualLibraryFiles(String packageName, String apkPath);
}
