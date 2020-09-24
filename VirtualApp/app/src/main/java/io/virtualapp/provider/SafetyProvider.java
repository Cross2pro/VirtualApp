package io.virtualapp.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.xdja.utils.SignatureVerify;
import com.xdja.zs.VWaterMarkManager;
import com.xdja.zs.WaterMarkInfo;

import static android.os.Binder.getCallingUid;

/**
 * Created by thz on 2019/4/16.
 * 工作域外部提供ContentProvider
 */

public class SafetyProvider extends ContentProvider {
    private static final String TAG = SafetyProvider.class.getSimpleName();
    /**
     * Uri
     */
    private static final Uri CONTENT_URI = Uri.parse(PathConfig.CONTENT_URI);
    /**
     * 传入的包名
     */
    private static final String PACKAGE_NAME = "packageName";
    /**
     * success标识
     */
    private static final String SUCCESS = "success";

    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {

        String packageName = null;
        String[] packages = getContext().getPackageManager().getPackagesForUid(getCallingUid());
        if(packages!=null && packages.length>0) {
            packageName = packages[0];
        }
        Log.e(TAG,"CallingPackageName "+ packageName);
        String sha1 = SignatureVerify.getSHA1Signature(packageName);
        Log.e(TAG,"SHA1 "+ sha1);

        Bundle result = new Bundle();
        //校验调用方的合法性
        if (TextUtils.isEmpty(method) || TextUtils.isEmpty(arg) || !judgeCaller(arg)) {
            return result;
        }
        switch (method) {
            case "currentSpace":{
                result.putBoolean("space", true);
                break;
            }
            case "setWaterMarkContent":
                String content = extras.getString("content");
                int rotate = extras.getInt("rotate");
                float distance = extras.getFloat("distance");
                float textSize = extras.getFloat("textSize");
                float textAlpha = extras.getFloat("textAlpha");
                String textColor = extras.getString("textColor");

                WaterMarkInfo info = new WaterMarkInfo();
                info.setWaterMarkContent(content);
                info.setRotate(rotate);
                info.setDistance(distance);
                info.setTextSize(textSize);//20 24 26sp 60 72 78;
                info.setTextAlpha(textAlpha);
                info.setTextColor(textColor);
                VWaterMarkManager.get().setWaterMark(info);
                break;
        }
        return result;
    }



    /**
     * 判断调用方的合法性
     */
    private boolean judgeCaller(String pkg) {
        //TODO thz 校验调用方的合法性
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

}
