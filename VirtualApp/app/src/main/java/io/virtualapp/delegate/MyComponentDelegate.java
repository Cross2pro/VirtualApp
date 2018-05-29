package io.virtualapp.delegate;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.delegate.ComponentDelegate;
import com.lody.virtual.server.am.AttributeCache;

import java.lang.reflect.Field;

import mirror.com.android.internal.R_Hide;

public class MyComponentDelegate implements ComponentDelegate {

    @Override
    public void beforeApplicationCreate(Application application) {
        Log.e("lxf", "beforeApplicationCreate ");
    }

    @Override
    public void afterApplicationCreate(Application application) {
        Log.e("lxf", "afterApplicationCreate ");
        //TODO: listen activity lifecycle
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Log.e("lxf", "onActivityCreated "+activity.getLocalClassName());
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log.e("lxf", "onActivityStarted "+activity.getLocalClassName());

            }
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onActivityResumed(Activity activity) {
                Log.e("lxf", "onActivityResumed "+activity.getLocalClassName());
//                boolean issd = isStubDialog(activity);
//                Log.e("lxf", "isStubDialog "+issd);
//                if(issd)
//                    return;
                //清除前景水印
                View view = activity.getWindow().getDecorView();
                view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        Log.e("lxf", "onLayoutChange w "+v.getMeasuredWidth()+" h "+v.getMeasuredHeight());
                        view.setForeground(null);
                        int screenWidth = v.getMeasuredWidth();
                        int screenHeight = v.getMeasuredHeight();
                        if(screenWidth<=0 || screenHeight<=0)
                            return;
                        Bitmap mBackgroundBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(mBackgroundBitmap);
                        draw(canvas,screenWidth,screenHeight,"8ik7uj6yh5tg4rf");
                        view.setForeground(new BitmapDrawable(mBackgroundBitmap));
                    }
                });

            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log.e("lxf", "onActivityPaused "+activity.getLocalClassName());
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Log.e("lxf", "onActivityStopped "+activity.getLocalClassName());
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                Log.e("lxf", "onActivitySaveInstanceState "+activity.getLocalClassName());
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }


    public boolean isStubDialog(Activity target){
        ActivityInfo targetInfo = null;
        try {
            Field field=Activity.class.getDeclaredField("mActivityInfo");
            field.setAccessible(true);//修改访问权限
            targetInfo = (ActivityInfo)field.get(target);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if(targetInfo==null)
            return false;
        boolean isFloating = false;
        boolean isTranslucent = false;
        boolean showWallpaper = false;
        try {
            int[] R_Styleable_Window = R_Hide.styleable.Window.get();
            int R_Styleable_Window_windowIsTranslucent = R_Hide.styleable.Window_windowIsTranslucent.get();
            int R_Styleable_Window_windowIsFloating = R_Hide.styleable.Window_windowIsFloating.get();
            int R_Styleable_Window_windowShowWallpaper = R_Hide.styleable.Window_windowShowWallpaper.get();
            AttributeCache.init(VirtualCore.get().getContext());
            AttributeCache.Entry ent = AttributeCache.instance().get(targetInfo.packageName, targetInfo.theme,
                    R_Styleable_Window);
            if (ent != null && ent.array != null) {
                showWallpaper = ent.array.getBoolean(R_Styleable_Window_windowShowWallpaper, false);
                isTranslucent = ent.array.getBoolean(R_Styleable_Window_windowIsTranslucent, false);
                isFloating = ent.array.getBoolean(R_Styleable_Window_windowIsFloating, false);
            }else{
                Resources resources= VirtualCore.get().getResources(targetInfo.packageName);
                if(resources!=null) {
                    TypedArray typedArray = resources.newTheme().obtainStyledAttributes(targetInfo.theme, R_Styleable_Window);
                    if(typedArray!=null){
                        showWallpaper = typedArray.getBoolean(R_Styleable_Window_windowShowWallpaper, false);
                        isTranslucent = typedArray.getBoolean(R_Styleable_Window_windowIsTranslucent, false);
                        isFloating = typedArray.getBoolean(R_Styleable_Window_windowIsFloating, false);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        boolean isDialogStyle = isFloating || isTranslucent || showWallpaper;
        return isDialogStyle;
    }
    @Override
    public void beforeActivityCreate(Activity activity) {
        Log.e("lxf", "beforeActivityCreate ");
    }

    @Override
    public void beforeActivityResume(Activity activity) {
        Log.e("lxf", "beforeActivityResume ");

    }

    @Override
    public void beforeActivityPause(Activity activity) {
        Log.e("lxf", "beforeActivityPause ");

    }

    @Override
    public void beforeActivityDestroy(Activity activity) {

    }

    @Override
    public void afterActivityCreate(Activity activity) {
        Log.e("lxf", "afterActivityCreate ");
    }

    @Override
    public void afterActivityResume(Activity activity) {
    }

    @Override
    public void afterActivityPause(Activity activity) {

    }

    @Override
    public void afterActivityDestroy(Activity activity) {

    }

    @Override
    public void onSendBroadcast(Intent intent) {

    }

    public static void draw(Canvas canvas,int width, int height,String Imei){
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#AEAEAE"));
        paint.setAntiAlias(true);
//          paint.setTextAlign(Paint.Align.CENTER);
        String logo = "安  全  盒";
        //绘制文字
        canvas.save();
        paint.setStyle(Paint.Style.STROKE);
        paint.setFakeBoldText(true);
        paint.setAlpha(70);
        paint.setStrokeWidth(5);
        paint.setTextSize(100);
        canvas.rotate(-45);
        float textWidth = paint.measureText(logo);
        int index = 0;
        for (int positionY = height / 4; positionY <= height * 2 + 50; positionY += height / 4) {
            float fromX = -width + (index++ % 2) * textWidth;
            for (float positionX = fromX; positionX < width; positionX += textWidth * 2) {
                canvas.drawText(logo, positionX, positionY, paint);
            }
        }
        canvas.restore();

        //Imei不为空时才绘制Imei
        if (!TextUtils.isEmpty(Imei)) {
            canvas.save();
            paint.setStyle(Paint.Style.FILL);
            paint.setFakeBoldText(false);
            paint.setAlpha(70);
            paint.setTextSize(20);
            canvas.rotate(-45);
            int index1 = 0;
            for (int positionY = height / 4 + 40; positionY <= height * 2 + 50; positionY += height / 4) {
                float fromX = -width + (index1++ % 2) * textWidth;
                for (float positionX = fromX; positionX < width; positionX += textWidth * 2) {
                    canvas.drawText(Imei, positionX, positionY, paint);
                }
            }
            canvas.restore();
        }

        Log.e("lxf","onDraw end");
    }

}
