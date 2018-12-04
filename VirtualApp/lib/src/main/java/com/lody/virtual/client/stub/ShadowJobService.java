package com.lody.virtual.client.stub;

import android.annotation.TargetApi;
import android.app.Service;
import android.app.job.IJobService;
import android.app.job.JobParameters;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/**
 * @author Lody
 * <p>
 * This service running on the Server process.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ShadowJobService extends Service {

    private final IJobService mService = new IJobService.Stub() {

        @Override
        public void startJob(JobParameters jobParams) {
            ShadowJobWorkService.startJob(ShadowJobService.this, jobParams);
        }

        @Override
        public void stopJob(JobParameters jobParams) {
            ShadowJobWorkService.stopJob(ShadowJobService.this, jobParams);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mService.asBinder();
    }

}
