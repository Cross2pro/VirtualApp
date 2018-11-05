package com.lody.virtual.server.interfaces;

import android.app.job.JobInfo;
import android.os.Parcelable;
import android.os.RemoteException;

import java.util.List;

/**
 * @author Lody
 */
public interface IJobService extends IPCInterface {

    int schedule(int uid, JobInfo job) throws RemoteException;

    void cancel(int uid, int jobId) throws RemoteException;

    void cancelAll(int uid) throws RemoteException;

    List<JobInfo> getAllPendingJobs(int uid) throws RemoteException;

    JobInfo getPendingJob(int uid, int jobId) throws RemoteException;

    int enqueue(int uid, JobInfo job, Parcelable workItem) throws RemoteException;

    abstract class Stub implements IJobService {
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
