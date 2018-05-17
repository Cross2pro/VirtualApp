// IAppPermissionCallback.aidl
package com.lody.virtual.server.interfaces;

// Declare any non-default types here with import statements

interface IAppPermissionCallback {
    void onPermissionTrigger(in String appPackageName,in String permissionName);
}
