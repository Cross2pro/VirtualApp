package com.lody.virtual.client.ipc;

import android.app.job.JobInfo;
import android.os.Parcelable;
import android.os.RemoteException;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.helper.ipcbus.IPCSingleton;
import com.lody.virtual.server.interfaces.IJobService;

import java.util.List;

/**
 * @author Lody
 */

public class VJobScheduler {

    private static final VJobScheduler sInstance = new VJobScheduler();

    private IPCSingleton<IJobService> singleton = new IPCSingleton<>(IJobService.class);

    public static VJobScheduler get() {
        return sInstance;
    }

    public IJobService getService() {
        return singleton.get();
    }

    public int schedule(JobInfo job) {
        try {
            return getService().schedule(VClient.get().getVUid(), job);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public List<JobInfo> getAllPendingJobs() {
        try {
            return getService().getAllPendingJobs(VClient.get().getVUid());
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void cancelAll() {
        try {
            getService().cancelAll(VClient.get().getVUid());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cancel(int jobId) {
        try {
            getService().cancel(VClient.get().getVUid(), jobId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public JobInfo getPendingJob(int jobId) {
        try {
            return getService().getPendingJob(VClient.get().getVUid(), jobId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int enqueue(JobInfo job, Object workItem) {
        if (workItem == null) return -1;
        try {
            return getService().enqueue(VClient.get().getVUid(), job, (Parcelable) workItem);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }
}
