package com.lody.virtual.client.hook.proxies.window.session;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.WindowManager;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.helper.utils.ArrayUtils;

import java.lang.reflect.Method;

/**
 * @author Lody
 */
/*package*/ class BaseMethodProxy extends StaticMethodProxy {

    public BaseMethodProxy(String name) {
        super(name);
    }

    private boolean mDrawOverlays = false;

    protected boolean isDrawOverlays(){
        return mDrawOverlays;
    }

    @SuppressLint("SwitchIntDef")
    @Override
    public boolean beforeCall(Object who, Method method, Object... args){
        mDrawOverlays = false;
        int index = ArrayUtils.indexOfFirst(args, WindowManager.LayoutParams.class);
        if (index != -1) {
            WindowManager.LayoutParams attrs = (WindowManager.LayoutParams) args[index];
            if (attrs != null) {
                attrs.packageName = getHostPkg();
                switch (attrs.type) {
                    case WindowManager.LayoutParams.TYPE_PHONE:
                    case WindowManager.LayoutParams.TYPE_PRIORITY_PHONE:
                    case WindowManager.LayoutParams.TYPE_SYSTEM_ALERT:
                    case WindowManager.LayoutParams.TYPE_SYSTEM_ERROR:
                    case WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY:
                    case WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:
                        mDrawOverlays = true;
                        break;
                    default:
                        break;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (VirtualCore.get().getTargetSdkVersion() >= Build.VERSION_CODES.O) {
                        //
                        if(mDrawOverlays){
                            attrs.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                        }
                    }
                }
            }
        }
        return true;
    }
}
