package io.virtualapp.home.models;

import android.graphics.drawable.Drawable;

/**
 * @author Lody
 */

public abstract class AppData {

    public boolean isFirstOpen;
    public boolean isLoading;

    public boolean isLoading() {
        return isLoading;
    }


    public boolean isFirstOpen() {
        return isFirstOpen;
    }


    public Drawable getIcon() {
        return null;
    }


    public String getName() {
        return null;
    }


    public String getPackageName() {
        return null;
    }


    public boolean canReorder() {
        return false;
    }


    public boolean canLaunch() {
        return false;
    }


    public boolean canDelete() {
        return false;
    }


    public boolean canCreateShortcut() {
        return false;
    }


    public int getUserId() {
        return 0;
    }
}
