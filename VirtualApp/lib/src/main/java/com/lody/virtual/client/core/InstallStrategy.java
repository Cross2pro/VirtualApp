package com.lody.virtual.client.core;

/**
 * @author Lody
 *
 *
 */
public interface InstallStrategy {
	int TERMINATE_IF_EXIST = 0x01 << 1;

    int FORCE_UPDATE = 0x01 << 2;
	int COMPARE_VERSION = 0X01 << 3;
	int IGNORE_NEW_VERSION = 0x01 << 4;
	int NOT_COPY_APK = 0x01 << 5;

    /**
     * @see #NOT_COPY_APK
     */
    @Deprecated
	int DEPEND_SYSTEM_IF_EXIST = NOT_COPY_APK;
    @Deprecated
    int UPDATE_IF_EXIST = FORCE_UPDATE;

}