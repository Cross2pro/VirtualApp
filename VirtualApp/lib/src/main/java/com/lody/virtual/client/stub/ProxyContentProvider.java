package com.lody.virtual.client.stub;


import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import com.lody.virtual.helper.compat.ContentProviderCompat;
import com.lody.virtual.helper.compat.UriCompat;
import com.lody.virtual.helper.utils.VLog;

import java.io.FileNotFoundException;

public class ProxyContentProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    private Uri wrapperUri(Uri uri) {
        return UriCompat.wrapperUri(uri);
    }

    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        try {
            return ContentProviderCompat.crazyAcquireContentProvider(getContext(), wrapperUri(uri)).query(uri, strArr, str, strArr2, str2);
        } catch (Exception e) {
            e.printStackTrace();
            return new MatrixCursor(new String[]{});
        }
    }

    public String getType(Uri uri) {
        Uri a = wrapperUri(uri);
        try {
            return ContentProviderCompat.crazyAcquireContentProvider(getContext(), a).getType(a);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Uri insert(Uri uri, ContentValues contentValues) {
        Uri a = wrapperUri(uri);
        try {
            return ContentProviderCompat.crazyAcquireContentProvider(getContext(), a).insert(a, contentValues);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int delete(Uri uri, String str, String[] strArr) {
        Uri a = wrapperUri(uri);
        try {
            return ContentProviderCompat.crazyAcquireContentProvider(getContext(), a).delete(a, str, strArr);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        Uri a = wrapperUri(uri);
        try {
            return ContentProviderCompat.crazyAcquireContentProvider(getContext(), a).update(a, contentValues, str, strArr);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public AssetFileDescriptor openAssetFile(Uri uri, String str) throws FileNotFoundException {
        Uri a = wrapperUri(uri);
        try {
            return ContentProviderCompat.crazyAcquireContentProvider(getContext(), a).openAssetFile(a, str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public AssetFileDescriptor openAssetFile(Uri uri, String str, CancellationSignal cancellationSignal) throws FileNotFoundException {
        Uri a = wrapperUri(uri);
        try {
            return ContentProviderCompat.crazyAcquireContentProvider(getContext(), a).openAssetFile(a, str, cancellationSignal);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        Uri a = wrapperUri(uri);
        try {
            return ContentProviderCompat.crazyAcquireContentProvider(getContext(), a).openFile(a, str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public ParcelFileDescriptor openFile(Uri uri, String str, CancellationSignal cancellationSignal) throws FileNotFoundException {
        Uri a = wrapperUri(uri);
        try {
            return ContentProviderCompat.crazyAcquireContentProvider(getContext(), a).openFile(a, str, cancellationSignal);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Bundle call(String str, String str2, Bundle bundle) {
        VLog.d("ProxyContentProvider", "method: " + str + " arg: " + str2);
        return super.call(str, str2, bundle);
    }

}
