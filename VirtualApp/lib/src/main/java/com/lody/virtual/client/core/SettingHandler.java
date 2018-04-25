package com.lody.virtual.client.core;

public interface SettingHandler {
    boolean isDisableDlOpen(String packageName);

    boolean isUseRealDataDir(String packageName);
}
