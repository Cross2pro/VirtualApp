package com.lody.virtual.remote;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Lody
 */
public class BroadcastIntentData implements Parcelable {
    public int userId;
    public Intent intent;
    public String targetPackage;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.userId);
        dest.writeParcelable(this.intent, flags);
        dest.writeString(this.targetPackage);
    }

    public BroadcastIntentData(int userId, Intent intent, String targetPackage) {
        this.userId = userId;
        this.intent = intent;
        this.targetPackage = targetPackage;
    }

    public BroadcastIntentData(Parcel in) {
        this.userId = in.readInt();
        this.intent = in.readParcelable(Intent.class.getClassLoader());
        this.targetPackage = in.readString();
    }

    public static final Parcelable.Creator<BroadcastIntentData> CREATOR = new Parcelable.Creator<BroadcastIntentData>() {
        @Override
        public BroadcastIntentData createFromParcel(Parcel source) {
            return new BroadcastIntentData(source);
        }

        @Override
        public BroadcastIntentData[] newArray(int size) {
            return new BroadcastIntentData[size];
        }
    };
}
