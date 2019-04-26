package com.xdja.call;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.RemoteException;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.xdja.utils.Stirrer;

public class CallLogObserver extends android.database.ContentObserver {
    private static final CallLogObserver ourInstance = new CallLogObserver(getAsyncHandler());
    private static final String TAG = "xela-" + new Object() { }.getClass().getEnclosingClass().getSimpleName();

    static CallLogObserver getInstance() {
        return ourInstance;
    }

    private CallLogObserver(Handler handler) {
        super(handler);
    }

    private Context getContext() {
        return VirtualCore.get().getContext();
    }

    private void transferCallLog() {
        long currTime = System.currentTimeMillis();
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "i do not have permission to read call log.");
            return;
        }

        Cursor cursor = getContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, "date DESC");

        if (cursor != null) {
            String[] columns = new String[]{"add_for_all_users", "countryiso", "data_usage", "date", "duration", "features", "formatted_number", "geocoded_location", "is_read", "last_modified", "lookup_uri", "matched_number", "name", "new", "normalized_number", "number", "numberlabel", "numbertype", "phone_account_address", "photo_id", "photo_uri", "post_dial_digits", "presentation", "subscription_component_name", "subscription_id", "transcription", "type", "via_number", "voicemail_uri"};

            if (cursor.moveToFirst()) {
                ContentValues contentValues = new ContentValues();

                long last_modified = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.LAST_MODIFIED));
                if (Math.abs(currTime - last_modified) > 500) {
                    return;
                }

                for (String column : columns) {
                    int colindex = cursor.getColumnIndex(column);
                    if (colindex != -1) {
                        int type = cursor.getType(colindex);
                        if (type == Cursor.FIELD_TYPE_INTEGER) {
                            long value = cursor.getLong(colindex);
                            contentValues.put(column, value);
                        } else if (type == Cursor.FIELD_TYPE_STRING) {
                            String value = cursor.getString(colindex);
                            contentValues.put(column, value);
                        }
                    }
                }
                try {
                    Stirrer.getConentProvider(CallLog.AUTHORITY).insert(CallLog.Calls.CONTENT_URI, contentValues);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                int id = cursor.getInt(cursor.getColumnIndex(CallLog.Calls._ID));
                cursor.close();
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, this.getClass() + " do not have permission to write call log.");
                    return;
                }
                getContext().getContentResolver().unregisterContentObserver(getInstance());
                getContext().getContentResolver().delete(CallLog.Calls.CONTENT_URI, "_id = ?", new String[]{String.valueOf(id)});
                getContext().getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, true, getInstance());
            } else {
                Log.d(TAG, "cursor.moveToFirst failed");
            }

        } else {
            Log.d(TAG, "get nothing from calllog ");
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        transferCallLog();
    }

    private synchronized static Handler getAsyncHandler() {
        if (sAsyncHandlerThread == null) {
            sAsyncHandlerThread = new HandlerThread("sAsyncHandlerThread",
                    Process.THREAD_PRIORITY_BACKGROUND);
            sAsyncHandlerThread.start();
            sAsyncHandler = new Handler(sAsyncHandlerThread.getLooper());
        }
        return sAsyncHandler;
    }

    private static HandlerThread sAsyncHandlerThread;
    private static Handler sAsyncHandler;

    public static void observe() {
        getInstance().getContext().getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, true, getInstance());
    }

    public static void unObserve() {
        getInstance().getContext().getContentResolver().unregisterContentObserver(getInstance());
    }
}
