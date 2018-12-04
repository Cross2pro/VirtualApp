package com.lody.virtual.helper.utils;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.os.Process;

import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.VClient;
import com.lody.virtual.client.NativeEngine;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.stub.InstallerActivity;
import com.lody.virtual.client.stub.ShadowPendingActivity;
import com.lody.virtual.client.stub.ShadowPendingReceiver;
import com.lody.virtual.client.stub.ShadowPendingService;
import com.lody.virtual.helper.compat.ActivityManagerCompat;
import com.lody.virtual.helper.compat.ObjectsCompat;
import com.lody.virtual.os.VUserHandle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_INSTANCE;

/**
 * @author Lody
 */
public class ComponentUtils {

    public static String getTaskAffinity(ActivityInfo info) {
        if (info.launchMode == LAUNCH_SINGLE_INSTANCE) {
            return "-SingleInstance-" + info.packageName + "/" + info.name;
        } else if (info.taskAffinity == null && info.applicationInfo.taskAffinity == null) {
            return info.packageName;
        } else if (info.taskAffinity != null) {
            return info.taskAffinity;
        }
        return info.applicationInfo.taskAffinity;
    }

    public static boolean isSameIntent(Intent a, Intent b) {
        if (a != null && b != null) {
            if (!ObjectsCompat.equals(a.getAction(), b.getAction())) {
                return false;
            }
            if (!ObjectsCompat.equals(a.getData(), b.getData())) {
                return false;
            }
            if (!ObjectsCompat.equals(a.getType(), b.getType())) {
                return false;
            }
            Object pkgA = a.getPackage();
            if (pkgA == null && a.getComponent() != null) {
                pkgA = a.getComponent().getPackageName();
            }
            String pkgB = b.getPackage();
            if (pkgB == null && b.getComponent() != null) {
                pkgB = b.getComponent().getPackageName();
            }
            if (!ObjectsCompat.equals(pkgA, pkgB)) {
                return false;
            }
            if (!ObjectsCompat.equals(a.getComponent(), b.getComponent())) {
                return false;
            }
            if (!ObjectsCompat.equals(a.getCategories(), b.getCategories())) {
                return false;
            }
        }
        return true;
    }

    public static String getProcessName(ComponentInfo componentInfo) {
        String processName = componentInfo.processName;
        if (processName == null) {
            processName = componentInfo.packageName;
            componentInfo.processName = processName;
        }
        return processName;
    }

    public static boolean isSameComponent(ComponentInfo first, ComponentInfo second) {

        if (first != null && second != null) {
            String pkg1 = first.packageName + "";
            String pkg2 = second.packageName + "";
            String name1 = first.name + "";
            String name2 = second.name + "";
            return pkg1.equals(pkg2) && name1.equals(name2);
        }
        return false;
    }

    public static ComponentName toComponentName(ComponentInfo componentInfo) {
        return new ComponentName(componentInfo.packageName, componentInfo.name);
    }

    public static boolean isSystemApp(ApplicationInfo applicationInfo) {
        if(applicationInfo == null){
            return false;
        }
        if("com.google.android.gsf".equals(applicationInfo.packageName)){
            return !VirtualCore.get().isAppInstalled("com.google.android.gsf");
        }else if(GmsSupport.isGoogleAppOrService(applicationInfo.packageName)){
            return false;
        }else if(SpecialComponentList.isSpecSystemPackage(applicationInfo.packageName)){
            return true;
        }else if(applicationInfo.uid >= Process.FIRST_APPLICATION_UID && (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0){
            //app in /system,but it's updated
            return false;
        }else if(SpecialComponentList.isSpecSystemPackage(applicationInfo.packageName)){
            return true;
        }else if(applicationInfo.uid >= Process.FIRST_APPLICATION_UID){
            //app in /system,but it's updated
            return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        }
        //system
        return true;
    }

    public static boolean isStubComponent(Intent intent) {
        return intent != null
                && intent.getComponent() != null
                && VirtualCore.get().getHostPkg().equals(intent.getComponent().getPackageName());
    }

    public static Intent redirectBroadcastIntent(Intent intent, int userId) {
        Intent newIntent = intent.cloneFilter();
        newIntent.setComponent(null);
        newIntent.setPackage(null);
        ComponentName component = intent.getComponent();
        String pkg = intent.getPackage();
        newIntent.putExtra("_VA_|_user_id_", userId);
        newIntent.putExtra("_VA_|_callingUid_", VClient.get().getVUid());
        if (component != null) {

            newIntent.setAction(String.format("_VA_%s_%s", component.getPackageName(), component.getClassName()));
            newIntent.putExtra("_VA_|_component_", component);
            newIntent.putExtra("_VA_|_intent_", new Intent(intent));
        } else if (pkg != null) {
            newIntent.putExtra("_VA_|_creator_", pkg);
            newIntent.putExtra("_VA_|_intent_", new Intent(intent));
            String protectedAction = SpecialComponentList.protectAction(intent.getAction());
            if (protectedAction != null) {
                newIntent.setAction(protectedAction);
            }
        } else {
            newIntent.putExtra("_VA_|_intent_", new Intent(intent));
            String protectedAction = SpecialComponentList.protectAction(intent.getAction());
            if (protectedAction != null) {
                newIntent.setAction(protectedAction);
            }
        }
        return newIntent;
    }

    private static final String SCHEME_FILE = "file";
    private static final String SCHEME_PACKAGE = "package";
    private static final String SCHEME_CONTENT = "content";
    private static String parseInstallRequest(Intent intent){
        Uri packageUri = intent.getData();
        String path = null;
        if (SCHEME_FILE.equals(packageUri.getScheme())) {

            File sourceFile = new File(packageUri.getPath());
            path = NativeEngine.getRedirectedPath(sourceFile.getAbsolutePath());
            VLog.e("wxd", " parseInstallRequest path : " + path);
        }else if (SCHEME_CONTENT.equals(packageUri.getScheme())) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            File sharedFileCopy = new File(VirtualCore.get().getContext().getCacheDir(), packageUri.getLastPathSegment());
            try {
                inputStream = VirtualCore.get().getContext().getContentResolver().openInputStream(packageUri);
                outputStream = new FileOutputStream(sharedFileCopy);
                byte[] buffer = new byte[1024];
                int count;
                while ((count = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, count);
                }
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                FileUtils.closeQuietly(inputStream);
                FileUtils.closeQuietly(outputStream);
            }
            path = sharedFileCopy.getPath();
            VLog.e("wxd", " parseInstallRequest sharedFileCopy path : " + path);
        }
        return path;
    }
    public static Intent redirectIntentSender(int type, String creator, Intent intent, IBinder iBinder) {
        Intent newIntent = intent.cloneFilter();
        switch (type) {
            case ActivityManagerCompat.INTENT_SENDER_ACTIVITY_RESULT:
            case ActivityManagerCompat.INTENT_SENDER_ACTIVITY: {
                if(Intent.ACTION_VIEW.equalsIgnoreCase(intent.getAction())
                        && "application/vnd.android.package-archive".equalsIgnoreCase(intent.getType())){
                    Intent intent1 = new Intent();
                    intent1.putExtra("source_apk", VirtualRuntime.getInitialPackageName());
                    intent1.putExtra("installer_path", parseInstallRequest(intent));
                    intent1.setComponent(new ComponentName(VirtualCore.get().getContext(), InstallerActivity.class));
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    return intent1;
                }
                ComponentInfo info = VirtualCore.get().resolveActivityInfo(intent, VUserHandle.myUserId());
                if (info != null) {
                    newIntent.setClass(VirtualCore.get().getContext(), ShadowPendingActivity.class);
                    newIntent.setFlags(intent.getFlags());
                    if (iBinder != null) {
                        try {
                            ComponentName activityForToken = VActivityManager.get().getActivityForToken(iBinder);
                            if (activityForToken != null) {
                                newIntent.putExtra("_VA_|_caller_", activityForToken);
                                break;
                            }
                        } catch (Throwable e) {
                        }
                    }
                }
            }
            break;
            case ActivityManagerCompat.INTENT_SENDER_SERVICE: {
                ComponentInfo info = VirtualCore.get().resolveServiceInfo(intent, VUserHandle.myUserId());
                if (info != null) {
                    newIntent.setClass(VirtualCore.get().getContext(), ShadowPendingService.class);
                }
            }
            break;
            case ActivityManagerCompat.INTENT_SENDER_BROADCAST: {
                newIntent.setClass(VirtualCore.get().getContext(), ShadowPendingReceiver.class);
            }
            break;
            default:
                return null;
        }
        newIntent.putExtra("_VA_|_user_id_", VUserHandle.myUserId());
        newIntent.putExtra("_VA_|_intent_", intent);
        newIntent.putExtra("_VA_|_creator_", creator);
        newIntent.putExtra("_VA_|_from_inner_", true);
        return newIntent;
    }

}
