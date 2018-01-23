package com.lody.virtual.helper.ipcbus;

import android.os.IBinder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author Lody
 */
public class IPCInvocationBridge implements InvocationHandler {

    private ServerInterface serverInterface;
    private IBinder binder;

    public IPCInvocationBridge(ServerInterface serverInterface, IBinder binder) {
        this.serverInterface = serverInterface;
        this.binder = binder;
    }

    private boolean isBinderAlive() {
        return binder != null && binder.isBinderAlive();
    }

    private boolean pingBinder() {
        return binder != null && binder.pingBinder();
    }

    @Override
    public Object invoke(Object o, Method method, Object[] args) throws Throwable {
        if("pingBinder".equals(method.getName())){
            return pingBinder();
        }else if("isBinderAlive".equals(method.getName())){
            return isBinderAlive();
        }
        IPCMethod ipcMethod = serverInterface.getIPCMethod(method);
        if (ipcMethod == null) {
            throw new IllegalStateException("Can not found the ipc method : " + method.getDeclaringClass().getName() + "@" +  method.getName());
        }
        return ipcMethod.callRemote(binder, args);
    }
}
