package com.lody.virtual.remote;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Lody
 */
public class ClientConfig implements Parcelable {
    public boolean is64Bit;
    public int vpid;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.is64Bit ? (byte) 1 : (byte) 0);
        dest.writeInt(this.vpid);
    }

    public ClientConfig() {
    }

    protected ClientConfig(Parcel in) {
        this.is64Bit = in.readByte() != 0;
        this.vpid = in.readInt();
    }

    public static final Parcelable.Creator<ClientConfig> CREATOR = new Parcelable.Creator<ClientConfig>() {
        @Override
        public ClientConfig createFromParcel(Parcel source) {
            return new ClientConfig(source);
        }

        @Override
        public ClientConfig[] newArray(int size) {
            return new ClientConfig[size];
        }
    };
}
