package com.lody.virtual.server.interfaces;

import android.graphics.Bitmap;
import android.os.RemoteException;

import com.lody.virtual.os.VUserInfo;

import java.util.List;

/**
 * @author Lody
 */
interface IUserManager{
    VUserInfo createUser(String name, int flags, String callingPackage);

    boolean removeUser(int userHandle, String callingPackage);

    void setUserName(int userHandle, String name, String callingPackage);

    void setUserIcon(int userHandle,in  Bitmap icon, String callingPackage);

    Bitmap getUserIcon(int userHandle);

    List<VUserInfo> getUsers(boolean excludeDying);

    VUserInfo getUserInfo(int userHandle);

    void setGuestEnabled(boolean enable, String callingPackage);

    boolean isGuestEnabled();

    void wipeUser(int userHandle, String callingPackage);

    int getUserSerialNumber(int userHandle);

    int getUserHandle(int userSerialNumber);
}
