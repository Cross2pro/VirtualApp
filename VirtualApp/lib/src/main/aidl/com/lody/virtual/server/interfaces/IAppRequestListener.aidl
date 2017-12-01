// IAppRequestListener.aidl
package com.lody.virtual.server.interfaces;

interface IAppRequestListener {
    void onRequestInstall(in String path, boolean file);
    void onRequestUninstall(in String pkg);
}
