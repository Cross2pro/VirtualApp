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

import com.xdja.watermark.VWaterMarkManager;
import com.xdja.zs.WaterMarkInfo;

/**
 * Created by thz on 2019/4/16.
 * 工作域外部提供ContentProvider
 */

public class SafetyProvider extends ContentProvider {
    private static final String TAG = SafetyProvider.class.getSimpleName();
    private static final String TAG_LXF = "lxf " + SafetyProvider.class.getSimpleName();
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
        Bundle result = new Bundle();
        //校验调用方的合法性
        if (TextUtils.isEmpty(method) || TextUtils.isEmpty(arg) || !judgeCaller(arg)) {
            return result;
        }
        switch (method) {
            case "setWaterMarkContent":
                String content = extras.getString("watermarkContext");
                Log.e("lxf-SafettyProvider","content "+content);
                VWaterMarkManager.get().setWaterMark(content);
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
