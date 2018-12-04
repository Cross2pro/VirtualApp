package com.lody.virtual.client.stub;

import android.annotation.TargetApi;
import android.app.Service;
import android.app.job.IJobCallback;
import android.app.job.IJobService;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.hook.proxies.am.ActivityManagerStub;
import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.helper.compat.JobWorkItemCompat;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.server.job.VJobSchedulerService;

import java.util.Map;

import static com.lody.virtual.server.job.VJobSchedulerService.get;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ShadowJobWorkService extends Service {
    private static final boolean debug = false;
    private static final String TAG = ShadowJobWorkService.class.getSimpleName();
    private final SparseArray<JobSession> mJobSessions = new SparseArray<>();
    private JobScheduler mScheduler;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            String action = intent.getAction();
            if("action.startJob".equals(action)){
                JobParameters jobParams = intent.getParcelableExtra("jobParams");
                startJob(jobParams);
            }else if("action.stopJob".equals(action)){
                JobParameters jobParams = intent.getParcelableExtra("jobParams");
                stopJob(jobParams);
            }
        }
        return START_NOT_STICKY;
    }

    public static void startJob(Context context, JobParameters jobParams) {
        Intent intent = new Intent("action.startJob");
        intent.setPackage(context.getPackageName());
        intent.putExtra("jobParams", jobParams);
        context.startService(intent);

    }

    public static void stopJob(Context context, JobParameters jobParams) {
        Intent intent = new Intent("action.stopJob");
        intent.setPackage(context.getPackageName());
        intent.putExtra("jobParams", jobParams);
        context.startService(intent);
    }

    /**
     * Make JobScheduler happy.
     */
    private void emptyCallback(IJobCallback callback, int jobId) {
        try {
            callback.acknowledgeStartMessage(jobId, false);
            callback.jobFinished(jobId, false);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        InvocationStubManager.getInstance().checkEnv(ActivityManagerStub.class);
        mScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
    }

    @Override
    public void onDestroy() {
        if(debug) {
            VLog.i(TAG, "ShadowJobService:onDestroy");
        }
        synchronized (mJobSessions) {
            for (int i = mJobSessions.size() - 1; i >= 0; i--) {
                JobSession session = mJobSessions.valueAt(i);
                session.stopSessionLocked();
            }
            mJobSessions.clear();
        }
        super.onDestroy();
    }


    public void startJob(JobParameters jobParams) {
        int jobId = jobParams.getJobId();
        IBinder binder = mirror.android.app.job.JobParameters.callback.get(jobParams);
        IJobCallback callback = IJobCallback.Stub.asInterface(binder);
        Map.Entry<VJobSchedulerService.JobId, VJobSchedulerService.JobConfig> entry = get().findJobByVirtualJobId(jobId);
        if (entry == null) {
            emptyCallback(callback, jobId);
            mScheduler.cancel(jobId);
        } else {
            VJobSchedulerService.JobId key = entry.getKey();
            VJobSchedulerService.JobConfig config = entry.getValue();
            JobSession session;
            synchronized (mJobSessions) {
                session = mJobSessions.get(jobId);
            }
            if (session != null) {
                // Job Session has exist.
                session.startJob(true);
            } else {
                boolean bound = false;
                synchronized (mJobSessions) {
                    mirror.android.app.job.JobParameters.jobId.set(jobParams, key.clientJobId);
                    session = new JobSession(jobId, callback, jobParams, key.packageName);
                    mirror.android.app.job.JobParameters.callback.set(jobParams, session.asBinder());
                    mJobSessions.put(jobId, session);
                    Intent service = new Intent();
                    service.setComponent(new ComponentName(key.packageName, config.serviceName));
                    service.putExtra("_VA_|_user_id_", VUserHandle.getUserId(key.vuid));
                    try {
                        if (debug) {
                            VLog.i(TAG, "ShadowJobService:binService:%s, jobId=%s",
                                    service.getComponent(), jobId);
                        }
                        bound = bindService(service, session, Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND);
                    } catch (Throwable e) {
                        VLog.e(TAG, e);
                    }
                }
                if (!bound) {
                    synchronized (mJobSessions) {
                        mJobSessions.remove(jobId);
                    }
                    emptyCallback(callback, jobId);
                    mScheduler.cancel(jobId);
                    get().cancel(-1, jobId);
                }
            }
        }
    }

    public void stopJob(JobParameters jobParams) {
        int jobId = jobParams.getJobId();
        JobSession session;
        synchronized (mJobSessions) {
            session = mJobSessions.get(jobId);
            if (session != null) {
                if(debug) {
                    VLog.i(TAG, "stopJob:%d", jobId);
                }
                session.stopSessionLocked();
            }
        }
    }

    private final class JobSession extends IJobCallback.Stub implements ServiceConnection {

        private int jobId;
        private IJobCallback clientCallback;
        private JobParameters jobParams;
        private IJobService clientJobService;
        private boolean isWorking;
        private String packageName;

        JobSession(int jobId, IJobCallback clientCallback, JobParameters jobParams, String packageName) {
            this.jobId = jobId;
            this.clientCallback = clientCallback;
            this.jobParams = jobParams;
            this.packageName = packageName;
        }

        @Override
        public void acknowledgeStartMessage(int jobId, boolean ongoing) throws RemoteException {
            isWorking = true;
            if(debug) {
                VLog.i(TAG, "ShadowJobService:acknowledgeStartMessage:%d", this.jobId);
            }
            clientCallback.acknowledgeStartMessage(jobId, ongoing);
        }

        @Override
        public void acknowledgeStopMessage(int jobId, boolean reschedule) throws RemoteException {
            isWorking = false;
            if(debug) {
                VLog.i(TAG, "ShadowJobService:acknowledgeStopMessage:%d", this.jobId);
            }
            clientCallback.acknowledgeStopMessage(jobId, reschedule);
        }

        @Override
        public void jobFinished(int jobId, boolean reschedule) throws RemoteException {
            isWorking = false;
            if(debug) {
                VLog.i(TAG, "ShadowJobService:jobFinished:%d", this.jobId);
            }
            clientCallback.jobFinished(jobId, reschedule);
        }

        @Override
        public boolean completeWork(int jobId, int workId) throws RemoteException {
            if(debug) {
                VLog.i(TAG, "ShadowJobService:completeWork:%d", this.jobId);
            }
            return clientCallback.completeWork(jobId, workId);
        }

        @Override
        public JobWorkItem dequeueWork(int jobId) throws RemoteException {
            if(debug) {
                VLog.i(TAG, "ShadowJobService:dequeueWork:%d", this.jobId);
            }
            JobWorkItem workItem = clientCallback.dequeueWork(jobId);
            if(workItem != null){
                return JobWorkItemCompat.redirect(workItem, packageName);
            }
            return null;
        }

        public void startJob(boolean wait){
            if(isWorking){
                if(debug) {
                    VLog.w(TAG, "ShadowJobService:startJob:%d,but is working", jobId);
                }
                return;
            }
            if(debug) {
                VLog.i(TAG, "ShadowJobService:startJob:%d", jobId);
            }
            if (clientJobService == null) {
                if(!wait) {
                    emptyCallback(clientCallback, jobId);
                    synchronized (mJobSessions) {
                        stopSessionLocked();
                    }
                }
                return;
            }
            try {
                clientJobService.startJob(jobParams);
            } catch (RemoteException e) {
                forceFinishJob();
                if(debug) {
                    Log.e(TAG, "ShadowJobService:startJob", e);
                }
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if(debug) {
                VLog.i(TAG, "ShadowJobService:onServiceConnected:%s", name);
            }
            clientJobService = IJobService.Stub.asInterface(service);
            startJob(false);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        void forceFinishJob() {
            try {
                clientCallback.jobFinished(jobId, false);
            } catch (RemoteException e) {
                e.printStackTrace();
            } finally {
                synchronized (mJobSessions) {
                    stopSessionLocked();
                }
            }
        }

        void stopSessionLocked() {
            if(debug) {
                VLog.i(TAG, "ShadowJobService:stopSession:%d", jobId);
            }
            if (clientJobService != null) {
                try {
                    clientJobService.stopJob(jobParams);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            mJobSessions.remove(jobId);
            unbindService(this);
        }
    }

}
