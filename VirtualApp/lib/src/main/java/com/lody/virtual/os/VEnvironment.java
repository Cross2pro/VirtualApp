package com.lody.virtual.os;

import android.content.Context;
import android.os.Build;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.EncodeUtils;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.VLog;

import java.io.File;

/**
 * @author Lody
 */

public class VEnvironment {

    private static final String TAG = VEnvironment.class.getSimpleName();

    private static final File ROOT;
    private static final File DATA_DIRECTORY;
    private static final File USER_DIRECTORY;
    private static final File DALVIK_CACHE_DIRECTORY;
    private static final File EXTERNAL_STORAGE_DIRECTORY;
    private static final File EMULATED_DIRECTORY;

    private static final String DIRECTORY_MUSIC = "Music";
    private static final String DIRECTORY_PODCASTS = "Podcasts";
    private static final String DIRECTORY_RINGTONES = "Ringtones";
    private static final String DIRECTORY_ALARMS = "Alarms";
    private static final String DIRECTORY_NOTIFICATIONS = "Notifications";
    private static final String DIRECTORY_PICTURES = "Pictures";
    private static final String DIRECTORY_MOVIES = "Movies";
    private static final String DIRECTORY_DOWNLOADS = "Download";
    private static final String DIRECTORY_DCIM = "DCIM";
    private static final String DIRECTORY_DOCUMENTS = "Documents";

    public static final String[] STANDARD_DIRECTORIES = {
            DIRECTORY_MUSIC,
            DIRECTORY_PODCASTS,
            DIRECTORY_RINGTONES,
            DIRECTORY_ALARMS,
            DIRECTORY_NOTIFICATIONS,
            DIRECTORY_PICTURES,
            DIRECTORY_MOVIES,
            DIRECTORY_DOWNLOADS,
            DIRECTORY_DCIM,
            DIRECTORY_DOCUMENTS
    };

    static {
        File host = new File(getContext().getApplicationInfo().dataDir);
        // Point to: /
        ROOT = ensureCreated(new File(host, "virtual"));
        // Point to: /data/
        DATA_DIRECTORY = ensureCreated(new File(ROOT, "data"));
        // Point to: /data/user/
        USER_DIRECTORY = ensureCreated(new File(DATA_DIRECTORY, "user"));
        // Point to: /opt/
        DALVIK_CACHE_DIRECTORY = ensureCreated(new File(ROOT, "opt"));
        // Point to: /storage/
        EXTERNAL_STORAGE_DIRECTORY = ensureCreated(new File(ROOT, "storage"));
        // Point to: /storage/emulated
        EMULATED_DIRECTORY = ensureCreated(new File(EXTERNAL_STORAGE_DIRECTORY, "emulated"));
    }

    public static void systemReady() {
        //create private dir at SdCard & all of TFCard
        getContext().getExternalFilesDir(null).getAbsolutePath();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                FileUtils.chmod(ROOT.getAbsolutePath(), FileUtils.FileMode.MODE_755);
                FileUtils.chmod(DATA_DIRECTORY.getAbsolutePath(), FileUtils.FileMode.MODE_755);
                FileUtils.chmod(getDataAppDirectory().getAbsolutePath(), FileUtils.FileMode.MODE_755);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private static Context getContext() {
        return VirtualCore.get().getContext();
    }

    private static File ensureCreated(File folder) {
        if (!folder.exists() && !folder.mkdirs()) {
            VLog.w(TAG, "Unable to create the directory: %s.", folder.getPath());
        }
        return folder;
    }

    public static File getDataUserPackageDirectory(int userId,
                                                   String packageName) {
        return ensureCreated(new File(getUserSystemDirectory(userId), packageName));
    }

    public static File getPackageResourcePath(String packageName) {
        return new File(getDataAppPackageDirectory(packageName), /*base.apk*/EncodeUtils.decodeBase64("YmFzZS5hcGs="));
    }

    public static File getDataAppDirectory() {
        return ensureCreated(new File(getDataDirectory(), "app"));
    }

    public static File getUidListFile() {
        return new File(getSystemSecureDirectory(), "uid-list.ini");
    }

    public static File getBakUidListFile() {
        return new File(getSystemSecureDirectory(), "uid-list.ini.bak");
    }

    public static File getAccountConfigFile() {
        return new File(getSystemSecureDirectory(), "account-list.ini");
    }

    public static File getAccountVisibilityConfigFile() {
        return new File(getSystemSecureDirectory(), "account-visibility-list.ini");
    }

    public static File getVirtualLocationFile() {
        return new File(getSystemSecureDirectory(), "virtual-loc.ini");
    }

    public static File getDeviceInfoFile() {
        return new File(getSystemSecureDirectory(), "device-info.ini");
    }

    public static File getPackageListFile() {
        return new File(getSystemSecureDirectory(), "packages.ini");
    }

    /**
     * @return Virtual storage config file
     */
    public static File getVSConfigFile() {
        return new File(getSystemSecureDirectory(), "vss.ini");
    }

    public static File getBakPackageListFile() {
        return new File(getSystemSecureDirectory(), "packages.ini.bak");
    }


    public static File getJobConfigFile() {
        return new File(getSystemSecureDirectory(), "job-list.ini");
    }

    public static File getDalvikCacheDirectory() {
        return DALVIK_CACHE_DIRECTORY;
    }

    public static File getOdexFile(String packageName) {
        return new File(DALVIK_CACHE_DIRECTORY, "data@app@" + packageName + "-1@base.apk@classes.dex");
    }

    public static File getDataAppPackageDirectory(String packageName) {
        return ensureCreated(new File(getDataAppDirectory(), packageName));
    }

    public static File getAppLibDirectory(String packageName) {
        return ensureCreated(new File(getDataAppPackageDirectory(packageName), "lib"));
    }

    public static File getUserAppLibDirectory(int userId, String packageName) {
        return new File(getDataUserPackageDirectory(userId, packageName), "lib");
    }

    public static File getPackageCacheFile(String packageName) {
        return new File(getDataAppPackageDirectory(packageName), "package.ini");
    }

    public static File getSignatureFile(String packageName) {
        return new File(getDataAppPackageDirectory(packageName), "signature.ini");
    }

    public static File getUserSystemDirectory() {
        return USER_DIRECTORY;
    }

    /**
     * @param userId
     * @return
     * @see #getUserDataDirectory
     * @deprecated
     */
    public static File getUserSystemDirectory(int userId) {
        return getUserDataDirectory(userId);
    }

    public static File getUserDataDirectory(int userId) {
        return new File(USER_DIRECTORY, String.valueOf(userId));
    }

    public static File getSystemDirectory(int userId) {
        return new File(getUserDataDirectory(userId), "system");
    }

    public static File getSystemBuildFile(int userId) {
        return new File(getSystemDirectory(userId), "build.prop");
    }

    public static File getAppBuildFile(String packageName, int userId) {
        return new File(getSystemDirectory(userId), packageName + "_build.prop");
    }

    public static File getWifiMacFile(int userId) {
        return new File(getSystemDirectory(userId), "wifiMacAddress");
    }

    public static File getDataDirectory() {
        return DATA_DIRECTORY;
    }

    public static File getSystemSecureDirectory() {
        return ensureCreated(new File(getDataAppDirectory(), "system"));
    }

    public static File getPackageInstallerStageDir() {
        return ensureCreated(new File(DATA_DIRECTORY, ".session_dir"));
    }

    /**
     * get all of data dir of current context
     * @return
     */
    public static File[] getTFRoots(){
        return getContext().getExternalFilesDirs(null);
    }
    /**
     * get TFCard root dir
     * @param Dir  /storage/XXXXX/Android/data/@packagename
     * @return /storage/XXXXX
     */
    public static File getTFRoot(String Dir){
        int lastIndex = Dir.lastIndexOf("/Android/data/");
        return ensureCreated(new File(Dir.substring(0,lastIndex)));
    }
    /**
     * create and return virtual dir of safetybox at TFCard
     * @param tfroot /storage/XXXXXX
     * @return  /storage/XXXXXX/Android/daa/com.xdja/safetybox/virtual/
     */
    public static File getTFVirtualRoot(String tfroot){
        String appFileDir = tfroot + "/Android/data/"+getContext().getPackageName()+"";
        return ensureCreated(new File(appFileDir, "virtual"));
    }
    public static File getTFVirtualRoot(String tfroot,String Dir) {
        return ensureCreated(new File(getTFVirtualRoot(tfroot).getAbsolutePath(), Dir));
    }

    public static File getExternalStorageDirectory(int userId) {
        File storage_dir = ensureCreated(new File(EMULATED_DIRECTORY, String.valueOf(userId)));
        for (String sdir : STANDARD_DIRECTORIES) {
            ensureCreated(new File(storage_dir, sdir));
        }
        //return ensureCreated(new File(EMULATED_DIRECTORY, String.valueOf(userId)));
        return storage_dir;
    }

    public static File buildPath(File base, String... segments) {
        File cur = base;
        for (String segment : segments) {
            if (cur == null) {
                cur = new File(segment);
            } else {
                cur = new File(cur, segment);
            }
        }

        return cur;
    }

    public static File getExternalStorageAppDataDir(int userId, String packageName) {
        return buildPath(getExternalStorageDirectory(userId), "Android", "data", packageName);
    }
}
