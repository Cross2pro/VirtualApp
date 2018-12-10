package com.lody.virtual.client.stub;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.IInterface;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;

import mirror.android.content.ContentProviderClientICS;
import mirror.android.content.ContentProviderClientJB;

/**
 * @author Lody
 */
public class ContentProviderProxy extends ContentProvider {

    private class TargetProviderInfo {
        int userId;
        ProviderInfo info;
        Uri uri;

        TargetProviderInfo(int userId, ProviderInfo info, Uri uri) {
            this.userId = userId;
            this.info = info;
            this.uri = uri;
        }
    }

    public static Uri buildProxyUri(int userId, boolean is64bit, String authority, Uri uri) {
        String proxyAuthority = StubManifest.getProxyAuthority(is64bit);
        Uri proxyUriPrefix = Uri.parse(String.format(Locale.ENGLISH, "content://%1$s/%2$d/%3$s", proxyAuthority, userId, authority));
        return Uri.withAppendedPath(proxyUriPrefix, uri.toString());
    }


    private TargetProviderInfo getProviderProviderInfo(Uri uri) {
        if (!VirtualCore.get().isEngineLaunched()) {
            return null;
        }
        List<String> segments = uri.getPathSegments();
        if (segments == null || segments.size() < 3) {
            return null;
        }
        int userId = -1;
        try {
            userId = Integer.parseInt(segments.get(0));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (userId == -1) {
            return null;
        }
        String authority = segments.get(1);
        ProviderInfo providerInfo = VPackageManager.get().resolveContentProvider(authority, 0, userId);
        if (providerInfo == null) {
            return null;
        }
        String uriContent = uri.toString();
        return new TargetProviderInfo(
                userId,
                providerInfo,
                Uri.parse(uriContent.substring(authority.length() + uriContent.indexOf(authority, 1) + 1))
        );
    }

    private ContentProviderClient acquireProviderClient(TargetProviderInfo info) {
        try {
            IInterface provider = VActivityManager.get().acquireProviderClient(info.userId, info.info);
            if (provider != null) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    return ContentProviderClientJB.ctor.newInstance(getContext().getContentResolver(), provider, true);
                } else {
                    return ContentProviderClientICS.ctor.newInstance(getContext().getContentResolver(), provider);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ContentProviderClient acquireTargetProviderClient(Uri uri) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            return acquireProviderClient(info);
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.query(info.uri, projection, selection, selectionArgs, sortOrder);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.getType(info.uri);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.insert(info.uri, values);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.delete(info.uri, selection, selectionArgs);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.update(info.uri, values, selection, selectionArgs);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public Uri canonicalize(Uri uri) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.canonicalize(info.uri);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public Uri uncanonicalize(Uri uri) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.uncanonicalize(info.uri);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return uri;
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public boolean refresh(Uri uri, Bundle args, CancellationSignal cancellationSignal) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.refresh(info.uri, args, cancellationSignal);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.openFile(info.uri, mode);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        TargetProviderInfo info = getProviderProviderInfo(uri);
        if (info != null) {
            ContentProviderClient client = acquireProviderClient(info);
            if (client != null) {
                try {
                    return client.getStreamTypes(info.uri, mimeTypeFilter);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
