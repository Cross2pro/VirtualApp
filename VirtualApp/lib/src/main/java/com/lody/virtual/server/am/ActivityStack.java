package com.lody.virtual.server.am;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.helper.compat.ObjectsCompat;
import com.lody.virtual.helper.utils.ArrayUtils;
import com.lody.virtual.helper.utils.ClassUtils;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.remote.AppTaskInfo;
import com.lody.virtual.remote.StubActivityRecord;
import com.lody.virtual.server.pm.PackageCacheManager;
import com.lody.virtual.server.pm.PackageSetting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import mirror.android.app.ActivityManagerNative;
import mirror.android.app.ActivityThread;
import mirror.android.app.IActivityManager;
import mirror.android.app.IApplicationThread;
import mirror.com.android.internal.R_Hide;

import static android.content.pm.ActivityInfo.LAUNCH_MULTIPLE;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_INSTANCE;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_TASK;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_TOP;

/**
 * @author Lody
 */

/* package */ class ActivityStack {

    private final ActivityManager mAM;
    private final VActivityManagerService mService;

    /**
     * [Key] = TaskId [Value] = TaskRecord
     */
    private final SparseArray<TaskRecord> mHistory = new SparseArray<>();
    private final List<ActivityRecord> mLaunchingActivities = new ArrayList<>();


    ActivityStack(VActivityManagerService mService) {
        this.mService = mService;
        mAM = (ActivityManager) VirtualCore.get().getContext().getSystemService(Context.ACTIVITY_SERVICE);
    }

    private static void removeFlags(Intent intent, int flags) {
        intent.setFlags(intent.getFlags() & ~flags);
    }

    private static boolean containFlags(Intent intent, int flags) {
        return (intent.getFlags() & flags) != 0;
    }

    private static ActivityRecord topActivityInTask(TaskRecord task) {
        synchronized (task.activities) {
            for (int size = task.activities.size() - 1; size >= 0; size--) {
                ActivityRecord r = task.activities.get(size);
                if (!r.marked) {
                    return r;
                }
            }
            return null;
        }
    }


    private void deliverNewIntentLocked(int userId, ActivityRecord sourceRecord, ActivityRecord targetRecord, Intent intent) {
        if (targetRecord == null) {
            return;
        }
        String creator = getCallingPackage(userId, sourceRecord);
        if (creator == null) {
            creator = "android";
        }
        try {
            targetRecord.process.client.scheduleNewIntent(creator, targetRecord.token, intent);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    private TaskRecord findTaskByAffinityLocked(int userId, String affinity) {
        for (int i = 0; i < this.mHistory.size(); i++) {
            TaskRecord r = this.mHistory.valueAt(i);
            if (userId == r.userId && affinity.equals(r.affinity)) {
                return r;
            }
        }
        return null;
    }

    private TaskRecord findTaskByIntentLocked(int userId, Intent intent) {
        for (int i = 0; i < this.mHistory.size(); i++) {
            TaskRecord r = this.mHistory.valueAt(i);
            if (userId == r.userId && r.taskRoot != null
                    && ObjectsCompat.equals(intent.getComponent(), r.taskRoot.getComponent())) {
                return r;
            }
        }
        return null;
    }

    private ActivityRecord findActivityByToken(int userId, IBinder token) {
        ActivityRecord target = null;
        if (token != null) {
            for (int i = 0; i < this.mHistory.size(); i++) {
                TaskRecord task = this.mHistory.valueAt(i);
                if (task.userId != userId) {
                    continue;
                }
                synchronized (task.activities) {
                    for (ActivityRecord r : task.activities) {
                        if (r.token == token) {
                            target = r;
                        }
                    }
                }
            }
        }
        return target;
    }

    /**
     * App started in VA may be removed in OverView screen, then AMS.removeTask
     * will be invoked, all data struct about the task in AMS are released,
     * while the client's process is still alive. So remove related data in VA
     * as well. A new TaskRecord will be recreated in `onActivityCreated`
     */
    private void optimizeTasksLocked() {
        List<ActivityManager.RecentTaskInfo> recentTask = VirtualCore.get().getRecentTasksEx(Integer.MAX_VALUE,
                ActivityManager.RECENT_WITH_EXCLUDED | ActivityManager.RECENT_IGNORE_UNAVAILABLE);
        int N = mHistory.size();
        while (N-- > 0) {
            TaskRecord task = mHistory.valueAt(N);
            ListIterator<ActivityManager.RecentTaskInfo> iterator = recentTask.listIterator();
            boolean taskAlive = false;
            while (iterator.hasNext()) {
                ActivityManager.RecentTaskInfo info = iterator.next();
                if (info.id == task.taskId) {
                    taskAlive = true;
                    iterator.remove();
                    break;
                }
            }
            if (!taskAlive) {
                mHistory.removeAt(N);
            }
        }
    }

    void finishAllActivity(ProcessRecord record) {
        synchronized (mHistory) {
            int N = mHistory.size();
            while (N-- > 0) {
                TaskRecord task = mHistory.valueAt(N);
                synchronized (task.activities) {
                    for (ActivityRecord r : task.activities) {
                        if (r.process.pid == record.pid) {
                            Log.e("wxd", " finishActivity : " + r.component);
                            try {
                                task.activities.remove(r);
                                r.process.client.finishActivity(r.token);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }


    int startActivitiesLocked(int userId, Intent[] intents, ActivityInfo[] infos, String[] resolvedTypes, IBinder resultTo, Bundle options, int callingUid) {
        synchronized (mHistory) {
            optimizeTasksLocked();
        }
        Intent intent = intents[0];
        ActivityInfo info = infos[0];
        ActivityRecord sourceRecord = findActivityByToken(userId, resultTo);
        String affinity = ComponentUtils.getTaskAffinity(info);
        TaskRecord sourceTask = null;
        if (sourceRecord != null) {
            sourceTask = sourceRecord.task;
        }
        boolean newTask = containFlags(intent, Intent.FLAG_ACTIVITY_NEW_TASK);
        boolean multipleTask = newTask && containFlags(intent, Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        TaskRecord reuseTask = null;
        if (!multipleTask) {
            switch (info.launchMode) {
                case LAUNCH_MULTIPLE: {
                    if (sourceRecord != null && sourceRecord.info.launchMode != LAUNCH_SINGLE_INSTANCE) {
                        reuseTask = sourceTask;
                    }
                    break;
                }
                case LAUNCH_SINGLE_INSTANCE: {
                    reuseTask = findTaskByAffinityLocked(userId, affinity);
                    break;
                }
                case LAUNCH_SINGLE_TASK: {
                    if (newTask || sourceTask == null) {
                        reuseTask = findTaskByAffinityLocked(userId, affinity);
                    } else {
                        reuseTask = sourceTask;
                    }
                    break;
                }
                case LAUNCH_SINGLE_TOP: {
                    reuseTask = findTaskByAffinityLocked(userId, affinity);
                    break;
                }

            }
        }
        Intent[] destIntents = startActivitiesProcess(userId, intents, infos, sourceRecord, callingUid);
        if (reuseTask == null) {
            realStartActivitiesLocked(null, destIntents, resolvedTypes, options);
        } else {
            ActivityRecord top = topActivityInTask(reuseTask);
            if (top != null) {
                realStartActivitiesLocked(top.token, destIntents, resolvedTypes, options);
            }
        }
        return 0;
    }

    private Intent[] startActivitiesProcess(int userId, Intent[] intents, ActivityInfo[] infos, ActivityRecord resultTo, int callingUid) {
        Intent[] destIntents = new Intent[intents.length];
        for (int i = 0; i < intents.length; i++) {
            destIntents[i] = startActivityProcess(userId, resultTo, intents[i], infos[i], callingUid);
        }
        return destIntents;
    }

    private boolean isAllowUseSourceTask(ActivityRecord source, ActivityInfo info) {
        if (source == null) {
            return false;
        }
        if (source.info.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
            return false;
        }
        return true;
    }

    int startActivityLocked(int userId, Intent intent, ActivityInfo info, IBinder resultTo, Bundle options,
                            String resultWho, int requestCode, int callingUid) {
        synchronized (mHistory) {
            optimizeTasksLocked();
        }
        ActivityRecord sourceRecord = findActivityByToken(userId, resultTo);
        if (sourceRecord == null) {
            resultTo = null;
        }
        String affinity = ComponentUtils.getTaskAffinity(info);
        int mLauncherFlags = 0;
        boolean newTask = containFlags(intent, Intent.FLAG_ACTIVITY_NEW_TASK);
        boolean forwardResult = containFlags(intent, Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        boolean noHistory = containFlags(intent, Intent.FLAG_ACTIVITY_NO_HISTORY);
        boolean clearTop = containFlags(intent, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        boolean clearTask = containFlags(intent, Intent.FLAG_ACTIVITY_CLEAR_TASK);
        boolean multipleTask = newTask && containFlags(intent, Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        boolean reorderToFront = containFlags(intent, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        boolean singleTop = containFlags(intent, Intent.FLAG_ACTIVITY_SINGLE_TOP);

        boolean notStartToFront = false;
        if (clearTop || singleTop || clearTask) {
            notStartToFront = true;
        }
        if (!newTask) {
            clearTask = false;
        }
        TaskRecord sourceTask = null;
        if (sourceRecord != null) {
            if (forwardResult && sourceRecord.resultTo != null) {
                ActivityRecord forwardRecord = findActivityByToken(userId, sourceRecord.resultTo);
                if (forwardRecord != null) {
                    sourceRecord = forwardRecord;
                    resultTo = forwardRecord.token;
                }
            }
            sourceTask = sourceRecord.task;
        }

        TaskRecord reuseTask = null;
        if (!multipleTask) {
            switch (info.launchMode) {
                case LAUNCH_MULTIPLE: {
                    if (newTask) {
                        reuseTask = findTaskByAffinityLocked(userId, affinity);
                    } else if (isAllowUseSourceTask(sourceRecord, info)) {
                        reuseTask = sourceTask;
                    }
                    break;
                }
                case LAUNCH_SINGLE_INSTANCE: {
                    reuseTask = findTaskByAffinityLocked(userId, affinity);
                    break;
                }
                case LAUNCH_SINGLE_TASK: {
                    if (newTask || sourceTask == null) {
                        reuseTask = findTaskByAffinityLocked(userId, affinity);
                    } else if (isAllowUseSourceTask(sourceRecord, info)) {
                        reuseTask = sourceTask;
                    }
                    break;
                }
                case LAUNCH_SINGLE_TOP: {
                    reuseTask = findTaskByAffinityLocked(userId, affinity);
                    break;
                }

            }
        }
        if (reuseTask == null || reuseTask.isFinishing()) {
            return startActivityInNewTaskLocked(userId, intent, info, options, callingUid);
        }
        mAM.moveTaskToFront(reuseTask.taskId, 0);

        /*
         * 一个APP的界面已经打开，我们按Home，再从桌面打开App，不会重新启动App的界面，
         * 而是直接仅仅把界面切到前台。
         *
         */
        boolean startTaskToFront = !notStartToFront
                && ComponentUtils.intentFilterEquals(reuseTask.taskRoot, intent)
                && reuseTask.taskRoot.getFlags() == intent.getFlags();

        if (startTaskToFront) {
            return 0;
        }
        ActivityRecord notifyNewIntentActivityRecord = null;
        boolean marked = false;
        if (clearTask) {
            synchronized (reuseTask.activities) {
                for (ActivityRecord r : reuseTask.activities) {
                    r.marked = true;
                }
            }
            marked = true;
        }
        ComponentName component = ComponentUtils.toComponentName(info);
        if (info.launchMode == LAUNCH_SINGLE_INSTANCE) {
            synchronized (reuseTask.activities) {
                for (ActivityRecord r : reuseTask.activities) {
                    if (r.component.equals(component)) {
                        notifyNewIntentActivityRecord = r;
                        break;
                    }
                }
            }
        }
        boolean notReorderToFront = false;
        if (info.launchMode == LAUNCH_SINGLE_TASK || clearTop) {
            synchronized (reuseTask.activities) {
                notReorderToFront = true;
                /*
                 * (1）如果当前task包含这个Activity，这个Activity以上的Activity出栈，这个Activity到达栈顶。
                 */
                int N = reuseTask.activities.size();
                while (N-- > 0) {
                    ActivityRecord r = reuseTask.activities.get(N);
                    if (!r.marked && r.component.equals(component)) {
                        notifyNewIntentActivityRecord = r;
                        marked = true;
                        break;
                    }
                }

                if (marked) {
                    while (N++ < reuseTask.activities.size() - 1) {
                        reuseTask.activities.get(N).marked = true;
                    }
                    /*
                     *  处理 ClearTop:
                     * （2）如果这个Activity是standard模式，这个Activity也出栈，并且重新实例化到达栈顶。
                     */
                    if (clearTop && info.launchMode == LAUNCH_MULTIPLE) {
                        if (notifyNewIntentActivityRecord != null) {
                            notifyNewIntentActivityRecord.marked = true;
                            notifyNewIntentActivityRecord = null;
                        }
                    }
                }
            }
        }
        if (info.launchMode == LAUNCH_SINGLE_TOP || singleTop) {
            notReorderToFront = true;
            /*
             * 打开的Activity如果在栈顶，则不创建新的实例，并且会触发onNewIntent事件。
             */
            ActivityRecord top = reuseTask.getTopActivityRecord();
            if (top != null && !top.marked && top.component.equals(component)) {
                notifyNewIntentActivityRecord = top;
            }
        }
        if (reorderToFront) {
            ActivityRecord top = reuseTask.getTopActivityRecord();
            if (top.component.equals(component)) {
                notifyNewIntentActivityRecord = top;
            } else {
                /*
                 * 由于无法直接实现将要启动的Activity从栈中拉到栈顶，
                 * 我们直接将它finish掉，并在栈顶重新启动。
                 * 然而，某些Activity不能这样做（典例：网易新闻分享到微博然后点取消）
                 * 好在还可以workaround之。
                 */
                synchronized (reuseTask.activities) {
                    int N = reuseTask.activities.size();
                    while (N-- > 0) {
                        ActivityRecord r = reuseTask.activities.get(N);
                        if (r.component.equals(component)) {
                            if (notReorderToFront) {
                                notifyNewIntentActivityRecord = r;
                            } else {
                                r.marked = true;
                                marked = true;
                            }
                            break;
                        }
                    }
                }
            }
        }
        if (marked) {
            finishMarkedActivity();
        }
        if (notifyNewIntentActivityRecord != null) {
            deliverNewIntentLocked(userId, sourceRecord, notifyNewIntentActivityRecord, intent);
            return 0;
        }
        ActivityRecord targetRecord = newActivityRecord(intent, info, resultTo);
        Intent destIntent = startActivityProcess(userId, targetRecord, intent, info, callingUid);
        if (destIntent != null) {
            destIntent.addFlags(mLauncherFlags);
            IBinder startFrom = null;
            if (sourceTask == reuseTask) {
                startFrom = resultTo;
            } else {
                ActivityRecord topRecord = reuseTask.getTopActivityRecord();
                if (topRecord != null) {
                    startFrom = topRecord.token;
                }
            }
            startActivityFromSourceTask(startFrom, destIntent, resultWho, requestCode, options);
            return 0;
        } else {
            synchronized (mLaunchingActivities) {
                mLaunchingActivities.remove(targetRecord);
            }
            return -1;
        }
    }

    private ActivityRecord newActivityRecord(Intent intent, ActivityInfo info, IBinder resultTo) {
        ActivityRecord targetRecord = new ActivityRecord(intent, info, resultTo);
        synchronized (mLaunchingActivities) {
            mLaunchingActivities.add(targetRecord);
        }
        return targetRecord;
    }


    private int startActivityInNewTaskLocked(final int userId, Intent intent, final ActivityInfo info, final Bundle options, int callingUid) {
        ActivityRecord targetRecord = newActivityRecord(intent, info, null);
        final Intent destIntent = startActivityProcess(userId, targetRecord, intent, info, callingUid);
        if (destIntent != null) {
            destIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            destIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            destIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                // noinspection deprecation
                destIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            } else {
                destIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            }
            boolean noAnimation = false;
            try {
                noAnimation = intent.getBooleanExtra("_VA_|no_animation", false);
            } catch (Throwable e) {
                // ignore
            }
            if (noAnimation) {
                destIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            if (options != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                VirtualCore.get().getContext().startActivity(destIntent, options);
            } else {
                VirtualCore.get().getContext().startActivity(destIntent);
            }
            return 0;
        } else {
            mLaunchingActivities.remove(targetRecord);
            return -1;
        }

    }

    private void finishMarkedActivity() {
        synchronized (mHistory) {
            int N = mHistory.size();
            final List<ActivityRecord> removeRecords = new LinkedList<>();
            while (N-- > 0) {
                final TaskRecord task = mHistory.valueAt(N);
                synchronized (task.activities) {
                    for (ActivityRecord r : task.activities) {
                        if (r.marked) {
                            removeRecords.add(r);
                        }
                    }
                }
            }
            VirtualRuntime.getUIHandler().post(new Runnable() {
                @Override
                public void run() {
                    for (ActivityRecord r : removeRecords) {
                        try {
                            r.process.client.finishActivity(r.token);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void startActivityFromSourceTask(final IBinder resultTo, final Intent intent, final String resultWho,
                                             final int requestCode, final Bundle options) {
        realStartActivityLocked(resultTo, intent, resultWho, requestCode, options);
    }


    private void realStartActivitiesLocked(IBinder resultTo, Intent[] intents, String[] resolvedTypes, Bundle options) {
        Class<?>[] types = IActivityManager.startActivities.paramList();
        Object[] args = new Object[types.length];
        if (types[0] == IApplicationThread.TYPE) {
            args[0] = ActivityThread.getApplicationThread.call(VirtualCore.mainThread());
        }
        int pkgIndex = ArrayUtils.protoIndexOf(types, String.class);
        int intentsIndex = ArrayUtils.protoIndexOf(types, Intent[].class);
        int resultToIndex = ArrayUtils.protoIndexOf(types, IBinder.class, 2);
        int optionsIndex = ArrayUtils.protoIndexOf(types, Bundle.class);
        int resolvedTypesIndex = intentsIndex + 1;
        if (pkgIndex != -1) {
            args[pkgIndex] = VirtualCore.get().getHostPkg();
        }
        args[intentsIndex] = intents;
        args[resultToIndex] = resultTo;
        args[resolvedTypesIndex] = resolvedTypes;
        args[optionsIndex] = options;
        ClassUtils.fixArgs(types, args);
        IActivityManager.startActivities.call(ActivityManagerNative.getDefault.call(),
                (Object[]) args);
    }

    private void realStartActivityLocked(IBinder resultTo, Intent intent, String resultWho, int requestCode,
                                         Bundle options) {
        Class<?>[] types = mirror.android.app.IActivityManager.startActivity.paramList();
        Object[] args = new Object[types.length];
        if (types[0] == IApplicationThread.TYPE) {
            args[0] = ActivityThread.getApplicationThread.call(VirtualCore.mainThread());
        }
        int intentIndex = ArrayUtils.protoIndexOf(types, Intent.class);
        int resultToIndex = ArrayUtils.protoIndexOf(types, IBinder.class, 2);
        int optionsIndex = ArrayUtils.protoIndexOf(types, Bundle.class);
        int resolvedTypeIndex = intentIndex + 1;
        int resultWhoIndex = resultToIndex + 1;
        int requestCodeIndex = resultToIndex + 2;

        args[intentIndex] = intent;
        args[resultToIndex] = resultTo;
        args[resultWhoIndex] = resultWho;
        args[requestCodeIndex] = requestCode;
        if (optionsIndex != -1) {
            args[optionsIndex] = options;
        }
        args[resolvedTypeIndex] = intent.getType();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            args[intentIndex - 1] = VirtualCore.get().getHostPkg();
        }
        ClassUtils.fixArgs(types, args);

        try {
            mirror.android.app.IActivityManager.startActivity.call(ActivityManagerNative.getDefault.call(),
                    (Object[]) args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private String fetchStubActivity(int vpid, ActivityInfo targetInfo) {

        boolean isFloating = false;
        boolean isTranslucent = false;
        boolean showWallpaper = false;
        try {
            int[] R_Styleable_Window = R_Hide.styleable.Window.get();
            int R_Styleable_Window_windowIsTranslucent = R_Hide.styleable.Window_windowIsTranslucent.get();
            int R_Styleable_Window_windowIsFloating = R_Hide.styleable.Window_windowIsFloating.get();
            int R_Styleable_Window_windowShowWallpaper = R_Hide.styleable.Window_windowShowWallpaper.get();

            AttributeCache.Entry ent = AttributeCache.instance().get(targetInfo.packageName, targetInfo.theme,
                    R_Styleable_Window);
            if (ent != null && ent.array != null) {
                showWallpaper = ent.array.getBoolean(R_Styleable_Window_windowShowWallpaper, false);
                isTranslucent = ent.array.getBoolean(R_Styleable_Window_windowIsTranslucent, false);
                isFloating = ent.array.getBoolean(R_Styleable_Window_windowIsFloating, false);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        boolean isDialogStyle = isFloating || isTranslucent || showWallpaper;
        if (isDialogStyle) {
            return StubManifest.getStubDialogName(vpid);
        } else {
            return StubManifest.getStubActivityName(vpid);
        }
    }

    private Intent startActivityProcess(int userId, ActivityRecord targetRecord, Intent intent, ActivityInfo info, int callingUid) {
        ProcessRecord targetApp = mService.startProcessIfNeedLocked(info.processName, userId, info.packageName, -1, callingUid);
        if (targetApp == null) {
            return null;
        }
        return getStartStubActivityIntentInner(intent, targetApp.is64bit, targetApp.vpid, userId, targetRecord, info);
    }

    private Intent getStartStubActivityIntentInner(Intent intent, boolean is64bit, int vpid, int userId, ActivityRecord targetRecord, ActivityInfo info) {
        intent = new Intent(intent);
        Intent targetIntent = new Intent();
        targetIntent.setClassName(StubManifest.getStubPackageName(is64bit), fetchStubActivity(vpid, info));
        ComponentName component = intent.getComponent();
        if (component == null) {
            component = ComponentUtils.toComponentName(info);
        }
        targetIntent.setType(component.flattenToString());
        StubActivityRecord saveInstance = new StubActivityRecord(intent, info, userId, targetRecord);
        saveInstance.saveToIntent(targetIntent);
        return targetIntent;
    }

    private Intent getStartStubActivityIntent(int userId, ActivityRecord targetRecord, Intent intent, ActivityInfo info) {
        PackageSetting ps = PackageCacheManager.getSetting(info.packageName);
        if (ps == null) {
            return null;
        }
        boolean is64bit = ps.isRunOn64BitProcess();
        int vpid = VActivityManagerService.get().queryFreeStubProcess(is64bit);
        return getStartStubActivityIntentInner(intent, is64bit, vpid, userId, targetRecord, info);
    }


    void onActivityCreated(ProcessRecord targetApp, IBinder token, int taskId, ActivityRecord record) {
        synchronized (mLaunchingActivities) {
            mLaunchingActivities.remove(record);
        }
        synchronized (mHistory) {
            optimizeTasksLocked();
            TaskRecord task = mHistory.get(taskId);
            if (task == null) {
                task = new TaskRecord(taskId, targetApp.userId, ComponentUtils.getTaskAffinity(record.info), record.intent);
                mHistory.put(taskId, task);
                Intent intent = new Intent(Constants.ACTION_NEW_TASK_CREATED);
                intent.putExtra(Constants.EXTRA_USER_HANDLE, record.userId);
                intent.putExtra(Constants.EXTRA_PACKAGE_NAME, record.info.packageName);
                VirtualCore.get().getContext().sendBroadcast(intent);
            }
            record.init(task, targetApp, token);
            synchronized (task.activities) {
                task.activities.add(record);
            }
        }
    }

    void onActivityResumed(int userId, IBinder token) {
        synchronized (mHistory) {
            optimizeTasksLocked();
            ActivityRecord r = findActivityByToken(userId, token);
            if (r != null) {
                synchronized (r.task.activities) {
                    r.task.activities.remove(r);
                    r.task.activities.add(r);
                }
            }
        }
    }


    void onActivityFinish(int userId, IBinder token) {
        synchronized (mHistory) {
            ActivityRecord r = findActivityByToken(userId, token);
            if (r != null) {
                r.marked = true;
            }
        }
    }

    ActivityRecord onActivityDestroyed(int userId, IBinder token) {
        synchronized (mHistory) {
            optimizeTasksLocked();
            ActivityRecord r = findActivityByToken(userId, token);
            if (r != null) {
                r.marked = true;
                synchronized (r.task.activities) {
                    // We shouldn't remove task at this point,
                    // it will be removed by optimizeTasksLocked().
                    r.task.activities.remove(r);
                }
            }
            return r;
        }
    }

    void processDied(ProcessRecord record) {
        synchronized (mHistory) {
            optimizeTasksLocked();
            int N = mHistory.size();
            while (N-- > 0) {
                TaskRecord task = mHistory.valueAt(N);
                synchronized (task.activities) {
                    Iterator<ActivityRecord> iterator = task.activities.iterator();
                    while (iterator.hasNext()) {
                        ActivityRecord r = iterator.next();
                        if (r.process.pid != record.pid) {
                            continue;
                        }
                        iterator.remove();
                        if (task.activities.isEmpty()) {
                            mHistory.remove(task.taskId);
                        }
                    }
                }
            }

        }
    }

    String getPackageForToken(int userId, IBinder token) {
        synchronized (mHistory) {
            ActivityRecord r = findActivityByToken(userId, token);
            if (r != null) {
                return r.info.packageName;
            }
            return null;
        }
    }

    private ActivityRecord getCallingRecordLocked(int userId, IBinder token) {
        ActivityRecord r = findActivityByToken(userId, token);
        if (r == null) {
            return null;
        }
        return findActivityByToken(userId, r.resultTo);
    }

    ComponentName getCallingActivity(int userId, IBinder token) {
        ActivityRecord r = getCallingRecordLocked(userId, token);
        return r != null ? r.intent.getComponent() : null;
    }

    String getCallingPackage(int userId, IBinder token) {
        ActivityRecord r = getCallingRecordLocked(userId, token);
        return r != null ? r.info.packageName : null;
    }

    AppTaskInfo getTaskInfo(int taskId) {
        synchronized (mHistory) {
            TaskRecord task = mHistory.get(taskId);
            if (task != null) {
                return task.getAppTaskInfo();
            }
            return null;
        }
    }

    ComponentName getActivityClassForToken(int userId, IBinder token) {
        synchronized (mHistory) {
            ActivityRecord r = findActivityByToken(userId, token);
            if (r != null) {
                return r.component;
            }
            return null;
        }
    }
}