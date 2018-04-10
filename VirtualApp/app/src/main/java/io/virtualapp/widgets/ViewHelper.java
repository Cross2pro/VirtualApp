package io.virtualapp.widgets;

import io.virtualapp.App;

/**
 * @author Lody
 */
public class ViewHelper {

    public static int dip2px(float dpValue) {
        final float scale = App.getApp().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
