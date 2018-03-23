package io.virtualapp.delegate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.lody.virtual.client.hook.delegate.ComponentDelegate;

public class MyComponentDelegate implements ComponentDelegate {

    public final int watermarkid = 0x7fffffff;//As big as possible
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
                String acname = activity.getLocalClassName();
                if (acname.equals("com.xdja.jxclient.jingxin.activity.AlertDialogActivity")
                        || acname.equals("com.xdja.jxclient.main.activity.ConflictActivity")
                        || acname.equals("com.xdja.jxclient.jingxin.activity.ContextMenu"))
                    return;
                boolean appPermissionEnable = true;
                try{
                    View v =  activity.findViewById(watermarkid);

                    Log.e("lxf", "onActivityStarted watermark "+(v==null?"FALSE":"TRUE")+" "+appPermissionEnable);
                    if (v==null&&appPermissionEnable){
                        WaterMarkBorder _getBorder = new WaterMarkBorder(activity);
                       ViewGroup.LayoutParams params =
                                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        activity.getWindow().addContentView(_getBorder,params);

                        if(activity.getParent()!=null)
                        Log.e("lxf", "onActivityStarted 1 "+activity.getParent().getLocalClassName());
                    }else if(v!=null&&!appPermissionEnable){
                        ((ViewGroup) activity.getWindow().findViewById(android.R.id.content)).removeView(v);
                    }
                }catch (NoSuchMethodError e){
                    e.printStackTrace();
                }
            }
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onActivityResumed(Activity activity) {
                Log.e("lxf", "onActivityResumed "+activity.getLocalClassName());
                View v =  activity.findViewById(watermarkid);
                Log.e("lxf", "onActivityResumed watermark "+(v==null?"FALSE":"TRUE"));
                if(v!=null)
                    v.bringToFront();
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
//        String acname = activity.getLocalClassName();
//        Log.e("lxf", "getLocalClassName " + acname);
//        if (acname.equals("com.xdja.jxclient.jingxin.activity.AlertDialogActivity")
//                || acname.equals("com.xdja.jxclient.main.activity.ConflictActivity")
//                || acname.equals("com.xdja.jxclient.jingxin.activity.ContextMenu"))
//            return;
////        View v = ((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
////        if(v!=null){
////            Drawable dl = v.getForeground();
////            String f_img_watermark = VirtualCore.get().getContext().getFilesDir().getAbsolutePath()+"/img_watermark.png";
////            Drawable d = Drawable.createFromPath(f_img_watermark);
////            if(d!=null&&dl==null){
////                v.setForeground(d);
////            }
////        }
//        VAppPermissionManager appPermissionManager = VAppPermissionManager.get();
//        if (appPermissionManager == null) {
//            return;
//        }
//        boolean appPermissionEnable = appPermissionManager.getAppPermissionEnable(
//                activity.getPackageName(), VAppPermissionManager.ALLOW_WATER_MARK);
//        if (appPermissionEnable) {
//            WaterMarkBorder _getBorder = new WaterMarkBorder(activity);
//            ((ViewGroup) activity.findViewById(android.R.id.content)).addView(_getBorder);
//        }
//        boolean appPermissionEnable1 = appPermissionManager.getAppPermissionEnable(
//                activity.getPackageName(), VAppPermissionManager.PROHIBIT_SCREEN_SHORT_OR_RECORDER);
//        if (appPermissionEnable1) {
//            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
//        }
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

    public class WaterMarkBorder extends View {
        private String Imei = null;
        private Paint paint;

        @SuppressLint("ResourceType")
        public WaterMarkBorder(Context context) {
            super(context);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.e("lxf", "Application maybe crash . ");
                Imei = "";
                return;
            }
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                Imei = telephonyManager.getDeviceId();
            }
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            setId(watermarkid);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (paint == null) {
                paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            }
            int width = getWidth();
            int height = getHeight();
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
            //TODO 暂时屏蔽绿色边框
//            //绘制边框
//            paint.setStyle(Paint.Style.STROKE);//不填充
//            int lineW = 10;
//            paint.setStrokeWidth(lineW);
//            paint.setColor(Color.GREEN);
//            canvas.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, paint);
        }
    }

}
