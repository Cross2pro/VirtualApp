package com.lody.virtual.server.interfaces;

import android.app.job.JobInfo;
import android.os.Parcelable;
import android.os.RemoteException;

import java.util.List;

/**
 * @author Lody
 */
public interface IJobService extends IPCInterface {

    int schedule(JobInfo job) throws RemoteException;

    void cancel(int jobId) throws RemoteException;

    void cancelAll() throws RemoteException;

    List<JobInfo> getAllPendingJobs() throws RemoteException;

    JobInfo getPendingJob(int jobId) throws RemoteException;

    int enqueue(JobInfo job, Parcelable workItem) throws RemoteException;

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
