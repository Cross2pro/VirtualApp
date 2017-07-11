package com.lody.virtual.server.location;

import android.os.Parcel;
import android.os.Parcelable;

public class GpsStatus implements Parcelable {
    private int svCount;
    private int[] svidWithFlags;
    private float[] cn0s;
    private float[] elevations;
    private float[] azimuths;

    public GpsStatus() {
    }

    public int getSvCount() {
        return svCount;
    }

    public void setSvCount(int svCount) {
        this.svCount = svCount;
    }

    public int[] getSvidWithFlags() {
        return svidWithFlags;
    }

    public void setSvidWithFlags(int[] svidWithFlags) {
        this.svidWithFlags = svidWithFlags;
    }

    public float[] getCn0s() {
        return cn0s;
    }

    public void setCn0s(float[] cn0s) {
        this.cn0s = cn0s;
    }

    public float[] getElevations() {
        return elevations;
    }

    public void setElevations(float[] elevations) {
        this.elevations = elevations;
    }

    public float[] getAzimuths() {
        return azimuths;
    }

    public void setAzimuths(float[] azimuths) {
        this.azimuths = azimuths;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.svCount);
        dest.writeIntArray(this.svidWithFlags);
        dest.writeFloatArray(this.cn0s);
        dest.writeFloatArray(this.elevations);
        dest.writeFloatArray(this.azimuths);
    }

    protected GpsStatus(Parcel in) {
        this.svCount = in.readInt();
        this.svidWithFlags = in.createIntArray();
        this.cn0s = in.createFloatArray();
        this.elevations = in.createFloatArray();
        this.azimuths = in.createFloatArray();
    }

    public static final Parcelable.Creator<GpsStatus> CREATOR = new Parcelable.Creator<GpsStatus>() {
        @Override
        public GpsStatus createFromParcel(Parcel source) {
            return new GpsStatus(source);
        }

        @Override
        public GpsStatus[] newArray(int size) {
            return new GpsStatus[size];
        }
    };
}
