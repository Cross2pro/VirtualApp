package com.lody.virtual.server.interfaces;

import android.os.RemoteException;

import com.lody.virtual.remote.vloc.VCell;
import com.lody.virtual.remote.vloc.VLocation;

import java.util.List;

/**
 * @author Lody
 */
public interface IVirtualLocationManager extends IPCInterface {

    int getMode(int userId, String pkg) throws RemoteException;

    void setMode(int userId, String pkg, int mode) throws RemoteException;

    void setCell(int userId, String pkg, VCell cell) throws RemoteException;

    void setAllCell(int userId, String pkg, List<VCell> cell) throws RemoteException;

    void setNeighboringCell(int userId, String pkg, List<VCell> cell) throws RemoteException;

    void setGlobalCell(VCell cell) throws RemoteException;

    void setGlobalAllCell(List<VCell> cell) throws RemoteException;

    void setGlobalNeighboringCell(List<VCell> cell) throws RemoteException;

    VCell getCell(int userId, String pkg) throws RemoteException;

    List<VCell> getAllCell(int userId, String pkg) throws RemoteException;

    List<VCell> getNeighboringCell(int userId, String pkg) throws RemoteException;

    void setLocation(int userId, String pkg, VLocation loc) throws RemoteException;

    VLocation getLocation(int userId, String pkg) throws RemoteException;

    void setGlobalLocation(VLocation loc) throws RemoteException;

    VLocation getGlobalLocation() throws RemoteException;

    abstract class Stub implements IVirtualLocationManager {
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
