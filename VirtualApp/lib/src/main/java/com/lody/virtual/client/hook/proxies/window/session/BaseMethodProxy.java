package com.lody.virtual.client.hook.proxies.window.session;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.helper.utils.ArrayUtils;
import com.xdja.watermark.VWaterMarkManager;
import com.xdja.zs.MobileInfoUtil;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Lody
 */

class Relayout extends BaseMethodProxy{

    private String mImei = "";
    private String mIconPath = "";
    private List<String> infos = new ArrayList<>();
    public Relayout(String name) {
        super(name);
    }

    private void updateContent(){
        String info = VWaterMarkManager.get().getWaterMark();
        infos.clear();
        if(!TextUtils.isEmpty(info)){
            String content[] = info.split("#");
            for (String c : content){
                if("T".equals(c)){
                    @SuppressLint("SimpleDateFormat")
                    SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    infos.add(df.format(new Date()));
                } else if("I".equals(c)){
                    mImei = MobileInfoUtil.getIMEI(VirtualCore.get().getContext());
                    infos.add(mImei);
                } else if(c.startsWith("C")){
                    infos.add(c.substring(2));
                } else if(c.startsWith("P")){
                    mIconPath = c.substring(2);
                }
            }
        }
    }
    @Override
    public boolean beforeCall(Object who, Method method, Object... args) {
        updateContent();
        return super.beforeCall(who, method, args);
    }

    @Override
    public Object call(Object who, Method method, Object... args) throws Throwable {

        //args[0] IWindow  ViewRootImpl.W extends IWindow.stub
        //args[2] WindowManager.LayoutParams attrs,

//        for (int i=0;i<args.length;i++){
//            Log.e("lxf","relayout args["+i+"] "+ args[i]);
//        }
        Class<?> IWindow = getPM().getClass().getClassLoader().loadClass(args[0].getClass().getName());
        Field ViewRootImpl = IWindow.getDeclaredField("mViewAncestor");
        ViewRootImpl.setAccessible(true);
        WeakReference VRI = (WeakReference)ViewRootImpl.get(args[0]);
        @SuppressLint("PrivateApi")
        Class ViewRootImplClass = Class.forName("android.view.ViewRootImpl");
        Field mView = ViewRootImplClass.getDeclaredField("mView");
        mView.setAccessible(true);
        View omView = (View)mView.get(VRI.get());

        if(omView.getClass().getName().equals("com.android.internal.policy.DecorView")){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                omView.setForeground(null);
                WindowInsets inset = omView.getRootWindowInsets();
                int top = inset.getSystemWindowInsetBottom();

                int screenWidth = omView.getMeasuredWidth();
                int screenHeight = omView.getMeasuredHeight();
                Log.e("lxf","relayout "+screenWidth +":"+ screenHeight);

                //长宽比例适配，去除小窗口水印绘制
                Configuration configuration = VirtualCore.get().getContext().getResources().getConfiguration();
                if((((float)screenHeight/screenWidth)>1.5 && (configuration.orientation==configuration.ORIENTATION_PORTRAIT))
                        || (((float)screenWidth/screenHeight)>1.5 && (configuration.orientation==configuration.ORIENTATION_LANDSCAPE))){
                    Bitmap mBackgroundBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(mBackgroundBitmap);
                    draw(canvas,screenWidth,screenHeight,mImei);
                    Bitmap mDestBitmap = drawDestBitmap(mBackgroundBitmap,top,screenWidth,screenHeight);
                    omView.setForeground(new BitmapDrawable(mDestBitmap));
                }
            }
        }

        return super.call(who, method, args);
    }


    /*
    适配水印纵向偏移
     */
    public Bitmap drawDestBitmap(Bitmap src,int top,int width,int height){
        Bitmap mDestBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mDestBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setAntiAlias(true);
        Rect mTopSrcRect = new Rect(0, top, width, height+top);
        Rect mTopDestRect = new Rect(0, 0, width, height);
        canvas.drawBitmap(src, mTopSrcRect, mTopDestRect, paint);
        return mDestBitmap;
    }

    public void draw(Canvas canvas,int width, int height,String Imei){
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#AEAEAE"));
        paint.setAntiAlias(true);

        float textWidth = 110;
        //Imei不为空时才绘制Imei
        if (!TextUtils.isEmpty(Imei)) {
            canvas.save();
            paint.setStyle(Paint.Style.FILL);
            paint.setFakeBoldText(false);
            paint.setAlpha(70);
            paint.setTextSize(50);
            canvas.rotate(-30);
            int index1 = 0;
            for (int positionY = height / 4 + 40; positionY <= height * 2 + 50; positionY += height / 5) {
                float fromX = -width + (index1++ % 2) * textWidth;
                for (float positionX = fromX; positionX < width; positionX += textWidth * 5) {
                    int _positionY = positionY;
                    for (String s:infos) {
                        if(TextUtils.isEmpty(s)){
                            continue;
                        }
                        canvas.drawText(s, positionX, _positionY, paint);
                        _positionY+=60;
                    }
                }
            }
            canvas.restore();
        }

    }

    @Override
    public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {

        return super.afterCall(who, method, args, result);
    }
}

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
