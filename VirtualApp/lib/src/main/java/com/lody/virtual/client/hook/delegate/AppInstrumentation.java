package com.lody.virtual.client.hook.delegate;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.fixer.ActivityFixer;
import com.lody.virtual.client.fixer.ContextFixer;
import com.lody.virtual.client.hook.proxies.am.HCallbackStub;
import com.lody.virtual.client.interfaces.IInjector;
import com.lody.virtual.helper.compat.ActivityManagerCompat;

import java.lang.reflect.Field;

import mirror.android.app.ActivityThread;

/**
 * @author Lody
 */
public final class AppInstrumentation extends InstrumentationDelegate implements IInjector {

    private static final String TAG = AppInstrumentation.class.getSimpleName();

    private static AppInstrumentation gDefault;

    private AppInstrumentation(Instrumentation base) {
        super(base);
    }

    public static AppInstrumentation getDefault() {
        if (gDefault == null) {
            synchronized (AppInstrumentation.class) {
                if (gDefault == null) {
                    gDefault = create();
                }
            }
        }
        return gDefault;
    }

    private static AppInstrumentation create() {
        Instrumentation instrumentation = ActivityThread.mInstrumentation.get(VirtualCore.mainThread());
        if (instrumentation instanceof AppInstrumentation) {
            return (AppInstrumentation) instrumentation;
        }
        return new AppInstrumentation(instrumentation);
    }


    @Override
    public void inject() {
        base = ActivityThread.mInstrumentation.get(VirtualCore.mainThread());
        ActivityThread.mInstrumentation.set(VirtualCore.mainThread(), this);
    }

    @Override
    public boolean isEnvBad() {
        return !checkInstrumentation(ActivityThread.mInstrumentation.get(VirtualCore.mainThread()));
    }

    private boolean checkInstrumentation(Instrumentation instrumentation) {
        if (instrumentation instanceof AppInstrumentation) {
            return true;
        }
        Class<?> clazz = instrumentation.getClass();
        if (Instrumentation.class.equals(clazz)) {
            return false;
        }
        do {
            Field[] fields = clazz.getDeclaredFields();
            if (fields != null) {
                for (Field field : fields) {
                    if (Instrumentation.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object obj;
                        try {
                            obj = field.get(instrumentation);
                        } catch (IllegalAccessException e) {
                            return false;
                        }
                        if ((obj instanceof AppInstrumentation)) {
                            return true;
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        } while (!Instrumentation.class.equals(clazz));
        return false;
    }

    private void checkActivityCallback() {
        InvocationStubManager.getInstance().checkEnv(HCallbackStub.class);
        InvocationStubManager.getInstance().checkEnv(AppInstrumentation.class);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        checkActivityCallback();
        ContextFixer.fixContext(activity);
        ActivityFixer.fixActivity(activity);
        ActivityInfo info = mirror.android.app.Activity.mActivityInfo.get(activity);
        if (info != null) {
            if (info.theme != 0) {
                activity.setTheme(info.theme);
            }
            if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    && info.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                if (activity.getRequestedOrientation() != info.screenOrientation) {
                    ActivityManagerCompat.setActivityOrientation(activity, info.screenOrientation);
                    boolean needWait;
                    //set orientation
                    Configuration configuration = activity.getResources().getConfiguration();
                    if (isOrientationLandscape(info.screenOrientation)) {
                        needWait = configuration.orientation != Configuration.ORIENTATION_LANDSCAPE;
                        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
                    } else {
                        needWait = configuration.orientation != Configuration.ORIENTATION_PORTRAIT;
                        configuration.orientation = Configuration.ORIENTATION_PORTRAIT;
                    }
                    if (needWait) {
                        try {
                            Thread.sleep(800);
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                }
            }
        }
        super.callActivityOnCreate(activity, icicle);
    }

    private boolean isOrientationLandscape(int requestedOrientation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                    || (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
                    || (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                    || (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        } else {
            return (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                    || (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
                    || (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        }
    }


    @Override
    public void callActivityOnDestroy(Activity activity) {
        super.callActivityOnDestroy(activity);
    }

    @Override
    public void callActivityOnPause(Activity activity) {
        super.callActivityOnPause(activity);
    }


    @Override
    public void callApplicationOnCreate(Application app) {
        checkActivityCallback();
        super.callApplicationOnCreate(app);
    }

}
