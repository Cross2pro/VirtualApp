package com.lody.virtual.server.interfaces;

import android.graphics.Bitmap;
import android.os.RemoteException;

import com.lody.virtual.os.VUserInfo;

import java.util.List;

/**
 * @author Lody
 */
public interface IUserManager extends IPCInterface {
    VUserInfo createUser(String name, int flags, String callingPackage) throws RemoteException;

    boolean removeUser(int userHandle, String callingPackage) throws RemoteException;

    void setUserName(int userHandle, String name, String callingPackage) throws RemoteException;

    void setUserIcon(int userHandle, Bitmap icon, String callingPackage) throws RemoteException;

    Bitmap getUserIcon(int userHandle) throws RemoteException;

    List<VUserInfo> getUsers(boolean excludeDying) throws RemoteException;

    VUserInfo getUserInfo(int userHandle) throws RemoteException;

    void setGuestEnabled(boolean enable, String callingPackage) throws RemoteException;

    boolean isGuestEnabled() throws RemoteException;

    void wipeUser(int userHandle, String callingPackage) throws RemoteException;

    int getUserSerialNumber(int userHandle) throws RemoteException;

    int getUserHandle(int userSerialNumber) throws RemoteException;

    abstract class Stub implements IUserManager {
        @Override
        public boolean isBinderAlive() {
            return false;
        }

        @Override
        public boolean pingBinder() {
            return false;
        }
    }
}
