package com.lody.virtual.helper.ipcbus;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.server.interfaces.IPCInterface;

/**
 * @author Lody
 */
public class IPCSingleton<T extends IPCInterface> {

    private Class<?> ipcClass;
    private T instance;

    public IPCSingleton(Class<?> ipcClass) {
        this.ipcClass = ipcClass;
    }

    public T get() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = IPCBus.get(ipcClass);
                }
            }
        }
        if (VirtualCore.get().isMainProcess()) {
            if (!instance.pingBinder()) {
                instance = IPCBus.get(ipcClass);
                VLog.w("ipcbus", "main reload ipc:%s", ipcClass);
            }
        } else if (!instance.isBinderAlive()) {
            instance = IPCBus.get(ipcClass);
            VLog.w("ipcbus", "app and x reload ipc:%s", ipcClass);
        }
        return instance;
    }
}
