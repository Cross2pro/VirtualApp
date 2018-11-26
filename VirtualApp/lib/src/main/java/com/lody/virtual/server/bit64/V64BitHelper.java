package com.lody.virtual.server.bit64;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.ipc.ProviderCall;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.ArtDexOptimizer;
import com.lody.virtual.helper.compat.NativeLibraryHelperCompat;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.os.VEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dalvik.system.DexFile;

/**
 * @author Lody
 */
public class V64BitHelper extends ContentProvider {

    public static String DEF_AUTHORITY = "virtual.service.64bit_helper";
    public static String AUTHORITY = DEF_AUTHORITY;

    private static final String[] METHODS = {
            "getRunningAppProcess",
            "getRunningTasks",
            "forceStop",
            "installPackage"

    };
    private static boolean sInit = false;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }


    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (METHODS[0].equals(method)) {
            return getRunningAppProcess64(extras);
        } else if (METHODS[1].equals(method)) {
            return getRunningTasks64(extras);
        } else if (METHODS[2].equals(method)) {
            return forceStop64(extras);
        } else if (METHODS[3].equals(method)) {
            return installPackage64(extras);
        }
        return null;
    }

    private Bundle getRunningAppProcess64(Bundle extras) {
        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningAppProcessInfo> processes = new ArrayList<>(am.getRunningAppProcesses());
        Bundle res = new Bundle();
        res.putParcelableArrayList("running_processes", processes);
        return res;
    }

    private Bundle getRunningTasks64(Bundle extras) {
        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        int maxNum = extras.getInt("max_num", 99);
        ArrayList<ActivityManager.RunningTaskInfo> tasks = new ArrayList<>(am.getRunningTasks(maxNum));
        Bundle res = new Bundle();
        res.putParcelableArrayList("running_tasks", tasks);
        return res;
    }

    private Bundle forceStop64(Bundle extras) {
        Object pidOrPids = extras.get("target");
        if (pidOrPids instanceof Integer) {
            int pid = (int) pidOrPids;
            Process.killProcess(pid);
        } else if (pidOrPids instanceof int[]) {
            int[] pids = (int[]) pidOrPids;
            for (int pid : pids) {
                Process.killProcess(pid);
            }
        }
        return null;
    }

    private Bundle installPackage64(Bundle extras) {
        boolean success = false;
        String publicApkPath = extras.getString("public_apk_path");
        String packageName = extras.getString("package_name");
        if (publicApkPath != null && packageName != null) {
            File publicPath = new File(publicApkPath);
            if (publicPath.exists()) {
                File targetPath = VEnvironment.getPackageResourcePath(packageName);
                try {
                    FileUtils.copyFile(publicPath, targetPath);
                    VEnvironment.chmodPackageDictionary(targetPath);
                    File libDir = VEnvironment.getAppLibDirectory(packageName);
                    NativeLibraryHelperCompat.copyNativeBinaries(targetPath, libDir);
                    VEnvironment.linkUserAppLib(0, packageName);
                    if (VirtualRuntime.isArt()) {
                        try {
                            ArtDexOptimizer.interpretDex2Oat(targetPath.getPath(), VEnvironment.getOdexFile(packageName).getPath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            DexFile.loadDex(targetPath.getPath(), VEnvironment.getOdexFile(packageName).getPath(), 0).close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    success = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Bundle res = new Bundle();
        res.putBoolean("res", success);
        return res;
    }

    public static void check64BitRunning(Activity activity){
        String action = VASettings.PACKAGE_NAME+".action.MAIN64";
        Intent intent = new Intent(action);
        intent.setPackage(VASettings.PACKAGE_NAME_64BIT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if(activity != null){
                activity.startActivity(intent);
            }else {
                VirtualCore.get().getContext().startActivity(intent);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void checkOnce() {
        if (sInit) return;
        sInit = true;
        check64BitRunning(null);
    }

    public static List<ActivityManager.RunningAppProcessInfo> getRunningAppProcess64() {
        if (!VirtualCore.get().is64BitEngineInstalled()) {
            return Collections.emptyList();
        }
        checkOnce();
        Bundle res = new ProviderCall.Builder(VirtualCore.get().getContext(), AUTHORITY).methodName(METHODS[0]).call();
        return res.getParcelableArrayList("running_processes");
    }

    public static List<ActivityManager.RunningTaskInfo> getRunningTasks64(int maxNum) {
        if (!VirtualCore.get().is64BitEngineInstalled()) {
            return Collections.emptyList();
        }
        checkOnce();
        Bundle res = new ProviderCall.Builder(VirtualCore.get().getContext(), AUTHORITY).methodName(METHODS[1]).addArg("max_num", maxNum).call();
        return res.getParcelableArrayList("running_tasks");
    }

    public static void forceStop64(int pid) {
        if (!VirtualCore.get().is64BitEngineInstalled()) {
            return;
        }
        checkOnce();
        new ProviderCall.Builder(VirtualCore.get().getContext(), AUTHORITY).methodName(METHODS[2]).addArg("target", pid).call();
    }

    public static void forceStop64(int[] pids) {
        if (!VirtualCore.get().is64BitEngineInstalled()) {
            return;
        }
        checkOnce();
        new ProviderCall.Builder(VirtualCore.get().getContext(), AUTHORITY).methodName(METHODS[2]).addArg("target", pids).call();
    }

    public static boolean installPackage64(String publicApkPath, String packageName) {
        if (!VirtualCore.get().is64BitEngineInstalled()) {
            return false;
        }
        checkOnce();
        Bundle res = new ProviderCall.Builder(VirtualCore.get().getContext(), AUTHORITY)
                .methodName(METHODS[0])
                .addArg("public_apk_path", publicApkPath)
                .addArg("package_name", packageName)
                .call();
        return res.getBoolean("res");
    }
}
