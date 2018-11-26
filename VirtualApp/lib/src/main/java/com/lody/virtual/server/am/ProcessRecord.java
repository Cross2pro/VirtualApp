package com.lody.virtual.server.am;

import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.ConditionVariable;
import android.os.IInterface;

import com.lody.virtual.client.IVClient;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.ClientConfig;

import java.util.HashSet;
import java.util.Set;

final class ProcessRecord extends Binder {

	final ConditionVariable lock = new ConditionVariable();
	public final ApplicationInfo info; // all about the first app in the process
	final public String processName; // name of the process
	final Set<String> pkgList = new HashSet<>(); // List of packages
	public IVClient client;
	IInterface appThread;
	public int pid;
	public int vuid;
	public int vpid;
	public boolean is64bit;
	public int callingUid;
	public int userId;
	boolean doneExecuting;

	public ProcessRecord(ApplicationInfo info, String processName, int vuid, int vpid, boolean is64bit) {
		this(info, processName, vuid, vpid, -1, is64bit);
	}

	public ProcessRecord(ApplicationInfo info, String processName, int vuid, int vpid, int callingUid, boolean is64bit) {
		this.info = info;
		this.vuid = vuid;
		this.vpid = vpid;
		this.userId = VUserHandle.getUserId(vuid);
		this.callingUid = callingUid;
		this.processName = processName;
		this.is64bit = is64bit;
	}

    public void setVCallingUid(int callingUid) {
        if (callingUid < 0) return;
        synchronized (this) {
            if (this.callingUid == 0) {
                this.callingUid = callingUid;
            } else {
                if(isCalling() && callingUid == vuid){
                    return;
                }
                this.callingUid = callingUid;
            }
        }
    }

    public boolean isCalling(){
	    synchronized (this){
	        return this.callingUid != 0 && this.callingUid != vuid;
        }
    }

    public int getVCallingUid() {
        synchronized (this) {
            return callingUid;
        }
    }

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ProcessRecord record = (ProcessRecord) o;
		return processName != null ? processName.equals(record.processName) : record.processName == null;
	}

	public ClientConfig getClientConfig() {
		ClientConfig config = new ClientConfig();
		config.is64Bit = is64bit;
		config.vpid = vpid;
		return config;
	}
}
