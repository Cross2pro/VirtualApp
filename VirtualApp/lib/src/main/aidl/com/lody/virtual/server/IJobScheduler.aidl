package com.lody.virtual.server;

import android.app.job.JobInfo;
import android.app.job.JobWorkItem;
 /**
  * IPC interface that supports the app-facing {@link #JobScheduler} api.
  */
interface IJobScheduler {
    int schedule(in JobInfo job);
    void cancel(int jobId);
    void cancelAll();
    List<JobInfo> getAllPendingJobs();

    JobInfo getPendingJob(int jobId);
    int enqueue(in JobInfo job, in JobWorkItem workItem);
}
