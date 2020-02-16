package andhook;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.swift.sandhook.HookLog;
import com.swift.sandhook.PendingHookHandler;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.SandHookConfig;

import java.io.File;

import com.swift.sandhook.xposedcompat.XposedCompat;

import static com.swift.sandhook.xposedcompat.utils.DexMakerUtils.MD5;

public class SandXposed {
    private static int getPreviewSDKInt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                return Build.VERSION.PREVIEW_SDK_INT;
            } catch (Throwable e) {
                // ignore
            }
        }
        return 0;
    }

    private static boolean isQ() {
        return Build.VERSION.SDK_INT > 28 || (Build.VERSION.SDK_INT == 28 && getPreviewSDKInt() > 0);
    }

    /**
     * 在Application，Va的App进程的初始化调用，onVirtualProcess
     */
    public static void init(boolean debug, File cacheDir) {
        SandHookConfig.DEBUG = debug;
        HookLog.DEBUG = debug;
        SandHookConfig.SDK_INT = isQ() ? 29 : Build.VERSION.SDK_INT;
        SandHookConfig.compiler = SandHookConfig.SDK_INT < Build.VERSION_CODES.O;
        if (PendingHookHandler.canWork()) {
            Log.e("SandHook", "Pending Hook Mode!");
        }
        SandHook.disableVMInline();
        XposedCompat.cacheDir = new File(cacheDir, "sandhook_cache_general");
    }

    /**
     * 在VClient的Application初始化之前调用，AppCallback#beforeStartApplication
     */
    public static void initForXposed(Context context, String processName) {
        XposedCompat.cacheDir = new File(context.getCacheDir(), MD5(processName));
    }
}
