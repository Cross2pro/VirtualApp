package com.xdja.zs;

import android.util.Log;
import com.lody.virtual.os.VEnvironment;
import java.io.File;
import java.math.BigDecimal;

public class DataCleanManager {

    public static int FOLDER_TYPE_APP = 1;
    public static int FOLDER_TYPE_DATA = 2;
    public static int FOLDER_TYPE_CACHE = 3;
    public static int FOLDER_TYPE_TOTAL = 4;

    public static String TAG = "DataCleanManager";

    public static String getPackageFolderSize(int userId, String packageName, int type){
        String folderSize = null;
        try {
            if (type == FOLDER_TYPE_APP) {
                File file_app = VEnvironment.getDataAppPackageDirectory(packageName);
                folderSize = getFormatSize(getFolderSize(file_app));
                Log.i(TAG, file_app.getAbsolutePath() + " size : " + folderSize);
            }
            if (type == FOLDER_TYPE_DATA) {
                File file_data = VEnvironment.getDataUserPackageDirectory(userId, packageName);
                File file_data_storage = VEnvironment.getExternalStorageAppDataDir(userId, packageName);
                folderSize = getFormatSize(getFolderSize(file_data) + getFolderSize(file_data_storage));
                Log.i(TAG, file_data.getAbsolutePath() + " size : " + getFormatSize(getFolderSize(file_data))
                        + file_data_storage.getAbsolutePath() + "size : " + getFormatSize(getFolderSize(file_data_storage)));
            }
            if (type == FOLDER_TYPE_CACHE) {
                File file_cache = new File(VEnvironment.getDataUserPackageDirectory(userId, packageName), "cache");
                File file_cache_storage = new File(VEnvironment.getExternalStorageAppDataDir(userId, packageName), "cache");
                folderSize = getFormatSize(getFolderSize(file_cache) + getFolderSize(file_cache_storage));
                Log.i(TAG, file_cache.getAbsolutePath() + " size : " + getFormatSize(getFolderSize(file_cache))
                        + file_cache_storage.getAbsolutePath() + " size : " + getFormatSize(getFolderSize(file_cache_storage)));
            }
            if (type == FOLDER_TYPE_TOTAL) {
                File file_app = VEnvironment.getDataAppPackageDirectory(packageName);
                File file_data = VEnvironment.getDataUserPackageDirectory(userId, packageName);
                File file_data_storage = VEnvironment.getExternalStorageAppDataDir(userId, packageName);
                folderSize = getFormatSize(getFolderSize(file_app) + getFolderSize(file_data) + getFolderSize(file_data_storage));
                Log.i(TAG, file_app.getAbsolutePath() + " size : " + getFormatSize(getFolderSize(file_app))
                        + file_data.getAbsolutePath() + " size : " + getFormatSize(getFolderSize(file_data))
                        + file_data_storage.getAbsolutePath() + " size : " + getFormatSize(getFolderSize(file_data_storage)));
            }
        }catch (Exception e){
            e.printStackTrace();
            return folderSize;
        }
        return folderSize;
    }


    public static void clearPackageFolder(int useId, String packageName, int type){
        try {
            if (type == FOLDER_TYPE_DATA) {
                File file_data = VEnvironment.getDataUserPackageDirectory(useId, packageName);
                File file_data_storage = VEnvironment.getExternalStorageAppDataDir(useId, packageName);
                deleteDir(file_data);
                deleteDir(file_data_storage);
                Log.i(TAG, " clearPackageFolder FOLDER_TYPE_DATA");
            }else if (type == FOLDER_TYPE_CACHE) {
                File file_cache = new File(VEnvironment.getDataUserPackageDirectory(useId, packageName), "cache");
                File file_cache_storage = new File(VEnvironment.getExternalStorageAppDataDir(useId, packageName), "cache");
                deleteDir(file_cache);
                deleteDir(file_cache_storage);
                Log.i(TAG, " clearPackageFolder FOLDER_TYPE_CACHE");
            }else{
                Log.i(TAG, " clearPackageFolder " + type);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static long getFolderSize(File file) throws Exception {
        long size = 0;
        try {
            File[] fileList = file.listFiles();
            if(fileList == null){
                return 0;
            }
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isDirectory()) {
                    size = size + getFolderSize(fileList[i]);
                } else {
                    size = size + fileList[i].length();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    public static String getFormatSize(double size) {
        double kiloByte = size / 1024;
        if (kiloByte < 1) {
//            return size + "Byte";
            return "0K";
        }

        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "K";
        }

        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "M";
        }

        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
                + "TB";
    }
}
