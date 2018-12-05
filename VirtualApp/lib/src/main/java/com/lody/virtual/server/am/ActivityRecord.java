package com.lody.virtual.server.am;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Binder;
import android.os.IBinder;

import com.lody.virtual.helper.utils.ComponentUtils;

/**
 * @author Lody
 *
 */

/* package */ class ActivityRecord extends Binder {
    public TaskRecord task;
    public ActivityInfo info;
    public ComponentName component;
    public Intent intent;
    public IBinder token;
    public IBinder resultTo;
    public int userId;
    public ProcessRecord process;
    public boolean marked;

    public ActivityRecord(Intent intent, ActivityInfo info, IBinder resultTo) {
        this.intent = intent;
        this.info = info;
        this.component = ComponentUtils.toComponentName(info);
        this.resultTo = resultTo;
    }

    public void init(TaskRecord task, ProcessRecord process, IBinder token) {
        this.task = task;
        this.process = process;
        this.token = token;
    }

    public boolean isLaunching() {
        return process == null;
    }

}
