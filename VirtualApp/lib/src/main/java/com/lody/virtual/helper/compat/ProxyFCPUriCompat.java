package com.lody.virtual.helper.compat;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.server.interfaces.IPackageObserver;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static android.content.ContentResolver.SCHEME_FILE;

public class ProxyFCPUriCompat {
    private static final String TAG = "UriCompat";
    private static boolean DEBUG = false;
    public static String AUTH = "virtual.fileprovider";

    private static final String SUFFIX = "@_outside";
    private static ProxyFCPUriCompat sInstance = new ProxyFCPUriCompat();

    public static ProxyFCPUriCompat get(){
        return sInstance;
    }

    private boolean isInsideuthority(String name){
        return VPackageManager.get().isVirtualAuthority(name);
    }

    /**
     * 内外部都存在某个contentProvider的时候使用
     *
     * @return 是否是指定访问外部contentprovider
     */
    public boolean isOutSide(String auth) {
        return auth != null && auth.endsWith(SUFFIX);
    }

    /**
     * 内外部都存在某个contentProvider的时候使用
     *
     * @param auth 外部contentProvider的auth
     * @return 经过处理的auth
     */
    public String wrapperOutSide(String auth) {
        int index = auth.lastIndexOf(SUFFIX);
        if (index < 0) {
            return auth + SUFFIX;
        }
        return auth;
    }

    /**
     * 内外部都存在某个contentProvider的时候使用
     *
     * @param auth 经过处理的auth
     * @return 外部contentProvider的auth
     */
    public String unWrapperOutSide(String auth) {
        int index = auth.lastIndexOf(SUFFIX);
        if (index > 0) {
            return auth.substring(0, index);
        }
        return auth;
    }

    public boolean needFake(Intent intent) {
        String pkg = intent.getPackage();
        //inside
        if (pkg != null && VirtualCore.get().isAppInstalled(pkg)) {
            return false;
        }
        //inside
        ComponentName componentName = intent.getComponent();
        if (componentName != null && VirtualCore.get().isAppInstalled(componentName.getPackageName())) {
            return false;
        }
        //fake all intent's uri
        return true;
    }

    public String getAuthority() {
        return VirtualCore.getConfig().getProxyFileContentProviderAuthority();
    }

    private String encode(String uri) throws UnsupportedEncodingException {
        byte[] data = Base64.encode(uri.getBytes("utf-8"),
                Base64.URL_SAFE | Base64.NO_WRAP);
        return new String(data, "US-ASCII");
    }

    private String decode(String uri) throws UnsupportedEncodingException {
        byte[] data = Base64.decode(uri.getBytes("US-ASCII"),
                Base64.URL_SAFE | Base64.NO_WRAP);
        return new String(data, "utf-8");
    }

    public Uri wrapperUri(Uri uri) {
        if (uri == null){
            return null;
        }
        String auth = uri.getAuthority();
        if(getAuthority().equals(auth) || !isInsideuthority(auth)){
            return uri;
        }
        if ("content".equals(uri.getScheme())) {
            //fake auth
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(uri.getScheme());
            builder.authority(getAuthority()).appendPath("out");
            try {
                builder.appendQueryParameter("uri", encode(uri.toString()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            Uri u = builder.build();
            if (DEBUG) {
                Log.i(TAG, "fake uri:" + uri + "->" + u);
            }
            return u;
        } else if (SCHEME_FILE.equals(uri.getScheme())) {
            String path = uri.getPath();
            String external_path = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (path.startsWith(external_path)) {
                String split_path = path.substring(external_path.length());

                Uri fake_uri = uri.buildUpon().scheme(SCHEME_CONTENT).path("/external" + split_path).authority(AUTH).appendQueryParameter("__va_scheme", SCHEME_FILE).build();
                return fake_uri;
            }

            return null;
        } else {
            //https://play.google.com
            return null;
        }
    }

    public Uri unWrapperUri(Uri uri) {
        if (uri == null) {
            return null;
        }
        if(!TextUtils.equals(uri.getAuthority(), getAuthority())){
            return uri;
        }
        String uriStr = uri.getQueryParameter("uri");
        if(!TextUtils.isEmpty(uriStr)) {
            Uri u = null;
            try {
                u = Uri.parse(decode(uriStr));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (DEBUG) {
                Log.i(TAG, "wrapperUri uri:" + uri + "->" + u);
            }
            return u;
        }
        String auth = uri.getQueryParameter("__va_auth");
        if (!TextUtils.isEmpty(auth)) {
            Uri.Builder builder = uri.buildUpon().authority(auth);
            Set<String> names = uri.getQueryParameterNames();
            Map<String, String> querys = null;
            if (names != null && names.size() > 0) {
                querys = new HashMap<>();
                for (String name : names) {
                    querys.put(name, uri.getQueryParameter(name));
                }
            }
            builder.clearQuery();
            for (Map.Entry<String, String> e : querys.entrySet()) {
                if (!"__va_auth".equals(e.getKey())) {
                    builder.appendQueryParameter(e.getKey(), e.getValue());
                }
            }
            Uri u = builder.build();
            if (DEBUG) {
                Log.i(TAG, "unWrapperUri uri:" + uri + "->" + u);
            }
            return u;
        } else if (SCHEME_FILE.equals(uri.getQueryParameter("__va_scheme"))) {
            String path = uri.getEncodedPath();
            final int splitIndex = path.indexOf('/', 1);
            final String tag = Uri.decode(path.substring(1, splitIndex));
            path = Uri.decode(path.substring(splitIndex + 1));
            if ("external".equals(tag))
            {
                int userId = VUserHandle.myUserId();
                File root = VEnvironment.getExternalStorageDirectory(userId);
                File file = new File(root, path);

                Uri u = Uri.fromFile(file);
                return u;
            }
        }
        return null;
    }

    public Intent fakeFileUri(Intent intent) {
        if (!needFake(intent)) {
            if (DEBUG) {
                VLog.i(TAG, "don't need fake intent");
            }
            return intent;
        }
        Uri uri = intent.getData();
        if (uri != null) {
            Uri u = wrapperUri(uri);
            if (u != null) {
                if (DEBUG) {
                    Log.i(TAG, "fake data uri:" + uri + "->" + u);
                }
                intent.setDataAndType(u, intent.getType());
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            ClipData data = intent.getClipData();
            if (data != null) {
                int count = data.getItemCount();
                for (int i = 0; i < count; i++) {
                    ClipData.Item item = data.getItemAt(i);
                    Uri u = wrapperUri(item.getUri());
                    if (u != null) {
                        Reflect.on(item).set("mUri", u);
                    }
                }
            }
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            boolean changed2 = false;
            Set<String> keys = extras.keySet();
            for (String key : keys) {
                Object val = extras.get(key);
                if (val instanceof Intent) {
                    Intent i = fakeFileUri((Intent) val);
                    if (i != null) {
                        changed2 = true;
                        extras.putParcelable(key, i);
                    }
                } else if (val instanceof Uri) {
                    Uri u = wrapperUri((Uri) val);
                    if (u != null) {
                        changed2 = true;
                        extras.putParcelable(key, u);
                    }
                }
            }
            if (changed2) {
                intent.putExtras(extras);
            }
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        return intent;
    }
}
